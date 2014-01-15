/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.motorola.contacts.group;

import com.android.contacts.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
/**
 * Shows a dialog asking the user which account to chose.
 *
 * The result is passed to {@code targetFragment} passed to {@link #show}.
 */
public final class AddGroupNameDialogFragment extends DialogFragment {
    public static final String TAG = "AddGroupNameDialogFragment";

    private static final String KEY_TITLE_RES_ID = "title_res_id";
    private static final String KEY_LIST_FILTER = "list_filter";
    private static final String KEY_EXTRA_ARGS = "extra_args";
    private static EditText mGroupName;

    public AddGroupNameDialogFragment() { // All fragments must have a public default constructor.
    }

    /**
     * Show the dialog.
     *
     * @param fragmentManager {@link FragmentManager}.
     * @param targetFragment {@link Fragment} that implements {@link Listener}.
     * @param titleResourceId resource ID to use as the title.
     */
    public static <F extends Fragment & Listener> void show(FragmentManager fragmentManager,
            F targetFragment, int titleResourceId) {
        final Bundle args = new Bundle();
        args.putInt(KEY_TITLE_RES_ID, titleResourceId);

        final AddGroupNameDialogFragment instance = new AddGroupNameDialogFragment();
        instance.setArguments(args);
        instance.setTargetFragment(targetFragment, 0);
        instance.show(fragmentManager, null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Bundle args = getArguments();

        LayoutInflater mInflater = LayoutInflater.from(builder.getContext());
        final View resultView = mInflater.inflate(R.layout.add_group_name, null, false);
        mGroupName = (EditText)resultView.findViewById(R.id.dialog_name_group_edit);

        builder.setTitle(args.getInt(KEY_TITLE_RES_ID));
        builder.setView(resultView);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String groupName = mGroupName.getText().toString();
                onGroupNameSet(groupName);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final Fragment targetFragment = getTargetFragment();
                if (targetFragment != null && targetFragment instanceof Listener) {
                    final Listener target = (Listener) targetFragment;
                    target.onGroupNameCancelled();
                }
            }
        });

        final AlertDialog result = builder.create();
        return result;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null && targetFragment instanceof Listener) {
            final Listener target = (Listener) targetFragment;
            target.onGroupNameCancelled();
        }
    }

    /**
     * Calls {@link Listener#onGroupNameSet} of {@code targetFragment}.
     */
    private void onGroupNameSet(String groupName) {
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null && targetFragment instanceof Listener) {
            final Listener target = (Listener) targetFragment;
            target.onGroupNameSet(groupName);
        }
    }

    public interface Listener {
        void onGroupNameSet(String groupName);
        void onGroupNameCancelled();
    }
}
