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

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.dialpad.DialpadFragment;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

/**
 * Activity that displays a twelve-key phone dialpad.
 * This is just a simple container around DialpadFragment.
 * @see DialpadFragment
 */
public class DialpadActivity extends Activity {
    private static final String TAG = "DialpadActivity";

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    private DialpadFragment mFragment;

    //MOTO Dialer Code - IKHSS7-8639 - Start
    private boolean isChinaCarMode = false;
    private static final String IS_CHINA_CAR_MODE = "CHINA_CAR_MODE";

    /**
      * Listener interface for Fragments accommodated in {@link ViewPager} enabling them to know
      * when it becomes visible or invisible inside the ViewPager.
      */
    public interface ViewVisibilityListener {
        public void onVisibilityChanged(boolean visible);
    }
    //MOTO Dialer Code - IKHSS7-8639 - End

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        isChinaCarMode = ContactsUtils.isCarDockMode(getIntent());
        if (!isChinaCarMode && null!=icicle && icicle.containsKey(IS_CHINA_CAR_MODE)) {
            isChinaCarMode = icicle.getBoolean(IS_CHINA_CAR_MODE);
        }
        if (isChinaCarMode) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }

        Resources r = getResources();
        // Do not show title in the case the device is in carmode.
        if (((r.getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK) ==
                Configuration.UI_MODE_TYPE_CAR) || isChinaCarMode) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            //MOT Dialer Code Start
            getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME|ActionBar.DISPLAY_SHOW_TITLE,
                                ActionBar.DISPLAY_SHOW_HOME|ActionBar.DISPLAY_SHOW_TITLE);
            //MOT Dialer Code End
        }
        // Have the WindowManager filter out touch events that are "too fat".
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
        setContentView(R.layout.dialpad_activity);
        mFragment = (DialpadFragment) getFragmentManager().findFragmentById(
                R.id.dialpad_fragment);

    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        setIntent(newIntent);
        mFragment.configureScreenFromIntent(newIntent);
    }

    public DialpadFragment getFragment() {
        return mFragment;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            // Hide soft keyboard, if visible (it's fugly over button dialer).
            // The only known case where this will be true is when launching the dialer with
            // ACTION_DIAL via a soft keyboard.  we dismiss it here because we don't
            // have a window token yet in onCreate / onNewIntent
            InputMethodManager inputMethodManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(
                    mFragment.getDigitsWidget().getWindowToken(), 0);
        }
    }

// MOT Dialer Start - comment out: implement these function in fragment.
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_CALL: {
//                long callPressDiff = SystemClock.uptimeMillis() - event.getDownTime();
//                if (callPressDiff >= ViewConfiguration.getLongPressTimeout()) {
//                    // Launch voice dialer
//                    Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    try {
//                        startActivity(intent);
//                    } catch (ActivityNotFoundException e) {
//                        Log.w(TAG, "Failed to launch voice dialer: " + e);
//                    }
//                }
//                return true;
//            }
//            //not support lanuch voicemail by long press hard key 1
//            case KeyEvent.KEYCODE_1: {
//                long timeDiff = SystemClock.uptimeMillis() - event.getDownTime();
//                if (timeDiff >= ViewConfiguration.getLongPressTimeout()) {
//                    // Long press detected, call voice mail
//                    mFragment.callVoicemail();
//                }
//                return true;
//            }
//        }
//        return super.onKeyDown(keyCode, event);
//    }
//
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_CALL: {
//                mFragment.dialButtonPressed();
//                return true;
//            }
//        }
//        return super.onKeyUp(keyCode, event);
//    }
// MOT Dialer End

    //MOTO Dialer Code -IKHSS7-8639 - Start
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
    //MOTO Dialer Code - IKHSS7-8639 - End

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null){
            outState.putBoolean(IS_CHINA_CAR_MODE, isChinaCarMode);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle icicle) {
        super.onRestoreInstanceState(icicle);
        if (icicle != null) {
            isChinaCarMode = icicle.getBoolean(IS_CHINA_CAR_MODE);
        }
    }

    public boolean isChinaCarMode() {
        return isChinaCarMode;
    }

}
