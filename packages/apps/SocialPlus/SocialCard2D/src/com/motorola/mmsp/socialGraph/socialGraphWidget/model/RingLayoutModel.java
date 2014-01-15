package com.motorola.mmsp.socialGraph.socialGraphWidget.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.motorola.mmsp.socialGraph.Constant;
import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.FrequencyColumns;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Contact;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;
import com.motorola.mmsp.socialGraph.socialGraphWidget.define.Intents;
import com.motorola.mmsp.socialGraph.socialGraphWidget.providers.DatebaseHelper;

public class RingLayoutModel{
	private final String TAG = "SocialGraphWidget";
	private final String VERSION = "2012-06-11";
	
	public HashMap<Integer, Contact> contacts = new HashMap<Integer, Contact>();
	public HashMap<Integer, Integer> sizes = new HashMap<Integer, Integer>();
	public ArrayList<ModelListener> listeners = new ArrayList<ModelListener>();
	private Setting setting;
	private Context mContext;
	private static RingLayoutModel defaultRingLayoutModel;
	private static Object o = new Object();
	public boolean bStrenchPhoto = false;
	
	public boolean mDataReady = true;
	
	public RingLayoutModel(Context context) {
		mContext = context;
		setting = Setting.getInstance(mContext);
		boolean mode = setting.isAutoMode();
		loadSizes(context);
		loadContacts(context, setting.getWidgetContacts(mode));

		this.bStrenchPhoto = setting.isStretchOn();
	}
	
	public static RingLayoutModel getDefaultRingLayoutModel(Context context) {		

			synchronized (o) {
				if (defaultRingLayoutModel == null) {
					defaultRingLayoutModel = makeDefaultRingLayoutModel(context);
				}
			}
	
		return defaultRingLayoutModel;
	}
	
	public static void setDefaultRingLayoutModel(Context context, RingLayoutModel model) {
		if (defaultRingLayoutModel == null) {
			defaultRingLayoutModel = new RingLayoutModel(context);
		}
		defaultRingLayoutModel.cloneFrom(model);
	}
	
	private static RingLayoutModel makeDefaultRingLayoutModel(Context context) {
		RingLayoutModel mode = new RingLayoutModel(context);
		mode.sync();
		return mode;
	}
	
	public void reLoadContacts(Context context) {
		Set<Integer> posSet = contacts.keySet();

		for (Integer entry : posSet) {
			int person = contacts.get(entry).getPerson();
			Contact contact = new Contact(context, person);
			setContact(entry, contact, false);
		}

		notifyAllContactsChange();
	}
	
	public void refreshContactsInfo(Context context,
			ArrayList<Integer> informationChangedPeople) {

		if (informationChangedPeople == null
				|| informationChangedPeople.size() <= 0) {
			return;
		}

		Set<Integer> posSet = contacts.keySet();
		for (Integer entry : posSet) {
			Integer person = contacts.get(entry).getPerson();
			for (int i = 0; i < informationChangedPeople.size(); i++) {
				if (person .equals( informationChangedPeople.get(i))) {
					Contact contact = new Contact(context, person);
					contacts.put(entry, contact);
					break;
				}
			}

		}
	}
	
	public void refreshContactsInfo(Context context, String[] numbers) {
		ArrayList<Integer> contactIds = new ArrayList<Integer>();
		contactIds = setting.getWidgetContacts(false);
		boolean changed = false;
		Set<Integer> posSet = contacts.keySet();
		
		if ((numbers != null) && (numbers.length > 0)) {
			Integer newid = Contact.getContactIdbyNumber(context, numbers[0]);
			Contact newGuy = new Contact(context, newid);
			String displayName = newGuy.getName();
			for (Integer entry : posSet) {
				Integer person = contacts.get(entry).getPerson();
				boolean isJoin = false;
				if (displayName != null) {
					isJoin = displayName.equals(contacts.get(entry).getName())
							&& !Contact.isPersonExistInDb(context, person);
				}
				if (isJoin) {
					contacts.put(entry, newGuy);
					contactIds.set(entry, newid);
				} else {
					boolean exist = Contact.isPersonExistInDb(context, person);
					if (!exist) {
						Contact contact = new Contact(context);
						contacts.put(entry, contact);
						contactIds.set(entry, 0);
						changed = true;
					}
				}
			}

		} else {
			for (Integer entry : posSet) {
				Integer person = contacts.get(entry).getPerson();
				boolean exist = Contact.isPersonExistInDb(context, person);
				if (!exist) {
					Contact contact = new Contact(context);
					contacts.put(entry, contact);
					contactIds.set(entry, 0);
					changed = true;
				}
			}
		}

		if (changed) {
			// IKDOMINO-5654 begin
			if (contactIds != null) {
				for (int i = 0; i < contactIds.size(); i++) {
					Integer person = contactIds.get(i);
					if (person == null || person == 0) {
						continue;
					}
					for (int j = i + 1; j < contactIds.size(); j++) {
						if (person .equals( contactIds.get(j))) {
							contactIds.set(j, 0);
						}
					}
				}
			}
			// IKDOMINO-5654 end
			setting.setWidgetContacts(contactIds, false);
		}
	}
	
	public void refreshContactsInfo(Context context) {
		if ((setting == null) || (contacts == null)) {
			return;
		}
		
		ArrayList<Integer> contactIds = new ArrayList<Integer>();
		contactIds = setting.getWidgetContacts(false);
		boolean changed = false;
		Set<Integer> posSet = contacts.keySet();
		
		ArrayList<Integer> validIds = Contact.getContactIdsExistInDb(context,
				contactIds);		

		for (Integer entry : posSet) {
			int person = 0;
			if (contacts.get(entry) != null) {
				person = contacts.get(entry).getPerson();
			}
			if (person == 0) {
				continue;
			}

			boolean exist = false;

			if (validIds != null && validIds.contains(person)) {
				exist = true;
			}
			
			if (!exist) {
				Contact contact = new Contact(context);
				contacts.put(entry, contact);
				contactIds.set(entry, 0);
				changed = true;
			}
		}

		if (changed) {
			// IKDOMINO-5654 begin
			if (contactIds != null) {
				for (int i = 0; i < contactIds.size(); i++) {
					Integer person = contactIds.get(i);
					if (person == null || person == 0) {
						continue;
					}
					for (int j = i + 1; j < contactIds.size(); j++) {
						if (person.equals(contactIds.get(j))) {
							contactIds.set(j, 0);
						}
					}
				}
			}
			// IKDOMINO-5654 end
			setting.setWidgetContacts(contactIds, false);
		}
            
	}

	public void sync() {
		Log.d(TAG, "ring layout model sync start");
		Log.d(TAG, "SocialGraphWidget version:" + VERSION);
		ArrayList<Integer> contactIds = new ArrayList<Integer>();
		if (setting.isAutoMode()) {
			Cursor c = null;
			try {
				c = mContext.getContentResolver().query(
						Constant.FREQUENCY_CONTENT_URI,
						new String[] { FrequencyColumns.PERSON }, null, null,
						String.format("%s ASC", FrequencyColumns.RANKING));
				 ArrayList<Integer> contacts = new ArrayList<Integer>();
				 if (c != null && c.getCount() > 0) {
					 Log.d(TAG, "ring layout model sync frequency is not null");
					 int count = c.getCount();
					 c.moveToFirst();
					 for (int i = 0; i < count; i++) {
						 int person = c.getInt(0);
						 contacts.add(i, person);
						 c.moveToNext();
					 }
				 }else{
					 Log.d(TAG, "ring layout model sync frequency is null");
				 }
				for (int i = 0; i < contacts.size(); i++) {
					Log.d(TAG, "ring layout model sync contact i = " + contacts.get(i));
				}
				
				setContactsAndResize(contacts);
				
				//debug log info
				Log.d(TAG, "RingLayoutModel read contact id");
				//log
				
				contactIds = setting.getWidgetContacts(true);
				
				//debug log info
				Log.d(TAG, "RingLayoutModel read contact id ok");
				if (contactIds.size() == 0) {
					Log.d(TAG, "ring layout model sync contactIds size = 0");
				} else {
					for (int i = 0; i < contactIds.size(); i++) {
						Log.d(TAG, "ring layout model sync contactIds i = "
								+ contactIds.get(i));
					}
				}//log
			} catch (Exception e) {
				e.printStackTrace();	
			} finally {
				if (c != null) {
					c.close();
				}
			}					
		//Manual mode	
		} else {
			contactIds = setting.getWidgetContacts(false);

			setContactsInManualMode(contactIds);
		}
		
		loadSizes(mContext);
		loadContacts(mContext, contactIds);		
	}
	
	public void reset() {		
		
		int[] sizeOri = mContext.getResources().getIntArray(R.array.shortcut_size_manual);
		ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
		for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
			Shortcut shortcut = new Shortcut();
			shortcut.id = 0;
			shortcut.size = sizeOri[i];
			shortcut.pos = i;
			shortcuts.add(shortcut);
		}
		
		ArrayList<ChangeHistory> chagneHistory = new ArrayList<ChangeHistory>();
		for (int i = 0; i < 3; i++) {
			ChangeHistory changeHistoryItem = new ChangeHistory();
			changeHistoryItem.id = 0;
			changeHistoryItem.type = ChangeHistory.CHANGE_NONE;
			changeHistoryItem.data1 = 0;
			changeHistoryItem.data2 = 0;
			chagneHistory.add(changeHistoryItem);
		}

		storageShortcutsToDb(shortcuts, chagneHistory);

		loadSizes(mContext);
		loadContacts(mContext, null);
	}
	
	public void StretchPhotoChange() {

		ArrayList<ChangeHistory> chagneHistory = new ArrayList<ChangeHistory>();
		for (int i = 0; i < 3; i++) {
			ChangeHistory changeHistoryItem = new ChangeHistory();
			changeHistoryItem.id = 0;
			changeHistoryItem.type = ChangeHistory.CHANGE_NONE;
			changeHistoryItem.data1 = 0;
			changeHistoryItem.data2 = 0;
			chagneHistory.add(changeHistoryItem);
		}

		storageShortcutsToDb(null, chagneHistory);
	}	
	/**
	 * for contact information changed
	 */
	public void setHistoryForInformationChange(
			ArrayList<Integer> informationChangedPeople) {

		if (informationChangedPeople == null
				|| informationChangedPeople.size() <= 0) {
			return;
		}

		ArrayList<ChangeHistory> historys = new ArrayList<ChangeHistory>();
		ArrayList<Integer> contactIds = getContactFromDb();

		// tell the widget that how many changes
		// person id change
		if (contactIds != null) {
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {

				int person = (i < contactIds.size()) ? contactIds.get(i) : 0;

				for (int j = 0; j < informationChangedPeople.size(); j++) {
					if (person == informationChangedPeople.get(j)) {
						ChangeHistory changeHistoryItem = new ChangeHistory();
						changeHistoryItem.id = person;
						changeHistoryItem.type = ChangeHistory.CHANGE_POPULATED;
						changeHistoryItem.data1 = 0;
						changeHistoryItem.data2 = i;
						historys.add(changeHistoryItem);
						break;
					}
				}

			}
		} else {
			Log.d(TAG, "contactIds is null");
		}

		// debug log info
		if (contactIds != null) {
			for (int i = 0; i < contactIds.size(); i++) {
				Log.d(TAG, "contact id = " + contactIds.get(i));
			}
		}

		Log.d(TAG, "information change history count=" + historys.size());
		for (int i = 0; i < historys.size(); i++) {
			ChangeHistory changeHistoryItem = historys.get(i);
			Log.d(TAG, "change history " + i + ": id = " + changeHistoryItem.id
					+ " type= " + changeHistoryItem.type + " data1="
					+ changeHistoryItem.data1 + " data2="
					+ changeHistoryItem.data2);
		}// log

		// save change in database for the widget using.
		storageShortcutsToDb(null, historys);

	}

	private void loadContacts(Context context, ArrayList<Integer> contactIds) {
		int size = (contactIds != null) ? contactIds.size() : 0;
		for (int i = 0 ; i < size; i++) {
			Integer personInt = contactIds.get(i);
			int person = 0;
			if (personInt != null) {
				person = personInt;
			}
			
			Contact contact;
			if (person >= 0) {
				contact = new Contact(context, person);
			} else {
				contact = new Contact(context);
			}
			
			setContact(i, contact, false);
		}
		notifyAllContactsChange();
	}
	
	private void loadSizes(Context context) {
		boolean mode = setting.isAutoMode();
		if (mode) {
			ArrayList<Integer> sizes = setting.getWidgetSizes();
			int[] counts = new int[] { 0, Constant.SHORTCUT_BIG_COUNT,
					Constant.SHORTCUT_MEDIUM_COUNT,
					Constant.SHORTCUT_SMALL_COUNT };
			for (int i = 0; i < sizes.size(); i++) {
				counts[sizes.get(i)] -= 1;
			}
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
				int size;
				if (i >= sizes.size()) {
					size = 0;
				} else {
					size = sizes.get(i);
				}
				if (size == 0) {
					for (int j = 1 ; j < counts.length; j++) {
						if (counts[j] > 0) {
							size = j;
							counts[j]--;
							break;
						}
					}
				}
				this.sizes.put(i, size);
			}
		} else {
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
				int shortcutSize = getSize(i);
				this.sizes.put(i, shortcutSize);
			}
		}
		
	}
	
	/**
	 * Only for auto mode*/
	private void setContactsAndResize(ArrayList<Integer> contactIds) {
		ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
		ArrayList<Integer> oldContacts = setting.getWidgetContacts(true);
		ArrayList<Integer> oldSizes = setting.getWidgetSizes();
		ArrayList<Integer> sizes = setting.getWidgetSizes();
		for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
			Shortcut shortcut = new Shortcut();
			if (i < oldContacts.size()) {
				shortcut.id = oldContacts.get(i);
			} else {
				shortcut.id = 0;
			}
			if (i < sizes.size()) {
				shortcut.size = sizes.get(i);
			} else {
				shortcut.size = 0;
			}
			shortcut.pos = i;			
			shortcuts.add(shortcut);
			
		}
		Log.d(TAG, "start to calculator rank");
		int[] posAuto = mContext.getResources().getIntArray(R.array.shortcut_position_auto);
		
		ShortcutsRankingCalculator ranking = null;
		boolean is2DSocialGraph = mContext.getResources().getBoolean(R.bool.SocialGraph2D);
		if( !is2DSocialGraph ){
			Log.d(TAG, "SocialGraph3D ");
			 ranking = new ShortcutsRankingCalculator(
						shortcuts, 
						null/*setting.getHideContacts()*/,
						contactIds,
						posAuto);
		} else {
			Log.d(TAG, "SocialGraph2D ");
			ranking = new SecondShortcutsRankingCalculator(
					shortcuts, 
					null/*setting.getHideContacts()*/, 
					contactIds,
					posAuto);
		}
		
		shortcuts = ranking.getShortcuts();
		for (int i = 0; i < shortcuts.size(); i++) {
			Shortcut shortcut = shortcuts.get(i);
			Log.d(TAG, "shortcut id = " + shortcut.id + "shortcut size = "
					+ shortcut.size);
		}
		Log.d(TAG, "shortcut size = " + shortcuts.size());

		ArrayList<Integer> newIds = new ArrayList<Integer>();
		ArrayList<Integer> newSizes = new ArrayList<Integer>();
		for (int i=0; i < shortcuts.size(); i++) {
			Shortcut shortcut = shortcuts.get(i);
			newIds.add(shortcut.id);
			newSizes.add(shortcut.size);
		}
		
		if (oldSizes.equals(newSizes) && oldContacts.equals(newIds)) {
			Log.d(TAG, "RingLayoutModel no change");
		} else {
			Log.d(TAG, "RingLayoutModel changed");
			for (int i = 0; i < newIds.size(); i++) {
				Log.d(TAG, "newIds[" + i + "]=" + newIds.get(i));
			}
			Log.d(TAG, "newIds saved");
			setting.setWidgetContacts(newIds, true);
			setting.setWidgetSizes(newSizes);
		}

		//save change in database for the widget using.
		Log.d(TAG, "RingLayoutModel start to db storage");
		storageShortcutsToDb(shortcuts, ranking.getChangeHistroy());
	}
	
	/**
	 * Only for manual mode*/
	private void setContactsInManualMode(ArrayList<Integer> contactIds) {
		ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
		ArrayList<ChangeHistory> historys = new ArrayList<ChangeHistory>();
		ArrayList<Integer> oldContactIds = getContactFromDb();

		int[] sizesManual = mContext.getResources().getIntArray(
				R.array.shortcut_size_manual);

		ArrayList<Integer> validIds = Contact.getContactIdsExistInDb(mContext,
				contactIds);
		
		//debug log
		if (contactIds != null) {
			for (int i = 0; i < contactIds.size(); i++) {
				Log.d(TAG, "before verify valid:new contact id = "
						+ contactIds.get(i));
			}
		}else{
			return;
		}
		
		if (validIds != null) {
			for (int i = 0; i < validIds.size(); i++) {
				Log.d(TAG, "valid contact id = " + validIds.get(i));
			}
		}else{
			return;
		}
		//log

		int contactSize = contactIds.size();
		for (int i = 0; i < contactSize; i++) {
			int contactId = 0;
			Integer tmpId = contactIds.get(i);
			if (tmpId != null) {
				contactId = tmpId;
			}

			if (contactId != 0) {
				if (!validIds.contains(contactId)) {
					contactIds.set(i, 0);
				}
			}
		}
		
		//debug log
		if (contactIds != null) {
			for (int i = 0; i < contactIds.size(); i++) {
				Log.d(TAG, "after verify valid:new contact id = "
						+ contactIds.get(i));
			}
		}
		//log

		for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
			Shortcut shortcut = new Shortcut();

			if (i < contactIds.size()) {
				shortcut.id = contactIds.get(i);
			} else {
				shortcut.id = 0;
			}

			shortcut.size = sizesManual[i];
			shortcut.pos = i;
			shortcuts.add(shortcut);
		}

		// tell the widget that how many changes
		// person id change
		if (oldContactIds != null) {
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {

				int oldId = (i < oldContactIds.size()) ? oldContactIds.get(i)
						: 0;
				int newId = (i < contactIds.size()) ? contactIds.get(i) : 0;
				if (newId != oldId) {
					ChangeHistory changeHistoryItem = new ChangeHistory();
					changeHistoryItem.id = newId;
					changeHistoryItem.type = ChangeHistory.CHANGE_POPULATED;
					changeHistoryItem.data1 = 0;
					changeHistoryItem.data2 = i;
					historys.add(changeHistoryItem);
				}

			}
		} else {
			Log.d(TAG, "oldContactIds is null");
		}

		// debug log info
		if (oldContactIds != null) {
			for (int i = 0; i < oldContactIds.size(); i++) {
				Log.d(TAG, "old contact id = " + oldContactIds.get(i));
			}
		}

		if (contactIds != null) {
			for (int i = 0; i < contactIds.size(); i++) {
				Log.d(TAG, "new contact id = " + contactIds.get(i));
			}
		}

		Log.d(TAG, "manual change history count=" + historys.size());
		for (int i = 0; i < historys.size(); i++) {
			ChangeHistory changeHistoryItem = historys.get(i);
			Log.d(TAG, "change history " + i + ": id = " + changeHistoryItem.id
					+ " type= " + changeHistoryItem.type + " data1="
					+ changeHistoryItem.data1 + " data2="
					+ changeHistoryItem.data2);
		}// log

		// save change in database for the widget using.
		storageShortcutsToDb(shortcuts, historys);

	}
	
	private ArrayList<Integer> getContactFromDb() {
		ArrayList<Integer> contactIds = new ArrayList<Integer>();
		ArrayList<Shortcut> shortcuts = getShortcutsFromDb();
		
		for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
			contactIds.add(0);
		}
		
		for (int i = 0; shortcuts != null && i < shortcuts.size(); i++) {
			Shortcut shortcut = shortcuts.get(i);
			if (shortcut != null) {
				if (shortcut.pos < contactIds.size()) {
					contactIds.set(shortcut.pos, shortcut.id);
				}
			}
		}		
		
		return contactIds;

	}
	
	private ArrayList<Shortcut> getShortcutsFromDb() {
		
		ArrayList<Shortcut> shortcuts = new ArrayList<Shortcut>();
		
		Cursor c = null;

		try {
			c = mContext.getContentResolver().query(
					Constant.SHORTCUTS_CONTENT_URI,
					new String[] { DatebaseHelper.SHORTCUT_INDEX,
							DatebaseHelper.SHORTCUT_CONTACT_ID,
							DatebaseHelper.SHORTCUT_SIZE }, null, null,
					DatebaseHelper.SHORTCUT_INDEX + " ASC");
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				for (int i = 0; i < c.getCount(); i++) {
					Shortcut shortcut = new Shortcut();
					shortcut.pos = c.getInt(0);
					shortcut.id = c.getInt(1);
					shortcut.size = c.getInt(2);
					shortcuts.add(shortcut);
					c.moveToNext();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return shortcuts;
	}

	private void storageShortcutsToDb(ArrayList<Shortcut> shortcuts, ArrayList<ChangeHistory> historys) {
		
		for (int i=0; historys != null && i < historys.size(); i++) {
			ChangeHistory history = historys.get(i);
			if (history != null) {
				try {
				ContentValues values = new ContentValues();
				values.put(DatebaseHelper.HISTORY_INDEX, history.id);
				values.put(DatebaseHelper.HISTORY_TYPE, history.type);
				values.put(DatebaseHelper.HISTORY_DATA1, history.data1);
				values.put(DatebaseHelper.HISTORY_DATA2, history.data2);
				mContext.getContentResolver().insert(Constant.CHANGE_HISTORY_CONTENT_URI, 
						values);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}		
		
		for (int i = 0; shortcuts != null && i < shortcuts.size(); i++) {
			Shortcut shortcut = shortcuts.get(i);
			if (shortcut != null) {
				ContentValues values = new ContentValues();
				values.put(DatebaseHelper.SHORTCUT_CONTACT_ID, shortcut.id);
				values.put(DatebaseHelper.SHORTCUT_INDEX, shortcut.pos);
				values.put(DatebaseHelper.SHORTCUT_SIZE, shortcut.size);
				Cursor c = null;
				try {
					c = mContext.getContentResolver().query(Constant.SHORTCUTS_CONTENT_URI,
									new String[] { DatebaseHelper.SHORTCUT_ID },
									DatebaseHelper.SHORTCUT_INDEX + "=?",
									new String[] { String.valueOf(shortcut.pos) },
									null);
					if (c == null || c.getCount() <= 0) {
						mContext.getContentResolver().insert(
								Constant.SHORTCUTS_CONTENT_URI, values);
					} else if (c != null && c.getCount() > 0) {
						mContext.getContentResolver().update(
								Constant.SHORTCUTS_CONTENT_URI, values,
								DatebaseHelper.SHORTCUT_INDEX + "=?",
								new String[] { String.valueOf(shortcut.pos) });
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (c != null) {
						c.close();
					}
				}
			}
		}
		
		Log.d(TAG, "RingLayoutModel shortcut db storage ok");
		
		if ((historys != null) && (historys.size() > 0)) {
			Log.d(TAG, "RingLayoutModel have some change");			
		} else {
			Log.d(TAG, "RingLayoutModel have no change");
		}
		
		Intent intent = new Intent(Intents.BROADCAST_WIDGET_SHORTCUT_CHNAGE);
		mContext.sendBroadcast(intent);
	}
	
	private int getSize(int order) {
		return ShortcutsRankingCalculator.calculateSize(order);
	}
	
	public void setContact(int pos, Contact contact, boolean bNotify) {
		contacts.put(pos, contact);
		if (bNotify) {
			notifyContactChange(pos);
		}
	}
	
	public Contact getContact(int pos) {
		return contacts.get(pos);
	}
	
	private void notifyAllContactsChange() {
		Log.d(TAG, "notifyAllContactsChange begin");
		for (ModelListener l : listeners) {
			if (l != null) {
				l.onAllContactChange(contacts,sizes,setting.isAutoMode());
			}
		}
		Log.d(TAG, "notifyAllContactsChange end");
	}
	
	private void notifyContactChange(int pos) {
		if (listeners != null) {
			for (ModelListener l : listeners) {
				if (l != null) {
					Contact ncontact = contacts != null ? contacts.get(pos)
							: new Contact(mContext);

					int nsize = 0;
					if (sizes != null) {
						Integer tmpSize = sizes.get(pos);
						if (tmpSize != null) {
							nsize = tmpSize;
						}
					}
					boolean nmode = setting != null ? setting.isAutoMode()
							: false;
					l.onContactChange(pos, ncontact, nsize, nmode);
				}
			}
		}
	}
	
	public void setStretchPhoto(boolean bStretch) {
		if (bStretch == this.bStrenchPhoto) {
			return;
		}
		
		this.bStrenchPhoto = bStretch;
		for (ModelListener l : listeners) {
			if (l != null) {
				l.onImageStretchChange(bStretch);
			}
		}
	}
	
	public void notifyDataLoadFinish() {
		Log.d(TAG, "notify data load begin");
		for (ModelListener l : listeners) {
			if (l != null) {
				l.onDataLoadFinish();
			}
		}
		Log.d(TAG, "notify data load end");
	}
	
	public boolean getStretchPhoto() {
		return this.bStrenchPhoto;
	}
	
	public void addListener(ModelListener l) {
		if (!listeners.contains(l)) {
			listeners.add(l);
		}		
	}
	
	public void removeListener(ModelListener l) {
		listeners.remove(l);
	}
	
	public void cloneFrom(RingLayoutModel model) {
		if (this == model) {
			return;
		}
		
		this.contacts = (HashMap<Integer, Contact>)model.contacts.clone();
		this.sizes = (HashMap<Integer, Integer>)model.sizes.clone();
		if (this == defaultRingLayoutModel) {
			boolean bAutoMode = setting.isAutoMode();
			ArrayList<Integer> contactArray = new ArrayList<Integer>();
			for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
				if (contacts.get(i) != null) {
					contactArray.add(i, contacts.get(i).getPerson());
				} else {
					contactArray.add(i, 0);
				}
			}
			setting.setWidgetContacts(contactArray, bAutoMode);
			if (bAutoMode) {
				ArrayList<Integer> sizeArray = new ArrayList<Integer>();
				for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {
					if (sizes.get(i) != null) {
						sizeArray.add(i, sizes.get(i));
					} else {
						sizeArray.add(i, 0);
					}
				}
				setting.setWidgetSizes(sizeArray);
			} else {
				ArrayList<Integer> sizeArray = new ArrayList<Integer>();
				int[] sizeOri = mContext.getResources().getIntArray(
						R.array.shortcut_size_manual);
				for (int i = 0; i < Constant.SHORTCUT_COUNT; i++) {

					int shortcutSize = sizeOri[i];
					sizeArray.add(i, shortcutSize);
				}
				setting.setWidgetSizes(sizeArray);
			}
			
		}
		notifyAllContactsChange();
	}
	
	public interface ModelListener {
		void onContactChange(int pos, Contact contact, int size, boolean mode);
		void onAllContactChange(HashMap<Integer, Contact> contacts, HashMap<Integer, Integer> sizes, boolean mode);
		void onImageStretchChange(boolean bStretch);
		void onDataLoadFinish();
	}
	
}
