/*
 * @(#)RuleTuple.java
 *
 * (c) COPYRIGHT 2010 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2010/11/01 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlSerializer;

import com.motorola.contextual.smartrules.db.SQLiteManager;
import com.motorola.contextual.smartrules.db.table.RuleTable.RuleType;
import com.motorola.contextual.smartrules.db.table.RuleTable.Source;
import com.motorola.contextual.smartrules.db.table.RuleTable.SuggState;
import com.motorola.contextual.smartrules.rulesimporter.XmlConstants;
import com.motorola.contextual.smartrules.util.Util;

/**This class abstracts or encapsulates one row of the Rule table.
 *
 * The Rule table is used here to hold a Rule.
 *
 *<code><pre>
 * CLASS:
 * 	Extends TupleBase which implements the row id (_id) field of the tuple.
 * 	Implements Parcelable to allow bundling.
 * 	Implements Comparable to allow sorting.
 * 	Implements Cloneable to allow cloning of records.
 *
 * RESPONSIBILITIES:
 * 	create RuleTuple instance from fields or from a parcel.
 *
 * COLABORATORS:
 * 	RuleTable - which wraps all the table functions around this class.
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class RuleTuple extends TupleBase implements Cloneable, Parcelable, Comparable<RuleTuple> {

	private static final String TAG = RuleTuple.class.getSimpleName();

    /** whether the user enabled all rules or disabled all rules (see RuleTable) */
    private   int		enabled;
    /** user-ordered rank */
    private   float		rank;
    /** unique Key of the rule */
    private   String	key;
    /** currently active rule = 1, else 0 */
    private   int		active;
    /** automatic = 0, manual = 1, default = -1 */
    private   int       ruleType;
    /** source of this rule 0=USER, 1=COMMUNITY, 2=INFERRED, 3=SUGGESTED */
    private   int 		source;
    /** Community Rating  */
    private   int 		communityRating;
    /** Community Author contact info XML format, @see RuleTable.Columns.COMMUNITY_AUTHOR  */
    private   String 	communityAuthor;
    /** Flags can be in any order where: n=not editable, i=invisible (Defaults are "visible editable) */
    private   String 	flags;
    /** Currently not used - Common Name of this location */
    private   String 	name;
    /** Interleave of the Lat/Long digits to determine proximity from another location */
    private   String 	desc;
    /** DVS key or other information of interest, should be name-value pair format like DVSKEY=01221, etc.  */
    private   String 	tags;
    /** XML format and/or conditions between the Conditions, each having the ConditionTable key between each   */
    private   String 	conditions;
    /** Infer logic  */
    private   String 	inferLogic;
    /** Infer status  */
    private   String 	inferStatus;
    /** Suggested state */
    private   int       suggState;
    /** Suggested (XML format) &ltREASON&gt  */
    private   String 	suggested;
    /** -1=Immediate, 0=Never Expires, 1=One Time and can also be date for future use */
    private	  long		lifecycle;
    /** 0=Show, 1=Do Not Show */
    private   int		silent;
    /** VSensor
     * NOTE: This column is deprecated as of version 35 of the DB as Virtual Sensors
     * are no longer used */
    @Deprecated
    private   String 	vSensor;
    /** Rule Syntax  */
    private   String 	ruleSyntax;
    /** The date and time this rule was last active (set to 0 initially). */
    private   long 		lastActiveDateTime;
    /** The date and time this rule was last inactive (set to 0 initially). */
    private   long 		lastInactiveDateTime;
    /** The original created date and time. */
    private   long 		createdDateTime;
    /** The date and time this rule was last edited (set to createdDateTime initially). */
    private   long		lastEditedDateTime;
    /** 3 icon paths, like this (in XML format) <ldpi>, <mdpi>, <hdpi> */
    private   String	icon;
    /** Rule ID of the Sample Rule (Parent) from which this rule (Child) is derived or to store
     * the incremental count of the sample rule adopted. 
     * NOTE: This column is deprecated as of version 34 of the DB and is replaced
     * by the new column ADOPT_COUNT */
    @Deprecated
    private   long 		sampleFkOrCount;
    /** Publisher key*/
    private   String	publisherKey;
    /** Validity of the rule. If all the actions of this rule are available,
     * then this field is set to Valid */
    private   String	validity;
    /** Intent to be broadcasted to invoke the UI for this rule. Used by
     *  Suggestions module */
    private   String	uiIntent;
    /** Rule Key of the Parent from which this child rule is either derived from for
     * 	adopted Sample/Suggestion or for Battery Rules it is the parent to which it is
     *  linked. */    
    private	  String	parentRuleKey;
    /** Incremental counter to indicate the number of times the sample or suggested
     *  rule is adopted. The count value is the value of number of times the rule is
     *  adopted and not the number to be used for next adoption. To clarify, this
     *  value must be incremented before used for a new adoption. This column will 
     *  replace the SampleFkOrCount column. */
    private	  long		adoptCount;

    // Node Name & Rule Table mapping for <RULEINFO>
    // and is used to convert Rule Table into XML String
    private static HashMap<String, String> ruleInfo;

    /** Basic constructor, constructs Rule record class */
    public RuleTuple() {
        super();

        name = "";
        flags = "";
        setCreatedDateTime(new Date().getTime());
        this.dirtyFlag = false;
    }


    /** Constructor make a copy (clone) of the record
     *
     * @param tuple
     */
    public RuleTuple(RuleTuple tuple) {
        super();
        copy(tuple);
        this.dirtyFlag = false;
    }


    /** constructs Rule record class */
    public RuleTuple(			long 	_id,
                                int		enabled,
                                float	rank,
                                final 	String 	key,
                                int		active,
                                int 	ruleType,
                                int		source,
                                int		communityRating,
                                final	String	communityAuthor,
                                final	String	flags,
                                final 	String 	name,
                                final 	String 	desc,
                                final	String	tags,
                                final	String	conditions,
                                final	String	inferLogic,
                                final	String	inferStatus,
                                int		suggState,
                                final	String	suggested,
                                long	lifecycle,
                                int		silent,
                                final 	String 	vSensor,
                                final 	String 	ruleSyntax,
                                long 	lastActiveDateTime,
                                long 	lastInactiveDateTime,
                                long 	createdDateTime,
                                long	lastEditedDateTime,
                                final	String icon,
                                final   long sampleFkOrCount,
                                final   String publisherKey,
                                final   String validity,
                                final   String uiIntent,
                                final   String parentRuleKey,
                                long	adoptCount
                    ) {

        super();
        this._id = _id;
        this.enabled = enabled;
        this.rank = rank;
        this.key = key;
        this.active = active;
        this.ruleType = ruleType;
        this.source = source;
        this.communityRating = communityRating;
        this.communityAuthor = communityAuthor;
        this.flags = flags;
        this.name = name;
        this.desc = desc;
        this.tags = tags;
        this.conditions = conditions;
        this.inferLogic = inferLogic;
        this.inferStatus = inferStatus;
        this.suggState = suggState;
        this.suggested = suggested;
        this.lifecycle = lifecycle;
        this.silent = silent;
        this.vSensor = vSensor;
        this.ruleSyntax = ruleSyntax;
        this.lastActiveDateTime = lastActiveDateTime;
        this.lastInactiveDateTime = lastInactiveDateTime;
        this.lastEditedDateTime = lastEditedDateTime;
        setCreatedDateTime(createdDateTime);	
        this.icon = icon;
        this.sampleFkOrCount = sampleFkOrCount;
        this.dirtyFlag = false;
        this.publisherKey = publisherKey;
        this.validity = validity;
        this.uiIntent = uiIntent;
        this.parentRuleKey = parentRuleKey;
        this.adoptCount = adoptCount;
    }


    /** constructor for parcel re-inflate.
     *
     * @param parcel to reconstruct the instance
     */
    public RuleTuple(Parcel parcel) {

        super(parcel);
    }


    /** clone a tuple.
     *
     * @param tuple - tuple to be cloned.
     * @return - new instance cloned from the old one.
     */
    public RuleTuple copy(RuleTuple other) {

    	super.copy(other);
        // don't clone row Id as it would cause a duplicate _id primary key value
        this.enabled 			= other.enabled;
        this.rank 				= other.rank;
        this.key 				= other.key;
        this.active				= other.active;
        this.ruleType			= other.ruleType;
        this.source				= other.source;
        this.communityRating	= other.communityRating;
        this.communityAuthor	= other.communityAuthor;
        this.flags				= other.flags;
        this.name 				= other.name;
        this.desc 				= other.desc;
        this.tags				= other.tags;
        this.conditions			= other.conditions;
        this.inferLogic			= other.inferLogic;
        this.inferStatus		= other.inferStatus;
        this.suggState			= other.suggState;
        this.suggested			= other.suggested;
        this.lifecycle			= other.lifecycle;
        this.silent				= other.silent;
        this.vSensor			= other.vSensor;
        this.ruleSyntax			= other.ruleSyntax;
        this.lastActiveDateTime = other.lastActiveDateTime;
        this.lastInactiveDateTime = other.lastInactiveDateTime;
        this.lastEditedDateTime = other.lastEditedDateTime;
        setCreatedDateTime(createdDateTime);
        this.icon				= other.icon;
        this.sampleFkOrCount	= other.sampleFkOrCount;
        this.dirtyFlag = false;
        this.publisherKey       = other.publisherKey;
        this.validity           = other.validity;
        this.uiIntent           = other.uiIntent;
        this.parentRuleKey		= other.parentRuleKey;
        this.adoptCount			= other.adoptCount;
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
    public static final Parcelable.Creator<RuleTuple> CREATOR =
    new Parcelable.Creator<RuleTuple>() {

        public RuleTuple createFromParcel(Parcel in) {
            return new RuleTuple(in);
        }

        public RuleTuple [] newArray(int size) {
            return new RuleTuple[size];
        }
    };


    /** getter - enabled (1) means the rule automatically activated.
     *  disabled (0) means the rule requires manual interaction to activate the rule.
     * 
     * @return the enabled - 1=enabled or 0=disabled
     */
    public int getEnabled() {
        return enabled;
    }


    /** getter - returns if the user is enabled or disabled.
     *  
     * @return true if enabled else false
     */
    public boolean isEnabled() {
        return enabled==1;
    }


    /** setter - enabled (1) means the rule automatically activated.
     *  disabled (0) means the rule requires manual interaction to activate the rule.
     * 
     * @param enabled - 1=enabled or 0=disabled
     */
    public void setEnabled(int enabled) {
        this.enabled = enabled;
        this.dirtyFlag = true;
    }


    /** getter - user ordered ranking value for the rule
     * 
     * @return the rank set by the user for the rule
     */
    public float getRank() {
        return rank;
    }


    /** setter - user ordered ranking value for the rule
     * 
     * @param rank the rank to set for the rule
     */
    public void setRank(float rank) {
        this.rank = rank;
        this.dirtyFlag = true;
    }


    /** getter - unique rule key
     * 
     * @return the key for the rule
     */
    public String getKey() {
        return key;
    }


    /** setter - unique rule key to be set
     * 
     * @param key the unique key to set
     */
    public void setKey(String key) {
        this.key = key;
        this.dirtyFlag = true;
    }


    /** getter - to indicate if the rule is active or inactive
     * 
     * @return true if the rule is active, else false
     */
    public boolean isActive() {
        return active==1;
    }


    /** getter - active (1)  means the rule is currently active and inactive (0) means the
     *  rule is in ready (Automatic) or disabled (Manual) state.
     *  
     * @return the active - 1=active or 0=inactive
     */
    public int getActive() {
        return active;
    }


    /** setter - active (1)  means the rule is currently active and inactive (0) means the
     *  rule is in ready (Automatic) or disabled (Manual) state.
     *  
     * @param active the active to set (1=active or 0=inactive)
     */
    public void setActive(int active) {
        this.active = active;
        this.dirtyFlag = true;
    }


    /** getter - to indicate the type of rule i.e. Manual (1) or Automatic (0) 
     * 	or Default (-1)
     * 
     * @return the ruleType - 0=Automatic, 1=Manual and -1=Default
     */
    public int getRuleType() {
        return ruleType;
    }

    /** setter - the rule type to set 0=Automatic, 1=Manual and -1=Default
     * 
     * @param ruleType the ryleType to set (0=Automatic, 1=Manual and -1=Default)
     */
    public void setRuleType(int ruleType) {
        this.ruleType = ruleType;
        this.dirtyFlag = true;
    }

    /** getter - returns the flags associated with the rule (invisible (i), non-editable (n)
     * 	source list visible (s)
     * 
     * @return the flags associated with the rule (i=invisible, n=non-editable,
     * 	s=source list visible)
     */
    public String getFlags() {
        return flags;
    }


    /** setter - flags for the rule (i=invisible, n=non-editable, s=source list visible
     * 
     * @param flags the flags to set ((i=invisible, n=non-editable, s=source list visible)
     */
    public void setFlags(String flags) {
        this.flags = flags;
        this.dirtyFlag = true;
    }


    /** getter - source for the rule @see RuleTable.Columns.Source
     * 
     * @return the source of the rule
     */
    public int getSource() {
        return source;
    }


    /** setter - the source of the rule @see RuleTable.Columns.Source
     * 
     * @param source the source to set @see RuleTable.Columns.Source
     */
    public void setSource(int source) {
        this.source = source;
        this.dirtyFlag = true;
    }


    /** getter - rating for the rule from the community
     * 
     * @return the communityRating for the rule
     */
    public int getCommunityRating() {
        return communityRating;
    }


    /** setter - the rating for the rule
     * 
     * @param communityRating the communityRating to set
     */
    public void setCommunityRating(int communityRating) {
        this.communityRating = communityRating;
        this.dirtyFlag = true;
    }


    /** getter - the author of the rule
     * 
     * @return the  author for the rule
     */
    public String getCommunityAuthor() {
        return communityAuthor;
    }


    /** setter - the author/creator of the rule
     * 
     * @param communityAuthor - the author of the rule
     */
    public void setCommunityAuthor(String communityAuthor) {
        this.communityAuthor = communityAuthor;
        this.dirtyFlag = true;
    }


    /** getter - name of the rule.
     *
     * @return the name of the rule
     */
    public String getName() {
        return name;
    }


    /** setter - name of the rule
     *
     * @param commonName the name of the rule to set
     */
    public void setName(String commonName) {
        this.name = commonName;
        this.dirtyFlag = true;
    }


    /** getter - description of the rule
     * 
     * @return the description of the rule
     */
    public String getDesc() {
        return desc;
    }


    /** setter - description of the rule
     *
     * @param desc - the desc to set
     */
    public void setDesc(String desc) {
        this.desc = desc;
        this.dirtyFlag = true;
    }


    /** getter - any tags (miscellaneous information) associated with the rule
     * 
     * @return the tags (miscellaneous information) associated with the rule
     */
    public String getTags() {
        return tags;
    }


    /** setter - to set the tags (miscellaneous information) for the rule
     * 
     * @param tags the miscellaneous information to set
     */
    public void setTags(String tags) {
        this.tags = tags;
        this.dirtyFlag = true;
    }


    /** setter - the XML format and/or conditions between the Conditions
     * 
     * @return the and/or conditions in XML format
     */
    public String getConditions() {
        return conditions;
    }


    /** setter - XML format and/or conditions between the Conditions
     * 
     * @param conditions - the XML format and/or conditions between the Conditions to set
     */
    public void setConditions(String conditions) {
        this.conditions = conditions;
        this.dirtyFlag = true;
    }


    /** getter - inference logic associated with the rule in XML format
     * 
     * @return the inferLogic in XML format
     */
    public String getInferLogic() {
        return inferLogic;
    }


    /** setter - inference logic associated with the rule in XML format
     * 
     * @param inferLogic the inferLogic to set in XML format
     */
    public void setInferLogic(String inferLogic) {
        this.inferLogic = inferLogic;
        this.dirtyFlag = true;
    }


    /** getter - status of the inference
     * 
     * @return the inferStatus - the inference status
     */
    public String getInferStatus() {
        return inferStatus;
    }


    /** setter - inference status
     * 
     * @param inferStatus the inferStatus to set
     */
    public void setInferStatus(String inferStatus) {
        this.inferStatus = inferStatus;
        this.dirtyFlag = true;
    }


    /** getter - 0=accepted suggested rule, 1=unread suggested rule, 
     * 	2=read suggested rule
     * 
     * @return the suggState - 0=accepted, 1=unread, 2=read
     */
    public int getSuggState() {
        return suggState;
    }


    /** setter - suggState (0=accepted suggested rule, 1=unread suggested rule, 
     * 	2=read suggested rule)
     * 
     * @param suggState - 0=accepted, 1=unread, 2=read
     */
    public void setSuggState(int suggState) {
        this.suggState = suggState;
        this.dirtyFlag = true;
    }


    /** getter - XML format of reason the rule is suggested
     * 
     * @return the suggested reason in XML format
     */
    public String getSuggested() {
        return suggested;
    }


    /** setter - XML format for the suggestion
     * 
     * @param suggested the XML format reason to set
     */
    public void setSuggested(String suggested) {
        this.suggested = suggested;
        this.dirtyFlag = true;
    }


    /** getter - lifecycle for the suggestion @see RuleTable.Columns.LifeCycle
     * 
     * @return the lifecycle - @see RuleTable.Columns.LifeCycle
     */
    public long getLifecycle() {
        return lifecycle;
    }


    /** setter - the lifecycle for the suggestion @see RuleTable.Columns.LifeCycle
     * 
     * @param lifecycle the lifecycle to set for the suggestion
     *  	@see RuleTable.Columns.LifeCycle
     */
    public void setLifecycle(long lifecycle) {
        this.lifecycle = lifecycle;
        this.dirtyFlag = true;
    }


    /** getter - indicator to let the user know 'tell user' (0) or not silent (1) when rule 
     * 	becomes active and inactive @see RuleTable.Columns.Silent
     * 		
     * @return the silent - 0=tell user or 1=silent
     */
    public int getSilent() {
        return silent;
    }


    /** setter - indicator to let the user know 'tell user' (0) or not silent (1) when rule 
     * 	becomes active and inactive @see RuleTable.Columns.Silent
     * 
     * @param silent - 0=tell user or 1=silent
     */
    public void setSilent(int silent) {
        this.silent = silent;
        this.dirtyFlag = true;
    }


    /** getter - rule level sensor
     * 
     * @return the rule level sensor
     */
    public String getVSensor() {
        return vSensor;
    }


    /** setter - to set the rule level sensor
     * 
     * @param vSensor the rule level sensor to set
     */
    public void setVSensor(String vSensor) {
        this.vSensor = vSensor;
        this.dirtyFlag = true;
    }


    /** getter - XML format string for rule syntax
     * 
     * @return the ruleSyntax in XML format
     */
    public String getRuleSyntax() {
        return ruleSyntax;
    }


    /** setter - the XML format string to indicate the rule syntax
     * 
     * @param ruleSyntax the ruleSyntax to set in XML format
     */
    public void setRuleSyntax(String ruleSyntax) {
        this.ruleSyntax = ruleSyntax;
        this.dirtyFlag = true;
    }


    /** getter - the date time when the rule was last active (see java.util.Date.getTime())
     * 
     * @return the date and time when the rule was last active
     */
    public long getLastActiveDateTime() {
        return lastActiveDateTime;
    }


    /** setter - the date time when the rule was last active (see java.util.Date.getTime())
     * 
     * @param lastActiveDateTime the date time to set
     */
    public void setLastActiveDateTime(long lastActiveDateTime) {
        this.lastActiveDateTime = lastActiveDateTime;
        this.dirtyFlag = true;
    }


    /** getter - the date time when the rule was last inactive (see java.util.Date.getTime())
     * 
     * @return the date and time when the rule was last inactive
     */
    public long getLastInactiveDateTime() {
        return lastInactiveDateTime;
    }


    /** setter - the date time when the rule was last inactive (see java.util.Date.getTime())
     * 
     * @param lastInactiveDateTime the date time to set
     */
    public void setLastInactiveDateTime(long lastInactiveDateTime) {
        this.lastInactiveDateTime = lastInactiveDateTime;
        this.dirtyFlag = true;
    }


    /** getter - the date time when the rule was created (see java.util.Date.getTime())
     * 
     * @return the date and time when the rule was created
     */
    public long getCreatedDateTime() {
        return createdDateTime;
    }


    /** setter - the date time when the rule was last inactive (see java.util.Date.getTime())
     * 
     * @param fixDateTime the date time to set for the rule creation
     */
    public void setCreatedDateTime(long fixDateTime) {

        if (fixDateTime < 1)
            this.createdDateTime = new Date().getTime();
        else
            this.createdDateTime = fixDateTime;
        this.dirtyFlag = true;
    }
    


    /** getter - the date time when the rule was last edited (see java.util.Date.getTime())
     * 
     * @return the date and time when the rule was created
     */
	public long getLastEditedDateTime() {
		return lastEditedDateTime;
	}


    /** setter - the date time when the rule was last edited (see java.util.Date.getTime())
     * 
     * @param lastEditedDateTime the date time to set the rule was edited
     */
	public void setLastEditedDateTime(long lastEditedDateTime) {
		this.lastEditedDateTime = lastEditedDateTime;
        this.dirtyFlag = true;
	}
	

    /** getter - for rule icon
     * 
     * @return the icon used for the rule
     */
    public String getIcon() {
        return icon;
    }


    /** setter - the rule icon
     * 
     * @param icon the icon to set
     */
    public void setIcon(final String icon) {
        this.icon = icon;
        this.dirtyFlag = true;
    }

    /** getter - the _id to indicate the parent rule(for rule adopted by user from
     *  samples or suggestions) or the incremental count (for samples and suggestions)
     *  
	 * @return the the parent rule _id or incremental counter
	 */
	public long getSampleFkOrCount() {
		return sampleFkOrCount;
	}


	/** setter - to set the parent rule _id for user adopted samples/suggestions or the 
	 *  incremental counter for samples/suggestions
	 * 
	 * @param sampleFkOrCount the parent rule _id or incremental counter
	 */
	public void setSampleFkOrCount(long sampleFkOrCount) {
		this.sampleFkOrCount = sampleFkOrCount;
        this.dirtyFlag = true;
	}

	
    /**
     * @return the publisherKey
     */
    public String getPublisherKey() {
        return publisherKey;
    }


    /**
     * @param publisherKey the publisherKey to set
     */
    public void setPublisherKey(String publisherKey) {
        this.publisherKey = publisherKey;
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
     * @return the uiIntent
     */
    public String getUiIntent() {
        return uiIntent;
    }


    /**
     * @param uiIntent the uiIntent to set
     */
    public void setUiIntent(String uiIntent) {
        this.uiIntent = uiIntent;
    }

    
    /** getter - parentRuleKey
     * 
	 * @return the parentRuleKey
	 */
	public String getParentRuleKey() {
		return parentRuleKey;
	}


	/** setter - parentRuleKey
	 * 
	 * @param parentRuleKey the parentRuleKey to set
	 */
	public void setParentRuleKey(String parentRuleKey) {
		this.parentRuleKey = parentRuleKey;
	}
	
	
	/** getter - adoptCount
	 * 
	 * @return the adoptCount value
	 */
    public long getAdoptCount() {
		return adoptCount;
	}


    /** setter - adoptCount
     * 
     * @param adoptCount the adoptCount value to set
     */
	public void setAdoptCount(long adoptCount) {
		this.adoptCount = adoptCount;
	}


	/** getter - is this a valid rule
     * 
     * @return - true if VALIDITY is set to Validity.VALID
     */
    public boolean isValid() {
    	return this.validity.equals(TableBase.Validity.VALID);
    }
    
    /** getter - is this a suggested rule
     * 
     * @return - true if source is SUGGESTED else false
     */
    public boolean isSuggested() {
    	return this.source == Source.SUGGESTED;
    }

    /** getter - is this a sample rule
     * 
     * @return - true if source is FACTORY else false
     */
    public boolean isSample() {
    	return this.source == Source.FACTORY;
    }

    /** getter - is this an automatic rule
     * 
     * @return - true if ruleType is set to AUTOMATIC else false
     */
    public boolean isAutomatic() {
    	return this.ruleType == RuleType.AUTOMATIC;
    }
    
    /** getter - is this an adopted suggestion
     *  
     * @return - true if the rule is an adopted suggestion else false
     */
    public boolean isAdoptedSuggestion() {
    	return this.suggState == SuggState.ACCEPTED;
    }
    
    /** getter - is the rule edited since creation/adoption
     * 
     * @return - true if edited else false
     */
    public boolean isRuleEdited() {
    	return (this.lastEditedDateTime != 0 || (this.lastEditedDateTime - this.createdDateTime > 0));
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof RuleTuple)
            return compareTo((RuleTuple)o) == 0;
        else
            return false;
    }


    @Override
    public int hashCode() {
        int result = Hash.SEED;
        result = Hash.hash( result, _id );
        result = Hash.hash( result, enabled );
        result = Hash.hash( result, rank );
        result = Hash.hash( result, active );
        result = Hash.hash( result, ruleType );
        result = Hash.hash( result, source );
        result = Hash.hash( result, communityRating );
        result = Hash.hash( result, lastActiveDateTime );
        result = Hash.hash( result, lastInactiveDateTime );
        result = Hash.hash( result, createdDateTime );
        result = Hash.hash( result, lastEditedDateTime);
        result = Hash.hash( result, suggState );
        result = Hash.hash( result, lifecycle );
        result = Hash.hash( result, silent );
        result = Hash.hash( result, communityAuthor );
        result = Hash.hash( result, flags );
        result = Hash.hash( result, key );
        result = Hash.hash( result, name );
        result = Hash.hash( result, desc );
        result = Hash.hash( result, tags );
        result = Hash.hash( result, conditions );
        result = Hash.hash( result, inferLogic );
        result = Hash.hash( result, inferStatus );
        result = Hash.hash( result, suggested );
        result = Hash.hash( result, vSensor );
        result = Hash.hash( result, suggested );
        result = Hash.hash( result, ruleSyntax );
        result = Hash.hash( result, icon );
        result = Hash.hash( result, sampleFkOrCount);
        result = Hash.hash( result, logicalDelete);
        result = Hash.hash( result, dirtyFlag);
        result = Hash.hash( result, publisherKey);
        result = Hash.hash( result, validity);
        result = Hash.hash( result, uiIntent);
        result = Hash.hash( result, parentRuleKey);
        result = Hash.hash( result, adoptCount);
        return result;
    }


    /** Comparison method for equals, not really good for sorting.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(RuleTuple another) {

        if (this._id 				!= another._id) return 1;
        if (this.enabled 			!= another.enabled) return 1;
        if (this.rank 				!= another.rank) return 1;
        if (this.active 			!= another.active) return 1;
        if (this.ruleType			!= another.ruleType) return 1;
        if (this.source 			!= another.source) return 1;
        if (this.communityRating	!= another.communityRating) return 1;

        if (this.lastActiveDateTime != another.lastActiveDateTime) return 1;
        if (this.lastInactiveDateTime != another.lastInactiveDateTime) return 1;
        if (this.createdDateTime 	!= another.createdDateTime) return 1;
        if (this.lastEditedDateTime != another.lastEditedDateTime) return 1;
        if (this.suggState       	!= another.suggState) return 1;
        if (this.lifecycle 			!= another.lifecycle) return 1;
        if (this.silent 			!= another.silent) return 1;
        if (this.sampleFkOrCount    != another.sampleFkOrCount) return 1;
        if (this.adoptCount			!= another.adoptCount) return 1;
        
        if (this.communityAuthor == null && another.communityAuthor != null) return 1;
        if (this.communityAuthor != null && !communityAuthor.equals(another.communityAuthor)) return 1;

        if (this.flags 			== null && another.flags != null) return 1;
        if (this.flags 			!= null && !flags.equals(another.flags)) return 1;

        if (this.key 			== null && another.key != null) return 1;
        if (this.key 			!= null && !key.equals(another.key)) return 1;

        if (this.name 			== null && another.name != null) return 1;
        if (this.name 			!= null && !name.equals(another.name)) return 1;

        if (this.desc 			== null && another.desc != null) return 1;
        if (this.desc 			!= null && !desc.equals(another.desc)) return 1;

        // TODO: should check tags using some kind of loop as the name/value pairs 
        // determine whether or not the values are equal.
        if (this.tags 			== null && another.tags != null) return 1;
        if (this.tags 			!= null && !tags.equals(another.tags)) return 1;

        if (this.conditions 	== null && another.conditions != null) return 1;
        if (this.conditions 	!= null && !conditions.equals(another.conditions)) return 1;

        if (this.inferLogic 	== null && another.inferLogic != null) return 1;
        if (this.inferLogic 	!= null && !inferLogic.equals(another.inferLogic)) return 1;

        if (this.inferStatus 	== null && another.inferStatus != null) return 1;
        if (this.inferStatus 	!= null && !inferStatus.equals(another.inferStatus)) return 1;

        if (this.suggested 		== null && another.suggested != null) return 1;
        if (this.suggested 		!= null && !suggested.equals(another.suggested)) return 1;

        if (this.vSensor 		== null && another.vSensor != null) return 1;
        if (this.vSensor 		!= null && !vSensor.equals(another.vSensor)) return 1;

        if (this.ruleSyntax 	== null && another.ruleSyntax != null) return 1;
        if (this.ruleSyntax 	!= null && !ruleSyntax.equals(another.ruleSyntax)) return 1;

        if (this.icon 			== null && another.icon != null) return 1;
        if (this.icon 			!= null && !icon.equals(another.icon)) return 1;

        if (this.publisherKey	== null && another.publisherKey != null) return 1;
        if (this.publisherKey 	!= null && !publisherKey.equals(another.publisherKey)) return 1;

        if (this.validity 		== null && another.validity != null) return 1;
        if (this.validity 		!= null && !validity.equals(another.validity)) return 1;

        if (this.uiIntent 		== null && another.uiIntent != null) return 1;
        if (this.uiIntent 		!= null && !uiIntent.equals(another.uiIntent)) return 1;

        if (this.parentRuleKey  == null && another.parentRuleKey != null) return 1;
        if (this.parentRuleKey  != null && !parentRuleKey.equals(another.parentRuleKey)) return 1;

        return 0;
    }


    /** converts tuple to String mainly for debugging purposes.
     *
     * @see com.motorola.contextual.smartrules.db.table.TupleBase#toString()
     */
    public String toString() {

        StringBuilder builder = new StringBuilder("  Rule."+super.toString());

        builder.append(", enabled="+ enabled)
        .append(", rank="+ rank)
        .append(", key="+ key)
        .append(", ruleType="+ ruleType)
        .append(", source="+ source)
        .append(", commRating="+ communityRating)
        .append(", commAuthor="+ communityAuthor)
        .append(", flags="+ flags)
        .append(", act="+ active)
        .append(", nm="+ name)
        .append(", desc="+ desc)
        .append(", tags="+ tags)
        .append(", conditions="+ conditions)
        .append(", inferLogic="+ inferLogic)
        .append(", inferStatus="+ inferStatus)
        .append(", suggState="+ suggState)
        .append(", suggested="+ suggested)
        .append(", lifecycle="+ lifecycle)
        .append(", silent="+ silent)
        .append(", vSensor="+ vSensor)
        .append(", ruleSyntax="+ ruleSyntax)
        .append(", lastActDT="+ lastActiveDateTime)
        .append(", lastInactDT="+ lastInactiveDateTime)
        .append(", createDT="+ createdDateTime)
        .append(", lastEditedDT="+ lastEditedDateTime)
        .append(", icon="+ icon==null? "null": "present")
        .append(", sampleFkOrCount="+ sampleFkOrCount)
        .append(", publisherKey="+ publisherKey)
        .append(", validity="+ validity)
        .append(", uiIntent="+ uiIntent)
        .append(", parentRuleKey="+ parentRuleKey)
        .append(", adoptCount="+ adoptCount);

        return builder.toString();
    }



    /** This method returns the mapping Table between
     *  XML Nodes and Smart Rules DB fields for Rule table
     *
     * @return the ruleInfo map
     */
    public static HashMap<String, String> getRuleInfoMap() {
        
    	if(ruleInfo == null){
    		initRuleInfoMap();
    	}
    	
    	return ruleInfo;
    }


    /** This method initializes the mapping Table between
     *  XML Nodes and Smart Rules DB fields for Rule table
     */
    private static void initRuleInfoMap() {

        // Mapping table for XML Nodes & Smart Rules Table with respect to
        // a Rule.

        // This mapping is needed since the <RULEINFO> node in the XML will contain
        // only specific parameters from Rules Table with respect to a Rule

        ruleInfo = new HashMap<String, String>();
        ruleInfo.put(XmlConstants.IDENTIFIER, RuleTable.Columns.KEY);
        ruleInfo.put(XmlConstants.ENABLED, RuleTable.Columns.ENABLED);
        ruleInfo.put(XmlConstants.NAME, RuleTable.Columns.NAME);
        ruleInfo.put(XmlConstants.DESCRIPTION_NEW, RuleTable.Columns.DESC);
        ruleInfo.put(XmlConstants.FLAGS, RuleTable.Columns.FLAGS);
        ruleInfo.put(XmlConstants.TYPE, RuleTable.Columns.SOURCE);
        ruleInfo.put(XmlConstants.SUGGESTION_TYPE, RuleTable.Columns.LIFECYCLE);
        ruleInfo.put(XmlConstants.ICON, RuleTable.Columns.ICON);
        ruleInfo.put(XmlConstants.SILENT, RuleTable.Columns.SILENT);
        ruleInfo.put(XmlConstants.PUBLISHER_KEY, RuleTable.Columns.PUBLISHER_KEY);
        ruleInfo.put(XmlConstants.UI_INTENT, RuleTable.Columns.UI_INTENT);
        ruleInfo.put(XmlConstants.SUGGESTED_STATE, RuleTable.Columns.SUGGESTED_STATE);
        ruleInfo.put(XmlConstants.SUGGESTION_FREEFLOW, RuleTable.Columns.RULE_SYNTAX);
        ruleInfo.put(XmlConstants.PARENT_RULE_KEY, RuleTable.Columns.PARENT_RULE_KEY);
    }


    /** This method provides the XML String for <RULEINFO> node
     *  with respect to a rule
     *  @param serializer - the XML Serializer
     *  @param ruleCursor - cursor with respect to the Rule Table
     */

    public static void createXmlStringForRuleInfo(XmlSerializer serializer,final Cursor ruleCursor) throws Exception {
    	
    	if((serializer == null) || (ruleCursor == null)) {
    		Log.e(TAG, " Xml Serializer or Rule Cursor is null");
    	} else {
	        //Start tag for <RULEINFO>
	        serializer.startTag(null, XmlConstants.RULEINFO);
	        serializer.text("\n");
	
	        // Export the DB version
	        serializer.startTag(null, XmlConstants.DB_VERSION);
	        serializer.text(String.valueOf(SQLiteManager.DATABASE_VERSION));
	        serializer.endTag(null, XmlConstants.DB_VERSION);
	        serializer.text("\n");

	        Iterator<Entry<String, String>> iter = getRuleInfoMap().entrySet().iterator();
	
	        while (iter.hasNext()) {
	            Entry<String, String> entry = iter.next();
	            String dbData = null;

	            /*
	             * special case for FreeFlow suggestions tag. Do not even create the
	             * start/end tags if dbData is Empty
	             */
	            if(entry.getKey().equals(XmlConstants.SUGGESTION_FREEFLOW)){
	                dbData = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(entry.getValue()));
	                if(!TextUtils.isEmpty(dbData) && !dbData.equals("null")){
	                    serializer.startTag(null, entry.getKey());
	                    serializer.cdsect(dbData);
	                    serializer.endTag(null, entry.getKey());
	                    serializer.text("\n");
	                }
                    continue;
	            }else if(entry.getKey().equals(XmlConstants.FLAGS)){
	            	dbData = ruleCursor.getString(ruleCursor.getColumnIndexOrThrow(entry.getValue()));
	            	if(Util.isNull(dbData)){
		            	continue;
	            	}
	            }
	            serializer.startTag(null, entry.getKey());

	            //For ACTIVE node, by default the value should be INACTIVE
	            if(entry.getKey().equals(XmlConstants.ACTIVE)) {
	                serializer.text(entry.getValue());
	                // For nodes which has XML internally it will be considered as
	                // CDATA SECTION, Since parsing of those will be done by
	                // modules like VSM, INFERENCE MANAGER etc
	            } else {
	                dbData = ruleCursor.getString(ruleCursor.
	                				getColumnIndexOrThrow(entry.getValue()));
	                if (entry.getKey().equals(XmlConstants.COMMUNITY_AUTHOR) ||
	                       entry.getKey().equals(XmlConstants.INFERENCE_LOGIC)) {
	                    serializer.cdsect(TextUtils.isEmpty(dbData) ? "null" : dbData);
	                } else if (entry.getKey().equals(XmlConstants.ENABLED)){
	                	int ruleType = ruleCursor.getInt(ruleCursor.
	         					getColumnIndexOrThrow(RuleTable.Columns.RULE_TYPE));
            		   	if (ruleType == RuleTable.RuleType.MANUAL){
            			   serializer.text(Integer.toString(RuleTable.Enabled.DISABLED));
            		   	}else{
            		   		serializer.text(TextUtils.isEmpty(dbData) ? "null" : dbData);
            		   	}			   
	                } else if (entry.getKey().equals(XmlConstants.TYPE)){
	                	serializer.text(TextUtils.isEmpty(dbData) ? "null" : getRuleSourceString(
            					Integer.parseInt(dbData)));
	                } else {
	                    serializer.text(TextUtils.isEmpty(dbData) ? "null" : dbData);
	                }
	            }

	            serializer.endTag(null, entry.getKey());
	            serializer.text("\n");
	        }
	
	        //End tag for <RULEINFO>
	        serializer.endTag(null, XmlConstants.RULEINFO);
	        serializer.text("\n\n");
    	}
    }
    
    /**This methods helps to maps number with   
     * the corresponding text value for different 
     * types of rules
     * 
     * @param source - Rule type like sample -4 
     *                 Suggestion - 3 etc
     * @return String - the rule source in text
     */
    public static String getRuleSourceString(int source){
        switch (source){
        case Source.SUGGESTED :
        	return  "suggestion";
        case Source.FACTORY :
        	return "sample";
        case Source.INFERRED :
        	return "inference";
        case Source.USER :
        	return "user";
        case Source.CHILD :
        	return "child";
        default:
        	return "notsupported";
        }
    }   
}