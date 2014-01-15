package com.motorola.firewall;

import com.motorola.firewall.FireWall.Name;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.CursorLoader; // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class NameEditorActivity extends Activity {

    private static final String TAG = "NameEditor";

    private static final int ERROR_PATTERN = 1;
    private static final int DUP_NUMBER = 2;
    private static final int PATTERN_ALERT = 3;
    private static final int NAME_ALERT = 4;
    private static final int NULL_NUMBER = 5;

    private static final String[] mlist = new String[] {
        Name.WHITELIST,
        Name.WHITEPATTERN,
        Name.BLACKLIST,
        Name.BLACKPATTERN,
        Name.WHITENAME,
        Name.BLACKNAME,
        Name.WHITE,
        Name.BLACK
    };
    
    private static final String[] PROJECTION = new String[] {
        Name._ID, // 0
        Name.PHONE_NUMBER_KEY, // 1
        Name.CACHED_NAME, // 2
        Name.CACHED_NUMBER_TYPE, // 3
        Name.CACHED_NUMBER_LABEL, // 4
        Name.BLOCK_TYPE, // 5
        Name.FOR_CALL, // 6
        Name.FOR_SMS   // 7
    };
    
    private static final int COLUMN_INDEX_NUMBER = 1;
    private static final int COLUMN_INDEX_NAME = 2;
    private static final int COLUMN_INDEX_NUMBER_TYPE = 3;
    private static final int COLUMN_INDEX_NUMBER_LABEL = 4;
    private static final int COLUMN_INDEX_BLOCK_TYPE = 5;
    private static final int COLUMN_INDEX_FOR_CALL = 6;
    private static final int COLUMN_INDEX_FOR_SMS = 7;
    
    // This is our state data that is stored when freezing.
    private static final String ORIGINAL_CONTENT = "origContent";
    private static final String ORIGINAL_PATTERN = "origPattern";
    private static final String ORIGINAL_CALL = "origCall";
    private static final String ORIGINAL_SMS = "origSms";

    private Uri mUri;
    private EditText mText;
    private Button mSave;
    private Button mDiscard;
    private String mBlocktype;
    private String mOriginalContent;
    private Boolean mOriginalPattern;
    private Boolean mOriginalCall;
    private Boolean mOriginalSms;
    private CheckBox mPattern;
    private CheckBox mForcall;
    private CheckBox mForsms;
    private String mAction;
    private Intent intent;
	private MotoActionBar mActionBar;

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ERROR_PATTERN:
                return new AlertDialog.Builder(NameEditorActivity.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_error_pattern).setCancelable(false)
                        .setInverseBackgroundForced(true)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismissDialog(ERROR_PATTERN);
                                        finish();
                                    }
                                }).create();
            case DUP_NUMBER:
                int mmessageid = 0;
                if (mBlocktype.equals(mlist[0])) {
                    mmessageid = R.string.alert_dialog_dup_number_white_list;
                } else if (mBlocktype.equals(mlist[2])) {
                    mmessageid = R.string.alert_dialog_dup_number_black_list;
                } else if (mBlocktype.equals(mlist[1])) {
                    mmessageid = R.string.alert_dialog_dup_number_white_pattern;
                } else if (mBlocktype.equals(mlist[3])) {
                    mmessageid = R.string.alert_dialog_dup_number_black_pattern;
                } else if (mBlocktype.equals(mlist[6])) {
                    mmessageid = (mPattern.isChecked()) ? R.string.alert_dialog_dup_number_white_pattern :R.string.alert_dialog_dup_number_white_list; 
                } else if (mBlocktype.equals(mlist[7])) {
                    mmessageid = (mPattern.isChecked()) ? R.string.alert_dialog_dup_number_black_pattern :R.string.alert_dialog_dup_number_black_list;
                }
                return new AlertDialog.Builder(NameEditorActivity.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(mmessageid).setCancelable(false).setInverseBackgroundForced(true)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismissDialog(DUP_NUMBER);
                                        finish();
                                    }
                                }).create();
            case PATTERN_ALERT:
                return new AlertDialog.Builder(NameEditorActivity.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_pattern_content).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismissDialog(PATTERN_ALERT);
                                    }
                                }).create();
            case NAME_ALERT:
                return new AlertDialog.Builder(NameEditorActivity.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_name_content).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismissDialog(NAME_ALERT);
                                    }
                                }).create();
	    case NULL_NUMBER:
                return new AlertDialog.Builder(NameEditorActivity.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_number_null).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {

                                        /* User clicked OK so do some stuff */
                                        dismissDialog(NULL_NUMBER);
                                    }
                                }).create();
        }
        return null;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Name.CONTENT_URI);
        }

        // Do some setup based on the action being performed.

        mAction = intent.getAction();
        
        mBlocktype = intent.getStringExtra("blocktype");
        if (mBlocktype == null) {
            Log.e(TAG, "No block type, exiting");
            finish();
            return;
        }

        // Set the layout for this activity. You can find it in
        // res/layout/name_editor.xml
        if (mBlocktype.equals(mlist[1]) || mBlocktype.equals(mlist[3]) ){
            setContentView(R.layout.pattern_editor);
            mText = (EditText) findViewById(R.id.patternNumber);
        } else if (mBlocktype.equals(mlist[0]) || mBlocktype.equals(mlist[2])) {
            setContentView(R.layout.name_editor);
            mText = (EditText) findViewById(R.id.phoneNumber);
            mText.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
        } else {
            setContentView(R.layout.number_pattern_editor);
            mText = (EditText) findViewById(R.id.number_pattern);
            mPattern = (CheckBox) findViewById(R.id.is_pattern);
            mForcall = (CheckBox) findViewById(R.id.for_call);
            mForsms = (CheckBox) findViewById(R.id.for_sms);

            mPattern.setOnClickListener(mClickLister);
            mForcall.setOnClickListener(mClickLister);
            mForsms.setOnClickListener(mClickLister);
        }

        // The text view for our note, identified by its ID in the XML file.
        
        //mSave = (Button) findViewById(R.id.doneButton);
        //mDiscard = (Button) findViewById(R.id.revertButton);
        mActionBar = (MotoActionBar)findViewById(R.id.action_bar);
		mSave = new Button(this);
		mSave.setText(R.string.button_done);
		mSave.setOnClickListener(mClickLister);
		mDiscard = new Button(this);
		mDiscard.setText(R.string.button_revert);
        mDiscard.setOnClickListener(mClickLister);
		mActionBar.addButton(mSave);
		mActionBar.addButton(mDiscard);

        if ( ! Intent.ACTION_INSERT.equals(mAction) && ! Intent.ACTION_EDIT.equals(mAction)) {
            // Whoops, unknown action! Bail.
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }

        // If an instance of this activity had previously stopped, we can
        // get the original text it started with.
        if (savedInstanceState != null) {
            mOriginalContent = savedInstanceState.getString(ORIGINAL_CONTENT);
            mOriginalPattern = savedInstanceState.getBoolean(ORIGINAL_PATTERN);
            mOriginalCall = savedInstanceState.getBoolean(ORIGINAL_CALL);
            mOriginalSms = savedInstanceState.getBoolean(ORIGINAL_SMS);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        String[] medtitle = getResources().getStringArray(R.array.white_black_editor_title);
        String mtitle = null;
        mText.setInputType(EditorInfo.TYPE_CLASS_PHONE);
        if (mBlocktype.equals(mlist[0])) {
            mtitle = medtitle[0];
        } else if (mBlocktype.equals(mlist[2])) {
            mtitle = medtitle[2];
        } else if (mBlocktype.equals(mlist[1])) {
            mtitle = medtitle[1];
        } else if (mBlocktype.equals(mlist[3])) {
            mtitle = medtitle[3];
        } else if (mBlocktype.equals(mlist[6])) {
            if (Intent.ACTION_INSERT.equals(mAction)) {
                mtitle = medtitle[4];
            }
            mForcall.setText(R.string.number_for_call_white);
            mForsms.setText(R.string.number_for_sms_white);
        } else if (mBlocktype.equals(mlist[7])) {
            if (Intent.ACTION_INSERT.equals(mAction)) {
                mtitle = medtitle[5];
            }
            mForcall.setText(R.string.number_for_call_black);
            mForsms.setText(R.string.number_for_sms_black);
        }else {
            Log.e(TAG, "Unknown block type, exiting");
            finish();
            return;
        }

        TextView pattern_help = (TextView) findViewById(R.id.pattern_text);
        if (Intent.ACTION_EDIT.equals(mAction)) {
            Cursor mcuror = getContentResolver().query(intent.getData(),PROJECTION, null, null, null);
            if (mcuror != null && mcuror.moveToFirst()) {
                mText.setText(mcuror.getString(COLUMN_INDEX_NUMBER));
                String mbtype = mcuror.getString(COLUMN_INDEX_BLOCK_TYPE);
                mPattern.setChecked(Name.BLACKPATTERN.equals(mbtype) || Name.WHITEPATTERN.equals(mbtype));
                mPattern.setVisibility(View.GONE);
                if(Name.BLACKLIST.equals(mbtype)){
                    mtitle = medtitle[7];
                    pattern_help.setVisibility(View.GONE);
                }else if(Name.BLACKPATTERN.equals(mbtype)){
                    mtitle = medtitle[9];
                }else if(Name.WHITELIST.equals(mbtype)){
                    mtitle = medtitle[6];
                    pattern_help.setVisibility(View.GONE);
                }else if(Name.WHITEPATTERN.equals(mbtype)){
                    mtitle = medtitle[8];
                    // no pattern mode in white list
                    pattern_help.setVisibility(View.GONE);
                }else mtitle = "unknow type";
                
                mForcall.setChecked(mcuror.getInt(COLUMN_INDEX_FOR_CALL) != 0);
                mForsms.setChecked(mcuror.getInt(COLUMN_INDEX_FOR_SMS) != 0);
                mcuror.close();
            }
        } else if (Intent.ACTION_INSERT.equals(mAction) && mBlocktype.equals(mlist[6])) {
            // no pattern mode in white list
            pattern_help.setVisibility(View.GONE);
            mPattern.setVisibility(View.GONE);
        }
        setTitle(mtitle);
        if (mOriginalContent != null) {
            mText.setTextKeepState(mOriginalContent);
            mPattern.setChecked(mOriginalPattern);
            mForcall.setChecked(mOriginalCall);
            mForsms.setChecked(mOriginalSms);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save away the original text, so we still have it if the activity
        // needs to be killed while paused.
        mOriginalContent = mText.getText().toString();
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
        mOriginalPattern = mPattern.isChecked();
        outState.putBoolean(ORIGINAL_PATTERN, mOriginalPattern);
        mOriginalCall = mForcall.isChecked();
        outState.putBoolean(ORIGINAL_CALL, mOriginalCall);
        mOriginalSms = mForsms.isChecked();
        outState.putBoolean(ORIGINAL_SMS, mOriginalSms);
    }

    private boolean checkAndSave (boolean isBackPressed) {
        String mnumber = PhoneNumberUtils.extractNetworkPortion(mText.getText().toString());
        int len = mnumber.length();
        if ((mnumber == null) || (mnumber.length() == 0)) {
	    if (!isBackPressed) {
	        showDialog(NULL_NUMBER);
	        return false;
	    } else {
	        return true;
	    }
	} else {
            if (mBlocktype.equals(mlist[0]) || mBlocktype.equals(mlist[2])) {
                if (NameListActivity.checkDup(getContentResolver() ,mnumber, mBlocktype, false)) {
                    showDialog(DUP_NUMBER);
                    return false;
                }
            } else if (mBlocktype.equals(mlist[1]) || mBlocktype.equals(mlist[3])) {
                try {
                    Pattern.compile("^" + mnumber);
                } catch (PatternSyntaxException e) {
                    showDialog(ERROR_PATTERN);
                    return false;
                }
            } else if (mBlocktype.equals(mlist[6]) || mBlocktype.equals(mlist[7])) {
                if (mPattern.isChecked()) {
                    try {
                       if(mnumber.charAt(len-1) == '#')
                            Pattern.compile(mnumber.substring(0,len-1)+'$');
                        else Pattern.compile("^" + mnumber);
                    } catch (PatternSyntaxException e) {
                        showDialog(ERROR_PATTERN);
                        return false;
                    }
                    String mselecting_string;
                    if (mBlocktype.equals(mlist[6])) {
                        mselecting_string = Name.BLOCK_TYPE + " = " + "'" + Name.WHITEPATTERN + "'";
                    } else {
                        mselecting_string = Name.BLOCK_TYPE + " = " + "'" + Name.BLACKPATTERN + "'";
                    }
                    if (! Intent.ACTION_EDIT.equals(mAction)) {
                        // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
			/*Cursor cursor = managedQuery(Name.CONTENT_URI, PROJECTION, mselecting_string, null,
                                null);*/
			CursorLoader cursorLoader = new CursorLoader(this, Name.CONTENT_URI, PROJECTION, mselecting_string, null, null);
			Cursor cursor = cursorLoader.loadInBackground();
			// MOT Calling Code End
                        if (cursor != null && cursor.getCount() >= Name.MAXBLACKPATTERNS) {
                            showDialog(PATTERN_ALERT);
                            return false;
                        }
                    }
                } else {
                    String mselecting_string;
                    if (mBlocktype.equals(mlist[6])) {
                        mselecting_string = Name.BLOCK_TYPE + " = " + "'" + Name.WHITELIST + "'";
                    } else {
                        mselecting_string = Name.BLOCK_TYPE + " = " + "'" + Name.BLACKLIST + "'";
                    }
                    if (! Intent.ACTION_EDIT.equals(mAction)) {
                        // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
			/*Cursor cursor = managedQuery(Name.CONTENT_URI, PROJECTION, mselecting_string, null,
                                null);*/
			CursorLoader cursorLoader = new CursorLoader(this, Name.CONTENT_URI, PROJECTION, mselecting_string, null, null);
			Cursor cursor = cursorLoader.loadInBackground();
			// MOT Calling Code End
                        if (cursor != null && cursor.getCount() >= Name.MAXNAMEITEMS) {
                            showDialog(NAME_ALERT);
                            return false;
                        }
                    }
                }
            }

            ContentValues values = new ContentValues();
            if (Intent.ACTION_EDIT.equals(mAction)) {
                mUri = intent.getData();
            } else {
                if (NameListActivity.checkDup(getContentResolver() ,mnumber, mBlocktype, mPattern.isChecked())) {
                    showDialog(DUP_NUMBER);
                    return false;
                }
                mUri = getContentResolver().insert(intent.getData(), null);
            }

            if (mBlocktype.equals(mlist[6]) || mBlocktype.equals(mlist[7])) {
                String mtype;
                if (mPattern.isChecked()) {
                    mtype = (mBlocktype.equals(mlist[6])) ? Name.WHITEPATTERN : Name.BLACKPATTERN ;
                    values.putNull(Name.CACHED_NAME);
                    values.putNull(Name.CACHED_NUMBER_TYPE);
                    values.putNull(Name.CACHED_NUMBER_LABEL);
                } else {
                    mtype = (mBlocktype.equals(mlist[6])) ? Name.WHITELIST : Name.BLACKLIST ;
                }
                // Write our text back into the provider.
                values.put(Name.PHONE_NUMBER_KEY, mnumber);
                values.put(Name.BLOCK_TYPE, mtype);
                values.put(Name.FOR_CALL, mForcall.isChecked());
                values.put(Name.FOR_SMS, mForsms.isChecked());
                getContentResolver().update(mUri, values, null, null);
            } else {
                // Write our text back into the provider.
                values.put(Name.PHONE_NUMBER_KEY, mnumber);
                values.put(Name.BLOCK_TYPE, mBlocktype);
                getContentResolver().update(mUri, values, null, null);
            }
        }
        return true;
    }

    public void onBackPressed () {
        if ( checkAndSave(true) ) {
            finish();
        }
    }

    private View.OnClickListener mClickLister = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.equals(mSave)) {
                if ( checkAndSave(false) ) {
                    finish();
                }
            } else if (v.equals(mDiscard)) {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    };
}
