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

package com.android.contacts.preference;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * This fragment shows the preferences for the first header.
 */
public class CT189PreferenceFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String LOG_TAG = "CT189PreferenceFragment";
    private CheckBoxPreference mCheckBoxPref = null;

    public interface Prefs {
        public static final String  KEY_ADD_189 = "add_189";
        public static final boolean KEY_ADD_189_DEFAULT = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        if (ContactsUtils.isCT189EmailEnabled(getActivity())) {
            addPreferencesFromResource(R.xml.preferences_189email);

            mCheckBoxPref = (CheckBoxPreference)findPreference(Prefs.KEY_ADD_189);
            if (mCheckBoxPref != null) {
                mCheckBoxPref.setOnPreferenceChangeListener(this);
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        // Log.v(LOG_TAG, "onPreferenceChange() called, preference = "+preference+", key= "+key+", newValue ="+newValue);

        if (preference instanceof CheckBoxPreference) {
                ((CheckBoxPreference) preference).setChecked((Boolean) newValue);
        }
        return true;
    }

}

