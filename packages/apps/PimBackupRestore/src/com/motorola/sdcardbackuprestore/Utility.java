package com.motorola.sdcardbackuprestore;

import java.io.File;
import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class Utility {
	private final static String TAG = "Utility";
	private Context mContext;
	
	public Utility(Context context) {
		mContext = context;
	}
	
	public void mediaScanFolder(File folder) {
        Intent intent = new Intent("com.motorola.internal.intent.action.MEDIA_SCANNER_SCAN_FOLDER");
        Uri uri = Uri.fromFile(folder);
        intent.setData(uri);
        try {
            mContext.sendBroadcast(intent);
        } catch (android.content.ActivityNotFoundException ex) { ex.printStackTrace(); }
    }
	
	public int getBackupDest() {
    	if (SdcardManager.isSdcardMounted(mContext, true)) {
			return Constants.OUTER_STORAGE;
		}
    	if (SdcardManager.hasInternalSdcard(mContext)) {
            if (SdcardManager.isSdcardMounted(mContext, false)) {
    			return Constants.INNER_STORAGE;
    		}
		}
        return R.id.no_sdcard;
	}
	
    public static boolean hasVcardFileInPathList(ArrayList<String> path_list) {
    	String tmpString = null;
    	File tmpFile = null;
    	File[] files = null;
    	if (path_list == null) return false;
    	for (int i = 0; i < path_list.size(); i++) {
    		tmpString = path_list.get(i);
    		if (tmpString == null || tmpString.equals("")) {
				continue;
			}
    		tmpFile = new File(tmpString);
    		if (!tmpFile.exists()) {
				continue;
			}
    		if (tmpFile.isFile() && tmpFile.getName().endsWith(Constants.vcardExtern)) {
    			return true;
			} else if (tmpFile.isDirectory()) {
				files = tmpFile.listFiles();
				if (files != null) {
				for (int j = 0; j < files.length; j++) {
					if (files[j].isFile() && files[j].getName().endsWith(Constants.vcardExtern)) {
						return true;
	                    }
					}
				}
			}
		}
    	return false;
    }
    
    public static ArrayList<Account> getAccount(Context context) {
		ArrayList<Account> mAccounts = new ArrayList<Account>();
		AccountManager mAccountManager = AccountManager.get(context);
		Account[] mAccArray = mAccountManager.getAccounts();
		for (int i = 0; i < mAccArray.length; i++) {
			Log.d("Account[" + i + "].name = ", mAccArray[i].name);
			Log.d("Account[" + i + "].type = ", mAccArray[i].type);
			Log.d("Account equals exchange account ?", "" + mAccArray[i].type.equals(Constants.EXCHANGE_ACCOUNT_TYPE));
			if (mAccArray[i].type.equals(Constants.LOCAL_ACCOUNT_TYPE)
					|| mAccArray[i].type.equals(Constants.EXCHANGE_ACCOUNT_TYPE)
					|| mAccArray[i].type.equals(Constants.GOOGLE_ACCOUNT_TYPE)) {
				mAccounts.add(mAccArray[i]);
			}
		}
		return mAccounts;
	}
    
    public static String getAccName(Context context, String str){
    	int index = str.indexOf(':');
    	if(index == -1) {
            if (str.equals(Constants.LOCAL_ACCOUNT_NAME)) {
		return context.getString(R.string.mobile);
	    } else if (str.equals(Constants.GCARD_ACCOUNT_NAME)) {
		return context.getString(R.string.gCard_contacts);
	    } else if (str.equals(Constants.CCARD_ACCOUNT_NAME)) {
		return context.getString(R.string.cCard_contacts);
	    } else {
                Log.e(TAG, "getTitle() error, should not be here");
    		return str;
        }
        } else {
    	    return str.substring(index+1,str.length());
    	}
    }
    
    public static String getAccType(Context context, String str){
    	if (str.equals(Constants.GOOGLE_ACCOUNT_TYPE) || str.startsWith(Constants.GACCOUNT_PREFIX)) {
		    return context.getString(R.string.google);
		} else if(str.equals(Constants.EXCHANGE_ACCOUNT_TYPE) || str.startsWith(Constants.EACCOUNT_PREFIX)){
		    return context.getString(R.string.corporate);
		} else if(str.equals(Constants.LOCAL_ACCOUNT_TYPE) || str.equals(Constants.LOCAL_ACCOUNT_NAME)){
	    	return context.getString(R.string.local);
	    } else if (str.equals(Constants.GCARD_ACCOUNT_NAME)) {
		    return context.getString(R.string.gCard_contacts);
		} else if (str.equals(Constants.CCARD_ACCOUNT_NAME)) {
		    return context.getString(R.string.cCard_contacts);
		} else {
	            Log.e(TAG, "getSummary() error, should not be here");
		    return str;
		}
    }
    
    public static boolean deleteDirFile(File folder, boolean deleteFolder, String suffix) {
        if (folder == null) {
            return false;
        } else if (!folder.exists() || !folder.isDirectory()) {
            return true;
        }
        File[] list = folder.listFiles();
        if (list != null && !list.equals("")) {
            for (int i = 0; i < list.length; i++) {
                if (suffix != null && !suffix.equals("")) {
                    if (list[i].getName().endsWith(suffix)) {
                        if(!list[i].delete()) return false;
                    }
                } else {
                    if(!list[i].delete()) return false;
                }
            }
//            if (!folder.delete()) {
//                return false;
//            }
            return true;
        }
        return false;
    }
    
}
