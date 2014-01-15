package com.motorola.mmsp.activitygraph.activityWidget2d;

import java.util.ArrayList;

import com.motorola.mmsp.activitygraph.R;
import com.motorola.mmsp.activitygraph.activityWidget2d.GraphWidgetProvider.Item;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.RemoteViews;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class ActivityProvider extends GraphWidgetProvider {
    private static ActivityModel myModel;
    // private static ActivityApplication myApplication;
    private static int box = 0;
    private static final String TAG = "gxl/ActivityProvider";
    public static int MAXCARD = 9;
    private static Context mContext;
    private AppWidgetManager myAppManager;
    private RemoteViews mRemoteView = null;

    private static ArrayList<Item> items = null;
    private static final String EXTRA_APPWIDGET_RESUME_LISTENING = "EXTRA_APPWIDGET_RESUME_LISTENING";
    private static final int EXTRA_APPWIDGET_RESUME_VALUE = 1;

    private static final int[] ITEMS_ICON_THEME_ID = { R.drawable.bg_image_big,
            R.drawable.bg_image_big, R.drawable.bg_image_big,
            R.drawable.bg_image_big, R.drawable.bg_image_big,
            R.drawable.bg_image_big, R.drawable.bg_image_big,
            R.drawable.bg_image_big, R.drawable.bg_image_big, };

    private static final int[] ITEMS_ICON_ID = { R.drawable.welcome01,
            R.drawable.welcome02, R.drawable.welcome03, R.drawable.welcome04,
            R.drawable.welcome05, R.drawable.welcome06, R.drawable.welcome07,
            R.drawable.welcome08, R.drawable.welcome09, };

    private static final int[] ITEMS_TEXT_THEME_ID = {
            R.drawable.bar_image_big, R.drawable.bar_image_big,
            R.drawable.bar_image_big, R.drawable.bar_image_big,
            R.drawable.bar_image_big, R.drawable.bar_image_big,
            R.drawable.bar_image_big, R.drawable.bar_image_big,
            R.drawable.bar_image_big, };

    private static final String[] ITEMS_TEXT = { "Folder", "FM radio",
            "Camera", "Browser", "Contacts", "Gallery", "Clock", "Calenda",
            "File Manager", };

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        
        if (items != null) {
            clearItemBitmap(items);
            items.clear();
        }
        items = null;
        myModel = null;

    }

    public void onEnabled(Context context) {
        Log.i(TAG, "++onEnable");
        mContext = context;
        setAppInfo();

        // if (box == 0) {
        // Context graphContext = myModel.getGraphContext();
        // SharedPreferences shared = graphContext.getSharedPreferences(
        // "graphyPreferences", Context.MODE_WORLD_READABLE
        // + Context.MODE_WORLD_WRITEABLE
        // + Context.MODE_MULTI_PROCESS);
        // boolean showWelcome = shared.getBoolean("popupWelcome", true);
        // if (showWelcome) {
        // showPlayApp(mContext);
        // shared.edit().putBoolean("popupWelcome", false).commit();
        // }
        // }
    }

    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "++onReceive : " + intent.getAction());
        mContext = context;
        
        String action = intent.getAction();
        if (action
                .equals("com.motorola.mmsp.activitywidget2d.action.databaseChanged")) {
            if (myModel == null || items == null) {
                Log.i(TAG, "++myModel is null, update the widget");
                this.onUpdate(context, AppWidgetManager.getInstance(context), null);
            }
            
        }
        
        setAppInfo();
        super.onReceive(context, intent);

        // if (action.equals("com.motorola.mmsp.home.ACTION_WIDGET_ADDED")) {
        // myAppManager = AppWidgetManager.getInstance(context);
        // // myModel.resetState();
        // RemoteViews main = new RemoteViews(context.getPackageName(),
        // getMainLayoutId());
        // init(main, context, AppWidgetManager.getInstance(context),
        // new ComponentName(mContext, ActivityProvider.class));
        // updateOutBox(main);
        // if (items == null) {
        // Log.i(TAG, "++onReceive---------items is null");
        // updateWidget(main);
        // } else {
        // Log.i(TAG, "++onReceive---------items is not null");
        // updateAll(main, mContext, new ComponentName(mContext,
        // ActivityProvider.class), items);
        // }
        // }
        // if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
        // Bundle extras = intent.getExtras();
        // if (extras != null
        // && extras.getInt(EXTRA_APPWIDGET_RESUME_LISTENING, 0) !=
        // EXTRA_APPWIDGET_RESUME_VALUE) {
        // int[] appWidgetIds = extras
        // .getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        // if (appWidgetIds != null && appWidgetIds.length > 0) {
        // this.onUpdate(context,
        // AppWidgetManager.getInstance(context), appWidgetIds);
        // }
        // }
        // }

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onUpdate++");
        mContext = context;
        setAppInfo();
        myAppManager = appWidgetManager;
        // myModel.resetState();
        // Modifyed by amt_chenjing 20120711 for SWITCHUI-2033 begin
        //if (mRemoteView == null) {
            mRemoteView = new RemoteViews(context.getPackageName(),
                    getMainLayoutId());
            init(mRemoteView, context, appWidgetManager, new ComponentName(
                    mContext, ActivityProvider.class));
        //}
       // Modifyed by amt_chenjing 20120711 for SWITCHUI-2033 end
        if (items == null) {
            updateWidget();

        } else {
            updateOutBox(mRemoteView);
            updateAll(mRemoteView, mContext, new ComponentName(mContext,
                    ActivityProvider.class), items);
        }

    }

    public void onItemClick(Context context, int position) {
        Log.i(TAG, "position=" + position);
        if (mContext == null) {
            mContext = context;
        }
        switch (position) {
        case 0:
            setIntent(0);
            break;
        case 1:
            setIntent(1);
            break;
        case 2:
            setIntent(2);
            break;
        case 3:
            setIntent(3);
            break;
        case 4:
            setIntent(4);
            break;
        case 5:
            setIntent(5);
            break;
        case 6:
            setIntent(6);
            break;
        case 7:
            setIntent(7);
            break;
        case 8:
            Log.d(TAG, "onItemClick-8");
            setIntent(8);
            break;
        }
    }

    @Override
    public void onPlayClick(Context context) {
        // TODO Auto-generated method stub
        showPlayApp(context);
        // Toast.makeText(context,
        // "Play Button is clicked.",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSettingClick(Context context) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onSettingClick-------box = " + box);
        if (box == 1) {
            showSettingApp(context);
        }
        // Toast.makeText(context,
        // "Setting Button is clicked.",Toast.LENGTH_SHORT).show();
    }

    public void showSettingApp(Context context) {
        Intent intent = new Intent();
        intent.putExtra("screen", MAXCARD);
        intent.setComponent(ComponentName
                .unflattenFromString("com.motorola.mmsp.activitygraph/.ActivityGraphSettings"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
        // showSettingButton(mContext, new ComponentName(mContext,
        // ActivityProvider.class), false);
        // updateWidget();
    }

    public void showPlayApp(Context context) {
        Log.d(TAG, "onWelcomeTextClicked");
        if (getBox() == 1) {
            Log.d(TAG, "onWelcomeTextClicked-----------box is 1");
            return;
        }
        Intent intent = new Intent();
        intent.putExtra("screen", MAXCARD);
        intent.setClassName("com.motorola.mmsp.activitygraph",
                "com.motorola.mmsp.activitygraph.WelcomeActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);

    }

    public void setAppInfo() {
        // myApplication = ActivityApplication.getApplicationInstance();
        // myApplication.setProviderInstance(this);
        // myModel = myApplication.getModel();
        if (myModel == null) {
            myModel = new ActivityModel(mContext, this);
        }
        box = getBox();
    }

    public int getBox() {
        if (myModel == null) {
            // myModel =
            // ActivityApplication.getApplicationInstance().getModel();
            myModel = new ActivityModel(mContext, this);
        }
        return myModel.getOutBox();
    }

    // public void updateWidget(RemoteViews main) {
    // if (mContext == null) {
    // //
    // mContext=ActivityApplication.getApplicationInstance().getApplicationContext();
    // Log.i(TAG, "updateWidget01---mContext is null---");
    // }
    // Log.i(TAG, "++box=" + box);
    // if (myModel == null) {
    // return;
    // }
    // box = myModel.getOutBox();
    // if (items != null) {
    // clearItemBitmap(items);
    // items.clear();
    // }
    // if (box == 1) {
    // items = myModel.getList();
    // Log.i(TAG, "+size=" + items.size());
    //
    // } else {
    //
    // if (items == null) {
    // items = new ArrayList<Item>();
    // }
    // for (int i = 0; i < MAXCARD; i++) {
    // Item item = new Item();
    // item.skin = BitmapFactory.decodeResource(
    // mContext.getResources(), ITEMS_ICON_ID[i]);
    // item.position = i;
    // item.intentStr = "";
    // items.add(item);
    // }
    // }
    // updateOutBox(main);
    // updateAll(main, mContext, new ComponentName(mContext,
    // ActivityProvider.class), items);
    //
    // }

    public void updateWidget() {
        if (mContext == null) {
            // mContext=ActivityApplication.getApplicationInstance().getApplicationContext();
            Log.i(TAG, "updateWidget---mContext is null---");
        }
        Log.d(TAG, "updateWidget:++box=" + box);
        if (myModel == null) {
            return;
        }
        box = myModel.getOutBox();
        if (items != null) {
            clearItemBitmap(items);
            items.clear();
        }
        if (box == 1) {
            items = myModel.getList();
            Log.i(TAG, "+size=" + items.size());
        } else {

            if (items == null) {
                items = new ArrayList<Item>();
            }
            for (int i = 0; i < MAXCARD; i++) {
                Item item = new Item();
                // modified by gxl-----begin
                //item.skin = BitmapFactory.decodeResource(
                //        mContext.getResources(), ITEMS_ICON_ID[i]);
                if (i == 0) {
                    item.skin = BitmapFactory.decodeResource(
                            mContext.getResources(),
                            R.drawable.class_bg_image_big);
                } else if (i < 4) {
                    item.skin = BitmapFactory.decodeResource(
                            mContext.getResources(),
                            R.drawable.class_bg_image_medium);
                } else {
                    item.skin = BitmapFactory.decodeResource(
                            mContext.getResources(),
                            R.drawable.class_bg_image_small);
                }
                item.cover = BitmapFactory.decodeResource(
                        mContext.getResources(), ITEMS_ICON_ID[i]);
             // modified by gxl-----end
                item.position = i;
                item.intentStr = "";
                items.add(item);
            }
        }
     // Modifyed by amt_chenjing 20120711 for SWITCHUI-2033 begin
        //if (mRemoteView == null) {
            mRemoteView = new RemoteViews(mContext.getPackageName(),
                    getMainLayoutId());
            init(mRemoteView, mContext, AppWidgetManager.getInstance(mContext),
                    new ComponentName(mContext, ActivityProvider.class));
        //}
     // Modifyed by amt_chenjing 20120711 for SWITCHUI-2033 end
        updateOutBox(mRemoteView);
     // Modifyed by amt_chenjing 20120711 for SWITCHUI-2033 begin
        /*updateAll(mRemoteView, mContext, new ComponentName(mContext,
                ActivityProvider.class), items);*/
        Runnable runable = new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				updateAll(mRemoteView, mContext, new ComponentName(mContext,
		                ActivityProvider.class), items);
			}
		};
		runable.run();
     // Modifyed by amt_chenjing 20120711 for SWITCHUI-2033 end

    }

    public void updateSpecificItems(int[] positions) {
        if (mContext == null) {
            // mContext=ActivityApplication.getApplicationInstance().getApplicationContext();
            Log.i(TAG, "updateSpecificItems---mContext is null---");
        }
        Log.d(TAG, "updateSpecificItems:++box=" + box);
        if (myModel == null) {
            return;
        }
        box = myModel.getOutBox();
        if (items != null) {
            clearItemBitmap(items);
            items.clear();
        }
        // modified by gxl....begin
        // if (mRemoteView == null) {
            mRemoteView = new RemoteViews(mContext.getPackageName(),
                    getMainLayoutId());
            init(mRemoteView, mContext, AppWidgetManager.getInstance(mContext),
                    new ComponentName(mContext, ActivityProvider.class));
        // }
        // modified by gxl,...end
        updateOutBox(mRemoteView);

        if (box == 1) {
            items = myModel.getList();
            if (positions != null) {
                //ArrayList<Item> tmpList = new ArrayList<Item>();
                Log.i(TAG, "position--length =" + positions.length);
                //for (int i = 0; i < positions.length; i++) {
                //    tmpList.add(items.get(positions[i]));
                //}
                updateAll(mRemoteView, mContext, new ComponentName(mContext,
                        ActivityProvider.class), items, positions);
                return;
            }
            Log.i(TAG, "+size=" + items.size());
        } else {

            if (items == null) {
                items = new ArrayList<Item>();
            }
            for (int i = 0; i < MAXCARD; i++) {
                Item item = new Item();
             // modified by gxl-----begin
                //item.skin = BitmapFactory.decodeResource(
                //        mContext.getResources(), ITEMS_ICON_ID[i]);
                if (i == 0) {
                    item.skin = BitmapFactory.decodeResource(
                            mContext.getResources(),
                            R.drawable.class_bg_image_big);
                } else if (i < 4) {
                    item.skin = BitmapFactory.decodeResource(
                            mContext.getResources(),
                            R.drawable.class_bg_image_medium);
                } else {
                    item.skin = BitmapFactory.decodeResource(
                            mContext.getResources(),
                            R.drawable.class_bg_image_small);
                }
                item.cover = BitmapFactory.decodeResource(
                        mContext.getResources(), ITEMS_ICON_ID[i]);
             // modified by gxl-----end
                item.position = i;
                item.intentStr = "";
                items.add(item);
            }
        }
        updateAll(mRemoteView, mContext, new ComponentName(mContext,
                ActivityProvider.class), items);
    }

    public void isShowPlayButton(RemoteViews main, boolean show) {
        Log.i(TAG, "++show play button=" + show);
        // showPlayButton(main, mContext, new ComponentName(mContext,
        // ActivityProvider.class), show);
        showPlayButton(main, mContext, show);
    }

    public void isShowPlayButton(boolean show) {
        Log.i(TAG, "++show play button=" + show);
        if (mRemoteView == null) {
            mRemoteView = new RemoteViews(mContext.getPackageName(),
                    getMainLayoutId());
        }
        showPlayButton(mRemoteView, mContext, new ComponentName(mContext,
                ActivityProvider.class), show);
    }

    public void setIntent(int position) {
        Log.d(TAG, "setIntent:" + position);
        if (mContext == null) {
            // mContext=ActivityApplication.getApplicationInstance().getApplicationContext();
            Log.i(TAG, "setIntent---mContext is null---");
        }
        if (items == null) {
            Log.i(TAG, "++MY LIST IS NULL");
            // setAppInfo();
            // // box = myModel.getOutBox();
            // if (box == 1) {
            // items = myModel.getList();
            // }
            return;
        }
        // Log.d(TAG, "items size:" + items.size());
        if (items != null) {
            Log.d(TAG, "items size:" + items.size());
         // Added by amt_chenjing for SWITCHUI-2076 20120712 begin
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
             // Added by amt_chenjing for SWITCHUI-2076 20120712 end
                Log.i(TAG, "++position=" + item.position + "++intentSTR="
                        + item.intentStr);
                if (item.position == position) {
                    if (!item.intentStr.equals("") && item.intentStr != null) {
                        Log.i(TAG, "++setIntent+");
                        Intent intent = new Intent();
                        intent.setComponent(ComponentName
                                .unflattenFromString(item.intentStr));
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (!item.intentStr
                                .equals("com.motorola.mmsp.activitygraph/.ManageShortcuts")) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            intent.setAction(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        } else {
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        }
                        // intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        try {
                            mContext.startActivity(intent);
                            // Intent intentCount = new Intent(
                            // "com.motorola.mmsp.activitygraph.COUNT_APP");
                            // intentCount.putExtra("appname", item.intentStr);
                            // Log.d(TAG, "start activity:" + item.intentStr
                            // + " from graph");
                            // mContext.sendBroadcast(intentCount);
                        } catch (Exception e) {
                            Log.e(TAG, "not find application");
                            myModel.setAppResolveInfo();
                            updateWidget();
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
    }

    public void clearItemBitmap(ArrayList<Item> list) {
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                Item item = list.get(i);
                if (item.skin != null && !item.skin.isRecycled()) {
                    item.skin.recycle();
                    item.skin = null;
                }
                if (item.cover != null && !item.cover.isRecycled()) {
                    item.cover.recycle();
                    item.cover = null;
                }
                if (item.textbar != null && !item.textbar.isRecycled()) {
                    item.textbar.recycle();
                    item.textbar = null;
                }
                item = null;
            }
        }
    }

    public void updateOutBox(RemoteViews main) {
        if (box == 1) {
            isShowPlayButton(main, false);
        } else {
            isShowPlayButton(main, true);
        }
    }

    public static ActivityProvider createProviderInstance(Context context) {
        ActivityProvider mInstance = new ActivityProvider();
        mInstance.mContext = context;
        mInstance.setAppInfo();
        return mInstance;
    }
}
