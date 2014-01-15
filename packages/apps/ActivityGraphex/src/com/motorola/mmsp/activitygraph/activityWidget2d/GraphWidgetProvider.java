package com.motorola.mmsp.activitygraph.activityWidget2d;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.motorola.mmsp.activitygraph.R;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

public abstract class GraphWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "gxl/GraphWidgetProvider";

    /**
     * Icon's data structure
     */
    public static class Item {
        public int id;
        public String AppName;
        public Bitmap skin; // can null , if null, it will be cleared.
        public Bitmap cover; // can null , if null, it will be cleared.
        public Bitmap textbar;// can null , if null, it will be cleared.
        public String text; // can null , if null, it will show "".
        String intentStr;
        public int position; // 0-8 or 0-6
    }

    private static final int[] ITEM_COVER_ID = { R.id.iv_cover0,
            R.id.iv_cover1, R.id.iv_cover2, R.id.iv_cover3, R.id.iv_cover4,
            R.id.iv_cover5, R.id.iv_cover6, R.id.iv_cover7, R.id.iv_cover8, };

    private static final int[] ITEM_TEXT_ID = { R.id.tv_text0, R.id.tv_text1,
            R.id.tv_text2, R.id.tv_text3, R.id.tv_text4, R.id.tv_text5,
            R.id.tv_text6, R.id.tv_text7, R.id.tv_text8, };

    private static final int[] ITEM_IMGBTN_ID = { R.id.ib0, R.id.ib1, R.id.ib2,
            R.id.ib3, R.id.ib4, R.id.ib5, R.id.ib6, R.id.ib7, R.id.ib8,
            R.id.ib9, R.id.ib10 };

    private static Uri[] mUri_array = new Uri[9];

    private static final String[] ITEM_ACTION_NAME = {
            "com.motorola.mmsp.activitywidget2d.action.click0",
            "com.motorola.mmsp.activitywidget2d.action.click1",
            "com.motorola.mmsp.activitywidget2d.action.click2",
            "com.motorola.mmsp.activitywidget2d.action.click3",
            "com.motorola.mmsp.activitywidget2d.action.click4",
            "com.motorola.mmsp.activitywidget2d.action.click5",
            "com.motorola.mmsp.activitywidget2d.action.click6",
            "com.motorola.mmsp.activitywidget2d.action.click7",
            "com.motorola.mmsp.activitywidget2d.action.click8",
            "com.motorola.mmsp.activitywidget2d.action.click9",
            "com.motorola.mmsp.activitywidget2d.action.click10" };

    private static int L_SIZE_MDPI = 119;
    private static int M_SIZE_MDPI = 91;
    private static int S_SIZE_MDPI = 67;
    private static int BAR_HEIGHT_MDPI = 20;

    private int L_SIZE = L_SIZE_MDPI;
    private int M_SIZE = M_SIZE_MDPI;
    private int S_SIZE = S_SIZE_MDPI;
    private int BAR_HEIGHT = BAR_HEIGHT_MDPI;

    private float density = 1.0f;

    private static int ITEMS_COUNT = 9;

    private int getItemsCount() {
        return ITEMS_COUNT;
    }

    private int getPlayButtonIndex() {
        return 9;
    }

    private int getSettingButtonIndex() {
        return 10;
    }

    protected int getMainLayoutId() {
        return R.layout.main_layout;
    }

    static Bitmap clear;

    static {
        clear = Bitmap.createBitmap(180, 180, Config.ARGB_8888);
        clear.eraseColor(Color.TRANSPARENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);

        String action = intent.getAction();

        if (action.equals(ITEM_ACTION_NAME[getPlayButtonIndex()])) {
            onPlayClick(context);
            return;
        }

        if (action.equals(ITEM_ACTION_NAME[getSettingButtonIndex()])) {
            onSettingClick(context);
            return;
        }

        for (int i = 0; i < getItemsCount(); i++) {
            if (action.equals(ITEM_ACTION_NAME[i])) {
                onItemClick(context, i);
                return;
            }
        }

    }

    private void updateDensity(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        Log.d(TAG, "density = " + dm.density);
        if (density != dm.density) {
            density = dm.density;
            L_SIZE = (int) (L_SIZE_MDPI * dm.density);
            M_SIZE = (int) (M_SIZE_MDPI * dm.density);
            S_SIZE = (int) (S_SIZE_MDPI * dm.density);
            BAR_HEIGHT = (int) (BAR_HEIGHT_MDPI * dm.density);
        }
    }

    private Bitmap createBitmap(String name, Bitmap src, int dstWidth,
            int dstHeight) {

        if (src == null) {
            Log.d(TAG, "createBitmap" + name + " is null.");
            return null;
        }

        if (src.isRecycled()) {
            Log.d(TAG, "createBitmap" + name + " is recycled");
            return null;
        }

        return Bitmap.createScaledBitmap(src, dstWidth, dstHeight, true);
    }

    private void drawBitmap(Canvas canvas, String name, Bitmap src, float left,
            float top) {

        if (src == null) {
            Log.d(TAG, "drawBitmap" + name + " is null.");
            return;
        }

        if (src.isRecycled()) {
            Log.d(TAG, "drawBitmap" + name + " is recycled");
            return;
        }

        canvas.drawBitmap(src, left, top, null);
    }

    private Uri mergeBitmap(Context context, Bitmap bmp1, Bitmap bmp2,
            Bitmap bmp3, int position) {

        Log.i(TAG, "----mergeBitmap----");
        int size = 0;
        int bar_height = 0;

        if (position == 0) {
            size = L_SIZE;
        }
        // else if (position == 1 || position == 3
        // || (position == 2 && density == 1.5f)) {
        // size = M_SIZE;
        else if (position == 1 || position == 3 || position == 2) {
            size = M_SIZE;
        } else {
            size = S_SIZE;
        }
        bar_height = BAR_HEIGHT;

        Bitmap bmp = createBitmap("bmp1", bmp1.copy(Config.ARGB_8888,true), size, size);
        if (bmp == null) {
            if (clear.isRecycled()) {
                bmp = Bitmap.createBitmap(size, size, Config.ARGB_8888);
                bmp.eraseColor(Color.TRANSPARENT);
            } else {
                bmp = Bitmap.createScaledBitmap(clear, size, size, true).copy(Bitmap.Config.ARGB_8888, true);
            }
        }
        Canvas canvas = new Canvas(bmp);
      //2012-09-13 Modifyed by amt_chenjing begin    
        if (bmp2 != null) {
            if (bmp2.isRecycled()) {
                Log.d(TAG, "bmp2 is recycled.");
            } else {
                float bmp2_width = bmp2.getWidth();
                float bmp2_height = bmp2.getHeight();
                if ((bmp2_height <= size) && (bmp2_width <= size)) {
                    drawBitmap(canvas, "bmp2", bmp2, (size - bmp2_width) / 2,
                            (size - bmp2_height) / 2);
                } else {
                	Bitmap bmp_a = createMiniThumb( bmp2, size -1, size -1, false);
					drawBitmap(canvas, "bmp_a", bmp_a, 0, 0);
                    if (bmp_a != null) {
                        bmp_a.recycle();
                    }
                }
            }
        }
      //2012-09-13 Modifyed by amt_chenjing end
        Bitmap bmp_c = createBitmap("bmp3", bmp3, size, bar_height);
        drawBitmap(canvas, "bmp_c", bmp_c, 0, size - bar_height);
        Uri uri = null;
        try {
            uri = saveMyBitmap(context, "card_" + position, bmp);
            mUri_array[position] = uri;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bmp_c != null) {
            bmp_c.recycle();
        } else {
            Log.i(TAG, "bmp_c is null");
        }

        if (bmp != null) {
            bmp.recycle();
        }
        return uri;
    }
  //2012-09-13 Added by amt_chenjing begin
    public static Bitmap createMiniThumb(Bitmap source, int w, int h,
            boolean recycle) {
        if (source == null) {
            return null;
        }
        float scale;
        if (source.getWidth() < source.getHeight()) {
            scale = w / (float) source.getWidth();  
        } else {
            scale = h / (float) source.getHeight();
        }
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        Bitmap miniThumbnail = transform(matrix, source, w, h, true,
                recycle);
        return miniThumbnail;
    }
    public static Bitmap transform(Matrix scaler, Bitmap source,
            int targetWidth, int targetHeight, boolean scaleUp, boolean recycle) {
        int deltaX = source.getWidth() - targetWidth;
        int deltaY = source.getHeight() - targetHeight;
        if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
            Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight,
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b2);
            int deltaXHalf = Math.max(0, deltaX / 2);
            int deltaYHalf = Math.max(0, deltaY / 2);
            Rect src = new Rect(deltaXHalf, deltaYHalf, deltaXHalf
                    + Math.min(targetWidth, source.getWidth()), deltaYHalf
                    + Math.min(targetHeight, source.getHeight()));
            int dstX = (targetWidth - src.width()) / 2;
            int dstY = (targetHeight - src.height()) / 2;
            Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight
                    - dstY);
            c.drawBitmap(source, src, dst, null);
            if (recycle) {
                source.recycle();
            }
            return b2;
        }
        float bitmapWidthF = source.getWidth();
        float bitmapHeightF = source.getHeight();
        float bitmapAspect = bitmapWidthF / bitmapHeightF;
        float viewAspect = (float) targetWidth / targetHeight;
        if (bitmapAspect > viewAspect) {
            float scale = targetHeight / bitmapHeightF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        } else {
            float scale = targetWidth / bitmapWidthF;
            if (scale < .9F || scale > 1F) {
                scaler.setScale(scale, scale);
            } else {
                scaler = null;
            }
        }
        Bitmap b1;
        if (scaler != null) {
            b1 = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source
                    .getHeight(), scaler, true);
        } else {
            b1 = source;
        }
        if (recycle && b1 != source) {
            source.recycle();
        }
        int dx1 = Math.max(0, b1.getWidth() - targetWidth);
        int dy1 = Math.max(0, b1.getHeight() - targetHeight);
        Bitmap b2 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth,
                targetHeight);
        if (b2 != b1) {
            if (recycle || b1 != source) {
                b1.recycle();
            }
        }
        return b2;
    }
  //2012-09-13, Add end
    public Uri saveMyBitmap(Context context, String bitName, Bitmap bmp)
            throws IOException {
        File fir = context.getFilesDir();
        Log.i(TAG, "------dir path = " + fir.getPath());

        if (!fir.exists()) {
            fir.mkdir();
        }
        String fileName = bitName + ".png";
        FileOutputStream fOut = null;
        File file = new File(fir, fileName);
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        }

        try {
            fOut = context.openFileOutput(fileName, Context.MODE_WORLD_READABLE
                    + Context.MODE_WORLD_WRITEABLE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);

        try {
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Uri.fromFile(file);
    }

    private RemoteViews createRemoteViewsAll(RemoteViews main, Context context,
            ArrayList<Item> items) {
        Log.i(TAG, "------createRemoteViewAll-----01");
        Uri uri = null;
        if (main == null) {
            main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < items.size(); i++) {
            int position = items.get(i).position;
            Log.i(TAG, "------createRemoteViewAll-----position: " + position);
            Bitmap bmp = null;
            if (items.get(i).text != null && items.get(i).text != "") {
                uri = mergeBitmap(context, items.get(i).skin,
                        items.get(i).cover, items.get(i).textbar, position);
            } else {
                uri = mergeBitmap(context, items.get(i).skin,
                        items.get(i).cover, null, position);
            }

            Log.i(TAG, "--------createRemoteViewAll----uri : " + uri.toString());
            // main.setBitmap(ITEM_COVER_ID[position], "setImageBitmap", bmp);
            main.setImageViewUri(ITEM_COVER_ID[position], Uri.parse(""));

            main.setImageViewUri(ITEM_COVER_ID[position], uri);

            main.setTextViewText(ITEM_TEXT_ID[position], items.get(i).text);
        }
        long end = System.currentTimeMillis();
        Log.i(TAG, "mergebitmap cost time : " + (end - start));

        // main.setViewVisibility(R.id.border, View.VISIBLE);
        main.setViewVisibility(R.id.shadow, View.VISIBLE);
        main.setViewVisibility(R.id.r_setting, View.VISIBLE);

        return main;
    }
    
    /**
     * update specific items of widget.
     * @param main
     * @param context
     * @param items
     * @param positions     the specific positions
     * @return
     */
    private RemoteViews createRemoteViewsAll(RemoteViews main, Context context,
            ArrayList<Item> items, int[] positions) {
        Log.i(TAG, "------createRemoteViewAll-----02");
        Uri uri = null;
        if (main == null) {
            main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        long start = System.currentTimeMillis();

        if (positions == null) {
            positions = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
        }
        Log.i(TAG, "----items.size  = " + items.size());
        Log.i(TAG, "----positions.length = " + positions.length);

        for (int i = 0; i < items.size(); i++) {
            int position = items.get(i).position;

            boolean reUpdate = false;

            for (int j = 0; j < positions.length; j++) {
                if (position == positions[j]) {
                    reUpdate = true;
                    break;
                }
            }
            if (reUpdate) {
                Log.i(TAG, "------createRemoteViewAll-----position: "
                        + position);
                if (items.get(i).text != null && items.get(i).text != "") {
                    uri = mergeBitmap(context, items.get(i).skin,
                            items.get(i).cover, items.get(i).textbar, position);
                } else {
                    uri = mergeBitmap(context, items.get(i).skin,
                            items.get(i).cover, null, position);
                }
                Log.i(TAG,
                        "--------createRemoteViewAll----uri : "
                                + uri.toString());
                // main.setBitmap(ITEM_COVER_ID[position], "setImageBitmap",
                // bmp);
                main.setImageViewUri(ITEM_COVER_ID[position], Uri.parse(""));

                main.setImageViewUri(ITEM_COVER_ID[position], uri);
            } else {
                main.setImageViewUri(ITEM_COVER_ID[position],
                        mUri_array[position]);
            }

            main.setTextViewText(ITEM_TEXT_ID[position], items.get(i).text);
        }
        long end = System.currentTimeMillis();
        Log.i(TAG, "mergebitmap cost time : " + (end - start));

        // main.setViewVisibility(R.id.border, View.VISIBLE);
        main.setViewVisibility(R.id.shadow, View.VISIBLE);
        main.setViewVisibility(R.id.r_setting, View.VISIBLE);

        return main;
    }


    private RemoteViews createRemoteViewsOne(Context context, Item item) {

        RemoteViews main = new RemoteViews(context.getPackageName(),
                getMainLayoutId());

        Uri uri = null;
        Bitmap bmp = null;
        if (item.text != null && item.text != "") {
            uri = mergeBitmap(context, item.skin, item.cover, item.textbar,
                    item.position);
        } else {
            uri = mergeBitmap(context, item.skin, item.cover, null,
                    item.position);
        }
        // main.setBitmap(ITEM_COVER_ID[item.position], "setImageBitmap", bmp);
        main.setImageViewUri(ITEM_COVER_ID[item.position], Uri.parse(""));
        main.setImageViewUri(ITEM_COVER_ID[item.position], uri);

        main.setTextViewText(ITEM_TEXT_ID[item.position], item.text);

        return main;
    }

    public void init(RemoteViews main, Context context,
            AppWidgetManager appWidgeManger, ComponentName cn) {
        if (main == null) {
            main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        for (int i = 0; i < getItemsCount(); i++) {
            Intent intent = new Intent(ITEM_ACTION_NAME[i]);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    0, intent, 0);
            main.setOnClickPendingIntent(ITEM_IMGBTN_ID[i], pendingIntent);
        }

        Intent intent = new Intent(ITEM_ACTION_NAME[getPlayButtonIndex()]);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
                intent, 0);
        main.setOnClickPendingIntent(ITEM_IMGBTN_ID[getPlayButtonIndex()],
                pendingIntent);

        Intent intent1 = new Intent(ITEM_ACTION_NAME[getSettingButtonIndex()]);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context, 0,
                intent1, 0);
        main.setOnClickPendingIntent(ITEM_IMGBTN_ID[getSettingButtonIndex()],
                pendingIntent1);

        // appWidgeManger.updateAppWidget(cn, main);
    }

    /**
     * Update all the icons.
     * 
     * @param context
     * @param cn
     *            ComponentName
     * @param items
     *            The items contain the icons information.
     */
    public void updateAll(RemoteViews main, Context context, ComponentName cn,
            ArrayList<Item> items) {

        updateDensity(context);

        Log.i(TAG, "updateAll----componentName = " + cn);
        createRemoteViewsAll(main, context, items);
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        appWidgetManager.updateAppWidget(cn, main);

        Log.i(TAG, "updateAll------end");
    }

    /**
     * update specific cards of widget
     * @param main
     * @param context
     * @param cn
     * @param items
     *              The items contain the icons information.
     * @param positions
     *              The specific positions to update
     */
    public void updateAll(RemoteViews main, Context context, ComponentName cn,
            ArrayList<Item> items, int[] positions) {

        updateDensity(context);

        Log.i(TAG, "updateAll----componentName = " + cn + ", positions = " + positions);
        createRemoteViewsAll(main, context, items, positions);
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        appWidgetManager.updateAppWidget(cn, main);

        Log.i(TAG, "updateAll------end");
    }
    /**
     * Update one icon.
     * 
     * @param context
     * @param cn
     * @param item
     *            The item contains the icon information.
     */
    public void updateOne(Context context, ComponentName cn, Item item) {

        RemoteViews main = createRemoteViewsOne(context, item);
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        appWidgetManager.updateAppWidget(cn, main);
    }

    /**
     * Update one icon.
     * 
     * @param context
     * @param cn
     * @param item
     *            The item contains the icon information.
     */
    public void updateOne(Context context, int appWidgetId, Item item) {

        RemoteViews main = createRemoteViewsOne(context, item);
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        appWidgetManager.updateAppWidget(appWidgetId, main);
    }

    /**
     * Show the play button.
     * 
     * @param context
     * @param cn
     * @param show
     *            if true show the button , if false hide the button.
     */
    public void showPlayButton(RemoteViews main, Context context,
            ComponentName cn, boolean show) {
        if (main == null) {
            main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        if (show) {
            main.setInt(ITEM_IMGBTN_ID[getPlayButtonIndex()], "setVisibility",
                    View.VISIBLE);
        } else {
            main.setInt(ITEM_IMGBTN_ID[getPlayButtonIndex()], "setVisibility",
                    View.INVISIBLE);
        }
        AppWidgetManager appWidgetManager = AppWidgetManager
                .getInstance(context);
        appWidgetManager.updateAppWidget(cn, main);
    }

    /**
     * Show the play button.
     * 
     * @param context
     * @param show
     *            if true show the button , if false hide the button.
     */
    public void showPlayButton(RemoteViews main, Context context, boolean show) {
        if (main == null) {
            main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        if (show) {
            main.setInt(ITEM_IMGBTN_ID[getPlayButtonIndex()], "setVisibility",
                    View.VISIBLE);
        } else {
            main.setInt(ITEM_IMGBTN_ID[getPlayButtonIndex()], "setVisibility",
                    View.INVISIBLE);
        }
        // AppWidgetManager appWidgetManager = AppWidgetManager
        // .getInstance(context);
        // appWidgetManager.updateAppWidget(appWidgetId, main);
    }

    /**
     * Show the setting button.
     * 
     * @param context
     * @param cn
     * @param show
     *            if true show the button , if false hide the button.
     */
    // public void showSettingButton(Context context, ComponentName cn,
    // boolean show) {

    // RemoteViews main = new RemoteViews(context.getPackageName(),
    // getMainLayoutId());
    // if (show) {
    // main.setInt(ITEM_IMGBTN_ID[getSettingButtonIndex()],
    // "setVisibility", View.VISIBLE);
    // } else {
    // main.setInt(ITEM_IMGBTN_ID[getSettingButtonIndex()],
    // "setVisibility", View.INVISIBLE);
    // }
    // AppWidgetManager appWidgetManager = AppWidgetManager
    // .getInstance(context);
    // appWidgetManager.updateAppWidget(cn, main);
    // }

    /**
     * Show the setting button.
     * 
     * @param context
     * @param cn
     * @param show
     *            if true show the button , if false hide the button.
     */
    // public void showSettingButton(Context context, int appWidgetId, boolean
    // show) {

    // RemoteViews main = new RemoteViews(context.getPackageName(),
    // getMainLayoutId());
    // if (show) {
    // main.setInt(ITEM_IMGBTN_ID[getSettingButtonIndex()],
    // "setVisibility", View.VISIBLE);
    // } else {
    // main.setInt(ITEM_IMGBTN_ID[getSettingButtonIndex()],
    // "setVisibility", View.INVISIBLE);
    // }
    // AppWidgetManager appWidgetManager = AppWidgetManager
    // .getInstance(context);
    // appWidgetManager.updateAppWidget(appWidgetId, main);
    // }

    /**
     * Called when one of the icons is clicked.
     * 
     * @param context
     * @param rank
     *            The position of the icon.
     */
    abstract public void onItemClick(Context context, int position);

    /**
     * Called when play button is clicked.
     * 
     * @param context
     * @param position
     *            The position of the icon.
     */
    abstract public void onPlayClick(Context context);

    /**
     * Called when setting button is clicked.
     * 
     * @param context
     * @param position
     *            The position of the icon.
     */
    abstract public void onSettingClick(Context context);
}
