/*
 * @(#)SuggestionDetailsActivity.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2011/04/09 NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.suggestions;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.contextual.debug.DebugTable;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app.LandingPageActivity;
import com.motorola.contextual.smartrules.app.LandingPageActivity.LandingPageIntentExtras;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.ConditionPersistence;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.business.SuggestionsPersistence;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;
import com.motorola.contextual.smartrules.publishermanager.RulesValidatorInterface;
import com.motorola.contextual.smartrules.rulesimporter.FileUtil;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants.ImportType;
import com.motorola.contextual.smartrules.service.DumpDbService;
import com.motorola.contextual.smartrules.util.Util;
import com.motorola.contextual.virtualsensor.locationsensor.AppPreferences;
/**
 * This class is a transparent activity that displays suggestions alert dialog to the user.
 * <code><pre>
 * CLASS:
 *  extends Activity
 *
 *  implements
 *      View.OnClickListener - To handle Edit button clicks
 *      DialogInterface.OnClickListener - to handle dialog yes/no clicks
 *      DialogInterface.OnCancelListener - to kill this activity when dialog is canceled.
 *
 * RESPONSIBILITIES:
 *  To show the suggested rule in a proper user readable language and format
 *  Depending on the user action, either:
 *      - Add suggestion as a rule
 *      - Delete suggestion from MM dbase
 *      - Take no action but mark the suggestion as Read
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 */

public class SuggestionDialogActivity extends Activity implements Constants, DbSyntax,
    View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    // debug TAG
    private static final String TAG = SuggestionDialogActivity.class.getSimpleName();

    private Context mContext = null;

    private static final String VSENSOR = "VSENSOR";
    private static final String DEFAULT_DESCRIPTION = "Needs to be configured";
    public static final long DEFAULT_ID = -1;

    // Bullet to be shown in front of "items"
	private static final String HTML_BULLET = "&#8226;&nbsp;&nbsp;";

    // Current suggestion's _id in MM dbase
    private long mRuleId;
    private String mRuleName = null;
    private String mRuleKey = null;
    private int mAdoptCount = 0;
    
    // Suggestion edit button
    private Button mEdit = null;

    private AlertDialog mAlertDialog = null;

    private ArrayList<Map<String, String>> mConfigs = new ArrayList<Map<String, String>>();

    /**
     * Possible values for LIFECYCLE column
     *
     * <pre><code>
     *  -1=IMMEDIATE
     *   0=NEVER_EXPIRES (New rule type suggestions)
     *   1=ONE_TIME (One time actions type suggestions or behavior change suggestions)
     *   For future use > 1 will mean Date/Time the rule expires.
     */
    private int mSugType;

    private interface SugSubType {

        byte invalid = -1;
        byte initial = 3;
    }

    // If this is a behavior type suggestion
    private int mSugSubType = SugSubType.invalid;

    /**
     * Suggestion reasons: what == actions when == conditions why == Rule
     */
    private interface Reason {

        byte what = 0;
        byte when = 1;
        byte why = 2;
    }

    /**
     * Here we initialize window and fetch extras from the intent first, then start building the
     * suggestion text (non-Javadoc)
     *
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove the window title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // save the context
        mContext = this;

        // get extras from the intent
        mRuleId = getIntent().getLongExtra(PUZZLE_BUILDER_RULE_ID, RuleTable.RuleType.DEFAULT);

        // Incorrect suggestion, lets exit.
        if (mRuleId == RuleTable.RuleType.DEFAULT && ! Suggestions.isInitState(mContext)) {
            Log.e(TAG, "Rule ID id invalid!");

            // We should never get here... launch landing page as a safe exit
            LandingPageActivity.startLandingPageActivity(mContext);
            finish();
            return;
        }

        checkSuggestionType();
        
        // start building the suggestion pieces into
        // user readable format
        buildSuggestionText();
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
    private void buildSuggestionText() {

        // Populate fields with suggestion type/subtype
        checkSuggestionType();

     // Inflate our custom layout for the dialog
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        ViewGroup rootView = null;
        if (mSugSubType == SugSubType.initial) {
        	rootView = (ViewGroup)inflater.inflate(R.layout.suggestion_dialog_init, null);
        } else {
        	rootView = (ViewGroup)inflater.inflate(R.layout.suggestion_dialog, null);
        }

        // This could never happen but
        // no point in going ahead if this is true
        if (rootView == null) {
            Log.e(TAG, "rootView is NULL > Impossible!");
            finish();
            return;
        }
        // build the suggestion title if applicable
        if (mSugSubType != SugSubType.initial) {
        	// initialize the edit button
            mEdit = (Button)rootView.findViewById(R.id.sg_customize);
            mEdit.setOnClickListener(this);
            setRuleIcon(rootView);
                      
            // read cursor and append what, when and why reasons
            if (LOG_INFO) Log.i(TAG, "Creating Suggestion Dynamic action/condition text");
            setDynamicText(rootView);
        }
       
        // show the alert dialog with our custom view
        if (LOG_INFO) Log.i(TAG, "Creating Suggestion Alert Dialog");
        showAlertDialog(rootView);

        // mark the suggestion as READ
        SuggestionsPersistence.setSuggestionState(mContext, mRuleId, RuleTable.SuggState.READ);

        // Remove notification bar
        Suggestions.removeNotification(mContext);

        if (LOG_INFO) Log.i(TAG, "Suggestion Displayed = " + mRuleName);
    }

    /**
     * Check and populate the Suggestion type and sub type
     */
    private void checkSuggestionType() {

        // is this the first launch?
        if (Suggestions.isInitState(mContext)) {
            mSugSubType = SugSubType.initial;
            mSugType = RuleTable.Lifecycle.IMMEDIATE;
            return;
        }

        // get the rule cursor
        Cursor ruleCursor = SuggestionsPersistence.getSuggestedRuleCursor(mContext, mRuleId);

        if (ruleCursor == null)
            Log.e(TAG, "Action cursor is null in createSuggestionDetailTitle");
        else {
            try {
                if (ruleCursor.moveToFirst()) {
                    // Get the suggestion type
                    mSugType = ruleCursor.getInt(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.LIFECYCLE));
                    if ((mSugType == RuleTable.Lifecycle.ONE_TIME) ||
                            (mSugType == RuleTable.Lifecycle.IMMEDIATE)) {

                        // hide edit button
                        mEdit.setVisibility(View.GONE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // close the cursor
                ruleCursor.close();
            }
        }

        if (LOG_INFO)
            Log.i(TAG, "SugType= " + mSugType + "; SugSubType=" + mSugSubType);
    }


    /**
     * read respective tables to append what, when and why reasons what == action table when ==
     * condition table why == Rule table
     *
     * @param view - root view of our custom layout
     */
    private void setDynamicText(ViewGroup view) {

        // read suggested reason from action table first
        createActionTexts(view);

        // Read Rule or Condition table depending on the type of the suggestion
        if (mSugType == RuleTable.Lifecycle.NEVER_EXPIRES)
            createConditionText(view);
        else
            createRuleTexts(view);

            try{
                printFreeFlowText(view);
            } catch (NullPointerException e){
                Log.e(TAG, "Free Flow Error, reverting to Boiler plate");

                // reset the SubType call this method again.
                mSugSubType = SugSubType.invalid;
                mConfigs.clear();
                setDynamicText(view);
            }
    }


    /**
     * This method reads Content from RuleTable RULE_SYNTAX, parses the XML
     * and populate the textviews of Free Flow Suggestions UI
     *
     *  Ex:
     *
     *  <SUGGESTION_FREEFLOW><![CDATA[<SUGGESTION_CONTENT>
     *  	<SUGGESTION_ICON>ic_sleep_w</SUGGESTION_ICON>
     *      <SUGGESTION_DESC>rulesxml_nameSleepDesc</SUGGESTION_DESC>
     *      <PROLOGUE>rulesxml_nameSleepPrologue</PROLOGUE>
     *      <BODY><ITEM>rulesxml_nameSleepItem1</ITEM><ITEM>rulesxml_nameSleepItem2</ITEM></BODY>
     *      or
     *      <BODY><BULLET_ITEM><ICON>icon1</ICON><DESC>rulesxml_nameSleepItem1</DESC></BULLET_ITEM><BULLET_ITEM><ICON>icon2</ICON><DESC>rulesxml_nameSleepItem2</DESC></BODY>
     *      <EPILOGUE>rulesxml_nameSleepEpilogue</EPILOGUE>
     * </SUGGESTION_CONTENT>]]></SUGGESTION_FREEFLOW>
     *
     * @param view - root view of the suggestion alert dialog
     */
    private void printFreeFlowText(View view) throws NullPointerException {

        String[] columns = new String[] {RuleTableColumns.RULE_SYNTAX};
        Cursor rCursor = RulePersistence.getRuleCursor(mContext, RulePersistence.getRuleKeyForRuleId(mContext, mRuleId), columns);

        if(rCursor == null || !rCursor.moveToFirst()) {
            Log.e(TAG,"NULL Rule cursor in createFreeFlowText");
            return;
        }

        String sugXml = null;
        try {
            int sugCol  = rCursor.getColumnIndexOrThrow(RuleTableColumns.RULE_SYNTAX);
            sugXml = rCursor.getString(sugCol);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return;
        } finally {
            rCursor.close();
        }

        if(LOG_INFO) Log.i(TAG,"SUGGESTION_FREEFLOW text:\n"+ sugXml);

        NodeList rulesChildNodes = null;
        Document doc = FileUtil.getParsedDoc(sugXml);
        if (doc == null) {
            Log.e(TAG,"NULL return from getParsedDoc");
            return;
        }

        NodeList nl = doc.getElementsByTagName("SUGGESTION_CONTENT");
        if (nl == null) {
            Log.e(TAG,"NodeList is null");
            return;
        }

        Node node = nl.item(0);
        if (node == null) {
            Log.e(TAG,"Node is null");
            return;
        }

        rulesChildNodes = node.getChildNodes();
        if (rulesChildNodes == null) {
            Log.e(TAG,"Child Node is null");
            return;
        }

        for (int i = 0; i < rulesChildNodes.getLength(); i++) {

            Node curNode = rulesChildNodes.item(i);
            if(curNode == null) return;

            String curNodeName = curNode.getNodeName();
            if(curNodeName == null) return;

            if (curNodeName.equals("SUGGESTION_ICON") && curNode.getFirstChild() != null) {
                int iconRes = this.getResources().getIdentifier(curNode.getFirstChild().getNodeValue(), "drawable", this.getPackageName());
                TextView titleDesc = (TextView)view.findViewById(R.id.suggestionDesc);
                titleDesc.setCompoundDrawablePadding(13);
                titleDesc.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconRes, 0);
            } else if (curNodeName.equals("SUGGESTION_DESC") && curNode.getFirstChild() != null) {
                String desc = curNode.getFirstChild().getNodeValue();
                if (! TextUtils.isEmpty(desc)) {
                    TextView titleDesc = (TextView)view.findViewById(R.id.suggestionDesc);
                    titleDesc.setText(desc);
                    if(LOG_INFO) Log.i(TAG,"SUGGESTION_DESC DATA: "+ desc);
                }

            } else if (curNodeName.equals("PROLOGUE") && curNode.getFirstChild() != null) {
                String prologue = curNode.getFirstChild().getNodeValue();
                TextView proView = (TextView)view.findViewById(R.id.suggestionPrologue);
                proView.setText(prologue);

                if(LOG_INFO) Log.i(TAG,"PROLOGUE DATA: "+ prologue);

            } else if (curNodeName.equals("BODY")) {

                String xml = FileUtil.getXmlTreeIn(curNode);
                if(LOG_DEBUG) Log.i(TAG,"Print the BODY XML" + xml);
                if(xml.equals(EMPTY_STRING)) return;

                NodeList bodyChildNodes = null;

                Document bodyDoc = FileUtil.getParsedDoc(xml);
                if (bodyDoc == null) {
                    Log.e(TAG,"NULL return from getParsedDoc");
                    continue;
                }

                NodeList bodyList = bodyDoc.getElementsByTagName("BODY");
                if(bodyList == null){
                    Log.e(TAG, "nodelist is null");
                    continue;
                }

                // item 0 should not be null
                bodyChildNodes = bodyList.item(0).getChildNodes();
                if(bodyChildNodes == null){
                    Log.e(TAG, "Child Node is null");
                    continue;
                }

                LinearLayout layout = (LinearLayout)view.findViewById(R.id.suggestionItems);
                for (int j = 0; j < bodyChildNodes.getLength(); j++) {
                    Node bodyCurNode = bodyChildNodes.item(j);
                    if(bodyCurNode == null) continue;

                    String bodyCurNodeName = bodyCurNode.getNodeName();
                    if(bodyCurNodeName.equals("ITEM") && bodyCurNode.getFirstChild() != null) {
                        String body = bodyCurNode.getFirstChild().getNodeValue();

                        addBulletedTextBox(layout, body);
                        if(LOG_DEBUG) Log.i(TAG,"Item " + j + ": " + body);
                    } else if(bodyCurNodeName.equals("BULLET_ITEM")) {
                    	String itemXml = FileUtil.getXmlTreeIn(bodyCurNode);
                        if(LOG_DEBUG) Log.i(TAG,"Print the BULLET_ITEM XML" + itemXml);
                        if(itemXml.equals(EMPTY_STRING)) continue;

                        Document itemDoc = FileUtil.getParsedDoc(itemXml);
                        if (itemDoc == null) {
                            Log.e(TAG,"NULL return from getParsedDoc");
                            continue;
                        }

                        NodeList itemList = itemDoc.getElementsByTagName("BULLET_ITEM");
                        if(itemList == null){
                            Log.e(TAG, "nodelist is null");
                            continue;
                        }

                        // item 0 should not be null
                        NodeList itemChildNodes = itemList.item(0).getChildNodes();
                        int bulletItemImageRes = 0;
                        String bulletItemText = null;
                        for (int k = 0; k < itemChildNodes.getLength(); k++) {
                        	Node itemCurNode = itemChildNodes.item(k);
                        	if(itemCurNode == null) return;

                            String itemCurNodeName = itemCurNode.getNodeName();
                            if (itemCurNodeName.equals("ICON") && itemCurNode.getFirstChild() != null)
                            	bulletItemImageRes = getResources().getIdentifier(itemCurNode.getFirstChild().getNodeValue(), "drawable", getPackageName());
                            else if(itemCurNodeName.equals("DESC") && itemCurNode.getFirstChild() != null)
                            	bulletItemText = itemCurNode.getFirstChild().getNodeValue();
                        }
                        
                        if (bulletItemImageRes == 0 || bulletItemText == null) {
                        	Log.e(TAG, "nodelist is null");
                            continue;
                        }
                        addBulletedItem(layout, bulletItemImageRes, bulletItemText);
                    }
                }
            }
        }
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
        if (mSugSubType == SugSubType.initial){
            builder.setTitle(getString(R.string.sg_init_title));
        } else {
            String str = String.format(getString(R.string.sg_title), mRuleName);
            builder.setTitle(str);
        }
        builder.setIcon(R.drawable.suggestion_card_app_icon);
        
        // set buttons depending on suggestion type
        // IMP: "initial" should be the first condition!
        if (mSugSubType == SugSubType.initial) {
            builder.setPositiveButton(getString(R.string.sg_next), this);
            builder.setNegativeButton(getString(R.string.cancel), this);
        } else if (mSugType == RuleTable.Lifecycle.IMMEDIATE) {
            builder.setPositiveButton(getString(R.string.ok), this);
        } else if (mSugType == RuleTable.Lifecycle.SWAP_ONE) {
            builder.setPositiveButton(getString(R.string.sg_swap), this);
            builder.setNegativeButton(getString(R.string.sg_thanks), this);
        } else {
            builder.setPositiveButton(getString(R.string.yes), this);
            builder.setNegativeButton(getString(R.string.no), this);
        }

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
     * Read WHEN reasons from condition table and insert those into the condition layout
     *
     * @param rootView - root view of our custom layout
     */
    private void createConditionText(ViewGroup rootView) {

        // read and insert reasons
        showSuggestedReason(Reason.when, rootView);
    }

    /**
     * Read WHAT reasons from action table and insert those into the action layout
     *
     * @param rootView - root view of our custom layout
     */
    private void createActionTexts(ViewGroup rootView) {

        // read and insert reasons
        showSuggestedReason(Reason.what, rootView);
    }

    /**
     * Read WHY reasons from rule table and insert those into the condition layout
     *
     * @param rootView - root view of our custom layout
     */
    private void createRuleTexts(ViewGroup rootView) {

        // read and insert reasons
        showSuggestedReason(Reason.why, rootView);
    }

    /**
     * Read "which" suggestion reason field from "from" cursor, put them into text boxes and then
     * insert them to "where" layout
     *
     * @param which - table to use (action/condition/rule)
     * @param rootView - root view of our custom layout
     */
    private void showSuggestedReason(byte which, ViewGroup rootView) {

        // init the column to be read, depending on the reason type
        Cursor fromCursor = null;

        if (which == Reason.what) {

            // read action table
            fromCursor = SuggestionsPersistence.getSuggestedActionCursor(mContext, mRuleId);
        } else if (which == Reason.when) {

            // read condition table
            fromCursor = SuggestionsPersistence.getSuggestedConditionCursor(mContext, mRuleId);
        }

        // cursor should not be null and should have at least one row
        if (fromCursor == null)
            Log.e(TAG, " Null Cursor in showSuggestedReason");
        else {

            try {
                if (fromCursor.moveToFirst() && fromCursor.getCount() > 0) {

                    do {
                        if (which == Reason.what) {

                            // move on if Action already Accepted
                            int accCol = fromCursor.getColumnIndexOrThrow(ActionTable.Columns.SUGGESTED_STATE);
                            if (mSugType == RuleTable.Lifecycle.UPDATE_RULE &&
                                    Integer.parseInt(fromCursor.getString(accCol)) == RuleTable.SuggState.ACCEPTED)
                                continue; // don't show this action

                            // Launch confirmation when UriToFire is null
                            setIfActionConfigRequired(fromCursor);
                        } else if (which == Reason.when) {

                            // Is this condition compatible on this device?
                            String pubKey = fromCursor.getString(fromCursor.getColumnIndexOrThrow(ConditionTable.Columns.CONDITION_PUBLISHER_KEY));
                            boolean compatible = Util.checkForFeatureAvailability(mContext, pubKey,
                                                 ConditionTable.PkgMgrConstants.CATEGORY);

                            // Is this a hidden condition?
                            String actIntent = fromCursor.getString(fromCursor.getColumnIndexOrThrow(ConditionTable.Columns.ACTIVITY_INTENT));

                            // Don't show this condition for the following
                            if (!compatible || Util.isNull(actIntent))
                                continue;

                            // Launch configuration screen
                            setIfConditionRequired(fromCursor, actIntent, pubKey);
                        }

                    } while (fromCursor.moveToNext());
                } else {
                    Log.e(TAG, "Empty Cursor in showSuggestedReason!");
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } finally {
                fromCursor.close();
            }
        }
    }

    /**
     * When a suggestion shows up with location and is "unverified" the UI from suggestion should take
     * the user to Map Activity. On the other hand, the same from puzzle builder should take the user
     * to a list activity. To accomplish this the suggested uri is set as an extra in the EditURI intent
     * and is set in the rulesimporter.rules or from server appropriately!
     *
     * @param fromCursor - condition cursor
     * @param actIntent - activity intent
     * @param pubKey - publisher key
     * @return -
     * @throws URISyntaxException
     */
     private void setIfConditionRequired(Cursor fromCursor, String actIntent, String pubKey)
                throws URISyntaxException{

       /* long id = fromCursor.getLong(fromCursor.getColumnIndexOrThrow(ConditionTable.Columns._ID));
        String whereClause = ConditionSensorTable.Columns.PARENT_FKEY + EQUALS + id;
        Cursor cSensorCursor = getContentResolver().query(
                            Schema.CONDITION_SENSOR_TABLE_CONTENT_URI, null, whereClause,
                            null, null);

        if (cSensorCursor != null) {
            boolean enabled = fromCursor.getInt(fromCursor.getColumnIndexOrThrow(ConditionTable.Columns.ENABLED))
                              == ConditionTable.Enabled.ENABLED? true : false;

            // Launch confirmation when:
            // - the condition is enabled
            // - there is no corresponding entry in condition sensor table
            if (enabled && cSensorCursor.getCount() == 0) {

                // This code exists for a special case. CxD requirement is when a suggestion shows up with location and is "unverified"
                // the UI from suggestion should take the user to Map Activity. On the other hand, the same from puzzle builder should
                // take the user to a list activity. To accomplish this the suggested uri is set as an extra in the EditURI intent and
                // is set in the rulesimporter.rules or from server appropriately!
                Intent intent = Intent.parseUri(actIntent, 0);
                String suggIntent = intent.getStringExtra(SUGGESTED_URI);
                if ( suggIntent != null && suggIntent.length() > 0 && !suggIntent.equals("null")) {
                    Intent sIntent = Intent.parseUri(suggIntent, 0);
                    // Update the package name to handle upgrades
                    if (sIntent != null && sIntent.getComponent() != null) {
                        sIntent.setClassName(PACKAGE, sIntent.getComponent().getClassName());
                        actIntent = sIntent.toUri(0);
                    }

                } else {
                    // Get the activity intent from package manager
                    Intent aIntent = getActivityIntent(this, ConditionTable.PkgMgrConstants.PUB_KEY,
                                                       pubKey, ConditionTable.PkgMgrConstants.CATEGORY);
                    if (aIntent != null) {
                        actIntent = aIntent.toUri(0);
                    }
                }

                Map<String, String> val = new HashMap<String, String>();

                val.put(ConditionTable.Columns.ACTIVITY_INTENT, actIntent);
                val.put(ConditionTable.Columns.CONDITION_PUBLISHER_KEY, pubKey);

                mConfigs.add(val);
                if(LOG_INFO){
                    String name = fromCursor.getString(fromCursor.getColumnIndexOrThrow(ConditionTable.Columns.SENSOR_NAME));
                    Log.i(TAG, "Condition config required for" + name);
                }
            }

            cSensorCursor.close();
        } */
    } 

    /**
     * Launch confirmation when Action is enabled and UriToFire is null
     *
     * @param fromCursor - action cursor
     */
    private void setIfActionConfigRequired(Cursor fromCursor){

        String config = fromCursor.getString(fromCursor.getColumnIndexOrThrow(ActionTable.Columns.CONFIG));
        boolean enabled = fromCursor.getInt(fromCursor.getColumnIndexOrThrow(ActionTable.Columns.ENABLED))
                             == ActionTable.Enabled.ENABLED? true : false;
        if (enabled && Util.isNull(config)) {

            String pubKey = fromCursor.getString(fromCursor.getColumnIndexOrThrow(ActionTable.Columns.ACTION_PUBLISHER_KEY));
            // Get the activity intent from package manager
            Intent intent = getActivityIntent(this, ActionTable.PkgMgrConstants.PUB_KEY,
                                              pubKey, ActionTable.PkgMgrConstants.CATEGORY);
            String actIntent = (intent != null) ? intent.toUri(0) : null;

            if (actIntent != null && !actIntent.equalsIgnoreCase("null")) {
                Map<String, String> val = new HashMap<String, String>();

                val.put(ConditionTable.Columns.ACTIVITY_INTENT, actIntent);
                val.put(ConditionTable.Columns.CONDITION_PUBLISHER_KEY, pubKey);

                mConfigs.add(val);
                if(LOG_INFO){
                    String name = fromCursor.getString(fromCursor.getColumnIndexOrThrow(ActionTable.Columns.STATE_MACHINE_NAME));
                    Log.i(TAG, "Action config required for" + name);
                }
            }
        }
    }

    /**
     * Create new TextView on the fly, fills a text line with a Bullet 
     * and insert it into 'where' view group.
     *
     * @param where - the view group where text is inserted
     * @param what - the string to be used
     */
    private void addBulletedTextBox(ViewGroup where, String what) {

        LinearLayout bullet = new LinearLayout(mContext);
        bullet.setPadding(10,10,10,10);

        addTextBox(bullet, Html.fromHtml(HTML_BULLET).toString(), R.style.Suggestion_Text_Bullet);
        addTextBox(bullet, what, R.style.Suggestion_Text);

        where.addView(bullet);
    }

    /**
     * Create new ImageView TextView on the fly, fills a text line with an image 
     * and insert it into 'where' view group.
     *
     * @param where - the view group where text is inserted
     * @param bulletItemImage - the image Drawable to be used
     * @param what - the string to be used
     */
    private void addBulletedItem(ViewGroup where, int bulletItemImageRes, String what) {

    	 // create TextView object
        TextView text = new TextView(this);

        // set 'what' reason text
        text.setText(what);
        text.setTextAppearance(mContext, R.style.Suggestion_Text);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setCompoundDrawablePadding(3);
        text.setCompoundDrawablesWithIntrinsicBounds(bulletItemImageRes, 0, 0, 0);
        text.setPadding(0, 0, 0, 10);
        // insert the view into the parent view/layout
        where.addView(text);
    }

    /**
     * Create new TextView on the fly and insert it into 'where' view group.
     *
     * @param where - the view group where text is inserted
     * @param what - the string to be used
     * @param style - Text box font style
     */
    private void addTextBox(ViewGroup where, String what, int style) {

        // create TextView object
        TextView text = new TextView(this);

        // set 'what' reason text
        text.setText(what);
        text.setTextAppearance(mContext, style);
     
        where.addView(text);
    }


  
    /**
     * create the rule title if the suggestion is of new rule type
     *
     * @param rootView - view of root layout
     */
    private void setRuleIcon(ViewGroup rootView) {

        if (LOG_INFO) Log.i(TAG, "Setting Suggestion Rule Icon");

        // get the rule cursor
        Cursor ruleCursor = SuggestionsPersistence.getSuggestedRuleCursor(mContext, mRuleId);

        if (ruleCursor == null)
            Log.e(TAG, "Action cursor is null in createSuggestionDetailTitle");
        else {

            try {
                if (ruleCursor.moveToFirst()) {

                    // hide the title layout for one time action suggestions
                    if (mSugType == RuleTable.Lifecycle.ONE_TIME ||
                            mSugType == RuleTable.Lifecycle.SWAP_ONE) {
                        LinearLayout rL = (LinearLayout)rootView.findViewById(R.id.sg_rule_title_ll);
                        rL.setVisibility(View.GONE);

                        // return as title doesn't exist for ONE_TIME/SWAP suggestions
                        return;
                    }

                    // get Name column
                    int colNo = ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.NAME);
                    mRuleName = ruleCursor.getString(colNo);
                    mRuleKey = ruleCursor.getString(ruleCursor.getColumnIndex(RuleTable.Columns.KEY));
                    
                    // fetch the file name of the drawable for the icon to be used
                    colNo = ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.ICON);
                    String ruleIconString = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(RuleTable.Columns.ICON));

                    if (ruleIconString != null){
                    	int iconRes = getResources().getIdentifier(ruleIconString,
                                "drawable", getPackageName());
                        TextView titleDesc = (TextView)rootView.findViewById(R.id.suggestionDesc);
                        titleDesc.setCompoundDrawablePadding(13);
                        titleDesc.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconRes, 0);
                    }

                  /*  // set the background
                    RelativeLayout iconWrapper = (RelativeLayout) rootView.findViewById(R.id.sg_rule_icon_wrapper);
                    iconWrapper.setBackgroundResource(R.drawable.sr_rule_btn_active);
*/
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // close the cursor
                ruleCursor.close();
            }
        }
    }

    /**
     * We need to kill this activity when user cancels the dialog (non-Javadoc)
     *
     * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
     */
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    /**
     * This handles the click event of edit button - Puzzle builder is launched with rule ID extra
     * in the intent
     *
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View editButton) {

        if (editButton == mEdit) {
            // IKSTABLE6-3076 - disable the button to prohibit accidental multi-clicks
            editButton.setEnabled(false);

            int source = RulePersistence.getColumnIntValue(mContext, mRuleId, RuleTable.Columns.SOURCE);
            mAdoptCount = RulePersistence.launchRulesBuilder(mContext, source, 
            											mRuleId, RULE_SUGGESTED);
        }
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
        // positive/yes button clicked
        if (button == AlertDialog.BUTTON_POSITIVE) { // YES
            Log.i(TAG, "onClick: Positive Button: mSugType = "+mSugType);

            if(mRuleKey != null)
            	DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, null,
            						mRuleName, mRuleKey, SMARTRULES_INTERNAL_DBG_MSG, null,  SUGG_ACCEPTED_DBG_MSG,
            			            Constants.PACKAGE, Constants.PACKAGE);
            
            	Intent dumpActionConditionServiceIntent = new Intent(mContext, DumpDbService.class);
            	dumpActionConditionServiceIntent.putExtra(DumpDbService.SERVICE_TYPE, DumpDbService.REGULAR_REQUEST);
            	dumpActionConditionServiceIntent.putExtra(RuleTable.Columns._ID, mRuleId);
            	mContext.startService(dumpActionConditionServiceIntent);

            // IMP: "initial" should be the first condition!
            if (mSugSubType == SugSubType.initial) {
                Suggestions.setInitState(mContext, false);

                Intent intent = null;
                try {
                    intent = getIntent();
                    if(intent != null) {
                        String uri = intent.getStringExtra(INTENT_ACTION);

                        if(uri != null) {
                            intent = Intent.parseUri(uri, 0);
                            startActivity(intent);
                        }
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            } else if (mSugType == RuleTable.Lifecycle.UPDATE_RULE) {

                // Accept/Enable all actions
                SuggestionsPersistence.enableAllSuggestedActions(mContext, mRuleId);

                launchConfigScreens();
                return; // dont let the act be finished yet

            } else if (mSugType == RuleTable.Lifecycle.IMMEDIATE) {

                // delete this suggestion
                RulePersistence.deleteRule(mContext, mRuleId, null, null, false);

            } else if (mSugType == RuleTable.Lifecycle.SWAP_ONE) {

                // replace old condition with new one
                Suggestions.swapAcceptCondition(mContext, mRuleId);

            } else {
                if(canShowLocationConsentDialog(mRuleId))
                    showLocationConsentDialog();
                else
                    launchConfigScreens();
                return; // dont let the act be finished yet
            }
        } else if (button == AlertDialog.BUTTON_NEGATIVE) { //No

            Log.i(TAG, "onClick: Negative Button");
            if(mRuleKey != null)
            	DebugTable.writeToDebugViewer(mContext, DebugTable.Direction.OUT, null,
                                          mRuleName, mRuleKey, SMARTRULES_INTERNAL_DBG_MSG, null,  SUGG_REJECTED_DBG_MSG,
                                          Constants.PACKAGE, Constants.PACKAGE);

            if (mSugType == RuleTable.Lifecycle.SWAP_ONE) {

                // delete the new condition
                Suggestions.swapRejectCondition(mContext, mRuleId);

            } else {
                // show the rejection dialog
                boolean diagShown = showFirstRejectDialog();

                // return, else our Act would be killed
                if(diagShown) return;
            }
        }

        // kill activity
        finish();
    }


    /**
     * Takes the last Activity Intent from the array list and launches activity thru uri. If array
     * list is empty, just add the new rule
     */
    private void launchConfigScreens() {
        int size = mConfigs.size();
        if (size > 0) {
            String uri = mConfigs.get(size - 1).get(ConditionTable.Columns.ACTIVITY_INTENT);

            try {
                Intent intent = Intent.parseUri(uri, 0);
                startActivityForResult(intent, RULE_SUGGESTED);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {

            // is not of behavior type
            if (mSugType == RuleTable.Lifecycle.ONE_TIME) {
                if (LOG_INFO)
                    Log.i(TAG, "launchConfigScreens: Firing actions" + mRuleId);

                // fire actions
                Suggestions.fireInstantActions(mContext, mRuleId);
            } else {

                if (LOG_INFO)
                    Log.i(TAG, "launchConfigScreens: Adding Rule = " + mRuleId);

                // Connect/Enable the Newly suggested action
                if(mSugType == RuleTable.Lifecycle.UPDATE_RULE)
                    SuggestionsPersistence.enableNewSuggestedActions(mContext, mRuleId);

                // add suggestion as a new rule
                Suggestions.addSuggestionAsRule(mContext, mRuleId);

                // show Landing page
                LandingPageActivity.startLandingPageActivity(mContext);

                Toast.makeText(this,
                               getString(R.string.sg_rule_added),Toast.LENGTH_SHORT).show();
            }

            // Changes related to CR IKSTABLE6-16442
            // User has silently accepted the suggestion so call the handler
            // to check and notify if the trigger publishers need to start
            // listening to sensor state changes.
            if(LOG_DEBUG) Log.d(TAG, "calling notifyConditionPublishers for "+mRuleId);
            ConditionPersistence.notifyConditionPublishers(mContext, mRuleId, true);

            // kill activity
            finish();
        }
    }

    /** Function to check if the first time Location and Wi-Fi error dialog
     *  needs to be shown to the user or not.
     *
     * @param ruleId - rule id of the suggested rule
     * @return - true if the dialog has to be shown else false
     */
    private boolean canShowLocationConsentDialog(long ruleId) {
        boolean result = false;

        String whereClause = ConditionTable.Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q
                             + AND + ConditionTable.Columns.CONDITION_PUBLISHER_KEY + EQUALS + Q + LOCATION_TRIGGER_PUB_KEY + Q
                             + AND + ConditionTable.Columns.ENABLED + EQUALS + Q + ConditionTable.Enabled.ENABLED + Q;

        Cursor cursor = mContext.getContentResolver().query(Schema.CONDITION_TABLE_CONTENT_URI, null, whereClause, null, null);

        int count = 0;
        if(cursor != null && cursor.moveToFirst())
            count = cursor.getCount();

        result = count > 0 && !Util.isMotLocConsentAvailable(mContext);
        if(LOG_DEBUG) Log.d(TAG, "Returning from canShowLocationConsentDialog: "+result);
        return result;
    }

    /** displays the Location and Wi-Fi error dialog to the user.
     */
    private void showLocationConsentDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.location_title);
        builder.setMessage(R.string.wifi_loc_wifi_autoscan_desc);
        builder.setIcon(R.drawable.ic_location_w);
        builder.setPositiveButton(R.string.agree, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Util.setLocSharedPrefStateValue(mContext, AppPreferences.HAS_USER_LOC_CONSENT, TAG, LOC_CONSENT_SET);
                launchConfigScreens();
            }
        });
        builder.setNegativeButton(R.string.disagree, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                // kill activity
                finish();
           }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Fire the actions when PB returned RESULT as OK
     *
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(LOG_INFO) Log.i(TAG, "onActivityResult with reqCode=" + requestCode + ", resultCode="+ resultCode);
        if (requestCode == RULE_SUGGESTED) {

            // kill the dialog first
            if(mAlertDialog != null)  {
            	mAlertDialog.cancel();
            }
            
            if (resultCode == Activity.RESULT_OK 
            		&& mSugType == RuleTable.Lifecycle.ONE_TIME) {
                // If returning from PB, just Fire the suggestion
                long newRuleId = data.getLongExtra(LandingPageIntentExtras.RULE_ID_INSERTED, RuleTable.RuleType.DEFAULT);
                if(newRuleId != mRuleId) {
                    // this means we returned from smart profile
                    updateActionOnResult(data);
                }

                Suggestions.fireInstantActions(mContext, mRuleId);
                finish();
            } else if (resultCode == Activity.RESULT_OK
                       && mSugType == RuleTable.Lifecycle.NEVER_EXPIRES) {
                if (data == null) {
                    Log.e(TAG, NULL_INTENT);
                } else {
                    // If returning from PB
                    long newRuleId = 
                    		data.getLongExtra(LandingPageIntentExtras.RULE_ID_INSERTED, 
                    				RuleTable.RuleType.DEFAULT);
                    if(newRuleId == mRuleId) {
                    	// This is case for a suggestion that is not a sample for example
                    	// Low Battery Saver
                        if(LOG_INFO) Log.i(TAG, "Saving Rule=" + mRuleName);
                        // Logically speaking, we shouldnt do anything here
                        // Accept the suggestion and enable it.
                        SuggestionsPersistence.acceptSuggestion(mContext, mRuleId);

                        // Just show landing page
                        LandingPageActivity.startLandingPageActivity(mContext);
                        finish();
                        return;
                    } else if (newRuleId != RuleTable.RuleType.DEFAULT) {
                    	// This means the suggestion adopted was also a sample rule for
                    	// example Sleep or Meeting. So need to accept the suggestion i.e.
                    	// the parent rule so that it will show back as a  sample and also 
                    	// set the right state for the adopted suggestion i.e. the child rule.
                        if(LOG_INFO) Log.i(TAG, "Adopting as child rule =" + mRuleName);
                        
                        // Enable the accepted suggestion
                        SuggestionsPersistence.acceptSuggestion(mContext, 
                        							newRuleId, mAdoptCount);

                        // remove parent suggestion from inbox
                        SuggestionsPersistence.setSuggestionState(mContext, mRuleId, 
                        							RuleTable.SuggState.ACCEPTED);
                        String ruleKey = RulePersistence.getRuleKeyForRuleId(mContext, newRuleId);
                        RulesValidatorInterface.launchModeAd(mContext, ruleKey, 
                                ImportType.IGNORE, 
                                RuleTable.Validity.VALID, 
                                RulePersistence.fetchRuleOnly(mContext, ruleKey),
                                RulePersistence.isRulePsuedoManualOrManual(mContext, ruleKey));

            /*            // Launch RulesValidator
                        Intent rvIntent =  new Intent(ACTION_RULES_VALIDATE_REQUEST);
                        ArrayList<String> ruleList = new ArrayList<String>();
                        ruleList.add(RulePersistence.getRuleKeyForRuleId(mContext, newRuleId));
                        rvIntent.putExtra(EXTRA_RULE_LIST, ruleList);
                        rvIntent.putExtra(EXTRA_REQUEST_ID, String.valueOf(newRuleId));
                        mContext.sendBroadcast(rvIntent);
*/
                        // show Landing page
                        LandingPageActivity.startLandingPageActivity(mContext);
                        finish();
                        return;
                    }
                    else {

                        if(LOG_INFO) Log.i(TAG, "checking configs for =" + mRuleName);

                        // If returning from smart profile, build sensors, configure actions and conditions
                        String vSensor = data.getStringExtra(VSENSOR);
                        if (vSensor == null || vSensor.length() == 0
                                || vSensor.equalsIgnoreCase("null")) {
                            updateActionOnResult(data);
                        } else {
                            configConditionsOnResult(data);
                        }

                        mConfigs.remove(mConfigs.size() - 1);
                    }
                }
                launchConfigScreens();
            }
        }
    }

    /**
     * This method takes values from the incoming intent and updates the respective fields in
     * Action table
     *
     * @param data - Result intent
     */
    private void updateActionOnResult(Intent data) {
        if (LOG_INFO) Log.i(TAG, "updateActionOnResult: update actions=");

        boolean whenRuleEnds = data.getBooleanExtra(EXTRA_RULE_ENDS, false);

        String actionDescription = data.getStringExtra(EXTRA_DESCRIPTION);
        if (actionDescription == null)
            actionDescription = DEFAULT_DESCRIPTION;

        String config = data.getStringExtra(EXTRA_CONFIG);

        ContentValues cv = new ContentValues();
        cv.put(ActionTable.Columns.CONFIG, config);
        cv.put(ActionTable.Columns.ACTION_DESCRIPTION, actionDescription);
        cv.put(ActionTable.Columns.ON_MODE_EXIT, whenRuleEnds);

        // get publisher key for the action
        String pubKey = mConfigs.get(mConfigs.size() - 1).get(ConditionTable.Columns.CONDITION_PUBLISHER_KEY);

        final String whereClause = ActionTable.Columns.PARENT_FKEY + EQUALS + mRuleId
                                   + AND + ActionTable.Columns.ACTION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;

        // update the row
        getContentResolver().update(Schema.ACTION_TABLE_CONTENT_URI, cv, whereClause, null);
    }

    /**
     * Update condition table and insert a new record in the condition sensor table
     *
     * @param data
     */
    private void configConditionsOnResult(Intent data) {

        if (LOG_INFO)
            Log.i(TAG, "configConditionsOnResult START");

        // Return if the incoming intent is null
        if(data == null) return;

        // get publisher key for the condition
        String pubKey = mConfigs.get(mConfigs.size() - 1).get(ConditionTable.Columns.CONDITION_PUBLISHER_KEY);

        // Update the Condition Table First
        updateConditionTable(data, pubKey);

        if (LOG_INFO)
            Log.i(TAG, "configConditionsOnResult END");
    }


    /**
     * Get fields from incoming intent and update condition table
     *
     * @param data - Intent from Smart Profile
     */
    private void updateConditionTable(Intent data, String pubKey) {

        final String whereClause = ConditionTable.Columns.PARENT_FKEY + EQUALS + mRuleId
                                   + AND + ConditionTable.Columns.CONDITION_PUBLISHER_KEY + EQUALS + Q + pubKey + Q;

        // Update the Condition Table First
        String config = data.getStringExtra(EXTRA_CONFIG);
        String sensorDescription = data.getStringExtra(EXTRA_DESCRIPTION);

        ContentValues cv = new ContentValues();

        // Populate values for condition Table
        cv.put(ConditionTable.Columns.CONDITION_DESCRIPTION, sensorDescription);
        cv.put(ConditionTable.Columns.CONDITION_CONFIG, config);
        // update condition table row
        getContentResolver().update(Schema.CONDITION_TABLE_CONTENT_URI, cv, whereClause, null);
    }




    /**
     * Shows an alert dialog with first rejection message
     *
     * @return - true if dialog is shown
     */
    private boolean showFirstRejectDialog() {

        boolean firstReject = false;

        Intent intent = getIntent();
        if(intent != null)
            firstReject = intent.getBooleanExtra(Suggestions.FIRST_REJECT, false);

        /*
         * Show Suggestion rejection Dialog if this is a first time rejection
         * of the Suggestion dialog
         */
        if( ! firstReject) return firstReject;

        // dismiss the current dialog
        mAlertDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

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
        alertDialog.show();

        // lets not show it again. Ever.
        Suggestions.setFirstRejectState(mContext, false);

        return firstReject;
    }

    /** Returns the activity intent corresponding to a given action/condition publisher by doing a
     *  Package Manager query. Uses the publisher key of the corresponding action or condition.
     *
     * @param ct - Context
     * @param key - Key to retrieve the Publisher key from package manager records
     * @param pubKey - Publisher key to be matched against
     * @param category - Category to be used to query the package manager
     * @return
     */
    private static Intent getActivityIntent(final Context ct, final String key, final String pubKey, final String category) {

        Intent result = null;

        if(key != null && pubKey != null && category != null) {

            Intent mainIntent = new Intent(ACTION_GET_CONFIG, null);
            mainIntent.addCategory(category);
            PackageManager pm = ct.getPackageManager();
            List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA);

            for( ResolveInfo info : list) {

                Bundle metaData = info.activityInfo.metaData;
                if(metaData != null && pubKey.equals(metaData.getString(key))) {
                    result = new Intent(ACTION_GET_CONFIG);
                    result.setClassName(info.activityInfo.applicationInfo.packageName, info.activityInfo.name);
                    break;
                }
            }

        }

        return result;
    }

    /**
     * Shows an alert dialog with first rejection message
     *
     * @return - true if dialog is shown
     */
    @SuppressWarnings("unused")
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
        alertDialog.show();

        // lets not show it again. Ever.
        Suggestions.setFirstRejectState(ct, false);

        if (LOG_DEBUG) Log.d(TAG,"End firstReject : " +firstReject);

        return firstReject;
    }
}
