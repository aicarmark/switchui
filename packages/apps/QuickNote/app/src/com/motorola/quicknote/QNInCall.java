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
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface;
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
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.text.format.DateUtils;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
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

public class QNInCall extends QNActivity implements View.OnClickListener
{
    /******************************
     * Constants
     ******************************/

    static private String TAG = "QNInCall";

    private enum _RunningModeE {
        EDIT_NEW,
        EDIT_EDIT
    }


    /******************************
     * members
     ******************************/
    private QNContent            mQNContent         = null;    // quick note contents
    // "mContentUri is not null" means this is existing note!
    private Uri                  mContentUri        = null;  //current note's conent uri : content://com.motorola.provider.quicknote/qnotes/4
    private Uri                  mFileUri           = null;  //current note's file uir :file:///mnt/sdcard/quicknote/text/2012-02-06_05-00-49_932.txt
    private String               mMimeType         = null;
    private _RunningModeE        _runmode           = null;
    private int                  mBgColor           = _BackgroundRes.GRIDME.column(); // this is default
    private ImageView            mThumbnail         = null;
    private ImageView            mSetReminder       = null;
    private ImageView            mTrash             = null;
    private ImageView            mShare             = null;
    private ImageView            mAdd               = null;
    private ImageView            mEditOK            = null;
    private ImageView            mEditCancel        = null;
    private TextView             mIndexCount        = null;
    private EditText             mEditTitle         = null;
    private TextView             mDetailTitle       = null;
    private EditText             mEditTextContent   = null;
    private ScrollView           mDetailTitleScroll = null;
    private ScrollView           mEditTitleScroll   = null;
    private RelativeLayout       mTextContainer     = null;
    private ScrollView           mDetailTextScroll  = null;
    private ScrollView           mEditTextScroll    = null;
    private RelativeLayout       mContentPart       = null;
    private LinearLayout         mBackgroundBtns    = null;
    private ImageView            mBackgroundBtn_1   = null;
    private ImageView            mBackgroundBtn_2   = null;
    private ImageView            mBackgroundBtn_3   = null;
    private ImageView            mBackgroundBtn_4   = null;
    private RelativeLayout       mErrorLayout       = null;

    private ViewGroup mViewGroup;
    private View                 mNoteView          = null;
    private ContentValues mValues = null;

    private View mReminderIndicator = null;
    // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
    private String mCallNumber = null;
    private long mCallDateBegin = 0;
    private long mCallDateEnd = 0;
    int mCallType = 0;
    private boolean mInCall = false; //create in-call note
    // END IKCNDEVICS-751

    // Start from Launcher (to create an note)

    /******************************
     * Types
     ******************************/

    /**************************
     * Local Functions
     **************************/
    private void _create_note()
    {
        QNDev.log(TAG + ": _create_note()");
        if (null == mQNContent) {return;}

        String title = mQNContent.isTitle()? mEditTitle.getText().toString(): "";

        mQNContent.trigger_update();

        ContentValues contentValues = new ContentValues();
        contentValues.put(QNColumn.TITLE.column(), title);
        contentValues.put(QNColumn.MIMETYPE.column(), mMimeType);
        contentValues.put(QNColumn.BGCOLOR.column(), mBgColor);
        // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
        if (mInCall && mMimeType.startsWith("text/")) {
            String content = mEditTextContent.getText().toString();
            String summary = content.length()>40? content.substring(0, 40): content;
            contentValues.put(QNColumn.SUMMARY.column(), summary);
            contentValues.put(QNColumn.CALLNUMBER.column(), mCallNumber);
            contentValues.put(QNColumn.CALLDATEBEGIN.column(), mCallDateBegin);
            contentValues.put(QNColumn.CALLTYPE.column(), mCallType);
        }
        // END IKCNDEVICS-751

        if (mFileUri == null) {
             QNDev.logi(TAG, "_create_note failed, mFileUri is null");
             return;
        }
        contentValues.put(QNColumn.URI.column(), mFileUri.toString());

        mContentUri = QuickNotesDB.insert(this, contentValues, true);
        mValues = getDBData();
        getCommonValues();

        // notify to provider that new note (with 'uri') is generated.
        // Provider should map uri with app widget id.
        if (mContentUri != null) {
           Intent i = new Intent(QNConstants.INTENT_ACTION_NOTE_CREATED);
           i.putExtra(QNConstants.NOTE_URI, mContentUri.toString());
           i.putExtra(QNConstants.GRID_ID, 0);
           sendBroadcast(i);
         }
    }

    private void _update_note() {
        QNDev.log(TAG + ": _update_note()");
        if ((null == mContentUri) || !QuickNotesDB.is_qnuri_type(mContentUri) || (null == mQNContent)) { return;}
        mValues = getDBData();
        getCommonValues();

        boolean bUpdated = false;

        bUpdated = mQNContent.trigger_update(); // give a chance to update to _qnc

        if ( mQNContent.isTitle() ){
            String new_title = mEditTitle.getText().toString();
            if( !new_title.equals((String)QuickNotesDB.read(this, mContentUri, QNColumn.TITLE)) ) {
                //update the title
                mValues.put(QNColumn.TITLE.column(), new_title);
                QNDev.logd(TAG, ": _update_note(): update new_title:" + new_title);
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
                QNDev.logd(TAG, ": _update_note(): update new_summary:" + new_summary);
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
            mContentUri = QuickNotesDB.insert(this, mValues, true);
            if (mContentUri == null ){
                QNDev.log(TAG + " update note failed! return;");
                Toast.makeText(QNInCall.this, R.string.note_update_failed, Toast.LENGTH_SHORT);
                finish();
                return;
            }

            //delete the old item in DB
            QuickNotesDB.delete(QNInCall.this,  oldContentUri);

            //broadcast the intent
            Intent intentUpdate = new Intent(QNConstants.INTENT_ACTION_NOTE_UPDATED);
            intentUpdate.putExtra("old_content_uri", oldContentUri.toString());
            intentUpdate.putExtra(QNConstants.NOTE_URI, mContentUri.toString());
            intentUpdate.putExtra(QNConstants.GRID_ID, 0);
            sendBroadcast(intentUpdate);
        }
    }


    /**
     *
     * @param qnc
     * @param uri
     * @param title : if null, do nothing to Title Box.
     * @param bgcolor
     * @return
     */
    private void setupMainView() {
        QNDev.logd(TAG, " setupMainView()");
        mViewGroup = (ViewGroup)findViewById(R.id.main);
        mThumbnail = (ImageView)findViewById(R.id.thumbnail);
        mSetReminder = (ImageView)findViewById(R.id.set_reminder);
        mTrash = (ImageView)findViewById(R.id.trash);
        mShare = (ImageView)findViewById(R.id.share);
        mAdd = (ImageView)findViewById(R.id.add);
        //for edit top icons
        mEditOK = (ImageView)findViewById(R.id.edit_ok);
        mEditOK.setOnClickListener(this);
        mEditCancel = (ImageView)findViewById(R.id.edit_cancel);
        mEditCancel.setOnClickListener(this);
        //for title
        mIndexCount = ((TextView)findViewById(R.id.count));
        mEditTitle = ((EditText)findViewById(R.id.edit_title));
        mEditTitle.addTextChangedListener(new TextWatcher() {
          public void afterTextChanged(Editable s) {
            //in note.xml, we set the maxlength for title to 30 which will full-fill the window width when it is the smallest text size
            if (s.length() == 30 ) {
                 Toast.makeText(QNInCall.this, R.string.title_maxLength, Toast.LENGTH_LONG).show();
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
        mDetailTitleScroll = (ScrollView)findViewById(R.id.detail_title_scrolling);
        mEditTitleScroll = (ScrollView)findViewById(R.id.edit_title_scrolling);
        //for text content of text note
        mTextContainer = (RelativeLayout)findViewById(R.id.scrolling_text);
        mEditTextScroll = (ScrollView)findViewById(R.id.edit_scrolling_area);
        mEditTextContent = (EditText)findViewById(R.id.edit_text_content);
        mDetailTextScroll = (ScrollView)findViewById(R.id.detail_scrolling_area);

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
        mReminderIndicator = (View) findViewById(R.id.reminder);
        mErrorLayout = (RelativeLayout)findViewById(R.id.error);

        setupNoteView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_ok:
                // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
                saveEditingNote();
                finish();
                // END IKCNDEVICS-751
                return;

            case R.id.edit_cancel:
                // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
                finish();
                // END IKCNDEVICS-751
                return;

            case R.id.bg_button_1:
                if (mMimeType.startsWith("text/")) {
                    mBgColor = _BackgroundRes.GRIDME.column();
                    mContentPart.setBackgroundResource(_BackgroundRes.GRIDME.textEditResId());
                }
                return;

            case R.id.bg_button_2:
                if (mMimeType.startsWith("text/")) {
                    mBgColor = _BackgroundRes.PROJECT_PAPER.column();
                    mContentPart.setBackgroundResource(_BackgroundRes.PROJECT_PAPER.textEditResId());
                }
                return;

            case R.id.bg_button_3:
                if (mMimeType.startsWith("text/")) {
                    mBgColor = _BackgroundRes.MATHEMATICS.column();
                    mContentPart.setBackgroundResource(_BackgroundRes.MATHEMATICS.textEditResId());
                }
                return;

            case R.id.bg_button_4:
                if (mMimeType.startsWith("text/")) {
                    mBgColor = _BackgroundRes.GRAPHY.column();
                    mContentPart.setBackgroundResource(_BackgroundRes.GRAPHY.textEditResId());
                }
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

        setResult(RESULT_CANCELED);
        Intent  intent  = getIntent();
        if (intent == null) { return;}
        String action = intent.getAction();

        // BEGIN Motorola, a22183, 2012/02/06, IKCNDEVICS-751/Quicknote: in-call quicknote demo upmerge part 1.
        if ((QNConstants.INTENT_ACTION_NEW_INCALL).equals(action)) {
            mCallNumber = intent.getStringExtra("call_number");
            mCallDateBegin = intent.getLongExtra("call_date_begin", 0);
            mCallType = intent.getIntExtra("call_type", 0);
            mInCall = true;
            QNDev.logd(TAG, "onCreate: in-call note: call_number=" + mCallNumber + " call_date_begin=" + mCallDateBegin + " call_type=" + mCallType);

            if(!QNUtil.checkStorageCard(this)) {
                finish();
                return;
            }
            QNUtil.initDirs(this);

            String[] projection = new String[] { QNColumn._ID.column() };
            String selection = QNColumn.CALLNUMBER.column() + "=?" + " AND " + QNColumn.CALLDATEBEGIN.column() + "=?";
            String[] selectionArgs = new String[] { mCallNumber, String.valueOf(mCallDateBegin) };
            Cursor c = getContentResolver().query(QuickNotesDB.CONTENT_URI, projection, selection, selectionArgs, null);
            if (c!= null) {
                if (c.getCount() == 0) {
                    //create new
                    _runmode = _RunningModeE.EDIT_NEW;
                    mMimeType = QNConstants.MIME_TYPE_TEXT;
                    mContentUri = null;
                    mFileUri = QNUtil.prepareTextNote();
                    if (null == mFileUri) {
                        Log.i(TAG, "onCreate: in-call note: NEW: create mFileUri fail");
                        c.close();
                        finish();
                        return;
                    }
                    QNDev.logd(TAG, "onCreate: in-call note: NEW: mFileUri = " + mFileUri.toString());
                } else if (c.getCount() == 1) {
                    //edit
                    _runmode = _RunningModeE.EDIT_EDIT;
                    mMimeType = QNConstants.MIME_TYPE_TEXT;
                    c.moveToFirst();
                    mContentUri = ContentUris.withAppendedId(QuickNotesDB.CONTENT_URI, c.getLong(c.getColumnIndex(QNColumn._ID.column())));
                    mFileUri = QNUtil.buildUri((String)QuickNotesDB.read(this, mContentUri, QNColumn.URI));
                    if (null == mFileUri) {
                        Log.i(TAG, "onCreate: in-call note: EDIT: get mFileUri fail");
                        c.close();
                        finish();
                        return;
                    }
                    QNDev.logd(TAG, "onCreate: in-call note: EDIT: mFileUri = " + mFileUri.toString() + " mContentUri = " + mContentUri.toString());
                } else {
                    QNDev.logd(TAG, "onCreate: duplicate in-call note : error : finish()");
                    c.close();
                    finish();
                    return;
                }
                c.close();
            } else {
                Log.i(TAG, "onCreate: in-call note: c == null");
                finish();
                return;
            }
        } else {
            Log.i(TAG, "onCreate: in-call note: intent action is not QNConstants.INTENT_ACTION_NEW_INCALL");
            finish();
            return;
        }
        // END IKCNDEVICS-751

         _viewroot(R.id.main);
         QNDev.logd(TAG, " onCreate: in-call note: setContentView()");
         setContentView(R.layout.note);
         setupMainView();
    }


    @Override
    public void onDestroy() {
      QNDev.log(TAG + " onDestroy()");
      try {
         if(null != mQNContent) {
           mQNContent.stop();
           mQNContent.close();
         }

         destroyQNContent(mQNContent);
      } catch (Exception e) {
         QNDev.log(TAG + " onDestroy() catch exception e ="+ e);
      }
      super.onDestroy();
    }


    @Override
    public void onResume() {
       QNDev.log(TAG+ " onResume");
       super.onResume();
    }


    @Override
    public void onPause() {
       QNDev.log(TAG+ " onPause");
       super.onPause();
    }


    @Override
    public void onNewIntent(Intent intent) {
       QNDev.logd(TAG, "onNewIntent");
       String tmp1 = intent.getStringExtra("call_number");
       long tmp2 = intent.getLongExtra("call_date_begin", 0);
       int tmp3 = intent.getIntExtra("call_type", 0);
       QNDev.logd(TAG, "onNewIntent: call_number=" + tmp1 + " call_date_begin=" + tmp2 + " call_type=" + tmp3);

       if (tmp1.equals(mCallNumber) && (tmp2 == mCallDateBegin) && (tmp3 == mCallType)) {
           QNDev.logd(TAG, "onNewIntent: resume in-call note");
           return;
       } else {
           QNDev.logd(TAG, "onNewIntent: start new in-call note: save existing first");
           saveEditingNote();

           mCallNumber = tmp1;
           mCallDateBegin = tmp2;
           mCallType = tmp3;

           //create new
           _runmode = _RunningModeE.EDIT_NEW;
           mMimeType = QNConstants.MIME_TYPE_TEXT;
           mContentUri = null;
           mFileUri = QNUtil.prepareTextNote();
           if (null == mFileUri) {
               Log.i(TAG, "onNewIntent: in-call note: NEW: create mFileUri fail");
               finish();
           }
           QNDev.logd(TAG, "onNewIntent: in-call note: NEW: mFileUri = " + mFileUri.toString());

           setupNoteView();
       }
    }

    @Override
    public void onStop() {
       QNDev.log(TAG+ " onStop");
       super.onStop();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                saveEditingNote();
                finish();
        }

        return super.onKeyDown(keyCode, event);
    }


    private void setupNoteView() {
        QNDev.logd(TAG, " setupNoteView()");

        destroyQNContent(mQNContent);
        mQNContent = prepareQNContent(mMimeType, mFileUri);
        if (null == mQNContent) {
            finish();
            return;
        }

        mNoteView = mQNContent.noteView(QNInCall.this, mViewGroup);
        if (mNoteView == null) {
            QNDev.logd(TAG, "Noteview is null, so will show the error info.");
            mErrorLayout.setVisibility(View.VISIBLE);
            mTextContainer.setVisibility(View.GONE);
        }

        if(_RunningModeE.EDIT_EDIT == _runmode && QuickNotesDB.is_qnuri_type(mContentUri)) {
           String title = (String)QuickNotesDB.read(this, mContentUri, QNColumn.TITLE);
           if(title != null && title.length() > 0) {
              mEditTitle.setText(title);
           }
        }

        if (mMimeType.startsWith("text/")) {
            mContentPart.setBackgroundResource(R.drawable.bg_text_note_gridme);
            mTextContainer.setVisibility(View.VISIBLE);
            mBackgroundBtns.setVisibility(View.VISIBLE);
            mEditTextContent.requestFocus();
            toEditMode();
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

        // Setup with new uri content
        qnc.setup();
        return qnc;
    }

    /** edit mode, show OK and Cancel btn, editText areas is permitted to edit
       InputMethod is shown
    **/
    private void toEditMode() {
        QNDev.logd(TAG, " toEditMode()");
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
        mThumbnail.setVisibility(View.GONE);
        // END IKCNDEVICS-751

        if (mMimeType.startsWith("text/")) {
           mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "edit", "text"));
           mBackgroundBtns.setVisibility(View.VISIBLE);
        }

    }

   private void saveEditingNote() {
       QNDev.log(TAG+ " saveEditingNote()");

       if(_RunningModeE.EDIT_NEW == _runmode) {
          if (!QNUtil.checkStorageCard(QNInCall.this)) { return; }
            try {
               _create_note();
            } catch (Exception e) {
               Log.e(TAG, "saveEditingNote(): _create_note(): exception: ", e);
               Toast.makeText(QNInCall.this, R.string.load_failed, Toast.LENGTH_SHORT);
               return;
            }
            return;
        } else if (_RunningModeE.EDIT_EDIT == _runmode) {
            if (!QNUtil.checkStorageCard(QNInCall.this))  { return; }
              try {
                 _update_note();
              } catch (Exception e) {
                 Log.e(TAG, "saveEditingNote(): _update_note(): exception: ", e);
                 Toast.makeText(QNInCall.this, R.string.load_failed, Toast.LENGTH_SHORT);
                 return;
              }
        }
   }

    private void getCommonValues() {
       if ( null != mValues) {
         mMimeType = mValues.getAsString(QNColumn.MIMETYPE.column());
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

}
