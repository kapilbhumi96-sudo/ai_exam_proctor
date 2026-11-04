package com.proctor.rules;

import com.proctor.pose.HeadPose;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.proctor.rules.SustainedCondition.Change;

/**
 * Decides, frame by frame, whether the candidate is doing something suspicious.
 *
 * Rules:
 *  - NO_FACE:        no face visible
 *  - MULTIPLE_FACES: two or more faces visible
 *  - LOOKING_AWAY:   a face is visible but |yaw| or |pitch| is past its limit
 *
 * Each rule has a grace period (so a blink or a quick glance is ignored) and a recovery period
 * (so one long event is not chopped into pieces). Call update() once per frame with the frame's
 * timestamp. Pure Java: no camera or OpenCV is needed to test it.
 */
public class RuleEngine {

    private final RuleConfig config;
    private final SustainedCondition noFace;
    private final SustainedCondition multipleFaces;
    private final SustainedCondition lookingAway;

    private int peakFaces;
    private double peakYaw;
    private double peakPitch;

    public RuleEngine(RuleConfig config) {
        this.config = config;
        this.noFace = new SustainedCondition(config.noFaceGraceMs(), config.recoveryMs());
        this.multipleFaces = new SustainedCondition(config.multipleFacesGraceMs(), config.recoveryMs());
        this.lookingAway = new SustainedCondition(config.lookAwayGraceMs(), config.recoveryMs());
    }

    /**
     * Feeds one frame.
     *
     * @param nowMillis timestamp of the frame
     * @param faceCount number of faces found
     * @param pose      head pose of the main face, or empty if there is none
     * @return events that started or ended on this frame (usually empty)
     */
    public List<EventUpdate> update(long nowMillis, int faceCount, Optional<HeadPose> pose) {
        List<EventUpdate> updates = new ArrayList<>();

        boolean looksAway = faceCount >= 1 && pose.isPresent()
                && (Math.abs(pose.get().yaw()) > config.yawLimitDeg()
                || Math.abs(pose.get().pitch()) > config.pitchLimitDeg());

        step(EventType.NO_FACE, noFace, faceCount == 0, nowMillis, faceCount, pose, updates);
        step(EventType.MULTIPLE_FACES, multipleFaces, faceCount >= 2, nowMillis, faceCount, pose, updates);
        step(EventType.LOOKING_AWAY, lookingAway, looksAway, nowMillis, faceCount, pose, updates);
        return updates;
    }

    /** Ends every active event at the given time. Call this when the session stops. */
    public List<EventUpdate> flush(long nowMillis) {
        List<EventUpdate> updates = new ArrayList<>();
        flushOne(EventType.NO_FACE, noFace, nowMillis, updates);
        flushOne(EventType.MULTIPLE_FACES, multipleFaces, nowMillis, updates);
        flushOne(EventType.LOOKING_AWAY, lookingAway, nowMillis, updates);
        return updates;
    }

    /** Events that are active right now (for on-screen alerts). */
    public EnumSet<EventType> activeTypes() {
        EnumSet<EventType> active = EnumSet.noneOf(EventType.class);
        if (noFace.isActive()) {
            active.add(EventType.NO_FACE);
        }
        if (multipleFaces.isActive()) {
            active.add(EventType.MULTIPLE_FACES);
        }
        if (lookingAway.isActive()) {
            active.add(EventType.LOOKING_AWAY);
        }
        return active;
    }

    private void step(EventType type, SustainedCondition rule, boolean condition, long now,
                      int faceCount, Optional<HeadPose> pose, List<EventUpdate> out) {
        Change change = rule.update(now, condition);

        if (change == Change.ACTIVATED) {
            resetPeaks(type);
            out.add(new EventUpdate(EventUpdate.Kind.STARTED, type, rule.activeStartMillis(), now, ""));
        }
        if (rule.isActive() && condition) {
            trackPeaks(type, faceCount, pose);
        }
        if (change == Change.DEACTIVATED) {
            out.add(new EventUpdate(EventUpdate.Kind.ENDED, type, rule.activeStartMillis(),
                    rule.lastEndMillis(), detailFor(type)));
        }
    }

    private void flushOne(EventType type, SustainedCondition rule, long now, List<EventUpdate> out) {
        long start = rule.activeStartMillis();
        if (rule.forceDeactivate(now)) {
            out.add(new EventUpdate(EventUpdate.Kind.ENDED, type, start, now, detailFor(type)));
        }
    }

    private void resetPeaks(EventType type) {
        switch (type) {
            case MULTIPLE_FACES -> peakFaces = 0;
            case LOOKING_AWAY -> {
                peakYaw = 0;
                peakPitch = 0;
            }
            default -> {
            }
        }
    }

    private void trackPeaks(EventType type, int faceCount, Optional<HeadPose> pose) {
        switch (type) {
            case MULTIPLE_FACES -> peakFaces = Math.max(peakFaces, faceCount);
            case LOOKING_AWAY -> pose.ifPresent(p -> {
                peakYaw = Math.max(peakYaw, Math.abs(p.yaw()));
                peakPitch = Math.max(peakPitch, Math.abs(p.pitch()));
            });
            default -> {
            }
        }
    }

    private String detailFor(EventType type) {
        return switch (type) {
            case MULTIPLE_FACES -> String.format(Locale.ROOT, "peak %d faces", peakFaces);
            case LOOKING_AWAY -> String.format(Locale.ROOT, "peak yaw %.0f, peak pitch %.0f", peakYaw, peakPitch);
            default -> "";
        };
    }
}
