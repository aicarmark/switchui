/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts.activities;

import com.android.contacts.R;
import com.android.contacts.calllog.CallLogFragment;
import com.android.internal.telephony.ITelephony;
import com.google.common.annotations.VisibleForTesting;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

/**
 * Displays a list of call log entries.
 */
public class CallLogActivity extends Activity {
    private static final String TAG = "CallLogActivity";

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    private CallLogFragment mFragment;

    //MOTO Dialer Code - IKHSS6-583 - Start
    /**
     * Listener interface for Fragments accommodated in {@link ViewPager} enabling them to know
     * when it becomes visible or invisible inside the ViewPager.
     */
    public interface ViewVisibilityListener {
        public void onVisibilityChanged(boolean visible);
    }
    //MOTO Dialer Code - IKHSS6-583 - End

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.call_log_activity);

        // Typing here goes to the dialer
        setDefaultKeyMode(DEFAULT_KEYS_DIALER);

        mFragment = (CallLogFragment) getFragmentManager().findFragmentById(
                R.id.call_log_fragment);

        //MOT Dialer Code Start
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME|ActionBar.DISPLAY_SHOW_TITLE,
                                           ActionBar.DISPLAY_SHOW_HOME|ActionBar.DISPLAY_SHOW_TITLE);
        //MOT Dialer Code End
    }

    @VisibleForTesting
    /*package*/ CallLogFragment getFragment() {
        return mFragment;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                long callPressDiff = SystemClock.uptimeMillis() - event.getDownTime();
                if (callPressDiff >= ViewConfiguration.getLongPressTimeout()) {
                    // Launch voice dialer
                    Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                    }
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                try {
                    ITelephony phone = ITelephony.Stub.asInterface(
                            ServiceManager.checkService("phone"));
                    if (phone != null && !phone.isIdle()) {
                        // Let the super class handle it
                        break;
                    }
                } catch (RemoteException re) {
                    // Fall through and try to call the contact
                }

                mFragment.callSelectedEntry();
                return true;
        }
        return super.onKeyUp(keyCode, event);
    }
    //MOTO Dialer Code - IKHSS6-583 - Start
    @Override
    public void onResume() {
        super.onResume();
        if (mFragment instanceof ViewVisibilityListener) {
            ((ViewVisibilityListener) mFragment).onVisibilityChanged(true);
            if(DBG) Log.d(TAG, "onResume, onVisibilityChanged to true.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFragment instanceof ViewVisibilityListener) {
            ((ViewVisibilityListener) mFragment).onVisibilityChanged(false);
            if(DBG) Log.d(TAG, "onPause, onVisibilityChanged to false.");
        }
    }
    //MOTO Dialer Code - IKHSS6-583 - End
}
