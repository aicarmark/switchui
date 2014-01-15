/*
 * @(#)DistinctConditionView.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/05/03 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table.view;

import android.net.Uri;

import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.db.table.ConditionTable;
import com.motorola.contextual.smartrules.db.table.RuleTable;

/** This class is a view to get the config, publisher key and Condition met columns
 * from RuleConditionView for enabled conditions from enabled rules.
 *
 *<code><pre>
 * CLASS:
 * 	 extends RuleView
 *
 *
 * RESPONSIBILITIES:
 * Create a view in the DB that indexes on the config,  publisher key and condition met columns for
 * enabled conditions from enabled rules.
 *
 * COLABORATORS:
 * 	None.
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class DistinctConditionView extends RuleView {


    /** name of the view in the database */
    public static final String VIEW_NAME = DistinctConditionView.class.getSimpleName();
    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+VIEW_NAME+"/");


    public interface Columns extends ConditionTable.Columns {

        public static final int MAX_COLUMNS =  	ConditionTable.getColumnNames().length;

    }

    /** SQL that constructs the view in the database */
    public static final String CREATE_VIEW =
        DbSyntax.CREATE_VIEW+VIEW_NAME +AS+
        SELECT+DISTINCT+ConditionTable.Columns.CONDITION_PUBLISHER_KEY+
		COMMA+ConditionTable.Columns.CONDITION_CONFIG+COMMA+
			ConditionTable.Columns.CONDITION_MET+FROM+
				RuleConditionView.VIEW_NAME+WHERE+
					RuleTable.Columns.ENABLED+EQUALS+RuleTable.Enabled.ENABLED+AND+
				ConditionTable.Columns.ENABLED+EQUALS+ConditionTable.Enabled.ENABLED+AND+
                RuleTable.Columns.VALIDITY+EQUALS+Q+RuleTable.Validity.VALID+Q+AND+
            ConditionTable.Columns.CONDITION_VALIDITY+EQUALS+Q+ConditionTable.Validity.VALID+Q+AND+
        RuleTable.Columns.FLAGS+NOT_EQUAL+Q+RuleTable.Flags.SOURCE_LIST_VISIBLE+Q;

    /** basic constructor
     */
    public DistinctConditionView() {
        super();
    }


    /* (non-Javadoc)
     * @see com.motorola.contextual.smartrules.db.table.view.RuleView#getViewName()
     */
    @Override
    public String getViewName() {
        return VIEW_NAME;
    }



}
