package com.test.silentcapture;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.IBinder;
import android.view.KeyEvent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.view.WindowManager;
import android.os.PowerManager;
import android.os.AsyncTask;
import android.view.View;
import android.os.Handler;
import android.os.HandlerThread;

import com.test.silentcapture.Settings;
import com.test.silentcapture.mail.MailDeliver;

import android.util.Log;

/**
 * Deliver serivce is to send the image file captured to a kind of server, such as:
 * Mail, Http, Google+ etc, prefered by user.
 */
public class DeliverService extends Service {

    //private IDeliver mDeliver;
    
    @Override
    public IBinder onBind(Intent intent) {
        // not supported
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(Settings.TAG, "DeliverService on create");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Settings.TAG, "DeliverService on start command, intent:" + intent);
        if (intent != null) {
            String path = intent.getStringExtra(Settings.EXTRA_ATTACHMENT);
            Log.d(Settings.TAG, "DeliverSerivce onStartCommand attachment:" + path);
            new DeliverAsyncTask().execute(path); 
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(Settings.TAG, "DeliverService on destroy");
        super.onDestroy();
    }


    class DeliverAsyncTask extends AsyncTask <String, Void, Boolean> { 
        MailDeliver mail = new MailDeliver();

        public DeliverAsyncTask() { }

        @Override
        protected Boolean doInBackground(String... params) {
            Log.d(Settings.TAG, "Deliver Async Task doInBackground, params:" + params); 
            int ret = mail.deliver(params[0]);
            if (ret == 1) {
                return true;
            } else if (ret == 0) {
                Log.d(Settings.TAG, "Deliver Async Task doInBackground deliver failed, will retry...");
                sRetry.postDelayed(new RetryFailure(params[0]), 60*1000); // delay 1 minute
            }
            return false;
        }
    }
    
    /**
     * Deliver retry for those failure files.
     */
    private static final HandlerThread sRetryThread = new HandlerThread("deliver-retry");
    static {
        sRetryThread.start();
    }
    private static final Handler sRetry = new Handler(sRetryThread.getLooper());
    class RetryFailure implements Runnable {
        private String path;
        private int times; // record how many times failed
        private MailDeliver mail;

        public RetryFailure(String path) {
            this.path = path;
            this.times = 0;
            this.mail = new MailDeliver();
        }

        public void run() {
            Log.d(Settings.TAG, "RetryFailure try to deliver again, path:" + this.path + ", times:" + this.times); 
            int ret = mail.deliver(this.path);
            if (ret == 0) {
                this.times++;
                Log.d(Settings.TAG, "RetryFailure failed again, path:" + this.path + ", times:" + this.times); 
                if (this.times < 3) {
                    sRetry.postDelayed(this, this.times*60*1000); // delay 1 minute at least
                } else {
                    Log.d(Settings.TAG, "RetryFailure failed all 3 times, stop retry");
                }
            }  
        }
    }
}
