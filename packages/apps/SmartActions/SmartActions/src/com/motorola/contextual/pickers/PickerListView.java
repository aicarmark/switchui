/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/07/03 Smart Actions 2.1  Initial Version
 */

package com.motorola.contextual.pickers;

import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.widget.ListView;

import com.motorola.contextual.pickers.PickerController.Params;


/**
 * The class extends a ListView to implement some specific behavior for Pickers.
 *
 * <code><pre>
 *
 * CLASS:
 *  extends ListView
 *
 * RESPONSIBILITIES:
 * Intercepts calls to ListView and provide some specific behavior for Pickers.
 *
 * COLLABORATORS:
 *  PickerController - manages this ListView class to present the choices in
 *  the pickers.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class PickerListView extends ListView {

		private PickerController mController;
		private Params mParams;

		/**
		 * Constructor
		 *
		 * @param context - application context.
		 */
	    public PickerListView(Context context) {
	        super(context);
	    }

	    /**
	     * Constructor
	     *
	     * @param context - application context
	     * @param attrs - attributes for this ListView
	     */
	    public PickerListView(Context context, AttributeSet attrs) {
	        super(context, attrs);
	    }

	    /**
	     * Constructor
	     *
	     * @param context - application context
	     * @param attrs - attributes for this ListView
	     * @param defStyle - default style for this ListView
	     */
	    public PickerListView(Context context, AttributeSet attrs, int defStyle) {
	        super(context, attrs, defStyle);
	    }

	    @Override
		public void setItemChecked(int position, boolean value) {
	    	super.setItemChecked(position, value);
	    	PickerController.enableButtonIfItemSelected(!mParams.mIsNoChoice, mController,
	                DialogInterface.BUTTON_POSITIVE, mParams.mIsBottomButtonAlwaysEnabled);
	    }

	    /**
	     * Initialize the a few picker-specific parameters for this ListView.
	     *
	     * @param controller - the PickerController that this ListView belongs to
	     * @param params - the picker parameters from this ListView.
	     */
	    void initializeParams(PickerController controller, Params params) {
	    	mController = controller;
	    	mParams = params;
	    }
}
