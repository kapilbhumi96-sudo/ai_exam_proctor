package com.proctor.util;

import java.util.Locale;

/** Small helpers for showing times. */
public final class TimeFormat {

    private TimeFormat() {
    }

    /** Formats milliseconds as mm:ss.t, for example 65300 becomes 01:05.3. */
    public static String mmss(long millis) {
        long clamped = Math.max(0, millis);
        long minutes = clamped / 60_000;
        long seconds = (clamped / 1000) % 60;
        long tenths = (clamped / 100) % 10;
        return String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, tenths);
    }
}
