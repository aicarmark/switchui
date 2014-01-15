/*
 * @(#)IntentHandler.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/21                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.paf.PafIntentHandler;
import com.motorola.contextual.smartrules.psf.psr.PsrInitHandler;
import com.motorola.contextual.smartrules.psf.psr.ConfigListHandler;

/** Abstract class Intent Handler - Various intents to be handled by
 * PAF (Publisher Availability Finder) are expected to be derived from this class
 * 
 *<code><pre>
 * CLASS:
 *     IntentHandler
 *
 * RESPONSIBILITIES:
 *     Provides a factory function to create the right handler for an incoming intent
 *     Contains many utility functions that are of use to JobIntentHandlers
 *
 * USAGE:
 *     See each method.
 * </pre></code>
 */
public abstract class IntentHandler implements PsfConstants, DbSyntax {
	
    protected static String TAG = PsfConstants.PSF_PREFIX + IntentHandler.class.getSimpleName();
    protected Context mContext = null;
    protected Intent mIncomingIntent = null;

    abstract public boolean handleIntent();

    /** Use this function to get the right Job Handler to work with
     *  Add new handlers here
     *
     * @param context - Application context to work with
     * @param intent - Incoming intent for Job handler
     * @return An instance of IntentHandler to work with
     */
    public static IntentHandler getIntentHandler(Context context, Intent intent) {
        IntentHandler handler = null;
        String action = null;
        if (intent != null && ((action = intent.getAction()) != null)) {
            if (!action.equals(ACTION_AP_RESPONSE)) { 
                if (LOG_INFO) Log.i(TAG, action);
            }

            if (action.equals(ACTION_PSF_INIT) ||
            		// all the PackageManager actions that we're interested in.
            		// if a new Android package (application) is added or removed, replaced, changed or cleared, then, it could 
            		// have an effect on the SmartActions publisher list (if it's a publisher).
                    action.equals(Intent.ACTION_PACKAGE_ADDED) ||
                    action.equals(Intent.ACTION_PACKAGE_REMOVED) ||
                    action.equals(Intent.ACTION_PACKAGE_REPLACED) ||
                    action.equals(Intent.ACTION_LOCALE_CHANGED) ||
                    action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                    action.equals(Intent.ACTION_PACKAGE_DATA_CLEARED)) {
                handler = new PafIntentHandler(context, intent);
            } else if (action.equals(ACTION_PSR_INIT)) {
                handler = new PsrInitHandler(context, intent);
            } else if (action.equals(ACTION_AP_RESPONSE)) {
                handler = new ConfigListHandler(context, intent);
            }
        }

        return handler;
    }

    /**
     * Returns the action of the intent
     * @return - Action string associated with the incoming intent
     */
    public String getAction() {
        return(mIncomingIntent.getAction());
    }

    /**
     * Returns the extras as bundle
     * @return - Bundle of extras associated with the incoming intent
     */
    public Bundle getExtraBundle() {
        return (mIncomingIntent.getExtras());
    }

    /**
     * Returns the intent associated with the handler
     * @return Intent
     */
    public Intent getIntent() {
        return mIncomingIntent;
    }

    /**
     * Constructor
     * @param context - Application context to work with
     * @param intent - Incoming intent to operate on
     */
    protected IntentHandler(Context context, final Intent intent) {
        mContext = context.getApplicationContext();
        mIncomingIntent = intent;
    }

}
