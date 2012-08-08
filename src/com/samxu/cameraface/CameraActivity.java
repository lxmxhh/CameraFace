package com.samxu.cameraface;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.PictureCallback;


public class CameraActivity extends Activity {
	
    private static Camera mSingletonCamera = null;
    private Button mOpenButton = null;
    private Button mCloseButton = null;
    private Button mTakePictureButton = null;
    private CameraPreview mCameraPreview = null;
    private static final String TAG = "myCameraTag";
    
    private boolean bIsPreview = false;
	protected static final String strCaptureFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+ "/camera_snap.jpg";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);

        
        mOpenButton = (Button) findViewById(R.id.button1);
        mCloseButton = (Button) findViewById(R.id.button2);
        mTakePictureButton = (Button) findViewById(R.id.button3);
        
        mCameraPreview = new CameraPreview(CameraActivity.this);
		FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mCameraPreview);
        
        //开始预览
        mOpenButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				initCamera();
			}
		});
        
        
        //关闭预览
        mCloseButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				resetCamera();
			}
		});
        
        //拍摄
        mTakePictureButton.setOnClickListener( new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	            // get an image from the camera
	        	if( getCameraInstance() != null)
	        		getCameraInstance().takePicture(null, null, mPicture);
	        }
	    });
        
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }
    
    private void releaseCamera() {
    	resetCamera();

    	if(mSingletonCamera != null) {
    		mSingletonCamera.release();
    		mSingletonCamera = null;
    	}
    }
    
    class MyFaceDetectionListener implements Camera.FaceDetectionListener {

        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            if (faces.length > 0){
                Log.d("FaceDetection", "face detected: "+ faces.length +
                        " Face 1 Location X: " + faces[0].rect.centerX() +
                        "Y: " + faces[0].rect.centerY() );
            }
        }
    }
    
    
    public static Camera getCameraInstance(){
        if(mSingletonCamera == null) {
	        try {
	        	mSingletonCamera = Camera.open(); // attempt to get a Camera instance
	        }
	        catch (Exception e){
	            // Camera is not available (in use or does not exist)
	        	Log.e(TAG,e.getMessage());
	        }
        }
        return mSingletonCamera; // returns null if camera is unavailable
    }

    private void initCamera() {
    	if( bIsPreview ){
    		return;
    	}
    	
    	if (checkCameraHardware(CameraActivity.this)) {
			Camera camera = getCameraInstance();
			
			// set Camera parameters
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(params);
            
            camera.setFaceDetectionListener(new MyFaceDetectionListener());
			//mCamera.setDisplayOrientation(-90);
			if( camera != null && mCameraPreview != null ) {
				mCameraPreview.setCamera(camera);
				bIsPreview = true;
				Log.e(TAG, "create preview ...");
			}
    	}
    }

    public void startFaceDetection(){
        // Try starting Face Detection
        Camera.Parameters params = getCameraInstance().getParameters();

        // start face detection only *after* preview has started
        Log.e(TAG,"start face pams..."+params.getMaxNumDetectedFaces());
        if (params.getMaxNumDetectedFaces() > 0){
            // camera supports face detection, so can start it:
        	getCameraInstance().startFaceDetection();

            Log.e(TAG,"start face detection...");
        }
    }
    
    
    private void resetCamera() {
    	if( mSingletonCamera != null && bIsPreview) {
			mSingletonCamera.stopPreview();
			bIsPreview = false;
			Log.e(TAG,"stop preview...");
		}
    }
    
    private PictureCallback mPicture = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = null;//getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: no file");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    /** A basic Camera preview class */
    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;

        @SuppressWarnings("deprecation")
		public CameraPreview(Context context) {
        	
            super(context);

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        
        public void setCamera(Camera camera) {
        	mCamera = camera;
        	
        	try {
        		mCamera.setPreviewDisplay(mHolder);
        		mCamera.startPreview();
                
        		//Log
        		Log.e(TAG,"prepare to start Face Detection...");
        		
                startFaceDetection();
        		
        	}catch (Exception e) {
        		Log.e(TAG, "Error setting camera preview: " + e.getMessage());
			}
        }

        public void surfaceCreated(SurfaceHolder holder) {
        	if( mCamera == null )
        	{
        		return;
        	}
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                Log.e(TAG,"surface_created.....");
                startFaceDetection();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
              // preview surface does not exist
              return;
            }
            
            if( mCamera == null )
        	{
        		return;
        	}

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
              // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();

                startFaceDetection();
            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }
    }
    
}
