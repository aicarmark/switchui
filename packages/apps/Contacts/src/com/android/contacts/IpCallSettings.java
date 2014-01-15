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

package com.android.contacts;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.EditTextPreference;
import android.util.Log;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.view.MenuItem;

import com.motorola.android.telephony.PhoneModeManager;

public class IpCallSettings extends PreferenceActivity {
	
    private EditTextPreference cdmaIP;
    private EditTextPreference gsmIP;

    private static final String CDMA_IP_KEY = "ip_cdma";
    private static final String GSM_IP_KEY = "ip_gsm";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        addPreferencesFromResource(R.xml.ip_call_setting);
        
        cdmaIP = (EditTextPreference)findPreference(CDMA_IP_KEY);
        gsmIP = (EditTextPreference)findPreference(GSM_IP_KEY);

        String curIP;
        curIP = cdmaIP.getText();
        cdmaIP.setSummary((curIP.length() == 0) ? getString(R.string.empty_ip_indication) : curIP);

        SharedPreferences ip_shareddata = getSharedPreferences("IP_PREFIX", MODE_WORLD_READABLE);
        String ip_gsm = ip_shareddata.getString("ip_gsm", null);
        if (ip_gsm == null) {
            // If user never change ip prefix, we will use default. Otherwise we keep user's input
            int res_id = ContactsUtils.getDefaultIPPrefixbyPhoneType(TelephonyManager.PHONE_TYPE_GSM);
            if (res_id != -1) {
                ip_gsm = getString(res_id);
            } else { // For safty
                ip_gsm = getString(R.string.default_gsm_ip_prefix);
            }
            gsmIP.setText(ip_gsm);
            Log.d("IpCallSettings", "ip_gsm = "+ip_gsm);
        }

        curIP = gsmIP.getText();
        gsmIP.setSummary((curIP.length() == 0) ? getString(R.string.empty_ip_indication) : curIP);

        cdmaIP.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
            	String newIP = newValue.toString();
            	pref.setSummary((newIP.length() == 0) ? getString(R.string.empty_ip_indication) : newIP);
            	SharedPreferences.Editor shareddata = getSharedPreferences("IP_PREFIX", MODE_WORLD_READABLE).edit();
            	shareddata.putString("ip_cdma", newIP);
            	shareddata.commit();
                return true;
            }
        });
        gsmIP.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object newValue) {
               	String newIP = newValue.toString();
            	pref.setSummary((newIP.length() == 0) ? getString(R.string.empty_ip_indication) : newIP);
            	SharedPreferences.Editor shareddata = getSharedPreferences("IP_PREFIX", MODE_WORLD_READABLE).edit();
            	shareddata.putString("ip_gsm", newIP);
            	shareddata.commit();
                return true;
            }
        });

        if (!PhoneModeManager.isDmds()) {
            if (TelephonyManager.getDefault().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
                cdmaIP.setTitle(R.string.ip_title);
                getPreferenceScreen().removePreference(gsmIP);
            } else {
                gsmIP.setTitle(R.string.ip_title);
                getPreferenceScreen().removePreference(cdmaIP);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
        }
        return false;
    }

}
