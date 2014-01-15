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

import android.app.Activity;
import android.telephony.SmsManager;
import android.content.Context;
import com.motorola.motoapr.service.CarrierInfo.CARRIER;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.motorola.motoapr.service.SMTPSender;

public class SMS extends Activity {
    
    static String TAG = "APRMessaging";
    
    //static String SMS_SUBJECT_TEXT = "APR";
    static String SMS_SUBJECT_TEXT = "A";
    
    public final static int ACTIVITY_INVOKE = 0;
    public final static int MAX_SMS_LENGTH=158;
    
    boolean spy_enabled = false;
    
    Context mContext = null;
    TelephonyManager      telephony_mngr = null;
    APRPhoneStateListener phone_state_listener = null;      
    
    SMS(Context context) {
    	super();  
	mContext = context;  	
    }
    
    //---sends an SMS message to another device---
	public boolean sendSMS( CARRIER carrier, String phoneNumber, String message, String message_type )
    {   
		boolean sms_success = false;
		
		APRDebug.APRLog ( TAG, "sendSMS" );
		
		if ( APRPhoneStateListener.isPhoneInService() )
			{
			if ( carrier.sms_type.compareTo(CarrierInfo.EMAIL_VIA_SMS) == 0 ) {
				sendEmailViaSMS( carrier, message, message_type );
			}
			if ( carrier.sms_type.compareTo(CarrierInfo.EMAIL_VIA_SMS2) == 0 ) {
				sendEmailViaSMS2( carrier, message, message_type );
			}
			if ( carrier.sms_type.compareTo(CarrierInfo.EMAIL_VIA_SMS3) == 0 ) {
				sendEmailViaSMS3( carrier, message, message_type );
			}
			if ( carrier.sms_type.compareTo(CarrierInfo.SMS_ONLY) == 0  ) {
				sendSMSOnly( carrier, message, message_type );
			}
			if ( carrier.sms_type.compareTo(CarrierInfo.EMAIL_ONLY) == 0  ) {
			        sendEmailOnly( carrier, message, message_type );
			}

			sms_success = true;
		} else {
			//To avoid APR from not receiving in-service message, we should re-listen the message
			telephony_mngr = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);  
			phone_state_listener = APRPhoneStateListener.getInstance();
			telephony_mngr.listen( phone_state_listener, PhoneStateListener.LISTEN_NONE );
			telephony_mngr.listen( phone_state_listener, PhoneStateListener.LISTEN_SERVICE_STATE );
  		}
		
		return( sms_success );
    }
	
     
	private void sendEmailViaSMS( CARRIER carrier, String message, String message_type )
	{		
		String sms_number = carrier.sms_number;
		String Subject = carrier.subject_lead +  SMS_SUBJECT_TEXT + carrier.subject_trail;
				
		APRDebug.APRLog ( TAG, "sendEmailViaSMS" );
		
		String EmailAddress = "panicdt@motorola.com";
		EmailSMS( sms_number, EmailAddress, Subject, message );
		
		if ( spy_enabled )
		{
			String spy_email_address = carrier.spy_number;
			EmailSMS( sms_number, spy_email_address, Subject, message );
		}
    }
	
   // generalized function for emailing via sms.
	private void EmailSMS( String SMSNumber, String Address, String Subject, String Message )
    {
    	String temp_message = new String();
    	String message_to_send = new String();
    	
		APRDebug.APRLog ( TAG, "EmailSMS" );
		
		temp_message = Address + Subject + Message;
    	
		// Duplicate the string, but either truncate it, or leave it alone, based
		// upon the message string length.  Never Exceed Max Message Length.
		if ( ( temp_message.length() ) > MAX_SMS_LENGTH )
	    	{
				message_to_send = temp_message.substring( 0, MAX_SMS_LENGTH );
		    }
		else
			{
				message_to_send = new String();
				message_to_send = temp_message;
		   	}

		APRDebug.APRLog( TAG, "SMS Number: " + SMSNumber );
		APRDebug.APRLog( TAG, "Message:    " + message_to_send );
		
    	try {
			
	    SmsManager sms = SmsManager.getDefault();
		
	    if ( sms!= null )
    		{
	    		sms.sendTextMessage( SMSNumber, null, message_to_send, null, null);
    		}
	    else
    		{
		  		APRDebug.APRLog( TAG, "Failed To Get Default SMS Manager");
    		}
    	}
    	catch ( Exception e ) {
	    	APRDebug.APRLog( TAG, "Message Send Failure" );
			APRDebug.APRDebugStack(e);
    	}
    }

	// there is a subject, but it is an empty space
	private void sendEmailViaSMS3( CARRIER carrier, String message, String message_type )
	{		
		String sms_number = carrier.sms_number;
		String Subject = " ";
		
		APRDebug.APRLog ( TAG, "sendEmailViaSMS3" );

		String EmailAddress = "panicdt@motorola.com";
		EmailSMS( sms_number, EmailAddress, Subject, message );
		
		if ( spy_enabled )
		{
			String spy_email_address = carrier.spy_number;
			EmailSMS( sms_number, spy_email_address, Subject, message );
		}
    }

	private void sendEmailViaSMS2( CARRIER carrier, String message, String message_type )
	{		
		String sms_number = carrier.sms_number;
		String Subject = "";
		
		APRDebug.APRLog ( TAG, "sendEmailViaSMS2" );

				
		String EmailAddress = "panicdt@motorola.com";
		EmailSMS2( sms_number, EmailAddress, Subject, message );
		
		if ( spy_enabled )
		{
			String spy_email_address = carrier.spy_number;
			EmailSMS2( sms_number, spy_email_address, Subject, message );
		}
    }

    // generalized function for emailing via sms.
    private void EmailSMS2( String SMSNumber, String Address, String Subject, String Message )
    {
    	String temp_message = new String();
    	String message_to_send = new String();
    	
		APRDebug.APRLog ( TAG, "EmailSMS2" );

    	
    	temp_message = Message;
    	
		// Duplicate the string, but either truncate it, or leave it alone, based
		// upon the message string length.  Never Exceed Max Message Length.
		if ( ( temp_message.length() + Address.length()) > MAX_SMS_LENGTH )
	    	{
				message_to_send = temp_message.substring( 0, MAX_SMS_LENGTH - Address.length() );
		    }
		else
			{
				message_to_send = new String();
				message_to_send = temp_message;
		   	}

		APRDebug.APRLog( TAG, "SMS Number: " + Address );
		APRDebug.APRLog( TAG, "Message:    " + message_to_send );
		
    	try {
			
	    SmsManager sms = SmsManager.getDefault();
		
	    if ( sms!= null )
    		{
	    		sms.sendTextMessage( Address, null, message_to_send, null, null);
    		}
	    else
    		{
		  		APRDebug.APRLog( TAG, "Failed To Get Default SMS Manager");
    		}
    	}
    	catch ( Exception e ) {
	    	APRDebug.APRLog( TAG, "Message Send Failure" );
			APRDebug.APRDebugStack(e);
    	}
    }
    

	private void sendSMSOnly( CARRIER carrier, String message, String message_type )
	{
		String sms_number = APRPreferences.APRGetCarrierNumber();
		String spy_number = carrier.spy_number;
		String message_to_send = null;

		if(sms_number== null)
		{
			APRDebug.APRLog ( TAG, "sendSMSOnly sms_number is null " );
		   	sms_number = carrier.sms_number;
		}
		
		APRDebug.APRLog ( TAG, "sendSMSOnly sms_number = " + sms_number);

		
		// Duplicate the string, but either truncate it, or leave it alone, based
		// upon the message string length.  Never Exceed Max Message Length.
		if ( ( message.length() ) > MAX_SMS_LENGTH)
	    	{
				message_to_send = new String();
				message_to_send = message.substring( 0, MAX_SMS_LENGTH);
		    }
		else
			{
				message_to_send = new String();
				message_to_send = message;
		   	}
			
	    APRDebug.APRLog( TAG, "Message length" + message_to_send.length());

    	try {
	    SmsManager sms = SmsManager.getDefault();
	    
	    if ( sms!= null )
    		{
		    	sms.sendTextMessage( sms_number, null, message_to_send, null, null);
		    	
		    	if ( spy_enabled ) 
		    	{
		    		sms.sendTextMessage( spy_number, null, message_to_send, null, null);
		    	}
    		}
	    else
    		{
		    APRDebug.APRLog( TAG, "Failed To Get Default SMS Manager");
    		}
    	}
    	catch ( Exception e ) {
	    	APRDebug.APRLog( TAG, "SMS Message Send Failure" );
			APRDebug.APRDebugStack(e);
    	}		
    }



    private void sendEmailOnly( CARRIER carrier, String message, String message_type )
    {
        String smtpServerAddress = carrier.sms_number;
        String Subject = carrier.subject_lead +  SMS_SUBJECT_TEXT + carrier.subject_trail;
	
        APRDebug.APRLog ( TAG, "sendEmailOnly" );
	
        String phoneNumber = getPhoneNumber();
        String senderServerName = getServerName();
        String senderEmailAddress = phoneNumber + "@" + senderServerName;
        String receiverEmailAddress = "panicdt@motorola.com";
	
        try {
            SMTPSender.sendMail( smtpServerAddress, senderEmailAddress, receiverEmailAddress, Subject, message );
        } catch (Exception e) {
            APRDebug.APRLog( TAG, "Email Send Failure" );
            APRDebug.APRDebugStack(e);
        }
	
        if ( spy_enabled )
        {
        	String spyEmailAddress = carrier.spy_number;
        	try {
        		SMTPSender.sendMail( smtpServerAddress, senderEmailAddress, spyEmailAddress, Subject, message );
        	} catch (Exception e) {
                APRDebug.APRLog( TAG, "Email Send Failure" );
                APRDebug.APRDebugStack(e);
            }
        }
    }

    private String getPhoneNumber() {
        String number = TelephonyManager.getDefault().getLine1Number();
        if(!TextUtils.isEmpty(number)){
            return number;
        }
        else{
            return "NoNumber";
        }
    }

    private String getServerName() {
        String number = TelephonyManager.getDefault().getLine1Number();
        if(APRPreferences.APRPrefsGetCarrierName().equalsIgnoreCase("SKTELECOM_KR")){
            return "nate.com";
        }
        else{
            return "motorola.com";
        }
    }

}


