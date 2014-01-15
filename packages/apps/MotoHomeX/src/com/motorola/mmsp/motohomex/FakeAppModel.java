/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
*Create by ncqp34 at Mar-20-2012 for fake app 
*
*/

package com.motorola.mmsp.motohomex;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import java.util.ArrayList;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import java.util.Collections;
import java.util.HashMap;
import java.io.File;

public class FakeAppModel {

    private final static String TAG = "FakeAppModel";
    private final static int TO_HIDE = 1 ;
    private final static int TO_SHOW = 0 ;
    private final static int IS_HIDDEN = 1 ;
    private final static int IS_SHOWING = 0 ;
    
    private ArrayList<ApplicationInfo> mFakeAppList = new ArrayList<ApplicationInfo> ();
    public final HashMap<ComponentName, Bitmap> mBitmapCache = new HashMap<ComponentName, Bitmap>();
    private Context mContext;

    public static String EXTRA_COMPONENT = "fake_app_component";

    public FakeAppModel(Context context){
	mContext = context;
    }

    public ArrayList<ApplicationInfo> getAppList(Context context,  ArrayList<ApplicationInfo> list){
	if(mFakeAppList == null ) return null;
	for(int i =0; i < mFakeAppList.size() ; i++){
	    ApplicationInfo info = mFakeAppList.get(i);
	    Log.d(TAG,"getAppList---i =" + i + ",info =" + info  + " ,info.componentName =" + info.componentName);
	    for(int j =0; j < list.size() ; j++){
	        Log.d(TAG,"getAppList---AllAppList---j =" + j + ",info =" + list.get(j) + 
			" ,info.componentName =" + list.get(j).componentName);
		if(info.componentName.equals(list.get(j).componentName)){
		    Log.d(TAG,"getAppList--exist");
		    mFakeAppList.remove(i);
		    hideItem(context, info.componentName);
		    break;
		}
	    }
	}
	return mFakeAppList;
    }

    public void getFakeAppList(Context context){
	/*Added by ncqp34 at Apr-13-2012 for switchui-558*/
	if(context == null)  return;
	/*ended by ncqp34*/

	final String sWhereHidden = FakeAppProvider.FakeApplication.HIDDEN + "=?";
	final String [] sTempArg = new String[] {"0"};

    	final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(FakeAppProvider.FakeApplication.CONTENT_URI,
             FakeAppProvider.FakeApplication.PROJECTION, sWhereHidden, sTempArg, FakeAppProvider.FakeApplication._ID);

        try {
	    while (c != null && c.moveToNext()) {

	     String comp = c.getString(FakeAppProvider.FakeApplication.INDEX_COMPONENT);	
	     String title = c.getString(FakeAppProvider.FakeApplication.INDEX_TITLE);
	     String iconPath = c.getString(FakeAppProvider.FakeApplication.INDEX_ICON);	
	     String uri = c.getString(FakeAppProvider.FakeApplication.INDEX_URI);
	     
	     Log.d(TAG,"FakeApp info :compnentName =="  + comp + ", iconPath==" + iconPath +  
		",title ==" +title +  ",uri ==" + uri);
             Bitmap bmp = null;
	     try{
	      	bmp = BitmapFactory.decodeFile(iconPath);
	        Log.d(TAG,"FakeApp--icon size:width ==" + bmp.getWidth() + " , height ==" + bmp.getHeight());
	     } catch(Exception e){
		Log.w(TAG,"FakeApp--get flex icon error");	
		//Drawable d = resources.getDrawableForDensity(iconId, mIconDpi);
	     }
	     ComponentName cn = ComponentName.unflattenFromString(comp);
	     Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(uri));
	     Log.d(TAG,"FakeApp info :cn ==" + cn);
	     ApplicationInfo fakeAppInfo = new ApplicationInfo(cn, intent, bmp, title );
	     mFakeAppList.add(fakeAppInfo);	
	    }	
        } catch (Exception e) {
           Log.w(TAG,"Error query component index:e ==" + e);
	} finally {
            c.close();
        }

	Log.d(TAG,"FakeApp info---mFakeAppList ==" + mFakeAppList );
    }

    public boolean isShowingFakeApp(Context context, ComponentName cn){
	return IS_SHOWING == getShowState(context,cn);
    }

    public boolean isHiddenFakeApp(Context context, ComponentName cn){
	return IS_HIDDEN == getShowState(context,cn);
    }

    private int getShowState(Context context, ComponentName cn){

        final ContentResolver cr = context.getContentResolver();
        final ContentValues values = new ContentValues();
	Log.d(TAG,"componentName ==" + cn.flattenToString());

        Cursor c = cr.query(FakeAppProvider.FakeApplication.CONTENT_URI,
            new String[] { FakeAppProvider.FakeApplication.HIDDEN, FakeAppProvider.FakeApplication.COMPONENT_NAME },
	    "componentName=?",new String[] { cn.flattenToString()}, null);

	Log.d(TAG,"componentName ==" + cn.flattenToString());

        int result = -1;
        try {
	    while (c != null && c.moveToNext()) {	
		result = c.getInt(0);
		Log.d(TAG,"Hidden state ==" + result);
		break;	
	    }	
        } catch (Exception e) {
           Log.w(TAG,"Error query component index");
	}finally {
            c.close();
        }
        return result;
    }

    private ApplicationInfo getFakeAppInfo(Context context, ComponentName cn){
	final String sWhereComponent = FakeAppProvider.FakeApplication.COMPONENT_NAME + "=?";
	final String [] sTempArg = new String[] {cn.flattenToString()};
	ApplicationInfo fakeAppInfo = new ApplicationInfo();
    	final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(FakeAppProvider.FakeApplication.CONTENT_URI,
             FakeAppProvider.FakeApplication.PROJECTION, sWhereComponent, sTempArg, FakeAppProvider.FakeApplication._ID);

        try {
	    while (c != null && c.moveToNext()) {

	     String comp = c.getString(FakeAppProvider.FakeApplication.INDEX_COMPONENT);	
	     String title = c.getString(FakeAppProvider.FakeApplication.INDEX_TITLE);
	     String iconPath = c.getString(FakeAppProvider.FakeApplication.INDEX_ICON);	
	     String uri = c.getString(FakeAppProvider.FakeApplication.INDEX_URI);
	     
	     Log.d(TAG,"getFakeAppInfo---info :compnentName =="  + comp + ", iconPath==" + iconPath +  
		",title ==" +title +  ",uri ==" + uri);
             Bitmap bmp = null;
	     try{
	      	bmp = BitmapFactory.decodeFile(iconPath);
	        Log.d(TAG,"FakeApp--icon size:width ==" + bmp.getWidth() + " , height ==" + bmp.getHeight());
	     } catch(Exception e){
		Log.w(TAG,"FakeApp--get flex icon error");	
		//Drawable d = resources.getDrawableForDensity(iconId, mIconDpi);
	     }
	     ComponentName compName = ComponentName.unflattenFromString(comp);
	     Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(uri));
	     intent.putExtra(FakeAppModel.EXTRA_COMPONENT, cn.flattenToString());

	     fakeAppInfo = new ApplicationInfo(compName, intent, bmp, title );
	     break;	
	    }	
        } catch (Exception e) {
           Log.w(TAG,"Error query component index");
	}finally {
            c.close();
        }

	return fakeAppInfo;
    }

    public void addFakeToApplist(Context context, ArrayList<ApplicationInfo> installlist,
			ArrayList<ApplicationInfo> allAppList){
        int count = installlist.size();
        for (int i = 0; i < count; ++i) {
            ApplicationInfo info = installlist.get(i);
	    Log.d(TAG,"addFakeToApplist ---i ==" + i  + ",getComponent==" + info.intent.getComponent());

	    if(!isHiddenFakeApp(context, info.intent.getComponent())){
		continue;
	    }
	    /*Added by ncqp34 at Jan-06-2012 for menuOrder flex update*/
            //int index = Collections.binarySearch(mApps, info, LauncherModel.APP_NAME_COMPARATOR);
	    ApplicationInfo fakeInfo = getFakeAppInfo(context, info.intent.getComponent());
            int index = Collections.binarySearch(allAppList, fakeInfo, Flex.getMenuOrderComparator(context));
	    Log.d(TAG,"addFakeToApplist ---index ==" + index  + ",getComponent==" + info.intent.getComponent());
            /*ended by ncqp34*/
            if (index < 0) {
                allAppList.add(-(index + 1), fakeInfo);
		showItem(context, info.intent.getComponent());
            }
        }
    }

    public void removeFakeFromApplist(Context context, ArrayList<ApplicationInfo> uninstalllist,
			ArrayList<ApplicationInfo> allAppList){
        // loop through all the apps and remove apps that have the same component
	Log.d(TAG," removeFakeFromApplist---uninstalllist =="+ uninstalllist);
        int length = uninstalllist.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = uninstalllist.get(i);
            int removeIndex = findAppByComponent(allAppList, info);
            if (removeIndex > -1) {
                allAppList.remove(removeIndex);
		hideItem(context, info.componentName);
            }
        }
    }

    private int findAppByComponent(ArrayList<ApplicationInfo> list, ApplicationInfo item) {
        ComponentName removeComponent = item.intent.getComponent();
	Log.d(TAG," findAppByComponent--- removeComponent=="+ removeComponent);
        int length = list.size();
        for (int i = 0; i < length; ++i) {
            ApplicationInfo info = list.get(i);
		Log.d(TAG," findAppByComponent---info.componentName =="+ info.componentName );
            if (info.componentName.getPackageName().equals(removeComponent.getPackageName())) {
		Log.d(TAG," findAppByComponent--- i=="+ i );
                return i;
            }
        }
        return -1;
    }

    public void hideItem(Context context, ComponentName cn){
	updateItemHiddenState(context, cn, TO_HIDE);
    }

    public void showItem(Context context, ComponentName cn){
	updateItemHiddenState(context, cn, TO_SHOW);
    }

    private void updateItemHiddenState(Context context, ComponentName cn, int change){

        final ContentResolver cr = context.getContentResolver();
        final ContentValues values = new ContentValues();
        values.put(FakeAppProvider.FakeApplication.HIDDEN, change);
	final String sWhereComponent = FakeAppProvider.FakeApplication.COMPONENT_NAME + "=?";
	final String [] sTempArg = new String[] {cn.flattenToString()};
	Log.d(TAG,"updateItemHiddenState --- sTempArg==" + sTempArg + ",ComponentName ==" + cn.flattenToString());
        cr.update(FakeAppProvider.FakeApplication.CONTENT_URI, values, sWhereComponent, sTempArg);
    }

    public static void addItemToDatabase(Context context, final ContentValues values, final boolean notify){
	Log.d(TAG,"addItemToDatabase---context ==" + context + ",values==" + values);
	Log.d(TAG,"URI=" + FakeAppProvider.FakeApplication.CONTENT_URI );
        final ContentResolver cr = context.getContentResolver();
        cr.insert(FakeAppProvider.FakeApplication.CONTENT_URI, values);
    }

}
