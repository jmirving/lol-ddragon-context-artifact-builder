package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChampionMappingBuilderTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void buildsSortedMapping() throws Exception {
        String payload = """
                {
                  "data": {
                    "Wukong": {"name": "Wukong", "id": "MonkeyKing", "key": "62"},
                    "Ahri": {"name": "Ahri", "id": "Ahri", "key": "103"}
                  }
                }
                """;

        var mapping = ChampionMappingBuilder.build(mapper.readTree(payload));

        assertEquals(2, mapping.size());
        assertEquals("Ahri", mapping.get(0).name());
        assertEquals("Wukong", mapping.get(1).name());
        assertEquals("ahri", mapping.get(0).normalizedName());
        assertEquals("wukong", mapping.get(1).normalizedName());
    }

    @Test
    void rejectsDuplicateNormalizedNames() throws Exception {
        String payload = """
                {
                  "data": {
                    "LeBlanc": {"name": "LeBlanc", "id": "Leblanc", "key": "7"},
                    "Le Blanc": {"name": "Le Blanc", "id": "Leblanc", "key": "7"}
                  }
                }
                """;

        assertThrows(IllegalArgumentException.class,
                () -> ChampionMappingBuilder.build(mapper.readTree(payload)));
    }
}
