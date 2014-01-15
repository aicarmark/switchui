package com.motorola.firewall;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.PhoneLookup;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.SimpleCursorAdapter;

import java.util.HashMap;
import java.util.LinkedList;


public abstract class SyncNumberAdapter extends SimpleCursorAdapter implements Runnable,
                 ViewTreeObserver.OnPreDrawListener {
    public static final int UPDATE_NUMBER = 1;
    public static final int UPDATE_CALLLOG = 2;
    public static final int UPDATE_SMSLOG = 3;
    
    public static final class ContactInfo {
        public long personId;
        public String name;
        public int type;
        public String label;
        public String number;
        public String formattedNumber;

        public static ContactInfo EMPTY = new ContactInfo();
    };
    
    public static final class CallerInfoQuery {
        String number;
        int position;
        String name;
        int numberType;
        String numberLabel;
    };
    
    static final String[] PHONES_PROJECTION = new String[] {
        PhoneLookup._ID, 
        PhoneLookup.DISPLAY_NAME, 
        PhoneLookup.TYPE, 
        PhoneLookup.LABEL, 
        PhoneLookup.NUMBER
    };

    static final int PERSON_ID_COLUMN_INDEX = 0;
    static final int NAME_COLUMN_INDEX = 1;
    static final int PHONE_TYPE_COLUMN_INDEX = 2;
    static final int LABEL_COLUMN_INDEX = 3;
    static final int MATCHED_NUMBER_COLUMN_INDEX = 4;
    
    private static final int COLUMN_INDEX_NUMBER = 1;
    private static final int COLUMN_INDEX_NAME = 2;
    private static final int COLUMN_INDEX_NUMBER_TYPE = 3;
    private static final int COLUMN_INDEX_NUMBER_LABEL = 4;
    
    HashMap<String, ContactInfo> mContactInfo;
    private final LinkedList<CallerInfoQuery> mRequests;
    private volatile boolean mDone;
    private boolean mLoading = true;
    ViewTreeObserver.OnPreDrawListener mPreDrawListener;
    private static final int REDRAW = 1;
    private static final int START_THREAD = 2;
    private boolean mFirst;
    private Thread mCallerIdThread;
    private Context mContext;

    public boolean onPreDraw() {
        if (mFirst) {
            mHandler.sendEmptyMessageDelayed(START_THREAD, 1000);
            mFirst = false;
        }
        return true;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REDRAW:
                    notifyDataSetChanged();
                    break;
                case START_THREAD:
                    startRequestProcessing();
                    break;
            }
        }
    };

    public SyncNumberAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);

        mContactInfo = new HashMap<String, ContactInfo>();
        mRequests = new LinkedList<CallerInfoQuery>();
        mPreDrawListener = null;
        mContext = context;
    }

    @Override
    public void bindView(View view, Context context, Cursor c) {
        super.bindView(view, context, c);
        String number = c.getString(COLUMN_INDEX_NUMBER);
        String callerName = c.getString(COLUMN_INDEX_NAME);
        int nametype = c.getInt(COLUMN_INDEX_NUMBER_TYPE);
        String namelable = c.getString(COLUMN_INDEX_NUMBER_LABEL);
        // Lookup contacts with this number
        ContactInfo info = mContactInfo.get(number);
        if (info == null) {
            // Mark it as empty and queue up a request to find the name
            // The db request should happen on a non-UI thread
            info = ContactInfo.EMPTY;
            mContactInfo.put(number, info);
            enqueueRequest(number, c.getPosition(), callerName, nametype, namelable);
        } else if (info != ContactInfo.EMPTY) { // Has been queried
            // Check if any data is different from the data cached in the
            // calls db. If so, queue the request so that we can update
            // the calls db.
            if (!TextUtils.equals(info.name, callerName)) {
                // Something is amiss, so sync up.
                enqueueRequest(number, c.getPosition(), callerName, nametype, namelable);
            }
        }
        if (mPreDrawListener == null) {
            mFirst = true;
            mPreDrawListener = this;
            view.getViewTreeObserver().addOnPreDrawListener(this);
        }
    }

    void setLoading(boolean loading) {
        mLoading = loading;
    }

    @Override
    public boolean isEmpty() {
        if (mLoading) {
            // We don't want the empty state to show when loading.
            return false;
        } else {
            return super.isEmpty();
        }
    }

    public ContactInfo getContactInfo(String number) {
        return mContactInfo.get(number);
    }

    public void startRequestProcessing() {
        mDone = false;
        mCallerIdThread = new Thread(this);
        mCallerIdThread.setPriority(Thread.MIN_PRIORITY);
        mCallerIdThread.start();
    }

    public void stopRequestProcessing() {
        mDone = true;
        if (mCallerIdThread != null)
            mCallerIdThread.interrupt();
    }

    public void clearCache() {
        synchronized (mContactInfo) {
            mContactInfo.clear();
        }
    }
    
    abstract protected  void updateList(CallerInfoQuery ciq, ContactInfo ci);

    private void enqueueRequest(String number, int position, String name ,
            int nametype, String namelabel) {
        CallerInfoQuery ciq = new CallerInfoQuery();
        ciq.number = number;
        ciq.position = position;
        ciq.name = name;
        ciq.numberType = nametype;
        ciq.numberLabel = namelabel;
        synchronized (mRequests) {
            mRequests.add(ciq);
            mRequests.notifyAll();
        }
    }

    private void queryContactInfo(CallerInfoQuery ciq) {
        // First check if there was a prior request for the same number
        // that was already satisfied
        ContactInfo info = mContactInfo.get(ciq.number);
        if (info != null && info != ContactInfo.EMPTY) {
            synchronized (mRequests) {
                if (mRequests.isEmpty()) {
                    mHandler.sendEmptyMessage(REDRAW);
                }
            }
        } else {
            Cursor phonesCursor = mContext.getContentResolver().query(
                    Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(ciq.number)),
                    PHONES_PROJECTION, null, null, null);
            if (phonesCursor != null) {
                if (phonesCursor.moveToFirst()) {
                    info = new ContactInfo();
                    info.personId = phonesCursor.getLong(PERSON_ID_COLUMN_INDEX);
                    info.name = phonesCursor.getString(NAME_COLUMN_INDEX);
                    info.type = phonesCursor.getInt(PHONE_TYPE_COLUMN_INDEX);
                    info.label = phonesCursor.getString(LABEL_COLUMN_INDEX);
                    info.number = phonesCursor.getString(MATCHED_NUMBER_COLUMN_INDEX);

                    // New incoming phone number invalidates our formatted
                    // cache. Any cache fills happen only on the GUI thread.
                    info.formattedNumber = null;

                    mContactInfo.put(ciq.number, info);
                    // Inform list to update this item, if in view
                    synchronized (mRequests) {
                        if (mRequests.isEmpty()) {
                            mHandler.sendEmptyMessage(REDRAW);
                        }
                    }
                }
                phonesCursor.close();
            }
        }
        if (info != null) {
            updateList(ciq, info);
        }
    }

    public void run() {
        while (!mDone) {
            CallerInfoQuery ciq = null;
            synchronized (mRequests) {
                if (!mRequests.isEmpty()) {
                    ciq = mRequests.removeFirst();
                } else {
                    try {
                        mRequests.wait(1000);
                    } catch (InterruptedException ie) {
                        // Ignore and continue processing requests
                    }
                }
            }
            if (ciq != null) {
                queryContactInfo(ciq);
            }
        }
    }
}
