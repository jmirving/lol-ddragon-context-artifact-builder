package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChampionSpellCsvBuilderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildsChampionSpellRows() throws Exception {
        String payload = """
                {
                  "data": {
                    "Ahri": {
                      "id": "Ahri",
                      "name": "Ahri",
                      "spells": [
                        {"id": "AhriQ", "name": "Orb", "maxrank": 5, "cooldown": [7,7,7,7,7], "cost": [55,65,75,85,95], "range": [970,970,970,970,970], "costType": " Mana", "resource": "55 Mana"},
                        {"id": "AhriW", "name": "Fire", "maxrank": 5, "cooldown": [9,8,7,6,5], "cost": [40,40,40,40,40], "range": [700,700,700,700,700], "costType": " Mana", "resource": "40 Mana"},
                        {"id": "AhriE", "name": "Charm", "maxrank": 5, "cooldown": [12,12,12,12,12], "cost": [60,60,60,60,60], "range": [975,975,975,975,975], "costType": " Mana", "resource": "60 Mana"},
                        {"id": "AhriR", "name": "Rush", "maxrank": 3, "cooldown": [140,120,100], "cost": [100,100,100], "range": [450,450,450], "costType": " Mana", "resource": "100 Mana"}
                      ]
                    }
                  }
                }
                """;

        List<ChampionSpellRow> rows = ChampionSpellCsvBuilder.build(List.of(mapper.readTree(payload)));

        assertEquals(4, rows.size());
        assertEquals("Q", rows.get(0).spellSlot());
        assertEquals("AhriQ", rows.get(0).spellId());
        assertEquals("7|7|7|7|7", rows.get(0).cooldown());
        assertEquals("R", rows.get(3).spellSlot());
        assertEquals("140|120|100", rows.get(3).cooldown());
    }
}
