package com.proctor.detection;

import java.util.Optional;

/**
 * One detected face: a bounding box in pixel coordinates plus the detector's confidence (0..1).
 */
public record FaceResult(int x, int y, int width, int height, float confidence) {

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    /**
     * Converts the detector's normalized box (0..1 relative to the frame) into a pixel box.
     * The box is clamped to the frame, and boxes smaller than minSizePx are rejected.
     * Pure Java, so it can be unit-tested without OpenCV or a camera.
     */
    public static Optional<FaceResult> fromNormalized(float x1, float y1, float x2, float y2,
                                                      float confidence,
                                                      int frameWidth, int frameHeight,
                                                      int minSizePx) {
        int left = clamp(Math.round(x1 * frameWidth), 0, frameWidth - 1);
        int top = clamp(Math.round(y1 * frameHeight), 0, frameHeight - 1);
        int right = clamp(Math.round(x2 * frameWidth), 0, frameWidth - 1);
        int bottom = clamp(Math.round(y2 * frameHeight), 0, frameHeight - 1);

        int width = right - left;
        int height = bottom - top;
        if (width < minSizePx || height < minSizePx) {
            return Optional.empty();
        }
        return Optional.of(new FaceResult(left, top, width, height, confidence));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
