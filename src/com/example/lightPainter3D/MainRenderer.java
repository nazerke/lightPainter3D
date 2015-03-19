package com.example.lightPainter3D;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import com.example.myloader.R;
import rajawali.BaseObject3D;
import rajawali.Geometry3D;
import rajawali.animation.Animation3D;
import rajawali.bounds.BoundingBox;
import rajawali.lights.PointLight;
import rajawali.math.Number3D;
import rajawali.math.Number3D.Axis;
import rajawali.math.Quaternion;
import rajawali.parser.ObjParser;
import rajawali.renderer.RajawaliRenderer;
import android.content.Context;
import android.opengl.Matrix;

public class MainRenderer extends RajawaliRenderer {
	private PointLight mLight;
	private BaseObject3D mObjectGroup;
	private float frameRate = 30;

	public MainRenderer(Context context) {
		super(context);
		setRate(frameRate);

		mLight = new PointLight();
		mLight.setPosition(0, 0, -10);
		mLight.setPower(3);

		mCamera.setPosition(0, 0, -5);
		mCamera.setLookAt(0, 0, 0);
		mCamera.setUpAxis(0, 1, 0);

		ObjParser objParser = new ObjParser(mContext.getResources(), mTextureManager,R.raw.monkey_obj);
		objParser.parse();
		mObjectGroup = objParser.getParsedObject();

		mObjectGroup.addLight(mLight);
		mObjectGroup.rotateAround(Number3D.getAxisVector(Axis.X),90);

		addChild(mObjectGroup);

		findCenterOfBB();
	}


	public Quaternion getInitialOrientation()
	{
		return mObjectGroup.getOrientation();
	}
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		super.onSurfaceCreated(gl, config);
	}

	float initialFar = 4.2f;
	float far = initialFar;
	float initialNear = 3.1f;
	float near = initialNear;
	float increment =0.05f / 3; 
	float farLimit = 6.7f;

	public void onDrawFrame(GL10 glUnused) {

		if(far<farLimit)
		{far+=increment;
		near += increment;
		}
		else{far = initialFar; near = initialNear;}
		if(near<far){
			mCamera.setNearPlane(near);
			mCamera.setFarPlane(far);
		}
		mCamera.setProjectionMatrix(mViewportWidth, mViewportHeight);
		super.onDrawFrame(glUnused);
	}

	public float getFarLimit()
	{return (float) (Math.round(farLimit*100.0)/100.0);}

	public void setFarLimit(float newfarLimit)
	{   
		if(newfarLimit>initialFar){
			far = initialFar; near =initialNear;
			farLimit = (float) (Math.round(newfarLimit*100.0)/100.0);
		}
	}
	public void setInitNear(float newinitialNear)//done
	{
		if(initialNear<initialFar){
			initialNear = (float) (Math.round(newinitialNear*100.0)/100.0);
			far = initialFar; near =initialNear;
		}
	}
	public float getInitNear()
	{return (float) (Math.round(initialNear*100.0)/100.0); }

	public float getRate()
	{return frameRate;}

	public void setRate(float frate)
	{
		frameRate= frate;
		setFrameRate(frate);
	}
	public float getSlice()
	{
		return  (float) (Math.round((initialFar - initialNear)*100.0)/100.0);
	}
	public void setFar(float mFar)
	{
		if(mFar>initialNear&&mFar<farLimit){
			initialFar = (float) (Math.round(mFar*100.0)/100.0);
			far = initialFar; near =initialNear;
		}
	}
	public float getFar()
	{ return (float) (Math.round(initialFar*100.0)/100.0);
	}

	public void findCenterOfBB()
	{
		Geometry3D anObject = mObjectGroup.getGeometry();
		BoundingBox aBox = anObject.getBoundingBox();
		Number3D min = aBox.getMin();
		Number3D max = aBox.getMax();

		float averageX = (min.x+max.x)/2;
		float averageY = (min.y+max.y)/2;
		float averageZ = (min.z+max.z)/2;

		mObjectGroup.setPosition(-averageX,-averageY,-averageZ);

		float bbWidthx = max.x - min.x;
		float bbHeighty = max.y - min.y;
		float bbDepthz = max.z - min.z;

		float scaleFactorx = 2/bbWidthx;
		float scaleFactory = 2/bbHeighty;
		float scaleFactorz = 2/bbDepthz;

		float maxScale = Math.min(Math.min(scaleFactory, scaleFactorz),scaleFactorx);

		mObjectGroup.setScale(maxScale,maxScale,maxScale);
	}

	public void setQuat(Quaternion quat)
	{
		mObjectGroup.setOrientation(quat);
	}
}
