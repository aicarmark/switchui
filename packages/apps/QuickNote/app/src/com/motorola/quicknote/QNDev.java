/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * 
 * ***************************************************************************************
 * [QuickNote] Functions to support development
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created.
 * 
 * 
 *****************************************************************************************/

package com.motorola.quicknote;

import android.util.Log;

public abstract class QNDev {
	public static final boolean STORE_TEXT_NOTE_ON_SDCARD = true;

	public static void qnAssert(boolean cond, String msg) {
		// if(QNConfig.__DEBUG__) {
		if (false) {
			if (!cond) {
				throw new RuntimeException(msg);
				// Exception e = new RuntimeException(msg);
				// e.printStackTrace();
			}
			// junit.framework.Assert.assertTrue(msg, cond);
		}
	}

	public static void qnAssert(boolean cond) {
		qnAssert(cond, "");
	}

	public static void log(String string) {
		if (QNConfig.__DEBUG__) {
			Log.d("[QuickNote]", string);
		}
	}

	public static void logd(String tag, String string) {
		if (QNConfig.__DEBUG__) {
			Log.d(tag, string);
		}
	}

	public static void logi(String tag, String string) {
		if (QNConfig.__DEBUG__) {
			Log.i(tag, string);
		}
	}

	/**
	 * This function is used only to increase readibility
	 */
	public static void unused_parameter(Object o) {
	}
}
