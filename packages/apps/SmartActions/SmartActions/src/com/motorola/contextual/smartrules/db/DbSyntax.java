/*
 * @(#)DbSyntax.java
 *
 * (c) COPYRIGHT 2009-2010 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db;

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
 *				COL_ROW_ID						+ PKEY_TYPE		+ CONT +
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
    String CREATE_TABLE 	= " CREATE TABLE ";
    String CREATE_VIEW 		= " CREATE VIEW IF NOT EXISTS ";
    String ALTER_TABLE 		= " ALTER TABLE ";
    String ADD_COLUMN 		= " ADD COLUMN ";
    String DROP_COLUMN_NA 	= ""; // DROP COLUMN is not available in sqlite3";
    String PRIMARY_KEY 		= " PRIMARY KEY";
    String FOREIGN_KEY 		= " FOREIGN KEY";
    String REFERENCES 		= " REFERENCES ";
    String AUTO_INCREMENT  	= " AUTOINCREMENT";
    String DEFAULT  		= " DEFAULT ";
    String DEFAULT_0  		= DEFAULT+"0 ";
    String UNIQUE  			= " UNIQUE";
    String UNIQUE_REPLACE	= UNIQUE+" ON CONFLICT REPLACE";
    String CREATE_INDEX		= " CREATE INDEX ixName ON table (field)";
    String ALTER_COLUMN         = " ALTER COLUMN ";
    String DROP_IF_TABLE_EXISTS  = "DROP TABLE IF EXISTS ";
    String DROP_IF_VIEW_EXISTS	 = "DROP VIEW IF EXISTS ";
    String DROP_IF_INDEX_EXISTS = "DROP INDEX IF EXISTS ";
    
    String UPDATE			= "UPDATE ";
    String SET				= " SET ";
    String INSERT			= "INSERT ";
    String SELECT			= "SELECT ";
    String DELETE           = "DELETE ";
    String UNION			= " UNION ";
    String EXISTS           = " EXISTS ";
    String ALL				= "* ";
    String SELECT_ALL		= SELECT+ALL;
    String CS				= ", ";
    String FROM				= " FROM ";
    String SELECT_ALL_FROM	= SELECT_ALL+FROM;
    String AS				= " AS ";
    String ON				= " ON ";
    String WHERE			= " WHERE ";
    String JOIN				= " JOIN ";
    String INNER_JOIN		= " INNER JOIN ";
    String LEFT_OUTER_JOIN		= " LEFT OUTER JOIN "; // Note: Right outer joins not supported in SQLITE3
    String AND				= " AND ";
    String OR				= " OR ";
    String NOT_EQUAL		= " != ";
    String LP				= "(";
    String RP				= ")";
    String ORDER			= " ORDER ";
    String ORDER_BY			= ORDER+"BY ";
    String GROUP			= " GROUP ";
    String GROUP_BY			= GROUP+"BY ";
    String DESC				= " DESC";
    String LIMIT			= " LIMIT ";
    String COUNT			= " COUNT";
    String COUNT_ALL      	= COUNT+LP+ALL+RP;
    String GROUP_CONCAT		= " GROUP_CONCAT"+LP; //group_concat(
    String MAX				= " max";	  // OR+LP+COL_ROW_ID+IN+LP+SELECT+MAX+LP+COL_ROW_ID+RP+FROM+TABLE_NAME+RP+RP
    String MIN				= " min";
    String IS_NULL  		= " IS NULL";
    String NOT_NULL  		= " NOT NULL";
    String IS_NOT_NULL		= NOT_NULL;
    String IN  				= " IN ";
    String NOT_IN  			= " NOT IN";
    String LIKE				= " LIKE ";
    String NOT_LIKE 		= " NOT LIKE ";
    String IS_NOT_LIKE		= NOT_LIKE;
    String LIKE_WILD		= "%";
    String WILD				= "%";
    String Q				= "'";
    String EQUALS			= " = ";
    String LESS_THAN		= "<";
    String GREATER_THAN		= ">";
    String LT 				= LESS_THAN;
    String GT				= GREATER_THAN;
    String GT_OR_EQUAL      = " >= ";
    String ZERO				= "0";
    String EQUAL			= EQUALS;
    String PARM				= " ? ";
    String COMMA            = ",";
    String SEMI_COLON       = ";";
    char   ANY              = '?';
    String NOT_NULL_CONSTRAINT      = " NOT NULL ";
    String DISTINCT      = " DISTINCT ";
    /** see @link http://www2.sqlite.org/lang_corefunc.html */
    String LOWER			= "lower";
    String UPPER			= "upper";
    
    // types
    String TEXT_TYPE 			= " TEXT";
    String BLOB_TYPE 			= " BLOB";
    String LONG_TYPE 			= " LONG";
    String INTEGER_TYPE 		= " INTEGER";
    String REAL_TYPE 			= " REAL";
    String DATE_TYPE 			= LONG_TYPE;
    String CONT 				= ", ";
    String KEY_TYPE 			= INTEGER_TYPE; // Don't make LONG type, SQLite3 says not valid for _id column
    String KEY_TYPE_NOT_NULL	= LONG_TYPE+NOT_NULL;
    String PKEY_TYPE 			= KEY_TYPE + PRIMARY_KEY + AUTO_INCREMENT;
    String DATE_TIME_TYPE 		= LONG_TYPE;
    String LAT_LONG_TYPE		= REAL_TYPE;
    String STATE_TYPE 			= INTEGER_TYPE;

    // macros
    String CASE					= "CASE ";
    String THEN					= " THEN ";
    String WHEN					= " WHEN ";
    String ELSE					= " ELSE ";
    String END					= " END ";

    //default values
    String CURRENT_DATETIME     = " CURRENT_DATETIME ";
    
    // String COL_ROW_ID 		= "_id";
    String FK 				= "Fk";

    // miscellaneous name translation fields
    String INDEX 			= "Index";   // for index names
    String RELATE 			= "Relate"; // for relational table names
    String TO 				= "To";  	// for relational table names

    long MIN_VALID_KEY      = 1; //Row ID must not be less than 1
    
    /*
     * Command line example:
     * 		insert into MapFriend(Title, Phone) values("Fred", "8472221111");
     *      insert into Gps (GpsLat, GpsLng, Interleave, DateTime) values(42.412197, -88.037532, "-8482.403", 01234456);
     *      insert into RelateMapFriendToGps (FkMapFriend_id, FkWaypoint_id) values (1, 1);
     *
     *
     *      insert into RelateMapFriendToPlace (Relationship, FkMapFriend_id, FkPlace_id) values ("at", 1, 1);
     *
     *
     *      Inner join:
     *
     *      SELECT *  FROM RelateMapFriendToGPS  inner join GPS on RelateMapFriendToGPS.FkGPS_id =  GPS._id   ORDER BY  GPS.DateTime desc;
     *      SELECT *  FROM RelateMapFriendToGPS r inner join GPS g on r.FkGPS_id =  g._id   ORDER BY  g.DateTime desc;
     *
    */
}