/*
* (c) COPYRIGHT 2009-2011 MOTOROLA INC.
* MOTOROLA CONFIDENTIAL PROPRIETARY
* MOTOROLA Advanced Technology and Software Operations
*
* REVISION HISTORY:
* Author        Date       CR Number         Brief Description
* ------------- ---------- ----------------- ------------------------------
* e51141        2011/02/27 IKCTXTAW-201		   Initial version
*/

package com.motorola.contextual.virtualsensor.locationsensor;

import com.motorola.contextual.virtualsensor.locationsensor.LocationSensorApp.LSAppLog;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
*<code><pre>
* CLASS:
*   Util functions to handle Data Encryption/Decryption
*
* USAGE:
*   String crypto = EncryptUtils.encrypt(masterpassword, cleartext);
*   ...
*   String cleartext = EncryptUtils.decrypt(masterpassword, crypto);
*</pre></code>
*/

public class EncryptUtils {
    private final static String TAG = "LSAPP_Enc";
    private final static String SEED = "Ctx2011";
    private final static String HEX = "0123456789ABCDEF";
    private final static byte[] mRawKey = getRawKey(SEED.getBytes());

    /**
     * calcuate the seed's raw byte array.
     */
    private static byte[] getRawKey(byte[] seed) {
        byte[] raw = null;
        try {
            KeyGenerator kgen = KeyGenerator.getInstance("AES");
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(seed);
            kgen.init(128, sr); // 192 and 256 bits may not be available
            SecretKey skey = kgen.generateKey();
            raw = skey.getEncoded();
        } catch (Exception e) {
            LSAppLog.e(TAG, e.toString());
        }
        return raw;
    }

    /**
     * encrypt a string with its byte array using seed's raw byte array.
     * @param raw the seed's byte array
     * @param clear text's byte array
     * @return the encrypted string's byte array, or null if exception hppened.
     */
    private static byte[] encrypt(byte[] raw, byte[] clear) {
        byte[] encrypted = null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            encrypted = cipher.doFinal(clear);

        } catch (Exception e) {
            LSAppLog.e(TAG, e.toString());
        }
        return encrypted;
    }

    /**
     * decrypt a encrypto string using the seed's raw byte array.
     * @param raw the seed's byte array
     * @param encrypted text's byte array
     * @return the decrypted string's byte array, or null if exception hppened.
     */
    private static byte[] decrypt(byte[] raw, byte[] encrypted) {
        byte[] decrypted = null;
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            decrypted = cipher.doFinal(encrypted);
        } catch (Exception e) {
            LSAppLog.e(TAG, e.toString());
        }
        return decrypted;
    }


    /**
     * encrypt a string using this modules private SEED.
     * @param cleartext
     * @return encrypted string
     */
    public static String encrypt(String cleartext) {
        if (cleartext == null)
            return cleartext;

        byte[] result = encrypt(mRawKey, cleartext.getBytes());
        return toHex(result);
    }

    /**
     * decrypt an encrypted string using private SEED.
     * @param encrypted the encrypted string
     * @return decrypted clear text string
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null)
            return encrypted;
        byte[] enc = toByte(encrypted);
        byte[] result = decrypt(mRawKey, enc);
        if (result != null) {
            return new String(result);
        }
        return null;
    }

    /**
     * encrypted string with given seed string and clear text string.
     * @param seed  the private seed to encry
     * @param cleartext the clear text string to encrypt
     */
    public static String encrypt(String seed, String cleartext) {
        if (seed == null || cleartext == null)
            return null;

        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] result = encrypt(rawKey, cleartext.getBytes());
        return toHex(result);
    }

    /**
     * decrypt string with given seed string and the encrypted string.
     * @param seed  the private seed to decrypt
     * @param encrypted text to be decrypted to clear text
     */
    public static String decrypt(String seed, String encrypted) {
        if (seed == null || encrypted == null)
            return null;

        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] enc = toByte(encrypted);
        byte[] result = decrypt(rawKey, enc);
        if (result != null) {
            return new String(result);
        }
        return null;
    }


    /**
     * convert a string to byte[] array with its hex value
     * @param hexString guranteed to be not null
     */
    private static byte[] toByte(String hexString) {
        int len = hexString.length()/2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
        }
        return result;
    }

    /**
     * convert a byte buffer to string using its hex notation.
     */
    private static String toHex(byte[] buf) {
        if (buf == null)
            return "";

        StringBuffer result = new StringBuffer(2*buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }
    /*
     * for each byte, convert to HEX
     */
    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
    }

    static String toHex(String txt) {
        return toHex(txt.getBytes());
    }
    private static String fromHex(String hex) {
        return new String(toByte(hex));
    }
}
