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

package com.android.contacts.calllog;

import com.android.contacts.activities.CallLogActivity;
import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CallLog.Calls;

import java.util.ArrayList;

/**
 * Dialog that clears the call log after confirming with the user
 */
public class ClearCallLogDialog extends DialogFragment {
    /** Preferred way to show this dialog */
    public static int mCallType = 0;
    public static void show(FragmentManager fragmentManager, int callType) {
        ClearCallLogDialog dialog = new ClearCallLogDialog();
        dialog.show(fragmentManager, "deleteCallLog");
        mCallType = callType;
    }

    public ClearCallLogDialog() {
          super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ContentResolver resolver = getActivity().getContentResolver();
        final OnClickListener okListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final ProgressDialog progressDialog = ProgressDialog.show(getActivity(),
                        getString(R.string.clearCallLogProgress_title),
                        "", true, false);
                final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        if(mCallType == CallLogViewTypeHelper.VIEW_OPTION_ALL) {
                            resolver.delete(Calls.CONTENT_URI, null, null);
                        } else {
                            resolver.delete(Calls.CONTENT_URI, Calls.TYPE +" = " +  callLogType(mCallType), null);
                        }
                        return null;
                    }
                    @Override
                    protected void onPostExecute(Void result) {
                        progressDialog.dismiss();
                        // MOTO Dialer Code IKHSS7-2637 Start
                        CallLogFragment fragment = null;
                        if(getActivity() instanceof DialtactsActivity) {
                           fragment = (CallLogFragment)((DialtactsActivity)getActivity()).getFragmentAt(DialtactsActivity.TAB_INDEX_CALL_LOG);
                        } else if (getActivity() instanceof CallLogActivity) {
                           fragment = (CallLogFragment)getActivity().getFragmentManager().findFragmentById(R.id.call_log_fragment);
                        }
                        if (fragment != null) {
                            if (fragment.getAdapter() != null) {
                              fragment.getAdapter().clearAllCache();
                          }
                        }
                        // MOTO Dialer Code IKHSS7-2637 End
                    }
                };
                // TODO: Once we have the API, we should configure this ProgressDialog
                // to only show up after a certain time (e.g. 150ms)
                progressDialog.show();
                task.execute();
            }
        };
        return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.recentCalls_deleteAll)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setMessage(getMessageStringId(mCallType))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.recentCalls_confirmdelete, okListener)
            .setCancelable(true)
            .create();
    }

    private int callLogType(int viewType) {
        /*
         * types in calllogs db
         * Calls.INCOMING_TYPE = 1;

      * Calls.OUTGOING_TYPE = 2;
         * Calls.MISSED_TYPE = 3;
         * Calls.VOICEMAIL_TYPE = 4;
         */
        int callLogType;
        switch (viewType) {
            case CallLogViewTypeHelper.VIEW_OPTION_MISSED:
                callLogType = Calls.MISSED_TYPE;
                break;
            case CallLogViewTypeHelper.VIEW_OPTION_RECEIVED:
                callLogType = Calls.INCOMING_TYPE;
                break;
            case CallLogViewTypeHelper.VIEW_OPTION_OUTGOING:
                callLogType = Calls.OUTGOING_TYPE;
                break;
            case CallLogViewTypeHelper.VIEW_OPTION_VOICEMAIL:
                callLogType = Calls.VOICEMAIL_TYPE;
                break;
            default:
                return 0;
        }
        return callLogType;
    }

    int getMessageStringId(int viewType) {
        int stringId = R.string.recentCalls_deleteDialog;
        switch (viewType) {
            case CallLogViewTypeHelper.VIEW_OPTION_MISSED:
                stringId = R.string.missedCalls_deleteDialog;
                break;
            case CallLogViewTypeHelper.VIEW_OPTION_RECEIVED:
                stringId = R.string.receivedCalls_deleteDialog;
                break;
            case CallLogViewTypeHelper.VIEW_OPTION_OUTGOING:
                stringId = R.string.outgoingCalls_deleteDialog;
                break;
            case CallLogViewTypeHelper.VIEW_OPTION_ALL:
                stringId = R.string.allCalls_deleteDialog;
                break;
            default:
                break;
        }
        return stringId;
    }
}
