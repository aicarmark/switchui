/*
 * @(#)PlayPlaylist.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/04/04  NA                  Initial version
 *
 */
package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android_const.app.MediaConst;

/**
 * This class represents Play playlist action
 *
 * <code><pre>
 * CLASS:
 *     Extends StatelessAction
 *
 * RESPONSIBILITIES:
 *    This class implements the methods needed to respond to fire, refresh
 *    and list commands. It gets the necessary information in the form of an Intent
 *
 * COLABORATORS:
 *     Smart Actions Core
 *
 * USAGE:
 *      See individual methods.
 *
 * </pre></code>
 **/

public class PlayPlaylist extends StatelessAction implements Constants {

    public static final String TAG = TAG_PREFIX + PlayPlaylist.class.getSimpleName();

    @Override
    public ReturnValues fireAction(Context context, Intent configIntent) {
        ReturnValues retValues = new ReturnValues();
        retValues.status = true;
        new PlayPlaylistTask(context, configIntent).execute();
        return retValues;
    }

    @Override
    public String getActionString(Context context) {
        return context.getString(R.string.play_playlist);
    }

    @Override
    public String getDescription(Context context, Intent configIntent) {
        return (configIntent != null) ? configIntent.getStringExtra(EXTRA_TITLE) : null;
    }

    @Override
    public String getUpdatedConfig(Context context, Intent configIntent) {
        return getConfig(configIntent.getStringExtra(EXTRA_PLAYLIST_ID),
                configIntent.getStringExtra(EXTRA_DESCRIPTION),
                configIntent.getStringExtra(EXTRA_PLAYER_COMPONENT));
    }

    @Override
    public boolean isResponseAsync() {
        return true;
    }

    @Override
    public boolean validateConfig(Intent configIntent) {
        String playlistId = configIntent.getStringExtra(EXTRA_PLAYLIST_ID);
        String playerComponent = configIntent.getStringExtra(EXTRA_PLAYER_COMPONENT);
        return playlistId != null && playerComponent != null;
    }

    /**
     * Method to get config based on supplied parameters
     *
     * @param playlistId ID of the selected playlist
     * @param title Playlist title
     * @param playerComponent Selected player to play the playlist
     * @return Config
     */
    public static String getConfig (String playlistId, String title, String playerComponent) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG_VERSION, INITIAL_VERSION);
        intent.putExtra(EXTRA_PLAYLIST_ID, playlistId);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_PLAYER_COMPONENT, playerComponent);
        return intent.toUri(0);
    }

    /**
     * Task to play a playlist if it exists in the system
     */
    private class PlayPlaylistTask extends AsyncTask<Void, Void, Void> {
        private Context mContext;
        private Intent mConfigIntent;
        private boolean mStatus;
        private String mPlaylistId;
        private ComponentName mPlayerComponent;

        public PlayPlaylistTask(Context context, Intent configIntent) {
            mContext = context;
            mConfigIntent = configIntent;
            mStatus = true;
            mPlaylistId = mConfigIntent.getStringExtra(EXTRA_PLAYLIST_ID);

            String playerComponent = mConfigIntent.getStringExtra(EXTRA_PLAYER_COMPONENT);
            if (playerComponent != null) {
                mPlayerComponent = ComponentName.unflattenFromString(playerComponent);
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            //Query the music DB
            mStatus = mPlayerComponent != null &&
                    Utils.isPlaylistPresent(mContext, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mPlaylistId);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        	if(LOG_INFO) Log.i(TAG, "Playing playlist : " + mConfigIntent.getStringExtra(EXTRA_TITLE) + 
        			", mStatus="+mStatus);
            if (mStatus) {
                try {
                    Intent playIntent = new Intent(Intent.ACTION_VIEW);
                    playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    playIntent.setComponent(mPlayerComponent);
                    playIntent.setDataAndType(Uri.EMPTY, MediaStore.Audio.Playlists.CONTENT_TYPE);
                    playIntent.putExtra(MediaConst.PLAYLIST_EXTRA, String.valueOf(mPlaylistId));
                    mContext.startActivity(playIntent);
                } catch (Exception e) {
                    mStatus = false;
                    Log.e(TAG, "Exception while playing playlist");
                }
            }
            Intent statusIntent = new Intent(ACTION_PUBLISHER_EVENT);
            statusIntent.putExtra(EXTRA_EVENT_TYPE, EXTRA_FIRE_RESPONSE);
            statusIntent.putExtra(EXTRA_PUBLISHER_KEY, PLAYLIST_ACTION_KEY);
            statusIntent.putExtra(EXTRA_RESPONSE_ID, mConfigIntent.getStringExtra(EXTRA_RESPONSE_ID));
            statusIntent.putExtra(EXTRA_DEBUG_REQRESP, mConfigIntent.getStringExtra(EXTRA_DEBUG_REQRESP));
            ActionHelper.sendActionStatus(mContext, statusIntent, mStatus, null);
        }

    }

}
