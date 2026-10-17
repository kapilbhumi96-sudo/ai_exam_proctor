package com.proctor.detection;

import org.bytedeco.javacpp.indexer.FloatIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_dnn.Net;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_32F;
import static org.bytedeco.opencv.global.opencv_dnn.blobFromImage;
import static org.bytedeco.opencv.global.opencv_dnn.readNetFromCaffe;

/**
 * Face detector using OpenCV's pre-trained SSD model (res10, 300x300, Caffe).
 * Call detect() with a BGR frame; it returns the faces found above the confidence threshold.
 * Not thread-safe: use it from one thread.
 */
public class FaceDetector implements AutoCloseable {

    private static final String PROTOTXT = "/models/deploy.prototxt";
    private static final String WEIGHTS = "/models/res10_300x300_ssd_iter_140000_fp16.caffemodel";

    private static final int INPUT_SIZE = 300;
    private static final Size NETWORK_SIZE = new Size(INPUT_SIZE, INPUT_SIZE);
    /** Per-channel mean (B, G, R) the model was trained with. */
    private static final Scalar MEAN = new Scalar(104.0, 177.0, 123.0, 0.0);
    /** Ignore boxes smaller than this many pixels on either side (usually noise). */
    private static final int MIN_FACE_SIZE_PX = 20;

    private final Net net;
    private final float confidenceThreshold;

    public FaceDetector() throws IOException {
        this(0.6f);
    }

    public FaceDetector(float confidenceThreshold) throws IOException {
        if (confidenceThreshold <= 0f || confidenceThreshold > 1f) {
            throw new IllegalArgumentException("confidenceThreshold must be in (0, 1]");
        }
        this.confidenceThreshold = confidenceThreshold;

        Path prototxt = ModelFiles.extract(PROTOTXT);
        Path weights = ModelFiles.extract(WEIGHTS);
        this.net = readNetFromCaffe(prototxt.toString(), weights.toString());
        if (net.empty()) {
            throw new IOException("OpenCV could not load the face detection model");
        }
    }

    public float confidenceThreshold() {
        return confidenceThreshold;
    }

    /** Detects faces in a BGR frame. The frame is not modified. */
    public List<FaceResult> detect(Mat frame) {
        List<FaceResult> faces = new ArrayList<>();
        if (frame == null || frame.empty()) {
            return faces;
        }
        final int frameWidth = frame.cols();
        final int frameHeight = frame.rows();

        try (Mat blob = blobFromImage(frame, 1.0, NETWORK_SIZE, MEAN, false, false, CV_32F)) {
            net.setInput(blob);
            try (Mat output = net.forward()) {
                // Output shape is [1, 1, N, 7]; each row is
                // [imageId, classId, confidence, x1, y1, x2, y2] with coordinates in 0..1.
                final int detections = output.size(2);
                FloatIndexer data = output.createIndexer();
                try {
                    for (int i = 0; i < detections; i++) {
                        float confidence = data.get(0, 0, i, 2);
                        if (confidence < confidenceThreshold) {
                            continue;
                        }
                        float x1 = data.get(0, 0, i, 3);
                        float y1 = data.get(0, 0, i, 4);
                        float x2 = data.get(0, 0, i, 5);
                        float y2 = data.get(0, 0, i, 6);
                        FaceResult.fromNormalized(x1, y1, x2, y2, confidence,
                                        frameWidth, frameHeight, MIN_FACE_SIZE_PX)
                                .ifPresent(faces::add);
                    }
                } finally {
                    data.release();
                }
            }
        }
        return faces;
    }

    @Override
    public void close() {
        net.close();
    }
}
