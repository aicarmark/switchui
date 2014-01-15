/*
 * @(#)RuleConditionView.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2010/10/28 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.db.table.view;

import android.net.Uri;
import android.provider.BaseColumns;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;

public class RuleViewCnt extends ViewBase implements DbSyntax, Constants  {

	
	/** name of the view in the database */
	public static final String VIEW_NAME = RuleViewCnt.class.getSimpleName(); 
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");   		

	
	public interface Columns extends RuleTable.Columns {

		public static final String CONDITION_ROW_COUNT = "CondRowCnt";
				
		public static final String[] NAMES = {_ID, ENABLED, RANK, KEY, ACTIVE, RULE_TYPE, SOURCE, 
			COMMUNITY_RATING, COMMUNITY_AUTHOR,
			FLAGS, NAME, DESC, TAGS, CONDITIONS, 
			INFERENCE_LOGIC, INFERENCE_STATUS, SUGGESTED_REASON, 
			 RULE_SYNTAX,
			LAST_ACTIVE_DATE_TIME, LAST_INACTIVE_DATE_TIME, 
			CREATED_DATE_TIME, ICON, CONDITION_ROW_COUNT};
		
		public static final int MAX_COLUMNS =  	RuleTable.getColumnNames().length+1;

	}
		
	/** SQL that constructs the view in the database */
	public static final String CREATE_VIEW =
		DbSyntax.CREATE_VIEW+VIEW_NAME +AS+ 
			SELECT+RuleTable.SQL_REF+"."+ALL+	CONT+
					COUNT+LP+ConditionTable.SQL_REF+"."+BaseColumns._ID+RP+AS+Columns.CONDITION_ROW_COUNT+
			  FROM+
			  	RuleTable.TABLE_NAME+AS+RuleTable.SQL_REF+
					LEFT_OUTER_JOIN +ConditionTable.TABLE_NAME+AS+ConditionTable.SQL_REF+
						ON +ConditionTable.SQL_REF+"."+ConditionTable.Columns.PARENT_FKEY+EQUALS+
							RuleTable.SQL_REF+"."+RuleTable.Columns._ID+
			GROUP_BY+RuleTable.SQL_REF+"."+RuleTable.Columns._ID;	
			//	ORDER_BY+RuleTable.SQL_REF+"."+RuleTable.Columns.NAME;	
	

	/** basic constructor
	 */
	public RuleViewCnt() {
		super();
	}
	
	
	/* (non-Javadoc)
	 * @see com.motorola.contextual.smartrules.db.table.view.RuleView#getViewName()
	 */
	@Override
	public String getViewName() {
		return VIEW_NAME;
	}


	
}
