package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class ArtifactBuilderApp {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true);

    static {
        MAPPER.getFactory().configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
    }

    private static final Set<String> SUPPORTED_ARGS = Set.of(
            "snapshot-base-uri",
            "snapshot-input",
            "snapshot-version",
            "snapshot-locale",
            "artifacts-base-uri",
            "output-directory",
            "artifact-version",
            "structured-output"
    );

    private ArtifactBuilderApp() {
    }

    public static void main(String[] args) {
        try {
            run(args, System.out);
        } catch (Exception exc) {
            System.err.println("Artifact build failed: " + exc.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) throws IOException {
        run(args, System.out);
    }

    static void run(String[] args, PrintStream stdout) throws IOException {
        Map<String, String> parsedArgs = parseArgs(args);
        ArtifactConfig config = buildConfig(parsedArgs);

        SnapshotPaths snapshotPaths = resolveSnapshotPaths(config);
        Path snapshotPath = snapshotPaths.championIndex();
        Path championDirectory = snapshotPaths.championDirectory();

        if (!Files.exists(snapshotPath)) {
            throw new FileNotFoundException("Snapshot not found: " + snapshotPath);
        }
        if (!Files.isDirectory(championDirectory)) {
            throw new FileNotFoundException("Champion directory not found: " + championDirectory);
        }

        ArtifactPaths artifactPaths = resolveArtifactPaths(config);
        Path mappingOutputPath = artifactPaths.mapping();
        Path coreOutputPath = artifactPaths.core();
        Path spellsOutputPath = artifactPaths.spells();

        JsonNode payload = MAPPER.readTree(snapshotPath.toFile());
        var mapping = ChampionMappingBuilder.build(payload);
        List<JsonNode> championPayloads = readChampionPayloads(championDirectory);
        var coreRows = ChampionCoreCsvBuilder.build(championPayloads);
        var spellRows = ChampionSpellCsvBuilder.build(championPayloads);

        String json = deterministicJson(mapping);
        writeAtomically(mappingOutputPath, json + "\n");
        writeAtomically(coreOutputPath,
                CsvWriter.write(ChampionCoreCsvBuilder.headers(), ChampionCoreCsvBuilder.toCsvRows(coreRows)));
        writeAtomically(spellsOutputPath,
                CsvWriter.write(ChampionSpellCsvBuilder.headers(), ChampionSpellCsvBuilder.toCsvRows(spellRows)));

        BuildMetadata metadata = new BuildMetadata(
                config.snapshotVersion(),
                config.snapshotLocale(),
                config.artifactVersion(),
                List.of(
                        describeArtifact("champion-mapping", mappingOutputPath),
                        describeArtifact("champion-core", coreOutputPath),
                        describeArtifact("champion-spells", spellsOutputPath)
                )
        );
        if ("json".equals(config.structuredOutput())) {
            stdout.println(deterministicJson(new StructuredResult("SUCCESS", metadata)));
        } else {
            stdout.println("Wrote champion mapping to " + mappingOutputPath);
            stdout.println("Wrote champion core CSV to " + coreOutputPath);
            stdout.println("Wrote champion spells CSV to " + spellsOutputPath);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsed = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }
            String key = arg.substring(2);
            if (!SUPPORTED_ARGS.contains(key)) {
                throw new IllegalArgumentException("Unsupported argument: " + arg);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for argument: " + arg);
            }
            parsed.put(key, args[++i]);
        }
        return parsed;
    }

    private static ArtifactConfig buildConfig(Map<String, String> args) {
        String snapshotBase = pickValue(args.get("snapshot-base-uri"),
                System.getenv("SNAPSHOT_BASE_URI"),
                "data/ddragon/extracted");
        String snapshotInput = pickValue(args.get("snapshot-input"),
                System.getenv("SNAPSHOT_INPUT"),
                null);
        String snapshotVersion = pickValue(args.get("snapshot-version"),
                System.getenv("SNAPSHOT_VERSION"),
                null);
        if (snapshotVersion == null) {
            throw new IllegalArgumentException("Snapshot version is required.");
        }
        String snapshotLocale = pickValue(args.get("snapshot-locale"),
                System.getenv("SNAPSHOT_LOCALE"),
                "en_US");
        String artifactsBase = pickValue(args.get("artifacts-base-uri"),
                System.getenv("ARTIFACTS_BASE_URI"),
                "data");
        String outputDirectory = pickValue(args.get("output-directory"),
                System.getenv("OUTPUT_DIRECTORY"),
                null);
        String artifactVersion = pickValue(args.get("artifact-version"),
                System.getenv("ARTIFACT_VERSION"),
                "latest");
        String structuredOutput = pickValue(args.get("structured-output"),
                System.getenv("STRUCTURED_OUTPUT"),
                null);
        if (structuredOutput != null && !"json".equals(structuredOutput)) {
            throw new IllegalArgumentException("STRUCTURED_OUTPUT must be 'json' when provided.");
        }

        return new ArtifactConfig(snapshotBase, snapshotInput, snapshotVersion, snapshotLocale,
                artifactsBase, outputDirectory, artifactVersion, structuredOutput);
    }

    private static String pickValue(String argValue, String envValue, String defaultValue) {
        if (argValue != null && !argValue.isBlank()) {
            return argValue;
        }
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    private static Path resolveBasePath(String uriValue, String envLabel) {
        if (uriValue == null || uriValue.isBlank()) {
            throw new IllegalArgumentException(envLabel + " is required.");
        }
        if (uriValue.regionMatches(true, 0, "file:", 0, 5)) {
            return Paths.get(URI.create(uriValue));
        }
        if (uriValue.matches("^[A-Za-z][A-Za-z0-9+.-]*:.*")) {
            throw new IllegalArgumentException(envLabel + " must be a local path or file:// URI.");
        }
        return Paths.get(uriValue);
    }

    private static Path resolveSnapshotRoot(Path snapshotBase, String snapshotVersion, String snapshotLocale) {
        Path directRoot = snapshotBase.resolve(snapshotVersion);
        if (isSnapshotRoot(directRoot, snapshotLocale)) {
            return directRoot;
        }

        Path nestedRoot = directRoot.resolve(snapshotVersion);
        if (isSnapshotRoot(nestedRoot, snapshotLocale)) {
            return nestedRoot;
        }

        return directRoot;
    }

    private static SnapshotPaths resolveSnapshotPaths(ArtifactConfig config) {
        if (config.snapshotInputUri() == null) {
            Path snapshotBase = resolveBasePath(config.snapshotBaseUri(), "SNAPSHOT_BASE_URI");
            Path snapshotRoot = resolveSnapshotRoot(snapshotBase, config.snapshotVersion(), config.snapshotLocale());
            return snapshotPathsForLocale(snapshotRoot.resolve("data").resolve(config.snapshotLocale()));
        }

        Path input = resolveBasePath(config.snapshotInputUri(), "SNAPSHOT_INPUT");
        if (Files.isRegularFile(input)) {
            if (!"champion.json".equals(input.getFileName().toString())) {
                throw new IllegalArgumentException("SNAPSHOT_INPUT file must be champion.json.");
            }
            return snapshotPathsForLocale(input.getParent());
        }
        if (Files.exists(input.resolve("champion.json"))) {
            return snapshotPathsForLocale(input);
        }
        if (isSnapshotRoot(input, config.snapshotLocale())) {
            return snapshotPathsForLocale(input.resolve("data").resolve(config.snapshotLocale()));
        }
        throw new IllegalArgumentException(
                "SNAPSHOT_INPUT must be a snapshot root, locale directory, or champion.json file: " + input);
    }

    private static SnapshotPaths snapshotPathsForLocale(Path localeDirectory) {
        return new SnapshotPaths(localeDirectory.resolve("champion.json"), localeDirectory.resolve("champion"));
    }

    private static boolean isSnapshotRoot(Path root, String snapshotLocale) {
        Path localeDirectory = root.resolve("data").resolve(snapshotLocale);
        return Files.exists(localeDirectory.resolve("champion.json"))
                && Files.isDirectory(localeDirectory.resolve("champion"));
    }

    private static Path resolveArtifactPath(Path artifactsBase, String artifactName, String artifactVersion, String extension) {
        return artifactsBase
                .resolve("ddragon")
                .resolve("artifacts")
                .resolve(artifactName)
                .resolve(artifactVersion + "." + extension);
    }

    private static ArtifactPaths resolveArtifactPaths(ArtifactConfig config) {
        if (config.outputDirectoryUri() != null) {
            Path outputDirectory = resolveBasePath(config.outputDirectoryUri(), "OUTPUT_DIRECTORY");
            return new ArtifactPaths(
                    outputDirectory.resolve("champion-mapping.json"),
                    outputDirectory.resolve("champion-core.csv"),
                    outputDirectory.resolve("champion-spells.csv")
            );
        }
        Path artifactsBase = resolveBasePath(config.artifactsBaseUri(), "ARTIFACTS_BASE_URI");
        return new ArtifactPaths(
                resolveArtifactPath(artifactsBase, "champion-mapping", config.artifactVersion(), "json"),
                resolveArtifactPath(artifactsBase, "champion-core", config.artifactVersion(), "csv"),
                resolveArtifactPath(artifactsBase, "champion-spells", config.artifactVersion(), "csv")
        );
    }

    private static void writeAtomically(Path outputPath, String content) throws IOException {
        Files.createDirectories(outputPath.toAbsolutePath().getParent());
        Path temporaryPath = Files.createTempFile(outputPath.toAbsolutePath().getParent(),
                "." + outputPath.getFileName(), ".tmp");
        try {
            Files.writeString(temporaryPath, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryPath, outputPath, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exc) {
                Files.move(temporaryPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryPath);
        }
    }

    private static ArtifactMetadata describeArtifact(String name, Path path) throws IOException {
        Path absolutePath = path.toAbsolutePath().normalize();
        return new ArtifactMetadata(name, absolutePath.toString(), sha256(absolutePath), Files.size(absolutePath));
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
        } catch (NoSuchAlgorithmException exc) {
            throw new IllegalStateException("SHA-256 is not available.", exc);
        }
    }

    private static String deterministicJson(Object value) throws IOException {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\n"));
        return MAPPER.writer(prettyPrinter).writeValueAsString(value);
    }

    private static List<JsonNode> readChampionPayloads(Path championDirectory) throws IOException {
        try (Stream<Path> paths = Files.list(championDirectory)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .map(ArtifactBuilderApp::readJson)
                    .toList();
        }
    }

    private static JsonNode readJson(Path path) {
        try {
            return MAPPER.readTree(path.toFile());
        } catch (IOException exc) {
            throw new IllegalArgumentException("Failed to read champion payload: " + path, exc);
        }
    }

    private record SnapshotPaths(Path championIndex, Path championDirectory) {
    }

    private record ArtifactPaths(Path mapping, Path core, Path spells) {
    }

    private record ArtifactMetadata(String name, String path, String sha256, long sizeBytes) {
    }

    private record BuildMetadata(
            String snapshotVersion,
            String snapshotLocale,
            String artifactVersion,
            List<ArtifactMetadata> artifacts
    ) {
    }

    private record StructuredResult(String status, BuildMetadata metadata) {
    }
}
