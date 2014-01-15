/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/04   IKCTXTAW-479    Initial version
 */

package com.motorola.android.wrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * A wrapper for {@link android.telephony.TelephonyManager} class.
 * All of its calls are delegated to the TelephonyManager class.
 * Can be subclassed to modify behavior for tesing or mocking purposes.
 */
public class TelephonyManagerWrapper {
    private static final String TAG = TelephonyManagerWrapper.class.getSimpleName();
    protected final TelephonyManager mTelephonyManager;

    // map of service state
    private static final HashMap<Integer, String> sServiceStates = new HashMap<Integer, String>();
    static {
        // populate the constant map using reflection
        Class<ServiceState> serviceStateClass = ServiceState.class;
        // get all the declared constants
        Field[] serviceStates = serviceStateClass.getDeclaredFields();
        if (serviceStates != null) {
            for (Field serviceState : serviceStates) {
                // make sure it's a public static final int as the documentation says
                if (serviceState.getType().equals(Integer.TYPE)
                        && Modifier.isPublic(serviceState.getModifiers())
                        && Modifier.isStatic(serviceState.getModifiers())
                        && Modifier.isFinal(serviceState.getModifiers()) ) {
                    try {
                        // map the int value to the constant name
                        sServiceStates.put(serviceState.getInt(null), serviceState.getName());
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Unable to read value for " + serviceState.getName());
                    } catch (IllegalAccessException e) {
                        Log.w(TAG, "Unable to access " + serviceState.getName());
                    }
                }
            }
        }
    }

    // map of network types
    private static final String NETWORK_TYPE_PREFIX = "NETWORK_TYPE_";
    private static final HashMap<Integer, String> sNetworkTypes = new HashMap<Integer, String>();
    static {
        // populate the constant map using reflection
        Class<TelephonyManager> telephonyClass = TelephonyManager.class;
        // get all the declared constants
        Field[] networkTypes = telephonyClass.getDeclaredFields();
        if (networkTypes != null) {
            for (Field networkType : networkTypes) {
                // make sure it's a public static final int as the documentation says
                if (networkType.getType().equals(Integer.TYPE)
                        && networkType.getName().startsWith(NETWORK_TYPE_PREFIX)
                        && Modifier.isPublic(networkType.getModifiers())
                        && Modifier.isStatic(networkType.getModifiers())
                        && Modifier.isFinal(networkType.getModifiers()) ) {
                    try {
                        // map the int value to the constant name
                        sNetworkTypes.put(networkType.getInt(null), networkType.getName());
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "Unable to read value for " + networkType.getName());
                    } catch (IllegalAccessException e) {
                        Log.w(TAG, "Unable to access " + networkType.getName());
                    }
                }
            }
        }
    }

    /**
     * @param serviceState service state as defined in ServiceState
     * @return name of the service state
     */
    public static String getServiceStateName(int serviceState) {
        return sServiceStates.get(serviceState);
    }

    /**
     * @param networkType network type as defined in TelephonyManager
     * @return name of the network type
     */
    public static String getNetworkTypeName(int networkType) {
        return sNetworkTypes.get(networkType);
    }

    public TelephonyManagerWrapper(Context context) {
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /**
     * {@link TelephonyManager#getCallState}
     */
    public int getCallState() {
        return mTelephonyManager.getCallState();
    }

    /**
     * {@link TelephonyManager#getCellLocation}
     */
    public CellLocation getCellLocation() {
        return mTelephonyManager.getCellLocation();
    }

    /**
     * {@link TelephonyManager#getDataActivity}
     */
    public int getDataActivity() {
        return mTelephonyManager.getDataActivity();
    }

    /**
     * {@link TelephonyManager#getDataState}
     */
    public int getDataState() {
        return mTelephonyManager.getDataState();
    }

    /**
     * {@link TelephonyManager#getDeviceId}
     */
    public String getDeviceId() {
        return mTelephonyManager.getDeviceId();
    }

    /**
     * {@link TelephonyManager#getDeviceSoftwareVersion}
     */
    public String getDeviceSoftwareVersion() {
        return mTelephonyManager.getDeviceSoftwareVersion();
    }

    /**
     * {@link TelephonyManager#getLine1Number}
     */
    public String getLine1Number() {
        return mTelephonyManager.getLine1Number();
    }

    /**
     * {@link TelephonyManager#getNeighboringCellInfo}
     */
    public List<NeighboringCellInfo> getNeighboringCellInfo() {
        return mTelephonyManager.getNeighboringCellInfo();
    }

    /**
     * {@link TelephonyManager#getNetworkCountryIso}
     */
    public String getNetworkCountryIso() {
        return mTelephonyManager.getNetworkCountryIso();
    }

    /**
     * {@link TelephonyManager#getNetworkOperator}
     */
    public String getNetworkOperator() {
        return mTelephonyManager.getNetworkOperator();
    }

    /**
     * {@link TelephonyManager#getNetworkOperatorName}
     */
    public String getNetworkOperatorName() {
        return mTelephonyManager.getNetworkOperatorName();
    }

    /**
     * {@link TelephonyManager#getNetworkType}
     */
    public int getNetworkType() {
        return mTelephonyManager.getNetworkType();
    }

    /**
     * {@link TelephonyManager#getPhoneType}
     */
    public int getPhoneType() {
        return mTelephonyManager.getPhoneType();
    }

    /**
     * {@link TelephonyManager#getSimCountryIso}
     */
    public String getSimCountryIso() {
        return mTelephonyManager.getSimCountryIso();
    }

    /**
     * {@link TelephonyManager#getSimOperator}
     */
    public String getSimOperator() {
        return mTelephonyManager.getSimOperator();
    }

    /**
     * {@link TelephonyManager#getSimOperatorName}
     */
    public String getSimOperatorName() {
        return mTelephonyManager.getSimOperatorName();
    }

    /**
     * {@link TelephonyManager#getSimSerialNumber}
     */
    public String getSimSerialNumber() {
        return mTelephonyManager.getSimSerialNumber();
    }

    /**
     * {@link TelephonyManager#getSimState}
     */
    public int getSimState() {
        return mTelephonyManager.getSimState();
    }

    /**
     * {@link TelephonyManager#getSubscriberId}
     */
    public String getSubscriberId() {
        return mTelephonyManager.getSubscriberId();
    }

    /**
     * {@link TelephonyManager#getVoiceMailAlphaTag}
     */
    public String getVoiceMailAlphaTag() {
        return mTelephonyManager.getVoiceMailAlphaTag();
    }

    /**
     * {@link TelephonyManager#getVoiceMailNumber}
     */
    public String getVoiceMailNumber() {
        return mTelephonyManager.getVoiceMailNumber();
    }

    /**
     * {@link TelephonyManager#hasIccCard}
     */
    public boolean hasIccCard() {
        return mTelephonyManager.hasIccCard();
    }

    /**
     * {@link TelephonyManager#isNetworkRoaming}
     */
    public boolean isNetworkRoaming() {
        return mTelephonyManager.isNetworkRoaming();
    }

    /**
     * {@link TelephonyManager#listen}
     */
    public void listen(PhoneStateListener listener, int events) {
        mTelephonyManager.listen(listener, events);
    }
}
