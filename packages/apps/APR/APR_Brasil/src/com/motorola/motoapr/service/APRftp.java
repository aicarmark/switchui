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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class APRftp {
	
    static String TAG = "APRftp";
    
    Context mContext = null;
    
    //String server_name = "ftp.motoapr1.com";
    //String username = "motoapr1@ymail.com@motoapr1.com";
    //String password = "9@815642KSM";
    String server_name = "144.190.204.202";
    String username = "aprupload";
    String password = "aprupload2010";
    String backup_dir = "/data/apr_backup";
    String dummy_file = "/data/done.txt";
    
    static boolean data_connection_state = false;
    
    APRftp( Context context )
    {
    	mContext = context;
    }
    
    static boolean getDataConnectionState()
    {
    	return( data_connection_state );
    }
        
    // send a single file.  handles general network state issues.
    public boolean APRFtpFile( String src_filename, String dst_filename )
    {
    	boolean return_result = false;
    	
        try
        {
        	ConnectivityManager cm = (ConnectivityManager) 
        	   mContext.getSystemService( Context.CONNECTIVITY_SERVICE );
    		
        	NetworkInfo ni = cm.getActiveNetworkInfo();
        	
        	if ( ni.isAvailable() && ni.isConnected() )
        	{
        		try {
					
        			FileInputStream input = new FileInputStream( src_filename );
        							
        			return_result = APRftp_send( null, input, dst_filename );
        			
				    APRDebug.APRLog( TAG, "APRFtpFile return_result: " + return_result );
        			
        			// close the input file.
        			input.close();
        			
        		} catch ( Exception e )
        		{
        			APRDebug.APRLog( TAG, "FTP Failure..." );
        		}
        	}
        
        } catch (Exception e ) {
        	APRDebug.APRLog( TAG, "Connect Failed." );
        }
        finally {
        	data_connection_state = return_result;
        }
        
		return( return_result );
    }
    
    // send a single file.  handles general network state issues.
    public boolean APRftp_send_dummy( final String package_name )
    {
    	boolean return_result = false;
	    String src_filename = dummy_file;
	    String dst_filename = "done.txt";
    	
        try
        {
        	ConnectivityManager cm = (ConnectivityManager) 
        	   mContext.getSystemService( Context.CONNECTIVITY_SERVICE );
    		
        	NetworkInfo ni = cm.getActiveNetworkInfo();
        	
        	if ( ni.isAvailable() && ni.isConnected() )
        	{
        		try {
					
        			FileInputStream input = new FileInputStream( src_filename );
        							
				    APRDebug.APRLog( TAG, "APRftp_send_dummy sending done.txt" );
        			return_result = APRftp_send( package_name, input, dst_filename );
				    APRDebug.APRLog( TAG, "APRftp_send_dummy return_result: " + return_result );

        			// close the input file.
        			input.close();

        		} catch ( Exception e )
        		{
        			APRDebug.APRLog( TAG, "FTP Failure..." );
        		}
        	}
        
        } catch (Exception e ) {
        	APRDebug.APRLog( TAG, "Connect Failed." );
        }
        finally {
        	data_connection_state = return_result;
        }
        
		return( return_result );
    }

    // this needs to run off in another thread.  there's a lot of work that will be going on here.
	public void APRftp_send_package(final String package_name) { 		
		new Thread( new Runnable() 
		{
			public void run() 
			{
				boolean send_success = false;

				try {
			        	ConnectivityManager cm = (ConnectivityManager) 
			        	   mContext.getSystemService( Context.CONNECTIVITY_SERVICE );
			    		
			        	NetworkInfo ni = cm.getActiveNetworkInfo();
			        	
			        	if ( ni.isAvailable() && ni.isConnected() )
			        	{
			        		try {
		        				// send files in the package directory.
			            		send_success = APRftp_send_files_in_package( package_name );

			        		} catch ( Exception e )
			        		{
			        			send_success = false;
			        			data_connection_state = false;
			        			APRDebug.APRLog( TAG, "FTP Failure..." );
			        		}
			        		
			        	} // end if we have a network connection
			        
			        } catch (Exception e ) {
			        	APRDebug.APRLog( TAG, "Connect Failed." );
			        }
			        finally {

			        	// signal back to the main thread that we're done.
			        	//   need to do this, to enable the next time around
			        	//   and/or to clear out the "files" list. 
					if( send_success ) {
						if( APRftp_send_dummy( package_name ) ) {
				        	APRBuildFTPPackage.RemoveBackupPackage( package_name );
				        	data_connection_state = true;
						} else {
							data_connection_state = false;
						}
				       }
			        } // end try/catch/finally code.
			} // end run function 
			
		} //end new thread
		).start();
	}

	/**
	 * send files in a package directory.
	 */
	private boolean APRftp_send_files_in_package( String package_name ) {

		String package_dir = new String( backup_dir + "/" + package_name );
		File dir = new File(package_dir);
		String[] children = dir.list();
    	boolean return_result = false;
    	boolean setup_correct = true;
    	String file_to_send = null;
    	String dummy_text = null;
    	int i = 0;
    	
    	if (children == null) {
    		// Either dir does not exist or is not a directory
      		APRDebug.APRLog( TAG, "Package directory does not exist" );
        	return( return_result );
    	}

    	FTPClient ftp = new FTPClient(); 
        
    	APRDebug.APRLog( TAG, "Doing Connect..." );
    	
    	try {
    		// connect to the server
    		ftp.connect( server_name );
        	
    		APRDebug.APRLog( TAG, "Connected to Server..." );
        	
    		int reply = ftp.getReplyCode();
        	
    		if ( FTPReply.isPositiveCompletion( reply )) {
    			// login to the server
    			if ( ftp.login( username, password ) ) {
    				// specify the transfer type
    				ftp.setFileType(FTP.BINARY_FILE_TYPE);
        			
    				// to handle firewalls...
    				ftp.enterLocalPassiveMode();
        			
    				if ( package_name != null ) {
    					
    					// make a directory. it fails if the directory exists.
    					ftp.makeDirectory( package_name );

    					// change working directory.
    					if ( !ftp.changeWorkingDirectory( package_name ) ) {
    						setup_correct = false;
    					}
    				}

    				if ( setup_correct ) try {
    					for (i=0; i<children.length; i++) {
    						// Get filename of file or directory
    						file_to_send = new String( package_dir + "/" + children[i] );
    						APRDebug.APRLog( TAG, "APRftp_send_package: " + file_to_send );

    						// send each file.
    						FileInputStream input = new FileInputStream( file_to_send );

    						// store the input file to the remote file name        			
    						if ( !ftp.storeFile( children[i], input) ) {
    							APRDebug.APRLog( TAG, "FTP StoreFile Failed..." );
    							return_result = false;
    							break;
    						} else {
    					 		// success
    							return_result = true;
    						}

    						// close the input file.
    						input.close();
    					}

    				} catch ( Exception e ) {
    					return_result = false;
    					APRDebug.APRLog( TAG, "FTP Failure..." );
    				} finally {
    					if ( return_result == true ) {
    						dummy_text = Integer.toString(i);

    						// create the dummy file which has dummy text.
    						try {
    							FileOutputStream dummy = new FileOutputStream(new File(dummy_file));
    							dummy.write( dummy_text.getBytes(), 0, dummy_text.length() );
    							dummy.close();
    						} catch (IOException e) {
    							// Ignored exception.
    						}
    					}
    				}
    				ftp.logout();
    			} else {
    				APRDebug.APRLog( TAG, "FTP Login Failed." );    					
    			}
    		} else {
    			APRDebug.APRLog( TAG, "Server Refused Connection..." );
    		}

    	} catch ( IOException e ) {
    		if ( ftp.isConnected()) {
    			try {
    				ftp.disconnect();
    			} catch ( IOException f ) {
    				// failed to disconnect.
    			}
    		}
    		APRDebug.APRLog( TAG, "Connection Failed" );
    	}

    	return( return_result );
	}
    
	/**
	 * send a single file. assumes the network connection is UP.
	 *   changes directory to directory (or creates and changes to it ).
	 */
    private boolean APRftp_send( String directory, FileInputStream input, String dst_filename ) {
    	
    	boolean return_result = false;
    	boolean setup_correct = true;
    	
        FTPClient ftp = new FTPClient(); 
        
    	APRDebug.APRLog( TAG, "Doing Connect..." );
    	
    	try 
    	{
        	// connect to the server
        	ftp.connect( server_name );
        	
        	APRDebug.APRLog( TAG, "Connected to Server..." );
        	
        	int reply = ftp.getReplyCode();
        	
        	if ( FTPReply.isPositiveCompletion( reply )) 
        	{
            		// login to the server
    			if ( ftp.login( username, password ) )
    			{
        			// specify the transfer type
        			ftp.setFileType(FTP.BINARY_FILE_TYPE);
        			
        			// to handle firewalls...
        			ftp.enterLocalPassiveMode();
        			
        			if ( directory != null )
        			{
        				// make a directory. it fails if the directory exists.
            			ftp.makeDirectory( directory );

            			// change working directory.
        				if ( !ftp.changeWorkingDirectory( directory ) ) {
        					setup_correct = false;
        				}
        			}
        			       
        			if ( setup_correct ) {
        			
            			// store the input file to the remote file name        			
            			if ( !ftp.storeFile( dst_filename, input) )
            			{
            				APRDebug.APRLog( TAG, "FTP StoreFile Failed..." );
            			}
            			else
            			{
    	        			// success
           	 				return_result = true;
            			}
        			}
        			ftp.logout();
    			}
    			else
    			{
    				APRDebug.APRLog( TAG, "FTP Login Failed." );    					
    			}
        	}
        	else
        	{
        		APRDebug.APRLog( TAG, "Server Refused Connection..." );
        	}
    	
    	} catch ( IOException e )
    	{
    		if ( ftp.isConnected())
    		{
    			try
    			{
    				ftp.disconnect();
    				
    			} catch ( IOException f )
    			{
    				// failed to disconnect.
    			}
    		}
    		
    		APRDebug.APRLog( TAG, "Connection Failed" );
    	}
        
        return( return_result );
    }
    
}
