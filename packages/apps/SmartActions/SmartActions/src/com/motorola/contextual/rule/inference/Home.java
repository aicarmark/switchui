/*
 * @(#)Home.java
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

import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.publisher.XmlConfigUpdater;
import com.motorola.contextual.rule.receivers.HomeStateMonitor;

/** This class contains the logic to infer Home Rule
*
*<code><pre>
* Logic:
* 1. Home inferred intent is received.
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
public class Home extends Inference {

    public Home() {
        super(Home.class.getSimpleName());
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

        if (action.equals(CoreConstants.LOCATION_INFERRED_ACTION)) {

            String poi = intent.getStringExtra(CoreConstants.LOCATION_TAG);
            if(poi == null) return result;
            result = poi.equals(CoreConstants.HOME_POI);
        }

        if(result){
            // we need to update location config
            updateConfigs(ct);
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
        mRuleKey = RULE_KEY_HOME;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.contextual.rule.inferer.Inferencing#register(android.content.Context, boolean)
     */
    protected void initRegister(Context ct, boolean state){
        registerListener(ct, HomeStateMonitor.class.getName(), state);
   }

    /**
     * Adds the location config in the rule XML
     *
     * @param ct - context
     */
    private void updateConfigs(Context ct){

        Set<String> pubKeySet = new HashSet<String>();
        pubKeySet.add(CoreConstants.PUBLISHER_KEY_LOCATION);

        XmlConfigUpdater.updateOrAddConfig(ct, getRuleKey(), pubKeySet);
    }

    /** returns the config with location set to Home poi
     *
     * @param ct - context
     * @param pubKey - pub key of the publisher that needs to be updated
     * @param config - null
     * @return - Home location config
     * 
     * (non-Javadoc)
     * @see com.motorola.contextual.rule.inference.Inference#getUpdatedConfig(android.content.Context, java.lang.String, java.lang.String)
     */
    public String getUpdatedConfig(Context ct, String pubKey, String config){

        if(pubKey != null && pubKey.endsWith(PUBLISHER_KEY_LOCATION))
            config = HOME_LOCATION_CONFIG;

        if (LOG_DEBUG)Log.i(TAG, "Config=" + config + " for=" + pubKey);

        return config;
    }
}