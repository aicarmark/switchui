package com.motorola.FileManager;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.util.Log;

public class FMBaseAdapter extends BaseAdapter {

    private LayoutInflater mInflater;
    protected boolean mChoiceMode = false;
    protected int mViewMode = Util.VIEW_LIST;
    protected FileSystem mFileSystem;
    private int mResource = R.layout.listitem;
    private boolean mShowCount = true;
    protected Context mContext = null;

    protected FileInfo[] mFiles;

    static class ViewHolder {
	TextView fileName;
	TextView folderName;
	TextView modified_time;
	TextView fileInfo;
	ImageView fileImage;
	CheckBox fileCB;
	LinearLayout panel;
	LinearLayout text_info;
    }

    static class GridViewHolder {
	TextView mText;
	ImageView mIcon;
	CheckBox fileCB;
    }

    // Begin CQG478 liyun, IKDOMINO-4110, adapter image icon 2011-11-09
    protected int mCount = 0;

    // End

    public FMBaseAdapter(Context context) {
	mContext = context;
	mFileSystem = FileSystem.getFileSystem(context);
	//mInflater = (LayoutInflater) context
		//.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	
	mInflater = (LayoutInflater) LayoutInflater.from(context);
    }

    public void setViewMode(int mode) {
	mViewMode = mode;
    }

    public void updateView() {
	if (mViewMode == Util.VIEW_LIST) {
	    mResource = R.layout.listitem;
	} else {
	    mResource = R.layout.grid_item;
	}
    }

    public void setChoiceMode(boolean mode) {
	mChoiceMode = mode;
    }

    public void setResource(int resource) {
	mResource = resource;
    }

    public FileInfo getRoot() {
	return FileSystem.getRoot();
    }

    public int getCount() {
	return mCount;
    }

    // test fi's subfolder
    public boolean hasFolder(FileInfo fi) {
	if (fi == null)
	    return false;

	FileInfo[] ffi = fi.getChildren();
	if (ffi == null)
	    return false;
	for (int i = 0; i < ffi.length; i++)
	    if (ffi[i].isDirectory())
		return true;

	return false;
    }

    public void setShowCountForFolder(boolean show) {
	mShowCount = show;
    }

    public FileSystem getFileSystem() {
	return mFileSystem;
    }

    public Object getItem(int position) {
	if (mFiles != null && mFiles.length > 0) {
	    // Begin, kbg374, IKDOMINO-5524, check the bound, 2011-12-30
	    if (position >= 0 && position < mFiles.length) {
		return mFiles[position];
	    } else {
		Log.e("FMBaseAdapter", "position exceed the length");
		return null;
	    }
	    // End
	}
	return null;
    }

    public long getItemId(int position) {
	return position;
    }

    public void refresh() {

    }

    public void setCurrentFile(FileInfo fi) {

    }

    // Begin CQG478 liyun, IKDOMINO-3650, sub folder 2011-11-01
    public View getView(int position, View convertView, ViewGroup parent) {

	if (position >= getCount())
	    return null;
	final FileInfo fi = mFiles[position];
	if (fi == null) {
		return null;
	}
	if (mViewMode == Util.VIEW_LIST) {
	    ViewHolder holder;
	    if (convertView == null) {
		convertView = mInflater.inflate(mResource, parent, false);
		holder = new ViewHolder();
		holder.fileName = (TextView) convertView
			.findViewById(R.id.vf_name);
		holder.fileInfo = (TextView) convertView
				.findViewById(R.id.info);
		holder.modified_time = (TextView) convertView
				.findViewById(R.id.info_time);
		holder.folderName = (TextView) convertView
			.findViewById(R.id.name);
		holder.fileImage = (ImageView) convertView
			.findViewById(R.id.icon);
		holder.fileCB = (CheckBox) convertView
			.findViewById(R.id.checkbox);
		holder.panel = (LinearLayout) convertView
			.findViewById(R.id.text_panel);
		holder.text_info = (LinearLayout) convertView
				.findViewById(R.id.text_info);

		convertView.setTag(holder);
	    } else {
	    	try {
	    	holder = (ViewHolder) convertView.getTag();
	    	} catch (Exception e){
	    		Log.e("","w23001-- ClassCastException");
	    		convertView = mInflater.inflate(mResource, parent, false);
	    		holder = new ViewHolder();
	    		holder.fileName = (TextView) convertView
	    			.findViewById(R.id.vf_name);
	    		holder.fileInfo = (TextView) convertView
	    				.findViewById(R.id.info);
	    		holder.modified_time = (TextView) convertView
	    				.findViewById(R.id.info_time);
	    		holder.folderName = (TextView) convertView
	    			.findViewById(R.id.name);
	    		holder.fileImage = (ImageView) convertView
	    			.findViewById(R.id.icon);
	    		holder.fileCB = (CheckBox) convertView
	    			.findViewById(R.id.checkbox);
	    		holder.panel = (LinearLayout) convertView
	    			.findViewById(R.id.text_panel);
	    		holder.text_info = (LinearLayout) convertView
	    				.findViewById(R.id.text_info);
	    		convertView.setTag(holder);
	    	}
	    }
	    if (fi.isVirtualFolder()) {
	    	if (holder.text_info != null) {
			    holder.text_info.setVisibility(View.VISIBLE);
			}
			if (holder.fileName != null) {
			    holder.fileName.setVisibility(View.VISIBLE);
			    holder.fileName.setText(fi.getTitle());
			}	
			if (holder.panel != null
				&& holder.panel.getVisibility() != View.GONE) {
			    holder.panel.setVisibility(View.GONE);
			}

		} else {
			if (holder.panel != null) {
			    holder.panel.setVisibility(View.VISIBLE);
			}
			if (holder.folderName != null) {
				holder.folderName.setVisibility(View.VISIBLE);
			    holder.folderName.setText(fi.getTitle());
			    if (fi.isHidden()) {
			    	holder.folderName.setTextColor(Color.DKGRAY);
			    } else {
			    	holder.folderName.setTextColor(Color.WHITE);
			    }
			} 
			if (holder.fileInfo != null) {
			    if (mShowCount) {
				if (holder.fileInfo.getVisibility() != View.VISIBLE) {
				    holder.fileInfo.setVisibility(View.VISIBLE);
				}
			        if (fi.isDirectory()) {
			            //Log.d("", "dir = " + fi.getRealPath());
				    holder.fileInfo.setText(String.format("(%d)",
			            fi.getUnHideChildCount()));
				 } else {
				    	//Log.d("", "file = " + fi.getRealPath());
				    	holder.fileInfo.setText(Util.getSizeString(fi.getSize()));
				 }
				 if (fi.isHidden()) {
					  holder.fileInfo.setTextColor(Color.DKGRAY);
			         } else {
					    holder.fileInfo.setTextColor(Color.WHITE);
					}
				 } else {
					if (holder.fileInfo.getVisibility() != View.GONE) {
					    holder.fileInfo.setVisibility(View.GONE);
					}
				 }
			}
		}

		if (holder.fileImage != null && mViewMode == Util.VIEW_LIST) {
			holder.fileImage.setImageDrawable(fi.getIcon(mContext));	
			if (fi.getFileMimeId() == Util.NEED_QUERY_MIME) {
			    ImageView tempImage = holder.fileImage;
	            fi.setFileMimeId(Util.getMimeId(mContext, fi.getRealPath()));
	            tempImage.setImageDrawable(fi.getIcon(mContext));
			}
		}
		
		if (holder.modified_time != null) {
                        if (mShowCount) {
			    holder.modified_time.setVisibility(View.VISIBLE);
			    holder.modified_time.setText(new java.sql.Date(fi.getDate()).toString());
                         } else {
                             holder.modified_time.setVisibility(View.GONE);
                         } 
		}
		
	    if (holder.fileCB != null)
		holder.fileCB.setVisibility(mChoiceMode ? View.VISIBLE
			: View.GONE);
	    return convertView;
	} else {
	    GridViewHolder gridholder = new GridViewHolder();
	    if (convertView == null) {
		convertView = mInflater.inflate(mResource, parent, false);
		gridholder.mText = (TextView) convertView
			.findViewById(R.id.text);
		gridholder.mIcon = (ImageView) convertView
			.findViewById(R.id.icon);
		gridholder.fileCB = (CheckBox) convertView
			.findViewById(R.id.checkbox);
		convertView.setTag(gridholder);
	    } else {
	        try{	
	        	gridholder = (GridViewHolder) convertView.getTag();
	        } catch (Exception e){	        	
	        	Log.e("","w23001-- ClassCastException");
	        	convertView = mInflater.inflate(mResource, parent, false);
			//add by amt_xulei for SWITCHUITWOV-129 2012-9-6
			gridholder = new GridViewHolder();
			//end add by amt_xulei for SWITCHUITWOV-129 2012-9-6
				gridholder.mText = (TextView) convertView
					.findViewById(R.id.text);
				gridholder.mIcon = (ImageView) convertView
					.findViewById(R.id.icon);
				gridholder.fileCB = (CheckBox) convertView
					.findViewById(R.id.checkbox);
				convertView.setTag(gridholder);
	        }
	    }
	    if (fi.isVirtualFolder()) {
		if (gridholder.mText != null) {
		    gridholder.mText.setVisibility(View.VISIBLE);
		    gridholder.mText.setText(fi.getTitle());
		}
	    } else {
		if (gridholder.mText != null) {
		    gridholder.mText.setText(fi.getTitle());
		    if (fi.isHidden()) {
			gridholder.mText.setTextColor(Color.DKGRAY);
		    } else {
			gridholder.mText.setTextColor(Color.WHITE);
		    }
		}
	    }

	    if (gridholder.mIcon != null) {
		gridholder.mIcon.setImageDrawable(fi.getIcon(mContext));

		if (fi.getFileMimeId() == Util.NEED_QUERY_MIME) {
		    final ImageView tempImage = gridholder.mIcon;
                    fi.setFileMimeId(Util.getMimeId(mContext, fi.getRealPath()));
                    tempImage.setImageDrawable(fi.getIcon(mContext));
		    /*fi.setImageViewIcon(mContext, new Util.ImageCallBack() {

			@Override
			public void setImageIcon() {
			    tempImage.setImageDrawable(fi.getIcon(mContext));
			}
		    });*/
		}
	    }

	    if (gridholder.fileCB != null)
		if (FileManagerActivity.isSearch) {
		    gridholder.fileCB.setVisibility(View.GONE);
		} else {
		    gridholder.fileCB.setVisibility(mChoiceMode ? View.VISIBLE
			    : View.GONE);
		}
	    return convertView;
	}
    }
}
