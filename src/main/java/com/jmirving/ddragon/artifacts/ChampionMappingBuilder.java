package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChampionMappingBuilder {
    private ChampionMappingBuilder() {
    }

    public static List<ChampionMappingEntry> build(JsonNode root) {
        JsonNode dataNode = root.path("data");
        if (!dataNode.isObject()) {
            throw new IllegalArgumentException("Champion payload is missing a data object.");
        }

        List<ChampionMappingEntry> entries = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = dataNode.fields();
        while (fields.hasNext()) {
            JsonNode champNode = fields.next().getValue();
            String name = requiredText(champNode, "name");
            String id = requiredText(champNode, "id");
            String key = requiredText(champNode, "key");
            String normalized = NameNormalizer.normalize(name);
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("Normalized name is blank for champion: " + name);
            }
            entries.add(new ChampionMappingEntry(normalized, name, id, key));
        }

        entries.sort(Comparator.comparing(ChampionMappingEntry::normalizedName));
        validateUnique(entries);
        return entries;
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Champion entry missing field: " + fieldName);
        }
        String text = value.asText();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Champion field is blank: " + fieldName);
        }
        return text;
    }

    private static void validateUnique(List<ChampionMappingEntry> entries) {
        Set<String> seen = new HashSet<>();
        for (ChampionMappingEntry entry : entries) {
            if (!seen.add(entry.normalizedName())) {
                throw new IllegalArgumentException(
                        "Duplicate normalized_name detected: " + entry.normalizedName()
                );
            }
        }
    }
}
