/*
 * @(#)BaseTogglerWidget.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * Ankur Agarwal 2009/11/06  IKAPP-616		  Initial version 
 *
 */
package com.motorola.mmsp.togglewidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.content.res.Configuration;
import android.os.Bundle;
import com.android.internal.telephony.TelephonyIntents;


/**
 * Base class for the different Toggler widgets such as WiFi, GPS, Bluetooth,
 * AirplaneMode.
 */

abstract public class BaseTogglerWidget extends AppWidgetProvider {

	public static final String TAG = "BaseTogglerWidget";
	private boolean needDelay=false;
    protected static boolean service_state_intent_recved_this_time = true;

	/**
	 * State signifies the present condition of the Service
	 * For different state we have different layouts
	 */
	public enum State {
		DISABLED_STATE, 
		ENABLED_STATE, 
		INTERMEDIATE_STATE, 
		INVALID_STATE;

		public static State convert(int value) {
			return State.class.getEnumConstants()[value];
		}

		public static String toString(int value) {
			return convert(value).toString();
		}
	};

	/**
	 * Availability signifies the availability of the Service
	 * If the service is not available in the phone a Toast has to be
	 * shown and the state needs to be made as DISABLED_STATE
	 */
	public enum Availability {
		DISABLED_SERVICE, 
		ENABLED_SERVICE;

		public static Availability convert(int value) {
			return Availability.class.getEnumConstants()[value];
		}

		public static String toString(int value) {
			return convert(value).toString();
		}

	};


	protected static final int BUTTON_SERVICE = 0;
	
	protected String LOG_TAG = getClass().getSimpleName();

    abstract protected State getWidgetState(Context context);

	abstract protected State getServiceState(Context context);

	abstract protected void toggleService(Context context, State serviceState);

	abstract protected Availability getServiceAvailability(Context context);

	abstract protected int getSleeveImageName(State currentState);
	
	abstract protected boolean getSleeveState();

	abstract protected int getTogglerImageName();

	abstract protected int getStatusTextColor(State currentState);
	
	abstract protected int getToastText();


	/**
	 * onReceive is fired when: 
	 * 1.) the user clicks the Service check box in Settings 
	 * 2.) the user clicks the Widget (to toggle the mode)
	 * 
	 * @see android.appwidget.AppWidgetProvider#onReceive(android.content.Context,
	 *      android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		myLog(LOG_TAG, "OnReceive :" +intent);
		super.onReceive(context, intent);
		State currentState = getServiceState(context);
        if("android.appwidget.action.DELAY_UPDATE".equals(intent.getAction())){
            return;
        }

        if( this instanceof AirplaneModeToggle ){
            if( intent.hasCategory(Intent.CATEGORY_ALTERNATIVE) ) /* IKPRODUCT5-1 */
                service_state_intent_recved_this_time = false;
            else if(intent.getAction() != null &&
                    intent.getAction().equals(TelephonyIntents.ACTION_SERVICE_STATE_CHANGED)){
                service_state_intent_recved_this_time = true;
                currentState = getWidgetState(context);
            }
        }
		if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {

			myLog(LOG_TAG, "OnReceive.hasCategory");
			Uri data = intent.getData();
			int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
			if (buttonId == BUTTON_SERVICE) {

				Availability serviceAvailability = getServiceAvailability(context);
				if (serviceAvailability == Availability.ENABLED_SERVICE) {
					
						toggleService(context, currentState);
						currentState = getServiceState(context);
				
				} 
				else {
					Toast.makeText(context, getToastText(),
							Toast.LENGTH_SHORT).show();
					currentState = State.DISABLED_STATE;
				}
			}
		}

          if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
              Bundle extras = intent.getExtras();
            if (extras != null) {
              int[] appWidgetIds = extras.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                if (appWidgetIds != null && appWidgetIds.length > 0) {
		        updateWidget(context, currentState, appWidgetIds);
                } else {
                    myLog(LOG_TAG, "appWidgetIds is null");
                    updateWidget(context, currentState);
              }
            } else {
                myLog(LOG_TAG, "extras is null");
                updateWidget(context, currentState);
           }
         } else if ("com.motorola.mmsp.home.ACTION_WIDGET_ADDED".equals(intent.getAction())) {
             Bundle extras = intent.getExtras();
             if (extras != null) {
                 int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
                 myLog(LOG_TAG, "appWidgetId:"+appWidgetId);
                 if (appWidgetId != -1) {
                     updateWidget(context, currentState, appWidgetId);
                 } else {
                     updateWidget(context, currentState);
                 }
             } else {
                 myLog(LOG_TAG, "extras is null");
                 updateWidget(context, currentState);
             }
         } else {
              if (getClass().getSimpleName().equals("AirplaneModeToggle")&&(!"android.intent.action.AIRPLANE_MODE".equals(intent.getAction()))) {
                 myLog(LOG_TAG, "needDelay is true");
                 needDelay=true;
              }
              updateWidget(context, currentState);
              if (getClass().getSimpleName().equals("AirplaneModeToggle")&&(!"android.intent.action.AIRPLANE_MODE".equals(intent.getAction()))) {
                  myLog(LOG_TAG, "sleep 2s");

                  try {
                      Thread.sleep(800);
                      } catch (InterruptedException e) {
                      e.printStackTrace();
                      }
                      needDelay=false;
                      updateWidget(context, currentState);
              }
           }

	}

	
	/**
	 * onUpdate is fired when: 
	 * A instance of the widget is launched 
	 */
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		// Update each requested appWidgetId
		myLog(LOG_TAG, "OnUpdate,GlobalVersion:SWITCHUI2.0_00.00.3AI");
		//State currentState = getServiceState(context);
		//updateWidget(context,currentState);
		
	}


	/**
	 * Updates the widget when something changes, or when a button is pushed.
	 *
	 * @param context
	 * @param currentState
	 *            Indicates the currentState of the Service
	 */
	public void updateWidget(Context context, State currentState) {

		myLog(LOG_TAG, "UpdateWidget by component name, currentState:" + currentState);
		RemoteViews views = buildUpdate(context, -1, currentState);

		final AppWidgetManager mgr = AppWidgetManager.getInstance(context);

		ComponentName thisWidget = new ComponentName(context, getClass());

		mgr.updateAppWidget(thisWidget, views);
	}

        /**
         * Updates the widget when something changes, or when a button is pushed.
         *
         * @param context
         * @param currentState
         *            Indicates the currentState of the Service
         */
        public void updateWidget(Context context, State currentState,int[] appWidgetIds) {
            myLog(LOG_TAG, "UpdateWidget by IDs, currentState:" + currentState);
            RemoteViews views = buildUpdate(context, -1, currentState);

            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);

            mgr.updateAppWidget(appWidgetIds, views);
        }

        /**
         * Updates the widget when something changes, or when a button is pushed.
         *
         * @param context
         * @param currentState
         * @param appWidgetId
         *            Indicates the currentState of the Service
         */
        public void updateWidget(Context context, State currentState,int appWidgetId) {
            myLog(LOG_TAG, "UpdateWidget by ID, currentState:" + currentState);
            RemoteViews views = buildUpdate(context, -1, currentState);

            final AppWidgetManager mgr = AppWidgetManager.getInstance(context);

            mgr.updateAppWidget(appWidgetId, views);
        }
	/**
	 * Load image and text for given widget and build {@link RemoteViews} for
	 * it.
	 * 
	 * @param context
	 * @param appWidgetId
	 * @param currentState
	 *            Indicates the current state of the Service
	 * @return RemoteViews
	 */
	protected RemoteViews buildUpdate(Context context, int appWidgetId, State currentState) {
		
		int rescIdLayout;

		switch (currentState) {
			case ENABLED_STATE:
				rescIdLayout = R.layout.layout_servicetoggle_on;
				break;
			case DISABLED_STATE:
				rescIdLayout = R.layout.layout_servicetoggle_off;
				break;
			default:
				if(getSleeveState() == true)
					rescIdLayout = R.layout.layout_servicetoggle_on_intermediate;
				else
					rescIdLayout = R.layout.layout_servicetoggle_off_intermediate;
				break;
		}
		
		RemoteViews views = new RemoteViews(context.getPackageName(),
				rescIdLayout);

		try {
			views.setOnClickPendingIntent(R.id.widget, getLaunchPendingIntent(
					context, appWidgetId, BUTTON_SERVICE));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		updateButtons(views, context, currentState);
		return views;
	}

	/**
	 * Updates the buttons based on the service type and underlying states of
	 * Service.
	 * 
	 * @param views
	 *            The RemoteViews to update.
	 * @param context
	 * @param currentState
	 *            Indicates the currentState of the Service
	 */
	private void updateButtons(RemoteViews views, Context context, State currentState) {
		
		myLog(LOG_TAG, "updateButtons");
		
		views.setImageViewResource(R.id.imagebg, getSleeveImageName(currentState));
		views.setImageViewResource(R.id.togglerImage, getTogglerImageName());
		if(currentState != State.INTERMEDIATE_STATE)
		views.setTextColor(R.id.statusText, context.getResources().getColor(
				getStatusTextColor(currentState)));
	}

	/**
	 * Creates PendingIntent to notify the widget when the widget button is
	 * clicked.
	 * 
	 * @param context
	 * @param appWidgetId
	 * @param buttonId
	 * @return PendingIntent
	 */
	protected PendingIntent getLaunchPendingIntent(Context context,
							int appWidgetId, int buttonId) {

		Intent launchIntent = new Intent();

		myLog(LOG_TAG, "getLaunchPendingIntent needDelay:"+needDelay);
		if(needDelay){
			launchIntent.setAction("android.appwidget.action.DELAY_UPDATE");
		}else{
			launchIntent.setClass(context, getClass());
			launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
			launchIntent.setData(Uri.parse("custom:" + buttonId));
		}
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, launchIntent, 0);
		return pi;
	}

	
	/** logs a message
	 * 
	 * @param tag - tag for message
	 * @param message - message to log
	 */
	protected static void myLog(final String tag, final String message) {

		Log.i(tag, message);
	}

}
