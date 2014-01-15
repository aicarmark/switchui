 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import android.content.Context;
import android.preference.PreferenceManager;

public abstract class TransitionFactory {

    private String mKey;
    private Context mContext;
    int default_effect = 0;

    public TransitionFactory (Context context, String key) {
        mContext = context;
        mKey = key;
    }

    public int getEffect() {
        String value = PreferenceManager.getDefaultSharedPreferences(
                mContext).getString(mKey, String.valueOf(default_effect));
        return Integer.parseInt(value);
    }

    public abstract PagedTransition creatPagedTransition(int type);
    public abstract EntryTransition creatEntryTransition(int type);
}
