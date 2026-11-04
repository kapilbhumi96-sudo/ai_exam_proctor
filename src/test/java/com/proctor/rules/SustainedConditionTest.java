package com.proctor.rules;

import com.proctor.rules.SustainedCondition.Change;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SustainedConditionTest {

    @Test
    void activatesOnlyAfterTheFullDelay() {
        SustainedCondition c = new SustainedCondition(2000, 500);

        assertEquals(Change.NONE, c.update(0, true));
        assertEquals(Change.NONE, c.update(1000, true));
        assertEquals(Change.NONE, c.update(1999, true));
        assertEquals(Change.ACTIVATED, c.update(2000, true));
        assertTrue(c.isActive());
    }

    @Test
    void aBrokenRunRestartsTheTimer() {
        SustainedCondition c = new SustainedCondition(2000, 500);

        c.update(0, true);
        c.update(1500, true);
        c.update(1600, false);          // run broken before it counted
        assertEquals(Change.NONE, c.update(1700, true));
        assertEquals(Change.NONE, c.update(3600, true));   // only 1900 ms since 1700
        assertEquals(Change.ACTIVATED, c.update(3700, true));
        assertEquals(1700, c.activeStartMillis());
    }

    @Test
    void startTimeIsWhenTheConditionBegan() {
        SustainedCondition c = new SustainedCondition(2000, 500);

        c.update(100, true);
        c.update(2100, true);

        assertTrue(c.isActive());
        assertEquals(100, c.activeStartMillis());
    }

    @Test
    void staysActiveThroughAShortGap() {
        SustainedCondition c = new SustainedCondition(1000, 500);
        c.update(0, true);
        c.update(1000, true);

        assertEquals(Change.NONE, c.update(1100, false));
        assertEquals(Change.NONE, c.update(1400, false));
        assertEquals(Change.NONE, c.update(1500, true));   // signal is back, gap was under 500 ms

        assertTrue(c.isActive());
    }

    @Test
    void deactivatesAfterTheRecoveryTimeAndReportsWhenItStopped() {
        SustainedCondition c = new SustainedCondition(1000, 500);
        c.update(0, true);
        c.update(1000, true);

        assertEquals(Change.NONE, c.update(5000, false));
        assertEquals(Change.NONE, c.update(5200, false));
        assertEquals(Change.DEACTIVATED, c.update(5500, false));

        assertFalse(c.isActive());
        assertEquals(5000, c.lastEndMillis());
    }

    @Test
    void canActivateAgainAfterDeactivation() {
        SustainedCondition c = new SustainedCondition(1000, 500);
        c.update(0, true);
        c.update(1000, true);
        c.update(2000, false);
        c.update(2500, false);

        c.update(3000, true);
        assertEquals(Change.ACTIVATED, c.update(4000, true));
        assertEquals(3000, c.activeStartMillis());
    }

    @Test
    void forceDeactivateEndsAnActiveState() {
        SustainedCondition c = new SustainedCondition(0, 500);
        c.update(100, true);

        assertTrue(c.forceDeactivate(900));
        assertFalse(c.isActive());
        assertEquals(900, c.lastEndMillis());
        assertFalse(c.forceDeactivate(1000));
    }

    @Test
    void rejectsNegativeTimes() {
        assertThrows(IllegalArgumentException.class, () -> new SustainedCondition(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new SustainedCondition(0, -1));
    }
}
