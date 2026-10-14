package com.proctor.capture;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Grabs frames from the webcam on a background thread and puts them in a
 * {@link FrameQueue}. Start with start(), read from queue(), stop with
 * stop() or close().
 */
public class WebcamCapture implements AutoCloseable {

    private final CaptureConfig config;
    private final FrameQueue<FramePacket> queue;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String lastError;
    private Thread thread;

    public WebcamCapture(CaptureConfig config) {
        this.config = config;
        this.queue = new FrameQueue<>(config.queueCapacity(), FramePacket::close);
    }

    public FrameQueue<FramePacket> queue() {
        return queue;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Non-null if the capture thread stopped because of an error. */
    public String lastError() {
        return lastError;
    }

    public synchronized void start() {
        if (running.getAndSet(true)) {
            return; // already running
        }
        lastError = null;
        thread = new Thread(this::runLoop, "webcam-capture");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running.set(false);
        if (thread != null) {
            try {
                thread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
        queue.clear();
    }

    @Override
    public void close() {
        stop();
    }

    private void runLoop() {
        final long frameIntervalMs = 1000L / config.targetFps();

        try (OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(config.cameraIndex());
             OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat()) {

            grabber.setImageWidth(config.width());
            grabber.setImageHeight(config.height());
            grabber.start();

            long nextId = 0;
            while (running.get()) {
                long loopStart = System.currentTimeMillis();

                Frame frame = grabber.grab();
                if (frame == null || frame.image == null) {
                    continue;
                }

                Mat shared = converter.convert(frame);
                if (shared == null || shared.empty()) {
                    continue;
                }
                // The converter reuses its buffer, so we must copy before queueing.
                queue.offer(new FramePacket(nextId++, System.currentTimeMillis(), shared.clone()));

                long elapsed = System.currentTimeMillis() - loopStart;
                long sleepMs = frameIntervalMs - elapsed;
                if (sleepMs > 0) {
                    Thread.sleep(sleepMs);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            lastError = e.getMessage() != null ? e.getMessage() : e.toString();
            System.err.println("[WebcamCapture] stopped: " + lastError);
        } finally {
            running.set(false);
        }
    }
}
