package com.motorola.mmsp.activitygraph;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import android.graphics.drawable.Drawable;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.text.format.Time;
import android.util.Log;
import android.util.DisplayMetrics;
import com.motorola.mmsp.activitygraph.AppRank.ManualList;
import com.motorola.mmsp.activitygraph.AppRank.Ranks;
import com.motorola.mmsp.activitygraph.AppRank.Apps;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;
import android.util.TypedValue;
import android.content.res.Resources;
import android.provider.Settings;
import android.content.ContentResolver;
import android.content.pm.PackageManager.NameNotFoundException;

public class ActivityGraphModel extends BroadcastReceiver {

    private static final String PREFERENCE_NAME = "graphyPreferences";
    private static final String TAG = "ActivityGraphModel";
    private static int MODE = Context.MODE_WORLD_READABLE
            + Context.MODE_WORLD_WRITEABLE + Context.MODE_MULTI_PROCESS;
    public static final int AUTO_MODE = 0;
    public static final int MANUAL_MODE = 1;
    private static final String GRAPHY_MODE = "Mode";
    private static final String GRAPHY_SKIN = "Skin";
    private static final String GRAPHY_TIME = "Time";
    private static final String GRAPHY_DATE = "date";
    private static final String GRAPHY_MONTH = "month";
    private static final String GRAPHY_YEAR = "year";
    private static final String GRAPHY_ALBUM = "album";
    private static final String MAX_CARD_NUMBER = "max card";
    private static final String GRAPHY_AUDIO = "audio";
    private static Context mContext;
    private static ActivityGraphModel mModel = null;
    private static ArrayList<String> mAllAppList = new ArrayList<String>();
    private static ArrayList<ManualInfo> mManualList = new ArrayList<ManualInfo>();
    private static ArrayList<String> mHiddenList = new ArrayList<String>();
    private static HashMap<String, AppLabelIconInfo> mAppLabelMap = new HashMap<String, AppLabelIconInfo>();

    private static ArrayList<String> mAdded = new ArrayList<String>();
    private static ArrayList<String> mRemoved = new ArrayList<String>();
    private static ArrayList<String> mUpdated = new ArrayList<String>();

    private Handler mHandler = new Handler();

    private static ContentResolver mResolver = null;
    private static final String SORT_ORDER_CURRENT_COUNT = Ranks.CURRENT_RANK
            + " DESC";
    private static final String ACTION_COUNT_APP = "com.motorola.mmsp.activitygraph.COUNT_APP";
    public static final String BROADCAST_DAY_PASS = "com.motorola.mmsp.activitygraph.DAY_PASS";
    public static final String BROADCAST_ACTIVITY_NOT_FOUND = "com.motorola.mmsp.activitygraph.ACTIVITY_NOT_FOUND";
    public static final String BROADCAST_RESET_FOR_OUT_OF_BOX = "com.motorola.mmsp.activitygraph.RESET_FOR_OUT_OF_BOX";
    public static final String ACTION_OUT_OF_BOX = "com.motorola.mmsp.activitygraph.OUT_OF_BOX";
    private static final String META_CHANGED = "com.android.music.metachanged";
    private ArrayList<WeakReference<Callbacks>> mCallbacks = new ArrayList<WeakReference<Callbacks>>();
    private final Object mLock = new Object();
    private static long mAlbum;
    private static long mAudio;
    private final static String[] mSpecialApp = {
    // "com.android.contacts/.activities.DialtactsActivity",
    // "com.android.contacts/.activities.PeopleActivity"
    };
    private static int mRankingDays;

    private static final String GRAPHY_FLEX_DAY_VALUE = "Flex";
    private static final String TAG_RANKINGDAYS = "activity-ranking-days";
    private static final int MISC_TOTAL_DAY_MIN = 1;
    private static final int MISC_TOTAL_DAY_MAX = 20;
    private static int mOutOfBoxed = 0;
    private static final String STK_COMPONENT_NAME = "com.android.stk/.StkLauncherActivity";
    private static ArrayList<String> mNotExistAutoApps = new ArrayList<String>();
    private static ArrayList<String> mNotExistManualApps = new ArrayList<String>();

    private static final int REMOVE_PACKAGES = 0;
    private static final int CHANGE_PACKAGES = 1;

    public interface Callbacks {
        public void bindAppsAdded(ArrayList<String> list);

        public void bindAppsDeleted(ArrayList<String> list);

        public void bindAppsUpdated(ArrayList<String> list);
    }

    public static ActivityGraphModel getInstance() {
        if (mModel == null) {
            mModel = new ActivityGraphModel();
        }
        return mModel;
    }

    public void addCallback(Callbacks callbacks) {
        for (int i = mCallbacks.size(); --i >= 0;) {
            Callbacks callback = mCallbacks.get(i).get();
            if (callback == callbacks) {
                return;
            }
        }
        mCallbacks.add(new WeakReference<Callbacks>(callbacks));
    }

    public void startLoader(Context context) {
        Log.d(TAG, "start loader");
        mContext = context;
        mResolver = context.getContentResolver();
        HandlerThread thread = new HandlerThread(
                ActivityGraphModel.class.getSimpleName());
        thread.start();

        mHandler = new Handler(thread.getLooper());
        createPreferenceData(false);
        loadSettings(context);
        mAlbum = getGraphyAlbum(context);
        mAudio = getGraphyMusic(context);

        mRankingDays = getGraphyFlexRankingDays();
        Log.d(TAG, "mRankingDays =" + mRankingDays);
        if (mRankingDays == 0xff) {
            if (!loadRankingDaysFromFlex()) {
                mRankingDays = 7;
                setGraphyFlexRankingDays(7);
            }
        }
        Log.d(TAG, "mRankingDays =" + mRankingDays);

        ContentResolver resolver = context.getContentResolver();
        mOutOfBoxed = Settings.System.getInt(resolver, "box", 0);
        Log.d(TAG, "mOutOfBoxed =" + mOutOfBoxed);
    }

    public void stopLoader() {

    }

    private void clearData() {
        mManualList.clear();
        mHiddenList.clear();
        mAllAppList.clear();
        mAppLabelMap.clear();
    }

    private void setAppLabel(Context context) {
        final Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager pm = context.getPackageManager();
        List<ResolveInfo> apps = null;
        apps = pm.queryIntentActivities(main, 0);
        if (apps == null || apps.size() == 0) {
            Log.d(TAG, "apps return");
            return;
        }
        int num = apps.size();
        Log.d(TAG, "query database begin");
        for (int i = 0; i < num; i++) {
            ResolveInfo item = apps.get(i);
            ComponentName name = new ComponentName(
                    item.activityInfo.applicationInfo.packageName,
                    item.activityInfo.name);
            String appname = name.flattenToShortString();
            mAllAppList.add(appname);
            loadAppsLabelAndIcon(appname, pm, item);
        }

    }

    private void loadSettings(Context context) {
        Log.d(TAG, "load Settings");
        clearData();
        setAppLabel(context);
        Cursor c = null;
        mNotExistAutoApps.clear();
        mNotExistManualApps.clear();
        try {
            Log.d(TAG, "QUERY RANKS");
            c = mResolver.query(Ranks.CONTENT_URI, Ranks.PROJECTION, null,
                    null, SORT_ORDER_CURRENT_COUNT);
            Log.d(TAG, "query ranks database  ");
            if (c == null) {
                Log.d(TAG, "query curson is null");
            }
            if (c != null && c.getCount() > 0) {
                Log.d(TAG, "query count is > 0");
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    Log.d(TAG, "move cursor");
                    boolean exist = false;
                    String name = c.getString(1);
                    for (int i = 0; i < mAllAppList.size(); i++) {
                        if (name.equals(mAllAppList.get(i))) {
                            exist = true;
                            int bHidden = c.getInt(2);
                            Log.d(TAG, "get ranks info, name:" + name
                                    + ", hidden:" + bHidden);
                            if (bHidden != 0) {
                                Log.d(TAG, "add to hidden List");
                                mHiddenList.add(name);
                            }
                            break;
                        }
                    }
                    if (!exist) {
                        mNotExistAutoApps.add(name);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "ERROR");
            e.printStackTrace();
        } finally {
            Log.d(TAG, "FINALLY");
            if (c != null) {
                c.close();
            }
        }
        /*
         * if(mNotExistAutoApps.size() > 0){
         * deleteAppsFromRanksDatabase(mNotExistApps); } mNotExistApps.clear();
         */
        try {
            Log.d(TAG, "QUERY MANUAL LIST DATABASE");
            c = mResolver.query(ManualList.CONTENT_URI, ManualList.PROJECTION,
                    null, null, null);
            if (c != null && c.getCount() > 0) {
                while (c != null && c.moveToNext()) {
                    ManualInfo info = new ManualInfo();
                    info.mId = c.getInt(ManualList.INDEX_MANUALINDEX);
                    info.mName = c.getString(ManualList.INDEX_APPNAME);
                    boolean exist = false;

                    for (int i = 0; i < mAllAppList.size(); i++) {
                        if ((info.mName).equals(mAllAppList.get(i))) {
                            exist = true;
                            mManualList.add(info);
                            break;
                        }
                    }
                    if (!exist) {
                        mNotExistManualApps.add(info.mName);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "ERROR");
            e.printStackTrace();
        } finally {
            Log.d(TAG, "finally");
            if (c != null) {
                c.close();
            }
        }

        Log.d(TAG, "query database end");
    }

    private void deleteAppsFromRanksDatabase(ArrayList<String> notExistApps) {
        Log.d(TAG, "deleteAppsFromRanksDatabase()");
        for (int i = 0; i < notExistApps.size(); i++) {
            String name = notExistApps.get(i);
            if (!name.equals(STK_COMPONENT_NAME)) {
                mResolver.delete(Ranks.CONTENT_URI, Ranks.APPName + "=?",
                        new String[] { name });
                AppLabelIconInfo appinfo = checkAppResolveInfo(name);
                if (appinfo != null) {
                    mAppLabelMap.remove(name);
                }
                removeFromHiddenList(name);
                mAllAppList.remove(name);
                Intent intent = new Intent(
                        "com.motorola.mmsp.activitygraph.REMOVE_APP_PACKAGE");
                intent.putExtra("component name", name);
                mContext.sendBroadcast(intent);
            }
        }
        updateAllRanking(mContext);
    }

    private void deleteAppsFromManualDatabase(ArrayList<String> notExistApps) {
        Log.d(TAG, "deleteAppsFromManualDatabase()");
        for (int i = 0; i < notExistApps.size(); i++) {
            String name = notExistApps.get(i);
            if (!name.equals(STK_COMPONENT_NAME)) {
                mResolver.delete(ManualList.CONTENT_URI, ManualList.APPNAME
                        + "=?", new String[] { name });
                AppLabelIconInfo appinfo = checkAppResolveInfo(name);
                if (appinfo != null) {
                    mAppLabelMap.remove(name);
                }
                removeFromManualList(name);
                mAllAppList.remove(name);
                Intent intent = new Intent(
                        "com.motorola.mmsp.activitygraph.REMOVE_APP_PACKAGE");
                intent.putExtra("component name", name);
                mContext.sendBroadcast(intent);
            }
        }
    }

    private void loadAppsLabelAndIcon(String component, PackageManager pm,
            ResolveInfo info) {
        AppLabelIconInfo app = new AppLabelIconInfo();

        app.appname = info.loadLabel(pm).toString();
        app.appicon = info.loadIcon(pm);

        mAppLabelMap.put(component, app);
    }

    public static AppLabelIconInfo getLabelIconInfo(String component) {

        AppLabelIconInfo info = mAppLabelMap.get(component);
        return (info != null ? info : null);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        // context = mApp;
        boolean bAdd = false;
        boolean bUpdate = false;
        boolean bRemove = false;
        final String action = intent.getAction();
        mContext = context;
        if (action == null) {
            return;
        }
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                || Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)
                || Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
            mAdded.clear();
            mRemoved.clear();
            mUpdated.clear();

            if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
                final String packages[] = intent
                        .getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
                addAppstoDatabase(context, packages);
                addExternalAppstoDatabase(context, packages);
                bAdd = true;
            } else {
                final String[] packages = { intent.getData()
                        .getSchemeSpecificPart() };
                final boolean replacing = intent.getBooleanExtra(
                        Intent.EXTRA_REPLACING, false);

                if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                    updateAppstoDatabase(context, packages);
                    bAdd = true;
                } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    if (!replacing) {
                        removeAppsFromDatabase(context, packages,
                                REMOVE_PACKAGES);
                        bRemove = true;
                    }
                } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                    if (!replacing) {
                        addAppstoDatabase(context, packages);
                        bAdd = true;
                    } else {
                        updateAppstoDatabase(context, packages);
                        bUpdate = true;
                    }
                }
            }

            for (int i = mCallbacks.size(); --i >= 0;) {
                WeakReference<Callbacks> ref = mCallbacks.get(i);
                final Callbacks callback = ref.get();
                if (callback == null) {
                    mCallbacks.remove(ref);
                }
            }

            int index = mCallbacks.size();
            while (--index >= 0) {

                final Callbacks callback;

                callback = mCallbacks.get(index).get();
                if (callback == null) {
                    continue;
                }
                if (bAdd && mAdded.size() > 0) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            Log.d(TAG, "call back bind apps");
                            callback.bindAppsAdded(mAdded);
                        }
                    });
                }
                if (bRemove && mRemoved.size() > 0) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            callback.bindAppsDeleted(mRemoved);
                        }
                    });
                }
                if (bUpdate && mUpdated.size() > 0) {
                    mHandler.post(new Runnable() {
                        public void run() {
                            callback.bindAppsUpdated(mUpdated);
                        }
                    });
                }
            }
        } else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE
                .equals(action)) {
            final String packages[] = intent
                    .getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
            if (packages != null) {
                removeAppsFromDatabase(context, packages, CHANGE_PACKAGES);
            }
        } else if (ACTION_COUNT_APP.equals(action)) {
            String name = intent.getStringExtra("appname");
            Log.d(TAG, "get app name:" + name);
            if (mOutOfBoxed == 1) {
                if ((name != null) && (!name.equals(""))) {
                    addFrequencetoDatabase(context, name);
                    updateAllRanking(context);
                }
            }
        } else if (Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {

            startAlarm(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Intent logObserverIntent = new Intent(context,
                    ActivityGraphLogService.class);
            context.startService(logObserverIntent);
            startAlarm(context);
            Time t = new Time();
            t.setToNow();
            int date = getGraphyRecordDate();
            int year = getGraphyRecordYear();
            int month = getGraphyRecordMonth();
            Log.d(TAG, "get date/month/year" + date + "/" + month + "/" + year);
            if ((t.monthDay != date) || (t.month != month) || (t.year != year)) {
                Log.d(TAG, "BOOT pass day");
                passDay(context);
                setGraphyRecordDate(t.monthDay);
                setGraphyRecordMonth(t.month);
                setGraphyRecordYear(t.year);
                setGraphyTime(System.currentTimeMillis());

            }
        } else if (intent.getAction().equals(BROADCAST_DAY_PASS)) {
            Log.d(TAG, "pass day intent");
            Time t = new Time();
            t.setToNow();
            if ((t.hour == 0) && (t.minute == 0)) {
                setGraphyRecordDate(t.monthDay);
                setGraphyRecordMonth(t.month);
                setGraphyRecordYear(t.year);
                setGraphyTime(System.currentTimeMillis());
                passDay(context);
            }
            startAlarm(context);
        } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            mAppLabelMap.clear();
            mAllAppList.clear();
            setAppLabel(context);
        } else if (ACTION_OUT_OF_BOX.equals(action)) {
            mOutOfBoxed = 1;
        } else if (BROADCAST_RESET_FOR_OUT_OF_BOX.equals(action)) {
            resetForOutOfBox();
        } else if (BROADCAST_ACTIVITY_NOT_FOUND.equals(action)) {
            /*
             * String name = intent.getStringExtra("app name");
             * Log.d(TAG,"app name = " + name); if (name != null) {
             * deleteNotFoundAppFromDatabase(name); }
             */
        } else if (META_CHANGED.equals(action)) {
            long id = intent.getLongExtra("id", 0);
            Log.d(TAG, "get changed meta audioid:" + id);
            setGraphyMusic(id);
            long album = getAlbumIdBySongId(id);
            setGraphyAlbum(album);
            Intent newIntent = new Intent("MOTOWIDGET_UPDATE_ALBUMNID");
            newIntent.putExtra("audioId", id);
            newIntent.putExtra("albumnId", album);
            Log.d(TAG, "send broadcast, album:" + album + ", audio:" + id);
            context.sendBroadcast(newIntent);
        } else {
            return;
        }
    }

    private void addFrequencetoDatabase(Context context, String name) {
        Cursor c = null;
        try {
            c = context.getContentResolver().query(Ranks.CONTENT_URI,
                    new String[] { Ranks.APPName },
                    Ranks.APPName + "=?" + " and " + Ranks.DAY + "=?",
                    new String[] { name, "0" }, null);

            if (c != null && c.getCount() > 0) {
                String sql = String.format(
                        "update ranks set %s=%s+1 where %s='%s' and %s=%s",
                        Ranks.CURRENT_COUNT, Ranks.CURRENT_COUNT,
                        Ranks.APPName, name, Ranks.DAY, "0");
                context.getContentResolver().update(AppRank.EXECSQL, null, sql,
                        null);
            } else {
                ContentValues values = new ContentValues();
                values.put(Ranks.APPName, name);
                values.put(Ranks.CURRENT_COUNT, 1);
                values.put(Ranks.HIDDEN, 0);
                context.getContentResolver().insert(Ranks.CONTENT_URI, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        try {
            c = context.getContentResolver().query(Ranks.CONTENT_URI,
                    new String[] { Ranks.APPName },
                    "appName" + "=?" + " and " + "day" + "=?",
                    new String[] { name, "-1" }, null);
            if (c != null && c.getCount() > 0) {

            } else {
                ContentValues value = new ContentValues();
                value.put(Ranks.APPName, name);
                value.put(Ranks.DAY, -1);
                value.put(Ranks.HIDDEN, 0);
                mResolver.insert(Ranks.CONTENT_URI, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private void setGraphyMusic(long id) {
        Log.d(TAG, "set graphy music:" + id);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putLong(GRAPHY_AUDIO, id);
        editor.commit();
    }

    private void setGraphyAlbum(long id) {
        Log.d(TAG, "set graphy album:" + id);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putLong(GRAPHY_ALBUM, id);
        editor.commit();
    }

    private long getGraphyAlbum(Context context) {
        SharedPreferences shared = context.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        long id = shared.getLong(GRAPHY_ALBUM, 0);
        return id;
    }

    private long getGraphyMusic(Context context) {
        SharedPreferences shared = context.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        long id = shared.getLong(GRAPHY_AUDIO, 0);
        return id;
    }

    private long getAlbumIdBySongId(long id) {
        long album = 0;
        if (id < 0) {
            return album;
        }
        Cursor c = null;
        try {
            c = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null, "_id" + " = " + id, null, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                if (!c.isAfterLast()) {
                    album = c.getLong(c.getColumnIndex("album_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
                c = null;
            }
        }
        return album;
    }

    public static void updateDayRanking(Context context, int day) {
        String sql = String
                .format("SELECT _id, %s,NUM FROM (select _id, %s ,sum(%s) AS NUM FROM %s where %s=%s GROUP BY %s) ORDER BY NUM ASC,_id",
                        Ranks.APPName, Ranks.APPName, Ranks.CURRENT_COUNT,
                        Ranks.TABLE_NAME, Ranks.DAY, String.valueOf(day),
                        Ranks.APPName);
        Cursor c = null;
        try {
            c = context.getContentResolver().query(AppRank.EXECSQL, null, sql,
                    null, null);

            ContentValues values = new ContentValues();
            if (c != null && c.getCount() > 0) {
                int currentFrequency = -1;
                int currentRank = 0;
                int formerRank = 0;
                c.moveToFirst();
                int count = c.getCount();

                for (int i = 0; i < count; i++) {
                    if (c.getInt(2) != currentFrequency) {
                        currentRank = i + 1;
                        formerRank = currentRank;
                        currentFrequency = c.getInt(2);
                    } else {
                        currentRank = formerRank;
                    }
                    Log.d(TAG,
                            "updateDayRANKing:i :" + i + ":" + c.getString(0)
                                    + c.getString(1) + c.getInt(2)
                                    + currentRank);

                    values.put(c.getString(1), currentRank);
                    c.moveToNext();
                }
            }
            context.getContentResolver().update(AppRank.SET_RANK, values,
                    String.valueOf(day), null);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }

    }

    public static void updateAllRanking(Context context) {
        updateDayRanking(context, 0);
        updateTotalRanking(context);
    }

    public static void updateTotalRanking(Context context) {
        String sql = String.format(
                "select %s ,sum(%s) AS NUM FROM %s where %s >= 0 GROUP BY %s",
                Ranks.APPName, Ranks.CURRENT_RANK, Ranks.TABLE_NAME, Ranks.DAY,
                Ranks.APPName);
        Cursor c = null;
        Cursor wc = null;
        try {
            c = context.getContentResolver().query(AppRank.EXECSQL, null, sql,
                    null, null);
            Log.i("test", "-----c.getCount = " + c.getCount());
            ContentValues values = new ContentValues();
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                int count = c.getCount();
                for (int i = 0; i < count; i++) {
                    int sum = 0;
                    // wc =
                    // context.getContentResolver().query(AppRank.CONTENT_URI,
                    // null, "(" + Ranks.APPName + "=?)",
                    // new String[]{c.getString(0)},null);
                    wc = context.getContentResolver().query(
                            AppRank.EXECSQL,
                            null,
                            "select * from " + Ranks.TABLE_NAME + " where "
                                    + Ranks.APPName + "='" + c.getString(0)
                                    + "' and " + Ranks.DAY + " >= 0", null,
                            null);
                    if (wc != null && wc.getCount() > 0) {
                        wc.moveToFirst();
                        int wcount = wc.getCount();
                        Log.i("test", "-----wcount = " + wcount);
                        for (int j = 0; j < wcount; j++) {
                            switch (wc.getInt(3)) {
                            case 0:
                                sum = (int) (sum + wc.getInt(5) * 80);
                                break;
                            case 1:
                                sum = (int) (sum + wc.getInt(5) * 70);
                                break;
                            case 2:
                                sum = (int) (sum + wc.getInt(5) * 60);
                                break;
                            case 3:
                                sum = (int) (sum + wc.getInt(5) * 50);
                                break;
                            case 4:
                                sum = (int) (sum + wc.getInt(5) * 40);
                                break;
                            case 5:
                                sum = (int) (sum + wc.getInt(5) * 30);
                                break;
                            case 6:
                                sum = (int) (sum + wc.getInt(5) * 20);
                                break;
                            case 7:
                                sum = (int) (sum + wc.getInt(5) * 10);
                                break;
                            default:
                                break;
                            }
                            wc.moveToNext();
                        }
                    }
                    // values.put(c.getString(0), c.getInt(1));
                    values.put(c.getString(0), sum);
                    Log.i("test", "-----sum = " + sum);
                    c.moveToNext();
                    /*2012-08-31 Added by amt_chenjing  close cursor after database operation*/
                    wc.close();
                    /*2012-08-31 Add end*/
                }
            }
            context.getContentResolver().update(AppRank.SET_RANK_ALL, values,
                    null, null);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("test", "-----exception:  " + e.getMessage());
        } finally {
            if (c != null) {
                c.close();
            }
            /*2012-08-31 Added by amt_chenjing  close cursor after database operation*/
            if (wc != null) {
            	wc.close();
            }
            /*2012-08-31 Add end*/
        }
    }

    public static void createPreferenceData(boolean reset) {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        boolean init = true;

        if (!reset) {
            init = getGraphySkinBMP(mContext) == 0 ? true : false;
        }

        if (init) {
            editor.putInt("bm0", R.drawable.class_bar_image_big);
            editor.putInt("bm1", R.drawable.class_bg_image_big);
            editor.putInt("bm2", R.drawable.class_bar_image_medium);
            editor.putInt("bm3", R.drawable.class_bg_image_medium);
            editor.putInt("bm4", R.drawable.class_bar_image_medium);
            editor.putInt("bm5", R.drawable.class_bg_image_medium);
            editor.putInt("bm6", R.drawable.class_bar_image_medium);
            editor.putInt("bm7", R.drawable.class_bg_image_medium);
            editor.putInt("bm8", R.drawable.class_bar_image_small);
            editor.putInt("bm9", R.drawable.class_bg_image_small);
            editor.putInt("bm10", R.drawable.class_bar_image_small);
            editor.putInt("bm11", R.drawable.class_bg_image_small);
            editor.putInt("bm12", R.drawable.class_bar_image_small);
            editor.putInt("bm13", R.drawable.class_bg_image_small);
            editor.putInt("bm14", R.drawable.class_bar_image_small);
            editor.putInt("bm15", R.drawable.class_bg_image_small);
            editor.putInt("bm16", R.drawable.class_bar_image_small);
            editor.putInt("bm17", R.drawable.class_bg_image_small);

            editor.putInt(GRAPHY_SKIN, 0xff);
            editor.commit();
        }

    }

    private void setGraphyTime(long time) {
        Log.d(TAG, "set graphy Time " + time);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putLong(GRAPHY_TIME, time);
        editor.commit();
    }

    private void setGraphyRecordDate(int monthDay) {
        Log.d(TAG, "set graphy RecordDate" + monthDay);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt(GRAPHY_DATE, monthDay);
        editor.commit();
    }

    private void setGraphyRecordMonth(int month) {
        Log.d(TAG, "set graphy Record Month" + month);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt(GRAPHY_MONTH, month);
        editor.commit();
    }

    private void setGraphyRecordYear(int year) {
        Log.d(TAG, "set graphy Record Year" + year);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt(GRAPHY_YEAR, year);
        editor.commit();
    }

    static void setMaxCard(int cardNumBer) {
        Log.d(TAG, "set Max Card = " + cardNumBer);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt(MAX_CARD_NUMBER, cardNumBer);
        editor.commit();
    }

    static int getMaxCard() {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        int number = shared.getInt(MAX_CARD_NUMBER, 0);
        Log.d(TAG, "getMaxCard:" + number);
        return number;
    }

    private long getGraphyTime() {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        long time = shared.getLong(GRAPHY_TIME, 0x0);
        return time;
    }

    private int getGraphyRecordYear() {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        int year = shared.getInt(GRAPHY_YEAR, 1980);
        return year;
    }

    private int getGraphyRecordMonth() {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        int month = shared.getInt(GRAPHY_MONTH, 1);
        return month;
    }

    private int getGraphyRecordDate() {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        int date = shared.getInt(GRAPHY_DATE, 1);
        return date;
    }

    static void setGraphyMode(int modeIndex) {
        Log.d(TAG, "set graphy mode:" + modeIndex);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        if (modeIndex >= 0 && modeIndex <= 1) {
            editor.putInt(GRAPHY_MODE, modeIndex);
        } else {
            editor.putInt(GRAPHY_MODE, AUTO_MODE);
        }
        editor.commit();
        Intent intent = new Intent(
                "com.motorola.mmsp.activitygraph.CHANGE_MODE");
        intent.putExtra("widgetmode", modeIndex);
        mContext.sendBroadcast(intent);
    }
    // Added by amt_chenjing for SWITCHUI-2058 20120712 begin
    static void setGraphyModeOnly(int modeIndex) {
        Log.d(TAG, "set graphy mode:" + modeIndex);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        if (modeIndex >= 0 && modeIndex <= 1) {
            editor.putInt(GRAPHY_MODE, modeIndex);
        } else {
            editor.putInt(GRAPHY_MODE, AUTO_MODE);
        }
        editor.commit();
    }
    static void sendModeChange(int modeIndex) {
        Intent intent = new Intent(
                "com.motorola.mmsp.activitygraph.CHANGE_MODE");
        intent.putExtra("widgetmode", modeIndex);
        mContext.sendBroadcast(intent); 
    }
 // Added by amt_chenjing for SWITCHUI-2058 20120712 end
    static void setGraphySkin(int skinIndex) {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt(GRAPHY_SKIN, skinIndex);
        editor.commit();
        Intent intent = new Intent(
                "com.motorola.mmsp.activitygraph.CHANGE_SKIN");
        intent.putExtra("widgetskin", skinIndex);
        mContext.sendBroadcast(intent);
    }

    static void setGraphySkinBMP(int id, int value) {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt("bm" + String.valueOf(id), value);
        editor.commit();
    }

    static int getGraphySkinBMP(Context context) {
        SharedPreferences shared = context.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        int resouceId = shared.getInt("bm0", 0);
        Log.d(TAG, "getGraphySkinBMP0:" + resouceId);
        return resouceId;
    }

    static int getGraphyMode(Context context) {
        SharedPreferences shared = context.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        int mode = shared.getInt(GRAPHY_MODE, 0);
        Log.d(TAG, "getGraphyMode:" + mode);
        return mode;
    }

    static int getGraphySkin() {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        int mode = shared.getInt(GRAPHY_SKIN, 0xff);
        return mode;
    }

    static boolean addHiddenItemToDatabase(Context context, String compenetName) {
        if (compenetName == null) {
            return false;
        }
        mHiddenList.add(compenetName);
        Cursor c = null;
        try {
            c = mResolver.query(Ranks.CONTENT_URI, new String[] { "_id" },
                    Ranks.APPName + "=?" + " and " + Ranks.DAY + "=?",
                    new String[] { compenetName, "-1" }, null);
            if (c != null && (c.moveToFirst()) && c.getCount() > 0) {
                long index = c.getLong(0);
                Log.d(TAG, "add compenentName:" + compenetName);
                ContentValues values = new ContentValues();
                values.put(Ranks.HIDDEN, 1);
                mResolver.update(Ranks.CONTENT_URI, values, "_id=?",
                        new String[] { String.valueOf(index) });

            } else {
                ContentValues values = new ContentValues();
                values.put(Ranks.HIDDEN, 1);
                values.put(Ranks.APPName, compenetName);
                values.put(Ranks.DAY, -1);
                values.put(Ranks.CURRENT_RANK, -1);
                mResolver.insert(Ranks.CONTENT_URI, values);

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return true;
    }

    static boolean removeHiddenItemFromDatabase(Context context, String compName) {
        if (compName == null) {
            return false;
        }
        final ContentValues values = new ContentValues();
        values.put(AppRank.Ranks.HIDDEN, 0);
        mResolver.update(Ranks.CONTENT_URI, values, "appName" + "=?" + " and "
                + Ranks.DAY + "=?", new String[] { compName, "-1" });
        mHiddenList.remove(compName);
        return true;
    }

    static void resetWidget() {
        mResolver.delete(Ranks.CONTENT_URI, Ranks.DAY + ">=?",
                new String[] { "-1" });
        mResolver.delete(ManualList.CONTENT_URI, null, null);
        // Modifyed by amt_chenjing for SWITCHUI-2076 20120712 begin
        //setGraphyMode(AUTO_MODE);
        setGraphyModeOnly(AUTO_MODE);
        // Modifyed by amt_chenjing for SWITCHUI-2076 20120712 end
        createPreferenceData(true);
        mHiddenList.clear();
        mManualList.clear();
        // Settings.System.putInt(mResolver,"box",3);
        Intent intent = new Intent("com.motorola.mmsp.activitygraph.RESET");
        mContext.sendBroadcast(intent);

    }

    static void resetForOutOfBox() {
        mResolver.delete(Ranks.CONTENT_URI, Ranks.DAY + ">=?",
                new String[] { "-1" });
        mResolver.delete(ManualList.CONTENT_URI, null, null);
        setGraphyMode(AUTO_MODE);
        createPreferenceData(true);
        mHiddenList.clear();
        mManualList.clear();
    }

    public static void passDay(Context context) {
        Time t = new Time();
        t.setToNow();

        String sql = "update ranks set day = day+1 where day > -1";
        try {
            context.getContentResolver().update(AppRank.EXECSQL, null, sql,
                    null);
            // TODO:delete days exceed total day number
            context.getContentResolver().delete(Ranks.CONTENT_URI,
                    Ranks.DAY + ">=?",
                    new String[] { String.valueOf(mRankingDays + 1) });
        } catch (Exception e) {
            Log.d(TAG, "exception caught");
            e.printStackTrace();
        }
        updateTotalRanking(context);

    }

    static boolean addManualItemToDatabase(Context context, String compName,
            int index) {
        if (compName == null) {
            return false;
        }
        final ContentValues values = new ContentValues();
        values.put(AppRank.ManualList.APPNAME, compName);
        values.put(AppRank.ManualList.MANUALINDEX, index);
     // Modifyed by amt_chenjing for switchui-2192 20120715 begin
        try {
            mResolver.insert(AppRank.ManualList.CONTENT_URI, values);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
     // Modifyed by amt_chenjing for switchui-2192 20120715 end
        ManualInfo info = new ManualInfo();
        info.mId = index;
        info.mName = compName;
        mManualList.add(info);
        return true;
    }

    static boolean removeManualItemFromDatabase(Context context,
            String compName, int index) {
        if (compName == null) {
            return false;
        }
        String[] arg1 = new String[1];
        arg1[0] = compName;
        mResolver.delete(ManualList.CONTENT_URI, "apps_name" + "=?" + " and "
                + "manual_index" + "=?",
                new String[] { compName, String.valueOf(index) });

        int num = mManualList.size();
        Log.d(TAG, "removeManualItemFromDatabase:" + num);
        for (int i = 0; i < num; i++) {
            ManualInfo info = mManualList.get(i);
            Log.d(TAG, "info:" + info.mName + "index:" + i);
            if (info.mId == index && info.mName.equals(compName)) {
                mManualList.remove(info);
                info = null;
                break;
            }
        }

        return true;
    }

    static boolean updateManualItemToDatabase(Context context, String compName,
            int index) {
        if (compName == null) {
            return false;
        }
        final ContentValues values = new ContentValues();
        values.put(AppRank.ManualList.APPNAME, compName);
        // values.put(AppRank.ManualList.MANUALINDEX, index);
        String[] arg = new String[1];
        arg[0] = Integer.toString(index);
        mResolver.update(AppRank.ManualList.CONTENT_URI, values,
                "manual_index=?", arg);
        int num = mManualList.size();
        for (int i = 0; i < num; i++) {
            ManualInfo info = mManualList.get(i);
            if (info.mId == index) {
                mManualList.remove(i);
                info.mId = index;
                info.mName = compName;
                mManualList.add(i, info);
                break;
            }

        }
        return true;
    }

    /** 2012-07-22, jimmyli added */
    static boolean hiddenAppExists() {
        Cursor c = null;
        boolean exists = false;
        try {
            c = mResolver.query(Ranks.CONTENT_URI, 
                new String[] {"hidden"}, "hidden=1", null, null);
            if (c != null && c.getCount() > 0) {
                Log.d(TAG, "ActivityGraphModel hiddenAppExists hidden count:" + c.getCount());
                exists = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return exists;
    }
    /** jimmyli end */


    static ArrayList<String> getHiddenList() {
        Cursor c = null;
        mHiddenList.clear();
        try {
            Log.d(TAG, "getHiddenList()");
            c = mResolver.query(Ranks.CONTENT_URI, Ranks.PROJECTION, null,
                    null, SORT_ORDER_CURRENT_COUNT);
            if (c == null) {
                Log.d(TAG, "query cursor is null");
            }
            if (c != null && c.getCount() > 0) {
                while (c != null && c.moveToNext()) {
                    String name = c.getString(1);
                    int bHidden = c.getInt(2);
                    if (bHidden != 0 && checkAppResolveInfo(name) != null) {
                        Log.d(TAG, "add to hidden List:" + name);
                        mHiddenList.add(name);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "ERROR");
            e.printStackTrace();
        } finally {
            Log.d(TAG, "finally");
            if (c != null) {
                c.close();
            }
        }
        return mHiddenList;

    }

    static ArrayList<ManualInfo> getManualList() {
        Cursor c = null;
        mManualList.clear();
        try {
            Log.d(TAG, "getManualList()");
            c = mResolver.query(ManualList.CONTENT_URI, ManualList.PROJECTION,
                    null, null, null);
            if (c != null && c.getCount() > 0) {
                while (c != null && c.moveToNext()) {
                    ManualInfo info = new ManualInfo();
                    info.mId = c.getInt(ManualList.INDEX_MANUALINDEX);
                    info.mName = c.getString(ManualList.INDEX_APPNAME);
                    if (info.mName != null
                            && checkAppResolveInfo(info.mName) != null) {
                        mManualList.add(info);
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "ERROR");
            e.printStackTrace();
        } finally {
            Log.d(TAG, "finally");
            if (c != null) {
                c.close();
            }
        }
        return mManualList;
    }

    private void addAppstoDatabase(Context context, String[] applist) {
        int num = applist.length;
        if (num > 0) {
            int mode = getGraphyMode(mContext);
            for (int i = 0; i < num; i++) {
                addApptoDatabase(context, applist[i]);
                Intent intent = new Intent(
                        "com.motorola.mmsp.activitygraph.ADD_APP_PACKAGE");
                intent.putExtra("package name", applist[i]);
                mContext.sendBroadcast(intent);
            }
        }
    }

    private void addExternalAppstoDatabase(Context context, String[] applist) {
        Log.d(TAG, "addExternalAppstoDatabase()");
        int addCount = 0;
        int num = applist.length;
        if (num > 0) {
            int mode = getGraphyMode(mContext);
            Cursor c = null;
            if (mode == AUTO_MODE) {
                try {
                    c = mResolver.query(Ranks.CONTENT_URI, null, "day" + "=?"
                            + " and " + "current_rank" + "!=?", new String[] {
                            "-1", "-1" }, "current_rank DESC");
                    Log.d(TAG, "query rank data");
                    if (c != null && c.getCount() > 0) {
                        Log.d(TAG, "count > 0");
                        c.moveToFirst();
                        while (!c.isAfterLast()) {
                            String name = c.getString(c
                                    .getColumnIndex("appName"));
                            boolean bHidden = c.getInt(c
                                    .getColumnIndex("hidden")) == 0 ? false
                                    : true;
                            Log.d(TAG, "app name:" + name + ", hidden:"
                                    + bHidden);
                            if (name != null && !bHidden) {
                                for (int i = 0; i < num; i++) {
                                    int ind = name.indexOf('/');
                                    if (ind > 1) {
                                        String newname = name.substring(0, ind);
                                        if (newname.equals(applist[i])) {
                                            Log.d(TAG, "find in ranks:" + name);
                                            addCount++;
                                            break;
                                        }
                                    }
                                }
                            }
                            c.moveToNext();
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "get rank error");
                    e.printStackTrace();
                } finally {
                    Log.d(TAG, "finally");
                    if (c != null) {
                        c.close();
                        c = null;
                    }
                }
            } else {
                try {
                    c = mResolver.query(ManualList.CONTENT_URI, null, null,
                            null, "manual_index ASC");
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        while (!c.isAfterLast()) {
                            String name = c.getString(c
                                    .getColumnIndex("apps_name"));
                            int index = c.getInt(c
                                    .getColumnIndex("manual_index"));
                            Log.d(TAG, "app name:" + name + ", index:" + index);
                            if (name != null && !name.equals("")) {
                                for (int i = 0; i < num; i++) {
                                    int ind = name.indexOf('/');
                                    if (ind > 1) {
                                        String appName = name.substring(0, ind);
                                        if (appName.equals(applist[i])) {
                                            Log.d(TAG, "find in manual:" + name);
                                            addCount++;
                                            break;
                                        }
                                    }
                                }
                            }
                            c.moveToNext();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        c.close();
                        c = null;
                    }
                }
            }
            if (addCount > 0) {
                Intent intent = new Intent(
                        "com.motorola.mmsp.activitygraph.UPDATE_EXTERNAL_APPS");
                mContext.sendBroadcast(intent);
            }
        }

    }

    private void removeAppsFromDatabase(Context context, String[] applist,
            int type) {
        int num = applist.length;
        for (int i = 0; i < num; i++) {
            removeAppFromDatabase(context, applist[i], type);
        }
        if (type == REMOVE_PACKAGES) {
            updateAllRanking(mContext);
        }
    }

    private void updateAppstoDatabase(Context context, String[] applist) {
        int num = applist.length;
        for (int i = 0; i < num; i++) {
            updateApptoDatabase(context, applist[i]);
            Intent intent = new Intent(
                    "com.motorola.mmsp.activitygraph.UPDATE_APP_PACKAGE");
            intent.putExtra("package name", applist[i]);
            mContext.sendBroadcast(intent);
        }
        int addCount = 0;
        if (num > 0) {
            int mode = getGraphyMode(mContext);
            Cursor c = null;
            if (mode == AUTO_MODE) {
                try {
                    c = mResolver.query(Ranks.CONTENT_URI, null, "day" + "=?"
                            + " and " + "current_rank" + "!=?", new String[] {
                            "-1", "-1" }, "current_rank DESC");
                    Log.d(TAG, "query rank data");
                    if (c != null && c.getCount() > 0) {
                        Log.d(TAG, "count > 0");
                        c.moveToFirst();
                        while (!c.isAfterLast()) {
                            String name = c.getString(c
                                    .getColumnIndex("appName"));
                            boolean bHidden = c.getInt(c
                                    .getColumnIndex("hidden")) == 0 ? false
                                    : true;
                            Log.d(TAG, "app name:" + name + ", hidden:"
                                    + bHidden);
                            if (name != null && !bHidden) {
                                for (int i = 0; i < num; i++) {
                                    int ind = name.indexOf('/');
                                    if (ind > 1) {
                                        String newname = name.substring(0, ind);
                                        if (newname.equals(applist[i])) {
                                            Log.d(TAG, "find in ranks:" + name);
                                            addCount++;
                                            break;
                                        }
                                    }
                                }
                            }
                            c.moveToNext();
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "get rank error");
                    e.printStackTrace();
                } finally {
                    Log.d(TAG, "finally");
                    if (c != null) {
                        c.close();
                        c = null;
                    }
                }
            } else {
                try {
                    c = mResolver.query(ManualList.CONTENT_URI, null, null,
                            null, "manual_index ASC");
                    if (c != null && c.getCount() > 0) {
                        c.moveToFirst();
                        while (!c.isAfterLast()) {
                            String name = c.getString(c
                                    .getColumnIndex("apps_name"));
                            int index = c.getInt(c
                                    .getColumnIndex("manual_index"));
                            Log.d(TAG, "app name:" + name + ", index:" + index);
                            if (name != null && !name.equals("")) {
                                for (int i = 0; i < num; i++) {
                                    int ind = name.indexOf('/');
                                    if (ind > 1) {
                                        String appName = name.substring(0, ind);
                                        if (appName.equals(applist[i])) {
                                            Log.d(TAG, "find in manual:" + name);
                                            addCount++;
                                            break;
                                        }
                                    }
                                }
                            }
                            c.moveToNext();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        c.close();
                        c = null;
                    }
                }
            }
            if (addCount > 0) {
                Intent intent = new Intent(
                        "com.motorola.mmsp.activitygraph.UPDATE_APPS");
                mContext.sendBroadcast(intent);
            }
        }

    }

    private void addApptoDatabase(Context context, String appName) {
        List<ResolveInfo> matches = findActivitiesForPackage(context, appName);
        final PackageManager pm = context.getPackageManager();
        if (matches.size() > 0) {
            for (ResolveInfo info : matches) {
                ComponentName name = new ComponentName(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name);
                String comp = name.flattenToShortString();

                AppLabelIconInfo appinfo = new AppLabelIconInfo();
                appinfo.appicon = info.loadIcon(pm);
                appinfo.appname = info.loadLabel(pm).toString();
                mAppLabelMap.put(comp, appinfo);

                mAdded.add(comp);
                mAllAppList.add(comp);
            }
        }
    }

    private void addApptoRankData(Context context, String appName) {
        ContentValues values = new ContentValues();
        values.put(Ranks.APPName, appName);
        values.put(Ranks.CURRENT_COUNT, 0);
        values.put(Ranks.CURRENT_RANK, -1);
        values.put(Ranks.HIDDEN, 0);
     // Modifyed by amt_chenjing for switchui-2192 20120715 begin
        try {
            Uri uri = mResolver.insert(Ranks.CONTENT_URI, values);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
     // Modifyed by amt_chenjing for switchui-2192 20120715 end
        // Long dataId = ContentUris.parseId(uri);
    }

    private void removeAppFromDatabase(Context context, String appName, int type) {
        Log.d(TAG, "remove app from database:" + appName);
        ArrayList<String> mAllAppListTemp = new ArrayList<String>();
        Cursor c = null;

        try {
            c = mResolver.query(Ranks.CONTENT_URI,
                    new String[] { "distinct appName" }, null, null, null);
            if ((null != c && c.getCount() > 0)) {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    mAllAppListTemp.add(c.getString(0));
                }
            }
            c = mResolver.query(ManualList.CONTENT_URI,
                    new String[] { "distinct apps_name" }, null, null, null);
            if ((null != c && c.getCount() > 0)) {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    mAllAppListTemp.add(c.getString(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            c.close();
            c = null;
        }
        int num = mAllAppListTemp.size();
        for (int i = 0; i < num; i++) {
            final String name = mAllAppListTemp.get(i);
            Log.d(TAG, "applist index:" + i + ", name:" + name);
            int ind = name.indexOf('/');
            if (ind > 1) {
                String newname = name.substring(0, ind);
                Log.d(TAG, "new name:" + newname);
                if (newname.equals(appName)) {
                    if (type == REMOVE_PACKAGES) {
                        mResolver.delete(Ranks.CONTENT_URI, Ranks.APPName
                                + "=?", new String[] { name });
                        mResolver.delete(ManualList.CONTENT_URI,
                                ManualList.APPNAME + "=?",
                                new String[] { name });
                    }
                    AppLabelIconInfo appinfo = checkAppResolveInfo(name);
                    if (appinfo != null) {
                        mAppLabelMap.remove(name);
                    }
                    removeFromHiddenList(name);
                    removeFromManualList(name);
                    mRemoved.add(name);
                    Intent intent = new Intent(
                            "com.motorola.mmsp.activitygraph.REMOVE_APP_PACKAGE");
                    intent.putExtra("component name", name);
                    mContext.sendBroadcast(intent);
                }
            }
        }
        try {
            mAllAppList.removeAll(mRemoved);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeFromHiddenList(String name) {
        int num = mHiddenList.size();
        if (num > 0) {
            for (int i = 0; i < mHiddenList.size(); i++) {
                String info = mHiddenList.get(i);
                if (info.equals(name)) {
                    mHiddenList.remove(info);
                    info = null;
                    break;
                }
            }
        }
    }

    private void removeFromManualList(String name) {
        int num = mManualList.size();
        ArrayList<ManualInfo> mRemovedList = new ArrayList<ManualInfo>();
        Log.d(TAG, "removeFromManualList:" + num);
        if (num > 0) {
            for (int i = 0; i < num; i++) {
                ManualInfo info = mManualList.get(i);
                Log.d(TAG, "info name:" + info.mName);
                if (info.mName.equals(name)) {
                    mRemovedList.add(info);
                }
            }
            mManualList.removeAll(mRemovedList);
        }
    }

    private void updateApptoDatabase(Context context, String appName) {
        Log.d(TAG, "updateApptoDatabase:" + appName);
        List<ResolveInfo> matches = findActivitiesForPackage(context, appName);
        if (matches.size() > 0) {
            for (ResolveInfo info : matches) {
                ComponentName name = new ComponentName(
                        info.activityInfo.applicationInfo.packageName,
                        info.activityInfo.name);
                String comp = name.flattenToShortString();
                Log.d(TAG, "find comp :" + comp);
                AppLabelIconInfo appinfo = checkAppResolveInfo(comp);
                if (appinfo != null) {
                    mAppLabelMap.remove(comp);
                    mAppLabelMap.put(comp, appinfo);
                }
                mUpdated.add(comp);
            }
        } else {
            ArrayList<String> removed = new ArrayList<String>();
            for (String comp : mAppLabelMap.keySet()) {
                int ind = comp.indexOf('/');
                if (ind > 1) {
                    String newname = comp.substring(0, ind);
                    if (newname.equals(appName)) {
                        removed.add(comp);
                    }
                }
            }
            for (int i = 0; i < removed.size(); i++) {
                mAppLabelMap.remove(removed.get(i));
            }
        }
    }

    private boolean checkOverTime() {
        long t = System.currentTimeMillis();
        long time = getGraphyTime();
        Log.d(TAG, "t:" + t);
        Log.d(TAG, "time :" + time);

        if (time - t >= 24 * 3600 * 1000) {
            Log.d(TAG, "checkOverTime: TRUE");
            return true;
        } else {
            return false;
        }
    }

    private static AppLabelIconInfo checkAppResolveInfo(String appName) {
        AppLabelIconInfo info = null;

        final Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> apps = null;
        apps = pm.queryIntentActivities(main, 0);
        if (apps == null || apps.size() == 0) {
            return null;
        }
        int num = apps.size();
        for (int i = 0; i < num; i++) {
            ResolveInfo item = apps.get(i);
            ComponentName name = new ComponentName(
                    item.activityInfo.applicationInfo.packageName,
                    item.activityInfo.name);
            String appname = name.flattenToShortString();
            if (appName.equals(appname)) {
                info = new AppLabelIconInfo();
                info.appicon = item.loadIcon(pm);
                info.appname = item.loadLabel(pm).toString();
                return info;
            }
        }
        return info;
    }

    /**
     * Query the package manager for MAIN/LAUNCHER activities in the supplied
     * package.
     */
    private static List<ResolveInfo> findActivitiesForPackage(Context context,
            String packageName) {
        final PackageManager packageManager = context.getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);

        final List<ResolveInfo> apps = packageManager.queryIntentActivities(
                mainIntent, 0);
        return apps != null ? apps : new ArrayList<ResolveInfo>();

    }

    private boolean checkSpecialApp(String name) {
        int num = mSpecialApp.length;
        for (int i = 0; i < num; i++) {
            if (mSpecialApp[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void startAlarm(Context context) {
        Intent operator = new Intent(BROADCAST_DAY_PASS);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                operator, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);

        long updateTimes = calendar.getTimeInMillis() + 1000;

        AlarmManager alarm = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        Log.v(TAG,
                "UpdateServie next day pass time = "
                        + new Date(updateTimes).toLocaleString());
        alarm.cancel(pendingIntent);
        alarm.set(AlarmManager.RTC, updateTimes, pendingIntent);
    }

    public void deleteNotFoundAppFromDatabase(String name) {

        try {
            mResolver.delete(ManualList.CONTENT_URI, ManualList.APPNAME + "=?",
                    new String[] { name });
            mResolver.delete(Ranks.CONTENT_URI, Ranks.APPName + "=?",
                    new String[] { name });
            AppLabelIconInfo appinfo = checkAppResolveInfo(name);
            if (appinfo != null) {
                mAppLabelMap.remove(name);
            }
            removeFromHiddenList(name);
            removeFromManualList(name);
            mAllAppList.remove(name);
            updateAllRanking(mContext);
        } catch (Exception e) {
            Log.d(TAG, "delete error");

            e.printStackTrace();
        }
    }

    private static void setGraphyFlexRankingDays(int value) {
        Log.d(TAG, "set graphy ranking days = " + value);
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        SharedPreferences.Editor editor = shared.edit();
        editor.putInt(GRAPHY_FLEX_DAY_VALUE, value);
        editor.commit();
    }

    private static int getGraphyFlexRankingDays() {
        SharedPreferences shared = mContext.getSharedPreferences(
                PREFERENCE_NAME, MODE);
        int value = shared.getInt(GRAPHY_FLEX_DAY_VALUE, 0xff);
        return value;
    }

    private static boolean loadRankingDaysFromFlex() {
        String xmlstr = "";
        TypedValue isCDA = new TypedValue();
        TypedValue contentCDA = new TypedValue();
        Resources r = mContext.getResources();
        try {
            r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
            Log.d(TAG, "Activity isCDA to string = " + isCDA.coerceToString());
            if (isCDA.coerceToString().equals("true")) {
                Log.e(TAG, "is cda true");
                r.getValue("@MOTOFLEX@getActivityGraphSetting", contentCDA,
                        false);
                xmlstr = contentCDA.coerceToString().toString();
                Log.e(TAG, "xmlstr =" + xmlstr);
                if (!xmlstr.trim().equals("")) {
                    Log.e(TAG, "xmlstr get ok");
                } else {
                    Log.e(TAG, "xmlstr is empty");
                    return false;
                }
            } else {
                Log.e(TAG, "is cda false");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "e " + e);
            e.printStackTrace();
            return false;
        }

        Element root = null;
        DocumentBuilderFactory dbf = null;
        DocumentBuilder db = null;

        if ((null != xmlstr) && ("" != xmlstr)) {
            try {
                dbf = DocumentBuilderFactory.newInstance();
                db = dbf.newDocumentBuilder();
                Document xmldoc = db.parse(new InputSource(new StringReader(
                        xmlstr)));
                root = xmldoc.getDocumentElement();
                NodeList list = root.getChildNodes();
                if (list != null) {
                    int len = list.getLength();
                    for (int i = 0; i < len; i++) {
                        Node currentNode = list.item(i);
                        String name = currentNode.getNodeName();
                        String value = currentNode.getTextContent();
                        if (value != null && !value.equals("") && name != null
                                && name.equals(TAG_RANKINGDAYS)) {
                            Log.d(TAG,
                                    "init activity-ranking-days with flex value ok");
                            int days = Integer.valueOf(value);
                            if ((days >= MISC_TOTAL_DAY_MIN)
                                    && (days <= MISC_TOTAL_DAY_MAX)) {
                                setGraphyFlexRankingDays(days);
                                mRankingDays = days;
                                return true;
                            } else {
                                if (name != null) {
                                    Log.d(TAG, "name = " + name);
                                } else {
                                    Log.d(TAG, "name = null");
                                }
                                if (value != null) {
                                    Log.d(TAG, "value = " + value);
                                } else {
                                    Log.d(TAG, "value = null");
                                }
                                Log.d(TAG, "write database ok");
                                return false;
                            }
                        } else {
                            if (name != null) {
                                Log.d(TAG, "name = " + name);
                            } else {
                                Log.d(TAG, "name = null");
                            }
                            if (value != null) {
                                Log.d(TAG, "value = " + value);
                            } else {
                                Log.d(TAG, "value = null");
                            }
                            Log.d(TAG,
                                    "init activity-ranking-days with flex value bad");
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "get from flex fail");
        return false;
    }
}
