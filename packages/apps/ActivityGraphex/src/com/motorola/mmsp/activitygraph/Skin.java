package com.motorola.mmsp.activitygraph;

import java.util.HashMap;

import android.util.Log;

public class Skin extends CommonColumn{
	public HashMap<String, Object> properties = new HashMap<String, Object>();
	
	Skin(HashMap<String, Object> map) {
		this.properties = map;
	}
	
}
