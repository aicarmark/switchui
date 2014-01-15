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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeSet;

import android.os.Bundle;
import android.util.Log;

import com.motorola.data.event.api.Segment;
import com.motorola.kpi.perfstats.LogSetting;
import com.motorola.kpi.perfstats.LogSetting.Metrics;
import com.motorola.kpi.perfstats.Logger;


/**
 * @author w04917 Brian Lee
 * Stores information about device network usage
 */
public class DeviceNetworkMetric extends Metric {
    private static final String TAG = "DevNetworkMetric";

    private static final Collection<String> NETWORK_DEVICE_LIST = new TreeSet<String>();

    private final HashMap<String, NetworkDevice> mNetworkDevices;

    private static final int NET_DEV_FIELDS = 17;
    private static final int NET_DEV_FIELD_RX_BYTES = 1;
    private static final int NET_DEV_FIELD_RX_ERRS = 3;
    private static final int NET_DEV_FIELD_TX_BYTES = 9;
    private static final int NET_DEV_FIELD_TX_ERRS = 11;
    private static final String STR_BYTES = "bytes";

    private static final String FIELD_DEVICE = "DEV";
    private static final String FIELD_RX = "RX";
    private static final String FIELD_RX_ERROR = "RXERR";
    private static final String FIELD_TX = "TX";
    private static final String FIELD_TX_ERROR = "TXERR";

    public DeviceNetworkMetric(LogSetting logSetting) {
        super(logSetting);

        if (NETWORK_DEVICE_LIST.isEmpty()) {
            for (String device : NetworkDeviceList.NETWORK_DEVICES) {
                if (device != null && !device.isEmpty()) {
                    NETWORK_DEVICE_LIST.add(device);
                }
            }
        }
        mNetworkDevices = new HashMap<String, NetworkDevice>();
    }

    @Override
    public void handleStart(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_NETWORK)) {
            long callStartTime = Logger.getCallTime();
            final boolean isBaseParsing = true;

            mNetworkDevices.clear();
            parseNetDev(isBaseParsing);

            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Logger.logExecutionTime(TAG, "handleStart", callStartTime);
            }
        }
    }

    @Override
    public void handleStop(Bundle bundle) {
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_NETWORK)) {
            long callStartTime = Logger.getCallTime();
            final boolean isBaseParsing = false;

            parseNetDev(isBaseParsing);

            if (PerformanceStatsService.LOGD && Log.isLoggable(TAG, Log.DEBUG)) {
                Logger.logExecutionTime(TAG, "handleStop", callStartTime);
            }
        }
    }

    public Collection<Segment> getCheckinSegments() {
        Collection<Segment> segmentList = null;
        if (mLogSetting != null && mLogSetting.isMetricLogged(Metrics.DEVICE_NETWORK) &&
                !mNetworkDevices.isEmpty()) {
            segmentList = new LinkedList<Segment>();

            Collection<NetworkDevice> devices = mNetworkDevices.values();
            for(NetworkDevice device : devices) {
                if (device != null &&
                        ((device.getRxBytes() > 0) || (device.getTxBytes() > 0)) ) {
                    Segment segment = new Segment(PerformanceStatsHandler.SEGMENT_NETWORK);
                    segment.setValue(FIELD_DEVICE, device.getDeviceName());
                    segment.setValue(FIELD_RX, device.getRxBytes());
                    segment.setValue(FIELD_RX_ERROR, device.getRxErrors());
                    segment.setValue(FIELD_TX, device.getTxBytes());
                    segment.setValue(FIELD_TX_ERROR, device.getTxErrors());
                    segmentList.add(segment);
                }
            }
        }
        return segmentList;
    }

    /**
     * Parses proc/net/dev and fills in mNetworkDevices
     * @param isBaseParsing if this parse result will be used as basis point
     */
    private void parseNetDev(boolean isBaseParsing) {
        BufferedReader br = null;
        boolean parseOk = false;

        try {
            br = new BufferedReader(new FileReader("/proc/net/dev"));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Unable to parse network device information");
        }

        String line = null;
        String deviceName = null;
        String[] fields = null;
        NetworkDevice networkDevice = null;

        if (br != null) {
            try {
                /* skip first line of header (categories) */
                if ((line = br.readLine()) != null) {
                    parseOk = line.trim().startsWith("Inter-");
                }

                /* skip second line of header(fields) */
                if (parseOk && (line = br.readLine()) != null) {
                    parseOk = false;
                    fields = line.trim().split("\\|");
                    /* basic sanity check */
                    parseOk = (fields.length == 3) && (fields[1].trim().startsWith(STR_BYTES)) &&
                              (fields[2].trim().startsWith(STR_BYTES));
                }

                /* sanitize line read since we'll be parsing it for numbers */
                while (parseOk && (line = Metric.sanitizedReadLine(br)) != null) {
                    parseOk = false;
                    fields = line.trim().split("\\s+");
                    if (fields.length == NET_DEV_FIELDS) {
                        /* remove the : at the end of the device name */
                        deviceName = fields[0].substring(0, fields[0].length()-1);
                        if (deviceName != null) {
                            if (isBaseParsing && NETWORK_DEVICE_LIST.contains(deviceName)) {
                                /* if we're parsing for base bytes, add new devices */
                                networkDevice = new NetworkDevice(deviceName,
                                                                  fields[NET_DEV_FIELD_RX_BYTES],
                                                                  fields[NET_DEV_FIELD_RX_ERRS],
                                                                  fields[NET_DEV_FIELD_TX_BYTES],
                                                                  fields[NET_DEV_FIELD_TX_ERRS]);
                                mNetworkDevices.put(deviceName, networkDevice);
                            } else if (!isBaseParsing && mNetworkDevices.containsKey(deviceName)) {
                                /* if we're parsing for current bytes,
                                 * only update existing devices and don't add new devices,
                                 * since the new devices don't have a basis point
                                 */
                                networkDevice = mNetworkDevices.get(deviceName);
                                networkDevice.update(
                                    fields[NET_DEV_FIELD_RX_BYTES],
                                    fields[NET_DEV_FIELD_RX_ERRS],
                                    fields[NET_DEV_FIELD_TX_BYTES],
                                    fields[NET_DEV_FIELD_TX_ERRS]);
                            }
                        }
                        parseOk = true;
                    }
                }
            } catch (IOException ioe) {
                Log.e(TAG, "Error parsing network device information lines");
            }
            try {
                br.close();
            } catch (IOException ioe) {
                Log.w(TAG, "Unable to close stream.");
            }
        }
    }

}
