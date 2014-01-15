/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 * w04917 (Brian Lee)        2011/10/13   IKCTXTAW-368    Update check-in string format
 * w04917 (Brian Lee)        2012/02/14   IKCTXTAW-441    Use the new Checkin API
 * w04917 (Brian Lee)        2012/02/21   IKCTXTAW-442    Detailed meter data checkin event change.
 *
 */

package com.motorola.datacollection.meter.checkin;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;

import com.motorola.collectionservice.CollectionServiceAPI;
import com.motorola.data.event.api.Segment;
import com.motorola.datacollection.CheckinEventHelper;

/** Generic data structure used for check-in of meter reading values
 * @author w04917 (Brian Lee)
 *
 */
public class CheckInMeterData {
    private static final String TAG = "CheckInMeterData";

    static final String CHECK_IN_TAG_POWER_TOTAL = "MOT_MTR_PWR_TOTAL";
    static final String CHECK_IN_EVENT_POWER_TOTAL = "PWR_TOTAL";
    static final String CHECK_IN_EVENT_POWER_TOTAL_VERSION = "4";

    static final String CHECK_IN_TAG_POWER_DETAIL = "MOT_MTR_PWR_DETAIL";

    /* these meter reading fields have to match the variable names in MeterReading class */
    private static final String METER_READING_FIELD_VERSION = "version";

    /* fields used in check-in segment */
    private static final String FIELD_TIME = "time";
    private static final String FIELD_TOTAL = "totalUwh";

    /* reserved fields by CheckinEvent */
    private static final String[] RESERVED_EVENT_FIELDS = {
        "ver",
        "time"
    };

    private MeterData[] mMeterData;

    public CheckInMeterData(List<Bundle> meterBundles) {
        if (meterBundles != null) {
            int size = meterBundles.size();
            mMeterData = new MeterData[size];

            String type = null;
            String rawData = null;
            long timeStamp = -1;
            int totalUwh = -1;
            String[] tokens;
            for (int i=0; i < size; i++) {
                type = CollectionServiceAPI.getDataType(meterBundles.get(i));
                timeStamp = CollectionServiceAPI.getTimestamp(meterBundles.get(i));
                totalUwh = CollectionServiceAPI.getTotalUwh(meterBundles.get(i));
                rawData = CollectionServiceAPI.getRawData(meterBundles.get(i));

                if (type != null && timeStamp >= 0 && totalUwh >= 0 && rawData != null) {
                    /* just use the class name for type, not the full canonical name */
                    tokens = type.split("\\.");
                    if (tokens.length > 0)  {
                        type = tokens[tokens.length-1];
                    }

                    mMeterData[i] = new MeterData(type, timeStamp, totalUwh, rawData);
                }
            }
        }
    }

    Collection<Segment> getSegmentsTotal() {
        Collection<Segment> segmentList = null;

        if (mMeterData != null && mMeterData.length > 0) {
            segmentList = new LinkedList<Segment>();

            for (MeterData meterData : mMeterData) {
                if (meterData != null) {
                    Segment segment = meterData.getSegmentTotal();
                    if (segment != null) {
                        segmentList.add(segment);
                    }
                }
            }

            if (segmentList.isEmpty()) {
                segmentList = null;
            }
        }
        return segmentList;
    }

    Collection<CheckinEventHelper> getCheckinEventsDetail() {
        Collection<CheckinEventHelper> checkinEventList = null;

        if (mMeterData != null && mMeterData.length > 0) {
            checkinEventList = new LinkedList<CheckinEventHelper>();

            for (MeterData meterData : mMeterData) {
                if (meterData != null) {
                    CheckinEventHelper checkinEvent = meterData.getCheckinEventDetail();
                    if (checkinEvent != null) {
                        checkinEventList.add(checkinEvent);
                    }
                }
            }

            if (checkinEventList.isEmpty()) {
                checkinEventList = null;
            }
        }

        return checkinEventList;
    }

    private static class MeterData {
        final String mType;
        final long mTimestamp;
        final String mRawData;
        final int mTotalUwh;

        MeterData(String type, long timestamp, int totalUwh, String rawData) {
            mType = type;
            mTimestamp = timestamp;
            mTotalUwh = totalUwh;
            mRawData = rawData;
        }

        Segment getSegmentTotal() {
            Segment segment = new Segment(mType);
            try {
                segment.setValue(FIELD_TIME, mTimestamp);
                segment.setValue(FIELD_TOTAL, mTotalUwh);
            } catch (IllegalArgumentException iae) {
                Log.e(TAG, "Unable to set total segment values for " + mType);
                segment = null;
            }
            return segment;
        }

        /**
         * We need to use separate CheckinEvent for each detailed Meter data.
         * This is because each meter data fields can change, along with their
         * version number. Unless we do it as separate events (vs segments), the
         * check-in server parser won't be able to handle it.
         * @return CheckinEventHelper object with meter data
         */
        CheckinEventHelper getCheckinEventDetail() {
            CheckinEventHelper checkinEvent = null;
            boolean valueAdded = false;
            try {
                /* get the raw data fields */
                JSONObject jsonObject = new JSONObject(mRawData);

                /* get version of this meter data to use as event version */
                String version = String.valueOf(jsonObject.getString(METER_READING_FIELD_VERSION));
                checkinEvent = new CheckinEventHelper(CHECK_IN_TAG_POWER_DETAIL, mType, version,
                                                      mTimestamp);

                /* set totalUwh first */
                checkinEvent.setValue(FIELD_TOTAL, mTotalUwh);

                /* sort the raw data field keys */
                Iterator keys = jsonObject.keys();
                List<String> sortedKeys = new LinkedList<String>();
                while (keys.hasNext()) {
                    String key = String.valueOf(keys.next());
                    if (key != null && !key.isEmpty()) {
                        sortedKeys.add(key);
                    }
                }
                Collections.sort(sortedKeys);

                /* remove the fields already added */
                sortedKeys.remove(METER_READING_FIELD_VERSION);
                sortedKeys.remove(FIELD_TOTAL);

                /* remove variables with reserved names */
                for (String reservedField : RESERVED_EVENT_FIELDS) {
                    sortedKeys.remove(reservedField);
                }

                /* added the rest of the sorted key-value pair to segment */
                for (String key : sortedKeys) {
                    String value = String.valueOf(jsonObject.getString(key));
                    checkinEvent.setValue(key, value);
                    valueAdded = true;
                }
            } catch (JSONException e) {
                Log.e(TAG, "Unable to create JSONObject");
                valueAdded = false;
            } catch (IllegalArgumentException iae) {
                Log.e(TAG, mType + " has invalid values for checkin!");
                valueAdded = false;
            }

            if (!valueAdded) {
                checkinEvent = null;
            }
            return checkinEvent;
        }
    }
}
