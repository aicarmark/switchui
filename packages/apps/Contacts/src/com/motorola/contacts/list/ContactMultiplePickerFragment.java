package com.motorola.contacts.list;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.ContentUris;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.android.contacts.R;
import com.android.contacts.list.ContactEntryListAdapter;
import com.android.contacts.list.ContactListAdapter;
import com.android.contacts.list.ContactListFilter;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.DefaultContactListAdapter;
import com.android.contacts.list.DirectoryListLoader;
import com.android.contacts.list.LegacyContactListAdapter;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.util.PhoneCapabilityTester;
import com.android.contacts.list.MulContactListAdapter;

import android.accounts.Account;

public class ContactMultiplePickerFragment extends ContactPickerFragment {
    private static final String TAG = "ContactMultiplePickerFragment";
    private static final String KEY_SELECTION_CACHE = "userSelectionCache";
    private static final String KEY_INCLUDE_ACCOUNT = "includeAccount";
    private static final String KEY_EXCLUDE_ACCOUNT = "excludeAccount";
    private static final String KEY_EXCLUDE_ID = "excludeId";
    private ContactEntryListAdapter mAdapter=null;
    private OnContactMultiplePickerActionListener mListener;
    public HashMap<Uri, Boolean> mSelectionCache;
    private ContactListFilter mFilter;
    private ArrayList<Account> mIncludeAccount = null;   // MOT CHINA
    private ArrayList<Account> mExcludeAccount = null;   // MOT CHINA         
    private ArrayList<String> mExcludeId = null;   //mtdr83 for IKCBSMMCPPRC-1310 


    public ContactMultiplePickerFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
        setQuickContactEnabled(false);
        setCheckBoxEnabled(true);
        setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);  //search local only contacts
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (mSelectionCache==null) {
            mSelectionCache = new HashMap<Uri, Boolean>();
        }
        if (PhoneCapabilityTester.isUsingTwoPanes(activity)==false) {
            setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        } else {
            setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_LEFT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_SELECTION_CACHE, mSelectionCache);
        outState.putParcelableArrayList(KEY_INCLUDE_ACCOUNT, mIncludeAccount);
        outState.putParcelableArrayList(KEY_EXCLUDE_ACCOUNT, mExcludeAccount);
        outState.putStringArrayList(KEY_EXCLUDE_ID, mExcludeId);//mtdr83 for IKCBSMMCPPRC-1310 
    }

    @SuppressWarnings("unchecked")
    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);

        if (savedState == null) {
            return;
        }

        mSelectionCache = (HashMap<Uri, Boolean>) savedState.getSerializable(KEY_SELECTION_CACHE);
        mIncludeAccount = savedState.getParcelableArrayList(KEY_INCLUDE_ACCOUNT);
        mExcludeAccount = savedState.getParcelableArrayList(KEY_EXCLUDE_ACCOUNT);
        mExcludeId = savedState.getStringArrayList(KEY_EXCLUDE_ID);//mtdr83 for IKCBSMMCPPRC-1310 
    }

    @Override
    public void setOnContactPickerActionListener(OnContactPickerActionListener listener) {
        super.setOnContactPickerActionListener(listener);
        mListener = (OnContactMultiplePickerActionListener) listener;
    }

    @Override
    protected ContactEntryListAdapter createListAdapter() {
    	if (mIncludeAccount == null && mExcludeAccount == null && mExcludeId == null) {//mtdr83 for IKCBSMMCPPRC-1310 
            mAdapter = super.createListAdapter();
        }
        else {  // customozied adapter to set the include/exclude account selection
            MulContactListAdapter adapter = new MulContactListAdapter(getActivity());
            adapter.setSectionHeaderDisplayEnabled(true);
            adapter.setDisplayPhotos(true);
            adapter.setQuickContactEnabled(false);
            if (mIncludeAccount != null) {   // MOT CHINA
                adapter.setIncludeAccount(mIncludeAccount);
            }
            else if (mExcludeAccount != null) {
                adapter.setExcludeAccount(mExcludeAccount);
            }
            //begin mtdr83 for IKCBSMMCPPRC-1310 
            if(mExcludeId != null){
            	adapter.setExcludeId(mExcludeId);
            }
            //end mtdr83 for IKCBSMMCPPRC-1310 
            mAdapter = adapter;
        }
        
        mAdapter.setCheckBoxEnabled(isCheckBoxEnabled());
        if (mFilter == null) {
        		/*2013-1-23, add by amt_sunzhao for SWITCHUITWO-542 */ 
            //if (mIncludeAccount == null && mExcludeAccount == null) {
                // show the contacts by "AccountFilter" setting
                mFilter = ContactListFilter.restoreDefaultPreferences(PreferenceManager.getDefaultSharedPreferences(getActivity()));
            /*} else {
                // show the visible contacts
                mFilter = ContactListFilter.createFilterWithType(ContactListFilter.FILTER_TYPE_CUSTOM);
            }*/
                /*2013-1-23, add end*/ 
            mAdapter.setFilter(mFilter);
        }
        return mAdapter;
    }

    @Override
    public void completeFragmentSpecificActionsOnLoadingCompleted(){

        //If the key exists, replace the status with cached status
        //If the key is new, check it and put in cache
        getListView().clearChoices();
        if ((mAdapter != null) && (getListView() != null)) {
            for (int i = 0; i < mAdapter.getCount(); i++) {
                Uri uri;
                if (isLegacyCompatibilityMode()) {
                    uri = ((LegacyContactListAdapter) mAdapter).getPersonUri(i);
                } else {
                    uri = ((ContactListAdapter) mAdapter).getContactUri(i);
                }
                if (uri != null) {
                    if (mSelectionCache.containsKey(uri)) {
                        getListView().setItemChecked(i, mSelectionCache.get(uri));
                    } else {
                        /* MOT CHINA, DO NOT SELECT ALL BY DEFAULT
                        if(mListener.isSelectAllSupported()){ //  MOT MOD BEGIN - IKPIM-1026
                            getListView().setItemChecked(i, true); //set to checked by default per requirement
                            mSelectionCache.put(uri, true);
                        } */
                    }
                }
            }
        }

        mListener.onContactLoadingCompletedAction();
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }


    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.contact_list_content, null);
    }

    @Override
    protected void onItemClick(int position, long id) {

        super.onItemClick(position, id);

        Uri uri;
        if (isLegacyCompatibilityMode()) {
            uri = ((LegacyContactListAdapter)getAdapter()).getPersonUri(position);
        } else {
            uri = ((ContactListAdapter)getAdapter()).getContactUri(position);
        }

        toggleContact(uri,getListView().isItemChecked(position));
    }

    public void toggleContact(Uri uri,boolean isItemChecked) {
        mListener.onToggleContactAction(uri,isItemChecked);
    }

    public void setIncludeAccount(ArrayList<Account> includeAccount) {
        if (mIncludeAccount == null) {
            mIncludeAccount = new ArrayList<Account>();
        }
        mIncludeAccount.clear();
        mIncludeAccount.addAll(includeAccount);
    }

    public void setExcludeAccount(ArrayList<Account> excludeAccount) {
        if (mExcludeAccount == null) {
            mExcludeAccount = new ArrayList<Account>();
        }
        mExcludeAccount.clear();
        mExcludeAccount.addAll(excludeAccount);
    }

    //begin mtdr83 for IKCBSMMCPPRC-1310 
    public void setExcludeId(ArrayList<String> excludeId) {
        if (mExcludeId == null) {
        	mExcludeId = new ArrayList<String>();
        }
        mExcludeId.clear();
        mExcludeId.addAll(excludeId);
    }
	//end mtdr83 for IKCBSMMCPPRC-1310 

    public ContactListFilter getFilter() {
        return mFilter;
    }

    public void setFilter(ContactListFilter filter) {
        if (mFilter == null && filter == null) {
            return;
        }
        mFilter = filter;
        mAdapter.setFilter(mFilter);
        reloadData();
    }

    @Override
    public int getVisibleOffset(){
        return 0;
    }
}
