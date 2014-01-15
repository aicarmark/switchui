/*
 * @(#)SuggestionDetailsActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2011/04/09 NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.drivingmode;

import java.net.URISyntaxException;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.util.PublisherFilterlist;
import com.motorola.contextual.smartrules.util.Util;
import com.motorola.contextual.smartrules.widget.DialogUtil;

/**
 * This class is a transparent activity that displays suggestions alert dialog to the user.
 * <code><pre>
 * CLASS:
 *  extends Activity
 *
 *  implements
 *      DialogInterface.OnClickListener - to handle dialog yes/no clicks
 *      DialogInterface.OnCancelListener - to kill this activity when dialog is canceled.
 *
 * RESPONSIBILITIES:
 *  To show the Driving Mode Dialogs and import the configured rule to ruleimpoirt
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 */

public class DrivingModeSuggestionDialog extends Activity implements Constants, XmlConstants, DbSyntax,
    View.OnClickListener, android.widget.AdapterView.OnItemClickListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
    SimpleCursorAdapter.ViewBinder, AsyncTaskQueryHandler {

    // debug TAG
    private static final String TAG = DrivingModeSuggestionDialog.class.getSimpleName();
    private static final String BT_HELP_PAGE = "<a href=\"http://www.motorola.com/bluetoothdevices\">";
    private static final String DOCK_HELP_PAGE = "<a href=\"http://www.motorola.com/vehicledock\">";
    private static final String CARDOCK_ACTION = "com.motorola.smartcardock.LAUNCHER";
    private Context mContext = null;
    private AlertDialog mAlertDialog = null;
    private int mDialogType = DialogType.INITIAL;
    private static String mRuleKey =  DRIVEMODE_RULE_KEY;
    private boolean mDockAvailable = true;
    private boolean mVehicleModeAvailable = true;

    ViewGroup rootView = null;
    ListView ruleList = null;
    //Possible values for DialogType.
    private interface DialogType {
        /** This is the default value. This means that the initial suggestion dialog is shown */
        int INITIAL = 0;
        /** This is the rule selection Dialog */
        int SELECT_RULE = 1;
        /** This is the trigger selection Dialog */
        int SELECT_TRIGGER = 2;
        /** This is the confirmation Dialog for Manual Trigger */
        int CONFIRM_MANUAL   = 3;
        /** This is the confirmation Dialog for BT Trigger */
        int CONFIRM_BT   = 4;
        /** This is the confirmation Dialog for Dock Trigger */
        int CONFIRM_DOCK   = 5;
    }

    private interface Trigger {
        int MANUAL = 0;
        int BLUETOOTH    = 1;
        int DOCK   = 2;
    }

    /** Used to store the values for each row
     */
    private static class KeyValues {
        long 	_id;
        String 	name;
        String 	key;
        boolean enabled;
        boolean active;
        int ruleType;
        String icon;
    }

    private KeyValues mClickedListInfo = null;
    private int mTriggerType = Trigger.MANUAL;
    private static int mRequestId = 0;
    private static final String BT_LAUNCH_URI = "#Intent;action=android.intent.action.EDIT;component=com.motorola.contextual.smartrules/com.motorola.contextual.pickers.conditions.bluetooth.BTDeviceActivity;end";

    private static final String EXTRA_TYPE = "dialogtype";
    private static final String EXTRA_NAME = "name";


    /**
     * Here we initialize window and fetch extras from the intent first, then start building the
     * suggestion text (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (LOG_DEBUG) Log.d(TAG, "Oncreate Called with intent " + getIntent().toUri(0));
        // remove the window title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // save the context
        mContext = this;
        mDialogType = getIntent().getIntExtra(EXTRA_TYPE, DialogType.INITIAL);
        if (mDialogType == DialogType.INITIAL)
            mRequestId = getIntent().getIntExtra(EXTRA_REQUEST_ID, INVALID_REQUEST_ID);

        PublisherFilterlist instFilterList = PublisherFilterlist.getPublisherFilterlistInst();
        mDockAvailable = !instFilterList.isBlacklisted(mContext, DRIVEMODE_DOCK_PUB_KEY);
        mVehicleModeAvailable = isVehicleModeAvailable();

       //Build the dialogView and show dialog
        rootView = buildDialogView();
        if (rootView != null)
            showAlertDialog(rootView);
    }


    @Override
    protected void onPause() {
        if (mDriveModeQuery != null) {
            mDriveModeQuery.cancel();
        }
        super.onPause();
    }


    /*
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    protected void onDestroy() {

        // kill the dialog
        if(mAlertDialog != null) mAlertDialog.cancel();

        super.onDestroy();
    }

    /**
     * The main method where we build the actual suggestion, all other functions are called
     * internally from here.
     */
    private ViewGroup buildDialogView() {
         if (LOG_INFO) Log.i(TAG, "buildSuggestionText DialogType is " + mDialogType);
        // Inflate our custom layout for the dialog based on the dialog type
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        ViewGroup dialogView = null;
        switch (mDialogType) {
            case DialogType.INITIAL :
                //Query to get any existing driveMode rules
                ProgressDialog progress = new ProgressDialog(this);
                progress.setMessage(mContext.getResources().getString(R.string.loading));
                mDriveModeQuery = new DriveModeQueryRules(progress, this);
                mDriveModeQuery.execute(this);
                break;
            case DialogType.SELECT_TRIGGER :
                dialogView = (ViewGroup)inflater.inflate(R.layout.sugg_card_drive_mode_trigger_select, null);
                //Check to see if Dock Trigger exists
                if (mDockAvailable) {
                    // Normal Flow
                    LinearLayout radioLayout = (LinearLayout) dialogView.findViewById(R.id.bulletpoint_wrapper_1);
                    radioLayout.setOnClickListener(this);
                    radioLayout = (LinearLayout) dialogView.findViewById(R.id.bulletpoint_wrapper_2);
                    radioLayout.setOnClickListener(this);
                    radioLayout = (LinearLayout) dialogView.findViewById(R.id.bulletpoint_wrapper_3);
                    radioLayout.setOnClickListener(this);

                    // Set the hyperLinks
                    TextView btLinkView = (TextView)dialogView.findViewById(R.id.btLink);
                    btLinkView.setMovementMethod(LinkMovementMethod.getInstance());
                    String btText = BT_HELP_PAGE + mContext.getText(R.string.sg_drivemode_WhatisThis) + "</a>";
                    btLinkView.setText(Html.fromHtml(btText));

                    TextView dockLinkView = (TextView)dialogView.findViewById(R.id.dockLink);
                    dockLinkView.setMovementMethod(LinkMovementMethod.getInstance());
                    String dockText = DOCK_HELP_PAGE + mContext.getText(R.string.sg_drivemode_WhatisThis) + "</a>";
                    dockLinkView.setText(Html.fromHtml(dockText));
                } else {
                    // MMCP flow when Dock Trigger is not available
                    TextView headerTextView = (TextView)dialogView.findViewById(R.id.top_text);
                    headerTextView.setText(getString(R.string.sg_drivemode_selectRuleDesc));

                    // Set the BT hyperlink
                    TextView btLinkView = (TextView)dialogView.findViewById(R.id.btLink);
                    btLinkView.setMovementMethod(LinkMovementMethod.getInstance());
                    String btText = BT_HELP_PAGE + mContext.getText(R.string.sg_drivemode_WhatisThis) + "</a>";
                    btLinkView.setText(Html.fromHtml(btText));

                    //Hide Everything but the BT option
                    RadioButton radio1 = (RadioButton) dialogView.findViewById(R.id.radioButton_1);
                    radio1.setVisibility(View.GONE);
                    LinearLayout radioLayout = (LinearLayout) dialogView.findViewById(R.id.bulletpoint_wrapper_2);
                    radioLayout.setVisibility(View.GONE);
                    TextView dockLinkView = (TextView)dialogView.findViewById(R.id.dockLink);
                    dockLinkView.setVisibility(View.GONE);
                    View seperatorView = (View)dialogView.findViewById(R.id.separator2);
                    seperatorView.setVisibility(View.GONE);
                    radioLayout = (LinearLayout) dialogView.findViewById(R.id.bulletpoint_wrapper_3);
                    radioLayout.setVisibility(View.GONE);
                    seperatorView = (View)dialogView.findViewById(R.id.separator3);
                    seperatorView.setVisibility(View.GONE);
                }
                break;
            case DialogType.CONFIRM_MANUAL :
                dialogView = (ViewGroup)inflater.inflate(R.layout.sugg_card_drive_mode_confirm_manual, null);
                break;
            case DialogType.CONFIRM_DOCK :
                dialogView = (ViewGroup)inflater.inflate(R.layout.sugg_card_drive_mode_confirm_dock, null);
                break;
            case DialogType.CONFIRM_BT :
                dialogView = (ViewGroup)inflater.inflate(R.layout.sugg_card_drive_mode_confirm_bt, null);
                break;
            default:
                Log.e(TAG, "Invalid Dialog Type; How did I get here?");
        }
        return dialogView;
    }


    /**
     * Build the alert dialog with our custom layout
     *
     * @param view - root view of our custom layout
     */
    private void showAlertDialog(ViewGroup view) {
        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder(mContext);

        // set the custom layout and the title
        builder.setView(view);

        String str = getString(R.string.sg_drivemode_widget_title);
        builder.setTitle(str);
        builder.setIcon(R.drawable.suggestion_card_app_icon);

        if (mDialogType == DialogType.SELECT_TRIGGER) {
            if (mDockAvailable) {
                builder.setPositiveButton(getString(R.string.continue_prompt), this);
            } else {
                builder.setPositiveButton(getString(R.string.yes), this);
                builder.setNegativeButton(getString(R.string.no), this);
            }
        } else if (mDialogType == DialogType.INITIAL) {
            builder.setPositiveButton(getString(R.string.yes), this);
            builder.setNegativeButton(getString(R.string.no), this);
        } else  {
            builder.setPositiveButton(getString(R.string.ok), this);
        }


        // show the dialog after canceling the previous one
        if(mAlertDialog != null) mAlertDialog.cancel();
        mAlertDialog = builder.create();
        mAlertDialog.setOnShowListener(new OnShowListener() {

            public void onShow(DialogInterface dialog) {
                //Button is disabled for Trigger Selection Dialog
                if ((mDialogType == DialogType.SELECT_TRIGGER && mDockAvailable)
                        || mDialogType == DialogType.SELECT_RULE)
                    ((AlertDialog)dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            }
        });

        mAlertDialog.setOnCancelListener(this);
        mAlertDialog.setInverseBackgroundForced(true);
        mAlertDialog.show();

    }


    /**
     * We need to kill this activity when user cancels the dialog (non-Javadoc)
     *
     * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
     */
    public void onCancel(DialogInterface dialog) {
         setResult(RESULT_CANCELED);
        finish();
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
        if (LOG_DEBUG) Log.d(TAG, "Item Clicked = " + button);
        // positive/yes button clicked
        if (button == AlertDialog.BUTTON_POSITIVE) { // YES
            if (LOG_INFO) Log.i(TAG, "onClick: Positive Button");

            if (mDialogType == DialogType.INITIAL) {
                //Change the dialogType to Trigger selection
                mDialogType = DialogType.SELECT_TRIGGER;
                showNextDialog(mContext, mDialogType, null);
            } else if (mDialogType == DialogType.SELECT_RULE) {
                if (mClickedListInfo == null) {
                    //Create new Drive Mode Rule
                    mDialogType = DialogType.SELECT_TRIGGER;
                    showNextDialog(mContext, mDialogType, null);
                } else {
                    String RuleName = mClickedListInfo.name;
                    if (LOG_INFO) Log.i(TAG, "Accepted rule " + RuleName);
                    sendWidgetExistingRuleIntent(mContext, mClickedListInfo);
                }
            } else if (mDialogType == DialogType.SELECT_TRIGGER) {
                if (!mDockAvailable) mTriggerType = Trigger.BLUETOOTH;
                if (LOG_DEBUG) Log.d(TAG, "Suggestion Accepted");
                if (LOG_INFO) Log.i(TAG, "Invoking Rules Importer");
                String drivingRule = null;
                boolean enabled = false;
                int ruleType = RuleTable.RuleType.MANUAL;
                mRuleKey = DRIVEMODE_RULE_KEY.substring(0, DRIVEMODE_RULE_KEY.lastIndexOf(".")) + DOT + new Date().getTime()+"";
                switch (mTriggerType) {

                    case Trigger.MANUAL :
                        drivingRule = DRIVEMODE_RULEINFO_PREFIX + DRIVEMODE_RULETYPE_MANUAL +
                                DRIVEMODE_KEY_PREFIX + mRuleKey + DRIVEMODE_KEY_SUFFIX +
                                DRIVEMODE_RULEINFO_SUFFIX + getDriveModeActions();
                        mDialogType = DialogType.CONFIRM_MANUAL;
                        break;
                    case Trigger.BLUETOOTH :
                        if (RulePersistence.getVisibleEnaAutoRulesCount(mContext)
                                >= MAX_VISIBLE_ENABLED_AUTOMATIC_RULES) {
                            if (LOG_DEBUG) Log.d(TAG, "Max visible auto enabled rules - " +
                            "cannot enable anymore.");
                            DialogUtil.showMaxVisibleEnaAutoRulesDialog(mContext);
                            setResult(RESULT_CANCELED);
                            return;
                        }
                        enabled = true;
                        ruleType = RuleTable.RuleType.AUTOMATIC;
                        mDialogType = DialogType.CONFIRM_BT;
                        Intent launchIntent = null;
                        try {
                            launchIntent = Intent.parseUri(BT_LAUNCH_URI, 0);
                        } catch (URISyntaxException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            Log.e(TAG, "URI parsing failed");
                        }
                        startActivityForResult(launchIntent, 0);
                        return;

                    case Trigger.DOCK :
                        if (RulePersistence.getVisibleEnaAutoRulesCount(mContext)
                                >= MAX_VISIBLE_ENABLED_AUTOMATIC_RULES) {
                            if (LOG_DEBUG) Log.d(TAG, "Max visible auto enabled rules - " +
                            "cannot enable anymore.");
                            DialogUtil.showMaxVisibleEnaAutoRulesDialog(mContext);
                            setResult(RESULT_CANCELED);
                            return;
                        }
                        drivingRule = DRIVEMODE_RULEINFO_PREFIX + DRIVEMODE_RULETYPE_AUTO +
                                DRIVEMODE_KEY_PREFIX + mRuleKey + DRIVEMODE_KEY_SUFFIX +
                                DRIVEMODE_RULEINFO_SUFFIX + DRIVEMODE_DOCK_TRIGGER + DRIVEMODE_ACTIONS_DOCK;
                        enabled = true;
                        ruleType = RuleTable.RuleType.AUTOMATIC;
                        mDialogType = DialogType.CONFIRM_DOCK;
                        break;
                    default:
                        Log.e(TAG, "Invalid trigger type");
                }

                invokeRulesImporter(mContext, drivingRule);
                sendWidgetNewRuleIntent (mContext, enabled, false, ruleType);
                showNextDialog(mContext, mDialogType, null);

            }

        } else if (button == AlertDialog.BUTTON_NEGATIVE) { //No
            if (LOG_INFO) Log.i(TAG, "onClick: Negative Button");
            if (mDialogType == DialogType.SELECT_TRIGGER) {
                if (LOG_DEBUG) Log.d(TAG, "Suggestion Manual Rule Accepted");
                mRuleKey = DRIVEMODE_RULE_KEY.substring(0, DRIVEMODE_RULE_KEY.lastIndexOf(".")) + DOT + new Date().getTime()+"";

                String drivingRule = DRIVEMODE_RULEINFO_PREFIX + DRIVEMODE_RULETYPE_MANUAL +
                        DRIVEMODE_KEY_PREFIX + mRuleKey + DRIVEMODE_KEY_SUFFIX +
                        DRIVEMODE_RULEINFO_SUFFIX + getDriveModeActions();
                mDialogType = DialogType.CONFIRM_MANUAL;
                if (LOG_DEBUG) Log.d(TAG, "Suggestion Manual Rule Accepted Rule is " + drivingRule);
                invokeRulesImporter(mContext, drivingRule);
                sendWidgetNewRuleIntent (mContext, false, false, RuleTable.RuleType.MANUAL);
                showNextDialog(mContext, mDialogType, null);
            } else {
                if (LOG_DEBUG) Log.d(TAG, "Suggestion Not accepted; No Button click");
                setResult(RESULT_CANCELED);
            }

        }

        // kill activity
        finish();
    }

    private boolean isVehicleModeAvailable() {
        Intent intent = new Intent(CARDOCK_ACTION);
        if (Util.isActivityAvailable(mContext, intent))
            return true;
        else
            return false;
    }

    private String getDriveModeActions() {

        if (mVehicleModeAvailable)
            return DRIVEMODE_ACTIONS;
        else
            return DRIVEMODE_ACTIONS_DOCK;
    }


    public void onRadioButtonClicked(View v) {
        // Perform action on clicks
        int id = v.getId();
        RadioButton button1 = (RadioButton) rootView.findViewById(R.id.radioButton_1);
        RadioButton button2 = (RadioButton) rootView.findViewById(R.id.radioButton_2);
        RadioButton button3 = (RadioButton) rootView.findViewById(R.id.radioButton_3);
        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        switch(id) {
        case R.id.radioButton_1 :
            if (LOG_DEBUG) Log.d(TAG, "Button1");
            mTriggerType = Trigger.BLUETOOTH;
            button2.setChecked(false);
            button3.setChecked(false);
            break;
        case R.id.radioButton_2 :
            if (LOG_DEBUG) Log.d(TAG, "Button2");
            mTriggerType = Trigger.DOCK;
            button1.setChecked(false);
            button3.setChecked(false);
            break;
        case R.id.radioButton_3 :
            if (LOG_DEBUG) Log.d(TAG, "Button3");
            mTriggerType = Trigger.MANUAL;
            button1.setChecked(false);
            button2.setChecked(false);
            break;
        default:
            Log.e(TAG, "Invalid selection; How did I get here?");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (LOG_DEBUG)  Log.d(TAG, "onActivityResult requestCode " + requestCode);
        if (LOG_DEBUG) Log.d(TAG, "onActivityResult resultCode " + resultCode);
        if (resultCode == RESULT_OK){
            if (LOG_DEBUG) Log.d(TAG, "onActivityResult data " + data.toUri(0));
            StringBuffer btTrigger = new StringBuffer();
            btTrigger.append(DRIVEMODE_BT_TRIGGER_PREFIX);
            btTrigger.append(DRIVEMODE_SYNTAX_PREFIX);
            btTrigger.append(data.getStringExtra(Intent.EXTRA_TEXT));
            btTrigger.append(DRIVEMODE_SYNTAX_SUFFIX);
            btTrigger.append(DRIVEMODE_DESC_PREFIX);
        btTrigger.append(data.getStringExtra(EXTRA_DESCRIPTION));
            btTrigger.append(DRIVEMODE_DESC_SUFFIX);
        btTrigger.append(DRIVEMODE_CONFIG_PREFIX);
        btTrigger.append(data.getStringExtra(EXTRA_CONFIG));
        btTrigger.append(DRIVEMODE_CONFIG_SUFFIX);
            btTrigger.append(DRIVEMODE_BT_TRIGGER_SUFFIX);
            if (LOG_DEBUG) Log.d(TAG, "onActivityResult btRigger " + btTrigger.toString());

            String drivingRule = DRIVEMODE_RULEINFO_PREFIX + DRIVEMODE_RULETYPE_AUTO +
                    DRIVEMODE_KEY_PREFIX + mRuleKey + DRIVEMODE_KEY_SUFFIX +
                    DRIVEMODE_RULEINFO_SUFFIX + btTrigger.toString() + getDriveModeActions();

            mDialogType = DialogType.CONFIRM_BT;

            invokeRulesImporter(mContext, drivingRule);
            sendWidgetNewRuleIntent (mContext, true, false, RuleTable.RuleType.AUTOMATIC);
            showNextDialog(mContext, mDialogType, data.getStringExtra(EXTRA_DESCRIPTION));
        }
        finish();
    }

    private static void sendWidgetNewRuleIntent (Context context, boolean enabled, boolean active, int ruleType) {
        //Send Rule Added intent to the Widget
        Intent responseIntent = new Intent(RULE_ADDED_ACTION);
        responseIntent.putExtra(RuleTable.Columns.KEY, mRuleKey);
        responseIntent.putExtra(RuleTable.Columns.ACTIVE, active);
        responseIntent.putExtra(RuleTable.Columns.ENABLED, enabled);
        responseIntent.putExtra(RuleTable.Columns.RULE_TYPE, ruleType);
        responseIntent.putExtra(EXTRA_RESPONSE_ID, mRequestId);
        if (LOG_DEBUG) Log.d(TAG, "New Rule Added intent " + responseIntent.toUri(0));
        context.sendBroadcast(responseIntent, SMART_RULES_PERMISSION);
    }

    private static void sendWidgetExistingRuleIntent (Context context, KeyValues info) {
        Intent responseIntent = new Intent(RULE_ATTACHED_ACTION);
        responseIntent.putExtra(RuleTable.Columns.KEY, info.key);
        responseIntent.putExtra(RuleTable.Columns.ACTIVE, info.active);
        responseIntent.putExtra(RuleTable.Columns.ENABLED, info.enabled);
        responseIntent.putExtra(RuleTable.Columns.RULE_TYPE, info.ruleType);
        responseIntent.putExtra(RuleTable.Columns._ID, info._id);
        responseIntent.putExtra(RuleTable.Columns.ICON, info.icon);
        responseIntent.putExtra(RuleTable.Columns.NAME, info.name);
        responseIntent.putExtra(EXTRA_RESPONSE_ID, mRequestId);
        if (LOG_DEBUG) Log.d(TAG, "Existing Rule intent " + responseIntent.toUri(0));
        context.sendBroadcast(responseIntent, SMART_RULES_PERMISSION);

    }
    private static void invokeRulesImporter(Context context, String ruleXml) {
        if (LOG_INFO) Log.i(TAG, "Invoking Rules Importer");

        Intent i = new Intent(LAUNCH_CANNED_RULES);
        i.putExtra(IMPORT_TYPE, 9);
        i.putExtra(XML_CONTENT, ruleXml);
        i.putExtra(EXTRA_REQUEST_ID, Integer.toString(mRequestId));
        context.sendBroadcast(i, SMART_RULES_PERMISSION);

    }

    private static void showNextDialog(Context context, int type, String name) {
        Intent intent = new Intent(context, DrivingModeSuggestionDialog.class);
        intent.putExtra(EXTRA_TYPE, type);
        if (name != null)
            intent.putExtra(EXTRA_NAME, name);
        context.startActivity(intent);
    }


    public void onClick(View v) {
        // Perform action on clicks
        if (v.getId() == R.id.bulletpoint_wrapper_1) {
            if (LOG_DEBUG) Log.d(TAG, "Button1");
            mTriggerType = Trigger.BLUETOOTH;
            RadioButton button = (RadioButton) rootView.findViewById(R.id.radioButton_1);
            button.setChecked(true);
            button = (RadioButton) rootView.findViewById(R.id.radioButton_2);
            button.setChecked(false);
            button = (RadioButton) rootView.findViewById(R.id.radioButton_3);
            button.setChecked(false);
            mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }
        else if (v.getId() == R.id.bulletpoint_wrapper_2) {
            if (LOG_DEBUG) Log.d(TAG, "Button2");
            mTriggerType = Trigger.DOCK;
            RadioButton button = (RadioButton) rootView.findViewById(R.id.radioButton_1);
            button.setChecked(false);
            button = (RadioButton) rootView.findViewById(R.id.radioButton_2);
            button.setChecked(true);
            button = (RadioButton) rootView.findViewById(R.id.radioButton_3);
            button.setChecked(false);
            mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }
        else if (v.getId() == R.id.bulletpoint_wrapper_3) {
            if (LOG_DEBUG) Log.d(TAG, "Button3");
            mTriggerType = Trigger.MANUAL;
            RadioButton button = (RadioButton) rootView.findViewById(R.id.radioButton_1);
            button.setChecked(false);
            button = (RadioButton) rootView.findViewById(R.id.radioButton_2);
            button.setChecked(false);
            button = (RadioButton) rootView.findViewById(R.id.radioButton_3);
            button.setChecked(true);
            mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }

    }

    private DriveModeQueryRules mDriveModeQuery = null;

    /** Async task class to handle the query of the drive mode rules cursor
     */
    private class DriveModeQueryRules extends AsyncTask<Context, Integer, Cursor> {

        private AsyncTaskQueryHandler qHandler = null;
        ProgressDialog progressDialog = null;
        /** Constructor
         *
         * @param handler - QueryHandler interface instance
         */
        DriveModeQueryRules(ProgressDialog progress, AsyncTaskQueryHandler handler) {
            qHandler = handler;
            progressDialog = progress;
        }

        public void onPreExecute() {
            progressDialog.show();
          }

        @Override
        protected void onPostExecute(Cursor ruleCursor) {
            super.onPostExecute(ruleCursor);
            if(qHandler != null)
                qHandler.onQueryFinished(ruleCursor);
            progressDialog.dismiss();
            mDriveModeQuery = null;
        }

        @Override
        protected Cursor doInBackground(Context... params) {
            String[] selectionArgs = {DRIVEMODE_RULE_KEY};
            return mContext.getContentResolver().query(Schema.ADOPTED_SAMPLES_LIST_VIEW_CONTENT_URI, null, null, selectionArgs, null);
        }

        protected void cancel() {
            if (progressDialog != null)
                progressDialog.cancel();
        }
    }

    /** Processes the cursor returned from the async task and displays the rules list
     *
     * @param rulesCursor - cursor of Adopted Drive Mode Rules
     */
    public void onQueryFinished(Cursor rulesCursor) {

        if(rulesCursor == null) {
            Log.e(TAG, "Rule Cursor returned from Async Task is null");
        } else {
            try {
                   LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

                   if (rulesCursor.getCount() == 0) {
                       if (!rulesCursor.isClosed())
                           rulesCursor.close();
                       //No existing Drive Mode rules
                       rootView = (ViewGroup)inflater.inflate(R.layout.sugg_card_drive_mode_initial, null);
                       if (!mVehicleModeAvailable) {
                           LinearLayout mBulletView = (LinearLayout) rootView.findViewById(R.id.bulletpoint_wrapper_3);
                           mBulletView.setVisibility(View.GONE);
                       }
                       showAlertDialog(rootView);
                   } else {
                       // Show the list
                       startManagingCursor(rulesCursor);
                       mDialogType = DialogType.SELECT_RULE;
                       String[] from = {RuleTable.Columns.NAME};
                       int[] to = {R.id.line1};

                       SimpleCursorAdapter adapter = new SimpleCursorAdapter(mContext, R.layout.sugg_card_drive_mode_rule_list_row,
                                                                           rulesCursor, from, to, 0);


                       rootView = (ViewGroup)inflater.inflate(R.layout.sugg_card_drive_mode_rule_select, null);
                       ruleList = (ListView)rootView.findViewById(R.id.rule_list);
                       TextView headerText = new TextView(this);
                       headerText.setPadding(13, 13, 13, 30);
                       headerText.setText(getString(R.string.sg_drivemode_selectRuleDesc));
                       headerText.setTextAppearance(mContext, R.style.Suggestion_Text);
                       ruleList.addHeaderView(headerText, null, false);

                       ruleList.setPadding(0, 13, 0, 0);
                       CheckedTextView rowView = (CheckedTextView)inflater.inflate(R.layout.sugg_card_drive_mode_rule_row, null);

                    ruleList.addFooterView(rowView, null, true);
                    adapter.setViewBinder(this);
                       ruleList.setAdapter(adapter);
                       ruleList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                       ruleList.setOnItemClickListener(this);
                       showAlertDialog(rootView);

                   }
            } catch (Exception e) {
                Log.e(TAG, " Exception managing the ruleCursor");
                e.printStackTrace();
            }
        }
     }


    public Context getContext() {
        return mContext;
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (LOG_DEBUG) Log.d(TAG, "onItemClick : position is " + position);

        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);

        if (view.getId() == R.id.line1) {
            mClickedListInfo = (KeyValues)view.getTag();
            if(LOG_DEBUG) Log.d(TAG, "Rule id is "  + mClickedListInfo._id);
        } else if(view.getId() == R.id.bulletpoint_text_1) {
            if (LOG_DEBUG) Log.d(TAG, "onItemClick : Footer");
            mClickedListInfo = null;
        }
    }

    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        if (LOG_DEBUG) Log.d(TAG, "setViewValue Called");

        if (view.getId() == R.id.line1 && view instanceof CheckedTextView) {

            ((CheckedTextView) view).setText(cursor.getString(cursor.getColumnIndex(RuleTable.Columns.NAME)));
            ((CheckedTextView) view).setVisibility(View.VISIBLE);

            KeyValues info = new KeyValues();

            info._id = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns._ID));
            if (LOG_DEBUG) Log.d(TAG, "id is " + info._id);

            info.name = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.NAME));
            if (LOG_DEBUG) Log.d(TAG, "name is " + info.name);

            info.icon = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.ICON));
            if (LOG_DEBUG) Log.d(TAG, "icon is " + info.icon);

            info.key = cursor.getString(cursor.getColumnIndex(RuleTable.Columns.KEY));
            if (LOG_DEBUG) Log.d(TAG, "key is " + info.key);

            info.ruleType = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.RULE_TYPE));
            if (LOG_DEBUG) Log.d(TAG, "ruleType is " + info.ruleType);

            info.active = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.ACTIVE)) == RuleTable.Active.ACTIVE;
            if (LOG_DEBUG) Log.d(TAG, "active is " + info.active);

            info.enabled = cursor.getInt(cursor.getColumnIndex(RuleTable.Columns.ENABLED)) == RuleTable.Enabled.ENABLED;
            if (LOG_DEBUG) Log.d(TAG, "enabled is " + info.enabled);

            if (mClickedListInfo != null && mClickedListInfo._id == info._id) {
                ((CheckedTextView) view).setChecked(true);
            } else {
                ((CheckedTextView) view).setChecked(false);
            }

            view.setTag(info);
        }

       return true;
    }
}

/** Interface for the call backs to the main thread from the Async Task.
 */
interface AsyncTaskQueryHandler {
    void onQueryFinished(Cursor rulesCursor);
    Context getContext();
}

