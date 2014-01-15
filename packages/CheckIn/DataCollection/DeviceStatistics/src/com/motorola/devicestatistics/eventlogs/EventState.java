package com.motorola.devicestatistics.eventlogs;

import android.util.EventLog;

abstract class EventState {
    abstract void reset();
    abstract boolean hasChanged(int id, EventLog.Event e);
    abstract void getLog(int id, ILogger logger);
    abstract void dumpLog(ILogger logger);
}

