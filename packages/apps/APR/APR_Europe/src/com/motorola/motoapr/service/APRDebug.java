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

import android.os.RemoteException;
import android.util.Log;

public class APRDebug {
	
	// this is the only place we can reliably control debug
	//   state.  since we don't need a UI control for this 
	//   there are no functions to change the state.
	static private boolean debug_normal = false;
		
	static public void APRDebugStack( Exception e ) {
		if ( debug_normal )
		{
			Log.e( "CRASH STACK", e.getMessage() );
			Log.e( "CRASH STACK", e.getLocalizedMessage() );
			e.printStackTrace();
		}
	}

	public static void APRDebugStack( RemoteException e) {
			APRDebugStack( (Exception)e);
	}

	static public boolean APRDebugGetUIDebugState() {
		return( false );
	}

	public static void APRDebugStack( VerifyError e) {
		if ( debug_normal )
		{
			Log.e( "CRASH STACK", e.getMessage() );
			Log.e( "CRASH STACK", e.getLocalizedMessage() );
			e.printStackTrace();
		}	
	}
	
	public static void APRLog( String tag, String DebugText )
	{
		if ( debug_normal )
		{
			Log.d( tag, DebugText );
		}
	}

	public static void APRLog( String tag, String string, byte[] raw_message ) 
	{
		if ( debug_normal )
		{
			String log = new String();
			
			for ( int i = 0; i < raw_message.length; i ++ )
			{
				log = log + String.format( "%02x", raw_message[i] );
			}
		}
	}
}
