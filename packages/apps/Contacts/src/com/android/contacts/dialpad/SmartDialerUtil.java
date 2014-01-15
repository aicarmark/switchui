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
 * @author htf768
 */

//MOT calling code- IKMAIN-4172
package com.android.contacts.dialpad;

// MOT Calling Code - IKMAIN-18641
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
//END MOT Calling Code - IKMAIN-18641
import android.util.Log;
import java.lang.Character;

public class SmartDialerUtil {
    private static final String TAG   = "SmartDialerUtil";
    //private static final boolean DBG  = DialerApp.DBG;
    //private static final boolean VDBG = DialerApp.VDBG;
    private static final boolean DBG  = true;
    private static final boolean VDBG  = true;

    /* In dialer, 3 kinds of chars can be input in the dialer edit box,
     * 1. digits: 0~9*
     * 2. alpha char: a~z, A~Z
     * 3. separator: space()/-.,;#+
     */
    public static boolean isSeparator(char ch) {
        // add ', ; # +' as separator
        return (ch == ' ') || (ch == '(') || (ch == '/') || (ch == ')')
                || (ch == '-') || (ch == '.') || (ch == ',') || (ch == ';')
                || (ch == '#') || (ch == '+');// MOT Calling Code - IKMAIN-18641 IKPIM-598
    }

    public static String stripSeparators(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        int len = phoneNumber.length();
        StringBuilder ret = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = phoneNumber.charAt(i);
            if (!isSeparator(c)) {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    // MOT Calling Code - IKMAIN-18641
    /* decide if the string contains digit  0-9
     * if the string contains digit, return true
     * if the string only contains alphabet or separator, return false
     */
    public static boolean hasDigitsChar(final String str) {
        if (TextUtils.isEmpty(str)) {
            return false;
        }
        char[] tempArray = str.toCharArray();
        for (char ch : tempArray) {
            if (((ch >= '0') && (ch <= '9'))) {
                return true;
            }
        }
        return false;
    }

    /* decide if the string contains alpha char a-z, A-Z
     * if the string only contains digits or separator, return true
     * if the string has alpha char, return false
     */
    public static boolean noAlphaChar(final String str) {
        if (TextUtils.isEmpty(str)) {
            return true;
        }
        char[] tempArray = str.toCharArray();
        for (char ch : tempArray) {
            if (((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    public static final String CHARACTERS = "0123456789#*+,;";

    static class AlphaDialableInputFilter implements InputFilter {
        protected char[] getAcceptedChars() {
            return CHARACTERS.toCharArray();
        }

        public CharSequence filter(CharSequence source, int start, int end,
                Spanned dest, int dstart, int dend) {
            char[] accept = getAcceptedChars();
            boolean filter = false;

            int i;
            for (i = start; i < end; i++) {
                if (!ok(accept, source.charAt(i))) {
                    break;
                }
            }

            if (i == end) {
                // It was all OK.
                return null;
            }

            if (end - start == 1) {
                // It was not OK, and there is only one char, so nothing remains,
                return "";
            }

            SpannableStringBuilder filtered =
                new SpannableStringBuilder(source, start, end);
            i -= start;
            end -= start;

            int len = end - start;
            // Only count down to i because the chars before that were all OK.
            for (int j = end - 1; j >= i; j--) {
                if (!ok(accept, source.charAt(j))) {
                    filtered.delete(j, j + 1);
                }
            }
            return filtered;
        }

        protected static boolean ok(char[] accept, char c) {
            for (int i = accept.length - 1; i >= 0; i--) {
                if (accept[i] == c) {
                    return true;
                }
            }
            return false;
        }
    }
    // END - MOT Calling Code - IKMAIN-18641

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
