package com.motorola.FileManager;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.String;

import com.motorola.FileManager.FileSystem;
import com.motorola.FileManager.FileManager;

public class VirtualFolder {
    public final static char SEPERATOR = '%';

    private static int mSeed;

    private String mTitle;
    private String mPath;
    private VirtualFolder mParent;
    private VirtualFolder[] mChildren;
    private int mId;
    private static String mRoot = Util.ID_EXTSDCARD;
    private static Context mContext;
    
    public static void setRoot(String root) {
    	mRoot = root;
    }

    public static String getRoot() {
    	return mRoot;
    }
    
    public static void setContext(Context iContext) {
    	mContext = iContext;
    }
    

    public VirtualFolder(String title, String path) {
	if (mSeed == 0)
	    mSeed = 10000;

	mTitle = title;
	mPath = path;
	mId = mSeed++;
    }

    public String getTitle() {
	return mTitle;
    }

    public String getRealPath() {
	return mPath;
    }

    public String getVirtualPath() {
	return SEPERATOR + String.format("%d", mId);
    }

    public String getVirtualPathForDisplay() {
	String path = null;

	VirtualFolder vf = this;

	while (vf != null) {
	    if (vf.mParent != null) {
		if (path != null)
		    path = '/' + vf.mTitle + path;
		else
		    path = '/' + vf.mTitle;
	    }

	    vf = vf.mParent;
	}
	return path;
    }

    public int getId() {
	return mId;
    }

    public VirtualFolder getParent() {
	return mParent;
    }

    public VirtualFolder[] getChildren() {
	return mChildren;
    }

    public static VirtualFolder buildVirtualFolderTree() {
	VirtualFolder root = null;
        Log.d("","buildVirtualFolderTree enter");
	if (mRoot.equals(Util.ID_EXTSDCARD)) {
	    root = null;
	    root = new VirtualFolder(FileManagerActivity.sSDcardTitle, FileManager.getRemovableSd(mContext));
	} else {
	    root = null;
	    root = new VirtualFolder(FileManagerActivity.sInternalSDcardTitle, FileManager.getInternalSd(mContext));
	}
        return root;
    }

}
