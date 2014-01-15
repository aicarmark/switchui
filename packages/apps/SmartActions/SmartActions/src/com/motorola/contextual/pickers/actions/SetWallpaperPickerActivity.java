/*
 * @(#)SetWallpaperActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18984       2011/02/09  NA                  Initial version
 * rdq478       2011/05/10  IKMAIN-15833        Rework on wallpaper activity
 * rdq478       2011/05/11  IKINTNETAPP-280     Fixed dialog leak issue and
 *                                              exception that cause thread to die.
 * rdq478       2011/05/16  IKMAIN-17373        Changed wallpaper desc to filename and
 *                                              and fix receiver leak.
 * rdq478       2011/05/24  IKMAIN-17917        Saved wallpaer in private folder and called
 *                                              to start clean up service.
 * rdq478       2011/05/31  IKINTNETAPP-320     Fixed find bug warning
 * rdq478       2011/07/13  IKINTNETAPP-386     Changed to statefull action
 * rdq478       2011/08/16  IKSTABLE6-6826      Remove thread from onReceive and use service instead
 * rdq478       2011/09/22  IKMAIN-28241        Change launch flag for SET_WALLPAPER intent
 * rdq478       2011/10/04  IKMAIN-28956        Use android internal layout instead
 * rdq478       2011/10/07  IKMAIN-28811        Fixed force close and window leakd issues
 * rdq478       2011/10/21  IKMAIN-31135        Remove framework private resources & rework on wp chooser
 * mxdn83       2012/05/11  NA                  Updated with new guided UI style
 */

package com.motorola.contextual.pickers.actions;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.WallpaperService;
import com.motorola.contextual.actions.setwallpaper;

import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

/**
 * This class allows the user to select an image to be set as wallpaper as part of Rule activation.
 * <code><pre>
 * CLASS:
 *     Extends MultiScreenPickerActivity
 *
 * RESPONSIBILITIES:
 *     Does a startActivity with intent action GET_CONTENT and image mime type
 *     Packages Uri of the selected image in a intent and sends it to Rules Builder
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class SetWallpaperPickerActivity extends MultiScreenPickerActivity implements Constants {

    private static final String TAG = TAG_PREFIX + SetWallpaperPickerActivity.class.getSimpleName();
    private static final String LIVE_WALLPAPER_PKG = "com.android.wallpaper.livepicker";

    private Context mContext = null;
    private ProgressDialog mProgress = null;
    private boolean mRegisterReceiver = false;
    private boolean mShouldFinish = true;

    private boolean mListenerAltered = false;
    public static String sSelectedWallpaperFileName = null;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setActionBarTitle(getString(R.string.change_wallpaper_title));

        mContext = this.getApplicationContext();
        
        // check if fragment already created
        if (getFragmentManager().findFragmentByTag(getString(R.string.change_wallpaper)) == null) {
            launchNextFragment(LaunchAppPickerFragment.newInstance(
                    Intent.ACTION_SET_WALLPAPER,
                    null, 
                    new String[]{LIVE_WALLPAPER_PKG},
                    getString(R.string.change_wp_prompt), 
                    ListItem.typeTHREE), 
                R.string.change_wallpaper, 
                true);
        }
        
        // if a rule is active, temporarily disable the listener, so that it ignores
        // wallpaper change intent.
        if (setwallpaper.getListener(this)) {
            mListenerAltered = true;
            setwallpaper.setListener(this, false);
        }
        Intent configIntent = ActionHelper.getConfigIntent(getIntent()
                .getStringExtra(EXTRA_CONFIG));
        if (configIntent != null) {
            setSelectedWallpaperFileName(configIntent
                    .getStringExtra(EXTRA_URI));
        }
        saveCurrentWallpaper();
    }

    protected void onResume() {
        super.onResume();
        if (LOG_INFO) Log.i(TAG, "onResume called");

        if (mProgress == null) {

            WallpaperManager wallManager = WallpaperManager.getInstance(mContext);

            // Display warning message if the current wallpaper is Live Wallpaper
            if (wallManager.getWallpaperInfo() != null) {
                if (LOG_DEBUG) Log.d(TAG, "The current wallpaper is Live Wallpaper.");
                changeWallpaperDialog();
                mShouldFinish = true;
                return;
            }

            mShouldFinish = true;
        }
        else {
            mProgress.show();
        }
    }

    /**
     * This method starts WallpaperService for saving the current wallpaper in a
     * file
     */
    // Method copied from com.motorola.contextual.actions.SetWallpaperActivity
    private void saveCurrentWallpaper() {
        Intent serviceIntent = new Intent();
        serviceIntent.putExtra(EXTRA_WP_SAVE_CURRENT, true);
        serviceIntent.setClass(this, WallpaperService.class);
        startService(serviceIntent);
    }

    /**
     * This method starts WallpaperService for removing unused wallpapers stored
     * in files
     */
    // Method copied from com.motorola.contextual.actions.SetWallpaperActivity
    private void removeUnusedWallpapers() {
        Intent serviceIntent = new Intent();
        serviceIntent.putExtra(EXTRA_WP_PERFORM_CLEANUP, true);
        serviceIntent.setClass(this, WallpaperService.class);
        startService(serviceIntent);
    }

    protected void onDestroy() {
        super.onDestroy();
        removeUnusedWallpapers();
        if (mProgress != null) {
            mProgress.dismiss();
        }

        if ((mBroadcastReceiver != null) && (mRegisterReceiver)) {
            unregisterReceiver(mBroadcastReceiver);
        }

        // restore listener for active rule
        if (mListenerAltered) {
            setwallpaper.setListener(this, true);
        }
    }

    /**
     * Build the dialog to inform user that the current wallpaper is
     * Live Wallpaper, the user will not able to use this action until
     * he changes the wallpaper to a static image first.
     */
    private void changeWallpaperDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_wp_title);
        builder.setMessage(R.string.change_wp_msg);
        builder.setPositiveButton(R.string.settings, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mShouldFinish = true;
                dialog.dismiss();
                Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mShouldFinish = true;
                dialog.dismiss();
            }
        });

        final AlertDialog warningDialog = builder.create();
        warningDialog.show();
        warningDialog.setOnDismissListener(new OnDismissListener() {

            public void onDismiss(DialogInterface dialog) {
                /*
                 * Finish() the activity if this dialog dismiss before
                 * user make any selection, usually happen with back key.
                 */
                if (mShouldFinish) {
                    sendResultToCaller(RESULT_CANCELED, null);
                }
                else {
                    /*
                     * This is a dissmiss but the intent chooser dialog is still active,
                     * so don't finish now.
                     */
                    mShouldFinish = true;
                }
            }

        });
    }

    /**
     * Register to listen to WALLPAPER_CHANGED intent
     */
    private void registerListener() {
        if (!mRegisterReceiver) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_WALLPAPER_CHANGED);
            intentFilter.addAction(SETTING_WP_ACTION);
            registerReceiver(mBroadcastReceiver, intentFilter);
            mRegisterReceiver = true;
        }
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        private boolean mAcceptWallpaper = true;

        @Override
        public void onReceive(Context context, Intent intent) {

            if ((context == null) || (intent == null)) {
                Log.e(TAG, "Error: either context or intent is null.");
                sendResultToCaller(RESULT_CANCELED, null);
                return;
            }

            String action = intent.getAction();

            if (action == null) {
                Log.e(TAG, "Error: action = null");
                sendResultToCaller(RESULT_CANCELED, null);
            }
            else if ((action.equals(Intent.ACTION_WALLPAPER_CHANGED)) && (mAcceptWallpaper)) {
                // do not accept any more wallpaper changed intent
                mAcceptWallpaper = false;

                startWallpaperService(context);
            }
            else if (action.equals(SETTING_WP_ACTION)) {
                if (intent.getBooleanExtra(EXTRA_STATUS, false)) {
                    sendResultToCaller(RESULT_OK, intent.getStringExtra(EXTRA_URI));
                }
                else {
                    sendResultToCaller(RESULT_CANCELED, null);
                }
            }
        }

        /**
         * Wallpaper service to save newly set and restore previous wallpaper.
         * @param context
         */
        private void startWallpaperService(Context context) {

            Resources res = context.getResources();
            mProgress = new ProgressDialog(context);
            mProgress.setOnCancelListener(mProgressCancelListener);
            mProgress.setMessage(res.getString(R.string.saving_wallpaper));

            mShouldFinish = false;
            mProgress.show();
            // start wallpaper service to save, restore and delete wallpaper
            startService(new Intent(mContext, WallpaperService.class));
        }
    };

    /**
     * This method will send the result back to it caller.
     * @param result - either RESULT_OK or RESULT_CANCELED
     * @param fileName - wallpaper file name
     */
    private void sendResultToCaller(int result, String fileName) {
        if (!isFinishing()) {
            Intent resultIntent = new Intent();
            if (result == RESULT_OK) {
                resultIntent.putExtra(EXTRA_CONFIG, setwallpaper.getConfig(fileName));
                resultIntent.putExtra(EXTRA_DESCRIPTION, fileName);
                setSelectedWallpaperFileName(fileName);
            }
            setResult(result, resultIntent);

            if (LOG_DEBUG) {
                Log.d(TAG, "SendResult: result = " + result + " fileName = "
                        + fileName);
            }
            finish();
        }
    }

    /**
     * onCancelListener for progress dialog
     */
    private OnCancelListener mProgressCancelListener = new OnCancelListener() {
        // Required by OnCancelListener interface
        public void onCancel(DialogInterface dialog) {
            sendResultToCaller(RESULT_CANCELED, null);
        }
    };


    /**
     * Send intent to start activity which handle SET_WALLPAPER.
     * @param returnValue - return intent to start the wallpaper app
     */
    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {
        if(returnValue != null) {
            Intent i = (Intent) ((ListItem)returnValue).mMode;
            i.setAction(Intent.ACTION_SET_WALLPAPER);
            i.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
            startActivity(i);
            /*
             * Register to listen to WALLPAPER_CHANGED intent
             */
            registerListener();

            mShouldFinish = false;
        }
    }

    /**
     * Method for getting the file name of the selected wallpaper
     *
     * @return - the name of the file in which the selected wallpaper is saved
     */
    // Method copied from com.motorola.contextual.actions.SetWallpaperActivity
    public static String getSelectedWallpaperFileName() {
        return sSelectedWallpaperFileName;
    }

    /**
     * Method for setting the file name of the selected wallpaper
     *
     * @param fileName
     *            - the name of the file in which the selected wallpaper is
     *            saved
     */
    // Method copied from com.motorola.contextual.actions.SetWallpaperActivity
    private static void setSelectedWallpaperFileName(String fileName) {
        sSelectedWallpaperFileName = fileName;
    }

}

