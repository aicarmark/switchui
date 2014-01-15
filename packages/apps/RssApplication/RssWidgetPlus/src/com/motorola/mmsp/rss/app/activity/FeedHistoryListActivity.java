package com.motorola.mmsp.rss.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.RssConstant;

public class FeedHistoryListActivity extends RssAppBaseActivity implements OnItemClickListener {
	private ListView mFeedHistorySettings = null;
	private List<FeedInfo> mFeedHistoryList = new ArrayList<FeedInfo>();
	private HistoryAdapter mHistoryAdapter = null;
//	private HistoryAdapter mHistoryAdapter = null;
	private static final String TAG = "FeedHistoryListActivity";
    private static final String KEY_FEED_HISTORYLIST = "historyFeedList";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG,"onCreate begin");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.feedhistorylist);		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.setting_relative_title);
		TextView titleView = (TextView) getWindow().findViewById(R.id.setting_relative_title);
		titleView.setText(R.string.addbyhistorytitle);
		mFeedHistorySettings = (ListView) findViewById(R.id.feedhistorylist);
		/*2012-11-13, add by amt_jiayuanyuan for SWITCHUITWOV-3056 */
		mFeedHistorySettings.setOnItemClickListener(this);
		/*2012-11-13, add end*/
		Cursor feedHistoryCursor = null;
		try {
			feedHistoryCursor = getContentResolver().query(RssConstant.Content.HISTORY_URI, null, "", null, "");
			if(feedHistoryCursor != null && feedHistoryCursor.getCount() > 0) {
				feedHistoryCursor.moveToFirst();
				if(!feedHistoryCursor.isAfterLast()) {
					do {
						FeedInfo feedInfo = new FeedInfo();
						feedInfo.feedTitle = feedHistoryCursor.getString(feedHistoryCursor.getColumnIndex(RssConstant.Content.HISTORY_TITLE));
						feedInfo.feedUrl = feedHistoryCursor.getString(feedHistoryCursor.getColumnIndex(RssConstant.Content.HISTORY_URL));
						mFeedHistoryList.add(feedInfo);
					} while (feedHistoryCursor.moveToNext());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(feedHistoryCursor != null){
				feedHistoryCursor.close();
			}
		}
		mHistoryAdapter = new HistoryAdapter(this, mFeedHistoryList);
		mFeedHistorySettings.setAdapter(mHistoryAdapter);
		mFeedHistorySettings.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
	}
	class ViewHolder {
		TextView historyName;
		ImageView historyImg;
	}
	public class HistoryAdapter extends ArrayAdapter<FeedInfo>{
		private LayoutInflater mInflater;
		public HistoryAdapter(Context context,List<FeedInfo> list) {
			super(context, 0,list);
			mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView == null){
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.historyitem, null);
				holder.historyName = (TextView) convertView.findViewById(R.id.history_name);
				holder.historyImg = (ImageView) convertView.findViewById(R.id.history_img);
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder) convertView.getTag();
			}
			String feedTitle = mFeedHistoryList.get(position).feedTitle;
			String feedUrl = mFeedHistoryList.get(position).feedUrl;
			if(feedTitle != null){
				holder.historyName.setText(feedTitle);
			}else{
				holder.historyName.setText(feedUrl);	
			}
			boolean checked = mFeedHistorySettings.isItemChecked(position);
			if(checked){
				holder.historyImg.setImageResource(R.drawable.cheakbox_on);
			}else{
				holder.historyImg.setImageResource(R.drawable.cheakbox_off);
			}
			return convertView;
		}
	}

	public void onSubscribe(View view) {
		SparseBooleanArray array = mFeedHistorySettings
				.getCheckedItemPositions();
		int size = mFeedHistorySettings.getCount();
		ArrayList<FeedInfo> historyFeedInfoList = new ArrayList<FeedInfo>();
		FeedInfo feedInfo = null;
		for (int i = 0; i < size; i++) {
			if (array.get(i)) {
				feedInfo = new FeedInfo();
				feedInfo.feedTitle = mFeedHistoryList.get(i).feedTitle;
				feedInfo.feedUrl = mFeedHistoryList.get(i).feedUrl;
				historyFeedInfoList.add(feedInfo);
			}
		}
		Intent intent = new Intent();
		intent.putParcelableArrayListExtra(KEY_FEED_HISTORYLIST,historyFeedInfoList);
		this.setResult(RssConstant.Flag.FEEDHISTORYLIST_RESULECODE, intent);
		finish();
	}
	/*2012-11-13, add by amt_jiayuanyuan for SWITCHUITWOV-3056 */
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub
		mHistoryAdapter.notifyDataSetChanged();
	}
	/*2012-11-13, add end*/
}
