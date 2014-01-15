package com.motorola.mmsp.motohomex.apps;

import java.util.Comparator;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.motorola.mmsp.motohomex.LauncherApplication;
import com.motorola.mmsp.motohomex.R;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Groups;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Members;
//import com.motorola.mmsp.motohomex.product.config.ProductConfigs;

/**
 * An item that describes an application group, either user-created or
 * a special group such as "Recent".
 */
public class GroupItem implements Comparable<GroupItem> {

    public static final String TAG = "GroupItem";

    /** Intent action for starting a group from Home workspace. */
    public static final String ACTION_START_GROUP =
        "com.android.launcher.action.START_GROUP";

    public static long INVALID_ID = -1L;

    //-----------------------------------------------------
    // Constants

    private static final String sWhereId = "_id=?";
    private static final String sWhereGroup = Members.GROUP + "=?";
    private static final String [] sTempArg1 = new String[1];


//-----------------------------------------------------
    // Fields

    /** DB index of this group. */
    private long mId;
    /** Name of this group. */
    private CharSequence mName;
    /** Type of this group. */
    private int mType;
    /** Sort index of this group. */
    private int mSort;
    /** Icon set of this group. */
    private int mIconSet;

    /** IDs of folder icons in grid, indexed by icon set. */
    public static final int [] sGridIconIds = {
        R.drawable.ic_launcher_app_group_all,
        R.drawable.ic_launcher_app_group_downloaded,
        R.drawable.ic_launcher_app_group_frequents,
        R.drawable.ic_launcher_app_group_generic,
        R.drawable.ic_launcher_app_group_calendar,
        R.drawable.ic_launcher_app_group_camcorder,
        R.drawable.ic_launcher_app_group_camera,
        R.drawable.ic_launcher_app_group_chat,
        R.drawable.ic_launcher_app_group_drink,
        R.drawable.ic_launcher_app_group_favorites,
        R.drawable.ic_launcher_app_group_finance,
        R.drawable.ic_launcher_app_group_fitness,
        R.drawable.ic_launcher_app_group_food,
        R.drawable.ic_launcher_app_group_games,
        R.drawable.ic_launcher_app_group_globe,
        R.drawable.ic_launcher_app_group_love,
        R.drawable.ic_launcher_app_group_movies,
        R.drawable.ic_launcher_app_group_music,
        R.drawable.ic_launcher_app_group_navigation,
        R.drawable.ic_launcher_app_group_smiley,
        R.drawable.ic_launcher_app_group_social,
        R.drawable.ic_launcher_app_group_travel,
        R.drawable.ic_launcher_app_group_utilities,
        R.drawable.ic_launcher_app_group_weather,
    };

    /** IDs of folder icons shown when filtering, indexed by icon set. */
    public static final int [] sFilterIconIds = {
        R.drawable.ic_filter_app_group_all,
        R.drawable.ic_filter_app_group_downloaded,
        R.drawable.ic_filter_app_group_frequents,
        R.drawable.ic_filter_app_group_generic,
        R.drawable.ic_filter_app_group_calendar,
        R.drawable.ic_filter_app_group_camcorder,
        R.drawable.ic_filter_app_group_camera,
        R.drawable.ic_filter_app_group_chat,
        R.drawable.ic_filter_app_group_drink,
        R.drawable.ic_filter_app_group_favorites,
        R.drawable.ic_filter_app_group_finance,
        R.drawable.ic_filter_app_group_fitness,
        R.drawable.ic_filter_app_group_food,
        R.drawable.ic_filter_app_group_games,
        R.drawable.ic_filter_app_group_globe,
        R.drawable.ic_filter_app_group_love,
        R.drawable.ic_filter_app_group_movies,
        R.drawable.ic_filter_app_group_music,
        R.drawable.ic_filter_app_group_navigation,
        R.drawable.ic_filter_app_group_smiley,
        R.drawable.ic_filter_app_group_social,
        R.drawable.ic_filter_app_group_travel,
        R.drawable.ic_filter_app_group_utilities,
        R.drawable.ic_filter_app_group_weather,
    };

    /**
     * IDs of folder icons shown during edit, indexed by icon set.
     * Note that the entries for special groups shouldn't be needed,
     * and so these entries in the array point to the filter icons.
     * This is simply to avoid a crash and show something reasonable
     * in case a group has an incorrect icon set index.
     */
    /*2012-7-11, modify by bvq783 for switchui-2120*/
    public static final int [] sEditIconIds = {
        R.drawable.ic_filter_app_group_all_off,
        R.drawable.ic_filter_app_group_downloaded_off,
        R.drawable.ic_filter_app_group_frequents_off,
        R.drawable.ic_filter_app_group_generic_off,
        R.drawable.ic_filter_app_group_calendar_off,
        R.drawable.ic_filter_app_group_camcorder_off,
        R.drawable.ic_filter_app_group_camera_off,
        R.drawable.ic_filter_app_group_chat_off,
        R.drawable.ic_filter_app_group_drink_off,
        R.drawable.ic_filter_app_group_favorites_off,
        R.drawable.ic_filter_app_group_finance_off,
        R.drawable.ic_filter_app_group_fitness_off,
        R.drawable.ic_filter_app_group_food_off,
        R.drawable.ic_filter_app_group_games_off,
        R.drawable.ic_filter_app_group_globe_off,
        R.drawable.ic_filter_app_group_love_off,
        R.drawable.ic_filter_app_group_movies_off,
        R.drawable.ic_filter_app_group_music_off,
        R.drawable.ic_filter_app_group_navigation_off,
        R.drawable.ic_filter_app_group_smiley_off,
        R.drawable.ic_filter_app_group_social_off,
        R.drawable.ic_filter_app_group_travel_off,
        R.drawable.ic_filter_app_group_utilities_off,
        R.drawable.ic_filter_app_group_weather_off,
    };
    /*2012-7-11, modify end*/

    //-----------------------------------------------------
    // Methods

    /** Constructor called when loading all settings from database. */
    public GroupItem(long id, CharSequence name, int type, int sort, int iconSet) {
        mId = id;
        mName = name;
        mType = type;
        setSort(sort);
        mIconSet = iconSet;
    }

    /** Constructor called when creating a new group. */
    public GroupItem(CharSequence newName) {
        mId = INVALID_ID;
        mName = newName;
        mType = Groups.TYPE_USER;
        setSort(Groups.SORT_ALPHA);
        mIconSet = Groups.ICON_SET_USER;
    }

    public long getId() {
        return mId;
    }

    /**
     * Return the group name, including the localized version of special names.
     * @param context For retrieving localized names.
     * @return The group name.
     */
    public CharSequence getName(Context context) {
        // For special groups, get the string resource ID
        int stringId = 0;
        /* Modifyed by amt_chenjin 2012.04026 for SWITCHUI-850 begin */
        Context mContext = null;
        if(context != null) {
        	mContext = context.getApplicationContext();
        }
        switch (mType) {
            case Groups.TYPE_ALL_APPS:
                stringId = R.string.all_apps;
                break;
            case Groups.TYPE_DOWNLOADED:
                stringId = R.string.downloaded;
                break;
            case Groups.TYPE_FREQUENTS:
                stringId = R.string.frequent;
                break;
        }
		/* Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 begin */
		if (stringId == 0) {
			if(mName == null || mName=="") {
				return mName;
			}
			String name = mName.toString().trim();
			String finalName = "";
			if (name.length()>3 && "com".equals(name.substring(0, 3))) {
				String packName = null;
				int index = name.indexOf(":");
				if (index > 0) {
					packName = name.substring(0, index);
				}
				Context tempContext;
				try {
					tempContext = mContext.createPackageContext(packName,
							Context.CONTEXT_IGNORE_SECURITY);
				} catch (Exception e) {
					//Log.d("GroupItem", "createPackageContext exception: " + e);
					e.printStackTrace();
					return "default";
				}
				Resources r = tempContext.getResources();
				int id = r.getIdentifier(name, null, null);
				if (id == 0) {
					finalName = "default";
				} else {
					finalName = r.getString(id);
				}
			} else if (name.length() > 7
					&& "android".equals(name.substring(0, 7))) {
				/* modifyed by amt_chenjing for SWITCHUI-878 begin */
				/*
				 * if ((name != null) && (context != null) && (context
				 * instanceof AppsActivity))
				 */
				if ((name != null) && (context != null))
				/* modifyed by amt_chenjing for SWITCHUI-878 end */{
					Resources r = context.getResources();
					int id = r.getIdentifier(name, null, null);
					if (id == 0) {
						finalName = "default";
					} else {
						finalName = context.getString(id);
					}
				}
			} else {
				if ((mName != null) && (context != null)) {
	                	    return AppsModel.localizeGroupName(mName.toString(), context); 
	            		}
	            		return mName;
			}
			return finalName;
			/* Modifyed by amt_chenjin 2012.04026 for SWITCHUI-850 end */
			/* Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 end */
        } else if (context == null) {
            return null;
        } else {
            return context.getString(stringId);
        }
    }

    @Override
    public String toString() {
        return (mName == null ? "" : mName.toString());
    }

    public void setName(CharSequence name) {
        mName = name.toString();
    }

    public int getType() {
        return mType;
    }
    /**add for IKDOMINO-6717 by bphx43 2012-03-21*/
    public void setType(int type){
    	mType = type;
    }
    /**end by bphx43*/

    /** @return true if this group can have its name and icon modified. */
    boolean isEditable() {
        switch (mType) {
            case Groups.TYPE_ALL_APPS:
            case Groups.TYPE_DOWNLOADED:
            case Groups.TYPE_FREQUENTS:
				/**add for 	IKDOMINO-6717 by bphx43 2012-03-21*/
            case Groups.TYPE_CBS:
				/**end by bphx43*/
                return false;
        }
        return true;
    }

    /** @return true if the user may add or remove apps from this group. */
    boolean canManageApps() {
        switch (mType) {
            case Groups.TYPE_ALL_APPS:
            case Groups.TYPE_DOWNLOADED:
            case Groups.TYPE_FREQUENTS:
	    /**add for IKDOMINO-6717 by bphx43 2012-03-21*/
            case Groups.TYPE_CBS:
	    /**end by bphx43*/
                return false;
        }
        return true;
    }

    /** @return true if the user may sort apps in this group. */
    boolean isSortable() {
        return true;
    }

    /** @return the string ID of the message to display when this group is empty. */
    public int getEmptyGroupMessageId() {
        switch (mType) {
            case Groups.TYPE_ALL_APPS:
                return R.string.loading_apps;
            case Groups.TYPE_DOWNLOADED:
                return R.string.no_downloaded_apps;
            case Groups.TYPE_FREQUENTS:
                return R.string.no_frequents_apps;
            default:
                return R.string.empty_group;
        }
    }

    public int getSort() {
        return mSort;
    }

    public void setSort(int sort) {
        switch (sort) {
            default: // change illegal values to SORT_ALPHA
            case Groups.SORT_ALPHA:
                mSort = Groups.SORT_ALPHA;
                break;
            case Groups.SORT_FREQUENTS:
            case Groups.SORT_CARRIER:
                mSort = sort;
                break;
        }
    }

    public int getIconSet() {
        return mIconSet;
    }

    public void setIconSet(int iconSet) {
        mIconSet = iconSet;
    }
    /*Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 begin*/
    public Drawable getGridIcon(Context context) {
        return context.getResources().getDrawable(mIconSet > sGridIconIds.length ? mIconSet : sGridIconIds[mIconSet]);
    }

    public Drawable getFilterIcon(Context context) {
        return context.getResources().getDrawable(mIconSet > sGridIconIds.length ? mIconSet : sFilterIconIds[mIconSet]);
    }

    public Drawable getEditIcon(Context context) {
        return context.getResources().getDrawable(mIconSet > sGridIconIds.length ? mIconSet : sEditIconIds[mIconSet]);
    }
    /*Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 end*/
    /**
     * Save this group to the database.
     */
    public long save(ContentResolver cr) {
        // Don't try to save if name is empty.
        // this can happen while creating a new group,
        // but before user has saved group (e.g. icon is set).
        if ((mType == Groups.TYPE_USER) && (mName == null)) {
            return mId;
        }
        ContentValues values = new ContentValues();
        if (mName != null) {
            values.put(Groups.NAME, mName.toString());
        }
        values.put(Groups.TYPE, mType);
        values.put(Groups.SORT, mSort);
        values.put(Groups.ICON_SET, mIconSet);
        if (mId >= 0) {
            sTempArg1[0] = Long.toString(mId);
            cr.update(Groups.CONTENT_URI, values, sWhereId, sTempArg1);
        } else {
            Uri uri = cr.insert(Groups.CONTENT_URI, values);
            mId = ContentUris.parseId(uri);
        }
        return mId;
    }

    /**
     * Delete this group from the database.
     */
    public void delete(ContentResolver cr) {
        sTempArg1[0] = Long.toString(mId);
        cr.delete(Groups.CONTENT_URI, sWhereId, sTempArg1);
        cr.delete(Members.CONTENT_URI, sWhereGroup, sTempArg1);
    }

    /** @return true if the groups have the same ID. */
    public boolean equals(GroupItem other) {
        return mId == other.getId();
    }

    /** Creates a Comparator to compare groups by name only. */
    static Comparator<GroupItem> createNameComparator() {
        return new Comparator<GroupItem>() {
            public int compare(GroupItem item1, GroupItem item2) {
                return item1.toString().compareToIgnoreCase(item2.toString());
            }
        };
    }

    /** Makes sure that special groups appear at the top of lists. */
    public int compareTo(GroupItem other) {
        final int type1 = mType;
        final int type2 = other.getType();
        if (type1 != type2) {
            return type1 - type2;
        }
        return toString().compareToIgnoreCase(other.toString());
    }

    /** Create an intent that views this group in the apps tray. */
    public Intent createGroupIntent() {
        return createGroupIntent(mId);
    }
    public static Intent createGroupIntent(final long id) {
        final Uri uri = new Uri.Builder()
            .scheme(Groups.TABLE_NAME)
            .authority(AppsSchema.AUTHORITY)
            .path(Long.toString(id))
            .query("")
            .fragment("")
            .build();
        return new Intent(ACTION_START_GROUP).setData(uri);
    }

    /** @return an Intent with extras that describe this group. */
    public Intent createIntent(Context context) {
       /* LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        TypedArray array = app.getConfigManager().obtainTypedArray(ProductConfigs.GRID_ICON_IDS);
        Resources res = array.getResources();
        int resid = array.getResourceId(mIconSet, 0);
        String resPackage = res.getResourcePackageName(resid);
        String resName = res.getResourceName(resid);
        */
        // Modified by e13775 at July11 2012 for organize apps' group start
        //final Intent.ShortcutIconResource groupIcon = new
        //        Intent.ShortcutIconResource();
        /*Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 begin*/
        final Intent.ShortcutIconResource groupIcon = Intent.ShortcutIconResource.fromContext(context,
                mIconSet > sGridIconIds.length ? mIconSet : sGridIconIds[mIconSet]);
        /*Modifyed by amt_chenjin 2012.04022 for SWITCHUI-743 end*/
        // Modified by e13775 at July11 2012 for organize apps' group end

        //groupIcon.packageName = resPackage;
        //groupIcon.resourceName = resName;

        final Intent intent = new Intent()
                .putExtra(Intent.EXTRA_SHORTCUT_NAME, getName(context))
                .putExtra(Intent.EXTRA_SHORTCUT_INTENT, createGroupIntent())
                .putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, groupIcon);
        return intent;
    }

}
