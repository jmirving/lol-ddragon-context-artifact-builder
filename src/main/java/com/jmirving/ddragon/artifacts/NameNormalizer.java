package com.jmirving.ddragon.artifacts;

import java.text.Normalizer;
import java.util.Locale;

public final class NameNormalizer {
    private NameNormalizer() {
    }

    public static String normalize(String name) {
        String decomposed = Normalizer.normalize(name, Normalizer.Form.NFKD);
        StringBuilder stripped = new StringBuilder();
        for (int i = 0; i < decomposed.length(); i++) {
            char ch = decomposed.charAt(i);
            if (Character.getType(ch) != Character.NON_SPACING_MARK) {
                stripped.append(ch);
            }
        }

        String lowered = stripped.toString().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < lowered.length(); i++) {
            char ch = lowered.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                normalized.append(ch);
            }
        }
        return normalized.toString();
    }
}
