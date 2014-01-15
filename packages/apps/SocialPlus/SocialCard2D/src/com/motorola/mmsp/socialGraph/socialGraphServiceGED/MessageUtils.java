package com.motorola.mmsp.socialGraph.socialGraphServiceGED;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;

import com.motorola.mmsp.socialGraph.Constant;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.ContactInfoColumns;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.Frequency;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.FrequencyColumns;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.MiscValue;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.MiscValueColumns;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphProvider;
import com.motorola.mmsp.socialGraph.socialGraphWidget.Setting;

public class MessageUtils {
	private final static String TAG = "SocialGraphService";
	private final static boolean Debug = true;
	
	public static final int NOTIFY_TYPE_FREQUENCY = 0x2;
	public static final int NOTIFY_TYPE_CONTACT_INFO_CHANGE = 0x4;
	public static final int NOTIFY_TYPE_CONTACT_CHANGE = 0x8;

	/**
	 * Get the contact id by phoneNunber
	 * 
	 * @param phoneNunber
	 * 
	 * @return the contact id or zero if not found
	 * 
	 */
	public static int getContactIdbyNumber(Context context,String phoneNumber) {

		int personid = 0;
		 
		if (Debug)
			Log.e(TAG, "the phone number need person is " + phoneNumber);
		if(phoneNumber==null || phoneNumber.length() == 0){
			return personid;
		}
		
		String mySortedPhonenumber = phoneNumber.replace("-", "");
		if (Debug)
			Log.e(TAG, "the sorted phone number need person is "
					+ mySortedPhonenumber);
		
		Cursor cursorContact = null;
		try{
			cursorContact = context.getContentResolver().query(
				Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri
						.encode(mySortedPhonenumber)), null, null, null, null);
			if (Debug)
				Log.e(TAG, PhoneLookup.CONTENT_FILTER_URI.toString());

			if (cursorContact!= null && cursorContact.getCount() > 0) {
				cursorContact.moveToFirst();
				personid = cursorContact.getInt(cursorContact.getColumnIndex("_id"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if (cursorContact != null) {
				cursorContact.close();
			}
		}
		if (Debug)
			Log.e(TAG, "the latset person point is " + personid);
		
		return personid;
	}	
	
	/**
	 * Get the phoneNunber by contact id 
	 * 
	 * @param contact id
	 * 
	 * @return the phoneNunber or null if not found
	 * 
	 */
	public static String[] getNumbersByContactId(Context context,int contactId) {
		String[] numbers = null;
		
		Cursor c = null;
		try {
			c = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
					new String[] { ContactsContract.Data.DATA1 },
					Data.CONTACT_ID + "=?" + " AND "
	                  + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'",
					new String[] { String.valueOf(contactId) }, null);
			if (c != null && c.getCount() > 0) {
				int len = c.getCount();
				numbers = new String[len];
				c.moveToFirst();
				for (int i = 0; i < len; i++) {
					numbers[i] = c.getString(0);
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
		return numbers;
	}
	
	public static int readTotalDaysFromDb(Context context) {
		Cursor c = null;
		int days = ConstantDefinition.TOTAL_DAY;
		try {
			c = context.getContentResolver().query(MiscValue.CONTENT_URI,
					new String[] { MiscValueColumns.MISC_VALUE },
					MiscValueColumns.MISC_KEY + "=?",
					new String[] { ConstantDefinition.MISC_TOTAL_DAY }, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				days = c.getInt(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return days;
	}
	
	public static int handleContactChanged(Context context, Intent intent,
			ArrayList<Integer> informationChangedPeople) {
		
		int affectType = 0;

		ContentValues newContactvalues = new ContentValues();
		ContentValues oldContactvalues = new ContentValues();
		
		ArrayList<Integer> newFrequentlyList = new ArrayList<Integer>();
		
		SQLiteDatabase db = SocialGraphProvider.getDatabase();

		if (Setting.getInstance(context).isAutoMode()) {// Auto mode

			// query contact
			getFrequentlyContactedInfoFromPhoneBook4(context, newContactvalues,
					newFrequentlyList);

			queryContactsInfo(db, oldContactvalues, "0");

			// compare frequency
			affectType |= compareFrequentlyContacted(context, newFrequentlyList);

			// compare info
			affectType |= compareContactsInfoAuto(context,newContactvalues.valueSet(),
					oldContactvalues.valueSet(), informationChangedPeople);

			// save change
			if ((affectType & NOTIFY_TYPE_FREQUENCY) != 0) {
				Log.e(TAG,
						"auto mode : contact frequency has some change, update frequency to frequency table");
				updateFrequencyToDB(db, newFrequentlyList);
			}

			if (affectType > 0) {
				// debug info
				if ((affectType & NOTIFY_TYPE_FREQUENCY) != 0) {
					Log.e(TAG,
							"auto mode : frequency has some change, update to socialcontacts");
				} else {
					Log.e(TAG, "auto mode : frequency has no change");
				}
				if ((affectType & NOTIFY_TYPE_CONTACT_INFO_CHANGE) != 0) {
					Log.e(TAG,
							"auto mode : contact info has some change, update to socialcontacts");
				} else {
					Log.e(TAG, "auto mode : contact info has no change");
				}
				// debug

				updateContactsInfoToDB(db, newContactvalues.valueSet(), "0");
			}

		} else {// manual mode

			// query contact
			getContactInfoFromPhoneBook(context, newContactvalues);

			queryContactsInfo(db, oldContactvalues, "1");

			// compare info
			affectType |= compareContactsInfoManual(context,newContactvalues.valueSet(),
					oldContactvalues.valueSet(), informationChangedPeople);

			// debug info
			if ((affectType & NOTIFY_TYPE_CONTACT_INFO_CHANGE) != 0) {
				Log.e(TAG, "manual mode : contact info has some change");
			}// debug

			// save to db
			updateContactsInfoToDB(db, newContactvalues.valueSet(), "1");

		}

		return affectType;
	}
	
	private static StringBuffer buildQueryStrBuf(ArrayList<Integer> contactIds) {
		StringBuffer contactIdsStrBuf = new StringBuffer();

		for (int i = 0; contactIds != null && i < contactIds.size(); i++) {
			if (i != 0) {
				contactIdsStrBuf.append(",");
			}
			contactIdsStrBuf.append(contactIds.get(i));
		}
		
		return contactIdsStrBuf;
	}
	
	private static void boxContactInfos(Cursor myContactCursor,
			ContentValues Contactvalues, ArrayList<Integer> ContactList) {
		if (myContactCursor != null && myContactCursor.getCount() > 0) {
			Log.e(TAG, "ContactCursor from phone book count = " + myContactCursor.getCount());
			myContactCursor.moveToFirst();

			do {
				int PersonId = myContactCursor.getInt(myContactCursor
						.getColumnIndex(Contacts._ID));
				String Name = myContactCursor.getString(myContactCursor
						.getColumnIndex(Contacts.DISPLAY_NAME));
				String PhotoUrl = myContactCursor.getString(myContactCursor
						.getColumnIndex(Contacts.PHOTO_URI));
				//Contacts.
				//int dataVersion = myContactCursor.getInt(myContactCursor
				//		.getColumnIndex(Contacts.Data.DATA_VERSION));
/*				Log.e(TAG, "Contact from phone book: PersonId = " + PersonId + " , Name_Photourl="
						+ Name + "_" + PhotoUrl+",dataVersion = "+dataVersion);*/
				if (Contactvalues != null) {
				Contactvalues.put(PersonId + "", Name + "_" + PhotoUrl);
				}
				if (ContactList != null) {
					ContactList.add(PersonId);
				}
			} while (myContactCursor.moveToNext());

		}
	}
	
	private static void boxContactIds(Cursor myContactCursor,
			ArrayList<Integer> ContactList) {
		if (myContactCursor != null && myContactCursor.getCount() > 0) {
			Log.e(TAG, "ContactCursor from phone book count = " + myContactCursor.getCount());
			myContactCursor.moveToFirst();

			do {
				int PersonId = myContactCursor.getInt(0);
				
				if (ContactList != null) {
					ContactList.add(PersonId);
				}
			} while (myContactCursor.moveToNext());

		}
	}
	
	private static void queryContactsInfo(SQLiteDatabase db,
			ContentValues contactInfos, String type){
		Cursor myContactCursor = null;
		try {
			myContactCursor = db.query("socialcontacts", null, "type=?", new String[] { type },
					null, null, null);
			if (myContactCursor != null && myContactCursor.getCount() > 0) {
				myContactCursor.moveToFirst();
				do {
					int PersonId = myContactCursor.getInt(myContactCursor
							.getColumnIndex(ContactInfoColumns.PERSON));
					String Name_Photourl = myContactCursor
							.getString(myContactCursor
									.getColumnIndex(ContactInfoColumns.INFO_URI));
					Log.e(TAG, "contact from socialcontacts PersonId = " + PersonId
							+ ", Name_Photourl=" + Name_Photourl);
					contactInfos.put("" + PersonId, Name_Photourl);
				} while (myContactCursor.moveToNext());

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myContactCursor != null) {
				myContactCursor.close();
			}
		}
	}
	
	private static int compareContactsInfoManual(Context context,
			Set<Map.Entry<String, Object>> Contactset,
			Set<Map.Entry<String, Object>> Tempset,
			ArrayList<Integer> informationChangedPeople) {

		int affectType = 0;

		if (Contactset.size() == Tempset.size()) {
			for (Map.Entry<String, Object> Contactentry : Contactset) {
				String k1 = Contactentry.getKey();
				String v1 = (String) Contactentry.getValue();
				boolean exit = false;
				for (Map.Entry<String, Object> Tempentry : Tempset) {
					String k2 = Tempentry.getKey();
					String v2 = (String) Tempentry.getValue();
					if (k1.equals(k2)) {
						exit = true;
						if (!v1.equals(v2)) {
							informationChangedPeople.add(Integer.valueOf(k1));
							affectType |= NOTIFY_TYPE_CONTACT_INFO_CHANGE;
						}else if (v1!= null&&v1.contains("contacts/#/photo".replace("#", k1))&&compareDataVersion(context,k1)){
							
							informationChangedPeople.add(Integer.valueOf(k1));
							affectType |= NOTIFY_TYPE_CONTACT_INFO_CHANGE;
						}
						break;
					}
				}
				if (!exit) {
					affectType |= NOTIFY_TYPE_CONTACT_CHANGE;
				}
			}
		} else if (Contactset.size() < Tempset.size()) {
			affectType |= NOTIFY_TYPE_CONTACT_CHANGE;
		} else if (Contactset.size() > Tempset.size()) {
			affectType |= NOTIFY_TYPE_CONTACT_CHANGE;
		}
		
		//debug info
		Log.d(TAG,
				"information changed peopel num="
						+ informationChangedPeople.size());
		for (int i = 0; i < informationChangedPeople.size(); i++) {
			Log.d(TAG, "changed people: i= " + i + " id = "
					+ informationChangedPeople.get(i));
		}
		//debug

		return affectType;
	}
	
	private static int compareContactsInfoAuto(Context context,
			Set<Map.Entry<String, Object>> newContactset,
			Set<Map.Entry<String, Object>> oldContactset,
			ArrayList<Integer> informationChangedPeople) {

		int affectType = 0;

		for (Map.Entry<String, Object> Contactentry : newContactset) {
			String k1 = Contactentry.getKey();
			String v1 = (String) Contactentry.getValue();
			for (Map.Entry<String, Object> Tempentry : oldContactset) {
				String k2 = Tempentry.getKey();
				String v2 = (String) Tempentry.getValue();
				if (k1.equals(k2)) {
					if (!v1.equals(v2)) {
						informationChangedPeople.add(Integer.valueOf(k1));
						affectType |= NOTIFY_TYPE_CONTACT_INFO_CHANGE;
					}else if (v1!= null&&v1.contains("contacts/#/photo".replace("#", k1))&&compareDataVersion(context,k1)){					
						informationChangedPeople.add(Integer.valueOf(k1));
						affectType |= NOTIFY_TYPE_CONTACT_INFO_CHANGE;
					}
					break;
				}
			}
		}

		// debug info
		Log.d(TAG,
				"auto information changed peopel num="
						+ informationChangedPeople.size());
		for (int i = 0; i < informationChangedPeople.size(); i++) {
			Log.d(TAG, "auto changed people: i= " + i + " id = "
					+ informationChangedPeople.get(i));
		}
		// debug

		return affectType;
	}
	private static boolean compareDataVersion(Context context,String PersonID) {
		boolean reVal = false;
		int newDataVersion =-1;
		int oldDataVersion =-1;
		Log.d(TAG, "compareDataVersion begin");
		final Uri myUri = Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.CONTENT_URI, Integer.valueOf(PersonID)), 
                Photo.CONTENT_DIRECTORY);
		
		//query contacts
		Cursor myContactCursor = null;
		try {
			myContactCursor = context.getContentResolver().query(myUri, new String[]{Contacts.Data.DATA_VERSION},null, null, null);
			if (myContactCursor != null && myContactCursor.getCount() > 0) {
				myContactCursor.moveToFirst();
				do {
					newDataVersion= myContactCursor.getInt(0);
					Log.e(TAG, "contact from contacts data DataVersion = " + newDataVersion);
				} while (myContactCursor.moveToNext());

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myContactCursor != null) {
				myContactCursor.close();
			}
		}
		
		SharedPreferences sp = context.getSharedPreferences("DataVersion", Context.MODE_WORLD_READABLE | Context.MODE_WORLD_WRITEABLE);
		oldDataVersion = sp.getInt(PersonID, -1);
		Log.d(TAG, "compareDataVersion oldDataVersion "+oldDataVersion);
		//query socialcontacts
		if(oldDataVersion != newDataVersion){
			Editor edit = sp.edit();
			edit.putInt(PersonID, newDataVersion);
			edit.commit();
			reVal=true;
		}
		Log.d(TAG, "compareDataVersion end");
		return reVal;
	}
	
	private static int compareFrequentlyContacted(Context context,
			ArrayList<Integer> newContactList) {
		
		int affectType = 0;
		ArrayList<Integer> oldContactList = new ArrayList<Integer>();
		
		getOldFrequentlyContacted(context, oldContactList);
		
		if (newContactList.size() == oldContactList.size()) {
			for (int index = 0; index < newContactList.size(); index++) {
				if (newContactList.get(index) != oldContactList.get(index)) {
					affectType |= NOTIFY_TYPE_FREQUENCY;
					break;
				}
			}
		} else if (newContactList.size() > oldContactList.size()) {
			affectType |= NOTIFY_TYPE_FREQUENCY;
		} else if (newContactList.size() < oldContactList.size()) {
			affectType |= NOTIFY_TYPE_FREQUENCY;
			affectType |= NOTIFY_TYPE_CONTACT_CHANGE;
		}

		return affectType;
	}
	
	public static void updateFrequencyToDB(SQLiteDatabase db,
			ArrayList<Integer> ContactList) {
			try{
		db.beginTransaction();
		db.execSQL(String.format("delete from %s", Frequency.TABLE_NAME));
		for (int index = 0; index < ContactList.size(); index++) {
			db.execSQL(String.format(
					"insert or replace into %s (%s, %s) VALUES (%s, %s)",
					Frequency.TABLE_NAME, FrequencyColumns.PERSON,
					FrequencyColumns.RANKING, ContactList.get(index), index));
		}

		db.setTransactionSuccessful();
		db.endTransaction();
						} catch (Exception e) {
					e.printStackTrace();
				} 
	}
	
	private static void getOldFrequentlyContacted(Context context,
			ArrayList<Integer> oldContacts) {
		Cursor c = null;
		try {
			c = context.getContentResolver().query(
					Constant.FREQUENCY_CONTENT_URI,
					new String[] { FrequencyColumns.PERSON }, null, null,
					String.format("%s ASC", FrequencyColumns.RANKING));

			if (c != null && c.getCount() > 0) {
				Log.d(TAG, "old frequency is not null");
				int count = c.getCount();
				c.moveToFirst();
				for (int i = 0; i < count; i++) {
					int person = c.getInt(0);
					oldContacts.add(i, person);
					c.moveToNext();
				}
			} else {
				Log.d(TAG, "old frequency is null");
			}

			for (int i = 0; i < oldContacts.size(); i++) {
				Log.d(TAG, "old contact i = " + i + " id = " + oldContacts.get(i));
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}
	
	public static void updateContactsInfoToDB(SQLiteDatabase db,
			Set<Map.Entry<String, Object>> Contactset, String type) {
       try{
		ContentValues values = new ContentValues();

		db.beginTransaction();

		db.execSQL(String.format("delete from %s where type = %s",
				"socialcontacts", type));
		
		for (Map.Entry<String, Object> entry : Contactset) {
			String k = entry.getKey();
			String v = (String) entry.getValue();
			Log.e(TAG, "update to socialcontacts set: k = " + k + ",v=" + v);
			values.put(ContactInfoColumns.PERSON, k);
			values.put(ContactInfoColumns.INFO_URI, v);
			values.put(ContactInfoColumns.TYPE, type);
			
			db.insert("socialcontacts", "foo", values);
		}

		db.setTransactionSuccessful();
		db.endTransaction();
		} catch (Exception e) {
					e.printStackTrace();
		}
	}
	
	public static void getContactInfoFromPhoneBook(Context context,
			ContentValues Contactvalues) {
		Cursor myContactCursor = null;
		try {
			ArrayList<Integer> manualContacts = Setting.getInstance(context)
					.getWidgetContacts(false);

			StringBuffer manualContactsString = buildQueryStrBuf(manualContacts);

			myContactCursor = context.getContentResolver().query(
					Contacts.CONTENT_URI,
					ConstantDefinition.COLUMNS,
					String.format("%s in (%s)", Contacts._ID,
							manualContactsString), null, null);

			boxContactInfos(myContactCursor, Contactvalues, null);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myContactCursor != null) {
				myContactCursor.close();
			}
		}
	}
	
	public static void getFrequentlyContactedInfoFromPhoneBook(Context context,
			ArrayList<Integer> ContactList) {
		Cursor myContactCursor = null;
		try {
			ArrayList<Integer> hiddedContacts = Setting.getInstance(context)
					.getHideContacts();

			StringBuffer hiddenContactsString = buildQueryStrBuf(hiddedContacts);

			myContactCursor = context.getContentResolver().query(
					Uri.withAppendedPath(Contacts.CONTENT_URI, "frequent"),
					new String[] { Contacts._ID },
					String.format("%s not in (%s)", Contacts._ID,
							hiddenContactsString), null, "last_time_used DESC");

			boxContactIds(myContactCursor, ContactList);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myContactCursor != null) {
				myContactCursor.close();
			}
		}
	}
	
	public static void getFrequentlyContactedInfoFromPhoneBook2(Context context,
			ContentValues Contactvalues, ArrayList<Integer> ContactList) {
		Cursor myContactCursor = null;
		try {
			ArrayList<Integer> hiddedContacts = Setting.getInstance(context)
					.getHideContacts();

			StringBuffer hiddenContactsString = buildQueryStrBuf(hiddedContacts);
			Log.e(TAG, "getFrequentlyContactedInfoFromPhoneBook2 hiddenContactsString= " + hiddenContactsString.toString());
			myContactCursor = context.getContentResolver().query(
					Contacts.CONTENT_URI.buildUpon()
							.appendQueryParameter("limit", String.valueOf(9))
							.build(),
					ConstantDefinition.COLUMNS,
					String.format("%s not in (%s)", Contacts._ID,
							hiddenContactsString)
							+ " and "
							+ Contacts.TIMES_CONTACTED + ">?",
					new String[] { "0" },
					"times_contacted DESC, last_time_contacted DESC");

			boxContactInfos(myContactCursor, Contactvalues, ContactList);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myContactCursor != null) {
				myContactCursor.close();
			}
		}
	}
	
	public static void getFrequentlyContactedInfoFromPhoneBook3(
			Context context, ContentValues Contactvalues,
			ArrayList<Integer> ContactList, ArrayList<Integer> FContactList) {
		Cursor myContactCursor = null;
		try {
			StringBuffer fContactsString = buildQueryStrBuf(FContactList);
			Log.e(TAG,
					"getFrequentlyContactedInfoFromPhoneBook3 FContactsString= "
							+ fContactsString.toString());
			
			myContactCursor = context.getContentResolver().query(
					Contacts.CONTENT_URI.buildUpon()
							.appendQueryParameter("limit", String.valueOf(9))
							.build(), ConstantDefinition.COLUMNS,
					String.format("%s in (%s)", Contacts._ID, fContactsString),
					null, "times_contacted DESC, last_time_contacted DESC");

			boxContactInfos(myContactCursor, Contactvalues, ContactList);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myContactCursor != null) {
				myContactCursor.close();
			}
		}
	}
	
	public static void getFrequentlyContactedInfoFromPhoneBook4(
			Context context, ContentValues Contactvalues,
			ArrayList<Integer> ContactList) {

		ArrayList<Integer> FContactList = new ArrayList<Integer>();

		getFrequentlyContactedInfoFromPhoneBook(context, FContactList);

		getFrequentlyContactedInfoFromPhoneBook3(context, Contactvalues,
				ContactList, FContactList);

	}

}
