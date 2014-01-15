/*
 * @(#)Ringtone.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/03/04  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

/**
 * This class extends the StatefulAction class for Ringtone <code><pre>
 * CLASS:
 *     extends StatefulAction
 *
 * RESPONSIBILITIES:
 *     Uses Settings.System interface to get/set Ringtone
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public final class Ringtone extends StatefulAction implements Constants {

    private static final String TAG = TAG_PREFIX + Ringtone.class.getSimpleName();

    private String mOldUri;
    private String mUri;
    private String mTitle;

    public boolean setState(Context context, Intent intent) {

        //Initialize mOldUri so that default ringtone value is saved to persistence if it is a fire request
        mOldUri = Settings.System.getString(context.getContentResolver(), Settings.System.RINGTONE);
        if (mOldUri == null) {
            // Current ringtone is silent. This is enough since in UI we
            // don't allow the user to select silent ringtone
            mOldUri = RINGTONE_SILENT;
        }

        mUri = intent.getStringExtra(EXTRA_URI);
        mTitle = intent.getStringExtra(EXTRA_TITLE);

        if (LOG_INFO)
            Log.i(TAG, "setState Ringtone is " + mTitle);

        intent.setClass(context, DatabaseUtilityService.class);
        intent.putExtra(EXTRA_INTENT_ACTION, RINGTONE_ACTION_KEY);
        context.startService(intent);

        return true;
    }

    public String getState(Context context) {
        return mTitle;
    }

    public String getSettingString(Context context) {
        String title = getRingtoneTitle(context, mUri);
        return (title != null) ? title : getActionString(context);
    }

    public String getDefaultSetting(Context context) {
        return getConfig(mOldUri, getRingtoneTitle(context, mOldUri));
    }

    public void registerForSettingChanges(Context context) {
        StatefulActionHelper.registerForSettingChanges(context, RINGTONE_ACTION_KEY);

    }

    public void deregisterFromSettingChanges(Context context) {
        StatefulActionHelper.deregisterFromSettingChanges(context, RINGTONE_ACTION_KEY);
    }

    public String getActionString(Context context) {
        return context.getString(R.string.ringtone);
    }

    public Status handleSettingChange(Context context, Object obj) {

        Status status = Status.FAILURE;
        if (obj instanceof String) {
            String oldUri = Persistence.retrieveValue(context, RINGTONE_STATE_KEY);
            String observed = (String)obj;
            mUri = Settings.System.getString(context.getContentResolver(), observed);
            if (mUri == null) {
                // The ringtone has been set to Silent
                mUri = RINGTONE_SILENT;
            }
            mTitle = getRingtoneTitle(context, mUri);
            if (LOG_INFO)
                Log.i(TAG, "settingChange Ringtone is " + mTitle);
            if(!mUri.equals(oldUri)) {
                status = Status.SUCCESS;
            } else {
                status = Status.NO_CHANGE;
            }
        }

        return status;
    }

    public String[] getSettingToObserve() {
        return new String[] {
                   Settings.System.RINGTONE
               };
    }

    public Uri getUriForSetting(String setting) {
        return Settings.System.getUriFor(setting);
    }

    public String getActionKey() {
        return RINGTONE_ACTION_KEY;
    }

    /**
     * Returns the ringtone name corresponding to the passed in string representation
     * of the uri
     *
     * @param context - caller's context
     * @param uri - string representation of the uri
     * @return
     */
    public static String getRingtoneTitle(Context context, String uri) {
        if (uri != null && uri.equals(RINGTONE_SILENT)) {
            // This check is added so that if user manually changes the
            // ringtone to Silent while the rule is active, the intent for
            // reverting the rule shall not come

            return context.getString(R.string.silent);
        }

        // Do not turn the following into an AsyncTask.  The caller of this
        // method should call this from an AsyncTask instead.
        android.media.Ringtone r = (uri != null) ? RingtoneManager.getRingtone(context, Uri.parse(uri)) : null;
        String title = null;
        if (r != null) {
            title = r.getTitle(context);
            r.stop();
        }
        return title;
    }

    @Override
    String getDefaultSetting(Context context, Intent defaultIntent) {
        return getUpdatedConfig(context, defaultIntent);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        return getRingtoneTitle(context, configIntent.getStringExtra(EXTRA_URI));
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getStringExtra(EXTRA_URI), configIntent.getStringExtra(EXTRA_TITLE));
    }


    @Override
    public List<String> getConfigList(Context context) {
        return new ArrayList<String>();
    }

    @Override
    public List<String> getDescriptionList(Context context) {
        return new ArrayList<String>();
    }

    @Override
    public boolean isResponseAsync() {
        return true;
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        return configIntent.getStringExtra(EXTRA_URI) != null;
    }

    /**
     * Method to return config using supplied parameters
     *
     * @param ringtoneUri Ringtone uri
     * @param title Ringtone title
     * @return Config
     */
    public static String getConfig(String ringtoneUri, String title) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_URI, ringtoneUri);
        intent.putExtra(EXTRA_TITLE, title);
        return intent.toUri(0);
    }
}
