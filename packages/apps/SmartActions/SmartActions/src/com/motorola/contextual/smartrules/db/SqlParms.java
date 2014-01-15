/*
 * @(#)SqlParms.java
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


/** This class is not used very much, but should be used more, to help pass the common
 *  parms for a SQL query to various methods.
 *
 *<code><pre>
 * CLASS:
 * 	Wraps where, orderBy and limit clauses into one class for easy passing between methods.
 *
 * RESPONSIBILITIES:
 * 	Simply a repository for the where, orderBy and limit clauses.
 *
 * COLABORATORS:
 * 	none
 *
 * USAGE:
 *
 * 	SqlParms sql = new SqlParms().setLimit(1).setWhereClause(WHERE_CLAUSE);
 * 	MessageContentFields[] messageContent = fetchMatchingMessagesFromSmsDb(cr, sql);
 *
 *</pre></code>
 */
public class SqlParms implements DbSyntax {


    private String 		whereClause;
    private String 		sortOrder = "_id desc";
    private int			limit = 0;


    /** getter for where clause
     * @return the whereClause
     */
    public String getWhereClause() {
        return whereClause;
    }

    /** setter for where clause
     *
     * @param whereClause the whereClause to set
     */
    public SqlParms setWhereClause(String whereClause) {
        this.whereClause = whereClause;
        return this;
    }


    /** getter for sort order
     * @return the sortOrder
     */
    public String getSortOrder() {
        return sortOrder;
    }


    /** setter for the sort order
     *
     * @param sortOrder the sortOrder to set
     */
    public SqlParms setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
        return this;
    }



    /** getter for the LIMIT
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }


    /** setter for the LIMIT
     *
     * @param limit the limit to set
     */
    public SqlParms setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /** Gets ORDER BY and LIMIT clause
     *
     * @return - ORDER BY and LIMIT clause together formatted for SQL
     */
    public String getOrderByClause() {

        return sortOrder +LIMIT+ limit;
    }



}
