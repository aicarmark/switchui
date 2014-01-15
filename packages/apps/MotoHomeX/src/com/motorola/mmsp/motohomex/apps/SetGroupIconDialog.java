/*
 * Copyright (C) 2011 Motorola Mobility.
 */

package com.motorola.mmsp.motohomex.apps;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.motorola.mmsp.motohomex.LauncherApplication;
import com.motorola.mmsp.motohomex.R;
import com.motorola.mmsp.motohomex.apps.AllAppsPage.AppsDialog;
import com.motorola.mmsp.motohomex.apps.AppsSchema.Groups;
//import com.motorola.mmsp.motohomex.product.config.ProductConfigManager;
//import com.motorola.mmsp.motohomex.product.config.ProductConfigs;

/**
 * Manages a dialog to set an app group icon.
 */
class SetGroupIconDialog extends BaseAdapter implements AppsDialog, OnItemClickListener, OnClickListener, OnCancelListener {

    /** Back pointer to apps view. */
    private final AllAppsPage mAllAppsPage;
    /** Icon set used by current group (for highlighting). */
    private int mCurrentIconSet;

    //private ProductConfigManager mConfig;

    SetGroupIconDialog(AllAppsPage appsPage) {
        mAllAppsPage = appsPage;
        LauncherApplication app = (LauncherApplication) mAllAppsPage.getContext().getApplicationContext();
        //mConfig = app.getConfigManager();
    }

    public void show() {
        mAllAppsPage.mAppsDialog = this;

        final Context context = mAllAppsPage.getContext();

        // Create the icon grid
        LayoutInflater inflater = LayoutInflater.from(context);
        GridView iconGrid = (GridView) inflater.inflate(R.layout.all_apps_dialog_set_group_icon, null);
        iconGrid.setAdapter(this);
        iconGrid.setOnItemClickListener(this);

        // Highlight the current icon
        GroupItem editGroupItem = mAllAppsPage.getEditGroupItem();
        mCurrentIconSet = editGroupItem.getIconSet();

        // Build and run the dialog
        AlertDialog dialog = new AlertDialog.Builder(context)
                                .setTitle(R.string.group_icon)
                                .setView(iconGrid)
                                .setNegativeButton(android.R.string.cancel, this)
                                .create();
        dialog.setOnCancelListener(this);

        mAllAppsPage.showDialog(dialog);
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        mAllAppsPage.setNextDialog(AllAppsPage.DIALOG_EDIT_GROUP);

        // Click on an item in the group icon picker dialog
        mAllAppsPage.setGroupIconSet(getIconSet(position));
        mAllAppsPage.showDialog(null);
    }


    /* This funtion need modification later */
    public int getCount() {
        //Resources res =  mAllAppsPage.getContext().getResources();
        /*TypedArray array  = mConfig.obtainTypedArray(ProductConfigs.EDIT_ICON_IDS);
        int count = array.length() - Groups.ICON_SET_FIRST_USER;
        array.recycle();
        return count;*/
	return GroupItem.sEditIconIds.length - Groups.ICON_SET_FIRST_USER;
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return position + Groups.ICON_SET_FIRST_USER;
    }

    public int getIconSet(int position) {
        return position + Groups.ICON_SET_FIRST_USER;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mAllAppsPage.mInflater.inflate(R.layout.all_apps_dialog_set_group_icon_item, parent, false);
        }
        int iconSet = getIconSet(position);
        TextView textView = (TextView) convertView;
        Drawable d = mAllAppsPage.getContext().getResources().getDrawable(GroupItem.sEditIconIds[iconSet]);
        textView.setCompoundDrawablesWithIntrinsicBounds(null, d, null, null);
	textView.setEnabled(iconSet != mCurrentIconSet);
        return textView;
    }

    @Override
    public int getId() {
        return AllAppsPage.DIALOG_SET_ICON;
    }

    @Override
    public void restoreState(Bundle state) {
        mCurrentIconSet = state.getInt(AllAppsPage.SAVE_STATE_SET_ICON_CURRENT);
    }

    @Override
    public void saveState(Bundle state) {
        state.putInt(AllAppsPage.SAVE_STATE_SET_ICON_CURRENT, mCurrentIconSet);
    }

    @Override
    public void dismiss() {}

    @Override
    public void onClick(DialogInterface dialog, int which) {
        mAllAppsPage.setNextDialog(AllAppsPage.DIALOG_EDIT_GROUP);
        mAllAppsPage.setNextDialog(AllAppsPage.DIALOG_EDIT_GROUP);
        mAllAppsPage.showDialog(null);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mAllAppsPage.setNextDialog(AllAppsPage.DIALOG_EDIT_GROUP);
    }

}
