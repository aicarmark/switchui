/*
 * @(#)RuleActionView.java
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

import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;

public class RuleActionView extends RuleView {
	
	//private static final String TAG = RuleActionView.class.getSimpleName();
	
	/** name of the view in the database */
	public static final String VIEW_NAME = RuleActionView.class.getSimpleName(); 
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");   		

	
	public interface Columns extends RuleTable.Columns, 
										ActionTable.Columns {
		
		public static final int MAX_COLUMNS =  	RuleTable.getColumnNames().length+
												ActionTable.getColumnNames().length;
				
	}
	
	
	/** SQL that constructs the view in the database */
	public static final String CREATE_VIEW_OUTER_JOIN =
		CREATE_VIEW+VIEW_NAME +AS+ 
			SELECT_ALL+FROM+
				RuleTable.TABLE_NAME+AS+RuleTable.SQL_REF+
					LEFT_OUTER_JOIN +ActionTable.TABLE_NAME+AS+ActionTable.SQL_REF+ 
						ON +ActionTable.SQL_REF+"."+ActionTable.Columns.PARENT_FKEY+EQUALS+
							RuleTable.SQL_REF+"."+RuleTable.Columns._ID+ 
					ORDER_BY+RuleTable.SQL_REF+"."+RuleTable.Columns.NAME;	
	

	/** basic constructor
	 */
	public RuleActionView() {
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
