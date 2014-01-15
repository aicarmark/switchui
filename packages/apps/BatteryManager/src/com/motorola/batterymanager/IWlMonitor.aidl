 
package com.motorola.batterymanager;
/** @hide */
interface IWlMonitor {
  
   void storeWakeLock(int uid, int pid, int type, String tag);
  
   void removeWakeLock(int uid);
   
   
   }
