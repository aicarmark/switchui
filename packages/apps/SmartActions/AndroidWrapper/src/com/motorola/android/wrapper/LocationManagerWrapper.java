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

package com.motorola.android.wrapper;

import android.app.PendingIntent;
import android.content.Context;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.GpsStatus.NmeaListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;

import java.util.List;

/**
 * A wrapper for {@link android.location.LocationManager} class.
 * All of its calls are delegated to the LocationManager class.
 * Can be subclassed to modify behavior for tesing or mocking purposes.
 */
public class LocationManagerWrapper {

    protected final LocationManager mLocationManager;

    /**
     * the context here is the android platform context
     */
    public LocationManagerWrapper(Context ctx) {
        mLocationManager = (LocationManager)ctx.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * {@link LocationManager#addGpsStatusListener}
     * @param listener
     * @return
     */
    public boolean  addGpsStatusListener(GpsStatus.Listener listener) {
        return mLocationManager.addGpsStatusListener(listener);
    }

    /**
     *  {@link LocationManager#addNmeaListener}
     * @param listener
     */
    public boolean addNmeaListener(NmeaListener listener) {
        return mLocationManager.addNmeaListener(listener);
    }

    /**
     * {@link LocationManager#addProximityAlert}
     * @param latitude
     * @param longitude
     * @param radius
     * @param expiration
     * @param intent
     */
    public void addProximityAlert(double latitude, double longitude, float radius, long expiration, PendingIntent intent) {
        mLocationManager.addProximityAlert(latitude, longitude, radius, expiration, intent);
    }

    /**
     * {@link LocationManager#addTestProvider}
     * @param name
     * @param requiresNetwork
     * @param requiresSatellite
     * @param requiresCell
     * @param hasMonetaryCost
     * @param supportsAltitude
     * @param supportsSpeed
     * @param supportsBearing
     * @param powerRequirement
     * @param accuracy
     */
    public void addTestProvider(String name, boolean requiresNetwork, boolean requiresSatellite, boolean requiresCell, boolean hasMonetaryCost, boolean supportsAltitude, boolean supportsSpeed, boolean supportsBearing, int powerRequirement, int accuracy) {
        mLocationManager.addTestProvider(name, requiresNetwork, requiresSatellite, requiresCell, hasMonetaryCost, supportsAltitude, supportsSpeed, supportsBearing, powerRequirement, accuracy);
    }

    /**
     * {@link LocationManager#clearTestProviderLocation}
     * @param provider
     */
    public void  clearTestProviderLocation(String provider) {
        mLocationManager.clearTestProviderEnabled(provider);
    }

    /**
     * {@link LocationManager#clearTestProviderEnabled}
     * @param provider
     */
    public void clearTestProviderEnabled(String provider) {
        mLocationManager.clearTestProviderEnabled(provider);
    }

    /**
     * {@link LocationManager#clearTestProviderStatus}
     * @param provider
     */
    public void clearTestProviderStatus(String provider) {
        mLocationManager.clearTestProviderStatus(provider);
    }

    /**
     * {@link LocationManager#getBestProvider}
     * @param criteria
     * @param enabledOnly
     * @return provider
     */
    public String getBestProvider(Criteria criteria, boolean enabledOnly) {
        return mLocationManager.getBestProvider(criteria, enabledOnly);
    }

    /**
     * {@link LocationManager#getGpsStatus}
     * @param status
     * @return
     */
    public GpsStatus getGpsStatus(GpsStatus status) {
        return mLocationManager.getGpsStatus(status);
    }

    /**
     * {@link LocationManager#getProviders}
     * @param enabledOnly
     * @return
     */
    public List<String> getProviders(boolean enabledOnly) {
        return mLocationManager.getProviders(enabledOnly);
    }

    /**
     * {@link LocationManager#getBestProviders}
     * @param criteria
     * @param enabledOnly
     * @return
     */
    public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
        return mLocationManager.getProviders(criteria, enabledOnly);
    }

    /**
     * {@link LocationManager#getLastKnownLocation}
     * @param provider
     * @return
     */
    public Location getLastKnownLocation(String provider) {
        return mLocationManager.getLastKnownLocation(provider);
    }

    /**
     *  {@link LocationManager#getAllProviders}
     */
    public List<String> getAllProviders() {
        return mLocationManager.getAllProviders();
    }

    /**
     *  {@link LocationManager#getProvider}
     */
    public LocationProvider getProvider(String name) {
        return mLocationManager.getProvider(name);
    }

    /**
     * {@link LocationManager#isProviderEnabled}
     */
    public boolean isProviderEnabled(String provider) {
        return mLocationManager.isProviderEnabled(provider);
    }

    /**
     * {@link LocationManager#removeNmeaListener}
     * @param listener
     */
    public void removeNmeaListener(NmeaListener listener) {
        mLocationManager.removeNmeaListener(listener);
    }

    /**
     * {@link LocationManager#removeProximityAlert}
     * @param intent
     */
    public void removeProximityAlert(PendingIntent intent) {
        mLocationManager.removeProximityAlert(intent);
    }

    /**
     * {@link LocationManager#removeTestProvider}
     * @param provider
     */
    public void removeTestProvider(String provider) {
        mLocationManager.removeTestProvider(provider);
    }

    /**
     * {@link LocationManager#removeUpdates}
     * @param intent
     */
    public void removeUpdates(PendingIntent intent) {
        mLocationManager.removeUpdates(intent);
    }

    /**
     * {@link LocationManager#requestLocationUpdates}
     * @param minTime
     * @param minDistance
     * @param criteria
     * @param listener
     * @param looper
     */
    public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria,
                                       LocationListener listener, Looper looper) {
        mLocationManager.requestLocationUpdates(minTime, minDistance, criteria, listener, looper);
    }

    /**
     *  {@link LocationManager#requestLocationUpdates}
     * @param minTime
     * @param minDistance
     * @param criteria
     * @param intent
     */
    public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria,
                                       PendingIntent intent) {
        mLocationManager.requestLocationUpdates(minTime, minDistance, criteria, intent);
    }

    /**
     *  {@link LocationManager#requestLocationUpdates}
     * @param provider
     * @param minTime
     * @param minDistance
     * @param listener
     */
    public void requestLocationUpdates(String provider, long minTime, float minDistance,
                                       LocationListener listener) {
        mLocationManager.requestLocationUpdates(provider, minTime, minDistance, listener);
    }

    /**
     *  {@link LocationManager#requestLocationUpdates}
     * @param criteria
     * @param listener
     * @param looper
     */
    public void requestSingleUpdate(Criteria criteria, LocationListener listener, Looper looper) {
        mLocationManager.requestSingleUpdate(criteria, listener, looper);
    }

    /**
     *  {@link LocationManager#requestSingleUpdate}
     * @param criteria
     * @param intent
     */
    public void requestSingleUpdate(Criteria criteria, PendingIntent intent) {
        mLocationManager.requestSingleUpdate(criteria, intent);
    }

    /**
     *  {@link LocationManager#requestSingleUpdate}
     * @param provider
     * @param listener
     * @param looper
     */
    public void requestSingleUpdate(String provider, LocationListener listener, Looper looper) {
        mLocationManager.requestSingleUpdate(provider, listener, looper);
    }

    /**
     *  {@link LocationManager#requestSingleUpdate}
     * @param provider
     * @param intent
     */
    public void requestSingleUpdate(String provider, PendingIntent intent) {
        mLocationManager.requestSingleUpdate(provider, intent);
    }

    /**
     *  {@link LocationManager#sendExtraCommand}
     * @param provider
     * @param command
     * @param extras
     * @return
     */
    public boolean sendExtraCommand(String provider, String command, Bundle extras) {
        return mLocationManager.sendExtraCommand(provider, command, extras);
    }

    /**
     *  {@link LocationManager#setTestProviderStatus}
     * @param provider
     * @param status
     * @param extras
     * @param updateTime
     */
    public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime) {
        mLocationManager.setTestProviderStatus(provider, status, extras, updateTime);
    }

    /**
     * {@link LocationManager#requestLocationUpdates}
     * @param provider
     * @param minTime
     * @param minDistance
     * @param intent
     */
    public void requestLocationUpdates(String provider, long minTime, float minDistance, PendingIntent intent) {
        mLocationManager.requestLocationUpdates(provider, minTime, minDistance, intent);
    }

    /**
     * {@link LocationManager#requestLocationUpdates}
     * @param provider
     * @param minTime
     * @param minDistance
     * @param listener
     * @param looper
     */
    public void requestLocationUpdates(String provider, long minTime, float minDistance, LocationListener listener, Looper looper) {
        mLocationManager.requestLocationUpdates(provider, minTime, minDistance, listener, looper);
    }

    /**
     * {@link LocationManager#removeUpdates}
     * @param listener
     */
    public void removeUpdates(LocationListener listener) {
        mLocationManager.removeUpdates(listener);
    }

    /**
     * {@link LocationManager#removeGpsStatusListener}
     * @param listener
     */
    public void removeGpsStatusListener(GpsStatus.Listener listener) {
        mLocationManager.removeGpsStatusListener(listener);
    }

    /**
     * {@link LocationManager#setTestProviderLocation}
     * @param provider
     * @param loc
     */
    public void setTestProviderLocation(String provider, Location loc) {
        mLocationManager.setTestProviderLocation(provider, loc);
    }

    /**
     * {@link LocationManager#setTestProviderEnabled}
     * @param provider
     * @param enabled
     */
    public void setTestProviderEnabled (String provider, boolean enabled) {
        mLocationManager.setTestProviderEnabled(provider, enabled);
    }


}
