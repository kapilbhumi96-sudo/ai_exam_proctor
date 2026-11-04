# Week 4 Guide: Rule Engine

**Goal:** the app turns raw detections (how many faces, where the head points) into proctoring events: **No Face**, **Multiple Faces** and **Looking Away**. A red banner appears on the preview while an event is active, and every event is printed to the console with its start time, end time and duration.

**Time:** about 1 week at 8-10 hours. The code is small. Most of the time goes into tuning the thresholds so honest candidates are not flagged.

**What you will have at the end**
- A `RuleEngine` (pure Java, no camera needed) that you can test with plain unit tests.
- Alert banners on the live preview.
- A console log of events, which Week 5 will save to a database.
- Thresholds tuned to your own measurements.
- Commit and tag `week-4`.

---

## The idea (read once)

Detection output flickers: one frame has no face, the next one does. If an alert fired on every bad frame, the app would be useless. So each rule has two timers:

- **Grace period:** the condition must last this long without a break before an event starts. A blink, a cough, or a quick glance at the keyboard is ignored.
- **Recovery period:** the condition must be gone this long before the event ends. This keeps one long event from being chopped into many short ones when the signal flickers.

| Rule | Condition | Starts after | Ends after it has been gone for |
|---|---|---|---|
| NO_FACE | no face visible | 3 s | 0.5 s |
| MULTIPLE_FACES | 2 or more faces | 1.5 s | 0.5 s |
| LOOKING_AWAY | a face is visible and \|yaw\| > 25° or \|pitch\| > 20° | 2 s | 0.5 s |

Every event has a start time, an end time, a duration, and a detail (for example the peak yaw reached). The start time is when the condition first began, not when the alert fired, so durations are honest.

## Step 0: Prepare (10 minutes)

1. Make sure Week 3 is committed, pushed and tagged `week-3`.
2. Open your `docs/week-3-notes.md` table. You will need your yaw and pitch readings in Step 5. If you haven't filled it in yet, do that first (the table is in the Week 3 guide).

## Step 1: Add the Week 4 files (10 minutes)

Unzip `ai-proctor-week4-files.zip` **into the project root**, the folder that contains `pom.xml`, and choose **Replace** when asked.

| File | What it does |
|---|---|
| `rules/EventType.java` | The three event types and their banner labels. |
| `rules/EventUpdate.java` | A STARTED or ENDED notification with times and details. |
| `rules/RuleConfig.java` | All thresholds and timings in one place. `defaults()` holds the starting values. |
| `rules/SustainedCondition.java` | The grace and recovery timer logic, used by every rule. |
| `rules/RuleEngine.java` | Feeds each frame through the three rules and produces events. |
| `util/TimeFormat.java` | Formats times as `mm:ss.t` for the log. |
| `ui/FaceOverlay.java` | **Replaces your Week 3 file.** Adds the red alert banner. |
| `App.java` | **Replaces your Week 3 file.** Runs the engine and prints events. |
| three test files | 21 new tests. |

Reload Maven afterwards.

## Step 2: Run the tests (5 minutes)

Run the tests from the Maven panel. You should see **47 tests passed** (26 from earlier weeks plus 21 new).

If there is a red error in `App.java` or `FaceOverlay.java`, send me the first error line. I could not compile those two files against OpenCV on my side. The rule engine itself I ran with a test harness and all its tests passed.

## Step 3: Run the app and trigger each rule (15 minutes)

Run `App`. Try each of these and check the result. Times are approximate.

| Do this | Expected |
|---|---|
| Sit normally, look at the screen | No banner, nothing in the console |
| Cover the camera lens for 4 seconds | After about 3 s a red banner says `ALERT: NO FACE`, and the console prints `ALERT  NO_FACE` |
| Uncover the lens | About 0.5 s later the banner clears and the console prints `ENDED  NO_FACE after ...` |
| Have a second person (or a photo of a face) enter the frame for 3 s | `ALERT: MULTIPLE FACES` after about 1.5 s |
| Turn your head to the side and hold for 3 s | `ALERT: LOOKING AWAY` after about 2 s, if your head passes the yaw limit |
| Glance sideways for 1 s and return | Nothing: the glance is shorter than the grace period |
| Look down at the keyboard for 3 s | `LOOKING AWAY` if your pitch passes the pitch limit |
| Blink, cough, move slightly | Nothing |

The console log looks like this:

```
[00:12.4] ALERT  LOOKING_AWAY
[00:17.8] ENDED  LOOKING_AWAY after 5.4 s  peak yaw 41, peak pitch 6
```

The time in brackets is the time since the first frame.

**A known limitation.** If you turn your head so far that the detector loses your face completely, the app reports `NO_FACE` after 3 s, not `LOOKING_AWAY`. Both are suspicious, so it is acceptable. Week 6 may merge them.

## Step 4: How the code fits together (read once)

1. `App` detects faces and estimates the head pose, as in Week 3.
2. It calls `rules.update(timestamp, faceCount, pose)` once per frame.
3. The engine returns zero or more `EventUpdate` objects. `App` prints them.
4. `rules.activeTypes()` tells the overlay which banners to draw.
5. When you close the window, `rules.flush(...)` ends any event still open, so nothing is left unfinished in the log.

Because the engine takes timestamps as input and never reads a clock, the tests can feed it a made-up session (for example, "no face from 0 to 5 seconds") and check the events exactly.

## Step 5: Tune the thresholds (the main work of this week)

The default limits (25° yaw, 20° pitch) are guesses. Use your Week 3 table:

1. **Yaw limit:** find the largest \|yaw\| you got while **looking at the far left and far right of the screen** (reading the edges is normal behaviour). Set the limit about 10° above it. If your screen-edge reading was 18°, use about 28°.
2. **Pitch limit:** decide your policy. If looking down at a keyboard is allowed in your exam, set the limit above your keyboard reading. If not, set it between your screen reading and your keyboard reading.
3. **Grace periods:** if honest behaviour triggers alerts (for example thinking while looking up), lengthen `lookAwayGraceMs`. If real cheating would be missed, shorten it. Stay between 1.5 s and 4 s.
4. Edit the numbers in `RuleConfig.defaults()`:
   ```java
   return new RuleConfig(3000, 1500, 2000, 500, 25.0, 20.0);
   //                    noFace multi  away  recov yaw   pitch
   ```
5. **Run a 2-minute "exam" session** where you read the screen, type, think, and look around naturally. Count the false alerts. Then run a second session where you deliberately look away and count the missed ones. Write both counts for each setting into `docs/week-4-notes.md`.
6. Change the config, run the unit tests again (a test that fails after you change a default only means a test depended on the old numbers, so the tests pass their own config), and repeat until the false alerts are few.

## Step 6: Troubleshooting

| Problem | Fix |
|---|---|
| Compile error in `App.java` or `FaceOverlay.java` | Send me the first error line. |
| Banner never appears | Check the console. If events print but no banner appears, send me a screenshot. |
| Alerts fire constantly while you sit normally | Your yaw or pitch limit is too low. Use the Week 3 readings, Step 5. |
| Alerts never fire when you turn away | The limit is too high, or the head turn is too small. Turn further, or lower the limit. |
| `NO_FACE` instead of `LOOKING_AWAY` on big turns | See the known limitation in Step 3. |
| Event keeps splitting in two | Raise `recoveryMs` from 500 to 800. |

## Step 7: Commit and push

Update the README. Change:

```
- [ ] Week 4 - Rule engine
```
to
```
- [x] Week 4 - Rule engine
```

And add this line to the Structure section:

```
  rules/              rule engine (no face, multiple faces, looking away)
```

Then in the IntelliJ terminal, inside the `ai-proctor` folder:

```
git status
git add .
git commit -m "feat(rules): rule engine for no-face, multiple-faces and looking-away events"
git push
git tag week-4
git push --tags
```

## Week 4 checklist

- [ ] 47 tests pass
- [ ] Each of the three rules fires in the live app, and a banner shows
- [ ] A quick glance or blink does not trigger an alert
- [ ] Thresholds tuned from your own readings, with notes in `docs/week-4-notes.md`
- [ ] README updated, committed, pushed, tagged `week-4`

## Next: Week 5

Event logger, risk score and alert UI. Events are saved to a SQLite database with timestamp, type and duration, each event adds weighted points to a risk score (Low, Moderate, High), and the alerts move from a banner on the video to a proper panel with a session summary.
