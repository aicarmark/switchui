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

//import com.android.contacts.DeviceSetup;

/**
 * Hard-coded definition of some {@link ContactsSource} constraints, since the
 * XML language hasn't been finalized.
 */
public class HardCodedSources {
    // TODO: finish hard-coding all constraints

    public static final String ACCOUNT_TYPE_LOCAL = "com.local.contacts";    
    public static final String ACCOUNT_TYPE_CARD = "com.card.contacts";
/*    
    public static final String ACCOUNT_TYPE_BLUR = DeviceSetup.MOTHER_USER_CREDS_TYPE;
    public static final String ACCOUNT_TYPE_BLUR_CHILD = DeviceSetup.GAMS_BLUR_AUTHENTICATOR_TYPE;
    public static final String ACCOUNT_TYPE_BLUR_UNCONNECTED = DeviceSetup.UNCONNECTED_ACCOUNT_TYPE;
    public static final String ACCOUNT_TYPE_BLUR_ALL = DeviceSetup.GAMS_BLUR_TYPE;
*/
    public static final String ACCOUNT_TYPE_BLUR = "com.motorola.blur.service.bsutils.MOTHER_USER_CREDS_TYPE";
    public static final String ACCOUNT_TYPE_BLUR_CHILD = "com.motorola.blur.provider";
    public static final String ACCOUNT_TYPE_BLUR_UNCONNECTED = "com.motorola.blur.contacts.UNCONNECTED_ACCOUNT";
    public static final String ACCOUNT_TYPE_BLUR_ALL = "com.motorola.blur";

    
    public static final String ACCOUNT_LOCAL_DEVICE = "local-contacts";
    public static final String ACCOUNT_CARD = "card-contacts";
    public static final String ACCOUNT_CARD_C = "c-contacts";
    public static final String ACCOUNT_CARD_G = "g-contacts";
    
    public static final String LOCAL_CONTACTS_SYSTEM_GROUP = "System Group: Local Contacts";    

    private HardCodedSources() {
        // Static utility class
    }

}
