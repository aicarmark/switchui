/*
 * @(#)LowBatterySaver.java
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

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

import com.motorola.contextual.rule.publisher.db.PublisherPersistence;
import com.motorola.contextual.rule.receivers.LbsBatteryStateMonitor;

/** This class contains the logic to infer Low Battery Saver Rule
*
*<code><pre>
* Logic:
* 1. Battery level falls below 25
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
public class LowBatterySaver extends Inference {

    public LowBatterySaver() {
        super(LowBatterySaver.class.getSimpleName());
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

        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,100);
            if (LOG_INFO) Log.i(TAG, "BatteryLevel=" + level);

            result = (level < 25);

            if(result){

                // Force Set the inferred state of LBS
                PublisherPersistence.setInferredState(ct, getRuleKey(), RuleState.INFERRED);

                // kick off BE now
                new BatterySaver().initInference(ct);
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
    }

    /*
     * (non-Javadoc)
     *
     * @see com.motorola.contextual.rule.inferer.IInference#setRuleKey()
     */
    @Override
    public void setRuleKey() {
        mRuleKey = RULE_KEY_LBS;
    }

    @Override
    protected void initRegister(Context ct, boolean state) {
        registerListener(ct, LbsBatteryStateMonitor.class.getName(), state);
    }

    @Override
    public String getUpdatedConfig(Context ct, String pubKey, String config) {
        return null;
    }
}