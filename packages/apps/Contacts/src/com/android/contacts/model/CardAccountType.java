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
import com.android.contacts.model.AccountType.DefinitionException;
import com.android.contacts.SimUtility;
//import com.motorola.android.telephony.PhoneModeManager;
import com.google.android.collect.Lists;

import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import com.android.contacts.util.Constants;

import android.util.Log;

public class CardAccountType extends BaseAccountType {
	private static final String TAG = "CardAccountType";

    public static final String ACCOUNT_TYPE = HardCodedSources.ACCOUNT_TYPE_CARD;

    public CardAccountType(Context context, String resPackageName) {
        this.accountType = ACCOUNT_TYPE;
        this.resPackageName = null;
        this.summaryResPackageName = resPackageName;
 
        try {
            addDataKindStructuredName(context);
            addDataKindDisplayName(context);
            addDataKindPhoneticName(context);
            addDataKindPhone(context);
            addDataKindEmail(context);
            
            mIsInitialized = true;
        } catch (DefinitionException e) {
            Log.e(TAG, "Problem building card account type", e);
        }
    }
/*
    @Override
    protected DataKind addDataKindStructuredName(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(StructuredName.CONTENT_ITEM_TYPE,
                R.string.nameLabelsGroup, -1, true, R.layout.structured_name_editor_view));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);

        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME,
                R.string.full_name, FLAGS_PERSON_NAME));

        return kind;
    }
*/
    @Override
    protected DataKind addDataKindDisplayName(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME,
                R.string.nameLabelsGroup, -1, true, R.layout.text_fields_editor_view));
        kind.actionHeader = new SimpleInflater(R.string.nameLabelsGroup);
        kind.actionBody = new SimpleInflater(Nickname.NAME);
        kind.typeOverallMax = 1;

        kind.fieldList = Lists.newArrayList();
        // only show a single 'Name' field
        kind.fieldList.add(new EditField(StructuredName.DISPLAY_NAME,
                R.string.full_name, FLAGS_PERSON_NAME).setShortForm(true));

        return kind;
    } 

    @Override
    protected DataKind addDataKindPhone(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(Phone.CONTENT_ITEM_TYPE, R.string.phoneLabelsGroup,
                10, true, R.layout.text_fields_editor_view));
        kind.iconAltRes = R.drawable.ic_text_holo_light;
        kind.iconAltDescriptionRes = R.string.sms;
        kind.actionHeader = new PhoneActionInflater();
        kind.actionAltHeader = new PhoneActionAltInflater();
        kind.actionBody = new SimpleInflater(Phone.NUMBER);
        kind.typeOverallMax = 2;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add((new EditField(Phone.NUMBER, R.string.phoneLabelsGroup, FLAGS_PHONE)).setAcceptChars(Constants.validPhoneNumber));

        return kind;
    }
    
    @Override
    protected DataKind addDataKindEmail(Context context) throws DefinitionException {
        DataKind kind = addKind(new DataKind(Email.CONTENT_ITEM_TYPE, R.string.emailLabelsGroup,
                15, true, R.layout.text_fields_editor_view));
        kind.actionHeader = new EmailActionInflater();
        kind.actionBody = new SimpleInflater(Email.DATA);
        kind.typeOverallMax = 1;
        kind.fieldList = Lists.newArrayList();
        kind.fieldList.add(new EditField(Email.DATA, R.string.emailLabelsGroup, FLAGS_EMAIL));

        return kind;
    }

/*
    public CardAccountType() {
        this.accountType = ACCOUNT_TYPE;
        if (SimUtility.PhoneModeManager.isDmds()) {
            this.titleRes = R.string.account_card;
        } else {
            this.titleRes = R.string.account_card_single_c;
        }
        this.iconRes = R.drawable.ic_launcher_card;
    }

    @Override
    protected void inflate(Context context, int inflateLevel) {
        super.inflate(context, inflateLevel);
    }
*/
    @Override
    public boolean areContactsWritable() {
        return true;
    }

}
