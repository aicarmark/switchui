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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.text.TextUtils;

import com.android.providers.contacts.HanziToPinyin;
import com.android.providers.contacts.HanziToPinyin.Token;

/**
 * This utility class is customized to generate Chinese name token.
 *
 */

public class SmartDialerChineseTokenUtils extends SmartDialerTokenUtils {

    public static final String TAG = "SmartDialerChineseTokenUtils:";
    private static SmartDialerChineseTokenUtils mChineseTokenInstance = null;

    public static SmartDialerChineseTokenUtils getInstance() {
        if (null == mChineseTokenInstance) {
            return new SmartDialerChineseTokenUtils();
        }
        return mChineseTokenInstance;
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
            log("input name is empty, no need to generate name token !");
            return null;
        }

        final StringBuilder fullPartialTk = new StringBuilder();
        final StringBuilder acronymTk = new StringBuilder();

        ArrayList<Token> tokens = HanziToPinyin.getInstance().get(name);
        final int tokenCount = tokens.size();

        log("token count=" + tokenCount);

        for (int i = tokenCount - 1, j = 0; i >= 0; i--, j++) {
            final Token token = tokens.get(i);

            String digitStr = coreLanAlpha2Digit(token.target);

            if (!TextUtils.isEmpty(digitStr)) {
                fullPartialTk.insert(INITIAL_OFFSET, digitStr);
                acronymTk.insert(INITIAL_OFFSET, digitStr.charAt(INITIAL_OFFSET));
                log("nameSpliter[" + i + "] = [" + token.source + "], pinyin=[" + token.target + "], tokenDigit=[" + digitStr + "]");
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

            log("fullpartial_token[" + j + "] = " + fullpartial_token);
            log("acronym_token[" + j + "] = " + acronym_token + ")");
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
// END Motorola, PIM Contacts, gfj763, 4/12/2011, IKPIM-436

