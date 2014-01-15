/*
 * @(#)PublisherProviderList.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/03/26                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.paf;

import java.util.ArrayList;

import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTuple;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

/** Provides List of LocalPublisherTuple populated from querying PublisherProvider
 *<code><pre>
 * CLASS:
 *     PublisherProviderList Extends PublisherList
 * Interface:
 * 		PafConstants
 *
 * RESPONSIBILITIES:
 * 		Provides List of LocalPublisherTuple for all the publishers in PublisherProvider or
 *      for the given package from PublisherProvider
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PublisherProviderList extends PublisherList {

    private static final long serialVersionUID = -9126020185689872439L;
    private static final String TAG = PublisherProviderList.class.getSimpleName();

    /**
     * Constructor to Provides List of LocalPublisherTuple for all the publishers in PublisherProvider
     * @param context Context of the caller
     */
    public PublisherProviderList(Context context) {
        super(context);
    }

    /**
     * Constructor to Provides List of LocalPublisherTuple for the given package from PublisherProvider
     * @param context Context of the caller
     * @param packageName Name of the package
     */
    public PublisherProviderList(Context context, String packageName) {
        super(context, packageName);
    }

    /*
     * Queries the entries in PublisherProvider for the given where clause
     * and populate List of {@link LocalPublisherTuple}
     */
    private void populatePublisherListFor(String where) {
        Cursor cursor = null;
        mBlackList = new ArrayList<String>();
        ContentResolver cr = mContext.getContentResolver();
        try {
            cursor = cr.query(LocalPublisherTable.CONTENT_URI, null, where, null, null);
            if(cursor != null && cursor.moveToFirst()) {
                do {
                    LocalPublisherTuple publisher = new LocalPublisherTuple(cursor);
                    this.put(publisher.getPublisherKey(), publisher);
                    if(publisher.isBlackListed() == LocalPublisherTable.BlackList.TRUE) {
                        mBlackList.add(publisher.getPublisherKey());
                    }
                } while (cursor.moveToNext());
            }
        } catch(Exception e) {
            Log.e(TAG, "Error in Query " + e.getMessage());
            e.printStackTrace();
        } finally {
            if(cursor != null) cursor.close();
        }
    }

    /**
     * Queries all the entries in PublisherProvider and populate List of {@link LocalPublisherTuple}
     */
    @Override
    protected void populatePublisherList() {
        if(LOG_DEBUG) Log.d(TAG,"populatePublisherList called");
        populatePublisherListFor(null);
    }

    /**
     * Queries PublisherProvider for package and populate List of {@link LocalPublisherTuple}
     */
    @Override
    protected void populatePublisherListForPackage() {
        if(LOG_DEBUG) Log.d(TAG,"populatePublisherListForPackage called packageName : " + mPackageName);
        String where = LocalPublisherTable.Columns.PACKAGE + EQUALS + Q + mPackageName + Q;
        populatePublisherListFor(where);
    }
}
