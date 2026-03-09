package com.jmirving.ddragon.artifacts;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvWriterTest {
    @Test
    void escapesQuotedValues() {
        String csv = CsvWriter.write(
                List.of("name", "value"),
                List.of(List.of("A, B", "\"quoted\""))
        );

        assertEquals("name,value" + System.lineSeparator()
                + "\"A, B\",\"\"\"quoted\"\"\"" + System.lineSeparator(), csv);
    }
}
