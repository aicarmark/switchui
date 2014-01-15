/*
 * @(#)VisibleEnaAutoRulesCntView.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA MOBILITY INC.
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

import android.net.Uri;

import com.motorola.contextual.smartrules.Constants;

/**This class is a virtual view of that uses RuleTable as a basis.
 * 
 *<code><pre>
 * CLASS:
 *  implements Constants.
 *  
 * RESPONSIBILITIES:
 *  Virtual view will return a cursor with one column that is used to store
 *  the count of the number of visible enabled automatic rules. 
 *  
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class VisibleEnaAutoRulesCntView implements Constants {
	
	// ADD comments to state that this is a virtual view......
	public static final String VIEW_NAME = VisibleEnaAutoRulesCntView.class.getSimpleName(); 
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");
}
