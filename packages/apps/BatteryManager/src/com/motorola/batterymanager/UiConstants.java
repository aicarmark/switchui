/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: UiConstants.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Feb 18, 2010	      A24178      Created file
 **********************************************************
 */

package com.motorola.batterymanager;

/**
 * @author A24178
 *
 */
// package-scope
class UiConstants {
    // Main Screen - Preset/Custom - matches spinner entry index
    public final static int UI_MAIN_PRESET_SCREEN = 0;
    public final static int UI_MAIN_CUSTOM_SCREEN = 1;
    
    // TODO - Find a better place for this
    // Key to read mode in case of ModeInfoUi
    public final static String UI_MODE_KEY = "whichMode";
    // Key for saving temp mode in BatteryProfileUi
    public final static String UI_BACKUP_MODE = "backupMode";
    public final static String UI_BACKUP_PRESET = "backupPreset";
    
    // Preference Ui - Keys
    public final static String UI_PREF_KEY_MAXBATT = "preset_maxbatt_saver";
    public final static String UI_PREF_KEY_NIGHTTIME = "preset_nighttime_saver";
    public final static String UI_PREF_KEY_PERFMODE = "preset_perfmode_saver";
    public final static String UI_PREF_KEY_CUSTOMS = "custom_saver";

    public final static String UI_PREF_KEY_BATTMODE = "pref_profile_settings";
}
