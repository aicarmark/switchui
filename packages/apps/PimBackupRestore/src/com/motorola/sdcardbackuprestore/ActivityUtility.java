package com.motorola.sdcardbackuprestore;

import java.util.HashMap;
import com.motorola.sdcardbackuprestore.R;
import android.app.Activity;
import android.os.Bundle;
import android.content.Context;

public abstract class ActivityUtility extends Activity {


    protected int mBackupDest;
    protected int mStorageType = R.string.outer_storage;
    protected int mOpt = Constants.NO_ACTION;
    protected Constants mConstants = null;
    protected Context mContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        mConstants = new Constants(this);
        mContext = getApplicationContext();
        super.onCreate(savedInstanceState);
    }
    
    protected String getActionString(int operationType) {
        String actionString = null;
        switch (operationType) {
        case Constants.BACKUP_ACTION:
            actionString = getString(R.string.backup);
            break;
        case Constants.RESTORE_ACTION:
            actionString = getString(R.string.restore);
            break;
        case Constants.IMPORT3RD_ACTION:
            actionString = getString(R.string.import3rd);
            break;
        case Constants.EXPORTBYACCOUNT_ACTION:
            actionString = getString(R.string.export);
            break;
        default:
            break;
        }
        return actionString;
    }
    
    protected String getShowMsg(int msgPara, Object obj) {
        String showMsg = "";
        switch (msgPara) {
        case R.id.final_result:
            showMsg = getFinalResultMessage((HashMap<Integer, int[]>) obj);
            break;
        case R.id.no_sdcard:
            showMsg = getString(R.string.no_sdcard);
            break;
        case R.id.read_sdcard_error:
            showMsg = getString(R.string.read_sdcard_error);
            break;
        case R.id.read_phone_db_error:
            showMsg = getString(R.string.read_phone_db_error);
            break;
        case R.id.write_sdcard_error:
            showMsg = getString(R.string.write_sdcard_error);
            break;
        case R.id.write_phone_db_error:
            showMsg = getString(R.string.write_phone_db_error);
            break;
        case R.id.sdcard_read_only:
            showMsg = getString(R.string.sdcard_readonly);
            break;
        case R.id.insufficient_space:
            showMsg = getInsufficientSpaceMsg();
            break;
        case R.id.empty_storage:
            showMsg = getEmptyStorageShortMsg(obj == null ? 0 : (Integer)obj);
            break;
        case R.id.cancel:
            showMsg = getString(R.string.cancel);
            break;
        case R.id.copy_files_failed:
            showMsg = getString(R.string.copy_files_failed);
            break;
        case R.id.sdcard_file_contents_error:
            showMsg = getString(R.string.sdcard_file_contents_error);
            break;
        case R.id.out_of_memory_error:
            showMsg = getString(R.string.out_of_memory_error);
            break;
        default:
            showMsg = getString(R.string.unknown_error);
            break;
        }
        return showMsg;
    }
    
    protected String getFinalResultMessage(HashMap<Integer, int[]> obj) {
        StringBuilder finalString = new StringBuilder();
        StringBuilder failedItemList = new StringBuilder();
        StringBuilder successItemList = new StringBuilder();
        boolean nextIsFirstSuccess = true;
        boolean nextIsFirstFailed = true;
        int[] tmp;
        
        String actionString = getActionString(mOpt);
        String path = "";
        String from_or_to = "";
        String storageType = "";
        String isfile = "";
        switch (mOpt) {
        case Constants.BACKUP_ACTION:
            path = "/" + mConstants.DEFAULT_FOLDER;
            from_or_to = getString(R.string.to);
            storageType = getString(mStorageType);
            break;
        case Constants.RESTORE_ACTION:
            path = "";
            from_or_to = "";
            storageType = "";
            break;
        case Constants.IMPORT3RD_ACTION:
            path = "";
            from_or_to = getString(R.string.from);
            storageType = getString(mStorageType);
            isfile = " "+getString(R.string.files);
            break;
        default:
            break;
        }
        
        if (null != (tmp = obj.get(Constants.INDEX_CONTACT))) {
            if (tmp[0] == R.id.success) {
                nextIsFirstSuccess = false;
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.contacts) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                nextIsFirstSuccess = false;
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.contacts) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                nextIsFirstFailed = false;
                failedItemList.append("- ");
                failedItemList.append(getString(R.string.item_list, getString(R.string.contacts) + isfile, getShowMsg(tmp[0], Constants.CONTACTS_ACTION)));
            } 
        }
        if (null != (tmp = obj.get(Constants.INDEX_SMS))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.sms) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.sms) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.sms) + isfile, getShowMsg(tmp[0], Constants.SMS_ACTION)));
            }
        }
        if (null != (tmp = obj.get(Constants.INDEX_QUICKNOTE))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.quicknote) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.quicknote) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.quicknote) + isfile, getShowMsg(tmp[0], Constants.QUICKNOTE_ACTION)));
            } 
        }
        if (null != (tmp = obj.get(Constants.INDEX_CALENDAR))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.calendar) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.calendar) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.calendar) + isfile, getShowMsg(tmp[0], Constants.CALENDAR_ACTION)));
            } 
        }
        if (null != (tmp = obj.get(Constants.INDEX_APP))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.app) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.app) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.app) + isfile, getShowMsg(tmp[0], Constants.APP_ACTION)));
            } 
        }
        if (null != (tmp = obj.get(Constants.INDEX_BOOKMARK))) {
            if (tmp[0] == R.id.success) {
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                successItemList.append("- ").append(getString(R.string.item_list, getString(R.string.bookmark) + isfile, 
                        getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "")));
            } else if (tmp[0] == R.id.cancel) {
                String cancelResult = "";
                if (!nextIsFirstSuccess) {
                    successItemList.append("\r\n");
                } else {
                    nextIsFirstSuccess = false;
                }
                if (tmp[2] != 0) {
                    cancelResult = getString(R.string.done, Integer.toString(tmp[1]), "", Integer.toString(tmp[2]), "");
                }
                successItemList.append("- ");
                successItemList.append(getString(R.string.item_list, getString(R.string.bookmark) + isfile, 
                        getString(R.string.cancel) + cancelResult));
            } else {
                if (!nextIsFirstFailed) {
                    failedItemList.append("\r\n");
                } else {
                    nextIsFirstFailed = false;
                }
                failedItemList.append("- ").append(getString(R.string.item_list, getString(R.string.bookmark) + isfile, getShowMsg(tmp[0], Constants.BOOKMARK_ACTION)));
            } 
        }
        if (successItemList.length() != 0) {
            finalString.append(getString(R.string.final_success_result, actionString, from_or_to,
                    storageType, path, successItemList.toString()));
        }
        if (failedItemList.length() != 0) {
            if (successItemList.length() != 0) {
                finalString.append("\r\n");
            }
            finalString.append(getString(R.string.final_failed_result, actionString, failedItemList.toString()));
        }
        return finalString.toString();
    }
    
    abstract protected void startBackupRestoreService(int delOption);

    abstract protected String getProgressMessage(int type);

    abstract protected String getEmptyStorageShortMsg(int type);

    abstract protected String getInsufficientSpaceMsg();
}
