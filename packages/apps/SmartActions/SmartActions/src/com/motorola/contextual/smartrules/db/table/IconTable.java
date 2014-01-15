/*
 * @(#)IconTable.java
 *
 * (c) COPYRIGHT 2010-2011 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * a21345        2012/04/24 NA				  Initial version
 *
 */
package com.motorola.contextual.smartrules.db.table;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.motorola.contextual.smartrules.Constants;
import com.motorola.contextual.smartrules.util.Util;



/** This class allows access and updates to the Icon table. Basically, it abstracts a
 * the IconTable tuple instance.
 *
 * The Icon table is used here to hold a Icon.
 *
 *<code><pre>
 * CLASS:
 * 	Extends TableBase which provides basic table inserts, deletes, etc.
 *
 * RESPONSIBILITIES:
 * 	Insert, delete, update, fetch  Rule Table records
 *  Converts cursor of RuleTable records to a RuleTuple.
 *
 * COLABORATORS:
 * 	None
 *
 * USAGE:
 * 	See each method.
 *</pre></code>
 */
public class IconTable extends TableBase implements Constants {

    /** This is the name of the table in the database */
    public static final String TABLE_NAME 			= "Icon";

    /** Currently not used, but could be for joining this table with other tables. */
    public static final String SQL_REF 	 			= " a";

    /** Name of the column in any child table, that reference this table's _id column. */
    public static final String COL_FK_ROW_ID    	= (FK+TABLE_NAME+Columns._ID);

    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/"+TABLE_NAME+"/");

    public static interface Columns extends TableBase.Columns {

        /** _id foreign key of Rule table record*/
        public static final String PARENT_FKEY					= RuleTable.COL_FK_ROW_ID;
        /**
         * icon of the Rule pointed by PARENT_FKEY
         */
        public static final String ICON                         = "icon";
    }

    private static final String[] COLUMN_NAMES = {
        Columns._ID, Columns.PARENT_FKEY, Columns.ICON
    };

    public static String[] getColumnNames() {
        return Util.copyOf(COLUMN_NAMES);
    }

    /** alternate index on the Rule foreign key column */
    public static final String PARENT_FK_INDEX_NAME =
        TABLE_NAME+Columns.PARENT_FKEY+INDEX;

    public static final String CREATE_PARENT_FKEY_INDEX =
        CREATE_INDEX
        .replace("ixName", 	PARENT_FK_INDEX_NAME)
        .replace("table", 	TABLE_NAME)
        .replace("field", 	Columns.PARENT_FKEY);


    /** SQL statement to create the Table */
    public static final String CREATE_TABLE_SQL =
        CREATE_TABLE +
        TABLE_NAME + " (" +
        Columns._ID						+ PKEY_TYPE									+ CONT +
        Columns.PARENT_FKEY				+ KEY_TYPE+NOT_NULL							+ CONT +
        Columns.ICON                      + BLOB_TYPE									+ CONT +
        FOREIGN_KEY +" ("+Columns.PARENT_FKEY+
        ") "+ REFERENCES +RuleTable.TABLE_NAME+" ("+RuleTable.Columns._ID+")" +
        ")";


    /** Basic constructor
     */
    public IconTable() {
        super();
    }


    /** Get the table name for this table.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getTableName()
     */
    @Override
    public String getTableName() {
        return TABLE_NAME;
    }


    /** gets the foreign key column for this table.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getTableName()
     */
    @Override
    public String getFkColName() {
        return COL_FK_ROW_ID;
    }

    /** converts to ContentValues
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toContentValues(com.motorola.contextual.smartrules.db.table.TupleBase)
     * @param _tuple - instance to convert to content values.
     * @return content values for this instance
     */
    @Override
    public <T extends TupleBase> ContentValues toContentValues(T _tuple) {

        ContentValues args = new ContentValues();
        if (_tuple instanceof IconTuple) {
            IconTuple tuple = (IconTuple) _tuple;

            if (tuple.get_id() > 0) {
                args.put(Columns._ID, 							tuple.get_id());
            }
            args.put(Columns.PARENT_FKEY, 						tuple.getParentFk());

            args.put(Columns.ICON, tuple.getIconBlob());

        }
        return args;
    }

    @SuppressWarnings("unchecked")
    public IconTuple fetch1(Context context, String whereClause) {
        IconTuple tuple = null;
        Cursor cursor = this.fetchWhere(context, null, whereClause, null, null, 1);
        if(cursor != null && cursor.moveToFirst()) {
            int[] tupleColNos = getColumnNumbers(cursor, "");
            int ix = 0;
            long id = cursor.getLong(tupleColNos[ix++]);
            long parentKey = cursor.getLong(tupleColNos[ix++]);
            tuple = new IconTuple(
                // NOTE: The order of these must match the physical order in the CREATE_TABLE syntax above, as well as the constructor
                id, 			// _id
                parentKey, 		// parentFkey
                cursor.getBlob(tupleColNos[ix++])		    // icons
            );

        }
        if (cursor!=null && !cursor.isClosed()) cursor.close();
        return tuple;
    }

    /** returns a tuple from a cursor position.
     *
     * @param cursor - cursor - current position used to convert to tuple.
     * @param tupleColumnNumbersIndex - contains column numbers which must be in sync here
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toTuple(android.content.Context, android.database.Cursor, int[])
     */
    @SuppressWarnings("unchecked")
    public IconTuple toTuple(final Cursor cursor, int[] tupleColumnNumbersIndex) {

        return toTuple(cursor, tupleColumnNumbersIndex, 0);
    }


    /** returns a tuple from a cursor position.
     *
     * @param context - context
     * @param cursor - cursor - current position used to convert to tuple.
     * @param tupleColumnNumbersIndex - contains column numbers which must be in sync here
     * @param ix - current index position in tupleColumnNumbersIndex to start.
     *
     * @see com.motorola.contextual.smartrules.db.table.TableBase#toTuple(android.content.Context, android.database.Cursor, int[])
     */
    private IconTuple toTuple(Cursor cursor, int[] tupleColNos, int ix) {
        IconTuple tuple = null;
        long id = cursor.getLong(tupleColNos[ix++]);
        long parentKey = cursor.getLong(tupleColNos[ix++]);
        tuple = new IconTuple(
            // NOTE: The order of these must match the physical order in the CREATE_TABLE syntax above, as well as the constructor
            id, 			// _id
            parentKey, 		// parentFkey
            cursor.getBlob(tupleColNos[ix++])		    // icons
        );
        if(ix != tupleColNos.length) {
            throw new UnsupportedOperationException("tupleColNos length = "+
                                                    tupleColNos.length+" and ix = "+ix+" do not match");
        }

        return tuple;
    }

    /** gets column numbers so that data can be extracted from a cursor
     *
     * @param cursor - cursor to be parsed to s
     * @param sqlRef - reference prefix for the table name like "g." if needed.
     * @return int array of column numbers.
     * @see com.motorola.contextual.smartrules.db.table.TableBase#getColumnNumbers(android.database.Cursor, java.lang.String, java.lang.String)
     */
    public int[] getColumnNumbers(Cursor cursor, String sqlRef) {

        String _idColName = getRowIdColName(cursor);


        String[] colNames = {	_idColName,
                                sqlRef+Columns.PARENT_FKEY,
                                sqlRef+Columns.ICON,
                            };
        return TableBase.getColumnNumbers(cursor, colNames);
    }

    /** required by ModalTable */
    public Uri getTableUri() {
        return CONTENT_URI;
    }

    /** deletes icon for 1 rule
    *
    * @param context
    * @param rule_id
    */
    public static void deleteAllIcons(Context context, long rule_id) {
        // delete all icons
        new IconTable().massDelete(context, IconTable.Columns.PARENT_FKEY+EQUALS+rule_id);
    }
}