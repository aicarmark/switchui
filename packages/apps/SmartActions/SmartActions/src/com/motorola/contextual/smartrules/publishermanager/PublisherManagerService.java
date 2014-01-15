/*
 * @(#)PublisherManagerService.java
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
import java.util.Map;
import java.util.Map.Entry;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.PublisherProviderInterface;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.ActionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.ConditionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants.ImportType;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.util.Util;

/** IntentService to handle all the intents to Publisher Manager
 *<code><pre>
 * CLASS:
 *     PublisherManagerService Extends IntentService
 * Interface:
 * 		PublisherManagerConstants,
 * 		DbSyntax
 *
 * RESPONSIBILITIES:
 * 		handle all the intents sent to Publisher Manager
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PublisherManagerService extends IntentService implements PublisherManagerConstants, DbSyntax {

    static private final String TAG = PublisherManagerService.class.getSimpleName();

    // This variable is used to tell whether RulesImpoter has invoked PublisherManager
    private boolean mRulesImport = false;

    private String publisherUpdateReason;

    /**
     * Constructor
     */
    public PublisherManagerService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(LOG_INFO) Log.i(TAG, "onHandleIntent " + intent.toUri(0));
        String action = intent.getAction();
        if(action == null) {
            if(LOG_DEBUG) Log.d(TAG,"onHandleIntent Action is null");
        } else if(action.equals(ACTION_PUBLISHER_UPDATER)) {
            @SuppressWarnings("unchecked")
            Map<String, ArrayList<String>> ruleKeyMap = (Map<String, ArrayList<String>>) intent.getSerializableExtra(EXTRA_RULE_INFO);
            int importType = intent.getIntExtra(EXTRA_IMPORT_TYPE, 0);
            String responseId = intent.getStringExtra(EXTRA_RESPONSE_ID);
            if (responseId != null && responseId.contains(XML_UPGRADE)) {
                if (LOG_INFO) Log.i(TAG, "Samples updated; Handle Upgrade usecase");
                handleUpgrade();
            }
            if(ruleKeyMap != null) {
                if (LOG_INFO) Log.i(TAG, "Rules Imported; Handle Import usecase");
                handleRulesImport(ruleKeyMap, importType);
            }
        } else if(action.equals(ACTION_PUBLISHER_EVENT) ||
                  action.equals(SA_CORE_KEY) ||
                  action.equals(RULE_PUBLISHER_EVENT)) {
            String eventType = intent.getStringExtra(EXTRA_EVENT_TYPE);
            if(!eventType.equals(INITIATE_REFRESH_REQUEST)) {
                handleRefreshResponse(intent);
            } else {
                handleConditionPublisherRefreshRequest(intent);
            }
        } else if(action.equals(ACTION_PUBLISHER_UPDATED)
                  || action.equals(CONDITION_PUBLISHER_UPDATED)
                  || action.equals(RULE_PUBLISHER_UPDATED)) {
            publisherUpdateReason = intent.getStringExtra(EXTRA_PUBLISHER_UPDATED_REASON);
            handlePafPublisherUpdater(intent);
        } else {
            String command = intent.getStringExtra(EXTRA_COMMAND);
            if(command != null && command.equals(COMMAND_REFRESH)) {
                handleRefreshTimeout(intent);
            }
        }

    }

    /*
     * Handles the Refresh Request event from Condition publishers
     */
    private void handleConditionPublisherRefreshRequest(Intent intent) {
        ArrayList<String> publisherList = new ArrayList<String>();
        String pubKey = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        publisherList.add(pubKey);
        PublisherManager publisherMgr = PublisherManager.getPublisherManager(this, CONDITION);
        getPPDataUpdatePublisher(publisherMgr, publisherList, true);
    }

    /*
     * Handles the Response for the earlier Refresh request
     */
    private void handleRefreshResponse(Intent intent) {
        String responseId = intent.getStringExtra(EXTRA_RESPONSE_ID);
        String[] responseIdSplit = responseId.split(COLON);
        String type = responseIdSplit[0];
        PublisherManager pubMgr = PublisherManager.getPublisherManager(this, type);
        pubMgr.handleRefreshResponse(intent);
    }

    /*
     * Handles the timeout for the earlier Refresh request
     */
    private void handleRefreshTimeout(Intent intent) {
	if(LOG_INFO) Log.i(TAG, " handleRefreshTimeout " + intent.toUri(0));
        String responseId = intent.getStringExtra(EXTRA_REQUEST_ID);
        String[] responseIdSplit = responseId.split(COLON);
        String type = responseIdSplit[0];
        PublisherManager pubMgr = PublisherManager.getPublisherManager(this, type);
        if(pubMgr != null)
            pubMgr.handleRefreshTimeout(intent);
    }

    /*
     * Queries the updated data from PublisherProvider and updates publishers in MMDB
     */
    private void getPPDataUpdatePublisher(PublisherManager publisherMgr,
                                          ArrayList<String> publisherList, boolean isModified) {
        for(String pubKey : publisherList) {
            Uri contentUri = PublisherProviderInterface.CONTENT_URI;
            String[] projection = {PublisherProviderInterface.Columns.PUBLISHER_KEY,
                                   PublisherProviderInterface.Columns.DESCRIPTION,
                                   PublisherProviderInterface.Columns.MARKET_LINK,
                                  };
            String where = PublisherProviderInterface.Columns.PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
            ContentResolver cr = getContentResolver();
            Cursor cursor = null;
            String description = null;
            String marketLink = null;
            try {
                cursor = cr.query(contentUri, projection, where, null, null);
                if(cursor != null && cursor.moveToFirst()) {
                    do {
                        description = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.DESCRIPTION));
                        marketLink = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.MARKET_LINK));
                    } while (cursor.moveToNext());
                }
            } catch(Exception e) {
                Log.e(TAG, "Query failed with exception : " + e, e);
            } finally {
                if(cursor != null) cursor.close();
            }
            if(!isModified) {
                publisherMgr.handlePublisherAdded(pubKey, description, marketLink);
            } else {
                publisherMgr.handlePublisherModified(pubKey, description, marketLink);
            }
        }
    }

    /*
     * Handles ACTION_PUBLISHER_UPDATED, CONDITION_PUBLISHER_UPDATED and RULE_PUBLISHER_UPDATED intent
     */
    private void handlePafPublisherUpdater(Intent intent) {
	if(LOG_INFO) Log.i(TAG, " handlePafPublisherUpdater " + intent.toUri(0));
        ArrayList<String> publisherModList = intent.getStringArrayListExtra(EXTRA_PUBLISHER_MODIFIED_LIST);
        ArrayList<String> publisherAddList = intent.getStringArrayListExtra(EXTRA_PUBLISHER_ADDED_LIST);
        ArrayList<String> publisherRemList = intent.getStringArrayListExtra(EXTRA_PUBLISHER_REMOVED_LIST);
        String intentAction = intent.getAction();
        if(intentAction != null) {
            String type = (intentAction.equals(ACTION_PUBLISHER_UPDATED) ? ACTION :
                           (intentAction.equals(CONDITION_PUBLISHER_UPDATED) ? CONDITION : RULE));
            PublisherManager publisherMgr = null;
            publisherMgr = PublisherManager.getPublisherManager(this, type);
            publisherMgr.setmPublisherUpdateReason(publisherUpdateReason);
            if(publisherRemList != null && publisherRemList.size() > 0) {
                for(String pubKey : publisherRemList) {
                    publisherMgr.handlePublisherDeleted(pubKey);
                }
            }

            if(publisherAddList != null && publisherAddList.size() > 0)
                getPPDataUpdatePublisher(publisherMgr, publisherAddList, false);
            if(publisherModList != null && publisherModList.size() > 0)
                getPPDataUpdatePublisher(publisherMgr, publisherModList, true);
        }
    }

    /*
     * Send refresh request for all the publishers used in the given rule key
     */
    private void refreshRequest(HashMap<String, String> defaultRuleMap,
                                String ruleKeyVal, boolean silent, int importType) {
        PublisherManager rulePubMgr = PublisherManager.getPublisherManager(this, RULE);
        rulePubMgr.setmImportType(importType);
        ContentResolver cr = getContentResolver();
        Cursor cursor = null;
        Uri uri = RuleTable.CONTENT_URI;
        HashMap<String, ArrayList<String>> rpMap = new HashMap<String, ArrayList<String>>();

        String[] projection = { RuleTableColumns._ID,
                                RuleTableColumns.KEY,
                                RuleTableColumns.SOURCE,
                                RuleTableColumns.PUBLISHER_KEY,
                                RuleTableColumns.FLAGS
                              };
        String where = null;

        if(ruleKeyVal != null) {
            where = RuleTableColumns.KEY + EQUALS + Q + ruleKeyVal + Q;
        }

        try {
            cursor = cr.query(uri, projection, where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    long id = cursor.getLong(cursor.getColumnIndex(RuleTableColumns._ID));
                    String ruleKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.KEY));
                    String ruleSrc = cursor.getString(cursor.getColumnIndex(RuleTableColumns.SOURCE));
                    String rulePubKey = cursor.getString(cursor.getColumnIndex(RuleTableColumns.PUBLISHER_KEY));
                    String flags = cursor.getString(cursor.getColumnIndex(RuleTableColumns.FLAGS));
                    if(LOG_DEBUG) Log.d(TAG,"refreshRequest : ruleKey : " + ruleKey + " rulePubKey : " + rulePubKey);
                    if(!ruleKey.equals(DEFAULT_RULE_KEY)) {
                        if(!mRulesImport && rulePubKey != null && flags.equals(RuleTable.Flags.SOURCE_LIST_VISIBLE)) {
                            if(LOG_DEBUG) Log.d(TAG,"refreshRequest : ruleKey : mRulesImport && rulePubKey != null");
                            ArrayList<String> ruleKeyList = null;
                            if(!rpMap.containsKey(rulePubKey)) {
                                ruleKeyList = new ArrayList<String>();
                                rpMap.put(rulePubKey, ruleKeyList);
                            }
                            ruleKeyList = rpMap.get(rulePubKey);
                            ruleKeyList.add(ruleKey);
                        }
                    }
                    if(!flags.equals(RuleTable.Flags.SOURCE_LIST_VISIBLE) || mRulesImport) {
                        sendRefreshToActionPublishers(id, ruleKey, ruleSrc, silent, importType, defaultRuleMap);
                        sendRefreshToConditionPublishers(id, ruleKey, ruleSrc, silent, importType);
                    }
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG,"Exception in query : "+e, e);
        } finally {
            if(cursor != null) cursor.close();
        }
        if(!rpMap.isEmpty()) {
            for(Entry<String, ArrayList<String>> rpEntry : rpMap.entrySet()) {
                ((RulePublisherManager)rulePubMgr).refreshRequest(rpEntry.getKey(), rpEntry.getValue());
            }
        }
    }

    /*
     * This method queries the actions for a given rulekey and then invokes corresponding handleRefreshRequest method
     * on each of the action publishers.
     */
    private void sendRefreshToActionPublishers(long ruleId, String ruleKey, String ruleSrc,
            boolean silent, int importType, HashMap<String, String> defaultRuleMap) {
        PublisherManager actionPubMgr = PublisherManager.getPublisherManager(this, ACTION);
        actionPubMgr.setmImportType(importType);
        String actionTableSelection = ActionTable.Columns.PARENT_FKEY +  EQUALS + Q + ruleId + Q;
        Cursor cursor = getContentResolver().query(Schema.ACTION_TABLE_CONTENT_URI, null, actionTableSelection, null,  null);
        String actionPubKey, actionConfig;
        if(cursor != null ) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        actionPubKey = cursor.getString(cursor.getColumnIndex(ActionTableColumns.ACTION_PUBLISHER_KEY));
                        actionConfig = cursor.getString(cursor.getColumnIndex(ActionTableColumns.CONFIG));
                        if(actionPubKey != null) {
                            if(LOG_DEBUG) Log.d(TAG,"refreshRequest : ruleKey : actionPubKey != null");
                            updatePublisherStateMachineName(actionPubKey, ACTION);
                            String defaultAction = null;
                            if(defaultRuleMap != null) defaultAction = defaultRuleMap.get(actionPubKey);
                            actionPubMgr.handleRefreshRequest(ruleKey, ruleSrc, actionPubKey, actionConfig,
                                                              defaultAction, silent);
                        }
                    } while(cursor.moveToNext());
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                if(!cursor.isClosed())
                    cursor.close();
            }
        }
    }

    /**
     * Query the description for Action/Condition publisher from Publisher provider and update them.
     * @param pubKey
     */
    private void updatePublisherStateMachineName(String pubKey, String type) {
        Uri contentUri = PublisherProviderInterface.CONTENT_URI;
        String[] projection = {PublisherProviderInterface.Columns.PUBLISHER_KEY,
                               PublisherProviderInterface.Columns.DESCRIPTION,
                               PublisherProviderInterface.Columns.ACTIVITY_INTENT,
                               PublisherProviderInterface.Columns.PACKAGE,
                               PublisherProviderInterface.Columns.MARKET_LINK
                              };
        String where = PublisherProviderInterface.Columns.PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
        ContentResolver cr = getContentResolver();
        Cursor cursor = null;
        String description = null;
        String marketLink = null;
        String componentName = null;
        String pubPkg = null;
        Intent actIntent = null;
        try {
            cursor = cr.query(contentUri, projection, where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                description = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.DESCRIPTION));
                marketLink = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.MARKET_LINK));
                componentName = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.ACTIVITY_INTENT));
                pubPkg = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.PACKAGE));
                if(componentName != null) actIntent = Publisher.getActivityIntentForPublisher(pubPkg, componentName);

                ContentValues cv = new ContentValues();
                if(type.equals(ACTION)) {
                    if(!Util.isNull(description))  cv.put(ActionTableColumns.STATE_MACHINE_NAME, description);
                    if(actIntent != null) cv.put(ActionTableColumns.ACTIVITY_INTENT, actIntent.toUri(0));
                    cv.put(ActionTableColumns.MARKET_URL, marketLink);
                    where = ActionTableColumns.ACTION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
                    cr.update(ActionTable.CONTENT_URI, cv, where, null);
                } else if(type.equals(CONDITION)) {
                    if(!Util.isNull(description)) cv.put(ConditionTableColumns.SENSOR_NAME, description);
                    if(actIntent != null) cv.put(ConditionTableColumns.ACTIVITY_INTENT, actIntent.toUri(0));
                    cv.put(ConditionTableColumns.CONDITION_MARKET_URL, marketLink);
                    where = ConditionTableColumns.CONDITION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;
                    cr.update(ConditionTable.CONTENT_URI, cv, where, null);
                }
            }
        } catch(Exception e) {
            Log.e(TAG, "Query failed with exception : " + e, e);
        } finally {
            if(cursor != null) cursor.close();
        }
    }


    /*
     * This method queries the conditions for a given rulekey and then invokes corresponding handleRefreshRequest method
     * on each of the condition publishers.
     */
    private void sendRefreshToConditionPublishers(long ruleId, String ruleKey, String ruleSrc, boolean silent, int importType) {
        PublisherManager conditionPubMgr = PublisherManager.getPublisherManager(this, CONDITION);
        conditionPubMgr.setmImportType(importType);
        String selection = ConditionTable.Columns.PARENT_FKEY +  EQUALS + Q + ruleId + Q;
        Cursor cursor = getContentResolver().query(Schema.CONDITION_TABLE_CONTENT_URI, null, selection, null,  null);
        String conditionPubKey, conditionConfig;
        if(cursor != null ) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        conditionPubKey = cursor.getString(cursor.getColumnIndex(ConditionTableColumns.CONDITION_PUBLISHER_KEY));
                        conditionConfig = cursor.getString(cursor.getColumnIndex(ConditionTableColumns.CONDITION_CONFIG));
                        if(conditionPubKey != null) {
                            if(LOG_DEBUG) Log.d(TAG,"refreshRequest : ruleKey :" + ruleKey + ":" +conditionPubKey );
                            updatePublisherStateMachineName(conditionPubKey, CONDITION);
                            conditionPubMgr.handleRefreshRequest(ruleKey, ruleSrc, conditionPubKey, conditionConfig, null, silent);
                        }
                    } while(cursor.moveToNext());
                }
            } catch(Exception e) {
                e.printStackTrace();
            } finally {
                if(!cursor.isClosed())
                    cursor.close();
            }
        }
    }

    /*
     * Handles send refresh to all the publishers for all rules in ruleKeyMap
     */
    private void handleRulesImport(Map<String, ArrayList<String>> ruleKeyMap, int importType) {
        mRulesImport = true;
        if(LOG_INFO) Log.i(TAG, "handleRulesImport " + ruleKeyMap.toString() + "\n " + importType);
        ArrayList<String> silentList = ruleKeyMap.get(KEY_RULE_SILENT);
        ArrayList<String> ruleKeyList =  ruleKeyMap.get(KEY_RULE_KEY);
        for(String ruleKey : ruleKeyList) {
            boolean isSilent = Boolean.parseBoolean(silentList.get(ruleKeyList.indexOf(ruleKey)));
            refreshRequest(null, ruleKey, isSilent, importType);
        }
    }
    
    private void handleUpgrade() {
        mRulesImport = false;
        ArrayList<String> ruleKeyList = RulePersistence.getEnabledRuleKeyList(getApplicationContext());
        if(LOG_INFO) Log.i(TAG, "handleUpgrade ruleKeyList is " + ruleKeyList.toString());
        for(String ruleKey : ruleKeyList) {
            refreshRequest(null, ruleKey, true, ImportType.SILENT_UPDATE);
        }
    }
}
