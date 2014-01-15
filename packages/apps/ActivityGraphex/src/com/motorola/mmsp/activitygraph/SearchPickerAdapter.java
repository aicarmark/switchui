package com.motorola.mmsp.activitygraph;
import android.widget.ArrayAdapter;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.view.LayoutInflater;
import java.util.ArrayList;
import java.util.List;

import android.view.ViewGroup;
import android.widget.Filter;


public class SearchPickerAdapter extends ArrayAdapter<AppInfo>
implements Filterable {
    
    private ArrayList<AppInfo> mAppInfoList;
    private ArrayList<AppInfo> mSubAppInfoList;
    private Context mContext;
    
    private final LayoutInflater mInflater;
    
    private AppsFilter mFilter;
    private final Object mLock = new Object();
    
    public SearchPickerAdapter(Context context, int resource,int textViewResourceId, ArrayList<AppInfo> apps) {
        super(context,resource, textViewResourceId, apps);
        mInflater = LayoutInflater.from(context);
        mContext = context;
        this.mSubAppInfoList = apps;
        this.mAppInfoList = this.mSubAppInfoList;
    }
    
    @Override
    public int getCount() {
        return mSubAppInfoList.size();
    }
    
    @Override
    public AppInfo getItem(int position) {
        return mSubAppInfoList.get(position);
    }
    
    @Override
    public int getPosition(AppInfo item) {
        return mSubAppInfoList.indexOf(item);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final AppInfo info = getItem(position);
        
        if(info.appname == mContext.getString(R.string.none_application)) {
            convertView = mInflater.inflate(R.layout.app_none_item, parent, false);
            ((TextView) convertView.findViewById(R.id.appnone)).setText(R.string.none_string);       	
            
        }else{
            
            ViewHolder holder;
            ViewHolder holderTag;
            
            if(convertView == null){
                convertView = mInflater.inflate(R.layout.app_info_item, parent, false);
                
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.appname);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.checkbox = (CheckedTextView) convertView
                        .findViewById(R.id.multi_picker_box);
                convertView.setTag(holder);
                
            } else {
                holderTag = (ViewHolder) convertView.getTag();
                if (holderTag == null) {
                    
                    convertView = mInflater.inflate(R.layout.app_info_item, parent, false);
                    
                    holder = new ViewHolder();
                    holder.text = (TextView) convertView.findViewById(R.id.appname);
                    holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                    holder.checkbox = (CheckedTextView) convertView
                            .findViewById(R.id.multi_picker_box);
                    convertView.setTag(holder);
                } else {
                    // Get the ViewHolder back to get fast access to the TextView
                    // and the ImageView.
                    holder = holderTag;//(ViewHolder) convertView.getTag();
                    
                }
            }
            if (info.checkvisibility) {
                holder.checkbox.setVisibility(View.VISIBLE);
                holder.checkbox.setChecked(info.checked);
            } else
                holder.checkbox.setVisibility(View.GONE);
            
            info.appname = info.appname.replace('\r', ' ');
            info.appname = info.appname.replace('\n', ' ');
            holder.text.setText(info.appname);
            holder.icon.setImageDrawable(info.appicon);
        }
        return convertView;
    }
    protected void finalize() throws Throwable {
        try {
            Log.d("PickerAdapter", "finalize()");
            //mServiceListener.clear();
            //close();
            //bConnected = false;
        } finally {
            super.finalize();
        }
    }
    @Override
    public Filter getFilter() {
        if (mFilter == null){
            mFilter  = new AppsFilter();
        }
        return mFilter;
    }
    
    private class AppsFilter extends Filter {
        protected FilterResults performFiltering(CharSequence prefix) {
            // Initiate our results object
            FilterResults results = new FilterResults();
            
            // No prefix is sent to filter by so we're going to send back the original array
            if (prefix == null || prefix.length() == 0) {
                synchronized (mLock) {
                    results.values = mAppInfoList;
                    results.count = mAppInfoList.size();
                }
            } else {
                // Compare lower case strings
                String prefixString = prefix.toString().toLowerCase();
                
                // Local to here so we're not changing actual array
                final ArrayList<AppInfo> items = mAppInfoList;
                final int count = items.size();
                final ArrayList<AppInfo> newItems = new ArrayList<AppInfo>(count);
                String itemName;
                for (int i = 0; i < count; i++) {
                    final AppInfo item = items.get(i);
                    itemName = item.getAppName().toString().toLowerCase();
                    
                    // First match against the whole, non-splitted value
                    
                    // add by gxl ------begin
                    if (itemName == null || itemName.length() == 0) {
                        Log.i("gxl", "-------name is null---------");
                    } else {
                        itemName = itemName.trim();
                        if (itemName.length() > 0) {
                            if (itemName.charAt(0) == 160) {
                                //if the first char is chinese-space, remove it.
                                itemName = itemName.substring(1);
                            }
                        } else {
                            Log.i("gxl", "----itemName length is 0");
                        }
                    }
                 // add by gxl ------end
                    if (itemName.startsWith(prefixString)) {
                        newItems.add(item);
                    } else {} /* This is option and taken from the source of ArrayAdapter
                        final String[] words = itemName.split(" ");
                        final int wordCount = words.length;

                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(prefixString)) {
                                newItems.add(item);
                                break;
                            }
                        }
                    } */
                }
                
                // Set and return
                results.values = newItems;
                results.count = newItems.size();
            }
            
            return results;
        }
        
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence prefix, FilterResults results) {
            //noinspection unchecked
            mSubAppInfoList = (ArrayList<AppInfo>) results.values;
            // Let the adapter know about the updated list
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
    
    public class ViewHolder {
        TextView text;
        ImageView icon;
        CheckedTextView checkbox;
    }
    
    public void updateList() {
        // TODO Auto-generated method stub
        notifyDataSetChanged();
    }
    
    public void updateList( ArrayList<AppInfo> appList) {
        this.mSubAppInfoList = appList;
        this.mAppInfoList = this.mSubAppInfoList;   
        notifyDataSetChanged(); 
    }
    
}
