/*
 * @(#)StringUtils.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21383       2011/03/28  NA                  Initial version
 *
 */
package com.motorola.contextual.commonutils;

import java.util.StringTokenizer;

import android.database.DatabaseUtils;
import android.text.Editable;

/**
 * This class implements string utility functions  <code><pre>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 *     This class implements string utility functions
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class StringUtils {

    public static final String EMPTY_STRING = "";
    public static final String SPACE = " ";
    public static final String TOKEN_DELIMITER = "^";
    public static final char SPACE_CHAR = SPACE.charAt(0);
    public static final char SEMI_COLON = ';';
    public static final char COMMA = ',';
    public static final char COLON = ':';
    public static final char NEW_LINE = '\n';
    public static final String NEW_LINE_STRING = "\n";
    public static final String COMMA_STRING = ",";

    /**
     * Returns true if string is empty
     * @param str - string to check
     * @return - true if string is empty else false
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }

    /**
     * Returns true if the passed CharSequence is empty
     * @param str - character sequence to check
     * @return - true if the passed CharSequence is empty else false
     */
    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0 || str.toString().trim().length() == 0;
    }

    /**
     * Returns true if the text of the given Editable is not empty
     * @param editable
     * @return
     */
    public static boolean isTextEmpty(Editable editable) {
        return ((editable == null) || (isEmpty(editable.toString())));
    }

    /**
     * Returns SQL-escape a string
     * @param str
     * @return
     */
    public static String escapeSQLString(String str) {
        if (isEmpty(str))
            return EMPTY_STRING;

        return DatabaseUtils.sqlEscapeString(str);
    }

    /**
     * Concats tokens with space
     * @param tokens
     * @return
     */
    public static String concatTokens(StringTokenizer tokens) {
        if (tokens == null)
            return SPACE;

        StringBuilder tokenBuf = new StringBuilder();
        while (tokens.hasMoreTokens()) {
            tokenBuf.append(tokens.nextToken());
            tokenBuf.append(SPACE);
        }

        return tokenBuf.toString().trim();
    }

    /**
     * Returns if the two strings are equal
     * A null string is equal to a trimmed string of zero length
     * @param one
     * @param two
     * @return
     */
    public static boolean areEqual(String one, String two) {
        if (isEmpty(one) && isEmpty(two))
            return true;

        if (isEmpty(one) || isEmpty(two))
            return false;

        // now both are not empty
        return one.equals(two);
    }

    /**
     * Returns true if the two strings are equal ignoring the case
     * Trimmed strings are also considered equal and trimmed strings
     * of zero-length are equal to a null string.
     * @param one
     * @param two
     * @return
     */
    public static boolean areEqualIgnoreCase(String one, String two) {
        if (isEmpty(one) && isEmpty(two))
            return true;

        if (isEmpty(one) || isEmpty(two))
            return false;

        // now both are not empty
        return one.equalsIgnoreCase(two);
    }

    /**
     * Returns true if the two strings are equal, where "null or empty" counts as a single value.
     * @param s1 The first string to compare.
     * @param s2 The second string to compare.
     * @return true if the strings are both empty are identical (ignoring case), false otherwise.
     */
    public static boolean equalsIgnoreCase(String s1, String s2) {
        return isEmpty(s1) ? isEmpty(s2) : s1.equalsIgnoreCase(s2);
    }

    /**
     * Trims the spaces in the string passed as input
     * @param String
     * @returns the trimmed string
     */
    public static String trimSpaces(String str)
    {
        if(StringUtils.isEmpty(str))
            return StringUtils.EMPTY_STRING;

        StringTokenizer st = new StringTokenizer(str);
        StringBuilder strBuffer = new StringBuilder();
        while (st.hasMoreTokens())
        {
            strBuffer.append(st.nextToken());
            strBuffer.append(StringUtils.SPACE);
        }

        return strBuffer.toString().trim();
    }

}
