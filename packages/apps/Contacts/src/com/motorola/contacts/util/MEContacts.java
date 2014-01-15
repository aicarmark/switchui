/*
 * Copyright (C) 2011, Motorola, Inc,
 * All Rights Reserved
 * Class name: MEDialer.java
 * Description: Please see the class comment.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * 19-Apr-2011    a19562       Init ME log feature
 **********************************************************
 */
package com.motorola.contacts.util;

import android.util.Log;
import android.content.Context;
import android.content.Intent;
import com.android.contacts.R;

import com.motorola.contacts.util.MELogger;
import com.motorola.contacts.util.MELogger.MEValueBuilder;
import com.motorola.contacts.util.MEDialer;

/**
 * MEContacts will cook Contacts Events info. and pass formated stuff to MELogger.
 *
 * @author a19562
 */
public class MEContacts {
    private static final String LOG_TAG = "MEContacts";

    private static final String CHECKIN_TAG_L1 = "MOT_CONTACTS_STATS_L1";
    private static final String CHECKIN_TAG_L2 = "MOT_CONTACTS_STATS_L2";
    private static final String CHECKIN_TAG_L3 = "MOT_CONTACTS_STATS_L3";
    private static final String CHECKIN_TAG_L4 = "MOT_CONTACTS_STATS_L4";
    private static final String CHECKIN_TAG_L5 = "MOT_CONTACTS_STATS_L5";
    private static final String CHECKIN_TAG_L6 = "MOT_CONTACTS_STATS_L6";

    public static final String TAG_ID_CONTACT_ADD = "ID=CONTACT_CNT_ADD;";
    public static final String TAG_ID_CONTACT_DEL = "ID=CONTACT_CNT_DEL;";
    public static final String TAG_ID_CONTACT_MRG = "ID=CONTACT_CNT_MRG;";
    public static final String TAG_ID_GROUP_ASN = "ID=CONTACT_GRP_ASN;";
    public static final String TAG_ID_GROUP_DEA = "ID=CONTACT_GRP_DEA;";

    private static final String TAG_CONTACTID = "contact id";
    private static final String TAG_CONTACTID_FROM = "from";
    private static final String TAG_CONTACTID_TO = "to";
    private static final String TAG_GROUPID = "group id";
    private static final String TAG_GROUPTITLE = "group title";

    // static for MELogger initialization state
    public static MELogger mMELogger = null;
    public static boolean mLaunched = false;
    public static boolean fromFavorite = false;

    // some ME log is big. we need prepare big buffer to avoid reallocation
    private static int MOST_BIG_GROUP_LOG_SIZE = 512;

    public static boolean inited(Context context) {
        if (!mLaunched ) {
            mLaunched = true;
            if (context.getResources().getBoolean(R.bool.ftr_36344_meaningful_event_log) ) {
                mMELogger = MELogger.createSingleInstance(context);
            }
        }
        return (mMELogger != null);
    }

    public static void onContactOperation(final Context context, final String id, final long contactId) {
        log(" <- onContactOperation: " + id + ",contact id:" + contactId);
        if (inited(context)) {
            MELogger.MEValueBuilder valueBuilder = new MELogger.MEValueBuilder();
            valueBuilder.open()
                .appendRaw(id)
                .appendRaw(MELogger.TAG_VER_CURRENT)
                .append(MELogger.TAG_TIME, getCurrentTime())
                .append(TAG_CONTACTID, contactId)
                .close();

            MELogger.logEvent(CHECKIN_TAG_L2, valueBuilder.toString());
        } else {
            log(" onContactOperation:" + id);
        }
    }

    public static void onMergeContact(final Context context, final long contactIdFrom, final long contactIdTo) {
        log(" <- onMergeContact -> " + "Contact Id from is: " + contactIdFrom + "Contact ID To is: " +contactIdTo);
        if (inited(context)) {
            MELogger.MEValueBuilder valueBuilder = new MELogger.MEValueBuilder();
            valueBuilder.open()
                .appendRaw(TAG_ID_CONTACT_MRG)
                .appendRaw(MELogger.TAG_VER_CURRENT)
                .append(MELogger.TAG_TIME, getCurrentTime())
                .append(TAG_CONTACTID_FROM, contactIdFrom)
                .append(TAG_CONTACTID_TO, contactIdTo)
                .close();

            MELogger.logEvent(CHECKIN_TAG_L2, valueBuilder.toString());
        } else {
            log(" onMergeContact:");
        }
    }

    public static void onGroupMembership(final Context context, final String id,  final long groupId, final long[] contactIds) {
        log(" <- onGroupMembership: " + id + "group id is:" + groupId);
        if (inited(context)) {
            MELogger.MEValueBuilder valueBuilder = new MELogger.MEValueBuilder(MOST_BIG_GROUP_LOG_SIZE);
            valueBuilder.open()
                .appendRaw(id)
                .appendRaw(MELogger.TAG_VER_CURRENT)
                .append(MELogger.TAG_TIME, getCurrentTime())
                .append(TAG_GROUPID, groupId);
            for ( long contactId : contactIds) {
                valueBuilder.append(TAG_CONTACTID, contactId);
            }
            valueBuilder.close();

            MELogger.logEvent(CHECKIN_TAG_L2, valueBuilder.toString());
        } else {
            log(" onGroupMembership:" + id);
        }
    }

    public static void onGroupMembership(final Context context, final String id,  final String groupTitle, final long[] contactIds) {
        log(" <- onGroupMembership: " + id + "groupTitle is:" + groupTitle);
        if (inited(context)) {
            MELogger.MEValueBuilder valueBuilder = new MELogger.MEValueBuilder(MOST_BIG_GROUP_LOG_SIZE);
            valueBuilder.open()
                .appendRaw(id)
                .appendRaw(MELogger.TAG_VER_CURRENT)
                .append(MELogger.TAG_TIME, getCurrentTime())
                .append(TAG_GROUPTITLE, groupTitle);
            for ( long contactId : contactIds) {
                valueBuilder.append(TAG_CONTACTID, contactId);
            }
            valueBuilder.close();

            MELogger.logEvent(CHECKIN_TAG_L2, valueBuilder.toString());
        } else {
            log(" onGroupMembership:" + id);
        }
    }


    // intent: will be modified to include dialer information.
    public static void onDial(final Context context, Intent intent, final MEDialer.DialFrom dialFrom) {
        if (inited(context)) {
            MEDialer.onDial(context, intent, dialFrom);
        }
    }


    // print log with local tag
    private static final void log(final String text) {
        Log.d(LOG_TAG, text);
    }

    private static long getCurrentTime() {
        return System.currentTimeMillis();
    }
}

