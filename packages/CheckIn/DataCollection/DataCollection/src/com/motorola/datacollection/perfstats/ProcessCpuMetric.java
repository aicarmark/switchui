/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2011/11/01   IKCTXTAW-359    Initial version
 * w04917 (Brian Lee)          2012/02/14   IKCTXTAW-441    Use the new Checkin API
 *
 */

package com.motorola.datacollection.perfstats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.motorola.data.event.api.Segment;
import com.motorola.kpi.perfstats.LogSetting;
import com.motorola.kpi.perfstats.LogSetting.Metrics;
import com.motorola.kpi.perfstats.Logger;
import com.motorola.kpi.perfstats.ProcessCpuMetricCollector.ProcessCpuMetricData;
import com.motorola.kpi.perfstats.ProcessCpuMetricCollector.ProcessCpuRawData;

/**
 * @author w04917 Brian Lee
 * Stores information about CPU metric per active process
 */
public class ProcessCpuMetric extends Metric {
    private static final String TAG = "ProcCpuMetric";

    private final ArrayList<ProcessCpuData> mProcessData;
    /* uptimes are in ms, not jiffies */
    private long mBaseUptime, mCurrentUptime;

    private static final String FIELD_NAME = "NAME";
    private static final String FIELD_USER = "USER";
    private static final String FIELD_SYSTEM = "SYS";

    public ProcessCpuMetric(LogSetting logSetting) {
        super(logSetting);
        mProcessData = new ArrayList<ProcessCpuData>();
    }

    @Override
    public void handleStart(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.PROCESS_CPU)
                && bundle != null) {
            long callStartTime = SystemClock.uptimeMillis();

            ProcessCpuMetricData metricData = ProcessCpuMetricData.unpack(bundle);
            if (metricData != null && metricData.isValid()) {
                mBaseUptime = metricData.mUptime;
                mProcessData.clear();
                for (ProcessCpuRawData process : metricData.mProcesses) {
                    if (process != null) {
                        ProcessCpuData pData = new ProcessCpuData(process.mPid,
                                process.mUserTime, process.mSystemTime, process.mBaseName);
                        mProcessData.add(pData);
                    }
                }
            }

            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Logger.logExecutionTime(TAG, "handleStart", callStartTime);
            }
        }
    }

    @Override
    public void handleStop(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.PROCESS_CPU)
                && bundle != null) {
            long callStartTime = SystemClock.uptimeMillis();

            ProcessCpuMetricData metricData = ProcessCpuMetricData.unpack(bundle);
            if (metricData != null && metricData.isValid()) {
                mCurrentUptime = metricData.mUptime;

                int index = 0;
                for(index = 0; (index < mProcessData.size()) &&
                        (index < metricData.mProcesses.size()); index++) {
                    /* the lists are sorted by pid */
                    ProcessCpuData baseProcess = mProcessData.get(index);
                    ProcessCpuRawData currentProcess = metricData.mProcesses.get(index);

                    if (baseProcess == null) {
                        mProcessData.remove(index);
                        index--;
                        continue;
                    }

                    if (currentProcess == null) {
                        metricData.mProcesses.remove(index);
                        index--;
                        continue;
                    }

                    if (baseProcess.mPid == currentProcess.mPid) {
                        /* update process */
                        baseProcess.mCurrentUserTime = currentProcess.mUserTime;
                        baseProcess.mCurrentSystemTime = currentProcess.mSystemTime;
                    } else if (baseProcess.mPid < currentProcess.mPid) {
                        /* process gone, skip */
                        mProcessData.remove(index);
                        index--;
                    } else {
                        /* new process, skip */
                        metricData.mProcesses.remove(index);
                        index--;
                    }
                }

                /* remove processes that are gone, if any are left */
                for (int i = index; i < mProcessData.size(); i++) {
                    mProcessData.remove(i);
                }
            }

            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Logger.logExecutionTime(TAG, "handleStop", callStartTime);
            }
        }
    }

    private static final Comparator<ProcessCpuData> SORT_BY_CPU_TIME_DESC =
    new Comparator<ProcessCpuData>() {
        public int compare(ProcessCpuData p1, ProcessCpuData p2) {
            if (p1 == null && p2 == null) {
                return 0;
            } else if (p1 == null) {
                return 1;
            } else if (p2 == null) {
                return -1;
            }

            long p1Time = p1.getUserTime() + p1.getSystemTime();
            long p2Time = p2.getUserTime() + p2.getSystemTime();

            /* more time = less, since we're sorting in descending order */
            if (p1Time == p2Time) {
                if (p1.getUserTime() < p2.getUserTime()) {
                    /* if total time is equal, sort on user time */
                    return 1;
                } else if (p1.getUserTime() > p2.getUserTime()) {
                    return -1;
                } else {
                    return 0;
                }
            } else if (p1Time < p2Time) {
                return 1;
            } else {
                return -1;
            }
        }
    };

    public Collection<Segment> getCheckinSegments() {
        Collection<Segment> segmentList = null;

        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.PROCESS_CPU)
                && !mProcessData.isEmpty()) {
            long callStartTime = 0;
            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                callStartTime = SystemClock.uptimeMillis();
            }

            /* copy mProcessData, which is sorted by pid, and sort the copied list by usage time */
            ArrayList<ProcessCpuData> outputList = new ArrayList<ProcessCpuData>(mProcessData);
            Collections.sort(outputList, SORT_BY_CPU_TIME_DESC);
            long uptimeJiffies = (mCurrentUptime - mBaseUptime + 5) / 10;
            if (uptimeJiffies > 0) {
                segmentList = new LinkedList<Segment>();
                for (int i = 0; (i < mLogSetting.getMaxNumProcess() &&
                                 i < outputList.size()); i++) {
                    ProcessCpuData pData = outputList.get(i);
                    if (pData != null) {
                        if (pData.getUserTime() == 0 && pData.getSystemTime() == 0) {
                            /* only show active processes */
                            break;
                        }
                        String userPercent = getPercent(pData.getUserTime(), uptimeJiffies);
                        String systemPercent = getPercent(pData.getSystemTime(), uptimeJiffies);

                        Segment segment = new Segment(PerformanceStatsHandler.SEGMENT_PROCESS);
                        segment.setValue(FIELD_NAME, pData.getName());
                        segment.setValue(FIELD_USER, userPercent);
                        segment.setValue(FIELD_SYSTEM, systemPercent);
                        segmentList.add(segment);
                    }
                }
            }
            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Logger.logExecutionTime(TAG, "output", callStartTime);
            }
        }
        return segmentList;
    }

    private String getPercent(long numerator, long denominator) {
        long thousands = (numerator*1000)/denominator;
        long hundreds = thousands/10;
        String percent = String.valueOf(hundreds);
        if (hundreds < 10) {
            long remainder = thousands - (hundreds*10);
            if (remainder != 0) {
                percent = percent + "." + remainder;
            }
        }
        return percent;
    }
}
