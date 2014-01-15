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
 *
 */

package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

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

    private static final String TAG = TAG_PREFIX + RingtoneActivity.class.getSimpleName();
    private static final int PICK_RINGTONE = 1;
    private static final int DEFAULT_RINGTONE_POSITION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Intent launchIntent = getIntent();
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        Intent configIntent = ActionHelper.getConfigIntent(launchIntent.getStringExtra(EXTRA_CONFIG));
        if (configIntent != null) {
            // edit case
            Uri uri = Uri.parse(configIntent.getStringExtra(EXTRA_URI));
            setUpSelectedRingtone(intent, uri);
        } else {
            setUpSelectedRingtone(intent, RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE));
        }

        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, this.getResources().getString(
                            R.string.ringtone));
        startActivityForResult(intent, PICK_RINGTONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_RINGTONE) {

            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if(uri != null){
	                    if (LOG_INFO)
	                        Log.i(TAG, "Picked ringtone is " + uri.toString());
                        android.media.Ringtone rt = RingtoneManager.getRingtone(this, uri);
                        String title = null;
                        if (rt != null) {
                            title = rt.getTitle(this);
                            rt.stop();
                        }

	                    Intent intent = new Intent();
	                    intent.putExtra(EXTRA_DESCRIPTION, title);
	                    intent.putExtra(EXTRA_CONFIG, Ringtone.getConfig(uri.toString(), title));
	                    setResult(RESULT_OK, intent);
                    }
                } else {
                    // probably an error
                    Log.e(TAG, "Received null data");
                }
            } else {
                //User canceled the picker, nothing to send back
            }
            finish();
        }
    }

    /**
     * Method to set up the Ringtone to be selected initially in the picker
     * If the requested uri is available then it is selected otherwise first Ringtone is selected
     * @param intent Intent to set up
     * @param uri of requested Ringtone.
     */
    private void setUpSelectedRingtone (Intent intent, Uri uri) {
        RingtoneManager ringtoneManager = new RingtoneManager(this);
        if (uri != null && ringtoneManager.getRingtonePosition(uri) >= 0)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
        else
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    ringtoneManager.getRingtoneUri(DEFAULT_RINGTONE_POSITION));
    }
}
