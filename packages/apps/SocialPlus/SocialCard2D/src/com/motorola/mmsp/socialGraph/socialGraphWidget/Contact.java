package com.motorola.mmsp.socialGraph.socialGraphWidget;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Photo;
import android.util.Log;
import android.widget.Toast;

import com.motorola.mmsp.socialGraph.Constant;
import com.motorola.mmsp.socialGraph.R;
import com.motorola.mmsp.socialGraph.socialGraphWidget.define.ContactInfo;
import com.motorola.mmsp.socialGraph.socialGraphWidget.define.EmailAddress;
import com.motorola.mmsp.socialGraph.socialGraphWidget.define.PhoneNumber;

public class Contact {
	private static final String TAG = "SocialGraphWidget";
	
	private ContactInfo mContactInfo;
	private Context mContext;
	ContentResolver cr;
	
	public Contact(Context context) {
		this.mContext = context;
		this.cr = mContext.getContentResolver();
		mContactInfo = new ContactInfo();
	}
	
	public Contact(Context context, int personID) {
		this(context, ContentUris.withAppendedId(Contacts.CONTENT_URI, personID));
	}
	
	public Contact(Context context, Uri personUri) {
		this.mContext = context;
		this.cr = mContext.getContentResolver();
		if (personUri != null) {
			mContactInfo = loadContactInfo(cr, personUri);
		} else {
			ContactInfo contactInfo = new ContactInfo();
			contactInfo.photo = null;
			contactInfo.numbers = null;
			contactInfo.emails = null;
			mContactInfo = contactInfo;
		}

	}
	
	/**
	 * Is the person in the contacts db
	 * 
	 * @param contact
	 *            id
	 * 
	 * @return true if yes, false or not
	 * 
	 */
	public static boolean isPersonExistInDb(Context context, int contactId) {
		boolean exist = false;
		Cursor c = null;
		try {
			c = context.getContentResolver().query(
					ContactsContract.Contacts.CONTENT_URI,
					new String[] { ContactsContract.Contacts._ID },
					ContactsContract.Contacts._ID + "=?",
					new String[] { String.valueOf(contactId) }, null);
			if (c != null && c.getCount() > 0) {
				exist = true;
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return exist;
	}
	
	/**
	 * return person id array that person in the contacts db
	 * 
	 * @param Context
	 *            context
	 * 
	 * @return ArrayList<Integer> validIds
	 * 
	 */
	public static ArrayList<Integer> getContactIdsExistInDb(Context context,
			ArrayList<Integer> contactIds) {

		ArrayList<Integer> validIds = new ArrayList<Integer>();

		if (contactIds == null) {
			return validIds;
		}

		StringBuffer contactsString = new StringBuffer();
		for (int i = 0; i < contactIds.size(); i++) {
			if (contactIds.get(i) != 0) {				
				contactsString.append(contactIds.get(i));
				contactsString.append(",");
			}
		}
		int cStrLen = contactsString.length();
		if (cStrLen > 1) {
			contactsString.deleteCharAt(cStrLen - 1);
		}

		Cursor c = null;
		try {
			c = context.getContentResolver().query(
					ContactsContract.Contacts.CONTENT_URI,
					new String[] { ContactsContract.Contacts._ID },
					String.format("%s in (%s)", ContactsContract.Contacts._ID,
							contactsString), null, null);
			if (c != null && c.getCount() > 0) {
				while (c.moveToNext()) {
					validIds.add(c.getInt(0));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return validIds;
	}

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
		 
		Log.d(TAG, "the phone number need person is " + phoneNumber);
		if(phoneNumber==null || phoneNumber.length() == 0){
			return personid;
		}
		
		String mySortedPhonenumber = phoneNumber.replace("-", "");
		 Log.d(TAG, "the sorted phone number need person is " + mySortedPhonenumber);
		
		Cursor cursorContact = null;
		try{
			cursorContact = context.getContentResolver().query(
				Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri
						.encode(mySortedPhonenumber)), null, null, null, null);
			Log.d(TAG, PhoneLookup.CONTENT_FILTER_URI.toString());

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
		Log.d(TAG, "the latset person point is " + personid);
		
		return personid;
	}
	
	/**
	 * Get the contact id by name
	 * 
	 * @param name
	 * 
	 * @return the contact id or zero if not found
	 * 
	 */
	public static int getContactIdbyName(Context context, String name) {

		int personid = 0;

		Log.d(TAG, "the name of contact is " + name);

		if (name == null || name.length() == 0) {
			return personid;
		}

		String selection = null;

		selection = ContactsContract.Contacts.DISPLAY_NAME + " = ?";

		Cursor cursorContact = null;
		try {
			cursorContact = context.getContentResolver().query(
					ContactsContract.Contacts.CONTENT_URI,
					new String[] { ContactsContract.Contacts._ID, }, selection,
					new String[] { name, },
					ContactsContract.Contacts._ID + " DESC LIMIT 1");
			if (cursorContact != null && cursorContact.getCount() > 0) {
				cursorContact.moveToFirst();
				personid = cursorContact.getInt(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursorContact != null) {
				cursorContact.close();
			}
		}
		Log.d(TAG, "the latset contact id is " + personid);

		return personid;
	}
	
	public Bitmap getPhoto() {
		return mContactInfo.photo;
	}

	public String getName() {
		if (mContactInfo.name != null) {
			return mContactInfo.name;
		}
		if (mContactInfo.numbers != null) {
			for (PhoneNumber number : mContactInfo.numbers) {
				if (number.number != null) {
					return number.number;
				}
			}
		}
		
		return null;
	}
	
	public int getPerson() {
		return (int)mContactInfo.id;
	}
	
	public ArrayList<PhoneNumber> getPhoneNumber() {
        return mContactInfo.numbers;
	}
	
	public ArrayList<EmailAddress> getEmails() {
        return mContactInfo.emails;
	}
	
	public static HashMap<Integer,String> getContactNameMap(ContentResolver cr, long[] personID) {
		HashMap<Integer,String> map = new HashMap<Integer,String>();

		if(personID==null || personID.length ==0 ){
			return null;
		}

		String sql = " _id in (";
		for(int i  = 0 ; i < personID.length; i++ ){
			if(i != 0 ){
				sql += ",";
			}			
			sql += personID[i];		
		}
		sql += ") ";
		Cursor cursor = null;
		try {		
			cursor = cr.query( Contacts.CONTENT_URI, new String[] {Contacts._ID,
					Contacts.DISPLAY_NAME }, sql, null, null);
			if (cursor != null && cursor.getCount() > 0) {
				if(map == null ) map =  new HashMap<Integer,String>();
				while (cursor.moveToNext()) {					
					if(cursor.getString(1) == null){
						map.put(cursor.getInt(0), "");
					} else {
						map.put(cursor.getInt(0), cursor.getString(1));					
					}
					
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return map;
	}
	
	public static String loadContactName(ContentResolver cr, long personID) {
		HashMap<Integer,String> map = getContactNameMap(cr,new long[] {personID});		
		String name = null;
		if(map !=null ){
			name = map.get(personID);//warning:long to inter
		}
		return name;
	}
	public static ContactInfo loadContactInfo(ContentResolver cr, Uri personUri) {
		ContactInfo contactInfo = new ContactInfo();
        
		Cursor cursor = null;
		try {
			cursor = cr.query(personUri, new String[] { Contacts._ID,
					Contacts.DISPLAY_NAME }, null, null, null);
			if (cursor != null && cursor.getCount() > 0) {

				if (cursor.moveToFirst()) {
					contactInfo.id = cursor.getLong(0);
					contactInfo.name = cursor.getString(1);
				}

				contactInfo.photo = loadPhoto(cr, contactInfo.id);
				contactInfo.numbers = loadPhoneNumber(cr, contactInfo.id);
				contactInfo.emails = loadEmailAddress(cr, contactInfo.id);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

        return contactInfo;
	}
	
	public static Bitmap loadPhoto(ContentResolver cr, long person) {
		final Uri myUri = Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.CONTENT_URI, person), 
                Photo.CONTENT_DIRECTORY);
        final String wherePerson = Data.CONTACT_ID + "=" + person;
        Cursor photoCursor = null;
        Bitmap bm = null;

        try {
            photoCursor = cr.query(myUri, new String[] {ContactsContract.CommonDataKinds.Photo.PHOTO}, wherePerson, null, null);            
			if (photoCursor != null && photoCursor.getCount() > 0) {
				photoCursor.moveToFirst();
				if (!photoCursor.isAfterLast()) {
					byte[] data = photoCursor.getBlob(0);
					bm = BitmapFactory.decodeByteArray(data, 0, data.length,
							null);
				}
			}
        } catch (Exception e) {
			e.printStackTrace();
        } finally {
            if (photoCursor != null) {
                photoCursor.close();
            }
        }

        return bm;
	}
	
	public static ArrayList<PhoneNumber> loadPhoneNumber(ContentResolver cr, long personId) {
		final Uri myUri = Phone.CONTENT_URI;
		final String wherePerson = Data.CONTACT_ID + "=" + personId + " AND "
				+ Phone.NUMBER + " is not null" + " AND " + Phone.NUMBER
				+ " <> ''";
        ArrayList<PhoneNumber> PhoneNumberList = new ArrayList<PhoneNumber>();
        Cursor phoneCursor = null;

        try {
            phoneCursor = cr.query(myUri, PhoneNumber.NUMBER_PROJECTION, wherePerson, null, null);
            if (phoneCursor != null) {
                for (phoneCursor.moveToFirst(); !phoneCursor.isAfterLast();
                        phoneCursor.moveToNext()) {
                    PhoneNumber num = new PhoneNumber();
                    num.id = phoneCursor.getLong(PhoneNumber.ID_COLUMN_INDEX);
                    num.number = phoneCursor.getString(PhoneNumber.NUMBER_COLUMN_INDEX);
                    num.type = phoneCursor.getInt(PhoneNumber.TYPE_COLUMN_INDEX);
                    num.label = phoneCursor.getString(PhoneNumber.LABEL_COLUMN_INDEX);
					PhoneNumberList.add(num);					
                }
            }
        } catch (Exception e) {
			e.printStackTrace();
        } finally {
            if (phoneCursor != null) {
                phoneCursor.close();
            }
       }
       return PhoneNumberList;
	}
	
	public static ArrayList<EmailAddress> loadEmailAddress(ContentResolver cr, long personId) {
		final Uri myUri = Email.CONTENT_URI;
		final String wherePerson = Data.CONTACT_ID + "=" + personId + " AND "
				+ Email.DATA1 + " is not null" + " AND " + Email.DATA1
				+ " <> ''";
        ArrayList<EmailAddress> emailAddressList = new ArrayList<EmailAddress>();
        Cursor emailCursor = null;

        try {
            emailCursor = cr.query(myUri, EmailAddress.NUMBER_PROJECTION, wherePerson, null, null);
            if (emailCursor != null) {
                for (emailCursor.moveToFirst(); !emailCursor.isAfterLast();
                emailCursor.moveToNext()) {
                	EmailAddress num = new EmailAddress();
                    num.id = emailCursor.getLong(EmailAddress.ID_COLUMN_INDEX);
                    num.number = emailCursor.getString(EmailAddress.NUMBER_COLUMN_INDEX);
                    num.type = emailCursor.getInt(EmailAddress.TYPE_COLUMN_INDEX);
                    num.label = emailCursor.getString(EmailAddress.LABEL_COLUMN_INDEX);
					emailAddressList.add(num);
                }
            }
        } catch (Exception e) {
			e.printStackTrace();
        } finally {
            if (emailCursor != null) {
            	emailCursor.close();
            }
       }
       return emailAddressList;
	}
	
	public boolean setBitmap(Bitmap photo) {
		if (photo == null) {
			Log.d(TAG, "photo is null");
            return false;
        }
		
        final int size = photo.getWidth() * photo.getHeight() * 4;
        final ByteArrayOutputStream out = new ByteArrayOutputStream(size);

        try {
            photo.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
           
            
            ContentValues values = new ContentValues();
            values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
            values.put(ContactsContract.CommonDataKinds.Photo.PHOTO, out.toByteArray());
            
            String where = String.format("%s=? and %s=?", 
            		ContactsContract.Data.CONTACT_ID,
            		ContactsContract.Data.MIMETYPE);
            String[] selection = new String[] {String.valueOf(mContactInfo.id),
            		ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE};
            
            Cursor c = null;
			try {
				c = cr.query(ContactsContract.Data.CONTENT_URI,
						new String[] { ContactsContract.Data.MIMETYPE }, where,
						selection, null);
				String s = "";
				if(c != null && c.getCount() > 0){
					c.moveToFirst();
					for (int i = 0; i < c.getCount(); i++) {
						s += c.getString(0) + ",";
						c.moveToNext();
					}
				}
	
				if (c != null && c.getCount() > 0) {
					cr.update(ContactsContract.Data.CONTENT_URI, values, where, selection);
				} else {
					Log.w(TAG, "SetContactPhoto, No this person contact_id = "
							+ mContactInfo.id);
					Cursor cursorId = null;
					try {
						cursorId = cr.query(RawContacts.CONTENT_URI, 
										new String[] { RawContacts._ID },
										RawContacts.CONTACT_ID + "=?",
										new String[] { String.valueOf(mContactInfo.id) },
										null);
						long rawId = 0;
						if (cursorId != null && cursorId.getCount() > 0) {
							cursorId.moveToFirst();
							rawId = cursorId.getLong(0);
						}
						values.put(ContactsContract.Data.RAW_CONTACT_ID, rawId);
						values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);
						values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
						cr.insert(ContactsContract.Data.CONTENT_URI, values);

					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (cursorId != null) {
							cursorId.close();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (c != null) {
					c.close();
				}
			}
            
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Unable to serialize photo: " + e.toString());
        }
        
        return false;
	}
	
	public static Intent makeCall(String number) {
		Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
		return intent;
	}
	
	public static Intent makeSms(String number, String body) {
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
		intent.putExtra("sms_body", body); 
		return intent;
	}
	
	public static Intent makeEmail(String email) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		Uri data = Uri.parse("mailto:" + email);
		intent.setData(data);
		return intent;
	}
	
	public static Intent getPickContactIntent() {
        return new Intent("com.motorola.mmsp.socialGraph.SINGLE_CONTACTS_PICKER");
    }
	
	public static void viewPerson(Context context, Contact contact) {
    	Uri uri = ContactsContract.Contacts.CONTENT_URI;
        Uri personUri = ContentUris.withAppendedId(uri, contact.getPerson());
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(personUri);
        context.startActivity(intent);
    }
    
	public static void connectToPerson(Context context, int type, String address) {
    	Intent intent = null;
    	if (type == Constant.MESSAGE_TYPE_CALL) {
    		intent = Contact.makeCall(address);
    	} else if (type == Constant.MESSAGE_TYPE_MMS
    			|| type == Constant.MESSAGE_TYPE_SMS) {
    		intent = Contact.makeSms(address, null);
    	} else if (type == Constant.MESSAGE_TYPE_EMAIL) {
    		intent = Contact.makeEmail(address);
    	}
    	
		if (intent != null) {
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_SINGLE_TOP
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);
			try {
				context.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Toast.makeText(context, context.getString(R.string.email_activity_not_found), Toast.LENGTH_LONG).show();
			}
		}
    }
}
