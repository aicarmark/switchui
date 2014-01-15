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
 * limitations under the License
 */
package com.motorola.contacts.group;

import java.util.ArrayList;

import com.motorola.contacts.group.LocalGroupUtils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Meta-data for a contact aggregation group.  We load all groups associated with the contact's
 * constituent accounts.
 */

public class LocalGroupAccountMemberData implements Parcelable {
    private String mGroupTitle;
    private String mAccountType;
    private String mAccountName;
    private String mDataSet;
    private boolean mIsGroupMembershipEditable;

    private ArrayList<Long> mRawContactIdList;
    //private ArrayList<MemberData> mMemberRawDataList;

    public LocalGroupAccountMemberData(String groupTitle, String accountType, String accountName, String dataSet, boolean isGroupMemberEditable) {
        mGroupTitle = groupTitle;
        mAccountType = accountType;
        mAccountName = accountName;
        mDataSet = dataSet;
        mIsGroupMembershipEditable = isGroupMemberEditable;
        mRawContactIdList=  new ArrayList<Long> ();
        //mMemberRawDataList = new ArrayList<MemberData> ();
    }

    public String getGroupTitle() {
        return mGroupTitle;
    }

    public String getAccountType() {
        return mAccountType;
    }

    public String getAccountName() {
        return mAccountName;
    }

    public String getDataSet() {
        return mDataSet;
    }


    public ArrayList<Long> getRawContactIdList() {
        return mRawContactIdList;

    }

    public void setGroupMembershipEditable(boolean isEditable) {
        mIsGroupMembershipEditable = isEditable;
    }

    public boolean isGroupMembershipEditable() {
        return mIsGroupMembershipEditable;
    }


    public void insertRawContactId(long rawId) {
        mRawContactIdList.add(rawId);
    }

    // Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mGroupTitle);
        dest.writeString(mAccountType);
        dest.writeString(mAccountName);
        dest.writeString(mDataSet);
        boolean[] boolVal = new boolean[]{mIsGroupMembershipEditable};
        dest.writeBooleanArray(boolVal);
        dest.writeLongArray(LocalGroupUtils.convertToLongArray(mRawContactIdList));

    }

    private LocalGroupAccountMemberData(Parcel in) {
        mGroupTitle = in.readString();
        mAccountType = in.readString();
        mAccountName = in.readString();
        mDataSet = in.readString();
        boolean[] boolVal = in.createBooleanArray();
        mIsGroupMembershipEditable = boolVal[0];
        long[] longVal = in.createLongArray();
        mRawContactIdList = LocalGroupUtils.convertToLongList(longVal);
    }

    public static final Parcelable.Creator<LocalGroupAccountMemberData> CREATOR = new Parcelable.Creator<LocalGroupAccountMemberData>() {
        public LocalGroupAccountMemberData createFromParcel(Parcel in) {
            return new LocalGroupAccountMemberData(in);
        }

        public LocalGroupAccountMemberData[] newArray(int size) {
            return new LocalGroupAccountMemberData[size];
        }
    };

    /*
    public ArrayList<MemberData> getMemberRawDataList() {
        return mMemberRawDataList;

    }

    public void insertMemberRawData(MemberData rawData) {
        mMemberRawDataList.add(rawData);
    }

    public static class MemberData {
        private  long mRawContactId;
        private  long mContactId;
        private  String mDisplayName;
        private  Uri mLookupUri;
        private  Uri mPhotoUri;

        public MemberData(long rawId) {
            mRawContactId = rawId;
        }

        public long getRawId() {
            return mRawContactId;
        }
    }
    */

}
