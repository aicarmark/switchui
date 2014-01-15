/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.contacts;

import com.android.contacts.activities.DialtactsActivity;
import com.android.internal.telephony.ITelephony;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Telephony.Intents;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import android.os.SystemProperties; // MOTO A Okehee.Goh@Motorola
import android.os.Build;

// [ MOTO:Begin A EAC053C@Motorola  Subsidy Lock
import android.app.ActivityManagerNative;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.telephony.TelephonyManager.PHONE_TYPE_CDMA;
import static android.telephony.TelephonyManager.PHONE_TYPE_GSM;
// ] MOTO:End A Subsidy Lock

// [ MOTO:Begin A  Log Checkin
/* to-pass-build, Xinyu Liu/dcjf34 */ 
//import com.motorola.blur.service.blur.checkin.CheckinUtils;
import android.os.Process;
import android.os.ServiceManager;
// ] MOTO:End A  Log Checkin
import com.motorola.android.telephony.PhoneModeManager;
import com.android.internal.telephony.IPhoneSubInfo;

/**
 * Helper class to listen for some magic character sequences
 * that are handled specially by the dialer.
 *
 * Note the Phone app also handles these sequences too (in a couple of
 * relativly obscure places in the UI), so there's a separate version of
 * this class under apps/Phone.
 *
 * TODO: there's lots of duplicated code between this class and the
 * corresponding class under apps/Phone.  Let's figure out a way to
 * unify these two classes (in the framework? in a common shared library?)
 */
public class SpecialCharSequenceMgr {
    private static final String TAG = "SpecialCharSequenceMgr";
    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    private static final String MMI_IMEI_DISPLAY = "*#06#";
    private static final String USSD_SW_VERSION_DISPLAY = "*#9999#"; // MOTO A Okehee.Goh@Motorola
    private static final String USSD_HW_VERSION_DISPLAY = "*#8888#"; // MOTO A Okehee.Goh@Motorola
    private static final String NWSCP_DISPLAY = "#073887*";  // MOTO A EAC053C@Motorola Special Char Seq for Subsidy Lock
    private static final String LOG_CHECKIN_SVC = "#35468#"; // MOTO A Okehee.Goh@Motorola  Key for log checkin


    private static final String MMI_CT_PROP_DISPLAY = "*#0000#";//for CT tita
    private static final String HSTCMD = "*#563289#";
    private static final String HSTCMD_ACTIVITY = "com.motorola.android.hstcmd";

    /** This class is never instantiated. */
    private SpecialCharSequenceMgr() {
    }

    static boolean handleChars(Context context, String input) {
        return handleChars(context, input, false, null, null);
    }

    public static boolean handleChars(Context context, String input, EditText textField) {
        return handleChars(context, input, false, null,textField);
    }


    /**
     * Generally used for the PUK unlocking case, where we
     * want to be able to maintain a handle to the calling
     * activity so that we can close it or otherwise display
     * indication if the PUK code is recognized.
     *
     * NOTE: The counterpart to this file in Contacts does
     * NOT contain the special PUK handling code, since it
     * does NOT need it.  When the device gets into PUK-
     * locked state, the keyguard comes up and the only way
     * to unlock the device is through the Emergency dialer,
     * which is still in the Phone App.
     */
    static boolean handleChars(Context context, String input, Activity pukInputActivity) {
        return handleChars(context, input, false, pukInputActivity, null);
    }

    static boolean handleChars(Context context, String input, boolean useSystemWindow) {
        return handleChars(context, input, useSystemWindow, null, null);
    }

    /**
     * Check for special strings of digits from an input
     * string.
     *
     * @param context input Context for the events we handle
     * @param input the dial string to be examined
     * @param useSystemWindow used for the IMEI event to
     * determine display behaviour.
     * @param pukInputActivity activity that originated this
     * PUK call, tracked so that we can close it or otherwise
     * indicate that special character sequence is
     * successfully processed.
     */
    static boolean handleChars(Context context,
                               String input,
                               boolean useSystemWindow,
                               Activity pukInputActivity,
            EditText textField) {

        //get rid of the separators so that the string gets parsed correctly
        String dialString = PhoneNumberUtils.stripSeparators(input);

        //MOT Calling code - IKSTABLEFOUR-106 - Subsidy lock is only for GSM phone
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        int phoneType = tm.getPhoneType();
        if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
            if (handleIMEIDisplay(context, dialString, useSystemWindow)
                || handleVersionDisplay(context, dialString, useSystemWindow) // MOTO A Okehee.Goh@Motorola
                || handlePinEntry(context, dialString, pukInputActivity)
                || handleAdnEntry(context, dialString,textField)
                || handleNWSCPDisplay(dialString)  // MOTO A EAC053C@Motorola  Subsidy lock
                || handleSecretCode(context, dialString)
                || handleLogCheckinSvc(context, dialString)) {  // MOTO A Okehee.Goh@Motorola  Log checkin
                return true;
            }
        } else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            if (handleIMEIDisplay(context, dialString, useSystemWindow)
                    || handleCTPROPDisplay(context, dialString, useSystemWindow, tm)
                    || handleVersionDisplay(context, dialString, useSystemWindow) // MOTO A Okehee.Goh@Motorola
                    || handlePinEntry(context, dialString, pukInputActivity)
                    || handleAdnEntry(context, dialString, textField)
                    || handleSecretCode(context, dialString)
                    || handleLogCheckinSvc(context, dialString)  // MOTO A Okehee.Goh@Motorola  Log checkin
                    || handleHstcmdEntry(context, dialString)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Handles secret codes to launch arbitrary activities in the form of *#*#<code>#*#*.
     * If a secret code is encountered an Intent is started with the android_secret_code://<code>
     * URI.
     *
     * @param context the context to use
     * @param input the text to check for a secret code in
     * @return true if a secret code was encountered
     */
    static boolean handleSecretCode(Context context, String input) {
        // Secret codes are in the form *#*#<code>#*#*
        int len = input.length();
        if (len > 8 && input.startsWith("*#*#") && input.endsWith("#*#*")) {
            Intent intent = new Intent(Intents.SECRET_CODE_ACTION,
                    Uri.parse("android_secret_code://" + input.substring(4, len - 4)));
            context.sendBroadcast(intent);
            return true;
        }

        return false;
    }

    /**
     * Handle ADN requests by filling in the SIM contact number into the requested
     * EditText.
     *
     * This code works alongside the Asynchronous query handler {@link QueryHandler}
     * and query cancel handler implemented in {@link SimContactQueryCookie}.
     */
    static boolean handleAdnEntry(Context context, String input, EditText textField) {
        /* ADN entries are of the form "N(N)(N)#" */
        if (DBG) log("Inside handleAdnEntry");

        // Begin Mot Calling Code - IKSTABLETWO-8923
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA) {
            return false;
        }
        // End IKSTABLETWO-8923

        // if the phone is keyguard-restricted, then just ignore this
        // input.  We want to make sure that sim card contacts are NOT
        // exposed unless the phone is unlocked, and this code can be
        // accessed from the emergency dialer.
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.inKeyguardRestrictedInputMode()) {
            return false;
        }

        int len = input.length();
        if ((len > 1) && (len < 5) && (input.endsWith("#"))) {
            try {
                // get the ordinal number of the sim contact
                int index = Integer.parseInt(input.substring(0, len-1));

                // The original code that navigated to a SIM Contacts list view did not
                // highlight the requested contact correctly, a requirement for PTCRB
                // certification.  This behaviour is consistent with the UI paradigm
                // for touch-enabled lists, so it does not make sense to try to work
                // around it.  Instead we fill in the the requested phone number into
                // the dialer text field.

                // create the async query handler
                QueryHandler handler = new QueryHandler (context.getContentResolver());

                // create the cookie object
                SimContactQueryCookie sc = new SimContactQueryCookie(index - 1, handler,
                        ADN_QUERY_TOKEN);

                // setup the cookie fields
                sc.contactNum = index - 1;
                sc.setTextField(textField);

                // create the progress dialog
                // making it system so it's not tied to the calling activity
                sc.progressDialog = new ProgressDialog(context); //MOT Calling code - IKSTABLEFOURV-9511
                sc.progressDialog.setTitle(R.string.simContacts_title);
                sc.progressDialog.setMessage(context.getText(R.string.simContacts_emptyLoading));
                sc.progressDialog.setIndeterminate(true);
                sc.progressDialog.setCancelable(true);
                sc.progressDialog.setOnCancelListener(sc);
                //Begin: MOT Calling code - IKSTABLE6-347
                boolean blur_behind_dialog = !context.getResources().getBoolean(com.android.internal.R.bool.config_sf_slowBlur);
                if(VDBG) log("flag com.android.internal.R.bool.config_sf_slowBlur is set to: " + !blur_behind_dialog);
                if (blur_behind_dialog) {
                    sc.progressDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                }
                //End IKSTABLE6-347
                sc.progressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

                // display the progress dialog
                sc.progressDialog.show();

                // run the query.
                handler.startQuery(ADN_QUERY_TOKEN, sc, Uri.parse("content://icc/adn"),
                        new String[]{ADN_PHONE_NUMBER_COLUMN_NAME}, null, null, null);
                if (DBG) log("handleAdnEntry return:true");
                return true;
            } catch (NumberFormatException ex) {
                // Ignore
            }
        }
        if (DBG) log("handleAdnEntry return:false");
        return false;
    }


    static boolean handlePinEntry(Context context, String input, Activity pukInputActivity) {
        // TODO: The string constants here should be removed in favor of some call to a
        // static the MmiCode class that determines if a dialstring is an MMI code.
        if ((input.startsWith("**04") || input.startsWith("**05"))
                && input.endsWith("#")) {
            try {
                //MOT Calling oode - IKMAIN-22581
                ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
                if (telephony != null) {
                    return telephony.handlePinMmi(input);
                }
                //End MOT Calling oode - IKMAIN-22581
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to handlePinMmi due to remote exception");
                return false;
            }
        }
        return false;
    }

    static boolean handleIMEIDisplay(Context context,
                                     String input, boolean useSystemWindow) {
     // IKHSS7-2681, Motorola Korea requirement - do not show IMEI - tkwj46
     if (context.getResources().getBoolean(R.bool.mmi_imei_display)) {
     //End 20120102
        if (input.equals(MMI_IMEI_DISPLAY)) {
        	if (PhoneModeManager.isDmds()) {
                showImeiMeidPanel(context, useSystemWindow);
                return true;
            } else {
        	    TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        	    int phoneType = tm.getPhoneType();
        	    if (phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
        	        showMEIDPanel(context, useSystemWindow);
        	        return true;
        	    } else if (phoneType == TelephonyManager.PHONE_TYPE_GSM) {
        	        showIMEIPanel(context, useSystemWindow);
        	        return true;
        	    }
            }
        }
      }
        return false;
    }

    // TODO: Combine showIMEIPanel() and showMEIDPanel() into a single
    // generic "showDeviceIdPanel()" method, like in the apps/Phone
    // version of SpecialCharSequenceMgr.java.  (This will require moving
    // the phone app's TelephonyCapabilities.getDeviceIdLabel() method
    // into the telephony framework, though.)
    static void showIMEIPanel(Context context, boolean useSystemWindow) {
        if (DBG) log("showIMEIPanel");

        String imeiStr = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE))
                .getDeviceId();

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.imei)
                .setMessage(imeiStr)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    // MOT Calling code - IKPITTSBURGH-309 (LIBss77114)
    static void showMEIDPanel(Context context, boolean useSystemWindow) {
        if (DBG) log("showMEIDPanel");
        // MOT Calling code -CR IKSTABLEFOURV-5495
        String meidStrD1 = "";
        String meidStrD2 = "";
        // MOT Calling code -CR IKSTABLEFOURV-5495 -End

        String meidStrH = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE))
                .getDeviceId();
        if (null != meidStrH && meidStrH.length() >= 8 ) { // MOT Calling code IKMAIN-35715
            //length should greater than 8, otherwise bellow meidStrH.substring(0, 8)  will get IndexOutOfBoundsException
            meidStrH=meidStrH.toUpperCase();

            String meidStrP1 = meidStrH.substring(0, 8);
            String meidStrP2 = meidStrH.substring(8);

            long meidD1 = 0;
            try {
                meidD1 = Long.parseLong(meidStrP1, 16);
            } catch (NumberFormatException e) {
                if (DBG) log("meidStrP1 is not a valid hex long value: " + meidStrP1);
            }
            meidStrD1 = String.format("%10d", meidD1);

            long meidD2 = 0;
            try {
                meidD2 = Long.parseLong(meidStrP2, 16);
            } catch (NumberFormatException e) {
                if (DBG) log("meidStrP2 is not a valid hex long value: " + meidStrP2);
            }
            meidStrD2 = String.format("%08d", meidD2);
        // MOT Calling code -CR IKSTABLEFOURV-5495
        } else {
            if (DBG) log("meidStrH is null pointer or its HEX format lenght not equal 14");
            meidStrH = "";
        }
        // MOT Calling code -CR IKSTABLEFOURV-5495 -End

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.meid)
                .setMessage("HEX:\n"+meidStrH+"\n"+"\nDEC:\n"+meidStrD1+meidStrD2)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show();
    }
    // MOT Calling code - end

    // [ MOTO:Begin A Okehee.Goh@Motorola
    static boolean handleVersionDisplay(Context context,
                                     String input, boolean useSystemWindow) {
        if (input.equals(USSD_SW_VERSION_DISPLAY)) {
            showSWVersionPanel(context, useSystemWindow);
            return true;
        }
        /**modify for IKCBSMMCPPRC-1748 by bphx43 2012-08-23 */
        /*else if (input.equals(USSD_HW_VERSION_DISPLAY)) {
            showHWVersionPanel(context, useSystemWindow);
            return true;
        }*/
        /**modify for IKCBSMMCPPRC-1748 by bphx43 2012-08-23 */

        return false;
    }

    static void showSWVersionPanel(Context context, boolean useSystemWindow) {
        if (DBG) log("showSWVersionPanel");

        String swVersionStr = Build.VERSION.RELEASE;

        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.sw_version_dialog_title)
                .setMessage(swVersionStr)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show();
    }

    static void showHWVersionPanel(Context context, boolean useSystemWindow) {
        if (DBG) log("showHWVersionPanel");

        // This functionality is common to any product.
        // However the implementation in using "ro.radio.hw.version" is
        // specific to Motorola.
        // The property name is changed to "ro.radio.hw.version", which is
        // dependent on the bugs IKMUSTANG-857 (IKMUSTANG-1331 for pass through
        // from sig to modem) and IKMAP-1513
        String hwVersionStr = SystemProperties.get("ro.radio.hw.version");
        
        AlertDialog alert = new AlertDialog.Builder(context)
                .setTitle(R.string.hw_version_dialog_title)
                .setMessage(hwVersionStr)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(false)
                .show();
    }
    // ] MOTO:End A Okehee.Goh@Motorola

    // [ MOTO:Begin A  EAC053C@Motorola Subsidy Lock
    static boolean handleNWSCPDisplay(String input) {
         if (input.equals(NWSCP_DISPLAY)) {
             showNWSCPPanel();
              return true;
           }

           return false;
    }

    static void showNWSCPPanel() {
        /* Broadcast Intent from here */
        Intent intent = new Intent("com.android.phone.ACTION_LAUNCH_SIM_UNLOCK_UI");
        intent.putExtra("ui_type", "NETWORK_LOCKED_UI");
        ActivityManagerNative.broadcastStickyIntent(intent, READ_PHONE_STATE);
    }
    // ] MOTO:End A  Subsidy Lock

    // [ MOTO:Begin A Okehee.Goh@Motorola Log Checkin SVC
    static boolean handleLogCheckinSvc(Context context,
                                     String dialString) {
        if (dialString.equals(LOG_CHECKIN_SVC)) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            //CheckinUtils.sendCheckin(context, Process.myPid());
            new AlertDialog.Builder(context)
                    .setMessage("Log Checkin initiated")
                .setPositiveButton(android.R.string.ok, null)
                .show();
            return true;
        }

        return false;
    }
   //] MOTO:End A  Log Checkin SVC

    private static void log(String msg) {
        Log.d(TAG, "[SpecialCharSequenceMgr] " + msg);
    }

    /* [MOTO: Begin A Adding following classes to keep this file in-sync
                      with Android's contact SpecialCharSequenceMgr file
    */
    /*******
     * This code is used to handle SIM Contact queries
     *******/
    private static final String ADN_PHONE_NUMBER_COLUMN_NAME = "number";
    private static final String ADN_NAME_COLUMN_NAME = "name";
    private static final int ADN_QUERY_TOKEN = -1;

    /**
     * Cookie object that contains everything we need to communicate to the
     * handler's onQuery Complete, as well as what we need in order to cancel
     * the query (if requested).
     *
     * Note, access to the textField field is going to be synchronized, because
     * the user can request a cancel at any time through the UI.
     */
    private static class SimContactQueryCookie implements DialogInterface.OnCancelListener{
        public ProgressDialog progressDialog;
        public int contactNum;

        // Used to identify the query request.
        private int mToken;
        private QueryHandler mHandler;

        // The text field we're going to update
        private EditText textField;

        public SimContactQueryCookie(int number, QueryHandler handler, int token) {
            contactNum = number;
            mHandler = handler;
            mToken = token;
        }

        /**
         * Synchronized getter for the EditText.
         */
        public synchronized EditText getTextField() {
            return textField;
        }

        /**
         * Synchronized setter for the EditText.
         */
        public synchronized void setTextField(EditText text) {
            textField = text;
        }

        /**
         * Cancel the ADN query by stopping the operation and signaling
         * the cookie that a cancel request is made.
         */
        public synchronized void onCancel(DialogInterface dialog) {
            // close the progress dialog
            if (progressDialog != null) {
                progressDialog.dismiss();
            }

            // setting the textfield to null ensures that the UI does NOT get
            // updated.
            textField = null;

            // Cancel the operation if possible.
            mHandler.cancelOperation(mToken);
        }
    }

    /**
     * Asynchronous query handler that services requests to look up ADNs
     *
     * Queries originate from {@link handleAdnEntry}.
     */
    private static class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /**
         * Override basic onQueryComplete to fill in the textfield when
         * we're handed the ADN cursor.
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor c) {
            SimContactQueryCookie sc = (SimContactQueryCookie) cookie;

            // close the progress dialog.
            //MOT Calling code - IKSTABLEFOURV-9511
            try {
            sc.progressDialog.dismiss();
            } catch (IllegalArgumentException e) {
            }
            //END - MOT Calling code - IKSTABLEFOURV-9511

            // get the EditText to update or see if the request was cancelled.
            EditText text = sc.getTextField();

            // if the textview is valid, and the cursor is valid and postionable
            // on the Nth number, then we update the text field and display a
            // toast indicating the caller name.
            if ((c != null) && (text != null) && (c.moveToPosition(sc.contactNum))) {
                String name = c.getString(c.getColumnIndexOrThrow(ADN_NAME_COLUMN_NAME));
                String number = c.getString(c.getColumnIndexOrThrow(ADN_PHONE_NUMBER_COLUMN_NAME));

                // fill the text in.
                text.getText().replace(0, 0, number);

                // display the name as a toast
                Context context = sc.progressDialog.getContext();
                name = context.getString(R.string.menu_callNumber, name);
                Toast.makeText(context, name, Toast.LENGTH_SHORT)
                    .show();
            }

            if(c != null)
            {
                c.close();
            }
        }
    }
    // MOTO: End A
    
    //ChinaDev
    //extended for dual camp
    static void showImeiMeidPanel(Context context, boolean useSystemWindow) {
        String meid = "";
        String imei = "";

        /* to-pass-build, Xinyu Liu/dcjf34 */ 
        int defaultPhoneType = TelephonyManager.getDefault().getPhoneType();//PhoneModeManager.getDefaultPhoneType();
        TelephonyManager secondMgr = null;// TelephonyManager.getDefault(false);

        if(defaultPhoneType == PHONE_TYPE_CDMA)
        {
            meid = TelephonyManager.getDefault().getDeviceId();
            if (secondMgr != null) {
                imei = secondMgr.getDeviceId();
            }
        }
        else if(defaultPhoneType == PHONE_TYPE_GSM) 
        {
            imei = TelephonyManager.getDefault().getDeviceId();
            if (secondMgr != null) {
                meid = secondMgr.getDeviceId();
            }
        }

        String meid_imei = context.getString(R.string.meid_imei_prop, meid, imei);

        AlertDialog alert = new AlertDialog.Builder(context)
            .setMessage(meid_imei)
            .setTitle(R.string.meid_imei_title)
            .setPositiveButton(android.R.string.ok, null)
            .setCancelable(false)
            .create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_PRIORITY_PHONE);
        alert.show();
    }

    static boolean handleCTPROPDisplay(Context context, String input, boolean useSystemWindow, TelephonyManager tm) {
        if (PhoneModeManager.isDmds() == false) {
            if (TelephonyManager.getDefault().getPhoneType() == TelephonyManager.PHONE_TYPE_GSM)
                return false;
        }
        if (input.equals(MMI_CT_PROP_DISPLAY)) {
            showCtPropPanel(context, useSystemWindow, tm);
            return true;
        }
        return false;
    } 

    //get Cdma UIM ID 
    static String getCdmaUIMId(Context context) {
        // if (!SimUtility.isSimReady(TelephonyManager.PHONE_TYPE_CDMA))
        //     return "";
        //as CT reported, to show correct UIMID 
        /* to-pass-build, Xinyu Liu/dcjf34 
        MotorolaCdmaTelephonyManager cdmaTelephonyManager = new MotorolaCdmaTelephonyManager();
        return cdmaTelephonyManager.getUimid();*/
        return "";
    }
    
    static String getCdmaMEID(Context context) {
        String meid ="";
        // if (!SimUtility.isSimReady(TelephonyManager.PHONE_TYPE_CDMA))
        //     return meid;
        
        int defaultPhoneType = TelephonyManager.getDefault().getPhoneType();
        
        if (defaultPhoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            meid = TelephonyManager.getDefault().getDeviceId();
        } else if (PhoneModeManager.isDmds()/* && (TelephonyManager.getDefault(false) != null)*/) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            meid = "";//TelephonyManager.getDefault(false).getDeviceId();
        }        

        return meid;
    }
    
    static String getCdmaPrlVersion(Context context) {
        String prlVersion = "";
        if (PhoneModeManager.isDmds()) {
            /* to-pass-build, Xinyu Liu/dcjf34 */ 
            int defaultPhoneType = TelephonyManager.getDefault().getPhoneType();//PhoneModeManager.getDefaultPhoneType();
            if (defaultPhoneType == PHONE_TYPE_CDMA) {
                IPhoneSubInfo phoneSubInfo = IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
                if (phoneSubInfo != null ) {
                    try {
                        /* to-pass-build, Xinyu Liu/dcjf34 */ 
                        prlVersion = "";//phoneSubInfo.getPrlVersion();
                    } catch (Exception e) {
                        Log.e(TAG, "RemoteException when getPrlVersion");
                    }
                }
            } else if (defaultPhoneType == PHONE_TYPE_GSM) {
                IPhoneSubInfo phoneSubInfo = IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo2"));
                if(phoneSubInfo != null ) {
                    try {
                        /* to-pass-build, Xinyu Liu/dcjf34 */ 
                        prlVersion = "";//phoneSubInfo.getPrlVersion();
                    } catch (Exception e) {
                        Log.e(TAG, "IllegalStateException when getPrlVersion");
                    }
                }
            }
        } else {//for single CDMA phone
            IPhoneSubInfo phoneSubInfo = IPhoneSubInfo.Stub.asInterface(ServiceManager.getService("iphonesubinfo"));
            if(phoneSubInfo != null ) {
                try {
                    /* to-pass-build, Xinyu Liu/dcjf34 */ 
                    prlVersion = "";//phoneSubInfo.getPrlVersion();
                } catch (Exception e) {
                    Log.e(TAG, "RemoteException when getPrlVersion");
                }
            }
        }
        return prlVersion;
    }

    static void showCtPropPanel(Context context, boolean useSystemWindow, TelephonyManager tm) {

        String deviceType = SystemProperties.get("ro.product.model", context.getString(R.string.device_info_default));
        String hardwareVersion = SystemProperties.get("ro.radio.hw.version", context.getString(R.string.device_info_default));
        String softwareVersion = SystemProperties.get("ro.build.id", context.getString(R.string.device_info_default));
        String prlVersion = getCdmaPrlVersion(context);
        String uimIdStr = getCdmaUIMId(context);
        String meidStr = getCdmaMEID(context);
        CdmaCellLocation cellLocation = (CdmaCellLocation) tm.getCellLocation();
        String sid = "";
        String nid = "";
        if (cellLocation != null) {
            sid = String.valueOf(cellLocation.getSystemId());
            nid = String.valueOf(cellLocation.getNetworkId());
        }
        String unknown = context.getString(R.string.device_info_default);
        // display unknown if null
        if (TextUtils.isEmpty(prlVersion)) prlVersion = unknown;
        if (TextUtils.isEmpty(uimIdStr)) uimIdStr = unknown;
        if (TextUtils.isEmpty(meidStr)) meidStr = unknown;
        if (TextUtils.isEmpty(sid)) sid = unknown;
        if (TextUtils.isEmpty(nid)) nid = unknown;
        String version_info = context.getString(R.string.ct_version_info, deviceType, hardwareVersion, softwareVersion, 
        		prlVersion, uimIdStr, meidStr, sid, nid);
        AlertDialog alert = new AlertDialog.Builder(context)
            .setMessage(version_info)
            .setTitle(R.string.version_info)
            .setPositiveButton(android.R.string.ok, null)
            .setCancelable(false)
            .create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_PRIORITY_PHONE);
        alert.show();
    }

    static boolean handleHstcmdEntry(Context context, String input) {
        if (input.equals(HSTCMD)) {
            try {
                Intent intent = new Intent(HSTCMD_ACTIVITY);
                context.startActivity(intent);
            } catch (Exception e) {
                Log.v(TAG, "start the HSTcmdActivity error");
            }
            return true;
        }
        return false;
    }

}
