/*
 * @(#)Sleep.java
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

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.Util;
import com.motorola.contextual.rule.publisher.XmlConfigUpdater;
import com.motorola.contextual.rule.receivers.SleepRingerStateMonitor;

/** This class contains the logic to infer Battery Extender Rule
*<code><pre>
* Logic:
* 1. Time frame is Night
* 2. User changes volume level to silent or vib
* 3. Infer the rule with user changed ringer config
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
public class Sleep extends Inference {

    private static boolean sInNightTimeFrame            = false;
    private static String sInferredRingerMode           = null;

    public Sleep() {
        super(Sleep.class.getSimpleName());
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

            String stateString = getConfigStateFromIntent(intent, CONFIG_TF_NIGHT);
            if(Util.isNull(stateString)) return result;

            boolean state = Boolean.parseBoolean(stateString);
            if (LOG_INFO) Log.i(TAG, "Night="+state);

            if(state != sInNightTimeFrame){
                sInNightTimeFrame = state;
                registerListener(ct, SleepRingerStateMonitor.class.getName(), state);
            }

        } else if (action.equals(AudioManager.RINGER_MODE_CHANGED_ACTION)) {
            int ringerMode = intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, -1);

            if (ringerMode == AudioManager.RINGER_MODE_SILENT
                    || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {

                // ringerMode should not be sticky and volume should not be set by vol actions
                result = !(intent.getBooleanExtra(IS_STICKY, false) ||
                        ct.getSharedPreferences(ACTIONS_PACKAGE, 0).getBoolean(KEY_IGNORE_VOLUME_CHANGE, false));

                if (LOG_INFO) Log.i(TAG, "RingerMode=" + ringerMode);

                if(result){
                    updateConfigs(ct, ringerMode);
                }
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.motorola.contextual.rule.inferer.IInference#cleanUp
     * (android.content.Context)
     */
    public void cleanUp(Context ct) {

        initRegister(ct, false);
        registerListener(ct, SleepRingerStateMonitor.class.getName(), false);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.motorola.contextual.rule.inferer.IInference#setRuleKey()
     */
    @Override
    public void setRuleKey() {
        mRuleKey = RULE_KEY_SLEEP;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.contextual.rule.inferer.Inferencing#register(android.content.Context, boolean)
     */
    protected void initRegister(Context ct, boolean state){

        // if NBS is not inferred, do not cancel TF config subscription
        if(! state && ! getInferredState(ct, RULE_KEY_NBS))
            return;

        registerListener(ct, CoreConstants.PUBLISHER_KEY_TIMEFRAME,
                CoreConstants.CONFIG_TF_NIGHT, state);
    }

    /**
     * updates the Ringer config in the rule XML
     *
     * @param ct - context
     * @param ringerMode - inferred ringer mode
     */
    private void updateConfigs(Context ct, int ringerMode){

        sInferredRingerMode = ringerMode == AudioManager.RINGER_MODE_SILENT?
                "Silent":"Vibrate";

        Set<String> pubKeySet = new HashSet<String>();
        pubKeySet.add(CoreConstants.PUBLISHER_KEY_RINGER_ACTION);

        XmlConfigUpdater.updateOrAddConfig(ct, getRuleKey(), pubKeySet);
    }

    /**
     * returns the updated config as per the inferred logic
     *
     * @param ct - context
     * @param pubKey - pub key of the publisher that needs to be updated
     * @param ringerConfig - original ringer config
     * @return - updated ringer config
     * 
     * (non-Javadoc)
     * @see com.motorola.contextual.rule.inference.Inference#getUpdatedConfig(android.content.Context, java.lang.String, java.lang.String)
     */
    public String getUpdatedConfig(Context ct, String pubKey, String ringerConfig){

        if(ringerConfig == null) return ringerConfig;

        try {
            Intent intent = Intent.parseUri(ringerConfig, 0);
            if (intent != null) {
                intent.putExtra("MODE_STRING", sInferredRingerMode);
                if (LOG_DEBUG) Log.d(TAG, "MODE_STRING=" + sInferredRingerMode);
                ringerConfig = intent.toUri(0);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return ringerConfig;
    }
}
