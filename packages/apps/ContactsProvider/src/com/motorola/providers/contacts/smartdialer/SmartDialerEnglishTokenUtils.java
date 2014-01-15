/*
 * Copyright (C) 2009/2010 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
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
 *
 * @author gfj763
 */

// BEGIN Motorola, PIM Contacts, gfj763, 4/12/2011, IKPIM-436
package com.motorola.providers.contacts.smartdialer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.text.TextUtils;

/**
 * This utility class is customized to generate English name token.
 *
 */

public class SmartDialerEnglishTokenUtils extends SmartDialerTokenUtils {

    public static final String TAG = "SmartDialerEnglishTokenUtils:";

    @Override
    public Iterator<Map.Entry<String, Integer>> getSmartDialerToken(String name) {
        HashMap<String, Integer> tokenKeysHash = new HashMap<String, Integer>();

        log(TAG + "input name = " + name);

        if (TextUtils.isEmpty(name)) {
            log("input name is null or empty, no need to generate name token !");
            return null;
        }

        final StringBuilder fullPartialTk = new StringBuilder();
        //final StringBuilder acronymTk = new StringBuilder();

        String[] splitNameArray = name.split("[\\s]+"); // split name by space character

        for (int i = splitNameArray.length - 1, j = 0; i >= 0; i--, j++) {
            log("nameSpliter[" + i + "] = [" + splitNameArray[i] + "], length = " + splitNameArray[i].length());
            String filterStr = stripSeparators(splitNameArray[i]);
            if (!TextUtils.isEmpty(filterStr)) {
                String digitStr = alphabet2Num(filterStr);
                if (!TextUtils.isEmpty(digitStr)) {
                    fullPartialTk.insert(INITIAL_OFFSET, digitStr);
                    //acronymTk.insert(INITIAL_OFFSET, digitStr.charAt(INITIAL_OFFSET));
                    log("nameFilter[" + i + "] = [" + splitNameArray[i] + "], tokenDigit=[" + digitStr + "]");
                } else {
                        continue;
                    }

                String fullpartial_token = fullPartialTk.toString();
                if (!TextUtils.isEmpty(fullpartial_token)) {
                    tokenKeysHash.put(fullpartial_token, Integer.valueOf(FULL_PARTIAL_NAME_FLAG));
                }

                /*
                // No need to support acronym token for English name
                String acronym_token = acronymTk.toString();
                if (!TextUtils.isEmpty(acronym_token)) {
                    tokenkeysHash.put(acronym_token, Integer.valueOf(ACRONYM_NAME_FLAG));
                }
                */

                log("token[" + j + "] = " + fullpartial_token);
            }
        }

        return tokenKeysHash.entrySet().iterator();
    }

}
// END Motorola, PIM Contacts, gfj763, 4/12/2011, IKPIM-436

