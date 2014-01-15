/*
 * @(#)DiscardRuleDialogFragment.java
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

import com.motorola.contextual.smartrules.R;

public class DiscardRuleDialogFragment extends DialogFragment {

	public static final String TAG = DiscardRuleDialogFragment.class.getSimpleName();
	
	/** Factory method to instantiate Dialog Fragment
     *  @return new instance of DialogFragment
     */
    public static DiscardRuleDialogFragment newInstance() {
    	DiscardRuleDialogFragment frag = new DiscardRuleDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

    	return new AlertDialog.Builder(getActivity())
			.setMessage(R.string.discard_rule_message)
			.setTitle(R.string.discard_changes)
			.setIcon(R.drawable.ic_dialog_warning)
			.setInverseBackgroundForced(true)
			.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (getTargetFragment() instanceof DialogListener) {
						((DialogListener) getTargetFragment()).onSaveRule();
					}
					dialog.dismiss();
				}})
			.setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (getTargetFragment() instanceof DialogListener) {
						((DialogListener) getTargetFragment()).onDiscardRule();
					}
					dialog.dismiss();
				}}).create();
    }
    
    /** Interface that calling Fragment implements.
     */
    public interface DialogListener {
    	public void onSaveRule();
    	public void onDiscardRule();
    }
}