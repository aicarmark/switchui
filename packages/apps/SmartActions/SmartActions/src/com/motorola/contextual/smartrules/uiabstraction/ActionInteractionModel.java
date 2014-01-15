/*
 * @(#)ActionInteractionModel.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2011/05/09 NA				  Initial version
 * A18385        2011/06/20 NA                Added Action business logic to this file
 * E51185        2012/05/20 NA                Re-factored
 *
 */
package com.motorola.contextual.smartrules.uiabstraction;

import java.util.ArrayList;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.business.Action;
import com.motorola.contextual.smartrules.db.business.Rule;
import com.motorola.contextual.smartrules.db.table.ActionTable;
import com.motorola.contextual.smartrules.db.table.ActionTuple;
import com.motorola.contextual.smartrules.db.table.TableBase;
import com.motorola.contextual.smartrules.rulesbuilder.Blocks.EditType;
import com.motorola.contextual.smartrules.util.Util;

/** This implements the business rules around a single SmartRule Action. 
*
*<code><pre>
* CLASS:
* 	Extends ActionTuple which implements the basic methods of storing and retrieving a action 
*              (without the business logic) 
*
* RESPONSIBILITIES:
* 	implement business-layer of the Action.
*
* COLABORATORS:
* 	ActionTuple - providing utilities to persist the Action entity to DB
*
* USAGE:
* 	See each method.
*
*</pre></code>
*/
public class ActionInteractionModel implements Constants{
	Action mAction;
	String mBlockId;
	String mBlockInstanceId;
	String mState;
	String pubKey;
	String mChildRuleXml;
	Rule mChildRule = null;
	
	/**Basic constructor constructs Action class
	 * 
	 */
	public ActionInteractionModel() {
		super();
	}
	
	/**Constructor - Constructs action class from a tuple
	 * 
	 * @param tuple
	 */
	public ActionInteractionModel(Action action) {
		setAction(action);
		mBlockId = EMPTY_STRING;
		mBlockInstanceId = EMPTY_STRING;
		mState = EditType.UNKNOWN;
		pubKey = action.getPublisherKey();
		mChildRuleXml = null;
		mChildRule = action.getChildRule();
	}
	
	/** Constructor - constructs action class from parameters*/
	public ActionInteractionModel(long id,
			      long parentFk, 
			      int enabled, 
			      int active,
			      int confWinner, 
			      int onExitModeFlag, 
			      int suggState,
			      String suggReason, 
			      String desc, 
			      String publisherKey, 
			      int modality,
			      String stateMachineName, 
			      String targetState, 
			      String uri,
			      String activityIntent, 
			      String actionSyntax, 
			      long lastFiredDateTime,
			      String actFailMsg, 
			      String icon,
			      String blockId,
			      String blockInstanceId,
			      String state,
			      String xmlString,
			      String config,
			      String validity,
			      String marketUrl,
			      String childRuleKey) {
		ActionTuple actTuple = new ActionTuple( id, 
												parentFk,
												enabled, 
												active, 
												confWinner, 
												onExitModeFlag, 
												suggState,
												suggReason, 
												desc, 
												publisherKey,
												modality, 
												stateMachineName,
												targetState,
												uri, 
												activityIntent, 
												actionSyntax, 
												lastFiredDateTime,
												actFailMsg,
												icon,
												config,
												validity,
												marketUrl,
												childRuleKey);
		 setAction(new Action(actTuple));
		
		 setBlockId(blockId);
		 setBlockInstanceId(blockInstanceId);
		 setState(state);
		 setPubKey(publisherKey);
		 setChildRuleXml(xmlString);
	}
	
	/** getter - block ID
	 * 
	 * @return block id
	 */
	public String getBlockId() {
        return mBlockId;
    }
	 
	/**setter - block Id
	 * 
	 * @param blockId to be set
	 */
	public void setBlockId(String blockId) {
        this.mBlockId = blockId;
    }
	    
	/**Gets the block Instance id - this is needed for multiple instance of the same block
	 *  - like for the stateless actions
	 *  
	 * @return block instance id
	 */
    public String getBlockInstanceId() {
        return mBlockInstanceId;
    }
	
    /**setter - block instance id
     * 
     * @param blockInstanceId
     */
    public void setBlockInstanceId(String blockInstanceId) {
        this.mBlockInstanceId = blockInstanceId;
    }
    
    /**getter - state. Can be Added/Deleted/Edited
     * 
     * @return state
     */
    public String getState() {
        return mState;
    }
	    
    /**setter - state. 
     * 
     * @param state to be set.
     */
    public void setState(String state) {
        this.mState = state;
    }
    
    /**
     * Returns the ActionTuple instance
     * @return
     */
    public Action getAction() {
		return mAction;
	}

    /**
     * Sets the ActionTuple instance
     * @param actionTuple
     */
	public void setAction(Action action) {
		mAction = action;
	}

	/**setter - Child Rule Xml String
     * 
     * @param Child Rule Xml String to be set
     */
    public void setChildRuleXml(String xmlString) {
        this.mChildRuleXml = xmlString;
    }
    
    /** getter - Child Rule Xml String
     * 
     * @return Child Rule Xml String
     */
    public String getChildRuleXml() {
        return mChildRuleXml;
    }
    
	/**
	 * Returns the Publisher key
	 * @return
	 */
	public String getPubKey() {
		return pubKey;
	}

	/**
	 * Sets the Publisher key
	 * @param pubKey
	 */
	public void setPubKey(String pubKey) {
		this.pubKey = pubKey;
	}

	public Rule getChildRule() {
		return mChildRule;
	}

	public void setChildRule(Rule mChildRule) {
		this.mChildRule = mChildRule;
	}

	/**
	 * This class gives a ArrayList collection of all the Actions represented
	 * in a Model format.
	 *
	 */
	public static class ActionInteractionModelList extends ArrayList<ActionInteractionModel>{
    	
		private static final long serialVersionUID = 1L;
		
	  /**If there is at least one connected action present, then return true.
	    *
	    * @param actions
	    * @return true if there is at least one connected action present.
	    */
	   public boolean isConnectedActionPresent(){
	       boolean isPresent = false;
	       if (this.size() != 0){
	           for ( int i = 0; i < this.size(); i++ ) {
	               if ((this.get(i).getAction().getEnabled() == ActionTable.Enabled.ENABLED) 
	            	    && (!this.get(i).getState().equals(EditType.DELETED))
	            		&& (!this.get(i).getAction().getValidity().equals(TableBase.Validity.BLACKLISTED))
	            		&& (!this.get(i).getAction().getValidity().equals(TableBase.Validity.UNAVAILABLE) 
	            			|| !Util.isNull(this.get(i).getAction().getMarketUrl()))){
	                   isPresent = true;
	                   break;
	               }
	           }
	       }
	       return isPresent;
	   }
	   
		  /**If there is at least one visible action present, then return true.
	    *
	    * @param actions
	    * @return true if there is at least one visible action present.
	    */
	   public boolean isVisibleActionPresent(){
	       boolean isPresent = false;
	       if (this.size() != 0){
	           for ( int i = 0; i < this.size(); i++ ) {
	               if ((!this.get(i).getState().equals(EditType.DELETED))
	            		&& (!this.get(i).getAction().getValidity().equals(TableBase.Validity.BLACKLISTED))
	            		&& (!this.get(i).getAction().getValidity().equals(TableBase.Validity.UNAVAILABLE) 
	            			|| !Util.isNull(this.get(i).getAction().getMarketUrl()))){
	                   isPresent = true;
	                   break;
	               }
	           }
	       }
	       return isPresent;
	   }
	   
		/** Checks if there is any unconfigured action present. The action needs to be a connected trigger
		 * for it to be unconfigured. If it is not connected, it means it is an optional action and optional
		 * action can be unconfigured.
		 * 
		 * @return True if unconfigured action present
		 */
	     public boolean isUnconfiguredActionPresent() {
			boolean isPresent = false;
			if (this.size() != 0){
				for ( int i = 0; i < this.size(); i++ ) {
					if (!this.get(i).getState().equals(EditType.DELETED) && 
						(this.get(i).getAction().getEnabled() == ActionTable.Enabled.ENABLED) &&
						Util.isNull(this.get(i).getAction().getConfig())
						&& (!this.get(i).getAction().getValidity().equals(TableBase.Validity.BLACKLISTED))
				        && ((!this.get(i).getAction().getValidity().equals(TableBase.Validity.UNAVAILABLE)) 
				            || (!Util.isNull(this.get(i).getAction().getMarketUrl())))){
						isPresent = true;
						break;
					}
				}
			}
			return isPresent;
	     }
    }
}
