package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"normalized_name", "name", "id", "key"})
public record ChampionMappingEntry(
        @JsonProperty("normalized_name") String normalizedName,
        String name,
        String id,
        String key
) {
}
