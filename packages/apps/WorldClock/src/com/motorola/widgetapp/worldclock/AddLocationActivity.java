package com.motorola.widgetapp.worldclock;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView.OnEditorActionListener;

import com.motorola.widgetapp.worldclock.WorldClockDBAdapter;
import com.motorola.widgetapp.worldclock.WorldClockDBAdapter.WidgetDataHolder;

public class AddLocationActivity extends ListActivity {
    protected static final String TAG = AddLocationActivity.class
            .getSimpleName();

    AddLocationActivity.CityListAdapter mLocationAdapter;
    int mAppWidgetId;
    int mLastDisplayOrder;
    private int mCurrentCity;
    private WorldClockDBAdapter mDbHelper;

    private String[] mMatchCityNames;
    private String[] mMatchTimezoneId;
    private int[] mMatchTimezoneStringId;

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        return super.onCreateDialog(id, args);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // directly add location to database
        WidgetDataHolder widgetData = mDbHelper.queryWidgetData(mAppWidgetId);

        if (widgetData == null) {
            Log.e(TAG, "queryWidgetData failed!!!!!!!!!!!");
            finish();
            return;
        }

        widgetData.mDisplayName[mCurrentCity] = mMatchCityNames[position];
        widgetData.mTimeZoneId[mCurrentCity] = mMatchTimezoneId[position];
        widgetData.mTimeZoneStringId[mCurrentCity] = mMatchTimezoneStringId[position];
        widgetData.mAdjustDSTStatus[mCurrentCity] = WorldClockPreferenceActivity.isCityInDST(mMatchTimezoneId[position]);


        boolean result = mDbHelper.insertOrUpdateWidgetData(mAppWidgetId,
                widgetData);
        if (!result) {
            Log.e(TAG, "insertOrUpdateWidgetData faile!!!!!!!!!!!");
            finish();
            return;
        }

        Intent updateIntent = new Intent(WorldClockWidgetProvider.ACTION_WORLDCLOCK_UPDATE);
        updateIntent.putExtra(WorldClockWidgetProvider.APPWIDGETID, mAppWidgetId);
        this.sendBroadcast(updateIntent);

        setResult(RESULT_OK, null);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP |
                ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        mAppWidgetId = getIntent().getIntExtra(
                WorldClockWidgetProvider.APPWIDGETID, -1);
        mCurrentCity = getIntent().getIntExtra(
                WorldClockWidgetProvider.CURRENTCITY, -1);
        if (mAppWidgetId < 0 || mCurrentCity < 0 || mCurrentCity > 1) {
            finish();
            return;
        }
        mDbHelper = new WorldClockDBAdapter(this);
        mDbHelper.open();

        setContentView(R.layout.addlocation);
        mLocationAdapter = new CityListAdapter(this);
        setListAdapter(mLocationAdapter);

        final EditText term = (EditText) findViewById(R.id.addlocation_term);
        term.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String matchstr = s.toString();
                matchstr = matchstr.trim();
                searchMatchedCities(matchstr);
                mLocationAdapter.setCityCountryArray(mMatchCityNames, "/");
                setListAdapter(mLocationAdapter);
            }


            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
                // XXX do something
            }

            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                // XXX do something
            }
        });
        term.setOnEditorActionListener(new OnEditorActionListener(){
            public boolean onEditorAction(TextView v, int actionId,
                    KeyEvent event) {
                // TODO Auto-generated method stub
                if(actionId == EditorInfo.IME_ACTION_SEARCH){
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        if (mDbHelper != null)
            mDbHelper.close();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showResultsText(int number) {
        TextView tv = (TextView) findViewById(R.id.result_string_view);
        if (tv != null) {
            if (number == 0) {
                tv.setText(getString(R.string.No_matches));
                tv.setVisibility(View.VISIBLE);
            } else {
                tv.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onPause() {
        // If user leaves this activity, just finish the activity.
        super.onPause();
        finish();
    }

    private void searchMatchedCities(String input) {
        mMatchCityNames = null;
        mMatchTimezoneId = null;
        mMatchTimezoneStringId = null;
        if (input.length() == 0)
        {
        	TextView tv = (TextView) findViewById(R.id.result_string_view);
        	if(tv != null && tv.getVisibility() == View.VISIBLE)
        	{
        		tv.setVisibility(View.GONE);
        	}
            return;
        }
        Cursor c = mDbHelper.queryCities(input);
        if (c == null)
            return;

        int count = c.getCount();
        showResultsText(count);
        if (count == 0) {
            c.close();
            return;
        }
        int timeZoneIndex = c
                .getColumnIndex(WorldClockDBAdapter.KEY_TIMEZONE_ID);
        int displayNameIndex = c
                .getColumnIndex(WorldClockDBAdapter.KEY_DISPLAY_NAME);
        int timeZoneStringIdIndex = c
                .getColumnIndex(WorldClockDBAdapter.KEY_TIMEZONE_STRING_ID);
        mMatchCityNames = new String[count];
        mMatchTimezoneId = new String[count];
        mMatchTimezoneStringId = new int[count];
        for (int i = 0; i < count; i++) {
            mMatchCityNames[i] = c.getString(displayNameIndex);
            mMatchTimezoneId[i] = c.getString(timeZoneIndex);
            mMatchTimezoneStringId[i] = c.getInt(timeZoneStringIdIndex);
            c.moveToNext();
        }
        c.close();
    }

    /**
     * A ListAdapter that presents content of City and Country
     *
     */
    private class CityListAdapter extends BaseAdapter {
        public CityListAdapter(Context context) {
            mContext = context;
        }

        public void setCityCountryArray(String[] cities, String[] countries) {
            mCities = cities;
            mCountries = countries;
        }

        public void setCityCountryArray(String[] citywithcountries, String expr) {
            if (citywithcountries == null || citywithcountries.length == 0) {
                mCities = null;
                mCountries = null;
            } else {
                mCities = new String[citywithcountries.length];
                mCountries = new String[citywithcountries.length];
                for (int i = 0; i < citywithcountries.length; i++) {
                    String[] CityandCountry = citywithcountries[i].split(expr);
                    mCities[i] = CityandCountry[0];
                    if (CityandCountry.length == 2) {
                        mCountries[i] = CityandCountry[1];
                    } else {
                        mCountries[i] = "";
                    }
                }
            }

        }

        public int getCount() {
            if (mCities == null)
                return 0;
            return mCities.length;
        }

        public Object getItem(int position) {
            return position;
        }

        /**
         * Use the array index as a unique id.
         */
        public long getItemId(int position) {
            return position;
        }

        /**
         * Make a CityCountryView to hold each row.
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            CityCountryView sv=null;
            if (convertView == null) {
                sv = new CityCountryView(mContext, mCities[position],
                        mCountries[position]);
            } else {
                if(convertView instanceof CityCountryView )
                    sv = (CityCountryView) convertView;
                sv.setCity(mCities[position]);
                sv.setCountry(mCountries[position]);
            }

            return sv;
        }

        /**
         * Remember our context so we can use it when constructing views.
         */
        private Context mContext;

        /**
         * Our data, part 1.
         */
        private String[] mCities;

        /**
         * Our data, part 2.
         */
        private String[] mCountries;
    }

    /**
     * We will use a CityCountryView to display each City Country pair. It's
     * just a LinearLayout with two text fields.
     *
     */
    private class CityCountryView extends LinearLayout {
        public CityCountryView(Context context, String City, String Country) {
            super(context);

            this.setOrientation(VERTICAL);

            LinearLayout myRoot = new LinearLayout(context);
            mItemView = inflate(context,android.R.layout.simple_list_item_2, myRoot);

            mCity = (TextView)mItemView.findViewById(android.R.id.text1);
            mCity.setText(City);

            mCountry = (TextView)mItemView.findViewById(android.R.id.text2);
            mCountry.setText(Country);

            addView(mItemView);
        }

        public void setCity(String city) {
            mCity.setText(city);
        }

        public void setCountry(String country) {
            mCountry.setText(country);
        }
        private View mItemView;
        private TextView mCity;
        private TextView mCountry;
    }
}
