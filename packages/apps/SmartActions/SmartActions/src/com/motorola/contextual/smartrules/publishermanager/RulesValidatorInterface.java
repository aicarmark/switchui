/*
 * @(#)PublisherList.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/04/12                   Initial version
 *
 */
package com.motorola.contextual.smartrules.publishermanager;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.service.SmartRulesService;
import com.motorola.contextual.smartrules.util.Util;

/** Provides interface to inform RulesValidator about the change in the
 * Validation state of the Publishers used in the Rules
 *<code><pre>
 * CLASS:
 *     RulesValidatorInterface
 * Interface:
 * 		PublisherManagerConstants,
 * 		DbSyntax
 *
 * RESPONSIBILITIES:
 * 		Provides method to inform RulesValidator about the change inthe
 * 		Validation state of the Publishers used in the Rules
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class RulesValidatorInterface implements PublisherManagerConstants, DbSyntax {

    private static final String TAG = RulesValidatorInterface.class.getSimpleName();
    private Context mContext;
    private static final String INVALID_PUBLISHER = "INVALID_PUBLISHER";

    /**
     * Constructor
     * @param context Context of the caller
     */
    public RulesValidatorInterface(Context context) {
        mContext = context;
    }

    /**
     * This method is used to poke the RulesValidator to validate all the Rules
     * that uses the given publisher key and config combination
     * @param silent tells whether it is a silent rule
     * @param reqId Request id
     * @param deleteInvalidRule Denotes whether to delete the Invalid rules
     */
    public void pokeRulesValidatorForPublisher(String ruleKey, String ruleSrc, boolean silent, int importType,
            String reqId, String publisherUpdateReason) {
        String validateResult = new RulesValidator(mContext).validateRule(ruleKey);
        if(LOG_DEBUG) Log.d(TAG, "pokeRulesValidatorForPublisher : " + ruleKey + ":" + validateResult);
        if(!validateResult.equals(RuleTable.Validity.INPROGRESS)) {
            validateResult = (validateResult.equals(RuleTable.Validity.UNAVAILABLE) ||
                              validateResult.equals(RuleTable.Validity.BLACKLISTED)) ?
                             RuleTable.Validity.INVALID:validateResult;
            boolean isManual = (RulePersistence.isRulePsuedoManualOrManual(mContext, ruleKey));
            if(LOG_DEBUG) Log.d(TAG, "pokeRulesValidatorForPublisher : isManual" + isManual);
            updateRuleValidity(ruleKey, validateResult, isManual, publisherUpdateReason);
            Rule rule = RulePersistence.fetchRuleOnly(mContext, ruleKey);
            sendValidateResultForSuggestion(ruleKey, ruleSrc, silent,
                                            validateResult);
            sendRuleUpdatedIntentForWidgets(ruleKey, importType, rule);
            sendValidateResult(ruleKey, reqId, validateResult);
            launchModeAd(mContext, ruleKey, importType, validateResult, rule, isManual);
        }
    }

    /*
     * This method is called to Update the validation state of Rule to Valid
     */
    private void updateRuleValidity(String ruleKey, String validateResult, boolean isManual, String publisherUpdateReason) {
        if(LOG_INFO) Log.i(TAG, "updateRuleValidity : " + ruleKey + " Validity : " + validateResult +
			"\n publisherUpdateReason : " + publisherUpdateReason);
        ContentResolver cr = mContext.getContentResolver();
        ContentValues contentValues = new ContentValues();
        String where = RuleTableColumns.KEY + EQUALS + Q + ruleKey + Q;
        if(publisherUpdateReason == null ||
			(publisherUpdateReason != null && !publisherUpdateReason.equals(LOCALE_CHANGED))){
	        Intent serviceIntent = new Intent(mContext, SmartRulesService.class);
	        serviceIntent.putExtra(MM_RULE_KEY,ruleKey);

	        if(isManual) {
	            contentValues.put(RuleTableColumns.RULE_TYPE, RuleTable.RuleType.MANUAL);
	            contentValues.put(RuleTableColumns.ENABLED, RuleTable.Enabled.DISABLED);
	            serviceIntent.putExtra(MM_DISABLE_RULE, true);
	            boolean isActive = RulePersistence.isRuleActive(mContext,ruleKey);
	            if(isActive)
	                serviceIntent.putExtra(MM_RULE_STATUS, FALSE);
	            if(LOG_INFO) Log.i(TAG, "Marking rule as disabled : " + ruleKey);
	            mContext.startService(serviceIntent);
	        } else {
	            contentValues.put(RuleTableColumns.RULE_TYPE, RuleTable.RuleType.AUTOMATIC);
	        }
        }
        contentValues.put(RuleTableColumns.VALIDITY, validateResult);
        cr.update(RuleTable.CONTENT_URI, contentValues, where, null);
    }

    public static void launchModeAd(Context context, String ruleKey, int importType,
                              String validateResult, Rule rule, boolean isManual) {
        if (rule == null) {
            Log.e(TAG, "launchModeAd : rule is null; returning...");
            return;
        }
        
        String flags = rule.getFlags();
        if(LOG_DEBUG) Log.d(TAG, " launchModeAd : with : + " + ruleKey +
                                ":" + flags + ":" + validateResult + ":" + rule.getEnabled());
        // Launch this intent for user created rules/ accepted suggestions or samples and
        // not for samples.
        if(!isManual && rule.getEnabled() == RuleTable.Enabled.ENABLED &&
                validateResult.equals(RuleTable.Validity.VALID) && (Util.isNull(flags) ||
                        flags.length() == 0 ||
                        flags.equals(RuleTable.Flags.INVISIBLE))) {
            // Launch the Mode AD to subscribe to Condition publishers.
            Intent createIntent = new Intent(ACTION_LAUNCH_MODE_ACT_DEACTIVATOR);
            createIntent.putExtra(EXTRA_RULE_KEY, ruleKey);
            createIntent.putExtra(EXTRA_LAUNCH_REASON, RULE_CREATED);
            context.sendBroadcast(createIntent);
        } else if(rule.getEnabled() == RuleTable.Enabled.ENABLED &&
                  validateResult.equals(RuleTable.Validity.INVALID) &&
                  (Util.isNull(flags) || flags.equals(RuleTable.Flags.INVISIBLE)) || isManual) {
            long ruleId = rule.get_id();
            ArrayList<String> configPubKeyList = RulePersistence.getConfigPubKeyListForDeletion(context, ruleId);
            if(configPubKeyList != null) {
                // Launch the Mode AD to cancel to Condition publishers.
                Intent intent = new Intent(ACTION_LAUNCH_MODE_ACT_DEACTIVATOR);
                intent.putExtra(EXTRA_RULE_KEY, ruleKey);
                intent.putExtra(EXTRA_LAUNCH_REASON, RULE_DELETED);
                intent.putExtra(EXTRA_PREV_CONFIG_PUBKEY_LIST, configPubKeyList);
                context.sendBroadcast(intent);
            }
        }
    }

    /**
     * This method is used to poke the RulesValidator to validate a specific rule
     * @param ruleKey : Rule Key to validate
     * @param context calling context
     */
    public static String updateRuleValidity(Context context, String ruleKey) {
        String validateResult = new RulesValidator(context).validateRule(ruleKey);
        if(LOG_DEBUG) Log.d(TAG, "updateRuleValidity : " + ruleKey + ":" + validateResult);
        if(!validateResult.equals(RuleTable.Validity.INPROGRESS)) {
            validateResult = (validateResult.equals(RuleTable.Validity.UNAVAILABLE) ||
                              validateResult.equals(RuleTable.Validity.BLACKLISTED)) ?
                             RuleTable.Validity.INVALID:validateResult;
            boolean isManual = (RulePersistence.isRulePsuedoManualOrManual(context, ruleKey));
            if(LOG_DEBUG) Log.d(TAG, "updateRuleValidity : isManual" + isManual);
            
            ContentResolver cr = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            String where = RuleTableColumns.KEY + EQUALS + Q + ruleKey + Q;
                
            contentValues.put(RuleTableColumns.VALIDITY, validateResult);
            cr.update(RuleTable.CONTENT_URI, contentValues, where, null);
        }
        return validateResult;
    }
    
    /** 
    /*
     * If reqId is not null, then send ACTION_RULE_VALIDATED intent to RulesValidatorService
     */
    private void sendValidateResult(String ruleKey, String reqId,
                                    String validateResult) {
        if(LOG_DEBUG) Log.d(TAG,"sendValidateResult " + ruleKey + COLON + reqId + COLON + validateResult);
        if(reqId != null && !reqId.equals("null")) {
            Intent validateResponseIntent = new Intent(ACTION_RULE_VALIDATED);
            validateResponseIntent.setClass(mContext, RulesValidatorService.class);
            validateResponseIntent.putExtra(EXTRA_RULE_STATUS,
                                            (validateResult.equals(RuleTable.Validity.VALID) ? SUCCESS : FAILURE +COLON+INVALID_PUBLISHER));
            validateResponseIntent.putExtra(EXTRA_RULE_KEY, ruleKey);
            validateResponseIntent.putExtra(EXTRA_RESPONSE_ID, reqId);
            mContext.startService(validateResponseIntent);
        }
    }

    /*
     * If the rules import type is INFERRED_ACCEPTED, this method sends RULE_MODIFIED_MESSAGE
     * intent that are used by Widgets
     */
    private void sendRuleUpdatedIntentForWidgets(String ruleKey, int importType, Rule rule) {
        if(rule != null) {
            Intent widgetUpdateIntent = new Intent(RULE_MODIFIED_MESSAGE);
            widgetUpdateIntent.putExtra(RuleTable.Columns.KEY, ruleKey);
            widgetUpdateIntent.putExtra(RuleTable.Columns._ID, rule.get_id());
            widgetUpdateIntent.putExtra(RuleTable.Columns.ACTIVE, rule.getActive());
            widgetUpdateIntent.putExtra(RuleTable.Columns.ENABLED, rule.getEnabled());
            widgetUpdateIntent.putExtra(RuleTable.Columns.RULE_TYPE, rule.getRuleType());
            widgetUpdateIntent.putExtra(RuleTable.Columns.ICON, rule.getIcon());
            widgetUpdateIntent.putExtra(RuleTable.Columns.NAME, rule.getName());
            mContext.sendBroadcast(widgetUpdateIntent, SMART_RULES_PERMISSION);
        }
    }

    /*
     * If rule is not set as silent, then send INTENT_RULES_VALIDATED intent
     */
    private void sendValidateResultForSuggestion(String ruleKey,
            String ruleSrc, boolean silent, String validateResult) {
        if(!silent) {
            HashMap<String, ArrayList<String>> validRuleKeyMap = new HashMap<String, ArrayList<String>>();

            ArrayList<String> validRuleKeyList = new ArrayList<String>();
            ArrayList<String> validRuleKeySrcList = new ArrayList<String>();
            ArrayList<String> validRuleKeyStatusList = new ArrayList<String>();

            validRuleKeyMap.put(KEY_RULE_KEY, validRuleKeyList);
            validRuleKeyMap.put(KEY_RULE_SOURCE, validRuleKeySrcList);
            validRuleKeyMap.put(KEY_RULE_STATUS, validRuleKeyStatusList);

            validRuleKeyList.add(ruleKey);
            validRuleKeySrcList.add(ruleSrc);
            validRuleKeyStatusList.add(Boolean.toString(validateResult.equals(RuleTable.Validity.VALID)));

            Intent intent = new Intent(INTENT_RULES_VALIDATED);
            intent.putExtra(EXTRA_RULE_INFO,validRuleKeyMap);
            mContext.sendBroadcast(intent);
            if(LOG_DEBUG) Log.d(TAG,"validateRules sending INTENT_RULES_VALIDATED intent with "
                                    + "\nvalidRuleKeyMap : " + validRuleKeyMap.toString());
        }
    }
}
