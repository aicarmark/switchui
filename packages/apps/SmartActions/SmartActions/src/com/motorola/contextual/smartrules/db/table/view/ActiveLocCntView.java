/*
 * @(#)ActiveLocCntView.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/09/06 NA				  Initial version 
 *
 */
package com.motorola.contextual.smartrules.db.table.view;

import com.motorola.contextual.smartrules.Constants;

import android.net.Uri;

/**This class is a virtual view of that uses RuleConditionView as a basis.
 * 
 *<code><pre>
 * CLASS:
 *  implements Constants.
 *  
 * RESPONSIBILITIES:
 *  Virtual view will return a cursor with one column that is used to store
 *  the count of the number of active Location based rules. 
 *  
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class ActiveLocCntView implements Constants {
	
	public static final String VIEW_NAME = ActiveLocCntView.class.getSimpleName(); 
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");

}
