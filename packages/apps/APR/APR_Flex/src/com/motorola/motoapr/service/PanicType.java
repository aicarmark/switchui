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

// These are the crash types we're prepared to handle. 

public class PanicType {

	static boolean num_panic_types_init = false; 
	static int     num_panic_types = 0;
    
    enum PANIC_ID {	

    	    // this is a panic id that reflects a normal poweron.  it is not a failure.
    	    //   0xA0080500 is an IGNORED PANIC on the APR Web Site.
   			NORMAL_POWERDOWN( 0, "NORMAL_POWERDOWN", false, 0xA0080500, "/data/donotscan" ),

    	    // the panic id concept is really a combination of several pieces of information.
    	    //   we do this to greatly simply panic identification.  Instead of our APR code
    	    //   having knowledge about panic_ids that could be out there, we reduce complexity
    	    //   greatly by shoving the panics into these simple categories.

   			// this is really just a scanner in preparation of the other panic types.
   			BOOT_INFO(              1, "UNKNOWN_CRASH_TYPE",     true,  0xA0080500, "/proc/bootinfo"),
    	    KERNEL_CRASH(           2, "KERNEL_CRASH",           true,  0xA0080520, "/data/kpanic" ),
    	    KERNEL_APANIC(          3, "KERNEL_APANIC",          true,  0xA0080520, "/data/dontpanic/apanic_console" ),
    	    MODEM_CRASH(            4, "MODEM_CRASH",            true,  0xA0080501, "/data/panic/apr" ),
    	    MODEM_CRASH1(           5, "MODEM_CRASH1",           true,  0xA0080501, "/data/bp_panic" ),
    	    MODEM_CRASH2(           6, "MODEM_CRASH2",           true,  0xA0080501, "/data/bp_panic/apr" ),
    	    MODEM_CRASH3(           7, "MODEM_CRASH3",           true,  0xA0080501, "/data/panicreports" ),
   			LINUX_USER_SPACE_CRASH( 8, "LINUX_USER_SPACE_CRASH", false, 0xA0080523, "/data/tombstones" ),
   			JAVA_APP_CRASH(         9, "JAVA_APP_CRASH",         false, 0xA0080524, "/data/anr/traces.txt" ),
   			POWER_CUT(             10, "POWER_CUT",              true,  0xA0080525, "/data/donotscan" ),
   			AP_WATCHDOG(           11, "AP_WATCHDOG",            true,  0xA0080526, "/data/donotscan" ),
   			CPCAP_WATCHDOG(        12, "CPCAP_WATCHDOG",         true,  0xA0080527, "/data/donotscan" ),
   			KERNEL_APANIC_ND(      13, "KERNEL_APANIC_NO_DATA",  true,  0xA0080528, "/data/donotscan" ),
   			JAVA_APP_CRASH_ANR(    14, "JAVA_APP_CRASH_ANR",     false, 0xA0080529, "/data/donotscan" ),
   			JAVA_APP_EXCEPTION(    15, "JAVA_APP_EXCEPTION",     false, 0xA008052A, "/data/donotscan" ),
   			BUGREPORT(             16, "BUGREPORT",              false, 0xA008052B, "/data/donotscan" ),
   			DROPBOX_CRASH(         17, "DROPBOXCRASH",           false, 0xA0080524, "/data/system/dropbox" );

   	    // Notice: /data/panic_report.txt is not panic information.  it is other non-panic-data.
   		//   do not use /data/panic_report.txt for uptime, nor flex version, nor software version.

   	    // Add new entries above this line, at the END of the list.

        public final int index;
        public final String label;
        public boolean critical_panic; 

        // this is the panic id used by the LV APR server.
        public final int id;  
        public final String dir_location;
   
 	    // Add new entries above this line, at the END of the list.

        PANIC_ID(final int index, 
        		 final String label,
        		 final boolean critical_panic,
        		 final int id,
        		 final String dir_location ) { 
            this.index = index;
            this.label = label;
            this.critical_panic = critical_panic;
            this.id    = id;
            this.dir_location = dir_location;
        }

        public int getIndex() {
            return index;
        }
     
        public String toString() {
            return label;
        }
     
        public static PANIC_ID fromLabel(final String label) {
            for (PANIC_ID panic_id : PANIC_ID.values()) {
                if (panic_id.toString().equalsIgnoreCase(label)) {
                    return panic_id;
                }
            }
            return null;
        }
     
        public static PANIC_ID fromId(final int id) {
            for (PANIC_ID panic_id : PANIC_ID.values()) {
                if (panic_id.id == id) {
                    return panic_id;
                }
            }
            return ( NORMAL_POWERDOWN );
        }
        
        public static PANIC_ID fromPanicType( final String panic_type ) {
        	for ( PANIC_ID panic_id : PANIC_ID.values() ) {
        		if ( panic_id.toString().equalsIgnoreCase(panic_type)) {
        			return panic_id;
        		}
        	}
			return ( NORMAL_POWERDOWN );
        }
        
       public static int getNumPanicTypes() {
        	
        	if ( ! num_panic_types_init ) 
        	{
        		num_panic_types = 0;
        		
        		for ( PANIC_ID panic_id : PANIC_ID.values() ) {     			
        			num_panic_types++;
        		}
        		
        		num_panic_types_init = true;
        	}
        	
			return num_panic_types;
        	
        }
        
        public static String[] getAllLabels() {
        	String[] all_labels = null;
        	int count = 0;
        	
        	all_labels = new String[ getNumPanicTypes() ];
        	
        	for ( PANIC_ID panic_id : PANIC_ID.values() ) {
        		
        		all_labels[ count ] = panic_id.toString();
        		
        		count++;
        	}
        	
			return all_labels;
        	
        }


    }
}
