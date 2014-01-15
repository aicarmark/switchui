/*
 * Copyright (C) 2009 The Android Open Source Project
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
 * limitations under the License.
 */

package com.android.contacts.util;

import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

import com.android.contacts.R;
import com.android.contacts.model.AccountTypeManager;

import java.util.List;
// MOTO MOD BEGIN
import android.widget.TextView;
//MOTO MOD END


import com.motorola.contacts.preference.ContactPreferenceUtilities;
//<!-- MOTOROLA MOD End of IKPIM-491 -->
/**
 * Utility class for selectiong an Account for importing contact(s)
 */
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
//Do not use this Utilities in further! we have a select account fragment which is an advanced replacement
@Deprecated
//<!-- MOTOROLA MOD End of IKPIM-491 -->
public class AccountSelectionUtil {
    // TODO: maybe useful for EditContactActivity.java...
    private static final String LOG_TAG = "AccountSelectionUtil";

    public static boolean mVCardShare = false;
    //<!-- MOTOROLA MOD Start: IKPIM-491 -->
    public static int lastSelectedIndex;
    public static CheckBox userSelection;
    public static DialogInterface.OnClickListener onGlobalClickListener;
    public static DialogInterface.OnCancelListener onGlobalCancelListener;
    //<!-- MOTOROLA MOD End of IKPIM-491 -->

    public static Uri mPath;

    public static class AccountSelectedListener
            implements DialogInterface.OnClickListener {

        final private Context mContext;
        final private int mResId;

        final protected List<AccountWithDataSet> mAccountList;

        public AccountSelectedListener(Context context, List<AccountWithDataSet> accountList,
                int resId) {
            if (accountList == null || accountList.size() == 0) {
                Log.e(LOG_TAG, "The size of Account list is 0.");
            }
            mContext = context;
            mAccountList = accountList;
            mResId = resId;
        }

        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
            //MOTO MOD BEGIN, IKTABLETMAIN-1842
            if(mAccountList == null || mAccountList.size() == 0 || which >= mAccountList.size()){
                 doImport(mContext, mResId, null);
             }
             else{
                 doImport(mContext, mResId, mAccountList.get(which));
             }
            //MOTO MOD END
        }
    }

    public static Dialog getSelectAccountDialog(Context context, int resId) {
        return getSelectAccountDialog(context, resId, null, null);
    }

    public static Dialog getSelectAccountDialog(Context context, int resId,
            DialogInterface.OnClickListener onClickListener) {
        return getSelectAccountDialog(context, resId, onClickListener, null);
    }

    /**
     * When OnClickListener or OnCancelListener is null, uses a default listener.
     * The default OnCancelListener just closes itself with {@link Dialog#dismiss()}.
     */
//<!-- MOTOROLA MOD End of IKPIM-491 -->
    public static Dialog getSelectAccountDialog(Context context, int resId,
            final DialogInterface.OnClickListener onClickListener,
            final DialogInterface.OnCancelListener onCancelListener) {
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
        final List<AccountWithDataSet> writableAccountList = accountTypes.getAccounts(true);

        Log.i(LOG_TAG, "The number of available accounts: " + writableAccountList.size());

        // Assume accountList.size() > 1
//
//        // Wrap our context to inflate list items using correct theme
//        final Context dialogContext = new ContextThemeWrapper(
//                context, android.R.style.Theme_Light);
//        final LayoutInflater dialogInflater = (LayoutInflater)dialogContext
//                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        final ArrayAdapter<Account> accountAdapter =
//            new ArrayAdapter<Account>(context, android.R.layout.simple_list_item_2,
//                    writableAccountList) {
//
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                if (convertView == null) {
//                    convertView = dialogInflater.inflate(
//                            android.R.layout.simple_list_item_2,
//                            parent, false);
//                }
//
//                // TODO: show icon along with title
//                final TextView text1 =
//                        (TextView)convertView.findViewById(android.R.id.text1);
//                final TextView text2 =
//                        (TextView)convertView.findViewById(android.R.id.text2);
//
//                final Account account = this.getItem(position);
//                final AccountType accountType = accountTypes.getAccountType(account.type);
//                final Context context = getContext();
//
//                text1.setText(account.name);
//                text2.setText(accountType.getDisplayLabel(context));
//
//                return convertView;
//            }
//        };

        if (onClickListener == null) {
            onGlobalClickListener =
                new AccountSelectedListener(context, writableAccountList, resId);

        } else {
            onGlobalClickListener = onClickListener;
        }

        if (onCancelListener == null) {
            onGlobalCancelListener = new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    dialog.dismiss();
                }
            };
        } else {
            onGlobalCancelListener = onCancelListener;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        //MOT MOD BEGIN - IKHSS7-1813, only need writable accounts just like accouts in writableAccountList
        final AccountsListAdapter accountAdapter = new AccountsListAdapter(builder.getContext(),
                                                           AccountListFilter.ACCOUNTS_CONTACT_WRITABLE);
        //MOT MOD END - IKHSS7-1813

        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AccountSelectionUtil.lastSelectedIndex = which;
            }
        };

        builder.setTitle(R.string.dialog_new_contact_account);

        AccountSelectionUtil.lastSelectedIndex = ContactPreferenceUtilities.GetUserPreferredAccountIndex(
                builder.getContext(), true);
        if (AccountSelectionUtil.lastSelectedIndex == -1) {
            AccountSelectionUtil.lastSelectedIndex = 0;
        }
        builder.setSingleChoiceItems(accountAdapter, AccountSelectionUtil.lastSelectedIndex, clickListener);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

                // If user want to remember the choice, write to contact
                // preference first

               //MOTO MOD BEGIN, IKTABLETMAIN-1842, if account list is empty ,just return to avoid exception.
                if(accountAdapter.getCount() != 0 && lastSelectedIndex < accountAdapter.getCount()){
                    if (userSelection != null) { // MOT CHINA IKCNDEVICS-2769 
                       ContactPreferenceUtilities.SaveUserPreferredAccountToSharedPreferences(
                            builder.getContext(), userSelection.isChecked(),
                            accountAdapter.getItem(lastSelectedIndex));
                    } else {
                       ContactPreferenceUtilities.SaveUserPreferredAccountToSharedPreferences(
                            builder.getContext(), false,
                            accountAdapter.getItem(lastSelectedIndex));
                    }

                     }
               //MOTO MOD END
                // User will get call back which account was selected just now
                onGlobalClickListener.onClick(dialog, lastSelectedIndex);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                onGlobalCancelListener.onCancel(dialog);
            }
        });

        //MOTO MOD BEGIN, IKTABLETMAIN-2114
        if(accountAdapter.getCount() != 0 && lastSelectedIndex < accountAdapter.getCount()){
            LayoutInflater mInflater = LayoutInflater.from(builder.getContext());
            final View resultView = mInflater.inflate(R.layout.account_selector_remember_choice, null, false);
            userSelection = (CheckBox) resultView.findViewById(R.id.remember);
            userSelection.setChecked(ContactPreferenceUtilities.IfUserChoiceRemembered(builder.getContext()));
            resultView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    userSelection.toggle();
                }

            });
            builder.setView(resultView);
        }
        else {
           TextView textview = new TextView(context);
           textview.setText(R.string.no_account_dispaly);
           builder.setView(textview);
        }
        //MOTO MOD END, IKTABLETMAIN-2114

        builder.setOnCancelListener(onGlobalCancelListener);

        return builder.create();
    }
//<!-- MOTOROLA MOD End of IKPIM-491 -->

    public static void doImport(Context context, int resId, AccountWithDataSet account) {
        switch (resId) {
            case R.string.import_from_sim: {
//<!-- MOTOROLA MOD Start: IKPIM-493 James Shen - A22398 -->
                doImportFromSimManager(context, account);
//<!-- MOTOROLA MOD End: IKPIM-493 James Shen - A22398 -->
                break;
            }
            case R.string.import_from_sdcard: {
                doImportFromSdCard(context, account);
                break;
            }
        }
    }

//<!-- MOTOROLA MOD Start: IKPIM-493 James Shen - A22398 -->
    public static void doImportFromSimManager(Context context, AccountWithDataSet account) {
         Intent importIntent = new Intent("com.motorola.android.simmanager.ACTION_IMPORT");
         if (account != null) {
             importIntent.putExtra("account_name", account.name);
             importIntent.putExtra("account_type", account.type);
             importIntent.putExtra("data_set", account.dataSet);
         }
         importIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         context.startActivity(importIntent);
    }
//<!-- MOTOROLA MOD End: IKPIM-493 James Shen - A22398 -->

    public static void doImportFromSim(Context context, AccountWithDataSet account) {
        Intent importIntent = new Intent(Intent.ACTION_VIEW);
        importIntent.setType("vnd.android.cursor.item/sim-contact");
        if (account != null) {
            importIntent.putExtra("account_name", account.name);
            importIntent.putExtra("account_type", account.type);
            importIntent.putExtra("data_set", account.dataSet);
        }
        importIntent.setClassName("com.android.phone", "com.android.phone.SimContacts");
        context.startActivity(importIntent);
    }

    public static void doImportFromSdCard(Context context, AccountWithDataSet account) {
        Intent importIntent = new Intent(context,
                com.android.contacts.vcard.ImportVCardActivity.class);
        if (account != null) {
            importIntent.putExtra("account_name", account.name);
            importIntent.putExtra("account_type", account.type);
            importIntent.putExtra("data_set", account.dataSet);
        }

        if (mVCardShare) {
            importIntent.setAction(Intent.ACTION_VIEW);
            importIntent.setData(mPath);
        }
        mVCardShare = false;
        mPath = null;
        importIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //MOT MOD, fix AndroidRunTimeException, Calling startActivity from outside of an Activity.
        context.startActivity(importIntent);
    }
}
