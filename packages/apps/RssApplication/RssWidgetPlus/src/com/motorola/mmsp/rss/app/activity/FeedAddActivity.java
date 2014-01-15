package com.motorola.mmsp.rss.app.activity;

import java.util.ArrayList;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TwoLineListItem;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.RssConstant;
public class FeedAddActivity extends RssAppBaseActivity implements OnItemClickListener {
	public static final int KEY_FEED_BUNDLE = 0;
	public static final int KEY_FEED_CUSTOM = 1;
	public static final int KEY_FEED_ADDHISTORY=2; 
	private ListView mAddFeedSettings = null;
	private SettingListAdapter mAddFeedAdapter = null;
	private String mAddFeedWays[];
	private String mAddFeedummary[];
	public static final int REQUESTCODE_VALUE = 0;
    private static final String KEY_FEED_CUSTOMINFO = "feedCustomInfo";
    private static final String KEY_FEED_BUNDLELIST = "bundleFeedList";
    private static final String KEY_FEED_HISTORYLIST = "historyFeedList";
    private static final String KEY_DELETED_FEED_BUNDLE_LIST = "deletedbundleFeedList";
    private static final String KEY_WIDGET_ID = "widgetId";
    private static final String TAG = "FeedAddActivity";
    private static final String KEY_ADDED_FEED_LIST = "addedFeedList";
    private static final String KEY_DATA_CHANGE = "data_change";
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.feedadd);		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.setting_relative_title);
		TextView titleView = (TextView) getWindow().findViewById(R.id.setting_relative_title);
		titleView.setText(R.string.addfeed_prefenrence);
//		setTitle(this.getResources().getString(R.string.addfeedtitle));
		mAddFeedWays = this.getResources().getStringArray(R.array.add_feed_ways);
		mAddFeedummary = this.getResources().getStringArray(R.array.feed_summary);
		mAddFeedSettings = (ListView) findViewById(R.id.addfeedlist);
		mAddFeedAdapter = new SettingListAdapter(this);
		mAddFeedSettings.setAdapter(mAddFeedAdapter);
		mAddFeedSettings.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Intent intent = null;
		switch(arg2){
		case KEY_FEED_BUNDLE:
			intent = new Intent(this, FeedBundleListActivity.class);
			mAppWidgetId = this.getIntent().getIntExtra(KEY_WIDGET_ID, -1);
			ArrayList<FeedInfo> addedFeedList = this.getIntent().getParcelableArrayListExtra(KEY_ADDED_FEED_LIST);
			intent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
			intent.putParcelableArrayListExtra(KEY_ADDED_FEED_LIST, addedFeedList);
			startActivityForResult(intent, REQUESTCODE_VALUE);
			break;
		case KEY_FEED_CUSTOM:
			intent = new Intent(this, FeedCustomAddedActivity.class);
			startActivityForResult(intent, REQUESTCODE_VALUE);
			break;
		case KEY_FEED_ADDHISTORY:
			intent =new Intent(this,FeedHistoryListActivity.class);
			startActivityForResult(intent, REQUESTCODE_VALUE);
			break;
		default:
			break;
		}
	}

	private class SettingListAdapter extends ArrayAdapter<String> {
		private LayoutInflater mInflater;
		public SettingListAdapter(Context context) {
			super(context, 0, mAddFeedWays);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TwoLineListItem view = null;
			if (convertView == null) {
				view = (TwoLineListItem) mInflater.inflate(
						android.R.layout.simple_list_item_2, null);
			} else {
				view = (TwoLineListItem) convertView;
			}
			view.getText1().setTextColor(Color.WHITE);
			view.getText1().setText(mAddFeedWays[position]);
			view.getText2().setTextColor(Color.WHITE);
			view.getText2().setText(mAddFeedummary[position]);
			return view;
		}
	}
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
		    case RssConstant.Flag.FEEDCUSTOMADDED_RESULECODE:
				Bundle feedCustomBundle = (Bundle) data.getExtras();
				FeedInfo feedCustomInfo = null;
				if(feedCustomBundle != null){
				feedCustomInfo = (FeedInfo) feedCustomBundle.getParcelable(KEY_FEED_CUSTOMINFO);
				}
				Intent intent = new Intent();
				if(feedCustomInfo != null){
				intent.putExtra(KEY_FEED_CUSTOMINFO, feedCustomInfo);
				}
				this.setResult(RssConstant.Flag.FEEDADDBYCUSTOM_RESULECODE, intent);
				finish();
			    break;
			case RssConstant.Flag.FEEDBUNDLELIST_RESULECODE:
				ArrayList<FeedInfo> bundleFeedList = data.getParcelableArrayListExtra(KEY_FEED_BUNDLELIST);
				ArrayList<FeedInfo> deletedBundleFeedList = data.getParcelableArrayListExtra(KEY_DELETED_FEED_BUNDLE_LIST);
				boolean datachanged = data.getBooleanExtra(KEY_DATA_CHANGE, false);
				Intent bundleIntent = new Intent();
				if(bundleFeedList != null){
					bundleIntent.putExtra(KEY_FEED_BUNDLELIST,bundleFeedList);
				}
				if(deletedBundleFeedList != null){
					bundleIntent.putExtra(KEY_DELETED_FEED_BUNDLE_LIST,deletedBundleFeedList);
				}
				bundleIntent.putExtra(KEY_DATA_CHANGE, datachanged);
				this.setResult(RssConstant.Flag.FEEDADDBYBUNDLE_RESULECODE, bundleIntent);
				finish();
				break;
			case RssConstant.Flag.FEEDHISTORYLIST_RESULECODE:
				ArrayList<FeedInfo> historyFeedList=data.getParcelableArrayListExtra(KEY_FEED_HISTORYLIST);
				if(historyFeedList != null){
				Intent historyintent = new Intent();
				historyintent.putExtra(KEY_FEED_HISTORYLIST,historyFeedList);
				this.setResult(RssConstant.Flag.FEEDADDBYHISTORY_RESULECODE, historyintent);
				}
				finish();
				break;
		   default:
			    break;
		}
	}
}
