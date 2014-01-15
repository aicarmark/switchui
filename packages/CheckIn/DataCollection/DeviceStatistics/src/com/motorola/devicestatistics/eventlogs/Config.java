/**
 * Copyright (C) 2010, Motorola, Inc,
 * All Rights Reserved
 * Class name: EventConfig.java
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

import java.util.HashMap;

import android.content.Intent;

import com.motorola.devicestatistics.eventlogs.EventConstants.Events;
import com.motorola.devicestatistics.eventlogs.EventConstants.RawEvents;

/**
 * @author bluremployee
 *
 */
public class Config {
        
    public final static String[] TAGS = new String[] {
        null,                  // This should always be first entry
        "MOT_DEVICE_STATS_L1",
        "MOT_DEVICE_STATS_L2",
        "MOT_DEVICE_STATS_L3",
        "MOT_DEVICE_STATS_KBD",
        "MOT_DEVICE_STATS_MM",
        "MOT_DEVICE_STATS_PERF",
        "MOT_DEVICE_STATS_CAM",
        "MOT_DEVICE_STATS",    // This should always be last entry
    };
    
    public final static int DISABLED = 0;
    public final static int ALWAYS_ON = TAGS.length - 1;
    
    public final static class ReceiverConfig {
        public final static String[][] ACTIONS = {
            null,
            new String[] {
                Intent.ACTION_BATTERY_LOW,
                Intent.ACTION_SHUTDOWN,
            },
            null,
            null,
            null,
            null,
            null
        };
        
        public final static HashMap<String, Integer> LEVELMAP = new HashMap<String, Integer>();
        
        static {
            LEVELMAP.put(Events.BATTLOW, 1);
            LEVELMAP.put(Events.BOOTUP, 1);
            LEVELMAP.put(Events.SHUTDOWN, 1);
            LEVELMAP.put(Events.CHARGE, ALWAYS_ON);
            LEVELMAP.put(Events.DISCHARGE, ALWAYS_ON);
            LEVELMAP.put(Events.BATTSTS, 1);
            LEVELMAP.put(Events.BATTLVL, 1);
            LEVELMAP.put(Events.CHARGEFULL, 1);
            LEVELMAP.put(Events.SETTINGSTAT, 1);
        }
    }
    
    public final static class EventConfig {

        public final static HashMap<String, Integer> LEVELMAP = new HashMap<String, Integer>();

        static {
            LEVELMAP.put(Events.SCREEN_STATE, 1);
            LEVELMAP.put(Events.AM_RESUME_ACTIVITY, 2);
            LEVELMAP.put(Events.AM_PAUSE_ACTIVITY, 2);
            LEVELMAP.put(Events.AM_RELAUNCH_RESUME_ACTIVITY, 2);
            LEVELMAP.put(Events.AM_RESTART_ACTIVITY, 2);
            LEVELMAP.put(Events.DSBT_STATUS, 2);
            LEVELMAP.put(Events.DC_AUTOCMPL, 2);
            LEVELMAP.put(Events.DC_VIB, 2);
            LEVELMAP.put(Events.DC_SND, 2);
            LEVELMAP.put(Events.DC_KEYBOARD, 4);
            LEVELMAP.put(Events.DC_MM, 5);
            LEVELMAP.put(Events.PERF_STATS, 6);
            LEVELMAP.put(Events.DC_MMLOG, 7);
        }
    }

    public final static class AddnEventConfig {

        public final static HashMap<String, Integer> LEVELMAP = new HashMap<String, Integer>();

        static {
            LEVELMAP.put(Events.AM_RESUME_ACTIVITY, 3);
            LEVELMAP.put(Events.AM_PAUSE_ACTIVITY, 3);
        }
    }
    
    private int mLevel;
    private String[] mReceiverActions;
    private String[] mEventFilter;
    private int[] mSizeControls;
    
    public Config(int level, String sizes) {
        mLevel = level;
        mSizeControls = parseSizes(sizes);
    }
    
    public void initialize() {
        initReceiverConfig();
        initEventLogConfig();
    }
    
    // These are for pre-collection filtering
    public String[] getReceiverConfig() {
        return mReceiverActions;
    }
    
    public String[] getEventConfig() {
        return mEventFilter;
    }
    
    public boolean isEnabled() {
        return mLevel != DISABLED;
    }
    
    public int getConfigLevel() {
        return mLevel;
    }

    public int getNumLevels() {
        return TAGS.length;
    }
    
    // These are for log post processing
    public boolean isEnabled(int level) {
        return level != DISABLED &&
                (level == ALWAYS_ON || level <= mLevel);
    }
        
    public String getTag(int level) {
        return TAGS[level];
    }
    
    public int getLevel(int source, int type) {
        switch(source) {
        case EventConstants.Source.EVENTLOG:
            // no event int types currently supported
            return DISABLED;
        case EventConstants.Source.HELPER:
            return ALWAYS_ON;
        case EventConstants.Source.RECEIVER:
            // no receiver int types currently supported
            return DISABLED;
        case EventConstants.Source.ADDNL_LOG:
            // no event int types currently supported
            return DISABLED;
        }
        return DISABLED;
    }
    
    public int getLevel(int source, String type) {
        Integer i;
        switch(source) {
        case EventConstants.Source.EVENTLOG:
            i = EventConfig.LEVELMAP.get(type);
            if(i == null) return DISABLED;
            return i;
        case EventConstants.Source.HELPER:
            return ALWAYS_ON;
        case EventConstants.Source.RECEIVER:
            i = ReceiverConfig.LEVELMAP.get(type);
            if(i == null) return DISABLED;
            return i;
        case EventConstants.Source.ADDNL_LOG:
            i = AddnEventConfig.LEVELMAP.get(type);
            if(i == null) return DISABLED;
            return i;
        }
        return DISABLED;
    }

    // Size controls
    public int getSizeFilter(int level) {
        if(level == DISABLED) return 0;
        if(level == ALWAYS_ON) return -1;
        
        return mSizeControls[level];
    }

    public int[] getSizeFilters() {
        return mSizeControls;
    }

    public int getLevel(String tag) {
        if(tag == null) return 0;
        for(int i = 0; i < TAGS.length; ++i) {
            if(tag.equals(TAGS[i])) return i;
        }
        return -1;
    }

    private void initEventLogConfig() {
        mEventFilter = new String[Events.EVENT_FILTER.length + RawEvents.EVENT_FILTER.length];
        for(int i = 0; i < mEventFilter.length; ++i) {
            String filter = null;
            if(i < Events.EVENT_FILTER.length) {
                filter = Events.EVENT_FILTER[i];
            }else {
                int index = i - Events.EVENT_FILTER.length;
                if(index < RawEvents.EVENT_FILTER.length) {
                    filter = RawEvents.EVENT_FILTER[index];
                }
            }
            if(filter == null) {
                mEventFilter[i] = filter;
            }else {
                mEventFilter[i] = EventConfig.LEVELMAP.get(filter) > mLevel
                        ? null : filter;
            }
        }
    }

    private void initReceiverConfig() {
        mReceiverActions = null;
        int len = 0;
        for(int i = 0; i < ReceiverConfig.ACTIONS.length; ++i) {
            if(i <= mLevel && ReceiverConfig.ACTIONS[i] != null) {
                len += ReceiverConfig.ACTIONS[i].length;
            }
        }
        
        if(len > 0) {
            mReceiverActions = new String[len];
            int k = 0;
            for(int i = 0; i < ReceiverConfig.ACTIONS.length; ++i) {
                if(i <= mLevel && ReceiverConfig.ACTIONS[i] != null) {
                    String[] sub = ReceiverConfig.ACTIONS[i];
                    for(int j = 0; j < sub.length; ++j) {
                        mReceiverActions[k] = sub[j];
                        k++;
                    }
                }
            }
        }
    }

    private int[] parseSizes(String sizes) {
        String[] parts = sizes.split(",");
        int[] numbers = new int[parts.length];
        for(int i = 0; i < numbers.length; ++i) {
            try {
                numbers[i] = Integer.parseInt(parts[i]);
            }catch(NumberFormatException nfEx) {
                numbers[i] = 0;
            }
        }
        // First entry is always 0 and last is always -1
        numbers[0] = 0;
        numbers[parts.length-1] = -1;
        return numbers;
    }
}

