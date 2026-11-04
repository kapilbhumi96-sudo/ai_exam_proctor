package com.proctor.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatTest {

    @Test
    void formatsMinutesSecondsAndTenths() {
        assertEquals("00:00.0", TimeFormat.mmss(0));
        assertEquals("01:05.3", TimeFormat.mmss(65_300));
        assertEquals("10:00.0", TimeFormat.mmss(600_000));
    }

    @Test
    void negativeTimesShowAsZero() {
        assertEquals("00:00.0", TimeFormat.mmss(-500));
    }
}
