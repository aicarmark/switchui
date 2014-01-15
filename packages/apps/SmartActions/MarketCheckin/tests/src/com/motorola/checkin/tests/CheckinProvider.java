/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/14   IKCTXTAW-481    Initial version
 */

package com.motorola.checkin.tests;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.test.mock.MockContentProvider;
import android.util.Log;

public class CheckinProvider extends MockContentProvider {
    private static final String TAG = CheckinProvider.class.getSimpleName();
    private static final long MAX_WAIT_TIME = 1000;

    // store checkin rows in a list
    private List<CheckinRow> mCheckinRows = new LinkedList<CheckinRow>();
    private static final String COL_TAG = "tag";
    private static final String COL_VALUE = "value";

    @Override
    public void attachInfo(Context context, ProviderInfo info) {
        // do nothing
    }

    /**
     * Object to store parsed check-in values for verification
     */
    static class CheckinRow {
        final String mCheckinTag;
        final ContentValues mCheckinEvent;
        final List<ContentValues> mSegments = new LinkedList<ContentValues>();

        CheckinRow(String checkinTag, ContentValues checkinEvent) {
            mCheckinTag = checkinTag;
            mCheckinEvent = checkinEvent;
        }

        void addSegment(ContentValues segment) {
            mSegments.add(segment);
        }
    }

    /**
     * Parses the CheckinSegment String into ContentValues
     * @param segment CheckinSegment in String representation
     * @return ContentValues with key-value pair
     */
    private ContentValues parseSegment(String segment) {
        ContentValues values = new ContentValues();
        if (segment != null) {
            String[] fields = segment.split(";");
            for (String field : fields) {
                int pos = field.indexOf('=');
                if (pos > 0 && ((pos+1) <= field.length())) {
                    // get the key/value pair from a single field
                    String key = field.substring(0, pos);
                    String value = field.substring(pos+1);
                    values.put(key, value);
                }
            }
        }
        return values;
    }

    @Override
    public Uri insert(Uri uri, ContentValues cv) {
        // get the event string and parse all fields and store them as ContentValues
        String eventString = cv.getAsString(COL_VALUE);
        // strip the brackets []
        if (eventString != null) {
            eventString = eventString.substring(1, eventString.length() - 1);

            // break each segment
            String[] segments = eventString.split(";\\]\\[");
            if (segments != null) {
                // Segment 0 is CheckinEvent
                ContentValues segmentValues = parseSegment(segments[0]);
                String checkinTag = cv.getAsString(COL_TAG);
                CheckinRow row = new CheckinRow(checkinTag, segmentValues);

                // the rest of the segments are CheckinSegments
                for (int i = 1; i < segments.length; i++) {
                    segmentValues = parseSegment(segments[i]);
                    row.addSegment(segmentValues);
                }
                synchronized(mCheckinRows) {
                    mCheckinRows.add(row);
                    mCheckinRows.notify();
                }
            }
        }
        return uri;
    }

    /**
     * @return a list of CheckinRows
     */
    public List<CheckinRow> getCheckinValues() {
        synchronized(mCheckinRows) {
            if (mCheckinRows.isEmpty()) {
                try {
                    mCheckinRows.wait(MAX_WAIT_TIME);
                } catch (InterruptedException e) {
                    Log.e(TAG, "getCheckinValues() interrupted.");
                }
            }
        }
        return Collections.unmodifiableList(mCheckinRows);
    }

    public void cleanup() {
        mCheckinRows.clear();
    }
}