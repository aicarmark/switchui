package com.android.calendar.confinfo;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.content.SharedPreferences;
import com.android.calendar.GeneralPreferences;

public class ConferenceCallDialogPreference extends DialogPreference {
    private Context mContext;

    public ConferenceCallDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        super.onPrepareDialogBuilder(builder);

        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(mContext);
        String confNumber = prefs.getString(GeneralPreferences.KEY_CONFERENCE_NUMBER, null);
        String confId = prefs.getString(GeneralPreferences.KEY_CONFERENCE_ID, null);

        ConferenceCallPreferenceDialogBuilder.buildDialog(mContext, builder, confNumber, confId);
    }
}
