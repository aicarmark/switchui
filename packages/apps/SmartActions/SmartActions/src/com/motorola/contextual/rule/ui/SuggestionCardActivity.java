/*
 * @(#)SuggestionCardActivity.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/06/8 NA                Initial version
 *
 */

package com.motorola.contextual.rule.ui;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.rule.CoreConstants;
import com.motorola.contextual.rule.publisher.XmlUtils;
import com.motorola.contextual.rule.publisher.db.PublisherPersistence;
import com.motorola.contextual.rule.publisher.db.RulePublisherTable;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app.LandingPageActivity;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.rulesimporter.FileUtil;

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

public class SuggestionCardActivity extends Activity implements Constants, DbSyntax,
    View.OnClickListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    // debug TAG
    private static final String TAG = RP_TAG + SuggestionCardActivity.class.getSimpleName();
    private Context mContext = null;

    // Bullet to be shown in front of "items"
	private static final String HTML_BULLET    = "&#8226;&nbsp;&nbsp;";
    private static final String EXTRA_RULE_KEY = "RuleKey";

    private String mRuleName = null;
    private String mRuleKey = null;

    // Suggestion edit button
    private Button mEdit = null;
    private AlertDialog mAlertDialog = null;

    /**
     * Here we initialize window and fetch extras from the intent first, then start building the
     * suggestion text 
     * 
     * (non-Javadoc)
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
        mRuleKey = getIntent().getStringExtra(EXTRA_RULE_KEY);
        mRuleKey = Uri.encode(mRuleKey);

        // fetch and translate rule name
        mRuleName = PublisherPersistence.getRuleColumnValue(mContext, mRuleKey, RulePublisherTable.Columns.RULE_NAME);
        mRuleName = getString(getStringResource(mRuleName));

        // Incorrect suggestion, lets exit.
        if (mRuleKey == null) {
            Log.e(TAG, "Rule Key id invalid!");

            // We should never get here... launch landing page as a safe exit
            LandingPageActivity.startLandingPageActivity(mContext);
            finish();
            return;
        }

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

        // Inflate our custom layout for the dialog
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.suggestion_dialog, null);

        // This could never happen but
        // no point in going ahead if this is true
        if (rootView == null) {
            Log.e(TAG, "rootView is NULL > Impossible!");
            finish();
            return;
        }

        // initialize the edit button
        mEdit = (Button)rootView.findViewById(R.id.sg_customize);
        mEdit.setOnClickListener(this);

        // read cursor and append what, when and why reasons
        if (LOG_DEBUG)
            Log.i(TAG, "Creating Suggestion Dynamic action/condition text");
        try{
            printFreeFlowText(rootView);
        } catch (NullPointerException e){
            Log.e(TAG, "Free Flow Error");
        }

        // show the alert dialog with our custom view
        if (LOG_DEBUG)
            Log.i(TAG, "Creating Suggestion Alert Dialog");

        showAlertDialog(rootView);

        if (LOG_INFO) Log.i(TAG, "Suggestion Displayed = " + mRuleName);
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

        String SuggContent = PublisherPersistence.getRuleSuggestionText(mContext, mRuleKey);
        if(LOG_VERBOSE) Log.i(TAG,SUGGESTION_FREEFLOW +" text:\n"+ SuggContent);

        Document doc1 = XmlUtils.getParsedDoc(SuggContent);
        if(doc1 == null) return;

        NodeList nl = doc1.getElementsByTagName(SUGGESTION_CONTENT);
        if (nl == null) {
            Log.e(TAG,"NodeList is null");
            return;
        }

        Node node = nl.item(0);
        if (node == null) {
            Log.e(TAG,"Node is null");
            return;
        }

        NodeList rulesChildNodes = node.getChildNodes();
        if (rulesChildNodes == null) {
            Log.e(TAG,"Child Node is null");
            return;
        }

        for (int i = 0; i < rulesChildNodes.getLength(); i++) {

            Node curNode = rulesChildNodes.item(i);
            if(curNode == null) return;

            String curNodeName = curNode.getNodeName();
            if(curNodeName == null) return;

            if (curNodeName.equals(SUGGESTION_ICON) && curNode.getFirstChild() != null) {
                int iconRes = this.getResources().getIdentifier(curNode.getFirstChild().getNodeValue(), "drawable", this.getPackageName());
                TextView titleDesc = (TextView)view.findViewById(R.id.suggestionDesc);
                titleDesc.setCompoundDrawablePadding(13);
                titleDesc.setCompoundDrawablesWithIntrinsicBounds(0, 0, iconRes, 0);
            } else if (curNodeName.equals(SUGGESTION_DESC) && curNode.getFirstChild() != null) {
                String desc = curNode.getFirstChild().getNodeValue();
                if (! TextUtils.isEmpty(desc)) {
                    TextView titleDesc = (TextView)view.findViewById(R.id.suggestionDesc);
                    titleDesc.setText(getStringResource(desc));
                    if(LOG_DEBUG) Log.d(TAG,SUGGESTION_DESC + " DATA: "+ desc);
                }

            } else if (curNodeName.equals(SUG_PROLOGUE) && curNode.getFirstChild() != null) {
                String prologue = curNode.getFirstChild().getNodeValue();
                TextView proView = (TextView)view.findViewById(R.id.suggestionPrologue);
                proView.setText(getStringResource(prologue));

                if(LOG_DEBUG) Log.d(TAG, SUG_PROLOGUE + " DATA: "+ prologue);

            } else if (curNodeName.equals(SUG_BODY)) {

                String xml = FileUtil.getXmlTreeIn(curNode);
                if(LOG_VERBOSE) Log.i(TAG,"Print the BODY XML" + xml);
                if(xml.equals(EMPTY_STRING)) return;

                NodeList bodyChildNodes = null;

                Document bodyDoc = FileUtil.getParsedDoc(xml);
                if (bodyDoc == null) {
                    Log.e(TAG,"NULL return from getParsedDoc");
                    continue;
                }

                NodeList bodyList = bodyDoc.getElementsByTagName(SUG_BODY);
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
                    if(bodyCurNodeName.equals(SUG_ITEM) && bodyCurNode.getFirstChild() != null) {
                        String body = bodyCurNode.getFirstChild().getNodeValue();

                        addBulletedTextBox(layout, body);
                        if(LOG_DEBUG) Log.d(TAG,"Item " + j + ": " + body);
                    } else if(bodyCurNodeName.equals(BULLET_ITEM)) {
                        String itemXml = FileUtil.getXmlTreeIn(bodyCurNode);
                        if(LOG_VERBOSE) Log.i(TAG,"Print the BULLET_ITEM XML" + itemXml);
                        if(itemXml.equals(EMPTY_STRING)) continue;

                        Document itemDoc = FileUtil.getParsedDoc(itemXml);
                        if (itemDoc == null) {
                            Log.e(TAG,"NULL return from getParsedDoc");
                            continue;
                        }

                        NodeList itemList = itemDoc.getElementsByTagName(BULLET_ITEM);
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
                            if (itemCurNodeName.equals(SUG_ICON) && itemCurNode.getFirstChild() != null)
                                bulletItemImageRes = getResources().getIdentifier(itemCurNode.getFirstChild().getNodeValue(), "drawable", getPackageName());
                            else if(itemCurNodeName.equals(SUG_DESC) && itemCurNode.getFirstChild() != null)
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
        String str = String.format(getString(R.string.sg_title), mRuleName);
        builder.setTitle(str);
        builder.setIcon(R.drawable.suggestion_card_app_icon);
        builder.setPositiveButton(getString(R.string.yes), this);
        builder.setNegativeButton(getString(R.string.no), this);

        if(mAlertDialog == null){
            mAlertDialog = builder.create();
            mAlertDialog.setOnCancelListener(this);
            mAlertDialog.setInverseBackgroundForced(true);
        }

        // show the dialog
        mAlertDialog.show();

        Button b = mAlertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if(b != null) b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        b = mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if(b != null) b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
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
        text.setText(getStringResource(what));
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
        text.setText(getStringResource(what));
        text.setTextAppearance(mContext, style);

        where.addView(text);
    }


    /**
     * We need to kill this activity when user cancels the dialog (non-Javadoc)
     *
     * @see android.content.DialogInterface.OnCancelListener#onCancel(android.content.DialogInterface)
     */
    public void onCancel(DialogInterface dialog) {

        // kill the dialog
        if(mAlertDialog != null) mAlertDialog.cancel();

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
            // kill the dialog
            if(mAlertDialog != null) mAlertDialog.cancel();

            // IKSTABLE6-3076 - disable the button to prohibit accidental multi-clicks
            editButton.setEnabled(false);
            setActivityResult(CoreConstants.RP_RESPONSE_CUSTOMIZE);

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

        // kill the dialog
        if(mAlertDialog != null) mAlertDialog.cancel();

        // positive/yes button clicked
        if (button == AlertDialog.BUTTON_POSITIVE) { // YES

            Log.i(TAG, "onClick: Positive Button");
            setActivityResult(CoreConstants.RP_RESPONSE_ACCEPT);

        } else if (button == AlertDialog.BUTTON_NEGATIVE) { //No

            Log.i(TAG, "onClick: Negative Button");
            // show the rejection dialog
            setActivityResult(CoreConstants.RP_RESPONSE_REJECT);

        }
    }

    /**
     * sets the activity result
     *
     * @param result - accept/reject/customize
     */
    private void setActivityResult(String result){

        if(LOG_INFO) Log.i(TAG, "setResult=" + result);

        Intent intent = new Intent();
        intent.putExtra(CoreConstants.EXTRA_RP_RESPONSE, result);
        setResult(RESULT_OK, intent);

        finish();
    }

    /**
     * reads the "string" from the resources
     *
     * @param text - resource name
     * @return - language verbiage
     */
    private int getStringResource(String text){
        return getResources().getIdentifier(text,
                "string", getPackageName());
    }
}