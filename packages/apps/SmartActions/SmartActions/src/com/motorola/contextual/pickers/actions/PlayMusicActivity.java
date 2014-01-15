/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number          Brief Description
 * ------------- ---------- -----------------  ------------------------------
 * E11636        2012/07/02 Smart Actions 2.1  Initial Version
 */
package com.motorola.contextual.pickers.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.motorola.contextual.actions.ActionHelper;
import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.pickers.MultiScreenPickerActivity;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * The Activity that hosts the Play Music picker.
 * <code><pre>
 *
 * CLASS:
 *  extends MultiScreenPickerActivity
 *
 * RESPONSIBILITIES:
 *  This Activity hosts the multiple fragments that implement the Play Music Picker.
 *
 * COLLABORATORS:
 *  PlayMusicChoosePlayerFragment - the fragment that allows the user to choose the music player.
 *  PlayMusicPlaylistFragment - the fragment that allows the user to choose the play lists.
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class PlayMusicActivity extends MultiScreenPickerActivity implements Constants, PlayMusicChoosePlayerFragment.PlayMusicChoosePlayerDelegate {

    private List<IntentItem> mIntentItems = null;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarTitle(getString(R.string.play_music_title));

        Intent intent = getIntent();
        mInputConfigs = ActionHelper.getConfigIntent(intent.getStringExtra(Constants.EXTRA_CONFIG));
        if (mInputConfigs != null) {
            mInputConfigs.putExtra(EXTRA_RULE_ENDS, intent.getBooleanExtra(EXTRA_RULE_ENDS, false));
        } else {
            mInputConfigs = new Intent();
        }
        mOutputConfigs = new Intent();

        Intent playIntent = new Intent(Intent.ACTION_VIEW);
        playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        playIntent.setDataAndType(Uri.EMPTY, MediaStore.Audio.Playlists.CONTENT_TYPE);

        mIntentItems = queryIntentItems(playIntent);

        if (mIntentItems == null || mIntentItems.size() == 0) {
            //Error to be displayed on the playlist screen. No compatible player.
            launchNextFragment(PlayMusicPlaylistFragment.newInstance(mInputConfigs, mOutputConfigs), R.string.play_music_title, true);
        } else if (mIntentItems.size() == 1) {
            //No need to show player list dialog. Select the available player by default.
            String pkg = mIntentItems.get(0).mDeliverToPkg;
            String activity = mIntentItems.get(0).mDeliverToActivity;
            mInputConfigs.putExtra(EXTRA_PLAYER_COMPONENT, new ComponentName(pkg, activity).flattenToString());
            launchNextFragment(PlayMusicPlaylistFragment.newInstance(mInputConfigs, mOutputConfigs), R.string.play_music_title, true);
        } else {
            launchNextFragment(PlayMusicChoosePlayerFragment.newInstance(mInputConfigs, mOutputConfigs), R.string.play_music_title, true);
        }
    }


    @Override
    public void onReturn(Object returnValue, PickerFragment fromFragment) {

        if (returnValue == null) {
            //Just go back to the previous fragment without committing any changes.
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                finish();
            }
            return;
        }

        if (fromFragment instanceof PlayMusicChoosePlayerFragment) {
            launchNextFragment(PlayMusicPlaylistFragment.newInstance(mInputConfigs, mOutputConfigs), R.string.play_music_title, false);
        } else {
            // Returning from PlayMusicPlaylistFragment.
            setResult(RESULT_OK, (Intent)returnValue);
            finish();
        }
    }


    /**
     * Get a list of intent item objects based on the supplied intent.
     * This list is a list of apps which can handle the incoming intent.
     *
     * @param intent Intent to be handled
     * @return IntentItem list to handle provided intent
     */
    private List<IntentItem> queryIntentItems(Intent intent) {
        PackageManager pkgManager = getPackageManager();
        List<ResolveInfo> actLists = pkgManager.queryIntentActivities(intent, 0);

        if ((actLists != null) && (actLists.size() > 0)) {
            List<IntentItem> intentList = new ArrayList<IntentItem>();
            ResolveInfo.DisplayNameComparator comparator = new ResolveInfo.DisplayNameComparator(pkgManager);
            Collections.sort(actLists, comparator);

            for (ResolveInfo rInfo : actLists) {
                String pkg = rInfo.activityInfo.packageName;

                // check and ignore Google music
                if ((pkg != null) && !pkg.equals(GOOGLE_MUSIC_PKG)) {
                    intentList.add(new IntentItem(this, rInfo));
                }
            }

            return intentList;
        }

        return null;
    }


    /**
     * This is the intent item class which holds the title, package name, activity name and icon
     * for generic playlist intent. This item will be shown in the chooser list,
     * and when the user clicks on it, selected player will be chosen to play the selected playlist.
     */
    static class IntentItem {
        public String mTitle = null;
        public String mDeliverToPkg = null;
        public String mDeliverToActivity = null;
        public Drawable mIcon = null;

        /**
         * Constructor for IntentItem class
         *
         * @param context: application context
         * @param rInfo: ResolveInfo object containing details of this item
         */
        public IntentItem(Context context, ResolveInfo rInfo) {
            PackageManager pkgManager = context.getPackageManager();
            mTitle = (String) rInfo.loadLabel(pkgManager);
            mIcon = rInfo.loadIcon(pkgManager);
            mDeliverToPkg = rInfo.activityInfo.packageName;
            mDeliverToActivity = rInfo.activityInfo.name;
        }

        @Override
        public String toString() {
            return mTitle;
        }
    }
    
    public List<IntentItem> getPlayerIntentItems() {
        return mIntentItems;
    }
}
