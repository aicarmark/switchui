package com.motorola.contextual.smartrules.publishermanager;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;

/** Class for processing refresh request of Rule publishers
 *<code><pre>
 * CLASS:
 *     RulePublisherManager
 * Interface:
 * 		PublisherManagerConstants,
 * 		DbSyntax
 *
 * RESPONSIBILITIES:
 * 		Process Rule Publisher added / deleted / replaced
 *      Process Refresh request Rule publishers
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class RulePublisherManager extends PublisherManager {

    private static final String TAG = RulePublisherManager.class.getSimpleName();

    public RulePublisherManager(Context context) {
        super(context);
        type = RULE;
        populateValidPublishers();
    }

    @Override
    protected void processRefreshResponse(Intent refreshResponseIntent) {
        if(LOG_DEBUG) Log.d(TAG,"processRefreshResponse : Refresh response is not sent by RP");
    }

    @Override
    protected void processRefreshTimeout(String ruleKey, String ruleSrc,
                                         String pubKey, String config, boolean silent) {
        if(LOG_DEBUG) Log.d(TAG,"processRefreshTimeout : Refresh Timer iks not used for RP");
    }

    @Override
    protected void updateValidity(String pubKey, String config, String ruleKey,
                                  String validity) {
        RulePersistence.updateRuleValidity(mContext, pubKey, ruleKey, validity);
    }

    /*
     * Deletes all the user not accepted suggestions and samples published by invalid RP
     */
    private void deleteRulesForInavildRP(String pubKey, String ruleKey) {
        if(LOG_DEBUG) Log.d(TAG,"deleteRulesForInavildRP pubKey : " + pubKey + " rulekey : " + ruleKey);
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = null;
        String[] projection = {RuleTableColumns.KEY};
        String where = RuleTableColumns.PUBLISHER_KEY + EQUALS + Q + pubKey + Q + AND +
                       RuleTableColumns.FLAGS + EQUALS + Q + RuleTable.Flags.SOURCE_LIST_VISIBLE + Q;
        try {
            cursor = cr.query(RuleTable.CONTENT_URI, projection, where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKeyVal = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    if(ruleKey != null) {
                        if(ruleKey.equals(ruleKeyVal)) deleteRule(ruleKey);
                    } else {
                        deleteRule(ruleKeyVal);
                    }
                } while (cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG,"Query failed with Exception " + e);
        } finally {
            if(cursor != null) cursor.close();
        }
    }

    /**
     * processes the refresh request of list of Rules published by Rule publisher
     * @param publisherKey Publisher Key of rule publisher
     * @param ruleKeyList List of rules
     */
    public void refreshRequest(String publisherKey, ArrayList<String> ruleKeyList) {
        sendRefreshCommand(publisherKey, ruleKeyList);
    }

    /*
     * Sends refresh command to rule publisher
     */
    private void sendRefreshCommand(String publisherKey, ArrayList<String> ruleKeyList) {
        if(LOG_DEBUG) Log.d(TAG, " sendRefreshCommand " +
                                publisherKey + ":" +
                                ((ruleKeyList == null)?"null":ruleKeyList.toString()));
        Intent intent = new Intent(publisherKey);
        intent.putExtra(EXTRA_COMMAND, COMMAND_REFRESH);
        intent.putExtra(EXTRA_VERSION, VERSION);
        intent.putExtra(EXTRA_RULE_LIST, ruleKeyList);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        mContext.sendBroadcast(intent, PERM_RULE_PUBLISHER);
    }

    /*
     * Queries all the rules in SOURCE_LIST_VISIBLE published by given publisher and
     * sends refresh command to RP
     */
    private void refreshPublisher(String pubKey) {
        ArrayList<String> ruleKeyList = new ArrayList<String>();
        ContentResolver cr = mContext.getContentResolver();
        String where = RuleTableColumns.PUBLISHER_KEY + EQUALS + Q + pubKey + Q + AND +
                       RuleTableColumns.FLAGS + EQUALS + Q + RuleTable.Flags.SOURCE_LIST_VISIBLE + Q;
        Cursor cursor = null;
        try {
            cursor = cr.query(RuleTable.CONTENT_URI,
                              new String[] {RuleTableColumns.KEY, RuleTableColumns.SOURCE},
                              where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    ruleKeyList.add(ruleKey);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Query failed");
        } finally {
            if(cursor != null) cursor.close();
        }
        if(ruleKeyList.size() > 0) {
            sendRefreshCommand(pubKey, ruleKeyList);
        }
    }


    @Override
    protected void processPublisherModified(String pubKey, String description,
                                            String marketLink) {
        if(!mValidPublishers.contains(pubKey)) {
            deleteRulesForInavildRP(pubKey, null);
        } else {
            refreshPublisher(pubKey);
        }
    }

    @Override
    protected void processPublisherAdded(String pubKey, String description,
                                         String marketLink) {
        if(LOG_DEBUG) Log.d(TAG,"Not supported for RP");
    }

    @Override
    protected void processPublisherDeleted(String pubKey) {
        deleteRulesForInavildRP(pubKey, null);
    }

    @Override
    protected void processRefreshRequest(List<String> ruleList) {
        if(LOG_DEBUG) Log.d(TAG,"Not supported for RP");
    }
}
