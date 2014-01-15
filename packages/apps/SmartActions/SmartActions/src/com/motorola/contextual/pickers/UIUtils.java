/**
 * Copyright (C) 2012, Motorola, Inc,
 * All Rights Reserved
 * Class name: UIUtils.java
 * Description: UI-related utility class
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * May 13, 2012	  MXDN83       Created file
 **********************************************************
 */
package com.motorola.contextual.pickers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.widget.TextView;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * Collection of UI related methods & classes
 *
 */
public class UIUtils {

    /**
     * Get the list of resolved apps as list item objects based on specify action.
     * @param intentAction - intent action
     * @param category - intent category like CATEGORY_LAUNCHER for launcher apps
     * @param ctx - host activity
     * @param excludePkgs - apps/pkgs to exclude (optional)
     * @param listType list item type, with radio button or without
     * @return ListItem[] or NULL if package name is not exist.
     */
    public static ListItem[] getResolvedIntentListItems(String intentAction, String category,
            Context ctx, String[] excludePkgs, int listType) {
        Intent baseIntent = new Intent(intentAction, null);
        if(category != null) {
            baseIntent.addCategory(category);
        }
        PackageManager pm = ctx.getPackageManager();
        List<ResolveInfo> applications = pm.queryIntentActivities(baseIntent, 0);
        //begin to store the activity label to a hashmap, so we don't need to
        //load label from package manager every time, to improve the performance.
        list_map = new HashMap<ResolveInfo,CharSequence>();
        for (int i=0; i<applications.size(); i++) {
            ResolveInfo ri = applications.get(i);
            if(excludePkgs != null) {
                //see if this package exist in the list of excluded packages list
                //if it does then don't include in the UI list items
                if(doesStringExist(excludePkgs,
                        applications.get(i).activityInfo.applicationInfo.packageName)) {
                    applications.remove(i--);
                    continue;
                }
            }
            CharSequence sq = ri.loadLabel(pm);
            if (sq == null) sq = ri.activityInfo.name;
            list_map.put(ri, sq);
        }
        //sort the list in alphabetical order
        Collections.sort(applications, new DisplayNameComparator());
        //Build the array of ListItems to be passed onto the adapter
        ListItem[] items = new ListItem[applications.size()];
        Intent intent;
        IconResizer reSizer = UIUtils.getResizer(ctx);
        for(int i=0; i<applications.size(); i++) {
            ResolveInfo ri = applications.get(i);
           //build the intent with app's package name & class name params
            intent = new Intent(baseIntent);
            intent.setClassName(ri.activityInfo.applicationInfo.packageName,
                    ri.activityInfo.name);
            items[i] = new ListItem(reSizer.createIconThumbnail(ri.loadIcon(pm)),
                    ri.loadLabel(pm), null, listType, intent, null);
        }
        return items;

    }

    /**
     * Utility class to find if a string is present in a string array
     * used to find if a package name exist in a list of packages to be shown etc
     */
    public static boolean doesStringExist(String[] excludePkgs, String pkg) {
        boolean found = false;
        for(String s:excludePkgs) {
            if(s.equals(pkg)) {
                found = true;
                break;
            }
        }
        return found;
    }

    protected static HashMap<ResolveInfo,CharSequence> list_map = null;
    /**
     * Application package display name comparator, used to sort list of
     * apps that are resolved to pick up an intent
     */
    public static class DisplayNameComparator implements Comparator<ResolveInfo> {
        public DisplayNameComparator() {
        }

        public final int compare(ResolveInfo a, ResolveInfo b) {
          return sCollator.compare(list_map.get(a).toString(),
                    list_map.get(b).toString());
        }
        private final Collator sCollator = Collator.getInstance();
    }

    public static IconResizer getResizer(Context context) {
        final Resources resources = context.getResources();
        int size = (int) resources.getDimension(android.R.dimen.app_icon_size);
        return new IconResizer(size, size, resources.getDisplayMetrics());
    }

    /**
     * Utility class to resize icons to match default icon size. Code is mostly
     * borrowed from Launcher. Code borrowed from system App picker
     */
    public static class IconResizer {
        private final int mIconWidth;
        private final int mIconHeight;

        private final DisplayMetrics mMetrics;
        private final Rect mOldBounds = new Rect();
        private final Canvas mCanvas = new Canvas();

        public IconResizer(int width, int height, DisplayMetrics metrics) {
            mCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
                    Paint.FILTER_BITMAP_FLAG));

            mMetrics = metrics;
            mIconWidth = width;
            mIconHeight = height;
        }

        /**
         * Returns a Drawable representing the thumbnail of the specified Drawable.
         * The size of the thumbnail is defined by the dimension
         * android.R.dimen.launcher_application_icon_size.
         *
         * This method is not thread-safe and should be invoked on the UI thread only.
         *
         * @param icon The icon to get a thumbnail of.
         *
         * @return A thumbnail for the specified icon or the icon itself if the
         *         thumbnail could not be created.
         */
        public Drawable createIconThumbnail(Drawable icon) {
            int width = mIconWidth;
            int height = mIconHeight;

            if (icon == null) {
                return new EmptyDrawable(width, height);
            }

            try {
                if (icon instanceof PaintDrawable) {
                    PaintDrawable painter = (PaintDrawable) icon;
                    painter.setIntrinsicWidth(width);
                    painter.setIntrinsicHeight(height);
                } else if (icon instanceof BitmapDrawable) {
                    // Ensure the bitmap has a density.
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    if (bitmap.getDensity() == Bitmap.DENSITY_NONE) {
                        bitmapDrawable.setTargetDensity(mMetrics);
                    }
                }
                int iconWidth = icon.getIntrinsicWidth();
                int iconHeight = icon.getIntrinsicHeight();

                if (iconWidth > 0 && iconHeight > 0) {
                    if (width < iconWidth || height < iconHeight) {
                        final float ratio = (float) iconWidth / iconHeight;

                        if (iconWidth > iconHeight) {
                            height = (int) (width / ratio);
                        } else if (iconHeight > iconWidth) {
                            width = (int) (height * ratio);
                        }

                        final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ?
                                    Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                        final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                        final Canvas canvas = mCanvas;
                        canvas.setBitmap(thumb);
                        // Copy the old bounds to restore them later
                        // If we were to do oldBounds = icon.getBounds(),
                        // the call to setBounds() that follows would
                        // change the same instance and we would lose the
                        // old bounds
                        mOldBounds.set(icon.getBounds());
                        final int x = (mIconWidth - width) / 2;
                        final int y = (mIconHeight - height) / 2;
                        icon.setBounds(x, y, x + width, y + height);
                        icon.draw(canvas);
                        icon.setBounds(mOldBounds);
                        //noinspection deprecation
                        icon = new BitmapDrawable(thumb);
                        ((BitmapDrawable) icon).setTargetDensity(mMetrics);
                        canvas.setBitmap(null);
                    } else if (iconWidth < width && iconHeight < height) {
                        final Bitmap.Config c = Bitmap.Config.ARGB_8888;
                        final Bitmap thumb = Bitmap.createBitmap(mIconWidth, mIconHeight, c);
                        final Canvas canvas = mCanvas;
                        canvas.setBitmap(thumb);
                        mOldBounds.set(icon.getBounds());
                        final int x = (width - iconWidth) / 2;
                        final int y = (height - iconHeight) / 2;
                        icon.setBounds(x, y, x + iconWidth, y + iconHeight);
                        icon.draw(canvas);
                        icon.setBounds(mOldBounds);
                        //noinspection deprecation
                        icon = new BitmapDrawable(thumb);
                        ((BitmapDrawable) icon).setTargetDensity(mMetrics);
                        canvas.setBitmap(null);
                    }
                }

            } catch (Throwable t) {
                icon = new EmptyDrawable(width, height);
            }

            return icon;
        }
    }

    private static class EmptyDrawable extends Drawable {
        private final int mWidth;
        private final int mHeight;

        EmptyDrawable(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        @Override
        public int getIntrinsicWidth() {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mHeight;
        }

        @Override
        public int getMinimumWidth() {
            return mWidth;
        }

        @Override
        public int getMinimumHeight() {
            return mHeight;
        }

        @Override
        public void draw(Canvas canvas) {
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    /**
     * Force-hide the virtual keyboard.
     *
     * @param activity - the host activity of the text view.
     * @param view - the text view where the keyboard is attached to.
     */
    public static void hideKeyboard(Activity activity, TextView view) {
		InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    	imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * Hides soft input keyboard, if shown.
     *
     * @param context Context for input method service
     * @param view View to check for active soft input method
     */
    public static void hideSoftInputKeyboard(final Context context, final View view) {
        if ((context != null) && (view != null)) {
            final InputMethodManager inputMethodMan = (InputMethodManager)
                    context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputMethodMan.isActive(view)) {
                inputMethodMan.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

}
