/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 *
 */

package com.motorola.contextual.pickers.actions;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.motorola.contextual.actions.Brightness;
import com.motorola.contextual.actions.Constants;

import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to select a brightness value to be set as part of Rule activation
 * <code><pre>
 * CLASS:
 *     Extends Activity.
 *
 * RESPONSIBILITIES:
 *     Shows a dialog with a brightness slider
 *     A preview of the brightness setting is shown when user moves the slider
 *     The selected brightness setting is returned in an intent to Rules Builder
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class BrightnessActivity extends MultiScreenPickerActivity implements Constants {

    private static final String TAG = TAG_PREFIX + BrightnessActivity.class.getSimpleName();
    private static final String HANDLER_THREAD_NAME = "BrightnessHandlerThread";

    private static final int SET_BRIGHTNESS_MODE = 1;
    private static final String BRIGHTNESS_MODE = "BrightnessMode";

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    /**
     * interface for defining constants for thresholds
     */
    private interface Thresholds {
        // cjd - can remove "public static final" from all these, static and final are assumed in an interface.
        public static final int MIN = 0;
        public static final int LOW = 30;
        public static final int MEDIUM = 70;
        public static final int HIGH = 99;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.brightness_title));
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
        mHandlerThread.start();
        Looper looper = mHandlerThread.getLooper();
        if (looper != null) {
            mHandler = new BrightnessHandler(looper, getApplicationContext());
        } else {
            mHandler = new BrightnessHandler(getApplicationContext());
        }
        // check if fragment already created
        if (getFragmentManager().findFragmentByTag(getString(R.string.brightness)) == null) {
            launchNextFragment(BrightnessPickerFragment.newInstance(mInputConfigs, mOutputConfigs), 
                    R.string.brightness, true);
        }
    }



    /** Returns the state based on the item that was checked
      *
      * @param checkedItem
      * @return state
      */
    protected final static String getConfig(int mode, int brightness) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_BRIGHTNESS, brightness);
        intent.putExtra(EXTRA_MODE, mode);
        if (LOG_INFO) Log.i(TAG, "getConfig : " +  intent.toUri(0));
        return intent.toUri(0);
    }

    /**
     * This method returns the appropriate threshold string on the basis of
     * current value of brightness
     *
     * @param brightness
     *            - current value of brightness
     * @return - appropriate threshold string on the basis of current value of
     *         brightness
     */
    public static String convertBrightnessToThresholdString(Context context, int brightness) {
        String thresholdString = null;
        float percentage = (brightness / (float) BACKLIGHT_RANGE) * 100;
        // cjd - seems like a risk if percentage is rounded slightly below 0, perhaps
        //      "if percentage <= Thresholds.MIN"
        if (percentage == Thresholds.MIN) {
            thresholdString = context.getString(R.string.min);
        } else if (percentage <= Thresholds.LOW) {
            thresholdString = context.getString(R.string.low);
        } else if (percentage <= Thresholds.MEDIUM) {
            thresholdString = context.getString(R.string.medium);
        } else if (percentage <= Thresholds.HIGH) {
            thresholdString = context.getString(R.string.high);
        } else {
            thresholdString = context.getString(R.string.max);
        }
        return thresholdString;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHandlerThread != null) {
            mHandlerThread.quit();
        }
    }

    /**
     * This method sends the message to handler thread's message queue for
     * setting the brightness mode.
     *
     * @param brightnessMode
     *            - the brightness mode
     */
    @SuppressWarnings("unused")
    private void setBrightnessMode(int brightnessMode) {
        mHandler.removeMessages(SET_BRIGHTNESS_MODE);
        Message msg = mHandler.obtainMessage(SET_BRIGHTNESS_MODE);
        Bundle data = new Bundle();
        data.putInt(BRIGHTNESS_MODE, brightnessMode);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }

    /**
     * This method sends the message to handler thread's message queue for
     * setting the brightness value
     *
     * @param brightnessValue
     *            - the brightness value
     */
    void setBrightnessValue(int brightnessValue) {
        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        if (brightnessValue < MINIMUM_BACKLIGHT) brightnessValue = MINIMUM_BACKLIGHT;
        lp.screenBrightness = (float)brightnessValue/MAXIMUM_BACKLIGHT;
        if (LOG_DEBUG) Log.d(TAG, "Setting screen brightness:"+lp.screenBrightness);
        window.setAttributes(lp);
    }

    /**
     * This class extends Handler and overrides handleMessage function for
     * setting the brightness value and brightness mode
     *
     * @author wkh346
     *
     */
    private class BrightnessHandler extends Handler {

        private Context mContext;

        /**
         * Constructor. Use this if you want the handleMessage to run in a
         * secondary thread
         *
         * @param looper
         *            - the looper obtained from mHandlerThread
         * @param context
         *            - the application's context
         */
        public BrightnessHandler(Looper looper, Context context) {
            super(looper);
            mContext = context;
        }

        /**
         * Constructor. handleMessage will be executed in primary UI thread
         *
         * @param context
         *            - the application's context
         */
        public BrightnessHandler(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case SET_BRIGHTNESS_MODE: {
                int brightnessMode = msg.getData().getInt(BRIGHTNESS_MODE);
                // cjd - this performs a Db operation in the main UI thread, I believe. That is,
                //    Settings.System.putInt() I believe performs a Db update or insert. I'm not sure if it's done in a thread, I doubt it.
                //    can you start a thread for this to be sure?
                setBrightnessMode(mContext, brightnessMode);
                break;
            }
            default: {
                super.handleMessage(msg);
            }
            }
        }

    }

    /**
     * Sets the brightness mode
     *
     * @param context
     * @param mode
     */
    public static void setBrightnessMode(Context context, int mode) {
        if (mode >= 0) {
            Settings.System.putInt(context.getContentResolver(),
                                   Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
        }
    }

    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        // returnValue is mOutputConfig intent
        if(returnValue instanceof Intent) {
            //set the appropriate values and return the result intent
            //to the rule builder
            Intent returnIntent = (Intent)returnValue;
            int mode = returnIntent.getIntExtra(EXTRA_MODE, Brightness.Mode.AUTOMATIC);
            int brightness = 0;

            switch (mode) {
            case Brightness.Mode.MANUAL:
                brightness = returnIntent.getIntExtra(EXTRA_BRIGHTNESS, 0);
                returnIntent.putExtra(EXTRA_CONFIG, getConfig(mode, brightness));
                returnIntent.putExtra(
                        EXTRA_DESCRIPTION,
                        convertBrightnessToThresholdString(getApplicationContext(),
                                brightness));
                break;
            case Brightness.Mode.AUTOMATIC:
                returnIntent.putExtra(EXTRA_CONFIG, getConfig(mode, 0));
                returnIntent.putExtra(EXTRA_DESCRIPTION, getString(R.string.auto_adjust));
                break;
            case Brightness.Mode.SMART:
                returnIntent.putExtra(EXTRA_CONFIG, getConfig(mode, 0));
                returnIntent.putExtra(EXTRA_DESCRIPTION, getString(R.string.smart_adjust));
                break;
            default:
                // cjd - should this return something other than "RESULT_OK"?
                Log.e(TAG, "Invalid brightness mode");
            }
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }
}
