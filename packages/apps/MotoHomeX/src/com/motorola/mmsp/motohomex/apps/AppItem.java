package com.motorola.mmsp.motohomex.apps;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.motorola.mmsp.motohomex.apps.AppsSchema.Apps;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Members;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Handler;

/**
 * Data stored in the apps database for each app on the device.
 */
public class AppItem {

    private static final String sWhereId = "_id=?";
    private static final String sWhereGroup = Members.GROUP + "=?";
    private static final String sWhereApp = Members.APP + "=?";
    private static final String sWhereGroupApp = sWhereGroup + " AND " + sWhereApp;
    private static final String [] sTempArg1 = new String[1];
    public static final long INVALID_ID = -1L;

    /** DB index of this app. */
    private long mId;
    /** Component of this app. */
    private ComponentName mComponent;
    /** Number of times this app has been used. */
    private int mCount;
    /** Last time this app was used. */
    private long mTime;
    /** Whether this app was downloaded. */
    private boolean mDownloaded;
    /** Set of groups to which this app belongs. */
    private final HashSet<Long> mGroupIds;
    /** Tell if the application is in a folder */
    private long mFolderId = INVALID_ID;

    /** Temporary index used when sorting list of apps. */
    public int index;

    /** Constructor called when loading all settings from database. */
    AppItem(long id, ComponentName component, int count, long time, boolean downloaded,
            HashSet<Long> groupIds, long folderId) {
        mId = id;
        mComponent = component;
        mCount = count;
        mTime = time;
        mFolderId = folderId;
        mDownloaded = downloaded;
        if (groupIds != null) {
            mGroupIds = groupIds;
        } else {
            mGroupIds = new HashSet<Long>();
        }
    }

    /** Constructor called when a new app is being added. */
    public AppItem(ComponentName component, boolean downloaded, long folderId) {
        mId = -1;
        mComponent = component;
        mCount = 0;
        mTime = 0L;
        mDownloaded = downloaded;
        mGroupIds = new HashSet<Long>();
        mFolderId = folderId;
    }

    public long getId() {
        return mId;
    }

    public int getCount() {
        return mCount;
    }

    public long getTime() {
        return mTime;
    }

    public void markAsUsed() {
        mTime = System.currentTimeMillis();
        ++mCount;
    }

    public boolean isDownloaded() {
        return mDownloaded;
    }

    public void setIsDownloaded(boolean isDownloaded) {
        mDownloaded = isDownloaded;
    }

    public HashSet<Long> getGroupIds() {
        return mGroupIds;
    }

    /**
     * Return true if the application is in a folder, otherwise false
     *
     * @return  true if the application is in a folder, otherwise false
     */
    public boolean isInFolder() {
        return mFolderId != INVALID_ID;
    }

    public void addToGroup(final ContentResolver cr, final long groupId,
            Handler handler) {
        mGroupIds.add(groupId);

        //modified by amt_wangpeipei 2012/07/23 for switchui-2410 begin
        handler.post(new Runnable() {
            public void run() {
                if (mId == -1) {
                    save(cr);
                }
                ContentValues values = new ContentValues();
                values.put(Members.GROUP, groupId);
                values.put(Members.APP, mId);
                cr.insert(Members.CONTENT_URI, values);
            }
        });
        //modified by amt_wangpeipei 2012/07/23 for switchui-2410 end.
    }

    public boolean removeFromGroup(final ContentResolver cr, long groupId,
            Handler handler) {
        if (!mGroupIds.remove(groupId)) {
            return false;
        }

        final String [] strs = new String[2];
        strs[0] = Long.toString(groupId);
        strs[1] = Long.toString(mId);
        handler.post(new Runnable() {
            public void run() {
                cr.delete(Members.CONTENT_URI, sWhereGroupApp, strs);
            }
        });
        return true;
    }

    public boolean isMemberOf(long groupId) {
        return mGroupIds.contains(groupId);
    }

    public long save(ContentResolver cr) {
        ContentValues values = new ContentValues();
        values.put(Apps.COMPONENT, mComponent.flattenToString());
        values.put(Apps.COUNT, mCount);
        values.put(Apps.TIME, mTime);
        values.put(Apps.DOWNLOADED, mDownloaded);
        if (mId >= 0) {
            sTempArg1[0] = Long.toString(mId);
            cr.update(Apps.CONTENT_URI, values, sWhereId, sTempArg1);
        } else {
            Uri uri = cr.insert(Apps.CONTENT_URI, values);
            mId = ContentUris.parseId(uri);
        }
        return mId;
    }

    public void delete(ContentResolver cr) {
        sTempArg1[0] = Long.toString(mId);
        cr.delete(Apps.CONTENT_URI, sWhereId, sTempArg1);
        cr.delete(Members.CONTENT_URI, sWhereApp, sTempArg1);
    }

    static Comparator<AppItem> createIndexComparator() {
        return new Comparator<AppItem>() {
            public int compare(AppItem data1, AppItem data2) {
                return data1.index - data2.index;
            }
        };
    }

    static Comparator<AppItem> createFrequentComparator() {
        return new Comparator<AppItem>() {
            public int compare(AppItem data1, AppItem data2) {
                final int count1 = (data1 == null ? 0 : data1.getCount());
                final int count2 = (data2 == null ? 0 : data2.getCount());
                return count2 - count1; // Reverse comparison to make list descending
            }
        };
    }

    static Comparator<AppItem> createRecentComparator() {
        return new Comparator<AppItem>() {
            public int compare(AppItem data1, AppItem data2) {
                final long time1 = (data1 == null ? 0L : data1.getTime());
                final long time2 = (data2 == null ? 0L : data2.getTime());
                if (time1 > time2) {
                    return -1;
                } else if (time1 < time2) {
                    return 1;
                } else {
                    return 0;
                }
            }
        };
    }

    static Comparator<AppItem> createCarrierComparator(final List<String> carrierList){
        return new Comparator<AppItem>() {

            @Override
            public int compare(AppItem data1, AppItem data2) {
                final int carrierIndex1 = carrierList.indexOf(data1.mComponent.flattenToString());
                final int carrierIndex2 = carrierList.indexOf(data2.mComponent.flattenToString());

                if((carrierIndex1 != -1) && (carrierIndex2 != -1)){
                    //If both are in the carrier list, compare their index
                    return carrierIndex1 - carrierIndex2 ;
                }else if((carrierIndex1 != -1) && (carrierIndex2 == -1)){
                    //Only first package is on the list, it must take precedence
                    return -1;
                }else if ((carrierIndex1 == -1 ) && (carrierIndex2 != -1)){
                    //Only second package is on the list, thus it must take precedence
                    return 1;
                }else{
                    // None of packages are in the carrier list. Stick to alphabetical
                    return data1.index - data2.index;
                }
            }
        };
    }
}
