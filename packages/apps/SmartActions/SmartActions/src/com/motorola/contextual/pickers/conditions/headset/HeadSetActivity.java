/*
 * @(#)Headset.java
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

package  com.motorola.contextual.pickers.conditions.headset;

import android.os.Bundle;

import com.motorola.contextual.pickers.conditions.DialogActivity;
import com.motorola.contextual.smartrules.R;

/**
 * Constants for Headset
 *
 */
interface HeadSetConstants {

    String NOT_CONNECTED = "Headset=NotConnected;Version=1.0";

    String CONNECTED = "Headset=Connected;Version=1.0";

    String HEADSET_STATE_MONITOR = "com.motorola.contextual.pickers.conditions.headset.HeadSetStateMonitor";

    String HEADSET_PERSISTENCE = "com.motorola.contextual.smartprofile.headset.persistence";

    String HEADSET_PUB_KEY = "com.motorola.contextual.smartprofile.headset";

    String NEW_HEADSET_CONFIG_PREFIX = "Headset=";

    String NEW_HEADSET_CONFIG_PREFIX_KEY = "Headset";

    String HEADSET_CONFIG_VERSION = "1.0";
}

/**
 * This class displays options for Headset precondition and allows the user to choose one
 * This also constructs the rule for Headset precondition and passes to the Smart Rules
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends DialogActivity which is a super class for the dialog based PreConditions
 *     Implements HeadsetModeConstants which contains constants for rule construction
 *
 * RESPONSIBILITIES:
 * This class displays options for Headset precondition and allows the user to choose one
 * This also constructs the rule for Headset precondition and passes to the Smart Rules
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public final class HeadSetActivity extends DialogActivity implements HeadSetConstants {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {

        // Update super class members
        mIcons = new int[] {R.drawable.ic_headset_w, R.drawable.ic_headset_disconnect_w};
        mItems = new String[] {getString(R.string.Connected), getString(R.string.NotConnected)};
        mDescription = new String[] {getString(R.string.headset_block_desc_Connected), getString(R.string.headset_block_desc_NotConnected)};
        mModeDescption = new String[] {CONNECTED, NOT_CONNECTED};

        mActionBarTitle = getString(R.string.headset_title);
        mTitle = this.getResources().getString(R.string.headset_prompt);
        mIcon = R.drawable.ic_dialog_headset;

        super.onCreate(savedInstanceState);
    }
}
