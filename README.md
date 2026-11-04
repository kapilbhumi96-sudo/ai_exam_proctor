# AI-Based Exam Proctoring (Java + OpenCV + AI)

Desktop proctoring app that detects face absence, multiple faces and looking-away events, scores risk, and generates an AI-written suspicion report.

## Progress
- [x] Week 1 - Webcam capture, frame queue, live preview
- [x] Week 2 - Face detection
- [x] Week 3 - Head pose
- [x] Week 4 - Rule engine
- [ ] Week 5 - Event logger, risk score, alert UI
- [ ] Week 6 - Calibration and tuning
- [ ] Week 7 - AI report
- [ ] Week 8 - QA and release

## Run
```
mvn test
mvn compile exec:java
```
Requires JDK 17+, Maven, and a webcam.

## Structure
```
com.proctor
  App                 entry point
  capture/            webcam capture + bounded frame queue
  detection/          face detection + landmarks
  pose/               head pose estimation
  rules/              rule engine (no face, multiple faces, looking away)
  ui/                 Swing windows
  util/               small helpers
```