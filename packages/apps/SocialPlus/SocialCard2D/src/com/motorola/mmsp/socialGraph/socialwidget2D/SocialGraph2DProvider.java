package com.motorola.mmsp.socialGraph.socialwidget2D;

import java.util.ArrayList;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.os.Handler;
import android.os.SystemClock;

import com.motorola.mmsp.socialGraph.Constant;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.SocialDataService;

public class SocialGraph2DProvider extends GraphWidgetProvider{
    
	private static final String TAG = "SocialGraph2DProvider";
	private static final String VERSION = "2012-06-27";
	private static final boolean D = true;
	private Handler mHandler = new Handler();

	 public void onDisabled(Context context) {		
	     super.onDisabled(context); 
	     Log.d(TAG, "onDisabled");
	     try {
	    	 Log.d("sunli", "SocialGraph2DProvider:onDisabled--->stopService");
	         Intent intent = new Intent(context, SocialDataService.class);
		     context.stopService(intent);
		     System.gc();
	     } catch(Exception e) {
	    	 e.printStackTrace();
	     }
	 }
	 
    public void onEnabled(Context context) {    	
    	super.onEnabled(context); 
    	Log.d(TAG, "onEnabled");
    	 Log.d("sunli", "SocialGraph2DProvider:onEnabled--->startService");
    	context.startService(new Intent("com.motorola.mmsp.intent.action.SOCIAL_SERVICE_ACTION"));
    }

    //add for manual update
    @Override
    public void onReceive(Context context, Intent intent) {
    	Log.d(TAG, "SocialWidget onReceive intent = " + intent);
    	Log.d(TAG, "SocialWidget onReceive version = " + VERSION);
    	super.onReceive(context, intent);
    	
    	final Context ct = context;
    	final Intent it = intent;

        if (context == null || intent == null || intent.getAction() == null) {
            return;
        }
		if (!SocialWidget2DModel.getInstace().isFirstLoadFinished() && (!intent.getAction().contains("com.motorola.mmsp.action.click"))) {
			Log.d(TAG, "person data not prepared. start thread to send broadcast message ");
			new Thread(new Runnable() {
				public void run() {
					while (true) {
						SystemClock.sleep(2000);
						if (SocialWidget2DModel.getInstace()
								.isFirstLoadFinished()) {
							ct.sendBroadcast(it);
							break;
						}
					}

				}
			}).start();
			return;
		}
    	
		final SocialWidget2DModel model = SocialWidget2DModel.getInstace();
		if (!model.isEnable()) {
			Log.d(TAG, "model return");
			//return;
		}
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
    	String action = intent.getAction();

		if (action.equals(Constant.BROADCAST_WIDGET_CONTACT_CHNAGE)) {
			if(D)Log.d(TAG, " BROADCAST_WIDGET_CONTACT_CHNAGE received " );
			
			ArrayList<Integer> informationChangedPeople = intent.getIntegerArrayListExtra("informationChangedPeople");
			ArrayList<Integer> indexs = indexsFromPeople(model, informationChangedPeople);
			final RemoteViews main = init(context, appWidgetManager, getWidgetComponent(context));
			initSocialWidget(main, context);
			model.asyncReload(context, SocialWidget2DModel.LOAD_PERSONSINFO_SHORTCUT, indexs , mHandler, new 			Runnable(){
				public void run() {					
					updateWidget(main, ct, null, null);
				}});
		
		} else if (action.equals(Constant.BROADCAST_WIDGET_OUT_OF_BOX)) {
			if(D)Log.d(TAG, " BROADCAST_WIDGET_OUT_OF_BOX received ");
			final RemoteViews main = init(context, appWidgetManager, getWidgetComponent(context));
			updateOutOfBoxStatus(main, context);
			model.asyncReload(context,
					SocialWidget2DModel.LOAD_PERSONSINFO_ALL, null, mHandler,
					new Runnable() {
						public void run() {
							updateWidget(main, ct, null, null);
						}
					});
        
		} else if (action.equals(Constant.BROADCAST_WIDGET_SHORTCUT_CHNAGE)) {
			if(D)Log.d(TAG, " BROADCAST_WIDGET_SHORTCUT_CHNAGE received " );
			model.updateOutOfBoxStatus(context);
			boolean bHasSet = model.getOutOfBoxStatus();
			if (bHasSet) {
			ArrayList<ChangeHistory> history = SocialWidget2DUtils.getChangeHistory(context);
			SocialWidget2DUtils.deleteHistory(context);
			Log.d(TAG, "history size = " + (history != null ? history.size() : 0));
			if (history != null && history.size() > 0) {
				final RemoteViews main = init(context, appWidgetManager, getWidgetComponent(context));
				initSocialWidget(main, context);
				//ArrayList<Integer> indexs = indexsFromHistory(history);
				model.asyncReload(context,
						SocialWidget2DModel.LOAD_PERSONSINFO_SHORTCUT, null,
						mHandler, new Runnable() {
							public void run() {
								updateWidget(main, ct, null, null);
							}
						});
			}		
			}
		} else if (action.equals(Constant.BROADCAST_WIDGET_SKIN_CHNAGE)) {
			final RemoteViews main = init(context, appWidgetManager, getWidgetComponent(context));
			updateOutOfBoxStatus(main, context);
			model.asyncReload(context, SocialWidget2DModel.LOAD_PERSONSINFO_SKIN, null , mHandler, new Runnable(){
				public void run() {					
					updateWidget(main, ct, null, null);
				}});
			
		} else if (action.equals(Constant.BROADCAST_WIDGET_MODE_CHANGE)) {
			model.updateOutOfBoxStatus(context);
		    boolean bHasSet = model.getOutOfBoxStatus();
			if (bHasSet) {
			final RemoteViews main = init(context, appWidgetManager, getWidgetComponent(context));
			Runnable delayRunnable = new Runnable(){
				public void run() {	
					model.asyncReload(ct, SocialWidget2DModel.LOAD_PERSONSINFO_ALL, null , mHandler, new Runnable(){
						public void run() {					
							updateWidget(main, ct, null, null);
						}});
}				
			};
			model.postDelayed(delayRunnable);
			}
			
		} else if (action.equals(Constant.ACTION_WIDGET_ADDED)){
			final RemoteViews main = init(context, AppWidgetManager.getInstance(context), getWidgetComponent(context));
			boolean bHasSet = model.getOutOfBoxStatus();			
			showPlayButton(main, context, getWidgetComponent(context), !bHasSet);
			updateWidget(main, context, null, null);
			
		} else if (action.equals(Constant.BROADCAST_WIDGET_UPDATE)){
			model.updateOutOfBoxStatus(context);
			boolean bHasSet = model.getOutOfBoxStatus();
			if (bHasSet) {
			final RemoteViews main = init(context, AppWidgetManager.getInstance(context), getWidgetComponent(context));
			updateOutOfBoxStatus(main, context);
			Runnable delayRunnable = new Runnable(){
				public void run() {	
							updateWidget(main, ct, null, null);
				}				
			};
			model.postDelayed(delayRunnable);
			}
		}
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
		Log.d(TAG, "onUpdate called.  context = " + context.getPackageName());
		ensureModelCreate(context);
		if(!SocialWidget2DModel.getInstace().isFirstLoadFinished()){
			Log.d(TAG, "person data not prepared. ignore broadcast message " );
			return;
		}
		final RemoteViews main = init(context, appWidgetManager, getWidgetComponent(context));
		initSocialWidget(main, context);
		updateWidget(main, context, null, null);
    }

    public void onItemClick(Context context, int position){
    	Log.d(TAG, "onItemClick position = " + position);
    	ensureModelCreate(context);
        SocialWidget2DModel model = SocialWidget2DModel.getInstace();
		if (!model.getOutOfBoxStatus()) {
			return;
		}
		if (!SocialWidget2DModel.getInstace().isFirstLoadFinished()) {
			Log.d(TAG, "zc~~~person data not prepared.");
			return;
		}
        ArrayList<GraphWidgetProvider.Item> personInfos = model.getPersonInfos();
        if(position > personInfos.size()-1){
        	return;
        }
        GraphWidgetProvider.Item singlePersonInfo = personInfos.get(position);
        boolean autoMode = SocialWidget2DUtils.isAutoMode(context);
        
		if (singlePersonInfo != null && singlePersonInfo.id > 0) {
			Log.d(TAG, "shortcut " + position + " has person. Its name = " + singlePersonInfo.text);
			SocialWidget2DUtils.showQuickContact(context, singlePersonInfo.id);
		} else {
			Log.d(TAG, "shortcut " + position + " has no person");
			if (autoMode) {
				// do nothing
			} else {
				SocialWidget2DUtils.showManageContactsAcitivity(context);
			}
		}
    }
    
    @Override
    public void onPlayClick(Context context) {
        ensureModelCreate(context);
        SocialWidget2DModel model = SocialWidget2DModel.getInstace();
        model.updateOutOfBoxStatus(context);
        boolean bHasSet = model.getOutOfBoxStatus();
        if (bHasSet) {
           return;
        }
    	SocialWidget2DUtils.showWelcomeAcitivity(context);
    }
    
    @Override
    public void onSettingClick(Context context) {
    	// SWITCHUI-923 begin
        //showSettingButton(null, context, getWidgetComponent(context), false);
    	// SWITCHUI-923 end
    	ensureModelCreate(context);
        SocialWidget2DModel model = SocialWidget2DModel.getInstace();
		if (!model.getOutOfBoxStatus()) {
			return;
		}
    	SocialWidget2DUtils.showSettingApp(context,false);
    }

	private ArrayList<Integer> indexsFromHistory(ArrayList<ChangeHistory> history) {
		ArrayList<Integer> indexs = new ArrayList<Integer>();
		if (history != null) {
			for (ChangeHistory item : history) {
				indexs.add(item.index);
			}
		}
		return indexs;
	}
	
	private ArrayList<Integer> indexsFromPeople(SocialWidget2DModel model, ArrayList<Integer> informationChangedPeople) {
		return model.indexsFromPeople(informationChangedPeople);
	}
	
	
	private void initSocialWidget(RemoteViews main, Context context) {
		updateOutOfBoxStatus(main, context);
	}
	
	private boolean isOurWidget(Context context, int widgetId) {
		int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(getWidgetComponent(context));
		if (appWidgetIds != null && appWidgetIds.length > 0) {
			for (int item : appWidgetIds) {
				if (item == widgetId) {
					return true;
				}
			}
		}
		return false;
	}
	
	/*
	 * Update all if indexs is null.
	 * Use component name if appWidgetIds is null
	 */
	public void updateWidget(RemoteViews main, Context context, ArrayList<Integer> indexs, int[] appWidgetIds) {
		String s = makeDumpString(appWidgetIds);
		Log.d(TAG, "updateWidget appWidgetIds = " + s);
		
		SocialWidget2DModel model = SocialWidget2DModel.getInstace();
		ArrayList<GraphWidgetProvider.Item> personInfos = model
				.getPersonInfos();
		if(personInfos!=null){
			int i=0;
			for(GraphWidgetProvider.Item item:personInfos){
				i++;
				Log.d(TAG, "zc~updateAll shortcut item"+i+": id:"+item.id+",position:"+item.position+",text:"+item.text+",skin:"+item.skin+",textbar:"+item.textbar+",cover:"+item.cover);			
			}
		}

		if (indexs == null || indexs.size() > model.getShortcutNumber() / 3) {
			if (personInfos != null && personInfos.size() > 0) {
				if (appWidgetIds == null) {
					Log.d(TAG, "zc~updateAll shortcut by Component Name");
					updateAll(main, context, getWidgetComponent(context),
							 personInfos);
				} else {
					Log.d(TAG, "updateAll shortcut by WidgetIds = "  + s);
					for (int widgetid : appWidgetIds) {
						updateAll(main, context, AppWidgetManager.getInstance(context), 
								widgetid, personInfos);
					}
				}
				
			}
		} else if (indexs != null) {
			for (Integer index : indexs) {

				if (index != null) {
					if (appWidgetIds == null) {
						Log.d(TAG, "updateOne shortcut by Component Name");
						updateOne(main, context, getWidgetComponent(context), personInfos
								.get(index));
					} else {
						Log.d(TAG, "updateOne shortcut by widgetIds = " + s);
						for (int widgetid : appWidgetIds) {
							updateOne(main, context, widgetid, personInfos.get(index));
						}
					}
					
				}
			}
		}
	}

	private ComponentName getWidgetComponent(Context context) {
		return new ComponentName(context, SocialGraph2DProvider.class);
	}
	
	private void updateOutOfBoxStatus(RemoteViews main, Context context) {
		SocialWidget2DModel model = SocialWidget2DModel.getInstace();
		model.updateOutOfBoxStatus(context);
		boolean bHasSet = model.getOutOfBoxStatus();
		Log.d(TAG, "show play button " + !bHasSet);
		showPlayButton(main, context, getWidgetComponent(context), !bHasSet);
	}
	
	private void ensureModelCreate(Context context) {
		SocialWidget2DModel model = SocialWidget2DModel.getInstace();
		if (!model.isEnable()) {
			Log.e(TAG, "UnWanted happend!!!! Model is released. Create it Again!!!!");
			model.onCreate(context);
		}
	}
	
	private String makeDumpString(int[] array) {
		String s = "[";
		for (int i=0; array != null && i < array.length; i++) {
			s += array[i] + ",";
		}
		s += "]";
		return s;
	}
	
	private Context getApplicationContext(Context context) {
		Context applicatioContext = context;
		if (context == null) {
			return null;
		}

		if (context instanceof Application) {
			applicatioContext = context;
		} else {
			applicatioContext = context.getApplicationContext();
		}

		return applicatioContext;
	}
}
