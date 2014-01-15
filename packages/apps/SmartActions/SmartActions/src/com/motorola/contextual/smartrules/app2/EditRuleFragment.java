/*
 * @(#)EditRuleFragment.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185 		2012/05/14   NA				  Re-designed version
 * VHJ384       2012/07/15   NA				  Conversion to Fragment
 *
 */

package com.motorola.contextual.smartrules.app2;

import java.net.URISyntaxException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app2.LandingPageFragment.LandingPageIntentExtras;
import com.motorola.contextual.smartrules.app2.dialog.AirplaneModeDialogFragment;
import com.motorola.contextual.smartrules.app2.dialog.DeleteRuleDialogFragment;
import com.motorola.contextual.smartrules.app2.dialog.DiscardRuleDialogFragment;
import com.motorola.contextual.smartrules.app2.dialog.ManualRuleDialogFragment;
import com.motorola.contextual.smartrules.app2.dialog.WiFiLocationDialogFragment;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.ActionPersistence;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.db.table.RuleTable.Columns;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.rulesbuilder.BlockController;
import com.motorola.contextual.smartrules.rulesbuilder.BlockGestureListener;
import com.motorola.contextual.smartrules.rulesbuilder.BlockLayout;
import com.motorola.contextual.smartrules.rulesbuilder.BlockLayout.BlockLayerInterface;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks.EditType;
import com.motorola.contextual.smartrules.rulesbuilder.DisplayActionsActivity;
import com.motorola.contextual.smartrules.rulesbuilder.DisplayConditionsActivity;
import com.motorola.contextual.smartrules.rulesbuilder.DisplayRuleIconGrid;
import com.motorola.contextual.smartrules.rulesbuilder.IBlocksMotionAndAnim;
import com.motorola.contextual.smartrules.rulesbuilder.LocationConsent;
import com.motorola.contextual.smartrules.rulesbuilder.LocationConsent.AIRPLANE_MODE_STATUS;
import com.motorola.contextual.smartrules.rulesbuilder.LocationConsent.ILocationPicker;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderConstants;
import com.motorola.contextual.smartrules.rulesbuilder.RulesBuilderUtils;
import com.motorola.contextual.smartrules.rulesbuilder.SingletonBlocksMediaPlayer;
import com.motorola.contextual.smartrules.service.SmartRulesService;
import com.motorola.contextual.smartrules.uiabstraction.ActionInteractionModel;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel;
import com.motorola.contextual.smartrules.uiabstraction.ConditionInteractionModel.ConditionInteractionModelList;
import com.motorola.contextual.smartrules.uiabstraction.IEditRulePluginCallback;
import com.motorola.contextual.smartrules.uiabstraction.IRuleUserInteraction;
import com.motorola.contextual.smartrules.uiabstraction.RuleController;
import com.motorola.contextual.smartrules.uiabstraction.RuleInteractionModel;
import com.motorola.contextual.smartrules.uipublisher.ActionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisherList;
import com.motorola.contextual.smartrules.uipublisher.IRulesBuilderPublisher;
import com.motorola.contextual.smartrules.uipublisher.Publisher;
import com.motorola.contextual.smartrules.util.Util;
import com.motorola.contextual.smartrules.widget.DialogUtil;

/**The main function of this class is to compose rules from the user selected blocks (actions or conditions) in the Puzzle builder UI and writing them to the Rules DB,
 *  and reading a rule from the Rules DB and displaying the appropriate blocks in the Puzzle Builder UI.
 *
 *<code><pre>
 * CLASS:
 *     Extends Activity.
 *     Implements Constants
 *     Implements DbSyntax to perform SQL operations.
 *     Implements OnLongClickListener to hear the long press of the title bar and rule elements
 *     Implements BlockLayerInterface
 *
 * RESPONSIBILITIES:
 *     -Displays appropriate Puzzle Builder UI, depending on the Rule Key received from the LandingPageActivity class -
 *         Clean UI for Create from Scratch scenario (default rule key) or
 *         UI with appropriate action and condition blocks, for an already existing user-created rule, Preset Rule, Suggested Rule.
 *     -Composes a rule from the action and condition blocks that the user has selected and writes the rule to the Rules DB.
 *     -Constructs action or condition blocks to display on the Puzzle Builder UI, corresponding to a particular rule that the user has selected.
 *     -Interprets block operations (adding, deleting, re-configuring, connecting/disconnecting) that are performed by the user on the Puzzle Builder UI,
 *      and updates the Rules DB accordingly.
 *
 * USAGE:
 *     See each method.
 *
 *</pre></code>
 */
public class EditRuleFragment extends Fragment implements Constants,
                                                          	  RulesBuilderConstants,
                                                          	  DbSyntax,
                                                          	  BlockLayerInterface, 
                                                          	  OnLongClickListener,
                                                          	  IBlocksMotionAndAnim,
                                                          	  ILocationPicker,
                                                          	  IEditRulePluginCallback,
                                                          	  BackKeyListener,
                                                          	  DeleteRuleDialogFragment.DialogListener,
                                                          	  DiscardRuleDialogFragment.DialogListener,
                                                          	  ManualRuleDialogFragment.DialogListener,
                                                          	  AirplaneModeDialogFragment.DialogListener,
                                                          	  WiFiLocationDialogFragment.DialogListener {

    public static final String TAG = EditRuleFragment.class.getSimpleName();
    
    /** Grid view related constants. 
     */
    private static final int GRID_HORIZONTAL_SPACING = 20;
    private static final int GRID_VERTICAL_SPACING = 25;
    private static final int GRID_GRAVITY = 0x11;
    private static final int GRID_NO_OF_COLS = 3;
    private static final int GRID_STRETCH_MODE = 2;
	
    public static final int DEFAULT_RULE_ID = -1;
	public static final int DEFAULT_VALUE = -1;
  
	private static final String SAVE_INTENT_CALLBACK = "com.motorola.contextual.SmartActions.EditRuleActivity.Save";

	
    public static final boolean Debugging = false;
    private Context                   mContext = null;	
	private ActionInteractionModel    mLastAction;
    private ConditionInteractionModel mLastCondition;
    private RuleController			  mRuleController = null;
    private boolean 				  mActivityCurrentlyVisible = true;
    private boolean                   mPresetOrSuggestedEdited = false;
    private boolean					  mInEditMode = false;
    private boolean 				  mLongPressed = false;
    private boolean 				  mSingleTap = false;
    private boolean 				  mFirstTimeLocWiFiDialogShown = false;
    private boolean					  mRulesBuilderBroadcastReceiverRegistered = false;
    private boolean                   mRuleIconClickProcessed = false;
    private boolean 				  mLockedForFlagEdit = false;
    private boolean                   mIconShownInEditMode = false;
    private TextView 				  empty_trigger, empty_action;
    private ProgressDialog            mProgressDialog = null;
    private ProgressDialog 			  mDeleteRuleProgressDialog = null;
    private AlertDialog               mShowRuleIconDialog = null;
    private BroadcastReceiver         mRulesBuilderIntentReceiver = null;
    private	SingletonBlocksMediaPlayer	mInstMediaPlayer = null;
    private boolean                   mActivityPauseUserLeave = false;
    private boolean                   mActivityRuleEditLeave = false;
    private ComponentName             mFromWhereActivityWasInvoked = null;
    private Intent 					  mIntentForDialog = null;
    private View.OnClickListener 	  mThisOnClickListener;
    private GridView                  mRuleIconGridView = null;
    private AlertDialog               mRuleIconDialog   = null;
    private ImageButton               mRuleIconButton = null;
    private EditText                  mEditRuleName = null;
    private TextView                  mViewRuleName = null;
    private TextView				  mRuleStatus = null;
    private Button                    mAddActionButton = null;
    private Button                    mAddTriggerButton = null;
    private View                      mLastBlockViewSelected = null;
    private Switch                    mOnOffSwitch = null;
    //These are Plugin UI implementation specific
    //TODO need to come up with better encapsulation for these
	private BlockController mCondBlockController, mActionBlockController;   // Object that sends out drag-drop events while a view is being moved.
    private BlockLayout mCondBlockLayer, mActionBlockLayer;             // The ViewGroup that supports drag-drop.
    private IRulesBuilderPublisher 	  mPubCallback;
    private IRuleUserInteraction		mRuleInteractCallback;
    //private final Activity mInstance = this;

    
    // SA 2.2 additions
    private Menu mMenu;
	private Delegate mDelegate;
	private int mRequestCode = -1;
	
    /** Holds message types for handler.
     */
    private static interface HandlerMessage {
        int REFRESH_STATUS = 0;
        int EDIT_ACTIVE_RULE = 1;
        int REQ_FOCUS = 2;
        int SKIP_COND_BUILDER = 3;
        int RULE_DELETED = 4;
    }
    
    /** Factory method to instantiate Fragment
     *  @param ruleId
     *  @param ruleCopy
     *  @param requestCode
     *  @return new instance of EditRuleFragment
     */
    public static EditRuleFragment newInstance(Bundle extras, int requestCode) {
    	EditRuleFragment f = new EditRuleFragment();
		extras.putInt(LandingPageFragment.LandingPageIntentExtras.REQUEST_CODE, requestCode);
		f.setArguments(extras);
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
		// Inflate and create the static view hierarchical tree 
        return createAndInflateViewHierarchy(inflater, container);
	}
	
	/** onActivityCreated
	 * TODO: Per code review. Move instantiation of BroadcastReceiver to separate method.
	 */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
    	super.onActivityCreated(savedInstanceState);        
      
    	Activity hostActivity = getActivity();
		
		setHasOptionsMenu(true);
		hostActivity.getActionBar().setDisplayHomeAsUpEnabled(true);
		hostActivity.getActionBar().setTitle(getString(R.string.app_name));
		
		if (null == savedInstanceState) {
        	savedInstanceState = getArguments();
		}
		initializeRulesBuilder(savedInstanceState);

		// unregistered in onDestroy()
        mRulesBuilderIntentReceiver = new BroadcastReceiver(){
        	@Override
        	public void onReceive(Context context, Intent intent) {
        		if (intent == null) {
                    Log.e(TAG, "intent is null");
                }
                else {
                	String action = intent.getAction();
                	if (LOG_DEBUG) Log.d(TAG, "mRulesBuilderIntentReceiver In onReceive to handle : " + action);
                	if((action != null) && (action.equals(SAVE_INTENT_CALLBACK))) {

        	            if (LOG_DEBUG) Log.d(TAG, "mRulesBuilderIntentReceiver In onReceive to handle : " + action);
        	            EditRuleFragment.this.mHandler.sendEmptyMessageDelayed(HandlerMessage.EDIT_ACTIVE_RULE, 2000);
      	            
                	} else{
	                		Log.e(TAG, "Action type does not match");
	                }
                }
            }
        };
    }
    
    /** Determines the target entry in the back stack to return to 
     *  and pops the stack to that target. 
     */
    private void closeFragment() {
		FragmentManager fragMan = getFragmentManager();
		int numFragments = fragMan.getBackStackEntryCount();
		String prevFragName = "";
		
		if (numFragments >= 2) {
			prevFragName = fragMan.getBackStackEntryAt(numFragments - 2).getName();
		}
		
		if (prevFragName.equals(TransitionFragment.TAG)) {
			fragMan.popBackStack(TransitionFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
		} else {
			fragMan.popBackStack();
		}
	}
    
    /** This function initializes the puzzle builder
     *  -Creates a list of the action and condition blocks based on a package manager query
     *  - Reads the values of an existing Rule from the rule table
     *  - Displays the corresponding Action and Condition blocks on the screen.
     *
     * @param startIntent - intent with which the puzzle builder is started.
     */
    private void initializeRulesBuilder(Bundle extras)
    {        
        long ruleId = DEFAULT_RULE_ID;
        boolean ruleCopy = false;
        Rule ruleInst = null;
        if(extras != null) {
			ruleId = extras.getLong(PUZZLE_BUILDER_RULE_ID);
			ruleCopy = extras.getBoolean(PUZZLE_BUILDER_RULE_COPY);
			ruleInst = (Rule)extras.getParcelable(PUZZLE_BUILDER_RULE_INSTANCE);
			
			if(LOG_DEBUG) Log.d(TAG, "ruleId = "+ruleId+" ruleCopy = "+ruleCopy);
        }
        
        // Rule instance should never be null. Even for a blank rule the rule instance
    	// should be an empty rule object. From every where rules builder is launched
    	// the check is done to make sure rule instance is fetched and checked for null
    	// or not before launching rules builder.
    	if(ruleInst == null){
    		Log.e(TAG, "error scenario - should never happen");
    		closeFragment();
    	}
    	
		mRuleController = new RuleController(mContext, ruleInst, ruleCopy);
		mPubCallback = mRuleController;
		mRuleInteractCallback = mRuleController;
		//The callback needs to be initialized for the Controller
		//this is needed for implementing functionality from Controller
		mRuleController.setBlockLayerCallback(this);
		mRuleController.loadRule();
    }	
    
    /** Sets the correct Save and Cancel buttons. 
     */
    private void displayRulesBuilderScreen(){
    	//The rules builder has to be in the Edit mode in the following cases - 
    	// Create from scratch, RuleCopy and Preset/Suggested
        //In these cases, the Add Rule button needs to be displayed.
        if ((mRuleController.getRuleModel().getRuleInstance().get_id() == DEFAULT_RULE_ID) 
        		|| mRuleController.getRuleModel().getIsCopyFlag() 
        		|| mRuleController.getRuleModel().getIsPresetOrSuggestedFlag()) {
        	enterEditMode(false);
        	mEditRuleName.setText(mRuleController.getRuleModel().getRuleInstance().getName());
        }
        // Disable/enable the save button accordingly.
        // On entering an empty Puzzle Builder screen the Save button is disabled and 
        // will be enabled when at least one action has been added
        // When viewing a rule from Landing Page or Drive Widget the save button is
        // disabled until user makes a change.
        // When trying to add a suggestion or sample or via copy rule the save button is
        // enabled if the rule is completely configured else will be disabled.
        if((mRuleController.getRuleModel().getIsCopyFlag() 
        		|| mRuleController.getRuleModel().getIsPresetOrSuggestedFlag())
        		&& mRuleController.getRuleModel().isCompletelyConfigured())
        	toggleSaveButtonStatus(true);
        else 
        	toggleSaveButtonStatus(false);
    }
  
    /** Handler for all messages, see message types below.
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
        	if (LOG_DEBUG) Log.d(TAG, ".handleMessage - msg="+msg.what);

            switch (msg.what) {

            case HandlerMessage.REFRESH_STATUS:
            	handleRefreshStatus();
                break;
                
            case HandlerMessage.EDIT_ACTIVE_RULE:
            	handleEditActiveRule();
                break;
                
            case HandlerMessage.REQ_FOCUS:
            	handleRequestFocus();
            	break;
            
        	case HandlerMessage.SKIP_COND_BUILDER:
        		fireActionsManuallyForSave();
        		break;
        	
        	case HandlerMessage.RULE_DELETED:
	        	if(mDeleteRuleProgressDialog != null) {
	            	
	        		if(mDeleteRuleProgressDialog.isShowing())
	        			mDeleteRuleProgressDialog.dismiss();
	        		mDeleteRuleProgressDialog = null;
	                Toast.makeText(mContext, R.string.rule_deleted, Toast.LENGTH_LONG).show();
	                closeFragment();
	        	}
	        	break; 
            }
        }
    };

    /**
     * The calling of this api through message handler is not being used currently
     * leaving the api if needed in future design change
     */
    private void fireActionsManuallyForSave(){
    	if(LOG_DEBUG) Log.d(TAG, "Moving from disabled to ready state");
    	// rule was disabled so enable it
    	long _id = mRuleController.getRuleModel().getRuleInstance().get_id();
    	if(LOG_DEBUG) Log.d(TAG, "toggleRuleStatus: Rule ID is "+_id);
    	
    	//first save the rule tuple
    	mRuleController.getRuleModel().updateExistingRule(mContext);
    	
        Cursor ruleCursor = null;
        try {
            String whereClause = Columns._ID + EQUALS + Q + _id + Q;
            ruleCursor = mContext.getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, whereClause, null, null);

            if(ruleCursor != null) {
                if(ruleCursor.moveToFirst()) {
                    int ruleType = ruleCursor.getInt(ruleCursor.getColumnIndex(Columns.RULE_TYPE));

                    if(ruleType == RuleTable.RuleType.AUTOMATIC) {
                    	String ruleKey = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.KEY));
                        String ruleName = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.NAME));
                        String ruleIcon = ruleCursor.getString(ruleCursor.getColumnIndex(Columns.ICON));
                    	//adding code to fire actions just as in case of Manual rules
                    	//rather than waiting for CB to do it
                        ActionPersistence.fireManualRuleActions(mContext, _id, ruleKey, ruleName);
                        ActionPersistence.updateActionTable(mContext, _id, false);                    	
                    	RulePersistence.updateDatabaseTables(mContext, _id, true, false);

                        int ruleIconResId = 0;
                        if(ruleIcon != null)
                            ruleIconResId = mContext.getResources().getIdentifier(ruleIcon, "drawable", mContext.getPackageName());
                        Util.sendMessageToNotificationManager(mContext, ruleIconResId);
                        if (mProgressDialog != null) mProgressDialog.dismiss();
                    }
                }
                else
                    Log.e(TAG, "ruleCursor.moveToFirst() for ruleID "+_id);
            }
            else
                Log.e(TAG, "ruleCursor is null for ruleID "+_id);
        } catch (Exception e) {
            Log.e(TAG, PROVIDER_CRASH+" for ruleID"+_id);
            e.printStackTrace();

        } finally {
            // Close the cursor
            if(ruleCursor != null && !ruleCursor.isClosed())
                ruleCursor.close();
            if (mProgressDialog != null) mProgressDialog.dismiss();    
        }
        //----- DELEGATE CALLBACK BEGIN
        Bundle result = new Bundle();
		result.putInt(LandingPageIntentExtras.RESULT_CODE, Activity.RESULT_OK);
		result.putLong(LandingPageIntentExtras.RULE_ID_INSERTED, mRuleController.getRuleModel().getRuleInstance().get_id());
		result.putBoolean(LandingPageIntentExtras.RULE_ICON_CHANGED, mRuleController.getRuleModel().getHasRuleIconChanged());        
        result.putBoolean(LandingPageIntentExtras.IS_RULE_MANUAL_RULE, mRuleController.getRuleModel().isManualRule());
		
		if (mRequestCode != -1) {
			result.putInt(LandingPageIntentExtras.REQUEST_CODE, mRequestCode);
		}
        mDelegate.setReturnResult(result);
        //----- DELEGATE CALLBACK END
        
        if(!mFirstTimeLocWiFiDialogShown ){
        	//finish();	//if its not this case  finish will be called by handler of Ok/Accept of that Dialog at showWiFiLocationCorrelationDialog
        	closeFragment();
        }
    }
  
    /*********************************************************
     *           Listeners
     *********************************************************/
    /** Sets up listeners for the Action and Trigger Add buttons*/
    private void setupActionAndTriggerButtons(final ActionPublisherList acList, final ConditionPublisherList cdList){
    	mAddActionButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	if(mFromWhereActivityWasInvoked == null){
            		mActivityRuleEditLeave = true;
            		if (LOG_DEBUG) Log.d (TAG,"Add action button: set mActivityRuleEditLeave true");
            	}
            	//TODO Remove this Hack by implementing Parcelable on PublisherList and send in Intent
            	DisplayActionsActivity.setPubCallback(mPubCallback);
	            Intent launchIntent = new Intent(mContext, DisplayActionsActivity.class);
	            launchIntent.putExtra(RULE_NAME_EXTRA, mRuleController.getRuleModel().getRuleInstance().getName());
	            startActivityForResult(launchIntent,PROCESS_ACTION_LIST_ITEM);
	       }
     	});
    	
    	mAddTriggerButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	if(mFromWhereActivityWasInvoked == null){
            		mActivityRuleEditLeave = true;
            		if (LOG_DEBUG) Log.d (TAG,"Add condition button: set mActivityRuleEditLeave true");
            	}
            	//TODO Remove this Hack by implementing Parcelable on PublisherList and send in Intent
            	DisplayConditionsActivity.setPubCallback(mPubCallback);
                Intent launchIntent = new Intent(mContext, DisplayConditionsActivity.class);
                launchIntent.putExtra(RULE_NAME_EXTRA, mRuleController.getRuleModel().getRuleInstance().getName());
                startActivityForResult(launchIntent, PROCESS_CONDITION_LIST_ITEM);
           }
     	}); 	
    }

    /**
     * For Saving an edited/loaded Rule
     * This calls addNewRuleToDB() if a new rule needs to be saved to DB or editExistingRuleFromDB()
     * if an existing rule needs to be modified.
     */
    private void saveRule(){
    	if (mEditRuleName.getVisibility() == View.VISIBLE) getRuleNameFromEditBox(); 
		if ((mRuleController.getRuleModel().isManualRule()) && (!mRuleController.getRuleModel().getQuickSaveOfActiveRuleFlag())){
	           showDialog(DialogId.DIALOG_MANUAL_RULE_CONFIRMATION_ID);
	    }else{
	        mRuleController.saveRule(false);
	    }

    }
    
    /*********************************************************
     *           Overrides
     *********************************************************/
   /**
     * This is to handle the keyboard open/ keyboard hidden configuration change in the Puzzle Builder.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig){
    	super.onConfigurationChanged(newConfig);
    }

    /** onDestroy()*/
    @Override
    public void onDestroy() {
    	if(LOG_DEBUG) Log.d(TAG, "In onDestroy()");
    	super.onDestroy();
        if (mProgressDialog != null){
        	mProgressDialog.dismiss();
			mProgressDialog = null;
		}
        
        if(mDeleteRuleProgressDialog != null) {
        	if(mDeleteRuleProgressDialog.isShowing())
        		mDeleteRuleProgressDialog.dismiss();
    		mDeleteRuleProgressDialog = null;
        }
        
        if(mRulesBuilderBroadcastReceiverRegistered) {
        	mContext.unregisterReceiver(mRulesBuilderIntentReceiver);
	        mRulesBuilderBroadcastReceiverRegistered = false;
	    }
        
        if (mShowRuleIconDialog != null){
        		mShowRuleIconDialog.dismiss();
        		mShowRuleIconDialog = null;
     	}
                
        dismissDialogs();
        releaseActivityResources();	//release all instances of view group.layout and their references 
   }
    
    /** onResume() */
    @Override
    public void onResume() {
        if(LOG_DEBUG) Log.d(TAG, "In onResume");
        super.onResume();

        //Refresh the rule status
        if(!mInEditMode) handleRefreshStatus();

        // Register to listen to the pending intent fired by VSM for updating the rule status.
        // Only register if the activity is launched for non-edit mode i.e. view the rule summary. Do not register 
        // when launched to create a rule from scratch or for copy rule or for preset or suggested rule.
        // Also the same should be applicable to the content observer that we register for DB updates.
        // NOTE: unregister for these only in onPause() or if we enter into edit mode (enterEditMode()).
        // We need not re-register for these once we are in Edit Mode as there is no way to go back into
        // summary mode, we can only save the rule and go back to Landing Page with the current design.
        if (!mInEditMode) {
            mContext.getContentResolver().registerContentObserver(Schema.RULE_TABLE_CONTENT_URI, true, myContentObserver);
        }
        
        // check from where we came into this activity
        // currently if we come from widget this activity needs to get finished
        // if the activity was paused for any outside interaction (Notification/History/IncomingCall/Home)
        mFromWhereActivityWasInvoked = getActivity().getComponentName();
        mActivityRuleEditLeave = false; //reset the flag
        
        if (!this.mActivityCurrentlyVisible){
	        if ((LocationConsent.getSecuritySettingsScreenLaunchedFlag()) )
	        	LocationConsent.onBackPressInSecuritySettings(mContext, mLastBlockViewSelected);
	        
	        if ((LocationConsent.getLocationPickerLaunchedFlag()) && ( mLastBlockViewSelected != null) && (mLastCondition != null))
	        	LocationConsent.onBackPressInLocationPicker(mContext, mLastBlockViewSelected, mLastCondition);

	    }
        this.mActivityCurrentlyVisible = true;
        mCondBlockController.setDoesTouchNeedsSupressed(false);
        
        //sets the MediaPlayer instances
        startThreadSetMediaPlayer();

        //suggesting that the receiver was registered before activity went out of scope and could be expecting, 
        // a broadcast, need to re-register
        if(mRulesBuilderBroadcastReceiverRegistered){	
        	IntentFilter intentToReceiveFilter = new IntentFilter(SAVE_INTENT_CALLBACK);
   	 		mContext.registerReceiver(mRulesBuilderIntentReceiver, intentToReceiveFilter, null, null);
        }
    }

    /** onPause()  */
    @Override
    public void onPause() {
        if(LOG_DEBUG) Log.d(TAG, "In onPause");
        super.onPause();
        mActivityPauseUserLeave = false;
        this.mActivityCurrentlyVisible = false;
        if(myContentObserver != null)
        	mContext.getContentResolver().unregisterContentObserver(myContentObserver);
        
        //Need to release all MediaPlayer instances
        if(null!=mInstMediaPlayer){
        	mInstMediaPlayer.releaseMediaPlayerInstances();
        	mInstMediaPlayer = null;
        }
    }
    
   /** Handles the onActivityResult for various request codes */
   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data)
   {
       super.onActivityResult(requestCode, resultCode, data);
       
       //This activity handles just RESULT_OK
       if (resultCode != Activity.RESULT_OK) {
    	   if(LOG_DEBUG) Log.d(TAG, "onActivityResult resultCode != RESULT_OK ");
    	   if (mProgressDialog != null){
       			mProgressDialog.dismiss();
       			mProgressDialog = null;
       		}
    	   //Add check if Block was not Disabled, AND Cancel was pressed on a Block with Error/non-configured/empty intent to fire
    	   //In which case we need to slide/animate block back to its disconnected state
    	   //mLastBlockViewSelected to be used and see if we can get setOnTouchListener/gestureListener
    	   //and then call animView on that
    	   if(null != mLastBlockViewSelected){
	           Publisher savedBlockInfo = (Publisher) mLastBlockViewSelected.getTag();
	           //TODO simplify the below composite conditional
	           if (	savedBlockInfo.getBlockConnectedStatus() &&
					  (Util.isNull(savedBlockInfo.getConfig()) || savedBlockInfo.getError())){
			        savedBlockInfo.getInstBlockGestureListener().setConnect(false);
			        savedBlockInfo.getInstBlockGestureListener().traceDetachAudPlayed = true;
			        savedBlockInfo.getInstBlockGestureListener().animFromDialogView();
	           }
    	   }
       }else {
    	   //resultCode is RESULT_OK
    	   if(LOG_DEBUG) Log.d(TAG, "onActivityResult resultCode == RESULT_OK ");
           switch (requestCode) {
                case CONFIGURE_ACTION_BLOCK: {
            	    if (data == null) {
            	    	Log.e(TAG, NULL_INTENT);
            	    } else {
            	    	if (data.getAction() != null && data.getAction().equals(Intent.ACTION_DELETE)) {
            	    		if (null != mLastBlockViewSelected) {
            	    			this.onDelete(mLastBlockViewSelected);
            	    		}
            	    	} else {
            	    		mRuleInteractCallback.configureActionInRule(data, mLastAction);
            	    	}
            	    }
           			break;
           		}
	            case CONFIGURE_CONDITION_BLOCK: {
	            	if (data == null) {
	            		Log.e(TAG, NULL_INTENT);
	            	} else {
	            		if (data.getAction() != null && data.getAction().equals(Intent.ACTION_DELETE)) {
            	    		if (null != mLastBlockViewSelected) {
            	    			this.onDelete(mLastBlockViewSelected);
            	    		}
            	    	} else {
            	    		mRuleInteractCallback.configureConditionInRule(data, mLastCondition);
            	    	}
	            	}
	                break;
	            }
	            case PROCESS_CONDITION_LIST_ITEM: {
	            	mRuleInteractCallback.addConditionPublisherInRule(data);
	            	break;
	            }
	            case PROCESS_ACTION_LIST_ITEM: {
	            	mRuleInteractCallback.addActionPublisherInRule(data);
	                break;
	            }
           }
           
           if(null != mLastBlockViewSelected){
	           Publisher savedBlockInfo = (Publisher) mLastBlockViewSelected.getTag();
	           if (	savedBlockInfo!=null && !savedBlockInfo.getBlockConnectedStatus()){	
	           //need to do this only if its in disconnected, else would get in loop
	           //This behavior would connect the Block automatically upon configuration
	           //as per latest specifications
	           if(LOG_DEBUG) Log.d(TAG, "onActivityResult resultCode == RESULT_OK and it was disabled");
			   savedBlockInfo.getInstBlockGestureListener().setConnect(true);
			   savedBlockInfo.getInstBlockGestureListener().animFromDialogView();
	           }

    	   }
           // remove the user instructions when at least one block has been added
           refreshUserInstructions();
       }
   }
    
   	/**
     * This is called when the back key is pressed by the user. A dialog will be displayed to the user
     * so that the user can save changes made in the puzzle builder.
     */
   	public void onBackPressed(){
   		handleCancel();
   	}

    /** Creates the options menu when pressing the hard 'menu' key.
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.rules_builder_menu, menu);
        
        inflater.inflate(R.menu.edit_menu, menu);
		menu.findItem(R.id.edit_add_button).setVisible(false);
		
		mMenu = menu;
    }
    
   	/** onOptionsItemSelected()
   	 *  handles the back press of icon in the ICS action bar.
   	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
			case android.R.id.home:
				// handle similar to cancel button.
				handleCancel();
				result = true;
				break;
			
			case R.id.edit_icon:
				mRuleIconClickProcessed = true;
	          	mRuleIconDialog = showRuleIconList();
	          	result = true;
	          	break;

			case R.id.rename:
				renameRule();
				result = true;
				break;

			case R.id.delete_rule:
				showDialog(DialogId.DIALOG_DELETE_RULE_ID);
				result = true;
				break;
				
			case R.id.edit_rb_save:
				processOnSaveOptionItemSelected();
            	result = true;
            	break;
            	
			case R.id.edit_cancel:
	            handleCancel();
				result = true;
				break;
		}
		
		return result;
	}
	
	/**
	 * Processes the needful to be done before starting to save/persist
	 * the changes to DB
	 */
	private void processOnSaveOptionItemSelected(){
		toggleSaveButtonStatus(false);
    	if(RulePersistence.isRuleActive(mContext, mRuleController.getRuleModel().getRuleInstance().get_id()) 
    			&& !mRuleController.getRuleModel().getQuickSaveOfActiveRuleFlag())
    		mRuleController.getRuleModel().setEditWhileActiveFlag(true);

    	ConditionInteractionModelList conditions = mRuleController.getRuleModel().getConditionList();
    	if(conditions.isParticularConditionPresent(LOCATION_TRIGGER)){
    		if( !LocationConsent.isWifiAutoscanSupported(mContext) &&
					Util.getSharedPrefStateValue(mContext, WIFI_LOCATION_WARNING_PREF, TAG)){
    			mFirstTimeLocWiFiDialogShown = true;
    			showDialog(DialogId.DIALOG_WIFI_AIRPLANE_LOC_CORRELATION_ID);
    			Util.setSharedPrefStateValue(mContext, WIFI_LOCATION_WARNING_PREF, TAG, false);
			}

			if( LocationConsent.isAirplaneModeOn(mContext)){
				LocationConsent.setAirplaneModeLocCorrelationDialogShownOnSave(true);
    			showDialog(DialogId.DIALOG_AIRPLANE_LOC_CORRELATION_ID);
    			return;
			}

    	}
    	//actual saving of Rule happens here
    	saveRule();
	}
	
	private void handleCancel() {
		
		// hide the keyboard if it is showing
		hideKeyboard();
		
		boolean showDiscardDialog = false;
		if(mRuleController.getRuleModel().isSaveButtonEnabled()
				&& (mPresetOrSuggestedEdited || mInEditMode )) {
			Log.e(TAG, "Need to show Discard Dialog");
			showDialog(DialogId.DIALOG_DISCARD_CHANGES_FOR_SAVE_ID);
			showDiscardDialog = true;
		}
		
		if(!showDiscardDialog) {
			Log.e(TAG, "No dicard dialog - just exit");
			if(mRuleController.getRuleModel().getRuleInstance().getSource() == RuleTable.Source.SUGGESTED) 				
           		RulesBuilderUtils.writeInfoToDebugTable(mContext, DebugTable.Direction.OUT, 
           				                                SMARTRULES_INTERNAL_DBG_MSG, mRuleController.getRuleModel().getRuleInstance().getName(), mRuleController.getRuleModel().getRuleInstance().getKey(), 
           				                                null, null, SUGG_REJECTED_DBG_MSG );
			onDiscardExit();
		}
		
	}
   
   /*********************************************************
    *           Miscellaneous 
    *********************************************************/
   /** Retrieves the rule name from the RuleName Edit Box*/
   private void getRuleNameFromEditBox(){
       /*Update with the latest Rule Name and Rule Icon*/
       String ruleName = mEditRuleName.getText().toString();
       if (ruleName.length() == 0){
           if (mRuleController.getRuleModel().getRuleInstance().get_id() == DEFAULT_RULE_ID)
               ruleName = mRuleController.getRuleModel().getConditionList().getFirstConditionName(mContext);
           else{
               Toast.makeText(mContext, getString(R.string.empty_rule_name), Toast.LENGTH_SHORT).show();
               ruleName = mRuleController.getRuleModel().getRuleInstance().getName(); 
           }
       }

       DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, 
                 null, ruleName, mRuleController.getRuleModel().getRuleInstance().getKey(), 
                 SMARTRULES_INTERNAL_DBG_MSG, null, 
                 Constants.RULE_RENAMED_STR, null,
                 Constants.PACKAGE, Constants.PACKAGE, null, null);
       mRuleController.getRuleModel().getRuleInstance().setName(ruleName);
   }
  
   /** Initializes some fields of the rule Tuple*/
   private void initializeExtraRuleTupleValues(){
   	//If the rule name in the DB is null, set it to a default value
       if (mRuleController.getRuleModel().getRuleInstance().getName() == null){
    	   mRuleController.getRuleModel().getRuleInstance().setName(UNNAMED_RULE);
       }
       
       if(mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID
    		   && !mRuleController.getRuleModel().getIsCopyFlag() 
    		   && !mRuleController.getRuleModel().getIsPresetOrSuggestedFlag()) {	
    	   if(LOG_DEBUG) Log.d(TAG, "Calling displayRuleStatus from onCreate");
    	   displayRuleStatus();

       }
   }
 
   /** Removes the User Instruction text from the Rules builder screen */
   private void removeUserInstructions(){
	   if (mRuleController.getRuleModel().getActionList().size() != 0 &&
			   mRuleController.getRuleModel().getActionList().isVisibleActionPresent()) empty_action.setVisibility(View.GONE);
	   if (mRuleController.getRuleModel().getConditionList().size() != 0 &&
			   mRuleController.getRuleModel().getConditionList().isVisibleConditionPresent()) empty_trigger.setVisibility(View.GONE);
   }
   
   /** User instruction text is shown on the screen if no blocks are present. If there is at least
    *  one action or trigger block, then the corresponding instruction is removed.
    * 
    */
   private void refreshUserInstructions(){
         int actionVisibility = 
             (Blocks.getListofActions(mActionBlockLayer).size() == 0) ? View.VISIBLE: View.GONE;
         empty_action.setVisibility(actionVisibility);
        
         int triggerVisibility = 
             (Blocks.getListofTriggers(mCondBlockLayer).size() == 0) ? View.VISIBLE: View.GONE;
         empty_trigger.setVisibility(triggerVisibility);
   }
   
   /** Adds the User Instruction text to the Rules Builder screen
    * 
    * @param blockLayout
    */
   private void addUserInstructions(BlockLayout blockLayout){
	   if(blockLayout.equals(mActionBlockLayer)){
		   if(Blocks.getListofActions(mActionBlockLayer).size() == 1) empty_action.setVisibility(View.VISIBLE);
       }
	   else if(blockLayout.equals(mCondBlockLayer)){
		   if(Blocks.getListofTriggers(mCondBlockLayer).size() == 1) empty_trigger.setVisibility(View.VISIBLE);
       }
   }
   
    /*********************************************************
     *           Dialogs
     *********************************************************/
   
   public interface DialogId {
       final int DIALOG_DELETE_RULE_ID = 1;
       final int DIALOG_DISCARD_CHANGES_FOR_SAVE_ID = 2 ;
       final int DIALOG_MANUAL_RULE_CONFIRMATION_ID = 3;
       final int DIALOG_LOCATION_CONSENT_ID = 4;
       final int DIALOG_LOCATION_WIFI_ENABLE_CONSENT_ID =5;
       final int DIALOG_WIFI_AIRPLANE_LOC_CORRELATION_ID = 6; //one time Dialog for educating the user
       final int DIALOG_AIRPLANE_LOC_CORRELATION_ID = 7; //for showing in Error state when Airplane mode is on
       final int DIALOG_LOC_WIFI_AUTOSCAN_CONSENT_ID = 8; //for Location consent regarding WiFi auto-scan
   }
   
   /** Creates and displays different dialogs based on the id that is passed in.
    * 
    */
   private void showDialog(int id) {
   	 	
   	switch(id) {
	    	case DialogId.DIALOG_DELETE_RULE_ID:
	    		showDeleteDialog(DialogId.DIALOG_DELETE_RULE_ID);
	    		break;
	    		
	    	case DialogId.DIALOG_DISCARD_CHANGES_FOR_SAVE_ID:
	    		showDiscardChangesForSaveDialog(DialogId.DIALOG_DISCARD_CHANGES_FOR_SAVE_ID);
	    		break;
	    		
	    	case DialogId.DIALOG_MANUAL_RULE_CONFIRMATION_ID:
	    		showManualRuleConfirmationDialog(DialogId.DIALOG_MANUAL_RULE_CONFIRMATION_ID);
	    		break;
	    		
	    	case DialogId.DIALOG_LOCATION_CONSENT_ID:
	    		LocationConsent.showLocationConsentDialog(mContext, mLastBlockViewSelected);
	       		break;
	       	
	    	case DialogId.DIALOG_LOCATION_WIFI_ENABLE_CONSENT_ID:
	    		LocationConsent.showWifiTurnOnDialog(mContext, mLastBlockViewSelected);
	    		break;
	    		 
	    	case DialogId.DIALOG_AIRPLANE_LOC_CORRELATION_ID:	
	    		showAirplaneModeLocationCorrelationDialog(DialogId.DIALOG_AIRPLANE_LOC_CORRELATION_ID);
	       		break;
	       		
	    	case DialogId.DIALOG_WIFI_AIRPLANE_LOC_CORRELATION_ID:	
	    		showWiFiLocationCorrelationDialog(DialogId.DIALOG_WIFI_AIRPLANE_LOC_CORRELATION_ID);
	       		break;
	       		
	    	case DialogId.DIALOG_LOC_WIFI_AUTOSCAN_CONSENT_ID:
	    		LocationConsent.showWiFiLocationAutoscanConsentDialog(mContext, mIntentForDialog, mLastBlockViewSelected);
	    		break;
   		}
	}
   
   /**
    * Creates and shows a dialog to the user when the user presses the delete menu option
    * @param id - Id for Delete.
    * @return
    */
   private void showDeleteDialog(int id) {
	    // hide keyboard before showing delete dialog
	    hideKeyboard();
	    
		DialogFragment frag = DeleteRuleDialogFragment.newInstance();
		frag.setTargetFragment(this, 0);
		frag.show(getFragmentManager(), DeleteRuleDialogFragment.TAG);
	}
   
   /** Delete Dialog callback
    */
   public void onDeleteRule() {
	   if(mRuleController.getRuleModel().getRuleInstance().getActive() == RuleTable.Active.ACTIVE) {
		   if(LOG_DEBUG) Log.d(TAG, "Start SmartRulesService to process the rule state change of active rule before deleting");
		   mDeleteRuleProgressDialog = ProgressDialog.show(mContext, "", mContext.getString(R.string.deleting_rule), true);
		   mHandler.sendEmptyMessageDelayed(HandlerMessage.RULE_DELETED, 4000);
		   // The rule will be deleted in the service - so that even if the user 
	   	   // accidentally hits the home key the rule is actually deleted
	   	   // and does not show up in Ready/Disabled state in the Landing Page
		   Intent serviceIntent = new Intent(mContext, SmartRulesService.class);
		   serviceIntent.putExtra(MM_RULE_KEY, mRuleController.getRuleModel().getRuleInstance().getKey());
		   serviceIntent.putExtra(MM_RULE_STATUS, FALSE);
		   serviceIntent.putExtra(MM_DELETE_RULE, true);
		   mContext.startService(serviceIntent);
	   } else {
		   if(LOG_DEBUG) Log.d(TAG, "Deleting a non-active auto/manual rule");
		   RulePersistence.deleteRule(mContext, mRuleController.getRuleModel().getRuleInstance().get_id(), mRuleController.getRuleModel().getRuleInstance().getName(),
	   							mRuleController.getRuleModel().getRuleInstance().getKey(), true);
	       Toast.makeText(mContext, R.string.rule_deleted, Toast.LENGTH_SHORT).show();
	       closeFragment();
	   }
   }

   /**
    * Creates and shows a dialog to the user when the user presses Cancel or the Back key
    * @param id - Id for Discard
    * @return
    */
   private void showDiscardChangesForSaveDialog(int id) {
  		DialogFragment frag = DiscardRuleDialogFragment.newInstance();
		frag.setTargetFragment(this, 0);
		frag.show(getFragmentManager(), DiscardRuleDialogFragment.TAG);
	}
   
    /** Save Dialog callback to save the rule
     */
	public void onSaveRule() {
	   ConditionInteractionModelList conditions = mRuleController.getRuleModel().getConditionList();
	   if(conditions.isParticularConditionPresent(LOCATION_TRIGGER)){
			if( !LocationConsent.isWifiAutoscanSupported(mContext) &&
				 Util.getSharedPrefStateValue(mContext, WIFI_LOCATION_WARNING_PREF, TAG)){
	   			mFirstTimeLocWiFiDialogShown = true;
	   			showDialog(DialogId.DIALOG_WIFI_AIRPLANE_LOC_CORRELATION_ID);
	   			Util.setSharedPrefStateValue(mContext, WIFI_LOCATION_WARNING_PREF, TAG, false);
			}
	
			if( LocationConsent.isAirplaneModeOn(mContext)){
				LocationConsent.setAirplaneModeLocCorrelationDialogShownOnSave(true);
	   			showDialog(DialogId.DIALOG_AIRPLANE_LOC_CORRELATION_ID);	           			
			} else{
				saveRule();
			}
	
		}else{
			saveRule();
		}            
	}
   
	/** Save Dialog callback to discard the rule
     */
	public void onDiscardRule() {
		Toast.makeText(mContext, R.string.changes_discarded, Toast.LENGTH_SHORT).show();
	    onDiscardExit();
	}
   
   /**
    * Creates and shows a dialog to the user when the user tries to save a rule that has no conditions
    * @param id - Id for Manual rule confirmation
    * @return
    */
	private void showManualRuleConfirmationDialog(int id) {
   		DialogFragment frag = ManualRuleDialogFragment.newInstance();
		frag.setTargetFragment(this, 0);
		frag.show(getFragmentManager(), ManualRuleDialogFragment.TAG);
   	}
	
	/** Manual Rule Dialog callback to save the rule
	 */
	public void onSaveManualRule() {
		mRuleController.saveRule(true);
	}
	
	/** Manual Rule Dialog callback to cancel the rule
	 */
	public void onCancelManualRule() {
   		//The Save button is disabled after the user clicks on it once, in order to prevent multiple clicks.
        //But after the Manual Confirmation dialog is canceled the user should be allowed to click on it again.
        toggleSaveButtonStatus(true);
        mInEditMode = true;
	}
   
   /**
    * Creates and shows a dialog to the user when the user tries to save a Loc based rule
    * @param id - Id for Location-Airplane mode correlation
    * @return
    */
	private void showWiFiLocationCorrelationDialog(int id) {
		DialogFragment frag = WiFiLocationDialogFragment.newInstance();
		frag.setTargetFragment(this, 0);
		frag.show(getFragmentManager(), WiFiLocationDialogFragment.TAG);
	}
	
	/** Disable WiFi Dialog callback
	 */
	public void onDisableWiFiDialog() {
		if (mFirstTimeLocWiFiDialogShown) {
			//this flag now needs to be reset
			mFirstTimeLocWiFiDialogShown = false; 
     	   	closeFragment(); 
        }
	}
   
   /**
    * Creates and shows a dialog to the user when the user tries to save a Loc based rule
    * @param id - Id for Location-Airplane mode correlation
    * @return
    */
	private void showAirplaneModeLocationCorrelationDialog(int id) {
		DialogFragment frag = AirplaneModeDialogFragment.newInstance();
		frag.setTargetFragment(this, 0);
		frag.show(getFragmentManager(), AirplaneModeDialogFragment.TAG);
	}
	
	/** Airplane Mode Dialog callback to turn off airplane mode
	 */
	public void onTurnOffAirplaneMode() {
    	// toggle airplane mode off
        Settings.System.putInt(
              getActivity().getContentResolver(),
              Settings.System.AIRPLANE_MODE_ON, AIRPLANE_MODE_STATUS.AIRPLANE_MODE_OFF.getStat());

        // Broadcast intent so that notification bar and other subscribers can know that its turned off 
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", AIRPLANE_MODE_STATUS.AIRPLANE_MODE_OFF.getStat());
        mContext.sendBroadcast(intent);
        
        if (LocationConsent.isAirplaneModeLocCorrelationDialogShownOnSave()) {           	   
     	   saveRule();	//in this case while save rule is called from here
     	   LocationConsent.setAirplaneModeLocCorrelationDialogShownOnSave(false);
        }
	}
	    
	/** Airplane Mode Dialog callback to cancel turn off airplane mode
	 */
	public void onCancelTurnOffAirplaneMode() {
		handleCancel();
		// in this case we again prompt user to consider saving,
		// which would again ask for turning Airplane mode off
		LocationConsent.setAirplaneModeLocCorrelationDialogShownOnSave(false);
	}

    /*********************************************************
     *           Block Edits
     *********************************************************/
   /** When a block has been tapped to re-configure*/
   public void onConfigure(View block) {
	   // Set flag which suggests us that this Activity does not need to be finished yet
	   if(mFromWhereActivityWasInvoked == null){
	   		mActivityRuleEditLeave = true;
	   }
	   invokeActivityToConfigure(block);
    }
   
    /** When a block has been deleted*/
	public void onDelete(View block) {
    	// Set flag which suggests us that this Activity does not need to be finished yet
    	if(mFromWhereActivityWasInvoked == null){
    		mActivityRuleEditLeave = true;
    	}
	     Publisher savedBlockInfo = (Publisher)block.getTag();
	     if (savedBlockInfo.isAction()){
	    	 mRuleInteractCallback.removeExistingActionFromRule(block);
	     }
	     else{
	    	 mRuleInteractCallback.removeExistingConditionFromRule(block);
	     }        
    }

    /** When a block has been connected or disconnected*/
	public void onConnected(View blockView, boolean connectStatus) {
    	//TODO split and move this into RuleController
    	if(LOG_DEBUG) Log.d (TAG,"onConnected "+ connectStatus);

        Publisher savedBlockInfo = (Publisher)blockView.getTag();
        savedBlockInfo.setBlockConnectedStatus(connectStatus);
        blockView.setTag(savedBlockInfo);
        
        if((connectStatus) && (savedBlockInfo.getSuggested())) {
		Blocks.removeStatus(blockView);
        	updateBlockSuggestionAcceptedStatus(blockView);
        }
        else if (Util.isNull(savedBlockInfo.getConfig())){
		if (connectStatus){
			Blocks.enableConfigurationRequiredStatus(mContext, blockView);
			invokeActivityToConfigure(blockView);
		}
		else{
			Blocks.disableConfigurationRequiredStatus(mContext, blockView);
		}
        }else if((connectStatus) && savedBlockInfo.getError()){
        	invokeActivityToConfigure(blockView);
        }

        updateBlockConnectedStatus(blockView);
        
        enterEditMode(true);
        if (mRuleController.getRuleModel().isValidRule()) 
        	toggleSaveButtonStatus(true);
        else 
        	toggleSaveButtonStatus(false);
    }
   
   
	 
	  /**This function is called when a block is clicked for configuration
	  *
	  * @param block - Block selected by the user on canvas
	  */
	 private void invokeActivityToConfigure(View blockView) {
	
	     Publisher savedBlockInfo = (Publisher)blockView.getTag();
	     if (savedBlockInfo.isAction()){
	    	 invokeActionActivityToConfigure(blockView);
	     }
	     else{
	    	 invokeConditionActivityToConfigure(blockView);
	     }
	     
	     
	 }
	 
	 
	 /** Invokes the Action Activity
	  * 
	  * @param actionBlockView
	  */
	 public void invokeActionActivityToConfigure(View actionBlockView){
		Publisher actionBlock = (Publisher)actionBlockView.getTag();
		String blockIntentUriString = actionBlock.getIntentUriString();
		if(LOG_DEBUG) Log.d(TAG, " actionBlock " + actionBlock);
		if(blockIntentUriString == null) {
			if(actionBlock.getValidity().equals(TableBase.Validity.UNAVAILABLE)
				&& actionBlock.getMarketUrl() != null){
				if(LOG_DEBUG) Log.d(TAG, " Download action publisher");
				// Launch the market app with market url to download publisher.
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(actionBlock.getMarketUrl()));
				startActivity(intent);
			} else {
				// Error scenario where validity is invalid and there is no marketUrl to launch Google
				// play store. Hence set the following to true, so that on return path,
				// in BlockGestureListener.onSingleTapUp, touch events are not suppressed.
				actionBlock.setNeedToPopDialogFromThisActivity(true);
			}

		} else {
			try{
				Intent myIntent = Intent.parseUri(blockIntentUriString, 0);
				//Store the corresponding action container for this block so that we can update the same container on activity return
				mLastAction = mRuleController.getRuleModel().getActionForBlockInstanceId(actionBlock.getBlockInstanceId());
			    mLastBlockViewSelected = actionBlockView;
			    if (mLastAction != null) {
		            //This extra is necessary so that the last user saved option is shown in the activity.
				    myIntent.putExtra(EXTRA_CONFIG, mLastAction.getAction().getConfig());
		            String publisherKey = mLastAction.getAction().getPublisherKey();
		            myIntent.putExtra(EXTRA_RULE_ENDS, mLastAction.getAction().isOnExitModeAction());
		            if ((publisherKey != null ) && (publisherKey.equals(LAUNCH_APP_PUBLISHER_KEY))){
		            	mProgressDialog = ProgressDialog.show(mContext, "",getString(R.string.please_wait), true);
		            	mProgressDialog.setCancelable(true);
		            }
			    }
			    startActivityForResult(myIntent, CONFIGURE_ACTION_BLOCK);
			} catch (URISyntaxException e) {
	         e.printStackTrace();
			}
		}
	 }
	 
	 /** Invokes the Condition Activity
	  * 
	  * @param conditionBlockView
	  */
	 public void invokeConditionActivityToConfigure(View conditionBlockView){
		Publisher conditionBlock = (Publisher)conditionBlockView.getTag();
		String blockIntentUriString = conditionBlock.getIntentUriString();
		if(LOG_DEBUG) Log.d(TAG, " conditionBlock " + conditionBlock);
		if(blockIntentUriString == null){
			if(conditionBlock.getValidity().equals(TableBase.Validity.UNAVAILABLE) &&
					conditionBlock.getMarketUrl() != null){
				if(LOG_DEBUG) Log.d(TAG, " Download condition publisher");
				// Launch the market app with market url to download publisher.
				Intent intent = new Intent(Intent.ACTION_VIEW,
			            Uri.parse(conditionBlock.getMarketUrl()));
				startActivity(intent);
			}else {
				// Error scenario where validity is invalid and there is no marketUrl to launch Google
				// play store. Hence set the following to true, so that on return path,
				// in BlockGestureListener.onSingleTapUp, touch events are not suppressed.
				conditionBlock.setNeedToPopDialogFromThisActivity(true);
			}
		} else {
			try{
				mIntentForDialog = Intent.parseUri(blockIntentUriString, 0);
				//Store the corresponding event container for this block so that we can update the same container on activity return
				//mLastEvent = mNewRule.getEvents().getEventContainerForBlockInstanceId(savedBlockInfo.blockInstanceId);

				if (mIntentForDialog!=null) {
					mIntentForDialog.putExtra(EXTRA_CONFIG, conditionBlock.getConfig());

					mLastCondition = mRuleController.getRuleModel().getConditionForBlockInstanceId(conditionBlock.getBlockInstanceId());
					mLastBlockViewSelected = conditionBlockView;

					if (conditionBlock.getPublisherKey().equals(LOCATION_TRIGGER)) {
						//if consent not available show the consent dialog
						if(!Util.isMotLocConsentAvailable(mContext)){
							LocationConsent.showLocationWiFiAutoscanDialog(mContext);
						}else{
							LocationConsent.startRelevantActivity(mContext, mIntentForDialog);
						}
					} else {
						startActivityForResult(mIntentForDialog, CONFIGURE_CONDITION_BLOCK);
					}
				}

			} catch (URISyntaxException e) {
	              e.printStackTrace();
	            }
		}
	 }
	 
	 /** Interface function for ILocationPicker
	  * 
	  */
	 public void invokeLocationPicker() {
		LocationConsent.setLocationPickerLaunchedFlag(true);
		startActivityForResult(LocationConsent.getIntent(), CONFIGURE_CONDITION_BLOCK);
	 }
		
	 /**This function sets the block connected status in the container
	  *
	  * @param block
	  * @param connectStatus
	  */
	 private void updateBlockConnectedStatus(View blockView) {
	     Publisher savedBlockInfo = (Publisher)blockView.getTag();
	     if (savedBlockInfo.isAction()) Blocks.updateActionBlockConnectStatus(mRuleController.getRuleModel(), blockView);
	     else Blocks.updateConditionBlockConnectStatus(mRuleController.getRuleModel(), blockView);
	
	 }
	 
	 /** Updates the Suggestion Accepted Status in the Rule class*/
	 private void updateBlockSuggestionAcceptedStatus(View block) {
	     Publisher savedBlockInfo = (Publisher)block.getTag();
	     if (savedBlockInfo.isAction()) Blocks.updateActionBlockSuggestionAcceptedStatus(mRuleController.getRuleModel(), block);
	     else  Blocks.updateConditionBlockSuggestionAcceptedStatus(mRuleController.getRuleModel(), block);
	 }
    
    /*************************************************************************
     *        Rule Icon and Title Bar
     ************************************************************************/
     
     /** Sets the rule icon image button listeners
      */
     private void setRuleIconListeners(){

    	   mRuleIconButton.setOnClickListener(new OnClickListener() {
    		   public void onClick(View v) {
    			   if(LOG_DEBUG) Log.d(TAG, "Titlebar Rule Icon Selected");
    			   if(mRuleIconClickProcessed) {
    				   Log.e(TAG, "Icon click is being processed - ignoring this click");
    		   		}else {
    		   			if(LOG_DEBUG) Log.d(TAG, "in title bar edit mode so handle the rule icon change");
    		   			mRuleIconClickProcessed = true;
    		   			mRuleIconDialog = showRuleIconList();
    		   		}
    		   	}            
    	   });
    	   mRuleIconButton.setOnLongClickListener(new OnLongClickListener() {
    		   	public boolean onLongClick(View v) {
	    		   	mRuleIconClickProcessed = true;
	    		   	mRuleIconDialog = showRuleIconList();
	    		   	return true;
    		   	}
    	   });
	}
    
     /** Sets the listeners for the Edit text field for rule name
      */
     private void setEditRuleNameListeners(){
     	//Remove the blinking cursor when DONE is pressed on the soft Keypad
         mEditRuleName.setOnEditorActionListener(new OnEditorActionListener() {
         	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
 				if (actionId == EditorInfo.IME_ACTION_DONE) {
 	                   mEditRuleName.setCursorVisible(false);
 	                }
 				return false;
 			}
         });
         
         //The blinking cursor should be visible when the user touches the edit box
         mEditRuleName.setOnClickListener(new OnClickListener() {
         	public void onClick(View v) {
         		mEditRuleName.setCursorVisible(true);
         	}
         });
         
         mEditRuleName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
             public void onFocusChange(View v, boolean hasFocus) {
            	InputMethodManager inputMethod = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (hasFocus) {
                   ((EditText) v).setCursorVisible(true);
                    if (inputMethod != null)
                        inputMethod.showSoftInput(v, 0); 
                } else {
                	if (inputMethod != null)
                		inputMethod.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
             }
         });
     }
     
     /** sets the listeners for the On/Off switch that is used to change the rule state.
      */
     private void setOnOffSwitchListeners() {	 
    	 mOnOffSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(mRuleController.getRuleModel().getRuleInstance().isAutomatic() && 
						! mRuleController.getRuleModel().getRuleInstance().isEnabled() &&
						(RulePersistence.getVisibleEnaAutoRulesCount(mContext) 
								>= MAX_VISIBLE_ENABLED_AUTOMATIC_RULES) &&
						 //Make sure it is a manual rule dynamically because
						 // there can be blacklisted blocks which are connected
					     // invalid blocks without market url
						 (!mRuleController.getRuleModel().isManualRule())) {
					// Rule is of type automatic (state is from disabled to ready
					// and when conditions are met the rule will be activated)
					// and the limit of enabled auto rules is hit
					if (LOG_DEBUG) Log.d(TAG, "Max visible auto enabled rules - " +
							"cannot enable anymore.");
					DialogUtil.showMaxVisibleEnaAutoRulesDialog(mContext);
				} else {				
					// Disable the switch so that user does not click it multiple times.
					// This is enabled once the refresh of the rule happens via the content
					// observer.
					mOnOffSwitch.setEnabled(false);
					handleOnOffSwitchClick();	
				}
			}
    	 });
     }
     
     /** Shows the title bar in an Edit case, which is defined by the user opening
      * up an already existing rule.
      */
     private void showTitleBarInUserEditMode() {
     	mRuleStatus.setText(getString(R.string.block_action_edit));
     	mRuleStatus.setTextColor(mContext.getResources().getColor(
     	                             R.color.second_line));
     	mOnOffSwitch.setEnabled(false);
     	setRuleIconInEditMode();
     }

	   
     /** Shows the title bar in the edit mode i.e. displays the edit text for the
     * rule name, changes the icon background to edit the rule icon and also
     * shows the save and cancel buttons in the title bar. This full edit mode
     * is needed for Create From Scratch/Copy/ Sample cases.
     */
     private void showTitleBarInScratchOrSampleMode() {
     	mViewRuleName.setVisibility(View.GONE);
     	mRuleStatus.setVisibility(View.GONE);
     	mEditRuleName.setVisibility(View.VISIBLE);
     	mEditRuleName.setText(mRuleController.getRuleModel().getRuleInstance().getName());
     	mOnOffSwitch.setVisibility(View.GONE);
     	setRuleIconInEditMode();
     }
	  	
    /** Shows the title bar in the edit mode i.e. displays the edit text for the
     * rule name, changes the icon background to edit the rule icon and also
     * shows the save and cancel buttons in the title bar.
     */
 	private void showTitleBarEditMode() {
        mViewRuleName.setVisibility(View.GONE);
 		mRuleStatus.setVisibility(View.GONE);
 		mEditRuleName.setVisibility(View.VISIBLE);
 		mEditRuleName.setText(mRuleController.getRuleModel().getRuleInstance().getName());
 		mOnOffSwitch.setVisibility(View.GONE);
 		setRuleIconInEditMode();
 	}

    /** handles the long click for the title bar.
     */
	public boolean onLongClick(View view) {
		
		boolean result = false;
		if(view.getId() == R.id.title_wrapper && view instanceof RelativeLayout) {
			if(LOG_DEBUG) Log.d(TAG, "The title bar edited moving to edit mode");
			renameRule();
			result = true;
		}
		return result;
	}
	
	/** constructs the layer drawable and sets the rule icon in the edit mode.
	 */
	private void setRuleIconInEditMode() {
		mIconShownInEditMode = true;
        Drawable[] layers = new Drawable[2];
        if(mRuleController.getRuleModel().getHasRuleIconChanged())
	        layers[0] = getResources().getDrawable(getResources()
                .getIdentifier(mRuleController.getRuleModel().getRuleInstance().getIcon(), "drawable", getActivity().getPackageName()));
        else
            layers[0] = mRuleController.getRuleModel().getRuleInstance().getIconDrawable(mContext);
        layers[1] = getResources().getDrawable(R.drawable.rule_icon_edit);
        LayerDrawable layerDrawable = new LayerDrawable(layers);
        mRuleIconButton.setImageDrawable(layerDrawable);
        mRuleIconButton.setAlpha(OPAQUE_APLHA_VALUE);
	}
	
	/**
     * This function gets called when the user selects an icon from the icon picker, either through the Menu->Edit Icon option
     * or Long press of the Rule Icon button. This will qualify for a "Quick Save" if the rule is Active. This means that the
     * regular process of de-activating the rule enabling it again will be bypassed because it is simply an icon change.
     */
    private void editIcon(){
    	//If mEditRuleWhileActive is true, it means that some thing else has been edited before the icon. Hence it will not
    	//qualify for the "Quick Save". Also this stands an exception for Manual rule, so flag should not be set
    	//adding mIsCreateFromScratch flag for use case First time create from scratch and user changes the icon, should not be quick save
    	//Adding the mIsInEditMode condition, without which upon editing rule and then changing icon, it may falsely do QuickSave
    	if (!mInEditMode && !mRuleController.getRuleModel().getEditWhileActiveFlag() 
    			&& (mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID)) 
    		mRuleController.getRuleModel().setQuickSaveOfActiveRuleFlag(true);
    	
        if ((!mInEditMode) && (mRuleController.getRuleModel().getRuleInstance().getEnabled() == RuleTable.Enabled.DISABLED))
        	mRuleIconButton.setAlpha(FIFTY_PERECENT_ALPHA_VALUE);
        
        if(mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID){
        	if (!mRuleController.getRuleModel().isValidRule()){
            	toggleSaveButtonStatus(false);
            }
            else{
            	mInEditMode = true;
            	toggleSaveButtonStatus(true);
            }
        }
    }

    /**
     * This function gets called when the Rename menu option is selected.
     */
    private void renameRule(){
    	if (mEditRuleName.getVisibility() != View.VISIBLE) showTitleBarEditMode();
        else mEditRuleName.clearFocus();
    	mHandler.sendEmptyMessageDelayed(HandlerMessage.REQ_FOCUS, 250);
    	//removed the entering of edit mode as we do not want to do in this case
    	if(mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID){
        	if (!mRuleController.getRuleModel().isValidRule()){
            	toggleSaveButtonStatus(false);
            }
            else{
            	toggleSaveButtonStatus(true);
            }
        }
    	if (!mInEditMode && !mRuleController.getRuleModel().getEditWhileActiveFlag() 
    			&& (mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID)) 
    		mRuleController.getRuleModel().setQuickSaveOfActiveRuleFlag(true);
    	
    	mInEditMode = true;
    }
    
    /**Displays the rule icon selection grid in an alert dialog
    *
    * @return
    */
   private AlertDialog showRuleIconList() {

       AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
       builder.setTitle(mContext.getString(R.string.rule_icons));
       createRuleIconGridView();
       builder.setView(mRuleIconGridView);
       builder.setNegativeButton(mContext.getString(R.string.cancel), new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int which) {
        	   mRuleIconClickProcessed = false;
           	   dialog.dismiss();
           }
       });
       builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
    	   public void onCancel(DialogInterface dialog) {
    		   // User pressed the back key so the dialog is dismissed and hence
    		   // the icon click can be processed again reset the flag
    		   mRuleIconClickProcessed = false;
    	   }  
       });
       builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
    	   public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
    		   if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
    			   // User pressed the search button so the dialog is dismissed and hence
    		   	   // the icon click can be processed again reset the flag
    			   mRuleIconClickProcessed = false;                   
    		   }
    		   return false; // Any other keys are still processed as normal
    	   }
       });
       
       mShowRuleIconDialog =builder.create();
       mShowRuleIconDialog.show();
       return mShowRuleIconDialog;
   }

   /**Creates the Rule icon grid
    *
    */
   private void createRuleIconGridView() {

       mRuleIconGridView = new GridView(mContext);
       
       LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
       View view = inflater.inflate(R.layout.icon_grid, null);
       mRuleIconGridView = (GridView) view.findViewById(R.id.gridview);

       mRuleIconGridView.setHorizontalSpacing(GRID_HORIZONTAL_SPACING);
       mRuleIconGridView.setVerticalSpacing(GRID_VERTICAL_SPACING);
       mRuleIconGridView.setGravity(GRID_GRAVITY);
       mRuleIconGridView.setNumColumns(GRID_NO_OF_COLS);
       mRuleIconGridView.setStretchMode(GRID_STRETCH_MODE);
       mRuleIconGridView.setPadding(27, 27, 27, 27);
       mRuleIconGridView.setAdapter(new DisplayRuleIconGrid.ImageAdapter(mContext));

       mRuleIconGridView.setOnItemClickListener(new OnItemClickListener() {

           public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

               mRuleIconClickProcessed = false;
        	   if(!mRuleController.getRuleModel().getRuleInstance().getIcon().equals(DisplayRuleIconGrid.ruleIconPath[position])) {
        		   mRuleController.getRuleModel().getRuleInstance().setIcon(DisplayRuleIconGrid.ruleIconPath[position]);
        		   mRuleController.getRuleModel().setHasRuleIconChanged(true);
	               if(mIconShownInEditMode)
	            	   setRuleIconInEditMode();
	               else
	            	   mRuleIconButton.setImageResource(
                               getResources().getIdentifier(mRuleController.getRuleModel().getRuleInstance().getIcon(),"drawable", getActivity().getPackageName()));
	               editIcon();
        	   }
               if (mRuleIconDialog != null) 
            	   mRuleIconDialog.dismiss();
           }
       });
   }
	
   /********************************************************************
	 *           Dynamic Rule status change
	 ********************************************************************/
  
	/** displays the rule status below the rule name and also the background
	 * behind the rule icon.
	 */
	public void displayRuleStatus() {

		// reset this boolean. This part of the code would come only if the rule status
		// has changed while viewing the rule or when the user initially enters this screen.
		mRuleIconClickProcessed = false;
		
		boolean enabled = mRuleController.getRuleModel().getRuleInstance().getEnabled() == RuleTable.Enabled.ENABLED;
		boolean active = mRuleController.getRuleModel().getRuleInstance().getActive() == RuleTable.Active.ACTIVE;

       if(mIconShownInEditMode)
    	   setRuleIconInEditMode();
       else
           mRuleIconButton.setImageDrawable(mRuleController.getRuleModel().getRuleInstance().getIconDrawable(mContext));

       // Disable the checked state change listener so that the listener does not
       // get called when the setChecked() is called to indicate the current state
       // of the OnOff Switch.
		mOnOffSwitch.setOnCheckedChangeListener(null);
		
		if (enabled) {
			mRuleIconButton.setAlpha(OPAQUE_APLHA_VALUE);
			mOnOffSwitch.setChecked(true);
			if (active) {
				String ruleStatusText = mContext.getString(R.string.active);
				mRuleStatus.setText(ruleStatusText);
				mRuleStatus.setTextColor(mContext.getResources().getColor(
						R.color.active_blue));
			} else {
				// The last inactive date time was when the rule was last active i.e. 
				// moved from Active to Ready state. So need to display that the
				// rule was last active at this timestamp.
				long lastActiveTime = mRuleController.getRuleModel().getRuleInstance().getLastInactiveDateTime();
				String ruleStatusText = null;
				if (lastActiveTime > 0) {
					// Use the date/time field by mixing relative and
					// absolute times.
					int flags = DateUtils.FORMAT_ABBREV_RELATIVE;
					ruleStatusText = getString(R.string.last_active)
							+ DateUtils.getRelativeTimeSpanString(lastActiveTime,
									System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, 
									flags);
				} else {
					ruleStatusText = getString(R.string.ready);
				}
				mRuleStatus.setText(ruleStatusText);
				mRuleStatus.setTextColor(mContext.getResources().getColor(
						R.color.second_line));
			}
		} else {
			mOnOffSwitch.setChecked(false);
			mRuleIconButton.setAlpha(FIFTY_PERECENT_ALPHA_VALUE);
			mRuleStatus.setText(R.string.empty_string);
			mRuleStatus.setVisibility(View.INVISIBLE);
		}
		displayStatusForAllVisibleBlocks(mRuleController.getRuleModel().getRuleInstance().getActive());
		mRuleStatus.setVisibility(View.VISIBLE);

		// Set the on/off switch listener only after the state for the rule i.e. Active
	   	// or Ready or Disabled is set based on the rule state and also the On Off Switch
		// state is set via the setChecked() call to avoid a continuous loop of onChecked
		// listener getting called due to state change.
		setOnOffSwitchListeners();
	}
  
	/** This function goes through all the list of visible trigger and action
	 * blocks on the puzzle builder screen and set the status light. As per the
	 * current CXD design, the blocks should be lit up with
	 * status_indicator_active (blue light) when the trigger/action representing
	 * the block is Active and Connected. There might be cases, like in some
	 * sample rules, where some blocks are not configured. In those cases, the
	 * status light should be set to status_indicator_suggestion (green light)
	 * 
	 * @param ruleActiveFlag
	 */
	private void displayStatusForAllVisibleBlocks(int ruleActiveFlag) {
		if (mCondBlockLayer != null)
			Blocks.displayStatusForAllConditionBlocks(mContext, mCondBlockLayer, ruleActiveFlag);
		if (mActionBlockLayer != null)
			Blocks.displayStatusForAllActionBlocks(mContext, mActionBlockLayer, ruleActiveFlag);

	}
	
	/** Handles the user selection of the On/Off switch in the rule summary mode.
     */
	private void handleOnOffSwitchClick() {
		boolean enabled = mRuleController.getRuleModel().getRuleInstance().getEnabled() 
								== RuleTable.Enabled.ENABLED;
		boolean active = mRuleController.getRuleModel().getRuleInstance().getActive() 
								== RuleTable.Active.ACTIVE;
		boolean isAutomatic = ((mRuleController.getRuleModel().getRuleInstance().getRuleType() 
								== RuleTable.RuleType.AUTOMATIC) && 
								(!mRuleController.getRuleModel().isManualRule()));		
		int displayTextResId = 0;
		String debugString = null;

		Intent serviceIntent = new Intent(mContext, SmartRulesService.class);
		serviceIntent.putExtra(MM_RULE_KEY, 
							mRuleController.getRuleModel().getRuleInstance().getKey());

		mProgressDialog = ProgressDialog.show(mContext, "",
				getString(R.string.changin_rule_state), true);
		if (enabled) { // Rule is enabled
			displayTextResId = R.string.shutting_down;
			if (active) {
				// Rule is active (state is from active to disabled state for
				// manual and automatic rules)
				if (LOG_DEBUG) Log.d(TAG, "Rule Icon Active -> Disable " +
						"Setting editWhileActive flag FALSE");

				// this flag guards for setEditWhileActiveFlag which should not
				// be toggled while rule state is in transition
				// for now this is guarded by the progress bar, but this flag
				// will be a lock to prevent any issue
				// Changing anything here would have impact on use case toggling
				// rule status of active rule and further edits.
				mLockedForFlagEdit = true;
				
				serviceIntent.putExtra(MM_RULE_STATUS, FALSE);
				serviceIntent.putExtra(MM_DISABLE_RULE, true);
				if(isAutomatic)
					debugString = AUTO_ACTIVE_TO_DISABLED;
				else
					debugString = MAN_ACTIVE_TO_DISABLED;
				    
			} else { // Rule is enabled but inactive (Automatic)
				// Automatic rule moving from Ready to Disabled State
				serviceIntent.putExtra(MM_DISABLE_RULE, true);
				debugString = AUTO_READY_TO_DISABLED;
			}
		} else { // Rule is disabled
			displayTextResId = R.string.starting_up;

			if(isAutomatic) {
				// Automatic rule moving from Disabled to Ready State
				serviceIntent.putExtra(MM_ENABLE_RULE, true);
				debugString = AUTO_DISABLED_TO_READY;
			} else {
				// Manual rule moving from Disabled to Active state
				serviceIntent.putExtra(MM_RULE_STATUS, TRUE);
				debugString = MAN_DISABLED_TO_ACTIVE;
			}
		}

		if(debugString != null)
				RulesBuilderUtils.writeInfoToDebugTable(mContext, DebugTable.Direction.OUT, null, 
						mRuleController.getRuleModel().getRuleInstance().getName() ,
						mRuleController.getRuleModel().getRuleInstance().getKey(), 
						SMARTRULES_INTERNAL_DBG_MSG, null, debugString);
		
		mRuleStatus.setText(displayTextResId);
		mContext.startService(serviceIntent);
	}
		
	/** Refreshes the rule status  */
    private void handleRefreshStatus(){
    	// Short circuit this method if fragment is no longer displayed.
    	if (!isVisible()) return;

    	if(LOG_DEBUG) Log.d(TAG, "Refreshing the rule status");
    	// Re-enable the on off switch
    	mOnOffSwitch.setEnabled(true);
    	if(mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID) {
            String whereClause = RuleTable.Columns._ID +  EQUALS + Q + mRuleController.getRuleModel().getRuleInstance().get_id() + Q;
            Cursor cursor = getActivity().getContentResolver().query(Schema.RULE_TABLE_CONTENT_URI, null, whereClause, null,  null);
            if(cursor != null) {
            	if(cursor.moveToFirst()) {
            		mRuleController.getRuleModel().getRuleInstance().setEnabled(cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.ENABLED)));
            		mRuleController.getRuleModel().getRuleInstance().setActive(cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.ACTIVE)));
            		mRuleController.getRuleModel().getRuleInstance().setLastInactiveDateTime(cursor.getLong(cursor.getColumnIndex(RuleTable.Columns.LAST_INACTIVE_DATE_TIME)));
            		if(LOG_DEBUG) Log.d(TAG, "mRuleEnabled = "+mRuleController.getRuleModel().getRuleInstance().getEnabled()+"; mRuleActive = "+mRuleController.getRuleModel().getRuleInstance().getActive());
             		if(mInEditMode) {
            			// In edit mode so only update the status of the blocks where
            			// applicable. Do no update the title bar area.
            			displayStatusForAllVisibleBlocks(mRuleController.getRuleModel().getRuleInstance().getActive());
            		}
            		else {
            			// Not in edit mode so can update the block status as well as the
            			// title bar area.
            			displayRuleStatus();
            		}
            	}
            }
            
            if(cursor != null && !cursor.isClosed())
            	cursor.close();
            // need to toggle the setEditWhileActiveFlag to update latest status
            if(LOG_DEBUG) Log.d(TAG,"handleRefreshStatus isLockedForFlagEdit: "+mLockedForFlagEdit+" mIsInEditMode: "+mInEditMode);
            //if the dialog is set for changing state from rule icon, refer to handleRuleIconClick enabled case
            if (mProgressDialog != null) 
            	mProgressDialog.dismiss();    
            mLockedForFlagEdit = false; 
    	}
    }
    
    /** Handles an edit while active case
     */
    private void handleEditActiveRule(){
       	if(LOG_DEBUG) Log.d(TAG, "Saving an active rule");
    	if (mProgressDialog != null) mProgressDialog.dismiss();
    	//Unregistering the BroadcastReceiver for listening back from ConditionBuilder
        if(mRulesBuilderBroadcastReceiverRegistered) {
            mContext.unregisterReceiver(mRulesBuilderIntentReceiver);
            mRulesBuilderBroadcastReceiverRegistered = false;
        }
        mRuleController.editActiveRule();
    }
    
    /** Handle request focus*/
    private void handleRequestFocus(){
    	mEditRuleName.requestFocus();
    }
	
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
	        	if(localHandler.hasMessages(HandlerMessage.REFRESH_STATUS)) {
	        		localHandler.removeMessages(HandlerMessage.REFRESH_STATUS);
	        	}
	        	localHandler.sendEmptyMessageDelayed(HandlerMessage.REFRESH_STATUS, 1000);
	        }

	    }
   }
   
   /** Unregisters the content observer*/
   private void unregisterContentObserver(){
       if(myContentObserver != null)
               mContext.getContentResolver().unregisterContentObserver(myContentObserver);
   }
   
   private MyContentObserver myContentObserver = new MyContentObserver(mHandler);
   
   /*****************************************************************************
    *    UI Related methods
    *****************************************************************************/
   
   /** OnClickListener needs to be not-null for some system properties such as
    *  Audible selection Sounds.
    */
   private void setupOnClickListener() {
	   mThisOnClickListener = new OnClickListener(){
		   public void onClick(View v) {
				// Just a dummy onClickListener, not using it for now
			}
       };
   }

   /** Sets the Gesture callback for all the visible blocks*/
   private void setGestureCallbackForVisibleBlocks(){
		int numConditions = mCondBlockLayer.getChildCount();
		for(int i=0; i< numConditions; i++){
			View blockView = (View)mCondBlockLayer.getChildAt(i);
			setAddedBlockGestureCallback(blockView);
		}
		
		int numActions = mActionBlockLayer.getChildCount();
		for(int i=0; i< numActions; i++){
			View blockView = (View)mActionBlockLayer.getChildAt(i);
			setAddedBlockGestureCallback(blockView);
		}
	}
   
   /**Set the Gesture listener for the blocks
    * @param gestureView
    */
   private void setAddedBlockGestureCallback(View gestureView){
   	
   	final GestureDetector gestureDetector;
   	final BlockGestureListener blockGestureListener = 
			new BlockGestureListener(gestureView,
					mCondBlockController, mActionBlockController,
					mCondBlockLayer, mActionBlockLayer,
					this, mContext);
   	
       View.OnTouchListener gestureListener;
       gestureDetector = new GestureDetector(mContext, blockGestureListener);
       gestureListener = new View.OnTouchListener() {
    	   public boolean onTouch(View v, MotionEvent event) {
           	if (gestureDetector.onTouchEvent(event)) {
           		if (LOG_DEBUG) Log.d (TAG, "Curious whats happening here ...!! : "+ event.getAction());
                  return true;
               }
           	   
               if( event.getAction() == MotionEvent.ACTION_UP && ! mLongPressed && !mSingleTap) {
                   if (LOG_DEBUG) Log.d (TAG, "Possible End of Scroll, Anim Begins now");
                   blockGestureListener.animView();
               }
               if( event.getAction() == MotionEvent.ACTION_UP && mLongPressed){
            	   mLongPressed = false;
               }
               if( event.getAction() == MotionEvent.ACTION_UP && mSingleTap){
            	   mSingleTap = false;
            	   if (LOG_DEBUG) Log.d (TAG, "isSingleTap gets here ...!! : "+ event.getAction());            	   
               }
               
               return false;
           }
       };
       gestureView.setOnTouchListener(gestureListener);
       
       //With new requirements the Blocks and Animations for Enable/Disable needs to be triggered depending
       //upon inter-Activity interactions, hence we need to have an instance of the gestureListener to trigger the animations
       Publisher savedRuleInfo = (Publisher) gestureView.getTag();
       savedRuleInfo.setInstBlockGestureListener(blockGestureListener);
       //set the connect flag instance appropriately while initializing the BlockGestureListener callback 
       savedRuleInfo.getInstBlockGestureListener().setConnect(!savedRuleInfo.isDisabled());
       
   }

    /** IBlocksMotionAndAnim interface method*/
	public void setPressedState(boolean pressedState) {
		mLongPressed = pressedState;
	}
	
	/** IBlocksMotionAndAnim interface method*/
	public void setTapState(boolean tapState) {
		mSingleTap = tapState;
	}
	
	/** 
	  * This forks a thread for setting the MediaPlayer instances
	  */
	    private void startThreadSetMediaPlayer()
	    {
	        Thread thread = new Thread() {
	        		public void run() {
	        			try {
	        				setInstMediaPlayer();
	        	   		} catch (Exception e) {
	        	   			Log.e(TAG, "Exception while initializing MediaPlayer ");
	        	   			e.printStackTrace();
	        	   		} 
	        		}
	        	};
	        	thread.setPriority(Thread.NORM_PRIORITY-1);
	        	thread.start();
	    }
	
	/**
	 * @param mInstMediaPlayer the mInstMediaPlayer to set
	 * One time instantiation of the MediaPlayer instance
	 * for life cycle of this Activity.
	 * This needs to be called while loading Rule and register/deregister in
	 * onPause/onResume
	 * and corresponding api in onDestroy to release the MediaPlayer inst
	 */
	private void setInstMediaPlayer() {
		//BlocksMediaPlayer has Singleton instance of MediaPlayer types
		this.mInstMediaPlayer =  SingletonBlocksMediaPlayer.getBlocksMediaPlayerInst(mContext);
	}

	/**
	 * @return the mInstMediaPlayer
	 */
	public SingletonBlocksMediaPlayer getInstMediaPlayer() {
		return mInstMediaPlayer;
	}
	
	/**
	 * All calls to toggle save button status would be made through this function
	 * We also can use this to track and implement locks for this change
	 * 
	 * @param toEnabled
	 */
	private void toggleSaveButtonStatus(boolean toEnabled){
		if (LOG_DEBUG) Log.d (TAG,"toggleSaveButtonStatus  toEnabled: "+ toEnabled);
		
		mRuleController.getRuleModel().setSaveButtonEnabled(toEnabled);
		mMenuGroupVisibility = toEnabled;
		getActivity().invalidateOptionsMenu();
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		setMenuGroupsVisibility(menu);
	}
	
	private boolean mMenuGroupVisibility = false;
	private void setMenuGroupsVisibility(Menu menu) {
		if (mMenuGroupVisibility) {
			mMenu.findItem(R.id.edit_add_button).setVisible(false);
			mMenu.findItem(R.id.edit_cancel).setVisible(true);
			mMenu.findItem(R.id.edit_rb_save).setVisible(true).setEnabled(true);
		} else {
			mMenu.findItem(R.id.edit_add_button).setVisible(false);
			mMenu.findItem(R.id.edit_cancel).setVisible(true);
			mMenu.findItem(R.id.edit_rb_save).setVisible(true).setEnabled(false);
		}
	}
	
	/**
	 * Release the BlockLayout instances, for GC to reclaim objects when gets chance to run
	 * Should be called from onDestroy. Later should be tried to implement even in onPause
	 * 
	 */
	private void releaseActivityResources(){
		if (LOG_DEBUG) Log.d (TAG,"releaseActivityResources: Activity resources released");
		if(mCondBlockController != null) {
	        if(mCondBlockLayer != null) {
		        mCondBlockController.removeDropTarget(mCondBlockLayer);
		        mCondBlockLayer.setDragController(null);
		        mCondBlockLayer.setBlockLayerCallback(null);
		        mCondBlockLayer = null;
	        }
	        mCondBlockController = null;
		}
		if(mActionBlockController != null) {
	        if(mActionBlockLayer != null) {
		        mActionBlockController.removeDropTarget(mActionBlockLayer);
		        mActionBlockLayer.setBlockLayerCallback(null);
		        mActionBlockLayer.setDragController(null);
		        mActionBlockLayer = null;
	        }
	        mActionBlockController = null;
		}
	}
	
	/** 
	 * Initialize the Graphical interfaces, should be called from onCreate
	 */
	private void createActivityResources(){
		if (LOG_DEBUG) Log.d (TAG,"createActivityResources: Activity resources created");
		View contentView = getView();
		mCondBlockController = new BlockController(mContext);
        mActionBlockController = new BlockController(mContext);
		mCondBlockLayer = (BlockLayout) contentView.findViewById(R.id.pc_drag_layer);
        mCondBlockLayer.setDragController(mCondBlockController);
        mCondBlockLayer.setTag("pc");
        mCondBlockController.addDropTarget (mCondBlockLayer);
        mCondBlockLayer.setBlockLayerCallback(this);
        mActionBlockLayer = (BlockLayout) contentView.findViewById(R.id.action_drag_layer);
        mActionBlockLayer.setDragController(mActionBlockController);
        mCondBlockLayer.setTag("action");
        mActionBlockController.addDropTarget (mActionBlockLayer);
        mActionBlockLayer.setBlockLayerCallback(this);        
	}

	/**
	 * dismiss the dialogs from BlocksGestureListener, for all Blocks
	 * required to avoid any window leaks resulting from Dialog when
	 * activity goes to onPause or onDestroy
	 */
	private void dismissDialogs(){
		if (LOG_DEBUG) Log.d (TAG,"dismissDialogs");
		View blockView = null;
		if(mCondBlockLayer != null) {
			ArrayList<View> triggerList = Blocks.getListofTriggers(mCondBlockLayer);
			for(int i=0; i< mCondBlockLayer.getChildCount(); i++){		
	    	    blockView = triggerList.get(i);
	    	    Publisher savedRuleInfo = (Publisher) blockView.getTag();
	    	    savedRuleInfo.getInstBlockGestureListener().dimissDialog();
	    	    blockView.setOnTouchListener(null);
			}
		}
		if(mActionBlockLayer != null) {
			ArrayList<View> actionList = Blocks.getListofActions(mActionBlockLayer);
			for(int i=0; i< mActionBlockLayer.getChildCount(); i++){		
	    	    blockView = actionList.get(i);
	    	    Publisher savedRuleInfo = (Publisher) blockView.getTag();
	    	    savedRuleInfo.getInstBlockGestureListener().dimissDialog();
	    	    blockView.setOnTouchListener(null);
			}
		}
	}
	
	/**
	 * This would take care of logic behind how we go back to previous activity
	 * after this activity gets finished on discard / cancel
	 * If we came here from LandingpageActivity, we just need to set result and finish.
	 * If we came here NOT from LandingPage (from a widget) we need to go to LandingPage
	 * by setting the explicit Intent.
	 * 
	 * previous activity which launched this
	 */
	private void onDiscardExit(){
		if(mFromWhereActivityWasInvoked!= null){
			//if we came to RulesBuilder from 
			if(LOG_DEBUG) Log.d(TAG, "handleCancel: Came from Landing page, and heading back there");
			
			Bundle result = new Bundle();
			result.putInt(LandingPageIntentExtras.RESULT_CODE, Activity.RESULT_CANCELED);
			result.putInt(LandingPageIntentExtras.RULE_ID_INSERTED, DEFAULT_RULE_ID);
			if (mRequestCode != -1) {
				result.putInt(LandingPageIntentExtras.REQUEST_CODE, mRequestCode);
			}
            mDelegate.setReturnResult(result);
            
		}else{
			//in case we did not come here from LandingPage, we need to explicitly take us there
			if(LOG_DEBUG) Log.d(TAG, "handleCancel: Did not Came from Landing page, still heading back there");
		}
		closeFragment();
	}
	
	
	/**
	 * Create all the static inflation of View hierarchy
	 *  
	 */
	private View createAndInflateViewHierarchy(LayoutInflater inflater, ViewGroup container) {
		
		View contentView = inflater.inflate(R.layout.rb_bottom_slider, container, false);     
 		
        RelativeLayout titleWrapper = (RelativeLayout) contentView.findViewById(R.id.title_wrapper);
        titleWrapper.setOnLongClickListener(this);
        
        mRuleIconButton = (ImageButton) contentView.findViewById(R.id.rule_icon);
        mViewRuleName = (TextView) contentView.findViewById(R.id.title_line);
        mEditRuleName = (EditText) contentView.findViewById(R.id.edit_rule_name);
        setEditRuleNameListeners();
        
    	mRuleStatus = (TextView) contentView.findViewById(R.id.description_line);
        setRuleIconListeners();
        
        mOnOffSwitch = (Switch) contentView.findViewById(R.id.on_off_switch);
        mAddActionButton = (Button) contentView.findViewById(R.id.add_action_button);
        mAddTriggerButton = (Button) contentView.findViewById(R.id.add_trigger_button);
        empty_trigger = (TextView) contentView.findViewById(R.id.empty_trigger);
        empty_action = (TextView) contentView.findViewById(R.id.empty_action);
        
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setupOnClickListener();
        return contentView; 
	}

	/**
	 * Callback function implemented from RuleController for loading generic views
	 */
	public void loadGenericRuleViews() {
		// Inflate and create the static view hierarchical tree 
        //createAndInflateViewHierarchy();
		if (mViewRuleName.getVisibility() == View.VISIBLE)  mViewRuleName.setText(mRuleController.getRuleModel().getRuleInstance().getName());
	    if (mEditRuleName.getVisibility() == View.VISIBLE) mEditRuleName.setText(mRuleController.getRuleModel().getRuleInstance().getName());
		mRuleController.getRuleModel().setIsPresetOrSuggestedFlag();
	}

	/**
	 * Callback function implemented from RuleController for loading Plugin UI views
	 */
	public void loadPluginRuleViews(RuleInteractionModel ruleModel, 
										ActionPublisherList acList, ConditionPublisherList cdList) {

		if (mRuleController.getRuleModel() != null) 
        	displayRulesBuilderScreen();
        else{
        	Log.e(TAG, "Rule instance is null - finishing activity");
        	closeFragment();
        }
		
        setupActionAndTriggerButtons(acList, cdList);
	    removeUserInstructions();
		createActivityResources();
		
		initializeExtraRuleTupleValues();
		if(acList != null || cdList != null){
	    	Blocks.displayActionBlocks(mContext, mRuleController.getRuleModel(), acList,
	    			mCondBlockController, mActionBlockController,
					mCondBlockLayer, mActionBlockLayer, mThisOnClickListener );
	    	Blocks.displayConditionBlocks(mContext,mRuleController.getRuleModel(), cdList,
	    			mCondBlockController, mActionBlockController,
					mCondBlockLayer, mActionBlockLayer, mThisOnClickListener);
	    	setGestureCallbackForVisibleBlocks();
		}
        
        //Audible changes, related to audio played on moving of blocks
        //this would change the default vol up/down key behavior from Ringer to Media
        // which is required we are introducing sound effects on some UI elements 
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);	 
        //set instances of MediaPlayer which would live only if activity is visible
        //that would mean keeping the instances only between onCreate and onPause/onDestroy
        //and to re-instantiate upon entering onResume/onCreate
        startThreadSetMediaPlayer();
	}


	public void enterEditMode(boolean userEdited) {
    	if(LOG_DEBUG)Log.d(TAG,"enterEditMode flag userEdited: "+userEdited);
    	mInEditMode = true; 
    	mRuleController.getRuleModel().setQuickSaveOfActiveRuleFlag(false);
    	// set the correct flag if any of the conditions is being changed/touched
    	//If the rule was enabled/disabled from this Activity/from Rule icon
    	//and later the rule was edited, and saved before the DB operation was over
    	//then this needs to be taken care of ...
    	if(!mLockedForFlagEdit){
    		if (mRuleController.getRuleModel().getRuleInstance().getActive() 
    				== RuleTable.Active.ACTIVE) 
    			mRuleController.getRuleModel().setEditWhileActiveFlag(true);
    	}

    	if (( mRuleController.getRuleModel().getIsPresetOrSuggestedFlag()) && (userEdited)) 
    		mPresetOrSuggestedEdited = true;
    	//TODO why does this keeps coming and going in commits, do we need it
    	//setRuleIconInEditMode();
        if( (mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID) 
        		&& mRuleController.getRuleModel().isValidRule())
        	toggleSaveButtonStatus(true);	
        displayStatusForAllVisibleBlocks(RuleTable.Active.INACTIVE);
        
        if (userEdited) 
            showTitleBarInUserEditMode();
        else
            showTitleBarInScratchOrSampleMode();
        
        // Unregister the content observer as we do not want to update the rule icon
        // if we are in the edit mode.
        unregisterContentObserver();
    }

	public void processGenericSelectedConditionListItem() {
        
        if (mRuleController.getRuleModel().isValidRule()) 
        	toggleSaveButtonStatus(true);
        else
        	toggleSaveButtonStatus(false);
		
	}


	public View processPluginSelectedConditionListItem(Publisher condPubInfo, Intent data) {
		 //now create the views representing the selected publisher
        View blockView = Blocks.createCorrespondingConditionBlocks(mContext, mCondBlockLayer, 
        											mThisOnClickListener, condPubInfo, mRuleController.getRuleModel());
        setAddedBlockGestureCallback(blockView);
      	Blocks.generateConditionBlockInstanceId(mRuleController.getRuleModel(), blockView);
        enterEditMode(true);
        //need to update the instance of ViewGroup else leading to issue post ICS
        mLastBlockViewSelected = blockView;
		
        return blockView;
	}


	public void processGenericSelectedActionListItem() {
        if (mRuleController.getRuleModel().isValidRule()) 
        	toggleSaveButtonStatus(true);
        else
        	toggleSaveButtonStatus(false);
		
	}


	public View processPluginSelectedActionListItem(Publisher actionPubInfo, Intent data) {
		View blockView = Blocks.createCorrespondingActionBlocks(mContext,mActionBlockLayer,  
													mThisOnClickListener, actionPubInfo, mRuleController.getRuleModel());
	    Blocks.generateActionBlockInstanceId(mRuleController.getRuleModel(), blockView);
		setAddedBlockGestureCallback(blockView);
		enterEditMode(true);
        //need to update the instance of ViewGroup else leading to issue post ICS
        mLastBlockViewSelected = blockView;
		return blockView;
	}


	public void removeGenericPublisher(View block) {
        
	    enterEditMode(true);
	     if (!mRuleController.getRuleModel().isValidRule()){
        	toggleSaveButtonStatus(false);
        }
        else{
        	toggleSaveButtonStatus(true);
        }
        
        //If all the blocks have been deleted, bring back user instruction on canvas
        BlockLayout blockLayout = (BlockLayout)block.getParent();
        addUserInstructions(blockLayout);
		
	}
	
	public void removePluginPublisher(View block){
		BlockLayout blockLayout = (BlockLayout)block.getParent();
		blockLayout.removeView(block);
		blockLayout.requestLayout();
	}


	public void postConfigureGenericActionBlock() {
		if (mProgressDialog != null){
    		mProgressDialog.dismiss();
    		mProgressDialog = null;
			}
			enterEditMode(true);
			//TODO check if check for valid rule can be removed
			if (mRuleController.getRuleModel().isValidRule()) 
        	toggleSaveButtonStatus(true);
			
			if (mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID && mLastAction != null) 
	       		mLastAction.setState(EditType.EDITED);
		
	}


	public void postConfigureGenericConditionBlock() {
		enterEditMode(true);
		//TODO check if check for valid rule can be removed
        if (mRuleController.getRuleModel().isValidRule()) 
        	toggleSaveButtonStatus(true);
        
        if  (mRuleController.getRuleModel().getRuleInstance().get_id() != DEFAULT_RULE_ID && mLastCondition != null) 
			   mLastCondition.setState(EditType.EDITED);
		
	}
	

	public void postConfigurePluginActionBlock(){
		   
		   if(mLastAction == null) {
			   Log.e(TAG, "mLastAction is null");
		   }
		   else {
			   if (mLastAction.getAction().getConfig() != null) {
				    Publisher savedBlockInfo = (Publisher) mLastBlockViewSelected.getTag();
				    savedBlockInfo.setError(false); //if this flag was set then this is place to toggle it back
				    Blocks.removeConfigurationRequiredStatus(mContext, mLastBlockViewSelected);
			   }
			
			   if (mLastAction.getAction().getOnExitModeFlag() == ActionTable.OnModeExit.ON_EXIT)
			   		Blocks.displayEndOfRuleStatus(mContext, mLastBlockViewSelected);

				//TODO in future move all this piece of code from Activity to the 
		   		//Action/Trigger callback listening on the UI entity
		   		//In current UI mLastEvent can be passed and all updates happen in
		   		//BlockGestureListener callback
			   Publisher savedRuleInfo = (Publisher) mLastBlockViewSelected.getTag();
			   savedRuleInfo.setBlockDescription(mLastAction.getAction().getDescription());
			   savedRuleInfo.setConfig(mLastAction.getAction().getConfig());
			   savedRuleInfo.setWhenRuleEnds(mLastAction.getAction().isOnExitModeAction());
			   mLastBlockViewSelected.setTag(savedRuleInfo);
			   TextView aDescription = (TextView)mLastBlockViewSelected.findViewById(R.id.actiondescription);
			   aDescription.setText(mLastAction.getAction().getDescription()); //TODO check if other block fields have to be updated
			   if(null==savedRuleInfo.getBlockDescription()){
		        	aDescription.setVisibility(View.GONE);
		       }else{
		        	aDescription.setVisibility(View.VISIBLE);
		        	aDescription.setText(mLastAction.getAction().getDescription());
		       }
			   Blocks.setActionBlockStatus(mContext, mLastBlockViewSelected, RuleTable.Active.INACTIVE);
			   
			   //get an instance of the gesture listener callback and use it to force re-measure
			   //and redraw the child TextView on getting new descriptions
			   BlockGestureListener blockGestListInst = savedRuleInfo.getInstBlockGestureListener();
			   if(blockGestListInst != null)
				   blockGestListInst.updateLayoutOnReconfigure();
		   }
	   }


	    public void postConfigurePluginConditionBlock(){
	    	if (mLastCondition == null) {
	    		Log.e(TAG, "mLastEvent is null");
	    	}
	    	else {
			if(mLastCondition.getCondition().getConfig() != null) {
				//the status line should not be displayed if the block has been configured
				Blocks.removeConfigurationRequiredStatus(mContext, mLastBlockViewSelected);
	    		}
	    		
	    		//TODO in future move all this piece of code from Activity to the 
	    		//Action/Trigger callback listening on the UI entity
	    		//In current UI mLastEvent can be passed and all updates happen in
	    		//BlockGestureListener callback
		    	Publisher savedRuleInfo = (Publisher) mLastBlockViewSelected.getTag();
		    	savedRuleInfo.setBlockDescription(mLastCondition.getCondition().getDescription());
                savedRuleInfo.setConfig(mLastCondition.getCondition().getConfig());
		    	mLastBlockViewSelected.setTag(savedRuleInfo);
		    	TextView aDescription = (TextView)mLastBlockViewSelected.findViewById(R.id.actiondescription);
		    	aDescription.setText(mLastCondition.getCondition().getDescription());
		    	if(null==savedRuleInfo.getBlockDescription()){
		        	aDescription.setVisibility(View.GONE);
		        }else{
		        	aDescription.setVisibility(View.VISIBLE);
		        	aDescription.setText(mLastCondition.getCondition().getDescription());
		        }
				Blocks.setConditionBlockStatus(mContext, mLastBlockViewSelected, RuleTable.Active.INACTIVE);
		    	//get an instance of the gesture listener callback and use it to force re-measure
		    	//and redraw the child TextView on getting new descriptions
		    	BlockGestureListener blockGestListInst = savedRuleInfo.getInstBlockGestureListener();
		    	if(blockGestListInst != null)
		    		blockGestListInst.updateLayoutOnReconfigure();
	    	}
	    }

		public void onSaveEditWhileActive(boolean isManual) {
			if(isManual){
				mHandler.sendEmptyMessageDelayed(HandlerMessage.EDIT_ACTIVE_RULE, 500);
			}else{			
				mProgressDialog = ProgressDialog.show(mContext,"", getString(R.string.saving_rule), true);
	 		    if(LOG_DEBUG) Log.d(TAG, "Automatic rule Conditions seemed to have changed");		    
	 		    IntentFilter intentToReceiveFilter = new IntentFilter(SAVE_INTENT_CALLBACK);
	   	 		mContext.registerReceiver(mRulesBuilderIntentReceiver, intentToReceiveFilter, null, null);
	   	 	    mRulesBuilderBroadcastReceiverRegistered = true;   	 		
			}
			
		}

		/** onSaveFinish
		 * TODO: Per code review: Reformat code to break lines at column 90.
		 */
		public void onSaveFinish() {
			// hide the keyboard if it is showing
			hideKeyboard();
			mInEditMode = false;    //toggle the flag just before exiting, in case this instance gets to use this flag
			if(LOG_DEBUG) Log.d(TAG, " mFromWhereActivityWasInvoked : " + mFromWhereActivityWasInvoked);
			if(mFromWhereActivityWasInvoked!= null){
				Bundle result = new Bundle();
				result.putLong(LandingPageIntentExtras.RULE_ID_INSERTED, mRuleController.getRuleModel().getRuleInstance().get_id()); //This ruleId is used by the Suggestions Module.
				result.putBoolean(LandingPageIntentExtras.RULE_ICON_CHANGED, mRuleController.getRuleModel().getHasRuleIconChanged());        
			    result.putBoolean(LandingPageIntentExtras.IS_RULE_MANUAL_RULE, mRuleController.getRuleModel().isManualRule());
			    result.putInt(LandingPageIntentExtras.RESULT_CODE, Activity.RESULT_OK);	
				if (mRequestCode != -1) {
					result.putInt(LandingPageIntentExtras.REQUEST_CODE, mRequestCode);
				}
		        mDelegate.setReturnResult(result);
		            
		        if(!mFirstTimeLocWiFiDialogShown ){
		        	//finish();	//if its not this case  finish will be called by handler of Ok/Accept of that Dialog at showWiFiLocationCorrelationDialog
		        	closeFragment();
		        }
			}else{
				//in case we did not come here from LandingPage, we need to explicitly take us there
				if(LOG_DEBUG) Log.d(TAG, "onSave: Did not Came from Landing page, still heading back there");
				//LandingPageActivity.startLandingPageActivity(mContext);
				closeFragment();
			}			
		}
		
		/** Hides the keyboard
		 *  This method is usually called when leaving the Fragment
		 */
		private void hideKeyboard() {
	    	InputMethodManager inputMethod = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
	    	if (inputMethod != null) {
	    		inputMethod.hideSoftInputFromWindow(mEditRuleName.getWindowToken(), 0);
	        }
		}
	
		/** Container Activity must implement this interface
	     */
		public interface Delegate {
			public void setReturnResult(Bundle result);
		}
}
