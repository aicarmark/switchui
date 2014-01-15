/*
 * @(#)ModeAdService.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21383        2012/05/03 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.modead;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.view.RuleConditionView;
/** This is a Intent Service class that handles the intents from
 * 	Condition Publisher and Mode Manager components
 *
 *<code><pre>
 * CLASS:
 * 	 extends IntentService
 *
 *  implements
 *   Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	 Processes the intents from Condition Publisher
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class ModeAdService extends IntentService implements Constants, DbSyntax {

    private static final int RETRY_LIMIT = 2;
    private static final int TIMEOUT_VALUE = 5000;
    private static final String TAG = ModeAdService.class.getSimpleName();
    private static final String EXTRA_RETRY_COUNT = "com.motorola.smartactions.intent.extra.RETRY_COUNT";
    private static final String EXTRA_INTENT_URI = "com.motorola.smartactions.intent.extra.INTENT_URI";
    private static final String ACTION_TIMEOUT = "com.motorola.smartactions.intent.action.TIMEOUT";


    public ModeAdService() {
        super(TAG);
    }

    public ModeAdService(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onHandleIntent(Intent intent) {
        if(LOG_INFO) Log.i( TAG, "onHandleIntent called intent is " + intent.toUri(0) );
        if(intent != null) {
            String action = intent.getAction();
            if(action != null) {
                if(action.equals(ACTION_LAUNCH_MODE_ACT_DEACTIVATOR)) {
                    handleLaunchModeAD(intent);
                } else if(action.equals(SA_CORE_KEY)) {
                    String eventType = intent.getStringExtra(EXTRA_EVENT_TYPE);
                    if(eventType != null) {
                        if(eventType.equals(SUBSCRIBE_RESPONSE)) {
                            String config = intent.getStringExtra(EXTRA_CONFIG);
                            String pubKey = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
                            String responseId = intent.getStringExtra(EXTRA_RESPONSE_ID);
                            String state = intent.getStringExtra(EXTRA_STATE);
                            String status = intent.getStringExtra(EXTRA_STATUS);
                            handleSubscribeResponse(config, pubKey, responseId, state, status);
                        } else if(eventType.equals(CANCEL_RESPONSE)) {
                            String config = intent.getStringExtra(EXTRA_CONFIG);
                            String pubKey = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
                            String responseId = intent.getStringExtra(EXTRA_RESPONSE_ID);
                            String status = intent.getStringExtra(EXTRA_STATUS);
                            handleCancelResponse(config, pubKey, responseId, status);
                        } else if(eventType.equals(NOTIFY)) {
                            String pubKey = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
                            HashMap<String, String> configStateMap = (HashMap<String, String>)intent.getSerializableExtra(EXTRA_CONFIG_STATE_MAP);
                            handleNotify(configStateMap, pubKey);
                        } else {
                            Log.e(TAG, " Unknown eventType ");
                        }
                    }
                } else if(action.startsWith(ACTION_TIMEOUT)) {
                    handleSubscribeTimeout(intent);
                } else  if(action.equals(CONDITION_PUBLISHER_DATA_RESET)) {
                    ArrayList<String> pubKeyList = intent.getStringArrayListExtra(EXTRA_PUBLISHER_KEY_LIST);
                    subscribeToPubkeyList(pubKeyList);
                }
            }
        }
    }

    /*
     * Handles the timeout for Subscrobe command.
     * If the retry count is less than RETRY_LIMIT, subscribe is sent again, else
     * Publisher with the given config is marked as invalid
     */
    private void handleSubscribeTimeout(Intent intent) {
        String intentUri = intent.getStringExtra(EXTRA_INTENT_URI);
        if(intentUri != null) {
            int retryCount = intent.getIntExtra(EXTRA_RETRY_COUNT, 0);
            try {
                Intent subscribeIntent = Intent.parseUri(intentUri, 0);
                if(retryCount < RETRY_LIMIT) {
                    sendBroadcast(subscribeIntent);
                    SubscribeTimer subscribeTimer = new SubscribeTimer(this, subscribeIntent, retryCount++);
                    subscribeTimer.start(TIMEOUT_VALUE);
                } else {
                    String pubKey = subscribeIntent.getStringExtra(EXTRA_PUBLISHER_KEY);
                    String config = subscribeIntent.getStringExtra(EXTRA_CONFIG);
                    ConditionPersistence.updateConditionValidity(this, null, pubKey, config, ConditionTable.Validity.INVALID);
                }
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /*
     * Sends subscribe request to all the configs of the given pubKeyList
     */
    private void subscribeToPubkeyList(ArrayList<String> pubKeyList) {
        if(LOG_DEBUG) Log.d(TAG, "subscribeToPubkeyList : pubKeyList = " + pubKeyList);
        // Query all the config + pub keys from distinct condition view
        ContentResolver cr = getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(Schema.DISTINCT_CONDITION_VIEW_CONTENT_URI, new String[] { ConditionTable.Columns.CONDITION_CONFIG,
                              ConditionTable.Columns.CONDITION_PUBLISHER_KEY
                                                                                       }, null, null, null);
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    if(LOG_DEBUG) Log.d(TAG, "Extracting distinct config and pub keys ");
                    do {
                        String config = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_CONFIG));
                        String pubKey = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_PUBLISHER_KEY));
                        if(LOG_DEBUG) Log.d(TAG,"Config: " + config + " and pubkey : " + pubKey);

                        if (pubKeyList == null) {
                            sendSubscribeCommand(config, pubKey);
                        } else if (pubKeyList.contains(pubKey)) {
                            sendSubscribeCommand(config, pubKey);
                        }
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
    }

    /*
     * Handles ModeAD launch for Rules Edited, Created and Deleted
     */
    private void handleLaunchModeAD(Intent intent) {
        String launchReason = intent.getStringExtra(EXTRA_LAUNCH_REASON);
        String ruleKey = intent.getStringExtra(EXTRA_RULE_KEY);
        if(launchReason != null) {
            if(launchReason.equals(RULE_CREATED)) {
                handleRuleCreated(ruleKey);
            } else if(launchReason.equals(RULE_EDITED)) {
                ArrayList<String> prevConfigPubKeyList = intent.getStringArrayListExtra(EXTRA_PREV_CONFIG_PUBKEY_LIST);
                handleRuleEdited(ruleKey, prevConfigPubKeyList);
            } else if(launchReason.equals(RULE_DELETED)) {
                ArrayList<String> prevConfigPubKeyList = intent.getStringArrayListExtra(EXTRA_PREV_CONFIG_PUBKEY_LIST);
                String callbackIntent = intent.getStringExtra(EXTRA_CALLBACK_INTENT);
                handleRuleDeleted(ruleKey, prevConfigPubKeyList, callbackIntent, false);
            } else if(launchReason.equals(DISABLE_ALL)) {
                ArrayList<String> prevConfigPubKeyList = intent.getStringArrayListExtra(EXTRA_PREV_CONFIG_PUBKEY_LIST);
                handleRuleDeleted(ruleKey, prevConfigPubKeyList, null, true);
            } else {
                Log.e(TAG, " Unknown action ");
            }
            // Send NOTIFY_DATA_CHANGE intent, so that BackupManager is invoked and backs up the latest data
            Intent newIntent = new Intent(NOTIFY_DATA_CHANGE);
            this.sendBroadcast(newIntent);
        }
    }



    /**
     * Perform the operations when Mode AD is invoked when a rule is created.
     */
    private void handleRuleCreated(String ruleKey) {
        ArrayList<String> newConfigPubKeyList = extractConfigPubKeyPairsToSubscribe(ruleKey);
        if(LOG_DEBUG) Log.d(TAG, "handleRuleCreated " + newConfigPubKeyList);
        for(String configPubKey : newConfigPubKeyList) {
            String[] configPubKeySplit = configPubKey.split(COMMA);
            sendSubscribeCommand(configPubKeySplit[0], configPubKeySplit[1]);
        }
    }

    /**
     * Perform the operations when Mode AD is invoked when a rule is edited.
     */
    private void handleRuleEdited(String ruleKey, ArrayList<String> configPubKeyList) {
        if(LOG_DEBUG) Log.d(TAG, " handleRuleEdited " + configPubKeyList);
        ArrayList<String> prevConfigPubKeyList = null;
        if(configPubKeyList != null && configPubKeyList.size() > 0) {
            prevConfigPubKeyList = extractConfigPubKeyPairsToCancel(configPubKeyList);
        }

        ArrayList<String> newConfigPubKeyList = extractConfigPubKeyPairsToSubscribe(ruleKey);
        if(LOG_DEBUG) Log.d(TAG, "handleRuleEdited " + configPubKeyList + ":" + prevConfigPubKeyList);

        if(prevConfigPubKeyList != null && prevConfigPubKeyList.size() > 0) {
            for(String prevConfigPubKey : prevConfigPubKeyList) {
                String[] prevConfigPubKeySplit = prevConfigPubKey.split(COMMA);
                sendCancelCommand(prevConfigPubKeySplit[0], prevConfigPubKeySplit[1]);
            }
        }

        if(newConfigPubKeyList != null && newConfigPubKeyList.size() > 0) {
            for(String configPubKey : newConfigPubKeyList) {
                String[] configPubKeySplit = configPubKey.split(COMMA);
                sendSubscribeCommand(configPubKeySplit[0], configPubKeySplit[1]);
            }
        }

    }


    /**
     * Perform the operations when Mode AD is invoked when a rule is deleted.
     */
    private void handleRuleDeleted(String ruleKey, ArrayList<String> configPubKeyList, String callbackIntent,
                                   boolean disableAll) {
        if(LOG_DEBUG) Log.d(TAG, " handleRuleDeleted " + configPubKeyList + ":" + ruleKey +":" + callbackIntent);
        if(configPubKeyList != null && configPubKeyList.size() > 0) {
            ArrayList<String> prevConfigPubKeyList = configPubKeyList;
            if(disableAll == false) prevConfigPubKeyList = extractConfigPubKeyPairsToCancel(configPubKeyList);

            if(prevConfigPubKeyList != null && prevConfigPubKeyList.size() > 0) {
                for(String prevConfigPubKey : prevConfigPubKeyList) {
                    String[] prevConfigPubKeySplit = prevConfigPubKey.split(COMMA);
                    sendCancelCommand(prevConfigPubKeySplit[0], prevConfigPubKeySplit[1]);
                }
            }
        }
        fireCallbackIntent(callbackIntent);

    }


    /**
     * Perform the operations to extract config and publisher key pairs to send cancel request.
     */
    private ArrayList<String> extractConfigPubKeyPairsToCancel(ArrayList<String> tempConfigPubKeyList) {
        if(LOG_DEBUG) Log.d(TAG, " extractConfigPubKeyPairsToCancel");
        ArrayList<String> configPubKeyList = extractDistinctCondPubKeyPairs();
        ArrayList<String> oldConfigPubKeyList = new ArrayList<String>();

        // For each old configPubKey pair, check if it is present in the distinct condition view
        for(String oldConfigPubKeyStr : tempConfigPubKeyList) {
            if(!configPubKeyList.contains(oldConfigPubKeyStr)) {
                if(LOG_DEBUG) Log.d(TAG,"Adding : " + oldConfigPubKeyStr);
                oldConfigPubKeyList.add(oldConfigPubKeyStr);
            }
        }

        return oldConfigPubKeyList;

    }

    /**
     * Perform the operations to extract all distinct config and publisher key pairs
     */
    private ArrayList<String> extractDistinctCondPubKeyPairs() {
        if(LOG_DEBUG) Log.d(TAG, " extractDistinctCondPubKeyPairs");
        ArrayList<String> configPubKeyList = new ArrayList<String>();

        // Query all the config + pub keys from distinct condition view
        ContentResolver cr = getContentResolver();
        Cursor cursor = null;
        try {

            cursor = cr.query(Schema.DISTINCT_CONDITION_VIEW_CONTENT_URI, new String[] { ConditionTable.Columns.CONDITION_CONFIG,
                              ConditionTable.Columns.CONDITION_PUBLISHER_KEY
                                                                                       }, null, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String config = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_CONFIG));
                    String pubKey = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_PUBLISHER_KEY));
                    configPubKeyList.add(config + COMMA + pubKey);
                    if(LOG_DEBUG) Log.d(TAG,"Config: " + config + " and pubkey : " + pubKey);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return configPubKeyList;

    }

    /**
     * Perform the operations to extract config and publisher key pairs for a given rulekey
     */
    private ArrayList<String> extractConfigPubKeyPairsToSubscribe(String ruleKey) {
        if(LOG_DEBUG) Log.d(TAG, " extractConfigPubKeyPairsToSubscribe: Rule key is " + ruleKey);
        ArrayList<String> configPubKeyList = new ArrayList<String>();
        // Query all the config + pub keys from distinct condition view
        ContentResolver cr = getContentResolver();
        Cursor cursor = null;
        try {
            String whereClause = RuleConditionView.Columns.KEY+EQUALS+Q+ruleKey+Q+
                                 AND+ConditionTable.Columns.ENABLED+EQUALS+Q+ConditionTable.Enabled.ENABLED+Q +
                                 AND+ConditionTable.Columns.CONDITION_VALIDITY+EQUALS+Q+TableBase.Validity.VALID+Q;

            cursor = cr.query(Schema.RULE_CONDITION_VIEW_CONTENT_URI, new String[] { ConditionTable.Columns.CONDITION_CONFIG,
                              ConditionTable.Columns.CONDITION_PUBLISHER_KEY
                                                                                   }, whereClause, null, null);
            if(cursor != null) {
                if(cursor.moveToFirst()) {
                    do {
                        String config = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_CONFIG));
                        String pubKey = cursor.getString(cursor.getColumnIndex(ConditionTable.Columns.CONDITION_PUBLISHER_KEY));
                        configPubKeyList.add(config + COMMA + pubKey);
                        if(LOG_DEBUG) Log.d(TAG,"Config: " + config + " and pubkey : " + pubKey);
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }

        return configPubKeyList;

    }

    /**
     * Handles the response for the subscribe command. It updates the state
     * value of the config from the response, for which subscribe command was sent.
     * @param config - Configuration for which the response is received
     * @param pubKey - Publisher key for which the response is received
     * @param responseId - Response ID that was sent in the Command
     * @param state - Latest state info sent from the Condition publisher
     * @param status - String representation of the Subscribe status - "success" or "failure"
     */
    private void handleSubscribeResponse(String config, String pubKey,
                                         String responseId, String state, String status) {
        if(status != null && status.equals(SUCCESS) && state != null) {
            updateConfigState(config, pubKey, state);
        } else {
            if(LOG_DEBUG) Log.d(TAG, " Status : " + status + "State : " + state);
        }
        Intent subscribeIntent = getSubscribeIntent(config, pubKey);
        SubscribeTimer subscribeTimer = new SubscribeTimer(this, subscribeIntent, 0);
        subscribeTimer.stop();
    }

    /**
     * Handles the response for the cancel command. For now, it does not do anything
     * @param config - Configuration for which the response is received
     * @param pubKey - Publisher key for which the response is received
     * @param responseId - Response ID that was sent in the Command
     * @param status - String representation of the Subscribe status - "success" or "failure"
     */
    private void handleCancelResponse(String config, String pubKey,
                                      String responseId, String status) {
        if(LOG_DEBUG) Log.d(TAG, "Cancel Status : " + status +" for config :" + config +" pubKey " + pubKey);

    }
    /**
     * Updates the state information for the given config of the publisher key
     * @param config - Config for which the state has to be updated
     * @param pubKey - Publisher key for the config
     * @param state - Latest state of the config
     */
    private void updateConfigState(String config, String pubKey,
                                   String state) {
        if(LOG_DEBUG) Log.d(TAG, " updateConfigState : " + config + ":" + pubKey + ":" + state);
        ContentValues contentValue = new ContentValues();

        contentValue.put(ConditionTable.Columns.CONDITION_MET,
                         ((state.equals("true"))?1:0));
        ContentResolver cr = getContentResolver();
        String whereClause = ConditionTable.Columns.CONDITION_CONFIG+EQUALS+Q+config+Q+
                             AND+ConditionTable.Columns.CONDITION_PUBLISHER_KEY+EQUALS+Q+pubKey+Q;
        int rowsUpdated = cr.update(ConditionTable.CONTENT_URI, contentValue, whereClause, null);

        if(rowsUpdated > 0) {
            DebugTable.writeToDebugViewer(this, DebugTable.Direction.INTERNAL,
                                          state, "updateConfigState",
                                          pubKey, MODEAD_DBG_MSG, null, config,
                                          Constants.PACKAGE, Constants.PACKAGE);
            RulesEvaluator re = new RulesEvaluator(this);
            re.evaluateAndSend(config, pubKey);
        }
    }

    /**
     * Handles the Notify from Condition Publishers and updates the state information
     * for all the configurations of the publisher key.
     * @param configList - List configurations whose state is updated
     * @param pubKey - Publisher key of the condition publisher
     * @param stateList - List of New states of all the Configurations
     */
    private void handleNotify(HashMap<String, String> configStateMap, String pubKey) {
        if(configStateMap != null && configStateMap.size() > 0) {
            if(LOG_DEBUG) Log.d(TAG, "handleNotify " + configStateMap.toString() + configStateMap.size() + ":" + pubKey);
            Iterator<Entry<String, String>> iterator = configStateMap.entrySet().iterator();
            while(iterator.hasNext()) {
                Entry<String, String> entry = iterator.next();
                String config = entry.getKey();
                if(LOG_DEBUG) Log.d(TAG, " config " + config + " state " + entry.getValue());
                updateConfigState(config, pubKey,  entry.getValue());
                iterator.remove();
            }
        }
    }


    /** The Intent that will be received back while saving a Rule and to be used for
     * 	knowing in UI when ModeAd is done. This would happen while saving only if Condition/Actions
     * 	are changed in the User Interface for an existing+active Rule
     */
    private void fireCallbackIntent(String callbackIntentStr) {
        if (LOG_DEBUG) Log.d(TAG, " fireCallbackIntent "+ callbackIntentStr);
        if(callbackIntentStr != null) {
            Intent intent;
            try {
                intent = Intent.parseUri(callbackIntentStr, 0);
                sendBroadcast(intent);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

        }
    }



    /**
     * Sends the subscribe command for the given config and publisher key
     * @param config - Configuration for which Subscribe has to be sent
     * @param pubKey - Publisher key of the Command Publisher
     */
    private void sendSubscribeCommand(String config, String pubKey) {
        if(config == null) return;
        if(LOG_DEBUG) Log.d(TAG, "sendSubscribeCommand :" + config + ":" + pubKey);
        Intent intent = getSubscribeIntent(config, pubKey);
        sendBroadcast(intent, PERM_CONDITION_PUBLISHER_ADMIN);
        SubscribeTimer subscribeTimer = new SubscribeTimer(this, intent, 0);
        subscribeTimer.start(TIMEOUT_VALUE);
    }



    /*
     * Forms the intent for Subscribe command
     */
    private Intent getSubscribeIntent(String config, String pubKey) {
        Intent intent = new Intent(ACTION_CONDITION_PUBLISHER_REQUEST);
        intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_COMMAND, COMMAND_SUBSCRIBE);
        intent.putExtra(EXTRA_REQUEST_ID, config+pubKey);
        intent.putExtra(EXTRA_PUBLISHER_KEY, pubKey);
        intent.putExtra(EXTRA_CONSUMER, SA_CORE_KEY);
        intent.putExtra(EXTRA_CONSUMER_PACKAGE, PACKAGE);
        return intent;
    }

    /**
     * Sends the cancel command for the given config and publisher key
     * @param config - Configuration for which cancel has to be sent
     * @param pubKey - Publisher key of the Command Publisher
     */
    private void sendCancelCommand(String config, String pubKey) {
        if(LOG_DEBUG) Log.d(TAG, "sendCancelCommand :" + config + ":" + pubKey);

        Intent intent = new Intent(ACTION_CONDITION_PUBLISHER_REQUEST);
        intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_COMMAND, COMMAND_CANCEL);
        intent.putExtra(EXTRA_REQUEST_ID, config+pubKey);
        intent.putExtra(EXTRA_PUBLISHER_KEY, pubKey);
        intent.putExtra(EXTRA_CONSUMER, SA_CORE_KEY);
        sendBroadcast(intent, PERM_CONDITION_PUBLISHER_ADMIN);
    }

    /**
     * Provides the utility for starting and stopping the timer for subscribe content
     */
    private static class SubscribeTimer {
        private PendingIntent mPi = null;
        private Intent mSubscribeIntent = null;
        private int mRetryCount = 0;
        private Context mContext;

        public SubscribeTimer(Context context, Intent subscribeIntent, int retryCount) {
            mContext = context;
            mSubscribeIntent = subscribeIntent;
            mRetryCount = retryCount;
            mPi = getPi();
        }

        /*
         * Forms the pending intent to be used for Subscribe Timeout
         */
        private PendingIntent getPi() {
            String pubKey = mSubscribeIntent.getStringExtra(EXTRA_PUBLISHER_KEY);
            String config = mSubscribeIntent.getStringExtra(EXTRA_CONFIG);
            Intent amIntent = new Intent(ACTION_TIMEOUT+pubKey.hashCode()+config.hashCode());
            amIntent.putExtra(EXTRA_INTENT_URI, mSubscribeIntent.toUri(0));
            amIntent.putExtra(EXTRA_RETRY_COUNT, mRetryCount);
            amIntent.setClass(mContext, ModeAdService.class);
            PendingIntent pi = PendingIntent.getService(mContext, 0, amIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            return pi;
        }

        /*
         * Start the timer
         */
        public void start(long timeOut) {
            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + timeOut, mPi);
        }

        /*
         * Stop the timer
         */
        public void stop() {
            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            am.cancel(mPi);
        }
    }

}
