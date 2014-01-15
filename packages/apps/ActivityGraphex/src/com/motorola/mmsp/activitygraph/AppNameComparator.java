package com.motorola.mmsp.activitygraph;

import java.text.Collator;
import java.util.Comparator;

public class AppNameComparator implements Comparator<AppInfo>
{
	public AppNameComparator() {
    }
	private final Collator   sCollator = Collator.getInstance();
    public int compare(AppInfo App1, AppInfo App2) {
    	return sCollator.compare(App1.getAppName(), App2.getAppName());
    }
    
}