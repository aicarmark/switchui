package com.motorola.mmsp.rss.common;

public class IndexInfo {

	public int widgetId;
	public int feedId;
	public int itemId;
	public int itemState;

	@Override
	public String toString() {
		String out = "widgetId = " + widgetId + " , feedId = " + feedId
				+ " , itemId = " + itemId + " , itemState = " + itemState;
		return out;
	}

}
