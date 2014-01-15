package com.android.phone;

import com.motorola.firewall.CallFirewallCallback;

interface PhoneHub {

    void registerCallback(CallFirewallCallback cb);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(CallFirewallCallback cb);
}