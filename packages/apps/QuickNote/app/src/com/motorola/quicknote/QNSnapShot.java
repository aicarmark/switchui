package com.motorola.quicknote;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import com.motorola.quicknote.content.QNContent;

public class QNSnapShot extends Activity {

	// ALL files in this temp directory are regarded as temporal files!
	private final static String TAG = "QNSnapShot";
	private Button _save;
	private Button _create;
	private Button _discard;
	private Uri _uri = null;
	private TextView _textview = null;
	private File fnote = null;
	private Bitmap mBmp = null;
	private ActionBar mActionBar;

	private void _do_action() {
		Intent intent = new Intent(QNConstants.INTENT_ACTION_NEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setClass(QNSnapShot.this, QNNoteView.class);
		if (fnote != null) {
			_uri = Uri.parse("file://" + fnote.getAbsolutePath());
		}
		intent.setDataAndType(_uri, "image/png");
		startActivity(intent);
		_uri = null; // prevent from being deleted on "Destroy"
		finish();
	}

	private void doSnapshot() {
		// reference from
		// frameworks/base/packages/SystemUI/src/com/android/systemui/screenshot/GlobalScreenshot.java
		// takeScreenshot().
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

		Display mDisplay = wm.getDefaultDisplay();

		DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);
        
    float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
		float degrees = getDegreesForRotation(mDisplay.getRotation());
		boolean requiresRotation = (degrees > 0);
		if (requiresRotation) {
			// Get the dimensions of the device in its native orientation
			Matrix mDisplayMatrix = new Matrix();
			mDisplayMatrix.reset();
			mDisplayMatrix.preRotate(-degrees);
			mDisplayMatrix.mapPoints(dims);
			dims[0] = Math.abs(dims[0]);
			dims[1] = Math.abs(dims[1]);
		}
    mBmp = Surface.screenshot((int) dims[0], (int) dims[1]);
		if (requiresRotation) {
			// Rotate the screenshot to the current orientation
			Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
					mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(ss);
			c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
			c.rotate(degrees);
			c.translate(-dims[0] / 2, -dims[1] / 2);
			c.drawBitmap(mBmp, 0, 0, null);
			c.setBitmap(null);
			mBmp = ss;
		}

		// If we couldn't take the screenshot, notify the user
		if (mBmp == null) {
			Log.v("GlobalScreenshot", "gdnq47, takeScreenshot, error step 1");
            Toast.makeText(QNSnapShot.this, R.string.snapshot_failed, Toast.LENGTH_LONG).show();
			finish();
		}

		// Optimizations
		mBmp.setHasAlpha(false);
		mBmp.prepareToDraw();
	}

	/**
	 * @return the current display rotation in degrees
	 */
	private float getDegreesForRotation(int value) {
		switch (value) {
		case Surface.ROTATION_90:
			return 360f - 90f;
		case Surface.ROTATION_180:
			return 360f - 180f;
		case Surface.ROTATION_270:
			return 360f - 270f;
		}
		return 0f;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        if(!QNUtil.is_storage_mounted(this)){
            Toast.makeText(this, R.string.no_sd_snapshot,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        } else if (QNUtil.available_storage_space(this) <= QNConstants.MIN_STORAGE_REQUIRED) {
            Toast.makeText(this, R.string.not_enough_free_space_sd,
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        doSnapshot();
        setTheme(android.R.style.Theme_Dialog);
        setupView();
     }

    /**
     * save screenshot image to file.
     */
    private boolean saveScreenshot() {
     try {
       QNUtil.initDirs(this);
       QNDev.qnAssert(null != QNConstants.SNAPSHOT_DIRECTORY && QNUtil.is_absolute_path(QNConstants.SNAPSHOT_DIRECTORY));
       String fileName = QNUtil.createNewFileName(QNConstants.SNAPSHOT_DIRECTORY+"ss", ".png"); 
       File file = new File(fileName);
       if(file != null && QNUtil.save_image(file, mBmp, CompressFormat.JPEG)) {
           QNDev.log("fScreenshot"+ "save the screen capture in file: "+file);
           QNUtil.register_to_MediaStore(QNSnapShot.this, file, "image/png", new File(QNConstants.TEMP_DIRECTORY));
           fnote = file;
           return true;
       } else {
     	  QNDev.log("fScreenshot"+ "Failed to save the screen capture in file: "+file);
          Toast.makeText(this, R.string.snapshot_save_failed, Toast.LENGTH_LONG).show();
     	  return false;
       }
     } catch (Exception e) {
        Toast.makeText(this, R.string.snapshot_save_failed, Toast.LENGTH_LONG).show();
        finish();
        return false;
     }
    }
     
     private void setupView () {
        this.setContentView(R.layout.snapshot_dialog);
        ImageView sourceImage= (ImageView)findViewById(R.id.sourceImage);
        
        sourceImage.setImageBitmap(mBmp);

        _textview = (TextView)findViewById(R.id.text);

        mActionBar = (ActionBar)findViewById(R.id.action_bar);

        //_save    = (Button)findViewById(R.id.save);
        _save  = new Button(QNSnapShot.this);
        _save.setText(R.string.snapshot_btn_save);
        mActionBar.addButton(_save); 
        _save.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
              try {
            	  saveScreenshot();
                  _uri = null; // prevent from being deleted on "Destroy"
                  finish();
               } catch (Exception e){
                 finish();; //do nothing except finish this activity
               }
            }
        });

       // _create  = (Button)findViewById(R.id.create);
       _create  = new Button(QNSnapShot.this);
       _create.setText(R.string.snapshot_btn_create);
       mActionBar.addButton(_create); 
       _create.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
        /*
               Intent i = new Intent(QNConstants.INTENT_ACTION_FIND_EMPTY_CELL);
                i.putExtra("currentScreenOnly", false);
                sendOrderedBroadcast(i, null, _result_receiver, null, 0, null, null);
        */
            	if(saveScreenshot()) {
            		_do_action();
            	}
            }
        });

        //_discard = (Button)findViewById(R.id.discard);
        _discard = new Button(QNSnapShot.this);
        _discard.setText(R.string.snapshot_btn_discard);
        mActionBar.addButton(_discard); 
        _discard.setOnClickListener( new View.OnClickListener() {
        	public void onClick(View v) {
        	    // delete snapshot file.
        		finish();
        	}
        });

        updateLayoutViews();
		
    }

   @Override
   protected void onResume() {
       QNDev.log(TAG+ "onResume, ");
       super.onResume();
   }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
          setupView();
    }

   private void updateLayoutViews() {
     _textview.setVisibility(View.VISIBLE);
     _save.setVisibility(View.VISIBLE);
     _create.setVisibility(View.VISIBLE);
     _discard.setVisibility(View.VISIBLE);
   }
   
   protected void onPause() {
      super.onPause();
      QNDev.log(TAG+ "onPause, ");
   }

   protected void onStop() {
     super.onStop();
     QNDev.log(TAG+ "onStop, ");
     if(null != _uri) {
        QNDev.log(TAG+ "onStop, delete the file");
        QNDev.qnAssert(_uri.getScheme().equals("file"));
        new File(_uri.getPath()).delete();
        _uri = null;
      }
      finish();
   }

   @Override
   public void onDestroy() {
      QNDev.log(TAG+ "onDestroy, ");
      if(null != _uri) {
         QNDev.log(TAG+ "onDestroy, delete the file");
         QNDev.qnAssert(_uri.getScheme().equals("file"));
         new File(_uri.getPath()).delete();
         _uri = null;
       }
       if (mBmp != null) {
          mBmp.recycle();
       }
       super.onDestroy();
    }

}
