/*
 * @(#)PackageManagerPublisherList.java
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import com.motorola.contextual.smartrules.psf.table.LocalPublisherTable;
import com.motorola.contextual.smartrules.psf.table.LocalPublisherTuple;

/** Provides List of LocalPublisherTuple populated from querying package manager
 *<code><pre>
 * CLASS:
 *     PackageManagerPublisherList Extends PublisherList
 * Interface:
 *      PafConstants
 *
 * RESPONSIBILITIES:
 *      Provides List of LocalPublisherTuple for all the packages in the white list or
 *      for the given package
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PackageManagerPublisherList extends PublisherList implements PafConstants {

    private static final long serialVersionUID = -1738121185551596007L;

    static final String TAG = PackageManagerPublisherList.class.getSimpleName();

    private PublisherFilterList mWhiteList = null;
    private static final HashMap<String, String> mInternalActionPublishers = new HashMap<String, String>() {
        private static final long serialVersionUID = 6908427472445794851L;

        {
            put ("com.motorola.contextual.actions.Sync", LocalPublisherTable.PkgMgr.Type.STATEFUL);
        }
    };

    /**
     * Constructor to Provides List of LocalPublisherTuple for all the packages in the white list
     * @param context Context of the caller
     */
    public PackageManagerPublisherList(Context context) {
        super(context);
    }

    /**
     * Constructor to Provides List of LocalPublisherTuple for the given package
     * @param context Context of the caller
     * @param packageName Name of the package
     */
    public PackageManagerPublisherList(Context context, String packageName) {
        super(context, packageName);
    }

    @Override
    protected void populatePublisherList() {
        mBlackList = new PublisherFilterList(PafConstants.BLACKLIST_DEFAULT_XML_FILE_PATH);
        mWhiteList = new PublisherFilterList(PafConstants.WHITELIST_DEFAULT_XML_FILE_PATH);
        populateConditionPublishers();
        populateActionPublishers();
        populateRulePublishers();
        if(LOG_DEBUG) Log.d(TAG,"populatePublisherList : " + this.toString());
    }

    /**
     * populates the list of {@link ResolveInfo} from querying package manager for given
     * Intent action and Category
     */
    private List<ResolveInfo> getPublisherMetaData(String intentAction, String intentCategory) {
        List<ResolveInfo> list = null;
        Intent mainIntent = new Intent(intentAction);
        mainIntent.addCategory(intentCategory);
        PackageManager pm = mContext.getPackageManager();
        if(!intentCategory.equals(RULE_PUBLISHER_CATEGORY)) {
            list = pm.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA);
        } else {
            list = pm.queryBroadcastReceivers(mainIntent, PackageManager.GET_META_DATA);
        }
        if(LOG_DEBUG) Log.d(TAG, "list is "+list.toString());
        return list;
    }

    /**
     * populates the list of {@link LocalPublisherTuple} from querying package manager for
     * Rule publishers
     */
    private void populateRulePublishers() {
        List<ResolveInfo> list = getPublisherMetaData(RULE_PUBLISHER_ACTION, RULE_PUBLISHER_CATEGORY);
        PackageManager pm = mContext.getPackageManager();

        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            Bundle metaData = info.activityInfo.metaData;
            if(!mWhiteList.contains(info.activityInfo.packageName) || metaData == null) continue;
            CharSequence labelSeq = info.loadLabel(pm);
            String publisherKey = metaData.getString(RULE_PUBLISHER_META_DATA_PUBKEY);

            int blackList = (!mBlackList.contains(publisherKey)) ?
                            LocalPublisherTable.BlackList.FALSE : LocalPublisherTable.BlackList.TRUE;

            LocalPublisherTuple publisherTuple = new LocalPublisherTuple(
                publisherKey, info.activityInfo.packageName,
                ((labelSeq != null) ? labelSeq.toString() : info.activityInfo.name),
                info.activityInfo.name,
                Boolean.toString(metaData.getBoolean(RULE_PUBLISHER_META_DATA_NEW_STATE)),
                null, LocalPublisherTable.Type.RULE,
                metaData.getString(RULE_PUBLISHER_META_DATA_DOWNLOAD_LINK),
                LocalPublisherTable.PkgMgr.Type.STATEFUL,
                metaData.getString(RULE_PUBLISHER_META_DATA_BATTERY_DRAIN),
                metaData.getString(RULE_PUBLISHER_META_DATA_DATA_USAGE),
                metaData.getString(RULE_PUBLISHER_META_DATA_RESPONSE_LATENCY),
                null,
                LocalPublisherTable.Share.FALSE, blackList, info.loadIcon(pm),
                metaData.getFloat(RULE_PUBLISHER_META_DATA_INTERFACE_VERSION),
                metaData.getFloat(RULE_PUBLISHER_META_DATA_PUBLISHER_VERSION));

            this.put(publisherKey, publisherTuple);
        }
    }

    /**
     * populates the list of {@link LocalPublisherTuple} from querying package manager for
     * Condition publishers
     */
    private void populateConditionPublishers() {
        List<ResolveInfo> list = getPublisherMetaData(CONDITION_PUBLISHER_ACTION, CONDITION_PUBLISHER_CATEGORY);
        PackageManager pm = mContext.getPackageManager();

        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            Bundle metaData = info.activityInfo.metaData;
            if(!mWhiteList.contains(info.activityInfo.packageName) || metaData == null) continue;
            CharSequence labelSeq = info.loadLabel(pm);
            String publisherKey = metaData.getString(CONDITION_PUBLISHER_META_DATA_PUBKEY);

            int blackList = (!mBlackList.contains(publisherKey)) ?
                            LocalPublisherTable.BlackList.FALSE : LocalPublisherTable.BlackList.TRUE;

            LocalPublisherTuple publisherTuple = new LocalPublisherTuple(
                publisherKey, info.activityInfo.packageName,
                ((labelSeq != null) ? labelSeq.toString() : info.activityInfo.name),
                info.activityInfo.name,
                Boolean.toString(metaData.getBoolean(CONDITION_PUBLISHER_META_DATA_NEW_STATE)),
                null, LocalPublisherTable.Type.CONDITION,
                metaData.getString(CONDITION_PUBLISHER_META_DATA_DOWNLOAD_LINK),
                LocalPublisherTable.PkgMgr.Type.STATEFUL,
                metaData.getString(CONDITION_PUBLISHER_META_DATA_BATTERY_DRAIN),
                metaData.getString(CONDITION_PUBLISHER_META_DATA_DATA_USAGE),
                metaData.getString(CONDITION_PUBLISHER_META_DATA_RESPONSE_LATENCY),
                null,
                LocalPublisherTable.Share.FALSE, blackList, info.loadIcon(pm),
                metaData.getFloat(CONDITION_PUBLISHER_META_DATA_INTERFACE_VERSION),
                metaData.getFloat(CONDITION_PUBLISHER_META_DATA_PUBLISHER_VERSION));

            this.put(publisherKey, publisherTuple);
        }
    }

    /**
     * populates the list of {@link LocalPublisherTuple} from querying package manager for
     * Action publishers
     */
    private void populateActionPublishers() {
        List<ResolveInfo> list = getPublisherMetaData(ACTION_PUBLISHER_ACTION, ACTION_PUBLISHER_CATEGORY);
        PackageManager pm = mContext.getPackageManager();

        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            Bundle metaData = info.activityInfo.metaData;
            if(!mWhiteList.contains(info.activityInfo.packageName) || metaData == null) continue;
            CharSequence labelSeq = info.loadLabel(pm);
            String publisherKey = metaData.getString(ACTION_PUBLISHER_META_DATA_PUBKEY);
            String stateType = metaData.getString(ACTION_PUBLISHER_META_DATA_ACTION_TYPE);

            int blackList = (!mBlackList.contains(publisherKey)) ?
                            LocalPublisherTable.BlackList.FALSE : LocalPublisherTable.BlackList.TRUE;

            LocalPublisherTuple publisherTuple = new LocalPublisherTuple(
                publisherKey, info.activityInfo.packageName,
                ((labelSeq != null) ? labelSeq.toString() : info.activityInfo.name),
                info.activityInfo.name,
                Boolean.toString(metaData.getBoolean(ACTION_PUBLISHER_META_DATA_NEW_STATE)),
                null, LocalPublisherTable.Type.ACTION,
                metaData.getString(ACTION_PUBLISHER_META_DATA_DOWNLOAD_LINK),
                (stateType == null || stateType.length() == 0) ?
                LocalPublisherTable.PkgMgr.Type.STATEFUL:
                stateType,
                metaData.getString(ACTION_PUBLISHER_META_DATA_BATTERY_DRAIN),
                metaData.getString(ACTION_PUBLISHER_META_DATA_DATA_USAGE),
                metaData.getString(ACTION_PUBLISHER_META_DATA_RESPONSE_LATENCY),
                metaData.getString(ACTION_PUBLISHER_META_DATA_SETTINGS_ACTION),
                LocalPublisherTable.Share.FALSE, blackList, info.loadIcon(pm),
                metaData.getFloat(ACTION_PUBLISHER_META_DATA_INTERFACE_VERSION),
                metaData.getFloat(ACTION_PUBLISHER_META_DATA_PUBLISHER_VERSION));

            this.put(publisherKey, publisherTuple);
        }
        //Populate internal invisible action publishers
        Iterator<Entry<String, String>> iter = mInternalActionPublishers.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<String, String> entry = iter.next();
            LocalPublisherTuple publisherTuple = new LocalPublisherTuple(
                entry.getKey(),
                "com.motorola.contextual.actions",
                "Internal Action Publisher",
                null,
                null,
                null,
                LocalPublisherTable.Type.ACTION,
                null,
                entry.getValue(),
                "low",
                "low",
                "low",
                "android.settings.SETTINGS",
                LocalPublisherTable.Share.FALSE,
                LocalPublisherTable.BlackList.FALSE,
                null,
                1,
                1);
            this.put(entry.getKey(), publisherTuple);
        }

    }

    @Override
    protected void populatePublisherListForPackage() {
        mBlackList = new PublisherFilterList(PafConstants.BLACKLIST_DEFAULT_XML_FILE_PATH);
        mWhiteList = new PublisherFilterList();
        mWhiteList.add(mPackageName);
        populateConditionPublishers();
        populateActionPublishers();
        populateRulePublishers();
        if(LOG_DEBUG) Log.d(TAG,"populatePublisherList : " + this.toString());
    }

}
