/*
 * @(#)AddRuleListFragment.java
 *
 * (c) COPYRIGHT 2011 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/01/23 NA				  Initial version
 * VHJ384        2012/07/15 NA				  Conversion to Fragment
 *
 */
package com.motorola.contextual.smartrules.app2;

import android.app.Activity;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app2.LandingPageFragment.LandingPageIntentExtras;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.IconTableColumns;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.business.SuggestionsPersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTable.Source;
import com.motorola.contextual.smartrules.db.table.RuleTable.SuggState;
import com.motorola.contextual.smartrules.list.AddRuleList;
import com.motorola.contextual.smartrules.list.ListRow;
import com.motorola.contextual.smartrules.list.ListRowInterface;
import com.motorola.contextual.smartrules.list.RuleListBase;
import com.motorola.contextual.smartrules.service.DumpDbService;
import com.motorola.contextual.smartrules.suggestions.Suggestions;
import com.motorola.contextual.smartrules.uiabstraction.SmartActionsListInterface.MenuType;
import com.motorola.contextual.smartrules.uiabstraction.UiAbstractionLayer;
import com.motorola.contextual.smartrules.widget.SeparatedListAdapter;

/** This class abstracts the list activity and uses the separated list adapter to display
 * different sections from which the user can select to add a rule.
 *
 *<code><pre>
 * CLASS:
 * 	extends ListActivity which provides basic list building, scrolling, etc.
 *
 *  implements
 *  	Constants - for the constants used
 *  	DbSyntax - for the DB related constant strings
 *  	ListRowInterface - for the list row related constants
 *  	SeparatedListAdapter.ViewBinder - interface to use the SeparatedListAdapter
 *
 * RESPONSIBILITIES:
 * 	Show to the user three sections a. Blank Rule b. Suggestions (if present) and
 * 		(c) Samples.
 * 	Process the user selection based on which section is selected.
 *  Provide user with a menu with options.
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class AddRuleListFragment extends ListFragment implements Constants, DbSyntax, 
											ListRowInterface,
											SeparatedListAdapter.ViewBinder,
											BackKeyListener {

	public static final String TAG = AddRuleListFragment.class.getSimpleName();
	
	public static final String NUM_OF_VISIBLE_RULES = PACKAGE + ".numofvisiblerules";
	
	private   Context 				mContext 			= null;
	protected AddRuleList  			mListTopRow 		= null;
	protected AddRuleList			mSamplesList 		= null;
	protected AddRuleList			mSuggestionsList	= null;
	protected SeparatedListAdapter	mListAdapter 		= null;
	protected Activity 				mActivity 			= null;
	protected RuleListBase[] 		mLists	 			= null;
    private   ProgressDialog 		mProgressDialog 	= null;
    private   ListRow 				clickedListRow 		= null;
    private   String 				mNewRuleKey 		= null;
    private   boolean 				onClickHandled 		= false;
    private	  int					mNumOfVisibleRules  = 0;
    private   Delegate		    	mDelegate           = null;
    
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
            	refreshList(mContext);
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
    
    /** Factory method to instantiate Fragment
     *  @param numOfVisibleRules
     *  @return new instance of AddRuleListFragment
     */
    public static AddRuleListFragment newInstance(int numOfVisibleRules) { 
    	AddRuleListFragment f = new AddRuleListFragment();
		Bundle args = new Bundle(); 
		args.putInt(NUM_OF_VISIBLE_RULES, numOfVisibleRules); 
		f.setArguments(args);
		return f; 
	}
	
    @Override
    public void onAttach(Activity activity) {    	
    	super.onAttach(activity);
		mContext = (Context) activity;
		mActivity = activity;
		
		try {
            mDelegate = (Delegate) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement Delegate");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	if (null == savedInstanceState) { 
    		savedInstanceState = getArguments(); 
    	}
    	
    	if (null != savedInstanceState) { 
    		onRestoreState(savedInstanceState);
    	}
    }
    
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		setHasOptionsMenu(true);
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		getActivity().getActionBar().setTitle(R.string.add_rule_title);
	}

	/** onResume()
	 */
	@Override
	public void onResume() {
		super.onResume();
        mContext.getContentResolver().registerContentObserver(Schema.RULE_TABLE_CONTENT_URI, true, myContentObserver);
        handleReturnedResult();
        refreshList(mContext);
	}

	/** onDestroy()
	 */
    @Override
	public void onDestroy() {
		super.onDestroy();
		if(mProgressDialog != null) {
        	mProgressDialog.dismiss();
        	mProgressDialog = null;
        }
	}

    /** onPause()
     */
	@Override
	public void onPause() {
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

	/** required by OnBackKeyListener
	 */
    public void onBackPressed() {
    	closeFragment();
    }
    
	/** Closes Fragment and returns to previous Fragment on the backstack.
	 */
	private void closeFragment() {
    	getFragmentManager().popBackStack();	
    }
	
	/** Handles the returned result from the prior Fragment if it exists.
	 *  This is similar to the onActivityResult method used for Activity based screens.
	 *  
	 *  TODO: Per code review. Convert nested conditional to a switch statement.
	 */
	private void handleReturnedResult() {
		
    	Bundle result = mDelegate.getReturnResult();
    	mDelegate.setReturnResult(null);
    	
		if (result == null)
			return;
		
		int resultCode = result.getInt(LandingPageIntentExtras.RESULT_CODE);
		int requestCode = result.getInt(LandingPageIntentExtras.REQUEST_CODE);
		
		// Setting it back to false here so that the onClicks can be handled again.
    	// Could have been set in onPause but there is a lag from the time the progress
    	// dialog is dismissed in onPause and the rules builder screen is shown. This
    	// gives the user a brief second to click on another row and it will result 
    	// in ANR/screen freeze as seen in the CR IKMAIN-29061
    	onClickHandled = false;
    	
    	if(LOG_DEBUG) Log.d(TAG, "in onActivityResult resultCode = "+resultCode+" requestCode = "+requestCode);

    	if(resultCode == Activity.RESULT_OK) {
    		if(clickedListRow != null) { 

	            if(LOG_DEBUG) Log.d(TAG, "User added a rule - just exit");
    	        if(requestCode == RULE_PRESET || requestCode == RULE_SUGGESTED) {
	        		String data2 = null;
	    			long insertedRuleId = DEFAULT_RULE_ID;
	    			if(result != null)
	    				insertedRuleId = result.getLong(LandingPageIntentExtras.RULE_ID_INSERTED,
	    														DEFAULT_RULE_ID);
	        		int listRowType = (Integer) clickedListRow.get(LIST_ROW_TYPE_KEY);
        			int count = (Integer) clickedListRow.get(RuleTable.Columns.ADOPT_COUNT);
        			long _id = (Long) clickedListRow.get(RuleTable.Columns._ID);

	        		if(listRowType == LIST_ROW_TYPE_SAMPLES) {
	        			data2 = SAMPLE_RULE_ACCEPTED_DBG_MSG;
                        RulePersistence.setAdoptCount(mContext, _id, count + 1);
	        		}
	        		else if(listRowType == LIST_ROW_TYPE_SUGGESTIONS) {
	        			if(insertedRuleId != DEFAULT_RULE_ID) {
	        				if(LOG_DEBUG) Log.d(TAG, "Sample rule was suggested and " +
	        					"accepted - so set the parent "+_id+" to accepted state " +
	        					"and the child "+insertedRuleId+" to accepted and enabled");
	        				// Suggestion is a sample also so just set the suggested state
	        				// to accepted for the parent rule.
		        			SuggestionsPersistence.setSuggestionState(mContext, _id, 
		        							RuleTable.SuggState.ACCEPTED);
	        				// Mark the child rule as accepted too.
	        				SuggestionsPersistence.acceptSuggestion(mContext, 
	        								insertedRuleId);
	        				// Increment the counter value stored in the ADOPT_COUNT column.
	                        RulePersistence.setAdoptCount(mContext, _id, count + 1);
	        			} else if(clickedListRow.get(RuleTable.Columns.SOURCE).equals(Source.SUGGESTED) 
	        						&& insertedRuleId == _id) {
	        					if(LOG_DEBUG) Log.d(TAG, "Suggesions accepted but is not " +
	        							"part of sample rules - so accept and enable " +
	        							"the suggestion "+_id);
	        					// This means this was a suggestion that was not part of the
	        					// sample rules list, so set it to enabled state.
	        					SuggestionsPersistence.acceptSuggestion(mContext, _id);
	        			}
	        			data2 = SUGG_ACCEPTED_DBG_MSG; 
	        		}
	        		
	        		if(data2 != null)
	        			DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
	        						null, String.valueOf(insertedRuleId), 
	        						mNewRuleKey, SMARTRULES_INTERNAL_DBG_MSG, null, data2,
	        						Constants.PACKAGE, Constants.PACKAGE);
	        		
	        		Intent dumpActionConditionServiceIntent = 
	        						new Intent(mContext, DumpDbService.class);
	                dumpActionConditionServiceIntent.putExtra(DumpDbService.SERVICE_TYPE, 
	                				DumpDbService.REGULAR_REQUEST);
	                dumpActionConditionServiceIntent.putExtra(RuleTable.Columns._ID, 
	                				insertedRuleId);
	                mContext.startService(dumpActionConditionServiceIntent);
	        		
		            boolean isManualRule = result.getBoolean(LandingPageIntentExtras.IS_RULE_MANUAL_RULE, true);
		            if(! isManualRule)
		            	RulePersistence.markRuleAsEnabled(mContext, insertedRuleId);
    	        }
        	}
    		closeFragment();
    	}
    	else if(resultCode == Activity.RESULT_CANCELED) {
            refreshList(mContext);
    	}
    }

    /** Creates the options menu when pressing the hard 'menu' key.
     */    
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.add_rule_page_menu, menu);
        // Hide the 'Copy an exiting rule' option in the menu if there are
        // no visible rules in the Landing Page.
        menu.setGroupVisible(R.id.add_rule_copy_menu, this.mNumOfVisibleRules != 0);
    	menu.setGroupVisible(R.id.add_rule_page_options_menu, true);
	}

	/** onOptionsItemSelected()
     *  handles the back press of icon in the ICS action bar.
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = true;
		
		int menuType = MenuType.INVALID;
		
		switch (item.getItemId()) {
			case android.R.id.home:
				closeFragment();
				break;
				
			case R.id.menu_profile:
				menuType = MenuType.MY_PROFILE;
				break;
				
			case R.id.menu_help:
				menuType = MenuType.HELP;
				break;
				
			case R.id.menu_settings:
				menuType = MenuType.SETTINGS;
				break;
				
			case R.id.menu_copy:
				menuType = MenuType.COPY_RULE;
				break;
				
			default:
				result = super.onOptionsItemSelected(item);
		}
		
		if(menuType != MenuType.INVALID) {
			Intent intent = new UiAbstractionLayer().fetchMenuIntent(mContext, menuType);
			if(menuType == MenuType.COPY_RULE) {
				mDelegate.showCopyExistingRule();
			} else {
				if (intent != null) {
					startActivity(intent);
				}
			}
		}
		return result;
	} 

    /** Saves the instance state when an onPause is triggered.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(NUM_OF_VISIBLE_RULES, mNumOfVisibleRules);
	    outState.putString(CLONED_CHILD_RULE_KEY, mNewRuleKey);
    	if(clickedListRow != null) {
	    	outState.putInt(LIST_ROW_TYPE_KEY, (Integer) clickedListRow.get(LIST_ROW_TYPE_KEY));
	    	outState.putLong(RuleTable.Columns._ID, (Long) clickedListRow.get(RuleTable.Columns._ID));
		outState.putInt(RuleTable.Columns.ADOPT_COUNT,
				(Integer) clickedListRow.get(RuleTable.Columns.ADOPT_COUNT));
    	}
    }
    
    /** restores the critical variables saved during pause operation.
     */
    protected void onRestoreState(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
        	if(LOG_DEBUG) Log.d(TAG, "Restoring instance state");
        	mNumOfVisibleRules = savedInstanceState.getInt(NUM_OF_VISIBLE_RULES);
            mNewRuleKey = savedInstanceState.getString(CLONED_CHILD_RULE_KEY);
            clickedListRow = new ListRow();
            clickedListRow.put(LIST_ROW_TYPE_KEY, 
            		savedInstanceState.getInt(LIST_ROW_TYPE_KEY));
            clickedListRow.put(RuleTable.Columns._ID, 
            		savedInstanceState.getLong(RuleTable.Columns._ID));
            clickedListRow.put(RuleTable.Columns.ADOPT_COUNT,
			savedInstanceState.getInt(RuleTable.Columns.ADOPT_COUNT));
        }
    }
        
    /** refreshes the list. 
     * 
     * @param context - context
     */
	protected void refreshList(Context context) {
		
		if (mListAdapter == null) {
			mListAdapter = new SeparatedListAdapter(mActivity);
			mListAdapter.setDelegate(this);
		} else {
			mListAdapter.clear();
		}
			
		refreshBlankRuleRow(context);
		refreshSuggestionsRulesRow(context);
		refreshSampleRulesRow(context);
		
		mListAdapter.addSection(context.getString(R.string.blank_rule_header), mListTopRow.bindListToAdapter(mActivity));

		if(mSuggestionsList != null && !mSuggestionsList.isEmpty()) {
			mListAdapter.addSection(context.getString(R.string.suggestion_header), mSuggestionsList.bindListToAdapter(mActivity));
			
			// Remove notification 
			Suggestions.removeNotification(mContext);

            // User has seen the suggestion list, remove init state
			Suggestions.setInitState(mContext, false);
		}
		
		if(mSamplesList != null && !mSamplesList.isEmpty())
			mListAdapter.addSection(context.getString(R.string.samples_header), mSamplesList.bindListToAdapter(mActivity));
		
		setListAdapter(mListAdapter);  
    	getListView().invalidateViews();
	}
	
	/** refreshes the top row - the blank rule row
	 * 
	 * @param context - context
	 */
	protected void refreshBlankRuleRow(Context context) {
		String whereClause = WHERE + RuleTable.Columns.SOURCE + EQUALS + Q + RuleTable.Source.DEFAULT + Q;
		mListTopRow = AddRuleList.getData(context, whereClause);
		mListTopRow.customizeList(context, LIST_ROW_TYPE_BLANK); 
	}
	
	/** refreshes the Sample rules section of the view
	 * 
	 * @param context - context
	 */
	protected void refreshSampleRulesRow(Context context) {
		
		String whereClause = WHERE + RuleTable.Columns.SOURCE + EQUALS + Q + RuleTable.Source.FACTORY + Q +
								AND + RuleTable.Columns.SUGGESTED_STATE + EQUALS + Q + RuleTable.SuggState.ACCEPTED + Q +
								AND + RuleTable.Columns.FLAGS + EQUALS + Q + RuleTable.Flags.SOURCE_LIST_VISIBLE + Q;
		mSamplesList = AddRuleList.getData(context, whereClause);
		if(mSamplesList != null) 
			mSamplesList.customizeList(context, LIST_ROW_TYPE_SAMPLES);
	}

	/** refreshes the Suggestions rules section of the view
	 * 
	 * @param context - context
	 */
	protected void refreshSuggestionsRulesRow(Context context) {
		
		String whereClause = WHERE + RuleTable.Columns.SUGGESTED_STATE + NOT_EQUAL + Q
								+ RuleTable.SuggState.ACCEPTED + Q;
		mSuggestionsList = AddRuleList.getData(context, whereClause);
		if(mSuggestionsList != null)
			mSuggestionsList.customizeList(context, LIST_ROW_TYPE_SUGGESTIONS);
	}
	
    /** Sets the view values for each element of the row.
     */
	public void setViewValue(int position, View view, final Object data) {
				
		ListRow listRow = (ListRow) data;	
		int listRowType = (Integer) listRow.get(LIST_ROW_TYPE_KEY);
		
		LinearLayout displayRulesWrapper = (LinearLayout) view.findViewById(R.id.display_rules_row_wrapper);
		if(displayRulesWrapper != null) {
			displayRulesWrapper.setTag(listRow);
		}
		
		ImageView ruleIcon = (ImageView) view.findViewById(R.id.display_rules_mode_icon);
		LinearLayout addedWrapper = (LinearLayout) view.findViewById(R.id.added_wrapper);
		TextView addedLine = (TextView) view.findViewById(R.id.added_line);
		
		int showAddedTextResId = -1;
		int textColor = -1;
		int ruleIconResId = R.drawable.ic_default_w;
		String ruleIconString = (String) listRow.get(RuleTable.Columns.ICON);
		if(ruleIconString != null)
			ruleIconResId = getResources().getIdentifier(ruleIconString, "drawable", getActivity().getPackageName());
		
		switch (listRowType) {
			case LIST_ROW_TYPE_BLANK:
				TextView firstLine = (TextView) view.findViewById(R.id.display_rules_first_line);
				firstLine.setText(R.string.blank_rule);
				TextView secondLine = (TextView) view.findViewById(R.id.display_rules_second_line);
				secondLine.setText(R.string.blank_rule_subtext);	
				ruleIconResId = R.drawable.ic_default_w;
				break;
			
			case LIST_ROW_TYPE_SAMPLES:
				int sampleCount = (Integer) listRow.get(RulePersistence.SAMPLE_RULE_ADOPTED_COUNT);
				if(sampleCount > 0) {
					showAddedTextResId = R.string.added;
					textColor = mContext.getResources().getColor(R.color.active_blue);
				}
				break;
			
			case LIST_ROW_TYPE_SUGGESTIONS:
				int suggState = (Integer) listRow.get(RuleTable.Columns.SUGGESTED_STATE);
				if(suggState == RuleTable.SuggState.UNREAD) {
					showAddedTextResId = R.string.new_string;
					textColor = mContext.getResources().getColor(R.color.suggestion_green);
				}
				break;
		}
		
		if(ruleIcon != null) {
			ruleIcon.setVisibility(View.VISIBLE);
			if(listRowType == LIST_ROW_TYPE_BLANK)
				ruleIcon.setImageDrawable(getResources().getDrawable(ruleIconResId));
			else {
			    Drawable iconDrawable = IconPersistence.getIconDrawableFromBlob(mContext,
					(byte[]) listRow.get(IconTableColumns.ICON));
				ruleIcon.setImageDrawable(iconDrawable);
			}
			ruleIcon.setAlpha(EIGHTY_PERCENT_ALPHA_VALUE);
		}
			
		if(showAddedTextResId == -1) {
			addedWrapper.setVisibility(View.GONE);
			addedLine.setVisibility(View.GONE);
		} else {
			addedWrapper.setVisibility(View.VISIBLE);
			addedLine.setVisibility(View.VISIBLE);
			addedLine.setText(mContext.getString(showAddedTextResId));
			if(textColor != -1)
				addedLine.setTextColor(textColor);
		}		
	}

	/** Handles the click of items in a list row.
     */
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		if(onClickHandled) {
			Log.e(TAG, "Click for the list has been handled - ignoring further clicks");
		} else {
			clickedListRow = (ListRow) view.getTag();
			if(clickedListRow != null) {
	    		mProgressDialog = ProgressDialog.show(mContext, "", 
						mContext.getString(R.string.copying_rule), true);
				startThreadToStartRulesBuilder(clickedListRow);
			}
		}
	}
	
	/** Thread to fetch the rule instance for the selected rule and launch rules builder.
	 * 
	 * @param listRow - List row instance of the selected list object.
	 * 
	 * TODO: Per code review. Re-factor and possibly simplify conditional.
	 */
	private void startThreadToStartRulesBuilder(final ListRow listRow) {
		Thread thread = new Thread() {
			public void run() {
				onClickHandled = true;
				int listRowType = (Integer) clickedListRow.get(LIST_ROW_TYPE_KEY);
				int ruleSource = (Integer) clickedListRow.get(RuleTable.Columns.SOURCE);
    			long _id = (Long) clickedListRow.get(RuleTable.Columns._ID);
				Rule rule = new Rule();
				int requestCode = 0;
				
				if(listRowType == LIST_ROW_TYPE_BLANK) {
					if(LOG_DEBUG) Log.d(TAG, "Blank Rule row selected - start rules builder for blank rule");
					requestCode = RULE_CREATE;
				} else {
	    			int suggState = (Integer) clickedListRow.get(RuleTable.Columns.SUGGESTED_STATE);	
					if(suggState == SuggState.UNREAD )
						SuggestionsPersistence.setSuggestionState(mContext, _id, 
												RuleTable.SuggState.READ);
					
					if(listRowType == LIST_ROW_TYPE_SAMPLES || 
							(listRowType == LIST_ROW_TYPE_SUGGESTIONS 
									&& ruleSource == Source.FACTORY)) {
						requestCode = RULE_PRESET;
						String ruleKey = (String) clickedListRow.get(RuleTable.Columns.KEY);
						if(LOG_DEBUG) Log.d(TAG, "Samples/Suggestion (Factory Source) " +
								"row clicked for rule key "+ruleKey);
			    		
			    		String name = (String) clickedListRow.get(RuleTable.Columns.NAME);
		    			int count = (Integer) clickedListRow.get(RuleTable.Columns.ADOPT_COUNT);

			    		String newRuleName = RulePersistence.createClonedRuleName(name, count);
		                if(ruleKey !=  null) 
					mNewRuleKey = RulePersistence.createClonedRuleKeyForSample(ruleKey);
		                
		                rule = RulePersistence.fetchFullRule(mContext, _id);
		                if(rule != null)
		                	rule.resetPersistentFields(mNewRuleKey, newRuleName, 0, false);
					} else if (listRowType == LIST_ROW_TYPE_SUGGESTIONS 
									&& ruleSource == Source.SUGGESTED) {
						if(LOG_DEBUG) Log.d(TAG, "Suggestion clicked and source " +
								"is Suggestion - so no clone");
						requestCode = RULE_SUGGESTED;
		    			rule = RulePersistence.fetchFullRule(mContext, _id);
					}
				}

				if(rule == null) {
					Log.e(TAG, "Fetching full rule for "+_id+" failed");
					onClickHandled = false;
					if(mProgressDialog != null) {
						mProgressDialog.dismiss();
						mProgressDialog = null;
					}
				} else {
					Intent intent = rule.fetchRulesBuilderIntent(mContext, false);
					startFragmentForResult(intent, requestCode);
				}
			}
		};
		thread.setPriority(Thread.NORM_PRIORITY - 1);
		thread.start();
	}
	
	/**
	 * Launches Rule Editor on UI thread
	 * @param intent
	 * @param requestCode
	 */
	private void startFragmentForResult(final Intent intent, final int requestCode) {
		getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mDelegate.showRuleEditorForRule(intent.getExtras(), requestCode, false);
            }
    	});
	}
    
    /** Container Activity must implement this interface
     */
    public interface Delegate {
        public void showRuleEditorForRule(Bundle extras, int requestCode, boolean useVerticalTransition);
        public void showCopyExistingRule();
        public void setReturnResult(Bundle result);
        public Bundle getReturnResult();
    }
}
