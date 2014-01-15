package com.motorola.mmsp.activitygraph;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.KeyEvent;
import android.provider.Settings;
import android.content.ContentResolver;
import com.motorola.mmsp.activitygraph.R;
import com.motorola.mmsp.activitygraph.R.*;
public class ManageShortcuts extends Activity implements View.OnClickListener 
,ActivityGraphModel.Callbacks {
    
    private static final int PICK_APP = 1;
    private static final String TAG = "ManageShortcuts";
    private View mContentViewPortrait;
    private View mContentViewLandscape;
    private int mTopAppNumber ;//= ActivityGraphModel.getMaxCard();
    private int mScreenSize; //= (mTopAppNumber == 9)?1:2;
    private int fromWelcome = 0; 
    private static final int NO_VISUAL_REPRESENTATION  = 0xffff;
    
    private int[] getIconIds() {
        return icons_all[mScreenSize];
    }
    
    private int[] getLabelIds() {
        return labels_all[mScreenSize];
    }
    
    private int[] getButtonIds() {
        return buttons_all[mScreenSize];
    }
    private int[] getChangeButtonIds() {
        return changebuttons_all[mScreenSize];
    }
    private int[] getBgIds(){
        return bgs_all[mScreenSize];
    }
    private int[] getButtonLayoutIds() {
        return buttonlayouts_all[mScreenSize];
    }
    private int[] getButtonTextIds() {
        return buttontexts_all[mScreenSize];
    }
    private int[] getLabelBgIds(){
        return labelbgs_all[mScreenSize];
    }
    
    private static int[] icons_9 = new int[] {
        R.id.image1,
        R.id.image2,
        R.id.image3,
        R.id.image4,
        R.id.image5,
        R.id.image6,
        R.id.image7,
        R.id.image8,
        R.id.image9
    };
    private static int[] icons_7 = new int[] {
        R.id.image1,
        R.id.image2,
        R.id.image3,
        R.id.image4,
        R.id.image5,
        R.id.image6,
        R.id.image7,
    };
    private static int[] labels_9 = new int[] {
        R.id.text1,
        R.id.text2,
        R.id.text3,
        R.id.text4,
        R.id.text5,
        R.id.text6,
        R.id.text7,
        R.id.text8,
        R.id.text9
    };
    private static int[] labels_7 = new int[] {
        R.id.text1,
        R.id.text2,
        R.id.text3,
        R.id.text4,
        R.id.text5,
        R.id.text6,
        R.id.text7
    };
    
    private static int[] labelBgs_9 = new int[] {
        R.drawable.bar_bg_big,
        R.drawable.bar_bg_medium,
        R.drawable.bar_bg_medium,
        R.drawable.bar_bg_medium,
        R.drawable.bar_bg_small,
        R.drawable.bar_bg_small,
        R.drawable.bar_bg_small,
        R.drawable.bar_bg_small,
        R.drawable.bar_bg_small
    };
    private static int[] labelBgs_7 = new int[] {
        R.drawable.bar_bg_big,
        R.drawable.bar_bg_medium,
        R.drawable.bar_bg_small,
        R.drawable.bar_bg_medium,
        R.drawable.bar_bg_small,
        R.drawable.bar_bg_small,
        R.drawable.bar_bg_small
    };
    private static int[] buttons_9 = new int[] {
        R.id.button1,
        R.id.button2,
        R.id.button3,
        R.id.button4,
        R.id.button5,
        R.id.button6,
        R.id.button7,
        R.id.button8,
        R.id.button9,
    };
    private static int[] changebuttons_9 = new int[] {
        R.id.changebutton1,
        R.id.changebutton2,
        R.id.changebutton3,
        R.id.changebutton4,
        R.id.changebutton5,
        R.id.changebutton6,
        R.id.changebutton7,
        R.id.changebutton8,
        R.id.changebutton9,
    };
    private static int[] buttonlayouts_9 = new int[] {
        R.id.buttonlayout1,
        R.id.buttonlayout2,
        R.id.buttonlayout3,
        R.id.buttonlayout4,
        R.id.buttonlayout5,
        R.id.buttonlayout6,
        R.id.buttonlayout7,
        R.id.buttonlayout8,
        R.id.buttonlayout9,
    };
    private static int[] bgs_9=new int[]{
        R.id.bg_IM_1,
        R.id.bg_IM_2,
        R.id.bg_IM_3,
        R.id.bg_IM_4,
        R.id.bg_IM_5,
        R.id.bg_IM_6,
        R.id.bg_IM_7,
        R.id.bg_IM_8,
        R.id.bg_IM_9
    };
    private static int[] bgs_7=new int[]{
        R.id.bg_IM_1,
        R.id.bg_IM_2,
        R.id.bg_IM_3,
        R.id.bg_IM_4,
        R.id.bg_IM_5,
        R.id.bg_IM_6,
        R.id.bg_IM_7
    };
    private static int[] buttons_7 = new int[] {
        R.id.button1,
        R.id.button2,
        R.id.button3,
        R.id.button4,
        R.id.button5,
        R.id.button6,
        R.id.button7
    };
    private static int[] changebuttons_7 = new int[] {
        R.id.changebutton1,
        R.id.changebutton2,
        R.id.changebutton3,
        R.id.changebutton4,
        R.id.changebutton5,
        R.id.changebutton6,
        R.id.changebutton7
    };
    private static int[] buttonlayouts_7 = new int[] {
        R.id.buttonlayout1,
        R.id.buttonlayout2,
        R.id.buttonlayout3,
        R.id.buttonlayout4,
        R.id.buttonlayout5,
        R.id.buttonlayout6,
        R.id.buttonlayout7
    };
    private static int[] buttontexts_9 = new int[] {
        R.id.btntext1,
        R.id.btntext2,
        R.id.btntext3,
        R.id.btntext4,
        R.id.btntext5,
        R.id.btntext6,
        R.id.btntext7,
        R.id.btntext8,
        R.id.btntext9
    };
    private static int[] buttontexts_7 = new int[] {
        R.id.btntext1,
        R.id.btntext2,
        R.id.btntext3,
        R.id.btntext4,
        R.id.btntext5,
        R.id.btntext6,
        R.id.btntext7
    };
    private int icons_all[][] = new int[][] {
            null,
            icons_9,
            icons_7
    };
    
    private int labels_all[][] = new int[][] {
            null,
            labels_9,
            labels_7
    };
    private int buttons_all[][] = new int[][] {
            null,
            buttons_9,
            buttons_7
    };
    private int changebuttons_all[][] = new int[][] {
            null,
            changebuttons_9,
            changebuttons_7
    };
    private int bgs_all[][]=new int[][]{
            null,
            bgs_9,
            bgs_7
    };
    private int buttonlayouts_all[][] = new int[][] {
            null,
            buttonlayouts_9,
            buttonlayouts_7
    };
    private int buttontexts_all[][] = new int[][] {
            null,
            buttontexts_9,
            buttontexts_7
    };	
    private int labelbgs_all[][] = new int[][] {
            null,
            labelBgs_9,
            labelBgs_7
    };	    
    
    private ActivityGraphModel mModel;
    
    private String[] mLocalManualComps = null;//new String[mTopAppNumber];
    private String[] mLocalManualComps_Init = null;//new String[mTopAppNumber];
    
    private int[] iconIds = null;
    private int[] labelIds = null;
    private int[] buttonTextIds = null;
    private int[] buttonIds = null;
    private int[] changeButtonIds = null;
    private int[] bgIds=null;
    private int[] labelBgIds = null;
    private int[] buttonLayoutIds = null;    
    
    /** IKDOMINO-5441: drop pre-load app icons,use the default icons for all apps */    	
    /*private String[] mPreloadComponents = {
	"com.android.browser/.BrowserActivity",
	"com.android.email/.activity.Welcome",
	"com.android.mms/.ui.traditional.MessageLaunchActivity",
	"com.android.mms/.ui.ConversationList",
	"com.android.contacts/.DialtactsActivity",
	"com.android.contacts/.DialtactsContactsEntryActivity",
	"com.android.deskclock/.DeskClock",
	"com.android.settings/.Settings",
	"com.android.camera/.Camera",
	"com.android.calendar/.LaunchActivity",	
	"com.android.quicksearchbox/.SearchActivity",
	"com.fihtdc.filemanager/.FileBrowser",
	"com.motorola.mmsp.taskmanager/.TaskManagerActivity",
	"com.android.providers.downloads.ui/.DownloadList",
	"com.android.calculator2/.Calculator",
	"com.android.voicedialer/.VoiceDialerActivity",
       "com.fihtdc.fmradio/.FMRadio",
       "com.broadcom.bt.app.fm/.rx.FmRadio",
       "com.android.music/.VideoBrowserActivity",
       "com.android.music/.MusicBrowserActivity", 
       "com.cooliris.media/.Gallery"
	};*/
    
    /*private static Integer[] mVisualRepesIds = {
	R.drawable.browser,
	R.drawable.email,
	R.drawable.message,
	R.drawable.message,
	R.drawable.phone,
	R.drawable.contacts,
	R.drawable.clock,
	R.drawable.settings,
	R.drawable.camera,
	R.drawable.calendar,
	R.drawable.search,
	R.drawable.filemanager,
	R.drawable.taskmanager,
	R.drawable.downloads,
	R.drawable.calculator,
	R.drawable.voicedialer,
       R.drawable.fmradio,
       R.drawable.fmradio,
       R.drawable.videoplayer,
	R.drawable.music,
       R.drawable.gallery
       };
     */
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        Intent intent = getIntent();
        fromWelcome = intent.getIntExtra("welcome", 0); 
        
        if ((ActivityGraphModel.getMaxCard()!=7)&&(ActivityGraphModel.getMaxCard()!=9)){            
            Log.d(TAG,"first set card num");           
            int cardNum = intent.getIntExtra("card num", 9);  
            ActivityGraphModel.setMaxCard(cardNum);	
        }	
        mTopAppNumber = ActivityGraphModel.getMaxCard();
        mScreenSize = (mTopAppNumber == 9)?1:2;
        
        
        mLocalManualComps = new String[mTopAppNumber];
        mLocalManualComps_Init = new String[mTopAppNumber];
        
        iconIds = getIconIds();
        labelIds = getLabelIds();
        buttonTextIds = getButtonTextIds();
        buttonIds = getButtonIds();
        changeButtonIds = getChangeButtonIds();
        bgIds=getBgIds();
        buttonLayoutIds = getButtonLayoutIds();
        labelBgIds = getLabelBgIds();
        
        LayoutInflater inflater;
        inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Display display = getWindowManager().getDefaultDisplay(); 
        boolean isLandscape = display.getWidth() > display.getHeight(); 
        
        if (isLandscape) {
            Log.d(TAG, "onCreate isLandscape");  
            if(mTopAppNumber == 7){
                mContentViewLandscape = inflater.inflate(R.layout.manage_shortcuts_land_seven, null);
            } else if(mTopAppNumber == 9){
                mContentViewLandscape = inflater.inflate(R.layout.manage_shortcuts_land_nine, null);
            }
            setContentView(mContentViewLandscape);
            
        }else{
            Log.d(TAG,"onCreate isPortrait");
            if(mTopAppNumber == 7){
                Log.d(TAG,"mTopAppNumber == 7");
                mContentViewPortrait= inflater.inflate(R.layout.manage_shortcuts_seven, null);
            } else if(mTopAppNumber == 9){
                Log.d(TAG,"mTopAppNumber == 9");
                mContentViewPortrait= inflater.inflate(R.layout.manage_shortcuts_nine, null);
            }
            setContentView(mContentViewPortrait);
        }
        ActivityGraphApplication app = ((ActivityGraphApplication)getApplication());        
        mModel = app.getModel();        
        mModel.addCallback(this); 
        
        final ArrayList<ManualInfo> mManualList = ActivityGraphModel.getManualList();                     
        Log.d(TAG,"mManualList SIZE:"+ mManualList.size());
        
        Button mButtonOk = (Button)findViewById(R.id.buttonOk);    	
        mButtonOk.setOnClickListener(this);
        Button mButtonCancel = (Button)findViewById(R.id.buttonCancel);    	
        mButtonCancel.setOnClickListener(this);
        
        for (int i = 0 ; buttonIds != null && i < buttonIds.length; i++) {
            ImageButton button = (ImageButton)findViewById(buttonIds[i]);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
        for (int i = 0 ; changeButtonIds != null && i < changeButtonIds.length; i++) {
            ImageButton button = (ImageButton)findViewById(changeButtonIds[i]);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
        for (int i = 0 ; bgIds != null && i < bgIds.length; i++) {
            ImageView button = (ImageView)findViewById(bgIds[i]);
            if (button != null) {
                Log.i("++set listner","++");
                button.setOnClickListener(this);
            }
        }
        for (int i = 0 ; buttonLayoutIds != null && i < buttonLayoutIds.length; i++) {
            LinearLayout layout = (LinearLayout)findViewById(buttonLayoutIds[i]);
            if (layout != null) {
                layout.setOnClickListener(this);
            }
        }
        initLocalManualList(mManualList);
        TextView textview=(TextView)findViewById(R.id.pro_text);
        textview.setMovementMethod(ScrollingMovementMethod.getInstance());
    }
    @Override
    protected void onResume() {
        Log.d(TAG,"Manual onResume");
        super.onResume(); 
        createScreen(mLocalManualComps);
    }
    
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
                Log.d(TAG,"null view"); 
                if(mTopAppNumber == 7){
                    mContentViewLandscape = inflater.inflate(R.layout.manage_shortcuts_land_seven, null);
                } else if(mTopAppNumber == 9){
                    mContentViewLandscape = inflater.inflate(R.layout.manage_shortcuts_land_nine, null);
                }
            }
            setContentView(mContentViewLandscape);
        }else {  
            Log.d(TAG,"isPortrait");
            if(mContentViewPortrait == null){
                Log.d(TAG,"null view"); 
                if(mTopAppNumber == 7){
                    mContentViewPortrait= inflater.inflate(R.layout.manage_shortcuts_seven, null);
                } else if(mTopAppNumber == 9){
                    mContentViewPortrait= inflater.inflate(R.layout.manage_shortcuts_nine, null);
                }
                
            }
            setContentView(mContentViewPortrait);
        }
        Button mButtonOk = (Button)findViewById(R.id.buttonOk);    	
        mButtonOk.setOnClickListener(this);
        Button mButtonCancel = (Button)findViewById(R.id.buttonCancel);    	
        mButtonCancel.setOnClickListener(this);
        
        for (int i = 0 ; buttonIds != null && i < buttonIds.length; i++) {
            ImageButton button = (ImageButton)findViewById(buttonIds[i]);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
        for (int i = 0 ; changeButtonIds != null && i < changeButtonIds.length; i++) {
            ImageButton button = (ImageButton)findViewById(changeButtonIds[i]);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
        for (int i = 0 ; bgIds != null && i < bgIds.length; i++) {
            ImageView button = (ImageView)findViewById(bgIds[i]);
            if (button != null) {
                button.setOnClickListener(this);
            }
        }
        for (int i = 0 ; buttonLayoutIds != null && i < buttonLayoutIds.length; i++) {
            LinearLayout layout = (LinearLayout)findViewById(buttonLayoutIds[i]);
            if (layout != null) {
                layout.setOnClickListener(this);
            }
        }
        TextView textview=(TextView)findViewById(R.id.pro_text);
        textview.setMovementMethod(ScrollingMovementMethod.getInstance());
        createScreen(mLocalManualComps);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
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
        System.gc();
        super.onDestroy();
        
    }
    private void initLocalManualList(ArrayList<ManualInfo> manualList){
        int i;
        for (i = 0; i < mTopAppNumber; i++) {
            mLocalManualComps[i] = "";			  
        }
        
        ManualInfo info = new ManualInfo();
        for (i = 0; i < manualList.size(); i++) {
            Log.d(TAG,"manuallist:" +i+":"+manualList.get(i).mName); 
            info.mId = manualList.get(i).mId;
            info.mName =  manualList.get(i).mName;	 
            mLocalManualComps[info.mId] = info.mName;
        }
        System.arraycopy(mLocalManualComps, 0, mLocalManualComps_Init, 0, mTopAppNumber);
    }
    private void createScreen(String[] localManualComps){
        
        TextView label = null;
        TextView buttonText = null;
        ImageView icon = null;
        AppLabelIconInfo labelIconInfo = null;
        Drawable appicon = null;
        ImageButton button = null;
        ImageButton changeButton = null;
        for (int i = 0; i < mTopAppNumber; i++)          
        {
            label  = (TextView)findViewById(labelIds[i]);
            buttonText  = (TextView)findViewById(buttonTextIds[i]);
            icon = (ImageView)findViewById(iconIds[i]);
            button = (ImageButton)findViewById(buttonIds[i]);
            button.setFocusable(false);
            changeButton = (ImageButton)findViewById(changeButtonIds[i]);
            changeButton.setFocusable(false);
            label.setText("");	
            label.setBackgroundResource(0);
            buttonText.setText(R.string.btn_add);
            icon.setImageDrawable(null);
            button.setBackgroundResource(R.drawable.ic_add_big);
            changeButton.setBackgroundResource(0);
            
            if(localManualComps[i].equals(getString(R.string.none_application))) {
                Log.d(TAG,"localManualComp == null"+ i );	
            } else {
                labelIconInfo = mModel.getLabelIconInfo(localManualComps[i]);
                if (labelIconInfo != null) {
                    
                    label.setText(labelIconInfo.appname);	
                    label.setBackgroundResource(labelBgIds[i]);	    		    	           
                    buttonText.setText("");
                    appicon = labelIconInfo.appicon;		    	    	
                    changeButton.setBackgroundResource(R.drawable.ic_remove_big);
                    button.setBackgroundResource(0);
                    Log.d(TAG,"ComponentName:" + localManualComps[i]);
                    /** IKDOMINO-5441: drop pre-load app icons,use the default icons for all apps */   
//			    int mChecked = checkVisualRepresentation(localManualComps[i]);
//  	  	    	    if (mChecked!=NO_VISUAL_REPRESENTATION){
//				Log.d(TAG,"mChecked = "+mChecked);
//  	  	    	    	icon.setImageResource(mVisualRepesIds[mChecked]);				
//  	  	    	    }else{
                    Log.d(TAG,"mChecked = 0xffff");
                    icon.setImageDrawable(appicon);	
//  	  	    	    }	
                    
                }
            }
        }
    }
    
    public void onClick(View v) {
        Log.i("+++","++view id="+v.getId());
        switch (v.getId()) {
        case R.id.buttonOk:
            for (int i = 0; i < mTopAppNumber; i++) {
                if (!mLocalManualComps_Init[i].equals(mLocalManualComps[i])){
                    if((mLocalManualComps_Init[i].equals(""))&&(!mLocalManualComps[i].equals(""))){
                        Log.d(TAG,"addManualItem"+mLocalManualComps[i]);
                        ActivityGraphModel.addManualItemToDatabase(this, mLocalManualComps[i], i);
                    }
                    else if ((!mLocalManualComps_Init[i].equals(""))&&(mLocalManualComps[i].equals(""))){
                        Log.d(TAG,"removeManualItem"+mLocalManualComps_Init[i]);
                        ActivityGraphModel.removeManualItemFromDatabase(this, mLocalManualComps_Init[i], i);
                    }else {
                        Log.d(TAG,"updateManualItem"+mLocalManualComps[i]);     			      	
                        ActivityGraphModel.updateManualItemToDatabase(this, mLocalManualComps[i], i); 
                    }
                }	 
            }
            if(fromWelcome == 1){
                ContentResolver resolver = getBaseContext().getContentResolver();
                Settings.System.putInt(resolver,"box",1);
                Intent intent = new Intent("com.motorola.mmsp.activitygraph.OUT_OF_BOX");        
                sendBroadcast(intent);
                
                Activity activity = WelcomeActivity.getActivity();
                Log.d(TAG, "activity = " +activity );	
                if (activity != null) {
                    activity.finish();
                }    					    			
            } else {
                Log.d(TAG, "----send broadcast : com.motorola.mmsp.activitygraph.UPDATE_MANUAL_LIST");   
                this.sendBroadcast(new Intent("com.motorola.mmsp.activitygraph.UPDATE_MANUAL_LIST"));
            }
            finish();
            break;
            
        case R.id.buttonCancel:
            finish();
            break;
            
        default:
            Log.i("+++","++view id="+v.getId());
            int mShortcutId = 0xff;
            for (int i=0; i<mTopAppNumber; i++){
                if (buttonIds[i]== v.getId()||changeButtonIds[i]== v.getId()||buttonLayoutIds[i]== v.getId()||bgIds[i]==v.getId())
                {
                    mShortcutId = i;
                    Log.i("++","++viewId="+v.getId()+"+button id="+buttonIds[i]+"+change button="+changeButtonIds[i]+"+Bg="+bgIds[i]);
                }
            }
            if (mShortcutId != 0xff){
                Intent intent = new Intent();
                intent.setClass(this, SingleApplicationPicker.class);
                Bundle data=new Bundle();            	
                data.putInt("shortcut_id", mShortcutId);
                intent.putExtras(data);
                startActivityForResult(intent, PICK_APP);
            }
            break;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode,
            int resultCode, Intent intent) {
        Log.d(TAG,"onActivityResult");
        int mIndex;
        String mComponentName;    	
        super.onActivityResult(requestCode, resultCode, intent);
        
        if(requestCode == PICK_APP){
            if(resultCode==RESULT_CANCELED){
                //finish();
            }else if(resultCode==RESULT_OK){
                
                Bundle data = intent.getExtras();
                mIndex = data.getInt("shortcut_index");
                mComponentName = data.getString("app_componentname");
                mLocalManualComps[mIndex] = mComponentName;
                Log.d(TAG,"mLocalManualComps:"+mIndex + mComponentName);
                Log.d(TAG,"mLocalManualComps_Init:"+mIndex+mLocalManualComps_Init[0]);
                
                TextView label  = (TextView)findViewById(labelIds[mIndex]);
                TextView buttonText  = (TextView)findViewById(buttonTextIds[mIndex]);
                ImageView image  = (ImageView)findViewById(iconIds[mIndex]); 
                
            }
        }
    }
    /** IKDOMINO-5441: drop pre-load app icons,use the default icons for all apps */   
    /*private int checkVisualRepresentation(String componentName){
    		Log.d(TAG,"checkVisualRepresentation:" + componentName);

		for (int i=0; i< mPreloadComponents.length; i++){
			Log.d(TAG,"mPreloadComponents["+i+"] = " + mPreloadComponents[i]);
			if (mPreloadComponents[i].equals(componentName))
				return i;
		}
		Log.d(TAG,"return 0xffff");
		return NO_VISUAL_REPRESENTATION;		
	}*/
    public void bindAppsAdded(ArrayList<String> list) {
        
    }
    public void bindAppsDeleted(ArrayList<String> list) {
        Log.d(TAG,"bindAppsDeleted:"+list.size());	
        
        for (int j = 0; j <list.size(); j++)
            for (int i = 0; i < mTopAppNumber; i++){
                if (mLocalManualComps[i].equals(list.get(j))) {
                    Log.d(TAG,"AppsDeleted:"+list.get(j)+":"+i);	
                    mLocalManualComps[i]= "";
                }
            }
        ManageShortcuts.this.runOnUiThread(new Runnable(){  
            public void run() {  
                createScreen(mLocalManualComps);
            }  
        }); 
    }
    public void bindAppsUpdated(ArrayList<String> list) {
        
    }
}
