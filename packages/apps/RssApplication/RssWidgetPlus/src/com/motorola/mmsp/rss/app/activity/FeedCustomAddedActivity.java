package com.motorola.mmsp.rss.app.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils.TruncateAt;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.RssConstant;

public class FeedCustomAddedActivity extends RssAppBaseActivity implements OnItemClickListener, OnClickListener, TextWatcher{
	private ListView mHistoryFeed = null;
	private HistoryListAdapter mHistoryListAdapter = null;
	private List<FeedInfo> mHistoryFeedList = null;
	private static final String TAG = "FeedCustomAddedActivity";
    private Button mCustomFeedNestBtn = null;
    private static final String KEY_FEED_CUSTOMINFO = "feedCustomInfo";
    private static final String KEY_FEED_URL = "feedUrl";
    private EditText mCustomUrlText = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.feed_custom_added);
//		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.setting_relative_title);
		LayoutInflater inflater = LayoutInflater.from(this);
		ActionBar actionBar = getActionBar();
		View view = inflater.inflate(R.layout.setting_relative_title, null);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setDisplayShowHomeEnabled(false);
		actionBar.setCustomView(view);
		TextView titleView = (TextView)view.findViewById(R.id.setting_relative_title);
		titleView.setText(R.string.customfeed);
		view = inflater.inflate(R.layout.feed_custom_added_header, null);
		mCustomUrlText = (EditText)view.findViewById(R.id.setfeedurl);
		mCustomUrlText.addTextChangedListener(this);
		mCustomUrlText.requestFocus();
		mCustomFeedNestBtn = (Button) findViewById(R.id.customnext);
		mCustomFeedNestBtn.setOnClickListener(this);
		mCustomFeedNestBtn.setEnabled(false);
		mCustomFeedNestBtn.setTextColor(Color.GRAY);
		mHistoryFeed=(ListView)findViewById(R.id.historyfeedlist);
		mHistoryFeedList = new ArrayList<FeedInfo>();
		mHistoryListAdapter = new HistoryListAdapter(this);
		mHistoryFeed.addHeaderView(view, null, false);
		mHistoryFeed.setAdapter(mHistoryListAdapter);
		mHistoryFeed.setOnItemClickListener(this);
		Cursor feedHistoryCursor = null;
		try {
			feedHistoryCursor = getContentResolver().query(RssConstant.Content.HISTORY_URI, null, "", null, "");
			if(feedHistoryCursor != null && feedHistoryCursor.getCount() > 0) {
				feedHistoryCursor.moveToFirst();
				if(!feedHistoryCursor.isAfterLast()) {
					do {
						FeedInfo info = new FeedInfo();
						info.feedTitle = feedHistoryCursor.getString(feedHistoryCursor
												.getColumnIndex(RssConstant.Content.HISTORY_TITLE));
						Log.d(TAG,"historyurl="+feedHistoryCursor
										.getString(feedHistoryCursor
												.getColumnIndex(RssConstant.Content.HISTORY_URL)));
						info.feedUrl = feedHistoryCursor.getString(feedHistoryCursor
								.getColumnIndex(RssConstant.Content.HISTORY_URL));
						mHistoryFeedList.add(info);
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
	}

	private class HistoryListAdapter extends ArrayAdapter<FeedInfo> {
		private LayoutInflater mInflater;
		public HistoryListAdapter(Context context) {
			super(context, 0, mHistoryFeedList);
			mInflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			TextView view = null;
			if (convertView == null) {
				view = (TextView) mInflater.inflate(
						android.R.layout.simple_list_item_1, null);
			} else {
				view = (TextView) convertView;
			}
			view.setSingleLine();
			view.setEllipsize(TruncateAt.END);
			FeedInfo info = mHistoryFeedList.get(position);
			view.setText(info.feedUrl);
			return view;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		switch (resultCode) {
		case RssConstant.Flag.FEEDCUSTOMCONFIRM_RESULECODE:
			FeedInfo feedCustomInfo = null;
			Bundle feedCustomBundle = (Bundle) data.getExtras();
			if(feedCustomBundle != null){
				feedCustomInfo = (FeedInfo) feedCustomBundle.getParcelable(KEY_FEED_CUSTOMINFO);
			}
			if(feedCustomInfo != null){
				Intent intent = new Intent();
				intent.putExtra(KEY_FEED_CUSTOMINFO, feedCustomInfo);
				this.setResult(RssConstant.Flag.FEEDCUSTOMADDED_RESULECODE, intent);
			}
			finish();
			break;
		default:
			break;
		}
	}

	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		FeedInfo info = mHistoryFeedList.get(arg2-1);
		Bundle feedUrlBundle = new Bundle();
		Intent intent = new Intent();
		feedUrlBundle.putString(KEY_FEED_URL, info.feedUrl);
		intent.putExtras(feedUrlBundle);
		intent.setClass(FeedCustomAddedActivity.this,FeedCustomConfirmActivity.class);
		startActivityForResult(intent, RssConstant.Flag.FEED_REQUESTCODE);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		final View view =  v;
		int id = view.getId();
		switch(id){
		case R.id.customnext:
			Bundle feedUrlBundle = new Bundle();
			Intent intent = new Intent();
			String feedUrl = null;
			if(mCustomUrlText != null){
				feedUrl = mCustomUrlText.getText().toString();
			}
			if(feedUrl != null){
				feedUrl = feedUrl.trim();
			}
			feedUrlBundle.putString(KEY_FEED_URL, feedUrl);
			intent.putExtras(feedUrlBundle);
			intent.setClass(FeedCustomAddedActivity.this,FeedCustomConfirmActivity.class);
			startActivityForResult(intent, RssConstant.Flag.FEED_REQUESTCODE);
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if(mCustomUrlText != null){
			Editable editable = mCustomUrlText.getText();
			int len = 0;
			if(editable != null){
				len = editable.length();
			}
			mCustomFeedNestBtn.setEnabled (len == 0 ? false : true);			
		}else{
			mCustomFeedNestBtn.setEnabled (false);			
		}
		boolean enable = mCustomFeedNestBtn.isEnabled();
		mCustomFeedNestBtn.setTextColor( enable ? Color.WHITE : Color.GRAY);
	}
}
