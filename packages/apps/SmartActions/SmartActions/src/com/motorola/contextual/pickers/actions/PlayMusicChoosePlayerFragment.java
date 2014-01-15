/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E11636        2012/07/02 Smart Actions 2.1 Initial Version
 * XPR643        2012/07/04 Smart Actions 2.1 Code review refactoring
 */
package com.motorola.contextual.pickers.actions;

import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.motorola.contextual.actions.Constants;
import com.motorola.contextual.actions.Utils;
import com.motorola.contextual.pickers.ListItem;
import com.motorola.contextual.pickers.Picker;
import com.motorola.contextual.pickers.PickerFragment;
import com.motorola.contextual.pickers.actions.PlayMusicActivity.IntentItem;
import com.motorola.contextual.smartrules.R;

/**
 * This Fragment class implements the UI to choose a music player.
 * <code><pre>
 *
 * CLASS:
 *  extends PickerFragment
 *
 * RESPONSIBILITIES:
 *  Query the list of music players installed on the device and present them as a
 *  list for the user to choose.
 *
 * COLLABORATORS:
 *  PlayMusicActivity.java - the Activity which hosts this Fragment.
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class PlayMusicChoosePlayerFragment extends PickerFragment implements Constants, OnClickListener {
    private Intent mInputConfigs, mOutputConfigs;
    private ListView mListView;
    private List<IntentItem> mIntentItems;
    
    public interface PlayMusicChoosePlayerDelegate {
        List<IntentItem> getPlayerIntentItems();
    }

    private PlayMusicChoosePlayerDelegate mPlayMusicChoosePlayerDelegate;
    
    private static final String INPUT_CONFIGS_INTENT = "INPUT_CONFIGS_INTENT";
    private static final String OUTPUT_CONFIGS_INTENT = "OUTPUT_CONFIGS_INTENT";

    public static PlayMusicChoosePlayerFragment newInstance(final Intent inputConfigs, final Intent outputConfigs) {

        Bundle args = new Bundle();

        if (inputConfigs != null) {
            args.putParcelable(INPUT_CONFIGS_INTENT, inputConfigs);
        }

        if (outputConfigs != null) {
            args.putParcelable(OUTPUT_CONFIGS_INTENT, outputConfigs);
        }

        PlayMusicChoosePlayerFragment f = new PlayMusicChoosePlayerFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mPlayMusicChoosePlayerDelegate = (PlayMusicChoosePlayerDelegate) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() +
                    " must implement PlayMusicChoosePlayerDelegate");
        }
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        final String playerComponent = mInputConfigs.getStringExtra(EXTRA_PLAYER_COMPONENT);
        ComponentName name = null;
        if (playerComponent != null) {
            name = ComponentName.unflattenFromString(playerComponent);
        }

        mIntentItems = mPlayMusicChoosePlayerDelegate.getPlayerIntentItems();
        
        final ListItem items[] = new ListItem[mIntentItems.size()];
        int checked = -1;

        for (int i = 0; i < items.length; i++) {
            items[i] = new ListItem(mIntentItems.get(i).mIcon,
                    mIntentItems.get(i).mTitle, null,
                    ListItem.typeONE, mIntentItems.get(i), null);
            if (name != null && name.getClassName().equals(mIntentItems.get(i).mDeliverToActivity)) {
                checked = i;
            }
        }

        final Picker.Builder builder = new Picker.Builder(getActivity())
                .setTitle(Html.fromHtml(getString(R.string.play_music_choose_player_prompt)))
                .setOnKeyListener(Utils.sDisableSearchKey)
                .setSingleChoiceItems(items, checked, null)
                .setPositiveButton(getString(R.string.continue_prompt), this);

        final View v = builder.create().getView();
        mListView = (ListView) v.findViewById(R.id.list);

        return v;
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
        final int checked = mListView.getCheckedItemPosition();
        if (checked >= 0 && checked < mIntentItems.size()) {
            final String pkg = mIntentItems.get(checked).mDeliverToPkg;
            final String activity = mIntentItems.get(checked).mDeliverToActivity;
            mInputConfigs.putExtra(EXTRA_PLAYER_COMPONENT, new ComponentName(pkg, activity).flattenToString());
            mHostActivity.onReturn(mOutputConfigs, this);
        }
     }
}
