package com.motorola.sdcardbackuprestore;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class ExportByAccount extends BaseActivity {

    private static final String TAG = "ExportByAccount";
    private static ExportByAccount sInstance = null;
    protected boolean[] accountCheckedFlags;

    protected boolean mIsLocalContactExisted = false;
    protected boolean mIsCCardContactExisted = false;
    protected boolean mIsCardContactExisted = false;
    protected boolean mIsGCardContactExisted = false;
    protected ArrayList<String> accountNames = null;
    protected ArrayList<String> checkedNames = new ArrayList<String>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate()");
                mOpt = Constants.EXPORTBYACCOUNT_ACTION;
        super.onCreate(savedInstanceState);
        onCreateView();
        sInstance = this;
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume()");
        super.onResume();
    }

    static ExportByAccount getInstance() {
        return sInstance;
    }

    @Override
    protected void startBackupRestoreService(int delOption) {
        Log.v(TAG, "Enter startBackupRestoreService()");

        Intent serviceIntent = new Intent(this, BackupRestoreService.class);

        Bundle bundle = new Bundle();
        bundle.putInt(Constants.ACTION, Constants.EXPORTBYACCOUNT_ACTION);
        bundle.putInt(Constants.DEL_OPTION, delOption);
        bundle.putStringArray(Constants.CHECKED_ACCOUNT_NAMES,
                checkedNames.toArray(new String[0]));
        serviceIntent.putExtras(bundle);
        startService(serviceIntent);
    }

    @Override
    protected String getEmptyStorageShortMsg(int type) {
        return null;
    }
    // modified by amt_jiayuanyuan 2012-12-25 SWITCHUITWO-368 begin
    @Override
    protected String getInsufficientSpaceMsg() {
        return getString(R.string.insufficient_space,
        		 getString(mStorageType));
    }
    // modified by amt_jiayuanyuan 2012-12-25 SWITCHUITWO-368 end
    @Override
    protected String getProgressMessage(int type) {
        return getString(R.string.export_to_sdcard, getTypeName(type), getString(mStorageType));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()");
        sInstance = null;
    }

    protected void onCreateView() {
        // TODO Auto-generated method stub

        if (accountNames == null)
            accountNames = accountNames();

        if (accountNames.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setMessage(
                            getString(R.string.no_content_found,
                                    getString(R.string.account),
                                    getString(R.string.contacts)))
                    .setPositiveButton(getString(android.R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    // TODO Auto-generated method stub
                                    dialog.dismiss();
                                    finish();
                                }
                            }).setCancelable(false)
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        public boolean onKey(DialogInterface dialog,
                                int keyCode, KeyEvent event) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_SEARCH :
                                    return true;
                            }
                            return false;
                        }

                    }).create().show();
        }

        setContentView(R.layout.checklist);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // TODO Auto-generated method stub
            	mListView.setItemChecked(position, mListView.isItemChecked(position));
                setOkButtonEnable();
            }
        });

        mListView.setAdapter(new ListViewAdapter());
        mListView.setItemsCanFocus(false);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mOkButton = (Button) findViewById(R.id.ok);
        mOkButton.setOnClickListener(new OnClickListener() {
        
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!checkedNames.isEmpty()) {
                    checkedNames.clear();
                }
                ListView mExportListView = (ListView) findViewById(R.id.list);
                SparseBooleanArray exportBoolArray = mExportListView
                        .getCheckedItemPositions();
                int size = mExportListView.getCount();
                accountCheckedFlags = new boolean[size];
                if (null == exportBoolArray)
                    return;
                for (int i = 0; i < size; i++) {
                    accountCheckedFlags[i] = exportBoolArray.get(i);
                    if (accountCheckedFlags[i]) {
                        checkedNames.add(accountNames.get(i));
                    }
                }
                //mOkButton.setEnabled(false);//remove for SWITCHUITWOV-291
                startBackupRestoreService(Constants.DEL_UNSET);
            }
        });
        setOkButtonEnable();
        mBackButton = (Button) findViewById(R.id.back);
        mBackButton.setOnClickListener(mBackButtonListener);
        /*mListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                // TODO Auto-generated method stub
                switch (item.getItemId()) {
                    case R.id.ok :
                        if (!checkedNames.isEmpty()) {
                            checkedNames.clear();
                        }
                        ListView mExportListView = (ListView) findViewById(R.id.list);
                        SparseBooleanArray exportBoolArray = mExportListView
                                .getCheckedItemPositions();
                        int size = mExportListView.getCount();
                        accountCheckedFlags = new boolean[size];
                        if (null == exportBoolArray)
                            return true;
                        for (int i = 0; i < size; i++) {
                            accountCheckedFlags[i] = exportBoolArray.get(i);
                            if (accountCheckedFlags[i]) {
                                checkedNames.add(accountNames.get(i));
                            }
                        }
                        startBackupRestoreService(Constants.DEL_UNSET);
                        mode.finish();
                        return true;
                    default :
                        return false;
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // TODO Auto-generated method stub
                //MenuInflater inflater = mode.getMenuInflater();
                //inflater.inflate(R.menu.base_activity, menu);
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
                // TODO Auto-generated method stub
                return false;
            }

            @Override
            public void onItemCheckedStateChanged(ActionMode arg0, int arg1,
                    long arg2, boolean arg3) {
                // TODO Auto-generated method stub

            }

        });*/
    }

    private ArrayList<String> accountNames() {
        Log.v("BaseActivity", "accountNames()");
        ArrayList<String> accountNames = new ArrayList<String>();
        setContactExistFlag();
        if (mIsLocalContactExisted) {
            accountNames.add(Constants.LOCAL_ACCOUNT_NAME);
        }
        if (mIsCCardContactExisted) {
            accountNames.add(Constants.CCARD_ACCOUNT_NAME);
        }
        if (mIsCardContactExisted) {
            accountNames.add(Constants.CARD_ACCOUNT_NAME);
        }
        if (mIsGCardContactExisted) {
            accountNames.add(Constants.GCARD_ACCOUNT_NAME);
        }
        if (null != getAccountName()) {
            accountNames.addAll(getAccountName());
        }
        return accountNames;
    }

    private void setContactExistFlag() {
        Cursor cursor;
        final String[] projection = {"account_name"};
        String selection;

        //local
        selection = "("+RawContacts.ACCOUNT_NAME + "='"
                + Constants.LOCAL_ACCOUNT_NAME +"' OR "+RawContacts.ACCOUNT_NAME + " is null )"
                + " AND " + RawContacts.DELETED + "=0";
        cursor = getContentResolver().query(RawContacts.CONTENT_URI,
                projection, selection, null, null);
        if (null != cursor && cursor.getCount() != 0) {
            mIsLocalContactExisted = true;
        }
        if (cursor != null)
            cursor.close();

        // query single sim card
        selection = "account_name = '" + Constants.CARD_ACCOUNT_NAME
                + "' AND " + RawContacts.DELETED + "='0'";
        cursor = getContentResolver().query(RawContacts.CONTENT_URI,
                projection, selection, null, null);
        if (null != cursor && cursor.getCount() != 0) {
            mIsCardContactExisted = true;
        }
        if (cursor != null)
            cursor.close();

        // query c card
        selection = "account_name = '" + Constants.CCARD_ACCOUNT_NAME
                + "' AND " + RawContacts.DELETED + "='0'";
        cursor = getContentResolver().query(RawContacts.CONTENT_URI,
                projection, selection, null, null);
        if (null != cursor && cursor.getCount() != 0) {
            mIsCCardContactExisted = true;
        }
        if (cursor != null)
            cursor.close();

        // query g card
        selection = "account_name = '" + Constants.GCARD_ACCOUNT_NAME
                + "' AND " + RawContacts.DELETED + "='0'";
        cursor = getContentResolver().query(RawContacts.CONTENT_URI,
                projection, selection, null, null);
        if (null != cursor && cursor.getCount() != 0) {
            mIsGCardContactExisted = true;
        }
        if (cursor != null)
            cursor.close();
    }

    // return gmail and exchange account names
    private ArrayList<String> getAccountName() {
        Log.v("BaseActivity", "getAccountName()");
        ArrayList<String> accountString = new ArrayList<String>();
        accountString.clear();
        String[] projection = {"account_name"};
        String selection;
        Cursor cursor;
        String tempString;
        int columnIndex;

        // query gmail account
        selection = "account_type = '" + Constants.GOOGLE_ACCOUNT_TYPE
                + "' AND " + RawContacts.DELETED + "='0'";
        cursor = getContentResolver().query(RawContacts.CONTENT_URI,
                projection, selection, null, null);
        if (cursor == null) {
            return null;
        }
        columnIndex = cursor.getColumnIndex("account_name");
        if (null != cursor && cursor.getCount() != 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                tempString = Constants.GACCOUNT_PREFIX + ":"
                        + cursor.getString(columnIndex);
                if (!accountString.contains(tempString)) {
                    accountString.add(tempString);
                }
                cursor.moveToNext();
            }
        }

        if (cursor != null)
            cursor.close();

        // exchange account
        selection = "account_type = '" + Constants.EXCHANGE_ACCOUNT_TYPE
                + "' AND " + RawContacts.DELETED + "='0'";
        cursor = getContentResolver().query(RawContacts.CONTENT_URI,
                projection, selection, null, null);
        if (cursor == null) {
            return null;
        }
        columnIndex = cursor.getColumnIndex("account_name");
        if (null != cursor && cursor.getCount() != 0) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                tempString = Constants.EACCOUNT_PREFIX + ":"
                        + cursor.getString(columnIndex);
                if (!accountString.contains(tempString)) {
                    accountString.add(tempString);
                }
                cursor.moveToNext();
            }
        }
        if (cursor != null)
            cursor.close();

        if (0 == accountString.size()) {
            return null;
        } else {
            return accountString;
        }
    }

    class ListViewAdapter extends BaseAdapter {
        protected LayoutInflater mLayoutInflater;
        protected CheckBox mCheckBox;
        protected TextView mTextTitle;
        protected TextView mTextSummary;

        ListViewAdapter() {
            mLayoutInflater = (LayoutInflater) getLayoutInflater();
        }

        public int getCount() {
            // TODO Auto-generated method stub
            return accountNames.size();
        }

        public View getView(int position, View view, ViewGroup parent) {
            // TODO Auto-generated method stub
            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.check_account_item,
                        parent, false);

            }
            mCheckBox = (CheckBox) view.findViewById(R.id.check_box);
            mCheckBox.setChecked(mListView.isItemChecked(position));
            mCheckBox.setClickable(false);
            mTextTitle = (TextView) view.findViewById(R.id.account_title);
            mTextSummary = (TextView) view.findViewById(R.id.account_summary);
            mTextTitle.setText(getTitle(accountNames.get(position)));
            mTextSummary.setText(getSummary(accountNames.get(position)));

            return view;
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    private String getTitle(String str) {
        int index = str.indexOf(':');
        if (index == -1) {
            if (str.equals(Constants.LOCAL_ACCOUNT_NAME)) {
                return getString(R.string.mobile);
            } else if (str.equals(Constants.GCARD_ACCOUNT_NAME)) {
                return getString(R.string.gCard_contacts);
            } else if (str.equals(Constants.CARD_ACCOUNT_NAME)) {
                return getString(R.string.gCard_contacts);
            } else if (str.equals(Constants.CCARD_ACCOUNT_NAME)) {
                return getString(R.string.cCard_contacts);
            } else {
                Log.e(TAG, "getTitle() error, should not be here");
                return str;
            }
        } else {
            return str.substring(index + 1, str.length());
        }
    }

    private String getSummary(String str) {
        if (str.equals(Constants.GOOGLE_ACCOUNT_TYPE)
                || str.startsWith(Constants.GACCOUNT_PREFIX)) {
            return getString(R.string.google);
        } else if (str.equals(Constants.EXCHANGE_ACCOUNT_TYPE)
                || str.startsWith(Constants.EACCOUNT_PREFIX)) {
            return getString(R.string.corporate);
        } else if (str.equals(Constants.LOCAL_ACCOUNT_TYPE)
                || str.equals(Constants.LOCAL_ACCOUNT_NAME)) {
            return getString(R.string.local);
        } else if (str.equals(Constants.GCARD_ACCOUNT_NAME)) {
            return getString(R.string.gCard_contacts);
        } else if (str.equals(Constants.CARD_ACCOUNT_NAME)) {
            return getString(R.string.gCard_contacts);
        } else if (str.equals(Constants.CCARD_ACCOUNT_NAME)) {
            return getString(R.string.cCard_contacts);
        } else {
            Log.e(TAG, "getSummary() error, should not be here");
            return str;
        }
    }

}

