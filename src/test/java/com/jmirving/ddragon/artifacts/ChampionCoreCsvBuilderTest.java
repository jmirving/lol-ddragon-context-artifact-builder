package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChampionCoreCsvBuilderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildsChampionCoreRows() throws Exception {
        String payload = """
                {
                  "data": {
                    "Ahri": {
                      "id": "Ahri",
                      "key": "103",
                      "name": "Ahri",
                      "tags": ["Mage", "Assassin"],
                      "partype": "Mana",
                      "info": {"attack": 3, "defense": 4, "magic": 8, "difficulty": 5},
                      "stats": {
                        "hp": 590,
                        "hpperlevel": 104,
                        "mp": 418,
                        "mpperlevel": 25,
                        "movespeed": 330,
                        "armor": 21,
                        "armorperlevel": 4.2,
                        "spellblock": 30,
                        "spellblockperlevel": 1.3,
                        "attackrange": 550,
                        "hpregen": 2.5,
                        "hpregenperlevel": 0.6,
                        "mpregen": 8,
                        "mpregenperlevel": 0.8,
                        "crit": 0,
                        "critperlevel": 0,
                        "attackdamage": 53,
                        "attackdamageperlevel": 3,
                        "attackspeedperlevel": 2.2,
                        "attackspeed": 0.668
                      }
                    }
                  }
                }
                """;

        List<ChampionCoreRow> rows = ChampionCoreCsvBuilder.build(List.of(mapper.readTree(payload)));

        assertEquals(1, rows.size());
        ChampionCoreRow row = rows.get(0);
        assertEquals("ahri", row.normalizedName());
        assertEquals("Mage|Assassin", row.tags());
        assertEquals("Mana", row.partype());
        assertEquals("590", row.hp());
        assertEquals("0.668", row.attackSpeed());
    }
}
