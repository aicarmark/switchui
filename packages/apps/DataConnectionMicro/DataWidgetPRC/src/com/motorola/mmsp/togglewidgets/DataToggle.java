package com.motorola.mmsp.togglewidgets;


import java.util.Observable;
import java.util.Observer;

import com.motorola.mmsp.togglewidgets.BaseTogglerWidget.State;

import android.content.ComponentName;
import android.content.ContentQueryMap;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.util.Log;

public class DataToggle extends BaseTogglerWidget{

    //begin: IKDOMINO-1438, kbg374, add observer to monitor the change, 2011-07-25
    private static ContentQueryMap mContentQueryMap;
    private static Context mContext;
    private static Cursor mSettingsCursor = null;
    //end: IKDOMINO-1438, kbg374, add observer to monitor the change, 2011-07-25

    /**
     * This method returns the state the widget should be shown based on mobile
     * data state and availability.
     *
     * @param context the context.
     *
     * @return the widget state
     */
    protected State getWidgetState(Context context)
    {
        return (getServiceState(context));
    }

    @Override
    protected Availability getServiceAvailability(Context context) {
        // TODO Auto-generated method stub
        if (ConnectivityManager.isNetworkTypeValid(ConnectivityManager.TYPE_MOBILE))
        {
            myLog(LOG_TAG,"Data ServiceAvailability Available");
            return Availability.ENABLED_SERVICE;
        }
        else 
        {
            myLog(LOG_TAG,"Data ServiceAvailability unavailable");
            return Availability.DISABLED_SERVICE;
        }
    }

    @Override
    protected State getServiceState(Context context) {
        //begin: IKDOMINO-1541, kbg374, set mContext value, 2011-07-29
        myLog(LOG_TAG,"getServiceState");
        mContext= context;
        //end: IKDOMINO-1541, kbg374, set mContext value, 2011-07-29
        //begin: IKDOMINO-1438, kbg374, add observer to monitor the change, 2011-07-25
        if (mContentQueryMap == null || 0 == mContentQueryMap.countObservers()) {
            Log.d(TAG, "current query map is null ");
	        if (mSettingsCursor == null) {
	            mSettingsCursor = context.getContentResolver().query(Settings.Secure.CONTENT_URI, null,
	               "(" + Settings.System.NAME + "=?)",
	               new String[]{Settings.Secure.PREFERRED_NETWORK_MODE},
	               null);
	        }
	        mContentQueryMap = new ContentQueryMap(mSettingsCursor, Settings.System.NAME, true, null);
            Log.d(TAG, "on update add settings observer");
            mContentQueryMap.addObserver(new SettingsObserver());
        }
        //end: IKDOMINO-1438, kbg374, add observer to monitor the change, 2011-07-25
    	if (getAirPlaneMode(context))
    	{
    		return State.DISABLED_STATE;
    	}
    	
        ConnectivityManager connManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        State connState=State.DISABLED_STATE;
        if(connManager != null) {
            if(connManager.getMobileDataEnabled()){
                connState = State.ENABLED_STATE;
            }else{
                connState =  State.DISABLED_STATE;
            }                  
        }
        else {
            
            connState = State.DISABLED_STATE;
            myLog(LOG_TAG,"Data Service Not Available");
        }
        myLog(LOG_TAG,"getServiceState.".concat(connState.toString()));
        return connState;
    }

    @Override
    protected int getSleeveImageName(State currentState) {
        // TODO Auto-generated method stub
        switch (currentState) {
            case ENABLED_STATE: 
                sleeveState = true;
                return R.drawable.sleeve_data_on;
                
            case DISABLED_STATE:
                sleeveState = false;
                return R.drawable.sleeve_toggle_off;
                
            default:
                return (sleeveState?
                            R.drawable.sleeve_data_on:
                            R.drawable.sleeve_toggle_off);
        }
    }
    private static boolean sleeveState = false;
    @Override
    protected boolean getSleeveState() {
        // TODO Auto-generated method stub
        return sleeveState;
    }

    @Override
    protected int getStatusTextColor(State currentState) {
        // TODO Auto-generated method stub
        return (currentState == State.ENABLED_STATE?
                R.color.wifiOnColor:
                R.color.offColor);
    }

    @Override
    protected int getToastText() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    protected int getTogglerImageName() {
        // TODO Auto-generated method stub
        return R.drawable.ic_toggle_data;
    }

    @Override
    protected void toggleService(Context context, State serviceState) {
        // TODO Auto-generated method stub
    	if (getAirPlaneMode(context))
    	{
    		return;
    	}
    	
        if(serviceState != State.INTERMEDIATE_STATE)
        {
            ConnectivityManager connManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);  
            if(connManager != null){
                connManager.setMobileDataEnabled(!(serviceState == State.ENABLED_STATE));
            }

        }
        myLog(LOG_TAG, "ToggleService");
        
    }
    private boolean getAirPlaneMode(Context context)
    {
    	if ( Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1)
    	{
    	    myLog(LOG_TAG, "air plane mode is true" );
    	    return true;
    	}
    	else 
    	{
    		myLog(LOG_TAG, "air plane mode is false" );
    		return false;
    	}
    }
    
    @Override
    public void onEnabled(Context context) {
        myLog(LOG_TAG, "onEnabled");
        super.onEnabled(context);
    	mContext= context;
        // Add the observer to listen Network change
        //begin: IKDOMINO-1541, kbg374, check number of observer, 2011-07-29
        if (mContentQueryMap == null || 0 == mContentQueryMap.countObservers()) {
            myLog(LOG_TAG, "current query map is null ");
	        if (mSettingsCursor == null) {
	            mSettingsCursor = context.getContentResolver().query(Settings.Secure.CONTENT_URI, null,
	               "(" + Settings.System.NAME + "=?)",
	               new String[]{Settings.Secure.PREFERRED_NETWORK_MODE},
	               null);
	        }
	        mContentQueryMap = new ContentQueryMap(mSettingsCursor, Settings.System.NAME, true, null);
            myLog(LOG_TAG, "on update add settings observer");
            mContentQueryMap.addObserver(new SettingsObserver());
        }
        //end: IKDOMINO-1541, kbg374, check number of observer, 2011-07-29
    }
    
    /**
     * Listen for network type change
     */
    private final class SettingsObserver implements Observer {
        public void update(Observable o, Object arg) {
            myLog(LOG_TAG, "SettingsObserver update()");
            
            //begin: IKDOMINO-1470, kbg374, check null point, 2011-07-25
            if (mContext == null) {
                myLog(LOG_TAG, "mContext == null");
                return;
            }
            //end: IKDOMINO-1470, kbg374, check null point, 2011-07-25
            updateWidget(mContext, getServiceState(mContext));
        }
    }

}
