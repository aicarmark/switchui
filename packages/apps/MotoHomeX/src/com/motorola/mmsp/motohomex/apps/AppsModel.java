/*
 * Copyright (C) 2010 Motorola Mobility.
 */

package com.motorola.mmsp.motohomex.apps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import com.motorola.mmsp.motohomex.ApplicationInfo;
import com.motorola.mmsp.motohomex.LauncherApplication;
import com.motorola.mmsp.motohomex.LauncherSettings;
import com.motorola.mmsp.motohomex.R;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Apps;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Folders;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Groups;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Members;
import android.util.Log;
public class AppsModel {

    //-----------------------------------------------------
    // Statics

    private static final String TAG = "Launcher";

    /** Maximum number of allowed groups. */
    static final int MAX_GROUPS = 100;

    /** Delay before serializing changes to database */
    private static final long SAVE_DELAY = 5000;

    /**
     * Maps group name keys for auto-localizable app group names (as found in
     * R.array.apps_group_name_key) into the current localized version of the
     * group name (as found in R.array.apps_group_name).
     */
    static HashMap<String, String> sGroupNameMap;

    /**
     * Prefix for localizable app group names in resource files.
     * See instructions in res/values/arrays.xml
     */
    private static final String LOCALIZE_PREFIX = "localize_key_"; // DO NOT TRANSLATE
    private static final int LOCALIZE_PREFIX_LEN = LOCALIZE_PREFIX.length();

    //-----------------------------------------------------
    // Fields

    private Context mContext;

    /** Handler for posting serialize Runnables. */
    private final Handler mHandler;

    /** Map of all apps, keyed by component. */
    private HashMap<ComponentName, AppItem> mAppMap;
    /** Map of all groups, keyed by ID. */
    private final HashMap<Long, GroupItem> mGroupMap;
    /** List of currently-installed apps (map may contain SD card apps too). */
    private final ArrayList<AppItem> mCurrentAppList;
    /** Flag that apps have been added or removed from AppsList. */
    //private boolean mIsDirty;
    /** Flag that carrier specified a list of ordered packages for default App Tray's sort*/
    private boolean mHasCarrierList;
    
    private final Object mLock = new Object();
    int mLastAppsListSeq = 0;
    int mAppsListSeq = 1;


    public AppsModel(Context context) {
        mContext = context;

        HandlerThread thread = new HandlerThread(AppsModel.class.getSimpleName());
        thread.start();
        mHandler = new Handler(thread.getLooper());

        mAppMap = new HashMap<ComponentName, AppItem>();
        mGroupMap = new HashMap<Long, GroupItem>();
        mCurrentAppList = new ArrayList<AppItem>();
        //mIsDirty = true;

        //Check if carrier has defined an ordered list of packages for App Tray
        mHasCarrierList = false;

        loadSettings();
    }
    
    private void postSave() {
    /*    mHandler.removeCallbacks(mSerializer);
        mHandler.postDelayed(mSerializer, SAVE_DELAY);
    */
    }

    public void setDirty() {
        //mIsDirty = true;
        synchronized (mLock) {
            mAppsListSeq++;
        }
    }

    /**
     * Update the time stamp and use count for an application.  This should be
     * called when an application is downloaded or launched.
     * @param intent An intent used to launch an application, if the intent is
     *      the same as one used by an application.
     */
    public void markAsUsed(Intent intent) {
        final ComponentName cn = intent.getComponent();
        if (cn == null) {
            return;
        }

        // Update the count and time stamp
        final AppItem appItem = mAppMap.get(cn);
        if (appItem != null) {
            appItem.markAsUsed();
            appItem.save(mContext.getContentResolver());
            postSave();
        } else {
            // ignore when a non-apps-tray app is launched
        }
    }

    /**
     * Add a group for an activity.
     * @param info The activity to add the group for.
     * @param groupId The new group to add.
     */
    public void addToGroup(ApplicationInfo info, long groupId) {
        final ComponentName cn = info.componentName;
        final AppItem appItem = mAppMap.get(cn);
        if (appItem != null) {
            final ContentResolver cr = mContext.getContentResolver();
            appItem.addToGroup(cr, groupId, mHandler);
            postSave();
        } else {
            Log.w(TAG, "Unable to add unknown activity " + cn.flattenToShortString() + " to group #" + groupId);
        }
    }

    /**
     * Add an application on app map.
     *
     * @param cn Componen name of application.
     * @param app Application that will be added.
     */
    public void addToAppMap(ComponentName cn, AppItem app) {
        if (mAppMap.get(cn) == null) {
            mAppMap.put(cn, app);
        }
    }

    /**
     * Remove a group for an activity.
     * @param info The activity to remove the group for.
     * @param groupId The group to remove.
     */
    public void removeFromGroup(ApplicationInfo info, long groupId) {
        final ComponentName cn = info.componentName;
        final AppItem appItem = mAppMap.get(cn);
        if (appItem != null) {
            final ContentResolver cr = mContext.getContentResolver();
            if (appItem.removeFromGroup(cr, groupId, mHandler)) {
                postSave();
            } else {
                Log.w(TAG,"App :" + cn.flattenToShortString() + " wasn't in group #" + groupId);
            }
        } else {
            Log.w(TAG, "Unable to remove unknown activity " + cn.flattenToShortString() + " from group #" + groupId);
        }
    }

    /**
     * Remove a group from all activities.
     * @param groupId The group to remove.
     */
    public void removeGroup(long groupId) {
        final ContentResolver cr = mContext.getContentResolver();
        for (AppItem appItem: mAppMap.values()) {
            appItem.removeFromGroup(cr, groupId, mHandler);
        }
        final GroupItem groupItem = mGroupMap.remove(groupId);
        if (groupItem != null) {
            groupItem.delete(mContext.getContentResolver());
            postSave();
        }
    }

    /**
     * Check all groups for one with the given group name.  Comparisons are
     * done case-insensitive, and if a match is found, the existing name is
     * returned.
     * @param groupNameSeq The group name to check.
     * @param ignoreGroupItem Ignore this group when checking.
     * @return If non-null, the name of the existing group.  (May use different
     *      casing than parameter).
     */
    CharSequence groupHasName(CharSequence groupNameSeq, GroupItem ignoreGroupItem) {
        if (TextUtils.isEmpty(groupNameSeq)) {
            return null;
        }
        final String groupName = groupNameSeq.toString();
        for (GroupItem groupItem: mGroupMap.values()) {
            if (ignoreGroupItem.getId() == groupItem.getId()) {
                continue;
            }
            final CharSequence otherName = groupItem.getName(mContext);
            if ((otherName != null) && groupName.equalsIgnoreCase(otherName.toString())) {
                return otherName;
            }
        }
        return null;
    }

    /**
     * Updates the apps that belong to a group, removing the old name
     * and adding the new name.  If the old name is not found, then
     * a new group is created.
     * @param appsList List of apps to check
     * @param editGroupItem The group being renamed.
     * @param newName New group name.
     * @return the new group ID, in case a new group was created.
     */
    public long saveGroup(GroupItem groupItem) {
        long groupId = groupItem.getId();
        final boolean isNewGroup = (groupId <= 0);
        groupId = groupItem.save(mContext.getContentResolver());
        postSave();
        if (isNewGroup) {
            mGroupMap.put(groupId, groupItem);
        }
        return groupId;
    }

    /**
     * Modify the sorting method used by a group.
     * @param groupId ID of the group to modify.
     * @param sort Index of new sorting method.
     */
    public void setGroupSort(long groupId, int sort) {
        final GroupItem groupItem = mGroupMap.get(groupId);
        groupItem.setSort(sort);
        groupItem.save(mContext.getContentResolver());
        postSave();
    }

    /**
     * Modify the icon set used to represent a group in the UI.
     * @param groupId ID of the group to modify.
     * @param iconSet Index of icon set.
     */
    public void setGroupIconSet(long groupId, int iconSet) {
        final GroupItem groupItem = mGroupMap.get(groupId);
        groupItem.setIconSet(iconSet);
        groupItem.save(mContext.getContentResolver());
        postSave();
    }

    /** @return the total number of app groups in use. */
    public int getNumGroups() {
        return mGroupMap.size();
    }

    /**
     * Create an unsorted list of all group names.
     * @param groupItemList A list to which all groups will be appended.
     */
    public void getAllGroups(ArrayList<GroupItem> groupItemList) {
        groupItemList.clear();
        groupItemList.addAll(mGroupMap.values());
    }

    /**
     * Return a list of all the indexes into the list of apps filtered and
     * sorted as appropriate for the group.
     * @param appsList The list of applications to check.
     * @param groupItem The current group (determines filter and sort).
     * @return a sorted list of indexes into appsList.  If none match, an empty
     *      list is returned.  If the group is null or empty, null is returned.
     */
    public ArrayList<Integer> getMatchingIndexes(ArrayList<ApplicationInfo> appsList,
                                                 GroupItem groupItem) {
        final int groupSort = groupItem.getSort();
        final int groupType = groupItem.getType();
        final boolean onlyDownloaded = (groupType == Groups.TYPE_DOWNLOADED);
	/**add for IKDOMINO-6717 by bphx43 2012-03-21*/
        boolean noFilter = true;
        switch(groupType){
        case Groups.TYPE_USER:
        case Groups.TYPE_CBS:
        case Groups.TYPE_NEW:
        	noFilter = false ;
        	break;
        }
//        final boolean noFilter = (groupType != Groups.TYPE_USER );

        if (noFilter && !onlyDownloaded && (groupSort == Groups.SORT_ALPHA)) {
            return null;
        }
	/**end by bphx43*/


        // Rebuild list of current apps as needed
        final int numApps = appsList.size();
        int appsListSeq;
        boolean appsListDirty;
        synchronized (mLock) {
            appsListSeq = mAppsListSeq;
            appsListDirty = mAppsListSeq != mLastAppsListSeq;
        }        
        Log.d(TAG,"getMatchingIndexes begin rebuildCurrentAppList mAppsListSeq="+mAppsListSeq);
        Log.d(TAG,"getMatchingIndexes begin rebuildCurrentAppList mLastAppsListSeq="+mLastAppsListSeq);
        if (appsListDirty || (numApps != mCurrentAppList.size())) {
            rebuildCurrentAppList(appsList);
            synchronized (mLock) { 
                if (appsListSeq == mAppsListSeq) {
                    mLastAppsListSeq = mAppsListSeq;
                }
            }
        }
        Log.d(TAG,"getMatchingIndexes end rebuildCurrentAppList mAppsListSeq="+mAppsListSeq);
        Log.d(TAG,"getMatchingIndexes end rebuildCurrentAppList mLastAppsListSeq="+mLastAppsListSeq);

        // Sort list as requested
        Comparator<AppItem> comparator;
        switch(groupSort) {
            case Groups.SORT_ALPHA:
                comparator = AppItem.createIndexComparator();
                break;
            case Groups.SORT_FREQUENTS:
                comparator = AppItem.createFrequentComparator();
                break;
            /*case Groups.SORT_CARRIER:
                LauncherApplication app = (LauncherApplication) mContext.getApplicationContext();
                ProductConfigManager config = app.getConfigManager();
                List<String> carrierList = Arrays.asList(config.getStringArray(ProductConfigs.CARRIER_APP_LIST));
                comparator = AppItem.createCarrierComparator(carrierList);
                break;*/
            default:
                throw new IllegalArgumentException("Invalid sort option: " + groupSort);
        }
        Collections.sort(mCurrentAppList, comparator);

        // Build list of indexes into appsList parameter
        ArrayList<Integer> indexList = new ArrayList<Integer>(numApps);
        for (int index = 0; index < numApps; ++index) {
            final AppItem appItem = mCurrentAppList.get(index);
            if ((appItem != null) &&
                    (noFilter || appItem.isMemberOf(groupItem.getId())) &&
                    (!noFilter || !appItem.isInFolder()) &&
                    (!onlyDownloaded || appItem.isDownloaded()) &&
                    (groupType != Groups.TYPE_FREQUENTS || appItem.getCount() != 0))
            {
                        indexList.add(appItem.index);
            }
        }
        return indexList;
    }

    private void rebuildCurrentAppList(ArrayList<ApplicationInfo> appsList) {
        boolean needPost = false;
        mCurrentAppList.clear();
        ArrayList<AppItem> appsToSave = new ArrayList<AppItem>();
        final int numApps = appsList.size();
        long folderId = AppItem.INVALID_ID;
        for (int index = 0; index < numApps; ++index) {
            ApplicationInfo appInfo = appsList.get(index);
            ComponentName cn = appInfo.componentName;
            AppItem appItem = mAppMap.get(cn);
            if (appItem == null) {
                // Create new AppItem when needed

                appItem = new AppItem(cn, !appInfo.isSystem, folderId);
                appsToSave.add(appItem);
                mAppMap.put(cn, appItem);
                needPost = true;
            }else if (appItem != null && appItem.isDownloaded() != (!appInfo.isSystem)) {
                appItem.setIsDownloaded(!appInfo.isSystem);
                appsToSave.add(appItem);
                needPost = true;
            }
            
            mCurrentAppList.add(appItem);
            appItem.index = index; // save the info index
        }
        if (needPost) {
            new SaveAppItems().execute(appsToSave);
        }
    }

    /**
     *
     * AsyncTask to add or upgrade items in Apps database
     *
     */
    private class SaveAppItems extends AsyncTask<ArrayList<AppItem>, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList<AppItem>... arg) {
            if (arg.length != 1) {
                Log.d(TAG, "Invalid argument received. Returning");
                return null;
            }
            ContentResolver cr = mContext.getContentResolver();
            ArrayList<AppItem> appsToSave = arg[0];
            for (AppItem app : appsToSave) {
                app.save(cr);
            }
            postSave();
            return null;
        }
    }

    public AppItem getAppItem(ApplicationInfo appInfo) {
        ComponentName cn = appInfo.componentName;
        AppItem appItem = mAppMap.get(cn);
        return appItem;
    }

    /**
     * Loads all settings from the apps database.
     */
    private void loadSettings() {
        ContentResolver cr = mContext.getContentResolver();
        Cursor c;

        // Load groups
        c = cr.query(Groups.CONTENT_URI, Groups.PROJECTION, null, null, Groups._ID);
        try {
            while (c != null && c.moveToNext()) {
                // Load group fields
                final long id = c.getLong(Groups.INDEX_ID);
                final String name = c.getString(Groups.INDEX_NAME);
                final int type = c.getInt(Groups.INDEX_TYPE);
                final int sort = c.getInt(Groups.INDEX_SORT);
                final int iconSet = c.getInt(Groups.INDEX_ICON_SET);

                // Add to group map
                GroupItem groupItem = new GroupItem(id, name, type, sort, iconSet);
                mGroupMap.put(id, groupItem);
            }
        } catch (Exception e) {
            Log.w(TAG,"Error loading groups :" + e);
        } finally {
            if (c != null) {
                c.close();
            }
        }

        // Build a map of group membership
        HashMap<Long, HashSet<Long>> memberMap = new HashMap<Long, HashSet<Long>>();
        c = cr.query(Members.CONTENT_URI, Members.PROJECTION, null, null, Members.APP);
        try {
            long lastAppId = -1;
            HashSet<Long> groupIds = null;
            while (c != null && c.moveToNext()) {
                final long appId = c.getLong(Members.INDEX_APP);
                final long groupId = c.getLong(Members.INDEX_GROUP);
                if (appId != lastAppId) {
                    if ((lastAppId != -1) && !groupIds.isEmpty()) {
                        memberMap.put(lastAppId, groupIds);
                        groupIds = null;
                    }
                    lastAppId = appId;
                }
                if (groupIds == null) {
                    groupIds = new HashSet<Long>();
                }
                groupIds.add(groupId);
            }
            if ((lastAppId != -1) && !groupIds.isEmpty()) {
                memberMap.put(lastAppId, groupIds);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error loading group membership:" + e);
        } finally {
            if (c != null) {
                c.close();
            }
        }


        // Load apps
        c = cr.query(Apps.CONTENT_URI, Apps.PROJECTION, null, null, Apps._ID);
        try {
            while (c != null && c.moveToNext()) {
                // Load app fields
                final long id = c.getLong(Apps.INDEX_ID);
                final String component = c.getString(Apps.INDEX_COMPONENT);
                final int count = c.getInt(Apps.INDEX_COUNT);
                final long time = c.getLong(Apps.INDEX_TIME);
                final boolean downloaded = (0 != c.getInt(Apps.INDEX_DOWNLOADED));
                long folderId = AppItem.INVALID_ID;

                // Add to app map
                final HashSet<Long> groupIds = memberMap.get(id);
                final ComponentName cn = ComponentName.unflattenFromString(component);
                AppItem appItem = new AppItem(id, cn, count, time, downloaded, groupIds, folderId);
                mAppMap.put(cn, appItem);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error loading apps :" + e);
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Check whether carrier has defined a ordered list of packages that should
     * appear first in apps tray when carrier sorting is set
     * @return  true if the list is present
     */
    public boolean hasCarrierList(){
        return mHasCarrierList;
    }

    /**
     * Helper function to convert some standard app group names into their
     * localizable versions.  This allows product PMs to indicate that an app
     * group name should be localized.  Two matching string arrays are in
     * arrays.xml: the first with an English string, and the second with a
     * localizable key string, which is simply the English string with a prefix.
     * This function matches the key string, and if found uses the localized
     * version of the name.
     * @param name The app group name as stored in the database.
     * @return The app group name to show in the UI.
     */
    public static String localizeGroupName(String name, Context context) {
        // See if this group name wants to be localized
        /*if (name.startsWith(LOCALIZE_PREFIX)) {
            // Rebuild name map when needed
            if (sGroupNameMap == null) {
                sGroupNameMap = new HashMap<String, String>();
                // Load the arrays and at least make sure their lengths match
                LauncherApplication app = (LauncherApplication)context.getApplicationContext();
                ProductConfigManager config = app.getConfigManager();
                final String [] groupNameKeys = config.getStringArray(ProductConfigs.APPS_GROUP_NAME_KEY);
                final String [] currentGroupNames =config.getStringArray(ProductConfigs.APPS_GROUP_NAME);
                final int keyLen = groupNameKeys.length;
                final int currentLen = currentGroupNames.length;
                if (keyLen != currentLen) {
                    Logger.w(TAG, "Mismatch in localizable app group name arrays");
                } else {
                    // Map name keys (with prefix stripped off)
                    // to the current localized version of the name
                    for (int i = 0; i < keyLen; ++i) {
                        sGroupNameMap.put(
                                groupNameKeys[i].substring(LOCALIZE_PREFIX_LEN),
                                currentGroupNames[i]);
                    }
                }
            }

            // Strip off prefix for use as key, and as visible name if not in map
            name = name.substring(LOCALIZE_PREFIX_LEN);
            final String localizedName = sGroupNameMap.get(name);
            if (localizedName == null) {
                Logger.w(TAG, "localizable app group not found: ",name);
            } else {
                name = localizedName;
            }
        }*/
        return name;
    }

    public void localeChanged(){
        sGroupNameMap = null;
        setDirty();
    }
}
