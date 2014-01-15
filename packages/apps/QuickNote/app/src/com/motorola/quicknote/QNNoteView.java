/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * 
 * ***************************************************************************************
 * [QuickNote] Edit screen.
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created - template version.
 * 
 * 
 *****************************************************************************************/

package com.motorola.quicknote;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlertDialog;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.text.format.DateUtils;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ActionMode;
import android.provider.MediaStore.Images.Thumbnails;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.KeyEvent;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.provider.MediaStore;
import android.util.Log;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;

import com.motorola.quicknote.QuickNotesDB;
import com.motorola.quicknote.QNConstants._BackgroundRes;
import com.motorola.quicknote.QuickNotesDB.QNColumn;
import com.motorola.quicknote.content.QNContent;
import com.motorola.quicknote.content.QNContent_Snd;
import com.motorola.quicknote.content.QNContent_Factory;
import com.motorola.quicknote.content.QNContent.NotestateE;
import com.motorola.quicknote.QNAppWidgetConfigure;


public class QNNoteView extends QNActivity implements View.OnClickListener, DialogInterface.OnDismissListener, DialogInterface.OnCancelListener  
{
	/******************************
	 * Constants
	 ******************************/

	static private String TAG = "QNNoteView";
	// Request code....
	private static final int REQCODE_QNC_EDIT = 1;
	private static final int REQCODE_REMINDER_SET = 2;

	private Menu mOptMenu = null; // option menu instance

    private enum _RunningModeE {
        DETAIL,
        EDIT_NEW,
        EDIT_EDIT,
        EDIT_ATTACH         //attach data from Camera/SoundRecorder
    }

	/******************************
	 * members
	 ******************************/
	private QNContent mQNContent = null; // quick note contents
	// "mContentUri is not null" means this is existing note!
	private Uri mContentUri = null; // current note's conent uri :
									// content://com.motorola.provider.quicknote/qnotes/4
	private Uri mOldContentUri = null;
	private Uri mFileUri = null; // current note's file uir
									// :file:///mnt/sdcard/quicknote/text/2012-02-06_05-00-49_932.txt
	private Uri mNewFileUri = null; // Uri of the result of edit...
    private String               ori_path           = null;  //used for Camera, soundRecorder
	private String mMimeType = null;
	private _RunningModeE currentRunMode = null;
	private _RunningModeE oldRunMode = null;
	private int mBgColor = R.color.yellow; // this is default
	private ImageView mThumbnail = null;
	private ImageView mSetReminder = null;
	private ImageView mTrash = null;
	private ImageView mShare = null;
	private ImageView mAdd = null;
	private ImageView mEditOK = null;
	private ImageView mEditCancel = null;
	private RelativeLayout mTitlePart = null;
	private TextView mIndexCount = null;
	private EditText mEditTitle = null;
	private TextView mDetailTitle = null;
	private ScrollView mDetailTitleScroll = null;
	private ScrollView mEditTitleScroll = null;
	private EditText mEditTextContent = null;
	private TextView mDetailTextContent = null;
	private RelativeLayout mTextContainer = null;
	private ScrollView mDetailTextScroll = null;
	private ScrollView mEditTextScroll = null;
	private RelativeLayout mTopBar = null;
	private RelativeLayout mContentPart = null;
	private ImageView mImageContent = null;
    private RelativeLayout       mImageContentLayout= null;          
	private ImageView mImageDetail = null;
	private ImageView mImageEditBtn = null;
	private RelativeLayout mVoiceContent = null;
	private ImageView mPlayPause = null;
	private LinearLayout mBackgroundBtns = null;
	private ImageView mBackgroundBtn_1 = null;
	private ImageView mBackgroundBtn_2 = null;
	private ImageView mBackgroundBtn_3 = null;
	private ImageView mBackgroundBtn_4 = null;
	private int mOffset = 0;
	// for voice note
	private TextView mEditVoiceDuration = null;
	private ImageView mVoiceIcon = null;
	private LinearLayout mDetailVoiceLayout = null;
	// for error layout
	private RelativeLayout mErrorLayout = null;

	private ViewGroup mViewGroup = null;
	private View mNoteView = null;
	private int mIndex = 0; // 0 ~ mCount-1
	private int mCount = 0;
	private ContentValues mValues = null;
	private boolean mActivityVisiable = true;
	private int mVoice = 0; // for statusbar

	// for reminder
	private long mDBReminder = 0L;
	private View mReminderIndicator = null;
	private TextView mRemindertime = null;

	// for Back key
	private boolean mFromOtherApp = false;
	// for scrolling view
	private boolean mScrollingViews = false;

	private int mTouchStartPointX;
	private int mTouchStartPointY;

	// to prevent clicking the trash btn multiple times in very short tim
	private AlertDialog mDeleteAlertDialog = null;

	private BroadcastReceiver mQuickNotesReceiver = new QuickNotesIntentReceiver();

	// Gesture support
	private static final int GESTURE_MIN_DISTANCE = 40;
	private static final int GESTURE_SLOPE_THRESHOLD = 2;
	private static final int INVALID = -1;
	private static final int PREV = 1;
	private static final int NEXT = 2;
	private GestureDetector mSlideshowGestureDetector = null;
	private SlideGestureListener mSlideGestureListener = null;
	private boolean mOnMoving = false;
	private int mDirection = INVALID;

    //for image rotate from camera
    private ProgressDialog mProgressDialog = null;
    private ImageProcessHandler mImageProcessHandler = null;
    private WaitingUiHandler mWaitingUiHandler = null;
    private static final int MSG_IMAGE_PROCESS_START = 1;
    private static final int MSG_IMAGE_PROCESS_COMPLETE = 2;
    private static final int MSG_IMAGE_PROCESS_FAILED = 3;
    private static final int MSG_DISMISS_PROGRESS = 4;
    private static final int DIALOG_IMAGE_PROCESS_START = 1001;

    // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
    private ViewGroup mCallInfo = null;
    private ImageView mCallInfoType = null;
    private TextView mCallInfoName = null;
    private TextView mCallInfoNumber = null;
    private TextView mCallInfoDate = null;

    private CopyPasteCallback mCopyPasteCallback;
	// END IKCNDEVICS-751
    private boolean isIMEShow = false;
    /*2012-9-10, add by amt_sunzhao for SWITCHUITWOV-92 */ 
    private boolean mIsResumed = false;
	 /*2012-9-10, add end*/ 
	 /*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-194 */ 
    private boolean mIsImageDetailMode = false;
	/*2012-9-18, add end*/ 

	// Start from Launcher (to create an note)

	/******************************
	 * Types
	 ******************************/

	/**************************
	 * Local Functions
	 **************************/
	/**
	 * User may select same content and select "Update!" So, We need to check
	 * whether "mFileUri" is same with "mNewFileUri" or not, to avoid
	 * unexpecting result!
     */
    private boolean _have_same_content(Uri uri0, Uri uri1) {
        QNDev.qnAssert(null != uri0 && null != uri1
                     && (QuickNotesDB.is_qnuri_type(uri0) 
                             || uri0.getScheme().equals("file") || uri0.getScheme().equals("qntext"))
                     && (QuickNotesDB.is_qnuri_type(uri1) 
                             || uri1.getScheme().equals("file") || uri1.getScheme().equals("qntext")) );
        if (uri0 == null || uri1 == null) {
            return false;
        }
        Uri content0 = QuickNotesDB.is_qnuri_type(uri0)? QNUtil.buildUri((String)QuickNotesDB.read(this, uri0, QNColumn.URI)): uri0;
        Uri content1 = QuickNotesDB.is_qnuri_type(uri1)? QNUtil.buildUri((String)QuickNotesDB.read(this, uri1, QNColumn.URI)): uri1;
		QNDev.qnAssert(null != content0 && null != content1);
		if (content0 == null || content1 == null) {
			return false;
		}
		return content0.equals(content1);
	}

    private boolean _create_note()
    {
        QNDev.log(TAG + ": _create_note()");
        if (null == mQNContent) {return false;}

        String title = mQNContent.isTitle()? mEditTitle.getText().toString(): "";

		mQNContent.trigger_update();

		if (null != mNewFileUri && _have_same_content(mNewFileUri, mFileUri)) {
			// actually it is NOT NEW
			mNewFileUri = null;
		}

		ContentValues contentValues = new ContentValues();
		contentValues.put(QNColumn.TITLE.column(), title);
		contentValues.put(QNColumn.MIMETYPE.column(), mMimeType);
		contentValues.put(QNColumn.BGCOLOR.column(), mBgColor);

		if (null == mNewFileUri) {
			if (mFileUri == null) {
				QNDev.logi(TAG, "_create_note failed, mFileUri is null");
				return false;
			}
			contentValues.put(QNColumn.URI.column(), mFileUri.toString());
		} else {
			contentValues.put(QNColumn.URI.column(), mNewFileUri.toString());
			// delete old file
			_clean(mFileUri);
			mFileUri = mNewFileUri;
			mNewFileUri = null;

		}

        setContentUri(QuickNotesDB.insert(this, contentValues, true));
		mValues = getDBData();
		getCommonValues();

		// notify to provider that new note (with 'uri') is generated.
		// Provider should map uri with app widget id.
		if (mContentUri != null) {
			Intent i = new Intent(QNConstants.INTENT_ACTION_NOTE_CREATED);
			i.putExtra(QNConstants.NOTE_URI, getContentUriString());
			i.putExtra(QNConstants.GRID_ID, 0);
			sendBroadcast(i);
		}

		oldRunMode = currentRunMode;
		currentRunMode = _RunningModeE.DETAIL;
		// update the mCount
		mCount = QuickNotesDB.getSize(QNNoteView.this);
		toDetailMode();
		return true;
	}

	private boolean _update_note() {
		QNDev.log(TAG + ": _update_note()");
        if ((null == mContentUri) || !QuickNotesDB.is_qnuri_type(mContentUri) || (null == mQNContent)) { return false;}
        
        boolean bUpdated = false;
        boolean contentUpdated = false;
        
        bUpdated = mQNContent.trigger_update(); // give a chance to update to _qnc
        if (bUpdated) {
           //for text note, after updating, the URL is same as before
           contentUpdated = true;
        }

        if(null != mNewFileUri && _have_same_content(mNewFileUri, mFileUri)) {
            // actually it is NOT NEW
            mNewFileUri = null;
            contentUpdated = false;
        }

        if(null != mNewFileUri) {
            contentUpdated = true;
            //update the file uri
            mValues.put(QNColumn.URI.column(), mNewFileUri.toString());

			// delete old file
			_clean(mFileUri);
			mFileUri = mNewFileUri;
			mNewFileUri = null;

			bUpdated = true;

		}

        if ( mQNContent.isTitle() ){
            String new_title = mEditTitle.getText().toString();

            if( !new_title.equals((String)QuickNotesDB.read(this, mContentUri, QNColumn.TITLE)) ) {
                //update the title
                mValues.put(QNColumn.TITLE.column(), new_title);
                bUpdated = true;
            }
        }
        
        // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
        if (mMimeType.startsWith("text/")) {
            String content = mEditTextContent.getText().toString();
            String new_summary = content.length()>40? content.substring(0, 40): content;
            if( !new_summary.equals((String)QuickNotesDB.read(this, mContentUri, QNColumn.SUMMARY)) ) {
                //update the summary
                mValues.put(QNColumn.SUMMARY.column(), new_summary);
                bUpdated = true;
            }
        }
        // END IKCNDEVICS-751

        //for background color
        if( mBgColor !=  mValues.getAsInteger(QNColumn.BGCOLOR.column())) {
            mValues.put(QNColumn.BGCOLOR.column(), mBgColor);
            bUpdated = true;
        }

        if(bUpdated) {
            //delete this item from the DB and re-insert it again as a new created note
            //step 1.insert it firstly
            Uri oldContentUri = mContentUri;
            setContentUri(QuickNotesDB.insert(this, mValues, contentUpdated));
            if (mContentUri == null ){
                QNDev.log(TAG + " update note failed! return;");
                Toast.makeText(QNNoteView.this, R.string.note_update_failed, Toast.LENGTH_SHORT);
                finish();
                return false;
            }

            //check the reminder
            if ( 0L != mDBReminder) {
               //reset the reminder
               QNUtil.setAlarm(this, oldContentUri, 0L);
               QNUtil.setAlarm(this, mContentUri, mDBReminder);
            }

            mIndex = 0;

            //delete the old item in DB
            QuickNotesDB.delete(QNNoteView.this,  oldContentUri);

            //broadcast the intent
            Intent intentUpdate = new Intent(QNConstants.INTENT_ACTION_NOTE_UPDATED);
            intentUpdate.putExtra("old_content_uri", oldContentUri.toString());
            intentUpdate.putExtra(QNConstants.NOTE_URI, getContentUriString());
            intentUpdate.putExtra(QNConstants.GRID_ID, 0);
            sendBroadcast(intentUpdate);
            
            // set result intent
            Intent i = new Intent();
            i.setDataAndType(mContentUri, mMimeType);
            setResult(RESULT_OK, i);

            return true;
        }

        return false;
    }
    
/*
    private void putUriToContentValue(ContentValues contentValues, Uri uri) {
            if (QNDev.STORE_FILE_ON_SDCARD) {
                String fileName = QNUtil.createNewTextName();
                QNDev.log(TAG + " to create text file: " + fileName);
                Uri uriFilePath = Uri.parse("file://" + fileName);
                contentValues.put(QNColumn.URI.column(), uriFilePath.toString());
                mQNContent.createOrUpdateTextFiles(fileName, uri);
            } else {
                contentValues.put(QNColumn.URI.column(), uri.toString());
            }
    }
*/

    private void _discard_edit() {
        // show confirm dialog...
        new AlertDialog.Builder(QNNoteView.this)
        .setTitle(R.string.discard)
        .setMessage(R.string.show_discard_dialog_message)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //after discard the edit, revert the static values to initial values
                //not clear the mDBReminder for the bug: discard the eidt and enter the edit from the detail again, it won't call oncreate()
                setResult(RESULT_CANCELED);
                /*2012-12-7, add by amt_sunzhao for SWITCHUITWO-156 */
                _cleanmFileUri();
                /*2012-12-7, add end*/
                //it is not from other app, so back to previous detail view or  Thumbnails
                if ( oldRunMode == _RunningModeE.DETAIL) {
                   //to previous detail view
                   if (mOldContentUri == null ) {
                      //cannot back to previous detail view, so return
                      finish();
                      return;
                   } else {
                      setContentUri(mOldContentUri);
                      mOldContentUri = null;
                      mValues = getDBData();
                      getCommonValues(); //in case bg color is changed
                      //set the running mode
                      oldRunMode = currentRunMode;
                      currentRunMode = _RunningModeE.DETAIL;
                      setupNoteView();
                   }
                } else {
                   //else back to where it from
                   finish();
                   return;
                }
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
               ; // nothing to do!!!
            }
        })
        .show();
        
    }

    /** DONOT USE THIS DIRECTLY!!! **/
    private void _clean(Uri uri) {
        QNDev.logd(TAG, "_clean(Uri uri)" );
        if(null != uri) {
            // Until now only file uri is considered...
            if (uri.getScheme().startsWith("file")) {
                //IKSF-3740 :For sounderRecorder file is deleted
                QNDev.log(TAG+"in _clean() to delete:"+uri);
                //only delete it if it is the last note which is referenced this media resource
                if (QuickNotesDB.countInQuickNoteDB(QNNoteView.this, uri) > 0) {
                   QNDev.log(TAG+"has notes reference this file, so cannot delete it");
                   return;
                } else {
                   QNDev.log(TAG+"no note will reference this file, so delete it");
                   new File(uri.getPath()).delete();
                }
            }
        }
    }
    
    private void _cleanmFileUri() {QNDev.logd(TAG, "_cleanmFileUri(Uri uri)" ); _clean(mFileUri); mFileUri = null; }
    private void _cleanNewFileUri() {QNDev.logd(TAG, "_cleanNewFileUri(Uri uri)" ); _clean(mNewFileUri); mNewFileUri = null; }
    

    /**
     * 
     * @param qnc
     * @param uri
     * @param title : if null, do nothing to Title Box.
     * @param bgcolor
     * @return
     */
    private void setupMainView() {
        //activiy is visiable again; set it here again otherwise the detailview activity will be finish after destroyQNContent() if it is a paused voice note
        mActivityVisiable = true;

        mViewGroup = (ViewGroup)findViewById(R.id.main);
        mThumbnail = (ImageView)findViewById(R.id.thumbnail);
        mThumbnail.setOnClickListener(this);
        //for detail top icons
        mSetReminder = (ImageView)findViewById(R.id.set_reminder);
        mSetReminder.setOnClickListener(this);
        mTrash = (ImageView)findViewById(R.id.trash);
        mTrash.setOnClickListener(this);
        mShare = (ImageView)findViewById(R.id.share);
        mShare.setOnClickListener(this);
        mAdd = (ImageView)findViewById(R.id.add);
        mAdd.setOnClickListener(this);
        //for edit top icons
        mEditOK = (ImageView)findViewById(R.id.edit_ok);
        mEditOK.setOnClickListener(this);
        mEditCancel = (ImageView)findViewById(R.id.edit_cancel);
        mEditCancel.setOnClickListener(this);
        //for title
        mTitlePart = ((RelativeLayout)findViewById(R.id.title));
        mIndexCount = ((TextView)findViewById(R.id.count));
        mEditTitle = ((EditText)findViewById(R.id.edit_title));
        mEditTitle.setCustomSelectionActionModeCallback(mCopyPasteCallback);
        mEditTitle.addTextChangedListener(new TextWatcher() {
          public void afterTextChanged(Editable s) {
            //in note.xml, we set the maxlength for title to 30 which will full-fill the window width when it is the smallest text size
            if (s.length() == 30 ) {
                 Toast.makeText(QNNoteView.this, R.string.title_maxLength, Toast.LENGTH_LONG).show();
             }
          }
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {
              //do Nothing
          }
          public void onTextChanged(CharSequence s, int start, int before, int count) {
             //do Nothing
          }
        });
        mDetailTitle = ((TextView)findViewById(R.id.detail_title));
        mDetailTitle.setOnClickListener(this);
        mDetailTitle.setOnTouchListener(new View.OnTouchListener() {
           public boolean onTouch(View v, MotionEvent event) {
              //get the cursor position in detail text view
              Layout layout = ((TextView) v).getLayout();
              int x = (int)event.getX();
              int y = (int)event.getY();
              if (layout != null){
                 int line = layout.getLineForVertical(y);
                 mOffset = layout.getOffsetForHorizontal(line, x);
              }
              return false;
            }
         });
        mTopBar = (RelativeLayout)findViewById(R.id.top_bar);
        mDetailTitleScroll = (ScrollView)findViewById(R.id.detail_title_scrolling);
        mEditTitleScroll = (ScrollView)findViewById(R.id.edit_title_scrolling);
        //for text content of text note
        mTextContainer = (RelativeLayout)findViewById(R.id.scrolling_text);
        mEditTextScroll = (ScrollView)findViewById(R.id.edit_scrolling_area);
        mEditTextContent = (EditText)findViewById(R.id.edit_text_content);
        mEditTextContent.setCustomSelectionActionModeCallback(mCopyPasteCallback);
        mDetailTextScroll = (ScrollView)findViewById(R.id.detail_scrolling_area);
        mDetailTextContent = (TextView)findViewById(R.id.detail_text_content);
        mDetailTextContent.setOnClickListener(this);
        mDetailTextContent.setOnTouchListener(new View.OnTouchListener() {
           public boolean onTouch(View v, MotionEvent event) {
              //get the cursor position in detail text view
              Layout layout = ((TextView) v).getLayout();
              int x = (int)event.getX();
              int y = (int)event.getY();
              if (layout != null){
                 int line = layout.getLineForVertical(y);
                 mOffset = layout.getOffsetForHorizontal(line, x);
              }
              return false;
            }
         });

        mDetailTextContent.setOnLongClickListener(new View.OnLongClickListener() {        	
            @Override
            public boolean onLongClick (View v) {
                //since nothing is in here, nothing will happen.  

                return true;
            }            
        });
        mImageContent = (ImageView)findViewById(R.id.image_content);
        mImageContentLayout = (RelativeLayout)findViewById(R.id.image_small);
        mImageContentLayout.setOnClickListener(this);
        mImageDetail = (ImageView)findViewById(R.id.image_detail);
        mImageDetail.setOnClickListener(this);
        mImageEditBtn = (ImageView)findViewById(R.id.image_edit_btn);
        mImageEditBtn.setOnClickListener(this);
        mVoiceContent = (RelativeLayout)findViewById(R.id.voice_content);
        mPlayPause = (ImageView)findViewById(R.id.playpause);
        
        mContentPart = (RelativeLayout)findViewById(R.id.content);
        mBackgroundBtns = (LinearLayout)findViewById(R.id.bottom_buttons);
        mBackgroundBtn_1=(ImageView)findViewById(R.id.bg_button_1);
        mBackgroundBtn_1.setOnClickListener(this);
        mBackgroundBtn_2=(ImageView)findViewById(R.id.bg_button_2);
        mBackgroundBtn_2.setOnClickListener(this);
        mBackgroundBtn_3=(ImageView)findViewById(R.id.bg_button_3);
        mBackgroundBtn_3.setOnClickListener(this);
        mBackgroundBtn_4=(ImageView)findViewById(R.id.bg_button_4);
        mBackgroundBtn_4.setOnClickListener(this);
        mEditVoiceDuration = ((TextView)findViewById(R.id.edit_voice_duration));
        mVoiceIcon = (ImageView)findViewById(R.id.voice_icon);
        mDetailVoiceLayout = ((LinearLayout)findViewById(R.id.detail_voioce));
        
        //for reminder
        mReminderIndicator = (View) findViewById(R.id.reminder);
        mRemindertime = (TextView) findViewById(R.id.reminder_time);
        //for Error layout
        mErrorLayout = (RelativeLayout)findViewById(R.id.error);

        // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
        mCallInfo = (ViewGroup) findViewById(R.id.call_info);
        mCallInfoType = (ImageView)findViewById(R.id.call_info_type_icon);
        mCallInfoName = (TextView) findViewById(R.id.call_info_name);
        mCallInfoNumber = (TextView) findViewById(R.id.call_info_number);
        mCallInfoDate = (TextView) findViewById(R.id.call_info_date);
        // END IKCNDEVICS-751

        setupNoteView();
    }

    @Override
    public void onClick(View v) {
    	QNDev.log("onClick E. mIsResumed is:"  + mIsResumed + ", Visibility is:" + v.getVisibility());
		/*2012-9-10, add by amt_sunzhao for SWITCHUITWOV-92 */ 
    	if(!mIsResumed) {
    		// if screen is not focused, do not response to click event.
    		return;
    	}
		/*2012-9-10, add end*/
    	/*2012-9-12, add by amt_sunzhao for SWITCHUITWOV-86 */ 
    	// if view is not visible, do not response event.
    	if(View.VISIBLE != v.getVisibility()) {
    		return;
    	}
		/*2012-9-12, add end*/ 
    	
        if (mScrollingViews && (currentRunMode == _RunningModeE.DETAIL) ) {
           //it is scrolling views in detail note, no need to take actions for this view clicking
           //otherwise 1. if it is image note: the image view's detail image view will pop up unexpectedly
           //2. the text note will be triggered to edit mode sometimes. 
           return;
        }
        switch (v.getId()) {
            case R.id.thumbnail: 
            	QNDev.log("thumbnail clicked E.");
                //if it is in editing mode, then save the note firstly, then back to thumbnails
                if ( currentRunMode != _RunningModeE.DETAIL) {
                    saveEditingNote();
                }
                Intent intent = new Intent();
                intent.setClass(QNNoteView.this, QNDisplayNotes.class);
                intent.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return;

            case R.id.set_reminder:           	
                // enter the reminder setting
                Intent intentReminder = new Intent("android.intent.action.Reminder");
                intentReminder.setClass(QNNoteView.this, QNSetReminder.class);
                QNDev.log(TAG+"reminder: mDBReminder = "+mDBReminder);
                intentReminder.putExtra("dbReminderSet", mDBReminder);
                intentReminder.putExtra("from","QNNoteView");
                if (mContentUri != null) {
                	intentReminder.putExtra("content_uri", getContentUriString());
                }
                startActivityForResult(intentReminder, REQCODE_REMINDER_SET);
                return;

            case R.id.trash:
                //by clicking Trash btn, it will delete the note and show the next note
                 if ((mDeleteAlertDialog == null) || (!mDeleteAlertDialog.isShowing())) {
                   mDeleteAlertDialog = new AlertDialog.Builder(QNNoteView.this)
                    .setTitle(R.string.delete)
                    .setMessage(R.string.delete_confirm)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
				/*2012-11-28, add by amt_sunzhao for SWITCHUITWO-143 */	
                        	if(null == mDeleteAlertDialog) {
                        		return;
                        	}
				/*2012-11-28, add end*/
                            if (QuickNotesDB.delete(QNNoteView.this,  mContentUri)) {
                                QNDev.logd(TAG, "notes deleted, to update view");
                                Intent i = new Intent(QNConstants.INTENT_ACTION_NOTE_DELETED);
                                //just put special value 0 here
                                i.putExtra(QNConstants.GRID_ID, 0);
                                sendBroadcast(i);
                                mCount = QuickNotesDB.getSize(QNNoteView.this);
                                if (mCount <= 0) {
                                    //go to thumbnails
                                    Intent intent = new Intent();
                                    intent.setClass(QNNoteView.this, QNDisplayNotes.class);
                                    intent.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(intent);
                                    finish();
                                    return;
                                }
                                if (mIndex > mCount - 1) {
                                    mIndex = mCount -1;
                                }
                                setContentUri(QuickNotesDB.getItem(QNNoteView.this, mIndex));
                                if (null == mContentUri) {
                                   Toast.makeText(QNNoteView.this, R.string.unexpected_failure, Toast.LENGTH_SHORT);
                                   finish();
                                   return;
                                }
                                mValues = getDBData();
                                if (null == mValues) {
                                   Toast.makeText(QNNoteView.this, R.string.unexpected_failure, Toast.LENGTH_SHORT);
                                   finish();
                                   return;
                                }
                                getCommonValues();
                                setupNoteView();
                            }
				/*2012-11-28, add by amt_sunzhao for SWITCHUITWO-143 */	
                            mDeleteAlertDialog = null;
				/*2012-11-28, add end*/
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
				/*2012-11-28, add by amt_sunzhao for SWITCHUITWO-143 */	
                             //; // nothing to do!!!
                        	mDeleteAlertDialog = null;
				/*2012-11-28, add end*/
                        }
                    })
                    .show();
                }
                return;

            case R.id.share:
                //by clicking share btn, it will share the note to other apps
                Intent intent_share = null;
                if(mContentUri == null){
                	return;
                }
                if(isMediaContent()) {
                    intent_share = intentMedianote(mContentUri);
                } else if(isTextContent()) {
                    intent_share = intentTextNote(mContentUri);
                } else { QNDev.qnAssert(false); }

                // Launch chooser to share quicknote via ...
                final CharSequence chooseTitle = getText(R.string.share_via);
                final Intent chooseIntent = Intent.createChooser(intent_share, chooseTitle);
                try {
                    startActivity(chooseIntent);
                } catch (ActivityNotFoundException ex) {
                    Toast.makeText(QNNoteView.this, R.string.share_error, Toast.LENGTH_SHORT).show();
                }
                return;

            case R.id.add:
                //by clicking add btn, it will create a new note
                if(QNUtil.isTextLoad(this)) {
                    boolean preCondition = false;
                    if (!QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
                        // text note store on phone memory
                        preCondition = true;
                    } else {
                        preCondition = QNUtil.checkStorageCard(this);
                    }
    
                    if (preCondition ) {
                    	QNUtil.initDirs(this);
                        Intent intentNoteNew = new Intent(QNConstants.INTENT_ACTION_NEW)
                        .setClassName(QNConstants.PACKAGE, QNConstants.PACKAGE + ".QNNoteView")
                        .setDataAndType(Uri.parse("qntext:"), QNConstants.MIME_TYPE_TEXT);                
                        
                        if (QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
                            Uri uriFilePath = QNUtil.prepareTextNote();
                            if (uriFilePath == null) {                         
                                finish();
                                return;
                            }
                            intentNoteNew.setDataAndType(uriFilePath, QNConstants.MIME_TYPE_TEXT);
                        }
                       
                        intentNoteNew.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP );
                        startActivity(intentNoteNew);
                    }
                } else {
                    Intent newIntent = new Intent();
                    newIntent.setClass(QNNoteView.this, QNNewActivity.class);
                    startActivity(newIntent);
                }
                return;

        	case R.id.edit_ok:
			// by clicking EditOk btn, it will save the note
        	QNDev.log("edit_ok clicked E.");
            int result = saveEditingNote();	
			if (result == SAVEEDITNOTE_RETURN_CREATE_FAIL || result == SAVEEDITNOTE_RETURN_EXCEPTION) {
				finish();
				return;
			} else {
				if (mFromOtherApp) {
					finish();
				} else {
					oldRunMode = currentRunMode;
					currentRunMode = _RunningModeE.DETAIL;
					toDetailMode();
				}
				return;
			}

            case R.id.edit_cancel:
                //from otherAPP, then it will back to previous app
                //otherwise, back to previous detail note or thumbnails
                if (mFromOtherApp) {
                	/*2012-12-7, add by amt_sunzhao for SWITCHUITWO-156 */
                	_cleanmFileUri();
                	/*2012-12-7, add end*/
                    finish();
                } else {
                    _discard_edit();
                }
                return;

            case R.id.bg_button_1:
                //this btn is only clicked in edit mode
                if (currentRunMode == _RunningModeE.DETAIL) {
                   return;
                }
                if (mMimeType.startsWith("text/") || mMimeType.startsWith("audio/") ) {
                    mBgColor = _BackgroundRes.GRIDME.column();
                    mContentPart.setBackgroundResource(_BackgroundRes.GRIDME.textEditResId());
                    if (mMimeType.startsWith("audio/")) {
                      mVoiceIcon.setImageResource(_BackgroundRes.GRIDME.soundNoteIconResId());
                    }
                }
                return;

            case R.id.bg_button_2:
                if (currentRunMode == _RunningModeE.DETAIL) {
                   return;
                }
                if (mMimeType.startsWith("text/") || mMimeType.startsWith("audio/")) {
                    mBgColor = _BackgroundRes.PROJECT_PAPER.column();
                    mContentPart.setBackgroundResource(_BackgroundRes.PROJECT_PAPER.textEditResId());
                    if (mMimeType.startsWith("audio/")) {
                      mVoiceIcon.setImageResource(_BackgroundRes.PROJECT_PAPER.soundNoteIconResId());
                    }
                }
                return;

            case R.id.bg_button_3:
                if (currentRunMode == _RunningModeE.DETAIL) {
                   return;
                }
                if (mMimeType.startsWith("text/") || mMimeType.startsWith("audio/")) {
                    mBgColor = _BackgroundRes.MATHEMATICS.column();
                    mContentPart.setBackgroundResource(_BackgroundRes.MATHEMATICS.textEditResId());
                    if (mMimeType.startsWith("audio/")) {
                      mVoiceIcon.setImageResource(_BackgroundRes.MATHEMATICS.soundNoteIconResId());
                    }
                }
                return;

            case R.id.bg_button_4:
                if (currentRunMode == _RunningModeE.DETAIL) {
                   return;
                }
                if (mMimeType.startsWith("text/") || mMimeType.startsWith("audio/")) {
                    mBgColor = _BackgroundRes.GRAPHY.column();
                    mContentPart.setBackgroundResource(_BackgroundRes.GRAPHY.textEditResId());
                    if (mMimeType.startsWith("audio/")) {
                      mVoiceIcon.setImageResource(_BackgroundRes.GRAPHY.soundNoteIconResId());
                    }
                }
                return;

            case R.id.detail_title:
                if (currentRunMode == _RunningModeE.DETAIL) {
                   if(mDetailTitle.getSelectionStart()==-1 && mDetailTitle.getSelectionEnd()==-1){
                      //This condition will satisfy only when it is not an autolinked text
                      //Fired only when you touch the part of the text that is not hyperlinked
                      oldRunMode = currentRunMode;
                      currentRunMode = _RunningModeE.EDIT_EDIT;
                      String currentTitle = mDetailTitle.getText().toString();
                      if (currentTitle != null ) {
                         mEditTitle.setText(currentTitle);
                      }
                      mDetailTitleScroll.setVisibility(View.GONE);
                      mEditTitleScroll.setVisibility(View.VISIBLE);
                      mEditTitle.requestFocus();
                      int editTextLength = mEditTitle.getText().toString().length();
                      if (mOffset > editTextLength) {
                         mOffset = editTextLength;
                      } else if (mOffset < 0) {
                          mOffset = 0;
                      }
                      mEditTitle.setSelection(mOffset);
                      mOldContentUri = mContentUri;
                      toEditMode();
                   }
                }
                return;

            case R.id.detail_text_content:
                if (currentRunMode == _RunningModeE.DETAIL) {
                  if(mDetailTextContent.getSelectionStart()==-1 && mDetailTextContent.getSelectionEnd()==-1){
                      //This condition will satisfy only when it is not an autolinked text
                      //Fired only when you touch the part of the text that is not hyperlinked
                      oldRunMode = currentRunMode;
                      currentRunMode = _RunningModeE.EDIT_EDIT;
                      String currentTitle = mDetailTitle.getText().toString();
                      if (currentTitle != null ) {
                         mEditTitle.setText(currentTitle);
                      }
                      mDetailTextScroll.setVisibility(View.GONE);
                      mEditTextScroll.setVisibility(View.VISIBLE);
                      mEditTextContent.requestFocus();
                      int editTextLength = mEditTextContent.getText().toString().length();
                      if (mOffset > editTextLength) {
                          mOffset = editTextLength;
                      } else if (mOffset < 0) {
                          mOffset = 0;
                      }
                      mEditTextContent.setSelection(mOffset);
                      mOldContentUri = mContentUri;
                      toEditMode();
                   }
                }
                return;

            case R.id.image_small:
			/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-194 */ 
            	mIsImageDetailMode = true;
                //hide all other icons and only show the image in full screen
                mViewGroup.setBackgroundResource(0);
                mQNContent.detailView(QNNoteView.this, mViewGroup, mFileUri);
                mTopBar.setVisibility(View.GONE);
                mContentPart.setVisibility(View.GONE);
                mBackgroundBtns.setVisibility(View.GONE);
                mImageDetail.setVisibility(View.VISIBLE);
                mImageEditBtn.setVisibility(View.GONE);
                return;

            case R.id.image_detail:
            	mIsImageDetailMode = false;
				/*2012-9-18, add end*/ 
                //back to normal view
                mViewGroup.setBackgroundResource(R.drawable.bg_board_softwood);
                mTopBar.setVisibility(View.VISIBLE);
                mContentPart.setVisibility(View.VISIBLE);
                /*2012-11-26, add by amt_sunzhao for SWITCHUITWO-96 */ 
                /*
                 * As the bitmap of Note may be recycled by invoked mQNContent.detailView,
                 * set the note content again
                 */
                mNoteView = mQNContent.noteView(QNNoteView.this, mViewGroup);
                /*2012-11-26, add end*/
                mBackgroundBtns.setVisibility(View.GONE);
                mImageDetail.setVisibility(View.GONE);
                mImageEditBtn.setVisibility(View.VISIBLE);
                mImageDetail.setImageResource(0);
                return;

            case R.id.image_edit_btn:
                //to edit mode
                if (currentRunMode == _RunningModeE.DETAIL) {
                   oldRunMode = currentRunMode;
                   currentRunMode = _RunningModeE.EDIT_EDIT;
                   String currentTitle = mDetailTitle.getText().toString();
                   if (currentTitle != null ) {
                     mEditTitle.setText(currentTitle);
                   }
                }
                mOldContentUri = mContentUri;
                toEditMode();
                //jump to sketch edit
                Intent i = new Intent(QNConstants.INTENT_ACTION_EDIT);
                if (mNewFileUri == null) {
                    i.setDataAndType(mFileUri, "image/jpeg");
                } else {
                    i.setDataAndType(mNewFileUri, "image/jpeg");
                }
                i.setClassName("com.motorola.quicknote", "com.motorola.quicknote.QNSketcher");
                startActivityForResult(i, REQCODE_QNC_EDIT);
                return;

            default:
                //do nothing;
                break;
        }
    }
    

    /**************************
     * Overriding.
     **************************/
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        QNDev.log(TAG+" onCreate");
        setResult(RESULT_CANCELED);

        Intent  intent  = getIntent();
    
            if (QNNewActivity.mQNNewActivityIsRunning && null==intent ) {
            //a new note is already in creating process, so return;
            QNDev.log(TAG+" QNNewActivity is already running, so finish it and return");
            finish();
            return;
        }

        //initial the static parameters
        mDBReminder = 0L;
        mReminderIndicator = null; 
        mOldContentUri = null;
        if (intent == null) { return;}
        receiveIntent(intent);

        mSlideGestureListener = new SlideGestureListener();
        mSlideshowGestureDetector = new GestureDetector(this, mSlideGestureListener);
        mCopyPasteCallback = new CopyPasteCallback();
        registerIntentReceivers();
        if (this.isFinishing ()){
           QNDev.log(TAG+" QNNoteView is in finishing, so return");
           return;
        }

        mActivityVisiable = true;
         _viewroot(R.id.main);
         setContentView(R.layout.note);
        final View activityRootView = findViewById(R.id.main);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(mMimeType.startsWith("image/") || QNUtil.is_landscape(QNNoteView.this)){
                    QNDev.log("onGlobalLayout, it's Image notes return !");
                    return;
                }
                
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                QNDev.log("onGlobalLayout, heightDiff = " + heightDiff + ", isIMEShow = " + isIMEShow); 
                if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                    if(!isIMEShow){
                        QNDev.log("onGlobalLayout ,Software Keyboard was  shown " );
                        isIMEShow = true;
                        mContentPart.setBackgroundResource(backgroundForIMEShow(mBgColor));
                        mViewGroup.setBackgroundResource(R.drawable.bg_ime_board_softwood);
                        mBackgroundBtns.setVisibility(View.GONE);
                    }
                }else {
                    if(isIMEShow){
                        QNDev.log("onGlobalLayout,Software Keyboard was  hiden  ");
                        isIMEShow = false;   
                        mViewGroup.setBackgroundResource(R.drawable.bg_board_softwood);
                        QNDev.log("onGlobalLayout,Software Keyboard was  hiden  currentRunMode = " + currentRunMode);
                        if(currentRunMode !=  _RunningModeE.DETAIL){
                            mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "edit", "text"));
                            mBackgroundBtns.setVisibility(View.VISIBLE);
                        }else{
                            mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "view", "text"));
                        }
                    }
                }
            }
        });
         
         setupMainView();
    }

    private int backgroundForIMEShow(int BgColor){
       int resId = R.drawable.bg_text_note_gridme_single_ime; // default to yellow background.
       switch(BgColor){
           case R.color.blue:
                resId = R.drawable.bg_text_note_mathematics_single_ime;
                break;
           case R.color.green:
               resId = R.drawable.bg_text_note_project_paper_single_ime;
                break;
           case R.color.orange:
               resId = R.drawable.bg_text_note_graphy_single_ime;
                break;
       }
       return resId;
    }

    private void receiveIntent(Intent intent) {
        String action = intent.getAction();
        if((QNConstants.INTENT_ACTION_NEW).equals(action)) {
            String from = intent.getStringExtra("from");
            if ((from != null) && (from.equals("textShare"))) {
                QNAppWidgetConfigure.saveWidget2Update(this, -1);
                mFromOtherApp = true;
            }

            mFileUri = intent.getData();
            mMimeType = intent.getType();

            if ((null == mMimeType) || (null == mFileUri)) {
               finish();
               return;
            } else if ( mMimeType.startsWith("audio/") ) {
               //check the voice file storage prefix
               String path = mFileUri.getPath();  
               mFileUri = checkStoragePrefix(path);
            }

            if ( null == mFileUri ) { 
               finish();
               return;
            }
            setContentUri(null);
            /*2013-1-23, add by amt_sunzhao for SWITCHUITWO-575 */ 
            oldRunMode = currentRunMode;
            /*2013-1-23, add end*/ 
            currentRunMode = _RunningModeE.EDIT_NEW;
        } else if ((QNConstants.INTENT_ACTION_EDIT).equals(action)) {
            setContentUri(intent.getData());
            mMimeType = intent.getType();
            if ( ( null == mContentUri ) || (null == mMimeType) ) { 
               finish();
               return;
            }
            /*2013-1-23, add by amt_sunzhao for SWITCHUITWO-575 */ 
            oldRunMode = currentRunMode;
            /*2013-1-23, add end*/ 
            currentRunMode = _RunningModeE.EDIT_EDIT;
            QNDev.qnAssert(QuickNotesDB.is_qnuri_type(mContentUri));
            mFileUri = QNUtil.buildUri((String)QuickNotesDB.read(this, mContentUri, QNColumn.URI));
            QNDev.log(TAG+"reminder: onCreate: mDBReminder = "+mDBReminder);
        } else if ((Intent.ACTION_ATTACH_DATA).equals(action)) {
            // Camera or Sound Recorder
           //it is not from widget, so just set widget2update = -1
           QNDev.log(TAG+"from other apps-> QNNoteView with ACTION_ATTACH_DATA, update the configure...");
           QNAppWidgetConfigure.saveWidget2Update(this, -1);
           mFromOtherApp = true;

           Uri ori_uri = intent.getData();
           mMimeType = intent.getType();

            if (  null == ori_uri   ) {
               finish();
               return;
            }
            /*2013-1-23, add by amt_sunzhao for SWITCHUITWO-575 */ 
            oldRunMode = currentRunMode;
            /*2013-1-23, add end*/ 
            currentRunMode = _RunningModeE.EDIT_ATTACH;
            String schema = ori_uri.getScheme();
            if (schema.equals("file")) {        // ? -> Quick Note
                if ((null != mMimeType) && mMimeType.startsWith("image/")) {
                    //this is a image by setting as
                    //try to copy this image to quicknote/image folder
                    if (!QNUtil.checkStorageCard(QNNoteView.this)) {
                       finish();
                       return;
                    }
                    QNUtil.initDirs(this);
                    String newFileName = QNUtil.createNewImageName();
                    File newFile = new File(newFileName);
                    String oldFileName = ori_uri.getPath();
                    File oldFile = new File(oldFileName);
                    QNDev.log(TAG+"newFile = "+newFile+" oldFile = "+oldFile);
                    if (oldFile.exists() && QNUtil.copyfile(oldFile, newFile)) {
                        mFileUri = Uri.parse("file://" + newFileName);
                        QNDev.log(TAG+ "file is copied successfully: mFileUri=" + mFileUri.toString()+ " mimeType=" + mMimeType);
                   } else {
                        QNDev.log(TAG+"Error found! old file not exists or copy failed. Return;");
                        Toast.makeText(this, R.string.fail_to_read_file_toast, Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                  } 

                }
              
            } else if (schema.equals("content")) {      // Camera -> Quick Note or SoundRecorder -> QN
                // data=content://media/external/images/media/2, query db for path
               // ContentResolver cr = this.getContentResolver();
					/*2012-10-18, add by amt_sunzhao for SWITCHUITWOV-270 */
                String[] projection = new String[] {
                                             MediaStore.Images.Media.DATA,
                                             MediaStore.Images.Media.MIME_TYPE,
                                             MediaStore.Images.Media.ORIENTATION
                                          }; 
                Cursor c = null;
                int orientation = 0;					
					/*2012-10-18, add end*/
				try {
					c = managedQuery(ori_uri, projection, null, null, null);
					if (c != null && c.getCount() > 0) {

						if (c.moveToFirst()) {
							ori_path = c
									.getString(c
											.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
							QNDev.log(TAG + " ori file path = " + ori_path);
							mMimeType = c
									.getString(c
											.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE));
							QNDev.logd(TAG,
									"ATTACH_DATA: image query db, mimeType="
											+ mMimeType);
							/*2012-10-18, add by amt_sunzhao for SWITCHUITWOV-270 */
							orientation = c.getInt(c.getColumnIndex( MediaStore.Images.Media.ORIENTATION));
							/*2012-10-18, add end*/
						}
					}

				} catch (Exception e) {
					e.printStackTrace();
					QNDev.log(TAG + " exception when query ori_uri");
				} finally {
					/*2012-9-11, add by amt_sunzhao for T810T_P004479 */ 
					/*
					 * do not close cursor from return of managedQuery, please refer to managedQuery in sdk 
					if (c != null) {
						 try {
							c.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					*/
					/*2012-9-11, add end*/ 
					// c.close();
				}

                //from Shadow, SoundRecorder -> QuickNote will also come to this case, so judge the mimetype
                QNDev.log(TAG+ " camera/sounderRecorder ? mMimeType = "+mMimeType);
                if ((null != mMimeType) && mMimeType.startsWith("image/")) {
                   //it is Camera -> QuickNote           
                   QNDev.log(TAG+" Camera -> quickNote");
                   mFileUri = Uri.parse("file://" +ori_path);
                   //show waiting UI and try to get final rotated image
                   showDialog(DIALOG_IMAGE_PROCESS_START);
                   initImageProcess();
						/*2012-10-18, add by amt_sunzhao for SWITCHUITWOV-270 */
                   //mImageProcessHandler.sendEmptyMessage(MSG_IMAGE_PROCESS_START);
                   final Message msg = mImageProcessHandler.obtainMessage(MSG_IMAGE_PROCESS_START);
                   msg.arg1 = orientation;
                   mImageProcessHandler.sendMessage(msg);
						/*2012-10-18, add end*/
               } else if ((null != mMimeType) && mMimeType.startsWith("audio/")) {
                   //it is SounderRecorder -> quickNote
                   QNDev.log(TAG+ "SounderRecorder -> quickNote");
                   mFileUri = checkStoragePrefix(ori_path);
                   if (null == mFileUri) {
                      finish();
                      return;
                   }
               } else {
                   QNDev.log(TAG+ "Others -> quickNote");
                   mFileUri = Uri.parse("file://" +ori_path);
                   QNDev.log(TAG+"  mFileUri = "+ mFileUri);
               }

            }
        } else {
        	/*2013-1-23, add by amt_sunzhao for SWITCHUITWO-575 */ 
            oldRunMode = currentRunMode;
            /*2013-1-23, add end*/ 
           //it is detail view
           currentRunMode = _RunningModeE.DETAIL;
           String from = intent.getStringExtra("from");
           if ((from != null) && (from.equals("widget"))) {
              QNDev.log(TAG+" from widget");
              String uri_string = intent.getStringExtra("contentUri");
              int widgetId = intent.getIntExtra("widgetId",-1);

              if (null == uri_string) {
                 QNDev.log(TAG+" onCreate: uri_string = null, Error case!");
                 finish();
             }

              if ("Special_widget".equals(uri_string)) {
                  QNDev.log(TAG+" specail widget view");
                  //it is a special widget to create note
                  if ( (currentRunMode == _RunningModeE.DETAIL) || (currentRunMode == null)) {
                      QNDev.log(TAG+" try to start QNNewActivity");
                      //currentRunMode = null means no note view is running
                      //start QNNewActivity 
                      Intent newIntent = new Intent();
                      newIntent.setClass(this, QNNewActivity.class);
                      newIntent.putExtra("contentUri", uri_string);
                      newIntent.putExtra("from","widget");
                      newIntent.putExtra("widgetId",widgetId);
                      startActivity(newIntent);
                      finish();
                      return;
                  } else {
                      //it is in note edit mode, so no need to start QNNewActivity,just pop up current editing note
                      return;
                  }
              } else {
                  //it is a normail widget
                 setContentUri(QNUtil.buildUri(uri_string));
                 QNDev.log(TAG+"onCreate: uri_string = "+uri_string+" mContentUri = "+mContentUri+" widgetId = "+widgetId);
                 if (widgetId != -1) {
                   QNDev.log(TAG+"from widget-> detail, update the configure...");
                   QNAppWidgetConfigure.saveWidget2Update(this, widgetId);
                 }
              }
           // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
           } else if ((from != null) && (from.equals("call_log"))) {
              //it is not from widget, so just set widget2update = -1
              QNAppWidgetConfigure.saveWidget2Update(this, -1);
              mFromOtherApp = true;

             String uri_string = intent.getStringExtra("contentUri");
             setContentUri(QNUtil.buildUri(uri_string));
             if (null == mContentUri) {
                 QNDev.log(TAG+" onCreate: in-call note: DETAIL: get mContentUri fail.");
                 finish();
             }
             QNDev.log(TAG+"onCreate: in-call note: DETAIL: mContentUri="+getContentUriString());
           // END IKCNDEVICS-751
           } else {
             QNDev.log(TAG+" from thumbnail");
             mIndex = intent.getIntExtra("index", 0);
             setContentUri(QuickNotesDB.getItem(QNNoteView.this, mIndex));
           }
           //common part for detail view
           if (null == mContentUri) {
               finish();
               return;
           }
           mValues = getDBData();
           if (null == mValues) {
                Intent updateIntent = new Intent();
        	  updateIntent.setAction("com.motorola.quicknote.action.FORCE_UPDATE_WIDGET");
        	  sendBroadcast(updateIntent);
                Toast.makeText(QNNoteView.this, R.string.unexpected_failure, Toast.LENGTH_SHORT);
                finish();
                return;
           }
       
           getCommonValues();

           QNDev.log(TAG+"onCreate:  mIndex = "+mIndex);
           if (mIndex < 0 ) {
              //this is a invalid or not existed uri
              Intent intentActivity = new Intent();
              intentActivity.setClass(QNNoteView.this, QNDisplayNotes.class);
              intentActivity.setFlags (Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
              startActivity(intentActivity);
              finish();
              return;
            }
        }
    }


    @Override
    protected Dialog onCreateDialog(int id) {
      if (id == DIALOG_IMAGE_PROCESS_START) {
         mProgressDialog = new ProgressDialog(QNNoteView.this);
         mProgressDialog.setMessage(getResources().getString(R.string.image_process));
         mProgressDialog.setCancelable(false);
         mProgressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
             public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                 if ((keyCode == KeyEvent.KEYCODE_SEARCH ) || (keyCode == KeyEvent.KEYCODE_BACK ))
                  {
                     return true; // Pretend we processed it
                  } else {
                     return false; // Any other keys are still processed as normal
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

    public void onCancel (DialogInterface dialog) {
        ;
    }


    private void initImageProcess() {
      try {
         if (mWaitingUiHandler == null) {
            mWaitingUiHandler = new WaitingUiHandler();
         }

         if (mImageProcessHandler == null) {
            HandlerThread imageProcessThread = new HandlerThread("imageProcess handler: Process Thread");
            imageProcessThread.start();
            Looper looper = imageProcessThread.getLooper();
            if (looper != null) {
               mImageProcessHandler = new ImageProcessHandler(looper, mWaitingUiHandler);
            }
         }
      } catch (Exception e) {
         Toast.makeText(QNNoteView.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
         QNDev.log(TAG+" initImageProcess found image process exception =  "+e);
         finish();
      }
    }


    private class WaitingUiHandler extends Handler {
       @Override
       public void handleMessage(Message msg) {
         switch (msg.what) {
           case MSG_IMAGE_PROCESS_COMPLETE:
              destroyQNContent(mQNContent);
              mQNContent = prepareQNContent(mMimeType, mFileUri);
              setupNoteView();
              if (mProgressDialog != null) {
			  /*2012-9-11, add by amt_sunzhao for T810T_P004479 */ 
                 //mProgressDialog.dismiss();
            	  /*
            	   *  Only call dismissDialog will remove dialog in system, please use removeDialog.
            	   *  If use method dismiss, dialog can close, but if process crashed,
            	   *  when restore, the dialog will restore again.
            	   */
            	  
                 removeDialog(DIALOG_IMAGE_PROCESS_START);
              /*2012-9-11, add end*/ 
              }
              break;
           case MSG_IMAGE_PROCESS_FAILED:
              if (mProgressDialog != null) {
                 mProgressDialog.dismiss();
              }
              Toast.makeText(QNNoteView.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
              QNDev.log(TAG+" WaitingUiHandler found image process error");
              finish();
              break;
           case MSG_DISMISS_PROGRESS:
              if (mProgressDialog != null) {
                mProgressDialog.dismiss();
              }
              finish();
              break;
           default:
              break;
         }
       }
    };

    @Override
    public void onDestroy() {
      QNDev.log(TAG + " onDestroy()");
      try {
         if(null != mQNContent) {
           if (mQNContent.isPlayable()) {
             //cancel the notification
             NotificationManager nm = (NotificationManager) getSystemService(QNNoteView.NOTIFICATION_SERVICE);
             nm.cancel(mVoice);
           }
           mQNContent.stop();
           mQNContent.close();
         }

         hideSoftInput();
         if(null != mNewFileUri && !_have_same_content(mNewFileUri, mFileUri)) {
            _cleanNewFileUri();
         }
         
 /*
         if( _RunningModeE.EDIT_NEW == currentRunMode && !mMimeType.startsWith("text/" )) {
           _cleanmFileUri();
         }*/

         destroyQNContent(mQNContent);
         unregisterReceiver(mQuickNotesReceiver);
      } catch (Exception e) {
         QNDev.log(TAG + " onDestroy() catch exception e ="+ e);
      }
      super.onDestroy();
    }       


    @Override
    public void onResume() {
		/*2012-9-10, add by amt_sunzhao for SWITCHUITWOV-92 */ 
    	mIsResumed = true;
		/*2012-9-10, add end*/ 
       QNDev.log(TAG+ " onResume");
       //activiy is visiable again;
       mActivityVisiable = true;                     
       validateViewState();
       /*2012-12-12, add by amt_sunzhao for SWITCHUITWO-288 */
       refreshIndexCount();    
       super.onResume();
    }    
    
    private void refreshIndexCount() {
    	/*2012-12-17, add by amt_sunzhao for SWITCHUITWO-330 */
    	if(null == mContentUri) {
    		QNDev.log("mContentUri is null. return.");
    		return;
    	}
    	/*2012-12-17, add end*/
    	final int count = QuickNotesDB.getSize(this);
    	if(count != mCount) {
    		mCount = count;
    		mIndex = QuickNotesDB.getIndexByUri(QNNoteView.this, mContentUri);
    		// index/count on top bar
            String indexAndCount = (mIndex + 1) + "/" + mCount;
            mIndexCount.setText(indexAndCount);
    	}
    }
    /*2012-12-12, add end*/


    @Override
    protected void onNewIntent (Intent intent) {
       QNDev.log(TAG+" onNewIntent");
       setResult(RESULT_CANCELED);

       mOldContentUri = null;
       if (QNNewActivity.mQNNewActivityIsRunning) {
            QNDev.log(TAG+" QNNewActivity is running , so return");
            //a new note is already in creating process, so return;
            return;
       }
       if ( currentRunMode == _RunningModeE.DETAIL) {
         mOldContentUri = mContentUri;
         setIntent(intent);
         receiveIntent(intent);
         if (this.isFinishing ()){
           QNDev.log(TAG+" QNNoteView is finishing -2, so return.");
           return;
        }
        setupNoteView();
        return;
      }
    }


    @Override
    public void onPause() {
       QNDev.log(TAG+ " onPause");
		/*2012-9-10, add by amt_sunzhao for SWITCHUITWOV-92 */ 
       mIsResumed = false;
		/*2012-9-10, add end*/ 
       hideSoftInput(); 
       if(null != mQNContent && mQNContent.isPlayable() 
               && mQNContent.state() == NotestateE.STARTED) { 
          QNDev.log(TAG+"it is a voice note & playing! so need send out a notification for playback");
          RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
          views.setImageViewResource(R.id.icon, R.drawable.stat_notify_musicplayer);
          String path = (mQNContent.uri().getPath());
          if ( null != path) {
              //get the pure file name by deleting " /sdcard/voice/ "
              views.setTextViewText(R.id.voicenote_path, path.substring(14));
          } 
 
          if ((mDetailTitle != null) && (mDetailTitle.length()>0)) {
               views.setTextViewText(R.id.voicenote_title, mDetailTitle.getText().toString());
          }

          Notification notification = new Notification();
          notification.contentView = views;
          notification.flags |= Notification.FLAG_ONGOING_EVENT;
          notification.flags |= Notification.FLAG_AUTO_CANCEL;
          notification.icon = R.drawable.stat_notify_musicplayer;
          Intent nIntent = new Intent("com.motorola.quicknote.action.QUICKNOTE");
          nIntent.setClassName("com.motorola.quicknote",  "com.motorola.quicknote.QNNoteView");
          nIntent.setFlags (Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY
                            | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
          nIntent.putExtra("index", mIndex);
         
          notification.contentIntent = PendingIntent.getActivity(this, 0, nIntent, 0);
          
          NotificationManager nm = (NotificationManager) getSystemService(QNNoteView.NOTIFICATION_SERVICE);
          QNDev.log(TAG+"mIndex = "+mIndex);
          nm.notify(mVoice, notification);
       } 

       super.onPause();
    }


    @Override
    public void onStop() {
       QNDev.log(TAG+ " onStop");
       hideSoftInput();
      //we cannot finish the detail note even it is the only activity in the task, 
      //otherwise user cannot back to detail note when back from calling/email/browser
      super.onStop();
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if ( currentRunMode == _RunningModeE.DETAIL) {
           setContentView(R.layout.note);
           setupMainView();
        } else {
           //it is in editing mode, so need to remember current settings
           //1.title
           String title = null;
           title = mEditTitle.getText().toString();
           //2.text content
           String content = null;
           if (mMimeType.startsWith("text/")) {
              content = mEditTextContent.getText().toString();
           }
           //3.current focus and cursor position
           View focusv = getCurrentFocus();
           int focused = 0; //0: no focus; 1:title is fouced; 2:text content is focused
           int cursorPosition = 0;
           if (focusv != null) {
             if (mEditTitle == focusv) {
               focused = 1;
               cursorPosition = mEditTitle.getSelectionStart();
             } else if (mEditTextContent == focusv ) {
               focused = 2;
               cursorPosition = mEditTextContent.getSelectionStart();
             }
           }
           //4.current bg color selection
           int bgColor = mBgColor;

           setContentView(R.layout.note);
           setupMainView();

           //set back the settings
           //1.title
           mEditTitle.setText(title);
           //2.text content
           if (mMimeType.startsWith("text/")) {
              mEditTextContent.setText(content);
           }
           //3.focus
           if (focused == 1){
              mEditTitle.requestFocus();
              mEditTitle.setSelection(cursorPosition);
           } else if (focused == 2) {
              mEditTextContent.requestFocus();
              mEditTextContent.setSelection(cursorPosition);
           }
           //4.bg color
           mBgColor = bgColor;
           if (mMimeType.startsWith("audio/")) {
             //set the background
             mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "edit", "audio"));
             mVoiceIcon.setImageResource(_BackgroundRes.find_drawable(mBgColor, "edit", "voice_icon"));
           } else if (mMimeType.startsWith("text/")) {
             mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "edit", "text"));
           }

           if (mNoteView == null) {
              //use the yellow bg to the background to keep consistent with widget and thumbnails
              mContentPart.setBackgroundResource(R.drawable.bg_text_note_gridme_single);
           }
        }
    }
    
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
         super.onActivityResult(requestCode, resultCode, intent);
          if (requestCode == REQCODE_QNC_EDIT) {
             switch(resultCode) {
                 case RESULT_OK: {
                     QNDev.qnAssert(null != intent);
                     if (null == intent) { return;}
                     /*2012-12-18, add by amt_sunzhao for SWITCHUITWO-333 */
                     // clear file which old mNewFileUri pointed
                     if(null != mNewFileUri 
                    		 && !_have_same_content(mNewFileUri, mFileUri)) {
                    	 _clean(mNewFileUri);
                     }
                     /*2012-12-18, add end*/
                     mNewFileUri = intent.getData();
                     if(null != mNewFileUri && !_have_same_content(mNewFileUri, mFileUri)) {
                         //update the note content;
                         mNoteView = mQNContent.updateNoteView(QNNoteView.this, mViewGroup, mNewFileUri); 
                         if (mNoteView == null) {
                            QNDev.log(TAG+"Noteview is null, so will show the error info.");
                            mErrorLayout.setVisibility(View.VISIBLE);
                            mTextContainer.setVisibility(View.GONE);
                            mImageContentLayout.setVisibility(View.GONE);
                            mImageEditBtn.setVisibility(View.GONE);
                            mImageDetail.setVisibility(View.GONE);
                            mVoiceContent.setVisibility(View.GONE);
                            mShare.setClickable(false);
                            mShare.setImageResource(R.drawable.ic_share_disabled);
                            mContentPart.setBackgroundResource(R.drawable.bg_text_note_gridme_single);
                         } 
                     }  
                 } break;
                 
                 case RESULT_CANCELED: {
                    ; //do nothing
                 } break;
                 
                 default:
                     QNDev.qnAssert(false); // unexpected.
             }
         } else  if (requestCode == REQCODE_REMINDER_SET) {
             switch(resultCode) {
                 case RESULT_OK: {
                     if (null == mContentUri) {
                         //error case
                         return;
                     }
                     boolean reminder_enabled = intent.getBooleanExtra("ReminderEnable", false);
                     mDBReminder = intent.getLongExtra("ReminderSetTime", 0L);

                     //write it to DB
                     if ((reminder_enabled) && (0L != mDBReminder)) {
                         //set the reminder
                         QNUtil.setAlarm(this, mContentUri, 0L);
                         QNUtil.setAlarm(this, mContentUri, mDBReminder);
                         QuickNotesDB.write(this, mContentUri, QNColumn.REMINDER, mDBReminder);
                         //update the reminder icon
                         mReminderIndicator.setVisibility(View.VISIBLE);
                         Calendar c = Calendar.getInstance();
                         c.setTimeInMillis(mDBReminder);
                         QNSetReminder.setReminderFlag(this, mRemindertime, c);
                         //update the reminder icon in top bar
                         mSetReminder.setImageResource(R.drawable.note_detail_reminder_set_btn);
                     } else {
                         //cancel the reminder
                         mDBReminder = 0L;
                         QNUtil.setAlarm(this, mContentUri, 0L);
                         QuickNotesDB.write(this, mContentUri, QNColumn.REMINDER, 0L);
                         //update the reminder icon
                         if (mMimeType.startsWith("audio/")) {
                             mReminderIndicator.setVisibility(View.INVISIBLE);
                         } else {
                             mReminderIndicator.setVisibility(View.GONE);
                         }
                         //update the reminder icon in top bar
                         mSetReminder.setImageResource(R.drawable.note_detail_reminder_btn);
                     }
                     //update the mValues
                     mValues.put(QNColumn.REMINDER.column(), mDBReminder);
                     //update the view in layout
                     mViewGroup.refreshDrawableState();

                     Intent intentUpdate = new Intent(QNConstants.INTENT_ACTION_NOTE_UPDATED);
                     if (mContentUri != null) {
                        intentUpdate.putExtra(QNConstants.NOTE_URI, getContentUriString());
                     }
                     sendBroadcast(intentUpdate);

                 } break;
                 
                 case RESULT_CANCELED: {
                    ; //do nothing
                 } break;
                 
                 default:
                     QNDev.qnAssert(false); // unexpected.
             }        
         }
    }


    @Override
    public void onBackPressed() {
       if ( currentRunMode != _RunningModeE.DETAIL) {
          if(_RunningModeE.EDIT_NEW == currentRunMode || _RunningModeE.EDIT_ATTACH == currentRunMode) {
             //if it is a blank new note, then no need to save it
             if (mMimeType.startsWith("text/")) {
                String title = mEditTitle.getText().toString();
                String content = mEditTextContent.getText().toString();
                if ((title == null || title.length() == 0 ) && ( content == null || content.length() == 0)) {
                	/*2012-12-7, add by amt_sunzhao for SWITCHUITWO-156 */
                	_cleanmFileUri();
                    /*2012-12-7, add end*/
                   //no need to save, just finish and return;
                   finish();
                   return;
                }
             }
          }
          int result = saveEditingNote();
        	if (result==SAVEEDITNOTE_RETURN_CREATE_SUCCESS ||
        			result==SAVEEDITNOTE_RETURN_UPDATE_SUCCESS) {
				Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT)
						.show();
			}
       }
       /*2012-12-7, add by amt_sunzhao for SWITCHUITWO-156 */
       _cleanmFileUri();
		/*2012-12-7, add end*/

       finish();
    }



    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        QNDev.log(TAG + " dispatchTouchEvent()");
        mScrollingViews = false;
/*
        //to detect move action for zooming
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mTouchStartPointX = (int) ev.getX();
            mTouchStartPointY = (int) ev.getY();
        }
        if ((ev.getAction() == MotionEvent.ACTION_MOVE) && (mBottomBar.getVisibility() != View.VISIBLE)) {
           int x = (int) ev.getX();
           int y = (int) ev.getY();
           if (mTouchStartPointX != x || mTouchStartPointY != y) {
               if (mZoomButtonsController != null)  {
                   mZoomButtonsController.setVisible(true);
               }   
           } 
        } 
 */    

        boolean superResult = super.dispatchTouchEvent(ev);

        //keep the code in case we may support seekbar draging in future
        
        if (mMimeType.startsWith("audio/") && mQNContent.mSeeking) {
             return superResult;
        }
        

        if (null != mSlideshowGestureDetector) {
            mSlideshowGestureDetector.onTouchEvent(ev);
        }
   
        return superResult;
    }



    /**
     * Given an alarm in hours,minutes,day,month and year, return a time suitable for
     * setting in AlarmManager.
     */
    private Calendar calculateAlarm(int year, int month, int day, int hour, int minute) {

        Calendar c = Calendar.getInstance();
       // c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c;
    }


/*
    private boolean checkSD() {
        if (mMimeType.startsWith("text/") && QNDev.STORE_TEXT_NOTE_ON_SDCARD) {
            if (!QNUtil.checkStorageCard(this)) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
*/

    synchronized private boolean isMediaContent(){
        if(mContentUri != null && mMimeType != null 
            && mContentUri.toString().startsWith(QuickNotesDB.CONTENT_URI.toString())){
            if(mMimeType.startsWith("image/") || mMimeType.startsWith("audio/")){
                return true;
            }
        }

        return false;
    }

    synchronized private boolean isTextContent(){
        if(mContentUri != null && mMimeType != null 
            && mContentUri.toString().startsWith(QuickNotesDB.CONTENT_URI.toString())){
            if(mMimeType.startsWith("text/")){
                return true;
            }
        }

        return false;
    }

    private Intent intentTextNote(Uri qnUri) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, mQNContent.getTextNoteContent());
        return i;
    }

    private Intent intentMedianote(Uri qnUri) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(mValues.getAsString(QNColumn.MIMETYPE.column()));
        Uri uri = QNUtil.buildUri(mValues.getAsString(QNColumn.URI.column()));
        if (uri == null) {
            return i;
        }
        Uri muri = QNUtil.mediaStore_uri(this, uri.getPath());
        /*2012-12-13, add by amt_sunzhao for SWITCHUITWO-283 */
        if(null == muri) {
        	muri = uri;
        }
        /*2012-12-13, add end*/
        i.putExtra(Intent.EXTRA_STREAM, muri);         
        return i;
    }

    
    private void resetViews() {
      //change all views' state to initial state and no content show for any type
      mImageDetail.setVisibility(View.GONE);
      mThumbnail.setVisibility(View.VISIBLE);
      mAdd.setVisibility(View.VISIBLE);
      mShare.setVisibility(View.VISIBLE);
      mTrash.setVisibility(View.VISIBLE);
      mSetReminder.setVisibility(View.VISIBLE);
      mEditOK.setVisibility(View.GONE);
      mEditCancel.setVisibility(View.GONE);
      mImageContentLayout.setVisibility(View.GONE);
      mEditTitleScroll.setVisibility(View.GONE);
      mDetailTitleScroll.setVisibility(View.VISIBLE);
      mTitlePart.setBackgroundResource(0);
      mIndexCount.setVisibility(View.VISIBLE);
      mVoiceContent.setVisibility(View.GONE);
      mPlayPause.setImageResource(R.drawable.btn_playback_ic_play_small);
      mImageEditBtn.setVisibility(View.GONE);
      mReminderIndicator.setVisibility(View.GONE);
      mCallInfo.setVisibility(View.GONE);
      mBackgroundBtns.setVisibility(View.GONE);
      mEditTextScroll.setVisibility(View.GONE);
      mDetailTextScroll.setVisibility(View.VISIBLE);
      mTextContainer.setVisibility(View.GONE);
      mErrorLayout.setVisibility(View.GONE);
      mShare.setClickable(true);
      mShare.setImageResource(R.drawable.note_detail_share_btn);
      //release the resouces
      mEditTitle.setText("");
      mDetailTitle.setText("");
      mEditTextContent.setText("");
      mDetailTextContent.setText("");
      mImageContent.setImageResource(0);
      mImageDetail.setImageResource(0);
      //let the scroll view at the top
      mDetailTitleScroll.fullScroll(ScrollView.FOCUS_UP);
      mEditTitleScroll.fullScroll(ScrollView.FOCUS_UP);
      mDetailTextScroll.fullScroll(ScrollView.FOCUS_UP);
      mEditTextScroll.fullScroll(ScrollView.FOCUS_UP);
    }

    private void setupNoteView() {
        QNDev.log(TAG + "setupNoteView()");
        //activiy is visiable again; set it here again otherwise the detailview activity will be finish after destroyQNContent() if it is a paused voice note
        mActivityVisiable = true;

        mCount = QuickNotesDB.getSize(this);

        //get the mContentUri again in setupView, because when the user scrolls the screen, it use index to get the uri in setupView() 

        destroyQNContent(mQNContent);
        mQNContent = prepareQNContent(mMimeType, mFileUri);
        if (null == mQNContent) {
            finish();
            return;
        }

        //reset the views
        resetViews();
        //set the note content
        mNoteView = mQNContent.noteView(QNNoteView.this, mViewGroup);

		/*2012-9-18, add by amt_sunzhao for SWITCHUITWOV-194 */
        if(mIsImageDetailMode) {
        	mIsImageDetailMode = false;
        	mViewGroup.setBackgroundResource(R.drawable.bg_board_softwood);
            mTopBar.setVisibility(View.VISIBLE);
            mContentPart.setVisibility(View.VISIBLE);
        }
		/*2012-9-18, add end*/ 
        
        //set the views' visibility
        if (mMimeType == null) {
           finish();
           return;
        } else if (mMimeType.startsWith("text/")) {
            mTextContainer.setVisibility(View.VISIBLE);
            mImageContentLayout.setVisibility(View.GONE);
            mImageEditBtn.setVisibility(View.GONE);
            mImageDetail.setVisibility(View.GONE);
            mVoiceContent.setVisibility(View.GONE);
        } else if(mMimeType.startsWith("image/")) {
            mContentPart.setBackgroundResource(R.drawable.bg_text_note_paper_single);
            mTextContainer.setVisibility(View.GONE);
            mBackgroundBtns.setVisibility(View.GONE);
            mImageContentLayout.setVisibility(View.VISIBLE);
            mImageEditBtn.setVisibility(View.VISIBLE);
            mImageDetail.setVisibility(View.GONE);
            mVoiceContent.setVisibility(View.GONE);
        } else if (mMimeType.startsWith("audio/")) {
            mTextContainer.setVisibility(View.GONE);
            mImageContentLayout.setVisibility(View.GONE);
            mImageEditBtn.setVisibility(View.GONE);
            mImageDetail.setVisibility(View.GONE);
            mVoiceContent.setVisibility(View.VISIBLE);
        }

        if (mNoteView == null) {
            QNDev.log(TAG+"Noteview is null, so will show the error info.");
            mErrorLayout.setVisibility(View.VISIBLE);
            mTextContainer.setVisibility(View.GONE);
            mImageContentLayout.setVisibility(View.GONE);
            mImageEditBtn.setVisibility(View.GONE);
            mImageDetail.setVisibility(View.GONE);
            mVoiceContent.setVisibility(View.GONE);
            mShare.setClickable(false);
            mShare.setImageResource(R.drawable.ic_share_disabled);
        } 

        //for title
        String title = (String)QuickNotesDB.read(this, mContentUri, QNColumn.TITLE);
        if(title != null && title.length() > 0) {
           mDetailTitle.setText(title);
        } else {
           mDetailTitle.setText(null);
        }

        if ( currentRunMode == _RunningModeE.DETAIL) {
           //detail mode
           toDetailMode();
        } else {
          //edit mode
          if (mMimeType.startsWith("text/")) {
            mEditTextContent.requestFocus();
            toEditMode();
          } else {
            mEditTitle.requestFocus();
            toEditMode();
          }
        }
    }


    private void destroyQNContent(QNContent qnc) {
        if(null != qnc) {
            if(NotestateE.ERROR != qnc.state() && NotestateE.IDLE != qnc.state()) { qnc.stop(); }
            qnc.close();
        }
    }

 
    private QNContent prepareQNContent(String mimetype, Uri qnuri) {
        QNContent qnc = QNContent_Factory.Create(mimetype, qnuri);
        if (qnc == null) { return null;}
        QNDev.qnAssert(null != qnc);

        // Setup with new uri contents before get detail view.
        qnc.setup();


        // We should register listener of new instance when it is a voice note..
     if (qnc.isPlayable()) {
        ((QNContent_Snd)qnc).setContext(this);
        qnc.register_OnStateListener(new QNContent.OnStateListener() {
            public void OnState(NotestateE from, NotestateE to, boolean pausedByIncomingCall) {
                    if ( !pausedByIncomingCall &&
                        (NotestateE.IDLE == to || NotestateE.PAUSED == to)) {
                       //cancel the notification
                       NotificationManager nm = (NotificationManager) getSystemService(QNNoteView.NOTIFICATION_SERVICE);
                       nm.cancel(mVoice);
                      if( NotestateE.IDLE == to ) {
                        if (!mActivityVisiable) {
                            //QNDetailview is invisiable and the soundRecorder is finsihed, so finish this activity
                            finish();
                        } 
                      } 

                       return;
                    } 
            }        
        });
     }
        return qnc;
    }


    /**  detail mode, show alarm/trash/share/add btn, EditText areas is not permitted to edit
         InputMethod is hide
    **/
    private void toDetailMode() {
        mOldContentUri = null;
        mSetReminder.setVisibility(View.VISIBLE);
        mTrash.setVisibility(View.VISIBLE);
        mShare.setVisibility(View.VISIBLE);
        mAdd.setVisibility(View.VISIBLE);
        mEditOK.setVisibility(View.GONE);
        mEditCancel.setVisibility(View.GONE);
        mDetailTitleScroll.setVisibility(View.VISIBLE);
        mEditTitleScroll.setVisibility(View.GONE);
        mDetailTextScroll.setVisibility(View.VISIBLE);
        mEditTextScroll.setVisibility(View.GONE);
        mBackgroundBtns.setVisibility(View.GONE);
        mIndexCount.setVisibility(View.VISIBLE);
        hideSoftInput();

        // set the reminder icon if need
        if (0L != mDBReminder) {
            mReminderIndicator.setVisibility(View.VISIBLE);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(mDBReminder);
            QNSetReminder.setReminderFlag(this, mRemindertime, c);
            mSetReminder.setImageResource(R.drawable.note_detail_reminder_set_btn);
        } else {
            if (mMimeType.startsWith("audio/")) { 
              mReminderIndicator.setVisibility(View.INVISIBLE);
            } else {
              mReminderIndicator.setVisibility(View.GONE);
            }
            mSetReminder.setImageResource(R.drawable.note_detail_reminder_btn);
        }


        if (mMimeType.startsWith("audio/")) { 
           //set the background
           mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "view", "audio"));
           mVoiceIcon.setImageResource(_BackgroundRes.find_drawable(mBgColor, "view", "voice_icon"));
           mEditVoiceDuration.setVisibility(View.GONE);
           mDetailVoiceLayout.setVisibility(View.VISIBLE);
        } else if  (mMimeType.startsWith("image/")) {
           //image view is clickable
           mImageContentLayout.setClickable(true);
        } else if (mMimeType.startsWith("text/")) {
           //set the background
           mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "view", "text"));
        }

        if (mNoteView == null) {
            //use the yellow bg to the background to keep consistent with widget and thumbnails
            mContentPart.setBackgroundResource(R.drawable.bg_text_note_gridme_single);
        }


        // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
        //in-call info in detailview
        String[] columns_excluded = new String[4];
        columns_excluded[0] =  QNColumn._ID.column();
        columns_excluded[1] =  QNColumn.WIDGETID.column();
        columns_excluded[2] =  QNColumn.SPANX.column();
        columns_excluded[3] =  QNColumn.SPANY.column();
        mValues = QuickNotesDB.copy_values(this, mContentUri, columns_excluded);
        if (null == mValues) {
            Toast.makeText(QNNoteView.this, R.string.unexpected_failure, Toast.LENGTH_SHORT);
            return;
        }
        getCommonValues();

        String callNumber = mValues.getAsString(QNColumn.CALLNUMBER.column());
        Long callDateEnd = mValues.getAsLong(QNColumn.CALLDATEEND.column());
        int callType = mValues.getAsInteger(QNColumn.CALLTYPE.column());

        if (mMimeType.startsWith("text/") && (callNumber != null) && (callNumber.length() != 0) && (callDateEnd != 0)) {
            String callDate = DateUtils.formatDateRange(this, callDateEnd, callDateEnd, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_YEAR);
            String callName = null;

            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(callNumber));
            String[] projection = new String[] { PhoneLookup.DISPLAY_NAME };
            Cursor c = getContentResolver().query(uri, projection, null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        callName = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));
                    } else {
                        callName = getString(R.string.contact_unknown);
                    }
                } finally {
                    c.close();
                }
            }

            switch (callType) {
                case 1:
                    mCallInfoType.setImageResource(R.drawable.ic_call_incoming_holo_dark);
                    break;
                case 2:
                    mCallInfoType.setImageResource(R.drawable.ic_call_outgoing_holo_dark);
                    break;
            }
            mCallInfoName.setText(callName);
            mCallInfoNumber.setText(callNumber);
            mCallInfoDate.setText(callDate);
            mCallInfo.setVisibility(View.VISIBLE);
        }
        // END IKCNDEVICS-751

        // index/count on top bar
        String indexAndCount = (mIndex + 1) + "/" + mCount;
        mIndexCount.setText(indexAndCount);

        //send out the intent to inform widget update if need
        QNDev.logd(TAG, "viewed QN index is " + mIndex);
        Intent i = new Intent(QNConstants.INTENT_ACTION_NOTE_VIEWED);
        i.putExtra(QNConstants.GRID_ID, mIndex);
        i.putExtra(QNConstants.NOTE_URI, mContentUri.toString());
        sendBroadcast(i);
    }

     //hide the softinput 
     private void hideSoftInput() {
        // hide the virtual keyboard
       try {
        final View v = getWindow().peekDecorView();
        if (v != null && v.getWindowToken() != null) {
           InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
           if (!imm.hideSoftInputFromWindow(v.getWindowToken(), 0)) {
              QNDev.log(TAG+" Error: hide softinputfromwindow failed!");
           }
        }
       } catch (Exception e){
           QNDev.log(TAG+" failed to hide the softInputMethod: error = "+e);
       }
    }


    /** edit mode, show OK and Cancel btn, editText areas is permitted to edit
       InputMethod is shown
    **/
    private void toEditMode() {
        mReminderIndicator.setVisibility(View.GONE);
        mSetReminder.setVisibility(View.GONE);
        mTrash.setVisibility(View.GONE);
        mShare.setVisibility(View.GONE);
        mAdd.setVisibility(View.GONE);
        mEditOK.setVisibility(View.VISIBLE);
        mEditCancel.setVisibility(View.VISIBLE);
        mDetailTitleScroll.setVisibility(View.GONE);
        mEditTitleScroll.setVisibility(View.VISIBLE);
        mDetailTextScroll.setVisibility(View.GONE);
        mEditTextScroll.setVisibility(View.VISIBLE);
        mIndexCount.setVisibility(View.INVISIBLE);
        // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
        mCallInfo.setVisibility(View.GONE);
        if (mFromOtherApp) {
            //it is from other app, no need to show thum btn
            mThumbnail.setVisibility(View.GONE);
        }
        // END IKCNDEVICS-751


        if (mMimeType.startsWith("audio/")) { 
           //set the background
           mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "edit", "audio"));
           mVoiceIcon.setImageResource(_BackgroundRes.find_drawable(mBgColor, "edit", "voice_icon"));
           mEditVoiceDuration.setVisibility(View.VISIBLE);
           mDetailVoiceLayout.setVisibility(View.GONE);
           mBackgroundBtns.setVisibility(View.VISIBLE);
        } else  if (mMimeType.startsWith("image/")) {
           //image view is not clickable
           mImageContentLayout.setClickable(false);
        } else if (mMimeType.startsWith("text/")) {
           mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "edit", "text"));
           mBackgroundBtns.setVisibility(View.VISIBLE);
        }

        if (mNoteView == null) {
            //use the yellow bg to the background to keep consistent with widget and thumbnails
            mContentPart.setBackgroundResource(R.drawable.bg_text_note_gridme_single);
        }
    }


    private static int SAVEEDITNOTE_RETURN_CREATE_SUCCESS = 1;
    private static int SAVEEDITNOTE_RETURN_CREATE_FAIL = 2;
    private static int SAVEEDITNOTE_RETURN_UPDATE_SUCCESS = 3;
    private static int SAVEEDITNOTE_RETURN_NO_UPDATE = 4;
    private static int SAVEEDITNOTE_RETURN_EXCEPTION = 5;

	private int saveEditingNote() {
		int ret = SAVEEDITNOTE_RETURN_CREATE_FAIL;

		String title = mEditTitle.getText().toString();
		if ((title != null) && (title.length() != 0) && !title.endsWith(" ")) {
			title += " ";
			// update the title view
			mEditTitle.setText(title);
		}
		// save the title
		mDetailTitle.setText(title);
		// save the text if need
		if (mMimeType.startsWith("text/")) {
			// update the detail text note
			String newContent = mEditTextContent.getText().toString();
			if ((newContent != null) && (newContent.length() != 0)
					&& !newContent.endsWith(" ")) {
				// add a space at the end of the text content
				// in case the end part is number or link or email
				newContent += " ";
				// update the text view
				mEditTextContent.setText(newContent);
			}
			mDetailTextContent.setText(newContent);
		}
		if (_RunningModeE.EDIT_NEW == currentRunMode
				|| _RunningModeE.EDIT_ATTACH == currentRunMode) {
			if (!QNUtil.checkStorageCard(QNNoteView.this)) {
				return SAVEEDITNOTE_RETURN_CREATE_FAIL;
			}
			try {
				 if(_create_note()){
					 return SAVEEDITNOTE_RETURN_CREATE_SUCCESS;	 
				 }else{
					 return SAVEEDITNOTE_RETURN_CREATE_FAIL;	
				 }
			} catch (Exception e) {
				Toast.makeText(QNNoteView.this, R.string.load_failed,
						Toast.LENGTH_SHORT);
				return SAVEEDITNOTE_RETURN_EXCEPTION;
			}
		} else if (_RunningModeE.EDIT_EDIT == currentRunMode) {
			if (!QNUtil.checkStorageCard(QNNoteView.this)) {
				return SAVEEDITNOTE_RETURN_NO_UPDATE;
			}
			try {
				if(_update_note()){
				 return  SAVEEDITNOTE_RETURN_UPDATE_SUCCESS;	
				}else{
				 return  SAVEEDITNOTE_RETURN_NO_UPDATE;
				}
			} catch (Exception e) {
				Toast.makeText(QNNoteView.this, R.string.load_failed,
						Toast.LENGTH_SHORT);
				return SAVEEDITNOTE_RETURN_EXCEPTION;
			}
		}

		return ret;
	}

    private void registerIntentReceivers() {
        IntentFilter qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED);
        registerReceiver(mQuickNotesReceiver, qnFilter);
        qnFilter = new IntentFilter(QNConstants.INTENT_ACTION_REMINDER_TIME_OUT);
        registerReceiver(mQuickNotesReceiver, qnFilter);
    }


    private void resetCommonValues() {
         mMimeType = null;
         mIndex = 0;
         mDBReminder = 0L;
         mFileUri = null;
         mBgColor = R.color.yellow; // this is default
    }


    private void getCommonValues() {
       resetCommonValues();
       if ( null != mValues) {
         mMimeType = mValues.getAsString(QNColumn.MIMETYPE.column());
         mIndex = QuickNotesDB.getIndexByUri(QNNoteView.this, mContentUri);
         mDBReminder = mValues.getAsLong(QNColumn.REMINDER.column());
         mFileUri = QNUtil.buildUri(mValues.getAsString(QNColumn.URI.column()));
         mBgColor = mValues.getAsInteger(QNColumn.BGCOLOR.column());
       }
    }

    private ContentValues getDBData() {
        ContentValues values = null;
        if ( null != mContentUri ) {
           String[] columns_excluded = new String[4];
           columns_excluded[0] =  QNColumn._ID.column();
           columns_excluded[1] =  QNColumn.WIDGETID.column();
           columns_excluded[2] =  QNColumn.SPANX.column();
           columns_excluded[3] =  QNColumn.SPANY.column();
           values = QuickNotesDB.copy_values(this, mContentUri, columns_excluded);
        }

        return values;
    }


    private void gotoPrev() {
        if (mIndex > 0) {
            mIndex --;
        } else {
            mIndex = mCount -1;
        }
        setContentUri(QuickNotesDB.getItem(QNNoteView.this, mIndex));
        if (null == mContentUri) {
            finish();
            return;
        }
        mValues = getDBData();
        if (null == mValues) {
            Toast.makeText(QNNoteView.this, R.string.unexpected_failure, Toast.LENGTH_SHORT);
            finish();
            return;
        }
        getCommonValues();
        setupNoteView();
     }

    private void gotoNext() {
        if (mIndex < mCount - 1) {
            mIndex ++;
        } else {
            mIndex = 0;
        }
        setContentUri(QuickNotesDB.getItem(QNNoteView.this, mIndex));
        if (null == mContentUri) {
            finish();
            return;
        }
        mValues = getDBData();
        if (null == mValues) {
            Toast.makeText(QNNoteView.this, R.string.unexpected_failure, Toast.LENGTH_SHORT);
            finish();
            return;
        }
        getCommonValues();
        setupNoteView();
    }


    // Inner class
    private class SlideGestureListener implements
            GestureDetector.OnGestureListener {
        public boolean onDown(MotionEvent e) {
            mDirection = INVALID;
            mOnMoving = false;
            return false;
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float deltaX,
                float deltaY) {
            mScrollingViews = true;
            boolean ret = false;
            int distanceX = (int) e1.getX() - (int) e2.getX();
            int distanceY = (int) e1.getY() - (int) e2.getY();
            int absDistanceX = Math.abs(distanceX);
            int absDistanceY = Math.abs(distanceY);
            QNDev.log(TAG + " onScroll, distanceX=" + distanceX + ", distanceY="
                    + distanceY);

            int currentDirection = INVALID;
            if (absDistanceX > GESTURE_MIN_DISTANCE) {
                currentDirection = distanceX > 0 ? NEXT : PREV;
                // check the slope of gesture movement. if x distance is smaller
                // than y distance times GESTURE_SLOPE_THRESHOD, the slope is
                // too
                // steep, and the scroll may not mean to switch slide.
                if (absDistanceX < GESTURE_SLOPE_THRESHOLD * absDistanceY) {
                    currentDirection = INVALID;
                    QNDev.log(TAG + " Gesture slope is steep, currentDirection: "
                            + currentDirection);
                }

                if (currentDirection != INVALID) {
                    if (currentDirection != mDirection) {
                        QNDev.log(TAG +" Do slide switch by scroll, "
                                + "currentDirection: " + currentDirection);
                        switch (currentDirection) {
                            case NEXT:
                                if ((currentRunMode == _RunningModeE.DETAIL) && (mImageDetail.getVisibility() == View.GONE)) {
                                  gotoNext();
                                  ret = true;
                                }
                                break;
                            case PREV:
                                if ((currentRunMode == _RunningModeE.DETAIL) && (mImageDetail.getVisibility() == View.GONE)) {
                                  gotoPrev();
                                  ret = true;
                                }
                                break;
                            default:
                                break;
                        }
                        mDirection = currentDirection;
                    }
                }
            }
            return ret;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
            mScrollingViews = true;
            boolean ret = false;
            QNDev.log(TAG + " onFling, velocityX=" + velocityX + ", velocityY="
                    + velocityY);

            if (Math.abs(velocityX) > GESTURE_SLOPE_THRESHOLD
                    * Math.abs(velocityY)) {
                int currentDirection = velocityX < 0 ? NEXT : PREV;
                if (currentDirection != mDirection) {
                    QNDev.log(TAG + " Do slide switch by Fling");
                    switch (currentDirection) {
                        case NEXT:
                            if ((currentRunMode == _RunningModeE.DETAIL) && (mImageDetail.getVisibility() == View.GONE)) {
                               gotoNext();
                               ret = true;
                            }
                            break;
                        case PREV:
                            if ((currentRunMode == _RunningModeE.DETAIL) && (mImageDetail.getVisibility() == View.GONE)) {
                               gotoPrev();
                               ret = true;
                            }
                            break;
                        default:
                            break;
                    }
                    mDirection = currentDirection;
                }
            }
            return ret;
        }

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        public void onLongPress(MotionEvent e) {
            return;
        }

        public void onShowPress(MotionEvent e) {
            return;
        }
    }


    private Uri checkStoragePrefix(String ori_path) {
        Uri fileUri;

        /*2012-12-14, add by amt_sunzhao for SWITCHUITWO-317 */
        final String MNT_SDCARD = "/mnt/sdcard";
        final String FILE_URI = "file://";
        if(ori_path == null 
        		|| !ori_path.startsWith(MNT_SDCARD)) {
        	return Uri.parse(FILE_URI + ori_path);
        }
        /*2012-12-14, add end*/
        
        //check the storage prefix
        //path format:/mnt/sdcard/sketch/filename.png
        int pos = ori_path.indexOf("/",5);
        String currentPref = ori_path.substring(0, pos);
        String targetPref = QNUtil.getInternalStoragePath(this);
        if (targetPref.equals(currentPref)) {
           //match! 
           fileUri = Uri.parse("file://" +ori_path);
           return fileUri;
        } else {
           //not match! need to copy the voice file from SD card to internal Memory"
           //try to copy this image to quicknote/image folder
           if (!QNUtil.checkStorageCard(QNNoteView.this)) {
              finish();
              return null;
           }
           QNUtil.initDirs(this);
           String newFileName = ori_path.replaceFirst(currentPref, targetPref);
           //make sure voice dir existed
           int pos_2 = newFileName.lastIndexOf("/");
           String newFileDir = newFileName.substring(0, pos_2 + 1);
           File toDir = new File(newFileDir);
           if(!toDir.exists()) {
              //mkdir if the directory does not existe
              toDir.mkdirs();
           }
           File newFile = new File(newFileName);
           int i = 0;
           String reNamedFileName = null;
           while (newFile.exists()) {
              //rename it
              i++;
              pos = newFileName.indexOf(".");
              String part_1 = newFileName.substring(0,pos);
              String part_2 = newFileName.substring(pos);
              reNamedFileName = part_1+"_"+i+part_2;
              newFile = new File(reNamedFileName);
            }
            if(null != reNamedFileName) {
               newFileName = reNamedFileName;
            }
            File oldFile = new File(ori_path);
            QNDev.log(TAG+" newFile = "+newFile+" oldFile = "+oldFile);

            if (oldFile.exists() && QNUtil.copyfile(oldFile, newFile)) {
              QNDev.log(TAG+"file copied successfully");
              fileUri = Uri.parse("file://" + newFileName);
              //inform media to scan since this voice file is copied by QN instead of created by soundRecorder
              QNUtil.mediaScanFolderOrFile(QNNoteView.this, newFile, false);
              return fileUri;
            } else {
              QNDev.log(TAG+"Error found! old file not exists or copy failed. Return;");
              Toast.makeText(this, R.string.fail_to_read_file_toast, Toast.LENGTH_SHORT).show();
              finish();
              return null;
            } 
        }
    }
    
    private void validateViewState(){    	
    	 if (mMimeType.startsWith("audio/")) { 
    		 if(null != mQNContent) {
    			 if(NotestateE.ERROR != mQNContent.state()) { 
    				 ((QNContent_Snd)mQNContent).validateViewState();
    				 }
    			 }
    	 	}
    	 }

    // Inner class
    private class QuickNotesIntentReceiver extends BroadcastReceiver {
        private static final String TAG = "[QNNoteView:QuickNotesIntentReceiver]";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action  = intent.getAction();

            if (action == null) { return; }
            if ( currentRunMode != _RunningModeE.DETAIL) {
               //only for detail mode
               return;
            }

            QNDev.log(TAG + " intent action : " + action);

            if (action.equals(QNConstants.INTENT_ACTION_MEDIA_MOUNTED_UNMOUNTED) )  {
                if(null != mQNContent && mQNContent.isPlayable()) {
                    //cancel the notification
                    NotificationManager nm = (NotificationManager) getSystemService(QNNoteView.NOTIFICATION_SERVICE);
                    nm.cancel(mVoice);
                }
                setupNoteView();
                return;
            } else if (action.equals(QNConstants.INTENT_ACTION_REMINDER_TIME_OUT)){
               String uri_string = intent.getStringExtra("qnUri");
               if ((null != uri_string) && (uri_string.equals(getContentUriString()))) {
                  mDBReminder = 0L;
                  mValues.put(QNColumn.REMINDER.column(), mDBReminder);
                  if (mMimeType.startsWith("audio/")) {
                     mReminderIndicator.setVisibility(View.INVISIBLE);
                  } else {
                     mReminderIndicator.setVisibility(View.GONE);
                  }
                  //update the view
                  mViewGroup.refreshDrawableState();
                  //update the reminder icon in top bar
                  mSetReminder.setImageResource(R.drawable.note_detail_reminder_btn);
               }
               return;
            }
    }
  }

    private  class CopyPasteCallback implements ActionMode.Callback {
        @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mTopBar.setVisibility(View.GONE);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mTopBar.setVisibility(View.VISIBLE);
            }
    }


    private class ImageProcessHandler extends Handler {
       private Handler mImageProcessUiHandler;

       public ImageProcessHandler(Looper looper, Handler imageProcessUiHandler) {
           super(looper);
           this.mImageProcessUiHandler = imageProcessUiHandler;
       }

       @Override
       public void handleMessage(Message msg) {
         Uri content_uri = null;

         try {
            switch (msg.what) {
              case MSG_IMAGE_PROCESS_START :
            	  QNDev.log("MSG_IMAGE_PROCESS_START E.");
                   //try to copy this image to quicknote/image folder
                   if (!QNUtil.checkStorageCard(QNNoteView.this)) {
                      finish();
                      QNDev.log("!QNUtil.checkStorageCard(QNNoteView.this)");
                      return;
                   }
                   QNUtil.initDirs(QNNoteView.this);
                  //check the exif info. to see whether the png from camere need to rotate or not
                  //original file should be kept in case other app will need it
                  Uri ori_uri = Uri.parse("file://" + ori_path);
                  File oldFile = new File(ori_path);
                  if (!oldFile.exists()) {
                      //file from camera is not exist, so return;
                      Toast.makeText(QNNoteView.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
                      QNDev.log(TAG+" handleMessage found image process error for old file is not exist");
                      finish();
                      return;
                  }

					/*2012-10-18, add by amt_sunzhao for SWITCHUITWOV-270 */
					// getDegreesRotated only can work well for jpeg
                  //int degrees = QNUtil.getDegreesRotated(ori_uri);
                  final int degrees = msg.arg1;
					/*2012-10-18, add end*/
                  if (degrees != 0) {
                      //need rotate the picture
                      Bitmap bm = QNUtil.decode_image_file(ori_path, getResources().getInteger(R.integer.detail_image_width), getResources().getInteger(R.integer.detail_image_height), true);  //BitmapFactory.decodeFile(ori_path);
                       Bitmap newbm = QNUtil.rotate(bm, degrees);
                       if (newbm == null) {
                         //something wrong when rotating the image
                         Toast.makeText(QNNoteView.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
                         QNDev.log(TAG+" handleMessage found image process error for newBm = null"); 
                         finish();
                         return;
                      }
                      String newFileName = QNUtil.createNewImageName();
                      File newFile = new File(newFileName);
                      QNUtil.save_image(newFile, newbm, CompressFormat.JPEG);
                      bm.recycle();
                      mFileUri = Uri.parse("file://" + newFileName);
                      QNDev.logd(TAG, "file rotated successfully and file is rotated too:  mFileUri =" + mFileUri.toString() + " mimeType=" + mMimeType);
                  } else {
                      //just copy the old file to new file name
                      String newFileName = QNUtil.createNewImageName();
                      File newFile = new File(newFileName);
                      QNDev.log(TAG+"newFile = "+newFile+" oldFile = "+oldFile);

                      if (oldFile.exists() && QNUtil.copyfile(oldFile, newFile)) {
                         QNDev.log(TAG+"file is renamed successfully");
                         mFileUri = Uri.parse("file://" + newFileName);
                         QNDev.logd(TAG, "file copied successfully: mFileUri =" + mFileUri.toString() + " mimeType=" + mMimeType);
                      } else {
                         QNDev.log(TAG+"Error found! old file not exists or copy failed. Return;");

                         Toast.makeText(QNNoteView.this, R.string.fail_to_read_file_toast, Toast.LENGTH_SHORT).show();
                         finish();
                         return;
                      }
                 }
                  QNDev.log("MSG_IMAGE_PROCESS_START X.");
                 mImageProcessUiHandler.sendMessage(mImageProcessUiHandler.obtainMessage(MSG_IMAGE_PROCESS_COMPLETE));
                 break;
              default:
                 mImageProcessUiHandler.sendMessage(mImageProcessUiHandler.obtainMessage(MSG_DISMISS_PROGRESS));
                 break;
           }
        } catch (Exception ex) {
           Toast.makeText(QNNoteView.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
           QNDev.log(TAG+" handleMessage found image process exception = "+ex);
           finish();
           return;
        } catch (java.lang.OutOfMemoryError e) {
           Toast.makeText(QNNoteView.this, R.string.image_process_failed, Toast.LENGTH_LONG).show();
           QNDev.log(TAG+" handleMessage found image process error for out of memory error = "+e);
           finish();
           return;
        }
     }
  }

    synchronized private void setContentUri(Uri uri) {
        mContentUri = uri;
    }
    
    synchronized private String getContentUriString() {
        if(mContentUri != null)
            return mContentUri.toString();
        else
            return null;
    }
}
