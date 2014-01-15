/*
 * @(#)TimeFrameXmlSyntax.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a15776       2011/02/17  NA                Initial Version
 *
 */
package com.motorola.contextual.smartprofile.sensors.timesensor;

import com.motorola.contextual.smartprofile.Constants;

/** This interface simply defines the XML tags and String Constants needed to compose
 *  VSENSOR string
 *
 *<code><pre>
 * INTERFACE:
 *   It allows the user to build a VSENSOR
 *
 * RESPONSIBILITIES:
 *  None - all static Strings here.
 *
 * COLABORATORS:
 *  None - all static Strings here.
 *</pre></code>
 */
public interface TimeFrameXmlSyntax extends Constants {
    String VIRTUAL_SENSOR_TAG_START = "<VirtualSensor ";
    String VIRTUAL_SENSOR_NAME = "name =\"" + VIRTUAL_SENSOR_STRING;
    String QUOTE_ESCAPE = "\"";
    String SENSOR_PERSISTENCE = "persistenceSensor =\"persist_forever\" ";
    String VALUE_PERSISTENCE = "persistenceValue =\"persist_forever\" ";
    String VENDOR = "vendor =\"Motorola\" ";
    String DESCRIPTION = "description =\"Time Frame Sensor\"  >";
    String INITIAL_VALUE = "<initialValue value=\"{false}\"/>";
    String POSSIBLE_VALUES =
        "<possibleValues0><value>true</value><value>false</value></possibleValues0>" ;
    String RULE_MECHANISM = "<mechanism value=\"rules\"/>";
    String RULE_SET_TAG_START = "<ruleset>";
    String RULE_SET_TAG_END = "</ruleset>";

    String RULE_TAG_START = "<rule>";
    String RULE_TAG_END = "</rule>";

    String VIRTUAL_SENSOR_TAG_END = "</VirtualSensor>";

    String VSENSOR_TAG_START = "<VSENSOR>";
    String VSENSOR_TAG_END   = "</VSENSOR>";
    String VERSION_NO = "version =\"1\" " ;
}
