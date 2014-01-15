/*
 * @(#)IconPersistence.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21345        2012/05/02 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.business;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.Schema;
import com.motorola.contextual.smartrules.db.Schema.IconTableColumns;
import com.motorola.contextual.smartrules.db.table.IconTable;
import com.motorola.contextual.smartrules.db.table.IconTuple;
import com.motorola.contextual.smartrules.util.Util;

/** This class holds the handlers that deal with query, insert, update and delete on
 * 	Icon Table Columns.
 *
 *<code><pre>
 * CLASS:
 * 	 extends IconTable
 *
 *  implements
 *   Constants - for the constants used
 *   DbSyntax - for the DB related constants
 *
 * RESPONSIBILITIES:
 *   None.
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class IconPersistence extends IconTable implements Constants, DbSyntax {

    protected static final String TAG = IconPersistence.class.getSimpleName();

    /** Fetches the icon associated with the rule ID passed in.
     *
     * @param context - context
     * @param ruleId - rule ID in the rule table
     * @return - tuple of Icon associated with the rule.
     */
    public IconTuple fetch(Context context, long ruleId) {
        String whereClause = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
        return this.fetch1(context, whereClause);
    }

    /** Inserts an icon tuple into the Icon table
     *
     * @param context - context
     * @param iconTuple - icon tuple
     */
    public static void insertIcon(Context context, IconTuple iconTuple) {
        try {
            context.getContentResolver().insert(Schema.ICON_TABLE_CONTENT_URI,
                                                new IconTable().toContentValues(iconTuple));
        } catch (Exception e) {
            Log.e(TAG, "Insert to Icon Table failed");
            e.printStackTrace();
        }
    }

    /** Inserts an icon into the Icon table
     *
     * @param context - context
     * @param resName - Resource name of icon
     * @param pkgName - Package name of Icon
     * @param ruleId - rule ID in the rule table
     */
    public static IconTuple insertIcon(Context context, String resName, String pkgName, long ruleId) {
        if (LOG_DEBUG) Log.d(TAG,"insertIcon : pkgName = " + pkgName +
                                 " ruleId = " + ruleId + " resName = " + resName);
        IconTuple iconTuple = new IconTuple();
        iconTuple.setParentFk(ruleId);
        Resources res = null;
        InputStream iconInputStream = null;
        try {
            res = context.getPackageManager().getResourcesForApplication(pkgName);
            int iconResId = res.getIdentifier(resName, "drawable", pkgName);
            if (LOG_DEBUG) Log.d(TAG,"insertIcon : iconResId : " + iconResId +
                                     " from pkgName : " + pkgName);
            if(iconResId <= 0) {
                res = context.getResources();
                List<String> ruleIconList = Util.getRuleIconsList(context, true);
                if(ruleIconList.contains(resName)) {
                    iconResId = res.getIdentifier(resName,
                                                  "drawable", context.getPackageName());
                }
                if(iconResId <= 0)
                    iconResId = res.getIdentifier(DEFAULT_RULE_ICON,
                                                  "drawable", context.getPackageName());
                if (LOG_DEBUG) Log.d(TAG,"insertIcon : iconResId : " + iconResId +
                                         " from pkgName : " + pkgName);
            }
            if(iconResId >0 ) {
                iconInputStream = res.openRawResource(iconResId);
                iconTuple.setIconBlob(getIconBlobFromInputstream(context, iconInputStream));
                insertIcon(context, iconTuple);
            }
        } catch (NameNotFoundException e1) {
            e1.printStackTrace();
        }
        return iconTuple;
    }

    /** Updates an icon in the Icon table
     *
     * @param context - context
     * @param resName - Resource name of icon
     * @param ruleId - rule ID in the rule table
     */
    public static void updateIcon(Context context, String resName, long ruleId) {
        if (LOG_DEBUG) Log.d(TAG,"updateIcon :  ruleId = " + ruleId + " resName = " + resName);
        Resources res = null;
        InputStream iconInputStream = null;
        res = context.getResources();
        int iconResId = res.getIdentifier(resName, "drawable", context.getPackageName());
        if(iconResId <= 0) {
            iconResId = context.getResources().getIdentifier(DEFAULT_RULE_ICON,
                        "drawable", context.getPackageName());
        }
        if(iconResId >0 )
            iconInputStream = res.openRawResource(iconResId);

        if(iconInputStream != null) {
            IconTuple iconTuple = new IconTuple();
            iconTuple.setParentFk(ruleId);
            iconTuple.setIconBlob(getIconBlobFromInputstream(context, iconInputStream));
            ContentValues updateValues = new IconTable().toContentValues(iconTuple);

            String whereClause = IconTableColumns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
            try {
                context.getContentResolver().update(Schema.ICON_TABLE_CONTENT_URI, updateValues, whereClause, null);
            } catch (Exception e) {
                Log.e(TAG, "Insert to Icon Table failed");
                e.printStackTrace();
            }
        }
    }

    /** Deletes an icon from the Icon table
     *
     * @param context - context
     * @param whereClause - where clause
     */
    public static void deleteIcon(Context context, String whereClause) {
        if (LOG_DEBUG) Log.d(TAG,"deleteIcon :  whereClause = " + whereClause );
        try {
            context.getContentResolver().delete(Schema.ICON_TABLE_CONTENT_URI, whereClause, null);
        } catch (Exception e) {
            Log.e(TAG, "Delete from Icon Table failed");
            e.printStackTrace();
        }
    }

    /** Deletes an icon from the Icon table
     *
     * @param context - context
     * @param ruleId - rule ID in the rule table
     */
    public static void deleteIcon(Context context, long ruleId) {
        if (LOG_DEBUG) Log.d(TAG,"deleteIcon :  ruleId = " + ruleId );
        try {
            String whereClause = IconTableColumns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
            context.getContentResolver().delete(Schema.ICON_TABLE_CONTENT_URI, whereClause, null);
        } catch (Exception e) {
            Log.e(TAG, "Delete from Icon Table failed");
            e.printStackTrace();
        }
    }

    /** Returns the drawable for the Icon corresponding to a Rule Id.
    *
    * @param context - context
     * @param ruleId - rule ID in the rule table
     * @param rulePubKey - Publisher key for the Rule
     * @param resName - Resource name of the the Icon
    */
    public static Drawable getIcon(Context context, long ruleId, String rulePubKey,
                                   String resName) {
        if (LOG_DEBUG) Log.d(TAG,"getIcon :  ruleId = " + ruleId  + " rulePublisher = " + rulePubKey + " ResName = " + resName);
        Drawable icon = null;
        if (context != null) {
            IconTuple tuple = new IconPersistence().fetch(context, ruleId);
            if(tuple != null) {
                icon = getIconDrawableFromBlob(context, tuple.getIconBlob());
            }
            if (LOG_DEBUG) Log.d(TAG,"getIcon :  tuple : " + tuple + " icon = " + icon );
            tuple = null;
            if(icon == null) {
                String pkgName = (rulePubKey != null) ?
                                 Util.getPackageNameForRulePublisher(context, rulePubKey) :
                                 context.getPackageName();
                tuple = insertIcon(context, resName, pkgName, ruleId);
                icon = getIconDrawableFromBlob(context, tuple.getIconBlob());
            }
        }
        return icon;
    }


    /** Returns the compressed byteArray for the Icon corresponding to a Rule Id.
    *
    * @param context - context
     * @param ruleId - rule ID in the rule table
     * @param rulePubKey - Publisher key for the Rule
     * @param resName - Resource name of the the Icon
    */
    public static byte[] getIconBlob(Context context, long ruleId, String rulePubKey,
                                     String resName) {
        if (LOG_DEBUG) Log.d(TAG,"getIconBlob :  ruleId = " + ruleId +
                                 " rulePubKey = " + rulePubKey + " resName = " + resName);
        byte[] iconBlob = null;
        if (context != null) {
            IconTuple tuple = new IconPersistence().fetch(context, ruleId);
            if(tuple != null) {
                iconBlob = tuple.getIconBlob();
            }
            tuple = null;
            if(iconBlob == null) {
                String pkgName = (rulePubKey != null) ?
                                 Util.getPackageNameForRulePublisher(context, rulePubKey) :
                                 context.getPackageName();
                tuple = insertIcon(context, resName, pkgName, ruleId);
                iconBlob = tuple.getIconBlob();
            }
        }
        return iconBlob;
    }

    /** Returns the compressed byteArray for the Icon corresponding to a Rule Id.
    *
    * @param context - context
     * @param ruleId - rule ID in the rule table
     * @param rulePubKey - Publisher key for the Rule
     * @param resName - Resource name of the the Icon
    */
    public static byte[] getIconBlob(Context context,long ruleId) {
        byte[] iconBlob = null;
        if (context != null) {
            IconTuple tuple = new IconPersistence().fetch(context, ruleId);
            if(tuple != null) {
                iconBlob = tuple.getIconBlob();
            }
            tuple = null;
        }
        return iconBlob;
    }

    /**
     * Returns the Drawable for SmartActions Default Icon
     * @param context - Context
     * @return Default Icon Drawable
     */
    public static Drawable getDefaulfIcon(Context context) {
        Drawable icon;
        int iconResId = context.getResources().getIdentifier(DEFAULT_RULE_ICON,
                        "drawable", context.getPackageName());
        icon = context.getResources().getDrawable(iconResId);
        return icon;
    }

    /**
     * Returns the Drawable for the given IconBlob
     * @param context - Context
     * @param iconBlob - Compressed format of Icon
     * @return Drawable for the given IconBlob
     */
    public static Drawable getIconDrawableFromBlob(Context context, byte[] iconBlob) {
        Drawable icon = null;
        if(iconBlob != null) {
            TypedValue tv = new TypedValue();
            context.getResources().getValue(R.drawable.ic_default_w, tv, true);

            ByteArrayInputStream is = new ByteArrayInputStream(iconBlob);
            icon = Drawable.createFromResourceStream(context.getResources(), tv, is, "");
        }
        return icon;
    }

    /**
     * Returns the compressed format of Icon in a byte array
     * @param context - Context
     * @param iconDrawable - Drawable of Icon
     * @return byte[] containing compressed format of Icon in a byte array
     */
    public static byte[] getIconBlobFromIcon(Context context, Drawable iconDrawable) {
        byte[] iconBlob = null;
        Bitmap bitmap = ((BitmapDrawable)iconDrawable).getBitmap();
        ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

        if (bitmap != null && bitmap.compress(CompressFormat.PNG, 100, byteArray)) {
            iconBlob = byteArray.toByteArray();
        }
        return iconBlob;
    }

    /**
     * Returns the byte array from the inputstream
     * @param context - Context
     * @param inputStream - Inputstream to read data
     * @return byte[] containing compressed format of Icon in a byte array
     */
    public static byte[] getIconBlobFromInputstream(Context context, InputStream inputStream) {
        if(inputStream == null) return null;
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len = 0;
        try {
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteBuffer.toByteArray();
    }

    /** Returns the Icon Table Cursor
     *  Note: The caller is expected to close the Cursor
     *
     *  @param context - context
     *  @param ruleId - the rule ID for Icons
     *
     *  @return - iconCursor - Icon Table Cursor
     */
    public static Cursor getIconCursor(Context context, final long ruleId) {

        Cursor iconCursor = null;

        if (context != null)
        {
            try {
                String tableSelection = Columns.PARENT_FKEY + EQUALS + Q + ruleId + Q;
                iconCursor = context.getContentResolver().query(
                                 Schema.ICON_TABLE_CONTENT_URI,
                                 null, tableSelection, null,  null);
            } catch(Exception e) {
                Log.e(TAG, "Query failed for Icon Table");
            }
        } else {
            Log.e(TAG,"context is null");
        }
        return iconCursor;
    }
}