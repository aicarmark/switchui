package com.motorola.mmsp.motohomex.apps;

import java.util.ArrayList;

import com.motorola.mmsp.motohomex.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GroupOptionsAdapter extends BaseAdapter {

    private final AllAppsPage mAllAppsPage;
    private final LayoutInflater mInflater;
    private ArrayList<Integer> mArray;
    private int mNumbBaseOptions;

    GroupOptionsAdapter(AllAppsPage allAppsPage, ArrayList<Integer> array, int numbBaseOptions) {
        mArray = array;
        mAllAppsPage = allAppsPage;
        mInflater = LayoutInflater.from(mAllAppsPage.getContext());
        mNumbBaseOptions = numbBaseOptions;
    }

    @Override
    public int getCount() {
        if (mArray == null) {
            return 0;
        }

        GroupItem group = mAllAppsPage.getCurrentGroupItem();
//Add by e13775 July25 for SWITCHUI-2431 start
        if (group == null){
           return 0;
        } 
//Add by e13775 July25 for SWITCHUI-2431 ened
        if(!mAllAppsPage.isApplicationTab()){
             return AllAppsPage.MENU_SYSTEM_SETTINGS + 1;
        }else if(group.isEditable()) {
            return mArray.size();
        } else {
            // Return only the base options
            return mNumbBaseOptions;
        }
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.all_apps_drop_down_menu_item, parent, false);
        }

        ((TextView)convertView).setText(mArray.get(position));
        return convertView;
    }
}
