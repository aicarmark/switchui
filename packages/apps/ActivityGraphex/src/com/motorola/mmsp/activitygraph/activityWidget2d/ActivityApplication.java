package com.motorola.mmsp.activitygraph.activityWidget2d;

import android.app.Application;
import android.util.Log;
/**
public class ActivityApplication{

    private static ActivityProvider mProvider;
    private static ActivityModel mModel;
    private static ActivityApplication sMe;

//    public void onCreate()
//    {
//        super.onCreate();
//        mModel=new ActivityModel(getApplicationContext());
//        sMe=this;
//        
//    }
    
    private  ActivityApplication() {
        mModel = new ActivityModel(null);
    }
    
    public ActivityProvider getProvider()
    {
        if(mProvider==null)
        {
            Log.i("AcitivityApplication+","mProvider is null");
//            mProvider=ActivityProvider.createProviderInstance(this.getApplicationContext());
        }
        return mProvider;
    }
    
    public void setProviderInstance(ActivityProvider provider)
    {
        mProvider=provider;
    }
    public static ActivityApplication getApplicationInstance()
    {
        if(sMe==null)
        {
            Log.i("ActivityApplication+","sMe is null");
        }
        return sMe;
    }
    
    public ActivityModel getModel()
    {
        if(mModel==null)
        {
//            mModel=new ActivityModel(getApplicationContext());
        }
        return mModel;
    }

}
*/