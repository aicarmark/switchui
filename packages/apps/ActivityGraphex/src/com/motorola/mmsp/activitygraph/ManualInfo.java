package com.motorola.mmsp.activitygraph;



public class ManualInfo {
    public int mId; //the index of the activity
    public String mName;
    
    ManualInfo() {
    	
    }
    
    ManualInfo(ManualInfo info) {
    	this.mId = info.mId;
    	this.mName = info.mName;
    }
}
