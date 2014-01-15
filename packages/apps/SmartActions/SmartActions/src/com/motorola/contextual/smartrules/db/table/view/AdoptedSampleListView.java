/*
 * @(#)AdoptedSampleListView.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2012/04/05 NA				  Initial version 
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
 *  Virtual view will return a cursor of adopted rules for the key passed in with the 
 *  Rule Table columns.
 *  
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class AdoptedSampleListView implements Constants {

	public static final String VIEW_NAME = AdoptedSampleListView.class.getSimpleName();
	public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");
}