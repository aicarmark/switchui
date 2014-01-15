/*
 * @(#)ActiveSettingsView.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/11/08 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.db.table.view;

import android.net.Uri;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;

/** This class is a view to get the active actions/settings that are currently 
 * 	controlled by Smart Actions. 
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
 * Create a view in the DB that returns the cursor of current actively set actions
 * or settings.
 *  
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class ActiveSettingsView extends ViewBase implements Constants, DbSyntax {
	
	public static final String VIEW_NAME = ActiveSettingsView.class.getSimpleName();
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");
	
	public interface Columns {
		
        public static final String ACTION_ID = "Action_id";

        public static final String[] NAMES = {RuleTable.Columns._ID,
												RuleTable.Columns.NAME,
												RuleTable.Columns.ICON,
												ActionTable.Columns.ACTION_DESCRIPTION,
        										ActionTable.Columns.MODAL,        										
        										ActionTable.Columns.ACTION_PUBLISHER_KEY,
        										ActionTable.Columns.STATE_MACHINE_NAME,
        										ActionTable.Columns.TARGET_STATE,
											    ActionTable.Columns.CONFIG,
        										ActionTable.Columns.ACTIVITY_INTENT,
        										ActionTable.Columns.ICON,
        										ACTION_ID};
        
		public static final int MAX_COLUMNS = NAMES.length;
	}

	/** select clause
	 */
	public static final String SELECT_CLAUSE = SELECT +
			  RuleTable.SQL_REF+"."+RuleTable.Columns._ID					+CONT+
			  RuleTable.SQL_REF+"."+RuleTable.Columns.NAME					+CONT+
			  RuleTable.SQL_REF+"."+RuleTable.Columns.ICON					+CONT+
			  ActionTable.SQL_REF+"."+ActionTable.Columns.ACTION_DESCRIPTION+CONT+
			  ActionTable.SQL_REF+"."+ActionTable.Columns.MODAL				+CONT+
			  ActionTable.Columns.ACTION_PUBLISHER_KEY						+CONT+
			  ActionTable.Columns.STATE_MACHINE_NAME						+CONT+
			  ActionTable.Columns.TARGET_STATE								+CONT+
			  ActionTable.Columns.CONFIG     								+CONT+
			  ActionTable.Columns.ACTIVITY_INTENT							+CONT+
			  ActionTable.Columns.ICON										+CONT+
			  ActionTable.SQL_REF+"."+ActionTable.Columns._ID+AS+Columns.ACTION_ID;
	
	/** where clause
	 */
	public static final String ACTIVE_WHERE_CLAUSE = WHERE + 
			RuleTable.SQL_REF+'.'+RuleTable.Columns.ENABLED+EQUALS+RuleTable.Enabled.ENABLED
			  +AND+  
			    RuleTable.SQL_REF+'.'+RuleTable.Columns.FLAGS+IS_NOT_LIKE+Q+WILD+RuleTable.Flags.INVISIBLE+Q+
			     AND+
				  RuleTable.SQL_REF+'.'+RuleTable.Columns.ACTIVE+EQUALS+RuleTable.Active.ACTIVE+ 
				   AND+
				    RuleTable.SQL_REF+'.'+RuleTable.Columns.KEY+NOT_EQUAL+Q+Constants.DEFAULT_RULE_KEY+Q+
				     AND+
				      ActionTable.SQL_REF+'.'+ActionTable.Columns.ACTIVE+EQUALS+ActionTable.Active.ACTIVE+ 
				       AND+
				   	    ActionTable.SQL_REF+'.'+ActionTable.Columns.MODAL+EQUALS+ModalTable.Modality.STATEFUL+
				   	     AND+
				          ActionTable.SQL_REF+'.'+ActionTable.Columns.CONFLICT_WINNER_FLAG+EQUALS+ActionTable.ConflictWinner.WINNER;

	/** from clause
	 */
	private static final String FROM_CLAUSE = FROM + ActionTable.TABLE_NAME+AS+ActionTable.SQL_REF;
	
	/** inner join clause
	 */
	private static final String INNER_JOIN_CLAUSE = INNER_JOIN+RuleTable.TABLE_NAME+AS+RuleTable.SQL_REF+
						ON+ActionTable.SQL_REF+"."+ActionTable.Columns.PARENT_FKEY+EQUALS+RuleTable.SQL_REF+'.'+RuleTable.Columns._ID;
		
	/** group by clause
	 */
	private static final String GROUP_BY_CLAUSE = GROUP_BY+ActionTable.Columns.ACTION_PUBLISHER_KEY;
	
	/** order by clause
	 */
	private static final String ORDER_BY_CLAUSE = ORDER_BY+ActionTable.Columns.ACTION_DESCRIPTION;
	
	/** SQL that constructs the view in the database */
	public static final String CREATE_VIEW =
				DbSyntax.CREATE_VIEW+VIEW_NAME +AS+
				  SELECT_CLAUSE+
				  FROM_CLAUSE+
				  INNER_JOIN_CLAUSE+
				  ACTIVE_WHERE_CLAUSE+
				  GROUP_BY_CLAUSE+
				  ORDER_BY_CLAUSE;
	
	/** basic constructor
	 */
	public ActiveSettingsView() {
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
