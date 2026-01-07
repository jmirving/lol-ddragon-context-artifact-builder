package com.jmirving.ddragon.artifacts;

public record ArtifactConfig(
        String snapshotBaseUri,
        String snapshotVersion,
        String snapshotLocale,
        String artifactsBaseUri,
        String artifactVersion
) {
}
