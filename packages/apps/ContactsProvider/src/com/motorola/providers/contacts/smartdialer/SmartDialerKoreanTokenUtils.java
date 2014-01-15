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

// BEGIN Motorola, PIM Contacts, gfj763, 8/11/2011, IKPIM-697
package com.motorola.providers.contacts.smartdialer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.text.TextUtils;

// BEGIN Motorola, PIM Contacts, gfj763, IKPIM-697

/**
 * This utility class is customized to generate Korean name token.
 *
 */

public class SmartDialerKoreanTokenUtils extends SmartDialerTokenUtils {

    public static final String TAG = "SmartDialerKoreanTokenUtils:";

    private static final HashMap<String, String> KOREAN_KEYPAD_MAP = new HashMap<String, String>();
    static {
        KOREAN_KEYPAD_MAP.put("\u3131", "1"); KOREAN_KEYPAD_MAP.put("\u3132", "1"); KOREAN_KEYPAD_MAP.put("\u314b", "1");
        KOREAN_KEYPAD_MAP.put("\u3134", "2"); KOREAN_KEYPAD_MAP.put("\u3141", "2");
        KOREAN_KEYPAD_MAP.put("\u3137", "4"); KOREAN_KEYPAD_MAP.put("\u3138", "4"); KOREAN_KEYPAD_MAP.put("\u314c", "4");
        KOREAN_KEYPAD_MAP.put("\u3139", "5");
        KOREAN_KEYPAD_MAP.put("\u3142", "7"); KOREAN_KEYPAD_MAP.put("\u3143", "7"); KOREAN_KEYPAD_MAP.put("\u314d", "7");
        KOREAN_KEYPAD_MAP.put("\u3145", "8"); KOREAN_KEYPAD_MAP.put("\u3146", "8");
        KOREAN_KEYPAD_MAP.put("\u3148", "*"); KOREAN_KEYPAD_MAP.put("\u3149", "*"); KOREAN_KEYPAD_MAP.put("\u314a", "*");
        KOREAN_KEYPAD_MAP.put("\u3147", "0"); KOREAN_KEYPAD_MAP.put("\u314e", "0");
    }

    private static int[] mKoreanArray = {
        0xAC00, 0xAE4C, 0xB098, 0xB2E4, 0xB530, // ga, gga, na, da, dda
        0xB77C, 0xB9C8, 0xBC14, 0xBE60, 0xC0AC, // la, ma, ba, bba, sa
        0xC2F8, 0xC544, 0xC790, 0xC9DC, 0xCC28, // ssa, a, ja, jja, cha
        0xCE74, 0xD0C0, 0xD30C, 0xD558, 0xD7A4  // ka, ta, pa, ha, end_boundary
    };

    private static String[] mKoreanArray2 = {
        "\u3131", "\u3132", "\u3134", "\u3137", "\u3138", // kiyeok, ssangkiyeok, nieun, digood, ssangdigood
        "\u3139", "\u3141", "\u3142", "\u3143", "\u3145", // rioul, mieum, bieum, ssangbieum, siout
        "\u3146", "\u3147", "\u3148", "\u3149", "\u314A", // ssang siout, eieng, jiout, ssang jiout, chiout
        "\u314B", "\u314C", "\u314D", "\u314E"            // kioek, tieot, pieup, hieut
    };

    private static int[] KOREAN_JAUM_CONVERT_MAP = {
        // JAUM in Hangul Compatibility Jamo area 0x3131 ~ 0x314E to
        // in Hangul Jamo area 0x1100 ~ 0x1112
        0x1100, // 0x3131 HANGUL LETTER KIYEOK
        0x1101, // 0x3132 HANGUL LETTER SSANGKIYEOK
        0x00,   // 0x3133 HANGUL LETTER KIYEOKSIOS (Ignored)
        0x1102, // 0x3134 HANGUL LETTER NIEUN
        0x00,   // 0x3135 HANGUL LETTER NIEUNCIEUC (Ignored)
        0x00,   // 0x3136 HANGUL LETTER NIEUNHIEUH (Ignored)
        0x1103, // 0x3137 HANGUL LETTER TIKEUT
        0x1104, // 0x3138 HANGUL LETTER SSANGTIKEUT
        0x1105, // 0x3139 HANGUL LETTER RIEUL
        0x00,   // 0x313A HANGUL LETTER RIEULKIYEOK (Ignored)
        0x00,   // 0x313B HANGUL LETTER RIEULMIEUM (Ignored)
        0x00,   // 0x313C HANGUL LETTER RIEULPIEUP (Ignored)
        0x00,   // 0x313D HANGUL LETTER RIEULSIOS (Ignored)
        0x00,   // 0x313E HANGUL LETTER RIEULTHIEUTH (Ignored)
        0x00,   // 0x313F HANGUL LETTER RIEULPHIEUPH (Ignored)
        0x00,   // 0x3140 HANGUL LETTER RIEULHIEUH (Ignored)
        0x1106, // 0x3141 HANGUL LETTER MIEUM
        0x1107, // 0x3142 HANGUL LETTER PIEUP
        0x1108, // 0x3143 HANGUL LETTER SSANGPIEUP
        0x00,   // 0x3144 HANGUL LETTER PIEUPSIOS (Ignored)
        0x1109, // 0x3145 HANGUL LETTER SIOS
        0x110A, // 0x3146 HANGUL LETTER SSANGSIOS
        0x110B, // 0x3147 HANGUL LETTER IEUNG
        0x110C, // 0x3148 HANGUL LETTER CIEUC
        0x110D, // 0x3149 HANGUL LETTER SSANGCIEUC
        0x110E, // 0x314A HANGUL LETTER CHIEUCH
        0x110F, // 0x314B HANGUL LETTER KHIEUKH
        0x1110, // 0x314C HANGUL LETTER THIEUTH
        0x1111, // 0x314D HANGUL LETTER PHIEUPH
        0x1112  // 0x314E HANGUL LETTER HIEUH
    };

    private static SmartDialerKoreanTokenUtils mKoreanTokenInstance = null;

    public static SmartDialerKoreanTokenUtils getInstance() {
        if (null == mKoreanTokenInstance) {
            return new SmartDialerKoreanTokenUtils();
        }
        return mKoreanTokenInstance;
    }

    @Override
    public Iterator<Map.Entry<String, Integer>> getSmartDialerToken(String name) {
        HashMap<String, Integer> tokenKeysHash = new HashMap<String, Integer>();

        log(TAG + "name = " + name);

        if (TextUtils.isEmpty(name)) {
            log("input name is null or empty, no need to generate name token!");
            return null;
        }

        final StringBuilder nameTokenSB = new StringBuilder();
        final StringBuilder latinTokenSB = new StringBuilder();
        String tokenStr;

        if (!TextUtils.isEmpty(name)) {
            boolean isLatinDigitChar = false;
            for (int i = name.length() - 1, j = 0; i >= 0; i--) {
                char ch = name.charAt(i);
                char tryLatinMap[] = coreLanAlpha2Digit(ch);
                if (CHAR_NULL != tryLatinMap[0]) { // This is a Latin letter or digit
                    isLatinDigitChar = true;
                    latinTokenSB.insert(INITIAL_OFFSET, tryLatinMap);
                    if (i > 0) {
                        continue;
                    } else {
                        nameTokenSB.insert(INITIAL_OFFSET, latinTokenSB.toString());
                        tokenStr = nameTokenSB.toString();
                        if (!TextUtils.isEmpty(tokenStr)) {
                            tokenKeysHash.put(tokenStr, Integer.valueOf(HANGUL_CONSONANTS));
                            log("token[" + j++ + "] = " + tokenStr);
                        }
                        break;
                    }
                }

                if (isLatinDigitChar) {
                    nameTokenSB.insert(INITIAL_OFFSET, latinTokenSB.toString());
                    latinTokenSB.delete(0,latinTokenSB.length());
                    isLatinDigitChar = false;

                    tokenStr = nameTokenSB.toString();
                    if (!TextUtils.isEmpty(tokenStr)) {
                        tokenKeysHash.put(tokenStr, Integer.valueOf(HANGUL_CONSONANTS));
                        log("token[" + j++ + "] = " + tokenStr);
                    }
                }

                char tryKoreanMap[] = mapKorean2Digit(ch);
                if (CHAR_NULL != tryKoreanMap[0]) {
                    nameTokenSB.insert(INITIAL_OFFSET, tryKoreanMap);
                } else {
                    continue;
                }

                tokenStr = nameTokenSB.toString();
                if (!TextUtils.isEmpty(tokenStr)) {
                    tokenKeysHash.put(tokenStr, Integer.valueOf(HANGUL_CONSONANTS));
                    log("token[" + j++ + "] = " + tokenStr);
                }
                }

        }

        return tokenKeysHash.entrySet().iterator();

    }

    private char [] mapKorean2Digit(final char ch) {
        // Converts enter char to keypad mapping
        // To be modified for using Google PhoneNumberUtils.convertKeypadLettersToDigits instead.

        char null_char [] = {CHAR_NULL};

        String charStr = String.valueOf(ch);
        int unicode = charStr.codePointAt(0);
        int character;

        //Jaum Unicode 0x3131 ~ 0x314E
        if(unicode >= 0x3131 && unicode <= 0x314E) {
            character = KOREAN_JAUM_CONVERT_MAP[unicode - 0x3131];
            if (character != 0) {
                // This is lead-consonant
                return KOREAN_KEYPAD_MAP.get(charStr).toCharArray();
            }
        } else if (mKoreanArray[0] <= unicode && unicode < mKoreanArray[mKoreanArray.length -1]) {
            for (int i = 0; i < mKoreanArray.length; i++) {
                if (mKoreanArray[i] <= unicode && unicode < mKoreanArray[i+1]) {
                    return KOREAN_KEYPAD_MAP.get(mKoreanArray2[i]).toCharArray();
                }
            }
        } else if ((unicode == 0x20) ||
                 (unicode > 0x1112 && unicode < 0x3131) ||
                 (unicode > 0x314E && unicode < 0xAC00) ||
                 (unicode > 0xD7A3)) {
            // Exclude space & characters that are not in Korean leading consonants area
            // and Korean characters area.
            return null_char;
        }

        // we tried to support mapping for 0-9 digits & Latin alphabet in Korean name
        // Will return 0 if digit & Latin character not found.
        return coreLanAlpha2Digit(ch);
    }

}
// END Motorola, PIM Contacts, gfj763, IKPIM-697

