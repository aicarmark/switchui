package com.motorola.contextual.smartrules.uipublisher;

import com.motorola.contextual.smartrules.uiabstraction.IEditRulePluginCallback;
import com.motorola.contextual.smartrules.uiabstraction.RuleController;
import com.motorola.contextual.smartrules.uiabstraction.RuleInteractionModel;

import android.content.Context;
import android.view.View;

/**
 * <code><pre>
 * CLASS:
 *  None.
 *
 * RESPONSIBILITIES:
 * Controller class which can be further generalized for individual
 * Conditions and Actions helping  RuleController to delegate the
 *
 *
 * COLABORATORS:
 * 	Context
 *  RuleInteractionModel
 *  RuleController
 *  IEditRulePluginCallback
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */


public class PublisherController {

	protected IEditRulePluginCallback				mEditRuleCallback;
	protected RuleController 						mRuleController;
	protected Context								mContext;
	protected RuleInteractionModel					mRuleModel;

	public PublisherController(Context context, RuleController rc,
			IEditRulePluginCallback cb){
		mEditRuleCallback = cb;
		mRuleController = rc;
		mContext = context;
		mRuleModel = mRuleController.getRuleModel();
	}

	public void removeGenericPublisher(View pub){
		mEditRuleCallback.removeGenericPublisher(pub);
	}

}
