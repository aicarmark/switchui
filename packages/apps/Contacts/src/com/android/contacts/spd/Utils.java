package com.android.contacts.spd;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.model.HardCodedSources;

//import android.app.Activity;
//import android.app.AlertDialog;
import android.content.ContentUris; // Motorola, Aug-05-2011, IKPIM-282
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
//import android.content.DialogInterface;
//import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
//import android.database.sqlite.SQLiteException;
import android.net.Uri; // Motorola, Aug-05-2011, IKPIM-282
//import android.os.RemoteException;
//import android.os.ServiceManager;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
//import android.provider.CallLog.Calls;
//import android.provider.ContactsContract;
//import android.provider.ContactsContract.RawContacts;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
//import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity; // Motorola, Aug-01-2011, IKMAIN-24377
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.TextView;
import android.widget.Toast; // Motorola, Aug-01-2011, IKMAIN-24377

//import com.android.internal.telephony.ITelephony;
//import com.android.internal.telephony.Phone;
//import com.motorola.android.provider.MotorolaSettings;
//import com.motorola.blur.provider.contacts.Intents;

import java.util.HashMap; // Motorola, July-26-2011, IKMAIN-24377
//import java.util.Hashtable;

//MOTO MOD BEGIN IKHSS7-2038
import com.motorola.internal.telephony.PhoneNumberUtilsExt;
//MOTO MOD END IKHSS7-2038

public class Utils {

    private static final String LOG_TAG = "Utils";
    private static final boolean DBG = (SystemProperties.getInt("ro.debuggable", 0) == 1);
    private static final String DEFAULT_CARD = "com.android.phone.SpeedDialUtils.DefaultCard";
    public static final String DEFAULT_CARD_C = "CDMA";
    public static final String DEFAULT_CARD_G = "GSM";
    public static final String NO_DEFAULT_CARD = "";
    private static final String SHARED_PREFS_FILE = "SpeedDialList";

     /** Identifier for the "Add Call" intent extra. */
//    static final String ADD_CALL_MODE_KEY = "add_call_mode";

    // Covering all screen resolutions supported by the platform
//    /* package */ enum ScreenSize {
//        QVGA,
//        HVGA,
//        WVGA,
//        UNKNOWN
//    }

//    private static final Hashtable<Integer, ScreenSize> SCREEN_SIZE_MAP =
//            new Hashtable<Integer, ScreenSize>();

//    static {
//        SCREEN_SIZE_MAP.put(240 * 320, ScreenSize.QVGA); // QVGA
//        SCREEN_SIZE_MAP.put(240 * 400, ScreenSize.QVGA); // WQVGA
//        SCREEN_SIZE_MAP.put(240 * 432, ScreenSize.QVGA); // FWQVGA
//
//        SCREEN_SIZE_MAP.put(320 * 480, ScreenSize.HVGA); // HVGA
//
//        SCREEN_SIZE_MAP.put(480 * 800, ScreenSize.WVGA); // WVGA
//        SCREEN_SIZE_MAP.put(480 * 854, ScreenSize.WVGA); // FWVGA
//    }

//    static void showInCallScreen() {
//        ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService("phone"));
//        try {
//            if (telephony != null) {  //MOT Calling oode - IKMAIN-22581
//                telephony.showCallScreen();
//            }
//        } catch (RemoteException ex) {
//            // bringing the inCallScreen failed; continue
//        }
//    }

//    /* package */ static ScreenSize getScreenSize(Context context) {
//
//        int x = context.getResources().getDisplayMetrics().widthPixels;
//        int y = context.getResources().getDisplayMetrics().heightPixels;
//
//        ScreenSize sz = SCREEN_SIZE_MAP.get(x * y);
//
//        if (null == sz)
//            return ScreenSize.UNKNOWN;
//
//        return sz;
//    }

    /*
//    static void resetNewCallsFlag() {
//        // run clear query in a separate thread to not load the UI thread
//        new Thread() {
//
//            @Override
//            public void run() {
//                // Mark all "new" missed calls as not new anymore
//                StringBuilder where = new StringBuilder("type=");
//                where.append(Calls.MISSED_TYPE);
//                where.append(" AND new=1");
//
//                ContentValues values = new ContentValues(1);
//                values.put(Calls.NEW, "0");
//
//                // Begin IKSTABLETWO-4644
//                // Reported by monkey test, in some rare/unknown cases, fail opening contacts database.
//                // However, the fix for this issue in F/W was denied due to CTS reason,
//                // so, F/w team suggested we catch SQLiteException exception to avoid crash.
//                try {
//                    DialerApp.getInstance().getContentResolver().update(
//                            Calls.CONTENT_URI, values, where.toString(), null);
//                } catch (SQLiteException e) {
//                    e.printStackTrace();
//                }
//                // End IKSTABLETWO-4644
//            }
//        }.start();
//    }
    */

//    public static void addToContacts(final Context context, final String number) {
//        addToContacts(context,number,null);
//    }

//    public static void addToContacts(final Context context, final String
//            number, final String name) {
//        /* MOTO MOD BEGIN JRB768 IKCBS-1015
//         * Changed calling PhoneNumberUtils.formatNumber API to New API
//         * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
//        */
//        /*
//        String msg = context.getResources().getString(R.string.addToContactsDialog,
//                        PhoneNumberUtilsExt.formatNumber(context, number));
//        //MOTO END IKCBS-1015
//        new AlertDialog.Builder(context)
//                .setTitle(R.string.recentCalls_addToContact)
//                .setMessage(msg)
//                .setPositiveButton(R.string.existingContact, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                        log("@@@ addToContactsDialog - to existing contact: " + number);
//                        Intent intent = new Intent(Intents.ACTION_ADD_TO_EXISTING_CONTACT);
//                        intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
//                        context.startActivity(intent);
//                    }
//                })
//                .setNegativeButton(R.string.newContact,  new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int whichButton) {
//                        log("@@@ addToContactsDialog - to new contact: " + number);
//                        Intent intent = new Intent(Intent.ACTION_INSERT, RawContacts.CONTENT_URI);
//                        intent.putExtra(ContactsContract.Intents.Insert.PHONE, number);
//                        //IKMAIN-10554, a19591, 34425 CNAP feature begin
//                        if(!(TextUtils.isEmpty(name))) {
//                            intent.putExtra(ContactsContract.Intents.Insert.NAME, name);
//                        }
//                        //IKMAIN-10554 end
//                        context.startActivity(intent);
//                    }
//                })
//                .show();
//                */
//    }

    /**
     * Used by TwelveKeyDialer and EmergencyDialer to initialize the keypad
     * Precondition: The activity passed in should implement OnClickListener
     * and OnLongClickListener
     */
    /*
//    public static void setupKeypad(Activity activity) {
//        int[][] keydata =  {
//            {R.id.one,      1,  R.string.dp_one,     R.drawable.ic_control_voicemail},
//            {R.id.two,      0,  R.string.dp_two,     R.string.dp_two_alpha},
//            {R.id.three,    0,  R.string.dp_three,   R.string.dp_three_alpha},
//
//            {R.id.four,     0,  R.string.dp_four,    R.string.dp_four_alpha},
//            {R.id.five,     0,  R.string.dp_five,    R.string.dp_five_alpha},
//            {R.id.six,      0,  R.string.dp_six,     R.string.dp_six_alpha},
//
//            {R.id.seven,    0,  R.string.dp_seven,   R.string.dp_seven_alpha},
//            {R.id.eight,    0,  R.string.dp_eight,   R.string.dp_eight_alpha},
//            {R.id.nine,     0,  R.string.dp_nine,    R.string.dp_nine_alpha},
//
//            {R.id.star,     2,  R.string.dp_star,    0},
//            {R.id.zero,     0,  R.string.dp_zero,    R.string.dp_zero_alpha},
//            {R.id.pound,    1,  R.string.dp_pound,   R.drawable.ic_keypad_space},//MOT Calling Code - IKPIM-433
//        };
//
//        for(int[] r: keydata) {
//            View keyView = activity.findViewById(r[0]);
//            keyView.setSoundEffectsEnabled(false); //MOT Calling Code: IKSTABLEONE-2549
//            keyView.setOnClickListener((View.OnClickListener)activity);
//            ((TextView)keyView.findViewById(R.id.BtnA)).setText(r[2]);
//            if(r[1] == 0) {
//                ((TextView)keyView.findViewById(R.id.BtnB)).setText(r[3]);
//            } else if(r[1] == 1) {
//                if (!(DialerApp.bExcludeVoicemail && (r[0] == R.id.one))) //MOT FID 36927-Speeddial#1 IKCBS-2013
//                    ((ImageView)keyView.findViewById(R.id.ImgBtnB)).setImageResource(r[3]);
//            }
//        }
//        activity.findViewById(R.id.zero).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.two).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.three).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.four).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.five).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.six).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.seven).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.eight).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.nine).setOnLongClickListener((View.OnLongClickListener)activity);
//        // MOT Calling Code - IKPIM-433
//        activity.findViewById(R.id.pound).setOnLongClickListener((View.OnLongClickListener)activity);
//        activity.findViewById(R.id.dial).setOnClickListener((View.OnClickListener)activity);
//        activity.findViewById(R.id.contactAddButton).setOnClickListener((View.OnClickListener)activity);
//    }
*/
//    public static String getLastOutgoingNumber() {
//
//        String outgoingNum="";
//
//        /** The projection to use when querying the call log table */
//    /*
//        String[] CALL_LOG_PROJECTION = new String[] {
//            Calls.NUMBER
//        };
//
//        StringBuilder where = new StringBuilder(Calls.TYPE);
//        where.append("=");
//        where.append(Calls.OUTGOING_TYPE);
//        if (DBG) log("filter: " + where);
//        DialerApp theApp = DialerApp.getInstance();
//        Cursor c = theApp.getContentResolver().query(Calls.CONTENT_URI,CALL_LOG_PROJECTION,
//                    where.toString(), null,Calls.DEFAULT_SORT_ORDER + " LIMIT 1");
//        if(c != null) {
//            if (DBG) log("Cursor count=" + c.getCount());
//            if(c.moveToFirst()){
//                outgoingNum = c.getString(c.getColumnIndex(Calls.NUMBER));
//                if (DBG) log("Recent outgoing call = "+outgoingNum);
//            }
//            c.close();
//        }else{
//        if (DBG) log("No outgoing calls");
//    }
//    */
//        return outgoingNum;
//    }

    // [ MOTO Calling Code CR - IKSHADOW-122, 33638 CityID
    /*
//    public static boolean isNanpNetwork(Context context){
//        boolean isNanpNetwork = true;
//        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
//        if (tm != null) isNanpNetwork = tm.isInNanpNetwork();
//        return isNanpNetwork;
//    }
    */
    /*
     * Place call to CityID lookup here
     * This lookup takes 10-30 ms
     * so can be done inline rather than asynchronously
     */
//    public static CityIdInfo recentCallCityIdLookup(Context context, String phoneNumber, boolean isNanpNetwork, boolean isIncoming)
//    {
//        if (TextUtils.isEmpty(phoneNumber) || context == null)
//            return null;
//
//        CityIdInfo cidInfo = new CityIdInfo();
//        cidInfo.doLookup(context, phoneNumber, isIncoming, isNanpNetwork);
//
//        return cidInfo;
//    }


    public static boolean isVvmAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        String packageName = context.getResources().getString(R.string.vvmPackageName);
        String packageActivityName = context.getResources().getString(R.string.vvmActivityName);
        if((null != packageName) && (null != packageActivityName)) {
            try {
                PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);

                if ((null == info) || (null == info.activities) || (0 == info.activities.length))
                    return false;

                for (int i = 0; i < info.activities.length; i++) {
                    String target = info.activities[i].name;
                    if(packageActivityName.equals(target))
                        return true;
                }

                return false;

            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
        else {
            return false;
        }
    }

//    public static boolean isPhoneCdma(Context context) {
//      TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
//      return((Phone.PHONE_TYPE_CDMA == tm.getPhoneType())?true:false);
//    }

    //MOT FID 36927-Speeddial#1 IKCBS-2013
    static boolean isVoicemailPos(int pos) {
        if (SpeedDialHelper.bExcludeVoicemail) {
            return false;
        }
        return pos == 1;
    }
    //END MOT FID 36927-Speeddial#1 IKCBS-2013

    private static void log(String msg) {
        Log.d(LOG_TAG, "[Utils] " + msg);
    }
    // BEGIN Motorola, July-26-2011, IKMAIN-24377
    /*static private HashMap<Integer, Integer> mSpeedDialPos = new HashMap<Integer, Integer>();
    static {

        mSpeedDialPos.put(1,R.drawable.sd01);
        mSpeedDialPos.put(2,R.drawable.sd02);
        mSpeedDialPos.put(3,R.drawable.sd03);
        mSpeedDialPos.put(4,R.drawable.sd04);
        mSpeedDialPos.put(5,R.drawable.sd05);
        mSpeedDialPos.put(6,R.drawable.sd06);
        mSpeedDialPos.put(7,R.drawable.sd07);
        mSpeedDialPos.put(8,R.drawable.sd08);
        mSpeedDialPos.put(9,R.drawable.sd09);
    };
    public static int getSpdDialPositionDrawable(Context context, int position) {
        return mSpeedDialPos.get(position);
    }*/

    // BEGIN Motorola, Aug-10-2011, IKPIM-282
    public static boolean unassignSpeedDial(Context context, String number) {
        int position = getSpeedDialPosByNumber(context, number);
        if (position != -1 &&
            0 < context.getContentResolver().delete(ContentUris.withAppendedId(SPD_URI, position), null, null)) {
            showSpeedDialChanged(context, number, String.valueOf(position), false);
    // END IKPIM-282
            context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                   .edit()
                   .remove(DEFAULT_CARD + position)
                   .commit();
            return true;
        }
        return false;
    }

    // BEGIN Motorola, Aug-05-2011, IKPIM-282
    public static boolean unassignSpeedDial(Context context, int position) {
        String number = getSpeedDialNumberByPos(context, position);
        if (false == TextUtils.isEmpty(number) &&
            0 < context.getContentResolver().delete(ContentUris.withAppendedId(SPD_URI, position), null, null)) {
            showSpeedDialChanged(context, number, String.valueOf(position), false);
    // END IKPIM-282
            context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                   .edit()
                   .remove(DEFAULT_CARD + position)
                   .commit();
            return true;
        }
        return false;
    }

    public static void assignSpeedDial(Context context, int position, String number) {
        ContentValues values = new ContentValues();
        values.put(NUMBER, number);
        String currentnumber = getSpeedDialNumberByPos(context, position);
        int existPosition = getSpeedDialPosByNumber(context, number);
        if (-1 != existPosition) {
            /* MOTO MOD BEGIN IKHSS7-2038
             * Changed calling PhoneNumberUtils.formatNumber API to New API
             * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
            */
            String confirmationMsg =
                context.getResources().getString(R.string.speedDial_numberAlreadyExists,
                PhoneNumberUtilsExt.formatNumber(context, number, null, ContactsUtils.getCurrentCountryIso(context)));
            //MOTO END IKHSS7-2038
            Toast toast = Toast.makeText(context, confirmationMsg, Toast.LENGTH_SHORT); // TBD
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        } else if (null == currentnumber) {
            Uri uri = context.getContentResolver().insert(ContentUris.withAppendedId(SPD_URI, position), values);
            if (null != uri) {
                showSpeedDialChanged(context, number, String.valueOf(position), true);
            }
        } else {
            int res = context.getContentResolver().update(ContentUris.withAppendedId(SPD_URI, position), values, null, null);
            if (res == 1) {
                showSpeedDialChanged(context, number, String.valueOf(position), true);
            }
        }
    }

    public static void setupDefaultCallCardPrefence(Context context, int position, int defaultCardType){
        switch (defaultCardType){
            case TelephonyManager.PHONE_TYPE_CDMA:
                context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                       .edit()
                       .putString(DEFAULT_CARD + position, DEFAULT_CARD_C)
                       .commit();
                break;
            case TelephonyManager.PHONE_TYPE_GSM:
                context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                       .edit()
                       .putString(DEFAULT_CARD + position, DEFAULT_CARD_G)
                       .commit();
                break;
       }
    }

    public static void showSpeedDialChanged(Context context, String number, String pos, boolean add) {
    // END IKPIM-282
        String confirmationMsg = null;
        String currentCountryIso = ContactsUtils.getCurrentCountryIso(context); //MOTO Dialer Code - IKHSS7-2091
        if(add) {
            /* MOTO MOD BEGIN IKHSS7-2038
             * Changed calling PhoneNumberUtils.formatNumber API to New API
             * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
            */
            confirmationMsg =
                context.getResources().getString(R.string.speedDial_addConfirmation,
                    PhoneNumberUtilsExt.formatNumber(context, number, null, currentCountryIso),pos);
        }
        else {
            confirmationMsg =
                context.getResources().getString(R.string.speedDial_deleteConfirmation,
                    pos, PhoneNumberUtilsExt.formatNumber(context, number, null, currentCountryIso));
            //MOTO END IKHSS7-2038
        }
        Toast toast = Toast.makeText(context, confirmationMsg, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }
    // END IKMAIN-24377
    // BEGIN Motorola, Aug-03-2011, IKPIM-282
    private static final String AUTHORITY = "com.android.contacts.spd";
    private static final Uri CONTENT_URI=Uri.parse("content://" + AUTHORITY);
    private final static Uri SPD_URI = Uri.withAppendedPath(CONTENT_URI, "spd");
    private final static Uri SPD_NUMBER_URI = Uri.withAppendedPath(SPD_URI, "number");
    private final static Uri SPD_FIRST_NEXT_AVAILABLE_URI = Uri.withAppendedPath(SPD_URI, "nextavailable");
    private final static String ID = "ID";
    private final static String NUMBER = "NUMBER";
    private final static String LOCK = "LOCK";

    private final static String PROJECT_SPEEDDIAL[] = new String[] {ID, NUMBER, LOCK};

    protected static final int MAX_SPEED_DIAL_POSITIONS = SpeedDialStorage.MAX_SPEED_DIAL_POSITIONS;

    private static int safeParseInt(String i, int defaultvalue){
        if (null != i) {
            return Integer.parseInt(i);
        }
        return defaultvalue;
    }

    private static String simpleQuery(Context context, Uri uri, String column) {
        String res = null;
        // MOTO Dialer Code - IKHSS6-1376 Begin
        Cursor cur = null;
        try {
            cur = context.getContentResolver().query(uri, PROJECT_SPEEDDIAL, null, null, null);
        } catch (Exception e) {
            cur = null;
            Log.w(LOG_TAG, "got a exception while query " + uri);
            e.printStackTrace();
        }
        // MOTO Dialer Code - IKHSS6-1376 End
        if (null != cur) {
            if (cur.moveToFirst()) {
                res = cur.getString(cur.getColumnIndex(column));
            }
            cur.close();
        }
        return res;
    }

    public static boolean hasSpeedDialByNumber(Context context, String number) {
        return (null != simpleQuery(context, Uri.withAppendedPath(SPD_NUMBER_URI, number), ID));
    }

    public static int getSpeedDialPosByNumber(Context context, String number) {
        return safeParseInt(
                   simpleQuery(context, Uri.withAppendedPath(SPD_NUMBER_URI, number), ID),
                   -1);
    }

    public static String getSpeedDialNumberByPos(Context context, int position) {
        return simpleQuery(context, ContentUris.withAppendedId(SPD_URI, position), NUMBER);
    }

    public static int adviseSpeedDialPosForAssign(Context context) {//MOT FID 36927-Speeddial#1 IKCBS-2013
        return safeParseInt(
                   simpleQuery(context, SPD_FIRST_NEXT_AVAILABLE_URI, ID),
                      1);
    }

    public static boolean isSpeedDialLocked(Context context, int position) {
        return (1 == safeParseInt(
                   simpleQuery(context, ContentUris.withAppendedId(SPD_URI, position), LOCK),
                      0));
    }

    public static boolean tryAssignSpeedDial(Context context, int pos, String number) {
        boolean assigned = false;
        // MOTO Dialer Code - IKHSS6-1376 Begin
        if(number != null) {
            number = PhoneNumberUtils.stripSeparators(number);
        }
        // MOTO Dialer Code - IKHSS6-1376 End

        // BEGIN Motorola, Aug-10-2011, IKPIM-282
        if (!TextUtils.isEmpty(number)) {
            if (!hasSpeedDialByNumber(context, number)) {
        // END IKPIM-282
                if (!isVoicemailPos(pos)) { //MOT FID 36927-Speeddial#1 IKCBS-2013
                    assignSpeedDial(context, // Motorola, July-28-2011, IKMAIN-24377
                        pos, number);
                    assigned = true;
                }
            }
            else {
                /* MOTO MOD BEGIN IKHSS7-2038
                 * Changed calling PhoneNumberUtils.formatNumber API to New API
                 * PhoneNumberUtilsExt.formatNumber for Hyphensation Feature 35615
                */
                String confirmationMsg =
                    context.getResources().getString(R.string.speedDial_numberAlreadyExists,
                    PhoneNumberUtilsExt.formatNumber(context, number, null, ContactsUtils.getCurrentCountryIso(context)));
                //MOTO END IKHSS7-2038
                   Toast toast = Toast.makeText(context, confirmationMsg, Toast.LENGTH_SHORT);
                   toast.setGravity(Gravity.CENTER, 0, 0);
                   toast.show();
            }
        } else {
            Log.e(LOG_TAG, "Picked contact doesn't have a number");
        }
        return assigned;
    }

    public static int getDefaultCallCard(Context context, int position) {
        SharedPreferences pref = context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        String val = pref.getString(DEFAULT_CARD + position, NO_DEFAULT_CARD);
        if (val.equalsIgnoreCase(DEFAULT_CARD_C)) {
            return TelephonyManager.PHONE_TYPE_CDMA;
        } else if(val.equalsIgnoreCase(DEFAULT_CARD_G)) {
            return TelephonyManager.PHONE_TYPE_GSM;
        } else return TelephonyManager.PHONE_TYPE_NONE;
    }

    public static int getDefaultCallCard(Context context, String number) {
        int position = getSpeedDialPosByNumber(context, number);
        int val = getDefaultCallCard(context, position);
        return val;
    }

    // END IKPIM-282

    public static int getLastCallCardType(Context context, String number){
        String selection = Calls.NUMBER + "='" + PhoneNumberUtils.stripSeparators(number) + "'";
        Cursor cursor = context.getContentResolver().query(Calls.CONTENT_URI,
                new String[] {ContactsUtils.CallLog_NETWORK}, selection, null, Calls.DEFAULT_SORT_ORDER);
        if(cursor != null && cursor.moveToFirst()) {
            if(!cursor.isNull(cursor.getColumnIndex(ContactsUtils.CallLog_NETWORK))){
                int callLogNetwork = cursor.getInt(cursor.getColumnIndex(ContactsUtils.CallLog_NETWORK));
                cursor.close();
                return callLogNetwork;
            }
            cursor.close();
        }
        return TelephonyManager.PHONE_TYPE_NONE;
    }

    public static int getContactLocation(Context context, String number) {
        String selection = Data.DATA1 + "='" + PhoneNumberUtils.stripSeparators(number) + "'";
        Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI, new String[] {Data.RAW_CONTACT_ID}, selection, null, null);
        if (null!= cursor && cursor.moveToFirst()) {
            do {
                long rawContactID = cursor.getLong(cursor.getColumnIndex(Data.RAW_CONTACT_ID));
                String sel = RawContacts._ID + "='" + rawContactID + "'";
                Cursor c = context.getContentResolver().query(RawContacts.CONTENT_URI,
                        new String[] {RawContacts.ACCOUNT_NAME}, sel, null, null);
                if (c != null && c.moveToFirst()) {
                    String accountName = c.getString(c.getColumnIndex(RawContacts.ACCOUNT_NAME));
                    if(accountName.equals(HardCodedSources.ACCOUNT_CARD_C)) {
                        cursor.close();
                        c.close();
                        return TelephonyManager.PHONE_TYPE_CDMA;
                    }else if(accountName.equals(HardCodedSources.ACCOUNT_CARD_G)) {
                        cursor.close();
                        c.close();
                        return TelephonyManager.PHONE_TYPE_GSM;
                    }
                    c.close();
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return TelephonyManager.PHONE_TYPE_NONE;
    }

    public static int getSpdDialPositionBgResid(int position, boolean excludeVoicemail) {
        if (position == 1 && !excludeVoicemail) {
            return R.drawable.sd_bg_voicemail;
        } else {
            return R.drawable.sd_bg_add_contact;
        }
    }

    public static int getSpdDialPositionIcResid(int position) {
        int result = R.drawable.sd_ic_9;
        switch (position) {
            case 1 :
                result = R.drawable.sd_ic_1;
                break;
            case 2 :
                result = R.drawable.sd_ic_2;
                break;
            case 3 :
                result = R.drawable.sd_ic_3;
                break;
            case 4 :
                result = R.drawable.sd_ic_4;
                break;
            case 5 :
                result = R.drawable.sd_ic_5;
                break;
            case 6 :
                result = R.drawable.sd_ic_6;
                break;
            case 7 :
                result = R.drawable.sd_ic_7;
                break;
            case 8 :
                result = R.drawable.sd_ic_8;
                break;
            case 9 :
                result = R.drawable.sd_ic_9;
                break;
            default:
                result = R.drawable.sd_ic_9;
                break;
        }
        return result;
    }

}

