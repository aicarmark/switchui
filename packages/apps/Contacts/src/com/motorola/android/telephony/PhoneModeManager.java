/*
/* Copyright (C) 2009, Motorola India, Inc.,
/* All Rights Reserved
/* Class name: PhoneModeManager
/*
/*
/*
/* Modification History:
/*************************************************************************
/* Date                Author                    Comments
/* 23 Mar 2012         dcjf34/xinyu Liu          Added to dual mode support
/***************************************************************************
/*/

package com.motorola.android.telephony;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import android.text.TextUtils;
import com.android.contacts.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.android.internal.util.XmlUtils;
import android.provider.CallLog.Calls;
import android.content.Context;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Xml;
import android.content.res.Resources;

public class PhoneModeManager {
      private static final String TAG = "PhoneModeManager";

      public static final String ACTION_ICC_PRECACHE_STATUS_CHANGED =
            "com.motorola.intent.action.ICC_PRECACHE_STATUS_CHANGED";


      public static final String ACTION_SECONDARY_ICC_PRECACHE_STATUS_CHANGED =
            "com.motorola.intent.action.ICC_PRECACHE_STATUS_CHANGED_2";

      static final String ContactsConfig = "/system/etc/ContactsConfig.xml";
      public static boolean bInitFinish = false;
      private static String ftr_36876_call_time_display_by_date_time_format  = "";
      private static boolean b_36876_callTimeDisplay = false;

      private static String ftr_36093_roamingcall = "";
      private static boolean b_36093_roamingcall = false;

      private static String ftr_36094_roaming_callback = "";
      private static boolean b_36094_roaming_callback = false;
 /**
     * To get whether the device is working in DMDS mode
     *
     * @return true if it is DMDS mode, false otherwise
     */
    public static boolean isDmds() {
        return SystemProperties.getInt("ro.telephony.secondary_network", -1) != -1;
    }

    public static void initPhoneModeMananger(Resources mResource){
        getContactsConfig();
        b_36876_callTimeDisplay = mResource.getBoolean(R.bool.ftr_36876_call_time_display_by_date_time_format);
        b_36093_roamingcall = mResource.getBoolean(R.bool.ftr_36093_roamingcall);
        b_36094_roaming_callback = mResource.getBoolean(R.bool.ftr_36094_roaming_callback);
    }


    private static void getContactsConfig() {
        FileReader ContactsConfigReader = null;
        File ContactsConfigFile = null;

        Log.d(TAG, "getContactsConfig!!!");

        try {
            ContactsConfigFile = new File(ContactsConfig);
            if(ContactsConfigFile == null){
                Log.d(TAG, "ContactsConfigFile == null");
            }
            ContactsConfigReader = new FileReader(ContactsConfigFile);
            if(ContactsConfigReader == null){
                Log.d(TAG, "ContactsConfigReader == null");
            }

            Boolean bexist = ContactsConfigFile.exists();
            Log.d(TAG, "bexist =" + bexist);

            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(ContactsConfigReader);

            XmlUtils.beginDocument(parser, "contactsValue");
            ftr_36876_call_time_display_by_date_time_format = parser.getAttributeValue(null, "ftr_36876_call_time_display_by_date_time_format");
            ftr_36093_roamingcall = parser.getAttributeValue(null, "ftr_36093_roamingcall");
            ftr_36094_roaming_callback = parser.getAttributeValue(null, "ftr_36094_roaming_callback");
            Log.d(TAG, ":ftr_36876_call_time_display_by_date_time_format=" + ftr_36876_call_time_display_by_date_time_format);
            Log.d(TAG, ":ftr_36093_roamingcall="+ftr_36093_roamingcall);
            bInitFinish = true;

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Could not open contacts configuration file: " + e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Exception in contacts parser " + e);
        } catch (IOException e) {
            Log.e(TAG, "Exception in contacts parser " + e);
        } finally {
            try {
                if (ContactsConfigReader != null) ContactsConfigReader.close();
            } catch (IOException e) {
                Log.e(TAG, " Exception in contacts parser finally " + e);
            }
        }
    }

    public static boolean IsCallTimeDisplayByDateTimeFormat() {
        boolean bCallTimeDisplayByDateTimeFormat = false;
        if(TextUtils.isEmpty(ftr_36876_call_time_display_by_date_time_format)){
            return b_36876_callTimeDisplay;
        }
        if(ftr_36876_call_time_display_by_date_time_format.equalsIgnoreCase("true")){
            bCallTimeDisplayByDateTimeFormat = true;
        }
        Log.d(TAG, "getCallTimeDisplayByDateTimeFormat : bCallTimeDisplayByDateTimeFormat="
                + bCallTimeDisplayByDateTimeFormat);
        return bCallTimeDisplayByDateTimeFormat;
    }

    public static boolean IsRoamingCallSupported() {
        boolean bRoamingCall = false;
        if(TextUtils.isEmpty(ftr_36093_roamingcall)) {
            return b_36093_roamingcall;
        }
        if(ftr_36093_roamingcall.equalsIgnoreCase("true")){
            bRoamingCall = true;
        }
        return bRoamingCall;
    }

    public static boolean IsRoamingCallBackSupported() {
        boolean bRoamingCallBack = false;
        if(TextUtils.isEmpty(ftr_36094_roaming_callback)) {
            return b_36094_roaming_callback;
        }
        if(ftr_36094_roaming_callback.equalsIgnoreCase("true")){
            bRoamingCallBack = true;
        }
        return bRoamingCallBack;
    }

}

