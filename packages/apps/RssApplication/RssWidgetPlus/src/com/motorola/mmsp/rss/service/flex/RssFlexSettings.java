package com.motorola.mmsp.rss.service.flex;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.TypedValue;

import com.motorola.mmsp.rss.R;

public class RssFlexSettings implements RssFlexInterface {
	private static RssFlexSettings sInstance;
	private static final int DEFAULT_SHOW_ITEM_FOR = 0;
	private static final int DEFAULT_UPDATE_FREQUENCY = 1;
	private static final int MIN_SHOW_ITEM_FOR = 0;
	private static final int MAX_SHOW_ITEM_FOR = 4;
	private static final int MIN_UPDATE_FREQUENCY = 0;
	private static final int MAX_UPDATE_FREQUENCY = 5;
	private static final String TAG = "RssFlexSettings";
	private static final String TAG_SHOW_ITEM_FOR = "show-item-for";
	private static final String TAG_UPDATE_FREQUENCY = "update-frequency";
	private static final String TAG_BUNDLE_FEED_LIST = "motorssbundle-list";
	private static final String TAG_FEED_LIST_ITEM = "feed";
	private static final String TAG_FEED_NAME = "name";
	private static final String TAG_FEED_BASE = "base";
	private static final String FLEX_RSS_SETTING = "@MOTOFLEX@getRssSetting";
	private static final String FLEX_BUNDLE_FEED = "@MOTOFLEX@getRssBundleFeedInfo";
	private ArrayList<RssFlexFeedInfo> mFlexFeedList = new ArrayList<RssFlexFeedInfo>();

	public static RssFlexSettings getInstance() {
		if (sInstance == null) {
			sInstance = new RssFlexSettings();
		}
		return sInstance;
	}

	private String loadFlexXmlString(Context context, String flexName) {
		TypedValue typedValue = new TypedValue();
		Resources r = context.getResources();
		String xmlResult = "";
		try {
			r.getValue(flexName, typedValue, false);
			xmlResult = typedValue.coerceToString().toString();
			Log.d(TAG, "loadFlexXmlString() xmlResult:" + xmlResult);

		} catch (Exception e) {
			Log.d(TAG, "loadFlexXmlString() exception");
			e.printStackTrace();
			return null;
		}
		return xmlResult;
	}

	private int loadFlexInt(String xmlStr, String valueTag) {
		if (null == xmlStr || xmlStr.trim().equals("")) {
			return -1;
		}

		String unitStr = null;
		Element root = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;

		try {
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			Document xmldoc = db
					.parse(new InputSource(new StringReader(xmlStr)));
			root = xmldoc.getDocumentElement();

			NodeList noteList = root.getElementsByTagName(valueTag);
			Element currentNode = (Element) noteList.item(0);
			unitStr = currentNode.getFirstChild().getNodeValue();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Log.d(TAG, "loadFlexInt() unit:" + unitStr);
		if (unitStr == null || unitStr.trim().equals("")) {
			return -1;
		} else {
			return Integer.parseInt(unitStr);
		}
	}
	
	private static final void checkDocumentStart(XmlPullParser parser, String firstElementName) throws XmlPullParserException, IOException
    {
        int type;
        while ((type=parser.next()) != XmlPullParser.START_TAG
                   && type != XmlPullParser.END_DOCUMENT) {
            ;
        }
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }        
        if (!parser.getName().equals(firstElementName)) {
            throw new XmlPullParserException("Unexpected start tag: found " 
            		+ parser.getName() + ", expected " + firstElementName);
        }
    }

	private ArrayList<RssFlexFeedInfo> parseBundleList(String xmlStr) {
		ArrayList<RssFlexFeedInfo> listArray = new ArrayList<RssFlexFeedInfo>();
		if (null == xmlStr || xmlStr.trim().equals("")) {
			return null;
		}
		Element root = null;
		DocumentBuilderFactory dbf = null;
		DocumentBuilder db = null;
		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(new StringReader(xmlStr));
			checkDocumentStart(parser, TAG_BUNDLE_FEED_LIST);
			dbf = DocumentBuilderFactory.newInstance();
			db = dbf.newDocumentBuilder();
			Document xmldoc = db.parse(new InputSource(new StringReader(xmlStr)));
			root = xmldoc.getDocumentElement();
			NodeList nodeList = root
					.getElementsByTagName(TAG_FEED_LIST_ITEM);
			Log.i(TAG, "nodeList.length() = " + nodeList.getLength());
			for (int i = 0; i < nodeList.getLength(); i++) {
				String name = null;
				String url = null;
				Element element = (Element) nodeList.item(i);
				// get feed name
				NodeList nameList = element.getElementsByTagName(TAG_FEED_NAME);
				Element nameElement = (Element) nameList.item(0);
				name = nameElement.getTextContent();
				Log.i(TAG, "name = " + name);
				// get feed base url
				NodeList urlList = element.getElementsByTagName(TAG_FEED_BASE);
				Element urlElement = (Element) urlList.item(0);
				url = urlElement.getTextContent();
				Log.i(TAG, "url = " + url);
				RssFlexFeedInfo tmp = new RssFlexFeedInfo(name, url);
				listArray.add(tmp);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return listArray;
	}
	
	private ArrayList<RssFlexFeedInfo> getDefaultBundleList(Context context) {
		ArrayList<RssFlexFeedInfo> flexFeedList = new ArrayList<RssFlexFeedInfo>();
		String[] bundleFeeds = context.getResources().getStringArray(R.array.bundle_rss);
		Log.d(TAG, "bundleFeeds length is " + bundleFeeds.length);
		for(int i = 0; i < bundleFeeds.length; i+=2) {
			RssFlexFeedInfo feedInfo = new RssFlexFeedInfo();
			feedInfo.setFeedName(bundleFeeds[i]);
			feedInfo.setFeedUrl(bundleFeeds[i+1]);
			flexFeedList.add(feedInfo);
			feedInfo = null;
		}
		return flexFeedList;
	}

	@Override
	public ArrayList<RssFlexFeedInfo> getFlexFeedList(Context context) {
		mFlexFeedList.clear();
		String xmlStr = loadFlexXmlString(context, FLEX_BUNDLE_FEED);
		if(null == xmlStr || xmlStr.trim().equals("")) {
			mFlexFeedList = getDefaultBundleList(context);			
		} else {
			mFlexFeedList = parseBundleList(xmlStr);
			if(mFlexFeedList == null) {
				mFlexFeedList = getDefaultBundleList(context);
			}
			if(mFlexFeedList.isEmpty()) {
				mFlexFeedList = getDefaultBundleList(context);
			}
		}
		return mFlexFeedList;
	}

	@Override
	public int getNewsValidity(Context context) {
		int showItemFor = 0;
		String xmlStr = loadFlexXmlString(context, FLEX_RSS_SETTING);
		showItemFor = loadFlexInt(xmlStr, TAG_SHOW_ITEM_FOR);
		Log.d(TAG, "showItemFor is " + showItemFor);
		if (showItemFor < MIN_SHOW_ITEM_FOR || showItemFor > MAX_SHOW_ITEM_FOR) {
			return DEFAULT_SHOW_ITEM_FOR;
		} else {
			return showItemFor;
		}
	}

	@Override
	public int getUpdateFrequency(Context context) {
		int frequency = 0;
		String xmlStr = loadFlexXmlString(context, FLEX_RSS_SETTING);
		frequency = loadFlexInt(xmlStr, TAG_UPDATE_FREQUENCY);
		Log.d(TAG, "frequency is " + frequency);
		if (frequency < MIN_UPDATE_FREQUENCY || frequency > MAX_UPDATE_FREQUENCY) {
			return DEFAULT_UPDATE_FREQUENCY;
		} else {
			return frequency;
		}
	}

}
