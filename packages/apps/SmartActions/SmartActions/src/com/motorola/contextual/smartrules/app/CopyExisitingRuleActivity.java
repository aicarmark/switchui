/*
 * @(#)CopyExisitingRuleActivity.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/12/09 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.R;

import com.motorola.contextual.smartrules.app.LandingPageActivity.LandingPageIntentExtras;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.uiabstraction.UiAbstractionLayer;
import com.motorola.contextual.smartrules.util.Util;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;

/**This class shows the list of rules that the user can see and are available for copy.
 *
 *<code><pre>
 * CLASS:
 *  extends
 *  	RuleListActivityBase to implement all the list methods
 *
 *  implements
 *  	ViewBinder - for the list view
 *  	OnClickListener - to listen for the on click of a row
 *
 * RESPONSIBILITIES:
 * 	display the list of rules and on user selection make a copy of that rule in the DB and
 *  invoke puzzle builder.
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class CopyExisitingRuleActivity extends RuleListActivityBase implements AsyncTaskQueryHandler, 
																			   ViewBinder {

    private static final String TAG = CopyExisitingRuleActivity.class.getSimpleName();

    private static final int RULE_EDIT = 1;
    
    private String 	  mNewRuleKey = null;
    private ProgressDialog mCopyRuleProgressDialog = null;
    private boolean onClickHandled = false;
    private List<Rule> mRuleList = null;
    private Rule mClickedRowRule = null;
    private List<String> ruleIconList = null;
    
    /** onCreate()
     */
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.copy_existing_rule_list);
        
        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
 		ab.show();
 		
 		if(savedInstanceState != null)
        	onRestoreState(savedInstanceState);
    }

    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        if(LOG_VERBOSE) Log.v(TAG, "In onDestroy()");
        super.onDestroy();
    }
    
    /** onResume()
     */
    @Override
    public void onResume() {
        if(LOG_VERBOSE) Log.v(TAG, "In onResume");
        super.onResume();
        onClickHandled = false;
        if(ruleIconList == null)
        	ruleIconList = Util.getRuleIconsList(mContext, true);
        displayListOfRules();
    }
    
    /** onPause()
     */
    @Override
    public void onPause(){
    	if(LOG_VERBOSE) Log.v(TAG, "in onPause");
    	super.onPause();
        if(mCopyRuleProgressDialog != null) {
        	mCopyRuleProgressDialog.dismiss();
        	mCopyRuleProgressDialog = null;
        }
    }

    /** onOptionsItemSelected()
     *  handles the back press of icon in the ICS action bar.
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    /** Handles the click of items via a physical keyboard in a list row.
     */
	@Override
    protected void onListItemClick(ListView list, View view, int position, long id)
    {
    	if(onClickHandled) {
    		if(LOG_DEBUG) Log.d (TAG, "On Click already handled ignoring " +
    									"further user clicks");
    	} else { 	
    		onClickHandled = true;
    		mCopyRuleProgressDialog = ProgressDialog.show(mContext, "", 
    							mContext.getString(R.string.copying_rule), true);
            mClickedRowRule = (Rule) view.getTag();
            
        	if(LOG_DEBUG) Log.d(TAG, "Handling onClick for "+mClickedRowRule.get_id());
        	startThreadToCloneTheRule(mClickedRowRule);  		
    	}
    }
	
    /**
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	
    	// Setting it back to false here so that the onClicks can be handled again.
    	// Could have been set in onPause but there is a lag from the time the progress
    	// dialog is dismissed in onPause and the rules builder screen is shown. This
    	// gives the user a brief second to click on another row and it will result 
    	// in ANR/screen freeze as seen in the CR IKMAIN-29061
    	onClickHandled = false;
    	
    	if (resultCode == RESULT_CANCELED && intent != null) {
            if (LOG_DEBUG) Log.d(TAG, "User selected the cancel button");
            setResult(RESULT_CANCELED);
        } else if (resultCode == RESULT_OK && intent != null) {
            if (LOG_DEBUG) Log.d(TAG, "User saved the rule to the database - " +
            								"finish the activity");
            long insertedRule = 
            		intent.getLongExtra(LandingPageIntentExtras.RULE_ID_INSERTED,
            								DEFAULT_RULE_ID);
            if(mClickedRowRule != null)
            	DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
            									null, String.valueOf(insertedRule), 
            									mNewRuleKey, SMARTRULES_INTERNAL_DBG_MSG, 
            									String.valueOf(mClickedRowRule.get_id()), 
            									"Copy Of: "+mClickedRowRule.getKey(),
            									PACKAGE, PACKAGE);
            setResult(RESULT_OK);
        }
        finish();
    }

    /** Saves the instance state when an onPause is triggered.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mClickedRowRule != null) {
        	if(LOG_DEBUG) Log.d(TAG, "Saving instance state");
        	outState.putParcelable(RULE_OBJECT, mClickedRowRule);
        	outState.putString(CLONED_CHILD_RULE_KEY, mNewRuleKey);
        }
    }
    
    /** restores the critical variables saved during pause operation.
     */
    protected void onRestoreState(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
        	if(LOG_DEBUG) Log.d(TAG, "Restoring instance state");
        	mClickedRowRule = new Rule();
        	mClickedRowRule = savedInstanceState.getParcelable(RULE_OBJECT);
            mNewRuleKey = savedInstanceState.getString(CLONED_CHILD_RULE_KEY);
        }
    }
    
    /** Starts an async task to fetch the visible rules list and display 
     * 	on the landing page.
     */
    private void displayListOfRules() {        
    	CopyRulesList smartRulesLandingPage = new CopyRulesList(this);
    	smartRulesLandingPage.execute(this);
    }

	/** Async task class to handle the query of the visible rules list for the landing page
	 */
    private class CopyRulesList extends AsyncTask<Context, Integer, List<Rule>> {
        
    	private AsyncTaskQueryHandler qHandler = null;
        
        /** Constructor
         * 
         * @param handler - QueryHandler interface instance
         */
    	CopyRulesList(AsyncTaskQueryHandler handler) {
    		qHandler = handler;
    	}
    	
		@Override
		protected void onPostExecute(List<Rule> list) {
			super.onPostExecute(list);
			if(qHandler != null) 
				qHandler.onVisibleRulesListQueryFinished(list);
		}

		@Override
		protected List<Rule> doInBackground(Context... params) {				
			return new UiAbstractionLayer().fetchLandingPageRulesList(mContext);
		}
    }


	public void onVisibleRulesListQueryFinished(List<Rule> list) {
		if(list == null || list.size() == 0) {
			Log.e(TAG, "rule list is "+(list == null ? "null" : "empty list"));
		} else {
			mRuleList = list;
	        String[] from = {RuleTable.Columns.NAME, RuleTable.Columns.TAGS};
	        int[] to = {R.id.copy_first_line, R.id.copy_mode_icon};
	        
       		List<Map<String, Object>> listMap = getListMap(list);
	        SimpleAdapter listAdapter = new SimpleAdapter(mContext, listMap, 
	        										R.layout.copy_list_row, from, to);
	        listAdapter.setViewBinder(this);
			setListAdapter(listAdapter);
		}
	}


	public Context getContext() {
		return mContext;
	}


	public boolean setViewValue(View view, Object data, String textRepresentation) {
		boolean result = false;
		int id = view.getId();	
		
		switch(id) {	
			case R.id.copy_mode_icon:
				if(view instanceof ImageView) {
					ImageView imageView = (ImageView)  view;
					Rule rule = mRuleList.get((Integer) data);			
					int iconResId = R.drawable.ic_default_w;
		            if(rule != null) {
			            imageView.setImageDrawable(rule.getIconDrawable(this));
		            } else {
                        imageView.setImageDrawable(getResources().getDrawable(iconResId));
		            }
		            
		            LinearLayout parent = (LinearLayout) view.getParent();
		            parent.setTag(rule);
		            result = true;
				}
				break;			
		}		
		return result;
	}
	
    /** iterates through the list of elements and adds them to the hash map.
     * 
     * @param list - list of Rule objects
     * @return - a list map of objects
     */
	private static List<Map<String, Object>> getListMap(List<Rule> list) {
		
		List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>(list.size());
		Iterator<Rule> iterator = list.iterator();
		
		int i = 0;
		while(iterator.hasNext()) {
			Rule rule = iterator.next();
			Map<String, Object> map = new HashMap<String, Object>();
			map.put(RuleTable.Columns.TAGS, i++);
			map.put(RuleTable.Columns.NAME, rule.getName());
			listMap.add(map);
		}		
		return listMap;	
	}  
	
	/** Thread to clone the clicked row rule instance and start rules builder.
	 * 
	 * @param clickedRule - Rule instance of the selected list row 
	 */
	private void startThreadToCloneTheRule(final Rule clickedRule) {
		Thread thread = new Thread() {
			public void run() {
				mNewRuleKey = RulePersistence.createClonedRuleKey(clickedRule.getKey());
	    		String newRuleName = clickedRule.getName() + BLANK_SPC 
						+ getString(R.string.copy);
	    		
	    		Rule rule = clickedRule.clone(mContext);
	    		rule.resetPersistentFields(mNewRuleKey, newRuleName, DEFAULT_RULE_ID, true);
	            Intent intent = rule.fetchRulesBuilderIntent(mContext, true);
	            startActivityForResult(intent, RULE_EDIT);
			}
		};
		thread.setPriority(Thread.NORM_PRIORITY - 1);
		thread.start();
	}
}

/** Interface for the call backs to the main thread from the Async Task.		
 */		
interface AsyncTaskQueryHandler {   		
	void onVisibleRulesListQueryFinished(List<Rule> list);		
	Context getContext(); 		
}