/*
 * Copyright (C) 2010/2011 Motorola Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.text.TextUtils;


// BEGIN Motorola, PIM Contacts, gfj763, IKPIM-697

/**
 * This utility class is customized to generate Core languages name token.
 *
 * In smartDialer Phase II, the supported core languages include
 * 1) en_GB (English Great Britain)
 * 2) fr_FR (French Europe)
 * 3) fr_CA (French Canada)
 * 4) it_IT  (Italy)
 * 5) de_DE (German Germany)
 * 6) es_ES (Spanish Spain)
 * 7) es_US (Spanish Latin America - Mexico, Columbia, Argentina, etc.)
 * 8) pt_BR (Portuguese Brazil)
 * 9) nl_NL  (Dutch Netherlands)
 *
 */

public class SmartDialerWesternCoreLanguageTokenUtils extends SmartDialerTokenUtils {

    public static final String TAG = "SmartDialerWesternCoreLanguageTokenUtils:";
    private static SmartDialerWesternCoreLanguageTokenUtils mWesternCoreLanTokenInstance = null;

    public static SmartDialerWesternCoreLanguageTokenUtils getInstance() {
        if (null == mWesternCoreLanTokenInstance) {
            return new SmartDialerWesternCoreLanguageTokenUtils();
        }
        return mWesternCoreLanTokenInstance;
    }

    private String constructFullToken (String parent, String son) {
    	String remaining = null;
    	String result = son;
    	int end = 0;
    	
    	if (!TextUtils.isEmpty(parent)) {
    		end = parent.lastIndexOf(son);
    		if (end != -1) {
    	        remaining = parent.substring(0, end);
    	    }
        }
        
        result += remaining;
    	log(TAG + "constructFullToken(), remaining = "+remaining+", final token = " + result);
    	
    	return result;
    }

    @Override
    public Iterator<Map.Entry<String, Integer>> getSmartDialerToken(String name) {
        HashMap<String, Integer> tokenKeysHash = new HashMap<String, Integer>();

        ArrayList<String> fullTokenBuffer = new ArrayList<String>();
        ArrayList<String> initialTokenBuffer = new ArrayList<String>();        

        log(TAG + "input name = " + name);

        if (TextUtils.isEmpty(name)) {
            log("input name is null or empty, no need to generate name token !");
            return null;
        }

        final StringBuilder fullPartialTk = new StringBuilder();
        final StringBuilder acronymTk = new StringBuilder();

        String[] splitNameArray = name.split("(,)+|(\\.)+|(\\s+)");

        for (int i = splitNameArray.length - 1, j = 0; i >= 0; i--, j++) {
            log("nameSpliter[" + i + "] = [" + splitNameArray[i] + "], length = " + splitNameArray[i].length());

            String filterStr = stripUnSupportCharacter(splitNameArray[i]);
            if (!TextUtils.isEmpty(filterStr)) {
                String digitStr = coreLanAlpha2Digit(filterStr);
                if (!TextUtils.isEmpty(digitStr)) {
                    fullPartialTk.insert(INITIAL_OFFSET, digitStr);
                    acronymTk.insert(INITIAL_OFFSET, digitStr.charAt(INITIAL_OFFSET));
                    log("nameFilter[" + i + "] = [" + filterStr + "], tokenDigit = [" + digitStr + "]");
                } else {
                    continue;
                }

                String fullpartial_token = fullPartialTk.toString();
                if (!TextUtils.isEmpty(fullpartial_token)) {
                	fullTokenBuffer.add(fullpartial_token);
                    //tokenKeysHash.put(fullpartial_token, Integer.valueOf(FULL_PARTIAL_NAME_FLAG));
                }

                String acronym_token = acronymTk.toString();
                if (!TextUtils.isEmpty(acronym_token)) {
                	initialTokenBuffer.add(acronym_token);
                	//tokenKeysHash.put(acronym_token, Integer.valueOf(ACRONYM_NAME_FLAG));
                }

                log("token[" + j + "] = " + fullpartial_token);
            }
        }
        
        // adjust full name token
        int size = fullTokenBuffer.size();
        String resToken;
        if (size > 0) {
            String full = fullTokenBuffer.get(size-1);
            if (size > 1) {
                for (int i=size-1; i>=0; i--) {
            	    resToken = constructFullToken (full, fullTokenBuffer.get(i));
            	    tokenKeysHash.put(resToken, Integer.valueOf(FULL_PARTIAL_NAME_FLAG));
                }
            } else {
            	tokenKeysHash.put(full, Integer.valueOf(FULL_PARTIAL_NAME_FLAG));
            }
        }       
        
        // adjust initial name token
        size = initialTokenBuffer.size();
        if (size > 0) {
            String full = initialTokenBuffer.get(size-1);
            if (size > 1) {
                for (int i=size-1; i>=0; i--) {
            	    resToken = constructFullToken (full, initialTokenBuffer.get(i));
            	    tokenKeysHash.put(resToken, Integer.valueOf(ACRONYM_NAME_FLAG));
                }
            } else {
            	tokenKeysHash.put(full, Integer.valueOf(ACRONYM_NAME_FLAG));
            }
        }               

        return tokenKeysHash.entrySet().iterator();
    }

}

// END Motorola, PIM Contacts, gfj763, IKPIM-697

