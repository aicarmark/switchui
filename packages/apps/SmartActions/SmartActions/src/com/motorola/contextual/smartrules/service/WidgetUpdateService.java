/*
 * @(#)WidgetUpdateService.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2012/03/12 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.service;

import java.util.ArrayList;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.util.RuleStateValues;

/** This is a Intent Service class that responds to the widget update request.
 * 	- Fetches the cursor for the rule keys passed in from the widget
 *  - Constructs the RuleStateValues list and broadcasts a resonse for the widget.
 *
 *<code><pre>
 * CLASS:
 * 	 extends IntentService
 *
 *  implements
 *   Constants - for the constants used
 *   DbSyntax - for the DB related constants
 *
 * RESPONSIBILITIES:
 * 	 Processes the widget update request and send a response.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class WidgetUpdateService extends IntentService implements Constants, DbSyntax {

    private static final String TAG = WidgetUpdateService.class.getSimpleName();

    public static final String RULE_KEYS_EXTRA = PACKAGE + ".RULE_KEYS_EXTRA";

    private Context mContext = null;

    /** Constructor
     */
    public WidgetUpdateService() {
        super(TAG);
    }

    /** Constructor
     *
     * @param name - name to be used for worker thread
     */
    public WidgetUpdateService(String name) {
        super(name);
    }

    /** onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /** onHandleIntent()
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if(LOG_INFO) Log.i(TAG, "In onHandleIntent to handle "+intent.toUri(0));
        Bundle bundle = intent.getExtras();
        if(bundle == null) {
            Log.e(TAG, "intent extras is null - do nothing");
        } else {
            String[] ruleKeys = bundle.getStringArray(RULE_KEYS_EXTRA);
            if(ruleKeys != null && ruleKeys.length > 0) {
                Log.d(TAG, "Service launched to fetch for the ruleKeys "+ruleKeys.toString());
                ArrayList<RuleStateValues> widgetUpdateList = fetchRuleStateValues(mContext, getWhereClause(ruleKeys));
                if(widgetUpdateList.size() > 0) {
                	for(int i = 0; i < widgetUpdateList.size(); i++) {
                		RuleStateValues element = widgetUpdateList.get(i);
	                    if(LOG_DEBUG) Log.d(TAG, "Sending response to the widget with values "+element.toString());
	                    Intent responseIntent = new Intent(WIDGET_UPDATE_RESPONSE);
	                    responseIntent.putExtra(RuleTable.Columns.KEY, element.getRuleKey());
	                    responseIntent.putExtra(RuleTable.Columns._ID, element.getRuleId());
	                    responseIntent.putExtra(RuleTable.Columns.ACTIVE, element.getActive());
	                    responseIntent.putExtra(RuleTable.Columns.ENABLED, element.getEnabled());
	                    responseIntent.putExtra(RuleTable.Columns.RULE_TYPE, element.getRuleType());
	                    responseIntent.putExtra(RuleTable.Columns.ICON, element.getRuleIcon());
	                    responseIntent.putExtra(RuleTable.Columns.NAME, element.getRuleName());
	                    if(LOG_DEBUG) Log.d(TAG, "responseIntent is "+responseIntent.toUri(0));
	                    mContext.sendBroadcast(responseIntent);
                	}
                }
            }
        }
    }

    /** getter for the whereClause for the string array of rule keys passed in.
     *
     * @param ruleKeys - string array of rule keys
     * @return - whereClause for the rule keys fetch
     */
    private static String getWhereClause(String[] ruleKeys) {
        String whereClause = "";
        for(int i = 0; i < ruleKeys.length; i++) {
            whereClause = whereClause + RuleTable.Columns.KEY + EQUALS + Q + ruleKeys[i] + Q;
            if(i != ruleKeys.length - 1)
                whereClause = whereClause + OR;
        }
        return whereClause;
    }

    /** fetched the rule cursor of the rule keys for which the update is needed and returns an
     * array list of elements.
     *
     * @param context - context
     * @param whereClause - whereClause to fetch the cursor for
     * @return - array list of rule state elements
     */
    private static ArrayList<RuleStateValues> fetchRuleStateValues(Context context, String whereClause) {
        ArrayList<RuleStateValues> widgetUpdateList = new ArrayList<RuleStateValues>();
        Cursor cursor = context.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, whereClause, null, null);

        if(cursor == null) {
            Log.e(TAG, "null cursor returned for "+whereClause);
        } else {
            try {
                if(cursor.moveToFirst()) {
                    DatabaseUtils.dumpCursor(cursor);
                    if(LOG_DEBUG) Log.d(TAG, "count of the elements in the cursor is "+cursor.getCount());
                    for(int j = 0; j < cursor.getCount(); j++) {
                        RuleStateValues element = new RuleStateValues();
                        element.ruleKey = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.KEY));
                        element.ruleId = cursor.getLong(cursor.getColumnIndex(RuleTable.Columns._ID));
                        element.active = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.ACTIVE));
                        element.enabled = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.ENABLED));
                        element.ruleType = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.RULE_TYPE));
                        element.iconRes = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.ICON));
                        element.ruleName = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.NAME));
                        widgetUpdateList.add(element);
                        cursor.moveToNext();
                    }
                } else {
                    Log.e(TAG, "cursor.moveToFirst failed for "+whereClause);
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception fetching rules for "+whereClause);
                e.printStackTrace();
            } finally {
                if (!cursor.isClosed())
                    cursor.close();
            }
        }
        return widgetUpdateList;
    }
}