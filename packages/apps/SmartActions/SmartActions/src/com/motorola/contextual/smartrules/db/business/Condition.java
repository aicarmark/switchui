/*
 * @(#)Condition.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA MOBILITY, INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/05/09 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import android.os.Parcel;
import android.os.Parcelable;

import com.motorola.contextual.smartrules.db.table.ConditionTuple;
import com.motorola.contextual.smartrules.db.table.ModalTable;
import com.motorola.contextual.smartrules.uipublisher.ConditionPublisher;

/** This implements the business rules around a single SmartRule (Pre)Condition. 
*
*<code><pre>
* CLASS:
* 	Extends ConditionTuple which implements the basic methods of storing and retrieving a 
*                      precondition (but has no business logic) 
*
* RESPONSIBILITIES:
* 	implement business-layer of the Condition.
*
* COLABORATORS:
* 	Rule - this class is a child class to Rule, which implements the business layer of Rules.
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class Condition extends ConditionTuple implements Parcelable{

	/** basic constructor
	 */
	public Condition() {
		super();
	}
	
	/** Constructor based on a ConditionPublisher. The caller needs to set the state syntax
	 *  once the Condition object is returned.
	 * 
	 * @param condPublisher - Condition Publisher
	 */
	public Condition(ConditionPublisher condPublisher) {
		super();
		this.setActivityIntent(condPublisher.getActivityPkgUri());
		this.setPublisherKey(condPublisher.getPublisherKey());
		this.setSensorName(condPublisher.getBlockName());
		this.setDescription(condPublisher.getBlockDescription());
		this.setModality(ModalTable.convertPkgMgrModalityType(condPublisher.getStateType()));			
	}
	
	/** tuple constructor
	 * 
	 * @param t - condition tuple
	 */
	public Condition(ConditionTuple t) {
		super(t);
	}

	/** constructor for parcel re-inflate.
    *
    * @param parcel to reconstruct the instance
    */
    public Condition(Parcel parcel) {
	super(parcel);
    }

    /** creator for parceling */
    public static final Parcelable.Creator<Condition> CREATOR =
    new Parcelable.Creator<Condition>() {

        public Condition createFromParcel(Parcel in) {
            return new Condition(in);
        }

        public Condition [] newArray(int size) {
            return new Condition[size];
        }
    };

    @Override
	public void writeToParcel(Parcel dest, int flags) {
	super.writeToParcel(dest, flags);
    }
	/** resets the persistent fields for the condition and calls the same kind
	 * 	of handler for the ConditionSensorList that is part of the Condition.
	 */
	public void resetPersistentFields() {
		this.set_id(-1);
		this.setLastFailDateTime(0);
		this.setCreatedDateTime(0);	
		this.setParentFkey(-1);
		this.setCondFailMsg(null);
	}
}
