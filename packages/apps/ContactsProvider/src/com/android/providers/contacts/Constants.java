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
package com.android.providers.contacts;

public class Constants {
    private Constants() {}

    // Log tag for performance measurement.
    // To enable: adb shell setprop log.tag.ContactsPerf VERBOSE
    public static final String PERFORMANCE_TAG = "ContactsPerf";

    //MOT MOD BEGIN -- IKPIM-955
    public static final String RawContacts_IS_RESTRICTED = "is_restricted";
    public static final String RawContactsColumns_CONCRETE_IS_RESTRICTED = "raw_contacts.is_restricted" ;
    public static final String ContactsContract_REQUESTING_PACKAGE_PARAM_KEY = "requesting_package";
    public static final String Contacts_Entity_IS_RESTRICTED = "is_restricted";
    public static final String Data_FOR_EXPORT_ONLY = "for_export_only";
    //MOT MOD END -- IKPIM-955
}
