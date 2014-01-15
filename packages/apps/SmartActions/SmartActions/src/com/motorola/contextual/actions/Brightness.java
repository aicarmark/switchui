/*
 * @(#)Brightness.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2011/04/23  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.motorola.contextual.pickers.actions.BrightnessActivity;
import com.motorola.contextual.smartrules.R;

import java.util.ArrayList;
import java.util.List;



/**
 * This class implements the FrameworkAction for Brightness <code><pre>
 * CLASS:
 *     implements FrameworkAction
 *
 * RESPONSIBILITIES:
 *     If Smart Actions FW is old then this class is used to handle commands
 *     that cannot be handled by old framework
 *
 * COLLABORATORS:
 *     None
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class Brightness extends FrameworkAction {

    private static final String TAG = TAG_PREFIX + Brightness.class.getSimpleName();

    private static final int INVALID_BRIGHTNESS_VALUE = 0;
    
    public interface Mode {
        /** Manual Brightness Setting Mode. */
        public static final int MANUAL = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
        /** Automatic Brightness Setting Mode. */
        public static final int AUTOMATIC = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        /** Smart Brightness Setting Mode. */
        public static final int SMART = AUTOMATIC + 1;
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.brightness);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        int mode = configIntent.getIntExtra(EXTRA_MODE, AUTOMATIC_NOT_SUPPORTED);
        int brightness = configIntent.getIntExtra(EXTRA_BRIGHTNESS, INVALID_BRIGHTNESS_VALUE);
       
        String description = null;
        if (mode == Mode.SMART)
            description = context.getString(R.string.smart_adjust);
        else if (mode == Mode.AUTOMATIC)
            description = context.getString(R.string.auto_adjust);
        else
          description = BrightnessActivity.convertBrightnessToThresholdString(context, (brightness - MINIMUM_BACKLIGHT));
       
        if (LOG_INFO) Log.i(TAG, "getDescription : " + description);
        return description;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        int mode = configIntent.getIntExtra(EXTRA_MODE, AUTOMATIC_NOT_SUPPORTED);
        if (mode == Mode.SMART) {
            boolean isDisplayCurveSupported = Persistence.retrieveBooleanValue(
                    context, DISPLAY_CURVE_SUPPORT_KEY);
             //default to automatic if smart is not supported
             if (!isDisplayCurveSupported) mode = Mode.AUTOMATIC;
        }
        return getConfig(mode, 
                         configIntent.getIntExtra(EXTRA_BRIGHTNESS, INVALID_BRIGHTNESS_VALUE));
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
    String getDefaultSetting(Context context, Intent defaultIntent) {
        return getUpdatedConfig(context, defaultIntent);
    }

    @Override
    public boolean validateConfig (Intent configIntent) {
        int mode = configIntent.getIntExtra(EXTRA_MODE, -2);
        boolean isValid = mode != -2;

        if (isValid && mode == Mode.MANUAL) {
            //Brightness value is needed only if the mode is manual
            int brightness = configIntent.getIntExtra(EXTRA_BRIGHTNESS, -1);
            isValid = brightness != -1;
        }

        return isValid;
    }

    /** Returns the state based on the item that was checked
     *
     * @param checkedItem
     * @return state
     */
    public static final String getConfig(int mode, int brightness) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_BRIGHTNESS, brightness);
        intent.putExtra(EXTRA_MODE, mode);
        if (LOG_INFO) Log.i(TAG, "getConfig : " +  intent.toUri(0));
        return intent.toUri(0);
    }

}
