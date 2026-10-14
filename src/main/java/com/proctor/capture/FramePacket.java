package com.proctor.capture;

import org.bytedeco.opencv.opencv_core.Mat;

/**
 * One captured frame. The Mat holds native memory, so whoever takes a packet
 * from the queue must call close() when finished (use try-with-resources).
 */
public record FramePacket(long id, long timestampMillis, Mat mat) implements AutoCloseable {

    @Override
    public void close() {
        mat.close();
    }
}
