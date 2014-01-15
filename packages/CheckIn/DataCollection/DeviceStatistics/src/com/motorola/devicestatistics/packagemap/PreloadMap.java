/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: PreloadMap.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Oct 18, 2010       A24178      Created file
 **********************************************************
 */

package com.motorola.devicestatistics.packagemap;

import java.util.HashMap;

/**
 * @author A24178
 *
 */
public class PreloadMap {

    public static final long MAX_PRELOAD_ID = 1000;
    static HashMap<String, Long> sMaps = new HashMap<String, Long>();

    static void init() {
        sMaps.put("com.google.android.location", 0L);
        sMaps.put("com.motorola.togglewidgets", 1L);
        sMaps.put("com.android.phone", 2L);
        sMaps.put("com.android.bluetooth", 3L);
        sMaps.put("com.android.browser", 4L);
        sMaps.put("com.motorola.blur.service.blur", 5L);
        sMaps.put("com.android.provider.settings", 6L);
        sMaps.put("com.amazon.mp3", 7L);
        sMaps.put("com.android.vending.updater", 8L);
        sMaps.put("com.motorola.im", 9L);
        sMaps.put("com.android.providers.downloads", 10L);
        sMaps.put("com.google.android.apps.uploader", 11L);
        sMaps.put("com.motorola.blur.home", 12L);
        sMaps.put("com.motorola.dialer", 13L);
        sMaps.put("com.motorola.batterymanager", 14L);
        sMaps.put("com.motorola.blur.email", 15L);
        sMaps.put("com.motorola.blur.providers.contacts", 16L);
        sMaps.put("com.google.android.syncadapters.calendar", 17L);
        sMaps.put("com.android.vending", 18L);
        sMaps.put("com.android.calculator2", 19L);
        sMaps.put("android.tts", 20L);
        sMaps.put("com.google.android.apps.genie.geniewidget", 21L);
        sMaps.put("com.motorola.wappushsi", 22L);
        sMaps.put("com.motorola.android.syncml.ui", 23L);
        sMaps.put("com.google.android.gm", 24L);
        sMaps.put("com.slacker.radio", 25L);
        sMaps.put("com.motorola.atcmd.plugin", 26L);
        sMaps.put("com.motorola.android.datamanager", 27L);
        sMaps.put("com.motorola.dlauncher", 28L);
        sMaps.put("com.motorola.cmp", 29L);
        sMaps.put("com.motorola.blur.alarmclock", 30L);
        sMaps.put("com.motorola.motocall", 31L);
        sMaps.put("com.android.providers.userdictionary", 32L);
        sMaps.put("com.motorola.btlowbattery", 33L);
        sMaps.put("com.motorola.Camera", 34L);
        sMaps.put("com.telenav.app.android.telenav", 35L);
        sMaps.put("com.motorola.usb", 36L);
        sMaps.put("com.motorola.videoplayer", 37L);
        sMaps.put("com.swype.android.inputmethod", 38L);
        sMaps.put("com.android.setupwizard", 39L);
        sMaps.put("com.motorola.blur.messaging", 40L);
        sMaps.put("com.motorola.blur.helpcenter", 41L);
        sMaps.put("com.motorola.cmas", 42L);
        sMaps.put("com.motorola.globalunplug", 43L);
        sMaps.put("com.google.android.googlequicksearchbox", 44L);
        sMaps.put("com.motorola.android.dm.service", 45L);
        sMaps.put("com.motorola.android.motophoneportal.androidui", 46L);
        sMaps.put("com.motorola.filemanager", 47L);
        sMaps.put("com.motorola.cardock", 48L);
        sMaps.put("com.motorola.bluetoothdun", 49L);
        sMaps.put("com.motorola.android.provisioning", 50L);
        sMaps.put("com.android.providers.calendar", 51L);
        sMaps.put("com.motorola.spellingcheckservice", 52L);
        sMaps.put("com.motorola.vclipboard", 53L);
        sMaps.put("com.motorola.Blockbuster", 54L);
        sMaps.put("com.motorola.blur.socialmessaging", 55L);
        sMaps.put("com.google.android.apps.maps", 56L);
        sMaps.put("com.google.android.voicesearch", 57L);
        sMaps.put("com.svox.pico", 58L);
        sMaps.put("com.motorola.blur.updater", 59L);
        sMaps.put("com.google.android.partnersetup", 60L);
        sMaps.put("com.motorola.PerformanceManager", 61L);
        sMaps.put("com.motorola.blur.conversations", 62L);
        sMaps.put("com.motorola.android.AudioEffectSettings", 63L);
        sMaps.put("com.motorola.blur.controller", 64L);
        sMaps.put("com.motorola.hiddenmenu", 65L);
        sMaps.put("com.motorola.devicestatistics", 66L);
        sMaps.put("com.motorola.im.offlineService", 67L);
        sMaps.put("com.motorola.android.wmdrm.dla", 68L);
        sMaps.put("com.motorola.blur.quickcontact", 69L);
        sMaps.put("com.motorola.blur.policymgr.provider", 70L);
        sMaps.put("com.android.calendar", 71L);
        sMaps.put("com.tmobile.nabservice", 72L);
        sMaps.put("com.motorola.tmobile.portal", 73L);
        sMaps.put("com.tmobile.selfhelp", 74L);
        sMaps.put("com.motorola.android.audiopostcard", 75L);
        sMaps.put("com.amazon.kindle", 76L);
        sMaps.put("com.tmobile.apppack", 77L);
        sMaps.put("UID0", 78L);
        sMaps.put("UID1002", 79L);
        sMaps.put("UID1003", 80L);
        sMaps.put("UID1008", 81L);
        sMaps.put("UID1010", 82L);
        sMaps.put("UID1013", 83L);
        sMaps.put("UID1014", 84L);
        sMaps.put("UID1017", 85L);
        sMaps.put("UID9000", 86L);
        sMaps.put("UID9004", 87L);
        sMaps.put("UID9007", 88L);
        sMaps.put("com.motorola.kpilogger", 89L);
        sMaps.put("com.qo.android.moto", 90L);
        sMaps.put("com.quickplay.android.pt2g", 91L);
        sMaps.put("com.android.kineto", 92L);
        sMaps.put("com.android.defcontainer", 93L);
        sMaps.put("com.motorola.Dlna", 94L);
        sMaps.put("com.motorola.mediashare", 95L);
        sMaps.put("com.motorola.android.vvm", 96L);
        sMaps.put("com.motorola.android.fmradio", 97L);
    }

    public static boolean isPreloaded(long id) {
        return id >= 0 && id < MAX_PRELOAD_ID;
    }
}

