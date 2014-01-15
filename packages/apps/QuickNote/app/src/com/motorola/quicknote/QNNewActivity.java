/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.quicknote;

import android.util.DisplayMetrics;
//import com.android.internal.app.AlertActivity;
//import com.android.internal.app.AlertController;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Matrix;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;

import com.motorola.quicknote.QNUtil;
import com.motorola.quicknote.QNAppWidgetConfigure;

/**
 * Displays a list of all activities matching the incoming
 * {@link Intent#EXTRA_INTENT} query, along with any injected items.
 */
public class QNNewActivity extends Activity  implements View.OnClickListener, DialogInterface.OnDismissListener, DialogInterface.OnCancelListener {
    
    static private String TAG = "QNNewActivity";
    
    /******************************
     * Constants
     ******************************/
    private int mWidgetId = -1;
    
    
    // path for camera to take picture
    private static String IMAGE_PATH;
    private static Uri IMAGE_URI;
    private final ImageButton[] mButtons = new ImageButton[4];
	private BroadcastReceiver mQuickNotesReceiver = new QuickNotesIntentReceiver();

	// for image rotate from camera
	private ProgressDialog mProgressDialog = null;
	private ImageProcessHandler mImageProcessHandler = null;
	private WaitingUiHandler mWaitingUiHandler = null;
	private static final int MSG_IMAGE_PROCESS_START = 1;
	private static final int MSG_IMAGE_PROCESS_COMPLETE = 2;
	private static final int MSG_IMAGE_PROCESS_FAILED = 3;
	private static final int MSG_DISMISS_PROGRESS = 4;
	private static final int DIALOG_IMAGE_PROCESS_START = 1001;

	/*
	 * before start QNNoteView, we should set mQNNewActivityIsRunning to false
	 * firstly, otherwise QNNoteView may receive the intent more quickly than
	 * onDestroy to set this value to false, it will result in QNNoteview
	 * automatically return;
	 */
	public static boolean mQNNewActivityIsRunning = false;

	private enum _Notes {
		TEXT(0, 0, R.string.text_note,
				new Intent(QNConstants.INTENT_ACTION_NEW).setClassName(
						QNConstants.PACKAGE,
						QNConstants.PACKAGE + ".QNNoteView").setDataAndType(
						Uri.parse("qntext:"), QNConstants.MIME_TYPE_TEXT),
				QNConstants.REQUEST_CODE_NONE), 
/*		PICTURE(1, 0,R.string.picture_note, 
				new Intent(
						MediaStore.ACTION_IMAGE_CAPTURE).setClassName(
						QNConstants.CAMERA_PACKAGE,
						QNConstants.CAMERA_PACKAGE + ".Camera").putExtra(
						MediaStore.EXTRA_OUTPUT, IMAGE_URI),
				QNConstants.REQUEST_CODE_CAMERA),*/
		PICTURE(1, 0,R.string.picture_note, 
						new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(
								MediaStore.EXTRA_OUTPUT, IMAGE_URI),
						QNConstants.REQUEST_CODE_CAMERA), 
		SKETCH(2, 0, R.string.sketch_note, 
				new Intent(QNConstants.INTENT_ACTION_NEW)
						.setClassName(QNConstants.PACKAGE, QNConstants.PACKAGE
								+ ".QNSketcher"), QNConstants.REQUEST_CODE_NONE), 
		VOICE(3, 0, R.string.voice_note,
				new Intent(Intent.ACTION_GET_CONTENT).setClassName(
						QNConstants.SR_PACKAGE,
						QNConstants.SR_PACKAGE + ".SoundRecorder").setType(
						QNConstants.MIME_TYPE_VOICE),
				QNConstants.REQUEST_CODE_SOUND_RECORDER);

		private int _index, _iconId, _textId;
		private Intent _intent;
		private int _resultCode;

		_Notes(int index, int iconId, int textId, Intent intent, int result) {
			_index = index;
			_iconId = iconId;
			_textId = textId;
			_intent = intent;
			_resultCode = result;
		}

		int index() {
			return _index;
		}

		int icon() {
			return _iconId;
		}

		int text() {
			return _textId;
		}

		Intent intent() {
			return _intent;
		}

		int requestCode() {
			return _resultCode;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

		String from = null;
		Intent intent = getIntent();
		from = intent.getStringExtra("from");
		if ((from != null) && (from.equals("widget"))) {
			String uri_string = intent.getStringExtra("contentUri");
			if (uri_string.equals("Special_widget")) {
				mWidgetId = intent.getIntExtra("widgetId", -1);
				QNDev.log(TAG + "QNNewActivity: from widget: widgetId = "
						+ mWidgetId);
				if (mWidgetId != -1) {
					QNDev.log(TAG
							+ "from widget-> QNNewActivity, update the configure...");
					QNAppWidgetConfigure.saveWidget2Update(this, mWidgetId);
				}
			}
		}

        QNUtil.initDirs(this);
        IMAGE_PATH = QNConstants.IMAGE_DIRECTORY + "tempimage.jpg";
        IMAGE_URI = Uri.parse("file://" + IMAGE_PATH);

        //requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.qn_new_activity);

        //getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, 
        //        R.drawable.ic_dialog_menu_generic);

		mButtons[0] = (ImageButton) findViewById(R.id.add_text_note);
		mButtons[1] = (ImageButton) findViewById(R.id.add_picture_note);
		mButtons[2] = (ImageButton) findViewById(R.id.add_sketch_note);
		mButtons[3] = (ImageButton) findViewById(R.id.add_voice_note);

		for (View btn : mButtons) {
			btn.setOnClickListener(this);
		}

        if(!QNUtil.is_storage_mounted(this)) {
            disableUI();
            alearSDCard(0);
        } else if (QNUtil.available_storage_space(this) <= QNConstants.MIN_STORAGE_REQUIRED) {
            disableUI();
            alearSDCard(1);
        } else {
            enableUI();
		}

		registerIntentReceivers();
		mQNNewActivityIsRunning = true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mQNNewActivityIsRunning = false;
		unregisterReceiver(mQuickNotesReceiver);
	}

	private void enableUI() {
		if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
			mButtons[0].setImageResource(R.drawable.ic_text_note);
		}
		mButtons[1].setImageResource(R.drawable.ic_picture_note);
		mButtons[2].setImageResource(R.drawable.ic_sketch_note);
		mButtons[3].setImageResource(R.drawable.ic_voice_note);

		if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
			for (int i = 0; i < 4; i++) {
				mButtons[i].setClickable(true);
			}
		} else {
			for (int i = 1; i < 4; i++) {
				mButtons[i].setClickable(true);
			}
		}
	}

	private void disableUI() {
		if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
			mButtons[0].setImageResource(R.drawable.ic_text_note_disable);
		}
		mButtons[1].setImageResource(R.drawable.ic_picture_note_disable);
		mButtons[2].setImageResource(R.drawable.ic_sketch_note_disable);
		mButtons[3].setImageResource(R.drawable.ic_voice_note_disable);

		if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
			for (int i = 0; i < 4; i++) {
				mButtons[i].setClickable(false);
			}
		} else {
			for (int i = 1; i < 4; i++) {
				mButtons[i].setClickable(false);
			}
		}
	}

	private void alearSDCard(int reason) {
		int msgId = R.string.no_sd_createnote;
		if (reason == 1) {
			msgId = R.string.not_enough_free_space_sd;
		}

		AlertDialog dialog = new AlertDialog.Builder(QNNewActivity.this)
				.setTitle(R.string.create_note)
				.setMessage(msgId)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								finish();
							}
						}).show();
		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		});
	}

	/**
	 * Handle clicking of dialog item by click
	 */
	public void onClick(View v) {
		QNDev.log(TAG + " onClick()");
		int position = 0;
		switch (v.getId()) {
		case R.id.add_picture_note:
			position = 1;
			break;
		case R.id.add_sketch_note:
			position = 2;
			break;
		case R.id.add_voice_note:
			position = 3;
			break;
		case R.id.add_text_note:
		default:
			position = 0;
		}

		for (_Notes n : _Notes.values()) {
			if (position == n.index()) {
				boolean preCondition = false;
                if (position == _Notes.TEXT._index && !QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
                    // text note store on phone memory
                    preCondition = true;
                } else {
                    preCondition = QNUtil.checkStorageCard(this);
				}

				if (preCondition && (null != n.intent())) {
					if (n.requestCode() == QNConstants.REQUEST_CODE_NONE) {
						Intent intent = n.intent();
						if (position == _Notes.TEXT._index
								&& QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
							Uri uriFilePath = QNUtil.prepareTextNote();
							if (uriFilePath == null) {
								mQNNewActivityIsRunning = false;
								finish();
								return;
							}
							intent.setDataAndType(uriFilePath,
									QNConstants.MIME_TYPE_TEXT);
						}
						QNDev.logd(TAG, "to startActivity: intent=" + intent);
						mQNNewActivityIsRunning = false;
						intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
								| Intent.FLAG_ACTIVITY_SINGLE_TOP);
						startActivity(intent);
						finish();
						return;
					} else {
                        QNDev.logd(TAG, "to startActivityForResult: requstCode= "+ n.requestCode() + " intent=" + n.intent());
                        if (n.intent().getExtras() != null) {QNDev.logd(TAG, "extras= "+ n.intent().getExtras()); }
                        // to launch Camera or Sound Recorder
                        try{
                            startActivityForResult(n.intent(), n.requestCode());
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(this, "ActivityNotFoundException", Toast.LENGTH_SHORT).show();
                            return;
                        } catch (SecurityException e) {
                            Toast.makeText(this, "SecurityException", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }
                break;
            }
        }

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		QNDev.logd(TAG, "onActivityResult: requestCode=" + requestCode
				+ " resultCode=" + resultCode);

		super.onActivityResult(requestCode, resultCode, intent);

		if (resultCode != RESULT_OK) {
			mQNNewActivityIsRunning = false;
			finish();
			return;
		}

		if (requestCode == QNConstants.REQUEST_CODE_CAMERA) {
			// show waiting UI and try to get final rotated image
			showDialog(DIALOG_IMAGE_PROCESS_START);
			initImageProcess();
			mImageProcessHandler.sendEmptyMessage(MSG_IMAGE_PROCESS_START);
		} else if (requestCode == QNConstants.REQUEST_CODE_SOUND_RECORDER) {
			Uri uriContent = null;
			Uri uriPath = null;
			String mimeType = null;

			if (null != intent) {
				uriContent = intent.getData();
				QNDev.logd(
						TAG,
						"intent data=" + uriContent + "type="
								+ intent.getType() + " extras="
								+ intent.getExtras());
			}
			
			if(uriContent.toString().startsWith("file://")){
				uriPath = uriContent;
				mimeType = QNConstants.MIME_TYPE_VOICE;
			}else if(uriContent.toString().startsWith("content://")){
				// dat=content://media/external/audio/media/8, query db for path
				// ContentResolver cr = this.getContentResolver();
				String[] projection = new String[] { Audio.Media.DATA,
						Audio.Media.MIME_TYPE };
				Cursor c = managedQuery(uriContent, projection, null, null, null);
				String path = null;
				if (c != null && c.getCount() > 0) {
					try {
						if (c.moveToFirst()) {
							path = c.getString(c
									.getColumnIndexOrThrow(Audio.Media.DATA));
							uriPath = Uri.parse("file://" + path);
							mimeType = c.getString(c
									.getColumnIndexOrThrow(Audio.Media.MIME_TYPE));
							QNDev.logd(TAG,
									"onActivityResult: sound recorder query db, content_uir="
											+ uriPath.toString() + " mimeType="
											+ mimeType);
						}
					} finally {
						// c.close();
					}
				} else {
					mQNNewActivityIsRunning = false;
					finish();
					return;
				}
			}						

			Intent newIntent = new Intent(QNConstants.INTENT_ACTION_NEW);
			newIntent.setClass(QNNewActivity.this, QNNoteView.class);
			newIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			newIntent.setDataAndType(uriPath, mimeType);
			mQNNewActivityIsRunning = false;
			startActivity(newIntent);
			finish();
			return;
		}
		// finish picker..
	}

	private void registerIntentReceivers() {
		IntentFilter qnFilter = new IntentFilter(
				QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED);
		registerReceiver(mQuickNotesReceiver, qnFilter);
	}

	// Inner class
	private class QuickNotesIntentReceiver extends BroadcastReceiver {
		private static final String TAG = "[QNNewActivity:QuickNotesIntentReceiver]";

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			QNDev.log(TAG + " intent action : " + action);

            if ((action != null) && action.equals(QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED)) {
                //update UI
                if (QNUtil.checkStorageCard(QNNewActivity.this)) {
                    enableUI();
                } else {
                    disableUI();
                }
            } 
        }
    }

	@Override
	protected Dialog onCreateDialog(int id) {
		if (id == DIALOG_IMAGE_PROCESS_START) {
			mProgressDialog = new ProgressDialog(QNNewActivity.this);
			mProgressDialog.setMessage(getResources().getString(
					R.string.image_process));
			mProgressDialog.setCancelable(false);
			mProgressDialog
					.setOnKeyListener(new DialogInterface.OnKeyListener() {
						public boolean onKey(DialogInterface dialog,
								int keyCode, KeyEvent event) {
							if ((keyCode == KeyEvent.KEYCODE_SEARCH)
									|| (keyCode == KeyEvent.KEYCODE_BACK)) {
								return true; // Pretend we processed it
							} else {
								return false; // Any other keys are still
												// processed as normal
							}
						}
					});

			return mProgressDialog;
		} else {
			return null;
		}
	}

	public void onDismiss(DialogInterface dialog) {
		;
	}

	public void onCancel(DialogInterface dialog) {
		;
	}

	private void initImageProcess() {
		try {
			if (mWaitingUiHandler == null) {
				mWaitingUiHandler = new WaitingUiHandler();
			}

			if (mImageProcessHandler == null) {
				HandlerThread imageProcessThread = new HandlerThread(
						"imageProcess handler: Process Thread");
				imageProcessThread.start();
				Looper looper = imageProcessThread.getLooper();
				if (looper != null) {
					mImageProcessHandler = new ImageProcessHandler(looper,
							mWaitingUiHandler);
				}
			}
		} catch (Exception e) {
			Toast.makeText(QNNewActivity.this, R.string.image_process_failed,
					Toast.LENGTH_LONG).show();
			mQNNewActivityIsRunning = false;
			finish();
		}
	}

	private class WaitingUiHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_IMAGE_PROCESS_COMPLETE:
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
				}
				break;
			case MSG_IMAGE_PROCESS_FAILED:
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
				}
				Toast.makeText(QNNewActivity.this,
						R.string.image_process_failed, Toast.LENGTH_LONG)
						.show();
				mQNNewActivityIsRunning = false;
				finish();
				break;
			case MSG_DISMISS_PROGRESS:
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
				}
				mQNNewActivityIsRunning = false;
				finish();
				break;
			default:
				break;
			}
		}
	};

	private class ImageProcessHandler extends Handler {
		private Handler mImageProcessUiHandler;

		public ImageProcessHandler(Looper looper, Handler imageProcessUiHandler) {
			super(looper);
			this.mImageProcessUiHandler = imageProcessUiHandler;
		}

		@Override
		public void handleMessage(Message msg) {
			File oldFile = new File(IMAGE_PATH);
			int degrees = 0;

			try {
				switch (msg.what) {
                case MSG_IMAGE_PROCESS_START :
                    if (!oldFile.exists()) {
                       //file from camera is not exist, so return;
                       Toast.makeText(QNNewActivity.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
                       QNDev.log(TAG+" handleMessage found image process error for old file is not exist");
                       mQNNewActivityIsRunning = false;
                       finish();
                       return;
                    } 
                    //check the exif info. to see whether the png from camere need to rotate or not
                    degrees = QNUtil.getDegreesRotated(IMAGE_URI);
                    if (degrees != 0) {
                       //need rotate the picture
                       Bitmap bm = QNUtil.decode_image_file(IMAGE_PATH, getResources().getInteger(R.integer.detail_image_width), getResources().getInteger(R.integer.detail_image_height), true);
                       oldFile.delete();
                       Bitmap newbm = QNUtil.rotate(bm, degrees);
                       if (newbm == null) {
                          //something wrong when rotating the image
                          Toast.makeText(QNNewActivity.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
                          QNDev.log(TAG+" handleMessage found image process error for newBm = null");
                          mQNNewActivityIsRunning = false;
                          finish();
                          return;
                       }
                       QNUtil.save_image(oldFile, newbm, CompressFormat.JPEG);
                       bm.recycle();
                    }
                    // mv tempimage.jpg to yyyy-MM-dd_kk-mm-ss_mm.jpg
                    String newFileName = QNUtil.createNewImageName();
                    File newFile = new File(newFileName);
                    if (oldFile.exists() && oldFile.renameTo(newFile)) {
                       QNDev.logd(TAG, "old file exists? =" + oldFile.exists() + " new file exists?=" + newFile.exists()); 
                       QNDev.logd(TAG, "new image file =" + newFileName); 
                       mQNNewActivityIsRunning = false;
                       Intent newIntent = new Intent(QNConstants.INTENT_ACTION_NEW);
                       newIntent.setClass(QNNewActivity.this, QNNoteView.class);
                       newIntent.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                       newIntent.setDataAndType(Uri.parse("file://" + newFileName), QNConstants.MIME_TYPE_IMAGE);
                       startActivity(newIntent);
                       finish();
                       return;
                    }

                    mImageProcessUiHandler.sendMessage(mImageProcessUiHandler.obtainMessage(MSG_IMAGE_PROCESS_COMPLETE));
                    break;
                default:
                    mImageProcessUiHandler.sendMessage(mImageProcessUiHandler.obtainMessage(MSG_DISMISS_PROGRESS));
                    break;
             }
           } catch (Exception ex) {
             Toast.makeText(QNNewActivity.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
             QNDev.log(TAG+" handleMessage found image process exception = "+ex);
             mQNNewActivityIsRunning = false;
             finish();
             return;
           } catch (java.lang.OutOfMemoryError e) {
             Toast.makeText(QNNewActivity.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
             QNDev.log(TAG+" handleMessage found image process error for out of memory error = "+e);
             mQNNewActivityIsRunning = false;
             finish();
             return;
           }
        }
    }
}
