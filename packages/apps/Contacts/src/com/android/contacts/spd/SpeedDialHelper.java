
package com.android.contacts.spd;

import com.android.contacts.activities.DialtactsActivity;
import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.motorola.contacts.util.MEDialer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

//MOTO MOD BEGIN IKHSS7-2038
import com.motorola.internal.telephony.PhoneNumberUtilsExt;
//MOTO MOD END IKHSS7-2038

public class SpeedDialHelper {

    static boolean bExcludeVoicemail = false; //MOT FID 36927-Speeddial#1 IKCBS-2013

    //MOTO Dialer Code - IKHSS6-583 - Start
    private static final boolean CDBG = DialtactsActivity.CDBG;
    private static final boolean DBG = DialtactsActivity.DBG;
    private static final boolean VDBG = DialtactsActivity.VDBG;
    //MOTO Dialer Code - IKHSS6-583 - End

    public static void setupOptionMenu_SpdSetup(Fragment fragment, Menu menu) {
        final MenuItem speedDialMenuItem = menu.findItem(R.id.menu_speed_dial_setup);
        tracer.ErrorOn(speedDialMenuItem == null);

        Intent intent = new Intent().setClass(fragment.getActivity(), SpeedDialEditActivity.class);
        speedDialMenuItem.setVisible(true);
        speedDialMenuItem.setIntent(intent);
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////
    public static void setupOptionMenu_SpdAddRemove(final Activity activity, MenuItem menuItemAdd,
            MenuItem menuItemRemove, final String phoneNumber, final String numberType) {
        final String number = PhoneNumberUtils.stripSeparators(phoneNumber);
        if (TextUtils.isEmpty(number) || isVoiceMailNumber(activity, number)) {
            menuItemAdd.setVisible(false);
            menuItemRemove.setVisible(false);
        } else {
            tracer.t("setupOptionMenu_SpdAddRemove : " + number);
            boolean bSpeedDialed = Utils.hasSpeedDialByNumber(activity, number);

            menuItemAdd.setVisible(!bSpeedDialed);
            menuItemRemove.setVisible(bSpeedDialed);
            if (!bSpeedDialed) {
                // number NOT speed-dial yet, now option for add it
                menuItemAdd.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem arg0) {
                        Intent intent = new Intent();
                        intent.putExtra(SpeedDialPicker.CALL_NUMBER_TYPE, numberType);
                        intent.putExtra(SpeedDialPicker.CALL_NUMBER, number);
                        intent.setClass(activity, SpeedDialPicker.class);
                        activity.startActivity(intent);
                        return false;
                    }
                });
            } else {
                // number already speed-dialed, now option for remove it
                menuItemRemove.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem arg0) {
                        Utils.unassignSpeedDial(activity, number);
                        return false;
                    }
                });
            }
        }
    }

    private static boolean isVoiceMailNumber(final Context context, final String number) {
        TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        return TextUtils.equals(tm.getVoiceMailNumber(), number);
    }
}

// ///////////////////////////////////////////////////////////////////////////////////
// class SpeedDialViewManager view manager for speed-dial
class SpeedDialViewManager {

    private class ViewPair {

        View view;
        SpeedDialInfo data;
    }

    private List<ViewPair> mPairList = new LinkedList<ViewPair>();

    public View findViewByData(SpeedDialInfo data) {
        for (ViewPair pair : mPairList) {
            if (pair.data == data)
                return pair.view;
        }
        return null;
    }

    public boolean bindView(View view, SpeedDialInfo data) {
        for (Iterator<ViewPair> it = mPairList.iterator(); it.hasNext();) {
            ViewPair pair = it.next();
            if (pair.view == view && pair.data == data) {
                return false;
            } else if (pair.view == view || pair.data == data) {
                it.remove();
            }
        }
        ViewPair newPair = new ViewPair();
        newPair.view = view;
        newPair.data = data;
        mPairList.add(newPair);
        return true;
    }
}

// ///////////////////////////////////////////////////////////////////////////////////
// class SpeedDialDataManger data manager for speed-dial
class SpeedDialDataManger {

    public final static int SPD_COUNT = 9;
    private static SpeedDialDataManger sInstance = null;

    static SpeedDialDataManger getInstance(Context context) {
        synchronized (SpeedDialDataManger.class) {
            if (sInstance == null) {
                sInstance = new SpeedDialDataManger(context);
            }
            return sInstance;
        }
    }

    private SpeedDialInfo[] mSpeedDialDataArray;
    private SpeedDialQueryHelper mSpeedDialQueryHelper;

    private SpeedDialObserver mContactObserver;
    private Context mContext;

    private SpeedDialDataManger(Context context) {
        mContext = context;
        Handler handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                SpeedDialInfo data = (SpeedDialInfo)msg.obj;
                for (SpeedDialDataManger.OnDataChangedListener listenner : mListDataListeners) {
                    listenner.onDataChanged(data);
                }
            }
        };

        mSpeedDialQueryHelper = new SpeedDialQueryHelper(context, handler);
        mSpeedDialDataArray = new SpeedDialInfo[SPD_COUNT];
        for (int i = 0; i < SPD_COUNT; i++) {
            mSpeedDialDataArray[i] = new SpeedDialInfo(i + 1);
        }

        mContactObserver = new SpeedDialObserver();

        Uri uri = Uri.parse("content://com.android.contacts.spd/");
        context.getContentResolver().registerContentObserver(uri, true, mContactObserver);
        context.getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI,
                true, mContactObserver);
        RequeryAllSpeedDial();
    }

    public boolean isVoicemailPos(int pos) {
        return Utils.isVoicemailPos(pos);
    }

    public void unassignSpeedDial(int pos) {
        Utils.unassignSpeedDial(mContext, pos);
    }

    public boolean assignSpeedDial(int pos, String number) {
        return Utils.tryAssignSpeedDial(mContext, pos, number);
    }

    public boolean hasSpeedDialByNumber(String number) {
        return Utils.hasSpeedDialByNumber(mContext, number);
    }

    public String getSpeedDialNumberByPos(int pos) {
        return Utils.getSpeedDialNumberByPos(mContext, pos);
    }

    public SpeedDialInfo getData(int key) {
        return mSpeedDialDataArray[key - 1];
    }

    public void reQueryData(int key) {
        SpeedDialInfo data = new SpeedDialInfo(key);
        mSpeedDialDataArray[key - 1] = data;
        mSpeedDialQueryHelper.pushDataToQueue(data);
    }

    private void RequeryAllSpeedDial() {
        for (SpeedDialInfo df : mSpeedDialDataArray) {
            // df.reset();
            mSpeedDialQueryHelper.pushDataToQueue(df);
        }
    }

    private List<SpeedDialDataManger.OnDataChangedListener> mListDataListeners = new LinkedList<SpeedDialDataManger.OnDataChangedListener>();

    // //////////////////////////////////////////////////////////////////////////
    // interface for up level notifiy
    public interface OnDataChangedListener {

        public void onDataChanged(SpeedDialInfo data);
    }

    public void registerListenner(SpeedDialDataManger.OnDataChangedListener listenner) {
        if (!mListDataListeners.contains(listenner)) {
            mListDataListeners.add(listenner);
        }
    }

    public void unRegisterListenner(SpeedDialDataManger.OnDataChangedListener listenner) {
        mListDataListeners.remove(listenner);
    }

    class SpeedDialObserver extends ContentObserver {

        public SpeedDialObserver() {
            super(new Handler());
        }

        public void onChange(boolean selfChange) {
            // tracer.t("Database changed !");
            RequeryAllSpeedDial();
        }
    }
}

class SpeedDialInfo {

    public int mBindKey;
    public boolean mLocked;
    public String mBaseNumber;
    public String mContactName;
    public String mDisplayNumber;
    public Bitmap mContactPhoto;

    public SpeedDialInfo(int pos) {
        mBindKey = pos;
    }

    public void reset() {
        mBaseNumber = null;
        mLocked = false;

        mContactName = null;
        mDisplayNumber = null;
        mContactPhoto = null;
    }
}

class SpeedDialQueryHelper implements Runnable {

    // //////////////////////////////////////////////////////////////////////////////////////////////////
    // projection for speed-dial info
    private static class SpeedDialQuery {

        private final static Uri CONTENT_URI = Uri.parse("content://com.android.contacts.spd/spd");
        private final static String[] PROJECTION = {
                "ID", "NUMBER", "LOCK"
        };
        private final static int INDEX_NUMBER = 1;
        private final static int INDEX_LOCK = 2;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////
    // projection for contact info
    private static class ContactQuery_Uri {

        private final static Uri CONTENT_URI = Data.CONTENT_URI;
        private final static String[] PROJECTION = {
                Data.DATA1, Data.DISPLAY_NAME, Data.PHOTO_ID
        };
        private final static int INDEX_NUMBER = 0;
        private final static int INDEX_DISPLAYNAME = 1;
        private final static int INDEX_PHOTO = 2;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////
    // projection for contact info
    private static class ContactQuery_Number {

        private final static Uri CONTENT_URI = PhoneLookup.CONTENT_FILTER_URI;
        private final static String[] PROJECTION = {
                PhoneLookup.NUMBER, PhoneLookup.DISPLAY_NAME, PhoneLookup.TYPE, PhoneLookup.LABEL,
                PhoneLookup.PHOTO_ID
        };
        private final static int INDEX_NUMBER = 0;
        private final static int INDEX_DISPLAYNAME = 1;
        private final static int INDEX_TYPE = 2;
        private final static int INDEX_LABLE = 3;
        private final static int INDEX_PHOTO = 4;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////
    // projection for photo info
    private static class PhotoQuery {

        private final static Uri CONTENT_URI = Data.CONTENT_URI;
        private static final String[] PROJECTION = {
            Photo.PHOTO
        };
        private final static int INDEX_PHOTO = 0;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////
    // section for final data
    // /////////////////////////////////////////////////////////////////////////////////////////////////
    private List<SpeedDialInfo> mQueueToQuery = new LinkedList<SpeedDialInfo>();
    private Thread mQueryingThread;
    private Context mContext;
    private ContentResolver mContentResolver;
    private Resources mResources;
    private Bitmap mPhotoUnknowContact;
    private Bitmap mPhotoDefaultContact;
    private Handler mDataChangeHandler;

    public SpeedDialQueryHelper(Context context, Handler handler) {
        mContext = context;
        mDataChangeHandler = handler;
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        // MOTO Dialer Code Start - IKMAIN-35664
        Options opts = new Options();
        opts.inPurgeable = true;
        mPhotoUnknowContact = BitmapFactory.decodeResource(mResources,
                R.drawable.ic_thb_unknown_caller, opts);
        mPhotoDefaultContact = BitmapFactory.decodeResource(mResources,
                R.drawable.ic_contact_picture_holo, opts); //MOTO Dialer Code IKHSS7-1980
        // MOTO Dialer Code End - IKMAIN-35664
    }

    public void pushDataToQueue(SpeedDialInfo data) {
        synchronized (mQueueToQuery) {
            if (!mQueueToQuery.contains(data)) {
                mQueueToQuery.add(data);
            }
        }

        synchronized (this) {
            if (mQueryingThread == null) {
                mQueryingThread = new Thread(this);
                mQueryingThread.setPriority(Thread.MIN_PRIORITY);
                mQueryingThread.start();
            }
        }
    }

    @Override
    public void run() {
        long queueEmptyTick = 0;
        // for avoid start and stop thread frequently, thread will finished after the queue empty 10
        // seconds
        while (queueEmptyTick == 0 || System.currentTimeMillis() - queueEmptyTick < 10000) {
            SpeedDialInfo data = popDataFromQueue();
            if (data != null) {
                queueEmptyTick = 0;
                querySpeedDialInfo(data);
                notifyDataChange(data);
            } else if (queueEmptyTick == 0) {
                queueEmptyTick = System.currentTimeMillis();
            }
        }
        synchronized (this) {
            mQueryingThread = null;
        }
    }

    private SpeedDialInfo popDataFromQueue() {
        SpeedDialInfo data = null;
        synchronized (mQueueToQuery) {
            if (!mQueueToQuery.isEmpty()) {
                data = mQueueToQuery.remove(0);
            }
        }
        return data;
    }

    private void querySpeedDialInfo(SpeedDialInfo data) {
        Uri uri = Uri.withAppendedPath(SpeedDialQuery.CONTENT_URI,
                Uri.encode(String.valueOf(data.mBindKey)));
        Cursor c = mContentResolver.query(uri, SpeedDialQuery.PROJECTION, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                data.mBaseNumber = c.getString(SpeedDialQuery.INDEX_NUMBER);
                data.mLocked = c.getInt(SpeedDialQuery.INDEX_LOCK) != 0;
                if (!TextUtils.isEmpty(data.mBaseNumber)) {
                    if (PhoneNumberUtils.isUriNumber(data.mBaseNumber)) {
                        queryContactByUriName(data);
                    } else {
                        queryContactByNumber(data);
                    }
                }
            } else {
                data.mContactName = data.mBaseNumber = null;
                data.mDisplayNumber = null;
            }
            c.close();
        }
    }

    private void queryContactByUriName(SpeedDialInfo data) {
        String sipAddress = data.mBaseNumber;
        Uri uri = ContactQuery_Uri.CONTENT_URI;
        String selection = "upper(" + Data.DATA1 + ")=? AND " + Data.MIMETYPE + "='"
                + SipAddress.CONTENT_ITEM_TYPE + "'";
        String[] selectionArgs = new String[] {
            sipAddress.toUpperCase()
        };
        Cursor c = mContentResolver.query(uri, ContactQuery_Uri.PROJECTION, selection,
                selectionArgs, null);
        if (c != null) {
            if (c.moveToFirst()) {
                data.mContactName = c.getString(ContactQuery_Uri.INDEX_DISPLAYNAME);
                data.mDisplayNumber = c.getString(ContactQuery_Uri.INDEX_NUMBER);

                long photoId = c.getInt(ContactQuery_Uri.INDEX_PHOTO);
                if (photoId != 0) {
                    queryPhoto(data, photoId);
                } else {
                    data.mContactPhoto = mPhotoDefaultContact;
                }
            } else {
                data.mContactName = data.mBaseNumber;
                data.mContactPhoto = mPhotoUnknowContact;
                data.mDisplayNumber = null;
            }
            c.close();
        }
    }

    private void queryContactByNumber(SpeedDialInfo data) {
        Uri uri = Uri.withAppendedPath(ContactQuery_Number.CONTENT_URI,
                Uri.encode(data.mBaseNumber));
        Cursor c = mContentResolver.query(uri, ContactQuery_Number.PROJECTION, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                data.mContactName = c.getString(ContactQuery_Number.INDEX_DISPLAYNAME);
                String type = CommonDataKinds.Phone.getTypeLabel(mResources,
                        c.getInt(ContactQuery_Number.INDEX_TYPE),
                        c.getString(ContactQuery_Number.INDEX_LABLE)).toString();
                String number = c.getString(ContactQuery_Number.INDEX_NUMBER);
                if (!TextUtils.isEmpty(type)) {
                    data.mDisplayNumber = type + " " + number;
                } else {
                    data.mDisplayNumber = number;
                }

                long photoId = c.getInt(ContactQuery_Number.INDEX_PHOTO);
                if (photoId != 0) {
                    queryPhoto(data, photoId);
                } else {
                    data.mContactPhoto = mPhotoDefaultContact;
                }
            } else {
                //MOT MOD BEGIN IKHSS7-2038
                //To Support New API PhoneNumberUtilsExt.formatNumber
                data.mContactName = PhoneNumberUtilsExt.formatNumber(mContext, data.mBaseNumber,
                                    null, ContactsUtils.getCurrentCountryIso(mContext));
                //MOT MOD END IKHSS7-2038
                data.mContactPhoto = mPhotoUnknowContact;
                data.mDisplayNumber = null;
            }
            c.close();
        }
    }

    private void queryPhoto(SpeedDialInfo data, long photoId) {
        Uri uri = ContentUris.withAppendedId(PhotoQuery.CONTENT_URI, photoId);
        Cursor c = mContentResolver.query(uri, PhotoQuery.PROJECTION, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                byte[] photoData = c.getBlob(PhotoQuery.INDEX_PHOTO);
                if (null != photoData) {
                    // MOTO Dialer Code Start - IKMAIN-35664
                    Options opts = new Options();
                    opts.inPurgeable = true;
                    data.mContactPhoto = BitmapFactory.decodeByteArray(photoData, 0,
                            photoData.length, opts);
                    // MOTO Dialer Code End - IKMAIN-35664
                }
            } else {
                data.mContactPhoto = mPhotoDefaultContact;
            }
            c.close();
        }
    }

    private void notifyDataChange(SpeedDialInfo data) {
        Message msg = mDataChangeHandler.obtainMessage(0, 0, 0, data);
        mDataChangeHandler.sendMessage(msg);
    }
}

class tracer {

    public static void t(String str) {
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        String tag = "RED@" + ste.getFileName() + "(" + ste.getLineNumber() + ")";
        String strHead = "" + Process.myPid() + ":" + Process.myTid() + " ";
        Log.d(tag, strHead + str);
    }

    public static void o(String str) {
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        String tag = ste.getFileName() + "(" + ste.getLineNumber() + ")";
        Log.d(tag, str);
    }

    public static void f(String str) {
        StackTraceElement ste = new Throwable().getStackTrace()[1];
        String tag = ste.getFileName() + "(" + ste.getLineNumber() + ")";
        String strHead = ste.getMethodName() + "() ";
        Log.d(tag, strHead + str);
    }

    public static void dumpstack() {
        StackTraceElement[] arrSTE = new Throwable().getStackTrace();
        String strTabText = "";
        for (int i = 1; i < arrSTE.length; i++) {
            StackTraceElement ste = arrSTE[i];
            String tag = ste.getFileName() + "(" + ste.getLineNumber() + ") ";
            String strHead = ste.getMethodName() + "()";
            Log.d("stack", strTabText + tag + strHead);
            strTabText += " ";
        }
    }

    public static void ErrorOn(boolean b, String str) {
        if (b) {
            StackTraceElement ste = new Throwable().getStackTrace()[1];
            throw new IllegalStateException(ste.getFileName() + ":" + ste.getLineNumber() + " "
                    + str);
        }
    }

    public static void ErrorOn(boolean b) {
        if (b) {
            StackTraceElement ste = new Throwable().getStackTrace()[1];
            throw new IllegalStateException("Error at " + ste.getFileName() + ":"
                    + ste.getLineNumber() + " !");
        }
    }
}
