/*
 * Copyright (c) 2011 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date         CR              Author      Description
 * 2011-11-04       XQH748      initial
 */

package com.motorola.filemanager;

import java.io.File;
import java.util.ArrayList;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.widget.AbsListView;
import android.widget.Toast;

//import com.motorola.filemanager.local.threads.Zip;
import com.motorola.filemanager.ui.ActionModeHandler;
import com.motorola.filemanager.ui.IconifiedText;
import com.motorola.filemanager.ui.ShadowBuilder;
import com.motorola.filemanager.utils.FileUtils;
import com.motorola.filemanager.utils.MimeTypeUtil;

public abstract class BaseFileManagerFragment extends Fragment implements OnDragListener,
        OnTouchListener {

    final static String TAG = "BaseBrowseFragment";
    protected int mfragmentLayoutID;
    protected View mContentView;
    protected Context mContext;
    protected Handler mCurrentHandler;

    // Set progress bar, arg1 = current value, arg2 = max value
    static final public int MESSAGE_SET_LOADING_PROGRESS = 510;

    static protected String DELIM = " > ";
    protected String mDisplayText = null;

    // Drag and Drop variables
    protected ClipData mData;
    protected ShadowBuilder mShadow;
    protected int mTouchX;
    protected static final boolean DEBUG_DRAG_DROP = false; // MUST NOT SUBMIT SET TO TRUE
    private static final int NO_DROP_TARGET = -1;
    // Total height of the top and bottom scroll zones, in pixels
    protected static final int SCROLL_ZONE_SIZE = 64;
    protected static final int SCROLL_ZONE_SIZE_X = 190;
    protected static final int SCROLL_OFFSET = 80;
    // The amount of time to scroll by one pixel, in ms
    protected static final int SCROLL_SPEED = 4;
    protected static final int SCROLL_SP = 50;
    protected static final int DRAG_LOCATION_OFFSET = 18;
    protected static final int IS_DRAG_OFFSET = 20;
    protected static final int RAW_TOUCH_OFFSET = -999;// Initalize offset
//    protected int mEncryptionType = Zip.NO_ENCRYPTION;
    // True if a drag is currently in progress
    protected boolean mDragInProgress = false;
    // The adapter position that the user's finger is hovering over
    protected int mDropTargetAdapterPosition = NO_DROP_TARGET;
    // The target view the user's finger is hovering over
    protected IconifiedText mDropTargetView;
    protected String mDropTargetPath;
    protected int mDragItemHeight = -1;
    protected int prevrawTouchY = -1;
    protected boolean mTargetScrolling;
    final static public int MESSAGE_SELECT_CONTEXT_MENU = 520;
    final static public int MESSAGE_SELECT_DRAG_LOCATION = 522;
    protected BaseFileManagerFragment mCurrIn;

    protected ActionMode mCurrentActionMode = null;
    protected ActionModeHandler mActionModeHandler = null;
    protected FileManagerApp mFileManagerApp = null;
    protected MainFileManagerActivity mMainActivity = null;

    public BaseFileManagerFragment() {

    }

    public BaseFileManagerFragment(int fragmentLayoutID) {
        mfragmentLayoutID = fragmentLayoutID;
        mCurrIn = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        mContext = getActivity();
        mMainActivity = (MainFileManagerActivity) mContext;
        mFileManagerApp = (FileManagerApp) mMainActivity.getApplication();
        mContentView = inflater.inflate(mfragmentLayoutID, container, false);
        onCreateViewInit();
        return mContentView;
    }

    /*
     * To be overridden by sub-Class if needed.
     */
    protected void onCreateViewInit() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mCurrentHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                BaseFileManagerFragment.this.handleMessage(msg);
            }
        };

    }

    protected void handleMessage(Message message) {
        switch (message.what) {

        }
    }

    public void switchView(int mode, boolean rebrowse) {

    }

    public void handleSelectMultiple() {

    }

    public void handleSelectAll() {

    }

    public void handleUnSelectAll() {

    }

    public void setActionMode(ActionMode currentActionMode) {
        mCurrentActionMode = currentActionMode;
    }

    public boolean upOneLevel() {
        return false;
    }

    public void updateRightFragment() {

    }

    public void updateShortcuts() {

    }

    public void updateRecentFiles() {

    }

    public void exitMultiSelect() {

    }

    public void updateHighlightedItem(String curPath) {

    }

    public void updateLeftFragment(File clickedFile) {

    }

    public void onNewIntent(Intent intent) {

    }

    public void handleDelete(boolean isContext) {

    }

    public void setUpViewSorting(final int sortMode) {

    }

    public String getFileName(boolean isContext) {
        return null;
    }

    public void handlePaste(ArrayList<String> sourceFile, String destDir) {

    }

    public void openClickedFile(String sPath, IconifiedText text) {
        // TODO Auto-generated method stub

    }

    public void clearActionMode() {
        // TODO Auto-generated method stub

    }

    public void refreshLeft(boolean isFullrefresh) {
        // TODO Auto-generated method stub

    }

    public void setDragCol(BaseFileManagerFragment dragCol) {

    }

    public void refreshList(final File aDirectory) {

    }

    public void handleCopy(boolean isContextMenu) {

    }

    public void handleMove(boolean isContextMenu) {

    }

    public String getZipFileName(boolean isContext) {
        return null;
    }

    public void handlePrint(boolean isContextMenu) {

    }

    public IconifiedText getHighlightedItem() {
        return null;
    }

 /*   public int getEncryptionType() {
        return mEncryptionType;
    }

    public void setEncryptionType(int type) {
        mEncryptionType = type;
    }
*/
    public void addShortcut(File aFile) {

    }

    protected void createBreadCrumbPath(String currDirPath) {
        String child_path = null;
        int position = 0;
        try {
            if (currDirPath != null) {
                if (currDirPath.endsWith("/")) {
                    currDirPath = currDirPath.substring(0, (currDirPath.length() - 1));
                }
                if (currDirPath.indexOf(mFileManagerApp.SD_CARD_EXT_DIR) == 0) {
                    position = mFileManagerApp.SD_CARD_EXT_DIR.length();
                    child_path = currDirPath.substring(position);
                    if (child_path != null && child_path.length() > 0) {
                        child_path = child_path.replaceAll("/", DELIM);
                        mDisplayText = getString(R.string.sd_card) + child_path;
                    } else {
                        mDisplayText = getString(R.string.sd_card);
                    }
                } else if (currDirPath.indexOf(mFileManagerApp.SD_CARD_DIR) == 0) {
                    position = mFileManagerApp.SD_CARD_DIR.length();
                    child_path = currDirPath.substring(position);

                    if (child_path != null && child_path.length() > 0) {
                        child_path = child_path.replaceAll("/", DELIM);
                        mDisplayText = getString(R.string.internal_device_storage) + child_path;
                    } else {
                        mDisplayText = getString(R.string.internal_device_storage);
                    }
                } else if (currDirPath
                        .indexOf(mFileManagerApp.mUsbDiskListPath[FileManagerApp.USB_DISK_LIST_INDEX_1]) == 0) {
                    ConvertUSBDirectoryPath(mFileManagerApp.mUsbDiskListPath[FileManagerApp.USB_DISK_LIST_INDEX_1],
                            FileManagerApp.USB_DISK_LIST_INDEX_1,
                            currDirPath);
                } else if (currDirPath
                        .indexOf(mFileManagerApp.mUsbDiskListPath[FileManagerApp.USB_DISK_LIST_INDEX_2]) == 0) {
                    ConvertUSBDirectoryPath(mFileManagerApp.mUsbDiskListPath[FileManagerApp.USB_DISK_LIST_INDEX_2],
                            FileManagerApp.USB_DISK_LIST_INDEX_2,
                            currDirPath);
                } else if (currDirPath
                        .indexOf(mFileManagerApp.mUsbDiskListPath[FileManagerApp.USB_DISK_LIST_INDEX_3]) == 0) {
                    ConvertUSBDirectoryPath(mFileManagerApp.mUsbDiskListPath[FileManagerApp.USB_DISK_LIST_INDEX_3],
                            FileManagerApp.USB_DISK_LIST_INDEX_3,
                            currDirPath);
                } else if (currDirPath
                        .indexOf(mFileManagerApp.mUsbDiskListPath[FileManagerApp.USB_DISK_LIST_INDEX_4]) == 0) {
                    ConvertUSBDirectoryPath(mFileManagerApp.mUsbDiskListPath[FileManagerApp.USB_DISK_LIST_INDEX_4],
                            FileManagerApp.USB_DISK_LIST_INDEX_4,
                            currDirPath);
                } else {
                    mDisplayText = getString(R.string.internal_device_storage);
                }
            } else {
                // set to show default folder if errors occurred above
                mDisplayText = getString(R.string.internal_device_storage);
            }
            FileManagerApp.log(TAG + "mDisplayText=" + mDisplayText, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ConvertUSBDirectoryPath(String dirPath, int index, String currDirPath) {
        int childPosition = 0;
        String childPath = "";
        if (dirPath != null && 0 <= index && index < FileManagerApp.MAX_USB_DISK_NUM) {
            childPosition = dirPath.length();
            try {
                childPath = currDirPath.substring(childPosition + 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (childPath != null) {
                childPath = childPath.replaceAll("/", DELIM);
                mDisplayText =
                        getString(R.string.usb_storage) + " " + (index + 1) + DELIM + childPath;
            } else {
                mDisplayText = getString(R.string.usb_storage) + " " + (index + 1);
            }
        } else {
            mDisplayText = getString(R.string.usb_storage);
        }
    }

    // Add Drag & Drop handling to base since ColumnViewFrame, Homepage left and Local operations each needs
    // handle differently

    @Override
    public boolean onDrag(View view, DragEvent event) {
        boolean result = false;
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED :
                result = onDragStarted(event);
                break;
            case DragEvent.ACTION_DRAG_ENTERED :
                // The drag has entered the ListView window
                FileManagerApp.log(TAG + "========== DRAG ENTERED (target = " +
                        mDropTargetAdapterPosition + ")", false);

                break;
            case DragEvent.ACTION_DRAG_EXITED :
                // The drag has left the building

                FileManagerApp.log(TAG + "========== DRAG EXITED (target = " +
                        mDropTargetAdapterPosition + ")", false);
                onDragExited();
                break;
            case DragEvent.ACTION_DRAG_ENDED :
                // The drag is over

                FileManagerApp.log(TAG + "========== DRAG ENDED", false);

                onDragEnded();
                break;
            case DragEvent.ACTION_DRAG_LOCATION :
                // We're moving around within our window; handle scroll, if necessary
                onDragLocation(event);
                break;
            case DragEvent.ACTION_DROP :
                // The drag item was dropped

                FileManagerApp.log(TAG + "========== DROP", false);

                result = onDrop(event);
                break;
            default :
                break;
        }
        return result;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Save the touch location to draw the drag overlay at the correct location
            mShadow.setTouchX((int) event.getX());
        }
        // don't do anything, let the system process the event
        return false;
    }

    // All functions are overriden by child calls depending on what they want to do for Drag and Drop

    /**
     * Called when  Target View gets a DRAG_STARTED event
     */

    protected boolean onDragStarted(DragEvent event) {
        ((MainFileManagerActivity) getActivity()).setIsDropSuccess(false);
        return true;
    }

    /**
     * Called when our Target View gets a DRAG_EXITED event
     */

    protected void onDragExited() {

    }

    /**
     * Called while dragging;  highlight possible drop targets, and autoscroll the list.
     */
    protected void onDragLocation(DragEvent event) {
        // The drag is somewhere in the ListView
        if (mDragItemHeight <= 0) {
            //check if MainActivity has got the event val
            mDragItemHeight = ((MainFileManagerActivity) getActivity()).mDragItemHeight;
            // if still <=0 return to avoid NPE
            if (mDragItemHeight <= 0) {
                // This shouldn't be possible, but avoid NPE
                return;
            }
        }
    }

    protected void onDragEnded() {

    }

    /** Called when dropped in the target **/
    protected boolean onDrop(DragEvent event) {
        return false;
    }

    public boolean handleDrop(DragEvent event) {
        return false;
    }

    protected void openFile(File aFile) {
        try {
            if (!aFile.exists()) {
                Toast.makeText(mContext, R.string.error_file_does_not_exists, Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FILE_MODE) {
                ((MainFileManagerActivity) getActivity()).rtnPickerResult(aFile.getAbsolutePath(),
                        null);
                return;
            } else if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_FOLDER_MODE) {
                return;
            }

            Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
            Uri data = FileUtils.getUri(aFile);
            String type = MimeTypeUtil.getMimeType(mContext, aFile.getName());

            // try to get media file content uri
            if (type != null) {
                if (type.indexOf("audio") == 0) {
                    long id =
                            FileUtils.getMediaContentIdByFilePath(mContext,
                                    aFile.getAbsolutePath(),
                                    1);
                    if (id > 0) {
                        data =
                                ContentUris
                                        .withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                                id);
                        FileManagerApp.log(TAG + "openFile: content uri: " + data + " " + type,
                                false);
                    }
                } else if (type.indexOf("image") == 0) {
                    long id =
                            FileUtils.getMediaContentIdByFilePath(mContext,
                                    aFile.getAbsolutePath(),
                                    0);
                    if (id > 0) {
                        data =
                                ContentUris
                                        .withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                id);
                        FileManagerApp.log(TAG + "openFile: content uri: " + data + " " + type,
                                false);
                    }
                }
            }
            intent.setDataAndType(data, type);

            // Were we in GET_CONTENT mode?
            if (mFileManagerApp.mLaunchMode == FileManagerApp.SELECT_GET_CONTENT) {
                FileManagerApp.log(TAG + "FileManagerApp.mLaunchMode is SELECT_GET_CONTENT", false);

                Intent sIntent = mFileManagerApp.getContentIntent();
                Bundle myExtras = sIntent.getExtras();
                // special case for handle contact pick picture from files
                if (myExtras != null &&
                        (myExtras.getParcelable("data") != null || myExtras
                                .getBoolean("return-data"))) {
                    Bitmap croppedImage =
                            FileUtils.cropImageFromImageFile(aFile.getPath(), myExtras);
                    if (croppedImage != null) {
                        Bundle extras = new Bundle();
                        extras.putParcelable("data", croppedImage);
                        Intent contactIntent = (new Intent()).setAction("inline-data");
                        contactIntent.putExtras(extras);
                        mMainActivity.rtnPickerResult(contactIntent);
                    } else {
                        Toast.makeText(mContext, R.string.crop_Image_error, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                mMainActivity.rtnPickerResult(intent);
                return;
            }
            // IKMAIN-10729,blm013 Move Zip handler to after the SELECT_GET_CONTENT
            // Mode for attachement use case.
            else if (FileUtils.isZipFile(aFile.getName())) {
                //Don't handle this in base since home left does not need it handle it in child classes
                return;
            }
            try {
                if (!FileUtils.isZipFile(aFile.getName()) && mFileManagerApp.addRecentFile(aFile)) {
                    BaseFileManagerFragment leftfrag =
                            (BaseFileManagerFragment) getFragmentManager()
                                    .findFragmentById(R.id.home_page);
                    if (leftfrag != null) {
                        leftfrag.updateRecentFiles();

                    }
                }
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(mContext, R.string.application_not_available, Toast.LENGTH_SHORT)
                        .show();
            };
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void dragUnderScroll(AbsListView listView, int rawTouchY) {
        int scrollDiff = rawTouchY - (listView.getHeight() - SCROLL_ZONE_SIZE);
        boolean scrollDown = (scrollDiff > 0);
        boolean scrollUp = (SCROLL_ZONE_SIZE > rawTouchY);
        int firstVisibleItem = listView.getFirstVisiblePosition();
        int lastVisibleItem = listView.getLastVisiblePosition();
        if (!mTargetScrolling && scrollDown) {
            int pixelsToScroll = (lastVisibleItem + 1) * mDragItemHeight;
            listView.smoothScrollBy(pixelsToScroll, pixelsToScroll * SCROLL_SPEED);
            FileManagerApp.log(TAG + "START TARGET SCROLLING DOWN", false);

            mTargetScrolling = true;
        } else if (!mTargetScrolling && scrollUp) {
            int pixelsToScroll = (firstVisibleItem + 1) * mDragItemHeight;
            listView.smoothScrollBy(-pixelsToScroll, pixelsToScroll * SCROLL_SPEED);

            FileManagerApp.log(TAG + "START TARGET SCROLLING UP", false);

            mTargetScrolling = true;
        } else if (!scrollUp && !scrollDown) {
            stopScrolling(listView);
        }
    }

    /**
     * Indicate that scrolling has stopped
     */
    protected void stopScrolling(AbsListView listView) {
        if (mTargetScrolling) {
            mTargetScrolling = false;
            FileManagerApp.log(TAG + "========== STOP TARGET SCROLLING", false);
            // Stop the scrolling
            listView.smoothScrollBy(0, 0);
        }
    }

    /** Check if user is still dragging the drag shadow **/
    protected boolean checkisDrag(int rawTouchY) {
        boolean isDrag = true;
        if (prevrawTouchY != RAW_TOUCH_OFFSET) {
            int offset = Math.abs(prevrawTouchY - rawTouchY);
            if (offset <= IS_DRAG_OFFSET) { // Check if drag shadow is still dragging around or on a fixed view
                isDrag = false;
            }
        }
        prevrawTouchY = rawTouchY;
        return isDrag;
    }

    /** Check if user is still dragging the drag shadow **/
    protected void sendHovertoTarget(IconifiedText newActTarget, int absChildPos) {
        newActTarget.enterOnDropLocation();//Increment the time the shadow was on this item
        if (newActTarget.getDropLocVal() > DRAG_LOCATION_OFFSET) {
            Message msg = Message.obtain();
            msg.what = MESSAGE_SELECT_DRAG_LOCATION;
            msg.obj = newActTarget;
            msg.arg1 = mDropTargetAdapterPosition - 1;
            msg.arg2 = absChildPos;
            mCurrentHandler.sendMessage(msg);
            newActTarget.resetDropLocVal();
        }
    }

    protected boolean isTargetDestDragging() {
        ArrayList<String> pasteFiles = FileManagerApp.getPasteFiles();
        if (pasteFiles != null && mDropTargetPath != null) {
            return pasteFiles.contains(mDropTargetPath);
        }
        return false;
    }

    protected boolean isSourceandDestSame() {
        try {
            ArrayList<String> pasteFiles = FileManagerApp.getPasteFiles();
            File pasteFile = new File(pasteFiles.get(0));
            File targetFile = new File(mDropTargetPath);

            if (pasteFile.getParent().equals(targetFile.getParent())) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void blankScreen() {

    }

    public void showScreen() {

    }

    public ActionMode getActionMode() {
        return mCurrentActionMode;
    }

}
