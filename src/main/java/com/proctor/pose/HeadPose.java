package com.proctor.pose;

/**
 * Estimated head orientation in degrees.
 *
 * Sign conventions (as seen in the unmirrored camera image):
 *  - yaw:   positive when the face turns toward the right edge of the image
 *  - pitch: positive when the head tilts down (looking toward the keyboard)
 *  - roll:  positive when the head tilts clockwise on screen
 *
 * The values are approximations from five landmarks, accurate enough to tell "looking at the
 * screen" from "looking away". Rules use absolute values, so the signs only matter for debugging.
 */
public record HeadPose(double yaw, double pitch, double roll) {
}
