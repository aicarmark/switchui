/*
 * @(#)DisplayRulesActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA MOBILITY INC.
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
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app.LandingPageActivity.LandingPageIntentExtras;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.rulesbuilder.EditRuleActivity;
import com.motorola.contextual.smartrules.service.DumpDbService;
import com.motorola.contextual.smartrules.suggestions.Suggestions;
import com.motorola.contextual.smartrules.uiabstraction.UiAbstractionLayer;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;

/** This class abstracts the list activity and is common activity that can be invoked to
 * 	display the Suggested or Preset rules to the user.
 *
 *
 *<code><pre>
 * CLASS:
 * 	extends RuleListActivityBase which provides basic list building, scrolling, etc.
 *
 * 	implements
 * 		ViewBinder - for the set view
 * 		OnClickListener - to handle the on click events
 *
 * RESPONSIBILITIES:
 * 	displays the list of suggested or preset rules and on user selection launches
 *  puzzle builder to display that rule.
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class DisplayRulesActivity extends RuleListActivityBase implements ViewBinder {

    private static final String TAG = DisplayRulesActivity.class.getSimpleName();

    public static final String DISPLAY_TITILE = PACKAGE + ".displaytitle";
    public static final String CHILD_RULE_ID = PACKAGE + ".childruleid";
    
    private String displayTitle = null;
    private long   mNewRuleId = -1;
    private String mNewRuleKey = null;
    private ProgressDialog mProgressDialog = null;
    private boolean onClickHandled = false;
    private List<Rule> mRuleList = null;
    protected Rule mClickedRowRule = null;

    /** Holds message types for handler.
     */
    private static interface HandlerMessage {
        int REFRESH_LIST = 0;
    }
    
    /** Handler for all messages, see message types below.
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            if (LOG_DEBUG) Log.d(TAG, ".handleMessage - msg="+msg.what);

            switch (msg.what) {

            case HandlerMessage.REFRESH_LIST:
            	showRulesList();
                break;
            }
        }
    };
    
    /** content observer to listen to database updates via notifyChange() call.
     */
    private static class MyContentObserver extends ContentObserver {

        Handler localHandler = null;
        public MyContentObserver(Handler handler) {
            super(handler);
            localHandler = handler;
        }

        @Override
        public void onChange(boolean selfChange) {
            if(LOG_DEBUG) Log.d(TAG, "MyContentObserver: OnChange Called "+localHandler);
            super.onChange(selfChange);
            if(localHandler != null) {
            	if(localHandler.hasMessages(HandlerMessage.REFRESH_LIST)) {
	                localHandler.removeMessages(HandlerMessage.REFRESH_LIST);
            	}
                localHandler.sendEmptyMessageDelayed(HandlerMessage.REFRESH_LIST, 2000);
            }
        }
    }
    private MyContentObserver myContentObserver = new MyContentObserver(mHandler);
    
    /** onCreate()
     */
    public void onCreate(Bundle savedInstanceState) {

        if(LOG_VERBOSE) Log.v(TAG, "In onCreate()");
        super.onCreate(savedInstanceState);

        if(savedInstanceState != null)
        	onRestoreState(savedInstanceState);
        else {
	        Intent intent = getIntent();
	        Bundle bundle = null;
	        if(intent != null){
	            bundle = intent.getExtras();
	        }
	
	        if(bundle != null){
	            displayTitle = bundle.getString(DISPLAY_TITILE);
	            Log.d(TAG, "displayTitle = "+displayTitle);
	        } else {
	            finish();
	            return;
	        }
        }
        
        if(displayTitle == null)
        	finish();
        
        setContentView(R.layout.display_rules_list);
        ActionBar ab = getActionBar();
        ab.setTitle(displayTitle);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
 		ab.show();
    }

    /** onResume()
     */
    @Override
    public void onResume() {
    	if(LOG_VERBOSE) Log.v(TAG, "In onResume");
    	super.onResume();
        mContext.getContentResolver().registerContentObserver(
        		Schema.RULE_TABLE_CONTENT_URI, true, myContentObserver);
        showRulesList();
    }
    
    /** onPause()
     */
    @Override
    public void onPause() {
    	if(LOG_VERBOSE) Log.v(TAG, "In onPause");
    	super.onPause();
    	if(myContentObserver != null)
    		mContext.getContentResolver().unregisterContentObserver(myContentObserver);
    	if(mHandler.hasMessages(HandlerMessage.REFRESH_LIST)) {
    		mHandler.removeMessages(HandlerMessage.REFRESH_LIST);
    	}
        if(mProgressDialog != null) {
        	mProgressDialog.dismiss();
        	mProgressDialog = null;
        }
    }
    
    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        if(LOG_VERBOSE) Log.v(TAG, "In onDestroy()");
        super.onDestroy();
    }

    /** Saves the instance state when an onPause is triggered.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mClickedRowRule != null) {
        	if(LOG_DEBUG) Log.d(TAG, "Saving instance state");
        	outState.putParcelable(RULE_OBJECT, mClickedRowRule);
        	outState.putLong(CHILD_RULE_ID, mNewRuleId);    
        	outState.putString(DISPLAY_TITILE, displayTitle);
        }
    }
    
    /** restores the critical variables saved during pause operation.
     */
    protected void onRestoreState(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
        	if(LOG_DEBUG) Log.d(TAG, "Restoring instance state");
        	mClickedRowRule = new Rule();
        	mClickedRowRule = savedInstanceState.getParcelable(RULE_OBJECT);
            mNewRuleId = savedInstanceState.getLong(CHILD_RULE_ID);
            displayTitle = savedInstanceState.getString(DISPLAY_TITILE);
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
    	
    	if(LOG_DEBUG) Log.d(TAG, "in onActivityResult resultCode = "+resultCode+
    									" requestCode = "+requestCode);
        if(requestCode == RULE_PRESET) {
        	if(resultCode == RESULT_OK) {
	            if(LOG_DEBUG) Log.d(TAG, "preset rules displayed from add rule " +
	            		"and user accepted - just exit");
	            DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, null, 
	            								String.valueOf(mNewRuleId),  mNewRuleKey, 
	            								SMARTRULES_INTERNAL_DBG_MSG, 
	            								null, SAMPLE_RULE_ACCEPTED_DBG_MSG,
	            								Constants.PACKAGE, Constants.PACKAGE);
	            Intent dumpActionConditionServiceIntent = new Intent(mContext, 
	            												DumpDbService.class);
	            dumpActionConditionServiceIntent.putExtra(DumpDbService.SERVICE_TYPE, 
	            				DumpDbService.REGULAR_REQUEST);
	            dumpActionConditionServiceIntent.putExtra(RuleTable.Columns._ID, mNewRuleId);
	            mContext.startService(dumpActionConditionServiceIntent);
	            boolean isManualRule = intent.getBooleanExtra(
	            			LandingPageIntentExtras.IS_RULE_MANUAL_RULE, true);
	            if(! isManualRule)
	            	RulePersistence.markRuleAsEnabled(mContext, mNewRuleId);
	            finish();
        	}
        	else {
        		if(LOG_DEBUG) Log.d(TAG, "User cancelled - so delete the " +
        				"new rule from the DB "+mNewRuleId);
        		
				if (mClickedRowRule != null) {
					new Thread(new Runnable() {

						public void run() {
							// use the older value which is stored in the row
							// tag.
							RulePersistence.deleteChildRule(mContext, mNewRuleId, mClickedRowRule.get_id(), mClickedRowRule.getCachedAdoptedCount());
							mHandler.sendEmptyMessage(HandlerMessage.REFRESH_LIST);
						}
					}).start();
				} else {
					showRulesList();
				}
        	}
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
	
   

    /** displays the suggested or preset rules based on the whereClause
     */
    protected void showRulesList() {
        if(LOG_DEBUG) Log.d(TAG, "In showRulesList");
        
        List<Rule> ruleList = null;
    	if(displayTitle != null) {
	        if(displayTitle.equals(getString(R.string.preset_rules))) {
	        	ruleList = new UiAbstractionLayer().fetchSamplesList(mContext);
	        }
	        else if(displayTitle.equals(getString(R.string.suggestions))) {
	        	ruleList = new UiAbstractionLayer().fetchSuggestionsList(mContext, 0);
	        }
        
        	if(ruleList == null || ruleList.size() == 0) {
            	if(displayTitle.equals(getString(R.string.preset_rules)))
                   showErrorMessage(getString(R.string.no_samples));
            	else if(displayTitle.equals(getString(R.string.suggestions)))
            	   showErrorMessage(getString(R.string.no_suggestions));     
        	} else {
        		mRuleList = ruleList;
        		hideErrorMessage();
        		String[] from = {RuleTable.Columns.TAGS, 
        						 RuleTable.Columns.TAGS, 
        						 RuleTable.Columns.DESC};
                int[] to = {R.id.display_rules_first_line, 
                		    R.id.display_rules_mode_icon, 
                		    R.id.display_rules_second_line};

                List<Map<String, Object>> listMap = getListMap(ruleList);
    	        SimpleAdapter listAdapter = new SimpleAdapter(mContext, listMap, 
    	        							R.layout.display_rules_list_row, from, to);
    	        listAdapter.setViewBinder(this);
    			setListAdapter(listAdapter);                
        	}
        }
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
			map.put(RuleTable.Columns.NAME, rule.getName());
			map.put(RuleTable.Columns.TAGS, i++);
			map.put(RuleTable.Columns.DESC, rule.getDesc());
			listMap.add(map);
		}		
		return listMap;	
	}

    /** Displays an error message if the rule list was null 
     * 	or empty (returned from Smart Rules DB provider)
     *
     * @param errorMsg - Message to be displayed to the user.
     */
    private void showErrorMessage(String errorMsg) {
        RelativeLayout errRl  	= null;
        TextView errTextView  = (TextView) findViewById(R.id.failmessage_text);
        if(errTextView != null) {
            errRl = (RelativeLayout) errTextView.getParent();
            errRl.setVisibility(View.VISIBLE);
            errTextView.setText(errorMsg);
        }
    }

    /** Hides the Error Mesg Layout
     *
     */
    private void hideErrorMessage() {
    	RelativeLayout errRl    = null;
    	TextView errTextView  = (TextView) findViewById(R.id.failmessage_text);
    	if(errTextView != null) {
    		errRl = (RelativeLayout) errTextView.getParent();
    		errRl.setVisibility(View.GONE);
    	}
   	}


	public boolean setViewValue(View view, Object data, String textRepresentation) {
		boolean result = false;
		int id = view.getId();	
		Rule rule = null;
		
		switch(id) {
		
			case R.id.display_rules_first_line:
				if(view instanceof TextView) {
					rule = mRuleList.get((Integer) data);
					((TextView) view).setText(rule.getName());
					result = true;
				}
				break;
				
			case R.id.display_rules_mode_icon:
				if(view instanceof ImageView) {
					ImageView imageView = (ImageView) view;
					rule = mRuleList.get((Integer) data);
					int iconResId = R.drawable.ic_default_w;
					String ruleIconString = rule.getIcon();
		            if(ruleIconString != null){
			            imageView.setImageDrawable(rule.getIconDrawable(this));
		            } else {
                        imageView.setImageDrawable(getResources().getDrawable(iconResId));
		            }
		            imageView.setAlpha(EIGHTY_PERCENT_ALPHA_VALUE);
		            imageView.setVisibility(View.VISIBLE);
		            
		    		LinearLayout addedWrapper = 
		    				(LinearLayout) ((LinearLayout) view.getParent().getParent()).findViewById(R.id.added_wrapper);
	        		addedWrapper.setVisibility(rule.getCachedAdoptedCount() > 0 
	        										? View.VISIBLE : View.GONE);						
						        	
		            ((View)view.getParent().getParent()).setTag(rule);
		            result = true;
				}
				break;
		}		
		return result;
	}
    
    /** processes the selection of a list item.
     */
    private void handleOnClickOfListItem() {
        if(LOG_DEBUG) Log.d(TAG, "in onClick handling the " +
        		"click for rule ID "+mClickedRowRule.get_id());

        if(displayTitle.equals(mContext.getString(R.string.preset_rules))) {
    		mProgressDialog = ProgressDialog.show(mContext, "", 
    								mContext.getString(R.string.copying_rule), true);
            startThreadToCloneTheRule();
        }
        else if(displayTitle.equals(mContext.getString(R.string.suggestions))) {
            Suggestions.showSuggestionDialog(mContext, mClickedRowRule.get_id());
        }
        else { 
        	Log.d(TAG, "Neither sample nor suggestion");
            Intent intent = new Intent(mContext, EditRuleActivity.class);
        	intent.putExtra(PUZZLE_BUILDER_RULE_ID, mClickedRowRule.get_id());
            startActivityForResult(intent, RULE_SUGGESTED);
        }
    }
    
    /** Thread to process the copy of the selected rule to a new rule and 
     *  launch puzzle builder.
     */
    private void startThreadToCloneTheRule() {
    	Thread thread = new Thread() {
    		public void run() {
                String newRuleName = 
                		RulePersistence.createClonedRuleName(mClickedRowRule.getName(), 
                								mClickedRowRule.getCachedAdoptedCount());
                mNewRuleKey = RulePersistence.createClonedRuleKey(mClickedRowRule.getKey());

	        	mNewRuleId = RulePersistence.cloneRule(mContext, mClickedRowRule.get_id(), 
	        								mNewRuleKey, newRuleName, false);  

            	if(mNewRuleId == DEFAULT_RULE_ID) {
            		if(LOG_DEBUG) Log.d(TAG, "Cloning failed for " +
            				"sample rule "+mClickedRowRule.get_id());
            		if(mProgressDialog != null) {
            			mProgressDialog.dismiss();
            			mProgressDialog = null;
                    }
                    showRulesList();
            	} else {
            		if(LOG_DEBUG) Log.d(TAG, "Launching rules builder for " +
            				"the cloned rule "+mNewRuleId);
            		// Increment the counter value stored in the 
            		// SAMPLE_FKEY_OR_COUNT column.
                    RulePersistence.setAdoptCount(mContext, mClickedRowRule.get_id(), 
                    				mClickedRowRule.getCachedAdoptedCount() + 1);

                    runOnUiThread(new Runnable() {
                        public void run() {
                            // Launch Puzzle Builder
                        	if (LOG_DEBUG) Log.d(TAG, "inserted ruleId is "+mNewRuleId);
                            Intent intent = new Intent(mContext, EditRuleActivity.class);
                        	intent.putExtra(PUZZLE_BUILDER_RULE_ID, mNewRuleId);
        	            	intent.putExtra(PUZZLE_BUILDER_RULE_COPY, true);
                            startActivityForResult(intent, RULE_PRESET);
                        }
                	});
                } 
    		}
    	};
    	thread.setPriority(Thread.NORM_PRIORITY - 1);
    	thread.start();
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
    		mClickedRowRule = (Rule) view.getTag();
            handleOnClickOfListItem();
    	}
    }
}
