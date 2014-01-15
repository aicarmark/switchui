package com.motorola.contextual.smartrules.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

public class ConflictingActionTriggerFilterList implements Constants{
	
	private interface Tags {
		String ACTION_FILTER_LIST = "actionfilterlist";
		String TRIGGER_FILTER_LIST = "triggerfilterlist";
		String TRIGGER_FILTER_TAG = "filter_for_trigger";
		String ACTION_FILTER_TAG = "filter_for_action";
		
		public interface Attrib {
			String TRIGGER_NAME = "trigger";
			String ACTION_NAME = "action";
			String VALUE = "value";
			String NAME = "name";
		}
	}
	
	private static final String TRIGGER_DEFAULT_XML_FILE_PATH = "com.motorola.smartactions_triggerfilterlist.xml";
	private static final String ACTION_DEFAULT_XML_FILE_PATH = "com.motorola.smartactions_actionfilterlist.xml";

	private static ConflictingActionTriggerFilterList mInstConflictingActionTriggerFilterList;
	private static final String TAG = PublisherFilterlist.class.getSimpleName();
	private ConflictFilterList trigger_filter_list = new ConflictFilterList();
	private ConflictFilterList action_filter_list = new ConflictFilterList();
	
	private static boolean loadedTriggerList = false;
	private static boolean loadedActionList = false;
	private static boolean isTriggerListFilePresent = false;
	private static boolean isActionListFilePresent = false;
	
	public static synchronized ConflictingActionTriggerFilterList getConflictingActionTriggerFilterListInst(){
		if(mInstConflictingActionTriggerFilterList == null){
			mInstConflictingActionTriggerFilterList = new ConflictingActionTriggerFilterList();
		}
		return mInstConflictingActionTriggerFilterList;
	}
	
	private class ConflictFilterList extends HashMap<String, Vector<String> > {
		
		private static final long serialVersionUID = -3788700027188974847L;

		/** 
		 * 
		 * @param is
		 * @param fl - FilterList instance (instance of either BlackLisy or WhiteList )
		 */
		public void load(InputSource is, ConflictFilterList fl) {
			loadXml(is, fl);
		}

		/** load XML from an input source, either URI or filepath, see the methods which drive this method
		 * 
		 * @param is - input source
		 * @param fl - FilterList instance (instance of either BlackLisy or WhiteList )
		 */
		private synchronized void loadXml(InputSource is, ConflictFilterList fl) {
						
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser;
			try {
				parser = spf.newSAXParser();
				XMLReader reader = parser.getXMLReader();
				reader.setContentHandler(new ConflictFilterListContentHandler(fl));
				reader.parse(is);
				
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		/** handle parsing out the XML file 
		 */
		private class ConflictFilterListContentHandler implements ContentHandler {
			private ConflictFilterList instFilterList;
			private boolean insideFilterlist = false, insideFilter = false;

			Vector<String> val = new Vector<String>();
			public String key_name;
			public ConflictFilterListContentHandler(ConflictFilterList fl){
				instFilterList = fl;
			}

			public void startElement(String uri, String localName,
							final String qName, Attributes attrs) throws SAXException {
				
				if (qName.toLowerCase().equals(Tags.ACTION_FILTER_LIST) ||
						qName.toLowerCase().equals(Tags.TRIGGER_FILTER_LIST))
					insideFilterlist = true;
				//else {
				if (qName.toLowerCase().equals(Tags.TRIGGER_FILTER_TAG) ||
						qName.toLowerCase().equals(Tags.ACTION_FILTER_TAG))
					insideFilter = true;
						
				if (insideFilterlist && insideFilter && 
						(qName.toLowerCase().equals(Tags.Attrib.TRIGGER_NAME) ||
								qName.toLowerCase().equals(Tags.Attrib.ACTION_NAME))){
						String name = null, value=null;
						for (int i=0; i<attrs.getLength(); i++) {
							name = attrs.getLocalName(i);
							if(name.toLowerCase().equals(Tags.Attrib.NAME)){
								
								value = attrs.getValue(i);
								if (value != null) {
									key_name = value;
								}
								if (LOG_DEBUG) Log.v(TAG, TAG+"("+i+"):"+"name = "+attrs.getValue(name) );
								
							}else if(name.toLowerCase().equals(Tags.Attrib.VALUE)){
								
								value = attrs.getValue(i);
								if (value != null) {
									val.add(value);
								}
								if (LOG_DEBUG) Log.v(TAG, TAG+"("+i+"):"+"value = "+attrs.getValue(name) );
								
							}
							
						}
				}

				if (LOG_DEBUG) Log.v(TAG, TAG+".startElement: insideFilterlist:"+insideFilterlist+" locName="+localName+
						" qName="+qName+" attrs:"+attrs.toString());
			}
			
			public void characters(char[] ch, int start, int length) throws SAXException {
			}

			public void endDocument() throws SAXException {
			}

			public void endElement(String uri, String localName, String qName) throws SAXException {
				if (qName.toLowerCase().equals(Tags.ACTION_FILTER_LIST) ||
						qName.toLowerCase().equals(Tags.TRIGGER_FILTER_LIST))
						insideFilterlist = false;
				if (insideFilterlist && 
						(	qName.toLowerCase().equals(Tags.TRIGGER_FILTER_TAG) ||
							qName.toLowerCase().equals(Tags.ACTION_FILTER_TAG))	){
						insideFilter = false;
						Vector<String> value = (Vector<String>) val.clone();	
						// without making clone list being class variable cannot be made persistent for 
						//each entry in HashMap in ConflictFilterListContentHandler of which instFilterList is instance of
						instFilterList.put(key_name, value);
						val.clear();
				}
				if (LOG_DEBUG) Log.v(TAG, TAG+".endElement:"+qName+" insideFilterlist="+insideFilterlist);
			}

			public void endPrefixMapping(String prefix) throws SAXException {
			}

			public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
			}

			public void processingInstruction(String target, String data) throws SAXException {
			}

			public void setDocumentLocator(Locator locator) {
			}

			public void skippedEntity(String name) throws SAXException {
			}

			public void startDocument() throws SAXException {
			}

			public void startPrefixMapping(String prefix, String uri) throws SAXException {				
			} 
			
		}
	}
	
	/**
	 * @param context - context passed from list activity
	 * @return Vector<String> as list String containing Actions which have triggers to be grayed
	 */
	public Vector<String> getListOfActionsToCheckForCorrespondingTriggerFilters(Context context){
		 Vector<String> actionList = new Vector<String>();
		 if (!loadedTriggerList) {
				if(LOG_DEBUG) Log.d(TAG,"Black list file not yet loaded, attempting to load");
				load(context, TRIGGER_DEFAULT_XML_FILE_PATH, trigger_filter_list);  
				loadedTriggerList = true;
			}
		if(isTriggerListFilePresent){
			if( !trigger_filter_list.isEmpty()){
				Set<String> action_key = trigger_filter_list.keySet();
				Iterator<String> list_iterator = action_key.iterator();
				while(list_iterator.hasNext()){
					actionList.add(list_iterator.next());
				}					
			}
		 }
		 return actionList;
	}
	
	/**
	 * @param context - context passed from list activity
	 * @return Vector<String> as list String containing Actions which have triggers to be grayed
	 */
	public Vector<String> getListOfTriggersToCheckForCorrespondingActionsFilters(Context context){
		 Vector<String> triggerList = new Vector<String>() ;
		 if (!loadedActionList) {
				if(LOG_DEBUG) Log.d(TAG,"Black list file not yet loaded, attempting to load");
				load(context, ACTION_DEFAULT_XML_FILE_PATH, action_filter_list);  
				loadedActionList = true;
		 }
		 if(isActionListFilePresent){
				if( !action_filter_list.isEmpty()){
					Set<String> trigger_key = action_filter_list.keySet();
					Iterator<String> list_iterator = trigger_key.iterator();
					while(list_iterator.hasNext()){
						triggerList.add(list_iterator.next());
					}
					
				}
		 }
		 return triggerList;
	}
	
	/** check to see if a trigger has conflict with an already added action
	 * 
	 * @param context - context
	 * @param publisherKey - the publisher key to check for the blacklist
	 * @return true if on the blacklist
	 */
	public Vector<String> isTriggerToBeFiltered(Context context, final String compareAgainst) {
		Vector<String> ret = null;
		if (!loadedTriggerList) {
			if(LOG_DEBUG) Log.d(TAG,"Black list file not yet loaded, attempting to load");
			load(context, TRIGGER_DEFAULT_XML_FILE_PATH, trigger_filter_list);  
			loadedTriggerList = true;
		}
		if(isTriggerListFilePresent){
			if( trigger_filter_list.containsKey(compareAgainst)){
				if(LOG_DEBUG) Log.d(TAG,"isActionToBeFiltered: Comparing against :"+compareAgainst);
				ret = trigger_filter_list.get(compareAgainst);
			}
		}
		return ret;
	}
	
	/** check to see if action has conflict with an already added trigger
	 * 
	 * @param context - context
	 * @param package name - the publisher key to check for the white list
	 * @return true if on the white list
	 */
	public Vector<String> isActionToBeFiltered(Context context, final String compareAgainst) {
		Vector<String> ret = null;
		if (!loadedActionList) {
			if(LOG_DEBUG) Log.d(TAG,"White list file not yet loaded, attempting to load");
			load(context, ACTION_DEFAULT_XML_FILE_PATH, action_filter_list);  
			loadedActionList = true;
		}
		if(isActionListFilePresent){
			if( action_filter_list.containsKey(compareAgainst)){
				if(LOG_DEBUG) Log.d(TAG,"isActionToBeFiltered: Comparing against :"+compareAgainst);
				ret = action_filter_list.get(compareAgainst);
			}			
		}
		return ret;

	}

	
	/** load the XML into the conflict list instance 
	 * 
	 * @param context - context
	 * @param uri - the uri to load from
	 */
	public synchronized ConflictFilterList load(Context context, final Uri uri, ConflictFilterList toLoad) {

		if (uri == null) throw new IllegalArgumentException("uri cannot be null");
		InputStream is;
		try {
			is = context.getContentResolver().openInputStream(uri);
			toLoad.load(new InputSource(is), toLoad);
			if(is!=null)
				is.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}			
		return toLoad;
	}
	
	
	/** load the XML into the conflict lists 
	 * @param Context
	 * @param filepath_suffix - the file path to load the XML from. See File class for more information.
	 * @param toLoad the FilterList either blacklist OR whitelist
	 * IT MAY BE SET TO AN EMPTY STRING TO LOAD THE DEFAULT LOCATION blacklist file.
	 * @return FilterList instance to use for checking the Filterlist
	 */
	public synchronized ConflictFilterList load(final Context context, String filepath_suffix, ConflictFilterList toLoad) {
		
		if (filepath_suffix == null) throw new IllegalArgumentException("filepath cannot be null");
		String filepath_prefix = "/system/etc/smartactions/";
		StringBuffer filepath = new StringBuffer(filepath_prefix);
		filepath.append(filepath_suffix);
		
		File f = new File(filepath.toString());
		InputStream is;
		
		try {
			//check if file is present for product specific override to greylist
			if (f != null && f.exists()) {
				is = new BufferedInputStream(new FileInputStream(f));
			} else {	// if no product specific file present, load default one from assets
				if(LOG_DEBUG) Log.d(TAG, TAG+".load: f="+filepath_suffix+" exists="+(f==null?"null":f.exists()) );
				AssetManager assetManager = context.getAssets();
				is = assetManager.open(filepath_suffix);
			}
			
			toLoad.load(new InputSource(is), toLoad);
			is.close();
			
			//mark the status of each file as loaded
			if(toLoad == trigger_filter_list){
				isTriggerListFilePresent = true;
			}else if (toLoad == action_filter_list){
				isActionListFilePresent = true;
			}
				
		} catch (FileNotFoundException e) {
			if (filepath_suffix.equals(TRIGGER_DEFAULT_XML_FILE_PATH)) {
				Log.e(TAG, TAG+".load - no default trigger greylist file in expected location:"+
						TRIGGER_DEFAULT_XML_FILE_PATH+" no trigger publisher grayed");
			}else if (filepath_suffix.equals(ACTION_DEFAULT_XML_FILE_PATH)) {
				Log.e(TAG, TAG+".load - no default action greylist in expected location:"+
						ACTION_DEFAULT_XML_FILE_PATH+" no action publisher grayed");
			}  
			else {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		return toLoad;		
	}

}
