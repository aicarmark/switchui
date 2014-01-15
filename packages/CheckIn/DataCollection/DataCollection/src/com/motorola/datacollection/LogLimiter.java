package com.motorola.datacollection;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.motorola.android.provider.CheckinEvent;

public class LogLimiter {
    private static final boolean DISCARD_TESTING = false; // ODO

    private static final String TAG = "DCE_LogLimiter";
    private static final boolean LOGD = Utilities.LOGD;

    private static HashMap<String,Long> sTagBytesLeft = new HashMap<String,Long>();
    private static final String OVERFLOW_PREFIX="DC_LOGOVERFLOW";
    private static final int INVALID_XML_VALUE = -1;
    private static final long DEFAULT_LONG_INITIALIZER = 0;
    private static long sStatsDayStartMs = DEFAULT_LONG_INITIALIZER;

    private static final long DAILY_MAX_LOG_SIZE = 77000L;
    private static final long LOG_LEVEL_1_MAX_SIZE = 5000L;
    private static final long LOG_LEVEL_2_MAX_SIZE = 32000L;
    private static final long LOG_LEVEL_3_MAX_SIZE = 10000L;
    private static final long LOG_LEVEL_4_MAX_SIZE = 10000L;
    private static final long LOG_LEVEL_5_MAX_SIZE = 10000L;
    private static final long LOG_LEVEL_6_MAX_SIZE = 10000L;

    private static final String LOG_STATS_XML_FILE = "LogStats";
    private static final String LOG_STATS_XML_STATS_DAY_START_KEY = "DayStartMs";
    private static final String LOG_STATS_XML_DAILY_BYTES_LEFT_KEY = "DailyBytesLeft";
    private static final String LOG_STATS_XML_TAG_BYTES_LEFT_KEY = "TagBytesLeft";
    private static final String LOG_STATS_XML_ID_OVERFLOW_STATS = "IdOverflowStats";
    private static final long LOG_STATS_QUEUE_TIME_MS = 10 * 60 * 1000;
    private static final String LOGTHROTTLE_PROPERTY = "LogThrottleEnabled";

    private static final String LOG_THROTTLE_DISABLED = "0";
    private static final String LOG_THROTTLE_ENABLED = "1";
    private static final String LOGLIMIT_REASON_NORMALBOOT = "okboot";
    private static final String LOGLIMIT_REASON_NORMAL = "ok";

    private static long sPreferenceLastWriteTime;
    private static long sDailyBytesLeft = DAILY_MAX_LOG_SIZE;
    private static boolean sPreferencesWritePending;
    private static boolean sLogThrottleDisabled;
    private static boolean sForcePreferenceWrite;

    @SuppressWarnings("all")
    public static final void logEvent( String logTag, String logId, int logLength, CheckinEvent checkinEvent ) {
        // Called from background thread
        boolean writeLog = false;

        if ( sLogThrottleDisabled == true ) {
            writeLog = true;
        } else {
            sPreferencesWritePending = true;

            reportStatsToServer( false, LOGLIMIT_REASON_NORMAL );

            long eventSize = logLength + logTag.length();

            Long tagFreeBytes = sTagBytesLeft.get( logTag );
            if ( DISCARD_TESTING && tagFreeBytes == null ) tagFreeBytes = eventSize;

            if ( tagFreeBytes == null ) {
                Log.e( TAG, "Invalid tag " + logTag + " " + checkinEvent.serializeEvent() );
                return;
            }

            if ( tagFreeBytes < eventSize || sDailyBytesLeft < eventSize ) {
                if ( LOGD ) {
                    Log.d( TAG, "Log limit exceeded " + tagFreeBytes + " " +
                            sDailyBytesLeft + " " + logTag + " " + checkinEvent.serializeEvent() );
                }

                OverflowStats.insert( logId, eventSize );
            } else {
                tagFreeBytes -= eventSize;
                sDailyBytesLeft -= eventSize;
                sTagBytesLeft.put( logTag, tagFreeBytes );

                writeLog = true;
            }
        }

        if ( writeLog == true ) {
            if ( Utilities.CHECKIN_TO_LOGCAT == false ) {
               try {
                       checkinEvent.publish(Utilities.getContext().getContentResolver());
                   } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if ( LOGD ) Log.d( TAG, logTag + " " + checkinEvent.serializeEvent() );

            } else {
                Log.d( TAG, checkinEvent.serializeEvent().toString() );
            }
        }

        if ( sLogThrottleDisabled == false ) writeLogStatsToFile(false);
    }

    public static final void logPriorityEvent( CheckinEvent checkinEvent ) {
        // Called from background thread

        if (!Utilities.CHECKIN_TO_LOGCAT) {
             try {
                  checkinEvent.publish( Utilities.getContext().getContentResolver() );
             } catch (Exception e) {
                 Log.e(TAG, "checkinEvent.publish failed", e);
             }
             if ( LOGD ) Log.d( TAG, checkinEvent.getTagName() + " " + checkinEvent.serializeEvent() );
        } else {
            Log.d( TAG, checkinEvent.serializeEvent().toString() );
        }

    }
    private static final void initLogStats() {
        // Called from background thread
        if ( LOGD ) Log.d( TAG, "initLogStats" );
        sTagBytesLeft.put( Utilities.LOG_TAG_LEVEL_1, LOG_LEVEL_1_MAX_SIZE );
        sTagBytesLeft.put( Utilities.LOG_TAG_LEVEL_2, LOG_LEVEL_2_MAX_SIZE );
        sTagBytesLeft.put( Utilities.LOG_TAG_LEVEL_3, LOG_LEVEL_3_MAX_SIZE );
        sTagBytesLeft.put( Utilities.LOG_TAG_LEVEL_4, LOG_LEVEL_4_MAX_SIZE );
        sTagBytesLeft.put( Utilities.LOG_TAG_LEVEL_5, LOG_LEVEL_5_MAX_SIZE );
        sTagBytesLeft.put( Utilities.LOG_TAG_LEVEL_6, LOG_LEVEL_6_MAX_SIZE );

        if ( DISCARD_TESTING ) sTagBytesLeft.clear();

        sDailyBytesLeft = DAILY_MAX_LOG_SIZE;
        OverflowStats.stats.clear();
        sStatsDayStartMs = Utilities.getDayStartTimeMs();
    }

    private static final void writeLogStatsToFile(boolean force) {
        // Called from background thread
        if ( sLogThrottleDisabled == true || sPreferencesWritePending == false ) return;

        long currentTimeMs = System.currentTimeMillis();

        if ( sForcePreferenceWrite || force ||
                sPreferenceLastWriteTime == DEFAULT_LONG_INITIALIZER ||
                Math.abs( currentTimeMs - sPreferenceLastWriteTime ) >=
                    LOG_STATS_QUEUE_TIME_MS ) {

            sPreferenceLastWriteTime = currentTimeMs;
            sPreferencesWritePending = false;
            sForcePreferenceWrite = false;

            SharedPreferences pref =
                Utilities.getContext().getSharedPreferences( LOG_STATS_XML_FILE,
                        Context.MODE_PRIVATE);
            if ( pref != null ) {
                SharedPreferences.Editor edit = pref.edit();
                if ( edit != null ) {
                    edit.putLong( LOG_STATS_XML_STATS_DAY_START_KEY, sStatsDayStartMs );
                    edit.putLong( LOG_STATS_XML_DAILY_BYTES_LEFT_KEY, sDailyBytesLeft );
                    edit.putString( LOG_STATS_XML_TAG_BYTES_LEFT_KEY,
                            Utilities.serializeObject(sTagBytesLeft) );
                    edit.putString( LOG_STATS_XML_ID_OVERFLOW_STATS,
                            Utilities.serializeObject(OverflowStats.stats) );
                    Utilities.commitNoCrash(edit);


                    if ( LOGD ) {
                        Log.d( TAG, "writeLogStatsToFile wrote " + currentTimeMs +
                                ' ' + sPreferenceLastWriteTime );
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    static final void initialize() {
        // Called from background thread
        if ( LOGD ) Log.d( TAG, "initialize" );

        initLogStats();
        sPreferenceLastWriteTime = System.currentTimeMillis();

        SharedPreferences pref =
            Utilities.getContext().getSharedPreferences( LOG_STATS_XML_FILE,
                    Context.MODE_PRIVATE);
        if ( pref == null ) return;

        if ( LOG_THROTTLE_DISABLED.equals(
                pref.getString( LOGTHROTTLE_PROPERTY, LOG_THROTTLE_ENABLED ) ) ) {
            if ( LOGD ) Log.d( TAG, "Log throttling disabled" );
            sLogThrottleDisabled = true;
            return;
        }

        long newStatsDayStartMs = pref.getLong( LOG_STATS_XML_STATS_DAY_START_KEY,
                INVALID_XML_VALUE);
        if ( newStatsDayStartMs == INVALID_XML_VALUE ) return;

        long newDailyBytesLeft = pref.getLong( LOG_STATS_XML_DAILY_BYTES_LEFT_KEY,
                INVALID_XML_VALUE);
        if ( newDailyBytesLeft == INVALID_XML_VALUE ) return;

        HashMap<String,Long> newTagBytesLeft =
            (HashMap<String,Long>) Utilities.deSerializeObject(
                    pref.getString( LOG_STATS_XML_TAG_BYTES_LEFT_KEY, null ) );
        if ( newTagBytesLeft == null ) return;

        HashMap<String,OverflowStats> newIdOverflowStats =
            (HashMap<String,OverflowStats>) Utilities.deSerializeObject(
                    pref.getString( LOG_STATS_XML_ID_OVERFLOW_STATS, null ) );
        if ( newIdOverflowStats == null ) return;

        sStatsDayStartMs = newStatsDayStartMs;
        sDailyBytesLeft = newDailyBytesLeft;
        sTagBytesLeft = newTagBytesLeft;
        OverflowStats.stats = newIdOverflowStats;
        if ( LOGD ) {
            Log.d( TAG, "readLogStatsFromFile " + sStatsDayStartMs + ' ' +
                    sDailyBytesLeft + ' ' + sTagBytesLeft + ' ' +
                    OverflowStats.stats );
        }

        reportStatsToServer( false, LOGLIMIT_REASON_NORMALBOOT );
    }

    @SuppressWarnings("all")
    private static final void reportStatsToServer( boolean force, String reason ) {
        // Called from background thread
        if ( sLogThrottleDisabled == true ) return;

        if ( force == false ) {
            if ( Math.abs( Utilities.getDayStartTimeMs() - sStatsDayStartMs ) <
                    Utilities.MS_IN_DAY ) {
                return;
            }
        }

        sForcePreferenceWrite = true;

        if ( OverflowStats.stats.isEmpty() ) {
            if ( LOGD ) Log.d(TAG, "No overflow stats to report, returning" );
            initLogStats();
            return;
        }
        CheckinEvent checkinEvent = new CheckinEvent(Utilities.LOG_TAG_LEVEL_1, OVERFLOW_PREFIX,
                       Utilities.EVENT_LOG_VERSION, sStatsDayStartMs);
        if (reason == null) {
            reason = "null";
        }
        checkinEvent.setValue("re", reason);
      //[ID=DC_LOGOVERFLOW;ver=VER;time=TIME;re=reason ... ;]
        int statsLength = 4 + OVERFLOW_PREFIX.length() + 5 + Utilities.EVENT_LOG_VERSION.length() +
                       6 + String.valueOf(sStatsDayStartMs).length() + 4 + reason.length() + 2;


        for ( Map.Entry<String,OverflowStats> entry : OverflowStats.stats.entrySet() ) {
            OverflowStats info = entry.getValue();
               String key = entry.getKey();
               String value = String.valueOf(info.count) + ',' + info.size + ',' + info.startTime;
               if (value == null) {
                   value = "null";
               }
               checkinEvent.setValue(key, value);
               statsLength += key.length() + 1 + value.length() + 1; // key=value;
        }

        initLogStats();
        int logLen = statsLength + Utilities.LOG_TAG_LEVEL_1.length();
        if ( logLen <= DAILY_MAX_LOG_SIZE && logLen <= LOG_LEVEL_1_MAX_SIZE ) {
            // Effectively a recursive call to the calling function
            LogLimiter.logEvent( Utilities.LOG_TAG_LEVEL_1, OVERFLOW_PREFIX, statsLength, checkinEvent );
        } else {
            if ( LOGD ) Log.d( TAG, "daily stats discarded" );
        }
    }

    static final void handlePeriodicWrite() {
        // Called from background thread
        if ( LOGD ) Log.d(TAG, "handlePeriodicWrite" );
        writeLogStatsToFile(true);
    }

    static final void handleTimeChange( String reason ) {
        // Called from background thread
        if ( LOGD ) Log.d(TAG, "handleTimeChange" );
        //   To avoid burst of logs due to time change,
        //    the statistics accumulated is not discarded,
        //    and the time associated with the start of day is reset.
        // reportStatsToServer( true, reason );
        sStatsDayStartMs = Utilities.getDayStartTimeMs();
        writeLogStatsToFile( true );
    }

    // All functions in this class are called from background thread
    static class OverflowStats implements Serializable {
        private static final long serialVersionUID = 1L;

        private long count;
        private long size;
        private long startTime;

        static HashMap<String,OverflowStats> stats = new HashMap<String,OverflowStats>();
        static final void insert( String logId, long eventSize ) {
            OverflowStats info = stats.get( logId );

            if ( info == null ) {
                info = new OverflowStats();
                info.startTime = System.currentTimeMillis();
                stats.put( logId, info );
            }

            info.count++;
            info.size += eventSize;
            if ( LOGD ) {
                Log.d( TAG, "OverflowStats.insert " + info.count + ' ' +
                        info.size + ' ' + info.startTime + " @ " + stats );
            }
        }

        @Override
        public String toString() {
            return "{count=" + count + ";size=" + size + ";startTime=" +
                DateFormat.getDateTimeInstance().format(new Date(startTime)) + "}";
        }
    }
}
