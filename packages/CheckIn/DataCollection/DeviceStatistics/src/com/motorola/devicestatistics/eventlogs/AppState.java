package com.motorola.devicestatistics.eventlogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.EventLog.Event;
import android.util.Log;

import com.motorola.devicestatistics.Utils;
import com.motorola.devicestatistics.eventlogs.EventConstants.Events;
import com.motorola.devicestatistics.eventlogs.EventConstants.Source;
import com.motorola.devicestatistics.packagemap.MapDbHelper;


public class AppState extends EventState {
    long mVal;
    long mTime;

    long mFocusPackage;
    boolean mHasFocusApp;
    long mPausedTime;
    boolean mInFocus;
    String mAddLog;
    boolean mChanged;
    String mApp;
    private long mBootTime;
    private Context mContext;
    private MapDbHelper mMapper;
    private IMapperCallback mCallback;
    static AppState mSelf=null;

    private final static boolean DUMP = false;
    private final static String TAG = AppState.class.getSimpleName();
    private final static String PAUSE_RESUME_EVENTS = "pause_resume_events";
    private final static String EVENT_COUNT  = "event_count";
    private final static int MAX_EVENT_COUNT = 500;

    private final static int APP_RESUME = 1;
    private final static int APP_PAUSE = 2;
    private static final int CURRENT_STATE_SHIFT = 0;
    private static final int SCREENON_BIT = 1;
    private static final int SCREENON = 1;
    private static final int PREVIOUS_STATE_SHIFT = 2;



    private AppState(Context context) {
        reset();
        mContext = context;
        mMapper = MapDbHelper.getInstance(mContext);
        mCallback = new IMapperCallback() {
            public long getId(String pkg) {
                long id = mMapper.getId(pkg, true);
                return id;
            }
        };
    }

    void reset() {
        mVal = -1;
        mHasFocusApp = false;
        mFocusPackage = -1;
        mPausedTime = -1;
        mInFocus = false;
        mAddLog = null;
        mApp = null;
        mChanged = false;
    }

    public static synchronized AppState getInstance(Context context) {
        if(mSelf != null) return mSelf;
        else {
            mSelf = new AppState(context);
            return mSelf;
        }
    }

    private boolean isLoggable(int id, String s) {
        boolean loggable = true;
        int index= s.lastIndexOf("]");
        String scrOffOn = s.substring(0, index);
        try {
            if (id == APP_RESUME) { // App Resume
                // resume happening when screen is off should not be logged
                int i = (scrOffOn.equals("null"))?0:Integer.parseInt(scrOffOn);
                loggable = ((i & 1) == 1) ? false : true;
                loggable = (((i << CURRENT_STATE_SHIFT) & SCREENON_BIT) == SCREENON) ? false : true;
            } else if (id == APP_PAUSE) { // App Pause
                // When getting a pause, it needs to be logged only when
                //  the previous resume happened when  screen was on
                int i = (scrOffOn.equals("null"))?0:Integer.parseInt(scrOffOn);
                loggable = ((( i >> PREVIOUS_STATE_SHIFT ) & SCREENON_BIT) == SCREENON) ? false : true;
            }
        } catch (Exception e) {
            // by default log the event
        }
        if (DUMP) Log.v(TAG, "isLoggable : " + loggable);
        return loggable;
    }

    private boolean hasChanged(String log,ILogger logger) {

        // e would be typically in the following format
        //time:1322043324957,am_pause_activity: [1083920776,com.motorola.setupwizard.controller/.ShowSimStatusActivity,-572662307]
        //time:1322209118204,am_resume_activity: [1086698808,4,com.motorola.setupwizard.controller/.ShowSimStatusActivity,2004318071]
        //time:1322222567274,am_relaunch_resume_activity: [1082003104,2,com.motorola.blur.home/.HomeActivity]
        //time:1322222567830,am_restart_activity: [1085242064,3,com.google.android.gsf/.update.SystemUpdateInstallDialog]
        int eventId = getEventId(log);
        if(eventId == -1) return false;

        String[] substrings = log.split(",");
        int compindex = getCompIndex(eventId);
        String comp = substrings[compindex];
        if(substrings.length>compindex+1) {
            if(!isLoggable(eventId, substrings[compindex+1])) return false;
        } else {
            compindex= comp.lastIndexOf("]");
            comp = comp.substring(0, compindex);
        }

        mTime = parseTime(substrings[0]);

        // Assume this is faster than
        // 1. split on '/'
        // 2. parsing forward from start
        int lindex = comp.lastIndexOf('/');
        String pkg = comp.substring(0, lindex);
        mVal = mCallback.getId(pkg);
        int dotidx = comp.lastIndexOf('.');
        mApp = comp.substring(dotidx + 1);

        if(DUMP) {
            Log.v(TAG, "AS: " + mHasFocusApp + "," + mFocusPackage +
                  "," + mPausedTime);
        }

        // RAHUL: For L2 logs, the special logic in the block below is to ensure that
        // pause/resumes within the same package do not get reported. i.e only if the
        // package changes, would the pause/resume get reported.

        switch(eventId) {
        case APP_RESUME: // App resume

            mInFocus = true;
            if(mHasFocusApp) {
                // a resume while we were resumed
                //  - if new pkg, reset to new one
                //  - if same pkg, continue to wait
                if(mFocusPackage == mVal) {
                    // RAHUL: AFAIK, the 5000 here is to force a pause to be reported
                    //   due to screenoff-pause-screenon-resume_of_same_activity.
                    //   Otherwise pause/resumes within the same package wont get reported
                    if(mPausedTime != -1 && mTime - mPausedTime > 5000) {
                        mAddLog = "appau;" + mPausedTime + ";" + mFocusPackage;
                        mChanged = true;
                        mPausedTime = -1;
                    }
                    //mPausedTime = -1;
                } else {
                    if(mPausedTime != -1) {
                        mAddLog = "appau;" + mPausedTime
                                  + ";" + mFocusPackage;
                    }
                    mFocusPackage = mVal;
                    mPausedTime = -1;
                    mChanged = true;
                }
            } else {
                // a new resume, straightforward
                mHasFocusApp = true;
                mFocusPackage = mVal;
                mPausedTime = -1;
                mChanged = true;
            }
            break;
        case APP_PAUSE: // App pause
            mInFocus = false;
            if(mHasFocusApp) {
                // a pause while something was in focus
                // - if same package - record time
                // - if diff package - reset
                if(mFocusPackage == mVal) {
                    mPausedTime = mTime;
                } else {
                    if(mPausedTime != -1) {
                        mAddLog = "appau;" + mPausedTime + ";" + mFocusPackage;
                    }
                    mFocusPackage = -1;
                    mPausedTime = -1;
                    mChanged = true;
                }
            } else {
                // a new pause, reset
                mFocusPackage = -1;
                mPausedTime = -1;
                mChanged = true;
            }
            mHasFocusApp = !mChanged;
            break;
        }
        getLog(eventId, logger);
        return true;
    }

    void getLog(int id, ILogger logger) {
        mTime = EventStatePool.filterEventTime(mBootTime, mTime);
        if(mAddLog != null) {
            logger.log(Source.EVENTLOG, Events.AM_PAUSE_ACTIVITY, EventConstants.CHECKIN_ID,
                       mAddLog);
        }
        switch(id) {
        case 1:
        case 3:
        case 4:
            if (mChanged) {
                logger.log(Source.EVENTLOG, Events.AM_RESUME_ACTIVITY, EventConstants.CHECKIN_ID,
                           "apres;" + mTime + ";" + mVal);
            }
            logger.log(Source.ADDNL_LOG, Events.AM_RESUME_ACTIVITY, EventConstants.CHECKIN_ID,
                       "apres;" + mTime + ";" + mVal + ";" + mApp);
            break;
        case 2:
            if (mChanged) {
                logger.log(Source.EVENTLOG, Events.AM_PAUSE_ACTIVITY, EventConstants.CHECKIN_ID,
                           "appau;" + mTime + ";" + mVal);
            }
            logger.log(Source.ADDNL_LOG, Events.AM_PAUSE_ACTIVITY, EventConstants.CHECKIN_ID,
                       "appau;" + mTime + ";" + mVal + ";" + mApp);
            break;
        }
        mAddLog = null;
        mChanged = false;
    }

    void dumpLog(ILogger logger) {
        if(!mInFocus && mPausedTime != -1) {
            logger.log(Source.EVENTLOG, Events.AM_PAUSE_ACTIVITY, EventConstants.CHECKIN_ID,
                       "appau;" + mPausedTime + ";" + mFocusPackage);
            mFocusPackage = -1;
            mPausedTime = -1;
            mHasFocusApp = false;
        }
        mInFocus = false;
    }

    private int getCompIndex(int id) {
        if(id == APP_RESUME) {
            return 3; // 3rd substring contains comp
        } else {
            return 2; // 2nd Substring contains comp
        }
    }

    private long parseTime(String log) {
        // typical log string is  "time:1322043324957"
        String time= (log.split(":"))[1];
        return Long.parseLong(time);

    }

    public void setBootTime(long boottime) {

        mBootTime = boottime;
    }

    private int getEventId(String log) {
        int id =-1;
        if(log.contains(Events.AM_RESUME_ACTIVITY) ||
                log.contains(Events.AM_RELAUNCH_RESUME_ACTIVITY) ||
                log.contains(Events.AM_RESTART_ACTIVITY)) {
            id = APP_RESUME;
        }
        else if (log.contains(Events.AM_PAUSE_ACTIVITY)) {
            id= APP_PAUSE;
        }
        return id;
    }


    public final void storeEventsInSp(String event, boolean addTimestamp) {
        try {
            storeEventsInSpImpl(event,addTimestamp);
        } catch ( java.lang.OutOfMemoryError exception ) {
            try {
                Log.e( TAG, "No memory for commit", exception );
            } catch ( Exception e ) {
                // Ignore any exception during logging
            }
        } catch (Exception e) {
            // java.lang.OutOfMemoryError is getting reported to blur portal
            Log.e(TAG, "storeEventsInSp", e);
        }
    }

    private final void storeEventsInSpImpl(String event, boolean addTimestamp) {
        String eventlog = null;
        if(addTimestamp) {
            // get the current time and add it to the event log received.
            long time = System.currentTimeMillis();
            eventlog = "time:"+String.valueOf(time)+","+event;
        } else {
            eventlog=event;
        }
        // store the event logs in SP which will be read later.
        SharedPreferences pref = mContext.getSharedPreferences(AppState.PAUSE_RESUME_EVENTS, Context.MODE_PRIVATE);
        synchronized (AppState.class) {
            String storedValue = pref.getString(AppState.PAUSE_RESUME_EVENTS, "");
            SharedPreferences.Editor edit = pref.edit();

            //append the current event with the existing events and also append a separator "##"
            edit.putString(AppState.PAUSE_RESUME_EVENTS, storedValue+eventlog+"##");
            edit.putInt(EVENT_COUNT, (pref.getInt(EVENT_COUNT, 0)+1));
            Utils.saveSharedPreferences(edit);
            if(pref.getInt(EVENT_COUNT, 0) >= MAX_EVENT_COUNT) {
                LogUtil lu = new LogUtil(mContext);
                LogBuilder logger = lu.getLogger();
                addAppEventLogs(logger);
                logger.checkin();
            }
        }
    }

    public void addAppEventLogs(ILogger logger) {

        String storedValue = null;
        SharedPreferences pref = mContext.getSharedPreferences(AppState.PAUSE_RESUME_EVENTS, Context.MODE_PRIVATE);
        synchronized (AppState.class) {
            storedValue = pref.getString(AppState.PAUSE_RESUME_EVENTS, "");
            SharedPreferences.Editor edit = pref.edit();
            edit.putString(AppState.PAUSE_RESUME_EVENTS, "");
            edit.putInt(EVENT_COUNT, 0);
            Utils.saveSharedPreferences(edit);
        }

        // gets the individual events by splitting using the separator ##
        String[] apps = storedValue.split("##");
        for(int i=0; i<apps.length; i++) {
            hasChanged(apps[i], logger);
        }
    }

    @Override
    boolean hasChanged(int id, Event e) {
        // TODO Auto-generated method stub
        return false;
    }
}
