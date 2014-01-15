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
 * limitations under the License.
 */

package com.motorola.contextual.commonutils.chips;

import com.motorola.contextual.smartrules.R;

import android.accounts.Account;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class RecipientAdapter extends BaseRecipientAdapter {

	private Context mContext;

    public RecipientAdapter(Context context, AddressEditTextView list) {
        super(context);
        mContext = context;
        Resources r = context.getResources();
        Bitmap def = BitmapFactory.decodeResource(r, R.drawable.ic_contact_picture_holo);
        list.setChipDimensions(
                r.getDrawable(R.drawable.chip_background),
                r.getDrawable(R.drawable.chip_background_selected),
                r.getDrawable(R.drawable.chip_background_invalid),
                r.getDrawable(R.drawable.chip_delete), def, R.layout.more_item,
                R.layout.chips_alternate_item,
                        r.getDimension(R.dimen.chip_height),
                        r.getDimension(R.dimen.chip_padding),
                        r.getDimension(R.dimen.chip_text_size),
                        R.layout.copy_chip_dialog_layout);
    }

    /**
     * Set the account when known. Causes the search to prioritize contacts from
     * that account.
     */
    public void setAccount(Account account) {
        if (account != null) {
            // TODO: figure out how to infer the contacts account
            // type from the email account
            super.setAccount(new android.accounts.Account(account.name, "unknown"));
        }
    }

    @Override
    protected int getDefaultPhotoResource() {
        return R.drawable.ic_contact_picture_holo;
    }

    @Override
    protected int getItemLayout() {
        return R.layout.chips_recipient_dropdown_item;
    }

    @Override
    protected int getWaitingForDirectorySearchLayout() {
        return R.layout.chips_waiting_for_directory_search;
    }

    @Override
    protected Cursor doQuery(CharSequence constraint, int limit,
    		Long directoryId) {        
        Uri uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, (String) constraint);

        return  mContext.getContentResolver().query(
                    uri, EmailQuery.PROJECTION,
                    null, null, Contacts.DISPLAY_NAME );
    }
}
