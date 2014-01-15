package com.motorola.firewall;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.firewall.FireWall.Settings;

public class FireWallInitReciver extends BroadcastReceiver {

    @Override
    public void onReceive(Context mcontext, Intent mintent) {

        String action = mintent.getAction();

        if (mcontext.getContentResolver() == null || action == null) {
            Log.e("FireWallInitReciver",
                    "FAILURE unable to get content resolver. FireWall inactive.");
            return;
        }
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            ContentResolver mresolver = mcontext.getContentResolver();
            // Boolean in_call_on = mPre.getBoolean("inblock_call_on", false);
            Boolean in_call_on = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_CALL_ON));
            // Boolean out_call_on = mPre.getBoolean("outblock_call_on", false);
            // Boolean in_sms_on = mPre.getBoolean("inblock_sms_on", false);
            Boolean in_sms_on = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.INBLOCK_SMS_ON));
            // Boolean in_sms_keyword = mPre.getBoolean("keywords_filter_on", false);
            // Boolean in_call_on_cdma = mPre.getBoolean("in_call_reject_cdma", false);
            // Boolean in_call_on_gsm = mPre.getBoolean("in_call_reject_gsm", false);
            // Boolean in_sms_on_cdma = mPre.getBoolean("in_sms_reject_cdma", false);
            // Boolean in_sms_on_gsm = mPre.getBoolean("in_sms_reject_gsm", false);
            Boolean in_call_on_cdma = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA));
            Boolean in_call_on_gsm = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_CALL_REJECT_GSM));
            Boolean in_sms_on_cdma = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA));
            Boolean in_sms_on_gsm = Boolean.valueOf(FireWallSetting.getDBValue(mresolver, Settings.IN_SMS_REJECT_GSM));
            // if (in_call_on || out_call_on || in_sms_on) {
            // if (in_call_on || in_sms_keyword || in_sms_on) {
            if (in_call_on || in_sms_on ||
                    in_call_on_cdma || in_call_on_gsm || in_sms_on_cdma || in_sms_on_gsm) {
                mcontext.startService(new Intent("com.motorola.firewall.action.START"));
            }
        }
    }
}
