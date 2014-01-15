package com.motorola.mmsp.rss.common;

import java.util.Date;

public class ItemInfo {

	public int itemId;
	public String itemTitle;
	public String itemUrl;
	public String itemDescription;
	public String itemGuid;
	public String itemAuthor;
	public long itemPubdate;

	@Override
	public String toString() {
		String pubDate = "";
		Date date = new Date(itemPubdate);
		pubDate = date.toLocaleString();
		String out = "itemId = " + itemId + " , itemTitle = " + itemTitle
				+ " , itemUrl = " + itemUrl + " , itemGuid = " + itemGuid
				+ " , itemAuthor = " + itemAuthor + " , itemPubdate = "
				+ pubDate;

		return out;
	}

}
