/*
 * @(#)AutoSmsTuple.java
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
 * This class represents each row in the auto SMS table present in the Quick Actions database <code><pre>
 * CLASS:
 *   Extends ContactsTuple which is used to represent a row in any table in the Quick Actions database
 *
 * RESPONSIBILITIES:
 *   To create an object representing a row in the Auto SMS table
 *
 * COLABORATORS:
 *       Extends ContactsTuple.
 *
 * USAGE:
 *      See each method.
 *
 * </pre></code>
 **/

public class AutoSmsTuple extends BaseTuple implements Constants {
    private String mInternalName;
    private String mNumber;
    private int mRespondTo;
    private String mMessage;
    private int mSentFlag;
    private String mName;
    private int mIsKnown;

    /**
     * Constructor that creates an AutoSmsTuple object using all the required parameters
     * @param internalName Internal name of the Auto Text Reply action
     * @param number Number which will receive auto reply
     * @param respondTo Flag indicating whether to respond to missed calls or texts or both
     * @param message Message to be sent
     * @param sentFlag Flag indicating if message has been sent to the number.
     * @param name Contact Name which will receive auto reply.
     * @param isKnown Flag indicating whether the contact is known or not.
     * Only one auto reply is sent to a number present in a rule while the rule is active.
     */
    public AutoSmsTuple (String internalName, String number, int respondTo, String message, int sentFlag, 
    		         String name, int isKnown) {
        mKey = AutoSmsTableColumns.INTERNAL_NAME;
        mTableName = AUTO_SMS_TABLE;
        mInternalName = internalName;
        mNumber = number;
        mRespondTo = respondTo;
        mMessage = message;
        mSentFlag = sentFlag;
        mName = name;
        mIsKnown = isKnown;
    }

    @Override
    public String getKeyValue() {
        return mInternalName;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues initialValues = new ContentValues();
        initialValues.put(AutoSmsTableColumns.INTERNAL_NAME, mInternalName);
        initialValues.put(AutoSmsTableColumns.NUMBER, mNumber);
        initialValues.put(AutoSmsTableColumns.RESPOND_TO, mRespondTo);
        initialValues.put(AutoSmsTableColumns.MESSAGE, mMessage);
        initialValues.put(AutoSmsTableColumns.SENT_FLAG, mSentFlag);
        initialValues.put(AutoSmsTableColumns.NAME, mName);
        initialValues.put(AutoSmsTableColumns.IS_KNOWN, mIsKnown);
        return initialValues;
    }
}
