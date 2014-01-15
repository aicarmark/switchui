package com.motorola.mmsp.rss.common;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.graphics.Bitmap;

public class ViewItems {

	public int _id;
	public int widgetId;
	public int feedId;
	public String feedTitle;
	public String feedUrl;
	public int itemId;
	public String itemTitle;
	public String itemUrl;
	public String itemDescription;
	public String itemAuthor;
	public int itemState;
	public long itemPubdate;
	public Bitmap feeIcon;

	@Override
	public String toString() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String out = "widgetId = " + widgetId + " , feedId = " + feedId
				+ " , itemId = " + itemId + " , itemTitle = " + itemTitle
				+ " , itemPubDate = " + sdf.format(new Date(itemPubdate));

		return out;
	}

}
