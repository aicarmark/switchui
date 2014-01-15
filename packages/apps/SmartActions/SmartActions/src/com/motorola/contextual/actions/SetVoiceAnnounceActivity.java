/*
 * @(#)SetVoiceAnnounceActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * rdq478       2011/09/28  IKMAIN-28588      Initial version
 *
 */

package com.motorola.contextual.actions;

import java.net.URISyntaxException;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to setup voice announce as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends MultSelectDialogActivity
 *
 * RESPONSIBILITIES:
 *     Does a startActivity with option to set voice announce for text message or
 *         incoming call.
 *     Select option(s) will be sent it to Rules Builder
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class SetVoiceAnnounceActivity extends MultiSelectDialogActivity implements Constants {

    private static final String TAG = TAG_PREFIX + SetVoiceAnnounceActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources res = this.getResources();

        mItems = res.getStringArray(R.array.voice_announce_items);
        mTitle = getString(R.string.voice_announce);
        mIconId = R.drawable.ic_dialog_voice_announce;

        // Do a show Dialog, only when the activity is first created, don't do it for
        // orientation changes
        if (savedInstanceState == null) {
            super.showDialog();
        }
    }

    @Override
    protected boolean[] getSettingFromConfigUri(String configUri) {

        if (mItems == null) {
            Log.e(TAG, "Error no item yet. mItems = null");
            return null;
        }

        boolean[] action = {true, true};

        if (configUri == null) {
            if (LOG_DEBUG) Log.d(TAG, "Could not get previous setting, configUri = null");
            return action;
        }

        try {
            // Convert the Uri to an intent to get at the fields
            Intent configIntent = Intent.parseUri(configUri, 0);
            action[0] = configIntent.getBooleanExtra(EXTRA_VA_READ_TEXT, false);
            action[1] = configIntent.getBooleanExtra(EXTRA_VA_READ_CALL, false);
        } catch (URISyntaxException e) {
            Log.w(TAG, "Exception occurred when parsing configUri = " + configUri);
            e.printStackTrace();
        }

        return action;
    }

    @Override
    protected Intent prepareResultIntent() {
        String display = getDisplayString();

        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG, SetVoiceAnnounce.getConfig(mCheckedItems[0], mCheckedItems[1]));
        intent.putExtra(EXTRA_DESCRIPTION, display);
        return intent;
    }

    @Override
    protected String getDisplayString() {
        String display = null;
        if (mCheckedItems != null) {
            int selectedItemIndex = 0;
            int numberOfSelectedItems = 0;
            for (int i = 0; i < mCheckedItems.length; i++) {
                if (mCheckedItems[i]) {
                    selectedItemIndex = i;
                    numberOfSelectedItems++;
                }
            }
            if (numberOfSelectedItems > 0) {
                if (numberOfSelectedItems > 1) {
                    display = getString(R.string.caller_name_and_text_message_sender);
                } else {
                    display = mItems[selectedItemIndex].toString();
                }
            }
        } else {
            Log.e(TAG, "Error mCheckedItems is null.");
        }
        return display;
    }
}


