/*
 * @(#)PoiLocationsListActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/09/11 NA				  Initial version
 *
 */
package com.motorola.contextual.smartprofile.locations;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.fragment.EditFragment;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps;
import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps.LocDbColumns;
import com.motorola.contextual.smartprofile.util.Util;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class displays the list of locations that have been tagged with a poitag
 * in Location Sensor.
 *
 *<code><pre>
 * CLASS:
 * 	extends ListActivity.
 *
 * 	implements
 *      Constants - for the constants used
 *      SimpleCursorAdapter.ViewBinder - to update the view of each item in setViewValue
 *
 * RESPONSIBILITIES:
 * 	displays the list of poi tagged locations.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class PoiLocationsListActivity extends ListActivity implements SimpleCursorAdapter.ViewBinder,
																	  Constants, OnClickListener,
																	  LocConstants, DbSyntax {

    private static final String TAG = PoiLocationsListActivity.class.getSimpleName();

    private static final String SMART_PROFILE_LAUNCH_INTENT = "com.motorola.contextual.smartprofilelaunch";

    private static final String POI_LOC_ARRAY = "PoiLocArray";
    private static final String SHOW_CHECK_BOX = "ShowCheckBox";

    public interface MenuOptions {
	    final int EDIT = 0;
	    final int DELETE = EDIT + 1;
	    final int ALL_LOCATIONS = DELETE + 1;
    }

    /** private class to store the poi tag and name of checked locations
     *  and to be used in an hashmap
     */
    private static class CheckedLocationValues {
    	String poiTag;
    	String poiName;
    }

    private LocDbColumns clickedRowInfo = null;
    private Context mContext = null;
    private boolean showCheckBox = false;
    private boolean isIlsInstalled = true;
    private ArrayList<String> passedPoiTagsList = new ArrayList<String>();
    private HashMap<String, CheckedLocationValues> checkedLocValues = new HashMap<String, CheckedLocationValues>();
    private Cursor mLocationsListCursor = null;
    
    /** onCreate()
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.location_list);
        isIlsInstalled = Util.isApplicationInstalled(mContext,
                ILS_LAUNCH_INTENT);
        if (savedInstanceState != null) {
            onRestoreState(savedInstanceState);
        } else {
            Intent intent = getIntent();
            if (LOG_DEBUG) {
                Log.d(TAG, "intent is " + intent.toUri(0));
            }
            String action = intent.getAction();
            if (action != null && !action.equals(SMART_PROFILE_LAUNCH_INTENT)) {
                showCheckBox = true;
                String config = intent.getStringExtra(EXTRA_CONFIG);
                if (config != null && !config.isEmpty()) {
                    // This is edit case. The user is trying to modify the
                    // existing configuration. Populate passedPoiTagsList from
                    // this config
                    LocationUtils.extractLocationsFromConfig(config,
                            passedPoiTagsList);
                    if (LOG_INFO) {
                        Log.i(TAG, "onCreate incoming config " + config);
                    }
                }
            }
        }
        ActionBar ab = getActionBar();
        ab.setTitle(R.string.smart_rules_loc);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setIcon(R.drawable.ic_launcher_smartrules);
        ab.show();
        setupActionBarItemsVisibility(false);
    }

    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        if(LOG_VERBOSE) Log.v(TAG, "In onDestroy");
        super.onDestroy();
        checkedLocValues.clear();
    }

    /** onResume()
     */
    @Override
    protected void onResume() {
        if(LOG_VERBOSE) Log.v(TAG, "In onResume");
        super.onResume();
        checkedLocValues.clear();
        displayPoiLocationsList();
    }

    /** Saves the instance state when an onPause is triggered.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(outState != null) {
	        outState.putStringArrayList(POI_LOC_ARRAY, passedPoiTagsList);
	        outState.putBoolean(SHOW_CHECK_BOX, showCheckBox);
        }
    }

    /**
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if(requestCode == ADD_LOC && resultCode == RESULT_OK && intent != null) {
            if(LOG_INFO) Log.i(TAG, "Returned RESULT_OK from ILS with intent "+intent.toURI());

            LocDbColumns columnValues = new LocDbColumns();

            columnValues.name = intent.getStringExtra(SEE_MY_LOC_NAME);
            columnValues.address = intent.getStringExtra(SEE_MY_LOC_ADDRESS);
            columnValues.lat = intent.getDoubleExtra(SEE_MY_LOC_LAT, 0);
            columnValues.lng = intent.getDoubleExtra(SEE_MY_LOC_LNG, 0);
            columnValues.radius = intent.getFloatExtra(SEE_MY_LOC_ACCURACY, 0);

            if(LOG_DEBUG) Log.d(TAG, "name = "+columnValues.name+"; address = "+columnValues.address
            					+"; lat = "+columnValues.lat+"; lng = "+columnValues.lng);

            if(columnValues.address != null) {
            	String poiTag = POILOC_TAG_PREFIX + new Date().getTime();
            	// Need to check if the location name is null or a blank string and
            	// if so then the address is stored in the name column.
            	if(columnValues.name == null || columnValues.name.trim().length() == 0)
            		columnValues.name = columnValues.address;
            	columnValues.poiType = USER_POI;
            	Uri insertedUri = RemoteLocationDatabaseOps.addPoiTagTableEntry(mContext, poiTag, columnValues);
		if(!checkAndShowInsertErrorToast(insertedUri)) {
			// Add this location to the list of checked locations if the activity was launched
			// from puzzle builder to select a location.
			if(showCheckBox) {
                		passedPoiTagsList.add(poiTag);
                		CheckedLocationValues addedLoc = new CheckedLocationValues();
                		addedLoc.poiName = columnValues.name;
                		addedLoc.poiTag = columnValues.poiTag;
                		checkedLocValues.put(poiTag, addedLoc);
			}
		}
            }
        }
    }

    /** onCreateContextMenu()
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        clickedRowInfo = (LocDbColumns) ((View)view.getParent()).getTag();
        if(clickedRowInfo != null) {
    		// if launched from menu Profiles -> Locations then show the context menu.
        	// do not show if launched from Puzzle Builder
    		if(!showCheckBox) {
                // Sets the menu header to be the title of the selected note.
                menu.setHeaderTitle(clickedRowInfo.name);

                menu.add(0, MenuOptions.EDIT, 0, R.string.edit);
                menu.add(0, MenuOptions.DELETE, 0, R.string.delete);
    		}
        }
    }

    /** onContextItemSelected()
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case MenuOptions.EDIT:
	        	showEditLocationActivity();
	        	return true;

	        case MenuOptions.DELETE:
	            if(LOG_DEBUG) Log.d(TAG, "Delete selected - Delete from poi table and reset poi tag in loctime table");
	            if(!LocationUtils.isPoiTagInUse(this, clickedRowInfo.poiTag)) {
		            RemoteLocationDatabaseOps.deletePoiTag(mContext,clickedRowInfo.poiTag);
		            displayPoiLocationsList();
	            } else {
	            	Toast.makeText(mContext, R.string.loc_delete_error, Toast.LENGTH_SHORT).show();
	            }
	            return true;

	        default:
	            return super.onContextItemSelected(item);
        }
    }

    /** onOptionsItemSelected()
     *  handles the back press of icon in the ICS action bar.
     */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = super.onOptionsItemSelected(item);
		
		switch (item.getItemId()) {
			case android.R.id.home:
				setConfigureResultCanceled();
				finish();
				result = true;
				break;
			case R.id.edit_add_button:
				try {
                    Intent intent = new Intent();
                    intent.setAction(ILS_LAUNCH_INTENT);
                    startActivityForResult(intent, ADD_LOC);
                } catch (Exception e) {
                    Log.e(TAG, "Cannot launch ILS");
                    e.printStackTrace();
                }
				result = true;
				break;
			case R.id.edit_save:
				if(checkedLocValues.size() > 0) {
	    			setConfigureResultOk();
	    		} else {
	    			Log.e(TAG, "Error with the selected list - sending cancel");
	                setConfigureResultCanceled();
	    		}
	            finish();
				result = true;
				break;
			case R.id.edit_cancel:
				setConfigureResultCanceled();
	            finish();
				result = true;
				break;
		}		
		return result;
	} 
	
    /** displays a toast if the insertion of the location failed.
     *
     * @param uri - uri returned from Location Sensor insert().
     */
    protected boolean checkAndShowInsertErrorToast(Uri uri) {
    	boolean displayError = false;
    	if(uri == null)
    		displayError = true;
    	else if(Long.parseLong(Uri.parse(uri.toString()).getLastPathSegment()) == -1)
    			displayError = true;

    	if(displayError)
    		Toast.makeText(mContext, R.string.duplicate_location, Toast.LENGTH_SHORT).show();
        return displayError;
    }

    /** restores the critical variables saved during pause operation.
     */
    protected void onRestoreState(Bundle savedInstanceState) {
    	if (savedInstanceState != null) {
    	    passedPoiTagsList = savedInstanceState.getStringArrayList(POI_LOC_ARRAY);
    	    showCheckBox = savedInstanceState.getBoolean(SHOW_CHECK_BOX, false);
    	}
    }

    /** sets the onClickListener for the + icon in the title bar if it
     *  is to be shown.
     *  
     *  @param enableDoneButton - true to enable the done button else false.
     */
    protected void setupActionBarItemsVisibility(boolean enableDoneButton) {    
    	int editFragmentOption = EditFragment.EditFragmentOptions.DEFAULT;
    	if(showCheckBox && enableDoneButton)
    		editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
    	else if(showCheckBox && !enableDoneButton)
    		editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;

        // Add menu items from fragment
    	Fragment fragment = EditFragment.newInstance(editFragmentOption, isIlsInstalled);
 		getFragmentManager().beginTransaction()
 				.replace(R.id.edit_fragment_container, fragment, null).commit();
    }

    
    /** launches the edit location activity for the location selected by the user.
     */
    private void showEditLocationActivity() {
        Bundle bundle = new Bundle();
       	bundle.putString(POI, clickedRowInfo.poiTag);

        Intent intent = new Intent(mContext, EditLocationActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }
    
	/**
	 * Get selected location infomation from last time saved selected location.
	 * 
	 * @param cursor
	 *            - location cursor
	 */
	private void getSelectedLocInf(Cursor cursor) {

		if (cursor != null && cursor.moveToFirst()) {
			ArrayList<String> tempPoiTagList = new ArrayList<String>();
			tempPoiTagList.addAll(passedPoiTagsList);
			if (tempPoiTagList.size() > 0) {
				do {
					String poiTag = cursor
							.getString(cursor.getColumnIndex(POI));
					if (cursor.getString(cursor.getColumnIndex(POITYPE))
							.equals(SUGGESTED_POI)) {
						passedPoiTagsList.remove(poiTag);
						if (checkedLocValues.containsKey(poiTag))
							checkedLocValues.remove(poiTag);
					} else if (tempPoiTagList.contains(poiTag)) {
						if (LOG_DEBUG)
							Log.d(TAG,
									"Poi Name matched with currentCheckedName");
						if (!checkedLocValues.containsKey(poiTag)) {
							CheckedLocationValues checkedLoc = new CheckedLocationValues();
							checkedLoc.poiTag = poiTag;
							checkedLoc.poiName = cursor.getString(cursor
									.getColumnIndex(NAME));
							checkedLocValues.put(poiTag, checkedLoc);
						}
					}
					if (LOG_DEBUG)
						Log.d(TAG,
								"poiTagsSelected = "
										+ passedPoiTagsList.toString());
				} while (cursor.moveToNext());
			}
		}

	}

    /** displays the list of poi tagged locations by querying the Location Sensor content providers poi table.
     */
    private void displayPoiLocationsList() {
        try {
            mLocationsListCursor = mContext.getContentResolver().query(Uri.parse(LOC_POI_URI), null, null, null, null);
            if(mLocationsListCursor != null) {
                this.startManagingCursor(mLocationsListCursor);
                getSelectedLocInf(mLocationsListCursor);
                if(mLocationsListCursor.moveToFirst()) {
                    if(LOG_DEBUG) DatabaseUtils.dumpCursor(mLocationsListCursor);
                    hideErrorMessage();
                    populatePoiLocationsListView(mLocationsListCursor);
                }
                else {
                    showErrorMessage(getString(R.string.no_meaningful_location));
                }
            }
            else {
                showErrorMessage(NULL_CURSOR);
            }
        } catch (Exception e) {
            Log.e(TAG, "Query to Location Sensor Content Provider crashed");
            e.printStackTrace();
            showErrorMessage("PROVIDER_CRASH");
        }

    }

    /** populates the list view
     *
     * @param cursor - cursor to populate the list view
     */
    private void populatePoiLocationsListView(Cursor cursor) {
        if(LOG_DEBUG) Log.d(TAG, "Populating the poi tags view");

        String[] from = {POI, ADDRESS, POI};
        int[] to = {R.id.placelist_first_text_line, R.id.placelist_second_text_line, R.id.placelist_checkbox};

        // setup the adapter, view binder, etc.
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.location_list_row, cursor, from, to);

        adapter.setViewBinder(this);
        setListAdapter(adapter);
    }

    /** Displays an error message if the cursor was null or empty (returned from Location Sensor
     * 	content provider)
     *
     * @param errorMsg - Message to be displayed to the user.
     */
    private void showErrorMessage(String errorMsg) {

        RelativeLayout errRl  	= null;
        TextView errTextView  = (TextView) findViewById(R.id.failmessage_text);
        if(errTextView != null) {
            errRl = (RelativeLayout) errTextView.getParent();
            errRl.setVisibility(View.VISIBLE);
            errTextView.setText(errorMsg);
            if(showCheckBox)
            	setupActionBarItemsVisibility(false);
        }
    }

    /** hides the error message layout when it does not requires display.
     */
    private void hideErrorMessage() {
        RelativeLayout errRl  	= null;
        TextView errTextView  = (TextView) findViewById(R.id.failmessage_text);
        if(errTextView != null) {
            errRl = (RelativeLayout) errTextView.getParent();
            errRl.setVisibility(View.GONE);
        }
    }

    /** sets the view value for each row.
     *
     * @param view - the view to bind the data to
     * @param cursor - cursor with data
     * @param columnIndex - column index for which the view value is being set
     * @return true or false
     */
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

        boolean boundHere = false;
        int id = view.getId();

        String poiType = cursor.getString(cursor.getColumnIndex(POITYPE));
        boolean isPoiSuggested = false;
        if(poiType != null)
        	isPoiSuggested = poiType.equals(SUGGESTED_POI)?true:false;

        String address = cursor.getString(cursor.getColumnIndex(ADDRESS));
        String name = cursor.getString(cursor.getColumnIndex(NAME));
        if(id == R.id.placelist_first_text_line && view instanceof TextView) {
        	if(address != null) {
        		if(address.equals(name))
        			((TextView) view).setText(cursor.getString(cursor.getColumnIndex(ADDRESS)));
        		else
        			((TextView) view).setText(cursor.getString(cursor.getColumnIndex(NAME)));
        	}
        	else
        		((TextView) view).setText(cursor.getString(cursor.getColumnIndex(NAME)));

            ((TextView) view).setVisibility(View.VISIBLE);
            setViewTagsforLaterUse(cursor, view);
            RelativeLayout leftWrapper = (RelativeLayout) view.getParent();
            leftWrapper.setOnClickListener(this);
            leftWrapper.setOnCreateContextMenuListener(this);

            LinearLayout rightWrapper = (LinearLayout) ((LinearLayout) leftWrapper.getParent()).findViewById(R.id.right_wrapper);
            rightWrapper.setOnClickListener(this);

            boundHere = true;
        }
        if(id == R.id.placelist_second_text_line && view instanceof TextView) {
        	if(isPoiSuggested) {
        		((TextView) view).setText(mContext.getString(R.string.suggested_location));
	            ((TextView) view).setVisibility(View.VISIBLE);
	            ((TextView) view).setTextColor(mContext.getResources().getColor(R.color.suggestion_green));
        	} else if(address != null && !address.equals(name)) {
	            ((TextView) view).setText(cursor.getString(cursor.getColumnIndex(ADDRESS)));
	            ((TextView) view).setVisibility(View.VISIBLE);
	            ((TextView) view).setTextColor(mContext.getResources().getColor(R.color.second_line));
        	}
        	else
	            ((TextView) view).setVisibility(View.GONE);

            boundHere = true;
        }
        else if(id == R.id.placelist_checkbox && view instanceof CheckBox) {
            if(showCheckBox) {
            	View divider = (View) ((LinearLayout) view.getParent()).findViewById(R.id.divider);
            	divider.setVisibility(View.VISIBLE);
                CheckBox checkBox = (CheckBox) view;
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(false);
                String poiTag = cursor.getString(cursor.getColumnIndex(POI));
                if(checkedLocValues.containsKey(poiTag)) {
                    ((CheckBox) view).setChecked(true);
                }
                if(passedPoiTagsList != null && passedPoiTagsList.size() > 0)
                	setupActionBarItemsVisibility(true);
                else
                	setupActionBarItemsVisibility(false);
                boundHere = true;
            }
        }
        return boundHere;
    }

    /** stores the values in tag for later use
     *
     * @param cursor - cursor with data
     * @param view - view where the tag is being bound to
     */
    private void setViewTagsforLaterUse(Cursor cursor, View view) {

    	LocDbColumns info = new LocDbColumns();

        info.name = cursor.getString(cursor.getColumnIndex(NAME));
        info.poiTag = cursor.getString(cursor.getColumnIndex(POI));
        info.poiType = cursor.getString(cursor.getColumnIndex(POITYPE));

        if(passedPoiTagsList.size() > 0) {
            for(int i = 0; i < passedPoiTagsList.size(); i++) {
                if(passedPoiTagsList.get(i).equals(info.poiTag)) {
                    info.isChecked = true;
                }
            }
        }

        // Reverting to the old code that worked. With the new code it does not get the right
        // listrow tags and hence see a different location details. Also a crash is seen when
        // a dialog is shown on the top of the list and this setViewValue() is getting called.
        ((View) ((View)view.getParent()).getParent()).setTag(info);
        //LinearLayout rowLayout = (LinearLayout) view.getRootView().findViewById(R.id.row_layout);
        //rowLayout.setTag(info);
    }

    /** Convenience method to set the RESULT_OK and return to the calling app.
     */
    public void setConfigureResultOk() {
        Intent intent = new Intent();
        ArrayList<String> poiTagsList;
        ArrayList<String> poiNamesList;
        Iterator<CheckedLocationValues> iter = checkedLocValues.values()
                .iterator();
        if (iter == null || checkedLocValues.isEmpty()) {
            Log.e(TAG,
                    "setConfigureResultOk checkedLocValues does not have anything stored to iterate through");
            setResult(RESULT_CANCELED, null);
        } else {
            poiTagsList = new ArrayList<String>();
            poiNamesList = new ArrayList<String>();
            while (iter.hasNext()) {
                CheckedLocationValues locationValue = iter.next();
                poiTagsList.add(locationValue.poiTag);
                poiNamesList.add(locationValue.poiName);
            }
            String config = LocationUtils.getConfig(poiTagsList);
            String description = LocationUtils.getDescription(this,
                    poiNamesList);
            intent.putExtra(EXTRA_CONFIG, config);
            intent.putExtra(EXTRA_DESCRIPTION, description);
            setResult(RESULT_OK, intent);
            if (LOG_INFO) {
                Log.i(TAG, "setConfigureResultOk setting result for config "
                        + config + " description " + description);
            }
        }
    }

    /** Convenience method to set the RESULT_CANCELED and return to the calling app.
     */
    public void setConfigureResultCanceled() {
    	Intent intent = new Intent();
    	setResult(RESULT_CANCELED, intent);
    }

    /** onClick()
     */
	public void onClick(View view) {
        clickedRowInfo = (LocDbColumns) ((View)view.getParent()).getTag();
		if(view.getId() == R.id.left_wrapper && view instanceof RelativeLayout) {
			showEditLocationActivity();
    	} else if(view.getId() == R.id.right_wrapper && view instanceof LinearLayout) {
    		handleCheckBoxListItemSelection(view);
    	}
	}

    /** Handles the click of items via a physical keyboard in a list row.
     */
	@Override
    protected void onListItemClick(ListView list, View view, int position, long id)
    {
    	mLocationsListCursor.moveToPosition(position);
        clickedRowInfo = new LocDbColumns();
        clickedRowInfo.name = mLocationsListCursor.getString(mLocationsListCursor.getColumnIndex(NAME));
        clickedRowInfo.poiTag = mLocationsListCursor.getString(mLocationsListCursor.getColumnIndex(POI));
        clickedRowInfo.poiType = mLocationsListCursor.getString(mLocationsListCursor.getColumnIndex(POITYPE));

        if(passedPoiTagsList.size() > 0) {
            for(int i = 0; i < passedPoiTagsList.size(); i++) {
                if(passedPoiTagsList.get(i).equals(clickedRowInfo.poiTag)) {
                	clickedRowInfo.isChecked = true;
                }
            }
        }
            
    	if(showCheckBox) {
    		handleCheckBoxListItemSelection(view);
    	}
    	else {
			showEditLocationActivity();
    	}
    }
	
	/** handles the selection of list items when the check box is being displayed.
	 * 
	 * @param view - View item
	 */
	private void handleCheckBoxListItemSelection(View view) {
        CheckBox checkBox = (CheckBox) view.findViewById(R.id.placelist_checkbox);
        if(clickedRowInfo.isChecked) {
            passedPoiTagsList.remove(clickedRowInfo.poiTag);
            if(checkedLocValues.containsKey(clickedRowInfo.poiTag))
            	checkedLocValues.remove(clickedRowInfo.poiTag);
            checkBox.setChecked(false);
            clickedRowInfo.isChecked = false;
        }
        else {
            passedPoiTagsList.add(clickedRowInfo.poiTag);
            if(!checkedLocValues.containsKey(clickedRowInfo.poiTag)) {
            	CheckedLocationValues checkedLoc = new CheckedLocationValues();
            	checkedLoc.poiName = clickedRowInfo.name;
            	checkedLoc.poiTag = clickedRowInfo.poiTag;
            	checkedLocValues.put(clickedRowInfo.poiTag, checkedLoc);
            }
            checkBox.setChecked(true);
            clickedRowInfo.isChecked = true;
        }
        view.setTag(clickedRowInfo);
        if(checkedLocValues.size() > 0)
        	setupActionBarItemsVisibility(true);
        else
        	setupActionBarItemsVisibility(false);
        if(LOG_DEBUG) Log.d(TAG, "poiTagsSelected = "+passedPoiTagsList.toString());
        boolean isSuggested = clickedRowInfo.poiType != null && clickedRowInfo.poiType.equals(SUGGESTED_POI);

        if(clickedRowInfo.isChecked && isSuggested) {
        	if(LOG_DEBUG) Log.d(TAG, "row checked is suggested so launch edit location activity for "+clickedRowInfo._id);
        	showEditLocationActivity();
        }
	}
}
