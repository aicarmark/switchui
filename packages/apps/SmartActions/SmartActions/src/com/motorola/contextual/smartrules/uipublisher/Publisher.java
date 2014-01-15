/*
 * @(#)Publisher.java
 *
 * (c) COPYRIGHT 2009 - 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * E51185        2012/04/03  NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.uipublisher;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.rulesbuilder.BlockGestureListener;

/**
 * Parent class for Action and Condition publishers that holds
 * all data attributes necessary for moulding the Publisher data
 * to the Model that can represent a Publisher in the UI element
 *
 * <code><pre>
 * CLASS:
 *  None.
 *
 * RESPONSIBILITIES:
 * This will be parent class has all the information that Publishers have
 * which is useful for UI interactions. Like their Title, Description
 * Icon amongst others.
 * Once Publisher gets pulled into the Rule,  attributes of these classes
 * get copied into the corresponding Model classes that help in the MVC
 * design and eventually get persisted into the DB
 *
 * This would implement some callback listening for state change
 * Any changes to any attribute here will have to be implemented
 * as listeners on that attribute
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */

public class Publisher implements Constants, Cloneable{

	private static final String TAG = Publisher.class.getSimpleName();

	/** Not Yet Implemented
	 * Need to implement per Publisher for knowing Install/Uninstall events
	 * and update related attributes
	 *
	 */
	public interface PublisherPresenceListener{
		public void onInstall();
		public void onUninstall();
	}

	/**
	 * Not Yet Implemented
	 *
	 */
	public interface StateChangeListener{
		public void onAddState(State newState);
		public void onStateChange(State changedState);
	}

	/**
	 * Not Yet Implemented
	 * This class will hold all the States related to a specific Publisher
	 * and their related methods
	 */
	public class State{

	}


	public Publisher() {

	}

	int blockId;
    boolean isAction = true;
    boolean isDisabled = false;
    Drawable image_drawable;
    String blockName;
    String blockDescription;
    String blockUsageSuggestion;
    String intentUriString;
    String publisherKey;
    String stateType;
    Boolean whenRuleEnds = false;
    Boolean suggested = false;
    Boolean conflict = false;
    Boolean error = false;
    Boolean active = false;
    String errorText;
    String activityPkgUri;
    String config = null;
    String marketUrl = null;
    String validity = null;
    String blockInstanceId;					//this needs to go from here, UI specific
    Boolean blockConnectedStatus = true;	//this needs to go from here, UI specific
    boolean needToPopDialogFromThisActivity = false; //this needs to go from here, UI specific
    BlockGestureListener instBlockGestureListener;   //this needs to go from here, UI specific

    PublisherPresenceListener callbackListInst = null;
    
    public Publisher(PublisherPresenceListener callbackList){
    	callbackListInst = callbackList;
    }

    /** converts tuple to String mainly for debugging purposes.
    *
    * @see com.motorola.contextual.smartrules.db.table.TupleBase#toString()
    */
   @Override
   public String toString() {

       StringBuilder builder = new StringBuilder(" Blocks: ");

       builder.append(", blockId="+ blockId)
       .append(", isAction="+ isAction)
       .append(", isDisabled"+ isDisabled)
       .append(", blockName="+ blockName)
       .append(", blockDescription="+ blockDescription)
       .append(", blockUsageSuggestion="+ blockUsageSuggestion)
       .append(" intentUriString="+ intentUriString)
       .append(", publisherKey="+ publisherKey)
       .append(", stateType="+ stateType)
       .append(", blockInstanceId="+ blockInstanceId)
       .append(", blockConnectedStatus="+ blockConnectedStatus)
       .append(", whenRuleEnds="+ whenRuleEnds)
       .append(", suggested="+ suggested)
       .append(", conflict="+ conflict)
       .append(", active="+ active)
       .append(", error="+ error)
       .append(", config="+ config)
       .append(", marketUrl="+marketUrl)
       .append(", validity="+validity);

       return builder.toString();
   }

    //Getters and setters

    /**
     * Check to see if Publisher is an Action
     * @return
     */
	public boolean isAction() {
		return isAction;
	}

	/**
	 * Is publisher disabled in the current Rule
	 * @return
	 */
	public boolean isDisabled() {
		return isDisabled;
	}

	/**
	 * Get the Icon/Graphics for Publisher
	 * @return
	 */
	public Drawable getImage_drawable() {
		return image_drawable;
	}

         /**
         * Copy a ImageDrawable object, which can be mutate.
         * @return a copy of image_drawable field.
         */
       public Drawable getMutateImageDrawable() {
            if(image_drawable == null) return null;
            else return image_drawable.getConstantState().newDrawable();
       }

	/**
	 * Gets the Title of the description that can be displayed
	 * @return
	 */
	public String getBlockName() {
		return blockName;
	}

	/**
	 * Get the description detailing the specific Publisher
	 * Parts of this get build while configuring the publisher
	 * @return
	 */
	public String getBlockDescription() {
		return blockDescription;
	}

    /**
     * Get descriptive text for usage
     * @return
     */
    public String getBlockUsageSuggestion() {
        return blockUsageSuggestion;
    }

	/**
	 * Gets the Intent URI string that can be used in Intent.parseUri
	 * to launch the specific Publisher for configuring and getting back
	 * the configured values in onActivityResult
	 * @return
	 */
	public String getIntentUriString() {
		return intentUriString;
	}

	/**
	 * The key distinguishes one Publisher from another
	 * Its also the key for Hashmap in PublisherList
	 * @return
	 */
	public String getPublisherKey() {
		return publisherKey;
	}

	/**
	 * Gets the state value Stateful/Stateless
	 * @return
	 */
	public String getStateType() {
		return stateType;
	}


	/**
	 * Gets any error state set in the Publisher over course of
	 * the lifetime of it being in a Rule
	 * @return
	 */
	public String getErrorText() {
		return errorText;
	}

	/**
	 * The package URI can be utilized to generate Intent URI
	 * using setClassName(pkg, componentName) from ActivityInfo
	 * @return
	 */
	public String getActivityPkgUri() {
		return activityPkgUri;
	}

	/**
	 * If theres need to pop dialog from this activity
	 * @return
	 */
	public boolean isNeedToPopDialogFromThisActivity() {
		return needToPopDialogFromThisActivity;
	}

	public Boolean getBlockConnectedStatus() {
		return blockConnectedStatus;
	}

	public String getBlockInstanceId() {
		return blockInstanceId;
	}

	public int getBlockId() {
		return blockId;
	}
	
	public Boolean getError() {
		return error;
	}

	public Boolean getWhenRuleEnds() {
		return whenRuleEnds;
	}

	public Boolean getConflict() {
		return conflict;
	}

	public Boolean getSuggested() {
		return suggested;
	}

	public Boolean getActive() {
		return active;
	}

	public BlockGestureListener getInstBlockGestureListener() {
		return instBlockGestureListener;
	}

	public static String getTag() {
		return TAG;
	}

	public String getConfig() {
		return config;
	}

	public String getMarketUrl() {
		return marketUrl;
	}

	public String getValidity() {
		return validity;
	}

	public void setBlockInstanceId(String blockInstanceId) {
		this.blockInstanceId = blockInstanceId;
	}

	public void setBlockConnectedStatus(Boolean blockConnectedStatus) {
		this.blockConnectedStatus = blockConnectedStatus;
	}

	public void setBlockId(int blockId) {
		this.blockId = blockId;
	}

	public void setAction(boolean isAction) {
		this.isAction = isAction;
	}

	public void setDisabled(boolean isDisabled) {
		this.isDisabled = isDisabled;
	}

	public void setImage_drawable(Drawable image_drawable) {
		this.image_drawable = image_drawable;
	}

	public void setBlockName(String blockName) {
		this.blockName = blockName;
	}

	public void setBlockDescription(String blockDescription) {
		this.blockDescription = blockDescription;
	}

        public void setBlockUsageSuggestion(String blockUsageSuggestion) {
                this.blockUsageSuggestion = blockUsageSuggestion;
        }

	public void setIntentUriString(String intentUriString) {
		this.intentUriString = intentUriString;
	}

	public void setPublisherKey(String publisherKey) {
		this.publisherKey = publisherKey;
	}

	public void setStateType(String stateType) {
		this.stateType = stateType;
	}

	public void setWhenRuleEnds(Boolean whenRuleEnds) {
		this.whenRuleEnds = whenRuleEnds;
	}

	public void setSuggested(Boolean suggested) {
		this.suggested = suggested;
	}

	public void setConflict(Boolean conflict) {
		this.conflict = conflict;
	}

	public void setError(Boolean error) {
		this.error = error;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}


	public void setErrorText(String errorText) {
		this.errorText = errorText;
	}

	public void setActivityPkgUri(String activityPkgUri) {
		this.activityPkgUri = activityPkgUri;
	}

	public void setNeedToPopDialogFromThisActivity(
			boolean needToPopDialogFromThisActivity) {
		this.needToPopDialogFromThisActivity = needToPopDialogFromThisActivity;
	}

	public void setInstBlockGestureListener(
			BlockGestureListener instBlockGestureListener) {
		this.instBlockGestureListener = instBlockGestureListener;
	}

	public void setCallbackListInst(PublisherPresenceListener callbackListInst) {
		this.callbackListInst = callbackListInst;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public void setMarketUrl(String marketUrl) {
		this.marketUrl = marketUrl;
	}

	public void setValidity(String validity) {
		this.validity = validity;
	}

	/**
	 * Log publisher data to ID for debug purpose
	 */
	public void logPublisherData(){
		if(LOG_DEBUG) Log.d(TAG, "Publisher Package URI : "+ activityPkgUri);
		if(LOG_DEBUG) Log.d(TAG, "Publisher Publisher key : "+ publisherKey);
	}

	/**
	 * Clones the Publisher object to avoid using same Publisher object
	 * multiple time in case we have same publisher used more than once
	 * in a Rule 
	 */
	protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


	/** Makes an intent with the package and component name
	 *
	 * @param pkg - package name
	 * @param componentName - component name
	 * @return intent
	 */
	public static Intent getActivityIntentForPublisher(String pkg, String componentName) {
		Intent result = new Intent(ACTION_GET_CONFIG);
		result.setClassName(pkg, componentName);
		return result;
	}
    
}
