package com.motorola.mmsp.rss.service.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.motorola.mmsp.rss.R;
import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.ItemInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.util.NetworkUtil;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class RssXmlParser {
	private String TAG = "RssXmlParser";
	private List<XmlParserInterface> mXMLParsers = null;
	private static RssXmlParser sRSSXmlParser = null;
	private Context mContext;
	private RssXmlParser(Context context){
		mContext = context;
		if(mXMLParsers == null){
			mXMLParsers = new ArrayList<XmlParserInterface>();
			registerParser(new RssXmlPullParser());
		}
	}
	
	public static RssXmlParser getInstance(Context context){
		if(sRSSXmlParser == null){
			sRSSXmlParser = new RssXmlParser(context);
		}
		return sRSSXmlParser;
	}
	
	
	public List<XmlParserInterface> getXMLParsers(){
		return mXMLParsers;
	}
	
	private void registerParser(XmlParserInterface parser){
		mXMLParsers.add(parser);
	}
	public int preParseRss(String checkedUrl){
		return tryPreParseRss(checkedUrl);
	}
	private int tryPreParseRss(String checkedUrl){
		int state = 0;
		int parserCount = 0;
		if(mXMLParsers != null){
			parserCount = mXMLParsers.size();
			for(int i=0;i<parserCount;i++){
				XmlParserInterface xmlParser = mXMLParsers.get(i);
				if(xmlParser != null){
					if(NetworkUtil.isNetworkAvailable(mContext)) {
					
						if(NetworkUtil.isUrlValid(checkedUrl)) {
							InputStream in = NetworkUtil.getRSSInputStream(checkedUrl);
							if(in != null){
								if(xmlParser.preParseRss(in)){
									try {
										in.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
									state = RssConstant.State.STATE_SUCCESS;
									Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_SUCCESS");
									break;
								}else{
									try {
										in.close();
									} catch (IOException e) {
										e.printStackTrace();
									}
									state = RssConstant.State.STATE_PARSE_XML_FAILURE;
									Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_PARSE_XML_FAILURE");
								}
							}else{
								state = RssConstant.State.STATE_CONNECTY_FAILURE;
								Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_CONNECTY_FAILURE");
							}
						}else{
							state = RssConstant.State.STATE_CONNECTY_FAILURE;
							Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_CONNECTY_FAILURE");
						}
					}else{
						state = RssConstant.State.STATE_NETWORK_NOT_AVAILABLE;
						Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_NETWORK_NOT_AVAILABLE");
					}
				}else{
					state = RssConstant.State.STATE_NO_PARSER;
					Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_NO_PARSER");
				}
			}
		}
		return state;
	}
	
	
	public int parse(String checkedUrl, FeedInfo info, List<ItemInfo> infos, boolean bCorrectedDate){
		return tryParseRSS(checkedUrl, info, infos, bCorrectedDate);
	}
	private int tryParseRSS(String checkedUrl, FeedInfo info, List<ItemInfo> infos, boolean bCorrectedDate){
		int state = 0;
		int parserCount = 0;
		if(mXMLParsers != null){
			parserCount = mXMLParsers.size();
			for(int i=0;i<parserCount;i++){
				XmlParserInterface xmlParser = mXMLParsers.get(i);
				if(xmlParser != null){
					if(NetworkUtil.isNetworkAvailable(mContext)) {
						InputStream in = NetworkUtil.getRSSInputStream(checkedUrl);
						if(in != null){
							int result = xmlParser.parseRss(in, info, infos, bCorrectedDate);
							try {
								in.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							state = result;

							Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_SUCCESS");
						} else {
							state = RssConstant.State.STATE_CONNECTY_FAILURE;
							Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_CONNECTY_FAILURE");
						}
//						if(NetworkUtil.isUrlValid(checkedUrl)) {
//							InputStream in = NetworkUtil.getRSSInputStream(checkedUrl);
//							if(in != null){
//								if(xmlParser.parseRss(in, info, infos)){
//									try {
//										in.close();
//									} catch (IOException e) {
//										e.printStackTrace();
//									}
//									state = RssConstant.State.STATE_SUCCESS;
//									Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_SUCCESS");
//									break;
//								}else{
//									try {
//										in.close();
//									} catch (IOException e) {
//										e.printStackTrace();
//									}
//									state = RssConstant.State.STATE_PARSE_XML_FAILURE;
//									Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_PARSE_XML_FAILURE");
//								}
//							}else{
//								state = RssConstant.State.STATE_CONNECTY_FAILURE;
//								Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_CONNECTY_FAILURE");
//							}
//						}else{
//							state = RssConstant.State.STATE_URL_NOT_AVAILABLE;
//							Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_URL_NOT_AVAILABLE");
//						}
					}else{
						state = RssConstant.State.STATE_NETWORK_NOT_AVAILABLE;
						Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_NETWORK_NOT_AVAILABLE");
					}
				}else{
					state = RssConstant.State.STATE_NO_PARSER;
					Log.d(TAG, "<<<< " + checkedUrl + " >>>>" + "State.STATE_NO_PARSER");
				}
			}
		}
		return state;
	}
}
