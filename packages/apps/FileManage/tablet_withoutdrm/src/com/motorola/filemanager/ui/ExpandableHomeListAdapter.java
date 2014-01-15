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
package com.motorola.filemanager.ui;

import java.text.DecimalFormat;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;

public class ExpandableHomeListAdapter extends BaseExpandableListAdapter {
    /** Remember our context so we can use it when constructing views. */
    public static final int GROUP_DEVICE_POS = 0;
    public static final int GROUP_SHORTCUT_POS = 1;
    public static final int GROUP_RECENTFILE_POS = 2;
    public static final float BYTE_IN_GB = 1073741824f;
    public static final String MEMSIZE_FORMAT = "#0.00";
    public static final float INVALID_MEMORY_SIZE = -1f;
    /** Set to Transparent (invalid for text), so we can check if the color is retrieved successfull. */
    private int mPrimaryTextColor = Color.TRANSPARENT;
    private int mSecondaryTextColor = Color.TRANSPARENT;

    private Context mContext;
    private Handler mHandler;
    private FileManagerApp mFileManagerApp;
    private List<IconifiedText> mGroups = null;
    private List<IconifiedText> mDeviceChildren = null;
    private List<IconifiedText> mShortcutChildren = null;
    private List<IconifiedText> mRecentFileChildren = null;

    public ExpandableHomeListAdapter(Context context, List<IconifiedText> groupItems,
                                     Handler handler) {
        mContext = context;
        mGroups = groupItems;
        mHandler = handler;
        mFileManagerApp = ((FileManagerApp) ((MainFileManagerActivity) mContext).getApplication());
        initHighlightTextColor();
    }

    private void initHighlightTextColor() {
        TypedValue tv = new TypedValue();
        try {
            if ((mContext != null) && (tv != null)) {
                mContext.getTheme().resolveAttribute(android.R.attr.textColorPrimary, tv, true);
                mPrimaryTextColor = mContext.getResources().getColor(tv.resourceId);

                mContext.getTheme().resolveAttribute(android.R.attr.textColorSecondary, tv, true);
                mSecondaryTextColor = mContext.getResources().getColor(tv.resourceId);
            }
        } catch (Resources.NotFoundException e) {
            // Cannot find the color attr. Set the color to invalid.
            mPrimaryTextColor = Color.TRANSPARENT;
            mSecondaryTextColor = Color.TRANSPARENT;
        }
    }

    @Override
    public IconifiedText getChild(int groupPosition, int childPosition) {
        switch (groupPosition) {
            case GROUP_RECENTFILE_POS :
                if ((mRecentFileChildren != null) && (childPosition < mRecentFileChildren.size())) {
                    return mRecentFileChildren.get(childPosition);
                }
            case GROUP_SHORTCUT_POS :
                if ((mShortcutChildren != null) && (childPosition < mShortcutChildren.size())) {
                    return mShortcutChildren.get(childPosition);
                }

            case GROUP_DEVICE_POS :
                if ((mDeviceChildren != null) && (childPosition < mDeviceChildren.size())) {
                    return mDeviceChildren.get(childPosition);
                }

        }
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    private void setHighlightTextColor(IconifiedTextView view) {
        // Apply to small text only, as default color for small is textColorSecondary.
        if (mPrimaryTextColor != Color.TRANSPARENT) {
            view.setTimeInfoTextColor(mPrimaryTextColor);
            view.setInfoTextColor(mPrimaryTextColor);
        }
    }

    private void restoreTextColor(IconifiedTextView view) {
        // Apply to small text only, as default color for small is textColorSecondary.
        if (mSecondaryTextColor != Color.TRANSPARENT) {
            view.setTimeInfoTextColor(mSecondaryTextColor);
            view.setInfoTextColor(mSecondaryTextColor);
        }
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {

        IconifiedTextView btv = null;
        IconifiedText aIconifiedText = null;
        if (groupPosition > GROUP_RECENTFILE_POS) {
            return btv;
        }

        if (groupPosition == GROUP_DEVICE_POS) {
            if (mDeviceChildren != null) {
                aIconifiedText = mDeviceChildren.get(childPosition);
            }
        } else if (groupPosition == GROUP_SHORTCUT_POS) {
            if (mShortcutChildren != null) {
                aIconifiedText = mShortcutChildren.get(childPosition);
            }
        } else if (groupPosition == GROUP_RECENTFILE_POS) {
            if (mRecentFileChildren != null) {
                aIconifiedText = mRecentFileChildren.get(childPosition);
            }
        }

        if (aIconifiedText == null) {
            return btv;
        }

        if (convertView == null) {
            if (groupPosition == GROUP_SHORTCUT_POS) {
                btv =
                        new IconifiedTextView(mContext, aIconifiedText,
                                IconifiedTextView.TEXTVIEW_TYPE.TITLE_WITH_CONTEXT, mHandler);
            } else {
                btv =
                        new IconifiedTextView(mContext, aIconifiedText,
                                IconifiedTextView.TEXTVIEW_TYPE.TITLE_ONLY, mHandler);
            }

        } else {
            if (convertView instanceof IconifiedTextView) {
                btv = (IconifiedTextView) convertView;
                ImageView tConMenuIcon = (ImageView) btv.findViewById(R.id.title_contextMenu);
                if (tConMenuIcon != null) {
                    tConMenuIcon.setVisibility(View.GONE);
                    if (groupPosition == GROUP_SHORTCUT_POS) {
                        tConMenuIcon.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        if (btv != null) {
            btv.setPosition(childPosition);
            String text = aIconifiedText.getText();
            btv.setText(text);
            btv.setIcon(aIconifiedText.getIcon());
            if (aIconifiedText.isHighlighted() == true) {
                setHighlightTextColor(btv);
                btv.setBackgroundColor(mContext.getResources().getColor(
                        R.color.selected_item_highlight_color));
            } else {
                restoreTextColor(btv);
                btv.setBackgroundColor(Color.TRANSPARENT);
            }
            if (groupPosition == GROUP_DEVICE_POS) {
                float availMem = getDeviceMemoryInfo(aIconifiedText);
                if (availMem != INVALID_MEMORY_SIZE) {
                    DecimalFormat df = new DecimalFormat(MEMSIZE_FORMAT);
                    if (mContext != null) {
                        btv.setInfo(
                                mContext.getResources().getString(R.string.availmem_gb,
                                        df.format(availMem)), false);
                    }
                }
            } else if ((groupPosition == GROUP_SHORTCUT_POS) ||
                    (groupPosition == GROUP_RECENTFILE_POS)) {
                btv.setInfo(aIconifiedText.getInfo(), false);

                if (groupPosition == GROUP_SHORTCUT_POS) {
                    final int pos = childPosition;
                    ImageView contextMenu = (ImageView) btv.findViewById(R.id.title_contextMenu);
                    if (contextMenu != null) {
                        contextMenu.setOnClickListener(new View.OnClickListener() {
                            //@Override
                            @Override
                            public void onClick(View v) {
                                Message msg = Message.obtain();
                                msg.what = BaseFileManagerFragment.MESSAGE_SELECT_CONTEXT_MENU;
                                msg.obj = v;
                                msg.arg1 = pos;
                                mHandler.sendMessage(msg);
                            }
                        });
                        if (((FileManagerApp) ((MainFileManagerActivity) mContext).getApplication()).mLaunchMode != FileManagerApp.NORMAL_MODE) {
                            contextMenu.setVisibility(View.INVISIBLE);
                        } else {
                            contextMenu.setVisibility(View.VISIBLE);
                        }
                    }
                }
            } else {
                btv.setInfo(null, false);
            }
        }
        return btv;

    }

    public void setGroupItems(List<IconifiedText> items) {
        if (mGroups != null) {
            mGroups.clear();
        }
        mGroups = items;
    }

    @Override
    public IconifiedText getGroup(int groupPosition) {
        return mGroups.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return mGroups.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
                             ViewGroup parent) {

        IconifiedTextView btv = null;

        if (convertView == null) {
            if (groupPosition > GROUP_RECENTFILE_POS) {
                return btv;
            }
            btv =
                    new IconifiedTextView(mContext, mGroups.get(groupPosition),
                            IconifiedTextView.TEXTVIEW_TYPE.TITLE_ONLY, mHandler);
        } else {
            if (convertView instanceof IconifiedTextView) {
                btv = (IconifiedTextView) convertView;
            }
            if (groupPosition > GROUP_RECENTFILE_POS) {
                return btv;
            }
        }
        if ((mGroups != null) && (btv != null)) {
            IconifiedText icText = mGroups.get(groupPosition);
            btv.setPosition(groupPosition);
            if (((TextView) btv.findViewById(R.id.text)) != null) {
                ((TextView) btv.findViewById(R.id.text)).setTextAppearance(mContext,
                        android.R.style.TextAppearance_Large);
            }
            btv.setBackgroundResource(R.drawable.header_holo);
            if (icText != null) {
                btv.setText(icText.getText());
            }

        }

        return btv;

    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public void addChildItem(IconifiedText it, int groupPosition) {
        if ((groupPosition == GROUP_DEVICE_POS) && (mDeviceChildren != null)) {
            mDeviceChildren.add(it);
        }
        if ((groupPosition == GROUP_SHORTCUT_POS) && (mShortcutChildren != null)) {
            mShortcutChildren.add(it);
        }
        if ((groupPosition == GROUP_RECENTFILE_POS) && (mRecentFileChildren != null)) {
            mRecentFileChildren.add(it);
        }
    }

    public void setChildItems(List<IconifiedText> lit, int groupPosition) {
        if (groupPosition == GROUP_DEVICE_POS) {
            if (lit != null) {
                if (mDeviceChildren != null) {
                    mDeviceChildren.clear();
                }
                mDeviceChildren = lit;
            }
        }
        if (groupPosition == GROUP_SHORTCUT_POS) {
            if (lit != null) {
                if (mShortcutChildren != null) {
                    mShortcutChildren.clear();
                }
                mShortcutChildren = lit;
            }
        }
        if (groupPosition == GROUP_RECENTFILE_POS) {
            if (lit != null) {
                if (mRecentFileChildren != null) {
                    mRecentFileChildren.clear();
                }
                mRecentFileChildren = lit;
            }
        }
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        switch (groupPosition) {
            case GROUP_RECENTFILE_POS :
                if (mRecentFileChildren != null) {
                    return mRecentFileChildren.size();
                }

            case GROUP_SHORTCUT_POS :
                if (mShortcutChildren != null) {
                    return mShortcutChildren.size();
                }

            case GROUP_DEVICE_POS :
                if (mDeviceChildren != null) {
                    return mDeviceChildren.size();
                }
        }
        return 0;
    }

    public void setHighlightedItem(IconifiedText item) {
        if (item != null) {
            item.setHighlighted(true);
            if (!FileManagerApp.isHomePage()) {
                // Home page does not have folder icons. Do not update it.
                item.setOpenFolderIcon(mContext);
            }

            if (mContext != null) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    //@Override
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }
    }

    public void clearCurrentHighlightedItem(IconifiedText item) {
        if ((mContext != null) && (item != null)) {

            item.setHighlighted(false);
            if (!FileManagerApp.isHomePage()) {
                // Home page does not have folder icons. Do not update it.
                item.clearOpenFolderIcon(mContext);
            }
            ((Activity) mContext).runOnUiThread(new Runnable() {
                //@Override
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public void updateMemoryInfo(IconifiedText item) {
        if (item != null) {
            float availMem = getDeviceMemoryInfo(item);
            if (availMem != INVALID_MEMORY_SIZE) {
                DecimalFormat df = new DecimalFormat(MEMSIZE_FORMAT);
                if (mContext != null) {
                    item.setInfo(mContext.getResources().getString(R.string.availmem_gb,
                            df.format(availMem)));
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        //@Override
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }
            }

        }
    }

    private float getDeviceMemoryInfo(IconifiedText aIconifiedText) {

        if (aIconifiedText == null) {
            return INVALID_MEMORY_SIZE;
        }

        StatFs stat = null;
        String text = aIconifiedText.getText();

        switch (aIconifiedText.getStorageMode()) {
            case FileManagerApp.INDEX_INTERNAL_MEMORY :
                stat =
                        new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());                           
                break;

            case FileManagerApp.INDEX_SD_CARD :
                stat =
                        new StatFs(
                                mFileManagerApp
                                        .getDeviceStorageVolumePath(FileManagerApp.INDEX_SD_CARD));
                break;

            case FileManagerApp.INDEX_USB :
                int indx = 0;
                if ((text != null) && (text.contains("" + FileManagerApp.USB_DISK_LIST_INDEX_1))) {
                    indx = FileManagerApp.USB_DISK_LIST_INDEX_1;
                } else if ((text != null) &&
                        (text.contains("" + FileManagerApp.USB_DISK_LIST_INDEX_2))) {
                    indx = FileManagerApp.USB_DISK_LIST_INDEX_2;
                } else if ((text != null) &&
                        (text.contains("" + FileManagerApp.USB_DISK_LIST_INDEX_3))) {
                    indx = FileManagerApp.USB_DISK_LIST_INDEX_3;
                } else if ((text != null) &&
                        (text.contains("" + FileManagerApp.USB_DISK_LIST_INDEX_4))) {
                    indx = FileManagerApp.USB_DISK_LIST_INDEX_4;
                }
                if (indx < FileManagerApp.MAX_USB_DISK_NUM) {
                    stat = new StatFs(mFileManagerApp.mUsbDiskListPath[indx]);
                }
                break;

            default :
                stat = null;
                break;
        }
        if (stat != null) {
            float blockSize = stat.getBlockSize();
            FileManagerApp.log("blocksize is" + blockSize, false);
            float availableBlocks = stat.getAvailableBlocks();
            FileManagerApp.log("avail block is" + availableBlocks, false);
            float availSize = (availableBlocks * blockSize) / BYTE_IN_GB;
            FileManagerApp.log("avail size is" + availSize, false);
            return availSize;
        }
        return INVALID_MEMORY_SIZE;
    }

}
