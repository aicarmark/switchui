/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 */

package com.motorola.contextual.smartnetwork.test;

import android.content.ContentValues;
import android.test.AndroidTestCase;

/**
 * Helper class for commonly used functions to keep klocwork happy
 */
public abstract class TestHelper extends AndroidTestCase {
    /**
     * Wrapper function for ContentValues
     * @param values ContentValues
     * @param key key
     * @return long value of key in ContentValues
     */
    public static long getLong(ContentValues values, String key) {
        long retVal = -1;
        assertNotNull(values);
        assertNotNull(key);
        Long longVal = values.getAsLong(key);
        assertNotNull(longVal);
        if (longVal != null) {
            retVal = longVal.longValue();
        }
        return retVal;
    }

    /**
     * Wrapper function for ContentValues
     * @param values ContentValues
     * @param key key
     * @return int value of key in ContentValues
     */
    public static long getInt(ContentValues values, String key) {
        long retVal = -1;
        assertNotNull(values);
        assertNotNull(key);
        Integer intVal = values.getAsInteger(key);
        assertNotNull(intVal);
        if (intVal != null) {
            retVal = intVal.intValue();
        }
        return retVal;
    }
}
