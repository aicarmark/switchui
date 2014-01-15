package com.motorola.firewall;

interface SmsFirewallCallback {

    boolean CheckSmsBlock(in String phonenumber, in String smscontent, in boolean updatelog, in int network);
}