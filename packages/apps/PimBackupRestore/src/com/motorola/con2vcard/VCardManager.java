package com.motorola.con2vcard;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.android.vcard2.VCardComposer;
import com.android.vcard2.VCardConfig;
import com.android.vcard2.VCardEntry;
import com.android.vcard2.VCardEntryConstructor;
import com.android.vcard2.VCardParser_V21;
import com.android.vcard2.VCardParser_V30;
import com.android.vcard2.VCardEntry.EmailData;
import com.android.vcard2.VCardEntry.ImData;
import com.android.vcard2.VCardEntry.NameData;
import com.android.vcard2.VCardEntry.NicknameData;
import com.android.vcard2.VCardEntry.NoteData;
import com.android.vcard2.VCardEntry.OrganizationData;
import com.android.vcard2.VCardEntry.PhoneData;
import com.android.vcard2.VCardEntry.PhotoData;
import com.android.vcard2.VCardEntry.PostalData;
import com.android.vcard2.VCardEntry.WebsiteData;
import com.android.vcard2.exception.VCardException;
import com.android.vcard2.exception.VCardInvalidLineException;
import com.motorola.sdcardbackuprestore.Constants;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;

import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.BaseTypes;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.Groups;
import android.util.Log;

public class VCardManager {
        private static final String TAG = "VCardManager";

	private static VCardManager mInstance = null ;
    	public static final int ENCODING_MAY_ASCII = 0x01;
    	public static final int ENCODING_MAY_GBK = 0x02;
    	public static final int ENCODING_MAY_UTF8 = 0x04;
	
	private static final String DEFAULT_ACCOUNT_NAME = "local-contacts";
	private static final String DEFAULT_ACCOUNT_TYPE = "com.local.contacts";
   
	private static final int OP_TYPE_NEW = 1;
	private static final int OP_TYPE_UPDATE = 2;
	private static final int OP_TYPE_DELETE = 3;
	
	private Context mContext = null;
	private ContentResolver mResolver = null;
	private VCardEntry mContactStruct = null;
	private String mAccountName = null;
	private String mAccountType = null;	
	ArrayList<ContentProviderOperation> mOPList = new ArrayList<ContentProviderOperation>();
	ArrayList<Integer> mIndex = new ArrayList<Integer>();
	ArrayList<Integer> mGroupIds = null;
	ArrayList<String> mGroupNames = null;
	HashMap<Integer,String> mGroups = null;
	private boolean mNeedCreateGroup = false;
	private boolean mNeedCategory = false;
	private boolean mIsSyncAdapter = false;
  	private String mFamilyName = "";
  	private String mMiddleName = "";
    private String mGivenName = "";
    private String mPhotoPath = null;
    int mPhotoNumAdded = 0 ;
    
    public static long mAllApplyBatchTime = 0;
	
	public static VCardManager getInstance(Context context, boolean needCategory, boolean needCreateGroup, boolean isSyncAdapter){
	    if(mInstance == null){
		mInstance = new VCardManager();
		mInstance.mGroups = null;
		mInstance.mGroupNames = null;
		mInstance.mGroupIds = null;
		mInstance.mContactStruct = null;
	    }

	    mInstance.mContext = context;
	    mInstance.mResolver = context.getContentResolver();
	    mInstance.mNeedCreateGroup = needCreateGroup;
	    mInstance.mNeedCategory = needCategory;
	    mInstance.mIsSyncAdapter = isSyncAdapter;
	    
    	    /*will refresh groups information upon getInstance.
 	      To avoid unnecessary database query which will decrease performance , application should save the instance 
	      if believe group information won't change during the life cycle.
	    */
	    mInstance.mAccountName = DEFAULT_ACCOUNT_NAME;
	    mInstance.mAccountType = DEFAULT_ACCOUNT_TYPE;
	    mInstance.getGroupsInternal(mInstance.mResolver);
	    return mInstance;
	}
	
	public static VCardManager getInstance(Context context, boolean needCategory, 
			boolean needCreateGroup, boolean isSyncAdapter, String accountName, String accountType){
	    if(mInstance == null){
		mInstance = new VCardManager();
		mInstance.mGroups = null;
		mInstance.mGroupNames = null;
		mInstance.mGroupIds = null;
		mInstance.mContactStruct = null;
	    }

	    mInstance.mContext = context;
	    mInstance.mResolver = context.getContentResolver();
	    mInstance.mNeedCreateGroup = needCreateGroup;
	    mInstance.mNeedCategory = needCategory;
	    mInstance.mIsSyncAdapter = isSyncAdapter;
	    mInstance.mAccountName = accountName;
	    mInstance.mAccountType = accountType;
	    
    	    /*will refresh groups information upon getInstance.
 	      To avoid unnecessary database query which will decrease performance , application should save the instance 
	      if believe group information won't change during the life cycle.
	    */
	    mInstance.getGroupsInternal(mInstance.mResolver);
	    return mInstance;
	}
	
	private VCardManager() {

	}

	public void SetCtx(Context context) {
		mContext = context;
		mResolver = context.getContentResolver();
	}

 	public Uri add(String vc) throws VCardInvalidLineException {
 		mContactStruct = null;
 		parse(vc);
		if (mContactStruct == null) {
		    return null;
		} else {
			return pushIntoContentResolver(mResolver, OP_TYPE_NEW, null, mContactStruct);
			}
        }
  
 	public int addNoCommit(String vc) throws VCardInvalidLineException {
 		parse(vc);
 		return buildOPlist(mContactStruct);
 		
 	}
 	
 	public int addNoCommit(String vc, String accName, String accType) throws VCardInvalidLineException {
 		if (accName != null && accType != null) {
 			mAccountName = accName;
 	 		mAccountType = accType;
		} else {
			mAccountName = DEFAULT_ACCOUNT_NAME;
			mAccountType = DEFAULT_ACCOUNT_TYPE;
		}
 		parse(vc);
 		return buildOPlist(mContactStruct); 		
 	}
 	
 	public ArrayList<Uri> commitAddedContacts(ContentResolver resolver){
 		return commitOP(resolver);
 	}
 	
	public Uri add(String vc, String accountName, String accountType) throws VCardInvalidLineException {
		mContactStruct = null;
		if(accountName != null) {
			mAccountName = accountName;
		}
		if(accountType != null) {
			mAccountType = accountType;
		}
		parse(vc);
		if(mContactStruct != null) {
			return pushIntoContentResolver(mResolver, OP_TYPE_NEW, null, mContactStruct);
		} else {
			return null;
		}
	}
	
	public Uri update(Uri uri , String vc) throws VCardInvalidLineException {
		mContactStruct = null;
		parse(vc);
		if(mContactStruct != null) {
			return pushIntoContentResolver(mResolver, OP_TYPE_UPDATE, uri, mContactStruct);
		} else {
			return null;
		}
    	}
	
	public Uri Delete(Uri uri) {
		return pushIntoContentResolver(mResolver, OP_TYPE_DELETE, uri, null);
	}

	public String getVCard(Uri uri) {
		return getVCardInternal(uri, true);
    	}    
	
	public String getVCard(long contact_id) {
		return getVCardInternal(contact_id, false);
	}

	public String getVCardNoBin(Uri uri) {
		return getVCardInternal(uri, false);
    	}   
	
	private String getVCardInternal(Uri uri, boolean needBin) {
		Log.d(TAG , "uri = " + uri.toString());
		
		String sid = uri.getPathSegments().get(1);
		int contact_id = Integer.valueOf(sid);
		return getVCardInternal(contact_id, needBin);
    	}    

	private String getVCardInternal(long contact_id, boolean needBin) {
        Log.d(TAG, "contact_id = " + contact_id);

        ComposerHandler h = new ComposerHandler();
        VCardComposer composer = null;
        String vc = null;
        int vcardType = VCardConfig.VCARD_TYPE_DEFAULT;
        if (!needBin) {
            vcardType = vcardType | VCardConfig.FLAG_REFRAIN_IMAGE_EXPORT;
        }
        try {
            composer = new VCardComposer(mContext, vcardType);
            if (mNeedCategory == false) {
                composer.setGroups(null);
            } else {
                composer.setGroups(mGroups);
            }
            composer.setPhotoPath(mPhotoPath);
            /*
             * ICS Porting: Lib do not support composer.addHandler(h);
             */
            if (contact_id == -1) {
                return null;
            }
            if (!composer.init("_id=" + contact_id, null)) {
                Log.d(TAG, "composer init fail");
                return null;
            }
            while (!composer.isAfterLast()) {
                if ((vc = composer.createOneEntry()) == null) {
                    Log.d(TAG, "composer createOneEntry fail");
                    return null;
                }
            }
        } finally {
            if (composer != null) {
                composer.terminate();
            }
        }
        /*
         * Log.d(TAG, "VCARD: " + h.getVC()); return h.getVC();
         */
        return vc;
	}
	
	private void parse(String vc) throws VCardInvalidLineException {
		ParseHandler h = new ParseHandler();
		VCardEntryConstructor ec = new VCardEntryConstructor();
		ec.addEntryHandler(h);
        ByteArrayInputStream is = null;       

        	try {
        		VCardParser_V21 vCardParser = new VCardParser_V21();
        		is = new ByteArrayInputStream(vc.getBytes()) ;
			if (is == null) return;
             		vCardParser.parse(is, ec);
        	} catch (VCardException e) {
        		if (e instanceof VCardInvalidLineException) {
					throw new VCardInvalidLineException();
				}
        	 	e.printStackTrace();
        	 	try {
                 		is.close();
             		} catch (IOException e1) {
            	 		e1.printStackTrace();
             		}
             		try {
            	 		is = new ByteArrayInputStream(vc.getBytes()) ;
				if (is == null) return;
            	 		VCardParser_V30 vCardParser = new VCardParser_V30();
            	 		vCardParser.parse(is, ec);
             		} catch (VCardException e2) {
            	 		e2.printStackTrace();
             		} catch (IOException e4) {
                        	e4.printStackTrace();
			}
        	} catch (IOException e3) {
                        e3.printStackTrace();
                } finally {
        		if (is != null) {
        			try {
        				is.close();
        			} catch (IOException e) {
        				e.printStackTrace();
        			}
        		}
        	}
        	mContactStruct = h.getContactStruct();
	}
	private ArrayList<Uri> commitOP(ContentResolver resolver){
		long pre_time = System.currentTimeMillis();
		if(mOPList == null || mOPList.size() == 0) {
			return null;
		}
		
		ArrayList<Uri> list = new ArrayList<Uri>();
		 try {
			 ContentProviderResult[] result = 
			     resolver.applyBatch(ContactsContract.AUTHORITY, mOPList);
			 	
			 for (Integer index : mIndex) {
				 list.add(result[index.intValue()].uri);
			 }	      
	        } catch (RemoteException e) {
	            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
	            return null;
	        } catch (OperationApplicationException e) {
	            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
	            return null;
	        }
	        
	        mOPList.clear();
	        mIndex.clear();
	        mPhotoNumAdded = 0;
        mAllApplyBatchTime += (System.currentTimeMillis() - pre_time)/1000;
	    Log.d("Contacts Manager Operation Time", "ApplyBatch time: " + (System.currentTimeMillis() - pre_time)/1000 + "s");
		return list;
	}
	
	//will return the buffered operation number, caller should make sure 
	//the batched number not exceed platform allow (currently it is 500)
    private int buildOPlist(VCardEntry contactStruct) {
	ContentProviderOperation.Builder builder = null;
	if(contactStruct == null) return -1;
	int numOperations = mOPList.size();
	mIndex.add(numOperations);

	// Add a16516 IKQLTTCW-1729
        mFamilyName = "";
        mMiddleName = "";
        mGivenName = "";
        // End a16516 IKQLTTCW-1729
        
        //only used to add contacts.
	builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI));
	builder.withValues(new ContentValues());
	builder.withValue(RawContacts.ACCOUNT_NAME, mAccountName);
	builder.withValue(RawContacts.ACCOUNT_TYPE, mAccountType);
	//modified by amt_jiayuanyuan 2013-01-28 SWITCHUITWO-49 begin
	builder.withValue(RawContacts.AGGREGATION_MODE, 2);
	//modified by amt_jiayuanyuan 2013-01-28 SWITCHUITWO-49 end
	builder.withYieldAllowed(true);//add for SWITCHUITWO-116 xtp876
	mOPList.add(builder.build());

	NameData nameData = contactStruct.getNameData();
         //name
    String prefix = nameData.getPrefix();
    String familyName = nameData.getFamily();
    String middleName = nameData.getMiddle();
	String givenName = nameData.getGiven();
	String suffix = nameData.getSuffix();
	/*
	 * ICS Porting: Lib do not support    */
	// modified by amt_jiayuanyuan 2013-01-07 SWITCHUITWO-390 begin
	String phoneticFamilyName = nameData.getmPhoneticFamily();
    String phoneticGivenName = nameData.getmPhoneticGiven();
    String phoneticMiddleName = nameData.getmPhoneticMiddle();
    // modified by amt_jiayuanyuan 2013-01-07 SWITCHUITWO-390 end
	builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, numOperations);
	builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
	if(prefix != null) builder.withValue(StructuredName.PREFIX, prefix);
	if(familyName != null) builder.withValue(StructuredName.FAMILY_NAME, familyName);
	if(Constants.isTDModel()){
		if(middleName != null) builder.withValue(StructuredName.MIDDLE_NAME, "");
		// modified by amt_jiayuanyuan 2013-01-07 T810T-JB_P000535 begin
		else {
			middleName = "";
		}
		// modified by amt_jiayuanyuan 2013-01-07 T810T-JB_P000535 begin
		if(givenName != null) builder.withValue(StructuredName.GIVEN_NAME, middleName+givenName);
	}else{
		if(middleName != null) builder.withValue(StructuredName.MIDDLE_NAME, middleName);
		if(givenName != null) builder.withValue(StructuredName.GIVEN_NAME, givenName);	
	}
	if(suffix != null) builder.withValue(StructuredName.SUFFIX, suffix);
	/*
	 * ICS Merge: Lib do not support  */
	// modified by amt_jiayuanyuan 2013-01-07 SWITCHUITWO-390 begin
	if(phoneticFamilyName != null) builder.withValue(StructuredName.PHONETIC_FAMILY_NAME, phoneticFamilyName);
    if(phoneticGivenName != null) builder.withValue(StructuredName.PHONETIC_GIVEN_NAME, phoneticGivenName);
    if(phoneticMiddleName != null) builder.withValue(StructuredName.PHONETIC_MIDDLE_NAME, phoneticMiddleName); 
    // modified by amt_jiayuanyuan 2013-01-07 SWITCHUITWO-390 end
	mOPList.add(builder.build());

	//NickName
	    List<NicknameData> nickNameDataList = contactStruct.getNickNameList();
	    if (nickNameDataList != null && nickNameDataList.size() > 0) {
            for (NicknameData nickName : nickNameDataList) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Nickname.RAW_CONTACT_ID, numOperations);
                builder.withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
                builder.withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT);
                builder.withValue(Nickname.NAME, nickName.getNickname());
                mOPList.add(builder.build());
            }
        }
  
        //Phone
        List<PhoneData>  phoneList = contactStruct.getPhoneList();
        if (phoneList != null) {
            for (PhoneData phoneData : phoneList) {
            	Log.d(TAG, phoneData.toString());
	        builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
                builder.withValueBackReference(Phone.RAW_CONTACT_ID, numOperations);
     		builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
                builder.withValue(Phone.TYPE, phoneData.getType());
                if (phoneData.getType() == BaseTypes.TYPE_CUSTOM) {
                    builder.withValue(Phone.LABEL, phoneData.getLabel());
                }
                builder.withValue(Phone.NUMBER, phoneData.getNumber());
                if (phoneData.isPrimary()) {
                    builder.withValue(Data.IS_PRIMARY, 1);
                	// modified by amt_jiayuanyuan 2013-01-10 SWITCHUITWO-404 begin 
                    builder.withValue(Data.IS_SUPER_PRIMARY, 1);
                	// modified by amt_jiayuanyuan 2013-01-10 SWITCHUITWO-404 end 
                }
                mOPList.add(builder.build());
            }
        }
        
        //Organization
        List<OrganizationData> organizationList = contactStruct.getOrganizationList();
        if (organizationList != null) {
            boolean first = true;
            for (OrganizationData organizationData : organizationList) {
	        builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	        builder.withValueBackReference(Organization.RAW_CONTACT_ID, numOperations);
	        builder.withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
	        //Currently, we do not use TYPE_CUSTOM.
	        builder.withValue(Organization.TYPE, organizationData.getType());
	        builder.withValue(Organization.COMPANY, organizationData.getOrganizationName());
	        builder.withValue(Organization.TITLE, organizationData.getTitle());
	        if (first) {
	        	builder.withValue(Data.IS_PRIMARY, 1);
	        }
	        mOPList.add(builder.build());
            }
        }
        
        List<EmailData> emailList = contactStruct.getEmailList();
        if (emailList != null) {
            for (EmailData emailData : emailList) {
	        builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
            	builder.withValueBackReference(Email.RAW_CONTACT_ID, numOperations);
		builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
                builder.withValue(Email.TYPE, emailData.getType());
                if (emailData.getType() == BaseTypes.TYPE_CUSTOM) {
                    builder.withValue(Email.LABEL, emailData.getLabel());
                }
                builder.withValue(Email.DATA, emailData.getAddress());
                if (emailData.isPrimary()) {
                    builder.withValue(Data.IS_PRIMARY, 1);
                 // modified by amt_jiayuanyuan 2013-01-07 SWITCHUITWO-574 begin
                    builder.withValue(Data.IS_SUPER_PRIMARY, 1);
                 // modified by amt_jiayuanyuan 2013-01-07 SWITCHUITWO-574 end
                }
                mOPList.add(builder.build());
            }
        }
        
        List<PostalData> postalList = contactStruct.getPostalList();
        if (postalList != null) {
            for (PostalData postalData : postalList) {
	            builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	     	    builder.withValueBackReference(StructuredPostal.RAW_CONTACT_ID, numOperations);
	     	    builder.withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
	     	    builder.withValue(StructuredPostal.TYPE, postalData.getType());
	     	    if (postalData.getType() == BaseTypes.TYPE_CUSTOM) {
        	        builder.withValue(StructuredPostal.LABEL, postalData.getLabel());
	     	    }
	     	    builder.withValue(StructuredPostal.POBOX, postalData.getPobox());
	     	    // Extended address is dropped since there's no relevant entry in ContactsContract.
	     	    builder.withValue(StructuredPostal.STREET, postalData.getStreet());
	     	    builder.withValue(StructuredPostal.CITY, postalData.getLocalty());
	     	    builder.withValue(StructuredPostal.REGION, postalData.getRegion());
	     	    builder.withValue(StructuredPostal.POSTCODE, postalData.getPostalCode());
	     	    builder.withValue(StructuredPostal.COUNTRY, postalData.getCountry());

	     	    builder.withValue(StructuredPostal.FORMATTED_ADDRESS,
            	    postalData.getFormattedAddress(VCardConfig.VCARD_TYPE_V21_GENERIC));
	     	    if (postalData.isPrimary()) {
        	        builder.withValue(Data.IS_PRIMARY, 1);
	     	    }
	     	   mOPList.add(builder.build());
            }
        }
                
        List<NoteData> noteList = contactStruct.getNotes();
        if (noteList != null) {
            for (NoteData note : noteList) {
	        builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
         	builder.withValueBackReference(Note.RAW_CONTACT_ID, numOperations);
	        builder.withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE);
                builder.withValue(Note.NOTE, note.getNote());
                mOPList.add(builder.build());
            }
        }

   		List<PhotoData> photoList = contactStruct.getPhotoList();
        if (photoList != null) {
            boolean first = true;
            for (PhotoData photoData : photoList) {
	        builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
                builder.withValueBackReference(Photo.RAW_CONTACT_ID, numOperations);
                builder.withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
                builder.withValue(Photo.PHOTO, photoData.getBytes());
                if (first) {
                    builder.withValue(Data.IS_PRIMARY, 1);
                    first = false;
                }
                mOPList.add(builder.build());
            }
        }
        
        List<WebsiteData> websiteList = contactStruct.getWebsiteList();
        if (websiteList != null) {
            for (WebsiteData website : websiteList) {
	        builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	     	builder.withValueBackReference(Website.RAW_CONTACT_ID, numOperations);
	        builder.withValue(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE);
                builder.withValue(Website.URL, website.getWebsite());
                mOPList.add(builder.build());
            }
        }
        
        String birthday = contactStruct.getBirthday();
        if (birthday!=null && birthday.length()>0) {
	    builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
            builder.withValueBackReference(Event.RAW_CONTACT_ID, numOperations);
	    builder.withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE);
            builder.withValue(Event.START_DATE, birthday);
            builder.withValue(Event.TYPE, Event.TYPE_BIRTHDAY);
            mOPList.add(builder.build());
        }
        
	List<ImData> mImList = contactStruct.getImList();
        if (mImList != null) {
            for (ImData imData : mImList) {
                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
                builder.withValueBackReference(Im.RAW_CONTACT_ID, numOperations);
                builder.withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE);
                builder.withValue(Im.TYPE, imData.getType());
                builder.withValue(Im.PROTOCOL, imData.getProtocol());
                builder.withValue(Im.DATA, imData.getAddress());
                if (imData.getProtocol() == Im.PROTOCOL_CUSTOM) {
                    builder.withValue(Im.CUSTOM_PROTOCOL, imData.getCustomProtocol());
                }
                if (imData.isPrimary()) {
                    builder.withValue(Data.IS_PRIMARY, 1);
                }
                mOPList.add(builder.build());
            }
        }

        List<String> cates = contactStruct.getCategories();
        if (cates!=null && cates.size()>0) {
            if(mNeedCreateGroup){
        	createGroups(mResolver, cates);
            }	        	
            ArrayList<Integer> ids = getIdsbyNames(cates);	        	
            for(Integer id : ids) {
	        builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
        	builder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        	builder.withValueBackReference(Event.RAW_CONTACT_ID, numOperations);        	
        	builder.withValue(GroupMembership.GROUP_ROW_ID, id);
        	mOPList.add(builder.build());
            }
        }

        //add big photo
        addPhoto(contactStruct, numOperations);
     
	// Add a16516 IKQLTTCW-1729
        mFamilyName = familyName;
        mMiddleName = middleName;
        mGivenName = givenName;
        // End a16516 IKQLTTCW-1729

        //one big photo equal to 50 normal contacts operation.
	    return mOPList.size() + mPhotoNumAdded * 50;	
	}

  //add photo
    private void addPhoto(VCardEntry contactStruct, int numOp) {
    	ContentProviderOperation.Builder builder = null;
        String pfilePath = null;
        String photoId = contactStruct.getPhotoFileId();
        long fileLen = 0;
        byte[] photoByte = null;
        
        if(photoId == null){
        	return;
        }
        pfilePath = mPhotoPath + "/" + photoId;
        File f = new File(pfilePath);
        if(!f.exists()){
        	return;
        }
        fileLen= f.length(); 
        photoByte = new byte[(int)fileLen];
        
        FileInputStream  fi = null;
        try {
        	fi = new FileInputStream(pfilePath);
        	fi.read(photoByte, 0, (int)fileLen);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
            if (fi != null) {
            	try {
            		fi.close();
            	} catch (IOException e) {
            		e.printStackTrace();
            	}
        	} 
         }
		
		builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
		builder.withValue(Photo.PHOTO, photoByte);
		builder.withValue(RawContacts.Data.IS_SUPER_PRIMARY, 1);
		builder.withValue(Photo.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
		builder.withValueBackReference(Event.RAW_CONTACT_ID, numOp);
		mOPList.add(builder.build());
		mPhotoNumAdded ++ ;
	}

	private Uri pushIntoContentResolver(ContentResolver resolver , int opType , Uri uri,
			 VCardEntry contactStruct ) {
	        ArrayList<ContentProviderOperation> operationList =
	            new ArrayList<ContentProviderOperation>();  
	        ContentProviderOperation.Builder builder = null;
	        int raw_contact_id = 0 ;

		if(contactStruct == null) return null;

   		mResolver = resolver;
	        
	        //delete all rows related to this contact.
	        if(opType == OP_TYPE_UPDATE) {	        	
	        	String sid = uri.getPathSegments().get(1);
	        	raw_contact_id = Integer.valueOf(sid);
	        	resolver.delete(addCallerIsSyncAdapterParameter(Data.CONTENT_URI), 
			    StructuredName.RAW_CONTACT_ID + "=" + raw_contact_id , null);
	        }
	        
	        if (opType == OP_TYPE_NEW) {
	        	builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI));
	        	builder.withValues(new ContentValues());
	        	builder.withValue(RawContacts.ACCOUNT_NAME, mAccountName);
		        builder.withValue(RawContacts.ACCOUNT_TYPE, mAccountType);
		    	builder.withYieldAllowed(true);//add for SWITCHUITWO-116 xtp876
	        } else if(opType == OP_TYPE_UPDATE) {
	        	builder = ContentProviderOperation.newUpdate(addCallerIsSyncAdapterParameter(uri));
	        	builder.withValues(new ContentValues());
	        	builder.withValue(RawContacts.ACCOUNT_NAME, mAccountName);
	        	builder.withValue(RawContacts.ACCOUNT_TYPE, mAccountType);
	        	builder.withYieldAllowed(true);//add for SWITCHUITWO-116 xtp876
	        } else if(opType == OP_TYPE_DELETE) {
	        	builder = ContentProviderOperation.newDelete(addCallerIsSyncAdapterParameter(uri));
	        } else {
	        	throw new IllegalArgumentException("Operation type wrong.");
	        }

	        operationList.add(builder.build());	        
	        
	        if(opType != OP_TYPE_DELETE) {
	        	
	        //Name	
	        {
	        NameData nameData = contactStruct.getNameData();
	        String prefix = nameData.getPrefix();
	        String familyName = nameData.getFamily();
	        String middleName = nameData.getMiddle();
		    String givenName = nameData.getGiven();
		    String suffix = nameData.getSuffix();
		    /*
		     * ICS Porting: Lib do not support
		    String phoneticFamilyName = contactStruct.getPhoneticFamilyName();
		    String phoneticGivenName = contactStruct.getPhoneticGivenName();
		    String phoneticMiddleName = contactStruct.getPhoneticMiddleName();
            */
		    
		    builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
		    if(opType == OP_TYPE_NEW ) {
	            	builder.withValueBackReference(StructuredName.RAW_CONTACT_ID, 0);
		    } else {
		    	builder.withValue(StructuredName.RAW_CONTACT_ID, raw_contact_id);
		    }
	            builder.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
	            if(prefix != null) builder.withValue(StructuredName.PREFIX, prefix);
	    	    if(familyName != null) builder.withValue(StructuredName.FAMILY_NAME, familyName);
	    	    if(middleName != null) builder.withValue(StructuredName.MIDDLE_NAME, middleName);
	    	    if(givenName != null) builder.withValue(StructuredName.GIVEN_NAME, givenName);
	    	    if(suffix != null) builder.withValue(StructuredName.SUFFIX, suffix);	
	    	    /*
	    	     * ICS Porting: Lib do not support
	    	    if(phoneticFamilyName != null) builder.withValue(StructuredName.PHONETIC_FAMILY_NAME, phoneticFamilyName);
	    	    if(phoneticGivenName != null) builder.withValue(StructuredName.PHONETIC_GIVEN_NAME, phoneticGivenName);
	    	    if(phoneticMiddleName != null) builder.withValue(StructuredName.PHONETIC_MIDDLE_NAME, phoneticMiddleName);    	    
	    	    */
	    	    
	            operationList.add(builder.build());
	        }

		//NickName
		List<NicknameData> nickNameList = contactStruct.getNickNameList();
		    if (nickNameList != null && nickNameList.size() > 0) {
	            for (NicknameData nickName : nickNameList) {
	                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
	                builder.withValueBackReference(Nickname.RAW_CONTACT_ID, 0);
	                builder.withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE);
	                builder.withValue(Nickname.TYPE, Nickname.TYPE_DEFAULT);
	                builder.withValue(Nickname.NAME, nickName.getNickname());
	                operationList.add(builder.build());
	            }
	        }

	        List<PhoneData>  phoneList = contactStruct.getPhoneList();
	        if (phoneList != null) {
	            for (PhoneData phoneData : phoneList) {
	            	Log.d(TAG, phoneData.toString());
	                builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	                if(opType == OP_TYPE_NEW ) {
	     	            builder.withValueBackReference(Phone.RAW_CONTACT_ID, 0);
	     		} else {
	     		    	builder.withValue(Phone.RAW_CONTACT_ID, raw_contact_id);
	     		}
	                builder.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
	                builder.withValue(Phone.TYPE, phoneData.getType());
	                if (phoneData.getType() == BaseTypes.TYPE_CUSTOM) {
	                    builder.withValue(Phone.LABEL, phoneData.getLabel());
	                }
	                builder.withValue(Phone.NUMBER, phoneData.getNumber());
	                if (phoneData.isPrimary()) {
	                    builder.withValue(Data.IS_PRIMARY, 1);
	                }
	                operationList.add(builder.build());
	            }
	        }
	        
	        List<OrganizationData> organizationList = contactStruct.getOrganizationList();
	        if (organizationList != null) {
	            boolean first = true;
	            for (OrganizationData organizationData : organizationList) {
	                    builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	            	if(opType == OP_TYPE_NEW ) {
		            builder.withValueBackReference(Organization.RAW_CONTACT_ID, 0);
		    	} else {
		            builder.withValue(Organization.RAW_CONTACT_ID, raw_contact_id);
		    	}
	                builder.withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE);

	                // Currently, we do not use TYPE_CUSTOM.
	                builder.withValue(Organization.TYPE, organizationData.getType());
	                builder.withValue(Organization.COMPANY, organizationData.getOrganizationName());
	                builder.withValue(Organization.TITLE, organizationData.getTitle());
	                if (first) {
	                    builder.withValue(Data.IS_PRIMARY, 1);
	                }
	                operationList.add(builder.build());
	            }
	        }
	        
	        List<EmailData> emailList = contactStruct.getEmailList();
	        if (emailList != null) {
	            for (EmailData emailData : emailList) {
	            	builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
			if(opType == OP_TYPE_NEW ) {
  	            	    builder.withValueBackReference(Email.RAW_CONTACT_ID, 0);
  		    	} else {
  		    	    builder.withValue(Email.RAW_CONTACT_ID, raw_contact_id);
  		    	}
	                builder.withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
	                builder.withValue(Email.TYPE, emailData.getType());
	                if (emailData.getType() == BaseTypes.TYPE_CUSTOM) {
	                    builder.withValue(Email.LABEL, emailData.getLabel());
	                }
	                builder.withValue(Email.DATA, emailData.getAddress());
	                if (emailData.isPrimary()) {
	                    builder.withValue(Data.IS_PRIMARY, 1);
	                }
	                operationList.add(builder.build());
	            }
	        }
	        
	        List<PostalData> postalList = contactStruct.getPostalList();
	        if (postalList != null) {
	            for (PostalData postalData : postalList) {
	            	builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	                if(opType == OP_TYPE_NEW ) {
		     	    builder.withValueBackReference(StructuredPostal.RAW_CONTACT_ID, 0);
		     	} else {
		     	    builder.withValue(StructuredPostal.RAW_CONTACT_ID, raw_contact_id);
		     	}

        		builder.withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
        		builder.withValue(StructuredPostal.TYPE, postalData.getType());
        		if (postalData.getType() == BaseTypes.TYPE_CUSTOM) {
            		    builder.withValue(StructuredPostal.LABEL, postalData.getLabel());
        		}

        		builder.withValue(StructuredPostal.POBOX, postalData.getPobox());
        		// Extended address is dropped since there's no relevant entry in ContactsContract.
        		builder.withValue(StructuredPostal.STREET, postalData.getStreet());
        		builder.withValue(StructuredPostal.CITY, postalData.getLocalty());
        		builder.withValue(StructuredPostal.REGION, postalData.getRegion());
        		builder.withValue(StructuredPostal.POSTCODE, postalData.getPostalCode());
        		builder.withValue(StructuredPostal.COUNTRY, postalData.getCountry());

        		builder.withValue(StructuredPostal.FORMATTED_ADDRESS,
                	    postalData.getFormattedAddress(VCardConfig.VCARD_TYPE_V21_GENERIC));
        		if (postalData.isPrimary()) {
            		    builder.withValue(Data.IS_PRIMARY, 1);
        		}
	                operationList.add(builder.build());
	            }
	        }
	        

	        
	        List<NoteData> noteList = contactStruct.getNotes();
	        if (noteList != null) {
	            for (NoteData note : noteList) {
	            	builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	            	if(opType == OP_TYPE_NEW ) {
		     	    builder.withValueBackReference(Note.RAW_CONTACT_ID, 0);
		        } else {
		     	    builder.withValue(Note.RAW_CONTACT_ID, raw_contact_id);
		       }
	                builder.withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE);
	                builder.withValue(Note.NOTE, note.getNote());
	                operationList.add(builder.build());
	            }
	        }
	
       		List<PhotoData> photoList = contactStruct.getPhotoList();
        
	        
	        if (photoList != null) {
	            boolean first = true;
	            for (PhotoData photoData : photoList) {
	                builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	                builder.withValueBackReference(Photo.RAW_CONTACT_ID, 0);
	                builder.withValue(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
	                builder.withValue(Photo.PHOTO, photoData.getBytes());
	                if (first) {
	                    builder.withValue(Data.IS_PRIMARY, 1);
	                    first = false;
	                }
	                operationList.add(builder.build());
	            }
	        }
	        
	        
	        List<WebsiteData> websiteList = contactStruct.getWebsiteList();
	        if (websiteList != null) {
	            for (WebsiteData website : websiteList) {
	                builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	                if(opType == OP_TYPE_NEW ) {
		     	    builder.withValueBackReference(Website.RAW_CONTACT_ID, 0);
		        } else {
		     	    builder.withValue(Website.RAW_CONTACT_ID, raw_contact_id);
		     	    }
	                builder.withValue(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE);
	                builder.withValue(Website.URL, website.getWebsite());
	                operationList.add(builder.build());
	            }
	        }
	        
	        String birthday = contactStruct.getBirthday();
	        if (birthday!=null && birthday.length()>0) {
			builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
			if(opType == OP_TYPE_NEW ) {
 	            	    builder.withValueBackReference(Event.RAW_CONTACT_ID, 0);
 		    	} else {
 		    	    builder.withValue(Event.RAW_CONTACT_ID, raw_contact_id);
 		    	}
	            builder.withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE);
	            builder.withValue(Event.START_DATE, birthday);
		    builder.withValue(Event.TYPE, Event.TYPE_BIRTHDAY);
	            operationList.add(builder.build());
	        }
	        
	        }
	        
		List<ImData> mImList = contactStruct.getImList();
	        if (mImList != null) {
	            for (ImData imData : mImList) {
	                builder = ContentProviderOperation.newInsert(Data.CONTENT_URI);
	                builder.withValueBackReference(Im.RAW_CONTACT_ID, 0);
	                builder.withValue(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE);
	                builder.withValue(Im.TYPE, imData.getType());
	                builder.withValue(Im.PROTOCOL, imData.getProtocol());
	                builder.withValue(Im.DATA, imData.getAddress());
	                if (imData.getProtocol() == Im.PROTOCOL_CUSTOM) {
	                    builder.withValue(Im.CUSTOM_PROTOCOL, imData.getCustomProtocol());
	                }
	                if (imData.isPrimary()) {
	                    builder.withValue(Data.IS_PRIMARY, 1);
	                }
	                operationList.add(builder.build());
	            }
	        }

	        /*
	         * ICS Porting: Lib do not support
	        List<String> cates = contactStruct.getCategories();
	        if (cates!=null && cates.size()>0) {
	            if(mNeedCreateGroup){
	        	createGroups(resolver, cates);
	            }	        	
	            ArrayList<Integer> ids = getIdsbyNames(cates);	        	
	            for(Integer id : ids){
			builder = ContentProviderOperation.newInsert(addCallerIsSyncAdapterParameter(Data.CONTENT_URI));
	        	builder.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
	        	if(opType == OP_TYPE_NEW ) {
	        	    builder.withValueBackReference(GroupMembership.RAW_CONTACT_ID, 0);
	        	} else {
	        	    builder.withValue(GroupMembership.RAW_CONTACT_ID, raw_contact_id);	        	}
	        	
	        	builder.withValue(GroupMembership.GROUP_ROW_ID, id);
	        	operationList.add(builder.build());
	            }
	        }
	        */

	        try {
	        	ContentProviderResult[] result = 
	        		resolver.applyBatch(ContactsContract.AUTHORITY, operationList);

			for (int i = 0; i < result.length; i ++) {
	        		Log.d(TAG , " i = " + i );
	        		Log.d(TAG , " result = " + result[i].toString() );
	        	}

			if (opType == OP_TYPE_NEW) {
	        		return result[0].uri; //this is the uri in raw_contacts table
			} else {
				return uri; 
			}
	        } catch (RemoteException e) {
	            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
	            return null;
	        } catch (OperationApplicationException e) {
	            Log.e(TAG, String.format("%s: %s", e.toString(), e.getMessage()));
	            return null;
	        }
	    }

    public static int guessEncoding(byte[] bytes) {
	boolean maybeAsc = true;        
        boolean maybeUTF8 = true;

	// Does it start with the UTF-8 byte order mark? then guess it's UTF-8
        if (bytes.length > 3 && bytes[0] == (byte) 0xEF
            && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return ENCODING_MAY_UTF8;
        }
	
        int len = bytes.length;
        int i = 0;
        while (i < len -6 && (maybeUTF8 || maybeAsc)) {
        	byte c0 = bytes[i];
        	byte c1 = bytes[i+1];
        	byte c2 = bytes[i+2];
        	byte c3 = bytes[i+3];
        	byte c4 = bytes[i+4];
        	byte c5 = bytes[i+5];
        	
        	if(maybeAsc && c0 < 0) {
        		maybeAsc = false;
        	} 
        	
        	if(twoBytes(c0)){
        		if(!continueByte(c1)){
        			maybeUTF8 = false ;
        		} else {
        			i++ ;
        		}
        			
        	} else if(threeBytes(c0)){
        		if(!(continueByte(c1) && continueByte(c2)) ){
        			maybeUTF8 = false ;
        		} else {
        			i += 2 ;
        		}
        		
        	} else if(fourBytes(c0)){
        		
        		if(!(continueByte(c1) && continueByte(c2) && continueByte(c3)) ){
        			maybeUTF8 = false ;
        		} else {
        			i += 3 ;
        		}
        		
        	} else if(fiveBytes(c0)){
        		
        		if(!(continueByte(c1) && continueByte(c2) && continueByte(c3) 
        				&& continueByte(c4) )  ){
        			maybeUTF8 = false ;
        		} else {
        			i += 4 ;
        		}
        		
        	} else if(sixBytes(c0)){
        		
        		if(!(continueByte(c1) && continueByte(c2) && continueByte(c3) 
        				&& continueByte(c4) && continueByte(c5))  ){
        			maybeUTF8 = false ;
        		} else {
        			i += 5 ;
        		}
        	} else if(c0 < 0){
        		maybeUTF8 = false ; 
        	}        	
        	i++;
        }        
       
        int result = 0 ;
        if (maybeAsc) {
            result = ENCODING_MAY_ASCII;
        } else if (maybeUTF8) {
        	result = ENCODING_MAY_UTF8;
        } else {
        	result = ENCODING_MAY_GBK;
        }       

        Log.d("guessEncoding", "result = " + result);
        return result;
    }

	public String constructDisplayName(String familyName, String middleName, String givenName){
		boolean isChinese = false ;
		StringBuilder sb = new StringBuilder();
		
		if(familyName != null) {
			if(isChinese(familyName)){
				isChinese = true;
			}			
		}
		if(middleName != null) {
			if(isChinese(middleName)){
				isChinese = true;
			}			
		}
		if(givenName != null) {
			if(isChinese(givenName)){
				isChinese = true;
			}			
		}
		
		if (isChinese) {
			if(familyName != null && familyName.length() != 0) {
				sb.append(familyName);
			}
		    if (middleName != null && middleName.length() != 0) {
				sb.append(middleName);
			}
			if(givenName != null && givenName.length() != 0) {
				sb.append(givenName);
			}	
		} else {
			if(givenName != null && givenName.length() != 0) {
				sb.append(givenName);
			}	
			if (middleName != null && middleName.length() != 0) {
				sb.append(middleName);
			}
			if(familyName != null && familyName.length() != 0) {
				if(givenName != null && givenName.length() != 0) {
					sb.append(" ");
				}
				sb.append(familyName);
			}
		}
		return sb.toString();
	}

	private boolean isChinese(char c) {  
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);  
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS  
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS  
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A  
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION  
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION  
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {  
            return true;  
        }  
        return false;  
    }  
  
    private boolean isChinese(String strName) {  
        char[] ch = strName.toCharArray();
        boolean isChinese = false;
        for (int i = 0; i < ch.length; i++) { 
            char c = ch[i];  
            if (isChinese(c)) {  
                isChinese = true;
            }
        }
        return isChinese;
    }
	
	public int getContactIDbyRawID(int rawID){
	    String project[] = new String[]{RawContacts.CONTACT_ID};
	    Cursor cursor = mResolver.query(RawContacts.CONTENT_URI, project, "_id=" + rawID, null, null);
	    if (cursor == null) {
		return -1;
	    }
		
	    if (cursor.getCount() == 0 || !cursor.moveToFirst()) {
	 	try {
	            cursor.close();
	        } catch (SQLiteException e) {
	            Log.e(TAG, "SQLiteException on Cursor#close(): " + e.getMessage());
	        } 
	        return -1;
	    }
	    int contactId = cursor.getInt(cursor.getColumnIndexOrThrow(RawContacts.CONTACT_ID));
	    try {
                cursor.close();
            } catch (SQLiteException e) {
                Log.e(TAG, "SQLiteException on Cursor#close(): " + e.getMessage());
            }
	    return contactId;		 
        }
	
    private static boolean sixBytes(byte c){
    	return c <= -3 && -4 <= c; 
    }
    
    private static boolean fiveBytes(byte c){
    	return c <= -5 && -8 <= c; 
    }
    
    private static boolean fourBytes(byte c){
   	 return c <= -9 && -16 <= c;
    }
    
    private static boolean threeBytes(byte c){
    	  return c <= -17 && -32 <= c ;
    }
    
    private static boolean twoBytes(byte c){
    	return c <= -33 && -64 <= c; 
    } 
    
    private static boolean continueByte(byte c) {
    	return c <= -65 && -128 <= c ; 
    }

    private void getGroupsInternal(ContentResolver resolver ){
    	String project[] = new String[]{BaseColumns._ID, Groups.TITLE,};
    	String accName = mAccountName;
    	if (accName == null) {
	    accName = DEFAULT_ACCOUNT_NAME;
	}
    	String selection = Groups.ACCOUNT_NAME + "='" + accName +
        "' AND " + Groups.DELETED + "=0";
        mGroupIds = null;
        mGroupNames = null;
        mGroups = null;
        Cursor cursor = resolver.query(Groups.CONTENT_URI, project, selection, null, null);
    	if(cursor != null ){
    	    int group_count = cursor.getCount();
    	    if( group_count != 0 ) {
    	    	mGroupIds = new ArrayList<Integer>(group_count);
    	    	mGroupNames = new ArrayList<String>(group_count);
    	    	mGroups = new HashMap<Integer, String>(group_count);
    	    	while (cursor.moveToNext()){
    	    		int group_id = cursor.getInt(cursor.getColumnIndexOrThrow(BaseColumns._ID));
    	    		Integer ID = new Integer(group_id);
    	    		mGroupIds.add(ID);
    			
    	    		String group_name = cursor.getString(cursor.getColumnIndexOrThrow(Groups.TITLE));    			
    	    		mGroupNames.add(group_name);
    	    		mGroups.put(ID, group_name);
    	    	}  
    	    }
    		 cursor.close();
    	}
    }
    
    public ArrayList<String> getGroupNames(ContentResolver resolver){
    	getGroupsInternal(resolver);
    	return mGroupNames;
    }
    
    private int createGroup(ContentResolver resolver, String groupName ){
        final ContentValues newGroup = new ContentValues();
        newGroup.put(Groups.TITLE, groupName);
        newGroup.put(Groups.ACCOUNT_NAME, mAccountName);
        newGroup.put(Groups.ACCOUNT_TYPE, mAccountType);
        newGroup.put(Groups.GROUP_VISIBLE, "1");
        Uri uri = resolver.insert(Groups.CONTENT_URI, newGroup);
        String sid = uri.getPathSegments().get(1);
    	int id = Integer.valueOf(sid);
    	return id;   
    }
	
    public void createGroups(ContentResolver resolver, List<String> nameList){
	for(String name : nameList ) {		
	    String[] seperateValues = name.split(",");
	    if(seperateValues != null) {
		int num = seperateValues.length;
		if(mGroups==null){
    		    mGroupIds = new ArrayList<Integer>(num);
	    	    mGroupNames = new ArrayList<String>(num);
	    	    mGroups = new HashMap<Integer, String>(num);
    	        }
		for(int i = 0 ; i < num ; i ++){
		    if(seperateValues[i].length()>0 && !mGroupNames.contains(seperateValues[i])) {		
		    	int id = createGroup(resolver,seperateValues[i]);
		    	mGroupIds.add(Integer.valueOf(id));
		    	mGroupNames.add(seperateValues[i]);
		    	mGroups.put(Integer.valueOf(id), seperateValues[i]);
		    }
		}
	    }
	}	
    }
	
	
    public void setCreateGroup(boolean need){
	mNeedCreateGroup = need;
    }	
	
    private ArrayList<Integer> getIdsbyNames(List<String> nameList) {
	ArrayList<Integer> ids = new ArrayList<Integer>();
	for(String name : nameList){
		String[] seperateValues = name.split(",");
	    if(seperateValues != null) {
	    	int num = seperateValues.length;
	    	for(int i = 0 ; i < num ; i ++){
	    		int ind = mGroupNames.indexOf(seperateValues[i]);
	    		if(ind >=0){
	    			ids.add(mGroupIds.get(ind));
	    		}
	    	}
	    }
	}
	return ids;		
    }

 // Add A16516 IKQLTTCW-1729
    public String getFamilyName() {
        return mFamilyName;
    }

    public String getMiddleName() {
    	return mMiddleName;
    }
    
    public String getGivenName() {
        return mGivenName;
    }
    // End A16516 IKQLTTCW-1729

    private Uri addCallerIsSyncAdapterParameter(Uri uri) {
    	if(mIsSyncAdapter == true) {
            return uri.buildUpon().appendQueryParameter(
                ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
    	} else {
    	    return uri;
    	}
    }

    public void clearContactDirtyBit(int id){
    	Uri record_uri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, id);
    	ContentValues cv = new ContentValues();
    	cv.put(RawContacts.DIRTY, 0);
    	mResolver.update(addCallerIsSyncAdapterParameter(record_uri), cv, null, null);
    }
    
    public void setPhotoPath(String path){
        mPhotoPath = path;
    }
    
}

