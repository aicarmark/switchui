/*
 * @(#)Schema.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2010/11/15 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db;


import android.net.Uri;

import com.motorola.contextual.smartrules.db.table.*;
import com.motorola.contextual.smartrules.db.table.view.ActiveLocCntView;
import com.motorola.contextual.smartrules.db.table.view.ActiveSettingsView;
import com.motorola.contextual.smartrules.db.table.view.AdoptedSampleListView;
import com.motorola.contextual.smartrules.db.table.view.DistinctConditionView;
import com.motorola.contextual.smartrules.db.table.view.RuleActionView;
import com.motorola.contextual.smartrules.db.table.view.RuleCloneView;
import com.motorola.contextual.smartrules.db.table.view.RuleConditionView;
//import com.motorola.contextual.smartrules.db.table.view.RuleIconView;
import com.motorola.contextual.smartrules.db.table.view.RuleView;
import com.motorola.contextual.smartrules.db.table.view.RuleViewCnt;
import com.motorola.contextual.smartrules.db.table.view.TriggerStateCountView;
import com.motorola.contextual.smartrules.db.table.view.VisibleEnaAutoRulesCntView;


/** this allows other packages (when copied in)
 * to easily reference all of the tables and views
 *
 */
public interface Schema extends DbSyntax {

    /** tables
     */
    public static final 	Uri	RULE_TABLE_CONTENT_URI = RuleTable.CONTENT_URI;
    public static interface RuleTableColumns extends RuleTable.Columns {}

    public static final 	Uri	ICON_TABLE_CONTENT_URI = IconTable.CONTENT_URI;
    public static interface IconTableColumns extends IconTable.Columns {}

    public static final 	Uri	ACTION_TABLE_CONTENT_URI = ActionTable.CONTENT_URI;
    public static interface ActionTableColumns extends ActionTable.Columns {}

    public static final 	Uri	CONDITION_TABLE_CONTENT_URI = ConditionTable.CONTENT_URI;
    public static interface ConditionTableColumns extends ConditionTable.Columns {}

    /** virtual views
     */
    public static final		Uri	ACTIVE_LOC_CNT_VIEW_CONTENT_URI = ActiveLocCntView.CONTENT_URI;
    public static final		Uri	VISIBLE_ENA_AUTO_RULES_CNT_VIEW_CONTENT_URI = VisibleEnaAutoRulesCntView.CONTENT_URI;
    public static final     Uri RULE_CLONE_URI = RuleCloneView.CONTENT_URI;
    public static final		Uri ADOPTED_SAMPLES_LIST_VIEW_CONTENT_URI = AdoptedSampleListView.CONTENT_URI;
   
    /** views
     */
    public static final 	Uri	RULE_VIEW_CONTENT_URI = RuleView.CONTENT_URI;
    public static interface RuleViewColumns extends RuleView.Columns {}

    public static final 	Uri	RULE_ACTION_VIEW_CONTENT_URI = RuleActionView.CONTENT_URI;
    public static interface RuleActionColumns extends RuleActionView.Columns {}

    public static final 	Uri	RULE_CONDITION_VIEW_CONTENT_URI = RuleConditionView.CONTENT_URI;
    public static interface RuleConditionColumns extends RuleConditionView.Columns {}

    public static final 	Uri	RULE_VIEW_CNT_CONTENT_URI = RuleViewCnt.CONTENT_URI;
    public static interface RuleViewCntColumns extends RuleViewCnt.Columns {}

    public static final		Uri TRIGGER_STATE_CNT_VIEW_CONTENT_URI = TriggerStateCountView.CONTENT_URI;
    
    public static final		Uri	ACTIVE_SETTINGS_VIEW_CONTENT_URI = ActiveSettingsView.CONTENT_URI;       

    public static final     Uri DISTINCT_CONDITION_VIEW_CONTENT_URI = DistinctConditionView.CONTENT_URI;
    public static interface DistinctConditionColumns extends DistinctConditionView.Columns {}
}
