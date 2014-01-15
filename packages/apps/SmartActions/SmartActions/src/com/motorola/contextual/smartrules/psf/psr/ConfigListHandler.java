/*
 * @(#)ConfigListHandler.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/21                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.psr;

import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.motorola.contextual.smartrules.psf.IntentHandler;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherConfigTable;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherConfigTuple;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;

/**
* Handles the intent carrying response for command=list, PSR=Publisher States Receiver
* CLASS:
*     ConfigListHandler extends IntentHandler
*
* RESPONSIBILITIES:
*  Handles response for command=list
*  Ensures the PP (Publisher Provider) database is up to date with the latest list of states
*  Deletes the current list and stores fresh list when there is an update
*  This class does not selectively update a config
*
* USAGE:
*     See each method.
*
*/
public class ConfigListHandler extends IntentHandler {

    /**
     * Constructor
     * @param context - Context to work with
     * @param intent  - Intent to work on
     */
    public ConfigListHandler(Context context, Intent intent) {
        super(context, intent);
    }

    @Override
    public boolean handleIntent() {

        String eventType = getIntent().getStringExtra(EXTRA_EVENT_TYPE);

        if (eventType!= null && eventType.equals(LIST_RESPONSE)) {
            String newStateLabel = getIntent().getStringExtra(EXTRA_NEW_STATE_TITLE);
            String pubKey = getIntent().getStringExtra(EXTRA_PUBLISHER_KEY);
            String xml = getIntent().getStringExtra(EXTRA_CONFIG_ITEMS);

            ConfigList list = new ConfigList(xml);

            List<ConfigItem> configItems = list.getListOfItems();

            updateNewStateLabel(pubKey, newStateLabel);
            deleteStates(pubKey);

            for (int i = 0; i < configItems.size(); i++) {
                persistConfig(pubKey, configItems.get(i));
            }
        } else {
            if (LOG_WARN) Log.w(TAG, "Ignoring action publisher event " + eventType);
        }

        return true;
    }

    /**
     * Updates the new config label for the given publisher key
     * @param pubKey         - Publisher key
     * @param newStateLabel  - New config label
     */
    private void updateNewStateLabel(String pubKey, String newStateLabel) {
        ContentResolver cResolver = mContext.getContentResolver();

        String whereClause = LocalPublisherTable.Columns.PUBLISHER_KEY
                             + EQUALS
                             + Q + pubKey + Q;

        ContentValues values = new ContentValues();

        values.put(LocalPublisherTable.Columns.NEW_CONFIG_LABEL, newStateLabel);

        int count = 0;

        try {
            count = cResolver.update(LocalPublisherTable.CONTENT_URI, values, whereClause, null);
        } catch (Exception e) {
            Log.e(TAG, "Exception while update - updateNewStateLabel " + pubKey + Q + newStateLabel);
        }

        if (LOG_DEBUG) Log.d(TAG, "New label updated " + count);
    }

    /**
     * Deletes all the list of configs associated with a publisher key
     * @param pubKey - Publisher key for which the list of configs has to be deleted
     */
    private void deleteStates(String pubKey) {
        ContentResolver cResolver = mContext.getContentResolver();
        String whereClause = LocalPublisherConfigTable.Columns.PUBLISHER_KEY
                             + EQUALS
                             + Q + pubKey + Q;

        cResolver.delete(LocalPublisherConfigTable.CONTENT_URI, whereClause, null);
    }

    /**
     * Persists the given config for the given publisher key, i.e.,
     * writes the information into the database
     * @param pubKey - publisher key for whom the config has to be persisted
     * @param item   - config item
     */
    private void persistConfig(String pubKey, ConfigItem item) {
        ContentResolver cResolver = mContext.getContentResolver();
        @SuppressWarnings("unused")
        String whereClause = LocalPublisherConfigTable.Columns.PUBLISHER_KEY
                             + EQUALS
                             + Q + pubKey + Q;

        LocalPublisherConfigTuple tuple = new LocalPublisherConfigTuple(pubKey,
                item.getConfig(), "", item.getDescription());
        cResolver.insert(LocalPublisherConfigTable.CONTENT_URI, tuple.getAsContentValues());
    }


}
