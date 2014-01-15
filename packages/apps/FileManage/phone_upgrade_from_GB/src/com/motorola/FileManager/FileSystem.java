package com.motorola.FileManager;

import java.io.File;
import android.content.Context;
import android.util.AndroidRuntimeException;
import android.util.Log;


final class TooManyResultException extends AndroidRuntimeException {
	private static final long serialVersionUID = 1L;

	public TooManyResultException(String msg) {
        super(msg);
    }
}

public class FileSystem {	
	//modify by amt_xulei for SWITCHUITWO-7
    //reason:CT JB testload SDcard and internal storage path
    // is /storage/sdcard0 and /storage/sdcard1
//	public static final String SDCARD_DIR ="/mnt/external1";//IronPrime CT
	public static final String SDCARD_DIR ="/storage/sdcard0";//SDcard
	
//  public static final String Internal_SDCARD_DIR = "/mnt/sdcard";
	public static final String Internal_SDCARD_DIR = "/storage/sdcard1";//internal storage
	//end modify
	
	public final static String ROOT_DIR = "/";
        
	public static final String TAG_FOLDER = "folder";
	public static final String TAG_ITEM = "folder";
	
	public static final String ATTR_NAME = "name";
	public static final String ATTR_PATH = "path";
	
	private static FileInfo mRoot = null;
	
	private static FileSystem mFileSystem;
	
	private FileSystem(Context context) {
		mRoot = null;
		if(mRoot == null)
		{
			Log.d("FileSystem", "src mRoot is null");
			mRoot = FileInfo.make(VirtualFolder.buildVirtualFolderTree(), null);
		} else {
			Log.d("FileSystem", "src mRoot is not null");
		}
	}

	public static FileSystem getFileSystem(Context context) {
		if(mFileSystem == null) {
			mFileSystem = new FileSystem(context);
		}
		return mFileSystem;
	}
	
	public static void setRoot() {
		mRoot = FileInfo.make(VirtualFolder.buildVirtualFolderTree(), null);
	}
	public static FileInfo getRoot() {
		//mRoot = null;
		//if(mRoot == null)
		//{
			Log.d("FileSystem", "src mRoot is null");
			mRoot = FileInfo.make(VirtualFolder.buildVirtualFolderTree(), null);
		//} else {
		//	Log.d("FileSystem", "src mRoot is not null");
		//}
		return mRoot;
	}
	
	public FileInfo createFolderOrFile(FileInfo parent, String path) {
		try {
			File file = new File(path);	
			if(file.mkdir()) {
				return FileInfo.make(parent.getTop(), file);
			}
		} catch(Exception e) {
			return null;
		}
		return null;
	}


	public static boolean renameFileOrFolder(FileInfo fi, String newName) {
		if(fi == null || fi.isVirtualFolder() || newName == null)
			return false;
		
		try {
			String oldpath = fi.getRealPath();		
			if(oldpath != null) {
				File oldFile = new File(oldpath);
				String newpath = oldFile.getParent();
				if(newpath != null) {
					newpath += "/" + newName;
					File newFile = new File(newpath);
					if(newFile.exists()) {
						if(newFile.isFile() && oldFile.isFile() || 
								newFile.isDirectory() && oldFile.isDirectory())
							return false;
					}
					return oldFile.renameTo(newFile);
				}
			}
		} catch (Exception e) {
			return false;
		}	
		return false;
	}

	public static boolean hideFolder(FileInfo fi) {
		if(fi == null || fi.isVirtualFolder() || fi.isDirectory() == false )
			return false;
		
		try {
			String oldpath = fi.getRealPath();		
			if(oldpath != null) {
				File oldFile = new File(oldpath);
				if(oldFile.isHidden() ==true) return true;
				String newpath = oldFile.getParent();
				String newName = oldFile.getName();
				newName="." + newName;
				if(newpath != null) {
					newpath += "/" + newName;
					File newFile = new File(newpath);
					if(newFile.exists()) {
						if(newFile.isFile() && oldFile.isFile() || 
								newFile.isDirectory() && oldFile.isDirectory())
							return false;
					}
					return oldFile.renameTo(newFile);
				}
			}
		} catch (Exception e) {
			return false;
		}	
		return false;
	}
	public static boolean unhideFolder(FileInfo fi) {
		if(fi == null || fi.isVirtualFolder() || fi.isDirectory() == false)
			return false;
		
		try {
			String oldpath = fi.getRealPath();		
			if(oldpath != null) {
				File oldFile = new File(oldpath);
				if(oldFile.isHidden()==false) return true;
				String newpath = oldFile.getParent();
				String newName = oldFile.getName();
				if(newName.charAt(0) == '.')
					newName = newName.substring(1);
				if(newpath != null) {
					newpath += "/" + newName;
					File newFile = new File(newpath);
					if(newFile.exists()) {
						if(newFile.isFile() && oldFile.isFile() || 
								newFile.isDirectory() && oldFile.isDirectory())
							return false;
					}
					return oldFile.renameTo(newFile);
				}
			}
		} catch (Exception e) {
			return false;
		}	
		return false;
	}
	public static boolean unHideAllFolderAtSameLevel(FileInfo fi) {
		FileInfo[] ffi= fi.getParent().getChildren();
		for(int i=0;i<ffi.length;i++) unhideFolder(ffi[i]);
		return true;
	}
	public static boolean unHideAllSubFolder(FileInfo fi) {
		FileInfo[] ffi= fi.getChildren();
		for(int i=0;i<ffi.length;i++) unhideFolder(ffi[i]);
		return true;
	}
	

	
	
	public static boolean isVirtualFolder(File file) {
		return isVirtualFolder(mRoot.getTop(), file);
	}
	
	
	private static boolean isVirtualFolder(VirtualFolder vf, File file) {
		String path = vf.getRealPath();
				
		VirtualFolder[] children = vf.getChildren();
		if(children != null) {
			for(int i = 0; i < children.length; i++) {
				if(isVirtualFolder(children[i], file))
					return true;
			}
		}
		
		if(path != null) {
			File vfile = new File(path);
			return vfile.equals(file);
		}

		return false;
	}
	
	public static VirtualFolder getVirtualFolderByPath(String path) {
		if(path != null ) {
			if(path.charAt(0) == VirtualFolder.SEPERATOR) {
				int index = path.lastIndexOf(VirtualFolder.SEPERATOR);
				Log.d("", "getVirtualFolderByPath -index = "+ index);
				if(index > 0) {
					String sub = path.substring(1, index);
					if(sub != null) {
						try {
							int id =  Integer.parseInt(sub);
							VirtualFolder vf = mRoot.getTop();
							Log.d("", "getVirtualFolderByPath- vf = " + vf);
							return findVirtualFolderById(vf, id);
						} catch (NumberFormatException e) {
							Log.d("", "getVirtualFolderByPath - index > 0 findVirtualFolderById exception");
							return null;
						}
					}
				} else if (index == 0 && path.length() > 1) {
					try {
						int id =  Integer.parseInt(path.substring(1));
						VirtualFolder vf = mRoot.getTop();
						return findVirtualFolderById(vf, id);
					} catch (NumberFormatException e) {
						Log.d("", "getVirtualFolderByPath - index = 0 findVirtualFolderById exception");
						return null;
					}					
				}
			} else if (path.charAt(0) == FileInfo.SEPERATOR) {
				//Log.d("", "getVirtualFolderByPath -inpath.charAt(0)= "+ path.charAt(0));
				VirtualFolder vf = FileSystem.getRoot().getTop();
				//Log.d("", "getVirtualFolderByPath- vf =" + vf.getVirtualPath());
				return findVirtualFolderByPath(vf, path);
			}
		}
		return null;
	}
	
	private static VirtualFolder findVirtualFolderById(VirtualFolder vf, int id) {
		if(vf != null) {
			Log.d("", "findVirtualFolderById - vf is not null");
			if(vf.getId() == id) {
				return vf;
			} else {
				Log.d("", "findVirtualFolderById - vf.getId() != id");
				VirtualFolder[] children = vf.getChildren();
				if(children != null) {
					for(int i = 0; i < children.length; i++) {
						if(findVirtualFolderById(children[i], id) != null)
							return children[i];
					}
				}
			}
		}
		return null;
	}

	private static VirtualFolder findVirtualFolderByPath(VirtualFolder vf, String path) {
		if(vf != null) {
			String p = vf.getRealPath();
			Log.d("", "findVirtualFolderByPath- p = " + p);
			if(p != null) {
				int index = path.indexOf(path);
				if(index >= 0)
					return vf;
			}
			VirtualFolder[] children = vf.getChildren();
			if(children != null) {
				for(int i = 0; i < children.length; i++) {
					if(findVirtualFolderByPath(children[i], path) != null)
						return children[i];
				}
			}
		}
		return null;
	}
}
