/*
 * @(#)RuleListActivityBase.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2010/12/09 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.app;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageButton;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.RuleTable;

/** This class is the base class for all the list view activities.
 *
 *
 *<code><pre>
 * CLASS:
 * 	extends ListActivity which provides basic list building, scrolling, etc.
 *
 *  implements
 *  	Constants - for the constants used
 *  	DbSyntax - for the DB related constant strings
 *
 * RESPONSIBILITIES:
 * 	base class for list activities
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class RuleListActivityBase extends ListActivity implements Constants, DbSyntax {

    protected Context   mContext = null;

    /** onCreate()
     */
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mContext = this;
    }

    /** sets the rule icon
     *
     * @param cursor - cursor
     * @param button - button
     * @return - true or false
     */
    protected boolean setRuleIcon(final Cursor cursor, ImageButton button) {

        boolean result = false;
        int ix = -1;
        String s = null;
        if((s = cursor.getString(ix = cursor.getColumnIndex(RuleTable.Columns.ICON))) != null) {
            if (s.length() > 15) { // prevent attempt to convert resource id
                Drawable d = RuleTable.decodeToDrawable(cursor, ix);
                button.setBackgroundDrawable(d);
                result = true;
            } else {
                button.setBackgroundDrawable(null);
                result = false;
            }
        }
        return result;
    }
}