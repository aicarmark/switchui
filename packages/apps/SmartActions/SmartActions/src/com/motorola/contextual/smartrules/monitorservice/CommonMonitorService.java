/*
 * @(#)CommonMonitorService.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a18491       2012/03/16  NA                  Initial version
 *
 */

package com.motorola.contextual.smartrules.monitorservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.motorola.contextual.smartrules.Constants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;


/**
 * This class is a common service which handles the registration and deregistration of
 * receivers and observers for publishers.
 * 
 * <code><pre>
 * CLASS:
 *     Extends Service
 *     Implements Constants
 *
 * RESPONSIBILITIES:
 * This is a STICKY service which takes care of re-registrations during process/service re-starts,
 * in addition to fresh registrations
 *
 * COLLABORATORS:
 *     ConditionPublsher - Implements the preconditions available across the system
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */
public class CommonMonitorService extends Service implements Constants {

    private static final String LOG_TAG =   CommonMonitorService.class.getSimpleName();
    private static final String CLIENT_LIST_PREFERENCE =   "ClientsList";
    private static final String VALUE_SEPARATOR = "--";
    private static final String RECEIVER = "Receiver";
    private static final String OBSERVER = "Observer";
    public static final String EXTRA_STATE_MON_CLASS ="STATE_MONITOR_CLASS";
    public static final String EXTRA_REGISTER ="REGISTER";
    private static final String PACKAGE = (Constants.class.getPackage()!= null)? Constants.class.getPackage().getName():null;
    private static HashMap<String, Object> mObservers = new HashMap<String, Object>();

    // Accepts tasks to run executing only one at a time queuing the rest.
    private static final ExecutorService mRequestExecutor = Executors.newSingleThreadExecutor();


    @Override
    public void onCreate() {
        if (LOG_INFO)
            Log.i(LOG_TAG, "In onCreate");
        mRequestExecutor.submit(new ProcessRequests(mHandler, null));
    }

    /**
     * Handler
     */
    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            // TBD : Need to check whether this handler is needed at all
        }
    };

    /**
    *
    * Runnable to process the requests
    *
    */
    private  final class ProcessRequests implements Runnable {
        private Handler callBackHandle = null;
        private Intent intent = null;

        /** constructor
         */
        public ProcessRequests(Handler callBack, Intent tempIntent) {
            callBackHandle = callBack;
            intent = tempIntent;

        }

        /**
         * Thread started to handle registrations and deregistrations
         */
        public void run() {

            if (LOG_INFO) Log.i(LOG_TAG, "service restart");
            if (intent == null) {

                // Re-register all clients from the persistent list
                List<String> valueList = retrieveValuesAsList(getApplicationContext(), CLIENT_LIST_PREFERENCE);

                for(String className : valueList) {
                    register(className);
                }

                if (LOG_INFO) Log.i(LOG_TAG, "service restart");

            } else {
                // Register / deregister based on request
                boolean register = intent.getBooleanExtra(EXTRA_REGISTER, false);
                String className = intent.getStringExtra(EXTRA_STATE_MON_CLASS);

                if(register) {
                    register(className);
                }
                else {
                    // deregister
                    deregister(className);
                }
            }

            synchronized(mObservers) {
                if (mObservers.size() == 0) {
                    // No more settings to observe, stop the service
                    if (LOG_INFO) Log.i(LOG_TAG, "Stopping service");
                    stopSelf();
                }
            }



            Message responseMsg = Message.obtain();
            responseMsg.setTarget(callBackHandle);
            responseMsg.sendToTarget();
        }


    }

    /** Instantiates the state monitor interface list from class name
     * @param classname
     * @return stateMon
     */
    private static final CommonStateMonitor instantiateFromClassName(String classname) {
        Class<?> cls = null;

        try {
            if(LOG_INFO) Log.i(LOG_TAG, "Class to be created " + classname);
            cls = Class.forName(classname);
        } catch(ClassNotFoundException e) {
            Log.e(LOG_TAG, "Class not found " + classname);
            return null;
        }
        try {
            return ((CommonStateMonitor)cls.newInstance());
        } catch(IllegalAccessException e) {
            Log.e(LOG_TAG, "class instantiation failed: default ctor not visible " + classname);

        } catch (InstantiationException e) {
            Log.e(LOG_TAG, "class instantiation failed: instance cannot be created " + classname);
        }
        return null;
    }
    /** This method registers receivers / observers using className
     * @param className
     */
    private final void register(String className) {
        ArrayList<String> monitorIds = null;
        CommonStateMonitor stateMon = null;

        stateMon = instantiateFromClassName(className);
        
        if(stateMon == null) return;
        
        monitorIds = stateMon.getStateMonitorIdentifiers();

        if(monitorIds == null) return;
        
        synchronized(mObservers) {
            if(!mObservers.containsKey(className)) {
                if (LOG_INFO) Log.i(LOG_TAG, "STATE_MONITOR_CLASS : " + className + " : " + monitorIds);

                for(String id : monitorIds)
                    handleRegisterRequest(stateMon, id, className);

                saveMonitoredClient(getApplicationContext(), className);
            }
        }

    }

    /** This method deregisters receivers / observers using className
     * @param className
     */
    private final void deregister(String className) {

        synchronized(mObservers) {

            CommonStateMonitor stateMon = (CommonStateMonitor)mObservers.get(className);
            
            if(stateMon == null) return;

            if(stateMon.getType() != null) {
            	if(RECEIVER.equals(stateMon.getType())) {
                    getApplicationContext().unregisterReceiver(stateMon.getReceiver());
                    if (LOG_INFO) Log.i(LOG_TAG, "unregistered receiver for className " + className);
                    stateMon.setReceiver(null);
                } else if(OBSERVER.equals(stateMon.getType())) {
                    getApplicationContext().getContentResolver().unregisterContentObserver(stateMon.getObserver(getApplicationContext()));
                    if (LOG_INFO) Log.i(LOG_TAG, "unregistered observer for className " + className);
                }
            }
            
            mObservers.remove(className);
        }

        removeMonitoredClient(getApplicationContext(), className);
    }
    /** This method saves monitored client classes to the persistence list
     * @param context
     * @param className
     */
    private static final void saveMonitoredClient(Context context, String className) {

        List<String> valueList = retrieveValuesAsList(context, CLIENT_LIST_PREFERENCE);

        if(!valueList.contains(className)) {
            valueList.add(className);
        }

        commitValuesAsList(context, CLIENT_LIST_PREFERENCE, valueList);
    }

    /** This method removes monitored client classes from the persistence list
     * @param context
     * @param className
     */
    private void removeMonitoredClient(Context context, String className) {

        List<String> valueList = retrieveValuesAsList(context, CLIENT_LIST_PREFERENCE);

        if(valueList.contains(className)) {
            valueList.remove(className);
        }
        commitValuesAsList(context, CLIENT_LIST_PREFERENCE, valueList);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (LOG_INFO)
            Log.i(LOG_TAG, "In onStartCommand with intent " + intent);

        mRequestExecutor.submit(new ProcessRequests(mHandler, intent));

        // We want to stay alive until we decide to kill ourselves
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (LOG_INFO) Log.i(LOG_TAG, "In onDestroy");

        synchronized(mObservers) {
            Iterator<Entry<String, Object>> iter = mObservers.entrySet().iterator();
            while (iter.hasNext()) {
                Entry<String, Object> entry = iter.next();
                CommonStateMonitor stateMon = (CommonStateMonitor)(entry.getValue());
                if(stateMon == null) return;
                if(RECEIVER.equals(stateMon.getType())) {
                    getApplicationContext().unregisterReceiver(stateMon.getReceiver());
                    stateMon.setReceiver(null);
                } else if(OBSERVER.equals(stateMon.getType())) {
                    getApplicationContext().getContentResolver().unregisterContentObserver(stateMon.getObserver(getApplicationContext()));
                }

                if (LOG_INFO) Log.i(LOG_TAG, "unregistered observer for " + entry.getKey());
                iter.remove();
            }
        }
    }

    /**
     * Handles the request to register a receiver or observer
     * @param stateMon
     * @param monitorId
     * @param clsName
     */
    private final void handleRegisterRequest(CommonStateMonitor stateMon, String monitorId, String clsName) {

        // Handle registration of receiver or observer
        if (RECEIVER.equals(stateMon.getType())) {
            if (LOG_INFO) Log.i(LOG_TAG, "handleRegisterRequest : " + stateMon.getReceiver());

            IntentFilter filter = new IntentFilter();
            filter.addAction(monitorId);
            getApplicationContext().registerReceiver(stateMon.getReceiver(), filter);
        } else if (OBSERVER.equals(stateMon.getType())) {
            getApplicationContext().getContentResolver().registerContentObserver(Uri.parse(monitorId), true, stateMon.getObserver(getApplicationContext()));
        }
        mObservers.put(clsName, stateMon);

    }

    /** Commits an array of string values to persistent storage
     * @param context
     * @param key
     * @param values
     */
    public static void commitValuesAsList(Context context, String key, List<String> values) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        SharedPreferences.Editor editor = preferences.edit();
        StringBuilder sb = new StringBuilder(); // check String Builder code
        if(!values.isEmpty()) {
            sb.append(values.get(0));
            for (int i = 1; i < values.size(); ++i) {
                sb.append(VALUE_SEPARATOR);
                sb.append(values.get(i));
            }
        }
        editor.putString(key, sb.toString());
        editor.commit();
    }


    /** Retrieves a list of String values from persistent storage
     * @param context
     * @param key
     * @return
     */
    public static List<String> retrieveValuesAsList(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PACKAGE, 0);
        String info = preferences.getString(key, null);
        List<String> list = new ArrayList<String>();
        if (info != null && !info.isEmpty()) {
            String[] elements = info.split(VALUE_SEPARATOR);
            list = new ArrayList<String>();
            if(elements.length != 0)  {
                for (String e: elements)
                    list.add(e);
            }
        } else {
            //return an empty list
        }
        return list;
    }

}


