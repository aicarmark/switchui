package com.motorola.motoapr.service;

public class SpeedInfo {

    public static final String EMAIL_VIA_SMS = "EMAIL_VIA_SMS";
    public static final String EMAIL_VIA_SMS2 = "EMAIL_VIA_SMS2";
	public static final String SMS_ONLY = "SMS_ONLY";
	
	static String TAG = "SpeedInfo";
	
	SpeedInfo speed_info = null;
	
	static boolean num_carriers_init = false;
	static int     num_carriers = 0;
    
    enum SPEED {
    	FIFTEEN(0, "15 minutes", 15 * 60),
        ONEHOUR(1, "1 hour", 60 * 60),
        THREEHOURS(2, "3 hours", 3 * 60 * 60),
        SIXHOURS(3, "6 hours", 6 * 60 * 60),
    	TWELVEHOURS(4, "12 hours", 12 * 60 * 60);

   	    // Add new entries above this line, at the END of the list.
     
        public final int id;
        public final String label;
        public final long time;
     
        SPEED(final int id, 
        		final String label, 
        		final long time) {
            this.id = id;
            this.label = label;
            this.time = time;
        }
     
        public int getId() {
            return id;
        }
     
        public String toString() {
            return label;
        }
     
        public static SPEED fromLabel(final String label) {
            for (SPEED carrier : SPEED.values()) {
                if (carrier.toString().equalsIgnoreCase(label)) {
                    return carrier;
                }
            }
            return null;
        }
     
        public static SPEED fromId(final int id) {
            for (SPEED carrier : SPEED.values()) {
                if (carrier.id == id) {
                    return carrier;
                }
            }
            return null;
        }
        
        public static int getNumCarriers() {
        	
        	if ( ! num_carriers_init ) 
        	{
        		num_carriers = 0;
        		
        		for ( SPEED carrier : SPEED.values() ) {     			
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
        	
        	for ( SPEED carrier : SPEED.values() ) {
        		
        		all_labels[ count ] = carrier.toString();
        		
        		count++;
        	}
        	
			return all_labels;
        	
        }
    }
}
