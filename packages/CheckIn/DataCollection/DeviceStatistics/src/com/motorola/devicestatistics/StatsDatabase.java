/**
 * Copyright (C) 2011, Motorola Mobility Inc,
 * All Rights Reserved
 * Class name: StatsDatabase.java
 * Description: What the class does.
 *
 * Modification History:
 **********************************************************
 * Date           Author       Comments
 * Jan 30, 2011        bluremployee      Created file
 **********************************************************
 */
package com.motorola.devicestatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.motorola.devicestatistics.DatabaseIface.IDbInitListener;
import com.motorola.devicestatistics.DatabaseIface.Transaction;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * @author bluremployee
 *
 */
public class StatsDatabase implements IDbInitListener {
    
    public final static class Type {
        public final static int LAST = 0;
        public final static int CUMULATIVE = 1;
        public final static int SCREEN_ON = 2;
        public final static int SCREEN_OFF = 3;
    }
    
    public final static class Group {
        public final static String DEFAULT = "default";
        public final static String ALL = "all"; // Special group to retrieve all entries
    }
    
    public final static class CommitLevel {
        public final static int GROUP = 0;
        public final static int SUBID = 1;
        public final static int KEY = 2;
    }

    private final static String TAG = "StatsDatabase";
    
    private final static String[] TABLES = new String[] {
        "last_stats",
        "cumulative_stats",
        "screenon_stats",
        "screenoff_stats",
        "idmap"
    };

    private final static String CREATE_TABLE_TEMPLATE =
        "CREATE TABLE IF NOT EXISTS TABLENAME" +
                    " (id INTEGER PRIMARY KEY ASC AUTOINCREMENT, " +
                    " group_id INTEGER, " +
                    " sub_id INTEGER, " +
                    " stat_name TEXT," +
                    " stat_value TEXT," +
                    " FOREIGN KEY (group_id) REFERENCES idmap (id)," +
                    " FOREIGN KEY (sub_id) REFERENCES idmap (id)," +
                    " UNIQUE (group_id, sub_id, stat_name) )";
    
    private final static String[] TABLE_CREATE_Q = new String[] {
        CREATE_TABLE_TEMPLATE.replace( "TABLENAME", "last_stats" ),
        CREATE_TABLE_TEMPLATE.replace( "TABLENAME", "cumulative_stats" ),
        CREATE_TABLE_TEMPLATE.replace( "TABLENAME", "screenon_stats" ),
        CREATE_TABLE_TEMPLATE.replace( "TABLENAME", "screenoff_stats" ),
        "CREATE TABLE IF NOT EXISTS idmap" +
                    " (id INTEGER PRIMARY KEY ASC," +
                    " map_value TEXT," +
                    " UNIQUE (map_value) )" 
    };
    
    /*
     * Mapping:
     * First level - groups to map
     * Second level - sub-id to map
     * Third level - key-value map
     */
    private static class GroupMap {
        String mGroup;
        private HashMap<String, HashMap<String, String>> mValueMap;
        
        GroupMap(String group) {
            mGroup = group;
            mValueMap = new HashMap<String, HashMap<String, String>>();
        }
        
        boolean addMap(String subid, String name, String value) {
            HashMap<String, String> map = mValueMap.get(subid);
            if(map == null) {
                map = new HashMap<String, String>();
                mValueMap.put(subid, map);
            }
            map.put(name, value);
            return true;
        }
    }
    
    private ArrayList<GroupMap> mGroups;
    private int mType;
    private DatabaseIface mDb;

    private StatsDatabase(Context context, int type) {
        mType = type;
        mGroups = new ArrayList<GroupMap>();
        mDb = DatabaseIface.getInstance(context, this);
    }
    
    private void load(String query) {
        if(query != null) {
            Cursor c = mDb.doQuery(query);
            try {
                if(c != null && c.getCount() > 0 && c.moveToFirst()) {
                    int groupIndex = c.getColumnIndex("groupC");
                    int subIndex = c.getColumnIndex("subid");
                    int nameIndex = c.getColumnIndex("name");
                    int valueIndex = c.getColumnIndex("value");
                    
                    GroupMap g = null;
                    while(!c.isAfterLast()) {
                        String group = c.getString(groupIndex);
                        if(g == null || !g.mGroup.equals(group)) {
                            g = getGroup(group);
                            if(g == null) {
                                g = addGroup(group);
                            }
                        }
                        if(g != null) {
                            String subid = c.getString(subIndex);
                            String name = c.getString(nameIndex);
                            String value = c.getString(valueIndex);
                            g.addMap(subid, name, value);
                        }
                        c.moveToNext();
                    }
                }
            }finally {
                if(c != null) {
                    c.close();
                }
                mDb.close();
            }
        }
    }
    
    private GroupMap getGroup(String group) {
        int N = mGroups.size();
        for(int i = 0; i < N; ++i) {
            GroupMap map = mGroups.get(i);
            if(map.mGroup.equals(group)) {
                return map;
            }
        }
        return null;
    }
    
    private GroupMap addGroup(String group) {
        GroupMap map = new GroupMap(group);
        mGroups.add(map);
        return map;
    }
    
    private static String getTable(int type) {
        if(type < TABLES.length) {
            return TABLES[type];
        }
        return null;
    }
    
    private static String createLoadQuery(int type, String[] groups) {
        String query = null;
        String table = getTable(type);
        if(table != null) {
            query = createLoadQuery(table, groups);
        }
        return query;
    }
    
    private static String createLoadQuery(String table, String[] groups) {
        String query = "SELECT g.map_value AS groupC, " +
                " sub.map_value AS subid, s.stat_name AS name, " +
                " s.stat_value AS value FROM " + table +
                " s JOIN idmap sub ON sub.id=s.sub_id " +
                " JOIN idmap g ON g.id=s.group_id ";
        final String ORDER = " ORDER BY g.map_value ";
        
        boolean doAll = false;
        StringBuilder join = new StringBuilder(" AND g.map_value IN (");
        for(int i = 0; i < groups.length; ++i) {
            if(groups[i].equals(Group.ALL)) {
                doAll = true;
                break;
            }else {
                if(i != 0) {
                    join.append(",");
                }
                join.append("'").append(groups[i]).append("'");
            }
        }
        if(doAll) {
            return query + ORDER;
        }else {
            join.append(")").append(ORDER).insert(0, query);
            return join.toString();
        }
    }
    
    private static class DbCache {
        final static String KEY = "key";
        final static String VALUE = "value";
        
        private HashMap<String, Long> mMap;
        private Transaction mTransaction;
        private String mSearch;
        private String[] mSearchArgs;
        
        DbCache(String loadQuery, String searchQuery, Transaction t) {
            mMap = new HashMap<String, Long>();
            mSearch = searchQuery;
            mTransaction = t;
            mSearchArgs = new String[1];
            load(loadQuery);
        }
        
        private void load(String query) {
            if(query != null) {
                Cursor c = mTransaction.doQuery(query, null);
                if(c != null) {
                    try {
                        if(c.getCount() > 0 && c.moveToFirst()) {
                            int keyIndex = c.getColumnIndex(KEY);
                            int valueIndex = c.getColumnIndex(VALUE);
                            if(keyIndex == -1 || valueIndex == -1) return;
                            while(!c.isAfterLast()) {
                                mMap.put(c.getString(keyIndex), c.getLong(valueIndex));
                                c.moveToNext();
                            }
                        }
                    }finally {
                        c.close();
                    }
                }
            }
        }
        
        public void insert(String key, long value) {
            mMap.put(key, value);
        }
        
        public long search(String key) {
            Long l = mMap.get(key);
            if(l == null) {
                mSearchArgs[0] = key;
                Cursor c = mTransaction.doQuery(mSearch, mSearchArgs);
                if(c != null) {
                    try {
                        if(c.getCount() > 0 && c.moveToFirst()) {
                            long value = c.getLong(c.getColumnIndex(VALUE));
                            mMap.put(key, value);
                            return value;
                        }
                    }finally {
                        c.close();
                    }
                }
                return -1;
            }else {
                return l;
            }
        }
    }
    
    private long getId(String key, DbCache cache, Transaction t) {
        // First try cache
        long l = cache.search(key);
        if(l == -1) {
            boolean done = t.runSql("INSERT INTO idmap (map_value) VALUES ('" + key + "')", null);
            // Now get back the id
            if(done) {
                Cursor c = t.doQuery("SELECT last_insert_rowid() as result", null);
                if(c != null) {
                    try {
                        if(c.getCount() > 0 && c.moveToFirst()) {
                            l = c.getLong(0);
                        }
                    }finally {
                        c.close();
                    }
                }
            }
        }
        if(l != -1) {
            cache.insert(key, l);
            return l;
        }else {
            throw new SQLException("Unable to get id for " + key);
        }       
    }
    
    public static StatsDatabase load(Context context, int type, String[] groups) {
        String query = createLoadQuery(type, groups);
        StatsDatabase dbase = new StatsDatabase(context, type);
        dbase.load(query);
        return dbase;
    }
    
    public static StatsDatabase loadEmpty(Context context, int type) {
        StatsDatabase dbase = new StatsDatabase(context, type);
        return dbase;
    }

    public void cleanup() {
        // After this call we cannot use this object anymore
        mDb = null;
        DatabaseIface.putInstance();
    }
    
    public void commit(int type, int commitLevel) {
        final String loadQ = "SELECT map_value as " + DbCache.KEY + "" +
                ", id as " + DbCache.VALUE + " FROM idmap LIMIT 30";
        final String searchQ = "SELECT id as " + DbCache.VALUE +
                " FROM idmap WHERE map_value=? "; 
        
        String table = getTable(type);
        Transaction t = mDb.getTransaction();
        t.startTransaction();
        try {
            if(table != null) {
                final String insertQ = "INSERT OR REPLACE INTO " + table +
                        " (group_id, sub_id, stat_name, stat_value) VALUES (?,?,?,?)";
                Object[] insertArgs = new Object[4];
                
                int N = mGroups.size();
                String deleteQ = null;
                DbCache cache = new DbCache(loadQ, searchQ, t);

                for(int i = 0; i < N; ++i) {
                    GroupMap map = mGroups.get(i);
                    String group = map.mGroup;
                    
                    long gid = getId(group, cache, t);
                    if(commitLevel == CommitLevel.GROUP) {
                        deleteQ = "DELETE FROM " + table + " WHERE group_id = " + gid;
                        t.runSql(deleteQ, null);
                    }
                    
                    Iterator<Map.Entry<String, HashMap<String, String>>> iterator =
                            map.mValueMap.entrySet().iterator();
                    while(iterator.hasNext()) {
                        Map.Entry<String, HashMap<String, String>> entry = iterator.next();
                        String subid = entry.getKey();
                        long sid = getId(subid, cache, t);

                        if(commitLevel == CommitLevel.SUBID) {
                            deleteQ = "DELETE FROM " + table + " WHERE group_id = " + gid + 
                                    " AND sub_id = " + sid;
                            t.runSql(deleteQ, null);
                        }
                        
                        Iterator<Map.Entry<String, String>> kviterator =
                                entry.getValue().entrySet().iterator();
                        while(kviterator.hasNext()) {
                            Map.Entry<String, String> kvpair = kviterator.next();
                            String key = kvpair.getKey();
                            String value = kvpair.getValue();
                            
                            insertArgs[0] = Long.valueOf(gid);
                            insertArgs[1] = Long.valueOf(sid);
                            insertArgs[2] = key;
                            insertArgs[3] = value;
                            t.runSql(insertQ, insertArgs);
                        }
                    }
                }
            }
            t.markTransaction(true);
        }catch(SQLException sqlEx) {
            // If we ever get here we are in big trouble
            Log.v(TAG, "commit encountered ex", sqlEx);
        }finally {
            t.stopTransaction();
        }
    }
    
    public int getGroupSize(String group) {
        GroupMap map = getGroup(group);
        if(map != null) {
            return map.mValueMap.size();
        }
        return 0;
    }
    
    public Iterator<String> getGroupSubIds(String group) {
        GroupMap map = getGroup(group);
        if(map != null) {
            return map.mValueMap.keySet().iterator();
        }
        return null;
    }
    
    public HashMap<String, String> getSubIdPairs(String group, String subid) {
        GroupMap map = getGroup(group);
        if(map != null) {
            return map.mValueMap.get(subid);
        }
        return null;
    }
    
    public String getValue(String group, String subid, String name) {
        GroupMap map = getGroup(group);
        if(map != null) {
            HashMap<String, String> values = map.mValueMap.get(subid);
            if(values != null) {
                return values.get(name);
            }
        }
        return null;
    }
    
    public boolean putValue(String group, String subid, String name, String value) {
        GroupMap map = getGroup(group);
        if(map == null) {
            map = addGroup(group);
        }
        if(map != null) {
            return map.addMap(subid, name, value);
        }
        return false;
    }
    
    public void resetValues() {
        int N = mGroups.size();
        for(int i = 0; i < N; ++i) {
            GroupMap map = mGroups.get(i);
            map.mValueMap.clear();
        }
        mGroups.clear();
    }

    /* (non-Javadoc)
     * @see com.motorola.devicestatistics.DatabaseIface.IDbInitListener#onCreate(android.database.sqlite.SQLiteDatabase)
     */
    public void onCreate(SQLiteDatabase db) {
        for(int i = 0; i < TABLE_CREATE_Q.length; ++i) {
            db.execSQL(TABLE_CREATE_Q[i]);
        }
    }

    /* (non-Javadoc)
     * @see com.motorola.devicestatistics.DatabaseIface.IDbInitListener#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
     */
    public void onUpgrade(SQLiteDatabase db, int oldVer, int newVer) {
        for ( int i=0; i<TABLES.length; i++ ) {
            db.execSQL( "DROP TABLE IF EXISTS " + TABLES[i] );
        }
        onCreate( db );
    }

    public int getType() {
        return mType;
    }
}

