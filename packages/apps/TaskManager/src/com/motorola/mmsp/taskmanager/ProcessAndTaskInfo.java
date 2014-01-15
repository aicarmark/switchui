package com.motorola.mmsp.taskmanager;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.content.Intent;

class ProcessAndTaskInfo {
    String packagename;
    String appname;
    boolean isPersistent;
    boolean isPersistent_TopActivity;
    boolean checked;
    boolean checkvisibility;
    int tid;
    int importance;
    int pid;
    ComponentName baseActivity;
    ComponentName topActivity;
    Drawable appicon;
    Intent currentIntent;
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "packagename: " + packagename + ", appname: " + appname + ", isPersistent: " + isPersistent
		+ ", isPersistent_TopActivity: " + isPersistent_TopActivity + ", checked = " + checked;
	}
    
    
}
