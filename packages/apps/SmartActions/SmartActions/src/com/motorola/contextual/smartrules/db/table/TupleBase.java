/*
 * @(#)TupleBase.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table;

import android.os.Parcel;

import com.motorola.contextual.smartrules.util.ParcelableBase;

/**This class is a base class for any tuple abstraction in the system.
 *
 *<code><pre>
 * CLASS:
 *	extends ParcelableBase for parceling of tuple instances.
 *
 * RESPONSIBILITIES:
 * 	create Tuple instance from fields or from parcel.
 * 	support get and set for row id (_id).
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class TupleBase extends ParcelableBase implements Cloneable {

    /** primary key of the tuple */
    protected long 	_id;
    /** dirty flag to indicate edited */
    protected boolean dirtyFlag = false;
    /** marked for deletion in the tuple */
    protected boolean logicalDelete = false;

    /** basic constructor */
    protected TupleBase() {
        super();
        this.set_id(-1);
    }

    /** constructor from parcel.
     * @param parcel - input parcel
     */
    protected TupleBase(Parcel parcel) {
        super(parcel);
    }

    /** getter - rowId.
     *
     * @return the rowId
     */
    public long get_id() {
        return _id;
    }

    /** setter - rowId.
     *
     * @param rowId the rowId to set
     */
    public void set_id(long rowId) {
        this._id = rowId;
    }

	/** This function will set the primary key value to zero
     * which will cause the various Table insert function to not insert the
     * _id value as one of the ContentValues. Therefore, those insert functions
     * will generate a new primary key.
     */
    public void set_idToGenerateNewKeyOnInsert() {
        this._id = 0;
    }
   
    /** getter - dirtyFlag
     * 
     * @return the dirtyFlag value (true or false)
     */
    public boolean isDirtyFlag() {
		return dirtyFlag;
	}
   
    /** clears the dirtyFlag value (sets it to false)
     */
	public void clearDirtyFlag() {
		this.dirtyFlag = false;
	}
	
	/** getter - logicalDelete
	 * 
	 * @return the logicalDelete value (true or false)
	 */
	public boolean isLogicalDelete() {
		return logicalDelete;
	}

	/** setter - logically deletes the tuple (used for update)
	 */
	public void setLogicallyDeleted() {
		this.logicalDelete = true;
	}
	
	/** clears the logicalDelete value (sets it to false)
	 */
	public void clearLogicalDelete() {
		this.logicalDelete = false;
	}
	
	/** getter - to check if the element is new
	 * 
	 * @return - true if the element is new else false
	 */
	public boolean isNew() {
		return this._id == -1;
	}
	
    /** toString - rowId.
     * @return rowId - rowId in string form.
     */
    public String toString() {
        return " _id="+_id;
    }

    /** this function sets the primary key value to the id in the
     *  tuple passed in and returns the tuple.
     * 
     * @param t - tuple
     * @return - this
     */
    public TupleBase copy(TupleBase t) {
        this._id = t._id;
		return this;  	
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
