/*
 * @(#)LocalPublisherConfigTuple.java
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

import android.content.ContentValues;
import android.database.Cursor;

/**
* LocalPublisherConfigTuple - Represents a row in the LocalPublisherConfigTable
* CLASS:
*     LocalPublisherConfigTuple
*
* RESPONSIBILITIES:
*  A convenience class to handle a row of LocalPublisherConfigTuple
*
* USAGE:
*     See each method.
*
*/
public class LocalPublisherConfigTuple {

    private String mPublisherKey;
    private String mConfig;
    private String mState;
    private String mDescription;

    /**
     * Constructor to create LocalPublisherConfigTuple
     * @param publisherKey - publisher key
     * @param config       - config
     * @param state        - state of the config
     * @param description  - description of the config
     */
    public LocalPublisherConfigTuple(String publisherKey, String config, String state, String description) {
        setPublisherKey(publisherKey);
        setConfig(config);
        setState(state);
        setDescription(description);
    }

    /**
     * Constructor for creating a LocalPublisherConfigTuple from a cursor
     * of LocalPublisherConfigTable
     * @param cursor - Cursor of the table LocalPublisherConfigTable
     */
    public LocalPublisherConfigTuple(Cursor cursor) {
        this(cursor.getString(cursor.getColumnIndex(LocalPublisherConfigTable.Columns.PUBLISHER_KEY)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherConfigTable.Columns.CONFIG)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherConfigTable.Columns.STATE)),
             cursor.getString(cursor.getColumnIndex(LocalPublisherConfigTable.Columns.DESCRIPTION)));
    }

    /**
     * Returns a ContentValue that can be inserted into LocalPublisherConfigTable
     * @return - ContentValue
     */
    public ContentValues getAsContentValues() {
        ContentValues value = new ContentValues();
        value.put(LocalPublisherConfigTable.Columns.PUBLISHER_KEY, getPublisherKey());
        value.put(LocalPublisherConfigTable.Columns.CONFIG, getConfig());
        value.put(LocalPublisherConfigTable.Columns.STATE, getState());
        value.put(LocalPublisherConfigTable.Columns.DESCRIPTION, getDescription());
        return value;
    }

    /**
     * Sets publisher key for LocalPublisherConfigTuple
     * @param publisherKey - publisher key
     */
    public void setPublisherKey(String publisherKey) {
        mPublisherKey = publisherKey;
    }

    /**
     * Sets the config for LocalPublisherConfigTuple
     * @param config - Config
     */
    public void setConfig(String config) {
        mConfig = config;
    }

    /**
     * Sets the state for LocalPublisherConfigTuple
     * @param state - state
     */
    public void setState(String state) {
        mState = state;
    }

    /**
     * Sets the description of LocalPublisherConfigTuple
     * @param description - description
     */
    public void setDescription(String description) {
        mDescription = description;
    }

    /**
     * Returns the publisher key for LocalPublisherConfigTuple
     * @return - publisher key
     */
    public String getPublisherKey() {
        return mPublisherKey;
    }

    /**
     * Returns the config for LocalPublisherConfigTuple
     * @return - Config
     */
    public String getConfig() {
        return mConfig;
    }

    /**
     * Returns the state of the LocalPublisherConfigTuple
     * @return - state
     */
    public String getState() {
        return mState;
    }

    /**
     * Returns the description of the LocalPublisherConfigTuple
     * @return - description
     */
    public String getDescription() {
        return mDescription;
    }
}
