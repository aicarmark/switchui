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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.motorola.filemanager.samba.SambaExplorer;

public class NetInfo {
    private static final String TAG = "NetInfo:";
    private Context ctxt;

    private String intf = null;
    private String ip = "0.0.0.0";
    private int cidr = 24;
    private String ipPath = "/system/xbin/ip";
    private String ifConfig = "/system/bin/ifconfig";

    public NetInfo(Context ctxt) {
        this.ctxt = ctxt;
    }

    public String getIp() {
        WifiManager wifiManager = (WifiManager) ctxt.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String dotIPAddress = "";
        dotIPAddress =
                Integer.toString((ipAddress & 0x000000ff)) + "." +
                        Integer.toString((ipAddress & 0x0000ff00) >>> 8) + "." +
                        Integer.toString((ipAddress & 0x00ff0000) >>> 16) + "." +
                        Integer.toString((ipAddress & 0xff000000) >>> 24);
        ip = dotIPAddress;
        SambaExplorer.log(TAG + "IP " + ip, false);
        return dotIPAddress;
    }

    public String getWlanInterface(String host) {
        // Iterate throught interfaces
        NetworkInterface ni = null;
        if (host != null) {
            try {
                ni = NetworkInterface.getByInetAddress(InetAddress.getByName(host));
                if (ni != null) {
                    intf = ni.getName();
                    SambaExplorer.log(TAG + "NetworkInterface : " + intf + " ip " + ip, false);
                } else {
                    intf = null;
                    SambaExplorer.log(TAG + "NetworkInterface not found", true);
                }
            } catch (SocketException ex) {
                SambaExplorer.log(TAG + ex.getMessage(), true);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                SambaExplorer.log(TAG + e.getMessage(), true);
            }
        } else {
            intf = null;
        }

        return intf;
    }

    public int getCidr(String iface) {
        if (iface != null) {
            if ((runCommand(new File(ipPath), "ip -f inet addr show " + iface,
                    "\\s*inet [0-9\\.]+\\/([0-9]+) brd [0-9\\.]+ scope global " + iface + "$") > 0)) {
            } else if ((runCommand(new File(ifConfig), "ifconfig " + iface, "^" + iface +
                    ": ip [0-9\\.]+ mask ([0-9\\.]+) flags")) > 0) {
            }
        }
        return cidr;

    }

    private int runCommand(File file, String cmd, String ptrn) {
        InputStreamReader inp = null;
        BufferedReader r = null;
        InputStream iS = null;

        try {
            if (file.exists() == true) {
                String line;
                Matcher matcher;
                Process p = Runtime.getRuntime().exec(cmd);
                iS = p.getInputStream();
                inp = new InputStreamReader(iS);
                r = new BufferedReader(inp, 1);
                SambaExplorer.log(TAG + "CMD=" + cmd, false);
                while ((line = r.readLine()) != null) {
                    SambaExplorer.log(TAG + line, false);
                    matcher = Pattern.compile(ptrn).matcher(line);
                    if (matcher.matches()) {
                        SambaExplorer.log(TAG + "MATCH=" + matcher.group(1), false);
                        cidr = Integer.parseInt(matcher.group(1));
                        iS.close();
                        inp.close();
                        r.close();
                        return cidr;
                    }
                }
                iS.close();
                inp.close();
                r.close();
            }
        } catch (Exception e) {
            SambaExplorer.log(TAG + "Can't use native command: " + e.getMessage(), true);
            if (iS !=null) {
              try {
                  iS.close();
              } catch (IOException e1) {
                  SambaExplorer.log(TAG + e1 + " "  + e1.getMessage(), true);
              }
            }
            if (inp !=null) {
              try {
                  inp.close();
              } catch (IOException e1) {
                  SambaExplorer.log(TAG + e1 + " "  + e1.getMessage(), true);
              }
            }
            if (r !=null) {
              try {
                  r.close();
              } catch (IOException e1) {
                  SambaExplorer.log(TAG + e1 + " "  + e1.getMessage(), true);
              }
            }
            //  } catch (IOException e1) {
            // TODO Auto-generated catch block
            //  e1.printStackTrace();
            // }

        }
        return 0;
    }

    public static boolean isConnected(Context ctxt) {
        NetworkInfo nfo =
                ((ConnectivityManager) ctxt.getSystemService(Context.CONNECTIVITY_SERVICE))
                        .getActiveNetworkInfo();
        if (nfo != null) {
            return nfo.isConnected();
        }
        return false;
    }

    public static long getUnsignedLongFromIp(String ip_addr) {
        String[] a = ip_addr.split("\\.");
        return (Integer.parseInt(a[0]) * 16777216 + Integer.parseInt(a[1]) * 65536 +
                Integer.parseInt(a[2]) * 256 + Integer.parseInt(a[3]));
    }

    public static String getIpFromLongUnsigned(long ip_long) {
        String ip = "";
        for (int k = 3; k > -1; k--) {
            ip = (ip + ((ip_long >> k * 8) & 0xFF)).concat(".");
        }
        return ip.substring(0, ip.length() - 1);
    }
}
