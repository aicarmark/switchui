/*
 * Copyright (C) 2010 Motorola Mobility.
 */

package com.motorola.mmsp.motohomex.apps;

import java.util.ArrayList;
import java.util.Collections;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.motorola.mmsp.motohomex.R;

import com.motorola.mmsp.motohomex.apps.AppsSchema.Groups;

/**
 * Adapter that supplies the names of available app groups, as well as
 * special groups such as "All apps" and "Recent"
 */
class GroupAdapter extends BaseAdapter {

    public static final int ICON_NONE = 0;
    public static final int ICON_TOP = 1;
    public static final int ICON_LEFT = 2;

    private final Context mContext;
    private final LayoutInflater mInflater;


    /** The array of app groups. */
    /*2012-7-13, modify by bvq783 for switchui-2176*/
    private final ArrayList<GroupItem> mGroupItems = new ArrayList<GroupItem>();
    /** True to ignore special folders (for adding app to group). */
    private boolean mIgnoreSpecial;
    /** True to add the option "add group". */
    private boolean mAddGroupAvail;

    GroupAdapter(Context context, AppsModel appsModel, int iconLocation) {
        mContext = context;
        mInflater = LayoutInflater.from(context);

        // Fetch groups in use by installed applications
        /*2012-7-13, remove by bvq783 for switchui-2176*/
        //mGroupItems = new ArrayList<GroupItem>();
        appsModel.getAllGroups(mGroupItems);

        mIgnoreSpecial = false;
        mAddGroupAvail = true;
        sort();
    }

    public void setIgnoreSpecial(boolean ignoreSpecial) {
        mIgnoreSpecial = ignoreSpecial;
        mAddGroupAvail = !ignoreSpecial;
    }

    /**
     * This helper function is needed when some special groups are hidden
     * (all-apps and recent groups when choosing group to add app to).
     * @param position filtered position.
     * @return index into unfiltered list of groups.
     */
    public int getUnfilteredPosition(int position) {
        if (mIgnoreSpecial) {
            position += Groups.TYPE_NUM_SPECIAL;
        }
        return position;
    }

    public boolean isAddOption(int position){
        int count = 0;
        if (mAddGroupAvail){
            count = getCount();
            if (position == count - 1){
                return true;
            }
        }
        return false;
    }

    public void remove(GroupItem groupItem) {
        mGroupItems.remove(groupItem);
    }

    /**
     * Gets the index of the given group item.
     * @param groupItem The group to find
     * @return Index of the group, or -1 if not found.
     */
    public int indexOf(GroupItem groupItem) {
        return mGroupItems.indexOf(groupItem);
    }

    /**
     * Gets the index of the group with the given ID.
     * @param groupId The ID of the group to find.
     * @return Index of the group, or -1 if not found.
     */
    public int indexOf(long groupId) {
        final int numGroups = mGroupItems.size();
        for (int i = 0; i < numGroups; ++i) {
            if (groupId == mGroupItems.get(i).getId()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Add a group to the adapter.
     * @param name Name of the group.
     * @return The added group item.
     */
    GroupItem add(CharSequence name) {
        GroupItem groupItem = new GroupItem(name);
        add(groupItem);
        return groupItem;
    }

    /**
     * Add a group item to the adapter.
     */
    void add(GroupItem groupItem) {
        mGroupItems.add(groupItem);
    }

    void sort() {
        Collections.sort(mGroupItems);
    }

    public int getCount() {
        int n = mGroupItems.size();
        if (mIgnoreSpecial) {
            n -= Groups.TYPE_NUM_SPECIAL;
        }
        if (mAddGroupAvail){
            n++;
        }
        return n;
    }

    public Object getItem(int position) {
        return mGroupItems.get(position);
    }

    public GroupItem getGroupItemFromIndex(int position) {
        if ((position >= 0) && (position < mGroupItems.size())) {
            return mGroupItems.get(position);
        }
        return null;
     }

    public GroupItem getGroupItemFromId(long id) {
        return getGroupItemFromIndex(indexOf(id));
    }

    public GroupItem getGroupItemFromType(int type) {
        int n = mGroupItems.size();
        for (int i = 0; i < n; ++i) {
            final GroupItem groupItem = mGroupItems.get(i);
            if (groupItem.getType() == type) {
                return groupItem;
            }
        }
        return null;
    }

    public long getItemId(int position) {
        if (position < mGroupItems.size()){
            return getGroupItemFromIndex(position).getId();
        }
        return -1L;
    }

    // Spinner calls this method to measure itself and when it needs
    // the view to show up in the top
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.all_apps_drop_down_group_header, parent, false);
        }
        final TextView textView = (TextView) convertView.findViewById(R.id.all_apps_drop_down_group_item_button_title);
        final ImageView imageView = (ImageView) convertView.findViewById(R.id.all_apps_drop_down_group_item_button_icon);

        // Convert index to one that refers to all groups
        position = getUnfilteredPosition(position);
        GroupItem groupItem = null;
        if (position < mGroupItems.size()){
            groupItem = mGroupItems.get(position);
        }

        if (groupItem == null){
            // Add option
            imageView.setVisibility(View.INVISIBLE);

            // Set the item text
            textView.setText(mContext.getResources().getString(R.string.create_group));
        } else {
            final Drawable d = groupItem.getEditIcon(mContext);
            imageView.setImageDrawable(d);
            imageView.setVisibility(View.VISIBLE);

            // Set the item text
            textView.setText(groupItem.getName(mContext));
        }

        return convertView;
    }

    // Spinner calls this method to populate the ListPopupWindow
     @Override
     public View getDropDownView(int position, View convertView, ViewGroup parent) {
         if (convertView == null) {
             convertView = mInflater.inflate(R.layout.all_apps_drop_down_group_item, parent, false);
         }
         final TextView textView = (TextView) convertView.findViewById(R.id.all_apps_drop_down_group_item_button_title);
         final ImageView imageView = (ImageView) convertView.findViewById(R.id.all_apps_drop_down_group_item_button_icon);

         // Convert index to one that refers to all groups
         position = getUnfilteredPosition(position);
         GroupItem groupItem = null;
         if (position < mGroupItems.size()){
             groupItem = mGroupItems.get(position);
         }

         if (groupItem == null){
             // Add option
             imageView.setVisibility(View.INVISIBLE);

             // Set the item text
             textView.setText(mContext.getResources().getString(R.string.create_group));
         } else {
             final Drawable d = groupItem.getEditIcon(mContext);
             imageView.setImageDrawable(d);
             imageView.setVisibility(View.VISIBLE);

             // Set the item text
             textView.setText(groupItem.getName(mContext));
         }

         return convertView;
     }

}
