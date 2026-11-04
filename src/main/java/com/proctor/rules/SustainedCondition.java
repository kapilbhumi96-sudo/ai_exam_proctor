package com.proctor.rules;

/**
 * Turns a flickering true/false signal into a stable on/off state.
 *
 *  - It becomes ACTIVE only after the signal has been true without a break for activateAfterMs.
 *  - Once active, it becomes INACTIVE only after the signal has been false without a break for
 *    deactivateAfterMs.
 *
 * Time is passed in with every call, so it is easy to test. Pure Java.
 */
public class SustainedCondition {

    public enum Change {
        NONE,
        ACTIVATED,
        DEACTIVATED
    }

    private final long activateAfterMs;
    private final long deactivateAfterMs;

    private boolean active;
    private long trueSince = -1;
    private long falseSince = -1;
    private long activeStart = -1;
    private long lastEnd = -1;

    public SustainedCondition(long activateAfterMs, long deactivateAfterMs) {
        if (activateAfterMs < 0 || deactivateAfterMs < 0) {
            throw new IllegalArgumentException("times must be >= 0");
        }
        this.activateAfterMs = activateAfterMs;
        this.deactivateAfterMs = deactivateAfterMs;
    }

    public Change update(long nowMillis, boolean conditionTrue) {
        if (conditionTrue) {
            falseSince = -1;
            if (trueSince < 0) {
                trueSince = nowMillis;
            }
            if (!active && nowMillis - trueSince >= activateAfterMs) {
                active = true;
                activeStart = trueSince;
                return Change.ACTIVATED;
            }
            return Change.NONE;
        }

        if (!active) {
            trueSince = -1;                  // the run broke before it counted
            return Change.NONE;
        }
        if (falseSince < 0) {
            falseSince = nowMillis;
        }
        if (nowMillis - falseSince >= deactivateAfterMs) {
            active = false;
            lastEnd = falseSince;
            trueSince = -1;
            falseSince = -1;
            return Change.DEACTIVATED;
        }
        return Change.NONE;
    }

    /** Ends an active state right now (used when the session stops). Returns true if it was active. */
    public boolean forceDeactivate(long nowMillis) {
        if (!active) {
            return false;
        }
        active = false;
        lastEnd = nowMillis;
        trueSince = -1;
        falseSince = -1;
        return true;
    }

    public boolean isActive() {
        return active;
    }

    /** When the signal first became true, for the most recent activation. */
    public long activeStartMillis() {
        return activeStart;
    }

    /** When the signal stopped being true, for the most recent deactivation. */
    public long lastEndMillis() {
        return lastEnd;
    }
}
