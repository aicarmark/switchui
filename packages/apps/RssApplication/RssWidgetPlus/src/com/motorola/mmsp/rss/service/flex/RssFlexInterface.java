package com.motorola.mmsp.rss.service.flex;

import java.util.ArrayList;

import android.content.Context;

public interface RssFlexInterface {
	public ArrayList<RssFlexFeedInfo> getFlexFeedList(Context context);
	public int getNewsValidity(Context context);
	public int getUpdateFrequency(Context context);
}
