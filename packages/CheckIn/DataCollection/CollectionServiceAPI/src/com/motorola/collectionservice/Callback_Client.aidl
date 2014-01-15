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

import android.os.Bundle;

/**
 * This is the oneway callback used by CollectionService to return data to the cilent app
 * We don't want the service blocking on what the client app does, so we use
 * the "fire and forget" style oneway interface
 *
 * @author w04917 (Brian Lee)
 */
oneway interface Callback_Client {
    /**
     * Callback for CollectionService to call upon obtaining meter reading values
     *
     * @param readings List of Bundles, each of which has the packed meter reading
     */
    void readingCallback(in List<Bundle> readings);

    /**
     * Callback for CollectionService to call upon getting an error while trying to
     * read the requested meter types. Typically, this would indicate that
     * none of the requested meters are registered with the system.
     *
     * @param meterTypes The originally requested meterTypes.
     */
    void readingErrorCallback(in List<String> meterTypes);

    /**
     * Callback for CollectionService to call upon getting a list of available meters
     *
     * @param meterTypes The List of String, each of which has the full canonical name of the registered
     * Meter reading. This is the value used for meterType, to request a reading.
     */
    void availableMetersCallback(in List<String> meterTypes);
}
