/*
 * @(#)EditLocationActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2011/01/20 NA				  Initial version
 *
 */
package com.motorola.contextual.smartprofile.locations;

import java.io.IOException;
import java.util.List;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.smartprofile.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;
import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps;
import com.motorola.contextual.smartprofile.RemoteLocationDatabaseOps.LocDbColumns;
import com.motorola.contextual.smartprofile.util.Util;

/**
 * This class displays the location on the map and allows the user to edit the location name
 * or address. When a new address is typed in the map would be updated with the new location.
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
public class EditLocationActivity extends MapActivity implements Constants, LocConstants, TextWatcher {

    private static final String TAG = EditLocationActivity.class.getSimpleName();
    private static final String mConfirmSuggestLoc		= "ConfirmSuggestedLoc";
    
    private LocDbColumns 		storedKeyValues 	 	= null;
    private MyItemizedOverlay 	mMyItemizedOverlay		= null;
    private Context 		  	mContext 			 	= null;
    private EditText    	  	editLocationName 	 	= null;
    private TextView    	  	editLocationAddress 	= null;
    private MapView     		mMapView 				= null;
    private Button				mChangeButton 			= null;
    private boolean				isLaunchedForSuggestedLoc = false;
    
    /** onCreate()
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if(LOG_DEBUG) Log.d(TAG, "In onCreate");

        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.edit_locations);
 		
        Bundle bundle = getIntent().getExtras();

        if(bundle != null) {

            String poiTag = bundle.getString(POI);
            if(LOG_INFO) Log.i(TAG, "poiTag = "+poiTag);

            storedKeyValues = RemoteLocationDatabaseOps.fetchLocationDetailsFromDb(mContext, poiTag);

            if(storedKeyValues != null && storedKeyValues._id != 0) {

            	if(	(storedKeyValues.poiType != null
            			&& 	storedKeyValues.poiType.equals(SUGGESTED_POI)) 
            		||	 bundle.getBoolean(mConfirmSuggestLoc)	){
            		isLaunchedForSuggestedLoc = true;
            	}

                // Hide the soft keyboard and it will show up when user selects the edit boxes.
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

                mMapView = (MapView) findViewById(R.id.googleMap_map);
                mMapView.setClickable(false);
                if(mMapView.isSatellite())
                    mMapView.getController().setZoom(mMapView.getMaxZoomLevel() - DEFAULT_SATELLITE_ZOOM_LEVEL);
                else
                    mMapView.getController().setZoom(mMapView.getMaxZoomLevel() - DEFAULT_MAP_ZOOM_LEVEL);

                mMapView.setBuiltInZoomControls(true);
                mMapView.getZoomButtonsController().setVisible(true);

                if(storedKeyValues.lat != 0 && storedKeyValues.lng != 0) {

                    if(mMyItemizedOverlay != null) {
                        mMapView.getOverlays().remove(mMyItemizedOverlay);
                        mMapView.invalidate();
                    }
                    GeoPoint point = new GeoPoint((int)(storedKeyValues.lat * 1E6), (int)(storedKeyValues.lng * 1E6));
                    OverlayItem overlayItem = new OverlayItem(point, "Lat Lng", "Lat Lng");
                    mMyItemizedOverlay = new MyItemizedOverlay(this, getResources().getDrawable(R.drawable.ic_control_location_pin_me));
                    mMyItemizedOverlay.addOverlay(overlayItem);
                    mMapView.getOverlays().add(mMyItemizedOverlay);
                    mMapView.getController().animateTo(point);
                }
                
                ActionBar ab = getActionBar();
                ab.setDisplayHomeAsUpEnabled(true);
                ab.setIcon(R.drawable.ic_launcher_smartrules);
         		ab.show(); 	
         		setupActionBarItemsVisibility(false);
         		setLocChangeButtonOnClickListeners();
            }
            else
                finish();
        }
        else
            finish();
    }

    /** onDestroy()
     */
    @Override
    public void onDestroy() {
        if(LOG_DEBUG) Log.d(TAG, "In onDestroy");
        super.onDestroy();
    }

    /** isRouteDisplayed()
     */
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

            setupActionBarItemsVisibility(true);
            storedKeyValues.address = intent.getStringExtra(SEE_MY_LOC_ADDRESS);
            storedKeyValues.name = intent.getStringExtra(SEE_MY_LOC_NAME);

            if(storedKeyValues.name == null)
                storedKeyValues.name = storedKeyValues.address;
            //storedKeyValues.poiTag = storedKeyValues.name;
            storedKeyValues.lat = intent.getDoubleExtra(SEE_MY_LOC_LAT, 0);
            storedKeyValues.lng = intent.getDoubleExtra(SEE_MY_LOC_LNG, 0);
            storedKeyValues.radius = intent.getFloatExtra(SEE_MY_LOC_ACCURACY, 0);
            storedKeyValues.wifiSSID = null;
            storedKeyValues.cellJsonValue = null;
            if(storedKeyValues.name != null && storedKeyValues.address != null
            		&& !storedKeyValues.name.equals(storedKeyValues.address))
                editLocationName.setText(storedKeyValues.name);

            if(storedKeyValues.address != null)
                editLocationAddress.setText(storedKeyValues.address);

            if(storedKeyValues.lat != 0 && storedKeyValues.lng != 0) {
            	if(mMyItemizedOverlay != null) {
                    mMapView.getOverlays().remove(mMyItemizedOverlay);
                    mMapView.invalidate();
                }
                GeoPoint point = new GeoPoint((int) (storedKeyValues.lat * 1E6), (int) (storedKeyValues.lng * 1E6));
                OverlayItem addressOverlayItem = new OverlayItem(point, "Address", "Address");
                mMyItemizedOverlay = new MyItemizedOverlay(this, getResources().getDrawable(R.drawable.ic_control_location_pin_me));
                mMyItemizedOverlay.addOverlay(addressOverlayItem);
                mMapView.getOverlays().add(mMyItemizedOverlay);
                mMapView.getController().animateTo(point);
            }
            else
                startThreadToGeocodeAddress();
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
				finish();
				result = true;
				break;
				
			case R.id.edit_confirm:
				int rowsUpdated = RemoteLocationDatabaseOps.updatePoiLocInDb(mContext,
                        	storedKeyValues, ACCEPTED_POI, getEditedLocationName());
				if(rowsUpdated == 1)
					setConfigureResult(RESULT_OK);
				else
					setConfigureResult(RESULT_CANCELED);
				finish();  
				result = true;
				break;
				
			case R.id.edit_save:
				int updatedRows = RemoteLocationDatabaseOps.updatePoiLocInDb(mContext, storedKeyValues,
    								storedKeyValues.poiType, getEditedLocationName());
				if(updatedRows == 1)
					finish();
				else
					Toast.makeText(mContext, R.string.duplicate_location, Toast.LENGTH_SHORT).show();
				result = true;
				break;
				
			case R.id.edit_cancel:
				finish();
				result = true;
				break;
		}		
		return result;
	} 
	
    /** sets the onClickListener for the + icon in the title bar if it
     *  is to be shown.
     *  
     *  @param enableDoneButton - true to enable the done button else false.
     */
    protected void setupActionBarItemsVisibility(boolean enableDoneButton) {    
    	int editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_DISABLED;
    	if(enableDoneButton)
    		editFragmentOption = EditFragment.EditFragmentOptions.SHOW_SAVE_ENABLED;
    	else if(isLaunchedForSuggestedLoc)
    		editFragmentOption = EditFragment.EditFragmentOptions.SHOW_CONFIRM;

        // Add menu items from fragment
    	Fragment fragment = EditFragment.newInstance(editFragmentOption, false);
 		getFragmentManager().beginTransaction()
 				.replace(R.id.edit_loc_fragment_container, fragment, null).commit();
    }
	
    /** sets the onClickListeners for change location
     *  and the name edit text box listeners.
     */
    private void setLocChangeButtonOnClickListeners() {
        editLocationName = (EditText) findViewById(R.id.edit_location_name);
        String name = storedKeyValues.name;
        // If the name is the same as the address then need to leave the name field as blank for the
        // user to enter a value.
        if(storedKeyValues.address != null && storedKeyValues.name != null
        		&& storedKeyValues.name.equals(storedKeyValues.address))
        	name = "";
        editLocationName.setText(name);
        editLocationName.addTextChangedListener(this);

        editLocationAddress = (TextView) findViewById(R.id.edit_location_address);
        if(storedKeyValues.address != null)
        	editLocationAddress.setText(storedKeyValues.address);

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
    	if(isTextEmpty(editLocationName.getText()))
    		setupActionBarItemsVisibility(false);
    	else
    		setupActionBarItemsVisibility(true);    	
    }

    /**
     * Returns true if the text of the given Editable is not empty
     * @param editable - editable text
     * @return - true if the text of the given Editable is not empty
     */
    public static boolean isTextEmpty(Editable editable) {
       return ((editable == null) || (StringUtils.isEmpty(editable.toString())));
    }

    /** fetches the name from the edit text box and if it is null or empty then
     *  returns the name as in the DB else the new name entered by the user.
     *
     * @return - name of the location.
     */
    private String getEditedLocationName() {
    	String editedName = editLocationName.getText().toString().trim();
    	if(editedName == null || editedName.length() == 0)
    		editedName = storedKeyValues.name;

    	return editedName;
    }

    /** Convenience method to set the result values and return to the calling app.
     */
    private void setConfigureResult(int resultCode) {

        Intent intent = new Intent();

        if(resultCode == RESULT_OK) {
            String poiTagName = storedKeyValues.poiTag;
            String correctedPoiTagName = Util.getReplacedString(storedKeyValues.poiTag);
            String poiName = storedKeyValues.name;
            String rule = MEANINGFUL_LOC_SENSOR_STRING + poiTagName + END_STRING;
            String extraText = SENSOR_NAME_START_STRING + VIRTUAL_SENSOR_STRING + correctedPoiTagName + SENSOR_NAME_END_STRING;
            String xmlString = VSENSOR_START_TAG + Util.generateLocDVSString(correctedPoiTagName, rule) + VSENSOR_END_TAG;
            String uriToFire =  URI_TO_FIRE_STRING + poiName + END_STRING;

            if(LOG_DEBUG) Log.d(TAG, "poiTagName = "+poiTagName+"; extraText = "+extraText+"; poiName = "+poiName);
            if(LOG_DEBUG) Log.d(TAG, "xmlString = "+xmlString);
            intent.putExtra(EVENT_NAME, poiName);
            intent.putExtra(EVENT_DESC, poiName);
            intent.putExtra(EDIT_URI, uriToFire);
            intent.putExtra(Intent.EXTRA_TEXT, extraText);
            intent.putExtra(VSENSOR, xmlString);
        }
        setResult(resultCode, intent);
    }

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
}
