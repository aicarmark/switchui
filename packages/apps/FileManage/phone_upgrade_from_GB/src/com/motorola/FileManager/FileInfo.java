package com.motorola.FileManager;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.drm.DrmManagerClient;
import android.drm.DrmStore;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;

public class FileInfo {

    public final static char SEPERATOR = '/';

    private final static int OTHER_FILE = 0x00001;
    private final static int DIRECTORY = 0x00002;
    private final static int WRITABLE = 0x00004;
    private final static int READABLE = 0x00008;
    private final static int HIDDEN = 0x00010;

    // Begin CQG478 liyun, IKDOMINO-4110, adapter image icon 2011-11-09
    private int mFileMimeId = Util.NEED_QUERY_MIME;
    // End

    private VirtualFolder mTop; // this will not be null;

    private String mPath; // if path is null then this will be a virtual folder
    private long mSize;
    private long mDate;
    private int mChildCount;
    private int mAttr;
    private int mUnHideChildCount;

    private FileInfo(VirtualFolder top, String path, int attr, long size,
	    long date, int childCount, int unHideCount) {
	mTop = top;
	mChildCount = childCount;
	mUnHideChildCount = unHideCount;

	if (path == null) {
	    mPath = null;
	    mSize = 0;
	    mDate = 0;
	    mAttr = 0;
	} else {
	    mPath = new String(path);
	    mAttr = attr;
	    mSize = size;
	    mDate = date;
	}
    }

    private FileInfo(VirtualFolder top) {
	this(
		top,
		null,
		0,
		0,
		0,
		(top != null && top.getChildren() != null) ? top.getChildren().length
			: 0, 0);
    }

    public int getIconId(Context context) {
	if (isVirtualFolder()) {
	    return R.drawable.ic_virtual_folder;
	} else if ((mAttr & DIRECTORY) != 0) {
	    return R.drawable.ic_folder;
	} else if ((mAttr & OTHER_FILE) != 0) {
	    // Begin CQG478 liyun, IKDOMINO-4110, adapter image icon 2011-11-09
	    // switch (Util.getMimeId(context, this)) {
	    if (mFileMimeId == Util.NEED_QUERY_MIME) {
		mFileMimeId = Util.getMimeIdWithoutQueryDB(this.getRealPath());
	    }
	    switch (mFileMimeId) {
	    // End
	    case MimeType.MIME_AUDIO:
		return R.drawable.ic_audio;
	    case MimeType.MIME_VIDEO:
		return R.drawable.ic_video;
	    case MimeType.MIME_IMAGE:
		return R.drawable.ic_image;
	    case MimeType.MIME_TEXT:
		return R.drawable.ic_txt;
	    case MimeType.MIME_APK:
		return R.drawable.ic_apk;
	    case MimeType.MIME_HTML:
		return R.drawable.ic_html;
	    case MimeType.MIME_PDF:
		return R.drawable.ic_pdf;
	    case MimeType.MIME_DOC:
		return R.drawable.ic_doc;
	    case MimeType.MIME_PPT:
		return R.drawable.ic_ppt;
	    case MimeType.MIME_EXCEL:
		return R.drawable.ic_excel;
	    case MimeType.MIME_ZIP:
		return R.drawable.ic_zip;
		// Begin CQG478 liyun, IKDOMINO-2266, add vcf type 2011-9-5
	    case MimeType.MIME_VCF:
		return R.drawable.ic_vcf;
		// End
	    default:
		return R.drawable.ic_unknow;
	    }
	} else {
	    return R.drawable.icon;
	}
    }

    public Drawable getIcon(Context context) {
	int iconId = R.drawable.icon;

	if (isVirtualFolder()) {
	    iconId = R.drawable.ic_virtual_folder;
	} else if ((mAttr & DIRECTORY) != 0) {
	    iconId = R.drawable.ic_folder;
	} else if ((mAttr & OTHER_FILE) != 0) {
		if (FileManagerActivity.isDrm && Util.getExtension(this.getRealPath()).equals("dcf")) {
			DrmManagerClient drmClient = new DrmManagerClient(context);
			if (DrmStore.RightsStatus.RIGHTS_VALID == drmClient.checkRightsStatus(this.getRealPath())){
				iconId = R.drawable.drm_unlock;
			} else {
				iconId = R.drawable.drm_lock;
			}
			drmClient = null;
		}
		else {		
		    if (mFileMimeId == Util.NEED_QUERY_MIME) {
			mFileMimeId = Util.getMimeIdWithoutQueryDB(this.getRealPath());
		    }
			switch (mFileMimeId) {
			    case MimeType.MIME_AUDIO:
			    	iconId = R.drawable.ic_audio;
			    	break;
			    case MimeType.MIME_VIDEO:
			    	iconId = R.drawable.ic_video;
			    	break;
			    case MimeType.MIME_IMAGE:
			    	iconId = R.drawable.ic_image;
			    	break;
			    case MimeType.MIME_TEXT:
			    	iconId = R.drawable.ic_txt;
			    	break;
			    case MimeType.MIME_APK:		
			    	Drawable icon = Util.getApkIcon(context, this.getRealPath());
			    	if (icon != null) {
			    		return icon;
			    	} else {
			    		iconId = R.drawable.ic_apk;
			    	}		
			    	break;
			    case MimeType.MIME_HTML:
			    	iconId = R.drawable.ic_html;
			    	break;
			    case MimeType.MIME_PDF:
			    	iconId = R.drawable.ic_pdf;
			    	break;
			    case MimeType.MIME_DOC:
			    	iconId = R.drawable.ic_doc;
			    	break;
			    case MimeType.MIME_PPT:
			    	iconId = R.drawable.ic_ppt;
			    	break;
			    case MimeType.MIME_EXCEL:
			    	iconId = R.drawable.ic_excel;
			    	break;
			    case MimeType.MIME_ZIP:
			    	iconId = R.drawable.ic_zip;
			    	break;
			    case MimeType.MIME_VCF:
			    	iconId = R.drawable.ic_vcf;
			    	break;
			    default:
			    	iconId = R.drawable.ic_unknow;
			    	break;
			}
		}
	} else {
	    iconId = R.drawable.icon;
	}
	return context.getResources().getDrawable(iconId);
    }

    public String getTitle() {
	if (mPath == null) {
	    return mTop.getTitle();
	} else {
	    int n = mPath.lastIndexOf(SEPERATOR);
	    return mPath.substring((n < 0) ? 0 : n + 1);
	}
    }

    public long getSize() {
	return mSize;
    }

    public long getDate() {
	return mDate;
    }

    public int getChildCount() {
	return mChildCount;
    }

    public int getUnHideChildCount() {
	return mUnHideChildCount;
    }

    public VirtualFolder getTop() {
	return mTop;
    }

    public boolean isRoot() {
	return (mTop.getParent() == null && mPath == null);
    }

    public boolean isDirectory() {
	return (mPath == null || (mAttr & DIRECTORY) != 0);
    }

    public boolean isHidden() {
	return (mPath != null && (mAttr & HIDDEN) != 0);
    }

    public void setHidden() {
	if (mPath != null) {
	    mAttr |= HIDDEN;
	}
    }

    public void setNotHidden() {
	if (mPath != null) {
	    mAttr = mAttr & (~HIDDEN);
	}
    }

    public boolean canWrite() {
	return (mPath != null && (mAttr & WRITABLE) != 0);
    }

    public boolean isVirtualFolder() {
	return (mPath == null && mTop.getRealPath() == null);
    }

    public String getRealPath() {
	if (mPath != null)
	    return mTop.getRealPath() + SEPERATOR + mPath;
	else
	    return mTop.getRealPath();
    }

    public String getVirtualPath() {
	if (mPath != null)
	    return mTop.getVirtualPath() + VirtualFolder.SEPERATOR + mPath;
	else
	    return mTop.getVirtualPath();
    }

    public boolean canOperate() {
	return (mPath != null || mTop.getChildren() == null);
    }

    public String getVirtualPathForDisplay() {
	if (mPath != null)
	    return mTop.getVirtualPathForDisplay() + SEPERATOR + mPath;
	else
	    return mTop.getVirtualPathForDisplay();
    }

    public FileInfo getParent() {
	if (mPath != null) {
	    int index = mPath.lastIndexOf(SEPERATOR);
	    if (index < 0) {
		return new FileInfo(mTop);
	    } else {
		File file = new File(mTop.getRealPath() + SEPERATOR
			+ mPath.substring(0, index));
		return FileInfo.make(mTop, file);
	    }
	} else if (mTop.getParent() != null) {
	    return new FileInfo(mTop.getParent());
	} else {
	    return null;
	}
    }

    public FileInfo[] getChildren() {
	if (mPath == null) {
	    VirtualFolder[] vchildren = mTop.getChildren();

	    if (vchildren != null) {
		int count = vchildren.length;
		FileInfo[] children = new FileInfo[count];
		for (int i = 0; i < count; i++) {
		    children[i] = new FileInfo(vchildren[i]);
		}
		return children;
	    }
	}

	String path = getRealPath();
	if (path != null) {
	    File file = new File(path);

	    // Begin CQG478 liyun, IKDOMINO-1423, remove hide/unhide feature
	    // 2011-8-2
	    // File[] files = file.listFiles();
	    File[] files = Util.getFileList(file, true);
	    // End
	    int count = (files != null) ? files.length : 0;
	    if (count > 0) {
		FileInfo[] children = new FileInfo[count];
		int i = 0;
		for (File currentFile : files) {
		    children[i++] = FileInfo.make(mTop, currentFile);
		}
		return children;
	    }
	}
	return null;
    }

    public FileInfo[] getFolderChildren() {
	if (mPath == null) {
	    VirtualFolder[] vchildren = mTop.getChildren();

	    if (vchildren != null) {
		int count = vchildren.length;
		FileInfo[] children = new FileInfo[count];

		for (int i = 0; i < count; i++) {
		    children[i] = new FileInfo(vchildren[i]);
		}
		return children;
	    }
	}

	String path = getRealPath();
	if (path != null) {
	    File file = new File(path);

	    // Begin CQG478 liyun, IKDOMINO-1423, remove hide/unhide feature
	    // 2011-8-2
	    // File[] files = file.listFiles();
	    File[] files = Util.getFileList(file, true);
	    // End
	    int count = (files != null) ? files.length : 0;
	    if (count > 0) {
		int foldercount = 0;
		int i = 0;
		for (File currentFile : files) {
		    if (currentFile.isDirectory())
			foldercount++;
		}

		FileInfo[] children = new FileInfo[foldercount];
		i = 0;
		for (File currentFile : files) {
		    if (currentFile.isDirectory())
			children[i++] = FileInfo.make(mTop, currentFile);
		}
		return children;
	    }
	}
	return null;
    }

    public ArrayList<FileInfo> getChildrenList() {

	ArrayList<FileInfo> childrenList = new ArrayList<FileInfo>();

	if (mPath == null) {
	    VirtualFolder[] vchildren = mTop.getChildren();

	    if (vchildren != null) {
		int count = vchildren.length;
		for (int i = 0; i < count; i++) {
		    FileInfo fi = new FileInfo(vchildren[i]);
		    childrenList.add(fi);
		}

		return childrenList;
	    }
	}

	String path = getRealPath();
	if (path != null) {
	    File file = new File(path);

	    // Begin CQG478 liyun, IKDOMINO-1423, remove hide/unhide feature
	    // 2011-8-2
	    // File[] files = file.listFiles();
	    File[] files = Util.getFileList(file, true);
	    // End
	    int count = (files != null) ? files.length : 0;
	    if (count > 0) {
		for (File currentFile : files) {
		    FileInfo fileInfo = FileInfo.make(mTop, currentFile);
		    if (fileInfo != null) {
			childrenList.add(fileInfo);
		    }
		}

		return childrenList;
	    }
	}
	return null;
    }

    public static FileInfo make(VirtualFolder mTop, File file) {
	if (mTop == null)
	    return null;

	String topPath = mTop.getRealPath();

	if (file == null || topPath == null) {
	    return new FileInfo(mTop);
	}

	String filePath = file.getAbsolutePath();

	if (Util.isSamePath(topPath, filePath)) {
	    return new FileInfo(mTop);
	}

	int n = topPath.length();
	String sub = null;
    try {
    	sub = filePath.substring(0, n);
    } catch (Exception e){
    	return null;
    }
    
    if (sub == null){
    	return null;
    }
    
	if (sub.compareToIgnoreCase(topPath) == 0) {
	    if (topPath.charAt(topPath.length() - 1) != SEPERATOR)
		n++;

	    sub = filePath.substring(n, filePath.length());
	    if (sub != null) {
		int attr = file.isDirectory() ? DIRECTORY : OTHER_FILE;
		if (file.canWrite())
		    attr |= WRITABLE;
		if (file.canRead())
		    attr |= READABLE;
		if (file.isHidden())
		    attr |= HIDDEN;

		int count = 0;
		int unhidecount = 0;
		if (file.isDirectory()) {

		    // Begin CQG478 liyun, IKDOMINO-1423, remove hide/unhide
		    // feature 2011-8-2
		    // File[] list = file.listFiles();
		    File[] list = Util.getFileList(file, true);
		    // End
		    if (list != null)
			count = list.length;
		    for (int i = 0; i < count; i++)
			if (list[i].isHidden() == false)
			    unhidecount++;

		}
		return new FileInfo(mTop, sub, attr, file.length(),
			file.lastModified(), count, unhidecount);

	    }
	}
	return null;
    }

    public static FileInfo make(String path) {
	VirtualFolder vf = FileSystem.getVirtualFolderByPath(path);
	if (vf != null) {
	    if (path.charAt(0) == SEPERATOR) {
		return FileInfo.make(vf, new File(path));
	    } else if (path.charAt(0) == VirtualFolder.SEPERATOR) {

		int index = path.lastIndexOf(VirtualFolder.SEPERATOR);
		if (index > 0 && path.length() > 1) {
		    File file = new File(vf.getRealPath() + SEPERATOR
			    + path.substring(index + 1));
		    return FileInfo.make(vf, file);
		} else if (index == 0) {
		    return new FileInfo(vf);
		}
	    }
	}
	return null;
    }

    // Begin CQG478 liyun, IKDOMINO-4110, adapter image icon 2011-11-09
    public int getFileMimeId() {
	return mFileMimeId;
    }

    public void setFileMimeId(int id) {
    	mFileMimeId = id;
    }

    public void setImageViewIcon(final Context context,
	    final Util.ImageCallBack imageCallBack) {

	final Handler handler = new Handler() {
	    @Override
	    public void handleMessage(Message msg) {
		imageCallBack.setImageIcon();
	    }
	};

	new Thread() {
	    @Override
	    public void run() {
		mFileMimeId = Util.getMimeId(context, getRealPath());
		Message msg = handler.obtainMessage();
		msg.sendToTarget();
	    }
	}.start();
    }
    // End
}
