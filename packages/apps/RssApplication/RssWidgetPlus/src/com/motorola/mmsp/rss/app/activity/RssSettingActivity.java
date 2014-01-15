package com.motorola.mmsp.rss.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.DatabaseHelper;
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.service.flex.RssFlexFactory;
import com.motorola.mmsp.rss.service.manager.RssAlarmManager;

public class RssSettingActivity extends RssAppBaseActivity implements
		OnClickListener,
		android.content.DialogInterface.OnClickListener, OnItemSelectedListener{
	private static final String TAG = "RssSettingActivity";
	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private String[] mShowItemFor = null;
	private String[] mUpdateFrequency = null;
	private String mCurrentWidgetTitle;
    private String mLastWidgetTitle;
	private int mLastShowTimeFor;
	private int mLastFrequency;
	private int mSelectedPosition = -1;
	private FeedListAdapter mFeedListAdapter;
	private ListView mListSettings;
	private List<FeedInfo> list = new ArrayList<FeedInfo>();
	private ArrayList<FeedInfo> mNewAddedFeedList = new ArrayList<FeedInfo>();
	private ArrayList<FeedInfo> mAddedFeedList = new ArrayList<FeedInfo>();
	private ArrayList<String> mFeedUrlList = new ArrayList<String>();
	private int mUpdateIndex = -1;
	private int mShowIndex = -1;
	private boolean mIsFirstSetting = false;
	private boolean mShouldShowLoadingOnWidget = false;
	private boolean mIsEnterSettingsFromWidget = false;
	private RssSettingBroadcastReceiver mSettingReceiver;
	private ProgressDialog mRefreshDialog;	
	private DatabaseHelper mDatabaseHelper;
	private EditText mWidgetTitle ;
	private Spinner mSpinnerValidity;
	private Spinner mSpinnerFrequency;
	private SettingsThread mSettingsThread;
	private boolean mCancelSubscribe;
	private AlertDialog.Builder mDisclaimerDialog;
	private Handler mHandler = null;
	public static final int KEY_SHOW_ITEM_FOR = 0;
	public static final int KEY_UPDATE_FREQUENCY = 1;
	private static final int FEEDADDINADAPTER = 1;
    private static final String KEY_WIDGET_ID = "widgetId";
    private static final String KEY_WIDGET_UPDATEFREQUENCY = "updateFrequency";
    private static final String KEY_WIDGET_AUTOUPDATE = "autoUpdate";
    private static final String KEY_FEED_INFOS = "feedInfos";
    private static final String KEY_FEED_ID = "feedId";
    private static final String KEY_FEED_CUSTOMINFO = "feedCustomInfo";
    private static final String KEY_FEED_HISTORYLIST = "historyFeedList";
    private static final String KEY_FEED_BUNDLELIST = "bundleFeedList";
    private static final String KEY_DATA_CHANGE = "data_change";
    private static final String KEY_DELETED_FEED_BUNDLE_LIST = "deletedbundleFeedList";
    private static final String KEY_ADDED_FEED_LIST = "addedFeedList";
    private static final String KEY_ONLY_WIDGET = "key_only_widget";
    private static final String SETTING_PREFERENCE = "setting_preference";
    private static final String KEY_SHOW_DISCLAIMER = "key_show_disclaimer";
    
    private static final String KEY_FEED_SUBSCRIBING = "feed_subscribing";
    private static final int WIDGET_NAME_WIDTH = 30;
    
    private static final String KEY_WIDGET_IDS = "widgetIds";
    
    private AlertDialog mRemoveConfirmDlg = null;
    private boolean mDatachanged = false;
    private InputFilter mInputFilter = new InputFilter() {
		
		@Override
		public CharSequence filter(CharSequence source, int start, int end,
				Spanned dest, int dstart, int dend) {
			try {
                int destLen = dest.toString().getBytes("GB18030").length;
                int sourceLen = source.toString().getBytes("GB18030").length;
                if (destLen + sourceLen > WIDGET_NAME_WIDTH) {
                    return "";
                }
                if (source.length() < 0 && (dend - dstart >= 1)) {
                    return dest.subSequence(dstart, dend - 1);
                }
                return source;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate begin");
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.rss_settings);		
//		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.setting_relative_title);
		LayoutInflater inflater = LayoutInflater.from(this);
		ActionBar actionBar = getActionBar();
		View view = inflater.inflate(R.layout.setting_relative_title, null);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setCustomView(view);
		TextView titleView = (TextView) view.findViewById(R.id.setting_relative_title);
		titleView.setText(R.string.rsssettings_prefenrence);
		
		mListSettings = (ListView) findViewById(R.id.addfeeds_list);
		
		view = inflater.inflate(R.layout.widget_title_edit, null);
		mWidgetTitle = (EditText) view.findViewById(R.id.edit_widget_name);
//		mWidgetTitle.setOnClickListener(this);
		mWidgetTitle.setFilters(new InputFilter[]{mInputFilter});
		mWidgetTitle.requestFocus();
		mListSettings.addHeaderView(view, null, false);
		
		view = inflater.inflate(R.layout.setting_header, null);
		ArrayAdapter<CharSequence> adapter = null;
		mSpinnerValidity = (Spinner) view.findViewById(R.id.validity_set);		
		adapter = ArrayAdapter.createFromResource(this, R.array.show_item_for, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
		mSpinnerValidity.setAdapter(adapter);
		mSpinnerValidity.setOnItemSelectedListener(this);
		mSpinnerFrequency = (Spinner) view.findViewById(R.id.frequency_set);
		adapter = ArrayAdapter.createFromResource(this, R.array.update_frequency, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
		mSpinnerFrequency.setAdapter(adapter);
		mSpinnerFrequency.setOnItemSelectedListener(this);
		
		mDatabaseHelper = DatabaseHelper.getInstance(this);
		mListSettings.addHeaderView(view, null, false);	
		setAdapter();
		mShowItemFor = getResources().getStringArray(R.array.show_item_for);
		mUpdateFrequency = getResources().getStringArray(
				R.array.update_frequency);
		initSettings();
//		registerReceiver();
		mHandler = new Handler(){

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case FEEDADDINADAPTER:
					FeedInfo info = (FeedInfo) msg.obj;
					if(info != null) {
						mFeedListAdapter.add(info);
					}					
					break;
				default:
					break;
				}
				super.handleMessage(msg);
			}
			
		};
		mSettingsThread = new SettingsThread();
		mSettingsThread.start();
	}
	@Override
	protected void onResume() {
		Log.d(TAG,"onResume begin");
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG,"onDestroy begin");
//		unRegisterReceiver();
		super.onDestroy();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG,"onCreateid = "+id);
		AlertDialog.Builder builder = null;
		switch (id) {
		case KEY_SHOW_ITEM_FOR:
			Log.d(TAG,"validityid ="+id);
			builder = new AlertDialog.Builder(this).setSingleChoiceItems(
					mShowItemFor, mShowIndex, this).setTitle(
					R.string.title_input_duration);
			return builder.create();
		case KEY_UPDATE_FREQUENCY:
			Log.d(TAG,"frequencyvalueid ="+id);
			builder = new AlertDialog.Builder(this).setSingleChoiceItems(
					mUpdateFrequency, mUpdateIndex, this).setTitle(
					R.string.title_input_frequency);
			return builder.create();
		}
		return null;
	}

	private String setInitWidgetTitle() {
		String title = getResources().getString(R.string.rss);
		return title;
	}

	private void initSettings() {
       	String action = this.getIntent().getAction();
       	Log.d(TAG, "initsetting begin" + action);
       	if (RssConstant.Intent.INTENT_RSSWIDGET_CONFIG.equals(action)) {
			mIsFirstSetting = true;
			mAppWidgetId = getIntent().getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			updateWidgetName();
			showDisclaimerDialog();
		} else if (RssConstant.Intent.INTENT_RSSAPP_SETTINGS.equals(action)) {
			mIsEnterSettingsFromWidget = getIntent().getBooleanExtra("isEnterSettingsFromWidget", false);
			Log.d(TAG, "isEnterSettingsFromWidget = " + mIsEnterSettingsFromWidget);
			Log.d(TAG, "firstEnterBundle != null");
			mIsFirstSetting = false;
			mAppWidgetId = getIntent().getIntExtra(
					KEY_WIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
		}
       	checkDatabase();
       	Cursor widgetsCursor = null;
		try {
			widgetsCursor = getContentResolver().query(
					RssConstant.Content.WIDGET_URI, null,
					RssConstant.Content.WIDGET_ID + "=" + mAppWidgetId, null,
					"");
			if(widgetsCursor != null && widgetsCursor.getCount() > 0) {
				widgetsCursor.moveToFirst();
				String widgetName = widgetsCursor
						.getString(widgetsCursor
								.getColumnIndex(RssConstant.Content.WIDGET_TITLE));
				mUpdateIndex = widgetsCursor
						.getInt(widgetsCursor
								.getColumnIndex(RssConstant.Content.WIDGET_UPDATE_FREQUENCY));
				mShowIndex = widgetsCursor
						.getInt(widgetsCursor
								.getColumnIndex(RssConstant.Content.WIDGET_VALIDITY_TIME));

				mWidgetTitle.setText(widgetName);
				mSpinnerValidity.setSelection(mShowIndex);
				mSpinnerFrequency.setSelection(mUpdateIndex);
				mLastWidgetTitle = widgetName;
				mLastShowTimeFor = mShowIndex;
				mLastFrequency = mUpdateIndex;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(widgetsCursor != null){
				widgetsCursor.close();
			}
		}
	}
	
	private void showDisclaimerDialog(){
		SharedPreferences showDisclaimerPreferences = getSharedPreferences(SETTING_PREFERENCE,Context.MODE_PRIVATE);
		boolean showAgain = showDisclaimerPreferences.getBoolean(KEY_SHOW_DISCLAIMER, true);
		if(showAgain){
			LayoutInflater inflater = LayoutInflater.from(this);
			View view = inflater.inflate(R.layout.setting_disclaimer, null);
			final CheckBox showDisclaimer = (CheckBox) view.findViewById(R.id.is_disclaimer_show); 
			if(mDisclaimerDialog == null){
				mDisclaimerDialog = new AlertDialog.Builder(this);
			}
			mDisclaimerDialog.setTitle(this.getResources().getString(R.string.rsssettings_disclaimer));
			mDisclaimerDialog.setView(view);
			mDisclaimerDialog.setPositiveButton(this.getResources().getString(R.string.ok),new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					boolean checked = showDisclaimer.isChecked();
					SharedPreferences showDisclaimerPreferences = getSharedPreferences(SETTING_PREFERENCE,Context.MODE_PRIVATE);
					showDisclaimerPreferences.edit().putBoolean(KEY_SHOW_DISCLAIMER, !checked).commit();
				}
			});
			mDisclaimerDialog.show();
		}
	}
	private class SettingsThread extends Thread {

		@Override
		public void run() {
			try{
				updateFeedData();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private synchronized void updateFeedData() {
		if(mFeedListAdapter != null){
			mFeedListAdapter.clear();
		}
		Cursor feedsCursor = null;
		try {
			feedsCursor = getContentResolver().query(
					RssConstant.Content.VIEW_FEED_URI, null,
					RssConstant.Content.WIDGET_ID + "=" + mAppWidgetId, null,
					"");
			if(feedsCursor != null && feedsCursor.getCount() > 0) {
				feedsCursor.moveToFirst();
				if(mFeedListAdapter != null){
					FeedInfo info = null;
					if(!feedsCursor.isAfterLast()) {
						do {
							info = new FeedInfo();
							info.feedTitle = feedsCursor
									.getString(feedsCursor
											.getColumnIndex(RssConstant.Content.FEED_TITLE));
							info.feedUrl = feedsCursor.getString(feedsCursor
									.getColumnIndex(RssConstant.Content.FEED_URL));
							info.feedId = feedsCursor.getInt(feedsCursor
									.getColumnIndex(RssConstant.Content.FEED_ID));
							List<String> feedUrlList = new ArrayList<String>();
							for (int j = 0; j < mFeedListAdapter.getCount(); j++) {
								String feedUrl = mFeedListAdapter.getItem(j).feedUrl;
								if(feedUrl != null){
									feedUrlList.add(feedUrl);
								}
								Log.d(TAG,"feedUrlList.SIZE="+mFeedListAdapter.getCount());
							}
							if (!feedUrlList.contains(info.feedUrl)) {
								mHandler.sendMessage(mHandler.obtainMessage(FEEDADDINADAPTER, info));
							}
						} while (feedsCursor.moveToNext());
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(feedsCursor != null){
				feedsCursor.close();
			}
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		// TODO Auto-generated method stub
		super.onPrepareDialog(id, dialog);
	}
	
	public void onClick(DialogInterface dialog, int which) {
		if (which >= 0) {
			dialog.dismiss();
			Log.d(TAG,"mSelectedPosition = "+mSelectedPosition);
			switch (mSelectedPosition) {
			case KEY_SHOW_ITEM_FOR:
//				mWidgetValidity.setText(mShowItemFor[which]);
//				mShowIndex = which;
				break;
			case KEY_UPDATE_FREQUENCY:
//				mWidgetFrequency.setText(mUpdateFrequency[which]);
//				mUpdateIndex = which;
				break;
			default:
				break;
			}
		}
	}
    private void getFeedAddedUrl(){
		mFeedUrlList.clear();
		String [] addedValidatyUrl = null;
		for (int j = 0; j < mFeedListAdapter.getCount(); j++) {
			String feedUrl = mFeedListAdapter.getItem(j).feedUrl;
			if(feedUrl != null){
			 addedValidatyUrl = (feedUrl).split("://");
				if(addedValidatyUrl.length == 1){
					Log.d(TAG, "split size is 1, url is " + addedValidatyUrl[0]);
					mFeedUrlList.add(addedValidatyUrl[0]);
				}else{
					mFeedUrlList.add(addedValidatyUrl[1]);
					Log.d(TAG, "split size is not 1, url is " + addedValidatyUrl[1]);
				}
			}	
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		int count = 0;
		int uncontain = 0;
		String [] newAddedFeedUrl = null;
		String newAddedValidatyUrl = null;
		switch (resultCode) {
		case RssConstant.Flag.FEEDADDBYCUSTOM_RESULECODE:
			Bundle feedCustomBundle = (Bundle) data.getExtras();
			FeedInfo feedCustomInfo = null;
			if(feedCustomBundle != null){
				feedCustomInfo = (FeedInfo) feedCustomBundle
						.getParcelable(KEY_FEED_CUSTOMINFO);
			}
			if(feedCustomInfo != null){
				feedCustomInfo.widgetId = mAppWidgetId;
				Log.d(TAG, "feedset=" + feedCustomInfo.feedUrl);
				setAdapter();
				if(mFeedListAdapter != null && mNewAddedFeedList != null){
					if (mFeedListAdapter.getCount() != 0) {
						getFeedAddedUrl();
						if(feedCustomInfo.feedUrl != null){
							newAddedFeedUrl = (feedCustomInfo.feedUrl).split("://");
						}
						if (newAddedFeedUrl != null) {
							if (newAddedFeedUrl.length == 1) {
								newAddedValidatyUrl = newAddedFeedUrl[0];
							} else {
								newAddedValidatyUrl = newAddedFeedUrl[1];
							}
						}
						Log.d(TAG, "newAddedValidatyUrl is " + newAddedValidatyUrl);
						if (!mFeedUrlList.contains(newAddedValidatyUrl)) {
							uncontain++;
							count = mFeedListAdapter.getCount() + uncontain;
							mFeedListAdapter.add(feedCustomInfo);
							mNewAddedFeedList.add(feedCustomInfo);
						} else {
							Log.d(TAG, "repeat feed, not need added");
						}
					} else {
						mFeedListAdapter.add(feedCustomInfo);
						mNewAddedFeedList.add(feedCustomInfo);
					}
					mFeedListAdapter.notifyDataSetChanged();
				}
			}
			break;
		case RssConstant.Flag.FEEDADDBYBUNDLE_RESULECODE:
			Log.d(TAG, "mAppWidgetId is " + mAppWidgetId);
			boolean datachanged = data.getBooleanExtra(KEY_DATA_CHANGE, false);
			mDatachanged = datachanged;
			Log.d(TAG, "datachanged = " + datachanged);
			ArrayList<FeedInfo> bundleFeedList = data.getParcelableArrayListExtra(KEY_FEED_BUNDLELIST);
			ArrayList<FeedInfo> deletedBundleFeedList = data.getParcelableArrayListExtra(KEY_DELETED_FEED_BUNDLE_LIST);
			if(bundleFeedList != null){
				for (int i = 0; i < bundleFeedList.size(); i++) {
					bundleFeedList.get(i).widgetId = mAppWidgetId;
				}
				setAdapter();
				if(mFeedListAdapter != null && mNewAddedFeedList != null){
					Log.d(TAG,"mFeedListAdapter.getCount() = "+mFeedListAdapter.getCount());
				if (mFeedListAdapter.getCount() != 0) {
					getFeedAddedUrl(); 
					for (int i = 0; i < bundleFeedList.size(); i++) {
					    newAddedFeedUrl = (bundleFeedList.get(i).feedUrl).split("://");
					    if(newAddedFeedUrl != null){
							if(newAddedFeedUrl.length == 1){
								   newAddedValidatyUrl = newAddedFeedUrl[0];
								}
								else{
								   newAddedValidatyUrl = newAddedFeedUrl[1];
								}
							if (!mFeedUrlList.contains(newAddedValidatyUrl)) {
								uncontain++;
								count = mFeedListAdapter.getCount() + uncontain;
								mFeedListAdapter.add(bundleFeedList.get(i));
								Log.d(TAG,"mFeedListAdapterFROMBUNDLESIZE = "+mFeedListAdapter.getCount());
								mNewAddedFeedList.add(bundleFeedList.get(i));
							}
					    }
					}    
				} else {
					for (int i = 0; i < bundleFeedList.size(); i++) {
						mFeedListAdapter.add(bundleFeedList.get(i));
						mNewAddedFeedList.add(bundleFeedList.get(i));
					}
				}
				if (deletedBundleFeedList != null) {
					for (int i = 0; i < deletedBundleFeedList.size(); i++) {
						FeedInfo deleteFeedInfo = deletedBundleFeedList.get(i);
						if(deleteFeedInfo != null) {
							String deleteFeedUrl = deleteFeedInfo.feedUrl;
							FeedInfo addedFeedInfo = null;
							String addedFeedUrl = null;
							for (int j = 0; j < mFeedListAdapter.getCount(); j++) {
								addedFeedInfo = mFeedListAdapter.getItem(j);
								if(addedFeedInfo != null){
									addedFeedUrl = addedFeedInfo.feedUrl;
									if(addedFeedUrl != null && deleteFeedUrl != null){
										if(addedFeedUrl.equals(deleteFeedUrl)){
											mFeedListAdapter.remove(addedFeedInfo);
											mNewAddedFeedList.remove(addedFeedInfo);
										}
									}
								}
							}
						}					
					}
				}
				mFeedListAdapter.notifyDataSetChanged();
				}
			}
			break;
		case RssConstant.Flag.FEEDADDBYHISTORY_RESULECODE:
			ArrayList<FeedInfo> historyFeedList = (ArrayList<FeedInfo>) data
					.getSerializableExtra(KEY_FEED_HISTORYLIST);
			if(historyFeedList != null){
				for (int i = 0; i < historyFeedList.size(); i++) {
					historyFeedList.get(i).widgetId = mAppWidgetId;
				}
				setAdapter();
				if(mFeedListAdapter != null && mNewAddedFeedList != null){
					if (mFeedListAdapter.getCount() != 0) {
						getFeedAddedUrl();
						for (int i = 0; i < historyFeedList.size(); i++) {
						    newAddedFeedUrl = (historyFeedList.get(i).feedUrl).split("://");
						    if(newAddedFeedUrl != null){
							    if(newAddedFeedUrl.length == 1){
									   newAddedValidatyUrl = newAddedFeedUrl[0];
									}
									else{
									   newAddedValidatyUrl = newAddedFeedUrl[1];
									}
						    }
							if (!mFeedUrlList.contains(newAddedValidatyUrl)) {
								count = mFeedListAdapter.getCount() + uncontain;
								mFeedListAdapter.add(historyFeedList.get(i));
								mNewAddedFeedList.add(historyFeedList.get(i));
							}
						}
					} else {
						for (int i = 0; i < historyFeedList.size(); i++) {
							mFeedListAdapter.add(historyFeedList.get(i));
							mNewAddedFeedList.add(historyFeedList.get(i));
						}
					}
					mFeedListAdapter.notifyDataSetChanged();
				}
			}
			break;
		default:
			break;
		}
	}

	private class FeedListAdapter extends ArrayAdapter<FeedInfo> {
		private LayoutInflater mInflater;
		public FeedListAdapter(Context context, List<FeedInfo> list) {
			super(context, 0, list);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.feed_list, null);
			}
			TextView feedName = (TextView) convertView
					.findViewById(R.id.feed_name);
			if (this.getItem(position).feedTitle != null) {
				feedName.setText(this.getItem(position).feedTitle);
			} else {
				feedName.setText(this.getItem(position).feedUrl);
			}
			ImageButton feedDelBtn = (ImageButton) convertView
					.findViewById(R.id.sub_feed);
			feedDelBtn.setTag(position);
			feedDelBtn.setOnClickListener(RssSettingActivity.this);
			return convertView;
		}
	}

	public void setAdapter() {
		if (mFeedListAdapter == null) {
			mFeedListAdapter = new FeedListAdapter(this, list);
			if (mListSettings != null) {
				mListSettings.setAdapter(mFeedListAdapter);
			}
		}
	}

	public void onAddFeed(View view) {
		mAddedFeedList.clear();
		for(int i = 0; i < mFeedListAdapter.getCount(); i ++){
			mAddedFeedList.add(mFeedListAdapter.getItem(i)); 
		}
		Log.d(TAG,"mAddedFeedListTOBUNDLE = " + mAddedFeedList.size());
		Intent in = new Intent(this, FeedAddActivity.class);
		in.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		in.putParcelableArrayListExtra(KEY_ADDED_FEED_LIST, mAddedFeedList);
		startActivityForResult(in, RssConstant.Flag.FEED_REQUESTCODE);
	}

	private void updateWidgetName(){
		Intent update = new Intent(RssConstant.Intent.INTENT_RSSAPP_UPDATE_WIDGET_NAME);
		update.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		sendBroadcast(update);
	}
	public void onSubmit(View v) {
		Log.d(TAG, "onSubmit1");
		Intent in = this.getIntent();
		String action = in.getAction();
		updateSettingData();
		
		updateWidgetName();
		
		manageAlarm();
		Log.d(TAG,"action = "+action);
		in.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		int mFeedAddedNum = -1;
        if(mFeedListAdapter != null){
          mFeedAddedNum = mFeedListAdapter.getCount();
          Log.d(TAG, "mFeedAddedNum is " + mFeedAddedNum);
        } else {
        	 Log.d(TAG, "mFeedListAdapter is empty");
        }
		if (RssConstant.Intent.INTENT_RSSWIDGET_CONFIG.equals(action)){
			if ((mNewAddedFeedList != null) && !mNewAddedFeedList.isEmpty()) {
				if (mFeedAddedNum > RssConstant.Value.MAX_FEED_ADDEDNUM) {
					Toast toast = Toast.makeText(this, this.getResources().getString(R.string.feedaddednumtips),Toast.LENGTH_SHORT);
					toast.show();
				} else {
					mShouldShowLoadingOnWidget = true;
					notifyServiceToRefresh();
//					showRefreshDialog();
					notifyWidgetOrDetailToUpdate();
					Intent i = new Intent(RssConstant.Intent.INTENT_RSSWIDGET_REFRESH);
					i.putExtra(KEY_WIDGET_ID, mAppWidgetId);
					i.putExtra("autoUpdate", false);
					i.putExtra(KEY_ONLY_WIDGET, true);
//					sendBroadcast(i);
					finish();
				}
			} else{
//				notifyWidgetOrDetailToUpdate();
				Intent i = new Intent(RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED);
				i.putExtra(KEY_WIDGET_ID, mAppWidgetId);
				i.putExtra("autoUpdate", false);
				i.putExtra(KEY_ONLY_WIDGET, true);
				sendBroadcast(i);
				finish();
			}
		}else if ((RssConstant.Intent.INTENT_RSSAPP_SETTINGS.equals(action))){
			if((mNewAddedFeedList != null) && !mNewAddedFeedList.isEmpty()) {
				Log.d(TAG, "mNewAddedFeedList size is " + mNewAddedFeedList.size() + " , empty = " + mNewAddedFeedList.isEmpty());
				if(mFeedAddedNum > RssConstant.Value.MAX_FEED_ADDEDNUM){
					Toast toast = Toast.makeText(this,this.getResources().getString(R.string.feedaddednumtips),Toast.LENGTH_SHORT);
					toast.show();
				}else{
					mShouldShowLoadingOnWidget = true;
					notifyServiceToRefresh();
//					showRefreshDialog();
					mDatachanged = true;
					Intent i = new Intent(RssConstant.Intent.INTENT_RSSWIDGET_REFRESH);
					i.putExtra(KEY_WIDGET_ID, mAppWidgetId);
					i.putExtra("autoUpdate", false);
					i.putExtra(KEY_ONLY_WIDGET, true);
//					sendBroadcast(i);
					notifyWidgetOrDetailToUpdate();
					
					Intent intent = new Intent();
					intent.putExtra(KEY_FEED_SUBSCRIBING, true);
					intent.putExtra(KEY_DATA_CHANGE, mDatachanged);
					setResult(RESULT_OK, intent);
					finish();
				}
//				notifyWidgetOrDetailToUpdate();
			}else{
			/*	Log.d(TAG,"else");
				if(sizeofWidgetContainFeed(mAppWidgetId) != 0) {
					if(mUpdateIndex != mLastFrequency) {
						Log.d(TAG, "update frequency changed, update alarm");
						if(mUpdateIndex == RssConstant.Content.UPDATE_FRE_NEVER){
							notifyServiceToDeleteAlarm();
						}else{
							notifyServiceToUpdateAlarm();
						}
					}
				} else {
					Log.d(TAG, "all feeds has been deleted, delete alarm");
					notifyServiceToDeleteAlarm();
				}
//				notifyWidgetOrDetailToUpdate();
			 	*/
				Intent intent = new Intent();
				intent.putExtra(KEY_DATA_CHANGE, mDatachanged);
				setResult(RESULT_OK, intent);
				finish();
			}
		}
	}
	
	private void notifyServiceToUpdateAlarm() {
		Intent alarmIntent = new Intent();
		alarmIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		alarmIntent.putExtra(KEY_WIDGET_UPDATEFREQUENCY, mUpdateIndex);
		alarmIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_UPDATE_ALARM);				
		Log.d(TAG, "uodate alarm, widgetId is " + mAppWidgetId + " updateFrequency is" + mUpdateIndex);
		startService(alarmIntent);
	}
	
	private void notifyServiceToAddAlarm() {
		Intent alarmIntent = new Intent();
		alarmIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		alarmIntent.putExtra(KEY_WIDGET_UPDATEFREQUENCY, mUpdateIndex);
		alarmIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_ADD_ALARM);
		Log.d(TAG, "add alarm, widgetId is " + mAppWidgetId + " updateFrequency is" + mUpdateIndex);
		startService(alarmIntent);
	}
	
	private void notifyServiceToDeleteAlarm() {
		Intent alarmIntent = new Intent();
		alarmIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		alarmIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_DELETE_ALARM);
		Log.d(TAG, "delete alarm, widgetId is " + mAppWidgetId);
		startService(alarmIntent);
	}
	
	private void updateSettingData() {
		mCurrentWidgetTitle = mWidgetTitle.getText().toString().trim();
		if(mCurrentWidgetTitle == null||"".equals(mCurrentWidgetTitle)){			
			mCurrentWidgetTitle = setInitWidgetTitle();
			mWidgetTitle.setText(mCurrentWidgetTitle);
		}
		ContentValues values = new ContentValues();
		values.put(RssConstant.Content.WIDGET_ID, mAppWidgetId);
		Log.d(TAG, "mCurrentWidgetTitle is " + mCurrentWidgetTitle + " mLastWidgetTitle is " + mLastWidgetTitle);
		if(!mCurrentWidgetTitle.equals(mLastWidgetTitle)){
			values.put(RssConstant.Content.WIDGET_TITLE, mCurrentWidgetTitle);
		}
		if(mUpdateIndex != mLastFrequency){
			values.put(RssConstant.Content.WIDGET_UPDATE_FREQUENCY, mUpdateIndex);
		}
		if(mShowIndex != mLastShowTimeFor){
			values.put(RssConstant.Content.WIDGET_VALIDITY_TIME, mShowIndex);
		}
		getContentResolver().update(RssConstant.Content.WIDGET_URI, values, "widget_id = '" + mAppWidgetId + "'", null);
	}
	
	private void notifyServiceToRefresh() {
		Intent subscribeIntent = new Intent();
		subscribeIntent.setAction(RssConstant.Intent.INTENT_RSSSERVICE_ADDFEED);
		if(mNewAddedFeedList != null){
			subscribeIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
			subscribeIntent.putExtra(KEY_FEED_INFOS, mNewAddedFeedList);
//			subscribeIntent.putExtra(KEY_WIDGET_UPDATEFREQUENCY, (mUpdateIndex != mLastFrequency));
			subscribeIntent.putParcelableArrayListExtra(KEY_FEED_INFOS, mNewAddedFeedList);
			subscribeIntent.putExtra(KEY_WIDGET_AUTOUPDATE, false);
			Log.d(TAG, "Add feedList size is " + mNewAddedFeedList.size());
		}
		startService(subscribeIntent);
	}
	
	private void showRefreshDialog() {
		if(mRefreshDialog == null) {
			mRefreshDialog = new ProgressDialog(this);
		}		
		mRefreshDialog.setMessage(this.getResources().getString(R.string.subscribing));
		Log.d(TAG,"dialog begin");
		mRefreshDialog.show();
		mRefreshDialog.setCanceledOnTouchOutside(false);
		mRefreshDialog.setOnKeyListener(new OnKeyListener() {
			
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if(keyCode==KeyEvent.KEYCODE_BACK){
					mRefreshDialog.dismiss();
					mCancelSubscribe = true;
					Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_STOP_UPDATING);
					intent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
					Log.d(TAG,"BACK");
					startService(intent);
					notifyWidgetOrDetailToUpdate();
				}
				return false;
			}
		});
		
	}
	
	private void dismissRefreshDialog() {
		if(mRefreshDialog != null && mRefreshDialog.isShowing()) {
			mRefreshDialog.dismiss();
			mRefreshDialog = null;
		}
	}
	
	private void registerReceiver() {
		mSettingReceiver = new RssSettingBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(RssConstant.Intent.INTENT_RSSAPP_SUBSCRIBE_FINISHED);
		filter.addAction(RssConstant.Intent.INTENT_NETWORK_NOTAVAILABLE);
		registerReceiver(mSettingReceiver, filter);
	}
	
	private void unRegisterReceiver() {
		if(mSettingReceiver != null) {
			unregisterReceiver(mSettingReceiver);
			mSettingReceiver = null;
		}
	}
	
	class RssSettingBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
			if (RssConstant.Intent.INTENT_RSSAPP_SUBSCRIBE_FINISHED
					.equals(action)
					|| RssConstant.Intent.INTENT_NETWORK_NOTAVAILABLE
							.equals(action)) {
				Log.d(TAG,
						"mShouldShowLoadingOnWidget network is ont available");
				mShouldShowLoadingOnWidget = false;
				notifyWidgetOrDetailToUpdate();
				dismissRefreshDialog();
				if (mIsFirstSetting) {
					if (sizeofWidgetContainFeed(mAppWidgetId) != 0) {
						Log.d(TAG, "first setting, add feed finish");
						if (mUpdateIndex != RssConstant.Content.UPDATE_FRE_NEVER) {
							notifyServiceToAddAlarm();
						}
					} else {
						Log.d(TAG, "first setting, no feed added");
					}
				} else {
					if (sizeofWidgetContainFeed(mAppWidgetId) != 0) {
						if (sizeofWidgetContainFeed(mAppWidgetId) == mNewAddedFeedList
								.size()) {
							Log.d(TAG,
									"last with no feed, no add new feed, need add alarm");

							if (mUpdateIndex != RssConstant.Content.UPDATE_FRE_NEVER) {
								notifyServiceToAddAlarm();
							}
						} else {
							if (mLastFrequency != mUpdateIndex) {
								Log.d(TAG,
										"update frequency changed, update alarm");
								if (mUpdateIndex == RssConstant.Content.UPDATE_FRE_NEVER) {
									notifyServiceToDeleteAlarm();
								} else {
									notifyServiceToUpdateAlarm();
								}
							}
						}
					} else {
						Log.d(TAG, "all feed has been deleted, delete alarm");
						notifyServiceToDeleteAlarm();
					}
				}
				submitSettingFinish(widgetId);
			}
		}
	}
	
	private void notifyWidgetOrDetailToUpdate(){
		Intent in = null;
	    in = new Intent(RssConstant.Intent.INTENT_RSSWIDGET_REFRESH);
	    int count = mDatabaseHelper.getItemCount(this, mAppWidgetId);
	    /*
		if(count <= 0){
			Log.d(TAG,"mIsFirstSettingNewAdded");
			in = new Intent(RssConstant.Intent.INTENT_RSSAPP_FEED_ADDED_FROM_EMPTY_FINISHED);
		}
		*/
		Log.d(TAG, "mAppWidgetId = " + mAppWidgetId);
		in.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		in.putExtra("autoUpdate", false);
		in.putExtra(KEY_ONLY_WIDGET, true);
		sendBroadcast(in);
	}
	
	private void submitSettingFinish(int widgetId) {
		Intent in = new Intent();
		in.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		Log.d(TAG, "mIsFirstSetting = " + mIsFirstSetting);
		Log.d(TAG, "widgetId = " + widgetId);
		Log.d(TAG, "mAppWidgetId = " + mAppWidgetId);
		if (widgetId == mAppWidgetId) {
			if (mIsFirstSetting) {
				mShouldShowLoadingOnWidget = false;
				notifyWidgetOrDetailToUpdate();
			} else {
				Intent intent = new Intent();
				intent.putExtra(KEY_DATA_CHANGE, true);
				setResult(RESULT_OK, intent);
			}
			if (mCancelSubscribe) {
				mCancelSubscribe = false;
			} else {
				finish();
			}
		}
	}

	public void onClick(View v) {
		final View view =  v;
		int id = view.getId();
		switch(id){
		/**
		case R.id.validityvalue:
			showDialog(KEY_SHOW_ITEM_FOR);
			mSelectedPosition = KEY_SHOW_ITEM_FOR;
			break;
		case R.id.frequencyvalue:
			showDialog(KEY_UPDATE_FREQUENCY);
			mSelectedPosition = KEY_UPDATE_FREQUENCY;
			break;
		*/
		case R.id.sub_feed:
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
			.setMessage(this.getResources().getString(R.string.removefeedconfirm))
			.setPositiveButton(this.getResources().getString(R.string.remove), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if(mNewAddedFeedList != null && mFeedListAdapter != null){
					mSelectedPosition = (Integer) view.getTag();
					if(mNewAddedFeedList.contains(mFeedListAdapter.getItem(mSelectedPosition))) {
						mNewAddedFeedList.remove(mFeedListAdapter.getItem(mSelectedPosition));
					}
					List<String> feedUrlList = new ArrayList<String>();
					for (int j = 0; j < mFeedListAdapter.getCount(); j++) {
						feedUrlList.add(mFeedListAdapter.getItem(j).feedUrl);
					}
					Log.d(TAG, "deletefeedId = "+mFeedListAdapter.getItem(mSelectedPosition).feedId);
					Log.d(TAG, "deletefeedname = "+mFeedListAdapter.getItem(mSelectedPosition).feedTitle);
					if(feedUrlList.contains(mFeedListAdapter.getItem(mSelectedPosition).feedUrl)){
						deleteFeed(mFeedListAdapter.getItem(mSelectedPosition).feedId);
						SharedPreferences preference = getSharedPreferences(RssConstant.Content.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
						preference.edit().putInt(RssConstant.Key.KEY_WIDGET_ID, mAppWidgetId).commit();
					}		
					mFeedListAdapter.remove(mFeedListAdapter.getItem(mSelectedPosition));
				    Log.d(TAG,"mFeedListAdapter.getCount= "+mFeedListAdapter.getCount());
					}
				}
			})
			.setNegativeButton(this.getResources().getString(R.string.cancel), null);
			mRemoveConfirmDlg = builder.create(); 
			mRemoveConfirmDlg.setCanceledOnTouchOutside(false);
			mRemoveConfirmDlg.show();
			break;
		default:
		    break;
		}
	}
	
	public void deleteFeed(int feedId){
		mDatachanged = true;
		mDatabaseHelper.markFeedDeleted(mAppWidgetId, feedId);
		mDatabaseHelper.markItemDeleted(mAppWidgetId, feedId);
		Intent deleteFeedIntent = new Intent();
		deleteFeedIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_DELETE_FEED);
		deleteFeedIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		deleteFeedIntent.putExtra(KEY_FEED_ID, feedId);
        startService(deleteFeedIntent);		
	}
	
	private int sizeofWidgetContainFeed(int widgetId) {
		List<FeedInfo> list = new ArrayList<FeedInfo>();
		mDatabaseHelper.getFeedsByWidgetId(widgetId, list);
		if(list.isEmpty()){
			return 0;
		}
		return list.size();
	}
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		int listPos = mListSettings.getScrollY();
		Log.d(TAG, "listPos = " + listPos);
		super.onConfigurationChanged(newConfig);
	}
	@Override
	public void onBackPressed() {
		Intent intent = new Intent();
		intent.putExtra(KEY_DATA_CHANGE, mDatachanged);
		setResult(RESULT_OK, intent);
		finish();
	}
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		int id = arg0.getId();
		switch(id){
		case R.id.validity_set:
			Log.d(TAG, "argo = " + arg0 + " , arg1 = " + arg1 + " , arg2 = " + arg2);
			mShowIndex = arg2;
			break;
		case R.id.frequency_set:
			Log.d(TAG, "argo = " + arg0 + " , arg1 = " + arg1 + " , arg2 = " + arg2);
			mUpdateIndex = arg2;
			break;
		}
	}
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		int id = arg0.getId();
		switch(id){
		case R.id.validity_set:
			mShowIndex = -1;
			break;
		case R.id.frequency_set:
			mUpdateIndex = -1;
			break;
		default:
			break;
		}
	}
	
	
	private void manageAlarm() {
		if (mIsFirstSetting) {
			Log.d(TAG, "first setting, add feed finish");
			if (mUpdateIndex != RssConstant.Content.UPDATE_FRE_NEVER) {
				notifyServiceToAddAlarm();
			}
		} else {
			if (mLastFrequency != mUpdateIndex) {
				Log.d(TAG, "update frequency changed, update alarm");
				if (mUpdateIndex == RssConstant.Content.UPDATE_FRE_NEVER) {
					notifyServiceToDeleteAlarm();
				} else {
					notifyServiceToUpdateAlarm();
				}
			}
			RssAlarmManager manager = RssAlarmManager.getInstance(this);
			boolean alarmSetted = manager.alartSetted(mAppWidgetId);
			Log.d(TAG, "Widget`s Alarm is setted : " + alarmSetted);
			if(!alarmSetted && (mNewAddedFeedList != null) && !mNewAddedFeedList.isEmpty()){
				notifyServiceToAddAlarm();
			}
		}
	}
	private void checkDatabase(){
		String selection = RssConstant.Content.WIDGET_ID + "=" + mAppWidgetId;
		Cursor c = null;
		String []projection = new String[]{RssConstant.Content.WIDGET_ID};
		boolean exist = false;
		try{
			c = mDatabaseHelper.query(RssConstant.Content.WIDGET_URI, projection, selection, null, null);
			if(c != null){
				exist = c.getCount() > 0;
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		Log.d(TAG ,"exist = " + exist);
		if(!exist){
			ContentValues values = new ContentValues();
			values.put(RssConstant.Content.WIDGET_ID, mAppWidgetId);
			values.put(RssConstant.Content.WIDGET_TITLE, getResources().getString(R.string.rss));
			int defaultFrequencyIndex = RssFlexFactory.createFlexSettings().getUpdateFrequency(this);
			int defaultValidityIndex = RssFlexFactory.createFlexSettings().getNewsValidity(this);
			values.put(RssConstant.Content.WIDGET_UPDATE_FREQUENCY, defaultFrequencyIndex);
			values.put(RssConstant.Content.WIDGET_VALIDITY_TIME, defaultValidityIndex);
			getContentResolver().insert(RssConstant.Content.WIDGET_URI, values);
			
			Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_UPDATE_WIDGET_FOR_EXCEPTION);
			intent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
			sendBroadcast(intent);
		}
	}
}
