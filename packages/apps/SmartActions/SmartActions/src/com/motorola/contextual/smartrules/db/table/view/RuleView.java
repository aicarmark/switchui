/*
 * @(#)RuleView
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
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


import android.content.Context;
import android.net.Uri;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;


/** This class allows access and updates to the Rule table. Basically, it 
 * encapsulates the Rule table along with all the related actions and conditions.
 * 
 * The purpose of the RuleView table is to allow a view of the combined
 * rule, actions, conditions table rows. This differs from the
 * RuleTable class in that it ONLY manages the Rule table
 * records versus this combined view of the 3 tables (Rule, action, condition).
 * 
 * 
 *<code><pre>
 * CLASS:
 * 	Extends ViewBase which provides basic view fetches, etc.
 *  implements DbSyntax for constructing SQL statements.
 *
 * RESPONSIBILITIES:
 * 	Insert, delete, update, fetch Rule View records 
 * 
 *  A view is essentially a dynamically joined table. This allows it to be used like
 *  any table in any query. However, because of their dynamic nature, views are generally
 *  not updatable nor deletable. This class however, allows those update and delete 
 *  functions as it can update and delete individual records in the appropriate tables.
 *  
 * COLABORATORS:
 * 	RuleJoin - abstracts the "tuple" of the view.
 * 	RuleTable, ActionTable, ConditionTable - these are combined to produce this view.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class RuleView extends ViewBase implements DbSyntax, Constants {
	
	@SuppressWarnings("unused")
	private static final String TAG = RuleView.class.getSimpleName();
	public static final String VIEW_NAME = RuleView.class.getSimpleName(); 
	
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");   		

	
	/** name of the view in the database */
	
	public interface Columns extends RuleTable.Columns, 
										ActionTable.Columns, 
										ConditionTable.Columns {
		
		public static final int MAX_COLUMNS =  	RuleTable.getColumnNames().length+
												ActionTable.getColumnNames().length+
												ConditionTable.getColumnNames().length;
				
	}
	
		
	// this will cause a number of problems identifying record types, missing data, etc.
	/** SQL that constructs the view in the database */
	public static final String CREATE_VIEW_OUTER_JOIN =
		DbSyntax.CREATE_VIEW+VIEW_NAME +AS+ 
			SELECT_ALL+FROM+
				RuleTable.TABLE_NAME+AS+RuleTable.SQL_REF+
					LEFT_OUTER_JOIN +ActionTable.TABLE_NAME+AS+ActionTable.SQL_REF+ 
						ON +ActionTable.SQL_REF+"."+ActionTable.Columns.PARENT_FKEY+EQUALS+
							RuleTable.SQL_REF+"."+RuleTable.Columns._ID+ 
					LEFT_OUTER_JOIN +ConditionTable.TABLE_NAME+AS+ConditionTable.SQL_REF+
						ON +ConditionTable.SQL_REF+"."+ConditionTable.Columns.PARENT_FKEY+EQUALS+
							RuleTable.SQL_REF+"."+RuleTable.Columns._ID+
					ORDER_BY+RuleTable.SQL_REF+"."+RuleTable.Columns.NAME;	
	

	/** basic constructor
	 */
	public RuleView() {
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
	


	/** Dumps all 3 tables.
	 * 
	 * @param context
	 * @param prefix
	 */
	public static void dumpAll(Context context, String prefix) {
		
		if (! PRODUCTION_MODE) { 
		
	    	new RuleTable().dumpDebug(context, prefix);
	    	new ActionTable().dumpDebug(context, prefix);
	    	new ConditionTable().dumpDebug(context, prefix);
		}
	}

	
}