package com.jmirving.ddragon.artifacts;

public record ArtifactConfig(
        String snapshotBaseUri,
        String snapshotInputUri,
        String snapshotVersion,
        String snapshotLocale,
        String artifactsBaseUri,
        String outputDirectoryUri,
        String artifactVersion,
        String structuredOutput
) {
}
