package com.proctor.detection;

/**
 * The five facial landmarks produced by the YuNet face detector, in pixel coordinates.
 * The names follow the model's output order. Which eye lands on which side of the image depends
 * on camera mirroring, so the pose math never relies on the names: it orders the points itself.
 */
public record Landmarks(Point rightEye, Point leftEye, Point noseTip,
                        Point rightMouthCorner, Point leftMouthCorner) {

    /** A point in pixel coordinates (x grows to the right, y grows downward). */
    public record Point(float x, float y) {
    }
}
