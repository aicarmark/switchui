package com.motorola.contextual.smartrules.publishermanager;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema.ActionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.ConditionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.view.RuleView;

public class RulesValidator implements PublisherManagerConstants, DbSyntax {

    private static final String TAG = RulesValidator.class.getSimpleName();

    private Context mContext;

    public RulesValidator(Context context) {
        mContext = context;
    }

    /*
     * Queries the validation state of all the publisher in the given rule and
     * returns VALID if all the  publishers are VALID else returns INVALID
     */
    public String validateRule(String ruleKey) {
        if(LOG_DEBUG) Log.d(TAG, "validateRule : " + ruleKey);
        boolean hasValidActionPublisher = false;
        String pubValidity = RuleTable.Validity.INVALID;
        String tmpPubValidity = RuleTable.Validity.INVALID;
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = null;
        String[] projection = { RuleTableColumns._ID, RuleTableColumns.KEY, RuleTableColumns.VALIDITY,
                                ActionTableColumns.ACTION_PUBLISHER_KEY,
                                ActionTableColumns.ACTION_VALIDITY,
                                ActionTableColumns.ENABLED,
                                ActionTableColumns.MARKET_URL,
                                ConditionTableColumns.CONDITION_PUBLISHER_KEY,
                                ConditionTableColumns.ENABLED,
                                ConditionTableColumns.CONDITION_VALIDITY,
                                ConditionTableColumns.CONDITION_MARKET_URL,
                              };
        String where = RuleTableColumns.KEY + EQUALS + ANY;
        String[] whereArgs = {ruleKey};

        try {
            cursor = cr.query(RuleView.CONTENT_URI, projection, where, whereArgs, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String actionPubKey = cursor.getString(cursor.getColumnIndex(ActionTableColumns.ACTION_PUBLISHER_KEY));
                    String conditionPubKey = cursor.getString(cursor.getColumnIndex(ConditionTableColumns.CONDITION_PUBLISHER_KEY));
                    int actionPubEnabled = cursor.getInt(cursor.getColumnIndex(ActionTableColumns.ENABLED));
                    int conditionPubEnabled = cursor.getInt(cursor.getColumnIndex(ConditionTableColumns.ENABLED));
                    String actionMarketUrl = cursor.getString(cursor.getColumnIndex(ActionTableColumns.MARKET_URL));
                    String conditionMarketUrl = cursor.getString(cursor.getColumnIndex(ConditionTableColumns.CONDITION_MARKET_URL));
                    pubValidity = RuleTable.Validity.VALID;
                    if(actionPubKey != null) {
                        tmpPubValidity = pubValidity = cursor.getString(cursor.getColumnIndex(ActionTableColumns.ACTION_VALIDITY));
                        if(pubValidity.equals(ActionTable.Validity.VALID)) hasValidActionPublisher = true;
                        if(((pubValidity.equals(ActionTable.Validity.UNAVAILABLE) && actionMarketUrl == null) ||
                                pubValidity.equals(ActionTable.Validity.BLACKLISTED)) ||
                                actionPubEnabled != ActionTable.Enabled.ENABLED)
                            tmpPubValidity = ActionTable.Validity.VALID;
                        if(LOG_DEBUG) Log.d(TAG, "validateRule : " + actionPubKey + ":" + ":" + pubValidity + ":" + tmpPubValidity);
                    }
                    if(pubValidity.equals(RuleTable.Validity.VALID) && conditionPubKey != null) {
                        tmpPubValidity = pubValidity = cursor.getString(cursor.getColumnIndex(ConditionTableColumns.CONDITION_VALIDITY));
                        if(((pubValidity.equals(ConditionTable.Validity.UNAVAILABLE) && conditionMarketUrl == null) ||
                                pubValidity.equals(ConditionTable.Validity.BLACKLISTED)) ||
                                conditionPubEnabled != ConditionTable.Enabled.ENABLED)
                            tmpPubValidity = ConditionTable.Validity.VALID;
                        if(LOG_DEBUG) Log.d(TAG, "validateRule : " + conditionPubKey + ":" + ":" + pubValidity + ":" + tmpPubValidity);
                    }
                    if(!tmpPubValidity.equals(RuleTable.Validity.VALID)) {
                        break;
                    }
                } while(cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Query failed " + e);
        } finally {
            if(cursor != null) cursor.close();
        }
        if(LOG_DEBUG) Log.d(TAG, "validateRule : " + ruleKey + " Validity:" + pubValidity + ":" + tmpPubValidity);
        if(!pubValidity.equals(RuleTable.Validity.INPROGRESS) && !hasValidActionPublisher) {
            pubValidity = RuleTable.Validity.INVALID;
        } else {
            pubValidity = tmpPubValidity;
        }
        return pubValidity;
    }
}
