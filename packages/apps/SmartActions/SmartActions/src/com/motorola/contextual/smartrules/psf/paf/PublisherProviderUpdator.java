/*
 * @(#)PublisherProviderUpdator.java
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

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTuple;

/** Class provides utility functions to insert, update and delete
 * {@link LocalPublisherTuple} from PublisherProvider
 *<code><pre>
 * CLASS:
 *     PublisherProviderUpdator
 * Interface:
 * 		DbSyntax
 *
 * RESPONSIBILITIES:
 * 		Provides methods to insert, update and delete {@link LocalPublisherTuple} from PublisherProvider
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PublisherProviderUpdator implements DbSyntax {

    public static final String TAG = PublisherProviderUpdator.class.getSimpleName();
    private Context mContext;

    /**
     * Constructor
     * @param context
     */
    PublisherProviderUpdator(Context context) {
        mContext = context;
    }

    /**
     * Inserts the given {@link LocalPublisherTuple} to PublisherProvider
     * @param tuple {@link LocalPublisherTuple} to insert
     * @return Uri of the inserted tuple
     */
    public Uri insertPublisher(LocalPublisherTuple tuple) {
        ContentResolver cr = mContext.getContentResolver();
        return cr.insert(LocalPublisherTable.CONTENT_URI, tuple.getAsContentValues());
    }

    /**
     * Updates the given {@link LocalPublisherTuple} to PublisherProvider
     * @param tuple {@link LocalPublisherTuple} to update
     * @return Count of updated rows
     */
    public int updatePublisher(LocalPublisherTuple tuple) {
        ContentResolver cr = mContext.getContentResolver();
        String whereClause = LocalPublisherTable.Columns.PUBLISHER_KEY +
                             EQUALS +
                             Q + tuple.getPublisherKey() + Q;
        return cr.update(LocalPublisherTable.CONTENT_URI, tuple.getAsContentValues(), whereClause, null);
    }

    /**
     * Deletes the given PublisherKey from PublisherProvider
     * @param publisherKey PublisherKey to delete
     * @return Count of deleted rows
     */
    public int deletePublisher(String publisherKey) {
        ContentResolver cr = mContext.getContentResolver();
        String whereClause = LocalPublisherTable.Columns.PUBLISHER_KEY +
                             EQUALS +
                             Q + publisherKey + Q;
        return cr.delete(LocalPublisherTable.CONTENT_URI, whereClause, null);
    }
}
