package com.example.kaos2;

import java.io.File;
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener{


	private Sensor gyroscopeSensor, accelerometerSensor;
	private SensorManager sensorManager;
	private TextView gyroscopeText, accelerationText;
	private static final String TAG = "KAOSSSSSSSSSSSS";
	private PdUiDispatcher dispatcher;
	private int fade;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_FASTEST);

		gyroscopeText = (TextView) findViewById(R.id.gyroscope);
		accelerationText = (TextView) findViewById(R.id.acceleration);

		//Touch parameters
		RelativeLayout myLayout = 
				(RelativeLayout)findViewById(R.id.RelativeLayout1);

		myLayout.setOnTouchListener(
				new RelativeLayout.OnTouchListener() {
					public boolean onTouch(View v, MotionEvent m) {
						handleTouch(m);			
						return true;
					}
				}
				);

		try {
			initPd();
			loadPatch();
			modifyPitch(10);
			modifyVolume(10);
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			finish();
		}

	}

	private void initPd() throws IOException {
		// Configure the audio glue
		int sampleRate = AudioParameters.suggestSampleRate();
		PdAudio.initAudio(sampleRate, 0, 2, 8, true);

		// Create and install the dispatcher
		dispatcher = new PdUiDispatcher();
		PdBase.setReceiver(dispatcher);
	}

	private void loadPatch() throws IOException {
		File dir = getFilesDir();

		IoUtils.extractZipResource(
				getResources().openRawResource(R.raw.soundtest2), dir, true);

		File patchFile = new File(dir, "soundtest2.pd");
		PdBase.openPatch(patchFile.getAbsolutePath());
	}

	@Override
	protected void onResume() {
		super.onResume();
		PdAudio.startAudio(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		PdAudio.stopAudio();
	}

	private void triggerUp(){
		PdBase.sendFloat("up", fade);
	}
	
	private void triggerUp2(){
		PdBase.sendFloat("up2", fade);
	}

	private void triggerDown() {
		PdBase.sendBang("down");
	}
	
	private void triggerDown2() {
		PdBase.sendBang("down2");
	}

	private void modifySignalWithXYValue(int x, int y) {
		PdBase.sendFloat("xvalue", x);
		PdBase.sendFloat("yvalue", y);
	}

	private void modifySignalWithXYValue2(int x2, int y2) {
		PdBase.sendFloat("x2value", x2);
		PdBase.sendFloat("y2value", y2);
	}
	
	private void modifyPitch(int p) {
		PdBase.sendFloat("pitch", p);
	}
	
	private void modifyVolume(int vol) {
		PdBase.sendFloat("volume", vol);
	}

	public void handleTouch(MotionEvent m)
	{
		TextView textView4 = (TextView)findViewById(R.id.textView4);
		TextView textView5 = (TextView)findViewById(R.id.textView5);

		int pointerCount = m.getPointerCount();

		for (int i = 0; i < pointerCount; i++)
		{
			int x = (int) (m.getX(i)/72)+15;
			int y = (int) (10-(m.getY(i)/128)+25);
			int fingerId = m.getPointerId(i);
			int action = m.getActionMasked();
			
			String actionString;

			switch (action)
			{
			case MotionEvent.ACTION_DOWN:
				if(fingerId==0) {
					modifySignalWithXYValue(x, y);
					triggerDown();
				} 
				if(fingerId!=0) { 
					modifySignalWithXYValue2(x, y);
					triggerDown2();
				}
				actionString = "DOWN";
				break;
			case MotionEvent.ACTION_UP:
				if(fingerId==0)
					triggerUp();
				if(fingerId!=0)
					triggerUp2();
				actionString = "UP";
				break;	
			case MotionEvent.ACTION_POINTER_DOWN:
				actionString = "PNTR DOWN";
				break;
			case MotionEvent.ACTION_POINTER_UP:
				actionString = "PNTR UP";
				break;
			case MotionEvent.ACTION_MOVE:
				if(fingerId==0) 
					modifySignalWithXYValue(x, y);
				if(fingerId!=0)
					modifySignalWithXYValue2(x, y);
				actionString = "MOVE";
				break;
			default:
				actionString = "";
			}
			String touchStatus = "Action: " + actionString + " Finger ID: " + fingerId + " X: " + x + " Y: " + y;
			if (fingerId == 0)
				textView4.setText(touchStatus);
			else
				textView5.setText(touchStatus);
		}
	}


	@Override
	public void onDestroy() {
		super.onDestroy();
		PdAudio.release();
		PdBase.release();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE) {
			//Recalc to degrees instead of radians
			float xDegreePerSeconds = Math.round(event.values[0]*180/(float)Math.PI);
			float yDegreePerSeconds = Math.round(event.values[1]*180/(float)Math.PI);
			float zDegreePerSeconds = Math.round(event.values[2]*180/(float)Math.PI);
			gyroscopeText.setText("X Degree/s: "+xDegreePerSeconds+"\nY Degree/s:  "+yDegreePerSeconds+"\nZ Degree/s:  "+zDegreePerSeconds);
		} else if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
			float xAccel=Math.round(event.values[0]);
			float yAccel=Math.round(event.values[1]);
			float zAccel=Math.round(event.values[2]);
			fade = (int)(10-yAccel);
			modifyPitch((int)(xAccel/2)+12);// 12 coz of -10 < x < 10
			accelerationText.setText("X [m/s^2]: "+ xAccel +"\nY [m/s^2]: "+yAccel+"\n Z [m/s^2]: "+zAccel);
		}
	}

}
