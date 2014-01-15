/*
 * @(#)DbSyntax.java
 *
 * (c) COPYRIGHT 2009-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * ACD100        2009/07/27 NA				  Initial version
 *
 */
package com.motorola.contextual.virtualsensor.locationsensor.dbhelper;

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
    public static final String CREATE_TABLE 	= " CREATE TABLE ";
    public static final String CREATE_VIEW 		= " CREATE VIEW IF NOT EXISTS ";
    public static final String ALTER_TABLE 		= " ALTER TABLE ";
    public static final String ADD_COLUMN 		= " ADD COLUMN ";
    public static final String PRIMARY_KEY 		= " PRIMARY KEY";
    public static final String FOREIGN_KEY 		= " FOREIGN KEY";
    public static final String REFERENCES 		= " REFERENCES ";
    public static final String AUTO_INCREMENT  	= " AUTOINCREMENT";
    public static final String DEFAULT  		= " DEFAULT ";
    public static final String DEFAULT_0  		= " DEFAULT 0 ";
    public static final String UNIQUE  			= " UNIQUE";
    public static final String UNIQUE_REPLACE	= UNIQUE+" ON CONFLICT REPLACE";
    public static final String CREATE_INDEX		= " CREATE INDEX ixName ON table (field)";

    public static final String UPDATE			= "UPDATE ";
    public static final String SET				= " SET ";
    public static final String INSERT			= "INSERT ";
    public static final String SELECT			= "SELECT ";
    public static final String DELETE           = "DELETE ";
    public static final String EXISTS           = " EXISTS ";
    public static final String ALL				= "* ";
    public static final String SELECT_ALL		= SELECT+ALL;
    public static final String CS				= ", ";
    public static final String FROM				= " FROM ";
    public static final String SELECT_ALL_FROM	= SELECT_ALL+FROM;
    public static final String AS				= " AS ";
    public static final String ON				= " ON ";
    public static final String WHERE			= " WHERE ";
    public static final String INNER_JOIN		= " INNER JOIN ";
    public static final String AND				= " AND ";
    public static final String OR				= " OR ";
    public static final String NOT_EQUAL		= " != ";
    public static final String ORDER_BY			= " ORDER BY ";
    public static final String DESC				= " DESC";
    public static final String LIMIT			= " LIMIT ";
    public static final String MAX				= " max";	  // sample - OR+LP+COL_ROW_ID+IN+LP+SELECT+MAX+LP+COL_ROW_ID+RP+FROM+TABLE_NAME+RP+RP
    public static final String MIN				= " min";
    public static final String IS_NULL  		= " IS NULL";
    public static final String NOT_NULL  		= " NOT NULL";
    public static final String IS_NOT_NULL		= NOT_NULL;
    public static final String IN  				= " IN ";
    public static final String NOT_IN  			= " NOT IN";
    public static final String LIKE				= " LIKE ";
    public static final String LIKE_WILD		= "%";
    public static final String Q				= "'";
    public static final String EQUALS			= " = ";
    public static final String LP				= "(";
    public static final String RP				= ")";
    public static final String LESS_THAN		= "<";
    public static final String GREATER_THAN		= ">";
    public static final String LT 				= LESS_THAN;
    public static final String GT				= GREATER_THAN;
    public static final String GT_OR_EQUAL      = " >= ";
    public static final String ZERO				= "0";
    public static final String EQUAL			= EQUALS;

    /** see @link http://www2.sqlite.org/lang_corefunc.html */
    public static final String LOWER			= "lower";
    public static final String UPPER			= "upper";

    // types
    public static final String TEXT_TYPE 			= " TEXT";
    public static final String BLOB_TYPE 			= " BLOB";
    public static final String LONG_TYPE 			= " LONG";
    public static final String INTEGER_TYPE 		= " INTEGER";
    public static final String REAL_TYPE 			= " REAL";
    public static final String DATE_TYPE 			= LONG_TYPE;
    public static final String CONT 				= ", ";
    public static final String KEY_TYPE 			= INTEGER_TYPE;
    public static final String KEY_TYPE_NOT_NULL	= INTEGER_TYPE+NOT_NULL;
    public static final String PKEY_TYPE 			= KEY_TYPE + PRIMARY_KEY + AUTO_INCREMENT;
    public static final String DATE_TIME_TYPE 		= LONG_TYPE;
    public static final String LAT_LONG_TYPE		= REAL_TYPE;
    public static final String STATE_TYPE 			= INTEGER_TYPE;

    //public static final String COL_ROW_ID 		= "_id";
    public static final String FK 					= "Fk";

    // miscellaneous name translation fields
    public static final String INDEX 			= "Index";   // for index names
    public static final String RELATE 			= "Relate"; // for relational table names
    public static final String TO 				= "To";  	// for relational table names

    public static final long MIN_VALID_KEY      = 1; //Row ID must not be less than 1
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
