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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;


/**
 * Some common functions and static String definitions.
 *
 */

public final class LocalGroupUtils {

    public static final String EXTRA_BROWSE_LOCAL_GROUP_TITLE = "browseLocalGroupTitle";

    public static final String EXTRA_EDIT_LOCAL_GROUP_TITLE = "editLocalGroupTitle";

    public static final String EXTRA_LOCAL_GROUP_TITLE = "localGroupTitle";

    public static final String EXTRA_SELECTED_LOCAL_GROUP_TITLE = "selectedLocalGroupTitle";

    public static final String TAG_DEBUG = "gfj763";
    public static final boolean DEBUG = true;

    public static long[] convertToLongArray(List<Long> listMembers) {
        int N = listMembers.size();
        long[] val = new long[N];
        for (int i = 0; i < N; i++) {
            val[i] = listMembers.get(i);
        }
        return val;
    }

    public static boolean[] convertToBooleanArray(List<Boolean> listMembers) {
        int N = listMembers.size();
        boolean[] val = new boolean[N];
        for (int i = 0; i < N; i++) {
            val[i] = listMembers.get(i);
        }
        return val;
    }

    public static ArrayList<Long> convertToLongList(long[] arrayMembers) {
        ArrayList<Long> val = new ArrayList<Long>();
        int N = arrayMembers.length;
        for (int i=0; i<N; i++) {
            val.add(arrayMembers[i]);
        }
        return val;
    }

    public static ArrayList<Boolean> convertToBooleanList(boolean[] arrayMembers) {
        ArrayList<Boolean> val = new ArrayList<Boolean>();
        int N = arrayMembers.length;
        for (int i = 0; i < N; i++) {
            val.add(arrayMembers[i]);
        }
        return val;
    }

    public static AccountType getLocalGroupAccountType(Context context, String accountType, String dataSet) {
        return AccountTypeManager.getInstance(context).getAccountType(accountType, dataSet);
    }

    /**
     * @return true if the group membership is editable on this account type.  false otherwise,
     *         or account is not set yet.
     */
    public static boolean isLocalGroupMembershipEditable(Context context, String accountType, String dataSet) {
        if (accountType == null) {
            return false;
        }

        return getLocalGroupAccountType(context, accountType, dataSet).isGroupMembershipEditable();
    }

    public static String buildSelectionIdList(ArrayList<Long> memberIdList) {
        if ( (null == memberIdList) || (memberIdList.isEmpty()) ) {
            return String.valueOf(-1);
        }

        List<String> memberContactIdList = new ArrayList<String>();
        for (long member : memberIdList) {
            memberContactIdList.add(String.valueOf(member));
        }
        return TextUtils.join(",", memberContactIdList);
    }

    public static void log(String logInfo) {
        if (DEBUG) {
            Log.d(TAG_DEBUG, logInfo);
        }
    }

}
