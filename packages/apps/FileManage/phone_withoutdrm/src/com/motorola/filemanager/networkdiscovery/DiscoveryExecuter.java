/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date            CR                Author               Description
 *  2010-03-23  IKDROIDTWO-152:    E12758                   initial
 */
package com.motorola.filemanager.networkdiscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.AsyncTask;

import com.motorola.filemanager.networkdiscovery.network.NetInfo;
import com.motorola.filemanager.networkdiscovery.network.RateControl;
import com.motorola.filemanager.networkdiscovery.network.Reachable;
import com.motorola.filemanager.samba.SambaExplorer;

public class DiscoveryExecuter extends AsyncTask<Void, String, Void> implements
	DiscoveryPrefInterface {
    private static final String TAG = "DiscoveryExecuter";
    private Reachable mReachable;
    private ExecutorService mPool;
    private int hosts_done = 0;
    private int mRateCnt = 0;
    private boolean mLongTimeout = true;
    private RateControl mRateControl;
    private Context mContext;

    private long ip;
    private long start;
    private long end;
    private long size;
    private DiscoveryHandler mdiscoveryhandler;

    public DiscoveryExecuter(Context context) {
	mContext = context;
	mReachable = new Reachable();
	mRateControl = new RateControl();
	mdiscoveryhandler = new DiscoveryHandler(context);
    }

    protected void publish(String str) {
	publishProgress(str);
	mRateCnt++;
    }

    public DiscoveryHandler getHandler() {
	return mdiscoveryhandler;
    }

    @Override
    protected Void doInBackground(Void... params) {
	SambaExplorer.log(TAG + " start="
		+ NetInfo.getIpFromLongUnsigned(start) + " (" + start
		+ "), end=" + NetInfo.getIpFromLongUnsigned(end) + " (" + end
		+ "), length=" + size);
	mPool = Executors
		.newFixedThreadPool(Integer.parseInt(DEFAULT_NTHREADS));

	try {
	    // gateway
	    launch(start);
	    middleLaunch(start, end, ip);
	    // nearbyLaunch(start,end,ip);
	    mPool.shutdown();
	    mPool.awaitTermination(3600L, TimeUnit.SECONDS);
	} catch (InterruptedException e) {
	    SambaExplorer.log(TAG + "Got Interrupted");
	}
	return null;
    }

    private void middleLaunch(long start, long end, long ip) {
	long middle = (end + start) / 2;
	start = start + 1;

	if (middle < ip) {
	    oppositeCount(middle, ip);
	    oppositeCount(start, middle - 1);
	    oppositeCount(ip + 1, end);
	} else if (middle > ip) {
	    oppositeCount(ip, middle);
	    oppositeCount(start, ip - 1);
	    oppositeCount(middle + 1, end);
	} else {
	    oppositeCount(start, ip);
	    oppositeCount(ip + 1, end);
	}
    }

    private void oppositeCount(long start, long end) {
	long size_hosts = end - start + 1;
	long pt_backward = end;
	long pt_forward = start;
	int direct = 2;

	for (int i = 0; i < size_hosts; i++) {
	    if (direct == 1) {
		launch(pt_backward);
		pt_backward--;
		direct = 2;
	    } else if (direct == 2) {
		launch(pt_forward);
		pt_forward++;
		direct = 1;
	    }
	}
    }

    /*
     * private void nearbyLaunch(long start ,long end, long ip ){ long
     * pt_backward = ip - 1; long pt_forward = ip + 1; long size_hosts = end -
     * start - 2; int step = 2; for (int i = 0; i < size_hosts; i++) { // Set
     * pointer if of limits if (pt_backward <= start) { step = 2; } else if
     * (pt_forward > end) { step = 1; } // Move back and forth if (step == 1) {
     * launch(pt_backward); pt_backward--; step = 2; } else if (step == 2) {
     * launch(pt_forward); pt_forward++; step = 1; } } }
     */

    private void launch(long i) {
	mPool.execute(new CheckRunnable(NetInfo.getIpFromLongUnsigned(i)));
    }

    private class CheckRunnable implements Runnable {
	private String host;

	CheckRunnable(String host) {
	    this.host = host;
	    // SambaExplorer.log(TAG + " CheckRunnable Host: " + host);
	}

	// @Override
	public void run() {
	    try {
		Thread.sleep(getRate());
		InetAddress h = InetAddress.getByName(host); // FIXME: is that

		// SambaExplorer.log(TAG + " CheckRunnable run Host: " + host);
		// producing logs?
		// Rate control check
		// mRateControl.setIndicatorHost(host);
		// if (mRateControl.is_indicator_discovered &&( mRateCnt %
		// mRateMult) ==
		// 0) {
		// mRateControl.adaptRate();
		// }
		// Native InetAddress check
		// if (h.isReachable(getRate())) {
		// SambaExplorer.log(TAG + "Use ECHO Successfully get Address: "
		// +
		// host);
		// publish(host);
		// return;
		// if (!mRateControl.is_indicator_discovered) {
		// mRateControl.indicator = new String[]{host};
		// mRateControl.adaptRate();
		// }
		// }
		// Custom check
		int port = -1;
		if ((port = mReachable.isReachable(h, getPoliceRate())) > -1) {
		    SambaExplorer.log(TAG + "Reachable HOST: " + host
			    + "  Reachable PORT: " + port + " Rate "
			    + getPoliceRate());
		    publish(host);
		    // mRateControl.setIndicatorHost(host);
		    // mRateControl.adaptRate();
		    mLongTimeout = false;
		    return;
		} else {
		    mLongTimeout = true;
		    // publish((String) null);
		}
	    } catch (IOException e) {
		publish((String) null);
		SambaExplorer.log(TAG + e + " " + e.getMessage());
	    } catch (InterruptedException e) {
	    }
	}

	private int getPoliceRate() {
	    return ((mLongTimeout ? 4 : 3) * Integer
		    .parseInt(DEFAULT_TIMEOUT_DISCOVER));
	}

	private int getRate() {
	    if (DEFAULT_RATECTRL_ENABLE == true) {
		return (int) mRateControl.getControlRate();
	    }
	    return Integer.parseInt(DEFAULT_TIMEOUT_DISCOVER);
	}
    }

    @Override
    protected void onPreExecute() {
	SambaExplorer.log(TAG + "  ++onPreExecute()++");

	NetInfo net = new NetInfo(mContext);
	String IpAddress = net.getIp();
	ip = NetInfo.getUnsignedLongFromIp(IpAddress);
	int shift = (32 - net.getCidr(net.getWlanInterface(IpAddress)));
	start = (ip >> shift << shift) + 1;
	end = (start | ((1 << shift) - 1)) - 1;
	size = (int) (end - start + 1);
	mdiscoveryhandler.fireDiscoveryStartEvent();
    }

    @Override
    protected void onProgressUpdate(String... item) {
	if (!isCancelled()) {
	    if (item[0] != null) {
		SambaExplorer.log(TAG + " Item[0]:     " + item[0]);
		mdiscoveryhandler.fireHostFoundEvent(item[0]);
	    }
	    hosts_done++;
	}
    }

    @Override
    protected void onPostExecute(Void unused) {
	SambaExplorer.log(TAG + " ++onPostExecute()++");
	mdiscoveryhandler.fireDiscoveryFinishEvent();
    }

    @Override
    protected void onCancelled() {
	SambaExplorer.log(TAG + " ++onCancelled()++");
	mPool.shutdownNow();
	mdiscoveryhandler.fireDiscoveryFinishEvent();
    }
}
