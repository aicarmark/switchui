/*
 * @(#)CheckStatusActivity.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/12/09 NA				  Initial version
 * CSD053 		 2011/03/17 NA				  Changes for onClick as per CxD
 * 												redesign.
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
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.Action;
import com.motorola.contextual.smartrules.db.business.IconPersistence;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.IconTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.uiabstraction.ConflictItem;
import com.motorola.contextual.smartrules.uiabstraction.UiAbstractionLayer;
import com.motorola.contextual.smartrules.util.Util;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.SimpleAdapter.ViewBinder;
import android.widget.TextView;

/** This class abstracts the list activity which shows actions and conflicting actions
 * that are resolved using ConflictResolution
 *
 *
 *<code><pre>
 * CLASS:
 * 	Extends ListActivity which provides basic list building, scrolling, etc.
 *
 * RESPONSIBILITIES:
 * 	Show actions currently in effect by various rules.
 * 	Show conflicting actions as a result of various rules overriding the same setting.
 *
 * COLABORATORS:
 * 	ConflictResolution as part of the SmartRules service.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class CheckStatusActivity extends ListActivity implements Constants, 
																 DbSyntax, 
																 AsyncTaskHandler, 
																 ViewBinder {

    private static final String TAG = CheckStatusActivity.class.getSimpleName();

    private static final float EIGHTY_PERCENT_ALPHA_VALUE = 0.8f;
    
    private interface OverrideActionItems {
    	public final int GO_TO_ACTIVE_RULE = 0;
    	//public final int RESET_DEFAULT = GO_TO_ACTIVE_RULE + 1;
    	public final int GO_TO_SETTINGS = GO_TO_ACTIVE_RULE + 1;
    };

    /** Holds message types for handler.
     */
    private static interface HandlerMessage {
        int REFRESH_LIST = 0;
    }
    
    /** class to store the action meta data from the package manager.
     */
    private static class ActionMetaData {
    	Drawable icon;
    	String settingsIntentString;
    	
    	/** constructor
    	 * 
    	 * @param icon - icon for the action
    	 * @param settingsIntent - intent string for the action setting
    	 */
    	ActionMetaData(Drawable icon, String settingsIntent) {
    		this.icon = icon;
    		this.settingsIntentString = settingsIntent;
    	}
    }
    
    private Context mContext = null;
    private HashMap<String, ActionMetaData> actionPubMetaData = 
    										new HashMap<String, ActionMetaData>();
    private boolean onClickHandled = false;
    private List<ConflictItem> mConflictList = null;
    private List<String> ruleIconList		 = null;

    /** Handler for all messages, see message types below.
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {

            if (LOG_DEBUG) Log.d(TAG, ".handleMessage - msg="+msg.what);

            switch (msg.what) {

            case HandlerMessage.REFRESH_LIST:
            	showSettingsControlList();
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
                localHandler.sendEmptyMessageDelayed(HandlerMessage.REFRESH_LIST, 2000);
            }
        }
    }
    private MyContentObserver myContentObserver = new MyContentObserver(mHandler);
    
    /** onCreate()
     */
    public void onCreate(Bundle savedInstanceState) {
    	if(LOG_VERBOSE) Log.v(TAG, "In onCreate");
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.check_status_list);
        
        ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
 		ab.show();
 		
 		DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT,
        		null, null, null,
                SMARTRULES_INTERNAL_DBG_MSG, null, CHECK_STATUS,
                Constants.PACKAGE, Constants.PACKAGE);
    }

    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        if (LOG_VERBOSE) Log.v(TAG, "In onDestroy");
        super.onDestroy();
        if(actionPubMetaData != null) {
        	if(LOG_DEBUG) Log.d(TAG, "Clearing the actionPubMetaData" +
        								" of size = "+actionPubMetaData.size());
        	actionPubMetaData.clear();
        }
    }
    
    /** onResume()
     */
    @Override
    public void onResume() {
    	if(LOG_VERBOSE) Log.v(TAG, "In onResume");
    	super.onResume();
        mContext.getContentResolver().registerContentObserver(
        				Schema.ACTION_TABLE_CONTENT_URI, true, myContentObserver);
        if(ruleIconList == null)
        	ruleIconList = Util.getRuleIconsList(mContext, true);
    	showSettingsControlList();
    }
    
    /** onPause()
     */
    @Override
    public void onPause() {
    	if(LOG_VERBOSE) Log.v(TAG, "In onPause");
    	super.onPause();
    	if(myContentObserver != null)
    		mContext.getContentResolver().unregisterContentObserver(myContentObserver);
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
	
    /** Displays an error message if the conflict list was null or empty
     *  (returned from Smart Rules DB provider)
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

    /** Hides the error message displayed when no rules are active.
     *
     */
    private void hideErrorMessage() {

    	RelativeLayout errRl  	= null;
    	TextView errTextView  = (TextView) findViewById(R.id.failmessage_text);
    	if(errTextView != null) {
    		errRl = (RelativeLayout) errTextView.getParent();
    		errRl.setVisibility(View.GONE);
    	}
    }


    /** Displays the dialog to override an action.
     * 
     * @param context - context
     * @param _id - rule id associated with the item
     * @param activityIntent - intent that needs to used to launch this action intent
     * @param stateMachineName - action name
     * @param actionPubKey - action publisher key
     */
    private void showOverrideDialog(final Context context, final long _id,
    					final String activityIntent, final String stateMachineName,
    					final String actionPubKey) {
    	
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(stateMachineName);
        
        ActionMetaData actionMetaData = actionPubMetaData.get(actionPubKey);
        if(actionMetaData == null)
        	Log.e(TAG, "actionMetaData is null so icon cannot be set in dialog title");
        else
        	builder.setIcon(actionMetaData.icon);
        
        builder.setItems(
        		context.getResources().getStringArray(R.array.override_actions_list_array), 
        		new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                dialog.dismiss();
                onClickHandled = false;
                handleOverrideItemSelection(context, item, _id, 
                		activityIntent, actionPubKey);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                onClickHandled = false;
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {			
			public void onCancel(DialogInterface dialog) {
                onClickHandled = false;
			}
        });
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
                    onClickHandled = false;                	
                }
                return false; // Any other keys are still processed as normal
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    /** handles the user selection of the override dialog item.
     * 
     * @param context - context
     * @param item - item selected in the dialog
     * @param _id - rule id associated with the item
     * @param activityIntent - intent that needs to used to launch this action intent
     * @param actionPubKey - action publisher key
     */
    private void handleOverrideItemSelection(final Context context, final int item,
    						final long _id, final String activityIntent, 
    						final String actionPubKey) {
    
    	switch(item) {
	    	case OverrideActionItems.GO_TO_ACTIVE_RULE:
	    		launchPuzzleBuilder(context, _id);
	    		break;
    		
	    	/* case OverrideActionItems.RESET_DEFAULT:
	    		setDefaultValueForAction(context, actionPubKey);
	    		break; */
	    
	    	case OverrideActionItems.GO_TO_SETTINGS:	    		
	    	   	startSettingsActivityForAction(activityIntent, actionPubKey);	    		
	    		break;
    	}  	
    }
    
    /** launches the puzzle builder for the rule ID.
     * 
     * @param context - context
     * @param _id - rule ID
     */
    private void launchPuzzleBuilder(final Context context, final long _id) {
    	Rule rule_inst = RulePersistence.fetchFullRule(mContext, _id);
    	if(rule_inst == null) {
    		Log.e(TAG, "Cannot launch rules builder, fetchFullRule returned null for "+_id);
    	} else {
    		Intent intent = rule_inst.fetchRulesBuilderIntent(mContext, false);
    		startActivityForResult(intent, RULE_EDIT);
    	}
    }
    
    /** sets the default value for the action that is selected
     * 
     * @param context - context
     * @param actionPubKey - action publisher key
     */
	@SuppressWarnings("unused")
	private void setDefaultValueForAction(final Context context, final String actionPubKey) {
    	
		Cursor defaultActionsCursor = null;
		try {
			long defaultRuleId = RulePersistence.getDefaultRuleId(context);
			String whereClause = 
					ActionTable.Columns.PARENT_FKEY + EQUALS + Q + defaultRuleId + Q +
					AND + ActionTable.Columns.ACTION_PUBLISHER_KEY + 
						EQUALS + Q + actionPubKey + Q;			
			defaultActionsCursor = context.getContentResolver().query(
					Schema.ACTION_TABLE_CONTENT_URI, null, whereClause, null, null);

			if(defaultActionsCursor != null) {
				if(defaultActionsCursor.moveToFirst()) {
                    String config = defaultActionsCursor.getString(
                            defaultActionsCursor.getColumnIndexOrThrow(ActionTable.Columns.CONFIG));
                    long id = defaultActionsCursor.getLong(defaultActionsCursor.getColumnIndex(ActionTable.Columns._ID));

                    Action.sendBroadcastIntentForAction(context, actionPubKey, DEFAULT_RULE_KEY,
                                            context.getString(R.string.default_rule), false, null, COMMAND_FIRE, config, id);

		
					whereClause = ActionTable.Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + actionPubKey + Q;
		            ContentValues	contentValues = new ContentValues();
		            contentValues.put(ActionTable.Columns.ACTIVE, 
		            						ActionTable.Active.INACTIVE);
		            context.getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, 
		            							contentValues, whereClause, null);
		            
		            showSettingsControlList();
				}
				else 
					Log.e(TAG, "defaultActionsCursor.moveToFirst() for "+defaultRuleId);
			}
			else
				Log.e(TAG, "defaultActionsCursor is null for "+defaultRuleId);
		} catch (Exception e) {
			Log.e(TAG, PROVIDER_CRASH+" fetching the default action " +
								"cursor for "+actionPubKey);
			e.printStackTrace();
		} finally {
			if(defaultActionsCursor != null && !defaultActionsCursor.isClosed())
				defaultActionsCursor.close();
		}
    }
    
    /** starts the settings activity related to the activity intent if found in 
     *  the quick actions manifest else will just starts the default settings activity.
     * 
     * @param activityIntent - action activity intent
     * @param actionPubKey - action publisher key
     */
    private void startSettingsActivityForAction(final String activityIntent, 
    											final String actionPubKey) {
    	
	   	if(LOG_DEBUG) Log.d(TAG, "in getting metadata for "+activityIntent);
        Intent settingsIntent = null;
	   	if(actionPubMetaData == null) {
	   		Log.e(TAG, "actionPubMetaData is null so start default settings activity");
	   		settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
    		startActivity(settingsIntent);
	   	} else {
	   		ActionMetaData actMetaData = actionPubMetaData.get(actionPubKey);
	   		if(actMetaData != null) {
		   		String settingsIntentString = actMetaData.settingsIntentString;
		        if(settingsIntentString != null)
		        	settingsIntent = new Intent(settingsIntentString);
		        else
		        	settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
		        
			if(LOG_DEBUG) Log.d(TAG, "settingsIntent is = "+settingsIntent.toUri(0));
		    	
		    	try {
		    		startActivity(settingsIntent);
		    	} catch (Exception ActivityNotFoundException) {
		    		Log.e(TAG, "Activity "+settingsIntent+" not found");
		        	settingsIntent = new Intent(android.provider.Settings.ACTION_SETTINGS);
		    		startActivity(settingsIntent);
		    	}	
	   		}
	   	}	
    }

    /** displays the list of actions controlled by the smart rules app currently along 
     *  with the mode that is controlling it. If there are no active actions controlled 
     *  by Smart Rules then an error message is displayed to the user.
     */
    private void showSettingsControlList() {
        if(LOG_DEBUG) Log.d(TAG, "in showSettingsControlList");
        SettingsControlList settingsControlList = new SettingsControlList(this);
        settingsControlList.execute(this);
    }
    
	/** Async task class to handle the query to fetch the active settings controlled
	 *  by Smart Actions.
	 */
    private class SettingsControlList 
    					extends AsyncTask<Context, Integer, List<ConflictItem>> {

    	private AsyncTaskHandler qHandler = null;
    	
    	/** constructor
    	 * 
    	 * @param handler - callback handler instance
    	 */
    	SettingsControlList(AsyncTaskHandler handler) {
    		qHandler = handler;
    	}
    	
    	@Override
		protected void onPostExecute(List<ConflictItem> list) {
			super.onPostExecute(list);
			if(qHandler != null) 
				qHandler.onSettingControlListQueryFinished(list);
		}
    	
		@Override
		protected List<ConflictItem> doInBackground(Context... params) {
			// Fetch the action publishers meta data only once - the first
			// time we enter this activity. This does not change when we
			// resume this activity.
			if(LOG_DEBUG) Log.d(TAG, " Fetching fetchConflictWinnersList");
			if(actionPubMetaData.size() == 0)
				fetchAllActionsPubMetaData();	
			
			List<ConflictItem> list = 
					new UiAbstractionLayer().fetchConflictWinnersList(mContext);
			return list;
		}
    	
    } 

    /** Processes the cursor returned from the async task and displays the settings list
     * 
     * @param activeActionscursor - cursor of settings controlled by Smart Actions
     */
	public void onSettingControlListQueryFinished(List<ConflictItem> list) {
        if(list != null) {
        	if(list.size() > 0) {
        		mConflictList = list;
            	if(LOG_DEBUG) Log.d(TAG, "conflict list is not null " +
            							"and size is "+list.size());
            	hideErrorMessage();
    	        String[] from = {
    	        	ActionTable.Columns.ACTION_PUBLISHER_KEY,	
    	        	ActionTable.Columns.STATE_MACHINE_NAME,
    	            ActionTable.Columns.ACTION_DESCRIPTION,
	            IconTable.Columns.ICON,
    	            RuleTable.Columns.NAME,
    	        };
    	
    	        int[] to = {
    	        	R.id.setting_icon,	
    	            R.id.setting_line,
    	            R.id.value_line,
    	            R.id.mode_icon,
    	            R.id.mode_text_line,
    	        };
    	        
    	        List<Map<String, Object>> listMap = getListMap(list);
    	        SimpleAdapter listAdapter = new SimpleAdapter(mContext, listMap, 
    	        								R.layout.check_status_row, from, to);
    	        listAdapter.setViewBinder(this);
    			setListAdapter(listAdapter);
        	}
        	else{
        		Log.e(TAG, "conflict list has no records");
        		showErrorMessage(getString(R.string.no_actions));
        	}	
        }
        else {
    		Log.e(TAG, "conflict list is null");
        	showErrorMessage(getString(R.string.no_actions));
        }
	}
	
    /** iterates through the list of elements and adds them to the hash map.
     * 
     * @param list - list of ConflictItem objects
     * @return - a list map of objects
     */
	private static List<Map<String, Object>> getListMap(List<ConflictItem> list) {
		List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>(list.size());
		Iterator<ConflictItem> iter = list.iterator();
		while(iter.hasNext()) {
			ConflictItem item = iter.next();
			listMap.add(item.getMap());
		}
		return listMap;
	}


	public boolean setViewValue(View view, Object data, String textRepresentation) {
		boolean result = false;
		int id = view.getId();		
		switch(id) {
			case R.id.setting_icon:
				if(view instanceof ImageView) {
					ImageView imageView = (ImageView) view;
	        		Bitmap actionBitmap = null;	
	        		ActionMetaData actionMetaData = actionPubMetaData.get((String) data);

	        		if(actionMetaData != null) {
		        		Drawable drawable = actionMetaData.icon;
		        		if(drawable != null) {
		        			Bitmap src = ((BitmapDrawable) drawable).getBitmap();
				        	actionBitmap = Bitmap.createScaledBitmap(src, src.getWidth(), 
				        									src.getHeight(), true);
		        		}	
	        		}
	        		imageView.setImageDrawable(new BitmapDrawable(actionBitmap));
	        		imageView.setAlpha(EIGHTY_PERCENT_ALPHA_VALUE);
	        		result = true;
				}
				break;
			
			case R.id.mode_icon:
	        	if(view instanceof ImageView) {
		    		ImageView modeIcon = 
		    			(ImageView) ((View)view.getParent()).findViewById(R.id.mode_icon);
		    		modeIcon.setVisibility(View.VISIBLE);
		    		int iconResId = R.drawable.ic_default_w;
				if(data != null) {
					modeIcon.setImageDrawable(IconPersistence.getIconDrawableFromBlob(mContext, (byte[])data));
				} else
					modeIcon.setImageDrawable(getResources().getDrawable(iconResId));
		            result = true;
	        	}	
		}
		return result;
	}
	
    /** fetches the publisher key and icon for all available action publishers in the
     *  system for use in showing the list.
     */
    public void fetchAllActionsPubMetaData() {
    	Intent mainIntent = new Intent(ACTION_GET_CONFIG, null);
		mainIntent.addCategory(ACTION_PACKAGE_MANAGER_STRING);
		
    	PackageManager pm = mContext.getPackageManager();
    	List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 
    										PackageManager.GET_META_DATA);
    	int len = list.size();
    	
    	for (int i = 0; i < len; i++) {
    		
 	    	ResolveInfo info = list.get(i);
 	    	Drawable drawable = info.loadIcon(pm);
 	    	String publisherKey = null;
			String settingsIntentString = null;

 	    	Bundle metaData = info.activityInfo.metaData;
 	    	if (metaData != null ) {
			publisherKey = metaData.getString(GENERIC_PUBLISHER_KEY);
	            settingsIntentString = metaData.getString(QA_SETTINGS_INTENT_METADATA);
 	    	}
	          
 	    	if(drawable != null && publisherKey != null) {
 	    		if(LOG_DEBUG) Log.d(TAG, "For publisherKey "+publisherKey);	        	
	        	ActionMetaData actionData = new ActionMetaData(drawable, 
	        										settingsIntentString);	        	
	        	actionPubMetaData.put(publisherKey, actionData);
        	} else {
        		Log.e(TAG, "drawable is "+drawable == null ? "null" : "not-null"
        							+" publisherkey = "+publisherKey);
        	}
 	    }
    }

    /** Handles the click of items via a physical keyboard in a list row.
     */
	@Override
    protected void onListItemClick(ListView list, View view, int position, long id) {  
    	if(onClickHandled) {
    		if(LOG_DEBUG) Log.d (TAG, "On Click already handled ignoring " +
    				"further user clicks");
    	} else {
    		if(mConflictList != null) {
        		onClickHandled = true;
    			ConflictItem item = mConflictList.get(position);  			
    			if(LOG_DEBUG) Log.d(TAG, "Clicked for list item "+item.toString());
    			showOverrideDialog(mContext, item.ruleId, item.actionActivityIntent, 
    					item.actionName, item.actionPubKey);    			

    		}
    	}
    }
}

/** Interface for the call backs to the main thread from the Async Task.
 */
interface AsyncTaskHandler {
	void onSettingControlListQueryFinished(List<ConflictItem> list);
}
