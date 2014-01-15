/*
 * Copyright (c) 2010 Motorola, Inc.
 * All Rights Reserved
 *
 * The contents of this file are Motorola Confidential Restricted (MCR).
 * Revision history (newest first):
 *
 *  Date            CR               Author               Description
 *  2010-03-23  IKSHADOW-2074        E12758                 initial
 */

package com.motorola.filemanager.samba.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Arrays;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import android.content.Context;

import com.motorola.filemanager.R;
import com.motorola.filemanager.samba.SambaExplorer;
import com.motorola.filemanager.utils.FileUtils;

public class SambaTransferHandler extends AbstractTransferService {
    private final static String TAG = "SambaTransferHandler: ";

    private static NtlmPasswordAuthentication userAuth = null;
    private static NtlmPasswordAuthentication mDownloadUserAuth = NtlmPasswordAuthentication.ANONYMOUS;

    protected static void setDownloadAuth(NtlmPasswordAuthentication authinfo) {
	mDownloadUserAuth = authinfo;
    }

    public static void setUserAuth(NtlmPasswordAuthentication auth) {
	userAuth = auth;
    }

    public static NtlmPasswordAuthentication getUserAuth() {
	return userAuth;
    }

    public static void provideLoginCredentials(String domain, String username,
	    String password) {
	try {
	    userAuth = new NtlmPasswordAuthentication(null, username, password);
	    SambaExplorer.log("ProvideLoginCredentials is: " + userAuth);
	} catch (Exception e) {
	    SambaExplorer.log("ProvideLoginCredentials Exception: " + e);
	}
    }

    public SambaTransferHandler(OnCanelTransListener listener) {
	super(listener);
    }

    public SambaTransferHandler(Context context, OnCanelTransListener listener) {
	super(context, listener);
    }

    public static void setSambaConfig() {
	jcifs.Config.setProperty("jcifs.encoding", "Cp1252");
	jcifs.Config.setProperty("jcifs.smb.lmCompatibility", "0");
	jcifs.Config.setProperty("jcifs.netbios.hostname", "AndroidPhone");
	jcifs.Config.setProperty("jcifs.resolveOrder", "LMHOSTS,BCAST,DNS");
	jcifs.Config.setProperty("jcifs.smb.client.lport", "5139");
	jcifs.Config.setProperty("jcifs.netbios.lport", "5137");
	jcifs.Config.registerSmbURLHandler();
    }

    private String fomatStrEndWithSlash(String input) {
	if (!input.endsWith("/")) {
	    return input + "/";
	}

	return input;
    }

    private static final int COPY_BUFFER_SIZE = 32 * BASE_SIZE;

    private static final int SMB_COPY_BUFFER_SIZE = 16 * BASE_SIZE;

    @Override
    public int localRename(String destLocal, String srcLocal) {
	if (destLocal.startsWith(srcLocal)) {
	    if (destLocal.substring(srcLocal.length()).startsWith("/")) {
		return DownloadServer.TRANSFER_FAIL;
	    }
	}

	boolean result = false;
	File oldFile = new File(srcLocal);
	setLength(oldFile.length());
	File newFile = FileUtils.getFile(destLocal, oldFile.getName());
	if ((oldFile == null) || (newFile == null)) {
	    return DownloadServer.TRANSFER_FAIL;
	}
	String srcFileName = oldFile.getName();
	showNotification(getTransferOngoingIndicator(), srcFileName,
		TRANS_FLAG, TRANS_START, true, destLocal, srcFileName,
		"FileType");
	try {
	    result = oldFile.renameTo(newFile);
	} catch (Exception e) {
	    e.printStackTrace();
	    result = false;
	}
	showNotification(getTransferCompleteIndicator(), srcFileName,
		TRANS_FLAG, result ? TRANS_END : TRANS_FAIL, true, destLocal,
		srcFileName, "FileType");
	if (result) {
	    if (newFile.isDirectory()) {
		// Need to count total # of folders and files moved for showing
		// in
		// complete dialog
		File[] listFile = newFile.listFiles();
		int total = 0;
		if (listFile != null) {
		    total = listFile.length;
		    for (File f : listFile) {
			if (f.isDirectory()) {
			    total = total + countContent(f);
			}
		    }
		}
		addTransFileCount(total);
		return DownloadServer.TRANSFER_MULTI_COMPLETE;
	    } else {
		return DownloadServer.TRANSFER_COMPLETE;
	    }
	} else {
	    if (oldFile.isDirectory()) {
		return DownloadServer.TRANSFER_MULTI_FAIL;
	    } else {
		return DownloadServer.TRANSFER_FAIL;
	    }
	}
    }

    private int countContent(File directory) {
	int count = 0;
	if (directory != null) {
	    File[] listFile = directory.listFiles();
	    if (listFile != null) {
		count = listFile.length;
		for (File f : listFile) {
		    if (f.isDirectory()) {
			count = count + countContent(f);
		    }
		}
	    }
	}
	return count;
    }

    @Override
    public int local2Local(String destLocal, String srcLocal,
	    boolean deleteAfter) {
	if (destLocal.startsWith(srcLocal)) {
	    if (destLocal.substring(srcLocal.length()).startsWith("/")) {
		return DownloadServer.TRANSFER_FAIL;
	    }
	}

	File destFile = new File(destLocal);
	File srcFile = new File(srcLocal);
	return localTransfer(destFile, srcFile, deleteAfter);
    }

    private int localTransfer(File destFile, File sourceFile,
	    boolean deleteAfter) {
	if ((sourceFile == null) || (destFile == null)) {
	    return DownloadServer.TRANSFER_FAIL;
	}

	if (sourceFile.isDirectory()) { // Directory
	    Boolean result = true;
	    String destPath = destFile.getAbsolutePath();
	    setLength(sourceFile.length());
	    showNotification(getTransferOngoingIndicator(),
		    sourceFile.getName(), TRANS_FLAG, TRANS_START, true,
		    destPath, sourceFile.getName(), "FileType");
	    try {
		destFile = createUniqueLocalTransferName(mContext, destFile,
			sourceFile.getName(), deleteAfter);
		if (destFile == null) {
		    SambaExplorer.log(TAG
			    + " mkdir failed because destFile is null");
		    result = false;
		} else {
		    result = destFile.mkdir();
		    SambaExplorer.log(TAG + " mkdir " + destFile.getName()
			    + " result is " + result);
		}
	    } catch (Exception e) {
		e.printStackTrace();
		SambaExplorer.log(TAG + " can not create directory ");
		result = false;
	    }
	    showNotification(getTransferCompleteIndicator(),
		    sourceFile.getName(), TRANS_FLAG, result ? TRANS_END
			    : TRANS_FAIL, true, destPath, sourceFile.getName(),
		    "FileType");
	    if (result == false) {
		return DownloadServer.TRANSFER_MULTI_FAIL;
	    }
	    File[] listFile = sourceFile.listFiles();
	    int result2;
	    int resultTotal = DownloadServer.TRANSFER_MULTI_COMPLETE;
	    if (listFile != null) {
		for (File f : listFile) {
		    result2 = localTransfer(destFile, f, deleteAfter);
		    if ((result2 == DownloadServer.TRANSFER_COMPLETE)
			    || (result2 == DownloadServer.TRANSFER_MULTI_COMPLETE)) {
			resultTotal = DownloadServer.TRANSFER_MULTI_COMPLETE;
		    } else {
			resultTotal = DownloadServer.TRANSFER_MULTI_FAIL;
		    }
		}
	    }
	    if (deleteAfter == true) {
		if (resultTotal == DownloadServer.TRANSFER_MULTI_COMPLETE) {
		    // if all file transfer completed successfully, then delete
		    // the source
		    // folder
		    Boolean delResult = sourceFile.delete();
		    if (delResult == false) {
			SambaExplorer
				.log(TAG
					+ " fail to delete source folder after move complete");
			resultTotal = DownloadServer.TRANSFER_MULTI_FAIL;
		    }
		}
	    }
	    return resultTotal;
	} else { // Single File
	    destFile = createUniqueLocalTransferName(mContext, destFile,
		    sourceFile.getName(), deleteAfter);
	    if (destFile != null) {
		boolean result3 = localTransferfile(destFile, sourceFile,
			deleteAfter);
		if (result3 == true) {
		    return DownloadServer.TRANSFER_COMPLETE;
		}
	    }
	    return DownloadServer.TRANSFER_FAIL;
	}
    }

    private boolean localTransferfile(File destFile, File sourceFile,
	    boolean deleteAfter) {
	boolean result = false;
	String localFileName = sourceFile.getName();
	setLength(sourceFile.length());
	showNotification(getTransferOngoingIndicator(), localFileName,
		TRANS_FLAG, TRANS_START, false, destFile.getParent(),
		localFileName, "FileType");
	FileInputStream input = null;
	FileOutputStream output = null;
	FileInputStream input1 = null;
	FileInputStream input2 = null;
	try {
	    input = new FileInputStream(sourceFile);
	    output = new FileOutputStream(destFile);
	    byte[] buffer = new byte[COPY_BUFFER_SIZE];
	    long length = sourceFile.length();
	    long tot = 0;
	    while (true) {
		int bytes = input.read(buffer);
		tot = tot + bytes;
		if (bytes <= 0) {
		    break;
		}
		output.write(buffer, 0, bytes);
		showNotification(getTransferOngoingIndicator(), localFileName,
			length, tot, false, destFile.getParent(),
			localFileName, "FileType");
	    }
	    input1 = new FileInputStream(destFile);
	    input2 = new FileInputStream(sourceFile);
	    if (inputStreamEquals(input2, input1)) {
		if (deleteAfter == true) { // move case
		    result = sourceFile.delete();
		    if (result == false) {
			SambaExplorer.log(TAG
				+ "Uploaded: but failed to delete source file");
		    }
		} else { // copy case complete
		    result = true;
		}
	    } else { // copy failed. erase the corrupt file
		SambaExplorer.log(TAG
			+ "Upload: copy failed, deleting destination file");
		boolean del = destFile.delete();
		if (del == false) {
		    SambaExplorer
			    .log(TAG
				    + "Upload: copy failed, failed to delete destination file");
		}
		result = false;
	    }
	    closeOutputStream(output);
	    closeInputStream(input);
	    closeInputStream(input1);
	    closeInputStream(input2);
	} catch (Exception e) {
	    SambaExplorer.log(TAG + " fail to transfer " + localFileName);
	    boolean del = destFile.delete(); // Make sure to cleanup the dest
					     // dir
	    if (del == false) {
		SambaExplorer
			.log(TAG
				+ "Upload: copy failed, failed to delete destination file");
	    }
	    closeOutputStream(output);
	    closeInputStream(input);
	    closeInputStream(input1);
	    closeInputStream(input2);
	    e.printStackTrace();
	    result = false;
	}

	showNotification(getTransferCompleteIndicator(), localFileName,
		TRANS_FLAG, result ? TRANS_END : TRANS_FAIL, false,
		destFile.getParent(), localFileName, "FileType");
	return result;
    }

    private File createUniqueLocalTransferName(Context context, File path,
	    String fileName, boolean deleteAfter) {
	File file = FileUtils.getFile(path, fileName);
	if (!file.exists()) {
	    return file;
	}
	if (deleteAfter == true) {
	    // If move use case and filename exists in destination, return null
	    // to go
	    // into error case
	    return null;
	}
	file = FileUtils.getFile(path,
		context.getString(R.string.copied_file_name, fileName));
	if (!file.exists()) {
	    return file;
	}
	int copyIndex = 2;
	while (copyIndex < 500) {
	    file = FileUtils.getFile(path, context.getString(
		    R.string.copied_file_name_2, copyIndex, fileName));
	    if (!file.exists()) {
		return file;
	    }
	    copyIndex++;
	}
	return null;
    }

    @Override
    public int local2Remote(String destRemote, String srcLocal,
	    boolean deleteAfter) {
	SambaExplorer.log(TAG + "local2Remote Transfer file from: " + srcLocal
		+ " to: " + destRemote);
	File localFile = new File(srcLocal);
	return local2RemoteTransfer(localFile, destRemote, deleteAfter);
    }

    private int local2RemoteTransfer(File localFile, String destRemote,
	    boolean deleteAfter) {
	if ((localFile == null) || (destRemote == null)) {
	    SambaExplorer
		    .log(TAG
			    + "Either localFile or destRemote is null in loca2RemoteTransfer");
	    return DownloadServer.TRANSFER_FAIL;
	}

	destRemote = fomatStrEndWithSlash(destRemote);
	setLength(localFile.length());
	if (localFile.isDirectory()) { // Directory
	    Boolean result = true;
	    SmbFile remoteDir = null;
	    showNotification(getTransferOngoingIndicator(),
		    localFile.getName(), TRANS_FLAG, TRANS_START, true,
		    destRemote, localFile.getName(), "FileType");
	    try {
		remoteDir = createUniqueRemoteTransferName(mContext,
			destRemote, localFile.getName(), deleteAfter);
		if (remoteDir == null) {
		    SambaExplorer
			    .log(TAG
				    + "unable to mkdirs the dest because remoteDir is null");
		    result = false;
		} else {
		    remoteDir.mkdirs(); // success if complete
		    SambaExplorer.log(TAG + " mkdirs "
			    + remoteDir.getCanonicalPath() + " result is "
			    + result);
		}
	    } catch (Exception e) {
		SambaExplorer.log(TAG + "unable to mkdirs the dest.");
		result = false;
	    }
	    showNotification(getTransferCompleteIndicator(),
		    localFile.getName(), TRANS_FLAG, result ? TRANS_END
			    : TRANS_FAIL, true, destRemote,
		    localFile.getName(), "FileType");
	    if (result == false) {
		return DownloadServer.TRANSFER_MULTI_FAIL;
	    }
	    File[] listFiles = localFile.listFiles();
	    int result2;
	    int resultTotal = DownloadServer.TRANSFER_MULTI_COMPLETE;
	    if (listFiles != null) {
		for (File f : listFiles) {
		    if (loadingCanceled()) {
			SambaExplorer.log(TAG
				+ "upload stopped at user request");
			break;
		    }
		    result2 = local2RemoteTransfer(f,
			    remoteDir.getCanonicalPath(), deleteAfter);
		    if ((result2 == DownloadServer.TRANSFER_COMPLETE)
			    || (result2 == DownloadServer.TRANSFER_MULTI_COMPLETE)) {
			resultTotal = DownloadServer.TRANSFER_MULTI_COMPLETE;
		    } else {
			resultTotal = DownloadServer.TRANSFER_MULTI_FAIL;
		    }
		}
	    }
	    if (deleteAfter == true) {
		if (resultTotal == DownloadServer.TRANSFER_MULTI_COMPLETE) {
		    // if all file transfer completed successfully, then delete
		    // the source
		    // folder
		    Boolean delResult = localFile.delete();
		    if (delResult == false) {
			SambaExplorer
				.log(TAG
					+ " fail to delete source folder after move complete");
			resultTotal = DownloadServer.TRANSFER_MULTI_FAIL;
		    }
		}
	    }
	    return resultTotal;
	} else { // Single File
	    SambaExplorer.log(TAG + "found: filename: [" + localFile.getName()
		    + "]");
	    SmbFile remoteFile = createUniqueRemoteTransferName(mContext,
		    destRemote, localFile.getName(), deleteAfter);
	    if (remoteFile != null) {
		boolean result3 = local2RemoteTransferFile(localFile,
			remoteFile, deleteAfter);
		if (result3 == true) {
		    return DownloadServer.TRANSFER_COMPLETE;
		}
	    }
	    return DownloadServer.TRANSFER_FAIL;
	}
    }

    private boolean local2RemoteTransferFile(File localFile,
	    SmbFile remoteSmbFile, boolean deleteAfter) {
	boolean result = false;
	if ((localFile == null) || (remoteSmbFile == null)) {
	    return result;
	}

	String localFileName = localFile.getName();
	setLength(localFile.length());
	showNotification(getTransferOngoingIndicator(), localFileName,
		TRANS_FLAG, TRANS_START, false, remoteSmbFile.getParent(),
		localFileName, "FileType");
	FileInputStream in = null;
	SmbFileOutputStream out = null;
	SmbFileInputStream input1 = null;
	FileInputStream input2 = null;
	try {
	    if (!remoteSmbFile.exists()) {
		SambaExplorer.log(TAG + "Uploading " + localFileName + " to "
			+ remoteSmbFile.getParent());
		in = new FileInputStream(localFile);
		out = new SmbFileOutputStream(remoteSmbFile);
		byte[] b = new byte[SMB_COPY_BUFFER_SIZE];
		long length = localFile.length();
		long tot = 0;
		int n;
		while ((n = in.read(b)) > 0) {
		    out.write(b, 0, n);
		    tot += n;
		    if (loadingCanceled()) {
			closeInputStream(in);
			closeOutputStream(out);
			remoteSmbFile.delete();
			result = false;
			throw new Exception("Cancel " + localFileName);
		    } else {
			showNotification(getTransferOngoingIndicator(),
				localFileName, length, tot, false,
				remoteSmbFile.getParent(), localFileName,
				"FileType");
		    }
		}
		closeOutputStream(out);
		closeInputStream(in);
		input1 = new SmbFileInputStream(remoteSmbFile);
		input2 = new FileInputStream(localFile);
		if (inputStreamEquals(input2, input1)) {
		    if (deleteAfter == true) { // move case
			result = localFile.delete();
			if (result == false) {
			    SambaExplorer
				    .log(TAG
					    + "Uploaded: but failed to delete source file");
			}
		    } else { // copy case complete
			result = true;
		    }
		} else { // copy failed. erase the corrupt file
		    SambaExplorer.log(TAG
			    + "Upload: copy failed, deleting destination file");
		    closeInputStream(input1);
		    remoteSmbFile.delete();
		    result = false;
		}
		closeInputStream(input1);
		closeInputStream(input2);
		SambaExplorer.log(TAG + "Uploaded: " + localFileName);
	    } else {
		SambaExplorer.log(TAG + "cowardly skipping file: "
			+ localFileName);
		showNotification("skipping ", localFileName, TRANS_FLAG,
			TRANS_IGNORE, false, remoteSmbFile.getParent(),
			localFileName, "FileType");
		result = false;
	    }
	} catch (SmbException e) {
	    closeInputStream(in);
	    closeOutputStream(out);
	    closeInputStream(input1);
	    closeInputStream(input2);
	    try {
		remoteSmbFile.delete(); // Delete partialy copied/moved file
	    } catch (SmbException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
	    SambaExplorer.log(TAG + "upload failed: " + e);
	    e.printStackTrace();
	    result = false;
	} catch (Exception e) {
	    closeInputStream(in);
	    closeOutputStream(out);
	    closeInputStream(input1);
	    closeInputStream(input2);
	    SambaExplorer.log(TAG + "upload failed: " + e);
	    e.printStackTrace();
	    try {
		remoteSmbFile.delete(); // Delete partialy copied/moved file
	    } catch (SmbException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
	    result = false;
	}

	showNotification(getTransferCompleteIndicator(), localFileName,
		TRANS_FLAG, result ? TRANS_END : TRANS_FAIL, false,
		remoteSmbFile.getParent(), localFileName, "FileType");
	return result;
    }

    private SmbFile createUniqueRemoteTransferName(Context context,
	    String path, String fileName, boolean deleteAfter) {
	SmbFile file;
	try {
	    file = new SmbFile(path + fileName, mDownloadUserAuth);
	    if (!file.exists()) {
		return file;
	    }
	} catch (Exception e) {
	    SambaExplorer.log(TAG + "error: " + e);
	    e.printStackTrace();
	    return null;
	}
	if (deleteAfter == true) {
	    // If move use case and filename exists in destination, return null
	    // to go
	    // into error case
	    return null;
	}
	try {
	    file = new SmbFile(path
		    + context.getString(R.string.copied_file_name, fileName),
		    mDownloadUserAuth);
	    if (!file.exists()) {
		return file;
	    }
	} catch (Exception e) {
	    SambaExplorer.log(TAG + "error: " + e);
	    e.printStackTrace();
	    return null;
	}
	int copyIndex = 2;
	while (copyIndex < 500) {
	    try {
		file = new SmbFile(path
			+ context.getString(R.string.copied_file_name_2,
				copyIndex, fileName), mDownloadUserAuth);
		if (!file.exists()) {
		    return file;
		}
	    } catch (Exception e) {
		SambaExplorer.log(TAG + "error: " + e);
		e.printStackTrace();
		return null;
	    }
	    copyIndex++;
	}
	return null;
    }

    @Override
    public int remote2Local(String destLocal, String srcRemote,
	    boolean deleteAfter) {
	SambaExplorer.log(TAG + "remote2Local Transfer file from: " + srcRemote
		+ " to: " + destLocal);
	try {
	    SmbFile file = new SmbFile(srcRemote, mDownloadUserAuth);
	    if (file != null) {
		return remote2LocalTransfer(file, destLocal, deleteAfter);
	    }
	} catch (Exception e) {
	    SambaExplorer.log(TAG + "error: " + e);
	    e.printStackTrace();
	}
	return DownloadServer.TRANSFER_FAIL;
    }

    private int remote2LocalTransfer(SmbFile srcSmbFile, String destFilePath,
	    boolean deleteAfter) {
	if (srcSmbFile == null) {
	    SambaExplorer.log(TAG + "SmbFile null in remote2LocalCopy");
	    return DownloadServer.TRANSFER_FAIL;
	}
	destFilePath = fomatStrEndWithSlash(destFilePath);
	try {
	    if (srcSmbFile.isDirectory()) {
		Boolean result = true;
		SmbFile file = new SmbFile(srcSmbFile.getCanonicalPath() + "/",
			mDownloadUserAuth);
		SmbFile[] listSmbfiles = file.listFiles();
		File dir = null;
		setLength(srcSmbFile.length());
		showNotification(getTransferOngoingIndicator(), file.getName(),
			TRANS_FLAG, TRANS_START, true, destFilePath,
			file.getName(), "FileType");
		try {
		    dir = new File(destFilePath);
		    dir = createUniqueLocalTransferName(mContext, dir,
			    file.getName(), deleteAfter);
		    if (dir == null) {
			SambaExplorer.log(TAG
				+ " mkdir failed because dir is null");
			result = false;
		    } else {
			result = dir.mkdir();
			SambaExplorer.log(TAG + " mkdir "
				+ dir.getAbsolutePath() + " result is "
				+ result);
		    }
		} catch (Exception e) {
		    SambaExplorer.log(TAG + "unable to mkdirs the dest.");
		    result = false;
		}
		showNotification(getTransferCompleteIndicator(),
			file.getName(), TRANS_FLAG, result ? TRANS_END
				: TRANS_FAIL, true, destFilePath,
			file.getName(), "FileType");
		if (result == false) {
		    return DownloadServer.TRANSFER_MULTI_FAIL;
		}
		int result2;
		int resultTotal = DownloadServer.TRANSFER_MULTI_COMPLETE;
		if (listSmbfiles != null) {
		    for (SmbFile f : listSmbfiles) {
			if (loadingCanceled()) {
			    SambaExplorer.log(TAG
				    + "fileTransfer stopped at user request");
			    break;
			}
			result2 = remote2LocalTransfer(f,
				dir.getAbsolutePath(), deleteAfter);
			if ((result2 == DownloadServer.TRANSFER_COMPLETE)
				|| (result2 == DownloadServer.TRANSFER_MULTI_COMPLETE)) {
			    resultTotal = DownloadServer.TRANSFER_MULTI_COMPLETE;
			} else {
			    resultTotal = DownloadServer.TRANSFER_MULTI_FAIL;
			}
		    }
		}
		if (deleteAfter == true) {
		    if (resultTotal == DownloadServer.TRANSFER_MULTI_COMPLETE) {
			// if all file transfer completed successfully, then
			// delete the
			// source folder
			file.delete();
			SambaExplorer
				.log(TAG
					+ " delete source folder after move complete, assume success");
		    }
		}
		return resultTotal;
	    } else {
		SambaExplorer.log(TAG + "found: filename: ["
			+ srcSmbFile.getName() + "]");
		File destFile = new File(destFilePath);
		destFile = createUniqueLocalTransferName(mContext, destFile,
			srcSmbFile.getName(), deleteAfter);
		if (destFile != null) {
		    boolean result3 = remote2LocalTransferFile(srcSmbFile,
			    destFile, deleteAfter);
		    if (result3 == true) {
			return DownloadServer.TRANSFER_COMPLETE;
		    }
		}
		return DownloadServer.TRANSFER_FAIL;
	    }
	} catch (SmbException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return DownloadServer.TRANSFER_FAIL;
	} catch (MalformedURLException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return DownloadServer.TRANSFER_FAIL;
	}
    }

    private boolean remote2LocalTransferFile(SmbFile srcSmbFile, File destFile,
	    boolean deleteAfter) {
	boolean result = false;
	String srcFileName = srcSmbFile.getName();
	showNotification(getTransferOngoingIndicator(), srcFileName,
		TRANS_FLAG, TRANS_START, false, destFile.getParent(),
		srcFileName, "FileType");
	SmbFileInputStream in = null;
	FileOutputStream out = null;
	FileInputStream input1 = null;
	SmbFileInputStream input2 = null;
	try {
	    SambaExplorer.log(TAG + "downloading " + srcFileName + " to "
		    + destFile.getPath());
	    in = new SmbFileInputStream(srcSmbFile);
	    out = new FileOutputStream(destFile);

	    byte[] b = new byte[SMB_COPY_BUFFER_SIZE];
	    long length = srcSmbFile.length();
	    long tot = 0;
	    int n;
	    while ((n = in.read(b)) > 0) {
		out.write(b, 0, n);
		tot += n;
		if (loadingCanceled()) {
		    in.close();
		    out.close();
		    if (destFile.delete() == false) {
			SambaExplorer.log(TAG + "delete failed "
				+ destFile.getName());
		    }
		    result = false;
		    throw new Exception("Cancel " + srcFileName);
		} else {
		    showNotification(getTransferOngoingIndicator(),
			    srcFileName, length, tot, false,
			    destFile.getParent(), srcFileName, "FileType");
		}
	    }
	    closeInputStream(in);
	    closeOutputStream(out);
	    input1 = new FileInputStream(destFile);
	    input2 = new SmbFileInputStream(srcSmbFile);
	    if (inputStreamEquals(input2, input1)) {
		if (deleteAfter == true) { // move case
		    closeInputStream(input2);
		    srcSmbFile.delete(); // doesn't return result so assume
					 // succeeded
		}
		result = true;
	    } else { // copy failed. erase the corrupt file
		SambaExplorer.log(TAG
			+ "Upload: copy failed, deleting destination file");
		boolean del = destFile.delete();
		if (del == false) {
		    SambaExplorer
			    .log(TAG
				    + "Upload: copy failed, failed to delete destination file");
		}
		result = false;
	    }
	    closeInputStream(input1);
	    closeInputStream(input2);
	    SambaExplorer.log(TAG + "download: " + srcFileName);
	    result = true;
	} catch (SmbException e) {
	    closeInputStream(in);
	    closeOutputStream(out);
	    closeInputStream(input1);
	    closeInputStream(input2);
	    SambaExplorer.log(TAG + "downloading failed SmbException : " + e);
	    boolean del = destFile.delete();// Delete partialy copied/moved file
	    if (del == false) {
		SambaExplorer
			.log(TAG
				+ "Upload: copy failed, failed to delete destination file");
	    }
	    e.printStackTrace();
	    result = false;
	} catch (Exception e) {
	    closeInputStream(in);
	    closeOutputStream(out);
	    closeInputStream(input1);
	    closeInputStream(input2);
	    SambaExplorer.log(TAG + "downloading failed Exception: " + e);
	    boolean del = destFile.delete();// Delete partialy copied/moved file
	    if (del == false) {
		SambaExplorer
			.log(TAG
				+ "Upload: copy failed, failed to delete destination file");
	    }
	    e.printStackTrace();
	    result = false;
	}

	showNotification(getTransferCompleteIndicator(), srcFileName,
		TRANS_FLAG, result ? TRANS_END : TRANS_FAIL, false,
		destFile.getParent(), srcFileName, "FileType");
	return result;
    }

    @Override
    public int remote2Remote(String destRemote, String srcRemote,
	    boolean deleteAfter) {
	// TODO Auto-generated method stub
	if (destRemote.startsWith(srcRemote)) {
	    if (destRemote.substring(srcRemote.length()).startsWith("/")) {
		return DownloadServer.TRANSFER_FAIL;
	    }
	}
	destRemote = fomatStrEndWithSlash(destRemote);
	SambaExplorer.log(TAG + "remote2Remote: " + " From: " + srcRemote);
	SambaExplorer.log(TAG + "remote2Remote: " + " to: " + destRemote);
	SmbFile from;
	try {
	    from = new SmbFile(srcRemote, mDownloadUserAuth);
	    // SambaExplorer.log(TAG + "sambaServer Permission: " + srcRemote +
	    // "" + from.getPermission());
	    if (from == null) {
		SambaExplorer.log(TAG + "from file is null in remote2Remote");
		return DownloadServer.TRANSFER_FAIL;
	    }

	    if (from.isDirectory()) {
		from = new SmbFile(srcRemote + "/", mDownloadUserAuth);
		if (from == null) {
		    SambaExplorer.log(TAG
			    + "2nd from file is null in remote2Remote");
		    return DownloadServer.TRANSFER_MULTI_FAIL;
		}
		Boolean result = true;
		SmbFile remoteDir = null;
		SmbFile[] files = from.listFiles();
		showNotification(getTransferOngoingIndicator(), from.getName(),
			TRANS_FLAG, TRANS_START, true, destRemote,
			from.getName(), "FileType");
		try {
		    remoteDir = createUniqueRemoteTransferName(mContext,
			    destRemote, from.getName(), deleteAfter);
		    if (remoteDir == null) {
			SambaExplorer.log(TAG
				+ "remote folder name unavailable");
			result = false;
		    } else {
			SambaExplorer.log(TAG + "sambaServer Permission: "
				+ remoteDir.getCanonicalPath()
				+ remoteDir.getPermission());
			remoteDir.mkdirs(); // success if complete
		    }
		} catch (MalformedURLException e) {
		    // showNotification("Failed to use destination, SD card inserted and writable?",
		    // 1, 1, 1, 1);
		    SambaExplorer.log(TAG + "unable to mkdirs the dest.");
		    result = false;
		} catch (SmbException e) {
		    SambaExplorer.log(TAG + "unable to mkdirs the dest.");
		    result = false;
		}
		showNotification(getTransferCompleteIndicator(),
			from.getName(), TRANS_FLAG, result ? TRANS_END
				: TRANS_FAIL, true, destRemote, from.getName(),
			"FileType");
		if (result == false) {
		    return DownloadServer.TRANSFER_MULTI_FAIL;
		}
		int result2;
		int resultTotal = DownloadServer.TRANSFER_MULTI_COMPLETE;
		if (files != null) {
		    for (int i = 0; i < files.length; i++) {
			if (loadingCanceled()) {
			    // showNotification("Copy stopped at user request",
			    // 1,
			    // 1, 1, 1);
			    return DownloadServer.TRANSFER_MULTI_FAIL;
			}
			result2 = remote2Remote(remoteDir.getCanonicalPath(),
				srcRemote + "/" + files[i].getName(),
				deleteAfter);
			if ((result2 == DownloadServer.TRANSFER_COMPLETE)
				|| (result2 == DownloadServer.TRANSFER_MULTI_COMPLETE)) {
			    resultTotal = DownloadServer.TRANSFER_MULTI_COMPLETE;
			} else {
			    resultTotal = DownloadServer.TRANSFER_MULTI_FAIL;
			}
		    }
		}
		if (deleteAfter == true) {
		    if (resultTotal == DownloadServer.TRANSFER_MULTI_COMPLETE) {
			// if all file transfer completed successfully, then
			// delete the
			// source folder
			from.delete();
			SambaExplorer
				.log(TAG
					+ " delete source folder after move complete, assume success");
		    }
		}
		return resultTotal;
	    } else {
		boolean result3 = false;
		String srcFileName = from.getName();
		SmbFile to = createUniqueRemoteTransferName(mContext,
			destRemote, srcFileName, deleteAfter);
		if (to == null) {
		    return DownloadServer.TRANSFER_FAIL;
		}
		showNotification(getTransferOngoingIndicator(), srcFileName,
			TRANS_FLAG, TRANS_START, true, destRemote, srcFileName,
			"FileType");
		SambaExplorer
			.log(TAG + "copy: filename: [" + srcFileName + "]");
		try {
		    if (deleteAfter == true) { // Move case
			from.renameTo(to);
		    } else { // Copy case
			     // long t0 = System.currentTimeMillis();
			from.copyTo(to);
			// long t = System.currentTimeMillis() - t0;
			// SambaExplorer.log(TAG + "remote2Remote copyTo cost: "
			// +
			// t/1000 + "sec");
		    }
		    if (!to.canRead()) {
			to.delete(); // Transfer failed.
			throw new Exception(
				"Copied remote file can not be read.");
		    }
		    result3 = true;
		} catch (Exception e) {
		    SambaExplorer.log(TAG + "copy: failed filename: ["
			    + to.getName() + "]");
		    e.printStackTrace();
		    result3 = false;
		}
		showNotification(getTransferCompleteIndicator(), srcFileName,
			TRANS_FLAG, result3 ? TRANS_END : TRANS_FAIL, true,
			destRemote, srcFileName, "FileType");
		return result3 ? DownloadServer.TRANSFER_COMPLETE
			: DownloadServer.TRANSFER_FAIL;
	    }
	} catch (SmbException e) {
	    SambaExplorer.log(TAG + "remote2Remote:SmbException " + e);
	} catch (MalformedURLException e) {
	    SambaExplorer.log(TAG + "remote2Remote:MalformedURLException " + e);
	} catch (Exception e) {
	    SambaExplorer.log(TAG + "remote2Remote:Exception " + e);
	}
	return DownloadServer.TRANSFER_FAIL;
    }

    private void closeInputStream(InputStream in) {
	try {
	    if (in != null) {
		in.close();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private void closeOutputStream(OutputStream out) {
	try {
	    if (out != null) {
		out.close();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static boolean inputStreamEquals(InputStream is1, InputStream is2) {
	byte buff1[] = new byte[BASE_SIZE];
	byte buff2[] = new byte[BASE_SIZE];

	if (is1 == is2) {
	    return true;
	}
	if (is1 == null && is2 == null) {
	    return true;
	}
	if (is1 == null || is2 == null) {
	    return false;
	}
	try {
	    int read1 = -1;
	    int read2 = -1;

	    do {
		int offset1 = 0;
		while (offset1 < BASE_SIZE
			&& (read1 = is1.read(buff1, offset1, BASE_SIZE
				- offset1)) >= 0) {
		    offset1 += read1;
		}

		int offset2 = 0;
		while (offset2 < BASE_SIZE
			&& (read2 = is2.read(buff2, offset2, BASE_SIZE
				- offset2)) >= 0) {
		    offset2 += read2;
		}
		if (offset1 != offset2) {
		    return false;
		}
		if (offset1 != BASE_SIZE) {
		    Arrays.fill(buff1, offset1, BASE_SIZE, (byte) 0);
		    Arrays.fill(buff2, offset2, BASE_SIZE, (byte) 0);
		}
		if (!Arrays.equals(buff1, buff2)) {
		    return false;
		}
	    } while (read1 >= 0 && read2 >= 0);
	    if (read1 < 0 && read2 < 0) {
		return true;// both at EOF
	    }
	    return false;

	} catch (Exception ei) {
	    return false;
	}
    }

}
