/*
 * @(#)RuleStateValues.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2012/03/12 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.util;

import com.motorola.contextual.smartrules.Constants;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/** This is a parcelable class element that is used to send as an element in 
 *  the intent response.
 *
 *<code><pre>
 * CLASS:
 *
 *  implements
 *   Constants - for the constants used
 *   Parcelable - for the DB related constants
 *
 * RESPONSIBILITIES:
 * 	 parcel the object
 *
 * COLABORATORS:
 * 	 None.
 *
 * USAGE:
 * 	 See each method.
 *</pre></code>
 */
public class RuleStateValues implements Parcelable, Constants {
	
	private static final String TAG = RuleStateValues.class.getSimpleName();
	
    public String ruleKey;
    public long ruleId;
    public int enabled;
    public int active;
    public int ruleType;
    public String iconRes;
    public String ruleName;

    /** simple constructor
     */
    public RuleStateValues() {
        super();
    }

    /** parcel constructor
     * 
     * @param parcel - parcel
     */
    public RuleStateValues(Parcel parcel) {
    	ruleKey = parcel.readString();
    	ruleId = parcel.readLong();
    	enabled = parcel.readInt();
    	active = parcel.readInt();
    	ruleType = parcel.readInt();
    	iconRes = parcel.readString();
    	ruleName = parcel.readString();
    }
    
    public int describeContents ()
    {
        return 0;
    }
    
    @SuppressWarnings("rawtypes")
    public final Parcelable.Creator CREATOR = new Parcelable.Creator() {

        public Object createFromParcel(Parcel source) {
            return new RuleStateValues(source);
        }

        public Object[] newArray(int size) {
            return new RuleStateValues[size];
        }
    };
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ruleKey = "+ruleKey)
                .append(" ruleId ="+ruleId)
                .append(" enabled ="+enabled)
                .append(" active = "+active)
                .append(" ruleType = "+ruleType)
        		.append(" iconRes = "+iconRes)
        		.append(" ruleName = "+ruleName);
        
        return builder.toString();
    }
    
    public void writeToParcel(Parcel dest, int flags) {
    	if(LOG_DEBUG) Log.d(TAG, "in writeToParcel - "+this.toString());
    	dest.writeString(ruleKey);
    	dest.writeLong(ruleId);
    	dest.writeInt(enabled);
    	dest.writeInt(active);
    	dest.writeInt(ruleType);
    	dest.writeString(iconRes);
    	dest.writeString(ruleName);
    } 

    /** getter - ruleKey
     * 
     * @return - the rule key
     */
    public String getRuleKey() {
    	return this.ruleKey;
    }
    
    /** getter - ruleId
     * 
     * @return - the rule id
     */
    public long getRuleId() {
    	return this.ruleId;
    }
    
    /** getter - enabled
     * 
     * @return - enabled value (0 = Enabled 1 = Disabled)
     */
    public int getEnabled() {
    	return this.enabled;
    }
    
    /** getter - active
     * 
     * @return - active value
     */
    public int getActive() {
    	return this.active;
    }
    
    /** getter - rule type
     * 
     * @return - the rule type
     */
    public int getRuleType() {
    	return this.ruleType;
    }     
  
    /*
    * 
    * @return - the rule icon
    */
   public String getRuleIcon() {
   	return this.iconRes;
   }
  
   /*
    * 
    * @return - the rule Name
    */
   public String getRuleName() {
   	return this.ruleName;
   }
}