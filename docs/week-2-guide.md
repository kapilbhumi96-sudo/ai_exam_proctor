# Week 2 Guide: Face Detection

**Goal:** the live preview draws a box around every face with a confidence score, and the status line shows the face count and the time detection takes per frame.

**Time:** about 1 week at 8-10 hours. Most of it is testing and tuning, because the code itself is small.

**What you will have at the end**
- A `FaceDetector` that runs OpenCV's pre-trained SSD face model on each frame.
- Green boxes when exactly one face is visible, red boxes when there are none or several. This is the first step toward the Week 4 rules (No Face, Multiple Faces).
- Unit tests for the box-conversion logic.
- Commit and tag `week-2`.

---

## Step 0: Prepare (15 minutes)

1. Make sure Week 1 is committed and pushed, and that `git status` shows a clean working tree.
2. Point the camera at your face, not the ceiling. Sit about an arm's length from the screen with your face well lit from the front (a window or lamp in front of you, not behind).
3. Record 4 short test clips (10-15 seconds each) with your phone or webcam. You will reuse them in Weeks 4 and 6.
   - Looking straight at the screen
   - Leaving the frame and coming back
   - A second person walking into the frame
   - Dim lighting
4. Save the clips in a `testdata/` folder inside the project. Add this line to the end of `.gitignore` so large videos are not pushed:
   ```
   testdata/
   ```

## Step 1: Add the Week 2 files (10 minutes)

The zip `ai-proctor-week2-files.zip` is laid out to match your project. Unzip it **into the project root** (the folder that contains `pom.xml`) and allow it to merge folders.

| File | What it does |
|---|---|
| `src/main/java/com/proctor/detection/FaceResult.java` | One face: box plus confidence. Converts the model's 0-1 coordinates into pixels, clamps them to the frame, and drops tiny boxes. |
| `src/main/java/com/proctor/detection/FaceDetector.java` | Loads the model and returns a list of faces for a frame. |
| `src/main/java/com/proctor/detection/ModelFiles.java` | Copies the bundled model files to a temp folder, because OpenCV reads from file paths. |
| `src/main/java/com/proctor/ui/FaceOverlay.java` | Draws the boxes and confidence labels on the frame. |
| `src/main/java/com/proctor/App.java` | **Replaces your Week 1 file.** Now runs detection between the queue and the preview. |
| `src/main/resources/models/` | The two model files (about 5 MB). |
| `src/test/java/com/proctor/detection/FaceResultTest.java` | 5 unit tests. |

After copying, IntelliJ should show a new `detection` package and a `resources/models` folder. If the folders don't appear, right-click the project and choose Reload from Disk. Then reload Maven (the circular arrows in the Maven panel).

## Step 2: Check the model files (2 minutes)

In `src/main/resources/models/` you should see:
- `deploy.prototxt` (about 28 KB)
- `res10_300x300_ssd_iter_140000_fp16.caffemodel` (about 5.3 MB)

If either is missing or has size 0, download it from:
- https://raw.githubusercontent.com/opencv/opencv/4.x/samples/dnn/face_detector/deploy.prototxt
- https://raw.githubusercontent.com/opencv/opencv_3rdparty/dnn_samples_face_detector_20180205_fp16/res10_300x300_ssd_iter_140000_fp16.caffemodel

Keep the exact file names.

## Step 3: Run the tests (5 minutes)

Run `mvn test`, or Run 'ai-proctor [test]' in the Maven panel. You should see **11 tests passed** (6 from Week 1 plus 5 new).

If the build fails with red errors in `FaceDetector.java` or `FaceOverlay.java`, copy the first error and send it to me. I could not compile these two files against OpenCV on my side, so your first build is the real check, and a small signature difference is the most likely cause.

## Step 4: Run the app (5 minutes)

Run `App` with the same run configuration as Week 1, including the camera environment variable if you needed it. You should see:

- A green box with a percentage (for example `97%`) around your face.
- A status line like `frame 210 | 13.9 FPS | faces 1 | detect 28 ms | dropped 4`.

Try these and watch the result:

| Do this | Expected |
|---|---|
| Look straight at the camera | 1 face, green box |
| Cover the camera lens or leave the frame | 0 faces, no box |
| Have another person step in | 2 faces, red boxes |
| Turn your head sideways | The box may disappear. This is normal; Week 3 handles head pose. |
| Turn the lights down | Confidence drops, and the box may flicker |

A `dropped` count above zero is fine. It means detection is slower than the camera, and the queue is discarding stale frames on purpose.

## Step 5: How it works (read once)

1. `WebcamCapture` puts frames in the queue (Week 1).
2. `App` takes the newest frame and calls `detector.detect(mat)`.
3. `FaceDetector` resizes the frame to 300x300, subtracts the training mean colour, and runs the network.
4. The network returns up to 200 candidate boxes, each with a confidence. Anything under the threshold (0.6) is ignored.
5. `FaceResult.fromNormalized` turns each remaining box into pixel coordinates, clamps it to the frame, and drops tiny boxes.
6. `FaceOverlay` draws the boxes, and the status line reports the count.

## Step 6: Tune and experiment (the main work of this week)

1. **Threshold.** In `FaceDetector`, the default is `0.6f`. Try `0.5f` and `0.8f` through `new FaceDetector(0.8f)` in `App`. Write down what happens in your test conditions: a lower value finds more faces but gives more false boxes, and a higher value does the opposite.
2. **Speed.** Note the `detect` time. Under about 40 ms is comfortable for 15 FPS. If it's much higher, tell me your laptop model, and we'll look at options.
3. **Edge cases.** Test with glasses, a face mask, a face partly out of frame, and a poster or photo of a face held up to the camera. The last one matters for proctoring: a photo can still be detected as a face. Liveness checks are out of scope for now, but note it in your project's limitations.
4. **Save your findings.** Create `docs/week-2-notes.md` with a small table of threshold, lighting, result. You will reuse this in Week 6.

## Step 7: Troubleshooting

| Problem | Fix |
|---|---|
| `Missing model file on classpath` | Check Step 2, then run Maven > Lifecycle > clean and run again. |
| `OpenCV could not load the face detection model` | The `.caffemodel` file is probably incomplete. Download it again. |
| Compile error in `FaceDetector` or `FaceOverlay` | Send me the first error line. |
| No box appears, but `faces 0` and the video is fine | Face too small or too dark. Move closer and add front light. |
| Video is slow or choppy | Check the `detect` time. Close heavy apps. Send me the numbers. |
| Camera black or fails | Use the same camera fix you used in Week 1. |

## Step 8: Commit and push

Update the README first. Change this line in the progress list:

```
- [ ] Week 2 - Face detection
```
to
```
- [x] Week 2 - Face detection
```

Add `detection/` to the structure section as well, if you like. Then, in the IntelliJ terminal, inside the `ai-proctor` folder:

```
git status
git add .
git commit -m "feat(detection): DNN face detection with overlay and face count"
git push
git tag week-2
git push --tags
```

Run `git status` before `git add .` and check that no videos or `target/` folder are listed. Also check `git log -1 --format="%an <%ae>"`, which should show your own name and email.

## Week 2 checklist

- [ ] 11 tests pass
- [ ] Green box on one face, red boxes on 0 or 2+ faces
- [ ] The `detect` time is shown and is under about 40 ms
- [ ] Threshold tried at three values, with notes in `docs/week-2-notes.md`
- [ ] README updated, committed, pushed, tagged `week-2`

## Next: Week 3

Head pose: facial landmarks, then yaw, pitch and roll from `solvePnP`, so the app knows when the candidate looks away from the screen.
