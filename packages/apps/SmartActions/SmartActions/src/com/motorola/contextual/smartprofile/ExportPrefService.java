/*
 * @(#)SmartProfileLocUtils.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * VXMD37        10/15/2012 NA				  Initial version
 *
 */
package com.motorola.contextual.smartprofile;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Service to run the backup job.
 *
 *<code><pre>
 * CLASS:
 *  None.
 *
 *  implements
 *      IntentService
 *
 * RESPONSIBILITIES:
 *  None
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class ExportPrefService extends IntentService {

    private static final String TAG = ExportPrefService.class.getSimpleName();

    public ExportPrefService(String name) {
        super(name);
    }

    public ExportPrefService() {
        this(ExportPrefService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Constants.LOG_DEBUG)
            Log.d(TAG, "starting async task");
        new ExportPrefTask().execute();
    }

    private class ExportPrefTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (Constants.LOG_DEBUG)
                Log.d(TAG, "async task executed");
            SmartProfileLocUtils.exportLocationData(getApplicationContext());
            return null;
        }

    }

}
