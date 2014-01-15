/*
 * Copyright (C) 2010 Motorola Inc.
 * All Rights Reserved.
 * Motorola Confidential Restricted.
 */
package motorola.app.admin;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import java.io.File;
import java.util.*;

/**
 *  This Class MotEdmDb contains methods that are useful for DB operations ,This DB contains the
 *   Infomation of Unique ID of each VPN , Cert ,E-Mail for all available Device Policy Admins
 */
public class MotEdmDb {
    private static final String CONFIG_UUID_TABLE = "UuidTable";
    private static final String ADMIN_COLUMN_ID   = "admin_id";
    private static final String EMAIL_COLUMN_ID   = "email_id";
    private static final String VPN_COLUMN_ID     = "vpn_id";
    private static final String CERT_COLUMN_ID    = "cert_id";

    // This path will change with package name
    private static final String DB_PATH = "/data/data/mot.app.admin/databases/";
    private static final String DB_NAME = "MotEDM.db";
    private static final String TAG = "Mot EDM DB";


    private static SQLiteDatabase myDataBase =null;
    private static MotEdmDb m_instance = null;

    private MotEdmDb(){

    }

    /**
     * This Method is Used to create single instance of this class
     */
    public static MotEdmDb getInstance() {
        if (m_instance == null) {
            m_instance = new MotEdmDb();
            if (init() != true) {
                Log.e(TAG," Database Init failed");
                return null;
            }
        }
        return m_instance;
    }

    /**
     *   This Method is used to Initialize (create /opend DB and table) the databse
     */
    private static boolean init() {
        Log.i(TAG," Database Init");
        String dbFullPath = DB_PATH + DB_NAME;
        try {
            File file = new File(dbFullPath);
            if (file.exists()) {
                myDataBase = SQLiteDatabase.openDatabase(dbFullPath,
                                                         null,
                                                         SQLiteDatabase.OPEN_READWRITE);
            } else {
                File parent = file.getParentFile();
                if (!(!parent.exists() && !parent.mkdirs())) {
                    myDataBase = SQLiteDatabase.openDatabase(
                               dbFullPath,
                               null,
                               SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.OPEN_READWRITE);

                    // Create a table with necessary fields.
                    myDataBase.execSQL("CREATE TABLE " + CONFIG_UUID_TABLE + " ("
                                       +ADMIN_COLUMN_ID+" TEXT ,"
                                       +EMAIL_COLUMN_ID+" TEXT , "
                                       +VPN_COLUMN_ID+" TEXT , "
                                       +CERT_COLUMN_ID+" TEXT);");
                    Log.i(TAG," Database Created");
                } else {
                    Log.d(TAG," Not able to create parent folder for Database");
                }
            }
        } catch (SQLiteException e) {
            Log.d(TAG," Database Creation failure", e);
            myDataBase = null;
            return false;
        }
        return true;
    }

    /**
     *  This finalize method is used to close the DB after all operations
     */
    @Override
    protected void finalize() throws Throwable {
        // close the db
        Log.i(TAG, " closing Mot EDM DB");
        if (myDataBase != null) {
            myDataBase.close();
            myDataBase = null;
        }
        super.finalize();
    }

    /**
     * This method can delete the table enties in DB for specified admin
     */
    public boolean deleteAdmin(String admin) {
        String admin_id[] = {admin};
        if (admin !=null && myDataBase != null) {
            if (0 != myDataBase.delete(CONFIG_UUID_TABLE, ADMIN_COLUMN_ID+"=?", admin_id)) {
                Log.i(TAG,admin +" is deleted from database");
                return true;
            }
        }
        return false;
    }

    /**
     * This method deletes all entries of the DB
     */
    public boolean deleteAll() {
        if (myDataBase !=null) {
            if (0 != myDataBase.delete ( CONFIG_UUID_TABLE,null , null)) {
                Log.i(TAG,"database deleted");
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to delete the table enties with specified admin and vpn ID
     */
    public boolean deleteVpnUuid(String admin, String vpnUuid) {
        String vpnId[]={vpnUuid, admin};
        if (myDataBase !=null) {
            if (0 != myDataBase.delete (CONFIG_UUID_TABLE,
                            "("+VPN_COLUMN_ID+" =?) AND ("+ADMIN_COLUMN_ID+"=?)",
                              vpnId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to delete the table enties with specified admin and email ID
     */
    public boolean deleteEmailUuid(String admin, String emailUuid) {
        String emailId[]={emailUuid , admin};
        if (myDataBase !=null) {
            if (0 != myDataBase.delete (CONFIG_UUID_TABLE,
                            "("+EMAIL_COLUMN_ID+" =?) AND ("+ADMIN_COLUMN_ID+"=?)",
                             emailId)) {
                return true;
            }
        }
        return false;
    }
    /**
     * This method is used to delete the table enties with specified admin and certificate ID
     */
    public boolean deleteCertUuid(String admin, String certUuid) {
        String certId[]={certUuid , admin};
        if(myDataBase !=null) {
            if (0 != myDataBase.delete (CONFIG_UUID_TABLE,
                                       "("+CERT_COLUMN_ID+" =?) AND ("+ADMIN_COLUMN_ID+"=?)",
                                        certId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is used to add table with the specified parameters, here parameter
     * admin is mandatory remaining parameters may be null
     */
    public boolean addUuid(String admin, String emailUuid, String certUuid, String vpnUuid) {
        if (myDataBase !=null && admin !=null) {

            ContentValues values = new ContentValues();
            values.put(ADMIN_COLUMN_ID, admin);
            values.put(EMAIL_COLUMN_ID, emailUuid);
            values.put(CERT_COLUMN_ID, certUuid);
            values.put(VPN_COLUMN_ID, vpnUuid);

            if (myDataBase.insert(CONFIG_UUID_TABLE, null, values) != -1) {
                Log.i(TAG,"Added new row to table");
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all Email IDs of the given admin
     */
    public Vector <String> getEmailUuids(String admin) {
        Vector <String> emailId= new Vector<String>();
        Cursor c = null;
        if (myDataBase == null) {
            return null;
        }

        try {
            c = myDataBase.query(CONFIG_UUID_TABLE,
                    new String[] { EMAIL_COLUMN_ID }, ADMIN_COLUMN_ID+"='" + admin + "'",
                    null, null, null, null);
            int totalRows =c.getCount();
            if (totalRows > 0) {
                int i=0, row=0;
                c.moveToFirst();
                while (row < totalRows) {
                    if (c.getString(0)!= null) {
                        emailId.add(c.getString(0));
                    }
                    row++;
                    c.moveToNext();
                }
                // START - IKDROIDPRO-569
                if (emailId.size() == 0) {
                    emailId = null;
                }
                // END - IKDROIDPRO-569
            } else {
                emailId = null;
                Log.i(TAG, "No of Email Entries= "  +totalRows);
            }
        } catch (SQLiteException e) {
            Log.e("Not EDM DB", "Query exception  "+ e);
            emailId = null;
        } finally {
            c.close();
        }
        return emailId;
    }

    /**
     * Returns all VPN IDs of the given admin
     */
    public Vector <String> getVpnUuids(String admin) {
        Vector <String> vpnId= new Vector<String>();
        Cursor c = null;
        if (myDataBase == null) {
            return null;
        }
        try {
            c = myDataBase.query(CONFIG_UUID_TABLE,
                    new String[] { VPN_COLUMN_ID }, ADMIN_COLUMN_ID+"='" + admin + "'",
                    null, null, null, null);
            int totalRows =c.getCount();
            if (totalRows > 0) {
                int i=0, row=0;
                c.moveToFirst();
                while(row < totalRows) {
                    if ( c.getString(0)!= null) {
                        vpnId.add(c.getString(0));
                    }
                    row++;
                    c.moveToNext();
                }
                // START - IKDROIDPRO-569
                if( vpnId.size() == 0 ) {
                    vpnId = null;
                }
                // END - IKDROIDPRO-569
            } else {
                vpnId = null;
                Log.i(TAG, "No of Vpn Entries= "  +totalRows);
            }
        } catch (SQLiteException e) {
            Log.e("Not EDM DB", "Query exception  "+ e);
            vpnId = null;
        } finally {
            c.close();
        }
        return vpnId;
    }

    /**
     * Returns all Certificate IDs of the given admin
     */
    public Vector <String> getCertUuids(String admin) {
        Vector <String> certId = new Vector<String>();
        Cursor c = null;

        if (myDataBase == null) {
            return null;
        }
        try {
            c = myDataBase.query(CONFIG_UUID_TABLE,
                    new String[] { CERT_COLUMN_ID }, ADMIN_COLUMN_ID+"='" + admin + "'",
                    null, null, null, null);
            int totalRows =c.getCount();
            if (totalRows > 0) {
                int  row=0;
                c.moveToFirst();
                while(row < totalRows) {
                    if ( c.getString(0)!= null) {
                        certId.add(c.getString(0));
                    }
                    row++;
                    c.moveToNext();
                }
                // START - IKDROIDPRO-569
                if(certId.size() == 0) {
                    certId = null;
                }
                // END - IKDROIDPRO-569
            } else {
                certId = null;
                Log.i(TAG, "No of Certificate Entries= " +totalRows);
            }
        } catch (SQLiteException e) {
            Log.e("Not EDM DB", "Query exception  "+ e);
            certId = null;
        } finally {
            c.close();
        }
        return certId;
    }

    /**
     * Displays the contents of DB
     */
    public void display() {//used to just for debugging
        Cursor c=null;
        if (myDataBase == null) {
            return;
        }
        try {
            c = myDataBase.query(CONFIG_UUID_TABLE, null, null, null, null, null, null);
            int totalRows = c.getCount();
            int totalColumns = c.getColumnCount();

            Log.i("" , "Rows =  " + totalRows + "\n Column = " +totalColumns );
            if (totalRows > 0 && totalColumns >0) {
                c.moveToFirst();
                int row=0;
                while(row < totalRows) {
                    Log.i("" , "-->Row No=  " + row );
                    for (int k=0;k <totalColumns ; k++) {
                        Log.w(""," Column No= "+ k + "    " + c.getString(k));
                    }
                    c.moveToNext();
                    row++;
                }
            } else {
                Log.i(TAG, "No Entries "  +totalRows);
            }
        } catch (SQLiteException e) {
            Log.e("Not EDM DB", "Query exception  "+ e);
        } finally {
            c.close();
        }
    }
}
