package com.motorola.mmsp.rss.service.flex;

public class RssFlexFeedInfo {
	private String mFeedName;
	private String mFeedUrl;
	
	public RssFlexFeedInfo() {
		
	}
	
	public RssFlexFeedInfo(String feedName, String feedUrl) {
		mFeedName = feedName;
		mFeedUrl = feedUrl;
	}

	public String getFeedName() {
		return mFeedName;
	}

	public void setFeedName(String feedName) {
		this.mFeedName = feedName;
	}

	public String getFeedUrl() {
		return mFeedUrl;
	}

	public void setFeedUrl(String feedUrl) {
		this.mFeedUrl = feedUrl;
	}	
	
}
