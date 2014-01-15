/*
 * @(#)DbConstants.java
 *
 * (c) COPYRIGHT 2012 MOTOROLA MOBILITY INC.
 * MOTOROLA MOBILITY CONFIDENTIAL PROPRIETARY
 * MOTOROLA MOBILITY Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * CSD053        2012/18/05 NA				  Initial version
 */
package com.motorola.contextual.smartrules.db;

/** This is the Database related constants class
*
*<code><pre>
* CLASS:
*   This class simply represents constants required for DB, that are not visible to
*   the user or otherwise dependent upon being changed if the language is translated
*   from English to some other language.
*
*   This should not be used for constants within a class.
*   Keep all constants as static
*
* RESPONSIBILITIES:
*   None - static constants only
*
* COLABORATORS:
* 	 None.
*
* USAGE:
* 	All constants here should be static, almost or all should be final.
*
* </pre></code>
**/
public interface DbConstants {

    public static final String SQL_TABLE_TYPE = "table";
    public static final String SQL_INDEX_TYPE = "index";
    public static final String SQL_VIEW_TYPE = "view";
    public static final String ENTRY_TYPE = "type";
    public static final String ENTRY_NAME = "name";
	public static final String SQLITE_MASTER_TABLE = "sqlite_master";
    public static final String ANDROID_METADATA_TABLENAME = "android_metadata";
    public static final String SQLITE_SEQUENCE_TABLENAME = "sqlite_sequence";
}
