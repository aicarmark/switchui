/*
 * @(#)CursorToListException.java
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
package com.motorola.contextual.smartrules.db;

/** This class implements exception in CursorToList.
 *
 *<code><pre>
 * CLASS:
 * 	extends Exception to interrupt processing.
 *
 * RESPONSIBILITIES:
 * 	provide exception construction.
 *
 * COLABORATORS:
 *	see class: CursorToList - which throws this exception.
 *
 * USAGE:
 * 	see CursorToList
 *
 *</pre></code>
 */
public class CursorToListException extends Exception {

    /** support for serialization, and also prevents warning message. */
    private static final long serialVersionUID = 831460509464566341L;


    /** default constructor
     */
    public CursorToListException() {
        super();
    }

    /** constructor using detailMessage and throwable - standard Exception constructor.
     *
     * @param detailMessage - gives details of reason for failure
     * @param throwable - instance of throwable.
     */
    public CursorToListException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /** Standard Exception constructor
     *
     * @param detailMessage - gives details of reason for failure
     */
    public CursorToListException(String detailMessage) {
        super(detailMessage);
    }

    /** Standard Exception constructor
     *
     * @param throwable
     */
    public CursorToListException(Throwable throwable) {
        super(throwable);
    }


}
