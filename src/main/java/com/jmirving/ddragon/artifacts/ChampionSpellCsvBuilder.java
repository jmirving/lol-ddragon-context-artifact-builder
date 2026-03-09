package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class ChampionSpellCsvBuilder {
    private static final List<String> HEADERS = List.of(
            "normalized_name",
            "champion_name",
            "champion_id",
            "spell_slot",
            "spell_id",
            "spell_name",
            "maxrank",
            "cooldown",
            "cost",
            "range",
            "cost_type",
            "resource"
    );

    private static final List<String> SPELL_SLOTS = List.of("Q", "W", "E", "R");

    private ChampionSpellCsvBuilder() {
    }

    public static List<String> headers() {
        return HEADERS;
    }

    public static List<ChampionSpellRow> build(List<JsonNode> payloads) {
        List<ChampionSpellRow> rows = new ArrayList<>();
        for (JsonNode payload : payloads) {
            rows.addAll(buildRows(payload));
        }
        rows.sort((left, right) -> {
            int byChampion = left.normalizedName().compareTo(right.normalizedName());
            if (byChampion != 0) {
                return byChampion;
            }
            return Integer.compare(slotOrder(left.spellSlot()), slotOrder(right.spellSlot()));
        });
        return rows;
    }

    public static List<List<String>> toCsvRows(List<ChampionSpellRow> rows) {
        return rows.stream()
                .map(row -> List.of(
                        row.normalizedName(),
                        row.championName(),
                        row.championId(),
                        row.spellSlot(),
                        row.spellId(),
                        row.spellName(),
                        row.maxRank(),
                        row.cooldown(),
                        row.cost(),
                        row.range(),
                        row.costType(),
                        row.resource()
                ))
                .toList();
    }

    static List<ChampionSpellRow> buildRows(JsonNode payload) {
        JsonNode champion = ChampionJsonExtractor.extractChampionNode(payload);
        JsonNode spells = champion.path("spells");
        if (!spells.isArray()) {
            throw new IllegalArgumentException("Champion payload is missing spells.");
        }

        String championName = requiredText(champion, "name");
        String championId = requiredText(champion, "id");
        String normalizedName = NameNormalizer.normalize(championName);
        List<ChampionSpellRow> rows = new ArrayList<>();
        for (int index = 0; index < spells.size(); index++) {
            JsonNode spell = spells.get(index);
            String spellSlot = index < SPELL_SLOTS.size() ? SPELL_SLOTS.get(index) : "SPELL_" + index;
            rows.add(new ChampionSpellRow(
                    normalizedName,
                    championName,
                    championId,
                    spellSlot,
                    requiredText(spell, "id"),
                    requiredText(spell, "name"),
                    text(spell.path("maxrank")),
                    CsvWriter.joinValues(spell.path("cooldown")),
                    CsvWriter.joinValues(spell.path("cost")),
                    CsvWriter.joinValues(spell.path("range")),
                    text(spell.path("costType")),
                    text(spell.path("resource"))
            ));
        }
        return rows;
    }

    private static String requiredText(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Spell payload missing field: " + fieldName);
        }
        String text = value.asText();
        if (text.isBlank()) {
            throw new IllegalArgumentException("Spell field is blank: " + fieldName);
        }
        return text;
    }

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText();
    }

    private static int slotOrder(String spellSlot) {
        int order = SPELL_SLOTS.indexOf(spellSlot);
        return order >= 0 ? order : Integer.MAX_VALUE;
    }
}
