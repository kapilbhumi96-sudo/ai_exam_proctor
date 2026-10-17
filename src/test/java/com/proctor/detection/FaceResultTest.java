package com.proctor.detection;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaceResultTest {

    @Test
    void scalesNormalizedBoxToPixels() {
        Optional<FaceResult> face = FaceResult.fromNormalized(0.25f, 0.25f, 0.75f, 0.75f, 0.9f, 640, 480, 20);

        assertTrue(face.isPresent());
        FaceResult f = face.get();
        assertEquals(160, f.x());
        assertEquals(120, f.y());
        assertEquals(320, f.width());
        assertEquals(240, f.height());
        assertEquals(0.9f, f.confidence());
    }

    @Test
    void clampsBoxesThatLeaveTheFrame() {
        FaceResult f = FaceResult.fromNormalized(-0.2f, -0.1f, 1.3f, 1.2f, 0.8f, 640, 480, 20).orElseThrow();

        assertEquals(0, f.x());
        assertEquals(0, f.y());
        assertEquals(639, f.x() + f.width());
        assertEquals(479, f.y() + f.height());
    }

    @Test
    void rejectsBoxesThatAreTooSmall() {
        // 0.01 * 640 = about 6 px wide
        Optional<FaceResult> face = FaceResult.fromNormalized(0.50f, 0.50f, 0.51f, 0.70f, 0.9f, 640, 480, 20);

        assertTrue(face.isEmpty());
    }

    @Test
    void rejectsBoxesFullyOutsideTheFrame() {
        // Both x coordinates clamp to the same edge, leaving zero width.
        Optional<FaceResult> face = FaceResult.fromNormalized(1.1f, 0.2f, 1.4f, 0.8f, 0.9f, 640, 480, 20);

        assertTrue(face.isEmpty());
    }

    @Test
    void computesCenter() {
        FaceResult f = new FaceResult(100, 50, 80, 60, 0.9f);

        assertEquals(140, f.centerX());
        assertEquals(80, f.centerY());
    }
}
