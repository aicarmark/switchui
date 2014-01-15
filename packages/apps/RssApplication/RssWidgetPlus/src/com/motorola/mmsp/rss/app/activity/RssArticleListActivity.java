package com.motorola.mmsp.rss.app.activity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.DatabaseHelper;
import com.motorola.mmsp.rss.common.IndexInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.common.ViewItems;

public class RssArticleListActivity extends ListActivity implements OnItemClickListener, OnScrollListener{
	private static final String TAG = "RssArticleListActivity";
//	private static final String WIDGET_ID = "widgetId";
	private static final String KEY_GALLERY_POS = "gallery_pos";
	private static final String KEY_LIST_IDS = "list_ids";
	private static final String KEY_UNREAD_COUNT = "unread_count";
	private static final String KEY_WIDGET_TITLE = "widget_title";
	
	private static final int MSG_CLEAR_LIST = 0;
	private static final int MSG_ADD_ITEM_COMPLETE = 1;
	private static final int MSG_ADD_ITEM = 2;
	private static final int MSG_SHOW_LOADING_DIALOG = 3;
	private static final int MSG_DISMISS_LOADING_DIALOG = 4;
	private static final int MSG_LOADINIG_NEW_DATA = 5;
	private static final int MSG_SHOW_ITEM_STATE = 6;
	private static final int MSG_SET_TITLE = 7;
	private static final int MSG_LOAD_TITLE = 8;
	
	private static final int CACHE_SIZE = 20;
	
	private boolean mLoadComplete = false;
	private int mLastSize = 0;
	
	private Handler mHandler = null;
	private TextView mListTitle = null;
	private TextView mUnreadCount = null;
	private ArticleListAdapter mArticleListAdapter = null;
	private DatabaseHelper mDatabaseHelper = null;
	private ProgressDialog mProcessDialog = null;
	private List<ViewItems> mListItems = null;
	private ArticleListCacheThread mArticleListCacheThread;
	private List<Integer> mListIds;
	private List<ViewItems> mAddedItem;
	private boolean mLoadingData = false;
	private boolean mBackToDetail = false;
	
	class ArticleListHandler extends Handler{
		private Context mContext;
		public ArticleListHandler(Context context){
			mContext = context;
		}
		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			switch(what){
			case MSG_CLEAR_LIST:
				mListItems.clear();
				break;
			case MSG_ADD_ITEM_COMPLETE:
				Log.d(TAG, "mLoadComplete = " + mLoadComplete + " , SIZE = " + mAddedItem.size());
				for(int i=0; i<mAddedItem.size(); i++){
					ViewItems item = mAddedItem.get(i);
					if(item != null){
						mArticleListAdapter.add(item);
					}
				}
				
				int thisSize = mListItems.size();
				if(mLastSize >= thisSize){
					mLoadComplete = true;
				}
				Log.d(TAG, "thisSize = " + thisSize + " , lastSize = " + mLastSize);
				mLastSize = mListItems.size();
//				mArticleListAdapter = new ArticleListAdapter(mContext, mListItems);
//				getListView().setAdapter(mArticleListAdapter);
				
				View emptyView = getListView().getEmptyView();
				((TextView)emptyView).setText(R.string.empty_article_list);
				break;
			case MSG_ADD_ITEM:
//				ViewItems item = fillViewItems(msg.arg1);
				ViewItems item = (ViewItems) msg.obj;
				if(item != null){
					mArticleListAdapter.add(item);
				}
				break;
			case MSG_SHOW_LOADING_DIALOG:
				mProcessDialog = ProgressDialog.show(mContext, null, getResources().getString(R.string.listloadingtips), false, true);
				mProcessDialog.setCanceledOnTouchOutside(false);
				mProcessDialog.setCancelable(false);
				break;
			case MSG_DISMISS_LOADING_DIALOG:
				if(mProcessDialog != null && mProcessDialog.isShowing()){
					try {
						mProcessDialog.dismiss();
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				TextView empty = (TextView) getListView().getEmptyView();
				empty.setText(R.string.empty_article_list);
				break;
			case MSG_LOADINIG_NEW_DATA:
				if(!mBackToDetail) {
					int loadPos = msg.arg1;
					mArticleListCacheThread = new ArticleListCacheThread(mContext, loadPos);
					mArticleListCacheThread.start();
				}							
				break;
			case MSG_SHOW_ITEM_STATE:
				break;			
			case MSG_SET_TITLE:
				int unReadCount = msg.arg1;
				String widgetTitle = (String) msg.obj;
				widgetTitle = widgetTitle == null ? getResources().getString(R.string.rss) : widgetTitle;
				String title = widgetTitle;
				mListTitle.setText(title); 
				mUnreadCount.setText("(" + unReadCount + ")");
				break;
			default:
				break;
			}
		}
		
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE); 		
		setContentView(R.layout.rssarticle_list);		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.rssarticle_list_title);
		mListTitle = (TextView) findViewById(R.id.list_title);
		mUnreadCount = (TextView) findViewById(R.id.unreadcount);
		mDatabaseHelper = DatabaseHelper.getInstance(this);
		mHandler = new ArticleListHandler(this);
		
//		View emptyView = getListView().getEmptyView();
//		((TextView)emptyView).setText(R.string.loading);
		Intent newIntent = getIntent();
		int unReadCount = 0;
		String widgetTitle = null;
		if(newIntent != null){
			mListIds = newIntent.getIntegerArrayListExtra(KEY_LIST_IDS);
			Log.d(TAG, "list.size = " + mListIds.size());
			unReadCount = newIntent.getIntExtra(KEY_UNREAD_COUNT, 0);
			widgetTitle = newIntent.getStringExtra(KEY_WIDGET_TITLE);
			Message msg = mHandler.obtainMessage(MSG_SET_TITLE);
			msg.arg1 = unReadCount;
			msg.obj = widgetTitle;
			mHandler.sendMessage(msg);
		}
		mAddedItem = new ArrayList<ViewItems>();
		mListItems = new ArrayList<ViewItems>();
		mArticleListAdapter = new ArticleListAdapter(this, mListItems);
		getListView().setAdapter(mArticleListAdapter);
		getListView().setOnItemClickListener(this);
		getListView().setOnScrollListener(this);		
		if(mListIds != null){
			Message msg = mHandler.obtainMessage(MSG_LOADINIG_NEW_DATA);
			msg.arg1 = 0;
			mHandler.sendMessage(msg);
		}
	}
	
	@Override
	protected void onResume() {		
		super.onResume();
		increaseActivityCount();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		decreaseActivityCount();
		if(mProcessDialog != null && mProcessDialog.isShowing()){
			try {
				mProcessDialog.dismiss();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	@Override
	protected void onStop() {
		checkRssApplicationIsExit();
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		mBackToDetail = true;
		joinCacheThread();
		super.onBackPressed();
	}
	
	private void joinCacheThread() {
		if (mArticleListCacheThread != null && mArticleListCacheThread.isAlive()) {
			try {
				mArticleListCacheThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void increaseActivityCount() {
		int count = DatabaseHelper.getInstance(this).getActivityCount();
		count++;
		DatabaseHelper.getInstance(this).updateActivityCount(count);
	}
	
	private void decreaseActivityCount() {
		int count = DatabaseHelper.getInstance(this).getActivityCount();
		count--;
		DatabaseHelper.getInstance(this).updateActivityCount(count);
	}
	
	private void checkRssApplicationIsExit() {
		int count = DatabaseHelper.getInstance(this).getActivityCount();
		if(count <= 0) {
			startService(new Intent(RssConstant.Intent.INTENT_RSSAPP_EXITED));
		}
	}
	
	private ViewItems fillViewItems(int _id){
		Cursor c = null;
		ViewItems item = null;
		String selection = RssConstant.Content._ID + "=" + _id;
		String projection[] = new String[]{
				RssConstant.Content.FEED_TITLE, RssConstant.Content.ITEM_TITLE, 
				RssConstant.Content.ITEM_PUBDATE, RssConstant.Content.ITEM_STATE};
		try{
			c = mDatabaseHelper.query(RssConstant.Content.VIEW_ITEM_URI, projection, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					item = new ViewItems();
					item.feedTitle = c.getString(c.getColumnIndex(RssConstant.Content.FEED_TITLE));
					item.itemTitle = c.getString(c.getColumnIndex(RssConstant.Content.ITEM_TITLE));
					item.itemPubdate = c.getLong(c.getColumnIndex(RssConstant.Content.ITEM_PUBDATE));
					item.itemState = c.getInt(c.getColumnIndex(RssConstant.Content.ITEM_STATE));
//					Log.d(TAG, "ID = " + item._id + " , TITLE = " + item.itemTitle);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		return item;
		
	}
	
	class ArticleListCacheThread extends Thread{
		private int mLoadPosition = -1;
		public ArticleListCacheThread(Context context, int loadPos){
			mLoadPosition = loadPos;
		}
		
		public void run(){
			mHandler.sendEmptyMessage(MSG_SHOW_LOADING_DIALOG);
			
			int listSize = mListIds.size();
			int cacheSize = listSize - mLoadPosition;
			Log.d(TAG, "cacheSize1 = " + cacheSize);
			cacheSize = cacheSize >= CACHE_SIZE ? CACHE_SIZE : cacheSize;
			Log.d(TAG, "cacheSize2 = " + cacheSize);
			ViewItems item = null;
			mAddedItem.clear();
			for(int i = mLoadPosition;i < mLoadPosition + cacheSize;i++){
				int _id = mListIds.get(i);
				item = fillViewItems(_id);
				/**
				Message msg = mHandler.obtainMessage(MSG_ADD_ITEM);
				msg.obj = item;
				msg.arg1 = _id;
				mHandler.sendMessage(msg);
				*/
				mAddedItem.add(item);
			}
			mLoadingData = false;
			mHandler.sendEmptyMessage(MSG_ADD_ITEM_COMPLETE);
			mHandler.sendEmptyMessage(MSG_DISMISS_LOADING_DIALOG);
		}
	}
	
	class ItemAdder implements Runnable{
		private int mIndexId = -1;
		public ItemAdder(int _id){
			mIndexId = _id;
		}
		public void run(){
			ViewItems item = fillViewItems(mIndexId);
			mArticleListAdapter.add(item);
		}
	}

	class ViewHolder {
		TextView itemTitle;
		TextView feedTitle;
		TextView itemPubDate;
		LinearLayout dateTitleLayout;
		View dateTitleDivider;
		TextView dateTitle;
	}
	class ArticleListAdapter extends ArrayAdapter<ViewItems>{
		private Context mContext;
		private LayoutInflater mInflater = null;
		private SectionManager mSectionManager;
		public ArticleListAdapter(Context context, List<ViewItems> list) {
			super(context, 0, list);
			mSectionManager = new SectionManager();
			mContext = context;
			mInflater = (LayoutInflater)mContext.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
		} 

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder = null;
			ViewItems item = null;
			if(convertView == null){
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.rssarticle_list_item, null);
				holder.itemTitle = (TextView)convertView.findViewById(R.id.article_title);
				holder.feedTitle = (TextView)convertView.findViewById(R.id.article_author);
				holder.itemPubDate = (TextView)convertView.findViewById(R.id.article_pubdate);
				
				holder.dateTitleLayout = (LinearLayout)convertView.findViewById(R.id.datetitle_layout);
				holder.dateTitleDivider = convertView.findViewById(R.id.datetitle_divider);
				holder.dateTitle = (TextView)convertView.findViewById(R.id.datetitle);
				
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder)convertView.getTag();
			}
			item = getItem(position);
			if(item != null){
				String itemTitle = item.itemTitle;
				String feedTitle = item.feedTitle;
				int itemState = item.itemState;
				long itemPubdate = item.itemPubdate;
				
				SimpleDateFormat sdf = new SimpleDateFormat ("yyyy/MM/dd");
				String dateTitle = sdf.format(new Date(itemPubdate));
				holder.dateTitle.setText(dateTitle);		
				holder.itemTitle.setText(itemTitle);
				holder.feedTitle.setText(feedTitle);			
				long currentTime = 0;
				long thisTime = 0;
				Date today = new Date();
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");		
				try {
					currentTime = formatter.parse(formatter.format(today)).getTime();
					thisTime = formatter.parse(formatter.format(new Date(itemPubdate))).getTime();
				} catch (ParseException e) {
					e.printStackTrace();
				}
				if(currentTime == thisTime){
					sdf = new SimpleDateFormat ("MM/dd HH:mm");
					holder.itemPubDate.setText(sdf.format(new Date(itemPubdate)));
				}else{
					sdf = new SimpleDateFormat ("yyyy/MM/dd");
					holder.itemPubDate.setText(sdf.format(new Date(itemPubdate)));
				}
				SectionDetail sd = mSectionManager.get(position);
				if (sd == null) {
					sd = new SectionDetail(position, thisTime, itemState);
					mSectionManager.put(position, sd);
				}
				if (sd.getShowable()) {
					holder.dateTitleLayout.setVisibility(View.VISIBLE);
				}else{
					holder.dateTitleLayout.setVisibility(View.GONE);
				}
				if(position == 0){
					holder.dateTitleDivider.setVisibility(View.GONE);
				}else{
					holder.dateTitleDivider.setVisibility(View.VISIBLE);
				}
				if(itemState == RssConstant.State.STATE_ITEM_READ){
					holder.itemTitle.setTextColor(mContext.getResources().getColor(R.color.light_grey));
					holder.feedTitle.setTextColor(mContext.getResources().getColor(R.color.light_grey));
					holder.itemPubDate.setTextColor(mContext.getResources().getColor(R.color.light_grey));
				}else{
					holder.itemTitle.setTextColor(mContext.getResources().getColor(R.color.white));
					holder.feedTitle.setTextColor(mContext.getResources().getColor(R.color.white));
					holder.itemPubDate.setTextColor(mContext.getResources().getColor(R.color.white));
				}
			}
			return convertView;
		}

	}
	public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
		startToArticleDetail(position);
		mBackToDetail = true;
		joinCacheThread();
		finish();
	}
	
	private void startToArticleDetail(int position) {
		ViewItems item = mArticleListAdapter.getItem(position); 
		Intent intent = new Intent();
		intent.putExtra(RssConstant.Content._ID, item._id);
		intent.putExtra(RssConstant.Key.KEY_WIDGET_ID, item.widgetId);
		intent.putExtra(RssConstant.Key.KEY_ITEM_ID, item.itemId);
		intent.putExtra(RssConstant.Content.ITEM_URL, item.itemUrl);
		intent.putExtra(KEY_GALLERY_POS, position);
		intent.setAction(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL_FROMLIST);		
		startActivity(intent);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.d(TAG, "onConfigurationChange");
		super.onConfigurationChanged(newConfig);
	}

	class SectionManager {
		private HashMap<String, SectionDetail> mSectionHash = null;	
		public SectionManager(){
			mSectionHash = new HashMap<String, SectionDetail>();
		}
				
		public boolean showSelection(int position){
			if(position == 0){
				return true;
			}
			return false;
		}
		
		public SectionDetail get(int position){
			return mSectionHash.get(String.valueOf(position));
		}
		
		public void put(int position, SectionDetail sd){
			if(position == 0){
				sd.setShowable(true);
				mSectionHash.put(String.valueOf(position), sd);
				return ;
			}
			
			Set<Entry<String, SectionDetail>> set = mSectionHash.entrySet();
			Iterator<Entry<String, SectionDetail>> iterator = set.iterator();
			SectionDetail sdl = null;
			boolean timeExist = false;
			while(iterator.hasNext()){
				Entry<String, SectionDetail> entry =  iterator.next();
//				String key = entry.getKey();
//				Log.d(TAG, "key = " + key);
				sdl = entry.getValue();
				if((sdl.getDateTime() == sd.getDateTime()) /*&& (sdl.getState() == sd.getState())*/){
					timeExist = true;
					break;
				}
			}
			if(!timeExist){
				sd.setShowable(true);
				mSectionHash.put(String.valueOf(position), sd);
			}else{
				sd.setShowable(false);	
			}
		}
	}
	
	class SectionDetail {
		private int mPosition;
		private boolean mShowable;
		private long mDateTime;
		private int mItemState;
		public SectionDetail(int position, long time, int state){
			mDateTime = time;
			mPosition = position;
			mItemState = state;
		}
		public boolean getShowable(){
			return mShowable;				
		}
		public long getDateTime(){
			return mDateTime;
		}
		public int getPosition(){
			return mPosition;
		}
		public int getState(){
			return mItemState;
		}
		public void setShowable(boolean show){
			mShowable = show;
		}
		public void setDateTime(long time){
			mDateTime = time;
		}
		public void setPosition(int position){
			mPosition = position;
		}
		public void setState(int state){
			mItemState = state;
		}
	}
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		
	}
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		int lastVisibleItem = getListView().getLastVisiblePosition();
		int totalItemCount = getListView().getCount();
		if(scrollState == SCROLL_STATE_IDLE){
			if(lastVisibleItem > 0 && !mLoadComplete && !mLoadingData){
				if(lastVisibleItem == totalItemCount - 1){
					Log.d(TAG, "mBackPressed is " + mBackToDetail);
					mLoadingData = true;
					Message msg = mHandler.obtainMessage(MSG_LOADINIG_NEW_DATA);
					msg.arg1 = mArticleListAdapter.getCount();	
					Log.d(TAG, "Has Added = " + msg.arg1);
					mHandler.sendMessage(msg);
		
				}
			}
		}
	}
}
