/*
 * @(#)MediatorCommand.java
 *
 * (c) COPYRIGHT 2011-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * qwfn37       2012/05/21  NA                Initial version
 *
 */

package com.motorola.contextual.smartrules.psf.mediator.protocol;

import java.util.List;

import android.content.Context;
import android.content.Intent;

/**
 * Interface to be implemented by each of the commands of the publisher provider framework protocol. 
 *
 * <CODE><PRE>
 *
 * INTERFACE:
 *
 * RESPONSIBILITIES:
 * 	   Define the required methods of a publisher provider protocol command.
 *
 * COLABORATORS:
 *
 * USAGE:
 *     Implement this interface.
 *
 * </PRE></CODE>
 */

public interface IMediatorProtocol {

    /**
     * Method to check if input intent has all the required extras
     * @param context Context to work with
     * @param intent Input intent to the mediator
     *
     * @return true if intent contains all the required extras, false otherwise
     */
    public boolean processRequest (Context context, Intent intent);

    /**
     * Method to be executed whenever mediator receives an intent.
     * Whenever mediator receives an intent it is expected to process it and
     * broadcast a modified intent. This method is used to implement this feature.
     *
     * @param context Caller context
     * @param intent Incoming intent
     * @return List of intents to be broadcasted by the mediator
     */
    public List<Intent> execute (Context context, Intent intent);

}
