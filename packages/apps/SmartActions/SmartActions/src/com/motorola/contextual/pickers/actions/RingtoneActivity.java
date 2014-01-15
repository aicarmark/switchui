/*
 * @(#)RingtoneActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 * XPR643       2012/05/17  Smart Actions 2.1   Launch custom ringtone chooser
 *
 */

package com.motorola.contextual.pickers.actions;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Window;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Ringtone;

import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to select the ringtone to be set as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select the ringtone.
 *     Sends the intent containing the Uri of the selected ringtone
 *     to Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class RingtoneActivity extends Activity implements Constants {
    // Adapted class copied from com.motorola.contextual.actions.RingtoneActivity

    private static final String TAG = TAG_PREFIX + RingtoneActivity.class.getSimpleName();
    private static final int PICK_RINGTONE = 1;
    private static final int DEFAULT_RINGTONE_POSITION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (LOG_DEBUG) Log.d(TAG, "onCreate called");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Intent launchIntent = getIntent();
        Intent configIntent = ActionHelper.getConfigIntent(launchIntent.getStringExtra(EXTRA_CONFIG));
        final Uri uri;
        if (configIntent != null) {
            // edit case
            uri = Uri.parse(configIntent.getStringExtra(EXTRA_URI));
        } else {
            uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE);
        }

        (new SetupTask()).execute(new SetupTask.Param(this, uri));
    }

    private static class SetupTask extends AsyncTask<SetupTask.Param, Void, Void> {

        static class Param {
            RingtoneActivity rt;
            Uri uri;

            Param(RingtoneActivity rt, Uri uri) {
                this.rt = rt;
                this.uri = uri;
            }
        }

        protected Void doInBackground(Param... params) {
            Param param = params[0];
            if (param == null)
                return null;

            final RingtoneActivity rt = param.rt;         	// rt will not be null
            final Intent intent = new Intent(rt, RingtoneChooserActivity.class);
            rt.setUpSelectedRingtone(intent, param.uri);

            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, rt.getResources().getString(
                                R.string.ringtone));
            rt.startActivityForResult(intent, PICK_RINGTONE);
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_RINGTONE) {
            (new FinishTask()).execute(new FinishTask.Param(this, resultCode, data));
        }
    }

    private static class FinishTask extends AsyncTask<FinishTask.Param, Void, Void> {

        static class Param {
            RingtoneActivity rt;
            int resultCode;
            Intent data;

            Param(RingtoneActivity rt, int resultCode, Intent data) {
                this.rt = rt;
                this.resultCode = resultCode;
                this.data = data;
            }
        }

        protected Void doInBackground(Param... params) {
            Param param = params[0];
            if (param == null)
                return null;

            final RingtoneActivity rt = param.rt;         	// rt will not be null
            final Intent data = param.data;

            if (param.resultCode == RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if(uri != null){
                        if (LOG_INFO)
                            Log.i(TAG, "Picked ringtone is " + uri.toString());
                        String title = Ringtone.getRingtoneTitle(rt, uri.toString());
                        Intent intent = new Intent();
                        intent.putExtra(EXTRA_DESCRIPTION, title);
                        intent.putExtra(EXTRA_CONFIG, Ringtone.getConfig(uri.toString(), title));
                        rt.setResult(RESULT_OK, intent);
                    }
                } else {
                    // probably an error
                    Log.e(TAG, "Received null data");
                }
            } else {
                //User canceled the picker, nothing to send back
            }
            rt.finish();
            return null;
        }
    }

    /**
     * Method to set up the Ringtone to be selected initially in the picker
     * If the requested uri is available then it is selected otherwise first Ringtone is selected
     * @param intent Intent to set up
     * @param uri of requested Ringtone.
     */
    private void setUpSelectedRingtone (Intent intent, Uri uri) {
        if ((uri != null) && (exists(uri))) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
        } else {
            RingtoneManager ringtoneManager = new RingtoneManager(this);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtoneManager.getRingtoneUri(DEFAULT_RINGTONE_POSITION));
            Cursor c = ringtoneManager.getCursor();
            if (c != null) c.close();
        }
    }

    /**
     * Method to check whether the specified uri exists (deleted or not).
     * @param uri The uri of the checked sound media.
     * @return true if the sound exists, false if the sound has been deleted.
     */
    private boolean exists(Uri uri) {
        boolean result=false;
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
            if ((c != null) && (c.moveToFirst())) {
                result=true;
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        finally {
            if (c != null) c.close();
        }
        return result;
    }
}
