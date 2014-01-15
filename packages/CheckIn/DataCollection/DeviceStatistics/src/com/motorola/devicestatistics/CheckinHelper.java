package com.motorola.devicestatistics;

import android.content.ContentResolver;

import com.motorola.android.provider.CheckinEvent;
import com.motorola.data.event.api.Segment;

public final class CheckinHelper {
    private static final String TAG = "CheckinHelper";

    public static final DsCheckinEvent getCheckinEvent(String tag,
            String id, String version, long timestamp, String... args) {
        DsCheckinEvent checkinEvent = new DsCheckinEvent(tag,id,version,timestamp);
        for (int i=0; i+1<args.length; i+=2) {
            checkinEvent.setValue(args[i], String.valueOf(args[i+1]));
        }
        return checkinEvent;
    }

    public static final void addKeyValues(DsCheckinEvent checkinEvent, String... args) {
        for (int i=0; i+1<args.length; i+=2) {
            checkinEvent.setValue(args[i], args[i+1]);
        }
    }

    public static final DsSegment createNamedSegment(String... args) {
        DsSegment segment = new DsSegment(false, args[0]);
        for (int i=1; i+1<args.length; i+=2) {
            segment.setValue(args[i], args[i+1]);
        }
        return segment;
    }

    public static final void addNamedSegment(DsCheckinEvent event, String... args) {
        DsSegment segment = createNamedSegment(args);
        event.addSegment(segment);
    }

    public static final DsSegment createUnnamedSegment(String... args) {
        DsSegment segment = new DsSegment(true, args[0]);
        for (int i=1; i<args.length; i++) {
            segment.setValue(i, args[i]);
        }
        return segment;
    }

    public static final void addUnnamedSegment(DsCheckinEvent checkinEvent,
            String... args ) {
        DsSegment segment = createUnnamedSegment(args);
        checkinEvent.addSegment(segment);
    }

    public static final class DsCheckinEvent {
        private int mLength;
        private CheckinEvent mCheckinEvent;

        public DsCheckinEvent(String tag, String id, String version, long timestamp) {
            // [ID=id;ver=version;time=timestamp;]
            String time = String.valueOf(timestamp);
            mLength = 4 + id.length() + 5 + version.length() + 6 + time.length() + 1 + 1; // 1 for ]
            mCheckinEvent = new CheckinEvent(tag,id,version,timestamp);
        }

        public void setValue(String key, String value) {
            if (key == null) key = "null";
            if (value == null) value = "null";

            mCheckinEvent.setValue(key, value);
            mLength += key.length() + 1 + value.length() + 1;
        }

        public void setValue(String key, long value) {
            setValue(key, String.valueOf(value));
        }

        public void addSegment(DsSegment segment) {
            segment.addParent(this, mCheckinEvent);
            mLength += segment.length();
        }

        public StringBuilder serializeEvent() {
            return mCheckinEvent.serializeEvent();
        }

        public String getTagName() {
            return mCheckinEvent.getTagName();
        }

        public void publish(ContentResolver cr) {
            TagSizeLimiter.log(this, cr);
        }

        public void forcePublish(ContentResolver cr) {
            try {
                mCheckinEvent.publish(cr);
            } catch (Exception e) {
                DevStatUtils.logExceptionMessage(TAG, "Publish threw exception", e);
            }
        }

        public void addLength(int extraLength) {
            mLength += extraLength;
        }

        public int length() {
            return mLength;
        }
    }

    public static final class DsSegment {
        private final Segment mSegment;
        private int mLength;
        private DsCheckinEvent mParent;

        public DsSegment(boolean positionBased, String name) {
            mSegment = new Segment(name);
            if (positionBased) {
                mLength = 1 + 1; // []
            } else {
                mLength = 4 + 1; // [ID=]
            }
            mLength += name.length() + 1;
        }

        public void setValue(String key, String value) {
            if (key == null) key = "null";
            if (value == null) value = "null";

            // string=string2;
            mSegment.setValue(key, value);

            int extraLength = key.length() + 1 + value.length() + 1;
            mLength += extraLength;
            if (mParent != null) mParent.addLength(extraLength);
        }

        public void setValue(int position, String encodedName) {
            if (encodedName == null) encodedName = "null";

            // encodedName;
            mSegment.setValue(position, encodedName);
            int extraLength = encodedName.length() + 1;
            mLength += extraLength;
            if (mParent != null) mParent.addLength(extraLength);
        }

        public int length() {
            return mLength;
        }

        public void addParent(DsCheckinEvent dsCheckinEvent, CheckinEvent frameworkEvent) {
            mParent = dsCheckinEvent;
            frameworkEvent.addSegment(mSegment);
        }
    }
}
