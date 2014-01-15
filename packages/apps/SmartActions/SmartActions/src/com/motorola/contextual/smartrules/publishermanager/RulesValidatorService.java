package com.motorola.contextual.smartrules.publishermanager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.motorola.contextual.smartrules.publishermanager.PublisherManager.RefreshTimer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/** Service to handle RuleValidate request
 *<code><pre>
 * CLASS:
 *     RulesValidatorService Extends Service
 * Interface:
 * 		PublisherManagerConstants,
 * 		DbSyntax
 *
 * RESPONSIBILITIES:
 * 		handle RuleValidate request
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class RulesValidatorService extends Service implements Callback, PublisherManagerConstants {
    private static final String TAG = RulesValidatorService.class.getSimpleName();

    public static final String INPROGRESS = "inprogress";

    public static final int RULE_LIST_VALIDATE_REQUEST = 101;

    public static final int RULE_LIST_PUBLISHER_REFRESH_TIMEOUT = 102;

    private HandlerThread rvReqThread;

    private Handler rvReqHandler;

    private RuleListValidateRequestMap rvReqMap = null;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        rvReqThread = new HandlerThread(TAG);
        rvReqThread.setPriority(Thread.NORM_PRIORITY-1);
        rvReqThread.start();
        Looper looper = rvReqThread.getLooper();
        if(looper != null) rvReqHandler = new Handler(looper, this);
        rvReqMap = new RuleListValidateRequestMap(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if(intent != null) {
            if(LOG_INFO) Log.i(TAG,"onStartCommand Intent : " + intent.toUri(0));
            String action = intent.getAction();
            if(action == null) {
                if(LOG_DEBUG) Log.d(TAG,"Process has restarted");
                return START_NOT_STICKY;
            } else if(action.equals(ACTION_RULES_VALIDATE_REQUEST)) {
                List<String> ruleList = intent.getStringArrayListExtra(EXTRA_RULE_LIST);
                String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
                rvReqMap.put(requestId, ruleList);
            } else if(action.equals(ACTION_RULE_VALIDATED)) {
                String ruleKey = intent.getStringExtra(EXTRA_RULE_KEY);
                String ruleStatus = intent.getStringExtra(EXTRA_RULE_STATUS);
                String resId = intent.getStringExtra(EXTRA_RESPONSE_ID);
                rvReqMap.update(resId, ruleKey, ruleStatus);
            }  else if(action.startsWith(ACTION_PUBLISHER_REFRESH_TIMEOUT)) {
                List<String> ruleList = intent.getStringArrayListExtra(EXTRA_RULE_LIST);
                String requestId = intent.getStringExtra(EXTRA_REQUEST_ID);
                String pubType = intent.getStringExtra(EXTRA_PUBLISHER_TYPE);
                handleRuleListPublisherRefreshTimeout(ruleList, requestId, pubType);
            }
        }
        return START_STICKY;
    }

    /*
     * Handles the Refresh timeout of the publisher configurations used in given list of rules
     */
    private void handleRuleListPublisherRefreshTimeout(List<String> ruleList,
            String requestId, String pubType) {
        ArrayList<String> deltaRuleList = new ArrayList<String>();
        HashMap<String, String> ruleStautsMap = null;

        if(rvReqMap.containsKey(requestId)) {
            ruleStautsMap = rvReqMap.get(requestId);

            for(String ruleKey : ruleList) {
                if(ruleStautsMap.get(ruleKey).equals(IN_PROGRESS)) {
                    deltaRuleList.add(ruleKey);
                }
            }
            if(deltaRuleList.size() > 0) {
                Message msg = rvReqHandler.obtainMessage(RULE_LIST_PUBLISHER_REFRESH_TIMEOUT);
                Bundle bundle = new Bundle();
                bundle.putString(EXTRA_REQUEST_ID, requestId);
                bundle.putString(EXTRA_PUBLISHER_TYPE, pubType);
                bundle.putStringArrayList(EXTRA_RULE_LIST, deltaRuleList);
                msg.setData(bundle);
                rvReqHandler.sendMessage(msg);
            }
        }
    }

    /*
     * Starts a thread to validate rule List
     */
    private void processValidateRuleList(List<String> ruleList, String requestId) {
        if(LOG_DEBUG) Log.d(TAG,"handleValidateRuleList called with requestId : " +
                                requestId + " ruleList : " + ruleList.toString());
        new validateRuleListThread(this, ruleList, requestId).start();
    }

    /*
     * helper class to maintain the map of request Id and list of rules
     */
    private class RuleListValidateRequestMap extends LinkedHashMap<String, LinkedHashMap<String, String>> {

        private static final long serialVersionUID = 9143336200045703808L;
        private Context mContext;

        public RuleListValidateRequestMap(Context context) {
            mContext = context;
        }

        public void put(String reqId, List<String> ruleList) {
            if(ruleList == null || ruleList.size() == 0) return;
            LinkedHashMap<String, String> ruleStautsMap = null;
            if(!this.containsKey(reqId)) {
                ruleStautsMap = new LinkedHashMap<String, String>();
                this.put(reqId, ruleStautsMap);
            } else {
                ruleStautsMap = this.get(reqId);
            }
            for(String ruleKey : ruleList) {
                ruleStautsMap.put(ruleKey, IN_PROGRESS);
            }
            Message msg = rvReqHandler.obtainMessage(RULE_LIST_VALIDATE_REQUEST);
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_REQUEST_ID, reqId);
            bundle.putStringArrayList(EXTRA_RULE_LIST, (ArrayList<String>) ruleList);
            msg.setData(bundle);
            rvReqHandler.sendMessage(msg);
        }

        public void update(String reqId, String ruleKey, String status) {
            HashMap<String, String> ruleStatusMap = null;
            if(this.containsKey(reqId)) {
                ruleStatusMap = this.get(reqId);
                if(ruleStatusMap != null) {
                    ruleStatusMap.put(ruleKey, status);
                    Collection<String> ruleStatusCollection = ruleStatusMap.values();
                    if(!ruleStatusCollection.contains(IN_PROGRESS)) {
                        RefreshTimer timer = new RefreshTimer(mContext, new ArrayList<String>(ruleStatusMap.keySet()), reqId, ACTION);
                        timer.stop();
                        timer = new RefreshTimer(mContext, new ArrayList<String>(ruleStatusMap.keySet()), reqId, CONDITION);
                        timer.stop();
                        Intent validateResponseIntent = new Intent(ACTION_RULES_VALIDATE_RESPONSE);
                        validateResponseIntent.putExtra(EXTRA_RULE_STATUS, ruleStatusMap);
                        validateResponseIntent.putExtra(EXTRA_RESPONSE_ID, reqId);
                        sendBroadcast(validateResponseIntent);
                        this.remove(reqId);
                    }
                }
            }
            if(this.isEmpty()) {
                if(LOG_DEBUG) Log.d(TAG,"Stoping the service");
                stopSelf();
            }
        }
    }

    public boolean handleMessage(Message msg) {
        if(LOG_DEBUG) Log.d(TAG,"handleMessage msg : " + msg.what);
        Bundle bundle = msg.getData();
        ArrayList<String> ruleList = bundle.getStringArrayList(EXTRA_RULE_LIST);
        String reqId = bundle.getString(EXTRA_REQUEST_ID);
        switch (msg.what) {
        case RULE_LIST_VALIDATE_REQUEST:
            processValidateRuleList(ruleList, reqId);
            break;
        case RULE_LIST_PUBLISHER_REFRESH_TIMEOUT:
            String pubType = bundle.getString(EXTRA_PUBLISHER_TYPE);
            if(pubType != null)
                processRuleListRefreshTimeout(ruleList, reqId, pubType);
            break;
        default:
            break;
        }
        return false;
    }

    /*
     * Starts a thread to process Refresh timeout
     */
    private void processRuleListRefreshTimeout(ArrayList<String> ruleList,
            String reqId, String pubType) {
        if(ruleList == null || ruleList.size() == 0) return;
        if(LOG_DEBUG) Log.d(TAG,"processRuleListRefreshTimeout called with requestId : " +
                                reqId + " ruleList : " + ruleList.toString());
        new handleRefreshTimeoutThread(this, ruleList, reqId, pubType).start();      
    }
    
    /** 
     * Thread that handles the Validates the rule list 
     * Sends refresh to all the publisher config pair used in the list of rules
     */
    private class validateRuleListThread extends Thread {        
        Context context;
        List<String> ruleList;
        String requestId;
        
        /** only constructor that should be used */
        public validateRuleListThread(Context context, List<String> ruleList, String requestId) {
            super("validateRuleListThread");
            this.context = context;
            this.ruleList = ruleList;
            this.requestId = requestId;
            this.setPriority(Thread.NORM_PRIORITY-1);
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#destroy()
         */
        @Override
        public void destroy() {
            super.destroy();
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            super.run();
            PublisherManager actionPublisherManager = PublisherManager.getPublisherManager(context, ACTION);
            actionPublisherManager.setmReqId(requestId);
            PublisherManager conditionPublisherManager = PublisherManager.getPublisherManager(context, CONDITION);
            conditionPublisherManager.setmReqId(requestId);
            actionPublisherManager.processRefreshRequest(ruleList);
            conditionPublisherManager.processRefreshRequest(ruleList);
        }
    }
    
    /** 
     * Thread which processes the timeout of refresh requests 
     * for the publisher configurations used in given list of rules
     */
    private class handleRefreshTimeoutThread extends Thread {        
        Context context;
        List<String> ruleList;
        String requestId;
        String pubType;
        
        /** only constructor that should be used */
        public handleRefreshTimeoutThread(Context context, List<String> ruleList, 
                String requestId, String pubType) {
            super("validateRuleListThread");
            this.context = context;
            this.ruleList = ruleList;
            this.requestId = requestId;
            this.pubType = pubType;
            this.setPriority(Thread.NORM_PRIORITY-1);
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#destroy()
         */
        @Override
        public void destroy() {
            super.destroy();
        }

        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            super.run();
            if(pubType.equals(ACTION)) {
                PublisherManager actionPublisherManager = PublisherManager.getPublisherManager(context, ACTION);
                actionPublisherManager.setmReqId(requestId);
                actionPublisherManager.processRefreshTimeout(ruleList);
            } else {
                PublisherManager conditionPublisherManager = PublisherManager.getPublisherManager(context, CONDITION);
                conditionPublisherManager.setmReqId(requestId);
                conditionPublisherManager.processRefreshTimeout(ruleList);
            }
        }
    }
        
}
