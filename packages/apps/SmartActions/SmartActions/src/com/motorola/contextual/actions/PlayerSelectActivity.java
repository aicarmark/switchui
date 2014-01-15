/*
 * @(#)PlayerSelectActivity.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/06/08  NA                  Initial version
 *
 */

package com.motorola.contextual.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.motorola.contextual.smartrules.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * This class allows the user to select a player to be used by play a playlist action
 *
 * <code><pre>
 * CLASS:
 *     Extends Activity
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 *     Shows a dialog allowing the user to select a player to play selected playlist
 *     Sends the intent containing player and playlist details to Rules Builder.
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class PlayerSelectActivity extends Activity implements Constants {

    private static final int REQ_CODE = 1;
    private static final int PLAYER_SELECT_DIALOG_ID = 100;
    private static final String DIALOG_FRAGMENT_TAG = "PLAYER_SELECT_DIALOG";

    private boolean mShouldFinish = true;
    private List<IntentItem> mIntentItems = null;

    /**
     * This is the intent item class which holds the title, package name, activity name and icon
     * for generic playlist intent. This item will be shown in the chooser list,
     * and when the user clicks on it, selected player will be chosen to play the selected playlist.
     */
    private static class IntentItem {
        public String mTitle = null;
        public String mDeliverToPkg = null;
        public String mDeliverToActivity = null;
        public Drawable mIcon = null;

        /**
         * Constructor for IntentItem class
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent playIntent = new Intent(Intent.ACTION_VIEW);
        playIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        playIntent.setDataAndType(Uri.EMPTY, MediaStore.Audio.Playlists.CONTENT_TYPE);

        mIntentItems = queryIntentItems(playIntent);

        if (mIntentItems == null || mIntentItems.size() == 0) {
            //Error to be displayed on the playlist screen. No compatible player.
            launchPlaylistActivity(null, null);
            return;
        }

        if (mIntentItems.size() == 1) {
            //No need to show player list dialog. Select the available player by default.
            launchPlaylistActivity(mIntentItems.get(0).mDeliverToPkg, mIntentItems.get(0).mDeliverToActivity);
            return;
        }

        // Do a show Dialog, only when the activity is first created, don't do it for
        // orientation changes, the framework will do that for you
        if (savedInstanceState == null) {
            showPlayerSelectDialog();
        }
    }

    /**
     * Get a list of intent item objects based on the supplied intent.
     * This list is a list of apps which can handle the incoming intent.
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
     * Build the dialog for user to make a selection.
     * This dialog should not display Google play music application
     */
    private void showPlayerSelectDialog() {
        PlayerSelectDialog dialogFragment = PlayerSelectDialog.newInstance(PLAYER_SELECT_DIALOG_ID);
        dialogFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
    }

    /**
     * Method to be called when dialog is dismissed
     */
    public void onDialogDismissed() {
        /*
         * Finish() the activity if this dialog dismiss before
         * user make any selection, usually happen with back key.
         */
        if (mShouldFinish) {
            finish();
        }
    }

    /**
     * Method to get the list of intent items
     * @return List of intent items
     */
    public List<IntentItem> getIntentItems() {
        return mIntentItems;
    }

    /**
     * Method to set the flag which dictates whether to finish the activity when dialog is dismissed or not
     * @param shouldFinish
     */
    public void setShouldFinishFlag(boolean shouldFinish) {
        mShouldFinish = shouldFinish;
    }

    /**
     * Launch playlist selector activity
     * @param pkg Package of the selected player
     * @param activity Activity that is going to hand play a playlist broadcast
     */
    private void launchPlaylistActivity(String pkg, String activity) {
        Intent intent = new Intent(this, PlayPlaylistActivity.class);
        intent.putExtras(getIntent());
        if (pkg != null && activity != null) {
            intent.putExtra(EXTRA_PLAYER_COMPONENT, new ComponentName(pkg, activity).flattenToString());
        }
        startActivityForResult(intent, REQ_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE) {
            if (data != null) {
                setResult(RESULT_OK, data);
            }
            finish();
        }
    }

    /**
     * Dialog fragment to show intent items in a list
     */
    public static class PlayerSelectDialog extends DialogFragment implements Constants {

        private static final String DIALOG_ID = "DIALOG_ID";

        private PlayerSelectActivity mActivity;
        private ListAdapter mIntentAdapter = null;
        private List<IntentItem> mIntentItems;

        /**
         * Method for creating and initializing an instance of Fragment
         *
         * @param dialogId unique id for the dialog
         * @return - the initialized instance of BrightnessDialogFragment
         */
        public static PlayerSelectDialog newInstance(int dialogId) {
            PlayerSelectDialog dialogFragment = new PlayerSelectDialog();
            Bundle arguments = new Bundle();
            arguments.putInt(DIALOG_ID, dialogId);
            dialogFragment.setArguments(arguments);
            return dialogFragment;
        }

        /**
         * Public empty constructor
         */
        public PlayerSelectDialog() {
            // Nothing to be done here
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt(DIALOG_ID);
            mActivity = (PlayerSelectActivity)getActivity();
            mIntentItems = mActivity.getIntentItems();
            initializeAdapter();

            AlertDialog alertDialog = null;
            if (id == PLAYER_SELECT_DIALOG_ID) {
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setTitle(getResources().getString(R.string.select_music_player));
                builder.setAdapter(mIntentAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        mActivity.launchPlaylistActivity(mIntentItems.get(item).mDeliverToPkg,
                                mIntentItems.get(item).mDeliverToActivity);

                        mActivity.setShouldFinishFlag(false);
                        dialog.dismiss();
                    }
                });

                alertDialog = builder.create();
            }
            return alertDialog;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            mActivity.onDialogDismissed();
        }

        /**
         * Method to initialize the list adapter
         */
        public void initializeAdapter() {
            mIntentAdapter = new ArrayAdapter<IntentItem>(mActivity,
                    android.R.layout.select_dialog_item,
            android.R.id.text1, mIntentItems) {

                public View getView(int position, View convertView, ViewGroup parent) {
                    View view = null;
                    if (convertView == null) {
                        LayoutInflater mInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        view = mInflater.inflate(R.layout.resolve_list_item, parent, false);
                    }
                    else {
                        view = convertView;
                    }

                    TextView text = (TextView) view.findViewById(R.id.text1);
                    text.setText(mIntentItems.get(position).toString());
                    ImageView icon = (ImageView) view.findViewById(R.id.icon);
                    icon.setImageDrawable(mIntentItems.get(position).mIcon);

                    return view;
                }
            };
        }
    }

}
