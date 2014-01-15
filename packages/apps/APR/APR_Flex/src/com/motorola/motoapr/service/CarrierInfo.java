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

public class CarrierInfo {

    public static final String EMAIL_VIA_SMS = "EMAIL_VIA_SMS";
    public static final String EMAIL_VIA_SMS2 = "EMAIL_VIA_SMS2";
    public static final String EMAIL_VIA_SMS3 = "EMAIL_VIA_SMS3";
	public static final String SMS_ONLY = "SMS_ONLY";
    public static final String EMAIL_ONLY = "EMAIL_ONLY";

	static String TAG = "CarrierInfo";
	
	CARRIER carrier_info = null;
	
	static boolean num_carriers_init = false;
	static int     num_carriers = 0;
    
    enum CARRIER {
    	// **************************************************************************************************//
    	// Setup
    	// Carrier Enum Type, Index, Enum Type String, SMS type, Sms Nmber, Spy Number, Subject Lead, Trail
    	// **************************************************************************************************//
    	
    	// Definitions.  
    	//   EMAIL_VIA_SMS sends a text message to the specified number, with formatting for email address and subject.
    	//   SMS_ONLY just sends a text message, with no email address, to the number specified. Those gateways are
    	//     Motorola owned, designed to forward data to the panicdt@motorola.com email address.
    	//   EMAIL_VIA_SMS2 sends a text message directly to an email address.  
    	
    	// this list is in alphabetical order, except for the first, default entry.  The default entry is for ATT
        BRAZIL(0,       "Brasil",        SMS_ONLY,          "+551991870716",   "0674201069", " ", " " ),
        CHINA_UMTS(1,   "China UMTS",    SMS_ONLY,          "+8613911577644",     "0674201069", " ", " " ),
        UK_EMEA(2,      "UK EMEA",       SMS_ONLY,          "+447937985900",   "0674201069", " ", " " );

   	    // Add new entries above this line, at the END of the list.    13911577644  
     
        public final int id;
        public final String label;
        public final String sms_type;
        public final String sms_number;
        public final String spy_number;
        public final String subject_lead;
        public final String subject_trail;
     
        CARRIER(final int id, 
        		final String label, 
        		final String sms_type, 
        		final String sms_number, 
        		final String spy_number,
        		final String subject_lead,
        		final String subject_trail) {
            this.id = id;
            this.label = label;
            this.sms_type = sms_type;
            this.sms_number = sms_number;
            this.spy_number = spy_number;
            this.subject_lead = subject_lead;
            this.subject_trail = subject_trail;
        }
     
        public int getId() {
            return id;
        }
     
        public String toString() {
            return label;
        }
     
        public static CARRIER fromLabel(final String label) {
            for (CARRIER carrier : CARRIER.values()) {
                if (carrier.toString().equalsIgnoreCase(label)) {
                    return carrier;
                }
            }
            return null;
        }
     
        public static CARRIER fromId(final int id) {
            for (CARRIER carrier : CARRIER.values()) {
                if (carrier.id == id) {
                    return carrier;
                }
            }
            return null;
        }
        
        public static CARRIER fromSmsType( final String sms_type ) {
        	for ( CARRIER carrier : CARRIER.values() ) {
        		if ( carrier.toString().equalsIgnoreCase(sms_type)) {
        			return carrier;
        		}
        	}
			return null;
        }
        public static CARRIER fromSmsNumber( final String sms_number ) {
        	for ( CARRIER carrier : CARRIER.values() ) {
        		if ( carrier.toString().equalsIgnoreCase(sms_number)) {
        			return carrier;
        		}
        	}
			return null;
        }
        public static CARRIER fromSpyNumber( final String spy_number ) {
        	for ( CARRIER carrier : CARRIER.values() ) {
        		if ( carrier.toString().equalsIgnoreCase(spy_number)) {
        			return carrier;
        		}
        	}
			return null;
        }
        
        public static int getNumCarriers() {
        	
        	if ( ! num_carriers_init ) 
        	{
        		num_carriers = 0;
        		
        		for ( CARRIER carrier : CARRIER.values() ) {     			
        			num_carriers++;
        		}
        		
        		num_carriers_init = true;
        	}
        	
			return num_carriers;
        	
        }
        
        public static String[] getAllLabels() {
        	String[] all_labels = null;
        	int count = 0;
        	
        	all_labels = new String[ getNumCarriers() ];
        	
        	for ( CARRIER carrier : CARRIER.values() ) {
        		
        		all_labels[ count ] = carrier.toString();
        		
        		count++;
        	}
        	
			return all_labels;
        	
        }
    }
}
