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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcelable;
import android.content.ContentValues;
import android.util.Log;
import com.motorola.mmsp.motohomex.R;


public class UpdateShortcutReceiver extends BroadcastReceiver {
    public static final String ACTION_UPDATE_SHORTCUT =
        "com.android.launcher.action.UPDATE_SHORTCUT";

    public void onReceive(Context context, Intent data) {
        if (!ACTION_UPDATE_SHORTCUT.equals(data.getAction())) {
            return;
        }
        //Modified by e13775 July26 2012 for SWITCHUI-2473
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String title = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);

        if (intent != null && title != null) {
            final ContentResolver cr = context.getContentResolver();
            Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
                    new String[] { LauncherSettings.Favorites._ID, LauncherSettings.Favorites.INTENT },
                    LauncherSettings.Favorites.INTENT + "=?", new String[] { intent.toUri(0) }, null);

            final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
            final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);

            boolean changed = false;

            try {
                while (c.moveToNext()) {
                    try {
                        if (c.getString(intentIndex) != null
                                && intent.filterEquals(Intent.parseUri(c.getString(intentIndex), 0))) {
                            final long id = c.getLong(idIndex);
                            final Uri uri = LauncherSettings.Favorites.getContentUri(id, false);

                            ContentValues values = new ContentValues();
                            values.put(LauncherSettings.BaseLauncherColumns.TITLE, title);
                            values.put(LauncherSettings.BaseLauncherColumns.INTENT, intent.toUri(0));
                            if (extra != null && extra instanceof ShortcutIconResource) {
                                values.put(LauncherSettings.BaseLauncherColumns.ICON_PACKAGE,
                                        ((ShortcutIconResource)extra).packageName);
                                values.put(LauncherSettings.BaseLauncherColumns.ICON_RESOURCE,
                                        ((ShortcutIconResource)extra).resourceName);
                            }
                            cr.update(uri, values, null, null);
                            changed = true;
                                }
                    } catch (Exception ex) {
                        Log.e("UpdateShortcutReceiver", "unable to update the shortcut");
                    }
                }
            } finally {
                c.close();
            }

            if (changed) {
                cr.notifyChange(LauncherSettings.Favorites.CONTENT_URI, null);
            }
        }
    }
}
//Modified by e13775 July26 2012 for SWITCHUI-2473
