package com.motorola.datacollection.sdcard;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;

import com.motorola.datacollection.Utilities;
import com.motorola.datacollection.Utilities.FileSystemStats;
import com.motorola.datacollection.Watchdog;

/* FORMAT
 * [ID=DC_UMS;ver=vv; time=tt;st=xxx;am=AAA;tm=ZZZ;]
 * st : 1 = UMS mode, 0 = SD card mode
 * amem = available memory in case of SD card mode, -1 otherwise
 * tmem = total memory size in case of SD card mode, -1 otherwise
 *
 * [ID=DC_SDCARD;ver=vv; time=tt;ins=xx;]
 * xx : 0 = SD card removed, 1 = SD card inserted.
*/

public class SdCardBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "DCE_SdCardBroadcastReceiver";
    private static final boolean LOGD = Utilities.LOGD;

    private static boolean sSdCardRemoved = false;

    private static final int STATE_INVALID = -1;
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLED = 1;
    private static int mLastUmsState = STATE_INVALID;
    private static int mLastSdcardState = STATE_INVALID;
    private static String sExternalSdcard = Environment.getExternalStorageDirectory().getPath();
    private static final String sDefaultSdcard = "/mnt/sdcard";
    private static StorageStateGetter sStorageState = new StorageStateGetter();

    static class StorageStateGetter {
        String get() {
            return Environment.getExternalStorageState();
        }
    }

    private static final File getExternalAltStorageDirectory(StorageManager storageManager) {
        // The logic in this method came from one of the 2 classes with the name MotoEnvironment
        StorageVolume[] vols = storageManager.getVolumeList();
        if (vols != null) {
            int count = vols.length;
            for (int i=1; i<count; i++) { // Note: loop starts from index 1
                StorageVolume vol = vols[i];
                if (vol != null) {
                    String path = vol.getPath();
                    if (path != null && !path.startsWith("/mnt/usbdisk")) return new File(path);
                }
            }
        }
        return null;
    }

    public static void initialize() {
        // Called from background thread
        if ( LOGD )  Log.d( TAG, "initialize" );

        final StorageManager storageManager =
                (StorageManager)Utilities.getContext().getSystemService(Context.STORAGE_SERVICE);

        if (storageManager != null) {
            File externalSdcard = getExternalAltStorageDirectory(storageManager);
            if ( externalSdcard != null ) {
                sExternalSdcard = externalSdcard.getPath();
                if ( LOGD ) Log.d( TAG, "Detected external sdcard name=" + sExternalSdcard );

                sStorageState = new StorageStateGetter() {
                    String get() {
                        return storageManager.getVolumeState(sExternalSdcard);
                    }
                };
            }
        }

        String storageState = sStorageState.get();

        if ( ( storageState.equals(Environment.MEDIA_REMOVED) ) ||
         ( storageState.equals(Environment.MEDIA_BAD_REMOVAL) ) ) {
            if ( LOGD ) Log.d( TAG, "Init sSdCard Removed" );
            sSdCardRemoved = true;
        }
        else {
            sSdCardRemoved = false;
            if ( LOGD ) Log.d( TAG, "Init sSdCard Present" );
        }
    }

    @Override
    public void onReceive(Context context, final Intent intent) {
        // Called from main thread
        final long timeMs = System.currentTimeMillis();
        new Utilities.BackgroundRunnable() {
            public void run() {
                onReceiveImpl( intent, timeMs );
            }
        };
    }

    private final void onReceiveImpl(Intent intent, long timeMs) {
        // Called from background thread
        if (Watchdog.isDisabled()) return;
        if ( LOGD ) Log.d( TAG, "onReceive called" );
        if ( intent == null ) return;
        if ( LOGD ) Log.d( TAG , "Received intent " + intent.toString() );
        String action = intent.getAction();
        if( action == null ) return;

        if ( action.equals( Intent.ACTION_MEDIA_SHARED ) ) {
            Uri uri = intent.getData();
            if ( uri == null || ( !uri.getPath().equals(sExternalSdcard)
                && !uri.getPath().equals(sDefaultSdcard) ) ) {
                return;
            }

            if ( mLastUmsState != STATE_ENABLED ) {
                mLastUmsState = STATE_ENABLED;

                if ( LOGD ) Log.d( TAG , "UMS Mode Enabled");
                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "DC_UMS",
                        Utilities.EVENT_LOG_VERSION, timeMs,"st","1","am","-1", "tm","-1");

            }
        } else if ( action.equals( Intent.ACTION_MEDIA_MOUNTED ) ) {
            Uri uri = intent.getData();
            if ( uri == null || ( !uri.getPath().equals(sExternalSdcard)
                && !uri.getPath().equals(sDefaultSdcard) ) ) {
                return;
            }

            if ( LOGD ) Log.d( TAG , "ACTION_MEDIA_MOUNTED");
            if((sSdCardRemoved == true) && uri.getPath().equals(sExternalSdcard))
            {
                sSdCardRemoved = false;
                if ( LOGD ) Log.d( TAG , "SDCard Inserted");
                /* Logging for SD card insertion since the media mount event has
                 * been received when the SD card had been removed.
                 */
                if ( mLastSdcardState != STATE_ENABLED ) {
                    mLastSdcardState = STATE_ENABLED;

                    Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2,"DC_SDCARD",
                            Utilities.EVENT_LOG_VERSION, timeMs, "ins", "1");
                }
            }
            /* Putting in SD card mode to take care of scenario where the mode was
             * changed to SD card mode after the SD card was removed. In case the
             * SD card was in shared(UMS)mode, immediately after this a shared intent
             * will be received using which the state will be changed back.
             */
             if ( mLastUmsState != STATE_DISABLED ) {
                mLastUmsState = STATE_DISABLED;

                if ( LOGD ) Log.d( TAG , "SDcard mode enabled");
                FileSystemStats stats = new FileSystemStats( sExternalSdcard );
                Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2, "DC_UMS",
                        Utilities.EVENT_LOG_VERSION, timeMs, "st","0","tm",
                        String.valueOf(stats.mTotalSize),"am",String.valueOf(stats.mAvailableSize));
             }
        } else if ( action.equals( Intent.ACTION_MEDIA_UNMOUNTED) ) {
            Uri uri = intent.getData();
            if ( uri == null || !uri.getPath().equals(sExternalSdcard) ) return;
            if ( LOGD ) Log.d( TAG , "intent.getData URI = " + intent.getData());

            String storageState = sStorageState.get();

            if ( (storageState.equals(Environment.MEDIA_REMOVED) ) ||
                (storageState.equals(Environment.MEDIA_BAD_REMOVAL) ) ) {
                sSdCardRemoved = true;
                if ( mLastSdcardState != STATE_DISABLED ) {
                    mLastSdcardState = STATE_DISABLED;

                    if ( LOGD ) Log.d( TAG , "SDCard Removed");
                    Utilities.reportBasic (Utilities.LOG_TAG_LEVEL_2,"DC_SDCARD",
                            Utilities.EVENT_LOG_VERSION, timeMs, "ins", "0");
                }
            }
            else {
                // Should not come here. If it does, there is some error.
                if ( LOGD ) Log.d( TAG , "Error in sdcard insert/removal" + storageState);
            }
        }
    }
}
