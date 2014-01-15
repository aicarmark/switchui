/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.ListActivity;
import android.os.Bundle;

import android.content.AsyncQueryHandler;
import java.util.ArrayList;
import android.util.Log;
import java.util.Iterator;

import android.view.View;
import android.widget.ListView;
import android.widget.CheckBox;
import android.widget.AdapterView;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.app.AlertDialog;
import android.app.Dialog;

import android.accounts.Account;
import android.database.Cursor;
import android.content.ContentResolver;
import android.widget.SimpleCursorAdapter;

import android.content.Context;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;

import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;

import java.lang.ref.WeakReference;
import android.content.OperationApplicationException;
import android.content.ContentProviderOperation;
import android.os.RemoteException;

import android.widget.Toast;
import android.os.Handler;
import android.os.Message;

import android.app.ProgressDialog;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnClickListener;

import com.android.contacts.R;
import com.android.contacts.model.HardCodedSources;

import android.widget.TextView;
import android.widget.CheckedTextView;

/**
 * A group list to show all groups under certain account
 */
public class MultiplePickPreviewActivity extends ListActivity 
             implements View.OnClickListener {

    private static final String TAG = "MultiplePickPreviewActivity";

    private static final int LIST_TOKEN = 100;    
    private QueryHandler mQueryHandler = null;
    private dataListAdapter mAdapter = null;

    private static final String[] MIX_PHONE_EMAIL_PROJECTION = new String[] {
        Data._ID, Contacts.DISPLAY_NAME, Phone.TYPE, Phone.NUMBER, 
    };
    
    static final String[] PHONES_PROJECTION = new String[] {
        Data._ID, //0
        Phone.TYPE, //1
        Phone.LABEL, //2
        Phone.NUMBER, //3
        Contacts.DISPLAY_NAME, // 4
        RawContacts.CONTACT_ID, // 5     
    };
    static final int PHONE_ID_COLUMN_INDEX = 0;
    static final int PHONE_TYPE_COLUMN_INDEX = 1;
    static final int PHONE_LABEL_COLUMN_INDEX = 2;
    static final int PHONE_NUMBER_COLUMN_INDEX = 3;
    static final int PHONE_DISPLAY_NAME_COLUMN_INDEX = 4;
    static final int PHONE_CONTACT_ID_COLUMN_INDEX = 5;    
    
    static final String[] EMAIL_PROJECTION = new String[] {
        Data._ID, //0
        Email.TYPE, //1
        Email.LABEL, //2
        Email.DATA, //3
        Contacts.DISPLAY_NAME, // 4
        RawContacts.CONTACT_ID, // 5       
    };    

    static final int EMAIL_ID_COLUMN_INDEX = 0;
    static final int EMAIL_TYPE_COLUMN_INDEX = 1;
    static final int EMAIL_LABEL_COLUMN_INDEX = 2;
    static final int EMAIL_DATA_COLUMN_INDEX = 3;
    static final int EMAIL_DISPLAY_NAME_COLUMN_INDEX = 4;
    static final int EMAIL_CONTACT_ID_COLUMN_INDEX = 5;        

    static final String[] ALL_PROJECTION = new String[] {
    	Data._ID, //0
    	Phone.TYPE,  //1
    	Phone.LABEL, 
        Email.DATA, //3 
        Contacts.DISPLAY_NAME, // 4
        Data.MIMETYPE,  //5       
    };    
    static final int ALL_ID_COLUMN_INDEX = 0;
    static final int ALL_TYPE_COLUMN_INDEX = 1;
    static final int ALL_LABEL_COLUMN_INDEX = 2;
    static final int ALL_NUMBER_COLUMN_INDEX = 3;
    static final int ALL_DISPLAY_NAME_COLUMN_INDEX = 4;
    static final int ALL_MIMETYPE_INDEX = 5;    
    
    
    public static final int MODE_DEFAULT = 0;
    public static final int MODE_PICK_PHONE = 10;
    public static final int MODE_PICK_EMAIL = 20;
    public static final int MODE_PICK_ALL = 30;

    int mMode = MODE_DEFAULT;
    
    public static final String EXTRA_PICK_TYPE =
        "com.android.contacts.action.PICK_TYPE";

    public static final String EXTRA_PICK_DATA_ID =
        "com.android.contacts.action.PICK_DATA_ID";
    
    public static final String EXTRA_PICK_FULLLIST_URI = 
    	"com.android.contacts.action.PICK_FULLLIST_URI";
    
    public static final String  MULTIPLE_PREVIEW=
        "com.android.contacts.action.MULTIPLE_PREVIEW";
    
    private long[] mDataIds = null;
    private long[] mReturnDataIds = null;
    private ArrayList <Uri> mFullListUri = null;    
    private boolean[] mIsChecked = null;


    private class DataViewBinder implements SimpleCursorAdapter.ViewBinder {

        public DataViewBinder() {
        }
        
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int position = 0;
            if (cursor != null) {
                position = cursor.getPosition();
            } else {
                return false;
            }

            if (cursor != null && Contacts.DISPLAY_NAME.equals(cursor.getColumnName(columnIndex))) {
                String name = cursor.getString(columnIndex);
                ((TextView)view).setText(name);
                return true;
            }else if(view instanceof CheckBox){
				if (mIsChecked != null && position < mIsChecked.length) {
                    ((CheckBox)view).setChecked(mIsChecked[position]);
                    	// set listItem checked status
                    	getListView().setItemChecked(position, mIsChecked[position]);
						((CheckBox)view).setOnClickListener(new View.OnClickListener(){
							public void onClick(View view){
								checkBoxClicked(view);
							}
						});
					}
				return true;
			}else{
				int type = cursor.getInt(columnIndex); 
				String label = null;
				if (mMode == MODE_PICK_PHONE) {
					if (Phone.TYPE.equals(cursor.getColumnName(columnIndex))) {
						label = cursor.getString(PHONE_LABEL_COLUMN_INDEX);   
						((TextView)view).setText(Phone.getTypeLabel(MultiplePickPreviewActivity.this.getResources(), type, label));
						return true;
					} else if (Phone.NUMBER.equals(cursor.getColumnName(columnIndex))) {
						((TextView)view).setText(cursor.getString(columnIndex));
						return true;
					}
				} else if (mMode == MODE_PICK_EMAIL) {
					if (Email.TYPE.equals(cursor.getColumnName(columnIndex))) {
						label = cursor.getString(EMAIL_LABEL_COLUMN_INDEX);   
						((TextView)view).setText(Email.getTypeLabel(MultiplePickPreviewActivity.this.getResources(), type, label));
						return true;
					} else if (Email.DATA.equals(cursor.getColumnName(columnIndex))) {
						((TextView)view).setText(cursor.getString(columnIndex));
						return true;
					}            	
				} else if (mMode == MODE_PICK_ALL) {
					String mimetype = cursor.getString(ALL_MIMETYPE_INDEX);
					label = cursor.getString(ALL_LABEL_COLUMN_INDEX);   
            	
					if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
						//Log.v(TAG, "phone type");
						if (Phone.TYPE.equals(cursor.getColumnName(columnIndex))) {
							((TextView)view).setText(Phone.getTypeLabel(MultiplePickPreviewActivity.this.getResources(), type, label));
							return true;
						} else if (Phone.NUMBER.equals(cursor.getColumnName(columnIndex))) {
							((TextView)view).setText(cursor.getString(columnIndex));
							return true;
						}
					} else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
						//Log.v(TAG, "email type");
						if (Email.TYPE.equals(cursor.getColumnName(columnIndex))) {
							((TextView)view).setText(Email.getTypeLabel(MultiplePickPreviewActivity.this.getResources(), type, label));
							return true;
						} else if (Email.DATA.equals(cursor.getColumnName(columnIndex))) {
							((TextView)view).setText(cursor.getString(columnIndex));
							return true;
						}            	
					} 
				}
			}
            return false;
        }
        
    }


    /** Adapter class to fill in data for the GroupMemeber  */
    private final class dataListAdapter extends SimpleCursorAdapter {
        private boolean mLoading = true;

        public dataListAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {   
            super(context, 
                  layout, 
                  c, 
                  from,
                  to);                      	           	                  
        } 

        void setLoading(boolean loading) {
            mLoading = loading;
        }
        
        boolean getLoading() {
            return mLoading;
        }

        @Override
        public boolean isEmpty() {
            if (mLoading) {
                // We don't want the empty state to show when loading.
                return false;
            } else {
                return super.isEmpty();
            }
        }

        /**
         * Callback on the UI thread when the content observer on the backing cursor fires.
         * should finish whenever detecting the DB is been updated by other activities until
         * the DB is stable, because it can't gurantee data is consistent with the original one.
         */
        @Override
        protected void onContentChanged() {
            //Log.v(TAG, "onContentChanged() be called, do nothing");
        }
    }
    

    private static final class QueryHandler extends AsyncQueryHandler {
        private final WeakReference<MultiplePickPreviewActivity> mActivity;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<MultiplePickPreviewActivity>((MultiplePickPreviewActivity) context);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            final MultiplePickPreviewActivity activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {
                if (token == LIST_TOKEN) {
                    //Log.v(TAG, "onQueryComplete(),  query list finish");                	
                    final MultiplePickPreviewActivity.dataListAdapter dataAdapter = activity.mAdapter;
                    dataAdapter.setLoading(false);
                    dataAdapter.changeCursor(cursor);
                    // update the mark 
                    if (cursor != null) {
                    	// a17894 just for test
                        /*int colcnt = cursor.getColumnCount();
                        String [] colname = cursor.getColumnNames();
                        Log.v(TAG, "IAN, columncount=" + colcnt );
                        for(int i=0; i<colcnt;i++) {
                            Log.v(TAG, "IAN, column[" + i + "] = " + colname[i] );
                        } */
                        // end a17894
                    	activity.initCheckState(cursor);
                    }
                }
            } else {
                cursor.close();
            }
        }
    }
    
    
    private void initCheckState(Cursor cursor) {
    	
    	int count = cursor.getCount();
        mIsChecked = new boolean[count];
        int count1 = getListView().getCount();
        //Log.v(TAG, "count = " + count + ", count1 = " + count1);
        for (int i = 0; i < count; i++) {
            mIsChecked[i] = true;
            // getListView().setItemChecked(i, true);
        }
    	   
        long id = 0;
        int i = 0;
        mReturnDataIds = new long[count];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            id = cursor.getLong(0);
            mReturnDataIds[i++] = id;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        final String action = intent.getAction();
        
        mMode = intent.getIntExtra(EXTRA_PICK_TYPE, MODE_DEFAULT);
        if (mMode == MODE_DEFAULT)
            return;

        mDataIds = intent.getLongArrayExtra(EXTRA_PICK_DATA_ID);
        //Log.v(TAG, "mDataIds: " + mDataIds + ", length: " + mDataIds.length);
        if (mDataIds == null || mDataIds.length <= 0) {
            //Log.v(TAG, "no data in the list");
            finish();
            return;
        }

        setContentView(R.layout.muti_pick_preview_list);
        findViewById(R.id.btn_done).setOnClickListener(this);
        findViewById(R.id.btn_discard).setOnClickListener(this);
        
        String [] from = null;
        int [] to = null;
        
        if (mMode == MODE_PICK_PHONE) {
        	setTitle(getString(R.string.select_phone));
            from = new String[] {Contacts.DISPLAY_NAME, Data._ID, Phone.TYPE, Phone.NUMBER};
            to = new int[] {R.id.name, R.id.checkitembox, R.id.label, R.id.number};
        }
        else if (mMode == MODE_PICK_EMAIL) {
        	setTitle(getString(R.string.select_email));
            from = new String[] {Contacts.DISPLAY_NAME, Data._ID, Email.TYPE, Email.DATA};
            to = new int[] {R.id.name, R.id.checkitembox, R.id.label, R.id.number};
        } else if (mMode == MODE_PICK_ALL) {
        	setTitle(getString(R.string.select_contacts_preview));
        	from = new String[] {Contacts.DISPLAY_NAME, Data._ID, Email.TYPE, Email.DATA};
        	to = new int[] {R.id.name, R.id.checkitembox, R.id.label, R.id.number};
        }

        mAdapter = new dataListAdapter(this, 
                                       R.layout.muti_pick_preview_list_item,
                                       null,
                                       from,
                                       to);
                                       
        DataViewBinder viewBinder = new DataViewBinder();
        mAdapter.setViewBinder(viewBinder);

        setListAdapter(mAdapter);
        mQueryHandler = new QueryHandler(this);
        
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);   

        Button done = (Button)findViewById(R.id.btn_done);
        int count = mDataIds.length;
        if (done != null) {
        	CharSequence newText = getString(R.string.menu_done) + " (" + count + ")";       	      
            //Log.v(TAG, "done button text: "+ newText);
            done.setText(newText);
            if (count > 0 ) {
            	done.setEnabled(true);
            } else {
            	done.setEnabled(false);
            }
        }        

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }	
    }


    @Override
    protected void onResume() {
        //Log.v(TAG, "onResume be called, refresh the list");
        startQueryList();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdapter != null)
            mAdapter.changeCursor(null);
        // BEGIN Motorola, ODC_001639, 2013-01-28, SWITCHUITWO-589
        //setListAdapter(null);
        // END SWITCHUITWO-589
    }
        
    private void startQueryList() {

        Uri uri = null;
        String[] projection = null;
        String selection = null;
        
        if (mDataIds == null)
            return;

        if (mMode == MODE_PICK_PHONE) {
        	uri = Phone.CONTENT_URI;
        	projection = PHONES_PROJECTION;
            selection = Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "' AND ";
        }
        else if (mMode == MODE_PICK_EMAIL) {
        	uri = Email.CONTENT_URI;
        	projection = EMAIL_PROJECTION;
            selection = Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "' AND ";
        } else if (mMode == MODE_PICK_ALL) {
        	uri = Data.CONTENT_URI;
        	//Log.v(TAG, "!!!URI: " + uri);
        	projection = ALL_PROJECTION; 
        	selection = "";
        }

        StringBuilder where = new StringBuilder();
        where.append(Data._ID + " IN (");
        for (int i = 0; i < mDataIds.length; i++) {
            where.append(mDataIds[i]);
            if (i < mDataIds.length - 1) {
                where.append(",");
            }
        }
        where.append(")");

        selection += where.toString();
        //Log.v(TAG, "!!!!selection: " + selection);
        mAdapter.setLoading(true);

        // Cancel any pending queries
        mQueryHandler.cancelOperation(LIST_TOKEN);

        // query conditionally according to network type and call type selected
        mQueryHandler.startQuery(LIST_TOKEN, null,
                                 uri,
                                 projection,
                                 selection,
                                 null,
                                 Contacts.SORT_KEY_PRIMARY + " ASC");
    }

            
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        //Log.v(TAG, "the positon = " + position + ", the id = " + id);
        if (mIsChecked != null && l != null) {
            boolean isChecked = l.isItemChecked(position);
            mIsChecked[position] = isChecked;
			((CheckBox)v.findViewById(R.id.checkitembox)).setChecked(isChecked);
        }
   	    int count = 0;
        for (int i = 0; i < mReturnDataIds.length; i++) {
            if (mIsChecked[i]) {
            	count++;
            }
        }
        //update the selected count info
        Button done = (Button)findViewById(R.id.btn_done);
        if (done != null) {
        	CharSequence newText = getString(R.string.menu_done) + " (" + count + ")";       	      
            //Log.v(TAG, "done button text: "+ newText);
            done.setText(newText);
            if (count > 0 ) {
            	done.setEnabled(true);
            } else {
            	done.setEnabled(false);
            }
        }        

        
    }
   
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_done:
                doSaveAction();
                break;
                
            case R.id.btn_discard:
                //setResult(RESULT_CANCELED);
                //finish();
                break;
        }
    }
 
	private void doSaveAction() {
		if (mReturnDataIds != null) {// Motorola, ODC_001639, 2013-01-28, SWITCHUITWO-589
			ArrayList<Long> dataList = new ArrayList<Long>();
			for (int i = 0; i < mReturnDataIds.length; i++) {
				if (mIsChecked[i])
					dataList.add(mReturnDataIds[i]);
			}

			long[] dataIds = new long[dataList.size()];
			int i = 0;
			Iterator<Long> it = dataList.iterator();
			while (it.hasNext()) {
				Long info = (Long) it.next();
				dataIds[i++] = info;
			}

			Intent intent = new Intent();
			intent.putExtra(EXTRA_PICK_DATA_ID, dataIds);
			setResult(RESULT_OK, intent);
			finish();
		}
	}

    public void checkBoxClicked(View view) {
        final ListView l = getListView();
        if (l != null && view != null) {
            int position = l.getPositionForView((CheckBox)view);
            if (mIsChecked != null && position < mIsChecked.length) {
                boolean isChecked = !(mIsChecked[position]);
                mIsChecked[position] = isChecked;
                Log.v(TAG, "the positon = " + position );
                int count = 0;
                for (int i = 0; i < mReturnDataIds.length; i++) {
                    if (mIsChecked[i]) {
                        count++;
                    }
                }
                //update the selected count info
                Button done = (Button)findViewById(R.id.btn_done);
                if (done != null) {
                    CharSequence newText = getString(R.string.menu_done) + " (" + count + ")";       	      
                    //Log.v(TAG, "done button text: "+ newText);
                    done.setText(newText);
                    if (count > 0 ) {
                        done.setEnabled(true);
                    } else {
                        done.setEnabled(false);
                    }
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }       
}
