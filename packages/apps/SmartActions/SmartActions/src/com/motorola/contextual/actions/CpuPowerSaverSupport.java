/*
 * @(#)CpuPowerSaverSupport.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51185        20012/07/13  NA				  Initial version
 *
 */

package com.motorola.contextual.actions;

import android.content.Context;

/** 
* Helper class to contain logic related to UI, related to getting and setting
* preferences for ProcessorSpeed action which would be later utilized in UI
* 
* 
*<code><pre>
* CLASS:
* 	 
*
* RESPONSIBILITIES:
* 	set and get shared preference
*
* COLABORATORS:
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class CpuPowerSaverSupport implements Constants{
	
	/**
	 * Checks if ProcessorSpeed power savings is supported by querying package specific 
	 * SharedPreference
	 * @param mContext
	 * @return
	 */
	public static boolean retrievePckgSpecificPref(Context mContext){
		boolean ret = false;
		ret = Persistence.retrieveBooleanValue(mContext, CPU_POWERSAVER_SUPPORT_KEY);
		return ret;
	}
	
	/**
	 * Sets the package specific ProcessorSpeed power savings shared preference
	 * @param mContext
	 * @param isCpuPowerSaveSupported
	 */
	public static void setCpuPowerSaverSupportPref(Context mContext, boolean isCpuPowerSaveSupported){
		Persistence.commitValue(mContext, CPU_POWERSAVER_SUPPORT_KEY, isCpuPowerSaveSupported);
	}

}
