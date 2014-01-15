package com.motorola.contextual.smartrules.publishermanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.db.Schema.ConditionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.view.RuleConditionView;
import com.motorola.contextual.smartrules.util.Util;

/** Class for processing refresh request / response of Condition publishers
 *<code><pre>
 * CLASS:
 *     ConditionPublisherManager extends PublisherManger
 * Interface:
 *
 * RESPONSIBILITIES:
 * 		Process Condition Publisher added / deleted / replaced
 *      Process Refresh request and response of Condition publishers
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class ConditionPublisherManager extends PublisherManager {

    private static final String TAG = ConditionPublisherManager.class.getSimpleName();
    public ConditionPublisherManager(Context context) {
        super(context);
        type = CONDITION;
        populateValidPublishers();
    }

    @Override
    protected void processRefreshResponse(Intent refreshResponseIntent) {
        String config  = refreshResponseIntent.getStringExtra(EXTRA_CONFIG);
        String desc = refreshResponseIntent.getStringExtra(EXTRA_DESCRIPTION);
        String pubKey = refreshResponseIntent.getStringExtra(EXTRA_PUBLISHER_KEY);
        String status = refreshResponseIntent.getStringExtra(EXTRA_STATUS);
        String responseId = refreshResponseIntent.getStringExtra(EXTRA_RESPONSE_ID);
        String[] responseIdSplit = responseId.split(COLON);

        String oldConfig = Uri.decode(responseIdSplit[1]);
        String ruleKey = responseIdSplit[2];
        String ruleSrc = responseIdSplit[3];
        boolean silent = Boolean.valueOf(responseIdSplit[4]);
        mImportType = Integer.parseInt(responseIdSplit[5]);
        mPublisherUpdateReason = responseIdSplit[6];
        mReqId = responseIdSplit[7];

        if(oldConfig == null) return;

        if(LOG_DEBUG) Log.d(TAG,"processRefreshResponse responseIdSplit : " + Arrays.toString(responseIdSplit) +
                                "\n config : " + config + "desc : " + desc + "pubKey : " + pubKey +
                                "\n ruleKey " + ruleKey);
        long ruleId = RulePersistence.getRuleIdForRuleKey(mContext, ruleKey);
        String conditionWhereClause = ConditionTable.Columns.CONDITION_PUBLISHER_KEY + EQUALS + ANY
                                      + AND + ConditionTableColumns.CONDITION_CONFIG + EQUALS + ANY
                                      + AND + ConditionTableColumns.PARENT_FKEY + EQUALS + ANY;
        String[] conditionWhereArgs = {pubKey, oldConfig, Long.toString(ruleId)};
        ContentValues cv = new ContentValues();
        if(status.equals(SUCCESS)) {
            cv.put(ConditionTableColumns.CONDITION_CONFIG, config);
            if(desc != null) cv.put(ConditionTableColumns.CONDITION_DESCRIPTION, desc);
            cv.put(ConditionTableColumns.CONDITION_VALIDITY , ConditionTable.Validity.VALID);
            String state = refreshResponseIntent.getStringExtra(EXTRA_STATE);
            if(state != null)
                cv.put(ConditionTable.Columns.CONDITION_MET, ((state.equals(TRUE))?1:0));
        }
        else {
            cv.put(ConditionTableColumns.CONDITION_VALIDITY , ConditionTable.Validity.INVALID);
        }

        int count = mContext.getContentResolver().update(ConditionTable.CONTENT_URI, cv, conditionWhereClause, conditionWhereArgs);
        String flags = RulePersistence.getRuleFlagsForRuleKey(mContext, ruleKey);
        if(!oldConfig.equals(config) &&
                (Util.isNull(flags) || flags.equals(RuleTable.Flags.INVISIBLE))) {
            ArrayList<String> configPubKeyList = new ArrayList<String>();
            configPubKeyList.add(oldConfig + COMMA + pubKey);
            invokeModeAdToCancel(configPubKeyList, ruleKey);
        }
        if(count > 0)
            mRvInterface.pokeRulesValidatorForPublisher(ruleKey, ruleSrc, silent, mImportType, mReqId, mPublisherUpdateReason);
    }

    /*
     * Invokes modeAd to cancel the subscription from the given config publisher  key list
     */
    private void invokeModeAdToCancel(ArrayList<String> configPubKeyList,
                                      String ruleKey) {

        // Launch the Mode AD to cancel to Condition publishers.
        Intent intent = new Intent(ACTION_LAUNCH_MODE_ACT_DEACTIVATOR);
        intent.putExtra(EXTRA_RULE_KEY, ruleKey);
        intent.putExtra(EXTRA_LAUNCH_REASON, RULE_DELETED);
        intent.putExtra(EXTRA_PREV_CONFIG_PUBKEY_LIST, configPubKeyList);
        mContext.sendBroadcast(intent);
    }

    @Override
    protected void processRefreshTimeout(String ruleKey, String ruleSrc,
                                         String pubKey, String config, boolean silent) {
        if(LOG_DEBUG) Log.d(TAG,"processRefreshTimeout for pubKey : " + pubKey + " config : "
                                + config + " " + pubKey+config.hashCode());
        ContentResolver cr = mContext.getContentResolver();
        long ruleId =  RulePersistence.getRuleIdForRuleKey(mContext, ruleKey);
        String where = ConditionTableColumns.PARENT_FKEY + EQUALS + ANY + AND +
                       ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + ANY + AND +
                       ConditionTableColumns.CONDITION_CONFIG + EQUALS + ANY + AND +
                       ConditionTableColumns.CONDITION_VALIDITY + EQUALS + ANY;
        String[] whereArgs = {Long.toString(ruleId), pubKey, config, ConditionTable.Validity.INPROGRESS};
        ContentValues cv = new ContentValues();
        cv.put(ConditionTableColumns.CONDITION_VALIDITY, RuleTable.Validity.INVALID);
        int count = cr.update(ConditionTable.CONTENT_URI, cv, where, whereArgs);
        if(count > 0) {
            ArrayList<String> configPubkeyList = RulePersistence.getConfigPubKeyListForDeletion(mContext, ruleId);
            invokeModeAdToCancel(configPubkeyList, ruleKey);
            mRvInterface.pokeRulesValidatorForPublisher(ruleKey, ruleSrc, silent, 0, null, mPublisherUpdateReason);
        }
    }

    @Override
    protected void updateValidity(String pubKey, String config, String ruleKey,
                                  String validity) {
        ConditionPersistence.updateConditionValidity(mContext, ruleKey, pubKey, config, validity);
    }

    @Override
    protected void processPublisherModified(String pubKey, String description,
                                            String marketLink) {
        if(!mValidPublishers.contains(pubKey)) {
            processPublisherDeleted(pubKey);
        } else {
            ContentResolver cr = mContext.getContentResolver();
            ContentValues cv = new ContentValues();
            cv.put(ConditionTableColumns.CONDITION_MARKET_URL, marketLink);
            cv.put(ConditionTableColumns.SENSOR_NAME, description);
            String where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
            if(LOG_DEBUG) Log.d(TAG, " processPublisherModified : " + pubKey + ":" + mPublisherUpdateReason);
            if(mPublisherUpdateReason != null && mPublisherUpdateReason.equals(LOCALE_CHANGED)) {
                where += AND + ConditionTableColumns.PARENT_FKEY + IN + LP +
                         SELECT + RuleTableColumns._ID +
                         FROM + RuleTable.TABLE_NAME +
                         WHERE + RuleTableColumns.FLAGS + NOT_EQUAL +
                         Q +
                         RuleTable.Flags.SOURCE_LIST_VISIBLE +
                         Q +
                         RP;
            }

            int count = cr.update(ConditionTable.CONTENT_URI, cv, where, null);
            if(count > 0) {
                refreshPublisher(pubKey);
            }
        }
    }

    /*
     * Gets all the rules using the given publisher and calls handle request
     * to send the refresh command to publisher
     */
    private void refreshPublisher(String pubKey) {
        ContentResolver cr = mContext.getContentResolver();
        String where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleConditionView.CONTENT_URI,
                              new String[] {RuleTableColumns.KEY, RuleTableColumns.SOURCE, ConditionTableColumns.CONDITION_CONFIG},
                              where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    if(!ruleKey.equals(DEFAULT_RULE_KEY)){
	                    String ruleSrc = cursor.getString(cursor.getColumnIndex(RuleTableColumns.SOURCE));
	                    String config = cursor.getString(cursor.getColumnIndex(ConditionTableColumns.CONDITION_CONFIG));
	                    handleRefreshRequest(ruleKey, ruleSrc, pubKey, config, null, true);
                    }
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Query failed");
        } finally {
            if(cursor != null) cursor.close();
        }
    }

    @Override
    protected void processPublisherAdded(String pubKey, String description,
                                         String marketLink) {
        if(mValidPublishers.contains(pubKey)) {
            ContentResolver cr = mContext.getContentResolver();
            ContentValues cv = new ContentValues();
            cv.put(ConditionTableColumns.CONDITION_MARKET_URL, marketLink);
            cv.put(ConditionTableColumns.SENSOR_NAME, description);
            cv.put(ConditionTableColumns.CONDITION_VALIDITY, ConditionTable.Validity.INPROGRESS);
            String where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;

            int count = cr.update(ConditionTable.CONTENT_URI, cv, where, null);
            if(count > 0) {
                refreshPublisher(pubKey);
            }
        }
    }

    /*
     * Invoke Rulesvalidator to validate the rules using the given publisher key
     */
    private void pokeRv(String pubKey) {
        ContentResolver cr = mContext.getContentResolver();
        String where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q
                       + AND + RuleTableColumns.KEY + NOT_EQUAL + Q + DEFAULT_RULE_KEY + Q;;
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleConditionView.CONTENT_URI,
                              new String[] {RuleTableColumns.KEY, RuleTableColumns.SOURCE},
                              where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    String ruleSrc = cursor.getString(cursor.getColumnIndex(RuleTableColumns.SOURCE));
                    mRvInterface.pokeRulesValidatorForPublisher(ruleKey, ruleSrc, true, mImportType, null, mPublisherUpdateReason);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Query failed");
        } finally {
            if(cursor != null) cursor.close();
        }
    }


    @Override
    protected void processPublisherDeleted(String pubKey) {
        if(mBlacklist.contains(pubKey))
            updateValidity(pubKey, null, null, ConditionTable.Validity.BLACKLISTED);
        else
            updateValidity(pubKey, null, null, ConditionTable.Validity.UNAVAILABLE);
        pokeRv(pubKey);
    }

    @Override
    protected void processRefreshRequest(List<String> ruleList) {
        if(ruleList == null || ruleList.size() == 0) return;

        HashSet<RefreshParams> pubRefreshParamList = new HashSet<PublisherManager.RefreshParams>();

        StringBuilder ruleListString = new StringBuilder();
        int listSize = ruleList.size();
        ruleListString.append(LP);
        ruleListString.append(Q);
        ruleListString.append(ruleList.get(0));
        ruleListString.append(Q);

        for(int i = 1; i <  listSize ; i++) {
            ruleListString.append(COMMA);
            ruleListString.append(Q);
            ruleListString.append(ruleList.get(i));
            ruleListString.append(Q);
        }
        ruleListString.append(RP);
        ContentResolver cr = mContext.getContentResolver();
        String where = RuleTableColumns.KEY + IN + ruleListString.toString();
        String[] projection = { RuleTableColumns.KEY,
                                RuleTableColumns.SOURCE,
                                ConditionTableColumns.CONDITION_PUBLISHER_KEY,
                                ConditionTableColumns.CONDITION_CONFIG
                              };
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleConditionView.CONTENT_URI,
                              projection,
                              where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    String ruleSrc = cursor.getString(cursor.getColumnIndex(RuleTableColumns.SOURCE));
                    String pubKey = cursor.getString(cursor.getColumnIndex(ConditionTableColumns.CONDITION_PUBLISHER_KEY));
                    String conditionConfig = cursor.getString(cursor.getColumnIndex(ConditionTableColumns.CONDITION_CONFIG));
                    if(LOG_DEBUG) Log.d(TAG, "processRefreshRequest " + ruleKey + COLON + ruleSrc + COLON +
                                            pubKey + COLON + conditionConfig);
                    RefreshParams refreshParams = new RefreshParams(ruleKey, ruleSrc, pubKey, conditionConfig);
                    pubRefreshParamList.add(refreshParams);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Query failed");
        } finally {
            if(cursor != null) cursor.close();
        }

        if(pubRefreshParamList.size() > 0) {
            for(RefreshParams pubRefreshParams : pubRefreshParamList) {
                pubRefreshParams.sendRefreshRequest();
            }
            RefreshTimer timer = new RefreshTimer(mContext, (ArrayList<String>) ruleList, mReqId, type);
            timer.start(pubRefreshParamList.size() * PUBLISHER_COMMON_REFRESH_TIMEOUT_VALUE);
        }
    }
}
