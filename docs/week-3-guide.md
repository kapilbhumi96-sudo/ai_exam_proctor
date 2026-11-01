# Week 3 Guide: Landmarks and Head Pose

**Goal:** the app estimates where the candidate's head is pointing (yaw, pitch, roll) and shows it live, so Week 4 can decide when someone is looking away from the screen.

**Time:** about 1 week at 8-10 hours. The code is small; most of the time goes into testing and noting how the numbers behave on your face.

**What you will have at the end**
- A new face detector (YuNet) that returns the face box and five landmarks: both eyes, nose tip, both mouth corners.
- A head-pose estimator that turns those landmarks into yaw, pitch and roll in degrees.
- A smoother that removes jitter from the numbers.
- Landmark dots and a `yaw / pitch / roll` label drawn on the live preview.
- Commit and tag `week-3`.

## A change from the original plan

The plan said "landmarks, then `solvePnP`". I changed the method, for two reasons:

- A proper landmark model with 68 points is a 54 MB file, which is too large to keep in your repo.
- YuNet gives 5 landmarks in a 230 KB model. Five points on a generic 3D face model give a shaky `solvePnP` result, while simple face geometry gives a stable number you can explain in a viva.

How the geometry works: roll is the angle of the line between the eyes. Yaw is how far the nose tip sits sideways from the eye midpoint, compared to the eye distance. Pitch is how far the nose sits above or below its usual place between the eyes and mouth. The numbers are approximate. Week 6 calibration measures your own frontal position, which tightens them. If accuracy turns out to be too low, a `solvePnP` version is a possible upgrade after Week 6.

---

## Step 0: Prepare (5 minutes)

1. Week 2 should be committed and pushed. Run `git status` and confirm the working tree is clean.
2. Sit in front of the camera with your face well lit from the front.

## Step 1: Download the YuNet model (5 minutes)

The model must be downloaded **in your browser**. The command-line and "raw" links return only a 131-byte placeholder, which is why I could not put it in the zip.

1. Open this page in your browser:
   `https://github.com/opencv/opencv_zoo/blob/main/models/face_detection_yunet/face_detection_yunet_2023mar.onnx`
2. Click the **download icon** (Download raw file) at the top right of the file view.
3. Move the downloaded file to `src/main/resources/models/` in your project. Keep the exact name: `face_detection_yunet_2023mar.onnx`.
4. Check the size. It should be around 230 KB. If it is only a few bytes, the download failed.

If that page has moved, search GitHub for `opencv_zoo face_detection_yunet` and download the `2023mar` ONNX file from the model folder.

## Step 2: Add the Week 3 files (10 minutes)

Unzip `ai-proctor-week3-files.zip` **into the project root**, the folder that contains `pom.xml`, and let it merge folders. Windows will ask about replacing files. Choose **Replace**.

| File | What it does |
|---|---|
| `detection/Landmarks.java` | The five landmark points. |
| `detection/YuNetFaceDetector.java` | Detects faces and returns box, confidence and landmarks. |
| `detection/FaceResult.java` | **Replaces your Week 2 file.** Now optionally carries landmarks. Old code still works. |
| `pose/HeadPose.java` | Yaw, pitch, roll in degrees. |
| `pose/HeadPoseEstimator.java` | The geometry that turns landmarks into a pose. |
| `pose/PoseSmoother.java` | Smooths the numbers over time. |
| `ui/FaceOverlay.java` | **Replaces your Week 2 file.** Adds landmark dots and the pose label. |
| `App.java` | **Replaces your Week 2 file.** Uses the new detector and shows the pose. |
| three test files | 15 new tests. |

Your Week 2 `FaceDetector` and its model files stay in the project. They are no longer used, and we can delete them in the Week 8 cleanup.

Reload Maven afterwards, and use Reload from Disk if folders don't appear.

## Step 3: Run the tests (5 minutes)

Run the tests from the Maven panel. You should see **26 tests passed** (11 from Weeks 1 and 2, plus 15 new).

If there are red errors in `YuNetFaceDetector.java` or `FaceOverlay.java`, copy the first error and send it to me. I could not compile those two files against OpenCV on my side, so your build is the real check. The pure-Java parts (the pose math, the smoother, the box conversion) I ran and checked.

## Step 4: Run the app (5 minutes)

Run `App` with the same run configuration as before. You should see:

- A box around your face, green for one face and red for none or several.
- Five yellow dots: both eyes, the nose tip, and both mouth corners.
- Text under the box: `yaw +2  pitch -3  roll +1`.
- A status line like `faces 1 | yaw +2 pitch -3 roll +1 | 14.8 FPS | detect 9 ms | dropped 0`.

If the model file is missing or broken, the console prints `Could not start face detector:` followed by the reason, and the app exits.

**Sign conventions.** The preview is not mirrored, like a camera and unlike a mirror, so it will feel reversed at first.
- **Yaw is positive** when your face turns toward the right edge of the preview.
- **Pitch is positive** when you look down.
- **Roll is positive** when your head tilts clockwise on screen.

## Step 5: Measure how the numbers behave (the main work of this week)

Create `docs/week-3-notes.md` and fill in a table like this, holding each pose for a few seconds and noting the typical reading:

| Where you look | Yaw | Pitch | Roll |
|---|---|---|---|
| Center of the screen | | | |
| Far left of the screen | | | |
| Far right of the screen | | | |
| Down at the keyboard | | | |
| Up at the ceiling | | | |
| Head tilted to one side | | | |
| Turned fully sideways (face may be lost) | | | |

Things to check and write down:

1. **Looking at the screen should give small numbers.** Roughly within ±10° yaw and a bit more on pitch is normal before calibration. If your pitch sits at, say, +12° while looking straight at the screen, that is the frontal-offset that Week 6 calibration removes.
2. **Turning toward the screen edges should clearly increase yaw.** Note the values; they will become the thresholds in Week 4.
3. **Jitter.** Hold still and watch whether the numbers wander. Try `new PoseSmoother(0.4)` in `App` at 0.2 and 0.7, and note the trade-off: a lower value is smoother but reacts slower.
4. **Glasses, a mask, bad lighting.** Check whether the landmarks stay on the right spots (the yellow dots).
5. **Two people.** The pose is shown for the largest face only. Confirm the box turns red.

## Step 6: Troubleshooting

| Problem | Fix |
|---|---|
| `Missing model file on classpath` | Check Step 1, then run Maven > Lifecycle > clean and run again. |
| `... is only N bytes, so it is not the real model` | Download the model again in the browser (Step 1). |
| `OpenCV could not load the YuNet face detection model` | The file is corrupt or the wrong version. Download it again. |
| Compile error in `YuNetFaceDetector` or `FaceOverlay` | Send me the first error line. |
| Yellow dots are in the wrong places | Send me a screenshot. |
| Video is slow | Check the `detect` time and send me the numbers. |
| Camera black or fails | Use the same camera fix as in Week 1. |

## Step 7: Commit and push

Update the README first. Change this line in the progress list:

```
- [ ] Week 3 - Head pose
```
to
```
- [x] Week 3 - Head pose
```

And add these lines to the Structure section:

```
  detection/          face detection + landmarks
  pose/               head pose estimation
```

Then, in the IntelliJ terminal, inside the `ai-proctor` folder:

```
git status
git add .
git commit -m "feat(pose): YuNet landmarks and head pose estimation (yaw, pitch, roll)"
git push
git tag week-3
git push --tags
```

Check before `git add .` that `git status` lists the new files and the `.onnx` model, and no videos or `target/`.

## Week 3 checklist

- [ ] 26 tests pass
- [ ] Five yellow landmark dots sit on the right spots
- [ ] Yaw, pitch and roll change in the expected directions when you move your head
- [ ] `docs/week-3-notes.md` has your measured table
- [ ] README updated, committed, pushed, tagged `week-3`

## Next: Week 4

The rule engine. Using face count and head pose over time, it raises events for No Face, Multiple Faces and Looking Away. Each rule has a grace period, so a quick glance or a blink doesn't trigger a false alarm. The values in your Week 3 table become the first thresholds.
