package com.motorola.quicknote;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.quicknote.QNConstants._BackgroundRes;
import com.motorola.quicknote.QuickNotesDB.QNColumn;
import com.motorola.quicknote.content.QNContent;
import com.motorola.quicknote.content.QNContent.NotestateE;
import com.motorola.quicknote.content.QNContent_Factory;
import com.motorola.quicknote.content.QNContent_Error;
import com.motorola.quicknote.content.QNContent_Snd;

public class QNAlarmShow extends Activity {
	// public class QNAlarmShow extends QNActivity implements
	// View.OnClickListener {
	/******************************
	 * Constants
	 ******************************/
	private static final String TAG = "[QNAlarmShow]";

	private ContentValues mValues = null;
	private QNContent mQNContent = null; // quick note contents
	private Uri mContentUri = null; // current note's conent uri :
									// content://com.motorola.provider.quicknote/qnotes/4
	private String mMimeType = null;
	private int mBgColor = _BackgroundRes.GRIDME.column(); // this is default
	private Uri mFileUri = null; // current note's file uir
									// :file:///mnt/sdcard/quicknote/text/2012-02-06_05-00-49_932.txt

	private ViewGroup mViewGroup = null;
	private RelativeLayout mTopBar = null;
	private RelativeLayout mTitlePart = null;
	private TextView mIndexCount = null;
	private EditText mEditTitle = null;
	private TextView mDetailTitle = null;
	private EditText mEditTextContent = null;
	private TextView mDetailTextContent = null;
	private RelativeLayout mTextContainer = null;
	private ScrollView mDetailTextPart = null;
	private ScrollView mEditTextPart = null;
	private RelativeLayout mContentPart = null;
	private ImageView mImageContent = null;
    private RelativeLayout       mImageContentLayout= null;
	private ImageView mImageDetail = null;
	private ImageView mImageEditBtn = null;
	private RelativeLayout mVoiceContent = null;
	private LinearLayout mBackgroundBtns = null;
	private TextView mEditVoiceDuration = null;
	private LinearLayout mDetailVoiceLayout = null;
	private View mNoteView = null;
	private View mReminderIndicator = null;
	private ViewGroup mCallInfo = null;
	private ImageView mCallInfoType = null;
	private TextView mCallInfoName = null;
	private TextView mCallInfoNumber = null;
	private TextView mCallInfoDate = null;
	private RelativeLayout mErrorLayout = null;

	/**************************
	 * Override Functions
	 **************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		QNDev.log(TAG + "reminder: onCreate, ");

		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String uri_string = intent.getStringExtra("qnUri");
		QNDev.log(TAG + "reminder: alarm uri = " + uri_string);
		if (uri_string == null) {
			return;
		}
		mContentUri = QNUtil.buildUri(uri_string);
        if (null == mContentUri) { return;}


        //check whether this Uri is valid or not
        if (!QuickNotesDB.isValidUri(this, mContentUri)) {
           //not a valid uri, maybe this note has been delete
           QNDev.log(TAG+"QNAlarmShow: uri = "+mContentUri+" is not a valid uri, return!");
           Toast.makeText(this, getString(R.string.notenotfound_toast), Toast.LENGTH_LONG).show();
           return;
        }

		String[] columns_excluded = new String[4];

		columns_excluded[0] = QNColumn._ID.column();
		columns_excluded[1] = QNColumn.WIDGETID.column();
		columns_excluded[2] = QNColumn.SPANX.column();
		columns_excluded[3] = QNColumn.SPANY.column();

		mValues = QuickNotesDB.copy_values(this, mContentUri, columns_excluded);
		String fileUri_string = mValues.getAsString(QNColumn.URI.column());
		mFileUri = QNUtil.buildUri(fileUri_string);
		mBgColor = mValues.getAsInteger(QNColumn.BGCOLOR.column());
		mMimeType = mValues.getAsString(QNColumn.MIMETYPE.column());

		setContentView(R.layout.note);

		mViewGroup = (ViewGroup) findViewById(R.id.main);
		// for top bar
		mTopBar = (RelativeLayout) findViewById(R.id.top_bar);
		mTopBar.setVisibility(View.INVISIBLE);
		// for title
		mTitlePart = ((RelativeLayout) findViewById(R.id.title));
		mIndexCount = ((TextView) findViewById(R.id.count));
		mIndexCount.setVisibility(View.GONE);
		mEditTitle = ((EditText) findViewById(R.id.edit_title));
		mEditTitle.setVisibility(View.GONE);
		mDetailTitle = ((TextView) findViewById(R.id.detail_title));
		mDetailTitle.setVisibility(View.VISIBLE);
		mDetailTitle.setHint(R.string.no_title_label);
		// for content
		mContentPart = (RelativeLayout) findViewById(R.id.content);
		// for text content of text note
		mTextContainer = (RelativeLayout) findViewById(R.id.scrolling_text);
		mEditTextPart = (ScrollView) findViewById(R.id.edit_scrolling_area);
		mEditTextPart.setVisibility(View.GONE);
		mDetailTextPart = (ScrollView) findViewById(R.id.detail_scrolling_area);
		mDetailTextContent = (TextView) findViewById(R.id.detail_text_content);
		// for content of image note
		mImageContent = (ImageView) findViewById(R.id.image_content);
        mImageContentLayout = (RelativeLayout)findViewById(R.id.image_small);
        mImageContentLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               //hide all other icons and only show the image in full screen
                mViewGroup.setBackgroundResource(0);
                mQNContent.detailView(QNAlarmShow.this, mViewGroup, mFileUri);
                mContentPart.setVisibility(View.GONE);
                mImageDetail.setVisibility(View.VISIBLE);
                return;
            }
        });

        mImageDetail = (ImageView)findViewById(R.id.image_detail);
        mImageDetail.setOnClickListener (new View.OnClickListener() {
            public void onClick(View v) {
               //back to normal view
               mViewGroup.setBackgroundResource(R.drawable.bg_board_softwood);
               mImageDetail.setImageResource(0);
               mContentPart.setVisibility(View.VISIBLE);
               mImageDetail.setVisibility(View.GONE);
               return;
            }
        });

		mImageEditBtn = (ImageView) findViewById(R.id.image_edit_btn);
		mImageEditBtn.setVisibility(View.GONE);
		// for content of voice note
		mVoiceContent = (RelativeLayout) findViewById(R.id.voice_content);
		mEditVoiceDuration = ((TextView) findViewById(R.id.edit_voice_duration));
		mEditVoiceDuration.setVisibility(View.GONE);
		mDetailVoiceLayout = ((LinearLayout) findViewById(R.id.detail_voioce));
		// for bottom btns
		mBackgroundBtns = (LinearLayout) findViewById(R.id.bottom_buttons);
		mBackgroundBtns.setVisibility(View.GONE);
		// for reminder
		mReminderIndicator = (View) findViewById(R.id.reminder);
		// for call info
		mCallInfo = (ViewGroup) findViewById(R.id.call_info);
		mCallInfoType = (ImageView) findViewById(R.id.call_info_type_icon);
		mCallInfoName = (TextView) findViewById(R.id.call_info_name);
		mCallInfoNumber = (TextView) findViewById(R.id.call_info_number);
		mCallInfoDate = (TextView) findViewById(R.id.call_info_date);
		// for Error layout
		mErrorLayout = (RelativeLayout) findViewById(R.id.error);

		setupView();

		// send out the broadcast
		Intent intentUpdate = new Intent(QNConstants.INTENT_ACTION_NOTE_UPDATED);
		if (mContentUri != null) {
			intentUpdate.putExtra(QNConstants.NOTE_URI, mContentUri.toString());
		}
		sendBroadcast(intentUpdate);

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
//		case KeyEvent.KEYCODE_MENU:
			finish();
			break;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onDestroy() {
		QNDev.log(TAG + " onDestroy()");
		try {
			if (null != mQNContent) {
				mQNContent.stop();
				mQNContent.close();
			}
			destroyQNContent(mQNContent);
		} catch (Exception e) {
			QNDev.log(TAG + " onDestroy() catch exception e =" + e);
		}

		super.onDestroy();
	}

	/**************************
	 * Local Functions
	 **************************/
	private void setupView() {
		QNDev.log(TAG + "setupView()");
		destroyQNContent(mQNContent);
		mQNContent = prepareQNContent(mMimeType, mFileUri);
		if (null == mQNContent) {
			finish();
			return;
		}

		mNoteView = mQNContent.noteView(this, mViewGroup);

        String title = (String)QuickNotesDB.read(this, mContentUri, QNColumn.TITLE);
        if(title != null && title.length() > 0) {
            mDetailTitle.setText(title);
        } else {
            mDetailTitle.setText(null);
        }

        //set the views' visibility
        if (mMimeType.startsWith("text/")) {
            mTextContainer.setVisibility(View.VISIBLE);
            mImageContentLayout.setVisibility(View.GONE);
            mImageDetail.setVisibility(View.GONE);
            mVoiceContent.setVisibility(View.GONE);
            mReminderIndicator.setVisibility(View.GONE);
            //set the background
            mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "view", "text"));
        } else if(mMimeType.startsWith("image/")) {
            mContentPart.setBackgroundResource(R.drawable.bg_text_note_paper_single);
            mTextContainer.setVisibility(View.GONE);
            mImageContentLayout.setVisibility(View.VISIBLE);
            mImageDetail.setVisibility(View.GONE);
            mVoiceContent.setVisibility(View.GONE);
            mReminderIndicator.setVisibility(View.GONE);
        } else if (mMimeType.startsWith("audio/")) {
            mTextContainer.setVisibility(View.GONE);
            mImageContentLayout.setVisibility(View.GONE);
            mImageDetail.setVisibility(View.GONE);
            mVoiceContent.setVisibility(View.VISIBLE);
            mDetailVoiceLayout.setVisibility(View.VISIBLE);
            mReminderIndicator.setVisibility(View.INVISIBLE);
            //set the background
            mContentPart.setBackgroundResource(_BackgroundRes.find_drawable(mBgColor, "view", "audio"));
        }

        if (mNoteView == null) {
            QNDev.log(TAG+"Noteview is null, so will show the error info.");
            //use the yellow bg to the background to keep consistent with widget and thumbnails
            mContentPart.setBackgroundResource(R.drawable.bg_text_note_gridme_single);
            mErrorLayout.setVisibility(View.VISIBLE);
            mTextContainer.setVisibility(View.GONE);
            mImageContentLayout.setVisibility(View.GONE);
            mImageEditBtn.setVisibility(View.GONE);
            mImageDetail.setVisibility(View.GONE);
            mVoiceContent.setVisibility(View.GONE);
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

		return qnc;
	}

}
