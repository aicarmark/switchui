package com.motorola.mmsp.weather.locationwidget.small;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class NewsManager {
	public static class NewsInfo {
		public int mNewsId;
		public int mNewsPosition;
		public String mNewsTitle;
		public String mNewsContent;
		public String mNewsTime;
		public String mNewsTopic;
	}

	private static final String LOG_TAG = GlobalDef.LOG_TAG;
	private Context mContext = null;
	private String[] mFeeds = null;
	private String mRssServiceOrigin = null;
	private boolean mIsRssServiceConnectNetwork = false;
	private HashMap<Integer, ArrayList<NewsInfo>> mNewsMap;

	public NewsManager(Context context) {
		mContext = context;
		mNewsMap = new HashMap<Integer, ArrayList<NewsInfo>>();
	}

	public void setFeeds(String feeds) {
		GlobalDef.MyLog.d(LOG_TAG, "feeds from rss : " + feeds);
		if (feeds != null && !feeds.equals("")) {
			mFeeds = feeds.split(",", 10);
			for (String temp : mFeeds)
				GlobalDef.MyLog.d(LOG_TAG, "current mFeeds contain: " + temp);
		} else {
			mFeeds = null;
			GlobalDef.MyLog.d(LOG_TAG, "invalid mFeeds");
		}
	}

	public void setOrigin(String origin) {
		if (origin != null && !origin.equals(mRssServiceOrigin)) {
			mRssServiceOrigin = origin;
			GlobalDef.MyLog.d(LOG_TAG, "mRssServiceOrigin: " + mRssServiceOrigin);
		}
	}

	public String getOrigin() {
		return mRssServiceOrigin;
	}

	public void setNewsList(int widgetId, ArrayList<NewsInfo> list) {
		mNewsMap.put(widgetId, list);
	}

	public void deleteNewsList(int widgetId) {
		if (mNewsMap.containsKey(widgetId)) {
			mNewsMap.remove(widgetId);
		}
	}

	public ArrayList<NewsInfo> getNewsList(int widgetId, String cityName, int requestNewsCount) {
		if (mNewsMap.containsKey(widgetId)) {
			return mNewsMap.get(widgetId);
		} else {
			return createNewsList(widgetId, cityName, requestNewsCount, true);
		}
	}

	/** If the rss setting contain the specific topic. */
	public boolean isTopicActive(String topic) {
		if (mFeeds == null || topic == null) {
			return false;
		}
		int n = mFeeds.length;
		for (int i = 0; i < n; i++) {
			if (topic.equals(mFeeds[i]))
				return true;
		}
		return false;
	}

	public boolean isNewsEmpty() {
		return mNewsMap.isEmpty();
	}

	public boolean isNetWorkConnecting() {
		return mIsRssServiceConnectNetwork;
	}

	public void startConnect() {
		mIsRssServiceConnectNetwork = true;
	}

	public void finishConnect() {
		mIsRssServiceConnectNetwork = false;
	}

	public void clearAllInfo() {
		mNewsMap.clear();
	}

	private ArrayList<NewsInfo> createNewsList(int widgetId, String cityName, int requestNewsCount, boolean isBaseOnFeed) {
		if (isBaseOnFeed) {
			return createNewsListBaseOnFeed(widgetId, cityName, requestNewsCount);
		} else {
			return createNewsListBaseOnTime(widgetId, cityName, requestNewsCount);
		}
	}

	private ArrayList<NewsInfo> createNewsListBaseOnFeed(int widgetId, String cityName, int requestNewsCount) {
		if (mFeeds != null) {
			ArrayList<NewsInfo> list = new ArrayList<NewsInfo>();
			NewsInfo info = null;
			// query database and set list
			GlobalDef.MyLog.w(LOG_TAG, "database operation");
			Uri newsUri = Uri.parse("content://com.motorola.mmsp.rssnews/news");
			Cursor cur = null;
			int newsCount = 0;
			int index = 0;
			int position = 0;
			int length = mFeeds.length;
			int[] failtag = new int[length];
			// for(int temp:failtag)
			// GlobalDef.MyLog.d(LOG_TAG,"temp="+temp);
			
			try {
				while (newsCount < requestNewsCount) {
					cur = null;
					if (failtag[index] == 0) {
						String[] category = { mFeeds[index] };
						if (mFeeds[index].equals("l")) {
							GlobalDef.MyLog.d(LOG_TAG, "mFeeds:" + mFeeds[index]);
							if (cityName != null) {
								cur = GlobalDef
										.query(mContext, newsUri, "city = ? ", new String[] { cityName }, "_id DESC");
							} else {
								GlobalDef.MyLog.d(LOG_TAG, "city is emtry");
								failtag[index] = 1;
							}
						} else {
							GlobalDef.MyLog.d(LOG_TAG, "mFeeds:" + mFeeds[index]);
							cur = GlobalDef.query(mContext, newsUri, "topic = ?", category, "_id DESC");
						}
						if (cur != null) {
							GlobalDef.MyLog.d(LOG_TAG, "cur.getCount:" + cur.getCount());
							if (cur.moveToPosition(position)) {
								int id;
								String title = null;
								String content = null;
								String updateTime = null;
								String topic = null;
								id = cur.getInt(cur.getColumnIndex("_id"));
								topic = cur.getString(cur.getColumnIndex("topic"));
								title = cur.getString(cur.getColumnIndex("title"));
								title = GlobalDef.reverseFilter(title);
								content = cur.getString(cur.getColumnIndex("content"));
								content = HtmlTagFilter.removeHtmlTag(content);
								content = GlobalDef.reverseFilter(content);
								updateTime = cur.getString(cur.getColumnIndex("pubDate"));
								Date updateDate = new Date(updateTime);
								SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
								String formatTime = dateFormat.format(updateDate);
								GlobalDef.MyLog.d(LOG_TAG, "_id=" + id + " topic=" + topic + " title=" + title);
								info = new NewsInfo();
								info.mNewsId = id;
								info.mNewsPosition = position;
								info.mNewsTopic = topic;
								info.mNewsTitle = title;
								info.mNewsContent = content;
								info.mNewsTime = formatTime;
								list.add(info);
								newsCount++;
							} else {
								failtag[index] = 1;
							}
						} else {
							failtag[index] = 1;
							GlobalDef.MyLog.d(LOG_TAG, "cur == null , mFeeds:" + mFeeds[index] + "  failtag = 1 ");
						}
					} else {
						GlobalDef.MyLog.d(LOG_TAG, "mFeeds:" + mFeeds[index] + "  failtag = 1 not need to query");
					}
					GlobalDef.MyLog.d(LOG_TAG, "position = " + position + " failtag = " + failtag[index]);
					index++;
					if (index >= length) {
						int sum = 0;
						for (int i = 0; i < length; i++) {
							sum += failtag[i];
						}
						if (sum >= length) {// no news
							GlobalDef.MyLog.d(LOG_TAG, "sum >= length");
							break;
						}
						position++;
						index = 0;
					}
				}
			}
	        catch(Exception e){
	        	e.printStackTrace();
	        }
	        finally{
		        // close Cursor
		        if( cur != null )
		        cur.close();
	        }
			
			mNewsMap.put(widgetId, list);
			return list;
		}
		return null;
	}

	private ArrayList<NewsInfo> createNewsListBaseOnTime(int widgetId, String cityName, int requestNewsCount) {
		if (mFeeds != null) {
			ArrayList<NewsInfo> list = new ArrayList<NewsInfo>();
			NewsInfo info = null;
			GlobalDef.MyLog.w(LOG_TAG, "database operation");
			String[] topics = new String[requestNewsCount];
			Uri newsUri = Uri.parse("content://com.motorola.mmsp.rssnews/news");
			String selection = getNewsQuerySelection(cityName);
			GlobalDef.MyLog.d(LOG_TAG, "selection:" + selection);
			long start = System.currentTimeMillis();
			Cursor cur = null;
			try {
				cur = GlobalDef.query(mContext, newsUri, selection, null, "_id DESC limit 10");
				long time_use = System.currentTimeMillis() - start;
				GlobalDef.MyLog.d(LOG_TAG, "query time_use:" + time_use);
	
				int newsCount = 0;
				if (cur != null) {
					GlobalDef.MyLog.d(LOG_TAG, "cur.getCount()=" + cur.getCount());
					if (cur.moveToFirst()) {
						do {
							String city = cur.getString(cur.getColumnIndex("city"));
							if (city != null) {
								if (!city.equals(cityName)) {
									GlobalDef.MyLog.d(LOG_TAG, "city=" + city + " abandoned");
									continue;
								}
							}
							int id;
							int position = 0;
							String title = null;
							String content = null;
							String updateTime = null;
							String topic = null;
							id = cur.getInt(cur.getColumnIndex("_id"));
							topic = cur.getString(cur.getColumnIndex("topic"));
							if (topic == null) {
								topic = "l";
							}
							position = getNewsPositionOnFeed(topics, topic);
							topics[newsCount] = topic;
							title = cur.getString(cur.getColumnIndex("title"));
							title = GlobalDef.reverseFilter(title);
							content = cur.getString(cur.getColumnIndex("content"));
							content = HtmlTagFilter.removeHtmlTag(content);
							content = GlobalDef.reverseFilter(content);
							updateTime = cur.getString(cur.getColumnIndex("pubDate"));
							Date updateDate = new Date(updateTime);
							SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
							String formatTime = dateFormat.format(updateDate);
							GlobalDef.MyLog.d(LOG_TAG, "_id=" + id + " topic=" + topic + " title=" + title);
							info = new NewsInfo();
							info.mNewsId = id;
							info.mNewsPosition = position;
							info.mNewsTopic = topic;
							info.mNewsTitle = title;
							info.mNewsContent = content;
							info.mNewsTime = formatTime;
							list.add(info);
							newsCount++;
						} while (newsCount < requestNewsCount && cur.moveToNext());
					}
				}
			}
	        catch(Exception e){
	        	e.printStackTrace();
	        }
	        finally{
		        // close Cursor
		        if( cur != null )
		        {
					cur.close();
					cur = null;
				}
	        }
			mNewsMap.put(widgetId, list);
			return list;
		}
		return null;
	}

	/** if cityName is null ,it will query all citys */
	private String getNewsQuerySelection(String cityName) {
		if (mFeeds == null)
			return null;
		String selection = "";
		String prefix = "";
		for (int i = 0; i < mFeeds.length; i++) {
			if (mFeeds[i].equals("l")) {
				if (cityName != null) {
					selection = selection + prefix + "city ='" + cityName + "'";
					prefix = " or ";
				} else {
					selection = selection + prefix + "city !='null'";
					prefix = " or ";
				}
			} else {
				selection = selection + prefix + "topic = '" + mFeeds[i] + "'";
				prefix = " or ";
			}

		}
		return "(" + selection + ")";
	}

	private int getNewsPositionOnFeed(String[] topics, String topic) {
		if (topic == null)
			return 0;
		int n = topics.length;
		int position = 0;
		for (int i = 0; i < n; i++) {
			if (topic.equals(topics[i]))
				position++;
		}
		return position;
	}
}