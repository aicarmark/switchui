/*
 * @(#)SuggestionsNotificationService.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/04/27 NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.suggestions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app.AddRuleListActivity;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.SuggestionsPersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTable.RuleType;
import com.motorola.contextual.smartrules.db.table.RuleTable.SuggState;
import com.motorola.contextual.smartrules.util.Util;


/**
 * This service processes the broadcast received from Rules Importer
 *
 * <code><pre>
 *
 * CLASS:
 *  extends IntentService
 *
 *  implements
 *      Constants - Contain common constants to be used
 *      DbSyntax - DB related constants
 *
 * RESPONSIBILITIES:
 *      If correct broadcast received, it:
 *      - Shows a notification
 *      - Updated Rule Table
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 */
public class SuggestionsNotificationService extends IntentService implements Constants, DbSyntax {

    private static final String TAG = SuggestionsNotificationService.class.getSimpleName();
    private int mSugCount = 0;
    private Context mContext = null;

    /** default constructor
     *
     */
    public SuggestionsNotificationService() {
        super(TAG);
    }

    public SuggestionsNotificationService(String name) {
        super(name);
    }

    /** onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        mContext = this;
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if(intent == null)
            return;

        String action = intent.getStringExtra(INTENT_ACTION);
        if (action == null) return;

        if(action.equals(ACTION_SA_CORE_INIT_COMPLETE)) {
            if (LOG_INFO)
                Log.i(TAG, "Suggestion Notification: SA Core Init Recevied! ");

            if(Suggestions.isNotificationbeingShown(mContext)) {
                readDbAndSendNotification();
            }
        } else if(action.equals(INTENT_RULES_VALIDATED)) {
            if (LOG_INFO)
                Log.i(TAG, "Suggestion Notification: START ");

            Bundle bundle = intent.getExtras();
            if(bundle == null) return;

            // Check if there are any suggestion type rules in the intent
            countSuggestionsFromIntent(bundle);

            // send notification only if new suggestion is available
            if (mSugCount > 0) {
                readDbAndSendNotification();
            }
        }
    }

    /**
     * Counts the number of Suggestion type rules present in the incoming intent
     *
     * @param bundle - bundle carrying HashMap
     */
    private void countSuggestionsFromIntent(Bundle bundle) {

        Serializable ruleKeySourcedata = bundle.getSerializable(EXTRA_RULE_INFO);
        if (ruleKeySourcedata != null) {
            @SuppressWarnings("unchecked")
            HashMap<String, ArrayList<String>> ruleKeySourceMap = (HashMap<String, ArrayList<String>>)ruleKeySourcedata;

            Iterator<Entry<String, ArrayList<String>>> iter = ruleKeySourceMap.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, ArrayList<String>> entry = iter.next();

                if (entry.getKey().equals(KEY_RULE_SOURCE)) {
                    ArrayList<String> ruleSourceInfo = entry.getValue();

                    for (int indx = 0; indx < ruleSourceInfo.size(); indx++) {
                        int source = Integer.parseInt(ruleSourceInfo.get(indx));
                        if ((source == RuleTable.Source.SUGGESTED) ||
                                (source == RuleTable.Source.FACTORY)   ||
                                (source == RuleTable.Source.USER)) {

                            ArrayList<String> status = ruleKeySourceMap.get(KEY_RULE_STATUS);

                            if (status != null && status.size() > indx) {
                                @SuppressWarnings("unused")
								boolean state = Boolean.parseBoolean(status.get(indx));

//                                if (state) {
                                    ArrayList<String> key = ruleKeySourceMap.get(KEY_RULE_KEY);

                                    if (key != null && key.size() > 0) {
                                        if(markRuleAsUnreadInRuleTable(key.get(indx)))
                                            mSugCount++;
                                    }
//                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        if (LOG_INFO)
            Log.i(TAG, "SugCount = " + mSugCount);
    }

    /**
     * Marking the rule as UNREAD will ensure that this rule shows up in the suggestion inbox list,
     * ie, it will be treated as a new suggestion
     *
     * @param ruleKey - Key of the rule that needs to be updated
     * @return - True if the Rule was marked Unread
     */
    private boolean markRuleAsUnreadInRuleTable(String ruleKey) {

        boolean result = false;
        if(LOG_INFO) Log.i(TAG, "New Suggestion Received: Rule Key = " + ruleKey);

        // we need to first check if a notification should be shown or not
        if(!SuggestionsPersistence.isSilentSuggestion(mContext, ruleKey)) {

            // Mark the rule as UNREAD
            SuggestionsPersistence.setSuggestionState(mContext, ruleKey, SuggState.UNREAD);

            result = true;
        } else {

            if(LOG_INFO)
                Log.i(TAG, "Silent Rule, no notification will be shown");

        }

        return result;
    }

    /**
     * Send notification depending on new/unread suggestions available in MM db.
     *
     * @param context - onReceive context
     */
    private void readDbAndSendNotification() {

        // Quietly return if notification preference is NOT set
        if (!Util.getSharedPrefStateValue(mContext, NOTIFY_SUGGESTIONS_PREF, TAG))
            return;

        boolean launchInbox = false;

        // Read Rule Table Cursor
        Cursor rulesCursor = mContext.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI,
                             null, SuggestionsPersistence.WHERE_CLAUSE_UNREAD, null, null);

        if (rulesCursor == null) {
            launchInbox = true;
            Log.e(TAG, "NULL Cursor in sendNotification");
        } else {
            try {
                if (rulesCursor.moveToFirst()) {

                    // Get unread count from DB
                    mSugCount = rulesCursor.getCount();

                    if (mSugCount == 1) {
                        long id = rulesCursor.getLong(rulesCursor.getColumnIndex(RuleTable.Columns._ID));

                        // Launch suggestions detail activity
                        showSuggestionNotification(mContext, mSugCount, id);
                    } else {
                        launchInbox = true;
                    }
                } else {
                    launchInbox = true;
                    if(LOG_DEBUG) Log.w(TAG, "Empty cursor! Unread Rows: " + rulesCursor.getCount());
                }
            } catch (Exception e) {
                launchInbox = true;
                e.printStackTrace();
            } finally {
                rulesCursor.close();
            }
        }

        // if something went wrong or there were more than 1 unread suggestions,
        // let us send a notification to launch the suggestion inbox
        if (launchInbox)
            showSuggestionNotification(mContext, mSugCount, RuleType.DEFAULT);
    }

    /**
     * Send Notification with pending intent to launch either Suggestions inbox if Suggestions is ?
     * 1 or launch Suggestion detail activity if count == 1
     *
     * @param context - context
     * @param ruleId - rule if count == 1 or -1 if count > 1
     */
    public static void showSuggestionNotification(Context context, int sCount, long ruleId) {

        if (LOG_INFO)
            Log.i(TAG, "sendNotification with Count="+sCount);

        final int SUGGESTIONS_NOTIF_ID = 0x2112;// Random
        final int NOTIF_ICON_ID = SUGGESTIONS_NOTIF_ID + 1;

        // get the current time.
        final long when = System.currentTimeMillis();

        NotificationManager mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);       
        
        Notification.Builder builder = new Notification.Builder(context)
        										.setTicker(context.getString(R.string.sg_new))
        										.setSmallIcon(R.drawable.stat_notify_sr_suggestion)
        										.setWhen(when);
        
        Notification  notification =  builder.getNotification();
        									
        // remove notification
        mNotificationManager.cancel(NOTIF_ICON_ID);
        Log.i(TAG,"Suggestion Notification removed");

        if (sCount == 0) {
            // Update preference
            Suggestions.setNotificationShow(context, false);

            // Count is zero, just scoot!
            return;
        }

        // Cancel the intent once used
        notification.flags = Notification.FLAG_AUTO_CANCEL | Notification.FLAG_ONLY_ALERT_ONCE;

        Intent launchIntent = null;
        if (sCount == 1 && ruleId != RuleTable.RuleType.DEFAULT) {
            // Launch suggestions detail activity
        	if (LOG_DEBUG) Log.d(TAG,"Launching UI Controller");
        	launchIntent = new Intent(context, PublisherUiController.class);
            launchIntent.putExtra(PUZZLE_BUILDER_RULE_ID, ruleId);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            // Go to Suggestions Inbox
        	if (LOG_DEBUG) Log.d(TAG,"Launching Suggestion Inbox");
            launchIntent = new Intent(context, AddRuleListActivity.class);
            launchIntent.putExtra(Suggestions.DISPLAY_TITLE,
                                  context.getString(R.string.suggestions));

            launchIntent.putExtra(Suggestions.WHERE_CLAUSE, SuggestionsPersistence.WHERE_CLAUSE_ALL);
            launchIntent.putExtra(Suggestions.EXTERNAL_LAUNCH, true);
        }

        // create pending intent
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (sCount == 1) {
            notification.setLatestEventInfo(context, context.getString(R.string.app_name), sCount
                                            + " " + context.getString(R.string.sg_suggestion), contentIntent);
        } else if (sCount > 1) {
            notification.setLatestEventInfo(context, context.getString(R.string.app_name), sCount
                                            + " " + context.getString(R.string.sg_suggestions), contentIntent);
        }

        Log.i(TAG,"Showing Suggestion Notification, Count="+ sCount);
        mNotificationManager.notify(NOTIF_ICON_ID, notification);

        // Update preference
        Suggestions.setNotificationShow(context, true);
    }
}
