/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/07/02 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers;

/**
 * This interface may be implemented by rule publishers to segregate
 * code for new versus edit conditions.
 * <code><pre>
 *
 * CLASS:
 *  N/A
 *
 * RESPONSIBILITIES:
 *  Interface for publishers to segregate code for new versus edit cases.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public interface PublisherLaunchHandler {
    /** Handles edit case for rule element */
    void onEditConfig();
    /** Handles new rule element case */
    void onFirstConfig();
}
