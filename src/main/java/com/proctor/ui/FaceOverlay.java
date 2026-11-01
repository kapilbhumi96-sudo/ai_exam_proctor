package com.proctor.ui;

import com.proctor.detection.FaceResult;
import com.proctor.detection.Landmarks;
import com.proctor.pose.HeadPose;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.util.List;
import java.util.Locale;

import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_8;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_AA;
import static org.bytedeco.opencv.global.opencv_imgproc.circle;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

/** Draws face boxes, landmarks and head-pose text directly onto a frame (colors are BGR). */
public final class FaceOverlay {

    private static final Scalar GREEN = new Scalar(0, 200, 0, 0);
    private static final Scalar RED = new Scalar(0, 0, 255, 0);
    private static final Scalar YELLOW = new Scalar(0, 255, 255, 0);

    private FaceOverlay() {
    }

    /** Green boxes when exactly one face is visible; red when there are none or several. */
    public static void draw(Mat frame, List<FaceResult> faces) {
        Scalar color = faces.size() == 1 ? GREEN : RED;
        for (FaceResult face : faces) {
            Point topLeft = new Point(face.x(), face.y());
            Point bottomRight = new Point(face.x() + face.width(), face.y() + face.height());
            rectangle(frame, topLeft, bottomRight, color, 2, LINE_8, 0);

            String label = String.format(Locale.ROOT, "%.0f%%", face.confidence() * 100);
            Point labelPos = new Point(face.x(), Math.max(15, face.y() - 6));
            putText(frame, label, labelPos, FONT_HERSHEY_SIMPLEX, 0.5, color, 1, LINE_AA, false);

            if (face.hasLandmarks()) {
                drawLandmarks(frame, face.landmarks());
            }
        }
    }

    /** Small yellow dots on the eyes, nose tip and mouth corners. */
    public static void drawLandmarks(Mat frame, Landmarks landmarks) {
        for (Landmarks.Point p : List.of(landmarks.rightEye(), landmarks.leftEye(), landmarks.noseTip(),
                landmarks.rightMouthCorner(), landmarks.leftMouthCorner())) {
            circle(frame, new Point(Math.round(p.x()), Math.round(p.y())), 3, YELLOW, -1, LINE_8, 0);
        }
    }

    /** Writes yaw / pitch / roll under the face box. */
    public static void drawPose(Mat frame, FaceResult face, HeadPose pose) {
        String text = String.format(Locale.ROOT, "yaw %+.0f  pitch %+.0f  roll %+.0f",
                pose.yaw(), pose.pitch(), pose.roll());
        Point pos = new Point(face.x(), Math.min(frame.rows() - 6, face.y() + face.height() + 18));
        putText(frame, text, pos, FONT_HERSHEY_SIMPLEX, 0.5, YELLOW, 1, LINE_AA, false);
    }
}
