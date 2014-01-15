/*
 * @(#)BtConnectionChooserActivity.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * tqrb48       2012/06/12                    Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.dock;

import android.os.Bundle;

import com.motorola.contextual.pickers.conditions.DialogActivity;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.R;


/**
 * Constants for Dock
 *
 */
interface DockConstants {

    String DOCK_ANY = "Dock=AnyDock;Version=1.0";

    String DOCK_DESK = "Dock=DeskDock;Version=1.0";

    String DOCK_CAR = "Dock=CarDock;Version=1.0";

    String DOCK_MOBILE = "Dock=MobileDock;Version=1.0";

    String DOCK_HD = "Dock=HDDock;Version=1.0";

    int DOCK_EXTRA_MOBILE = 5;

    int DOCK_EXTRA_HD = 4;

    String DOCK_STATE_MONITOR = "com.motorola.contextual.pickers.conditions.dock.DockStateMonitor";

    String DOCK_PERSISTENCE = "com.motorola.contextual.smartprofile.dock.persistence";

    String DOCK_PUB_KEY = "com.motorola.contextual.smartprofile.dock";

    String ACTION_MOT_DOCK_EVENT = "com.motorola.internal.intent.action.MOT_DOCK_EVENT";

    String EXTRA_MOT_DOCK_STATE = "com.motorola.internal.intent.extra.DOCK_STATE";

    String NEW_DOCK_CONFIG_PREFIX = "Dock=";

    String NEW_DOCK_CONFIG_PREFIX_KEY = "Dock";

    String DOCK_CONFIG_VERSION = "1.0";
}


/**
 * This class displays options for Dock precondition and allows the user to
 * choose one This also constructs the rule for Dock precondition and passes to
 * the Smart Rules
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Extends DialogActivity
 *      Implements Constants, DockConstant
 *
 * RESPONSIBILITIES:
 * This class displays options for Dock precondition and allows the user to choose one
 * This also constructs the rule for Dock precondition and passes to the Smart Rules
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public final class DockActivity extends DialogActivity implements DockConstants, Constants {
//    private final static String TAG = DockActivity.class.getSimpleName();

    protected static final String sSupportedDockLevels[] = new String[] { DOCK_ANY, DOCK_DESK,
            DOCK_HD, DOCK_CAR};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(final Bundle savedInstanceState) {

        final String supportedDockLevelDesc[] = new String[] {
                getString(R.string.any), getString(R.string.desk),
                getString(R.string.hd), getString(R.string.car)
        };

        mItems = supportedDockLevelDesc;
        mDescription = supportedDockLevelDesc;
        mModeDescption = sSupportedDockLevels;

        mActionBarTitle = getString(R.string.dock_title);
        mTitle = getString(R.string.dock_prompt);
        super.onCreate(savedInstanceState);
    }
}

