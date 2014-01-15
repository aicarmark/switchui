package com.motorola.mmsp.socialGraph.socialGraphWidget.common;

import android.app.ProgressDialog;
import android.content.Context;
//maybe delete
public class WaitDialog extends ProgressDialog {

	public WaitDialog(Context context) {
		super(context);		
	}

	/**
	 * This hook is called when the user signals the desire to start a search.
	 */
	@Override
	public boolean onSearchRequested() {
		return true;
	}

}
