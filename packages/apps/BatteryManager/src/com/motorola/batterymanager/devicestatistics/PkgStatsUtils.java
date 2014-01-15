/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.batterymanager.devicestatistics;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageStats;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.internal.app.IUsageStats;
import com.android.internal.os.PkgUsageStats;

import com.motorola.batterymanager.Utils;

public class PkgStatsUtils {
   private final static String TAG = "PkgStatsUtils";
    
   private static boolean isUserInstalledPkg(Context ctx, String pkgName) {
       try {
           ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(pkgName, 
                   PackageManager.GET_UNINSTALLED_PACKAGES); 
           return (ai.sourceDir.startsWith("/system", 0))?false:true;
       } catch (PackageManager.NameNotFoundException e) { /* nothing to be done */ }
       	
       return false;
   }

   /**
    * Gets application usage information
    */
    public static ArrayList<String> getUsageStats(Context ctx, long checkinTime) throws RemoteException {
        PkgUsageStats[] stats = getUsageStatsInterface().getAllPkgUsageStats();
        StringBuilder sb = new StringBuilder();
        ArrayList<String> logList = new ArrayList<String>();
        int logSize = 0;
        String hdr = new String("[ID=AppUsage;ver=" + DevStatPrefs.VERSION + ";time=" + checkinTime + ";]");     
        if (stats != null) {
            sb.setLength(0);
            for (PkgUsageStats ps : stats) {
                  int userPkg = (isUserInstalledPkg(ctx, ps.packageName))?1:0;
                  String str = new String("[ID=appdata;pkg=" + ps.packageName + ";lc=" + ps.launchCount + 
                         ";ut=" + ps.usageTime + ";dwld=" + Integer.toString(userPkg) + ";]");
                  DevStatPrefs dsp = DevStatPrefs.getInstance(ctx);
                  if (logSize + str.length() < dsp.getMaxLogSize()) {
                      logSize += str.length();
                      sb.append(str);
                  } else {
                      logList.add(hdr + sb.toString());
                      sb.setLength(0);
                      sb.append(str);
                      logSize = str.length();
                  }
            }

            if (sb.length() > 0) {
                logList.add(hdr + sb.toString());
            }
        }
        return logList;
     }

     private static IUsageStats getUsageStatsInterface() throws RemoteException {
        IUsageStats iface = IUsageStats.Stub.asInterface(ServiceManager.getService("usagestats"));
        if (iface == null) {
            throw new RemoteException();
        }
        return iface;
    }
}
