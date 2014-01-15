/*
 * @(#)PublisherUiController.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A18172        2012/23/04 NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.suggestions;


import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app.LandingPageActivity;
import com.motorola.contextual.smartrules.app.LandingPageActivity.LandingPageIntentExtras;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.business.SuggestionsPersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.publishermanager.RulesValidatorInterface;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants.ImportType;
import com.motorola.contextual.smartrules.util.Util;
import com.motorola.contextual.smartrules.db.DbSyntax;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;


public class PublisherUiController extends Activity implements Constants,
 DialogInterface.OnClickListener, DialogInterface.OnCancelListener{

    private static final String TAG = PublisherUiController.class.getSimpleName();
    private long mRuleId = -1;
    private Context mContext = null;
    
    // Ignore the Activity Not Found exception in onActivityResult
	private boolean mIgnoreException = false;
    private int mAdoptCount = 0;
	private AlertDialog mAlertDialog = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate " + mRuleId);
        // remove the window title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        mContext = this;

        // get extras from the intent
        mRuleId = getIntent().getLongExtra(PUZZLE_BUILDER_RULE_ID, RuleTable.RuleType.DEFAULT);
        Log.d(TAG, "onCreate " + mRuleId);
        if(mRuleId > 0){
             // is this the first launch?
            if (Suggestions.isInitState(mContext)) {
                showInitSuggDialog();
            } else{
                showRulePublisherUi(mRuleId);
            }
        } else {
        	if (LOG_DEBUG) Log.d(TAG,"Finish");
            finish();
        }
    }
    
    /*
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {

	if(LOG_DEBUG) Log.d(TAG,"onDestroy");
        // kill the dialog
        if(mAlertDialog != null) mAlertDialog.cancel();

        super.onDestroy();
    }

    private void showInitSuggDialog(){

	if(LOG_DEBUG) Log.d(TAG,"showInitSuggDialog");

        // Inflate our custom layout for the dialog
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        ViewGroup rootView = null;
        rootView = (ViewGroup)inflater.inflate(R.layout.suggestion_dialog_init, null);

        // This could never happen but
        // no point in going ahead if this is true
        if (rootView == null) {
            Log.e(TAG, "rootView is NULL > Impossible!");
            finish();
            return;
        }

        // show the alert dialog with our custom view
        if (LOG_INFO)
            Log.i(TAG, "Creating Suggestion Alert Dialog");
        showAlertDialog(rootView);

        // mark the suggestion as READ
        //SuggestionsPersistence.setSuggestionState(mContext, mRuleId, RuleTable.SuggState.READ);

        // Remove notification bar
        Suggestions.removeNotification(mContext);
    }

     /**
     * Build the alert dialog with our custom layout
     *
     * @param view - root view of our custom layout
     */
    private void showAlertDialog(ViewGroup view) {

		if(LOG_DEBUG) Log.d(TAG,"showAlertDialog");
	
		AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(mContext);

        // set the custom layout and the title
        builder.setView(view);
        builder.setTitle(getString(R.string.sg_init_title));
        builder.setIcon(R.drawable.suggestion_card_app_icon);

        // set buttons depending on suggestion type
        // IMP: "initial" should be the first condition!
        builder.setPositiveButton(getString(R.string.sg_next), this);
        builder.setNegativeButton(getString(R.string.cancel), this);

         // show the dialog
        mAlertDialog = builder.create();
        mAlertDialog.setOnCancelListener(this);
        mAlertDialog.setInverseBackgroundForced(true);
        mAlertDialog.show();

        Button b = mAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if(b != null) b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b = mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if(b != null) b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
    }

     /**
     * To handle clicks on any of the dialog default buttons
     * <pre><code>
     *      - Yes: a. Fire actions for One time action suggestions
     *             b. Add suggestion as rule
     *      - No and c. Do nothing, just kill the dialog and the activity.
     * </pre></code>
     *
     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface,
     *      int)
     */
    public void onClick(DialogInterface dialog, int button) {

	if (LOG_DEBUG) Log.d(TAG,"onClick D");
        // positive/yes button clicked
        if (button == AlertDialog.BUTTON_POSITIVE) { // YES
               if (LOG_INFO) Log.i(TAG, "onClick: Positive Button");
               Suggestions.setInitState(mContext, false);

                showRulePublisherUi(mRuleId);
        }else if (button == AlertDialog.BUTTON_NEGATIVE) { //No
		 if (LOG_INFO) Log.i(TAG, "onClick: Negative Button");

             // show the rejection dialog
             boolean diagShown = showRejectDialog(mContext);

             // return, else our Act would be killed
             if(diagShown) return;

             finish();
        }
    }

    /**
     * We need to kill this activity when user cancels the dialog (non-Javadoc)
     *
     * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
     */
    public void onCancel(DialogInterface dialog) {
	if(LOG_DEBUG) Log.d(TAG,"onCancel");
        finish();
    }

    /**
     * updates suggestion related columns in smart rules db
     *
     * @param result - Whether to finish the activity or not
     * @param ruleId - rule id
     * @param mSugType - Suggestion type
     * @param ruleKey - Rule Key for the rule
     * @return - to finish or not finish the activity
     */
    private boolean updateSuggestion(Intent intent, long ruleId, 
    		                         int mSugType, String ruleKey,
    		                         String ruleName){

        String data= null;
        boolean finish = true;
        String result = intent.getStringExtra(EXTRA_RP_RESPONSE);

        if(LOG_DEBUG) Log.d(TAG, "Updating result for the result="+result);

        if(result != null){
            if(result.equals(RP_RESPONSE_ACCEPT)){
                 if(mSugType == RuleTable.Lifecycle.NEVER_EXPIRES){
	                if (RulePersistence.anyUnconfiguredEnabledActionsOrConditions(mContext, ruleId)){
	                	
	                	if(LOG_DEBUG) Log.d(TAG,"Atleast one enabled  block is unconfigured");
	                	
	                	if(LOG_DEBUG) Log.d(TAG, "Customize for rule ID "+ruleId);
	                	 
	                     int source = RulePersistence.getColumnIntValue(mContext, 
	                    		 					ruleId, RuleTable.Columns.SOURCE);
	                     mAdoptCount = RulePersistence.launchRulesBuilder(mContext, source, 
	                     												ruleId, RULE_EDIT);

	                     data = SUGG_CUSTOMIZE_DBG_MSG;
	                     finish = false;
	                }else{
	                	if(LOG_DEBUG) Log.d(TAG,"All the enabled blocks are configured");
	                	
	                	if(LOG_DEBUG) Log.d(TAG, "Accept : Setting the suggested state to accepted for rule ID "+ruleId);
		                Suggestions.addSuggestionAsRule(mContext, ruleId);
		                // show Landing page
		                LandingPageActivity.startLandingPageActivity(mContext);
		                data = SUGG_ACCEPTED_DBG_MSG;
	                }
                }else if ( mSugType == RuleTable.Lifecycle.ONE_TIME) {
                	if (LOG_DEBUG) Log.d(TAG,"Lifecycle:ONE_TIME");
                    // If returning from PB, just Fire the suggestion
                    long newRuleId = intent.getLongExtra(LandingPageIntentExtras.RULE_ID_INSERTED, RuleTable.RuleType.DEFAULT);
                    if(newRuleId != mRuleId) {
                        // this means we returned from smart profile
                    	// How to get the data about config ?
                       // updateActionOnResult(intent);
                    }

                    Suggestions.fireInstantActions(mContext, mRuleId);
                } else if (mSugType == RuleTable.Lifecycle.UPDATE_RULE) {
                	if (LOG_DEBUG) Log.d(TAG,"Lifecycle:UPDATE_RULE");
                    // Accept/Enable all actions
                    SuggestionsPersistence.enableAllSuggestedActions(mContext, mRuleId);

                    //launchConfigScreens();
                    //return; // dont let the act be finished yet
                    finish = false;
                } else if (mSugType == RuleTable.Lifecycle.IMMEDIATE) {
                	if (LOG_DEBUG) Log.d(TAG,"Lifecycle:IMMEDIATE");
                    // delete this suggestion
                    RulePersistence.deleteRule(mContext, mRuleId, null, null, false);

                } else if (mSugType == RuleTable.Lifecycle.SWAP_ONE) {
                	if (LOG_DEBUG) Log.d(TAG,"Lifecycle:SWAP_ONE");
                    // replace old condition with new one
                    Suggestions.swapAcceptCondition(mContext, mRuleId);
                } else {
                	if (LOG_DEBUG) Log.d(TAG,"mSugType not supported" +mSugType);	
                }
            } else if(result.equals(RP_RESPONSE_CUSTOMIZE)){

                if(LOG_DEBUG) Log.d(TAG, "Customize for rule ID "+ruleId);
 
                int source = RulePersistence.getColumnIntValue(mContext, mRuleId, RuleTable.Columns.SOURCE);
                mAdoptCount = RulePersistence.launchRulesBuilder(mContext, source, 
                												ruleId, RULE_EDIT);

                data = SUGG_CUSTOMIZE_DBG_MSG;
                finish = false;
            } else {
                if(LOG_DEBUG) Log.d(TAG, "Setting the suggested state to read for rule ID "+ruleId);
                finish = setSuggStateAndShowRejectDialog(ruleId);

                data = SUGG_REJECTED_DBG_MSG;
            }
        } else {
        	 Log.e(TAG, "Unknown case result" +result);
        }

        if(data != null){
            DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, null,
					   ruleName, ruleKey, SMARTRULES_INTERNAL_DBG_MSG, null,  
                       data, Constants.PACKAGE, Constants.PACKAGE);
        }
        
        return finish;
    }

    /**
     * Launches Rule Publisher UI
     *
     * @param uri - intent to launch
     */
    private void showRulePublisherUi(long ruleId){
    	
    	if (LOG_DEBUG) Log.i(TAG,"Fn: showRulePublisherUi");
        // Launch RulesBuilder iff Exception happens
    	boolean launchRB = false;
    	mIgnoreException = false;
    	
        try {
            String uri = RulePersistence.getRuleStringValue(mContext, ruleId, RuleTable.Columns.UI_INTENT);
            if (LOG_DEBUG) Log.d(TAG,"Uri"+uri);
            SuggestionsPersistence.setSuggestionState(mContext, ruleId, RuleTable.SuggState.READ);
            
            // If there is no UI_INTENT then launch Rules Builder
            if (Util.isNull(uri)){
		         if (LOG_DEBUG) Log.d(TAG,"Launch PB Since UI_INTENT is null");
		         launchRB = true;
            }else{
            	if (LOG_DEBUG) Log.d(TAG,"UI Shown by RP");
	            Intent in = Intent.parseUri(uri, 0);
	            startActivityForResult(in, RULE_SUGGESTED);
	            if(LOG_INFO) Log.i(TAG, "Showing Suggestion Card for Rule Id =" + ruleId);
            }
        }  catch (Exception e) {
        	if(LOG_INFO) Log.i(TAG, "Exception for Rule ID : "+ruleId + 
					"Exception : "+e.getMessage());
		launchRB = true;
        	mIgnoreException = (e instanceof ActivityNotFoundException);
        } 
        
        if(launchRB){
       	 	if(LOG_INFO) Log.i(TAG, "URI not valid, Launch PB for rule ID "+mRuleId);
       	 	int source = RulePersistence.getColumnIntValue(mContext, mRuleId, RuleTable.Columns.SOURCE);
            mAdoptCount = RulePersistence.launchRulesBuilder(mContext, source, 
																ruleId, RULE_EDIT);
        }
    }
    
    /**
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if(LOG_DEBUG) Log.d(TAG, "in onActivityResult resultCode = "+
        							resultCode+" requestCode = "+requestCode);

        int mSugType = INVALID; 
        boolean finish = true;
        
        mSugType = RulePersistence.getSuggType(mContext, mRuleId);
        
        if (LOG_DEBUG) Log.d(TAG,"mSugType = "+mSugType);
        
       if(mIgnoreException){
		    // if startActivity() fails because of an exception still onActivityResult()
		    // is invoked. So, need to ignore this invocation.
		    mIgnoreException = false;
			if(LOG_INFO) Log.i(TAG, "Ignore Activtiy Not Found Exception for : "+mRuleId);
	            finish = false;
        }else if (requestCode == RULE_SUGGESTED) {
        	if (LOG_INFO) Log.i(TAG,"Suggested");

        	// Get the RuleKey & Rule Name for Check in Server
        	String whereClause = RuleTable.Columns._ID+ DbSyntax.EQUALS + DbSyntax.Q + mRuleId + DbSyntax.Q;
           
        	Cursor c = RulePersistence.getRuleCursor(mContext, 
        						new String[]{RuleTable.Columns.KEY, RuleTable.Columns.NAME}, 
                                whereClause);
        	String ruleName = null;
        	String ruleKey = null;
        	
        	try{
	            if (c != null && c.moveToFirst()) {
	            	ruleKey = c.getString(c.getColumnIndex(RuleTable.Columns.KEY));
	            	ruleName = c.getString(c.getColumnIndex(RuleTable.Columns.NAME));
				}
			} catch (IllegalArgumentException e){
				e.printStackTrace();
	        } finally {
	            if(c != null) c.close();
	        }
        		
	        if(resultCode == RESULT_OK) {
	            finish = updateSuggestion(intent, mRuleId, mSugType, 
	            							ruleKey, ruleName);
	        } else {
	            if(LOG_INFO) Log.i(TAG, "Setting the suggested state to read for rule ID "+mRuleId);
	            DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, null,
	            		ruleName, ruleKey, SMARTRULES_INTERNAL_DBG_MSG,
	            		null,  SUGG_REJECTED_DBG_MSG,
                        Constants.PACKAGE, Constants.PACKAGE);
	            
	            finish = setSuggStateAndShowRejectDialog(mRuleId);
	        }     
        }else if (requestCode == RULE_EDIT){
    		if (LOG_INFO) Log.i(TAG,"Edit");      

    		if (resultCode == Activity.RESULT_OK
                       && mSugType == RuleTable.Lifecycle.NEVER_EXPIRES) {
                if (intent == null) {
                    Log.e(TAG, NULL_INTENT);
                } else {
                	if (LOG_DEBUG) Log.d(TAG,"Never Expires");
                    // If returning from PB
                    long newRuleId = intent.getLongExtra(LandingPageIntentExtras.RULE_ID_INSERTED, RuleTable.RuleType.DEFAULT);
                    if(newRuleId == mRuleId) {
                        if(LOG_INFO) Log.i(TAG, "Saving Rule=" + mRuleId);
                        // This is case for a suggestion that is not a sample for example
                        // Low Battery Saver
                        // Enable the accepted suggestion
                        SuggestionsPersistence.acceptSuggestion(mContext, mRuleId);
                        String ruleKey = RulePersistence.getRuleKeyForRuleId(mContext, newRuleId);
                        RulesValidatorInterface.launchModeAd(mContext, ruleKey, 
                                ImportType.IGNORE, 
                                RuleTable.Validity.VALID, 
                                RulePersistence.fetchRuleOnly(mContext, ruleKey),
                                RulePersistence.isRulePsuedoManualOrManual(mContext, ruleKey));

                        
                  /*      // Launch RulesValidator
                        Intent rvIntent =  new Intent(ACTION_RULES_VALIDATE_REQUEST);
                        ArrayList<String> ruleList = new ArrayList<String>();
                        ruleList.add(RulePersistence.getRuleKeyForRuleId(mContext, newRuleId));
                        rvIntent.putExtra(EXTRA_RULE_LIST, ruleList);
                        rvIntent.putExtra(EXTRA_REQUEST_ID, String.valueOf(newRuleId));
                        mContext.sendBroadcast(rvIntent);
*/
                        // Just show landing page
                        LandingPageActivity.startLandingPageActivity(mContext);
                    } else if (newRuleId != RuleTable.RuleType.DEFAULT) {
                    	// This means the suggestion adopted was also a sample rule for
                    	// example Sleep or Meeting. So need to accept the suggestion i.e.
                    	// the parent rule so that it will show back as a  sample and also 
                    	// set the right state for the adopted suggestion i.e. the child rule.                        
                        // Enable the accepted suggestion
                        SuggestionsPersistence.acceptSuggestion(mContext, newRuleId);

                        // remove parent suggestion from inbox
                        SuggestionsPersistence.setSuggestionState(mContext, mRuleId, 
                        							RuleTable.SuggState.ACCEPTED);
                        
                        //Update the adopt count value for the parent rule
                        RulePersistence.setAdoptCount(mContext, mRuleId, ++mAdoptCount);
                        
               /*         // Launch RulesValidator
                        Intent rvIntent =  new Intent(ACTION_RULES_VALIDATE_REQUEST);
                        ArrayList<String> ruleList = new ArrayList<String>();
                        ruleList.add(RulePersistence.getRuleKeyForRuleId(mContext, newRuleId));
                        rvIntent.putExtra(EXTRA_RULE_LIST, ruleList);
                        rvIntent.putExtra(EXTRA_REQUEST_ID, String.valueOf(newRuleId));
                        mContext.sendBroadcast(rvIntent);
*/
                        // show Landing page
                        LandingPageActivity.startLandingPageActivity(mContext);
                    }
                }
            }  else if(resultCode == Activity.RESULT_CANCELED) {
                // In case of Delete Rule from RB mSugType will be INVALID because
                // the rule is already deleted
                if(LOG_INFO) Log.i(TAG, "Cancelled, show rejection dialog =" + mRuleId);
                finish = setSuggStateAndShowRejectDialog(mRuleId);
            }
        }
        
        if(finish) finish();
    }
        
    /**
     * Shows an alert dialog with first rejection message
     *
     * @return - true if dialog is shown
     */
    private boolean showRejectDialog(Context ct) {
    	
    	if (LOG_DEBUG) Log.d(TAG,"showRejectDialog");

        boolean firstReject = false;

        firstReject = Suggestions.getFirstRejectState(ct);

        if (LOG_DEBUG) Log.d(TAG,"From Preference firstReject : " +firstReject);
        
        /*
         * Show Suggestion rejection Dialog if this is a first time rejection
         * of the Suggestion dialog
         */
        if( ! firstReject) return firstReject;

        AlertDialog.Builder builder = new AlertDialog.Builder(ct);

        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.sg_reject);
        builder.setIcon(R.drawable.ic_launcher_smartrules);

        builder.setPositiveButton(R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnCancelListener(this);
        alertDialog.show();

        // lets not show it again. Ever.
        Suggestions.setFirstRejectState(ct, false);

        if (LOG_DEBUG) Log.d(TAG,"End firstReject : " +firstReject);
        
        return firstReject;
    }

    /**
     * This method to set the suggestion state as READ as well as it will
     * helps in showing the rejecting dialog for the first time, as well as
     * it indicates whether the activity needs to be finished or not.
     * 
     * @param ct - Context
     * @return boolean - whether the activity needs to be finished or not
     *  
     */
    private boolean setSuggStateAndShowRejectDialog(long ruleId){
    	
    	//Set the Rule state to Read
	    SuggestionsPersistence.setSuggestionState(mContext, ruleId, 
	    										RuleTable.SuggState.READ);
	    // show the rejection dialog, if the dialog is shown then
	    // the activity need not be finished.
	    return (!showRejectDialog(mContext));
    }

}
