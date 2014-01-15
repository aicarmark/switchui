/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.model;

import com.android.contacts.R;

import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;

import com.android.contacts.util.DateUtils;
import com.google.android.collect.Lists;

public class FallbackAccountType extends BaseAccountType {
    private static final String TAG = "FallbackAccountType";

    private FallbackAccountType(Context context, String resPackageName) {
        this.accountType = null;
        this.dataSet = null;
        this.titleRes = R.string.account_phone;
        this.iconRes = R.mipmap.ic_launcher_contacts;

        this.resPackageName = resPackageName;
        this.summaryResPackageName = resPackageName;

        try {
            addDataKindStructuredName(context);
            addDataKindDisplayName(context);
            addDataKindPhoneticName(context);
            addDataKindNickname(context);
            addDataKindPhone(context);
            addDataKindEmail(context);
            addDataKindStructuredPostal(context);
            addDataKindIm(context);
            addDataKindOrganization(context);
            addDataKindPhoto(context);
            addDataKindNote(context);
            addDataKindWebsite(context);
            //2012.09.13 jrw647 removed to meet CTA lab requirement
            //addDataKindSipAddress(context);
//MOTO MOD BEGIN IKHSS6-2526 add event mimetype support in FallbackAccountType
            addDataKindEvent(context);
//MOTO MOD END
            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building account type", e);
        }
    }

    public FallbackAccountType(Context context) {
        this(context, null);
    }

    /**
     * Used to compare with an {@link ExternalAccountType} built from a test contacts.xml.
     * In order to build {@link DataKind}s with the same resource package name,
     * {@code resPackageName} is injectable.
     */
    static AccountType createForTest(Context context, String resPackageName) {
        return new FallbackAccountType(context, resPackageName);
    }

    @Override
    public boolean areContactsWritable() {
        return true;
    }

//MOTO MOD BEGIN IKHSS6-2526 add event mimetype support in FallbackAccountType
    protected DataKind addDataKindEvent(Context context) throws DefinitionException {
        DataKind kind = addKind(
                new DataKind(Event.CONTENT_ITEM_TYPE, R.string.eventLabelsGroup, 150, true,
                R.layout.event_field_editor_view));
        kind.actionHeader = new EventActionInflater();
        kind.actionBody = new SimpleInflater(Event.START_DATE);

        kind.typeOverallMax = 1;

        kind.typeColumn = Event.TYPE;
        kind.typeList = Lists.newArrayList();
        kind.typeList.add(buildEventType(Event.TYPE_BIRTHDAY, false).setSpecificMax(1));

        kind.dateFormatWithYear = DateUtils.DATE_AND_TIME_FORMAT;

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Event.DATA, R.string.eventLabelsGroup, FLAGS_EVENT));

        return kind;
    }
//MOTO MOD END
}
