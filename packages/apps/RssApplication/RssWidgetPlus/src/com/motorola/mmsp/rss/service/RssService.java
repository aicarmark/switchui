package com.motorola.mmsp.rss.service;

import java.lang.Thread.State;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.DatabaseHelper;
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.common.WidgetInfo;
import com.motorola.mmsp.rss.service.flex.RssFlexFactory;
import com.motorola.mmsp.rss.service.manager.RssAlarmManager;
import com.motorola.mmsp.rss.service.parse.RssWorker;
import com.motorola.mmsp.rss.util.MyThreadFactory;
import com.motorola.mmsp.rss.util.SettingsUtil;

public class RssService extends Service {
	static final int THREAD_POOL_THREAD_TIMEOUT = 60; 
	private static final String TAG = "RSSService";
	private static final long DAY_TO_MILLISECONDS = 3600 * 24 * 1000;
	private List<WidgetInfo> mWidgetInfoList = new ArrayList<WidgetInfo>();
	private HashMap<String, RssWorkerHolder> mHashWorker = null;
	private static final String KEY_WIDGET_ID = "widgetId";
	private static final String KEY_WIDGET_IDS = "widgetIds";
	private static final String KEY_FEED_INFOS = "feedInfos";
	private static final String KEY_AUTO_UPDATE = "autoUpdate";
	private static final String KEY_FEED_ID = "feedId";
	private static final String KEY_REMAIN_FEEDINFO = "remainFeedInfo";
	private static final String KEY_HAS_REMAIN_INFO = "hasRemainInfo";
	private static final String KEY_CHECK_URL_RESULT = "key_check_url_result";
	private static final String KEY_ID = "id";
	private DatabaseHelper mDatabaseHelper;
	private ExecutorService mThreadPool;
	private int numThreads = 9;
	private boolean mNeedDeleteOutofDateItems = false;
	
	private DatabaseObserver mDatabaseObserver;
	private final int MSG_NEW_INTENT_TO_HANDLE = 0;
	private final int MSG_DATABASE_CHANGE = 1;
	private final int MSG_START_ACTIVITY = 2;
	private final int START_ACTIVITY_DELAY = 2000;
	
	private AsncyQueryThreadComplete mAsncyQueryThreadComplete;
	private Handler mHandler;
	private static final int DELAY_QUERY = 30 * 1000;
	private RssNewsDeleteThread mRssNewsDeleteThread;
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
		mHashWorker = new HashMap<String, RssWorkerHolder>();
		mDatabaseHelper = DatabaseHelper.getInstance(this);
		HandlerThread thread = new HandlerThread("IntentService[" + mName + "]");
        thread.start();
        mThreadPool = new MyThreadPool(numThreads, THREAD_POOL_THREAD_TIMEOUT);
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        
        mDatabaseObserver = new DatabaseObserver(new Handler());
        getContentResolver().registerContentObserver(RssConstant.Content.ITEM_INDEX_URI, false, mDatabaseObserver);
        
        mHandler = new Handler();
	}

	@Override
    public void onStart(Intent intent, int startId) {
        Message msg = mServiceHandler.obtainMessage(MSG_NEW_INTENT_TO_HANDLE);
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }
	
	@Override
	public void onDestroy() {
		Log.d(TAG ,"onDestroy");
		getContentResolver().unregisterContentObserver(mDatabaseObserver);
		mServiceLooper.quit();
	
        try {
        	mThreadPool.shutdown();
			if (!mThreadPool.awaitTermination(5, TimeUnit.SECONDS)) { // 5 seconds should be enough time for stuff to finish up
			    Log.i(TAG,
			            "onDestroy() - we waited but our thread pool didn't shutdown nicely, forcing it now...");
			    List<Runnable> leftOvers = mThreadPool.shutdownNow();
			    Log.i(TAG,
			            "onDestroy() - after the force shutdown we have this many leftovers: "
			                    + leftOvers.size());
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "onDestroy() caught an exception - which is odd", e);
		}
        super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand called intent = " + intent);
		onStart(intent, startId);
		if(intent == null){
			Log.d(TAG ,"intent is null ,so stop the service itself");
			stopSelf();
			Intent i = new Intent(RssConstant.Intent.INTENT_RSSAPP_SERVICE_RESTART);
	        sendBroadcast(i);
		}
		return START_STICKY;
	}
	
	private boolean checkWidgetExsitState() {
		int widgetCount = mDatabaseHelper.getWidgetCount();
		if(widgetCount > 0) {
			return true;
		}
		return false;
	}
	
	private void markNewsOutofDate() {
		synchronized (mWidgetInfoList){ 
			mWidgetInfoList.clear();
			int result = mDatabaseHelper.getWidgetInfo(mWidgetInfoList);
			Log.d(TAG, "current widget size is " + mWidgetInfoList.size());
			if(result != 0) {
				for(WidgetInfo info : mWidgetInfoList) {
					mDatabaseHelper.markItemsOutofDate(info.widgetId, getValidityTime(info.widgetValidaty));
				}
			}
		}
	}
	
	private void notifyWidgetToUpdate() {
		if(!mWidgetInfoList.isEmpty()) {
			int[] widgetId = new int[mWidgetInfoList.size()];
			for(int i = 0; i < mWidgetInfoList.size(); i++) {
				widgetId[i] = mWidgetInfoList.get(i).widgetId;
			}
			Intent widgetUpdateIntent = new Intent();
			widgetUpdateIntent.setAction(RssConstant.Intent.INTENT_RSSWIDGET_UPDATE);
			widgetUpdateIntent.putExtra(KEY_WIDGET_IDS, widgetId);
			sendBroadcast(widgetUpdateIntent);
		}		
	}
	
	private void startTimerforWidget() {
		synchronized (mWidgetInfoList) {
			mDatabaseHelper.getWidgetInfo(mWidgetInfoList);
			if(!mWidgetInfoList.isEmpty()) {
				for(WidgetInfo info : mWidgetInfoList) {
					if(checkIsWidgetContainFeed(info.widgetId)) {
						RssAlarmManager.getInstance(this).addAlarm(info.widgetId, SettingsUtil.indexToUpdateFrequency(info.updateFrequency));
					}				
				}
			} else {
				Log.d(TAG, "widget is empty");
			}
		}		
	}
	
	/**
	 * @param validatyIndex: the index stored in database for validity time
	 * @return the value of validity time by mili_seconds
	 */
	private long getValidityTime(int validatyIndex) {
		long currentTime = 0;
		long validatyTime = 0;
		Date date = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");		
		try {
			currentTime = formatter.parse(formatter.format(date)).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Log.d(TAG, "current time is " + System.currentTimeMillis());
		validatyTime = currentTime + DAY_TO_MILLISECONDS -SettingsUtil.indexToShowItemFor(validatyIndex);
		Log.d(TAG, "validatyTime is " + validatyTime);
		return validatyTime;
	}
	
	private boolean stopRssWorkerIfNeed(int widgetId){
		RssWorkerHolder rssWorkerHolder = mHashWorker.get(String.valueOf(widgetId));
		if(rssWorkerHolder != null){
			if(rssWorkerHolder.rssWorker != null){
				Log.d(TAG, "The widget is updating, so place the 'beingDeleted' tag with true");
				rssWorkerHolder.rssWorker.stopRssUpdating();
				rssWorkerHolder.beingDeleted = true;
				return true;
			}
		}
		return false;
	}
	
	private void deleteTimerforWidget(int widgetId) {
		RssAlarmManager.getInstance(this).deleteAlarm(widgetId);
	}

	
	private boolean widgetUpdating(int widgetId){
		Log.d(TAG, "widgetUpdating = " + widgetId);
		String key = String.valueOf(widgetId);
		Log.d(TAG, "key = " + key);
		if(mHashWorker != null){
			return mHashWorker.containsKey(key);
		}
		return false;
	}
	private boolean removeFromHashTable(int widgetId){
		Log.d(TAG, "removeFromHashTable = " + widgetId);
		String key = String.valueOf(widgetId);
		if(mHashWorker.containsKey(key)){
			mHashWorker.remove(key);
		}
		return true;
	}
	
	private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;
    private String mName = "ServiceHandler";

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
        	int what = msg.what;
        	switch(what){
        	case MSG_NEW_INTENT_TO_HANDLE:
        		onHandleIntent((Intent)msg.obj);
        		break;
        	case MSG_DATABASE_CHANGE:
        		Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_NOTIFY_WIDGET_UPDATE);
        		sendBroadcast(intent);
        		
        		break;
        	default:
        		break;
        	}
        }
    }
	private void deleteWidget(int widgetId){
		mDatabaseHelper.markWidgetDeleted(widgetId);
		mDatabaseHelper.markAllFeedDeleted(widgetId);
		mDatabaseHelper.markAllItemDeleted(widgetId);
		startUpOrAddDeleteTask();
	}
	private void deleteFeed(int widgetId, int feedId){
		startUpOrAddDeleteTask();
	}
	
	private void deleteNewsOutofDate() {
//		markNewsOutofDate();
//		notifyWidgetToUpdate();
		Log.d(TAG, "mHashWorker size is " + mHashWorker.size());
		if(mHashWorker.size() == 0){
			markNewsOutofDate();
			notifyWidgetToUpdate();
			startUpOrAddDeleteTask();
		}else{
			Log.d(TAG, "Waiting for thread over ");
			mAsncyQueryThreadComplete = new AsncyQueryThreadComplete();
			mHandler.postDelayed(mAsncyQueryThreadComplete, DELAY_QUERY);
		}
	}
	private synchronized void startUpOrAddDeleteTask(){	
		Log.d(TAG, "startUpOrAddDeleteTask = " + mRssNewsDeleteThread);
		if(mRssNewsDeleteThread == null){
			mRssNewsDeleteThread = new RssNewsDeleteThread();
			mRssNewsDeleteThread.start();
		}else{
			boolean isAlive = mRssNewsDeleteThread.isAlive();
			Log.d(TAG ,"is Alive = " + isAlive);
			if(isAlive){
				mRssNewsDeleteThread.AddDeleteTask();
			}else{
				mRssNewsDeleteThread = new RssNewsDeleteThread();
				mRssNewsDeleteThread.start();
			}
		}
	}
    private void onHandleIntent(Intent intent){
    	if(intent != null) {
    		Log.d(TAG, "onHandleIntent() action = " + intent.getAction());
			String action = intent.getAction();
			if(RssConstant.Intent.INTENT_RSSSERVICE_BOOT_COMPLETED.equals(action)){
				Log.d(TAG, "boot completed, rss service started");
				startTimerforCheckNewsOutofDate();
				resetActivityCount();
				if(checkWidgetExsitState()) {
					deleteNewsOutofDate();
					mNeedDeleteOutofDateItems = false;
					startTimerforWidget();
				}
			} else if(RssConstant.Intent.INTENT_RSSSERVICE_VALIDITY_CHECKING.equals(action)) {
				int appActivityCount = getActivityCount();
				if(checkWidgetExsitState()) {
					if(appActivityCount == 0) {
						Log.d(TAG, "activity count is 0, you can delete news out of date");
						deleteNewsOutofDate();
						mNeedDeleteOutofDateItems = false;
					} else {
						Log.d(TAG, "activity count is not 0, you can not delete news out of date");
						mNeedDeleteOutofDateItems = true;
					}					
				}
				
			} else if (RssConstant.Intent.INTENT_RSSAPP_EXITED.equals(action)) {
				Log.d(TAG, "received rss app exited broadcast");
				if(mNeedDeleteOutofDateItems) {
					Log.d(TAG, "need delete news out of date, delete news");
					deleteNewsOutofDate();
					mNeedDeleteOutofDateItems = false;
				}
			} else if(RssConstant.Intent.INTENT_RSSSERVICE_STARTREFRESH.equals(action)) {
				
				Log.d(TAG, "refresh start");
				RssWorker rssWorker = null;
				ArrayList<FeedInfo> feedInfos = new ArrayList<FeedInfo>();
				boolean autoUpdate = intent.getBooleanExtra(KEY_AUTO_UPDATE, false);
				Log.d(TAG, "autoUpdate is " + autoUpdate);
				boolean hasRemainInfo = intent.getBooleanExtra(KEY_HAS_REMAIN_INFO, false);
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, 0);
				boolean updating = false;
				/** Whether there is a widget with the same widgetId updating or not**/
				updating = widgetUpdating(widgetId);
				if(!updating){
					Intent refreshIntent = new Intent();
					refreshIntent.setAction(RssConstant.Intent.INTENT_RSSWIDGET_REFRESH);
					Log.d(TAG,"widgetIdrefresh ="+widgetId);
					refreshIntent.putExtra(KEY_WIDGET_ID, widgetId);
					sendBroadcast(refreshIntent);
				}
				checkDatabase(widgetId);
				/*
				boolean success = checkDatabase(widgetId);
				if(!success){
					return ;
				}
				*/
				if(hasRemainInfo) {
					feedInfos = intent.getParcelableArrayListExtra(KEY_REMAIN_FEEDINFO);
				} else {
					mDatabaseHelper.getFeedsByWidgetId(widgetId, feedInfos);
				}				
				Log.d(TAG, "updating is " + updating);
				if(!updating){
					RssWorkerHolder rssWorkerHolder = new RssWorkerHolder();
					rssWorker = new RssWorker(this, widgetId, feedInfos, RssConstant.Flag.UPDATE_RSS_NEWS, autoUpdate);
					rssWorkerHolder.rssWorker = rssWorker;
					rssWorkerHolder.beingDeleted = false;
					mHashWorker.put(String.valueOf(widgetId), rssWorkerHolder);
					//rssWorker.start();
//					Log.d(TAG, "mRssNewsDeleteThread = " + mRssNewsDeleteThread + " , mRssNewsDeleteThread.isAlive = " + mRssNewsDeleteThread.isAlive());
					if(mRssNewsDeleteThread != null && mRssNewsDeleteThread.isAlive() && mRssNewsDeleteThread.getState() != State.WAITING){
						mDatabaseHelper.pauseDeleteOperation();
						Log.d(TAG, "mRssNewsDeleteThread is alive , so waiting for the complete of it");
						while(mRssNewsDeleteThread.getState() != State.WAITING){
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						State state = mRssNewsDeleteThread.getState();
						Log.d(TAG, "state = " + state);
						Log.d(TAG, "mRssNewsDeleteThread is waiting now ~~~~~~~~~~~~~~~~~~~~~~~");
					}
					
					_runTask(rssWorker);
					
				}
				
			} else if (RssConstant.Intent.INTENT_RSSSERVICE_ADDFEED.equals(action)) {
				
				Log.d(TAG, "feed add start");
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, 0);
				Log.d(TAG, "widgetId = " + widgetId);
				ArrayList<FeedInfo> feedInfos = new ArrayList<FeedInfo>();
				boolean autoUpdate = intent.getBooleanExtra(KEY_AUTO_UPDATE, false);
				Log.d(TAG, "autoUpdate is " + autoUpdate);
				feedInfos = intent.getParcelableArrayListExtra(KEY_FEED_INFOS);
				Log.d(TAG, "feedInfo is " + feedInfos);
				
				RssWorkerHolder rssWorkerHolder = new RssWorkerHolder();
				RssWorker rssWorker = new RssWorker(this, widgetId, feedInfos, RssConstant.Flag.ADD_RSS_NEWS, false);
				rssWorkerHolder.rssWorker = rssWorker;
				rssWorkerHolder.beingDeleted = false;
				mHashWorker.put(String.valueOf(widgetId), rssWorkerHolder);
				//rssWorker.start();
				if(mRssNewsDeleteThread != null && mRssNewsDeleteThread.isAlive() && mRssNewsDeleteThread.getState() != State.WAITING){
					mDatabaseHelper.pauseDeleteOperation();
					Log.d(TAG, "mRssNewsDeleteThread is alive , so waiting for the complete of it");
					while(mRssNewsDeleteThread.getState() != State.WAITING){
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					State state = mRssNewsDeleteThread.getState();
					Log.d(TAG, "state = " + state);
					Log.d(TAG, "mRssNewsDeleteThread is waiting now");
				}
				_runTask(rssWorker);
				
			} else if(RssConstant.Intent.INTENT_RSSWIDGET_DELETE.equals(action)) {
				
				Log.d(TAG, "delete a widget.");
				int[] widgetIds = intent.getIntArrayExtra(KEY_WIDGET_IDS);
				for(int i = 0; i < widgetIds.length; i++) {
					Log.d(TAG, "widgetId is " + widgetIds[i]);
					if(!stopRssWorkerIfNeed(widgetIds[i])){
						Log.d(TAG, "The widget you are deleting is not be updated by RssWorker ,so delete relative info about this widget");
						deleteTimerforWidget(widgetIds[i]);
						deleteWidget(widgetIds[i]);
					}else{
						Log.d(TAG, "The widget you are deleting is updating by now, so you cannot delete from database");
					}
				}
				
			} else if(RssConstant.Intent.INTENT_RSSAPP_UPDATE_ALARM.equals(action)) {
				
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, 0);
				int updateFrequency = intent.getIntExtra("updateFrequency", 0);
				RssAlarmManager.getInstance(this).updateAlarm(widgetId, SettingsUtil.indexToUpdateFrequency(updateFrequency));
				
			} else if(RssConstant.Intent.INTENT_RSSAPP_CHECK_URL.equals(action)){
				
				String url = intent.getStringExtra("key_check_url");
				Log.d(TAG, "check url = " + url);
				RssWorker rssWorker = new RssWorker(this, url, RssConstant.Flag.CHECK_RSS_ADDRESS);
				_runTask(rssWorker);
				
			} else if(RssConstant.Intent.INTENT_RSSAPP_ADD_ALARM.equals(action)) {
				
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, 0);
				int updateFrequency = intent.getIntExtra("updateFrequency", 0);
				RssAlarmManager.getInstance(this).addAlarm(widgetId, SettingsUtil.indexToUpdateFrequency(updateFrequency));
				
			} else if(RssConstant.Intent.INTENT_RSSAPP_DELETE_ALARM.equals(action)){
				Log.d(TAG, "receive alarm delete action, delete alarm");
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, 0);
				RssAlarmManager.getInstance(this).deleteAlarm(widgetId);
			} else if(RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED.equals(action)){
				
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				RssWorkerHolder rssWorkerHolder = mHashWorker.get(String.valueOf(widgetId));
				if(rssWorkerHolder != null){
					if(rssWorkerHolder.beingDeleted){
						Log.d(TAG, "The widget is updating while you want to delete it ,so waiting until updating complete!");
						deleteTimerforWidget(widgetId);
						deleteWidget(widgetId);
					}else{
						Log.d(TAG,"Notify application to update user interface !");
					}
				}else{
					Log.d(TAG,"Error");
				}
				removeFromHashTable(widgetId);
				
				//All updating is complete, so 
				int count = mHashWorker.size();
				Log.d(TAG, "The active thread count is " + count);
				if(count <= 0){
					if(mRssNewsDeleteThread != null && mRssNewsDeleteThread.getState() == State.WAITING){
						synchronized (DatabaseHelper.class) {
							Log.d(TAG, "Notify the RssDeleteThread to work agian !");
							DatabaseHelper.class.notify();
						}
					}
				}
				
			} else if(RssConstant.Intent.INTENT_RSSAPP_DELETE_FEED.equals(action)){
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				int feedId = intent.getIntExtra(KEY_FEED_ID, -1);
				deleteFeed(widgetId, feedId);
			} else if(RssConstant.Intent.INTENT_RSSAPP_STOP_UPDATING.equals(action)){
				Log.d(TAG, "com.motorola.mmsp.rss.intent.STOP_UPDATING");
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				RssWorkerHolder rssWorkerHolder = mHashWorker.get(String.valueOf(widgetId));
				if(rssWorkerHolder != null){
					RssWorker rssWorker = rssWorkerHolder.rssWorker;
					rssWorker.stopRssUpdating();
				}
			} else if(RssConstant.Intent.INTENT_RSSAPP_START_ARTICLE_ACTIVITY.equals(action)){
				if(!mHandler.hasMessages(MSG_START_ACTIVITY)){
					int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
					int id = intent.getIntExtra(KEY_ID, -1);
					Intent itemIntent = new Intent(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL);
					itemIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
					itemIntent.putExtra(KEY_WIDGET_ID, widgetId);
					itemIntent.putExtra(KEY_ID, id);
					startActivity(itemIntent);
					Log.d(TAG, "start Article Detail Activity");
					mHandler.sendEmptyMessageDelayed(MSG_START_ACTIVITY, START_ACTIVITY_DELAY);
				}
			}
		}
    }
    
    private void startTimerforCheckNewsOutofDate() {
    	RssAlarmManager.getInstance(this).addAlarmforNewsCheck();
    }
    private void _runTask(final Runnable task) {
        if (!mThreadPool.isShutdown()) {
            mThreadPool.execute(task);
        } else {
            Log.e(TAG, "_runTask(): trying to execute task after I've been killed: " + task);
        }
    }
    private static class MyThreadPool extends ThreadPoolExecutor {

        MyThreadPool(int maxPoolSize, int threadTimeOut) {
            super(maxPoolSize, maxPoolSize, threadTimeOut, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(), new MyThreadFactory(
                            "New-MyThreadPool", Process.THREAD_PRIORITY_BACKGROUND));
        }

        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (t != null) {
                // TODO, do something for real here...
         
            }
        }
    }	
    class RssWorkerHolder{
    	RssWorker rssWorker;
    	boolean beingDeleted;
    }
    
    private boolean checkIsWidgetContainFeed(int widgetId) {
		List<FeedInfo> list = new ArrayList<FeedInfo>();
		mDatabaseHelper.getFeedsByWidgetId(widgetId, list);
		if(list.isEmpty()){
			Log.d(TAG, "widget has no feed, widgetId is " + widgetId);
			return false;
		}
		return true;
	}
    
    private int getActivityCount() {
    	int count = mDatabaseHelper.getActivityCount();
    	return count;
    }
    
    private void resetActivityCount() {
    	mDatabaseHelper.resetActivityCount();
    }
	
    class RssNewsDeleteThread extends Thread{
    	volatile private boolean  mContinueDelete = false;
    	public RssNewsDeleteThread(){
    		mContinueDelete = true;
    	}
    	public void AddDeleteTask(){
    		Log.d(TAG, "Add a delete task ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
    		mContinueDelete = true;
    	}
		@Override
		public synchronized void run() {
			while(mContinueDelete){
				mContinueDelete = false;
				mDatabaseHelper.deleteDataMarkedDeleted();
			}
		}
    }
    private final String NEWTAG = "RssService";
    class DatabaseObserver extends ContentObserver{

		public DatabaseObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			mServiceHandler.sendEmptyMessage(MSG_DATABASE_CHANGE);
		}
    	
    }
    
    class AsncyQueryThreadComplete implements Runnable{

		@Override
		public void run() {
			if(mHashWorker.size() == 0){
				markNewsOutofDate();
				notifyWidgetToUpdate();
				startUpOrAddDeleteTask();
				return ;
			}
			Log.d(TAG, "The task of updating is not over ,  waiting 30s to try to do it again !");
			mHandler.postDelayed(mAsncyQueryThreadComplete, DELAY_QUERY);
		}
    	
    }
    private boolean checkDatabase(int widgetId){
		String selection = RssConstant.Content.WIDGET_ID + "=" + widgetId;
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
			exist = false;
		}finally{
			if(c != null){
				c.close();
			}
		}
		Log.d(TAG ,"exist = " + exist);
		if(!exist){
			ContentValues values = new ContentValues();
			values.put(RssConstant.Content.WIDGET_ID, widgetId);
			values.put(RssConstant.Content.WIDGET_TITLE, getResources().getString(R.string.rss));
			int defaultFrequencyIndex = RssFlexFactory.createFlexSettings().getUpdateFrequency(this);
			int defaultValidityIndex = RssFlexFactory.createFlexSettings().getNewsValidity(this);
			values.put(RssConstant.Content.WIDGET_UPDATE_FREQUENCY, defaultFrequencyIndex);
			values.put(RssConstant.Content.WIDGET_VALIDITY_TIME, defaultValidityIndex);
			getContentResolver().insert(RssConstant.Content.WIDGET_URI, values);
			
			Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_UPDATE_WIDGET_FOR_EXCEPTION);
			intent.putExtra(KEY_WIDGET_ID, widgetId);
			sendBroadcast(intent);
		}
		return exist;
	}
}
