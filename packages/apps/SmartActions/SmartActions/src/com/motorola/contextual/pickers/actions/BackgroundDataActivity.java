/*
 * @(#)SyncActivity.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * crgw47        2012/05/12 NA                Initial version
 *
 */

package com.motorola.contextual.pickers.actions;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to select the Background Data setting to be set as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends MultiScreenPickerActivity
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select Settings for Background Data.
 *     The base class takes care of sending the intent containing the setting to
 *     Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class BackgroundDataActivity extends MultiScreenPickerActivity implements Constants {

    private static final String TAG = TAG_PREFIX + BackgroundDataActivity.class.getSimpleName();
    private static final String SYNC_RULES_FILE_NAME = "syncrules.rules";
    private static final String SYNC_RULES  = "SYNC_RULES";
    private static final String SYNC_RULE  = "SYNC_RULE";
    private static final String RULES  = "RULES";
    private static final String STATE_NODE  = "STATE";
    private static final String EXTRA_IMPORT_RULE = "com.motorola.intent.action.IMPORT_RULE";
    private static final String RULEKEY_PREFIX = "com.motorola.contextual.childRule.";
    private static final String KEY_TAG_S = "<IDENTIFIER>";
    private static final String KEY_TAG_E = "</IDENTIFIER>";
    private static final String RULEKEY_TAG = KEY_TAG_S + "ruleKey" + KEY_TAG_E;

    private String mRuleKey = null;
    private ArrayList<ListItem> mListItems = null;

    public interface State {
        final int NO_ITEM_SELECTED = -1;
        final int ALWAYS = 0;
        final int WHEN_USING_DEVICE = 1;
        final int WHEN_USING = 2;
    }

    private int mCheckedItem = State.NO_ITEM_SELECTED;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.backgrounddata_title));

        if (LOG_INFO)
            Log.i(TAG, "onCreate called");

        setHelpHTMLFileUrl(this.getClass());
        setContentView(createPicker().getView());
    }

    private Picker createPicker() {
        Picker picker = null;

        Intent configIntent = ActionHelper.getConfigIntent(getIntent().getStringExtra(EXTRA_CONFIG));

        // Setup item list to use with picker
        mListItems = new ArrayList<ListItem>();
        mListItems.add(new ListItem(null, getString(R.string.sync_on), getString(R.string.sync_on_desc), ListItem.typeONE, new Integer(State.ALWAYS), null));
        mListItems.add(new ListItem(null, getString(R.string.sync_when_using_device), null, ListItem.typeONE, new Integer(State.WHEN_USING_DEVICE), null));
        mListItems.add(new ListItem(null, getString(R.string.sync_off), getString(R.string.sync_off_desc), ListItem.typeONE, new Integer(State.WHEN_USING), null));
        if ((getHelpHTMLFileUrl() != null) && (configIntent == null)) {
            mListItems.add(new ListItem(R.drawable.ic_info_details, getString(R.string.help_me_choose), null, ListItem.typeTHREE, new Integer(State.NO_ITEM_SELECTED), new onHelpItemSelected()));
        }

        if (configIntent != null) {
            int state = configIntent.getIntExtra(EXTRA_MODE, State.NO_ITEM_SELECTED);
            if (state != State.NO_ITEM_SELECTED) {
                for (int i = 0; i < mListItems.size(); i++) {
                    if ((Integer)mListItems.get(i).mMode == state) {
                        mCheckedItem = i;
                        break;
                    }
                }
            }

            mRuleKey = configIntent.getStringExtra(EXTRA_RULE_KEY);
        }
        if (mRuleKey == null) {
            mRuleKey = RULEKEY_PREFIX + new Date().getTime();
        }

        Picker.Builder builder = new Picker.Builder(this);
        builder.setTitle(Html.fromHtml(getString(R.string.sync_prompt)))
        .setOnKeyListener(Utils.sDisableSearchKey)
        .setSingleChoiceItems(mListItems, mCheckedItem, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                mCheckedItem = (Integer) mListItems.get(item).mMode;
            }
        })
        .setPositiveButton(getString(R.string.iam_done),
        new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int pos) {
                if (mCheckedItem != State.NO_ITEM_SELECTED) {
                    handleUserAction(mCheckedItem);
                }
            }
        });

        picker = builder.create();
        return picker;
    }

    // TODO Refactor based on cjd comment below
    // cjd - seems like this isn't a very descriptive method name.
    private void handleUserAction(int pos) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_CONFIG, getConfig(pos, mRuleKey));
        intent.putExtra(EXTRA_DESCRIPTION, mListItems.get(pos).mLabel);

       if (LOG_DEBUG) Log.d(TAG, "returning intent " + intent.toUri(0));
       setResult(RESULT_OK, intent);
       finish();
    }

    // TODO Refactor based on cjd comment below
    // cjd - seems like this isn't a very descriptive method name.
    private String getConfig(int mode, String key) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_STATE, true);
        intent.putExtra(EXTRA_MODE, mode);
        intent.putExtra(EXTRA_RULE_KEY, mRuleKey);
        intent.putExtra(EXTRA_IMPORT_RULE, buildRuleXml(mode));
        if (LOG_INFO) Log.i(TAG, "getConfig : " +  intent.toUri(0));
        return intent.toUri(0);
    }

    /** Retrieves the state from the fireUri
     * @param fireUri
     * @return state contained in fireUri
     */
    protected static int getStateFromFireUri(String fireUri) {
        int state = State.NO_ITEM_SELECTED;
        try {
            // Convert the Uri to an intent to get at the fields
            Intent fireIntent = Intent.parseUri(fireUri, 0);
            state = fireIntent.getIntExtra(EXTRA_STATE, State.NO_ITEM_SELECTED);
        } catch (URISyntaxException e) {
            Log.w(TAG, "Exception when retrieving from fireUri");
        }

        return state;
    }

    /** Retrieves the state from the fireUri
     * @param fireUri
     * @return ruleKey contained in fireUri
     */
    protected static String getRuleKeyFromFireUri(String fireUri) {
        String ruleKey = null;
        try {
            // Convert the Uri to an intent to get at the fields
            Intent fireIntent = Intent.parseUri(fireUri, 0);
            ruleKey = fireIntent.getStringExtra(EXTRA_RULE_KEY);
        } catch (URISyntaxException e) {
            Log.w(TAG, "Exception when retrieving from fireUri");
        }

        return ruleKey;
    }

   /** Returns the selected item
    *
    * @return
    */
   protected final int getCheckedItem() {
       return mCheckedItem;
   }

   // TODO Refactor based on cjd comment below
   // cjd - name of this method is not very descriptive
   private String buildRuleXml(int pos) {
        switch (pos) {
        case State.ALWAYS :
            return getXmlString(this, State.ALWAYS, mRuleKey);

        case State.WHEN_USING :
            return getXmlString(this, State.WHEN_USING, mRuleKey);

        case State.WHEN_USING_DEVICE :
            return getXmlString(this, State.WHEN_USING_DEVICE, mRuleKey);

        default:
            return null;
        }
    }

   // TODO Refactor based on cjd comment below
   //    the method name isn't very descriptive, what is the XML that is returned?
   // cjd - method seems really long. Your team probably didn't write it, but it's hard to understand what it's doing.
   private static String getXmlString(Context context, int state, String ruleKey) {
        String ruleString = null;
        String xmlString = Utils.readFilefromAssets(context, SYNC_RULES_FILE_NAME);

        if(xmlString == null)
            return null;

         NodeList nodes = Utils.getAllRelevantNodesFromXml(xmlString, SYNC_RULES);
         if(nodes != null) {
             for ( int i = 0; i < nodes.getLength(); i++ ) {
                 Node curNode = nodes.item(i);
                 String curNodeName = curNode.getNodeName();

                 if (curNodeName.equalsIgnoreCase(SYNC_RULE)) {
                     NodeList childNodes = curNode.getChildNodes();
                     if(childNodes != null) {
                         for ( int j = 0; j < childNodes.getLength(); j++ ) {
                             Node curChildNode = childNodes.item(j);

                             String curChildNodeName = curChildNode.getNodeName();
                             if (curChildNodeName.equalsIgnoreCase(STATE_NODE)) {
                                 String curChildNodeValue = curChildNode.getFirstChild().getNodeValue();
                                 try {
                                     if (Integer.parseInt(curChildNodeValue) != state) {
                                         ruleString = null;
                                         break;
                                     }
                                 } catch (NumberFormatException e) {
                                     Log.w(TAG, "exception parsing curChildNodeValue " + curChildNodeValue);
                                 }
                             }
                             if (curChildNodeName.equalsIgnoreCase(RULES)) {
                                 ruleString = Utils.getXmlTreeIn(curChildNode);
                                 //Replace rule key with incoming rulekey
                                 ruleString = ruleString.replaceFirst(RULEKEY_TAG, KEY_TAG_S +ruleKey+ KEY_TAG_E);
                             }
                         }
                     }
                     if (ruleString != null) break;
                 }
             }
         }
         return ruleString;
    }
}
