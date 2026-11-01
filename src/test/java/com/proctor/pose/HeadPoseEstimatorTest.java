package com.proctor.pose;

import com.proctor.detection.Landmarks;
import com.proctor.detection.Landmarks.Point;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadPoseEstimatorTest {

    private static final double TOLERANCE = 0.5; // degrees

    /** Frontal face: eye distance 60, mouth 50 below the eyes, nose 60% of the way down. */
    private static Landmarks frontal() {
        return new Landmarks(new Point(100, 100), new Point(160, 100), new Point(130, 130),
                new Point(115, 150), new Point(145, 150));
    }

    private static Landmarks withNose(float x, float y) {
        Landmarks f = frontal();
        return new Landmarks(f.rightEye(), f.leftEye(), new Point(x, y), f.rightMouthCorner(), f.leftMouthCorner());
    }

    private static Point rotate(Point p, double degrees, double cx, double cy) {
        double a = Math.toRadians(degrees);
        double dx = p.x() - cx;
        double dy = p.y() - cy;
        return new Point((float) (cx + dx * Math.cos(a) - dy * Math.sin(a)),
                (float) (cy + dx * Math.sin(a) + dy * Math.cos(a)));
    }

    @Test
    void frontalFaceHasZeroPose() {
        HeadPose pose = HeadPoseEstimator.estimate(frontal()).orElseThrow();

        assertEquals(0, pose.yaw(), TOLERANCE);
        assertEquals(0, pose.pitch(), TOLERANCE);
        assertEquals(0, pose.roll(), TOLERANCE);
    }

    @Test
    void yawFollowsSidewaysNoseOffset() {
        float shift = (float) (HeadPoseEstimator.NOSE_PROTRUSION * 60 * Math.tan(Math.toRadians(30)));

        HeadPose right = HeadPoseEstimator.estimate(withNose(130 + shift, 130)).orElseThrow();
        HeadPose left = HeadPoseEstimator.estimate(withNose(130 - shift, 130)).orElseThrow();

        assertEquals(30, right.yaw(), TOLERANCE);
        assertEquals(-30, left.yaw(), TOLERANCE);
        assertEquals(0, right.pitch(), TOLERANCE);
    }

    @Test
    void pitchFollowsVerticalNoseOffset() {
        float shift = (float) (HeadPoseEstimator.NOSE_PROTRUSION * 60 * Math.tan(Math.toRadians(20)));

        HeadPose down = HeadPoseEstimator.estimate(withNose(130, 130 + shift)).orElseThrow();
        HeadPose up = HeadPoseEstimator.estimate(withNose(130, 130 - shift)).orElseThrow();

        assertEquals(20, down.pitch(), TOLERANCE);
        assertEquals(-20, up.pitch(), TOLERANCE);
        assertEquals(0, down.yaw(), TOLERANCE);
    }

    @Test
    void rollFollowsEyeLine() {
        Landmarks tilted = new Landmarks(new Point(100, 100), new Point(160, 130), new Point(130, 130),
                new Point(115, 150), new Point(145, 150));

        HeadPose pose = HeadPoseEstimator.estimate(tilted).orElseThrow();

        assertEquals(Math.toDegrees(Math.atan2(30, 60)), pose.roll(), TOLERANCE);
    }

    @Test
    void swappingEyeOrderGivesSameResult() {
        Landmarks f = frontal();
        Landmarks swapped = new Landmarks(f.leftEye(), f.rightEye(), f.noseTip(),
                f.leftMouthCorner(), f.rightMouthCorner());
        float shift = (float) (HeadPoseEstimator.NOSE_PROTRUSION * 60 * Math.tan(Math.toRadians(25)));
        Landmarks turned = withNose(130 + shift, 130);
        Landmarks turnedSwapped = new Landmarks(turned.leftEye(), turned.rightEye(), turned.noseTip(),
                turned.leftMouthCorner(), turned.rightMouthCorner());

        assertEquals(HeadPoseEstimator.estimate(f).orElseThrow(), HeadPoseEstimator.estimate(swapped).orElseThrow());
        assertEquals(HeadPoseEstimator.estimate(turned).orElseThrow().yaw(),
                HeadPoseEstimator.estimate(turnedSwapped).orElseThrow().yaw(), 1e-6);
    }

    @Test
    void rotatingTheWholeFaceChangesOnlyRoll() {
        Landmarks f = frontal();
        Landmarks rotated = new Landmarks(rotate(f.rightEye(), 20, 130, 125), rotate(f.leftEye(), 20, 130, 125),
                rotate(f.noseTip(), 20, 130, 125), rotate(f.rightMouthCorner(), 20, 130, 125),
                rotate(f.leftMouthCorner(), 20, 130, 125));

        HeadPose pose = HeadPoseEstimator.estimate(rotated).orElseThrow();

        assertEquals(0, pose.yaw(), TOLERANCE);
        assertEquals(0, pose.pitch(), TOLERANCE);
        assertEquals(20, pose.roll(), TOLERANCE);
    }

    @Test
    void unusableLandmarksGiveEmptyResult() {
        Landmarks f = frontal();
        Landmarks eyesTogether = new Landmarks(new Point(100, 100), new Point(100, 100), f.noseTip(),
                f.rightMouthCorner(), f.leftMouthCorner());
        Landmarks mouthAboveEyes = new Landmarks(f.rightEye(), f.leftEye(), f.noseTip(),
                new Point(115, 60), new Point(145, 60));

        Optional<HeadPose> a = HeadPoseEstimator.estimate(eyesTogether);
        Optional<HeadPose> b = HeadPoseEstimator.estimate(mouthAboveEyes);

        assertTrue(a.isEmpty());
        assertTrue(b.isEmpty());
    }
}
