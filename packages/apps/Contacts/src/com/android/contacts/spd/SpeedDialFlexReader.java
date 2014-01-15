/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.android.contacts.spd;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import android.os.Environment;

import com.android.internal.util.XmlUtils;

/**
 * Parses and Reads flex file from /etc folder.
 *
 * MOT FID 35850-Flexing SDN's IKCBS-2075
 *
 * @author Jyothi Asapu - a22850
 */
public final class SpeedDialFlexReader {

    private static final ArrayList<ItemInfo> sdList = new ArrayList<ItemInfo>();
    private static final String TAG = "SpeedDialFlexRdr";

    /**
     * Returns speed dial list.
     *
     * @param fileName
     * @return
     */
    public static ArrayList<ItemInfo> getSpeedDialList(String fileName) {
        getFlexEntries(fileName);
        return sdList;
    }

    private static ArrayList<?> readFile(String str) {
        InputStream in = null;
        File fileName = new File(Environment.getRootDirectory(), str);

        try {
            if (fileName.exists()) {
                // Parsing the xml to get a dataset for the contacts information
                in = new FileInputStream(fileName);
                return XmlUtils.readListXml(in);
            }
        } catch (Exception e) {
            // Catch any failures with reading the data from xml and print a log
            // with trace
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private static void getFlexEntries(String fileName) {
        if (sdList.isEmpty()) {
            ArrayList<?> flexList = readFile(fileName);
            if (flexList == null) {
                return;
            }

            for (Object obj : flexList) {
                if (!(obj instanceof HashMap)) {
                    continue;
                }

                HashMap<String, String> item = (HashMap<String, String>) obj;
                ItemInfo sdn = new ItemInfo(item.get("number"),
                        item.get("lock"), item.get("digit"));

                sdList.add(sdn);
            }
        }
    }

    static class ItemInfo {
        private String mNum;
        private String mLockOrUnlock;
        private int mDialDigit;

        public ItemInfo(String num, String lock, String digit) {
            mNum = num;
            mLockOrUnlock = lock;
            mDialDigit = Integer.parseInt(digit);
        }

        public String getContactNum() {
            return mNum;
        }

        public String getLockOrUnLock() {
            return mLockOrUnlock;
        }

        public int getDialDigit() {
            return mDialDigit;
        }
    };
}
