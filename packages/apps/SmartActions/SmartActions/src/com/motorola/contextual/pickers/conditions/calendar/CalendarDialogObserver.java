/*
 * @(#)CalendarDialogObserver.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * wkh346        2011/10/11 NA                Initial version
 *
 */

package com.motorola.contextual.pickers.conditions.calendar;

/**
 * This interface declares methods for getting notified when positive or
 * negative button is clicked on calendar dialog
 *
 * <CODE><PRE>
 *
 * RESPONSIBILITIES:
 * This interface declares methods for getting notified when positive or negative
 * button is clicked on calendar dialog
 *
 * COLABORATORS:
 *     SmartProfile - Implements the preconditions available across the system
 *
 * </PRE></CODE>
 */
public interface CalendarDialogObserver {

    /**
     * Method to notify the observer that positive button of the dialog is
     * clicked
     */
    public void onPositiveButtonClickOnCalendarDialog();

    /**
     * Method to notify the observer that negative button of the dialog is
     * clicked
     */
    public void onNegativeButtonClickOnCalendarDialog();

    /**
     * Method to notify the observer that all items have been unchecked
     */
    public void onAllItemsUnChecked();

    /**
     * Method to notify the observer that an item has been checked
     */
    public void onItemChecked();

    /**
     * Method to notify the observer that initialization is complete
     */
    public void onInitComplete();

}
