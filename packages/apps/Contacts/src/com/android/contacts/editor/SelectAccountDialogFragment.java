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

package com.android.contacts.editor;

import com.android.contacts.R;

import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.util.AccountsListAdapter;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import com.motorola.contacts.preference.ContactPreferenceUtilities;
//<!-- MOTOROLA MOD End of IKPIM-491 -->
/**
 * Shows a dialog asking the user which account to chose.
 *
 * The result is passed to {@code targetFragment} passed to {@link #show}.
 */
public final class SelectAccountDialogFragment extends DialogFragment {
    public static final String TAG = "SelectAccountDialogFragment";
    //<!-- MOTOROLA MOD Start: IKPIM-491 -->
    private int lastSelectedIndex=-1;
    private CheckBox userSelection;
    //<!-- MOTOROLA MOD End of IKPIM-491 -->

    // Android default select mode
    private boolean mDefaultSelectMode = false;

    private static final String KEY_TITLE_RES_ID = "title_res_id";
    private static final String KEY_LIST_FILTER = "list_filter";
    private static final String KEY_EXTRA_ARGS = "extra_args";
    private static final String KEY_DEFAULT_SELECT_MODE = "Default_select_mode";

    public SelectAccountDialogFragment() { // All fragments must have a public default constructor.
    }

    /**
     * Show the dialog.
     *
     * @param fragmentManager {@link FragmentManager}.
     * @param targetFragment {@link Fragment} that implements {@link Listener}.
     * @param titleResourceId resource ID to use as the title.
     * @param accountListFilter account filter.
     * @param extraArgs Extra arguments, which will later be passed to
     *     {@link Listener#onAccountChosen}.  {@code null} will be converted to
     *     {@link Bundle#EMPTY}.
     */
    public static <F extends Fragment & Listener> void show(FragmentManager fragmentManager,
            F targetFragment, int titleResourceId,
            AccountListFilter accountListFilter, Bundle extraArgs) {
        final Bundle args = new Bundle();
        args.putInt(KEY_TITLE_RES_ID, titleResourceId);
        args.putSerializable(KEY_LIST_FILTER, accountListFilter);
        args.putBundle(KEY_EXTRA_ARGS, (extraArgs == null) ? Bundle.EMPTY : extraArgs);

        final SelectAccountDialogFragment instance = new SelectAccountDialogFragment();
        instance.setArguments(args);
        if (extraArgs != null) {
            instance.mDefaultSelectMode = extraArgs.getBoolean(KEY_DEFAULT_SELECT_MODE, false);
        }
        instance.setTargetFragment(targetFragment, 0);
        instance.show(fragmentManager, null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Bundle args = getArguments();

        final AccountListFilter filter = (AccountListFilter) args.getSerializable(KEY_LIST_FILTER);
        final AccountsListAdapter accountAdapter = new AccountsListAdapter(builder.getContext(),
                filter);

        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mDefaultSelectMode) {
                    dialog.dismiss();
                    onAccountSelected(accountAdapter.getItem(which));
                } else {
                    //<!-- MOTOROLA MOD Start: IKPIM-491 -->
                    lastSelectedIndex = which;
                    //<!-- MOTOROLA MOD End of IKPIM-491 -->
                }
            }
        };

        builder.setTitle(args.getInt(KEY_TITLE_RES_ID));
        if (mDefaultSelectMode) {
            // Android default behavior
            builder.setSingleChoiceItems(accountAdapter, 0, clickListener);
        } else {
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        lastSelectedIndex = ContactPreferenceUtilities.GetUserPreferredAccountIndex(
                builder.getContext(), true);
        if (lastSelectedIndex == -1) {
            lastSelectedIndex = 0;
        }
        builder.setSingleChoiceItems(accountAdapter, lastSelectedIndex, clickListener);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                // If user want to remember the choice, write to contact
                // preference first
                ContactPreferenceUtilities.SaveUserPreferredAccountToSharedPreferences(
                        builder.getContext(), userSelection.isChecked(),
                        accountAdapter.getItem(lastSelectedIndex));

                // User will get call back which account was selected just now
                onAccountSelected(accountAdapter.getItem(lastSelectedIndex));
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final Fragment targetFragment = getTargetFragment();
                if (targetFragment != null && targetFragment instanceof Listener) {
                    final Listener target = (Listener) targetFragment;
                    target.onAccountSelectorCancelled();
                }
            }
        });

        LayoutInflater mInflater = LayoutInflater.from(builder.getContext());
        final View resultView = mInflater.inflate(R.layout.account_selector_remember_choice, null,
                false);
        userSelection = (CheckBox) resultView.findViewById(R.id.remember);
        //MOT MOD BEGIN - IKHSS7-730, checkbox is on when is remembered & preferred account exist & account still available
        boolean bRemembered = ContactPreferenceUtilities.IfUserChoiceRemembered(builder.getContext()) &&
              (ContactPreferenceUtilities.GetUserPreferredAccountIndex(builder.getContext(), true) != -1);
        userSelection.setChecked(bRemembered);
        //MOT MOD END - IKHASS7-730
        resultView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                userSelection.toggle();
            }

        });

        builder.setView(resultView);
//<!-- MOTOROLA MOD End of IKPIM-491 -->
        }
        final AlertDialog result = builder.create();
        return result;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null && targetFragment instanceof Listener) {
            final Listener target = (Listener) targetFragment;
            target.onAccountSelectorCancelled();
        }
    }

    /**
     * Calls {@link Listener#onAccountChosen} of {@code targetFragment}.
     */
    private void onAccountSelected(AccountWithDataSet account) {
        final Fragment targetFragment = getTargetFragment();
        if (targetFragment != null && targetFragment instanceof Listener) {
            final Listener target = (Listener) targetFragment;
            target.onAccountChosen(account, getArguments().getBundle(KEY_EXTRA_ARGS));
        }
    }

    public interface Listener {
        void onAccountChosen(AccountWithDataSet account, Bundle extraArgs);
        void onAccountSelectorCancelled();
    }
}
