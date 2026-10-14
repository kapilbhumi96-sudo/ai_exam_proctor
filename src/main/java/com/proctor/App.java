package com.proctor;

import com.proctor.capture.CaptureConfig;
import com.proctor.capture.FramePacket;
import com.proctor.capture.WebcamCapture;
import com.proctor.ui.PreviewWindow;
import com.proctor.util.FpsMeter;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Week 1 entry point: webcam -> frame queue -> live Swing preview. */
public class App {

    public static void main(String[] args) throws Exception {
        CaptureConfig config = CaptureConfig.defaults();
        AtomicBoolean windowOpen = new AtomicBoolean(true);

        PreviewWindow window = new PreviewWindow("AI Proctor - Week 1: Capture", () -> windowOpen.set(false));
        SwingUtilities.invokeAndWait(() -> window.setVisible(true));

        FpsMeter fpsMeter = new FpsMeter();

        try (WebcamCapture capture = new WebcamCapture(config);
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
                    Frame frame = toFrame.convert(packet.mat());
                    BufferedImage converted = toImage.convert(frame);
                    BufferedImage copy = copyOf(converted); // converter may reuse its buffer

                    fpsMeter.tick();
                    String status = String.format("frame %d | %.1f FPS | queue %d | dropped %d",
                            packet.id(), fpsMeter.fps(), capture.queue().size(), capture.queue().droppedCount());
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
