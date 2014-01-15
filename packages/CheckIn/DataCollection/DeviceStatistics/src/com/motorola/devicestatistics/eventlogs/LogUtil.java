package com.motorola.devicestatistics.eventlogs;

import com.motorola.devicestatistics.Utils;
import com.motorola.devicestatistics.eventlogs.EventConstants.SizeFilters;

import android.content.Context;
import android.content.SharedPreferences;

public class LogUtil implements LogController.ILogFullCallback {

    private Context mContext;
    private SharedPreferences mSp;
    private boolean mReset =false;
    private Config mConfig;
    private LogController mController;
    private int[] mOflowCounters;
    private int[] mMissedEvCounters;


    public LogUtil(Context context) {
        mContext = context;
        mSp = context.getSharedPreferences(EventConstants.OPTIONS,
                                           Context.MODE_PRIVATE);
    }

    LogBuilder getLogger() {
        int level = mSp.getInt(EventConstants.CONFIG_LEVEL,
                               EventConstants.LOG_CONFIG);
        int[] counters = parseSizes(mSp.getString(EventConstants.SIZECOUNTER,
                                    SizeFilters.DEFAULT_SIZES));
        mOflowCounters = parseSizes(mSp.getString(EventConstants.OFLOWSIZECOUNTER,
                                    SizeFilters.DEFAULT_SIZES));
        mMissedEvCounters = parseSizes(mSp.getString(EventConstants.MISSEDEVENTCOUNTER,
                                       SizeFilters.DEFAULT_SIZES));
        String filters = mSp.getString(EventConstants.SIZEFILTER,
                                       SizeFilters.FILTER);
        int levelCount = mSp.getInt(EventConstants.LEVELCOUNTER,
                                    SizeFilters.DEFAULT_LEVELS);
        long base = mSp.getLong(EventConstants.SIZETIME_REFERENCE,
                                0);

        counters = adjustArray(counters, levelCount);
        mOflowCounters = adjustArray(mOflowCounters, levelCount);
        mMissedEvCounters = adjustArray(mMissedEvCounters, levelCount);
        filters = adjustStringArray(filters, levelCount);

        mReset = false;
        mConfig = new Config(level, filters);
        mConfig.initialize();
        mController = new LogController(this, mConfig,
                                        mContext.getContentResolver(), counters, mOflowCounters,
                                        mMissedEvCounters, base);

        LogBuilder logger = new LogBuilder(mConfig, mController);
        return logger;
    }

    private int[] parseSizes(String sizes) {
        String[] parts = sizes.split(",");
        int[] numbers = new int[parts.length];
        for(int i = 0; i < numbers.length; ++i) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            } catch(NumberFormatException nfEx) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private int[] adjustArray(int[] array, int size) {
        int orig = array.length;
        if(size == orig) return array;
        int[] narray = new int[size];
        for(int i = 0; i < size; ++i) {
            if(i < orig) narray[i] = array[i];
            else narray[i] = 0;
        }
        return narray;
    }

    private String adjustStringArray(String value, int count) {
        int[] converted = parseSizes(value);
        converted = adjustArray(converted, count);
        StringBuilder sb = new StringBuilder();
        encodeSizes(converted, ",", sb);
        return sb.toString();
    }

    private void encodeSizes(int[] sizes, final String separator, StringBuilder sb) {
        sb.setLength(0);
        sb.append(Integer.toString(sizes[0]));
        for(int i = 1; i < sizes.length; ++i) {
            sb.append(separator).append(sizes[i]);
        }
    }

    // ILogFullCallback
    public void onLogFull() {
        SharedPreferences.Editor ed = mSp.edit();
        ed.putBoolean(EventConstants.LOGFULL, true);
        Utils.saveSharedPreferences(ed);
    }

    // ILogFullCallback
    public void onLogFull(int level) {
    }

    // ILogFullCallback
    public void onLogReset() {
        SharedPreferences.Editor ed = mSp.edit();
        ed.putBoolean(EventConstants.LOGFULL, false);
        ed.putLong(EventConstants.SIZETIME_REFERENCE, System.currentTimeMillis());
        Utils.saveSharedPreferences(ed);
        mReset = true;
    }

    public int[] getMissedEvCounters() {
        return mMissedEvCounters;
    }

    public int[] getOflowCounters() {
        return mOflowCounters;
    }

    public LogController getController() {
        return mController;
    }

    public Config getConfig() {
        return mConfig;
    }

    public boolean isReset() {
        return mReset;
    }


}
