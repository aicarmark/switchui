// ********************************************************** //
// PROJECT:     APR (Automatic Panic Recording)
// DESCRIPTION: 
//   The purpose of APR is to gather the panics in the device
//   and record statics about those panics to a centralized
//   server, for automated tracking of the quality of a program.
//   To achieve this, several types of messages are required to
//   be sent at particular intervals.  This package is responsible
//   for sending the data in the correct format, at the right 
//   intervals.
// ********************************************************** //
// Change History
// ********************************************************** //
// Author          Date      Tracking  Description
// ************** ********** ********  ********************** //
// Stephen Dickey 10/31/2009  1.0      Initial Version Supporting
//   wlsd10                              ftp.
//
// ********************************************************** //
package com.motorola.motoapr.service;

import android.content.Context;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.lang.Integer;


// class to build a package for ftp transfer.  
//   the package is essentially a list of files, and a unique given 
//   name for the package.  depending upon phone capabilities
//   this may tar/gzip the package prior to sending, or it may
//   simply upload the package.  the second is preferable, as it
//   requires no additional disk space on the device. the advantage
//   of the first is reduced bandwidth.

public class APRBuildFTPPackage {
	
	static String TAG = "APRBuildFTPPackage";

	static HashSet<String> files = null;
	static HashSet<String> contents = null;
	Context mContext = null;
	APRftp ftp_service = null;
	String dummy_file = "/data/done.txt";
	static String backup_dir = "/data/apr_backup";
	static String dummy_text = "done";
	
	// list if files to always send if they exist.
	String add_file_list[] = {
			"/data/logs.txt",
			"/data/user.log",
			"/data/messages",
			"/data/radio_logs.txt",
			"/etc/oshwt-version.txt",
	};

	APRBuildFTPPackage( Context context )
	{
		mContext = context;

		// create the backup directory unless it exists.
		if( !(new File(backup_dir)).exists() )
		{
			(new File(backup_dir)).mkdirs();	
		}
		
		// create the dummy file which has dummy text.
		try{
			FileOutputStream dummy = new FileOutputStream(new File(dummy_file));
			dummy.write( dummy_text.getBytes(), 0, dummy_text.length() );
			dummy.close();
		} catch (IOException e) {
		}

		ftp_service = new APRftp( context );
	}
	
	/**
	 * Add a file to the package, for later ftp upload.
	 * @param file_name
	 * @param content
	 * @return
	 */
	static public boolean add_file( String file_name, String content )
	{
		boolean return_result = false;
		
		try {			
			if ( files == null ) files = new HashSet<String>();
			if ( contents == null ) contents = new HashSet<String>();
			
			if ( file_name != null )
			{
				// remove the new String hack when everything works great.
				files.add( new String( file_name ) ); 
			}
			
			if ( content != null )
			{
				// remove the new String hack when everything works great.
				if ( contents.add( new String( content ) ) )
				{
					return_result = true;
				}
			}
			
		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Failed to add to package." );
		} finally {
			if ( !return_result )
			{
				APRDebug.APRLog( TAG, "Failed to add to package." );
			}
		}
		
		return return_result;
	}
	
	public void report_send()
	{
		Calendar cal = Calendar.getInstance();
		String package_name = null;
		String package_list = null;
		String date_time = null;

		try {
			// create a package list
			// add package list to the package
			// build/send the package

			// time format for package name: YYYYMMDD_HHMMSS (24hour format)
			date_time = String.format("%1$tY%1$tm%1$td_%1$tH%1$tM%1$tS", cal);

			// String format of the package name: SW_Version-BuildCRC-Barcode-Date_time
			package_name = new String( APRMessage.CurrentBaselabel() + "-" + 
				Integer.toHexString(APRMessage.BuildCRC()).toUpperCase() + "-" + 
				APRMessage.CurrentBarcode() + "-" + date_time );

			package_list = BuildPackageList( mContext.getFilesDir() + "/package_list.txt" );
						
			if( package_list != null )
			{
				build_final_package( package_list, package_name );
				
				// send the package list.
				ftp_service.APRftp_send_package( package_name );
			}

			// send old packages that haven't been uploaded
			File dir = new File(backup_dir);
			String[] children = dir.list();

			for (int i=0; i<children.length; i++) {
				APRDebug.APRLog( TAG, "Sending old package: " + children[i] );
				ftp_service.APRftp_send_package( children[i] );
			}

		} catch ( Exception e ) {
			APRDebug.APRLog( TAG, "Failed Package Upload." );
		} finally {
			
		}
	}
	
	// add extra log files, and build the final package we're going to send.
	private void build_final_package( String package_list, String package_name ) {
		
		for ( int i=0; i< add_file_list.length; i++ ) {
			File fp = new File( add_file_list[i] );
			
			if ( fp.exists() ) {
				files.add( add_file_list[i] );
			}			
		}
		
		files.add( package_list );

		// Create backup package.
		CreateBackupPackage( package_name, files );

		for ( int i=0; i< add_file_list.length; i++ ) {
			File fp = new File( add_file_list[i] );
			
			if ( fp.exists() ) {
				fp.delete();
			}
		}
	}
	
	private String BuildPackageList( String output_filename ) 
	{
		String return_result = null;
		OutputStream out = null;
		String this_entry = null;
		File old_file = null;
		
		try {
			if( contents != null )
			{
				// delete old package list if it exists.
				old_file = new File( output_filename );
				if( old_file.exists() )
				{
					old_file.delete();
				}

				out = new FileOutputStream( output_filename );
		
				// take the contents set, iterate through, and
				//   create a text file with those contents, adding 
				//   that text file to the package list.
			
				Iterator<String> c_iterator = contents.iterator();
			
				// iterate through the contents list.
				while( c_iterator.hasNext() )
				{
					this_entry = c_iterator.next();
					this_entry += "\n" ;

					out.write( this_entry.getBytes(), 0, this_entry.length());
				}
			
				return_result = output_filename;
			}
		
		} catch (FileNotFoundException e) {
			APRDebug.APRLog( TAG, "File Not Found." );
		} catch (IOException e) {
			APRDebug.APRLog( TAG, "Write Failure." );
		} catch ( Exception e) {
			APRDebug.APRLog( TAG, "General Failure." );
		} finally {
			
			try {
				if( contents != null )
				{
					out.close();
				}
			} catch (IOException e) {
				APRDebug.APRLog( TAG, "Close Failure." );
			}
			
			// wipe the contents set.  no longer needed.
			contents = null;
		}
		
		return( return_result );
	}
	
	// Create backup files to be uploaded to FTP server with directory information.
	public void CreateBackupPackage(final String package_name, final HashSet<String> files) {

		String package_dir = new String( backup_dir + "/" + package_name );
		Iterator<String> f_iterator = files.iterator();
		String file_to_send = null;
		int index_of_file = 0;

		APRDebug.APRLog( TAG, "CreateBackupPackage: " + package_dir );

		// create the package directory unless it exists.
		if( !(new File(package_dir)).exists() )
		{
			(new File(package_dir)).mkdirs();	
		}

		while ( f_iterator.hasNext() )
		{
			file_to_send = f_iterator.next();
			index_of_file = file_to_send.lastIndexOf( "/" );
			        				
			// copy each file to the package directory.
            try{
				copy_file( file_to_send, new String( package_dir + file_to_send.substring(index_of_file) ) );
				APRDebug.APRLog( TAG, "CreateBackupPackage: " + new String( package_dir + file_to_send.substring(index_of_file) ) );
            } catch (IOException e) {
            }

		}
	}

	// Copies src file to dst file.
	// If the dst file does not exist, it is created
	void copy_file(String src_path, String dst_path) throws IOException {
		File src = new File(src_path);
		File dst = new File(dst_path);
		FileInputStream in = new FileInputStream(src);
		FileOutputStream out = new FileOutputStream(dst);
    
		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	// Remove files in the backup package.
	public static void RemoveBackupPackage(final String package_name) {

		String package_dir = new String( backup_dir + "/" + package_name );
		APRDebug.APRLog( TAG, "RemoveBackupPackage: " + package_dir );
		File dir = new File( package_dir );

		if ( dir != null ) {
			delete_dir(dir);
		}
	}
	
	// Remove all Backup Packages: to be done when we 
	//   want to clear out the phone.
	public static void ClearBackupPackages( ) {

		// send old packages that haven't been uploaded
		File dir = new File(backup_dir);

		if ( dir != null ) {
			String[] children = dir.list();

			if ( children != null ) {
				for (int i=0; i<children.length; i++) {
					APRDebug.APRLog( TAG, "Deleting Old Package: " + children[i] );
					RemoveBackupPackage( children[i] );
				}
			}
		}
	}

	// Deletes all files and sub-directories under dir.
	// Returns true if all deletions were successful.
	// If a deletion fails, the method stops attempting to delete and returns false.
	private static boolean delete_dir(File dir) {

		boolean result = false;
		
		if ( dir != null ) {
			if (dir.isDirectory()) {
				String[] children = dir.list();				
				if ( children != null ) {
					for (int i=0; i<children.length; i++) {
						APRDebug.APRLog( TAG, "delete_dir: " + children[i] );
						boolean success = delete_dir(new File(dir, children[i]));
						if (!success) {
							return false;
						}
					}
				}
			}
			
			// The directory is now empty or it is a file, so delete it
			result = dir.delete();
		}

		return result; 
	}

}
