/**
 * Copyright (C) 2011, Motorola, Inc,
 * All Rights Reserved
 * Class name: EventLoggerService.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Aug 3, 2010	      a24178      Created file
 * Aug 19, 2011       a14813       Created background thread
 **********************************************************
 */

package com.motorola.devicestatistics.eventlogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Process;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Log;

import com.motorola.devicestatistics.CheckinHelper;
import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.DevStatPrefs;
import com.motorola.devicestatistics.SettingsStat;
import com.motorola.devicestatistics.Utils;
import com.motorola.devicestatistics.eventlogs.EventConstants.Debug;
import com.motorola.devicestatistics.eventlogs.EventConstants.RawEvents;
import com.motorola.devicestatistics.eventlogs.EventConstants.Source;
import com.motorola.devicestatistics.packagemap.MapDbHelper;

public final class EventLoggerService extends Thread {
    private final static boolean DUMP = false;
    private final static String TAG = "EventLoggerService";

    static {
        if ( DUMP ) Log.v( TAG, "Loading devicestats library" );
        System.loadLibrary("devicestats");
    }

    // Background thread typically checks the eventlog buffer level, after this much time has
    // elapsed, since the last eventlog scan
    private static final int SCAN_PERIOD_FAST_MS = (5*60*1000); // 5 mins

    // If less than FORCE_SCAN_MS has elapsed, and the event buffer has less than
    // MIN_BUFFERLEVEL_SCAN_PERCENT data, then no eventlog scan is done. This is to limit
    // filesystem operations
    private static final long FORCE_SCAN_MS = (45*60*1000); // 45 mins
    private static final long MIN_BUFFERLEVEL_SCAN_PERCENT = 5; // 5%

    // Don't add overflow reports if the time elapsed is less than 30 secs
    // This is to avoid an unnecessary report at boot complete
    private static final long MIN_ELAPSED_MS = (30*1000); // 30 seconds

    // event logs are scanned only if at least this much time has elapsed
    private static final long MINIMUM_TIME_BETWEEN_SCAN_MS = 3 * 60 * 1000; // 3 mins

    // For debugging
//    private static final int SCAN_PERIOD_FAST_MS = (30*1000); // 0.5 mins
//    private static final long FORCE_SCAN_MS = (2*60*1000); // 2 mins
//    private static final long MIN_BUFFERLEVEL_SCAN_PERCENT = 5; // 5%
//    private static final long MIN_ELAPSED_MS = (5 * 1000); // 5 seconds
//    private static final long MINIMUM_TIME_BETWEEN_SCAN_MS = 10 * 1000; // 10 sec


    // The maximum size of the overflow report written daily to checkin
    private static final long MAX_OVERFLOW_LOGSIZE = 8192;

    // To do sanity check on SystemClock.elapsedRealtime
    private static final long MAX_ELAPSED_MS = (86400*1000); // 1 day

    // If there is greater than eventbuffersize-MAX_OVERFLOW_LOGSIZE data to be incrementally read,
    //   the event log buffer is considered to have overflowed.
    private static final long LOGGER_FULL_THRESHOLD = 8 * 1024; // 8 KB

    // background thread wakeup reasons
    public static final int WAKEUP_SHUTDOWN = 0;
    public static final int WAKEUP_ALARM = 1;
    public static final int WAKEUP_SCREENOFF = 2;
    public static final int WAKEUP_FORCE_READ = 3;

    private static final int DEVICESTATS_EVENT_PARAM_LOGGER_MAX_SIZE = 0;
    private static final int DEVICESTATS_EVENT_PARAM_LOGGER_CURRENT_SIZE = 1;
    private static final int DEVICESTATS_EVENT_PARAM_FIRST_LOG_TIME = 2;
    private static final int DEVICESTATS_EVENT_PARAM_SANITY_ERRORS = 3;

    private static final String BGTHREAD_NAME = "devstatsEv";

    // The priority for this background thread.
    //  Give it a lower priority than background threads
    private static final int BGTHREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND +
        Process.THREAD_PRIORITY_LESS_FAVORABLE;

    private final Context mContext;
    private final SharedPreferences mSp;
    private boolean mThreadStarted, mShutdownInProgress, mForceEventRead;
    private MapDbHelper mMapper;
    private IMapperCallback mCb;
    private long mLastScanRealTime, mLastScanElapsedTime, mSanityErrors;
    private final Object mWaitObject = new Object();
    private int mBgThreadTid = -1;
    private Object mLock = new Object();
    private int mScanPeriodMs = SCAN_PERIOD_FAST_MS;

    // Single sInstance of this class
    private static EventLoggerService sInstance;

    private native void nativeInit();
    private native void nativeReadEvents(int[] ids, Collection<EventLog.Event> events) throws IOException;
    private native long nativeGetEventParam(int type);

    class EventLogger implements Runnable{

        final static boolean DBG = false;

        Context mContext;
        EventStatePool mPool;
        SharedPreferences mSp;

        EventLogger(Context context) {
            mContext = context.getApplicationContext();
            mPool = EventStatePool.getStatePool(mCb);
            mSp = mContext.getSharedPreferences(EventConstants.OPTIONS,
                    Context.MODE_PRIVATE);
        }

        public void run() {
            try {
                synchronized(mLock) {
                    long start = System.currentTimeMillis();

                    LogUtil lu = new LogUtil(mContext);
                    LogBuilder logger = lu.getLogger();
                    mPool.setBootTime(getBootTime(mSp));

                    synchronized(mMapper) {
                        mMapper.startLog();
                        readLogs(lu.getConfig().getEventConfig(), logger);
                        HashMap<String, Long> cl = mMapper.stopLog();
                        cleanupMapper(cl, logger);
                    }
                    addToCheckin(logger);
                    logger.reset();
                    //Add an overflow log if we hit a log full
                    updateSizeStatus(logger, lu.getController(), lu.isReset(),
                                             lu.getOflowCounters(), lu.getMissedEvCounters());
                    addToCheckin(logger);

                    if(DBG) {
                        long end = System.currentTimeMillis();
                        long thisTime = end - start;
                        SharedPreferences.Editor spe = mSp.edit();
                        long history = mSp.getLong(Debug.RUNTIME, 0);
                        long runs = mSp.getLong(Debug.RUNS, 0);
                        runs++;
                        history += thisTime;
                        spe.putLong(Debug.RUNTIME, history);
                        spe.putLong(Debug.RUNS, runs);
                        Utils.saveSharedPreferences(spe);
                    }
                }
            }catch(Exception e) {
                Log.v(TAG, "EventLoggerThread: got exception:", e);
            }
        }

        private long getBootTime(SharedPreferences sp) {
            SharedPreferences.Editor spe = sp.edit();

            long boottime = -1;
            int seq = sp.getInt(EventConstants.BOOTTIME_SEQNUM, 0);
            if(seq == 1) {
                boottime = sp.getLong(EventConstants.BOOTTIME_REFERENCE, -1);
                boottime = boottime < EventConstants.DATE_LOWLIMIT ? -1 : boottime;
            }
            spe.putInt(EventConstants.BOOTTIME_SEQNUM, 0);
            Utils.saveSharedPreferences(spe);
            return boottime;
        }

        private void cleanupMapper(HashMap<String, Long> cl, ILogger logger) {
            if(!cl.isEmpty()) {
                Iterator<String> keys = cl.keySet().iterator();
                while(keys.hasNext()) {
                    String key = keys.next();
                    Long l = cl.get(key);
                    if(l != null) {
                        logger.log(Source.HELPER, -1, "PMaps", "m;" + l + ";" + key );
                    }
                }
            }
        }

        private void updateSizeStatus(ILogger logger, LogController controller,
                boolean isReset, int[] oflowCounters, int[] missedEvCounters) {
            int[] sizes = controller.getUpdatedSizes();
            StringBuilder sb = new StringBuilder();

            encodeSizes(sizes, ",", sb);
            SharedPreferences.Editor ed = mSp.edit();
            ed.putString(EventConstants.SIZECOUNTER, sb.toString());

            sb.setLength(0);
            if(isReset && controller.isOflow(oflowCounters)) {
                encodeSizes(oflowCounters, ";", sb);
                if(sb.length() > 0) {
                    sb.insert(0, "oflow;");
                    logger.log(Source.HELPER, -1,
                            EventConstants.CHECKIN_ID, sb.toString());
                }
                sb.setLength(0);
                encodeSizes(missedEvCounters, ";", sb);
                Log.v(TAG, "Update " + sb.toString());
                if(sb.length() > 0) {
                    sb.insert(0, "missev;");
                Log.v(TAG, "Update2 " + sb.toString());
                    logger.log(Source.HELPER, -1,
                            EventConstants.CHECKIN_ID, sb.toString());
                }
                sb.setLength(0);
             }

            encodeSizes(controller.getOflowSizes(), ",", sb);
            ed.putString(EventConstants.OFLOWSIZECOUNTER, sb.toString());
            sb.setLength(0);
            encodeSizes(controller.getMissedEvents(), ",", sb);
            ed.putString(EventConstants.MISSEDEVENTCOUNTER, sb.toString());
            Utils.saveSharedPreferences(ed);
        }

        private void encodeSizes(int[] sizes, final String separator, StringBuilder sb) {
            sb.setLength(0);
            sb.append(Integer.toString(sizes[0]));
            for(int i = 1; i < sizes.length; ++i) {
                sb.append(separator).append(sizes[i]);
            }
        }

        private void readLogs(String[] filter, ILogger logger) {
            long now = System.currentTimeMillis();

            // first get the tags that are present
            int[] ids = new int[filter.length];
            for(int j = 0; j < filter.length; ++j) {
                if(filter[j] == null) {
                    ids[j] = -1;
                }else if(RawEvents.isRawEvent(filter[j])) {
                    Integer id = Integer.parseInt(filter[j]);
                    ids[j] = id == null ? -1 : id;
                }else {
                    ids[j] = EventLog.getTagCode(filter[j]);
                    if(DUMP) Log.v(TAG, "Adding id " + ids[j] + " for tag " + filter[j]);
                }
            }
            ArrayList<EventLog.Event> events = new ArrayList<EventLog.Event>();
            try {
                // The native code takes care of returning only incremental logs, so long as our
                // process has not crashed.
                nativeReadEvents(ids, events);
            } catch (IOException e) {
                Log.v(TAG, "Event logger thread, failed reading events");
            }

            long from = mSp.getLong(EventConstants.TIME_REFERENCE, 0);

            int N = events.size();

            if(N > 0) {
                SharedPreferences.Editor spe = mSp.edit();
                spe.putLong(EventConstants.TIME_REFERENCE, now);
                Utils.saveSharedPreferences(spe);
            }

            if ( from > now ) from = 0; // say, if someone moved the time back

            if(DUMP) Log.v(TAG, "Event logger thread, found " + N + " events");

            for(int i = 0; i < N; ++i) {
                EventLog.Event e = events.get(i);
                long nanos = e.getTimeNanos();
                nanos = nanos / (1000 * 1000);
                if(DUMP) Log.v(TAG, "nanos:" + nanos + ",from:" + from);
                if(nanos < from) continue;

                int id = e.getTag();
                for(int k = 0; k < ids.length; ++k) {
                    if(id == ids[k]) {
                        if(DUMP) Log.v(TAG, "id:" + id + ",pre:" + ids[k]);
                        //addLog(filter[k], e);
                        addLog(k, e, logger);
                        break;
                    }
                }
            }

            AppState es = AppState.getInstance(mContext);
            es.setBootTime(getBootTime(mSp));
            es.addAppEventLogs(logger);

            // This is needed as of now primarily for app logs
            mPool.dumpLog(logger);
            addMiscLogs(logger, mSp);
        }

        private boolean addLog(int id, EventLog.Event e, ILogger logger) {
            if(mPool.hasChanged(id, e)) {
                mPool.getLog(id, logger);
                return true;
            }
            return false;
        }

        private void addMiscLogs(ILogger logger, SharedPreferences sp) {
            EventNote.getEvents(sp, logger);
            SettingsStat.checkInSettingsData(sp, logger);
        }

        private void addToCheckin(ILogger logger) {
            logger.checkin();
        }
    }

    public EventLoggerService(Context context) {
        super( BGTHREAD_NAME );

        // This thread must NOT prevent this process from exiting
        setDaemon(true);

        mContext = context;
        mSp = mContext.getSharedPreferences(EventConstants.OPTIONS,
                Context.MODE_PRIVATE);

        // Read the parameters saved when the event log buffer was last scanned
        mLastScanRealTime = mSp.getLong( EventConstants.EVENTBGTHREAD_REAL, 0 );
        mLastScanElapsedTime = mSp.getLong( EventConstants.EVENTBGTHREAD_ELAPSED,0 );
        mSanityErrors = mSp.getLong( EventConstants.EVENTBGTHREAD_SANITYERRORS,0 );

        if (DUMP) {
            Log.v( TAG, "create, lsrt=" + mLastScanRealTime + " lset=" + mLastScanElapsedTime );
        }
    }

    // Retrieve the single instance of EventLoggerService
    public static final synchronized EventLoggerService getInstance(Context context) {
        if ( sInstance == null ) {
            sInstance = new EventLoggerService( context );
        }
        return sInstance;
    }

    // Called when boot complete is received at powerup
    final void handleBootComplete(SharedPreferences.Editor edit) {

        // When event logs are scanned later, if the data from the current bootup time if not seen,
        // then the missing duration needs to be reported. So save the bootup information.
        mLastScanRealTime = System.currentTimeMillis();
        mLastScanElapsedTime = SystemClock.elapsedRealtime();

        // Just in case our process crashes, save the bootup data to shared prefs
        edit.putLong( EventConstants.EVENTBGTHREAD_REAL, mLastScanRealTime );
        edit.putLong( EventConstants.EVENTBGTHREAD_ELAPSED, mLastScanElapsedTime );

        if (DUMP) Log.v( TAG, "boot,lsrt=" + mLastScanRealTime + " lset=" + mLastScanElapsedTime );
    }

    final void wakeupEventLogThread(int reason) {
        if (DUMP) Log.v( TAG, "wakeupEventLogThread " + reason );

        if ( reason == WAKEUP_SHUTDOWN ) mShutdownInProgress = true;
        if ( reason == WAKEUP_FORCE_READ ) mForceEventRead = true;

        // Before the background thread was introduced, the event logs were scanned at normal
        // priority. So to be consistent, restore the higher priority of the background thread
        // when the events are scanned periodically at 2 hours
        setBgThreadPriority( Process.THREAD_PRIORITY_DEFAULT );

        synchronized (mWaitObject) {
            mWaitObject.notifyAll(); // wakeup the background thread
        }
    }

    // Make the background thread start running
    // This will happen when the WakelockMonitorService is started(which is at bootup or
    // when our process is restarted after a crash)
    public final void startThread() {
        if (DUMP) Log.v( TAG, "startThread" );

        if ( mThreadStarted ) return;
        mThreadStarted = true;
        start();
    }

    // Report the background thread event log overflow stats to checkin
    public final void checkin( ArrayList<DsCheckinEvent> logs, long checkinTime) {
        String timesPref = mSp.getString(EventConstants.EVENTBGTHREAD_STATS, null);
        if (timesPref==null) return;

        DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent(
                DevStatPrefs.CHECKIN_EVENT_ID, "BgThrOfl", DevStatPrefs.VERSION,
                checkinTime, "se", String.valueOf(mSanityErrors) );
        String[] times = timesPref.split(";");
        for (int i=0; i+2 < times.length; i+= 3) {
            CheckinHelper.addUnnamedSegment(checkinEvent, "ev", times[i], times[i+1], times[i+2]);
        }

        logs.add(checkinEvent);
        if ( DUMP ) Log.v( TAG, "addBgThreadOverflowLog logging " + checkinEvent.serializeEvent() );

        // clear the overflow statistics in shared preferences.
        SharedPreferences.Editor edit = mSp.edit();
        edit.remove(EventConstants.EVENTBGTHREAD_STATS);
        Utils.saveSharedPreferences(edit);
    }

    private final void setBgThreadPriority( int priority ) {
        if ( mBgThreadTid == -1 ) return; // The background thread has not been started yet

        int oldPrio = 0;
        if (DUMP) oldPrio = Process.getThreadPriority( mBgThreadTid );

        Process.setThreadPriority(mBgThreadTid,priority);

        if (DUMP) {
            Log.v( TAG, "setBgThreadPriority prio=" + priority + " old=" + oldPrio + " new=" +
                    Process.getThreadPriority( mBgThreadTid ) );
        }
    }

    @Override
    public final void run() {
        if (DUMP) Log.v( TAG, "run" );

        // save the thread id, so that it can be used by other threads to alter my priority
        mBgThreadTid = Process.myTid();

        // Lower our priority so that we dont interfere with other things happening on the phone
        setBgThreadPriority(BGTHREAD_PRIORITY);

        // open the event log buffer for reading, and get its size
        // Both these functions throw IOException if any error happens.
        nativeInit();
        long maxLogBufferSize = nativeGetEventParam(DEVICESTATS_EVENT_PARAM_LOGGER_MAX_SIZE);

        if (DUMP) Log.v( TAG, "cap=" + maxLogBufferSize );

        while ( true ) {
            if (DUMP) Log.v( TAG, "calling EventLogger" );

            // save the incremental size to be read, so that it can be used for overflow detection
            long incrementalLogSize = nativeGetEventParam(DEVICESTATS_EVENT_PARAM_LOGGER_CURRENT_SIZE);

            mMapper = MapDbHelper.getInstance(mContext);
            mCb = new IMapperCallback() {
                public long getId(String pkg) {
                    long id = mMapper.getId(pkg, true);
                    return id;
                }
            };

            // Create the EventLogger and call run() which will internally read the eventlog buffer
            EventLogger ev = new EventLogger( mContext );
            ev.run();
            mMapper = null; mCb = null; ev = null; // allow garbage collection to happen if required

            long currentRealTime = System.currentTimeMillis();
            long currentElapsedTime = SystemClock.elapsedRealtime();

            // When the event log buffer was read above, the time of the first log read is saved
            // in the native code. Retrieve it.
            long firstLogTime = nativeGetEventParam(DEVICESTATS_EVENT_PARAM_FIRST_LOG_TIME);
            long overflowSinceLastScan = firstLogTime - mLastScanRealTime;
            long elapsedSinceLastScan = currentElapsedTime - mLastScanElapsedTime;

            // If the data read incrementally is less than the event log buffer size,
            //   then an overfow did not happen
            if (overflowSinceLastScan < 0 ||
                    incrementalLogSize < maxLogBufferSize-LOGGER_FULL_THRESHOLD) {
                overflowSinceLastScan = 0;
            }

            if (DUMP) {
                Log.v( TAG, "osls=" + overflowSinceLastScan + " esls=" + elapsedSinceLastScan +
                        " flt=" + firstLogTime + " lsrt=" + mLastScanRealTime +
                        " lset=" + mLastScanElapsedTime );
            }

            SharedPreferences.Editor edit = mSp.edit();

            if ( mLastScanElapsedTime != 0 && mLastScanRealTime != 0 &&
                    (mShutdownInProgress || elapsedSinceLastScan>MIN_ELAPSED_MS) &&
                    elapsedSinceLastScan < MAX_ELAPSED_MS ) {

                // Save the overflow statistics so that it can be reported later
                String pre = mSp.getString(EventConstants.EVENTBGTHREAD_STATS, null);
                StringBuilder preSb = (pre==null) ?
                        new StringBuilder() : new StringBuilder(pre).append(';');

                // Append something like "1312207548;7200;5000"
                // i.e the logs from (1312207548-7200) to ( 1312207548-7200+5000) was lost
                //  note that all times are in seconds.
                preSb.append(currentRealTime/1000).append(';')
                    .append(elapsedSinceLastScan/1000).append(';')
                    .append(overflowSinceLastScan/1000);

                if ( preSb.length() < MAX_OVERFLOW_LOGSIZE ) {
                    edit.putString(EventConstants.EVENTBGTHREAD_STATS, preSb.toString() );
                    if (DUMP) Log.v( TAG, "update stats" + preSb.toString() );
                } else {
                    if (DUMP) Log.v( TAG, "invalid size " + preSb.length() );
                }
            } else {
                if (DUMP) Log.v( TAG, "not reporting overflow duration" );
            }

            mLastScanRealTime = currentRealTime;
            mLastScanElapsedTime = currentElapsedTime;
            mSanityErrors += nativeGetEventParam(DEVICESTATS_EVENT_PARAM_SANITY_ERRORS);

            edit.putLong( EventConstants.EVENTBGTHREAD_REAL, mLastScanRealTime );
            edit.putLong( EventConstants.EVENTBGTHREAD_ELAPSED, mLastScanElapsedTime );
            edit.putLong( EventConstants.EVENTBGTHREAD_SANITYERRORS, mSanityErrors );
            Utils.saveSharedPreferences(edit);

             // if shutdown is in progress, now that we have read the event logs, exit the thread
            if ( mShutdownInProgress ) break;

            long nextWakeupElapsedRealTime = mLastScanElapsedTime + mScanPeriodMs;

            while ( true ) {
                // Reduce the thread priority if it was increased earlier
                setBgThreadPriority(BGTHREAD_PRIORITY);

                long timeToSleep = nextWakeupElapsedRealTime - SystemClock.elapsedRealtime();

                // The "=" part here is mandatory.
                // Otherwise obj.wait(0) called below surprisingly hangs.
                if ( timeToSleep <= 0 ) break;

                // Cannot happen, just for sanity
                if ( timeToSleep > mScanPeriodMs ) timeToSleep = mScanPeriodMs;

                synchronized (mWaitObject) {
                    if (DUMP) Log.v( TAG, "sleeping " + timeToSleep );
                    try {
                        mWaitObject.wait( timeToSleep );
                    } catch ( InterruptedException e ) {
                        if (DUMP) Log.v( TAG, "Ignoring exception " + e );
                    }

                    // If shutdown is in progress, then break out and forcibly scan the event logs
                    if ( mShutdownInProgress ) {
                        if (DUMP) Log.v( TAG, "awakened by shutdown " );
                        break;
                    }

                    if ( mForceEventRead ) {
                        if (DUMP) Log.v( TAG, "BgT forced to read logs " );
                        mForceEventRead = false;
                        break;
                    }

                    currentElapsedTime = SystemClock.elapsedRealtime();
                    long timeSinceLastScan = Math.abs( currentElapsedTime - mLastScanElapsedTime );

                    // Find out how much data needs to be incrementally read.
                    incrementalLogSize = nativeGetEventParam(DEVICESTATS_EVENT_PARAM_LOGGER_CURRENT_SIZE);

                    if (DUMP) {
                        Log.v( TAG, "tsls=" + timeSinceLastScan + " str=" + incrementalLogSize );
                    }

                    // Ensure that we dont scan if this much time has not elapsed
                    if ( timeSinceLastScan < MINIMUM_TIME_BETWEEN_SCAN_MS ) continue;

                    // If only small chunk needs to be incrementally read, then lets postpone it
                    // to avoid frequent filesystem operations
                    if ( timeSinceLastScan < FORCE_SCAN_MS && incrementalLogSize <
                            maxLogBufferSize * MIN_BUFFERLEVEL_SCAN_PERCENT / 100 ) {

                        nextWakeupElapsedRealTime = currentElapsedTime + mScanPeriodMs;
                        continue;
                    }

                    // break out and scan the event log buffer
                    break;
                }
            }
        }

        // Should really happen only during shutdown
        Log.i( TAG, "exiting" );
        mBgThreadTid = -1;
    }
}
