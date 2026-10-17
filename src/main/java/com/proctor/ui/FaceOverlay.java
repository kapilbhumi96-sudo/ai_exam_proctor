package com.proctor.ui;

import com.proctor.detection.FaceResult;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;

import java.util.List;
import java.util.Locale;

import static org.bytedeco.opencv.global.opencv_imgproc.FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_8;
import static org.bytedeco.opencv.global.opencv_imgproc.LINE_AA;
import static org.bytedeco.opencv.global.opencv_imgproc.putText;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

/** Draws face boxes and confidence labels directly onto a frame (colors are BGR). */
public final class FaceOverlay {

    private static final Scalar GREEN = new Scalar(0, 200, 0, 0);
    private static final Scalar RED = new Scalar(0, 0, 255, 0);

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
        }
    }
}
