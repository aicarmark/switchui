 /*
  * Copyright (C) 2009/2010 Motorola Inc.
  * All Rights Reserved.
  * Motorola Confidential Restricted.
  */

package com.motorola.mmsp.motohomex;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import com.motorola.mmsp.motohomex.R;

/*changed by bvq783 for removing some unused panel transition*/
public class TransitionSettingActivity extends Activity {

    final int[] items_img = {
            //R.drawable.transition_default,
            R.drawable.transition_boxin,
            R.drawable.transition_door,
            //R.drawable.transition_boxout,
            R.drawable.transition_twirling,
            //R.drawable.transition_rotate,
            R.drawable.transition_rotate_smooth,
            //R.drawable.transition_jump,
    };

    final int [][] frames = {
            /*{
                R.drawable.h_default_00001,
                R.drawable.h_default_00002,
                R.drawable.h_default_00003,
                R.drawable.h_default_00004,
                R.drawable.h_default_00005,
                R.drawable.h_default_00006,
                R.drawable.h_default_00007,
                R.drawable.h_default_00008,
                R.drawable.h_default_00009,
                R.drawable.h_default_00010,
                R.drawable.h_default_00011,
                R.drawable.h_default_00012,
                R.drawable.h_default_00013,
                R.drawable.h_default_00014,
                R.drawable.h_default_00015,
                R.drawable.h_default_00016,
                R.drawable.h_default_00017				
            },*/
            {
            	R.drawable.c_box_in_00000,
                R.drawable.c_box_in_00001,
                R.drawable.c_box_in_00002,
                R.drawable.c_box_in_00003,
                R.drawable.c_box_in_00004,
                R.drawable.c_box_in_00005,
                R.drawable.c_box_in_00006,
                R.drawable.c_box_in_00007,
                R.drawable.c_box_in_00008,
                R.drawable.c_box_in_00009,
                R.drawable.c_box_in_00010,
                R.drawable.c_box_in_00011,
                R.drawable.c_box_in_00012
                /*R.drawable.c_box_in_00013,
                R.drawable.c_box_in_00014,
                R.drawable.c_box_in_00015,
                R.drawable.c_box_in_00016,
                R.drawable.c_box_in_00017*/
            },
            {
                R.drawable.a_door_gray_00000,
                R.drawable.a_door_gray_00001,
                R.drawable.a_door_gray_00002,
                R.drawable.a_door_gray_00003,
                R.drawable.a_door_gray_00004,
                R.drawable.a_door_gray_00005,
                R.drawable.a_door_gray_00006,
                R.drawable.a_door_gray_00007,
                R.drawable.a_door_gray_00008,
                R.drawable.a_door_gray_00009,
                R.drawable.a_door_gray_00010,
                R.drawable.a_door_gray_00011,
                R.drawable.a_door_gray_00012,
                R.drawable.a_door_gray_00013,
                R.drawable.a_door_gray_00014,
                R.drawable.a_door_gray_00015,
                R.drawable.a_door_gray_00016,
                R.drawable.a_door_gray_00017,
                R.drawable.a_door_gray_00018,
                R.drawable.a_door_gray_00019,
                R.drawable.a_door_gray_00020
            },
            /*{
            	R.drawable.b_box_out_00000,
                R.drawable.b_box_out_00001,
                R.drawable.b_box_out_00002,
                R.drawable.b_box_out_00003,
                R.drawable.b_box_out_00004,
                R.drawable.b_box_out_00005,
                R.drawable.b_box_out_00006,
                R.drawable.b_box_out_00007,
                R.drawable.b_box_out_00008,
                R.drawable.b_box_out_00009,
                R.drawable.b_box_out_00010,
                R.drawable.b_box_out_00011,
                R.drawable.b_box_out_00012,
                R.drawable.b_box_out_00013,
                R.drawable.b_box_out_00014,
                R.drawable.b_box_out_00015,
                R.drawable.b_box_out_00016,
                R.drawable.b_box_out_00017
            },*/
            {
                R.drawable.d_rotate_slide_in_00000,
                R.drawable.d_rotate_slide_in_00001,
                R.drawable.d_rotate_slide_in_00002,
                R.drawable.d_rotate_slide_in_00003,
                R.drawable.d_rotate_slide_in_00004,
                R.drawable.d_rotate_slide_in_00005,
                R.drawable.d_rotate_slide_in_00006,
                R.drawable.d_rotate_slide_in_00007,
                R.drawable.d_rotate_slide_in_00008,
                R.drawable.d_rotate_slide_in_00009,
                R.drawable.d_rotate_slide_in_00010,
                R.drawable.d_rotate_slide_in_00011,
                R.drawable.d_rotate_slide_in_00012,
                R.drawable.d_rotate_slide_in_00013,
                R.drawable.d_rotate_slide_in_00014,
                R.drawable.d_rotate_slide_in_00015,
                R.drawable.d_rotate_slide_in_00016,
                R.drawable.d_rotate_slide_in_00017,
                R.drawable.d_rotate_slide_in_00018,
                R.drawable.d_rotate_slide_in_00019,
                R.drawable.d_rotate_slide_in_00020,
                R.drawable.d_rotate_slide_in_00021,
                R.drawable.d_rotate_slide_in_00022
            },
            /*{
                R.drawable.e_rotate_center_00000,
                R.drawable.e_rotate_center_00001,
                R.drawable.e_rotate_center_00002,
                R.drawable.e_rotate_center_00003,
                R.drawable.e_rotate_center_00004,
                R.drawable.e_rotate_center_00005,
                R.drawable.e_rotate_center_00006,
                R.drawable.e_rotate_center_00007,
                R.drawable.e_rotate_center_00008,
                R.drawable.e_rotate_center_00009,
                R.drawable.e_rotate_center_00010,
                R.drawable.e_rotate_center_00011,
                R.drawable.e_rotate_center_00012,
                R.drawable.e_rotate_center_00013,
                R.drawable.e_rotate_center_00014,
                R.drawable.e_rotate_center_00015
            },*/
            {
                R.drawable.f_rotate_smooth_00000,
                R.drawable.f_rotate_smooth_00001,
                R.drawable.f_rotate_smooth_00002,
                R.drawable.f_rotate_smooth_00003,
                R.drawable.f_rotate_smooth_00004,
                R.drawable.f_rotate_smooth_00005,
                R.drawable.f_rotate_smooth_00006,
                R.drawable.f_rotate_smooth_00007,
                R.drawable.f_rotate_smooth_00008,
                R.drawable.f_rotate_smooth_00009,
                R.drawable.f_rotate_smooth_00010,
                R.drawable.f_rotate_smooth_00011,
                R.drawable.f_rotate_smooth_00012,
                R.drawable.f_rotate_smooth_00013,
                R.drawable.f_rotate_smooth_00014,
                R.drawable.f_rotate_smooth_00015,
                R.drawable.f_rotate_smooth_00016,
                R.drawable.f_rotate_smooth_00017,
                R.drawable.f_rotate_smooth_00018,
                R.drawable.f_rotate_smooth_00019,
                R.drawable.f_rotate_smooth_00020
            }
            /*{
                R.drawable.g_jump_in_00000,
                R.drawable.g_jump_in_00001,
                R.drawable.g_jump_in_00002,
                R.drawable.g_jump_in_00003,
                R.drawable.g_jump_in_00004,
                R.drawable.g_jump_in_00005,
                R.drawable.g_jump_in_00006,
                R.drawable.g_jump_in_00007,
                R.drawable.g_jump_in_00008,
                R.drawable.g_jump_in_00009,
                R.drawable.g_jump_in_00010,
                R.drawable.g_jump_in_00011,
                R.drawable.g_jump_in_00012,
                R.drawable.g_jump_in_00013,
                R.drawable.g_jump_in_00014,
                R.drawable.g_jump_in_00015,
                R.drawable.g_jump_in_00016,
                R.drawable.g_jump_in_00017
            }*/
    };

    /*add by bvq783 for entering transtion and app menu transition*/
    private static final String ENTERING_TRANSITION_EFFECT = "entering_transition_effect";
    private static final String APP_MENU_TRANSITION_EFFECT = "apps_transition_effect";
    /*add by bvq783 end*/
    private static final String WORKSPACE_TRANSITION_EFFECT = "workspace_transition_effect";
    String[] entries,values;
    int effect;
    TextView tv;
    ImageView iv,selected_item;
    AnimationDrawable animations[] = new AnimationDrawable[4];
    Gallery g;
    MovieLoader mLoader;

    class MovieLoader extends AsyncTask<Integer, Void, AnimationDrawable> {
        protected void onPreExecute() {
            iv.setImageResource(frames[effect][0]);
        }
        @Override
        protected AnimationDrawable doInBackground(Integer... params) {
            if (isCancelled()) return null;

            int position = params[0];
            AnimationDrawable animation = new AnimationDrawable();
            int num = frames[position].length;
            for ( int i=1; i < num-1 ; i++) {
                animation.addFrame(getResources().getDrawable(frames[position][i]), 83);
            }
            animation.addFrame(getResources().getDrawable(frames[position][num-1]), 800);
            animation.addFrame(getResources().getDrawable(frames[position][0]), 1000);
            animation.setOneShot(false);

            return animation;
        }

        @Override
        protected void onPostExecute(AnimationDrawable animation) {
            if (animation == null) return;

            if (!isCancelled()) {
                iv.setImageDrawable(animation);
                animation.start();
            }
        }

        void cancel() {
            super.cancel(true);
        }
    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            if (mLoader != null) {
                mLoader.cancel();
            }
            mLoader = (MovieLoader) new MovieLoader().execute(effect);
        }
    };

    private void startAnimation(){
        handler.sendEmptyMessageDelayed(0, 700);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transition_setting);

        effect = Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString(WORKSPACE_TRANSITION_EFFECT, "0"));

        Resources res =getResources();
        entries = res.getStringArray(R.array.workspace_tansition_effect_entries);
        values  = res.getStringArray(R.array.workspace_tansition_effect_values);

        g = (Gallery) findViewById(R.id.gallery);
        g.setAdapter(new ImageAdapter(this));
        g.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                /*2012-07-27, ChenYidong added for SWITCHUI-2482*/
                if(position == -1 || v == null) return;
                /*2012-07-27, end*/
                if ( effect == position) return;

                for ( int i = 0; i < g.getChildCount(); i++) {
                    if (g.getChildAt(i).getTag() == String.valueOf(effect)) {
                        ((ImageView)g.getChildAt(i)).setBackgroundDrawable(null);
                    }
                }
                ((ImageView)v).setBackgroundResource(R.drawable.grid_pressed);

                effect = position;
                tv.setText(entries[effect]);
                startAnimation();
            }
        });
        g.setSelection(effect);

        iv = (ImageView) findViewById(R.id.iv);
        tv = (TextView) findViewById(R.id.tv);
        tv.setText(entries[effect]);

        Button set = (Button) findViewById(R.id.set);
        set.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(TransitionSettingActivity.this);
                Editor editor = sp.edit();
                editor.putString(WORKSPACE_TRANSITION_EFFECT, String.valueOf(effect));
                /*2012-6-20, removed by bvq783 for switchui-1569*/
                /*add by bvq783 for entering transtion and app menu transition*/
                //int id = effect==0?0:1;
                editor.putString(APP_MENU_TRANSITION_EFFECT, String.valueOf(effect));
                //editor.putString(ENTERING_TRANSITION_EFFECT, String.valueOf(id));
                /*add by bvq783 end*/
                /*2012-6-20, removed end*/
                editor.commit();
                /*2012-07-24, added by Bai Jian SWITCHUI-2449*/
                setResult(Activity.RESULT_OK);
                /*2012-07-24, end*/
                finish();
            }
        });
        /*2012-6-20, removed by bvq783 for switchui-1569*/
        //Button reset = (Button) findViewById(R.id.reset);
        //reset.setOnClickListener(new View.OnClickListener() {

            //@Override
            //public void onClick(View v) {
                // TODO Auto-generated method stub
                //SharedPreferences sp = PreferenceManager
                //    .getDefaultSharedPreferences(TransitionSettingActivity.this);
                //Editor editor = sp.edit();
                //editor.putString(WORKSPACE_TRANSITION_EFFECT, String.valueOf(0));
                /*add by bvq783 for entering transtion and app menu transition*/
		  //editor.putString(APP_MENU_TRANSITION_EFFECT, String.valueOf(0));
                //editor.putString(ENTERING_TRANSITION_EFFECT, String.valueOf(0));
                /*add by bvq783 end*/
                //editor.commit();
                //finish();
            //}
        //});
        /*2012-6-20, removed end*/
        startAnimation();
    }

    public class ImageAdapter extends BaseAdapter {
        private static final int ITEM_WIDTH = 60;
        private static final int ITEM_HEIGHT = 60;

        private final Context mContext;
        private final float mDensity;

        public ImageAdapter(Context c) {
            mContext = c;
            mDensity = c.getResources().getDisplayMetrics().density;
        }

        public int getCount() {
            return items_img.length;
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                convertView = new ImageView(mContext);

                imageView = (ImageView) convertView;
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                imageView.setLayoutParams(new Gallery.LayoutParams(
                        (int) (ITEM_WIDTH * mDensity + 0.5f),
                        (int) (ITEM_HEIGHT * mDensity + 0.5f)));
            } else {
                imageView = (ImageView) convertView;
            }
            imageView.setImageResource(items_img[position]);
            if ( effect == position) imageView.setBackgroundResource(
                    R.drawable.grid_pressed);
            imageView.setTag(String.valueOf(position));

            return imageView;
        }
    }

    public void onDestroy() {
        super.onDestroy();

        if (mLoader != null ) {
            mLoader.cancel(true);
            mLoader = null;
        }
    }
}
