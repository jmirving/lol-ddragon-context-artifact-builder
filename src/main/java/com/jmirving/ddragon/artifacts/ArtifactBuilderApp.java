package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            "snapshot-version",
            "snapshot-locale",
            "artifacts-base-uri",
            "artifact-version"
    );

    private ArtifactBuilderApp() {
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch (Exception exc) {
            System.err.println("Artifact build failed: " + exc.getMessage());
            System.exit(1);
        }
    }

    static void run(String[] args) throws IOException {
        Map<String, String> parsedArgs = parseArgs(args);
        ArtifactConfig config = buildConfig(parsedArgs);

        Path snapshotBase = resolveBasePath(config.snapshotBaseUri(), "SNAPSHOT_BASE_URI");
        Path snapshotRoot = resolveSnapshotRoot(snapshotBase, config.snapshotVersion(), config.snapshotLocale());
        Path snapshotPath = snapshotRoot
                .resolve("data")
                .resolve(config.snapshotLocale())
                .resolve("champion.json");
        Path championDirectory = snapshotRoot
                .resolve("data")
                .resolve(config.snapshotLocale())
                .resolve("champion");

        if (!Files.exists(snapshotPath)) {
            throw new FileNotFoundException("Snapshot not found: " + snapshotPath);
        }
        if (!Files.isDirectory(championDirectory)) {
            throw new FileNotFoundException("Champion directory not found: " + championDirectory);
        }

        Path artifactsBase = resolveBasePath(config.artifactsBaseUri(), "ARTIFACTS_BASE_URI");
        Path mappingOutputPath = resolveArtifactPath(artifactsBase, "champion-mapping", config.artifactVersion(), "json");
        Path coreOutputPath = resolveArtifactPath(artifactsBase, "champion-core", config.artifactVersion(), "csv");
        Path spellsOutputPath = resolveArtifactPath(artifactsBase, "champion-spells", config.artifactVersion(), "csv");

        JsonNode payload = MAPPER.readTree(snapshotPath.toFile());
        var mapping = ChampionMappingBuilder.build(payload);
        List<JsonNode> championPayloads = readChampionPayloads(championDirectory);
        var coreRows = ChampionCoreCsvBuilder.build(championPayloads);
        var spellRows = ChampionSpellCsvBuilder.build(championPayloads);

        Files.createDirectories(mappingOutputPath.getParent());
        Files.createDirectories(coreOutputPath.getParent());
        Files.createDirectories(spellsOutputPath.getParent());
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(mapping);
        Files.writeString(mappingOutputPath, json + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.writeString(coreOutputPath,
                CsvWriter.write(ChampionCoreCsvBuilder.headers(), ChampionCoreCsvBuilder.toCsvRows(coreRows)),
                StandardCharsets.UTF_8);
        Files.writeString(spellsOutputPath,
                CsvWriter.write(ChampionSpellCsvBuilder.headers(), ChampionSpellCsvBuilder.toCsvRows(spellRows)),
                StandardCharsets.UTF_8);

        System.out.println("Wrote champion mapping to " + mappingOutputPath);
        System.out.println("Wrote champion core CSV to " + coreOutputPath);
        System.out.println("Wrote champion spells CSV to " + spellsOutputPath);
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
        String artifactVersion = pickValue(args.get("artifact-version"),
                System.getenv("ARTIFACT_VERSION"),
                "latest");

        return new ArtifactConfig(snapshotBase, snapshotVersion, snapshotLocale, artifactsBase, artifactVersion);
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
        URI uri = URI.create(uriValue);
        if (uri.getScheme() == null) {
            return Paths.get(uriValue);
        }
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return Paths.get(uri);
        }
        throw new IllegalArgumentException(envLabel + " must be a local path or file:// URI.");
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
}
