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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;

import com.motorola.filemanager.BaseFileManagerFragment;
import com.motorola.filemanager.FileManagerApp;
import com.motorola.filemanager.MainFileManagerActivity;
import com.motorola.filemanager.R;

public class IconifiedTextListAdapter extends BaseAdapter implements Filterable {
    /** Remember our context so we can use it when constructing views. */
    private Context mContext;
    private static String lastFilter;

    /** Set to Transparent (invalid for text), so we can check if the color is retrieved successfull. */
    private int mPrimaryTextColor = Color.TRANSPARENT;
    private int mSecondaryTextColor = Color.TRANSPARENT;

    class IconifiedFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence arg0) {

            lastFilter = (arg0 != null) ? arg0.toString() : null;

            Filter.FilterResults results = new Filter.FilterResults();

            // No results yet?
            if (mOriginalItems == null) {
                results.count = 0;
                results.values = null;
                return results;
            }

            int count = mOriginalItems.size();

            if (arg0 == null || arg0.length() == 0) {
                results.count = count;
                results.values = mOriginalItems;
                return results;
            }

            List<IconifiedText> filteredItems = new ArrayList<IconifiedText>(count);

            int outCount = 0;
            CharSequence lowerCs = arg0.toString().toLowerCase();

            for (int x = 0; x < count; x++) {
                IconifiedText text = mOriginalItems.get(x);

                if (text.getText().toLowerCase().contains(lowerCs)) {
                    // This one matches.
                    filteredItems.add(text);
                    outCount++;
                }
            }

            results.count = outCount;
            results.values = filteredItems;
            return results;
        }

        @Override
        protected void publishResults(CharSequence arg0, FilterResults arg1) {
            mItems = (List<IconifiedText>) arg1.values;
            notifyDataSetChanged();
        }

        List<IconifiedText> synchronousFilter(CharSequence filter) {
            FilterResults results = performFiltering(filter);
            return (List<IconifiedText>) (results.values);
        }
    }

    private IconifiedFilter mFilter = new IconifiedFilter();
    private Handler mHandler;

    private List<IconifiedText> mItems = new ArrayList<IconifiedText>();
    private List<IconifiedText> mOriginalItems = new ArrayList<IconifiedText>();

    public IconifiedTextListAdapter(Context context) {
        mContext = context;
        initHighlightTextColor();
    }

    public IconifiedTextListAdapter(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
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

    public void addItem(IconifiedText it) {
        mItems.add(it);
    }

    public void setListItems(List<IconifiedText> lit, boolean filter) {
        mOriginalItems = lit;

        if (filter) {
            mItems = mFilter.synchronousFilter(lastFilter);
            //FileManagerApp.log("Aki Filter" + mItems.size());
        } else {
            mItems = lit;
            //FileManagerApp.log("Aki NFilter" + mItems.size());
        }
    }

    /** @return The number of items in the */
    public int getCount() {
        return mItems.size();
    }

    public Object getItem(int position) {
        if (position >= getCount()) {
            return null;
        } else {
            return mItems.get(position);
        }
    }

    public boolean areAllItemsSelectable() {
        return false;
    }

    /** Use the array index as a unique id. */
    public long getItemId(int position) {
        return position;
    }

    protected void setBackground(IconifiedTextView btv, IconifiedText icText, boolean titleFrag) {
        if (!titleFrag) {
            if (((FileManagerApp) mContext.getApplicationContext()).getViewMode() == FileManagerApp.COLUMNVIEW) {
                if (icText.isHighlighted()) {
                    setHighlightTextColor(btv);
                    btv.setBackgroundColor(mContext.getResources()
                            .getColor(R.color.selected_item_highlight_color));
                } else {
                    if (icText.isChecked()) {
                        restoreTextColor(btv);
                        btv.setBackgroundColor(mContext.getResources()
                                .getColor(R.color.multiselect_highlight_color));
                    } else {
                        restoreTextColor(btv);
                        btv.setBackgroundColor(Color.TRANSPARENT);
                    }
                }
            } else {
                if (icText.isChecked()) {
                    btv.setBackgroundColor(mContext.getResources()
                            .getColor(R.color.multiselect_highlight_color));

                } else {
                    restoreTextColor(btv);
                    btv.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        } else {
            if (icText.isHighlighted()) {
                setHighlightTextColor(btv);
                btv.setBackgroundColor(mContext.getResources()
                        .getColor(R.color.selected_item_highlight_color));
            } else {
                if (icText.isChecked() && ((MainFileManagerActivity) mContext).getIsDragMode()) {
                    restoreTextColor(btv);
                    btv.setBackgroundColor(mContext.getResources()
                            .getColor(R.color.multiselect_highlight_color));
                } else {
                    restoreTextColor(btv);
                    btv.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        IconifiedTextView btv = null;
        IconifiedTextView.TEXTVIEW_TYPE tvType = IconifiedTextView.TEXTVIEW_TYPE.NON_TITLE;
        parent.getId();
        if (parent.getId() == R.id.file_list_t || (parent.getId() == R.id.SmbGroupList) ||
                (parent.getId() == R.id.SavedServerList) ||
                (parent.getId() == R.id.SambaServerPref) ||
                (parent.getId() == R.id.SmbFileList_l) || (parent.getId() == R.id.SmbFileGrid_l)) {
            tvType = IconifiedTextView.TEXTVIEW_TYPE.TITLE_ONLY;
        } else {
            tvType = IconifiedTextView.TEXTVIEW_TYPE.NON_TITLE;
        }
        if (convertView == null) {
            if (position >= mItems.size()) {
                return btv;
            }
            btv = new IconifiedTextView(mContext, mItems.get(position), tvType, mHandler);
        } else {
            if (convertView instanceof IconifiedTextView) {
                btv = (IconifiedTextView) convertView;
            }
            if (position >= mItems.size()) {
                return btv;
            }
            if (parent.getId() != R.id.file_grid) { // Temp solution for FC issue
                btv.setConfig(mContext.getResources().getConfiguration().orientation);
            }

        }
        IconifiedText icText = mItems.get(position);
        btv.setPosition(position);
        btv.setText(icText.getText());
        btv.setIcon(icText.getIcon());
        btv.setInfo(icText.getInfo(), icText.isDirectory());
        btv.setType(icText.getTypeDesc(mContext), icText.isDirectory());
        btv.setTime(icText.getTimeInfo(), icText.getDayInfo());

        final int pos = position;
        RelativeLayout contextMenu =
                (RelativeLayout) btv.findViewById(R.id.filebrowser_contextMenuContainer);
        if (contextMenu != null) {
            contextMenu.setOnClickListener(new View.OnClickListener() {
                //@Override
                public void onClick(View v) {
                    Message msg = Message.obtain();
                    msg.what = BaseFileManagerFragment.MESSAGE_SELECT_CONTEXT_MENU;
                    msg.obj = v;
                    msg.arg1 = pos;
                    mHandler.sendMessage(msg);
                }
            });
            if (!icText.isTopColumn() ||
                    isSelectMode() ||
                    ((FileManagerApp) ((MainFileManagerActivity) mContext).getApplication()).mLaunchMode != FileManagerApp.NORMAL_MODE) {
                contextMenu.setVisibility(View.INVISIBLE);
            } else {
                contextMenu.setVisibility(View.VISIBLE);
            }
        }
        if (tvType == IconifiedTextView.TEXTVIEW_TYPE.NON_TITLE) {
            setBackground(btv, icText, false);
        } else {
            setBackground(btv, icText, true);
        }

        if (((FileManagerApp) ((MainFileManagerActivity) mContext).getApplication()).mLaunchMode == FileManagerApp.SELECT_FOLDER_MODE) {
            Button setlectBtn = (Button) btv.findViewById(R.id.file_select);
            if (mItems.get(position).getMiMeType().equals(IconifiedText.DIRECTORYMIMETYPE)) {
                setlectBtn.setVisibility(View.VISIBLE);
                setlectBtn.setTag(new NameAuth(mItems.get(position).getPathInfo(), mItems
                        .get(position).getAuthInfo()));
                setlectBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        ((MainFileManagerActivity) mContext)
                                .rtnPickerResult(((NameAuth) v.getTag()).mPathInfo, ((NameAuth) v
                                        .getTag()).mAuthInfo);
                        return;
                    }
                });
            } else {
                setlectBtn.setVisibility(View.GONE);
            }
        }
        FileManagerApp.log("Aki text" + btv.getText(), false);
        return btv;
    }

    private void setHighlightTextColor(IconifiedTextView view) {
        // Apply to samll text only, as default color for small is textColorSecondary.
        if (mPrimaryTextColor != Color.TRANSPARENT) {
            view.setTimeInfoTextColor(mPrimaryTextColor);
            view.setInfoTextColor(mPrimaryTextColor);
        }
    }

    private void restoreTextColor(IconifiedTextView view) {
        // Apply to samll text only, as default color for small is textColorSecondary.
        if (mSecondaryTextColor != Color.TRANSPARENT) {
            view.setTimeInfoTextColor(mSecondaryTextColor);
            view.setInfoTextColor(mSecondaryTextColor);
        }
    }

    static class NameAuth {
        public String mPathInfo;
        public String mAuthInfo;

        public NameAuth(String pathInfo, String authInfo) {
            mPathInfo = pathInfo;
            mAuthInfo = authInfo;
        }
    }

    public static final int NONE_SELECT_MODE = 250;
    public static final int MULTI_SELECT_MODE = 251;

    private int mSelectMode = NONE_SELECT_MODE;

    public void setSelectMode(int mode) {
        mSelectMode = mode;
        notifyDataSetChanged();
    }

    public boolean isSelectMode() {
        boolean rtn_mode = false;

        if (mSelectMode == MULTI_SELECT_MODE) {
            rtn_mode = true;
        }

        return (rtn_mode);
    }

    public void selectAll() {
        for (IconifiedText item : mItems) {
            item.setChecked(true);
        }
        notifyDataSetChanged();
    }

    public void unSelectAll() {
        for (IconifiedText item : mItems) {
            item.setChecked(false);
        }
        ((Activity) mContext).runOnUiThread(new Runnable() {
            //@Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void setIsTopColumnMode(boolean isTopcolumn) {
        for (IconifiedText item : mItems) {
            item.setIsTopColumn(isTopcolumn);
        }
        ((Activity) mContext).runOnUiThread(new Runnable() {
            //@Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void setItemChecked(int position) {
        if (mItems.get(position).isChecked()) {
            mItems.get(position).setChecked(false);
        } else {
            mItems.get(position).setChecked(true);
        }
        ((Activity) mContext).runOnUiThread(new Runnable() {
            //@Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public int isSingleFileSelected() {
        int count = 0;
        int selectPos = -1;
        for (IconifiedText item : mItems) {
            if (item.isChecked()) {
                count++;
                if (count > 1) {
                    selectPos = -1;
                    return selectPos;
                }
            }
        }
        if (count == 0) {
            selectPos = -1;
            return selectPos;
        }
        selectPos = count - 1;
        return selectPos;
    }

    public boolean isSomeFileSelected() {
        int count = 0;
        for (IconifiedText item : mItems) {
            if (item.isChecked()) {
                count++;
                if (count > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public ArrayList<String> getSelectFiles() {
        ArrayList<String> result = new ArrayList<String>();
        for (IconifiedText item : mItems) {
            if (item.isChecked()) {
                result.add(item.getPathInfo());
            }
        }
        return result;
    }

    public List<IconifiedText> getItems() {
        return mItems;
    }

    public Filter getFilter() {
        return mFilter;
    }

    public int getPosition(IconifiedText sitem) {
        int position = -1;
        for (IconifiedText item : mItems) {
            position++;
            if (item.equals(sitem)) {
                return position;
            }
        }
        return position;
    }

    public IconifiedText updateHighlightedItem(String selectedItemPath, boolean highLightOnly) {
        if (selectedItemPath != null) {
            if (!selectedItemPath.endsWith("/")) {
                selectedItemPath = selectedItemPath.concat("/");
            }
            FileManagerApp
                    .log("IconifiedTextListAdapter: updateHighlightedItem() selectedItemPath = " +
                            selectedItemPath, false);
            for (IconifiedText item : mItems) {
                String pathInfo = item.getPathInfo();
                if (!pathInfo.endsWith("/")) {
                    pathInfo = pathInfo.concat("/");
                }
                if (selectedItemPath.equals(pathInfo)) {
                    setHighlightedItem(item, highLightOnly);
                    return item;
                }
            }
        }
        return null;
    }

    public void clearHighlightedItem() {
        for (IconifiedText item : mItems) {
            if (item.isHighlighted()) {
                item.setHighlighted(false);
                /* Also clear Checked flag */
                item.setChecked(false);
            }
        }

    }

    public void setHighlightedItem(IconifiedText item1, boolean highLightOnly) { //Triggered only by Left Fragment and All columns of Column view when needed
        for (IconifiedText item : mItems) {
            if (item != null && item1 != null) {
                if (item.getPathInfo().equals(item1.getPathInfo())) {

                    item.setHighlighted(true);
                    /* to be applied for actions(move/copy/rename/delete)
                     * without being clicked
                     */
                    item.setChecked(true);
                    if (!highLightOnly) {
                        item.setOpenFolderIcon(mContext);
                    }
                    if (mContext != null) {
                        ((Activity) mContext).runOnUiThread(new Runnable() {
                            //@Override
                            public void run() {
                                notifyDataSetChanged();
                            }
                        });
                    }
                }
            }
        }
    }

    public void clearCurrentHighlightedItem(IconifiedText item) {//Triggered only by Left Fragment and All columns of Column view when needed
        if ((mContext != null) && (item != null)) {
            item.setHighlighted(false);
            item.setChecked(false);
            item.clearOpenFolderIcon(mContext);
            ((Activity) mContext).runOnUiThread(new Runnable() {
                //@Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }

    public void setNoItemsInfo(File currDir) {
        String dir = "";
        if (currDir == null) {
            currDir = FileManagerApp.getCurrDir();
        }

        int size = 0;
        IconifiedText item = null;
        String info = "";
        if (currDir != null) {
            dir = currDir.toString() + "/";
            for (IconifiedText item1 : mItems) {
                if (item1.getPathInfo().equalsIgnoreCase(dir)) {
                    item = item1;
                    break;
                }
            }
            if (item != null && mContext != null) {
                if (currDir.listFiles() == null) {
                    size = 0;
                } else {
                    size = currDir.listFiles().length;
                }
                info = mContext.getResources().getQuantityString(R.plurals.item, size, size);

                if ((mContext != null) && (item != null)) {
                    item.setInfo(info);
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        //@Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }
            }
        }
    }

    public IconifiedText setNewName(String oldName, String newName, File currDir) {
        IconifiedText item = null;
        if (currDir != null) {
            for (IconifiedText item1 : mItems) {
                if (item1.getText().equals(oldName)) {
                    item1.setText(newName);
                    String newPath = currDir.getAbsolutePath() + "/" + newName;
                    item1.setPathInfo(newPath);
                    item = item1;
                }
            }

            if ((mContext != null) && (item != null)) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    //@Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }
        return item;
    }

}
