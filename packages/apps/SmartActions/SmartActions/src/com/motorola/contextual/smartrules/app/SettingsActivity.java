/*
 * @(#)SettingsActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * csd053        2011/02/01 NA                Initial Version
 *
 */
package com.motorola.contextual.smartrules.app;


import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.util.PublisherFilterlist;
import com.motorola.contextual.smartrules.util.Util;
import com.motorola.contextual.virtualsensor.locationsensor.AppPreferences;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.MenuItem;
import android.app.ActionBar;
import android.content.Context;

/**
 * This class presents the UI to the user when they select Settings from the menu.
 *
 * <code><pre>
 * CLASS:
 *  extends PreferenceActivity
 *
 *  implements
 *  	Constants - for the constants used
 *
 * RESPONSIBILITIES:
 *  display the list to the user and handle the user selection by storing the preference 
 *  value to the shared preference.
 *
 * COLLABORATORS:
 *  None
 *
 * </pre></code>
 */
public class SettingsActivity extends PreferenceActivity implements Constants {

    private static final String TAG = SettingsActivity.class.getSimpleName();
    /** Identifier for Receive Suggestions check box preference */
    private CheckBoxPreference mNotifySuggestions;
    /** Identifier for Enabled Context Awareness check box preference */
    //private CheckBoxPreference mEnableCA;
    /** Identifier for prompt for important suggestions */
//    private CheckBoxPreference mPopupSuggestions;
    /** Identifier for prompt for important suggestions */
    private CheckBoxPreference mRuleStatusNotifications;
    /** Identifier for prompt for Motorola Location services consent */
    private CheckBoxPreference mMotLocServicesConsent;

    /** Application Context */
    private Context mContext;
    /** Flag to indicate enable CA */
    //private boolean mIsEnableCA;
    /** Flag to indicate receive suggestions */
    private boolean mIsReceiveSuggestions;
    /** Flag to indicate prompt for important suggestions */
//    private boolean mIsPopupSuggestions;
    /** Flag to indicate prompt for important suggestions */
    private boolean mIsRuleStatusNotifications;
    /** Flag to indicate prompt for Motorola Location consent */
    private boolean mIsMotLocServicesConsent = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.settings_preference);

        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
 		ab.show();
 		
        mRuleStatusNotifications = (CheckBoxPreference) findPreference(getString(R.string.rule_status_notifications));
        if(mRuleStatusNotifications != null) {
	        mIsRuleStatusNotifications = Util.getSharedPrefStateValue(this, RULE_STATUS_NOTIFICATIONS_PREF, TAG);
	        if(mIsRuleStatusNotifications)
	        	mRuleStatusNotifications.setChecked(true);
	
	        mRuleStatusNotifications.setOnPreferenceClickListener(
	        new Preference.OnPreferenceClickListener() {
	            public boolean onPreferenceClick(Preference preference) {
	                // just toggle the boolean value
	            	mIsRuleStatusNotifications = !mIsRuleStatusNotifications;
	                if(mIsRuleStatusNotifications) {
	                    if(LOG_DEBUG) Log.d(TAG, "Notify suggestions is true - save it to preference");
	                    Util.setSharedPrefStateValue(mContext, RULE_STATUS_NOTIFICATIONS_PREF, TAG, true);
	                    // Show the ongoing notification for the current active rules.
	                    Util.sendMessageToNotificationManager(mContext, 0);
	                    
	                    DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
	                    		TRUE, null, null,
	                            Constants.SMARTRULES_INTERNAL_DBG_MSG, null, SETTINGS_RULE,
	                            Constants.PACKAGE, Constants.PACKAGE);
	                }
	                else {
	                    if(LOG_DEBUG) Log.d(TAG, "Notify suggestions is false - save it to preference");
	                    Util.setSharedPrefStateValue(mContext, RULE_STATUS_NOTIFICATIONS_PREF, TAG, false);
	                    
	                    DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
	                    		FALSE, null, null,
	                            Constants.SMARTRULES_INTERNAL_DBG_MSG, null, SETTINGS_RULE,
	                            Constants.PACKAGE, Constants.PACKAGE);
	                    
	                    // Clear the ongoing notifications if any
	                    Util.clearOnGoingNotifications(mContext);
	                }
	                return true;
	            }
	        });
        }
        
        mNotifySuggestions = (CheckBoxPreference) findPreference(getString(R.string.suggestion_notifications));
        if(mNotifySuggestions != null) {
	        mIsReceiveSuggestions = Util.getSharedPrefStateValue(this, NOTIFY_SUGGESTIONS_PREF, TAG);
	        if(mIsReceiveSuggestions)
	            mNotifySuggestions.setChecked(true);
	
	        mNotifySuggestions.setOnPreferenceClickListener(
	        new Preference.OnPreferenceClickListener() {
	            public boolean onPreferenceClick(Preference preference) {
	                // just toggle the boolean value
	                mIsReceiveSuggestions = !mIsReceiveSuggestions;
	                if(mIsReceiveSuggestions) {
	                    if(LOG_DEBUG) Log.d(TAG, "Notify suggestions is true - save it to preference");
	                    Util.setSharedPrefStateValue(mContext, NOTIFY_SUGGESTIONS_PREF, TAG, true);
	                    DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
	                    		TRUE, null, null,
	                            Constants.SMARTRULES_INTERNAL_DBG_MSG, null, SETTINGS_SUGG,
	                            Constants.PACKAGE, Constants.PACKAGE);
	                }
	                else {
	                    if(LOG_DEBUG) Log.d(TAG, "Notify suggestions is false - save it to preference");
	                    Util.setSharedPrefStateValue(mContext, NOTIFY_SUGGESTIONS_PREF, TAG, false);
	                    DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
	                    		FALSE, null, null,
	                            Constants.SMARTRULES_INTERNAL_DBG_MSG, null, SETTINGS_SUGG,
	                            Constants.PACKAGE, Constants.PACKAGE);
	                }
	                return true;
	            }
	        });
        }

        mMotLocServicesConsent = (CheckBoxPreference) findPreference(getString(R.string.mot_loc_services));
        PublisherFilterlist instFilterList = PublisherFilterlist.getPublisherFilterlistInst();
        boolean isBlacklisted = instFilterList.isBlacklisted(mContext, LOCATION_TRIGGER_PUB_KEY);
        if(isBlacklisted && mMotLocServicesConsent != null){
            PreferenceScreen prefScrn = (PreferenceScreen) findPreference(getString(R.string.settings_prefs));
            prefScrn.removePreference(mMotLocServicesConsent);
        } else if(mMotLocServicesConsent != null) {
            if(Util.isMotLocConsentAvailable(mContext)){
                mIsMotLocServicesConsent = true;
                mMotLocServicesConsent.setChecked(true);
            }
            mMotLocServicesConsent.setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                    // just toggle the boolean value
                    mIsMotLocServicesConsent = !mIsMotLocServicesConsent;
                    if(mIsMotLocServicesConsent) {
                        if(LOG_DEBUG) Log.d(TAG, "Mot Loc services Consent is checked");
                        Util.setLocSharedPrefStateValue(mContext, AppPreferences.HAS_USER_LOC_CONSENT, TAG, LOC_CONSENT_SET);
	                    DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
	                    		TRUE, null, null,
	                            Constants.SMARTRULES_INTERNAL_DBG_MSG, null, SETTINGS_LOC,
	                            Constants.PACKAGE, Constants.PACKAGE);
                    }
                    else {
                        if(LOG_DEBUG) Log.d(TAG, "Mot Loc services Consent is NOT checked");
                        Util.setLocSharedPrefStateValue(mContext, AppPreferences.HAS_USER_LOC_CONSENT, TAG, LOC_CONSENT_NOTSET);
	                    DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
	                    		FALSE, null, null,
	                            Constants.SMARTRULES_INTERNAL_DBG_MSG, null, SETTINGS_LOC,
	                            Constants.PACKAGE, Constants.PACKAGE);
                    }
                    return true;
                }
            });
       }

/* No longer supported for V4.6 onwards

        mPopupSuggestions = (CheckBoxPreference) findPreference(getString(R.string.popup_suggestions));
        mIsPopupSuggestions = Util.getSharedPrefStateValue(this, POPUP_SUGGESTIONS_PREF, TAG);
        if(mIsPopupSuggestions)
        	mPopupSuggestions.setChecked(true);

        mPopupSuggestions.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                // just toggle the boolean value
            	mIsPopupSuggestions = !mIsPopupSuggestions;
                if(mIsPopupSuggestions) {
                    if(LOG_INFO) Log.i(TAG, "Prompt for important suggestions is true - save it to preference");
                    Util.setSharedPrefStateValue(mContext, POPUP_SUGGESTIONS_PREF, TAG, true);
                }
                else {
                    if(LOG_INFO) Log.i(TAG, "Prompt for important suggestions is false - save it to preference");
                    Util.setSharedPrefStateValue(mContext, POPUP_SUGGESTIONS_PREF, TAG, false);
                }
                return true;
            }
        });
*/
        /*
        mEnableCA = (CheckBoxPreference) findPreference(getString(R.string.ca_enabled));
        mIsEnableCA = Util.getSharedPrefStateValue(this, CONTEXT_AWARE_ENABLE_PREF, TAG);
        if(mIsEnableCA)
            mEnableCA.setChecked(true);

        mEnableCA.setOnPreferenceClickListener(
        new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                mIsEnableCA = !mIsEnableCA;
                if(mIsEnableCA) {
                    if(LOG_INFO) Log.i(TAG, "Enable CA is true - save it to preference");
                    Util.setSharedPrefStateValue(mContext, CONTEXT_AWARE_ENABLE_PREF, TAG, true);
                }
                else {
                    if(LOG_INFO) Log.i(TAG, "Enable CA is false - save it to preference");
                    Util.setSharedPrefStateValue(mContext, CONTEXT_AWARE_ENABLE_PREF, TAG, false);
                }
                return true;
            }
        });
		*/
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
