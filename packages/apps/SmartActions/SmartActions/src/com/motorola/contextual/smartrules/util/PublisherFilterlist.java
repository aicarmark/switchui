package com.motorola.contextual.smartrules.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeMap;

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
import android.net.Uri;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;

public class PublisherFilterlist implements Constants{
	
	private interface Tags {
		String BLACKLIST = "blacklist";
		String WHITELIST = "whitelist";
		String PUBLISHER = "publisher";
		
		public interface Attrib {
			String NAME = "name";
		}
	}
	
	private static final String BLACKLIST_DEFAULT_XML_FILE_PATH = "/system/etc/smartactions/com.motorola.smartactions_blacklist.xml";
	private static final String WHITELIST_DEFAULT_XML_FILE_PATH = "/system/etc/smartactions/com.motorola.smartactions_whitelist.xml";

	private static PublisherFilterlist mInstPublisherFilterlist;
	private static final String TAG = PublisherFilterlist.class.getSimpleName();
	private FilterList blacklist = new FilterList();
	private FilterList whitelist = new FilterList();
	
	private static boolean loadedBlackList = false;
	private static boolean loadedWhiteList = false;
	private static boolean isBlackListFilePresent = false;
	private static boolean isWhiteListFilePresent = false;
	
	public static synchronized PublisherFilterlist getPublisherFilterlistInst(){
		if(mInstPublisherFilterlist == null){
			mInstPublisherFilterlist = new PublisherFilterlist();
		}
		return mInstPublisherFilterlist;
	}
	
	private class FilterList extends TreeMap<String, Boolean> {
		
		private static final long serialVersionUID = -3788700027188974847L;

		/** 
		 * 
		 * @param is
		 * @param fl - FilterList instance (instance of either BlackLisy or WhiteList )
		 */
		public void load(InputSource is, FilterList fl) {
			loadXml(is, fl);
		}

		/** load XML from an input source, either URI or filepath, see the methods which drive this method
		 * 
		 * @param is - input source
		 * @param fl - FilterList instance (instance of either BlackLisy or WhiteList )
		 */
		private synchronized void loadXml(InputSource is, FilterList fl) {
						
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser;
			try {
				parser = spf.newSAXParser();
				XMLReader reader = parser.getXMLReader();
				reader.setContentHandler(new BlacklistContentHandler(fl));
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
		private class BlacklistContentHandler implements ContentHandler {
			private FilterList instFilterList;
			private boolean insideFilterlist;
			
			public BlacklistContentHandler(FilterList fl){
				instFilterList = fl;
			}

			public void startElement(String uri, String localName,
							final String qName, Attributes attrs) throws SAXException {
				
				if (qName.toLowerCase().equals(Tags.BLACKLIST) ||
						qName.toLowerCase().equals(Tags.WHITELIST))
					insideFilterlist = true;
				else {
					if (qName.toLowerCase().equals(Tags.PUBLISHER)) {
						String name = null, value=null;
						for (int i=0; i<attrs.getLength(); i++) {
							name = attrs.getLocalName(i);
							if (name.toLowerCase().equals(Tags.Attrib.NAME) && insideFilterlist) {
								value = attrs.getValue(i);
								if (value != null) {
									instFilterList.put(value, Boolean.TRUE);
								}
							}
							if (LOG_DEBUG) Log.v(TAG, TAG+"("+i+"):"+name+"="+attrs.getValue(name) );
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
				if (qName.toLowerCase().equals(Tags.BLACKLIST) ||
						qName.toLowerCase().equals(Tags.WHITELIST))
					insideFilterlist = false;
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
	
	/** check to see if a publisher key is blacklisted.
	 * 
	 * @param context - context
	 * @param publisherKey - the publisher key to check for the blacklist
	 * @return true if on the blacklist
	 */
	public boolean isBlacklisted(Context context, final String publisherKey) {
		boolean ret = false;
		//checking dynamic for publishers that might not be supported
		//on products due to h/w factors, or conflicting requirements
		if(publisherKey !=null){
			if(publisherKey.equalsIgnoreCase(LOCATION_TRIGGER_PUB_KEY)){
				if(Util.hideLocationTrigger(context)){
					ret = true;
					return ret;
				}
			}
			
			if(publisherKey.equalsIgnoreCase(PROCESSOR_SPD_PUB_KEY)){
				if(	!Util.isProcessorSpeedSupported(context)){
					ret = true;
					return ret;
				}
			}
		}
		
		//checking from the statically loaded blacklist xml file
		if (!loadedBlackList) {
			if(LOG_DEBUG) Log.d(TAG,"Black list file not yet loaded, attempting to load");
			load(context, BLACKLIST_DEFAULT_XML_FILE_PATH, blacklist);  
			loadedBlackList = true;
		}
		if(isBlackListFilePresent){
			if(blacklist!=null && publisherKey!=null){
				ret = blacklist.containsKey(publisherKey);
			}
			if(LOG_DEBUG) Log.d(TAG,"Black list :"+publisherKey +" "+ret);
		}
		return ret;
	}
	
	/** check to see if a publisher is in a contained set of permitted packages
	 * 
	 * @param context - context
	 * @param package name - the publisher key to check for the white list
	 * @return true if on the white list
	 */
	public boolean isWhitelisted(Context context, final String pkgName) {
		boolean ret = true;
		if (!loadedWhiteList) {
			if(LOG_DEBUG) Log.d(TAG,"White list file not yet loaded, attempting to load");
			load(context, WHITELIST_DEFAULT_XML_FILE_PATH, whitelist);  
			loadedWhiteList = true;
		}
		if(isWhiteListFilePresent){
			ret = whitelist.containsKey(pkgName);
			if(LOG_DEBUG) Log.d(TAG,"White list :"+pkgName +" "+ret);
		}
		return ret;

	}

	
	/** load the XML into the blacklist 
	 * 
	 * @param context - context
	 * @param uri - the uri to load from
	 */
	public synchronized FilterList load(Context context, final Uri uri, FilterList toLoad) {

		if (uri == null) throw new IllegalArgumentException("uri cannot be null");
		InputStream is;
		try {
			is = context.getContentResolver().openInputStream(uri);
			toLoad.load(new InputSource(is), toLoad);
			if(is != null)
				is.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}			
		return toLoad;
	}
	
	
	/** load the XML into the blacklist 
	 * @param Context
	 * @param filepath - the file path to load the XML from. See File class for more information.
	 * @param toLoad the FilterList either blacklist OR whitelist
	 * IT MAY BE SET TO AN EMPTY STRING TO LOAD THE DEFAULT LOCATION blacklist file.
	 * @return FilterList instance to use for checking the Filterlist
	 */
	public synchronized FilterList load(final Context context, String filepath, FilterList toLoad) {
		
		if (filepath == null) throw new IllegalArgumentException("filepath cannot be null");
		
		if (filepath.equals("") && (toLoad==blacklist)){
			filepath = BLACKLIST_DEFAULT_XML_FILE_PATH;
		}else if (filepath.equals("") && (toLoad==whitelist)){
			filepath = WHITELIST_DEFAULT_XML_FILE_PATH;
		}
		
		File f = new File(filepath);		
		if (f != null && f.exists()) {
			InputStream is;
			try {
				is = new BufferedInputStream(new FileInputStream(f));
				toLoad.load(new InputSource(is), toLoad);
				is.close();
				
				if(toLoad == blacklist){
					isBlackListFilePresent = true;
				}else if (toLoad == whitelist){
					isWhiteListFilePresent = true;
				}
				
			} catch (FileNotFoundException e) {
				if (filepath.equals(BLACKLIST_DEFAULT_XML_FILE_PATH)) {
					Log.e(TAG, TAG+".load - no default publisher blacklist in expected location:"+
									BLACKLIST_DEFAULT_XML_FILE_PATH+" no packages blacklisted");
				} 
				else {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} else {
			Log.e(TAG, TAG+".load: f="+filepath+" exists="+(f==null?"null":f.exists()) );
		}
		return toLoad;		
	}
	
}
