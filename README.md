# Handy-Meter
Use a really new method to measure distances with an Android Device

##How it works
1. You take a first picture of something in a plane
2. You move back
3. You take a second picture - It mesure distance between first and second picture
4. On the first pic, you select 2 point (single tap) - There are matched with the one in the second pic using template matching
5. You link them (long tap on each)
6. Real distance is displayed - Distance (d) is computed from focal distance (f), distance between two pics (m) and distance in pixels of segments in first and second pictures. Formula is d = m *  d1 * d2 / (f * (d1 - d2)). It use equations of 2D-3D projections for the two segments.

##!!
- Distance between 2 pics give sometime wrong results
- Template matching could be improved
- Calibration needs to be added as the focal distance is set in hard formy device right now (Check the calibration sample from Opencv for Android)
- I let the debug traces
- You needs to download OpenCV 2.4.10, import projects in Eclipse and correct in this project the link tothe OpenCV lib

