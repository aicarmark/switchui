/*
 * @(#)BlockGestureListener.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185        2011/03/04    NA              Initial version
 *
 */

package com.motorola.contextual.smartrules.rulesbuilder;


import java.util.Vector;
import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;
import com.motorola.contextual.smartrules.rulesbuilder.SingletonBlocksMediaPlayer.MediaPlayerType;
import com.motorola.contextual.smartrules.uipublisher.Publisher;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import 	android.view.GestureDetector.SimpleOnGestureListener;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation.AnimationListener;
import android.widget.RelativeLayout;

/**
 * @author e51185
 * This class uses the motion events filtered from main activity
 * Takes care of Flinging of Internal Block within main block
 * Each BlockLayout instantiated will have a separate BlockGestureListener
 * which through callback will listen and respond to touch based Gestures
 *
 */
public class BlockGestureListener extends SimpleOnGestureListener implements Constants {

    private static final String TAG = "BlockGestureListener";
    public static final int PAD_LEFT=18, PAD_TOP=5, PAD_RIGHT=62, PAD_BOTTOM=15; 	//these need to be in sync with action/trigger block layout
    private static final int VIBRATE_DURATION = 35;
    private static final int DISCONNECT_AUDIBLE_FEEDBACK_POS_MIN = 72;
    private static final int DISCONNECT_AUDIBLE_FEEDBACK_POS_MAX = 98;
    private static final int DISCONNECT_HAPTIC_FEEDBACK_POS_MIN = 45;
    private static final int DISCONNECT_HAPTIC_FEEDBACK_POS_MAX = 50;
    
    public View gestureView;
    private ViewGroup innerBlockContainer;
    private boolean isConnect;
	public int connectPos;
    public int disconnectPos;
    public int disp=0;
    public int releasePosX=0;
    public static float scale;
    public ViewGroup vg;
    private AlertDialog.Builder builder;
    private BlockController pcBlockController, actionBlockController;   // Object that sends out drag-drop events while a view is being moved.
    private BlockLayout pcBlockLayer, actionBlockLayer, block;             // The ViewGroup that supports drag-drop.
    private IBlocksMotionAndAnim localInstCallback;						// used to add callback to set press state in GraphicsActivity
    private Publisher blockInfoFromTag;
    private Vibrator mVibrator;
    private Context activityContext;
    public boolean traceDetachAudPlayed=false;
    public boolean traceAttachHapticPlayed=false;
    private static SingletonBlocksMediaPlayer mInstMediaPlayer;
    private AlertDialog mAlertDialog;
    
    public BlockGestureListener(View listeningView, BlockController pcController,
    							BlockController actionController,
    							BlockLayout pcLayer, BlockLayout actionLayer,
    							IBlocksMotionAndAnim instCallback,
    							Context mainActivityContext){
    	gestureView = listeningView;
    	pcBlockController = pcController;
    	actionBlockController = actionController;
    	pcBlockLayer = pcLayer;
    	actionBlockLayer = actionLayer;
    	localInstCallback = instCallback;
    	activityContext = mainActivityContext;
    	scale = mainActivityContext.getResources().getDisplayMetrics().density;
    	block = (BlockLayout)gestureView.getParent();
    	if(gestureView instanceof ViewGroup){
    		vg = (ViewGroup) gestureView;
    	}
    	innerBlockContainer = (ViewGroup) vg.findViewById(R.id.BlockContainer);
    	blockInfoFromTag = (Publisher)gestureView.getTag();
    	mVibrator = (Vibrator) mainActivityContext.getSystemService(Context.VIBRATOR_SERVICE);    	
    	
        builder = new AlertDialog.Builder(mainActivityContext);
        builder.setTitle(blockInfoFromTag.getBlockName());
        builder.setIcon(blockInfoFromTag.getImage_drawable());
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int id) {
            	innerBlockContainer.setBackgroundResource(R.drawable.sr_block_normal);
            	BlockGestureListener.setInnerViewPadding(innerBlockContainer);  
                dialog.cancel();
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {			

			public void onCancel(DialogInterface dialog) {
            	innerBlockContainer.setBackgroundResource(R.drawable.sr_block_normal);
            	BlockGestureListener.setInnerViewPadding(innerBlockContainer);  
			}
        });
        builder.setOnKeyListener(new DialogInterface.OnKeyListener() {

			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH && event.getRepeatCount() == 0) {
                	innerBlockContainer.setBackgroundResource(R.drawable.sr_block_normal);
                	BlockGestureListener.setInnerViewPadding(innerBlockContainer); 
                }
                return false; // Any other keys are still processed as normal
            }
        });
        
        connectPos = getPixelsOfDpi(20);
        disconnectPos = getPixelsOfDpi(RulesBuilderConstants.DISCONNECT_POS);
               
    }

    @Override
    public boolean onDown(MotionEvent e) {
        // TODO Auto-generated method stub
    	if(LOG_DEBUG) Log.d (TAG," Trace Downing");
    	localInstCallback.setTapState(true);
    	traceDetachAudPlayed = false; //reset every time down touch events received
    	traceAttachHapticPlayed = false;
        return false;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        // TODO Auto-generated method stub
    	if(LOG_DEBUG) Log.d (TAG," Trace Flinging");
    	
        //animView();
        //Fling disabled for now, similar fling-like feature enabled under onScroll gesture
    	//Comment being left here in case fling needs different behavior than scroll is already giving
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    	
        // TODO Auto-generated method stub
    	if(LOG_DEBUG) Log.d (TAG," Trace Long Press");
        
        final ViewGroup vg = (ViewGroup) gestureView;
        innerBlockContainer = (ViewGroup) vg.findViewById(R.id.BlockContainer);
        
        if (!gestureView.isInTouchMode()) {
            return ;
        }
        localInstCallback.setPressedState(true);
        mVibrator.vibrate(VIBRATE_DURATION);
        
        //There goes all the fun, who doesnt love Dialog and lists .. !!!
        //In future if the Blocks representing Logical Triggers and Action need to be displaced/moved
        //We need to call the startDrag api. It used to work awesome Once upon a time.
        //startDrag (vg);
        
        //builder can be null if during activity onDestroy theres a small time window where
        //the Dialog is being created
        if(builder !=null){	
	        //For showing up the adaptive list of items in AlertDialog
	        final String[] items = buildShownList();
	        builder.setItems(items, new DialogInterface.OnClickListener() {
	
				public void onClick(DialogInterface dialog, int item) {
	                //Disable toast for now, left in case if felt to be needed
	            	//Toast.makeText(GraphicsActivity.getContext(), items[item], Toast.LENGTH_SHORT).show();
	                
	                innerBlockContainer = (ViewGroup) vg.findViewById(R.id.BlockContainer);
	                
	                int w = innerBlockContainer.getWidth ();
	                int h = innerBlockContainer.getHeight ();
	                int left = innerBlockContainer.getLeft();
	                
	                if(items[item].equalsIgnoreCase(activityContext.getResources().getString(R.string.block_action_delete))){
	                	//TBD make sure this doesnt lead to any Memory leaks
	                	block.getBlockLayerCallback().onDelete(gestureView);
	                    return;
	                }
	                if(items[item].equalsIgnoreCase(activityContext.getResources().getString(R.string.block_action_edit))){
	                	block.getBlockLayerCallback().onConfigure(gestureView);
	                    innerBlockContainer.setBackgroundResource(R.drawable.sr_block_normal);
	                    //Needs to match the padding of Resource id BlockContainer in xml
	                    BlockGestureListener.setInnerViewPadding(innerBlockContainer);   
	                    
	                	return;
	                }
	                
	                if(items[item].equalsIgnoreCase(activityContext.getResources().getString(R.string.block_action_enable))){
	                	isConnect = true;
	                	releasePosX = getPixelsOfDpi(80-1);
	                	left =  disconnectPos;
	                }else if(items[item].equalsIgnoreCase(activityContext.getResources().getString(R.string.block_action_disable))){
	                	isConnect = false;
	                	releasePosX = getPixelsOfDpi(80+1);
	                	left =  connectPos;
	                	startThreadplayAudibleFeedback(MediaPlayerType.DISABLE);
	                }             
	                
	                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w,h);
	                if(LOG_DEBUG) Log.d (TAG," Scroll left n top "+ left);
	                
	                lp.setMargins(left, 0, 0, 0);    
	                ViewGroup vp = (ViewGroup)innerBlockContainer.getParent();
	                
	                //Needs to match the padding of Resource id BlockContainer in xml
	                BlockGestureListener.setInnerViewPadding(innerBlockContainer);  
	                
	                vp.updateViewLayout(innerBlockContainer, lp);
	 
	                animFromDialogView(); // not same in case of delete, come up with diff. delete animation
	            }
	        });
	        mAlertDialog = builder.create();
	        mAlertDialog.show();
        }        

        return;
        
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        // TODO Auto-generated method stub
    	if(LOG_DEBUG) Log.d (TAG," Scroll factors"+ distanceX+"\t"+ distanceY);
    	ViewGroup vg = (ViewGroup) gestureView;
    	localInstCallback.setTapState(false);
        innerBlockContainer = (ViewGroup) vg.findViewById(R.id.BlockContainer);
        ViewGroup vp = (ViewGroup)innerBlockContainer.getParent();
        vp.requestDisallowInterceptTouchEvent(true);
        
        int w = innerBlockContainer.getWidth ();
        int h = innerBlockContainer.getHeight ();
        int left = innerBlockContainer.getLeft();
        int right_margin=0;
        left = left - (int)distanceX;
        
        //if we dont do this the Relative Layout is setting the width to 1
        if( left>getPixelsOfDpi(40) )right_margin = left+w;
        
        if(left >getPixelsOfDpi(22) ){

        //check for playing sound on detach
        checkForPlayingDetachAudibleFeedback(left);
        checkForHapticFeedback(left);
        	
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w,h);
        if(LOG_DEBUG) Log.d (TAG," ScrollThis left n top "+ left);
        
        lp.setMargins(left, 0, right_margin, 0);     
        innerBlockContainer.setBackgroundResource(R.drawable.sr_block_press);         
        //Needs to match the padding of Resource id BlockContainer in xml
        BlockGestureListener.setInnerViewPadding(innerBlockContainer); 
        vp.updateViewLayout(innerBlockContainer, lp);

        }
        
        int[] location = new int[2];
    	innerBlockContainer.getLocationOnScreen( location);
    	if(LOG_DEBUG) Log.d (TAG," Touch Move Scroll Location coord : "+ location[0] +"\t" + location[1] );
    	
    	releasePosX=location[0];    	
    	
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    	ViewGroup vp = (ViewGroup)innerBlockContainer.getParent();
    	vp.requestDisallowInterceptTouchEvent(true);
    	
    	innerBlockContainer.setBackgroundResource(R.drawable.sr_block_press);
    	//Needs to match the padding of Resource id BlockContainer in xml
    	BlockGestureListener.setInnerViewPadding(innerBlockContainer); 
    }
     
    @Override
    public boolean onSingleTapUp(MotionEvent e) {

    	boolean statusRet = false;
    	if(LOG_DEBUG) Log.d (TAG," Trace Single Tap'uping");
    	
    	if(pcBlockController.doesTouchNeedsSupressed()){
    		return statusRet;
    	}
    	localInstCallback.setTapState(true);
    	
        innerBlockContainer.setBackgroundResource(R.drawable.sr_block_normal);        
        //Needs to match the padding of Resource id BlockContainer in xml
        BlockGestureListener.setInnerViewPadding(innerBlockContainer); 
        block.getBlockLayerCallback().onConfigure(gestureView);
		//Need to check for specific Block info, where Dialog gets popped from same activity and
		//we do not need to suppress/consume MotionEvents, as there is no time lag, and hence
		//does not give user the time in which he can, or by accident do multiple touch's
		if(!blockInfoFromTag.isNeedToPopDialogFromThisActivity()){
			pcBlockController.setDoesTouchNeedsSupressed(true);
		}
		
        return statusRet;
    }
    
    @Override
    public boolean onDoubleTap(MotionEvent ev){
    	if(LOG_DEBUG) Log.d (TAG," Double Tap'uping");
    	//Intentionally Blocking any Double-tap functionality, not supported as per spec

    	return true;
    }
    
    /*
     * Disabled/Not being used for now
     * Used to trigger the displacement of Blocks in a absolute coordinate space
     * Can be utilized as-is whenever this functionality needed in future
     */
    public boolean startDrag (ViewGroup v)
    {
    	//TBD Ensure this is as per design, best place to do this
        Object dragInfo = v;

        if(v.getParent().hashCode() ==  pcBlockLayer.hashCode()) {
        	if(LOG_DEBUG) Log.d (TAG, "BlockActivity, its pcDragLayer");
            pcBlockController.startDrag (v, pcBlockLayer, 
            								dragInfo, BlockController.DRAG_ACTION_MOVE);
        } else if(v.getParent().hashCode() ==  actionBlockLayer.hashCode()) {
        	if(LOG_DEBUG) Log.d (TAG,"BlockActivity, its actionDragLayer");
            actionBlockController.startDrag (v, actionBlockLayer, 
            								dragInfo, BlockController.DRAG_ACTION_MOVE);
        }
        return true;
    }
    
    /*
     * Call that takes care of animation of Blocks to enable/disable from 
     * Touch based movement of block we calculate the displacement of animation depending upon
     * where the block has been released. In case of Menu based, we just force it to
     * be leaved at point of threshold
     * 
     */
    public void animView(){
    	
    	AnimationSet animation = new AnimationSet(true);
    	
    	// if its not long press the displacement depends upon where you leave the Block
    	// This logic has been disabled, and for now, blocks have 2 states, and irrespective
    	// of where the user drags it, we toggle it in the other state. 
		// if(innerBlockContainer.getLeft() > getPixelsOfDpi(80)){
    	// Leaving condition in case needed in future 

    	if(blockInfoFromTag.getBlockConnectedStatus()){	//if its enabled then disable, vice-versa
		    isConnect = false;
		    disp = releasePosX - disconnectPos;
		}else{
		    isConnect = true;
		    disp = releasePosX - connectPos ;    		
		}
    	
    	
    	int[] location = new int[2];
    	innerBlockContainer.getLocationOnScreen( location);
    	if(LOG_DEBUG) Log.d (TAG," Touch UP Scroll Location coord : "+ location[0] +"\t" + location[1] );
    	if(LOG_DEBUG) Log.d (TAG," Disp. value of coordinates : "+ (-disp) );
    	
        TranslateAnimation translateAnimation = new TranslateAnimation(0, -disp, 0, 0);
        translateAnimation.setDuration(500);
        animation.addAnimation(translateAnimation);        

        animation.setAnimationListener(new AnimationListener() {

			public void onAnimationEnd(Animation anim) {
            	if(LOG_DEBUG) Log.d (TAG,"animView onAnimationEnd");
            	// TBD TO investigate, at times(1 in 10 times) this callback gets called twice 
            	// for every call of onAnimationStart ...
            	 
            	innerBlockContainer.clearAnimation();
            	
            	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams
                (innerBlockContainer.getWidth(), innerBlockContainer.getHeight());
            	
            	if(isConnect) {
                	lp.setMargins(connectPos, 0, 0, 0);
                	checkForPlayingAttachAudibleFeedback();
                }else{
                	lp.setMargins(disconnectPos, 0, 0, 0);
                }
                
                ViewGroup vp = (ViewGroup)innerBlockContainer.getParent();
                innerBlockContainer.setBackgroundResource(R.drawable.sr_block_normal);
                
                //Needs to match the padding of Resource id BlockContainer in xml
                BlockGestureListener.setInnerViewPadding(innerBlockContainer);
                vp.updateViewLayout(innerBlockContainer, lp);

            }

			public void onAnimationRepeat(Animation arg0) {}
            
			public void onAnimationStart(Animation arg0) {
            	mInstMediaPlayer = SingletonBlocksMediaPlayer.getBlocksMediaPlayerInst(activityContext);
            	if(LOG_DEBUG) Log.d (TAG,"animView onAnimationStart");
            	
            }
            
        });
        
        innerBlockContainer.startAnimation(animation);
        block.getBlockLayerCallback().onConnected(gestureView, isConnect);
    	
    }

    /*
     * Call that takes care of animation of Blocks to enable/disable from 
     * AlertDialog list based interface, we just force it to
     * be leaved at point of threshold where it would have been otherwise left 
     * had it been moved though touch. For now this threshold is 80pixels
     * 
     */
    public void animFromDialogView(){
    	if(LOG_DEBUG) Log.d (TAG,"animFromDialogView onAnimationEnd");
    	AnimationSet animation = new AnimationSet(true);
    	int[] location = new int[2];
    	innerBlockContainer.getLocationOnScreen( location);
    	if(LOG_DEBUG) Log.d (TAG," Touch Move Scroll Location coord : "+ location[0] +"\t" + location[1] );
    	
    	releasePosX=location[0];
    	
        // Try to get current position and then translate from here to end
        // or from the End to Connect/Disconnect (depending upon where u leave it )
    	if(!isConnect){
    		disp = (releasePosX - disconnectPos);
    	}else{
    		disp = (releasePosX - connectPos) ;    		
    	}
    	
        TranslateAnimation translateAnimation = new TranslateAnimation(0, -disp, 0, 0);
    	
        translateAnimation.setDuration(500);
        animation.addAnimation(translateAnimation);
        

        animation.setAnimationListener(new AnimationListener() {

			public void onAnimationEnd(Animation anim) {

            	innerBlockContainer.clearAnimation();
            	
            	RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams
                (innerBlockContainer.getWidth(), innerBlockContainer.getHeight());
            	
            	if(isConnect) {
                	lp.setMargins(connectPos, 0, 0, 0);
                	startThreadplayAudibleFeedback(MediaPlayerType.ENABLE);
                	
                }else{
                	lp.setMargins(disconnectPos, 0, 0, 0);
                }
                
                ViewGroup vp = (ViewGroup)innerBlockContainer.getParent();
                innerBlockContainer.setBackgroundResource(R.drawable.sr_block_normal);
                
                //Needs to match the padding of Resource id BlockContainer in xml
                BlockGestureListener.setInnerViewPadding(innerBlockContainer); 
                vp.updateViewLayout(innerBlockContainer, lp);
                
                innerBlockContainer.setBackgroundResource(R.drawable.sr_block_normal);

            }

			public void onAnimationRepeat(Animation arg0) {}
            
			public void onAnimationStart(Animation arg0) {
            	mInstMediaPlayer = SingletonBlocksMediaPlayer.getBlocksMediaPlayerInst(activityContext);
            	if(LOG_DEBUG) Log.d (TAG,"animFromDialogView onAnimationStart");
            }
            
        });
        
        innerBlockContainer.startAnimation(animation);
        block.getBlockLayerCallback().onConnected(gestureView, isConnect);
    	
    }
    
	/*
	 * Utility function to convert matching dpi's from xml for layout to pixel values
	 * that can be used in calls which take pixel values
	 */
    public static int getPixelsOfDpi(float dpi){
    	return (int) (dpi * scale + 0.5f);
    }
    
    /*
     * This adaptively builds the list to be displayed
     * in the options dialog displayed upon long press
     * Need to build this list every time on long press as the Connect status of blocks could have changed
     */ 
    private String[] buildShownList() {
        Vector<String> result = new Vector<String>();
        	if(blockInfoFromTag.getIntentUriString() != null){
        		result.add(activityContext.getResources().getString(R.string.block_action_edit));
        	}
        	if(blockInfoFromTag.getBlockConnectedStatus()){            
        		result.add(activityContext.getResources().getString(R.string.block_action_disable));
		    }else{
		    	result.add(activityContext.getResources().getString(R.string.block_action_enable));
		    }
            result.add(activityContext.getResources().getString(R.string.block_action_delete));

        return result.toArray(new String[result.size()]);
       
    }
    
    /*
     * Getters and Setters for isConnect from BlockGestureListener instance
     * External calls for any Animations needs to query/decide this flag before
     * calling upon animation model, as this decides the direction of Blocks (Enable/Disable)
     */
    public boolean isConnect() {
		return isConnect;
	}

	public void setConnect(boolean isConnect) {
		this.isConnect = isConnect;
	}
	
	public static void setInnerViewPadding(ViewGroup inner_block){
		inner_block.setPadding(getPixelsOfDpi(BlockGestureListener.PAD_LEFT), getPixelsOfDpi(BlockGestureListener.PAD_TOP),
				getPixelsOfDpi(BlockGestureListener.PAD_RIGHT), getPixelsOfDpi(BlockGestureListener.PAD_BOTTOM));
	}
	
	/**
	 * This facilitates checking for region in which scroll motion would be,
	 * to determine if a detach audible feedback is required
	 * 
	 * @param left_pos	position from left in pixels
	 */
	private void checkForPlayingDetachAudibleFeedback(int left_pos){
    	if(LOG_DEBUG) Log.d (TAG," Audible detach sound playing at: "+ left_pos + 
    									"was disabled= "+ blockInfoFromTag.isDisabled());
    	if(!blockInfoFromTag.isDisabled() && !traceDetachAudPlayed &&
    			(DISCONNECT_AUDIBLE_FEEDBACK_POS_MIN<left_pos && left_pos<DISCONNECT_AUDIBLE_FEEDBACK_POS_MAX)){
    		//falls in here if it was enabled and being moved towards right, i.e. being moved for disabling
    		traceDetachAudPlayed = true;
    		startThreadplayAudibleFeedback(MediaPlayerType.DISABLE);
    	}
	}
	
	/**
	 * This facilitates avoiding any duplicate audible feedback,
	 * owing to multiple touches, interactions from user
	 * 
	 */
	private void checkForPlayingAttachAudibleFeedback(){
    	if(LOG_DEBUG) Log.d (TAG," Audible attach sound playing was disabled= "+ 
    								blockInfoFromTag.isDisabled());
    	if( !traceDetachAudPlayed ){
    		// falls in here if it was disabled and being moved towards left, i.e. being moved to enable
    		traceDetachAudPlayed = true;
    		startThreadplayAudibleFeedback(MediaPlayerType.ENABLE);
    	}
	}
	
	/**
	 * This facilitates checking for region in which scroll motion would be,
	 * to determine if a attach haptic feedback is required
	 * 
	 * @param left_pos	position from left in pixels
	 */
	private void checkForHapticFeedback(int left_pos){
    	if(LOG_DEBUG) Log.d (TAG," Audible attach haptic playing at: "+ left_pos + "was enabled= "+ 
    									blockInfoFromTag.isDisabled());
    	if((DISCONNECT_HAPTIC_FEEDBACK_POS_MIN<left_pos && left_pos<DISCONNECT_HAPTIC_FEEDBACK_POS_MAX)){
    		//falls in here if it was disabled and being moved towards left, and bumps to the connector on left
    		traceAttachHapticPlayed = true;
    		mVibrator.vibrate(VIBRATE_DURATION);    		
    	}
	}
	
	/** 
	  * This forks a thread for setting the MediaPlayer instances
	  */
	    private void startThreadplayAudibleFeedback(final MediaPlayerType mpType)
	    {
	        Thread thread = new Thread() {
	        		public void run() {
	        			try {
	        				playAudibleFeedback(mpType);
	        	   		} catch (Exception e) {
	        	   			Log.e(TAG, "Exception while initializing MediaPlayer ");
	        	   			e.printStackTrace();
	        	   		} 
	        		}
	        	};
	        	thread.setPriority(Thread.NORM_PRIORITY-1);
	        	thread.start();
	    }	
	
	/**
	 * This plays the audible attach/detach/error tones in specific cases
	 * 
	 * @param MediaPlayerType mpType (MediaPlayerType.DISABLE/MediaPlayerType.ENABLE/MediaPlayerType.ERROR)
	 */
	public void playAudibleFeedback(MediaPlayerType mpType){
		mInstMediaPlayer = SingletonBlocksMediaPlayer.getBlocksMediaPlayerInst(activityContext);
		if(null!=mInstMediaPlayer){
        	try{ 
        		if(null != mInstMediaPlayer.getCorrespondingMediaPlayer(mpType)){
        			if(LOG_DEBUG) Log.d (TAG,"Is MediaPlayer sound instance in Start state/is it already playing " +
        					mInstMediaPlayer.getCorrespondingMediaPlayer(mpType).isPlaying() );
        			if(mInstMediaPlayer.getCorrespondingMediaPlayer(mpType).isPlaying()){
        				mInstMediaPlayer.getCorrespondingMediaPlayer(mpType).seekTo(0);
        			}else{
        				mInstMediaPlayer.getCorrespondingMediaPlayer(mpType).start();
        			}
        		}else{
        			if(LOG_DEBUG) Log.d (TAG,"animView Sounds not working owing to Singleton media player instance " );
        		}
            }catch (IllegalStateException illStException){
            	illStException.printStackTrace();
            }
    	}
	}
	
	/**
	 * Dismiss AlertDialog, to avoid Window leaks on activity pause/destroy
	 */
	public void dimissDialog(){
		if(mAlertDialog != null) mAlertDialog.dismiss();
		builder = null;
	}
	
	/**
	 * Called from Activity when parent of the BlockLayout will need to
	 * re-measure its child, specifically Text view so that it can re-draw
	 * the TextView according to the size of new description
	 */
	public void updateLayoutOnReconfigure(){
		ViewGroup vg = (ViewGroup)innerBlockContainer.getParent();
		ViewGroup innerBlockContainer = (ViewGroup) vg.findViewById(R.id.BlockContainer);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams
	                (innerBlockContainer.getWidth(), innerBlockContainer.getHeight());
		if(isConnect()){
			lp.setMargins(connectPos, 0, 0, 0);
		}else{
			lp.setMargins(disconnectPos, 0, 0, 0);
		}			
		vg.updateViewLayout(innerBlockContainer, lp);
	}
	
	/**
     * From the Listener callback we can use this to enable/disable touch events
     * passing true would disable touch events, passing false would enable touch events
     *
     * @param isTouchSuppressedNeeded
     */
    protected void setTouchSuppressed(boolean isTouchSuppressedNeeded){
            pcBlockController.setDoesTouchNeedsSupressed(isTouchSuppressedNeeded);
    }

}

