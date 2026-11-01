package com.proctor.pose;

/**
 * Exponential moving average over head poses, to remove frame-to-frame jitter.
 * alpha close to 1 follows the newest value quickly; close to 0 smooths heavily.
 * Call reset() when the face disappears so old values do not leak into the next face.
 */
public class PoseSmoother {

    private final double alpha;
    private HeadPose current;

    public PoseSmoother(double alpha) {
        if (alpha <= 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be in (0, 1]");
        }
        this.alpha = alpha;
    }

    public HeadPose update(HeadPose raw) {
        if (current == null) {
            current = raw;
        } else {
            current = new HeadPose(
                    blend(current.yaw(), raw.yaw()),
                    blend(current.pitch(), raw.pitch()),
                    blend(current.roll(), raw.roll()));
        }
        return current;
    }

    public void reset() {
        current = null;
    }

    private double blend(double old, double fresh) {
        return old + alpha * (fresh - old);
    }
}
