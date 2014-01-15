/*
 * @(#)MissedCallObserver.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a18491        2012/03/16 NA                Initial version of MissedCallObserver
 *
 */
package  com.motorola.contextual.smartprofile.sensors.missedcallsensor;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.util.Log;

import com.motorola.contextual.smartprofile.CommandInvokerIntentService;
import com.motorola.contextual.smartprofile.Constants;


/**
 * This is a Content observer which receives change notifications for Missed Calls
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      Implements Constants
 *      Implements MissedCallConstants
 *      Extends ContentObserver
 *
 * RESPONSIBILITIES:
 * Starts command service to handle notify command
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class MissedCallObserver extends ContentObserver implements Constants, MissedCallConstants {

    private final static String LOG_TAG = MissedCallObserver.class.getSimpleName();
    Context mContext = null;

    public MissedCallObserver(Context context) {
        super(null);
        mContext = context;
    }

    @Override
    public void onChange(boolean arg0) {
        super.onChange(arg0);

        if (LOG_INFO) Log.i(LOG_TAG, "onChange");
        if(mContext != null) {
            Intent intent = new Intent();
            intent.setClass(mContext, CommandInvokerIntentService.class);
            intent.putExtra(EXTRA_COMMAND, NOTIFY_REQUEST);
            intent.setAction(MISSED_CALLS_PUB_KEY);
            ComponentName compName = mContext.startService(intent);

            if(compName == null) {
                Log.e(LOG_TAG, " Start service failed for : " + CommandInvokerIntentService.class.getSimpleName());
            }
        }

    }
}
