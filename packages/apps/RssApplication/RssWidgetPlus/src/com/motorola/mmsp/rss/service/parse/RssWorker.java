package com.motorola.mmsp.rss.service.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.DatabaseHelper;
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.IndexInfo;
import com.motorola.mmsp.rss.common.ItemInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.service.parse.IconLoader.IconLoadFinishListener;
import com.motorola.mmsp.rss.util.NetworkUtil;

public class RssWorker implements Runnable, IconLoadFinishListener {

	private String TAG = "RSSWorker";
	private static final String KEY_CHECK_URL_RESULT = "key_check_url_result";
	private static final String KEY_WIDGET_ID = "widgetId";
	private static final String KEY_AUTO_UPDATE = "autoUpdate";
	private static final String KEY_STATE = "state";//check add news or update news
	private static final String KEY_SUBSCRIBED = "subscribed";
	private static final String KEY_NETWORK = "network";
	private static final int STATE_ADD_NEWS = 0;
	private static final int STATE_UPDATE_NEWS = 1;
	private static final int STATE_CHECK_URL = 2;
	private static final int MESSAGE_WAIT_ICON_LOAD_FINISH = 3;
	private static final int MESSAGE_WAIT_ICON_LOAD_TIMEOUT = 4;
	private static final int ICON_LOAD_WAIT = 500;
	private static final int ICON_LOAD_TIMEOUT = 5 * 1000;
	private Context mContext = null;
	
	private List<?> mFeedInfos ;
	private ArrayList<FeedInfo> mRemainFeedInfos;
	public static ConcurrentHashMap<Integer, ArrayList<FeedInfo>> autoRefreshRemainInfo;

	private List<FeedInfo> mFeedRetryInfos; //add by wdmk68 for SWITCHUI-1767
	
	private boolean mCanceled = false;
	private boolean mAutoUpdate = false;
//	private boolean mNetworkAvailable = false;
	private int mAppWidgetId;
	private DatabaseHelper mDatabaseHelper = null;
	
	private int mFlag = 0;	
	private String mNeedCheckUrl;
	private ArrayList<Integer> mIconLoaderList;
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_WAIT_ICON_LOAD_FINISH:
				if(mIconLoaderList.isEmpty()) {
					addRssComplete();
				} else {
					mHandler.sendEmptyMessageDelayed(MESSAGE_WAIT_ICON_LOAD_FINISH, ICON_LOAD_WAIT);
				}
				break;
			case MESSAGE_WAIT_ICON_LOAD_TIMEOUT:
				Log.d(TAG, "load feed icon timeout");
				if(mHandler.hasMessages(MESSAGE_WAIT_ICON_LOAD_FINISH)) {
					mHandler.removeMessages(MESSAGE_WAIT_ICON_LOAD_FINISH);
				}
				addRssComplete();
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
		
	};
	
	private int mTotalUpdate = 0;
	public RssWorker(Context context, String url, int flag){
		mContext = context;
		mFlag = flag;
		mNeedCheckUrl = url;
		mCanceled = false;
		mIconLoaderList = new ArrayList<Integer>();
	}
	public RssWorker(Context context, int widgetId, List<FeedInfo> infos, int flag, boolean autoUpdate){
		mContext = context;
		mFeedInfos = infos;
		mFlag = flag;
		mAutoUpdate = autoUpdate;
		mDatabaseHelper = DatabaseHelper.getInstance(context);
		mAppWidgetId = widgetId;
		mIconLoaderList = new ArrayList<Integer>();
		if(mRemainFeedInfos == null) {
			mRemainFeedInfos = new ArrayList<FeedInfo>();
		}
		if(autoRefreshRemainInfo == null) {
			autoRefreshRemainInfo = new ConcurrentHashMap<Integer, ArrayList<FeedInfo>>();
		}
		/**
		if(infos.size() != 0) {
			mAppWidgetId = infos.get(0).widgetId;
			Log.d(TAG, "mAppWidgetId is " + mAppWidgetId);
		}
		*/
		Log.d(TAG, "flag is " + flag + " , autoUpdate is " + autoUpdate);
		mCanceled = false;
	}
	
	private boolean updateRss(FeedInfo info){
		if(info == null){
			Log.d(TAG, "arguement is null in insertRSS");
			return false;
		}
		String url = info.feedUrl;
		List<ItemInfo> infos = new ArrayList<ItemInfo>();
		RssXmlParser parser = RssXmlParser.getInstance(mContext);
		int result = parser.parse(url, info, infos, true); //Modify by wdmk68: corrected date
		if(result == RssConstant.State.STATE_SUCCESS){
			updateRssNews(info, infos);
			infos.clear();
			infos = null;
			return true;
		} else if(result == RssConstant.State.STATE_CONNECTY_FAILURE 
				|| result == RssConstant.State.STATE_NETWORK_NOT_AVAILABLE
				|| result == RssConstant.State.STATE_URL_NOT_AVAILABLE) {
//			mRemainFeedInfos.add(info);
			if(mAutoUpdate) {
				mRemainFeedInfos.add(info);
			}
		} else{
			Log.d(TAG, "Parse XML < " + url + " >file failure !");
		}
		infos.clear();
		infos = null;
		return false;
	}
	private boolean addRss(FeedInfo info){
		if(info == null){
			Log.d(TAG, "arguement is null in insertRSS");
			return false;
		}
		if(mDatabaseHelper.feedAdded(info)){
			Log.d(TAG, "feed <" + info.feedUrl + "> has added !");
			return false;
		}
		String url = info.feedUrl;
		List<ItemInfo> infos = new ArrayList<ItemInfo>();
		RssXmlParser parser = RssXmlParser.getInstance(mContext);
		int result = parser.parse(url, info, infos, false);//wdmk68: not correct date, retry
		if(result == RssConstant.State.STATE_SUCCESS || result == RssConstant.State.STATE_HAS_INVALID_DATA){
			mTotalUpdate += infos.size();
			addRssNews(info, infos);
			addHistoryFeed(info);
			//Add by wdmk68
			if (result == RssConstant.State.STATE_HAS_INVALID_DATA){
				Log.d(TAG, "result == RssConstant.State.STATE_HAS_INVALID_DATA");
				addRetryFeed(info);
			}
			infos.clear();
			infos = null;
			return true;
		}else if(result == RssConstant.State.STATE_NETWORK_NOT_AVAILABLE){
			info.feedGuid = String.valueOf(info.feedUrl.hashCode());
			Log.d(TAG, "Feed : <" + info.feedUrl + "> is being added to db");
			mDatabaseHelper.addFeed(info);
			if(info.feedId != -1){
				IndexInfo feedIndex = new IndexInfo();
				feedIndex.widgetId = info.widgetId;
				feedIndex.feedId = info.feedId;
				mDatabaseHelper.addFeedIndex(feedIndex);
			}
			addHistoryFeed(info);
		}
		infos.clear();
		infos = null;
		return false;
	}
	
	private int checkRssAdress(String url){
		RssXmlParser parser = RssXmlParser.getInstance(mContext);
		Log.d(TAG, "Checked url is " + url);
		if(url == null){
			return RssConstant.State.STATE_URL_NOT_AVAILABLE;
		}
		return parser.preParseRss(url);
	}
	
	private void sendNetworkNotAvailabelBroadcast(int state) {
		Intent intent = new Intent(RssConstant.Intent.INTENT_NETWORK_NOTAVAILABLE);
		intent.putExtra(KEY_STATE, state);
		mContext.sendBroadcast(intent);
	}
	
	public synchronized void run(){
		mTotalUpdate = 0;
		int i = 0;
		int size = 0;
		FeedInfo info = null;
		if(mFlag == RssConstant.Flag.ADD_RSS_NEWS){
			if(!NetworkUtil.isNetworkAvailable(mContext)) {
				sendNetworkNotAvailabelBroadcast(STATE_ADD_NEWS);
				addRssComplete();
				return;
			}
			if(mFeedInfos != null){
				size = mFeedInfos.size();
				while(i<size){
					Log.d(TAG, "ADD_RSS_NEWS : mCanceled = " + mCanceled);
					if(mCanceled){
						break;
					}
					info = (FeedInfo) mFeedInfos.get(i);
					addRss(info);
					i++;
				}
				
				//Add by wdmk68 for SWITCHUI-1767
				if (mFeedRetryInfos != null && mFeedRetryInfos.size() > 0){
					size = mFeedRetryInfos.size();
					i = 0;
					while(i<size){
						if(mCanceled){
							break;
						}
						info = (FeedInfo) mFeedRetryInfos.get(i);
						Log.d(TAG, "retry load date error feed : info.feedUrl = " + info.feedUrl);
						updateRss(info);
						i++;
					}
					mFeedRetryInfos.clear();
					mFeedRetryInfos = null;
				} else {
					Log.d(TAG, "no need retry ");
				}
				//End add by wdmk68 for SWITCHUI-1767
				
				if(mIconLoaderList.isEmpty()) {
					addRssComplete();
				} else {
					Log.d(TAG, "please wait feed icon load");
					mHandler.sendEmptyMessageDelayed(MESSAGE_WAIT_ICON_LOAD_FINISH, ICON_LOAD_WAIT);
					mHandler.sendEmptyMessageDelayed(MESSAGE_WAIT_ICON_LOAD_TIMEOUT, ICON_LOAD_TIMEOUT);
				}				
			}
		}
		if(mFlag == RssConstant.Flag.UPDATE_RSS_NEWS){
			Log.d(TAG, "update rss news start");
			if(!NetworkUtil.isNetworkAvailable(mContext)) {
				sendNetworkNotAvailabelBroadcast(STATE_UPDATE_NEWS);
				updateRssComplete();
				return;
			}
			mRemainFeedInfos.clear();
			if(mFeedInfos != null){	
				i = 0;
				size = mFeedInfos.size();
				while(i<size){
					Log.d(TAG, "UPDATE_RSS_NEWS : mCanceled = " + mCanceled);
					if(mCanceled){
						break;
					}
					info = (FeedInfo) mFeedInfos.get(i);
					updateRss(info);
					i++;
				}
				updateRssComplete();
			}
		}
		if(mFlag == RssConstant.Flag.CHECK_RSS_ADDRESS){
			if(!NetworkUtil.isNetworkAvailable(mContext)) {
//				sendNetworkNotAvailabelBroadcast(STATE_CHECK_URL);
				
//				checkRssAddressComplete(false);	
				/*
				final String tip = mContext.getResources().getString(R.string.network_not_available);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(mContext, tip, Toast.LENGTH_SHORT).show();
					}
				});
				*/
				int state = RssConstant.State.STATE_NETWORK_NOT_AVAILABLE;
				checkRssAddressComplete(state);	
				return;
			}
			int state = checkRssAdress(mNeedCheckUrl);
			Log.d(TAG, "STATE = " + state);
			/*
			if(state == RssConstant.State.STATE_URL_NOT_AVAILABLE){
				final String tip = mContext.getResources().getString(R.string.invalid_url);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(mContext, tip, Toast.LENGTH_SHORT).show();
					}
				});
			}else if(state == RssConstant.State.STATE_NETWORK_NOT_AVAILABLE){
				final String tip = mContext.getResources().getString(R.string.network_not_available);
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(mContext, tip, Toast.LENGTH_SHORT).show();
					}
				});
			}
			boolean valiable = false;
			if(state == RssConstant.State.STATE_SUCCESS){
				valiable = true;
			}
			*/
			checkRssAddressComplete(state);			
		}		
	}
	public void stopRssUpdating(){
		mCanceled = true;
	}
	private void addRssComplete(){
		Log.d(TAG, "addRssComplete");
		boolean network = NetworkUtil.isNetworkAvailable(mContext);
		Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_SUBSCRIBE_FINISHED);
		intent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		intent.putExtra(KEY_AUTO_UPDATE, false);
		intent.putExtra(KEY_SUBSCRIBED, mTotalUpdate);
		intent.putExtra(KEY_NETWORK, network);
		mContext.sendBroadcast(intent);
		// Notify service that rss news has been added completely,make service remove the object of 
		// RssWorker in the hash table
		Intent updateIntent = new Intent();
		updateIntent.putExtra(KEY_NETWORK, network);
		updateIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED);
		updateIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		updateIntent.putExtra(KEY_SUBSCRIBED, mTotalUpdate);
		mContext.startService(updateIntent);
		mContext.sendBroadcast(updateIntent);
	}
	private void updateRssComplete(){
		Log.d(TAG, "mAppWidgetId is " + mAppWidgetId);
		boolean network = NetworkUtil.isNetworkAvailable(mContext);
		Intent updateIntent = new Intent();
		updateIntent.setAction(RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED);
		updateIntent.putExtra(KEY_AUTO_UPDATE, mAutoUpdate);
		updateIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		updateIntent.putExtra(KEY_SUBSCRIBED, mTotalUpdate);
		updateIntent.putExtra(KEY_NETWORK, network);
		Log.d(TAG, "mRemainFeedInfos size is " + mRemainFeedInfos.size());
//		if(!mRemainFeedInfos.isEmpty()) {
//			autoRefreshRemainInfo.put(mAppWidgetId, mRemainFeedInfos);
//		}
		if(mAutoUpdate){
			if(!mRemainFeedInfos.isEmpty()) {
				autoRefreshRemainInfo.put(mAppWidgetId, mRemainFeedInfos);
			}
			mContext.sendBroadcast(updateIntent);
			Log.d(TAG, "Auto Update updateRSSComplete");			
		}else{
			Log.d(TAG, "Manual Update updateRSSComplete");
			/*
			if(!mCanceled){
				mContext.sendBroadcast(updateIntent);
			}*/
			mContext.sendBroadcast(updateIntent);
		}
		mContext.startService(updateIntent);
	}
	private void checkRssAddressComplete(int state){
		Log.d(TAG, "checkRssAddressComplete is " + state);
		Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_CHECK_URL_COMPLETE);
		intent.putExtra(KEY_CHECK_URL_RESULT, state);
		mContext.sendBroadcast(intent);
	}
	private void addRssNews(FeedInfo info, List<ItemInfo> infos){
		info.feedGuid = String.valueOf(info.feedUrl.hashCode());
		Log.d(TAG, "Feed : <" + info.feedUrl + "> is being added to db");
		boolean success = mDatabaseHelper.addFeed(info);
		if(info.feedId != -1){
			IndexInfo feedIndex = new IndexInfo();
			feedIndex.widgetId = info.widgetId;
			feedIndex.feedId = info.feedId;
			mDatabaseHelper.addFeedIndex(feedIndex);
		}
		if(success){
			IconLoader loader = new IconLoader(mContext, info.feedId, info.feedIcon, null);
			loader.setLoadFinishListener(this);
			mIconLoaderList.add(info.feedId);
			loader.start();
		}
//		long initId = mDatabaseHelper.getNextItemId();
		int size = infos.size();
		int i = 0;
		while(i<size){
			IndexInfo index = new IndexInfo();
			ItemInfo item = infos.get(i);
//			item.itemId = initId;
			int itemId = mDatabaseHelper.addItem(item);
			if(!mDatabaseHelper.itemAdded(info.widgetId, info.feedId, itemId)){
				index.widgetId = info.widgetId;
				index.feedId = info.feedId;
				index.itemId = itemId;
				index.itemState = RssConstant.State.STATE_ITEM_UNREAD;
				Uri uri = mDatabaseHelper.addItemIndex(index);
				long id = ContentUris.parseId(uri);
				if(id == -1){
					mDatabaseHelper.deleteItem(info.widgetId, info.feedId, itemId);
					break;
				}
			}
			i++;
//			initId++;
		}
		
	}
	
	private void updateRssNews(FeedInfo info, List<ItemInfo> infos){
//		long initId = mDatabaseHelper.getNextItemId();
		int size = infos.size();
		int i = 0;
		while(i<size){
			IndexInfo itemIndex = new IndexInfo();
			ItemInfo item = infos.get(i);
//			item.itemId = initId;
			int itemId = mDatabaseHelper.updateItem(item);
			if(itemId != -1){
				if(!mDatabaseHelper.itemAdded(info.widgetId, info.feedId, itemId)){
					mTotalUpdate ++;
					itemIndex.widgetId = info.widgetId;
					itemIndex.feedId = info.feedId;
					itemIndex.itemId = itemId;
					itemIndex.itemState = RssConstant.State.STATE_ITEM_UNREAD;
					Uri uri = mDatabaseHelper.addItemIndex(itemIndex);
	//				Log.d(TAG, item.toString());
					long id = ContentUris.parseId(uri);
					if(id == -1){
						mDatabaseHelper.deleteItem(info.widgetId, info.feedId, itemId);
						break;
					}
//					initId++;
				}
			}					
			i++;
		}
	}
	
	//Add by wdmk68 for SWITCHUI-1767
	private void addRetryFeed(FeedInfo info){
		Log.d(TAG, "addRetryFeed info:" + info.toString());
		if (mFeedRetryInfos == null){
			mFeedRetryInfos = new ArrayList<FeedInfo>();
		}	
		
		if (mFeedRetryInfos != null){
			if (!mFeedRetryInfos.contains(info)){
				mFeedRetryInfos.add(info);
			}
		}
	}
	
	private void addHistoryFeed(FeedInfo info) {
		boolean isDumplicate = isFeedDumplicate(info);
		if(!isDumplicate) {
			mDatabaseHelper.deleteHistoryOutofLimit();
			int times = mDatabaseHelper.getHistoryTimes(info.feedUrl);
			if(times == 0){
				mDatabaseHelper.addHistoryFeedToDatabase(info.feedTitle, info.feedUrl, 1, info.feedPubdate);
			} else {
				mDatabaseHelper.updateHistoryFeed(info.feedUrl, times + 1);
			}
		}		
	}
	
	private boolean isFeedDumplicate(FeedInfo info) {
		if(info == null) {
			return true;
		}
		ArrayList<String> urlList = mDatabaseHelper.getHistoryUrl();
		ArrayList<String> newUrlList = new ArrayList<String>();
		for(int i = 0; i < urlList.size(); i++) {
			newUrlList.add(splitUrl(urlList.get(i)));
//			Log.d(TAG, "url is " + newUrlList.get(i));
		}
		if(newUrlList.contains(splitUrl(info.feedUrl))) {
			urlList.clear();
			urlList = null;
			newUrlList.clear();
			newUrlList = null;
			return true;
		}
		urlList.clear();
		urlList = null;
		newUrlList.clear();
		newUrlList = null;
		return false;
	}
	
	private String splitUrl(String url) {
		String[] urls = url.split("://");
		if(urls.length == 1) {
			return urls[0];
		} else {
			return urls[1];
		}
	}
	
	public int getUpdatingWidgetId(){
		return mAppWidgetId;
	}
	@Override
	public void IconLoadFinish(int feedId) {
		Log.d(TAG, "IconLoadFinish");
		if(mIconLoaderList.contains(feedId)) {
			mIconLoaderList.remove((Object)feedId);
		}
		Log.d(TAG, "mIconLoaderList size is " + mIconLoaderList.size());
	}
}
