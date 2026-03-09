package com.jmirving.ddragon.artifacts;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.List;

public final class CsvWriter {
    private CsvWriter() {
    }

    public static String write(List<String> headers, List<List<String>> rows) {
        StringBuilder builder = new StringBuilder();
        appendRow(builder, headers);
        for (List<String> row : rows) {
            appendRow(builder, row);
        }
        return builder.toString();
    }

    public static String joinValues(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        Iterator<JsonNode> iterator = arrayNode.elements();
        while (iterator.hasNext()) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(iterator.next().asText());
        }
        return builder.toString();
    }

    private static void appendRow(StringBuilder builder, List<String> row) {
        for (int index = 0; index < row.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(escape(row.get(index)));
        }
        builder.append(System.lineSeparator());
    }

    private static String escape(String value) {
        String safeValue = value == null ? "" : value;
        boolean needsQuotes = safeValue.contains(",")
                || safeValue.contains("\"")
                || safeValue.contains("\n")
                || safeValue.contains("\r");
        if (!needsQuotes) {
            return safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
