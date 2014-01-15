/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.widgetapp.worldclock;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.motorola.widgetapp.worldclock.WorldClockDBAdapter.WidgetDataHolder;

public class WorldClockPreferenceActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener, LocationListener {

    // -- variables defines -----------------------------------------------
    private static final String TAG = "WorldClockPreferenceActivity";
    public static final int MAX_CITY_NUM = 2;
    public static final String EMPTY_STRING = "";
    private static final String KEY_CITIES_STATUS = "KEY_CITIES_STATUS";
    private static final String KEY_TIMEZONE_ID = "KEY_TIMEZONE_ID";
    private static final String KEY_TIMEZONE_STRING_ID = "KEY_TIMEZONE_STRING_ID";
    private static final String KEY_CITIES_NAME = "KEY_CITIES_NAME";
    private static final String KEY_WIDGET_APPID = "KEY_WIDGET_ID";
    private static final String KEY_CLOCK_STYLE = "KEY_CLOCK_STYLE";
    private static final String KEY_USE_CURRENT_LOCATION = "KEY_USE_CURRENT";
    private static final String KEY_CITIES_HIDDEN="KEY_CITIES_HIDDEN";
    //begin IKCNDEVICS-373_zch45_cvmq63,2/2/2012
    private static final String KEY_CITY_B_CATEGORY="city_b_category";
    //end IKCNDEVICS-373_zch45_cvmq63,2/2/2012

    private static final String ITEM_SELECT_CITY_A = "select_city_a";
    private static final String ITEM_SELECT_CITY_B = "select_city_b";
    private static final String ITEM_DST_CHECKBOX_A = "dst_checkbox_a";
    private static final String ITEM_DST_CHECKBOX_B = "dst_checkbox_b";
    private static final String ITEM_USE_CURRENT_LOCATION = "use_current_location";
    private static final String ITEM_CLOCK_STYLE = "clock_style";
    private static final String ITEM_CITY_A_SWITCHER="city_a_switcher";
    private static final String ITEM_CITY_B_SWITCHER="city_b_switcher";

    private Preference mSelectCityAPref;
    private Preference mSelectCityBPref;
    private CheckBoxPreference mDSTCheckboxAPref;
    private CheckBoxPreference mDSTCheckboxBPref;
    private CheckBoxPreference mUseCurrentLocationPref;
    private ListPreference mClockStylePref;
    private SwitchPreference mCityASwitcherPref;
    private SwitchPreference mCityBSwitcherPref;

    private static final int FIND_CITY_CODE = 0;
    private static final int FIND_LOCATION_CODE = 1;

    private ReverseGeocoderTask mReverseGeocoderTask;
    private WorldClockDBAdapter mDbHelper;

    private boolean[] mAdjustDSTStatus;
    private String[] mRawOffsetStr;
    private int[] mRawOffset;
    private String[] mDisplayName;
    private String[] mTimeZoneId;
    private int[] mTimeZoneStringId;
    private int mCurrentDisplayCity;
    private String mCurrentLocale;
    private int mAppWidgetId;
    private boolean mNewCreating = true;
    private boolean[] misCityHidden;

    private int mCurrentSelectedStylePosition;
    private Boolean mClockStyle = true; // true: analog; false: digital
    private String[] mClockStyles;
    private android.location.Location mLocation;
    private String mCurrentLocationCityName;
    private LocationManager mLocationManager;
    private String bestProvider;
    private Boolean mUseCurrentLocation = false;

    enum CurrentLocationStatus {
        location_noservice, location_searching, location_found, location_canceled, location_timeout
    }

    private CurrentLocationStatus mLocationStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Add button to preference screen
        addPreferencesFromResource(R.xml.worldclock_preference);
        setContentView(R.layout.worldclock_settings);

        setResult(Activity.RESULT_CANCELED);

        mAdjustDSTStatus = new boolean[MAX_CITY_NUM];
        mDisplayName = new String[MAX_CITY_NUM];
        mTimeZoneId = new String[MAX_CITY_NUM];
        mTimeZoneStringId = new int[MAX_CITY_NUM];
        mRawOffsetStr = new String[MAX_CITY_NUM];
        mRawOffset = new int[MAX_CITY_NUM];
        mCurrentDisplayCity = 0;
        misCityHidden=new boolean[MAX_CITY_NUM];

        mDbHelper = new WorldClockDBAdapter(this);
        mDbHelper.open();

        mClockStyles = getResources().getStringArray(R.array.clock_style_entries);

        initConfigrationData(savedInstanceState);
        initUiHandler();
        refreshUI();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.layout.configure_option, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.date_time_settings:
            Intent intent = new Intent(Settings.ACTION_DATE_SETTINGS);
            startActivity(intent);
            return true;
        }
        return false; // should never happen
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        outState.putInt(KEY_WIDGET_APPID, mAppWidgetId);
        outState.putStringArray(KEY_CITIES_NAME, mDisplayName);
        outState.putBooleanArray(KEY_CITIES_STATUS, mAdjustDSTStatus);
        outState.putStringArray(KEY_TIMEZONE_ID, mTimeZoneId);
        outState.putIntArray(KEY_TIMEZONE_STRING_ID, mTimeZoneStringId);
        outState.putBoolean(KEY_CLOCK_STYLE, mClockStyle);
        outState.putBoolean(KEY_USE_CURRENT_LOCATION, mUseCurrentLocation);
        outState.putBooleanArray(KEY_CITIES_HIDDEN, misCityHidden);
        super.onSaveInstanceState(outState);
    }

    private void initConfigrationData(Bundle savedInstanceState) {
        initLocaleFromDb();
        if (!mCurrentLocale.equalsIgnoreCase(Locale.getDefault().toString())) {
            mCurrentLocale = Locale.getDefault().toString();
            mDbHelper.updateCurrentlocale(1, mCurrentLocale);
            //Update city name of timezone table in database during locale change
            mDbHelper.updateCityName();
        }
        if (savedInstanceState != null) {
            mAppWidgetId = savedInstanceState.getInt(KEY_WIDGET_APPID);
            mAdjustDSTStatus = savedInstanceState
                    .getBooleanArray(KEY_CITIES_STATUS);
            mDisplayName = savedInstanceState.getStringArray(KEY_CITIES_NAME);
            mTimeZoneId = savedInstanceState.getStringArray(KEY_TIMEZONE_ID);
            mTimeZoneStringId = savedInstanceState.getIntArray(KEY_TIMEZONE_STRING_ID);
            mClockStyle = savedInstanceState.getBoolean(KEY_CLOCK_STYLE);
            mUseCurrentLocation = savedInstanceState.getBoolean(KEY_USE_CURRENT_LOCATION);
            misCityHidden=savedInstanceState.getBooleanArray(KEY_CITIES_HIDDEN);

            if (mDisplayName == null || mTimeZoneId == null) {
                mDbHelper.deleteWidgetData(mAppWidgetId);
                loadDefaultSettings();
            }
        } else {
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) {
                mNewCreating = true;
                mAppWidgetId = extras.getInt(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        AppWidgetManager.INVALID_APPWIDGET_ID);
            } else {
                mNewCreating = false;
                Uri data = intent.getData();
                if (data != null) {
                    mAppWidgetId = Integer.valueOf(data.toString());
                } else {
                    finish();
                    return;
                }
            }

            WidgetDataHolder mydata = mDbHelper.queryWidgetData(mAppWidgetId);
            if (mydata != null) {
                mAdjustDSTStatus = mydata.mAdjustDSTStatus;
                mDisplayName = mydata.mDisplayName;
                mTimeZoneId = mydata.mTimeZoneId;
                mTimeZoneStringId = mydata.mTimeZoneStringId;
                mClockStyle = mydata.mClockStyle;
                mUseCurrentLocation = mydata.mUseCurrentLocation;
                misCityHidden=mydata.misCityHidden;
            } else {
                mDbHelper.deleteWidgetData(mAppWidgetId);
                loadDefaultSettings();
            }
        }

        if (mClockStyle) {
            mCurrentSelectedStylePosition = 0;
        } else {
            mCurrentSelectedStylePosition = 1;
        }

        Log.d(TAG, "mAppWidgetId = " + mAppWidgetId);

        for (int i = 0; i < MAX_CITY_NUM; i++) {
            int rawOffset = TimeZone.getTimeZone(mTimeZoneId[i]).getRawOffset();
            mRawOffset[i] = rawOffset;
            StringBuilder rawOffsetStr = new StringBuilder();
            rawOffsetStr.append(toOffsetString(rawOffset));
            mRawOffsetStr[i] = rawOffsetStr.toString();
        }
    }

    private void initLocaleFromDb() {
        Log.d(TAG, "initLocaleFromDb()");
        Cursor currentlocaleCursor = mDbHelper.queryCurrentlocale();
        if (currentlocaleCursor != null) {
            mCurrentLocale = currentlocaleCursor
                    .getString(WorldClockDBAdapter.CURRENTLOCALE_INDEX_LOCALE);
            currentlocaleCursor.close();
        }
    }

    private void loadDefaultSettings() {
        int displayCityNumber = 1;
        Log.d(TAG, "loadDefaultSettings()");
        // BEGIN MOTOROLA - IKSTABLE6-8711: Fix FindBug issues in WorldClock app
        if (mTimeZoneId != null && mTimeZoneStringId != null && mDisplayName != null && mAdjustDSTStatus != null&&misCityHidden!=null) {
        // END MOTOROLA - IKSTABLE6-8711
            if (getString(R.string.default_worldclock_config).equalsIgnoreCase(
                    "SYNC_WITH_")) {
                // syn with os
                mTimeZoneId[0] = TimeZone.getDefault().getID();
                mTimeZoneStringId[0] = 0;
                mDisplayName[0] = getCityNameFromTimeZoneId(mTimeZoneId[0], this);
            } else {
                // load from xml
                displayCityNumber = getResources().getInteger(
                        R.integer.default_worldclock_city_number);
                if (displayCityNumber > MAX_CITY_NUM)
                    displayCityNumber = MAX_CITY_NUM;
                for (int i = 0; i < MAX_CITY_NUM; i++) {
                    String resName = "default_worldclock_timezone_id" + i;
                    int resId = getResources().getIdentifier(resName, "string",
                            getPackageName());
                    if (resId != 0)
                        mTimeZoneId[i] = getString(resId);
                    mTimeZoneStringId[i] = 0;
                    mAdjustDSTStatus[i] = false; // not check
                    mDisplayName[i] = "";
                    misCityHidden[i]=false;
                }
            }
            // set the first city as current default display
            // mCurrentDisplay.setDefault(0);
            mAdjustDSTStatus[0] = false;
            mCurrentDisplayCity = 0;
            mClockStyle = true;
            mUseCurrentLocation = false;
            misCityHidden[0]=false;
            // saveDefaultSettings();
        }
    }

    private void saveDefaultSettings() {
        WidgetDataHolder mydata = new WidgetDataHolder();
        mydata.mAdjustDSTStatus = mAdjustDSTStatus;
        mydata.mDisplayName = mDisplayName;
        mydata.mTimeZoneId = mTimeZoneId;
        mydata.mTimeZoneStringId = mTimeZoneStringId;
        mydata.mCurrentDisplayCity = mCurrentDisplayCity;
        mydata.mCurrentLocale = mCurrentLocale;
        mydata.mClockStyle = mClockStyle;
        mydata.mUseCurrentLocation = mUseCurrentLocation;
        mydata.misCityHidden=misCityHidden;
        mDbHelper.insertWidgetData(mAppWidgetId, mydata);
    }

    public static String getCityNameFromTimeZoneId(String timezoneId,
            Context context) {

        final String XMLTAG_TIMEZONE = "timezone";

        try {
            XmlResourceParser xrp = context.getResources().getXml(
                    R.xml.widget_worldclock_timezones);
            while (xrp.next() != XmlResourceParser.START_TAG)
                ;
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                while (xrp.getEventType() != XmlResourceParser.START_TAG) {
                    if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
                        return fromatIdtoCityName(timezoneId);
                    }
                    xrp.next();
                }
                if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
                    String id = xrp.getAttributeValue(0);
                    if (id.equalsIgnoreCase(timezoneId)) {

                        String displayNameResourceString = xrp.nextText(); // Display
                        // City
                        // Sting
                        // Id
                        /* Get the display name accoring to the string ID */
                        int stringId = context.getResources().getIdentifier(
                                displayNameResourceString, "string",
                                context.getPackageName());
                        if (stringId != 0) {
                            String cityName = context.getString(stringId);
                            return cityName;
                        } else {
                            return fromatIdtoCityName(timezoneId);
                        }

                    }
                }
                while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                    xrp.next();
                }
                xrp.next();
            }
            xrp.close();
        } catch (XmlPullParserException xppe) {
            Log.e(TAG, "Ill-formatted timezones.xml file");

        } catch (java.io.IOException ioe) {
            Log.e(TAG, "Unable to read timezones.xml file");

        }
        return fromatIdtoCityName(timezoneId);

    }

    private static String fromatIdtoCityName(String timezoneId) {
        int num = timezoneId.lastIndexOf("/");
        if (num > 0)
            timezoneId = timezoneId.substring(num + 1);
        return timezoneId;
    }

    private boolean RefreshDatabase(){
        WidgetDataHolder widgetData = new WidgetDataHolder();
        widgetData.mAdjustDSTStatus = mAdjustDSTStatus;
        widgetData.mDisplayName = mDisplayName;
        widgetData.mTimeZoneId = mTimeZoneId;
        widgetData.mTimeZoneStringId = mTimeZoneStringId;
        widgetData.mCurrentDisplayCity = mCurrentDisplayCity;
        widgetData.mCurrentLocale = mCurrentLocale;
        widgetData.mClockStyle = mClockStyle;
        widgetData.mUseCurrentLocation = mUseCurrentLocation;
        widgetData.misCityHidden=misCityHidden;
        Intent updateIntent = new Intent(WorldClockWidgetProvider.ACTION_WORLDCLOCK_UPDATE);
        updateIntent.putExtra(WorldClockWidgetProvider.APPWIDGETID, mAppWidgetId);
        this.sendBroadcast(updateIntent);

        return mDbHelper.insertOrUpdateWidgetData(mAppWidgetId,widgetData);
    }

    private void setUiconfigfromIntent() {
        if(mUseCurrentLocation) {
            refreshUseCurrentLocation();
        }
        boolean result = RefreshDatabase();
        if (!result) {
            finish();
            return;
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private String getTimeZoneIdFromCityName(String cityname) {
        String matchTimeZoneID = null;
        String citynamewithoutcountry = cityname.split("/")[0];
        Log.e(TAG, "citynamewithoutcountry = " + citynamewithoutcountry);
        if (cityname.length() == 0)
            return null;
        Cursor c = mDbHelper.queryCities(citynamewithoutcountry);
        if (c == null)
            return null;

        int count = c.getCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                String queriedcityname = c.getString(c
                        .getColumnIndex(WorldClockDBAdapter.KEY_DISPLAY_NAME));
                Log.e(TAG, "queriedcityname = " + queriedcityname);
                if (cityname.equalsIgnoreCase(queriedcityname)) {
                    int timeZoneIndex = c
                            .getColumnIndex(WorldClockDBAdapter.KEY_TIMEZONE_ID);
                    matchTimeZoneID = c.getString(timeZoneIndex);
                    break;
                }
                c.moveToNext();
            }

        }
        c.close();
        return matchTimeZoneID;
    }

    private int getTimeZoneStringIdFromCityName(String cityname) {
        int matchTimeZoneStringID = 0;
        String citynamewithoutcountry = cityname.split("/")[0];
        Log.e(TAG, "citynamewithoutcountry = " + citynamewithoutcountry);
        if (cityname.length() == 0)
            return 0;
        Cursor c = mDbHelper.queryCities(citynamewithoutcountry);
        if (c == null)
            return 0;

        int count = c.getCount();
        if (count > 0) {
            for (int i = 0; i < count; i++) {
                String queriedcityname = c.getString(c
                        .getColumnIndex(WorldClockDBAdapter.KEY_DISPLAY_NAME));
                Log.e(TAG, "queriedcityname = " + queriedcityname);
                if (cityname.equalsIgnoreCase(queriedcityname)) {
                    int timeZoneStringIdIndex = c
                            .getColumnIndex(WorldClockDBAdapter.KEY_TIMEZONE_STRING_ID);
                    matchTimeZoneStringID = c.getInt(timeZoneStringIdIndex);
                    break;
                }
                c.moveToNext();
            }

        }
        c.close();
        return matchTimeZoneStringID;
    }

    private void initUiHandler() {
        mSelectCityAPref = findPreference(ITEM_SELECT_CITY_A);
        mSelectCityBPref = findPreference(ITEM_SELECT_CITY_B);
        mDSTCheckboxAPref = (CheckBoxPreference) findPreference(ITEM_DST_CHECKBOX_A);
        mDSTCheckboxBPref = (CheckBoxPreference) findPreference(ITEM_DST_CHECKBOX_B);
        mUseCurrentLocationPref = (CheckBoxPreference) findPreference(ITEM_USE_CURRENT_LOCATION);
        mClockStylePref = (ListPreference) findPreference(ITEM_CLOCK_STYLE);
        mCityASwitcherPref=(SwitchPreference)findPreference(ITEM_CITY_A_SWITCHER);
        mCityBSwitcherPref=(SwitchPreference)findPreference(ITEM_CITY_B_SWITCHER);
        if(null!=mCityASwitcherPref){
            mCityASwitcherPref.setOnPreferenceChangeListener(this);
        }
        if(null!=mCityBSwitcherPref){
            mCityBSwitcherPref.setOnPreferenceChangeListener(this);
        }
        //begin IKCNDEVICS-373_zch45_cvmq63,2/2/2012
        if(null!=mUseCurrentLocationPref){
            PreferenceScreen currpreScreen=getPreferenceScreen();
            if(null!=currpreScreen){
                currpreScreen.removePreference(mUseCurrentLocationPref);
            }
        }
    //IKCNDEVICS-373_fix_scanning_city
        /*if (mUseCurrentLocationPref != null && mSelectCityBPref != null && mDSTCheckboxBPref != null) {
            mUseCurrentLocationPref.setChecked(mUseCurrentLocation);
            mSelectCityBPref.setEnabled(!mUseCurrentLocation);
            mDSTCheckboxBPref.setEnabled(!mUseCurrentLocation);
            checkLocationProvider();
        }*/
        //end IKCNDEVICS-373_zch45_cvmq63,2/2/2012

        if (mClockStylePref != null) {
            mClockStylePref.setValueIndex(mCurrentSelectedStylePosition);
            mClockStylePref.setSummary(mClockStyles[mCurrentSelectedStylePosition]);
            mClockStylePref.setOnPreferenceChangeListener(this);
        }

        // Listen for add-city clicks
        Button doneButton = (Button) findViewById(R.id.settings_done);
        doneButton.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("synthetic-access")
            public void onClick(View v) {
                // Make sure we pass back the original appWidgetId
                setUiconfigfromIntent();
            }
        });
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDSTCheckboxAPref) {
            boolean DSTEnabled = mDSTCheckboxAPref.isChecked();
            mAdjustDSTStatus[0] = DSTEnabled;
            RefreshDatabase();
        }
        else if (preference == mDSTCheckboxBPref) {
            boolean DSTEnabled = mDSTCheckboxBPref.isChecked();
            mAdjustDSTStatus[1] = DSTEnabled;
            RefreshDatabase();
        }
        //begin IKCNDEVICS-373_zch45_cvmq63,2/2/2012
        /*else if (preference == mUseCurrentLocationPref) {
                final boolean isChecked = mUseCurrentLocationPref.isChecked();
                if (isChecked) {
                    if (mCurrentLocationCityName != null &&
                            mLocationStatus == CurrentLocationStatus.location_found) {
                        refreshUseCurrentLocation();
                    }
                }
                mUseCurrentLocation = isChecked;
                RefreshDatabase();
                refreshUI();
                mSelectCityBPref.setEnabled(!isChecked);
                mDSTCheckboxBPref.setEnabled(!isChecked);
        }*/
        //end IKCNDEVICS-373_zch45_cvmq63,2/2/2012
        else if (preference == mSelectCityAPref) {
            Intent intent = new Intent(WorldClockPreferenceActivity.this,
                    AddLocationActivity.class);
            intent.putExtra(WorldClockWidgetProvider.APPWIDGETID,
                    mAppWidgetId);
            intent.putExtra(WorldClockWidgetProvider.CURRENTCITY,
                    0);
            startActivityForResult(intent, FIND_CITY_CODE);
        }
        else if (preference == mSelectCityBPref) {
            Intent intent = new Intent(WorldClockPreferenceActivity.this,
                    AddLocationActivity.class);
            intent.putExtra(WorldClockWidgetProvider.APPWIDGETID,
                    mAppWidgetId);
            intent.putExtra(WorldClockWidgetProvider.CURRENTCITY,
                    1);
            startActivityForResult(intent, FIND_CITY_CODE);
        }else if(preference==mCityASwitcherPref){
        	boolean ischecked=((SwitchPreference)preference).isChecked();
        	misCityHidden[0]=ischecked;
            RefreshDatabase();
        } else if(preference==mCityBSwitcherPref){
        	boolean ischecked=((SwitchPreference)preference).isChecked();
        	misCityHidden[1]=ischecked;
            RefreshDatabase();
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (ITEM_CLOCK_STYLE.equals(key)) {
            mCurrentSelectedStylePosition = Integer.parseInt((String) objValue);

            if (mCurrentSelectedStylePosition == 0) {
                mClockStyle = true;
            } else {
                mClockStyle = false;
            }

            mClockStylePref.setSummary(mClockStyles[mCurrentSelectedStylePosition]);
            RefreshDatabase();
        } else if (ITEM_CITY_A_SWITCHER.equals(key)){
        	String valueStr=objValue.toString();
        	if (!TextUtils.isEmpty(valueStr)&&valueStr.equalsIgnoreCase("true")){
                misCityHidden[0]=false;
        	} else {
        		misCityHidden[0]=true;
        	}
        	RefreshDatabase();
        } else if (ITEM_CITY_B_SWITCHER.equals(key)){
        	String valueStr=objValue.toString();
        	if (!TextUtils.isEmpty(valueStr)&&valueStr.equalsIgnoreCase("true")){
                misCityHidden[1]=false;
        	} else {
        		misCityHidden[1]=true;
        	}
        	RefreshDatabase();
        }

        return true;
    }

    private void refreshUseCurrentLocation(){
        boolean cityindatabase = false;
        if (mCurrentLocationCityName != null) {
             mDisplayName[1] = mCurrentLocationCityName;
             String timezoneID = getTimeZoneIdFromCityName(mCurrentLocationCityName);
             if (timezoneID != null) {
                 mTimeZoneId[1] = timezoneID;
                 cityindatabase = true;
                 mTimeZoneStringId[1] = getTimeZoneStringIdFromCityName(mCurrentLocationCityName);
             } else {
                 mTimeZoneId[1] = getTimeZoneText();
                 mTimeZoneStringId[1] = 0;
             }
         } else {
             mCurrentLocationCityName = getTimeZoneText();
             mDisplayName[1] = mCurrentLocationCityName;
             mLocationStatus = CurrentLocationStatus.location_canceled;
             mTimeZoneId[1] = getTimeZoneText();
             mTimeZoneStringId[1] = 0;
        }
        if(cityindatabase){
            mAdjustDSTStatus[1] = isCityInDST(mTimeZoneId[1]);
        } else {
            mAdjustDSTStatus[1] = isCityInDST(null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FIND_CITY_CODE) {
            if (resultCode == RESULT_OK) {
                refreshCityNames();
                refreshUI();
            }
        } else if (requestCode == FIND_LOCATION_CODE) {

        }
    }

    private void refreshCityNames() {
        WidgetDataHolder mydata = mDbHelper.queryWidgetData(mAppWidgetId);
        if (mydata != null) {
            mDisplayName = mydata.mDisplayName;
            mTimeZoneId = mydata.mTimeZoneId;
            mTimeZoneStringId = mydata.mTimeZoneStringId;
            mAdjustDSTStatus = mydata.mAdjustDSTStatus;
            misCityHidden=mydata.misCityHidden;
        }
    }

    private void refreshUI() {
        WidgetDataHolder mydata = mDbHelper.queryWidgetData(mAppWidgetId);
        if (mydata != null) {
            mDisplayName = mydata.mDisplayName;
            mTimeZoneId = mydata.mTimeZoneId;
            mTimeZoneStringId = mydata.mTimeZoneStringId;
            misCityHidden=mydata.misCityHidden;
        }

        String findcity = getString(R.string.find_city);
        if (mSelectCityAPref != null && mSelectCityBPref != null && mDSTCheckboxAPref != null && mDSTCheckboxBPref != null
            && mDisplayName != null && mAdjustDSTStatus != null&&misCityHidden!=null&&mCityASwitcherPref!=null&&mCityBSwitcherPref!=null) {
            if (mDisplayName[0].length() != 0) {
                mSelectCityAPref.setSummary(formatDisplayCityName(mDisplayName[0]));
            } else {
                mSelectCityAPref.setSummary(findcity);
            }

            if (mDisplayName[1].length() != 0) {
                mSelectCityBPref.setSummary(formatDisplayCityName(mDisplayName[1]));
            } else {
                mSelectCityBPref.setSummary(findcity);
            }

            mDSTCheckboxAPref.setChecked(mAdjustDSTStatus[0]);
            mDSTCheckboxBPref.setChecked(mAdjustDSTStatus[1]);
            mCityASwitcherPref.setChecked(!misCityHidden[0]);
            mCityBSwitcherPref.setChecked(!misCityHidden[1]);

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (!mNewCreating && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            setUiconfigfromIntent();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    // -- Life cycle Facilities -----------------------------------------------
    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        mDbHelper.close();
        if(mLocationManager != null){
            mLocationManager.removeUpdates(this);
        }
        super.onDestroy();

        if (mReverseGeocoderTask != null &&
                mReverseGeocoderTask.getStatus() != ReverseGeocoderTask.Status.FINISHED) {
            mReverseGeocoderTask.cancel(true);
            mReverseGeocoderTask = null;
        }
    }

    public static boolean anyLocationProviderEnable(Context context) {
        boolean avail = false;
        Log.d(TAG, "*** anyLocationProviderEnable");
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Log.w(TAG, "Error. Location manager not found");
            return avail;
        }
        List<String> providers = locationManager.getProviders(true);
        if ((providers != null)) {
            String provider;
            for (int i = 0; i < providers.size(); i++) {
                provider = providers.get(i);
                if (provider != null) {
                    if ((TextUtils.equals(provider,
                            LocationManager.GPS_PROVIDER))
                            || (TextUtils.equals(provider,
                                    LocationManager.NETWORK_PROVIDER))) {
                        avail = true;
                        break;
                    }
                }
            }
        }
        return avail;
    }

    private void checkLocationProvider() {
        if (!anyLocationProviderEnable(this)) {
            Log.d(TAG, "None of location provider available");
            mLocationStatus = CurrentLocationStatus.location_noservice;
        } else {
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            bestProvider = mLocationManager.getBestProvider(criteria, true);
            mLocation = mLocationManager.getLastKnownLocation(bestProvider);
            mCurrentLocationCityName = null;
            if (mLocation != null) {
                Log.d(TAG, "mLocation is not null, set location as found");
                mLocationStatus = CurrentLocationStatus.location_found;
            } else {
                Log.d(TAG, "mLocation is null, set location as searching");
                mLocationStatus = CurrentLocationStatus.location_searching;
                mLocationManager.requestLocationUpdates(bestProvider, 0, 0,
                        this);
            }
        }
        refreshCurrentLocationUI();
    }

    private String formatDisplayCityName(String dbFormatCityName) {
        String[] cityandcountry;
        StringBuffer displayFormatCityName = new StringBuffer();
        if (dbFormatCityName != null) {
            cityandcountry = dbFormatCityName.split("/");
            int subparts = cityandcountry.length;
            if (subparts >= 1) {
                displayFormatCityName.append(cityandcountry[0]);
                for (int i = 1; i < subparts; i++) {
                    displayFormatCityName.append(", " + cityandcountry[i]);
                }
            }
        }
        return displayFormatCityName.toString();
    }

    /**
     * Reverse geocoding may take a long time to return, so put in in Async task to handle
     */
    private class ReverseGeocoderTask extends AsyncTask<Void, Void, String> {

        private Geocoder mGeocoder;
        private double mLat;
        private double mLng;

        public ReverseGeocoderTask(Geocoder geocoder, double[] latlng) {
            mGeocoder = geocoder;
            mLat = latlng[0];
            mLng = latlng[1];
        }

        @Override
        protected String doInBackground(Void... params) {
            String value = WorldClockPreferenceActivity.EMPTY_STRING;
            List<Address> lstAddr = null;
            try {
                Log.d("ReverseGeocoderTask", "try getFromLocation by Geocoder");
                lstAddr = mGeocoder.getFromLocation(mLat, mLng, 1);

                if (lstAddr != null && lstAddr.size() > 0) {
                    Address addr = lstAddr.get(0);
                    value = addr.getLocality() + "/" + addr.getCountryName();
                }
            } catch (IOException ex) {
                value = WorldClockPreferenceActivity.EMPTY_STRING;
                Log.e("ReverseGeocoderTask", "Geocoder exception: " + ex.getMessage());
            } catch (RuntimeException ex) {
                value = WorldClockPreferenceActivity.EMPTY_STRING;
                Log.e("ReverseGeocoderTask", "Geocoder exception: " + ex.getMessage());
            }
            Log.d("ReverseGeocoderTask", "return value = " + value);
            return value;
        }

        @Override
        protected void onPostExecute(String location) {
            if (!location.equals(WorldClockPreferenceActivity.EMPTY_STRING)) {
                Log.d("ReverseGeocoderTask", "onPostExecute, update location pref summary");
                mCurrentLocationCityName = location;
                mUseCurrentLocationPref.setSummary(formatDisplayCityName(mCurrentLocationCityName));
            } else {
                Log.d("ReverseGeocoderTask", "onPostExecute, location is empty");
            }
        }
    }

    private void refreshCurrentLocationUI() {
        //begin IKCNDEVICS-373_zch45_cvmq63,2/2/2012,dump this method
        /*  switch (mLocationStatus) {
        case location_noservice:
            Log.d(TAG, "location_noservice");
            mUseCurrentLocationPref.setSummary(getString(R.string.No_location_provider));
            break;
        case location_searching:
            Log.d(TAG, "location_searching");
            mUseCurrentLocationPref.setSummary(getString(R.string.Scanning_location));
            break;
        case location_canceled:
        case location_timeout:
            Log.d(TAG, "location_canceled&location_timeout");
            mUseCurrentLocationPref.setSummary(getTimeZoneText());
            break;
        case location_found:
            Log.d(TAG, "location_found");
            if (mLocation == null) {
                Log.e(TAG, "No valid location found");
            } else {
                double[] latlng = new double[2];
                Geocoder gc = new Geocoder(getBaseContext(), Locale.getDefault());
                latlng[0] = mLocation.getLatitude();
                latlng[1] = mLocation.getLongitude();
                mReverseGeocoderTask = new ReverseGeocoderTask(gc, latlng);
                mReverseGeocoderTask.execute();
            }
            break;
        }*/
        //end IKCNDEVICS-373_zch45_cvmq63,2/2/2012
        Log.d(TAG, "exit refreshCurrentLocationUI");
    }


    public void onLocationChanged(Location location) {
        //begin IKCNDEVICS-373_zch45_cvmq63,2/2/2012,dump this method
        // TODO Auto-generated method stub
        /*mLocation = location;
        mLocationManager.removeUpdates(this);
        mLocationStatus = CurrentLocationStatus.location_found;
        refreshCurrentLocationUI();*/
        //end IKCNDEVICS-373_zch45_cvmq63,2/2/2012
    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub

    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub

    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    // -- Date,Time,TimeZone Format Facilities
    // -----------------------------------------------
    public static String getTimeZoneText() {
        TimeZone tz = java.util.Calendar.getInstance().getTimeZone();
        boolean daylight = tz.inDaylightTime(new Date());
        StringBuilder sb = new StringBuilder();

        sb.append(toOffsetString(tz.getRawOffset()
                + (daylight ? tz.getDSTSavings() : 0)));

        return sb.toString();
    }

    private static char[] toOffsetString(int offset) {
        offset = offset / 1000 / 60;

        char[] buf = new char[9];
        buf[0] = 'G';
        buf[1] = 'M';
        buf[2] = 'T';

        if (offset < 0) {
            buf[3] = '-';
            offset = -offset;
        } else {
            buf[3] = '+';
        }

        int hours = offset / 60;
        int minutes = offset % 60;

        buf[4] = (char) ('0' + hours / 10);
        buf[5] = (char) ('0' + hours % 10);

        buf[6] = ':';

        buf[7] = (char) ('0' + minutes / 10);
        buf[8] = (char) ('0' + minutes % 10);

        return buf;
    }

    public static boolean isCityInDST(String timezoneID){
        TimeZone timeZone = null;
        if(timezoneID == null){
            timeZone = java.util.Calendar.getInstance().getTimeZone();
        } else {
            timeZone = TimeZone.getTimeZone(timezoneID);
        }
        boolean daylight = timeZone.inDaylightTime(new Date());
        return daylight;
    }
}
