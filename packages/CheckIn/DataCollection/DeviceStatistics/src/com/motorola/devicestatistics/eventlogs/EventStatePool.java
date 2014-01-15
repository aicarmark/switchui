/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: BatteryLogger.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date               Author            Comments
 * Jan 02, 2011       bluremployee      Created file
 * Sep 12, 2011       w04917            IKCTXAW-357 Add parsing for Performance Stats
 **********************************************************
 */
package com.motorola.devicestatistics.eventlogs;

import android.util.EventLog;
import android.util.Log;
import android.util.EventLog.Event;
import android.text.TextUtils;

import java.util.HashMap;

import com.motorola.devicestatistics.eventlogs.EventConstants.Events;
import com.motorola.devicestatistics.eventlogs.EventConstants.RawEvents;
import com.motorola.devicestatistics.eventlogs.EventConstants.Source;
import com.motorola.devicestatistics.DeviceStatsConstants;
import com.motorola.devicestatistics.Utils;

class EventStatePool {
    //TODO: This class is totally cluttered up now - need a new design in
    //       next cleanup iteration

    private final static boolean DUMP = false;
    private final static String TAG = "EventStatePool";
    private static String sPreview;

    HashMap<Integer, EventState> mStates;
    IMapperCallback mCallback;
    long mBootTime;

    static EventStatePool mSelf;

    public void setBootTime(long when) {
        mBootTime = when;
    }

    private EventStatePool() {
        mStates =
                new HashMap<Integer, EventState>(
                    Events.EVENT_FILTER.length + RawEvents.EVENT_FILTER.length);
    }

    static EventStatePool getStatePool(IMapperCallback cb) {
        if(mSelf == null) {
            mSelf = new EventStatePool();
        }
        // Update the callback everytime - this can change for every run
        if(mSelf != null) {
            mSelf.mCallback = cb;
        }
        return mSelf;
    }

    boolean hasChanged(int id, EventLog.Event e) {
        EventState es = initState(id);
        if(es == null) return false;

        return es.hasChanged(id, e);
    }

    void getLog(int id, ILogger logger) {
        EventState es = mStates.get(id);
        if(es != null) {
            es.getLog(id, logger);
        }
    }

    void dumpLog(ILogger logger) {
        // TODO: Expand in a generic manner
        EventState es = mStates.get((int)1);
        if(es != null) es.dumpLog(logger);
    }

    EventState initState(int id) {
        EventState es = mStates.get(id);
        if(es == null) {
            es = createState(id);
        }
        return es;
    }

    EventState createState(int id) {
        EventState es = null;
        if(id < Events.EVENT_FILTER.length + RawEvents.EVENT_FILTER.length) {
            switch(id) {
                case 0:
                    es = new KeyboardState();
                    break;
                case 1:
                    es = new MultimediaState();
                    break;
                case 2:
                    es = new PerformanceState();
                    break;
                case 3:
                    es = new SimpleState("vib", Events.DC_VIB);
                    break;
                case 4:
                    es = new SimpleState("snd", Events.DC_SND);
                    break;
                case 5:
                    es = new SimpleState("autocmpl", Events.DC_AUTOCMPL);
                    break;
                case 6:
                    es = new SimpleState("bt", Events.DSBT_STATUS);
                    break;
                case  7:
                    es = new MultimediaLogState();
                    break;

                default:
                    es = null;
                    break;
            }
        }
        if(es != null) {
            es.reset();
            mStates.put(id, es);
        }
        return es;
    }

    class DefaultState extends EventState {
        String mVal;
        String mId;
        int mIndex;
        long mTime;

        DefaultState(int index, String id) {
            mIndex = index;
            mId = id;
            reset();
        }

        void reset() {
            mVal = null;
        }

        boolean hasChanged(int id, EventLog.Event e) {
            Object list = e.getData();
            mTime = e.getTimeNanos() / DeviceStatsConstants.NANOSECS_IN_MSEC ;
            mVal = parseEvent(list, mIndex);
            if(DUMP) Log.v(TAG, "DS: " + mVal + ";" + mId + ";");
            return true;
        }

        void getLog(int id, ILogger logger) {
            mTime = filterEventTime(mBootTime, mTime);
            logger.log(Source.EVENTLOG, id, EventConstants.CHECKIN_ID,
                    mId + ";" + mTime + ";" + mVal);
        }

        void dumpLog(ILogger logger) {}
    }

    class SimpleState extends EventState {
        String mVal;
        String mId;
        String mKey;
        long mTime;

        SimpleState(String id, String key) {
            mId = id;
            mKey = key;
            reset();
        }

        void reset() {
            mVal = null;
        }

        boolean hasChanged(int id, EventLog.Event e) {
            Object list = e.getData();
            mTime = e.getTimeNanos() / DeviceStatsConstants.NANOSECS_IN_MSEC;
            mVal = parseSimpleEvent(list);
            if(DUMP) Log.v(TAG, "SiS: " + mVal + ";" + mId + ";");
            return true;
        }

        void getLog(int id, ILogger logger) {
            mTime = filterEventTime(mBootTime, mTime);
            logger.log(Source.EVENTLOG, mKey, EventConstants.CHECKIN_ID,
                    mId + ";" + mTime + ";" + mVal);
        }

        void dumpLog(ILogger logger) {}
    }

    class KeyboardState extends EventState {
        String mVal;
        long mTime;
        StringBuilder mSb;

        KeyboardState() {
            reset();
        }

        void reset() {
            mSb = new StringBuilder();
            mVal = null;
        }

        boolean hasChanged(int id, EventLog.Event e) {
            Object list = e.getData();
            mTime = e.getTimeNanos() /DeviceStatsConstants.NANOSECS_IN_MSEC;
            mTime = filterEventTime(mBootTime, mTime);
            mVal = parseKeyboardEvent(list, mTime);
            if(DUMP) Log.v(TAG, "KbS: " + mVal);
            return mVal != null;
        }

        void getLog(int id, ILogger logger) {
            logger.log(Source.EVENTLOG, Events.DC_KEYBOARD, EventConstants.DC_KEYBOARD_ID,
                    mVal);
        }

        void dumpLog(ILogger logger) {}

        final String[] KEYS =
                new String[] {"ke", "kat", "kc", "ac", "ker", ""};

        String parseKeyboardEvent(Object list, long time) {
            if(list == null) return null;
            mSb.setLength(0);
            mSb.append("ID=kbd;time=").append(time).append(";");
            try {
                if(list instanceof Object[]) {
                    Object[] objs = (Object[])list;
                    for(int i = 0; i < objs.length; ++i) {
                        parseKeys(objs[i], i, mSb);
                    }
                    return mSb.toString();
                }else {
                    return mSb.append(KEYS[0]).append("=")
                            .append(list.toString()).toString();
                }
            }catch(Exception ex) {
                // for non-matching versions we may fail parsing
                if(DUMP) Log.v(TAG, "KbS: failed parsing keyboard event");
                return null;
            }
        }

        void parseKeys(Object o, int index, StringBuilder sb) {
            if(o == null) return;
            // Being totally paranoid here as we dont't know
            // what could go wrong where - FIX THIS
            String s = o.toString();
            if(s.length() == 0) return;
            if(KEYS[index].length() > 0) {
                sb.append(KEYS[index]).append("=").append(s).append(";");
            }else {
                sb.append(s.replace(',',';'));
            }
        }

        boolean validList(Object[] list) {
            for(int i = 0; i < list.length; ++i) {
                if(list[i] != null) return true;
            }
            return false;
        }
    }

    class MultimediaState extends EventState {
        String mVal;
        long mTime;
        StringBuilder mSb;

        MultimediaState() {
            reset();
        }

        void reset() {
            mSb = new StringBuilder();
            mVal = null;
        }

        boolean hasChanged(int id, EventLog.Event e) {
            Object list = e.getData();
            mTime = e.getTimeNanos() / DeviceStatsConstants.NANOSECS_IN_MSEC;
            mTime = filterEventTime(mBootTime, mTime);
            mVal = parseMultimediaEvent(list, mTime);
            if(DUMP) Log.v(TAG, "MmS: " + mVal);
            return mVal != null;
        }

        void getLog(int id, ILogger logger) {
            logger.log(Source.EVENTLOG, Events.DC_MM, EventConstants.DC_MULTIMEDIA_ID,
                    mVal);
        }

        void dumpLog(ILogger logger) {}

        String parseMultimediaEvent(Object list, long time) {
            if(list == null || !(list instanceof String)) return null;
            try {
                mSb.setLength(0);
                String data = (String)list;
                HashMap<String,String> map = new HashMap<String,String>();
                for ( String param : TextUtils.split( data, ";" ) ) {
                    String values[] = TextUtils.split( param, "=" );
                    map.put( values[0], values[1] );
                }

                mSb.append("ID=DC_MIME");
                mSb.append( ";time=" ) . append( map.get("time") )
                        .append( ";e=" ) .append( map.get( "e" ) );

                for ( String mime : new String[] { "a", "v" } ) {
                    String mimeData = (String) map.get( mime );
                    if ( mimeData != null ) {
                        int counter = 0;
                        for ( String field : TextUtils.split( mimeData, "," ) ) {
                            mSb.append( ';' ).append( mime ) . append( counter++ ) . append( "=" )
                                .append( field );
                        }
                    }
                }
                return mSb.toString();
            }catch(Exception ex) {
                return null;
            }
        }
    }

    class MultimediaLogState extends EventState {
        String mVal;
        long mTime;
        String mCameraFacing;
        String mFocus;
        String mUid;
        StringBuilder mSb;
        String mFlash;
        String mExposure;
        String mSoundKind;
        String mPictureCount;

        MultimediaLogState() {
            reset();
        }

        void reset() {
            mSb = new StringBuilder();
            mVal = null;
        }

        boolean hasChanged(int id, EventLog.Event e) {
            Object list = e.getData();
            mTime = e.getTimeNanos() /DeviceStatsConstants.NANOSECS_IN_MSEC;
            mTime = filterEventTime(mBootTime, mTime);
            mVal = parseMultimediaLogEvent(list, mTime);
            if(DUMP) Log.v(TAG, "MmS: " + mVal);
            return mVal != null;
        }

        void getLog(int id, ILogger logger) {
            logger.log(Source.EVENTLOG, Events.DC_MMLOG, EventConstants.DC_MULTIMEDIALOG_ID,
                    mVal);
        }

        void dumpLog(ILogger logger) {}

        String parseMultimediaLogEvent(Object list, long time) {
            if(list == null || !(list instanceof String)) return null;
            try {
                mSb.setLength(0);
                String data = (String)list;

                for (String param : TextUtils.split( data, ";" ) ) {
                    String values[] = TextUtils.split( param, "=" );
                    if(DUMP) Log.v(TAG, "MmLog: values[0] is =" + values[0]);
                    if(DUMP) Log.v(TAG, "MmLog: values[1] is =" + values[1]);
                    if (values[0].equals("camera-facing")) {
                        mCameraFacing = values[1];
                    } else if (values[0].equals("uid")) {
                        mUid = values[1];
                    } else if (values[0].equals("focus")) {
                        mFocus = values[1];
                    } else if (values[0].equals("exposure")) {
                        mExposure = values[1];
                    } else if (values[0].equals("flash")) {
                        mFlash = values[1];
                    } else if (values[0].equals("sound-kind")) {
                        mSoundKind = values[1];
                    } else if (values[0].equals("picture-count")) {
                        mPictureCount = values[1];
                    } else if (values[0].equals("preview")) {
                       if (!(values[1].equals (sPreview))) {
                           sPreview = values[1];
                           mSb.append("ID=DC_MMCAM");
                           mSb.append(";time=" ) . append (mTime)
                              .append( ";uid=" ) .append( mUid )
                              .append( ";cam-facing=" ) .append( mCameraFacing )
                              .append( ";prvw=" ).append(sPreview);
                       }
                    } else if (values[0].equals("vid-record")) {
                       mSb.append("ID=DC_MMCAM");
                       mSb.append(";time=" ) . append (mTime)
                          .append( ";uid=" ) .append( mUid )
                          .append( ";cam-facing=" ) .append( mCameraFacing )
                          .append( ";vid-rec=" ).append(values[1]);
                    }
                }
                if (mPictureCount != null) {
                    mSb.append("ID=DC_MMCAM");
                    mSb.append(";time=" ) . append (mTime)
                        .append( ";uid=" ) .append( mUid )
                        .append( ";cam-facing=" ) .append( mCameraFacing )
                        .append( ";fcs=" ) .append( mFocus )
                        .append( ";exp=" ) .append( mExposure )
                        .append( ";flsh=" ) .append( mFlash )
                        .append( ";snd-knd=" ) .append( mSoundKind )
                        .append( ";pic-cnt=" ) .append( mPictureCount );

                    mPictureCount = null;
                }
                if (mSb.length() == 0) {
                    return null;
                } else {
                    return mSb.toString();
                }
            }catch(Exception ex) {
                Utils.Log.d(TAG, "Got exception ", ex);
                return null;
            }
        }
    }

    class PerformanceState extends EventState {
        private static final int INDEX_TIME = 0;
        private static final int INDEX_PERF_STATS_VERSION = 1;
        private static final int INDEX_PERF_COMPONENT = 2;
        private static final int INDEX_PERF_LOG = 3;

        String mLog;

        PerformanceState() {
            reset();
        }

        @Override
        void reset() {
            mLog = null;
        }

        @Override
        boolean hasChanged(int id, Event e) {
            boolean hasChanged = false;

            Object list = e.getData();

            if (list != null) {
                String timeString = parseEvent(list, INDEX_TIME);
                String perfVersion = parseEvent(list, INDEX_PERF_STATS_VERSION);
                String perfComponent = parseEvent(list, INDEX_PERF_COMPONENT);
                String perfLog = parseEvent(list, INDEX_PERF_LOG);

                if (timeString != null && perfVersion != null && perfComponent != null && perfLog != null) {
                    /* for Performance Stats, we want to format the checkin string differently,
                     * instead of using the generic header */
                    perfLog = perfLog.replace(";]", "]");
                    perfLog = perfLog.replace("]", "");
                    mLog = ("ID=" + EventConstants.PERF_STATS_ID +
                               ";ver=" + perfVersion +
                               ";time=" + timeString + perfLog);
                    if(DUMP) Log.v(TAG, "PerformanceState: " + mLog);
                    hasChanged = true;
                }
            }
            return hasChanged;
        }

        @Override
        void dumpLog(ILogger logger) {
        }

        @Override
        void getLog(int id, ILogger logger) {
            logger.log(Source.EVENTLOG, Events.PERF_STATS, EventConstants.PERF_STATS_ID, mLog);
        }

        private String parseEvent(Object o, int index) {
            String retVal = null;
            if (index >=0 && o instanceof Object[]) {
                Object[] list = (Object[])o;
                if (index < list.length) {
                    Object value = list[index];
                    if (value != null) {
                        retVal = value.toString();
                    }
                }
            }
            return retVal;
        }
    }

    static long filterEventTime(long boottime, long evtime) {
        if(evtime < EventConstants.DATE_LOWLIMIT && boottime != -1) return boottime;
        return evtime;
    }

    static int parseInt(String val) {
        int num = -1;
        try {
            num = Integer.parseInt(val);
        }catch(NumberFormatException nfEx) {}
        return num;
    }

    static String getString(int num) {
        return Integer.toString(num);
    }

    static String parseEvent(Object o, int index) {
        if(index >= 0 && o instanceof Object[]) {
            Object[] list = (Object[])o;
            Object value = list[index];
            return value.toString();
        }else {
            return "null";
        }
    }

    static String parseSimpleEvent(Object o) {
        if(o != null) {
            return o.toString();
        }else {
            return "null";
        }
    }
}

