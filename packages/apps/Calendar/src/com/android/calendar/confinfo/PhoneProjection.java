package com.android.calendar.confinfo;

import android.provider.ContactsContract.CommonDataKinds;

public final class PhoneProjection {

    private PhoneProjection() {
    }

    public static final String[] PHONES_PROJECTION = new String[] { CommonDataKinds.Phone._ID, // 0
            CommonDataKinds.Phone.TYPE, // 1
            CommonDataKinds.Phone.LABEL, // 2
            CommonDataKinds.Phone.NUMBER, // 3
            CommonDataKinds.Phone.DISPLAY_NAME, // 4
    };
    public static final int sPhoneIdIdx = 0;
    public static final int sPhoneTypeIdx = 1;
    public static final int sPhoneLabelIdx = 2;
    public static final int sPhoneNumberIdx = 3;
    public static final int sPhoneDisplayNameIdx = 4;
}