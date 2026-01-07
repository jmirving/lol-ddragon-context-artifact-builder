package com.jmirving.ddragon.artifacts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameNormalizerTest {
    @Test
    void normalizesPunctuationAndSpaces() {
        assertEquals("chogath", NameNormalizer.normalize("Cho'Gath"));
        assertEquals("drmundo", NameNormalizer.normalize("Dr. Mundo"));
        assertEquals("nunuwillump", NameNormalizer.normalize("Nunu & Willump"));
    }

    @Test
    void stripsDiacritics() {
        assertEquals("cafe", NameNormalizer.normalize("Caf\u00e9"));
    }
}
