/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add rawPut for rawQuery support
 */

package com.motorola.contextual.smartnetwork.db;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import android.content.ContentValues;
import android.util.Log;

public abstract class Tuple implements SmartNetworkDbSchema {
    private static final String TAG = "Tuple";

    // set of supported columns for this tuple
    private final SortedSet<String> mColumns = new TreeSet<String>();
    // map of column-value
    protected final HashMap<String, String> mFields = new HashMap<String, String>();

    /**
     * Constructor for creating a new tuple for insertion, without row id.
     * @param columns
     */
    protected Tuple(String[] columns) {
        mColumns.add(COL_ID);
        if (columns != null) {
            for (String column : columns) {
                mColumns.add(column);
            }
        }
    }

    /**
     * @return ContentValues populated with the tuple's column values
     */
    public final ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        for (Map.Entry<String, String> field : mFields.entrySet()) {
            cv.put(field.getKey(), field.getValue());
        }
        return cv;
    }

    /**
     * @return the columns supported for this tuple
     */
    public final String[] getColumns() {
        return mColumns.toArray(new String[mColumns.size()]);
    }

    /**
     * @return the number of values
     */
    public final int size() {
        return mFields.size();
    }

    /**
     * @return the row id of the tuple, -1 on error
     */
    public final long getId() {
        long id = -1;
        String val = mFields.get(COL_ID);
        if (val != null) {
            try {
                id = Long.parseLong(val);
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Unable to parse tuple id.");
            }
        }
        return id;
    }

    /**
     * Returns the value of the requested column as a double.
     * @param columnName name of the column
     * @return the value of that column as a double
     * @throws IllegalArgumentException if the column does not exist or if the value cannot be
     * parsed as a double
     */
    public final double getDouble(String columnName) throws IllegalArgumentException {
        double retVal = 0;
        String val = mFields.get(columnName);
        String exception = null;
        if (val != null) {
            try {
                retVal = Double.parseDouble(val);
            } catch (NumberFormatException nfe) {
                exception = "Unable to parse double value for " + columnName;
            }
        } else {
            exception = columnName + " does not exist.";
        }

        if (exception != null) {
            throw new IllegalArgumentException(exception);
        }

        return retVal;
    }

    /**
     * Returns the value of the requested column as a float.
     * @param columnName name of the column
     * @return the value of that column as a float
     * @throws IllegalArgumentException if the column does not exist or if the value cannot be
     * parsed as a float
     */
    public final float getFloat(String columnName) throws IllegalArgumentException {
        float retVal = 0;
        String val = mFields.get(columnName);
        String exception = null;
        if (val != null) {
            try {
                retVal = Float.parseFloat(val);
            } catch (NumberFormatException nfe) {
                exception = "Unable to parse float value for " + columnName;
            }
        } else {
            exception = columnName + " does not exist.";
        }

        if (exception != null) {
            throw new IllegalArgumentException(exception);
        }

        return retVal;
    }

    /**
     * Returns the value of the requested column as an int.
     * @param columnName name of the column
     * @return the value of that column as an int
     * @throws IllegalArgumentException if the column does not exist or if the value cannot be
     * parsed as an int
     */
    public final int getInt(String columnName) throws IllegalArgumentException {
        int retVal = 0;
        String val = mFields.get(columnName);
        String exception = null;
        if (val != null) {
            try {
                retVal = Integer.parseInt(val);
            } catch (NumberFormatException nfe) {
                exception = "Unable to parse int value for " + columnName;
            }
        } else {
            exception = columnName + " does not exist.";
        }

        if (exception != null) {
            throw new IllegalArgumentException(exception);
        }

        return retVal;
    }

    /**
     * Returns the value of the requested column as a long.
     * @param columnName name of the column
     * @return the value of that column as a long
     * @throws IllegalArgumentException if the column does not exist or if the value cannot be
     * parsed as a long
     */
    public final long getLong(String columnName) throws IllegalArgumentException {
        long retVal = 0;
        String val = mFields.get(columnName);
        String exception = null;
        if (val != null) {
            try {
                retVal = Long.parseLong(val);
            } catch (NumberFormatException nfe) {
                exception = "Unable to parse long value for " + columnName;
            }
        } else {
            exception = columnName + " does not exist.";
        }

        if (exception != null) {
            throw new IllegalArgumentException(exception);
        }

        return retVal;
    }

    /**
     * Returns the value of the requested column as a String.
     * @param columnName name of the column
     * @return the value of that column as a String
     * @throws IllegalArgumentException if the column does not exist
     */
    public final String getString(String columnName) throws IllegalArgumentException {
        String retVal = null;

        if (mFields.containsKey(columnName)) {
            retVal = mFields.get(columnName);
        } else {
            throw new IllegalArgumentException(columnName + " does not exist.");
        }

        return retVal;
    }

    /**
     * Sets the value of the specified column to the given value.
     * @param columnName name of column
     * @param value value to set
     * @return true if the column exists and can be set to the value, false otherwise
     */
    public final boolean put(String columnName, double value) {
        return put(columnName, String.valueOf(value));
    }

    /**
     * Sets the value of the specified column to the given value.
     * @param columnName name of column
     * @param value value to set
     * @return true if the column exists and can be set to the value, false otherwise
     */
    public final boolean put(String columnName, float value) {
        return put(columnName, String.valueOf(value));
    }

    /**
     * Sets the value of the specified column to the given value.
     * @param columnName name of column
     * @param value value to set
     * @return true if the column exists and can be set to the value, false otherwise
     */
    public final boolean put(String columnName, int value) {
        return put(columnName, String.valueOf(value));
    }

    /**
     * Sets the value of the specified column to the given value.
     * @param columnName name of column
     * @param value value to set
     * @return true if the column exists and can be set to the value, false otherwise
     */
    public final boolean put(String columnName, long value) {
        return put(columnName, String.valueOf(value));
    }

    /**
     * Sets the value of the specified column to the given value.
     * @param columnName name of column
     * @param value value to set
     * @return true if the column exists and can be set to the value, false otherwise
     */
    public final boolean put(String columnName, String value) {
        boolean success = false;
        if (mColumns.contains(columnName)) {
            mFields.put(columnName, value);
            success = true;
        }
        return success;
    }

    /**
     * Sets the value of the specified column to the given value.
     * This method does not check if the given column is a valid column.
     * This is used for fulfilling raw SQL queries with AS statements.
     * @param columnName name of column
     * @param value value to set
     */
    public final void rawPut(String columnName, String value) {
        mFields.put(columnName, value);
    }
}
