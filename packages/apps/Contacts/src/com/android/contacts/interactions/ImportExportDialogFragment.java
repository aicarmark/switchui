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

package com.android.contacts.interactions;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.interactions.ExportByAccountDialogFragment;
import com.android.contacts.util.AccountSelectionUtil;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
import com.android.contacts.vcard.ExportVCardActivity;
import com.motorola.contacts.list.ContactMultiplePickerResultContentProvider.Resultable;
//<!-- MOTOROLA MOD Start: IKPIM-491 -->
import com.motorola.contacts.preference.ContactPreferenceUtilities;
//<!-- MOTOROLA MOD End of IKPIM-491 -->
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
//<!-- MOTOROLA MOD Start: IKPIM-493 James Shen - A22398 -->
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager.NameNotFoundException;
//<!-- MOTOROLA MOD End: IKPIM-493 James Shen - A22398 -->


/**
 * An dialog invoked to import/export contacts.
 */
public class ImportExportDialogFragment extends DialogFragment
        implements SelectAccountDialogFragment.Listener {
    public static final String TAG = "ImportExportDialogFragment";

    private static final String KEY_RES_ID = "resourceId";
    private static final String KEY_DEFAULT_SELECT_MODE = "Default_select_mode";
    
    //export by account
    private static final int SUBACTIVITY_DISPLAY_FILE_CHOICE = 1;
    private static final int SUBACTIVITY_DISPLAY_FOLDER_CHOICE = 2;
    private static final int EXPORT_SPECIFIC_CONTACTS = 3;
    private Activity mActivity;
    private Intent mCurData = null;
    private int mCurChoice;
    // BEGIN Motorola, ODC_001639, 2013-01-23, SWITCHUITWO-578
    //private DialogInterface mDialog;
    //END SWITCHUITWO-578
    private final String[] LOOKUP_PROJECTION = new String[] {
            Contacts.LOOKUP_KEY
    };

    /** Preferred way to show this dialog */
    public static void show(FragmentManager fragmentManager) {
        final ImportExportDialogFragment fragment = new ImportExportDialogFragment();
        fragment.show(fragmentManager, ImportExportDialogFragment.TAG);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mActivity = getActivity();
        // Wrap our context to inflate list items using the correct theme
        final Resources res = getActivity().getResources();
        final LayoutInflater dialogInflater = (LayoutInflater)getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Adapter that shows a list of string resources
        final ArrayAdapter<Integer> adapter = new ArrayAdapter<Integer>(getActivity(),
                R.layout.select_dialog_item) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final TextView result = (TextView)(convertView != null ? convertView :
                        dialogInflater.inflate(R.layout.select_dialog_item, parent, false));

                final int resId = getItem(position);
                result.setText(resId);
                return result;
            }
        };

//<!-- MOTOROLA MOD Start: IKPIM-493 James Shen - A22398 -->
        boolean hasSimManager = false;
        final ComponentName simManagerComponent = new ComponentName("com.motorola.simmanager",
             "com.motorola.simmanager.SimManagerActivity");
        try {
             ActivityInfo simActivityInfo = getActivity().getPackageManager().getActivityInfo(simManagerComponent, 0);
             if (simActivityInfo != null) {
                 hasSimManager = true;
             } else {
                 hasSimManager = false;
             }
         } catch (NameNotFoundException e) {
            Log.w (TAG, "Sim Manager Activity not found");
            hasSimManager = false;
         }

        // MOTOROLA CHINA BACKUP/RESTORE
        if (res.getBoolean(R.bool.config_allow_export_to_sdcard)) {
            adapter.add(R.string.export_to_sdcard_moto);
        }
        if (res.getBoolean(R.bool.config_allow_import_from_sdcard)) {
            adapter.add(R.string.import_from_sdcard_moto);
            adapter.add(R.string.import_from_non_default);
            adapter.add(R.string.export_specific_contacts);
        }
        if (res.getBoolean(R.bool.config_allow_export_to_sdcard)) {
            adapter.add(R.string.export_by_account);
        }

//<!-- MOTOROLA MOD End: IKPIM-493 James Shen - A22398 -->
        /*
        if (TelephonyManager.getDefault().hasIccCard()
                && res.getBoolean(R.bool.config_allow_sim_import)) {
            adapter.add(R.string.import_from_sim);
        }
        */
        /* Removed for China products
        if (res.getBoolean(R.bool.config_allow_import_from_sdcard)) {
            adapter.add(R.string.import_from_sdcard);
        }
//<!-- MOTOROLA MOD Start: IKPIM-493 James Shen - A22398 -->
        if (hasSimManager && TelephonyManager.getDefault().hasIccCard()) {
            adapter.add(R.string.import_from_sim);
        }
//<!-- MOTOROLA MOD End: IKPIM-493 James Shen - A22398 -->
        if (res.getBoolean(R.bool.config_allow_export_to_sdcard)) {
            adapter.add(R.string.export_to_sdcard);
        }
//<!-- MOTOROLA MOD Start: IKPIM-493 James Shen - A22398 -->
        if (hasSimManager && TelephonyManager.getDefault().hasIccCard()) {
            adapter.add(R.string.export_to_sim);
        }
        if (hasSimManager && TelephonyManager.getDefault().hasIccCard()) {
            adapter.add(R.string.delete_from_sim);
        }
//<!-- MOTOROLA MOD End: IKPIM-493 James Shen - A22398 -->
        if (res.getBoolean(R.bool.config_allow_share_visible_contacts)) {
            adapter.add(R.string.share_visible_contacts);
        }
        */

        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean dismissDialog;
                final int resId = adapter.getItem(which);
                switch (resId) {
//<!-- MOTOROLA MOD Start: IKPIM-493 James Shen - A22398 -->
                    case R.string.import_from_sim: {
                        //MOTO MOD BEGIN
                        dismissDialog = handleImportRequest(resId);
                        //MOTO MOD END
                        break;
                    }
                    case R.string.export_to_sim: {
                        dismissDialog = true;
                        Intent simExportIntent = new Intent("com.motorola.android.simmanager.ACTION_EXPORT");
                        startActivity(simExportIntent);
                        break;
                    }
                    case R.string.delete_from_sim: {
                        dismissDialog = true;
                        Intent simDeleteIntent = new Intent("com.motorola.android.simmanager.ACTION_DELETE");
                        startActivity(simDeleteIntent);
                        break;
                    }
//<!-- MOTOROLA MOD End: IKPIM-493 James Shen - A22398 -->

                    case R.string.import_from_sdcard: {
                        dismissDialog = handleImportRequest(resId);
                        break;
                    }
                    case R.string.export_to_sdcard: {
                        dismissDialog = true;
                        Intent exportIntent = new Intent(getActivity(), ExportVCardActivity.class);
                        getActivity().startActivity(exportIntent);
                        break;
                    }
                    case R.string.export_specific_contacts: {
                    	dismissDialog = false;
                    	Intent pickIntent = new Intent("motorola.intent.ACTION_MULTIPLE_PICK", Contacts.CONTENT_URI);
                    	pickIntent.setType("vnd.android.cursor.dir/contact");
                    	startActivityForResult(pickIntent, EXPORT_SPECIFIC_CONTACTS);
                    	break;
                    }
                    case R.string.share_visible_contacts: {
                        dismissDialog = true;
                        doShareVisibleContacts();
                        break;
                    }
                    case R.string.export_to_sdcard_moto: {
                        dismissDialog = true;
                        handleSDCardImExport(false);
                        break;
                    }
                    case R.string.import_from_sdcard_moto: {
                        dismissDialog = true;
                        handleSDCardImExport(true);
                        break;
                    }
                    case R.string.import_from_non_default: {
                        dismissDialog = true;
                        /**modify for IKCBSMMCPPRC-1760 by bphx43 12-08-27*/
                        try {
							Intent intent = new Intent("com.motorola.action.import3rd");
							intent.putExtra("contacts", true);
							/**modify for SWITCHUITWO-92 by bphx43 12-11-21*/
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
							/**end*/
							startActivity(intent);
						} catch (ActivityNotFoundException e) {
							// TODO: handle exception
							Log.e(TAG, "Not found Activity  "+e.getMessage());
						}
                        /**end by bphx43 12-08-27*/
						// displayNonDefaultDialog(); // Used for EMMC+SD
                        break;
                    }
                    case R.string.export_by_account: {
                        dismissDialog = true;
                        ExportByAccountDialogFragment.show(getFragmentManager());
                        break;
                    }
                    default: {
                        dismissDialog = true;
                        Log.e(TAG, "Unexpected resource: "
                                + getActivity().getResources().getResourceEntryName(resId));
                    }
                }
                if (dismissDialog) {
                    dialog.dismiss();
                } 
                // BEGIN Motorola, ODC_001639, 2013-01-23, SWITCHUITWO-578
//                else {
//                    mDialog = dialog;
//                }
                //END SWITCHUITWO-578
            }
        };
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_import_export)
                .setSingleChoiceItems(adapter, -1, clickListener)
                .create();
    }

    private void doShareVisibleContacts() {
        // TODO move the query into a loader and do this in a background thread
        //MOTO MOD start: IKHSS7-21759: FB Contacts can be shared via "Import/export" > "Share visible contacts" > "Messages"
        final Uri contentUriForContacts= Contacts.CONTENT_URI.buildUpon()
            .appendQueryParameter("for_export_only", "1")
            .build();
        final Cursor cursor = getActivity().getContentResolver().query(contentUriForContacts,
        //MOTO MOD end of IKHSS7-21759
                LOOKUP_PROJECTION, Contacts.IN_VISIBLE_GROUP + "!=0", null, null);
        if (cursor != null) {
            try {
                if (!cursor.moveToFirst()) {
                    Toast.makeText(getActivity(), R.string.share_error, Toast.LENGTH_SHORT).show();
                    return;
                }

                StringBuilder uriListBuilder = new StringBuilder();
                int index = 0;
                do {
                    if (index != 0)
                        uriListBuilder.append(':');
                    uriListBuilder.append(cursor.getString(0));
                    index++;
                } while (cursor.moveToNext());
                Uri uri = Uri.withAppendedPath(
                        Contacts.CONTENT_MULTI_VCARD_URI,
                        Uri.encode(uriListBuilder.toString()));

                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType(Contacts.CONTENT_VCARD_TYPE);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                getActivity().startActivity(intent);
            } finally {
                cursor.close();
            }
        }
    }

    /**
     * Handle "import from SIM" and "import from SD".
     *
     * @return {@code true} if the dialog show be closed.  {@code false} otherwise.
     */
    private boolean handleImportRequest(int resId) {
        // There are three possibilities:
        // - more than one accounts -> ask the user
        // - just one account -> use the account without asking the user
        // - no account -> use phone-local storage without asking the user
        final AccountTypeManager accountTypes = AccountTypeManager.getInstance(getActivity());
        final List<AccountWithDataSet> accountList = accountTypes.getAccounts(true);
        final int size = accountList.size();

//<!-- MOTOROLA MOD Start: IKPIM-491 -->
        if (ContactPreferenceUtilities.IfUserChoiceRemembered(getActivity().getBaseContext()) && ContactPreferenceUtilities.GetUserPreferredAccountIndex(getActivity().getBaseContext(), true)!=-1){
            AccountSelectionUtil.doImport(getActivity().getBaseContext(), resId, ContactPreferenceUtilities.GetUserPreferredAccountFromSharedPreferences(getActivity().getBaseContext()));
        }else if (size==1 || size==0) { //MOT MOD IKHSS7-730, if size = 0, import contacts to local
            AccountSelectionUtil.doImport(getActivity().getBaseContext(), resId, (size == 1 ? accountList.get(0) : null));
        }else {
            // Send over to the account selector
            final Bundle args = new Bundle();
            args.putInt(KEY_RES_ID, resId);
            SelectAccountDialogFragment.show(
                    getFragmentManager(), this,
                    R.string.dialog_new_contact_account,
                    AccountListFilter.ACCOUNTS_CONTACT_WRITABLE, args);

            // In this case, because this DialogFragment is used as a target fragment to
            // SelectAccountDialogFragment, we can't close it yet.  We close the dialog when
            // we get a callback from it.
            return false;
        }

        //<!-- MOTOROLA MOD End of IKPIM-491 -->
        return true; // Close the dialog.
    }

    private void handleSDCardImExport(boolean ImportOrExport) {
        //Log.v(TAG, "Enter handleSDCardImExport()");
        Intent intent;
        if (ImportOrExport) {
            intent = new Intent("com.motorola.action.sdcardrestore");
        } else {
            intent = new Intent("com.motorola.action.sdcardbackup");
        }
        intent.putExtra("contacts", true);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e(TAG, "Not found Activity ");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.v(TAG, "Enter onActivityResult");
        switch (requestCode) {
            case EXPORT_SPECIFIC_CONTACTS:
            	if (resultCode == Activity.RESULT_OK && data != null) {
            		exportSpecificContacts(data);
            	}
            	
            	// BEGIN Motorola, ODC_001639, 2013-01-23, SWITCHUITWO-578
//            	// BEGIN Motorola, ODC_001639, 2013-01-04, SWITCHUITWO-414
//            	if (mDialog != null) {
//                    mDialog.dismiss();
//                }
//            	// END SWITCHUITWO-414
            	
            	dismiss();
            	// END SWITCHUITWO-578
            	break;
        }
    }
    
    private void exportSpecificContacts(Intent data) {
        if (data != null) {
        	data.setAction("com.motorola.action.sdcardbackup");
        	data.putExtra("contacts", true);
        	data.putExtra("specific_contacts", true);

            try {
                startActivity(data);
            } catch (ActivityNotFoundException ex) {
                Log.e(TAG, "Not found Activity ");
            }
        }
     // BEGIN Motorola, ODC_001639, 2013-01-04, SWITCHUITWO-414
//        if (mDialog != null) {
//            mDialog.dismiss();
//        }
     // END SWITCHUITWO-414
    }

    /**
     * Called when an account is selected on {@link SelectAccountDialogFragment}.
     */
    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        int resId = extraArgs.getInt(KEY_RES_ID);
        switch (resId) {
            case R.string.import_from_sdcard: {
                AccountSelectionUtil.doImport(getActivity(), extraArgs.getInt(KEY_RES_ID), account);
                break;
            }
        }

        // At this point the dialog is still showing (which is why we can use getActivity() above)
        // So close it.
        dismiss();
    }

    @Override
    public void onAccountSelectorCancelled() {
        // See onAccountChosen() -- at this point the dialog is still showing.  Close it.
        dismiss();
    }
    
}
