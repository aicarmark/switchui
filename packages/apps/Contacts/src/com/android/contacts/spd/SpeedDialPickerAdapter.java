/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.android.contacts.spd;

import java.util.Enumeration;
import java.util.Hashtable;

import com.android.contacts.R;
import com.android.contacts.ContactsUtils;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 *
 * @author bluremployee
 */
public class SpeedDialPickerAdapter extends ContactPhotoCacheListAdapter{

    private DataSetObserver mDataSetObserver = null;
    private static final String TAG = "SpeedDialEditAdapter";
    private boolean isDualMode  = false;
    private int gridChildWidth  = 0;
    private int gridChildHeight  = 0;

    final static class CardTypeHolder {
        public ImageView callByCDMA;
        public ImageView callByGSM;
        public ImageView mCallByCorGButton;
        public int position;
    }

    public SpeedDialPickerAdapter(Context context, boolean isDualMode) {
        super(context);
        this.isDualMode = isDualMode;
        Resources resources =mContext.getApplicationContext().getResources();
        /*DisplayMetrics dm = new DisplayMetrics();
        dm = resources.getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;
        int gridMarginLeft = resources.getDimensionPixelSize(R.dimen.speed_dialer_girdview_margin_left);
        int gridMarginRight = resources.getDimensionPixelSize(R.dimen.speed_dialer_girdview_margin_right);
        gridChildWidth = (int)((1f/3) * (screenWidth - gridMarginLeft - gridMarginRight));
        if (ViewConfiguration.get(mContext).hasPermanentMenuKey()) {
            gridChildHeight = (int)(((3f/5)/3) * screenHeight);
        } else {
            gridChildHeight = (int)((2f/9) * screenHeight);
        }*/
        gridChildWidth = resources.getDimensionPixelSize(R.dimen.sd_edit_grid_child_width);
        gridChildHeight = resources.getDimensionPixelSize(R.dimen.sd_picker_grid_child_height);
    }

    @Override
    protected View createItemView(int position, View view, ViewGroup viewGroup) {
        final int tempPosition = position + (position/9);
        View child;
        if (isDualMode) {
            child = (RelativeLayout) View.inflate(mContext, R.layout.speed_dial_picker_adapter_dual_mode,null);
            final CardTypeHolder holder = new CardTypeHolder();
            holder.callByCDMA = (ImageView) child.findViewById(R.id.callByCDMA);
            holder.callByGSM = (ImageView) child.findViewById(R.id.callByGSM);
            holder.mCallByCorGButton = (ImageView) child.findViewById(R.id.callByCOrG);
            holder.position = tempPosition + 1;
            updateDialCardType(holder);
            child.setTag(holder);
        } else {
            child = (RelativeLayout) View.inflate(mContext, R.layout.speed_dial_picker_adapter_single_mode,null);
        }

        RelativeLayout layout = (RelativeLayout) child.findViewById(R.id.speedDialPickerAdapterHandle);
        layout.setLayoutParams(new GridView.LayoutParams(gridChildWidth, gridChildHeight));

        TextView name = (TextView)child.findViewById(R.id.pickerName);
        name.setTextColor(Color.WHITE);
        name.setTypeface(null,Typeface.BOLD);
        name.setText("");

        TextView number = (TextView)child.findViewById(R.id.pickerNumber);
        number.setText("");
        number.setVisibility(View.GONE);

        boolean excludeVoicemail = mContext.getResources().getBoolean(R.bool.ftr_36927_exclude_voicemail);
        ImageView spdPosition = (ImageView)child.findViewById(R.id.pickerPosition);
        spdPosition.setImageResource(Utils.getSpdDialPositionBgResid((tempPosition+1), excludeVoicemail));

        ImageView spdPositionUpper = (ImageView)child.findViewById(R.id.pickerPosition_upper);
        spdPositionUpper.setImageResource(Utils.getSpdDialPositionIcResid((tempPosition+1)));

        ImageView image = (ImageView) child.findViewById(R.id.pickerImage);
        image.setVisibility(View.INVISIBLE);

        return child;
    }

    public void dataSetChangeRequest() {
        if(mDataSetObserver != null) {
            mDataSetObserver.onChanged();
        }
    }

    @Override
    protected String getItemKey(int position) {
        position = position + (position/9);
        String number = Utils.getSpeedDialNumberByPos(mContext,(position+1));
        return number;
    }

    @Override
    protected QueryData getContactQueryData(int position) {
        QueryData queryData = new QueryData();
        String num = getItemKey(position);
        if(num == null || !Utils.hasSpeedDialByNumber(mContext, num)) {
            return null;
        }
        queryData.uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, num);
        queryData.projection = new String[]{
            PhoneLookup.NUMBER,
            PhoneLookup.DISPLAY_NAME,
            PhoneLookup.TYPE,
            PhoneLookup.LABEL,
            PhoneLookup._ID,
            PhoneLookup.PHOTO_ID
        };
        queryData.selection = " LIMIT 1";
        queryData.selectionArgs = null;
        queryData.orderBy = null;
        return queryData;
    }

    @Override
    protected void updateViewForContact(String key, View view, Cursor cursor) {
        TextView viewName   = (TextView) view.findViewById(R.id.pickerName);
        TextView viewNumber   = (TextView) view.findViewById(R.id.pickerNumber);
        ImageView image = (ImageView) view.findViewById(R.id.pickerImage);
        View contactView = view.findViewById(R.id.speedDialContact);
        Object holder =  view.getTag();

        if((cursor != null) && (cursor.moveToFirst())) {
            String name = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            String type = Phone.getDisplayLabel(
                        mContext, cursor.getInt(cursor.getColumnIndex(PhoneLookup.TYPE)),
                        cursor.getString(cursor.getColumnIndex(PhoneLookup.LABEL))).toString();

            // update UI and row tag
            viewName.setText(name);
            viewName.setTextColor(Color.WHITE);
            viewName.setTypeface(null,Typeface.BOLD);
            viewNumber.setVisibility(View.VISIBLE);
            viewNumber.setText(type + " " + PhoneNumberUtils.formatNumber(key));
            viewNumber.setTextColor(Color.WHITE);
            contactView.setVisibility(View.VISIBLE);

            if(name.compareTo(mContext.getString(R.string.voicemail)) == 0) {
                image.setImageResource(R.drawable.ic_launcher_voicemail);
                image.setVisibility(View.VISIBLE);
                contactView.setVisibility(View.VISIBLE);
            }
        }
        else {
            // Update UI for speed dial numbers with no associated contact
            viewNumber.setText("");
            viewName.setTextColor(Color.WHITE);
            viewNumber.setVisibility(View.GONE);
            if(key != null && Utils.hasSpeedDialByNumber(mContext, key)) {
                viewName.setTextColor(Color.WHITE);
                viewName.setTypeface(null,Typeface.BOLD);
                viewName.setText(PhoneNumberUtils.formatNumber(key));
            }
            if(cursor != null) {
                image.setImageResource(R.drawable.ic_thb_unknown_caller);
                image.setVisibility(View.VISIBLE);
                contactView.setVisibility(View.VISIBLE);
            } else {
                image.setVisibility(View.INVISIBLE);
                contactView.setVisibility(View.INVISIBLE);
            }
        }
    }

    @Override
    protected void updateViewForPhoto(View view, Bitmap bitmap) {
        ImageView image = (ImageView) view.findViewById(R.id.pickerImage);
        image.setVisibility(View.VISIBLE);
        if(bitmap != null) {
            image.setImageBitmap(bitmap);
        }
        else {
            image.setImageResource(R.drawable.ic_thb_contact_photo);
        }
    }

    public boolean areAllItemsEnabled() {
        return true;
    }

    public boolean isEnabled(int arg0) {
        return true;
    }

    public void registerDataSetObserver(DataSetObserver dso) {
        mDataSetObserver = dso;
    }

    public void unregisterDataSetObserver(DataSetObserver dso) {
        if((mDataSetObserver != null) && (dso != null) && (dso.equals(mDataSetObserver))) {
            mDataSetObserver = null;
        }
    }

    public int getCount() {
        return SpeedDialDataManger.SPD_COUNT;
    }

    public Object getItem(int arg0) {
        return null;
    }

    public long getItemId(int arg0) {
        return 0;
    }

    public boolean hasStableIds() {
        return true;
    }

    public int getItemViewType(int arg0) {
        return 0;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public boolean isEmpty() {
        return (getCount() == 0);
    }

    void updateDialCardTypes(GridView grid) {
        for (int i = 0 ; null != grid && i < grid.getChildCount() ; i++) {
            View view = grid.getChildAt(i);
            Object holder =  view.getTag();
            if (null != holder) {
                updateDialCardType((CardTypeHolder)holder);
            }
        }
    }

    private void updateDialCardType(CardTypeHolder holder) {
        if (null == holder) return;

        String number = Utils.getSpeedDialNumberByPos(mContext, holder.position);
        if (number == null || !Utils.hasSpeedDialByNumber(mContext, number)) {
            return;
        }

        int defaultCard = Utils.getDefaultCallCard(mContext, holder.position);

        boolean isGSMEnabled = ContactsUtils.isPhoneEnabled(TelephonyManager.PHONE_TYPE_GSM);
        boolean isCDMAEnabled = ContactsUtils.isPhoneEnabled(TelephonyManager.PHONE_TYPE_CDMA);
        if (!isGSMEnabled && !isCDMAEnabled) {
            holder.callByCDMA.setVisibility(View.GONE);
            holder.callByGSM.setVisibility(View.GONE);
            holder.mCallByCorGButton.setVisibility(View.VISIBLE);
            if (TelephonyManager.PHONE_TYPE_CDMA == defaultCard) {
                holder.mCallByCorGButton.setBackgroundResource(R.drawable.sd_dual_mode_one_sim_c);
            } else if (TelephonyManager.PHONE_TYPE_GSM == defaultCard){
                holder.mCallByCorGButton.setBackgroundResource(R.drawable.sd_dual_mode_one_sim_g);
            }
        } else if ((isGSMEnabled && !isCDMAEnabled) || (!isGSMEnabled&&isCDMAEnabled)) {
            holder.callByCDMA.setVisibility(View.GONE);
            holder.callByGSM.setVisibility(View.GONE);
            holder.mCallByCorGButton.setVisibility(View.VISIBLE);
            if (isGSMEnabled) {
                holder.mCallByCorGButton.setBackgroundResource(R.drawable.sd_dual_mode_dual_sim_g_highlight);
            } else {
                holder.mCallByCorGButton.setBackgroundResource(R.drawable.sd_dual_mode_dual_sim_c_highlight);
            }
        } else {
            holder.callByCDMA.setVisibility(View.VISIBLE);
            holder.callByGSM.setVisibility(View.VISIBLE);
            holder.mCallByCorGButton.setVisibility(View.GONE);
            if (TelephonyManager.PHONE_TYPE_CDMA == defaultCard) {
                holder.callByCDMA.setBackgroundResource(R.drawable.sd_dual_mode_dual_sim_c_highlight);
                holder.callByGSM.setBackgroundResource(R.drawable.sd_dual_mode_dual_sim_g);
                holder.callByCDMA.setClickable(false);
                holder.callByGSM.setClickable(true);
            } else if (TelephonyManager.PHONE_TYPE_GSM == defaultCard){
                holder.callByCDMA.setBackgroundResource(R.drawable.sd_dual_mode_dual_sim_c);
                holder.callByGSM.setBackgroundResource(R.drawable.sd_dual_mode_dual_sim_g_highlight);
                holder.callByCDMA.setClickable(true);
                holder.callByGSM.setClickable(false);
            }
        }
    }
}
