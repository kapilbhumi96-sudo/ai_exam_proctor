package com.proctor;

import com.proctor.capture.CaptureConfig;
import com.proctor.capture.FramePacket;
import com.proctor.capture.WebcamCapture;
import com.proctor.detection.FaceResult;
import com.proctor.detection.YuNetFaceDetector;
import com.proctor.pose.HeadPose;
import com.proctor.pose.HeadPoseEstimator;
import com.proctor.pose.PoseSmoother;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Week 3 entry point: webcam -> queue -> face + landmarks -> head pose -> live preview. */
public class App {

    public static void main(String[] args) throws Exception {
        CaptureConfig config = CaptureConfig.defaults();
        AtomicBoolean windowOpen = new AtomicBoolean(true);

        YuNetFaceDetector detector;
        try {
            detector = new YuNetFaceDetector();
        } catch (IOException e) {
            System.err.println("Could not start face detector: " + e.getMessage());
            System.exit(1);
            return;
        }

        PreviewWindow window = new PreviewWindow("AI Proctor - Week 3: Head Pose", () -> windowOpen.set(false));
        SwingUtilities.invokeAndWait(() -> window.setVisible(true));

        FpsMeter fpsMeter = new FpsMeter();
        PoseSmoother smoother = new PoseSmoother(0.4);

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

                    FaceOverlay.draw(packet.mat(), faces);

                    // Head pose is computed for the largest face (the closest person).
                    String poseText = "no pose";
                    Optional<FaceResult> main = faces.stream()
                            .filter(FaceResult::hasLandmarks)
                            .max(Comparator.comparingInt(FaceResult::area));
                    if (main.isPresent()) {
                        Optional<HeadPose> raw = HeadPoseEstimator.estimate(main.get().landmarks());
                        if (raw.isPresent()) {
                            HeadPose pose = smoother.update(raw.get());
                            FaceOverlay.drawPose(packet.mat(), main.get(), pose);
                            poseText = String.format(Locale.ROOT, "yaw %+.0f pitch %+.0f roll %+.0f",
                                    pose.yaw(), pose.pitch(), pose.roll());
                        }
                    } else {
                        smoother.reset();
                    }

                    Frame frame = toFrame.convert(packet.mat());
                    BufferedImage copy = copyOf(toImage.convert(frame)); // converter may reuse its buffer

                    fpsMeter.tick();
                    String status = String.format(Locale.ROOT, "faces %d | %s | %.1f FPS | detect %d ms | dropped %d",
                            faces.size(), poseText, fpsMeter.fps(), detectMs, capture.queue().droppedCount());
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
