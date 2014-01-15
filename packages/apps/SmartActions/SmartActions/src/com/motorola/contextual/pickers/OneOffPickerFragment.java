/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/06/19 Smart Actions 2.1  Initial Version
 */
package com.motorola.contextual.pickers;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.motorola.contextual.smartrules.R;

/**
 * This class defines the base class for a guided picker fragment that requires a "custom"
 * layout not supported by the standard Picker class.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment
 *
 * RESPONSIBILITIES:
 * Provides the basic guided look of a prompt on the top and an action button at the bottom.
 * What goes between is provided the subclasses.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public abstract class OneOffPickerFragment extends PickerFragment implements View.OnClickListener {

	private View mContainerView;
	private Button mButton;

    /**
     * All subclasses of Fragment must include a public empty constructor. The framework
     * will often re-instantiate a fragment class when needed, in particular during state
     * restore, and needs to be able to find this constructor to instantiate it. If the
     * empty constructor is not available, a runtime exception will occur in some cases
     * during state restore.
     */
    public OneOffPickerFragment() {
        super();
    }

	@Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
        final Bundle savedInstanceState) {
		mContainerView = inflater.inflate(R.layout.custom_picker_container, container, false);
		mButton = (Button)mContainerView.findViewById(R.id.button1);
		mButton.setOnClickListener(this);
		final ViewGroup contentContainer = (ViewGroup)mContainerView.findViewById(R.id.content_container);
		contentContainer.addView(createCustomContentView(inflater, contentContainer, savedInstanceState));
		return mContainerView;
	}

	/**
	 * Implement this method to provide the custom view for this picker.
	 *
	 * @param inflater
	 * @param container - container of the custom view
	 * @param savedInstanceState - same semantics as onCreateView
	 * @return Must return a non-null view that represents the contents used in the fragment
	 */
	abstract protected View createCustomContentView(LayoutInflater inflater, ViewGroup container,
	        Bundle savedInstanceState);

	/**
	 * Implement this to be called back when the bottom (action) button is pressed.
	 *
	 * Required by OnClickListener (must be public).
	 *
	 * @param v - the button.
	 */
	abstract public void onClick(View v);

	/**
	 * Set the prompt on this guided picker.
	 *
	 * @param prompt - displayed on the picker
	 */
	protected final void setPrompt(final String prompt) {
        TextView v;
        v = (TextView)mContainerView.findViewById(R.id.prompt);
        v.setText(Html.fromHtml(prompt));
    }

	/**
	 * Set the display string used for the action button at the bottom.
	 *
	 * @param action - the label of the action button
	 */
	protected final void setActionString(final String action) {
		mButton.setText(action);
	}

	/**
	 * Enable or disable the action button at the bottom.
	 *
	 * @param enabled - true to enable the button
	 */
	protected final void enableButton(final boolean enabled) {
		mButton.setEnabled(enabled);
	}
}
