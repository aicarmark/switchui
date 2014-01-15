/*
 * @(#)ModalTable.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/01/26 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table;


/**This class is a base class for any table in the system using a modality flag which
 * needs to be sync'd to the PackageManager.
 *
 *<code><pre>
 * CLASS:
 *  extends TableBase to implement all the base methods for any table.
 *
 * RESPONSIBILITIES:
 * 	query the Modal column, set where unknown values exist
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public abstract class ModalTable extends TableBase {


    @SuppressWarnings("unused")
    private static final String TAG = ModalTable.class.getSimpleName();

    /** possible values for the Modal column. */
    public interface Modality {
        int STATEFUL 		= 1;
        int STATELESS 		= 0;
        int UNKNOWN 		= -1;
    }

    /** strings used in the package manager */
    public interface PkgMgr {

        public interface Type {
            String STATEFUL = "stateful";
            String STATELESS = "stateless";
        }
    }


    /**  converts the modality type from the package manager string to our discrete type
     * interface "Modality" value.
     *
     * @param type - the string to check for comparison to valid modality types
     * @return - the Modality type, @see Modality
     */
    public static int convertPkgMgrModalityType(final String type) {

        int result = Modality.UNKNOWN;
        if (PkgMgr.Type.STATEFUL.equals(type)) {
            result = Modality.STATEFUL;
        } else if (PkgMgr.Type.STATELESS.equals(type)) {
            result = Modality.STATELESS;
        }
        return result;
    }


}
