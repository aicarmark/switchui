/*
 * @(#)ConfigList.java
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

import java.util.ArrayList;
import java.util.List;

import com.motorola.contextual.smartrules.psf.PsfConstants;

/**
* ConfigList abstracts out a response from the publishers for the list command. PSR=Publisher States Receiver
* CLASS:
*     ConfigList
*
* RESPONSIBILITIES:
*  Provides functions to operate on the list of configs
*  Provides the list of configs in various formats
*
* USAGE:
*     See each method.
*
*/
public class ConfigList implements PsfConstants {
    @SuppressWarnings("unused")
    private final static String TAG = PsfConstants.PSF_PREFIX + ConfigList.class.getSimpleName();
    private List<ConfigItem> mConfigItems;

    interface Xml {
        String CONFIG_ITEMS = "CONFIG_ITEMS";
        String ITEM         = "ITEM";
        String CONFIG       = "CONFIG";
        String DESCRIPTION  = "DESCRIPTION";
    }

    /**
     * Constructor
     * @param xml - List response from publishers
     */
    public ConfigList(String xml) {
        mConfigItems = parseXml(xml);
    }

    /**
     * Add a config item to the existing list
     * @param configItem - configitem found inside the list response
     */
    public void addConfig(ConfigItem configItem) {
        if (configItem != null) {
            mConfigItems.add(configItem);
        } else {
            throw new IllegalArgumentException ("ConfigItem is null");
        }
    }

    /**
     * Removes the config from the list of configs stored internally
     * @param configItem
     */
    public void removeConfig(ConfigItem configItem) {
        mConfigItems.remove(configItem);
    }

    /**
     * Add a config item by passing in config and description
     * @param config       - Config associated with the configItem to be added
     * @param description  - Description associated with the configItem to be added
     */
    public void addConfig(String config, String description) {
        mConfigItems.add(new ConfigItem(config, description));
    }

    /**
     * Returns the list of config items associated with the object
     * @return
     */
    public List<ConfigItem> getListOfItems() {

        List<ConfigItem> returnList = new ArrayList<ConfigItem> ();

        for (ConfigItem configItem : mConfigItems) {
            ConfigItem returnItem = new ConfigItem(configItem);
            returnList.add(returnItem);
        }

        return returnList;
    }

    /**
     * Parses the list response xml
     * @param xml - Response for command=list to publishers
     * @return - Returns a list of config item
     */
    private List<ConfigItem> parseXml(String xml) {

        ConfigListXmlParser xmlParser = new ConfigListXmlParser(xml);
        return xmlParser.getConfigList();
    }
}
