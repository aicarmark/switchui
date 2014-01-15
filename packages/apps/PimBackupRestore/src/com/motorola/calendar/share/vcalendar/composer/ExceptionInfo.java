/*
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: ExceptionInfo.java
 * Description: See class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 25-Feb-2010    e12128       Created file
 **********************************************************
 */
package com.motorola.calendar.share.vcalendar.composer;

import java.util.List;

/**
 * This interface encapsulates the properties that characterize an ActiveSync
 * event exception.
 */
public interface ExceptionInfo {
    /**
     * Returns the list of categories for this Exception, each category being a String.
     * @return The list of categories for this Exception.
     */
    List<String> getCategories();

    /**
     * Returns the start time for the exception.
     * @return The exception's start time, as a YYMMDDTHHMSS string.
     */
    String getExceptionStartTime();

    /**
     * Returns whether this is a "deleted" exception.
     * @return true for a deleted exception, false for a modify exception.
     */
    boolean isDeleted();

}
