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

import android.content.ContentResolver;
import android.content.Context;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

public class TableContext extends MockContext {
    /* base context */
    private final Context mBaseContext;

    private final MockContentResolver mMockResolver = new MockContentResolver();

    public TableContext(Context context) {
        mBaseContext = context;
    }

    /**
     * Sets a mock provider to resolve the given authority
     * @param authority authority to resolve
     * @param provider the mock provider that should resolve the authority
     */
    public void addProvider(String authority, MockContentProvider provider) {
        mMockResolver.addProvider(authority, provider);
    }

    @Override
    public ContentResolver getContentResolver() {
        // return the MockContentResolver
        return mMockResolver;
    }

}
