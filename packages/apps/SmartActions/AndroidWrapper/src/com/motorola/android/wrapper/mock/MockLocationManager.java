/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * e51141                       06/26/12    IKCTXTAW-485    init version
 */

package com.motorola.android.wrapper.mock;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.GpsStatus.Listener;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;

import com.motorola.android.wrapper.LocationManagerWrapper;

import java.util.List;

/**
 * A mock {@link com.motorola.android.wrapper.LocationManagerWrapper} class.
 * All methods are non-functional and throw {@link java.lang.UnsupportedOperationException}.
 * You can use this to inject other dependencies, mocks, or monitors into the classes you are
 * testing.
 */

public class MockLocationManager extends LocationManagerWrapper {

    public MockLocationManager(Context context) {
        super(context);
    }

    @Override
    public boolean addGpsStatusListener(Listener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addNmeaListener(NmeaListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addProximityAlert(double latitude, double longitude, float radius, long expiration,
                                  PendingIntent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addTestProvider(String name, boolean requiresNetwork, boolean requiresSatellite,
                                boolean requiresCell, boolean hasMonetaryCost, boolean supportsAltitude,
                                boolean supportsSpeed, boolean supportsBearing, int powerRequirement, int accuracy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearTestProviderLocation(String provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearTestProviderEnabled(String provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearTestProviderStatus(String provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GpsStatus getGpsStatus(GpsStatus status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProviders(boolean enabledOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Location getLastKnownLocation(String provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAllProviders() {
        throw new UnsupportedOperationException();
    }

    @Override
    public LocationProvider getProvider(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isProviderEnabled(String provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeNmeaListener(NmeaListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeProximityAlert(PendingIntent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTestProvider(String provider) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUpdates(PendingIntent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria,
                                       LocationListener listener, Looper looper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria,
                                       PendingIntent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestLocationUpdates(String provider, long minTime, float minDistance,
                                       LocationListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestSingleUpdate(Criteria criteria, LocationListener listener, Looper looper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestSingleUpdate(Criteria criteria, PendingIntent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestSingleUpdate(String provider, LocationListener listener, Looper looper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestSingleUpdate(String provider, PendingIntent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean sendExtraCommand(String provider, String command, Bundle extras) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestLocationUpdates(String provider, long minTime, float minDistance,
                                       PendingIntent intent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void requestLocationUpdates(String provider, long minTime, float minDistance,
                                       LocationListener listener, Looper looper) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUpdates(LocationListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeGpsStatusListener(Listener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestProviderLocation(String provider, Location loc) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTestProviderEnabled(String provider, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}
