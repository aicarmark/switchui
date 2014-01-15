/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: LogBuilder.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date               Author            Comments
 * Nov 02, 2010       bluremployee      Created file
 * Sep 12, 2011       w04917            IKCTXAW-357 Add parsing for Performance Stats
 **********************************************************
 */
package com.motorola.devicestatistics.eventlogs;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.Map.Entry;

import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.CheckinHelper;
import com.motorola.devicestatistics.CheckinHelper.DsSegment;
import com.motorola.devicestatistics.DevStatPrefs;

/**
 * @author bluremployee
 *
 */
public class LogBuilder implements ILogger {
    
    private final static String VERSION = DevStatPrefs.VERSION;
    private final static int LOG_LIMIT = 4096;
    
    static long sNow, sTz;
    
    Config mConfig;
    HashMap<String, LogBuffer> mBufferMap;
    LogController mController;
    
    public LogBuilder(Config config, LogController controller) {
        mConfig = config;
        mController = controller;
        mBufferMap = new HashMap<String,LogBuffer>();
        LogBuilder.updateTimeConstants();
    }

    // ILogger
    public void checkin() {
        Iterator<Entry<String, LogBuffer>> set = mBufferMap.entrySet().iterator();
        while(set.hasNext()) {
            Entry<String, LogBuffer> ent = set.next();
            String tag = ent.getKey();
            LogBuffer buf = ent.getValue();
            if(buf != null) buf.checkin(mController, tag);
        }
    }

    // ILogger
    public void log(int source, String type, String id, String log) {
        int level = mConfig.getLevel(source, type);
        log(level, id, log);
    }

    // ILogger
    public void log(int source, int type, String id, String log) {
        int level = mConfig.getLevel(source, type);
        log(level, id, log);
    }

    // ILogger
    public void reset() {
        mBufferMap.clear();
    }
    
    private void log(int level, String id, String log) {
        LogBuffer buffer = getLogBuffer(level, id, log);
        if(buffer != null) buffer.log(id, log);
    }
    
    private LogBuffer getLogBuffer(int level, String id, String log) {
        if(mConfig.isEnabled(level)) {
            String tag = mConfig.getTag(level);
            if(mController.canLogEvent(tag, log)) {
                LogBuffer buf = mBufferMap.get(tag);
                if(buf == null) {
                    buf = new LogBuffer(tag);
                    mBufferMap.put(tag, buf);
                }
                return buf;
            }
        }
        return null;
    }
    
    static class LogBuffer {
        HashMap<String, LogPair> mLogs;
        String mTag;

        public LogBuffer(String tag) {
            mLogs = new HashMap<String, LogPair>();
            mTag = tag;
        }
        
        public void log(String id, String log) {
            LogPair pair = mLogs.get(id);
            if(pair == null) {
                if (EventConstants.PERF_STATS_ID.equals(id)) {
                    /* use direct logging for PERF_STATS */
                    pair = new DirectLogPair();
                }
                else {
                    pair = new LogPair();
                }
                mLogs.put(id, pair);
            }
            if(pair != null) pair.log(id, log,mTag);
        }
        
        public void checkin(LogController cr, String tag) {
            Iterator<LogPair> set = mLogs.values().iterator();
            while(set.hasNext()) {
                LogPair p = set.next();
                if(p!= null) p.checkin(cr, tag);
            }
        }
    }
    
    static class LogPair {
        protected DsCheckinEvent mCurrentLog;
        protected int mCurrentLength;
        protected ArrayList<DsCheckinEvent> mLogList;

        public LogPair() {
            mLogList = new ArrayList<DsCheckinEvent>();
        }

        public void log(String id, String logGroup, String tag) {
            // TODO : This might be overkill for a simple log
            // addition - anything better?
            String logs[] = logGroup.split("\\[");
            DsSegment[] segments = new DsSegment[logs.length];
            int thisLogLength = logGroup.length() + logs.length + 1;
            for (int i=0; i<logs.length; i++) {
                String log = logs[i].replace("=",";");
                if (log.startsWith("ID;")) {
                    segments[i] = CheckinHelper.createNamedSegment(log.substring(3).split(";"));
                    thisLogLength += 3;
                } else {
                    segments[i] = CheckinHelper.createUnnamedSegment(log.split(";"));
                }
            }
            DsCheckinEvent eventToAdd;
            if(mCurrentLength == 0) {
                eventToAdd = mCurrentLog = generateHeader(id, tag);
                mCurrentLength = mCurrentLog.length();
            }else if(mCurrentLength + thisLogLength > LOG_LIMIT) {
                if(mCurrentLength < thisLogLength) {
                    eventToAdd = generateHeader(id, tag);
                    mLogList.add(eventToAdd);
                    mCurrentLength -= thisLogLength; // This is reverted in the "if" block below
                }else {
                    mLogList.add(mCurrentLog);
                    eventToAdd = mCurrentLog = generateHeader(id, tag);
                    mCurrentLength = mCurrentLog.length();
                }
            }else {
                eventToAdd = mCurrentLog;
            }

            if (eventToAdd != null) { // The null check is to please klocworks
                for ( DsSegment segment : segments ) {
                    eventToAdd.addSegment(segment);
                }
            }

            mCurrentLength += thisLogLength;
        }
        
        public void checkin(LogController cr, String tag) {
            if(mCurrentLength > 0) mLogList.add(mCurrentLog);
            for(int i = 0; i < mLogList.size(); ++i) {
                cr.logEvent(tag, mLogList.get(i));
            }
        }
    }
    
    /**
     * @author w04917 Brian Lee
     * Some logs, like the PERF_STATS log, need to have a different header format
     * and need to have a check-in tag per log. 
     * The DirectLogPair does not merge multiple logs together under one check-in tag,
     * and does a separate check in for each log.
     * It checks in the passed in log as-is without any modification.
     */
    static class DirectLogPair extends LogPair {
        @Override
        public void log(String id, String logGroup, String tag) {
            String idStr = null;
            String verStr = null;
            String timeStr = null;
            DsCheckinEvent eventToAdd = null;

            if (logGroup == null) return;
            String logs[] = logGroup.split("\\[");

            for (String keyvalue : logs[0].split(";")) {
                if (keyvalue.isEmpty()) continue;
                String[] fields = keyvalue.split("=");
                if (fields.length < 2) return;

                String key = fields[0];
                String value = fields[1];
                if (key.equals("ID")) {
                    idStr = value;
                } else if (key.equals("time")) {
                    timeStr = value;
                    if (idStr == null || verStr == null || timeStr == null) return;
                    eventToAdd = CheckinHelper.getCheckinEvent(
                            tag, idStr, verStr, Long.valueOf(timeStr) );
                } else if (key.equals("ver")) {
                    verStr = value;
                } else {
                    if (eventToAdd == null) return;
                    eventToAdd.setValue(key, value);
                }
            }
            for (int i=1; i<logs.length; i++) {
                String[] fields = logs[i].split(";");
                if (fields.length < 1) return;

                String firstField = fields[0];
                if (firstField.startsWith("ID=")) {
                    String[] keyValue = firstField.split("=");
                    if (keyValue.length < 2) return;

                    DsSegment segment = CheckinHelper.createNamedSegment(keyValue[1]);
                    for (int j=1; j<fields.length; j++) {
                        keyValue = fields[i].split("=");
                        if (keyValue.length != 2) return;
                        segment.setValue(keyValue[0], keyValue[1]);
                    }

                    if (eventToAdd == null) return;
                    eventToAdd.addSegment(segment);
                } else {
                    DsSegment segment = CheckinHelper.createUnnamedSegment(firstField);
                    for (int j=1; j<fields.length; j++) {
                        segment.setValue(j, fields[j]);
                    }

                    if (eventToAdd == null) return;
                    eventToAdd.addSegment(segment);
                }
            }

            if (eventToAdd == null) return;
            mLogList.add(eventToAdd);
        }

        @Override
        public void checkin(LogController cr, String tag) {
            for(int i = 0; i < mLogList.size(); i++) {
                cr.logEvent(tag, mLogList.get(i));
            }
        }
    }

    static void updateTimeConstants() {
        sNow = (System.currentTimeMillis() / 1000);
        TimeZone tz = java.util.Calendar.getInstance().getTimeZone();
        boolean daylight = tz.inDaylightTime(new Date());

        sTz = tz.getRawOffset() +
                           (daylight ? tz.getDSTSavings() : 0);
    }

    static DsCheckinEvent generateHeader(String id, String tag) {
        String ver = VERSION;
        if (id.equals(EventConstants.DC_MULTIMEDIALOG_ID)) {  // dc_mmlog for camera params
            ver = EventConstants.DC_MMLOG_VERSION; // 4.8
        }

        DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent( tag, id, ver, sNow );
        if(wantTimeZone(id)) checkinEvent.setValue("tz",sTz);

        return checkinEvent;
    }

    static boolean wantTimeZone(String tag) {
        // TODO: Make this more organized
        return !("PMaps".equals(tag)
                 || EventConstants.DC_KEYBOARD_ID.equals(tag));
    }
}

