/*
 * @(#)TriggerStateCountView.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/09/12 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.db.table.view;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;

import android.net.Uri;

/** This class is a view to get the publisher key and count of enabled conditions in
 *  the DB. This count is for all accepted and inferred rules in the DB. 
 * 
 *<code><pre>
 * CLASS:
 * 	 extends ViewBase
 *
 *  implements
 *   Constants - for the constants used
 *   DbSyntax - for the DB related constants
 *  
 * RESPONSIBILITIES:
 * Create a view in the DB that indexes on the publisher key and returns the count
 * of enabled conditions for each publisher key from the set of publisher keys passed.
 * If the count is zero then that publisher key is not returned and if none of the passed
 * publisher keys have a count then an empty cursor is returned.
 *  
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class TriggerStateCountView extends ViewBase implements DbSyntax, Constants {
	
	private static final String STRING_SEPARATOR = Q + "^" + Q;
	
	public static final String VIEW_NAME = TriggerStateCountView.class.getSimpleName(); 
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");
	
	public interface Columns {
		
		public static final String TRIGGER_COUNT_BY_STATUS = "TriggerByStatusCount";
		public static final String TRIGGER_ACTIVITY_INTENTS = "TriggerStateActIntents";
		
		public static final String[] NAMES = {TRIGGER_COUNT_BY_STATUS,
												RuleTable.Columns.ENABLED,
												ConditionTable.Columns.CONDITION_PUBLISHER_KEY,
												ConditionTable.Columns.ENABLED,
												TRIGGER_ACTIVITY_INTENTS};
		
		public static final int MAX_COLUMNS = NAMES.length;
	}

	/** fetch for only conditions that are enabled and the rule is either inferred, user 
	 *  created or suggested and user accepted or adopted sample rule.
	 */
	private static final String WHERE_RUNNING_CLAUSE = 
		ConditionTable.Columns.ENABLED + EQUALS + Q + ConditionTable.Enabled.ENABLED + Q
		+ AND 
			+ LP
				+ RuleTable.Columns.SOURCE + IN
				+ LP
					+ RuleTable.Source.INFERRED + COMMA 
					+ RuleTable.Source.USER + COMMA
					+ RuleTable.Source.FACTORY
				+ RP
				+ OR
					+ LP
						+ RuleTable.Columns.SOURCE + EQUALS + Q + RuleTable.Source.SUGGESTED + Q
						+ AND
						+ RuleTable.Columns.SUGGESTED_STATE + EQUALS + Q + RuleTable.SuggState.ACCEPTED + Q
					+ RP
			+ RP
		+ AND 
			+ RuleTable.Columns.FLAGS + IS_NOT_LIKE
				+ Q + WILD + RuleTable.Flags.INVISIBLE + WILD + Q
		+ AND
			+ RuleTable.Columns.FLAGS + IS_NOT_LIKE
				+ Q + WILD + RuleTable.Flags.SOURCE_LIST_VISIBLE + WILD + Q 
		;
	
	/** from clause
	 */
	private static final String FROM_CLAUSE = FROM+ConditionTable.TABLE_NAME+AS+ConditionTable.SQL_REF+CONT+
												RuleTable.TABLE_NAME+AS+RuleTable.SQL_REF;
	
	/** join clause 
	 */
	private static final String WHERE_JOIN_CLAUSE = WHERE+ConditionTable.SQL_REF+"."+ConditionTable.Columns.PARENT_FKEY+
													EQUALS+RuleTable.SQL_REF+"."+RuleTable.Columns._ID;
	
	/** group by clause - group by the condition publisher key and rule enabled/disabled.
	 */
	private static final String GROUP_BY_CLAUSE = GROUP_BY+ConditionTable.Columns.CONDITION_PUBLISHER_KEY
													+CONT
													+RuleTable.Columns.ENABLED;
	
	/** SQL that constructs the view in the database */
	public static final String CREATE_VIEW =
		DbSyntax.CREATE_VIEW+VIEW_NAME +AS+ 
			SELECT+COUNT+LP+RuleTable.Columns.ENABLED+RP+AS+Columns.TRIGGER_COUNT_BY_STATUS+CONT+
					RuleTable.SQL_REF+"."+RuleTable.Columns.ENABLED+CONT+
					ConditionTable.SQL_REF+"."+ConditionTable.Columns.CONDITION_PUBLISHER_KEY+CONT+
					ConditionTable.SQL_REF+"."+ConditionTable.Columns.ENABLED+CONT+
					GROUP_CONCAT + ConditionTable.Columns.ACTIVITY_INTENT+CS+STRING_SEPARATOR+RP+AS+Columns.TRIGGER_ACTIVITY_INTENTS
					+FROM_CLAUSE
					+WHERE_JOIN_CLAUSE
					+AND
					+WHERE_RUNNING_CLAUSE
					+GROUP_BY_CLAUSE;

	/** basic constructor
	 */
	public TriggerStateCountView() {
		super();
	}

	/** gets the name of the view for this view.
	 * 
	 * @see com.motorola.contextual.smartrules.db.table.view.ViewBase#getViewName()
	 * 
	 * @return - name of this view in the datbase.
	 */
	@Override
	public String getViewName() {
		return VIEW_NAME;
	}	
}
