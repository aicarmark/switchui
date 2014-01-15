package com.android.contacts;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.RawContacts.Entity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.activities.ContactsCopySIM;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.model.HardCodedSources;
import com.android.internal.telephony.IccCardApplication;
import com.android.internal.telephony.IccCardStatus;
//import com.motorola.android.telephony.IccCardInfo;
import com.motorola.android.telephony.PhoneModeManager;
/*
import com.android.contacts.model.ContactsSource;
import com.android.contacts.model.ExchangeSource;
import com.android.contacts.model.GoogleSource;
import com.android.contacts.model.Sources;
*/
import com.android.contacts.model.AccountType;
import com.android.contacts.model.ExchangeAccountType;
import com.android.contacts.model.GoogleAccountType;
import com.android.contacts.model.AccountTypeManager;

import android.content.Context;
import android.accounts.AccountManager;
import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;

import java.util.List;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.AdnRecord;
import com.android.internal.telephony.IccConstants;
import com.android.internal.telephony.RILConstants;


public class SimUtility{

    // PhoneModeManager.isDmds = false, then use existing single mode interface in stable line
    // public static final boolean stable_line = true; // use stable framework telephony

    private static final String TAG = "SimUtility";
    private static final boolean DEBUG = true;
    
    private static final String[] SIMCARD_COLUMN_NAMES = new String[] {
        "name",
        "number"
    };
    protected static final int SIMCARD_NAME_COLUMN_IDX = 0;
    protected static final int SIMCARD_NUMBER_COLUMN_IDX = 1;

    // XXX: can unified the sim card columns?
    private static final String[] USIM_COLUMN_NAMES = new String[] {
        "name",
        "number",
        "emails",
        "number2"
    };
    public static final String SYNC_EXTRAS_MOTOROLA_NO_DATA_CONNECTION_CHECK = "SyncExtraMotorolaNoDataConnectionCheck";    
        
    protected static final int USIM_NAME_COLUMN_IDX = 0;
    protected static final int USIM_NUMBER_COLUMN_IDX = 1;
    protected static final int USIM_EMAIL_COLUMN_IDX = 2;
    protected static final int USIM_SECOND_NUMBER_COLUMN_IDX = 3;

    private static final String[] ACCOUNT_NAME_TYPE = new String[] {
        RawContacts.ACCOUNT_NAME,
        RawContacts.ACCOUNT_TYPE
    };
    protected static final int ACCOUNT_NAME_IDX = 0;
    protected static final int ACCOUNT_TYPE_IDX = 1;
    public static class PeopleInfo
    {
        public long peopleId;
        public String peopleName;
        public String primaryNumber;
        public String secondaryNumber;
        public String primaryEmail;
        public int diffStatus;
    }

    // refer to CR LIBtt74742, only set sLoaded to false during LoadSIM service running
    // thus it does not allow user to new/edit/delete contacts during SIM loading
    // altough the sLoaded is true when power up, but it is set to false immediately once LoadSIM service running
    // it also fix the issue that the acore process crash and the flag is cleared
    private static boolean sLoaded = true;

    public static final int SIM_TYPE_UNKNOWN = 0;
    public static final int SIM_TYPE_SIM = 1;
    public static final int SIM_TYPE_USIM = 2;

    public static final Uri SIM_CONTENT_URI = Uri.parse("content://icc/adn");
    public static final Uri SIM_CONTENT_URI2 = Uri.parse("content://icc/adn2");

    public static final Uri SIM_FREESPACE_URI = Uri.parse("content://icc/freeadn");
    public static final Uri SIM_FREESPACE_URI2 = Uri.parse("content://icc/freeadn2");
    public static final Uri SIM_CAPACITY_URI = Uri.parse("content://icc/adncapacity");
    public static final Uri SIM_CAPACITY_URI2 = Uri.parse("content://icc/adncapacity2");

    public static final Uri SIM_CAPABILITY_URI = Uri.parse("content://icc/capability/pbk");

    public static final Uri SIM_FREEEMAIL_URI = Uri.parse("content://icc/freeemail");

    public static final int SIM_NAME_LENGTH_CHN = 6;
    public static final int SIM_NAME_LENGTH_ENG = 14;

    public static final int DIFF_INIT = 0;
    public static final int DIFF_NEW = 1;
    public static final int DIFF_NORMAL = 2;

//    public static final String DISPLAY_NAME = "display_name";

    public static final int INSUFFICIENT_SPACE_FAILURE = -100;
    public static final int PHONE_NUMBER_LENGTH_FAILURE = -101;
    public static final int EMAIL_LENGTH_FAILURE = -102;
    public static final int EMAIL_PARTITION_FULL_FAILURE = -103;
    public static final int MAX_SIM_PHONE_NUMBER_LENGTH = 20;
    public static final int MAX_SIM_EMAIL_LENGTH = 40;

    private static int sCCardType = SIM_TYPE_UNKNOWN;
    private static int sGCardType = SIM_TYPE_UNKNOWN;

    private static final int SIM_ADD_BATCH = 20;
    // The maximum number of operations per yield point is 500, set 400 as optimized size
    private static final int MAX_BATCH_SIZE = 400; 

    /////////////////////////////////////////////////////////////////
    ///////////////// Sim Card Management APIs //////////////////////
    /////////////////////////////////////////////////////////////////


    /**
     * Build phone number for sim card storage
     *
     * Sim card only allow 0-9, *, #, p in its stored number. but
     * Google style will add '-' between numbers, which would cause
     * low level application crash.
     * @param phoneNumber Google style phone number.
     * @return Phone number contain 0-9, *, #, p only.
     */

    // make sure don't pass null to IccProvider in every case.
    public static void buildSIMPeople(PeopleInfo pInfo) {
        if (pInfo.peopleName == null)
            pInfo.peopleName = "";
        if (pInfo.primaryNumber == null)
            pInfo.primaryNumber = "";
        if (pInfo.secondaryNumber == null)
            pInfo.secondaryNumber = "";
        if (pInfo.primaryEmail == null)
            pInfo.primaryEmail = "";
    }


    public static String buildSimNumber(String phoneNumber) {
        if (phoneNumber == null)
            return "";

        String ret = phoneNumber.replaceAll("[^0-9*#p+,;]*", "");

        if (ret == null) return "";
        return ret;
    }

    public static String buildSimString(String cont) {
        if (cont == null) {
            Log.i(TAG, "Return empty string instead of null");
            return "";
        }
        String ret = cont.trim();
        if (ret == null) return "";
        return ret;
    }

    public static String buildSimName(String name) {
        if (name == null) {
            Log.i(TAG, "Return empty name instead of null");
            return "";
        }
        String tname = name.trim();
        String ret = null;
        int max_length = 0;
        if (isEngName(tname))
            max_length = SIM_NAME_LENGTH_ENG;
        else
            max_length = SIM_NAME_LENGTH_CHN;

        if (tname.length() > max_length)
            ret = tname.substring(0, max_length);
        else
            ret = tname;
        if (ret == null) return "";
        return ret;
    }

    public static String buildOldSimName(String name) {
        if (name == null) {
            Log.i(TAG, "buildOldSimName, Return empty name instead of null");
            return "";
        }
        return name;
    }

    public static boolean isEngName(String name) {
        byte[] bytes = name.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            if (b > 127) return false;
        }
        return true;
    }


    public static String getAccountNameByType(int type)
    {
        if (PhoneModeManager.isDmds()) {
            if (type == TelephonyManager.PHONE_TYPE_CDMA)
                return HardCodedSources.ACCOUNT_CARD_C;
            else if (type == TelephonyManager.PHONE_TYPE_GSM)
                return HardCodedSources.ACCOUNT_CARD_G;
            else
                return null;
        } else {
            return HardCodedSources.ACCOUNT_CARD;
        }
    }

    public static int getTypeByAccountName(String accountName)
    {
        if (TextUtils.equals(accountName, HardCodedSources.ACCOUNT_CARD_C))
            return TelephonyManager.PHONE_TYPE_CDMA;
        else if (TextUtils.equals(accountName, HardCodedSources.ACCOUNT_CARD_G))
            return TelephonyManager.PHONE_TYPE_GSM;
        else if (TextUtils.equals(accountName, HardCodedSources.ACCOUNT_CARD))
            return TelephonyManager.getDefault().getPhoneType();
        else
            return TelephonyManager.PHONE_TYPE_NONE;
    }

    public static ContactPhotoManager.DefaultImageProvider getCardPhotoByAccountName(String accountName)
    {
        ContactPhotoManager.DefaultImageProvider cardPhoto = ContactPhotoManager.CARD_AVATER;
        if (PhoneModeManager.isDmds() && accountName != null) {
            final int phoneType = getTypeByAccountName(accountName);
            if (TelephonyManager.PHONE_TYPE_CDMA == phoneType) {
                cardPhoto = ContactPhotoManager.CCARD_AVATER;
            } else if (TelephonyManager.PHONE_TYPE_GSM == phoneType) {
                cardPhoto = ContactPhotoManager.GCARD_AVATER;
            }
        }
        return cardPhoto;
    }

    /**
     * check the sim card TYPE in current phone device
     *
     * @return SIM_TYPE_UNKNOWN | SIM_TYPE_SIM | SIM_TYPE_USIM
     */

    // reset and query SIM type each time before query the SIM contacts, refer to CR LIBtt41944

    public static void resetSimType(int type)
    {
        if (type == TelephonyManager.PHONE_TYPE_CDMA) {
            sCCardType = SIM_TYPE_UNKNOWN;
        }
        if (type == TelephonyManager.PHONE_TYPE_GSM) {
            sGCardType = SIM_TYPE_UNKNOWN;
        }
    }

    public static void querySimType(ContentResolver resolver, int type)
    {
        if (!PhoneModeManager.isDmds()) {
            sCCardType = SIM_TYPE_UNKNOWN;
        }
        if (type == TelephonyManager.PHONE_TYPE_CDMA) {
            sCCardType = getIccType(resolver, type);
        }
        if (type == TelephonyManager.PHONE_TYPE_GSM) {
            sGCardType = getIccType(resolver, type);
        }
    }

    // in case the acore process crash, we need make sure the sim type is valid
    // query all sim type each time enter contacts app
    public static void queryAllSimType(ContentResolver resolver)
    {
        // if the sim is loading, then the LoadSimContactsService is querying the sim type.
        if (!sLoaded)
            return;
        if (PhoneModeManager.isDmds()) {
        	  if (sCCardType == SIM_TYPE_UNKNOWN)
                querySimType(resolver, TelephonyManager.PHONE_TYPE_CDMA);
        	  if (sGCardType == SIM_TYPE_UNKNOWN)
                querySimType(resolver, TelephonyManager.PHONE_TYPE_GSM);
        } else {
            int type = TelephonyManager.getDefault().getPhoneType();
            if  (((type == TelephonyManager.PHONE_TYPE_CDMA) && (sCCardType == SIM_TYPE_UNKNOWN))
                || ((type == TelephonyManager.PHONE_TYPE_GSM) && (sGCardType == SIM_TYPE_UNKNOWN)))
            querySimType(resolver, type);
        }
    }

    // in case the acore process crash, the sCCardType and sGCardType reset to SIM_TYPE_UNKNOWN, need query again
    // SimUtility class, the sGCardTypes, GCardType and sLoaded need be considerd in process crash.
    // as the querySimType add 1 parameter (resolver) to query the single mode sim type, here we don't call querySimType
    // to reduce the change, we call querySimType each time eneter contacts app
    public static int getSimType(int type)
    {
        if (!PhoneModeManager.isDmds() && (type != TelephonyManager.getDefault().getPhoneType())) {
            return SIM_TYPE_UNKNOWN;
        }
        if (type == TelephonyManager.PHONE_TYPE_CDMA) {
            return  sCCardType;
        }
        if (type == TelephonyManager.PHONE_TYPE_GSM) {
            return  sGCardType;
        }
        return SIM_TYPE_UNKNOWN;
    }


    /**
     * query all contacts in SIM card.
     * the information objects will be added into
     * ArrayList<PeopleInfo> peopleList.
     *
     * @param resolver the content resolver
     * @param peopleList all the group member information object will add into this list
     * @return true if the operation succeeded
     */
    public static boolean getSimCardContacts(ContentResolver resolver, ArrayList<PeopleInfo> peopleList, int type)
    {
        if (null == ServiceManager.getService("simphonebook")) {
            Log.w(TAG, "getSimCardContacts, simphonebook service is not ready!");
            return false;
        }

        if (type == TelephonyManager.PHONE_TYPE_NONE)
            return false;

        int simType = getSimType(type);
        if(simType == SIM_TYPE_UNKNOWN)
            return false;

        if(simType == SIM_TYPE_SIM)
            return get_Sim_Contacts(resolver, peopleList, type);
        else if(simType == SIM_TYPE_USIM)
            return get_USim_Contacts(resolver, peopleList, type);
        else
            return false;
    }


    /**
     * an internal function to query the Sim card contacts
     */
    private static boolean get_Sim_Contacts(ContentResolver resolver, ArrayList<PeopleInfo> peopleList, int type)
  {
        Uri simUri = getUriByType(type);
        if (simUri == null) return false;

        Log.d(TAG,"get_Sim_Contacts uri="+simUri);
        Cursor cursor = resolver.query(
                simUri,
                SIMCARD_COLUMN_NAMES,
                null,
                null,
                null);

            if (cursor != null) {
            try {
                while(cursor.moveToNext()) {
                    PeopleInfo pinfo = new PeopleInfo();
                    pinfo.peopleId = -1;
                    pinfo.peopleName = cursor.getString(SIMCARD_NAME_COLUMN_IDX);
                    pinfo.primaryNumber = cursor.getString(SIMCARD_NUMBER_COLUMN_IDX);
                    Log.d(TAG,"get_Sim_Contacts name=" + pinfo.peopleName + " Number" + pinfo.primaryNumber);
                    peopleList.add(pinfo);
                }
            }
            catch(Exception ex){
                Log.d(TAG,"get_Sim_Contacts ex=" + ex);
                return false;
            }
            finally {
                cursor.close();
            }
            return true;
        }

        Log.i(TAG, "sim card query failed!!");
        return false;
    }

    /**
     * an internal function to query the USIM card contacts
     */
    private static boolean get_USim_Contacts(ContentResolver resolver, ArrayList<PeopleInfo> peopleList, int type)
    {
        Uri simUri = getUriByType(type);
        if (simUri == null) {
            Log.e(TAG, "get_USim_Contacts(), simUri = null, type="+type+", resolver="+resolver);
            return false;
        }

        Cursor cursor = resolver.query(simUri,
                USIM_COLUMN_NAMES,
                null,
                null,
                null);

        if (cursor != null) {
            try {
                while(cursor.moveToNext()) {
                    PeopleInfo pinfo = new PeopleInfo();
                    pinfo.peopleId = -1;
                    pinfo.peopleName = cursor.getString(USIM_NAME_COLUMN_IDX);
                    pinfo.primaryNumber = cursor.getString(USIM_NUMBER_COLUMN_IDX);
                    pinfo.primaryEmail = cursor.getString(USIM_EMAIL_COLUMN_IDX);
                    pinfo.secondaryNumber = cursor.getString(USIM_SECOND_NUMBER_COLUMN_IDX);
                    Log.i(TAG, "usim card, [name]" + pinfo.peopleName + " "
                             + "[number]" + pinfo.primaryNumber + " "
                             + "[number2]" + pinfo.secondaryNumber + " "
                             + "[email]" + pinfo.primaryEmail);
                    peopleList.add(pinfo);
                }
            }
            catch(Exception ex){
                Log.e(TAG, "Receiving exception: " + ex.getMessage());
                return false;
            }
            finally {
                cursor.close();
            }
            return true;
        }

        Log.i(TAG, "Usim card query failed");
        return false;
    }

    public static boolean SimCard_QueryContactExist(ContentResolver resolver, PeopleInfo pInfo, int type) {
        if (type == TelephonyManager.PHONE_TYPE_NONE)
               return false;
        int simType = getSimType(type);
        
        Uri simUri = getUriByType(type);
        if (simUri == null) return false;

        if(simType == SIM_TYPE_SIM || simType == SIM_TYPE_USIM) {        
            Cursor cursor = resolver.query(
                    simUri,
                    SIMCARD_COLUMN_NAMES,
                    "name=? AND number=? AND number2=? AND emails=?",
                    new String[] {pInfo.peopleName, pInfo.primaryNumber, pInfo.secondaryNumber, pInfo.primaryEmail},
                    null);
            
            if (cursor != null && cursor.getCount() > 0) {     
            	// sim contact exists        
                cursor.close();
                return true;
            } else if (cursor != null ) {            	
                cursor.close();
                return false;
            } 
        }
        
        Log.v(TAG, "sim card query for existing record failed, simType = "+simType);
        return false;               
    }

    /**
     * save a contact to Sim card
     *
     * @param resolver the content resolver
     * @param pInfo use to store the new contact information
     * @return true if save succeeded
     */
    public static boolean SimCard_AddContact(ContentResolver resolver, PeopleInfo pInfo, int type)
    {
        if (type == TelephonyManager.PHONE_TYPE_NONE)
               return false;

        int simType = getSimType(type);

        Uri simUri = getUriByType(type);
        if (simUri == null) return false;

        buildSIMPeople(pInfo);

        ContentValues values = new ContentValues();

        if(simType == SIM_TYPE_SIM)
        {
            //XXX: to insert, we must use "tag", not "name"
            values.put("tag", pInfo.peopleName);
            values.put("number", pInfo.primaryNumber);
            if(resolver.insert(simUri, values) == null)
                return false;
        }
        else if(simType == SIM_TYPE_USIM)
        {
            //USIM: fixed the column names.
            values.put("tag", pInfo.peopleName);
            values.put("number", pInfo.primaryNumber);
            values.put("number2", pInfo.secondaryNumber);
            values.put("emails", pInfo.primaryEmail);
            Log.d(TAG,"cvmq63, let's check values before inserting.."+values.toString());
            if(resolver.insert(simUri, values) == null)
                return false;
        }

        return true;
    }

    /**
     * update a contact in Sim Card
     *
     * @param resolver the content resolver
     * @param pOldInfo use to store the old values
     * @param pNewInfo use to store the new values
     * @return true if update succeeded
     */
    public static boolean SimCard_UpdateContact(ContentResolver resolver, PeopleInfo pOldInfo, PeopleInfo pNewInfo, int type)
    {
        if (type == TelephonyManager.PHONE_TYPE_NONE)
               return false;

        int simType = getSimType(type);

        Uri simUri = getUriByType(type);
        if (simUri == null) return false;

        buildSIMPeople(pOldInfo);
        buildSIMPeople(pNewInfo);

        ContentValues values = new ContentValues();

        if(simType == SIM_TYPE_SIM)
        {
           /* values.put("tag", pOldInfo.peopleName);
            values.put("number", pOldInfo.primaryNumber);

            values.put("newTag", pNewInfo.peopleName);
            values.put("newNumber", pNewInfo.primaryNumber);
*/
            values.put("tag", pNewInfo.peopleName);
            values.put("number", pNewInfo.primaryNumber);
            if(resolver.update(simUri, values, "tag=" + "'" + pOldInfo.peopleName + "'" + " AND " +
                        "number=" + "'" + pOldInfo.primaryNumber+ "'", null) <= 0)
                return false;
        }
        else if(simType == SIM_TYPE_USIM)
        {
            //USIM: fixed the column names.
            values.put("tag", pNewInfo.peopleName);
            values.put("number", pNewInfo.primaryNumber);
            values.put("emails", pNewInfo.primaryEmail);
            values.put("number2", pNewInfo.secondaryNumber);
/*
            values.put("newTag", pNewInfo.peopleName);
            values.put("newNumber", pNewInfo.primaryNumber);
            values.put("newEmails", pNewInfo.primaryEmail);
            values.put("newNumber2", pNewInfo.secondaryNumber);
*/
            if (resolver.update(simUri, values, "tag=" + "'" + pOldInfo.peopleName + "'" + " AND " +
                        "number=" + "'" + pOldInfo.primaryNumber + "'" + " AND " +
                        "number2=" + "'" + pOldInfo.secondaryNumber+ "'" + " AND " +
                        "emails=" + "'" + pOldInfo.primaryEmail+ "'", null) <= 0)
                return false;
        }
        return true;

    }


    /**
     * delete a contact from Sim card
     *
     * @param resolver the content resolver
     * @param pInfo use to locate the contact item in sim card
     * @return true if delete succeeded
     */
    public static boolean SimCard_DeleteContact(ContentResolver resolver, PeopleInfo pInfo, int type)
    {
        if (type == TelephonyManager.PHONE_TYPE_NONE)
               return false;

        int simType = getSimType(type);

        Uri simUri = getUriByType(type);
        if (simUri == null) return false;

        buildSIMPeople(pInfo);
        if(simType == SIM_TYPE_SIM ){
            if(resolver.delete(simUri,
                        "tag=" + "'" + pInfo.peopleName + "'" + " AND " +
                        "number=" + "'" + pInfo.primaryNumber+ "'", null) <= 0)
                return false;
        } else if(simType == SIM_TYPE_USIM){
            //XXX: to delete, we must use "tag", not "name"
            if(resolver.delete(simUri,
                        "tag=" + "'" + pInfo.peopleName + "'" + " AND " +
                        "number=" + "'" + pInfo.primaryNumber + "'" + " AND " +
                        "number2=" + "'" + pInfo.secondaryNumber+ "'" + " AND " +
                        "emails=" + "'" + pInfo.primaryEmail+ "'", null) <= 0)
                return false;
        } else {
            return false;
        }
        return true;   
    }


    public static int bulkInsertSimContacts(ContentResolver resolver, ArrayList<PeopleInfo> peopleList, int type)
    {
        int nCount = 0;
        String accountName = getAccountNameByType(type);
        String accountType = HardCodedSources.ACCOUNT_TYPE_CARD;
        Account cardAccount = new Account(accountName, accountType);
        Iterator<PeopleInfo> it = peopleList.iterator();
        int nSize = peopleList.size();
        int nStep = nSize / 4;    // 4 times to finish load
        if (nStep < SIM_ADD_BATCH) {
            nStep = SIM_ADD_BATCH;
        } else if (nStep > MAX_BATCH_SIZE) {
            nStep = MAX_BATCH_SIZE;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

        while(it.hasNext())
        {
            PeopleInfo pInfo = (PeopleInfo)it.next();
            if (insertOneSimContacts(operationList, cardAccount, pInfo, type))
                nCount++;
            if (nCount % nStep == 0 && operationList != null && operationList.size() > 0) {
                Log.v(TAG, "nCount = " + nCount+", op_size = "+ operationList.size());
                try {
                    resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                } catch (RemoteException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                } catch (OperationApplicationException e) {
                    Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                }
                operationList.clear();
            }
        }

        if (operationList.size() > 0) {
            Log.v(TAG, "applyBtach, left = " + operationList.size());
            try {
                resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
            } catch (RemoteException e) {
                Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            } catch (OperationApplicationException e) {
                Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
            }
        }
        return nCount;
    }


    public static boolean insertOneSimContacts(ArrayList<ContentProviderOperation> operationList, Account account, PeopleInfo pInfo, int type)
    {
        if (!checkPeopleInfo(pInfo, type)) {
            Log.e(TAG, "error, not a valid contact, cancel insertOneSimContacts !");
            return false;
        }        
        return insertOneContacts(operationList, account, pInfo, true);
    }

    // sync means it is load sim contacts case, so insert the "has synced" info in the raw contacts
    public static boolean insertOneContacts(ArrayList<ContentProviderOperation> operationList, Account account, PeopleInfo pInfo, boolean sync)
    {
    	  boolean isSyncOperation = false;
        if (HardCodedSources.ACCOUNT_TYPE_LOCAL.equals(account.type) || sync) {
            // the local contacts change need not syncToNetwork, or sync = true indicate contacts has synced.
            isSyncOperation = true;
        }
        int numOperations = operationList.size();

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapter(RawContacts.CONTENT_URI, isSyncOperation));
        builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
        builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
        if (HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {
            builder.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_DISABLED); 
            /*2013-3-1, add by amt_sunzhao for SWITCHUITWO-604 */ 
        } else if (HardCodedSources.ACCOUNT_TYPE_LOCAL.equals(account.type)) {
        	builder.withValue(RawContacts.AGGREGATION_MODE, RawContacts.AGGREGATION_MODE_SUSPENDED);
        }
        /*2013-3-1, add end*/ 
        // sim backup data
        if (sync) {
            if(!TextUtils.isEmpty(pInfo.peopleName)) {
                builder.withValue(RawContacts.SYNC1, pInfo.peopleName);
            }
            if(!TextUtils.isEmpty(pInfo.primaryNumber)){
                builder.withValue(RawContacts.SYNC2, pInfo.primaryNumber);  
            }
            if(!TextUtils.isEmpty(pInfo.primaryEmail)) {
                builder.withValue(RawContacts.SYNC3, pInfo.primaryEmail);  
            }
            if(!TextUtils.isEmpty(pInfo.secondaryNumber)) {
                builder.withValue(RawContacts.SYNC4, pInfo.secondaryNumber); 
            }
        }
        builder.withYieldAllowed(true);
        operationList.add(builder.build());

        if(!TextUtils.isEmpty(pInfo.peopleName)) {
            builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapter(Data.CONTENT_URI, isSyncOperation));
            builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, numOperations);
            builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
            builder.withValue(StructuredName.FAMILY_NAME, pInfo.peopleName);            
            operationList.add(builder.build());
        }
        if(!TextUtils.isEmpty(pInfo.primaryNumber)){
            builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapter(Data.CONTENT_URI, isSyncOperation));
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, numOperations);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            builder.withValue(Phone.TYPE, Phone.TYPE_MOBILE);
            builder.withValue(Phone.NUMBER, pInfo.primaryNumber);
            builder.withValue(Data.IS_PRIMARY, 1);
            operationList.add(builder.build());
        }

        if(!TextUtils.isEmpty(pInfo.secondaryNumber)) {
            builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapter(Data.CONTENT_URI, isSyncOperation));
            builder.withValueBackReference(Phone.RAW_CONTACT_ID, numOperations);
            builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            builder.withValue(Phone.TYPE, Phone.TYPE_MOBILE);
            builder.withValue(Phone.NUMBER, pInfo.secondaryNumber);
            builder.withValue(Data.IS_PRIMARY, 0);
            operationList.add(builder.build());
        }

        if(!TextUtils.isEmpty(pInfo.primaryEmail)) {
            builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapter(Data.CONTENT_URI, isSyncOperation));
            builder.withValueBackReference(Email.RAW_CONTACT_ID, numOperations);
            builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            builder.withValue(Email.TYPE, Email.TYPE_MOBILE);
            builder.withValue(Email.DATA, pInfo.primaryEmail);
            builder.withValue(Data.IS_PRIMARY, 1);
            operationList.add(builder.build());
        }
        return true;
    }

            
    public static Uri addCallerIsSyncAdapter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            // If we're in the middle of a real sync-adapter operation, then go ahead
            // and tell the Contacts provider that we're the sync adapter.  That
            // gives us some special permissions - like the ability to really
            // delete a contact, and the ability to clear the dirty flag.
            //
            // If we're not in the middle of a sync operation (for example, we just
            // locally created/edited a new contact), then we don't want to use
            // the special permissions, and the system will automagically mark
            // the contact as 'dirty' for us!
            return uri.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();
        }
        return uri;
    }


    /**
     * delete all account members under SIM account from people database
     *
     * @param resolver the content resolver
     * @param type use to indicate which card need to be clean up
     */

    public static void deleteSIMAccountMembers(ContentResolver resolver, int type)
    {
        String accountName = SimUtility.getAccountNameByType(type);
        // Delete if we are deleting with correct account.
        Uri deleteWithCorrectAccountUri =
            RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                .appendQueryParameter(ContactsContract.RawContacts.ACCOUNT_TYPE, HardCodedSources.ACCOUNT_TYPE_CARD)
                .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
        try{
            resolver.delete(deleteWithCorrectAccountUri, null, null);
        } catch(Exception e){
            Log.e(TAG, "delete sim account member failed = " + e);
        }
    }

    /**
     * delete all account members under Local/C/G account which have been marked as "deleted" before
     * refer to ContactsContract.java : public static final String DELETED = "deleted";
     *
     * @param resolver the content resolver
     */
    public static void deleteAccountMembersMarked(ContentResolver resolver, String accountType)
    {
    	  Log.v(TAG, "begin deleteAccountMembersMarked, accountType = " + accountType);

        Cursor rawContactIdCursor = null;

        try {
            String selection = RawContacts.ACCOUNT_TYPE + "='" + accountType + "'" + 
                               " AND " + RawContacts.DELETED + "!='0'";
            rawContactIdCursor = resolver.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID},
                                 selection, null, null);
            if (rawContactIdCursor != null) {
                ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
                // delete 100 contacts in one batch, contacts provider don't support too much delete in one batch (500)
                int delete_batch = 100;
                int nCount = 0;
                while(rawContactIdCursor.moveToNext()) {
                    long rawContactId = rawContactIdCursor.getLong(0);
                    operationList.add(ContentProviderOperation
                                     .newDelete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId)
                                         .buildUpon()
                                         .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                         .build())
                                     .withYieldAllowed(true)
                                     .build());
                    nCount++;
                    if (nCount % delete_batch == 0) {
                        try {
                            resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                        } catch (RemoteException e) {
                            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                        } catch (OperationApplicationException e) {
                            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                        }
                        operationList.clear();
                    }
                }
                if (operationList.size() > 0) {
                    try {
                        resolver.applyBatch(ContactsContract.AUTHORITY, operationList);
                    } catch (RemoteException e) {
                        Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    } catch (OperationApplicationException e) {
                        Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
                    }
                }
            }
        } finally {
            if ( rawContactIdCursor != null ) {
                rawContactIdCursor.close();
            }
        }       
    	  Log.v(TAG, "end deleteAccountMembersMarked, accountType = " + accountType);
    }


    public static int getDefaultPhoto(ContentResolver resolver, long contactId) {
        int resId = R.drawable.ic_contact_list_picture;

        Cursor rawContactIdCursor = null;
        String accountType = null;
        String accountName = null;

        try {
            rawContactIdCursor = resolver.query(RawContacts.CONTENT_URI,
                    ACCOUNT_NAME_TYPE, RawContacts.CONTACT_ID + "=" + contactId, null, null);
            if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                // Just return the first one.
                accountName = rawContactIdCursor.getString(ACCOUNT_NAME_IDX);
                accountType = rawContactIdCursor.getString(ACCOUNT_TYPE_IDX);
                if (TextUtils.equals(accountType, HardCodedSources.ACCOUNT_TYPE_CARD)) {
                if (PhoneModeManager.isDmds()) {
                        if (TextUtils.equals(accountName, HardCodedSources.ACCOUNT_CARD_C))
                            resId = R.drawable.ic_contact_picture_c;
                        else if (TextUtils.equals(accountName, HardCodedSources.ACCOUNT_CARD_G))
                            resId = R.drawable.ic_contact_picture_g;
                    } else {
                        resId = R.drawable.ic_contact_picture_single;
                    }
                } else if (TextUtils.equals(accountType, HardCodedSources.ACCOUNT_TYPE_LOCAL)) {
                    resId = R.drawable.ic_contact_picture_local;
                } else if (TextUtils.equals(accountType, GoogleAccountType.ACCOUNT_TYPE)) {
                    resId = R.drawable.ic_contact_googgle;
                } else if (TextUtils.equals(accountType, ExchangeAccountType.ACCOUNT_TYPE)) {
                    resId = R.drawable.ic_contact_exchange;
                } else if (!TextUtils.isEmpty(accountType) && accountType.startsWith(HardCodedSources.ACCOUNT_TYPE_BLUR_ALL)) {
                    resId = R.drawable.ic_contact_picture_blur;
                }
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }

        return resId;
    }


    public static String getStructuredName(ContentResolver resolver, long rawContactId) {
        String peopleName = null;
        Cursor rawContactIdCursor = null;

        Uri uri =
              Uri.withAppendedPath(ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
              RawContacts.Data.CONTENT_DIRECTORY);

        final String[] projection = new String[] {
                StructuredName.DISPLAY_NAME};

        try {
            rawContactIdCursor = resolver.query(uri, projection, Data.MIMETYPE + "='"
                    + StructuredName.CONTENT_ITEM_TYPE + "'", null, null);

            if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                String displayName = rawContactIdCursor.getString(0);

                if (!TextUtils.isEmpty(displayName))
                    peopleName = displayName;
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }
        return peopleName;
    }


    /**
     * check if two PeopleInfo objects have the same VALUEs
     *
     * @return true if two objects are same
     */
    public static boolean isPeopleInfoEqual(PeopleInfo pinfo, PeopleInfo orginfo)
    {
        return ((TextUtils.isEmpty(orginfo.peopleName) && TextUtils.isEmpty(pinfo.peopleName)) ||
                (!TextUtils.isEmpty(orginfo.peopleName) && !TextUtils.isEmpty(pinfo.peopleName) &&
                 orginfo.peopleName.equals(pinfo.peopleName)))     // name equals
            && ((TextUtils.isEmpty(orginfo.primaryNumber) && TextUtils.isEmpty(pinfo.primaryNumber)) ||
                (!TextUtils.isEmpty(orginfo.primaryNumber) && !TextUtils.isEmpty(pinfo.primaryNumber) &&
                 orginfo.primaryNumber.equals(pinfo.primaryNumber)))     // primary number equals
            && ((TextUtils.isEmpty(orginfo.secondaryNumber) && TextUtils.isEmpty(pinfo.secondaryNumber)) ||
                (!TextUtils.isEmpty(orginfo.secondaryNumber) && !TextUtils.isEmpty(pinfo.secondaryNumber) &&
                 orginfo.secondaryNumber.equals(pinfo.secondaryNumber)))     // secondary number equals
            && ((TextUtils.isEmpty(orginfo.primaryEmail) && TextUtils.isEmpty(pinfo.primaryEmail)) ||
                (!TextUtils.isEmpty(orginfo.primaryEmail) && !TextUtils.isEmpty(pinfo.primaryEmail) &&
                 orginfo.primaryEmail.equals(pinfo.primaryEmail)));
    }


    /**
     * diff two PeopleInfo list and mark the
     * diffStatus member in every object.
     *
     * @param newList check if there are new items in
     *                this list that need to be added into Phone Sim Group.
     * @param oldList check if there are outdated items in this list
     *                that need to be deleted from Phone Sim Group
     */
    public static void diffPeopleInfoList(ArrayList<PeopleInfo> newList, ArrayList<PeopleInfo> oldList)
    {

        boolean found = false;
        for(int i = 0; i < newList.size(); i++)
        {
            PeopleInfo newInfo = newList.get(i);
            found = false;
            for(int j = 0; j < oldList.size(); j++)
            {
                PeopleInfo oldInfo = oldList.get(j);

                if((oldInfo.diffStatus == DIFF_INIT )
                        && isPeopleInfoEqual(oldInfo, newInfo))
                {
                    // find the item in old list
                    newInfo.diffStatus = DIFF_NORMAL;
                    oldInfo.diffStatus = DIFF_NORMAL;
                    found = true;
                    break;
                }
            }
            if(!found)
            {
                // the newInfo is a new item
                newInfo.diffStatus = DIFF_NEW;
            }
        }
    }


    /**
     * for debug only
     */
    public static void printPeopleInfo(ArrayList<PeopleInfo> list)
    {
        Iterator<PeopleInfo> it = list.iterator();
        while(it.hasNext())
        {
            PeopleInfo info = (PeopleInfo)it.next();
        }
    }


    public static boolean getSIMAccountMembers(ContentResolver resolver, int type, ArrayList<PeopleInfo> peopleList) {

        Cursor rawContactIdCursor = null;
        String accountName = getAccountNameByType(type);
        long rawContactId = -1;

        try {
            rawContactIdCursor = resolver.query(RawContacts.CONTENT_URI, new String[] {RawContacts._ID},
                RawContacts.ACCOUNT_TYPE + "='" + HardCodedSources.ACCOUNT_TYPE_CARD + "'" + " AND " +
                RawContacts.ACCOUNT_NAME + "='" + accountName + "'",
                null, null);
            if (rawContactIdCursor != null) {
                while(rawContactIdCursor.moveToNext()) {
                    rawContactId = rawContactIdCursor.getLong(0);
                    PeopleInfo info = new PeopleInfo();
                    if (getPeopleInfoById(resolver, rawContactId, info)) {
                        peopleList.add(info);
                    }
                }
            }
        } finally {
            if ( rawContactIdCursor != null ) {
                rawContactIdCursor.close();
            }
        }
        return true;
    }


    /**
     * delete a contact from people database permanently.
     *
     * @param resolver the content resolver
     * @param personId use to indicate which person need to be delete
     */
    public static void deleteContactInDB(ContentResolver resolver, long rawContactId)
    {
        Uri uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId)
                             .buildUpon()
                             .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                             .build();
                
        resolver.delete(uri, null, null);
    }


    /**
     * create the string to exclude the readonly account type 
     * multiple picker call it to filter out the readonly raw contacts when do multiple delete
     */
    public static String excludeReadonly(Context context)
    {        	
        Account[] accounts = AccountManager.get(context).getAccounts();
        AccountTypeManager sources = AccountTypeManager.getInstance(context);
        ArrayList<String> readonly = new ArrayList<String>();
    
        for (Account account : accounts) {
            // Ensure we have details loaded for each account
            //final ContactsSource source = sources.getInflatedSource(account.type,
            //        ContactsSource.LEVEL_SUMMARY);
            final AccountType source = sources.getAccountType(null, null);        
            if ((source != null) && !source.areContactsWritable()) {
                if (readonly.size() <= 0) {
                    readonly.add(account.type);
                }
                else if (!readonly.contains(account.type)) {
                    readonly.add(account.type);
                }                
            }
        }
                
        String excludeString = null;
        for(int i = 0; i < readonly.size(); i++) {
            String accountType = readonly.get(i);       	
        	  if (excludeString == null) {
                excludeString = RawContacts.ACCOUNT_TYPE + "!='" + accountType + "'";
            }
            else {
                excludeString += " AND " + RawContacts.ACCOUNT_TYPE + "!='" + accountType + "'";
            }
        }
        Log.v(TAG, "the exclude string = " + excludeString); 
        return excludeString;
    }


    public static boolean isReadOnlyRawContact(Context context, long rawContactId) {
        Cursor rawContactIdCursor = null;
        String accountType = null;
        AccountTypeManager sources = AccountTypeManager.getInstance(context);
        ContentResolver resolver = context.getContentResolver();

        try {
            rawContactIdCursor = resolver.query(RawContacts.CONTENT_URI,
                    ACCOUNT_NAME_TYPE, RawContacts._ID + "=" + rawContactId, null, null);
            if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                accountType = rawContactIdCursor.getString(ACCOUNT_TYPE_IDX);
                //final ContactsSource source = sources.getInflatedSource(accountType,
                //    ContactsSource.LEVEL_SUMMARY);
                final AccountType source = sources.getAccountType(null, null);            
                if (source != null) {
                   return !source.areContactsWritable();
                }
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }
        return false;
    }
   


    /**
     * request the manual sync for sim contacts
     */ 
    public static void requestSimSync(int type)
    {
        if (PhoneModeManager.isDmds()) {
            if (type == TelephonyManager.PHONE_TYPE_CDMA) {
                requestSync(new Account(HardCodedSources.ACCOUNT_CARD_C, HardCodedSources.ACCOUNT_TYPE_CARD));
            } else if (type == TelephonyManager.PHONE_TYPE_GSM) {
                requestSync(new Account(HardCodedSources.ACCOUNT_CARD_G, HardCodedSources.ACCOUNT_TYPE_CARD));
            }
        } else {
            requestSync(new Account(HardCodedSources.ACCOUNT_CARD, HardCodedSources.ACCOUNT_TYPE_CARD));
        }
    }
     
    /**
     * request the manual sync after there is DB change, it is useful for sim sync case
     */ 
    public static void requestSync(Account account)
    {    	            	
        Log.v(TAG, "requestSync(), account name  = " + account.name + ", account type = " + account.type);
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        // add moto defined extra to bypass data connection checking while syncing in SyncManager
        extras.putBoolean(SYNC_EXTRAS_MOTOROLA_NO_DATA_CONNECTION_CHECK, true);
        ContentResolver.requestSync(account, ContactsContract.AUTHORITY, extras);
    }


    /**
     * get the person info by the given contact ID
     * this function can be called when get SIM/USIM/Device info to copy between card and device    
     */ 
    public static int getSelectPeopleInfo(ContentResolver resolver, long contactId, ArrayList<PeopleInfo> peopleList, int copyType, int[] freeEmailSpace)
    {   
    	  int nCount = 0;

        if (copyType == ContactsCopySIM.C_CARD_TO_DEVICE || copyType == ContactsCopySIM.G_CARD_TO_DEVICE) {
            nCount = getPeopleInfoByContactId(resolver, contactId, peopleList);
        }
        else if (copyType == ContactsCopySIM.DEVICE_TO_C_CARD) {
            nCount = getMulPeopleInfoByContactId(resolver, contactId, peopleList);
        }
        else if (copyType == ContactsCopySIM.DEVICE_TO_G_CARD) {
            boolean isUSIM = isUSIMType(TelephonyManager.PHONE_TYPE_GSM);
			      if (isUSIM) {
				        nCount = getPeopleInfo4ExportById(resolver, contactId, peopleList, freeEmailSpace);
			      } else {
				        nCount = getMulPeopleInfoByContactId(resolver, contactId, peopleList);
			      }
			  }
	      return nCount;
    }


    /**
     * verify if the PeopleInfo is a valid sim contacts     
     */
     
    public static boolean checkPeopleInfo(PeopleInfo pInfo, int type)
    {
        if (type == TelephonyManager.PHONE_TYPE_NONE) {
    	      return true;
    	  }
        else if(isUSIMType(type)) {
            if(TextUtils.isEmpty(pInfo.primaryNumber) && TextUtils.isEmpty(pInfo.secondaryNumber) &&
        		(TextUtils.isEmpty(pInfo.peopleName))) {
                Log.e(TAG, "checkPeopleInfo, failed, USIM");
                return false;
            } else {
                return true;
            }
        } else {
            if(TextUtils.isEmpty(pInfo.primaryNumber) && TextUtils.isEmpty(pInfo.secondaryNumber)) {
                Log.e(TAG, "checkPeopleInfo, failed, SIM");
                return false;
            }
            else {
                return true;
            }
        }
    }

    /**
     * get the person info by the given person ID
     * this function can be called when get SIM/USIM info to copy to device
     * need not call buildSimName to truncate name, need not check the number < MAX_SIM_PHONE_NUMBER_LENGTH
     * but need check if it a valid sim contacts by checkPeopleInfo
     */
    public static boolean getPeopleInfoById(ContentResolver resolver, long rawContactId, PeopleInfo pInfo)
    {                
        String firstNumber = null;
        String secondaryNumber = null;
        Cursor rawContactIdCursor = null;
        String firstEmail = null;        
 
        Log.v(TAG, "getPeopleInfoById, query number, rawContactId = " + rawContactId);
        // fetch phone info of the specified raw_contact
    	//begin mtdr83 for IKCBSMMCPPRC-1278 IKCBSMMCPPRC-1626
		Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
				rawContactId);
		Uri entityUri = Uri.withAppendedPath(rawContactUri,
				Entity.CONTENT_DIRECTORY);
		if(DEBUG)Log.d(TAG,"getPeopleInfoById :rawContactUri=" + rawContactUri 
				+ ":entityUri=" + entityUri);
		
		Cursor entityCursor = resolver.query(entityUri, new String[] {
				Entity.DATA_ID, Entity.MIMETYPE, Entity.DATA1 },
				Entity.MIMETYPE + "=" + '"'
						+ Phone.CONTENT_ITEM_TYPE + '"', 
				null, null);
		if (null != entityCursor && entityCursor.moveToFirst()) {
			int mimeIndex = entityCursor.getColumnIndex(Entity.MIMETYPE);
			int dataIndex = entityCursor.getColumnIndex(Entity.DATA1);	
			String mimeType = null;
			String data1 = null;
			String tempnumber = null;

			do {
				if (!entityCursor.isNull(0)) {
					mimeType = entityCursor.getString(mimeIndex);
					data1 = entityCursor.getString(dataIndex);

					if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
						tempnumber = buildSimNumber(data1);
						Log.d(TAG, "tempnumber = " + tempnumber);
						if (tempnumber != null && tempnumber.length() <= MAX_SIM_PHONE_NUMBER_LENGTH) {
							if (firstNumber == null) {
                            firstNumber = tempnumber;
	                        } else if (secondaryNumber == null) {
	                            secondaryNumber = tempnumber;
	                            break;   // sim contacts only support at most 2 numbers, need not continue query.
	                        } else {
	                        	break;
	                        }
						}
					}
				}
			}while(entityCursor.moveToNext());
		}else{
			if(DEBUG)Log.d(TAG,"getPeopleInfoById :error null != entityCursor && entityCursor.moveToFirst()" );
		}
		
		if (null != entityCursor) {
			entityCursor.close();
		}
		//End mtdr83 for IKCBSMMCPPRC-1278 IKCBSMMCPPRC-1626

        // fetch email info of the specified raw_contact
        try {
            rawContactIdCursor = resolver.query(Email.CONTENT_URI, new String[] {Email.DATA},
                    Data.RAW_CONTACT_ID + "=" + rawContactId, null, null);
            if (rawContactIdCursor != null) {
                while(rawContactIdCursor.moveToNext()) {
                    String email = rawContactIdCursor.getString(0);
                    if (!TextUtils.isEmpty(email) && firstEmail == null) {
                        firstEmail = email;
                    }
                }
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }        
        
        Log.v(TAG, "getPeopleInfoById, query name, rawContactId = " + rawContactId);
        pInfo.peopleName = getStructuredName(resolver, rawContactId);
        pInfo.primaryNumber = firstNumber;
        pInfo.secondaryNumber = secondaryNumber;
        pInfo.primaryEmail = firstEmail;
        pInfo.diffStatus = DIFF_INIT;
        pInfo.peopleId = rawContactId;

        Log.v(TAG, "getPeopleInfoById, get Contact type, rawContactId = " + rawContactId);
    	  int type = getContactTypeById(resolver, rawContactId);

        Log.v(TAG, "getPeopleInfoById, checkPeopleInfo, rawContactId = " + rawContactId);
        return checkPeopleInfo(pInfo, type);
    }

    // actually the sim contacts disable the "join", it always has only 1 raw contact under a certain contact 
    public static int getPeopleInfoByContactId(ContentResolver resolver, long contactId, ArrayList<PeopleInfo> peopleList) {
        long rawContactId = ContactsUtils.queryForNameRawContactId(resolver, contactId);
        int nCount = 0;
        
        PeopleInfo info = new PeopleInfo();
        if (getPeopleInfoById(resolver, rawContactId, info)) {
            peopleList.add(info);
            nCount++;            
        }
        return nCount;
    }

    /**
     * get the person info by the given person ID
     * this function can be called when get device info to copy to SIM, one device contact need split to multiple entries on SIM
     */
    public static int getMulPeopleInfoById(ContentResolver resolver, long rawContactId, ArrayList<PeopleInfo> peopleList)
    {
        String peopleName = getStructuredName(resolver, rawContactId);
        peopleName = buildSimName(peopleName);
        String peopleNumber = null;
        Cursor rawContactIdCursor = null;
        int nCount = 0;

        final String[] projection = new String[] {
                Phone.NUMBER
        };

        try {
            rawContactIdCursor = resolver.query(Phone.CONTENT_URI, projection,
                    Data.RAW_CONTACT_ID + "=" + rawContactId, null, null);
            if (rawContactIdCursor != null) {
                while(rawContactIdCursor.moveToNext()) {
                    peopleNumber = buildSimNumber(rawContactIdCursor.getString(0));
                    if (!TextUtils.isEmpty(peopleNumber) && (peopleNumber.length() <= MAX_SIM_PHONE_NUMBER_LENGTH)) {
                        PeopleInfo pinfo = new PeopleInfo();
                        pinfo.peopleId = rawContactId;
                        pinfo.peopleName = peopleName;
                        pinfo.primaryNumber = peopleNumber;
                        pinfo.diffStatus = DIFF_INIT;
                        peopleList.add(pinfo);
                        nCount++;
                    }
                }
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }

        return nCount;
    }

    public static int getMulPeopleInfoByContactId(ContentResolver resolver, long contactId, ArrayList<PeopleInfo> peopleList) {
        final ArrayList<Long> rawContactIdsList =  ContactsUtils.queryForAllRawContactIds(resolver, contactId, true);
        int nCount = 0;
    	
        Iterator<Long> it = rawContactIdsList.iterator();
        while(it.hasNext())
        {
            Long rawContactId = (Long) it.next();
            nCount += getMulPeopleInfoById(resolver, rawContactId, peopleList);
        }
        return nCount;
    }


    public static int getContactTypeById(ContentResolver resolver, long rawContactId) {

        Cursor rawContactIdCursor = null;
        String accountName = null;
        String accountType = null;
        int type = TelephonyManager.PHONE_TYPE_NONE;
        PeopleInfo info = new PeopleInfo();

        try {
            rawContactIdCursor = resolver.query(RawContacts.CONTENT_URI,
                    ACCOUNT_NAME_TYPE, RawContacts._ID + "=" + rawContactId, null, null);
            if (rawContactIdCursor != null && rawContactIdCursor.moveToFirst()) {
                accountName = rawContactIdCursor.getString(ACCOUNT_NAME_IDX);
                accountType = rawContactIdCursor.getString(ACCOUNT_TYPE_IDX);
                if (TextUtils.equals(accountType, HardCodedSources.ACCOUNT_TYPE_CARD)) {
                    type = getTypeByAccountName(accountName);
                }
            }
        } finally {
            if (rawContactIdCursor != null) {
                rawContactIdCursor.close();
            }
        }
        return type;
    }

    public static boolean isSIMContactById(ContentResolver resolver, long rawContactId) {
        int type = getContactTypeById(resolver, rawContactId);
        if (type == TelephonyManager.PHONE_TYPE_CDMA || type == TelephonyManager.PHONE_TYPE_GSM)
            return true;
        else
            return false;
    }

    public static boolean isSimSyncing (Context context) {
    	boolean isSyncing = false;
    	
        Account[] accounts = AccountManager.get(context).getAccounts();
        if (accounts != null && accounts.length > 0) {
            for (Account account : accounts) {
                //Log.v(TAG, "account: "+account);
                if(HardCodedSources.ACCOUNT_TYPE_CARD.equals(account.type)) {      
                    isSyncing = (ContentResolver.isSyncActive(account, ContactsContract.AUTHORITY)
                                || ContentResolver.isSyncPending(account, ContactsContract.AUTHORITY));
                    Log.v(TAG, "Card type, name = "+account.name+", isSyncing = "+isSyncing);       
                }
            }
        }              
        return isSyncing;
    }

    public static boolean hasSIMAvailable(Context context) {
        return (isSimReady(TelephonyManager.PHONE_TYPE_CDMA)
               || isSimReady(TelephonyManager.PHONE_TYPE_GSM));
    }
    //begin add by txbv34 for IKCBSMMCPPRC-1364  
    public static void setSettingVisible(ContentResolver resolver, String accountName, String accountType,boolean isVisible) {

        // check if the setting exist
      boolean hasSetting = hasAccountSetting(resolver, accountName, accountType);
      ContentValues values = new ContentValues();
      if(DEBUG)Log.d(TAG,"setSettingVisible,hasSetting=" + hasSetting);
      if(DEBUG)Log.d(TAG,"setSettingVisible,accountName=" + accountName);
      if(DEBUG)Log.d(TAG,"setSettingVisible,accountType=" + accountType);
      if(DEBUG)Log.d(TAG,"setSettingVisible,isVisible=" + isVisible);
      if (hasSetting) {
          values.put(Settings.UNGROUPED_VISIBLE, isVisible? 1 : 0);
          resolver.update(Settings.CONTENT_URI, values, 
        		  Settings.ACCOUNT_NAME + "=? AND "
                      + Settings.ACCOUNT_TYPE + "=?", 
                      new String[] {accountName, accountType});
      }
      else {
          values.put(Settings.ACCOUNT_NAME, accountName);
          values.put(Settings.ACCOUNT_TYPE, accountType);
          values.put(Settings.UNGROUPED_VISIBLE, isVisible? 1 : 0);
          resolver.insert(Settings.CONTENT_URI, values);
      }
  }
    
    public static void ResetSettingVisible(ContentResolver resolver) {

        Cursor settingCursor = null;
        if(DEBUG)Log.d(TAG,"ResetSettingVisible");
        try {
        	settingCursor = resolver.query(Settings.CONTENT_URI,
                    new String[] {Settings.ACCOUNT_NAME,Settings.ACCOUNT_TYPE,Settings.UNGROUPED_VISIBLE}, 
                    null, null, null);
            if (settingCursor != null && settingCursor.moveToFirst()) {           	
            	do{
                	String accountName = settingCursor.getString(settingCursor.getColumnIndex(Settings.ACCOUNT_NAME));
                	String accountType = settingCursor.getString(settingCursor.getColumnIndex(Settings.ACCOUNT_TYPE));
                	setSettingVisible(resolver,accountName,accountType,true);		
            	}
            	while(settingCursor.moveToNext()) ;
            }
        } finally {
            if (settingCursor != null) {
            	settingCursor.close();
            }
        }
    }
    
    public static void ResetGroupsDBDefault(ContentResolver resolver) {

        Cursor groupCursor = null;
        if(DEBUG)Log.d(TAG,"ResetGroupsDBDefault");
        try {
        	groupCursor = resolver.query(Groups.CONTENT_URI,
                    new String[] {Groups.GROUP_VISIBLE,Groups.ACCOUNT_NAME,
            		Groups.ACCOUNT_TYPE,Groups.TITLE}, 
                    null, null, null);
            if (groupCursor != null && groupCursor.moveToFirst()) {           	
            	do{
                	String accountName = groupCursor.getString(groupCursor.getColumnIndex(Groups.ACCOUNT_NAME));
                	String accountType = groupCursor.getString(groupCursor.getColumnIndex(Groups.ACCOUNT_TYPE));
                	String accountTitle = groupCursor.getString(groupCursor.getColumnIndex(Groups.TITLE));
                	setGroupVisible(resolver,accountName,accountType,accountTitle,true);		
            	}
            	while(groupCursor.moveToNext()) ;
            }
        } finally {
            if (groupCursor != null) {
            	groupCursor.close();
            }
        }
    }
    public static boolean hasGroupSetting(ContentResolver resolver, String accountName, String accountType) {

        Cursor groupCursor = null;
        boolean hasSetting = false;
        if(DEBUG)Log.d(TAG,"hasGroupSetting,=");
        try {
            groupCursor = resolver.query(Groups.CONTENT_URI,
                    new String[] {Groups.GROUP_VISIBLE}, Groups.ACCOUNT_NAME + "=? AND "
                    + Groups.ACCOUNT_TYPE + "=?", new String[] {accountName, accountType}, null);
            if (groupCursor != null && groupCursor.moveToFirst()) {
                hasSetting = true;
                if(DEBUG)Log.d(TAG,"hasGroupSetting,hasSetting == true");
            }
        } finally {
            if (groupCursor != null) {
            	groupCursor.close();
            }
        }
        return hasSetting;
    }
    public static void setGroupVisible(ContentResolver resolver, String accountName, String accountType,String title, boolean isVisible) {

        // check if the setting exist
      boolean hasSetting = hasGroupSetting(resolver, accountName, accountType);
      ContentValues values = new ContentValues();
      if(DEBUG)Log.d(TAG,"setGroupVisible,hasSetting=" + hasSetting);
      if(DEBUG)Log.d(TAG,"setGroupVisible,accountName=" + accountName);
      if(DEBUG)Log.d(TAG,"setGroupVisible,accountType=" + accountType);
      if(DEBUG)Log.d(TAG,"setGroupVisible,title=" + title);
      if(DEBUG)Log.d(TAG,"setGroupVisible,isVisible=" + isVisible);
      if (hasSetting) {
          values.put(Groups.GROUP_VISIBLE, isVisible? 1 : 0);
          resolver.update(Groups.CONTENT_URI, values, 
        		  Groups.ACCOUNT_NAME + "=? AND "
        		   + Groups.TITLE + "=? AND " 
                      + Groups.ACCOUNT_TYPE + "=?", 
                      new String[] {accountName, title,accountType});
      }
      else {
          values.put(Groups.ACCOUNT_NAME, accountName);
          values.put(Groups.ACCOUNT_TYPE, accountType);
          values.put(Groups.TITLE, title);
          values.put(Groups.GROUP_VISIBLE, isVisible? 1 : 0);
          resolver.insert(Groups.CONTENT_URI, values);
      }
  }
    //end add by txbv34 for IKCBSMMCPPRC-1364
    public static void setAccountVisible(ContentResolver resolver, String accountName, String accountType, boolean isVisible) {

          // check if the setting exist
        boolean hasSetting = hasAccountSetting(resolver, accountName, accountType);
        ContentValues values = new ContentValues();

        if (hasSetting) {
            values.put(Settings.UNGROUPED_VISIBLE, isVisible? 1 : 0);
            resolver.update(Settings.CONTENT_URI, values, Settings.ACCOUNT_NAME + "=? AND "
                        + Settings.ACCOUNT_TYPE + "=?", new String[] {
                        accountName, accountType});
        }
        else {
            values.put(Settings.ACCOUNT_NAME, accountName);
            values.put(Settings.ACCOUNT_TYPE, accountType);
            values.put(Settings.UNGROUPED_VISIBLE, isVisible? 1 : 0);
            resolver.insert(Settings.CONTENT_URI, values);
        }
    }


    public static boolean getAccountVisible(ContentResolver resolver, String accountName, String accountType) {

        Cursor settingCursor = null;
        boolean isVisible = false;

        try {
            settingCursor = resolver.query(Settings.CONTENT_URI,
                    new String[] {Settings.UNGROUPED_VISIBLE}, Settings.ACCOUNT_NAME + "=? AND "
                    + Settings.ACCOUNT_TYPE + "=?", new String[] {accountName, accountType}, null);
            if (settingCursor != null && settingCursor.moveToFirst()) {
                // Just return the first one.
                isVisible = (settingCursor.getInt(0) != 0) ? true : false;
            }
        } finally {
            if (settingCursor != null) {
                settingCursor.close();
            }
        }
        return isVisible;
    }

    public static boolean hasAccountSetting(ContentResolver resolver, String accountName, String accountType) {

        Cursor settingCursor = null;
        boolean hasSetting = false;

        try {
            settingCursor = resolver.query(Settings.CONTENT_URI,
                    new String[] {Settings.UNGROUPED_VISIBLE}, Settings.ACCOUNT_NAME + "=? AND "
                    + Settings.ACCOUNT_TYPE + "=?", new String[] {accountName, accountType}, null);
            if (settingCursor != null && settingCursor.moveToFirst()) {
                hasSetting = true;
            }
        } finally {
            if (settingCursor != null) {
                settingCursor.close();
            }
        }
        return hasSetting;
    }

    public static void setAccountDefaultVisible(ContentResolver resolver, String accountName, String accountType, boolean isVisible) {

          // check if the setting exist
        boolean hasSetting = hasAccountSetting(resolver, accountName, accountType);

        if (!hasSetting) {   // not exist, creat new setting
            ContentValues values = new ContentValues();
            values.put(Settings.ACCOUNT_NAME, accountName);
            values.put(Settings.ACCOUNT_TYPE, accountType);
            values.put(Settings.UNGROUPED_VISIBLE, isVisible? 1 : 0);
            resolver.insert(Settings.CONTENT_URI, values);
        }
    }


    public static void setAccountsDefaultVisible(ContentResolver resolver)	{
        // set the default setting as "visible"
        setAccountDefaultVisible(resolver, HardCodedSources.ACCOUNT_LOCAL_DEVICE,
                                     HardCodedSources.ACCOUNT_TYPE_LOCAL, true);
        if (PhoneModeManager.isDmds()) {
            setAccountDefaultVisible(resolver, HardCodedSources.ACCOUNT_CARD_C,
                                     HardCodedSources.ACCOUNT_TYPE_CARD, true);
            setAccountDefaultVisible(resolver, HardCodedSources.ACCOUNT_CARD_G,
                                     HardCodedSources.ACCOUNT_TYPE_CARD, true);
        } else {
            setAccountDefaultVisible(resolver, HardCodedSources.ACCOUNT_CARD,
                                     HardCodedSources.ACCOUNT_TYPE_CARD, true);
        }
    }


    /////////////////////////////////////////////////////////////////
    ///////////////// SIM CARD Related APIs ///////////////////////////
    /////////////////////////////////////////////////////////////////


    public static boolean isUSIMType(int type)
    {
        return (getSimType(type) == SIM_TYPE_USIM);
    }


    public static Uri getUriByType(int type)
    {
        if (type == TelephonyManager.PHONE_TYPE_NONE)
            return null;

        if (!PhoneModeManager.isDmds()) {
            return SIM_CONTENT_URI;
        } else {
            int defaultPhoneType = TelephonyManager.getDefault().getPhoneType();
            if (type == defaultPhoneType)
                return SIM_CONTENT_URI;
            else
                return SIM_CONTENT_URI2;
        }
    }

    public static int getFreeSpaceHardCode(ContentResolver resolver, int type){
        String selection = RawContacts.ACCOUNT_NAME + "='"
                    + HardCodedSources.ACCOUNT_CARD + "' AND "
                    + RawContacts.DELETED + "=0";

        Cursor cursor = resolver.query(
                RawContacts.CONTENT_URI, null, selection, null, null);
        int simContacts = 0;
        if(cursor != null){
            simContacts = cursor.getCount();
            cursor.close();
        }
        Log.d(TAG, "getFreeSpaceHardCode simcontacts=" + simContacts);

        return getCapacity(resolver, type) - simContacts;
    }

    public static int getFreeSpaceCom(ContentResolver resolver, int type)
    {
        Uri simUri = null;
        int freeSpace = 0;

        int simType = getSimType(type);
        if(simType == SIM_TYPE_UNKNOWN)
            return -1;

        if (!PhoneModeManager.isDmds()) {
            simUri = SIM_FREESPACE_URI;
        } else {
            int defaultPhoneType = TelephonyManager.getDefault().getPhoneType();
            if (type == defaultPhoneType)
                simUri = SIM_FREESPACE_URI;
            else
                simUri = SIM_FREESPACE_URI2;
        }


        Cursor cursor;
        try{
            cursor = resolver.query(
                simUri,
                SIMCARD_COLUMN_NAMES,
                null,
                null,
                null);
        } catch(IllegalArgumentException e) {
            /* to be implemented, Xinyu Liu/dcjf34 */
            Log.e(TAG, "query error e = " + e);
            return getFreeSpaceHardCode(resolver, type);
        }
        
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    freeSpace = Integer.parseInt(cursor.getString(SIMCARD_NUMBER_COLUMN_IDX));
                }
            }
            catch(Exception ex){
                Log.i(TAG, "getFreeSpace, throw exception = " + ex.toString());
                return freeSpace;
            }
            finally {
                cursor.close();
            }
            Log.i(TAG, "type = " + type + "getFreeSpace = " + freeSpace);
            return freeSpace;
        }

        Log.i(TAG, "failed, type = " + type + "getFreeSpace = " + freeSpace);
        return freeSpace;
    }

    public static int getFreeSpace(ContentResolver resolver, int type)
    {
        if (type == TelephonyManager.PHONE_TYPE_NONE)
            return 0;

        if (!SimUtility.getSIMLoadStatus()) {  // Do not read free space when card is loading
            return 0;
        }

        return getFreeSpaceCom(resolver, type);
    }

    public static int getFreeSpace(ContentResolver resolver, int type, boolean simLoadingManual) {    	
    	if (simLoadingManual) {    	
    		// don't call getSIMLoadStatus() due to syncing in background
            if (type == TelephonyManager.PHONE_TYPE_NONE)
                return 0;
    		
    		return getFreeSpaceCom(resolver, type);     			
    	} else {
    	    return getFreeSpace(resolver, type);    
    	}
    }

    public static int getCapacity(ContentResolver resolver, int type)
    {
        if (type == TelephonyManager.PHONE_TYPE_NONE)
            return 0;
            
        if (!SimUtility.getSIMLoadStatus()) {  // Do not read capacity when card is loading
            return 0;
        }

        Uri simUri = null;
        int capacity = 0;

        int simType = getSimType(type);
        if(simType == SIM_TYPE_UNKNOWN)
            return -1;

        if (!PhoneModeManager.isDmds()) {
            simUri = SIM_CAPACITY_URI;
        } else {
            int defaultPhoneType = TelephonyManager.getDefault().getPhoneType();
            if (type == defaultPhoneType)
                simUri = SIM_CAPACITY_URI;
            else
                 simUri = SIM_CAPACITY_URI2;
        }

 
        Cursor cursor;
        try{
            cursor = resolver.query(
                simUri,
                SIMCARD_COLUMN_NAMES,
                null,
                null,
                null);
        } catch(IllegalArgumentException e) {
            /* to be implemented, Xinyu Liu/dcjf34 */
            Log.e(TAG, "query error e = " + e);
            return 250;
        }


            if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    capacity = Integer.parseInt(cursor.getString(SIMCARD_NUMBER_COLUMN_IDX));
                }
            }
            catch(Exception ex){
                Log.i(TAG, "getCapacity, throw exception = " + ex.toString());
                return capacity;
            }
            finally {
                cursor.close();
            }
            Log.i(TAG, "type = " + type + "getCapacity = " + capacity);
            return capacity;
        }

        Log.i(TAG, "failed, type = " + type + "getCapacity = " + capacity);
        return capacity;
    }

    public static int getIccType(ContentResolver resolver, int type) {
        if (!PhoneModeManager.isDmds()) {
    	      return getSingleModeSimType(resolver, type);
    	  }
    	  
        int simType = SIM_TYPE_UNKNOWN;        
        IccCardApplication.AppType iccType = IccCardApplication.AppType.APPTYPE_UNKNOWN;
        /* to-pass-build, Xinyu Liu/dcjf34 
        IccCardInfo iccInfo = getIccInfo(type);
        if (iccInfo == null) {
             return simType;
        }
        else*/ if (isSimBlocked(type)) {
            return simType;
        }
        else { // check imsi for each application
        /* to-pass-build, Xinyu Liu/dcjf34 
            int all_apps = iccInfo.getNumApplications();
            for (int i = 0; i < all_apps; i++) { // iterate all apps
                IccCardApplication app = iccInfo.getApplication(i);
                //FIXME: stub code
                String imsi = ((IccCardAppInfo)app).imsi;
                if (!invalidImsi(imsi)) {
                    iccType = app.app_type;
                    break;
                }
            }
        */
        }
        if (iccType == IccCardApplication.AppType.APPTYPE_SIM)
            simType = SIM_TYPE_SIM;
        else if (iccType == IccCardApplication.AppType.APPTYPE_USIM)
            simType = SIM_TYPE_USIM;
        else if (iccType == IccCardApplication.AppType.APPTYPE_RUIM)
            simType = SIM_TYPE_SIM;
        else
            simType = SIM_TYPE_UNKNOWN;

        return simType;
    }


    public static String getIccId(int type) {

        if (!PhoneModeManager.isDmds()) {
            TelephonyManager tm = TelephonyManager.getDefault();
		        if (tm != null && tm.getPhoneType() == type) {
		            return tm.getSimSerialNumber();
		        }
    	  }
           /* to-pass-build, Xinyu Liu/dcjf34 
    	  else {
            IccCardInfo iccInfo = getIccInfo(type);
            if (iccInfo != null) {
                return iccInfo.iccid;
            }
        }*/
    	  return null;
    }

/* to-pass-build, Xinyu Liu/dcjf34
    public static IccCardInfo getIccInfo(int slotPhoneType)
    {
        IccCardInfo iccInfo = null;
        if (!PhoneModeManager.isDmds()) {
            return null;
        }
                	
        if (slotPhoneType == TelephonyManager.PHONE_TYPE_NONE)
            return null;

        int[] ids = PhoneModeManager.getAllModemIds();
        if (ids != null) {
            SubscriptionInfo info = null;
            for(int i = 0; i < ids.length; i++) {
                if(PhoneModeManager.getPhoneTypeByModemId(ids[i]) == slotPhoneType) {
                    info = PhoneModeManager.getSubscriptionInfo(ids[i]);
                    break;
                }
            }

            if(info != null && info.getSubscriptionType() ==  RILConstants.SUBSCRIPTION_FROM_RUIM/* to-pass-build, Xinyu Liu/dcjf34 SubscriptionInfo.SUBSCRIPTION_TYPE_ICC) {
                iccInfo = info.iccCardInfo;
            }
        }
            
        return iccInfo;
    }
*/

    private static boolean invalidImsi(String imsi) {
        if (imsi == null) return true;
        String mccmnc = imsi.substring(0, 5);
        if (mccmnc.equals("46099")) { // invalid mnc code
            return true;
        }

        return false;
    }

    public static void setSIMLoadStatus(boolean flag) {
        sLoaded = flag;
    }

    public static boolean getSIMLoadStatus() {
        return sLoaded;
    }

   
    public static int getSingleModeSimType(ContentResolver resolver, int type) {
        int simType = SIM_TYPE_UNKNOWN;
        String isYes = "YES";
        
        TelephonyManager tm = TelephonyManager.getDefault();
        if (tm != null && tm.getPhoneType() == type) {
            Log.d(TAG, "TelephonyManager.hasIccCard = " + tm.hasIccCard() + "TelephonyManager.getSimState()"+ tm.getSimState()); 
            if (tm.hasIccCard() && (tm.getSimState() == TelephonyManager.SIM_STATE_READY)) {
                // query the sim capability, if it support email and phone number 2, then it is USIM card
                Cursor simCursor = null;
                try {
                    simCursor = resolver.query(SIM_CAPABILITY_URI, USIM_COLUMN_NAMES, null, null, null);
                    if (simCursor != null && simCursor.moveToFirst()) {
                        // check if it support email and second phone number, if YES, then support
                        String email = simCursor.getString(USIM_EMAIL_COLUMN_IDX);
                        String number2 = simCursor.getString(USIM_SECOND_NUMBER_COLUMN_IDX);                        
                        if (isYes.equals(email) && isYes.equals(number2))
                            simType = SIM_TYPE_USIM;
                        else
                            simType = SIM_TYPE_SIM;  
                    }
                } catch(IllegalArgumentException e) {
                    /* to-be implemented, Xinyu Liu/dcjf34 */ 
                    Log.e(TAG, "query error e = " + e); 
                    simType = SIM_TYPE_SIM;
                } finally {
                    if (simCursor != null) {
                        simCursor.close();
                    }
                }
            }
        }
        Log.d(TAG, "getSingleModeSimType, simType = " + simType);
        return simType;
    }

    
    public static boolean isSimReady(int type) {
        return (getSimType(type) != SimUtility.SIM_TYPE_UNKNOWN);
    }

    // for DMDS framework, we need actively query the card state
    public static boolean isSimBlocked(int type) {
  /*
        IccCardInfo iccInfo = getIccInfo(type);
        if(iccInfo != null) {  // detect the card info
        	  Log.v(TAG, "isSimBlocked, type = " + type);
            // check card state
            IccCardStatus.CardState card_state = iccInfo.getCardState();
            Log.v(TAG, "card state = " + card_state);
            if ((card_state == IccCardStatus.CardState.CARDSTATE_ABSENT) 
                    || (card_state == IccCardStatus.CardState.CARDSTATE_ERROR)) {
                return true;
            }
            // telephony don't set universal PIN for dual-mode ICC or RUIM
            // so we need check the PIN state of each app inside of IccCardInfo
            else {
                int all_apps = iccInfo.getNumApplications();
                Log.v(TAG, "all_apps = " + all_apps);
                for (int i = 0; i < all_apps; i++) { // iterate all apps
                    IccCardApplication app = iccInfo.getApplication(i);
                    IccCardApplication.AppState iccState = app.app_state;
                    Log.v(TAG, "iccState = " + iccState);
                    if (iccState == IccCardApplication.AppState.APPSTATE_PIN
                        || iccState == IccCardApplication.AppState.APPSTATE_PUK)
                        return true;
                }
            }
        }
        return false;
    */
       /* TelephonyManager manager; 
        manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE); 
        if(manager.getSimState() != TelephonyManager.SIM_STATE_READY){
            return true;
        }*/
        /* to-pass-build, Xinyu Liu/dcjf34 */ 
        return false;
    }


    public static int getEmailFreeSpace(ContentResolver resolver)
    {
/*
        int freeEmail = 0;
        if (getSimType(TelephonyManager.PHONE_TYPE_GSM) == SIM_TYPE_USIM) {
            try {
                IIccPhoneBook iccIpb = IIccPhoneBook.Stub.asInterface(
                    ServiceManager.getService("simphonebook"));
                if (iccIpb != null) {
                    // to-pass-build, Xinyu Liu/dcjf34
                    freeEmail = 100;//iccIpb.getFreeEmail();
                }
            } catch (Exception ex) {
                // ignore it
            }
        }
        Log.i(TAG, " getFreeEmailSpace = " + freeEmail);
        return freeEmail;
*/
        Uri simUri =  SIM_FREEEMAIL_URI;
        int freeEmailSpace = 0;

        Cursor cursor;
        try{
            cursor = resolver.query(
                simUri,
                SIMCARD_COLUMN_NAMES,
                null,
                null,
               null);
        } catch(IllegalArgumentException e) {
            /* to be implemented, Xinyu Liu/dcjf34 */
            Log.e(TAG, "query error e = " + e);
            return 250;
        }

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    freeEmailSpace = Integer.parseInt(cursor.getString(SIMCARD_NUMBER_COLUMN_IDX));
                }
            }
            catch(Exception ex){
                Log.i(TAG, "getfreeEmailSpace, throw exception = " + ex.toString());
                return freeEmailSpace;
            }
            finally {
                cursor.close();
            }
            Log.i(TAG, "getEmailFreeSpace = " + freeEmailSpace);
            return freeEmailSpace;
        }

        Log.i(TAG, "failed, " + "getEmailFreeSpace = " + freeEmailSpace);
        return freeEmailSpace;
    }


    
    /**
     * get the person info by the given person ID
     * this function can be called when get device info to copy to USIM, one device contact need split to multiple entries on USIM
     * need consider the free email space, and combine the 2 nubmer + 1 email into one USIM entry
     */
   public static int getPeopleInfo4Export(ContentResolver resolver, long rawContactId, ArrayList<PeopleInfo> pInfoList, int[] freeEmailSpace){
   	int nCount = 0;
		if (rawContactId < 0) {
			return nCount;
		}
		String peoplename = getStructuredName(resolver, rawContactId);
		peoplename = buildSimName(peoplename);
		Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
				rawContactId);
		Uri entityUri = Uri.withAppendedPath(rawContactUri,
				Entity.CONTENT_DIRECTORY);
		// to get phone number and email address under this entity.
		ArrayList<String> emailList = new ArrayList<String>();
		ArrayList<String> numbersList = new ArrayList<String>();
		Cursor entityCursor = resolver.query(entityUri, new String[] {
				Entity.DATA_ID, Entity.MIMETYPE, Entity.DATA1 },
				Entity.MIMETYPE + "=" + '"'
						+ Email.CONTENT_ITEM_TYPE + '"'
						+ " OR " + Entity.MIMETYPE + "=" + '"'
						+ Phone.CONTENT_ITEM_TYPE + '"', null,
				null);
		if (null != entityCursor && entityCursor.moveToFirst()) {
			int mimeIndex = entityCursor.getColumnIndex(Entity.MIMETYPE);
			int dataIndex = entityCursor.getColumnIndex(Entity.DATA1);	
			String mimeType = null;
			String data1 = null;
			String tempnumber = null;
			String tempemail = null;
			do {
				if (!entityCursor.isNull(0)) {
					mimeType = entityCursor.getString(mimeIndex);
					data1 = entityCursor.getString(dataIndex);
					if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {
						tempemail = buildSimString(data1);
						if (tempemail != null && tempemail.length() <= MAX_SIM_EMAIL_LENGTH) {
							emailList.add(tempemail);
						}
					} else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
						tempnumber = buildSimNumber(data1);
						if (tempnumber != null && tempnumber.length() <= MAX_SIM_PHONE_NUMBER_LENGTH) {
							numbersList.add(tempnumber);
						}
					}
				}
			} while (entityCursor.moveToNext());

		}else {
			return nCount;
		}

		if (null != entityCursor) {
			entityCursor.close();
		}
		// Now we get all the phone number and emails under this rawcotnact id
		// next begin to format them in usim card contacts style

		Iterator<String> numberIterator = null;
		Iterator<String> emailIterator = null;
		String tempNumber = null;
		String tempEmail = null;
		int freeEmail = freeEmailSpace[0];
		if (freeEmail >=0) {
			Log.d(TAG, "getPeopleInfo4Export original freeEmailSpace = " + freeEmail);
		}
		if (null != numbersList) {
			numberIterator = numbersList.iterator();
		}
		if (null != emailList) {
			emailIterator = emailList.iterator();
		}
		while (numberIterator.hasNext()) {
			tempNumber = numberIterator.next();
			if (!TextUtils.isEmpty(tempNumber)) {
				PeopleInfo tmpPeopleinfo = new PeopleInfo();
				tmpPeopleinfo.peopleName = peoplename;
				tmpPeopleinfo.peopleId=rawContactId;
				tmpPeopleinfo.primaryNumber = tempNumber;
				while (numberIterator.hasNext()) {
					tempNumber = numberIterator.next();
					if (!TextUtils.isEmpty(tempNumber)) {
						tmpPeopleinfo.secondaryNumber = tempNumber;
						break;
					}
				}
				while ((freeEmail >= 0) && (emailIterator.hasNext())) {
					tempEmail = emailIterator.next();
					if (!TextUtils.isEmpty(tempEmail)) {
						freeEmail --;
						if (freeEmail >=0) {
							tmpPeopleinfo.primaryEmail = tempEmail;
						}
						break;
					}
				}
				pInfoList.add(tmpPeopleinfo);
				nCount++;
				// Log.d(TAG, "a18768, number iterate people info = "+tmpPeopleinfo.peopleName+", "+tmpPeopleinfo.primaryNumber+", "+tmpPeopleinfo.secondaryNumber+", "+tmpPeopleinfo.primaryEmail);
			}
		}
		// if there are still email address, we still need to insert them into
		// list
		if (!TextUtils.isEmpty(peoplename)) { // Do not allow creating a contact without name and number
			while ((freeEmail >= 0) && (emailIterator.hasNext())) {
				tempEmail = emailIterator.next();
				if (!TextUtils.isEmpty(tempEmail)) {
					freeEmail --;
					if (freeEmail >=0) {
						PeopleInfo tmpPeopleinfo = new PeopleInfo();
						tmpPeopleinfo.peopleName = peoplename;
						tmpPeopleinfo.peopleId=rawContactId;
						tmpPeopleinfo.primaryEmail = tempEmail;
						pInfoList.add(tmpPeopleinfo);
						nCount++;
						// Log.d(TAG, "a18768, email iterate people info = "+tmpPeopleinfo.peopleName+", "+tmpPeopleinfo.primaryNumber+", "+tmpPeopleinfo.secondaryNumber+", "+tmpPeopleinfo.primaryEmail);
					}
				}
			}
		}
		// release resource, cvmq63
		{
			if (null != numbersList) {
				numbersList = null;
			}
			if (null != emailList) {
				emailList = null;
			}
		}

		freeEmailSpace[0] = freeEmail; // -1 means some emails are discarded
		// Log.d(TAG, "getPeopleInfo4Export remain freeEmailSpace = " + freeEmailSpace[0]);
		return nCount;

	}

    public static int getPeopleInfo4ExportById(ContentResolver resolver, long contactId, ArrayList<PeopleInfo> pInfoList, int[] freeEmailSpace){
    	final ArrayList<Long> rawContactIdsList = ContactsUtils.queryForAllRawContactIds(resolver, contactId, true);
    	int nCount = 0;
    	
        Iterator<Long> it = rawContactIdsList.iterator();
        while(it.hasNext())
        {
            Long rawContactId = (Long) it.next();
            nCount += getPeopleInfo4Export(resolver, rawContactId, pInfoList, freeEmailSpace);
        }
        return nCount;
    }

}
