package com.motorola.mmsp.rss.app.activity;

import java.util.ArrayList;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.DatabaseHelper;
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.service.flex.RssFlexFactory;
import com.motorola.mmsp.rss.service.flex.RssFlexFeedInfo;
import com.motorola.mmsp.rss.service.flex.RssFlexSettings;

public class FeedBundleListActivity extends RssAppBaseActivity implements OnItemClickListener {
	private ListView mBundleListSettings = null;
	private String[] mAllBundleFeedNames = null;
	private String[] mAllBundleFeedUrls = null;
	private BundleAdapter mBundleAdapter = null;
	private int mAppWidgetId = -1;
	private ArrayList<FeedInfo> mAddedFeedList = new ArrayList<FeedInfo>();
	private ArrayList<FeedInfo> mAddedIsBundleFeedList = new ArrayList<FeedInfo>();
	private static final String KEY_FEED_BUNDLELIST = "bundleFeedList";
	private static final String KEY_DELETED_FEED_BUNDLE_LIST = "deletedbundleFeedList";
	private static final String KEY_ADDED_FEED_LIST = "addedFeedList";
	private static final String KEY_WIDGET_ID = "widgetId";
	private String TAG = "FeedBundleListActivity";
	private static final String KEY_FEED_ID = "feedId";
	private static final String KEY_DATA_CHANGE = "data_change";
	private DatabaseHelper mDatabaseHelper;
	private boolean mDataChanged = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.feedbundlelist);		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.setting_relative_title);
		TextView titleView = (TextView) getWindow().findViewById(R.id.setting_relative_title);
		titleView.setText(R.string.addbybundlestitle);
		mDatabaseHelper = DatabaseHelper.getInstance(this);
		mBundleListSettings = (ListView) findViewById(R.id.feedlist);
		mBundleListSettings.setOnItemClickListener(this);
		ArrayList<RssFlexFeedInfo> bundleList = RssFlexFactory.createFlexSettings().getFlexFeedList(this);
		if(bundleList != null && !bundleList.isEmpty()) {
			mAllBundleFeedNames = new String[bundleList.size()];
			mAllBundleFeedUrls = new String[bundleList.size()];
			for(int i = 0; i < bundleList.size(); i++) {
				mAllBundleFeedNames[i] = bundleList.get(i).getFeedName();
				mAllBundleFeedUrls[i] = bundleList.get(i).getFeedUrl();
			}
		} else {
			Log.d(TAG, "bundleList from flex is empty!");
		}
//		String []rss = getResources().getStringArray(R.array.bundle_rss);
//		int j=0;
//		mAllBundleFeedNames = new String[rss.length/2];
//		mAllBundleFeedUrls = new String[rss.length/2];
//		for(int i=0;i<rss.length;i+=2){
//			mAllBundleFeedNames[j] = rss[i];
//			mAllBundleFeedUrls[j] = rss[i+1];
//			
//			j++;
//		}
		mAddedFeedList  = this.getIntent().getParcelableArrayListExtra(KEY_ADDED_FEED_LIST);
		mAppWidgetId = this.getIntent().getIntExtra(KEY_WIDGET_ID, -1);
		if(mAllBundleFeedNames != null && mAllBundleFeedUrls != null) {
			mBundleAdapter = new BundleAdapter(this, mAllBundleFeedNames);
			mBundleListSettings.setAdapter(mBundleAdapter);
			mBundleListSettings.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			for(int i = 0; i < mAllBundleFeedUrls.length; i ++){
				boolean checked = isChecked(mAllBundleFeedUrls[i]);
				mBundleListSettings.setItemChecked(i, checked);
			}
		}		
	}

	private boolean isChecked(String url) {
		FeedInfo info = null;
		for (int i = 0; i < mAddedFeedList.size(); i++) {
			info = mAddedFeedList.get(i);
			if (info != null) {
				if (info.feedUrl != null) {
					if (info.feedUrl.equals(url)) {
						mAddedIsBundleFeedList.add(info);
						return true;
					}
				}
			}
		}
		return false;
	}
	class ViewHolder {
		TextView bundleName;
		ImageView bundleImg;
	}
	private class BundleAdapter extends ArrayAdapter<String>{
		private LayoutInflater mInflater;
		public BundleAdapter(Context context, String[] list) {
			super(context, 0,list);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView == null){
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.bundleitem, null);
				holder.bundleName = (TextView) convertView.findViewById(R.id.bundle_name);
				holder.bundleImg = (ImageView) convertView.findViewById(R.id.bundle_img);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			holder.bundleName.setText(mAllBundleFeedNames[position]);
			boolean checked = mBundleListSettings.isItemChecked(position);
			if(checked){
				holder.bundleImg.setImageResource(R.drawable.cheakbox_on);
			}else{
				holder.bundleImg.setImageResource(R.drawable.cheakbox_off);
			}
			return convertView;
		}
	}
	
	public void deleteFeed(int feedId){
		Log.d(TAG,"deletemAppWidgetId = "+mAppWidgetId);
		Log.d(TAG,"feedId = "+feedId);
		mDataChanged = true;
		mDatabaseHelper.markFeedDeleted(mAppWidgetId, feedId);
		mDatabaseHelper.markItemDeleted(mAppWidgetId, feedId);
		Log.d(TAG,"delete Feed begin");
		Intent deleteFeedIntent = new Intent();
		deleteFeedIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_DELETE_FEED);
		deleteFeedIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		deleteFeedIntent.putExtra(KEY_FEED_ID, feedId);
		startService(deleteFeedIntent);		
	}
	
	private void notifyWidgetOrDetailToUpdate(){
		Intent in = null;
	    in = new Intent(RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED);
		Log.d(TAG, "mAppWidgetId = " + mAppWidgetId);
		in.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		in.putExtra("autoUpdate", false);
		sendBroadcast(in);
	}
	
	public void onSubscribe(View view) {
		SparseBooleanArray array = mBundleListSettings.getCheckedItemPositions();
		int size = mBundleListSettings.getCount();
		ArrayList<FeedInfo> currentBundleFeedInfoList = new ArrayList<FeedInfo>();
		ArrayList<String> currentFeedUrlList = new ArrayList<String>();
		ArrayList<FeedInfo> deletedBundleFeedList = new ArrayList<FeedInfo>();
		FeedInfo feedInfo = null;
		for (int i = 0; i < size; i++) {
			if (array.get(i)) {
				feedInfo = new FeedInfo();
				feedInfo.feedTitle = mAllBundleFeedNames[i];
				feedInfo.feedUrl=mAllBundleFeedUrls[i];
				feedInfo.feedIsBundle=RssConstant.State.STATE_BUNDLE;
				currentBundleFeedInfoList.add(feedInfo);
				currentFeedUrlList.add(feedInfo.feedUrl);
				Log.d(TAG,"currentBundleFeedUrlList ="+ mAddedFeedList.size());
				Log.d(TAG,"mAddedFeedList ="+ mAddedFeedList.size());
				
			}
		}
		if (mAddedIsBundleFeedList != null) {
			for (int j = 0; j < mAddedIsBundleFeedList.size(); j++) {
				FeedInfo addedIsBundleFeedInfo = mAddedIsBundleFeedList.get(j);
				if (addedIsBundleFeedInfo != null) {
						if (!currentFeedUrlList.contains(addedIsBundleFeedInfo.feedUrl)) {
							deleteFeed(mAddedFeedList.get(j).feedId);
							deletedBundleFeedList.add(addedIsBundleFeedInfo);
//							notifyWidgetOrDetailToUpdate();
							SharedPreferences preference = getSharedPreferences(RssConstant.Content.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
							preference.edit().putInt(RssConstant.Key.KEY_WIDGET_ID, mAppWidgetId).commit();
						}
				}
			}
		}
		Log.d(TAG,"deletedBundleFeedList = "+deletedBundleFeedList.size());
		Intent intent = new Intent();
		Log.d(TAG, "DataChanged = " + mDataChanged);
		intent.putExtra(KEY_DATA_CHANGE, mDataChanged);
		intent.putParcelableArrayListExtra(KEY_FEED_BUNDLELIST, currentBundleFeedInfoList);
		intent.putParcelableArrayListExtra(KEY_DELETED_FEED_BUNDLE_LIST, deletedBundleFeedList);
		this.setResult(RssConstant.Flag.FEEDBUNDLELIST_RESULECODE, intent);
		finish();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		mBundleAdapter.notifyDataSetChanged();
	}
}
