package com.motorola.mmsp.activitygraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.res.Configuration;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

public class SingleApplicationPicker extends Activity 
implements ActivityGraphModel.Callbacks {
    
    private ActivityGraphModel mModel;
    private List<AppInfo> mAppList;
    private SearchPickerAdapter mAdapter;
    SearchPickerLayout mSearchPickerLayout;
    
    private int mShortcutButtonId;
    
    private static final String TAG = "SingleApplicationPicker";
    
    SearchPickerLayout.Callbacks mCallback = new SearchPickerLayout.Callbacks () {
        public void buttonOkClick(){
            
        }
        public void buttonCancelClick(){
            
        }
        public void itemClick(int position){
            AppInfo currentItem = mAdapter.getItem(position);
            
            Intent intent = new Intent();
            Bundle data=new Bundle();
            data.putInt("shortcut_index", mShortcutButtonId);
            data.putString("app_componentname", currentItem.componentname);
            intent.putExtras(data);
            setResult(RESULT_OK, intent);
            EditText textIn = (EditText)findViewById(R.id.searchEdit);
            InputMethodManager inputMethodManager =
                    (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(textIn.getWindowToken(), 0);
            finish();
            
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.searchpicker_layout);
        
        ActivityGraphApplication app = ((ActivityGraphApplication)getApplication());        
        mModel = app.getModel();        
        mModel.addCallback(this);
        
        Log.i(TAG, "SearchPickerLayout instance created");
        mSearchPickerLayout = (SearchPickerLayout)findViewById(R.id.searchpicker);
        mSearchPickerLayout.setMultiCheck(false);
        mSearchPickerLayout.setButtonExist(false);
        
        mSearchPickerLayout.setCallbacks(mCallback);
        
        final Intent intent = getIntent();
        Bundle data=intent.getExtras();
        mShortcutButtonId = data.getInt("shortcut_id");
        
        PackageManager mPm;
        mPm = getPackageManager();
        mAppList = new ArrayList<AppInfo>();
        getAppInfo();
        
        mAdapter = new SearchPickerAdapter (this, R.layout.app_info_item, R.id.appname, (ArrayList<AppInfo>)mAppList);
        mSearchPickerLayout.setAdapter(mAdapter);
        
        Collections.sort( mAppList, new AppNameComparator() );
        
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {		
        Log.v(TAG, "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
    }
    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        EditText textIn = (EditText)findViewById(R.id.searchEdit);
        textIn.setText(""); 
        textIn.clearFocus();
        mAdapter.updateList((ArrayList<AppInfo>)mAppList);
        Collections.sort( mAppList, new AppNameComparator() );
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.gc();
    }
    
    private void getAppInfo(){
        
        List<ResolveInfo> apps = null;
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        apps =  getPackageManager().queryIntentActivities(mainIntent, 0);
        int N = apps.size();
        
        AppInfo tempAppInfo = new AppInfo();
        tempAppInfo.checked = false;
        tempAppInfo.checkvisibility = false;
        tempAppInfo.appname = getString(R.string.none_application);
        tempAppInfo.componentname = getString(R.string.none_application);
        tempAppInfo.appicon = null;
        mAppList.add(tempAppInfo);
        
        PackageManager mPm = getPackageManager();	
        
        for (int i = 0; i < N; i++) 
        {
            tempAppInfo = new AppInfo();
            tempAppInfo.componentname = new ComponentName(
                    apps.get(i).activityInfo.applicationInfo.packageName,
                    apps.get(i).activityInfo.name).flattenToShortString();
            
            tempAppInfo.checked = false;
            tempAppInfo.checkvisibility = false;
            
            tempAppInfo.appname = apps.get(i).loadLabel(mPm).toString();
            tempAppInfo.appicon = apps.get(i).loadIcon(mPm);
            mAppList.add(tempAppInfo);
            Log.d(TAG,"add");
        }  
    }
    
    public void bindAppsAdded(ArrayList<String> list) {
        Log.d(TAG,"bindAppsAdded");	
        
        AppLabelIconInfo info;
        String componentname;
        for (int i = 0; i <list.size(); i++){
            
            AppInfo tempAppInfo = new AppInfo();
            tempAppInfo.checked = false;
            tempAppInfo.entrychecked = false;
            tempAppInfo.checkvisibility = false;
            tempAppInfo.componentname = list.get(i);
            componentname = tempAppInfo.componentname;
            Log.d(TAG,componentname);
            info = mModel.getLabelIconInfo(componentname);
            if(info!=null){
                tempAppInfo.appname = info.appname;	    	  
                tempAppInfo.appicon = mModel.getLabelIconInfo(tempAppInfo.componentname).appicon;
                mAppList.add(tempAppInfo);
            }
        }
        SingleApplicationPicker.this.runOnUiThread(new Runnable(){ 
            public void run() {  
                mAdapter.updateList();
            }  
        });
    }
    public void bindAppsDeleted(ArrayList<String> list) {
        Log.d(TAG,"bindAppsDeleted");	
        
        for (int j = 0; j <list.size(); j++)
            for (int i = 0; i < mAppList.size(); i++) 
            {
                if (mAppList.get(i).componentname.equals(list.get(j))) {
                    mAppList.remove(i);
                    break;
                }
            }	
        SingleApplicationPicker.this.runOnUiThread(new Runnable(){ 
            public void run() {  
                mAdapter.updateList();
            }  
        });
    }
    public void bindAppsUpdated(ArrayList<String> list) {
        int N = mAppList.size();
        AppLabelIconInfo info;
        for (int j = 0; j <list.size(); j++)
            for (int i = 0; i < N; i++){
                if (mAppList.get(i).componentname.equals(list.get(j))) {
                    
                    info = mModel.getLabelIconInfo(mAppList.get(i).componentname);
                    if(info!=null){
                        mAppList.get(i).appname = info.appname;	    	  
                        mAppList.get(i).appicon = info.appicon;
                    }
                    break;
                }
            }
        SingleApplicationPicker.this.runOnUiThread(new Runnable(){ 
            public void run() {  
                mAdapter.updateList();
            }  
        });
    }
    
}
