package com.motorola.firewall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Data;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.view.Window;
import android.widget.Toast;
import android.util.Log;  // Motorola, bxj487, IKCNDEVICS-461: fix add contact issue

import com.motorola.firewall.FireWall.Name;

public class AddBlackWhiteList extends Activity {
    private static final String[] PROJECTION = new String[] {
        Name._ID, // 0
    };

    private static final String[] DATA_PROJECTION = new String[] {
            Contacts.DISPLAY_NAME,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL
    };

    private static final int COLUMN_DISPLAY_NAME = 0;
    private static final int COLUMN_NUMBER = 1;
    private static final int COLUMN_TYPE = 2;
    private static final int COLUMN_LABEL = 3;

    private String mBlocktype;
    private String mExphone;
    private String mAddBy;  // Motorola, bxj487, IKCNDEVICS-461: fix add contact issue
    private AlertDialog malert = null;
    private int mCount = 0;
    private ProgressDialog mprogress;
    private boolean mdupnumberinname = false;
    private boolean mdialogdismiss = false;
    private boolean minvalidnumber = false;
    
    private DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface dialog) {
            AddBlackWhiteList.this.finish();
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(Name.CONTENT_URI);
        }
        // Do some setup based on the action being performed.
        mBlocktype = intent.getStringExtra("blocktype");
        if (! TextUtils.equals(mBlocktype, Name.BLACKLIST) && ! TextUtils.equals(mBlocktype, Name.WHITELIST)
                && ! TextUtils.equals(mBlocktype, Name.BLACKNAME) && ! TextUtils.equals(mBlocktype, Name.WHITENAME)) {
            return;
        }
        mExphone = intent.getStringExtra(mBlocktype);
        // BEGIN Motorola, bxj487, IKCNDEVICS-461: fix add contact issue
        Log.d("AddBlackWhiteList", "add " + mExphone + " to firewall " + mBlocktype);
        mAddBy = intent.getStringExtra("addby");
        // END Motorola, bxj487, IKCNDEVICS-461

        // BEGIN Motorola, bxj487, IKYTZTD-1603: fix empty number force close
        if (mExphone == null) return;
        if (TextUtils.equals(mExphone, "")) return;
        // END Motorola, bxj487, IKYTZTD-1603

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String selecting_string;
        int malertid;
        if (TextUtils.equals(mBlocktype, Name.BLACKNAME)) {
            selecting_string = Name.BLOCK_TYPE + " = '" + Name.BLACKLIST +"'";
            malertid = R.string.alert_dialog_add_black_name_confirm;
        } else if (TextUtils.equals(mBlocktype, Name.WHITENAME)) {
            selecting_string = Name.BLOCK_TYPE + " = '" + Name.WHITELIST +"'";
            malertid = R.string.alert_dialog_add_white_name_confirm;
        } else if (TextUtils.equals(mBlocktype, Name.WHITELIST)) {
            selecting_string = Name.BLOCK_TYPE + " = '" + mBlocktype +"'";
            malertid = R.string.alert_dialog_add_white_list_confirm;
        } else if (TextUtils.equals(mBlocktype, Name.BLACKLIST)) {
            selecting_string = Name.BLOCK_TYPE + " = '" + mBlocktype +"'";
            malertid = R.string.alert_dialog_add_black_list_confirm;
        } else {
            return;
        }
        if (TextUtils.equals(mBlocktype, Name.WHITELIST) || TextUtils.equals(mBlocktype, Name.BLACKLIST)) {
            String mrealnumber = PhoneNumberUtils.extractNetworkPortion(mExphone);
            if (TextUtils.isEmpty(mrealnumber)) {
                malert = new AlertDialog.Builder(AddBlackWhiteList.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_error_number).setCancelable(false).setInverseBackgroundForced(true)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finish();
                            }
                        }).show();
                malert.setOnDismissListener(dismissListener);
                return;
            }
        }
        Cursor cursor = getContentResolver().query(Name.CONTENT_URI, PROJECTION, selecting_string, null,
                Name.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            mCount = cursor.getCount();
            cursor.close();
        }
        malert = new AlertDialog.Builder(this).setIcon(
                android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                .setMessage(malertid).setCancelable(false)
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mdialogdismiss=true;
                        dialog.dismiss();
                        // BEGIN Motorola, bxj487, IKCNDEVICS-461: fix add contact issue
                        if (TextUtils.equals(mAddBy, "uri")) {
                            addMultipleNumber();
                        } else {
                            addSingleNumber();
                        }
                        // END Motorola, bxj487, IKCNDEVICS-461
                    }
                 })
                 .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int whichButton) {
                                 finish();
                       }
                 })
                 .show();
        malert.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    if(!mdialogdismiss) {
                        AddBlackWhiteList.this.finish();
                    }
                }
        });

    }

    private void addSingleNumber() {
        if (! TextUtils.isEmpty(mExphone)) {
            if (mCount >= Name.MAXNAMEITEMS) {
                malert = new AlertDialog.Builder(AddBlackWhiteList.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_name_content).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finish();
                            }
                        }).show();
                malert.setOnDismissListener(dismissListener);
                return;
            }

            String mnumberkey = PhoneNumberUtils.extractNetworkPortion(mExphone);  // Motorola, bxj487, IKCNDEVICS-461: fix add contact issue
            int mstrid = R.string.alert_dialog_dup_number_black_list;
            int mtostid = R.string.ext_add_black_number_toast;
            if (TextUtils.equals(mBlocktype, Name.BLACKLIST)) {
                mstrid = R.string.alert_dialog_dup_number_black_list;
                mtostid = R.string.ext_add_black_number_toast;
                mnumberkey = PhoneNumberUtils.extractNetworkPortion(mExphone);
            } else if (TextUtils.equals(mBlocktype, Name.BLACKNAME)) {
                mstrid = R.string.alert_dialog_dup_name_black_list;
                mtostid = R.string.ext_add_black_name_toast;
            } else if (TextUtils.equals(mBlocktype, Name.WHITELIST)) {
                mstrid = R.string.alert_dialog_dup_number_white_list;
                mtostid = R.string.ext_add_white_number_toast;
                mnumberkey = PhoneNumberUtils.extractNetworkPortion(mExphone);
            } else if (TextUtils.equals(mBlocktype, Name.WHITENAME)) {
                mstrid = R.string.alert_dialog_dup_name_white_list;
                mtostid = R.string.ext_add_white_name_toast;
            }
            if (NameListActivity.checkDup (getContentResolver() ,mnumberkey, mBlocktype, false)) {
                malert = new AlertDialog.Builder(AddBlackWhiteList.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(mstrid).setCancelable(false).setInverseBackgroundForced(true)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        finish();
                                    }
                                }).show();
                malert.setOnDismissListener(dismissListener);
            } else {
                ContentValues values = new ContentValues();
                // BEGIN Motorola, bxj487, IKCNDEVICS-461: fix add contact issue
                String mbtype;
                if (mBlocktype.equals(Name.WHITENAME) || mBlocktype.equals(Name.WHITELIST)) { // Motorola, bxj487, IKCNDEVICS-1343: fix add sms number issue
                    mbtype = Name.WHITELIST;
                } else {
                    mbtype = Name.BLACKLIST;
                }
                values.put(Name.PHONE_NUMBER_KEY, mnumberkey);
                values.put(Name.FOR_CALL, true);
                values.put(Name.FOR_SMS, true);
                values.put(Name.BLOCK_TYPE, mbtype);
                // END Motorola, bxj487, IKCNDEVICS-461
                getContentResolver().insert(Name.CONTENT_URI, values);
                Toast.makeText(this, mtostid, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void addMultipleNumber() {
        Uri mLookupuri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, mExphone);
        Uri mDatauri = getDataUri(mLookupuri);
        Cursor mcursor = getContentResolver().query(mDatauri, DATA_PROJECTION,
                Data.MIMETYPE + " = '" + Phone.CONTENT_ITEM_TYPE +"'", null, null );
        if (mcursor != null) {
            if (mcursor.getCount() + mCount > Name.MAXNAMEITEMS) {
                malert = new AlertDialog.Builder(AddBlackWhiteList.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_dialog_name_content).setCancelable(false)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                finish();
                            }
                        }).show();
                malert.setOnDismissListener(dismissListener);
            } else if (mcursor.moveToFirst()) {
                new addContactNumber().execute(mcursor);
            } else {
                Log.d("AddBlackWhiteList", "addMultipleNumber with invalid URI");  // Motorola, bxj487, IKCNDEVICS-461: fix add contact issue
            }
        }

    }

    private class addContactNumber extends AsyncTask <Cursor, String, String> {

        @Override
        protected void onPreExecute() {
            mprogress = new ProgressDialog(AddBlackWhiteList.this, ProgressDialog.STYLE_SPINNER);
            if (mBlocktype.equals(Name.WHITENAME)) {
                mprogress.setTitle(R.string.menu_insert_white_list);
            } else {
                mprogress.setTitle(R.string.menu_insert_black_list);
            }
            mprogress.setIcon(android.R.drawable.ic_menu_add);
            mprogress.setCancelable(false);
            mprogress.show();

        }

        @Override
        protected String doInBackground(Cursor... params) {
            Cursor mDatac = params[0];
            int invalidcount = 0;
            do {
                String mphone = mDatac.getString(COLUMN_NUMBER);
                String mnumberkey = PhoneNumberUtils.extractNetworkPortion(mphone);
                if (TextUtils.isEmpty(mnumberkey)) {
                    invalidcount++;
                    continue ;
                }
                String mbtype;
                if (mBlocktype.equals(Name.WHITENAME)) {
                    mbtype = Name.WHITELIST;
                } else {
                    mbtype = Name.BLACKLIST;
                }
                if (NameListActivity.checkDup (getContentResolver() ,mnumberkey, mbtype, false)) {
                    String dupWord;
                    if (mBlocktype.equals(Name.WHITENAME)) {
                        dupWord = getResources().getString(R.string.is_dup_in_white_list, mnumberkey);
                    } else {
                        dupWord = getResources().getString(R.string.is_dup_in_black_list, mnumberkey);
                    }
                    publishProgress(dupWord);
                    mdupnumberinname = true;
                } else {
                    ContentValues values = new ContentValues();
                    values.put(Name.PHONE_NUMBER_KEY, mnumberkey);
                    values.put(Name.FOR_CALL, true);
                    values.put(Name.FOR_SMS, true);
                    String addNumber;
                    if (mBlocktype.equals(Name.WHITENAME)) {
                        values.put(Name.BLOCK_TYPE, Name.WHITELIST);
                        addNumber = getResources().getString(R.string.added_to_white_list, mnumberkey);
                    } else {
                        values.put(Name.BLOCK_TYPE, Name.BLACKLIST);
                        addNumber = getResources().getString(R.string.added_to_black_list, mnumberkey);
                    }
                    publishProgress(addNumber);
                    values.put(Name.CACHED_NAME, mDatac.getString(COLUMN_DISPLAY_NAME));
                    values.put(Name.CACHED_NUMBER_TYPE, mDatac.getInt(COLUMN_TYPE));
                    values.put(Name.CACHED_NUMBER_LABEL, mDatac.getString(COLUMN_LABEL));
                    getContentResolver().insert(Name.CONTENT_URI, values);
                    //publishProgress(addNumber);
                }
            } while (mDatac.moveToNext());
            if (mDatac.getCount() == invalidcount)
                minvalidnumber = true;
            return null;
        }


        @Override
        protected void onPostExecute(String parsedText) {
            //mprogress.dismiss();
            //if all the numbers are invalid, toast message will be omitted
            if (minvalidnumber) {
                finish();
                return;
            }
            int mstrid;
            int mtostrid;
            if (mBlocktype.equals(Name.WHITENAME)) {
                mstrid = R.string.alert_dialog_dup_numbers_white_list;
                mtostrid = R.string.ext_add_white_name_number_toast;
            } else {
                mstrid = R.string.alert_dialog_dup_numbers_black_list;
                mtostrid = R.string.ext_add_black_name_number_toast;
            }
            mprogress.dismiss();
            if (mdupnumberinname) {
                malert = new AlertDialog.Builder(AddBlackWhiteList.this).setIcon(
                        android.R.drawable.ic_dialog_alert).setTitle(android.R.string.dialog_alert_title)
                        .setMessage(mstrid).setCancelable(false).setInverseBackgroundForced(true)
                        .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        finish();
                                    }
                                }).show();
                malert.setOnDismissListener(dismissListener);
            } else {
                Toast.makeText(AddBlackWhiteList.this, mtostrid, Toast.LENGTH_LONG).show();
                finish();
            }
        }

        @Override
        protected void onProgressUpdate(String... args) {
            mprogress.setMessage(args[0]);
        }
    };

    private Uri getDataUri(Uri lookupUri) {
        lookupUri = Contacts.lookupContact(getContentResolver(), lookupUri);

        final long contactId = ContentUris.parseId(lookupUri);
        return Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
                Contacts.Data.CONTENT_DIRECTORY);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
