/*
 * @(#)Utils.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21034      2011/05/11    IKMAIN-16865     Initial version
 *
 */
package com.motorola.contextual.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.motorola.contextual.commonutils.StringUtils;
import com.motorola.contextual.commonutils.chips.AddressUtil;
import com.motorola.contextual.debug.DebugTable;

import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.AssetManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.MultiAutoCompleteTextView;
/**
 * This class implements common utility functions  <code><pre>
 *
 * CLASS:
 *
 * RESPONSIBILITIES:
 *     This class implements common functions that can be of use by multiple
 *          action modules
 *
 * COLLABORATORS:
 *     Rules Builder
 *
 * USAGE:
 *     See each method.
 *
 * </pre></code>
 */

public class Utils implements Constants {

    private static final String TAG = Constants.TAG_PREFIX + Utils.class.getSimpleName();
    private static final int MAXIMUM_DIGITS = 10;

    private static final String SEND_SMS_ACTION = "android.intent.action.SEND";
    private static final String EXTRA_RECIPIENT_ID = "com.motorola.blur.util.messaging.QuickMessageUtil.extra.RECIPIENT_ID";
    private static final String SEND_SMS_CATEGORY = "com.motorola.blur.util.messaging.QuickMessageUtil.SMS";
    private static final String SEND_SMS_MIME_TYPE = "text/plain";
    private static final int BUFSIZE = 8192;
    private static final int INVALID_KEY = -1;

    /**
     * Listener to ignore 'Search' key events
     */
    public static final OnKeyListener sDisableSearchKey = new OnKeyListener() {

        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_SEARCH)
                return true;
            return false;
        }
    };

    /**
     * Returns true if the file identified by the uri parameter is present
     * on the file system
     * @param context
     * @param uri
     * @returns true if file exists in file system, else otherwise
     */
    public static boolean isMediaPresent(Context context, Uri uri) {
    	boolean isPresent = false;

    	ContentResolver contentResolver = context.getContentResolver();
    	Cursor cursor = null;
    	try {
	    String[] projection = new String[] { MediaStore.Audio.Media._ID,
                                                 MediaStore.Audio.Media.DATA};
            cursor = contentResolver.query(uri, projection, null, null, null);
	    if (cursor != null && cursor.moveToFirst()) {
                // File name with complete path is present in _data column
	    	String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
	    	File file = new File(path);
	    	isPresent = file.exists();
	    }
    	} catch (Exception e) {
    	    Log.e(TAG, "Exception in isMediaPresent()");
    	} finally {
	    if (cursor != null) {
	        cursor.close();
	    }
    	}

    	if(Constants.LOG_INFO)
    		Log.i(TAG, " Media Presence : " + isPresent);

    	return isPresent;
    }

    /**
     * Returns true if the playlist identified by the uri parameter is present
     * on the file system
     * @param context
     * @param uri
     * @returns true if playlist exists in file system, else otherwise
     */
    public static boolean isPlaylistPresent(Context context, Uri uri, String id) {
        boolean isPresent = false;

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = null;
        try {
            String[] projection = new String[] { MediaStore.Audio.Playlists._ID };
            cursor = contentResolver.query(uri, projection, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                // Compare playlist IDs with the requested ID
                cursor.moveToFirst();
                String cId = "";
                do {
                	cId = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                	if (LOG_DEBUG) Log.d(TAG, "Checking:"+cId);
                    if (cId.equals(id)) {
                        isPresent = true;
                        break;
                    }
                } while (cursor.moveToNext());
            } else {
            	Log.e(TAG, "Failed to find playlist, cursor.getCount="+cursor.getCount()+" id=>"+id+"<");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in isMediaPresent()");
        } finally {
	        if (cursor != null) {
	            cursor.close();
	        }
        }

        if (Constants.LOG_INFO) Log.i(TAG, " Playlist Presence:" + isPresent+" id="+id);
        return isPresent;
    }

    /**
     * This function writes relevant debug information into DebugTable.
     *
     * @param context
     * @param ruleKey
     * @param direction
     * @param toFrom
     * @param state
     * @param debugReqResp
     * @param data
     * @param actionKey
     */
    public static void writeToDebugViewer(Context context, String ruleKey, String direction, String toFrom,
            String state, String debugReqResp, String data, String actionKey) {
    	
    	DebugTable.writeToDebugViewer(context, direction, state, 
				null, ruleKey, toFrom, data, null, null, 
				Constants.PACKAGE, actionKey, debugReqResp,
				direction.equals(DebugTable.Direction.IN) ? "requested" : "completed");
    }

    /**
     * Generate Request/Response Key for Writing to Debug DB
     *
     * @return
     */
    public static String generateReqRespKey() {
        return Long.toString(new Date().getTime());
    }
    
    /**
     * Method to generate a unique internal name for the customized contacts
     *
     * @return Unique internal name for numbers present in an Auto SMS/VIP Caller rule
     */
    public static String getUniqId() {
        return (Long.toString(new Date().getTime())).substring(7);
    }

    /**
     * Utility method to format number to contain maximum ten digits
     * This is done to remove the prefix at the beginning for maintaining consistency
     * @param number Number to be formatted
     * @return Formatted number
     */
    public static String formatNumber (String number) {
        number = PhoneNumberUtils.extractNetworkPortion(number);
        if(number != null && number.length() > MAXIMUM_DIGITS) {
            number = number.substring(number.length()-MAXIMUM_DIGITS);
        }
        return number;
    }

    /**
     * Utility method to find out whether a number is known or not.
     * A known number is present in the Contacts database
     *
     * @param number Number to be checked
     * @return True if the number is known, false otherwise
     */
    public static boolean isKnownContact (Context context, String number) {
        if (number != null) {
            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, number);
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, new String[] {PhoneLookup.NUMBER}, null, null, null);

                if (cursor != null && cursor.getCount() > 0) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return false;
    }

    /**
     * Method to send SMS using blur so that the Message is added to Sent items
     *
     * @param context Application context
     * @param message Message to be sent
     * @param recepientNumber Number of the recipient
     */
    public static void sendSms (Context context, String message, String recipientNumber) {
        Intent i = new Intent(SEND_SMS_ACTION);
        i.putExtra(EXTRA_RECIPIENT_ID, recipientNumber);
        i.putExtra(Intent.EXTRA_TEXT, message);
        i.addCategory(SEND_SMS_CATEGORY);
        i.setType(SEND_SMS_MIME_TYPE);
        context.getApplicationContext().sendBroadcast(i);
    }

    /**
     * AsyncTask to populate the addressing widget.
     * It takes names and numbers to be populated
     * and synchronizes them with Contacts before populating.
     *
     */
    public static class PopulateWidget extends AsyncTask<Void, Void, Void> {
        private Context mContext;
        private String mNames;
        private String mNumbers;
        private String mKnownFlag;
        private MultiAutoCompleteTextView mToView;

        public PopulateWidget(Context context, String numbers, String names,
                String knownFlag, MultiAutoCompleteTextView toView) {
            mContext = context;
            mNumbers = numbers;
            mNames = names;
            mKnownFlag = knownFlag;
            mToView = toView;
            if (LOG_INFO) Log.i(TAG, "Initial names-" + mNames + ", numbers-" + mNumbers);
        }

        @Override
        protected Void doInBackground(Void... params) {
            NameNumberInfo syncedContacts = getSyncedContactAttributes(mContext, mNumbers, mNames,
                    mKnownFlag, StringUtils.COMMA_STRING);
            if (syncedContacts != null) {
                mNumbers = syncedContacts.getNumbers();
                mNames = syncedContacts.getNames();
            }
            if (LOG_INFO) Log.i(TAG, "Synced names-" + mNames + ", numbers-" + mNumbers);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if(!StringUtils.isEmpty(mNumbers) && !StringUtils.isEmpty(mNames)) {
                List<String> addressList = AddressUtil.getFormattedAddresses(mNames, mNumbers, StringUtils.COMMA_STRING);
                if (addressList != null && !addressList.isEmpty() && mToView != null) {
                    for (String address : addressList) {
                        mToView.append(address + StringUtils.COMMA_STRING);
                    }
                }
            }
        }
    }

    /**
     * Method to get contact attributes synced with Contacts DB. The deleted contacts are removed. Edited contacts are updated.
     * This method should not be called from the UI thread as it queries Contacts DB.
     *
     * @param context Application context
     * @param numbers List of numbers that need to be synced
     * @param names List of names that need to be synced
     * @param knownFlags List of known flags. This list indicates whether a contact was present in the phonebook or not
     * at the time of creation of the rule
     * @param delimiter Delimiter that separates individual items present in numbers, names and knownFlags strings
     * @return Synced attributes object
     */
    public static NameNumberInfo getSyncedContactAttributes (Context context, String numbers,
            String names, String knownFlags, String delimiter) {
        StringBuilder numberBuilder = new StringBuilder();
        StringBuilder nameBuilder = new StringBuilder();
        String[] numbersArr = numbers.split(delimiter);
        String[] namesArr = names.split(delimiter);
        String[] knownFlagsArr = null;

        if(knownFlags != null) {
            knownFlagsArr = knownFlags.split(delimiter);

	} else {
	    knownFlagsArr = new String[numbersArr.length];
	    for(int i=0; i<numbersArr.length; i++) {
	        knownFlagsArr[i] = "0";
	    }
	}

        NameNumberInfo syncedAttributes = null;

        if (numbersArr.length == namesArr.length && numbersArr.length == knownFlagsArr.length) {
            for (int i=0; i<numbersArr.length; i++) {
                String number = numbersArr[i].trim();
                String name = namesArr[i].trim();

                int knownFlag = 0;
                try {
                    knownFlag = Integer.parseInt(knownFlagsArr[i].trim());
                } catch (NumberFormatException e) {
                    Log.w(TAG, "NumberFormatException while parsing known flag. Setting flag to 0");
                }

                if (isKnownContact(context, number)) {
                    //Known contacts to be displayed
                    numberBuilder.append(number).append(delimiter);
                    nameBuilder.append(AddressUtil.getContactDisplayName(number, context)).append(delimiter);
                } else {
                    //Number not found in the Contacts database. There are two possibilities
                    // 1. Contact is unknown and it was stored as a raw number. Check known flag.
                    // 2. Contact is deleted. No need to display the number in this case
                    // 3. Contact is edited. Display edited contact.
                    if (knownFlag == 0) {
                        numberBuilder.append(number).append(delimiter);
                        nameBuilder.append(name).append(delimiter);
                    } else {
                        String [] projection = new String[] {Contacts._ID, Contacts.HAS_PHONE_NUMBER};
                        String whereClause = Contacts.DISPLAY_NAME + EQUALS + QUOTE + name + QUOTE;
                        Cursor cursor = null;
                        Cursor pCur = null;
                        try {
                            cursor = context.getContentResolver().query(Contacts.CONTENT_URI,
                                                                projection, whereClause, null, null);

                            if (cursor != null) {
                                if (cursor.moveToFirst() && Integer.parseInt(
                                            cursor.getString(cursor.getColumnIndex(Contacts.HAS_PHONE_NUMBER))) > 0) {
                                    //Contact with saved name is found. So, the number was edited by the user.
                                    pCur = context.getContentResolver().query(
                                               Phone.CONTENT_URI, new String[] {Phone.NUMBER},
                                               Phone.CONTACT_ID + EQUALS + QUOTE +
                                               cursor.getString(cursor.getColumnIndex(Contacts._ID)) + QUOTE,
                                               null, null);

                                    if (pCur != null) {
                                        if (pCur.moveToFirst()) {
                                            //Edited number for the Contact found.
                                            numberBuilder.append(PhoneNumberUtils.extractNetworkPortion(pCur.getString(pCur.getColumnIndex(Phone.NUMBER)))).append(delimiter);
                                            nameBuilder.append(name).append(delimiter);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error while populating numbers to addressing widget");
                        } finally {
                            if (cursor != null)
                                cursor.close();
                            if (pCur != null)
                                pCur.close();
                        }
                    }
                }
            }
            //Delete trailing delimiter
            if (numberBuilder.length() >= delimiter.length())
                numberBuilder.delete(numberBuilder.length()-delimiter.length(), numberBuilder.length());
            if (nameBuilder.length() >= delimiter.length())
                nameBuilder.delete(nameBuilder.length()-delimiter.length(), nameBuilder.length());

            syncedAttributes = new NameNumberInfo(nameBuilder.toString(), numberBuilder.toString());
        }
        return syncedAttributes;
    }

    /**
     * An object representing synchronized names and numbers
     */
    public static class NameNumberInfo {

        private String mNames;
        private String mNumbers;

        public NameNumberInfo (String name, String number) {
            mNames = name;;
            mNumbers = number;
        }

        public String getNames() {
            return mNames;
        }

        public String getNumbers() {
            return mNumbers;
        }
    }

    
    /**  Reads the xml located in the asset directory
     *  and then convert it into  a string
     *  @param  Context - application context
     *  @return String - String
     *
     */
    public  static String readFilefromAssets(Context context, String fileName) {

        if (LOG_DEBUG) Log.d(TAG,"Fn: readFilefromAssets");

        StringBuilder result = new StringBuilder();

        // Read the file
        InputStream in = null;
        AssetManager assetManager = context.getAssets();

        try {
            in = assetManager.open(fileName);

            if (in != null) {
                byte[] buffer = new byte[BUFSIZE];
                int read = 0;
                while((read = in.read(buffer)) != INVALID_KEY) {
                    result.append(new String(buffer, 0, read));
                }
            } else {
                Log.e(TAG,"Opening Asset Manager Failed");
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG,"FileNotFoundException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG,"IOException");
            e.printStackTrace();
        } finally {
            if (in!=null)
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG,"IOException");
                    e.printStackTrace();
                }
        }

        if (LOG_DEBUG) Log.d(TAG, TAG+" readFilefromAssets - returning - "+result.toString());

        return result.toString();
    }
    
    /**
     * Get all the relevant child nodes from the XML
     * @param ruleXml - contents in XML
     * @return - all nodes
     */
    public static NodeList getAllRelevantNodesFromXml(final String ruleXml, String tag) {
        if (LOG_DEBUG) Log.d(TAG, "Entered getAllRelevantNodesFromXml for :" + ruleXml);
        Document doc = getParsedDoc(ruleXml);

        if(doc!= null) {
            NodeList nodes = getAllNodeNamesFromXml(doc, tag);
            return nodes;
        } else {
            Log.e(TAG,"doc is null");
            return null;
        }
    }
    
    /** Get the XML doc for an XML
    *
    * @param ruleXml - contents in XML
    * @return - xml Document
    */
   public static Document getParsedDoc(final String ruleXml) {

       if(ruleXml == null) return null;

       DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
       DocumentBuilder db = null;
       try {
           db = dbf.newDocumentBuilder();
       } catch (ParserConfigurationException e) {
           e.printStackTrace();
           return null;
       }
       Document doc = null;
       StringReader xmlReader = null;
       try {
           xmlReader = new StringReader( ruleXml );
           doc = db.parse(new InputSource(xmlReader));
       } catch (SAXException e) {
           Log.e(TAG, "XML Syntax error");
           e.printStackTrace();
       } catch (IOException e) {
           Log.e(TAG, "IO Exception");
           e.printStackTrace();
       } finally {
           if (xmlReader != null) xmlReader.close();
       }
       return doc;
   }
  
      /**Get all the Child Nodes within <RULES> node
      *
      * @param doc - XML document
      * @return - get all nodes under tag RULES
      */
     public static NodeList getAllNodeNamesFromXml(Document doc, String tag) {
         NodeList nodes = doc.getElementsByTagName(tag).item(0).getChildNodes();
    
         if (LOG_DEBUG){
             Element intElement = (Element) doc.getElementsByTagName(tag).item(0);
             String value = intElement.getChildNodes().item(0).getNodeValue();
    
             if (LOG_DEBUG) Log.d(TAG,"getAllNodeNamesFromXml : " + value);
         }
    
         return nodes;
     }
     /** Get the XML Tree inside a node
     *
     * @param node - Node containing the tree
     * @return - Entire tree under node as a String
     */
    public static String getXmlTreeIn(final Node node) {
    
        if (LOG_DEBUG) Log.d(TAG,"Fn: getXmlTreeIn");
        String result = "";
    
        if(node == null) return result;
    
        try {
            TransformerFactory tranFact = TransformerFactory.newInstance();
            Transformer transfor = tranFact.newTransformer();
            transfor.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            Source src = new DOMSource(node);
            StringWriter buffer = new StringWriter();
            Result dest = new StreamResult(buffer);
            transfor.transform(src, dest);
            result = buffer.toString();
        } catch (Exception e) {
            Log.e(TAG, "Unable to get XML tree inside " +
                  node.getNodeName() + " " + e.getMessage());
        }
        return result;
    }
}
