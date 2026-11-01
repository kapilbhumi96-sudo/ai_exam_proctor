package com.proctor.detection;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.FaceDetectorYN;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Face detector based on OpenCV's YuNet model. Besides the face box and confidence it returns
 * five landmarks (both eyes, nose tip, both mouth corners), which Week 3 uses for head pose.
 * Not thread-safe: use it from one thread.
 */
public class YuNetFaceDetector implements AutoCloseable {

    private static final String MODEL = "/models/face_detection_yunet_2023mar.onnx";
    /** The real model is about 230 KB. Anything much smaller is a broken download. */
    private static final long MIN_MODEL_BYTES = 100_000;
    private static final int MIN_FACE_SIZE_PX = 20;

    private final FaceDetectorYN detector;
    private final float scoreThreshold;
    private int inputWidth = -1;
    private int inputHeight = -1;

    public YuNetFaceDetector() throws IOException {
        this(0.7f);
    }

    public YuNetFaceDetector(float scoreThreshold) throws IOException {
        if (scoreThreshold <= 0f || scoreThreshold > 1f) {
            throw new IllegalArgumentException("scoreThreshold must be in (0, 1]");
        }
        this.scoreThreshold = scoreThreshold;

        Path model = ModelFiles.extract(MODEL);
        if (Files.size(model) < MIN_MODEL_BYTES) {
            throw new IOException("face_detection_yunet_2023mar.onnx is only " + Files.size(model)
                    + " bytes, so it is not the real model (probably a Git LFS pointer). "
                    + "Download it again in your browser, see docs/week-3-guide.md, step 1.");
        }
        // The input size is a placeholder here; detect() sets the real frame size.
        this.detector = FaceDetectorYN.create(model.toString(), "", new Size(320, 320),
                scoreThreshold, 0.3f, 5000, 0, 0);
        if (detector == null || detector.isNull()) {
            throw new IOException("OpenCV could not load the YuNet face detection model");
        }
    }

    public float scoreThreshold() {
        return scoreThreshold;
    }

    /** Detects faces in a BGR frame. The frame is not modified. */
    public List<FaceResult> detect(Mat frame) {
        List<FaceResult> result = new ArrayList<>();
        if (frame == null || frame.empty()) {
            return result;
        }
        final int frameWidth = frame.cols();
        final int frameHeight = frame.rows();
        if (frameWidth != inputWidth || frameHeight != inputHeight) {
            detector.setInputSize(new Size(frameWidth, frameHeight));
            inputWidth = frameWidth;
            inputHeight = frameHeight;
        }

        try (Mat faces = new Mat()) {
            detector.detect(frame, faces);
            if (faces.empty()) {
                return result;
            }
            // One row per face, 15 columns:
            // x, y, w, h, rightEye(x,y), leftEye(x,y), nose(x,y), rightMouth(x,y), leftMouth(x,y), score
            FloatIndexer data = faces.createIndexer();
            try {
                for (int row = 0; row < faces.rows(); row++) {
                    float score = data.get(row, 14);
                    if (score < scoreThreshold) {
                        continue;
                    }
                    Landmarks landmarks = new Landmarks(
                            new Landmarks.Point(data.get(row, 4), data.get(row, 5)),
                            new Landmarks.Point(data.get(row, 6), data.get(row, 7)),
                            new Landmarks.Point(data.get(row, 8), data.get(row, 9)),
                            new Landmarks.Point(data.get(row, 10), data.get(row, 11)),
                            new Landmarks.Point(data.get(row, 12), data.get(row, 13)));
                    FaceResult.fromPixelBox(data.get(row, 0), data.get(row, 1),
                                    data.get(row, 2), data.get(row, 3),
                                    score, landmarks, frameWidth, frameHeight, MIN_FACE_SIZE_PX)
                            .ifPresent(result::add);
                }
            } finally {
                data.release();
            }
        }
        return result;
    }

    @Override
    public void close() {
        detector.close();
    }
}
