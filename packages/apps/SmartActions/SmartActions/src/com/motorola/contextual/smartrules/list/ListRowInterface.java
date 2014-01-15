/*
 * @(#)ListRowInterface.java
 *
 * (c) COPYRIGHT 2011 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/01/23 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.list;


import com.motorola.contextual.smartrules.db.business.RulePersistence;
import com.motorola.contextual.smartrules.db.table.IconTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;

/** This class contains constants related to list rows.
 * 
 *<pre>
 * INTERFACE:
 * 	all constants
 *
 * RESPONSIBILITIES:
 * - contains constants related to list rows.
 *
 * COLABORATORS:
 *  None.
 *
 * USAGE:
 *   N/A - no methods.
 *
 **/
public interface ListRowInterface {

    // output columns for the customizeRow method:
    public static final String LIST_LINE_1_KEY 		= "LL1";
    public static final String LIST_LINE_2_KEY 		= "LL2";
    public static final String LIST_LINE_3_KEY 		= "LL3";  
    public static final String RULE_ICON_KEY		= "RI";
    
    /** all row types - used so that we can know which type of rule list row the 
     * operation is occurring upon */
    // TODO: Convert to a enum or interface.
    public static final String LIST_ROW_TYPE_KEY 	= "TYP";
	public static final int LIST_ROW_TYPE_SUGGESTIONS  = 1;
	public static final int LIST_ROW_TYPE_SAMPLES 	= 2;
	public static final int LIST_ROW_TYPE_BLANK 	= 3;
	public static final int LIST_ROW_TYPE_AUTO		= 4;
	public static final int LIST_ROW_TYPE_MANUAL	= 5;
           
    /** names of the columns typically used in a ListRow */
	public static final String[] RELATE_DB_COLUMNS = {
		
		RuleTable.Columns._ID,
		RuleTable.Columns.KEY,
		RuleTable.Columns.NAME,
		RuleTable.Columns.ACTIVE,
		RuleTable.Columns.RULE_TYPE,
		RuleTable.Columns.ICON,
		RuleTable.Columns.ENABLED,
		RuleTable.Columns.ADOPT_COUNT,
		RuleTable.Columns.DESC,
		RuleTable.Columns.SUGGESTED_STATE,
		RuleTable.Columns.SOURCE,
		RulePersistence.SAMPLE_RULE_ADOPTED_COUNT,
		RulePersistence.FAIL_COUNT,
		RulePersistence.SUGGESTION_ACTION_COUNT,
		RulePersistence.SUGGESTION_CONDITION_COUNT,		
		RulePersistence.LOCATION_BLOCK_COUNT,
		RuleTable.Columns.PUBLISHER_KEY,
		RuleTable.Columns.VALIDITY,
		IconTable.Columns.ICON

	};
    
    /** classes corresponding to the column names above. */	
	public static final Class<?>[] objectCorrelationClasses = {
		
		long.class,
		String.class,
		String.class,
		int.class,
		int.class,
		String.class,
		int.class,
		int.class,
		String.class,
		int.class,
		int.class,
		int.class,
		int.class,
		int.class,
		int.class,
		int.class,
		String.class,
		String.class,
		byte[].class
	};   
}