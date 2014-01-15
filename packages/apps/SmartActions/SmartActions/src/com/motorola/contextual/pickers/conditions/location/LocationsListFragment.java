/*
 * Copyright (C) 2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * MXDN83        06/05/2012 Smart Actions 2.1 Created file
 * XPR643        2012/08/10 Smart Actions 2.2 New architecture for data I/O
 */

package com.motorola.contextual.pickers.conditions.location;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.motorola.contextual.pickers.CustomListAdapter;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.conditions.location.LocationActivity.CheckedLocationValues;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps;
import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps.LocDbColumns;
import com.motorola.contextual.smartprofile.locations.LocConstants;
import com.motorola.contextual.smartprofile.locations.LocationUtils;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment displays the list of locations that have been tagged with a poitag
 * in Location Sensor.
 *
 *<code><pre>
 * CLASS:
 *  extends PickerFragment.
 *
 *  implements
 *      Constants - for the constants used
 *      LocConstants - location sensor specific constants
 *      DialogInterface.OnClickListener - done btn click listener
 *      View.OnClickListener - click listener for 'Add a location'
 *
 * RESPONSIBILITIES:
 *  displays the list of poi tagged locations.
 *
 * COLABORATORS:
 *  None
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class LocationsListFragment extends PickerFragment implements LocConstants, Constants,
    DialogInterface.OnClickListener, OnClickListener{

    protected static final String TAG = LocationsListFragment.class.getSimpleName();
    private Cursor mLocationsListCursor = null;
    //Locations UI List items
    private ArrayList<ListItem> mItems;
    private ListView mListView;
    private final ArrayList<String> passedPoiTagsList = new ArrayList<String>();
    private int mListType;
    private boolean[] mCheckedItems;

    //Config intent constants
    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIG_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIG_INTENT";

    private final HashMap<String, CheckedLocationValues> checkedLocValues = new HashMap<String,
            CheckedLocationValues>();

    /**
     * The true factory-style constructor. Forwards the passed in intents to onCreate.
     *
     * @param mInputConfigs - used to pass data to this fragment
     * @param mOutputConfigs - used to pass data to the host activity
     */
    public static LocationsListFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {
        Bundle args = new Bundle();

        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }

        LocationsListFragment f = new LocationsListFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().getParcelable(INPUT_CONFIGS_INTENT) != null) {
                mInputConfigs = (Intent) getArguments().getParcelable(INPUT_CONFIGS_INTENT);
            }

            if (getArguments().getParcelable(OUTPUT_CONFIGS_INTENT) != null) {
                mOutputConfigs = (Intent) getArguments().getParcelable(OUTPUT_CONFIGS_INTENT);
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        //don't have to create the view everytime onCreateView is called
        if(mContentView == null) {
            try {
                final Picker.Builder pickerBuilder = new Picker.Builder(mHostActivity);
                String title = "";
                //Check the mode, true - coming from profile,false - from the picker list
                if(mInputConfigs.getBooleanExtra(LocationActivity.MODE_STRING, false)) {
                    pickerBuilder.setTitle(Html.fromHtml(getString(R.string.loc_secondary_text)));
                    mListType = ListItem.typeTHREE;
                    title = getString(R.string.loc_secondary_text);
                } else {
                    mListType = ListItem.typeTWO;
                    pickerBuilder.setPositiveButton(R.string.iam_done, this);
                    title = getString(R.string.loc_prompt);
                }
                pickerBuilder.setTitle(Html.fromHtml(title));
                buildListItems(true);
                pickerBuilder.setMultiChoiceItems(mItems, mCheckedItems, null);
                final Picker picker = pickerBuilder.create();
                mContentView = picker.getView();
                mListView = (ListView) mContentView.findViewById(R.id.list);
            }catch(final Exception e) {
                Log.e("Location List fragment", "Exception:");
                e.printStackTrace();
            }
        }
        return mContentView;
    }

    /**
     * Closes cursor, if not null.
     *
     * @see android.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (mLocationsListCursor != null && !mLocationsListCursor.isClosed()) {
            mLocationsListCursor.close();
        }
        super.onDestroy();
    }

    /**
     * Handles the done bottom button click event
     * Required by DialogInterface.onClickListener interface
     */
    public void onClick(final DialogInterface dialog, final int which) {
        final SparseBooleanArray checked = mListView.getCheckedItemPositions();
        CheckedLocationValues checkedLoc;
        //Ignore the last item,which is add a location action item
        for(int i=0; i<mItems.size();i++) {
            if(checked.get(i)) {
                checkedLoc = new CheckedLocationValues();
                checkedLoc.poiName = mItems.get(i).mLabel.toString();
                checkedLoc.poiTag = (String) mItems.get(i).mMode;
                checkedLocValues.put(checkedLoc.poiTag, checkedLoc);
            }
        }
        mOutputConfigs.putExtra(LocationActivity.LOCATION_TAG_HASHMAP, checkedLocValues);
        mHostActivity.onReturn(mOutputConfigs, this);
    }

    /**
     * Handles 'Add a location' click event
     * Required by android.view.View.OnClickListener interface
     */
    public void onClick(final View v) {
//        try {
//            final Intent intent = new Intent();
//            intent.setAction(ILS_LAUNCH_INTENT);
//            startActivityForResult(intent, ADD_LOC);
//        } catch (final Exception e) {
//            e.printStackTrace();
//        }
    }

    /*
     * invokes when user clicks on + sign
     * launches new location activity
     */
    public void showAddLocation() {
        try {
            final Intent intent = new Intent();
            intent.setAction(ILS_LAUNCH_INTENT);
            startActivityForResult(intent, ADD_LOC);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
        if(requestCode == ADD_LOC && resultCode == Activity.RESULT_OK && intent != null) {
            //From ILS add location app, add the new location
            final LocDbColumns columnValues = new LocDbColumns();

            columnValues.name = intent.getStringExtra(SEE_MY_LOC_NAME);
            columnValues.address = intent.getStringExtra(SEE_MY_LOC_ADDRESS);
            columnValues.lat = intent.getDoubleExtra(SEE_MY_LOC_LAT, 0);
            columnValues.lng = intent.getDoubleExtra(SEE_MY_LOC_LNG, 0);
            columnValues.radius = intent.getFloatExtra(SEE_MY_LOC_ACCURACY, 0);
            //There is a chance that ILS returns empty addresses & names, particularly
            //when using wifi networks to determine locations,have a guard for those cases
            if(columnValues.address != null  && columnValues.address.trim().length() != 0) {
                final String poiTag = POILOC_TAG_PREFIX + new Date().getTime();
                // Need to check if the location name is null or a blank string and
                // if so then the address is stored in the name column.
                if(columnValues.name == null || columnValues.name.trim().length() == 0)
                    columnValues.name = columnValues.address;
                columnValues.poiType = USER_POI;
                final Uri insertedUri = RemoteLocationDatabaseOps
                        .addPoiTagTableEntry(mHostActivity, poiTag, columnValues);
                boolean displayError = false;
                if(insertedUri == null) {
                    displayError = true;
                }else if(Long.parseLong(Uri.parse(insertedUri.toString()).getLastPathSegment()) == -1) {
                    displayError = true;
                }
                if(displayError) {
                    Toast.makeText(mHostActivity, R.string.duplicate_location, Toast.LENGTH_SHORT).show();
                }else {
                    //To have the newly added location checked
                    passedPoiTagsList.add(poiTag);
                    buildListItems(false);
                }
            }
        }else if(requestCode == DEL_LOC && resultCode == Activity.RESULT_OK) {
            //From edit location activity, rebuild the list items
            //location is either edited or deleted, rebuild in either case
            buildListItems(false);
        }
    }

    /**
     * launches the edit location activity for the location selected by the user.
     *
     * @param poiTag - poi Tag for the selected location
     */
    private void showEditLocation(final String poiTag) {
        final Bundle bundle = new Bundle();
        bundle.putString(POI, poiTag);
        final Intent intent = new Intent(mHostActivity, EditLocationActivity.class);
        intent.putExtras(bundle);
        startActivityForResult(intent, DEL_LOC);
    }

    /**
     * Gets the cursor, builds the mItems to be passed onto the list view, called the
     * first time and whenever there's a change in the content
     */
    private void buildListItems(final boolean firstTime) {
        //Assign it to null first, Just to make sure the reference is updated

        // Check to see if it's not null, if it's not null, close it, then can resuse it.
        if (mLocationsListCursor != null && !mLocationsListCursor.isClosed()) {
            mLocationsListCursor.close();
        }
        mLocationsListCursor = null;
        mLocationsListCursor = mHostActivity.getContentResolver()
                .query(Uri.parse(LOC_POI_URI), null, null, null, null);
        mItems = new ArrayList<ListItem>();
        if(mListType == ListItem.typeTWO) {
            //Coming from the picker list or rule builder,
            //see if there are any pois passed in the intent
            final String config = mInputConfigs.getStringExtra(EXTRA_CONFIG);
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

        // cjd - please refactor - this is really messy and long. Since this is new code (not legacy) it must be
        //         refactored before it can be merged.
        if(mLocationsListCursor != null) {
            //create the array of cursor's size+1 to accommodate the 'add new location'
            //action item at the bottom, even though it's not semantically included
            //in the list of selectable locations so adapter doesn't throw index
            //out of bounds exception
            //Assign it to null first, Just to make sure the reference is updated
            mCheckedItems = null;
            mCheckedItems = new boolean[mLocationsListCursor.getCount()+1];
            if(mLocationsListCursor.moveToFirst()) {
                String locName, locAddr;
                int i=0;
                final int nameIndex=mLocationsListCursor.getColumnIndex(NAME), addrIndex=mLocationsListCursor.getColumnIndex(ADDRESS);
                final int tagIndex=mLocationsListCursor.getColumnIndex(POI);
                do {
                    try {
                        locName = mLocationsListCursor.getString(nameIndex);
                        locAddr = mLocationsListCursor.getString(addrIndex);
                        final String tag = mLocationsListCursor.getString(tagIndex);
                        if(locName != null) {
                            if(locAddr != null) {
                                if(locName.equals(locAddr)) {
                                    //If both are the same then just show one field
                                    locAddr = null;
                                }
                            }
                            mItems.add(new ListItem(-1, locName, locAddr,
                                    mListType, tag,
                                    new View.OnClickListener() {
                                        public void onClick(final View v) {
                                            showEditLocation(tag);
                                        }
                                    }));
                            //Sets the pre-selected items
                            if(passedPoiTagsList.contains(mLocationsListCursor
                                    .getString(mLocationsListCursor.getColumnIndex(POI)))) {
                                mCheckedItems[i] = true;
                            }else {
                                mCheckedItems[i] = false;
                            }
                            i++;
                        }
                    }catch(final Exception e) {
                        //should never happen but having a guard anyways looking at
                        //some of the null database column values
                        e.printStackTrace();
                    }
                } while(mLocationsListCursor.moveToNext());
            }else {
                //TODO show the no locations list message, show toast for now
                //showErrorMessage(getString(R.string.no_meaningful_location));
                Toast.makeText(mHostActivity, getString(R.string.no_meaningful_location), Toast.LENGTH_LONG).show();
            }
            /*if(!mInputConfigs.getBooleanExtra(LocationActivity.MODE_STRING, false)) {
                mItems.add(new ListItem(R.drawable.ic_add, getString(R.string.add_loc),
                        null, ListItem.typeTHREE, null, this));
            }*/
            if(!firstTime) {
                final CustomListAdapter adapter = (CustomListAdapter)mListView.getAdapter();
                adapter.setItemsList(mItems);
                adapter.setCheckedItems(mCheckedItems);
                adapter.notifyDataSetChanged();
            }
            // cjd - why can the cursor not be closed here? if you do close it here, wrap this
            //                 in try/catch/finally, where cursor closed in finally block
        }else {
            Log.e(TAG, "Null cursor returned for SA locations");
        }
    }
}
