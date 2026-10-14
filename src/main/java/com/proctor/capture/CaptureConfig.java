package com.proctor.capture;

/**
 * Settings for the webcam capture module.
 *
 * @param cameraIndex   0 is usually the built-in webcam
 * @param width         requested frame width in pixels (the camera may pick the nearest supported size)
 * @param height        requested frame height in pixels
 * @param targetFps     how many frames per second the capture thread should aim for
 * @param queueCapacity max frames waiting in the queue; when full, the oldest frame is dropped
 */
public record CaptureConfig(int cameraIndex, int width, int height, int targetFps, int queueCapacity) {

    public CaptureConfig {
        if (cameraIndex < 0) {
            throw new IllegalArgumentException("cameraIndex must be >= 0");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be > 0");
        }
        if (targetFps <= 0) {
            throw new IllegalArgumentException("targetFps must be > 0");
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("queueCapacity must be >= 1");
        }
    }

    public static CaptureConfig defaults() {
        return new CaptureConfig(0, 640, 480, 15, 5);
    }
}
