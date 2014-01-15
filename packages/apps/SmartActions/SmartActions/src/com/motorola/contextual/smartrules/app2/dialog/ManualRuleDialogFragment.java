/*
 * @(#)ManualRuleDialogFragment.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * VHJ384        2012/07/15 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.app2.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;

import com.motorola.contextual.smartrules.R;

public class ManualRuleDialogFragment extends DialogFragment {

	public static final String TAG = ManualRuleDialogFragment.class.getSimpleName();
	
	/** Factory method to instantiate Dialog Fragment
     *  @return new instance of DialogFragment
     */
    public static ManualRuleDialogFragment newInstance() {
    	ManualRuleDialogFragment frag = new ManualRuleDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

    	return new AlertDialog.Builder(getActivity())
			.setMessage(R.string.manual_rule_confirmation_msg)
			.setTitle(R.string.manual_title)
			.setIcon(R.drawable.ic_dialog_smart_rules)
			.setInverseBackgroundForced(true)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (getTargetFragment() instanceof DialogListener) {
						((DialogListener) getTargetFragment()).onSaveManualRule();
					}
					dialog.dismiss();
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (getTargetFragment() instanceof DialogListener) {
						((DialogListener) getTargetFragment()).onCancelManualRule();
					}
					dialog.dismiss();
				}
			})
			.setOnKeyListener(new DialogInterface.OnKeyListener() {
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
						
						/*
						 * The Save button is disabled after the user clicks on it once in 
						 * order to prevent multiple clicks. But after the Manual Confirmation 
						 * dialog is canceled the user should be allowed to click on it again.
						 */
						if (getTargetFragment() instanceof DialogListener) {
							((DialogListener) getTargetFragment()).onCancelManualRule();
						}
					}
					return false; // Any other keys are still processed as normal
				}
			}).create();
    }
    
    /** Interface that calling Fragment implements.
     */
    public interface DialogListener {
    	public void onSaveManualRule();
    	public void onCancelManualRule();
    }
}