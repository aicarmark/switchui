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

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.motorola.motoapr.service.R;
import com.motorola.motoapr.service.CarrierInfo.CARRIER;
import com.motorola.motoapr.service.PanicType.PANIC_ID;

import com.motorola.motoapr.service.APRDebug;

/**
 * @author wlsd10
 */
public class APRActivity extends Activity {
	
	static String TAG = "APRActivity"; 
	
    private IAPRService service;
    private boolean bound = false;
    private boolean binding = false;
    
    private TextView output;
    private TextView ddb_output;
    private Button storeButton;
    private Button echoButton;
    private Button ddbButton;
    private CheckBox enabled_checkbox;
    private Button rescan_button;
    private Cursor APRCursor;
    private EditText apr_stats;
    private Button mServerNum;
    
    private int spinner_item_selected = 0;
    private boolean spinner_initialized = false;
    private boolean apr_enabled_status = false;
    private boolean notice_displayed = false;

    
    String[] spinner_list = null;
    
    APRPreferences preferences = null;
    
    Timer updateProgressTimer = null;
    
    Toast mytoast = null;
    
    Context mContext = null;
               
    public APRActivity() {
    	// constructor
    }
    
    @Override
        public void onCreate(Bundle icicle) {
    	
        super.onCreate(icicle);
        
        mContext = getBaseContext();
        
        // this must be done on Create, to ensure that preferences are 
        //   initialized and ready to be used by this activity.
        preferences = new APRPreferences( mContext ); 
        
        if ( APRDebug.APRDebugGetUIDebugState() )
        {
        	onCreateDebug(icicle);
        }
        else
        {
        
        	setContentView(R.layout.apr_app_layout);
		APRDebug.APRLog( TAG, "onCreate is start"  );
    /*    
        	spinner_list = CARRIER.getAllLabels();
                
        	Spinner s1 = (Spinner) findViewById(R.id.carrier_spinner);
        	ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinner_list);
        	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        	s1.setAdapter(adapter);
        	s1.setOnItemSelectedListener(s1Listener);
     */   
     		this.mServerNum = (Button) findViewById(R.id.carrier_text); 
	       String servernumber = "Server SMS Number: ";
	
	
	     	servernumber = servernumber + APRPreferences.APRGetCarrierNumber();
		APRDebug.APRLog( TAG, "onCreate " + servernumber );
		mServerNum.setText(servernumber);

	

        	this.enabled_checkbox = (CheckBox) findViewById(R.id.apr_enabled_checkbox);
                
        	this.enabled_checkbox.setOnClickListener(new OnClickListener() {
        		public void onClick(View buttonView) {
        			// UpdateCheckbox( false );
        		}
        	});

        	this.enabled_checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
  
        		public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
	
        			boolean previous_apr_enabled_status = apr_enabled_status;
	
        			apr_enabled_status = isChecked;
								
        			if ( apr_enabled_status != previous_apr_enabled_status )
        			{
						try {
							if ( service != null )
							{		
								// do not move forward if there's a bad barcode.
								if ( APRMessage.APRMessageGoodBarCode() )
								{									
									// do not move forward if there's a bad flex version.
									//  this can cause the phone to crash.
									if ( APRMessage.APRMessageGoodFlexVersion() )
									{
										service.APRSetEnabledState( apr_enabled_status );
							
										if ( apr_enabled_status )
										{
											doMyToast( false, "APR Enabled... Scanning Started" );
										}
										else
										{
											doMyToast( false, "APR Disabled... Scanning Stopped" );
										}
									}
									else
									{
										doMyToast( false, "APR NOT ENABLED. Bad Flex Or Barcode.  Please re-flex and THEN rephase the phone." );
										UpdateCheckBox( false );
									}
								}
								else
								{
									doMyToast( false, "APR NOT ENABLED. Bad Flex Or Barcode.  Please re-flex and THEN rephase the phone." );
									UpdateCheckBox( false );
								}
							}
						
							// after things are initialized, we can update the
							//   stats.
							UpdateStatsBox();
				        
						} catch (RemoteException e) {
							APRDebug.APRDebugStack(e);
						}
        			}
        		}        	
        	});
        
        	// Update the stats box, so the new information is present.
        	UpdateStatsBox();        
        }
    } // End onCreate APRActivity

    private void doMyToast( boolean long_toast, String toaster )
    {   	
    	if (mytoast != null) 
    	{
    		mytoast.cancel();
    	}

    	if ( long_toast )
    	{
    		mytoast = Toast.makeText(mContext, toaster, Toast.LENGTH_LONG);
    	}
    	else
    	{
    		mytoast = Toast.makeText(mContext, toaster, Toast.LENGTH_SHORT);
    	}
   		mytoast.setGravity(Gravity.CENTER, 0, 0);
   		mytoast.show();
    }
    
    public void onCreateDebug(Bundle icicle) {
	    
    setContentView(R.layout.apr_app_layout_debug);
    
    spinner_list = CARRIER.getAllLabels();
            
    Spinner s1 = (Spinner) findViewById(R.id.carrier_spinner);
    ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, spinner_list);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    s1.setAdapter(adapter);
    s1.setOnItemSelectedListener(s1Listener);
    
    this.enabled_checkbox = (CheckBox) findViewById(R.id.apr_enabled_checkbox);
    
    this.enabled_checkbox.setOnClickListener(new OnClickListener() {
		public void onClick(View buttonView) {
    		// UpdateCheckbox( false );
		}
    });
            
    this.enabled_checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {

			boolean previous_apr_enabled_status = apr_enabled_status;

			apr_enabled_status = isChecked;
			
			if ( apr_enabled_status != previous_apr_enabled_status )
			{
				try {
					if ( service != null )
					{
						service.APRSetEnabledState( apr_enabled_status );
						
						if ( apr_enabled_status )
						{
							doMyToast( false, "APR Enabled... Scanning Started");
						}
						else
						{
							doMyToast( false, "APR Disabled... Scanning Stopped");
						}
					}
					
					// after things are initialized, we can update the
					//   stats.
			        UpdateStatsBox();
			        
				} catch (RemoteException e) {
					APRDebug.APRDebugStack(e);
				}
			}
		}
    });
    
	this.rescan_button = (Button) findViewById(R.id.rescan_button);
	this.rescan_button.setOnClickListener(new OnClickListener() {
		public void onClick(View v) {
        	try {
        		service.APRRescanPhoneInfo();
        		
			} catch (RemoteException e) {
				APRDebug.APRDebugStack(e);
			}
            output.setText( "Test Message Scheduled ..." );
        }
	});        

    
    this.output = (TextView) findViewById(R.id.output);
    this.ddb_output = (TextView) findViewById(R.id.ddb_output);                
    
    // Setup the Echo Button
    this.echoButton = (Button) findViewById(R.id.echo_button);
    this.echoButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	try {
					service.APRSendEmail( "wlsd10@email.mot.com",
										  "This is the message subject",
										  "This is the email body text.");
					
				} catch (RemoteException e) {
					APRDebug.APRDebugStack(e);
				}
                output.setText( "Test Message Scheduled ..." );
            }
        });
    
    // Setup the Store Button
    this.storeButton = (Button) findViewById(R.id.store_button);
    this.storeButton.setOnClickListener(new OnClickListener() {
    	
            public void onClick(View v) {
            	try {
        		    APRCursor = new APRMessageDatabase(mContext).GetDBCursor();         	
            		
					service.APRStoreTestCrash( "This is the test crash." + APRCursor.getCount()  );
	            	
	            	APRCursor.deactivate();
					APRCursor.close();
					
				} catch (RemoteException e) {
					APRDebug.APRDebugStack(e);
				}
                output.setText( "Test Crash Recorded ..." );
            }
        });
    
    // Toggle Speed Button
    this.ddbButton = (Button) findViewById(R.id.ddb_button);        
    this.ddbButton.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
    		    
        		APRCursor = new APRMessageDatabase(mContext).GetDBCursor();         	
    		    
    		    boolean run_fast = false;
    		    String  speed_string = "Run Normal";
    		    
    		    if ( service != null ) {
    		    			    
    		    	try {
    		    		service.APRToggleRunSpeed();
    		    	} catch (RemoteException e) {
    		    		e.printStackTrace();
    		    	}
    		    
    		    	try {
    		    		run_fast = service.APRGetRunSpeed();
    		    	} catch (RemoteException e) {
    		    		e.printStackTrace();
    		    	}
    		    
    		    	if ( run_fast )
    		    	{
    		    		speed_string = "Run Fast";
    		    	}
				
    		    	// Access Database...
    		    	ddb_output.setText( speed_string );
    		    }
            	
            	APRCursor.deactivate();
			APRCursor.close();
        	}      
    	});                
    
    // Update the stats box.
    apr_stats = (EditText)findViewById(R.id.APRStats);
    UpdateStatsBox();

} // End onCreate APRActivity
    
    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder iservice) {
        	
        	APRDebug.APRLog( TAG, "onServiceConnected" );
        	
        	try {
        		service = IAPRService.Stub.asInterface(iservice);
        		// doMyToast( "Connected to APR Service" );
        		binding = false;
        		bound = true;
        	} catch ( Exception e ) {
        		APRDebug.APRLog( TAG, "Failed To Connect" );
        	}
        }
        
        public void onServiceDisconnected(ComponentName className) {
        	
        	APRDebug.APRLog( TAG, "onServiceDisconnected" );
        	
        	try {
        		service = null;
        		// doMyToast( "Disconnected from APR Service" );
        		bound = false;
        		binding = false;
        	} catch ( Exception e ) {
        		APRDebug.APRLog( TAG, "Failed To Disconnect" );
        	}
        }
    };

    private void UpdateCheckBox( boolean update_complete ) {
    	
    	if ( service != null ) 
    	{
    		try {
    			boolean apr_enabled_state = service.APRGetEnabledState();
    			
    			this.enabled_checkbox.setChecked( apr_enabled_state );
    			
    			if ( !apr_enabled_state )
    			{
    				if ( !notice_displayed ) 
    				{
    					// if APR is off, display this notice once.
    					// doMyToast( true, "NOTICE: APR WILL EXIT if you have bad FLEX or PHASING data. If this happens, please reflex and rephase, and retry.");
    					notice_displayed = true;
    				}
    			}
    			else
    			{
    				// every time the apr service shuts off,
    				//   we will display the notice once in the future.
    				notice_displayed = false;
    			}
    			
    			if ( update_complete ) 
    			{
    				this.enabled_checkbox.setText(R.string.apr_enabled);
    			}
    			else
    			{
    			    this.enabled_checkbox.setText(R.string.apr_updating);
    			}
    		} catch (RemoteException e) {
    			APRDebug.APRDebugStack(e);
    		}
    	}
    }
    
    private void UpdateStatsBox()
    {
    	try { 
    			this.apr_stats = (EditText)findViewById(R.id.APRStats);
    
    			int critical_panics = APRPreferences.APRPrefsGetPanicCount( true );
    			int non_critical_panics = APRPreferences.APRPrefsGetPanicCount( false );
    			String bar_code = APRPreferences.APRPrefsGetDefaultBarcode();
    			String software_version = APRPreferences.APRPrefsGetSwVers();
    			long time_elapsed = APRPreferences.APRPrefsGetTotalUptimeElapsed();
    			
    			long uptime_to_be_sent = APRPreferences.APRPrefsGetTimeSinceLastReport();
    			long no_data_crash_count = APRPreferences.APRPrefsGetCrashCount( PANIC_ID.KERNEL_APANIC_ND );
    			    
    			String stats = 
    				"Software Version:     \n  " + software_version + "\n" +
    				"Bar Code In Use:      \n  " + bar_code + "\n" +
    				"Critical Panics:      \n  " + critical_panics + "\n" +
    				"Kernel Crash ND:      \n  " + no_data_crash_count + "\n" +
    				"App Crashes:          \n  " + non_critical_panics + "\n" +
    			    "Uptime:               \n  " + time_elapsed/(60*60*1000) + "h " + (time_elapsed%(60*60*1000))/(60*1000) + "m\n" +
    			    "Unreported Uptime:    \n  " + uptime_to_be_sent/(60*60*1000) + "h " + (uptime_to_be_sent%(60*60*1000))/(60*1000) + "m\n" ;
        
    			this.apr_stats.setText( stats );
    			    			
    	} catch ( Exception e ) {
    		APRDebug.APRLog ( TAG, " Stats not printed. Not Ready. " );
    	}
    }
    
    // Routine to create the list adaptor used in the app.
    //   every time there's an update to the database, this routine
    //   should be called.  
    @Override
        public void onPause() {
        super.onPause();
        if (this.bound && !this.binding) {
            unbindService(this.connection);
            this.binding = true;
        }
    }
        
    @Override
        public void onStart() {
    	
        super.onStart();

        if (!this.bound && !this.binding) {
            bindService(new Intent(APRActivity.this, APRService.class), 
            		    this.connection,
                        Context.BIND_AUTO_CREATE);
            this.binding = true;
        }
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    
    	startUpdateTimer();
    }
    
    private void startUpdateTimer() {
    	
    	if ( updateProgressTimer == null )
    	{
    		updateProgressTimer = new Timer();

    		updateProgressTimer.scheduleAtFixedRate(new TimerTask() {
    			   			
    			@Override
    			public void run() {
    				
    				runOnUiThread( new Runnable() {

    					public void run() {
    						UpdateCheckBox( true );
    						UpdateStatsBox();
    					}
    				});
    				
    			}
    		}, 0, 10000); // check every 10 seconds
    	}
    }
    
    private void stopUpdateTimer() {
    	
    	// stop the screen update timer
    	if ( updateProgressTimer != null )
    	{
    		updateProgressTimer.cancel();
    		
    		updateProgressTimer = null;
    	}
    }
    
    @Override
    public void onStop() {
    	
    	super.onStop();
    	
    	stopUpdateTimer();
    }
    
	private Spinner.OnItemSelectedListener s1Listener = new Spinner.OnItemSelectedListener(){		
		
		public void onItemSelected(AdapterView<?> parent, View v,
                                   int position, long id) {
			
			int initial_position = spinner_item_selected;
			
			if ( service != null )
			{
				if ( !spinner_initialized )
				{
					// get the carrier preference for usage below.
					try {
						spinner_item_selected = service.APRGetCarrierId();
					} catch (RemoteException e1) {
						e1.printStackTrace();
					}
				
					parent.setSelection( spinner_item_selected );
				
					spinner_initialized = true;
				}
			
				int pos = parent.getSelectedItemPosition();
            
				spinner_item_selected = pos;
            
				// check if we need to update the position.
				if ( initial_position != spinner_item_selected )
				{
					try {
						service.APRSetCarrierId(spinner_item_selected);
					} catch (RemoteException e) {
						APRDebug.APRDebugStack(e);
					}
				}
			}
			
			APRDebug.APRLog ( TAG, "Item Selected " + spinner_list[spinner_item_selected] );
            
            // This is here because we need someplace to initialize the checkbox
            //   AFTER the service has become available.  But we need to guarantee it gets
            //   filled in every time we start the app.
            UpdateCheckBox( true );
        }
		
        @SuppressWarnings("unchecked")
        public void onNothingSelected(AdapterView arg0) {
        }
	}; // end spinner selected listener
}
