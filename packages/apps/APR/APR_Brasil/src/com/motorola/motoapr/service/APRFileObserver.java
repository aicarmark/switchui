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
// w42725         11/13/2009  1.0      Initial Version.
//
// ********************************************************** //

package com.motorola.motoapr.service;

import com.motorola.motoapr.service.PanicType.PANIC_ID;

import android.os.FileObserver;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class APRFileObserver {

	static String TAG = "APRFileObserver";

	private static final long INTERVAL = 10000; // 10 seconds
	private static final long ONE_SECOND = 1000;

	static Context mContext = null;
	static APRFileObserverDirectory tracesObserver = null;
	static APRFileObserverDirectory tombstonesmObserver = null;
	static APRFileObserverDirectory modemObserver = null;
	static APRFileObserverDirectory dropboxObserver = null;

	static String traces_dir = "/data/anr/";
	static String tombstones_dir = "/data/tombstones/";
	static String modem_panics_dir = "/data/panicreports/";
	static String dropbox_dir = "/data/system/dropbox";
	static long previous_elapsed = 0;

	private final static int FLAGS = FileObserver.MODIFY;

	public APRFileObserver(Context context) {
		mContext = context;

        tracesObserver = APRFileObserverStartWatching( tracesObserver, traces_dir );
        tombstonesmObserver = APRFileObserverStartWatching( tombstonesmObserver, tombstones_dir );
        modemObserver = APRFileObserverStartWatching( modemObserver, modem_panics_dir );
        dropboxObserver = APRFileObserverStartWatching( dropboxObserver, dropbox_dir );
	}

	// simplified routine to create (if needed), and watch (if needed), a dir.
	private APRFileObserverDirectory APRFileObserverStartWatching( APRFileObserverDirectory observer, String path ) {

		if( !(new File(path)).exists() ) {
			(new File(path)).mkdirs();
		}

		APRDebug.APRLog( TAG, "New Watcher: " + path );
		observer = new APRFileObserverDirectory(path);
		observer.startWatching();
		
		APRDebug.APRLog( TAG, "Watching: " + path );
		
		return( observer );
	}

    static public void APRFileObserverHit(Context mContext, String path)
    {
      	APRDebug.APRLog( TAG, "Previous Time: " + previous_elapsed );
      	APRDebug.APRLog( TAG, "Current Time: " + android.os.SystemClock.elapsedRealtime() );
      	
    	if( previous_elapsed + INTERVAL < android.os.SystemClock.elapsedRealtime() )
    	{
    		// a logcat log should now be in the file system's package.
            long nextUpdate = android.os.SystemClock.elapsedRealtime() + 5*ONE_SECOND;

    		previous_elapsed = nextUpdate;

        	APRDebug.APRLog( TAG, "current time :" + nextUpdate );

        	// Create an intent that will cause the service to scan
        	Intent FileObserverIntent = new Intent(APRService.ACTION_FILE_OBSERVER_HIT);
        	FileObserverIntent.setClass(mContext, APRService.class);

        	PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, FileObserverIntent, 0);

        	// Schedule alarm, and force the device awake for this update with this intent
        	AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        	alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextUpdate, pendingIntent);
        	
        	// get the logcat and radio logs, when we have a failure.
    		//GetLogcatLog();
    		//GetRadioLog();

		//check if the path includes 'tombstone'
		if( path.contains("tombstone") ) {
    			APRDebug.APRLog( TAG, "Tombstone detected." );
    			//GetUserLog();
    			//GetMessages();
		}
    	} else {
    		APRDebug.APRLog( TAG, "Waiting before taking another logcat." );
    	}
    }

    static boolean logcat_log_running = false;
 
	static private void GetLogcatLog() {

		if ( logcat_log_running ) return;
		logcat_log_running = true;
		
		// taking a bug report takes too long.
		//   this must be handled in a distinct thread.
		new Thread( new Runnable() {
			
			public void run () {
				
				try {
			      	APRDebug.APRLog( TAG, "LOGCAT Start: " + android.os.SystemClock.elapsedRealtime() );

					String[] err = new String[1];
					String[] output = new String[1];
					String[] env = new String[1];
					env[0] = "PATH=" + System.getenv("PATH");
					execCommand(LOGCAT_LOG,env,err,output);
				} catch ( Exception e ) {
					// try to ensure we catch all exceptions
				} finally {
					APRDebug.APRLog( TAG, "LOGCAT Complete: " + android.os.SystemClock.elapsedRealtime() );
					logcat_log_running = false;
				}
			}
		} ).start();

		APRDebug.APRLog( TAG, "Logcat Log Started" );
	}

	static boolean userlog_running = false;

	static private void GetUserLog() {

		if ( userlog_running ) return;
		userlog_running = true;

		//   this must be handled in a distinct thread.
		new Thread( new Runnable() {
			
			public void run () {
				
				try {
			      	APRDebug.APRLog( TAG, "USER_LOG Start: " + android.os.SystemClock.elapsedRealtime() );

					String[] err = new String[1];
					String[] output = new String[1];
					String[] env = new String[1];
					env[0] = "PATH=" + System.getenv("PATH");
					execCommand(USER_LOG,env,err,output);
				} catch ( Exception e ) {
					// try to ensure we catch all exceptions
				} finally {
					APRDebug.APRLog( TAG, "USER_LOG Complete: " + android.os.SystemClock.elapsedRealtime() );
					userlog_running = false;
				}
			}	
		} ).start();

		APRDebug.APRLog( TAG, "Grabbing user.log Started" );
	}

	static boolean radio_log = false;
	static private void GetRadioLog() {
		
		if ( radio_log ) return;
		radio_log = true;

		//   this must be handled in a distinct thread.
		new Thread( new Runnable() {
			
			public void run () {
				
				try {
			      	APRDebug.APRLog( TAG, "RADIO Log Start: " + android.os.SystemClock.elapsedRealtime() );

					String[] err = new String[1];
					String[] output = new String[1];
					String[] env = new String[1];
					env[0] = "PATH=" + System.getenv("PATH");
					execCommand(RADIO_LOG,env,err,output);
				} catch ( Exception e ) {
					// try to ensure we catch all exceptions
				} finally {
					APRDebug.APRLog( TAG, "RADIO Log Complete: " + android.os.SystemClock.elapsedRealtime() );
					radio_log = false;
				}
			}	
		} ).start();

		APRDebug.APRLog( TAG, "Grabbing messages Started" );
	}
	
	static boolean get_message_running = false;
	
	static private void GetMessages() {

		if ( get_message_running ) return;
		get_message_running = true;	

		//   this must be handled in a distinct thread.
		new Thread( new Runnable() {

			public void run () {

				try {
			      	APRDebug.APRLog( TAG, "MESSAGES Start: " + android.os.SystemClock.elapsedRealtime() );

					APRDebug.APRLog(TAG, "Tombstone changes detected: ");

					String[] err = new String[1];
					String[] output = new String[1];
					String[] env = new String[1];
					env[0] = "PATH=" + System.getenv("PATH");
					execCommand(MESSAGES,env,err,output);
				} catch ( Exception e ) {
					// try to ensure we catch all exceptions
				} finally {
					APRDebug.APRLog( TAG, "MESSAGES Complete: " + android.os.SystemClock.elapsedRealtime() );
					get_message_running = false;
				}
			}	
		} ).start();

		APRDebug.APRLog( TAG, "Grabbing messages Started" );
	}

	
	static boolean bugreport_running = false;

	static public void APRScanBugReport() {
		
		if ( bugreport_running == false ) {

			bugreport_running = true;

			APRDebug.APRLog(TAG, "Bugreport Requested, spawning thread.");

			// taking a bug report takes too long.
			//   this must be handled in a distinct thread.
			new Thread( new Runnable() {
				
				public void run () {
					
					String bugreport_file = "/data/bugreport.txt";
											
					APRDebug.APRLog(TAG, "Bugreport Thread Running....");

					try {
						APRBuildFTPPackage    ftp_package_list = null; 

						ftp_package_list   = new APRBuildFTPPackage( mContext );

						String[] err = new String[1];
						String[] output = new String[1];
						String[] env = new String[1];
						env[0] = "PATH=" + System.getenv("PATH");
						
						APRDebug.APRLog(TAG, "Executing Bugreport....");

						execCommand(BUGREPORT,env,err,output);
						
						// add any new files here, if they exist.
						APRBuildFTPPackage.add_file( bugreport_file, "BugReport: " );

						ftp_package_list.report_send();

						File new_log = new File( bugreport_file );
						
						// delete log file if it exists.
						if( new_log.exists() ) {
							new_log.delete();
						}
						
						// increment the bugreport count, after a successful record.
						APRPreferences.APRPrefsCrashCount( PANIC_ID.BUGREPORT );
						
					} catch ( Exception e ) {
						APRDebug.APRDebugStack(e);
					} finally {
						APRDebug.APRLog(TAG, "Bugreport Thread is Completed.");
						
						bugreport_running = false;
					}
				}
			} ).start();
		} else {
			APRDebug.APRLog(TAG, "Bugreport in Progress, Not Restarting.");
		}
	}

	static private boolean execCommand(String[] command, String[] env, 
			   String[] err, String[] result) {
		boolean success = true;

		try {
			String commandOutput;        

			Process p = Runtime.getRuntime().exec(command, env);
			BufferedReader input =
				new BufferedReader(new InputStreamReader(p.getErrorStream()), 1024);

			while ((commandOutput = input.readLine()) != null) {
				if (err[0] == null) {
					err[0] = new String();
				}

				err[0] += commandOutput + "\n";
			}

			input.close();

			input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);

			while ((commandOutput = input.readLine()) != null) {
				if (result[0] == null) {
					result[0] = new String();
				}

				result[0] += commandOutput + "\n";
			}    	        

			input.close(); 

		} catch (Exception e) {
			success = false;
		}

		return success;
	}

	// logcat.  log all debug messages (*:D), dump and exit (-d),
	//   and specify the output format (-v time)
	private static final String LOGCAT_LOG[] = {
			"sh",
			"-c", 
			"logcat -d -v threadtime *:D > /data/logs.txt"
	};
	
	// logcat.  log all debug messages (*:D), dump and exit (-d),
	//   and specify the output format (-v time)
	private static final String RADIO_LOG[] = {
			"sh",
			"-c", 
			"logcat -d -v threadtime -b radio > /data/radio_logs.txt"
	};

	// grab last 1000 line of /var/log/user.log
	private static final String USER_LOG[] = {
			"sh",
			"-c", 
			"tail -1000 /var/log/user.log > /data/user.log"
	};

	// grab last 1000 line of /var/log/messages
	private static final String MESSAGES[] = {
			"sh",
			"-c", 
			"tail -1000 /var/log/messages > /data/messages"
	};

	// logcat.  log all debug messages (*:D), dump and exit (-d),
	//   and specify the output format (-v time)
	private static final String BUGREPORT[] = {
			"sh",
			"-c", 
			"bugreport > /data/bugreport.txt"
	};

	private class APRFileObserverDirectory extends FileObserver {

		boolean thread_running = false;
		String path_watched = null;
		
    	APRFileObserverDirectory(String path) {
			super(path, FLAGS);
    		APRDebug.APRLog( TAG, "APRFileObserverDirectory is constructed" );
		}

		@Override
		public void onEvent(int event, String path) {

			// note: we may not be allowed to use debug statements
			//   in this code.
			try {
	        	path_watched = new String( path );
	        	
	        	switch ( event ) {
	        	case MODIFY:
	        	case CREATE:
					APRFileObserverHit(mContext, path);
	        		break;
	        	default:
	        		break;
	        	}
			} catch ( Exception e ) {
			}
        	
        	// do not detect, kill or restart ANY Observables here.
		}
	}
} 
