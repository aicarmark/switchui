package com.motorola.mmsp.rss.service.parse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

import com.motorola.mmsp.rss.common.FeedInfo;
import com.motorola.mmsp.rss.common.ItemInfo;
import com.motorola.mmsp.rss.common.RssConstant;
import com.motorola.mmsp.rss.util.DateTimeUtil;

public class RssXmlPullParser implements XmlParserInterface {
	private final int MAX_YEAR_FOR_FEED = 200; //+1900: 2100
	private final int MIN_YEAR_FOR_FEED = 100; //+1900: 2000
	
	private final String TAG = "XmlDomParser";
	@SuppressWarnings("finally")
	public int parseRss(InputStream in, FeedInfo info, List<ItemInfo> infos, boolean bCorrectedDate) {
					// TODO: switch to sax
		//boolean hasRssTag = false;
		int result = RssConstant.State.STATE_PARSE_XML_FAILURE;
			XmlPullParser xpp = Xml.newPullParser();			
			try{
				xpp.setInput(in, null); // null = default to UTF-8
				int eventType;
				int maxYear = MAX_YEAR_FOR_FEED; //add by wdmk68
				String title = "";
				String link = "";
				String description = "";
				String author = "";
				String rssVersion = "";
				String pubFeedDate = "";
				String pubItemDate = "";
				String feedIcon = "";
				String feedTitle = "";
				String itemGuid = null;
				eventType = xpp.getEventType();
				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_TAG) {
						String tag = xpp.getName();
						if (tag.equalsIgnoreCase("rss")) {
							//hasRssTag = true;
							result = RssConstant.State.STATE_SUCCESS;
							rssVersion = xpp.getAttributeValue(0);
						} else if (tag.equalsIgnoreCase("title") && xpp.getDepth() == 3) {
							xpp.next();
							feedTitle = xpp.getText();
							if(info.feedTitle == null){
								info.feedTitle = removeTags(feedTitle);
							}
						}else if (tag.equalsIgnoreCase("url") && xpp.getDepth() == 4) {
							xpp.next();
							feedIcon = xpp.getText();
							info.feedIcon = removeTags(feedIcon);
						}else if (tag.equalsIgnoreCase("pubDate")
								&& xpp.getDepth() == 3) {
							xpp.next();
							pubFeedDate = xpp.getText();
						} else if (tag.equalsIgnoreCase("item")) {
							title = link = description = pubItemDate = author = "";
						} else if (tag.equalsIgnoreCase("title") && xpp.getDepth() == 4) {
							xpp.next(); // Skip to next element -- assume text is
							// directly inside the tag
							title = xpp.getText();
						} else if (tag.equalsIgnoreCase("link") && xpp.getDepth() == 4) {
							xpp.next();
							link = xpp.getText();
						} else if (tag.equalsIgnoreCase("description") && xpp.getDepth() == 4) {
							xpp.next();
							description = xpp.getText();
						} else if(tag.equalsIgnoreCase("author") && xpp.getDepth() == 4){
							xpp.next();
							author = xpp.getText();
						} else if(tag.equalsIgnoreCase("guid") && xpp.getDepth() == 4){
							xpp.next();
							itemGuid = xpp.getText();
//							Log.d(TAG, "itemGuid = " + itemGuid);
						}else if (tag.equalsIgnoreCase("pubDate") && xpp.getDepth() == 4) {
							xpp.next();
							pubItemDate = xpp.getText();
	//						Log.d(TAG, "pubItemDate = " + pubItemDate);
						}
					} else if (eventType == XmlPullParser.END_TAG) {
						// We have a comlete item -- post it back to the UI
						// using the mHandler (necessary because we are not
						// running on the UI thread).
						String tag = xpp.getName();
						if (tag.equalsIgnoreCase("rss")) {
	
						} else if (tag.equalsIgnoreCase("pubDate")
								&& xpp.getDepth() == 3) {
//							Log.d(TAG, pubFeedDate);
							Date date = DateTimeUtil.getDate(pubFeedDate);
							info.feedPubdate = date.getTime();
							//Add by wdmk68
							if (pubFeedDate != null  && pubFeedDate.length() > 0){
								if (date.getYear() > MIN_YEAR_FOR_FEED && date.getYear() < MAX_YEAR_FOR_FEED){
									maxYear = date.getYear();
								}
							}
							//End add by wdmk68
						} else if (tag.equalsIgnoreCase("item")) {
							ItemInfo itemInfo = new ItemInfo();
							Boolean bHasError = false;
							
							if(pubItemDate == null){
								itemInfo.itemPubdate = 0;
							}else{
								Date dateItem = DateTimeUtil.getDate(pubItemDate);
								//Only wap.sohu.com has error date.
								//FeedPubdate can't correct error date.
								//pubFeedDate is valid, and maxYear has been changed
//								if (info.feedUrl.contains("wap.sohu.com") 
//										&& maxYear < MAX_YEAR_FOR_FEED && dateItem.getTime() > info.feedPubdate){
//									bHasError = true;
//								}
								if (dateItem.getYear() <= MIN_YEAR_FOR_FEED || dateItem.getYear() > MAX_YEAR_FOR_FEED) { //year from 2000 to 2200 1900+date.getYear()
									Log.e(TAG, "date error: " + pubItemDate);
									Log.e(TAG, "pubFeedDate: " + pubFeedDate);
									Log.e(TAG, "itemInfo.itemUrl = " + link);
									if (bCorrectedDate) {
										if (maxYear < MAX_YEAR_FOR_FEED){//pubFeedDate is valid, and maxYear has been changed
											dateItem.setTime(info.feedPubdate);
											Log.e(TAG, "change date to " + dateItem.toString());
											bHasError = false;
										} else {
											bHasError = true; //Hide it.
										}
									
									} else {
										bHasError = true;//Hide it.
									}
								}
							
								itemInfo.itemPubdate = DateTimeUtil.getDate(pubItemDate).getTime();
							}
							itemInfo.itemId = 0;
							itemInfo.itemTitle = removeTags(title);
							itemInfo.itemUrl = removeTags(link);
							itemInfo.itemDescription = removeTags(description);
							itemInfo.itemAuthor = removeTags(author);
							
							if(itemGuid == null){
								String temp = itemInfo.itemUrl;//itemInfo.itemTitle + itemInfo.itemUrl + itemInfo.itemDescription;
								itemInfo.itemGuid = String.valueOf(temp.hashCode());
							}else{
								itemInfo.itemGuid = String.valueOf(itemGuid.hashCode());
							}
							if (!bHasError){
								infos.add(itemInfo);
							} else {
								result = RssConstant.State.STATE_HAS_INVALID_DATA;
							}
	
						}
					}
					eventType = xpp.next();
				}
			}catch(IOException e){
				e.printStackTrace();
				return RssConstant.State.STATE_PARSE_XML_FAILURE;
			}catch(XmlPullParserException e){
				e.printStackTrace();
				return RssConstant.State.STATE_PARSE_XML_FAILURE;
			}catch(Exception e){
				e.printStackTrace();
				return RssConstant.State.STATE_PARSE_XML_FAILURE;
			}finally{
//				Log.d(TAG, "hasRssTag = "  +hasRssTag);
				return result;
			}
	}

	String removeTags(String str) {
//		if(str != null){
//			str = str.replaceAll("<.*?>", " ");
//			str = str.replaceAll("\\s+", " ");
//			str = str.replaceAll("&gt;", ">");
//			str = str.replaceAll("&lt;", "<");
//			str = str.replaceAll("&amp;", "&");
//			str = str.replaceAll("&quot;", "\"");
//			str = str.replaceAll("&nbsp;", " ");
//		}
		if(str != null){
			str = str.trim();
		}
		return str;
	}

	@SuppressWarnings("finally")
	public boolean preParseRss(InputStream in) {
		XmlPullParser xpp = Xml.newPullParser();
		boolean hasRss =false;
		boolean hasChannel = false;
		boolean hasChannelTitle = false;
		boolean hasChannelLink = false;
		boolean hasChannelDes = false;
		boolean hasItem = false;
		boolean hasItemTitle = false;
		boolean hasItemLink =false;
		boolean hasItemDes = false;
		try{
			xpp.setInput(in, null); // null = default to UTF-8
			int eventType;
			eventType = xpp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					String tag = xpp.getName();
					if (tag.equalsIgnoreCase("rss") || tag.equalsIgnoreCase("rdf:RDF")) {
						hasRss = true;
					} else if (tag.equalsIgnoreCase("channel")){
						hasChannel = true;
					}else if (tag.equalsIgnoreCase("title")	&& xpp.getDepth() == 3) {
						hasChannelTitle = true;
					}else if (tag.equalsIgnoreCase("link") && xpp.getDepth() == 3) {
						hasChannelLink = true;
					}else if (tag.equalsIgnoreCase("description") && xpp.getDepth() == 3) {
						hasChannelDes = true;
					} else if (tag.equalsIgnoreCase("item") && xpp.getDepth() == 3) {
						hasItem = true;
					} else if (tag.equalsIgnoreCase("title") && xpp.getDepth() == 4) {
						hasItemTitle = true;
					} else if (tag.equalsIgnoreCase("link") && xpp.getDepth() == 4) {
						hasItemLink = true;
					} else if (tag.equalsIgnoreCase("description") && xpp.getDepth() == 4) {
						hasItemDes = true;
					}
				} else if (eventType == XmlPullParser.END_TAG) {
					// We have a comlete item -- post it back to the UI
					// using the mHandler (necessary because we are not
					// running on the UI thread).
					

				}
				eventType = xpp.next();
			}
		}catch(IOException e){
			Log.d(TAG, e.getLocalizedMessage());
			return false;
		}catch(XmlPullParserException e){
			Log.d(TAG, e.getLocalizedMessage());
			return false;
		}catch(Exception e){
			Log.d(TAG, e.getLocalizedMessage());
			return false;
		}finally{
				Log.d(TAG, "hasRss = " + hasRss);
				Log.d(TAG, "hasChannel = " + hasChannel);
				Log.d(TAG, "hasChannelTitle = " + hasChannelTitle);
				Log.d(TAG, "hasChannelLink = " + hasChannelLink);
				Log.d(TAG, "hasChannelDes = " + hasChannelDes);
				Log.d(TAG, "hasItem = " + hasItem);
				Log.d(TAG, "hasItemTitle = " + hasItemTitle);
				Log.d(TAG, "hasItemLink = " + hasItemLink);
				Log.d(TAG, "hasItemDes = " + hasItemDes);
			return hasRss && hasChannel && hasChannelTitle 
					&& hasChannelLink &&  hasChannelDes && hasItem 
					&& hasItemTitle && hasItemLink && hasItemDes;
					
		}
	}
}
