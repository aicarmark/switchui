/*
 * @(#)PlayPlaylistActivity.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21383       2011/03/15  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.fragment.EditFragment;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

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

public class PlayPlaylistActivity extends ListActivity implements Constants,
    SimpleCursorAdapter.ViewBinder {

    private static final String TAG = TAG_PREFIX + PlayPlaylistActivity.class.getSimpleName();
    private Cursor mCursor = null;
    private String mEditId = null;
    private static final String SCHEME = "file";

    private static final int MESSAGE_QUERY_DONE = 1;
    private static final int MESSAGE_REQUERY = 2;

    private TextView mEmptyText;

    private String mPlayer;

    private BroadcastReceiver mSDCardReceiver;

    // Class used to store the values for each row
    private static class KeyValues {
        String  idOfPlaylist;
        String  playlistName;
    };

    private SimpleCursorAdapter mAdapter = null;

    private String[] from = {
        MediaStore.Audio.Playlists.NAME,
        MediaStore.Audio.Playlists._ID
    };

    private int[] to = {
        R.id.playlist_name,
        R.id.selected_playlist
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play_playlist);

        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.pick_playlist);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.show();

        // Add menu items from fragment
        Fragment fragment = EditFragment.newInstance(EditFragment.EditFragmentOptions.HIDE_SAVE, false);
        getFragmentManager().beginTransaction().replace(R.id.edit_fragment_container, fragment, null).commit();

        mEmptyText = (TextView)findViewById(R.id.empty_text);

        Intent launchIntent = getIntent();
        mPlayer = launchIntent.getStringExtra(EXTRA_PLAYER_COMPONENT);

        if (mPlayer != null) {
            Intent configIntent = ActionHelper.getConfigIntent(launchIntent.getStringExtra(Constants.EXTRA_CONFIG));
            if (configIntent != null) {
                // edit case
                mEditId = configIntent.getStringExtra(EXTRA_PLAYLIST_ID);
                if (LOG_INFO) Log.i(TAG, "Edit case "+ mEditId);
            }

            setUpSdCardReceiver();

            mAdapter = new SimpleCursorAdapter(PlayPlaylistActivity.this,
                    R.layout.playlist_list_item, mCursor, from, to);

            //Querying for playlists to be done only if a player has been selected
            Thread t = new Thread(new QueryPlaylists());
            t.start();
        } else {
            mEmptyText.setText(getString(R.string.no_music_player_found));
            mEmptyText.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onDestroy() {
        if (mSDCardReceiver != null) {
            //Receiver is registered only when a compatible player is found
            unregisterReceiver(mSDCardReceiver);
        }
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        super.onDestroy();
    }

    /** onOptionsItemSelected()
     *  handles the back press of icon in the ICS action bar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
        case android.R.id.home:
        case R.id.edit_cancel:
            finish();
            result = true;
            break;
        }
        return result;
    }

    /** sets the view value for each row.
    *
    * @param view - the view to bind the data to
    * @param cursor - cursor with data
    * @param columnIndex - column index for which the view value is being set
    * @return true
    */
    //@Override
    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

        int id = view.getId();

        if(id == R.id.playlist_name && view instanceof TextView) {

            ((TextView) view).setText(cursor.getString(
                                          cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME)));

        } else if(id == R.id.selected_playlist && view instanceof RadioButton) {
            RadioButton button = (RadioButton)view;
            button.setChecked(false);

            // onListItemClick for custom ListView layouts is disabled, if the layout
            // contains radio buttons, the following statements make the ListView clickable
            button.setFocusable(false);
            button.setFocusableInTouchMode(false);


            //Store id and playlist details for later use
            KeyValues tagValues = new KeyValues();
            tagValues.idOfPlaylist = cursor.getString(
                                         cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
            tagValues.playlistName = cursor.getString(
                                         cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME));
            view.setTag(tagValues);

            //For edit case, check the radiobutton of the selected playlist
            if(mEditId != null && mEditId.equals(tagValues.idOfPlaylist)) {
                button.setChecked(true);
            }

            button.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    uncheckAllItems(getListView());
                    if (v instanceof RadioButton)
                        ((RadioButton)v).setChecked(true);

                    KeyValues tagValues = (KeyValues)v.getTag();
                    if(LOG_INFO) Log.i(TAG, "Selected playlist details: "+ tagValues.idOfPlaylist);
                    setResult(RESULT_OK, prepareResultIntent(tagValues));
                    finish();
                }
            });

        }
        return true;
    }



    /** handles the click events in the list
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Cursor c = (Cursor)l.getItemAtPosition(position);

        KeyValues tagValues = new KeyValues();
        tagValues.idOfPlaylist = "";
        tagValues.playlistName = "";
        if(c != null) {
            tagValues.idOfPlaylist = c.getString(
                                         c.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
            tagValues.playlistName = c.getString(
                                         c.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME));

            uncheckAllItems(l);
            RadioButton button = (RadioButton)v.findViewById(R.id.selected_playlist);
            button.setChecked(true);
        } else {
            Log.e(TAG, "Null Cursor");
        }

        if(LOG_INFO) Log.i(TAG, "Selected playlist details: "+ tagValues.idOfPlaylist + "," + tagValues.playlistName);
        setResult(RESULT_OK, prepareResultIntent(tagValues));
        finish();
    }

    /**
     * Method to initialize broadcast receiver to monitor SD Card insertion/removal
     */
    private void setUpSdCardReceiver() {
        mSDCardReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context arg0, Intent intent) {
                String action = intent.getAction();

                if (action == null) {
                    Log.e(TAG, "Null action");
                    return;
                }

                if(mHandler != null)
                    mHandler.sendEmptyMessage(MESSAGE_REQUERY);
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addDataScheme(SCHEME);
        registerReceiver(mSDCardReceiver, filter);
    }

    /**
     * Prepares the result intent to be returned to Rules Builder via activity result
     *
     * @param KeyValues
     * @return result intent
     */
    private Intent prepareResultIntent(KeyValues tagValues) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_CONFIG, PlayPlaylist.getConfig(tagValues.idOfPlaylist,
                tagValues.playlistName, mPlayer));
        intent.putExtra(EXTRA_DESCRIPTION, tagValues.playlistName);
        intent.putExtra(EXTRA_RULE_ENDS, false);
        return intent;

    }

    private void uncheckAllItems (ListView listView) {
        if (listView != null) {
            for (int i=0; i<listView.getChildCount(); i++) {
                RadioButton radioButton = (RadioButton) listView.getChildAt(i).findViewById(R.id.selected_playlist);
                radioButton.setChecked(false);
            }
        }
    }

    /**
     *  Defines the handler which receives the asynchronous message after querying database
     *  for the available list of playlists.
    */
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_QUERY_DONE: {
                if(mCursor == null ) {
                    Log.e(TAG, " Cursor null");
                    mEmptyText.setVisibility(View.VISIBLE);
                }
                else {
                    if(!mCursor.moveToFirst()) {
                        Log.e(TAG, " There are zero rows in the table ");
                        mEmptyText.setVisibility(View.VISIBLE);
                        return;
                    }
                    // This will close the existing cursor
                    mAdapter.changeCursor(mCursor);
                    if (mAdapter.getViewBinder() == null) {
                        mAdapter.setViewBinder(PlayPlaylistActivity.this);
                    }
                    setListAdapter(mAdapter);
                }
                break;
            }
            case MESSAGE_REQUERY: {
                Thread t = new Thread(new QueryPlaylists());
                t.start();
                break;
            }
            }
        }
    };

    /**
     * Helper class to query playlists from database
     *
     * CLASS - Implements Runnable
     */
    private final class QueryPlaylists implements Runnable {
        public void run() {

            try {
                mCursor = getContentResolver().query(
                        MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Playlists._ID,
                                MediaStore.Audio.Playlists.NAME }, null, null,
                        null);
            } catch(Exception e) {
                Log.e(TAG, "Received exception while querying " + e.toString());
            }
            if(mHandler != null)
                mHandler.sendEmptyMessage(MESSAGE_QUERY_DONE);
        }
    }

}
