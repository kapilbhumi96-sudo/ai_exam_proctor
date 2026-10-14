package com.proctor.util;

/** Measures frames per second, updated once per second. Not thread-safe; use from one thread. */
public class FpsMeter {

    private long windowStartNanos = System.nanoTime();
    private int count;
    private double fps;

    public void tick() {
        count++;
        long now = System.nanoTime();
        long elapsed = now - windowStartNanos;
        if (elapsed >= 1_000_000_000L) {
            fps = count * 1_000_000_000.0 / elapsed;
            count = 0;
            windowStartNanos = now;
        }
    }

    public double fps() {
        return fps;
    }
}
