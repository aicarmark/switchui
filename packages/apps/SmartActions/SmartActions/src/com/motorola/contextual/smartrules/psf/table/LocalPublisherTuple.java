/*
 * @(#)LocalPublisherTuple.java
 *
 * (c) COPYRIGHT 2009-2012 MOTOROLA MOBILITY INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034        2012/03/21                   Initial version
 *
 */
package com.motorola.contextual.smartrules.psf.table;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

/**
* LocalPublisherTuple - Represents a row of the LocalPublisherTable
* CLASS:
*     LocalPublisherTuple
*
* RESPONSIBILITIES:
*  Abstracts a row of the LocalPublisherTable
*
* USAGE:
*     See each method.
*
*/
public class LocalPublisherTuple {


    private String mPublisherKey, mPackageName, mDescription;
    private String mActivityIntent, mNewState, mNewLabel, mType;
    private String mMarketLink;
    private String mBatteryDrain, mDataUsage, mResponseLatency, mSettingsAction;
    private int mStateType;
    private double mInterfaceVersion, mPublisherVersion;
    private int mShare, mBlackList;
    private Drawable mIcon;

    /**
     * Constructor to create LocalPublisherTuple from individual column values
     * @param publisherKey
     * @param pacakgeName
     * @param description
     * @param activityIntent
     * @param newState
     * @param newLabel
     * @param type
     * @param marketLink
     * @param stateType
     * @param batteryDrain
     * @param dataUsage
     * @param responseLatency
     * @param share
     * @param blackList
     * @param icon
     */
    public LocalPublisherTuple(String publisherKey, String pacakgeName, String description,
                               String activityIntent, String newState, String newLabel, String type,
                               String marketLink, String stateType,
                               String batteryDrain, String dataUsage, String responseLatency, String settingsAction,
                               int share, int blackList, Drawable icon,
                               float interfaceVersion, float publisherVersion) {
        setPublisherKey(publisherKey);
        setPackageName(pacakgeName);
        setDescription(description);
        setActivityIntent(activityIntent);
        setNewState(newState);
        setNewLabel(newLabel);
        setType(type);
        setMarketLink(marketLink);
        setStateType(stateType);
        setBatteryDrain(batteryDrain);
        setDataUsage(dataUsage);
        setResponseLatency(responseLatency);
        setSettingsAction(settingsAction);
        setSharability(share);
        blackListPublisher(blackList);
        setDrawable(icon);
        setInterfaceVersion(interfaceVersion);
        setPublisherVersion(publisherVersion);
    }

    /**
     * Constructor to create a LocalPublisherTuple from a cursor
     * @param cursor
     */
    public LocalPublisherTuple(Cursor cursor) {
        this(cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.PUBLISHER_KEY)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.PACKAGE)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.DESCRIPTION)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.ACTIVITY_INTENT)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.NEW_CONFIG_SUPPORT)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.NEW_CONFIG_LABEL)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.TYPE)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.MARKET_LINK)),
             (cursor.getInt(cursor.getColumnIndex(LocalPublisherTable.Columns.STATE_TYPE)) ==
              LocalPublisherTable.Modality.STATEFUL ? LocalPublisherTable.PkgMgr.Type.STATEFUL :
              LocalPublisherTable.PkgMgr.Type.STATELESS),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.BATTERY_DRAIN)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.DATA_USAGE)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.RESPONSE_LATENCY)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherTable.Columns.SETTINGS_ACTION)),
             cursor.getInt(cursor.getColumnIndex(LocalPublisherTable.Columns.SHARE)),
             cursor.getInt(cursor.getColumnIndex(LocalPublisherTable.Columns.BLACKLIST)),
             null,
             cursor.getFloat(cursor.getColumnIndex(LocalPublisherTable.Columns.INTERFACE_VERSION)),
             cursor.getFloat(cursor.getColumnIndex(LocalPublisherTable.Columns.PUBLISHER_VERSION)));

        byte[] buf = cursor.getBlob(cursor.getColumnIndex(LocalPublisherTable.Columns.ICON));
        if (buf != null) {
            ByteArrayInputStream is = new ByteArrayInputStream(buf);
            if (is != null) {
                Drawable icon = Drawable.createFromStream(is, "");
                setDrawable(icon);
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Returns Contentvalue for LocalPublisherTuple
     * @return - ContentValue
     */
    public ContentValues getAsContentValues() {
        ContentValues value = new ContentValues();
        value.put(LocalPublisherTable.Columns.PUBLISHER_KEY, mPublisherKey);
        value.put(LocalPublisherTable.Columns.PACKAGE, mPackageName);
        value.put(LocalPublisherTable.Columns.DESCRIPTION, mDescription);
        value.put(LocalPublisherTable.Columns.ACTIVITY_INTENT, mActivityIntent);
        value.put(LocalPublisherTable.Columns.NEW_CONFIG_SUPPORT, mNewState);
        value.put(LocalPublisherTable.Columns.NEW_CONFIG_LABEL, mNewLabel);
        value.put(LocalPublisherTable.Columns.TYPE, mType);
        value.put(LocalPublisherTable.Columns.MARKET_LINK, mMarketLink);
        value.put(LocalPublisherTable.Columns.STATE_TYPE, mStateType);
        value.put(LocalPublisherTable.Columns.BATTERY_DRAIN, mBatteryDrain);
        value.put(LocalPublisherTable.Columns.DATA_USAGE, mDataUsage);
        value.put(LocalPublisherTable.Columns.RESPONSE_LATENCY, mResponseLatency);
        value.put(LocalPublisherTable.Columns.SETTINGS_ACTION, mSettingsAction);
        value.put(LocalPublisherTable.Columns.SHARE, mShare);
        value.put(LocalPublisherTable.Columns.BLACKLIST, mBlackList);
        value.put(LocalPublisherTable.Columns.INTERFACE_VERSION, mInterfaceVersion);
        value.put(LocalPublisherTable.Columns.PUBLISHER_VERSION, mPublisherVersion);

        if (mIcon != null) {
            Bitmap bitmap = ((BitmapDrawable)mIcon).getBitmap();
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

            if (bitmap != null && bitmap.compress(CompressFormat.PNG, 100, byteArray)) {
                value.put(LocalPublisherTable.Columns.ICON, byteArray.toByteArray());
            }
        }

        return value;
    }


    /**
     * @return the interfaceVersion
     */
    public double getInterfaceVersion() {
        return mInterfaceVersion;
    }

    /**
     * @param interfaceVersion the interfaceVersion to set
     */
    public void setInterfaceVersion(double interfaceVersion) {
        this.mInterfaceVersion = interfaceVersion;
    }

    /**
     * @return the publisherVersion
     */
    public double getPublisherVersion() {
        return mPublisherVersion;
    }

    /**
     * @param publisherVersion the publisherVersion to set
     */
    public void setPublisherVersion(double publisherVersion) {
        this.mPublisherVersion = publisherVersion;
    }

    /**
     * sets publisher key
     * @param publisherKey - publisher key
     */
    public void setPublisherKey(String publisherKey) {
        mPublisherKey = publisherKey;
    }

    /**
     * sets description
     * @param description - description
     */
    public void setDescription(String description) {
        mDescription = description;
    }

    /**
     * Sets the package name
     * @param packageName - package name
     */
    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    /**
     * sets the type - action or condition
     * @param type - type
     */
    public void setType(String type) {
        mType = type;
    }

    /**
     * Sets whether the publisher supports new state or not
     * @param newState - "true" or "false"
     */
    public void setNewState(String newState) {
        mNewState = newState;
    }

    /**
     * Sets the label used for creating "new state"
     * @param label - label
     */
    public void setNewLabel(String label) {
        mNewLabel = label;
    }

    /**
     * Sets the activity intent
     * @param activityIntent - fully qualified activity intent
     */
    public void setActivityIntent(String activityIntent) {
        mActivityIntent = activityIntent;
    }

    /**
     * Sets the state type - stateful or stateless
     * @param stateType - "stateful" or "stateless"
     */
    public void setStateType(String stateType) {
        mStateType = (stateType.equals(LocalPublisherTable.PkgMgr.Type.STATEFUL)?
                      LocalPublisherTable.Modality.STATEFUL:LocalPublisherTable.Modality.STATELESS);
    }

    /**
     * Complete URL for market link download
     * @param marketLink - marketlink
     */
    public void setMarketLink(String marketLink) {
        mMarketLink = marketLink;
    }

    /**
     * Sets response latency
     * @param latency - "high"/"low"/"medium"
     */
    public void setResponseLatency (String latency) {
        mResponseLatency = latency;
    }

    /**
     * Sets data usage
     * @param dataUsage - "high"/"low"/"medium"
     */
    public void setDataUsage(String dataUsage) {
        mDataUsage = dataUsage;
    }

    /**
     * Sets battery drain
     * @param batteryDrain - "high"/"low"/"medium"
     */
    public void setBatteryDrain(String batteryDrain) {
        mBatteryDrain = batteryDrain;
    }

    /**
     * Sets sharability
     * @param share - true/false i.e., 1/0
     */
    public void setSharability(int share) {
        mShare = share;
    }

    /**
     * Whether the publisher is blacklisted or not
     * @param blackList - 1 or 0
     */
    public void blackListPublisher(int blackList) {
        mBlackList = blackList;
    }

    /**
     * Sets icon associated with the publisher
     * @param icon - Icon
     */
    public void setDrawable(Drawable icon) {
        mIcon = icon;
    }

    /**
     * Gets the settings action
     * @return
     */
    public String getSettingsAction() {
        return mSettingsAction;
    }

    /**
     * Sets settings action
     * @param settingsAction - settings action to set
     */
    public void setSettingsAction(String settingsAction) {
        this.mSettingsAction = settingsAction;
    }

    /**
     * gets the publisher key
     * @return - publisher key
     */
    public String getPublisherKey() {
        return mPublisherKey;
    }

    /**
     * gets the package name
     * @return - package name
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * gets the description
     * @return - description
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * gets the activity intent
     * @return - activity intent
     */
    public String getActivityIntent() {
        return mActivityIntent;
    }

    /**
     * Returns whether the publisher supports new state creation via UI or not
     * @return - "true"/"false"
     */
    public String getNewStateIntent() {
        return mNewState;
    }

    /**
     * gets the new label for creating new state
     * @return - label
     */
    public String getNewLabel() {
        return mNewLabel;
    }

    /**
     * Whether the publisher is interested in sharing its data outside device
     * @return
     */
    public int isShared() {
        return mShare;
    }

    /**
     * Returns the type of the publisher - action or condition
     * @return - type
     */
    public String getType() {
        return mType;
    }

    /**
     * Returns the market link
     * @return - market link
     */
    public String getMarketLink() {
        return mMarketLink;
    }

    /**
     * Whether the publisher is blacklisted or not
     * @return 1/0
     */
    public int isBlackListed() {
        return mBlackList;
    }

    /**
     * Returns the state type - "stateful"/"stateless"
     * @return - state type
     */
    public int getStateType() {
        return mStateType;
    }

    /**
     * Returns battery drain value
     * @return - "high"/"low"/"medium"
     */
    public String getBatteryDrain() {
        return mBatteryDrain;
    }

    /**
     * Returns data usage value
     * @return - "high"/"low"/"medium"
     */
    public String getDataUsage() {
        return mDataUsage;
    }

    /**
     * Returns response latency value
     * @return - "high"/"low"/"medium"
     */
    public String getResponseLatency() {
        return mResponseLatency;
    }

    /**
     * Returns drawable associated with the activity intent
     * @return - drawable icon
     */
    public Drawable getDrawable() {
        return mIcon;
    }

    @Override
    public boolean equals(Object o) {

        if(o == null) {
            return false;
        }
        // Return true if the objects are identical.
        // (This is just an optimization, not required for correctness.)
        if (this == o) {
            return true;
        }

        // Return false if the other object has the wrong type.
        // This type may be an interface depending on the interface's specification.
        if (!(o instanceof LocalPublisherTuple)) {
            return false;
        }

        // Cast to the appropriate type.
        // This will succeed because of the instanceof, and lets us access private fields.
        LocalPublisherTuple lhs = (LocalPublisherTuple) o;

        // Check each field. Primitive fields, reference fields, and nullable reference
        // fields are all treated differently.
        if(!((mPublisherKey == null) ? lhs.mPublisherKey == null :
                mPublisherKey.equals(lhs.mPublisherKey))) return false;
        if(!((mPackageName == null) ? lhs.mPackageName == null :
                mPackageName.equals(lhs.mPackageName))) return false;
        if(!((mDescription == null) ? lhs.mDescription == null :
                mDescription.equals(lhs.mDescription))) return false;
        if(!((mActivityIntent == null) ? lhs.mActivityIntent == null :
                mActivityIntent.equals(lhs.mActivityIntent))) return false;
        if(!((mNewState == null) ? lhs.mNewState == null :
                mNewState.equals(lhs.mNewState))) return false;
        if(!((mType == null) ? lhs.mType == null :
                mType.equals(lhs.mType))) return false;
        if(!((mMarketLink == null) ? lhs.mMarketLink == null :
                mMarketLink.equals(lhs.mMarketLink))) return false;
        if(!((mBatteryDrain == null) ? lhs.mBatteryDrain == null :
                mBatteryDrain.equals(lhs.mBatteryDrain))) return false;
        if(!((mDataUsage == null) ? lhs.mDataUsage == null :
                mDataUsage.equals(lhs.mDataUsage))) return false;
        if(!((mResponseLatency == null) ? lhs.mResponseLatency == null :
                mResponseLatency.equals(lhs.mResponseLatency))) return false;
        if(!((mSettingsAction == null) ? lhs.mSettingsAction == null :
                mSettingsAction.equals(lhs.mSettingsAction))) return false;

        if(!(mStateType == lhs.mStateType)) return false;
        if(!(mShare == lhs.mShare)) return false;
        if(!(mBlackList == lhs.mBlackList)) return false;
        if(!(mInterfaceVersion == lhs.mInterfaceVersion)) return false;
        if(!(mPublisherVersion == lhs.mPublisherVersion)) return false;

        if(mIcon == null && lhs.mIcon != null) return false;
        if(mIcon != null && lhs.mIcon == null) return false;

        if (mIcon != null) {
            Bitmap bitmap1 = ((BitmapDrawable)mIcon).getBitmap();
            Bitmap bitmap2 = ((BitmapDrawable)lhs.mIcon).getBitmap();

            if(!bitmap1.sameAs(bitmap2))  return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        // Start with a non-zero constant.
        int result = 17;

        // Include a hash for each field.
        result = 31 * result + ((mPublisherKey == null) ? 0 : mPublisherKey.hashCode());
        result = 31 * result + ((mPackageName == null) ? 0 : mPackageName.hashCode());
        result = 31 * result + ((mDescription == null) ? 0 : mDescription.hashCode());
        result = 31 * result + ((mActivityIntent == null) ? 0 : mActivityIntent.hashCode());
        result = 31 * result + ((mNewState == null) ? 0 : mNewState.hashCode());
        result = 31 * result + ((mType == null) ? 0 : mType.hashCode());
        result = 31 * result + ((mMarketLink == null) ? 0 : mMarketLink.hashCode());
        result = 31 * result + ((mBatteryDrain == null) ? 0 : mBatteryDrain.hashCode());
        result = 31 * result + ((mDataUsage == null) ? 0 : mDataUsage.hashCode());
        result = 31 * result + ((mResponseLatency == null) ? 0 : mResponseLatency.hashCode());
        result = 31 * result + ((mSettingsAction == null) ? 0 : mSettingsAction.hashCode());
        result = 31 * result + ((mIcon == null) ? 0 : mIcon.hashCode());
        result = 31 * result + mStateType;
        result = 31 * result + mShare;
        result = 31 * result + mBlackList;
        result = 31 * result + (int)mInterfaceVersion;
        result = 31 * result + (int)mPublisherVersion;

        return result;
    }
}
