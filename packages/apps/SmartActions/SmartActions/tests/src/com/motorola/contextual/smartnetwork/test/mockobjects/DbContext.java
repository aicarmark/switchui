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

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.test.mock.MockContext;

public class DbContext extends MockContext {

    // isolated test db file generation and clean-up related
    private static final String FILE_PREFIX = "test_";
    private static final String JOURNAL_SUFFIX = "-journal";

    private Context mBaseContext;
    // created db list
    private final List<String> mDbList = new LinkedList<String>();

    public DbContext(Context context) {
        mBaseContext = context;
    }

    public void cleanup() {
        // delete all database files created by this test context
        for (String db : mDbList) {
            mBaseContext.deleteDatabase(db);
            mBaseContext.deleteDatabase(db + JOURNAL_SUFFIX);
        }
    }

    @Override
    public SQLiteDatabase openOrCreateDatabase(String file, int mode, CursorFactory factory,
            DatabaseErrorHandler errorHandler) {
        // isolate test database
        file = FILE_PREFIX + file;
        mDbList.add(file);
        return mBaseContext.openOrCreateDatabase(file, mode, factory, errorHandler);
    }

}
