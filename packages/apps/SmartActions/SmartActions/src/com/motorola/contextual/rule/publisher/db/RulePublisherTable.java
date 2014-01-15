/*
 * @(#)RulePublisherTable.java
 *
 * (c) COPYRIGHT 2010-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * A21693        2012/02/16 NA                Initial version
 *
 */
package com.motorola.contextual.rule.publisher.db;

import android.net.Uri;

import com.motorola.contextual.rule.Constants;
import com.motorola.contextual.smartrules.db.DbSyntax;
import com.motorola.contextual.smartrules.util.Util;


/** This class allows access and updates to the Rule Publisher table.
 * Basically, it abstracts the RulePublisher tuple instance.
 *
 * The RulePublisher table is used here to hold a rule xml and inference info.
 *
 *<code><pre>
 * CLASS:
 * 	Extends TableBase which provides basic table inserts, deletes, etc.
 *
 * RESPONSIBILITIES:
 * 	Insert, delete, update, fetch  Rule Table records
 *  Converts cursor of RulePublisherTable records to a RuleTuple.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class RulePublisherTable extends TableBase implements Constants, DbSyntax {

    /** This is the name of the table in the database */
    public static final String TABLE_NAME           = "rule";
    public static final String AUTHORITY            = "com.motorola.contextual.publisher.rule.publisher";

    /** Currently not used, but could be for joining this table with other tables. */
    public static final String SQL_REF 	 			= " r";

    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+TABLE_NAME+"/");

    public interface Columns {

        public static final String _ID              = "_id";

        /** key for the rule */
        public static final String RULE_KEY         = "key";

        /** key for the rule */
        public static final String RULE_NAME        = "name";

        /** rule type */
        public static final String RULE_TYPE        = "type";

        /** Entire rule xml comes here */
        public static final String RULE_XML         = "xml";

        /** Suggestion text comes here */
        public static final String SUGGESTION_TEXT  = "SuggText";

        /** set to 1, when a rule has been published */
        public static final String PUBLISH_STATE    = "published";

        /** Time at which the rule was published */
        public static final String PUBLISH_TIME     = "publish_time";

        /** set to 1, when a rule has been inferred */
        public static final String INFERRED_STATE   = "inferred";

        /** Time at which the rule was published */
        public static final String INFERRED_TIME    = "inferred_time";
    }

    private static final String[] COLUMN_NAMES = {
        Columns._ID, Columns.RULE_KEY, Columns.RULE_NAME, Columns.RULE_TYPE, Columns.RULE_XML,
        Columns.SUGGESTION_TEXT, Columns.PUBLISH_STATE, Columns.PUBLISH_TIME,
        Columns.INFERRED_STATE, Columns.INFERRED_TIME
       };

    public static String[] getColumnNames() {
	return Util.copyOf(COLUMN_NAMES);
    }

    /** SQL statement to create the Table */
    public static final String CREATE_TABLE_SQL =
        CREATE_TABLE +
        TABLE_NAME + " (" +
        Columns._ID                     + PKEY_TYPE                             + CONT +
        Columns.RULE_KEY                + TEXT_TYPE + UNIQUE                    + CONT +
        Columns.RULE_NAME               + TEXT_TYPE                             + CONT +
        Columns.RULE_TYPE               + TEXT_TYPE                             + CONT +
        Columns.RULE_XML                + TEXT_TYPE                             + CONT +
        Columns.SUGGESTION_TEXT         + TEXT_TYPE                             + CONT +
        Columns.PUBLISH_STATE           + INTEGER_TYPE                          + CONT +
        Columns.PUBLISH_TIME            + DATE_TIME_TYPE                        + CONT +
        Columns.INFERRED_STATE          + INTEGER_TYPE                          + CONT +
        Columns.INFERRED_TIME           + DATE_TIME_TYPE                        +
        ")";


    /** Basic constructor
     */
    public RulePublisherTable(String tag) {
        super(tag);
    }


    /** Get the table name for this table.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getTableName()
     */
    @Override
    public String getTableName() {
        return TABLE_NAME;
    }


    /** required by TableBase */
    public Uri getTableUri() {
        return CONTENT_URI;
    }


    @Override
    public Uri getContentUri() {

        return CONTENT_URI;
    }
}