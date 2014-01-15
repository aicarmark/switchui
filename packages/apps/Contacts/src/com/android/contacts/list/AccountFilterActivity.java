/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.list;

import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.test.InjectedServices;
import com.android.contacts.SimUtility;
import com.google.android.collect.Lists;
import com.motorola.android.telephony.PhoneModeManager;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import android.accounts.Account;


/**
 * Shows a list of all available accounts, letting the user select under which account to view
 * contacts.
 */
public class AccountFilterActivity extends ContactsActivity
        implements AdapterView.OnItemClickListener {

    private static final String TAG = "AccountFilterActivity";
    private static final boolean DEBUG = true;
    private static final int SUBACTIVITY_CUSTOMIZE_FILTER = 0;

    public static final String KEY_EXTRA_CONTACT_LIST_FILTER = "contactListFilter";

    private static final int FILTER_LOADER_ID = 0;

    private ListView mListView;

    private ArrayList<Account> mIncludeAccount = null;   // MOT CHINA
    private ArrayList<Account> mExcludeAccount = null;   // MOT CHINA         
    

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.contact_list_filter);

        // Resolve the intent
        final Intent intent = getIntent();

        mIncludeAccount = getIntent().getParcelableArrayListExtra("include_account");
        mExcludeAccount = getIntent().getParcelableArrayListExtra("exclude_account");        
        
        mListView = (ListView) findViewById(com.android.internal.R.id.list);
        mListView.setOnItemClickListener(this);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        getLoaderManager().initLoader(FILTER_LOADER_ID, null, new MyLoaderCallbacks());
    }

    private static class FilterLoader extends AsyncTaskLoader<List<ContactListFilter>> {
        private Context mContext;

        public FilterLoader(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public List<ContactListFilter> loadInBackground() {
            return loadAccountFilters(mContext);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        protected void onReset() {
            onStopLoading();
        }
    }

    private static List<ContactListFilter> loadAccountFilters(Context context) {
        AccountFilterActivity target = (AccountFilterActivity)context;
        final ArrayList<ContactListFilter> result = Lists.newArrayList();
        final ArrayList<ContactListFilter> accountFilters = Lists.newArrayList();
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(context);
        List<AccountWithDataSet> accounts = accountTypes.getAccounts(false);
        boolean skipAccount = false;  // hide any account ?
        for (AccountWithDataSet account : accounts) {
            AccountType accountType = accountTypes.getAccountType(account.type, account.dataSet);
            if (accountType.isExtension() && !account.hasData(context)) {
                // Hide extensions with no raw_contacts.
                continue;
            }
            if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {
                final int phoneType = SimUtility.getTypeByAccountName(account.name);
                final boolean isCardReady = SimUtility.isSimReady(phoneType);
                if (!isCardReady) {
                    // Hide Card accounts if SIM not ready
                    continue;
                }
            }
            if (target.mIncludeAccount != null) {
                // Hide certian account
                Account in_account = new Account(account.name, account.type);
                if (!target.mIncludeAccount.contains(in_account)) {
                    skipAccount = true;
                    continue;
                }
            }
            else if (target.mExcludeAccount != null) {
                // Hide certian account
                Account ex_account = new Account(account.name, account.type);
                if (target.mExcludeAccount.contains(ex_account)) {
                    skipAccount = true;
                    continue;
                }
            }
            
            Drawable icon = accountType != null ? accountType.getDisplayIcon(context) : null;
            if (PhoneModeManager.isDmds()) {
                if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {
                    final int phoneType = SimUtility.getTypeByAccountName(account.name);
                    int iconRes = R.drawable.ic_launcher_card;
                    if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                        iconRes = R.drawable.ic_launcher_card_c;
                    } else {
                        iconRes = R.drawable.ic_launcher_card_g;
                    }
                    icon = context.getResources().getDrawable(iconRes);
                }
            }
            accountFilters.add(ContactListFilter.createAccountFilter(
                    account.type, account.name, account.dataSet, icon));
        }

        // don't show "All" if we need hide any account
        if (!skipAccount) {
            result.add(ContactListFilter.createFilterWithType(
                    ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        }

        final int count = accountFilters.size();
        if (count >= 1) {
            // If we only have one account, don't show it as "account", instead show it as "all"
            // if "all" be hiden, then show the account even has one account
            if (count > 1 || skipAccount) {
                result.addAll(accountFilters);
            }
            result.add(ContactListFilter.createFilterWithType(
                    ContactListFilter.FILTER_TYPE_CUSTOM));
        }
        return result;
    }

    private class MyLoaderCallbacks implements LoaderCallbacks<List<ContactListFilter>> {
        @Override
        public Loader<List<ContactListFilter>> onCreateLoader(int id, Bundle args) {
            return new FilterLoader(AccountFilterActivity.this);
        }

        @Override
        public void onLoadFinished(
                Loader<List<ContactListFilter>> loader, List<ContactListFilter> data) {
            if (data == null) { // Just in case...
                Log.e(TAG, "Failed to load filters");
                return;
            }
            mListView.setAdapter(new FilterListAdapter(AccountFilterActivity.this, data));
        }

        @Override
        public void onLoaderReset(Loader<List<ContactListFilter>> loader) {
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ContactListFilter filter = (ContactListFilter) view.getTag();
        if (filter == null) return; // Just in case
        if (filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
            final Intent intent = new Intent(this,
                    CustomContactListFilterActivity.class);

            if (mIncludeAccount != null)
                intent.putParcelableArrayListExtra("include_account", mIncludeAccount);
            else if (mExcludeAccount != null)
                intent.putParcelableArrayListExtra("exclude_account", mExcludeAccount);

            startActivityForResult(intent, SUBACTIVITY_CUSTOMIZE_FILTER);
        } else {
        	//begin add by txbv34 for IKCBSMMCPPRC-1364
        	if(DEBUG)Log.d(TAG,"onItemClick,filter!=FILTER_TYPE_CUSTOM");
        	ContentResolver resolver = getContentResolver();

        	if(id == 2){
        		SimUtility.setSettingVisible(resolver,filter.accountName,filter.accountType,true);
            	SimUtility.ResetGroupsDBDefault(resolver);
        	}else if(id == 1){
        		SimUtility.setSettingVisible(resolver,filter.accountName,filter.accountType,true);
        	}else{
            	SimUtility.ResetSettingVisible(resolver);
            	SimUtility.ResetGroupsDBDefault(resolver);
        	}

        	//end add by txbv34 for IKCBSMMCPPRC-1364
            final Intent intent = new Intent();
            intent.putExtra(KEY_EXTRA_CONTACT_LIST_FILTER, filter);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case SUBACTIVITY_CUSTOMIZE_FILTER: {
                final Intent intent = new Intent();
                ContactListFilter filter = ContactListFilter.createFilterWithType(
                        ContactListFilter.FILTER_TYPE_CUSTOM);
                intent.putExtra(KEY_EXTRA_CONTACT_LIST_FILTER, filter);
                setResult(Activity.RESULT_OK, intent);
                finish();
                break;
            }
        }
    }

    private static class FilterListAdapter extends BaseAdapter {
        private final List<ContactListFilter> mFilters;
        private final LayoutInflater mLayoutInflater;

        public FilterListAdapter(Context context, List<ContactListFilter> filters) {
            mLayoutInflater = (LayoutInflater) context.getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            mFilters = filters;
        }

        @Override
        public int getCount() {
            return mFilters.size();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public ContactListFilter getItem(int position) {
            return mFilters.get(position);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            final ContactListFilterView view;
            if (convertView != null) {
                view = (ContactListFilterView) convertView;
            } else {
                view = (ContactListFilterView) mLayoutInflater.inflate(
                        R.layout.contact_list_filter_item, parent, false);
            }
            view.setSingleAccount(mFilters.size() == 1);
            final ContactListFilter filter = mFilters.get(position);
            view.setContactListFilter(filter);
            view.bindView(true);
            view.setTag(filter);
            return view;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // We have two logical "up" Activities: People and Phone.
                // Instead of having one static "up" direction, behave like back as an
                // exceptional case.
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
