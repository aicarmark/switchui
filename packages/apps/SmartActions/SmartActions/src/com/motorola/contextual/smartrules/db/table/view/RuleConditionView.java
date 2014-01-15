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

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;

public class RuleConditionView extends RuleView {

	
	//private static final String TAG = RuleConditionView.class.getSimpleName();
	
	/** name of the view in the database */
	public static final String VIEW_NAME = RuleConditionView.class.getSimpleName(); 
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");   		

	
	public interface Columns extends RuleTable.Columns, 
										ConditionTable.Columns {
		
		public static final int MAX_COLUMNS =  	RuleTable.getColumnNames().length+
												ConditionTable.getColumnNames().length;
				
	}
		
	/** SQL that constructs the view in the database */
	public static final String CREATE_VIEW_OUTER_JOIN =
		DbSyntax.CREATE_VIEW+VIEW_NAME +AS+ 
			SELECT_ALL+FROM+
				RuleTable.TABLE_NAME+AS+RuleTable.SQL_REF+
					LEFT_OUTER_JOIN +ConditionTable.TABLE_NAME+AS+ConditionTable.SQL_REF+
						ON +ConditionTable.SQL_REF+"."+ConditionTable.Columns.PARENT_FKEY+EQUALS+
							RuleTable.SQL_REF+"."+RuleTable.Columns._ID+
					ORDER_BY+RuleTable.SQL_REF+"."+RuleTable.Columns.NAME;	
	

	/** basic constructor
	 */
	public RuleConditionView() {
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
