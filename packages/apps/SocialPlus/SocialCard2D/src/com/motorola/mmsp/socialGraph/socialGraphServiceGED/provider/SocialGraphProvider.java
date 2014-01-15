package com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;

import com.motorola.mmsp.socialGraph.socialGraphServiceGED.ConstantDefinition;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.ContactInfo;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.ContactInfoColumns;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.Frequency;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.FrequencyColumns;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.MiscValue;
import com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider.SocialGraphContent.MiscValueColumns;

/*
 * it is the social graph content provider
 */

public class SocialGraphProvider extends ContentProvider{

	private final String TAG = "SocialGraphService";

    public static final String DATABASE_NAME = "socialgraph.db";
    //version 1: original version
    public static final int DATABASE_VERSION = 1;   
    
    private static SQLiteDatabase mDatabase;
    private static Context mContext;
    public static final String SOCIALGRAPH_AUTHORITY = "com.motorola.mmsp.socialgraphservice.provider";
    
    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    
    private static final int CONTACTINFO_BASE 					= 0;
    private static final int CONTACTINFO 						= CONTACTINFO_BASE;
    private static final int CONTACTINFO_ID 					= CONTACTINFO_BASE + 1;    
       
    private static final int FREQUENCY_BASE 				= 0x1000;
    private static final int FREQUENCY 						= FREQUENCY_BASE;
    private static final int FREQUENCY_ID 					= FREQUENCY_BASE + 1;
    
    private static final int MISCVALUE 						= 0x2000;
    
    private static final int PRIVATE_EXCUTE_SQL				= 0x3000;
    
    private static final int PRIVATE_SET_DAY_RANKING		= 0x4000;
    
    private static final int PRIVATE_SET_TOTAL_RANKING		= 0x5000;
        
    private static final int BASE_SHIFT 					= 12;  // 12 bits to the base type: 0, 0x1000, 0x2000, etc.
    
    
    private static final String[] TABLE_NAMES = {        
        SocialGraphContent.ContactInfo.TABLE_NAME,
        SocialGraphContent.Frequency.TABLE_NAME,
        SocialGraphContent.MiscValue.TABLE_NAME,
    };
    
    static {
    	// Social graph URI matching table
        UriMatcher matcher = sURIMatcher;
        
    	// All messages
        matcher.addURI(SOCIALGRAPH_AUTHORITY, "socialcontacts", CONTACTINFO);
        // A specific message
        // insert into this URI causes an record to be added to the message
        matcher.addURI(SOCIALGRAPH_AUTHORITY, "socialcontacts/#", CONTACTINFO_ID);
        
        // Frequency per person
        matcher.addURI(SOCIALGRAPH_AUTHORITY, "frequency", FREQUENCY);
        // A specific person
        matcher.addURI(SOCIALGRAPH_AUTHORITY, "frequency/#", FREQUENCY_ID);
        
        // A specific person
        matcher.addURI(SOCIALGRAPH_AUTHORITY, "excute_sql", PRIVATE_EXCUTE_SQL);  
        
        matcher.addURI(SOCIALGRAPH_AUTHORITY, "set_ranking", PRIVATE_SET_DAY_RANKING);
        
        matcher.addURI(SOCIALGRAPH_AUTHORITY, "set_total_ranking", PRIVATE_SET_TOTAL_RANKING);
        
        matcher.addURI(SOCIALGRAPH_AUTHORITY, "miscvalue", MISCVALUE);
        
    }
    
    private String whereWithId(String id, String selection) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("_id=");
        sb.append(id);
        if (selection != null) {
            sb.append(" AND (");
            sb.append(selection);
            sb.append(')');
        }
        return sb.toString();
    }
    
    static void createContactInfoTable(SQLiteDatabase db) {
		String contactInfoColumns = 
				ContactInfoColumns.PERSON	     + " integer default 0 , " 
		      + ContactInfoColumns.INFO_URI      + " text default '' , " 
			  + ContactInfoColumns.TYPE          + " integer default 0  " 
		      + ");";
    	
        
		// This String and the following String MUST have the same columns,
		// except for the type
		// of those columns!
		String createString = " (" + SocialGraphContent.RECORD_ID
				+ " integer primary key autoincrement, " + contactInfoColumns;

		// create the table
		try {
			db.execSQL("create table  if not exists " + ContactInfo.TABLE_NAME
					+ createString);
		} catch (Exception e) {
			e.printStackTrace();
		}
     
    }
    
    static void createFrequencyTable(SQLiteDatabase db) {
        String frequencyColumns = 
        	  FrequencyColumns.PERSON 				+ " integer default 0 , "
            + FrequencyColumns.RANKING 				+ " integer default 0   "
            + ");";
        
		// This String and the following String MUST have the same columns,
		// except for the type
		// of those columns!
		String createString = " (" + SocialGraphContent.RECORD_ID
				+ " integer primary key autoincrement, " + frequencyColumns;

		// The three tables have the same schema
		try {
			db.execSQL("create table  if not exists " + Frequency.TABLE_NAME
					+ createString);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    static void createMiscValueTable(SQLiteDatabase db) {
        String miscValueColumns = 
        	  MiscValueColumns.MISC_KEY       		+ " text    default 	'', "
            + MiscValueColumns.MISC_VALUE      		+ " integer not null default     0   "
            + ");";
        
		// This String and the following String MUST have the same columns,
		// except for the type
		// of those columns!
		String createString = " (" + SocialGraphContent.RECORD_ID
				+ " integer primary key autoincrement, " + miscValueColumns;

		// The three tables have the same schema
		try {
			db.execSQL("create table  if not exists " + MiscValue.TABLE_NAME
					+ createString);
            db.execSQL("insert into miscvalue (misc_key,misc_value)values('last_contact',0);");
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public synchronized static SQLiteDatabase getDatabase(Context context) {
        if (mDatabase !=  null) {
            return mDatabase;
        }
      
        mDatabase = helper.getWritableDatabase();

        return mDatabase;
    }
    
    public static synchronized SQLiteDatabase getDatabase() {
       return getDatabase(mContext);
    }
    
    static SQLiteDatabase getReadableDatabase(Context context) {
        return helper.getReadableDatabase();
    }
    
    private class DatabaseHelper extends SQLiteOpenHelper{
    	
    	DatabaseHelper(Context context, String database_name){
    		super(context, database_name, null,DATABASE_VERSION);
    			
    	}
    	
    	@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion,int newVersion) {
    			
    			if (oldVersion < 1) {    				
    				db.execSQL("DROP TABLE IF EXISTS " + ContactInfo.TABLE_NAME);  
    				db.execSQL("DROP TABLE IF EXISTS " + Frequency.TABLE_NAME);
    				db.execSQL("DROP TABLE IF EXISTS " + MiscValue.TABLE_NAME);
    				return;
    			}
    	}
    	
    	@Override
    	public void onOpen(SQLiteDatabase db){
    		//Log.d(TAG, "begin to open the database");    		
    	}
    	
    	@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				if (ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD) {
					Log.d(TAG, "begin to create tables");
				}
				// Create all tables here; each class has its own method
				createContactInfoTable(db);
				createFrequencyTable(db);
				createMiscValueTable(db);

				SocialFlex socialFlex = SocialFlex.getInstance(getContext());
				writeTotalDaysToDb(socialFlex.getTotalDays(), db);
				writeOutOfBoxToDb(db);
				writeFirstAddedToDb(db);
				
			} catch (Exception ex) {
				if (ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD) {
					Log.e(getClass().getSimpleName(),
							"Could not create or Open the database");
				}
			}
		}
    }
    
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		final int match = sURIMatcher.match(uri);
        Context context = getContext();
        
        SQLiteDatabase db = getDatabase(context);
        int table = match >> BASE_SHIFT;
        String id = "0";
        boolean messageDeletion = false;

        if (ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD) {
            Log.d(TAG, "socialGraphProvider.delete: uri=" + uri + ", match is " + match);
        }

        int result = -1;
        
        try {
            switch (match) {
                // These are cases in which one or more Messages might get deleted, either by
                // cascade or explicitly
                case FREQUENCY_ID:
                case FREQUENCY:                
                case CONTACTINFO:
                case CONTACTINFO_ID:
                case MISCVALUE:
                    
                    messageDeletion = true;
                    db.beginTransaction();
                    break;
            }
            switch (match) {
                case FREQUENCY_ID:           
                case CONTACTINFO_ID:
                    id = uri.getPathSegments().get(1);
                    
                    result = db.delete(TABLE_NAMES[table], whereWithId(id, selection),
                            selectionArgs);
                    break;
                
                case CONTACTINFO:
                case FREQUENCY:
                case MISCVALUE:
                    result = db.delete(TABLE_NAMES[table], selection, selectionArgs);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown URI " + uri);
            }
            if (messageDeletion) {
                if (match == CONTACTINFO_ID) {
                    // TODO  to delete the related data
                    
                } else {
                    // TODO to delete the related data
                    //db.execSQL(DELETE_ORPHAN_BODIES);
                }
                db.setTransactionSuccessful();
            }
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (messageDeletion) {
				db.endTransaction();
			}
		}
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }        
		

	@Override
	public String getType(Uri uri) {
		int match = sURIMatcher.match(uri);
        switch (match) {
            case FREQUENCY_ID:
                return "vnd.android.cursor.item/socialgraph-frequency";
            case FREQUENCY:
                return "vnd.android.cursor.dir/socialgraph-frequency";            
            case CONTACTINFO_ID:
                return "vnd.android.cursor.item/socialgraph-socialcontacts";         
            case CONTACTINFO:
                return "vnd.android.cursor.dir/socialgraph-socialcontacts";
            case MISCVALUE:
                return "vnd.android.cursor.dir/socialgraph-miscvalue";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int match = sURIMatcher.match(uri);
        Context context = getContext();
        // See the comment at delete(), above
        SQLiteDatabase db = getDatabase(context);
        int table = match >> BASE_SHIFT;
        long id;

        
        if (ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD) {
            Log.d(TAG, "socialGraphProvider.insert: uri=" + uri + ", match is " + match);
        }
        

        Uri resultUri = null;
        
        switch (match) {
       
        case CONTACTINFO:
        case FREQUENCY:
        case MISCVALUE:
			try {
				id = db.insert(TABLE_NAMES[table], "foo", values);
				resultUri = ContentUris.withAppendedId(uri, id);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;        
        case CONTACTINFO_ID:
            // TODO
            break;
        case FREQUENCY_ID:
            // TODO
            break;
        default:
            throw new IllegalArgumentException("Unknown URL " + uri);
        }

        // Notify with the base uri, not the new uri (nobody is watching a new record)
        getContext().getContentResolver().notifyChange(uri, null);
        return resultUri;
	}
	
	static DatabaseHelper helper;
	
	@Override
	public boolean onCreate() {
		Log.d(TAG, "SocialGraphProvider onCreate()");
		// TODO Auto-generated method stub	
		mContext =getContext();
		helper = new DatabaseHelper(mContext, DATABASE_NAME);
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor c = null;
        Uri notificationUri = SocialGraphContent.CONTENT_URI;
        int match = sURIMatcher.match(uri);
        Context context = getContext();
        // See the comment at delete(), above
        SQLiteDatabase db = getReadableDatabase(context);
        int table = match >> BASE_SHIFT;
        String id;

        if (ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD) {
            Log.d(TAG, "socialGraphProvider.query: uri=" + uri + ", match is " + match);
        }
        
        switch (match) {
        
		case CONTACTINFO:
        	break;
        case FREQUENCY:
        case MISCVALUE:
			try {
				c = db.query(TABLE_NAMES[table], projection, selection,
						selectionArgs, null, null, sortOrder);
			} catch (Exception e) {
				e.printStackTrace();
			}
        	break;
        
        case CONTACTINFO_ID:
        case FREQUENCY_ID:
			try {
				id = uri.getPathSegments().get(1);
				c = db.query(TABLE_NAMES[table], projection, whereWithId(id,
						selection), selectionArgs, null, null, sortOrder);
			} catch (Exception e) {
				e.printStackTrace();
			}
            break;      
            
        case PRIVATE_EXCUTE_SQL:
			try {
				c = db.rawQuery(selection, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
        	break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        if ((c != null) && !isTemporary()) {
        	c.setNotificationUri(getContext().getContentResolver(), notificationUri);
        }
        return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int match = sURIMatcher.match(uri);
        Context context = getContext();
        // See the comment at delete(), above
        SQLiteDatabase db = getDatabase(context);
        int table = match >> BASE_SHIFT;
        int result = -1;

        if (ConstantDefinition.SOCIAL_GRAPH_SOCIAL_LOGD) {
            Log.d(TAG, "socialGraphProvider.update: uri=" + uri + ", match is " + match);
        }
        
        String id;
        switch (match) {
       
        case CONTACTINFO_ID:
        case FREQUENCY_ID:
			try {
				id = uri.getPathSegments().get(1);

				result = db.update(TABLE_NAMES[table], values, whereWithId(id,
						selection), selectionArgs);
			} catch (Exception e) {
				e.printStackTrace();
			}
            break;
            
        case CONTACTINFO:
        case FREQUENCY:
        case MISCVALUE:
			try {
				result = db.update(TABLE_NAMES[table], values, selection,
						selectionArgs);
			} catch (Exception e) {
				e.printStackTrace();
			}
            break;
        case PRIVATE_EXCUTE_SQL:
			try {
				if (selectionArgs == null) {
					db.execSQL(selection);
				} else {
					db.execSQL(selection, selectionArgs);
				}
				result = 1;
			} catch (Exception e) {
				e.printStackTrace();
			}
        	break;
        	
        case PRIVATE_SET_DAY_RANKING:
        	break;
        	
        case PRIVATE_SET_TOTAL_RANKING:
			break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return result;
	}	
	
	private boolean writeTotalDaysToDb(int days, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(MiscValueColumns.MISC_KEY, ConstantDefinition.MISC_TOTAL_DAY);
		values.put(MiscValueColumns.MISC_VALUE, days);
		
		try {
			db.insert(SocialGraphContent.MiscValue.TABLE_NAME, "foo", values);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private boolean writeOutOfBoxToDb(SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(MiscValueColumns.MISC_KEY, ConstantDefinition.MISC_OUT_OF_BOX);
		values.put(MiscValueColumns.MISC_VALUE, 0);
		
		try {
			db.insert(SocialGraphContent.MiscValue.TABLE_NAME, "foo", values);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
	private boolean writeFirstAddedToDb(SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(MiscValueColumns.MISC_KEY, ConstantDefinition.MISC_FIRST_ADDED);
		values.put(MiscValueColumns.MISC_VALUE, 0);
		
		try {
			db.insert(SocialGraphContent.MiscValue.TABLE_NAME, "foo", values);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}
	
}

final class SocialFlex {
	private static final String TAG = "SocialFlex";
	private static final String TAG_RANKINGDAYS = "social-ranking-days";
	private static SocialFlex instance = null;
	private static Object o = new Object();

	private Context mContext;
	private int mTotalDays;

	private SocialFlex(Context context) {
		mContext = context;
	}

	public static SocialFlex getInstance(Context context) {

			synchronized (o) {
				if (instance == null) {
					instance = new SocialFlex(context);
					if (!loadMotoSocialFromFlex(instance)) {
						instance.setTotalDays(ConstantDefinition.MISC_TOTAL_DAY_DEFAULT);
					}
				}
			}

		return instance;
	}

	public void setTotalDays(int days) {
		mTotalDays = days;
	}

	public int getTotalDays() {
		return mTotalDays;
	}
	
	private static boolean loadMotoSocialFromFlex(SocialFlex socialFlex) {
		String xmlstr = "";
		TypedValue isCDA = new TypedValue();
		TypedValue contentCDA = new TypedValue();
		Resources r = socialFlex.mContext.getResources();
		try {
			r.getValue("@MOTOFLEX@isCDAValid", isCDA, false);
			Log.d(TAG, "Social isCDA to string = " + isCDA.coerceToString());
			if (isCDA.coerceToString().equals("true")) {
				Log.e(TAG, "is cda true");
				r.getValue("@MOTOFLEX@getSocialGraphSetting", contentCDA, false);
				xmlstr = contentCDA.coerceToString().toString();
				Log.e(TAG, "xmlstr =" + xmlstr);
				if (!xmlstr.trim().equals("")) {
					Log.e(TAG, "xmlstr get ok");
				} else {
					Log.e(TAG, "xmlstr is empty");
					return false;
				}
			} else {
				Log.e(TAG, "is cda false");
				return false;
			}
		} catch (Exception e) {
			Log.e(TAG, "e " + e);
			e.printStackTrace();
			return false;
		}
		
		Element root = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;

		if ((null != xmlstr) && (xmlstr.length()>0)) {
			try {
				dbf = DocumentBuilderFactory.newInstance();
				db = dbf.newDocumentBuilder();
				Document xmldoc = db.parse(new InputSource(new StringReader(
						xmlstr)));
				root = xmldoc.getDocumentElement();
				NodeList list = root.getChildNodes();
				if (list != null) {
					int len = list.getLength();
					for (int i = 0; i < len; i++) {
						Node currentNode = list.item(i);
						String name = currentNode.getNodeName();
						String value = currentNode.getTextContent();
						if (value != null && !value.equals("") && name != null
								&& name.equals(TAG_RANKINGDAYS)) {
							Log.d(TAG, "init social-ranking-days with flex value ok");
							int days = Integer.valueOf(value);
							if ((days >= ConstantDefinition.MISC_TOTAL_DAY_MIN)
									&& (days <= ConstantDefinition.MISC_TOTAL_DAY_MAX)) {
								socialFlex.setTotalDays(days);
							} else {
								if (name != null) {
									Log.d(TAG, "name = " + name);
								}
								if (value != null) {
									Log.d(TAG, "value = " + value);
								}
								Log.d(TAG, "write database ok");
								return false;
							}
						} else {
							if (name != null) {
								Log.d(TAG, "name = " + name);
							} else {
								Log.d(TAG, "name = null");
							}
							if (value != null) {
								Log.d(TAG, "value = " + value);
							} else {
								Log.d(TAG, "value = null");
							}
							Log.d(TAG, "init social-ranking-days with flex value bad");
						}
					}
					return true;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Log.d(TAG, "get from flex fail");
		return false;
	}

}
