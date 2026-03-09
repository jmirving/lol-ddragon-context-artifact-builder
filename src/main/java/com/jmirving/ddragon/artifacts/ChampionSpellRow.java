package com.jmirving.ddragon.artifacts;

public record ChampionSpellRow(
        String normalizedName,
        String championName,
        String championId,
        String spellSlot,
        String spellId,
        String spellName,
        String maxRank,
        String cooldown,
        String cost,
        String range,
        String costType,
        String resource
) {
}
