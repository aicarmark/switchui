package com.motorola.mmsp.socialGraph.socialwidget2D;

import java.util.ArrayList;

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
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.motorola.mmsp.socialGraph.R;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class GraphWidgetProvider extends AppWidgetProvider {

    private static final String Tag = "GraphWidgetProvider";

    /**
     * Icon's data structure
     */
    public static class Item {
        public int id;
        public Bitmap skin;   // can null , if null, it will be cleared.
        public Bitmap cover;  // can null , if null, it will be cleared.
        public Bitmap textbar;// can null , if null, it will be cleared.
        public String text;   // can null , if null, it will show "".
        public int position;  // 0-8 or 0-6
    }

    private static final int [] ITEM_COVER_ID = {
        R.id.iv_cover0,
        R.id.iv_cover1,
        R.id.iv_cover2,
        R.id.iv_cover3,
        R.id.iv_cover4,
        R.id.iv_cover5,
        R.id.iv_cover6,
        R.id.iv_cover7,
        R.id.iv_cover8,
    };

    private static final int [] ITEM_TEXT_ID = {
        R.id.tv_text0,
        R.id.tv_text1,
        R.id.tv_text2,
        R.id.tv_text3,
        R.id.tv_text4,
        R.id.tv_text5,
        R.id.tv_text6,
        R.id.tv_text7,
        R.id.tv_text8,
    };

    private static final int [] ITEM_IMGBTN_ID = {
        R.id.ib0,
        R.id.ib1,
        R.id.ib2,
        R.id.ib3,
        R.id.ib4,
        R.id.ib5,
        R.id.ib6,
        R.id.ib7,
        R.id.ib8,
        R.id.ib9,
        R.id.ib10
    };

    private static final String [] ITEM_ACTION_NAME = {
        "com.motorola.mmsp.action.click0",
        "com.motorola.mmsp.action.click1",
        "com.motorola.mmsp.action.click2",
        "com.motorola.mmsp.action.click3",
        "com.motorola.mmsp.action.click4",
        "com.motorola.mmsp.action.click5",
        "com.motorola.mmsp.action.click6",
        "com.motorola.mmsp.action.click7",
        "com.motorola.mmsp.action.click8",
        "com.motorola.mmsp.action.click9",
        "com.motorola.mmsp.action.click10"
    };

    private static int L_SIZE_MDPI = 119;
    private static int M_SIZE_MDPI = 91;
    private static int S_SIZE_MDPI = 67;
    private static int BAR_HEIGHT_MDPI = 20;

    private int L_SIZE = L_SIZE_MDPI;
    private int M_SIZE = M_SIZE_MDPI;
    private int S_SIZE = S_SIZE_MDPI;
    private int PADDING = 1;

    private int BAR_HEIGHT = BAR_HEIGHT_MDPI;
    
    private float density = 1.0f;
    private float mCoverSize = 0f;

    private static int ITEMS_COUNT = 9;

    private int getItemsCount(){
        return ITEMS_COUNT;
    }

    private int getPlayButtonIndex () {
        return 10;
    }

    private int getSettingButtonIndex () {
        return 9;
    }

    protected int getMainLayoutId(){
        return R.layout.main_layout;
    }

    static Bitmap clear;

    static {
        clear = Bitmap.createBitmap(180, 180, Config.ARGB_8888);
        //clear = Bitmap.createScaledBitmap(clear, L_SIZE, BAR_HEIGHT, true);
        clear.eraseColor(Color.TRANSPARENT);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        super.onReceive(context, intent);

        String action = intent.getAction();

        if(action.equals(ITEM_ACTION_NAME[getPlayButtonIndex()])){
            onPlayClick(context);
            return;
        }

        if(action.equals(ITEM_ACTION_NAME[getSettingButtonIndex()])){
            onSettingClick(context);
            return;
        }

        for (int i = 0; i < getItemsCount(); i++) {
            if(action.equals(ITEM_ACTION_NAME[i])){
                onItemClick(context, i);
                return;
            }
        }

    }

    private void updateDensity(Context context){
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        Log.d(Tag, "zc~density = " + dm.density);
        if ( density != dm.density) {
            density = dm.density;
            L_SIZE = (int) (L_SIZE_MDPI * dm.density);
            M_SIZE = (int) (M_SIZE_MDPI * dm.density);
            S_SIZE = (int) (S_SIZE_MDPI * dm.density);
            BAR_HEIGHT = (int) (BAR_HEIGHT_MDPI * dm.density);
            PADDING=(int) dm.density;
        }
    }

    private Bitmap createBitmap(String name, Bitmap src, int dstWidth, int dstHeight){
    	Log.d(Tag, "zc~createBitmap "+name );
        if ( src == null ) {
            Log.d(Tag, "zc~createBitmap" + name + " is null.");
            return null;
        }
        
        if ( src.isRecycled() ) {
            Log.d(Tag, "createBitmap" + name + " is recycled");
            return null;
        }
        // SWITCHUI-896 begin
        Bitmap resultBitmap = null;
		try {
			resultBitmap = Bitmap.createScaledBitmap(src, dstWidth, dstHeight,
					true);
		} catch (Exception e) {
			e.printStackTrace();
		}
        return resultBitmap;
        // SWITCHUI-896 end
    }

    private void drawBitmap(Canvas canvas, String name, Bitmap src, float left, float top){

        if ( src == null ) {
            Log.d(Tag, "drawBitmap" + name + " is null.");
            return;
        }
        
        if ( src.isRecycled() ) {
            Log.d(Tag, "drawBitmap" + name + " is recycled");
            return;
        }
		// SWITCHUI-896 begin
		try {
			canvas.drawBitmap(src, left, top, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// SWITCHUI-896 end
    }

    private Uri mergeBitmap(Context context, Bitmap bmp1, Bitmap bmp2,
            Bitmap bmp3, int position) {
        Log.d(Tag, "zc~mergeBitmap");
        int size = 0;
        int bar_height = 0;

        if ( position == 0 ) {
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

        Bitmap bmp = createBitmap("bmp1", bmp1.copy(Config.ARGB_8888, true), size, size);
        if ( bmp == null ) {
            if ( clear.isRecycled() ) {
                bmp = Bitmap.createBitmap(size, size, Config.ARGB_8888);
                bmp.eraseColor(Color.TRANSPARENT);
            } else {
				// SWITCHUI-896 begin
				try {
					bmp = Bitmap.createScaledBitmap(clear, size, size, true);
				} catch (Exception e) {
					e.printStackTrace();
					bmp = Bitmap.createBitmap(size, size, Config.ARGB_8888);
					bmp.eraseColor(Color.TRANSPARENT);
				}
				// SWITCHUI-896 end
            }
        }
        Canvas canvas = new Canvas(bmp);
       // bmp2=null;
        if ( bmp2 != null ){
            if ( bmp2.isRecycled() ){
                Log.d(Tag, "bmp2 is recycled.");
            } else {
                float bmp2_width = bmp2.getWidth();
                float bmp2_height = bmp2.getHeight();
                Bitmap bmp_a;
                if ((bmp2_height <= size-2*PADDING) && (bmp2_width <= size-2*PADDING)) {
                	   Log.d(Tag, "zc---bmp2 is recycled1.");
//                    drawBitmap(canvas, "bmp2", bmp2, (size - bmp2_width)/2,
//                    		(size - bmp2_height)/2);
                       bmp_a = createBitmap("bmp2", bmp2, (size - 20), (size - 20));
                       drawBitmap(canvas, "bmp_a", bmp_a, 10 * PADDING,
                             10 * PADDING);
                } else {
                	Log.d(Tag, "zc---bmp2 is recycled2.");
                    bmp_a = createBitmap("bmp2", bmp2, size-2*PADDING, size-2*PADDING);
                    drawBitmap(canvas, "bmp_a", bmp_a, PADDING, PADDING);
                }
            }
        }

        Bitmap bmp_c = createBitmap("bmp3", bmp3, size, bar_height);
        drawBitmap(canvas, "bmp_c", bmp_c, 0, size - bar_height);
        Uri uri = null;
        try {
            uri = saveMyBitmap(context, "card_" + position, bmp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (bmp_c != null) {
            bmp_c.recycle();
        } else {
            Log.d(Tag, "bmp_c is null");
        }

        if (bmp != null) {
            bmp.recycle();
        }
        return uri;
    }

    public Uri saveMyBitmap(Context context, String bitName, Bitmap bmp)
            throws IOException {
        File fir = context.getFilesDir();
        Log.d(Tag, "zc~saveMyBitmap dir path = " + fir.getPath());

        if (!fir.exists()) {
            fir.mkdir();
        }
        String fileName = bitName + ".png";
        FileOutputStream fOut = null;
        File file = null;

        try {
               file = new File(fir, fileName);
               if (file.exists()) {
                  file.delete();
                  file.createNewFile();
               }

               fOut = context.openFileOutput(fileName, Context.MODE_WORLD_READABLE
                    + Context.MODE_WORLD_WRITEABLE);
               bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
              if (fOut != null) {
                 fOut.flush();
                 fOut.close();
              }
        }

        return Uri.fromFile(file);
    }

    private RemoteViews createRemoteViewsAll(RemoteViews main, Context context, ArrayList<Item> items){
    	 Log.d(Tag, "zc~createRemoteViewsAll " );
        Uri uri = null;
        if(main == null){
    	    main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        for(int i = 0; i < items.size(); i++){
            int position = items.get(i).position;
            if ( items.get(i).text != null && items.get(i).text != "" ) {
                uri = mergeBitmap(context, items.get(i).skin,
                        items.get(i).cover, items.get(i).textbar, position);
            } else {
                uri = mergeBitmap(context, items.get(i).skin,
                        items.get(i).cover, null, position);
            }
            Log.d(Tag, "zc~createRemoteViewsAll position:"+position+" uri:"+uri+",text:"+items.get(i).text );
            main.setImageViewUri(ITEM_COVER_ID[position], Uri.parse(""));

            main.setImageViewUri(ITEM_COVER_ID[position], uri);

            main.setTextViewText(ITEM_TEXT_ID[position], items.get(i).text);
        }

        return main;
    }

    private RemoteViews createRemoteViewsOne(RemoteViews main, Context context, Item item){
        Uri uri = null;
    	if(main == null){
    	    main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        Bitmap bmp = null;
        if ( item.text != null && item.text != "" ) {
                uri = mergeBitmap(context, item.skin, item.cover, item.textbar,
                    item.position);
        } else {
                uri = mergeBitmap(context, item.skin, item.cover, null,
                     item.position);
        }
        main.setImageViewUri(ITEM_COVER_ID[item.position], Uri.parse(""));
        main.setImageViewUri(ITEM_COVER_ID[item.position], uri);

        main.setTextViewText(ITEM_TEXT_ID[item.position], item.text);

        return main;
    }

    protected RemoteViews init( Context context,AppWidgetManager appWidgeManger, ComponentName cn) {
    	 Log.d(Tag, "zc~createRemoteViewsAll " );

        RemoteViews main = new RemoteViews(context.getPackageName(), getMainLayoutId());

        for(int i = 0; i < getItemsCount(); i++){
            Intent intent = new Intent(ITEM_ACTION_NAME[i]);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent, 0);
            main.setOnClickPendingIntent(ITEM_IMGBTN_ID[i], pendingIntent);
        }

        Intent intent = new Intent(ITEM_ACTION_NAME[getPlayButtonIndex()]);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,0,intent, 0);
        main.setOnClickPendingIntent(ITEM_IMGBTN_ID[getPlayButtonIndex()], pendingIntent);

        Intent intent1 = new Intent(ITEM_ACTION_NAME[getSettingButtonIndex()]);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(context,0,intent1, 0);
        main.setOnClickPendingIntent(ITEM_IMGBTN_ID[getSettingButtonIndex()], pendingIntent1);
        return main;
        //appWidgeManger.updateAppWidget(cn, main);
    }

    /**
     * Update all the icons.
     *
     * @param context
     * @param appWidgeManger
     * @param appWidgetId
     * @param items The items contain the icons information.
     * @param coverSize is a percentage of the item size.
     */
    public void updateAll(RemoteViews main, Context context, AppWidgetManager appWidgeManger,
    		int appWidgetId, ArrayList<Item> items, float coverSize) {

    	mCoverSize = coverSize;
    	updateAll(main, context, appWidgeManger, appWidgetId, items);
    }

    /**
     * Update all the icons.
     *
     * @param context
     * @param appWidgeManger
     * @param appWidgetId
     * @param items The items contain the icons information.
     */
    public void updateAll(RemoteViews main, Context context, AppWidgetManager appWidgeManger,
    		int appWidgetId, ArrayList<Item> items) {

        updateDensity(context);

        createRemoteViewsAll(main, context, items);
        appWidgeManger.updateAppWidget(appWidgetId, main);
    }

    /**
     * Update all the icons.
     *
     * @param context
     * @param cn ComponentName
     * @param items The items contain the icons information.
     * @param coverSize is a percentage of the item size.
     */
    public void updateAll(RemoteViews main, Context context, ComponentName cn,
    		ArrayList<Item> items, float coverSize) {

        mCoverSize = coverSize;
        updateAll(main, context, cn, items);
    }

    /**
     * Update all the icons.
     *
     * @param context
     * @param cn ComponentName
     * @param items The items contain the icons information.
     */
    public void updateAll(RemoteViews main, Context context, ComponentName cn, ArrayList<Item> items) {

        updateDensity(context);

        createRemoteViewsAll(main, context, items);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(cn, main);
    }

    /**
     * Update one icon.
     *
     * @param context
     * @param cn
     * @param item The item contains the icon information.
     */
    public void updateOne(RemoteViews main, Context context, ComponentName cn, Item item){

        createRemoteViewsOne(main, context, item);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(cn, main);
    }

    /**
     * Update one icon.
     *
     * @param context
     * @param cn
     * @param item The item contains the icon information.
     */
    public void updateOne(RemoteViews main, Context context, int appWidgetId, Item item){

        createRemoteViewsOne(main, context, item);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context); 
        appWidgetManager.updateAppWidget(appWidgetId, main);
    }

    /**
     * Show the play button.
     *
     * @param context
     * @param cn
     * @param show if true show the button , if false hide the button.
     */
    public void showPlayButton(RemoteViews main, Context context, ComponentName cn, boolean show){
        if (main == null) {
        	main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        if (show) {
            main.setInt(ITEM_IMGBTN_ID[getPlayButtonIndex()], "setVisibility", View.VISIBLE);
        } else {
            main.setInt(ITEM_IMGBTN_ID[getPlayButtonIndex()], "setVisibility", View.INVISIBLE);
        }
//        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
//        appWidgetManager.updateAppWidget(cn, main);
    }

    /**
     * Show the play button.
     *
     * @param context
     * @param cn
     * @param show if true show the button , if false hide the button.
     */
    public void showPlayButton(RemoteViews main, Context context, int appWidgetId, boolean show){
    	if (main == null) {
        	main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        if (show) {
            main.setInt(ITEM_IMGBTN_ID[getPlayButtonIndex()], "setVisibility", View.VISIBLE);
        } else {
            main.setInt(ITEM_IMGBTN_ID[getPlayButtonIndex()], "setVisibility", View.INVISIBLE);
        }
//        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
//        appWidgetManager.updateAppWidget(appWidgetId, main);
    }

    /**
     * Show the setting button.
     *
     * @param context
     * @param cn
     * @param show if true show the button , if false hide the button.
     */
    public void showSettingButton(RemoteViews main, Context context, ComponentName cn, boolean show){
    	if (main == null) {
        	main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        if (show) {
            main.setInt(ITEM_IMGBTN_ID[getSettingButtonIndex()], "setVisibility", View.VISIBLE);
        } else {
            main.setInt(ITEM_IMGBTN_ID[getSettingButtonIndex()], "setVisibility", View.INVISIBLE);
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(cn, main);
    }

    /**
     * Show the setting button.
     *
     * @param context
     * @param cn
     * @param show if true show the button , if false hide the button.
     */
    public void showSettingButton(RemoteViews main, Context context, int appWidgetId, boolean show){
    	if (main == null) {
        	main = new RemoteViews(context.getPackageName(), getMainLayoutId());
        }
        if (show) {
            main.setInt(ITEM_IMGBTN_ID[getSettingButtonIndex()], "setVisibility", View.VISIBLE);
        } else {
            main.setInt(ITEM_IMGBTN_ID[getSettingButtonIndex()], "setVisibility", View.INVISIBLE);
        }
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(appWidgetId, main);
    }

    /**
     * Called when one of the icons is clicked.
     *
     * @param context
     * @param rank The position of the icon.
     */
    abstract public void onItemClick(Context context, int position);

    /**
     * Called when play button is clicked.
     *
     * @param context
     * @param position The position of the icon.
     */
    abstract public void onPlayClick(Context context);

    /**
     * Called when setting button is clicked.
     *
     * @param context
     * @param position The position of the icon.
     */
    abstract public void onSettingClick(Context context);
}
