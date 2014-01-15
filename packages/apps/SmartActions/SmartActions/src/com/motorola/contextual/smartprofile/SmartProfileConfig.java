/*
 * @(#)SmartProfileConfig.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- -----------------------------------
 * a21034	     7/7/2012    Initial version    Initial Version
 *
 */
package com.motorola.contextual.smartprofile;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class abstracts out a Smart Profile config
 *
 * <CODE><PRE>
 *
 * CLASS:
 *      SmartProfileConfig
 *
 * RESPONSIBILITIES:
 * 		Provides all utilities to operate on a smart profile config.
 *
 * COLABORATORS:
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */
public class SmartProfileConfig {
	public static final String PARAMETER_SEPARATOR = ";";
	public static final String NAME_VALUE_SEPARATOR = "=";

	private String mConfigString;
	private LinkedHashMap<String, String> mNameValuePairs = new LinkedHashMap<String, String> ();

	public SmartProfileConfig(String configString) {
		setConfigString(configString);
	}

	public SmartProfileConfig() {

	}

	/**
	 * @return the configString
	 */
	public String getConfigString() {
		sort();
		setStringFromMap();
		return mConfigString;
	}

	/**
	 * @param configString the configString to set
	 */
	public void setConfigString(String configString) {
		this.mConfigString = configString;
		parse();
	}

	public void addNameValuePair(String name, String value) {
		mNameValuePairs.put(name, value);
	}

	public void removeNameValueItem(String name) {
		mNameValuePairs.remove(name);
	}

	public String getValue(String name) {
		return mNameValuePairs.get(name);
	}

	public void addNameValueItemAsString(String nameValueItem) {
		String item[] = nameValueItem.split(NAME_VALUE_SEPARATOR);
		if (item.length == 2) {
			mNameValuePairs.put(item[0], item[1]);
		}
	}

	private void parse() {

		if (mConfigString != null && !mConfigString.isEmpty()) {
			String[] nameValueArray = mConfigString.split(PARAMETER_SEPARATOR);

			mNameValuePairs.clear();

			for (String nameValueItem : nameValueArray) {
				String[] nameAndValue = nameValueItem.split(NAME_VALUE_SEPARATOR);

				if (nameAndValue.length == 2) {
					mNameValuePairs.put(nameAndValue[0], nameAndValue[1]);
				}
			}
		}

	}

	private void sort() {
		List<String> nameList = new LinkedList<String>(mNameValuePairs.keySet());
		Collections.sort(nameList);
		LinkedHashMap<String, String> sortedMap = new LinkedHashMap<String, String> ();

		Iterator<String> i = nameList.iterator();
		while (i.hasNext()) {
			String name = (String)i.next();

			String value = mNameValuePairs.get(name);

			sortedMap.put(name, value);
		}

		mNameValuePairs = sortedMap;


	}

	private void setStringFromMap() {
		StringBuilder config = new StringBuilder();
		Set<String> nameSet = mNameValuePairs.keySet();

		for (String name : nameSet) {
			if (config.length() > 0) {
				config.append(PARAMETER_SEPARATOR);
			}
			String value = mNameValuePairs.get(name);
			config.append(name).append(NAME_VALUE_SEPARATOR).append(value);
		}

		mConfigString = config.toString();
	}

}
