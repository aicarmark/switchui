package com.test.silentcapture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.media.FaceDetector;
import android.media.FaceDetector.Face;
import android.os.Environment;
import android.os.Handler;
import android.view.WindowManager;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Surface;
import android.view.View;
import android.view.KeyEvent;
import android.view.HapticFeedbackConstants;
import android.util.Log;
import android.os.Vibrator;
import android.app.Service;
import android.content.Intent;
import java.lang.Thread;

import com.test.silentcapture.Settings;

public class CaptureSurface 
    extends SurfaceView 
    implements SurfaceHolder.Callback, Camera.PictureCallback {


    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.Parameters mParams;
    private Handler mHandler = new Handler();
    private Context mContext;
    private SurfaceView mSurfaceView;
    private AudioManager mAudio;
    private Vibrator mVibrator;
    private int mCurrent;

    private Thread mPrepareThread;
    private boolean mPrepareReady;
    private boolean mIsCapturing;

    // Vibrate for capture ready
    private final long[] VIBRATE_READY = new long[]{5,60,50,15};

    public CaptureSurface (Context context) {
        super(context);

        mSurfaceView = this;
        mAudio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        final int mCurrent = mAudio.getRingerMode();
        mAudio.setRingerMode(AudioManager.RINGER_MODE_SILENT);

        mVibrator = (Vibrator)context.getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);

        mContext = context;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mPrepareReady = false;
        mIsCapturing  = false;
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        Log.d(Settings.TAG, "CaptureSurface surfaceCreated");   
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(Settings.TAG, "CaptureSurface surfaceChanged format:" + format + ", w:" + width + ", h:" + height);
        mPrepareThread = new Thread() {
            @Override
            public void run() {
                mCamera = Camera.open();

                Camera.Parameters params = mCamera.getParameters();
                List<Size> sizelist = params.getSupportedPictureSizes(); 
                Size s = sizelist.get(0);
                params.setPictureSize(s.width, s.height);
                params.setJpegQuality(100);
                //Size size = params.getPictureSize();
                mCamera.setParameters(params);

                try {
                    mCamera.setPreviewDisplay(mHolder);    
                } catch (IOException e) {
                    Log.e(Settings.TAG, "prepare thread run met an error");
                    e.printStackTrace();
                }

                setCameraDisplayOrientation();
                mCamera.startPreview();
                mPrepareReady = true;
                Log.d(Settings.TAG, "prepare thread run over, camera is ready now");
            }
        };
        mPrepareThread.start();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int keyCode = event.getKeyCode();
        Log.d(Settings.TAG, "CaptureSurface gets a key event:" + keyCode);
        return true;
    }

    private Runnable mCaptureRunnable = new Runnable() {
        @Override
        public void run() {
            // Wait for camera be ready...
            if (mPrepareReady == false) {
                mHandler.postDelayed(this, 500); 

            // Camera is ready now, start capturing
            } else {
                Log.d(Settings.TAG, "CaptureSurface begin auto focus, isCapturing:" + mIsCapturing);
                // Cancel previous focusing, and start new focus for capturing
                if (mIsCapturing) {
                    mCamera.cancelAutoFocus();
                }
                    
                mIsCapturing = true;
                try {
                    mCamera.autoFocus(new AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            Log.d(Settings.TAG, "CaptureSurface onAutoFocus success:" + success);
                            
                            if (success) {
                                camera.takePicture(new ShutterCallback() {
                                    @Override
                                    public void onShutter(){}
                                }, null, CaptureSurface.this);
                            } else {
                                // failed auto focus, re-preview for next capture
                                //Log.d(Settings.TAG, "onAutoFocus failed, re-startpreview again...");
                                //mCamera.startPreview();
                                mIsCapturing = false;
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(Settings.TAG, "capture auto focus met an error: " + e);
                    //mCamera.startPreview();
                    mIsCapturing = false;
                }
            }
        }
    };

    public void capture() {
        mHandler.postDelayed(mCaptureRunnable, 100);
    }

    private void setCameraDisplayOrientation() {
        WindowManager wm = (WindowManager) mContext.getApplicationContext().getSystemService("window");
        int rotation = wm.getDefaultDisplay().getRotation();

        Camera.CameraInfo info = new Camera.CameraInfo();
        int N = Camera.getNumberOfCameras();
        int i = 0;
        for (i=0; i<N; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                break;
            }
        }

        Log.d(Settings.TAG, "setCameraDisplayOrientation i:" + i + 
                ", info.orientation:" + info.orientation + ", rotation:" + rotation);
        if (i < N) {
            int degrees = 0;
            switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            }

            int result;
            result = (info.orientation - degrees + 360) % 360;
            mCamera.setDisplayOrientation(result);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(Settings.TAG, "CaptureSurface surfaceDestroyed, prepare ready:" + mPrepareReady);

        if (mPrepareReady == true) {
            onDestroy();
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Camera is preparing, wait for it completed, and then do destroy...
                    if (mPrepareReady == false) {
                        mHandler.postDelayed(this, 500);
                    } else {
                        onDestroy();
                    }
                }
            }, 500);
        }    
    }

    private void onDestroy() {
        Log.d(Settings.TAG, "CaptureSurface onDestroy enter...");
        mHandler.removeCallbacks(mCaptureRunnable);
        //mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        mHolder.removeCallback(CaptureSurface.this);
        
        mAudio.setRingerMode(mCurrent);

        mVibrator.cancel();
        // TODO remove all pending runnables in handle
    }

    public void onPictureTaken(byte[] data, Camera camera) {
        Log.d(Settings.TAG, "CaptureSurface onPictureTaken callback");
        
        boolean done = false;
        try {
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
            String time = format.format(date);

            File file = new File(Environment.getExternalStorageDirectory() + "/silentcapture");
            Log.d(Settings.TAG, "CaptureSurface external dir:" + Environment.getExternalStorageDirectory());
            if (!file.exists()) {
                file.mkdirs();
            }

            String path = Environment.getExternalStorageDirectory() + "/silentcapture/" + time + ".jpg";
            date2file(data, path);

            // start deliver service to deliver it
            deliver(path);
            done = true;

        } catch (Exception e) {
            Log.e(Settings.TAG, "CaptureSurface onPictureTaken met an error");
            e.printStackTrace();
        }
        
        // Provide a haptic to tell user one capture has been done.
        if (done == true) {
            mVibrator.vibrate(50);
        }
        Log.d(Settings.TAG, "onPictureTaken re-startpreview again....");
        mCamera.startPreview();
        mIsCapturing = false; // Not capturing, available for next capture
    }

    private void date2file(byte[] w, String fileName) throws Exception {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(fileName);
            out.write(w);
            out.close();

        } catch (Exception e) {
            Log.d(Settings.TAG, "CaptureSurface data2file met an error:" + e);
            e.printStackTrace();
            if (out != null) {
                out.close();
            }
            throw e;
        }
    }

    private void deliver(String path) {
        Intent i = new Intent(mContext, com.test.silentcapture.DeliverService.class);
        i.putExtra(Settings.EXTRA_ATTACHMENT, path);
        mContext.startService(i);
    }

}
