/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
import android.content.ContentValues;
import com.motorola.mmsp.plugin.widget.PluginWidgetHostView;

/**
 * Represents a widget (either instantiated or about to be) in the Launcher.
 */
class LauncherPluginWidgetInfo extends ItemInfo {

    /**
     * Indicates that the widget hasn't been instantiated yet.
     */
    static final int NO_ID = -1;

    /**
     * Identifier for this widget when talking with
     * {@link android.appwidget.AppWidgetManager} for updates.
     */
    int appWidgetId = NO_ID;

    String key;

    /**
     * View that holds this widget after it's been created.  This view isn't created
     * until Launcher knows it's needed.
     */
    PluginWidgetHostView hostView = null;

    int pluginWidgetId = 0;

    LauncherPluginWidgetInfo(int pluginWidgetId) {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_PLUGINWIDGET;
        this.pluginWidgetId = pluginWidgetId;
    }

    @Override
    void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put(LauncherSettings.Favorites.APPWIDGET_ID, pluginWidgetId);
    }

    @Override
    public String toString() {
        return "pluginWidget(id=" + Integer.toString(appWidgetId) + ")";
    }

    @Override
    void unbind() {
        super.unbind();
        hostView = null;
    }
}
