package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public final class ChampionCoreCsvBuilder {
    private static final List<String> HEADERS = List.of(
            "normalized_name",
            "name",
            "id",
            "key",
            "tags",
            "partype",
            "info_attack",
            "info_defense",
            "info_magic",
            "info_difficulty",
            "hp",
            "hpperlevel",
            "mp",
            "mpperlevel",
            "movespeed",
            "armor",
            "armorperlevel",
            "spellblock",
            "spellblockperlevel",
            "attackrange",
            "hpregen",
            "hpregenperlevel",
            "mpregen",
            "mpregenperlevel",
            "crit",
            "critperlevel",
            "attackdamage",
            "attackdamageperlevel",
            "attackspeedperlevel",
            "attackspeed"
    );

    private ChampionCoreCsvBuilder() {
    }

    public static List<String> headers() {
        return HEADERS;
    }

    public static List<ChampionCoreRow> build(List<JsonNode> payloads) {
        return payloads.stream()
                .map(ChampionCoreCsvBuilder::buildRow)
                .sorted((left, right) -> left.normalizedName().compareTo(right.normalizedName()))
                .toList();
    }

    public static List<List<String>> toCsvRows(List<ChampionCoreRow> rows) {
        return rows.stream()
                .map(row -> List.of(
                        row.normalizedName(),
                        row.name(),
                        row.id(),
                        row.key(),
                        row.tags(),
                        row.partype(),
                        row.infoAttack(),
                        row.infoDefense(),
                        row.infoMagic(),
                        row.infoDifficulty(),
                        row.hp(),
                        row.hpPerLevel(),
                        row.mp(),
                        row.mpPerLevel(),
                        row.moveSpeed(),
                        row.armor(),
                        row.armorPerLevel(),
                        row.spellBlock(),
                        row.spellBlockPerLevel(),
                        row.attackRange(),
                        row.hpRegen(),
                        row.hpRegenPerLevel(),
                        row.mpRegen(),
                        row.mpRegenPerLevel(),
                        row.crit(),
                        row.critPerLevel(),
                        row.attackDamage(),
                        row.attackDamagePerLevel(),
                        row.attackSpeedPerLevel(),
                        row.attackSpeed()
                ))
                .toList();
    }

    static ChampionCoreRow buildRow(JsonNode payload) {
        JsonNode champion = ChampionJsonExtractor.extractChampionNode(payload);
        JsonNode info = champion.path("info");
        JsonNode stats = champion.path("stats");
        String name = requiredText(champion, "name");

        return new ChampionCoreRow(
                NameNormalizer.normalize(name),
                name,
                requiredText(champion, "id"),
                requiredText(champion, "key"),
                CsvWriter.joinValues(champion.path("tags")),
                text(champion.path("partype")),
                text(info.path("attack")),
                text(info.path("defense")),
                text(info.path("magic")),
                text(info.path("difficulty")),
                text(stats.path("hp")),
                text(stats.path("hpperlevel")),
                text(stats.path("mp")),
                text(stats.path("mpperlevel")),
                text(stats.path("movespeed")),
                text(stats.path("armor")),
                text(stats.path("armorperlevel")),
                text(stats.path("spellblock")),
                text(stats.path("spellblockperlevel")),
                text(stats.path("attackrange")),
                text(stats.path("hpregen")),
                text(stats.path("hpregenperlevel")),
                text(stats.path("mpregen")),
                text(stats.path("mpregenperlevel")),
                text(stats.path("crit")),
                text(stats.path("critperlevel")),
                text(stats.path("attackdamage")),
                text(stats.path("attackdamageperlevel")),
                text(stats.path("attackspeedperlevel")),
                text(stats.path("attackspeed"))
        );
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

    private static String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        return node.asText();
    }
}
