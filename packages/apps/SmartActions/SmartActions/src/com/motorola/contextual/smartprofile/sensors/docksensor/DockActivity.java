/*
 * @(#)DockActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21383        2012/08/02  NA                Initial version
 */

package com.motorola.contextual.smartprofile.sensors.docksensor;

import android.os.Bundle;

import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.DialogActivity;
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

    String DOCK_STATE_MONITOR = "com.motorola.contextual.smartprofile.sensors.docksensor.DockStateMonitor";

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
 * 		Extends DialogActivity
 * 		Implements Constants, DockConstant
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
	
    @SuppressWarnings("unused")
	private final static String TAG = DockActivity.class.getSimpleName();

    protected static final String sSupportedDockLevels[] = new String[] { DOCK_ANY, DOCK_DESK,
            DOCK_HD, DOCK_CAR, DOCK_MOBILE

                                                                        };
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String supportedDockLevelDesc[] = new String[] {
            getString(R.string.any), getString(R.string.desk),
            getString(R.string.hd), getString(R.string.car),
            getString(R.string.mobile)
        };

        mItems = supportedDockLevelDesc;
        mModeDescption = sSupportedDockLevels;
        mDescription = supportedDockLevelDesc;

        mTitle = getString(R.string.dock);
        mIcon = R.drawable.ic_dialog_dock;

        // Do a show Dialog, only when the activity is first created, don't do
        // it for
        // orientation changes
        if (savedInstanceState == null)
            super.showDialog();

    }
}
