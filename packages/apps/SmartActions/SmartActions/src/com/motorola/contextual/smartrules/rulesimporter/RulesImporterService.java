/*
 * @(#)CannedRulesService.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2011/01/18 NA				  Initial version
 * A18172        2011/02/21 NA				  Updated with review comments
 * 											  from Craig
 * A18172        2011/03/08 NA                Intent support for Inference Manager
 *                                            with xmlString as an extra
 * A18172        2011/03/17 IKINTNETAPP-54    New requirement for Inference Manager
 *                                            to launch Condition Builder as well
 */
package com.motorola.contextual.smartrules.rulesimporter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.publishermanager.PublisherManager;
import com.motorola.contextual.smartrules.util.Util;


/**
 * This Service is currently exposed
 * (1) to support Automation test framework
 * where in the xml file is being read from the sdcard (location : /sdcard/rules.d)
 * and the filename is "rulesimporter.rules". Once the rule is importerd, the
 * condition builder module is launched to create sensors
 * (2) Parse the xml String and inserts the data into the Smart Rules DB, if the xml
 * string content is send via an intent.
 *
 * @author bluremployee
 *
 */
public class RulesImporterService extends Service implements Constants, DbSyntax {


    private static final String TAG = RulesImporterService.class.getSimpleName();

    // Accepts tasks to run executing only one at a time queuing the rest.
    private static final ExecutorService mQueueExecutor = Executors.newSingleThreadExecutor();

    /** onStartCommand()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (LOG_DEBUG) Log.d(TAG, "RulesImporterService Started with intent " + intent.toUri(0));

        // Accepts tasks to run executing only one at a time queuing the rest.
        mQueueExecutor.submit(new ProcessRulesImporter(mHandler,intent, this));

        return START_NOT_STICKY;
    }

    /** onBind()
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     *
     * Runnable to process the Rules Importer
     *
     *
     */
    private  static final class ProcessRulesImporter implements Runnable {

        private Context mContext;
        Handler callBackHandle = null;

        // Import type
        private int importType = XmlConstants.ImportType.IGNORE;
        private String mRequestId = null;
        // The xml String content sent from inference manager
        private String  xmlString = null;
        // pubKey from RP
        private String mPubKey = null;
        
        //Core Protocol Version supported by the Publisher
        @SuppressWarnings("unused")
		private Float mCoreVersion = 0.0f;
        private String mParentRuleKey = null;

        /** constructor
         */
        public ProcessRulesImporter(Handler callBack, Intent intent, Context context) {
            mContext = context;
            callBackHandle = callBack;

            importType = intent.getIntExtra(IMPORT_TYPE, XmlConstants.ImportType.IGNORE);
            xmlString = (importType == XmlConstants.ImportType.RP_RULE) ?
            		intent.getStringExtra(NEW_XML_CONTENT) : 
            				intent.getStringExtra(XML_CONTENT);
            mRequestId = intent.getStringExtra(EXTRA_REQUEST_ID);
            mParentRuleKey = intent.getStringExtra(RuleTable.Columns.PARENT_RULE_KEY);
            mPubKey = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
            mCoreVersion = intent.getFloatExtra(EXTRA_VERSION, INVALID_VERSION);
            mRequestId = (Util.isNull(mRequestId)) ? 
			Integer.toString(INVALID_REQUEST_ID) : mRequestId;
        }

        /**
        * Thread started to get the Rules Importer XML parsing completed
        */
        public void run() {

  
            // Priority should be one less than that of ConditionBuilder
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY - 3);
            Thread.currentThread().setName("RulesImporter");
            
            // For all other cases except for rule published by RP
            // by default the RulesImporter should be launched.
            boolean launchRI = true;
            
            // If the import request is from RP then check whether the publisher is valid
            // means available and not blacklisted
            if(importType == XmlConstants.ImportType.RP_RULE){
            	// Reset to false
            	launchRI = false;
            	
            	if (LOG_DEBUG) Log.d(TAG,"Check the validity of the publisher : "+mPubKey);

	            String type = com.motorola.contextual.smartrules.publishermanager.PublisherManagerConstants.RULE;
	            PublisherManager pubMgr = PublisherManager.getPublisherManager(mContext, type);
	            if (pubMgr !=null){
	            	launchRI = pubMgr.isValidPublisher(mPubKey);
	            }else{
	            	Log.e(TAG,"pubMgr is null for pubKey : "+ mPubKey);
	            }
	            
	            if (LOG_DEBUG) Log.d(TAG,"Validity of the publisher : "+mPubKey +" Validity : "+launchRI);
            }

            if (LOG_DEBUG) Log.d(TAG, "New Rulesimporter instance importType=" + importType + "; mRequestId=" + mRequestId + ";mParentRuleKey=" + mParentRuleKey);
            
            if(launchRI){
	            RulesImporter ri = new RulesImporter(mContext, importType, mRequestId, mPubKey, mParentRuleKey);
	            ri.startRulesImporter(xmlString);
            }else{
            	if (LOG_DEBUG) Log.d(TAG,"RI not launched for RP pubKey : "+ mPubKey);
            }

            // kill service
            Message responseMsg = Message.obtain();
            responseMsg.setTarget(callBackHandle);
            responseMsg.sendToTarget();
        }
    }

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            stopSelf();
        }
    };
}
