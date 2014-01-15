/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19       Initial version
 * w04917        2012/07/16 IKCTXTAW-492      Add HIDDEN_LOCATION_RULE
 */

package com.motorola.contextual.virtualsensor.locationsensor;

import android.content.SharedPreferences;
import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;


/**
*<code><pre>
* CLASS:
*  To handle App setting and preference
*
* RESPONSIBILITIES:
*
* COLABORATORS:
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class AppPreferences {

    private static final String TAG = "LSAPP_PREF";
    public static final String LOC_CONSENT_PREF_NAME = "com.motorola.contextual.virtualsensor.locationsensor";

    public static final String POI = "poi";
    public static final String BACKGROUND_SCAN = "background_scan";
    public static final String HAS_LOCATION_RULE = "has_location_rule";
    public static final String HAS_HIDDEN_LOCATION_RULE = "has_hidden_location_rule";
    public static final String HAS_USER_LOC_CONSENT = "has_user_loc_consent";

    private LocationSensorApp mLSApp;
    private SharedPreferences mPref;

    public AppPreferences(LocationSensorApp lsapp) {
        mLSApp = lsapp;
        mPref = mLSApp.mContext.getSharedPreferences(Constants.PACKAGE_NAME, 0);
    }

    /**
     * Get the value of a key
     * @param key
     * @return
     */
    public String getString(String key) {
        return mPref.getString(key, null);
    }

    /**
     * Set the value of a key
     * @param key
     * @return
     */
    public void setString(String key, String value) {
        SharedPreferences.Editor editor = mPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * Get the value of a key from a different preference in the same application
     * adb shell cat /data/data/com.motorola.contextual.smartrules/shared_prefs/com.motorola.contextual.virtualsensor.locationsensor.xml
     *
     * <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
     * <map>
     * <string name="poi">Location:1331910178767</string>
     * <string name="background_scan">1</string>
     * </map>
     *
     * @param preferenceFileName, the file name under shared_prefs dir
     * @param key the key

     */
    public boolean getBooleanFromPref(String preferenceFileName, String key) {
        boolean value = false;
        SharedPreferences pref = mLSApp.mContext.getSharedPreferences(preferenceFileName, 0);
        if( pref != null) {
            value = pref.getBoolean(key, false);
        }
        LSAppLog.d(TAG, "getBoolean: " + preferenceFileName + " : " + key + " : " + value );
        return value;
    }
    /**
     * Get the value of a key from a different preference in the same application
     * adb shell cat /data/data/com.motorola.contextual.smartrules/shared_prefs/com.motorola.contextual.virtualsensor.locationsensor.xml
     *
     * <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
     * <map>
     * <string name="poi">Location:1331910178767</string>
     * <string name="background_scan">1</string>
     *
     * @param preferenceFileName, the file name under shared_prefs dir
     * @param key the key
     * </map>
     */
    public String getStringFromPref(String preferenceFileName, String key) {
        String value = null;
        SharedPreferences pref = mLSApp.mContext.getSharedPreferences(preferenceFileName, 0);
        if( pref != null) {
            value = pref.getString(key, null);
        }
        LSAppLog.d(TAG, "getString: " + preferenceFileName + " : " + key + " : " + value );
        return value;
    }
}
