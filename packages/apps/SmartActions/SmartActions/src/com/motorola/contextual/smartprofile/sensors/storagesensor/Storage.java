/*
 * @(#)Storage.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491        2010/11/26 NA                Initial version
 * a18491        2011/2/18  NA                Incorporated first set of
 *                                            review comments
 */

package com.motorola.contextual.smartprofile.sensors.storagesensor;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartprofile.DialogActivity;
import com.motorola.contextual.smartrules.R;


/**
 * Mode related strings used for the rule are different from that of the
 * strings associated with the buttons. The below interface defines Mode related
 * strings.
 *
 */

interface StorageModeConstants {

    String NOT_LOW = "NotLow";

    String LOW = "Low";
}

/**
 * Rule related strings used for constructing rule
 */
interface StorageRuleConstants {

    // Storage sensor related strings
    String STORAGE_URI_TO_FIRE_STRING = "#Intent;action=android.intent.action.EDIT;" +
                                        "component=com.motorola.contextual.smartrules/com.motorola.contextual.smartprofile.sensors.storagesensor.Storage;S.CURRENT_MODE=";
    String STORAGE_VIRTUAL_SENSOR_STRING = "com.motorola.contextual.Storage";
    String STORAGE_LOW = "#Intent;action=android.intent.action.DEVICE_STORAGE_LOW;" +
                         ".*end  TRIGGERS #vsensor;name=";
    String STORAGE_NOT_LOW = "#Intent;action=android.intent.action.DEVICE_STORAGE_OK;" +
                             ".*end  TRIGGERS #vsensor;name=";
    String STORAGE_DESCRIPTION = "Storage";

}

/**
 * This class displays options for Storage precondition and allows the user to choose one
 * This also constructs the rule for Storage precondition and passes to the Smart Rules
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends DialogActivity which is a super class for the dialog based PreConditions
 *     Implements StorageModeConstants which contains constants for rule construction
 *
 * RESPONSIBILITIES:
 *     This class displays options for Storage precondition and allows the user to choose one
 *     This also constructs the rule for Storage precondition and passes to the Smart Rules
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class Storage extends DialogActivity implements StorageModeConstants {

    private final static String TAG = Storage.class.getSimpleName();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update super class members
        //mRuleConstructor = new StorageRuleConstructor();

        mItems = new String[] {getString(R.string.Low), getString(R.string.NotLow)};
        mDescription = new String[] {getString(R.string.Low), getString(R.string.NotLow)};
        mModeDescption = new String[] {LOW, NOT_LOW};

        mTitle = this.getResources().getString(R.string.storage);
        mIcon = R.drawable.ic_dialog_sd_card;

        Intent incomingIntent = getIntent();

        if (incomingIntent != null) {
        	String currentMode = incomingIntent.getStringExtra(CURRENT_MODE);
            if(LOG_INFO) Log.i(TAG, " Current mode " + currentMode);

            if(currentMode != null)
            	mCheckedItem  = currentMode.equals(mItems[0]) ? 0 : 1;
        } else {
            if(LOG_DEBUG) Log.d(TAG, " No Configured Current Mode ");
        }

        // Do a show Dialog, only when the activity is first created, don't do it for
        // orientation changes
        if (savedInstanceState  == null) super.showDialog();

    }

    /**
     * This is a helper class used for the rule construction.
     * This fills the name, description, rules in the XML format and the
     * edit(fire) in the Intent URI which is sent back to the Smart Rules.
     * This class is used to isolate rule construction functionality from the precondition UI functionality.
     *
     */
    /*private final class StorageRuleConstructor extends  RuleConstructor implements StorageModeConstants,
        StorageRuleConstants {

        @Override
        protected final Intent constructResults() {

            StringBuilder sBuilder = new StringBuilder();

            // Rules to trigger Storage DVS ON/OFF based on the android intents.
            // The rules are used in the xmlString below to be passed to the Smart Rules with "VSENSOR" tag.
            // STORAGE_NOT_LOW condition triggers the  com.motorola.contextual.StorageNotLow virtual sensor
            // when the user selected mode is NotLow.
            // STORAGE_LOW condition triggers the com.motorola.contextual.StorageLow virtual sensor
            // when the user selected mode is Low

            sBuilder.append(STORAGE_NOT_LOW)
            .append(STORAGE_VIRTUAL_SENSOR_STRING)
            .append(mCurrentMode)
            .append(mCurrentMode.equals(NOT_LOW) ? END_TRUE : END_FALSE);
            String rule1 = sBuilder.toString();

            sBuilder = new StringBuilder();
            sBuilder.append(STORAGE_LOW)
            .append(STORAGE_VIRTUAL_SENSOR_STRING)
            .append(mCurrentMode)
            .append(mCurrentMode.equals(LOW) ? END_TRUE : END_FALSE);
            String rule2 = sBuilder.toString();

            String ruleSet[] = {
                rule1,
                rule2
            };

            String possibleValues[] = {
                POSSIBLE_VALUE_TRUE,
                POSSIBLE_VALUE_FALSE
            };
            String initialValue = mCurrentMode.equals(NOT_LOW) ? INITIAL_VALUE_TRUE : INITIAL_VALUE_FALSE;
            // Generate XML from rules.
            String xmlString = Util.generateDvsXmlString(mCurrentMode,
                               STORAGE_VIRTUAL_SENSOR_STRING,
                               STORAGE_DESCRIPTION,
                               ruleSet,
                               initialValue,
                               possibleValues,
                               PERSISTENCY_VALUE_REBOOT);

            Intent returnIntent = new Intent();
            returnIntent.putExtra(EVENT_NAME, mName);
            returnIntent.putExtra(EVENT_DESC, mDescription);


            // Fire URI - used for editing the precondition
            sBuilder = new StringBuilder();
            sBuilder.append(STORAGE_URI_TO_FIRE_STRING)
            .append(mCurrentModeUI)
            .append(END_STRING);

            String editUri = sBuilder.toString();
            returnIntent.putExtra(EDIT_URI,  editUri);
            returnIntent.putExtra(EVENT_TARGET_STATE, mCurrentMode);

            returnIntent.putExtra(VSENSOR, xmlString);
            if(LOG_INFO) Log.i(TAG, " Final XML String : " + xmlString);

            // Actual URI - Used for constructing the rule based DVS.
            sBuilder = new StringBuilder();
            sBuilder.append(SENSOR_NAME_START_STRING)
            .append(STORAGE_VIRTUAL_SENSOR_STRING)
            .append(mCurrentMode)
            .append(SENSOR_NAME_END_STRING);

            String extraText = sBuilder.toString();
            returnIntent.putExtra(Intent.EXTRA_TEXT, extraText);
            if(LOG_DEBUG){
            	Log.d(TAG, " Extra text : " + extraText);
            	Log.d(TAG, " Rules : " + rule1 + " : " + rule2 );
            	Log.d(TAG, " Edit uri " + editUri);
            }
            return returnIntent;
        }

    } */

}

