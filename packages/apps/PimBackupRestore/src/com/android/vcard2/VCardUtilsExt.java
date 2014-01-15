/*===============================================================================================
 *
 *  Module Name:  VCardUtils.java
 *
 *  General Description: Provides functionalities needed for Motorola features
 *
 *================================================================================================
 *                              Motorola Confidential Proprietary
 *                          Advanced Technology and Software Operations
 *                       (c) Copyright Motorola 2011, All Rights Reserved
 *
 * Revision History:
 *                            Modification     Tracking
 * Author                        Date          Number         Description of Changes
 *-------------------------   ------------    ----------      -------------------------------------
 * Glory Chen                 11/23/2011      IKHALFMWK-557   Initial Creation:Framework/HAL ICS VCard Upmerge
 * ------------------------------------------------------------------------------------------------
 */
package com.android.vcard2;

//BEGIN Motorola, w21678, 13/04/11, IKHALFMWK-320 / FID35684-Support Korean in VCard
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
//END Motorola, w21678, 13/04/11, IKHALFMWK-320

/**
 * Ext Utilities for VCard handling codes.
 *
 * {@hide}
 */
public class VCardUtilsExt {
    private static final String LOG_TAG = VCardConstants.LOG_TAG;

    private VCardUtilsExt() {
    }

    //BEGIN Motorola, w21678, 13/04/11, IKHALFMWK-320 / FID35684-Support Korean in VCard
    /**
     * (1byte)ascii ([\\x09\\x0A\\x0D\\x20-\\x7E])
     * @param an indexed byte to check
     * @return true/false
     */
    private static boolean isAscii(byte bytes) {
        return bytes == (byte)0x09
            || bytes == (byte)0x0A
            || bytes == (byte)0x0D
            || (bytes >= (byte)0x20
                && bytes <= (byte)0x7E
                );
    }

    /**
     * (2bytes) Korean straight (0xB0A1 ~ 0xC8FE : euc-kr korean range)
     * @param bytes bytes
     * @param index index
     * @return true/false
     */
    private static boolean isEucKrStraight(byte[] bytes, int index) {
        if(index + 1 >= bytes.length)
            return false;

        char tmp_char = 0;
        tmp_char = (char)(bytes[index] & 0xFF);
        tmp_char = (char)((tmp_char & 0x00FF) << 8);
        tmp_char |= (char)(bytes[index+1] & 0xFF);

        return ((tmp_char >= 0xB0A1) && (tmp_char <= 0xC8FE));
   }
   /**
    * Check given byte array is Korean Euc-Kr encoded character or not.
    *
    * @param bytes bytes
    * @return true/false
    */
    private static boolean isEucKrKorean(byte[] bytes) {
        boolean euckr_flag = false;
        for(int i=0; i < bytes.length;) {
            if(isAscii(bytes[i])) {
                i += 1; //Check 1 byte.
                continue;
            } else if (isEucKrStraight(bytes, i)) {
                i += 2; //Check 2 bytes.
                euckr_flag = true;
                continue;
            } else {
                return false;
            }
        }
        return euckr_flag; //if there are only ascii chars, then return false!
    }

    private static boolean isIncludeEucKr(String Code) { //special check for Hangul euc-kr handling.
        boolean result = false;
        byte[] codeBytes = null;
        try {
            codeBytes = Code.getBytes("iso-8859-1"); //To make byte array.
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "# isIncludeEucKr | getBytes error! ");
        }

        if(codeBytes != null) {
            result = isEucKrKorean(codeBytes);
        }

        return result;
    }
    //END Motorola, w21678, 13/04/11, IKHALFMWK-320

    // BEGIN Motorola, w21765, 22/09/2011, IKCBS-2373 / FID37602-Support SHIFT_JIS in vCard
    /**
      * (1byte)Katakana ([\\xA1-\\xDF])
      * @param byte an indexed byte to check
      * @return true/false
      */
    private static boolean isKatakana(byte bytes) {
        return (bytes >= (byte) 0xA1
                && bytes <= (byte) 0xDF
                );
    }

    /**
      * (2bytes) Japansese Shift-JIS straight.
      * (0x81 ~ 0x9F or 0xE0 ~ 0xEF : first byte range for Shift-JIS Japanese)
      * (0x40 ~ 0x7E or 0x80 ~ 0xFC : second byte range for Shift-JIS Japanese)
      *
      * @param bytes bytes
      * @param index index
      * @return true/false
      */
    private static boolean isShiftJISStraight(byte[] bytes, int index) {
        if (index + 1 >= bytes.length) {
            return false;
        }

        char first_byte = 0;
        char second_byte = 0;

        first_byte = (char)(bytes[index] & 0xFF);

        // if first byte is in Shift-JIS range, check second bytes's range.
        if (((first_byte >= 0x81) && (first_byte <= 0x9F)) ||
                ((first_byte >= 0xE0) && (first_byte <= 0xEF))) {

            second_byte = (char)(bytes[index + 1] & 0xFF);
            if (((second_byte >= 0x40) && (second_byte <= 0x7E)) ||
                    ((second_byte >= 0x80) && (second_byte <= 0xFC))) {
                return true;
            }
        }

        return false;
    }

    /**
      * Check given byte array is Japanese Shift-JIS encoded character or not.
      *
      * @param bytes bytes
      * @return true/false
      */
    private static boolean isShiftJISJapanese(byte[] bytes) {
        boolean shiftJIS_flag = false;
        for (int i = 0; i < bytes.length;) {
            if (isAscii(bytes[i])) {
                i += 1; //Check 1 byte.
                continue;
            } else if (isKatakana(bytes[i])) {
                i += 1; // Check 1 byte for Katakana half-width character.
                shiftJIS_flag = true;
                continue;
            }
            else if (isShiftJISStraight(bytes, i)) {
                i += 2; //Check 2 bytes.
                shiftJIS_flag = true;
                continue;
            } else {
                return false;
            }
        }

        //if there are only ascii chars, then return false!
        return shiftJIS_flag;
    }

    /**
      * Special check for Japanese Shift-JIS handling
      *
      * @param String String
      * @return true/false
      */
    private static boolean isIncludeShiftJIS(String Code) {
        boolean result = false;
        byte[] codeBytes = null;

        try {
            codeBytes = Code.getBytes("iso-8859-1"); //To make byte array.
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, "# isIncludeShiftJIS | getBytes error! ");
        }

        if (codeBytes != null) {
            result = isShiftJISJapanese(codeBytes);
        }

        return result;
    }
    // END IKCBS-2373

    /*
     * Get the most likely charset when Charset in a vCard file is empty.
     *
     */
    public static String getMissingCharset(final String value, String defaultCharset) {
        List<String> values = new ArrayList<String>();
        values.add(value);
        return getMissingCharset(values, defaultCharset);
    }
    public static String getMissingCharset(final List<String> values, String defaultCharset) {
        String targetCharset = defaultCharset;
        //BEGIN Motorola, w21678, 13/04/11, IKHALFMWK-320 / FID35684-Support Korean in VCard
        /*
         *  When Charset in a vCard field is empty, if there are other EUC strings in this field,
         *  below logic will have a known issue that such field will be wrongly decoded by EUC-KR.
         *  This is due to EUC-KR has coding overlap with EUC-CN, EUC-JP, and EUC-TW.
         */
        // BEGIN Motorola, w21667, 01/05/2012, IKMAIN-34902
        if (Arrays.asList(Locale.getAvailableLocales()).contains(Locale.KOREA) &&
            ("SKT".equalsIgnoreCase(android.os.Build.BRAND) ||
             "KT".equalsIgnoreCase(android.os.Build.BRAND)) ) {

            for (final String value : values) {
                if (isIncludeEucKr(value)) {
                    targetCharset = "euc-kr";
                    break;
                }
            }
        }
        // END Motorola, w21667, 01/05/2012, IKMAIN-34902
        //END Motorola, w21678, 13/04/11, IKHALFMWK-320

        // BEGIN Motorola, w21765, 22/09/2011, IKCBS-2373 / FID37602-Support SHIFT_JIS in vCard
        /*
         * When Charset in a vCard field is empty, whether Shift-JIS string exist or not by below logic,
         * If Charset is not defined and Japanese locale is supported,
         * will check encoding type to be set as Shift-JIS
         */
        // BEGIN Motorola, w21667, 01/05/2012, IKMAIN-34902
        else if (Arrays.asList(Locale.getAvailableLocales()).contains(Locale.JAPAN) &&
                 "KDDI".equalsIgnoreCase(android.os.Build.BRAND)) {
        // END Motorola, w21667, 01/05/2012, IKMAIN-34902)
            targetCharset = "SHIFT_JIS";

            for (final String value : values) {
                if (((value != null) && (value.length() > 0)) && !isIncludeShiftJIS(value)) {
                    targetCharset = defaultCharset;
                    break;
                }
            }
        }
        // END IKCBS-2373

        return targetCharset;
    }
}
