package com.motorola.datacollection.pkg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Watchdog;

/* FORMAT
 *      [ID=DC_PKG;ver=xx;time=xx ;name=xx;st=xx;]
 *      name = package name
 *      st = 0 for pkg added,
 *         = 2 for pkg removed
 *      av = app version
 *      an = app name
 *      au = app uid
 */

public class PackageInfoReceiver extends BroadcastReceiver {


    private static final String TAG = "DCE_PackageInfoReceiver";
    private static final boolean LOGD = Utilities.LOGD;
    private static final String APP_VER = Utilities.EVENT_LOG_VERSION;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl(context, intent, timeMs);
            }
        };
    }

    public void onReceiveImpl(Context context, Intent intent, long timeMs) {
        if (Watchdog.isDisabled()) return;

        int pkg_state=0;

        if ( intent == null ) return;
        String action = intent.getAction();
        if ( action == null ) return;

        if ( action.equals(Intent.ACTION_PACKAGE_ADDED) ) {
            pkg_state = 0;
            if (LOGD) Log.d(TAG,"Package added");
        } else if ( action.equals(Intent.ACTION_PACKAGE_REMOVED) ) {
            pkg_state = 2;
        } else {
            if (LOGD) Log.d(TAG,"Unknown package intent");
            return;
        }
        String pkgName = getPackageName(intent);

        if ( LOGD ) Log.d(TAG, "Package Update: " +intent.getAction() + pkgName);

        String av = "";
        String an = "";
        String au = "";
        if ( action.equals(Intent.ACTION_PACKAGE_ADDED) ) {
            if (LOGD) Log.d(TAG,"Package added");
            try {
                PackageManager pm = Utilities.getContext().getPackageManager();
                String pkg = getPackageName(intent);
                if (LOGD) Log.d(TAG,"Pkg="+pkg);
                PackageInfo pinfo = pm.getPackageInfo(pkg, 0);
                ApplicationInfo ainfo = pinfo.applicationInfo;
                CharSequence title = pm.getApplicationLabel(ainfo);
                av = pinfo.versionName;
                an = (String) title;
                au = Integer.toString(ainfo.uid);
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Exception while trying to get Application details");
            }
        }
        Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "DC_PKG", APP_VER, timeMs, "name",
                pkgName, "st", Integer.toString(pkg_state), "av", av, "an", an, "au", au);

    }

    private String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        String pkg = null;
        if ( uri != null ) {
           pkg = uri.getSchemeSpecificPart();
        }
        return pkg;
    }
}
