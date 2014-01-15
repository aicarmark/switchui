/*
 * Copyright (C) 2011 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date         CR Number       Brief Description
 * -------------------------   ----------   -------------   ------------------------------
 * w04917 (Brian Lee)          2011/11/01   IKCTXTAW-359    Initial version
 * w04917 (Brian Lee)          2012/02/14   IKCTXTAW-441    Use the new Checkin API
 *
 */

package com.motorola.datacollection.perfstats;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;

import com.motorola.data.event.api.Segment;
import com.motorola.datacollection.CheckinSegmentGenerator;
import com.motorola.kpi.perfstats.LogSetting;

public abstract class Metric implements CheckinSegmentGenerator {

    protected final LogSetting mLogSetting;

    private static final Pattern NULL_PATTERN = Pattern.compile("\0");

    public Metric(LogSetting logSetting) {
        mLogSetting = logSetting;
    }

    /**
     * starts the collection of metrics
     * @param bundle Bundle containing metric data from FW
     */
    public abstract void handleStart(Bundle bundle);

    /**
     * stops the collection of metrics
     * @param bundle Bundle containing metric data from FW
     */
    public abstract void handleStop(Bundle bundle);

    public Collection<Segment> getCheckinSegments(int type) {
        /* not used by default */
        return null;
    }

    /**
     * Sanitizes String from readLine().
     * Use this instead of BufferedReader.readLine() for data that will be
     * parsed as numbers or data that will be used as part of file/path name to read.
     * Addresses KlocWork error
     * @param line String to be sanitized
     * @return sanitized String, null if the String contains tained characters
     * @throws IOException Exception from BufferedReader
     */
    protected static final String sanitizedReadLine(BufferedReader br) throws IOException {
        String sanitized = null;
        if (br != null) {
            String line = br.readLine();
            if (line != null) {
                line = Normalizer.normalize(line, Form.NFKC);
                Matcher matcher = NULL_PATTERN.matcher(line);
                if (!matcher.find()) {
                    sanitized = line;
                }
            }
        }
        return sanitized;
    }
}
