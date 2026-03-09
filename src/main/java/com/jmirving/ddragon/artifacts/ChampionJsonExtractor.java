package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Map;

public final class ChampionJsonExtractor {
    private ChampionJsonExtractor() {
    }

    public static JsonNode extractChampionNode(JsonNode payload) {
        JsonNode data = payload.path("data");
        if (!data.isObject()) {
            throw new IllegalArgumentException("Champion payload is missing a data object.");
        }

        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        if (!fields.hasNext()) {
            throw new IllegalArgumentException("Champion payload contains no champion entries.");
        }

        JsonNode champion = fields.next().getValue();
        if (fields.hasNext()) {
            throw new IllegalArgumentException("Champion payload contains multiple champion entries.");
        }
        return champion;
    }
}
