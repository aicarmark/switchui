package com.motorola.FileManager;

import android.content.Context;

public class FileManagerAdapter extends FMBaseAdapter {

    private FileInfo mCurrentFile;

    public FileManagerAdapter(Context context) {
        super(context);
    }

    public void setCurrentFile(FileInfo fi) {
        mCurrentFile = fi;
    }

    public FileInfo getCurrentFile() {
        return mCurrentFile;
    }
   
    public void setFiles(FileInfo[] files){
        mFiles=files;
        //Begin CQG478 liyun, IKDOMINO-4110, adapter image icon 2011-11-09
        mCount = (mFiles != null ? mFiles.length : 0);
        //End
    }
    
    public FileInfo[] getFiles(){
        return mFiles;
    }
}
