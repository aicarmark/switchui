/*
 * @(#)PlayMusicPlaylistFragment.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21383        2011/03/15 NA                Initial version: PlayPlaylistActivity
 * XPR643        2012/06/28 Smart Actions 2.1 Refactor to use UI base classes
 *
 */
package com.motorola.contextual.pickers.actions;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.PlayPlaylist;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This class allows the user to select a playlist that is to be played as part of Rule activation.
 *
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *     Implements Constants and SimpleCursorAdapter.ViewBinder
 *
 * RESPONSIBILITIES:
 *     Shows an activity allowing the user to select a playlist.
 *     Sends the intent containing the Id of the selected playlist
 *     to Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class PlayMusicPlaylistFragment extends PickerFragment implements Constants,
        DialogInterface.OnClickListener {
    private static final String TAG = TAG_PREFIX + PlayMusicPlaylistFragment.class.getSimpleName();

    /**
     * Keys for saving and restoring instance state bundle.
     */
    private interface Key {
        /** Bundle key for selected playlist ID */
        String PLAYLIST_ID = "com.motorola.contextual.pickers.actions.KEY_PLAYLIST_ID";
        /** Bundle key for selected playlist name */
        String PLAYLIST_NAME = "com.motorola.contextual.pickers.actions.KEY_PLAYLIST_NAME";
    }

    /** Boolean to indicate no playlists */
    private static final String MARK_NO_PLAYLISTS = "com.motorola.contextual.pickers.actions.MARK_NO_PLAYLISTS";;

    private Cursor mCursor = null;
    private String mEditId = null;
    private static final String SCHEME = "file";

    private static final int MESSAGE_QUERY_DONE = 1;
    private static final int MESSAGE_REQUERY = 2;

    private ListView mListView;
    private TextView mEmptyText;

    private String mPlayer;

    private BroadcastReceiver mSDCardReceiver;

    /** List index of current playlist selection */
    private int mSelectedIndex = -1;

    /** List view adapter that populates the playlists */
    private CursorAdapter mAdapter = null;

    /** Positive confirmation button to save and dismiss this fragment */
    private Button mButtonPositive = null;

    // Class used to store the values of a playlist row
    private static class KeyValues {
        String idOfPlaylist = null;
        String playlistName = null;
    };

    /** Current playlist selection information */
    protected KeyValues mPlaylistInfo = null;

    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIGS_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIGS_INTENT";

    public static PlayMusicPlaylistFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {

        Bundle args = new Bundle();

        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }

        PlayMusicPlaylistFragment f = new PlayMusicPlaylistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlaylistInfo = new KeyValues();

        if (getArguments() != null) {
            if (getArguments().getParcelable(INPUT_CONFIGS_INTENT) != null) {
                mInputConfigs = (Intent) getArguments().getParcelable(INPUT_CONFIGS_INTENT);
            }

            if (getArguments().getParcelable(OUTPUT_CONFIGS_INTENT) != null) {
                mOutputConfigs = (Intent) getArguments().getParcelable(OUTPUT_CONFIGS_INTENT);
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
        final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (mContentView == null) {
            mContentView = buildView(inflater, container, savedInstanceState);
        }
        return mContentView;
    }

    /**
     * Build playlist list if any exist; else show help view.
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    private View buildView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        View v = null;
        mPlayer = mInputConfigs.getStringExtra(EXTRA_PLAYER_COMPONENT);
        if ((mPlayer == null) || (mInputConfigs.getBooleanExtra(MARK_NO_PLAYLISTS, false))) {
            v = inflater.inflate(R.layout.play_music_playlist, container, false);
            mListView = (ListView)v.findViewById(R.id.list);
            mEmptyText = (TextView)v.findViewById(R.id.empty_text);
            mEmptyText.setText(Html.fromHtml(getString(R.string.no_playlist_found)));
            mEmptyText.setVisibility(View.VISIBLE);
            mAdapter = null;
            mButtonPositive = null;
        } else {
            if (mInputConfigs != null) {
                // edit case
                mEditId = mInputConfigs.getStringExtra(EXTRA_PLAYLIST_ID);
                if (mPlaylistInfo.idOfPlaylist == null) {
                    mPlaylistInfo.idOfPlaylist = mEditId;
                }
                if (LOG_INFO) Log.i(TAG, "Edit case "+ mEditId);
            }
            setUpSdCardReceiver();
            v = new Picker.Builder(mHostActivity)
                    .setTitle(Html.fromHtml(getString(R.string.play_music_playlist_prompt)))
                    .setSingleChoiceItems(mCursor, mSelectedIndex, MediaStore.Audio.Playlists.NAME, this)
                    .setPositiveButton(R.string.iam_done, this)
                    .create()
                    .getView();
            // Configure the button enabled state
            mButtonPositive = (Button) v.findViewById(R.id.button1);
            mButtonPositive.setEnabled(mSelectedIndex >= 0);

            // Retrieve the list view
            mListView = (ListView) v.findViewById(R.id.list);
            // Retrieve the list view adapter
            mAdapter = (CursorAdapter) mListView.getAdapter();
        }
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((mPlayer != null) && (mHandler != null)) {
            //Querying for playlists to be done only if a player has been selected
            mHandler.sendEmptyMessage(MESSAGE_REQUERY);
        }
    }

    /**
     * Restores prior instance state in onCreateView.
     * Namely, current list selection index.
     *
     * @param savedInstanceState Bundle containing prior instance state
     */
    @Override
    protected void restoreInstanceState(final Bundle savedInstanceState) {
        mPlaylistInfo.idOfPlaylist = savedInstanceState.getString(Key.PLAYLIST_ID);
        mPlaylistInfo.playlistName = savedInstanceState.getString(Key.PLAYLIST_NAME);
    }

    /**
     * Saves current instance state. Namely, current list selection index.
     *
     * @param outState Bundle for saving current instance
     * @see android.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(Key.PLAYLIST_ID, mPlaylistInfo.idOfPlaylist);
        outState.putString(Key.PLAYLIST_NAME, mPlaylistInfo.playlistName);
    }

    @Override
    public void onDestroy() {
        if (mSDCardReceiver != null) {
            //Receiver is registered only when a compatible player is found
            mHostActivity.unregisterReceiver(mSDCardReceiver);
        }
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        super.onDestroy();
    }

    /**
     * Method to initialize broadcast receiver to monitor SD Card insertion/removal
     */
    private void setUpSdCardReceiver() {
        mSDCardReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context arg0, final Intent intent) {
                if (intent == null) {
                    Log.e(TAG, "Null intent");
                    return;
                }
                final String action = intent.getAction();
                if (action == null) {
                    Log.e(TAG, "Null action");
                    return;
                }
                if (mHandler != null) {
                    mHandler.sendEmptyMessage(MESSAGE_REQUERY);
                }
            }
        };
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme(SCHEME);
        mHostActivity.registerReceiver(mSDCardReceiver, filter);
    }

    /**
     * Prepares the result intent to be returned to Rules Builder via activity result
     *
     * @param KeyValues
     * @return result intent
     */
    private void prepareResultIntent(final Intent intent, final KeyValues tagValues) {
        intent.putExtra(EXTRA_CONFIG, PlayPlaylist.getConfig(tagValues.idOfPlaylist,
                tagValues.playlistName, mPlayer));
        intent.putExtra(EXTRA_DESCRIPTION, tagValues.playlistName);
        intent.putExtra(EXTRA_RULE_ENDS, false);
    }

    /**
     *  Defines the handler which receives the asynchronous message after querying database
     *  for the available list of playlists.
    */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
            case MESSAGE_QUERY_DONE: {
                if(mCursor == null) {
                    Log.e(TAG, " Cursor null");
                    // Reset row selection to none
                    mSelectedIndex = -1;
                    showHelpView();
                } else if (!mCursor.moveToFirst()) {
                    Log.e(TAG, " There are zero rows in the table ");
                    // Reset row selection to none
                    mSelectedIndex = -1;
                    showHelpView();
                } else if (mAdapter != null) {
                    mInputConfigs.removeExtra(MARK_NO_PLAYLISTS);
                    mSelectedIndex = getSelectionIndex(mPlaylistInfo.idOfPlaylist);
                    populatePlaylistInfo(mSelectedIndex);
                    final Cursor oldCurs = mAdapter.swapCursor(mCursor);
                    if ((oldCurs != null) && (!oldCurs.isClosed())) {
                        oldCurs.close();
                    }
                    // Refresh playlists list
                    mAdapter.notifyDataSetChanged();
                    if (mSelectedIndex == -1) {
                        mListView.clearChoices();
                    } else {
                        mListView.setItemChecked(mSelectedIndex, true);
                        mListView.setSelection(mSelectedIndex);
                    }
                    if (mButtonPositive != null) {
                        mButtonPositive.setEnabled(mSelectedIndex >= 0);
                    }
                }
                break;
            }
            case MESSAGE_REQUERY: {
                final Thread t = new Thread(new QueryPlaylists());
                t.start();
                break;
            }
            }
        }
    };

    /**
     * Shows help text view when no playlists exist.
     */
    protected void showHelpView() {
        mInputConfigs.putExtra(MARK_NO_PLAYLISTS, true);
        final Fragment nextFragment = PlayMusicPlaylistFragment.newInstance(mInputConfigs, mOutputConfigs);
        final FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, nextFragment);
            fragmentTransaction.setBreadCrumbShortTitle(R.string.play_music_title);
            fragmentTransaction.commit();
        }
    }

    /**
     * Helper class to query playlists from database
     *
     * CLASS - Implements Runnable
     */
    private final class QueryPlaylists implements Runnable {
        public void run() {
            try {
                mCursor = mHostActivity.getContentResolver().query(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Playlists._ID,
                                MediaStore.Audio.Playlists.NAME }, null, null,
                        null);
            } catch(final Exception e) {
                Log.e(TAG, "Received exception while querying " + e.toString());
            }
            if(mHandler != null) {
                mHandler.sendEmptyMessage(MESSAGE_QUERY_DONE);
            }
        }
    }

    /**
     * Finds cursor index of given playlist ID, if any.
     *
     * @param playlistId Player playlist ID
     * @return Cursor index of playlist ID
     * @return -1 if no matching playlist ID
     */
    private int getSelectionIndex(final String playlistId) {
        int index = -1;
        if ((playlistId != null) && (mCursor != null)) {
            for (int pos = 0; pos < mCursor.getCount(); ++pos) {
                if (mCursor.moveToPosition(pos)) {
                    final String id = mPlaylistInfo.idOfPlaylist = mCursor.getString(
                            mCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
                    if ((id != null) && playlistId.equals(id)) {
                        index = pos;
                    }
                }
            }
        }
        return index;
    }

    /**
     * Populates playlist ID and name from given cursor index.
     *
     * @param index Cursor index
     */
    private void populatePlaylistInfo(final int index) {
        if ((index >= 0) && (mCursor != null) && (mCursor.getCount() > index) &&
                mCursor.moveToPosition(index)) {
            mPlaylistInfo.idOfPlaylist = mCursor.getString(
                    mCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
            mPlaylistInfo.playlistName = mCursor.getString(
                    mCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME));
        } else {
            mPlaylistInfo.idOfPlaylist = null;
            mPlaylistInfo.playlistName = null;
        }
    }

    /**
     * Required by DialogInterface.OnClickListener interface.
     * Handles click of list item or done button.
     *
     * @param dialog Object that implements DialogInterface
     * @param which Identifier of button that was clicked
     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
     */
    public void onClick(final DialogInterface dialog, final int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            prepareResultIntent(mOutputConfigs, mPlaylistInfo);
            mHostActivity.onReturn(mOutputConfigs, PlayMusicPlaylistFragment.this);
            break;
        case DialogInterface.BUTTON_NEUTRAL:
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        default:
            mSelectedIndex = which;
            populatePlaylistInfo(mSelectedIndex);
            break;
        }
    }
}
