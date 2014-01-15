package com.motorola.mmsp.taskmanager;



import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

/** which is used to show list item */
class ProcessAndTaskAdapter extends BaseAdapter {

    private List<ProcessAndTaskInfo> mTaskInfoList;
    private LayoutInflater mInflater;

    private static final String TAG = "ProcessAndTaskAdapter";

    public ProcessAndTaskAdapter(Context context,
        List<ProcessAndTaskInfo> TaskList) {
        mInflater = LayoutInflater.from(context);
        mTaskInfoList = TaskList;
    }

    /**
     * The number of items in the list is determined by the number of speeches
     * in our array.
     * 
     * @see android.widget.ListAdapter#getCount()
     */
    public int getCount() {
        return mTaskInfoList.size();
    }

    /**
     * Since the data comes from an array, just returning the index is sufficent
     * to get at the data. If we were using a more complex data structure, we
     * would return whatever object represents one row in the list.
     * 
     * @see android.widget.ListAdapter#getItem(int)
     */
    public Object getItem(int position) {
        return mTaskInfoList.get(position);
    }

    /**
     * Use the array index as a unique id.
     * 
     * @see android.widget.ListAdapter#getItemId(int)
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Make a view to hold each row.
     * 
     * @see android.widget.ListAdapter#getView(int, android.view.View,
     *      android.view.ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        Common.Log(TAG, "EfficientAdapter.getView");
        // A ViewHolder keeps references to children views to avoid
        // unneccessary calls
        // to findViewById() on each row.
        ViewHolder holder;
        ProcessAndTaskInfo tempTaskInfo = mTaskInfoList.get(position);
    
        // When convertView is not null, we can reuse it directly, there is
        // no need
        // to reinflate it. We only inflate a new View when the convertView
        // supplied
        // by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_icon_text, null);
    
            // Creates a ViewHolder and store references to the two children
            // views
            // we want to bind data to.
            holder = new ViewHolder();
            holder.text = (TextView) convertView.findViewById(R.id.text);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.checkbox = (CheckedTextView) convertView
                .findViewById(R.id.multi_picker_list_item_name);
            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }
    
        if (tempTaskInfo.checkvisibility) {
            holder.checkbox.setVisibility(View.VISIBLE);
            /* set check box attribute */
            holder.checkbox.setClickable(false);
            holder.checkbox.setChecked(tempTaskInfo.checked);
            /* set disable list item */
            if (tempTaskInfo.isPersistent) {
            holder.checkbox.setEnabled(false);
            } else {
            holder.checkbox.setEnabled(true);
            }
        } else
            holder.checkbox.setVisibility(View.GONE);
    
        // Bind the data efficiently with the holder.
        holder.text.setText(tempTaskInfo.appname);
        holder.icon.setImageDrawable(tempTaskInfo.appicon);
        return convertView;
    }

    public void updateList(List<ProcessAndTaskInfo> appList) {
        mTaskInfoList = appList;
        notifyDataSetChanged();
    }

    public void updateList() {
        notifyDataSetChanged();
    }

    public class ViewHolder {
        TextView text;
        ImageView icon;
        CheckedTextView checkbox;
    }

}
