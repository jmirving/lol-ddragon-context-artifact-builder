package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtifactBuilderAppTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesStructuredMetadataForDirectInputAndOutputDirectories() throws Exception {
        Path snapshotRoot = createSnapshot();
        Path outputDirectory = temporaryDirectory.resolve("worker-output");
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();

        ArtifactBuilderApp.run(new String[]{
                "--snapshot-input", snapshotRoot.toString(),
                "--snapshot-version", "14.1.1",
                "--snapshot-locale", "en_US",
                "--output-directory", outputDirectory.toString(),
                "--artifact-version", "14.1.1",
                "--structured-output", "json"
        }, new PrintStream(stdout, true, StandardCharsets.UTF_8));

        JsonNode envelope = MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));
        assertEquals("SUCCESS", envelope.path("status").asText());
        JsonNode metadata = envelope.path("metadata");
        assertEquals("14.1.1", metadata.path("snapshotVersion").asText());
        assertEquals("en_US", metadata.path("snapshotLocale").asText());
        assertEquals("14.1.1", metadata.path("artifactVersion").asText());
        assertEquals(3, metadata.path("artifacts").size());

        for (JsonNode artifact : metadata.path("artifacts")) {
            Path path = Path.of(artifact.path("path").asText());
            assertTrue(path.startsWith(outputDirectory.toAbsolutePath()));
            assertTrue(Files.isRegularFile(path));
            assertEquals(Files.size(path), artifact.path("sizeBytes").asLong());
            assertEquals(sha256(path), artifact.path("sha256").asText());
        }
    }

    @Test
    void repeatedExecutionProducesIdenticalArtifacts() throws Exception {
        Path snapshotRoot = createSnapshot();
        Path outputDirectory = temporaryDirectory.resolve("repeatable-output");
        String[] args = {
                "--snapshot-input", snapshotRoot.resolve("data/en_US/champion.json").toUri().toString(),
                "--snapshot-version", "14.1.1",
                "--output-directory", outputDirectory.toUri().toString()
        };

        ArtifactBuilderApp.run(args, new PrintStream(new ByteArrayOutputStream()));
        byte[] firstMapping = Files.readAllBytes(outputDirectory.resolve("champion-mapping.json"));
        byte[] firstCore = Files.readAllBytes(outputDirectory.resolve("champion-core.csv"));
        byte[] firstSpells = Files.readAllBytes(outputDirectory.resolve("champion-spells.csv"));

        ArtifactBuilderApp.run(args, new PrintStream(new ByteArrayOutputStream()));

        assertArrayEquals(firstMapping, Files.readAllBytes(outputDirectory.resolve("champion-mapping.json")));
        assertArrayEquals(firstCore, Files.readAllBytes(outputDirectory.resolve("champion-core.csv")));
        assertArrayEquals(firstSpells, Files.readAllBytes(outputDirectory.resolve("champion-spells.csv")));
    }

    @Test
    void preservesStandaloneBaseUriLayout() throws Exception {
        Path snapshotBase = temporaryDirectory.resolve("legacy-input");
        Files.createDirectories(snapshotBase);
        Files.move(createSnapshot(), snapshotBase.resolve("14.1.1"));
        Path artifactsBase = temporaryDirectory.resolve("legacy-output");

        ArtifactBuilderApp.run(new String[]{
                "--snapshot-base-uri", snapshotBase.toString(),
                "--snapshot-version", "14.1.1",
                "--artifacts-base-uri", artifactsBase.toString()
        }, new PrintStream(new ByteArrayOutputStream()));

        assertTrue(Files.isRegularFile(
                artifactsBase.resolve("ddragon/artifacts/champion-mapping/latest.json")));
        assertTrue(Files.isRegularFile(
                artifactsBase.resolve("ddragon/artifacts/champion-core/latest.csv")));
        assertTrue(Files.isRegularFile(
                artifactsBase.resolve("ddragon/artifacts/champion-spells/latest.csv")));
    }

    private Path createSnapshot() throws Exception {
        Path localeDirectory = temporaryDirectory.resolve("snapshot/data/en_US");
        Path championDirectory = localeDirectory.resolve("champion");
        Files.createDirectories(championDirectory);
        Files.writeString(localeDirectory.resolve("champion.json"), """
                {"data":{"Ahri":{"name":"Ahri","id":"Ahri","key":"103"}}}
                """, StandardCharsets.UTF_8);
        Files.writeString(championDirectory.resolve("Ahri.json"), """
                {
                  "data": {
                    "Ahri": {
                      "id": "Ahri", "key": "103", "name": "Ahri",
                      "tags": ["Mage"], "partype": "Mana",
                      "info": {"attack": 3},
                      "stats": {"hp": 590},
                      "spells": [
                        {"id":"AhriQ","name":"Orb","maxrank":5,"cooldown":[7],"cost":[55],"range":[970]}
                      ]
                    }
                  }
                }
                """, StandardCharsets.UTF_8);
        return temporaryDirectory.resolve("snapshot");
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(Files.readAllBytes(path)));
    }
}
