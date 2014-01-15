/*
 * (c) COPYRIGHT 2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * vxmd37        10/19/2012   NA                1.0
 */
package com.motorola.contextual.model;

import android.content.Context;

import com.motorola.contextual.callback.Command;

/**
 * <code><pre>
 * CLASS:
 *  Data for ReadIntSystemPropTask.
 * 
 * RESPONSIBILITIES:
 * 
 * USAGE:
 *  
 * 
 * </pre></code>
 */
public class SystemPropTaskModel {
    
    public Context context;
    
    /* Command to be executed on task completion (UI thread) */
    public Command command;
    
    /* System Property to be read */
    public String prop;
    
    /* Data type of the property */
    public Class<?> clazz;

    public SystemPropTaskModel(Context context, Command command, String prop,
            Class<?> clazz) {
        super();
        this.context = context;
        this.command = command;
        this.prop = prop;
        this.clazz = clazz;
    }
    
}
