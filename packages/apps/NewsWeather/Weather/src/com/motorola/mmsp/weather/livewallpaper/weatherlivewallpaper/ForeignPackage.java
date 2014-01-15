package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import java.lang.reflect.Field;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.provider.SyncStateContract.Constants;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class ForeignPackage {
    public final static String TAG = "LiveWallpaperService";

    private String packageName;
    private Context foreignCtx;

    public ForeignPackage(Context ctx, String packageName) throws NameNotFoundException{
        this.packageName = packageName;
        foreignCtx = ctx.createPackageContext(packageName, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        Log.d(TAG,"ForeignPackage packageName = "+packageName);
    }

    public Context getContext(){
        return foreignCtx;
    }

    public AssetManager getAssets(){
        return foreignCtx.getAssets();
    }

    public Class<?> loadClass(String className) throws ClassNotFoundException{
        if( foreignCtx == null )
            Log.d(TAG, "ForeignPackage foreignCtx is null");
        ClassLoader c = foreignCtx.getClassLoader();
        if( c == null )
            Log.d(TAG, "ForeignPackage ClassLoader is null");
        else
            Log.d(TAG, "ForeignPackage ClassLoader is "+c);
        return foreignCtx.getClassLoader().loadClass(className);
    }

    public int getResourceID(String sID){
        int mID = -1;
        if(sID.indexOf('.') == -1 || sID.indexOf('.') == sID.length() - 1){
            return -1;
        }

        //String className = "com.apktest.R$attr";
        String className = packageName + "." + sID.substring(0, sID.lastIndexOf('.')).replace('.', '$');
        String idName = sID.substring(sID.lastIndexOf('.') + 1);
        Log.d(TAG, "ForeignPackage packageName = " + packageName);
        Log.d(TAG, "ForeignPackage className = " + className);
        Log.d(TAG, "ForeignPackage idName = " + idName);
        try {
            Class<?> c = loadClass(className);
            Log.d(TAG,"ForeignPackage load Class successflly");
            Field field = c.getField(idName);
            Log.d(TAG,"ForeignPackage get Field successflly");
            mID = field.getInt(null);
            Log.d(TAG,"ForeignPackage get ID successflly");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ForeignPackage className Not Found:" + className);
            return -1;
        } catch (SecurityException e) {
            Log.e(TAG, "ForeignPackage SecurityException:" + sID);
            return -1;
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "ForeignPackage The Field Not Found:" + idName);
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "ForeignPackage Exception:" + e);
            return -1;
        } 

        return mID;
    }

    public String getString(String id){
        int mID = getResourceID(id);
        if(mID == -1){
            return null;
        }

        String s = null;
        try {
            s = foreignCtx.getResources().getString(mID);
        } catch (Exception e) {
            Log.e(TAG, "ForeignPackage getString Exception:" + e);
        }

        return s;
    }

    public int getColor(String id){
        int mID = getResourceID(id);
        if(mID == -1){
            return -1;
        }

        int color = -1;
        try {
            color = foreignCtx.getResources().getColor(mID);
        } catch (Exception e) {
            Log.e(TAG, "ForeignPackage getColor Exception:" + e);
        }

        return color;
    }

    public Drawable getDrawable(String id){
        int mID = getResourceID(id);
        if(mID == -1){
            return null;
        }

        Drawable d = null;
        try {
            d = foreignCtx.getResources().getDrawable(mID);
        } catch (Exception e) {
            Log.e(TAG, "ForeignPackage getDrawable Exception:" + e);
        }

        return d;
    }

    public View getLayout(String id){
        int mID = getResourceID(id);
        if(mID == -1){
            return null;
        }

        LayoutInflater inflate  = (LayoutInflater)
        foreignCtx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflate.inflate(mID, null);
    }

    public Animation getAnimation(String id){
        int mID = getResourceID(id);
        if(mID == -1){
            return null;
        }

        Animation a = null;
        try {
            a = AnimationUtils.loadAnimation(foreignCtx, mID);
        } catch (Exception e) {
            Log.e(TAG, "ForeignPackage getAnimation Exception:" + e);
        }

        return a;
    }
}
