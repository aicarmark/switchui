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
 * limitations under the License.
 */

package com.android.contacts.util;

import com.android.contacts.R;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.SimUtility;
import com.motorola.android.telephony.PhoneModeManager;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
import android.widget.RadioButton;
//<!-- MOTOROLA MOD End of IKPIM-491 -->
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * List-Adapter for Account selection
 */
public final class AccountsListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final List<AccountWithDataSet> mAccounts;
    private final AccountTypeManager mAccountTypes;
    private final Context mContext;
    private final AccountWithDataSet mCurrentAccount;

    /**
     * Filters that affect the list of accounts that is displayed by this adapter.
     */
    public enum AccountListFilter {
        ALL_ACCOUNTS,                   // All read-only and writable accounts
        ACCOUNTS_CONTACT_WRITABLE,      // Only where the account type is contact writable, no card accounts
        ACCOUNTS_CONTACT_WRITABLE_WITH_CARD,      // MOT CHINA Only where the account type is contact writable and with card accounts
        ACCOUNTS_GROUP_WRITABLE         // Only accounts where the account type is group writable
    }

    public AccountsListAdapter(Context context, AccountListFilter accountListFilter) {
        this(context, accountListFilter, null);
    }

    /**
     * @param currentAccount the Account currently selected by the user, which should come
     * first in the list. Can be null.
     */
    public AccountsListAdapter(Context context, AccountListFilter accountListFilter,
            AccountWithDataSet currentAccount) {
        mContext = context;
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        //need get application context instead of context, otherwise you MAY not get proper account type manager instance, a bug in Google design
        mAccountTypes = AccountTypeManager.getInstance(context.getApplicationContext());
//<!-- MOTOROLA MOD End of IKPIM-491 -->
        mAccounts = getAccounts(accountListFilter);
        if (currentAccount != null
                && !mAccounts.isEmpty()
                && !mAccounts.get(0).equals(currentAccount)
                && mAccounts.remove(currentAccount)) {
            mAccounts.add(0, currentAccount);
        }
        mCurrentAccount = currentAccount;
        mInflater = LayoutInflater.from(context);
    }

    private List<AccountWithDataSet> getAccounts(AccountListFilter accountListFilter) {
        if (accountListFilter == AccountListFilter.ACCOUNTS_GROUP_WRITABLE) {
            return new ArrayList<AccountWithDataSet>(mAccountTypes.getGroupWritableAccounts());
        }
        // MOT CHINA to get writable accounts with Card Accounts
        if (accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE_WITH_CARD) {
            return new ArrayList<AccountWithDataSet>(mAccountTypes.getAccountsWithCard(true));
        }
        return new ArrayList<AccountWithDataSet>(mAccountTypes.getAccounts(
                accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View resultView = convertView != null ? convertView
                : mInflater.inflate(R.layout.account_selector_list_item, parent, false);

        final TextView text1 = (TextView) resultView.findViewById(android.R.id.text1);
        final TextView text2 = (TextView) resultView.findViewById(android.R.id.text2);
        final ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        final RadioButton checkbox = (RadioButton)resultView.findViewById(android.R.id.checkbox);
//<!-- MOTOROLA MOD End of IKPIM-491 -->
        final AccountWithDataSet account = mAccounts.get(position);
        if(account.equals(mCurrentAccount)){
             checkbox.setChecked(true);
        }
        final AccountType accountType = mAccountTypes.getAccountType(account.type, account.dataSet);

        if (HardCodedSources.ACCOUNT_TYPE_LOCAL.equals(account.type)) {
            text1.setText(mContext.getString(R.string.local_name));
            text2.setText(mContext.getString(R.string.account_local_device));
            icon.setImageDrawable(accountType.getDisplayIcon(mContext));
        } else if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {
            text1.setText(accountType.getDisplayLabel(mContext));
            int phoneType, str_id, str_id_cap;
            int iconRes = R.drawable.ic_launcher_card;
            phoneType = SimUtility.getTypeByAccountName(account.name);
            if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                if (PhoneModeManager.isDmds()) {
                    str_id_cap = R.string.account_card_C_cap;
                    str_id = R.string.account_card_C;
                    iconRes = R.drawable.ic_launcher_card_c;
                } else {
                    str_id_cap = R.string.account_card_uim_cap;
                    str_id = R.string.account_card_uim;
                }
            } else {
                if (PhoneModeManager.isDmds()) {
                    iconRes = R.drawable.ic_launcher_card_g;
                }
                str_id_cap = R.string.account_card_G_cap;
                str_id = R.string.account_card_G;
            }
            final int free = SimUtility.getFreeSpace(mContext.getContentResolver(), phoneType);
            final int capacity = SimUtility.getCapacity(mContext.getContentResolver(), phoneType);
            int used = capacity - free;
            if (capacity <= 0 || used < 0 ) {
                text2.setText(str_id);
            } else {
                text2.setText(mContext.getString(str_id_cap, used, capacity));
            }
            icon.setImageDrawable(mContext.getResources().getDrawable(iconRes));
    	} else {
            text1.setText(accountType.getDisplayLabel(mContext));

            // For email addresses, we don't want to truncate at end, which might cut off the domain
            // name.
            text2.setText(account.name);
            text2.setEllipsize(TruncateAt.MIDDLE);
            icon.setImageDrawable(accountType.getDisplayIcon(mContext));
    	}

//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        if (checkbox!=null) {
            checkbox.setFocusable(false);
            checkbox.setFocusableInTouchMode(false);
            checkbox.setClickable(false);
        }
//<!-- MOTOROLA MOD End of IKPIM-491 -->
        return resultView;
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public AccountWithDataSet getItem(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
}

