package com.proctor.rules;

/**
 * A change reported by the rule engine.
 *
 * STARTED: the condition has lasted long enough to count as an event. startMillis is when the
 *          condition actually began (before the grace period), atMillis is when the alert fires.
 * ENDED:   the condition is over. atMillis is the moment it stopped; detail has peak values.
 *
 * All times are milliseconds on the same clock as the frames that were fed to the engine.
 */
public record EventUpdate(Kind kind, EventType type, long startMillis, long atMillis, String detail) {

    public enum Kind {
        STARTED,
        ENDED
    }

    /** For ENDED updates: how long the event lasted. */
    public long durationMillis() {
        return atMillis - startMillis;
    }
}
