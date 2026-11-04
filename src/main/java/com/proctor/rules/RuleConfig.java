package com.proctor.rules;

/**
 * Thresholds and timing for the rules. All times are in milliseconds, angles in degrees.
 *
 * @param noFaceGraceMs         no face for this long before NO_FACE starts
 * @param multipleFacesGraceMs  two or more faces for this long before MULTIPLE_FACES starts
 * @param lookAwayGraceMs       head turned past a limit for this long before LOOKING_AWAY starts
 * @param recoveryMs            the condition must be gone this long before an event ends
 *                              (stops one event from splitting into many when the signal flickers)
 * @param yawLimitDeg           |yaw| above this counts as looking away
 * @param pitchLimitDeg         |pitch| above this counts as looking away
 */
public record RuleConfig(long noFaceGraceMs, long multipleFacesGraceMs, long lookAwayGraceMs,
                         long recoveryMs, double yawLimitDeg, double pitchLimitDeg) {

    public RuleConfig {
        if (noFaceGraceMs < 0 || multipleFacesGraceMs < 0 || lookAwayGraceMs < 0 || recoveryMs < 0) {
            throw new IllegalArgumentException("times must be >= 0");
        }
        if (yawLimitDeg <= 0 || pitchLimitDeg <= 0) {
            throw new IllegalArgumentException("angle limits must be > 0");
        }
    }

    /** Starting values. Tune them with your own measurements (docs/week-4-guide.md, step 5). */
    public static RuleConfig defaults() {
        return new RuleConfig(3000, 1500, 2000, 500, 25.0, 20.0);
    }
}
