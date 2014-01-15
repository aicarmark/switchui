/*
 * Copyright (C) 2011/2012 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 *
 * Revision History:
 *                             Modification     Tracking
 * Author (core ID)                Date          Number     Description of Changes
 * -------------------------   ------------    ----------   ----------------------------------------
 * dfb746                      16/04/2012                   Initial release
 */

package com.motorola.mmsp.performancemaster.engine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

public class PhoneInfo extends InfoBase {
    private final static String TAG = "PhoneInfo";

    public static final int EXCELLENT = 0;

    public static final int GOOD = 1;

    public static final int NORMAL = 2;

    public static final int POOR = 3;

    public static final int UNKNOWN = 4;

    private int mSignalStrength;

    private int mSignalStrengthPercent;

    private int mSignalStrengthExtent = UNKNOWN;

    private String mModel;

    private String mNetworkOperator;

    private int mPhoneType;

    private int mServiceState;

    private int mNetworkType;

    private String mPhoneNum;

    private String mImsi;

    private String mImei;

    private String mMeid;

    private String mKernelVersion;

    private String mAndroidVersion;

    private String mBuildVersion;

    private TelephonyManager telephonyMgr = null;

    public PhoneInfo(Context context) {
        super();

        telephonyMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        MyPhoneStateListener phoneListener = new MyPhoneStateListener();
        telephonyMgr.listen(phoneListener,
                PhoneStateListener.LISTEN_SERVICE_STATE
                        | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        getStaticInfo();
    }

    private void getStaticInfo() {
        mModel = Build.MANUFACTURER + " (" + Build.MODEL + ")";        
        mKernelVersion = getFormattedKernelVersion();
        mAndroidVersion = Build.VERSION.RELEASE;
        mBuildVersion = Build.DISPLAY;
        
        getDynamicInfo();
    }
    
    private void getDynamicInfo()
    {
        mPhoneType = telephonyMgr.getPhoneType();
        if (mPhoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            mMeid = telephonyMgr.getDeviceId();
        } else if (mPhoneType == TelephonyManager.PHONE_TYPE_GSM) {
            mImei = telephonyMgr.getDeviceId();
            mImsi = telephonyMgr.getSubscriberId();
        }

        mNetworkType = telephonyMgr.getPhoneType();

        mPhoneNum = telephonyMgr.getLine1Number();
        if (mPhoneNum != null) {
            mPhoneNum = PhoneNumberUtils.formatNumber(mPhoneNum);
        }
        mNetworkOperator = telephonyMgr.getNetworkOperatorName();
    }

    private String getFormattedKernelVersion() {
        String procVersionStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
            try {
                procVersionStr = reader.readLine();
            } finally {
                reader.close();
            }

            final String PROC_VERSION_REGEX =
                    "\\w+\\s+" + /* ignore: Linux */
                    "\\w+\\s+" + /* ignore: version */
                    "([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
                    "\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /*
                                                                 * group 2:
                                                                 * (xxxxxx
                                                                 * @xxxxx.
                                                                 * constant)
                                                                 */
                    "\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
                    "([^\\s]+)\\s+" + /* group 3: #26 */
                    "(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
                    "(.+)"; /* group 4: date */

            Pattern p = Pattern.compile(PROC_VERSION_REGEX);
            Matcher m = p.matcher(procVersionStr);

            if (!m.matches()) {
                Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
                return "";
            } else if (m.groupCount() < 4) {
                Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount()
                        + " groups");
                return "";
            } else {
                return (new StringBuilder(m.group(1)).append("\n").append(
                        m.group(2)).append(" ").append(m.group(3)).append("\n")
                        .append(m.group(4))).toString();
            }
        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting kernel version for Device Info screen" + e);

            return "";
        }
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        public void onServiceStateChanged(ServiceState state) {
           mServiceState = state.getState();
           
           getDynamicInfo();
           onInfoUpdate();
           
           Log.e(TAG, "onServiceStateChanged()" + mServiceState);
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {

            int nTempSignalStrength = 0;

            if (signalStrength.isGsm()) // gsm
            {
                nTempSignalStrength = signalStrength.getGsmSignalStrength();

                int asu = (nTempSignalStrength == 99 ? -1 : nTempSignalStrength);
                if (asu != -1) {
                    nTempSignalStrength = -113 + (2 * asu);
                } else {
                    nTempSignalStrength = -1;
                }

                if (asu <= 2 || asu == 99) {
                    mSignalStrengthExtent = UNKNOWN;
                } else if (asu >= 12) {
                    mSignalStrengthExtent = EXCELLENT;
                } else if (asu >= 8) {
                    mSignalStrengthExtent = GOOD;
                } else if (asu >= 5) {
                    mSignalStrengthExtent = NORMAL;
                } else {
                    mSignalStrengthExtent = POOR;
                }

                int nAdjustAsu = asu - 2;
                nAdjustAsu = (nAdjustAsu == 99) ? 0 :
                           ((nAdjustAsu == 2) ? 0 : (nAdjustAsu));
                nAdjustAsu = (nAdjustAsu >= 28) ? 28 : nAdjustAsu;
                mSignalStrengthPercent = Math.abs((int) nAdjustAsu * 100 / (30 - 2));
            } else {
                int nCdmaRssi, nEvdoRssi;
                if (TelephonyManager.NETWORK_TYPE_CDMA == telephonyMgr.getNetworkType()) // cdma
                {
                    nCdmaRssi = signalStrength.getCdmaDbm();

                    nTempSignalStrength = nCdmaRssi;

                    if (nTempSignalStrength >= -75) {
                        mSignalStrengthExtent = EXCELLENT;
                    } else if (nTempSignalStrength >= -85) {
                        mSignalStrengthExtent = GOOD;
                    } else if (nTempSignalStrength >= -95) {
                        mSignalStrengthExtent = NORMAL;
                    } else if (nTempSignalStrength >= -100) {
                        mSignalStrengthExtent = POOR;
                    } else {
                        mSignalStrengthExtent = UNKNOWN;
                    }

                    int nPresent = 100 + nTempSignalStrength;
                    nPresent = (nPresent >= 40) ? 40 : (nPresent);
                    mSignalStrengthPercent = Math.abs((int) nPresent * 100 / 40);
                } else { // evdo
                    nEvdoRssi = signalStrength.getEvdoDbm();

                    nTempSignalStrength = nEvdoRssi;

                    if (nTempSignalStrength >= -65) {
                        mSignalStrengthExtent = EXCELLENT;
                    } else if (nTempSignalStrength >= -75) {
                        mSignalStrengthExtent = GOOD;
                    } else if (nTempSignalStrength >= -90) {
                        mSignalStrengthExtent = NORMAL;
                    } else if (nTempSignalStrength >= -105) {
                        mSignalStrengthExtent = POOR;
                    } else {
                        mSignalStrengthExtent = UNKNOWN;
                    }

                    int nPresent = 105 + nTempSignalStrength;
                    nPresent = (nPresent >= 60) ? 60 : (nPresent);
                    mSignalStrengthPercent = Math.abs((int) nPresent * 100 / 60);
                }
            }

            mSignalStrength = nTempSignalStrength;

            if (mServiceState != ServiceState.STATE_IN_SERVICE) // no network,
                                                                // report null
            {
                mSignalStrengthExtent = UNKNOWN;
                mSignalStrengthPercent = 0;
                mSignalStrength = 0;
            }

            onInfoUpdate();

            Log.e(TAG, "enter onSignalStrengthsChanged");
        }
    }

    public int getSignalStrength() {
        return mSignalStrength;
    }

    public int getSignalStrengthPercent() {
        return mSignalStrengthPercent;
    }

    public int getSignalStrengthExtent() {
        return mSignalStrengthExtent;
    }

    public String getModel() {
        return mModel;
    }

    public String getNetworkOperator() {
        return mNetworkOperator;
    }

    public int getPhoneType() {
        return mPhoneType;
    }

    public int getNetworkType() {
        return mNetworkType;
    }

    public int getServiceState() {
        return mServiceState;
    }

    public String getPhoneNum() {
        return mPhoneNum;
    }

    public String getMeid() {
        return mMeid;
    }

    public String getImsi() {
        return mImsi;
    }

    public String getImei() {
        return mImei;
    }

    public String getKernelVersion() {
        return mKernelVersion;
    }

    public String getAndroidVersion() {
        return mAndroidVersion;
    }

    public String getBuildVersion() {
        return mBuildVersion;
    }

    @Override
    public String toString() {
        return "PhoneInfo: " + mSignalStrengthPercent + "/" + this.mSignalStrengthExtent + "/"
                + this.mSignalStrength + "/" + this.mServiceState + "/" + this.mImsi + "/"
                + this.mModel + "/" + this.mNetworkOperator + "/" + this.mPhoneType + "/"
                + this.mNetworkType + "/" + "/" + this.mPhoneNum + "/"
                + this.mKernelVersion + "/" + this.mAndroidVersion + "/" + this.mBuildVersion;
    }
}
