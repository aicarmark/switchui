/*
 * @(#)SettingsService.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/10  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.motorola.contextual.actions.StatefulAction.Status;
import com.motorola.contextual.debug.DebugTable;

/**
 * This class handles the change in system settings for ones that require a content observer.
 * <code><pre>
 * CLASS:
 *     Extends Service.
 *
 * RESPONSIBILITIES:
 *     When the change for a particular setting is received:
 *      1. Checks if this was triggered by Smart Rules, if so sends
 *         the action exec status intent.
 *      2. If this was caused by an external entity (user or other apps)
 *          a. If Smart Rules is interested in this setting, sends SETTING_CHANGE
 *             intent.
 *          b. Else ignores the setting change.
 *
 * COLLABORATORS:
 *     Smart Rules
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class SettingsService extends Service implements Constants {

    private static final String TAG = TAG_PREFIX + SettingsService.class.getSimpleName();
    private static final String SERVICE_KEY = "ServiceKey";

    private HashMap<String, SettingObserver> mObservers = new HashMap<String, SettingObserver>();

    private class SettingObserver extends ContentObserver {

        private String mObserved;
        private String mActionKey;

        /**
         * Constructor
         *
         * @param h
         * @param observed
         * @param actionKey
         * @param actionString
         */
        public SettingObserver(Handler h, String observed, String actionKey) {
            super(h);
            mObserved = observed;
            mActionKey = actionKey;
        }

        public void onChange(boolean selfChange) {

            Context context = SettingsService.this;
            String ruleKey = USER;
            String debugReqResp = Utils.generateReqRespKey();

            StatefulAction sai = (StatefulAction)ActionHelper.getAction(context, mActionKey);
            if(sai == null){
		Log.w(TAG, "StatefulAction not found for " + mActionKey);
            	return;
            }

            Status status = sai.handleSettingChange(context, mObserved);

            if (status.equals(Status.SUCCESS) || status.equals(Status.FAILURE)) {
                String settingString = sai.getSettingString(context);
                if (Persistence.removeValue(context, mActionKey + MONITOR_SUFFIX) != null) {
			StatefulActionHelper.sendSettingsChange(SettingsService.this, mActionKey, settingString);
			if (LOG_INFO) {
				Log.i(SettingsService.TAG, "sendSettingsChange : " + mActionKey);
			}
                }else {
			Log.w(SettingsService.TAG, "No active listeners");
			return;
                }
                // we sent the setting change, deregister now.
                handleRequest(mActionKey, false, sai);
                if (mObservers.size() == 0) {
			// no more settings to observe
			if (LOG_INFO) Log.i(TAG, "Stopping service");
			stopSelf();
                }

                Utils.writeToDebugViewer(context, ruleKey, DebugTable.Direction.INTERNAL, QA_TO_MM, sai.getState(context),
                        debugReqResp, status.equals(Status.SUCCESS) ? SUCCESS : FAILURE, mActionKey);

                if (LOG_INFO) {
                    Log.i(SettingsService.TAG, "In onChange, value of " + mActionKey);
                }

            } else {
                if (LOG_INFO)
                    Log.i(TAG, "Transient state or action status already sent");
                return;
            }
        }
    }

    /**
     * Update the observation status of a particular setting - this function either adds/removes
     * the action Key to/from persistent storage based on whether register is true or false.
     *
     * @param context - caller's context
     * @param actionKey
     * @param register
     */
    private static void updateObservationStatus(Context context, String actionKey, boolean register) {
        // an empty list would be returned if no mapping is found
        List<String> list = Persistence.retrieveValuesAsList(context, SERVICE_KEY);
        int size = list.size();

        if (register) {
            if (!list.contains(actionKey)) {
                list.add(actionKey);
            }else {
                // list already contains the key
            }
        } else {
            list.remove(actionKey);
        }

        // if the list was updated
        if (list.size() != size) {
            if (list.size() > 0) {
                Persistence.commitValues(context, SERVICE_KEY, list.toArray(new String[list.size()]));
            } else {
                Persistence.removeValues(context, SERVICE_KEY);
            }
        } else {
            // Setting already present, ignore
        }

    }

    /**
     * Checks if the intent has com.motorola.intent.action.BOOT_COMPLETE_RECEIVED action
     *
     * @param intent
     * @return
     */
    private boolean hasActionBootCompleted(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && action.equals(BOOT_COMPLETE_INTENT)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (LOG_INFO)
            Log.i(TAG, "In onStartCommand with intent " + intent);
        if (intent == null || hasActionBootCompleted(intent)) {
            // restart or reboot
            // need to register all the observers again
        	String actionKeys[] = null;
        	if(!MONITOR_SETTINGS_ALWAYS)
        		actionKeys = Persistence.retrieveValues(this, SERVICE_KEY);
        	else {
        		List<String> listActionKeys =
        				StatefulActionHelper.getAllContentObserverBackedSettings(getApplicationContext());
        		actionKeys = new String[listActionKeys.size()];
        		listActionKeys.toArray(actionKeys);

        	}

            if (actionKeys != null) {

                for (String key : actionKeys) {
                    StatefulAction sai = (StatefulAction)ActionHelper.getAction(this, key);
                    handleRequest(key, true, sai);
                }

            } else {
                if (LOG_INFO) Log.i(TAG, "no setting to observe");
            }


        } else if(!MONITOR_SETTINGS_ALWAYS){

            String actionKey = intent.getStringExtra(EXTRA_ACTION_KEY);
            boolean register = intent.getBooleanExtra(EXTRA_REGISTER, false);
            StatefulAction sai = (StatefulAction)ActionHelper.getAction(this, actionKey);
            handleRequest(actionKey, register, sai);
        }

        if (mObservers.size() == 0) {
            // No more settings to observe, stop the service
            if (LOG_INFO) Log.i(TAG, "Stopping service");
            stopSelf();
        }

        // We want to stay alive until we decide to kill ourselves
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LOG_INFO) Log.i(TAG, "In onDestroy");

        Iterator<Entry<String, SettingObserver>> iter = mObservers.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, SettingObserver> entry = iter.next();
            getContentResolver().unregisterContentObserver(entry.getValue());
            if (LOG_INFO) Log.i(TAG, "unregistered observer for " + entry.getKey());
            iter.remove();
        }
    }

    /**
     * Handles the request to either register or deregister a particular setting
     *
     * @param actionKey
     * @param register
     * @param sai
     */
    private void handleRequest(String actionKey, boolean register, StatefulAction sai) {
        if (sai != null) {
            boolean status = false;
            String[] setting = sai.getSettingToObserve();
            if (setting != null) {
                for (String s: setting) {
                    status = (register) ? registerObserver(s, actionKey, sai) : deregisterObserver(s);
                }
            }
            if (status) {
                updateObservationStatus(this, actionKey, register);
            }else {
                Log.w(TAG, "Duplicate request");
            }

        } else {
            Log.w(TAG, "unrecognized action");
        }
    }

    /** Create and register content observer for a particular setting
     *
     * @param observe
     * @param actionKey
     * @param sai
     * @return
     */
    private boolean registerObserver(String observe, String actionKey, StatefulAction sai) {
        boolean status = false;
        if (!mObservers.containsKey(observe)) {
            SettingObserver observer = new SettingObserver(new Handler(), observe, actionKey);
            Uri uri = sai.getUriForSetting(observe);

            if (uri != null && observer != null) {
                if (LOG_INFO) Log.i(TAG, "uri is " + uri.toString());
                getContentResolver().registerContentObserver(uri, false, observer);
                mObservers.put(observe, observer);
                status = true;
            }
        } else {

            Log.w(TAG, "Setting already registered");
        }
        return status;
    }

    /**  Deregister content observer for a particular setting
     *
     * @param observe
     * @return
     */
    private boolean deregisterObserver(String observe) {
        boolean status = false;
        SettingObserver observer = mObservers.remove(observe);
        if (LOG_INFO)
            Log.i(TAG, "In deregisterObserver for :" + observe );

        if (observer != null) {
            getContentResolver().unregisterContentObserver(observer);
            status = true;
        }else {
            Log.w(TAG, "Nothing to deregister");
        }
        return status;
    }


}
