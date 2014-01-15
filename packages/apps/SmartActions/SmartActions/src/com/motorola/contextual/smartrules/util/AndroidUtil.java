package com.motorola.contextual.smartrules.util;

import java.util.Date;
import java.util.List;

import com.motorola.contextual.smartrules.Constants;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class AndroidUtil implements Constants {


    private static final String TAG = AndroidUtil.class.getSimpleName();

    private static PackageManager mPkgMgr = null;
    private static final long TEN_MINUTES = 1000*60*10;

    private List<ResolveInfo> mResolveList = null;
    private long cachedResolveListDateTime = 0;


    /** basic constructor */
    public AndroidUtil() {
        super();
    }


    /**
     * @return the mPkgMgr
     */
    public static PackageManager getPkgMgr() {
        return mPkgMgr;
    }


    /** returns null if not found or entry for pkg mgr if found.
     *
     * @param context - context
     * @param category - category on which to find the resolve info
     * @param metaDataKey - key on which to locate the Resolve Info meta data
     * @param metaDataValue - value to match in the metaData
     * @return null if not found, or ResolveInfo for package manager entry if found
     */
    public ResolveInfo findPkgMgrEntry(final Context context, final String category, final String metaDataKey, final String metaDataValue) {

        ResolveInfo result = null;
        if (mPkgMgr == null)
            mPkgMgr = context.getPackageManager();
        // cache the resolve list for 10 minutes
        if (mResolveList == null || (new Date().getDate() - cachedResolveListDateTime > TEN_MINUTES )) {
            Intent mainIntent = new Intent(ACTION_GET_CONFIG, null);
            mainIntent.addCategory(category);
            mResolveList = mPkgMgr.queryIntentActivities(mainIntent, PackageManager.GET_META_DATA);
            cachedResolveListDateTime = new Date().getTime();
        }

        if (mResolveList != null)
            for (int i = 0; i < mResolveList.size(); i++) {
                ResolveInfo info = mResolveList.get(i);

                // Get the type
                android.os.Bundle metaData = info.activityInfo.metaData;
                if (metaData == null) metaData = info.serviceInfo.metaData;
                if ( metaData != null ) {
                    String key = metaData.getString(metaDataKey);
                    if (key != null && key.equals(metaDataValue)) {
                        // found !
                        result = info;
                        if (LOG_DEBUG) Log.d(TAG, "FindPkg - Found:("+category+") key=" + key);
                        break;
                    }
                }
            }
        return result;
    }


    //* fix for: IKJBREL1-8126 */
    public static void recycleDrawable(Drawable d) {
		if (d != null && d instanceof BitmapDrawable) {
			((BitmapDrawable)d).getBitmap().recycle();
		}
    }
    
}
