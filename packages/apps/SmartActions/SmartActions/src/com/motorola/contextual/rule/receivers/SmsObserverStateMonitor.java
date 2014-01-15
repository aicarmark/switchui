/*
 * @(#)MeetingSmsObserverStateMonitor.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21693        2012/05/16  NA               Initial version
 *
 */

package com.motorola.contextual.rule.receivers;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android_const.provider.TelephonyConst;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.Util;
import com.motorola.contextual.rule.publisher.PublisherIntentService;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.monitorservice.CommonStateMonitor;

/**
 * This class extends CommonStateMonitor for Sleep inferencing
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommonStateMonitor
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * Extends methods of CommonStateMonitor for charging publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class SmsObserverStateMonitor extends CommonStateMonitor implements Constants {

    public static SmsObserver mObserver = null;

    @Override
    public ContentObserver getObserver(Context context) {
        if (mObserver == null)
            mObserver = new SmsObserver(context);
        return mObserver;
    }

    @Override
    public void setObserver(ContentObserver obs) {
        mObserver = (SmsObserver) obs;
    }

    @Override
    public String getType() {
        return OBSERVER;
    }

    @Override
    public ArrayList<String> getStateMonitorIdentifiers() {
        ArrayList<String> actions = new ArrayList<String>();

        actions.add(TelephonyConst.SMS_CONTENT_URI);
        return actions;
    }

    /** This class contains the logic receive SMS onchange,
     * fetch sent SMS no# and launch Meeting inference
    *
    *<code><pre>
    *
    * CLASS:
    * extends ContentObserver
    *
    * RESPONSIBILITIES:
    *  see each method for details
    *
    * COLABORATORS:
    *  None.
    *
    * USAGE:
    *  see methods for usage instructions
    *
    *</pre></code>
    */
    private static class SmsObserver extends ContentObserver implements Constants {

        Context mContext = null;
        private final String TAG = Constants.RP_TAG + SmsObserver.class.getSimpleName();

        public SmsObserver(Context context) {
            super(null);
            mContext = context;
        }

        @Override
        public void onChange(boolean arg0) {
            super.onChange(arg0);

            processOnChange();

        }

        private synchronized void processOnChange() {

            boolean smsSent = false;
            String pNum = null;
            Uri smsUri = Uri.parse(TelephonyConst.SMS_CONTENT_URI);
            Cursor cur = null;
            if (LOG_DEBUG) Log.d(TAG, "Sms onchange received");

            try {
                String sortOrder = "_id" + DbSyntax.DESC + DbSyntax.LIMIT + "1";
                cur = mContext.getContentResolver().query(smsUri, null, null,
                        null, sortOrder);
                if (cur != null && cur.moveToFirst()) {
                    String protocol = cur.getString(cur.getColumnIndex(TelephonyConst.SmsDbColumns.PROTOCOL));
                    String type = cur.getString(cur.getColumnIndex(TelephonyConst.SmsDbColumns.TYPE));
                    pNum = cur.getString(cur.getColumnIndex(TelephonyConst.SmsDbColumns.ADDRESS));
                    if (LOG_DEBUG) Log.d(TAG, "no#=" + pNum);

                    pNum = PhoneNumberUtils.extractNetworkPortion(pNum);
                    pNum = Util.getLastXChars(pNum, 10);

                    if (LOG_DEBUG) Log.d(TAG, "Sms type=" + type + "; protocol=" + protocol);
                    smsSent = (protocol == null && type.equals(TelephonyConst.MESSAGE_TYPE_SENT));
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception in SmsObserver");
                e.printStackTrace();
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }

            if(smsSent){

                Intent serviceIntent = new Intent(mContext, PublisherIntentService.class);
                serviceIntent.setAction(CoreConstants.OUTBOUND_SMS_ACTION);
                serviceIntent.putExtra(Intent.EXTRA_PHONE_NUMBER, pNum);
                serviceIntent.putExtra(CoreConstants.EXTRA_EVENT_TYPE, Constants.INFERENCE_INTENT);
                serviceIntent.putExtra(Constants.IDENTIFIER, Constants.RULE_KEY_MEETING);

                if (LOG_DEBUG) Log.d(TAG, "Start RP Service for Action=" + CoreConstants.OUTBOUND_SMS_ACTION);
                ComponentName component = mContext.startService(serviceIntent);
                if (component == null) {
                    Log.e(TAG, "context.startService() failed for: "
                            + PublisherIntentService.class.getSimpleName());
                }
            }
        }
    }
}