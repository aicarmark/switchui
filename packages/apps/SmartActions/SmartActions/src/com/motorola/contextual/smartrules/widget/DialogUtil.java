/*
 * @(#)DialogUtil.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/09/22 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.drivingmode.DrivingModeSuggestionDialog;
import com.motorola.contextual.smartrules.rulesbuilder.EditRuleActivity;

/** This is an utility class for dialogs that are common to activities.
*
*<code><pre>
* CLASS:
* 	no extends, no implements
*
* RESPONSIBILITIES:
* 	This class is entirely for dialogs that are common to activities.
*
* COLABORATORS:
* 	None
*
* USAGE:
*  See individual routines.
*</pre></code>
*/
public class DialogUtil {

	/** displays the warning dialog to user to indicate that the user
	 * 	is at the maximum visible enabled automatic rules.
	 * 
	 * @param context - context
	 */
	public static void showMaxVisibleEnaAutoRulesDialog(final Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(R.string.max_visible_ena_auto_rules)
				.setTitle(R.string.app_name)
				.setIcon(R.drawable.ic_dialog_warning)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
								if(context instanceof EditRuleActivity) {
									((EditRuleActivity) context).displayRuleStatus();
								} else if (context instanceof DrivingModeSuggestionDialog) {
								    ((DrivingModeSuggestionDialog) context).finish();
								}
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}
}
