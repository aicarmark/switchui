/*
 * @(#)ConditionTuple.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2010/10/28 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table;



import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlSerializer;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import com.motorola.contextual.smartrules.rulesimporter.XmlConstants;

/**This class abstracts or encapsulates one row of the Condition table.
 *
 * The table is used here to hold a condition or precondition.
 *
 *<code><pre>
 * CLASS:
 * 	Extends TupleBase which implements the row id (_id) field of the tuple.
 * 	Implements Parcelable to allow bundling.
 * 	Implements Comparable to allow sorting.
 * 	Implements Cloneable to allow cloning of records.
 *
 * RESPONSIBILITIES:
 * 	create Tuple instance from fields or from a parcel.
 *
 * COLABORATORS:
 * 	ConditionTable - which wraps all the table functions around this class.
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class ConditionTuple extends TupleBase implements Cloneable, Parcelable, Comparable<ConditionTuple> {

	private static final String TAG = ConditionTuple.class.getSimpleName();
	
    /** foreign key pointing to RuleTable _id field */
    private   long	 	parentFkey;
    /**  0=inactive(visually disconnected), 1=active(visually connected), 3=suggested(suggested, but never connected) */
    private   int	 	enabled;
    /** 0=accepted, 1=unread, 2=read */
    private   int		suggState;
    /** Suggested XML &ltREASON&gt */
    private   String	suggReason;
    /** Whether or not this condition is true or false 0=not met, 1=met*/
    private   int		condMet;
    /** State publisher key. */
    private   String	publisherKey;
    /** flag indicating whether the precondition has states, stateless(0) or stateful (1) or unknown (-1).
     * for example, the precondition "at home" is stateful - you're either at home or not at home.
     * However, a missed call precondition is not stateful (has no beginning or end or state). */
    private   int		modality;
    /** sensor name or state machine that controls this condition. */
    private   String	sensorName;
    /** Intent to fire activity to perform customization */
    private   String	activityIntent;
    /** target state that will trigger this rule. 
     *  NOTE: This column is deprecated as of version 35 of the DB. In the new Condition Publisher
     *  architecture, CONDITION_CONFIG holds all the configuration information 
     *   */
    @Deprecated
    private   String	targetState;
    /** description for the user of the condition that will trigger this rule. */
    private   String	description;
    /** state Rule syntax  
     *  NOTE: This column is deprecated as of version 35 of the DB. In the new Condition Publisher
     * architecture, CONDITION_CONFIG holds all the configuration information   */
     @Deprecated
    private   String	stateSyntax;
    /** The original created date and time of this record. */
    private   long 		createdDateTime;
    /** The last condition failure date and time of this record. */
    private   long		lastFailDateTime;
    /** Failure Message */
    private   String	condFailMsg;
    /** 3 icon paths, like this (in XML format) <ldpi>, <mdpi>, <hdpi> */
    private   String	icon;
    /** config details */
    private   String	config;
	/** Validity of the condition publisher.  */
    private   String	validity;
	/** Market Url to download this condition publisher */
    private   String	marketUrl;

    //Node Name & Condition Table mapping for <CONDITION>
    //and is used to convert Condition Table into XML String
    private static HashMap<String, String> conditionInfo;

    /** Basic constructor, constructs GPS record class */
    public ConditionTuple() {
        super();
        setCreatedDateTime(0);
        this.dirtyFlag = false;
        modality = ConditionTable.Modality.UNKNOWN;
    }


    /** Constructor make a copy (clone) of the record
     *
     * @param tuple
     */
    public ConditionTuple(ConditionTuple tuple) {
        super();
        copy(tuple);
        this.dirtyFlag = false;     
    }


    /** constructs GPS record class */
    public ConditionTuple(			long 	_id,
                                    long 	ruleFkey,
                                    int		enabled,
                                    int		suggState,
                                    final   String  suggReason,
                                    int		condMet,
                                    final 	String 	publisherKey,
                                    int 	modality,
                                    final 	String 	sensorName,
                                    final 	String 	activityIntent,
                                    final	String 	targetState,
                                    final 	String 	description,
                                    final 	String	stateSyntax,
                                    long 	createdDateTime,
                                    long 	lastFailDateTime,
                                    final	String  condFailMsg,
                                    final	String 	icon,
                                    final   String  config,
                                    final   String  validity,
                                    final   String  marketUrl) {

        super();
        this._id = _id;
        this.parentFkey 	= ruleFkey;
        this.enabled 		= enabled;
        this.suggState		= suggState;
        this.suggReason		= suggReason;
        this.condMet		= condMet;
        this.publisherKey	= publisherKey;
        this.modality		= modality;
        this.sensorName 	= sensorName;
        this.activityIntent = activityIntent;
        this.targetState 	= targetState;
        this.description	= description;
        this.stateSyntax 	= stateSyntax;
        setCreatedDateTime(createdDateTime);
        this.lastFailDateTime = lastFailDateTime;
        this.condFailMsg	= condFailMsg;
        this.icon 			= icon;
        this.dirtyFlag 		= false;
        this.config = config;
        this.validity = validity;
        this.marketUrl = marketUrl;
    }


    /** constructor for parcel re-inflate.
     *
     * @param parcel to reconstruct the instance
     */
    public ConditionTuple(Parcel parcel) {

        super(parcel);
    }


    /** getter - foreign key to the rule _id field/column
     *
     * @return the ruleFk
     */
    public long getParentFkey() {
        return parentFkey;
    }


    /** setter - foreign key to the rule _id field/column
     *
     * @param fk - the ruleFk to set
     */
    public void setParentFkey(long ruleFk) {
        this.parentFkey = ruleFk;
        this.dirtyFlag = true;
    }


    /** getter - enabled (1)  means the condition will be used disabled (0) means the
     *  condition will be ignored when evaluating the rule sensor.
     * 
     * @return the enabled - 1=enabled or 0=disabled
     */
    public int getEnabled() {
        return enabled;
    }


    /** setter - enabled (enabled (1)  means the condition will be used disabled (0) 
     *  means the condition will be ignored when evaluating the rule sensor.) 
     * @param enabled - 1=enabled or 0=disabled
     */
    public void setEnabled(int enabled) {
        this.enabled = enabled;
        this.dirtyFlag = true;
    }


    /** getter - 0=accepted suggested condition, 1=unread suggested condition, 
     * 	2=read suggested condition
     * 
     * @return the suggState - 0=accepted, 1=unread, 2=read
     */
    public int getSuggState() {
        return suggState;
    }


    /** setter - suggState (0=accepted suggested condition, 1=unread suggested condition, 
     * 	2=read suggested condition)
     * 
     * @param suggState - 0=accepted, 1=unread, 2=read
     */
    public void setSuggState(int suggState) {
        this.suggState = suggState;
        this.dirtyFlag = true;
    }


    /** getter - reason why this condition is suggested
     * 
     * @return the suggReason
     */
    public String getSuggReason() {
        return suggReason;
    }


    /** setter - the reason to suggest the condition
     * 
     * @param suggReason - the suggReason to set
     */
    public void setSuggReason(String suggReason) {
        this.suggReason = suggReason;
        this.dirtyFlag = true;
    }


    /** getter - whether or not this condition is true or false 0=not met, 1=met
     * 
     * @return the condMet - 0=not met or 1=met
     */
    public int getCondMet() {
        return condMet;
    }


    /** setter - whether or not this condition is true or false 0=not met, 1=met
     * 
     * @param condMet - 0=not met or 1=met
     */
    public void setCondMet(int condMet) {
        this.condMet = condMet;
        this.dirtyFlag = true;
    }


    /** getter - unique key to identify the condition publisher 
     * 
     * @return the publisherKey
     */
    public String getPublisherKey() {
        return publisherKey;
    }


    /** setter - publisherKey (unique Key to identify the condition publisher)
     * 
     * @param publisherKey - unique Key to identify the condition publisher
     */
    public void setPublisherKey(String publisherKey) {
        this.publisherKey = publisherKey;
        this.dirtyFlag = true;
    }


    /** getter - indicates if the condition is stateful (0) or stateless (1).
  	 * 	for example, the precondition "at home" is stateful - you're either at home 
  	 *  or not at home. However, a missed call precondition is not stateful (has no 
  	 *  beginning or end or state).
     * 
     * @return the modality - 0=stateful or 1=stateless (1)
     */
    public int getModality() {
        return modality;
    }


    /** setter - modality to set (stateful (0) or stateless (1))
     * 
     * @param modality - 0=stateful or 1=stateless (1)
     */
    public void setModality(int modality) {
        this.modality = modality;
        this.dirtyFlag = true;
    }


    /** getter - sensor name or state machine name. This should be a common name like 
     *  "Location", "Wi-Fi" etc...
     *  
     * @return the sensorName visible to the user
     */
    public String getSensorName() {
        return sensorName;
    }


    /** setter - sensor name or state machine name
     * 
     * @param sensorName the sensorName to set
     */
    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
        this.dirtyFlag = true;
    }


    /** getter - Intent to fire to change or customize the condition, set to null if no 
     * 	configuration required.
     * 
     * @return the activityIntent to change or customize the condition or null
     */
    public String getActivityIntent() {
        return activityIntent;
    }


    /** setter - activityIntent the intent to fire to change or customize the condition, 
     * set to null if no configuration required.
     * 
     * @param activityIntent the intent to fire to change or customize the condition, 
     * set to null if no configuration required.
     */
    public void setActivityIntent(String activityIntent) {
        this.activityIntent = activityIntent;
        this.dirtyFlag = true;
    }


    /** getter - state for which this condition will activate.
     * 
     * @return the targetState
     */
    public String getTargetState() {
        return targetState;
    }


    /** setter - state for which this condition will activate.
     * 
     * @param targetState the targetState to set
     */
    public void setTargetState(String targetState) {
        this.targetState = targetState;
        this.dirtyFlag = true;
    }


    /** getter - description of the condition
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }


    /** setter - description of the condition
     * 
     * @param description - the description to set
     */
    public void setDescription(String description) {
        this.description = description;
        this.dirtyFlag = true;
    }


    /** getter - rule syntax for the condition
     * 
     * @return the stateSyntax of the condition
     */
    public String getStateSyntax() {
        return stateSyntax;
    }


    /** setter - the rule syntax to set for the condition 
     * 
     * @param stateSyntax the rule syntax to set for the condition
     */
    public void setStateSyntax(String stateSyntax) {
        this.stateSyntax = stateSyntax;
        this.dirtyFlag = true;
    }


    /** getter - the date time when the condition was created (see java.util.Date.getTime())
     * 
     * @return the date and time when the condition was created
     */
    public long getCreatedDateTime() {
        return createdDateTime;
    }


    /** setter - the date time when the condition was created
     * 
     * @param createdDateTime the condition creation date and time to set
     *   (see java.util.Date.getTime())
     */
    public void setCreatedDateTime(long createdDateTime) {
        if (createdDateTime < 1)
            this.createdDateTime = new Date().getTime();
        else
            this.createdDateTime = createdDateTime;
        this.dirtyFlag = true;
    }


    /** getter - the most recent date time when the condition failed 
     * (see java.util.Date.getTime())
     * 
     * @return the lastFailDateTime - the date time when the condition failed most recently
     */
    public long getLastFailDateTime() {
        return lastFailDateTime;
    }


    /** setter - the date time when the condition failed (see java.util.Date.getTime())
     * 
     * @param lastFailDateTime the date time to set when the condition failed
     */
    public void setLastFailDateTime(long lastFailDateTime) {
        this.lastFailDateTime = lastFailDateTime;
        this.dirtyFlag = true;
    }


    /** getter - failure Message when the condition failed.
     * 
     * @return the condFailMsg when the condition failed
     */
    public String getCondFailMsg() {
        return condFailMsg;
    }


    /** setter - condFailMsg the failure message to set when the condition failed.
     * 
     * @param condFailMsg the failure message to set when the condition failed
     */
    public void setCondFailMsg(String condFailMsg) {
        this.condFailMsg = condFailMsg;
        this.dirtyFlag = true;
    }


    /** getter - icon used for the condition
     * 
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }


    /** setter - icon of the condition
     * 
     * @param icon the icon to set for the condition
     */
    public void setIcon(String icon) {
        this.icon = icon;
        this.dirtyFlag = true;
    }
    

    /** getter - Config details of the condition
     *
     * @return the config
     */
    public String getConfig() {
		return config;
	}

    /** setter -  Config details of the condition
     *
     * @param config
     */
	public void setConfig(String config) {
		this.config = config;
	}

    /** getter - validity of the condition
     *
     * @return the validity
     */
    public String getValidity() {
		return validity;
	}

    /** setter - validity of the condition
     *
     * @param valdiity the validity to set for the condition
     */
	public void setValidity(String validity) {
		this.validity = validity;
	}

    /** getter - Market url of the condition
     *
     * @return the market url
     */
    public String getMarketUrl() {
		return marketUrl;
	}

    /** setter - Market url of the condition
     *
     * @param marketUrl the marketUrl to set for the condition
     */
	public void setMarketUrl(String marketUrl) {
		this.marketUrl = marketUrl;
	}

    /** clone a tuple.
     *
     * @param tuple - tuple to be cloned.
     * @return - new instance cloned from the old one.
     */
    public ConditionTuple copy(ConditionTuple tuple) {

    	super.copy(tuple);
        this.parentFkey 	= tuple.parentFkey;
        this.enabled		= tuple.enabled;
        this.suggState		= tuple.suggState;
        this.suggReason		= tuple.suggReason;
        this.condMet		= tuple.condMet;
        this.publisherKey	= tuple.publisherKey;
        this.modality		= tuple.modality;
        this.sensorName		= tuple.sensorName;
        this.activityIntent	= tuple.activityIntent;
        this.targetState	= tuple.targetState;
        this.description	= tuple.description;
        this.stateSyntax	= tuple.stateSyntax;
        this.condFailMsg	= tuple.condFailMsg;
        this.icon 			= tuple.icon;
        this.config         = tuple.config;
        this.validity       = tuple.validity;
        this.marketUrl      = tuple.marketUrl;
        this.setCreatedDateTime(0);
        this.setLastFailDateTime(0);
        
        this.dirtyFlag = false;
        return this;
    }


    /** write this tuple to a parcel for bundling.
     *
     * @see com.motorola.contextual.smartrules.util.ParcelableBase#writeToParcel(android.os.Parcel, int)
     */
    public void writeToParcel(Parcel dest, int flags) {

        super.writeToParcel(dest, flags);
    }


    /** creator for parceling */
    public static final Parcelable.Creator<ConditionTuple> CREATOR =
    new Parcelable.Creator<ConditionTuple>() {

        public ConditionTuple createFromParcel(Parcel in) {
            return new ConditionTuple(in);
        }

        public ConditionTuple [] newArray(int size) {
            return new ConditionTuple[size];
        }
    };

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ConditionTuple)
            return compareTo((ConditionTuple)o) == 0;
        else
            return false;
    }



    @Override
    public int hashCode() {
        int result = Hash.SEED;
        result = Hash.hash( result, _id );
        result = Hash.hash( result, parentFkey );
        result = Hash.hash( result, enabled );
        result = Hash.hash( result, suggState );
        result = Hash.hash( result, modality );
        result = Hash.hash( result, enabled );
        result = Hash.hash( result, modality );
        result = Hash.hash( result, condMet );
        result = Hash.hash( result, suggReason );
        result = Hash.hash( result, publisherKey );
        result = Hash.hash( result, sensorName );
        result = Hash.hash( result, activityIntent );
        result = Hash.hash( result, targetState );
        result = Hash.hash( result, description );
        result = Hash.hash( result, stateSyntax );
        result = Hash.hash( result, condFailMsg );
        result = Hash.hash( result, icon );
        result = Hash.hash( result, logicalDelete);
        result = Hash.hash( result, dirtyFlag);
        result = Hash.hash( result, config );
        result = Hash.hash( result, validity );
        result = Hash.hash( result, marketUrl );
        return result;
    }



    /** Comparison method for equals, not really good for sorting.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ConditionTuple another) {

        if (this._id 			!= another._id) return 1;
        if (this.parentFkey 	!= another.parentFkey) return 1;
        if (this.enabled 		!= another.enabled) return 1;
        if (this.suggState		!= another.suggState) return 1;
        if (this.modality 		!= another.modality) return 1;
        if (this.condMet        != another.condMet) return 1;

        if (this.suggReason     == null && another.suggReason != null) return 1;
        if (this.suggReason     != null && suggReason.equals(another.suggReason)) return 1;

        if (this.publisherKey 	== null && another.publisherKey != null) return 1;
        if (this.publisherKey 	!= null && !publisherKey.equals(another.publisherKey)) return 1;

        if (this.sensorName 	== null && another.sensorName != null) return 1;
        if (this.sensorName 	!= null && !sensorName.equals(another.sensorName)) return 1;

        if (this.activityIntent	== null && another.activityIntent != null) return 1;
        if (this.activityIntent != null && !activityIntent.equals(another.activityIntent)) return 1;

        if (this.targetState 	== null && another.targetState != null) return 1;
        if (this.targetState 	!= null && !targetState.equals(another.targetState)) return 1;

        if (this.description 	== null && another.description != null) return 1;
        if (this.description 	!= null && !description.equals(another.description)) return 1;

        if (this.stateSyntax 	== null && another.stateSyntax != null) return 1;
        if (this.stateSyntax 	!= null && !stateSyntax.equals(another.stateSyntax)) return 1;

        if (this.condFailMsg    == null && another.condFailMsg != null) return 1;
        if (this.condFailMsg    != null && !condFailMsg.equals(another.condFailMsg)) return 1;

        if (this.icon			== null && another.icon != null) return 1;
        if (this.icon 			!= null && !icon.equals(another.icon)) return 1;

        if (this.config 			== null && another.config != null) return 1;
        if (this.config 			!= null && !config.equals(another.config)) return 1;

        if (this.validity 			== null && another.validity != null) return 1;
        if (this.validity 			!= null && !validity.equals(another.validity)) return 1;

        if (this.marketUrl 			== null && another.marketUrl != null) return 1;
        if (this.marketUrl 			!= null && !marketUrl.equals(another.marketUrl)) return 1;
        return 0;
    }


    /** converts tuple to String mainly for debugging purposes.
     *
     * @see com.motorola.contextual.smartrules.db.table.TupleBase#toString()
     */
    public String toString() {

        StringBuilder builder = new StringBuilder("  Condition."+super.toString());

        builder.append(", parentFkey="+ parentFkey)
        .append(", enabled="+ enabled)
        .append(", suggState="+ suggState)
        .append(", suggReason="+ suggReason)
        .append(", condMet=" +condMet)
        .append(", publisherKey="+ publisherKey)
        .append(", modality="+ modality)
        .append(", sensorName="+ sensorName)
        .append(", activityIntent="+ activityIntent)
        .append(", targetState="+ targetState)
        .append(", description="+ description)
        .append(", stateSyntax="+ stateSyntax)
        .append(", createDateTm="+ createdDateTime)
        .append(", lastFailDateTm="+ lastFailDateTime)
        .append(", condFailMsg="+ condFailMsg)
        .append(", icon="+ icon)
        .append(", config="+ config)
        .append(", validity="+validity)
        .append(", marketUrl="+ marketUrl);

        return builder.toString();
    }


    /** This method returns the mapping Table between
     *  XML Nodes and Smart Rules DB fields for Condition table
     *
     * @return the conditionInfo map
     */
    public static HashMap<String, String> getConditionInfoMap() {

    	if(conditionInfo == null) {
    		initConditionInfoMap();
    	} 
    	
    	return conditionInfo;
    }


    /** This method initializes the mapping Table between
     *  XML Nodes and Smart Rules DB fields for Condition table
     */
    private static void initConditionInfoMap() {

        // This mapping is needed since the <CONDITION> node in the XML will contain
        // only specific parameters from the Condition Table with respect to a Rule

        conditionInfo = new HashMap<String, String>();
        conditionInfo.put(XmlConstants.PUBLISHER_KEY, ConditionTable.Columns.CONDITION_PUBLISHER_KEY);
        conditionInfo.put(XmlConstants.ENABLED, ConditionTable.Columns.ENABLED);
        conditionInfo.put(XmlConstants.CONFIG, ConditionTable.Columns.CONDITION_CONFIG);
        conditionInfo.put(XmlConstants.SUGGESTED_STATE, ConditionTable.Columns.SUGGESTED_STATE);
        conditionInfo.put(XmlConstants.SUGGESTED_REASON, ConditionTable.Columns.SUGGESTED_REASON);
        conditionInfo.put(XmlConstants.DOWNLOAD_URL, ConditionTable.Columns.CONDITION_MARKET_URL);
    }


    /** This method creates the XML String for the content of <CONDITION>
     *  node with respect to Action rules
     *
     *  @param serializer - the XML Serializer
     *  @param conditionCursor - cursor with respect to the Condition Table
     */
    public static void createXmlStringForCondition(XmlSerializer serializer, final Cursor conditionCursor) throws Exception {
    	
    	if((serializer == null) || (conditionCursor == null)) {
    		Log.e(TAG, "Xml Serializer or Condition Cursor is null");
    	} else {
	        // This creates the content of <CONDITION> tag.
	        // <CONDITION> has already been written into the XmlSerializer before this
	
	        Iterator<Entry<String, String>> iter = getConditionInfoMap().entrySet().iterator();
	
	        while (iter.hasNext()) {
	            Entry<String, String> entry = iter.next();
	            serializer.startTag(null, entry.getKey());
	            String dbData = null;
	
	            serializer.text(TextUtils.isEmpty(dbData = conditionCursor.getString(conditionCursor.
	            		getColumnIndexOrThrow(entry.getValue()))) ? "null" : dbData);
	
	            serializer.endTag(null, entry.getKey());
	            serializer.text("\n");
	        }
	    }
    }   
}
