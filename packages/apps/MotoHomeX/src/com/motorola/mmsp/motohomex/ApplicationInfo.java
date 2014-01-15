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

package com.motorola.mmsp.motohomex;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/*Added by ncqp34 at Mar-20 for fake app*/
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
/*ended by ncqp34*/
/**
 * Represents an app in AllAppsView.
 */
// Modified by e13775 at 19 June 2012 for organize apps' group
public class ApplicationInfo extends ItemInfo {
    private static final String TAG = "Launcher2.ApplicationInfo";

    /**
     * The application name.
     */
    // Modified by e13775 at 19 June 2012 for organize apps' group
    public CharSequence title;

    /**
     * The intent used to start the application.
     */
    // Modified by e13775 at 19 June 2012 for organize apps' group
    public Intent intent;

    /**
     * A bitmap version of the application icon.
     */
    // Modified by e13775 at 19 June 2012 for organize apps' group
    public Bitmap iconBitmap;

    /**
     * The time at which the app was first installed.
     */
    long firstInstallTime;

    // Modified by e13775 at 19 June 2012 for organize apps' group
    public ComponentName componentName;

    // Modified by e13775 at 19 June 2012 for organize apps' group
    public boolean isSystem;

    /*Added by ncqp34 at Mar-29-2012 for fake app*/
    static final int FAKE_APP_FLAG = 4;
    /*ended by ncqp34*/

    static final int DOWNLOADED_FLAG = 1;
    static final int UPDATED_SYSTEM_APP_FLAG = 2;
    // Modified by e13775 at 19 June 2012 for organize apps' group
    public int icon;  //resid for the application's icon

    int flags = 0;

    // Modified by e13775 at 19 June 2012 for organize apps' group
    /*2012-7-12 add by Hu ShuAn for switchui-2061 modify protected to public*/
    public ApplicationInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    /**
     * Must not hold the Context.
     */
    public ApplicationInfo(PackageManager pm, ResolveInfo info, IconCache iconCache,
            HashMap<Object, CharSequence> labelCache) {
        final String packageName = info.activityInfo.applicationInfo.packageName;

        this.componentName = new ComponentName(packageName, info.activityInfo.name);
        this.container = ItemInfo.NO_ID;
        this.setActivity(componentName,
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // Modified by e13775 at 19 June 2012 for organize apps' group
        this.isSystem = (0 != (info.activityInfo.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM));

        try {
            int appFlags = pm.getApplicationInfo(packageName, 0).flags;
            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                flags |= DOWNLOADED_FLAG;

                if ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    flags |= UPDATED_SYSTEM_APP_FLAG;
                }
            }
            firstInstallTime = pm.getPackageInfo(packageName, 0).firstInstallTime;
        } catch (NameNotFoundException e) {
            Log.d(TAG, "PackageManager.getApplicationInfo failed for " + packageName);
        }

        iconCache.getTitleAndIcon(this, info, labelCache);
    }

    /*Added by ncqp34 at Mar-20-2012 for fake app*/
    public ApplicationInfo(ComponentName cn, Intent market_intent, Bitmap appIcon, String appLable) {
        this.componentName = cn;
        this.container = ItemInfo.NO_ID;
    intent = market_intent;
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
    Bitmap b = null;
    if(appIcon == null){
        Drawable defaultIcon = null;
        Resources r = Resources.getSystem();
        int resId = r.getIdentifier("sym_def_app_icon", "mipmap", "android");
        if (resId != 0) { 
            defaultIcon = r.getDrawable(resId);
        }
            b = Bitmap.createBitmap(Math.max(defaultIcon.getIntrinsicWidth(), 1),
                Math.max(defaultIcon.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            defaultIcon.setBounds(0, 0, b.getWidth(), b.getHeight());
            defaultIcon.draw(c);
            c.setBitmap(null);

        iconBitmap = b;
    }else {
        iconBitmap = appIcon;
    }
    title = appLable;
    flags = FAKE_APP_FLAG;
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }
    /*ended by ncqp34*/

    public ApplicationInfo(ApplicationInfo info) {
        super(info);
        componentName = info.componentName;
        title = info.title.toString();
        intent = new Intent(info.intent);
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
        // Modified by e13775 at 19 June 2012 for organize apps' group
        isSystem = info.isSystem;
    }

    /** Returns the package name that the shortcut's intent will resolve to, or an empty string if
     *  none exists. */
    String getPackageName() {
        return super.getPackageName(intent);
    }

    /**
     * Creates the application intent based on a component name and various launch flags.
     * Sets {@link #itemType} to {@link LauncherSettings.BaseLauncherColumns#ITEM_TYPE_APPLICATION}.
     *
     * @param className the class name of the component representing the intent
     * @param launchFlags the launch flags
     */
    final void setActivity(ComponentName className, int launchFlags) {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(launchFlags);
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
    }

    @Override
    public String toString() {
        return "ApplicationInfo(title=" + title.toString() + ")";
    }

    public static void dumpApplicationInfoList(String tag, String label,
            ArrayList<ApplicationInfo> list) {
        Log.d(tag, label + " size=" + list.size());
        for (ApplicationInfo info: list) {
            Log.d(tag, "   title=\"" + info.title + "\" iconBitmap="
                    + info.iconBitmap + " firstInstallTime="
                    + info.firstInstallTime);
        }
    }

    public ShortcutInfo makeShortcut() {
	ShortcutInfo info = new ShortcutInfo(this);
	/*Added by ncqp34 at Mar-26-2012 for fake app*/
	if(this.flags == FAKE_APP_FLAG){
	    info.setIcon(this.iconBitmap);
	}
       /*ended by ncqp34*/
	return info;
        //return new ShortcutInfo(this);
    }
}
