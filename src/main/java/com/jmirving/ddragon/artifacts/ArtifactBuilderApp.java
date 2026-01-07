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
import java.util.Map;
import java.util.Set;

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
        Path snapshotPath = snapshotBase
                .resolve(config.snapshotVersion())
                .resolve("data")
                .resolve(config.snapshotLocale())
                .resolve("champion.json");

        if (!Files.exists(snapshotPath)) {
            throw new FileNotFoundException("Snapshot not found: " + snapshotPath);
        }

        Path artifactsBase = resolveBasePath(config.artifactsBaseUri(), "ARTIFACTS_BASE_URI");
        Path outputPath = artifactsBase
                .resolve("ddragon")
                .resolve("artifacts")
                .resolve("champion-mapping")
                .resolve(config.artifactVersion() + ".json");

        JsonNode payload = MAPPER.readTree(snapshotPath.toFile());
        var mapping = ChampionMappingBuilder.build(payload);

        Files.createDirectories(outputPath.getParent());
        String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(mapping);
        Files.writeString(outputPath, json + System.lineSeparator(), StandardCharsets.UTF_8);

        System.out.println("Wrote champion mapping to " + outputPath);
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
}
