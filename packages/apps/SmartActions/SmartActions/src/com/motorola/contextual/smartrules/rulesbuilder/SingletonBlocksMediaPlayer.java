/*
 * @(#)SingletonBlocksMediaPlayer.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185        2011/10/24    NA              Initial version
 *
 */

package com.motorola.contextual.smartrules.rulesbuilder;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.R;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

/*
 * Singleton instance of this class is used to instantiate a non-lazy(awake)
 * instances of MediaPlayer, which will be released upon onPause/onDestroy
 * of parent activity, and will be instantiated on onCreate/onResume
 * 
 * TODO Further improvement: needs to implement an enforcement on instantiation
 * before referencing, currently its just guarded by non-null checks, if someone
 * violates this we just avoid accessing those references
 * MediaPlayer.prepare needs time, so we call it onAnimationStart of Animation and actually
 * play the sound in onAnimationFinish. Thus leaving a scope that activity might have a small
 * window of phasing out in between. 
 * Not a major issue,  in above case just the sound will not play. 
 */
public class SingletonBlocksMediaPlayer implements Constants{
	
	private static SingletonBlocksMediaPlayer mInstMediaPlayer;
	private MediaPlayer mPlayInstForEnable;
	private MediaPlayer mPlayInstForDisable;
	private MediaPlayer mPlayInstForError;
	
	private static final String TAG = "SingletonBlocksMediaPlayer";
	
	private SingletonBlocksMediaPlayer(Context appContext){
		// For now there are only 2 supported, later more can be added here
		// and will have to be also supported in the getInstanceOfMediaPlayer api
		mPlayInstForEnable = MediaPlayer.create(appContext, MediaPlayerType.ENABLE.resId);		
		mPlayInstForDisable = MediaPlayer.create(appContext,MediaPlayerType.DISABLE.resId);
		mPlayInstForError = MediaPlayer.create(appContext,MediaPlayerType.ERROR.resId);
	}
	
	public static synchronized SingletonBlocksMediaPlayer getBlocksMediaPlayerInst(Context appContext){
		if( null == mInstMediaPlayer){
			mInstMediaPlayer = new SingletonBlocksMediaPlayer(appContext);
			if(LOG_DEBUG) Log.d (TAG," Singleton constructor invoked, MediaPlayer objects created " );
		}		
		return mInstMediaPlayer;
	}
	
	public MediaPlayer getCorrespondingMediaPlayer(MediaPlayerType mpType){
		MediaPlayer ret = null;
		switch(mpType){
			case ENABLE:{
				ret = mPlayInstForEnable;
				break;
			}
				
			case DISABLE:{
				ret = mPlayInstForDisable;
				break;
			}
			
			case ERROR:{
				ret = mPlayInstForError;
				break;
			}
			
			default:{
				ret = mPlayInstForError;
				break;
			}
		}
		
		return ret;		
	}
	
	public static enum MediaPlayerType{
		ENABLE (R.raw.block_connect), 
		DISABLE(R.raw.block_disconnect),
		ERROR(R.raw.deny1);		//tbd need diff sound media for this
		
		// For now we support only these, more can be added to this
		// with corresponding code to supplement in the constructor
		private int resId;
		MediaPlayerType(int res){
			this.resId = res;
		}
		
		public int getResId(){
			return this.resId;
		} 
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
	
	/*
	 * release specific MediaPlayer instances
	 * 
	 * @param 
	 */
	public void releaseMediaPlayerInstance(MediaPlayer mpInst) {
		if(null!= mpInst){
		    // Note isPlaying() is throwing IllegalStateException if the MediaPlayer
		    // is still being created, although the documentation does not specify this.
		    // There was a similar case with reset call; Please see http://idart.mot.com/browse/IKJBREL1-1623
		    try {
		        if (mpInst.isPlaying()) mpInst.stop();
		    } catch (IllegalStateException e) {
		        Log.e(TAG, "Received IllegalStateException trying to stop MediaPlayer");
            }
			mpInst.release();
			mpInst = null;
		}
		
		mInstMediaPlayer = null;
		if(LOG_DEBUG) Log.d (TAG," Singleton MediaPlayer object destroyed"+
									"constructor should be called upon before further referencing" );
	}
	
	public void stopMediaPlayerInstances(){
		if(mInstMediaPlayer!=null){
			mPlayInstForEnable.stop();
			mPlayInstForDisable.stop();
			mPlayInstForError.stop();
		}
		if(LOG_DEBUG) Log.d (TAG," Singleton MediaPlayer instances stopped");
	}
	
	/*
	 * Iteratively call each instance of MediaPlayer to release the resources
	 */
	public void releaseMediaPlayerInstances() {
		releaseMediaPlayerInstance(mPlayInstForEnable);
		releaseMediaPlayerInstance(mPlayInstForDisable);
		releaseMediaPlayerInstance(mPlayInstForError);
	}
	
	public void setErrorListeners(){
		if(mInstMediaPlayer!=null){
			mPlayInstForEnable.setOnErrorListener(new MediaPlayer.OnErrorListener(){

				public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
					if(LOG_DEBUG) Log.d (TAG," mPlayInstForEnable onError()" + " what= " +arg1+ " extra= "+ arg2);
					return false;
				}
				
			});
			mPlayInstForDisable.setOnErrorListener(new MediaPlayer.OnErrorListener(){

				public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
					if(LOG_DEBUG) Log.d (TAG," mPlayInstForDisable onError()" + " what= " +arg1+ " extra= "+ arg2);
					return false;
				}
				
			});
		}
	}

}
