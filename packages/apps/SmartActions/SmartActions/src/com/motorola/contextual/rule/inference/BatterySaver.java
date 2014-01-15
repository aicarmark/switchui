/*
 * @(#)BatteryExtender.java
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

import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.Util;
import com.motorola.contextual.rule.publisher.db.PublisherPersistence;
import com.motorola.contextual.rule.receivers.BsBatteryStateMonitor;


/** This class contains the logic to infer Battery Extender Rule
*
*<code><pre>
* Logic:
* 1. Init after LBS has been inferred
* 2. Wait for 7 days to pass
* 3. Battery level goes above 80
* 4. Battery level goes below 20
* 5. Repeat step 3-4 two more times (total 3 times) and then infer
*
* CLASS:
* extends Inferencing
*
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
public class BatterySaver extends Inference {

    private static final long TIME_7_DAYS_IN_MSECS = 604800000L;

    public BatterySaver() {
        super(BatterySaver.class.getSimpleName());
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.contextual.rule.inferer.Inferencing#initInference(android.content.Context)
     */
    @Override
    public void initInference(Context ct) {

        if (! getInferredState(ct, RULE_KEY_LBS)) {
            if (LOG_DEBUG) Log.d(TAG, "LBS not yet inferred, Do not INIT");
            return;
        }

        super.initInference(ct);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.motorola.contextual.rule.inferer.IInference#infer(android.content
     * .Context, android.content.Intent)
     */
    protected boolean infer(Context ct, Intent intent) {

        boolean result = false;

        String action = intent.getAction();
        if(action == null) return result;

        if (action.equals(Constants.PUBLISHER_KEY)) {
            handleTimeframeEvent(ct, intent);
        } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            result = handleBatteryChangedEvent(ct, intent);
        }

        return result;
    }

    /**
     * handles Time frame Event
     *
     * @param ct - context
     * @param intent - incoming intent
     */
    private void handleTimeframeEvent(Context ct, Intent intent){

        String stateString = getConfigStateFromIntent(intent, CONFIG_TF_DATE_CHANGE);
        if(Util.isNull(stateString)) return;

        boolean state = Boolean.parseBoolean(stateString);

        if(state){
            long inferredTime = PublisherPersistence.getRuleInferredTime(ct, RULE_KEY_LBS);
            if (LOG_VERBOSE) Log.i(TAG, "pubTime=" + inferredTime );

            long time = new Date().getTime();
            if (LOG_VERBOSE) Log.i(TAG, "Time=" + time );

            boolean sevenDays = (time - inferredTime) >= TIME_7_DAYS_IN_MSECS;
            if (LOG_INFO) Log.i(TAG, "7 days=" + sevenDays );

            if(sevenDays){
                // start listening to battery levels
                registerListener(ct, BsBatteryStateMonitor.class.getName(), true);
            }
        }
    }

    /**
     * This method implements the battery level inference logic
     *
     * @param ct - context
     * @param intent - incoming intent
     * @return - true if inferred
     */
    private boolean handleBatteryChangedEvent(Context ct, Intent intent){
        boolean result = false;

        // we don't care about the stick ones
        if(intent.getBooleanExtra(IS_STICKY, false)) return result;

        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,100);

        if (LOG_DEBUG) Log.d(TAG, "BatteryLevel=" + level);
        if (level < 20) {
            Util.setSharedPrefStateValue(ct, INFERENCE_STATE_BE_GT_20, false);
            int count = Util.getSharedPrefIntValue(ct, INFERENCE_BE_LT_20);

            count++;
            if (LOG_DEBUG) Log.d(TAG, "count=" + count);
            if(! Util.getSharedPrefStateValue(ct, INFERENCE_STATE_BE_LT_20)){
                if (LOG_INFO) Log.i(TAG, "count set=" + count);
                Util.setSharedPrefIntValue(ct, INFERENCE_BE_LT_20, count);
                Util.setSharedPrefStateValue(ct, INFERENCE_STATE_BE_LT_20, true);
            }

            if( count > 2){
                count = Util.getSharedPrefIntValue(ct, INFERENCE_BE_GT_20);
                if(count > 2) result = true;
            }

        } else if (level > 80) {
            Util.setSharedPrefStateValue(ct, INFERENCE_STATE_BE_LT_20, false);
            int count = Util.getSharedPrefIntValue(ct, INFERENCE_BE_GT_20);

            count++;
            if (LOG_DEBUG) Log.d(TAG, "count=" + count);
            if(! Util.getSharedPrefStateValue(ct, INFERENCE_STATE_BE_GT_20)){
                if (LOG_INFO) Log.i(TAG, "count set=" + count);
                Util.setSharedPrefIntValue(ct, INFERENCE_BE_GT_20, count);
                Util.setSharedPrefStateValue(ct, INFERENCE_STATE_BE_GT_20, true);
            }

            if( count > 2){
                count = Util.getSharedPrefIntValue(ct, INFERENCE_BE_LT_20);
                if(count > 2) result = true;
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.motorola.contextual.rule.inferer.IInference#unregisterAllListeners
     * (android.content.Context)
     */
    public void cleanUp(Context ct) {

        initRegister(ct, false);
        registerListener(ct, BsBatteryStateMonitor.class.getName(), false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.motorola.contextual.rule.inferer.IInference#setRuleKey()
     */
    @Override
    public void setRuleKey() {
        mRuleKey = RULE_KEY_BS;
    }

    @Override
    protected void initRegister(Context ct, boolean state) {
        registerListener(ct, CoreConstants.PUBLISHER_KEY_TIMEFRAME,
                CoreConstants.CONFIG_TF_DATE_CHANGE, state);
    }

    @Override
    public String getUpdatedConfig(Context ct, String pubKey, String config) {
        return null;
    }
}