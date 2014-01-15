/*
 * @(#)BTDeviceUtil.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/07/06   NA               Initial Version
 *
 */
package com.motorola.contextual.pickers.conditions.bluetooth;

import com.motorola.contextual.smartprofile.Constants;

/**
 * Utility class for BT smart profile
 *
 * RESPONSIBILITIES:
 *   Provides utility functions
 *
 */
public final class BTDeviceUtil implements BTConstants, Constants {
    @SuppressWarnings("unused")
    private static final String TAG =  BTDeviceUtil.class.getSimpleName();


    public static String trimBraces(String stringToTrim) {
    if (stringToTrim.contains(LEFT_PAREN) && stringToTrim.contains(RIGHT_PAREN)) {
        stringToTrim = stringToTrim.substring(stringToTrim.indexOf(LEFT_PAREN) + 1, stringToTrim.indexOf(RIGHT_PAREN));
    }

    return stringToTrim;
    }

}
