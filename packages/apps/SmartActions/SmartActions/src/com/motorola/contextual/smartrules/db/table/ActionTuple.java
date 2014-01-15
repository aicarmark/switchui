/*
 * @(#)ActionTuple.java
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

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlSerializer;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants;


/**This class abstracts or encapsulates one row of the Action table.
 *
 * The Action table is used here to hold a system action (typically implemented in
 * the Smart Actions process).
 *
 *<code><pre>
 * CLASS:
 * 	Extends TupleBase which implements the row id (_id) field of the tuple.
 * 	Implements Parcelable to allow bundling.
 * 	Implements Comparable to allow sorting.
 * 	Implements Cloneable to allow cloning of records.
 *
 * RESPONSIBILITIES:
 * 	create instance from fields or from a parcel.
 *
 * COLABORATORS:
 * 	QuickActions - implements the actions available across the system
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class ActionTuple extends TupleBase implements Cloneable, Parcelable, 
													  Comparable<ActionTuple>, Constants {

    private static final String TAG = ActionTuple.class.getSimpleName();

    /** foreign key pointing to rule _id field */
    private   long	 	parentFk;
    /** 0=inactive(visually disconnected), 1=active(visually connected), 
     *  3=suggested(suggested, but never connected)  */
    private   int	 	enabled;
    /**  fired action, currently active setting or mode */
    private   int	 	active;
    /** 0=loser of the conflict resolution, 1=winner of conflict resolution */
    private   int		confWinner;
    /**  1= fire this action only when the mode exits, any other value means action is fired
     * when the mode enters */
    private   int	 	onExitModeFlag;
    /** 0=accepted, 1=unread, 2=read */
    private   int		suggState;
    /** Suggested XML &ltREASON&gt */
    private	  String	suggReason;
    /** description for the user of the action that will occur when this action is fired */
    private   String	description;
    /** publisher key of the owner of the action implementation  */
    private   String	publisherKey;
    /** flag indicating whether the action has states, stateless(0) or stateful (1) 
     *  or unknown (-1). for example, the action "set Wifi ON" is stateful - wifi is 
     *  either at ON or OFF. However, a send SMS action is not stateful (has no beginning 
     *  or end or state). */
    private   int		modality;
    /** Name of the state machine for this action 
     *  (@link ActionTable.Columns.STATE_MACHINE_NAME for more info)  */
    private   String	stateMachineName;
    /** target or desired state of the state machine for this action
     * see (@link ActionTable.Columns.TARGET_STATE) for more info)
     * NOTE: This column is deprecated as of version 35 of the DB. In the new Action Publisher
     * architecture, CONFIG holds the state machine value.
     * */
    @Deprecated
    private   String	targetState;
    /** uri required to fire the action
     * NOTE: This column is deprecated as of version 35 of the DB. In the new Action Publisher
     * architecture,  CONFIG holds the information which is necessary to launch the Action
     * Publisher with correct configuration.
     * */
    @Deprecated
    private   String	uri;
    /** intent to fire the activity to customize the action (if applicable)  */
    private   String	activityIntent;
    /** Action Rule syntax
     * NOTE: This column is deprecated as of version 35 of the DB. In the new Action Publisher
     * architecture, CONFIG holds all the configuration information
     * */
    @Deprecated
    private   String	actionSyntax;
    /** The original created date and time of this record. */
    private   long 		lastFiredDateTime;
    /** Failure Message */
    private   String	actFailMsg;
    /** 3 icon paths, like this (in XML format) <ldpi>, <mdpi>, <hdpi> */
    private   String	icon;
    /** config details */
    private   String	config;
    /** Validity of the action publisher.  */
    private   String	validity;
    /** Market Url to download this action publisher */
    private   String	marketUrl;
    /** Rule Key of the child rule with which this action record is associated with.
     *  This key is valid only for action publishers that have a child rule else should
     *  be null. */
    private   String    childRuleKey;

    //Node Name & Action Table mapping for <ACTION>
    //and is used to convert Action Table into XML String
    private static HashMap<String, String> actionInfo;


    /** Basic constructor, constructs record class */
    public ActionTuple() {
        super();
        this.dirtyFlag = false;
        modality = ActionTable.Modality.UNKNOWN;
    }


    /** Constructor make a copy (clone) of the record
     *
     * @param tuple
     */
    public ActionTuple(ActionTuple tuple) {
        super();
        copy(tuple);
        this.dirtyFlag = false;
    }


    /** constructs record class */
    public ActionTuple(			long 	_id,
                                long 	parentFk,
                                int		enabled,
                                int		active,
                                int     confWinner,
                                int		onExitModeFlag,
                                int		suggState,
                                final   String	suggReason,
                                final 	String 	desc,
                                final 	String 	publisherKey,
                                final 	int		modality,
                                final 	String 	stateMachineName,
                                final   String  targetState,
                                final 	String 	uri,
                                final 	String 	activityIntent,
                                final 	String 	actionSyntax,
                                long 	lastFiredDateTime,
                                final	String	actFailMsg,
                                final 	String 	icon,
                                final   String  config,
                                final   String  validity,
                                final   String  marketUrl,
                                final   String  childRuleKey) {

        super();
        this._id 				= _id;
        this.parentFk 			= parentFk;
        this.enabled			= enabled;
        this.confWinner			= confWinner;
        this.active 			= active;
        this.onExitModeFlag		= onExitModeFlag;
        this.suggState			= suggState;
        this.suggReason			= suggReason;
        this.description 		= desc;
        this.publisherKey 		= publisherKey;
        this.modality			= modality;
        this.stateMachineName 	= stateMachineName;
        this.targetState 		= targetState;
        this.uri 				= uri;
        this.activityIntent		= activityIntent;
        this.actionSyntax		= actionSyntax;
        setLastFiredDateTime(lastFiredDateTime);
        this.actFailMsg         = actFailMsg;
        this.icon 				= icon;
        this.dirtyFlag 			= false;
        this.config	 			= config;
        this.validity 			= validity;
        this.marketUrl 			= marketUrl;
        this.childRuleKey 		= childRuleKey;
    }


    /** constructor for parcel re-inflate.
     *
     * @param parcel to reconstruct the instance
     */
    public ActionTuple(Parcel parcel) {

        super(parcel);
    }

    /** clone a tuple.
     *
     * @param tuple - tuple to be cloned.
     * @return - new instance cloned from the old one.
     */
    public ActionTuple copy(ActionTuple tuple) {

    	super.copy(tuple);
        this.parentFk 			= tuple.parentFk;
        this.enabled			= tuple.enabled;
        this.active 			= tuple.active;
        this.confWinner			= tuple.confWinner;
        this.onExitModeFlag		= tuple.onExitModeFlag;
        this.suggState			= tuple.suggState;
        this.suggReason			= tuple.suggReason;
        this.description		= tuple.description;
        this.publisherKey		= tuple.publisherKey;
        this.modality			= tuple.modality;
        this.stateMachineName	= tuple.stateMachineName;
        this.targetState		= tuple.targetState;
        this.uri 				= tuple.uri;
        this.activityIntent		= tuple.activityIntent;
        this.actionSyntax		= tuple.actionSyntax;
        this.actFailMsg			= tuple.actFailMsg;
        this.icon 				= tuple.icon;
        this.config             = tuple.config;
        this.validity           = tuple.validity;
        this.marketUrl          = tuple.marketUrl;
        this.childRuleKey		= tuple.childRuleKey;


        this.setLastFiredDateTime(0);
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
    public static final Parcelable.Creator<ActionTuple> CREATOR =
    new Parcelable.Creator<ActionTuple>() {

        public ActionTuple createFromParcel(Parcel in) {
            return new ActionTuple(in);
        }

        public ActionTuple [] newArray(int size) {
            return new ActionTuple[size];
        }
    };


    /** getter - foreign key to the rule _id field/column
     *
     * @return the ruleFk
     */
    public long getParentFk() {
        return parentFk;
    }


    /** setter - foreign key to the rule _id field/column
     *
     * @param fk - the ruleFk to set
     */
    public ActionTuple setParentFk(long fk) {

        this.parentFk = fk;
        this.dirtyFlag = true;
        return this;
    }


    /** getter - enabled (1)  means the action will be fired when the rule becomes active
     * 	and disabled (0) means the action will be ignored
     * 
     * @return the enabled - 1=enabled or 0=disabled
     */
    public int getEnabled() {
        return enabled;
    }

 
    /** setter - enabled (enabled (1)  means the action will be fired when the rule becomes 
     *  active and disabled (0) means the action will be ignored)
     * 
     * @param enabled - 1=enabled or 0=disabled
     */
    public void setEnabled(int enabled) {
        this.enabled = enabled;
        this.dirtyFlag = true;
    }


    /** getter - active (1)  means the action is currently controlled by Smart Actions and
     *  will show up on the conflict stack, inactive (0) means not controlled and will not
     *  show on the conflict stack.
     * 
     * @return the active  - 1=active or 0=inactive
     */
    public int getActive() {
        return active;
    }


    /** setter - active (active (1)  means the action is currently controlled by 
     *  Smart Actions and will show up on the conflict stack, inactive (0) means not 
     *  controlled and will not show on the conflict stack)
     * 
     * @param active - 1=active or 0=inactive
     */
    public void setActive(int active) {
        this.active = active;
        this.dirtyFlag = true;
    }


    /** getter - @see ActionTable.Columns.CONFLICT_WINNER_FLAG
     * 
     * @return the confWinner
     */
    public int getConfWinner() {
        return confWinner;
    }

    
    /** setter - confWinner (@see ActionTable.Columns.CONFLICT_WINNER_FLAG)
     * 
     * @param confWinner - @see ActionTable.Columns.CONFLICT_WINNER_FLAG
     */
    public void setConfWinner(int confWinner) {
        this.confWinner = confWinner;
        this.dirtyFlag = true;
    }

    
    /** getter @see ActionTable.Columns.ON_MODE_EXIT (ON_ENTER (0) means this action is
     *  fired when the rule becomes active and ON_EXIT (1) means this action is fired 
     *  when the rule becomes inactive.
     *
     * @return onExitModeFlag value (true or false)
     */
    public boolean isOnExitModeAction() {
        return onExitModeFlag == 1;
    }

    
    /** getter @see ActionTable.Columns.ON_MODE_EXIT (ON_ENTER (0) means this action is
     *  fired when the rule becomes active and ON_EXIT (1) means this action is fired 
     *  when the rule becomes inactive.
     * 
     * @return the onExitModeFlag
     */
    public int getOnExitModeFlag() {
        return onExitModeFlag;
    }


    /** setter - onExitModeFlag (@see ActionTable.Columns.ON_MODE_EXIT (ON_ENTER (0) 
     *  means this action is fired when the rule becomes active and ON_EXIT (1) means 
     *  this action is fired when the rule becomes inactive.)
     * 
     * @param onExitModeFlag - @see ActionTable.Columns.ON_MODE_EXIT (ON_ENTER (0) 
     *  means this action is fired when the rule becomes active and ON_EXIT (1) means 
     *  this action is fired when the rule becomes inactive.
     */
    public void setOnExitModeFlag(int onExitModeFlag) {
        this.onExitModeFlag = onExitModeFlag;
        this.dirtyFlag = true;
    }

    
    /** getter - 0=accepted suggested action, 1=unread suggested action, 
     * 	2=read suggested action
     * 
     * @return the suggState - 0=accepted, 1=unread, 2=read
     */
    public int getSuggState() {
        return suggState;
    }
    

    /** setter - suggState (0=accepted suggested action, 1=unread suggested action, 
     * 	2=read suggested action)
     * 
     * @param suggState - 0=accepted, 1=unread, 2=read
     */
    public void setSuggState(int suggState) {
        this.suggState = suggState;
        this.dirtyFlag = true;
    }


    /** getter - reason why this action is suggested
     * 
     * @return the suggReason
     */
    public String getSuggReason() {
        return suggReason;
    }


    /** setter - the reason to suggest the action
     * 
     * @param suggReason - the suggReason to set
     */
    public void setSuggReason(String suggReason) {
        this.suggReason = suggReason;
        this.dirtyFlag = true;
    }

    
    /** getter - description of the action
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }


    /** setter - description of the action
     * 
     * @param description - the description to set
     */
    public void setDescription(String description) {
        this.description = description;
        this.dirtyFlag = true;
    }


    /** getter - unique key to identify the action publisher 
     * 
     * @return the publisherKey
     */
    public String getPublisherKey() {
        return publisherKey;
    }


    /** setter - publisherKey (unique Key to identify the action publisher)
     * 
     * @param publisherKey - unique Key to identify the action publisher
     */
    public void setPublisherKey(String publisherKey) {
        this.publisherKey = publisherKey;
        this.dirtyFlag = true;
    }


    /** getter - indicates if the action is stateful (0) or stateless (1).
  	 * 	for example, the action "set Wifi ON" is stateful - wifi is either at ON or OFF.
     *  However, a send SMS action is not stateful (has no beginning or end or state).
     * 
     * @return the modality - 0=stateful or 1=stateless (1)
     */
    public int getModality() {
        return modality;
    }


    /** setter - modality to set (stateful (0) or stateless (1))
     * 
     * @param modality - 0=stateful or 1=stateless (1).
     */
    public void setModality(int modality) {
        this.modality = modality;
        this.dirtyFlag = true;
    }


    /** getter - returns the user visible name for the action 
     *  (for example like GPS, Wi-Fi etc...)
     * 
     * @return the stateMachineName
     */
    public String getStateMachineName() {
        return stateMachineName;
    }


    /** setter - stateMachineName the user visible name for the action
     * 
     * @param stateMachineName the user visible name for the action to set
     */
    public void setStateMachineName(String stateMachineName) {
        this.stateMachineName = stateMachineName;
        this.dirtyFlag = true;
    }


    /** getter - the state which the rule is going to set it to when the action gets active
     * 
     * @return the targetState
     */
    public String getTargetState() {
        return targetState;
    }


    /** setter - targetState
     * 
     * @param targetState the targetState to set
     */
    public void setTargetState(String targetState) {
        this.targetState = targetState;
        this.dirtyFlag = true;
    }

    /** getter - URI to fire to invoke the desired action.
     * 
     * @return the URI to fire to invoke the desired action
     */
    public String getUri() {
        return uri;
    }


    /** setter - uri the URI to fire to invoke the desired action
     * 
     * @param uri the URI to fire to invoke the desired action.
     */
    public void setUri(String uri) {
        this.uri = uri;
        this.dirtyFlag = true;
    }



    /** getter - Intent to fire to change or customize the action, set to null if no 
     * 	configuration required.
     * 
     * @return the activityIntent to fire for change or customize of the action
     */
    public String getActivityIntent() {
        return activityIntent;
    }


    /** setter - activityIntent the intent to fire to change or customize the action, 
     * set to null if no configuration required.
     * 
     * @param activityIntent the intent to fire to change or customize the action, 
     * set to null if no configuration required.
     */
    public void setActivityIntent(String activityIntent) {
        this.activityIntent = activityIntent;
        this.dirtyFlag = true;
    }


    /** getter - actionSyntax (used by rule importer to set the XML syntax)
     * 
     * @return the actionSyntax
     */
    public String getActionSyntax() {
        return actionSyntax;
    }


    /** setter actionSyntax the actionSyntax to set by the rule importer
     * 
     * @param actionSyntax the actionSyntax to set by the rule importer
     */
    public void setActionSyntax(String actionSyntax) {
        this.actionSyntax = actionSyntax;
        this.dirtyFlag = true;
    }


    /** getter - see java.util.Date.getTime() - last time the action was invoked 
     *  via the URI (@see ActionTable.Columns.URI_TO_FIRE)
     *
     * @return the lastFiredDateTime dateTime
     */
    public long getLastFiredDateTime() {
        return lastFiredDateTime;
    }


    /** setter - fixDateTime dateTime the (see #java.util.Date.getTime()) action 
     *  was invoked via the URI (@see ActionTable.Columns.URI_TO_FIRE)
     *
     * @param dateTime the (see #java.util.Date.getTime()) action was invoked via 
     * the URI (@see ActionTable.Columns.URI_TO_FIRE)
     */
    public void setLastFiredDateTime(long dateTime) {

        if (dateTime < 1)
            this.lastFiredDateTime = new Date().getTime();
        else
            this.lastFiredDateTime = dateTime;
        this.dirtyFlag = true;
    }

    
    /** clears the lastFiredDateTime value
     */
    public void clearLastFiredDateTime() {
    	this.lastFiredDateTime = 0;
    }

    
    /** getter - failure Message when the action failed to act on a request.
     * 
     * @return the actFailMsg when the action failed
     */
    public String getActFailMsg() {
        return actFailMsg;
    }
    

    /** setter - actFailMsg the failure message to set when the action failed to act on a
     * request
     * 
     * @param actFailMsg the failure message to set when the action failed to act on a
     * request
     */
    public void setActFailMsg(String actFailMsg) {
        this.actFailMsg = actFailMsg;
        this.dirtyFlag = true;
    }


    /** getter - icon used for the action
     * 
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }


    /** setter - icon of the action
     * 
     * @param icon the icon to set for the action
     */
    public void setIcon(String icon) {
        this.icon = icon;
        this.dirtyFlag = true;
    }
    

    /**
     * @return the config
     */
    public String getConfig() {
        return config;
    }


    /**
     * @param config the config to set
     */
    public void setConfig(String config) {
        this.config = config;
    }


    /**
     * @return the validity
     */
    public String getValidity() {
        return validity;
    }


    /**
     * @param validity the validity to set
     */
    public void setValidity(String validity) {
        this.validity = validity;
    }

    /**
     * @return the marketUrl
     */
    public String getMarketUrl() {
        return marketUrl;
    }


    /**
     * @param marketUrl the marketUrl to set
     */
    public void setMarketUrl(String marketUrl) {
        this.marketUrl = marketUrl;
    }

    /** getter - childRuleKey
     * 
	 * @return the childRuleKey
	 */
	public String getChildRuleKey() {
		return childRuleKey;
	}


	/** setter - childRuleKey
	 * 
	 * @param childRuleKey the childRuleKey to set
	 */
	public void setChildRuleKey(String childRuleKey) {
		this.childRuleKey = childRuleKey;
	}
	


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof ActionTuple)
            return compareTo((ActionTuple)o) == 0;
        else
            return false;
    }


    @Override
    public int hashCode() {
        int result = Hash.SEED;
        result = Hash.hash( result, _id );
        result = Hash.hash( result, parentFk );
        result = Hash.hash( result, active );
        result = Hash.hash( result, confWinner );
        result = Hash.hash( result, onExitModeFlag );
        result = Hash.hash( result, enabled );
        result = Hash.hash( result, modality );
        result = Hash.hash( result, suggState );
        result = Hash.hash( result, suggReason );
        result = Hash.hash( result, description );
        result = Hash.hash( result, description );
        result = Hash.hash( result, publisherKey );
        result = Hash.hash( result, stateMachineName );
        result = Hash.hash( result, targetState );
        result = Hash.hash( result, uri );
        result = Hash.hash( result, activityIntent );
        result = Hash.hash( result, actionSyntax );
        result = Hash.hash( result, actFailMsg );
        result = Hash.hash( result, icon );
        result = Hash.hash( result, logicalDelete);
        result = Hash.hash( result, dirtyFlag);
        result = Hash.hash( result, config );
        result = Hash.hash( result, validity );
        result = Hash.hash( result, marketUrl);
        result = Hash.hash( result, childRuleKey);
        return result;
    }


    /** Comparison method for equals, not really good for sorting.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ActionTuple another) {

        if (this._id 				!= another._id) return 1;
        if (this.parentFk 			!= another.parentFk) return 1;
        if (this.active 			!= another.active) return 1;
        if (this.confWinner			!= another.confWinner) return 1;
        if (this.onExitModeFlag		!= another.onExitModeFlag) return 1;
        if (this.enabled 			!= another.enabled) return 1;
        if (this.modality 			!= another.modality) return 1;
        if (this.suggState			!= another.suggState) return 1;

        if (this.suggReason 		== null && another.suggReason != null) return 1;
        if (this.suggReason         != null && !suggReason.equals(another.suggReason)) return 1;
        if (this.description 		== null && another.description != null) return 1;
        if (this.description 		!= null && !description.equals(another.description)) return 1;
        if (this.publisherKey 		== null && another.publisherKey != null) return 1;
        if (this.publisherKey 		!= null && !publisherKey.equals(another.stateMachineName)) return 1;
        if (this.stateMachineName 	== null && another.stateMachineName != null) return 1;
        if (this.stateMachineName 	!= null && !stateMachineName.equals(another.targetState)) return 1;
        if (this.targetState		== null && another.targetState != null) return 1;
        if (this.targetState 		!= null && !targetState.equals(another.targetState)) return 1;
        if (this.uri 				== null && another.uri != null) return 1;
        if (this.uri 				!= null && !uri.equals(another.uri)) return 1;


        if (this.activityIntent 	== null && another.activityIntent != null) return 1;
        if (this.activityIntent 	!= null && !activityIntent.equals(another.activityIntent)) return 1;

        if (this.actionSyntax 		== null && another.actionSyntax != null) return 1;
        if (this.actionSyntax 		!= null && !actionSyntax.equals(another.actionSyntax)) return 1;

        if (this.actFailMsg         == null && another.actFailMsg != null) return 1;
        if (this.actFailMsg         != null && actFailMsg.equals(another.actFailMsg)) return 1;

        if (this.icon 				== null && another.icon != null) return 1;
        if (this.icon 				!= null && !icon.equals(another.icon)) return 1;

        if (this.config 			== null && another.config != null) return 1;
        if (this.config 			!= null && !config.equals(another.config)) return 1;

        if (this.validity 			== null && another.validity != null) return 1;
        if (this.validity 			!= null && !validity.equals(another.validity)) return 1;

        if (this.marketUrl 			== null && another.marketUrl != null) return 1;
        if (this.marketUrl 			!= null && !marketUrl.equals(another.marketUrl)) return 1;

        if (this.childRuleKey       == null && another.childRuleKey != null) return 1;
        if (this.childRuleKey       != null && !childRuleKey.equals(another.childRuleKey)) return 1;


        return 0;
    }


    /** converts tuple to String mainly for debugging purposes.
     *
     * @see com.motorola.contextual.smartrules.db.table.TupleBase#toString()
     */
    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("  Action."+super.toString());

        builder.append(", parentFk="+ parentFk)
        .append(", active="+ active)
        .append(", confWinner="+ confWinner)
        .append(", onExitModeFlag="+ onExitModeFlag)
        .append(", suggState="+ suggState)
        .append(" suggReason="+ suggReason)
        .append(", enabled="+ enabled)
        .append(", desc="+ description)
        .append(", pubKey="+ publisherKey)
        .append(", modality="+ modality)
        .append(", stateMachineName="+ stateMachineName)
        .append(", targetState="+ targetState)
        .append(", uri="+ uri)
        .append(", activityIntent="+ activityIntent)
        .append(", actionSyntax="+ actionSyntax)
        .append(", lastFireDT="+ lastFiredDateTime)
        .append(", actFailMsg="+ actFailMsg)
        .append(", icon="+ icon)
        .append(", config="+ config)
        .append(", marketUrl="+ marketUrl)
        .append(", childRuleKey="+ childRuleKey);

        return builder.toString();
    }


    /** This method returns the mapping Table between
     *  XML Nodes and Smart Rules DB fields for Action table
     *
     * @return the actionInfo map
     */
    public static HashMap<String, String> getActionInfoMap() {

    	if(actionInfo == null) {
    		initActionInfoMap();
	}

    	return actionInfo;
    }

    /** This method initializes the mapping Table between
     *  XML Nodes and Smart Rules DB fields for Action table
     */
    private static void initActionInfoMap() {

        // This mapping is needed since the <ACTION> node in the XML will contain
        // only specific parameters from the Action Table with respect to a Rule

        actionInfo = new HashMap<String, String>();
        actionInfo.put(XmlConstants.ENABLED, ActionTable.Columns.ENABLED);
        actionInfo.put(XmlConstants.PUBLISHER_KEY, ActionTable.Columns.ACTION_PUBLISHER_KEY);
        actionInfo.put(XmlConstants.SUGGESTED_STATE, ActionTable.Columns.SUGGESTED_STATE);
        actionInfo.put(XmlConstants.SUGGESTED_REASON, ActionTable.Columns.SUGGESTED_REASON);
        actionInfo.put(XmlConstants.CONFIG, ActionTable.Columns.CONFIG);
        actionInfo.put(XmlConstants.DOWNLOAD_URL, ActionTable.Columns.MARKET_URL);
        actionInfo.put(XmlConstants.CHILD_RULE_KEY, ActionTable.Columns.CHILD_RULE_KEY);
    }


    /** This method creates the XML String for the content of <ACTION>
     *  node with respect to Action rules. Used for rules importer/exporter.
     *
     *  @param serializer - the XML Serializer
     *  @param actionCursor - cursor with respect to the Action Table
     */
    public static void createXmlStringForAction(XmlSerializer serializer, final Cursor actionCursor) throws Exception {

    	if((serializer == null) || (actionCursor == null)) {
    		Log.e(TAG, "Xml Serializer or Action Cursor is null");
    	} else {
	        //Start tag for <ACTION>
	        serializer.startTag(null, XmlConstants.ACTION);
	        serializer.text("\n");

	        Iterator<Entry<String, String>> iter = getActionInfoMap().entrySet().iterator();

	        while (iter.hasNext()) {
	            Entry<String, String> entry = iter.next();
	            String dbData = actionCursor.getString(actionCursor.getColumnIndex(entry.getValue()));
	            if ( dbData != null ) {
	                serializer.startTag(null, entry.getKey());

	                if(entry.getKey().equals(XmlConstants.ACTIVE)) {
	                    serializer.text(String.valueOf(ActionTable.Active.INACTIVE));
	                } else {
	                    serializer.text(dbData);
	                }
	                serializer.endTag(null, entry.getKey());
	                serializer.text("\n");
	            }
	        }

	        //End tag for <ACTION>
	        serializer.endTag(null, XmlConstants.ACTION);
	        serializer.text("\n\n");
    	}
    }
}