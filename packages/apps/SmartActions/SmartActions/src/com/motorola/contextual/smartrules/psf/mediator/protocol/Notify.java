/*
 * @(#)Notify.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/22  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator.protocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.mediator.MediatorConstants;
import com.motorola.contextual.smartrules.psf.mediator.MediatorHelper;
import com.motorola.contextual.smartrules.psf.table.MediatorTable;
import com.motorola.contextual.smartrules.psf.table.MediatorTuple;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

/**
 * This class handles "notify" command from publishers.
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements IMediatorProtocol 
 *
 * RESPONSIBILITIES:
 * Send the "notify" command intent to subscribed consumers
 *
 * COLABORATORS:
 *     Consumer - Uses the preconditions available across the system
 *     ConditionPublisher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class Notify implements IMediatorProtocol, MediatorConstants, DbSyntax {

    private static final String TAG = MEDIATOR_PREFIX + Notify.class.getSimpleName();

    private String mPublisher;
    private HashMap<String, String> mConfigStateMap;
    private List<String> mConsumers = new ArrayList<String>();

    public Notify(Intent intent) {
        mPublisher = intent.getStringExtra(EXTRA_PUBLISHER_KEY);
        mConfigStateMap = (HashMap<String, String>)intent.getSerializableExtra(EXTRA_CONFIG_STATE_MAP);
    }

    public boolean processRequest(Context context, Intent intent) {
        return (mPublisher != null &&
                mConfigStateMap != null && mConfigStateMap.size() > 0) ? true : false;
    }

    public List<Intent> execute(Context context, Intent intent) {
        List<Intent> intentsToBroadcast = new ArrayList<Intent>();
        ContentResolver contentResolver = context.getContentResolver();

        Iterator<Entry<String, String>> iterator = mConfigStateMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, String> entry = iterator.next();
            String config = entry.getKey();
            String state = entry.getValue();
            if (Constants.LOG_INFO) Log.i(TAG, "Config:" + config + ", state:" + state);

            String where = MediatorTable.Columns.CONFIG + EQUALS + Q + config + Q;
            Cursor cursor = null;

            try {
                cursor = contentResolver.query(MediatorTable.CONTENT_URI, null, where, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    updateMediatorDatabase(contentResolver, cursor, config, state);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        for (String consumer : mConsumers) {
            intentsToBroadcast.add(getFilteredIntentForConsumer(contentResolver, consumer)) ;
        }

        return intentsToBroadcast;
    }

    /**
     * Method to update the mediator database and
     * get the list of consumers subscribed to this config
     * @param contentResolver
     * @param cursor
     * @param consumers
     * @param config
     * @param state
     * @return
     */
    private void updateMediatorDatabase (ContentResolver contentResolver, Cursor cursor,
                                         String config, String state) {
        do {
            MediatorTuple mediatorTuple = new MediatorTuple(cursor);
            mediatorTuple.setState(state);
            String consumer = mediatorTuple.getConsumer();
            if (consumer != null) {
                if (!mConsumers.contains(consumer)) {
                    mConsumers.add(consumer);
                }
                MediatorHelper.updateTuple(contentResolver, mediatorTuple);
            }
        } while (cursor.moveToNext());
    }

    /**
     * Send notify for only those configs for which a consumer is subscribed.
     * This methods returns the notify intent to be broadcasted for a particular consumer
     * @param contentResolver
     * @param consumer
     * @return Notify intent
     */
    private Intent getFilteredIntentForConsumer (ContentResolver contentResolver, String consumer) {
        //Re-create config-state hashmap for a consumer
        //Send notify only for those configs which are subscribed by a consumer
        String where = MediatorTable.Columns.CONSUMER + EQUALS + Q + consumer + Q;
        String [] projection = {MediatorTable.Columns.CONFIG};
        Cursor cursor = null;
        HashMap<String, String> configStateMap = new HashMap<String, String>();

        try {
            cursor = contentResolver.query(MediatorTable.CONTENT_URI, projection, where, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String config = cursor.getString(cursor.getColumnIndex(MediatorTable.Columns.CONFIG));
                    if (mConfigStateMap.containsKey(config)) {
                        configStateMap.put(config, mConfigStateMap.get(config));
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return MediatorHelper.createNotifyIntent(consumer, mPublisher, configStateMap);
    }

}
