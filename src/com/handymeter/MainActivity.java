package com.handymeter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class MainActivity extends Activity implements
		CvCameraViewListener2, SensorEventListener {
	private static final String TAG = "OCVSample::Activity";

	private MainView mOpenCvCameraView;
	private Mat lastImage;
	private static Mat firstImage, secondImage;
	private SensorManager sensorManager;
	private static double[] distanceInMeter = new double[3];
	private double[] lastAcceleration = new double[3];
	private double[] gravity = new double[3];
	private double[] linearAcceleration = new double[3];
	private static float mFocalLength = 333.10f;
	private final double alpha = 0.8;

	private boolean firstTimeOfMeasurement = false;
	private boolean hasLinear = false;
	// private List<Point> mListePoints = new ArrayList<Point>();

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				// mOpenCvCameraView.setOnTouchListener(Tutorial3Activity.this);
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	public MainActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.main_surface_view);

		mOpenCvCameraView = (MainView) findViewById(R.id.main_activity_java_surface_view);

		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

		mOpenCvCameraView.setCvCameraViewListener(this);

		findViewById(R.id.ccam).setOnClickListener(mCCamListener);

		findViewById(R.id.ss_recording)
				.setOnClickListener(mSSRecordingListener);

		findViewById(R.id.mob_drone).setOnClickListener(mMobDroneListener);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	public void onCameraViewStarted(int width, int height) {
	}

	public void onCameraViewStopped() {
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		return (lastImage = inputFrame.rgba());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1, 0, Menu.NONE, getString(R.string.omodel));
		menu.add(1, 0, Menu.NONE, getString(R.string.calibrate));
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		if (item.getItemId() == 1) {
		} else if (item.getItemId() == 2) {
		}
		return true;
	}

	View.OnClickListener mCCamListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

		}
	};

	View.OnClickListener mSSRecordingListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			ImageView im = ((ImageView) v);
			if (im.getContentDescription().toString()
					.equals(getString(R.string.startr))) {
				im.setContentDescription(getString(R.string.stopr));
				im.setImageDrawable(getResources()
						.getDrawable(R.drawable.stopr));
				firstImage = lastImage.clone();
				firstTimeOfMeasurement = true;

				sensorManager.registerListener(MainActivity.this,
						sensorManager
								.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_FASTEST);
				
				sensorManager.registerListener(MainActivity.this,
						sensorManager
								.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
						SensorManager.SENSOR_DELAY_FASTEST);
/*
				sensorManager.registerListener(Tutorial3Activity.this,
						sensorManager
								.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
						SensorManager.SENSOR_DELAY_FASTEST);*/
			} else {
				im.setContentDescription(getString(R.string.startr));
				im.setImageDrawable(getResources().getDrawable(
						R.drawable.startr));
				secondImage = lastImage.clone();
				sensorManager.unregisterListener(MainActivity.this);
				if (distanceInMeter[2] < 0) {
					distanceInMeter[2] = -distanceInMeter[2];
					Mat tmp = firstImage;
					firstImage = secondImage;
					secondImage = tmp;
				}
				Intent intent = new Intent(MainActivity.this,
						MeasurementsActivity.class);
				startActivity(intent);
			}
		}
	};

	View.OnClickListener mMobDroneListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {

		}
	};

	private double[] speed = new double[3];
	private long timeStamp;
	double dT;

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
			hasLinear = true;
			if (firstTimeOfMeasurement) {
				firstTimeOfMeasurement = false;
				timeStamp = event.timestamp;

				gravity[0] = event.values[0];
				gravity[1] = event.values[1];
				gravity[2] = event.values[2];
				speed[0] = speed[1] = speed[2] = 0;
				lastAcceleration[0] = lastAcceleration[1] = lastAcceleration[2] = 0;
				linearAcceleration[0] = linearAcceleration[1] = linearAcceleration[2] = 0;
				distanceInMeter[0] = distanceInMeter[1] = distanceInMeter[2] = 0;

			} else {
				dT = (event.timestamp - timeStamp) / 1000000000.0d;
				timeStamp = event.timestamp;
				calculateDistance2(event.values, dT);
			}
		} else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !hasLinear) {
			if (firstTimeOfMeasurement) {
				firstTimeOfMeasurement = false;
				timeStamp = event.timestamp;

				gravity[0] = event.values[0];
				gravity[1] = event.values[1];
				gravity[2] = event.values[2];
				speed[0] = speed[1] = speed[2] = 0;
				lastAcceleration[0] = lastAcceleration[1] = lastAcceleration[2] = 0;
				linearAcceleration[0] = linearAcceleration[1] = linearAcceleration[2] = 0;
				distanceInMeter[0] = distanceInMeter[1] = distanceInMeter[2] = 0;

			} else {
				dT = (event.timestamp - timeStamp) / 1000000000.0d;
				timeStamp = event.timestamp;
				calculateDistance(event.values, dT);
			}
		}
	}

	private void calculateDistance(float[] acceleration, double deltaTime) {
		for (int i = 0; i < acceleration.length; i++) {
			gravity[i] = alpha * gravity[i] + (1 - alpha) * acceleration[i];
			linearAcceleration[i] = acceleration[i] - gravity[i];
			double newSpeed = speed[i]
					+ (linearAcceleration[i] + lastAcceleration[i]) * deltaTime
					/ 2;
			distanceInMeter[i] += (newSpeed + speed[i]) * deltaTime / 2;
			speed[i] = newSpeed;
			lastAcceleration[i] = linearAcceleration[i];
		}
	}

	private void calculateDistance2(float[] linearAcceleration, double deltaTime) {
		for (int i = 0; i < linearAcceleration.length; i++) {
			double newSpeed = speed[i]
					+ (linearAcceleration[i] + lastAcceleration[i]) * deltaTime
					/ 2;
			distanceInMeter[i] += (newSpeed + speed[i]) * deltaTime / 2;
			speed[i] = newSpeed;
			lastAcceleration[i] = linearAcceleration[i];
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	public static Mat getFirstImage() {
		return firstImage;
	}

	public static Mat getSecondImage() {
		return secondImage;
	}

	public static double[] getDistanceInMeter() {
		return distanceInMeter;
	}

	public static float getFocalLength() {
		return mFocalLength;
	}

	/*
	 * @SuppressLint("SimpleDateFormat")
	 * 
	 * @Override public boolean onTouch(View v, MotionEvent event) {
	 * Log.i(TAG,"onTouch event"); SimpleDateFormat sdf = new
	 * SimpleDateFormat("yyyy-MM-dd_HH-mm-ss"); String currentDateandTime =
	 * sdf.format(new Date()); String fileName =
	 * Environment.getExternalStorageDirectory().getPath() + "/sample_picture_"
	 * + currentDateandTime + ".jpg"; mOpenCvCameraView.takePicture(fileName);
	 * Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
	 * return false; }
	 */
}
