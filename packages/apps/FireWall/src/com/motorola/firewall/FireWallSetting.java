package com.motorola.firewall;


import com.motorola.firewall.FireWall.Settings;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;
// import com.motorola.android.telephony.PhoneModeManager; // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FireWallSetting extends PreferenceFragment{

    private CheckBoxPreference min_call_on;
    // private CheckBoxPreference mout_call_on;
    private CheckBoxPreference min_sms_on;
    private ListPreference mInblock_call_list;
    // private ListPreference mOutblock_call_list;
    private ListPreference mInblock_sms_list;
    private ListPreference mblock_type;
    private CheckBoxPreference mcall_log_on;
    private CheckBoxPreference msms_log_on;
    // private CheckBoxPreference msms_keyword_on;
    private CheckBoxPreference min_call_on_cdma;
    private CheckBoxPreference min_sms_on_cdma;
    private CheckBoxPreference min_call_on_gsm;
    private CheckBoxPreference min_sms_on_gsm;
    private PreferenceCategory min_network_set;
    private Activity mActivity;

  //add by fwr687 begin IKDINARACG-2366
    private static final String EXTRA_FIREWALL_TYPE = "firewall_type";
    private static final int CALL_CHANGE = 1;
    private static final int SMS_CHANGE = 2;
  //add by fwr687 end

    static final String[] SETTING_PROJECTION = new String[] {
        Settings._ID, // 0
        Settings.NAME, // 1
        Settings.VALUE // 2
    };

    static final int ID_COLUMN_INDEX = 0;
    static final int NAME_COLUMN_INDEX = 1;
    static final int VALUE_COLUMN_INDEX = 2;

    private String min_call_on_value;
    private String min_sms_on_value;
    private String mInblock_call_list_value;
    private String mInblock_sms_list_value;
    private String mblock_type_value;
    private String min_call_on_cdma_value;
    private String min_sms_on_cdma_value;
    private String min_call_on_gsm_value;
    private String min_sms_on_gsm_value;
    static private boolean misDualmode = false; // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS


    @Override
    public void onAttach (Activity activity){
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.firewall_setting_new);
        misDualmode = isDualMode();
        // misDualmode = PhoneModeManager.isDmds(); // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View settingLayout = inflater.inflate(R.layout.preference_list_content, container, false);

        return settingLayout;
    }

    @Override
    public void onResume() {
        ContentResolver mresolver = mActivity.getContentResolver();

        min_call_on_value = getDBValue(mresolver, Settings.INBLOCK_CALL_ON);
        min_sms_on_value = getDBValue(mresolver, Settings.INBLOCK_SMS_ON);
        mInblock_call_list_value = getDBValue(mresolver, Settings.IN_BLOCK_CALL_LIST);
        mInblock_sms_list_value = getDBValue(mresolver, Settings.IN_BLOCK_SMS_LIST);
        mblock_type_value = getDBValue(mresolver, Settings.CALL_BLOCK_TYPE);

        min_call_on = (CheckBoxPreference) findPreference("inblock_call_on");
        min_call_on.setChecked(Boolean.valueOf(min_call_on_value));
        // mout_call_on = (CheckBoxPreference) findPreference("outblock_call_on");
        min_sms_on = (CheckBoxPreference) findPreference("inblock_sms_on");
        min_sms_on.setChecked(Boolean.valueOf(min_sms_on_value));
        mInblock_call_list = (ListPreference) findPreference("in_block_call_list");
        mInblock_call_list.setValue(mInblock_call_list_value);
        // mOutblock_call_list = (ListPreference) findPreference("out_block_call_list");
        mInblock_sms_list = (ListPreference) findPreference("in_block_sms_list");
        mInblock_sms_list.setValue(mInblock_sms_list_value);
        mblock_type = (ListPreference) findPreference("reject_type");
        mblock_type.setValue(mblock_type_value);
        mcall_log_on = (CheckBoxPreference) findPreference("call_log_on");
        msms_log_on = (CheckBoxPreference) findPreference("sms_log_on");
        // msms_keyword_on = (CheckBoxPreference) findPreference("keywords_filter_on");
        min_network_set = (PreferenceCategory) findPreference("in_network_setting");

        min_call_on.setOnPreferenceChangeListener(mPChangeListener);
        // mout_call_on.setOnPreferenceChangeListener(mPChangeListener);
        min_sms_on.setOnPreferenceChangeListener(mPChangeListener);
        mInblock_call_list.setOnPreferenceChangeListener(mPChangeListener);
        // mOutblock_call_list.setOnPreferenceChangeListener(mPChangeListener);
        mInblock_sms_list.setOnPreferenceChangeListener(mPChangeListener);
        mblock_type.setOnPreferenceChangeListener(mPChangeListener);
        mcall_log_on.setOnPreferenceChangeListener(mPChangeListener);
        msms_log_on.setOnPreferenceChangeListener(mPChangeListener);
        // msms_keyword_on.setOnPreferenceChangeListener(mPChangeListener);
        if (misDualmode) { // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
            min_call_on_cdma_value = getDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA);
            min_sms_on_cdma_value = getDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA);
            min_call_on_gsm_value = getDBValue(mresolver, Settings.IN_CALL_REJECT_GSM);
            min_sms_on_gsm_value = getDBValue(mresolver, Settings.IN_SMS_REJECT_GSM);

            min_call_on_cdma = (CheckBoxPreference) findPreference("in_call_reject_cdma");
            min_sms_on_cdma = (CheckBoxPreference) findPreference("in_sms_reject_cdma");
            min_call_on_gsm = (CheckBoxPreference) findPreference("in_call_reject_gsm");
            min_sms_on_gsm = (CheckBoxPreference) findPreference("in_sms_reject_gsm");

            min_call_on_cdma.setChecked(Boolean.valueOf(min_call_on_cdma_value));
            min_sms_on_cdma.setChecked(Boolean.valueOf(min_sms_on_cdma_value));
            min_call_on_gsm.setChecked(Boolean.valueOf(min_call_on_gsm_value));
            min_sms_on_gsm.setChecked(Boolean.valueOf(min_sms_on_gsm_value));

            min_call_on_cdma.setOnPreferenceChangeListener(mPChangeListener);
            min_sms_on_cdma.setOnPreferenceChangeListener(mPChangeListener);
            min_call_on_gsm.setOnPreferenceChangeListener(mPChangeListener);
            min_sms_on_gsm.setOnPreferenceChangeListener(mPChangeListener);
        } else {
            if (min_network_set != null) {
                getPreferenceScreen().removePreference(min_network_set);
            }
        }
        super.onResume();
        setItemSum();
        manageFirewallService();
    }

    private OnPreferenceChangeListener mPChangeListener = new OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ContentResolver mresolver = mActivity.getContentResolver();
            String mkey = preference.getKey();
            if (preference instanceof CheckBoxPreference) {
                ((CheckBoxPreference) preference).setChecked((Boolean) newValue);
                boolean before = isVipMode(mresolver);
                if (Settings.INBLOCK_CALL_ON.equals(mkey) || Settings.INBLOCK_SMS_ON.equals(mkey)
                    || Settings.IN_CALL_REJECT_CDMA.equals(mkey) || Settings.IN_SMS_REJECT_CDMA.equals(mkey)
                    || Settings.IN_CALL_REJECT_GSM.equals(mkey) || Settings.IN_SMS_REJECT_GSM.equals(mkey)) {
                    //add by fwr687 begin IKDINARACG-2366
                    if(Settings.INBLOCK_CALL_ON.equals(mkey))
                        notifyChangeToSmartAction(CALL_CHANGE);
                    else if(Settings.INBLOCK_SMS_ON.equals(mkey))
                        notifyChangeToSmartAction(SMS_CHANGE);
                    //add by fwr687 end
                    setDBValue(mActivity.getContentResolver(), mkey, ((Boolean)newValue).toString());
                }
                boolean after = isVipMode(mresolver);
                if (before != after)
                    mActivity.stopService(new Intent("com.motorola.firewall.action.STOP"));

                setItemSum();
                manageFirewallService();
                return true;
            } else if (preference instanceof ListPreference) {
                ((ListPreference) preference).setValue((String) newValue);
                boolean before = isVipMode(mresolver);
                if (Settings.IN_BLOCK_CALL_LIST.equals(mkey) || Settings.IN_BLOCK_SMS_LIST.equals(mkey)
                        || Settings.CALL_BLOCK_TYPE.equals(mkey)) {
                    //add by fwr687 begin IKDINARACG-2366
                    if(Settings.IN_BLOCK_CALL_LIST.equals(mkey))
                        notifyChangeToSmartAction(CALL_CHANGE);
                    else if(Settings.IN_BLOCK_SMS_LIST.equals(mkey))
                        notifyChangeToSmartAction(SMS_CHANGE);
                    //add by fwr687 end
                    setDBValue(mActivity.getContentResolver(), mkey, (String) newValue);
                }
                boolean after = isVipMode(mresolver);
                if (before != after)
                    mActivity.stopService(new Intent("com.motorola.firewall.action.STOP"));

                setItemSum();
                manageFirewallService();
                return true;
            }
            return true;
        }
    };

    private void manageFirewallService() {
        ContentResolver mresolver = mActivity.getContentResolver();
        if ( misDualmode ) { // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
            if (min_call_on.isChecked() || min_sms_on.isChecked() || min_call_on_cdma.isChecked()
                    || min_call_on_gsm.isChecked() || min_sms_on_cdma.isChecked() || min_sms_on_gsm.isChecked()) {
                mActivity.startService(new Intent("com.motorola.firewall.action.START"));
            } else if ((!min_call_on.isChecked()) && (!min_sms_on.isChecked()) && (!min_call_on_cdma.isChecked())
                    && (!min_call_on_gsm.isChecked()) &&  (!min_sms_on_cdma.isChecked()) &&  (!min_sms_on_gsm.isChecked())) {
                mActivity.stopService(new Intent("com.motorola.firewall.action.STOP"));

            } else {
                mActivity.startService(new Intent("com.motorola.firewall.action.UPDATE"));
            }
        } else {
            if (min_call_on.isChecked() || min_sms_on.isChecked() ) {
                mActivity.startService(new Intent("com.motorola.firewall.action.START"));
            } else if ((!min_call_on.isChecked()) && (!min_sms_on.isChecked())) {
                mActivity.stopService(new Intent("com.motorola.firewall.action.STOP"));
            } else {
                mActivity.startService(new Intent("com.motorola.firewall.action.UPDATE"));
            }
        }
    }


    private void setItemSum() {
        mInblock_call_list.setSummary(mInblock_call_list.getEntry());
        // mOutblock_call_list.setSummary(mOutblock_call_list.getEntry());
        mInblock_sms_list.setSummary(mInblock_sms_list.getEntry());

        mblock_type.setSummary(mblock_type.getEntry());

    }

   public static boolean isDualMode() {
        String dual = SystemProperties.get("ro.telephony.dualmode");
        if (dual != null && dual.equals("yes")){
            return true;
        } else {
            return false;
        }
    }


    public static boolean isVipMode(ContentResolver mresolver) {
        if (isDualMode()) { // MOT Calling Code  - IKCNDEVICS-37: firewall app bringup on ICS
            return
                getDBValue(mresolver, Settings.INBLOCK_CALL_ON).equals("true")
             && getDBValue(mresolver, Settings.INBLOCK_SMS_ON).equals("true")
             && getDBValue(mresolver, Settings.IN_BLOCK_CALL_LIST).equals("in_call_whitelist")
             && getDBValue(mresolver, Settings.IN_BLOCK_SMS_LIST).equals("in_sms_whitelist")
             && getDBValue(mresolver, Settings.IN_CALL_REJECT_CDMA).equals("false")
             && getDBValue(mresolver, Settings.IN_SMS_REJECT_CDMA).equals("false")
             && getDBValue(mresolver, Settings.IN_CALL_REJECT_GSM).equals("false")
             && getDBValue(mresolver, Settings.IN_SMS_REJECT_GSM).equals("false");
        }
        else {
            return
                getDBValue(mresolver, Settings.INBLOCK_CALL_ON).equals("true")
             && getDBValue(mresolver, Settings.INBLOCK_SMS_ON).equals("true")
             && getDBValue(mresolver, Settings.IN_BLOCK_CALL_LIST).equals("in_call_whitelist")
             && getDBValue(mresolver, Settings.IN_BLOCK_SMS_LIST).equals("in_sms_whitelist");
        }
    }

    public static String getDBValue(ContentResolver resolver, String columnName) {
        String selection = Settings.NAME + "=?";
        String result = getDBDefaultValue(columnName);
        Cursor cursor = resolver.query(Settings.CONTENT_URI, SETTING_PROJECTION, selection, new String[] {columnName}, Settings.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            if ( cursor.moveToFirst() ) {
                result = cursor.getString(VALUE_COLUMN_INDEX);
            }
            cursor.close();
        }
        return result;
    }

    public static int setDBValue(ContentResolver resolver, String columnName, String value) {
        int result = 0;
        String selection = Settings.NAME + "=?";
        ContentValues mcvalue = new ContentValues();
        mcvalue.put(Settings.NAME, columnName);
        mcvalue.put(Settings.VALUE, value);
        result = resolver.update(Settings.CONTENT_URI, mcvalue, selection, new String[] {columnName});
        if (result == 0) {
            resolver.insert(Settings.CONTENT_URI, mcvalue);
            result = 1;
        }
        return result;
    }

    public static String getDBDefaultValue(String columnName) {
        if (TextUtils.isEmpty(columnName)) {
            return null;
        }
        if (Settings.INBLOCK_CALL_ON.equals(columnName) || Settings.INBLOCK_SMS_ON.equals(columnName)
            || Settings.IN_CALL_REJECT_CDMA.equals(columnName) || Settings.IN_SMS_REJECT_CDMA.equals(columnName)
            || Settings.IN_CALL_REJECT_GSM.equals(columnName) || Settings.IN_SMS_REJECT_GSM.equals(columnName)) {
            return "false";
        } else if (Settings.VINBLOCK_CALL_ON.equals(columnName) || Settings.VINBLOCK_SMS_ON.equals(columnName)
            || Settings.VIN_CALL_REJECT_CDMA.equals(columnName) || Settings.VIN_SMS_REJECT_CDMA.equals(columnName)
            || Settings.VIN_CALL_REJECT_GSM.equals(columnName) || Settings.VIN_SMS_REJECT_GSM.equals(columnName)) {
            return "false";
        } else if (Settings.IN_BLOCK_CALL_LIST.equals(columnName)) {
            return Settings.IN_BLOCK_CALL_LIST_VALUES[3];
        } else if (Settings.IN_BLOCK_SMS_LIST.equals(columnName)) {
            return Settings.IN_BLOCK_SMS_LIST_VALUES[3];
        } else if (Settings.VIN_BLOCK_CALL_LIST.equals(columnName)) {
            return Settings.IN_BLOCK_CALL_LIST_VALUES[3];
        } else if (Settings.VIN_BLOCK_SMS_LIST.equals(columnName)) {
            return Settings.IN_BLOCK_SMS_LIST_VALUES[3];
        } else if (Settings.CALL_BLOCK_TYPE.equals(columnName)) {
            return Settings.CALL_BLOCK_TYPE_VALUES[1];
        } else {
            return null;
        }
    }

    private void notifyChangeToSmartAction(int type){
        Intent intent = new Intent("com.motorola.firewall.MODE_CHANGE");
        intent.putExtra(EXTRA_FIREWALL_TYPE, type);
        mActivity.sendBroadcast(intent);
    }

}
