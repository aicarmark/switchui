/*
 * @(#)PsfService.java
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

import com.motorola.contextual.actions.FrameworkUtils;
import com.motorola.contextual.smartprofile.sensors.motiondetectoradapter.MotionDetectorAvailabilityFinder;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Gets the right handler for an intent and hands over the intent
 * to the handler for processing
 *<code><pre>
 * CLASS:
 *     PsfService Extends IntentService
 *
 * RESPONSIBILITIES:
 *     Implements abstract functions of IntentService onHandleIntent
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PsfService extends IntentService implements PsfConstants {

    private static final String TAG = PsfConstants.PSF_PREFIX + PsfService.class.getSimpleName();

    /**
     * Constructor
     */
    public PsfService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (LOG_DEBUG) Log.d(TAG, "Service created.. ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LOG_DEBUG) Log.d(TAG, "Service Destroyed.. ");
    }

    @Override
    protected void onHandleIntent (Intent intent) {
        handleIntent(intent);
    }

    /**
     * Handles separate intents
     * @param intent - Incoming intent
     */
    private void handleIntent(Intent intent) {
        Context context = this.getApplicationContext();
        IntentHandler handler = null;
        String action = null;

        if (intent != null) {
            action = intent.getAction();

            if (action != null) {
                if (action.equals(ACTION_PSF_INIT)) {
                    if (LOG_INFO) Log.i(TAG, "Checking if framework apk is available or not");
                    FrameworkUtils.checkAndDisableFramework(context);
                    MotionDetectorAvailabilityFinder.checkAndDisableMDAdapter(context);
                }

                handler = IntentHandler.getIntentHandler(context, intent);
                if (handler != null) {
                    handler.handleIntent();
                } else {
                    if (LOG_ERROR) Log.e(TAG, "Handler missing for intent");
                }
            } else {
                if (LOG_ERROR) Log.e(TAG, "Action null in handleIntent " + intent.toUri(0));
            }
        } else {
            if (LOG_ERROR) Log.e(TAG, "Intent null in handleIntent");
        }

    }

}
