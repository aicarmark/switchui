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

/**
 * Meta-data for a contact group.  We load all groups associated with the contact's
 * constituent accounts.
 */
public final class LocalGroupListItem {
    private final String mTitle;
    private final int mMemberCount;
    private final boolean mIsFirstGroupInAccount;

    public LocalGroupListItem(
           String title, int memberCount, boolean isFirstGroupInAccount) {
        mTitle = title;
        mMemberCount = memberCount;
        mIsFirstGroupInAccount = isFirstGroupInAccount;
    }


    public String getTitle() {
        return mTitle;
    }

    public int getMemberCount() {
        return mMemberCount;
    }

    public boolean hasMemberCount() {
        return mMemberCount != -1;
    }

    public boolean isFirstGroupInAccount() {
        return mIsFirstGroupInAccount;
    }
}
