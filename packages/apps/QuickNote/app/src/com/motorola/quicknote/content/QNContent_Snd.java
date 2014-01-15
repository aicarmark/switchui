/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * 
 * ***************************************************************************************
 * [QuickNote] Sound Content Viewer main.
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 17 : first created - template.
 * 
 * 
 *****************************************************************************************/


package com.motorola.quicknote.content;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
//import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
//import android.util.Log;

import com.motorola.quicknote.QNActivity;
import com.motorola.quicknote.QNConstants._BackgroundRes;
import com.motorola.quicknote.QNDev;
import com.motorola.quicknote.QNUtil;
import com.motorola.quicknote.QNSetReminder;
import com.motorola.quicknote.R;

import android.telephony.TelephonyManager;
import android.telephony.PhoneStateListener;
//import com.motorola.android.telephony.SecondaryTelephonyManager;

import java.util.Calendar;   

public class QNContent_Snd extends QNContent 
{
	/****************************
     * Constants
     ****************************/
    private static final String TAG               = "QNContent_Snd";
    
    private static final int    _UI_UPDATE_PERIOD  = 1000; //milliseconds
    
    private static final int    _PERIODIC_REFRESH  = 1;
    private static final int    _ONETIME_REFRESH   = 2;
    
    private OnStateListener     _state_listener    = null;
    private ViewGroup           _dv                = null;  // progress bar
    private Bitmap widgetBitmap = null;
    
    /****************************
     * Player Modules
     ****************************/
    private MediaPlayer _player     = null;
    // indicates that player has been initialized
    private int         _duration   = -1;
    private NotestateE  _state      = NotestateE.IDLE;
    
    private Handler     _progress_handler = null;

    private Context mContext;
    private boolean isPausedByIncomingCall = false;
    private TelephonyManager mPhone;
  //  private SecondaryTelephonyManager mSecondaryPhone;
    private int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
  //  private int mSecondaryPhoneState = SecondaryTelephonyManager.CALL_STATE_IDLE;

    private SeekBar mSeekBar = null;
    private TextView mPlayedTime = null;
    private TextView mDuration = null;
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();

        //remove the listener for call state changes
        if (mPhone != null) {
            mPhone.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
        }
/*
        if (mSecondaryPhone != null) {
            mSecondaryPhone.listen(mSecondaryPhoneListener, PhoneStateListener.LISTEN_NONE);
        }
*/
    }
    
    private boolean _initPlayer(String path){
        QNDev.log(TAG+"try to init player");
        QNDev.qnAssert(null != path && null == _player && NotestateE.IDLE == state());
        _state(NotestateE.SETTINGUP);
        
        try {
            _player = new MediaPlayer();
            _player.reset();
            
            // listener - especially error listener - should be set before calling any other function!!
            _player.setOnCompletionListener( new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    QNDev.log("QNContent_Snd : on Complete");
                    _state(NotestateE.PAUSED);
                    //update the progress bar again for the mediaplay bug
                    _remove_refresh();
                    _detailView_update_progress(_duration);
                  if(null != _dv) {
                   // it is paused, set the play image
                    ImageButton imagePlay = (ImageButton)_dv.findViewById(R.id.playpause);
                    imagePlay.setImageResource(R.drawable.btn_playback_ic_play_small);
                  }
                }
            });
            
            _player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    QNDev.log("QNContent_Snd : onError s " );
                    QNDev.log("QNContent_Snd : what : " + what + " / " + extra );
                    _state(NotestateE.ERROR);
                    //QNDev.qnAssert(false);
                    return true;
               }
            });
            
            _player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    // Document says that "MediaPlayer::prepare() works syncronously"
                    // But, even though there is no error, this callback is not called before end of 'prepare()' function..
                    // we should walkaround it!
                    QNDev.log("QNContent_Snd : On Prepare Listener");
                    ; // _state(NotestateE.SETUP);
                }
            });

            _player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            _player.setDataSource(path);
            _player.prepare();
            
            // This is walkaround of MediaPlayer::prepare() function
            // (callback is not called even though this is syncrhronous call!)
            _state(NotestateE.SETUP);
        } catch (Exception e) {
            QNDev.qnAssert(false);
            _state(NotestateE.ERROR);
            return false;
        }
        return true;
    }
    
    private boolean _closePlayer() {
        QNDev.log(TAG+"try to close player");
        QNDev.qnAssert(NotestateE.IDLE == state());
        if(null != _player) {
            _player.release();
            _player = null;
            return true;
        } else {
            return false;
        }
    }

    private void _remove_refresh() {
        _progress_handler.removeMessages(_PERIODIC_REFRESH);
        _progress_handler.removeMessages(_ONETIME_REFRESH);
    }
    
    /**
     * Get next refesh time.
     */
    private int  _next_refresh(int current_time) {
        int delay = _duration - current_time;
        if(delay > _UI_UPDATE_PERIOD) { delay = _UI_UPDATE_PERIOD; }
        return delay;
    }
    
    private void _queue_next_refresh(int msg_what, int delay) {
        if(NotestateE.STARTED == state()
            || NotestateE.IDLE == state()) {
            QNDev.qnAssert(null != _progress_handler && delay >= 0);
          try {
            Message msg = _progress_handler.obtainMessage(msg_what);
            _remove_refresh();
            if(0 < delay) {
                _progress_handler.sendMessageDelayed(msg, delay);
            } else if (0 == delay){
                // send message immediately
                _progress_handler.sendMessage(msg);
            }  else {
                QNDev.qnAssert(false); // unexpected.
            }
          } catch (Exception e) {
             //do nothing;
          }
         }
    }
        
    /******************************
     * Receivers & Handlers
     ******************************/
    QNContent_Snd(String mimetype, Uri uri) {
        super(mimetype, uri);
        QNDev.qnAssert(null != uri && uri.getScheme().equals("file") );
    }
    
    public void setContext(Context context) {
        mContext = context;
    }

    private NotestateE _state(NotestateE newstate) { 
        NotestateE sv = _state; 
        _state = newstate;

        if( null != _state_listener && sv != newstate) {
            QNDev.log("QNContent Snd : State [" + sv.toString() + "] => [" + newstate.toString() + "]");
            _state_listener.OnState(sv, newstate, isPausedByIncomingCall);
        }
        
        // Update View if needed!!
        if(null != _dv && null != _progress_handler && sv != newstate) {
            if(NotestateE.SETUP == newstate 
               || NotestateE.IDLE == newstate
               || NotestateE.PAUSED == newstate) {
                _queue_next_refresh(_ONETIME_REFRESH, 0);
            } else if (NotestateE.STARTED == newstate) {
                _queue_next_refresh(_PERIODIC_REFRESH, 0);
            } else {
                ; // FIXME : we don't consider other cases until now!!!
            }
        }
        return sv; 
    }
    
    
    @Override
    public boolean close() {
      try {
        if(null != _progress_handler) {
            _remove_refresh();
        }
    	if( _player != null){
    	    if(NotestateE.PAUSED == state() 
    	       || NotestateE.STARTED == state()
    	       || NotestateE.SETUP == state() ) {
    	        stop();
    	    }
    	    _closePlayer();
    	}
    	return true;
      } catch (Exception e) {
        return false;
      }
    }
   
    /* -- obsolete
    @Override
    public int Type() { return TYPE_SND; }
    */
   
    @Override
    public SharingTypeE[] shared_by() {
        SharingTypeE[] st = new SharingTypeE[2];
        st[0] = SharingTypeE.MESSAGE;
        st[1] = SharingTypeE.EMAIL;
        return st;
    }
    


    /**
     * 
     * @return position value
     */
    private int _detailView_update_progress(int myPos) {
        //ProgressBar pb = (ProgressBar)_dv.findViewById(R.id.voice_progress_bar);
        
        // FIXME : Assumption
        //          _player.getCurrentPosition() always return expected value.
        //          (QNContent_Snd class is in charge of make it work!
        //             - that is, in charge of handling 'State' )
        int pos = _player.getCurrentPosition();
        
        //QNDev.log(TAG+"myPos = "+myPos+"  _duration = "+_duration);
        if (myPos != -1) {
            //reset the pos, only use it for the bug in the progress bar when the voice is complete 
            pos = myPos;
        } 

        
        QNDev.log("[Progress] : (" + _duration + ") MediaPlayer pos : " + pos);
        // FIXME : Walkaround.
        //          MediaPlayer may return position that is larger than duration.(Bug of MediaPlayer???)
        //          So, we need to check it!
        
        String mCurrentTime = mPlayedTime.getText().toString();
        String mDurationTime = mDuration.getText().toString();

        if (pos > _duration) {
            pos = _duration;
        }
        mSeekBar.setProgress(_player.getCurrentPosition());
        //need set the progress if it is the maxlength, otherwise the seekBar may not in the end
	if (mCurrentTime.equals(mDurationTime)) {
		//pb.setProgress(pb.getMax());
                mSeekBar.setProgress(mSeekBar.getMax());
               // mSeekBar.setVisibility(View.GONE);
	}/* else {
                //int progress = pos * pb.getMax() / _duration;
                
		//pb.setProgress(progress);
                int progress = pos * mSeekBar.getMax() / _duration;
             //   mSeekBar.setVisibility(View.VISIBLE);
                mSeekBar.setProgress(progress);
	}
//        if (pos > _duration) { pos = _duration; }
//        pb.setProgress(pos * pb.getMax() / _duration);*/
        mPlayedTime.setText(QNUtil.makeTimeString(_dv.getContext(), pos / 1000));
        return pos;
    }
    

    @Override
    public View noteView(Context context, ViewGroup parent) {
      _dv = parent;
      if (null == context || null == parent || null == _player) { return null;}
        //set edit_voice_duration
        ((TextView)parent.findViewById(R.id.edit_voice_duration)).setText(QNUtil.makeTimeString(context, _player.getDuration() / 1000));
         //set detail voice parameters
       try {
         if (mContext != null) {
            //listening for call state changed
            mPhone  = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            mPhoneState = mPhone.getCallState();
            QNDev.log("initPlayer(), Current mPhoneState = " + mPhoneState);
            mPhone.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
            
         /*   mSecondaryPhone = (SecondaryTelephonyManager) mContext.getSystemService("phone2");
            if (mSecondaryPhone != null ) {
                mSecondaryPhoneState = mSecondaryPhone.getCallState();
                QNDev.log("initPlayer(), Current mSecondaryPhoneState = " + mSecondaryPhoneState);
                mSecondaryPhone.listen(mSecondaryPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
            }
         */
        }

        if (fUsable) {
          if (null == context || null == _player ) { return null;}

         // ((ProgressBar)parent.findViewById(R.id.voice_progress_bar)).setMax(100);
          mSeekBar = (SeekBar)_dv.findViewById(android.R.id.progress);

          mSeekBar.setMax(_player.getDuration());
          mSeekBar.setProgress(0);
          mSeekBar.setOnSeekBarChangeListener(mSeekListener);
        
          if(NotestateE.STARTED == state() || NotestateE.PAUSED == state()) {
            _detailView_update_progress(-1);
          }
          
          // update duration
          mDuration = ((TextView)parent.findViewById(R.id.duration));
          mPlayedTime = ((TextView)parent.findViewById(R.id.playedtime));
          mDuration.setText(QNUtil.makeTimeString(context, _duration / 1000));
          mPlayedTime.setText(QNUtil.makeTimeString(context, 0));
          
          final ImageButton imagePlay = (ImageButton)_dv.findViewById(R.id.playpause);

          imagePlay.setOnClickListener(new View.OnClickListener() {
             public void onClick(View view) {            
               if (NotestateE.IDLE == state() 
                   || NotestateE.SETUP == state()
                   || NotestateE.PAUSED == state()) {
                  // current state is stopped or  paused, so start to play
                  start();
                  // the audio is playing, set the pause image
                  imagePlay.setImageResource(R.drawable.btn_playback_ic_pause_small);
                } else if (NotestateE.STARTED == state()){
                   // current state is started, so change it to pause
                   pause();
                   // it is paused, set the play image
                   imagePlay.setImageResource(R.drawable.btn_playback_ic_play_small);
                }
             }
          });  
          // We need progress handler in detail view!
          _progress_handler = new Handler() {
              @Override
              public void handleMessage(Message msg) {
                  QNDev.logi(TAG, "handleMessage msg : " + msg);
                  switch (msg.what) {
                      case _PERIODIC_REFRESH: 
                      case _ONETIME_REFRESH: {
                         try {
                            QNDev.qnAssert(null != _dv && NotestateE.ERROR != state() && _duration >= 0);
                            int pos = _detailView_update_progress(-1);
                            if(_PERIODIC_REFRESH == msg.what && pos < _duration ) {
                                _queue_next_refresh(_PERIODIC_REFRESH, _next_refresh(pos));
                            }
                          } catch (Exception e) {
                             //do nothing here now, this error is usually caused by SD card is unmounted
                          }
                        } break;

                      default:
                        super.handleMessage(msg);
                  }
              }
          };
          return _dv;
        } else {
           //the voice file is not usable, so just show error info.
           fUsable = false;
           return null;
        }
     } catch  (Exception ex) {
         QNDev.log(TAG+"detailView catch error = "+ex);
         return null;
     } catch (java.lang.OutOfMemoryError e) {
         QNDev.log(TAG+"detailView catch out of memory error = "+e);
         return null;
     }

    }

    public void validateViewState(){
    	if(_player != null){
    		if(mSeekBar.getProgress() != _player.getCurrentPosition()){
    			mSeekBar.setProgress(_player.getCurrentPosition());
    		}
    	}    	
    }

    private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        //to prevent the user drag the seekbar
      //  int originalProgress;

        public void onStartTrackingTouch(SeekBar seekBar) {
            mSeeking = true;
            //originalProgress = seekBar.getProgress();
        }
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromuser) {
            if (!fromuser) {
                return;
            } /*else {
                //reset the seekbar to original one
                seekBar.setProgress( originalProgress);
            }*/
           //keep below code in case we may support seekbar draging in future
            _player.seekTo(progress);
            mPlayedTime.setText((QNUtil.makeTimeString(_dv.getContext(), progress / 1000)));
        }
        public void onStopTrackingTouch(SeekBar bar) {
            mSeeking = false;
        }
    };


    @Override
    public Bitmap widgetBitmap(Context context, int width, int height, int bgcolor) {
        //for sound png, we just use the original png, not use inSampleSize currently
        if (null != widgetBitmap) {widgetBitmap.recycle(); }
        widgetBitmap = BitmapFactory.decodeResource(context.getResources(), _BackgroundRes.find_drawable(bgcolor, "thumb", "audio"));
        return widgetBitmap;

    /*    try {
           //get the size of the bitmap
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            opts.inSampleSize = 1; 
            BitmapFactory.decodeResource(context.getResources(), BGColorMapThumbVoice.find_drawable(bgcolor), opts);
     
            // request a reasonably sized output image,
            while (opts.outHeight > height || opts.outWidth > width) {
              opts.outHeight /= 2;
              opts.outWidth /= 2;
              opts.inSampleSize *= 2;
            }
            
            //get the image for real now
            QNDev.log("QNContent_Snd"+"inSampleSize = "+opts.inSampleSize);
            opts.inJustDecodeBounds = false;
            Bitmap widgetBitmap = BitmapFactory.decodeResource(context.getResources(), BGColorMapThumbVoice.find_drawable(bgcolor), opts);
            return widgetBitmap;
       } catch (Exception e) {
            return null;
       }
     */
    }

    @Override
    public Bitmap widgetBitmap(Context context, int width, int height)
    {
       return widgetBitmap(context, width, height, _BackgroundRes.GRIDME.column());
    }
    
    @Override
    public QNContent register_OnStateListener(OnStateListener listener) {
        _state_listener = listener;
        return this; 
    }
    
    @Override
    public boolean setup() {
     try {
        QNDev.qnAssert(null != uri() && NotestateE.IDLE == state());
        _initPlayer(uri().getPath());
        if(NotestateE.SETUP == state()) {
            QNDev.qnAssert(null != _player);
            _duration = _player.getDuration();
            QNDev.qnAssert(_duration >= 0);
            fUsable = true; 
            return true;
        } else {
            fUsable = false;
            return false;
        }
      } catch (Exception e) {
            fUsable = false;
            return false;
      }
    }
    
    @Override
    public boolean start() {
    	QNDev.logi(TAG, "play s");
        /** workaround solution
            when the play is stop, it cannot be start when the state is IDLE
            so call prepare() to set the state to SETUP again
          **/
      try {
        if (NotestateE.IDLE == state() ) {
               _player.prepare();
            
               _state(NotestateE.SETUP);
        }
    	if(NotestateE.SETUP == state() 
           || NotestateE.PAUSED == state()) {
            _player.start();
            _state(NotestateE.STARTED);
            return true;
    	} else {
    	    QNDev.qnAssert(false); // unexpected
    	    return false;
    	}
      } catch (Exception e) {
        return false;
      }
    }
    
    public boolean seek(long pos) {
      try {
        if(NotestateE.SETUP == state() 
           || NotestateE.STARTED == state() 
           || NotestateE.PAUSED == state() ) {
            if (pos < 0) { pos = 0; }
            if (pos > _duration) { pos = _duration; }
            _player.seekTo((int)pos);
            mPlayedTime.setText((QNUtil.makeTimeString(_dv.getContext(), pos / 1000)));

            return true;
        } else {
            QNDev.qnAssert(false); // unexpected
            return false;
        }
      } catch (Exception e) {
        return false;
      }
    }
    
    @Override
    public boolean pause() {
      try {
        if (NotestateE.STARTED == state()) {
	        _player.pause();
	        _state(NotestateE.PAUSED);
	        return true;
	    } else {
	        return false;
	    }
      } catch (Exception e) {
        return false;
      }
    }

    @Override
    public boolean stop() {
      try {
        boolean ret = true;
        if(NotestateE.SETUP == state() 
           || NotestateE.STARTED == state()
           || NotestateE.PAUSED == state() ) {
            _player.stop();
            _state(NotestateE.IDLE);
        } else if(NotestateE.IDLE == state()) {
            ; // do nothing... already stopped
            
        } else {
            QNDev.qnAssert(false); // unexpected
            ret = false;
        }
        return ret;
      } catch (Exception e) {
        return false;
      }
    }

	@Override
	public boolean isPlayable() {
		return true;
	}

    @Override
    public boolean isBGColor() {
        return true;
    }
	@Override
	public NotestateE state() {
		return _state;
	}
       
    @Override
    public CharSequence getTextNoteContent()
    {return "";}

    private PhoneStateListener mPhoneListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    QNDev.logd(TAG, "mPhoneListener received call state change:CALL_STATE_OFFHOOK");
                    mPhoneState = state;
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    QNDev.logd(TAG, "mPhoneListener received call state change:CALL_STATE_RINGING");
                    if(mPhoneState == TelephonyManager.CALL_STATE_IDLE
                     /*  && mSecondaryPhoneState == SecondaryTelephonyManager.CALL_STATE_IDLE */) {
                        QNDev.logd(TAG, "will pause voice note !");
                        if (NotestateE.STARTED == state()) {
                            isPausedByIncomingCall = true;
                            pause();
                        }
                    } else {
                        QNDev.logd(TAG, "mPhoneState or mSecondaryPhoneState is not idle!");
                    }
                    mPhoneState = state;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (NotestateE.PAUSED == state() && isPausedByIncomingCall) {
                        start();
                        isPausedByIncomingCall = false;
                    }
                    mPhoneState = state;
                    QNDev.logd(TAG, "mPhoneListener received call state change:CALL_STATE_IDLE");
                    break;
                default:
                    break;
                
            }        
        }    
    };


/*
    private PhoneStateListener mSecondaryPhoneListener = new PhoneStateListener(){

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state){
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    QNDev.logd(TAG, "mSeconadryPhoneListener received call state change:CALL_STATE_OFFHOOK");
                    mSecondaryPhoneState = state;
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    QNDev.logd(TAG, "mSecondaryPhoneListener received call state change:CALL_STATE_RINGING");
                    if(mPhoneState == TelephonyManager.CALL_STATE_IDLE && mSecondaryPhoneState == SecondaryTelephonyManager.CALL_STATE_IDLE ){
                        QNDev.logd(TAG, "will pause voice note!");
                        if (NotestateE.STARTED == state()) {
                            isPausedByIncomingCall = true;
                            pause();
                        }
                    } else {
                        QNDev.logd(TAG, "mPhoneState or mSecondaryPhoneState is not idle!");
                    }                   
                    mSecondaryPhoneState = state;
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (NotestateE.PAUSED == state() && isPausedByIncomingCall) {
                        start();
                        isPausedByIncomingCall = false;
                    }

                    mSecondaryPhoneState = state;
                    QNDev.logd(TAG, "mSecondaryPhoneListener received call state change:CALL_STATE_IDLE");
                    break;
                default:
                    break;
           }
        }
    };
 */
}
