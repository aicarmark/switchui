/*
 * @(#)IconTuple.java
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
package com.motorola.contextual.smartrules.db.table;



import java.util.Arrays;

import android.os.Parcel;
import android.os.Parcelable;

import com.motorola.contextual.smartrules.Constants;


/**This class abstracts or encapsulates one row of the Icon table.
 *
 * The Icon table is used here to hold a Rule Icon
 *
 *<code><pre>
 * CLASS:
 * 	Extends TupleBase which implements the row id (_id) field of the tuple.
 * 	Implements Parcelable to allow bundling.
 * 	Implements Comparable to allow sorting.
 * 	Implements Cloneable to allow cloning of records.
 *
 * RESPONSIBILITIES:
 * 	create instance from fields or from a parcel.
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class IconTuple extends TupleBase implements Cloneable, Parcelable, Comparable<IconTuple>, Constants {

    /** foreign key pointing to rule _id field */
    private   long	 	parentFk;

    private byte[] iconBlob;

    /** Basic constructor, constructs record class */
    public IconTuple() {
        super();
        this.dirtyFlag = false;
    }


    /** Constructor make a copy (clone) of the record
     *
     * @param tuple
     */
    public IconTuple(IconTuple tuple) {
        super();
        copy(tuple);
        this.dirtyFlag = false;
    }


    /** constructs record class */
    public IconTuple(			long 	_id,
                                long 	parentFk,
                                byte[]   iconBlob) {

        super();
        this._id 				= _id;
        this.parentFk 			= parentFk;
        if(iconBlob != null)
            this.iconBlob = iconBlob;
        this.dirtyFlag = false;
    }

    /** constructor for parcel re-inflate.
     *
     * @param parcel to reconstruct the instance
     */
    public IconTuple(Parcel parcel) {
        super(parcel);
    }

    /** clone a tuple.
     *
     * @param tuple - tuple to be cloned.
     * @return - new instance cloned from the old one.
     */
    public IconTuple copy(IconTuple tuple) {

        super.copy(tuple);
        this.parentFk 			= tuple.parentFk;
        this.iconBlob = tuple.iconBlob;
        this.dirtyFlag = false;
        return this;
    }


    /** write this tuple to a parcel for bundling.
     *
     * @see com.motorola.contextual.smartrules.util.ParcelableBase#writeToParcel(android.os.Parcel, int)
     */
    public void writeToParcel(Parcel dest, int flags) {

        super.writeToParcel(dest, flags);
    }


    /** creator for parceling */
    public static final Parcelable.Creator<IconTuple> CREATOR =
    new Parcelable.Creator<IconTuple>() {

        public IconTuple createFromParcel(Parcel in) {
            return new IconTuple(in);
        }

        public IconTuple [] newArray(int size) {
            return new IconTuple[size];
        }
    };


    /** getter - foreign key to the rule _id field/column
     *
     * @return the ruleFk
     */
    public long getParentFk() {
        return parentFk;
    }


    /** setter - foreign key to the rule _id field/column
     *
     * @param fk - the ruleFk to set
     */
    public IconTuple setParentFk(long fk) {

        this.parentFk = fk;
        this.dirtyFlag = true;
        return this;
    }

    /** getter - iconBlob used for the Rule
    *
    * @return the icon
    */
    public byte[] getIconBlob() {
        return iconBlob;
    }

    /** setter - icon of the Rule
    *
    * @param icon the icon to set for the rule
    */
    public void setIconBlob(byte[] iconBlob) {
        this.iconBlob = iconBlob;
        this.dirtyFlag = true;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof IconTuple)
            return compareTo((IconTuple)o) == 0;
        else
            return false;
    }


    @Override
    public int hashCode() {
        int result = Hash.SEED;
        result = Hash.hash( result, _id );
        result = Hash.hash( result, parentFk );
        result = Hash.hash( result, iconBlob );
        return result;
    }


    /** Comparison method for equals, not really good for sorting.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(IconTuple another) {

        if (this._id 				!= another._id) return 1;
        if (this.parentFk 			!= another.parentFk) return 1;
        if (!Arrays.equals(this.iconBlob, another.iconBlob)) return 1;
        return 0;
    }


    /** converts tuple to String mainly for debugging purposes.
     *
     * @see com.motorola.contextual.smartrules.db.table.TupleBase#toString()
     */
    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("  Icon."+super.toString());

        builder.append(", parentFk="+ parentFk)
        .append(", icon="+ iconBlob);
        return builder.toString();
    }
}
