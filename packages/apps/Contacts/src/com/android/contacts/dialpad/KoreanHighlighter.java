/*
 * Copyright (C) 2010, Motorola, Inc,
 *
 * This file is derived in part from code issued under the following license.
 * Changed for MOTO UI requirements
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
 * @author khqn86
 */

//MOT calling code- IKPIM-598
package com.android.contacts.dialpad;;

import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import java.lang.Character;
import java.util.HashMap;


public class KoreanHighlighter extends Highlighter {
    private static KoreanHighlighter mKoreanHighlighter = null;
    private KoreanHighlighter(){
    }
    public static KoreanHighlighter getInstance(){
        if (null == mKoreanHighlighter) {
            mKoreanHighlighter = new KoreanHighlighter();
        }
        return mKoreanHighlighter;
    }

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

    public void nameHighlight(Spannable name, String matchStr, int color,
                int text_color) {
        String str = name.toString();
        int len = matchStr.length();
        int size = str.length();

        if(len == 0) return;//MOT Calling Code - IKMAIN-17655

        StringBuilder stripStrB = new StringBuilder(size);
        int[] acroArray = new int[size];
        int[] stripArray = new int[size];
        int[] acroStripArray = new int[size];
        boolean canAcro = true;

        int acroIndex = 0;
        int stripIndex = 0;

        for (int i = 0; i < size; i++) {
            char c = str.charAt(i);
            char ch = getKoreanConsonant(c);
            if ( (ch>= '\u3131') && (ch <= '\u314E')) {
                // if the char is Korean char, then convert it to digit by consonent
                char ch_k = mapToKoreanKeypad(ch);
                stripStrB.append(ch_k);
                stripArray[stripIndex] = i;
                acroArray[acroIndex] = i;
                acroStripArray[acroIndex] = stripIndex;
                acroIndex++;
                stripIndex++;

                canAcro = true;
            } else if ((c == ' ') || (c == '.') || (c == ',')) { //MOT Calling Code - IKMAIN-18641
                // ", . space" are considered as separator
                canAcro = true;
            } else if (Character.isLetterOrDigit(c)) { //MOT Calling Code - IKMAIN-18641
                // only "A~Z,a~z,0~9" are valid char to compare, all other symbols will ignore
                char NormalizedCh = coreLang2Alpha(c);
                stripStrB.append(NormalizedCh);
                stripArray[stripIndex] = i;
                if (canAcro) {
                    acroArray[acroIndex] = i;
                    acroStripArray[acroIndex] = stripIndex;
                    acroIndex++;
                }
                stripIndex++;
                canAcro = false;
            }
        // all the other char is special char, which to be stripped
        } //end of for clause

        String stripStr = stripStrB.toString();
        String stripNum = PhoneNumberUtils.convertKeypadLettersToDigits(stripStr);
        String patNum = PhoneNumberUtils.convertKeypadLettersToDigits(matchStr);
            // compare matchedStr with stripStr
            int pos = 0;
            while ((pos < acroIndex) && !stripNum.regionMatches(acroStripArray[pos], patNum, 0, len)) {
                pos++;
            }

            if (pos < acroIndex) {
            try {
                    // matched in the stripStr, and set styledname
                    name.setSpan(new BackgroundColorSpan(color), acroArray[pos],
                                    stripArray[acroStripArray[pos] + len - 1] + 1, 0);
                    name.setSpan(new ForegroundColorSpan(text_color), acroArray[pos],
                                    stripArray[acroStripArray[pos] + len - 1] + 1, 0);
            }catch(IndexOutOfBoundsException e) {
                // IndexOutOfBoundsException
            }
        }
    }
     private static char mapToKoreanKeypad(final char ch) {
        // Converts enter char to keypad mapping
        // To be modified for using Google PhoneNumberUtils.convertKeypadLettersToDigits instead.
        String s = String.valueOf(ch);
        //Jaum Unicode 0x3131 ~ 0x314E
        String key = KOREAN_KEYPAD_MAP.get(s);
        if(key != null) {
            return key.charAt(0);
        }
        return ch;
    }

    private char getKoreanConsonant(char ch) {
        int code = (int)ch;
         //Jaum Unicode 0x3131 ~ 0x314E
        if(code >= 0x3131 && code <= 0x314E) {
            for (int i = 0; i < mKoreanArray2.length; i++) {
                int charnum = mKoreanArray2[i].codePointAt(0);
                if(charnum == code) {
                    return ch;
                }
            }
        }
        for (int i = 0; i < mKoreanArray.length; i++) {
            if (mKoreanArray[i] <= code && code < mKoreanArray[i+1]) {
                return mKoreanArray2[i].charAt(0);
            }
        }
        return ch;
    }
}

