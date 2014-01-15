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

package com.motorola.contacts.group;

import com.android.contacts.ContactSaveService;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDeltaList;
import com.android.contacts.model.EntityModifier;
import com.android.contacts.model.HardCodedSources;
import com.android.contacts.SimUtility;
import com.google.android.collect.Lists;
import com.google.android.collect.Sets;

import com.motorola.contacts.group.LocalGroupAccountMemberData;
import com.motorola.contacts.group.LocalGroupUtils;
import com.motorola.contacts.groups.GroupAPI;

import android.accounts.Account;
import android.app.Activity;
import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Profile;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A service responsible for saving changes to the content provider.
 */
public class LocalGroupSaveService extends ContactSaveService {
    private static final String TAG = "LocalGroupSaveService";

    /** Set to true in order to view logs on content provider operations */
    private static final boolean DEBUG = true;

    public static final String ACTION_CREATE_LOCAL_GROUP = "createLocalGroup";
    public static final String EXTRA_LOCAL_GROUP_ACCOUNT_MEMBER_DATA = "localGroupAccountMemberData";

    public static final String ACTION_DELETE_LOCAL_GROUP = "deleteLocalGroup";
    public static final String EXTRA_LOCAL_GROUP_LABEL = "localGroupLabel";

    public static final String ACTION_UPDATE_LOCAL_GROUP = "updateLocalGroup";
    public static final String EXTRA_GROUP_ORIGINAL_LABEL = "originalGroupLabel";
    public static final String EXTRA_CONTACTS_TO_REMOVE = "contactsToRemove";

    private static final int CREATE_GROUP_MAX_BATCH_SIZE = 450;
    private static final int DELETE_GROUP_MAX_BATCH_SIZE = 20;
    private static final int RENAME_GROUP_MAX_BATCH_SIZE = DELETE_GROUP_MAX_BATCH_SIZE;
    private static final int REMOVE_MEMBER_MAX_BATCH_SIZE = CREATE_GROUP_MAX_BATCH_SIZE;

    private static boolean ASSERT_USED = true;

    public LocalGroupSaveService() {
        super();
    }

    private static final CopyOnWriteArrayList<Listener> sListeners =
        new CopyOnWriteArrayList<Listener>();

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (ACTION_CREATE_LOCAL_GROUP.equals(action)) {
            createLocalGroup(intent);
        } else if (ACTION_DELETE_LOCAL_GROUP.equals(action)) {
            deleteLocalGroup(intent);
        } else if (ACTION_UPDATE_LOCAL_GROUP.equals(action)) {
            updateLocalGroup(intent);
        }
    }

    /** gfj763
     * Creates an intent that can be sent to this service to create a new local group as
     * well as add new members at the same time.
     *
     * @param context of the application
     * @param accountList in which the group should be created
     * @param callbackActivity is the activity to send the callback intent to
     * @param callbackAction is the intent action for the callback intent
     */

    public static Intent createNewLocalGroupIntent(Context context, ArrayList<LocalGroupAccountMemberData> groupDataArray,
            Class<?> callbackActivity, String callbackAction) {
        Intent serviceIntent = new Intent(context, LocalGroupSaveService.class);
        serviceIntent.setAction(LocalGroupSaveService.ACTION_CREATE_LOCAL_GROUP);
        serviceIntent.putParcelableArrayListExtra(LocalGroupSaveService.EXTRA_LOCAL_GROUP_ACCOUNT_MEMBER_DATA, groupDataArray);

        // Callback intent will be invoked by the service once the new group is
        // created.
        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.setAction(callbackAction);
        serviceIntent.putExtra(LocalGroupSaveService.EXTRA_CALLBACK_INTENT, callbackIntent);

        return serviceIntent;
    }

    /*
     * To get the total operation count for create the new group
     */
    private int getOpCountForNewGroup(ArrayList<LocalGroupAccountMemberData> groupDataArray) {
        if ((null == groupDataArray) || (groupDataArray.size() == 0)) {
            return 0;
        }

        int totalOpCount = 0;
        final int groupOpCount = groupDataArray.size();
        final int memberOpTimes = (ASSERT_USED == true) ? 2:1;
        int memberOpCount = 0;
        ArrayList<Long> memberIdList;

        for (LocalGroupAccountMemberData groupData: groupDataArray) {
            memberIdList = groupData.getRawContactIdList();
            if (null != memberIdList) {
                memberOpCount += memberIdList.size()*memberOpTimes;
            }
        }

        totalOpCount = groupOpCount + memberOpCount;

        if (DEBUG) {
            Log.d(TAG, "getOpCountForNewGroup, groupOpCount = " + groupOpCount
                    + ", memberOpCount = " + memberOpCount
                    + ", totalOpCount = " + totalOpCount);
        }
        return totalOpCount;
    }

    /*
     * Create group in one operation batch
     */
    private void createGroupInOneBatch(ContentResolver resolver,
            ArrayList<LocalGroupAccountMemberData> groupDataArray) {

        int total_opt_count = 0;
        int group_opt_count = 0;
        final ArrayList<ContentProviderOperation> createNewGroupOperations = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder insertBuilder = null;
        ContentProviderOperation.Builder assertBuilder = null;

        for (LocalGroupAccountMemberData groupData: groupDataArray) {
            String label = groupData.getGroupTitle();
            String accountType = groupData.getAccountType();
            String accountName = groupData.getAccountName();
            String dataSet = groupData.getDataSet();
            boolean isGroupMembershipEditable = groupData.isGroupMembershipEditable();

            insertBuilder = ContentProviderOperation.newInsert(Groups.CONTENT_URI);
            insertBuilder.withValue(Groups.ACCOUNT_TYPE, accountType);
            insertBuilder.withValue(Groups.ACCOUNT_NAME, accountName);
            insertBuilder.withValue(Groups.DATA_SET, dataSet);
            insertBuilder.withValue(Groups.TITLE, label);
            insertBuilder.withValue(Groups.GROUP_VISIBLE, 1); // Motorola, PIM Contacts, gfj763, IKHSS7-5526
            createNewGroupOperations.add(insertBuilder.build());

            group_opt_count = total_opt_count;
            total_opt_count++;

            // Add new group members
            ArrayList<Long> rawContactIdList = groupData.getRawContactIdList();
            final long[] rawContactsToAdd = LocalGroupUtils.convertToLongArray(rawContactIdList);

            if (rawContactsToAdd.length > 0) {
                for (long rawContactId : rawContactsToAdd) {
                    // Build an assert operation to ensure the contact is not already in the group

                    assertBuilder = ContentProviderOperation.newAssertQuery(Data.CONTENT_URI);

                    assertBuilder.withSelection(Data.RAW_CONTACT_ID + "=? AND " +
                            Data.MIMETYPE + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
                            new String[] { String.valueOf(rawContactId),
                            GroupMembership.CONTENT_ITEM_TYPE, null});
                    assertBuilder.withSelectionBackReference(2, group_opt_count);

                    assertBuilder.withExpectedCount(0);
                    createNewGroupOperations.add(assertBuilder.build());
                    total_opt_count++;

                    // Build an insert operation to add the contact to the group
                    insertBuilder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                    insertBuilder.withValueBackReference(GroupMembership.GROUP_ROW_ID, group_opt_count);
                    insertBuilder.withValue(Data.RAW_CONTACT_ID, rawContactId);
                    // MOT MOD BEGIN - IKHSS6-1470: group redesign 2nd release, to add new mimetype for localgroup
                    if (isGroupMembershipEditable) {
                        insertBuilder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
                    } else {
                        insertBuilder.withValue(Data.MIMETYPE, GroupAPI.LOCAL_GROUP_MIMETYPE);
                    }
                    // MOT MOD END - IKHSS6-1470
                    createNewGroupOperations.add(insertBuilder.build());
                    total_opt_count++;
                }
            }
        }

        if (DEBUG) {
            for (ContentProviderOperation operation : createNewGroupOperations) {
                Log.v(TAG, operation.toString());
            }
        }

        ContentProviderResult[] results = null;
        try {
            if (!createNewGroupOperations.isEmpty()) {
                results = resolver.applyBatch(ContactsContract.AUTHORITY, createNewGroupOperations);
            }
        } catch (RemoteException e) {
                // Something went wrong, bail without success
                Log.e(TAG, "Problem persisting for adding group and its member: ", e);
        } catch (OperationApplicationException e) {
                // The assert could have failed because the contact is already in the group,
                // just continue to the next contact
                Log.w(TAG, "Assert failed in adding group and its member ", e);
        }

    }


    private void insertMembersToLocalGroup(ContentResolver resolver, long[] rawContactsToAdd,
            long groupId, boolean isGroupMembershipEditable) {
        final ArrayList<ContentProviderOperation> rawContactOperations = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder insertBuilder = null;
        ContentProviderOperation.Builder assertBuilder = null;
        int total_opt_count = 0;

        for (long rawContactId : rawContactsToAdd) {
            // Build an assert operation to ensure the contact is not already in the group
            assertBuilder = ContentProviderOperation.newAssertQuery(Data.CONTENT_URI);

            assertBuilder.withSelection(Data.RAW_CONTACT_ID + "=? AND " +
                    Data.MIMETYPE + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
                    new String[] { String.valueOf(rawContactId),
                    GroupMembership.CONTENT_ITEM_TYPE, String.valueOf(groupId)});
            assertBuilder.withExpectedCount(0);
            rawContactOperations.add(assertBuilder.build());
            total_opt_count++;

            // Build an insert operation to add the contact to the group
            insertBuilder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
            insertBuilder.withValue(GroupMembership.GROUP_ROW_ID, groupId);
            insertBuilder.withValue(Data.RAW_CONTACT_ID, rawContactId);
            // MOT MOD BEGIN - IKHSS6-1470: group redesign 2nd release, to add new mimetype for localgroup
            if (isGroupMembershipEditable) {
                insertBuilder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
            } else {
                insertBuilder.withValue(Data.MIMETYPE, GroupAPI.LOCAL_GROUP_MIMETYPE);
            }

            // MOT MOD END - IKHSS6-1470
            rawContactOperations.add(insertBuilder.build());
            total_opt_count++;

            if (total_opt_count >= CREATE_GROUP_MAX_BATCH_SIZE) {

                if (DEBUG) {
                    Log.d(TAG, "addMembersToLocalGroup applyBatch, opt_count = " + total_opt_count);
                }

                if (DEBUG) {
                    for (ContentProviderOperation operation : rawContactOperations) {
                        Log.v(TAG, operation.toString());
                    }
                }

                ContentProviderResult[] results = null;
                try {
                    if (!rawContactOperations.isEmpty()) {
                        results = resolver.applyBatch(ContactsContract.AUTHORITY, rawContactOperations);
                    }
                } catch (RemoteException e) {
                        // Something went wrong, bail without success
                        Log.e(TAG, "Problem persisting user edits for raw contact ID: ", e);
                } catch (OperationApplicationException e) {
                        // The assert could have failed because the raw_contact is already in the group,
                        // just continue to the next contact
                        Log.w(TAG, "Assert failed in adding raw contact ID: ", e);
                } finally {
                    rawContactOperations.clear();
                    total_opt_count = 0;
                }
            }

        }

        // Finish the job if total operations < 400 or %400 left
        if (!rawContactOperations.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "addMembersToLocalGroup(), applyBatch left, opt_count = " + total_opt_count);
            }

            if (DEBUG) {
                for (ContentProviderOperation operation : rawContactOperations) {
                    Log.v(TAG, operation.toString());
                }
            }

            ContentProviderResult[] results = null;
            try {
                if (!rawContactOperations.isEmpty()) {
                    results = resolver.applyBatch(ContactsContract.AUTHORITY, rawContactOperations);
                }
            } catch (RemoteException e) {
                    // Something went wrong, bail without success
                    Log.e(TAG, "Problem persisting user edits for raw contact ID: ", e);
            } catch (OperationApplicationException e) {
                    // The assert could have failed because the raw_contact is already in the group,
                    // just continue to the next contact
                    Log.w(TAG, "Assert failed in adding raw contact ID: ", e);
            }
        }

    }

    /*
     * Create group  in separate operation batch
     */
    private void createGroupInMultiBatch(ContentResolver resolver,
            ArrayList<LocalGroupAccountMemberData> groupDataArray) {

        if ((null == groupDataArray) || (groupDataArray.isEmpty())) {
            return;
        }

        for (LocalGroupAccountMemberData groupData: groupDataArray) {
            String label = groupData.getGroupTitle();
            String accountType = groupData.getAccountType();
            String accountName = groupData.getAccountName();
            String dataSet = groupData.getDataSet();
            boolean isGroupMembershipEditable = groupData.isGroupMembershipEditable();

            ContentValues values = new ContentValues();
            values.put(Groups.ACCOUNT_TYPE, accountType);
            values.put(Groups.ACCOUNT_NAME, accountName);
            values.put(Groups.DATA_SET, dataSet);
            values.put(Groups.TITLE, label);
            values.put(Groups.GROUP_VISIBLE, 1);

            // Create the new group
            final Uri groupUri = resolver.insert(Groups.CONTENT_URI, values);

            // If there's no URI, then the insertion failed. Abort early because group members can't be
            // added if the group doesn't exist
            if (groupUri == null) {
                Log.e(TAG, "Couldn't create group with label " + label);
                return;
            }

            long groupId = ContentUris.parseId(groupUri);
            ArrayList<Long> rawContactIdList = groupData.getRawContactIdList();
            final long[] rawContactsToAdd = LocalGroupUtils.convertToLongArray(rawContactIdList);

            if (DEBUG) {
                Log.d(TAG, "createGroupInMultiBatch(), groupId = " + groupId + ", rawId size = " + rawContactsToAdd.length);
            }

            if (rawContactsToAdd.length > 0) {
                insertMembersToLocalGroup(resolver, rawContactsToAdd, groupId, isGroupMembershipEditable);
            }
        }
    }

    private void createLocalGroup(Intent intent) {
        ArrayList<LocalGroupAccountMemberData> groupDataArray = intent.getParcelableArrayListExtra(LocalGroupSaveService.EXTRA_LOCAL_GROUP_ACCOUNT_MEMBER_DATA);
        ContentValues values = new ContentValues();

        final ContentResolver resolver = getContentResolver();

        // We need to judge how many provider operation will be executed.
        // if total Opt <= MAX_BATCH_SIZE, apply all in one batch
        // if total Opt > MAX_BATCH_SIZE, apply separate.
        int optBudget = getOpCountForNewGroup(groupDataArray);

        if (optBudget <= CREATE_GROUP_MAX_BATCH_SIZE) {
            createGroupInOneBatch(resolver, groupDataArray);
        } else {
            createGroupInMultiBatch(resolver, groupDataArray);
        }

        Intent callbackIntent = intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
        String groupTitle = groupDataArray.get(0).getGroupTitle(); // select the first one is ok
        if (!TextUtils.isEmpty(groupTitle)) {
            callbackIntent.putExtra(LocalGroupUtils.EXTRA_LOCAL_GROUP_TITLE, groupTitle);
        }
        deliverCallback(callbackIntent);
    }


    public static Intent createLocalGroupDeletionIntent(Context context, String groupTitle) {
        Intent serviceIntent = new Intent(context, LocalGroupSaveService.class);
        serviceIntent.setAction(LocalGroupSaveService.ACTION_DELETE_LOCAL_GROUP);
        serviceIntent.putExtra(LocalGroupSaveService.EXTRA_LOCAL_GROUP_LABEL, groupTitle);
        return serviceIntent;
    }

    private interface DeleteLocalGroupQuery {
        String[] PROJECTION = {
                Groups.ACCOUNT_NAME,
                Groups.ACCOUNT_TYPE,
                Groups.DATA_SET,
                Groups._ID,
        };

        String SELECTION = Groups.ACCOUNT_TYPE + " NOT NULL AND " +
                           Groups.ACCOUNT_NAME + " NOT NULL AND " +
                           Groups.AUTO_ADD + "=0 AND " +
                           Groups.FAVORITES + "=0 AND " +
                           Groups.DELETED + "=0 AND " + Groups.TITLE + "=?";

        int ACCOUNT_NAME = 0;
        int ACCOUNT_TYPE = 1;
        int DATA_SET = 2;
        int GROUP_ID = 3;
    }

    private void deleteLocalGroup(Intent intent) {

        final ArrayList<ContentProviderOperation> deleteGroupOperations = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder deleteBuilder = null;
        final ContentResolver resolver = getContentResolver();
        int total_opt_count = 0;

        String groupTitle = intent.getStringExtra(LocalGroupSaveService.EXTRA_LOCAL_GROUP_LABEL);

        if (TextUtils.isEmpty(groupTitle)) {
            Log.e(TAG, "Group title is empty or null, Invalid arguments for deleteLocalGroup request");
            return;
        }

        // Query all aggregation group
        Cursor c = resolver.query(Groups.CONTENT_SUMMARY_URI,
                DeleteLocalGroupQuery.PROJECTION,
                DeleteLocalGroupQuery.SELECTION,
                new String[]{groupTitle}, null);

        try {
           if (null == c || c.getCount() <=0) {
               Log.e(TAG, "No valid group could be deleted.");
           } else {
               c.moveToPosition(-1);
               while (c.moveToNext()) {
                   String accountType = c.getString(DeleteLocalGroupQuery.ACCOUNT_TYPE);
                   String accountName = c.getString(DeleteLocalGroupQuery.ACCOUNT_NAME);
                   String dataSet = c.getString(DeleteLocalGroupQuery.DATA_SET);
                   long groupId = c.getInt(DeleteLocalGroupQuery.GROUP_ID);

                   if (DEBUG) {
                       Log.d(TAG, "accountType: " + accountType + ", accountName: " + accountName
                               + ", groupId: " + groupId);
                   }

                   Context context = getApplicationContext();

                   Uri deleteGroupUri = ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);
                   // We delete moto agg group entirely.
                   // Google or Exchange account those support group sync need sync adapter to do the delete.
                   if (!LocalGroupUtils.isLocalGroupMembershipEditable(context, accountType, dataSet)) {
                       Log.d(TAG, "This account membership is not editable.");
                       deleteGroupUri = deleteGroupUri.buildUpon()
                                      .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
                   } else {
                       Log.d(TAG, "This account membership is editable.");
                   }
                   deleteBuilder = ContentProviderOperation.newDelete(deleteGroupUri);
                   deleteGroupOperations.add(deleteBuilder.build());
                   total_opt_count++;
                   if (total_opt_count >= DELETE_GROUP_MAX_BATCH_SIZE) {
                       if (DEBUG) {
                           Log.d(TAG, "deleteLocalGroup(), applyBatch, opt_count = " + total_opt_count);
                       }

                       if (DEBUG) {
                           for (ContentProviderOperation operation : deleteGroupOperations) {
                               Log.v(TAG, operation.toString());
                           }
                       }

                       ContentProviderResult[] results = null;
                       try {
                           if (!deleteGroupOperations.isEmpty()) {
                               results = resolver.applyBatch(ContactsContract.AUTHORITY, deleteGroupOperations);
                           }
                       } catch (RemoteException e) {
                               Log.e(TAG, "Problem persisting delete group: ", e);
                       } catch (OperationApplicationException e) {
                               Log.w(TAG, "Assert failed in delete group: ", e);
                       } finally {
                           deleteGroupOperations.clear();
                           total_opt_count = 0;
                       }
                   }

               }
           }
        } finally {
            if (null != c) {
                c.close();
                c = null;
            }
        }

        if (!deleteGroupOperations.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "deleteLocalGroup(), applyBatch left, opt_count = " + total_opt_count);
            }

            if (DEBUG) {
                for (ContentProviderOperation operation : deleteGroupOperations) {
                    Log.v(TAG, operation.toString());
                }
            }

            ContentProviderResult[] results = null;
            try {
                if (!deleteGroupOperations.isEmpty()) {
                    results = resolver.applyBatch(ContactsContract.AUTHORITY, deleteGroupOperations);
                }
            } catch (RemoteException e) {
                    Log.e(TAG, "Problem persisting delete group: ", e);
            } catch (OperationApplicationException e) {
                    Log.w(TAG, "Assert failed in delete group: ", e);
            }
        }

        // MOTO CHINA: Request Local sync
        Account account = new Account(HardCodedSources.ACCOUNT_LOCAL_DEVICE, HardCodedSources.ACCOUNT_TYPE_LOCAL);
        Log.v(TAG, "deleteGroup(), account = "+account);
        SimUtility.requestSync(account);

    }

    public static Intent createLocalGroupUpdateIntent(Context context, String originalLabel,  String newLabel,
            ArrayList<LocalGroupAccountMemberData> groupDataArray,
            long[]contactsToRemove, Class<?> callbackActivity, String callbackAction) {

        Intent serviceIntent = new Intent(context, LocalGroupSaveService.class);
        serviceIntent.setAction(LocalGroupSaveService.ACTION_UPDATE_LOCAL_GROUP);
        serviceIntent.putExtra(LocalGroupSaveService.EXTRA_GROUP_ORIGINAL_LABEL, originalLabel);
        serviceIntent.putExtra(LocalGroupSaveService.EXTRA_GROUP_LABEL, newLabel);
        serviceIntent.putParcelableArrayListExtra(LocalGroupSaveService.EXTRA_LOCAL_GROUP_ACCOUNT_MEMBER_DATA, groupDataArray);
        serviceIntent.putExtra(LocalGroupSaveService.EXTRA_CONTACTS_TO_REMOVE, contactsToRemove);

        // Callback intent will be invoked by the service once the group is updated
        Intent callbackIntent = new Intent(context, callbackActivity);
        callbackIntent.setAction(callbackAction);
        serviceIntent.putExtra(LocalGroupSaveService.EXTRA_CALLBACK_INTENT, callbackIntent);

        return serviceIntent;
    }

    private void updateLocalGroup(Intent intent) {
        ArrayList<LocalGroupAccountMemberData> groupDataArray =
            intent.getParcelableArrayListExtra(LocalGroupSaveService.EXTRA_LOCAL_GROUP_ACCOUNT_MEMBER_DATA);

        String label = intent.getStringExtra(EXTRA_GROUP_ORIGINAL_LABEL);
        String newLabel = intent.getStringExtra(EXTRA_GROUP_LABEL);
        long[] contactsIdToRemove = intent.getLongArrayExtra(EXTRA_CONTACTS_TO_REMOVE);

        final ContentResolver resolver = getContentResolver();
        int total_opt_count = 0;

        if (DEBUG) {
            Log.d(TAG, "original name: " + label + ", new name: " + newLabel);
        }

        // rename group if necessary
        if (newLabel != null) {
            if (DEBUG) {
                Log.d(TAG, "new group name is not null, will change group name.");
            }
            renameLocalGroup(resolver, label, newLabel);
            label = newLabel;
        }

        // add new members to group if necessary
        if (groupDataArray != null && !groupDataArray.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "there are new members to add.");
            }

            addMembersToLocalGroup(resolver, groupDataArray, label);
        }

        // remove members from group if necessary
        if (contactsIdToRemove.length > 0) {
            if (DEBUG) {
                Log.d(TAG, "there are members to be removed.");
                for (long id: contactsIdToRemove) {
                    Log.d(TAG, "contact_id = " + id);
                }
            }

            removeMembersFromLocalGroup(resolver, contactsIdToRemove, label);
        }

        Intent callbackIntent = intent.getParcelableExtra(EXTRA_CALLBACK_INTENT);
        callbackIntent.putExtra(LocalGroupUtils.EXTRA_LOCAL_GROUP_TITLE, label);
        deliverCallback(callbackIntent);
    }


    private interface LocalGroupQuery {
        String[] PROJECTION = {
                Groups._ID,
                Groups.TITLE,
        };

        String SELECTION = Groups.ACCOUNT_TYPE + " NOT NULL AND " +
                           Groups.ACCOUNT_NAME + " NOT NULL AND " +
                           Groups.AUTO_ADD + "=0 AND " +
                           Groups.FAVORITES + "=0 AND " +
                           Groups.DELETED + "=0 AND " + Groups.TITLE + "=?";

        int GROUP_ID = 0;
        int TITLE = 1;
    }

    private void renameLocalGroup(ContentResolver resolver, String originalName, String newName) {

        final ArrayList<ContentProviderOperation> renameOperations =
            new ArrayList<ContentProviderOperation>();
        int total_opt_count = 0;

        if (TextUtils.isEmpty(originalName) || TextUtils.isEmpty(newName)) {
            return;
        }

        Cursor c = resolver.query(Groups.CONTENT_SUMMARY_URI,
                LocalGroupQuery.PROJECTION,
                LocalGroupQuery.SELECTION,
                new String[]{originalName}, null);

        try {
            if (null == c || c.getCount() <=0) {
                Log.e(TAG, "No valid group for rename.");
            } else {
                c.moveToPosition(-1);
                while (c.moveToNext()) {
                    long groupId = c.getLong(LocalGroupQuery.GROUP_ID);
                    final ContentProviderOperation.Builder updateBuilder = ContentProviderOperation
                    .newUpdate(ContentUris.withAppendedId(Groups.CONTENT_URI, groupId));
                    updateBuilder.withValue(Groups.TITLE, newName);
                    renameOperations.add(updateBuilder.build());
                    total_opt_count++;

                    if (total_opt_count >= RENAME_GROUP_MAX_BATCH_SIZE) {
                        if (DEBUG) {
                            Log.d(TAG, "renameLocalGroup(), applyBatch, opt_count = " + total_opt_count);
                        }

                        if (DEBUG) {
                            for (ContentProviderOperation operation : renameOperations) {
                                Log.v(TAG, operation.toString());
                            }
                        }

                        ContentProviderResult[] results = null;
                        try {
                            if (!renameOperations.isEmpty()) {
                                results = resolver.applyBatch(ContactsContract.AUTHORITY, renameOperations);
                            }
                        } catch (RemoteException e) {
                            // Something went wrong, bail without success
                            Log.e(TAG, "Problem persisting update group name: " + e);
                            throw new RuntimeException("Failed to update group name: ", e);
                        } catch (OperationApplicationException e) {
                            Log.w(TAG, "Assert failed in update group name: " + e);
                            throw new RuntimeException("Failed to update group name: ", e);
                        } finally {
                            renameOperations.clear();
                            total_opt_count = 0;
                        }
                    }
                }
            }
        } finally {
            if (null != c) {
                c.close();
                c = null;
            }
        }


        if (!renameOperations.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "renameLocalGroup(), applyBatch left, opt_count = " + total_opt_count);
            }

            if (DEBUG) {
                for (ContentProviderOperation operation: renameOperations) {
                    Log.v(TAG, operation.toString());
                }
            }

            ContentProviderResult[] results = null;
            try {
                if (!renameOperations.isEmpty()) {
                    results = resolver.applyBatch(ContactsContract.AUTHORITY, renameOperations);
                }
            } catch (RemoteException e) {
                // Something went wrong, bail without success
                Log.e(TAG, "Problem persisting update group name: " + e);
                throw new RuntimeException("Failed to update group name: ", e);
            } catch (OperationApplicationException e) {
                Log.w(TAG, "Assert failed in update group name: " + e);
                throw new RuntimeException("Failed to update group name: ", e);
            }
        }

    }

    private interface LocalGroupAccountQuery {
        String[] PROJECTION = {
                Groups.ACCOUNT_NAME,
                Groups.ACCOUNT_TYPE,
                Groups.DATA_SET,
                Groups._ID,
                Groups.TITLE,
        };

        String SELECTION = Groups.TITLE + "=? AND " +
                           Groups.ACCOUNT_TYPE + " NOT NULL AND " +
                           Groups.ACCOUNT_TYPE + "=? AND " +
                           Groups.ACCOUNT_NAME + " NOT NULL AND " +
                           Groups.ACCOUNT_NAME + "=? AND " +
                           Groups.AUTO_ADD + "=0 AND " +
                           Groups.FAVORITES + "=0 AND " +
                           Groups.DELETED + "=0" ;

        int ACCOUNT_NAME = 0;
        int ACCOUNT_TYPE = 1;
        int DATA_SET = 2;
        int GROUP_ID = 3;
        int TITLE = 4;
    }

    private void addMembersToLocalGroup(ContentResolver resolver,
            ArrayList<LocalGroupAccountMemberData> groupDataArray, String groupTitle) {

        if ( (null == groupDataArray) || groupDataArray.isEmpty()
                || TextUtils.isEmpty(groupTitle) ) {
            Log.w(TAG, "No valid groupDataArray or groupTitle is empty ! groupTitle = "
                    + groupTitle);
            return;
        }

        // Add members to Edit Group
        for (LocalGroupAccountMemberData groupData: groupDataArray) {
            String label = groupData.getGroupTitle();
            String accountType = groupData.getAccountType();
            String accountName = groupData.getAccountName();
            String dataSet = groupData.getDataSet();
            boolean isGroupMembershipEditable = groupData.isGroupMembershipEditable();

            if (DEBUG) {
                Log.d(TAG, "In edit group, new added member account info: label=" + label
                        + ", accountType=" + accountType
                        + ", accountName=" + accountName
                        + ", dataSet=" + dataSet
                        + ", memberShipEditable=" + isGroupMembershipEditable);
            }

            ArrayList<Long> rawContactIdList = groupData.getRawContactIdList();
            final long[] rawContactsToAdd = LocalGroupUtils.convertToLongArray(rawContactIdList);
            String accountClause;
            long groupId;
            String[] args;

            // Check whether the group account already exists.
            if (dataSet == null) {
                accountClause = LocalGroupAccountQuery.SELECTION + " AND " + Groups.DATA_SET + " IS NULL";
                args = new String[] {groupTitle, accountType, accountName};
            } else {
                accountClause = LocalGroupAccountQuery.SELECTION + " AND " + Groups.DATA_SET + "=?";
                args = new String[] {groupTitle, accountType, accountName, dataSet};
            }

            Cursor c = resolver.query(Groups.CONTENT_SUMMARY_URI,
                    LocalGroupAccountQuery.PROJECTION,
                    accountClause,
                    args, null);

            try {
                if ( (c != null) && (c.moveToFirst()) ) {
                    // select the first existing group item to insert.
                    groupId = c.getLong(LocalGroupAccountQuery.GROUP_ID);
                    if (DEBUG) {
                        Log.d(TAG, "In edit group, the new add contacts will be insert to existing group, the groupId = " + groupId);
                    }
                } else {
                    // group account not exists
                    if (DEBUG) {
                        Log.d(TAG, "Group with this account not exists. Will create new group and insert the contacts.");
                    }

                    ContentValues values = new ContentValues();
                    values.put(Groups.ACCOUNT_TYPE, accountType);
                    values.put(Groups.ACCOUNT_NAME, accountName);
                    values.put(Groups.DATA_SET, dataSet);
                    values.put(Groups.TITLE, label);
                    values.put(Groups.GROUP_VISIBLE, 1);

                    // Create the new group
                    final Uri groupUri = resolver.insert(Groups.CONTENT_URI, values);

                    // If there's no URI, then the insertion failed. Abort early because group members can't be
                    // added if the group doesn't exist
                    if (groupUri == null) {
                        Log.e(TAG, "Couldn't create group with label " + label);
                        return;
                    }

                    groupId = ContentUris.parseId(groupUri);
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }

            // Add new group members if there exist
            if (rawContactsToAdd.length > 0) {
                insertMembersToLocalGroup(resolver, rawContactsToAdd, groupId, isGroupMembershipEditable);
            }

        }
    }

    private interface RemoveMemberQuery {
        // MOT MOD BEGIN - IKHSS6-1470: group redesign 2nd release, to add new mimetype for localgroup
        String[] PROJECTION = {
                Groups.TITLE,
                Groups._ID,
                Data.RAW_CONTACT_ID,
                RawContacts.CONTACT_ID,
                Groups.ACCOUNT_NAME,
                Groups.ACCOUNT_TYPE,
                Groups.DATA_SET
        };

        String SELECTION = RawContacts.CONTACT_ID + " IN (%s) AND " + Groups.TITLE + "=?";

        int GROUP_TITLE_INDEX = 0;
        int GROUP_ROW_ID_INDEX = 1;
        int RAW_CONTACT_ID_INDEX = 2;
        int CONTACT_ID_INDEX = 3;
        int GROUP_ACCOUNT_NAME_INDEX = 4;
        int GROUP_ACCOUNT_TYPE_INDEX = 5;
        int GROUP_DATA_SET_INDEX = 6;
        // MOT MOD END - IKHSS6-1470
    }

    private void removeMembersFromLocalGroup(ContentResolver resolver,
            long[] contactIdArray, String groupTitle) {

        if ( (null == contactIdArray) || (contactIdArray.length == 0)
                || TextUtils.isEmpty(groupTitle) ) {
            Log.w(TAG, "No valid contactId array or groupTitle is empty: contactIdArray="
                    + contactIdArray + ", groupTitle=" + groupTitle);
            return;
        }

        List<String> memberList = new ArrayList<String>();
        ContentProviderOperation.Builder deleteBuilder;
        final ArrayList<ContentProviderOperation> removeMemberOperations =
            new ArrayList<ContentProviderOperation>();
        int total_opt_count = 0;

        for (long id: contactIdArray) {
            memberList.add(String.valueOf(id));
        }

        String idArgs = TextUtils.join(",", memberList);

        // MOT MOD BEGIN - IKHSS6-1470: group redesign 2nd release, to add new mimetype for localgroup
        final Uri LOCAL_GROUP_RAWCONTACT_GROUPS_DETAIL_URI = Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "agg_groups_rawcontact_groups_detail");
        Cursor c = resolver.query(LOCAL_GROUP_RAWCONTACT_GROUPS_DETAIL_URI,
                RemoveMemberQuery.PROJECTION,
                String.format(RemoveMemberQuery.SELECTION, idArgs),
                new String[]{groupTitle}, null);
        // MOT MOD END - IKHSS6-1470

        try {
            if (null == c || c.getCount() <=0) {
                Log.e(TAG, "No valid member data could be removed.");
            } else {
                c.moveToPosition(-1);

                while (c.moveToNext()) {
                    boolean permitDelete = false;
                    long rawContactId = c.getLong(RemoveMemberQuery.RAW_CONTACT_ID_INDEX);
                    long groupId = c.getLong(RemoveMemberQuery.GROUP_ROW_ID_INDEX);

                    String title = c.getString(RemoveMemberQuery.GROUP_TITLE_INDEX);
                    String accountName = c.getString(RemoveMemberQuery.GROUP_ACCOUNT_NAME_INDEX);
                    String accountType = c.getString(RemoveMemberQuery.GROUP_ACCOUNT_TYPE_INDEX);
                    String dataSet = c.getString(RemoveMemberQuery.GROUP_DATA_SET_INDEX);

                    if (DEBUG) {
                        Log.d(TAG, "title: " + title + ", groupId: " + groupId + ", rawContactId: " + rawContactId
                                + ", accountName: " + accountName + ", accountType: " + accountType + ", dataSet: " + dataSet);
                    }

                    if (TextUtils.equals(accountType, GroupAPI.LOCAL_GROUP_ACCOUNT_TYPE)
                             && TextUtils.equals(accountName, GroupAPI.LOCAL_GROUP_ACCOUNT_NAME)) {
                        if (DEBUG) {
                            Log.d(TAG, " This is local group, permit to delete.");
                        }
                        permitDelete = true;
                    } else {
                        if (LocalGroupUtils.isLocalGroupMembershipEditable(getApplicationContext(), accountType, dataSet)) {
                            if (DEBUG) {
                                Log.d(TAG, " This is membership editable group, permit to delete.");
                            }
                            permitDelete = true;
                        } else {
                            if (DEBUG) {
                                Log.d(TAG, " This is membership not editable group, can't be deleted!");
                            }
                            permitDelete = false;
                        }
                    }

                    if (permitDelete) {
                        deleteBuilder = ContentProviderOperation
                        .newDelete(Data.CONTENT_URI)
                        .withSelection(Data.RAW_CONTACT_ID + "=? AND " +
                                "("+ Data.MIMETYPE + "=?" + " OR " + Data.MIMETYPE + "=?)" + " AND " + GroupMembership.GROUP_ROW_ID + "=?",
                                new String[] { String.valueOf(rawContactId),
                                GroupMembership.CONTENT_ITEM_TYPE,
                                "vnd.android.cursor.item/local_group_membership",
                                String.valueOf(groupId)} );
                        removeMemberOperations.add(deleteBuilder.build());
                        total_opt_count++;
                    }

                    if (total_opt_count >= REMOVE_MEMBER_MAX_BATCH_SIZE) {
                        if (DEBUG) {
                            Log.d(TAG, "removeMembersFromLocalGroup(), applyBatch, opt_count = " + total_opt_count);
                        }

                        if (DEBUG) {
                            for (ContentProviderOperation operation : removeMemberOperations) {
                                Log.v(TAG, operation.toString());
                            }
                        }

                        ContentProviderResult[] results = null;
                        try {
                            if (!removeMemberOperations.isEmpty()) {
                                results = resolver.applyBatch(ContactsContract.AUTHORITY, removeMemberOperations);
                            }
                        } catch (RemoteException e) {
                            // Something went wrong, bail without success
                            Log.e(TAG, "Problem persisting remove group members: " + e);
                            throw new RuntimeException("Failed to remove group members: ", e);
                        } catch (OperationApplicationException e) {
                            Log.w(TAG, "Assert failed in remove group members: " + e);
                            throw new RuntimeException("Failed to remove group members: ", e);
                        } finally {
                            removeMemberOperations.clear();
                            total_opt_count = 0;
                        }
                    }
                }
            }
        } finally {
            if (null != c) {
                c.close();
                c = null;
            }
        }


        if (!removeMemberOperations.isEmpty()) {
            if (DEBUG) {
                Log.d(TAG, "removeMembersFromLocalGroup(), applyBatch left, opt_count = " + total_opt_count);
            }

            if (DEBUG) {
                for (ContentProviderOperation operation : removeMemberOperations) {
                    Log.v(TAG, operation.toString());
                }
            }

            ContentProviderResult[] results = null;
            try {
                if (!removeMemberOperations.isEmpty()) {
                    results = resolver.applyBatch(ContactsContract.AUTHORITY, removeMemberOperations);
                }
            } catch (RemoteException e) {
                // Something went wrong, bail without success
                Log.e(TAG, "Problem persisting remove group members: " + e);
                throw new RuntimeException("Failed to remove group members: ", e);
            } catch (OperationApplicationException e) {
                Log.w(TAG, "Assert failed in remove group members: " + e);
                throw new RuntimeException("Failed to remove group members: ", e);
            }
        }

    }

}
