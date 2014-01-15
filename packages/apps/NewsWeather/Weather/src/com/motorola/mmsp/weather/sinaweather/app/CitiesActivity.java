package com.motorola.mmsp.weather.sinaweather.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.motorola.mmsp.sinaweather.app.WeatherScreenConfig;
import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.sinaweather.app.DragList.DragListener;
import com.motorola.mmsp.weather.sinaweather.app.DragList.DropListener;
import com.motorola.mmsp.weather.sinaweather.app.DragList.RemoveListener;

public class CitiesActivity extends Activity implements OnItemClickListener{
	public static ArrayList<String> mCityIds = null;
	private static final int SEARCH_CITY = 0;
	
	private AlertDialog mDisClaimDlg = null;
	private AlertDialog mDeleteDlg = null;   //add  by davidwgx
	
	private String mWidgetId;
	
	private Toast mToast;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

 		int screenMode = -100;
 		screenMode = WeatherScreenConfig.getWeatherScreenConfig(WeatherScreenConfig.CITIES_VIEW_INDEX);
		Log.d("WeatherWidget", "screenMode = " + screenMode );
		if (screenMode == WeatherScreenConfig.LANDSCAPE_ONLY) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		} else if (screenMode == WeatherScreenConfig.PORTRAIT_ONLY) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		}


		setContentView(R.layout.cities_activity);
		setTitle(R.string.title_weather_city);
		mCityIds = PersistenceData.getCityids(this);
		
		mWidgetId = getIntent().getStringExtra("widgetId");
		
		findViewById(R.id.cities_activity_add).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				if (mCityIds.size() < 9) {
					comfirAddCity();
				} else {
					if (mToast == null){
						mToast = Toast.makeText(CitiesActivity.this, R.string.too_most_cities, Toast.LENGTH_LONG);
					}
					mToast.show();
				}
			}
		});

		DragList l = ((DragList)findViewById(R.id.cities_list));
		l.setOnItemClickListener(this);
		l.setDragListener(new DragListener() {
			public void drag(int from, int to) {
								
			}
		});
		l.setDropListener(new DropListener() {
			public void drop(int from, int to) {
				if (from == to) {
					return;
				}
				String temp = mCityIds.get(from);
				int next = (to-from > 0) ? 1 : -1;
				for (int i = from; (i * next) <  (to * next); i += next) {
					mCityIds.set(i, mCityIds.get(i+next));
				}
				mCityIds.set(to, temp);
				PersistenceData.setCityids(CitiesActivity.this, mCityIds);
				WeathersManager.getInstance(CitiesActivity.this).syncCityListFromPersistenceData(CitiesActivity.this);
				initCities();
				
				Intent intent = new Intent(Constant.ACTION_CITIES_REARRANGED);
				startService(intent);
			}
		});
		l.setRemoveListener(new RemoveListener() {
			public void remove(int which) {
				
			}
		});
		initCities();
	}
	protected void onResume() {
		Log.d("WeatherWidget", "CitiesActivity -> onResume()" );  
		super.onResume();  
	}
	
	protected void onPause() {
		Log.d("WeatherWidget", "CitiesActivity -> onPause()" );
		super.onPause();
		//add  by davidwgx   for  switchui-753
		if (mDeleteDlg != null) {
			mDeleteDlg.dismiss();
		}
	}
	
	//@Override
	protected void onDestroy() {
		super.onDestroy();
//		mCityIds = null;
	}
	
	private void initCities() {
		ArrayList<String> cities = new ArrayList<String>();
//		SharedPreferences sp = PersistenceData.getSharedPreferences(this);
		for (String cityId : mCityIds) {
			String city = PersistenceData.getCityName(CitiesActivity.this, cityId); //String city = sp.getString(cityId, "");
			cities.add(city);
		}
		showCities(cities);
	}
	
	private void showCities(ArrayList<String> cities) {		
		if (cities != null) {
			ArrayList<Map<String,Object>> contents = new ArrayList<Map<String,Object>>();
			for (int i = 0; i < cities.size(); i++) {
				Map<String,Object> item = new HashMap<String,Object>();
				item.put("text", cities.get(i));
				item.put("icon", R.drawable.city_del);
				contents.add(item);
			}
			SimpleAdapter adapter = new SimpleAdapter(this, 
					contents,
					R.layout.text_image_list_item,
					new String[]{"text","icon"},
					new int[] {R.id.text_image_list_item_text, R.id.text_image_list_item_image}
			);
			((ListView)findViewById(R.id.cities_list)).setAdapter(adapter);
		}
	}
	
	private void launchAddCity() {
		Intent intent = new Intent(this, SearchCityActivity.class);
        startActivityForResult(intent, SEARCH_CITY);				
	}
	
	private void onContinue() {
		PersistenceData.setDisclaimer(this, false);
		launchAddCity();  
	}
	
	private void comfirAddCity() {
		boolean bDiscliamer = PersistenceData.getDisclaimer(this);
		if (bDiscliamer) {
			try {
				if (mDisClaimDlg != null) {
					mDisClaimDlg.dismiss();
				} 
				AlertDialog.Builder disClaimDlgBuilder = new AlertDialog.Builder(CitiesActivity.this)
																.setTitle(R.string.title_discliamer)
																.setMessage(getString(R.string.comment_discliamer_new))
																.setPositiveButton(
																		R.string.continue_string,
																		new DialogInterface.OnClickListener() {						
																			public void onClick(DialogInterface dialog, int which) {															
																				onContinue();
																			}
																		}
																).setNegativeButton(
																		android.R.string.cancel,
																		new DialogInterface.OnClickListener() {
																			public void onClick(DialogInterface dialog, int which) {
																				;
																			}
																		}
																).setOnCancelListener(new DialogInterface.OnCancelListener() {
																	@Override
																	public void onCancel(DialogInterface dialog) {
																		;
																	}
																});
				mDisClaimDlg = disClaimDlgBuilder.create();
				if (mDisClaimDlg != null) {
					mDisClaimDlg.show();
				}
			} catch(Exception e) {
				Log.d("WeatherWidget", "comfirAddCity exception occurs when show the alert dialog!!!!!!!");
			}
		} else {
			launchAddCity();
		}
	}
	
	//@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == SEARCH_CITY && resultCode == RESULT_OK && data != null) {
			addCity(data.getExtras().getString("search_city_name"), data.getExtras().getString("search_city_id"));
		} 
	}
	
	private void addCity(String city, String cityId)
	{
		if (cityId != null && !cityId.equals("") && mCityIds.contains(cityId))
		{
			Toast.makeText(this, R.string.city_exists, Toast.LENGTH_SHORT)
					.show();
		} else
		{
			mCityIds.add(cityId);
			PersistenceData.setCityids(this, mCityIds);
			PersistenceData.setCity(this, cityId, city);

			boolean isAddCitySuccessful = false;
			ArrayList<String> tmpCityIds = PersistenceData.getCityids(this);
			if (tmpCityIds != null && tmpCityIds.contains(cityId))
			{
				Log.d("WeatherWidget", "^^^^^^^ tmpCityIds is valid");
				String tmpCityName = "";
				tmpCityName = PersistenceData.getCityName(this, cityId);
				if (tmpCityName != null && !tmpCityName.equals(""))
				{
					Log.d("WeatherWidget",
							"@@@ city is inserted into setting table successfully");
					isAddCitySuccessful = true;
				} else
				{
					Log.d("WeatherWidget", ":( :( :( :( :(  city name is empty");
				}

			} else
			{
				Log.d("WeatherWidget", ":( :( :(  tmpCityIds is invalid, exit");
				if (tmpCityIds != null)
				{
					Log.d("WeatherWidget",
							"cityIds size is " + tmpCityIds.size());
				}
			}

			if (!isAddCitySuccessful)
			{
				mCityIds.remove(cityId);
				PersistenceData.setCityids(this, mCityIds);
				Toast toast = Toast.makeText(this, R.string.error,
						Toast.LENGTH_LONG);
				toast.show();
			} else
			{
				initCities();
				Intent intent = new Intent(Constant.ACTION_CITIES_ADD_REQUEST);
				intent.putExtra(Constant.CITY_NAME, city);
				intent.putExtra(Constant.CITY_ID, cityId);
				intent.putExtra("widgetId", mWidgetId);
				Log.i("Weather", "CitiesActivity addCity mWidgetId = "
						+ mWidgetId);
				startService(intent);
			}
		}
	}

	
	private void deleteCity(String cityId) {		
		mCityIds.remove(cityId);
		PersistenceData.setCityids(this, mCityIds);
		
		initCities();
		
		Intent intent = new Intent(Constant.ACTION_CITIES_DELETE_REQUEST);
		intent.putExtra(Constant.CITY_ID, cityId);
		startService(intent);
	}
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);

		setIntent(intent);
		mWidgetId = intent.getStringExtra("widgetId");
	
	}
	
	
	
	//@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Map<String,Object> item = (Map<String,Object>)arg0.getAdapter().getItem(arg2);
		final String city = (String)item.get("text");
		final String cityId = mCityIds.get(arg2);
		
		try {
/*
			new AlertDialog.Builder(this)
				.setTitle(R.string.del_city)
				.setMessage(getString(R.string.del_this_city) + ":" + city + "?")
				.setPositiveButton(
					android.R.string.ok, 
					new DialogInterface.OnClickListener() {						
						public void onClick(DialogInterface dialog, int which) {
							deleteCity(cityId);
							WeathersManager.getInstance(CitiesActivity.this).deleteWeatherFromCache(CitiesActivity.this, cityId);
						}
					}
				).setNegativeButton(
						android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								;
							}
						}
				).show();
*/
		AlertDialog.Builder disDeleteBuilder = new AlertDialog.Builder(this)
													.setTitle(R.string.del_city)
													.setMessage(getString(R.string.del_this_city) + ": " + city)
														.setPositiveButton(
																android.R.string.ok,
																new DialogInterface.OnClickListener() { 					
																	public void onClick(DialogInterface dialog, int which) {																			
																		deleteCity(cityId);
																		WeathersManager.getInstance(CitiesActivity.this).deleteWeatherFromCache(CitiesActivity.this, cityId);
																	}
																}
														).setNegativeButton(
																android.R.string.cancel,
																new DialogInterface.OnClickListener() {
																	public void onClick(DialogInterface dialog, int which) {
																		;
																	}
																}
														);
		
		mDeleteDlg= disDeleteBuilder.create();
		if (mDeleteDlg != null) {
			mDeleteDlg.show();
		}


		} catch(Exception e) {
			Log.d("WeatherWidget", "onItemClick exception occurs when show the alert dialog!!!!!!!");
		}
	}
}
