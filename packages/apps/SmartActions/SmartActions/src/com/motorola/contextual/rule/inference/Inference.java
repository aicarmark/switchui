/*
 * @(#)Inferencing.java
 *
 * (c) COPYRIGHT 2010-2013 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/06/14 NA                Initial version
 *
 */
package com.motorola.contextual.rule.inference;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.publisher.RulePublisher;
import com.motorola.contextual.rule.publisher.db.PublisherPersistence;
import com.motorola.contextual.rule.receivers.RegisterListeners;

/** This abstract class contains the base logic for all inferencing use cases.
 * This class also contains few abstract methods that its child classes must define
*
*<code><pre>
*
* CLASS:
*   extends None
*   implements
*    - Constants - local constants
*    - CoreConstants - constants from core and other publishers
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
public abstract class Inference implements Constants, CoreConstants{

    protected String TAG        = RP_TAG + Inference.class.getSimpleName();
    protected String mRuleKey   = null;

    public Inference(String tag) {
        setRuleKey();
        TAG = RP_TAG + tag;
    }

    /**
     * Method to init Inferencing
     *
     * @param ct - context
     */
    public final static void start(Context ct){

        new Sleep().initInference(ct);
        // IKJBREL1-1687 : Removing NBS inference
        // new NightTimeBatterySaver().initInference(ct);
        new LowBatterySaver().initInference(ct);
        new BatterySaver().initInference(ct);
        new Meeting().initInference(ct);
        new Home().initInference(ct);
    }


    /**
     * This method is used to register listeners for the inference use case
     *
     * @param ct - context
     */
    public void initInference(Context ct) {

        if(getInferredState(ct)) return;

        if (LOG_VERBOSE) Log.i(TAG, "Init Inference");
        initRegister(ct, true);
    }

    /**
     * this method publishes a rule based on the inference logic of the
     * child class
     *
     * @param ct
     * @param intent
     */
    public final void inferAndPublish(Context ct, Intent intent){

        // check if this has been inferred or not
        if(getInferredState(ct)) return;

        // call derived class method to infer
        if(infer(ct, intent)){

            if (LOG_INFO) Log.i(TAG,"Inferred, shutting down...");
            cleanUp(ct);

            RulePublisher.publishSuggestedRule(ct, getRuleKey());

            DebugTable.writeToDebugViewer(ct, DebugTable.Direction.OUT, TAG, null, getRuleKey(), 
                    DBG_RP_TO_CORE, getRuleKey() + DBG_MSG_RULE_INFERRED, null, PUBLISHER_KEY, Constants.PUBLISHER_KEY);

        }
    }

    /**
     * returns the rule key
     *
     * @return the rule key.
     */
    protected final String getRuleKey(){

        if(mRuleKey == null) throw new InstantiationError();
        return mRuleKey;
    }

    /**
     * Read inferred state from DB
     *
     * @param ct - context
     * @return - inferred state
     */
    protected final boolean getInferredState(Context ct){
        return getInferredState(ct, getRuleKey());
    }

    /**
     * Read inferred state of the passed "key" from DB
     *
     * @param ct - context
     * @param key - rule key
     * @return - inferred state
     */
    protected final boolean getInferredState(Context ct, String key){

        boolean state = (PublisherPersistence.getInferredState(ct, key).equals(RuleState.INFERRED));
        if (LOG_INFO && state) Log.i(TAG, "Inferred state=" + state);
        return state;
    }

    /**
     * Process CP response or Android inference related broadcasts and
     * fwd it to the interested inference classes
     *
     * @param ct
     * @param intent
     */
    public static final void handleInferenceIntent(Context ct, Intent intent){

        String eventType = intent.getStringExtra(CoreConstants.EXTRA_EVENT_TYPE);
        if(eventType == null) return;

        // these are mostly Android broadcasts like LowBattery, etc
        if(eventType.equals(INFERENCE_INTENT)){

            String ruleKey = intent.getStringExtra(IDENTIFIER);
            if(ruleKey != null) getInstance(ruleKey).inferAndPublish(ct, intent);

        } else {

            String pubKey = intent.getStringExtra(CoreConstants.EXTRA_PUB_KEY);
            if(pubKey == null) return;

            // Notify event from CPs
            if(LOG_DEBUG) Log.d(RP_TAG + Inference.class.getSimpleName(), "pubKey=" + pubKey);
            if (pubKey.equals(PUBLISHER_KEY_TIMEFRAME)) {
                String config = getConfigFromIntent(intent);
                if(config.contains(CoreConstants.CONFIG_TF_NIGHT)){
                    new Sleep().inferAndPublish(ct, intent);
                    // IKJBREL1-1687 : Removing NBS inference
                    // new NightTimeBatterySaver().inferAndPublish(ct, intent);
                } else if(config.contains(CoreConstants.CONFIG_TF_DATE_CHANGE)){
                    new BatterySaver().inferAndPublish(ct, intent);
                }
            } else if (pubKey.equals(PUBLISHER_KEY_CALENDAR)
                    || pubKey.equals(PUBLISHER_KEY_MISSED_CALL)) {
                new Meeting().inferAndPublish(ct, intent);
            }
        }
    }

    /**
     * Method to register or unregister receiver to listen to generic broadcasts
     *
     * @param ct - context
     * @param classNameString - Class that contains the receiver
     * @param register - true or false
     */
    protected final void registerListener(Context ct, String classNameString, boolean register){

        RegisterListeners.register(ct, TAG, classNameString, register);
    }

    /**
     * Method to subscribe for CP state Notify events
     *
     * @param ct - context
     * @param pubKey - pub key
     * @param config - config to subscribe to
     * @param ruleKey - rule key
     * @param register - true to register
     */
    protected final void registerListener(Context ct, String pubKey, String config, boolean register){

        RegisterListeners.subscribeCpNotify(ct, TAG, pubKey, config, getRuleKey(), register);
    }

    /**
     * returns the config from the incoming intent
     *
     * @param intent
     * @return the config
     */
    protected static final String getConfigFromIntent(Intent intent){

        String config = null;

        String event = intent.getStringExtra(CoreConstants.EXTRA_EVENT_TYPE);
        if(event != null && event.equals(CoreConstants.SUBSCRIBE_RESPONSE)){

            config = intent.getStringExtra(CoreConstants.CONFIG_EXTRA);

        } else if(event != null && event.equals(CoreConstants.NOTIFY)) {

            HashMap<String, String> configStateMap = (HashMap<String, String>)intent
                .getSerializableExtra(CoreConstants.EXTRA_CONFIG_STATE_MAP);

            if(configStateMap != null && configStateMap.size() > 0) {
                config = configStateMap.keySet().toString();
            }
        }

        if (LOG_VERBOSE) Log.i(RP_TAG + Inference.class.getSimpleName(), "Config=" + config);

        // ensure that we do not send null
        if(config == null) config = EMPTY_STRING;

        return config;
    }

    /**
     * returns the config state from the incoming intent
     *
     * @param intent
     * @param config
     * @return the config state
     */
    protected final String getConfigStateFromIntent(Intent intent, String config){

        String state = null;

        String event = intent.getStringExtra(CoreConstants.EXTRA_EVENT_TYPE);
        if(event != null && event.equals(CoreConstants.SUBSCRIBE_RESPONSE)){

            String intentConfig = intent.getStringExtra(CoreConstants.CONFIG_EXTRA);
            if(intentConfig != null && intentConfig.equals(config)){
                state = intent.getStringExtra(CoreConstants.EXTRA_STATE);

                if (LOG_DEBUG) Log.d(TAG, intentConfig + "=" + state);
            }
        } else if(event != null && event.equals(CoreConstants.NOTIFY)) {

            HashMap<String, String> configStateMap = (HashMap<String, String>)intent
                .getSerializableExtra(CoreConstants.EXTRA_CONFIG_STATE_MAP);

            if(configStateMap == null || configStateMap.size() == 0) {
                Log.e(TAG, "null config=" + (configStateMap==null));
                if (configStateMap != null) Log.i(TAG, "config="+configStateMap.size());
            } else {

                if(configStateMap.containsKey(config)){
                    state = configStateMap.get(config);

                    if (LOG_DEBUG) Log.d(TAG, config + "=" + state);
                }

            }
        }

        return state;
    }

    /**
     * creates the related object
     *
     * @param ruleKey - rule key
     * @return - inference use case object
     */
    public final static Inference getInstance(String ruleKey){

        // Ideally, this exception should be caught in unit testing
        if(ruleKey == null) throw new IllegalArgumentException();

        if(ruleKey.equals(RULE_KEY_HOME)){
            return new Home();
        } else if(ruleKey.equals(RULE_KEY_SLEEP)){
            return new Sleep();
        } else if(ruleKey.equals(RULE_KEY_MEETING)){
            return new Meeting();
        } else if(ruleKey.equals(RULE_KEY_LBS)){
            return new LowBatterySaver();
        } else if(ruleKey.equals(RULE_KEY_BS)){
            return new BatterySaver();
        // IKJBREL1-1687 : Removing NBS inference
        //} else if(ruleKey.equals(RULE_KEY_NBS)){
        //    return new NightTimeBatterySaver();
        } else {
            Log.e(RP_TAG + Inference.class.getName(), "Invalid rule key=" + ruleKey);
            throw new InstantiationError();
        }
    }

    /**
     * Actual inference logic goes here
     *
     * @param ct - content
     * @param intent - broadcast intent from publishers/android
     * @return
     */
    protected abstract boolean infer(Context ct, Intent intent);

    /**
     * unregister listeners, shut down inference.
     *
     * @param ct - context
     */
    protected abstract void cleanUp(Context ct);

    /**
     * derived class is required to set the rule key here
     *
     * @param key - rule key
     */
    protected abstract void setRuleKey();

    /**
     * Method to register/unregister listeners
     *
     * @param ct - context
     * @param state - true to register
     */
    protected abstract void initRegister(Context ct, boolean state);

    /**
     * Method to update the config for any condition/action (not applicable
     * for all inference use cases)
     *
     * @param ct - context
     * @param pubKey - pub key
     * @param config - config
     * @return - updated config
     */
    public abstract String getUpdatedConfig(Context ct, String pubKey, String config);
}
