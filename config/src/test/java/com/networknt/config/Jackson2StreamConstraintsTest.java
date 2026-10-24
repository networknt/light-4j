package com.networknt.config;

import java.io.StringWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class Jackson2StreamConstraintsTest {
    private List<ObjectMapper> mappers() {
        return List.of(Jackson2Defaults.mapper(), Config.getInstance().getMapper(), JsonMapper.objectMapper);
    }

    @Test
    void retainsAllMeasuredReadAndWriteConstraints() {
        for (var mapper : mappers()) {
            var factory = mapper.tokenStreamFactory();
            var read = factory.streamReadConstraints();
            assertEquals(1000, read.getMaxNestingDepth());
            assertEquals(-1, read.getMaxDocumentLength());
            assertEquals(-1, read.getMaxTokenCount());
            assertEquals(1000, read.getMaxNumberLength());
            assertEquals(20_000_000, read.getMaxStringLength());
            assertEquals(50_000, read.getMaxNameLength());
            assertEquals(1000, factory.streamWriteConstraints().getMaxNestingDepth());
            assertDoesNotThrow(() -> read.validateStringLength(20_000_000));
            assertThrows(StreamConstraintsException.class, () -> read.validateStringLength(20_000_001));
        }
    }

    @Test
    void readsUpToLegacyDepthAndRejectsBeyondIt() {
        String accepted = "[".repeat(1000) + "0" + "]".repeat(1000);
        String rejected = "[".repeat(1001) + "0" + "]".repeat(1001);
        for (var mapper : mappers()) {
            assertDoesNotThrow(() -> mapper.readTree(accepted));
            assertThrows(StreamConstraintsException.class, () -> mapper.readTree(rejected));
            String nested = "[".repeat(600) + "0" + "]".repeat(600);
            assertEquals(nested, mapper.writeValueAsString(mapper.readTree(nested)));
        }
    }

    @Test
    void enforcesLegacyWriteDepthAtGeneratorBoundary() {
        for (var mapper : mappers()) {
            try (var generator = mapper.createGenerator(new StringWriter())) {
                for (int i = 0; i < 1000; i++) generator.writeStartArray();
                assertThrows(StreamConstraintsException.class, generator::writeStartArray);
            }
        }
    }

    @Test
    void rejectsActualStringInputAboveLegacyLimit() {
        String document = "\"" + "x".repeat(20_100_000) + "\"";
        ObjectMapper mapper = Jackson2Defaults.mapper();
        assertThrows(StreamConstraintsException.class, () -> mapper.readTree(document));
    }

    @Test
    void sharedDefaultMapperIsReused() {
        assertSame(Jackson2Defaults.mapper(), Jackson2Defaults.mapper());
    }
}
