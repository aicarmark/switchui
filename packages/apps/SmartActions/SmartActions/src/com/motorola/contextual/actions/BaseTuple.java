/*
 * @(#)BaseTuple.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2011/08/12  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.content.ContentValues;

/**
 * This is the base class for representing a row in the Quick Actions database <code><pre>
 * CLASS:
 *   Represents a row in any table present in the Quick Actions database
 *
 * RESPONSIBILITIES:
 *   Abstract class to represent a row in the Quick Actions database
 *
 * COLABORATORS:
 *       None
 *
 * USAGE:
 *      See each method.
 *
 * </pre></code>
 **/

public abstract class BaseTuple {

    /** Column name to identify the set of contacts contained in a rule */
    protected String mKey;
    /** Name of the DB table corresponding to this tuple */
    protected String mTableName;

    /**
     * Method to get the table name for the tuple
     * @return table name
     */
    public String getTableName() {
        return mTableName;
    }

    /**
     * Method to return the value that is used to identify all the tuples
     * added by one rule
     * @return Key value
     */
    public abstract String getKeyValue();

    /**
     * Converts the tuple to content values which can be used for inserting into tables
     * @return - {@link ContentValues}
     */
    public abstract ContentValues toContentValues();

}
