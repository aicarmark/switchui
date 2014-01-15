package com.motorola.mmsp.weather.sinaweather.app;

import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.motorola.mmsp.sinaweather.app.WeatherScreenConfig;
import com.motorola.mmsp.weather.R;

public class SearchCityActivity extends Activity implements OnItemClickListener{
	private ProgressDialog mProgressDialog = null;
	private EditText mSearchCityInput = null;
	private ImageButton mSearchCityBtn;
	private ArrayList<Map> mItems = null;
	private boolean mIsStop = false; 
	private Toast mToast;
	private Handler myHandler = new Handler();
	private Runnable timeOutRun = new Runnable() {
		public void run() {
			mIsStop = true;
			mItems = null;
			onSearchComplete(null);
		}
	};
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		//@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constant.ACTION_SEARCH_CITY_RESPONSE)) {
				mItems = (ArrayList<Map>)intent.getSerializableExtra(Constant.SEARCH_CITY_RESPONSE_DATA);
				myHandler.removeCallbacks(timeOutRun);
				ArrayList<String> cities = new ArrayList<String>();
				
				 if (mItems != null) {
				    	for (Map map : mItems) {
							String adminArea = (String)map.get("adminArea");
							String country = (String)map.get("country");
							String city = (String)map.get("city");
							if (adminArea != null && !adminArea.equals("")) {
								city += ", " + adminArea;
							} 
							if (country != null && !country.equals("")) {
								city += ", " + country;
							} 
							cities.add(city);
						}
				    }
			
				if (!mIsStop) {
					onSearchComplete(cities);
				}
			}
		}
	};
	
	////@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int screenMode = -100;
		screenMode = WeatherScreenConfig.getWeatherScreenConfig(WeatherScreenConfig.SEARCHCITY_VIEW_INDEX);
		Log.d("WeatherWidget", "screenMode = " + screenMode);
		if (screenMode == WeatherScreenConfig.LANDSCAPE_ONLY) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (screenMode == WeatherScreenConfig.PORTRAIT_ONLY) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}

		setContentView(R.layout.search_city_activity);
		setTitle(R.string.title_add_city);
		
		ListView list = (ListView) findViewById(R.id.search_city_list);
		list.setOnItemClickListener(this);
		
		mSearchCityInput = (EditText)findViewById(R.id.search_city_text);
		mSearchCityInput.setOnEditorActionListener(new OnEditorActionListener() {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				Log.e("WeatherWidget", "onEditorAction actionId=" + actionId);
				return false;
			}});
		
		mSearchCityBtn = (ImageButton) findViewById(R.id.search_city_image);
		mSearchCityBtn.setEnabled(false);
		mSearchCityBtn.setOnClickListener(new OnClickListener() {
			//@Override
			public void onClick(View v) {
				searchCity(v);	
			}
		});
		
		mSearchCityInput.addTextChangedListener(new TextWatcher()
		{
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count)
			{
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after)
			{
				
			}
			
			@Override
			public void afterTextChanged(Editable s)
			{
				if (TextUtils.isEmpty(s.toString().trim()))
				{
					mSearchCityBtn.setEnabled(false);
				} else
				{
					mSearchCityBtn.setEnabled(true);
				}
			}
		});
		
		this.registerReceiver(receiver, new IntentFilter(Constant.ACTION_SEARCH_CITY_RESPONSE));
	}
	
	protected void onDestroy() {
		//unregisterReceiver(receiver);
		super.onDestroy();
              myHandler.removeCallbacks(timeOutRun);
		unregisterReceiver(receiver);
		finish();		
	}
	
	

	public void searchCity(View v) {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		
		InputMethodManager im = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		im.hideSoftInputFromWindow(mSearchCityBtn.getWindowToken(), 0);
		
		//check net status
		ConnectivityManager conManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE );
        NetworkInfo networkInfo = conManager.getActiveNetworkInfo();
        //无网络，直接提示错误
        if (networkInfo == null || !networkInfo.isAvailable()){ 
        	if (mToast == null){
        		mToast = Toast.makeText(this, getString(R.string.no_network), Toast.LENGTH_SHORT);
        	}
        	mToast.show();
        	return;
        }
		
		mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getText(R.string.search_city_title));
        mProgressDialog.setMessage(getText(R.string.search_city_msg));
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
        
        mIsStop = false;
        myHandler.postDelayed(timeOutRun, 140*1000);
        Intent intent = new Intent(Constant.ACTION_SEARCH_CITY_REQUEST);
        String search_string = mSearchCityInput.getText().toString();
        Log.d("WeatherWidget", "search_string = " + search_string );
        intent.putExtra(Constant.SEARCH_CITY_REQUEST_NAME, search_string);
        startService(intent);
	}

	////@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		final String city = (mItems != null) ? (String)mItems.get(arg2).get("city") : null;
		final String cityId = (mItems != null) ? (String)mItems.get(arg2).get("location") : null;
		new AlertDialog.Builder(this)
			.setTitle(R.string.city_name)
			.setMessage(getString(R.string.add_one_city) + ": " + city)
			.setPositiveButton(
				android.R.string.ok, 
				new DialogInterface.OnClickListener() {						
					//@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = getIntent();
						intent.putExtra("search_city_name", city);
						intent.putExtra("search_city_id", cityId);
						setResult(RESULT_OK, intent);
						finish();
					}
				}
			).setNegativeButton(
					android.R.string.cancel,
					new DialogInterface.OnClickListener() {						
						//@Override
						public void onClick(DialogInterface dialog, int which) {
							;
						}
					}
			).show();
	}
/*
	//@Override
	public void run() {
		String search_string = ((TextView)findViewById(R.id.search_city_text)).getText().toString();
		ArrayList<String> cities = new ArrayList<String>();
		items = WeatherUtils.searchCity(this,search_string);
		myHandler.removeCallbacks(timeOutRun);
		for (Map map : items) {
			String adminArea = (String)map.get("adminArea");
			String country = (String)map.get("country");
			String city = (String)map.get("city");
			if (adminArea != null && !adminArea.equals("")) {
				city += ", " + adminArea;
			} 
			if (country != null && !country.equals("")) {
				city += ", " + country;
			} 
			cities.add(city);
		}
		if (!isStop) {
			onSearchComplete(cities);
		}
	}	
*/	
	private void onSearchComplete(final ArrayList<String> cities) {
		runOnUiThread(new Runnable() {			
			//@Override
			public void run() {
				if (mProgressDialog != null && mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
				
				if (cities != null && !cities.isEmpty()) {
					ListView list = (ListView) findViewById(R.id.search_city_list);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(SearchCityActivity.this,R.layout.search_city_list_item, cities);
					list.setAdapter(adapter);
					
				} else {
					ListView list = (ListView) findViewById(R.id.search_city_list);
					list.setAdapter(null);
					Toast toast = Toast.makeText(SearchCityActivity.this, R.string.no_city_found, 1000);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			}
		});	
	}
}
