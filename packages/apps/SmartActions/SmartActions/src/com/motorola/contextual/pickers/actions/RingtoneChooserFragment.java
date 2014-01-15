/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/05/17 Smart Actions 2.1 Initial Version
 * E11636        2012/07/19 Monkey issue      Fix for that
 */
package com.motorola.contextual.pickers.actions;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.smartrules.R;

/**
 * This fragment presents a ringtone chooser.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment - fragment for interacting with a Smart Actions container activity
 *
 * RESPONSIBILITIES:
 *  Present a ringtone chooser that plays the current selection.
 *
 * COLLABORATORS:
 *  RingtoneChooserActivity.java - Launches ringtone chooser fragment and collects results
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class RingtoneChooserFragment extends PickerFragment implements Runnable, OnClickListener {
    protected static final String TAG = RingtoneChooserFragment.class.getSimpleName();

    /** Bundle key for current ringtone selection in list */
    private static final String KEY_SELECTED_RINGTONE = "com.motorola.contextual.pickers.actions.KEY_SELECTED_RINGTONE";

    /** Delay before playing new ringtone selection */
    private static final int DELAY_MS_SELECTION_PLAYED = 300;

    /** Manager for ringtones and its cursor */
    private RingtoneManager mRingtoneManager = null;

    /** Cursor from RingtoneManager */
    private Cursor mCursor = null;

    /** Handler for background operations */
    private Handler mHandler = null;

    /** List index of current ringtone selection */
    private int mSampleRingtonePos = -1;

    private final Object mCursorSync = new Object();

    
    public static RingtoneChooserFragment newInstance() {
        Bundle args = new Bundle();
        args.putInt(KEY_SELECTED_RINGTONE, -1);
        RingtoneChooserFragment f = new RingtoneChooserFragment();
        f.setArguments(args);
        return f;
    }

    /**
     * Creates fragment initial state.
     *
     * @param savedInstanceState Bundle from previous instance; else null
     * @see android.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            mSampleRingtonePos = getArguments().getInt(KEY_SELECTED_RINGTONE, -1);
        }
        
        mHandler = new Handler();
    }

    /**
     * Creates this fragment view.
     *
     * @param inflater LayoutInflator for inflating layouts
     * @param container ViewGroup containing the view
     * @param savedInstanceState Bundle from previous instance; else null
     * @see android.app.Fragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (mContentView == null) {
            // RingtoneManager manages ringtones cursor
            mRingtoneManager = new RingtoneManager(mHostActivity);
            mRingtoneManager.setIncludeDrm(true);

            // Get the types of ringtones to show
            final Intent intent = mHostActivity.getIntent();
            final int types = intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1);
            if (types != -1) {
                mRingtoneManager.setType(types);
            }

            // Ringtones cursor is managed by RingtoneManager, so no leak worries
            mCursor = mRingtoneManager.getCursor();

            // The volume keys will control the stream that we are choosing a ringtone for
            mHostActivity.setVolumeControlStream(mRingtoneManager.inferStreamType());

            if (mSampleRingtonePos < 0) {
                // Get the URI whose list item should be checked
                final Uri existingUri = (Uri) intent
                        .getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
                mSampleRingtonePos = mRingtoneManager.getRingtonePosition(existingUri);
            }

            // Create the actual view to show
            mContentView = new Picker.Builder(mHostActivity)
                    .setTitle(Html.fromHtml(getString(R.string.ringtone_question)))
                    .setSingleChoiceItems(mCursor, mSampleRingtonePos, MediaStore.Audio.Media.TITLE, this)
                    .setPositiveButton(R.string.iam_done, this)
                    .create()
                    .getView();
        }
        return mContentView;
    }

    /**
     * Restores prior instance state in onCreateView.
     * Namely, current list selection index.
     *
     * @param savedInstanceState Bundle containing prior instance state
     */
    @Override
    protected void restoreInstanceState(final Bundle savedInstanceState) {
        mSampleRingtonePos = savedInstanceState.getInt(KEY_SELECTED_RINGTONE, -1);
    }

    /**
     * Saves current instance state. Namely, current list selection index.
     *
     * @param outState Bundle for saving current instance
     * @see android.app.Fragment#onSaveInstanceState(android.os.Bundle)
     */
    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putInt(KEY_SELECTED_RINGTONE, mSampleRingtonePos);
        super.onSaveInstanceState(outState);
    }

    /**
     * Required by OnClickListener interface.
     * Handles click of list item or done button.
     *
     * @param dialog Object that implements DialogInterface
     * @param which Identifier of button that was clicked
     * @see android.content.DialogInterface.OnClickListener#onClick(android.content.DialogInterface, int)
     */
    public void onClick(final DialogInterface dialog, final int which) {
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            onPositiveButtonClicked();
            break;
        case DialogInterface.BUTTON_NEUTRAL:
        case DialogInterface.BUTTON_NEGATIVE:
            break;
        default:
            playRingtone(which, DELAY_MS_SELECTION_PLAYED);
        }
    }

    /**
     * Plays ringtone at given position in list.
     *
     * @param position Ringtone selection position in list (0-based)
     * @param delayMs Delay before playing (ms)
     */
    private void playRingtone(final int position, final int delayMs) {
        mHandler.removeCallbacks(this);
        mSampleRingtonePos = position;
        mHandler.postDelayed(this, delayMs);
    }

    /**
     * Required by Runnable interface.
     * Plays current ringtone selection.
     *
     * @see java.lang.Runnable#run()
     */
    public void run() {
        synchronized (mCursorSync) {
            if (!mCursor.isClosed()) {
                final Ringtone ringtone = mRingtoneManager.getRingtone(mSampleRingtonePos);
                if (ringtone != null) {
                    ringtone.play();
                }
            }
        }
    }

    /**
     * Stops any currently playing ringtone, if any.
     *
     * @see android.app.Fragment#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();
        stopAnyPlayingRingtone();
    }

    /**
     * Stops any currently playing ringtone, if any.
     */
    private void stopAnyPlayingRingtone() {
        if (mRingtoneManager != null) {
            mRingtoneManager.stopPreviousRingtone();
        }
    }

    /**
     * Handles done button click by returning ringtone Uri as activity result.
     */
    private void onPositiveButtonClicked() {
        // Stop playing the previous ringtone
        mRingtoneManager.stopPreviousRingtone();

        // Create results intent
        final Intent resultIntent = new Intent();
        final Uri uri = mRingtoneManager.getRingtoneUri(mSampleRingtonePos);
        resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, uri);

        // The following lines are potential cause of a monkey failure.
        // It doesn't really save much resources here since the cursor would
        // be close in just a bit.  Cursor.deactivate() is also a deprecated call.
        //
        //        mHostActivity.getWindow().getDecorView().post(new Runnable() {
        //            public void run() {
        //                mCursor.deactivate();
        //            }
        //        });
        mHostActivity.onReturn(resultIntent, this);
    }

    /**
     * Closes cursor, if not null.
     *
     * @see android.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        synchronized (mCursorSync) {
            if (mCursor != null && !mCursor.isClosed()) {
                // The stop here is a safeguard since Ringtone.play()
                // may have already begun asynchronously, causing
                // a Monkey crash.
                stopAnyPlayingRingtone();
                // Closing the cursor will cause mRingtoneManager.getRingtone(mSampleRingtonePos)
                // to crash in Monkey.  Safeguarding this with the sync block doesn't help.
                // So apparently, getRingtone is an async call inside RingtoneManager.
                // Looking at the Android system RingtonePickerActivity, it does not close
                // the cursor either.  So I'm taking a leap of faith here that we don't need
                // to close the cursor.
                //
                //mCursor.close();
            }
        }
        super.onDestroy();
    }
}
