/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * MXDN83        2012/05/07 Smart Actions 2.1 Initial Version
 */

package com.motorola.contextual.pickers;

import java.util.List;

import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SeekBar;

/**
 * This custom list adapter is intended for views with a
 * primary TextView, a secondary TextView, an ImageView,
 * a specific clickable View, and a View that is only
 * shown if a non-null View.OnClickListener is supplied.
 * More types of views can be added when needed in pickers
 * <code><pre>
 *
 * CLASS:
 *  extends BaseAdapter
 *
 * RESPONSIBILITIES:
 *  Provide a model-controller to map array list or collection list data
 *  to xml layout views.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class CustomListAdapter extends BaseAdapter implements ListAdapter{

    private LayoutInflater mInflater;
    private Context mContext;
    //Array of pre-selected list items
    private boolean[] mCheckedItems;
    //This adapter can take either an array or a collection of list items
    private ListItem[] mItemsListArray;
    private List<ListItem> mItemsList;
    //It can also take a custom layout for each list item,
    //list_item_tap_text layout resource is used by default
    private int mListItemLayoutResID=0;

    /**
     * Simple constructor.
     *
     * @param context The context where the ListView associated with this adapter is running
     * @param items Array of ListItem objects
     */
    public CustomListAdapter(Context context, ListItem[] items) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mItemsListArray = items;
        mCheckedItems = null;
    }

    /**
     * Constructor for multi selection lists.
     *
     * @param context The context where the ListView associated with this adapter is running
     * @param items Array of ListItem objects
     * @param checkedItems Array of boolean objects representing check states of the list items
     * @param listItemLayoutResId layout resource id for the individual list items
     */
    public CustomListAdapter(Context context, ListItem[] items,
            boolean[] checkedItems, int listItemLayoutResId) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mItemsListArray = items;
        mCheckedItems = checkedItems;
        mListItemLayoutResID = listItemLayoutResId;
    }

    /**
     * Simple constructor.
     *
     * @param context The context where the ListView associated with this adapter is running
     * @param items Collection list of ListItem objects
     */
    public CustomListAdapter(Context context, List<ListItem> items) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mItemsList = items;
        mCheckedItems = null;
    }

    /**
     * Constructor for multi selection lists.
     *
     * @param context The context where the ListView associated with this adapter is running
     * @param items Collection list of ListItem objects
     * @param checkedItems Array of boolean objects representing check states of the list items
     * @param listItemLayoutResId layout resource id for the individual list items
     */
    public CustomListAdapter(Context context, List<ListItem> items,
            boolean[] checkedItems, int listItemLayoutResId) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mItemsList = items;
        mCheckedItems = checkedItems;
        mListItemLayoutResID = listItemLayoutResId;
    }

    /**
     * setter for items list
     */
    public void setItemsList(List<ListItem> items) {
        mItemsList = items;
    }

    /**
     * setter for items list
     */
    public void setItemsListArray(ListItem[] items) {
        mItemsListArray = items;
    }

    /**
     * setter for checked items, typically used after the picker is built for updating list items
     */
    public void setCheckedItems(boolean[] checkedItems) {
        mCheckedItems = checkedItems;
    }

    /**
     * returns the number of items represented by this adapter
     * @see android.widget.Adapter#getCount()
     */
    public int getCount() {
        if(mItemsListArray != null) {
            return mItemsListArray.length;
        }else if(mItemsList != null) {
            return mItemsList.size();
        }
        return 0;
    }

    /**
     * @see android.widget.Adapter#getItem(int)
     */
    public Object getItem(int position) {
        if(mItemsListArray != null) {
            return mItemsListArray[position];
        }else if(mItemsList != null) {
            return mItemsList.get(position);
        }
        return null;
    }

    /**
     * @see android.widget.Adapter#getItemId(int)
     */
    public long getItemId(int position) {
        return position;
    }

    /**
     * Returns the view, based on the list item type
     * @see android.widget.Adapter#getView(int, View, ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        ListItem item = (ListItem) getItem(position);
        if (convertView == null) {
            //if there's a layout specified then use it
            if(mListItemLayoutResID > 0) {
                convertView = mInflater.inflate(mListItemLayoutResID, null);
            }else {
                convertView = mInflater.inflate(R.layout.list_item_tap_text, null);
            }
            holder = new ViewHolder();
            holder.label = (TextView) convertView.findViewById(R.id.list_item_label);
            holder.desc = (TextView) convertView.findViewById(R.id.list_item_desc);
            holder.icon = (ImageView) convertView.findViewById(R.id.list_item_icon);
            holder.colorChip = convertView.findViewById(R.id.list_item_color_chip);
            convertView.setTag(holder);
            holder.itemType = item.mType;
        }else {
            holder = (ViewHolder) convertView.getTag();
        }

        //check the view type - if the list items are added/deleted
        //and the new list item is of a different type, don't reuse the view
        if(holder.itemType != item.mType) {
            if(mListItemLayoutResID > 0) {
                convertView = mInflater.inflate(mListItemLayoutResID, null);
            }else {
                convertView = mInflater.inflate(R.layout.list_item_tap_text, null);
            }
            holder = new ViewHolder();
            holder.label = (TextView) convertView.findViewById(R.id.list_item_label);
            holder.desc = (TextView) convertView.findViewById(R.id.list_item_desc);
            holder.icon = (ImageView) convertView.findViewById(R.id.list_item_icon);
            holder.colorChip = convertView.findViewById(R.id.list_item_color_chip);
            convertView.setTag(holder);
            holder.itemType = item.mType;
        }
        holder.label.setText(Html.fromHtml(item.mLabel.toString()));
        //Set the description text, if supplied
        if(item.mDesc != null) {
            holder.desc.setText(Html.fromHtml(item.mDesc.toString()));
            holder.desc.setVisibility(View.VISIBLE);
        } else {
            holder.desc.setVisibility(View.GONE);
        }
        //Set the icon, if supplied
        if (item.mIconUri != null) {
            holder.icon.setImageURI(item.mIconUri);
        } else if(item.mIcon == null) {
            if(item.mIconDrawableResID >= 0) {
                final Bitmap image = BitmapFactory.decodeResource(mContext.getResources(),
                        item.mIconDrawableResID);
                if (image == null) {
                    holder.icon.setVisibility(View.GONE);
                } else {
                    holder.icon.setImageBitmap(image);
                }
            } else {
                holder.icon.setVisibility(View.GONE);
            }
        } else {
            holder.icon.setImageDrawable(item.mIcon);
        }

        // Set color chip if supplied
        if (item.mChipColor != 0) {
            holder.colorChip.setBackgroundColor(item.mChipColor);
            holder.colorChip.setVisibility(View.VISIBLE);
        } else if (holder.colorChip != null) {
            holder.colorChip.setVisibility(View.GONE);
        }

        //Setup the view based on the list item type
        if(item.mType == ListItem.typeTWO) {
            if(item.mLabelClickListener != null) {
                convertView.findViewById(R.id.list_item_text_area).setClickable(true);
                convertView.findViewById(R.id.list_item_text_area).setOnClickListener(item.mLabelClickListener);
                convertView.findViewById(R.id.list_item_text_area).setBackgroundResource(R.drawable.list_selector_holo_dark);
            }
            convertView.findViewById(R.id.divider_line).setVisibility(View.VISIBLE);
        }else if(item.mType == ListItem.typeTHREE) {
            if(item.mLabelClickListener != null) {
                convertView.findViewById(R.id.list_item_text_area).setClickable(true);
                convertView.findViewById(R.id.list_item_text_area).setOnClickListener(item.mLabelClickListener);
                convertView.findViewById(R.id.list_item_text_area).setBackgroundResource(R.drawable.list_selector_holo_dark);

                if (holder.icon.getVisibility() != View.GONE) {
                    holder.icon.setClickable(true);
                    holder.icon.setOnClickListener(item.mLabelClickListener);
                }
            }
            convertView.findViewById(android.R.id.text1).setVisibility(View.GONE);
        }else if(item.mType == ListItem.typeFOUR) {
            SeekBar sk = (SeekBar)convertView.findViewById(R.id.seekbar);
            sk.setOnSeekBarChangeListener(item.mSeekBarParams.seekBarChangeListener);
            if(item.mSeekBarParams.max > 0) {
                sk.setMax(item.mSeekBarParams.max);
            }
            sk.setProgress(item.mSeekBarParams.currentProgress);
            sk.setVisibility(View.VISIBLE);
        }else if(item.mType == ListItem.typeFIVE){
            ((SeekBar)convertView.findViewById(R.id.seekbar))
            .setOnSeekBarChangeListener(item.mSeekBarParams.seekBarChangeListener);
            ((SeekBar)convertView.findViewById(R.id.seekbar)).setProgress(item.mSeekBarParams.currentProgress);
            if(item.mSeekBarParams.max > 0) {
                ((SeekBar)convertView.findViewById(R.id.seekbar))
                .setMax(item.mSeekBarParams.max);
            }
            convertView.findViewById(R.id.seekbar).setVisibility(View.VISIBLE);
            convertView.findViewById(android.R.id.text1).setVisibility(View.GONE);
        }
        //Set the check state of the list item
        if (mCheckedItems != null) {
            try {
                boolean isItemChecked = mCheckedItems[position];
                if (isItemChecked) {
                    ListView listView = (ListView)parent;
                    listView.setItemChecked(position, true);
                }
            } catch(ArrayIndexOutOfBoundsException aiobe) {
                aiobe.printStackTrace();
            }
        }

        //Disable/enable all necessary elements based on mEnabled
        //... this probably needs to be extended to other elements after testing
        convertView.findViewById(android.R.id.text1).setEnabled(item.mEnabled);
        convertView.findViewById(R.id.list_item_text_area).setEnabled(item.mEnabled);
        convertView.findViewById(R.id.list_item_label).setEnabled(item.mEnabled);
        convertView.findViewById(R.id.list_item_desc).setEnabled(item.mEnabled);
        convertView.findViewById(R.id.list_item_icon).setEnabled(item.mEnabled);

        return convertView;
    }

    @Override
    public boolean isEnabled(int position) {
        ListItem item = (ListItem) getItem(position);
        boolean output = super.isEnabled(position);

        // Return false if item is specifically disabled
        if (item != null)
            output = item.mEnabled;

        return output;
    }

    /**
     * Class used as a convenient view tag.
     */
    class ViewHolder {
        TextView label;
        TextView desc;
        ImageView icon;
        View colorChip;
        int itemType = ListItem.typeONE;
    }

}
