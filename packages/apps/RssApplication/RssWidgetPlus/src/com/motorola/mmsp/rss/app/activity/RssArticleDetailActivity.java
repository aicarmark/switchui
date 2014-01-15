package com.motorola.mmsp.rss.app.activity;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.Window;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.DatabaseHelper;
import com.motorola.mmsp.rss.common.IndexInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.common.ViewItems;
import com.motorola.mmsp.rss.service.flex.RssFlexFactory;

public class RssArticleDetailActivity extends RssAppBaseActivity implements OnItemSelectedListener, OnItemClickListener, android.view.View.OnClickListener, OnCancelListener{
	private static final String TAG = "RssArticleDetailActivity";
	private static final int MENU_ITEM_REFRESH = 0;
	private static final int MENU_ITEM_SETTING = 1;
	private static final int MENU_ITEM_SHARE = 2;
	private static final int MENU_ITEM_ARTICLE_LIST = 3;
	
	private static final int MSG_CLEAR_LIST = 0;
	private static final int MSG_ADD_ITEM_COMPLETE = 1;
	private static final int MSG_ADD_ITEM = 2;
	private static final int MSG_SHOW_LOADING_DIALOG = 3;
	private static final int MSG_DISMISS_LOADING_DIALOG = 4;
	private static final int MSG_LOADINIG_NEW_DATA = 5;
	private static final int MSG_SHOW_ITEM_STATE = 6;
	private static final int MSG_SET_GALLERY_POS = 7;
	private static final int MSG_HIDE_BUTTON_LAYOUT = 8;
	private static final int MSG_CHANGE_ARTICLE = 9;
	
	private static final int DELAY_CHANGE_ARTICLE = 1000;
	private static final int DELAY_BUTTON_SHOW = 2000;
	
	private static final String KEY_WIDGET_ID = "widgetId";
//	private static final String KEY_ITEM_STATE = "itemState";
	private static final String KEY_ACTICLE_ID = "id";
	private static final String KEY_GALLERY_POS = "gallery_pos";
	private static final String KEY_LIST_IDS = "list_ids";
	private static final String KEY_UNREAD_COUNT = "unread_count";
	private static final String KEY_WIDGET_TITLE = "widget_title";
	private static final String KEY_CUR_PAGE = "current_page";
	private static final String KEY_DATA_CHANGE = "data_change";
	private static final String KEY_ONLY_WIDGET = "key_only_widget";
	
	private static final String KEY_FEED_SUBSCRIBING = "feed_subscribing";
	private static final String KEY_SUBSCRIBED = "subscribed";
	
	private static final int LANDSCAPE_WIDTH = 365;
	private static final int PORTRAIT_WIDTH = 218;
	private static final int PORTRAIT_HEIGHT = 375;
	private static final int PORTRAIT_HEIGHTSHORT = 339;
	private static final int LANDSCAPE_HEIGHT = 150;
	private static final int DEFAULT_HEIGHT = 800;
	
	private static final int DESCRIPTION_TEXT_LENGTH = 60;
	
	private static final int MAX_LINE_PORT = 7;
	private static final int MAX_LINE_LAND = 3;
	
	private static final String SHARED_NAME = "update-flags";
	
	private int mItemIndexId = 0;
	private int mGalleryPosition = 0;
	private int mTotalArticleCount= 0;
	private int mArticleUnRead = 0;
	protected ArrayAdapter<Integer> mArticleDetailAdapter = null;
	private DatabaseHelper mDatabaseHelper = null;
	private ProgressDialog mRefreshDialog;
	private RssArticleDetailReceiver mArticleDetailReceiver;
	private int mDisplayWidth = 0;
	private int mDisPlayHeight = 0;
	private boolean isCurrentFullScreen = true;
	
	private Gallery mGallery;
	private Button mArticleListButton;
	private ImageButton mViewOriginalButton;
	private Button mShareButton;
	private ImageButton mPrevButton;
	private ImageButton mNextButton;
	
	private TextView mDetailTitleView = null;
	private TextView mFullScreenTitleView = null;
	private ImageView mFullFeedIconView = null;
	private RelativeLayout mDetailTitleLayout = null;
	private RelativeLayout mFullScreenTitleLayout = null;
	private Bitmap mBitmap = null;

	private RelativeLayout mFullScreenLayout = null;
	private TextView mFullScreenDetailFeedTitle = null;
	private TextView mFullScreenDetailPubDate = null;
//	private ArticleDetailTextView mFullScreenDes = null;
	private WebView mFullScreenDes = null;
	private TextView mFullScreenNum = null;
//	private TextView mFullScreenEmptyDes = null;

	private TextView mArticleDetailNum = null;
	private List<ViewItems> mItemList = null;
	private int mAppWidgetId;
	private ArticleCacheThread mArticleCacheThread = null;
	private ArticleHandler mHandler;
	private ProgressDialog mProcessDialog = null;
	private HashMap<Integer, Bitmap>  mHashMap = null;
	private AlertDialog mShareDialog;
	private List<Integer> mListIds = null;
	private ViewItems mSelectedItem;
	private View mSplitLine;
	private String mWidgetTitle = null;
	private int mLastPage = 0;
	private boolean mBackToWidget = false;
//	private static final String TRACE_PATH = "/data/data/com.motorola.mmsp.rss/articledetailtrace.trace";
	
	private HashMap<Integer, ViewItems> mCachedHashMap = null;
	
	private final int CACHE_SIZE = 40;
	private FrameLayout mFullScreenDesTextZone = null;
	
	private int mTouchSlop = 0;
	private LinearLayout mButtonLayout = null;
	private View mHorizontalDivide = null;
	private boolean mEnterList = false;
	private TextView mNoArticleView = null;
	private int mUpdateItems;
	
	
	private static final int MODE_TAP = 0;
	private static final int MODE_MOVE = 1;
	private static final int MODE_LONG_PRESS = 2;
	private int mState = -1;
	private static final int MSG_LONG_CLICK_EVENT = 9;
	private int mLongTimeOut = 0;
	
	private boolean mActionMode = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);		
		setContentView(R.layout.rssarticle_detail);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.rssarticle_detail_title);
		
		mCachedHashMap = new HashMap<Integer, ViewItems>();
		mDatabaseHelper = DatabaseHelper.getInstance(this);
		initArticleUI();
		getDefaultDisplay();
		
		mListIds = new ArrayList<Integer>();
		mHashMap = new HashMap<Integer, Bitmap>();
		mItemList = new ArrayList<ViewItems>();
		mHandler = new ArticleHandler(this);
//		mHandler.sendEmptyMessage(MSG_LOADINIG_NEW_DATA);
		getNewIntent(getIntent());
		checkDatabase();
//		Debug.startMethodTracing(TRACE_PATH);
		registerReceiver();
	}

	private void loadWidgetTitle(){
		String selection = RssConstant.Content.WIDGET_ID + "=" + mAppWidgetId;
		Cursor c = null;
		try{
			c = mDatabaseHelper.query(RssConstant.Content.WIDGET_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					String widgetTitle = c.getString(c.getColumnIndex(RssConstant.Content.WIDGET_TITLE));
					Log.d(TAG, "widgetTitle = " + widgetTitle);
					mWidgetTitle = widgetTitle;
					mDetailTitleView.setText(widgetTitle);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
	}
	
	class ArticleCacheThread extends Thread{
		private int mWidgetId;
		private int mItemIndex;
		private int mSelectPos = 0;
		private List<Integer> mUpdateIds = null;
		public ArticleCacheThread(Context context, int widgetId, int itemIndex){
			mWidgetId = widgetId;
			mItemIndex = itemIndex;
			mUpdateIds = new ArrayList<Integer>();
			Log.d(TAG, "widgetId = " + mWidgetId + " , ItemIndex = " + mItemIndex);
		}
		private void updateItemState(){
			int size = mUpdateIds.size();
			if(size <= 0){
				return ;
			}
			int i = 0;
			String area = "(";
			for(i = 0;i < size - 1; i++ ){
				int id = mUpdateIds.get(i);
				area += String.valueOf(id);
				area += ",";
			}
			int id = mUpdateIds.get(i);
			area += String.valueOf(id);
			area += ")";
			String where = RssConstant.Content._ID + " in " + area;
			Log.d(TAG, "where = " + where);
			ContentValues values = new ContentValues();
			values.put(RssConstant.Content.DELETED, RssConstant.State.STATE_DELETED);
			mDatabaseHelper.update(RssConstant.Content.ITEM_INDEX_URI, values, where, null);
			SharedPreferences preference = getSharedPreferences(RssConstant.Content.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
			preference.edit().putInt(RssConstant.Key.KEY_WIDGET_ID, mAppWidgetId).commit();
		}
		private int queryPosAndFillIdToList(Cursor cursor, int itemIndex){		
			int position = 0;
			mListIds.clear();
			Log.d(TAG, "itemIndex is " + itemIndex);
			try {
				if (cursor != null && cursor.getCount() > 0) {
					cursor.moveToFirst();
					if(!cursor.isAfterLast()) {
						do {
							int id = cursor.getInt(cursor.getColumnIndexOrThrow(RssConstant.Content._ID));
							int state = cursor.getInt(cursor.getColumnIndex(RssConstant.Content.ITEM_STATE));
							String title = cursor.getString(cursor.getColumnIndex(RssConstant.Content.ITEM_TITLE));
							if(title == null || (title != null && title.trim().equals(""))){
//								Log.d(TAG, "Invalid data _id = " + id);
								mUpdateIds.add(id);
								continue;
							}
							
							mListIds.add(id);
							if(state == RssConstant.State.STATE_ITEM_UNREAD){
								mArticleUnRead ++;
							}
							if (id == itemIndex) {
								mSelectPos = position;
							}
							position++;
						} while (cursor.moveToNext());
					} else {
						Log.d(TAG, "cursor is after last");
					}					
				}
			} catch (Exception e) {
				e.printStackTrace();
			}		
			updateItemState();
			Log.d(TAG, "mPosition is " + position);
			return mSelectPos;
		}
		public synchronized void run(){
			Log.d(TAG, "run");
//			mHandler.sendEmptyMessage(MSG_SHOW_LOADING_DIALOG);
			String orderBy = RssConstant.Content.ITEM_STATE + " asc, "
					+ RssConstant.Content.ITEM_PUBDATE + " desc";
			String selection = null;
			String []projection = new String[]{RssConstant.Content._ID, RssConstant.Content.ITEM_STATE, RssConstant.Content.ITEM_TITLE};
			Cursor c = null;
//			mTotalArticleCount = queryAllItemsCount();
//			mArticleUnRead = queryUnreadItemsCount();
			mArticleUnRead = 0;
			selection= RssConstant.Content.WIDGET_ID + "=" + mAppWidgetId;
			Log.d(TAG, "selection = " + selection);
			try{
				c = mDatabaseHelper.query(RssConstant.Content.VIEW_ITEM_URI, projection, selection, null, orderBy);
				if(c != null){
					Log.d(TAG, "c.getCount = " + c.getCount());
					queryPosAndFillIdToList(c, mItemIndex);
				}
				
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(c != null){
					c.close();
				}
			}
			if(mListIds != null){
				mTotalArticleCount = mListIds.size();
			}
			Log.d(TAG, "mArticleSum = " + mTotalArticleCount + " , mArticleUnRead  = " + mArticleUnRead);
//			mHandler.sendEmptyMessage(MSG_DISMISS_LOADING_DIALOG);
			Message msg = mHandler.obtainMessage(MSG_ADD_ITEM_COMPLETE);
			msg.arg1 = mSelectPos;
			mHandler.sendMessage(msg);
		}
		
		
		private int queryAllItemsCount(){
			String orderBy = RssConstant.Content.ITEM_PUBDATE + " desc";
			String selection= RssConstant.Content.WIDGET_ID + "=" + mWidgetId;
			Cursor c = null;
			int count = 0;
			try{
				c = mDatabaseHelper.query(RssConstant.Content.VIEW_ITEM_URI, null, selection, null, orderBy);
				count = c.getCount();
				
			}catch(Exception e){
				e.printStackTrace();
				return 0;
			}finally{
				if(c != null){
					c.close();
				}
			}
			return count;
		}
		private int queryUnreadItemsCount(){
			String orderBy = RssConstant.Content.ITEM_PUBDATE + " desc";
			String selection= RssConstant.Content.ITEM_STATE + "=" +  RssConstant.State.STATE_ITEM_UNREAD 
					+ " and " + RssConstant.Content.WIDGET_ID + "=" + mWidgetId;
			Cursor c = null;
			int count = 0;
			try{
				c = mDatabaseHelper.query(RssConstant.Content.VIEW_ITEM_URI, null, selection, null, orderBy);
				count = c.getCount();
				
			}catch(Exception e){
				e.printStackTrace();
				return 0;
			}finally{
				if(c != null){
					c.close();
				}
			}
			return count;
		}
		
	}
	
	class ArticleHandler extends Handler{
		private Context mContext;
		public ArticleHandler(Context context){
			mContext = context;
		}
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "MSG = " + msg.toString());
			int what = msg.what;
			switch(what){
			case MSG_CLEAR_LIST:
				mItemList.clear();
				break;
			case MSG_ADD_ITEM_COMPLETE:
				int galleryPos = msg.arg1;
				Log.d(TAG, "listIds.size = " + mListIds.size());
				int dataSize = mListIds.size();
				if (dataSize > 0) {
					mArticleDetailAdapter = new ArrayAdapter<Integer>(mContext,
							android.R.layout.simple_list_item_1,
							android.R.id.text1, mListIds);
					mGallery.setAdapter(mArticleDetailAdapter);
					mGallery.setSelection(galleryPos);
					enableButtons(true);
					buttonLayoutShow();
					mFullScreenDes.setVisibility(View.VISIBLE);
					mNoArticleView.setVisibility(View.INVISIBLE);
				}else{
					enableButtons(false);
					mHandler.sendEmptyMessage(MSG_DISMISS_LOADING_DIALOG);
					emptyNews();
				}
				mLastPage = 0;
				setArticleDetailPage(galleryPos);
				break;
			case MSG_ADD_ITEM:
				ViewItems item = (ViewItems)msg.obj;
				mItemList.add(item);				
				break;
			case MSG_SHOW_LOADING_DIALOG:
				if(mProcessDialog == null || (mProcessDialog != null && !mProcessDialog.isShowing())){
					mProcessDialog = ProgressDialog.show(mContext, null, mContext.getResources().getString(R.string.article_loading), false, true);
					mProcessDialog.setCanceledOnTouchOutside(false);
					mProcessDialog.setCancelable(false);
					sendEmptyMessageDelayed(MSG_DISMISS_LOADING_DIALOG, 20000);
					
				}
				break;
			case MSG_DISMISS_LOADING_DIALOG:
				Log.d(TAG,"MSG_DISMISS_LOADING_DIALOG~~~~~~~~~~~~~~~~~~~");
				if(mProcessDialog != null){
					try {
						mProcessDialog.dismiss();
						mProcessDialog = null;
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
				break;
			case MSG_LOADINIG_NEW_DATA:
				if(!mBackToWidget) {
					mArticleCacheThread = new ArticleCacheThread(mContext, mAppWidgetId, mItemIndexId);
					mArticleCacheThread.start();
				}				
				break;
			case MSG_SET_GALLERY_POS:
				galleryPos = msg.arg1;
				Log.d(TAG, "galleryPos = " + galleryPos);
				if(galleryPos >= 0){
					mGallery.setSelection(galleryPos);
				}
				break;
			case MSG_HIDE_BUTTON_LAYOUT:
				mButtonLayout.setVisibility(View.GONE);
				mHorizontalDivide.setVisibility(View.GONE);
				break;
				
			case MSG_LONG_CLICK_EVENT:
				mState = MODE_LONG_PRESS;
				break;
			default:
				break;
			}
		}
		
	}
	@Override
	public void onBackPressed() {
		/*
		if(isCurrentFullScreen) {
			changeRssDetailSize();
			return;
		} else {
			mBackToWidget = true;
			joinCacheThread();
			Log.d(TAG, "press back key, exit rss detail");
		}
		*/
		mBackToWidget = true;
		joinCacheThread();
		Log.d(TAG, "press back key, exit rss detail");
		super.onBackPressed();
	}
	
	private void joinCacheThread() {
		if (mArticleCacheThread != null && mArticleCacheThread.isAlive()) {
			try {
				mArticleCacheThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (mListIds != null) {
			outState.putIntegerArrayList(KEY_LIST_IDS,
					(ArrayList<Integer>) mListIds);
			int listSize = mListIds.size();
			if (listSize > 0) {
				outState.putInt(KEY_CUR_PAGE, mGalleryPosition);
			}
		}
		super.onSaveInstanceState(outState);
	}

	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if(savedInstanceState == null){
			return ;
		}
		int pos = savedInstanceState.getInt(KEY_CUR_PAGE);
		if(mListIds == null){
			mListIds = savedInstanceState.getIntegerArrayList(KEY_LIST_IDS);
		}
		if(mListIds != null){
			mArticleDetailAdapter = new ArrayAdapter<Integer>(this, android.R.layout.simple_list_item_1, android.R.id.text1, mListIds);
			mGallery.setAdapter(mArticleDetailAdapter);
			mGallery.setSelection(pos);
		}
	}

	private void getDefaultDisplay() {
		mDisplayWidth = getWindowManager().getDefaultDisplay().getWidth();
		mDisPlayHeight = getWindowManager().getDefaultDisplay().getHeight();
		Log.d(TAG, "current height is " + mDisPlayHeight + " curren width is " + mDisplayWidth);
	}
	
	public void setArticleDetailPage(int pos){
		int listSize = 0;
		int page = 0;
		if(mListIds != null){
			listSize = mListIds.size();
			if(listSize > 0){
				page = pos + 1;
				Log.d(TAG, "mLastPage = " + mLastPage +  " , currentPage = " + page);
				if(page != mLastPage){
					mArticleDetailNum.setText(page+"/"+mTotalArticleCount);
					mLastPage = page;
				}
			}else{
				mArticleDetailNum.setText("");
			}
		}else{
			mArticleDetailNum.setText("");
		}
	}
	/*
	public int selectPosition(int realPosition){
		int count = mListIds.size();
		int returnValue = -1;
		if(count > 0){
		int temp = (Integer.MAX_VALUE / 2) % count; 
		int moved = realPosition - temp;
		returnValue = Integer.MAX_VALUE / 2 + moved;
		}else{
			returnValue = 0;
		}
		return returnValue;
	}
	*/
	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent");
		getNewIntent(intent);
		super.onNewIntent(intent);
	}

	
	@Override
	protected void onResume() {
		loadWidgetTitle();
		super.onResume();
	}

	private void getNewIntent(Intent intent){
		Log.d(TAG, "getNewIntent action = " + intent.getAction());
		if(intent.getAction().equals(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL)) {
			mAppWidgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
			mItemIndexId =  intent.getIntExtra(KEY_ACTICLE_ID, -1);
			if(mItemIndexId != -1){
				mHandler.sendEmptyMessage(MSG_SHOW_LOADING_DIALOG);
			}
			mHandler.sendEmptyMessage(MSG_LOADINIG_NEW_DATA);
		} else if(intent.getAction().equals(RssConstant.Intent.INTENT_RSSAPP_OPENARTICLE_DETAIL_FROMLIST)){
//			mAppWidgetId = intent.getIntExtra(RssConstant.Key.KEY_WIDGET_ID, -1);
//			mItemIndexId = intent.getIntExtra(RssConstant.Content._ID,-1);
			int selectPos = intent.getIntExtra(KEY_GALLERY_POS, -1);
			Message msg = mHandler.obtainMessage();
			msg.what = MSG_SET_GALLERY_POS;
			msg.arg1 = selectPos;
			mHandler.sendMessage(msg);
		}
		/*
		if(isCurrentFullScreen){
			changeRssDetailSize();
		}
		*/
		Log.d(TAG, "mItemIndexId = " + mItemIndexId);
	}
	
	private void initArticleUI(){
		
		mFullScreenDesTextZone = (FrameLayout)findViewById(R.id.fullscreen_textview_zone);
		
		mFullScreenTitleLayout = (RelativeLayout)findViewById(R.id.layout_fullscreen);
		mFullScreenTitleView = (TextView)findViewById(R.id.article_fullscreen_title);
		mFullScreenTitleView.setTextColor(Color.WHITE);
		mFullScreenLayout = (RelativeLayout)findViewById(R.id.fullscreen_detail_frame);
		mFullScreenDetailFeedTitle = (TextView)findViewById(R.id.fullscreen_articledetail_author); 
		mFullScreenDetailPubDate = (TextView)findViewById(R.id.fullscreen_articledetail_pubdate);
//		mFullScreenDes = (ArticleDetailTextView)findViewById(R.id.fullscreen_articledetail_description);
		mFullScreenDes = (WebView) findViewById(R.id.fullscreen_webview);
		/*2012-11-13, add by amt_jiayuanyuan for SWITCHUITWOV-3056 */
		//mFullScreenDes.setBackgroundColor(Color.TRANSPARENT);
		/*2012-11-13, add end*/
//		mFullScreenDes.setBackgroundResource(R.drawable.bg_portrait);
		mFullScreenDes.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		mFullScreenDes.setOnTouchListener(mOnTouchListener);
		mFullScreenDes.setWebViewClient(mWebViewClient);
		mTouchSlop = ViewConfiguration.get(mFullScreenDes.getContext()).getScaledTouchSlop();
		mLongTimeOut = ViewConfiguration.get(mFullScreenDes.getContext()).getLongPressTimeout();
//		mFullScreenDes.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
//		mFullScreenEmptyDes = (TextView)findViewById(R.id.fullscreen_articledetail_emptydescription);
		mNoArticleView = (TextView) findViewById(R.id.fullscreen_noarticle);
		mFullScreenNum = (TextView)findViewById(R.id.fullscreen_articledetail_num);
		mFullFeedIconView = (ImageView)findViewById(R.id.feed_icon);
		mDetailTitleLayout = (RelativeLayout)findViewById(R.id.layout_article);
		mDetailTitleView = (TextView)findViewById(R.id.article_title);
		
		mGallery = (Gallery) findViewById(R.id.gallery);
//		mGallery.setParentActivity(this);
		mArticleDetailNum = (TextView)findViewById(R.id.articledetail_num);
		mArticleDetailNum.setVisibility(View.VISIBLE);
		mArticleListButton = (Button) findViewById(R.id.articlelist);
		mViewOriginalButton = (ImageButton) findViewById(R.id.viewOriginal);
		mShareButton = (Button) findViewById(R.id.share);
		mPrevButton = (ImageButton) findViewById(R.id.previous);
		mNextButton = (ImageButton) findViewById(R.id.next);
		mSplitLine = findViewById(R.id.splitline);
		mGallery.setOnItemSelectedListener(this);
		mArticleListButton.setOnClickListener(this);
		mViewOriginalButton.setOnClickListener(this);
		mShareButton.setOnClickListener(this);
		mPrevButton.setOnClickListener(this);
		mNextButton.setOnClickListener(this);
		mGallery.setOnItemClickListener(this);
		actionBarSet((ImageButton) findViewById(R.id.action_bar_btn));
		
		mButtonLayout = (LinearLayout) findViewById(R.id.button_layout);
		mButtonLayout.setVisibility(View.GONE);
		mHorizontalDivide = findViewById(R.id.horizontal_divide);
		mHorizontalDivide.setVisibility(View.GONE);
		changeRssDetailSize();
	}
	private void enableButtons(boolean enabled){
		mShareButton.setEnabled(enabled);
		mViewOriginalButton.setEnabled(enabled);
		mPrevButton.setEnabled(enabled);
		mNextButton.setEnabled(enabled);
		
		mShareButton.setTextColor(enabled ? Color.WHITE : Color.GRAY);
//		mViewOriginalButton.setTextColor(enabled ? Color.WHITE : Color.GRAY);
//		mPrevButton.setTextColor(enabled ? Color.WHITE : Color.GRAY);
//		mNextButton.setTextColor(enabled ? Color.WHITE : Color.GRAY);
	}
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause, mAppWidgetId = " + mAppWidgetId);
		/*
		Intent in =new Intent(RssConstant.Intent.INTENT_RSSAPP_UPDATE_ITEM);        
		in.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		in.putExtra("autoUpdate", false);
		sendBroadcast(in);
		*/
		if(isCurrentFullScreen){
//			changeRssDetailSize();
		}
		/*
		if(mProcessDialog != null && mProcessDialog.isShowing()){
			try {
				mProcessDialog.dismiss();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		*/
		mCachedHashMap.clear();
        super.onPause();
	}

	public void notifyWidgetUpdate(){
		Intent in =new Intent(RssConstant.Intent.INTENT_RSSAPP_UPDATE_ITEM);        
		in.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		in.putExtra("autoUpdate", false);
		sendBroadcast(in);
	}
	@Override
	protected void onDestroy() {
//		Debug.stopMethodTracing();
//		if(mFullScreenDes != null){
//			mFullScreenDes.clearCache(true);
//		}
		unRegisterReceiver();
		if(mProcessDialog != null){
			mProcessDialog.dismiss();
			mProcessDialog = null;
		}
		if(mRefreshDialog != null){
			mRefreshDialog.dismiss();
			mRefreshDialog = null;
		}
		super.onDestroy();
	}
	@Override
	protected void onStop() {
		super.onStop();
		File file = getCacheDir();
		deleteFile(file);
		deleteDatabase("webview.db");
		deleteDatabase("webview.db-shm");
		deleteDatabase("webview.db-wal");
		deleteDatabase("webviewCookiesChromium.db");
		deleteDatabase("webviewCookiesChromium.db-journal");
		deleteDatabase("webviewCookiesChromiumPrivate.db");
		
	}
	private ViewItems fillViewItems(int _id){
		Cursor c = null;
		ViewItems item = null;
		mHandler.sendEmptyMessage(MSG_DISMISS_LOADING_DIALOG);
		if(mCachedHashMap.containsKey(_id)){
			return mCachedHashMap.get(_id);
		}
		int size = mListIds.size();
		int index = mListIds.indexOf(_id);
		int half = 0;
		int start = 0;
		int end = 0;
		int cache = 0;
		if(size > CACHE_SIZE){
			half = CACHE_SIZE / 2;
			cache = CACHE_SIZE;
		}else{
			half = size / 2;
			cache = size;
		}
		if(index >= half){
			start = index - half;
		}else{
			start = index - half + size;
		}
		end = start + cache;
		Log.d(TAG, "end = " + end);
		if(end >= size){
			end = end - size - 1;
		}
		String area = "(";
		int len = cache;
//		Log.d(TAG, "start = " + start + " , end = " + end + " , len = " + len);
		int i = 0;
		int real = 0;
		for(i = 0; i< len; i++){
			real = (start + i) % size;
//			Log.d(TAG, "mListIds.get(" + real +") = " + mListIds.get(i));
			area += String.valueOf(mListIds.get(real));
			area += ",";
		}
		real = (start + i) % size;
		area += String.valueOf(mListIds.get(real));
		area += ")";
//		Log.d(TAG, "area = " + area);
		String selection = RssConstant.Content._ID + " in " + area;
		mCachedHashMap.clear();
		try{
			c = mDatabaseHelper.query(RssConstant.Content.VIEW_ITEM_URI, null, selection, null, null);
			if(c != null){
				if(c.moveToFirst()){
					do{
						item = new ViewItems();
						item._id = c.getInt(c.getColumnIndex(RssConstant.Content._ID));
						item.widgetId = c.getInt(c.getColumnIndex(RssConstant.Content.WIDGET_ID));
						item.feedId = c.getInt(c.getColumnIndex(RssConstant.Content.FEED_ID));
						item.itemId = c.getInt(c.getColumnIndex(RssConstant.Content.ITEM_ID));
						item.feedTitle = c.getString(c.getColumnIndex(RssConstant.Content.FEED_TITLE));
						item.feedUrl = c.getString(c.getColumnIndex(RssConstant.Content.FEED_URL));
						item.itemTitle = c.getString(c.getColumnIndex(RssConstant.Content.ITEM_TITLE));
						item.itemUrl = c.getString(c.getColumnIndex(RssConstant.Content.ITEM_URL));
						item.itemDescription = c.getString(c.getColumnIndex(RssConstant.Content.ITEM_DESCRIPTION));
						item.itemPubdate = c.getLong(c.getColumnIndex(RssConstant.Content.ITEM_PUBDATE));
						item.itemState = c.getInt(c.getColumnIndex(RssConstant.Content.ITEM_STATE));
						item.feeIcon = getIcon(c, item);
						mCachedHashMap.put(item._id, item);
//						Log.d(TAG, "ID = " + item._id + " , TITLE = " + item.itemTitle);
					}while(c.moveToNext());
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(c != null){
				c.close();
			}
		}
		mHandler.sendEmptyMessage(MSG_DISMISS_LOADING_DIALOG);
		if(mCachedHashMap.containsKey(_id)){
			return mCachedHashMap.get(_id);
		}
		return null;
		
	}
	private Bitmap getIcon(Cursor c, ViewItems item){
		if(mHashMap != null){
			if(mHashMap.containsKey(item.feedId)){
				return mHashMap.get(item.feedId);
			}
		}
		byte []image = c.getBlob(c.getColumnIndex(RssConstant.Content.FEED_ICON));
		if(image != null){
			Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
			mHashMap.put(item.feedId, bitmap);
			return bitmap;
		}
		return null;
		
	}
	private String getItemPubDate(ViewItems item) {
    	long currentTime = 0;
		long thisTime = 0;
		String pubDate = null;
		Date today = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");	
		try {
			currentTime = formatter.parse(formatter.format(today)).getTime();
			thisTime = formatter.parse(formatter.format(new Date(item.itemPubdate))).getTime();				
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(currentTime == thisTime){
			SimpleDateFormat sdf = new SimpleDateFormat ("HH:mm:ss");
			pubDate = sdf.format(new Date(item.itemPubdate));
//			holder.itemPubdate.setText(sdf.format(new Date(item.itemPubdate)));
		}else{
			SimpleDateFormat sdf = new SimpleDateFormat ("yyyy-MM-dd");
			pubDate = sdf.format(new Date(item.itemPubdate));
//			holder.itemPubdate.setText(sdf.format(new Date(item.itemPubdate)));
		}
		return pubDate;
    }
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.d(TAG, "onPrepareOptionsMenu isCurrentFullScreen = " + isCurrentFullScreen);
		/**
		if(isCurrentFullScreen) {
			if(menu.findItem(MENU_ITEM_REFRESH) != null) {
				menu.removeItem(MENU_ITEM_REFRESH);
			}
		} else {
			if(menu.findItem(MENU_ITEM_REFRESH) == null) {
				menu.add(0, MENU_ITEM_REFRESH, 1, R.string.refresh);
			}
		}
		*/
		int count = mGallery.getCount();
		menu.findItem(MENU_ITEM_SHARE).setEnabled(count > 0 ? true : false);
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		menu.add(0, MENU_ITEM_REFRESH, 1, R.string.refresh);
        menu.add(0, MENU_ITEM_SHARE, 2, R.string.share);
        menu.add(0, MENU_ITEM_ARTICLE_LIST, 3, R.string.articlelist);
        menu.add(0, MENU_ITEM_SETTING, 4, R.string.settings);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case MENU_ITEM_REFRESH:
			notifyServiceToRefresh();
        	showRefreshDialog();
			break;
		case MENU_ITEM_ARTICLE_LIST:
			Intent listIntent = new Intent();
			listIntent.setClass(this, RssArticleListActivity.class);
//			listIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
			listIntent.putIntegerArrayListExtra(KEY_LIST_IDS, (ArrayList<Integer>) mListIds);
			listIntent.putExtra(KEY_WIDGET_TITLE, mWidgetTitle);
			listIntent.putExtra(KEY_UNREAD_COUNT, mArticleUnRead);
			RssArticleDetailActivity.this.startActivity(listIntent);
			break;
		case MENU_ITEM_SHARE:
			shareActicle();
			break;
		case MENU_ITEM_SETTING:
			Log.d(TAG, "mAppWidgetId is " + mAppWidgetId);
        	Intent intent = new Intent();
        	intent.setAction(RssConstant.Intent.INTENT_RSSAPP_SETTINGS);
        	intent.putExtra(KEY_WIDGET_ID,mAppWidgetId);
        	startActivityForResult(intent, 0);
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		boolean needLoad = false;
		boolean feedSubscribing = false;
		if(data != null){
			needLoad = data.getBooleanExtra(KEY_DATA_CHANGE, false);
			feedSubscribing = data.getBooleanExtra(KEY_FEED_SUBSCRIBING, false);
		}
		if(feedSubscribing){
			showSubscribingDialog();
		}
		if(!feedSubscribing && needLoad){
//			mItemIndexId = -1;
			mHandler.sendEmptyMessage(MSG_LOADINIG_NEW_DATA);
		}
	}
	
	private void showSubscribingDialog() {
		if(mRefreshDialog == null) {
			mRefreshDialog = new ProgressDialog(RssArticleDetailActivity.this);
		}
		mRefreshDialog.setCanceledOnTouchOutside(false);
		mRefreshDialog.setOnCancelListener(this);
		mRefreshDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); 
		mRefreshDialog.setMessage(this.getResources().getString(R.string.subscribing)); 
		mRefreshDialog.setCancelable(true);  
		mRefreshDialog.show();
	}
	private void showRefreshDialog() {
		if(mRefreshDialog == null) {
			mRefreshDialog = new ProgressDialog(RssArticleDetailActivity.this);
		}
		mRefreshDialog.setCanceledOnTouchOutside(false);
		mRefreshDialog.setOnCancelListener(this);
		mRefreshDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); 
		mRefreshDialog.setMessage(this.getResources().getString(R.string.articlerefreshtips)); 
		mRefreshDialog.setCancelable(true);  
		mRefreshDialog.show();
	}

	private void dismissRefreshDialog() {
		if(mRefreshDialog != null) {
			Log.d(TAG, "refresh finish, progress dialog dismiss");
			mRefreshDialog.dismiss();
			mRefreshDialog = null;
		}
	}

	private void notifyServiceToRefresh() {
		Intent intent = new Intent();
    	intent.setAction(RssConstant.Intent.INTENT_RSSSERVICE_STARTREFRESH);
    	intent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
    	intent.putExtra("autoUpdate", false);
    	startService(intent);
	}

	class ViewHolder {
		public View lineView;
		public View titleDivider;
		public TextView itemTitle;
		public TextView itemDescription;
		public TextView feedTitle;
		public TextView itemPubdate;
		public TextView itemNum;
		public RelativeLayout detailFrame;
		public ImageView bussinessView;
		public ImageView optionView;		
		public RelativeLayout layout;
		public RelativeLayout articleHeaderLayout;
		public View convertView;
	}

	public void onItemSelected(AdapterView<?> adapterView, View view, int position,
			long id) {
		Log.d(TAG, "current position = " + position);
		long start = System.currentTimeMillis();
		mGalleryPosition = position;
		int listSize = mListIds.size();
		if(listSize <= 0){
			return ;
		}
		int _id = mListIds.get(position);
		ViewItems item = fillViewItems(_id);
		mItemIndexId = _id;
//		if(mFullScreenLayout.getVisibility() != View.VISIBLE){
//			changeRssDetailSize();
//		}
		mSelectedItem = item;
		if(item != null){
			/**Full screen title**/
			if(isCurrentFullScreen){
				mFullScreenTitleView.setText(item.itemTitle);		
				mFullScreenDetailFeedTitle.setText(item.feedTitle);
				mFullScreenDetailPubDate.setText(getItemPubDate(item));
//				makeNewTextView();
				if(item.itemDescription != null){
					if("".equals(item.itemDescription.trim())||item.itemDescription.trim().length() == 0){
//						mFullScreenDes.setText(getResources().getString(R.string.empty_article_detail));
						String empty = getResources().getString(R.string.empty_article_detail);
						mFullScreenDes.loadDataWithBaseURL(null, cumstomWebView(empty), "text/html", "utf-8", null);
					}
					else{
						String baseUrl = null;
						
						try {
							String feedUrl = null;
							if(mSelectedItem.feedUrl != null){
								if(mSelectedItem.feedUrl.startsWith("http://") || mSelectedItem.feedUrl.startsWith("https://")){
									feedUrl = mSelectedItem.feedUrl;
								}else{
									feedUrl = "http://";
									feedUrl += mSelectedItem.feedUrl;
								}
							}
							if(feedUrl != null){
								URL url = new URL(feedUrl);
								String protocol = url.getProtocol();
								String host = url.getHost();
								baseUrl = protocol + "://" + host;
							}
						} catch (MalformedURLException e) {
							e.printStackTrace();
						}
						Log.d(TAG, "baseUrl = " + baseUrl);
						mFullScreenDes.loadDataWithBaseURL(baseUrl, cumstomWebView(item.itemDescription), "text/html", "utf-8", null);
					}
				}else{
					String empty = getResources().getString(R.string.empty_article_detail);
					mFullScreenDes.loadDataWithBaseURL(null, cumstomWebView(empty), "text/html", "utf-8", null);
				}
//				mFullScreenDes.requestLayout();
				int page = position + 1;
				mFullScreenNum.setText(page+"/"+mTotalArticleCount);
				mArticleDetailNum.setText(page+"/"+mTotalArticleCount);
				if(item.feeIcon != null){
					mFullFeedIconView.setImageBitmap(item.feeIcon);
				}else{
					mFullFeedIconView.setImageResource(R.drawable.ic_rss_big);
				}
				updateItemState(item);
			}
			long end = System.currentTimeMillis();
			Log.d("time", "time = " + (end - start));
			
			/*
			mArticleUnRead  = mArticleUnRead + item.itemState -1;
			if(item.itemState == RssConstant.State.STATE_ITEM_UNREAD){
				item.itemState = RssConstant.State.STATE_ITEM_READ;
				final IndexInfo info = new IndexInfo();
				info.widgetId = item.widgetId;
				info.feedId = item.feedId;
				info.itemId = item.itemId;
				info.itemState = RssConstant.State.STATE_ITEM_READ;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mDatabaseHelper.updateItemState(info);						
					}
				});
				SharedPreferences preference = getSharedPreferences(RssConstant.Content.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
				preference.edit().putInt(RssConstant.Key.KEY_WIDGET_ID, mAppWidgetId).commit();
			}*/
			
			/**
			if(item.itemState == RssConstant.State.STATE_ITEM_UNREAD){
				notifyWidgetUpdate();
			}
			*/
//			if(view != null){
//				TextView itemStateView = (TextView) view.findViewById(R.id.articledetail_num);
//				Message msg = mHandler.obtainMessage(MSG_SHOW_ITEM_STATE);
//				msg.arg1 = realPos;
//				msg.obj = itemStateView;
//				mHandler.sendMessageDelayed(msg, DELAY_TIME);
//			}
			
		}
	}
	private void updateItemState(ViewItems item){
		mArticleUnRead  = mArticleUnRead + item.itemState -1;
		/**Update state to database**/
		if(item.itemState == RssConstant.State.STATE_ITEM_UNREAD){
			item.itemState = RssConstant.State.STATE_ITEM_READ;
			SharedPreferences preferences = getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
			boolean existed = preferences.contains(String.valueOf(mAppWidgetId));
			if(existed){
				preferences.edit().remove(String.valueOf(mAppWidgetId)).commit();
			}
			
			IndexInfo info = new IndexInfo();
			info.widgetId = item.widgetId;
			info.feedId = item.feedId;
			info.itemId = item.itemId;
			info.itemState = RssConstant.State.STATE_ITEM_READ;
			mDatabaseHelper.updateItemState(info);
			SharedPreferences preference = getSharedPreferences(RssConstant.Content.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
			preference.edit().putInt(RssConstant.Key.KEY_WIDGET_ID, mAppWidgetId).commit();
		}else{
			SharedPreferences preferences = getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE);
			boolean existed = preferences.contains(String.valueOf(mAppWidgetId));
			if(existed && mUpdateItems <= 0){
				Intent i = new Intent(RssConstant.Intent.INTENT_RSSAPP_CLEAR_NOTIFICATION);
				i.putExtra(KEY_WIDGET_ID, mAppWidgetId);
				sendBroadcast(i);
			}
			
			mUpdateItems = 0;
		}
	}
	/*
	private void makeNewTextView(){
		if(mFullScreenDesTextZone != null){
			if(mFullScreenDesTextZone.getChildCount() > 0){
				mFullScreenDesTextZone.removeAllViews();
			}
			View view = getLayoutInflater().inflate(R.layout.fullscreen_textview, null);
			mFullScreenDes = (ArticleDetailTextView) view.findViewById(R.id.fullscreen_articledetail_description);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			mFullScreenDesTextZone.addView(view, params);
		}
	}
	*/
	public void onNothingSelected(AdapterView<?> arg0) {

	}

	class RssArticleDetailReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "action = " + action);
			if(RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED.equals(action)) {
				int widgetId = intent.getIntExtra(KEY_WIDGET_ID, -1);
				boolean autoUpdate = intent.getBooleanExtra("autoUpdate", false);
				boolean onlyWidget = intent.getBooleanExtra(KEY_ONLY_WIDGET, false);
				int updated = intent.getIntExtra(KEY_SUBSCRIBED, 0);
				mUpdateItems = updated;
				Log.d(TAG, "updated = " + updated);
				if((widgetId == mAppWidgetId) && !autoUpdate && !onlyWidget) {
					dismissRefreshDialog();
//					mItemIndexId = -1;
					if(updated > 0){
						mHandler.sendEmptyMessage(MSG_LOADINIG_NEW_DATA);
					}else{
						mHandler.sendEmptyMessage(MSG_DISMISS_LOADING_DIALOG);
					}
				}
			} else if(RssConstant.Intent.INTENT_NETWORK_NOTAVAILABLE.equals(action)) {
				Log.d(TAG, "network is not available");
				dismissRefreshDialog();
			}else if(RssConstant.Intent.INTENT_RSSAPP_SERVICE_RESTART.equals(action)){
				Log.d(TAG ,"Service restart");
				dismissRefreshDialog();		
				mHandler.sendEmptyMessage(MSG_LOADINIG_NEW_DATA);
			}
		}
	}

	private void registerReceiver() {
		mArticleDetailReceiver = new RssArticleDetailReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(RssConstant.Intent.INTENT_RSSAPP_UPDATE_FINISHED);
		filter.addAction(RssConstant.Intent.INTENT_NETWORK_NOTAVAILABLE);
		filter.addAction(RssConstant.Intent.INTENT_RSSAPP_SERVICE_RESTART);
		registerReceiver(mArticleDetailReceiver, filter);
	}

	private void unRegisterReceiver() {
		if(mArticleDetailReceiver != null) {
			unregisterReceiver(mArticleDetailReceiver);
			mArticleDetailReceiver = null;
		}
	}

	/*
	private void changeGallerySize(ViewHolder holder) {
		if(!isCurrentFullScreen) {
			if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				Gallery.LayoutParams params;
				if(mDisPlayHeight < DEFAULT_HEIGHT) {
					params = new LayoutParams(dip2px(this, PORTRAIT_WIDTH), dip2px(this, PORTRAIT_HEIGHTSHORT));
				} else {
					params = new LayoutParams(dip2px(this, PORTRAIT_WIDTH), dip2px(this, PORTRAIT_HEIGHT));
				}				
				holder.layout.setLayoutParams(params);
			}else{
				Gallery.LayoutParams params = new LayoutParams(dip2px(this, LANDSCAPE_WIDTH), dip2px(this, LANDSCAPE_HEIGHT));
				holder.layout.setLayoutParams(params);
			}
			holder.articleHeaderLayout.setVisibility(View.VISIBLE);
			
			if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
				holder.itemDescription.setMaxLines(MAX_LINE_PORT);
			}else{
				holder.itemDescription.setMaxLines(MAX_LINE_LAND);
			}
		}else{
			holder.articleHeaderLayout.setVisibility(View.GONE);
			holder.itemDescription.setMaxLines(Integer.MAX_VALUE);
			Gallery.LayoutParams params = (LayoutParams) holder.convertView.getLayoutParams();
			if(params != null)
				params.width = LayoutParams.FILL_PARENT;
		}
	}
	*/
	private void changeRssDetailSize() {
		Log.d(TAG, "change rss detail size, mCurrentPosition is " + mGalleryPosition);
		/*
		if(!isCurrentFullScreen) {			
			mDetailTitleLayout.setVisibility(View.GONE);
			mFullScreenTitleLayout.setVisibility(View.VISIBLE);
			mFullScreenLayout.setVisibility(View.VISIBLE);
			mGallery.setVisibility(View.INVISIBLE);
			mPrevButton.setVisibility(View.VISIBLE);
			mNextButton.setVisibility(View.VISIBLE);
			mShareButton.setVisibility(View.GONE);
			mArticleListButton.setVisibility(View.GONE);
			mArticleDetailNum.setVisibility(View.INVISIBLE);
			actionBarSet((ImageButton) findViewById(R.id.action_bar_fullscreen_btn));
			isCurrentFullScreen = true;
		} else {
			mDetailTitleLayout.setVisibility(View.VISIBLE);
			mFullScreenTitleLayout.setVisibility(View.GONE);
			mFullScreenLayout.setVisibility(View.INVISIBLE);
			mGallery.setVisibility(View.VISIBLE);
			mPrevButton.setVisibility(View.GONE);
			mNextButton.setVisibility(View.GONE);
			mShareButton.setVisibility(View.VISIBLE);
			mArticleListButton.setVisibility(View.VISIBLE);
			mArticleDetailNum.setVisibility(View.VISIBLE);
			actionBarSet((ImageButton) findViewById(R.id.action_bar_btn));
			isCurrentFullScreen = false;
		}
		*/
		mDetailTitleLayout.setVisibility(View.GONE);
		mFullScreenTitleLayout.setVisibility(View.VISIBLE);
		mFullScreenLayout.setVisibility(View.VISIBLE);
		mGallery.setVisibility(View.INVISIBLE);
		mPrevButton.setVisibility(View.VISIBLE);
		mNextButton.setVisibility(View.VISIBLE);
		mShareButton.setVisibility(View.GONE);
		mArticleListButton.setVisibility(View.GONE);
		mArticleDetailNum.setVisibility(View.INVISIBLE);
		actionBarSet((ImageButton) findViewById(R.id.action_bar_fullscreen_btn));
//		mGallery.setGalleryState(isCurrentFullScreen);
		
		/*
		if (mArticleDetailAdapter != null) {
			mArticleDetailAdapter.notifyDataSetChanged();
		}
		*/
	}
	
	private void actionBarSet(ImageButton actionBarBtn){
		if (actionBarBtn != null) {
			if (ViewConfiguration.get(this).hasPermanentMenuKey()) {
				actionBarBtn.setVisibility(View.GONE);
				mSplitLine.setVisibility(View.GONE);
			} else {
				actionBarBtn.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						PopupMenu popup = constructPopupMenu(v);
						if (popup != null) {
							popup.show();
						}
					}
				});
			}
		}
	}
	
	private PopupMenu constructPopupMenu(View anchorView) {		
		final PopupMenu popupMenu = new PopupMenu(this, anchorView);
		onCreateOptionsMenu(popupMenu.getMenu());
		onPrepareOptionsMenu(popupMenu.getMenu());
		popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener(){

			@Override
			public boolean onMenuItemClick(MenuItem item) {
				Log.i(TAG, "onMenuItemClick:" + item.getTitle() + "" + item.getOrder());
				onOptionsItemSelected(item);
				return false;
			}
			
		});
		
		return popupMenu;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.articlelist:
			Intent listIntent = new Intent();
			listIntent.setClass(this, RssArticleListActivity.class);
//			listIntent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
			listIntent.putIntegerArrayListExtra(KEY_LIST_IDS, (ArrayList<Integer>) mListIds);
			listIntent.putExtra(KEY_WIDGET_TITLE, mWidgetTitle);
			listIntent.putExtra(KEY_UNREAD_COUNT, mArticleUnRead);
			RssArticleDetailActivity.this.startActivity(listIntent);
			break;
		case R.id.share:
			shareActicle();
			break;
		case R.id.viewOriginal:
			buttonLayoutShow();
			if(mArticleDetailAdapter != null && mArticleDetailAdapter.getCount() > 0) {
				if (mSelectedItem != null){
					Log.d(TAG, "RssConstant.Content.ITEM_URL is "+ RssConstant.Content.ITEM_URL);
					if(mSelectedItem.itemUrl != null){
						Uri uri = Uri.parse(mSelectedItem.itemUrl);
						try{
							Intent viewOriginalIntent = new Intent(Intent.ACTION_VIEW, uri);
							viewOriginalIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
							startActivity(viewOriginalIntent);
						}catch(ActivityNotFoundException e){
							e.printStackTrace();
						}
						updateItemState(mSelectedItem);
					}
				}
			}			
			break;
		case R.id.previous:
			if(mHandler.hasMessages(MSG_CHANGE_ARTICLE)){
				return ;
			}
			mHandler.sendEmptyMessageDelayed(MSG_CHANGE_ARTICLE, DELAY_CHANGE_ARTICLE);
			buttonLayoutShow();
			Log.d(TAG, "change to previous news, mGalleryPosition is " + mGalleryPosition);		
			int posLeft = mGalleryPosition - 1;
			if(posLeft < 0){
				posLeft = mGallery.getCount() - 1;
			}
			mGallery.setSelection(posLeft);
			break;
		case R.id.next:
			if(mHandler.hasMessages(MSG_CHANGE_ARTICLE)){
				return ;
			}
			mHandler.sendEmptyMessageDelayed(MSG_CHANGE_ARTICLE, DELAY_CHANGE_ARTICLE);
			buttonLayoutShow();
			Log.d(TAG, "change to next news, mGalleryPosition is " + mGalleryPosition);
			int posRight = mGalleryPosition + 1;
			if(posRight >= mGallery.getCount()){
				posRight = 0;
			}
			mGallery.setSelection(posRight);
			break;
		default:
			break;
		}
	}
	
	private void shareActicle() {
		final ViewItems shareInfo = new ViewItems();
		final EditText ShareText = new EditText(RssArticleDetailActivity.this);
		ShareText.addTextChangedListener(textWatcher);
		if (mSelectedItem != null) {
			shareInfo.itemUrl = mSelectedItem.itemUrl;
			shareInfo.itemTitle = mSelectedItem.itemTitle;
			shareInfo.itemPubdate = mSelectedItem.itemPubdate;
			SimpleDateFormat sdf = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			ShareText.setText(this.getResources().getString(R.string.title) + " : " + shareInfo.itemTitle + "\n"
					+ this.getResources().getString(R.string.date) + " : " + sdf.format(new Date(shareInfo.itemPubdate)) + "\n"
					+ this.getResources().getString(R.string.url) + " : " + shareInfo.itemUrl);
			if (shareInfo.itemUrl != null) {
				if((mShareDialog == null) || (mShareDialog != null && !mShareDialog.isShowing())){
					AlertDialog.Builder builder = new AlertDialog.Builder(
							RssArticleDetailActivity.this);
					builder = builder.setTitle(getResources().getString(R.string.share)).setIcon(android.R.drawable.ic_dialog_info)
							.setView(ShareText).setNegativeButton(getResources().getString(R.string.cancel), null);

					builder.setPositiveButton(getResources().getString(R.string.confirm), new OnClickListener() {

						public void onClick(DialogInterface dialog, int which) {
							mShareDialog = null;
							Intent intent = new Intent(Intent.ACTION_SEND);
							intent.setType("text/plain");
							intent.putExtra(Intent.EXTRA_TEXT, ShareText.getText().toString());
							intent.putExtra(Intent.EXTRA_SUBJECT, "subject");
							startActivity(Intent.createChooser(intent, getTitle()));
						}

					});
					mShareDialog = builder.show();
				}
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		switch (arg0.getId()) {
		case R.id.gallery:
			if(!isCurrentFullScreen) {
				if(mGalleryPosition == arg2){
					changeRssDetailSize();
					if(mListIds != null){
						int listSize = mListIds.size();
						if(listSize > 0){
							int realPos = arg2 % listSize;
							ViewItems item = mSelectedItem;
							if(item != null){
								mFullScreenTitleView.setText(item.itemTitle);		
								mFullScreenDetailFeedTitle.setText(item.feedTitle);
								mFullScreenDetailPubDate.setText(getItemPubDate(item));
//								makeNewTextView();
								if(item.itemDescription != null){
									if("".equals(item.itemDescription.trim())||item.itemDescription.trim().length() == 0){
//										mFullScreenDes.setText(getResources().getString(R.string.empty_article_detail));
										String empty = getResources().getString(R.string.empty_article_detail);
										mFullScreenDes.loadDataWithBaseURL(null, cumstomWebView(empty), "text/html", "utf-8", null);
									}
									else{
										mFullScreenDes.loadDataWithBaseURL(null, cumstomWebView(item.itemDescription), "text/html", "utf-8", null);
										Log.d(TAG, cumstomWebView(item.itemDescription));
									}
								}else{
									String empty = getResources().getString(R.string.empty_article_detail);
									mFullScreenDes.loadDataWithBaseURL(null, cumstomWebView(empty), "text/html", "utf-8", null);
								}
//								mFullScreenDes.requestLayout();
								int page = realPos + 1;
								mFullScreenNum.setText(page+"/"+mTotalArticleCount);
								if(item.feeIcon != null){
									mFullFeedIconView.setImageBitmap(item.feeIcon);
								}else{
									mFullFeedIconView.setImageResource(R.drawable.ic_rss_big);
								}
								updateItemState(item);
							}
						}
					}
				}
			} else {
				Log.d(TAG, "now is in fullscreen");
			}		
			break;
		default:
			break;
		}		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if(mArticleDetailAdapter != null) {
			mArticleDetailAdapter.notifyDataSetChanged();
		}		
		super.onConfigurationChanged(newConfig);
	}
	
	private int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	
	@Override
	public void onCancel(DialogInterface dialog) {
		Log.d(TAG, "onCancel");
		Intent intent = new Intent(RssConstant.Intent.INTENT_RSSAPP_STOP_UPDATING);
		intent.putExtra(KEY_WIDGET_ID, mAppWidgetId);
		startService(intent);
//		mItemIndexId = -1;
//		mHandler.sendEmptyMessage(MSG_LOADINIG_NEW_DATA);
		mHandler.sendEmptyMessage(MSG_SHOW_LOADING_DIALOG);
	}
	private String removeTags(String str) {
		if(str != null){
			str = str.replaceAll("&gt;", ">");
			str = str.replaceAll("&lt;", "<");
			str = str.replaceAll("&amp;", "&");
			str = str.replaceAll("&quot;", "\"");
			str = str.replaceAll("&nbsp;", " ");
			str = str.replaceAll("<.*?>", " ");
			str = str.replaceAll("\\s+", " ");
		}
		return str;
	}
	private String cumstomWebView(String str){
		
		str = str.replaceAll("&gt;", ">");
		str = str.replaceAll("&lt;", "<");
		str = str.replaceAll("&amp;", "&");
		str = str.replaceAll("&quot;", "\"");
		str = str.replaceAll("&nbsp;", " ");
		str = str.replaceAll("<!--.*?-->", " ");
		String head = "<html><head></head><body style=\"padding-left:14px;padding-right:10px;background-color:transparent\">";
		String tail = "</body></html>";
		return head + str + tail;
	}

	private OnTouchListener mOnTouchListener = new OnTouchListener() {
		
		private int mDownX;
		private int mDownY;
		@Override
		public boolean onTouch(View v, MotionEvent event) {
//			Log.d(TAG, "onTouch event = " + event.toString());
			int action = event.getAction();
			switch(action){
			case MotionEvent.ACTION_DOWN:
				mDownX = (int) event.getX();
				mDownY = (int) event.getY();
				mState = MODE_TAP;
				mHandler.sendEmptyMessageDelayed(MSG_LONG_CLICK_EVENT, mLongTimeOut);
				break;
			case MotionEvent.ACTION_MOVE:
				int x = (int) event.getX();
				int y = (int) event.getY();
				int deltaX = Math.abs(x - mDownX);
				int deltaY = Math.abs(y - mDownY);
				if(deltaX > mTouchSlop || deltaY > mTouchSlop){
					mState = MODE_MOVE;	
				}
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				if(mHandler.hasMessages(MSG_LONG_CLICK_EVENT)){
					mHandler.removeMessages(MSG_LONG_CLICK_EVENT);
				}
				if(mState == MODE_TAP){
					onWebViewTap();
				}
				break;
			}
			return false;
		}
	};
	private void onWebViewTap(){
		Log.d(TAG, "onWebViwTap : ActionMode is " + mActionMode);
		if(!mActionMode){
			buttonLayoutShow();
		}
	}
	private void buttonLayoutShow(){
		if(mListIds == null || (mListIds != null && mListIds.size() <= 0)){
			return ;
		}
		if(mHandler.hasMessages(MSG_HIDE_BUTTON_LAYOUT)){
			mHandler.removeMessages(MSG_HIDE_BUTTON_LAYOUT);
		}
		mButtonLayout.setVisibility(View.VISIBLE);
		mHorizontalDivide.setVisibility(View.VISIBLE);
		mHandler.sendEmptyMessageDelayed(MSG_HIDE_BUTTON_LAYOUT, DELAY_BUTTON_SHOW);
	}
	private void emptyNews(){
		mFullScreenDes.setVisibility(View.INVISIBLE);
		mNoArticleView.setVisibility(View.VISIBLE);
		String noarticle = getResources().getString(R.string.empty_article_list);
//		SpannableString ss = new SpannableString("<h2><font color=#AAAAAA>" + noarticle + "</font></h2>");
		mFullScreenTitleView.setText(R.string.rss);		
		mFullScreenDetailFeedTitle.setText("");
		mFullScreenDetailPubDate.setText("");
//		mFullScreenDes.loadDataWithBaseURL(null, ss.toString(), "text/html", "utf-8", null);
		mNoArticleView.setText(noarticle);
		mFullScreenNum.setText("");
		mArticleDetailNum.setText("");
		mFullFeedIconView.setImageResource(R.drawable.ic_rss_big);
	}
	
	private void deleteFile(File file){
		if(file != null && file.exists() && file.isDirectory()){
			for(File f : file.listFiles()){
				Log.e(TAG, "F = " + f);
				if(f.isFile()){
					f.delete();
				}else{
					deleteFile(f);
				}
			}
			file.delete();
		}else{
			if(file != null){
				file.delete();
			}
		}
	}
	
	TextWatcher textWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			if(mShareDialog != null){
				Button button = mShareDialog.getButton(Dialog.BUTTON_POSITIVE);
				int textlen = 0;
				if(s != null){
					textlen = s.length();
				}
				if(button != null){
					button.setEnabled(textlen > 0 ? true : false);
				}
			}
		}
	};

	@Override
	public void onActionModeStarted(ActionMode mode) {
		super.onActionModeStarted(mode);
		mActionMode = true;
		Log.d(TAG, "ActionMode = " + mActionMode);
	}
	@Override
	public void onActionModeFinished(ActionMode mode) {
		super.onActionModeFinished(mode);
		mActionMode = false;
		Log.d(TAG, "ActionMode = " + mActionMode);
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
	
	private WebViewClient mWebViewClient = new WebViewClient(){
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.d(TAG, "shouldOverrideUrlLoading url = " + url);
			try{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
			}catch(ActivityNotFoundException e){
				e.printStackTrace();
			}
			return true;//super.shouldOverrideUrlLoading(view, url);
		}
		
	};
}
