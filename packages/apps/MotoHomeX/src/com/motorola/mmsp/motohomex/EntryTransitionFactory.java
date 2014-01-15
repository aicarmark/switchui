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

package com.motorola.mmsp.motohomex;

import android.content.Context;

public class EntryTransitionFactory extends TransitionFactory {
    private String mKey;
    private Context mContext;

    public EntryTransitionFactory(Context context, String key) {
        super(context, key);
    }

    public EntryTransition creatEntryTransition(int type) {
        switch (type) {
        /*2012-6-20, modify by bvq783 for switchui-1569*/
        case 0:
        /*    return EntryTransitionDefault.getInstance();
        case 1:*/
            return EntryTransitionFly.getInstance();
        /*2012-6-20, modify end*/
        default:
            throw new RuntimeException("Unknown type id " + type + "!");
        }
    }

    public PagedTransition creatPagedTransition(int type) {
        return null;
    }
}
