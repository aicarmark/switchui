package com.motorola.mmsp.activitygraph;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Gallery;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.view.ViewGroup;


public class SetSkin extends Activity implements OnClickListener {
    
    private AdapterView<SkinAdapter> mGallery;
    private SkinAdapter adapter;
    private int mSkinMode;
    private static final String TAG = "SetSkin";
    private View mContentViewPortrait;
    private View mContentViewLandscape;
    static ArrayList<Skin> skins;
    static SkinViews views;
    static int mMaxCard = 0;
    private static int defaultSkinIndex = 0;
    
    private OnItemClickListener mItemClickListener = new OnItemClickListener () {
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {  
            
            mSkinMode = position;
            if (adapter != null) {
                adapter.setHighlight(mSkinMode);
                adapter.notifyDataSetChanged();
            }    
            
            try {
                previewCurrentSkin(mSkinMode);          
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }        
    };
    private OnItemSelectedListener mItemSelectedListenner=new OnItemSelectedListener()
    {
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                if (adapter != null) {
                    adapter.setHighlight(position);
                    adapter.notifyDataSetChanged();
                }
            }
        }
        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // TODO Auto-generated method stub
            
        }
        
    };
    
    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState){
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        mMaxCard = ActivityGraphModel.getMaxCard();
        
        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        Display display = getWindowManager().getDefaultDisplay();  
        boolean isLandscape = display.getWidth() > display.getHeight(); 
        
        if (isLandscape) {
            Log.d(TAG,"isLandscape");  
            //mContentViewLandscape = inflater.inflate(R.layout.set_skin_land, null);
            if(mMaxCard == 7){
                mContentViewLandscape = inflater.inflate(R.layout.set_skin_land_seven, null);
            } else if(mMaxCard == 9){
                mContentViewLandscape = inflater.inflate(R.layout.set_skin_land_nine, null);
            }
            setContentView(mContentViewLandscape);
            mGallery = (AdapterView<SkinAdapter>) findViewById(R.id.list);                   
            
        }else{
            Log.d(TAG,"isPortrait");
            //mContentViewPortrait = inflater.inflate(R.layout.set_skin, null);
            if(mMaxCard == 7){
                mContentViewPortrait= inflater.inflate(R.layout.set_skin_seven, null);
            } else if(mMaxCard == 9){
                mContentViewPortrait= inflater.inflate(R.layout.set_skin_nine, null);
            }
            setContentView(mContentViewPortrait);
            mGallery = (AdapterView<SkinAdapter>) findViewById(R.id.gallery);                          	
        }
        
        getSkins();
        
        mSkinMode = ActivityGraphModel.getGraphySkin();
        if(mSkinMode == 0xff){
            mSkinMode = defaultSkinIndex;
        }        
        Log.d(TAG,"Skin Mode:"+mSkinMode);
        adapter = new SkinAdapter(this);
        adapter.setHighlight(mSkinMode);
        mGallery.setAdapter(adapter);
        
        mGallery.setSelection(mSkinMode);  
        mGallery.setOnItemClickListener(mItemClickListener);    
        mGallery.setOnItemSelectedListener(mItemSelectedListenner);
        findViewById(R.id.set).setOnClickListener(this);
        
    }
    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        try {
            previewCurrentSkin(mSkinMode);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
    }
    protected void finalize() throws Throwable {
        try {
            Log.d(TAG, "finalize()");
        } finally {
            super.finalize();
        }
    }
    
    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        mContentViewPortrait = null;
        mContentViewLandscape = null;
        skins = null;
        views = null;
        System.gc();
        super.onDestroy();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(TAG, "onConfigurationChanged: " + newConfig);
        
        boolean isLandscape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE;
        
        LayoutInflater inflater;
        inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        if (isLandscape) {
            Log.d(TAG,"isLandscape"); 
            if(mContentViewLandscape == null){
                Log.d(TAG, "null view"); 
                //mContentViewLandscape = inflater.inflate(R.layout.set_skin_land, null);
                if(mMaxCard == 7){
                    mContentViewLandscape = inflater.inflate(R.layout.set_skin_land_seven, null);
                } else if(mMaxCard == 9){
                    mContentViewLandscape = inflater.inflate(R.layout.set_skin_land_nine, null);
                }
            }
            setContentView(mContentViewLandscape);
            mGallery = (AdapterView<SkinAdapter>) findViewById(R.id.list);       
        }else {  
            Log.d(TAG,"isPortrait");
            if(mContentViewPortrait == null){
                Log.d(TAG,"null view"); 
                //mContentViewPortrait = inflater.inflate(R.layout.set_skin, null);
                if(mMaxCard == 7){
                    mContentViewPortrait= inflater.inflate(R.layout.set_skin_seven, null);
                } else if(mMaxCard == 9){
                    mContentViewPortrait= inflater.inflate(R.layout.set_skin_nine, null);
                }  
            }
            setContentView(mContentViewPortrait);
            mGallery = (AdapterView<SkinAdapter>) findViewById(R.id.gallery);                              
        }
        adapter = new SkinAdapter(this);
        adapter.setHighlight(mSkinMode);
        mGallery.setAdapter(adapter);
        
        findViewById(R.id.set).setOnClickListener(this);
        mGallery.setSelection(mSkinMode);  
        mGallery.setOnItemClickListener(mItemClickListener); 
        mGallery.setOnItemSelectedListener(mItemSelectedListenner);
        try {
            previewCurrentSkin(mSkinMode);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void previewCurrentSkin(int skinMode) throws IOException{
        Log.d(TAG,"previewCurrentSkin:"+skinMode);
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        ImageView imageView; 
        Bitmap mBitmap;
        
        for (String id : views.ids.keySet()) {
            //Log.d(TAG,"id="+id);
            if (id != null && views.ids.get(id) != null) {
                int view = (Integer)views.ids.get(id);
                if ((view >= 0)&&(view!=R.id.skinitemimage)) {
                    imageView = (ImageView) findViewById(view);
                    if(imageView != null){				
                        InputStream is = this.getResources().openRawResource((Integer)skins.get(skinMode).properties.get(id));
                        
                        mBitmap = BitmapFactory.decodeStream(is,null,options);				     
                        imageView.setImageBitmap(mBitmap);//((Integer)getSkins().get(skinMode).properties.get(id));
                        is.close();
                        is = null;
                    }
                }
            }
        }
    }
    
    public void onClick(View v) {
        
        setSkinBg();
        ActivityGraphModel.setGraphySkin(mSkinMode);
        finish();
    }
    
    private void setSkinBg() {
        if(skins != null) {
            Skin skin = skins.get(mSkinMode);
            for (String key : skin.properties.keySet()) {
                if (key.equals(Skin.NAME) || key.equals(Skin.THUMBNAIL)) {
                    continue;
                }
                int ind = (Integer)skin.properties.get(key);
                //Log.d(TAG, "name:"+key+", id:"+ind);
                if (key.equals(Skin.BAR_1)) {
                    ActivityGraphModel.setGraphySkinBMP(0, ind);
                } else if (key.equals(Skin.BG_1)) {
                    ActivityGraphModel.setGraphySkinBMP(1, ind);
                } else if (key.equals(Skin.BAR_2)) {
                    ActivityGraphModel.setGraphySkinBMP(2, ind);
                } else if (key.equals(Skin.BG_2)) {
                    ActivityGraphModel.setGraphySkinBMP(3, ind);
                } else if (key.equals(Skin.BAR_3)) {
                    ActivityGraphModel.setGraphySkinBMP(4, ind);
                } else if (key.equals(Skin.BG_3)) {
                    ActivityGraphModel.setGraphySkinBMP(5, ind);
                } else if (key.equals(Skin.BAR_4)) {
                    ActivityGraphModel.setGraphySkinBMP(6, ind);
                } else if (key.equals(Skin.BG_4)) {
                    ActivityGraphModel.setGraphySkinBMP(7, ind);
                } else if (key.equals(Skin.BAR_5)) {
                    ActivityGraphModel.setGraphySkinBMP(8, ind);
                } else if (key.equals(Skin.BG_5)) {
                    ActivityGraphModel.setGraphySkinBMP(9, ind);
                } else if (key.equals(Skin.BAR_6)) {
                    ActivityGraphModel.setGraphySkinBMP(10, ind);
                } else if (key.equals(Skin.BG_6)) {
                    ActivityGraphModel.setGraphySkinBMP(11, ind);
                } else if (key.equals(Skin.BAR_7)) {
                    ActivityGraphModel.setGraphySkinBMP(12, ind);
                } else if (key.equals(Skin.BG_7)) {
                    ActivityGraphModel.setGraphySkinBMP(13, ind);
                } else if (key.equals(Skin.BAR_8)) {
                    ActivityGraphModel.setGraphySkinBMP(14, ind);
                } else if (key.equals(Skin.BG_8)) {
                    ActivityGraphModel.setGraphySkinBMP(15, ind);
                } else if (key.equals(Skin.BAR_9)) {
                    ActivityGraphModel.setGraphySkinBMP(16, ind);
                } else if (key.equals(Skin.BG_9)) {
                    ActivityGraphModel.setGraphySkinBMP(17, ind);
                }
            }
        }
    }
    
    static class SkinAdapter extends SimpleAdapter {
        private static final String TEXT = "text";
        private static final String BG = "bg";
        private static final String HIGHLIGHT = "highlight";
        private int highlightInex = 0;
        
        
        public SkinAdapter(Context context) {
            this(context, getData(context), getResource(), getFrom(), getTo());
        }
        
        public SkinAdapter(Context context, List<? extends Map<String, ?>> data,
                int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }
        
        public void setHighlight(int index) {
            highlightInex = index;
        }
        
        private static List<? extends Map<String, ?>> getData(Context context) {
            Log.d(TAG,"SkinAdapter getData()");
            ArrayList<HashMap<String, Object>> data = new ArrayList<HashMap<String,Object>>();
            int count = skins.size();		
            Skin skin;
            
            for (int i = 0; i < count; i++) {
                HashMap<String, Object> item = new HashMap<String, Object>();
                skin = skins.get(i);
                item.put(BG, skin.properties.get(Skin.THUMBNAIL));
                data.add(item);
            }
            return data;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            if (view != null) {
                ImageView image = (ImageView) (view.findViewById(R.id.skinselected));
                if (position == highlightInex && image != null) {
                    image.setImageResource(R.drawable.skin_thumbnail_selected);
                } else if (image != null) {
                    image.setImageResource(0);
                    
                }
            }
            
            return view;
        }
        
        private static int getResource() {
            //return R.layout.setskin_item;
            if(mMaxCard == 7)
                return R.layout.setskin_item_seven;
            return R.layout.setskin_item_nine;              
            
        }
        
        private static String[] getFrom() {
            return new String[] {BG};
        }
        
        private static int[] getTo() {
            return new int[] {R.id.skinitemimage};
        }
    }
    
    private void getSkins() {  
        
        //XmlPullParser parser = (mMaxCard == 7)?getBaseContext().getResources().getXml(R.xml.skins_seven):getBaseContext().getResources().getXml(R.xml.skins_nine);
        XmlPullParser parser = getBaseContext().getResources().getXml(R.xml.skins); 
        
        String parserName;
        String attrName;
        String attrValue;
        Object skinName;
        
        skins = new ArrayList<Skin>();
        views = new SkinViews();
        
        int index = 0;
        try {
            while(parser.next() != XmlPullParser.END_DOCUMENT) {
                parserName = parser.getName();
                if (parserName != null && parserName.equals("item")) {
                    HashMap<String, Object> item = new HashMap<String, Object>();
                    int count = parser.getAttributeCount();
                    for (int i=0; i < count; i++) {
                        attrName = parser.getAttributeName(i);
                        attrValue = parser.getAttributeValue(i);
                        if (attrName != null && attrValue != null) {
                            if (attrValue.startsWith("@")) {
                                item.put(attrName, Integer.parseInt(attrValue.replace("@", "")));
                            } else {
                                item.put(attrName, attrValue);
                            }
                        }
                    }
                    skinName = (Object)item.get(CommonColumn.NAME);
                    if (skinName != null && !skinName.equals("")) {
                        if (skinName.equals(SkinViews.ME)) {
                            item.remove(CommonColumn.NAME);
                            views.form(item);
                        } else {
                            index ++;
                            Skin skin = new Skin(item);
                            skins.add(skin);
                        }
                        
                        if (skinName.equals(R.string.patchwork)){
                            defaultSkinIndex = index - 1;
                            Log.d(TAG,"getSkins(): defaultSkinIndex = " + defaultSkinIndex);
                        }						
                    }
                }
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 
}
