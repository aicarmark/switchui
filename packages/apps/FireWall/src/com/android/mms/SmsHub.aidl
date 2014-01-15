package com.android.mms;

import com.motorola.firewall.SmsFirewallCallback;

interface SmsHub {

    void registerCallback(SmsFirewallCallback cb);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(SmsFirewallCallback cb);
}