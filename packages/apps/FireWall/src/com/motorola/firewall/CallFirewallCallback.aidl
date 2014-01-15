package com.motorola.firewall;

interface CallFirewallCallback {

    int CheckNumberBlock(in String phonenumber, in int calltype, in boolean updatelog, in int network);
}