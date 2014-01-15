package com.motorola.datacollection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.TimerTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Log;

public abstract class IntervalAccumulator {

    private static final long serialVersionUID = 1L;
    private static final boolean LOGD = Utilities.LOGD;
    private static final String TAG = "DCE_IntervalAccumulator";
    private static final String LOG_NORMAL_REASON = "ok";
    private static final long SANITY_CHECK_TIME_MS = 10 * 365 * 24 * 60 * 60 * 1000; //year 1980
    private boolean mPreferenceValidInMemory;
    protected final long mIntervalSeconds;
    protected final long mIntervalMilliSeconds;
    protected final int mNumBins;
    private final long mWritePreferenceDelay;
    protected final String mPrefFile;
    protected final String mPrefKey;
    protected HashMapWrapper mAccumulatedData[];
    protected TimeBucketizer mLastTime;
    private TimerTask mTimerTask;
    private boolean mForcePreferenceWrite;

    @SuppressWarnings("serial")
    public static class HashMapWrapper extends HashMap<Object,Object> {
    }

    // All functions in this class run in the background thread.
    protected IntervalAccumulator( long thisIntervalSeconds, long writePreferenceDelayMs,
            String prefFile, String prefKey ) {
        if ( LOGD ) {
            Log.d(TAG, thisIntervalSeconds + " " + writePreferenceDelayMs + " " +
                prefFile + " " + prefKey );
        }

        mPrefFile = prefFile;
        mPrefKey = prefKey;
        mIntervalSeconds = thisIntervalSeconds;
        mIntervalMilliSeconds = mIntervalSeconds * 1000;
        mNumBins = (int)( 24 * 60 * 60 / mIntervalSeconds );
        mWritePreferenceDelay = writePreferenceDelayMs;
        mAccumulatedData = new HashMapWrapper[mNumBins];
        for ( int i=0; i<mNumBins; i++ ) {
            mAccumulatedData[i] = new HashMapWrapper();
        }
        readPreferences();
    }

    public final void accumulate( long toTimeMs ) {
        if ( toTimeMs < SANITY_CHECK_TIME_MS ) {
            Log.e( TAG, "accumulate called with invalid time " + toTimeMs );
            return;
        }
        TimeBucketizer currentTime = new TimeBucketizer( toTimeMs );
        if ( mLastTime != null ) {
            mLastTime.sanityCheck();

            if ( mLastTime.timeMs > currentTime.timeMs ) {
                if ( LOGD ) {
                    Log.d( TAG, "Ignoring back in time call : " + mLastTime.timeMs +
                        " " + currentTime.timeMs );
                }
            } else if ( currentTime.dayStartMs != mLastTime.dayStartMs &&
                    currentTime.dayStartMs - mLastTime.dayStartMs < Utilities.MS_IN_DAY ) {
                if ( LOGD ) {
                    Log.d( TAG, "Day size is less than 24 hours: " + currentTime.dayStartMs +
                        " " + mLastTime.dayStartMs );
                }
            } else {

                while ( mLastTime.dayStartMs != currentTime.dayStartMs ||
                        mLastTime.bucket != currentTime.bucket ) {
                    // The words bucket and interval are used interchangeably in this comment.
                    // Suppose mIntervalSeconds is "2 hours"
                    // Then "Bucket 0" is "12AM-2AM", "bucket 1" is "2AM-4AM", ...
                    // Suppose that lastime is 8:20AM, and that current time is 11:30AM.
                    // The bucket(4) for 8:20AM will correspond to the interval 8AM-10AM
                    // The bucket(5) for 11:30AM will correspond to the interval 10AM-12PM.
                    // Everything from 8:20AM to the end of that interval(10AM), needs to
                    //  be accumulated to the bucket/interval corresponding to 8:20AM.
                    // i.e accumulate into bucket 4, 100 minutes(=10AM-8:20AM)
                    accumulateImpl( mAccumulatedData[mLastTime.bucket],
                            mIntervalMilliSeconds - mLastTime.timeSinceStartOfBucketMSec );
                    mLastTime.timeSinceStartOfBucketMSec = 0;

                    if ( ++mLastTime.bucket >= mNumBins ) {
                        String reason = LOG_NORMAL_REASON;
                        if ( Utilities.DEBUG_DUPLICATE_LOGS ) {
                            reason += ";tzo=" + (TimeZone.getDefault().getRawOffset()/60000) +
                                ";ct=" + System.currentTimeMillis() +
                                ";ut=" + SystemClock.uptimeMillis() +
                                ";er=" + SystemClock.elapsedRealtime();
                        }
                        logToCheckinImpl(reason);
                        resetStatistics( false );
                        synchronized(this) { mForcePreferenceWrite = true; }

                        mLastTime.bucket = 0;
                        mLastTime.dayStartMs = currentTime.dayStartMs;
                    }
                }
                long timeDiff =
                    currentTime.timeSinceStartOfBucketMSec - mLastTime.timeSinceStartOfBucketMSec;
                accumulateImpl( mAccumulatedData[mLastTime.bucket], timeDiff );
            }
        }
        mLastTime = currentTime;

        // I cant call writePreferences here because caller may change other
        //   variables after this function returns. Only after all variables are modified,
        //   should writePreferences be called.
    }

    public final void flushToCheckinAndReset( String reason ) {
        if ( LOGD ) { Log.d( TAG, "flushToCheckinAndReset" ); }
        if ( mLastTime != null ) logToCheckinImpl( reason );
        resetStatistics( true );
    }

    public void accumulate() {
        accumulate( System.currentTimeMillis() );
    }

    public final synchronized void resetStatistics( boolean clearLastTime ) {
        if ( clearLastTime ) mLastTime = null;

        for ( HashMapWrapper hashMap : mAccumulatedData ) {
            hashMap.clear();
        }
    }

    public final String getSerializedString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
                oos = new ObjectOutputStream(baos);
                writeToStreamImpl(oos);
                oos.close();
                return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT );
        } catch ( Exception e ) {
            Log.e( TAG, e.toString() );
            return null;
        }
    }

    public final boolean deSerializeFromString( String encoded ) {
        try {
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream( Base64.decode( encoded, Base64.DEFAULT ) ) );
            readFromStreamImpl( ois );
        } catch (Exception e) {
            Log.e( TAG, e.toString() );
            return false;
        }
        return true;
    }

    protected void readFromStreamImpl(ObjectInputStream ois) throws Exception {
        boolean success = false;

        do {
            if ( ois.readLong() != serialVersionUID ) break;

            HashMapWrapper[] serialAccumData = (HashMapWrapper[]) ois.readObject();
            if ( serialAccumData == null || serialAccumData.length != mNumBins ) {
                throw new InvalidObjectException( "Invalid hash array " +
                        (serialAccumData==null?-123:serialAccumData.length) + " " + mNumBins );
            }
            byte timeType = ois.readByte();

            TimeBucketizer serialLastTime;
            if ( timeType == 0 ) {
                serialLastTime = null;
            } else if ( timeType == 1 ) {
                long fileTime = ois.readLong();
                long currentTime = System.currentTimeMillis();

                // Sanity check. time read from sharedpreferences can't be off by more than a year
                if ( Math.abs( fileTime - currentTime ) > Utilities.MS_IN_YEAR ) {
                    throw new InvalidObjectException( "Invalid fileTime " + fileTime +
                            " " + currentTime );
                }
                serialLastTime = new TimeBucketizer( fileTime );
            } else {
                break;
            }

            mAccumulatedData = serialAccumData;
            mLastTime = serialLastTime;

            success = true;
        } while ( false );

        if ( success == false ) {
            throw new InvalidObjectException( "Reading IntervalAccumulator from String failed");
        }
    }

    protected void writeToStreamImpl(ObjectOutputStream oos) {
        try {
            oos.writeLong( serialVersionUID );
            oos.writeObject( mAccumulatedData );
            if ( mLastTime == null ) {
                oos.writeByte( 0 );
            } else {
                oos.writeByte( 1 );
                oos.writeLong( mLastTime.timeMs );
            }
        } catch (IOException e) {
            Log.e( TAG, e.toString() );
        }
    }

    private final void readPreferences() {
        if ( LOGD ) { Log.d( TAG, "ReadPreferences " + mPrefFile + " " + mPrefKey ); }

        if ( mPreferenceValidInMemory == false ) {
            SharedPreferences pref =
                Utilities.getContext().getSharedPreferences( mPrefFile, Context.MODE_PRIVATE );
            String encoded = pref.getString( mPrefKey, null );
            if ( encoded != null ) deSerializeFromString( encoded );
            mPreferenceValidInMemory = true;
        }
    }

    @SuppressWarnings("all")
    public final synchronized void writePreferences( boolean force ) {
        // Called from background thread
        if ( LOGD ) { Log.d(TAG, "writePreferences" ); }

        if ( mTimerTask != null ) {
            if ( LOGD ) { Log.d(TAG, "Cancelling mTimerTask" ); }
            mTimerTask.cancel();
            mTimerTask = null;
        }

        if ( force || mForcePreferenceWrite ) {
            writePreferencesImpl();
        } else {
            mTimerTask = new TimerTask() {
                public void run() {
                    synchronized ( IntervalAccumulator.this ) {
                        if ( mTimerTask == this ) {
                            new Utilities.BackgroundRunnable() {
                                public void run() {
                                    writePreferencesImpl();
                                }
                            };
                            mTimerTask = null;
                        } else {
                            if ( LOGD ) { Log.d(TAG, "mTimerTask changed, not executing" ); }
                        }
                    }
                }
            };

            Utilities.sTimer.schedule( mTimerTask, mWritePreferenceDelay );

            if ( LOGD ) { Log.d(TAG, "writePreferences has been queued by " +
                    mWritePreferenceDelay + " ms"); }
        }
        mPreferenceValidInMemory = true;
    }

    private final synchronized void writePreferencesImpl() {
        // Called from background thread
        if ( LOGD ) { Log.d( TAG, "writePreferencesImpl " + mPrefFile + " " + mPrefKey ); }

        mForcePreferenceWrite = false;

        SharedPreferences pref =
            Utilities.getContext().getSharedPreferences( mPrefFile, Context.MODE_PRIVATE );
        SharedPreferences.Editor edit = pref.edit();
        edit.putString( mPrefKey, getSerializedString() );
        Utilities.commitNoCrash(edit);
    }

    protected abstract void logToCheckinImpl( String reason );
    protected abstract void accumulateImpl( HashMapWrapper hashMap, long duration );

    protected class TimeBucketizer {
        public long timeMs;
        public long dayStartMs;
        public int bucket;
        public long timeSinceStartOfBucketMSec;
        public TimeBucketizer( long epochTimeMs ) {
            timeMs = epochTimeMs;
            dayStartMs = Utilities.getDayStartTimeMs( epochTimeMs );
            long msFromStartOfDay = epochTimeMs - dayStartMs;
            bucket = (int) (msFromStartOfDay / mIntervalMilliSeconds);
            timeSinceStartOfBucketMSec = msFromStartOfDay - bucket * mIntervalMilliSeconds;
            sanityCheck();
        }

        public void sanityCheck() {
            // When daylight saving time changes, the day can have more than 24 hours.
            // IKSTABLE6-21021: change the bin value from 12 to 11
            if ( bucket >= mNumBins ) bucket = mNumBins - 1;

            // Sanity check, should not happen.
            if ( bucket < 0 ) bucket = 0;
        }
    }
}
