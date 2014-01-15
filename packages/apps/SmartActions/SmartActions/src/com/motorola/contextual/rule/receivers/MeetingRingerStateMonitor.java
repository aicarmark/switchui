/*
 * @(#)MeetingRingerStateMonitor.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21693        2012/05/16  NA               Initial version
 *
 */

package com.motorola.contextual.rule.receivers;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.smartrules.monitorservice.CommonStateMonitor;

/**
 * This class extends CommonStateMonitor for Sleep inferencing
 *
 * <CODE><PRE>
 *
 * CLASS:
 *     Extends CommonStateMonitor
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * Extends methods of CommonStateMonitor for charging publisher
 *
 * COLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </PRE></CODE>
 */

public class MeetingRingerStateMonitor extends CommonStateMonitor implements Constants {

    BroadcastReceiver mReceiver = null;

    @Override
    public BroadcastReceiver getReceiver() {
        if (mReceiver == null)
            mReceiver = new Receiver();
        return mReceiver;
    }

    @Override
    public void setReceiver(BroadcastReceiver receiver) {
        mReceiver = receiver;
    }

    @Override
    public String getType() {
        return RECEIVER;
    }

    @Override
    public ArrayList<String> getStateMonitorIdentifiers() {
        ArrayList<String> actions = new ArrayList<String>();

        actions.add(AudioManager.RINGER_MODE_CHANGED_ACTION);
        return actions;
    }

    /**
     * this class receives intents from Android FW and launches
     * RP intent service to handle the broadcast for inference usecase
     *
     * @author a21693
     *
     */
    private class Receiver extends BaseInferenceReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

            intent.putExtra(Constants.IDENTIFIER, Constants.RULE_KEY_MEETING);
            super.onReceive(context, intent);
        }
    }
}