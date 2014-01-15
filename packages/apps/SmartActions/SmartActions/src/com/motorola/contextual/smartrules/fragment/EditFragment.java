/*
 * @(#)EditFragment.java
 *
 * (c) COPYRIGHT 2011 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/12/25 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;

/** This class is mainly to set the different action bar items view visibility.
*
*<code><pre>
* CLASS:
* 	extends Fragment
*
*   implements Constants for debug constants
*
* RESPONSIBILITIES:
* 	provide a new instance of the fragment
* 	setup the menu items visibility for the action bar
*
* COLABORATORS:
* 	None.
*
* USAGE:
* 	See each method.
*</pre></code>
*/
public class EditFragment extends Fragment implements Constants {
	
	private static final String EDIT_FRAGMENT_OPTION = PACKAGE + ".option";
	private static final String EDIT_FRAGMENT_SHOW_ADD_BUTTON = PACKAGE + ".showaddbutton";
	
	public interface EditFragmentOptions {
		final int DEFAULT = 1;
		final int SHOW_SAVE_DISABLED = DEFAULT + 1;
		final int SHOW_SAVE_ENABLED = SHOW_SAVE_DISABLED + 1;
		final int SHOW_CONFIRM = SHOW_SAVE_ENABLED + 1;
		final int RULES_BUILDER_INITIAL = SHOW_CONFIRM + 1;
		final int RULES_BUILDER_EDIT_MODE_SAVE_DISABLED = RULES_BUILDER_INITIAL + 1;
		final int RULES_BUILDER_EDIT_MODE_SAVE_ENABLED = RULES_BUILDER_EDIT_MODE_SAVE_DISABLED + 1;
		final int HIDE_SAVE = RULES_BUILDER_EDIT_MODE_SAVE_ENABLED + 1;
	}
	
	/** default constructor
	 */
	public EditFragment() {
	}
	
	/** new instance of EditFragment and initialize the type of fragment
	 *  and if the + sign needs to be shown or not
	 * 
	 * @param editFragmentOption - type of options that needs to be displayed.
	 * @param showAddButton - true to show the + sign, false to hide
	 * @return an instance of EditFragment
	 */
	public static EditFragment newInstance(int editFragmentOption, boolean showAddButton) {
		EditFragment editFragment = new EditFragment();
		
		Bundle bundle = new Bundle();
		bundle.putInt(EDIT_FRAGMENT_OPTION, editFragmentOption);
		bundle.putBoolean(EDIT_FRAGMENT_SHOW_ADD_BUTTON, showAddButton);
		editFragment.setArguments(bundle);
		
		return editFragment;
	}
	
	/** getter for the type of EditFragment option
	 * 
	 * @return - the type of EditFragment option
	 */
	public int getEditFragmentOption() {	
		return getArguments().getInt(EDIT_FRAGMENT_OPTION, EditFragmentOptions.DEFAULT);
	}
	
	/** getter for if the + (ADD) button should be displayed or not
	 * 
	 * @return - true or false
	 */
	public boolean getShowAddButtonState() {
		return getArguments().getBoolean(EDIT_FRAGMENT_SHOW_ADD_BUTTON, true);
	}
	
	@Override
	public void onCreate(Bundle saved) {
		super.onCreate(saved);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.edit_menu, menu);
		setMenuItemsVisibility(menu);
	}

	/** sets the action bar menu items visibility.
	 * 
	 * @param menu - menu item
	 */
	private void setMenuItemsVisibility(Menu menu) {
		int editFragmentOption = getEditFragmentOption();
		boolean showAddButton = getShowAddButtonState();
		switch(editFragmentOption) {
			
			case EditFragmentOptions.DEFAULT:
				if(showAddButton)
					menu.findItem(R.id.edit_add_button).setVisible(true);
				else
					menu.findItem(R.id.edit_add_button).setVisible(false);
				break;			

			case EditFragmentOptions.SHOW_SAVE_DISABLED:
				menu.findItem(R.id.edit_cancel).setVisible(false);
				menu.findItem(R.id.edit_save).setVisible(true).setEnabled(false);
				if(showAddButton)
					menu.findItem(R.id.edit_add_button).setVisible(true);
				else
					menu.findItem(R.id.edit_add_button).setVisible(false);
				break;

			case EditFragmentOptions.SHOW_SAVE_ENABLED:
				menu.findItem(R.id.edit_cancel).setVisible(false);
				menu.findItem(R.id.edit_save).setVisible(true).setEnabled(true);
				if(showAddButton)
					menu.findItem(R.id.edit_add_button).setVisible(true);
				else
					menu.findItem(R.id.edit_add_button).setVisible(false);
				break;
		
			case EditFragmentOptions.SHOW_CONFIRM:
				menu.findItem(R.id.edit_add_button).setVisible(false);
				menu.findItem(R.id.edit_confirm).setVisible(true);
				break;
				
			case EditFragmentOptions.RULES_BUILDER_INITIAL:
				menu.findItem(R.id.edit_add_button).setVisible(false);
				break;		
			
			case EditFragmentOptions.RULES_BUILDER_EDIT_MODE_SAVE_DISABLED:
				menu.findItem(R.id.edit_add_button).setVisible(false);
				menu.findItem(R.id.edit_cancel).setVisible(true);
				menu.findItem(R.id.edit_rb_save).setVisible(true).setEnabled(false);
				break;
				
			case EditFragmentOptions.RULES_BUILDER_EDIT_MODE_SAVE_ENABLED:
				menu.findItem(R.id.edit_add_button).setVisible(false);
				menu.findItem(R.id.edit_cancel).setVisible(true);
				menu.findItem(R.id.edit_rb_save).setVisible(true).setEnabled(true);
				break;		
				
			case EditFragmentOptions.HIDE_SAVE:
				menu.findItem(R.id.edit_cancel).setVisible(true);
				if(showAddButton)
					menu.findItem(R.id.edit_add_button).setVisible(true);
				else
					menu.findItem(R.id.edit_add_button).setVisible(false);
				break;
		}
	}
}