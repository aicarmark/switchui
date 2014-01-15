/*
 * @(#)SmartActionsList.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA MOBILITY, INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/09/29 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import java.util.ArrayList;

import com.motorola.contextual.smartrules.db.table.TupleBase;

/** This is the base class for the list classes. 
*
*<code><pre>
* CLASS:
* 	Extends ArrayList which implements the basic methods of an array list. 
*
* RESPONSIBILITIES:
* 	Base class for the list classes (ActionList, ConditionList and ConditionSensorList).
*
* COLABORATORS:
* 	None.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public abstract class SmartActionsList<E extends TupleBase> extends ArrayList<E> {

	private static final long serialVersionUID = 2781350673941816654L;

	//private static final String TAG = SmartActionsList.class.getSimpleName();
	
	/** sets the ID column to the one passed in.
	 * 
	 * @param id - id to be set to
	 */
	public void setId(long id) {
		for(E tuple: this) {
			tuple.set_id(id);
		}
	}
	
	public abstract SmartActionsList<E> getNewList();
}
