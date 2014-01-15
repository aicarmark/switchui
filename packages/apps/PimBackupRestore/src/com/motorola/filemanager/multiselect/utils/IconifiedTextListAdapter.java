/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 * Date       CR                Author      Description
 * 2010-03-23   IKSHADOW-2425   A20815      initial
 */

package com.motorola.filemanager.multiselect.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Handler;
import android.os.Message;
import com.motorola.sdcardbackuprestore.R;
//import com.zecter.api.parcelable.ZumoFile;

import com.motorola.filemanager.multiselect.FileManager;
import com.motorola.filemanager.multiselect.FileManagerApp;
import com.motorola.filemanager.multiselect.local.FileManagerActivity;

public class IconifiedTextListAdapter extends BaseAdapter implements Filterable {
  /** Remember our context so we can use it when constructing views. */
  private Context mContext;

  private static String lastFilter;
  private boolean isHomePage = true;
  public static ArrayList<Integer> mCheckedList = new ArrayList<Integer>();
  private MultiSelectListener mMultiSelectListener;

  public static interface MultiSelectListener {
	  public void onMultiSelectionChanged(int position);
  }

  public static String getLastFilter() {
	return lastFilter;
  }

  public static void setLastFilter(String lastFilter) {
	IconifiedTextListAdapter.lastFilter = lastFilter;
  }
  
  public boolean isHomePage() {
	return isHomePage;
}

  public void setOnMultiSelectListener(MultiSelectListener listener) {
	  mMultiSelectListener = listener;
  }
  
public void setHomePage(boolean isHomePage) {
	this.isHomePage = isHomePage;
	notifyDataSetChanged();
}
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

  private List<IconifiedText> mItems = new ArrayList<IconifiedText>();
  private List<IconifiedText> mOriginalItems = new ArrayList<IconifiedText>();

  public IconifiedTextListAdapter(Context context) {
    mContext = context;
  }

  public void addItem(IconifiedText it) {
    mItems.add(it);
  }

  public void setListItems(List<IconifiedText> lit, boolean filter) {
    mOriginalItems = lit;
    if (filter) {
      mItems = mFilter.synchronousFilter(lastFilter);
    } else {
      mItems = lit;
    }
  }

  /** @return The number of items in the */
  @Override
  public int getCount() {
    return mItems.size();
  }

  @Override
  public Object getItem(int position) {
    return mItems.get(position);
  }

  public boolean areAllItemsSelectable() {
    return false;
  }

  /*
   * public boolean isSelectable(int position) { try{ return
   * mItems.get(position).isSelectable(); }catch (IndexOutOfBoundsException
   * aioobe){ return super.isSelectable(position); } }
   */

  /** Use the array index as a unique id. */
  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(final int position, View convertView, ViewGroup parentView) {
    IconifiedTextView btv = null;
    if (convertView == null) {
      if (position >= mItems.size()) {
        return btv;
      }
    } else {
      if (convertView instanceof IconifiedTextView) {
        btv = (IconifiedTextView) convertView;
        if (position >= mItems.size()) {
          return btv;
        }
      }
    }
    btv = new IconifiedTextView(mContext, mItems.get(position));

    btv.setText(mItems.get(position).getText());
    btv.setIcon(mItems.get(position).getIcon());
   // btv.setInfo(mItems.get(position).getInfo());
    btv.setTime(mItems.get(position).getTimeInfo());
    if (mItems.get(position).getFieldTag() == 3) {
      // Check Online Status.
      btv.setDisabled(mItems.get(position).getIsOnline());
    }
    if (FileManagerApp.isGridView()){
        String mcServerID = mItems.get(position).getMCServerID();
        if ((mcServerID != null) &&
            (!mcServerID.isEmpty())){
            TextView infoView = (TextView) btv.findViewById(R.id.info);
            if (infoView != null){
                infoView.setVisibility(View.VISIBLE);
            }
        }
    }
    /*
     * IKSTABLEFIVE-3513 Commenting out this code that is related to MotoConnect
     * since it affects how the text field in other screens behave when clicked.
     * This needs to be fixed appropriately when MotoConnect feature is enabled.
     * Check Jira for detailed analysis.
     * btv.setDisabled(mItems.get(position).getIsOnline());
     * //mItems.get(position). TextView txtview =
     * (TextView)btv.findViewById(R.id.text); txtview.clearFocus();
     * //txtview.setEnabled(false);
     */
    CheckBox checkbox = (CheckBox) btv.findViewById(R.id.item_ticker);
    checkbox.setChecked(mItems.get(position).isChecked());
    checkbox.setOnClickListener(new View.OnClickListener() {		
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			mItems.get(position).setChecked(!(mItems.get(position).isChecked()));
			mMultiSelectListener.onMultiSelectionChanged(position);
		}
	});
    if (isHomePage) {
    	checkbox.setVisibility(View.GONE);
	} else {
		checkbox.setVisibility(View.VISIBLE);
		//checkbox.setChecked(mItems.get(position).isChecked());
	}
    if (FileManagerApp.getLaunchMode() == FileManagerApp.SELECT_FOLDER_MODE) {
      Button setlectBtn = (Button) btv.findViewById(R.id.file_select);
      if (mItems.get(position).getMiMeType().equals(IconifiedText.DIRECTORYMIMETYPE)) {
        setlectBtn.setVisibility(View.VISIBLE);
        setlectBtn.setTag(new NameAuth(mItems.get(position).getPathInfo(), mItems.get(position)
            .getAuthInfo()));
        setlectBtn.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            FileManager.rtnPickerResult(((NameAuth) v.getTag()).mPathInfo,
                ((NameAuth) v.getTag()).mAuthInfo);
            return;
          }
        });
      } else {
        setlectBtn.setVisibility(View.GONE);
      }
    }
    return btv;
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
    if (mFilesOnlySelectMode){
      for (IconifiedText item : mItems) {
        if (!item.getMiMeType().equals(IconifiedText.DIRECTORYMIMETYPE)){
          item.setChecked(true);
        }
      }
    } else {
      for (IconifiedText item : mItems) {
        item.setChecked(true);
      }
    }
    notifyDataSetChanged();
  }

  public void unSelectAll() {
    for (IconifiedText item : mItems) {
      item.setChecked(false);
    }
    notifyDataSetChanged();
  }

  public void setItemChecked(int position) {
    if (mFilesOnlySelectMode){
      if (!mItems.get(position).getMiMeType().equals(IconifiedText.DIRECTORYMIMETYPE)){
        if (mItems.get(position).isChecked()) {
          mItems.get(position).setChecked(false);
        } else {
          mItems.get(position).setChecked(true);
        }
      }
    } else {
      if (mItems.get(position).isChecked()) {
        mItems.get(position).setChecked(false);
      } else {
        mItems.get(position).setChecked(true);
      }
    }
    notifyDataSetChanged();
  }

  public boolean isSingleFileSelected() {
    int count = 0;
    for (IconifiedText item : mItems) {
      if (item.isChecked()) {
        count++;
        if (count > 1) {
          return false;
        }
      }
    }
    if (count == 0) {
      return false;
    }
    return true;
  }

  public boolean isSomeFileSelected() {
    for (IconifiedText item : mItems) {
      if (item.isChecked()) {
        return true;
      }
    }
    return false;
  }

  // Start IKSTABLEFIVE-5633 - Hide Select All Menu if all is selected
  public boolean isAllFilesSelected() {
    for (IconifiedText item : mItems) {
      if (!item.isChecked()) {
        return false;
      }
    }
    return true;
  }

  // End IKSTABLEFIVE-5633

  public ArrayList<String> getSelectFiles() {
    ArrayList<String> result = new ArrayList<String>();
    for (IconifiedText item : mItems) {
      if (item.isChecked()) {
        result.add(item.getPathInfo());
      }
    }
    return result;
  }

  /*public ArrayList<ZumoFile> getSelectZumoFiles() {
    ArrayList<ZumoFile> result = new ArrayList<ZumoFile>();

    for (IconifiedText item : mItems) {
      if (item.isChecked()) {
        result.add(item.getZumoFile());
      }
    }
    return result;
  }*/

  private boolean mFilesOnlySelectMode = false;

  public void setFilesOnlySelectMode(boolean filesOnlySelectMode){
    mFilesOnlySelectMode = filesOnlySelectMode;
  }

  @Override
  public Filter getFilter() {
    return mFilter;
  }
}
