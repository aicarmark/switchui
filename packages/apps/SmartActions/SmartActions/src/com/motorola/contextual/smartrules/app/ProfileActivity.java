/*
 * @(#)ProfileActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * csd053        2011/03/07 NA                Initial Version
 *
 */
package com.motorola.contextual.smartrules.app;


import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.util.PublisherFilterlist;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import android.app.ActionBar;
import android.content.Intent;

/**
 * This class presents the UI to the user when they select Profile from the menu.
 *
 * <code><pre>
 * CLASS:
 *  extends PreferenceActivity
 *
 *  implements
 *  	Constants - for the constants used
 *
 * RESPONSIBILITIES:
 *  display the list to the user and handle the user selection by launching the right activity
 *
 * COLLABORATORS:
 *  None
 *
 * </pre></code>
 */
public class ProfileActivity extends PreferenceActivity implements Constants {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    private static final String SMART_PROFILE_LAUNCH_INTENT = "com.motorola.contextual.smartprofilelaunch";
    private static final String TIMEFRAMES_LAUNCH_INTENT = "com.motorola.contextual.timeframeslist";

    /** Identifier for the Location preference */
    private Preference mLocations;
    /** Identifier for the Timeframes preference */
    private Preference mTimeframes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.profile_preference);

        mLocations = findPreference(getString(R.string.locations_title));
        mTimeframes = findPreference(getString(R.string.timeframes));

       if(PublisherFilterlist.getPublisherFilterlistInst()
               .isBlacklisted(this, LOCATION_TRIGGER_PUB_KEY)) {
            if(LOG_DEBUG) Log.d(TAG, "Hide location preference as the ro is set to true");
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            if(preferenceScreen == null)
                Log.e(TAG, "getPreferenceScreen() call returned null, cannot hide locations");
            else
                preferenceScreen.removePreference(mLocations);
       }

       ActionBar ab = getActionBar();
       ab.setDisplayHomeAsUpEnabled(true);
       ab.show();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        if(preference.equals(mLocations)) {
            if(LOG_DEBUG) Log.d(TAG, "Locations selected");
            Intent intent = new Intent();
            intent.setAction(SMART_PROFILE_LAUNCH_INTENT);

            DebugTable.writeToDebugViewer(getBaseContext(), DebugTable.Direction.OUT,
                    null, null, null,
                    Constants.SMARTRULES_INTERNAL_DBG_MSG, null, MYPROFILE_LOC,
                    Constants.PACKAGE, Constants.PACKAGE);

            startActivity(intent);
        }
        else if(preference.equals(mTimeframes)) {
            if(LOG_DEBUG) Log.d(TAG, "Timeframes selected");
            Intent intent = new Intent();
            intent.setAction(TIMEFRAMES_LAUNCH_INTENT);

            DebugTable.writeToDebugViewer(getBaseContext(), DebugTable.Direction.OUT,
                    null, null, null,
                    Constants.SMARTRULES_INTERNAL_DBG_MSG, null, MYPROFILE_TIMEFRAME,
                    Constants.PACKAGE, Constants.PACKAGE);

            startActivity(intent);
        }
        else
            Log.e(TAG, "Not launched for location or timeframes preferences");
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /** onOptionsItemSelected()
     *  handles the back press of icon in the ICS action bar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
}