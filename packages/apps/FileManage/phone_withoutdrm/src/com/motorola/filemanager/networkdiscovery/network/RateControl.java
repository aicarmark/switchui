/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date            CR                Author               Description
 *  2010-03-23      IKDROIDTWO-152    E12758               initial
 */

package com.motorola.filemanager.networkdiscovery.network;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.motorola.filemanager.samba.SambaExplorer;

public class RateControl {
    // TODO: Calculate a rounded up value from experiments in different networks
    private final static String TAG = "RateControl";
    private String mIndicatorHost;
    private long rate = 800; // Slow start
    public boolean is_indicator_discovered = true;

    public void adaptRate() {
	long response_time = 0;
	// TODO: Use an indicator with a port, calculate java round trip time
	// if (indicator.length > 1) {
	// Log.v(TAG, "use a socket here, port=" + getIndicator()[1]);
	// } else {
	is_indicator_discovered = true;
	if ((response_time = getAvgResponseTime(mIndicatorHost, 3)) > 0) {
	    // Add 30% to the response time
	    rate = response_time + (response_time * 3 / 10);
	    SambaExplorer.log(TAG + " adaptRate rate=" + rate);
	}
	// }
    }

    public long getControlRate() {
	// SambaExplorer.log(TAG + " getControlRate rate=" + rate);
	return rate;
    }

    private long getAvgResponseTime(String host, int count) {
	InputStreamReader fIn = null;
	Process p = null;
	BufferedReader r = null;

	try {
	    if ((new File("/system/bin/ping")).exists() == true) {

		String line;
		Matcher matcher;
		p = Runtime.getRuntime().exec(
			"/system/bin/ping -q -n -W 2 -c " + count + " " + host);
		fIn = new InputStreamReader(p.getInputStream());
		r = new BufferedReader(fIn, 512);

		while ((line = r.readLine()) != null) {
		    matcher = Pattern
			    .compile(
				    "^rtt min\\/avg\\/max\\/mdev = [0-9\\.]+\\/([0-9\\.]+)\\/[0-9\\.]+\\/[0-9\\.]+ ms$")
			    .matcher(line);
		    if (matcher.matches()) {
			SambaExplorer.log(TAG
				+ " getAvgResponseTime matches line: "
				+ matcher.group(1));
			long time = Long.valueOf(matcher.group(1));
			SambaExplorer.log(TAG
				+ " getAvgResponseTime ping time: " + time);
			return time;
		    }
		}
	    }
	} catch (Exception e) {
	    SambaExplorer.log(TAG + " Can't use native ping: " + e);
	} finally {
	    try {
		if (r != null) {
		    r.close();
		}
		if (fIn != null) {
		    fIn.close();
		}
		if (p != null) {
		    p.destroy();
		}
	    } catch (Exception e) {

	    }
	}
	return 0;
    }

    public void setIndicatorHost(String indiator) {
	mIndicatorHost = indiator;
    }
}
