/**
 * Copyright (C) 2009 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 */

package com.motorola.devicestatistics;

import android.content.Context;

import com.motorola.devicestatistics.CheckinHelper.DsCheckinEvent;
import com.motorola.devicestatistics.eventlogs.EventLoggerService;

import java.util.ArrayList;

/**
 * Thread controlled by DeviceStatisticsSvc to log passive device statistics to 
 * Checkin DB
 */
public class CollectStatistics {
    private Context mContext;

    long mCheckinTime;

    public void getDeviceStats(Context ctx, long checkinTime, String usbStateInfo,
            ArrayList<DsCheckinEvent> logList) {
        mContext = ctx;
        mCheckinTime = checkinTime;
        parseStats(logList, usbStateInfo);
    }

    /**
     * parseStats collects stats and logs into checkin DB.
     */
    private void parseStats(ArrayList<DsCheckinEvent> logList, String usbStateInfo) {
        boolean triggeredByUsb = usbStateInfo != null;

        if ( triggeredByUsb ) {
            DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent(
                    DevStatPrefs.CHECKIN_EVENT_ID_BYCHARGER, "ChargerTrigger", DevStatPrefs.VERSION,
                    mCheckinTime );
            if (!usbStateInfo.isEmpty()) checkinEvent.setValue( "p", usbStateInfo );
            logList.add(checkinEvent);
        }

        // Mobile & Wi-Fi Data
        SysClassNetUtils.updateNetStats(mContext); //Update since the last update for accuracy.
        String[][] wifiData = SysClassNetUtils.getWifiRxTxBytes(mContext);
        String[][] mobileData = SysClassNetUtils.getMobileRxTxBytes(mContext);
        String[][] wifiPkt = SysClassNetUtils.getWifiRxTxPkts(mContext);
        String[][] mobilePkt = SysClassNetUtils.getMobileRxTxPkts(mContext);
        String logTag = triggeredByUsb?
                DevStatPrefs.CHECKIN_EVENT_ID_BYCHARGER : DevStatPrefs.CHECKIN_EVENT_ID;
        for (int i = 0; i < SysClassNetUtils.MAX_INDEX; i++) {
            if ((wifiData[i] != null) || (mobileData[i] != null) ||
                    (wifiPkt[i] != null) || (mobilePkt[i] != null)) {
                DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent(logTag,
                        "DataSizes" + SysClassNetUtils.ID_SUFFIX[i], DevStatPrefs.DATASIZES_VERSION,
                        mCheckinTime);
                Utils.addNonZeroValues(checkinEvent, mobileData[i]);
                Utils.addNonZeroValues(checkinEvent, wifiData[i]);
                Utils.addNonZeroValues(checkinEvent, mobilePkt[i]);
                Utils.addNonZeroValues(checkinEvent, wifiPkt[i]);
                logList.add(checkinEvent);
            }
        }

        if ( !triggeredByUsb ) {
            // SMS & Call Info
            DsCheckinEvent checkinEvent = CheckinHelper.getCheckinEvent(
                    DevStatPrefs.CHECKIN_EVENT_ID, "SMSCalls", DevStatPrefs.VERSION,
                    mCheckinTime );
            boolean smsInfo = DevStatUtils.addSMSDetails(checkinEvent, mContext);
            boolean callInfo = DevStatUtils.addCallDetails(checkinEvent, mContext);
            if (smsInfo || callInfo) {
                logList.add(checkinEvent);
            }

            // Contact & Email Info
            checkinEvent = CheckinHelper.getCheckinEvent( DevStatPrefs.CHECKIN_EVENT_ID,
                    "ContactEmailDetails", DevStatPrefs.VERSION, mCheckinTime );
            boolean contactInfo = DevStatUtils.getContactsDetails(checkinEvent, mContext);
            boolean emailInfo = DevStatUtils.getEmailDetails(checkinEvent, mContext);
            if (contactInfo || emailInfo) logList.add(checkinEvent);
        }

        // CPU Freq log
        DevStatUtils.addCpuFreqLog(logList, mCheckinTime, triggeredByUsb);

        if ( !triggeredByUsb ) {
            // Add background thread overflow stats
            EventLoggerService.getInstance(mContext).checkin(logList,mCheckinTime);
        }
    }
}
