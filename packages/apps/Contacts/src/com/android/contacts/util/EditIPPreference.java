/*
 * Copyright (C) 2008 The Android Open Source Project
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
package com.android.contacts.util;

import android.app.Dialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.method.DigitsKeyListener;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.text.TextUtils;

/**
 * TODO: set the cursor to the end of text.
 */
class EditIPPreference extends EditTextPreference {

    
    public EditIPPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditIPPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        EditText editText = getEditText();

        if (editText != null) {
            String value = editText.getText().toString();
            if(!TextUtils.isEmpty(value)){
                editText.setSelection(value.length());    
            }
        }
    }

}
