package com.motorola.contextual.smartrules.publishermanager;

import java.util.ArrayList;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.PublisherProviderInterface;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema.ActionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.ConditionTableColumns;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.view.RuleActionView;
import com.motorola.contextual.smartrules.db.table.view.RuleConditionView;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants.ImportType;
import com.motorola.contextual.smartrules.util.Util;

/** Abstract class for processing refresh request / response of publishers
 *<code><pre>
 * CLASS:
 *     PublisherManager
 * Interface:
 * 		PublisherManagerConstants,
 * 		DbSyntax
 *
 * RESPONSIBILITIES:
 * 		Process Publisher added / deleted / replaced
 *      Process Refresh request and response of publishers
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public abstract class PublisherManager implements PublisherManagerConstants, DbSyntax {

    private static final String TAG = PublisherManager.class.getSimpleName();
    private static final int REFRESH_TIMEOUT = 30000;
    protected Context mContext;
    protected List<String> mValidPublishers = new ArrayList<String>();
    protected List<String> mBlacklist = new ArrayList<String>();
    protected RulesValidatorInterface mRvInterface;
    protected String type = null;
    protected int mImportType = ImportType.IGNORE;
    protected String mPublisherUpdateReason = null;
    protected String mReqId = null;

    /*
     * Process the refresh response, if status is failure mark the publisher configuration as invalid else
     * mark it as Valid. It invokes RulesValidator to validate the rule
     */
    protected abstract void processRefreshResponse(Intent refreshResponseIntent) ;
    /*
     * Process refresh timeout for the publisher configuration. If the publisher configuration is in IN_PROGRESS
     * state, marks it as Invalid. It invokes RulesValidator to validate the rule
     */
    protected abstract void processRefreshTimeout(String ruleKey, String ruleSrc, String pubKey, String config, boolean silent);
    /*
     * Updates the validity field of the publisher configuration
     */
    protected abstract void updateValidity(String pubKey,
                                           String config, String ruleKey, String validity);
    /*
     *  Updates the publisher information of the modified publisher. It invokes RulesValidator to validate the rule
     */
    protected abstract void processPublisherModified(String pubKey, String description, String marketLink);
    /*
     *  Updates the publisher information of the Added publisher. It invokes RulesValidator to validate the rule
     */
    protected abstract void processPublisherAdded(String pubKey, String description, String marketLink);
    /*
     *  Marks all the configuration of the deleted publisher as invalid. It invokes RulesValidator to validate the rule
     */
    protected abstract void processPublisherDeleted(String pubKey);
    /*
     * Processes refresh request all the publisher configurations used in the list of rules
     */
    protected abstract void processRefreshRequest(List<String> ruleList);


    /**
     * Constructor
     * @param context Application context
     */
    PublisherManager(Context context) {
        mContext = context;
        mRvInterface = new RulesValidatorInterface(context);
    }

    /**
     * Factory function to get the instance of the publisher manger based on type
     * @param context Application context
     * @param type Type of the publisher ACTION, CONDITION or RULE
     * @return instance of PublisherManger based on given type
     */
    public static PublisherManager getPublisherManager(Context context, String type) {
        PublisherManager pubMgr = null;
        if(type == null || (!type.equals(ACTION) && !type.equals(RULE)
                            && !type.equals(CONDITION))) {
            throw new IllegalArgumentException("Unsupported Publisher type : " + type);
        } else if(type.equals(ACTION)) {
            pubMgr = new ActionPublisherManager(context);
        } else if(type.equals(RULE)) {
            pubMgr = new RulePublisherManager(context);
        } else if(type.equals(CONDITION)) {
            pubMgr = new ConditionPublisherManager(context);
        } else {
            throw new IllegalArgumentException("Unsupported Publisher type : " + type);
        }
        return pubMgr;
    }

    /**
     * Verifies whether the given publisher is valid.
     * @param context Application context
     * @param publisherKey PublisherKey
     * @return true is the give publisher is available and blacklisted else false
     */
    public static boolean isPublisherValid(Context context, String publisherKey) {
        boolean valid = true;

        //checking dynamic for publishers that might not be supported
        //on products due to h/w factors, or conflicting requirements
        if( publisherKey.equalsIgnoreCase(LOCATION_TRIGGER_PUB_KEY)) {
            if(Util.hideLocationTrigger(context)) {
                valid = false;
            }
        } else if(publisherKey.equalsIgnoreCase(PROCESSOR_SPD_PUB_KEY)) {
            if(	!Util.isProcessorSpeedSupported(context)) {
                valid = false;
            }
        }
        if(valid) {
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = null;
            String[] projection = {PublisherProviderInterface.Columns.BLACKLIST};
            String where = PublisherProviderInterface.Columns.PUBLISHER_KEY + EQUALS +  Q + publisherKey + Q;
            try {
                cursor = cr.query(PublisherProviderInterface.CONTENT_URI, projection, where, null, null);
                if(cursor != null && cursor.moveToFirst()) {
                    int blkListed = cursor.getInt(cursor.getColumnIndex(PublisherProviderInterface.Columns.BLACKLIST));
                    boolean isBlackListed = (blkListed ==  PublisherProviderInterface.BlackList.FALSE) ? false : true;
                    if(!isBlackListed)
                        valid = true;
                    else
                        valid = false;
                } else {
                    valid = false;
                }
            } catch(Exception e) {
                Log.e(TAG, "Exception in query " + e, e);
            } finally {
                if(cursor != null) cursor.close();
            }
        }
        if(LOG_DEBUG) Log.d(TAG,"isPublisherValid : " + valid);
        return valid;
    }

    /**
     * Check if given publisher is available and not Blacklisted
     * @param pubKey Publisher Key
     * @return true is publisher is available and not blacklisted
     */
    public boolean isValidPublisher(String pubKey) {
        return mValidPublishers.contains(pubKey);
    }

    /*
     * Populates Valid Publisher list and Black listed publisher list
     */
    protected void populateValidPublishers() {
        ContentResolver cr = mContext.getContentResolver();
        Cursor cursor = null;
        String[] projection = {PublisherProviderInterface.Columns.PUBLISHER_KEY,
                               PublisherProviderInterface.Columns.BLACKLIST
                              };
        String where = PublisherProviderInterface.Columns.TYPE + EQUALS + Q + type + Q;
        try {
            cursor = cr.query(PublisherProviderInterface.CONTENT_URI, projection, where, null, null);

            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String pubKey = cursor.getString(cursor.getColumnIndex(PublisherProviderInterface.Columns.PUBLISHER_KEY));
                    int blkListed = cursor.getInt(cursor.getColumnIndex(PublisherProviderInterface.Columns.BLACKLIST));
                    boolean isBlackListed = (blkListed ==  PublisherProviderInterface.BlackList.FALSE) ? false : true;
                    //checking dynamic for publishers that might not be supported
                    //on products due to h/w factors, or conflicting requirements
                    if(type.equals(CONDITION) && pubKey.equalsIgnoreCase(LOCATION_TRIGGER_PUB_KEY)) {
                        if(Util.hideLocationTrigger(mContext)) {
                            isBlackListed = true;
                        }
                    }
                    if(type.equals(ACTION) && pubKey.equalsIgnoreCase(PROCESSOR_SPD_PUB_KEY)) {
                        if(	!Util.isProcessorSpeedSupported(mContext)) {
                            isBlackListed = true;
                        }
                    }
                    if(!isBlackListed)
                        mValidPublishers.add(pubKey);
                    else
                        mBlacklist.add(pubKey);
                } while (cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Exception in query " + e, e);
        } finally {
            if(cursor != null) cursor.close();
        }
        if(LOG_DEBUG) Log.d(TAG,"Blacklisted publishers : " + mBlacklist.toString());
    }

    /**
     * Handles refresh response from publisher. Stops the Refresh timer for the publisher
     * @param refreshResponseIntent Refresh response intent
     */
    public void handleRefreshResponse(Intent refreshResponseIntent) {
        String responseId = refreshResponseIntent.getStringExtra(EXTRA_RESPONSE_ID);
        String[] responseIdSplit = responseId.split(COLON);
        String pubKey = refreshResponseIntent.getStringExtra(EXTRA_PUBLISHER_KEY);
        String oldConfig = Uri.decode(responseIdSplit[1]);

        if(LOG_DEBUG) Log.d(TAG,"handleRefreshResponse type : " + type);
        if(oldConfig != null) {
            RefreshTimer timer = new RefreshTimer(mContext, null, pubKey, oldConfig, responseId);
            timer.stop();
        }
        String ruleKey = responseIdSplit[2];
        if (ruleKey != null && RulePersistence.getRuleIdForRuleKey(mContext, ruleKey) != DEFAULT_RULE_ID)
            processRefreshResponse(refreshResponseIntent);
        else
            Log.e(TAG, "Rule does not exist in db for rulekey " + ruleKey);

    }

    /**
     * Handle Refresh request for given publisher key and config of given Rule.
     * If the give publisher is not available / blacklisted, then it will be marked as Invalid
     * and invokes RulesValidator to validate the rule
     * @param ruleKey RuleKey of the rule
     * @param ruleSource Rule Source
     * @param pubKey Publisher Key
     * @param config Configuration of publisher
     * @param extraArg optional argument
     * @param silent Whether this rule is silent
     */
    public void handleRefreshRequest(String ruleKey, String ruleSource, String pubKey,
                                     String config, String extraArg, boolean silent) {
        if(LOG_DEBUG) Log.d(TAG,"handleRefreshRequest : ruleKey = " + ruleKey +
                                "\n ruleSource = " + ruleSource +
                                "\n pubKey = " + pubKey +
                                "\n config = " + config +
                                "\n extraArg = " + extraArg +
                                "\n silent = " + silent +
                                "\n importType " + mImportType);
        if(mValidPublishers.contains(pubKey)) {
            if(!Util.isNull(config)) {
                if(mPublisherUpdateReason == null || (mPublisherUpdateReason != null &&
                                                      !mPublisherUpdateReason.equals(LOCALE_CHANGED)))
                    updateValidity(pubKey, config, ruleKey, TableBase.Validity.INPROGRESS);
                String responseId = type + COLON +	Uri.encode(config)
                                    + COLON + ruleKey  + COLON +
                                    ruleSource  + COLON + Boolean.toString(silent) + COLON + mImportType
                                    + COLON + mPublisherUpdateReason + COLON + mReqId;
                sendRefreshCommand(pubKey, responseId, config, type, extraArg);
            } else {
                updateValidity(pubKey, config, ruleKey, TableBase.Validity.VALID);
                mRvInterface.pokeRulesValidatorForPublisher(ruleKey, ruleSource, silent,
                        mImportType, mReqId, mPublisherUpdateReason);
            }
        } else {
            if(mBlacklist.contains(pubKey)) {
                updateValidity(pubKey, config, ruleKey, TableBase.Validity.BLACKLISTED);
            } else {
                updateValidity(pubKey, config, ruleKey, TableBase.Validity.UNAVAILABLE);
            }
//            updateValidity(pubKey, config, ruleKey, TableBase.Validity.INVALID);
            mRvInterface.pokeRulesValidatorForPublisher(ruleKey, ruleSource, silent,
                    mImportType, mReqId, mPublisherUpdateReason);
        }
    }

    /**
     * Updates the publisher information of the modified publisher
     * @param pubKey Publisher Key
     * @param description Updated description
     * @param marketLink Updated Market link
     */
    public void handlePublisherModified(String pubKey, String description, String marketLink) {
        processPublisherModified(pubKey, description, marketLink);
    }

    /**
     * Updates the publisher information of the added publisher
     * @param pubKey Publisher Key
     * @param description New description
     * @param marketLink New Market link
     */
    public void handlePublisherAdded(String pubKey, String description, String marketLink) {
        processPublisherAdded(pubKey, description, marketLink);
    }

    /**
     * Marks the removed publisher as Invalid
     * @param pubKey Publisher Key
     */
    public void handlePublisherDeleted(String pubKey) {
        processPublisherDeleted(pubKey);
    }

    /**
     * Handles refreshTimeout, If the timedout publisher is still IN_PROGRESS, then marks it as Invalid
     * @param refreshTimeoutIntent Timeout intent
     */
    public void handleRefreshTimeout(Intent refreshTimeoutIntent) {
        String responseId = refreshTimeoutIntent.getStringExtra(EXTRA_REQUEST_ID);
        String[] responseIdSplit = responseId.split(COLON);
        if(responseIdSplit[6].equals(LOCALE_CHANGED)) return;
        String config  = refreshTimeoutIntent.getStringExtra(EXTRA_CONFIG);
        String pubKey = refreshTimeoutIntent.getStringExtra(EXTRA_PUBLISHER_KEY);;
        String ruleKey = responseIdSplit[2];
        String ruleSrc = responseIdSplit[3];
        boolean silent = Boolean.valueOf(responseIdSplit[4]);
        processRefreshTimeout(ruleKey, ruleSrc, pubKey, config, silent);
    }

    /*
     * Broadcasts the refresh command to publishers
     */
    protected void sendRefreshCommand(String publisherKey,
                                      String responseId,
                                      String publisherConfig,
                                      String pubType, String uriToFire) {
        if(LOG_DEBUG) Log.d(TAG, " sendRefreshCommand " +
                                "publisherKey " + publisherKey + "," +
                                "responseId " + responseId + "," +
                                "publisherConfig " + publisherConfig + "," +
                                "pubType " + pubType);
        Intent intent = null;
        if (pubType.equalsIgnoreCase(CONDITION)) {
            intent = new Intent(ACTION_CONDITION_PUBLISHER_REQUEST);
            intent.putExtra(EXTRA_PUBLISHER_KEY, publisherKey);
            intent.putExtra(EXTRA_CONSUMER, SA_CORE_KEY);
            intent.putExtra(EXTRA_CONFIG, publisherConfig);
        } else {
            intent = new Intent(publisherKey);
            if(!pubType.equals(RULE)) {
                intent.putExtra(EXTRA_VERSION, ACTION_PUBLISHER_VERSION);
                intent.putExtra(EXTRA_CONFIG, publisherConfig);
                if(uriToFire != null) intent.putExtra(EXTRA_DEFAULT_URI, uriToFire);
            }
        }
        intent.putExtra(EXTRA_COMMAND, COMMAND_REFRESH);
        intent.putExtra(EXTRA_REQUEST_ID, responseId);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        if (pubType.equalsIgnoreCase(CONDITION)) {
            mContext.sendBroadcast(intent, PERM_CONDITION_PUBLISHER_ADMIN);
        } else if (pubType.equalsIgnoreCase(ACTION)) {
            mContext.sendBroadcast(intent, PERM_ACTION_PUBLISHER);
        } else if (pubType.equalsIgnoreCase(RULE)) {
            mContext.sendBroadcast(intent, PERM_RULE_PUBLISHER);
        } else {
            mContext.sendBroadcast(intent);
        }

        if(mReqId == null && !pubType.equals(RULE)) {
            RefreshTimer timer = new RefreshTimer(mContext, intent, publisherKey, publisherConfig, responseId);
            timer.start(REFRESH_TIMEOUT);
        }
    }

    /*
     * This method is used to delete the rule
     */
    protected void deleteRule(String ruleKey) {
        if(LOG_DEBUG) Log.d(TAG, "deleteRule : " + ruleKey);
        RulePersistence.deleteRule(mContext, RulePersistence.getRuleIdForRuleKey(mContext, ruleKey), null, ruleKey, false);
    }

    /*
     * Helper class for starting and stopping Refresh Timer
     */
    protected static class RefreshTimer {
        private PendingIntent mPi = null;
        private Intent mRefreshIntent = null;
        private String mPubKey = null;
        private String mConfig = null;
        private String mRespondId = null;
        private Context mContext;

        public RefreshTimer(Context context, ArrayList<String> ruleList, String reqId, String pubType) {
            mContext = context;
            mPi = getPi(ruleList, reqId, pubType);
        }

        public RefreshTimer(Context context, Intent refreshIntent, String pubKey, String config, String respondId) {
            mContext = context;
            mRefreshIntent = refreshIntent;
            mPubKey = pubKey;
            mConfig = config;
            mRespondId = respondId;
            mPi = getPi();
        }

        private PendingIntent getPi(ArrayList<String> ruleList, String reqId, String pubType) {
            String intentAction = ACTION_PUBLISHER_REFRESH_TIMEOUT + reqId.hashCode() + pubType.hashCode();
            if(LOG_DEBUG) Log.d(TAG,"RefreshTimer action : " + intentAction);
            Intent amIntent = new Intent(intentAction);
            amIntent.putExtra(EXTRA_RULE_LIST, ruleList);
            amIntent.putExtra(EXTRA_REQUEST_ID, reqId);
            amIntent.putExtra(EXTRA_PUBLISHER_TYPE, pubType);
            amIntent.setClass(mContext, RulesValidatorService.class);
            PendingIntent pi = PendingIntent.getService(mContext, 0, amIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            return pi;
        }

        private PendingIntent getPi() {
            String intentAction = mPubKey + (mConfig.hashCode() + mRespondId.hashCode());
            if(LOG_DEBUG) Log.d(TAG,"RefreshTimer action : " + intentAction);
            Intent amIntent = new Intent(intentAction);
            if(mRefreshIntent != null) amIntent.putExtras(mRefreshIntent.getExtras());
            amIntent.putExtra(EXTRA_PUBLISHER_KEY, mPubKey);
            amIntent.setClass(mContext, PublisherManagerService.class);
            PendingIntent pi = PendingIntent.getService(mContext, 0, amIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            return pi;
        }

        public void start(long timeOut) {
            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + timeOut, mPi);
        }

        public void stop() {
            AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            am.cancel(mPi);
        }
    }

    /*
     * Helper class for parameters used for refresh
     */
    protected class RefreshParams {
        private String mRuleKey;
        private String mRuleSrc;
        private String mPubKey;
        private String mConfig;

        public RefreshParams(String ruleKey, String ruleSrc, String pubKey, String config) {
            mRuleKey = ruleKey;
            mRuleSrc = ruleSrc;
            mPubKey = pubKey;
            mConfig = config;
        }

        public void sendRefreshRequest() {
            handleRefreshRequest(mRuleKey, mRuleSrc, mPubKey, mConfig, null, true);
        }

        @Override
        public boolean equals(Object obj) {

            RefreshParams comparedRefreshParams = (RefreshParams) obj;
            if(mPubKey != null && !mPubKey.equals(comparedRefreshParams.mPubKey)) return false;
            if(mConfig != null && !mConfig.equals(comparedRefreshParams.mConfig)) return false;
            if(mRuleKey != null && !mRuleKey.equals(comparedRefreshParams.mRuleKey)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            // Start with a non-zero constant.
            int result = 17;
            // Include a hash for each field.
            result = 31 * result + ((mPubKey == null) ? 0 : mPubKey.hashCode());
            result = 31 * result + ((mConfig == null) ? 0 : mConfig.hashCode());
            return result;
        }
    }

    /**
     * Processes Refresh timeout of the publishers in the Rulelist
     * @param ruleList List containing the ruleKeys
     */
    public void processRefreshTimeout(List<String> ruleList) {
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
        Cursor cursor = null;
        Uri contentUri = RuleActionView.CONTENT_URI;
        String[] columnNameArray = new String[5];
        columnNameArray[0] = RuleTableColumns.KEY;
        columnNameArray[1] = RuleTableColumns.SOURCE;
        if(type.equals(ACTION)) {
            contentUri = RuleActionView.CONTENT_URI;
            columnNameArray[2] = ActionTableColumns.ACTION_PUBLISHER_KEY;
            columnNameArray[3] = ActionTableColumns.CONFIG;
            columnNameArray[4] = ActionTableColumns.ACTION_VALIDITY;
        } else if(type.equals(CONDITION)) {
            contentUri = RuleConditionView.CONTENT_URI;
            columnNameArray[2] = ConditionTableColumns.CONDITION_PUBLISHER_KEY;
            columnNameArray[3] = ConditionTableColumns.CONDITION_CONFIG;
            columnNameArray[4] = ConditionTableColumns.CONDITION_VALIDITY;
        }
        String whereClause = RuleTableColumns.KEY + IN + ruleListString.toString() + AND +
                             columnNameArray[4] + EQUALS + Q + RuleTable.Validity.INPROGRESS + Q;
        try {
            cursor = cr.query(contentUri, columnNameArray, whereClause, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    String ruleKey = cursor.getString(cursor.getColumnIndex(columnNameArray[0]));
                    String ruleSrc = cursor.getString(cursor.getColumnIndex(columnNameArray[1]));
                    String pubKey = cursor.getString(cursor.getColumnIndex(columnNameArray[2]));
                    String config = cursor.getString(cursor.getColumnIndex(columnNameArray[3]));
                    processRefreshTimeout(ruleKey, ruleSrc, pubKey, config, true);
                } while(cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Query failed with exception : " + e, e);
        } finally {
            if(cursor != null) cursor.close();
        }
    }

    /*
     * Setters and getters
     */
    public int getmImportType() {
        return mImportType;
    }

    public void setmImportType(int mImportType) {
        this.mImportType = mImportType;
    }

    public String getmPublisherUpdateReason() {
        return mPublisherUpdateReason;
    }

    public void setmPublisherUpdateReason(String mPublisherUpdateReason) {
        this.mPublisherUpdateReason = mPublisherUpdateReason;
    }

    public String getmReqId() {
        return mReqId;
    }

    public void setmReqId(String mReqId) {
        this.mReqId = mReqId;
    }
}
