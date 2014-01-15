//********************************************************** //
//PROJECT:     APR (Automatic Panic Recording)
//DESCRIPTION: 
//The purpose of APR is to gather the panics in the device
//and record statics about those panics to a centralized
//server, for automated tracking of the quality of a program.
//To achieve this, several types of messages are required to
//be sent at particular intervals.  This package is responsible
//for sending the data in the correct format, at the right 
//intervals.
//********************************************************** //
//Change History
//********************************************************** //
//Author         Date       Tracking  Description
//************** ********** ********  ********************** //
//Stephen Dickey 03/01/2009 1.0       Initial Version
//
//********************************************************** //
package com.motorola.motoapr.service;

public class PuReasonType {

	static boolean num_pu_reason_types_init = false;
	static int num_pu_reason_types = 0;
 
 enum PU_REASON_TYPE {	

     // these are values not defined by the bootloader
     NO_PU_REASON_TYPE1    ( 0,  0x00000000, "NO_PU_REASON_TYPE"      , false ),
	 NO_PU_REASON_TYPE2    ( 1,  0x00000001, "NO_PU_REASON_TYPE"      , false ),
	 NO_PU_REASON_TYPE3    ( 2,  0x00000002, "NO_PU_REASON_TYPE"      , false ),
	 NO_PU_REASON_TYPE4    ( 3,  0x00000004, "NO_PU_REASON_TYPE"      , false ),

	 // these values are straight from the bootloader.  
     TIME_OF_DAY_ALARM     ( 4,  0x00000008, "TIME_OF_DAY_ALARM"      , false ),
	 USB_CABLE             ( 5,  0x00000010, "USB_CABLE"              , false ),
	 FACTORY_CABLE         ( 6,  0x00000020, "FACTORY_CABLE"          , false ),
	 AIRPLANE_MODE         ( 7,  0x00000040, "AIRPLANE_MODE"          , false ),
	 PWR_KEY_PRESS         ( 8,  0x00000080, "PWR_KEY_PRESS"          , false ),
	 CHARGER               ( 9,  0x00000100, "CHARGER"                , false ),
	 POWER_CUT             ( 10, 0x00000200, "POWER_CUT"              , false ),
	 REGRESSION_CABLE      ( 11, 0x00000400, "REGRESSION_CABLE"       , false ),
	 SYSTEM_RESTART        ( 12, 0x00000800, "SYSTEM_RESTART"         , false ),
	 MODEL_ASSEMBLY        ( 13, 0x00001000, "MODEL_ASSEMBLY"         , false ),
	 MODEL_ASSEMBLY_VOL    ( 14, 0x00002000, "MODEL_ASSEMBLY_VOL"     , false ),
	 SW_AP_RESET           ( 15, 0x00004000, "SW_AP_RESET"            , false ),
	 WDOG_AP_RESET         ( 16, 0x00008000, "WDOG_AP_RESET"          , false ),
	 CLKMON_CKIH_RESET     ( 17, 0x00010000, "CLKMON_CKIH_RESET"      , false ),
	 AP_KERNEL_PANIC       ( 18, 0x00020000, "AP_KERNEL_PANIC"        , false ),
     CPCAP_WDOG            ( 19, 0x00040000, "CPCAP_WDOG"             , false ),
     CIDTCMD               ( 20, 0x00080000, "CIDTCMD"                , false ),
     BAREBOARD             ( 21, 0x00100000, "BAREBOARD"              , false ),
	 PANIC_POWERUP ( 22, 0x00200000, "panic powerup"     , true);
	 
     // add new entries above this line.
		
     public final int index;
     public final long value;
     public final String label;
     public final boolean reportable;

     PU_REASON_TYPE(final int index,
    		 final long value,
     		 final String label, 
     		 final boolean reportable ) { 
         this.index = index;
         this.value = value;
         this.label = label;
         this.reportable = reportable;
     }
     
     
  
     public int getIndex() {
         return index;
     }
  
     public String toString() {
         return label;
     }
  
     public static PU_REASON_TYPE fromLabel(final String label) {
         for (PU_REASON_TYPE pu_reason_type : PU_REASON_TYPE.values()) {
             if (pu_reason_type.toString().equalsIgnoreCase(label)) {
                 return pu_reason_type;
             }
         }
         return null;
     }
  
    public static int getNumPuReasonTypes() {
     	
     	if ( ! num_pu_reason_types_init ) 
     	{
     		num_pu_reason_types = 0;
     		
     		for ( PU_REASON_TYPE pu_type : PU_REASON_TYPE.values() ) {     			
     			num_pu_reason_types++;
     		}
     		
     		num_pu_reason_types_init = true;
     	}
     	
			return num_pu_reason_types;
     	
     }
     
     public static String[] getAllLabels() {
     	String[] all_labels = null;
     	int count = 0;
     	
     	all_labels = new String[ getNumPuReasonTypes() ];
     	
     	for ( PU_REASON_TYPE pu_reason : PU_REASON_TYPE.values() ) {
     		
     		all_labels[ count ] = pu_reason.toString();
     		
     		count++;
     	}
     	
			return all_labels;
     	
     }
 }

 public static boolean matches( PU_REASON_TYPE matching_reason, long test_value )
 {
	 boolean matches = false;
	 
	 if ( ( matching_reason.value & test_value ) != 0 )
	 {
		 matches = true;
	 }
	 
	 return( matches );
 }

}
