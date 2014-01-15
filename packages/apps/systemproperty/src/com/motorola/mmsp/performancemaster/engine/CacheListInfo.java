/**
 ** Copyright (C) 2010, Motorola, Inc,
 ** All Rights Reserved
 ** Class name:         CacheListInfo.java
 ** Description: What the class does.
 **
 ** @author txvd74 @date May 7, 2012 create this file, 
 ** Author email: txvd74@motorola.com
 **
 ** Modification History:
 ***********************************************************
 ** Date                              Author           Comments
 * May 7, 2012                 txvd74
 *  
 *         
 */

package com.motorola.mmsp.performancemaster.engine;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import java.util.concurrent.CountDownLatch;

import com.motorola.mmsp.performancemaster.engine.Job.JobDoneListener;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.RemoteException;
import android.os.StatFs;
import android.util.Log;

/**
 * @author TXVD74
 */
public class CacheListInfo extends InfoBase implements JobDoneListener {
    private final static String TAG = "CacheListInfo";

    private Job mUpdateJob = null;

    private PackageManager mPackageManager;
    private Context mContext;
    private ArrayList<CacheInfo> mCacheInfoList = null; // list open to other
    // use it
    private  CountDownLatch mCountDownLatch; // wait for the complete
    public long mCacheSize = 0; // the app cachesize

    private boolean mUpdated = false; // the list if update
    private boolean mIsUpdating = false;

    public CacheListInfo(Context context) {
        super();
        this.mContext = context;
        mCacheInfoList = new ArrayList<CacheInfo>();
        mPackageManager = mContext.getPackageManager();

        mUpdateJob = new Job(CacheListInfo.this) {
            @Override
            public void doJob() {
                
                synchronized (CacheListInfo.this) {
                    Log.e(TAG, " CacheListInfo mUpdateJob  mIsUpdating = " + mIsUpdating);
                    if (!mIsUpdating) {
                        mIsUpdating = true;
                    } else {
                        return;
                    }
                }
                

                List<ApplicationInfo> mApplicationInfoList = mPackageManager
                        .getInstalledApplications(0);

                Log.e(TAG, " CacheListInfo mCacheInfoList.size = " + mCacheInfoList.size()
                            );

                ArrayList<CacheInfo> mCacheInfoListNew = new ArrayList<CacheInfo>();

                for (int l = 0; l < mApplicationInfoList.size(); l++) {

                    ApplicationInfo mApplicationInfo = (ApplicationInfo) mApplicationInfoList
                                .get(l);

                    mCountDownLatch = new CountDownLatch(1);

                    getCacheSize(mApplicationInfo.packageName);
                    try {
                        mCountDownLatch.await();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        Log.e(TAG,
                                " CacheListInfo mCountDownLatch.await() exception e.toString() = "
                                        + e.toString());

                        e.printStackTrace();
                    }
                    if ((mCacheSize == 0)
                                || (mApplicationInfo.packageName
                                        .equals("com.motorola.mmsp.performancemaster")))
                        continue;

                    CacheInfo mCacheInfo = new CacheInfo();
                    mCacheInfo.setName(mApplicationInfo.loadLabel(mPackageManager).toString());

                    Drawable localDrawable = mApplicationInfo.loadIcon(mPackageManager);
                    if (localDrawable != null) {
                        mCacheInfo.setIcon(localDrawable);
                    } else {
                        mCacheInfo.setIcon(mPackageManager.getDefaultActivityIcon());
                    }

                    mCacheInfo.setCacheSize(mCacheSize);

                    if ((mCacheInfo.getCacheSize() > 0) && (mCacheInfoListNew != null)) {
                        mCacheInfoListNew.add(mCacheInfo);
                    }

                    Log.e(TAG, " CacheListInfo mUpdateJob  mCacheSize = " + mCacheSize
                                + " name =" + mCacheInfo.getName());

                }
                
                Collections.sort(mCacheInfoListNew, new Comparator() {
                    @Override
                    public int compare(Object cacheInfo1, Object cacheInfo2) {
                        return (int) (((CacheInfo) cacheInfo2).getCacheSize() - ((CacheInfo) cacheInfo1)
                                    .getCacheSize());
                    }
                });

                synchronized (CacheListInfo.this) {
                    mUpdated = true;
                    mIsUpdating = false;
                    mCacheInfoList = mCacheInfoListNew;
                }

            }
        };

    }

    @Override
    protected void startInfoUpdate() {

        
        synchronized (CacheListInfo.this) {    
            Log.e(TAG, " CacheListInfo startInfoUpdate  mIsUpdating = " + mIsUpdating
            );
            if (mIsUpdating) {
               return;
            }
        }
        
        Worker.getInstance().addJob(mUpdateJob);

    }

    private long getEnvironmentSize() {
        File mFile = Environment.getDataDirectory(); // get the /data
        long mDataSize;
        if (mFile == null) {
            mDataSize = 0L;
        }

        String str = mFile.getPath();
        StatFs localStatFs = new StatFs(str);
        long mBlockSize = localStatFs.getBlockSize();
        mDataSize = localStatFs.getBlockCount() * mBlockSize;
        return mDataSize;

    }

    /*
     * clear the caches
     */

    public void clearAllCaches() {
        try {
            Method localMethod = mPackageManager.getClass().getMethod("freeStorageAndNotify",
                    Long.TYPE,
                    IPackageDataObserver.class);
            Long localLong = Long.valueOf(getEnvironmentSize() - 1L);
 
            Log.e(TAG, " clearAllCaches localLong = " + localLong);
            localMethod.invoke(mPackageManager, localLong, new IPackageDataObserver.Stub() {
                @Override
                public void onRemoveCompleted(String packageName,
                        boolean succeeded) throws RemoteException {

                    synchronized (CacheListInfo.this) {

                        if (mCacheInfoList.size() > 0) {
                            mCacheInfoList.clear();
                        }

                    }
                    Log.e(TAG, " clearAllCaches onRemoveCompleted  " );
                }
            });
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, " CacheListInfo clearAllCaches SecurityException exception e.toString() = "
                    + e.toString());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            Log.e(TAG,
                    " CacheListInfo clearAllCaches NoSuchMethodException exception e.toString() = "
                            + e.toString());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            Log
                    .e(
                            TAG,
                            " CacheListInfo clearAllCaches IllegalArgumentException exception e.toString() = "
                                    + e.toString());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            Log
                    .e(
                            TAG,
                            " CacheListInfo clearAllCaches IllegalAccessException exception e.toString() = "
                                    + e.toString());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            Log
                    .e(
                            TAG,
                            " CacheListInfo clearAllCaches InvocationTargetException exception e.toString() = "
                                    + e.toString());
            e.printStackTrace();
        }
    }

    public boolean isUpdated() {
        synchronized (CacheListInfo.this) {
            return mUpdated;
        }
    }

    public void getCacheSize(String paramString) {
        try {
            Method localMethod = mPackageManager.getClass().getMethod("getPackageSizeInfo",
                    String.class, IPackageStatsObserver.class);
            localMethod.invoke(mPackageManager, paramString, new IPackageStatsObserver.Stub()
            {
                public void onGetStatsCompleted(PackageStats paramPackageStats,
                        boolean paramBoolean)
                        throws RemoteException
                        {
                            if ((paramBoolean) && (paramPackageStats != null)) {
                                mCacheSize = paramPackageStats.cacheSize;
                            }
                            else {
                                mCacheSize = 0;
                            }

                            mCountDownLatch.countDown(); // get the size , wake
                                                         // the
                            // thread
                        }
            });
        } catch (IllegalArgumentException localIllegalArgumentException) {
            localIllegalArgumentException.printStackTrace();
        } catch (IllegalAccessException localIllegalAccessException) {
            localIllegalAccessException.printStackTrace();
        } catch (InvocationTargetException localInvocationTargetException) {
            localInvocationTargetException.printStackTrace();
        } catch (SecurityException localSecurityException) {
            localSecurityException.printStackTrace();
        } catch (NoSuchMethodException localNoSuchMethodException) {
            localNoSuchMethodException.printStackTrace();
        }
    }

    public void onCacheListUpdate() {

        synchronized (CacheListInfo.this) {            
            if (mIsUpdating) {
               return;
            }
        }
        
        Worker.getInstance().addJob(mUpdateJob);
    }

    public class CacheInfo {
        private long cacheSize;
        private Drawable icon;
        private String name;

        public CacheInfo() {

        }

        public long getCacheSize() {
            return this.cacheSize;
        }

        public Drawable getIcon() {
            return this.icon;
        }

        public String getName() {
            return this.name;
        }

        public void setCacheSize(long paramLong) {
            this.cacheSize = paramLong;
        }

        public void setIcon(Drawable paramDrawable) {
            this.icon = paramDrawable;
        }

        public void setName(String paramString) {
            this.name = paramString;
        }

        @Override
        public String toString() {
            return "CacheInfo: " + this.getName() + "/"
                    + this.getCacheSize();
        }

    }

    /*
     * (non-Javadoc)
     * @see com.motorola.mmsp.performancemaster.engine.Job.JobDoneListener#onJobDone()
     */
    @Override
    public void onJobDone() {
        synchronized (CacheListInfo.this) {
            onInfoUpdate();
        }
    }

    /*
     * the other can get the list
     */
    public ArrayList<CacheInfo> getCacheInfoList() {
        synchronized (CacheListInfo.this) {
            return mCacheInfoList;
        }
    }

}
