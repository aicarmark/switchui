/**
 * Copyright (C) 2009, Motorola, Inc,
 * All Rights Reserved
 * Class name: DisplayControl.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 11-06-09       A24178       Created file
 *                -Ashok
 **********************************************************
 */

package com.motorola.batterymanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import android.os.RemoteException;
//import android.os.IHardwareService;
import android.os.IPowerManager;
import android.os.ServiceManager;

// package-scope
class DisplayControl {

    private final static String LOG_TAG = "PowerProfileDispCtrl";

    private final static int DISP_STATE_IDLE = 0;
    private final static int DISP_STATE_SMART = 1;
    private final static int DISP_STATE_MAX = 2;

    public final static int OFFSET = 10;
    public final static int MAX_BRIGHTNESS = android.os.PowerManager.BRIGHTNESS_ON;
    private final static int MIN_BRIGHTNESS = android.os.PowerManager.BRIGHTNESS_DIM + OFFSET;
    //public final static int MAX_BRIGHTNESS = 255;
    //private final static int MIN_BRIGHTNESS = 0 + OFFSET;

    private static int sCurrDispState = DISP_STATE_IDLE;
    private static boolean sInChargingMode = false;
    private static boolean isSmartStateApplied = false;
    private static SharedPreferences sPreferences;

    private static void initPreferences(Context ctx) {
        if(sPreferences == null) {
            sPreferences = ctx.getSharedPreferences(BatteryProfile.OPTIONS_STORE,
                    Context.MODE_PRIVATE);
        }
    }

    public static boolean smartStateApplied() {
        return isSmartStateApplied;
    }

    public static void startLowPowerMode(Context ctx, int newMode) {

        initPreferences(ctx);

        int userval = sPreferences.getInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS,
                BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS);
        int currVal = getBrightness(ctx);

        Utils.Log.d(LOG_TAG, "StartLowPower: (mode,curr,user)" + newMode + "," +
                currVal + "," + userval);

        if(newMode == BatteryProfile.OPTION_PRESET_MAXSAVER_MODE) {
            int maxbatval;
            if(sCurrDispState == DISP_STATE_IDLE) {
                maxbatval = sPreferences.getInt(
                        BatteryProfile.KEY_OPTION_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS,
                        BatteryProfile.DEFAULT_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS);

                if(currVal >= maxbatval) {
                    if(userval == BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS) {
                        SharedPreferences.Editor myEditor = sPreferences.edit();
                        myEditor.putInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS, currVal);
                        myEditor.commit();
                    }

                    setBrightness(maxbatval, ctx);
                }else {
                    /*
                     * This is a highly unlikely case and we should get in here
                     * only if the user had set the normal brightness itself to
                     * a very low value, still lets handle it gracefully by pulling
                     * down our max val to this user value
                     */
                    SharedPreferences.Editor myEditor = sPreferences.edit();
                    myEditor.putInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS, currVal);
                    myEditor.putInt(BatteryProfile.KEY_OPTION_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS,
                            currVal);
                    myEditor.commit();
                }
            }else {

                if(userval == BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS) {
                    SharedPreferences.Editor myEditor = sPreferences.edit();
                    myEditor.putInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS, currVal);
                    myEditor.commit();
                }

                maxbatval = sPreferences.getInt(
                        BatteryProfile.KEY_OPTION_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS,
                        BatteryProfile.DEFAULT_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS);
                setBrightness(maxbatval, ctx);
            }

            Utils.Log.d(LOG_TAG, "StartLowPower:end: max:" + maxbatval);
        }else if (newMode == BatteryProfile.OPTION_PRESET_NTSAVER_MODE) {
            try {
                int cap = Integer.parseInt(Utils.getBattCapacity());

                if(cap <= BatteryProfile.SMARTBATT_CAPACITY_START_TRIGGER) {
                    if(sCurrDispState == DISP_STATE_MAX) {
                        int smartbatval = sPreferences.getInt(
                                BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                                BatteryProfile.DEFAULT_DISPLAY_BRIGHTNESS);
                        setBrightness(smartbatval, ctx);
                        isSmartStateApplied = true;

                        Utils.Log.d(LOG_TAG, "StartLowPower:end: smart:" +
                                smartbatval);
                    }else if(sCurrDispState == DISP_STATE_IDLE) {
                        runCurrentMode(ctx);
                    }
                }else {
                    if(sCurrDispState == DISP_STATE_MAX) {
                        exitLowPowerMode(ctx, 0); //Last argument is not used
                    }
                    isSmartStateApplied = false;

                    Utils.Log.d(LOG_TAG, "StartLowPower:end: smart: Batt capacity above trigger");
                }
            }catch (NumberFormatException nfEx) {
                Utils.Log.d(LOG_TAG, "StartLowPower:start: smart: Reading batt capacity failed");
            }
        }
    }

    public static void exitLowPowerMode(Context ctx, int newMode) {

        int currbright = getBrightness(ctx);

        initPreferences(ctx);

        int userval = sPreferences.getInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS,
                BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS);
        int batval = MAX_BRIGHTNESS;

        if(sCurrDispState == DISP_STATE_MAX) {
            batval = sPreferences.getInt(
                    BatteryProfile.KEY_OPTION_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS,
                    BatteryProfile.DEFAULT_PRESET_MAXSAVER_DISPLAY_BRIGHTNESS);
        }else if(sCurrDispState == DISP_STATE_SMART) {
            batval = sPreferences.getInt(BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                    BatteryProfile.DEFAULT_DISPLAY_BRIGHTNESS);
        }

        if((userval != BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS)
                && (currbright == batval)) {
            setBrightness(userval, ctx);
        }

        SharedPreferences.Editor myEditor = sPreferences.edit();
        myEditor.putInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS,
                BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS);
        myEditor.commit();

        Utils.Log.d(LOG_TAG, "ExitLowPowerMode:end: (user,bat,curr):" +
                userval + "," + batval + "," + currbright);
    }

    public static void runCurrentMode(Context ctx) {
        initPreferences(ctx);

        SharedPreferences.Editor dispPrefeditor = sPreferences.edit();

        int smartbatval = sPreferences.getInt(
                BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                BatteryProfile.DEFAULT_DISPLAY_BRIGHTNESS);
        int currVal = getBrightness(ctx);

        if(!isSmartStateApplied) {
            dispPrefeditor.putInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS, currVal);
            setBrightness(smartbatval, ctx);
            isSmartStateApplied = true;

            Utils.Log.d(LOG_TAG, "runMode: smart+: curr,smart:" +
                    currVal + "," + smartbatval);
        }else {
            int userval = sPreferences.getInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS,
                    BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS);

            if((userval != BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS)
                    && (currVal == smartbatval)) {
                setBrightness(userval, ctx);
            }

            dispPrefeditor.putInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS,
                    BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS);
            isSmartStateApplied = false;

            Utils.Log.d(LOG_TAG, "runMode: smart-: smart,user:" +
                    smartbatval + "," + userval);
        }

        dispPrefeditor.commit();
    }

    /*
     * To decide whether to switch to smart brightness do the following:
     * 1. Check if we are below the start trigger
     * 2. Check if we have already applied the smart display, if yes, exit
     * 3. Check if we are charging, if yes, lets wait for the next indication to see
     *    if batt improves, exit
     * 4. Apply the smart mode brightness and flag appropriately
     *
     * To decide when to switch to user display brightness
     * 1. Check if we are above the stop trigger
     * 2. Check if we have actually applied the smart display, if not, exit
     * 3. Apply the user brightness and flag appropriately
     */
    public static void noteCapacity(int capacity, boolean charging, Context ctx) {
        Utils.Log.d(LOG_TAG, "noteCap: " + capacity + "," +
                charging + "," + sCurrDispState);
        if(sCurrDispState == DISP_STATE_SMART) {
            if(capacity <= BatteryProfile.SMARTBATT_CAPACITY_START_TRIGGER) {
                if(!isSmartStateApplied && !charging) {
                    /*
                     * Power cycled in smart batt mode logic, the static variable is not
                     * set even though we are at smart mode brightness
                     */
                    initPreferences(ctx);

                    int smartbatval = sPreferences.getInt(
                            BatteryProfile.KEY_OPTION_CUSTOM_DISPLAY_BRIGHTNESS,
                            BatteryProfile.DEFAULT_DISPLAY_BRIGHTNESS);
                    int currVal = getBrightness(ctx);

                    if(currVal == smartbatval) {
                        isSmartStateApplied = true;
                        return;
                    }
                    runCurrentMode(ctx);
                }
            }else if(capacity > BatteryProfile.SMARTBATT_CAPACITY_STOP_TRIGGER) {
                if(isSmartStateApplied) {
                    runCurrentMode(ctx);
                }
            }
        }
    }

    private static int getBrightness(Context ctx) {
        int val = MAX_BRIGHTNESS;
        try {
            val = Settings.System.getInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        }catch(SettingNotFoundException snfEx) {
            Utils.Log.e(LOG_TAG, "getBrightness:Could not read brightness setting...");
        }
        return val;
    }

    // This will just update the display brightness and not modify the 
    // system setting - to be used while in the Custom Ui screen 
    // when the brightness is adjusted
    public static void changeBrightness(int newVal, Context ctx) {
         try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                   ServiceManager.getService("power"));
            if (power != null) {
               power.setBacklightBrightness(newVal);
            }
        }catch(RemoteException doEx) {
            Utils.Log.e(LOG_TAG, "setBrightness:failed to set disp brightness");
        }
    }

    public static void setBrightness(int newVal, Context ctx) {

        Settings.System.putInt(ctx.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,
                newVal);

         try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                   ServiceManager.getService("power"));
            if (power != null) {
               power.setBacklightBrightness(newVal);
            }
        }catch(RemoteException doEx) {
            Utils.Log.e(LOG_TAG, "setBrightness:failed to set disp brightness");
        }
    }

    public static void noteModeChange(Context ctx, int newMode) {
        if(sInChargingMode) {
            Utils.Log.d(LOG_TAG, "Mode Change: Charging:ignoring...");
            return;
        }

        Utils.Log.d(LOG_TAG, "Mode Change:Start: (dispState,newMode)" +
                                sCurrDispState + "," + newMode);

        switch(sCurrDispState) {
        case DISP_STATE_IDLE:
            switch(newMode) {
            case BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE:
                /*
                 * Should never hit this
                 */
                break;
            case BatteryProfile.OPTION_PRESET_NTSAVER_MODE:
                // STUB
                startLowPowerMode(ctx, newMode);
                sCurrDispState = DISP_STATE_SMART;
                break;
            case BatteryProfile.OPTION_PRESET_MAXSAVER_MODE:
                startLowPowerMode(ctx, newMode);
                sCurrDispState = DISP_STATE_MAX;
                break;
            }
            break;

        case DISP_STATE_SMART:
            switch(newMode) {
            case BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE:
                exitLowPowerMode(ctx, newMode);
                isSmartStateApplied = false;
                sCurrDispState = DISP_STATE_IDLE;
                break;
            case BatteryProfile.OPTION_PRESET_NTSAVER_MODE:
                /*
                 * Should never hit this
                 */
                break;
            case BatteryProfile.OPTION_PRESET_MAXSAVER_MODE:
                startLowPowerMode(ctx, newMode);
                isSmartStateApplied = false;
                sCurrDispState = DISP_STATE_MAX;
                break;
            }
            break;
        case DISP_STATE_MAX:
            switch(newMode) {
            case BatteryProfile.OPTION_PRESET_PERFORMANCE_MODE:
                exitLowPowerMode(ctx, newMode);
                sCurrDispState = DISP_STATE_IDLE;
                break;
            case BatteryProfile.OPTION_PRESET_NTSAVER_MODE:
                //exitLowPowerMode(window, ctx, BatteryProfile.MAX_PERF_MODE);
                //currDispState = DISP_STATE_IDLE;
                // STUB
                startLowPowerMode(ctx, newMode);
                sCurrDispState = DISP_STATE_SMART;
                break;
            case BatteryProfile.OPTION_PRESET_MAXSAVER_MODE:
                /*
                 * Should never hit this
                 */
                break;
            }
            break;
        }
        Utils.Log.d(LOG_TAG, "Mode Change:End: (dispState,newMode)" +
                sCurrDispState + "," + newMode);
    }

    public static void noteChargingMode(boolean isCharging) {
        sInChargingMode = isCharging;
    }

    public static void noteBrightnessChange(int newVal, int oldVal, Context ctx) {
        //if(sInChargingMode || (sCurrDispState == DISP_STATE_IDLE)) {
        // Ok..now this is for all the roundabout stuff to get around
        // having brightness control accessible for all modes
        if(sInChargingMode || (sCurrDispState != DISP_STATE_SMART)) {
            if(oldVal != -1) {
                Utils.Log.d(LOG_TAG, "noteBrightness:charging: restoring oldVal");
                setBrightness(oldVal, ctx);
            }
            return;
        }

	 Utils.Log.d(LOG_TAG, "noteBrightness sCurrDispState = " + sCurrDispState + "  isSmartStateApplied = " +isSmartStateApplied);

        if(sCurrDispState == DISP_STATE_MAX) {
            setBrightness(newVal, ctx);
        }else if(sCurrDispState == DISP_STATE_SMART) {
            try {
                if(!isSmartStateApplied) {
                    int cap = Integer.parseInt(Utils.getBattCapacity());
			 Utils.Log.d(LOG_TAG, "noteBrightness cap = " + cap );

                    if(cap <= BatteryProfile.SMARTBATT_CAPACITY_START_TRIGGER) {
                        initPreferences(ctx);
                        SharedPreferences.Editor myEditor = sPreferences.edit();

                        if(sPreferences.getInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS,
                                BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS)
                                == BatteryProfile.DEFAULT_BACKUP_DISPLAY_BRIGHTNESS) {
                            myEditor.putInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS, oldVal);
                            myEditor.commit();
                        }

                        setBrightness(newVal, ctx);
                        isSmartStateApplied = true;

                        Utils.Log.d(LOG_TAG, "noteBrightness:smart: new brightness" + newVal);
                    }else {
                        setBrightness(oldVal, ctx);
                        Utils.Log.d(LOG_TAG, "noteBrightness:smart: Batt capacity above trigger, " +
                                "restoring oldVal:" + oldVal);
                    }
                }else {
                    setBrightness(newVal, ctx);
                }
            }catch(NumberFormatException nfEx) {
                Utils.Log.d(LOG_TAG, "noteBrightness: smart: Reading batt capacity failed");
                return;
            }
        }
    }

    public static int getBackupBrightness(Context ctx) {
        initPreferences(ctx);

        int brightness = sPreferences.getInt(BatteryProfile.KEY_BACKUP_DISPLAY_BRIGHTNESS,
                -1);
        if(brightness == -1) {
            brightness = getBrightness(ctx);
        }
        return brightness;
    }
}
