/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.android.contacts.spd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import android.content.Context;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import android.util.Log;
import java.io.FileNotFoundException;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import android.text.TextUtils;

import android.os.Environment;

import com.android.internal.util.XmlUtils;

/**
 * Parses and Reads flex file from /etc folder.
 *
 * MOT FID 35850-Flexing SDN's IKCBS-2075
 *
 * @author Jyothi Asapu - a22850
 */
public final class SpeedDialSetting {

    private static final String TAG = "SpeedDialSetting";
    private static HashMap<String, String> spdMap = null;
    private static final String fileName = "SpeedDialSetting.xml";
    private static boolean settingChanged = false;


    public static String getAppSettings(Context context, String key) {
        InputStream inputStream;
        if(spdMap == null || !spdMap.containsKey(key)){
            try{
                inputStream = context.openFileInput(fileName);
            } catch(FileNotFoundException e){
                Log.e(TAG,"Speed dialer setting file was not found +" +fileName);
                return null;
            }

            try{
                spdMap = XmlUtils.readMapXml(inputStream);
            } catch(XmlPullParserException e) {
                Log.e(TAG,"Speed dialer parse setting xml error." + e);
                return null;
            } catch(IOException e) {
                Log.e(TAG,"Speed dialer parse setting xml error." + e);
                return null;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                    }
                }
            }
        }

        if(spdMap != null){
            return spdMap.get(key);
        }
    
        return null;
    }

    public static boolean setAppSettings(String key, String value) {
        if(spdMap == null) {
            spdMap = new HashMap<String, String>();
        }

        if(spdMap == null || TextUtils.isEmpty(value)){
            return false;
        }

        //spdMap.remove(key);
        if(!value.equals(spdMap.get(key))){
            spdMap.put(key, value);
            settingChanged = true;
        }
        return true;
    }

    public static boolean writeAppSettings(Context context) {
        if(spdMap == null || !settingChanged){
            return false;
        }

        FileOutputStream outStream;
        try{
            outStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
        } catch(FileNotFoundException e){
            Log.e(TAG,"Speed dialer setting file was not found +" +fileName);
            return false;
        }

        try{
            XmlUtils.writeMapXml(spdMap, outStream);
        } catch(XmlPullParserException e) {
            Log.e(TAG,"Speed dialer write setting xml error." + e);
            return false;
        } catch(IOException e) {
            Log.e(TAG,"Speed dialer write setting xml error." + e);
            return false;
        } finally { 
            if(outStream !=null){
                try {
                    outStream.close();
                } catch (Exception e) {
                }
            }
        }
        return true; 
    }    
}
