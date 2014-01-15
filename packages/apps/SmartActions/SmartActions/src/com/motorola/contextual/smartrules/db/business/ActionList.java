/*
 * @(#)ActionList.java
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

import com.motorola.contextual.smartrules.db.table.TupleBase;

/** This implements the business rules for an ActionList of Actions. 
*
*<code><pre>
* CLASS:
* 	Extends SmartActionsList 
*
* RESPONSIBILITIES:
* 	implement business-layer of the ActionList.
*
* COLABORATORS:
* 	Rule - this class is a child class to Rule, which implements the business layer of Rules.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class ActionList<E extends Action> extends SmartActionsList<E> {

	private static final long serialVersionUID = 5193933487225234043L;

	//private static final String TAG = ActionList.class.getSimpleName();
	
	/** returns a new ActionList.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ActionList getNewList() {
		return new ActionList<E>();
	}

	/** sets the parent FKey to the id passed in for each Action in the
	 *  ActionList.
	 * 
	 * @param id - rule ID that needs to be set as the ParentFKey
	 */
	public void setParentFKey(long id) {
		for(TupleBase action: this) {
			((Action) action).setParentFk(id);
		}		
	}
	
	/** reset the persistent fields for each Action in the ActionList.
	 *
	 * @param ruleKey - rule key of the rule
	 */
	public void resetPersistentFields(String ruleKey) {
		for(E tuple: this) {
			tuple.resetPersistentFields(ruleKey);
		}		
	}
}