package com.motorola.mmsp.socialGraph.socialwidget2D;

public class ChangeHistory {
	public static final String HISTORY_ID = "_id";
	public static final String HISTORY_TYPE = "type";
	public static final String HISTORY_INDEX = "history_index";
	public static final String HISTORY_DATA1 = "data1";
	public static final String HISTORY_DATA2 = "data2";
	public static final String HISTORY_DATA3 = "data3";
	public static final String HISTORY_DATA4 = "data4";
	
	public static final int NONE = 0;
	public static final int HISTORY_UPDATE_INFO = 1;
	public int type;
	int index;
	int from;
	int to;
}
