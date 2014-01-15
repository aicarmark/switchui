/*
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor;

import java.util.ArrayList;
import java.util.List;

import android.hardware.SensorManager;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *<code><pre>
 * CLASS:
 * Class representing a sensor. Use {@link SensorManager#getSensorList}
 * to get the list of available Sensors.
 *
 * RESPONSIBILITIES:
 *
 * COLABORATORS:
 *	Virtual Sensor Manager
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public final class LocationSensor implements Parcelable {

    public interface VSensorType {
        public static final String TYPE = "locationsensor";
    }

    public static enum VIRT_SENSOR_TYPE {
        VS_TYPE_LOCATION
    };

    private String mName;

    // keep an static array of all location sensors
    private static List<LocationSensor> mLocationSensors = new ArrayList<LocationSensor>();

    private LocationSensor(VIRT_SENSOR_TYPE type) {
        mName = "Sensor";
    }

    /**
     * static factory
     */
    public static synchronized LocationSensor newLocationSensor(VIRT_SENSOR_TYPE type) {
        LocationSensor sensor = new LocationSensor(type);
        mLocationSensors.add(sensor);
        return sensor;
    }

    /**
     * Parcelable interface to wrap location info into parcelable to pass between processes.
     */
    public static final Parcelable.Creator<LocationSensor> CREATOR = new Parcelable.Creator<LocationSensor>() {
        public LocationSensor createFromParcel(Parcel in) {
            return new LocationSensor(in);
        }

        public LocationSensor[] newArray(int size) {
            return new LocationSensor[size];
        }
    };

    private LocationSensor(Parcel in) {
        mName = in.readString();
    }

    /**
     * Not Applicable to Location info
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Parcelable interface to wrap location info into parcelable to pass between processes.
     */
    public void writeToParcel(Parcel p, int arg) {
        p.writeInt(arg);
    }

    /**
     * @return name string of the virtual sensor.
     */
    public String getName() {
        return mName;
    }
}
