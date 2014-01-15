/*
 * @(#)WifiConnect.java
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

package  com.motorola.contextual.smartprofile.sensors.wificonnectsensor;

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

interface WifiConnectModeConstants {

    String NOT_CONNECTED = "NotConnected";

    String CONNECTED = "Connected";
}

/**
  * Rule related strings used for constructing rule
  */
interface WifiConnectRuleConstants {

    String WIFI_CONNECTIVITY_STATUS_DESCRIPTION = "Wifi Connectivity Status";

    // Uri used for editing the precondition
    String WIFI_CONNECT_URI_TO_FIRE_STRING = "#Intent;action=android.intent.action.EDIT;" +
            "component=com.motorola.contextual.smartrules/com.motorola.contextual.smartprofile.sensors.wificonnectsensor.WifiConnect;S.CURRENT_MODE=";

    // Virtual Sensor String
    String WIFI_CONNECTIVITY_VIRTUAL_SENSOR_STRING = "com.motorola.contextual.WifiConnectivity";

    // Rules to trigger Wi-Fi Connectivity DVS ON/OFF based on the android intents.
    String WIFI_CONNECTED = "#sensor;name=wifi;.*status=connected.*;end TRIGGERS #vsensor;name=";
    String WIFI_NOT_CONNECTED = "#sensor;name=wifi;.*status=disconnected.*;end TRIGGERS #vsensor;name=";
}

/**
 * This class displays options for WifiConnect precondition and allows the user to chose one
 * This also constructs the rule for WifiConnect precondition and passes to the Condition Builder
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends DialogActivity which is a super class for the dialog based PreConditions
 *     Implements WifiConnectModeConstants which contains constants for rule construction
 *
 * RESPONSIBILITIES:
 *      This class displays options for WifiConnect precondition and allows the user to chose one
 *      This also constructs the rule for WifiConnect precondition and passes to the Condition Builder
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     None
 *
 * </PRE></CODE>
 */
public final class WifiConnect extends  DialogActivity implements WifiConnectModeConstants {

    private final static String TAG = WifiConnect.class.getSimpleName();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Update super class members
       // mRuleConstructor = new WiFiConnectRuleConstructor();

        mItems = new String[] {getString(R.string.wifi_connect), getString(R.string.wifi_disconnect)};
        mDescription = new String[] {getString(R.string.wifi_connect), getString(R.string.wifi_disconnect)};
        mModeDescption = new String[] {CONNECTED, NOT_CONNECTED};

        mTitle = this.getResources().getString(R.string.wifi_connectivity_status);
        mIcon = R.drawable.ic_dialog_wifi;

        Intent incomingIntent = getIntent();
        if (incomingIntent != null) {
            String info = incomingIntent.getStringExtra(CURRENT_MODE);
            if(info != null) {
                if(LOG_DEBUG) Log.d(TAG, " Current mode " + incomingIntent.getStringExtra(CURRENT_MODE));
                // edit case

                mCheckedItem  = info.equals(mItems[0]) ? 0 : 1;
            }
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
     * edit(fire) in the Intent URI which is sent back to the Condition Builder.
     * This class is used to isolate rule construction functionality from the precondition UI functionality.
     *
     */
    /*private final class WiFiConnectRuleConstructor extends RuleConstructor implements WifiConnectRuleConstants,
        WifiConnectModeConstants {

        @Override
        protected final Intent constructResults() {

            StringBuilder sBuilder = new StringBuilder();

            // Rules to trigger Wi-Fi Connectivity DVS ON/OFF based on the android intents.
            // The rules are used in the xmlString below to be passed to the Condition Builder with "VSENSOR" tag.
            // WIFI_NOT_CONNECTED condition triggers the  com.motorola.contextual.WifiConnectivityNotConnected virtual
            // sensor when the user selected mode is NotConnected.
            // WIFI_CONNECTED condition triggers the com.motorola.contextual.WifiConnectivityConnected virtual sensor
            // when the user selected mode is Connected

            sBuilder.append(WIFI_NOT_CONNECTED)
            .append(WIFI_CONNECTIVITY_VIRTUAL_SENSOR_STRING)
            .append(mCurrentMode)
            .append(mCurrentMode.equals(NOT_CONNECTED) ? END_TRUE : END_FALSE);
            String rule1 = sBuilder.toString();

            sBuilder = new StringBuilder();
            sBuilder.append(WIFI_CONNECTED)
            .append(WIFI_CONNECTIVITY_VIRTUAL_SENSOR_STRING)
            .append(mCurrentMode)
            .append(mCurrentMode.equals(NOT_CONNECTED) ? END_FALSE : END_TRUE);
            String rule2 = sBuilder.toString();

            String ruleSet[] = {
                rule1,
                rule2
            };

            //Possible values of the sensor
            String possibleValues[] = {
                POSSIBLE_VALUE_TRUE,
                POSSIBLE_VALUE_FALSE
            };

            ConnectivityManager connManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = null;
            if(connManager != null)
                mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            String initialValue;

            if (mWifi != null && mWifi.isConnected()) {

                initialValue = mCurrentMode.equals(CONNECTED) ? INITIAL_VALUE_TRUE : INITIAL_VALUE_FALSE;
            } else {

                initialValue = mCurrentMode.equals(NOT_CONNECTED) ? INITIAL_VALUE_TRUE : INITIAL_VALUE_FALSE;
            }



            // Generate XML from rules.
            String xmlString = Util.generateDvsXmlString(mCurrentMode,
                               WIFI_CONNECTIVITY_VIRTUAL_SENSOR_STRING,
                               WIFI_CONNECTIVITY_STATUS_DESCRIPTION,
                               ruleSet,
                               initialValue,
                               possibleValues,
                               PERSISTENCY_VALUE_REBOOT);


            Intent returnIntent = new Intent();
            returnIntent.putExtra(EVENT_NAME, mName);
            returnIntent.putExtra(EVENT_DESC, mDescription);

            // Fire URI - used for editing the precondition
            sBuilder = new StringBuilder();
            sBuilder.append(WIFI_CONNECT_URI_TO_FIRE_STRING)
            .append(mCurrentModeUI)
            .append(END_STRING);

            String editUri = sBuilder.toString();
            returnIntent.putExtra(EDIT_URI,  editUri);
            returnIntent.putExtra(EVENT_TARGET_STATE, mCurrentMode);


            if(LOG_INFO) Log.i(TAG, " Final XML String : " + xmlString);

            returnIntent.putExtra(VSENSOR, xmlString);

            // Actual URI - Used for constructing the rule based DVS.
            sBuilder = new StringBuilder();
            sBuilder.append(SENSOR_NAME_START_STRING)
            .append(WIFI_CONNECTIVITY_VIRTUAL_SENSOR_STRING)
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
