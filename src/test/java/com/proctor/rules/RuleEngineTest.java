package com.proctor.rules;

import com.proctor.pose.HeadPose;
import com.proctor.rules.EventUpdate.Kind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleEngineTest {

    private static final int STEP_MS = 100;

    /** noFace 3000, multipleFaces 1500, lookAway 2000, recovery 500, yaw limit 25, pitch limit 20. */
    private final RuleEngine engine = new RuleEngine(RuleConfig.defaults());

    private static Optional<HeadPose> pose(double yaw, double pitch) {
        return Optional.of(new HeadPose(yaw, pitch, 0));
    }

    /** Feeds one frame every 100 ms from fromMs to toMs (both included) and collects the updates. */
    private List<EventUpdate> feed(long fromMs, long toMs, int faces, Optional<HeadPose> pose) {
        List<EventUpdate> all = new ArrayList<>();
        for (long t = fromMs; t <= toMs; t += STEP_MS) {
            all.addAll(engine.update(t, faces, pose));
        }
        return all;
    }

    @Test
    void noFaceStartsOnlyAfterTheGracePeriod() {
        assertTrue(feed(0, 2900, 0, Optional.empty()).isEmpty());

        List<EventUpdate> updates = feed(3000, 3000, 0, Optional.empty());

        assertEquals(1, updates.size());
        assertEquals(Kind.STARTED, updates.get(0).kind());
        assertEquals(EventType.NO_FACE, updates.get(0).type());
        assertEquals(0, updates.get(0).startMillis());
        assertEquals(3000, updates.get(0).atMillis());
    }

    @Test
    void noFaceEndsWhenTheFaceReturnsAndReportsTheDuration() {
        feed(0, 5900, 0, Optional.empty());

        List<EventUpdate> updates = feed(6000, 6500, 1, pose(0, 0));

        assertEquals(1, updates.size());
        EventUpdate ended = updates.get(0);
        assertEquals(Kind.ENDED, ended.kind());
        assertEquals(EventType.NO_FACE, ended.type());
        assertEquals(6000, ended.durationMillis());
    }

    @Test
    void aShortAbsenceIsIgnored() {
        assertTrue(feed(0, 2000, 0, Optional.empty()).isEmpty());
        assertTrue(feed(2100, 6000, 1, pose(0, 0)).isEmpty());
    }

    @Test
    void multipleFacesStartsAfterItsGracePeriodAndReportsThePeakCount() {
        List<EventUpdate> started = feed(0, 1500, 2, pose(0, 0));
        feed(1600, 1800, 3, pose(0, 0));
        List<EventUpdate> ended = feed(1900, 2400, 1, pose(0, 0));

        assertEquals(1, started.size());
        assertEquals(EventType.MULTIPLE_FACES, started.get(0).type());
        assertEquals(1, ended.size());
        assertEquals(Kind.ENDED, ended.get(0).kind());
        assertTrue(ended.get(0).detail().contains("3"));
    }

    @Test
    void lookingAwayStartsAfterTwoSecondsPastTheYawLimit() {
        assertTrue(feed(0, 1900, 1, pose(40, 0)).isEmpty());

        List<EventUpdate> updates = feed(2000, 2000, 1, pose(40, 0));

        assertEquals(1, updates.size());
        assertEquals(EventType.LOOKING_AWAY, updates.get(0).type());
        assertEquals(Kind.STARTED, updates.get(0).kind());
        assertEquals(0, updates.get(0).startMillis());
    }

    @Test
    void lookingAwayEndsAfterRecoveryAndReportsPeakAngles() {
        feed(0, 2000, 1, pose(40, 5));
        feed(2100, 2300, 1, pose(-55, 8));

        List<EventUpdate> ended = feed(2400, 3200, 1, pose(0, 0));

        assertEquals(1, ended.size());
        assertEquals(Kind.ENDED, ended.get(0).kind());
        assertTrue(ended.get(0).detail().contains("55"));
        assertTrue(ended.get(0).detail().contains("8"));
    }

    @Test
    void smallHeadMovementsNeverTrigger() {
        assertTrue(feed(0, 20_000, 1, pose(20, 15)).isEmpty());
    }

    @Test
    void lookingDownPastThePitchLimitTriggers() {
        List<EventUpdate> updates = feed(0, 2000, 1, pose(0, 35));

        assertEquals(1, updates.size());
        assertEquals(EventType.LOOKING_AWAY, updates.get(0).type());
    }

    @Test
    void anAbsentFaceIsNotReportedAsLookingAway() {
        List<EventUpdate> updates = feed(0, 5000, 0, Optional.empty());

        assertEquals(1, updates.size());
        assertEquals(EventType.NO_FACE, updates.get(0).type());
    }

    @Test
    void activeTypesAndFlushFollowTheOpenEvents() {
        feed(0, 3000, 0, Optional.empty());
        assertTrue(engine.activeTypes().contains(EventType.NO_FACE));

        List<EventUpdate> flushed = engine.flush(4000);

        assertEquals(1, flushed.size());
        assertEquals(Kind.ENDED, flushed.get(0).kind());
        assertEquals(4000, flushed.get(0).atMillis());
        assertTrue(engine.activeTypes().isEmpty());
        assertTrue(engine.flush(5000).isEmpty());
    }

    @Test
    void configRejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new RuleConfig(-1, 1, 1, 1, 25, 20));
        assertThrows(IllegalArgumentException.class, () -> new RuleConfig(1, 1, 1, 1, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> new RuleConfig(1, 1, 1, 1, 25, -5));
    }
}
