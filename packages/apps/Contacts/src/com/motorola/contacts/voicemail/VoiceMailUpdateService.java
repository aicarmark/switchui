package com.motorola.contacts.voicemail;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.TelephonyManager;
import android.util.Log;
/*
 * This service is for updating the number in voice mail contact.
 */
public class VoiceMailUpdateService extends IntentService {
    public VoiceMailUpdateService(String name) {
        super("VoiceMailUpdateService");
    }
    public VoiceMailUpdateService() {
        super("VoiceMailUpdateService");
    }
    private static final String TAG = "VoiceMailUpdateService";

    public static final String ACTION_UPDATE_VOICEMAIL =
            "com.motorola.contacts.ACTION_UPDATE_VOICE_MAIL_NUMBER";

    public static final String ACTION_UPDATE_NOTIFICATIONS =
            "com.android.phone.CallFeaturesSetting.VOICEMAIL_CHANGED";

    private Handler mHandler;
    private UpdateThread mUpdateThread;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }
    private class UpdateThread implements Runnable{

        @Override
        public void run() {
            updateVoiceMailNumber();
        }

    }
    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_UPDATE_VOICEMAIL.equals(intent.getAction())) {
             if(mUpdateThread != null){
                mHandler.removeCallbacks(mUpdateThread);
                mHandler.post(mUpdateThread);
            }
            else{
                mUpdateThread = new UpdateThread();
                mHandler.post(mUpdateThread);
            }
        }
    }
    private void updateVoiceMailNumber(){
        int rawId = getVMRawContactId();
        if(rawId == 0){
            return;
        }
        String newVMNumber="*86";
        newVMNumber = TelephonyManager.getDefault().getVoiceMailNumber();
        Uri dataUri=Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "data/phones");
        Long data_id=new Long(-1);
        String phoneNumber=null;

        Cursor dataCursor = getBaseContext().getContentResolver().query(dataUri, new String[]{Data._ID,Phone.NUMBER},
                Data.RAW_CONTACT_ID+"=?",
                new String[]{Integer.toString(rawId)}, null);

        if (dataCursor != null) {
            try {
                    while(dataCursor.moveToNext()) {
                        data_id=dataCursor.getLong(0);
                        phoneNumber=dataCursor.getString(1);
                    };
            } finally {
                    dataCursor.close();
            }
        }

        if(!newVMNumber.equals(phoneNumber)&&!data_id.equals(Long.valueOf(-1))){
            ContentValues values=new ContentValues();
            values.put(Phone.NUMBER, newVMNumber);
            values.put(Phone.TYPE, Phone.TYPE_WORK);
            values.putNull(Data.DATA3);
            Uri vmUri = ContentUris.withAppendedId(Data.CONTENT_URI, data_id);//Uri.parse("content://com.android.contacts/data");
            int count = getBaseContext().getContentResolver().update(vmUri, values, null, null);
        }
    }
    private int getVMRawContactId(){

            String indexSelection = Data.MIMETYPE + "=? AND " +
                                    Data.DATA1 + "=?";
            String[] selectionArgs = new String[] {
                "vnd.android.cursor.item/preloaded", "1" };
            Cursor indexCursor = null;

            try{
                indexCursor = getBaseContext().getContentResolver().query(Data.CONTENT_URI,
                        new String[] { Data.RAW_CONTACT_ID },
                        indexSelection, selectionArgs, null);

                if(indexCursor != null && indexCursor.getCount()> 0){
                    if(indexCursor.moveToFirst()){
                        return indexCursor.getInt(0);
                    }else{
                        return 0;
                    }
                }else{
                    return 0;
                }
            }catch(Exception e) {
                Log.e(TAG, "FAIL TO get raw contact id");
                return 0;
            }finally{
                if(indexCursor != null){
                    indexCursor.close();
                }
            }
    }
}
