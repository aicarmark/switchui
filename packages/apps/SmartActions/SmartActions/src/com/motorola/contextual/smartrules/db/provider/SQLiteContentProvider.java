package com.motorola.contextual.smartrules.db.provider;


import com.motorola.contextual.smartrules.db.SQLiteManager;

import android.content.ContentProvider;
import android.content.Context;
import android.database.sqlite.SQLiteTransactionListener;


public abstract class SQLiteContentProvider extends ContentProvider implements SQLiteTransactionListener {

    @SuppressWarnings("unused")
	private static final String TAG = SQLiteContentProvider.class.getSimpleName();

    private static Context 				mContext;


    @Override
    public boolean onCreate() {
        mContext = this.getContext();
        return true;
    }

        
    /**
     * @param openFrom - for debugging
	 * @return the db manager instance
	 */
	public static SQLiteManager getWritableDb(String openFrom) {
		return SQLiteManager.openForWrite(mContext, openFrom);
	}

    /**
     * @param openFrom - for debugging
	 * @return the db manager instance
	 */
	public static SQLiteManager getReadableDb(String openFrom) {
		return SQLiteManager.openForRead(mContext, openFrom);
	}

	/* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
    	//TODO: put a function here to close all Managers
        super.finalize();
    }


    /** required by SQLiteTransactionListener */
    public void onBegin() {
    }

    /** required by SQLiteTransactionListener */
    public void onCommit() {
    }

    /** required by SQLiteTransactionListener */
    public void onRollback() {
    }



}
