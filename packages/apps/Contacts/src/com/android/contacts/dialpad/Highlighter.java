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

//MOT calling code- IKPIM-447
package com.android.contacts.dialpad;

import com.android.contacts.activities.DialtactsActivity;
import android.provider.ContactsContract.FullNameStyle;
import android.telephony.PhoneNumberUtils;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;//MOT Calling code - IKSTABLETWOV-2767
import android.util.Log;

//MOT Calling Code - IKPIM-598
import java.lang.Character.UnicodeBlock;
import java.text.Normalizer;
//End MOT Calling Code - IKPIM-598

public abstract class  Highlighter {
    public static final String TAG  = "Highlighter";

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    /*Use this SAFE_RECURSE_TIMES to make sure that some special string will not be recursed too many times when highlighting, 
    or it will affect the performance of highlighting, or some unpredictable errors may occur. */
    protected static final int SAFE_RECURSE_TIMES = 20;
    //MOTO Dialer Code - IKHSS6-583 - End

    //MOT Calling Code - IKPIM-598
    public static void highlight(Spannable name, String matchStr, boolean isNumber,
            int matchOffset,int color, int text_color) {
        if(isNumber) {
            numHighlight(name, matchStr, matchOffset, color, text_color);
        } else {
            getHighlighter(name).nameHighlight(name, matchStr, color, text_color);
        }
    }
    //END MOT Calling Code - IKPIM-598
    abstract void nameHighlight(Spannable name, String matchStr, int color,
            int text_color);

    private static void numHighlight(Spannable name, String matchStr, int matchOffset,  //MOT Calling oode - IKMAIN-22581
            int color,int text_color) {
        boolean match = false;
        String str = name.toString();
        String orig = str.toUpperCase();
        String pat;
        int startPos = -1;
        int len = matchStr.length();

        // MOT Calling Code - IKMAIN-18641
        if (SmartDialerUtil.hasDigitsChar(matchStr)) {
            pat = PhoneNumberUtils.convertKeypadLettersToDigits(matchStr);
        }else{
            return; // only highlight number string when search string has dialable char(0~9).
        }
        // END MOT Calling Code - IKMAIN-18641

        int length = orig.length() - matchOffset - pat.length() + 1;
        for (int j = 0; j < length; j++) {
            // MOT Calling code - IKSTABLETWOV-189
            // add exception check
            try {
                if (orig.charAt(j+matchOffset) == pat.charAt(0)) {
                    match = true;
                    int currPos = j + matchOffset + 1;
                    for (int i = 1; i < pat.length(); i++) {
                        if (orig.charAt(currPos) != pat.charAt(i)) {
                            if (SmartDialerUtil.isSeparator(orig.charAt(currPos))) { // MOT Calling Code - IKMAIN-18641
                                len++;
                                i--;
                            } else {
                                match = false;
                                startPos = -1;
                                len = matchStr.length();
                                break;
                            }
                        }
                        currPos++;
                    }
                }
            } catch (StringIndexOutOfBoundsException e) {
                match = false;
                startPos = -1;
                len = matchStr.length();
            }
            // END - MOT Calling code - IKSTABLETWOV-189
            if (match) {
                startPos = j + matchOffset;
                break;
            }
        }
        if (startPos != -1) {
            if (str.length() >= (startPos + len)) {
                name.setSpan(new BackgroundColorSpan(color), startPos, startPos + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                name.setSpan(new ForegroundColorSpan(text_color), startPos, startPos + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);//MOT Calling Code -- IKSTABLEFOUR-2767
            }
        }
    }

    //MOT Calling Code - IKPIM-598
    private static Highlighter getHighlighter(Spannable name){
        Highlighter highlighter = null;
        int nameStyle = guessFullNameStyle(name.toString());
        if (nameStyle == FullNameStyle.KOREAN) {
            if(DBG) log("it's Korean name.");
            highlighter = KoreanHighlighter.getInstance();
        } else if ((nameStyle == FullNameStyle.CHINESE) || (nameStyle == FullNameStyle.CJK)){
            if(DBG) log("it's Chinese name.");
            highlighter = ChineseHighlighter.getInstance();
        } else if (nameStyle == FullNameStyle.WESTERN) {
            if(DBG) log("it's Western name.");
            highlighter = EnglishHighlighter.getInstance();
        } else {
            if(DBG) log("Not recognized name style, default use Western highlight.");
            highlighter = EnglishHighlighter.getInstance();
        }
        return highlighter;
    }

    private static int guessFullNameStyle(String name) {
        if (name.isEmpty()) {
            return FullNameStyle.UNDEFINED;
        }

        int nameStyle = FullNameStyle.UNDEFINED;
        int length = name.length();
        int offset = 0;
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);

                if (!isLatinUnicodeBlock(unicodeBlock)) {

                    if (isCJKUnicodeBlock(unicodeBlock)) {
                        // We don't know if this is Chinese, Japanese or Korean -
                        // trying to figure out by looking at other characters in the name
                        return guessCJKNameStyle(name, offset + Character.charCount(codePoint));
                    }

                    if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                        return FullNameStyle.JAPANESE;
                    }

                    if (isKoreanUnicodeBlock(unicodeBlock)) {
                        return FullNameStyle.KOREAN;
                    }
                }
                nameStyle = FullNameStyle.WESTERN;
            }
            offset += Character.charCount(codePoint);
        }
        return nameStyle;
    }

    private static boolean isLatinUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.BASIC_LATIN ||
                unicodeBlock == UnicodeBlock.LATIN_1_SUPPLEMENT ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_A ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_B ||
                unicodeBlock == UnicodeBlock.LATIN_EXTENDED_ADDITIONAL;
    }

    private static boolean isCJKUnicodeBlock(UnicodeBlock block) {
        return block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || block == UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || block == UnicodeBlock.CJK_RADICALS_SUPPLEMENT
                || block == UnicodeBlock.CJK_COMPATIBILITY
                || block == UnicodeBlock.CJK_COMPATIBILITY_FORMS
                || block == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT;
    }

    private static boolean isKoreanUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.HANGUL_SYLLABLES ||
                unicodeBlock == UnicodeBlock.HANGUL_JAMO ||
                unicodeBlock == UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }

    private static boolean isJapanesePhoneticUnicodeBlock(UnicodeBlock unicodeBlock) {
        return unicodeBlock == UnicodeBlock.KATAKANA ||
                unicodeBlock == UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS ||
                unicodeBlock == UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
                unicodeBlock == UnicodeBlock.HIRAGANA;
    }

    private static int guessCJKNameStyle(String name, int offset) {
        int length = name.length();
        while (offset < length) {
            int codePoint = Character.codePointAt(name, offset);
            if (Character.isLetter(codePoint)) {
                UnicodeBlock unicodeBlock = UnicodeBlock.of(codePoint);
                if (isJapanesePhoneticUnicodeBlock(unicodeBlock)) {
                    return FullNameStyle.JAPANESE;
                }
                if (isKoreanUnicodeBlock(unicodeBlock)) {
                    return FullNameStyle.KOREAN;
                }
            }
            offset += Character.charCount(codePoint);
        }

        return FullNameStyle.CJK;
    }

    protected static char coreLang2Alpha(char ch) {
        char mapCh = 0;
        String newCh = String.valueOf(ch);
        if (ch >= '0' && ch <= '9') {
            mapCh = ch;
        } else {
            String normalizer = Normalizer.normalize(newCh, Normalizer.Form.NFD);
            mapCh = normalizer.charAt(0);
        }
        return mapCh;
    }
    // MOT Calling Code - IKPIM-598

    public static void log(String msg) {
        Log.d(TAG, msg);
    }
}
