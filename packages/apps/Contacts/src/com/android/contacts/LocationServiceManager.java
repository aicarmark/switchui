package com.android.contacts;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.widget.TextView;
import android.view.View;
import android.util.Log;

public final class LocationServiceManager {
    private static final String TAG = "---LocationService---";

    private Object mLocationBindingLock = new Object();
    private volatile boolean mIsLocationBound;

    //Intent
    private static final String NUMBER_LOCATION_SERVICE_ID = "com.motorola.numberlocation.INumberLocationService";
    //Flag
    private static final String NUM_ADDR_SERVICE_FLAG = "location_service_on";
    private static final String ENABLE_SHOW_LOCATION = "show_location_enable";
    //Service
    private  com.motorola.numberlocation.INumberLocationService mNumberLocationService = null;
    //Connection
    private ServiceConnection mNumberLocationConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            synchronized(mLocationBindingLock) {
                    mNumberLocationService = com.motorola.numberlocation.INumberLocationService.Stub.asInterface(service);
                    mIsLocationBound = true;
                    showLocation(mPhoneNumber);
                    mLocationBindingLock.notifyAll();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            synchronized(mLocationBindingLock) {
                mIsLocationBound = false;
                mNumberLocationService = null;
                mLocationBindingLock.notifyAll();
            }
        }
    };

    private Context mContext = null;
    private String mPhoneNumber = null;
    private TextView mCallLocation = null;

    public LocationServiceManager(Context context,TextView CallLocation)
    {
        mContext = context;
        mCallLocation = CallLocation;
        checkService(true);
    }

    public void unbind()
    {
        synchronized(mLocationBindingLock) {
            if (mIsLocationBound) {
                if (mNumberLocationService != null)
                {
                    mContext.unbindService(mNumberLocationConnection);
                    mIsLocationBound = false;
                    mNumberLocationService = null;
                }
            }
        }
    }

    public void showLocation(String PhoneNumber)
    {
        if(mCallLocation == null) return;
        if(PhoneNumber == null)
        {
            mCallLocation.setText(null);
            return;
        }

        mPhoneNumber = PhoneNumber;
        String address = getLocInfo(PhoneNumber);
        mCallLocation.setText(address);
    }

    public boolean checkService(boolean needBind) {
        if (Settings.System.getInt(mContext.getContentResolver(), NUM_ADDR_SERVICE_FLAG, 1) != 0)
        {
            if (mNumberLocationService != null)
            {
                return true;
            }
            else if(needBind)
            {
                synchronized(mLocationBindingLock) {
                    if (!mIsLocationBound) {
                        mContext.bindService(new Intent(NUMBER_LOCATION_SERVICE_ID),
                                mNumberLocationConnection,
                                Context.BIND_AUTO_CREATE);
                    }
                }
            }
        }
        else
        {
            // for directly unbind
            unbind();
        }

        return false;
    }

    public String getLocInfo(String phoneNumber)
    {
        phoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);
        //phoneNumber = removeGcCode(phoneNumber);
        if(phoneNumber == null || (0 == Settings.System.getInt(mContext.getContentResolver(), ENABLE_SHOW_LOCATION, 1))) {
            return null;
        }

        String loc = null;
        try
        {
            if (mNumberLocationService != null)
            {
                loc = mNumberLocationService.getLocationByNumber(phoneNumber);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return loc;
    }

    public static String removeGcCode(String number) {
        String removed = number;
        if (removed.startsWith("+86")) {
            if (!(removed.startsWith("0", 3))) {
                if (removed.startsWith("1", 3) && !(removed.startsWith("0", 4))) {
                    removed = removed.substring(3);
                } else {
                    removed = "0" + removed.substring(3);
                }
            }
        }
        if (removed.startsWith("0086")) {
            if (!(removed.startsWith("0", 4))) {
                if (removed.startsWith("1", 4) && !(removed.startsWith("0", 5))) {
                    removed = removed.substring(4);
                } else {
                    removed = "0" + removed.substring(4);
                }
            }
        }
        return removed;
    }

    public void setLocationView(TextView CallLocation)
    {
        mCallLocation = CallLocation;
    }

    public boolean ismIsLocationBound() {
        return mIsLocationBound;
    }

}
