package com.motorola.mmsp.socialGraph.socialwidget2D;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.motorola.mmsp.socialGraph.Constant;

public class SocialWidget2DModel {
	private static final String TAG = "SocialGraph2DProvider";
	private static final boolean D = /*false*/true;
	private final static String VERSION = "2012-06-28";
	
	public static final int LOAD_PERSONSINFO_SKIN = 1;
	public static final int LOAD_PERSONSINFO_SHORTCUT = 2;
	public static final int LOAD_PERSONSINFO_ALL = LOAD_PERSONSINFO_SKIN | LOAD_PERSONSINFO_SHORTCUT;

	private static SocialWidget2DModel m2dModel = new SocialWidget2DModel();
	
	private boolean isCreated = false;
	private boolean mFirstLoadFinished = false;
	private boolean mOutOfBoxStatus = false;
	private int mShortcutNum = 7;	
	private ArrayList<GraphWidgetProvider.Item> mPersonInfos = new ArrayList<GraphWidgetProvider.Item>();
	private  Handler mDelayHandler = new Handler();	
	private Runnable mModeChangeRunnable = null;
        public static final int DELAY_TIME = 2*1000;
	public void postDelayed(Runnable runnable){
		if(mModeChangeRunnable!=null){ 
			mDelayHandler.removeCallbacks(mModeChangeRunnable);			
		}
                mModeChangeRunnable = runnable;
		mDelayHandler.postDelayed(mModeChangeRunnable, DELAY_TIME);
	}
	
	private SocialWidget2DModel() {
		
	}
	
	public static SocialWidget2DModel getInstace(){
		return m2dModel;
	}
	
	public boolean isEnable() {
		return isCreated;
	}
	public boolean isFirstLoadFinished() {
		return mFirstLoadFinished;
	}
	public void onCreate(final Context context) {
		if (isCreated) {
			return;
		}
		
		if(D)Log.d(TAG, "SocialWidget statr service");
		context.startService(new Intent("com.motorola.mmsp.intent.action.SOCIAL_SERVICE_ACTION"));
//		context.startService(new Intent("com.motorola.mmsp.intent.action.CALLLOG_SERVICE_ACTION"));
		
		boolean bDirty = false;
		if (!isCreated) {
			isCreated = true;
			bDirty = true;
			mShortcutNum = SocialWidget2DUtils.getShortcutNum(context);			
			mOutOfBoxStatus = SocialWidget2DUtils.getOutOfBoxStatus(context);
		}
		
		if (bDirty) {
			//final Context ct = context;
			asyncReload(context, LOAD_PERSONSINFO_ALL, null , new Handler(), new Runnable(){
				public void run() {	
					Intent intent = new Intent(Constant.BROADCAST_WIDGET_UPDATE);
					context.sendBroadcast(intent);
				}});
		}
		
		if(D)Log.d(TAG, "SocialWidget verson:" + VERSION);
		
	}
	
	public void onRelease(Context context) {
		Log.d(TAG, "model onRelease");
		if (!isCreated) {
			return;
		}
		isCreated = false;
		releasePersonInfoBitmapMemory();
		
	}

	
	public boolean getOutOfBoxStatus() {
		return mOutOfBoxStatus;
	}
	
	public void updateOutOfBoxStatus(Context context) {
		mOutOfBoxStatus = SocialWidget2DUtils.getOutOfBoxStatus(context);
	}
	public void asyncReload(final Context context, final int mode, final ArrayList<Integer> reloadIndexs,
			final Handler handler, final Runnable callBack){
		if(D)Log.d(TAG, "SocialWidget asyncReload");
		Runnable r = new Runnable() {
		public void run() {
			reloadSocialPersonInfos(context,mode,reloadIndexs);       
               
			if(handler!=null && callBack!=null){
                	     handler.post(callBack);
			}
		    }
		};
        
		mTaskQueue.push(r);
	}

	public ArrayList<GraphWidgetProvider.Item> reloadSocialPersonInfos(Context context, int mode, ArrayList<Integer> reloadIndexs) {
		if(D)Log.d(TAG, "SocialAppCode getSocialPersonInfos begin");		
		Context graphContext = SocialWidget2DUtils.getGraphContext(context);

		
		if(mPersonInfos.size()==0){
			for (int i = 0; i < mShortcutNum; i++) {
				mPersonInfos.add(i, new GraphWidgetProvider.Item());
			}
		}
		
				
		if ((mode & LOAD_PERSONSINFO_SHORTCUT) != 0) {
			if (reloadIndexs == null || reloadIndexs.size() <= 0) {
				reloadIndexs = new ArrayList<Integer>();
				for (int i=0; i < mShortcutNum; i++) {
					reloadIndexs.add(i);
				}
			}
			
			Cursor c = null;
			try {
				c = context.getContentResolver().query(
						Constant.SHORTCUTS_CONTENT_URI,
						new String[] { Constant.SHORTCUT_INDEX, 
								Constant.SHORTCUT_CONTACT_ID,
								Constant.SHORTCUT_SIZE }, 
								null, 
								null,
								Constant.SHORTCUT_INDEX + " ASC");
				if(D)Log.d(TAG, "load shortcut cursor = " + c + "count = "
						+ (c != null ? c.getCount() : 0));
				
				if (c != null && c.getCount() > 0) {
					for (int i = 0; i < c.getCount() && i < mPersonInfos.size(); i++) {
						c.moveToNext();
						if (!reloadIndexs.contains(i)) {
							continue;
						}
						GraphWidgetProvider.Item person = mPersonInfos.get(i);
						person.position = c.getInt(0);
						person.id = c.getInt(1);
						person.text = SocialWidget2DUtils.loadName(context.getContentResolver(),
								person.id);
						if (person.cover != null && !person.cover.isRecycled()) {
							person.cover.recycle();
							person.cover=null;
						}
						person.cover = SocialWidget2DUtils.loadPhotoCheckStretched(context, person.id,person.position);
						if (person.cover == null) {
							if(D)Log.d(TAG, "need to load default photo");
							person.cover = getDefaultPhoto(context,i);
						}
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (c != null) {
					c.close();
				}
			}
		}
		
		if ((mode & LOAD_PERSONSINFO_SKIN) != 0) {
			Cursor skinCursor = null;
			try {
				skinCursor = context.getContentResolver().query(
						Constant.SKIN_CONTENT_URI, null, null, null, null);
                                if(skinCursor == null || skinCursor.getCount() <= 0 ){
					skinCursor = context.getContentResolver().query(
							Constant.SKIN_DEFAULT_CONTENT_URI, null, null, null, null);
				}
				if(D)Log.d(TAG, "load skin cursor = " + skinCursor + "count = "
						+ (skinCursor != null ? skinCursor.getCount() : 0));
				if (skinCursor == null)
					if(D)Log.d(TAG, "skin cursor = null");

				if (skinCursor != null && skinCursor.getCount() > 0) {
					if(D)Log.d(TAG, "skin cursor != null");
					if(D)Log.d(TAG, "skin count=" + skinCursor.getCount());
					skinCursor.moveToFirst();
		
					
					for (int i = 0; graphContext != null
							&& i < skinCursor.getColumnCount()
							&& i < mPersonInfos.size(); i++) {
						GraphWidgetProvider.Item person = mPersonInfos.get(i);
						if(D)Log.d(TAG, "load person: " + i + "'s skin start");
						try {
							Bitmap map = BitmapFactory.decodeResource(
									graphContext.getResources(), skinCursor
									.getInt(2 * i + 1));
							if (person.skin != null && !person.skin.isRecycled()) {
								person.skin.recycle();
								person.skin=null;
							}
							person.skin = map;	

							map = BitmapFactory.decodeResource(
									graphContext.getResources(), skinCursor
											.getInt(2 * i + 2));
							if (person.textbar != null && !person.textbar.isRecycled()) {
								person.textbar.recycle();
								person.textbar =null;
							}
							person.textbar = map;
							if(D)Log.d(TAG, "load person: " + i + "'s skin ok");
						} catch (OutOfMemoryError e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (skinCursor != null) {
					skinCursor.close();
				}
			}
		}
		
		for (int i = 0; i < mPersonInfos.size(); i++) {
			GraphWidgetProvider.Item person = mPersonInfos.get(i);
			if (person.position < 0)
				person.position = i;
             //IKDOMINO-6562 begin
             if(mOutOfBoxStatus == false){
				    person.position = i;
             }
             //IKDOMINO-6562 begin
			if (person.cover == null) {
				person.cover = getDefaultPhoto(context,i);
			}
		}
		
		for (int i = 0; i < mPersonInfos.size(); i++) {
			GraphWidgetProvider.Item person = mPersonInfos.get(i);
			if(D)Log.d(TAG, "SocialAppCode getGraphWidgetProvider.Items i=" + i 
					+ "id =" + person.id  
					+ " name=" + person.text 
					+ "photo = " + person.cover 
					+ " position = " + person.position 
					+ " skin=" + person.skin 
					+ " nameBg=" + person.textbar);
		}
		
		if(D)Log.d(TAG, "SocialWidgetApi get GraphWidgetProvider.Item end");
	        mFirstLoadFinished = true;
		return getPersonInfos();
	}
	
	public ArrayList<GraphWidgetProvider.Item> getPersonInfos() {
		return mPersonInfos;
	}
	
	private Bitmap getDefaultPhoto(Context context, int index) {
		if(D)Log.d(TAG, "get default photo + " + index);
		if(D)Log.d(TAG, "mShortcutNum + " + mShortcutNum);
		
		if (index >= mShortcutNum) {
			return null;
		}

		Bitmap bmp = null;
		if (mOutOfBoxStatus) {
			String default_photo = "default_photo" + String.valueOf(index + 1);
			if(D)Log.d(TAG, "default photo str =" + default_photo);
			bmp = SocialWidget2DUtils.loadBitmapByName(context,default_photo);
		} else {
			String welcome_photo = "welcome_photo" + String.valueOf(index + 1);
			if(D)Log.d(TAG, "welcome_photo photo str =" + welcome_photo);
			bmp = SocialWidget2DUtils.loadBitmapByName(context,welcome_photo);
		}

		return bmp;
	}
	
	private void releasePersonInfoBitmapMemory() {
		if(D)Log.d(TAG, "release person info BitmapMemory");
		for (int i = 0; i < mPersonInfos.size(); i++) {
			if (mPersonInfos.get(i).cover != null
					&& !mPersonInfos.get(i).cover.isRecycled()) {
				if(D)Log.d(TAG, "photo release");
				mPersonInfos.get(i).cover.recycle();
				mPersonInfos.get(i).cover = null;
			}
			if (mPersonInfos.get(i).skin != null
					&& !mPersonInfos.get(i).skin.isRecycled()) {
				if(D)Log.d(TAG, "skin release");
				mPersonInfos.get(i).skin.recycle();
				mPersonInfos.get(i).skin = null;
			}
			if (mPersonInfos.get(i).textbar != null
					&& !mPersonInfos.get(i).textbar.isRecycled()) {
				if(D)Log.d(TAG, "nameBg release");
				mPersonInfos.get(i).textbar.recycle();
				mPersonInfos.get(i).textbar = null;
			}
		}
	}
		
	public ArrayList<Integer> indexsFromPeople(ArrayList<Integer> informationChangedPeople) {
		ArrayList<Integer> indexs = new ArrayList<Integer>();
		if (informationChangedPeople == null || informationChangedPeople.size() <= 0) {
			return indexs;
		}
		int size = mPersonInfos.size();
		for (int i=0; i < size; i++) {
			GraphWidgetProvider.Item item = mPersonInfos.get(i);
			if (item != null && informationChangedPeople.contains(item.id)) {
				indexs.add(item.position);
			}
		}
		return indexs;
	}	
	
	public int getShortcutNumber() {
		return mShortcutNum;
	}
	
	private static class TaskStack {
        Thread mWorkerThread;
        private final ArrayList<Runnable> mThingsToLoad;

        public TaskStack() {
            mThingsToLoad = new ArrayList<Runnable>();
            mWorkerThread = new Thread(new Runnable() {
                public void run() {
                    while (true) {
                        Runnable r = null;
                        synchronized (mThingsToLoad) {
                            if (mThingsToLoad.size() == 0) {
                                try {
                                    mThingsToLoad.wait();
                                } catch (InterruptedException ex) {
                                    // nothing to do
                                }
                            }
                            if (mThingsToLoad.size() > 0) {
                                r = mThingsToLoad.remove(0);
                            }
                        }
                        if (r != null) {
                            r.run();
                        }
                    }
                }
            });
            mWorkerThread.start();
        }

        public void push(Runnable r) {
            synchronized (mThingsToLoad) {
                mThingsToLoad.add(r);
                mThingsToLoad.notify();
            }
        }
    }

    public void pushTask(Runnable r) {
        mTaskQueue.push(r);
    }
       
    private final TaskStack mTaskQueue = new TaskStack();
}

