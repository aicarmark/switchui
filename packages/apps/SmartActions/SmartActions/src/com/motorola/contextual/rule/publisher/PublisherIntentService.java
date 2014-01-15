/*
 * @(#)PublisherService.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/02/14 NA                Initial version
 *
 */

package com.motorola.contextual.rule.publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.CoreConstants.RuleType;
import com.motorola.contextual.rule.inference.Inference;
import com.motorola.contextual.rule.publisher.db.PublisherPersistence;

/**
 * This class initializes RP db and loads the default rules.
 *
 * <code><pre>
 * CLASS:
 *  Extends IntentService
 *
 * RESPONSIBILITIES:
 *  - start a thread to init db
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 *
 * @author a21693
 *
 */
public final class PublisherIntentService extends IntentService implements Constants {

    private static final String TAG = RP_TAG + PublisherIntentService.class.getSimpleName();

    public PublisherIntentService(String name) {
        super(name);
    }

    public PublisherIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        if (LOG_DEBUG) Log.d(TAG, "Action=" + intent.getAction());

        Context ct = getApplicationContext();
        if (action.equals(CoreConstants.SA_CORE_INIT_COMPLETE)) {

            handleCoreInit(ct, intent);
        } else {

            handleIntent(ct, intent);
        }
    }

    /**
     * Method to handle core init event.
     *
     * <PRE>
     *  - This initializes RP
     *  - Publishes samples if required
     *  - Initializes inference logic
     * </PRE>
     *
     * @param ct
     * @param intent
     */
    private void handleCoreInit(Context ct, Intent intent){

        boolean coreDataCleared = false;

        try {
            coreDataCleared = Boolean.parseBoolean(intent
                    .getStringExtra(CoreConstants.EXTRA_DATA_CLEARED));
        } catch (Exception e) {
            Log.e(TAG, "Exception in parsing extra " + intent.toUri(0));
            e.printStackTrace();
        }

        // start loading default rules
        new RulesLoader(ct).loadRules(coreDataCleared);

        // publish inferred but not yet published rules
        RulePublisher.publishInferredRules(ct);

        // start inferring rules
        Inference.start(ct);
    }

    /**
     * Handle responses from different CPs or Core
     *
     * @param intent - intent
     */
    private void handleIntent(Context ct, Intent intent) {

        if(intent == null) return;

        String eventType = intent.getStringExtra(CoreConstants.EXTRA_EVENT_TYPE);
        if(LOG_DEBUG) Log.d(TAG, "Event Received=" + eventType);
        if(eventType == null) return;

        if(eventType.equals(CoreConstants.REFRESH_REQUEST)
                || eventType.equals(CoreConstants.PUBLISH_RULE_RESPONSE)){

            // handle refresh or rule publish response
            handleCoreBroadcasts(ct, intent);

        } else if (eventType.equals(CoreConstants.NOTIFY)
                || eventType.equals(CoreConstants.SUBSCRIBE_RESPONSE)
                || eventType.equals(Constants.INFERENCE_INTENT)){

            Inference.handleInferenceIntent(ct, intent);

        } else if (eventType.equals(CoreConstants.CANCEL_RESPONSE)){

            // just log for now
            String resp = intent.getStringExtra(CoreConstants.RESPONSE_ID_EXTRA);
            if(LOG_DEBUG) Log.d(TAG, "Response=" + resp);

        }else{
            if (LOG_INFO) Log.w(TAG,"Unidentified event : "+eventType);
        }
    }

    /**
     * Handle responses from different Core
     *
     * @param intent - intent
     */
    private void handleCoreBroadcasts(Context ct, Intent intent) {

        // fetch the event, identifier list / map and the response id
        String event = intent.getStringExtra(CoreConstants.EXTRA_EVENT_TYPE);
        if (LOG_INFO) Log.i(TAG, "Core Event Received=" + event);
        if (event == null) return;

        // Need to make appropriate changes after the Version
        // protocol approach is defined whether to ignore the
        // request or response if the version does not match
        float coreVersion = intent.getFloatExtra(CoreConstants.EXTRA_VERSION,
                CoreConstants.INVALID_VERSION);
        if (LOG_VERBOSE) Log.d(TAG,"Protocol Version="+coreVersion);

        if (event.equalsIgnoreCase(CoreConstants.REFRESH_REQUEST)){

            //Core to RP - key list to republish
            ArrayList<String> ruleKeyList = intent.
                 getStringArrayListExtra(CoreConstants.EXTRA_RULE_LIST);

            if(ruleKeyList  == null || ruleKeyList.size() == 0){
                if (LOG_INFO) Log.i(TAG, "ruleKeyList is null or empty");
            } else {

                DebugTable.writeToDebugViewer(ct, DebugTable.Direction.IN, TAG, null, null,
                        DBG_CORE_TO_RP, DBG_RP_REPUBLISH_RULES, ruleKeyList.toString(),
                        PUBLISHER_KEY, Constants.PUBLISHER_KEY);

                if (LOG_INFO) Log.i(TAG, "Republishing Select Rules");
                RulePublisher.publishSelectRules(ct, ruleKeyList);
            }
        }else if (event.equalsIgnoreCase(CoreConstants.PUBLISH_RULE_RESPONSE)){

            String respId = intent.getStringExtra(CoreConstants.EXTRA_RESPONSE_ID);
            if (LOG_INFO) Log.d(TAG,"Response Id="+respId);
            if(respId == null) return;

            // Core to RP
            HashMap<String,String> ruleKeyMap = (HashMap<String,String>) intent.
            getSerializableExtra(CoreConstants.EXTRA_RULE_STATUS);

            // Store the ver number
            if(respId.contains(RuleType.FACTORY) && ruleKeyMap != null){
                String[] ver = respId.split("=");
                if(ver.length > 1 && ver[1] != null){
                    float v = Float.parseFloat(ver[1]);
                    XmlUtils.storeXmlVersionToSharedPref(ct, v);
                }
            }

            // this condition should never be true
            if(ruleKeyMap == null || ruleKeyMap.size() == 0){
                if (LOG_INFO) Log.e(TAG, "Error: ruleKeyMap is null or empty");
                return;
            }

            int count = 0;
            Set<String> keySet = ruleKeyMap.keySet();
            for(String key : keySet){
                // fetch and then store the response
                String pubResp = ruleKeyMap.get(key);
                if (LOG_DEBUG) Log.d(TAG, key + "=" + pubResp);
                if(! respId.contains(RuleType.DELETE)){

                    if(pubResp != null)
                        PublisherPersistence.setPublishResponse(ct, key, pubResp);
                    else
                        Log.e(TAG, "Publish reponse is Null");
                    count++;
                }
            }
            if (LOG_INFO) Log.i(TAG, "Publish Response for " + count + " rules");
        }else{
            if (LOG_INFO) Log.w(TAG,"Unidentified event : "+event);
        }
    }
}