package com.proctor.detection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceResultPixelBoxTest {

    private static final Landmarks LANDMARKS = new Landmarks(
            new Landmarks.Point(1, 1), new Landmarks.Point(2, 1), new Landmarks.Point(1.5f, 2),
            new Landmarks.Point(1, 3), new Landmarks.Point(2, 3));

    @Test
    void keepsBoxAndLandmarks() {
        FaceResult f = FaceResult.fromPixelBox(100, 80, 120, 150, 0.95f, LANDMARKS, 640, 480, 20).orElseThrow();

        assertEquals(100, f.x());
        assertEquals(80, f.y());
        assertEquals(120, f.width());
        assertEquals(150, f.height());
        assertTrue(f.hasLandmarks());
        assertSame(LANDMARKS, f.landmarks());
    }

    @Test
    void clampsBoxesThatLeaveTheFrame() {
        FaceResult f = FaceResult.fromPixelBox(-30, -20, 800, 600, 0.9f, LANDMARKS, 640, 480, 20).orElseThrow();

        assertEquals(0, f.x());
        assertEquals(0, f.y());
        assertEquals(639, f.x() + f.width());
        assertEquals(479, f.y() + f.height());
    }

    @Test
    void rejectsTinyBoxes() {
        assertTrue(FaceResult.fromPixelBox(10, 10, 8, 40, 0.9f, LANDMARKS, 640, 480, 20).isEmpty());
    }

    @Test
    void facesWithoutLandmarksReportNone() {
        assertFalse(new FaceResult(10, 10, 50, 50, 0.9f).hasLandmarks());
    }
}
