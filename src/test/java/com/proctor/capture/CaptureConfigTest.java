package com.proctor.capture;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CaptureConfigTest {

    @Test
    void defaultsAreValid() {
        assertDoesNotThrow(CaptureConfig::defaults);
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfig(-1, 640, 480, 15, 5));
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfig(0, 0, 480, 15, 5));
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfig(0, 640, 480, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> new CaptureConfig(0, 640, 480, 15, 0));
    }
}
