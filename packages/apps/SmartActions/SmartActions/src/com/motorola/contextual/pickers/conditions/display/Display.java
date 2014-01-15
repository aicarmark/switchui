/*
 * @(#)Display.java
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
package  com.motorola.contextual.pickers.conditions.display;

import android.os.Bundle;

import com.motorola.contextual.pickers.conditions.DialogActivity;
import com.motorola.contextual.smartrules.R;

/**
 * Constants for Display
 *
 */
interface DisplayConstants {

    String OFF = "Display=OFF;Version=1.0";

    String ON = "Display=ON;Version=1.0";

    String DISPLAY_STATE_MONITOR = "com.motorola.contextual.pickers.conditions.display.DisplayStateMonitor";

    String DISPLAY_PERSISTENCE = "com.motorola.contextual.smartprofile.display.persistence";

    String DISPLAY_PUB_KEY = "com.motorola.contextual.smartprofile.display";

    String NEW_DISPLAY_CONFIG_PREFIX = "Display=";

    String NEW_DISPLAY_CONFIG_PREFIX_KEY = "Display";

    String DISPLAY_CONFIG_VERSION = "1.0";
}
/**
 * This class displays options for Display precondition and allows the user to chose one
 * This also constructs the rule for Display precondition and passes to the Condition Builder
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends DialogActivity which is a super class for the dialog based PreConditions
 *     Implements DisplayModeConstants which contains constants for rule construction
 *
 * RESPONSIBILITIES:
 *      This class displays options for Display precondition and allows the user to chose one
 *      This also constructs the rule for Display precondition and passes to the Condition Builder
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     None
 *
 * </PRE></CODE>
 */
public final class Display extends DialogActivity implements DisplayConstants  {

//    private final static String TAG = Display.class.getSimpleName();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {

        // Update super class members
        mIcons = new int[] {R.drawable.ic_display_w, R.drawable.ic_display_off_w};
        mItems = new String[] {getString(R.string.display_on), getString(R.string.display_off)};
        mDescription = new String[] {getString(R.string.on), getString(R.string.off)};
        mModeDescption = new String[] {ON, OFF};

        mActionBarTitle = getString(R.string.display_title);
        mTitle = this.getResources().getString(R.string.display_prompt);
        mIcon = R.drawable.ic_dialog_display;

        super.onCreate(savedInstanceState);
    }
}
