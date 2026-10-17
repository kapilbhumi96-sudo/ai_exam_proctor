package com.proctor;

import com.proctor.capture.CaptureConfig;
import com.proctor.capture.FramePacket;
import com.proctor.capture.WebcamCapture;
import com.proctor.detection.FaceDetector;
import com.proctor.detection.FaceResult;
import com.proctor.ui.FaceOverlay;
import com.proctor.ui.PreviewWindow;
import com.proctor.util.FpsMeter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Week 2 entry point: webcam -> frame queue -> face detection -> live preview with boxes. */
public class App {

    public static void main(String[] args) throws Exception {
        CaptureConfig config = CaptureConfig.defaults();
        AtomicBoolean windowOpen = new AtomicBoolean(true);

        FaceDetector detector;
        try {
            detector = new FaceDetector();
        } catch (IOException e) {
            System.err.println("Could not start face detector: " + e.getMessage());
            System.exit(1);
            return;
        }

        PreviewWindow window = new PreviewWindow("AI Proctor - Week 2: Face Detection", () -> windowOpen.set(false));
        SwingUtilities.invokeAndWait(() -> window.setVisible(true));

        FpsMeter fpsMeter = new FpsMeter();

        try (detector;
             WebcamCapture capture = new WebcamCapture(config);
             OpenCVFrameConverter.ToMat toFrame = new OpenCVFrameConverter.ToMat();
             Java2DFrameConverter toImage = new Java2DFrameConverter()) {

            capture.start();

            while (windowOpen.get()) {
                FramePacket packet = capture.queue().poll(200, TimeUnit.MILLISECONDS);

                if (packet == null) {
                    if (!capture.isRunning()) {
                        window.showStatus("Camera stopped: " + capture.lastError());
                        break;
                    }
                    continue;
                }

                try (packet) {
                    long start = System.nanoTime();
                    List<FaceResult> faces = detector.detect(packet.mat());
                    long detectMs = (System.nanoTime() - start) / 1_000_000;

                    // The packet owns its Mat, so drawing on it is safe.
                    FaceOverlay.draw(packet.mat(), faces);

                    Frame frame = toFrame.convert(packet.mat());
                    BufferedImage copy = copyOf(toImage.convert(frame)); // converter may reuse its buffer

                    fpsMeter.tick();
                    String status = String.format("frame %d | %.1f FPS | faces %d | detect %d ms | dropped %d",
                            packet.id(), fpsMeter.fps(), faces.size(), detectMs,
                            capture.queue().droppedCount());
                    window.showFrame(copy, status);
                }
            }
        }

        // Give the user a moment to read an error message, then exit.
        if (windowOpen.get()) {
            Thread.sleep(5000);
        }
        System.exit(0);
    }

    private static BufferedImage copyOf(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }
}
