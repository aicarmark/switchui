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
 * limitations under the License
 */

package com.motorola.contacts.group;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactSaveService;
import com.android.contacts.R;
import com.motorola.contacts.activities.GroupConfirmActivity;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
import com.android.internal.util.Objects;

import com.motorola.contacts.group.LocalGroupUtils;
import com.motorola.contacts.groups.GroupAPI;
import com.motorola.contacts.group.GroupMemberDataLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.BaseTypes;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GroupConfirmFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "GroupConfirmFragment";
    private static final boolean DEBUG = true;

    private static final String KEY_ACTION = "action";
    private static final String KEY_ACTION_MIME_TYPE = "actionMimeType";
    private static final String KEY_GROUP_TITLE = "groupTitle";
    private static final String KEY_STATUS = "status";
    private static final String KEY_ALL_MEMBERS_DATA = "allMembersData";
    private static final String KEY_NO_SELECTION = "noSelection";
    private static String NOT_SELECT_STRING;

    private static final String GROUP_CONFIRM_TAG = "groupConfirmation";

    public static interface Listener {
        /**
         * Group metadata was not found, close the fragment now.
         */
        public void onGroupNotFound();

        /**
         * User has tapped Revert, close the fragment now.
         */
        void onReverted();

        /**
         * Contact was saved and the Fragment can now be closed safely.
         */
        void onSaveFinished(int resultCode, Intent resultIntent);

        /**
         * Fragment is created but there's no accounts set up.
         */
        void onAccountsNotFound();
    }

    private static final int LOADER_MEMBER_DATA = 1;
    private static final int LOADER_MEMBER_NO_DATA = 2;
    private static final int LOADER_MEMBER = 3;

    private static final int MAX_MEMBER_LIMIT = 300;

    public static final String SAVE_MODE_EXTRA_KEY = "saveMode";

    /**
     * Modes that specify the status of the editor
     */
    public enum Status {
        SELECTING_ACCOUNT, // Account select dialog is showing
        LOADING,    // Loader is fetching the group metadata
        EDITING,    // Not currently busy. We are waiting forthe user to enter data.
        CLOSING     // Prevents any more saves
    }

    private Context mContext;
    private String mAction;
    private String mActionMimetype;
    private Bundle mIntentExtras;
    private String mGroupTitle;
    private Listener mListener;
    private String mContactId;
    private Cursor mContactCursor;
    private TextView mContactItemView;
    private TextView mDataLabelView;

    private Status mStatus;

    private ViewGroup mRootView;
    private ListView mListView;
    private LayoutInflater mLayoutInflater;

    private int mLastGroupEditorId;

    private ContactPhotoManager mPhotoManager;
    private ContentResolver mContentResolver;

    private ArrayList<String> rtnData = new ArrayList<String>();
    private ArrayList<String> mContactIdsArray = new ArrayList<String>();
    private Bundle mIdNumberBundle = new Bundle();
    private Bundle mIdNotSelectBundle = new Bundle();
    private Cursor mListCursor;
    private Cursor mListNoDataCursor;
    private long mFirstContactId = -1;
    private long mFirstContactIdNoData = -1;

    public GroupConfirmFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mLayoutInflater = inflater;
        mRootView = (ViewGroup) inflater.inflate(R.layout.group_editor_fragment, container, false);
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mPhotoManager = ContactPhotoManager.getInstance(mContext);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // Just restore from the saved state.  No loading.
            onRestoreInstanceState(savedInstanceState);
            startGroupMetaDataLoader();
        } else if (mAction.equals(GroupAPI.GroupIntents.ACTION_CONFIRM_GROUP)) {
            startGroupMetaDataLoader();
        } else {
            throw new IllegalArgumentException("Unknown Action String " + mAction +
                    ". Only support " + Intent.ACTION_EDIT + " or " + Intent.ACTION_INSERT);
        }

        if(mActionMimetype.equals(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_CONFIRM_TYPE_SMS)) {
            NOT_SELECT_STRING = mContext.getString(R.string.moto_do_not_send_message);
        } else {
            NOT_SELECT_STRING = mContext.getString(R.string.moto_do_not_email);
        }
    }

    private void startGroupMetaDataLoader() {
        mStatus = Status.LOADING;
            getLoaderManager().initLoader(LOADER_MEMBER_DATA, null,
                    mGroupMemberDataLoaderListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putString(KEY_ACTION_MIME_TYPE, mActionMimetype);
        outState.putString(KEY_GROUP_TITLE, mGroupTitle);
        outState.putSerializable(KEY_STATUS, mStatus);
        outState.putBundle(KEY_ALL_MEMBERS_DATA, mIdNumberBundle);
        outState.putBundle(KEY_NO_SELECTION, mIdNotSelectBundle);
    }

    private void onRestoreInstanceState(Bundle state) {
        mAction = state.getString(KEY_ACTION);
        mActionMimetype = state.getString(KEY_ACTION_MIME_TYPE);
        mGroupTitle = state.getString(KEY_GROUP_TITLE);
        mStatus = (Status) state.getSerializable(KEY_STATUS);
        mIdNumberBundle = state.getBundle(KEY_ALL_MEMBERS_DATA);
        mIdNotSelectBundle = state.getBundle(KEY_NO_SELECTION);
        rtnData = new ArrayList<String>();
        for(String key : mIdNumberBundle.keySet()) {
            rtnData.add(mIdNumberBundle.getString(key));
        }
    }

    public void setContentResolver(ContentResolver resolver) {
        mContentResolver = resolver;
    }

    /**
     * Sets up the editor based on the group's account name and type.
     */
    private void setupEditorForAccount(Cursor data) {
        boolean isNewEditor = false;

        // Since this method can be called multiple time, remove old editor if the editor type
        // is different from the new one and mark the editor with a tag so it can be found for
        // removal if needed
        View editorView;
        int newGroupEditorId = R.layout.group_confirm_view;
        if (newGroupEditorId != mLastGroupEditorId) {
            View oldEditorView = mRootView.findViewWithTag(GROUP_CONFIRM_TAG);
            if (oldEditorView != null) {
                mRootView.removeView(oldEditorView);
            }
            editorView = mLayoutInflater.inflate(newGroupEditorId, mRootView, false);
            editorView.setTag(GROUP_CONFIRM_TAG);
            mLastGroupEditorId = newGroupEditorId;
            isNewEditor = true;
        } else {
            editorView = mRootView.findViewWithTag(GROUP_CONFIRM_TAG);
            if (editorView == null) {
                throw new IllegalStateException("Group editor view not found");
            }
        }

        myCursorAdapter adapter = new myCursorAdapter(mContext, data, false);
        mListView = (ListView) editorView.findViewById(android.R.id.list);
        mListView.setAdapter(adapter);
        mListView.setItemsCanFocus(true);
        mListView.setFocusableInTouchMode(true);
        mListView.requestFocus();
        mListView.setOnItemClickListener(this);


        if(isNewEditor) {
            mRootView.addView(editorView);
        }
        mStatus = Status.EDITING;
    }

    public void load(String action, String actionMimetype, String groupTitle, Bundle intentExtras) {
        mAction = action;
        mActionMimetype = actionMimetype;
        mGroupTitle = groupTitle;
        mIntentExtras = intentExtras;
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    public void onDoneClicked() {
        if(rtnData.size() > MAX_MEMBER_LIMIT) {
            Toast.makeText(mContext, R.string.group_message_limit, Toast.LENGTH_SHORT).show();
            return;
        } else {
            String[] dataList = rtnData.toArray(new String[0]);
            Intent i = new Intent();
            i.putExtra(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_STRING_ARRAY_CONFIRMED_DATALIST, dataList);
            if (mListener != null) {
                mListener.onSaveFinished( Activity.RESULT_OK, i );
            }
        }
    }

    /**
     * Saves or creates the group based on the mode, and if successful
     * finishes the activity. This actually only handles saving the group name.
     * @return true when successful
     */

    public void onSaveCompleted(boolean hadChanges, int saveMode, String groupTitle) {
        boolean success = groupTitle != null;
        Uri groupUri;
        Log.d(TAG, "onSaveCompleted(" + saveMode + ", " + groupTitle + ")");
        if (hadChanges) {
            Toast.makeText(mContext, success ? R.string.groupSavedToast :
                    R.string.groupSavedErrorToast, Toast.LENGTH_SHORT).show();
        }
        switch (saveMode) {
            case SaveMode.CLOSE:
            case SaveMode.HOME:
                break;
            case SaveMode.RELOAD:
                // TODO: Handle reloading the group list
            default:
                throw new IllegalStateException("Unsupported save mode " + saveMode);
        }
    }

        protected boolean isCustom(Integer type) {
            return type == BaseTypes.TYPE_CUSTOM;
        }

        protected CharSequence getTypeLabel(Resources res, Integer type, CharSequence label) {
            final int labelRes; 
            if(mActionMimetype.equals(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_CONFIRM_TYPE_SMS)) {
                labelRes = Phone.getTypeLabelResource(type);
            } else {
                labelRes = Email.getTypeLabelResource(type);
            }
            if (type == null) {
                return res.getText(labelRes);
            } else if (isCustom(type)) {
                return res.getString(labelRes, label == null ? "" : label);
            } else {
                return res.getText(labelRes);
            }
        }
      //begin add by txbv34 for IKCBSMMCPPRC-1427
        protected CharSequence getMimeTypeLabel(Resources res,String MimeType ,Integer type, CharSequence label) {
            final int labelRes; 
            if(DEBUG)Log.d(TAG,"getMimeTypeLabel,MimeType=" + MimeType);
            if(DEBUG)Log.d(TAG,"getMimeTypeLabel,type=" + type);
            if(MimeType.equals(GroupAPI.MIME_EMAIL_ADDRESS)) {
            	labelRes = Email.getTypeLabelResource(type);                
            } else {
            	labelRes = Phone.getTypeLabelResource(type);
            }
            if(DEBUG)Log.d(TAG,"getMimeTypeLabel,labelRes=" + labelRes);
            if (type == null) {
                return res.getText(labelRes);
            } else if (isCustom(type)) {
                return res.getString(labelRes, label == null ? "" : label);
            } else {
                return res.getText(labelRes);
            }
        }
    /**
     * The listener for the group memeber data (phone number and email) loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mMemberDataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            int selectionType = GroupMemberDataLoader.SELECTION_TYP_BOTH; //default select all types
            if(mActionMimetype.equals(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_CONFIRM_TYPE_SMS)) {
                //MOT MOD BEGIN IKMAIN-35771
                selectionType = GroupMemberDataLoader.SELECTION_TYP_BOTH;
                //MOT MOD END IKMAIN-35771
            } else {
                selectionType = GroupMemberDataLoader.SELECTION_TYP_EMAIL;
            }
            return new GroupMemberDataLoader(mContext, mContactId, selectionType, false);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            ArrayList<MemberData> contactData = new ArrayList<MemberData>();
            ArrayList<String> numberList = new ArrayList<String>();
            data.moveToPosition(-1);
            long contactId = -1;
            while (data.moveToNext()) {
                long rawContactId = data.getLong( GroupMemberDataLoader.RAW_CONTACT_ID_COLUMN_INDEX );
                String mimetype = data.getString( GroupMemberDataLoader.MIMETYPE_ID_COLUMN_INDEX );
                String data1 = data.getString( GroupMemberDataLoader.DATA1_COLUMN_INDEX );
                String data2 = data.getString( GroupMemberDataLoader.DATA2_COLUMN_INDEX );
                String data3 = data.getString( GroupMemberDataLoader.DATA3_COLUMN_INDEX );
                long dataId = data.getLong( GroupMemberDataLoader.ID_COLUMN_INDEX ); // MOT CHINA
                contactId = data.getLong( GroupMemberDataLoader.CONTACT_ID_COLUMN_INDEX );
                MemberData memberData = new MemberData(rawContactId, mimetype, data1, data2, data3, contactId, dataId);
                if(!numberList.contains(data1)) {
                    contactData.add(memberData);
                    numberList.add(data1);
                }
            }
            MemberData memberData = new MemberData(-1, null, NOT_SELECT_STRING, null, null, contactId);
            contactData.add(memberData);

            ListView dataSelectList = new ListView( mContext );
            dataSelectList.setAdapter( new MessageOptionsAdapter( mContext, contactData ) );

            // Build a dialog to hold the selection list.
            AlertDialog.Builder builder = new AlertDialog.Builder( mContext );
            if(mActionMimetype.equals(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_CONFIRM_TYPE_SMS)) {
                builder.setTitle(R.string.moto_group_select_number);
            }
            else {
                builder.setTitle(R.string.moto_group_select_email);
            }
            builder.setView( dataSelectList );
            final AlertDialog dialog = builder.create();
            dialog.show();

            dataSelectList.setOnItemClickListener( new OnItemClickListener() {

                public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                    MemberData selectedData =  (MemberData)
                        ((MessageOptionsAdapter) arg0.getAdapter()).getItem(position);
                    String data1 = selectedData.getData1();
                    String contactId = String.valueOf(selectedData.getContactId());

                    String previousData = mContactItemView.getText().toString();
                    Integer type = null;
                    String label = selectedData.getData3();
                    if(selectedData.getData2() != null) {
                        type = new Integer(selectedData.getData2());
                        if(DEBUG)Log.d(TAG,"onItemClick,getTypeLabel");
                        CharSequence typeName =  getTypeLabel(mContext.getResources(), type, label);
                        String selectedLabel = " (" + typeName + ")";
                        mDataLabelView.setText(selectedLabel);
                    }
                    mContactItemView.setText(data1);
                    if(rtnData.contains(previousData)) {
                        rtnData.remove(previousData);
                        mIdNumberBundle.remove("contactId:"+contactId);
                    }
                    if(!data1.equals(NOT_SELECT_STRING)) {
                        rtnData.add(data1);
                        mIdNumberBundle.putString("contactId:"+contactId, data1);
                        if(mIdNotSelectBundle.containsKey("contactId:"+contactId)) {
                            mIdNotSelectBundle.remove("contactId:"+contactId);
                        }

                        // MOT CHINA Set user selected item as default/primary
                        Intent setIntent = ContactSaveService.createSetSuperPrimaryIntent(mContext, selectedData.getDataId());
                        mContext.startService(setIntent);
                    } else {
                        mIdNotSelectBundle.putString("contactId:"+contactId, NOT_SELECT_STRING);
                        mDataLabelView.setText("");
                    }
                    dialog.dismiss();
                }

            });

            getLoaderManager().destroyLoader(LOADER_MEMBER);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The listener for the group memeber data (phone number and email) loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberNoDataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            int selectionType = GroupMemberDataLoader.SELECTION_TYP_BOTH; //default select all types
            if(mActionMimetype.equals(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_CONFIRM_TYPE_SMS)) {
                //MOT MOD BEGIN IKMAIN-35771
                selectionType = GroupMemberDataLoader.SELECTION_TYP_BOTH;
                //MOT MOD END IKMAIN-35771
            } else {
                selectionType = GroupMemberDataLoader.SELECTION_TYP_EMAIL;
            }

            return new GroupMemberDataLoader(mContext, mGroupTitle, selectionType, true, false);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mListNoDataCursor = data;
            mListCursor.moveToFirst();
            mListNoDataCursor.moveToFirst();
            if( mListCursor.getCount()>0 ) {
                mFirstContactId = mListCursor.getLong(GroupMemberDataLoader.CONTACT_ID_COLUMN_INDEX);
            }
            if( mListNoDataCursor.getCount()>0 ) {
                mFirstContactIdNoData = mListNoDataCursor.getLong(GroupMemberDataLoader.CONTACT_ID_COLUMN_INDEX);
            }
            mListCursor.moveToPosition(-1);
            mListNoDataCursor.moveToPosition(-1);
            Cursor totalCursor = new MergeCursor(new Cursor[]{mListCursor,mListNoDataCursor});
            setupEditorForAccount(totalCursor);

        }
        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The listener for the group memeber data (phone number and email) loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberDataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            int selectionType = GroupMemberDataLoader.SELECTION_TYP_BOTH; //default select all types
            if(mActionMimetype.equals(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_CONFIRM_TYPE_SMS)) {
                //MOT MOD BEGIN IKMAIN-35771
                selectionType = GroupMemberDataLoader.SELECTION_TYP_BOTH;
                //MOT MOD BEGIN IKMAIN-35771
            } else {
                selectionType = GroupMemberDataLoader.SELECTION_TYP_EMAIL;
            }

            return new GroupMemberDataLoader(mContext, mGroupTitle, selectionType, true, true);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mListCursor = data;
            while (mListCursor.moveToNext()) {
                String data1 = mListCursor.getString(GroupMemberDataLoader.DATA1_COLUMN_INDEX);
                long contactId = mListCursor.getLong(GroupMemberDataLoader.CONTACT_ID_COLUMN_INDEX); 
                if(!mIdNumberBundle.containsKey("contactId:"+contactId)
                   && !mIdNotSelectBundle.containsKey("contactId:"+contactId)) {
                    rtnData.add(data1);
                    mIdNumberBundle.putString("contactId:"+contactId, data1);
                }
            }

            getLoaderManager().initLoader(LOADER_MEMBER_NO_DATA, null,
                    mGroupMemberNoDataLoaderListener);

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * This represents a single data(phone or email) of the current member.
     */
    public static class MemberData implements Parcelable {
        private static final MemberData[] EMPTY_ARRAY = new MemberData[0];

        // TODO: Switch to just dealing with raw contact IDs everywhere if possible
        private final long mRawContactId;
        private final String mMimetype;
        private final String mData1;
        private final String mData2;
        private final String mData3;
        private final long mContactId;
        private final long mDataId; // MOT CHINA

        public MemberData(long rawContactId, String mimetype, String data1, 
          String data2, String data3, long contactId) {
            mRawContactId = rawContactId;
            mMimetype = mimetype;
            mData1 = data1;
            mData2 = data2;
            mData3 = data3;
            mContactId = contactId;
            mDataId = 0; // MOT CHINA
        }

        // MOT CHINA
        public MemberData(long rawContactId, String mimetype, String data1, String data2, String data3, long contactId, long dataId) {
            mRawContactId = rawContactId;
            mMimetype = mimetype;
            mData1 = data1;
            mData2 = data2;
            mData3 = data3;
            mContactId = contactId;
            mDataId = dataId;
        }

        public long getRawContactId() {
            return mRawContactId;
        }

        public String getMimetype() {
            return mMimetype;
        }

        public String getData1() {
            return mData1;
        }

        public String getData2() {
            return mData2;
        }

        public String getData3() {
            return mData3;
        }

        public long getContactId() {
            return mContactId;
        }

        // MOT CHINA
        public long getDataId() {
            return mDataId;
        }

        // Parcelable
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(mRawContactId);
            dest.writeString(mMimetype);
            dest.writeString(mData1);
            dest.writeString(mData2);
            dest.writeString(mData3);
            dest.writeLong(mContactId);
            dest.writeLong(mDataId); // MOT CHINA
        }

        private MemberData(Parcel in) {
            mRawContactId = in.readLong();
            mMimetype = in.readString();
            mData1 = in.readString();
            mData2 = in.readString();
            mData3 = in.readString();
            mContactId = in.readLong();
            mDataId = in.readLong(); // MOT CHINA
        }

        public static final Parcelable.Creator<MemberData> CREATOR = new Parcelable.Creator<MemberData>() {
            public MemberData createFromParcel(Parcel in) {
                return new MemberData(in);
            }

            public MemberData[] newArray(int size) {
                return new MemberData[size];
            }
        };
    }

    /**
     * Default (fallback) list item click listener.
     * This listener is used for other kind of entries.
     */

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String contactId = view.getTag(R.string.item_contact_id).toString();
            mContactItemView = (TextView) view.findViewById(R.id.number);
            mDataLabelView = (TextView) view.findViewById(R.id.label);
            mContactId = "(" + contactId + ")";
            getLoaderManager().initLoader(LOADER_MEMBER, null, mMemberDataLoaderListener);
    }


    //================================================================
    // MessageOptionsAdapter
    //================================================================

    private class MessageOptionsAdapter extends ArrayAdapter<MemberData> {
        public MessageOptionsAdapter (Context context, ArrayList<MemberData> contactDataList) {
            super (context, R.layout.moto_dialog_item_single_checked, contactDataList);
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            MemberData singleData = (MemberData) getItem(position);
            String data1 = singleData.getData1();
            TextView view = (TextView) super.getView(position, convertView, parent);
            String viewText = null;
            if(data1 == null || data1.equals("")) {
                viewText = NOT_SELECT_STRING;
            } else {
                    Integer type = null;
                    String label = singleData.getData3();
                    if(singleData.getData2() != null) {
                        type = new Integer(singleData.getData2());
                        //begin modify by txbv34 for IKCBSMMCPPRC-1427
                        if(DEBUG)Log.d(TAG,"getView,getTypeLabel");
                        String Mimetype = singleData.getMimetype();
                        CharSequence typeName =  getMimeTypeLabel(mContext.getResources(),Mimetype, type, label);
                        //end modify by txbv34 for IKCBSMMCPPRC-1427
//                        CharSequence typeName =  getTypeLabel(mContext.getResources(), type, label);
                    
                        String selectedLabel = " (" + typeName + ")";
                        viewText = data1 + selectedLabel;
                    } else {
                        viewText = data1;
                    }
            }
            view.setText( viewText );
            view.setEllipsize( TextUtils.TruncateAt.END );
            return view;
        }
    }

    private final class myCursorAdapter extends CursorAdapter{

        private LayoutInflater mInflater;
        private Context mContext;
        private boolean mNoData;

        private final class ViewHolder {
            public QuickContactBadge badge;
            public TextView name;
            public TextView number;
            public TextView label;
            public TextView noData;
            public TextView groupName;
        }

        public myCursorAdapter(Context context, Cursor c, boolean noData) {  
            //MOT MOD BEGIN IKMAIN-36222
            super(context, c, 0);
            //MOT MOD END IKMAIN-36222
            mContext = context;
            mContactCursor = c;
            mNoData = noData;
            mInflater = LayoutInflater.from(context);
            // TODO Auto-generated constructor stub
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            String lookupKey = cursor.getString(GroupMemberDataLoader.LOOKUP_KEY_COLUMN_INDEX);
            long contactId = cursor.getLong(GroupMemberDataLoader.CONTACT_ID_COLUMN_INDEX);
            String mimetype = cursor.getString(GroupMemberDataLoader.MIMETYPE_ID_COLUMN_INDEX);
            String displayName = cursor.getString(GroupMemberDataLoader.DISPLAY_NAME_COLUMN_INDEX);
            String data1 = cursor.getString(GroupMemberDataLoader.DATA1_COLUMN_INDEX);
            String photoUri = cursor.getString(GroupMemberDataLoader.PHOTO_URI_COLUMN_INDEX);

            ViewHolder holder = (ViewHolder) view.getTag(R.string.view_holder);
            QuickContactBadge badge = holder.badge;
            badge.assignContactUri(Contacts.getLookupUri(contactId, lookupKey));

            TextView name = holder.name;
            name.setText(displayName);
            TextView number = holder.number;
            String previousNumber = number.getText().toString();
            TextView labelView = holder.label; 
            TextView noDataView = holder.noData;
            noDataView.setVisibility(View.GONE);
            TextView groupNameView = holder.groupName;
            groupNameView.setVisibility(View.GONE);

            if(mimetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                if(contactId == mFirstContactIdNoData) {
                    noDataView.setVisibility(View.VISIBLE);
                }
                if(mActionMimetype.equals(GroupAPI.GroupIntents.ConfirmGroup.INTENTEXTRA_CONFIRM_TYPE_SMS)) {
                    noDataView.setText(R.string.noNumbers);
                } else {
                    noDataView.setText(R.string.noEmails);
                }
                number.setVisibility(View.GONE);
                labelView.setVisibility(View.GONE);
            } else {
                number.setVisibility(View.VISIBLE);
                labelView.setVisibility(View.VISIBLE);
                if(contactId == mFirstContactId) {
                    groupNameView.setVisibility(View.VISIBLE);
                    groupNameView.setText(mContext.getString(R.string.moto_group_confirm_assignment) 
                      + " " + mGroupTitle);
                }

                Integer type = null;
                String label = cursor.getString(GroupMemberDataLoader.DATA3_COLUMN_INDEX);
                if(cursor.getString(GroupMemberDataLoader.DATA2_COLUMN_INDEX)!= null) {
                    type = new Integer(cursor.getString(GroupMemberDataLoader.DATA2_COLUMN_INDEX));
                    if(DEBUG)Log.d(TAG,"bindView,getTypeLabel");
                    CharSequence typeName =  getTypeLabel(mContext.getResources(), type, label);
                    String selectedLabel = " (" + typeName + ")";
                    labelView.setText(selectedLabel);
                }
                String savedNumber = mIdNumberBundle.getString("contactId:"+contactId);
                if(savedNumber == null) {
                    if(mIdNotSelectBundle.containsKey("contactId:"+contactId)) {
                        number.setText(NOT_SELECT_STRING);
                        labelView.setText("");
                    } else {
                        number.setText(data1);
                    }
                } else {
                    number.setText(savedNumber);
                }
            }
            if(!mimetype.equals(StructuredName.CONTENT_ITEM_TYPE) 
               && !mIdNumberBundle.containsKey("contactId:"+contactId)) {
                if(previousNumber.equals("")) {
                    if(!mIdNotSelectBundle.containsKey("contactId:"+contactId)) {
                        rtnData.add(data1);
                        mIdNumberBundle.putString("contactId:"+contactId, data1);
                    }
                }
            }
            view.setTag(R.string.item_contact_id, String.valueOf(contactId));
            Uri photo = (photoUri != null) ? Uri.parse(photoUri) : null;
            mPhotoManager.loadPhoto(badge, photo, false, mContext.getResources().getBoolean(R.bool.contacts_dark_ui));


          //super.bindView(view, context, cursor);
        }

        @Override
        public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {  
             View convertView = mInflater.inflate(R.layout.group_member_confirm_item, arg2, false);
             ViewHolder holder = new ViewHolder();
             holder.badge = (QuickContactBadge) convertView.findViewById(R.id.badge);
             holder.name = (TextView) convertView.findViewById(R.id.name);
             holder.number = (TextView) convertView.findViewById(R.id.number);
             holder.label = (TextView) convertView.findViewById(R.id.label);
             holder.noData = (TextView) convertView.findViewById(R.id.no_data);
             holder.groupName = (TextView) convertView.findViewById(R.id.group_name);
             convertView.setTag(R.string.view_holder, holder);
             return convertView;
        }
    }
}
