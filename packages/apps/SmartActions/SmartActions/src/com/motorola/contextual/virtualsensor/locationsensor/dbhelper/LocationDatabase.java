/*
 * (c) COPYRIGHT 2009-2012 MOTOROLA INC.
 * MOTOROLA CONFIDENTIAL PROPRIETARY
 * MOTOROLA Advanced Technology and Software Operations
 *
 * REVISION HISTORY:
 * Author        Date       CR Number         Brief Description
 * ------------- ---------- ----------------- ------------------------------
 * e51141        2010/08/27 IKCTXTAW-19		   Initial version
 */
package com.motorola.contextual.virtualsensor.locationsensor.dbhelper;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.motorola.contextual.virtualsensor.locationsensor.Utils;


/**
 *<code><pre>
 * CLASS:
 *  implements operations on location database
 *
 * RESPONSIBILITIES:
 *  insert, update, delete
 *
 * COLABORATORS:
 *
 * USAGE:
 * 	See each method.
 *
 *</pre></code>
 */
public class LocationDatabase {

    protected final Context mContext;
    public DbAdapter mDbAdapter;

    private static LocationDatabase sSingleton = null;
    public static synchronized LocationDatabase getInstance(Context context) {
        if (sSingleton == null) {
            sSingleton  = new LocationDatabase(context);
        }
        return sSingleton;
    }

    protected LocationDatabase(Context c) {
        mContext = c;
        mDbAdapter = DbAdapter.openForWrite(c, null);
    }

    public static class CellTable extends BaseTable implements DbSyntax {

        /** This is the name of the table in the database */
        public static final String TABLE_NAME 	= "celltowers";

        /** Name of the foreign key column in other tables, that reference this table. */
        public static final String FK_ID = 	(FK+TABLE_NAME+Columns._ID);

        /** Currently not used, but could be for joining this table with other tables. */
        public static final String SQL_REF 				= "ct";

        /** Friend name - column name */
        public static class Columns {
            public static final String _ID				= "_id";
            public static final String NAME 			= "Name";
            public static final String JSONVALUE 		= "JsonValue";
            public static final String TIMESTAMP 		= "Timestamp";
            public static final String COUNT 			= "Count";
            public static final String LAT				= "Lat";
            public static final String LGT				= "Lgt";

            final static String[] NAMES = {_ID,  NAME, JSONVALUE, TIMESTAMP, COUNT, LAT, LGT };

            public static final int sNameIdx = 1;
            public static final int sJsonIdx = 2;
            public static final int sTimestampIdx = 3;
            public static final int sCountIdx = 4;
            public static final int sLatIdx = 5;
            public static final int sLgtIdx = 6;
        }


        /** SQL statement to create the Friend Table */
        public static final String CREATE_TABLE_SQL =
            CREATE_TABLE +
            TABLE_NAME + " (" +
            Columns._ID					+ PKEY_TYPE			+ CONT +
            Columns.NAME				+ TEXT_TYPE	    	+ CONT+  // unique doesn't create an index
            Columns.JSONVALUE			+ TEXT_TYPE	    	+ CONT+
            Columns.TIMESTAMP			+ DATE_TIME_TYPE	+ CONT+
            Columns.COUNT				+ LONG_TYPE			+ CONT+
            Columns.LAT					+ REAL_TYPE			+ CONT+
            Columns.LGT					+ REAL_TYPE			+
            ")";

        @Override
        public String getTableName() {
            return TABLE_NAME;
        }

        protected static int[] getColumnNumbers(Cursor cursor, String sqlRef) {
            return BaseTable.getColumnNumbers(cursor, Columns.NAMES);
        }

        @Override
        public String getFkColName() {
            return FK_ID;
        }

        public static ContentValues toContentValues(Tuple tuple) {
            ContentValues args = new ContentValues();

            if (tuple.get_id() > 0) {
                args.put(Columns._ID, tuple.get_id());
            }
            args.put(Columns.NAME,			tuple.getName());
            args.put(Columns.JSONVALUE,	    tuple.getCompoundID());
            args.put(Columns.TIMESTAMP,		tuple.getTimestamp());
            args.put(Columns.COUNT,			tuple.getCount());
            args.put(Columns.LAT,			tuple.getLat());
            args.put(Columns.LGT,			tuple.getLgt());
            return args;
        }

        @SuppressWarnings("unchecked")
        public static Tuple toTuple(Cursor cursor) {
            int ix = 0;
            int[] colNumbers = getColumnNumbers(cursor, "");

            Tuple tuple = new Tuple(
                cursor.getLong(colNumbers[ix++]), 			//	_id
                cursor.getString(colNumbers[ix++]),  		// name
                cursor.getString(colNumbers[ix++]),  		// jsonvalue
                cursor.getLong(colNumbers[ix++]),  			// timestamp
                cursor.getLong(colNumbers[ix++]),  			// count
                cursor.getLong(colNumbers[ix++]),  			// lat
                cursor.getLong(colNumbers[ix++])  			// lgt
            );

            if (ix != colNumbers.length) {
                throw new UnsupportedOperationException("colNumbers length = "+
                                                        colNumbers.length+" and ix = "+ix+" do not match");
            }

            return tuple;
        }


        public static class Tuple extends BaseTuple implements Parcelable, Comparable<Tuple> {

            /** name of this MimeType */
            private   String 	name;
            private   String 	compoundid;
            private   long 		timestamp;
            private   long 		count;
            private   double	lat;
            private	  double	lgt;

            public Tuple() {
                super();
            }

            public Tuple(long _id, final String name,
                         final String compoundid,
                         final long timestamp,
                         final long count,
                         final double lat,
                         final double lgt
                        ) {
                super();
                this._id = _id;
                this.name = name;
                this.compoundid = compoundid;   // do you need defensive copy ?
                this.timestamp = timestamp;
                this.count = count;
                this.lat = lat;
                this.lgt = lgt;
            }


            /** constructor using a parcel to re-inflate.
             *
             * @param parcel
             */
            public Tuple(Parcel parcel) {
                super(parcel);
            }


            /** writes this StateTuple to a parcel.
             *
             * @see com.motorola.contextual.smartrules.util.ParcelableBase#writeToParcel(android.os.Parcel, int)
             */
            public void writeToParcel(Parcel parcel, int flags) {
                super.writeToParcel(parcel, flags);
            }


            /** Creator field implementation for parcel restore operations.
             */
            public static final Parcelable.Creator<Tuple> CREATOR =
            new Parcelable.Creator<Tuple>() {
                public Tuple createFromParcel(Parcel in) {
                    return new Tuple(in);
                }

                public Tuple [] newArray(int size) {
                    return new Tuple[size];
                }
            };

            /**
             * @return the mimeTypeName
             */
            public String getName() {
                return name;
            }

            /**
             * @param name the mimeTypeName to set
             */
            public void setName(String name) {
                this.name = name;
            }


            /**
             * @return the stateName1
             */
            public String getCompoundID() {
                return compoundid;
            }


            /**
             * @param stateName1 the stateName1 to set
             */
            public void getCompoundID(String compoundid) {
                this.compoundid = compoundid;
            }


            /**
             * @return the stateName2
             */
            public long getTimestamp() {
                return timestamp;
            }
            public void setTimestamp(long timestamp) {
                this.timestamp = timestamp;
            }

            public long getCount() {
                return count;
            }
            public void setCount(long count) {
                this.count = count;
            }

            public double getLat() {
                return lat;
            }
            public void setLat(double lat) {
                this.lat = lat;
            }
            public double getLgt() {
                return lgt;
            }
            public void setLgt(double lgt) {
                this.lgt = lgt;
            }


            /** for sorting
             * @param another - another tuple to be compared
             */
            //@Override
            public int compareTo(Tuple another) {
                return this.getCompoundID().compareTo(another.getCompoundID());
            }

            @Override
            public int hashCode() {
                return (int)(this.lat * 1E7 + this.lgt * 1E6);
            }


            @Override  // override equal and hashcode together
            public boolean equals(Object o) {
                if ( this == o ) return true;
                if ( !(o instanceof Tuple) ) return false;

                if (1 == compareTo((Tuple)o)) // 1 means equals
                    return true;
                else
                    return false;
            }


            /** converts this instance to a string.
             *
             * @see com.motorola.BaseTuple.location.ils.db.table.TupleBase#toString()
             */
            public String toString() {

                StringBuilder result = new StringBuilder();
                result.append(TABLE_NAME);

                result.append(", _id="+this._id);
                result.append(", name="+this.name);
                result.append(", compoundid="+this.compoundid);
                result.append(", timestamp="+this.timestamp);
                result.append(", count="+this.count);
                result.append(", lat=" +this.lat);
                result.append(", lgt="+this.lgt);

                return result.toString();
            }
        }
    }

    /**
     * this class represents loctime table in location sensor db.
     * it stores metadata of discovered locations, including lat/lng/accurracy/ etc.
     */
    public static class LocTimeTable extends BaseTable implements DbSyntax {
        /** This is the name of the table in the database */
        public static final String TABLE_NAME 	= "loctime";

        /** Name of the foreign key column in other tables, that reference this table. */
        public static final String FK_ID = 	(FK+TABLE_NAME+Columns._ID);

        /** Currently not used, but could be for joining this table with other tables. */
        public static final String SQL_REF 				= "loctime";

        /** Friend name - column name */
        public static class Columns {
            public final static String _ID      = "_id";
            public final static String LAT				= "Lat";
            public final static String LGT				= "Lgt";
            public final static String STARTTIME 		= "StartTime";
            public final static String ENDTIME 		    = "EndTime";
            public final static String NAME 			= "Name";
            public final static String COUNT 			= "Count";
            public final static String ACCURACY 		= "Accuracy";
            public final static String BESTACCURACY 	= "BestAccuracy";
            public final static String ACCUNAME 		= "Accuname";
            public final static String POITAG	 		= "Poitag";
            public final static String CELLJSONVALUE 	= "CellJsonValue";
            public final static String WIFISSID 		= "wifissid";
            // column index matters when used by cursor to tuple!

            public static final int sLatIdx = 1;
            public static final int sLgtIdx = 2;
            public static final int sStartTimeIdx = 3;
            public static final int sEndTimeIdx = 4;
            public static final int sNameIdx = 5;
            public static final int sCountIdx = 6;
            public static final int sAccuracyIdx = 7;
            public static final int sBestAccuracyIdx = 8;
            public static final int sAccunameIdx = 9;
            public static final int sPoitagIdx = 10;
            public static final int sCellJsonValueIdx = 11;
            public static final int sWifiSsidIdx = 12;

            final static String[] NAMES = {_ID, LAT, LGT, STARTTIME, ENDTIME, NAME, COUNT, ACCURACY, BESTACCURACY, ACCUNAME, POITAG, CELLJSONVALUE, WIFISSID };
            public static String[] getNames() {
                return (String[])NAMES.clone();
            }
        }


        /** SQL statement to create the Friend Table */
        public static final String CREATE_TABLE_SQL =
            CREATE_TABLE +
            TABLE_NAME + " (" +
            Columns._ID					+ PKEY_TYPE			+ CONT +
            Columns.LAT					+ REAL_TYPE			+ CONT+
            Columns.LGT					+ REAL_TYPE			+ CONT+
            Columns.STARTTIME			+ DATE_TIME_TYPE	+ CONT+
            Columns.ENDTIME				+ DATE_TIME_TYPE	+ CONT+
            Columns.NAME				+ TEXT_TYPE	    	+ CONT+  // unique doesn't create an index
            Columns.COUNT				+ LONG_TYPE			+ CONT+
            Columns.ACCURACY			+ LONG_TYPE			+ CONT+
            Columns.BESTACCURACY		+ LONG_TYPE			+ CONT+
            Columns.ACCUNAME			+ TEXT_TYPE			+ CONT+
            Columns.POITAG				+ TEXT_TYPE			+ CONT+
            Columns.CELLJSONVALUE		+ TEXT_TYPE			+ CONT+
            Columns.WIFISSID    		+ TEXT_TYPE			+
            ")";

        /* the table is iterated thru based on time sequentially, so no need for any index for now
         * will consider adding index if we */

        @Override
        public String getTableName() {
            return TABLE_NAME;
        }

        //@Override
        protected static int[] getColumnNumbers(Cursor cursor, String sqlRef) {
            //return super.getColumnNumbers(cursor, Columns.NAMES);
            return BaseTable.getColumnNumbers(cursor, Columns.NAMES);
        }


        @Override
        public String getFkColName() {
            return FK_ID;
        }

        //@Override
        public static ContentValues toContentValues(Tuple tuple) {
            ContentValues args = new ContentValues();

            if (tuple.get_id() > 0) {
                args.put(Columns._ID, tuple.get_id());
            }
            args.put(Columns.LAT,			tuple.getLat());
            args.put(Columns.LGT,			tuple.getLgt());
            args.put(Columns.STARTTIME,	    tuple.getStartTime());
            args.put(Columns.ENDTIME,	    tuple.getEndTime());
            args.put(Columns.NAME,			tuple.getName());
            args.put(Columns.COUNT,			tuple.getCount());
            args.put(Columns.ACCURACY,		tuple.getAccuracy());
            args.put(Columns.BESTACCURACY,	tuple.getBestAccuracy());
            args.put(Columns.ACCUNAME,		tuple.getAccuName());
            args.put(Columns.POITAG,		tuple.getPoiTag());
            args.put(Columns.CELLJSONVALUE,	tuple.getCellJsonValue());
            args.put(Columns.WIFISSID,	    tuple.getCellJsonValue());

            return args;
        }

        /**
         * convert a db tuple to a matrix cursor with the row builder.
         * Note that the column seq must match col seq in NAMES, as the matrix cursor was inited that way.
         * @param t the database tuple
         * @param nrow the matrix cursor row builder to be filled.
         */
        public static void toMatrixCursorRow(Tuple t, MatrixCursor.RowBuilder nrow) {
            nrow.add(t.get_id());
            nrow.add(t.getLat());
            nrow.add(t.getLgt());
            nrow.add(t.getStartTime());
            nrow.add(t.getEndTime());
            nrow.add(t.getName());
            nrow.add(t.getCount());
            nrow.add(t.getAccuracy());
            nrow.add(t.getBestAccuracy());
            nrow.add(t.getAccuName());
            nrow.add(t.getPoiTag());
            nrow.add(t.getCellJsonValue());
            nrow.add(t.getWifiSsid());
        }


        //@Override
        @SuppressWarnings("unchecked")
        public static LocTimeTable.Tuple toTuple(Cursor cursor) {
            int ix = 0;
            int[] colNumbers = getColumnNumbers(cursor, "");

            LocTimeTable.Tuple tuple = new LocTimeTable.Tuple(
                cursor.getLong(colNumbers[ix++]), 			//_id
                cursor.getDouble(colNumbers[ix++]),  		// lat
                cursor.getDouble(colNumbers[ix++]),  		// lgt
                cursor.getLong(colNumbers[ix++]),  			// start time
                cursor.getLong(colNumbers[ix++]),  			// end time
                cursor.getString(colNumbers[ix++]),  		// name
                cursor.getLong(colNumbers[ix++]),  			// count
                cursor.getLong(colNumbers[ix++]),			// accuracy
                cursor.getLong(colNumbers[ix++]),			// bestaccuracy
                cursor.getString(colNumbers[ix++]), 		// accuname
                cursor.getString(colNumbers[ix++]), 		// poitag
                cursor.getString(colNumbers[ix++]),    		// celljsonvalue
                cursor.getString(colNumbers[ix++])    		// wifissid
            );

            if (ix != colNumbers.length) {
                throw new UnsupportedOperationException("colNumbers length = "+
                                                        colNumbers.length+" and ix = "+ix+" do not match");
            }
            return tuple;
        }


        public static class Tuple extends BaseTuple implements Parcelable, Comparable<Tuple> {

            /** name of this MimeType */
            private   double	lat;
            private	  double	lgt;
            private   long 		starttime;
            private   long 		endtime;
            private   String 	name;
            private   long 		count;
            private	  long		accuracy;
            private   long      bestaccuracy;
            private	  String	accuname;
            private	  String	poitag;
            private	  String	celljsonvalue;
            private	  String	wifissid;

            public Tuple() {
                super();
            }

            public Tuple(long _id, final double lat, final double lgt,
                         final long starttime, final long endtime,
                         final String name, final long count,
                         final long accuracy, final long bestaccuracy, final String accuname, final String poitag,
                         final String celljsonvalue, final String wifissid) {
                super();
                this._id = _id;
                this.name = name;
                this.starttime = starttime;   // do you need defensive copy ?
                this.endtime = endtime;
                this.count = count;
                this.lat = lat;
                this.lgt = lgt;
                this.accuracy = accuracy;
                this.bestaccuracy = bestaccuracy;
                this.accuname = accuname;
                this.poitag = poitag;
                this.celljsonvalue = celljsonvalue;
                this.wifissid = wifissid;
            }


            /** constructor using a parcel to re-inflate.
             *
             * @param parcel
             */
            public Tuple(Parcel parcel) {
                super(parcel);
            }


            /** writes this StateTuple to a parcel.
             *
             * @see com.motorola.contextual.smartrules.util.ParcelableBase#writeToParcel(android.os.Parcel, int)
             */
            public void writeToParcel(Parcel parcel, int flags) {
                super.writeToParcel(parcel, flags);
            }


            /** Creator field implementation for parcel restore operations.
             */
            public static final Parcelable.Creator<Tuple> CREATOR =
            new Parcelable.Creator<Tuple>() {

                public Tuple createFromParcel(Parcel in) {
                    return new Tuple(in);
                }

                public Tuple [] newArray(int size) {
                    return new Tuple[size];
                }
            };

            public boolean isValidLatLgt() {
                return Utils.compareDouble(lat, 0.0) && Utils.compareDouble(lgt, 0.0);
            }
            public double getLat() {
                return lat;
            }
            public void setLat(double lat) {
                this.lat = lat;
            }
            public double getLgt() {
                return lgt;
            }
            public void setLgt(double lgt) {
                this.lgt = lgt;
            }

            public long getStartTime() {
                return starttime;
            }
            public void setStartTime(long timestamp) {
                this.starttime = timestamp;
            }
            public long getEndTime() {
                return endtime;
            }
            public void setEndTime(long timestamp) {
                this.endtime = timestamp;
            }
            public String getName() {
                return name;
            }
            public void setName(String name) {
                this.name = name;
            }
            public long getCount() {
                return count;
            }
            public void setCount(long count) {
                this.count = count;
            }
            public long getAccuracy() {
                return accuracy;
            }
            public void setAccuracy(long accu) {
                this.accuracy = accu;
            }
            public long getBestAccuracy() {
                return bestaccuracy;
            }
            public void setBestAccuracy(long accu) {
                this.bestaccuracy = accu;
            }
            public String getAccuName() {
                return accuname;
            }
            public void setAccuName(String name) {
                this.accuname = name;
            }
            public String getPoiTag() {
                return poitag;
            }
            public void setPoiTag(String name) {
                this.poitag = name;
            }
            public void setCellJsonValue(String cjv) {
                this.celljsonvalue = cjv;
            }
            public String getCellJsonValue() {
                return celljsonvalue;
            }
            public String getWifiSsid() {
                return wifissid;
            }
            public void setWifiSsid(String wifissid) {
                this.wifissid = wifissid;
            }

            /**
             * @param another - another tuple to be compared
             * return 1 means equals, a flip of common standard. sorry!
             */
            //@Override
            public int compareTo(Tuple another) {
                return ! Utils.compareDouble(this.lat, another.getLat()) && ! Utils.compareDouble(this.lgt, another.getLgt()) &&
                       this.starttime == another.getStartTime() && this.endtime == another.getEndTime() ? 1 : 0;
            }

            @Override
            public int hashCode() {
                return (int)(this.lat * 1E7 + this.lgt * 1E6 + this.starttime + this.endtime);
            }

            @Override  // override equal and hashcode together
            public boolean equals(Object o) {
                if ( this == o ) return true;
                if ( !(o instanceof Tuple) ) return false;

                if (1 == compareTo((Tuple)o)) // 1 means equals
                    return true;
                else
                    return false;
            }

            /** converts this instance to a string.
             *
             * @see com.motorola.BaseTuple.location.ils.db.table.TupleBase#toString()
             */
            public String toString() {
                StringBuilder result = new StringBuilder();
                result.append(TABLE_NAME);

                result.append(", _id="+this._id);
                result.append(", lat=" +this.lat);
                result.append(", lgt="+this.lgt);
                result.append(", starttime="+this.starttime);
                result.append(", endtime="+this.endtime);
                result.append(", name="+this.name);
                result.append(", count="+this.count);
                result.append(", accuracy="+this.accuracy);
                result.append(", bestaccuracy="+this.bestaccuracy);
                result.append(", accuname="+this.accuname);
                result.append(", poitag="+this.poitag);
                result.append(", celljsonvalue="+this.celljsonvalue);
                result.append(", wifissid="+this.wifissid);
                return result.toString();
            }
        }
    }

    /**
     * this table stores all the beacon sensor(wifi, bt) within one cell to link cell to beacon sensor.
     * Not used for now.
     */
    @Deprecated
    public static class CellSensorTable extends BaseTable implements DbSyntax {
        /** This is the name of the table in the database */
        public static final String TABLE_NAME 	= "cellsensor";

        /** Name of the foreign key column in other tables, that reference this table. */
        public static final String FK_ID = 	(FK+TABLE_NAME+Columns._ID);

        /** Currently not used, but could be for joining this table with other tables. */
        public static final String SQL_REF 				= "cellsensor";

        /** Friend name - column name */
        public static class Columns {
            public final static String _ID		= "_id";
            public final static String CELL			= "cell";
            public final static String POI			= "poitag";
            public final static String NAME			= "name";
            public final static String SADDR  		= "saddr";
            public final static String TIME 		= "time";

            // column index matters when used by cursor to tuple!
            final static String[] NAMES = {_ID, CELL, POI, NAME, SADDR, TIME};

            public static final int sCellIdx = 1;
            public static final int sPOIIdx  = 2;
            public static final int sNAMEIdx = 3;
            public static final int sSAddrIdx = 4;
            public static final int sTimeIdx = 5;
        }


        /** SQL statement to create the Friend Table */
        public static final String CREATE_TABLE_SQL =
            CREATE_TABLE +
            TABLE_NAME + " (" +
            Columns._ID					+ PKEY_TYPE			+ CONT+
            Columns.CELL				+ TEXT_TYPE			+ CONT+
            Columns.POI	    			+ TEXT_TYPE +UNIQUE + CONT+
            Columns.NAME				+ TEXT_TYPE			+ CONT+
            Columns.SADDR				+ TEXT_TYPE			+ CONT+
            Columns.TIME				+ DATE_TIME_TYPE	+
            ")";

        @Override
        public String getTableName() {
            return TABLE_NAME;
        }

        protected static int[] getColumnNumbers(Cursor cursor, String sqlRef) {
            //return super.getColumnNumbers(cursor, Columns.NAMES);
            return BaseTable.getColumnNumbers(cursor, Columns.NAMES);
        }

        public String getFkColName() {
            return FK_ID;
        }

        public static ContentValues toContentValues(CellSensorTable.Tuple tuple) {
            ContentValues args = new ContentValues();

            if (tuple.get_id() > 0) {
                args.put(Columns._ID, tuple.get_id());
            }
            args.put(Columns.CELL,			tuple.getCell());
            args.put(Columns.POI,			tuple.getPoi());
            args.put(Columns.NAME,			tuple.getName());
            args.put(Columns.SADDR,			tuple.getSAddr());
            args.put(Columns.TIME,	    	tuple.getTime());
            return args;
        }

        @SuppressWarnings("unchecked")
        public static Tuple toTuple(Cursor cursor) {
            int ix = 0;
            int[] colNumbers = getColumnNumbers(cursor, "");

            Tuple tuple = new Tuple(
                cursor.getLong(colNumbers[ix++]), 			//_id
                cursor.getString(colNumbers[ix++]),  		// cell
                cursor.getString(colNumbers[ix++]),  		// poi
                cursor.getString(colNumbers[ix++]),  		// name
                cursor.getString(colNumbers[ix++]),  		// saddr
                cursor.getLong(colNumbers[ix++])  			// time
            );

            if (ix != colNumbers.length) {
                throw new UnsupportedOperationException("colNumbers length = "+
                                                        colNumbers.length+" and ix = "+ix+" do not match");
            }
            return tuple;
        }

        public static class Tuple extends BaseTuple implements Parcelable, Comparable<Tuple> {
            /** name of this MimeType */
            private   String	cell;
            private   String	poi;
            private   String	name;
            private	  String	saddr;
            private   long 		time;

            public Tuple() {
                super();
            }

            public Tuple(long _id, final String cell, final String poi, final String name, final String saddr, final long time) {
                super();
                this._id = _id;
                this.cell = cell;
                this.poi = poi;
                this.name = name;
                this.saddr = saddr;
                this.time = time;
            }

            public Tuple(Parcel parcel) {
                super(parcel);
            }


            /** writes this StateTuple to a parcel.
             *
             * @see com.motorola.contextual.smartrules.util.ParcelableBase#writeToParcel(android.os.Parcel, int)
             */
            public void writeToParcel(Parcel parcel, int flags) {
                super.writeToParcel(parcel, flags);
            }


            /** Creator field implementation for parcel restore operations.
             */
            public static final Parcelable.Creator<Tuple> CREATOR =
            new Parcelable.Creator<Tuple>() {

                public Tuple createFromParcel(Parcel in) {
                    return new Tuple(in);
                }

                public Tuple [] newArray(int size) {
                    return new Tuple[size];
                }
            };


            public String getCell() {
                return cell;
            }
            public void setCell(String cell) {
                this.cell = cell;
            }
            public String getPoi() {
                return poi;
            }
            public void setPoi(String poi) {
                this.poi = poi;
            }
            public String getName() {
                return name;
            }
            public void setName(String name) {
                this.name = name;
            }
            public String getSAddr() {
                return saddr;
            }
            public void setSAddr(String saddr) {
                this.saddr = saddr;
            }

            public long getTime() {
                return time;
            }
            public void setTime(long timestamp) {
                this.time = timestamp;
            }

            // Comparator, functor, closure ?
            public int compareTo(Tuple another) {
                return this.cell.equalsIgnoreCase(another.getCell()) && this.saddr.equalsIgnoreCase(another.getSAddr()) ? 1 : 0;
            }

            @Override
            public int hashCode() {
                return (int)(this.cell.hashCode() + this.saddr.hashCode());
            }

            @Override  // override equal and hashcode together for object comparator!!!!
            public boolean equals(Object o) {
                if ( this == o ) return true;
                if ( !(o instanceof Tuple) ) return false;

                if (1 == compareTo((Tuple)o))
                    return true;
                else
                    return false;
            }

            /** converts this instance to a string.
             *
             * @see com.motorola.BaseTuple.location.ils.db.table.TupleBase#toString()
             */
            public String toString() {
                StringBuilder result = new StringBuilder();
                result.append(TABLE_NAME);

                result.append(", _id="+this._id);
                result.append(", cell=" +this.cell);
                result.append(", poi="+this.poi);
                result.append(", saddr="+this.saddr);
                result.append(", time="+this.time);
                return result.toString();
            }
        }
    }

    public static class PoiTable extends BaseTable implements DbSyntax {
        /** This is the name of the table in the database */
        public static final String TABLE_NAME 	= "poi";

        /** Name of the foreign key column in other tables, that reference this table. */
        public static final String FK_ID = 	(FK+TABLE_NAME+Columns._ID);

        /** Currently not used, but could be for joining this table with other tables. */
        public static final String SQL_REF 		= "poi";

        /** Friend name - column name */
        public static class Columns {
            public final static String _ID		= "_id";
            public final static String POI		= "poi";
            public final static String LAT		= "lat";
            public final static String LGT		= "lgt";
            public final static String RADIUS	= "radius";
            public final static String ADDR 	= "addr";
            public final static String NAME 	= "name";
            public final static String POITYPE  = "poitype";
            public final static String CELLJSON	= "celljsons";
            public final static String WIFIMAC  = "wifimac";
            public final static String WIFICONNMAC = "wificonnmac";
            public final static String WIFISSID = "wifissid";
            public final static String BTMAC   	= "btmac";
            public final static String TIME 	= "time";


            // column index matters when used by cursor to tuple!
            final static String[] NAMES = {_ID, POI, LAT, LGT, RADIUS, ADDR, NAME, POITYPE, CELLJSON, WIFIMAC, WIFICONNMAC, WIFISSID, BTMAC, TIME};

            public static String[] getNames() {
                return (String[])NAMES.clone();
            }

            public static final int sPoiIdx = 1;
            public static final int sLatIdx  = 2;
            public static final int sLgtIdx = 3;
            public static final int sRadiusIdx = 4;
            public static final int sAddrIdx = 5;
            public static final int sNameIdx = 6;
            public static final int sPoiTypeIdx = 7;
            public static final int sCelljsonIdx = 8;
            public static final int sWifiMacIdx = 9;
            public static final int sWifiConnMacIdx = 10;
            public static final int sWifiSsidIdx = 11;
            public static final int sBtMacIdx = 12;
            public static final int sTimeIdx = 13;
        }

        /** SQL statement to create the Friend Table */
        public static final String CREATE_TABLE_SQL =
            CREATE_TABLE +
            TABLE_NAME + " (" +
            Columns._ID					+ PKEY_TYPE			+ CONT+
            Columns.POI					+ TEXT_TYPE +UNIQUE	+ CONT+
            Columns.LAT	    			+ REAL_TYPE			+ CONT+
            Columns.LGT	    			+ REAL_TYPE			+ CONT+
            Columns.RADIUS    			+ LONG_TYPE			+ CONT+
            Columns.ADDR    			+ TEXT_TYPE			+ CONT+
            Columns.NAME				+ TEXT_TYPE			+ CONT+
            Columns.POITYPE				+ TEXT_TYPE			+ CONT+
            Columns.CELLJSON			+ TEXT_TYPE			+ CONT+
            Columns.WIFIMAC				+ TEXT_TYPE			+ CONT+
            Columns.WIFICONNMAC			+ TEXT_TYPE			+ CONT+
            Columns.WIFISSID			+ TEXT_TYPE			+ CONT+
            Columns.BTMAC				+ TEXT_TYPE			+ CONT+
            Columns.TIME				+ DATE_TIME_TYPE	+
            ")";

        @Override
        public String getTableName() {
            return TABLE_NAME;
        }

        protected static int[] getColumnNumbers(Cursor cursor, String sqlRef) {
            return BaseTable.getColumnNumbers(cursor, Columns.NAMES);
        }

        public String getFkColName() {
            return FK_ID;
        }

        public static ContentValues toContentValues(PoiTable.Tuple tuple)
        {
            return toContentValues(tuple,false);
        }
        public static ContentValues toContentValues(PoiTable.Tuple tuple, boolean ignoreName) {
            ContentValues args = new ContentValues();

            if (tuple.get_id() > 0) {
                args.put(Columns._ID, tuple.get_id());
            }
            args.put(Columns.POI,			tuple.getPoiName());
            args.put(Columns.LAT,			tuple.getLat());
            args.put(Columns.LGT,			tuple.getLgt());
            args.put(Columns.RADIUS,		tuple.getRadius());
            args.put(Columns.ADDR, tuple.getAddr());
            if (!ignoreName)
                args.put(Columns.NAME, tuple.getName());
            args.put(Columns.POITYPE,		tuple.getPoiType());
            args.put(Columns.CELLJSON,		tuple.getCellJson());
            args.put(Columns.WIFIMAC,		tuple.getWifiMac());
            args.put(Columns.WIFICONNMAC,	tuple.getWifiConnMac());
            args.put(Columns.WIFISSID,  	tuple.getWifiSsid());
            args.put(Columns.BTMAC,			tuple.getBtMac());
            args.put(Columns.TIME,	    	tuple.getTime());
            return args;
        }

        @SuppressWarnings("unchecked")
        public static Tuple toTuple(Cursor cursor) {
            int ix = 0;
            int[] colNumbers = getColumnNumbers(cursor, "");

            Tuple tuple = new Tuple(
                cursor.getLong(colNumbers[ix++]), 			//_id
                cursor.getString(colNumbers[ix++]),  		// poi
                cursor.getDouble(colNumbers[ix++]),  		// lat
                cursor.getDouble(colNumbers[ix++]),  		// lgt
                cursor.getLong(colNumbers[ix++]), 			// radius
                cursor.getString(colNumbers[ix++]),  		// addr
                cursor.getString(colNumbers[ix++]),  		// name
                cursor.getString(colNumbers[ix++]),  		// poitype
                cursor.getString(colNumbers[ix++]),  		// celljson
                cursor.getString(colNumbers[ix++]),  		// wifimac
                cursor.getString(colNumbers[ix++]),  		// wificonnmac
                cursor.getString(colNumbers[ix++]),  		// wifissid
                cursor.getString(colNumbers[ix++]),  		// btmac
                cursor.getLong(colNumbers[ix++])  			// time
            );

            if (ix != colNumbers.length) {
                throw new UnsupportedOperationException("colNumbers length = "+
                                                        colNumbers.length+" and ix = "+ix+" do not match");
            }
            return tuple;
        }

        public static class Tuple extends BaseTuple implements Parcelable, Comparable<Tuple> {
            /** name of this MimeType */
            private   String	poi;
            private   double	lat;
            private   double	lgt;
            private   long  	radius;
            private	  String	addr;
            private   String	name;
            private   String	poitype;
            private   String	celljsons;
            private   String	wifimac;
            private   String	wificonnmac;
            private   String	wifissid;
            private   String	btmac;
            private   long 		time;
            // following are fields for strongest ss and registration
            private	  boolean   pnoregistered;
            private   List<String> topssid;

            public Tuple() {
                super();
            }

            public Tuple(long _id, final String poi, final double lat, final double lgt, final long radius,
                         final String addr, final String name, final String poitype, final String celljsons,
                         final String wifimac, final String wificonnmac, final String wifissid, final String btmac, final long time) {
                super();
                this._id = _id;
                this.poi = poi;
                this.lat = lat;
                this.lgt = lgt;
                this.radius = radius;
                this.addr = addr;
                this.name = name;
                this.poitype = poitype;
                this.celljsons = celljsons;
                this.wifimac = wifimac;
                this.wificonnmac = wificonnmac;
                this.wifissid = wifissid;
                this.btmac = btmac;
                this.time = time;
                this.pnoregistered = false;  // init to false
                this.topssid = new ArrayList<String>();
            }

            public Tuple(Parcel parcel) {
                super(parcel);
            }


            /** writes this StateTuple to a parcel.
             *
             * @see com.motorola.contextual.smartrules.util.ParcelableBase#writeToParcel(android.os.Parcel, int)
             */
            public void writeToParcel(Parcel parcel, int flags) {
                super.writeToParcel(parcel, flags);
            }


            /** Creator field implementation for parcel restore operations.
             */
            public static final Parcelable.Creator<Tuple> CREATOR =
            new Parcelable.Creator<Tuple>() {

                public Tuple createFromParcel(Parcel in) {
                    return new Tuple(in);
                }

                public Tuple [] newArray(int size) {
                    return new Tuple[size];
                }
            };

            public String getPoiName() {
                return poi;
            }
            public void setPoi(String poi) {
                this.poi = poi;
            }

            public double getLat() {
                return lat;
            }
            public void setLat(double lat) {
                this.lat = lat;
            }

            public double getLgt() {
                return lgt;
            }
            public void setLgt(double lgt) {
                this.lgt = lgt;
            }

            public void setRadius(long radius) {
                this.radius = radius;
            }
            public long getRadius() {
                return this.radius;
            }

            public String getAddr() {
                return addr;
            }
            public void setAddr(String addr) {
                this.addr = addr;
            }

            public String getName() {
                return name;
            }
            public void setName(String name) {
                this.name = name;
            }
            public String getPoiType() {
                return poitype;
            }
            public void setPoiType(String poitype) {
                this.poitype = poitype;
            }

            public String getCellJson() {
                return celljsons;
            }
            public void setCellJson(String celljsons) {
                this.celljsons = celljsons;
            }

            public String getWifiMac() {
                return wifimac;
            }
            public void setWifiMac(String wifimac) {
                this.wifimac = wifimac;
            }

            public String getWifiConnMac() {
                return wificonnmac;
            }
            public void setWifiConnMac(String wificonnmac) {
                this.wificonnmac = wificonnmac;
            }

            public String getWifiSsid() {
                return wifissid;
            }
            public void setWifiSsid(String wifissid) {
                this.wifissid = wifissid;
            }

            public String getBtMac() {
                return btmac;
            }
            public void setBtMac(String btmac) {
                this.btmac = btmac;
            }

            public long getTime() {
                return time;
            }
            public void setTime(long timestamp) {
                this.time = timestamp;
            }

            public boolean getPnoRegistration() {
                return this.pnoregistered;
            }
            public boolean setPnoRegistration(boolean flag) {
                this.pnoregistered = flag;
                return this.pnoregistered;
            }
            public List<String> getTopSsid() {
                return this.topssid;
            }
            public void addStrongestSsid(String ssid) {
                this.topssid.add(ssid);
            }
            public void clearStrongestSsid() {
                this.topssid.clear();
            }

            // Comparator, functor, closure ?
            public int compareTo(Tuple another) {
                return this.poi.equalsIgnoreCase(another.getPoiName()) ? 1 : 0;
            }

            @Override
            public int hashCode() {
                return (int)(this.poi.hashCode() + this.addr.hashCode() + this.name.hashCode());
            }

            @Override  // override equal and hashcode together for object comparator!!!!
            public boolean equals(Object o) {
                if ( this == o ) return true;
                if ( !(o instanceof Tuple) ) return false;

                if (1 == compareTo((Tuple)o))
                    return true;
                else
                    return false;
            }

            /** converts this instance to a string.
             *
             * @see com.motorola.BaseTuple.location.ils.db.table.TupleBase#toString()
             */
            public String toString() {
                StringBuilder result = new StringBuilder();
                result.append(TABLE_NAME);

                result.append(", _id="+this._id);
                result.append(", poi="+this.poi);
                result.append(", lat="+this.lat);
                result.append(", lgt="+this.lgt);
                result.append(", radius="+this.radius);
                result.append(", addr="+this.addr);
                result.append(", name="+this.name);
                result.append(", poitype="+this.poitype);
                result.append(", celljson=" +this.celljsons);
                result.append(", wifimac=" +this.wifimac);
                result.append(", wificonnmac=" +this.wificonnmac);
                result.append(", wifissid=" +this.wifissid);
                result.append(", btmac=" +this.btmac);
                result.append(", time="+this.time);
                return result.toString();
            }
        }
    }
}


