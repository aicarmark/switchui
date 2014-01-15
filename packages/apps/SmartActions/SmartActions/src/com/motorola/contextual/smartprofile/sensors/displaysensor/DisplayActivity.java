/*
 * @(#)DisplayActivity.java
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
 *                                            review comments.
 */
package  com.motorola.contextual.smartprofile.sensors.displaysensor;


import android.os.Bundle;

import com.motorola.contextual.smartprofile.DialogActivity;
import com.motorola.contextual.smartrules.R;



/**
 * Constants for Display
 *
 */
interface DisplayConstants {

    String OFF = "Display=OFF;Version=1.0";

    String ON = "Display=ON;Version=1.0";

    String DISPLAY_STATE_MONITOR = "com.motorola.contextual.smartprofile.sensors.displaysensor.DisplayStateMonitor";

    String DISPLAY_PERSISTENCE = "com.motorola.contextual.smartprofile.display.persistence";

    String DISPLAY_PUB_KEY = "com.motorola.contextual.smartprofile.display";

    String NEW_DISPLAY_CONFIG_PREFIX = "Display=";
    
    String NEW_DISPLAY_CONFIG_PREFIX_KEY = "Display";

    String DISPLAY_CONFIG_VERSION = "1.0";
}


/**
 * This class displays options for Display precondition and allows the user to chose one
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends DialogActivity which is a super class for the dialog based PreConditions
 *     Implements DisplayConstants which contains constants for rule construction
 *
 * RESPONSIBILITIES:
 *      This class displays options for Display precondition and allows the user to chose one
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     None
 *
 * </PRE></CODE>
 */
public final class DisplayActivity extends DialogActivity implements DisplayConstants  {

    @SuppressWarnings("unused")
	private final static String TAG = DisplayActivity.class.getSimpleName();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mItems = new String[] {getString(R.string.on), getString(R.string.off)};
        mDescription = mItems;
        mModeDescption = new String[] {ON, OFF};

        mTitle = getString(R.string.display);
        mIcon = R.drawable.ic_dialog_display;

        // Do a show Dialog, only when the activity is first created, don't do it for
        // orientation changes
        if (savedInstanceState  == null) super.showDialog();
    }




}
