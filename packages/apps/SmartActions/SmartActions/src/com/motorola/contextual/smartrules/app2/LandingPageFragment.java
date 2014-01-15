/*
 * @(#)LandingPageFragment.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/12/09 NA				  Initial version
 * VHJ384        2012/07/15 NA				  Conversion to Fragment
 *
 */
package com.motorola.contextual.smartrules.app2;

import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app2.dialog.DeleteRuleDialogFragment;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.IconTableColumns;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.db.table.RuleTable.RuleType;
import com.motorola.contextual.smartrules.list.AddRuleList;
import com.motorola.contextual.smartrules.list.ListRow;
import com.motorola.contextual.smartrules.list.ListRowInterface;
import com.motorola.contextual.smartrules.rulesbuilder.LocationConsent;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants;
import com.motorola.contextual.smartrules.service.SmartRulesService;
import com.motorola.contextual.smartrules.suggestions.Suggestions;
import com.motorola.contextual.smartrules.uiabstraction.SmartActionsListInterface.MenuType;
import com.motorola.contextual.smartrules.uiabstraction.UiAbstractionLayer;
import com.motorola.contextual.smartrules.util.Util;
import com.motorola.contextual.smartrules.widget.DialogUtil;
import com.motorola.contextual.smartrules.widget.SeparatedListAdapter;

/** This class abstracts the list activity and displays the landing page to the user.
 *
 *<code><pre>
 * CLASS:
 * 	extends RuleListActivityBase which provides basic list building, scrolling, etc.
 *
 *  implements
 *  	SeparatedListAdapter.ViewBinder - for the set view value
 *  	ListRowInterface
 *
 * RESPONSIBILITIES:
 * 	Show to the user the list of available rules and also indicate their status 
 * 	(active, inactive, disabled)
 *  Display the number of suggested rules
 *  Allow the user to add a new rule from scratch, from preset or copy existing rule.
 *  Provide user with a menu with options.
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class LandingPageFragment extends ListFragment implements SeparatedListAdapter.ViewBinder,
																 ListRowInterface,
																 Constants, 
																 DbSyntax, 
																 BackKeyListener,
																 DeleteRuleDialogFragment.DialogListener {

    public static final String TAG = LandingPageFragment.class.getSimpleName();
    
    //private static final int DIALOG_DELETE_RULE = 1;
    
    /** interface for menu options shown on long click
     */
    private interface MenuOptions {
    	final int VIEW_RULE = 0;
    	final int DELETE_RULE = VIEW_RULE + 1;
    }

    /** Holds message types for handler.
     */
    private static interface HandlerMessage {
        int REFRESH_LIST = 0;
        int DISABLE_ALL_REFRESH = REFRESH_LIST + 1;
    }

    /** interface to use for Intent extras
     */
    public interface LandingPageIntentExtras {
    	final String RULE_ID_INSERTED = PACKAGE + ".ruleinserted";
    	final String RULE_ICON_CHANGED = PACKAGE + ".ruleiconchanged";
    	final String IS_RULE_MANUAL_RULE = PACKAGE + ".isrulemanualrule";
		final String RESULT_CODE = PACKAGE + ".resultcode";
		final String REQUEST_CODE = PACKAGE + ".requestcode";
    }
    
    private BroadcastReceiver rulesServiceReceiver = null;
    private ProgressDialog mDeleteRuleProgressDialog = null;
    private ProgressDialog mDisableAllProgressDialog = null;
    private boolean isLocationErrorNeedsToBeShown = false;
    private int numOfVisibleRules = 0;
    private Menu mMenu = null;
	private SeparatedListAdapter	mListAdapter 		= null;
	private AddRuleList				mManualList			= null;
	private AddRuleList				mAutoList			= null;
    private ListRow 				clickedListRow 		= null;
    private List<String>			ruleIconList		= null;
    private Delegate				mDelegate			= null;
    private Context					mContext			= null;
    
    /** Handler for all messages, see message types below.
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            if (LOG_DEBUG) Log.d(TAG, ".handleMessage - msg="+msg.what);

            switch (msg.what) {

            case HandlerMessage.DISABLE_ALL_REFRESH:
            	if(mHandler.hasMessages(HandlerMessage.REFRESH_LIST)) {
                    mHandler.removeMessages(HandlerMessage.REFRESH_LIST);
            	}
            	//Unregister the receiver since it is no longer needed
            	if(rulesServiceReceiver != null) {
	            	mContext.unregisterReceiver(rulesServiceReceiver);
	            	rulesServiceReceiver = null;
            	}
            	if(mDisableAllProgressDialog != null) {
            		mDisableAllProgressDialog.dismiss();
    	        	mDisableAllProgressDialog = null;
            	}
                Toast.makeText(mContext, R.string.rules_disabled, Toast.LENGTH_LONG).show();
                refreshRulesList();
    	        break;
    	        
            case HandlerMessage.REFRESH_LIST:            	
            	if(mDeleteRuleProgressDialog != null) {                	
                	if(mDeleteRuleProgressDialog.isShowing())
                		mDeleteRuleProgressDialog.dismiss();
            		mDeleteRuleProgressDialog = null;
                    Toast.makeText(mContext, R.string.rule_deleted, Toast.LENGTH_LONG).show();
            	}
            	
            	refreshRulesList();
                break;
            }
        }
    };

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
                localHandler.sendEmptyMessageDelayed(HandlerMessage.REFRESH_LIST, 2500);
            }
        }
    }
    private MyContentObserver myContentObserver = new MyContentObserver(mHandler);

    public static LandingPageFragment newInstance() {
		LandingPageFragment f = new LandingPageFragment();
		Bundle args = new Bundle();
		f.setArguments(args);
		return f;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mContext = (Context) activity;

		try {
			mDelegate = (Delegate) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement Delegate");
		}
	}
	
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
    	super.onCreateView(inflater, container, savedInstanceState);
		return setViewLayoutParameters(inflater, container);
	}
    
    /** This is a method to start Landing Page Activity
     * 
     * @param context - context
     */
    public static void startLandingPageActivity(Context context){
        //Intent intent = new Intent(context, LandingPageFragment.class);
        //context.startActivity(intent);
    }
    
    /** To launch the Landing page, and clear the task stack.
     * 	For now one one flag supported.
     * 	In case more flags need to added, consider passing flag as other param
     * 	@param context - context
     */
    public static void startLandingPageActivityWithClearTask(Context context){
	     //Intent intent = new Intent(context, LandingPageFragment.class);
	     //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
	     //context.startActivity(intent);
    }
    
    /** onCreate()
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        if(LOG_VERBOSE) Log.v(TAG, "in onCreate()");
        super.onActivityCreated(savedInstanceState);
		//mActivity = this;

        ActionBar actionBar = getActivity().getActionBar();
		actionBar.setHomeButtonEnabled(false);
		actionBar.setDisplayHomeAsUpEnabled(false);
		actionBar.setTitle(getString(R.string.app_name));
		
		setHasOptionsMenu(true);

        // cjd - don't uncomment this.
        //new RuleTable().initialize(mContext);
        // cjd - this must be commented out
        //if (Test.RUN) Test.start(this);
     		
        if(savedInstanceState != null)
            onRestoreState(savedInstanceState);
        
        new ActionTable().populateModalityColumn(mContext);
        
        // Commenting the following out so that rules importer is not launched twice.
        // Load Sample Rules in case of data clear
        //launchConditionBuilderDuringDataClear(mContext);
        
        //Launch BlurRulesUpdater to download New Rules from the BEST server
        if (LOG_DEBUG) Log.d(TAG,"Launch Rules Updater from Landing page Activity");
    	Intent intent1 = new Intent(LAUNCH_RULES_UPDATER);
        intent1.putExtra(IMPORT_TYPE, XmlConstants.ImportType.SERVER_RULES);
        intent1.putExtra(EXTRA_IS_DOWNLOAD, true);
        mContext.sendBroadcast(intent1);
    }

    /** onConfigurationChanged()
     *  Need to set the layout, title and refresh the list as the configuration
     *  changed from portrait to landscape and vice-versa. Need to explicitly
     *  do this here for Landing Page as the activity has the parameter 
     *  android:configChanges="orientation|keyboardHidden" set in the manifest
     *  to handle configuration changes for dialogs.
     * 
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	if(LOG_VERBOSE) Log.v(TAG, "in onConfigurationChanged");
    	super.onConfigurationChanged(newConfig);
    	refreshRulesList();
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
        if(ruleIconList == null)
        	ruleIconList = Util.getRuleIconsList(mContext, true);
        registerBroadcastListeners();
        handleReturnedResult();
        refreshRulesList();
    }

    /** onPause()
     */
    @Override
    public void onPause() {
        if(LOG_VERBOSE) Log.v(TAG, "In onPause");
        super.onPause();
        unregisterBroadcastListeners();
        dismissProgressDialogsShown();
    }

	/** required by OnBackKeyListener
	 */
    public void onBackPressed() {
    	getActivity().finish();
    }
    
    /**
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if(intent == null) {
        	Log.e(TAG, "onActivityResult returned with a null intent");
        } else {
	        // if returned from RulesBuilder, we check if we updated a suggestion
	        if(requestCode == RULE_EDIT && resultCode == Activity.RESULT_OK){
	            long id = intent.getLongExtra(LandingPageIntentExtras.RULE_ID_INSERTED, RuleTable.RuleType.DEFAULT);	            
	            // Verify if the rule updated was of suggestion type
	            if(id != RuleType.DEFAULT)
	                Suggestions.verifyAndAcceptSuggestion(mContext, id);
	        }
        }
    }
    
    private void handleReturnedResult() {
		Bundle result = mDelegate.getReturnResult();
		mDelegate.setReturnResult(null);
		
		if (result != null) {
			
			int requestCode = result.getInt(LandingPageIntentExtras.REQUEST_CODE);
			int resultCode = result.getInt(LandingPageIntentExtras.RESULT_CODE);
			
			// if returned from RulesBuilder check for updated suggestion
			if (requestCode == RULE_EDIT && resultCode == Activity.RESULT_OK) {

				long id = result.getLong(LandingPageIntentExtras.RULE_ID_INSERTED, RuleTable.RuleType.DEFAULT);
				// Verify if the rule updated was of suggestion type
				if (id != RuleType.DEFAULT) {
					Suggestions.verifyAndAcceptSuggestion(mContext, id);
				}
			}
		}
	}

    /** Saves the instance state when an onPause is triggered.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(clickedListRow != null) {
        	outState.putLong(RuleTable.Columns._ID, 
        						(Long) clickedListRow.get(RuleTable.Columns._ID));
            outState.putString(RuleTable.Columns.NAME, 
            					(String) clickedListRow.get(RuleTable.Columns.NAME));
            outState.putString(RuleTable.Columns.KEY, 
            					(String) clickedListRow.get(RuleTable.Columns.KEY));
            outState.putInt(RuleTable.Columns.ACTIVE, 
            					(Integer) clickedListRow.get(RuleTable.Columns.ACTIVE));
            outState.putInt(RuleTable.Columns.ENABLED, 
            					(Integer) clickedListRow.get(RuleTable.Columns.ENABLED));
            outState.putInt(RuleTable.Columns.RULE_TYPE, 
            					(Integer) clickedListRow.get(RuleTable.Columns.RULE_TYPE));
        }
    }

    /** Creates the options menu when pressing the hard 'menu' key.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.landing_page_menu, menu);
        mMenu = menu;
        setMenuGroupsVisibility(menu);
        
        inflater.inflate(R.menu.edit_menu, menu);
		setMenuItemsVisibility(menu);
    }

    /** Sets the visibility of the add button in the action bar
     *  @param menu
     */
    private void setMenuItemsVisibility(Menu menu) {
		menu.findItem(R.id.edit_add_button).setVisible(true);
	}
    
	@Override
	public void onPrepareOptionsMenu(Menu menu) {        
		setMenuGroupsVisibility(menu);
	}
	
	/** Handles the selection of the menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
    	boolean result = true;  	
        int resId = -1;        
        int menuType = MenuType.INVALID;
        
        switch (item.getItemId()) {
        
	        case R.id.menu_check_status:
	        	resId = R.string.check_status;
	        	menuType = MenuType.CHECK_STATUS;
	        	break;
	        	
	        case R.id.menu_about:
	        	resId = R.string.about;
	        	showAbout();
	            break;
	        	
	        case R.id.menu_profile:
	        	resId = R.string.profile;
	        	menuType = MenuType.MY_PROFILE;
	            break;
	            
	        case R.id.menu_help:
	        	resId = R.string.help;
	        	menuType = MenuType.HELP;
	            break;
	
	        case R.id.menu_disable_all:
	        	resId = R.string.disable_all;
	        	menuType = MenuType.DISABLE_ALL;
	            handleDisableAll();
	            break;
  
	        case R.id.menu_settings:
	        	resId = R.string.settings;
	        	menuType = MenuType.SETTINGS;
	            break;	
	            
	        case R.id.edit_add_button:
	        	resId = R.string.add;
	        	menuType = MenuType.ADD_BUTTON;
	            break;   
	            
			default:
				result = super.onOptionsItemSelected(item);	            
        }
        
        if(menuType != MenuType.INVALID) {
    		Intent intent = new UiAbstractionLayer().fetchMenuIntent(mContext, menuType);
    		
			if(menuType == MenuType.ADD_BUTTON) {
				if(intent == null) {
					// Show this dialog when the number of visible and enabled automatic 
					// rules is >= 30.
					DialogUtil.showMaxVisibleEnaAutoRulesDialog(mContext);
				} else {
					mDelegate.showAddRuleList(numOfVisibleRules);
					return true;
				}
			}
    		
    		if(intent != null)
    			startActivity(intent);
    		
	        DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.INTERNAL, 
        						getString(resId), null, null, SMARTRULES_INTERNAL_DBG_MSG, 
        						null, null, PACKAGE, PACKAGE);
        }
        return result;        
    }

    /** onCreateContextMenu()
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {     
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;		
		clickedListRow = getClickedListRowObject(info.position);		
        if(clickedListRow == null) {
        	Log.e(TAG, "Could not fetch the list row object for "+info.position);
        } else {
            // Sets the menu header to be the title of the selected note.
            menu.setHeaderTitle((String) clickedListRow.get(RuleTable.Columns.NAME));            
            menu.add(0, MenuOptions.VIEW_RULE, 0, R.string.view);
            menu.add(0, MenuOptions.DELETE_RULE, 0, R.string.delete);
        }
    }
    
    /** onContextItemSelected()
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	
    	long _id = (Long) clickedListRow.get(RuleTable.Columns._ID);
        switch (item.getItemId()) {
        
        	case MenuOptions.VIEW_RULE:
	        	startPuzzleBuilder(_id);
	        	return true;
	        	
            case MenuOptions.DELETE_RULE:
	            if(LOG_DEBUG) Log.d(TAG, "Delete the row _id:"+_id);
	            showDeleteRuleDialog();
	            return true;
	            
	        default:
	            return super.onContextItemSelected(item);
        }
    }

    /** Displays the about dialog.
     */
    private void showAbout() {
    	String[] packageDetails = Util.getPackageDetails(mContext);
		String version = getString(R.string.about_copyright, packageDetails[0]);    	
		AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
		builder.setMessage(version)
				.setTitle(R.string.about_title)
				.setPositiveButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});
		AlertDialog alert = builder.create();
		// Checkin Debug message for SmartAction Statistics.
		DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
        		null, null, null,
                SMARTRULES_INTERNAL_DBG_MSG, null, ABOUT_VERSION,
                Constants.PACKAGE, Constants.PACKAGE);
		
		alert.show();
    }
    
    /** returns the list row object associated with the clicked row.
     * 
     * @param position - position in the list clicked
     * @return - the list row object
     */
    private ListRow getClickedListRowObject(int position) {
    	ListRow listRow = null;
    	try {
	    	if(position <= mAutoList.size()) {
	    		listRow = mAutoList.get(position - 1);
	    	}
	    	else if(position >= mAutoList.size() + 1 
	    			&& position <= mAutoList.size() + mManualList.size() + 1) {
	    		int relativePos = position - 1;
	    		if(mAutoList != null && mAutoList.size() > 0)
	    			relativePos = relativePos - (mAutoList.size() + 1);
	    		if(LOG_DEBUG) Log.d(TAG, "fetching the relative pos "+relativePos+" " +
	    				"from manual list of size "+mManualList.size());
	    		listRow = mManualList.get(relativePos);
	    	}   
    	} catch (ArrayIndexOutOfBoundsException e) {
    		Log.e(TAG, "ArrayIndexOutOfBoundsException while retrieving the " +
    				"list row object for "+position+" auto list size is "+mAutoList.size()+
    				" manual list size = "+mManualList.size());
    		e.printStackTrace();
    	}
    	return listRow;
    }
    
     /** dismiss the progress dialogs shown if any.
     */
    private void dismissProgressDialogsShown() {
    	if(mDeleteRuleProgressDialog != null) {
    		if(mDeleteRuleProgressDialog.isShowing())
    			mDeleteRuleProgressDialog.dismiss();
    		mDeleteRuleProgressDialog = null;
    	}
    	
    	if(mDisableAllProgressDialog != null && mDisableAllProgressDialog.isShowing()) {
    		mDisableAllProgressDialog.dismiss();
    		mDisableAllProgressDialog = null;
    	}
    }
    
    /** unregister for the content observer and VSM receivers.
     */
    private void unregisterBroadcastListeners() {
        if(myContentObserver != null)
        	mContext.getContentResolver().unregisterContentObserver(myContentObserver);
        if(rulesServiceReceiver != null) {
        	if(LOG_DEBUG) Log.d(TAG, "unregister the receiever to listen to SmartRulesService intent");
        	mContext.unregisterReceiver(rulesServiceReceiver);
        	rulesServiceReceiver = null;
        }
    }

    /** register for the content observer and VSM receivers.
     */
    private void registerBroadcastListeners() {
    	mContext.getContentResolver().registerContentObserver(Schema.RULE_TABLE_CONTENT_URI, true, myContentObserver);
    }
    
    /** set the visibility flags for the group of items that will be
     *  shown to the user when the menu key is selected.
     * 
     * @param menu - menu item
     */
	private void setMenuGroupsVisibility(Menu menu) {
		if(numOfVisibleRules == 0) {
                        //when no rules, introduce page should have no menu shown
			menu.setGroupVisible(R.id.full_menu, false);
			menu.setGroupVisible(R.id.no_rules_menu, false);
		} 
		else {
			// There are user rules visible on the Landing Page so show also
			// the menu group with options Check Status and Disable All.
			menu.setGroupVisible(R.id.full_menu, true);
			menu.setGroupVisible(R.id.no_rules_menu, true);			
		}	
	}
	
    /** restores the critical variables saved during pause operation.
     */
    protected void onRestoreState(Bundle savedInstanceState) {

        if (savedInstanceState != null) {
            clickedListRow = new ListRow();
            clickedListRow.put(RuleTable.Columns._ID, 
            					savedInstanceState.getLong(RuleTable.Columns._ID));
            clickedListRow.put(RuleTable.Columns.NAME, 
            					savedInstanceState.getString(RuleTable.Columns.NAME));
            clickedListRow.put(RuleTable.Columns.KEY, 
            					savedInstanceState.getString(RuleTable.Columns.KEY));
            clickedListRow.put(RuleTable.Columns.ACTIVE, 
            					savedInstanceState.getInt(RuleTable.Columns.ACTIVE));
            clickedListRow.put(RuleTable.Columns.ENABLED, 
            					savedInstanceState.getInt(RuleTable.Columns.ENABLED));
            clickedListRow.put(RuleTable.Columns.RULE_TYPE, 
            					savedInstanceState.getInt(RuleTable.Columns.RULE_TYPE));
        }
    }
    
    /** Sets the content view and the onclick properties.
     */
    private View setViewLayoutParameters(LayoutInflater inflater, ViewGroup container) {
		View contentView = inflater.inflate(R.layout.mm_landing_page_list, container, false);
        Button learnMore = (Button) contentView.findViewById(R.id.learn_more_button);
        learnMore.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				Util.showHelp(mContext);
			}        	
        });  
        Button getStarted = (Button) contentView.findViewById(R.id.get_started_button);
        getStarted.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				mDelegate.showAddRuleList(numOfVisibleRules);
			}        	
        });  
        return contentView;
    }
   
    /** Dismisses the notification and handles the disabling of all the rules.
     */
    private void handleDisableAll() {
        if(LOG_DEBUG) Log.d(TAG, "Disabling all rules");

           
        mDisableAllProgressDialog = ProgressDialog.show(mContext, "", 
        							mContext.getString(R.string.disabling_all), true);
        DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
        		null, null, null,
                SMARTRULES_INTERNAL_DBG_MSG, null, DISABLE_ALL_RULES,
                Constants.PACKAGE, Constants.PACKAGE);
        
     // Starts SmartRuleService to diable all rules. The result will be
		// returned via the broadcast receiver.
		rulesServiceReceiver = registerRulesServiceReceiver();

		Intent intent = new Intent(mContext, SmartRulesService.class);
        intent.putExtra(Constants.MM_DISABLE_ALL, true);
        mContext.startService(intent);

    }
    
    /** Registers receiver for intent from SmartRulesService.
     *
     * @return broadcastReceiver for SmartRulesService intent
     */
    private BroadcastReceiver registerRulesServiceReceiver() {
	IntentFilter filter = new IntentFilter(INTENT_RULES_DISABLED);
	BroadcastReceiver receiver = new BroadcastReceiver() {
		    @Override
		    public void onReceive(Context ct, Intent intent) {

		        if (LOG_DEBUG) Log.d(TAG, "onReceive SmartRulesServiceReceiver:"+intent.getAction());
                String action = intent.getAction();

                if (action != null && action.equals(INTENT_RULES_DISABLED)) {
			mHandler.sendEmptyMessage(HandlerMessage.DISABLE_ALL_REFRESH);
                }
            }
	};
	mContext.registerReceiver(receiver, filter);
	return receiver;
	}

    /** launches the GraphicsActivity class
    *
    * @param ruleId - rule ID that is to be viewed in puzzle builder.
    */
   private void startPuzzleBuilder(final long ruleId) {
	Rule rule = RulePersistence.fetchFullRule(mContext, ruleId);
	if(rule == null) {
		Log.e(TAG, "fetchFullRule returned null for "+ruleId);
	} else {
		if(LOG_DEBUG) Log.d(TAG, "launching rules builder for "+ruleId);
		Intent launchIntent = rule.fetchRulesBuilderIntent(mContext, false);
		mDelegate.showRuleEditorForRule(launchIntent.getExtras(), RULE_EDIT, false);
	}
   }


    /** refreshes the list. 
     */
	protected void refreshList() {
		// Short circuit this method if fragment is no longer displayed.
    	if (!isVisible()) return;
    	
		numOfVisibleRules = 0;
		if (mListAdapter == null) {
			mListAdapter = new SeparatedListAdapter(getActivity());
			mListAdapter.setDelegate(this);
		} else { 
			mListAdapter.clear();
		}
		isLocationErrorNeedsToBeShown = LocationConsent.isLocationErrorRequired(mContext);

		if((mAutoList == null || mAutoList.size() == 0) 
				&& (mManualList == null || mManualList.size() == 0))
			showNoRulesLayout();
		else
			hideNoRulesLayout();
		
		if(mAutoList != null && mAutoList.size() > 0) {
	   		numOfVisibleRules = numOfVisibleRules + mAutoList.size();
			mListAdapter.addSection(mContext.getString(R.string.auto_header), 
										mAutoList.bindLandingPageListToAdapter(mContext));
		}
		
		if(mManualList != null && mManualList.size() > 0) {
	   		numOfVisibleRules = numOfVisibleRules + mManualList.size();
			mListAdapter.addSection(mContext.getString(R.string.manual_header), 
										mManualList.bindLandingPageListToAdapter(mContext));
		}
                setListAdapter(mListAdapter);
                getListView().invalidateViews();
                getListView().setOnCreateContextMenuListener(this);
                
                if(mMenu != null)
                	setMenuGroupsVisibility(mMenu);
	}
	
	/** refreshes the Automatic Rules section
	 * 
	 * @param context - context
	 */
	protected void refreshAutoRulesList(Context context) {
		mAutoList = AddRuleList.getData(context, LIST_ROW_TYPE_AUTO);
		mAutoList.customizeList(context, LIST_ROW_TYPE_AUTO);
		
	}
	
	/** refreshes the Manual Rules section
	 * 
	 * @param context - context
	 */
	protected void refreshManualRulesList(Context context) {
		mManualList = AddRuleList.getData(context, LIST_ROW_TYPE_MANUAL);
		mManualList.customizeList(context, LIST_ROW_TYPE_MANUAL);
	}	
	
	/** refreshes the rules list
	 */
	private void refreshRulesList() {
		RefreshRulesList rulesList = new RefreshRulesList();
		rulesList.execute();	
	}
	
	/** Async task to fetch and refresh the manual and automatic rules section.
	 */
	private class RefreshRulesList extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			refreshAutoRulesList(mContext);
			refreshManualRulesList(mContext);			
			return null;
		} 
		
        @Override
        protected void onPostExecute(Void result) {
        	refreshList();
        }   	
	}

    /** Sets the view values for each element of the row.
     */
	public void setViewValue(int position, View view, final Object data) {
		ListRow listRow = (ListRow) data;	
		view.setTag(listRow);
		setRuleIcon(view, listRow);
		setSecondLineText(view, listRow);
		setStatusLineText(view, listRow);
	}
    
    /** Sets the rule icon and background for the view passed in
     * 
     * @param view - view
     * @param cursor - rule cursor
     */
	private void setRuleIcon(final View view, final ListRow listRow) {
        boolean isEnabled = (Integer) listRow.get(RuleTable.Columns.ENABLED) 
        							== RuleTable.Enabled.ENABLED;
        
        ImageView icon = (ImageView) view.findViewById(R.id.rule_icon);
        LinearLayout iconWrapper = (LinearLayout) view.findViewById(R.id.rule_icon_wrapper);

        String ruleIconString = (String) listRow.get(RuleTable.Columns.ICON);
        int iconResId = R.drawable.ic_default_w;
        if(ruleIconString != null) {
		Drawable iconDrawable = IconPersistence.getIconDrawableFromBlob(mContext,
				(byte[]) listRow.get(IconTableColumns.ICON));
            icon.setBackgroundDrawable(iconDrawable);
        } else {
            icon.setBackgroundDrawable(getResources().getDrawable(iconResId));
        }
        iconWrapper.setAlpha(isEnabled ? OPAQUE_APLHA_VALUE : FIFTY_PERECENT_ALPHA_VALUE);
    }

    /** Sets the second line of test for the list row
     * 
     * @param view - view
     * @param listRow - list row
     */
    private void setSecondLineText(View view, final ListRow listRow) {
    	
		int listRowType = (Integer) listRow.get(LIST_ROW_TYPE_KEY);

		TextView secondLine = (TextView) view.findViewById(R.id.second_line);

		int secondLineResId = -1;
		boolean active = (Integer) listRow.get(RuleTable.Columns.ACTIVE)
							== RuleTable.Active.ACTIVE;
		boolean enabled = (Integer) listRow.get(RuleTable.Columns.ENABLED)
							== RuleTable.Enabled.ENABLED;
        int color = R.color.active_blue;
        
		switch(listRowType) {
			case ListRowInterface.LIST_ROW_TYPE_AUTO:
				if(enabled) {
					if(active)
						secondLineResId = R.string.active;
					else {
						secondLineResId = R.string.ready;
						color = R.color.disable_gray;
					}
	
				} else {
					secondLineResId = R.string.off;
		        	color = R.color.disable_gray;
				}			
				break;
				
			case ListRowInterface.LIST_ROW_TYPE_MANUAL:
				if(enabled && active)
					secondLineResId = R.string.active;
				else {
					secondLineResId = R.string.off;
		        	color = R.color.disable_gray;
				}
				break;
		}

		if(secondLineResId == -1) {
			secondLine.setVisibility(View.INVISIBLE);
		}
		else {
			secondLine.setText(secondLineResId);
			secondLine.setVisibility(View.VISIBLE);
			secondLine.setTextColor(mContext.getResources().getColor(color));
		}
       
    }
    
    /** sets the value and visibility of the status line text
     * 
     * @param view - view
     * @param listRow - list row object
     */
    private void setStatusLineText(View view, ListRow listRow) {
		int failCount = (Integer) listRow.get(RulePersistence.FAIL_COUNT);
		int suggestionActionCount = (Integer) listRow.get(RulePersistence.SUGGESTION_ACTION_COUNT);
		int suggestionConditionCount = (Integer) listRow.get(RulePersistence.SUGGESTION_CONDITION_COUNT);		
		int suggestionCount = suggestionActionCount + suggestionConditionCount;
		int locBlockCount = (Integer) listRow.get(RulePersistence.LOCATION_BLOCK_COUNT);	
		int listRowType = (Integer) listRow.get(LIST_ROW_TYPE_KEY);
		boolean invalid = false;
		String validity = ((String) listRow.get(RuleTable.Columns.VALIDITY));
		if(LOG_DEBUG) Log.d(TAG, " Rule validity is " + validity);
		if(validity != null && validity.equals(TableBase.Validity.INVALID)){
			invalid = true;
		}
		
		TextView statusLine = (TextView) view.findViewById(R.id.status_line);

		int statusLineText = 0;
        int color = -1;
    	boolean showStatusLineView = true;			   
       
		switch(listRowType) {
			case ListRowInterface.LIST_ROW_TYPE_AUTO:
				if(failCount > 0 || (isLocationErrorNeedsToBeShown && locBlockCount > 0) || invalid) {
					statusLineText = R.string.error;
					color = R.color.error_orange;
				} else if(suggestionCount > 0) {
					statusLineText = R.string.suggestion;
		        	color = R.color.suggestion_green;
				} else
	                showStatusLineView = false;
				break;
				
			case ListRowInterface.LIST_ROW_TYPE_MANUAL:
				if(failCount > 0 || (isLocationErrorNeedsToBeShown && locBlockCount > 0) || invalid) {
					statusLineText = R.string.error;
					color = R.color.error_orange;
				} else
					showStatusLineView = false;
				break;
		} 
		
		if(showStatusLineView) {
			statusLine.setVisibility(View.VISIBLE);
			statusLine.setText(statusLineText);
			statusLine.setTextColor(mContext.getResources().getColor(color));

		} else {
			statusLine.setVisibility(View.INVISIBLE);
		}
    }
           
	/** Handles the click of items in a list row.
     */
	@Override
	public void onListItemClick(ListView list, View view, int position, long id) {
		clickedListRow = (ListRow) view.getTag();
		if(clickedListRow != null) {
			startRuleEditor(view);
		}
	}
	
    /** Displays the screen to the user when there are no rules to display.
     */
    private void showNoRulesLayout() {
    	View contentView = getView();
    	ScrollView aboutLayoutWrapper  = (ScrollView) contentView.findViewById(R.id.about_layout_wrapper);
        aboutLayoutWrapper.setVisibility(View.VISIBLE);
        ListView listView = (ListView) contentView.findViewById(android.R.id.list);
        listView.setVisibility(View.GONE);
    }

    /** Hides the layout that is shown to the user when there are no rules.
     */
    private void hideNoRulesLayout() {
    	View contentView = getView();
    	ScrollView aboutLayoutWrapper  = (ScrollView) contentView.findViewById(R.id.about_layout_wrapper);
        aboutLayoutWrapper.setVisibility(View.GONE);
        ListView listView = (ListView) contentView.findViewById(android.R.id.list);
        listView.setVisibility(View.VISIBLE);
    }
    
    private void showDeleteRuleDialog() {
		DialogFragment frag = DeleteRuleDialogFragment.newInstance();
		frag.setTargetFragment(this, 0); frag.show(getFragmentManager(),
		DeleteRuleDialogFragment.TAG);
	}
	
	public void onDeleteRule() {
		
		// Creating a blocking progress dialog so that the user is not able to delete the same
        // rule multiple times or do simultaneous multiple deletes and cause ANR. Basically trying
        // to synchronize the delete process.
		mDeleteRuleProgressDialog = ProgressDialog.show(mContext, "", mContext.getString(R.string.deleting_rule), true);
		
		// Check of the rule is active before proceeding - use case as
		// in IKINTNETAPP-400
		long _id = (Long) clickedListRow.get(RuleTable.Columns._ID);
		String key = (String) clickedListRow.get(RuleTable.Columns.KEY);
		String name = (String) clickedListRow.get(RuleTable.Columns.NAME);
		boolean isActive = RulePersistence.isRuleActive(mContext, _id);
		
        if(isActive) {                	
        	if(LOG_DEBUG) Log.d(TAG, "Start SmartRulesService to process the " +
        			"			rule state change of active rule before deleting");
        	// The rule will be deleted in the service - so that even if the user 
        	// accidentally hits the home key the rule is actually deleted
        	// and does not show up in Ready/Disabled state.
			Intent serviceIntent = new Intent(mContext, SmartRulesService.class);
			serviceIntent.putExtra(MM_RULE_KEY, key);
			serviceIntent.putExtra(MM_RULE_STATUS, FALSE);
			serviceIntent.putExtra(MM_DELETE_RULE, true);
			mContext.startService(serviceIntent);
        }
        else {
        	if(LOG_DEBUG) Log.d(TAG, "Deleting an non-active " +
        			"					manual or auto rule "+_id);
        	RulePersistence.deleteRule(mContext, _id, name, key, true);
        }
	}
	
	private void startRuleEditor(View view) {

		// get the icon res id
		String ruleIconString = (String) clickedListRow.get(RuleTable.Columns.ICON);
        int iconResId = R.drawable.ic_default_w;
        if(ruleIconString != null && ruleIconList.contains(ruleIconString))
        	iconResId = getResources().getIdentifier(ruleIconString, "drawable", getActivity().getPackageName());
        
        // get the icon location
		int[] sourceLocation = new int[2];
		ImageView iconView = (ImageView) view.findViewById(R.id.rule_icon);
		iconView.getLocationInWindow(sourceLocation);
		
		// get the rule name
		String name = (String) ((TextView) view.findViewById(R.id.first_line)).getText();
		String state = (String) ((TextView) view.findViewById(R.id.second_line)).getText();
		
		// get the rule id
		long ruleId = (Long) clickedListRow.get(RuleTable.Columns._ID);
		
		// get the rule state
		boolean isActive = (Integer) clickedListRow.get(RuleTable.Columns.ACTIVE) == RuleTable.Active.ACTIVE;
		boolean isEnabled = (Integer) clickedListRow.get(RuleTable.Columns.ENABLED) == RuleTable.Enabled.ENABLED;

		Rule rule = RulePersistence.fetchFullRule(mContext, ruleId);
		if (rule != null) {
			Intent launchIntent = rule.fetchRulesBuilderIntent(mContext, false);
			mDelegate.startIconOverlayAnimation(launchIntent.getExtras(), name, state, iconResId, sourceLocation[1], isActive, isEnabled);
		}
	}
	
    /** Container Activity must implement this interface
     */
	public interface Delegate {
		public void showAddRuleList(int numVisibleRules);
		public void showRuleEditorForRule(Bundle extras, int requestCode, boolean useVerticalTransition);
		public void startIconOverlayAnimation(Bundle extras, String ruleName, String ruleState, int resId, int sourceY, boolean isActive, boolean isEnabled);
		public void setReturnResult(Bundle result);
		public Bundle getReturnResult();
	}
}