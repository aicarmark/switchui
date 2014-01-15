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

package com.motorola.contacts.list;

import com.android.contacts.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.content.Context;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.provider.Contacts.Intents.Insert;
import android.content.Intent;
import android.provider.ContactsContract.Contacts;
import android.provider.Contacts.People;
/**
 * Shows a dialog asking the user to add number to existing contact or new contact.
 *
 * The result is passed to {@code targetFragment} passed to {@link #show}.
 */
public final class AddContactDialogFragment extends DialogFragment {
    public static final String TAG = "AddContactDialogFragment";

    //MOT MOD BEGIN IKHSS6UPGR-9089
    private static final String KEY_DATA = "data";
    private static final String KEY_IS_NUMBER = "isNumber";
    //MOT MOD END IKHSS6UPGR-9089

    private static final String SHOW_CREATE_NEW_BUTTON = "show_create_new_button"; //MOT MOD - IKHSS6UPGR-3604

    public AddContactDialogFragment() { // All fragments must have a public default constructor.
    }

    public static AddContactDialogFragment newInstance(String data, boolean isNumber) {
        AddContactDialogFragment frag = new AddContactDialogFragment();
        Bundle args = new Bundle();
        //MOT MOD BEGIN IKHSS6UPGR-9089
        args.putString(KEY_DATA, data);
        args.putBoolean(KEY_IS_NUMBER, isNumber);
        //MOT MOD END IKHSS6UPGR-9089
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Bundle args = getArguments();

        //MOT MOD BEGIN IKHSS6UPGR-9089
        int id;
        if(args.getBoolean(KEY_IS_NUMBER)) {
            id = R.string.addToContactsDialog;
        } else {
            id = R.string.addEmailToContactsDialog;
        }
        //MOT MOD END IKHSS6UPGR-9089
        Dialog dialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.addToContactsTitle)
                //MOT MOD BEGIN IKHSS6UPGR-9089
                .setMessage(getActivity().getString(id, args.getString(KEY_DATA)))
                //MOT MOD END IKHSS6UPGR-9089
                .setNegativeButton(R.string.existingContact,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            final Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
                            //MOT MOD BEGIN IKHSS6UPGR-9089
                            if(args.getBoolean(KEY_IS_NUMBER)) {
                                intent.putExtra(Insert.PHONE, args.getString(KEY_DATA));
                            } else {
                                intent.putExtra(Insert.EMAIL, args.getString(KEY_DATA));
                            }
                            //MOT MOD END IKHSS6UPGR-9089
                            intent.putExtra(SHOW_CREATE_NEW_BUTTON, false); //MOT MOD - IKHSS6UPGR-3604
                            intent.setType(People.CONTENT_ITEM_TYPE);
                            getActivity().startActivity(intent);
                            getActivity().finish();
                        }
                    }
                )
                .setPositiveButton(R.string.newContact,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                            //MOT MOD BEGIN IKHSS6UPGR-9089
                            if(args.getBoolean(KEY_IS_NUMBER)) {
                                intent.putExtra(Insert.PHONE, args.getString(KEY_DATA));
                            } else {
                                intent.putExtra(Insert.EMAIL, args.getString(KEY_DATA));
                            }
                            //MOT MOD END IKHSS6UPGR-9089
                            getActivity().startActivity(intent);
                            getActivity().finish();
                        }
                    }
                )
                .create();

        dialog.show();
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        getActivity().finish();
    }
}
