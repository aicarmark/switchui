/*
 * @(#)BackgroundData.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * w30219       2012/06/07                     Initial version
 *
 */

package com.motorola.contextual.actions;

import com.motorola.contextual.pickers.actions.BackgroundDataActivity;
import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class implements the StatefulActionInterface for Background Sync
 * 
 * <code><pre>
 * 
 * CLASS:
 *     Extends BinarySetting, implements StatefulActionInterface
 * 
 * RESPONSIBILITIES:
 *     Overrides setState for setting Background Sync
 *     Inherits the rest of the methods from BinarySetting
 * 
 * COLLABORATORS:
 *     None
 * 
 * USAGE:
 *     See each method.
 * 
 * </pre></code>
 */

public final class BackgroundData extends BinarySetting implements Constants {

    private static final String TAG = TAG_PREFIX
            + BackgroundData.class.getSimpleName();
    public static final String BD_ACTION_KEY = "com.motorola.contextual.actions.BackgroundData";
    public static final String CONFIG_DISABLE = "#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;B.state=false;i.mode=2;S.type=com.motorola.contextual.actions.Sync;end";
    public static final String CONFIG_ENABLE = "#Intent;d.com.motorola.smartactions.intent.extra.CONFIG_VERSION=1.0;B.state=true;i.mode=0;S.type=com.motorola.contextual.actions.Sync;end";
    public static final String ENABLE = "Enable";
    public static final String DISABLE = "Disable";
    public static final String OFF = "Off";

    public static final String RULE_STATE_CHANGE = "com.motorola.contextual.smartrules.rulestate";
    public static final String MM_CHANGED_RULE = "com.motorola.contextual.smartrules.changedrule";
    public static final String MM_RULE_STATUS = "com.motorola.contextual.smartrules.rulestatus";
    public static final String MM_ENABLE_RULE = "com.motorola.contextual.smartrules.enablerule";
    public static final String MM_DISABLE_RULE = "com.motorola.contextual.smartrules.disablerule";
    private static final String ACTIVE_CHILD_RULE = "sd_active_child_rule";

    BackgroundData() {
        mActionKey = BD_ACTION_KEY;
    }
    
    @Override
    public Intent handleChildActionCommands(Context context, Intent intent) {
        if (ACTION_PUBLISHER_EVENT.equalsIgnoreCase(intent.getAction()) &&
                Sync.SYNC_ACTION_KEY.equalsIgnoreCase(intent.getStringExtra(EXTRA_PUBLISHER_KEY))) {
            //Disable the active child rule since we no longer control it
            if (LOG_DEBUG) Log.d(TAG, "Sync settings changed, we should disable child rule");
            setChildRuleState(context, null, false);
            StatefulActionHelper.sendSettingsChange(context, BD_ACTION_KEY, getSettingString(context));
        } else {
            String command = intent.getStringExtra(EXTRA_EVENT_TYPE);
            boolean saveDefault = intent.getBooleanExtra(EXTRA_SAVE_DEFAULT, false);
            Intent configIntent = null;
            String event = null;
            if (command.equalsIgnoreCase(COMMAND_FIRE)){
                configIntent = ActionHelper.getConfigIntent(intent.getStringExtra(EXTRA_CONFIG));
                event = EXTRA_FIRE_RESPONSE;
            }
            else if (command.equalsIgnoreCase(COMMAND_REVERT)) {
                configIntent = ActionHelper.getConfigIntent(Persistence.retrieveValue(context, BD_ACTION_KEY + DEFAULT_SUFFIX));
                event = EXTRA_REVERT_RESPONSE;
            }
            if (configIntent != null && 
                    configIntent.getStringExtra(EXTRA_TYPE) != null &&
                    configIntent.getStringExtra(EXTRA_TYPE).equals(Sync.SYNC_ACTION_KEY)) {
                //We should not have a child rule enabled now since Sync is controlled by Old Sync Action
                setChildRuleState(context, null, false);
                if (LOG_DEBUG) Log.d(TAG, "Old Sync Action, send it to Sync");
                
                String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
                Intent sendIntent = new Intent(Sync.SYNC_ACTION_KEY);
                sendIntent.putExtras(intent);
                context.sendBroadcast(sendIntent, PERM_ACTION_PUBLISHER);
                
                if (saveDefault) {
                    //Save default setting value
                    Intent defaultIntent = new Intent();
                    defaultIntent.putExtra(EXTRA_TYPE, Sync.SYNC_ACTION_KEY);
                    Persistence.commitValue(context, BD_ACTION_KEY + DEFAULT_SUFFIX, defaultIntent.toUri(0));
                    if (LOG_DEBUG) Log.d(TAG, "Saved default setting");
                }
                
                return ActionHelper.getResponseIntent(context, BD_ACTION_KEY,
                        SUCCESS, requestId, null, event);
            }
        }
        return null;    
    }
    
    public boolean setState(Context context, Intent intent) {
        String ruleKey = intent.getStringExtra(EXTRA_RULE_KEY);
        mState = intent.getBooleanExtra(EXTRA_STATE, true);
        
        mOldState = false;
        setChildRuleState(context, ruleKey, mState);
        return true;

    }

    private void setChildRuleState(Context context, String ruleKey,
            boolean ruleState) {
        if (LOG_DEBUG)
            Log.d(TAG, "setChildRuleState rule key is " + ruleKey);

        // We have to deactivate the current one first
        String activeRule = Persistence.removeValue(context, ACTIVE_CHILD_RULE);
        if (activeRule != null) {
            if (LOG_DEBUG)
                Log.d(TAG, "Disabling child rule " + activeRule);
            Intent revertIntent = new Intent(RULE_STATE_CHANGE);
            revertIntent.putExtra(MM_CHANGED_RULE, activeRule);
            revertIntent.putExtra(MM_RULE_STATUS, "false");
            revertIntent.putExtra(MM_DISABLE_RULE, true);
            context.sendBroadcast(revertIntent);
        }

        Intent intent = new Intent(RULE_STATE_CHANGE);
        if (ruleState) {
            // Enable the incoming rule
            if (LOG_DEBUG)
                Log.d(TAG, "Enabling child rule " + ruleKey);
            Persistence.commitValue(context, ACTIVE_CHILD_RULE, ruleKey);
            intent.putExtra(MM_CHANGED_RULE, ruleKey);
            intent.putExtra(MM_RULE_STATUS, "true");
            intent.putExtra(MM_ENABLE_RULE, true);
            context.sendBroadcast(intent);

        }
    }

    public String getActionString(Context context) {
        return context.getString(R.string.backgrounddata);
    }

    public Status handleSettingChange(Context context, Object obj) {
        return Status.NO_CHANGE;
    }

    public String[] getSettingToObserve() {
        return null;
    }
    
    @Override
    public String getDescription(Context context, Intent configIntent) {
                
        int mode = configIntent.getIntExtra(EXTRA_MODE, BackgroundDataActivity.State.NO_ITEM_SELECTED);
        
        String description = null;
        if (mode == BackgroundDataActivity.State.ALWAYS)
            description = context.getString(R.string.bd_always);
        else if (mode == BackgroundDataActivity.State.WHEN_USING)
            description = context.getString(R.string.bd_never);
        else if (mode == BackgroundDataActivity.State.WHEN_USING_DEVICE)
          description = context.getString(R.string.bd_when_using);
       
        if (LOG_INFO) Log.i(TAG, "getDescription : " + description);
        
        return description;
    }


}
