package com.motorola.devicestatistics.eventlogs;

import android.content.ContentResolver;
import android.util.Log;

import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;

public class LogController {

    public interface ILogFullCallback {
        void onLogFull(int level);
        void onLogFull();
        void onLogReset();
    }
    
    private final static int BUFFER = 512;
    private final static boolean DUMP = true;
    private final static boolean DBG = false;
    
    Config mConfig;
    ILogFullCallback mCb;
    ContentResolver mCr;
    int[] mLogSizes;
    int[] mLogFilters;
    int[] mLogOflowSizes;
    int[] mLogMissedEvents;
    
    public LogController(ILogFullCallback cb, Config cfg, ContentResolver cr, int[] logSizes,
            int[] oflowSizes, int[] missedEvCounters, long timebase) {
        mCb = cb;
        mConfig = cfg;
        mCr = cr;
        mLogOflowSizes = clone(oflowSizes);
        mLogMissedEvents = clone(missedEvCounters);
        init(logSizes, timebase);
    }
    
    private void init(int[] sizes, long base) {
        long now = System.currentTimeMillis();

        mLogSizes = new int[sizes.length];
        if(now - base >= 24 * 3600 * 1000) {
            // reset all sizes
            for(int i = 0; i < mLogSizes.length; ++i) {
                mLogSizes[i] = 0;
            }
            for(int i = 0; i < mLogOflowSizes.length; ++i) {
                mLogOflowSizes[i] = 0;
            }
            for(int i = 0; i < mLogMissedEvents.length; ++i) {
                mLogMissedEvents[i] = 0;
            }
            if(mCb != null) mCb.onLogReset();
        }else {
            for(int i = 0; i < mLogSizes.length; ++i) {
                mLogSizes[i] = sizes[i];
            }
        }
        mLogFilters = mConfig.getSizeFilters();
    }

    private int[] clone(int[] array) {
        int[] copy = new int[array.length];
        for(int i = 0; i < array.length; ++i) {
            copy[i] = array[i];
        }
        return copy;
    }

    public void logEvent(String tag, DsCheckinEvent log) {
        log.publish(mCr);
        if(DUMP) Log.v(tag, log.serializeEvent().toString());
    }
    
    public boolean canLogEvent(String tag, String log) {
        // TODO: Make this base off level, instead of tag
        int level = mConfig.getLevel(tag);
        int limit = mLogFilters[level];
        
        if(limit != -1) {
            int total = mLogSizes[level];
            if(total < limit) {
                int size = log.length();
                if(DBG) Log.v(tag, "Level " + level + " has reached " + (total + size));
                if(total + size < limit + BUFFER) {
                    total += size;
                    mLogSizes[level] = total;
                    checkSizes(level, limit);
                }else {
                    // this is unfair but we cannot miss
                    // one log and allow later ones to pass
                    total += size;
                    mLogSizes[level] = total;
                    mLogOflowSizes[level] += size;
                    mLogMissedEvents[level] += 1;
                    checkSizes(level, limit);
                    return false;
                }
            }else {
                mLogOflowSizes[level] += log.length();
                mLogMissedEvents[level] += 1;
                return false;
            }
        }
        return true;
    }
    
    private void checkSizes(int level, int limit) {
        if(mLogSizes[level] < limit) return;

        boolean report = true;
        for(int i = 0; i < mLogSizes.length; ++i) {
            int filter = mLogFilters[i];
            if(filter != 0 && filter != -1) {
                if(mLogSizes[i] < filter) {
                    report = false;
                    break;
                }
            }
        }
        if(mCb != null) {
            mCb.onLogFull(level);
            if(report) mCb.onLogFull();
        }
    }
    
    public int[] getUpdatedSizes() {
        return mLogSizes;
    }

    public int[] getOflowSizes() {
        return mLogOflowSizes;
    }

    public int[] getMissedEvents() {
        return mLogMissedEvents;
    }

    public boolean isOflow(int[] counters) {
        boolean oflow = false;
        for(int i = 0; i < counters.length; ++i) {
            if(counters[i] > 0) {
                oflow = true;
                break;
            }
        }
        return oflow;
    }
}

