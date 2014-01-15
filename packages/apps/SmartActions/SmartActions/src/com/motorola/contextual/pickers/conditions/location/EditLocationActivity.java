/*
 * @(#)EditLocationActivity.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/01/20 NA				  Initial version
 * MXDN83        2012/06/12 NA                Updated to the new UI pickers style
 */
package com.motorola.contextual.pickers.conditions.location;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps;
import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps.LocDbColumns;
import com.motorola.contextual.smartprofile.locations.LocConstants;
import com.motorola.contextual.smartprofile.locations.LocationUtils;
import com.motorola.contextual.smartprofile.util.Util;

/**
 * This class displays the location on the map and allows the user to edit the
 * location name or address. When a new address is typed in the map would be
 * updated with the new location. Since this uses MapView, it has to extend
 * MapActivity.
 *
 *<code><pre>
 * CLASS:
 * 	extends MapActivity.
 *
 * 	implements
 *      Constants - for the constants used
 *
 * RESPONSIBILITIES:
 * 	displays the location on a map.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 *  None
 *</pre></code>
 **/
public class EditLocationActivity extends MapActivity implements Constants,
        LocConstants, TextWatcher, DbSyntax {

    private static final String TAG = EditLocationActivity.class.getSimpleName();
    private static final String mConfirmSuggestLoc		= "ConfirmSuggestedLoc";
    private static final String DELETE_CONFIRM_FRAGMENT = "DELETE_CONFIRM_FRAGMENT";
    private static final String EDIT_LOCATION_NAME_FRAGMENT = "EDIT_LOCATION_NAME_FRAGMENT";


    // cjd - perhaps this variable could be prefixed with 'm'
    private static LocDbColumns 		storedKeyValues 	 	= null;
    private MyItemizedOverlay 	mMyItemizedOverlay		= null;
    private Context 		  	mContext 			 	= null;
    // cjd - perhaps this variable could be prefixed with 'm'
    private TextView    	  	editLocationAddress 	= null;
    private MapView     		mMapView 				= null;
    private Button				mChangeButton 			= null;
    private Spinner             mSpinnerLocationName    = null;
    // cjd - this boolean is stored but never checked.
    private boolean             isLaunchedForSuggestedLoc = false;
    private ArrayAdapter<CharSequence> mLocationNamesAdapter = null;
    private ArrayList<CharSequence> mLocationNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_location_fragment);
        mContext = this;
        mSpinnerLocationName = (Spinner) findViewById(R.id.spinner_location_name);
        String[] locNames = getResources().getStringArray(R.array.location_names);
        mLocationNames = new ArrayList<CharSequence>();
        for(String str:locNames) {
            mLocationNames.add(str);
        }
        mLocationNamesAdapter = new ArrayAdapter<CharSequence>(mContext,
                android.R.layout.simple_spinner_item, mLocationNames);
        mLocationNamesAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mSpinnerLocationName.setAdapter(mLocationNamesAdapter);
        mSpinnerLocationName.setOnItemSelectedListener( new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                //Last name is 'custom', so launch the custom edit location name dialog
                if(position == mLocationNamesAdapter.getCount()-1) {
                    showEditLocationNameDialogFragment();
                }
                enableDoneButton();
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                enableDoneButton();
            }

        });

        Bundle bundle = getIntent().getExtras();

        // TODO: cjd - this method is long, should probably be refactored.
        if(bundle != null) {

            String poiTag = bundle.getString(POI);

            storedKeyValues = RemoteLocationDatabaseOps
                    .fetchLocationDetailsFromDb(mContext, poiTag);

            if(storedKeyValues != null && storedKeyValues._id != 0) {

                if(	(storedKeyValues.poiType != null
                        && 	storedKeyValues.poiType.equals(SUGGESTED_POI))
                    ||	 bundle.getBoolean(mConfirmSuggestLoc)	){
                    // cjd - this boolean is set but never checked.
                    isLaunchedForSuggestedLoc = true;
                }

                // Hide the soft keyboard and it will show up when user selects the edit boxes.
                getWindow().setSoftInputMode(WindowManager
                        .LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

                mMapView = (MapView) findViewById(R.id.googleMap_map);
                mMapView.setClickable(false);
                if(mMapView.isSatellite()) {
                    mMapView.getController().setZoom(mMapView.getMaxZoomLevel()
                            - DEFAULT_SATELLITE_ZOOM_LEVEL);
                }else {
                    mMapView.getController().setZoom(mMapView.getMaxZoomLevel()
                            - DEFAULT_MAP_ZOOM_LEVEL);
                }
                mMapView.setBuiltInZoomControls(true);
                mMapView.getZoomButtonsController().setVisible(true);

                if(storedKeyValues.lat != 0 && storedKeyValues.lng != 0) {

                    if(mMyItemizedOverlay != null) {
                        mMapView.getOverlays().remove(mMyItemizedOverlay);
                        mMapView.invalidate();
                    }
                    GeoPoint point = new GeoPoint((int)(storedKeyValues.lat * 1E6),
                            (int)(storedKeyValues.lng * 1E6));
                    OverlayItem overlayItem = new OverlayItem(point, "Lat Lng", "Lat Lng");
                    mMyItemizedOverlay = new MyItemizedOverlay(this,
                            getResources().getDrawable(R.drawable.ic_control_location_pin_me));
                    mMyItemizedOverlay.addOverlay(overlayItem);
                    mMapView.getOverlays().add(mMyItemizedOverlay);
                    mMapView.getController().animateTo(point);
                }

                ActionBar ab = getActionBar();
                ab.setDisplayHomeAsUpEnabled(true);
                ab.setIcon(R.drawable.ic_launcher_smartrules);
                ab.setTitle(getString(R.string.locations_title));
                 ab.show();
                 setLocChangeButtonOnClickListeners();
                 ((Button)findViewById(R.id.done_btn)).setOnClickListener(
                         new OnClickListener() {
                    public void onClick(View view) {
                       int updatedRows = RemoteLocationDatabaseOps
                               .updatePoiLocInDb(mContext, storedKeyValues,
                               storedKeyValues.poiType, getEditedLocationName());
                       if(updatedRows == 1) {
                           setResult(RESULT_OK);
                           finish();
                       }else {
                           Toast.makeText(mContext, R.string.duplicate_location,
                                   Toast.LENGTH_SHORT).show();
                       }
                    }
                 });
            }else {
                setResult(RESULT_CANCELED);
                finish();
            }
        }else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    /**
     * On config changes such as rotation, keyboard and screen size changes, we don't want to lose state
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        if(LOG_DEBUG) Log.d(TAG, "In onDestroy");
        super.onDestroy();
    }

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    /**
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if(requestCode == ADD_LOC && resultCode == RESULT_OK && intent !=null) {
            if(LOG_INFO) Log.i(TAG, "Returned RESULT_OK from ILS with intent "+intent.toURI());

            //setupActionBarItemsVisibility(true);
            storedKeyValues.address = intent.getStringExtra(SEE_MY_LOC_ADDRESS);
            storedKeyValues.name = intent.getStringExtra(SEE_MY_LOC_NAME);
            if(storedKeyValues.name == null || (storedKeyValues.name).isEmpty()) {
                storedKeyValues.name = storedKeyValues.address;
            }
            //storedKeyValues.poiTag = storedKeyValues.name;
            storedKeyValues.lat = intent.getDoubleExtra(SEE_MY_LOC_LAT, 0);
            storedKeyValues.lng = intent.getDoubleExtra(SEE_MY_LOC_LNG, 0);
            storedKeyValues.radius = intent.getFloatExtra(SEE_MY_LOC_ACCURACY, 0);
            storedKeyValues.wifiSSID = null;
            storedKeyValues.cellJsonValue = null;
            if(storedKeyValues.name != null) {
                //editLocationName.setText(storedKeyValues.name);
                setLocationName(storedKeyValues.name);
            }
            if(storedKeyValues.address != null)
                editLocationAddress.setText(storedKeyValues.address);

            if(storedKeyValues.lat != 0 && storedKeyValues.lng != 0) {
                if(mMyItemizedOverlay != null) {
                    mMapView.getOverlays().remove(mMyItemizedOverlay);
                    mMapView.invalidate();
                }
                GeoPoint point = new GeoPoint((int) (storedKeyValues.lat * 1E6),
                        (int) (storedKeyValues.lng * 1E6));
                OverlayItem addressOverlayItem = new OverlayItem(point,
                        "Address", "Address");
                mMyItemizedOverlay = new MyItemizedOverlay(this,
                        getResources().getDrawable(
                                R.drawable.ic_control_location_pin_me));
                mMyItemizedOverlay.addOverlay(addressOverlayItem);
                mMapView.getOverlays().add(mMyItemizedOverlay);
                mMapView.getController().animateTo(point);
            }else {
                startThreadToGeocodeAddress();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.picker_menu, menu);
        menu.findItem(R.id.menu_delete).setVisible(true);
        return true;
    }

    /** onOptionsItemSelected()
     *  handles the back press of icon in the ICS action bar and
     *  other action bar menu items
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                result = true;
                break;
            case R.id.menu_delete:
                showDeleteConfirmDialogFragment();
                break;
        }
        return result;
    }

    /** sets the onClickListeners for change location
     *  and the name edit text box listeners.
     */
    private void setLocChangeButtonOnClickListeners() {
        String name = "";

    //Check for emptiness. We check for null in onActivityResult, and do partially check for emptiness already as well, but let's be safe especially at this point in time.
        if (storedKeyValues.name != null && !(storedKeyValues.name).isEmpty()){
            name = storedKeyValues.name;
        } else if (storedKeyValues.address != null && !(storedKeyValues.address).isEmpty()){
            name = storedKeyValues.address;
        }

        if (!name.isEmpty()){//set location name here if not empty
            setLocationName(name);
        }

        editLocationAddress = (TextView) findViewById(R.id.edit_location_address);
        if(storedKeyValues.address != null) {
            editLocationAddress.setText(storedKeyValues.address);
        }

        mChangeButton = (Button) findViewById(R.id.change_button);
        if(Util.isApplicationInstalled(mContext, ILS_LAUNCH_INTENT)) {
            mChangeButton.setVisibility(View.VISIBLE);
            mChangeButton.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    try {
                        Intent intent = new Intent();
                        intent.setAction(ILS_LAUNCH_INTENT);
                        startActivityForResult(intent, ADD_LOC);
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot launch ILS");
                        e.printStackTrace();
                    }
                }
            });
        } else {
            mChangeButton.setVisibility(View.GONE);
        }
        enableDoneButton();
    }

    /** Enables the done button if certain constraints are met
     *
     */
    private void enableDoneButton() {
        // cjd - could simply be coded
        // findViewById(R.id.done_btn).setEnabled(! isTextEmpty((CharSequence)mSpinnerLocationName.getSelectedItem()));
        if(isTextEmpty((CharSequence)mSpinnerLocationName.getSelectedItem())) {
            findViewById(R.id.done_btn).setEnabled(false);
        }else {
            findViewById(R.id.done_btn).setEnabled(true);
        }
    }

    /**
     * Returns true if the text of the given Editable is not empty
     * @param text - text
     * @return - true if the text of the given Editable is not empty
     */
    public static boolean isTextEmpty(CharSequence text) {
       return ((text == null) || (StringUtils.isEmpty(text.toString())));
    }

    /** fetches the name from the edit text box and if it is null or empty then
     *  returns the name as in the DB else the new name entered by the user.
     *
     * @return - name of the location.
     */
    private String getEditedLocationName() {
        String editedName = ((CharSequence)mSpinnerLocationName.getSelectedItem()).toString();
        if(editedName == null || editedName.length() == 0)
            editedName = storedKeyValues.name;

        return editedName;
    }

    // TODO: per craig - this breaks encapsulation. This method and the geocodeAddress() method should be in another
    //            Geo or location class of some kind. I'm quite sure this code exists elsewhere
    /** starts a thread to geocode an address.
     */
    private void startThreadToGeocodeAddress() {
        Thread thread = new Thread() {

            public void run() {
                geocodeAddress();
            }
        };
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /** geocodes the address present in the storedKeyValues. If the geocode is a success
     *  will store the lat, lng to storedKeyValues and animate to this point on the map.
     */
    private void geocodeAddress() {

        if(LOG_DEBUG) Log.d(TAG, "geocodeAddress called for "+storedKeyValues.address);

        if(storedKeyValues.address != null) {
            try {
                Geocoder geocoder = new Geocoder(this);
                List<Address> addressList = geocoder.getFromLocationName(storedKeyValues.address, 1);
                // cjd - therefore, do something like this (preferably with a delay in between attempts_:
                //if (addressList.size() < 1)
                //	addressList = geocoder.getFromLocationName(passedAccuName, 1);

                if(addressList != null && addressList.size() < 1)
                    addressList = geocoder.getFromLocationName(storedKeyValues.address, 1);

                if(addressList != null && addressList.size() > 0) {
                    if(LOG_DEBUG) Log.d(TAG, "Geocode success");
                    if(mMyItemizedOverlay != null) {
                        mMapView.getOverlays().remove(mMyItemizedOverlay);
                        mMapView.invalidate();
                    }
                    storedKeyValues.lat = addressList.get(0).getLatitude();
                    storedKeyValues.lng = addressList.get(0).getLongitude();
                    GeoPoint point = new GeoPoint((int) (storedKeyValues.lat * 1E6), (int) (storedKeyValues.lng * 1E6));
                    OverlayItem addressOverlayItem = new OverlayItem(point, "Address", "Address");
                    mMyItemizedOverlay = new MyItemizedOverlay(this, getResources().getDrawable(R.drawable.ic_control_location_pin_me));
                    mMyItemizedOverlay.addOverlay(addressOverlayItem);
                    mMapView.getOverlays().add(mMyItemizedOverlay);
                    mMapView.getController().animateTo(point);
                }
                else if(LOG_DEBUG) Log.d(TAG, "Geocode returned null or no address list");


            } catch (IOException e) {
                Log.e(TAG, "Geocode of "+storedKeyValues.address+" failed");
                e.printStackTrace();
            }
        }
    }

    /** required for implementing TextWatcher
     */
    public void afterTextChanged(Editable s) {
        // Nothing to do

    }

    /** required for implementing TextWatcher
     */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing to do

    }

    /** required for implementing TextWatcher
     *  Enable/disable the done button
     */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        enableDoneButton();
    }

    private void showDeleteConfirmDialogFragment() {
        DialogFragment frag = DeleteConfirmDialogFragment.newInstance();
        frag.show(getFragmentManager(), DELETE_CONFIRM_FRAGMENT);
    }

    /** Delete Confirm Dialog
     */
    public static class DeleteConfirmDialogFragment extends DialogFragment {

        public static DeleteConfirmDialogFragment newInstance() {
            return new DeleteConfirmDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.delete)
            .setMessage(R.string.loc_delete_confirm)
            .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (!isAdded()) return;
                    int msgId;
                    if(LocationUtils.ifPoiCanBeDeleted(getActivity(), storedKeyValues.poiTag)) {
                        msgId = R.string.location_deleted;
                        RemoteLocationDatabaseOps.deletePoiTag(getActivity(),storedKeyValues.poiTag);
                        getActivity().setResult(RESULT_OK);
                        getActivity().finish();
                    } else {
                        msgId = R.string.loc_delete_error;
                    }
                    Toast.makeText(getActivity(), msgId, Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (!isAdded()) return;
                    getActivity().setResult(RESULT_CANCELED);
                    dismiss();
                }
            });
            return builder.create();
        }
    }

    private void showEditLocationNameDialogFragment() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag(EDIT_LOCATION_NAME_FRAGMENT);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        DialogFragment frag = EditLocationNameDialogFragment.newInstance();
        frag.show(ft, EDIT_LOCATION_NAME_FRAGMENT);
    }

    /** Edit Location Name Dialog
     */
    public static class EditLocationNameDialogFragment extends DialogFragment {

        public static EditLocationNameDialogFragment newInstance() {
            return new EditLocationNameDialogFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final EditText editText = new EditText(getActivity());
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.custom_loc_name)
            .setView(editText)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // cjd - minor issue - but remove these types of comments
                    // TODO Auto-generated method stub
                    if(editText.getText().length() > 0) {
                        ((EditLocationActivity) getActivity()).setLocationName(editText.getText());
                    }
                    dismiss();
                    ((EditLocationActivity) getActivity()).enableDoneButton();
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dismiss();
                }
            });
            return builder.create();
        }
    }

    /**
     * Sets the given text in the location name spinner
     * @param text
     */
    private void setLocationName(CharSequence text) {
        mLocationNamesAdapter.insert(text, 0);
        mLocationNamesAdapter.notifyDataSetChanged();
        mSpinnerLocationName.setSelection(0);
    }
}
