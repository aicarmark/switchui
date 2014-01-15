/*
 * @(#)SuggestionsInboxActivity.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/04/09 NA                Initial version
 *
 */
package com.motorola.contextual.smartrules.suggestions;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ImageView;
import android.widget.TextView;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.app.DisplayRulesActivity;
import com.motorola.contextual.smartrules.app.LandingPageActivity;
import com.motorola.contextual.smartrules.db.Schema.RuleTableColumns;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.business.SuggestionsPersistence;
import com.motorola.contextual.smartrules.db.table.RuleTable;

/**
 * This class displays suggestions as a List view
 * <code><pre>
 * CLASS:
 *  extends DisplayRulesActivity
 *
 *  implements
 *      OnLongClickListener - To handle long press clicks
 *
 * RESPONSIBILITIES:
 *      1. Show read/unread suggestions
 *      2. Show dialog to view/delete suggestions
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *  See each method.
 * </pre></code>
 */
public class SuggestionsInboxActivity extends DisplayRulesActivity {

    @SuppressWarnings("unused")
	private static final String TAG = SuggestionsInboxActivity.class.getSimpleName();

    /** interface for menu options shown on long click
     */
    private interface MenuOptions {
    	final int VIEW = 0; 
    	final int DELETE = VIEW + 1;
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.contextual.smartrules.app.DisplayRulesActivity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove notification bar
        Suggestions.removeNotification(mContext);

        // User has seen the inbox, remove init state
        Suggestions.setInitState(mContext, false);
    }

    /*
     * (non-Javadoc)
     * @see com.motorola.contextual.smartrules.app.DisplayRulesActivity#setViewValue(android.view.View, java.lang.Object, String)
     */
    @Override
	public boolean setViewValue(View view, Object data, String textRepresentation) {

        int id = view.getId();
        boolean result = false;
        Rule rule = null;
        
        switch(id) {
            case R.id.display_rules_first_line:{
                if(view instanceof TextView) {
                	rule = (Rule) view.getTag();
                    TextView tv = (TextView)view;
                    tv.setText(rule.getName());
                    if (rule.getSuggState() == RuleTable.SuggState.UNREAD) // 1 == unread
                        tv.setTextAppearance(mContext, R.style.Suggestion_Title_Unread);
                    else
                        tv.setTextAppearance(mContext, R.style.Suggestion_Title_Read);
                    result = true;
                }
                break;
            }

            case R.id.display_rules_mode_icon:{
                if(view instanceof ImageView) {
                    ((ImageView) view).setVisibility(View.GONE);
    	            ((View)view.getParent().getParent()).setTag(rule);
    	            ((View)view.getParent().getParent()).setOnCreateContextMenuListener(this);
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /** onCreateContextMenu()
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {     
    	mClickedRowRule = (Rule) view.getTag();

        if(mClickedRowRule != null) {
            // Sets the menu header to be the title of the selected note.
            menu.setHeaderTitle(mClickedRowRule.getName());
            menu.add(0, MenuOptions.VIEW, 0, R.string.view);
            menu.add(0, MenuOptions.DELETE, 0, R.string.delete);

            // disable rule row to avoid accidental double clicks
            view.setEnabled(false);
        }
    }

    /**
     *  Refresh the rule list on dialog close.
     *
     * (non-Javadoc)
     * @see android.app.Activity#onContextMenuClosed(android.view.Menu)
     */
    @Override
    public void onContextMenuClosed(Menu menu) {
        showRulesList();
        super.onContextMenuClosed(menu);
    }

    /** onContextItemSelected()
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case MenuOptions.VIEW:
                Suggestions.showSuggestionDialog(mContext, mClickedRowRule.get_id());
                return true;

            case MenuOptions.DELETE:
                // Is this a rule or just a suggestion
                if(mClickedRowRule.getFlags() != null 
                	&& mClickedRowRule.getFlags().equals(RuleTable.Flags.SOURCE_LIST_VISIBLE)){

                    int source = RulePersistence.getColumnIntValue(mContext, mClickedRowRule.get_id(), RuleTableColumns.SOURCE);

                    if(source == RuleTable.Source.SUGGESTED){
                        // This is a suggestion and not yet accepted by the user as rule
                        // we could just delete this suggestion from RuleTable
                        RulePersistence.deleteRule(mContext, mClickedRowRule.get_id(), null, null, false);
                    } else if(source == RuleTable.Source.FACTORY){
                        // just remove from the inbox... no other changes
                        SuggestionsPersistence.setSuggestionState(mContext, mClickedRowRule.get_id(), RuleTable.SuggState.ACCEPTED);
                    }
                } else {

                    // This means this is an existing rule, do not delete it.
                    // Just delete associated actions that weren't accepted by the user
                    SuggestionsPersistence.deleteUnAcceptedActions(mContext, mClickedRowRule.get_id());
                    // Now just accept the rule, this will make it disappear from Inbox
                    SuggestionsPersistence.acceptSuggestion(mContext, mClickedRowRule.get_id());
                }
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * We should always launch Landing page on back press
     *
     * (non-Javadoc)
     * @see android.app.Activity#onBackPressed()
     */
    @Override
    public void onBackPressed() {
    	LandingPageActivity.startLandingPageActivity(mContext);
        super.onBackPressed();
    }
}