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
// Author         Date       Tracking  Description
// ************** ********** ********  ********************** //
// Stephen Dickey 03/01/2009 1.0       Initial Version
//
// ********************************************************** //
package com.motorola.motoapr.service;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

// Class to deal with installed package lists.
// the creation of the list should only happen once
// the first time APR is launched as an app or as a service.
// upon creation, this list should not be modifiable.
public class APRInstalledPackages {
	
	final static String TAG = "APRInstalledPackages";
	
	static Context mContext = null;
		
	APRInstalledPackages( Context context )
	{
		mContext = context;
				
		return;
	}

	/**
	 * Attempt to match the app name with the 
	 * entry in the database.
	 * @param app_name
	 */
	public static boolean IsFactoryInstalledApp( String app_name )
	{
		return ( !IsUserInstalledApp( app_name ) ) ;
	}
	
	/**
	 * Attempt to match the app name with the 
	 * entry in the database.
	 * @param app_name
	 */
	public static boolean IsUserInstalledApp( String app_name )
	{
		boolean return_result = false;
		int index_of_colon = 0;
		String new_app_name;
		ApplicationInfo app_info;
				
		PackageManager pm = mContext.getPackageManager();
		
		try {
			// check if the app_name includes activity name
			if ( (index_of_colon = app_name.indexOf(":")) > 0 )
			{
				new_app_name = app_name.substring(0,index_of_colon);
				APRDebug.APRLog( TAG, "new app name:" + new_app_name );

				app_info = pm.getApplicationInfo(new_app_name, 0);
			}
			else
			{
				app_info = pm.getApplicationInfo(app_name, 0);
			}
			
			if ( ( app_info.flags & ( ApplicationInfo.FLAG_SYSTEM | 
					                  ApplicationInfo.FLAG_UPDATED_SYSTEM_APP ) ) == 0 )
			{
				return_result = true;
			}
		}
		catch ( NameNotFoundException e )
		{
			APRDebug.APRLog( TAG, "Application Lookup Failed:" + app_name );
		}
		finally
		{
			APRDebug.APRLog( TAG, "Application is UserInstalled " + app_name + ":" + return_result );
		}
		
		return( return_result ); 
	}
}
