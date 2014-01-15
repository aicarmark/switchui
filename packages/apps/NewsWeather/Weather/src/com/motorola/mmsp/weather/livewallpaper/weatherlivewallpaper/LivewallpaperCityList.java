package com.motorola.mmsp.weather.livewallpaper.weatherlivewallpaper;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.motorola.mmsp.weather.R;
import com.motorola.mmsp.weather.livewallpaper.utility.GlobalDef;
import com.motorola.mmsp.weather.livewallpaper.utility.WeatherInfoProvider;

public class LivewallpaperCityList extends Activity { 
    private static final String TAG = "LiveWallpaperService";
    private static LivewallpaperCityList  mWeatherCityList;
    private static CityAndWeather mCityAndWeather = null;

    private ListView mListView;
    private CityListAdapter mCityListAdapter;
    private ImageView mBottonLine;

    public static LivewallpaperCityList getWeatherCityListInstan(){
        return mWeatherCityList;
    }

    static public void registerCityAndWeather( CityAndWeather c){
        mCityAndWeather = c;
    }

    static public void unRegisterCityAndWeather( CityAndWeather c){
        mCityAndWeather = null;
    }

    @Override  
    protected void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.city_list);
        
        mWeatherCityList = this;
      
        init();
               
        try{
            mCityAndWeather.getCurDevice().setOrientationStatus(this);
        }catch(Exception e){
            e.printStackTrace();
        }      
    }   
    
    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG, "LivewallpaperCityList resume"); 

        //set the first item defaultly
        if( WeatherInfoProvider.getCurCityName(getBaseContext()).equals("") ){
            if(WeatherInfoProvider.getExitCities(getBaseContext()).isEmpty() == false ){
                GlobalDef.CityInfo info = WeatherInfoProvider.getExitCities(getBaseContext()).get(0);
                String str = info.name;
                if( str.equals("") == false ){
                    WeatherInfoProvider.setCurCity(0, info.id, str, getBaseContext());
                    Log.d(TAG,"LivewallpaperCityList default city is "+str);
                }
            }
        }

        String cityid = WeatherInfoProvider.getCurCityId(getBaseContext());
        boolean exit = false;
        ArrayList<GlobalDef.CityInfo> cityInfoList = WeatherInfoProvider.getExitCities( getBaseContext() );
        for( int i = 0; cityInfoList != null && i < cityInfoList.size(); ++i ){
            addCity( cityInfoList.get(i) );
            //mBottonLine.setVisibility(View.VISIBLE); 
            if( cityid.equals(cityInfoList.get(i).id) )
                exit = true;
        }     

        if( !exit ){
            WeatherInfoProvider.setCurCity(-1, "", "", getBaseContext());
        }

    }
    
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "LivewallpaperCityList pause");
        mCityListAdapter.removeAll(); 
        //mBottonLine.setVisibility(View.INVISIBLE); 
    }
    
    private void init(){
        this.setTitle(R.string.location_title);

        //if use sina service, set it unvisible
        TextView tip_name = (TextView) findViewById( R.id.tip_city_name);
        tip_name.setText( getString(R.string.tip_city_name) );

        mCityListAdapter = new CityListAdapter(this);
        mListView = (ListView)findViewById(R.id.city_listView);
        mListView.setAdapter(mCityListAdapter); 
        mListView.setOnItemClickListener( mItemClickLst );

        FrameLayout l =(FrameLayout)findViewById(R.id.barLayout);         
        l.setOnClickListener(new View.OnClickListener(){  
            @Override  
            public void onClick(View v) {  
                Intent intent = new Intent(GlobalDef.CITY_ACTIVITY);
                LivewallpaperCityList.this.startActivity(intent);
            }              
        }); 
        
        ImageButton mImageButton =(ImageButton)findViewById(R.id.add_button);         
        mImageButton.setOnClickListener(new View.OnClickListener(){  
            @Override  
            public void onClick(View v) {  
                Intent intent = new Intent(GlobalDef.CITY_ACTIVITY);
                LivewallpaperCityList.this.startActivity(intent);
            }              
        });
        
        //mBottonLine = (ImageView) findViewById( R.id.bottomLine);
        //mBottonLine.setVisibility(View.INVISIBLE);       
    }
    
    AdapterView<?> mAv;
    private OnItemClickListener  mItemClickLst = new OnItemClickListener(){
        @Override
        public void onItemClick(AdapterView<?> av, View v, int index, long arg3) {

            mAv = av;
            Log.d(TAG,"LivewallpaperCityList onItemClick : clear all status");
            //discheck all items
            clearItemStatus(av);

            //check the selected one
            //TextView tv = (TextView)av.getChildAt(index).findViewById(R.id.item_city_name);
            TextView tv = (TextView)v.findViewById(R.id.item_city_name);
            String city = (String) tv.getText();
            String id = ((GlobalDef.CityInfo)(mCityListAdapter.getItem(index))).id;
            WeatherInfoProvider.setCurCity(index, id, city, v.getContext());
            RadioButton rb = (RadioButton) v.findViewById(R.id.radioButton);
            rb.setChecked(true);
            Log.d(TAG, "LivewallpaperCityList onItemClick : "+index+","+id+","+city+ " is checked");
        }
    };

    private void clearItemStatus(AdapterView<?> av){
        for( int i = 0; i < av.getCount(); ++i ){
            if( av != null && av.getChildAt(i) != null ){
                RadioButton rb = (RadioButton) av.getChildAt(i).findViewById(R.id.radioButton);
                rb.setChecked(false);
            }
        }
    }
    
    public void setCurrentCity( String city ){
        TextView cur = (TextView) findViewById( R.id.cityTextView);  
        cur.setTextColor(android.graphics.Color.GRAY);
        cur.setText( city );
    }
    
    public void addCity( GlobalDef.CityInfo info ){
        mCityListAdapter.addCity(info);
    }
    
    public class CityListAdapter extends BaseAdapter {
        
        private ArrayList<GlobalDef.CityInfo> mItemList = new ArrayList<GlobalDef.CityInfo>();
        private LayoutInflater mInflater;

        public CityListAdapter(Context contex){
            mInflater = LayoutInflater.from(contex);
        }
        
        @Override
        public int getCount() {
            return mItemList.size();
        }

        @Override
        public Object getItem(int position) {
            return mItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) { 
            mAv = (AdapterView<?>) parent;
            if( convertView == null ){            
                convertView = mInflater.inflate(R.layout.city_item, null);
            } 

            //set city name
            TextView tv = (TextView)convertView.findViewById(R.id.item_city_name);
            String city = mItemList.get(position).name;
            tv.setText(city);

            RadioButton rb = (RadioButton)convertView.findViewById(R.id.radioButton);  
            rb.setTag(position);
            rb.setChecked(false);
            Log.d(TAG,"LivewallpaperCityList " + position + "."+mItemList.get(position).name+" clear check status");

            //if it is the selected item, check it.
            String id = WeatherInfoProvider.getCurCityId(convertView.getContext());
            if( id.equals(mItemList.get(position).id) ){
                Log.d(TAG,"LivewallpaperCityList init : "+city+" is checked");
                rb.setChecked(true);  
                WeatherInfoProvider.setCurCity(position, id, mItemList.get(position).name, convertView.getContext());
            }  
            else
                Log.d(TAG,"LivewallpaperCityList init : "+city+" is not checked");

            //sj 1919
            rb.setOnClickListener( new OnClickListener(){

                @Override
                public void onClick(View v) {
                    int index = 0;
                    for( int i = 0; i < mAv.getCount(); ++i ){
                        if( v.equals(mAv.getChildAt(i).findViewById(R.id.radioButton)) ){
                            index = i;
                            break;
                        }
                    }

                    int position =  (Integer)v.getTag();
                    Log.d(TAG,"LivewallpaperCityList radio button click : index = "+index+", position = "+position);
                    mItemClickLst.onItemClick(mAv, mAv.getChildAt(index), position, 0);
                }
            });

            return convertView;
        }
        
        public void addCity( GlobalDef.CityInfo info ){
            String city = info.name;
            if( city != null && city.length() > 0  ){
                mItemList.add(info);
                Log.d(TAG,"LivewallpaperCityList add "+city);
                mCityListAdapter.notifyDataSetInvalidated();  
            }
        }
        
        public void removeAll(){
            mItemList.clear();
            mCityListAdapter.notifyDataSetInvalidated();   
        }
    }
}  
