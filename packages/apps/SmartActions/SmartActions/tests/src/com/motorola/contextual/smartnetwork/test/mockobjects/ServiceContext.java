/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 */

package com.motorola.contextual.smartnetwork.test.mockobjects;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;
import android.util.Log;

public class ServiceContext extends MockContext {
    private static final String TAG = ServiceContext.class.getSimpleName();

    // isolated test db file generation and clean-up related
    private static final String FILE_PREFIX = "test_";
    private static final String JOURNAL_SUFFIX = "-journal";
    private static final long MAX_WAIT_TIME = 1000;

    // blur checkin
    private static final String AUTHORITY_CHECK_IN = "com.motorola.android.server.checkin";
    private final ContentProvider mCheckinProvider = new CheckinProvider();
    // store checkin values in a list
    private List<ContentValues> mCheckinValues = new LinkedList<ContentValues>();

    /**
     * MockContentProvider to intercept Checkin.
     * com.motorola.android.provider.Checkin eventually checks into the CheckinProvider
     */
    private class CheckinProvider extends MockContentProvider {
        private static final String COL_TAG = "tag";
        private static final String COL_VALUE = "value";

        @Override
        public void attachInfo(Context context, ProviderInfo info) {
            // do nothing
        }

        @Override
        public Uri insert(Uri uri, ContentValues cv) {
            // get the event string and parse all fields and store them as ContentValues
            ContentValues values = new ContentValues();
            values.put(COL_TAG, cv.getAsString(COL_TAG));

            String eventString = cv.getAsString(COL_VALUE);
            if (eventString != null) {
                // strip the brackets []
                eventString = eventString.substring(1, eventString.length() - 1);

                // get individual fields
                String[] fields = eventString.split(";");
                for (String field : fields) {
                    int pos = field.indexOf('=');
                    if (pos > 0 && ((pos+1) <= field.length())) {
                        // get the key/value pair from a single field
                        String key = field.substring(0, pos);
                        String value = field.substring(pos+1);
                        values.put(key, value);
                    }
                }
                synchronized(mCheckinValues) {
                    mCheckinValues.add(values);
                    mCheckinValues.notify();
                }
            } else {
                Log.e(TAG, "Unable to get event string.");
            }
            return uri;
        }
    }

    private Context mBaseContext;
    private final MockContentResolver mResolver = new MockContentResolver();
    // created db list
    private final List<String> mDbList = new LinkedList<String>();

    private final List<Intent> mIntentList = new LinkedList<Intent>();
    private final List<BroadcastReceiver> mReceiverList = new LinkedList<BroadcastReceiver>();

    public ServiceContext(Context context) {
        mBaseContext = context;
        // hijack blur checkin
        mResolver.addProvider(AUTHORITY_CHECK_IN, mCheckinProvider);
    }

    public void cleanup() {
        // delete all database files created by this test context
        for (String db : mDbList) {
            mBaseContext.deleteDatabase(db);
            mBaseContext.deleteDatabase(db + JOURNAL_SUFFIX);
        }
        mIntentList.clear();
        mReceiverList.clear();
        mCheckinValues.clear();
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        return mBaseContext.getApplicationInfo();
    }

    @Override
    public ContentResolver getContentResolver() {
        // return the MockContentResolver
        return mResolver;
    }

    @Override
    public String getPackageName() {
        return mBaseContext.getPackageName();
    }

    @Override
    public Object getSystemService(String name) {
        return mBaseContext.getSystemService(name);
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String file, int mode, CursorFactory factory,
            DatabaseErrorHandler errorHandler) {
        // isolate test database
        file = FILE_PREFIX + file;
        mDbList.add(file);
        return mBaseContext.openOrCreateDatabase(file, mode, factory, errorHandler);
    }

    @Override
    public Intent registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        synchronized(mReceiverList) {
            if (receiver != null) {
                mReceiverList.add(receiver);
            }
            mReceiverList.notify();
        }
        return null;
    }

    @Override
    public void unregisterReceiver(BroadcastReceiver receiver) {
        synchronized(mReceiverList) {
            if (receiver != null) {
                mReceiverList.remove(receiver);
            }
            mReceiverList.notify();
        }
    }

    public BroadcastReceiver getReceiver() {
        BroadcastReceiver receiver = null;
        synchronized(mReceiverList) {
            if (mReceiverList.isEmpty()) {
                try {
                    mReceiverList.wait(MAX_WAIT_TIME);
                } catch (InterruptedException e) {
                    Log.e(TAG, "getReceiver() interrupted.");
                }
            }
        }
        if (!mReceiverList.isEmpty()) {
            receiver = mReceiverList.remove(0);
        }
        return receiver;
    }

    @Override
    public ComponentName startService(Intent service) {
        // some components may do things on background thread
        synchronized(mIntentList) {
            if (service != null) {
                mIntentList.add(service);
            }
            mIntentList.notify();
        }
        return null;
    }

    /**
     * Adds an external provider to resolve the specified authority
     * @param authority authority
     * @param provider provider
     */
    public void addProvider(String authority, ContentProvider provider) {
        mResolver.addProvider(authority, provider);
    }

    /**
     * @return a list of ContentValues containing check-in values
     */
    public List<ContentValues> getCheckinValues() {
        synchronized(mCheckinValues) {
            if (mCheckinValues.isEmpty()) {
                try {
                    mCheckinValues.wait(MAX_WAIT_TIME);
                } catch (InterruptedException e) {
                    Log.e(TAG, "getCheckinValues() interrupted.");
                }
            }
        }
        return Collections.unmodifiableList(mCheckinValues);
    }

    /**
     * @return the intent that was passed in for startService()
     */
    public Intent getStartServiceIntent() {
        Intent intent = null;
        synchronized(mIntentList) {
            if (mIntentList.isEmpty()) {
                try {
                    mIntentList.wait(MAX_WAIT_TIME);
                } catch (InterruptedException e) {
                    Log.e(TAG, "getStartServiceIntent() interrupted.");
                }
            }
        }
        if (!mIntentList.isEmpty()) {
            intent = mIntentList.remove(0);
        }
        return intent;
    }
}
