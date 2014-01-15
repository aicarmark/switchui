/*
 * @(#)RefreshRequest.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/22  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.psf.PsfConstants;
import com.motorola.contextual.smartrules.psf.mediator.protocol.IMediatorProtocol;

/**
 * This class handles "refresh" command from consumers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "refresh" command intent to requested publisher
 *
 * COLABORATORS:
 *     Consumer - Uses the preconditions available across the system
 *     ConditionPublisher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class MediatorInit implements IMediatorProtocol, MediatorConstants {

    private static final String TAG = MediatorInit.class.getSimpleName();
    private static final String MEDIATOR_DB_PATH = "/data/data/" + Constants.PACKAGE + "/databases/" + MediatorProvider.getDbName();

    public boolean processRequest(Context context, Intent intent) {
        return true;
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();

        boolean dbPresent = MediatorHelper.isDbPresent(MEDIATOR_DB_PATH);

        Intent outBound = new Intent(PsfConstants.ACTION_SA_CORE_INIT_COMPLETE);

        if (dbPresent) {
            outBound.putExtra(PsfConstants.EXTRA_DATA_CLEARED, FALSE);
        } else {
            outBound.putExtra(PsfConstants.EXTRA_DATA_CLEARED, TRUE);
        }

        try {
            if(LOG_INFO) Log.i(TAG,
                                   "Version Code : " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode
                                   + " Version Name " + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);

        } catch(Exception e) {
            e.printStackTrace();
        }

        intentsToBroadcast.add(outBound);

        MediatorHelper.setMediatorInitialized(context);

        return intentsToBroadcast;
    }



}
