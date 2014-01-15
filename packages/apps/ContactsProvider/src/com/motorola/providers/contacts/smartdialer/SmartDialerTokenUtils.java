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

import java.lang.Character.UnicodeBlock;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.FullNameStyle;
import android.text.TextUtils;
import android.util.Log;

import com.android.providers.contacts.NameNormalizer;

/**
 * This utility class include some basic functions for smart-dialer token generation.
 * Any other language support need to inherit this class.
 *
 */

public abstract class SmartDialerTokenUtils {

    public static final String TAG = "SmartDialerTokenUtils";
    public static final boolean DBG = true;

    protected static final int INITIAL_OFFSET = 0;

    public static final int FULL_PARTIAL_NAME_FLAG = 0; // Indicate full/partial name token
    public static final int ACRONYM_NAME_FLAG = 1;      // Indicate acronym name token
    public static final int HANGUL_CONSONANTS = FullNameStyle.KOREAN;      // Indicate hangul consonant name token

    protected static final char CHAR_NULL = 0;

    // BEGIN Motorola, PIM Contacts, gfj763, 8/11/2011, IKPIM-697
    // language locale value
    private static final String CHINESE_LANGUAGE  = Locale.CHINESE.getLanguage().toLowerCase();   // locale = "zh"
    private static final String JAPANESE_LANGUAGE = Locale.JAPANESE.getLanguage().toLowerCase();  // locale = "ja"
    private static final String KOREAN_LANGUAGE   = Locale.KOREAN.getLanguage().toLowerCase();    // locale = "ko"
    // END Motorola, PIM Contacts, gfj763, 8/11/2011, IKPIM-697

    // Map corresponding alphabet to digits via Bell-Keypad
    protected static final char[] ALPHABET2NUM_TABLE = {
            '2', '2', '2',      // [2ABC]
            '3', '3', '3',      // [3DEF]
            '4', '4', '4',      // [4GHI]
            '5', '5', '5',      // [5JKL]
            '6', '6', '6',      // [6MNO]
            '7', '7', '7', '7', // [7PQRS]
            '8', '8', '8',      // [8TUV]
            '9', '9', '9', '9'  // [9WXYZ]
    };

    // BEGIN Motorola, PIM Contacts, gfj763, 8/11/2011, IKPIM-697
    /*
     * Map Western core languages Latin letter to bell-keypad digit
     */
    protected static final HashMap<String, String> sLatinNormalizerToBellPadDigitMap;
    static {
        sLatinNormalizerToBellPadDigitMap = new HashMap<String, String>();

        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("a"), "2");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("b"), "2");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("c"), "2");

        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("d"), "3");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("e"), "3");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("f"), "3");

        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("g"), "4");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("h"), "4");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("i"), "4");

        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("j"), "5");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("k"), "5");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("l"), "5");

        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("m"), "6");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("n"), "6");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("o"), "6");

        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("p"), "7");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("q"), "7");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("r"), "7");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("s"), "7");

        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("t"), "8");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("u"), "8");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("v"), "8");

        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("w"), "9");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("x"), "9");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("y"), "9");
        sLatinNormalizerToBellPadDigitMap.put(NameNormalizer.normalize("z"), "9");
    }

    /*
     * Map some special Western core languages Latin letters to bell-keypad digit
     */
    protected static final HashMap<String, String> sLatinExtendedToDigitMap;
    static {
        sLatinExtendedToDigitMap = new HashMap<String, String>();
        // French
        sLatinExtendedToDigitMap.put("\u00c6", "23"); // "AE"
        sLatinExtendedToDigitMap.put("\u00e6", "23"); // "ae"
        sLatinExtendedToDigitMap.put("\u0152", "63"); // "OE"
        sLatinExtendedToDigitMap.put("\u0153", "63"); // "oe"
        // German
        sLatinExtendedToDigitMap.put("\u00df", "77"); // 'Beta', map as "ss"
        // Turkish
        sLatinExtendedToDigitMap.put("\u0131", "4"); // "i"
    }

    /*
     * Convert core languages Latin string to digit token string
     */
    protected String coreLanAlpha2Digit(String str) {
        if (TextUtils.isEmpty(str)) {
            return null;
        }

        StringBuilder tokeStrBuilder = new StringBuilder();
        char[] charArray = str.toCharArray();
        for (char ch : charArray) {
            char mapch [] = coreLanAlpha2Digit(ch);
            if (CHAR_NULL != mapch[0]) {
                tokeStrBuilder.append(mapch);
            }
        }
        return tokeStrBuilder.toString();
    }

    /*
     * Map core languages Latin alphabet to digit
     */
    protected char [] coreLanAlpha2Digit(char ch) {
        char tkCh [] = {CHAR_NULL};

        String chStr = String.valueOf(ch);
        String normalizer = NameNormalizer.normalize(String.valueOf(ch));

        if (ch >= '0' && ch <= '9') {
            tkCh[0] = ch;
        } else if (sLatinNormalizerToBellPadDigitMap.containsKey(normalizer)) {
            tkCh = sLatinNormalizerToBellPadDigitMap.get(normalizer).toCharArray();
        } else if (sLatinExtendedToDigitMap.containsKey(chStr)) {
            tkCh = sLatinExtendedToDigitMap.get(chStr).toCharArray();
        }

        return tkCh;
    }
    // END Motorola, PIM Contacts, gfj763, 8/11/2011, IKPIM-697

    /*
     * Convert alphabet string to digital token string
     * Used in English token Utility
     */
    protected String alphabet2Num(String alphStr) {
        if (TextUtils.isEmpty(alphStr)) {
            return null;
        }

        StringBuilder tokeStrBuilder = new StringBuilder();
        char[] charArray = alphStr.toCharArray();
        for (char ch : charArray) {
            char mapch = alphabet2Num(ch);
            if (CHAR_NULL != mapch) {
                tokeStrBuilder.append(mapch);
            }
        }
        return tokeStrBuilder.toString();
    }

    /*
     * Map alphabet to digit
     * Now support 'A' - 'Z', 'a' - 'z', '0' - '9',
     * Used in English token Utility
     */
    protected char alphabet2Num(char alph) {
        char ch = CHAR_NULL;
        if (alph >= 'A' && alph <= 'Z') {
            ch = ALPHABET2NUM_TABLE[alph - 'A'];
        } else if (alph >= 'a' && alph <= 'z') {
            ch = ALPHABET2NUM_TABLE[alph - 'a'];
        } else if (alph >= '0' && alph <= '9') {
            ch = alph;
        }
        return ch;
    }

    /*
     * Filter all characters except 'A'-'Z', 'a'-'z', '0'-'9'
     * Used in English token Utility
     */
    protected String stripSeparators(String  str) throws PatternSyntaxException  {
        if (TextUtils.isEmpty(str)) {
            return null;
        }

        String regEx = "[^a-zA-Z0-9]";  // MOTOROLA MOD IKPIM-541
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(str);
        return  matcher.replaceAll("").trim();
    }

    /*
     * Filter the common characters those are not support for mapping
     * Now include characters " ()/-.,;*#+@$%&!\"':?"
     */
    protected String stripUnSupportCharacter(String  str) throws PatternSyntaxException  {
        if (TextUtils.isEmpty(str)) {
            return null;
        }

        String regEx = "[ ()-.,;*#+@$%&!\"':?/]";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(str);
        return  matcher.replaceAll("").trim();
    }


    /*
     * Filter all characters except '0'-'9', '*'
     * '*' is just used to match Korean consonant
     */
    public static String getFilterDigitsForMatch(String  str) {
        if(TextUtils.isEmpty(str)) {
            return null;
        }

        String regEx = "[^*0-9]";
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(str);
        return  matcher.replaceAll("").trim();
    }

    // BEGIN Motorola, PIM Contacts, gfj763, 8/11/2011, IKPIM-697
    /*
     * guess the name style based on the unicode
     */
    public static int guessFullNameStyle(String name) {
        if (name == null) {
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

    public static SmartDialerTokenUtils getSmartDialerInstance(Context context, String displayName, ContentValues values) {

        Locale locale = null;
        String language = null;
        locale = Locale.getDefault();
        Integer nameStyleFromNameParse = FullNameStyle.UNDEFINED;
        Integer nameStyleFromTable = FullNameStyle.UNDEFINED;
        boolean judgeFromTable = false;

        if (null != locale) {
            language = locale.getLanguage().toLowerCase();
    }

        Log.d(TAG, "Current locale language is: " + language);

        if (TextUtils.isEmpty(displayName)) {
            Log.w(TAG, "input name is null, unable to get corresponding name token instance !");
            return null;
        }

        if (null != values) {
            nameStyleFromTable = null;
            nameStyleFromTable = values.getAsInteger(StructuredName.FULL_NAME_STYLE);
            if (null != nameStyleFromTable ) {
                judgeFromTable = true;
            }
        }

        if (judgeFromTable) {
            if (FullNameStyle.WESTERN == nameStyleFromTable) {
                // A mix of Hanzi and latin chars are common in China, so we have to go through all names
                // to get proper name style if the name is not JANPANESE or KOREAN.
                Log.d(TAG, "name style from table is Western, need get adjust name style further.");

                nameStyleFromNameParse = guessFullNameStyle(displayName);

                if (FullNameStyle.WESTERN == nameStyleFromNameParse) {
                    Log.d(TAG, "adjusted name style is Western.");
                    return SmartDialerWesternCoreLanguageTokenUtils.getInstance();
                } else if (FullNameStyle.CJK == nameStyleFromNameParse) {
                    Log.d(TAG, "adjusted name style is Chinese.");
                    return SmartDialerChineseTokenUtils.getInstance();
                } else if (FullNameStyle.KOREAN == nameStyleFromNameParse) {
                    Log.d(TAG, "adjusted name style is Korean.");
                    return SmartDialerKoreanTokenUtils.getInstance();
                }  else if (FullNameStyle.JAPANESE == nameStyleFromNameParse) {
                    Log.d(TAG, "adjusted name style is Japanese, not support yet!");
                    return null;
                } else {
                    Log.d(TAG, "adjusted name style is not support : " + nameStyleFromNameParse);
                    return null;
                }
            } else if ( (FullNameStyle.CJK == nameStyleFromTable) ||
                        (FullNameStyle.CHINESE == nameStyleFromTable) ||
                        ((FullNameStyle.UNDEFINED == nameStyleFromTable) && (CHINESE_LANGUAGE.equals(language))) ) {
                Log.d(TAG, "name style is Chinese.");
                return SmartDialerChineseTokenUtils.getInstance();
            } else if ( (FullNameStyle.KOREAN == nameStyleFromTable) ||
                        ((FullNameStyle.UNDEFINED == nameStyleFromTable) && (KOREAN_LANGUAGE.equals(language))) ) {
                return SmartDialerKoreanTokenUtils.getInstance();
            } else if ( (FullNameStyle.JAPANESE == nameStyleFromTable) ||
                        ((FullNameStyle.UNDEFINED == nameStyleFromTable) && (JAPANESE_LANGUAGE.equals(language))) ) {
                Log.d(TAG, "name style is Japanese, not support yet!");
                return null;
            }
            }

        // if input table value is null
        nameStyleFromNameParse = guessFullNameStyle(displayName);
        if (FullNameStyle.WESTERN == nameStyleFromNameParse) {
            Log.d(TAG, "name style is Western.");
            return SmartDialerWesternCoreLanguageTokenUtils.getInstance();
        } else if (FullNameStyle.CJK == nameStyleFromNameParse) {
            Log.d(TAG, "name style is CJK.");
            return SmartDialerChineseTokenUtils.getInstance();
        } else if (FullNameStyle.KOREAN == nameStyleFromNameParse) {
            Log.d(TAG, "name style is Korean.");
            return SmartDialerKoreanTokenUtils.getInstance();
        }  else if (FullNameStyle.JAPANESE == nameStyleFromNameParse) {
            Log.d(TAG, "name style is Japanese, not support yet!");
            return null;
        } else {
            Log.d(TAG, "name style is not support : " + nameStyleFromNameParse);
            return null;
        }

    }
    // END Motorola, PIM Contacts, gfj763, 8/11/2011, IKPIM-697

    protected void log(String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }

    /*
     * This function will be override by sub-class to generate tokens
     */
    public abstract Iterator<Map.Entry<String, Integer>> getSmartDialerToken(String name);

}

// END Motorola, PIM Contacts, gfj763, 4/12/2011, IKPIM-436
