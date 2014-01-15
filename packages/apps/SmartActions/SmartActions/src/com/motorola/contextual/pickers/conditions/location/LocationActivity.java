/*
 * Copyright (C) 2010-2012, Motorola, Inc,
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/08/10 Smart Actions 2.2 New architecture for data I/O
 */

package com.motorola.contextual.pickers.conditions.location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartprofile.locations.LocConstants;
import com.motorola.contextual.smartprofile.locations.LocationUtils;
import com.motorola.contextual.smartrules.R;

/**
 * This activity presents locations list fragment.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity - activity base class for pickers
 *
 * RESPONSIBILITIES:
 *  Launches the locations list fragment.
 *  This activity can be launched from the rule builder and
 *  from the user profile screen. If launched from rule builder,
 *  it sends the chosen location back to the rule builder activity.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class LocationActivity extends MultiScreenPickerActivity implements LocConstants,Constants {

    //Used to communicate from the location list fragment to this activity
    public static final String LOCATION_TAG_HASHMAP="location_tag_hashmap";
    public static final String EDIT_LOCATION_TAG="edit_location";
    public static final String MODE_STRING="mode";
    public static final String CURRENT_POITAG = "CURRENT_POITAG";
    public static final String CURRENT_POINAME = "CURRENT_POINAME";

    private static final String TAG = LocationActivity.class.getSimpleName();
    private static final String SMART_PROFILE_LAUNCH_INTENT = "com.motorola.contextual.smartprofilelaunch";
    private PickerFragment mLocsListFragment;
    //Boolean to differentiate between picker and profile list modes
    // cjd should prefix this variable name with "is" like: "mIsProFileListMode"
    private boolean mProfileListMode = true;
    /** private class to store the poi tag and name of checked locations
     *  and to be used in an hashmap
     */
    public static class CheckedLocationValues {
        String poiTag;
        String poiName;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.locations_title));
        mInputConfigs = getIntent();
        String action = mInputConfigs.getAction();
        if(action != null) {
            if(!action.equals(SMART_PROFILE_LAUNCH_INTENT)) {
                // If there are no extras in the intent then this activity is just used to display
                // the poi locations list for the user to view else will have a check box for the user
                // to pick locations and return to rule builder.
                if(mInputConfigs.getCategories() == null) {
                    mProfileListMode = false;
                }
            }
        }
        mInputConfigs.putExtra(MODE_STRING, mProfileListMode);
        mLocsListFragment = LocationsListFragment.newInstance(mInputConfigs, mOutputConfigs);
        this.launchNextFragment(mLocsListFragment, R.string.location, true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        if(fromFragment == mLocsListFragment) {
            if(returnValue instanceof Intent) {
                if(!mProfileListMode) {
                    //Return the locations to rule builder and exit
                    setConfigureResultOk((HashMap<String, CheckedLocationValues>)
                            ((Intent)returnValue).getSerializableExtra(LOCATION_TAG_HASHMAP));
                }
                finish();
            }
        }
    }

    /** Convenience method to set the RESULT_OK and return to the calling app.
     *  @param checkedLocValues a hashmap of tags and checkedlocationvalues objects
     */
    public void setConfigureResultOk(HashMap<String,
            CheckedLocationValues> checkedLocValues) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.menu_add: {
                if (mLocsListFragment instanceof LocationsListFragment) {
                    final LocationsListFragment fragment = (LocationsListFragment)mLocsListFragment;
                    fragment.showAddLocation();
                }
            }
            break;
        }
        return result;
    }
}
