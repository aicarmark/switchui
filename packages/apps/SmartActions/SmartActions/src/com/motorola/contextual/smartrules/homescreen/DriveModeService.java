/*
 * @(#)DriveModeService.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        04/11/2012    NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.homescreen;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.service.WidgetUpdateService;

/**
 * Service to handle all the widget requests. <code><pre>
 * CLASS:
 * 	 extends
 *
 *  implements
 *
 *
 * RESPONSIBILITIES:
 *
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 * </pre></code>
 */
public class DriveModeService extends IntentService implements
        Constants {

    private static final String             TAG                = DriveModeService.class.getSimpleName();

    /* One widget manager manages all the widgets */
    private static WidgetManager            sWidgetManager     = new WidgetManager();

    /* Scheduler to schedule the sync tasks */
    private static ScheduledExecutorService sScheduler;

    /* Request type */
    public static final String              EXTRA_REQUEST_TYPE = "EXTRA_REQUEST_TYPE";

    /* timeout */
    private static final long               DELAY              = 3;
    public static String                    MAP_RULE           = "MAP_RULE";
    private Handler                         mHandler           = new Handler();

    public enum RequestType {

        /** Setup new widget UI */
        NEW_WIDGET,

        /** Smart actions has initialized after power up. Sets up the sync */
        INIT_COMPLETE,

        /** Schedules the sync for the rule */
        SCHEDULE_SYNC,

        /** Send the sync request */
        SYNC_REQUEST,

        /** Handle the sync response */
        SYNC_RESPONSE,

        /** Send the sync request immediately */
        SYNC_IMMEDIATE,

        /** Delete the rule entries from DB and put the mapped widgets in default state */
        RULE_DELETED,

        /** Refresh the UI for modified rule */
        RULE_MODIFIED,

        /** Purge all data */
        DATA_CLEAR,

        /** Cleanup for the deleted widget */
        WIDGET_DELETED
    }

    /**
     * Constructor
     */
    public DriveModeService() {
        super(DriveModeService.class.getName());
    }

    /**
     * Constructor
     *
     * @param name
     */
    public DriveModeService(String name) {
        super(name);
    }

    /**
     * onHandleIntent()
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        RequestType reqType = (RequestType) intent.getSerializableExtra(EXTRA_REQUEST_TYPE);

        if (LOG_DEBUG) Log.d(TAG, "Service started!");
        if (LOG_DEBUG) Log.d(TAG, reqType != null ? reqType.name() : "INVALID REQ");

        final int[] appWidgetIds;
        RuleEntity ruleEntity;
        switch (reqType) {
            case NEW_WIDGET:
                appWidgetIds = intent.getIntArrayExtra(EXTRA_RESPONSE_ID);
                sWidgetManager.setDefault(getApplicationContext(), appWidgetIds);
                new InitiateBatchSyncTask().execute();
                break;

            case INIT_COMPLETE:
                new InitiateBatchSyncTask().execute();
                break;

            case SCHEDULE_SYNC:
                setCurWidgetClickable(intent, false);

                ruleEntity = new RuleEntity(intent, getApplicationContext());
                Thread task = new Thread(new AddRuleTask(ruleEntity));
                task.start();

                // Shutdown the current scheduler, if any.
                if (sScheduler != null && !sScheduler.isShutdown()) sScheduler.shutdownNow();

                // Schedule a rule sync to be initiated after DELAY (s); if the rule sync happens
                // before that in INFERRED_RULES_ADDED, this task will be cancelled.
                sScheduler = Executors.newSingleThreadScheduledExecutor();
                sScheduler.schedule(new SyncTask(ruleEntity.getRuleKey()), DELAY, TimeUnit.SECONDS);
                break;

            case SYNC_REQUEST:
                ruleEntity = new RuleEntity(intent, getApplicationContext());
                // Shutdown the current scheduler, if any.
                if (sScheduler != null && !sScheduler.isShutdown()) sScheduler.shutdownNow();
                this.intiateRuleSync(ruleEntity.getRuleKey());
                new SyncDBTask().execute(ruleEntity);
                // Schedule a second timeout to restore to default state, if not synced.
                sScheduler = Executors.newSingleThreadScheduledExecutor();
                sScheduler.schedule(new RestoreDefaultTask(getApplicationContext(), ruleEntity.getWidgetIds()), DELAY,
                    TimeUnit.SECONDS);
                break;

            case SYNC_RESPONSE:
                setCurWidgetClickable(intent, true);

                // Shutdown the current scheduler, as we have got the sync response and SyncTask is
                // no longer needed.
                if (sScheduler != null && !sScheduler.isShutdown()) sScheduler.shutdownNow();
                new SyncUiTask().execute(intent);
                break;

            case SYNC_IMMEDIATE:
                setCurWidgetClickable(intent, false);

                String ruleKey = intent.getStringExtra(RuleTable.Columns.KEY);
                boolean mapRule = intent.getBooleanExtra(MAP_RULE, false);
                if (mapRule) {
                    ruleEntity = new RuleEntity(intent);
                    Thread t = new Thread(new AddRuleTask(ruleEntity));
                    t.start();
                }

                this.intiateRuleSync(ruleKey);
                break;

            case RULE_DELETED:

                new DeleteRuleTask().execute(new RuleEntity(intent, getApplicationContext()));
                break;

            case DATA_CLEAR:

                Context context = getApplicationContext();
                appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    new ComponentName(context, SmartActionWidgetProvider.class.getName()));
                ruleEntity = new RuleEntity(intent, context);
                ruleEntity.setWidgetId(appWidgetIds);
                new ResetTask().execute(ruleEntity);
                break;

            case WIDGET_DELETED:
                appWidgetIds = intent.getIntArrayExtra(EXTRA_RESPONSE_ID);
                Runnable r = new Runnable() {

                    public void run() {
                        sWidgetManager.deleteWidget(getApplicationContext(), appWidgetIds);
                    }

                };
                Thread deleteTask = new Thread(r);
                deleteTask.start();
                break;
        }

        if (LOG_VERBOSE) new Thread(new DumpTask()).start();

    }

    /**
     * Sync all the mapped widgets for the rule update response. <code><pre>
     * CLASS:
     * 	 extends AsyncTask
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     *  Get widget ids mapped to the rule in background
     *  Sync all the mapped widgets for the rule update response.
     *
     * COLABORATORS:
     * 	 None.
     *
     * USAGE:
     * 	 See each method.
     * </pre></code>
     */
    private class SyncUiTask extends AsyncTask<Intent, Void, int[]> {
        private Intent mIntent;

        /**
         * doInBackground()
         */
        @Override
        protected int[] doInBackground(Intent... params) {
            RuleEntity re = new RuleEntity(params[0]);
            this.mIntent = params[0];
            return sWidgetManager.getWidgets(getApplicationContext(), re.getRuleKey());
        }

        /**
         * onPostExecute()
         */
        @Override
        protected void onPostExecute(int[] result) {
            handleIntent(mIntent, result);
        }

    }

    /**
     * Send sync request for all mapped rules. <code><pre>
     * CLASS:
     * 	 extends AsyncTask
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     * 		Send sync request for all mapped rules.
     *
     * COLABORATORS:
     * 	 None.
     *
     * USAGE:
     * 	 See each method.
     * </pre></code>
     */
    private class InitiateBatchSyncTask extends AsyncTask<Void, Void, String[]> {

        /**
         * doInBackground()
         */
        @Override
        protected String[] doInBackground(Void... voids) {
            return sWidgetManager.getRuleKeysAndSync(getApplicationContext());
        }

        /**
         * onPostExecute()
         */
        @Override
        protected void onPostExecute(String[] result) {
            intiateRuleSync(result);
        }

    }

    /**
     * Delete a mapped rule. <code><pre>
     * CLASS:
     * 	 extends
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     * 		Delete a mapped rule in background.
     *      Put the mapped widgets in default state.
     *
     * COLABORATORS:
     * 	 None.
     *
     * USAGE:
     * 	 See each method.
     * </pre></code>
     */
    private class DeleteRuleTask extends AsyncTask<RuleEntity, Void, RuleEntity> {

        /**
         * doInBackground()
         */
        @Override
        protected RuleEntity doInBackground(RuleEntity... params) {
            // A rule is deleted; processed irrespective of number of widget instances.
            String deletedKey = params[0].getRuleKey();
            int[] widgets;
            if (deletedKey != null /* && sWidgetManager.hasRule(deletedKey) */) {

                widgets = sWidgetManager.deleteRule(getApplicationContext(), deletedKey);
                // If the deleted rule is drive mode.

                params[0].setWidgetId(widgets);
            }
            return params[0];
        }

        /**
         * onPostExecute()
         */
        @Override
        protected void onPostExecute(RuleEntity result) {
            if (result.getWidgetIds().length > 0)
                sWidgetManager.setDefault(result.getContext(), result.getWidgetIds());
        }

    }

    /**
     * Reset all the widgets. <code><pre>
     * CLASS:
     * 	 extends
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     * 		Reset all the widgets.
     *      Purge all local data.
     *
     * COLABORATORS:
     * 	 None.
     *
     * USAGE:
     * 	 See each method.
     * </pre></code>
     */
    private class ResetTask extends AsyncTask<RuleEntity, Void, RuleEntity> {

        /**
         * doInBackground()
         */
        @Override
        protected RuleEntity doInBackground(RuleEntity... params) {
            sWidgetManager.purge(params[0].getContext(), params[0].getWidgetIds());
            return params[0];
        }

        /**
         * onPostExecute()
         */
        @Override
        protected void onPostExecute(RuleEntity result) {
            sWidgetManager.setDefault(result.getContext(), result.getWidgetIds());
        }
    }

    /**
     * Sync the DB with rule ID. <code><pre>
     * CLASS:
     * 	 extends
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     * 		Sync the DB with rule ID.
     *      Put mapped widgets in syncing state.
     *
     * COLABORATORS:
     * 	 None.
     *
     * USAGE:
     * 	 See each method.
     * </pre></code>
     */
    private class SyncDBTask extends AsyncTask<RuleEntity, Void, RuleEntity> {

        /**
         * doInBackground()
         */
        @Override
        protected RuleEntity doInBackground(RuleEntity... params) {
            int[] wid = params[0].getWidgetIds();
            sWidgetManager.updateRuleId(getApplicationContext(), params[0].getRuleId(), wid[0]);
            return params[0];
        }

        /**
         * onPostExecute()
         */
        @Override
        protected void onPostExecute(RuleEntity result) {
            sWidgetManager.getRuleManager(result.getRuleKey()).syncing(result.getContext(),
                result.getWidgetIds());
        }

    }

    /**
     * Add a new rule. <code><pre>
     * CLASS:
     * 	 extends
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     * 		Put a new entry for the rule in DB and map it to the widget.
     *
     * COLABORATORS:
     * 	 None.
     *
     * USAGE:
     * 	 See each method.
     * </pre></code>
     */
    private class AddRuleTask implements
            Runnable {
        private RuleEntity mRuleEntity;

        public AddRuleTask(RuleEntity re) {
            mRuleEntity = re;
        }

        /**
         * run()
         */
        public void run() {
            sWidgetManager.mapRule(mRuleEntity);
        }
    }

    /**
     * Used to synchronize the widget with recently added rule key. <code><pre>
     * CLASS:
     *   extends Runnable
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     *  synchronize the widget with recently added rule key.
     *
     * COLABORATORS:
     *   None.
     *
     * USAGE:
     *   Use this with Executor or Thread or FutureTask or a relevant scheme.
     * </pre></code>
     */
    private class SyncTask implements
            Runnable {
        private String ruleKey;

        public SyncTask(String ruleKey) {
            this.ruleKey = ruleKey;
        }

        public void run() {
            if (!Thread.interrupted()) {
                if (LOG_DEBUG) {
                    Log.d(TAG, "not interrupted");
                }
                intiateRuleSync(ruleKey);
            } else {
                if (LOG_DEBUG) {
                    Log.d(TAG, "interrupted");
                }
            }
        }
    }

    /**
     * Used to create a new thread to put the widget in default state. <code><pre>
     * CLASS:
     *   extends Runnable
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     *  Restores to default state (ADD RULE).
     *
     * COLABORATORS:
     *   None.
     *
     * USAGE:
     *   Use this with Executor or Thread or FutureTask or a relevant scheme.
     * </pre></code>
     */
    private class RestoreDefaultTask implements
            Runnable {
        private Context ctx;
        private int[]     wid;

        public RestoreDefaultTask(Context ctx, int[] wid) {
            super();
            this.ctx = ctx;
            this.wid = wid;
        }


        public void run() {
            if (!Thread.interrupted()) {
                if (LOG_DEBUG) {
                    Log.d(TAG, "not interrupted");
                }

                // Post the UI operation back to UI thread.
                mHandler.post(new Runnable() {

                    public void run() {
                        sWidgetManager.setDefault(ctx, wid);
                    }

                });
            } else {
                if (LOG_DEBUG) {
                    Log.d(TAG, "interrupted");
                }
            }
        }
    }

    /**
     * Sends the intent for to get the status of the given rule key.
     *
     * @param ruleKey - Rule key
     */
    public void intiateRuleSync(String ruleKey) {
        intiateRuleSync(new String[] { ruleKey });
    }

    /**
     * Sends the intent for to get the status of the given rule key.
     *
     * @param ruleKey - Array of rule keys.
     */
    public void intiateRuleSync(String[] ruleKey) {
        if (LOG_DEBUG) {
            Log.d(TAG, "initiating rule sync for " + ruleKey);
        }
        Intent updateIntent = new Intent();
        updateIntent.setAction(WIDGET_UPDATE_INTENT).putExtra(WidgetUpdateService.RULE_KEYS_EXTRA,
            ruleKey);
        getApplicationContext().sendBroadcast(updateIntent);
    }

    /**
     * Parse the intent and updates the widget accordingly.
     *
     * @param context - Context
     * @param intent - Intent
     * @param appWidgetIds - Array of widgets to be updated
     */
    private void handleIntent(Intent intent, int[] appWidgetIds) {
        Context context = this.getApplicationContext();
        RuleInfo info = new RuleInfo(intent);
        RuleEntity re = new RuleEntity(intent);
        re.setWidgetId(appWidgetIds);
        if (re.getRuleKey() == null || info.getActive() == INVALID_KEY || info.getEnabled() == INVALID_KEY
                || info.getManual() == INVALID_KEY) {
            Log.e(TAG, "Invalid Return value from " + intent.getAction());
            return;
        }
        if (LOG_DEBUG) {
            Log.d(TAG, "key, id, act, enabled, manual - " + re.getRuleKey() + ", " + re.getRuleId() + ", " + info.getActive() + ", "
                    + info.getEnabled() + ", " + info.getManual());
            for (int wid : appWidgetIds)
                Log.d(TAG, "Widget id #" + wid);
        }

        RuleManager ruleManager = sWidgetManager.getRuleManager(re.getRuleKey());
        if (ruleManager == null) return;

        // Put in default state.
        ruleManager.deactivate();

        // Enable manual or auto state.
        if (info.isManual()) {
            if (LOG_DEBUG) {
                Log.d(TAG, "Manual");
            }
            ruleManager.manual();
        } else {
            if (LOG_DEBUG) {
                Log.d(TAG, "Auto");
            }
            ruleManager.auto();
        }

        if (!info.isEnabled()) { // Check if rule is disabled.
            if (LOG_DEBUG) {
                Log.d(TAG, "enabled = 0 --> off");
            }
            ruleManager.off(context, re);
        } else if ((info.getManual() | info.getActive()) == 1) { // Rule is enabled here.
            // Either manual (manual=1; act=0) or auto active (manual=0; act=1)
            if (LOG_DEBUG) {
                Log.d(TAG, "manual = " + info.getManual() + " act = " + info.getActive() + " --> ON");
            }
            ruleManager.on(context, re);
        } else { // Auto and Ready.
            if (LOG_DEBUG) {
                Log.d(TAG, "manual = " + info.getManual() + " act = " + info.getActive() + " --> READY");
            }
            ruleManager.ready(context, re);
        }

    }

    /**
     * Dumps the rule table to system out. <code><pre>
     * CLASS:
     * 	 extends
     *
     *  implements
     *
     *
     * RESPONSIBILITIES:
     *
     *
     * COLABORATORS:
     * 	 None.
     *
     * USAGE:
     * 	 See each method.
     * </pre></code>
     */
    private class DumpTask implements
            Runnable {

        /**
         * run()
         */
        public void run() {
            if (LOG_DEBUG) Log.d(TAG, "DB dump, may be out of sync due to threading");
            sWidgetManager.dump(getApplicationContext(), TAG);
        }

    }

    /**
      * Parse the intent and set the widget clickable state.
      *
      * @param intent - Intent
      * @param enable - whether the widget clickable
      */
      public void setCurWidgetClickable(Intent intent, boolean enable) {
          int wid = intent.getIntExtra(EXTRA_RESPONSE_ID, INVALID_KEY);
          String key = intent.getStringExtra(RuleTable.Columns.KEY);
          if(key == null)
             return;
          RuleManager ruleManager = sWidgetManager.getRuleManager(key);
          if(ruleManager == null)
              return;
          if(enable)
              ruleManager.enable(getApplicationContext(), wid);
          else
              ruleManager.disable(getApplicationContext(), wid);
     }
}
