package com.motorola.mmsp.activitygraph;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import java.util.ArrayList;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import java.util.List;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import android.content.res.Configuration;
import android.widget.EditText;
import android.view.inputmethod.InputMethodManager;

public class AppsHidePicker extends Activity implements
        ActivityGraphModel.Callbacks {

    private List<AppInfo> mAppList;
    private SearchPickerAdapter mAdapter;
    private SearchPickerLayout mSearchPickerLayout;
    private ActivityGraphModel mModel;
    // private final static String[] mBlackApps = {
    // "com.android.contacts/.activities.DialtactsActivity",
    // "com.android.contacts/.activities.PeopleActivity"
    // };
    private static final String TAG = "AppsHidePicker";
    private ArrayList<AppInfo> mChangeList = new ArrayList<AppInfo>();
    private Boolean bOkClicked = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        ActivityGraphApplication app = ((ActivityGraphApplication) getApplication());
        mModel = app.getModel();
        mModel.addCallback(this);

        Bundle data = this.getIntent().getExtras();
        ArrayList<String> componentsHidden = data
                .getStringArrayList("Key_HiddenComponents");
        ArrayList<String> componentsToUnhide = data
                .getStringArrayList("Key_ComponentsToUnhide");

        mAppList = new ArrayList<AppInfo>();

        PackageManager mPm;
        mPm = getPackageManager();

        getAppInfo(componentsHidden, componentsToUnhide);

        setContentView(R.layout.searchpicker_layout);
        Log.i(TAG, "SearchPickerLayout instance created");
        mSearchPickerLayout = (SearchPickerLayout) findViewById(R.id.searchpicker);
        mSearchPickerLayout.setMultiCheck(true);
        mSearchPickerLayout.setButtonExist(true);
        mSearchPickerLayout.setCallbacks(mCallback);

        mAdapter = new SearchPickerAdapter(this, R.layout.app_info_item,
                R.id.appname, (ArrayList<AppInfo>) mAppList);
        mSearchPickerLayout.setAdapter(mAdapter);

        Collections.sort(mAppList, new AppNameComparator());
    }

    private void getAppInfo(ArrayList<String> hiddenApps,
            ArrayList<String> toUnhideApps) {

        List<ResolveInfo> apps = null;
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager mPm;
        mPm = getPackageManager();

        apps = mPm.queryIntentActivities(mainIntent, 0);

        int N = apps.size();

        AppInfo tempAppInfo;

        for (int i = 0; i < N; i++) {
            tempAppInfo = new AppInfo();

            tempAppInfo.componentname = new ComponentName(
                    apps.get(i).activityInfo.applicationInfo.packageName,
                    apps.get(i).activityInfo.name).flattenToShortString();

            // if
            // ((tempAppInfo.componentname.equals(mBlackApps[0]))||(tempAppInfo.componentname.equals(mBlackApps[1]))){
            // continue;
            // }

            tempAppInfo.checkvisibility = true;
            tempAppInfo.appname = apps.get(i).loadLabel(mPm).toString();
            tempAppInfo.appicon = apps.get(i).loadIcon(mPm);

            tempAppInfo.checked = false;
            tempAppInfo.entrychecked = false;
            if (hiddenApps != null) {

                for (int j = 0; j < hiddenApps.size(); j++) {
                    if (tempAppInfo.componentname.equals(hiddenApps.get(j))) {
                        tempAppInfo.checked = true;
                        tempAppInfo.entrychecked = true;
                        break;
                    }
                }
            }
            if (toUnhideApps != null) {
                for (int j = 0; j < toUnhideApps.size(); j++) {
                    if (tempAppInfo.componentname.equals(toUnhideApps.get(j))) {
                        mChangeList.add(tempAppInfo);
                        tempAppInfo.checked = false;
                        break;
                    }
                }
            }

            mAppList.add(tempAppInfo);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EditText textIn = (EditText) findViewById(R.id.searchEdit);
        textIn.setText("");
        textIn.clearFocus();
        mAdapter.updateList((ArrayList<AppInfo>) mAppList);
        Collections.sort(mAppList, new AppNameComparator());
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        System.gc();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.v(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }

    SearchPickerLayout.Callbacks mCallback = new SearchPickerLayout.Callbacks() {
        public void buttonOkClick() {
            Log.d(TAG, "buttonOkClick");
            if (!bOkClicked) {
                bOkClicked = true;
                int count = mChangeList.size();
                AppInfo item;
                boolean bChanged = false;

                for (int i = 0; i < count; i++) {
                    item = mChangeList.get(i);
                    if (item.entrychecked != item.checked) {
                        bChanged = true;
                        if (!item.checked) {
                            ActivityGraphModel.removeHiddenItemFromDatabase(
                                    AppsHidePicker.this, item.componentname);
                        } else {
                            ActivityGraphModel.addHiddenItemToDatabase(
                                    AppsHidePicker.this, item.componentname);
                        }
                    }
                }

                if (bChanged) {
                    Log.d(TAG, "send broadcast update hidden");
                    Intent newIntent = new Intent(
                            "com.motorola.mmsp.activitygraph.UPDATE_HIDDEN");
                    sendBroadcast(newIntent);
                }
                hideInputMethod();
                finish();
            }
        }

        public void buttonCancelClick() {
            hideInputMethod();
            finish();
        }

        public void itemClick(int position) {
            AppInfo currentItem = mAdapter.getItem(position);
            currentItem.checked = !currentItem.checked;
            if (!mChangeList.contains(currentItem)) {
                mChangeList.add(currentItem);
            }
            mAdapter.updateList();
            hideInputMethod();
        }
    };

    public void bindAppsAdded(ArrayList<String> list) {
        Log.i(TAG, "bindAppsAdded");
        for (int i = 0; i < list.size(); i++) {

            AppInfo tempAppInfo = new AppInfo();
            tempAppInfo.checked = false;
            tempAppInfo.entrychecked = false;
            tempAppInfo.checkvisibility = true;
            tempAppInfo.componentname = list.get(i);
            String componentname = tempAppInfo.componentname;
            Log.d(TAG, componentname);
            AppLabelIconInfo info = mModel.getLabelIconInfo(componentname);
            if (info != null) {
                tempAppInfo.appname = info.appname;
                tempAppInfo.appicon = mModel
                        .getLabelIconInfo(tempAppInfo.componentname).appicon;
                mAppList.add(tempAppInfo);
            }
        }
        AppsHidePicker.this.runOnUiThread(new Runnable() {
            public void run() {
                mAdapter.updateList();
            }
        });
    }

    public void bindAppsDeleted(ArrayList<String> list) {

        Log.d(TAG, "bindAppsDeleted:" + list.get(0));

        for (int j = 0; j < list.size(); j++)
            for (int i = 0; i < mAppList.size(); i++) {
                if (mAppList.get(i).componentname.equals(list.get(j))) {
                    Log.d(TAG, "remove app:" + i);
                    mAppList.remove(i);
                }
            }
        AppsHidePicker.this.runOnUiThread(new Runnable() {
            public void run() {
                mAdapter.updateList();
            }
        });
    }

    public void bindAppsUpdated(ArrayList<String> list) {
        int N = mAppList.size();
        for (int i = 0; i < N; i++)
            for (int j = 0; j < list.size(); j++) {
                if (mAppList.get(i).componentname.equals(list.get(j))) {

                    AppLabelIconInfo info = mModel.getLabelIconInfo(mAppList
                            .get(i).componentname);
                    if (info != null) {
                        mAppList.get(i).appname = info.appname;
                        mAppList.get(i).appicon = info.appicon;
                    }
                    break;
                }
            }
        AppsHidePicker.this.runOnUiThread(new Runnable() {
            public void run() {
                mAdapter.updateList();
            }
        });
    }

    public void hideInputMethod() {
        EditText textIn = (EditText) findViewById(R.id.searchEdit);
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(textIn.getWindowToken(), 0);
    }
}
