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

import android.os.Parcel;
import android.os.Parcelable;

/**
 *<code><pre>
 * CLASS:
 *  This class encapsulates location state into a Parcelable so it can be passed across the processes.
 *  e.g., sent from location sensor to virtual sensor manager.
 *
 * RESPONSIBILITIES:
 *   implements Parcelable interface for passing between processes.
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */

public final class LocationSensorState implements Parcelable {

    private StatusCode mStatusCode;
    private String mExtendedStatus;

    public static enum StatusCode {
        VS_STATE_ON,
        VS_STATE_OFF,
        VS_STATE_NEARBY,
        VS_STATE_FAR,
        VS_STATE_UNKNOWN;

        public static StatusCode fromValue(int value) {
            return StatusCode.values()[value];
        }

    };

    /**
     * Constructor instantiates this class from a supplied parcel
     *
     * @param in
     */
    private LocationSensorState(Parcel in) {
        mStatusCode = StatusCode.fromValue(in.readInt());
        mExtendedStatus = in.readString();
    }


    /**
     * Constructor instantiates this class from the list of all variables.
     *
     * @param status
     * @param ext
     */
    public LocationSensorState(StatusCode status, String ext) {
        mStatusCode = status;
        mExtendedStatus = ext;
    }

    /**
     * Parcelable interface
     */
    public static final Parcelable.Creator<LocationSensorState> CREATOR = new Parcelable.Creator<LocationSensorState>() {
        public LocationSensorState createFromParcel(Parcel in) {
            return new LocationSensorState(in);
        }

        public LocationSensorState[] newArray(int size) {
            return new LocationSensorState[size];
        }
    };

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mStatusCode.ordinal());
        dest.writeString(mExtendedStatus);
    }

    public int describeContents() {
        return 0;
    }

    /**
     * Get a status code with a simple description.
     * @return
     */
    public StatusCode getStatusCode() {
        return mStatusCode;
    }

    /**
     * Get an extended description of the status.  Optional, and will be an empty string if not used.
     * @return
     */
    public String getExtendedStatus() {
        return mExtendedStatus;
    }

}
