/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-05-23   IKTABLETMAIN-348    XQH748      initial
 */

package com.motorola.filemanager.local;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ClipData.Item;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.HomePageLeftFileManagerFragment;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;
import com.motorola.filemanager.local.threads.DeleteThread;
//import com.motorola.filemanager.local.threads.Unzip;
//import com.motorola.filemanager.local.threads.Zip;
import com.motorola.filemanager.samba.service.DownloadServer;
import com.motorola.filemanager.ui.ActionModeHandler;
import com.motorola.filemanager.ui.CustomDialogFragment;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.IconifiedTextListAdapter;
import com.motorola.filemanager.ui.IconifiedTextView;
import com.motorola.filemanager.ui.ShadowBuilder;
import com.motorola.filemanager.ui.ActionModeHandler.ActionModeListener;
import com.motorola.filemanager.utils.FileUtils;
import com.motorola.filemanager.utils.MimeTypeUtil;

public abstract class LocalFileOperationsFragment extends LocalBaseFileManagerFragment {

    final static String TAG = "FileOperationsFragment";
    private String mZipFileName = null;
    private String[] mInputFileNames = null;
    private boolean mZipping = false;
    private boolean mUnzipping = false;
    private String mPassword = null;
    protected View mIconPlaceHolder = null;

    private String mTopPath = null;
    //protected Zip mZip;
   // protected Unzip mUnzip;
    private PopupMenu mPopupMenu;
    static final public int MESSAGE_COPY_FINISHED = 503;
    //static final public int MESSAGE_COPY_FAILED = 504;
    static final public int MESSAGE_DELETE_FINISHED = 505;
    static final public int MESSAGE_REFRESH = 506;
    static final public int MESSAGE_CANCEL_PROGRESS = 507;
    static final public int MESSAGE_MOVE_FINISHED = 508;
    //static final public int MESSAGE_MOVE_FAILED = 509;
    static final public int MESSAGE_ZIP_STARTED = 511;
    static final public int MESSAGE_ZIP_SUCCESSFUL = 512;
    static final public int MESSAGE_ZIP_FAILED = 513;
    static final public int MESSAGE_UNZIP_STARTED = 514;
    static final public int MESSAGE_UNZIP_SUCCESSFUL = 515;
    static final public int MESSAGE_UNZIP_FAILED = 516;
    static final public int MESSAGE_ZIP_STOPPED = 517;
    static final public int MESSAGE_UNZIP_STOPPED = 518;
    static final public int MESSAGE_SELECT_CHECKBOX = 519;
    static final public int MESSAGE_SET_DELETING_PROGRESS = 521;
    static final public int MESSAGE_ACTION_MODE_DESTROY = 540;

    public static final int ARG_DELETE_FAILED = 0;
    public static final int ARG_DELETE_COMPLETED = 1;

    private Object mZipUnzipLock = new Object();

    public LocalFileOperationsFragment() {
        super();
    }

    public LocalFileOperationsFragment(int fragmentLayoutID) {
        super(fragmentLayoutID);
    }

    public void startMultiSelect(boolean isContextMenu) {
        try {
            mActionModeHandler =
                    new ActionModeHandler((MainFileManagerActivity) getActivity(), mFileListView,
                            isContextMenu);
            if (mActionModeHandler != null) {
                mActionModeHandler.setActionModeListener(new ActionModeListener() {
                    public boolean onActionItemClicked(MenuItem item) {
                        return handleActionItemClicked(item);
                    }
                });
                mCurrentActionMode = mActionModeHandler.startActionMode();
                mFileListAdapter.setSelectMode(IconifiedTextListAdapter.MULTI_SELECT_MODE);
                BaseFileManagerFragment rightfrag =
                        (BaseFileManagerFragment) getFragmentManager()
                                .findFragmentById(R.id.details);
                rightfrag.setActionMode(mCurrentActionMode); /* In case of Column view, we need a copy of the action mode's handle to destroy it,
                                                             since its possible the column itself might have been destroyed. */
            }
        } catch (Exception e) {
            FileManagerApp.log("Exception in startMultiSelect", true);
            e.printStackTrace();
        }
    }

    @Override
    public void exitMultiSelect() {
        try {
            mContentView.setSelected(false);
            mFileListAdapter.unSelectAll();
            mFileListAdapter.setSelectMode(IconifiedTextListAdapter.NONE_SELECT_MODE);
            mActionModeHandler = null;
            mCurrentActionMode = null;
            mCurrentHandler.obtainMessage(MESSAGE_ACTION_MODE_DESTROY).sendToTarget();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleSelectAll() {
        (mFileListAdapter).selectAll();
        if (mCurrentActionMode != null) {
            mCurrentActionMode.invalidate();
        }
    }

    @Override
    public void handleUnSelectAll() {
        try {
            (mFileListAdapter).unSelectAll();
            if (mCurrentActionMode != null) {
                mCurrentActionMode.invalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleSelectMultiple() {
        try {
            if (mCurrentActionMode != null) {
                mCurrentActionMode.invalidate();
            } else {
                startMultiSelect(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isSomeFileSelected() {
        if (mFileListAdapter != null) {
            return (mFileListAdapter).isSomeFileSelected();
        } else {
            return false;
        }
    }

    public int isSingleFileSelected() {
        if (mFileListAdapter != null) {
            return (mFileListAdapter).isSingleFileSelected();
        } else {
            return -1;
        }
    }

    @Override
    public void addShortcut(File aFile) {
        if (aFile == null) {
            return;
        }
        if (mFileManagerApp != null) {
            if (mFileManagerApp.addShortcut(aFile)) {
                try {
                    BaseFileManagerFragment leftfrag =
                            (BaseFileManagerFragment) getFragmentManager()
                                    .findFragmentById(R.id.home_page);
                    if (leftfrag != null) {
                        leftfrag.updateShortcuts();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected boolean isSourceandDestSame() {
        try {
            ArrayList<String> pasteFiles = FileManagerApp.getPasteFiles();
            File pasteFile = new File(pasteFiles.get(0));
            File targetFile = new File(mDropTargetView.getPathInfo());

            if (!targetFile.isDirectory() && (pasteFile.getParent() != null)) {
                if (pasteFile.getParent().equals(targetFile.getParent())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void handleMove(boolean isContextMenu) {

        if (addCopyFileList(isContextMenu)) {
            FileManagerApp.setPasteMode(FileManagerApp.MOVE_MODE);
        }

    }

    @Override
    public void handlePaste(ArrayList<String> sourceFile, String destDir) {

        if (sourceFile == null) {
            return;
        }
        if (destDir == null) {
            destDir = FileManagerApp.getCurrDir().toString();
        }
        try {
            FileManagerApp.log(TAG + "onNewIntent ACTION_PASTE " + sourceFile.get(0) + " path: " +
                    destDir, false);

            Intent iDownload = new Intent(DownloadServer.ACTION_DOWNLOAD);
            if (iDownload != null) {
                iDownload.putStringArrayListExtra(MainFileManagerActivity.EXTRA_KEY_SOURCEFILENAME,
                        sourceFile);
                iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_DESTFILENAME, destDir + "/");
                if (FileManagerApp.getPasteMode() == FileManagerApp.COPY_MODE) {
                    iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON,
                            MainFileManagerActivity.EXTRA_VAL_REASONCOPY);
                } else {
                    iDownload.putExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON,
                            MainFileManagerActivity.EXTRA_VAL_REASONCUT);
                }
                mContext.sendBroadcast(iDownload);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void handleCopy(boolean isContextMenu) {
        if (addCopyFileList(isContextMenu)) {
            FileManagerApp.setPasteMode(FileManagerApp.COPY_MODE);
        }

    }

    @Override
    public void handlePrint(boolean isContextMenu) {
        try {
            if (isContextMenu) {
                if (mContextFile != null) {
                    printFile(mContextFile);
                }
            } else {
                if (mFileListView.getAdapter() != null) {
                    String selectedFile =
                            ((IconifiedTextListAdapter) mFileListView.getAdapter())
                                    .getSelectFiles().get(0);
                    if (selectedFile != null) {
                        printFile(FileUtils.getFile(selectedFile));
                    }
                }
            }
        } catch (Exception e) {
            if (mCurrentActionMode != null) {
                mCurrentActionMode.finish();
            }
        }
    }

    private boolean addCopyFileList(boolean isContextMenu) {
        try {
            ArrayList<String> tempCopy = null;
            if (isContextMenu) {
                tempCopy = new ArrayList<String>();
                tempCopy.add(mContextFile.getAbsolutePath());
            } else {
                if (mFileListView.getAdapter() != null) {
                    tempCopy = ((mFileListAdapter)).getSelectFiles();
                }
            }
            if (tempCopy != null && tempCopy.size() > 0) {
                FileManagerApp.setPasteFiles(tempCopy);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void handleDelete(boolean isContext) {
        try {
            ArrayList<String> deletFiles = null;
            if (isContext) {
                deletFiles = new ArrayList<String>();
                deletFiles.add(mContextFile.getAbsolutePath());
            } else {
                if (mFileListView.getAdapter() != null) {
                    deletFiles =
                            ((IconifiedTextListAdapter) mFileListView.getAdapter())
                                    .getSelectFiles();
                }
            }
            if (deletFiles != null && deletFiles.size() > 0) {
                deletSelected(deletFiles);
                if (mCurrentActionMode != null) {
                    mCurrentActionMode.finish();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

    protected void deletSelected(ArrayList<String> deletFiles) {
        try {
            new DeleteThread(deletFiles, mCurrentHandler, getActivity(), mContext).start();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void handleRename(String newName, boolean isContext) {
        try {
            String selectedFile = null;
            if (isContext) {
                selectedFile = mContextFile.getAbsolutePath();
            } else {
                if (mFileListView.getAdapter() != null) {
                    selectedFile =
                            ((IconifiedTextListAdapter) mFileListView.getAdapter())
                                    .getSelectFiles().get(0);
                }
            }
            if (selectedFile != null) {
                renameFileOrFolder(new File(selectedFile), newName, isContext);
            }
        } catch (Exception e) {
            if (mCurrentActionMode != null) {
                mCurrentActionMode.finish();
            }
        }

    }

    protected void renameFileOrFolder(File file, String newFileName, boolean isContext) {
        // If the user didn't put a name, toast a message letting him know for
        // the error.
        try {
            // Do not allow empty names or extension only names.
            // Names without extensions are allowed
            if ((newFileName.length() == 0) || (newFileName.lastIndexOf(".") == 0)) {
                //Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                DialogFragment newFragment =
                        CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_RENAME_EMPTY,
                                isContext,
                                mContext);
                // Show the dialog.
                newFragment.show(ft, "dialog");
            } else {
                File newFile = FileUtils.getFile(file.getParentFile(), newFileName);
                rename(file, newFile, isContext);
                // Send Intent to media scanner to scan the folder
                FileUtils.mediaScanFolder(mContext, FileManagerApp.getCurrDir());

            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void showDetailInfoFragment(IconifiedText item) {

    }

    protected boolean rename(File oldFile, File newFile, boolean isContext) {
        int toast = 0;
        boolean result = false;
        if (newFile != null && newFile.exists()) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            DialogFragment newFragment =
                    CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_RENAME_EXIST,
                            isContext,
                            mContext,
                            newFile.getName(),
                            true);
            // Show the dialog.
            newFragment.show(ft, "dialog");
        } else {
            File curPath = FileManagerApp.getCurrDir();
            String oldFileName = oldFile.getName();
            if ((curPath != null) && (newFile != null)) {
                if (oldFile.renameTo(newFile)) {
                    IconifiedText item =
                            mFileListAdapter.setNewName(oldFileName, newFile.getName(), curPath
                                    .getParentFile());

                    if (newFile.isDirectory()) {
                        //set the newly renamed folder path to the current path
                        FileManagerApp.setCurrDir(newFile);
                        //update right Fragment
                        updateRightFragment();

                        toast = R.string.folder_renamed;
                        //for folder renamed, need to check if it affect any shortcut
                        //and update shortcut if changed.
                        checkImpactForShortcut(oldFile, newFile);
                    } else {
                        // renamed file is a file on left panel,
                        // so the right fragment is showing
                        // detail view for it,
                        // need to update the detail view's path display info
                        showDetailInfoFragment(item);
                        toast = R.string.file_renamed;
                    }
                    //for both folder or file, need to check if its rename change impacts
                    //recent file, if so, below function will update recent file
                    checkImpactForRecentFile(oldFile, newFile);
                    result = true;
                }
            } else {
                if (oldFile.isDirectory()) {
                    toast = R.string.error_renaming_folder;
                } else {
                    toast = R.string.error_renaming_file;
                }
            }
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();

        }
        return result;
    }

    public void checkImpactForShortcut(File oldFile, File newFile) {
        boolean shortcutChanged =
                mFileManagerApp.syncShortcut(oldFile.getAbsolutePath(), newFile.getAbsolutePath());
        if (shortcutChanged) {
            Fragment leftfrag = getFragmentManager().findFragmentById(R.id.home_page);
            if ((leftfrag != null) &&
                    leftfrag.getClass().getName().equals(HomePageLeftFileManagerFragment.class
                            .getName())) {
                HomePageLeftFileManagerFragment titfrag =
                        (HomePageLeftFileManagerFragment) leftfrag;
                if (titfrag != null) {
                    titfrag.updateShortcuts();
                }
            }
        }
    }

    public void checkImpactForRecentFile(File oldFile, File newFile) {
        if (mFileManagerApp.syncRecentFile(oldFile.getAbsolutePath(), newFile.getAbsolutePath())) {
            Fragment leftfrag = getFragmentManager().findFragmentById(R.id.home_page);
            if ((leftfrag != null) &&
                    leftfrag.getClass().getName().equals(HomePageLeftFileManagerFragment.class
                            .getName())) {
                HomePageLeftFileManagerFragment titfrag =
                        (HomePageLeftFileManagerFragment) leftfrag;
                if (titfrag != null) {
                    titfrag.updateRecentFiles();
                }
            }
        }
    }

    public void createNewFolder(String foldername) {
        if (!TextUtils.isEmpty(foldername)) {
            File file = FileUtils.getFile(FileManagerApp.getCurrDir(), foldername);
            if (file.exists()) {
                Toast.makeText(mContext, R.string.renaming_folder_exist, Toast.LENGTH_SHORT).show();
                return;
            }
            if (file.mkdirs()) {
                if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                    LocalColumnViewFrameLeftFragment det =
                            (LocalColumnViewFrameLeftFragment) getFragmentManager()
                                    .findFragmentById(R.id.details);
                    if (det != null) {
                        det.getColumnFragment().refreshList(file.getParentFile());
                    }
                } else {
                    BaseFileManagerFragment det =
                            (BaseFileManagerFragment) getFragmentManager()
                                    .findFragmentById(R.id.details);
                    if (det != null) {
                        det.refreshList(file.getParentFile());
                    }
                }
            } else {
                Toast.makeText(mContext, R.string.error_creating_new_folder, Toast.LENGTH_SHORT)
                        .show();
            }
        }
        // If Folder name is empty
        else {
            Toast.makeText(mContext, R.string.renaming_folder_empty, Toast.LENGTH_SHORT).show();
        }
        mFileListAdapter.setNoItemsInfo(FileManagerApp.getCurrDir());
    }

    @Override
    public void openClickedFile(String fileAbsolutePath, IconifiedText text) {
        File clickedFile = new File(fileAbsolutePath);
        if (clickedFile != null) {
            if (clickedFile.isDirectory()) {
                // Clear the arrows from the view and reset the sort flags                
                mFileManagerApp.clearSortFlags();
                hideSortIcon();
                FileManagerApp.setCurrDir(clickedFile);
                browseTo(clickedFile);
            } else {
                openFile(clickedFile);
            }

        }
    }

    protected void openClickedFile(IconifiedText text) {
        String fileAbsolutePath = text.getPathInfo();
        openClickedFile(fileAbsolutePath, text);
    }

    @Override
    protected void openFile(File aFile) {
        try {
            super.openFile(aFile);
           /* if (FileUtils.isZipFile(aFile.getName())) {
                handleZipFile(aFile);
                return;
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

/*    private void stopZipThread() {
        if (mZip != null) {
            synchronized (mZipUnzipLock) {
                if (mZip.isAlive()) {
                    mZip.requestStop();
                }
                mZip = null;
            }
            // Change the dialog message if it is shown
            if (progressInfoDialog != null) {
                progressInfoDialog.setMessage(getString(R.string.zip_canceling));
            }
        }
        mZipping = false;
    }

    private void stopUnzipThread() {
        if (mUnzip != null) {
            synchronized (mZipUnzipLock) {
                if (mUnzip.isAlive()) {
                    mUnzip.requestStop();
                }
                mUnzip = null;
            }
            // Change the dialog message if it is shown
            if (progressInfoDialog != null) {
                progressInfoDialog.setMessage(getString(R.string.unzip_canceling));
            }
        }
        mUnzipping = false;
    }

    // called by multiple places to start unzip thread
    public void handleStartUnzip() {
        FileManagerApp.log(TAG + "enter handleStartUnzip()", false);

        mUnzip = new Unzip(mZipFileName, mTopPath, mPassword, mCurrentHandler);
        if (mUnzip != null) {
            synchronized (mZipUnzipLock) {
                mUnzip.start();
            }
        }
    }

    public void extractZipFile(final String password) {
        FileManagerApp.log(TAG + "enter extractZipFile " + password, false);
        int index = mZipFileName.lastIndexOf('.');
        mTopPath = mZipFileName.substring(0, index);
        mPassword = password;
        File folder = new File(mTopPath);
        FileManagerApp.log(TAG + "folder path" + mTopPath, false);

        mTopPath = mTopPath + "//";

        if (mPassword != null) {
            // means this function was called after Extract dialog was displayed
            // so check to make sure password was entered
            if (mPassword.isEmpty()) {
                Toast.makeText(mContext, R.string.password_cannot_be_blank, Toast.LENGTH_SHORT)
                        .show();
                return;
            }
        }
        if (folder.exists()) {
            FileManagerApp.log(TAG + "folder exist, show unzip_overwrite dialog", false);
            // display the overwrite confirmation dialog
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            DialogFragment newFragment =
                    CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_UNZIP_OVERWRITE,
                            false,
                            mContext);
            // Show the dialog.
            newFragment.show(ft, "dialog");
        } else {
            FileManagerApp.log(TAG + "folder not exist, create and unzip", false);
            boolean ret = folder.mkdirs(); //we ignore the return value
            if (!ret) {
                FileManagerApp.log(TAG + "mkdir failed", true);
            }
            handleStartUnzip();
        }
    }

    @Override
    public String getZipFileName(boolean isContext) {
        String zipFileName = null;
        try {
            int selSize =
                    ((IconifiedTextListAdapter) mFileListView.getAdapter()).getSelectFiles().size();
            boolean isFolder = false;
            if (isContext) {
                zipFileName = mContextFile.getName();
                isFolder = mContextFile.isDirectory();
            } else {
                if (mFileListView.getAdapter() != null && selSize > 0) {
                    zipFileName =
                            ((IconifiedTextListAdapter) mFileListView.getAdapter())
                                    .getSelectFiles().get(0);
                    File tmpFile = new File(zipFileName);
                    isFolder = tmpFile.isDirectory();
                    if (zipFileName.endsWith("/")) {
                        //If directory
                        zipFileName = zipFileName.substring(0, zipFileName.length() - 1);
                    }
                    zipFileName = zipFileName.substring(zipFileName.lastIndexOf("/") + 1);
                }
            }
            if (zipFileName != null) {
                int dotPosition = zipFileName.lastIndexOf(".");
                if ((dotPosition != -1) && !isFolder) {
                    zipFileName = zipFileName.substring(0, dotPosition) + ".zip";
                } else {
                    zipFileName = zipFileName + ".zip";
                }
            }
            FileManagerApp.log(TAG + "zip file name is: " + zipFileName, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zipFileName;
    }

    public void handleZip(boolean isContext, String zipName, final String password,
                          boolean isLeftZip) {
        ArrayList<String> selectedFiles = new ArrayList<String>();
        String[] inputFileNames = null;
        if (!isContext) {
            if (mFileListView.getAdapter() != null) {
                selectedFiles =
                        ((IconifiedTextListAdapter) mFileListView.getAdapter()).getSelectFiles();
            }
        } else {
            selectedFiles.add(mContextFile.getAbsolutePath());
        }

        if (selectedFiles.size() > 0) {
            inputFileNames = new String[selectedFiles.size()];
            selectedFiles.toArray(inputFileNames);
            mInputFileNames = inputFileNames;
            zipMultipleFiles(zipName, mEncryptionType, password, isLeftZip);
            FileManagerApp.log(TAG + "done calling zipMultipleFiles()", false);
        }
        if (mCurrentActionMode != null) {
            mCurrentActionMode.finish();
        }
    }

    private void zipMultipleFiles(final String zipFileName, final int encryptionType,
                                  final String password, boolean isLeftZip) {
        FileManagerApp.log(TAG + "enter zipMultipleFiles() with encryptionType: " + encryptionType,
                false);
        mZipFileName = null;
        mTopPath = null;
        if ((zipFileName != null) && (mInputFileNames != null)) {
            mZipFileName = zipFileName;
            if (!FileUtils.isZipFile(mZipFileName)) {
                if (mZipFileName.endsWith(".")) {
                    mZipFileName = mZipFileName + "zip";
                } else {
                    mZipFileName = mZipFileName + ".zip";
                }
            }
            // after the above operation, mZipeFileName will not be empty and will always
            // have an extension. So just need to check that the filename is not extension only
            if (mZipFileName.lastIndexOf(".") == 0) {
                Toast.makeText(mContext, R.string.renaming_file_empty, Toast.LENGTH_SHORT).show();
            } else if ((encryptionType > Zip.NO_ENCRYPTION) && password.isEmpty()) {
                FileManagerApp.log(TAG + "password is empty", false);
                Toast.makeText(mContext, R.string.password_cannot_be_blank, Toast.LENGTH_SHORT)
                        .show();
            } else {
                File curDir = FileManagerApp.getCurrDir();
                if (curDir != null) {
                    if (isLeftZip == false) {
                        mTopPath = curDir.getAbsolutePath() + File.separator;
                    } else {
                        /*this is needed so for lefthand operation, the zip file
                         * is added to left screen
                         */
  /*                      mTopPath = curDir.getParent() + File.separator;
                    }
                }

                mZipFileName = mTopPath + mZipFileName;
                File zipFile = new File(mZipFileName);
                if (zipFile.exists()) {
                    // Check to see if existing zip file is included in the
                    // files to be zipped
                    for (int i = 0; i < mInputFileNames.length; i++) {
                        if (mZipFileName.equals(mInputFileNames[i])) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setNegativeButton(android.R.string.cancel, null);
                            builder.setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            showOverwriteDialog(encryptionType, password);
                                        }
                                    });
                            builder.setMessage(R.string.zip_existing_zip_file);
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            return;
                        }
                    }
                    // If existing zip file is not included in the selected
                    // files to be zipped
                    // only show the Overwrite Dialog
                    showOverwriteDialog(encryptionType, password);
                } else {
                    mZip =
                            new Zip(mInputFileNames, mZipFileName, encryptionType, password,
                                    mCurrentHandler);
                    if (mZip != null) {
                        synchronized (mZipUnzipLock) {
                            mZip.start();
                        }
                    }
                }
            }
        } else {
            Toast.makeText(mContext, R.string.error_zipping_file, Toast.LENGTH_SHORT).show();
        }
    }
*/
    private void printFile(final File file) {
        Intent intent = new Intent("com.motorola.android.intent.action.PRINT");
        intent.setType(MimeTypeUtil.getMimeType(mContext, file.getName()));
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(mContext, R.string.send_not_available, Toast.LENGTH_SHORT).show();
        }
    }

    private void showOverwriteDialog(final int encryptionType, final String password) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setPositiveButton(R.string.overwrite, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
/*                mZip =
                        new Zip(mInputFileNames, mZipFileName, encryptionType, password,
                                mCurrentHandler);
                if (mZip != null) {
                    synchronized (mZipUnzipLock) {
                        mZip.start();
                    }
                }*/
            }
        });

        builder.setMessage(R.string.Overwrite_Zip_File);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

/*    private void handleZipFile(final File aFile) {
        String zipFileName = aFile.getName();
        if (zipFileName.lastIndexOf(".") <= 0) {
            // The filename is extension only or has no extension.
            // Do not allow extracting of such a file
            Toast.makeText(mContext, R.string.invalid_zip_name, Toast.LENGTH_LONG).show();
            return;
        }

        mZipFileName = aFile.getAbsolutePath();
        stopUnzipThread();
        try {

            // Initiate ZipFile object with the path/name of the zip file.
            ZipFile zipFile = new ZipFile(this.mZipFileName);

            // Check to see if the zip file is password protected
            if (zipFile.isEncrypted()) {
                // if yes, open dialog to input password
                FragmentTransaction ft = getFragmentManager().beginTransaction();

                DialogFragment newFragment =
                        CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_EXTRACT,
                                false,
                                mContext);
                // Show the dialog.
                newFragment.show(ft, "dialog");
            } else {
                extractZipFile(null);

            }
        } catch (ZipException e) {
            e.printStackTrace();
            Toast.makeText(mContext, R.string.error_unzip_file, Toast.LENGTH_SHORT).show();
        }
    }*/
    protected ProgressDialog progressInfoDialog = null;

    private void showProgressInfoDialog(String string) {
        try {
            if (getActivity().isFinishing()) {
                return;
            }
            hideProgressInfoDialog();
            progressInfoDialog = new ProgressDialog(mContext);
            progressInfoDialog.setMessage(string);
            progressInfoDialog.setIndeterminate(true);
            progressInfoDialog.setCancelable(true);
            progressInfoDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                }
            });

            progressInfoDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if ((progressInfoDialog != null) && (mZipping)) {
                           // stopZipThread();
                        } else if ((progressInfoDialog != null) && (mUnzipping)) {
                            //stopUnzipThread();
                        }

                        return true;
                    }
                    return false;
                }
            });

            if (!getActivity().isFinishing() && (progressInfoDialog != null)) {
                try {
                    progressInfoDialog.show();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public void hideProgressInfoDialog() {
        if (progressInfoDialog != null) {
            progressInfoDialog.dismiss();
            progressInfoDialog = null;
            mZipping = false;
            mUnzipping = false;

        }
    }

    @Override
    public void clearActionMode() {
        if (mCurrentActionMode != null) {
            mCurrentActionMode.finish(); // adapter.setItemChecked(position);
        }
    }

    public boolean handleActionItemClicked(MenuItem menuItem) {
        try {
            switch (menuItem.getItemId()) {
                case R.id.Copy :
                    handleCopy(false);
                    if (mCurrentActionMode != null) {
                        mCurrentActionMode.finish();
                    }
                    getActivity().invalidateOptionsMenu();
                    return true;
                case R.id.Delete :
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    DialogFragment newFragment =
                            CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_DELETE,
                                    false,
                                    mContext);
                    // Show the dialog.
                    newFragment.show(ft, "dialog");
                    return true;
                case R.id.Rename :
                    String fileName = getFileName(false);
                    FragmentTransaction ft1 = getFragmentManager().beginTransaction();
                    DialogFragment newFragment1 =
                            CustomDialogFragment.newInstance(CustomDialogFragment.DIALOG_RENAME,
                                    false,
                                    mContext,
                                    fileName);
                    // Show the dialog.
                    newFragment1.show(ft1, "dialog");
                    return true;
                case R.id.Move :
                    handleMove(false);
                    if (mCurrentActionMode != null) {
                        mCurrentActionMode.finish();
                    }
                    getActivity().invalidateOptionsMenu();
                    return true;
                case R.id.Info :
                    int pos = isSingleFileSelected();
                    if (pos != -1) {
                        (mFileListAdapter).getItem(pos);
                    }
                    return true;
                case R.id.Print :
                    handlePrint(false);
                    return true;
                case R.id.Zip :
                    FragmentTransaction ftc = getFragmentManager().beginTransaction();
                    DialogFragment newFragmentc =
                            CustomDialogFragment
                                    .newInstance(CustomDialogFragment.DIALOG_ZIP_OPTION,
                                            false,
                                            mContext,
                                            false);
                    // Show the dialog.
                    newFragmentc.show(ftc, "dialog");

                    return true;
                case R.id.Selectall :
                    handleSelectAll();
                    return true;
                case R.id.Unselectall :
                    handleUnSelectAll();
                    return true;

                default :
                    if (mCurrentActionMode != null) {
                        mCurrentActionMode.finish();
                    }

            }
        } catch (Exception e) {
            if (mCurrentActionMode != null) {
                mCurrentActionMode.finish();
            }
        }
        return true;
    }

    @Override
    public void onNewIntent(Intent intent) {
        try {
            String action = intent.getAction();
            FileManagerApp.log(TAG + "onNewIntent action= " + action, false);
            if (action == null) {
                return;
            }
            if (action.equals(MainFileManagerActivity.ACTION_TRANSFERFINISHED)) {
                int transferResult =
                        intent.getIntExtra(MainFileManagerActivity.EXTRA_KEY_TRANSFERRESULT, 0);
                int transferredNum =
                        intent.getIntExtra(MainFileManagerActivity.EXTRA_KEY_TRANSFERREDNUM, 0);
                if (MainFileManagerActivity.EXTRA_VAL_REASONCUT.equals(intent
                        .getStringExtra(MainFileManagerActivity.EXTRA_KEY_PASTEREASON))) {
                    Message.obtain(mCurrentHandler,
                            MESSAGE_MOVE_FINISHED,
                            transferResult,
                            transferredNum).sendToTarget();
                } else {
                    Message.obtain(mCurrentHandler,
                            MESSAGE_COPY_FINISHED,
                            transferResult,
                            transferredNum).sendToTarget();
                }
            }
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    protected void handleMessage(Message message) {
        BaseFileManagerFragment leftFragment = null;
        try {
            //protect the forceclose due to getFragmentManager() being null
            if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                leftFragment =
                        (BaseFileManagerFragment) getFragmentManager()
                                .findFragmentById(R.id.details);
            } else {
                leftFragment =
                        (BaseFileManagerFragment) getFragmentManager()
                                .findFragmentById(R.id.home_page);
            }
        } catch (Exception e) {
        }

        if (leftFragment == null) {
            return;
        }

        switch (message.what) {
            case MESSAGE_SELECT_CONTEXT_MENU :
                showContextMenu(message.obj, message.arg1);
                return;
            case MESSAGE_ZIP_STARTED :
                showProgressInfoDialog(getString(R.string.Zipping));
                mZipping = true;
                break;
            case MESSAGE_UNZIP_STARTED :
                showProgressInfoDialog(getString(R.string.Unzipping));
                mUnzipping = true;
                break;
            case MESSAGE_ZIP_SUCCESSFUL :
                hideProgressInfoDialog();
                refreshList();
                leftFragment.refreshLeft(false);
                break;
            case MESSAGE_UNZIP_SUCCESSFUL :
                hideProgressInfoDialog();
                if (message.obj != null) {
                    String newFolderName = (String) message.obj;
                    File tempFile = new File(newFolderName);
                    FileUtils.mediaScanFolder(getActivity(), tempFile);
                }
                refreshList();
                leftFragment.refreshLeft(true);
                break;
            case MESSAGE_ZIP_FAILED :
                hideProgressInfoDialog();
                Toast.makeText(mContext, R.string.error_zipping_file, Toast.LENGTH_SHORT).show();
                refreshList();
                break;
            case MESSAGE_UNZIP_FAILED :
                hideProgressInfoDialog();
                Toast.makeText(mContext, R.string.error_unzip_file, Toast.LENGTH_SHORT).show();
                refreshList();
                break;
            case MESSAGE_ZIP_STOPPED :
                hideProgressInfoDialog();
                Toast.makeText(mContext, R.string.stop_zip_file, Toast.LENGTH_SHORT).show();
                refreshList();
                break;

            case MESSAGE_UNZIP_STOPPED :
                hideProgressInfoDialog();
                Toast.makeText(mContext, R.string.stop_unzip_file, Toast.LENGTH_SHORT).show();
                refreshList();
                break;

            case MESSAGE_COPY_FINISHED :
                refreshList();
                leftFragment.refreshLeft(false);
                if (message.arg1 == DownloadServer.TRANSFER_COMPLETE) {
                    Toast.makeText(mContext,
                            mContext.getResources().getQuantityString(R.plurals.copy_success,
                                    message.arg2,
                                    message.arg2),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.error_copying, Toast.LENGTH_SHORT).show();
                }
                break;
            case MESSAGE_DELETE_FINISHED :
                hideLoadingDialog();
                if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                    refreshList();
                    leftFragment.refreshLeft(false);
                } else {
                    if (!FileManagerApp.getCurrDir().exists()) {
                        upOneLevel();
                    } else {
                        refreshList();
                        leftFragment.refreshLeft(false);
                    }
                }

                if (mFileManagerApp.getViewMode() == FileManagerApp.COLUMNVIEW) {
                    /* When in column view, leftFragment is LocalColumnViewFrameLeftFragment
                     * on right screen, so below is needed to update left screen home page
                     * memory info when in column view.
                     */
                    BaseFileManagerFragment homeFragment =
                            (BaseFileManagerFragment) getFragmentManager()
                                    .findFragmentById(R.id.home_page);
                    if (homeFragment != null) {
                        homeFragment.refreshLeft(false);
                    }
                }

                if (message.arg1 == ARG_DELETE_COMPLETED) {
                    Toast.makeText(mContext,
                            mContext.getResources().getQuantityString(R.plurals.delete_success,
                                    message.arg2,
                                    message.arg2),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.delete_failed, Toast.LENGTH_SHORT).show();
                }
                break;

            case MESSAGE_SELECT_DRAG_LOCATION :
                IconifiedText view = (IconifiedText) message.obj;
                openClickedFile(view.getPathInfo(), view);
                if (leftFragment != null) {
                    leftFragment.updateLeftFragment((FileManagerApp.getCurrDir()).getParentFile());
                }
                break;
            case MESSAGE_MOVE_FINISHED :
                refreshList();
                leftFragment.refreshLeft(true);
                if (message.arg1 == DownloadServer.TRANSFER_COMPLETE) {
                    Toast.makeText(mContext,
                            mContext.getResources().getQuantityString(R.plurals.move_success,
                                    message.arg2,
                                    message.arg2),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, R.string.error_moving, Toast.LENGTH_SHORT).show();
                }
                break;

            default :
                super.handleMessage(message);
                break;

        }
    }

    protected void showContextMenu(Object view, final int pos) {
        final IconifiedText selectedItem = (IconifiedText) mFileListAdapter.getItem(pos);

        if (selectedItem == null) {
            return;
        } else {
            selectedItem.getText();
            mContextFile = new File(selectedItem.getPathInfo());
            if (mPopupMenu != null) {
                mPopupMenu.dismiss();
            }
            mPopupMenu = new PopupMenu(getActivity(), (View) view);
            Menu menu = mPopupMenu.getMenu();
            mPopupMenu.getMenuInflater().inflate(R.menu.contextmenu, menu);
            if (mContextFile.isFile()) {
                menu.findItem(R.id.ContextMenuAddShortcut).setVisible(false);
            }
            if (mContextFile.isDirectory() ||
                    !(FileUtils.isPrintIntentHandled(mContext, mContextFile))) {
                menu.findItem(R.id.ContextMenuPrint).setVisible(false);
            }
            if (FileUtils.isZipFile(mContextFile.getName())) {
                menu.findItem(R.id.ContextMenuCompress).setVisible(false);
            }
            mPopupMenu.show();
            mPopupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                //@Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.ContextMenuOpen :
                            openClickedFile(selectedItem);
                            BaseFileManagerFragment leftFragment =
                                    (BaseFileManagerFragment) getFragmentManager()
                                            .findFragmentById(R.id.home_page);
                            if (leftFragment != null) {
                                leftFragment.updateLeftFragment((FileManagerApp.getCurrDir())
                                        .getParentFile());
                            }
                            break;
                        case R.id.ContextMenuCopy :
                            handleCopy(true);
                            getActivity().invalidateOptionsMenu();
                            break;
                        case R.id.ContextMenuDelete :
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            DialogFragment newFragment =
                                    CustomDialogFragment
                                            .newInstance(CustomDialogFragment.DIALOG_DELETE,
                                                    true,
                                                    mContext);
                            // Show the dialog.
                            newFragment.show(ft, "dialog");
                            break;
                        case R.id.ContextMenuMove :
                            handleMove(true);
                            getActivity().invalidateOptionsMenu();
                            break;
                        case R.id.ContextMenuCompress :
                            FragmentTransaction ftc = getFragmentManager().beginTransaction();
                            DialogFragment newFragmentc =
                                    CustomDialogFragment
                                            .newInstance(CustomDialogFragment.DIALOG_ZIP_OPTION,
                                                    true,
                                                    mContext,
                                                    false);
                            // Show the dialog.
                            newFragmentc.show(ftc, "dialog");
                            break;

                        case R.id.ContextMenuRename :
                            FragmentTransaction ft1 = getFragmentManager().beginTransaction();
                            DialogFragment newFragment1 =
                                    CustomDialogFragment
                                            .newInstance(CustomDialogFragment.DIALOG_RENAME,
                                                    true,
                                                    mContext,
                                                    mContextFile.getName());
                            // Show the dialog.
                            newFragment1.show(ft1, "dialog");
                            break;
                        case R.id.ContextMenuInfo :
                            IconifiedText text = null;
                            text = (IconifiedText) (mFileListAdapter).getItem(pos);
                            showDetailInfoFragmentFromContextMenu(text);
                            break;
                        case R.id.ContextMenuPrint :
                            handlePrint(true);
                            break;
                        case R.id.ContextMenuAddShortcut :
                            addShortcut(mContextFile);
                            break;
                    }

                    return true;
                }
            });
        }
    }

    public void showDetailInfoFragmentFromContextMenu(IconifiedText item) {
        if (item != null) {
            BaseFileManagerFragment leftFragment =
                    (BaseFileManagerFragment) getFragmentManager().findFragmentById(R.id.home_page);
            if (leftFragment != null) {
                leftFragment.updateLeftFragment(FileManagerApp.getCurrDir());
            }

            LocalLeftFileManagerFragment.mDetailInfoFragment.setItem(item);
            FragmentTransaction ft3 = getFragmentManager().beginTransaction();
            ft3.replace(R.id.details, LocalLeftFileManagerFragment.mDetailInfoFragment);
            ft3.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            ft3.addToBackStack(null);
            ft3.commit();
        }
    }

    @Override
    public void setUpViewSorting(final int sortMode) {
        mFileManagerApp.setSortMode(sortMode);
        new SortFilesTask().execute();
    }

    protected void setUpSorting(final SortingSetUpInterface iface, final int columnId,
                                final int imageViewId, final int sortMode) {

        RelativeLayout column = (RelativeLayout) mContentView.findViewById(columnId);
        column.setOnClickListener(new View.OnClickListener() {
            //@Override
            public void onClick(View v) {
                hideSortIcon();
                ImageView icon = (ImageView) mContentView.findViewById(imageViewId);
                if (icon != null) {
                    icon.setVisibility(View.VISIBLE);
                    if (!iface.isDescending()) {
                        iface.setDescending(true);
                        icon.setImageResource(android.R.drawable.arrow_down_float);
                    } else {
                        iface.setDescending(false);
                        icon.setImageResource(android.R.drawable.arrow_up_float);
                    }
                }
                setUpViewSorting(sortMode);
            }

        });
    }

    protected void hideSortIcon() {
        ImageView tmpView = (ImageView) mContentView.findViewById(R.id.folder_view_icon);
        tmpView.setVisibility(View.GONE);
        tmpView = (ImageView) mContentView.findViewById(R.id.date_view_icon);
        tmpView.setVisibility(View.GONE);
        tmpView = (ImageView) mContentView.findViewById(R.id.size_view_icon);
        tmpView.setVisibility(View.GONE);
        tmpView = (ImageView) mContentView.findViewById(R.id.type_view_icon);
        tmpView.setVisibility(View.GONE);
    }

    SortingSetUpInterface mNameSortIface = new SortingSetUpInterface() {
        //@Override
        public boolean isDescending() {
            return mFileManagerApp.mIsDescendingName;
        }

        //@Override
        public void setDescending(boolean isDescending) {
            mFileManagerApp.mIsDescendingName = isDescending;
        }
    };

    SortingSetUpInterface mDateSortIface = new SortingSetUpInterface() {
        //@Override
        public boolean isDescending() {
            return mFileManagerApp.mIsDescendingDate;
        }

        //@Override
        public void setDescending(boolean isDescending) {
            mFileManagerApp.mIsDescendingDate = isDescending;
        }
    };

    SortingSetUpInterface mSizeSortIface = new SortingSetUpInterface() {
        //@Override
        public boolean isDescending() {
            return mFileManagerApp.mIsDescendingSize;
        }

        //@Override
        public void setDescending(boolean isDescending) {
            mFileManagerApp.mIsDescendingSize = isDescending;
        }
    };

    SortingSetUpInterface mTypeSortIface = new SortingSetUpInterface() {
        //@Override
        public boolean isDescending() {
            return mFileManagerApp.mIsDescendingType;
        }

        //@Override
        public void setDescending(boolean isDescending) {
            mFileManagerApp.mIsDescendingType = isDescending;
        }
    };

    private class SortFilesTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... v) {

            switch (mFileManagerApp.getSortMode()) {

                case FileManagerApp.NAMESORT :
                    if (mFileManagerApp.mIsDescendingName == false) {
                        Collections.sort(mListDir, IconifiedText.mNameSort);
                        Collections.sort(mListFile, IconifiedText.mNameSort);
                    } else {
                        Collections.sort(mListDir, Collections
                                .reverseOrder(IconifiedText.mNameSort));
                        Collections.sort(mListFile, Collections
                                .reverseOrder(IconifiedText.mNameSort));
                    }
                    break;

                case FileManagerApp.DATESORT :
                    if (mFileManagerApp.mIsDescendingDate == false) {
                        Collections.sort(mListDir, IconifiedText.mTimeSort);
                        Collections.sort(mListFile, IconifiedText.mTimeSort);
                    } else {
                        Collections.sort(mListDir, Collections
                                .reverseOrder(IconifiedText.mTimeSort));
                        Collections.sort(mListFile, Collections
                                .reverseOrder(IconifiedText.mTimeSort));
                    }
                    break;

                case FileManagerApp.SIZESORT :
                    if (mFileManagerApp.mIsDescendingSize == false) {
                        Collections.sort(mListDir, IconifiedText.mSizeSort);
                        Collections.sort(mListFile, IconifiedText.mSizeSort);
                    } else {
                        Collections.sort(mListDir, Collections
                                .reverseOrder(IconifiedText.mSizeSort));
                        Collections.sort(mListFile, Collections
                                .reverseOrder(IconifiedText.mSizeSort));
                    }
                    break;

                case FileManagerApp.TYPESORT :
                    if (mFileManagerApp.mIsDescendingType == false) {
                        Collections.sort(mListDir, IconifiedText.mTypeSort);
                        Collections.sort(mListFile, IconifiedText.mTypeSort);
                    } else {
                        Collections.sort(mListDir, Collections
                                .reverseOrder(IconifiedText.mTypeSort));
                        Collections.sort(mListFile, Collections
                                .reverseOrder(IconifiedText.mTypeSort));
                    }
                    break;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            contentUpdate();
        }
    }

    AdapterView.OnItemLongClickListener fileListListener2 =
            new AdapterView.OnItemLongClickListener() {

                public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                               long id) {

                    IconifiedTextListAdapter adapter =
                            (IconifiedTextListAdapter) parent.getAdapter();

                    if (mFileManagerApp.mLaunchMode == FileManagerApp.NORMAL_MODE) {
                        if (mCurrentActionMode == null) {
                            if (FileManagerApp.getPasteMode() == FileManagerApp.NO_PASTE) {
                                adapter.setItemChecked(position);
                                if (mIconPlaceHolder != null) {
                                    mIconPlaceHolder.setVisibility(View.GONE);
                                }
                                startMultiSelect(false);
                            }
                        }
                        if (adapter.isSomeFileSelected()) {
                            mShadow =
                                    new ShadowBuilder(view, adapter.getSelectFiles().size(),
                                            mContext);
                            mData =
                                    ClipData.newPlainText("EXTRA_KEY_SOURCE", Integer
                                            .toString(FileManagerApp.getStorageMediumMode()));
                            view.startDrag(mData, mShadow, null, 0);
                            setDragCol(mCurrIn);

                        }
                        return true;
                    } else {
                        return false;
                    }
                }

            };

    AdapterView.OnItemClickListener fileListListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            IconifiedTextListAdapter adapter = (IconifiedTextListAdapter) parent.getAdapter();
            if (adapter == null) {
                return;
            }
            try {
                IconifiedText text = (IconifiedText) adapter.getItem(position);
                List<IconifiedText> mPageStaticItems = new ArrayList<IconifiedText>();
                int count = adapter.getCount();

                if (mFileManagerApp.mLaunchMode != FileManagerApp.NORMAL_MODE) {
                    adapter.setItemChecked(position);

                }
                if (mCurrentActionMode != null) {
                    adapter.setItemChecked(position);
                    mCurrentActionMode.invalidate();
                } else {
                    mPageStaticItems.clear();
                    for (int i = 0; i < count; i++) {
                        mPageStaticItems.add((IconifiedText) adapter.getItem(i));
                    }

                    if (mFileManagerApp.getUserSelectedViewMode() != mFileManagerApp.getViewMode()) {
                        switch (mFileManagerApp.getUserSelectedViewMode()) {
                            case FileManagerApp.LISTVIEW :
                                // Just do the switch to List View but no need to
                                // Browse inside the switchView statement since it
                                // will be performed by below code
                                FileManagerApp.log(TAG + "returning to list view ", false);
                                switchView(FileManagerApp.LISTVIEW, false);
                                break;
                            case FileManagerApp.GRIDVIEW :
                                // Just do the switch to Grid View but no need to
                                // Browse inside the switchView statement since it
                                // will be performed by below code
                                FileManagerApp.log(TAG + "returning to grid view ", false);
                                switchView(FileManagerApp.GRIDVIEW, false);
                                break;
                            case FileManagerApp.COLUMNVIEW :
                                FileManagerApp.log(TAG + "returning to column view ", false);
                                String fileAbsolutePath = text.getPathInfo();
                                File clickedFile = new File(fileAbsolutePath);
                                FileManagerApp.setCurrDir(clickedFile);
                                try {
                                    ((MainFileManagerActivity) getActivity())
                                            .enableViewModeSelection();
                                } catch (Exception e) {
                                    FileManagerApp.log(TAG + "Fail to enable view mode selection",
                                            false);
                                }
                                switchView(FileManagerApp.COLUMNVIEW, true);
                                return;
                        }
                    }

                    openClickedFile(text);
                    if (text.isDirectory()) {
                        BaseFileManagerFragment leftFragment =
                                (BaseFileManagerFragment) getFragmentManager()
                                        .findFragmentById(R.id.home_page);
                        if (leftFragment != null) {
                            leftFragment.updateLeftFragment((FileManagerApp.getCurrDir())
                                    .getParentFile());
                        }
                    }
                }
            } catch (Exception e) {

            }
        }
    };

    public File getClickedFile() {
        return null;
    }

    public File getDropPath() {
        return null;
    }

    //Drag and drop processing
    boolean processDrop(DragEvent event) {
        // Attempt to parse clip data
        if (!isTargetDestDragging() && !isSourceandDestSame()) {
            try {
                ClipData data = event.getClipData();
                if (data != null) {
                    if (data.getItemCount() > 0) {
                        Item item = data.getItemAt(0);
                        String srcStorage = (String) item.getText();
                        if (srcStorage != null) {
                            BaseFileManagerFragment det =
                                    (BaseFileManagerFragment) getFragmentManager()
                                            .findFragmentById(R.id.details);
                            if (det != null) {
                                if (FileManagerApp.getStorageMediumMode() == Integer
                                        .parseInt(srcStorage)) { // If same device storage, treat as move
                                    FileManagerApp.setPasteMode(FileManagerApp.MOVE_MODE);
                                } else {
                                    FileManagerApp.setPasteMode(FileManagerApp.COPY_MODE); //else treat as copy
                                }
                                det.handlePaste(FileManagerApp.getPasteFiles(), mDropTargetPath);
                                FileManagerApp.setPasteMode(FileManagerApp.NO_PASTE);
                                FileManagerApp.setPasteFiles(null);
                                ((MainFileManagerActivity) getActivity()).setIsDropSuccess(true);
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    @Override
    protected boolean onDragStarted(DragEvent event) {
        // Add items selected to the copy list for moving/copying
        addCopyFileList(false);
        FileManagerApp.log(TAG + " DRAG STARTED", false);
        mDragInProgress = true;
        int itemCount = mFileListView.getChildCount();
        // Lazily initialize the height of our list items
        if (itemCount > 0 && mDragItemHeight < 0 && mFileListView.getChildAt(0) != null) {
            mDragItemHeight = mFileListView.getChildAt(0).getHeight();
            ((MainFileManagerActivity) getActivity()).mDragItemHeight = mDragItemHeight;
        };
        return true;
    }

    @Override
    protected boolean onDrop(DragEvent event) {
        return processDrop(event);
    }

    @Override
    protected void onDragExited() {
        stopScrolling(mFileListView);
    }

    @Override
    protected void onDragEnded() {
        super.onDragEnded();
        if (mDragInProgress) {
            mDragInProgress = false;
            // Stop any scrolling that was going on
            stopScrolling(mFileListView);
        }
        if (((MainFileManagerActivity) getActivity()).getIsDropSuccess()) {
            if (mCurrentActionMode != null) {
                mCurrentActionMode.finish();
            }
        }
    }

    public int getDropTargetPosition() {
        return mDropTargetAdapterPosition;
    }

    public void updateColumnFragment() {
        LocalColumnViewFrameLeftFragment df =
                new LocalColumnViewFrameLeftFragment(R.layout.column_view);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.details, df);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();

        // Show the search icon instead of the widget when the fragment is updated.
        if (mContext != null) {
            ((MainFileManagerActivity) mContext).hideSearchWidget();
        }
    }

    /**
     * Called while dragging;  highlight possible drop targets, and autoscroll the list.
     */
    @Override
    protected void onDragLocation(DragEvent event) {
        try {
            super.onDragLocation(event);
            int rawTouchY = (int) event.getY();
            int rawTouchX = (int) event.getX();
            // Find out which item we're in and highlight as appropriate            
            int offset = 0;
            int dragWidth = 0;
            IconifiedTextView newTarget = null;
            IconifiedText newActTarget = null;
            int targetAdapterPosition = 0;

            if (mFileListView != null && mFileListView.getCount() >= 1 &&
                    mFileListView.getChildAt(0) != null) {
                offset = mFileListView.getChildAt(0).getTop();
                dragWidth = mFileListView.getChildAt(0).getMeasuredWidth();
                mDragItemHeight = mFileListView.getChildAt(0).getHeight();

                int targetScreenPositionY = (rawTouchY - offset) / (mDragItemHeight);
                int targetScreenPositionX = (rawTouchX - offset) / (dragWidth);
                int firstVisibleItem = mFileListView.getFirstVisiblePosition();
                if (mFileListView instanceof GridView) {
                    GridView gList = (GridView) mFileListView;
                    if (targetScreenPositionY == 0) {
                        if (firstVisibleItem == 0) {
                            targetAdapterPosition =
                                    firstVisibleItem + targetScreenPositionX +
                                            targetScreenPositionY;
                        } else {
                            targetAdapterPosition =
                                    firstVisibleItem + targetScreenPositionX +
                                            targetScreenPositionY - 1;
                        }
                    } else {
                        if (firstVisibleItem == 0) {
                            targetAdapterPosition =
                                    firstVisibleItem + targetScreenPositionX +
                                            (targetScreenPositionY * gList.getNumColumns());
                        } else {
                            targetAdapterPosition =
                                    firstVisibleItem + targetScreenPositionX +
                                            (targetScreenPositionY * gList.getNumColumns()) - 1;
                        }
                    }
                } else {
                    targetAdapterPosition = firstVisibleItem + targetScreenPositionY;
                }

                FileManagerApp.log(TAG + " DROP TARGET " + mDropTargetAdapterPosition + " -> " +
                        targetAdapterPosition, false);
                // Get the new target view
                try {
                    newTarget = (IconifiedTextView) mFileListView.getChildAt(targetScreenPositionX);
                    newActTarget =
                            (IconifiedText) mFileListView.getAdapter()
                                    .getItem(targetAdapterPosition);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // In any event, we're no longer dragging in the list view if newTarget is null
                if (newActTarget == null) {
                    if (newTarget == null) {
                        FileManagerApp.log(TAG + " Target Null...DRAG EXITED", false);
                        onDragExited();
                        return;
                    } else {
                        mDropTargetPath =
                                new File(newTarget.getIconText().getPathInfo()).getParent();
                        return;
                    }

                } else {
                    File temp = new File(newActTarget.getPathInfo());
                    if (!temp.isDirectory()) {
                        mDropTargetPath = temp.getParent();
                        mDropTargetAdapterPosition = targetAdapterPosition;
                        mDropTargetView = newActTarget;
                    } else {

                        // Save away our current position and view
                        mDropTargetAdapterPosition = targetAdapterPosition;
                        mDropTargetView = newActTarget;
                        mDropTargetPath = mDropTargetView.getPathInfo();

                        if (!isTargetDestDragging()) {
                            File dir = new File(newActTarget.getPathInfo());
                            File curr = FileManagerApp.getCurrDir();
                            if (dir.isDirectory() && !(dir.equals(curr))) {
                                sendHovertoTarget(newActTarget, -1);//the drag shadow is hovering send event to open the folder in this case          
                            }
                        }
                    }
                }
                // This is implementation of drag-under-scroll since Framework is not supporting it yet
                dragUnderScroll(mFileListView, rawTouchY);
            }
        } catch (Exception e) {

        }
    }

}
