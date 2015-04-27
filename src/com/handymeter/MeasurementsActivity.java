package com.handymeter;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.Toast;

public class MeasurementsActivity extends Activity implements OnTouchListener {

	private float[] lastEvent = null;
	private float d = 0f;
	private float newRot = 0f;
	private Matrix matrix = new Matrix();
	private Matrix savedMatrix = new Matrix();
	private Matrix inversedMatrix = new Matrix();
	private boolean hasMoved = false;
	private Bitmap currentScene, currentScene2;
	private List<Point> listKeypoints = new ArrayList<Point>();
	private List<Point> listMatchingKeypoints = new ArrayList<Point>();
	private List<Point> listLines = new ArrayList<Point>();
	private int memPoint = -1;

	// Fields
	private String TAG = this.getClass().getSimpleName();

	// We can be in one of these 3 states
	private static final int NONE = 0;
	private static final int DRAG = 1;
	private static final int ZOOM = 2;
	private int mode = NONE;

	// Remember some things for zooming
	private PointF start = new PointF();
	private PointF mid = new PointF();
	float oldDist = 1f;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.measurements_layout);
		
		Mat firstImage = Tutorial3Activity.getFirstImage();
		currentScene = Bitmap.createBitmap(firstImage.cols(),
				firstImage.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(Tutorial3Activity.getFirstImage(), currentScene);
		
		Mat secondImage = Tutorial3Activity.getSecondImage();
		currentScene2 = Bitmap.createBitmap(secondImage.cols(),
				secondImage.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(Tutorial3Activity.getSecondImage(), currentScene2);
		
		ImageView view = (ImageView) findViewById(R.id.current_scene);
		view.setImageBitmap(currentScene);
		view.setOnTouchListener(this);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		ImageView view = (ImageView) v;

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			hasMoved = false;
			savedMatrix.set(matrix);
			start.set(event.getX(), event.getY());
			mode = DRAG;
			lastEvent = null;
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			hasMoved = false;
			oldDist = spacing(event);
			if (oldDist > 10f) {
				savedMatrix.set(matrix);
				midPoint(mid, event);
				mode = ZOOM;
			}
			lastEvent = new float[4];
			lastEvent[0] = event.getX(0);
			lastEvent[1] = event.getX(1);
			lastEvent[2] = event.getY(0);
			lastEvent[3] = event.getY(1);
			d = rotation(event);
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_POINTER_UP:
			mode = NONE;
			lastEvent = null;
			if (!hasMoved) {
				float[] pts = { event.getX(), event.getY() };
				savedMatrix.invert(inversedMatrix);
				inversedMatrix.mapPoints(pts);
				pts[0] *= (float) currentScene.getWidth() / (float) view.getWidth();
				pts[1] *= (float) currentScene.getHeight() / (float) view.getHeight();
				float[] f = new float[9];
				matrix.getValues(f);
				float scaleX = f[Matrix.MSCALE_X];
				float scaleY = f[Matrix.MSCALE_Y];
				int exist = isExisting(pts, view, scaleX, scaleY);
				if (event.getEventTime() - event.getDownTime() < 300) {
					if (exist == -1) {
						Point p = new Point();
						p.x = (int) pts[0];
						p.y = (int) pts[1];
						listKeypoints.add(p);
						Point p2 = findCorrespondance(p);
						listMatchingKeypoints.add(p2);
					} else {
						listKeypoints.remove(exist);
						listMatchingKeypoints.remove(exist);
					}
				} else {
					if (exist == -1) {
						
					} else {
						if(memPoint == -1) {
							memPoint = exist;
						} else {
							Point p = new Point();
							p.x = memPoint;
							p.y = exist;
							listLines.add(p);
							memPoint = -1;
						}
					}
				}
				//float scale = (float) 640 / (float) currentScene.getWidth();
				Bitmap tmp = currentScene.copy(Bitmap.Config.ARGB_8888, true);
				Paint p = new Paint();
				p.setColor(Color.RED);
				p.setTextSize(15);
				p.setTextAlign(Align.LEFT);
				p.setTypeface(Typeface.DEFAULT);
				Canvas c = new Canvas(tmp);
				for(int i = 0; i < listKeypoints.size(); i++) {
					c.drawCircle(listKeypoints.get(i).x, listKeypoints.get(i).y,
							8, p);
				}
				for(int i = 0; i < listMatchingKeypoints.size(); i++) {
					c.drawCircle(listMatchingKeypoints.get(i).x,
							listMatchingKeypoints.get(i).y,
							8, p);
				}
				for(int i = 0; i < listLines.size(); i++) {
					c.drawText(computeDist(i) + "m",
							listKeypoints.get(listLines.get(i).y).x +
							(listKeypoints.get(listLines.get(i).x).x -
							listKeypoints.get(listLines.get(i).y).x) / 2, 
							listKeypoints.get(listLines.get(i).y).y +
							(listKeypoints.get(listLines.get(i).x).y -
							listKeypoints.get(listLines.get(i).y).y) / 2, p);
					c.drawLine(listKeypoints.get(listLines.get(i).x).x,
							listKeypoints.get(listLines.get(i).x).y,
							listKeypoints.get(listLines.get(i).y).x,
							listKeypoints.get(listLines.get(i).y).y, p);
				}
				view.setImageBitmap(tmp);
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (mode == DRAG) {
				if(Math.sqrt(Math.pow(event.getX() - start.x, 2) +  Math.pow(event.getY()
						- start.y, 2)) > 8) {
					hasMoved = true;
					matrix.set(savedMatrix);
					matrix.postTranslate(event.getX() - start.x, event.getY()
							- start.y);
				}
			} else if (mode == ZOOM && event.getPointerCount() == 2) {
				hasMoved = true;
				float newDist = spacing(event);
				matrix.set(savedMatrix);
				if (newDist > 10f) {
					float scale = newDist / oldDist;
					matrix.postScale(scale, scale, mid.x, mid.y);
				}
				if (lastEvent != null) {
					newRot = rotation(event);
					float r = newRot - d;
					matrix.postRotate(r, view.getMeasuredWidth() / 2,
							view.getMeasuredHeight() / 2);
				}
			}
			break;
		}

		view.setImageMatrix(matrix);

		return true;
	}

	private float rotation(MotionEvent event) {
		double delta_x = (event.getX(0) - event.getX(1));
		double delta_y = (event.getY(0) - event.getY(1));
		double radians = Math.atan2(delta_y, delta_x);

		return (float) Math.toDegrees(radians);
	}

	private float spacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	private void midPoint(PointF point, MotionEvent event) {
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}


	private Point findCorrespondance(Point p) {
		Mat frontImage = new Mat(), backImage = new Mat(), tpl, roi = new Mat();
		Utils.bitmapToMat(currentScene, frontImage);
		Utils.bitmapToMat(currentScene2, backImage);
		float scale = (float) frontImage.cols() / (float) 640;
		int sizeTpl = (int) (60 * scale);
		int startRow = (int) Math
				.max(p.y - sizeTpl / 2
						- Math.max(((p.y + sizeTpl / 2) - frontImage.rows()), 0),
						0);
		int startCol = (int) Math
				.max(p.x - sizeTpl / 2
						- Math.max(((p.x + sizeTpl / 2) - frontImage.cols()), 0),
						0);
		tpl = frontImage.submat(startRow, startRow + sizeTpl,
				startCol, startCol + sizeTpl);
		int startRowRoi, startColRoi;
		if(p.x < frontImage.cols() / 3 && p.y < frontImage.rows() / 3) {
			startRowRoi = startRow;
			startColRoi = startCol;
			roi = backImage.submat(startRowRoi,
					(int) (startRow + sizeTpl * 1.5), startColRoi,
					(int) (startCol + sizeTpl * 1.5));
		} else if(p.x > frontImage.cols() * 2 / 3 && p.y < frontImage.rows() / 3) {
			startRowRoi = startRow;
			startColRoi = startCol - sizeTpl;
			roi = backImage.submat(startRowRoi,
					(int) (startRow + sizeTpl * 1.5),
					startColRoi,
					(int) (startCol + sizeTpl * 1.5));	
		} else if(p.x < frontImage.cols() / 3 && p.y > frontImage.rows() * 2 / 3) {
			startRowRoi = startRow - sizeTpl;
			startColRoi = startCol;
			roi = backImage.submat(startRowRoi,
					(int) (startRow + sizeTpl * 1.5),
					startColRoi,
					(int) (startCol + sizeTpl * 1.5));
		} else if(p.x > frontImage.cols() * 2 / 3 && p.y > frontImage.rows() * 2 / 3) {
			startRowRoi = startRow - sizeTpl;
			startColRoi = startCol - sizeTpl;
			roi = backImage.submat(startRowRoi,
					(int) (startRow + sizeTpl * 1.5),
					startColRoi,
					(int) (startCol + sizeTpl * 1.5));
		} else {
			startRowRoi = startRow - sizeTpl;
			startColRoi = startCol - sizeTpl;
			roi = backImage.submat(startRowRoi,
					(int) (startRow + sizeTpl * 2),
					startColRoi,
					(int) (startCol + sizeTpl * 2));
		}
		
		Imgproc.resize(roi, roi,
				new Size(roi.size().width * 1.2, roi.size().height * 1.2));
		Highgui.imwrite(
				Environment.getExternalStorageDirectory().getPath() +
				"/Documents/roi.png", roi);
		Highgui.imwrite(
				Environment.getExternalStorageDirectory().getPath() +
				"/Documents/tpl.png", tpl);
		
		// / Create the result matrix
		int result_cols = roi.cols() - tpl.cols() + 1;
		int result_rows = roi.rows() - tpl.rows() + 1;
		Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

		// / Do the Matching and Normalize
		int match_method = Imgproc.TM_CCOEFF;
		Imgproc.matchTemplate(roi, tpl, result, match_method);

		// / Localizing the best match with minMaxLoc
		MinMaxLocResult mmr = Core.minMaxLoc(result);

		Point matchLoc = new Point();
		if (match_method == Imgproc.TM_SQDIFF
				|| match_method == Imgproc.TM_SQDIFF_NORMED) {
			matchLoc.x = (int) (mmr.minLoc.x / 1.2);
			matchLoc.y = (int) (mmr.minLoc.y / 1.2);
		} else {
			matchLoc.x = (int) (mmr.maxLoc.x / 1.2);
			matchLoc.y = (int) (mmr.maxLoc.y / 1.2);
		}
		matchLoc.x += startColRoi + tpl.width() / 2.4 -
				Math.max(sizeTpl / 2 - p.x, 0) +
				Math.max(sizeTpl / 2 - (frontImage.cols() - p.x - 1), 0);
		matchLoc.y += startRowRoi + tpl.height() / 2.4 -
				Math.max(sizeTpl / 2 - p.y, 0) +
				Math.max(sizeTpl / 2 - (frontImage.rows() - p.y - 1), 0);

		return matchLoc;
	}
	
	private int isExisting(float[] pts, ImageView view, float scaleX, float scaleY) {
		for (int i = 0; i < listKeypoints.size(); i++) {
			if (Math.sqrt(
					Math.pow(listKeypoints.get(i).x - pts[0], 2) +
					Math.pow(listKeypoints.get(i).y - pts[1], 2)) *
					640 / (float) currentScene.getWidth() < 60) {
				return i;
			}
		}
		return -1;
	}

	private float computeDist(int ind) {
		double distAccelero = Tutorial3Activity.getDistanceInMeter()[2];
		float focalLength = Tutorial3Activity.getFocalLength();
		double dist1 = Math.sqrt(Math.pow(listKeypoints.get(listLines.get(ind).x).x -
				listKeypoints.get(listLines.get(ind).y).x, 2) +
				Math.pow(listKeypoints.get(listLines.get(ind).x).y -
				listKeypoints.get(listLines.get(ind).y).y, 2));
		double dist2 = Math.sqrt(Math.pow(listMatchingKeypoints.get(listLines.get(ind).x).x -
				listMatchingKeypoints.get(listLines.get(ind).y).x, 2) +
				Math.pow(listMatchingKeypoints.get(listLines.get(ind).x).y -
						listMatchingKeypoints.get(listLines.get(ind).y).y, 2));
		Toast.makeText(this, distAccelero + " " + focalLength + " " + dist1 + " " + dist2,
				Toast.LENGTH_LONG).show();
		return (float) (distAccelero * dist1 * dist2 / (focalLength * (dist1 - dist2)));
	}
}