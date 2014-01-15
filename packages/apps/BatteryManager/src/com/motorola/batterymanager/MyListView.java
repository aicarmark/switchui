/** 
 * Copyright (C) 2009, Motorola, Inc, 
 * All Rights Reserved 
 * Class name: MyListView.java 
 * Description: What the class does. 
 * 
 * Modification History: 
 **********************************************************
 * Date           Author       Comments
 * Feb 15, 2010	      A24178      Created file
 **********************************************************
 */

package com.motorola.batterymanager;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.motorola.batterymanager.R;

/**
 * @author A24178
 *
 */
// pacakge-scope
class MyListView extends ListView {
    private final static String LOG_TAG = "MyListView";
    
    MyListAdapter mAdapter;
    ScreenResponseHandler mUpdateCallback;
     
    public MyListView(Context context) {
        super(context);
        commonInit(context);
    }
    
    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        commonInit(context);
    }
   
    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public MyListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        commonInit(context);
    }
    
    private void commonInit(Context context) {
        setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        setScrollContainer(false);
        mAdapter = new MyListAdapter(context, mCallback);
        setAdapter(mAdapter);
    }
    
    // App Interface methods
    // View update : App -> View
    public void setOffPeakContent(String[] values, boolean refresh) {
        mAdapter.setOffPeakContent(values, refresh);
    }
    
    public void setPeakContent(String value, boolean refresh) {
        mAdapter.setPeakContent(value, refresh);
    }
    
    public void setProgressContent(Integer[] values, boolean refresh) {
        mAdapter.setProgressContent(values, refresh);
    }
    
    // App update: View -> App
    public void setUpdateCallbacks(ScreenResponseHandler cb) {
        mUpdateCallback = cb;
    }
    
    private ScreenResponseHandler mCallback = new ScreenResponseHandler() {

        @Override
        public void onClick(View v) {
            if(mUpdateCallback != null) {
                mUpdateCallback.onClick(v);
            }
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                boolean fromUser) {
            if(mUpdateCallback != null) {
                mUpdateCallback.onProgressChanged(seekBar, progress, fromUser);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub
            
        }
        
    };
    
    private class MyListAdapter extends BaseAdapter {
        private Context mContext;
        private ScreenResponseHandler mCb;
        // created one-time
        //private Drawable mProgress1, mProgress2;
        
        Object mViewContent[] = new Object[4];
        String mStrings[] = new String[3];
        Integer mInts[] = new Integer[4];
        
        public void setOffPeakContent(String[] values, boolean refresh) {
            if(values.length >= 3) {
                String[] currValues = (String[])mViewContent[1];
                if(currValues != null) {
                    for(int i = 2; i >= 0; --i) {
                        if(values[i] != null) {
                            currValues[i] = values[i];
                        }
                    }
                }else {
                    mViewContent[1] = values;
                }
                if(refresh) {
                    notifyDataSetChanged();
                }
            }
        }
        
        public void setPeakContent(String value, boolean refresh) {
            mViewContent[2] = value;
            if(refresh) {
                notifyDataSetChanged();
            }
        }
        
        public void setProgressContent(Integer[] values, boolean refresh) {
            if(values.length >= 4) {
                Integer[] currValues = (Integer[])mViewContent[3];
                if(currValues != null) {
                    for(int i = 3; i >= 0; --i) {
                        if(values[i] != null) {
                            currValues[i] = values[i];
                        }
                    }
                }else {
                    mViewContent[3] = values;
                }
                if(refresh) {
                    notifyDataSetChanged();
                }
            }
        }
        
        private void updateDescView(View v) {
            if(v != null) {
                v.setVisibility(View.VISIBLE);
            }
        }
        
        private void updateProgressView(View v) {
            if(v != null && mViewContent[3] != null) {
                v.setVisibility(View.VISIBLE);
                Integer[] values = (Integer[])mViewContent[3];
                SeekBar sb = (SeekBar)v.findViewById(R.id.customs_disp_seekbar);
                if(sb != null) {
                    //sb.setProgressDrawable(mProgress1);
                    sb.setMax(values[2]);
                    sb.setProgress(values[0]);
                    sb.setOnSeekBarChangeListener(mCb);
                    // sb.setSecondaryProgress(values[1]);
                    SeekBar psb = (SeekBar)v.findViewById(R.id.customs_disp_current);
                    //psb.setProgressDrawable(mProgress2);
                    psb.setMax(values[2]);
                    psb.setEnabled(true);
                    psb.setProgress(values[1]);
                    psb.setEnabled(false);

                    TextView tv = (TextView)v.findViewById(R.id.disp_title);
                    if(values[3] != 0) {
                        sb.setEnabled(false);
                        tv.setText(R.string.lightsensorctrl);
                    }else {
                        tv.setText(R.string.customs_disp_text);
                    }
                }else {
                    Utils.Log.d(LOG_TAG, "Seekbar isn't there in this view");
                }
                // whole view is disabled for auto brightness
                if(values[3] != 0) {
                    v.setEnabled(false);
                }
            }
        }
        
        private void updateOffPeakView(View v) {
            if(v != null && mViewContent[1] != null) {
                v.setVisibility(View.VISIBLE);
                String[] values = (String[])mViewContent[1];
                Button b = (Button)v.findViewById(R.id.op_start_button);
                b.setOnClickListener(mCb);
                b.setText(values[0]);
                b = (Button)v.findViewById(R.id.op_end_button);
                b.setOnClickListener(mCb);
                b.setText(values[1]);
                b = (Button)v.findViewById(R.id.op_data_button);
                b.setOnClickListener(mCb);
                b.setText(values[2]);
            }
        }
        
        private void updatePeakView(View v) {
            if(v != null && mViewContent[2] != null) {
                v.setVisibility(View.VISIBLE);
                Button b = (Button)v.findViewById(R.id.p_data_button);
                b.setOnClickListener(mCb);
                b.setText((String)mViewContent[2]);
            }
        }

        private void updateButtonsView(View v) {
            if(v != null) {
                v.setVisibility(View.VISIBLE);
                Button b = (Button)v.findViewById(R.id.custom_positive);
                b.setOnClickListener(mCb);
                b = (Button)v.findViewById(R.id.custom_negative);
                b.setOnClickListener(mCb);
            }
        }
        
        public MyListAdapter(Context ctx, ScreenResponseHandler cb) {
            mContext = ctx;
            mCb = cb;
            
            mViewContent[1] = mStrings;
            mViewContent[3] = mInts;
            
            //mProgress1 = mContext.getResources().getDrawable(R.drawable.custom_seek_bg3);
            //mProgress2 = mContext.getResources().getDrawable(R.drawable.custom_seek_trans_bg2);
        }

        /* (non-Javadoc)
         * @see android.widget.Adapter#getCount()
         */
        public int getCount() {
            return 5;
        }

        /* (non-Javadoc)
         * @see android.widget.Adapter#getItem(int)
         */
        public Object getItem(int position) {
            return position;
        }

        /* (non-Javadoc)
         * @see android.widget.Adapter#getItemId(int)
         */
        public long getItemId(int position) {
            return position;
        }

        /* (non-Javadoc)
         * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        public View getView(int position, View convertView, ViewGroup parent) {
            MyStubLayout vw = null;
            switch(position) {
            case 0:
                if(convertView == null || !(convertView instanceof MyStubLayout)) {
                    vw = new MyStubLayout(mContext);
                }else {
                    clearAllViews(convertView);
                    vw = (MyStubLayout)convertView;
                }
                //Utils.Log.v(LOG_TAG, "case " + position + vw + convertView);
                updateDescView(setView(vw.findViewById(R.id.customs_stub_desc),
                        R.id.icustoms_stub_desc, vw));
                break;
            case 1:
                if(convertView == null || !(convertView instanceof MyStubLayout)) {
                    vw = new MyStubLayout(mContext);
                }else {
                    clearAllViews(convertView);
                    vw = (MyStubLayout)convertView;
                }
                //Utils.Log.v(LOG_TAG, "case " + position + vw + convertView);
                updateOffPeakView(setView(vw.findViewById(R.id.customs_stub_offpeak),
                        R.id.icustoms_stub_offpeak, vw));
                break;
            case 2:
                if(convertView == null || !(convertView instanceof MyStubLayout)) {
                    vw = new MyStubLayout(mContext);
                }else {
                    clearAllViews(convertView);
                    vw = (MyStubLayout)convertView;
                }
                //Utils.Log.v(LOG_TAG, "case " + position + vw + convertView);
                updatePeakView(setView(vw.findViewById(R.id.customs_stub_peak),
                        R.id.icustoms_stub_peak, vw));
                break;
            case 3:
                if(convertView == null || !(convertView instanceof MyStubLayout)) {
                    vw = new MyStubLayout(mContext);
                }else {
                    clearAllViews(convertView);
                    vw = (MyStubLayout)convertView;
                }
                //Utils.Log.v(LOG_TAG, "case " + position + vw + convertView);
                updateProgressView(setView(vw.findViewById(R.id.customs_stub_disp), 
                        R.id.icustoms_stub_disp, vw));
                break;
            case 4:
                if(convertView == null || !(convertView instanceof MyStubLayout)) {
                    vw = new MyStubLayout(mContext);
                }else {
                    clearAllViews(convertView);
                    vw = (MyStubLayout)convertView;
                }
                //Utils.Log.v(LOG_TAG, "case " + position + vw + convertView);
                /* updateButtonsView(setView(vw.findViewById(R.id.customs_stub_button),
                        R.id.icustoms_stub_button, vw)); */
                break;
            default:
                break;
            }
            return vw;
        }
        
        private void clearAllViews(View cv) {
            MyStubLayout sL = (MyStubLayout)cv;
            
            clearView(sL.findViewById(R.id.icustoms_stub_desc));
            clearView(sL.findViewById(R.id.icustoms_stub_offpeak));
            clearView(sL.findViewById(R.id.icustoms_stub_peak));
            clearView(sL.findViewById(R.id.icustoms_stub_disp));
            // clearView(sL.findViewById(R.id.icustoms_stub_button));
        }
        
        private void clearView(View v) {
            if(v != null) v.setVisibility(View.GONE);
        }
        
        private View setView(View v, int altId, View parent) {
            //Utils.Log.v(LOG_TAG, "setView " + v);
            ViewStub vs = (ViewStub) v;
            if(vs != null) {
                return vs.inflate();
            }else {
                return parent.findViewById(altId);
            }
        }
    }
}
