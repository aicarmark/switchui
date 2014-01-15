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
package com.motorola.contextual.task;

import android.os.AsyncTask;
import android.provider.Settings;

import com.motorola.contextual.model.SystemPropTaskModel;

/**
 * <code><pre>
 * CLASS:
 *  Read the system property in background.
 * 
 * RESPONSIBILITIES:
 * 
 * USAGE:
 *  Execute the task with the SystemPropTaskModel as argument.
 * 
 * </pre></code>
 */
// TODO This class will be generalized further in future refactoring/rewrite.
public class ReadSystemPropTask extends
        AsyncTask<SystemPropTaskModel, Void, Object> {

    private SystemPropTaskModel model;

    @Override
    protected Object doInBackground(SystemPropTaskModel... params) {
        this.model = params[0];
        Object state = null;
        if(Integer.class.isAssignableFrom(model.clazz))
            state = Settings.System.getInt(model.context.getContentResolver(),
                model.prop, 0);
        else if(Float.class.isAssignableFrom(model.clazz))
            state = Settings.System.getFloat(model.context.getContentResolver(),
                    model.prop, 0);
        else if(Long.class.isAssignableFrom(model.clazz))
            state = Settings.System.getLong(model.context.getContentResolver(),
                    model.prop, 0);
        else if(String.class.isAssignableFrom(model.clazz))
            state = Settings.System.getString(model.context.getContentResolver(),
                    model.prop);
        return state;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
     */
    @Override
    protected void onPostExecute(Object state) {
        model.command.execute(state);
    }
}