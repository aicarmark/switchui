/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.internal.app.IUsageStats;
import com.android.internal.os.PkgUsageStats;
import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.CheckinHelper.DsSegment;
import com.motorola.devicestatistics.packagemap.MapDbHelper;
import com.motorola.devicestatistics.packagemap.PreloadMap;

public class PkgStatsUtils {
    
   private static boolean isUserInstalledPkg(Context ctx, String pkgName) {
       try {
           ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(pkgName, 
                   PackageManager.GET_UNINSTALLED_PACKAGES); 
           return (!ai.sourceDir.startsWith("/system", 0));
       } catch (PackageManager.NameNotFoundException e) {
            /* This means the package was un-installed, has to be a user pkg */
            return true;
       }
   }

   /**
    * Gets application usage information
    */
    public static void addUsageStats(Context ctx, long checkinTime, ArrayList<DsCheckinEvent> logList)
                    throws RemoteException {
        PkgUsageStats[] stats = getUsageStatsInterface().getAllPkgUsageStats();
        int logSize = 0;
        DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent(DevStatPrefs.CHECKIN_EVENT_ID,
                "AppUsage", DevStatPrefs.VERSION, checkinTime );

        if (stats != null) {
            DevStatPrefs dsp = DevStatPrefs.getInstance(ctx);
            long maxSize = dsp.getMaxLogSize();
            MapDbHelper mapper = MapDbHelper.getInstance(ctx.getApplicationContext());

            HashMap<String, Long> cl = new HashMap<String, Long>();
            for (PkgUsageStats ps : stats) {
                if (ps.launchCount == 0 && ps.usageTime == 0) continue; // reduce data volume
                int userPkg = (isUserInstalledPkg(ctx, ps.packageName)) ? 1 : 0;
                long nid = mapper.getId(ps.packageName, true);
                if(!PreloadMap.isPreloaded(nid)) cl.put(ps.packageName, nid);

                DsSegment segment = CheckinHelper.createNamedSegment("appdata",
                        "pkg", ps.packageName, "lc", String.valueOf(ps.launchCount),
                        "ut", String.valueOf(ps.usageTime), "dwld", String.valueOf(userPkg));
                int segmentLength = segment.length();

                if (logSize + segmentLength >= maxSize) {
                    logList.add(checkinEvent);
                    checkinEvent = CheckinHelper.getCheckinEvent(DevStatPrefs.CHECKIN_EVENT_ID,
                            "AppUsage", DevStatPrefs.VERSION, checkinTime );
                    logSize = 0;
                }
                logSize += segmentLength;
                checkinEvent.addSegment(segment);
            }

            if (logSize > 0) logList.add(checkinEvent);

            if(!cl.isEmpty()) {
                checkinEvent = CheckinHelper.getCheckinEvent( DevStatPrefs.CHECKIN_EVENT_ID,
                        "PMaps", DevStatPrefs.VERSION, checkinTime );
                logSize = checkinEvent.length();
                Iterator<Map.Entry<String, Long>> values = cl.entrySet().iterator();
                while(values.hasNext()) {
                    Map.Entry<String, Long> value = values.next();
                    if(logSize > 4000) {
                        logList.add(checkinEvent);
                        checkinEvent = CheckinHelper.getCheckinEvent( DevStatPrefs.CHECKIN_EVENT_ID,
                                "PMaps", DevStatPrefs.VERSION, checkinTime );
                        logSize = checkinEvent.length();
                    }
                    DsSegment segment = CheckinHelper.createUnnamedSegment("m",
                            String.valueOf(value.getValue()), value.getKey());
                    logSize += segment.length();
                    checkinEvent.addSegment(segment);
                }
                logList.add(0, checkinEvent);
            }
        }
    }

     private static IUsageStats getUsageStatsInterface() throws RemoteException {
        IUsageStats iface = IUsageStats.Stub.asInterface(ServiceManager.getService("usagestats"));
        if (iface == null) {
            throw new RemoteException();
        }
        return iface;
    }

    static void getMapperDump(Context context, long checkinTime, ArrayList<DsCheckinEvent> logList) {
        MapDbHelper mapper = MapDbHelper.getInstance(context.getApplicationContext());
        HashMap<String, Long> cl = mapper.generateDump();
        if(!cl.isEmpty()) {
            DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent( DevStatPrefs.CHECKIN_EVENT_ID,
                    "PMaps", DevStatPrefs.VERSION, checkinTime );
            int logSize = checkinEvent.length();
            Iterator<String> keys = cl.keySet().iterator();
            while(keys.hasNext()) {
                String key = keys.next();
                Long l = cl.get(key);
                if(l != null) {
                    if(logSize > 3500) {
                        logList.add(0, checkinEvent);
                        checkinEvent = CheckinHelper.getCheckinEvent( DevStatPrefs.CHECKIN_EVENT_ID,
                                "PMaps", DevStatPrefs.VERSION, checkinTime );
                        logSize = checkinEvent.length();
                    }
                    DsSegment segment = CheckinHelper.createUnnamedSegment("m",String.valueOf(l),key);
                    logSize += segment.length();
                    checkinEvent.addSegment(segment);
                }
            }
            logList.add(0, checkinEvent);
        }
        updateMapperDumpTime(context, checkinTime * 1000);
    }

    static void updateMapperDumpTime(Context context, long time) {
        DevStatPrefs prefs = DevStatPrefs.getInstance(context);
        prefs.setLongSetting(DevStatPrefs.DEVSTATS_PMAPS_REFTIME, time);
    }

    // This is to force a start time for the dump, since we do not
    // do a package map dump at first powerup
    static void resetMapperDumpTime(Context context, long time) {
        DevStatPrefs prefs = DevStatPrefs.getInstance(context);
        if(prefs.getLongSetting(DevStatPrefs.DEVSTATS_PMAPS_REFTIME, 0) == 0)
            prefs.setLongSetting(DevStatPrefs.DEVSTATS_PMAPS_REFTIME, time);
    }
}
