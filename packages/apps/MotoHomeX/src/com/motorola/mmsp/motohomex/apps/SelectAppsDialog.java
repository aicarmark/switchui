/*
 * Copyright (C) 2010 Motorola Mobility.
 */

package com.motorola.mmsp.motohomex.apps;

import java.util.ArrayList;
import java.util.BitSet;

import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.TextView;

import com.motorola.mmsp.motohomex.ApplicationInfo;
import com.motorola.mmsp.motohomex.Launcher;
import com.motorola.mmsp.motohomex.R;
import android.util.Log;

/**
 * Manages a dialog to let user select which apps are in the current group.
 */
class SelectAppsDialog extends ArrayAdapter<ApplicationInfo>
        implements AllAppsPage.AppsDialog, OnClickListener, OnItemClickListener {

    /** Type: int.  Key for sanity check in SelectAppsDialog. */
    private static final String SAVED_STATE_APP_COUNT = "app_count";
    /** Type: JSONArray of ints.  Key for saving checked apps in SelectAppsDialog. */
    private static final String SAVED_STATE_SELECTED_APPS = "selected_apps";

    /** Back pointer to enclosing view. */
    private final AllAppsPage mAllAppsPage;
    /** The Dialog showing the list of apps. */
    private final Dialog mDialog;
    /** The ListView showing the list of apps. */
    private final ListView mListView;
    /** An array of originally-selected apps. */
    private final BitSet mWasSelected = new BitSet();
    /** Flag that we are restoring state. */
    private boolean mRestoring;
    /** List of applications not hidden on the device. */
    ArrayList<ApplicationInfo> mSelectableAppList;
    private Button mOkButton; //Added by e13775 July30 2012 for SWITCHUI-2477

    SelectAppsDialog(AllAppsPage appsPage) {
        super(appsPage.getContext(), 0);
        mAllAppsPage = appsPage;

        mSelectableAppList = new ArrayList<ApplicationInfo>();

        // Create the dialog and setup views
        mDialog = new Dialog(mAllAppsPage.getContext());
        mDialog.setContentView(R.layout.all_apps_dialog_select_apps);
        mDialog.setOnDismissListener(mAllAppsPage);

        mDialog.setTitle(R.string.select_apps);

        mListView = (ListView) mDialog.findViewById(R.id.all_apps_dialog_select_apps_list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setAdapter(this);
        mListView.setOnItemClickListener(this);
	    //Modified by e13775 July30 2012 for SWITCHUI-2477 start
        mOkButton = (Button) mDialog.findViewById(R.id.all_apps_dialog_select_apps_ok);
        mOkButton.setOnClickListener(this);
        Button button = (Button) mDialog.findViewById(R.id.all_apps_dialog_select_apps_cancel);
        //Modified by e13775 July30 2012 for SWITCHUI-2477 end
        button.setOnClickListener(this);
    }

    public int getId() {
        return AllAppsPage.DIALOG_SELECT_APPS;
    }

    public int getCount() {
        return mSelectableAppList.size();
    }

    /** Display a dialog to select which apps go in the current group. */
    public void show() {
        final int numApps = mAllAppsPage.getAppsListSize();
        mAllAppsPage.mAppsDialog = this;

        // Clear list items and add again
        clear();
        mSelectableAppList.clear();

        // Set the initial selection status
        for (int i = 0; i < numApps; ++i) {
            ApplicationInfo appInfo = mAllAppsPage.getAppsListItem(i);
            mSelectableAppList.add(appInfo);
            add(appInfo);
            boolean checked = mAllAppsPage.groupAppsListContains(appInfo);
            int idx = mSelectableAppList.indexOf(appInfo);
            mWasSelected.set(idx, checked);
            if (!mRestoring) {
                mListView.setItemChecked(idx, checked);
            }
        }
        mRestoring = false;

        // Make it so
        mAllAppsPage.showDialog(mDialog);
        //Added by e13775 July30 2012 for SWITCHUI-2477 start
        if (mOkButton != null)  
            mOkButton.setEnabled(true);
        //Added by e13775 July30 2012 for SWITCHUI-2477 end

        // log the event
        //HomeCheckin.logMeanEventSelectAppDialog();

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView =  mAllAppsPage.mInflater.inflate(R.layout.all_apps_dialog_select_apps_item, parent, false);
        }

        final Resources res = mAllAppsPage.getContext().getResources();
        ApplicationInfo appInfo = mSelectableAppList.get(position);
        CheckedTextView textView = (CheckedTextView) convertView;
        textView.setText(appInfo.title);
        textView.setCompoundDrawablesWithIntrinsicBounds(
                new BitmapDrawable(res, appInfo.iconBitmap), null, null, null);
        return textView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // This is here to force the audible click sound when a checkbox list item is tapped.
        // We don't actually have to do anything.  The code in View.performClick simply
        // checks for the existence of an OnItemClickListener, and if there is one, then
        // it calls playSoundEffect function.
    }

    /** Called from dialog when the user presses OK. */
    public void onClick(View v) {
        if (v.getId() ==  R.id.all_apps_dialog_select_apps_ok) {
            //Added by e13775 July30 2012 for SWITCHUI-2477
            //To avoid repeated submit in a short time 
            v.setEnabled(false);
            //Added by e13775 July30 2012 for SWITCHUI-2477
            // Add or remove selected apps as requested
            final long groupId = mAllAppsPage.getCurrentGroupItem().getId();
            final int numApps = mSelectableAppList.size();
            for (int i = 0; i < numApps; ++i) {
                boolean was = mWasSelected.get(i);
                boolean now = mListView.isItemChecked(i);
                if (!was && now) {
                    mAllAppsPage.mAppsModel.addToGroup(mSelectableAppList.get(i), groupId);

                    // log the event
                    //HomeCheckin.logMeanEventAppAddedToGroup(mAllAppsPage.getCurrentGroupItem().getName(getContext()), mSelectableAppList.get(i));
                } else if (was && !now) {
                    mAllAppsPage.mAppsModel.removeFromGroup(mSelectableAppList.get(i), groupId);
                }
            }
        }
        // A group was created, refresh the page
        mAllAppsPage.notifyGroupChanged();
        mDialog.dismiss();
    }

    @Override
    public void saveState(Bundle state) {
        ArrayList<Integer> positions = new ArrayList<Integer>();
        final int numApps = mSelectableAppList.size();
        for (int position = 0; position < numApps; ++position) {
            if (mListView.isItemChecked(position)) {
                positions.add(position);
            }
        }
        state.putInt(SAVED_STATE_APP_COUNT, numApps);
        state.putIntegerArrayList(SAVED_STATE_SELECTED_APPS, positions);
    }

    @Override
    public void restoreState(Bundle state) {
        // First do a sanity check with the number of apps in the list
        int oldCount = state.getInt(SAVED_STATE_APP_COUNT);
        int newCount = mSelectableAppList.size();
        if (oldCount != newCount) {
            return;
        }

        // Restore the checked items that were saved
        mListView.clearChoices();
        ArrayList<Integer> positions = state.getIntegerArrayList(SAVED_STATE_SELECTED_APPS);
        if (positions != null) {
            int N = positions.size();
            for (int i = 0; i < N; ++i) {
                int position = positions.get(i);
                mListView.setItemChecked(position, true);
            }
            mRestoring = true;
        }
    }

    @Override
    public void dismiss() {}

}
