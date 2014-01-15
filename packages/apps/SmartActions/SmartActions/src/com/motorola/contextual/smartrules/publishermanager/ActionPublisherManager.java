package com.motorola.contextual.smartrules.publishermanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.db.Schema.ActionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.ActionPersistence;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.view.RuleActionView;
import com.motorola.contextual.smartrules.db.table.view.RuleView;


/** Class for processing refresh request / response of action publishers
 *<code><pre>
 * CLASS:
 *     ActionPublisherManager extends PublisherManger
 * Interface:
 *
 * RESPONSIBILITIES:
 * 		Process Action Publisher added / deleted / replaced
 *      Process Refresh request and response of Action publishers
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class ActionPublisherManager extends PublisherManager {

    private static final String TAG = ActionPublisherManager.class.getSimpleName();

    private HashMap<String, String> mDefaultRuleMap = null;

    public ActionPublisherManager(Context context) {
        super(context);
        type = ACTION;
        populateValidPublishers();
        mDefaultRuleMap = getPublisherForDefaultRule();
    }

    @Override
    protected void processRefreshResponse(Intent refreshResponseIntent) {
        String config  = refreshResponseIntent.getStringExtra(EXTRA_CONFIG);
        String desc = refreshResponseIntent.getStringExtra(EXTRA_DESCRIPTION);
        String pubKey = refreshResponseIntent.getStringExtra(EXTRA_PUBLISHER_KEY);
        String status = refreshResponseIntent.getStringExtra(EXTRA_STATUS);
        String responseId = refreshResponseIntent.getStringExtra(EXTRA_RESPONSE_ID);
        boolean whenRuleEnds = refreshResponseIntent.getBooleanExtra(EXTRA_RULE_ENDS, false);
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
        String actionWhereClause = ActionTable.Columns.ACTION_PUBLISHER_KEY + EQUALS + ANY
                                   + AND + ActionTableColumns.CONFIG + EQUALS + ANY
                                   + AND + ActionTableColumns.PARENT_FKEY + EQUALS + ANY;
        String[] actionWhereArgs = {pubKey, oldConfig, Long.toString(ruleId)};
        ContentValues cv = new ContentValues();

        if(status.equals(SUCCESS)) {
            cv.put(ActionTableColumns.CONFIG, config);
            if(desc != null) cv.put(ActionTableColumns.ACTION_DESCRIPTION, desc);
            cv.put(ActionTableColumns.ACTION_VALIDITY , RuleTable.Validity.VALID);
        }
        else {
            cv.put(ActionTableColumns.ACTION_VALIDITY , RuleTable.Validity.INVALID);
        }
        cv.put(ActionTableColumns.ON_MODE_EXIT, whenRuleEnds?ActionTable.OnModeExit.ON_EXIT:ActionTable.OnModeExit.ON_ENTER);

        int count = mContext.getContentResolver().update(ActionTable.CONTENT_URI, cv, actionWhereClause, actionWhereArgs);
        if(count > 0) mRvInterface.pokeRulesValidatorForPublisher(ruleKey, ruleSrc, silent, mImportType, mReqId, mPublisherUpdateReason);
    }

    @Override
    protected void updateValidity(String pubKey, String config, String ruleKey,
                                  String validity) {
        ActionPersistence.updateActionValidity(mContext, pubKey, config, ruleKey, validity);
    }

    @Override
    protected void processRefreshTimeout(String ruleKey, String ruleSrc, String pubKey, String config, boolean silent) {
        if(LOG_DEBUG) Log.d(TAG,"processRefreshTimeout for pubKey : " + pubKey + " config : "
                                + config + " " + pubKey+config.hashCode());
        ContentResolver cr = mContext.getContentResolver();
        long ruleId =  RulePersistence.getRuleIdForRuleKey(mContext, ruleKey);
        String where = ActionTableColumns.PARENT_FKEY + EQUALS + ANY + AND +
                       ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + ANY + AND +
                       ActionTableColumns.CONFIG + EQUALS + ANY + AND +
                       ActionTableColumns.ACTION_VALIDITY + EQUALS + ANY;
        String[] whereArgs = {Long.toString(ruleId), pubKey, config, ActionTable.Validity.INPROGRESS};
        ContentValues cv = new ContentValues();
        cv.put(ActionTableColumns.ACTION_VALIDITY, RuleTable.Validity.INVALID);
        int count = cr.update(ActionTable.CONTENT_URI, cv, where, whereArgs);
        if(count > 0) {
            mRvInterface.pokeRulesValidatorForPublisher(ruleKey, ruleSrc, silent, 0, null, mPublisherUpdateReason);
        }
    }

    @Override
    protected void processPublisherModified(String pubKey, String description,
                                            String marketLink) {
        if(!mValidPublishers.contains(pubKey)) {
            processPublisherDeleted(pubKey);
        } else {
            ContentResolver cr = mContext.getContentResolver();
            ContentValues cv = new ContentValues();
            cv.put(ActionTableColumns.MARKET_URL, marketLink);
            cv.put(ActionTableColumns.STATE_MACHINE_NAME, description);
            String where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
            if(LOG_DEBUG) Log.d(TAG, " processPublisherModified : " + pubKey + ":" + mPublisherUpdateReason);
            if(mPublisherUpdateReason != null && mPublisherUpdateReason.equals(LOCALE_CHANGED)) {
                where += AND + ActionTableColumns.PARENT_FKEY + IN + LP +
                         SELECT + RuleTableColumns._ID +
                         FROM + RuleTable.TABLE_NAME +
                         WHERE + RuleTableColumns.FLAGS + NOT_EQUAL +
                         Q +
                         RuleTable.Flags.SOURCE_LIST_VISIBLE +
                         Q +
                         RP;
            }

            int count = cr.update(ActionTable.CONTENT_URI, cv, where, null);
            if(count > 0) {
                refreshPublisher(pubKey);
            }
        }
    }

    /*
     * Sends the refresh request to all the configs in the given publisher
     */
    private void refreshPublisher(String pubKey) {
        ContentResolver cr = mContext.getContentResolver();
        String where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleActionView.CONTENT_URI,
                              new String[] {RuleTableColumns.KEY, RuleTableColumns.SOURCE, ActionTableColumns.CONFIG},
                              where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    if(!ruleKey.equals(DEFAULT_RULE_KEY)){
	                    String ruleSrc = cursor.getString(cursor.getColumnIndex(RuleTableColumns.SOURCE));
	                    String actionConfig = cursor.getString(cursor.getColumnIndex(ActionTableColumns.CONFIG));
	                    String defaultAction = null;
	                    if(mDefaultRuleMap != null) defaultAction = mDefaultRuleMap.get(pubKey);
	                    handleRefreshRequest(ruleKey, ruleSrc, pubKey, actionConfig, defaultAction, true);
                    }
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Query failed");
        } finally {
            if(cursor != null) cursor.close();
        }
    }

    /*
     * Invokes RV to validate the Rules using the given publisher key
     */
    private void pokeRv(String pubKey) {
        ContentResolver cr = mContext.getContentResolver();
        String where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q
                       + AND + RuleTableColumns.KEY + NOT_EQUAL + Q + DEFAULT_RULE_KEY + Q;
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleActionView.CONTENT_URI,
                              new String[] {RuleTableColumns.KEY, RuleTableColumns.SOURCE},
                              where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    String ruleSrc = cursor.getString(cursor.getColumnIndex(RuleTableColumns.SOURCE));
                    // Validate only non default rules
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
    protected void processPublisherAdded(String pubKey, String description,
                                         String marketLink) {
        if(mValidPublishers.contains(pubKey)) {
            ContentResolver cr = mContext.getContentResolver();
            ContentValues cv = new ContentValues();
            cv.put(ActionTableColumns.MARKET_URL, marketLink);
            cv.put(ActionTableColumns.STATE_MACHINE_NAME, description);
            cv.put(ActionTableColumns.ACTION_VALIDITY, ActionTable.Validity.VALID);
            String where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;

            int count = cr.update(ActionTable.CONTENT_URI, cv, where, null);
            if(count > 0) {
                refreshPublisher(pubKey);
            }
        }
    }

    @Override
    protected void processPublisherDeleted(String pubKey) {
        if(mBlacklist.contains(pubKey))
            updateValidity(pubKey, null, null, ActionTable.Validity.BLACKLISTED);
        else
            updateValidity(pubKey, null, null, ActionTable.Validity.UNAVAILABLE);
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
                                ActionTableColumns.ACTION_PUBLISHER_KEY,
                                ActionTableColumns.CONFIG
                              };
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleActionView.CONTENT_URI,
                              projection,
                              where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    String ruleSrc = cursor.getString(cursor.getColumnIndex(RuleTableColumns.SOURCE));
                    String pubKey = cursor.getString(cursor.getColumnIndex(ActionTableColumns.ACTION_PUBLISHER_KEY));
                    String actionConfig = cursor.getString(cursor.getColumnIndex(ActionTableColumns.CONFIG));
                    if(LOG_DEBUG) Log.d(TAG, "processRefreshRequest " + ruleKey + COLON + ruleSrc + COLON +
                                            pubKey + COLON + actionConfig);
                    RefreshParams refreshParams = new RefreshParams(ruleKey, ruleSrc, pubKey, actionConfig);
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

    /*
     * Queries all the publishers used in the default rule
     */
    private HashMap<String, String> getPublisherForDefaultRule() {

        HashMap<String, String> defaultRuleMap = null;
        if(LOG_DEBUG) Log.d(TAG, "getPublisherForDefaultRule  called");
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            String[] projection = { RuleTableColumns.KEY,
                                    ActionTableColumns.ACTION_PUBLISHER_KEY,
                                    ActionTableColumns.CONFIG
                                  };
            String selection = RuleTableColumns.KEY + EQUALS + Q + DEFAULT_RULE_KEY + Q;
            cursor = cr.query(RuleView.CONTENT_URI, projection, selection, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                defaultRuleMap = new HashMap<String, String>();
                do {
                    String actionPub = cursor.getString(cursor.getColumnIndex(ActionTableColumns.ACTION_PUBLISHER_KEY));
                    String config = cursor.getString(cursor.getColumnIndex(ActionTableColumns.CONFIG));
                    defaultRuleMap.put(actionPub, config);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Query failed");
        } finally {
            if(cursor != null) cursor.close();
        }
        return defaultRuleMap;
    }
}
