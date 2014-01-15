/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.contacts.interactions;

import com.android.contacts.ContactsActivity;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.R;
import com.android.contacts.SimUtility;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.RawContacts;
import android.R.integer;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.motorola.android.telephony.PhoneModeManager;

import java.util.ArrayList;

public class ExportByAccountDialogFragment extends DialogFragment {
    private static final String TAG = "ExportByAccountDialogFragment";

    private static final String CHECKED_ACCOUNT_NAMES = "checked_account_names";
    private static final String KEY_CHECKED_FLAGS = "checkedFlags";
    private static final String LOCAL_ACCOUNT_TYPE = HardCodedSources.ACCOUNT_TYPE_LOCAL;
    private static final String GOOGLE_ACCOUNT_TYPE = "com.google";
    private static final String EXCHANGE_ACCOUNT_TYPE = "com.android.exchange";
    private static final String LOCAL_ACCOUNT_NAME = HardCodedSources.ACCOUNT_LOCAL_DEVICE;
    private static final String CARD_ACCOUNT_NAME = HardCodedSources.ACCOUNT_CARD;
    private static final String CCARD_ACCOUNT_NAME = HardCodedSources.ACCOUNT_CARD_C;
    private static final String GCARD_ACCOUNT_NAME = HardCodedSources.ACCOUNT_CARD_G;
    private static final String CONTACT_FILE_SUFFIX = ".vcf";
    private LayoutInflater mLayoutInflater;
    private CheckBox mCheckBox;
    private TextView mTextTitle;
    private TextView mTextSummary;
    private Button mOkButton;
    private Button mBackButton;
    private ListView mListView = null;
    private View mView = null;
    private boolean[] mCheckedFlags = null;
    private boolean[] mAccountCheckedFlags = null;
    private String[] mAccountNames;
    private ArrayList<String> mCheckedNames = new ArrayList<String>();

    /** Preferred way to show this dialog */
    public static void show(FragmentManager fragmentManager) {
        final ExportByAccountDialogFragment fragment = new ExportByAccountDialogFragment();
        fragment.show(fragmentManager, ExportByAccountDialogFragment.TAG);
    }
    
 // BEGIN Motorola, ODC_001639, 2012-12-26, SWITCHUITWO-138
    public void onResume() {
		super.onResume();
		if(!mContactsSizeZero){// Motorola, ODC_001639, 2012-12-26, SWITCHUITWO-445
			boolean okClickable = false;
	        if ( mCheckedFlags != null ){
	            for(int i = 0; i < mCheckedFlags.length; i++){
	            	okClickable = okClickable || mCheckedFlags[i];
	            }
	        }
	        Dialog alertDialog = getDialog();
	        if(alertDialog != null){
	        	mOkButton = ((AlertDialog)alertDialog).getButton(Dialog.BUTTON_POSITIVE);
	            if(mOkButton != null){
	            	mOkButton.setEnabled(okClickable);
	            }
	        }
		}
		
	}
    // END SWITCHUITWO-138

    //mtdr83 for IKCBSMMCPPRC-1709
    /**modify for SWITCHUITWOV-1 by bphx43 2012-09-06*/
    /*@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		boolean okClickable = false;
		// 2012.08.27 jrw added to fix IKCBSMMCPPRC-1752
        if ( mCheckedFlags != null ){
            for(int i = 0; i < mCheckedFlags.length; i++){
            	okClickable = okClickable || mCheckedFlags[i];
            }
        }
        mOkButton = ((AlertDialog) getDialog()).getButton(Dialog.BUTTON_POSITIVE);
        if(mOkButton != null){
        	mOkButton.setEnabled(okClickable);
        }
	}*/
    /** end by bphx43 */
    //mtdr83 for IKCBSMMCPPRC-1709

    // BEGIN Motorola, ODC_001639, 2012-12-26, SWITCHUITWO-445
    private boolean mContactsSizeZero;
    // END SWITCHUITWO-445
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCheckedFlags = savedInstanceState.getBooleanArray(KEY_CHECKED_FLAGS);
        }
        ArrayList<String> accNameList = new ArrayList<String>();
        accNameList = getAccountName();
        final DialogInterface.OnClickListener CancelListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };
        if (accNameList == null) {
        	mContactsSizeZero = true;// Motorola, ODC_001639, 2012-12-26, SWITCHUITWO-445
        	
            return new AlertDialog.Builder(getActivity())
                    .setMessage(getString(R.string.composer_has_no_exportable_contact_moto))
                    .setPositiveButton(getString(android.R.string.ok), CancelListener)
                    .create();
        }
        mLayoutInflater = LayoutInflater.from(getActivity());
        mView= mLayoutInflater.inflate(R.layout.checklist, null);
        mListView = (ListView)mView.findViewById(R.id.list);
        mAccountNames = (String[])(accNameList.toArray(new String[0]));
        if (mCheckedFlags == null) {
            mCheckedFlags = new boolean[mAccountNames.length];
        }
        for (boolean flag : mCheckedFlags) {
            flag = false;
        }
        setMyListView();

        final DialogInterface.OnClickListener OKListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                if(!mCheckedNames.isEmpty()) {
                    mCheckedNames.clear();
                }
                SparseBooleanArray exportBoolArray = mListView.getCheckedItemPositions();
                int size = mListView.getCount();
                boolean noneSelected = true;
                mAccountCheckedFlags = new boolean[size];
                if (null == exportBoolArray) {
                    return;
                }
                for (int i=0;i<size;i++){
                    mAccountCheckedFlags[i] = exportBoolArray.get(i);
                    if (mAccountCheckedFlags[i]) {
                        mCheckedNames.add(mAccountNames[i]);
                        noneSelected = false;
                   }
                }

                // if none is selected, OK button has no response
                if (noneSelected) {
                    Log.v(TAG, "No items checked.");
                    return;
                }
                Intent intent = new Intent("com.motorola.action.exportbyaccount");
                Bundle bundle = new Bundle();
                bundle.putStringArray(CHECKED_ACCOUNT_NAMES, (String[])mCheckedNames.toArray(new String[0]));
                intent.putExtras(bundle);
                startActivity(intent);
            }
        };
        return new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.select_account_title))
                .setView(mView)
                .setPositiveButton(getString(android.R.string.ok), OKListener)
                .setNegativeButton(getString(android.R.string.cancel), CancelListener)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBooleanArray(KEY_CHECKED_FLAGS, mCheckedFlags);
    }

    private void setMyListView(){
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent,
                View view, int position, long id) {
                    // TODO Auto-generated method stub
                    mCheckedFlags[(int) position] = mListView.isItemChecked(position);
                  //mtdr83 for IKCBSMMCPPRC-1709
                    boolean okClickable = false;
                    for(int i = 0; i < mCheckedFlags.length; i++){
                    	okClickable = okClickable || mCheckedFlags[i];
                    }
                    mOkButton = ((AlertDialog) getDialog()).getButton(Dialog.BUTTON_POSITIVE);
                    if(mOkButton != null){
                    	mOkButton.setEnabled(okClickable);
                    }
                    //mtdr83 for IKCBSMMCPPRC-1709
                    /**JB update SWITCHUITWOV-308 : "Does not display a marker icon when touch the select box." by bphx43 2012-11-12*/
                    ((CheckBox) view.findViewById(R.id.check_box)).setChecked(mCheckedFlags[position]);
					/**end */
                }
            });
        mListView.setAdapter(new ListViewAdapter());
        mListView.setItemsCanFocus(false);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }

    class ListViewAdapter extends BaseAdapter {
        public ListViewAdapter() {
            mLayoutInflater =(LayoutInflater)getActivity().getLayoutInflater();
        }

        public int getCount() {
            return mAccountNames.length;
        }

        public Object getItem(int position) {
            return null;
        }

        public long getItemId(int position) {
            return 0;
        }

        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.check_account_item, parent, false);
            }
            mCheckBox = (CheckBox) view.findViewById(R.id.check_box);
            mTextTitle = (TextView) view.findViewById(R.id.account_title);
            mTextSummary = (TextView) view.findViewById(R.id.account_summary);
            mCheckBox.setChecked(mCheckedFlags[position]);
            mTextTitle.setText(getTitle(mAccountNames[position]));
            mTextSummary.setText(getSummary(mAccountNames[position]));
            mCheckBox.setClickable(false);

            return view;
	}
    }

    private String getTitle(String str){
        int index = str.indexOf(':');
        if (index == -1) {
            if (str.equals(LOCAL_ACCOUNT_NAME)) {
                return getString(R.string.account_local_device);
            } else if (str.equals(CARD_ACCOUNT_NAME)) {
                final int phoneType = SimUtility.getTypeByAccountName(CARD_ACCOUNT_NAME);
                if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                    return getString(R.string.cCard_contacts);
                } else {
                    return getString(R.string.gCard_contacts);
                }
            } else if (str.equals(GCARD_ACCOUNT_NAME)) {
                return getString(R.string.gCard_contacts);
            } else if (str.equals(CCARD_ACCOUNT_NAME)) {
                return getString(R.string.cCard_contacts);
            } else {
                Log.e(TAG, "getTitle() error, should not be here");
                return str;
            }
        } else {
            return str.substring(index+1,str.length());
        }
    }

    private String getSummary(String str){
        if (str.equals(GOOGLE_ACCOUNT_TYPE) || str.startsWith("Gmail")) {
            return getString(R.string.account_google);
        } else if(str.equals(EXCHANGE_ACCOUNT_TYPE) || str.startsWith("Exchange")){
            return getString(R.string.account_corporate);
        } else if (str.equals(LOCAL_ACCOUNT_TYPE) || str.equals(LOCAL_ACCOUNT_NAME)) {
            return getString(R.string.account_local);
        } else if (str.equals(CARD_ACCOUNT_NAME)) {
            final int phoneType = SimUtility.getTypeByAccountName(CARD_ACCOUNT_NAME);
            if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
                return getString(R.string.cCard_contacts);
            } else {
                return getString(R.string.gCard_contacts);
            }
        } else if (str.equals(GCARD_ACCOUNT_NAME)) {
            return getString(R.string.gCard_contacts);
        } else if (str.equals(CCARD_ACCOUNT_NAME)) {
            return getString(R.string.cCard_contacts);
        } else {
            Log.e(TAG, "getSummary() error, should not be here");
            return str;
        }
    }

    private ArrayList<String> getAccountName() {
        ArrayList<String> accountString = new ArrayList<String>();
        accountString.clear();
        String[] projection ={"account_name"};
        String selection;
        Cursor cursor;
        String tempString;
        int columnIndex;

        //local
        selection = "account_name" + "=\"" +LOCAL_ACCOUNT_NAME + "\"";
        selection += " AND " + RawContacts.DELETED + "=0";
        cursor = getActivity().getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, null, null);
        try {
            if (null != cursor && cursor.getCount() != 0) {
                accountString.add(LOCAL_ACCOUNT_NAME);
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        if (PhoneModeManager.isDmds()) {
            //query c card
            selection ="account_name" + "=\"" +CCARD_ACCOUNT_NAME + "\"";
            selection += " AND " + RawContacts.DELETED + "=0";
            cursor = getActivity().getContentResolver().query(RawContacts.CONTENT_URI,
                    projection, selection, null, null);
            try {
                if (null != cursor && cursor.getCount() != 0) {
                    accountString.add(CCARD_ACCOUNT_NAME);
                }
            } finally {
                if (cursor != null) cursor.close();
            }

            //query g card
            selection = "account_name" + "=\"" +GCARD_ACCOUNT_NAME + "\"";
            selection += " AND " + RawContacts.DELETED + "=0";
            cursor = getActivity().getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, null, null);
            try {
                if (null != cursor && cursor.getCount() != 0) {
                    accountString.add(GCARD_ACCOUNT_NAME);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        } else {
            //query card
            selection = "account_name" + "=\"" +CARD_ACCOUNT_NAME + "\"";
            selection += " AND " + RawContacts.DELETED + "=0";
            cursor = getActivity().getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, null, null);
            try {
                if (null != cursor && cursor.getCount() != 0) {
                    accountString.add(CARD_ACCOUNT_NAME);
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }

        //query gmail account
        selection = "account_type" + "=\"" +GOOGLE_ACCOUNT_TYPE + "\"";
        selection += " AND " + RawContacts.DELETED + "=0";
        cursor = getActivity().getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, null, null);
        try {
            if (cursor != null) {
                columnIndex = cursor.getColumnIndex("account_name");
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        tempString = "Gmail:" + cursor.getString(columnIndex);
                        if (!accountString.contains(tempString)) {
                            accountString.add(tempString);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        //exchange account
        selection = "account_type" + "=\"" +EXCHANGE_ACCOUNT_TYPE + "\"";
        selection += " AND " + RawContacts.DELETED + "=0";
        cursor = getActivity().getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, null, null);
        try {
            if (cursor != null) {
                columnIndex = cursor.getColumnIndex("account_name");
                if (cursor.getCount() != 0) {
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        tempString = "Exchange:" + cursor.getString(columnIndex);
                        if (!accountString.contains(tempString)) {
                            accountString.add(tempString);
                        }
                        cursor.moveToNext();
                    }
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    	
        if(0 == accountString.size()){
            return null;
        } else {
            return accountString;
        }
    }
}
