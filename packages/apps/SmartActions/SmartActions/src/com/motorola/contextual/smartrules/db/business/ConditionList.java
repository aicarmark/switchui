/*
 * @(#)ConditionList.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/09/01 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import com.motorola.contextual.smartrules.db.table.ConditionTuple;
import com.motorola.contextual.smartrules.db.table.TupleBase;
import com.motorola.contextual.smartrules.db.table.ConditionTable.Enabled;

/** This implements the business rules for a ConditionList of Conditions. 
*
*<code><pre>
* CLASS:
* 	Extends SmartActionsList 
*
* RESPONSIBILITIES:
* 	implement business-layer of the ConditionList.
*
* COLABORATORS:
* 	Rule - this class is a child class to Rule, which implements the business layer of Rules.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class ConditionList<E extends Condition> extends SmartActionsList<E> {

	private static final long serialVersionUID = 1770092549726271393L;
	//private static final String TAG = ConditionList.class.getSimpleName();

	/** returns a new ConditionList.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ConditionList getNewList() {
		return new ConditionList<E>();
	}
	
	/** sets the parent FKey to the id passed in for each Condition in the
	 *  ConditionList
	 * 
	 * @param id - rule ID that needs to be set as the ParentFKey
	 */
	public void setParentFKey(long id) {
		for(TupleBase condition: this) {
			((ConditionTuple) condition).setParentFkey(id);
		}			
	}

	/** reset the persistent fields for each condition in the ConditionList
	 */
	public void resetPresistentFields() {
		for(Condition condition: this) {
			condition.resetPersistentFields();
		}		
	}
	
	/** returns the count of the enabled conditions in the condition list.
	 * 
	 * @return - count of enabled conditions
	 */
	public int getEnabledConditionsCount() {
		int count = 0;
		for (Condition condition : this) {
			if (condition.getEnabled() == Enabled.ENABLED)
				count++;
		}
		return count;
	}	
}
