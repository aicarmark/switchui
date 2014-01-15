/*
 * @(#)MediatorHelper.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/21  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator;

import java.util.HashMap;
import java.util.List;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;
import com.motorola.contextual.smartrules.psf.table.MediatorTable;
import com.motorola.contextual.smartrules.psf.table.MediatorTuple;

/**
 * Helper class for mediator
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Implements MediatorConstants, DbSyntax
 *
 * RESPONSIBILITIES:
 * Contains utility methods to be used by the mediator
 *
 * COLABORATORS:
 *     Mediator - Uses the constants present in this interface
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class MediatorHelper implements MediatorConstants, DbSyntax {

    private final static String ACTION_GET_CONFIG = "com.motorola.smartactions.intent.action.GET_CONFIG";
    private final static String CATEGORY_CP = "com.motorola.smartactions.intent.category.CONDITION_PUBLISHER";
    private final static String MOTION_PACKAGE = "com.motorola.contextual.Motion";
    private final static String MDM_PACKAGE = "com.motorola.mdmmovenotice";
    private final static String MDM_SERVICE = "com.motorola.mdmmovenotice.MdmMoveNotice";
    private final static String MOTION_PUBLISHER_KEY = "com.motorola.contextual.Motion";
    private static final String TAG = MediatorHelper.class.getSimpleName();

    /**
     * Create a subscribe/cancel/refresh/list intent to be sent to the desired publisher
     * @param publisher Requested condition publisher
     * @param config Requested config if applicable
     * @param requestId Request id of the command
     * @param command Subscribe/cancel/refresh/list
     *
     * @return Broadcast intent to be sent to the publisher
     */
    public static Intent createCommandIntent (String publisher, String config, String requestId,
            String command) {
        Intent intent = new Intent(publisher);
        if (!command.equals(LIST_EVENT)) intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        intent.putExtra(EXTRA_EVENT, command);

        return intent;
    }

    /**
     * Create a subscribe/cancel/refresh response intent to be sent to the desired consumer
     * @param consumer Consumer expecting a command response
     * @param config Requested config
     * @param responseId Response id of the command response
     * @param eventType Subscribe/cancel/refresh response
     * @param publisher Requested condition publisher
     * @param status Success or failure
     * @param state Current state of the publisher (if applicable)
     * @param description Description of the config (if applicable)
     *
     * @return Broadcast intent to be sent to the consumer
     */
    public static Intent createCommandResponseIntent (String consumer, String config, String responseId,
            String eventType, String publisher, String status, String state, String description) {
        Intent intent = new Intent(consumer);
        intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_RESPONSE_ID, responseId);
        intent.putExtra(EXTRA_EVENT, eventType);
        intent.putExtra(EXTRA_PUBLISHER_KEY, publisher);
        intent.putExtra(EXTRA_STATUS, status);
        if (!eventType.equals(CANCEL_RESPONSE_EVENT)) {
            intent.putExtra(EXTRA_STATE, state);
        }
        if (eventType.equals(REFRESH_RESPONSE_EVENT)) {
            intent.putExtra(EXTRA_DESCRIPTION, description);
        }

        return intent;
    }

    /**
     * Create a list response intent to be sent to the desired consumer
     * @param consumer Consumer expecting a list response
     * @param responseId Response id of the list response
     * @param publisher Requested condition publisher
     * @param configItems XML containing list of config items supported by the publisher
     * @param newStateTitle Title of the button that launches config activity in new state mode.
     * This is optional.
     * @return Broadcast intent to be sent to the consumer
     */
    public static Intent createListResponseIntent (String consumer, String responseId,
            String publisher, String configItems, String newStateTitle) {
        Intent intent = new Intent(consumer);
        intent.putExtra(EXTRA_EVENT, LIST_RESPONSE_EVENT);
        intent.putExtra(EXTRA_RESPONSE_ID, responseId);
        intent.putExtra(EXTRA_PUBLISHER_KEY, publisher);
        intent.putExtra(EXTRA_CONFIG_ITEMS, configItems);
        intent.putExtra(EXTRA_NEW_STATE_TITLE, newStateTitle);
        return intent;
    }

    /**
     * Create a "notify" intent to be sent to the desired consumer
     * @param consumer Consumer subscribed for this config and publisher
     * @param publisher Publisher sending notify intent
     * @param configStateMap Hashmap having configs and states
     *
     * @return Broadcast intent to be sent to the consumer
     */
    public static Intent createNotifyIntent (String consumer, String publisher, HashMap<String, String> configStateMap) {
        Intent intent = new Intent(consumer);
        intent.putExtra(EXTRA_EVENT, NOTIFY_EVENT);
        intent.putExtra(EXTRA_PUBLISHER_KEY, publisher);
        intent.putExtra(EXTRA_CONFIG_STATE_MAP, configStateMap);
        return intent;
    }

    /**
     * Method to create a subscribe/cancel request intent
     * Create an intent which is expected from a consumer
     *
     * @param config
     * @param publisher
     * @param consumer
     * @return Broadcast intent to be sent
     */
    public static Intent createCommandRequestIntent(String command, String config,
            String publisher, String consumer, String consumerPackage) {
        Intent cancelRequestIntent = new Intent(REQUEST);
        cancelRequestIntent.putExtra(EXTRA_CONFIG, config);
        cancelRequestIntent.putExtra(EXTRA_EVENT, command);
        cancelRequestIntent.putExtra(EXTRA_REQUEST_ID, config+MEDIATOR_SEPARATOR+publisher);
        cancelRequestIntent.putExtra(EXTRA_PUBLISHER_KEY, publisher);
        cancelRequestIntent.putExtra(EXTRA_CONSUMER, consumer);
        if (consumerPackage != null) cancelRequestIntent.putExtra(EXTRA_CONSUMER_PACKAGE, consumerPackage);
        return cancelRequestIntent;
    }

    /**
     * Create an "initiate refresh" intent to be sent to the desired consumer
     * @param consumer Consumer subscribed for this config and publisher
     * @param publisher Publisher sending notify intent
     *
     * @return Broadcast intent to be sent to the consumer
     */
    public static Intent createInitRefreshIntent (String consumer, String publisher) {
        Intent intent = new Intent(consumer);
        intent.putExtra(EXTRA_EVENT, INITIATE_REFRESH);
        intent.putExtra(EXTRA_PUBLISHER_KEY, publisher);
        return intent;
    }

    /**
     * Method to delete an entry from the mediator database.
     *
     * @param contentResolver
     * @param consumer
     * @param publisher
     * @param config
     */
    public static void deleteFromMediatorDatabase(ContentResolver contentResolver, String consumer, String publisher,
            String config) {
        String where = MediatorTable.Columns.CONSUMER + EQUALS + Q + consumer + Q + AND +
                       MediatorTable.Columns.CONFIG + EQUALS + Q + config + Q + AND +
                       MediatorTable.Columns.PUBLISHER + EQUALS + Q + publisher + Q;

        contentResolver.delete(MediatorTable.CONTENT_URI, where, null);
    }

    /**
     * Method to update the cancel request id in the mediator database
     *
     * @param contentResolver
     * @param mediatorTuple Tuple to be updated with new entries.
     * Consumer, publisher and config remain the same.
     */
    public static void updateTuple(ContentResolver contentResolver, MediatorTuple mediatorTuple) {
        String where = MediatorTable.Columns._ID + EQUALS + Q + mediatorTuple.getId() + Q;
        contentResolver.update(MediatorTable.CONTENT_URI, mediatorTuple.getAsContentValues(), where, null);
    }

    /**
     * Method to get the package of a publisher
     * This method queries LocalPublisherTable to get the publisher package
     * @param contentResolver
     * @param publisherKey
     * @return Publisher package
     */
    public static String getPublisherPackage(ContentResolver contentResolver, String publisherKey) {
        Cursor cursor = null;
        String where = LocalPublisherTable.Columns.PUBLISHER_KEY + EQUALS + Q + publisherKey + Q;
        String[] projection = {LocalPublisherTable.Columns.PACKAGE};

        try {
            cursor = contentResolver.query(LocalPublisherTable.CONTENT_URI,
                                           projection, where, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.PACKAGE));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    /**
     *
     * @param path  -  Path to db
     * @return      -  true if the dbase is openable
     */
    public static boolean isDbPresent(String path) {
        boolean isPresent = false;
        SQLiteDatabase db = null;

        try {
        	// cjd - why not like this instead????
        	// java.io.File file = new java.io.File(path);
        	// isPresent = file.exists();
        	
        	// this causes stack trace when db file is missing
            db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
            db.close();
            isPresent = true;
        } catch (Exception e) {
            Log.e(TAG, "Mediator DB absent");
        }

        return isPresent;
    }

    /**
     *
     * @param intent - Outgoing intent towards condition publishers
     * @return - true if its intended to Motion condition publisher
     */
    public static boolean isRequestForMotion(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(MOTION_PUBLISHER_KEY)) {
            return true;
        }

        return false;
    }

    /**
     * Method to check if the command needs to be converted to the old motion detector interface.
     * If the MDM service is not present it need not be converted
     * @param context
     * @return true if incoming command needs to be converted to new format, false otherwise
     */
    public static boolean isMDMPresent(Context context) {

        PackageManager pm = context.getPackageManager();
        try {
            pm.getServiceInfo(new ComponentName(MDM_PACKAGE, MDM_SERVICE), PackageManager.GET_META_DATA);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Method to check if Motion Detector activity has an Activity which is defined with
     * New arch category
     * @param context
     * @return true / false
     */
    public static boolean isNewArchMD(Context context) {
        PackageManager pm = context.getPackageManager();

        Intent newArchIntent = new Intent(ACTION_GET_CONFIG);
        newArchIntent.addCategory(CATEGORY_CP);
        List<ResolveInfo> list = pm.queryIntentActivities(newArchIntent, PackageManager.GET_META_DATA);

        boolean status = false;

        int size = list.size();
        if((list != null) && (size != 0)) {
            for(int i = 0; i < size; i++) {
                if(list.get(i).activityInfo.packageName.equals(MOTION_PACKAGE)) {
                    status = true;
                    break;
                }
            }
        }

        return status;
    }

    /**
     *
     * @param context - Context to work with
     * @return - value stored in persistence under key mediator_init_complete
     */
    public static boolean isMediatorInitialized(Context context) {
        return(SharedPrefWrapper.retrieveBooleanValue(context, MEDIATOR_INITIALIZATION_COMPLETE));
    }

    /**
     * Marks initialization complete in the persistence
     * @param context - Context to work with
     */
    public static void setMediatorInitialized(Context context) {
        SharedPrefWrapper.commitBooleanValue(context, MEDIATOR_INITIALIZATION_COMPLETE, true);
    }

}
