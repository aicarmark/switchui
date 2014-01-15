/*
 * Copyright (C) 2012 Motorola Mobility, Inc.
 * All Rights Reserved.
 * Motorola Mobility Confidential Restricted.
 *
 * Revision History:
 * Author                      Date        CR Number      Brief Description
 * ------------------------- ---------- ----------------- ------------------------------
 * w04917 (Brian Lee)        2012/06/08   IKCTXTAW-480    Initial version
 * w04917 (Brian Lee)        2012/07/09   IKCTXTAW-487    Add drop table syntax
 */

package com.motorola.contextual.smartnetwork.db;

/** This interface simply defines all the SqlLite3 syntax in one place.
 *
 *<code><pre>
 * INTERFACE:
 * 	 It allows the user to build a SQL query without worrying about spacing or formatting
 * 	 issues that can often cause crashes due to lack of spacing between certain SQL keywords
 *   or reserved words and field names, etc. It also
 *
 * RESPONSIBILITIES:
 * 	None - all static Strings here.
 *
 * COLABORATORS:
 * 	None - all static Strings here.
 *
 * USAGE:
 * 	Code sample:
 *
 *	public static final String CREATE_TABLE_SQL =
 * 		CREATE_TABLE +
 *			TABLE_NAME + " (" +
 *				_ID								+ PKEY_TYPE		+ CONT +
 *				COL_NAME						+ TEXT_TYPE		+ CONT+
 *				COL_MOBILE_PHONE_NO				+ TEXT_TYPE		+ UNIQUE + CONT +
 *				COL_NICK_NAME					+ TEXT_TYPE		+ CONT+
 *				COL_HIDE_DATETIME				+ TEXT_TYPE		+" default 0 "+ CONT+
 *				COL_MAP_UPDATE_MINUTES			+ INTEGER_TYPE	+" default 0 "+ CONT +
 *				COL_OPTIONS						+ TEXT_TYPE		+ CONT+
 *				COL_PERMISSIONS					+ TEXT_TYPE		+ CONT+
 *				COL_LAST_PING_DATETIME			+ TEXT_TYPE		+ " default 0 "+
 *			")"; *
 *</pre></code>
 *
 **/
public interface DbSyntax {

    // syntax
    public static final String CREATE_TABLE 	= " CREATE TABLE ";
    public static final String CREATE_VIEW 		= " CREATE VIEW IF NOT EXISTS ";
    public static final String ALTER_TABLE 		= " ALTER TABLE ";
    public static final String ADD_COLUMN 		= " ADD COLUMN ";
    public static final String PRIMARY_KEY 		= " PRIMARY KEY ";
    public static final String FOREIGN_KEY 		= " FOREIGN KEY ";
    public static final String REFERENCES 		= " REFERENCES ";
    public static final String ON_DELETE_CASCADE = " ON DELETE CASCADE ";
    public static final String ON_UPDATE_CASCADE = " ON UPDATE CASCADE ";
    public static final String AUTO_INCREMENT  	= " AUTOINCREMENT ";
    public static final String DEFAULT  		= " DEFAULT ";
    public static final String DEFAULT_0  		= " DEFAULT 0 ";
    public static final String UNIQUE  			= " UNIQUE ";
    public static final String UNIQUE_REPLACE	= UNIQUE + " ON CONFLICT REPLACE ";
    public static final String CREATE_INDEX		= " CREATE INDEX ";
    public static final String CHECK            = " CHECK ";
    public static final String DROP_TABLE_IF_EXISTS = "DROP TABLE IF EXISTS ";

    public static final String UPDATE			= " UPDATE ";
    public static final String SET				= " SET ";
    public static final String INSERT			= " INSERT ";
    public static final String SELECT			= " SELECT ";
    public static final String DELETE           = " DELETE ";
    public static final String EXISTS           = " EXISTS ";
    public static final String NOT_EXISTS       = " NOT EXISTS ";
    public static final String ALL				= "* ";
    public static final String SELECT_ALL		= SELECT+ALL;
    public static final String CS				= ", ";
    public static final String FROM				= " FROM ";
    public static final String SELECT_ALL_FROM	= SELECT_ALL+FROM;
    public static final String AS				= " AS ";
    public static final String ON				= " ON ";
    public static final String WHERE			= " WHERE ";
    public static final String JOIN             = " JOIN ";
    public static final String LEFT_JOIN        = " LEFT JOIN ";
    public static final String INNER_JOIN		= " INNER JOIN ";
    public static final String AND				= " AND ";
    public static final String OR				= " OR ";
    public static final String NOT_EQUAL		= " != ";
    public static final String ORDER_BY			= " ORDER BY ";
    public static final String DESC				= " DESC ";
    public static final String LIMIT			= " LIMIT ";
    public static final String MAX				= " max ";
    public static final String MIN				= " min ";
    public static final String IS_NULL  		= " IS NULL ";
    public static final String NOT_NULL  		= " NOT NULL ";
    public static final String IS_NOT_NULL		= NOT_NULL;
    public static final String IN  				= " IN ";
    public static final String NOT_IN  			= " NOT IN ";
    public static final String LIKE				= " LIKE ";
    public static final String LIKE_WILD		= "%";
    public static final String Q				= "'";
    public static final String EQUALS			= " = ";
    public static final String LP				= "(";
    public static final String RP				= ")";
    public static final String LESS_THAN		= " < ";
    public static final String GREATER_THAN		= " > ";
    public static final String LT 				= LESS_THAN;
    public static final String LT_OR_EQUAL      = " <= ";
    public static final String GT				= GREATER_THAN;
    public static final String GT_OR_EQUAL      = " >= ";
    public static final String ZERO				= " 0 ";
    public static final String EQUAL			= EQUALS;
    public static final String SUM              = " SUM ";
    public static final String MINUS            = " - ";
    public static final String GROUP_BY         = " GROUP BY ";

    /** see @link http://www2.sqlite.org/lang_corefunc.html */
    public static final String LOWER			= " lower ";
    public static final String UPPER			= " upper ";

    // types
    public static final String TEXT_TYPE 			= " TEXT ";
    public static final String BLOB_TYPE 			= " BLOB ";
    public static final String INTEGER_TYPE 		= " INTEGER ";
    public static final String REAL_TYPE 			= " REAL";
    public static final String DATE_TYPE            = " DATE ";
    public static final String DATE_TIME_TYPE       = " DATETIME ";
    public static final String CONT 				= ", ";
    public static final String KEY_TYPE 			= INTEGER_TYPE;
    public static final String KEY_TYPE_NOT_NULL	= INTEGER_TYPE+NOT_NULL;
    public static final String PKEY_TYPE 			= KEY_TYPE + PRIMARY_KEY + AUTO_INCREMENT;
    public static final String LAT_LONG_TYPE		= REAL_TYPE;
    public static final String STATE_TYPE 			= INTEGER_TYPE;

    //default values
    public static final String CURRENT_DATETIME     = " CURRENT_DATETIME ";
    //public static final String COL_ROW_ID 		= "_id";
    public static final String FK 				= "fk_";

    // miscellaneous name translation fields
    public static final String INDEX 			= " Index ";   // for index names
    public static final String RELATE 			= " Relate "; // for relational table names
    public static final String TO 				= " To ";  	// for relational table names

    public static final long MIN_VALID_KEY      = 1; //Row ID must not be less than 1

}
