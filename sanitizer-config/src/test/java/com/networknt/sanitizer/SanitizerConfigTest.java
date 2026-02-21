package com.networknt.sanitizer;

import org.junit.jupiter.api.Test;

public class SanitizerConfigTest {
    @Test
    public void testLoad() {
        SanitizerConfig config = SanitizerConfig.load();
        assert(config != null);
    }
}
