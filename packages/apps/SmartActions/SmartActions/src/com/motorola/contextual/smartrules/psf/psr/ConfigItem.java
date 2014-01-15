/*
 * @(#)ConfigItem.java
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
package com.motorola.contextual.smartrules.psf.psr;

/** Class for ConfigItem found in the list response sent by individual publishers.
 * 
 *  PSR=Publisher States Receiver
 *
 * CLASS:
 *     ConfigItem
 *
 * RESPONSIBILITIES:
 *  Provides getters and setters
 *
 * USAGE:
 *     See each method.
 *
 */
public class ConfigItem {
    private String mConfig;
    private String mDescription;

    /**
     * Default constructor
     */
    public ConfigItem() {

    }

    /**
     * Constructor
     * @param item - ConfigItem itself.  Use this to create a copy of ConfigItem
     */
    public ConfigItem (ConfigItem item) {
        if (item != null) {
            setConfig(item.getConfig());
            setDescription(item.getDescription());
        } else {
            throw new IllegalArgumentException("ConfigItem is null");
        }
    }

    /**
     * Constructor
     * @param config - Config of the publisher
     * @param description - Description related to the config
     */
    public ConfigItem(String config, String description) {
        setConfig(config);
        setDescription(description);
    }

    /**
     * Function to set/reset config on a ConfigItem
     * @param config - A config of the publisher
     */
    public void setConfig(String config) {
        if (config != null && !config.isEmpty()) {
            mConfig = config;
        } else {
            throw new IllegalArgumentException ("config is null or empty");
        }
    }

    /**
     * Function to set/reset description on a ConfigItem
     * @param description - Description of the configItem
     */
    public void setDescription(String description) {
        if (description != null && !description.isEmpty()) {
            mDescription = description;
        } else {
            throw new IllegalArgumentException ("description is null or empty");
        }
    }

    /**
     * Returns the config associated with the config item
     * @return
     */
    public String getConfig() {
        return mConfig;
    }

    /**
     * Returns the description associated with the config item
     * @return
     */
    public String getDescription() {
        return mDescription;
    }
}
