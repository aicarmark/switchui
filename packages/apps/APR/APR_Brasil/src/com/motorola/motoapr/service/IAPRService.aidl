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

interface IAPRService {
    // Store a crash for management by the APR Service
    //   for eventual sending to Server.
    boolean APRStoreTestCrash( String crash ); 

    // Send an APR Email Directly.  Bypass the APR Service.
    //   for direct sending to the Server.
    boolean APRSendEmail( in String address, in String subject, in String message );
    
    // Send out any pending messages queued by this Service.
    //   this is necessary to offload timer events to the 
    //   service thread level.
     boolean APRSendStoredEmail();
     
     // Toggle the APRRunningSpeed for Debug Purposes.
     boolean APRToggleRunSpeed();
     
     // Get the RunningSpeed for Debug Purposes
     boolean APRGetRunSpeed();
     
     // Set the carrier preference
     boolean APRSetCarrierId( int carrier_index );
     
     // Get the carrier preference
     int APRGetCarrierId();
     
     // Enabled/Disabled State.
     int APRSetEnabledState( boolean enabled_state );
     
     // Get Operating State.
     boolean APRGetEnabledState();
     
     // Rescan the Phone's Information. 
     boolean APRRescanPhoneInfo();
}


