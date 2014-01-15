/*
 * @(#)TransitionFragment.java
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
package com.motorola.contextual.smartrules.app2;

import com.motorola.contextual.smartrules.R;

import android.app.Fragment;
import android.os.Bundle;

/** This fragment class is used to display a black screen
 * during the transition from the Landing Page to the Rule Editor.
 *
 *<code><pre>
 * CLASS:
 * 	extends Fragment
 *
 * RESPONSIBILITIES:
 * 	Used to display black screen during transition.
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class TransitionFragment extends Fragment {

	public static final String TAG = TransitionFragment.class.getSimpleName();
	
	/** Factory method to instantiate Fragment
     *  @return new instance of TransitionFragment
     */
	public static TransitionFragment newInstance() { 
		TransitionFragment f = new TransitionFragment();
		return f; 
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getActivity().getActionBar().setTitle(getString(R.string.app_name));
	}
}