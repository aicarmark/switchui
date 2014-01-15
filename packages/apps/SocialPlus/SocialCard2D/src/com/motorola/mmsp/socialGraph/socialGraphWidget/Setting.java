package com.motorola.mmsp.socialGraph.socialGraphWidget;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.util.Log;

import com.motorola.mmsp.socialGraph.Constant;
import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.ConstantDefinition;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.contactService.RawContactsHandler;
import com.motorola.mmsp.socialGraph.socialGraphWidget.define.Intents;
import com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils.Skin;
import com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils.SkinResources;

public class Setting {
	private static final String TAG = "SocialGraphWidget";
	
	public static final boolean DEFAULT_AUTOMATIC_SETUP_MODE = false;
	public static final String DEFAULT_HIDE_CONTACTS = null;
	public static final String DEFAULT_HISTORY_SETTINGS = "30";
	public static final boolean DEFAULT_STRETCH_CONTACTS_PICTURES = false;
	public static int DEFAULT_SET_SKIN_INDEX = 0;
	
	//setting items
	public static final String KEY_AUTOMATIC_SETUP_MODE = "automatic_setup_mode";
	public static final String KEY_HIDE_CONTACTS = "hide_contacts";
	public static final String KEY_HISTORY_SETTINGS = "history_settings";
	public static final String KEY_STRETCH_CONTACTS_PICTURES = "stretch_contacts_pictures";
	public static final String KEY_SET_SKIN = "set_skin";
	
	//internal items.
	public static final String KEY_WIDGET_AUTO_CONTACTS = "widget_auto_contacts";
	public static final String KEY_WIDGET_MANUAL_CONTACTS = "widget_manual_contacts";
	public static final String KEY_WIDGET_SIZES = "widget_sizes";
	public static final String KEY_RESET = "reset";
	public static final String KEY_DAY_PASS_TIME = "day_pass_time";
	
	private static final String PACKAGE = "com.motorola.mmsp.socialGraph_preferences";
	private static final String EverSetDefaultValueName = "cdavalues.txt";
	public static final String EverSetDefaultValuePath = "/data/data/com.motorola.mmsp.socialGraph/files/" + EverSetDefaultValueName;
	private static SharedPreferences sp;
	private Context mContext;	
	
	private static Setting instance = null;
	private static Object o = new Object();
	
	private Setting(Context context) {
		this.mContext = context;
		this.sp = context.getSharedPreferences(PACKAGE, Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
	}
	
	public static Setting getInstance(Context context) {

			synchronized (o) {
				if (instance == null) {
					instance = new Setting(context);
					SkinResources skinRes = SkinResources.getInstance(context);
					for (int i = 0; i < skinRes.getSkinCount(); i++) {
						if (skinRes.getSkin(i).equals(
								context.getString(R.string.blue))) {
							DEFAULT_SET_SKIN_INDEX = i;
							break;
						}
					}
				}			
			}
	
		
		return instance;
	}
	
	public void setAutoMode(boolean mode) {
		Editor edit = sp.edit();
		edit.putBoolean(KEY_AUTOMATIC_SETUP_MODE, mode);
		edit.commit();
	}
	
	public boolean isAutoMode() {
		return sp.getBoolean(KEY_AUTOMATIC_SETUP_MODE, DEFAULT_AUTOMATIC_SETUP_MODE);
	}
	
	public boolean isStretchOn() {
		return sp.getBoolean(KEY_STRETCH_CONTACTS_PICTURES, DEFAULT_STRETCH_CONTACTS_PICTURES);
	}
	
	public ArrayList<Integer> getHideContacts() {
		return getIntList(KEY_HIDE_CONTACTS);
	}
	
	public void setHideContacts(ArrayList<Integer> hidenContacts) {
		if (!getIntList(KEY_HIDE_CONTACTS).equals(hidenContacts)) {
			setIntList(KEY_HIDE_CONTACTS, hidenContacts);
		}		
	}
	
	public void addHideContacts(ArrayList<Integer> persons) {
		ArrayList<Integer> hiddenContacts = getHideContacts();
		for (int person : persons) {
			if (!hiddenContacts.contains(person)) {
				hiddenContacts.add(person);				
			}
		}
		setHideContacts(hiddenContacts);
	}
	
	public void setHistorySetting(int count) {
		Editor edit = sp.edit();
		edit.putString(KEY_HISTORY_SETTINGS, String.valueOf(count));
		edit.commit();
	}
	
	public int getHistorySetting() {
		return Integer.valueOf(sp.getString(KEY_HISTORY_SETTINGS, DEFAULT_HISTORY_SETTINGS));
	}
	
	public void setStrechPicture(boolean b) {
		Editor edit = sp.edit();
		edit.putBoolean(KEY_STRETCH_CONTACTS_PICTURES, b);
		edit.commit();
	}	
	
	public void setSkin(int skin) {
		if (getSkin() != skin) {
			Editor edit = sp.edit();
			edit.putInt(KEY_SET_SKIN, skin);
			edit.commit();
		}
	}
	
	public int getSkin() {
		return sp.getInt(KEY_SET_SKIN, DEFAULT_SET_SKIN_INDEX);
	}
	
	/**
	 * @param mode : true --> auto;  false --> manual*/
	public ArrayList<Integer> getWidgetContacts(boolean mode) {
		String key = mode ? KEY_WIDGET_AUTO_CONTACTS : KEY_WIDGET_MANUAL_CONTACTS;
		return getIntList(key);
	}
	
	public void setWidgetContacts(ArrayList<Integer> contactsOri, boolean mode) {
		// IKDOMINO-5654 begin
		ArrayList<Integer> contacts = null;
		if (contactsOri != null) {
			contacts = (ArrayList<Integer>) contactsOri.clone();
		}
		
		if (contacts != null) {
			for (int i = 0; i < contacts.size(); i++) {
				Integer person = contacts.get(i);
				if (person == null || person == 0) {
					continue;
				}
				for (int j = i + 1; j < contacts.size(); j++) {
					if (person.equals(contacts.get(j))) {
						contacts.set(j, 0);
					}
				}
			}
		}
		// IKDOMINO-5654 end
		String key = mode ? KEY_WIDGET_AUTO_CONTACTS : KEY_WIDGET_MANUAL_CONTACTS;
		if (!getIntList(key).equals(contacts)) {
			setIntList(key, contacts);
		}		
	}
	
	public void removeWidgetContacts(boolean mode, ArrayList<Integer> persons) {
		ArrayList<Integer> contacts = getWidgetContacts(mode);
		for (int person : persons) {
			if (contacts.contains(person)) {
				contacts.set(contacts.indexOf(person), 0);
			}
		}
		setWidgetContacts(contacts, mode);
		
	}
	
	public ArrayList<Integer> getWidgetSizes () {
		return getIntList(KEY_WIDGET_SIZES);
	}
	
	public void setDayPassTime (long time) {
		Editor editor = sp.edit();
		editor.putLong(KEY_DAY_PASS_TIME, time);
		editor.commit();
	}
	
	public long getDayPassTime() {
		return sp.getLong(KEY_DAY_PASS_TIME, 0);
	}
	
	public void reset() {
		// clear frequency table
		Log.d(TAG, "Setting reset");
		try{
		mContext.getContentResolver().delete(Constant.FREQUENCY_CONTENT_URI,
				null, null);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		setToDefault();

		// notify.
		Intent intent = new Intent(Intents.BROADCAST_SETTING_CHANGE);
		intent.putExtra("key", KEY_RESET);
		mContext.sendBroadcast(intent);
	}
	
	private void setToDefault() {
		// auto mode
		setAutoMode(true);
		// Hidden contacts
		ArrayList<Integer> hidenContacts = new ArrayList<Integer>();
		setHideContacts(hidenContacts);
		// history
		setHistorySetting(Integer.parseInt(DEFAULT_HISTORY_SETTINGS));
		// Stretch picture
		setStrechPicture(DEFAULT_STRETCH_CONTACTS_PICTURES);
		// reset widget contacts
		ArrayList<Integer> contacts = new ArrayList<Integer>();
		ArrayList<Integer> sizes = new ArrayList<Integer>();
		int[] sizeOri = mContext.getResources().getIntArray(
				R.array.shortcut_size_manual);
		for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
			contacts.add(0);
			int shortcutSize = sizeOri[i];
			sizes.add(i, shortcutSize);
		}
		setWidgetContacts(contacts, true);
		setWidgetContacts(contacts, false);
		setWidgetSizes(sizes);		

		setSkin(DEFAULT_SET_SKIN_INDEX);
		setSkinToDb(mContext);
	}

	public void loadFactorySetting() {
		if (false == new File(EverSetDefaultValuePath).exists()) {
			Log.v(TAG, "Fetch values from CDA begin");
			
			setToDefault();			
			
			try {
				OutputStream os = mContext.openFileOutput(EverSetDefaultValueName, Context.MODE_PRIVATE);
				os.write(1);
				os.close();
				Log.v(TAG, "Fetch values from CDA end");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void setWidgetSizes(ArrayList<Integer> sizes) {
		setIntList(KEY_WIDGET_SIZES, sizes);
	}
	
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		sp.registerOnSharedPreferenceChangeListener(listener);
	}
	
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
		sp.unregisterOnSharedPreferenceChangeListener(listener);
	}
	
	private void setStringList(String key,ArrayList<String> cities) {
		Editor edit = sp.edit();
		edit.putString(key,cities.toString());
		edit.commit();
	}
	
	private ArrayList<String> getStringList(String key, String def) {
		String s = sp.getString(key, def);
		ArrayList<String> list = new ArrayList<String>();
		if (s != null && !s.equals("") && !s.equals("[]")) {
			String ss = s.replace('[', '\0');
			ss = ss.replace(']', '\0');
			String items[] = ss.split(",");
			for (int i=0; i < items.length; i++) {
				list.add(items[i].trim());
			}
		}
		return list;
	}
	
	private ArrayList<Integer> getIntList(String key) {
		ArrayList<String> Strings = getStringList(key, null);
		ArrayList<Integer> Ints = new ArrayList<Integer>();
		int size = (Strings != null) ? Strings.size() : 0;
		for (int i = 0; i < size; i++) {
			Ints.add(i, Integer.parseInt(Strings.get(i)));
		}
		
		return Ints;
	}
	
	private void setIntList(String key, ArrayList<Integer> Ints) {
		ArrayList<String> Strings = new ArrayList<String>();
		int size = (Ints != null) ? Ints.size() : 0;
		for (int i = 0; i < size; i++) {
			Strings.add(i, String.valueOf(Ints.get(i)));
		}
		setStringList(key, Strings);
	}
	
	public static void setSkinToDb(Context context) {
		Cursor c = null;
		try {
			c = context.getContentResolver().query(Constant.SKIN_CONTENT_URI,
					null, null, null, null);
			int skin_index =  Setting.getInstance(context).getSkin();
			Skin skin = SkinResources.getInstance(context).getSkins().get(
					skin_index);
			ContentValues values = new ContentValues();
			for (String key : skin.properties.keySet()) {
				values.put(key, String.valueOf(skin.properties.get(key)));
			}
			values.remove(Skin.NAME);
			values.remove(Skin.THUMBNAIL);
			if (c != null && c.getCount() > 0) {
				context.getContentResolver().update(Constant.SKIN_CONTENT_URI,
						values, null, null);
			} else {
				context.getContentResolver().insert(Constant.SKIN_CONTENT_URI,
						values);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}

		Intent intent = new Intent(Intents.BROADCAST_WIDGET_SKIN_CHNAGE);
		context.sendBroadcast(intent);
		Log.d(TAG, "zc~~~~~~~~~~~Intents.BROADCAST_WIDGET_SKIN_CHNAGE" );
	}
	
	public static void notifyContactUpdate(Context context) {
		Log.e(TAG, "notify change to for oob");
		Intent intent = new Intent(
				RawContactsHandler.CONTACTS_NOTIFICATION_ACTION);
		intent.putExtra("action",
				ConstantDefinition.MESSAGE_ACTION_CONTACT_DATA_UPDATE);
		context.sendBroadcast(intent);
	}
    // added by amt_sunli 2012-12-14 SWITCHUITWO-292 begin
    public boolean getWidgetMode() {
        return sp.getBoolean(KEY_AUTOMATIC_SETUP_MODE, true);
    }
    // added by amt_sunli 2012-12-14 SWITCHUITWO-292 end
}
