/*
 * @(#)DeleteRuleDialogFragment.java
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

public class DeleteRuleDialogFragment extends DialogFragment {

	public static final String TAG = DeleteRuleDialogFragment.class.getSimpleName();
	
	/** Factory method to instantiate Dialog Fragment
     *  @return new instance of DialogFragment
     */
    public static DeleteRuleDialogFragment newInstance() {
    	DeleteRuleDialogFragment frag = new DeleteRuleDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

    	return new AlertDialog.Builder(getActivity())
			.setMessage(R.string.delete_rule_message)
			.setTitle(R.string.delete_rule)
			.setIcon(R.drawable.ic_dialog_trash)
			.setInverseBackgroundForced(true)
			.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (getTargetFragment() instanceof DialogListener) {
						((DialogListener) getTargetFragment()).onDeleteRule();
					}
					dialog.dismiss();
				}})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.dismiss();
				}}).create();
    }
    
    /** Interface that calling Fragment implements.
     */
    public interface DialogListener {
        public void onDeleteRule();
    }
}