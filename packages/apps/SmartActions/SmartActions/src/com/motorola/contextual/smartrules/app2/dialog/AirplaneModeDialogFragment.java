/*
 * @(#)AirplaneModeDialogFragment.java
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

public class AirplaneModeDialogFragment extends DialogFragment {

	public static final String TAG = AirplaneModeDialogFragment.class.getSimpleName();
	
	/** Factory method to instantiate Dialog Fragment
     *  @return new instance of DialogFragment
     */
    public static AirplaneModeDialogFragment newInstance() {
    	AirplaneModeDialogFragment frag = new AirplaneModeDialogFragment();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

    	return new AlertDialog.Builder(getActivity())
			.setMessage(R.string.airplanemode_loc_correlation_desc)
			.setTitle(R.string.airplanemode_loc_correlation_title)
			.setIcon(R.drawable.ic_location_w)
			.setInverseBackgroundForced(true)
			.setPositiveButton(R.string.turn_off_airplanemode, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (getTargetFragment() instanceof DialogListener) {
						((DialogListener) getTargetFragment()).onTurnOffAirplaneMode();
					}
					dialog.dismiss();
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (getTargetFragment() instanceof DialogListener) {
						((DialogListener) getTargetFragment()).onCancelTurnOffAirplaneMode();
					}
					dialog.dismiss();
				}
			})
			.setOnKeyListener(new DialogInterface.OnKeyListener() {
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					if ( (keyCode == KeyEvent.KEYCODE_SEARCH) || (keyCode == KeyEvent.KEYCODE_BACK)
   							&& event.getRepeatCount() == 0) {

						if (getTargetFragment() instanceof DialogListener) {
							((DialogListener) getTargetFragment()).onCancelTurnOffAirplaneMode();
						}
						dialog.cancel();
					}
					return false; // Any other keys are still processed as normal
				}
			}).create();
    }
    
    /** Interface that calling Fragment implements.
     */
    public interface DialogListener {
    	public void onTurnOffAirplaneMode();
    	public void onCancelTurnOffAirplaneMode();
    }
}