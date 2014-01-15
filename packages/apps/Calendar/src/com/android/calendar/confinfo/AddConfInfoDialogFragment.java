package com.android.calendar.confinfo;

import com.android.calendar.GeneralPreferences;
import com.android.calendar.R;
import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnShowListener;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class AddConfInfoDialogFragment extends DialogFragment {
    private View mView;
    private EditText mNumberWidget;
    private EditText mIdWidget;

    private TextView mTitle;
    private Uri mUri;
    private final Context mContext;
    private String mConfereNumber;
    private String mConfereId;

    private static int DIALOG_WIDTH = 500;
    private static int DIALOG_HEIGHT = 600;

    public AddConfInfoDialogFragment(Context context, Uri uri) {

        DIALOG_WIDTH = (int) context.getResources().getDimension(R.dimen.event_info_width);
        DIALOG_HEIGHT = (int) context.getResources().getDimension(R.dimen.event_info_height);
        mUri = uri;
        mContext = context;
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    public AddConfInfoDialogFragment(Context context, String confereNumber, String confereId) {

        DIALOG_WIDTH = (int) context.getResources().getDimension(R.dimen.event_info_width);
        DIALOG_HEIGHT = (int) context.getResources().getDimension(R.dimen.event_info_height);
        mUri = null;
        mContext = context;
        mConfereNumber = confereNumber;
        mConfereId = confereId;
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.conference_call_preference_dialog_layout, null);
        mTitle = (TextView) mView.findViewById(R.id.conference_id);
        mNumberWidget = (EditText) mView.findViewById(R.id.telephone_number);
        if (mNumberWidget != null) {
            mNumberWidget.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

            mNumberWidget.setKeyListener(DigitsKeyListener.getInstance("0123456789-+(). "));
            mNumberWidget.setRawInputType(InputType.TYPE_CLASS_PHONE);

            if (mConfereNumber != null) {
                mNumberWidget.setText(mConfereNumber);
            }
        }
        mIdWidget = (EditText) mView.findViewById(R.id.conference_id);
        if (mIdWidget != null) {
            mIdWidget.setKeyListener(DigitsKeyListener.getInstance("0123456789-*#,;"));
            mIdWidget.setRawInputType(InputType.TYPE_CLASS_PHONE);

            if (mConfereId != null) {
                mIdWidget.setText(mConfereId);
            }
        }
        if (mUri != null) {
            // start loading the data
            /*
             * mHandler.startQuery(TOKEN_QUERY_EVENT, null, mUri,
             * EVENT_PROJECTION, null, null, null);
             */
        }

        Button b = (Button) mView.findViewById(R.id.conf_ok);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                saveConferenceInfo(mContext, mNumberWidget, mIdWidget);
            }
        });

        return mView;
    }

    public static void saveConferenceInfo(Context ctx, EditText numberWidget, EditText idWidget) {
        if ((numberWidget == null) || (idWidget == null))
            return;

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(ctx);
        SharedPreferences.Editor editor = prefs.edit();

        String newNumber = numberWidget.getText().toString();
        String newId = idWidget.getText().toString();
        String oldNumber = prefs.getString(GeneralPreferences.KEY_CONFERENCE_NUMBER, null);
        String oldId = prefs.getString(GeneralPreferences.KEY_CONFERENCE_ID, null);

        boolean dirty = false;

        if (!TextUtils.equals(newNumber, oldNumber)) {
            if (newNumber != null) {
                editor.putString(GeneralPreferences.KEY_CONFERENCE_NUMBER, newNumber);
            } else {
                editor.putString(GeneralPreferences.KEY_CONFERENCE_NUMBER, "");
            }
            dirty = true;
        }

        if (!TextUtils.equals(newId, oldId)) {
            if (newId != null) {
                editor.putString(GeneralPreferences.KEY_CONFERENCE_ID, newId);
            } else {
                editor.putString(GeneralPreferences.KEY_CONFERENCE_ID, "");
            }
            dirty = true;
        }

        if (dirty) {
            editor.commit();
        }
    }

}
