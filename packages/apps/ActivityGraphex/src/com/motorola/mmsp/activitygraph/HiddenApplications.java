package com.motorola.mmsp.activitygraph;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextWatcher;
import android.widget.EditText;
import android.view.View;
import android.widget.Button;
import android.content.res.Configuration;
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
import com.motorola.mmsp.activitygraph.R;
import com.motorola.mmsp.activitygraph.R.*;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

public class HiddenApplications extends Activity implements
        ActivityGraphModel.Callbacks, View.OnClickListener {
    private List<AppInfo> mAppList;
    private SearchPickerAdapter mAdapter;
    // private SearchPickerLayout mSearchPickerLayout;
    private ActivityGraphModel mModel;
    private Button mButtonAdd;
    private ArrayList<String> mUnhideList = new ArrayList<String>();
    private ArrayList<String> mHiddenList = new ArrayList<String>();
    private ArrayList<AppInfo> mChangeList = new ArrayList<AppInfo>();
    private static final String TAG = "HiddenApplications";
    SearchPickerLayout mSearchPickerLayout;
    private Boolean bOkClicked = false;
    private Boolean bResumeAfterCreate = false;

    // protected Context mContext;
    // private final static String[] mBlackApps = {
    // "com.android.contacts/.activities.DialtactsActivity",
    // "com.android.contacts/.activities.PeopleActivity"
    // };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityGraphApplication app = ((ActivityGraphApplication) getApplication());
        mModel = app.getModel();
        mModel.addCallback(this);

        mAppList = new ArrayList<AppInfo>();
        mUnhideList.clear();
        PackageManager mPm = getPackageManager();

        setContentView(R.layout.hidden_applications);

        mSearchPickerLayout = (SearchPickerLayout) findViewById(R.id.searchpicker);
        Log.i(TAG, "SearchPickerLayout instance created in hidden");

        mSearchPickerLayout.setMultiCheck(true);
        mSearchPickerLayout.setButtonExist(true);
        mSearchPickerLayout.setCallbacks(mCallback);

        mAdapter = new SearchPickerAdapter(this, R.layout.app_info_item,
                R.id.appname, (ArrayList<AppInfo>) mAppList);
        mSearchPickerLayout.setAdapter(mAdapter);

        mButtonAdd = (Button) findViewById(R.id.btn_select_to_hide);
        mButtonAdd.setOnClickListener(this);
        // add by gxl ----- begin
        mButtonAdd.setClickable(false);
        // add by gxl ----- end
        bResumeAfterCreate = true;
        // Collections.sort( mAppList, new AppNameComparator() );
    }

    public void onClick(View v) {
        CheckUnhideList();
        Intent intent = new Intent();
        Bundle data = new Bundle();
        data.putStringArrayList("Key_HiddenComponents", mHiddenList);
        data.putStringArrayList("Key_ComponentsToUnhide", mUnhideList);
        intent.putExtras(data);
        intent.setClass(HiddenApplications.this, AppsHidePicker.class);
        startActivity(intent);

        mUnhideList.clear();
        mChangeList.clear();
    }

    private void CheckUnhideList() {
        mUnhideList.clear();
        AppInfo Item;
        for (int i = 0; i < mAdapter.getCount(); i++) {
            Item = mAdapter.getItem(i);
            if (Item.checked) {
                continue;
            }
            mUnhideList.add(Item.componentname);
        }
        Log.d(TAG, "UnhideList size:" + mUnhideList.size());

    }

    private void getAppInfoFromComponentName(
            ArrayList<String> hiddencomponentnames) {
        Log.d(TAG, "getAppInfoFromComponentName()");
        int N = hiddencomponentnames.size();
        mAppList.clear();
        String componentname;
        AppLabelIconInfo info;

        for (int i = 0; i < N; i++) {
            AppInfo tempAppInfo = new AppInfo();

            tempAppInfo.componentname = hiddencomponentnames.get(i);

            // if
            // ((tempAppInfo.componentname.equals(mBlackApps[0]))||(tempAppInfo.componentname.equals(mBlackApps[1]))){
            // continue;
            // }
            tempAppInfo.checked = true;
            Log.d(TAG, "mUnhideList.size() = " + mUnhideList.size());
            for (int j = 0; j < mUnhideList.size(); j++) {
                if (tempAppInfo.componentname.equals(mUnhideList.get(j))) {
                    tempAppInfo.checked = false;
                    break;
                }
            }
            tempAppInfo.entrychecked = true;
            tempAppInfo.checkvisibility = true;

            componentname = tempAppInfo.componentname;
            Log.d(TAG, componentname);
            info = mModel.getLabelIconInfo(componentname);
            if (info != null) {
                tempAppInfo.appname = info.appname;
                tempAppInfo.appicon = info.appicon;
                mAppList.add(tempAppInfo);
            }
        }

    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();

        /** 2012-07-22, jimmyli added: if no hidden app, re-launch hidden search activity directly */
        if (!ActivityGraphModel.hiddenAppExists()) {
            mHiddenList = ActivityGraphModel.getHiddenList();
            display();
            return;
        }
        /** jimmyli end */
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                mHiddenList = ActivityGraphModel.getHiddenList();
                long end = System.currentTimeMillis();
                Log.i(TAG, "----getHiddenList cost time: " + (end - start));
                mHandler.sendEmptyMessage(0);
            }
        }).start();
    }
    
    private Handler mHandler = new Handler() {
        
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            display();
        }
        
    };

    private void display() {
        // add by gxl ----- begin
        mButtonAdd.setClickable(true);
        // add by gxl ----- end

        if (mHiddenList.size() == 0) {
            if (bResumeAfterCreate) {
                Intent intent = new Intent(this, AppsHidePicker.class);
                Bundle data = new Bundle();
                data.putStringArrayList("Key_HiddenComponents", mHiddenList);
                data.putStringArrayList("Key_ComponentsToUnhide", mUnhideList);
                intent.putExtras(data);
                startActivity(intent);
            }
            finish();
        }
        EditText textIn = (EditText) findViewById(R.id.searchEdit);
        textIn.setText("");
        textIn.clearFocus();
        
        getAppInfoFromComponentName(mHiddenList);
        
        if (mAdapter != null) {
            mAdapter.updateList((ArrayList<AppInfo>) mAppList);
        }
        Collections.sort(mAppList, new AppNameComparator());
    }

    @Override
    protected void onPause() {
        super.onPause();
        bResumeAfterCreate = false;
    }

    @Override
    protected void onDestroy() {
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
            if (!bOkClicked) {
                bOkClicked = true;
                int count = mChangeList.size();
                AppInfo item;
                boolean bChanged = false;

                for (int i = 0; i < count; i++) {
                    item = mChangeList.get(i);
                    if ((item.entrychecked != item.checked) && (!item.checked)) {
                        bChanged = true;
                        Log.d(TAG, "remove:" + item.componentname);
                        ActivityGraphModel.removeHiddenItemFromDatabase(
                                HiddenApplications.this, item.componentname);
                    }
                }
                if (bChanged) {
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
            CheckUnhideList();
            mAdapter.updateList();
            hideInputMethod();

        }
    };

    public void bindAppsAdded(ArrayList<String> list) {

    }

    public void bindAppsDeleted(ArrayList<String> list) {

        Log.i(TAG, "bindAppsDeleted");
        // int N = mAppList.size();

        for (int j = 0; j < list.size(); j++)
            for (int i = 0; i < mAppList.size(); i++) {
                if (mAppList.get(i).componentname.equals(list.get(j))) {
                    mAppList.remove(i);
                    break;
                }
            }
        HiddenApplications.this.runOnUiThread(new Runnable() {
            public void run() {
                mAdapter.updateList();
            }
        });
    }

    public void bindAppsUpdated(ArrayList<String> list) {
        int N = mAppList.size();
        AppLabelIconInfo info;
        for (int i = 0; i < N; i++)
            for (int j = 0; j < list.size(); j++) {
                if (mAppList.get(i).componentname.equals(list.get(j))) {

                    info = mModel
                            .getLabelIconInfo(mAppList.get(i).componentname);
                    if (info != null) {
                        mAppList.get(i).appname = info.appname;
                        mAppList.get(i).appicon = info.appicon;
                    }
                    break;
                }
            }
        HiddenApplications.this.runOnUiThread(new Runnable() {
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
