package com.proctor.pose;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoseSmootherTest {

    @Test
    void firstUpdateReturnsTheRawPose() {
        PoseSmoother smoother = new PoseSmoother(0.5);

        HeadPose result = smoother.update(new HeadPose(10, -4, 2));

        assertEquals(new HeadPose(10, -4, 2), result);
    }

    @Test
    void laterUpdatesMoveByAlphaTowardTheNewValue() {
        PoseSmoother smoother = new PoseSmoother(0.5);
        smoother.update(new HeadPose(0, 0, 0));

        HeadPose result = smoother.update(new HeadPose(20, -10, 4));

        assertEquals(10, result.yaw(), 1e-9);
        assertEquals(-5, result.pitch(), 1e-9);
        assertEquals(2, result.roll(), 1e-9);
    }

    @Test
    void resetForgetsHistory() {
        PoseSmoother smoother = new PoseSmoother(0.5);
        smoother.update(new HeadPose(40, 0, 0));

        smoother.reset();

        assertEquals(new HeadPose(-5, 0, 0), smoother.update(new HeadPose(-5, 0, 0)));
    }

    @Test
    void rejectsInvalidAlpha() {
        assertThrows(IllegalArgumentException.class, () -> new PoseSmoother(0));
        assertThrows(IllegalArgumentException.class, () -> new PoseSmoother(1.5));
    }
}
