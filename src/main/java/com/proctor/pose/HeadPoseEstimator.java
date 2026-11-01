package com.proctor.pose;

import com.proctor.detection.Landmarks;
import com.proctor.detection.Landmarks.Point;

import java.util.Optional;

/**
 * Estimates head pose from the five YuNet landmarks using face geometry. Pure Java.
 *
 * Idea: work in a coordinate frame aligned with the eye line, so head roll does not leak into
 * yaw or pitch.
 *  - roll  = angle of the line between the eyes
 *  - yaw   = how far the nose tip sits left/right of the eye midpoint, relative to the eye distance
 *  - pitch = how far the nose tip sits above/below where it sits on a frontal face
 *
 * The nose sticks out roughly 0.44 of the eye distance, so a sideways turn of angle t shifts the
 * nose by about 0.44 * eyeDistance * tan(t). Both constants below are averages for an adult face;
 * Week 6 calibration measures the actual frontal position for each person.
 */
public final class HeadPoseEstimator {

    /** Nose protrusion divided by eye distance (average adult face). */
    static final double NOSE_PROTRUSION = 0.44;
    /** On a frontal face the nose tip sits about this fraction of the way from the eyes to the mouth. */
    static final double FRONTAL_NOSE_POSITION = 0.6;

    private HeadPoseEstimator() {
    }

    /** Returns the pose, or empty if the landmarks are unusable (e.g. eyes on top of each other). */
    public static Optional<HeadPose> estimate(Landmarks landmarks) {
        Point eyeA = landmarks.rightEye();
        Point eyeB = landmarks.leftEye();
        if (eyeA.x() > eyeB.x()) {          // make eyeA the one on the left of the image
            Point swap = eyeA;
            eyeA = eyeB;
            eyeB = swap;
        }

        double ex = eyeB.x() - eyeA.x();
        double ey = eyeB.y() - eyeA.y();
        double eyeDistance = Math.hypot(ex, ey);
        if (eyeDistance < 1.0) {
            return Optional.empty();
        }

        // u runs along the eye line, v is perpendicular and points down the face.
        double ux = ex / eyeDistance;
        double uy = ey / eyeDistance;
        double vx = -uy;
        double vy = ux;

        double midX = (eyeA.x() + eyeB.x()) / 2.0;
        double midY = (eyeA.y() + eyeB.y()) / 2.0;

        double noseU = (landmarks.noseTip().x() - midX) * ux + (landmarks.noseTip().y() - midY) * uy;
        double noseV = (landmarks.noseTip().x() - midX) * vx + (landmarks.noseTip().y() - midY) * vy;

        double mouthX = (landmarks.rightMouthCorner().x() + landmarks.leftMouthCorner().x()) / 2.0;
        double mouthY = (landmarks.rightMouthCorner().y() + landmarks.leftMouthCorner().y()) / 2.0;
        double mouthV = (mouthX - midX) * vx + (mouthY - midY) * vy;
        if (mouthV <= 0) {                   // mouth is not below the eyes, landmarks are unreliable
            return Optional.empty();
        }

        double roll = Math.toDegrees(Math.atan2(ey, ex));
        double yaw = Math.toDegrees(Math.atan(noseU / (NOSE_PROTRUSION * eyeDistance)));
        double noseOffset = noseV - FRONTAL_NOSE_POSITION * mouthV;
        double pitch = Math.toDegrees(Math.atan(noseOffset / (NOSE_PROTRUSION * eyeDistance)));

        return Optional.of(new HeadPose(yaw, pitch, roll));
    }
}
