package com.motorola.widgetapp.worldclock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.motorola.widgetapp.worldclock.WorldClockWidgetService;
import com.motorola.widgetapp.worldclock.WorldClockDBAdapter.WidgetDataHolder;

class AnalogClockDrawer {
    // Original bitmap of dial,hour,minute,second
    private Bitmap mBmpDial;
    private Bitmap mBmpHour;
    private Bitmap mBmpMinute;
    private Bitmap mBmpHandstop;

    // Drawable of dial,hour,minute,second
    private BitmapDrawable mBmdHour;
    private BitmapDrawable mBmdMinute;
    // find bugs suggest mark this variable
    //private BitmapDrawable mBmdDial;
    private BitmapDrawable mBmdHandstop;

    int mWidth; // Dial width
    int mHeigh; // Dial height
    int mTempWidth; // Hour/Minute/Second width
    int mTempHeigh; // Hour/Minute/Second height
    int centerX; // Hour/Minute/Second picture center x
    int centerY; // Hour/Minute/Second picture center y

    int availableWidth;// Available width of the dial
    int availableHeight;// Available height of the dial

    public AnalogClockDrawer(Bitmap bmpHour, Bitmap bmpMinute, Bitmap bmpDial,
            Bitmap bmpHandstop) {

        mBmpHour = bmpHour;
        mBmdHour = new BitmapDrawable(mBmpHour);

        mBmpMinute = bmpMinute;
        mBmdMinute = new BitmapDrawable(mBmpMinute);

        mBmpDial = bmpDial;
		// find bugs suggest mark this variable
        //mBmdDial = new BitmapDrawable(mBmpDial);

        mBmpHandstop = bmpHandstop;
        mBmdHandstop = new BitmapDrawable(mBmpHandstop);
		if (null != bmpDial) {
			mBmpDial = bmpDial;
			// find bugs suggest mark this variable
			// mBmdDial = new BitmapDrawable(mBmpDial);
			mWidth = mBmpDial.getWidth();
			mHeigh = mBmpDial.getHeight();
		} else {
            Log.e("WorldClockWidgetProvider","mBmpDial is null");
		}

    }

    public Bitmap drawToBitmap(int hour, int minute) {
        availableWidth = mWidth;
        availableHeight = mHeigh;
        centerX = availableWidth / 2;
        centerY = availableHeight / 2;
        float hourRotate = hour * 30.0f + minute / 60.0f * 30.0f;
        float minuteRotate = minute * 6.0f;

        Bitmap newb = Bitmap.createBitmap(availableWidth, availableHeight,
                Config.ARGB_8888);
        Canvas canvas = new Canvas(newb);

        // Draw Minute on canvas
        mTempWidth = mBmpMinute.getWidth();
        mTempHeigh = mBmpMinute.getHeight();
        canvas.save();
        canvas.rotate(minuteRotate, centerX, centerY);
        mBmdMinute.setBounds(centerX - (mTempWidth / 2), centerY
                - (mTempHeigh / 2), centerX + (mTempWidth / 2), centerY
                + (mTempHeigh / 2));
        mBmdMinute.draw(canvas);
        canvas.restore();

        // Draw hour on canvas
        mTempWidth = mBmpHour.getWidth();
        mTempHeigh = mBmpHour.getHeight();
        canvas.save();// Save non rotated canvas
        canvas.rotate(hourRotate, centerX, centerY);
        mBmdHour.setBounds(centerX - (mTempWidth / 2), centerY
                - (mTempHeigh / 2), centerX + (mTempWidth / 2), centerY
                + (mTempHeigh / 2));
        mBmdHour.draw(canvas);

        canvas.restore(); // restore canvas to last saved state

        // Draw Handstop on canvas
        mTempWidth = mBmpHandstop.getWidth();
        mTempHeigh = mBmpHandstop.getHeight();
        mBmdHandstop.setBounds(centerX - (mTempWidth / 2), centerY
                - (mTempHeigh / 2), centerX + (mTempWidth / 2), centerY
                + (mTempHeigh / 2));
        mBmdHandstop.draw(canvas);

        canvas.save(Canvas.ALL_SAVE_FLAG);
        canvas.restore();

        return newb;

    }
}

public class WorldClockWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "WorldClockWidgetProvider";
    public static final String ACTION_WORLDCLOCK_UPDATE = "com.motorola.widgetapp.worldclock.WORLDCLOCKUPDATE";
    public static final String ACTION_WORLDCLOCK_UPDATE_ALL = "com.motorola.widgetapp.worldclock.WORLDCLOCKUPDATEALL";
    public static final String ACTION_WORLDCLOCK_TIMEZONE_CHANGE = "com.motorola.widgetapp.worldclock.TIMEZONECHANGE";
    public static final String APPWIDGETID = "worldClockAppWidgetId";
    public static final String SHOWBUTTON = "worldClockButtonActive";
    public static final String CURRENTCITY = "worldClockCurrentCity";
    public static final int WIDGET_ALL_BUTTON_ACTIVE = 0;
    public static final int WIDGET_ADD_BUTTON_ACTIVE = 1;
    public static final int WIDGET_ALL_BUTTON_INACTIVE = 2;
    private static final int TIME_FORMAT_DIGITAL_LENGTH = 5;

    private static final ComponentName THIS_APPWIDGET = new ComponentName(
            "com.motorola.widgetapp.worldclock",
            "com.motorola.widgetapp.worldclock.WorldClockWidgetProvider");

    private Context mContext;
    private WorldClockDBAdapter mDbHelper;
    private WidgetDataHolder mData;
    private boolean mClockStyle;
    private boolean misLandscape;
    private AnalogClockDrawer mAnalogClock = null;

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // TODO Auto-generated method stub
        super.onDeleted(context, appWidgetIds);
        int length = appWidgetIds.length;
        for (int i = 0; i < length; i++) {
            mDbHelper.deleteWidgetData(appWidgetIds[i]);
        }
    }

    @Override
    public void onDisabled(Context context) {
        // TODO Auto-generated method stub
        super.onDisabled(context);
        Log.d(TAG, "onDisabled");
        stopRealTimeMonitorService(context);
        mDbHelper.clearWorldClockTable();
    }

    @Override
    public void onEnabled(Context context) {
        // TODO Auto-generated method stub
        super.onEnabled(context);
        Log.d(TAG, "onEnabled");
        startRealTimeMonitorService(context);
    }

    private void startRealTimeMonitorService(Context context) {
        Log.e(TAG, "startRealTimeMonitorService");
        Intent intent = new Intent(
                WorldClockWidgetService.ACTION_WORLDCLOCK_REALTIMESERVICE);
        context.startService(intent);
    }

    private void stopRealTimeMonitorService(Context context) {
        Log.e(TAG, "stopRealTimeMonitorService");
        Intent intent = new Intent(context, WorldClockWidgetService.class);
        context.stopService(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        mContext = context;
        mDbHelper = new WorldClockDBAdapter(context);
        mDbHelper.open();
        final String action = intent.getAction();

        if (ACTION_WORLDCLOCK_UPDATE_ALL.equals(action)) {
            onMinuteUpdate(context, false);
        } else if (ACTION_WORLDCLOCK_TIMEZONE_CHANGE.equals(action)) {
            onMinuteUpdate(context, true);
        } else if (ACTION_WORLDCLOCK_UPDATE.equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                int appWidgetId = extras.getInt(APPWIDGETID);
                int buttonStatus = extras.getInt(SHOWBUTTON,
                        WIDGET_ALL_BUTTON_INACTIVE);
                startRealTimeMonitorService(mContext);
                Log.d(TAG, "appWidgetId is " + appWidgetId);
                // Log.e(TAG, "buttonActive is " + buttonStatus);
                forceUpdate(context, appWidgetId, buttonStatus);
            }
        }

        super.onReceive(context, intent);
        mDbHelper.close();

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // TODO Auto-generated method stub
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        int length = appWidgetIds.length;
        for (int i = 0; i < length; i++) {
            RemoteViews views = buildRemoteView(appWidgetIds[i],
                    WIDGET_ALL_BUTTON_INACTIVE);
            refreshTime(views, appWidgetIds[i]);
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);
        }
    }

    private void onMinuteUpdate(Context context, boolean tzChange) {
        int length = 0;
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(THIS_APPWIDGET);
        if ((appWidgetIds == null) || ((length = appWidgetIds.length) == 0))
            return;

        for (int i = 0; i < length; i++) {
            Log.d(TAG, "onMinuteUpdate, appWidgetId = " + appWidgetIds[i]);
            RemoteViews views = buildRemoteView(appWidgetIds[i],
                    WIDGET_ALL_BUTTON_INACTIVE);
            if (tzChange) {
                checkLocalTimezone(appWidgetIds[i]);
            }
            refreshTime(views, appWidgetIds[i]);
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);
        }
    }

    private void forceUpdate(Context context, int appWidgetId, int iconStatus) {
        RemoteViews views = buildRemoteView(appWidgetId, iconStatus);
        refreshTime(views, appWidgetId);
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private RemoteViews buildRemoteView(int appWidgetId, int iconStatus) {

        misLandscape = (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        mData = mDbHelper.queryWidgetData(appWidgetId);
        // For preload widgets
        if (mData == null) {
            Log.e(TAG, "LOAD DEFAULT DATA HERE!!!!!!!");
            mDbHelper.deleteWidgetData(appWidgetId);
            mData = loadDefaultSettings(appWidgetId, mContext, mDbHelper);
        }

        mClockStyle = mData.mClockStyle;
        mData=hiddenCityCheck(mData);
        Log.i(TAG, "cloclstyle = " + mData.mClockStyle);

        int layoutID = getLayoutId(misLandscape, mClockStyle);

        Log.i(TAG, "Orientation landscape=" + misLandscape);
        RemoteViews views = new RemoteViews(mContext.getPackageName(), layoutID);

        final ComponentName serviceName = new ComponentName(mContext,
                WorldClockPreferenceActivity.class);
        Intent intent = new Intent(mContext, WorldClockPreferenceActivity.class);
        intent.setComponent(serviceName);
        intent.setData(Uri.parse(String.valueOf(appWidgetId)));
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                intent, 0);
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent);

        return views;
    }

    private int getLayoutId(boolean isLandscape, Boolean isAnalog) {
        if (isAnalog) {
            return R.layout.widget_worldclock_analog;
        } else {
            return R.layout.widget_worldclock_digital;
        }

    }

    private void checkLocalTimezone(int appWidgetId) {
        Log.d(TAG, "checkLocalTimezone, mData.mUseCurrentLocation=" + mData.mUseCurrentLocation);
        if (mData.mUseCurrentLocation) {
            String[] timeZoneIds = mData.mTimeZoneId;
            int[] timeZoneStringIds= mData.mTimeZoneStringId;
            String[] displayNames = mData.mDisplayName;
            String localTimezone = WorldClockPreferenceActivity.getTimeZoneText();
            Log.d(TAG, "timeZoneIds[1]=" + timeZoneIds[1] + ", localTimezone=" + localTimezone);
            if (timeZoneIds[1] != null && !timeZoneIds[1].equals(localTimezone)) {
                // Timezone has changed, need update data of city id 1
                displayNames[1] = localTimezone;
                timeZoneIds[1] = localTimezone;
                timeZoneStringIds[1] = 0;
                mData.mAdjustDSTStatus[1] = WorldClockPreferenceActivity.isCityInDST(null);
                mDbHelper.updateWidgetData(appWidgetId, mData);
            }
        }
    }

    private void refreshTime(RemoteViews views, int appWidgetId) {
        if (mAnalogClock == null) {
            mAnalogClock = new AnalogClockDrawer(BitmapFactory.decodeResource(
                    mContext.getResources(), R.drawable.hour),
                    BitmapFactory.decodeResource(mContext.getResources(),
                            R.drawable.minute), BitmapFactory.decodeResource(
                            mContext.getResources(), R.drawable.dial),
                    BitmapFactory.decodeResource(mContext.getResources(),
                            R.drawable.handstop));
        }
        views.setViewVisibility(R.id.wclock_divider, View.VISIBLE);
        final String[] timeZoneIds = mData.mTimeZoneId;
        final int[] timeZoneStringIds= mData.mTimeZoneStringId;
        int cityId;
        for (cityId = 0; cityId < 2; cityId++) {
            String[] displayNames = mData.mDisplayName;
            // stub code to test no city selected
            if (mData.mDisplayName[cityId].length() == 0) {
                if (cityId == 0) {
                    views.setViewVisibility(R.id.Clock1, View.GONE);
                    views
                            .setViewVisibility(R.id.CityNotSelected1,
                                    View.VISIBLE);
                } else {
                    views.setViewVisibility(R.id.Clock2, View.GONE);
                    views
                            .setViewVisibility(R.id.CityNotSelected2,
                                    View.VISIBLE);
                }
                continue;
            }

            if (mData.mCurrentLocale != null
                    && !mData.mCurrentLocale.equalsIgnoreCase(Locale
                            .getDefault().toString())) {
                mData.mCurrentLocale = Locale.getDefault().toString();
                Log.w(TAG, "Locale = " + mData.mCurrentLocale);
                for (int i = 0; i < WorldClockPreferenceActivity.MAX_CITY_NUM; i++) {
                    if (displayNames[i].length() != 0) {
                        Log.w(TAG, "displayNames[" + i + "] = " + displayNames[i]);
                        Log.w(TAG, "timeZoneStringIds[" + i + "] = " + timeZoneStringIds[i]);
                        if (timeZoneStringIds[i] == 0) {
                            displayNames[i] = WorldClockPreferenceActivity
                                    .getCityNameFromTimeZoneId(timeZoneIds[i], mContext);
                        } else {
                            displayNames[i] = mContext.getString(timeZoneStringIds[i]);
                        }
                        Log.w(TAG, "update displayNames[" + i + "] as " + displayNames[i]);
                    }
                }
                mDbHelper.updateWidgetData(appWidgetId, mData);
            }

            Log.v(TAG, "  " + timeZoneIds[cityId]);

            TimeZone timeZone = TimeZone.getTimeZone(timeZoneIds[cityId]);

            Calendar c = Calendar.getInstance(timeZone);
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);
            int mHour = c.get(Calendar.HOUR_OF_DAY);
            int mMinute = c.get(Calendar.MINUTE);
            int mSecond = c.get(Calendar.SECOND);

            Date d = new Date(mYear - 1900, mMonth, mDay, mHour, mMinute,
                    mSecond);
            // if user adjust the DST status manually, adjust date accordingly
            boolean inDSTWithoutAdjust;
            if (timeZoneIds[cityId].equals(WorldClockPreferenceActivity.getTimeZoneText())){
                inDSTWithoutAdjust = WorldClockPreferenceActivity.isCityInDST(null);
            } else {
                inDSTWithoutAdjust = WorldClockPreferenceActivity.isCityInDST(timeZoneIds[cityId]);
            }
            if (mData.mAdjustDSTStatus[cityId]) {
                if (!inDSTWithoutAdjust) {
                        d.setTime(d.getTime() + 3600000);
                    }
                } else {
                if (inDSTWithoutAdjust) {
                        d.setTime(d.getTime() - 3600000);
                }
            }
            String displayName = displayNames[cityId];

            int stringId = mContext.getResources().getIdentifier(displayName,
                    "string", null);
            if (stringId != 0) {
                displayName = mContext.getResources().getString(stringId);
            }
            updateTimeForImageView(views, d, cityId, displayName);
        }

    }

    private void updateTimeForImageView(RemoteViews views, Date d, int cityId,
            String displayName) {
        String time = getTimeFormat().format(d);
        String amStatusDisplay;
        Log.d(TAG, "time=" + time + ", cityId=" + cityId);
        int idAnalogClock;
        int idAnalogAMPMLable;
        int idDigitalClock;
        int idDigitalAMPMLable;
        int idCityContainer;
        int idCityNotSelected;
        int idCityName;
        int idDate;

        if (cityId == 0) {
            idAnalogClock = R.id.analog1;
            idAnalogAMPMLable = R.id.analogAMPM1;
            idDigitalClock = R.id.Time1;
            idDigitalAMPMLable = R.id.digitalAMPM1;
            idCityContainer = R.id.Clock1;
            idCityNotSelected = R.id.CityNotSelected1;
            idCityName = R.id.City1;
            idDate = R.id.Date1;
        } else {
            idAnalogClock = R.id.analog2;
            idAnalogAMPMLable = R.id.analogAMPM2;
            idDigitalClock = R.id.Time2;
            idDigitalAMPMLable = R.id.digitalAMPM2;
            idCityContainer = R.id.Clock2;
            idCityNotSelected = R.id.CityNotSelected2;
            idCityName = R.id.City2;
            idDate = R.id.Date2;
        }

        views.setViewVisibility(idCityNotSelected, View.GONE);
        views.setViewVisibility(idCityContainer, View.VISIBLE);

        views.setTextViewText(idCityName, displayName.split("/")[0]);
        SimpleDateFormat dateformat = new SimpleDateFormat("MMM dd");
//        DateFormat dateformat = DateFormat.getDateInstance(DateFormat.LONG);
//        views.setTextViewText(idDate, dateformat.format(d));
        views.setTextViewText(idDate, DateUtils.formatDateTime(mContext, d.getTime(),
                DateUtils.FORMAT_ABBREV_ALL | DateUtils.FORMAT_SHOW_DATE));

        amStatusDisplay = getAmPmDisplayString(d);
        if (mClockStyle) {
            Bitmap analogclock = mAnalogClock.drawToBitmap(d.getHours() % 12, d
                    .getMinutes());
            views.setImageViewBitmap(idAnalogClock, analogclock);
            views.setTextViewText(idAnalogAMPMLable, amStatusDisplay);
            views.setViewVisibility(idAnalogAMPMLable, View.VISIBLE);
            views.setViewVisibility(R.id.wclock_divider, View.GONE);

        } else {
            String displayTime=null;
            if (time.substring(0, 1).equals("0")) {
                displayTime = time.substring(1, TIME_FORMAT_DIGITAL_LENGTH);
            } else {
                displayTime = time.substring(0, TIME_FORMAT_DIGITAL_LENGTH);
            }
            if (displayTime != null) {
                views.setTextViewText(idDigitalClock, displayTime);
            }

            if (!is24HourFormat()) {
                views.setViewVisibility(idDigitalAMPMLable, View.VISIBLE);
                views.setTextViewText(idDigitalAMPMLable, amStatusDisplay);
                views.setViewVisibility(idDigitalAMPMLable, View.VISIBLE);
            } else {
                views.setViewVisibility(idDigitalAMPMLable, View.GONE);
            }
        }
    }

    private String getAmPmDisplayString(Date d) {
        if (d.getHours() / 12 == 0)
            return mContext.getString(R.string.am);
        else
            return mContext.getString(R.string.pm);
    }

    // -- Date,Time,TimeZone Format Facilities
    // -----------------------------------------------
    private final java.text.DateFormat getTimeFormat() {
        boolean b24 = is24HourFormat();
        return new java.text.SimpleDateFormat(b24 ? mContext.getString(R.string.time_format_24) : mContext.getString(R.string.time_format_12));
    }

    private boolean is24HourFormat() {
        return android.text.format.DateFormat.is24HourFormat(mContext);
    }

    public static WidgetDataHolder loadDefaultSettings(int appWidgetId,
            Context context, WorldClockDBAdapter dbHelper) {
        int displayCityNumber = 1;
        final int maxCityNumber = WorldClockPreferenceActivity.MAX_CITY_NUM;
        boolean[] adjustDSTStatus = new boolean[maxCityNumber];
        String[] displayName = new String[maxCityNumber];
        String[] timeZoneId = new String[maxCityNumber];
        int[] timeZoneStringId = new int[maxCityNumber];
        final Resources res = context.getResources();
        boolean[] misCityHidden=new boolean[maxCityNumber];

        if (context.getString(R.string.default_worldclock_config)
                .equalsIgnoreCase("SYNC_WITH_")) {
            timeZoneId[0] = TimeZone.getDefault().getID();
            timeZoneStringId[0] = 0;
            displayName[0] = WorldClockPreferenceActivity
                    .getCityNameFromTimeZoneId(timeZoneId[0], context);
        } else { // load from xml
            for (int i = 0; i < maxCityNumber; i++) {
                String resName = "default_worldclock_timezone_id" + i;
                int resId = res.getIdentifier(resName, "string", context
                        .getPackageName());
                if (resId != 0)
                    timeZoneId[i] = context.getString(resId);
                timeZoneStringId[i] = 0;
                adjustDSTStatus[i] = false; // not adjust
                displayName[i] = "";
                misCityHidden[i]=false;

            }
        }

        // set the first city as current default display
        // mCurrentDisplay.setDefault(0);
        adjustDSTStatus[0] = false;
        int currentDisplayCity = 0;
        misCityHidden[0]=false;
        misCityHidden[1]=false;

        Cursor currentlocaleCursor = dbHelper.queryCurrentlocale();
        String currentLocale = currentlocaleCursor
                .getString(WorldClockDBAdapter.CURRENTLOCALE_INDEX_LOCALE);
        currentlocaleCursor.close();
        if (!currentLocale.equalsIgnoreCase(Locale.getDefault().toString())) {
            currentLocale = Locale.getDefault().toString();
            dbHelper.updateCurrentlocale(1, currentLocale);
            //Update city name of timezone table in database during locale change
            dbHelper.updateCityName();
        }

        WidgetDataHolder mydata = new WidgetDataHolder();
        mydata.mAdjustDSTStatus = adjustDSTStatus;
        mydata.mDisplayName = displayName;
        mydata.mTimeZoneId = timeZoneId;
        mydata.mTimeZoneStringId = timeZoneStringId;
        mydata.mCurrentDisplayCity = currentDisplayCity;
        mydata.mCurrentLocale = currentLocale;
        mydata.mClockStyle = true;
        mydata.mUseCurrentLocation = false;
        mydata.misCityHidden=misCityHidden;
        dbHelper.insertWidgetData(appWidgetId, mydata);
        return mydata;
    }
    public static WidgetDataHolder hiddenCityCheck(WidgetDataHolder mydata){
    	final int maxcitynum=WorldClockPreferenceActivity.MAX_CITY_NUM;
        for(int i=0;i<maxcitynum;i++){
            if(mydata.misCityHidden[i]==true){
                mydata.mDisplayName[i]="";
                mydata.mTimeZoneId[i]="";
                mydata.mTimeZoneStringId[i]=0;
            }
    	}
    	return mydata;
    }

}
