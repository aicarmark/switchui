/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2011/05/06   IKCTXTAW-272    Initial version
 *
 */

package com.motorola.collectionservice;

import com.motorola.collectionservice.Callback_Client;

/**
 * CollectionService interface of MeterReader exposed to applications
 * @author w04917 (Brian Lee)
 */
interface ICollectionService {
    /**
    /* sets up a one time request for a list of meter types
     *
     * @param meterTypes List of String meter types. Basically the canonical name of meter reading subclass requested.
     * @param client Callback_Client defined by the client
     *
     * @return True if parameter values are valid and request is made, false otherwise.
     */
    boolean requestReading(in List<String> meterTypes, Callback_Client client);

    /**
     * sets up a one time request for all available meter types
     *
     * @param client Callback_Client defined by the client
     *
     * @return True if parameter values are valid and request is made, false otherwise.
     */
    boolean requestAllReadings(Callback_Client client);

    /**
     * requests all available meter types
     *
     * @param client Callback_Client defined by the client
     *
     * @return True if parameter values are valid and request is made, false otherwise.
     */
    boolean requestAvailableMeters(Callback_Client client);
}
