/*
 * @(#)NightTimeBatterySaver.java
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

import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.Util;
import com.motorola.contextual.rule.receivers.NbsBatteryStateMonitor;
import com.motorola.contextual.rule.receivers.NbsDisplayStateMonitor;

/** This class contains the logic to infer Battery Extender Rule
*
*<code><pre>
* Logic:
* 1. Time is Night
* 2. Display is off
* 3. Battery goes down by 20 levels
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
public class NightTimeBatterySaver extends Inference {

    private static boolean sInNightTimeFrame = false;
    private static boolean sDisplayOff       = false;

    public NightTimeBatterySaver() {
        super(NightTimeBatterySaver.class.getSimpleName());
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

        if (action.equals(PUBLISHER_KEY)) {
            handleTimeframeEvent(ct, intent);
        } else if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            result = handleBatteryChangedEvent(ct, intent);
        } else {

            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                sDisplayOff = true;
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                sDisplayOff = false;
            }

            if (LOG_INFO) Log.i(TAG, "DsiplayOff="+sDisplayOff);

            registerListener(ct, NbsBatteryStateMonitor.class.getName(), sDisplayOff);
        }

        return result;
    }

    /**
     * handle time frame event
     *
     * @param ct - context
     * @param intent - incoming intent
     */
    private void handleTimeframeEvent(Context ct, Intent intent){

        String stateString = getConfigStateFromIntent(intent, CONFIG_TF_NIGHT);
        if(Util.isNull(stateString)) return;

        Boolean state = Boolean.parseBoolean(stateString);
        if (LOG_INFO) Log.i(TAG, "Night " + state);

        if(sInNightTimeFrame != state){
            sInNightTimeFrame = state;
            registerListener(ct, NbsDisplayStateMonitor.class.getName(), state);
        }

        if(sDisplayOff && !sInNightTimeFrame)
            registerListener(ct, NbsBatteryStateMonitor.class.getName(), false);
    }

    /**
     * handle battery changed intent
     *
     * @param ct - context
     * @param intent - incoming intent
     * @return - true if inferred
     */
    private boolean handleBatteryChangedEvent(Context ct, Intent intent){

        boolean result = false;
        int newLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,100);

        // we DO care about the stick one here!
        if(intent.getBooleanExtra(IS_STICKY, false)){
            if (LOG_INFO) Log.i(TAG, "BatteryLevel=" + newLevel);

            // store the sticky/original value
            Util.setSharedPrefIntValue(ct, INFERENCE_NBS_BATT_LEVEL, newLevel);
        } else {
            int oldLevel = Util.getSharedPrefIntValue(ct, INFERENCE_NBS_BATT_LEVEL);

            if(newLevel > oldLevel){
                /*
                 * maybe the user charged the phone or cheatbroadcaster app
                 * faked a new level, store the new value
                 */
                Util.setSharedPrefIntValue(ct, INFERENCE_NBS_BATT_LEVEL, newLevel);
            } else {
                result = (oldLevel - newLevel) > 20;
                if (result && LOG_INFO) Log.i(TAG, "Inferred BatteryLevel=" + newLevel);
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
        registerListener(ct, NbsDisplayStateMonitor.class.getName(), false);
        registerListener(ct, NbsBatteryStateMonitor.class.getName(), false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.motorola.contextual.rule.inferer.IInference#setRuleKey()
     */
    @Override
    public void setRuleKey() {
        mRuleKey = RULE_KEY_NBS;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.contextual.rule.inferer.Inferencing#register(android.content.Context, boolean)
     */
    protected void initRegister(Context ct, boolean state){

        // if sleep is not inferred, do not cancel TF config subscription
        if(! state && ! getInferredState(ct, RULE_KEY_SLEEP))
            return;

        registerListener(ct, CoreConstants.PUBLISHER_KEY_TIMEFRAME,
                CoreConstants.CONFIG_TF_NIGHT, state);
    }

    @Override
    public String getUpdatedConfig(Context ct, String pubKey, String config) {
        return null;
    }
}