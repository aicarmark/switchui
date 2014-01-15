package com.motorola.devicestatistics;

import static com.motorola.devicestatistics.DevStatPrefs.CHECKIN_EVENT_ID;
import static com.motorola.devicestatistics.DevStatPrefs.CHECKIN_EVENT_ID_BYCHARGER;
import static com.motorola.devicestatistics.DeviceStatsConstants.MS_IN_DAY;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.CheckinHelper.DsSegment;

public final class TagSizeLimiter {
    private static final String TAG = "TagSizeLimiter";
    private static final String PREF_KEY = "TagSizeLimiter";
    private static final boolean DUMP = DevStatUtils.GLOBAL_DUMP;
    private static final boolean VERBOSE_DUMP = false;
    private static final String ID_OVERFLOW = "TagOverflow";

    private static TagSizeLimiter sInstance;
    private Context mContext;
    private LimitPreferences mPrefs;

    private static final Object[] sLimits = {
        CHECKIN_EVENT_ID,           (long)(100 * 1024),
        CHECKIN_EVENT_ID_BYCHARGER, (long)(200 * 1024)
    };
    private static final long MAX_TIME_ERROR_MS = 2 * MS_IN_DAY;

    static class LimitPreferences implements Serializable {
        private static final long serialVersionUID = 1L;

        private long mDayEnd;
        private HashMap<String,Long> mTagBytesLeft;

        LimitPreferences() {
            mDayEnd = DevStatUtils.getMillisecsAtMidnight(MS_IN_DAY);

            mTagBytesLeft = new HashMap<String,Long>();
            for (int i=0; i+1<sLimits.length; i+=2) {
                mTagBytesLeft.put((String)sLimits[i], (Long)sLimits[i+1]);
            }
        }
    }

    public synchronized final static void init(Context context) {
        if (DUMP) Log.d(TAG, "TagSizeLimiter.init()");

        if (sInstance == null) sInstance = new TagSizeLimiter(context);
    }

    private TagSizeLimiter(Context context) {
        mContext = context;

        DevStatPrefs prefs = DevStatPrefs.getInstance(mContext);
        mPrefs = (LimitPreferences) DevStatUtils.deSerializeObject(
                prefs.getStringSetting(PREF_KEY), LimitPreferences.class);
        if (mPrefs == null || mPrefs.mTagBytesLeft == null) mPrefs = new LimitPreferences();

        if (DUMP) {
            Log.d(TAG, "TagSizeLimiter initalized to " + this );
        }
    }

    public static final void log(DsCheckinEvent event, ContentResolver resolver) {
        if (sInstance == null) {
            Log.e(TAG, "log called before init");
            return;
        }

        sInstance.logImpl(event, resolver);
    }

    private final synchronized void logImpl(DsCheckinEvent event, ContentResolver resolver) {
        if (VERBOSE_DUMP) Log.d(TAG, "logImpl start: " + this);

        Long bytesLeft = null;

        try {
            reportOverflowsOnDayChange();

            String tag = event.getTagName();
            long logSize = event.length();

            bytesLeft = mPrefs.mTagBytesLeft.get(tag);
            if (bytesLeft != null) {
                try {
                    if (bytesLeft <= 0) {
                        bytesLeft -= logSize;
                        if (DUMP) Log.v(TAG,"Cannot log " + tag + " bytesLeft=" + bytesLeft);
                        return;
                    }

                    bytesLeft -= logSize;
                    if (bytesLeft < 0) bytesLeft = 0L;

                } finally {
                    mPrefs.mTagBytesLeft.put(tag, bytesLeft);
                }

            }

            event.forcePublish(resolver);
        } finally {
            if (VERBOSE_DUMP) Log.d(TAG, "logImpl end: " + this);

            if (bytesLeft != null) {
                DevStatPrefs prefs = DevStatPrefs.getInstance(mContext);
                prefs.setStringSetting(PREF_KEY, DevStatUtils.serializeObject(mPrefs));
            }
        }
    }

    private final void reportOverflowsOnDayChange() {
        long currentTime = System.currentTimeMillis();

        if (currentTime >= mPrefs.mDayEnd ||
                Math.abs(currentTime - mPrefs.mDayEnd) >= MAX_TIME_ERROR_MS) {
            if (VERBOSE_DUMP) Log.d(TAG, "Day change: " + currentTime + " " + mPrefs.mDayEnd );

            DsCheckinEvent event = new DsCheckinEvent(CHECKIN_EVENT_ID, ID_OVERFLOW,
                    DevStatPrefs.VERSION, System.currentTimeMillis());
            event.setValue("end", mPrefs.mDayEnd);

            boolean report = false;
            for (Entry<String, Long> entry : mPrefs.mTagBytesLeft.entrySet()) {
                long bytesLeft = entry.getValue();

                if (bytesLeft < 0) {
                    DsSegment segment = CheckinHelper.createUnnamedSegment("o", entry.getKey(),
                            String.valueOf(-bytesLeft));
                    event.addSegment(segment);

                    report = true;
                }
            }

            if (report) {
                if (VERBOSE_DUMP) Log.d(TAG, "Reporting " + event.serializeEvent().toString());
                event.forcePublish(mContext.getContentResolver());
            }
            mPrefs = new LimitPreferences();
        }
    }

    public String toString() {
        return "{ dayend=" + mPrefs.mDayEnd + " limits=" +
                mPrefs.mTagBytesLeft.toString() + " }";
    }
}
