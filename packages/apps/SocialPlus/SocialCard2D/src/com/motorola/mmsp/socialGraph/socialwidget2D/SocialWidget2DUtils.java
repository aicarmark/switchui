package com.motorola.mmsp.socialGraph.socialwidget2D;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.Photo;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.QuickContact;
import android.util.Log;

import com.motorola.mmsp.socialGraph.Constant;

public class SocialWidget2DUtils {

	private static final String TAG = "SocialGraph2DProvider";
	private static final boolean D = true;
	
	public static Bitmap loadPhoto(ContentResolver cr, int person) {
		if (person <= 0) {
			return null;
		}
		
		final Uri myUri = Uri.withAppendedPath(ContentUris.withAppendedId(Contacts.CONTENT_URI, person), 
                Photo.CONTENT_DIRECTORY);

        final String wherePerson = Data.CONTACT_ID + "=" + person;
        Cursor photoCursor = null;
        Bitmap bm = null;
        
        try {
            photoCursor = cr.query(myUri, new String[] {ContactsContract.CommonDataKinds.Photo.PHOTO}, wherePerson, null, null);
            if (photoCursor != null) {
				for (photoCursor.moveToFirst(); !photoCursor.isAfterLast(); photoCursor
						.moveToNext()) {
					byte[] data = photoCursor.getBlob(0);

					try {
						bm = BitmapFactory.decodeByteArray(data, 0,
								data.length, null);
					} catch (OutOfMemoryError e) {
						e.printStackTrace();
					}
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
	
	public static Bitmap loadPhotoCheckStretched(Context context, int person, int position) {
		Bitmap bm = loadPhoto(context.getContentResolver(),
				person);
		Bitmap dstBitmap = null;
		if (bm != null&&!bm.isRecycled()) {
			if (isStretchOn(context)) {
				Matrix matrix = new Matrix();
				int width = bm.getWidth();
				int height = bm.getHeight();
				float widthScale = (193f) / width;
				float heightScale = (193f) / height;
				if(D)Log.d(TAG, "widthScale = " + widthScale
						+ "heightScale = " + heightScale);
				matrix.postScale(widthScale, heightScale);
				try {
					dstBitmap = Bitmap.createBitmap(bm, 0, 0,
							width, height, matrix, false);
					bm.recycle();
					bm = null;
				if(D)Log.d(TAG, "dst bitmap width = "
						+ dstBitmap.getWidth()
						+ "dst bitmap height = "
						+ dstBitmap.getHeight());
				} catch (OutOfMemoryError e) {
					e.printStackTrace();
				}
			} else {
			    try{
				dstBitmap = resizeCover(position,bm);
				bm.recycle();
				bm = null;
				} catch (OutOfMemoryError e) {
					e.printStackTrace();
				}			
			}
		}		
		return dstBitmap;
	}
	
    private static Bitmap resizeCover(int position,Bitmap bm) {
        int size = 0;
        if(bm == null){
        	return null;
        }
        if(D)Log.d(TAG, "resizeCover position = " + position);
        switch(position){
        case 0:
        	 size = 120;
        	 break;
        case 1:
        case 2:
        case 3:
        	size = 80;
        	break;
        case 4:
        case 5:
        case 6:
        case 7:
        case 8:
        	size = 60;
        	break;
        default:
        	return bm;
        } 
        return Bitmap.createScaledBitmap(bm, size, size, true);
    }		
			
	public static Bitmap loadBitmapByName(Context context ,String str) {
		Context ct = getGraphContext(context);
		Resources r = null;
		if (ct != null) {
			r = ct.getResources();
		}
		int iconId;
		if (r != null) {
			iconId = r.getIdentifier(Constant.DRAWBLE_NAME_PREFIX + str, null, null);
		} else {
			iconId = -1;
		}
		if (iconId <= 0) {
			return null;
		} else {
			Bitmap bmp = null;
			try {
				bmp = BitmapFactory.decodeResource(r, iconId);
				return bmp;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
	public static Context getGraphContext(Context context) {
/*		Context graphContext = null;
		if (graphContext == null) {
			try {
				graphContext = context.createPackageContext(
						"com.motorola.mmsp.socialGraphWidget", 0);
			} catch (NameNotFoundException e) {
				graphContext = null;
				e.printStackTrace();
			}
		}*/
		return context;
	}
	
	public static boolean getOutOfBoxStatus(Context context) {
		Cursor c = null;
		int outOfBox = 0;

		boolean outOfBoxStatus = false;
		try {
			c = context.getContentResolver().query(Constant.MISCVALUE_CONTENT_URI,
					new String[] { Constant.MISC_VALUE }, Constant.MISC_KEY + "=?",
					new String[] { Constant.OUT_OF_BOX }, null);

			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				outOfBox = c.getInt(0);
			}

		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
			
			if(outOfBox > 0) {	
				outOfBoxStatus = true;
			} else {
				outOfBoxStatus = false;
			}
		}
		
		return outOfBoxStatus;
	}

	public static  int getShortcutNum(Context context) {
		Uri uri = Uri.withAppendedPath(Constant.CONFIG_CONTENT_URI, "count");
		if(D)Log.d(TAG, "ShortcutNum uri = " + uri);

		Cursor c = null;
		try {
			c = context.getContentResolver().query(uri, null, null, null, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				return c.getInt(c.getColumnIndex("value"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		
		return 0;
	}
	
	public static String loadName(ContentResolver cr, int personID) {
		String name = "";
		if (personID <= 0) {
			return null;
		}
		Cursor contactCursor = null;
		try {
			contactCursor = cr.query(Contacts.CONTENT_URI,
					new String[] { Contacts.DISPLAY_NAME },
					Contacts._ID + "=?", new String[] { String
							.valueOf(personID) }, null);
			if (contactCursor != null) {
				if (contactCursor.getCount() > 0) {
					contactCursor.moveToFirst();
					name = contactCursor.getString(0);

					if(name == null){
				      name = "";
					}

				}
			} else {
				name = "";
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (contactCursor != null) {
				contactCursor.close();
			}
		}
		return name;
	}
	
	public static  ArrayList<ChangeHistory> getChangeHistory(Context context) {
		ArrayList<ChangeHistory> histories = new ArrayList<ChangeHistory>();
		
		Cursor c = null;
		try {
		c = context.getContentResolver().query(
				Constant.CHANGE_HISTORY_CONTENT_URI,
				new String[] { ChangeHistory.HISTORY_TYPE,
						ChangeHistory.HISTORY_INDEX, 
						ChangeHistory.HISTORY_DATA1,
						ChangeHistory.HISTORY_DATA2 },
						null,
						null, 
						null);
			if (c != null && c.getCount() > 0) {
				for (int i = 0; i < c.getCount(); i++) {
					c.moveToNext();
					ChangeHistory item = new ChangeHistory();
					item.type = c.getInt(0);
					item.index = c.getInt(1);
					item.from = c.getInt(2);
					item.to = c.getInt(3);
					histories.add(item);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}

		return histories;
	}
	
	public static  void deleteHistory(Context context) {
		try {
			context.getContentResolver().delete(Constant.CHANGE_HISTORY_CONTENT_URI,
					null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static int getLaunchFlag() {
		return Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP;
	}
	
	public static void showSettingApp(Context context,boolean bConfig) {
        if(D)Log.d(TAG, "show setting app");
        Intent intent = new Intent();
        intent.setComponent(ComponentName.unflattenFromString("com.motorola.mmsp.socialGraph/.socialGraphWidget.ui.SettingActivity"));
        intent.setFlags(getLaunchFlag());
        intent.putExtra("Config", bConfig);
		if (context != null) {
			if(D)Log.d(TAG, "start activity");
            context.startActivity(intent);
		}
    }

	public static void showManageContactsAcitivity(Context context) {
		if(D)Log.d(TAG, "start manage contacts activity");
		Intent intent = new Intent(
				"com.motorola.mmsp.socialGraph.CONTACTS_MANAGE");
		intent.addFlags(getLaunchFlag());
		context.startActivity(intent);
	}
	
	public static void showWelcomeAcitivity(Context context) {
		if(D)Log.d(TAG, "start welcome activity");
		Intent intent = new Intent(
				"com.motorola.mmsp.socialGraph.WELCOME_INFO");
		intent.addFlags(getLaunchFlag());
		context.startActivity(intent);
	}
	
	public static void showHistoryActivity(Context context, int personId) {
		Intent intent = new Intent("android.intent.action.VIEW_CONTACT_HISTORY");
		intent.setFlags(getLaunchFlag());
		intent.putExtra("person", personId);
		context.startActivity(intent);
	}

	public static boolean isStretchOn(Context context) {
		Uri uri = Uri.withAppendedPath(Constant.CONFIG_CONTENT_URI, "stretch");
		int stretch = 0;
		Cursor c = null;
		try {
			c = context.getContentResolver().query(uri, null, null, null, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				stretch = c.getInt(c.getColumnIndex("value"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return (stretch != 0);
	}
	
	public static  boolean isAutoMode(Context context) {
		Uri uri = Uri.withAppendedPath(Constant.CONFIG_CONTENT_URI, "mode");
		int mode = 0;
		Cursor c = null;
		try {
			c = context.getContentResolver().query(uri, null, null, null, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				mode = c.getInt(c.getColumnIndex("value"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}
		return (mode != 0);
	}
	
	public static void showQuickContact(Context context, int personId) {
		if(D)Log.d(TAG, "zc~~~~~~start quick contact");
		Cursor c = null;
		String displayName=null;
		String lookupKey = null;
		byte[] icon = null;
		try {
			c = context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI,
					new String[] { ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.PHOTO_ID,ContactsContract.Contacts.DISPLAY_NAME}, ContactsContract.Contacts._ID + " = " +personId,
					null, null);
			if (c != null && c.getCount() > 0) {
				c.moveToFirst();
				displayName = c.getString(2);
				lookupKey = c.getString(0);
				//icon = c.getBlob(1);
			}
		
		
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}	
		}
		Uri contactLookupUri = Contacts.getLookupUri(personId, lookupKey);
		Intent intent = new Intent("com.android.contacts.action.QUICK_CONTACT");
        // modified by amt_sunli SWITCHUITWO-11 2012-11-20 begin
        //intent.setFlags(getLaunchFlag());
        //intent.setFlags(getLaunchFlag());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        // modified by amt_sunli SWITCHUITWO-11 2012-11-20 end
		intent.setData(contactLookupUri);

		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, displayName);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
		intent.putExtra("mode", QuickContact.MODE_SMALL);

		intent.setSourceBounds(new Rect(20, 20, 200, 200));		
		context.startActivity(intent);
	}	
	
}
