package com.example.lightPainter3D;

import java.util.ArrayList;
import rajawali.math.Quaternion;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import rajawali.RajawaliActivity;;

public class MainActivity extends RajawaliActivity implements OnClickListener,TextWatcher,SensorEventListener{
	private MainRenderer mRenderer;
	Button farPlus,farMinus,speedPlus,speedMinus,initnearPlus,initfarPlus,initnearMinus,initfarMinus;
	TextView nearValue, farValue,slice, speed;
	LinearLayout mLinearLayout, mLinearLayoutTop;
	SensorManager mSensorManager;
	Sensor mRotVectSensor,mLinearAccelSensor;

	Quaternion quatFinal, quatInit,offsetQuaternion;
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		mRenderer = new MainRenderer(this);      
		mRenderer.setSurfaceView(mSurfaceView);
		super.setRenderer(mRenderer);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mRotVectSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		mLinearAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

		setupLayout();
	}
	@Override
	protected void onResume()
	{
		super.onResume();

		mSensorManager.registerListener(this, mRotVectSensor, 10000);
		mSensorManager.registerListener(this, mLinearAccelSensor,  SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		mSensorManager.unregisterListener(this,mRotVectSensor);
		mSensorManager.unregisterListener(this,mLinearAccelSensor);

		writeDataToFile();
	}

	private float[] lowFrequency = new float[3];
	private static final float ALPHA = 0.8f;

	private float[] highPass(float x, float y, float z)
	{
		float[] filteredValues = new float[3];

		lowFrequency[0] = ALPHA * lowFrequency[0] + (1 - ALPHA) * x;
		lowFrequency[1] = ALPHA * lowFrequency[1] + (1 - ALPHA) * y;
		lowFrequency[2] = ALPHA * lowFrequency[2] + (1 - ALPHA) * z;

		filteredValues[0] = x - lowFrequency[0];
		filteredValues[1] = y - lowFrequency[1];
		filteredValues[2] = z - lowFrequency[2];

		//window filter
		if(filteredValues[0]<=0.02&&filteredValues[0]>=-0.02)
		{filteredValues[0]=0.0f;}
		if(filteredValues[1]<=0.02&&filteredValues[1]>=-0.02)
		{filteredValues[1]=0.0f;}
		if(filteredValues[2]<=0.02&&filteredValues[2]>=-0.02)
		{filteredValues[2]=0.0f;}

		return filteredValues;
	}


	private int count = 0;
	private float[] offset,accelVals;
	private float Q[] = new float[4];
	private float finalSpeed,tempHolder,timestampHolder = 0.0f;
	ArrayList<Float> timeValues = new ArrayList<Float>();
	ArrayList<float[]> calibrated = new ArrayList<float[]>();

	int count2= 0;
	@SuppressLint("NewApi")
	@Override
	public void onSensorChanged(SensorEvent event) {

		switch (event.sensor.getType())
		{

		case Sensor.TYPE_LINEAR_ACCELERATION:
			count++;
			float currentTimestamp = event.timestamp ;//nanoseconds
			float timeInterval= (currentTimestamp - timestampHolder)/1000000000;//seconds 

			timeValues.add(tempHolder);

			if(timestampHolder!=0.0f){
				tempHolder+=timeInterval;}

			timestampHolder = currentTimestamp;


			//to calibrate

			if(count>25&&count<=126){
				calibrated.add(event.values.clone());
				if(count==100)
				{	offset = calibrate(calibrated);}
			}

			// when calibrated offset is available 
			if(count>126){

				//high pass filter
				accelVals = highPass(event.values[0]-offset[0],event.values[1]-offset[1],event.values[2]-offset[2]);

				//find v = v0 + at in 3 dimensions
				float velocityX = findXVelocity(timeInterval,accelVals[0]);
				float velocityZ = findZVelocity(timeInterval,accelVals[2]);
				float  velocityY = findYVelocity(timeInterval,accelVals[1]);

				finalSpeed = velocityX*velocityX +velocityY*velocityY+velocityZ*velocityZ;
				finalSpeed = (float) Math.sqrt(finalSpeed);

				//find d = d0 + v0*t
				float positionX = findXPosition(timeInterval,velocityX,accelVals[0]);
				float positionY = findYPosition(timeInterval,velocityY,accelVals[1]);
				float positionZ = findZPosition(timeInterval,velocityZ,accelVals[2]);

				float finalPosition = positionX*positionX+positionY*positionY+positionZ*positionZ;
				finalPosition = (float)Math.sqrt(finalPosition);

				saveDataToPlotGraph(tempHolder, event.values,offset,accelVals,velocityX,velocityZ,velocityY,finalSpeed,positionX,positionY,positionZ,finalPosition);
			}
			break;

		case Sensor.TYPE_ROTATION_VECTOR:

			SensorManager.getQuaternionFromVector(Q, event.values);

			Q[0]= (float) (Math.round(Q[0]*100000.0)/100000.0); 
			Q[1]= (float) (Math.round(Q[1]*100000.0)/100000.0);
			Q[2]=(float)  (Math.round(Q[2]*100000.0)/100000.0);
			Q[3]= (float) (Math.round(Q[3]*100000.0)/100000.0);

			Quaternion forLaterUse = new Quaternion(Q[0],Q[1],Q[2],Q[3]);
			quatFinal = new Quaternion(Q[0],Q[1],Q[2],Q[3]);

			quatFinal = quatFinal.inverse();
			mRenderer.setQuat(quatFinal);

			///uncomment if the initial rotation needed
			/*if(startSensing ==true)
			{
				if(firstTimeAfterTap == 1)
				{
				offsetQuaternion = mRenderer.getInitialOrientation();}

				else if(firstTimeAfterTap>1)
				{
					Quaternion differenceOfRotation = findDifferenceOfRotation(quatInit,quatFinal);
					differenceOfRotation.inverseSelf();
				offsetQuaternion.multiply(differenceOfRotation);
				}		

			quatInit = forLaterUse;		
			firstTimeAfterTap++;
			mRenderer.setQuat(offsetQuaternion);
			}*/
		}
	}

	float initPosition = 0.0f;
	private float findXPosition(float timeInterval, float velocityX, float acceleration) {
		float position = initPosition+velocityX*timeInterval;
		initPosition = position;
		return position;
	}

	float initPositionY;
	private float findYPosition(float timeInterval, float velocityY, float acceleration) {
		float position = initPositionY+velocityY*timeInterval;
		initPositionY = position;
		return position;
	}

	float initPositionZ;
	private float findZPosition(float timeInterval, float velocityZ, float acceleration) {
		float position = initPositionZ+velocityZ*timeInterval;
		initPositionZ = position;
		return position;
	}

	ArrayList<float[]> rawAx= new ArrayList<float[]>();
	ArrayList<float[]> calibratedAx= new ArrayList<float[]>();
	ArrayList<float[]> highpassedAx= new ArrayList<float[]>();
	ArrayList<float[]> velocityXList= new ArrayList<float[]>();

	ArrayList<float[]> rawAz= new ArrayList<float[]>();
	ArrayList<float[]> calibratedAz= new ArrayList<float[]>();
	ArrayList<float[]> highpassedAz= new ArrayList<float[]>();
	ArrayList<float[]> velocityZList= new ArrayList<float[]>();

	ArrayList<float[]> rawAy= new ArrayList<float[]>();
	ArrayList<float[]> calibratedAy= new ArrayList<float[]>();
	ArrayList<float[]> highpassedAy= new ArrayList<float[]>();
	ArrayList<float[]> velocityYList= new ArrayList<float[]>();

	ArrayList<float[]> velocityTotalList= new ArrayList<float[]>();

	ArrayList<float[]> positionXList= new ArrayList<float[]>();
	ArrayList<float[]> positionYList= new ArrayList<float[]>();
	ArrayList<float[]> positionZList= new ArrayList<float[]>();
	ArrayList<float[]> finalPositionList= new ArrayList<float[]>();

	private void saveDataToPlotGraph(float tempHolder2, float[] raw,
			float[] offset2, float[] accelVals2, float velocityX,float velocityZ,float velocityY,float finalSpeed,
			float positionX,float positionY,float positionZ,float finalPosition) {

		// all we need to plot graph of acceleration_X
		float[] time_ax = {tempHolder2,raw[0]};
		rawAx.add(time_ax);
		float[] time_calibrated_ax = {tempHolder2,raw[0]-offset2[0]};
		calibratedAx.add(time_calibrated_ax);
		float[] highpassed_ax = {tempHolder2,accelVals2[0]};
		highpassedAx.add(highpassed_ax);

		float[] velocityX_time = {tempHolder2,velocityX};
		velocityXList.add(velocityX_time);

		// all we need to plot graph of acceleration_Z
		float[] time_az = {tempHolder2,raw[2]};
		rawAz.add(time_az);
		float[] time_calibrated_az = {tempHolder2,raw[2]-offset2[2]};
		calibratedAz.add(time_calibrated_az);
		float[] highpassed_az = {tempHolder2,accelVals2[2]};
		highpassedAz.add(highpassed_az);

		float[] velocityZ_time = {tempHolder2,velocityZ};
		velocityZList.add(velocityZ_time);

		// all we need to plot graph of acceleration_Y
		float[] time_ay = {tempHolder2,raw[1]};
		rawAy.add(time_ay);
		float[] time_calibrated_ay = {tempHolder2,raw[1]-offset2[1]};
		calibratedAy.add(time_calibrated_ay);
		float[] highpassed_ay = {tempHolder2,accelVals2[1]};
		highpassedAy.add(highpassed_ay);

		float[] velocityY_time = {tempHolder2,velocityY};
		velocityYList.add(velocityY_time);

		float[] velocityTotal_time = {tempHolder2,finalSpeed};
		velocityTotalList.add(velocityTotal_time);


		float [] positionX_time = {tempHolder2,positionX};
		positionXList.add(positionX_time);

		float [] positionY_time = {tempHolder2,positionY};
		positionYList.add(positionY_time);

		float [] positionZ_time = {tempHolder2,positionZ};
		positionZList.add(positionZ_time);

		float [] finalPosition_time = {tempHolder2,finalPosition};
		finalPositionList.add(finalPosition_time);}



	private float[] calibrate(ArrayList<float[]> rawValues) {

		float offsets[] = {0.0f,0.0f,0.0f};
		for(float[] array: rawValues)
		{
			offsets[0]+=array[0];
			offsets[1]+=array[1];
			offsets[2]+=array[2];
		}
		int numberOfSamples=rawValues.size();
		offsets[0]/=numberOfSamples;
		offsets[1]/=numberOfSamples;
		offsets[2]/=numberOfSamples;

		return offsets;
	}

	float initialVelocityZ = 0.0f;
	private float findZVelocity(float timeInterval, float f) {

		float finalVelocity = initialVelocityZ+ f*timeInterval;
		initialVelocityZ = finalVelocity;
		return finalVelocity;
	}

	float initialVelocityY = 0.0f;
	private float findYVelocity(float timeInterval, float f) {
		float finalVelocity = initialVelocityY+ f*timeInterval;
		initialVelocityY = finalVelocity;
		return finalVelocity;
	}

	float initialVelocityX = 0.0f;
	private float findXVelocity(float timeInterval, float f) {
		float finalVelocity = initialVelocityX+ f*timeInterval;
		initialVelocityX = finalVelocity;
		return finalVelocity;
	}

	boolean startSensing=false;
	int firstTimeAfterTap=0;
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		startSensing = true;
		firstTimeAfterTap=1;
		quatInit = quatFinal;
		return super.onTouchEvent(event);
	}
	private Quaternion findDifferenceOfRotation(Quaternion initialOrientation,Quaternion finalOrientation)
	{
		initialOrientation.inverseSelf();
		finalOrientation.multiply(initialOrientation);
		return finalOrientation;
	}

	@Override
	public void onClick(View pressedButton) {
		switch (((Button)pressedButton).getId()){
		case 0:			//slicePlus
			mRenderer.setFar(mRenderer.getFar()+0.05f);
			slice.setText("Slice: "+mRenderer.getSlice());
			break;

		case 1: 			//sliceMinus
			if(mRenderer.getFar()>2.1f){
				mRenderer.setFar(mRenderer.getFar()-0.05f);
				slice.setText("Slice: "+mRenderer.getSlice());
			}
			break;
		case 2: 
			mRenderer.setRate(mRenderer.getRate()+5);
			speed.setText("FPS: "+mRenderer.getRate());
			break;
		case 3:
			if(mRenderer.getRate()>5)
			{
				mRenderer.setRate(mRenderer.getRate()-5);
				speed.setText("FPS: "+mRenderer.getRate());
			}
			break;
		case 4:
			mRenderer.setFarLimit(mRenderer.getFarLimit()+0.1f);
			farValue.setText("zFar: "+mRenderer.getFarLimit());
			break;

		case 5:
			mRenderer.setFarLimit(mRenderer.getFarLimit()-0.1f);
			farValue.setText("zFar: "+mRenderer.getFarLimit());
			break;

		case 6: //increase initial z near

			mRenderer.setInitNear(mRenderer.getInitNear()+0.1f);
			nearValue.setText("zNear: "+mRenderer.getInitNear());

			break;
		case 7://decrease initial z near
			mRenderer.setInitNear(mRenderer.getInitNear()-0.1f);
			nearValue.setText("zNear: "+mRenderer.getInitNear());
			break;
		} 
	} 

	GraphHelper graphHelper;
	private void writeDataToFile() {
		graphHelper = new GraphHelper(getApplication());
		graphHelper.writeToFile("rawAx.txt",rawAx);
		graphHelper.writeToFile("calibratedAx.txt",calibratedAx);
		graphHelper.writeToFile("highpassedAx.txt",highpassedAx);
		graphHelper.writeToFile("velocity_x.txt",velocityXList);

		graphHelper.writeToFile("rawAy.txt", rawAy);
		graphHelper.writeToFile("calibratedAy.txt", calibratedAy);
		graphHelper.writeToFile("highpassedAy.txt", highpassedAy);
		graphHelper.writeToFile("velocity_y.txt",velocityYList);

		graphHelper.writeToFile("rawAz.txt",rawAz);
		graphHelper.writeToFile("calibratedAz.txt",calibratedAz);
		graphHelper.writeToFile("highpassedAz.txt",highpassedAz);
		graphHelper.writeToFile("velocity_z.txt",velocityZList);

		graphHelper.writeToFile("velocity_total.txt",velocityTotalList);

		graphHelper.writeToFile("positionX.txt",positionXList);
		graphHelper.writeToFile("positionY.txt",positionYList);
		graphHelper.writeToFile("positionZ.txt",positionZList);
		graphHelper.writeToFile("finalPosition.txt",finalPositionList);

	}

	public void setupLayout()
	{		
		mLinearLayoutTop = new LinearLayout(this);
		mLinearLayoutTop.setOrientation(LinearLayout.HORIZONTAL);
		mLinearLayoutTop.setGravity(Gravity.TOP);
		mLinearLayoutTop.setVisibility(LinearLayout.INVISIBLE);

		nearValue = new TextView(this);
		nearValue.setTextColor(Color.RED);
		nearValue.setText("Znear: "+ mRenderer.near+" ");
		mLinearLayoutTop.addView(nearValue);

		initnearPlus = new Button(this);
		initnearPlus.setId(6);
		initnearPlus.setOnClickListener(this);
		initnearPlus.setText("+");
		initnearPlus.setTextSize(10);
		mLinearLayoutTop.addView(initnearPlus);


		initnearMinus = new Button(this);
		initnearMinus.setId(7);
		initnearMinus.setOnClickListener(this);
		initnearMinus.setText("-");
		initnearMinus.setTextSize(10);
		mLinearLayoutTop.addView(initnearMinus);

		farValue = new TextView(this);
		farValue.setTextColor(Color.RED);
		farValue.setText("Zfar: "+ mRenderer.getFarLimit());
		mLinearLayoutTop.addView(farValue);

		initfarPlus = new Button(this);
		initfarPlus.setId(4);
		initfarPlus.setOnClickListener(this);
		initfarPlus.setText("+");
		initfarPlus.setTextSize(10);
		mLinearLayoutTop.addView(initfarPlus);


		initfarMinus = new Button(this);
		initfarMinus.setId(5);
		initfarMinus.setOnClickListener(this);
		initfarMinus.setText("-");
		initfarMinus.setTextSize(10);
		mLinearLayoutTop.addView(initfarMinus);

		mLinearLayout = new LinearLayout(this);
		mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		mLinearLayout.setGravity(Gravity.BOTTOM);
		mLinearLayout.setVisibility(LinearLayout.INVISIBLE);

		slice = new TextView(this);
		slice.setTextColor(Color.RED);
		slice.addTextChangedListener(this);
		slice.setText("Slice: "+ mRenderer.getSlice());
		mLinearLayout.addView(slice);

		farPlus = new Button(this);
		farPlus.setId(0);
		farPlus.setOnClickListener(this);
		farPlus.setText("+");
		farPlus.setTextSize(10);
		mLinearLayout.addView(farPlus);

		farMinus = new Button(this);
		farMinus.setId(1);
		farMinus.setOnClickListener(this);
		farMinus.setText("-");
		farMinus.setTextSize(10);
		mLinearLayout.addView(farMinus);

		speed = new TextView(this);
		speed.setTextColor(Color.RED);
		speed.addTextChangedListener(this);
		speed.setText("FPS: "+mRenderer.getRate());
		mLinearLayout.addView(speed);

		speedPlus = new Button(this);
		speedPlus.setId(2);
		speedPlus.setOnClickListener(this);
		speedPlus.setText("+");
		speedPlus.setTextSize(10);
		mLinearLayout.addView(speedPlus);

		speedMinus = new Button(this);
		speedMinus.setId(3);
		speedMinus.setOnClickListener(this);
		speedMinus.setText("-");
		speedMinus.setTextSize(10);
		mLinearLayout.addView(speedMinus);

		mLayout.addView(mLinearLayout);
		mLayout.addView(mLinearLayoutTop);
		mLinearLayout.setVisibility(LinearLayout.VISIBLE);	
		mLinearLayoutTop.setVisibility(LinearLayout.VISIBLE);

	}
	@Override
	public void afterTextChanged(Editable arg0) {
		// TODO Auto-generated method stub

	}
	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}
}

