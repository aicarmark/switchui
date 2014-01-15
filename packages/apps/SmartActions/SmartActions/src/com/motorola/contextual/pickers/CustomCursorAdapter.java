/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * XPR643        2012/05/10 Smart Actions 2.1 Initial Version
 */
package com.motorola.contextual.pickers;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * This cursor adapter is intended for views with a
 * primary TextView, a secondary TextView, an ImageView,
 * a specific clickable View, and a View that is only
 * shown if a non-null View.OnClickListener is supplied.
 * <code><pre>
 *
 * CLASS:
 *  extends ResourceCursorAdapter - standard Android cursor adapter for xml layouts
 *
 * RESPONSIBILITIES:
 *  Provide a model-controller to map cursor data to xml layout views.
 *
 * COLLABORATORS:
 *  N/A
 *
 * USAGE:
 *  See each method.
 *</pre></code>
 */
public class CustomCursorAdapter extends ResourceCursorAdapter {
    protected static final String TAG = CustomCursorAdapter.class.getSimpleName();

    /** List item layout label text view */
    private int mLabelId = -1;
    /** List item label cursor column */
    private String mLabelCol = null;

    /** List item layout description text view resource ID */
    private int mDescId = -1;
    /** List item description cursor column */
    private String mDescCol = null;

    /** List item layout icon image view resource ID */
    private int mIconId = -1;
    /** List item icon Uri cursor column */
    private String mIconCol = null;

    /** List item layout clickable view resource ID */
    private int mClickViewId = -1;
    /** List item layout clickable view area divider resource ID */
    private int mClickDividerId = -1;
    /** View click listener */
    private OnClickListener mViewClickListener = null;

    /**
     * Constructs using minimum number of parameters.
     *
     * @param context The context where the ListView associated with this adapter is running
     * @param layout Resource identifier of a layout file that defines the views
     *               for this list item.  Unless you override them later, this will
     *               define both the item views and the drop down views.
     * @param cursor The cursor from which to get the data.
     * @param labelId Resource identifier of main label text view; negative value to disable
     * @param labelCol Cursor column name for main label; null displays nothing
     */
    public CustomCursorAdapter(final Context context, final int layout, final Cursor cursor,
            final int labelId, final String labelCol) {
        this(context, layout, cursor, 0, labelId, labelCol, -1, null, -1, null, null, -1, -1);
    }

    /**
     * Constructs using all parameters.
     *
     * @param context The context where the ListView associated with this adapter is running
     * @param layout Resource identifier of a layout file that defines the views
     *               for this list item.  Unless you override them later, this will
     *               define both the item views and the drop down views.
     * @param cursor The cursor from which to get the data.
     * @param flags Flags used to determine the behavior of the adapter,
     *              as per {@link CursorAdapter#CursorAdapter(Context, Cursor, int)}.
     * @param labelId Resource identifier of main label text view; negative value to disable
     * @param labelCol Cursor column name for main label; null displays nothing
     * @param descId Resource identifier of description text view; negative value to ignore
     * @param descCol Cursor column name for description; null displays nothing
     * @param iconId Resource identifier of icon image view; negative value to ignore
     * @param iconCol Cursor column name for icon Uri; null displays nothing
     * @param viewClickListener Clickable view click listener; null to disable
     * @param clickViewId Resource identifier of clickable view; negative value to ignore
     * @param clickViewDividerId Resource identifier of clickable view divider; negative value to ignore
     */
    public CustomCursorAdapter(final Context context, final int layout, final Cursor cursor,
            final int flags, final int labelId, final String labelCol,
            final int descId, final String descCol, final int iconId, final String iconCol,
            final OnClickListener viewOnClickListener,
            final int clickViewId, final int clickViewDividerId) {
        super(context, layout, cursor, flags);
        mLabelId = labelId;
        mLabelCol = labelCol;
        mDescId = descId;
        mDescCol = descCol;
        mIconId = iconId;
        mIconCol = iconCol;
        mClickViewId = clickViewId;
        mClickDividerId = clickViewDividerId;
        mViewClickListener = viewOnClickListener;
    }

    /**
     * Populates list item views with data from current cursor row position.
     *
     * @param view View to populate with data
     * @param context Context for operations
     * @param cursor Cursor row containing data to populate into the view
     * @see android.widget.CursorAdapter#bindView(android.view.View, android.content.Context, android.database.Cursor)
     */
    @Override
    public void bindView(final View view, final Context context, final Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.label = (TextView) view.findViewById(mLabelId);
            holder.desc = (TextView) view.findViewById(mDescId);
            holder.icon = (ImageView) view.findViewById(mIconId);
            holder.clickArea = view.findViewById(mClickViewId);
            holder.clickDivider = view.findViewById(mClickDividerId);
            view.setTag(holder);
        }

        // Populate main item label text view, if requested
        if (mLabelId >= 0) {
            if (mLabelCol != null) {
                holder.label.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndexOrThrow(mLabelCol))));
                holder.label.setVisibility(View.VISIBLE);
            } else {
                holder.label.setVisibility(View.GONE);
            }
        }

        // Populate item description text view, if requested
        if (mDescId >= 0) {
            if (mDescCol != null) {
                holder.desc.setText(Html.fromHtml(cursor.getString(cursor.getColumnIndexOrThrow(mDescCol))));
                holder.desc.setVisibility(View.VISIBLE);
            } else {
                holder.desc.setVisibility(View.GONE);
            }
        }

        // Populate item icon image view, if requested
        if (mIconId >= 0) {
            if (mIconCol != null) {
                holder.icon.setVisibility(View.VISIBLE);
                final String uriString = cursor.getString(cursor.getColumnIndexOrThrow(mIconCol));
                Uri imageUri = null;
                try {
                    imageUri = Uri.parse(uriString);
                    holder.icon.setImageURI(imageUri);
                } catch (NullPointerException e) {
                    Log.w(TAG, "Null icon image Uri for item " + cursor.getPosition());
                    holder.icon.setVisibility(View.INVISIBLE);
                }
            } else {
                holder.icon.setVisibility(View.GONE);
            }
        }

        // Set text area click listener, if supplied
        if (mViewClickListener != null) {
            holder.clickArea.setClickable(true);
            holder.clickArea.setOnClickListener(mViewClickListener);
            holder.clickArea.setVisibility(View.VISIBLE);
            holder.clickDivider.setVisibility(View.VISIBLE);
        } else {
            holder.clickArea.setClickable(false);
            holder.clickDivider.setVisibility(View.GONE);
        }
    }

    /**
     * Class used as a convenient view tag.
     */
    class ViewHolder {
        public TextView label;
        public TextView desc;
        public ImageView icon;
        public View clickArea;
        public View clickDivider;
    }
}
