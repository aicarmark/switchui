package com.motorola.mmsp.rss.service.flex;

public class RssFlexFactory {
	
	public static RssFlexInterface createFlexSettings() {
		return RssFlexSettings.getInstance();
	}
	
	public static RssFlexInterface createFlexTheme() {
		return null;
	}
}
