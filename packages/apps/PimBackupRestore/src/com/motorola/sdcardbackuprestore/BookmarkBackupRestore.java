package com.motorola.sdcardbackuprestore;

import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.motorola.sdcardbackuprestore.BookmarkConstants.HtmlTag;

import android.R.integer;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.BrowserContract.Bookmarks;
import android.util.Log;
import android.widget.Space;

public class BookmarkBackupRestore {
    private final String TAG = "BookmarkBackupRestore";
    private Context mContext = null;
    private Cursor mFolderCursor = null;
    private HashMap<String, Node> mBookmarkHashMap = new HashMap<String, Node>();
    private ArrayList<Node> mFolderNodeList = new ArrayList<Node>();
    private ArrayList<Node> mBookmarkNodeList = new ArrayList<Node>();
    private BookmarkTree mBookmarkTree = new BookmarkTree();
    private Node mRoot = null;
    private HashSet<String> mDefaultBookmarks = new HashSet<String>();
    private HashSet<String> mExistBookmarks = new HashSet<String>();
    private int mNeednotRestoreDefaultBookmarkNum = 0;
    
    public class Bookmark {
        private int id;
        private int version;
        private String url;
        private long modified;
        private long created;
        private String title;
        private int folder;
        private int parent;
        private String account_name;
        private String account_type;
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            return sb.append("id = ").append(id)
                    .append("; title = ").append(title)
                    .append("; folder = ").append(folder)
                    .append("; parent = ").append(parent)
                    .append("; url = ").append(url)
                    .append("; version = ").append(version)
                    .append("; modified = ").append(modified)
                    .append("; created = ").append(created).toString();
        }
        
    }
    
    public class Node {
        boolean isActive = false;
        Bookmark bm = null;
        ArrayList<Node> bookmarkList = null;
        ArrayList<Node> folderList = null;
    }
    
    public class BookmarkTree {
        
        private Node root;
        private HashMap<Integer, Node> nodeMap = new HashMap<Integer, Node>();
        
        BookmarkTree() {
            root = new Node();
            root.bm = new Bookmark();
            root.bm.title = "Bookmarks";
            root.bm.folder = 1;
            root.bm.id = 1;
            nodeMap.put(root.bm.id, root);
        }
        
        public Node getRoot() {
            return root;
        }
        
        public Node getNode(Bookmark bookmark) {
            Node node = new Node();
            node.bm = bookmark;
            return node;
        }
        
        public Node getNodeById(int id) {
            return nodeMap.get(id);
        }
        
        public Node getNode(Cursor cursor) {
            if (cursor == null) {
                return null;
            } else {
                int index_column = -1;
                Bookmark bm = new Bookmark();
                index_column = cursor.getColumnIndex(BookmarkConstants.column_id);
                bm.id = (index_column == -1 ? 0 : cursor.getInt(index_column));
                index_column = cursor.getColumnIndex(BookmarkConstants.column_version);
                bm.version = (index_column == -1 ? 0 : cursor.getInt(index_column));
                index_column = cursor.getColumnIndex(BookmarkConstants.column_url);
                bm.url = index_column == -1 ? "" : cursor.getString(index_column);
                index_column = cursor.getColumnIndex(BookmarkConstants.column_modified);
                bm.modified = index_column == -1 ? 0 : cursor.getLong(index_column);
                index_column = cursor.getColumnIndex(BookmarkConstants.column_created);
                bm.created = index_column == -1 ? 0 : cursor.getLong(index_column);
                index_column = cursor.getColumnIndex(BookmarkConstants.column_title);
                bm.title = index_column == -1 ? "" : cursor.getString(index_column);
                index_column = cursor.getColumnIndex(BookmarkConstants.column_folder);
                bm.folder = index_column == -1 ? 0 : cursor.getInt(index_column);
                index_column = cursor.getColumnIndex(BookmarkConstants.column_parent);
                bm.parent = index_column == -1 ? 0 : cursor.getInt(index_column);
                index_column = cursor.getColumnIndex(BookmarkConstants.column_account_name);
                bm.account_name = index_column == -1 ? "" : cursor.getString(index_column);
                index_column = cursor.getColumnIndex(BookmarkConstants.column_account_type);
                bm.account_type = index_column == -1 ? "" : cursor.getString(index_column);
                Node node = new Node();
                node.bm = bm;
                return node;
            }
        }
        
        private Node buildTree(ArrayList<Node> nodeList) {
            for (int i = 0; i < nodeList.size(); i++) {
                addNode(nodeList.get(i));
            }
            return root;
        }
        
        private void addNode(Node node) {
            if (node == null) {
                return;
            }
            node.isActive = true;
            boolean exist = false;
            Node tmpNode = nodeMap.get(node.bm.id);
            if (tmpNode != null) {
                exist = true;
                if (tmpNode.isActive) {
                    return;
                }
                tmpNode.isActive = node.isActive;
                tmpNode.bm = node.bm;
                tmpNode.bookmarkList = node.bookmarkList;
                tmpNode.folderList = node.folderList;
            } else {
                nodeMap.put(node.bm.id, node);
            }
            Node parent = nodeMap.get(node.bm.parent);
            if (parent == null) {
                parent = new Node();
            }
            if (parent.bm == null) {
                parent.bm = new Bookmark();
            }
            parent.bm.id = node.bm.parent;
            nodeMap.put(parent.bm.id, parent);
            if (node.bm.folder == 1) {
                if (parent.folderList == null) {
                    parent.folderList = new ArrayList<Node>();
                }
                if (!exist) {
                    parent.folderList.add(node);
                }
            } else {
                if (parent.bookmarkList == null) {
                    parent.bookmarkList = new ArrayList<Node>();
                }
                parent.bookmarkList.add(node);
            }
        }
        
        private void updateNodeId(int oldId, int newId) {
            Node node = nodeMap.get(oldId);
            if (node == null) return;
            node.bm.id = newId;
        }
        
    }    // Class BookmarkTree End
    
    int getNeednotRestoreDefaultBookmarksNum() {
        return mNeednotRestoreDefaultBookmarkNum;
    }
    
    public BookmarkBackupRestore(Context context) {
         mContext = context;
     }
    
    public int queryNumOfBookmarkWithoutFolder() {
        String selection =  BookmarkConstants.column_folder + " = '0' AND " + BookmarkConstants.column_deleted + " = '0'";
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Bookmarks.CONTENT_URI, new String[]{BookmarkConstants.column_id}, selection, null, null);
            return cursor.getCount();
        } catch (Exception e) {
            // TODO: handle exception
            return 0;
        } finally {
            if (null != cursor) {
                cursor.close();
                cursor = null;
            }
        }
    }
    
    public static Cursor queryBookmarkAndFolder(Context context) {
        Cursor cursor = context.getContentResolver().query(Bookmarks.CONTENT_URI, 
                new String[] { BookmarkConstants.column_id, BookmarkConstants.column_version,
                BookmarkConstants.column_modified, BookmarkConstants.column_url,
                BookmarkConstants.column_created, BookmarkConstants.column_title, 
                BookmarkConstants.column_folder, BookmarkConstants.column_parent,
                BookmarkConstants.column_account_name, BookmarkConstants.column_account_type} , 
                BookmarkConstants.column_deleted + " = ? AND " + BookmarkConstants.column_parent + " <> ?", new String[] {"0", "null"}, null);
        return cursor;
    }
    
    public Cursor queryFolder() {
        Cursor cursor = mContext.getContentResolver().query(Bookmarks.CONTENT_URI, 
                new String[] { BookmarkConstants.column_id,
                BookmarkConstants.column_title, BookmarkConstants.column_parent, 
                BookmarkConstants.column_account_name, BookmarkConstants.column_account_type} , 
                BookmarkConstants.column_deleted + " = ?" 
                    + " AND " + BookmarkConstants.column_parent + " <> ?"
                    + " AND " + BookmarkConstants.column_folder + " = ?", new String[] {"0", "null", "1"}, null);
        return cursor;
    }
    
    public void Backup(Cursor cursor, StringBuffer subject) {
        Log.d(TAG, "Backup()");
        int index_column = -1;
        if (cursor != null) {
            Node node = mBookmarkTree.getNode(cursor);
            subject.delete(0, subject.length()).append(node.bm.title);
            mBookmarkTree.addNode(node);
        }
    }
    
    public void initAllFolder(ArrayList<Node> folderNodeList) {
        mRoot = mBookmarkTree.buildTree(folderNodeList);
    }

    
    public ArrayList<Node> getAllFolder(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        cursor.moveToFirst();
        ArrayList<Node> folderNodeList = new ArrayList<Node>();
        Node tmpNode;
        while (!cursor.isAfterLast()) {
            tmpNode = mBookmarkTree.getNode(cursor);
            if (tmpNode.bm.folder == 1) {
                folderNodeList.add(tmpNode);
            }
            cursor.moveToNext();
        }
        return folderNodeList;
    }
    
    public void initBookMarkTree(String file) {
        ArrayList<Node> nodeList = createNodesFromFile(file);
        for (Node node : nodeList) {
            mBookmarkTree.addNode(node);
        }
        mRoot = mBookmarkTree.getRoot();
    }
    
    public String Restore(Context context, ContentValues cv) {
        String subject = null;
        if (cv != null) {
            subject = context.getString(R.string.bookmark);
        }
        mContext.getContentResolver().insert(Bookmarks.CONTENT_URI, cv);
        return subject;
    }
    
    private ContentValues getNodeCV(Node node) {
        if (node == null) {
            return null;
        }
        ContentValues cv = new ContentValues();
        int newParentId = 1;
        String tmpString;
        Node parentNode = mBookmarkTree.nodeMap.get(node.bm.parent);
        if (parentNode != null) {
            newParentId = parentNode.bm.id;
        }
        cv.put(BookmarkConstants.column_version, node.bm.version);
        cv.put(BookmarkConstants.column_folder, node.bm.folder);
        cv.put(BookmarkConstants.column_parent, newParentId);
        cv.put(BookmarkConstants.column_created, node.bm.created);
        cv.put(BookmarkConstants.column_modified, node.bm.modified);
        tmpString = node.bm.url;
        if (tmpString != null) {
            cv.put(BookmarkConstants.column_url, tmpString);
        }
        tmpString = node.bm.title;
        if (tmpString != null) {
            cv.put(BookmarkConstants.column_title, tmpString);
        }
        tmpString = node.bm.account_name;
        if (tmpString != null) {
            cv.put(BookmarkConstants.column_account_name, tmpString);
        }
        tmpString = node.bm.account_type;
        if (tmpString != null) {
            cv.put(BookmarkConstants.column_account_type, tmpString);
        }
        return cv;
    }
    
    public ArrayList<ContentValues> getBookmarkValues(boolean isFolder) {
        createExistBookmarkHashMap();
        createDefaultBookmarksHashMap();
        ArrayList<ContentValues> cvList = new ArrayList<ContentValues>();
        ContentValues cv = null;
        if (isFolder) {
            for (Node node : mFolderNodeList) {
                cv = getNodeCV(node);
                cvList.add(cv);
            }
        } else {
            for (Node node : mBookmarkNodeList) {
                cv = getNodeCV(node);
                if (isDefaultBookmark(cv) && isExistBookmark(cv)) {
                    mNeednotRestoreDefaultBookmarkNum++;
                } else {
                    cvList.add(cv);
                }
            }
        }
        return cvList;
    }
    
    private void createExistBookmarkHashMap() {
        Cursor cursor = queryBookmarkAndFolder(mContext);
        if (cursor == null) {
            return;
        }
        String url = null;
        int folder, col_folder, col_url;
        while (cursor.moveToNext()) {
            col_folder = cursor.getColumnIndex(BookmarkConstants.column_folder);
            if (col_folder != -1) {
                folder = cursor.getInt(col_folder);
                if (folder == 0) {
                    col_url = cursor.getColumnIndex(BookmarkConstants.column_url);
                    if (col_url != -1) {
                        url = cursor.getString(col_url);                                                
                                                if (null == url) {
                                                    continue;
                                                }
                        mExistBookmarks.add(ignoreLastSlash(url));
                    }
                }
            }
        }
    }
    
    private void createDefaultBookmarksHashMap() {
        Resources res = mContext.getResources();
        String[] defaultBookmarks = null;
        if(SystemProperties.get("ro.product.locale.region","CN").equalsIgnoreCase("TW")) {
            defaultBookmarks = res.getStringArray(R.array.default_bookmarks_for_HK_TWN);
        } else {
            defaultBookmarks = res.getStringArray(R.array.default_bookmarks);
        }
        if (defaultBookmarks == null) {
            return;
        }
        for (int i = 0; i < defaultBookmarks.length; i++) {
            mDefaultBookmarks.add(ignoreLastSlash(defaultBookmarks[i].trim()));
        }
    }
    
    private boolean isDefaultBookmark(ContentValues bookmark) {
        return mDefaultBookmarks.contains(ignoreLastSlash(bookmark.getAsString(BookmarkConstants.column_url).trim()));
    }
    
    private boolean isExistBookmark(ContentValues bookmark) {
        return mExistBookmarks.contains(ignoreLastSlash(bookmark.getAsString(BookmarkConstants.column_url)));
    }
    
    private String ignoreLastSlash(String url) {
        if(url.endsWith("/")) {
            return url.substring(0, url.length()-1);
        }
        return url;
    }
    
    /*
     * this queue don't contains the root node.
     */
    private LinkedList<Node> getFolderNodeQueue(Node root) {
        if (root == null) {
            return null;
        }
        LinkedList<Node> nodeQueue = new LinkedList<Node>();
        buildQueue(nodeQueue, root);
        if (nodeQueue.contains(root)) {
            nodeQueue.remove(root);
        }
        return nodeQueue;
    }
    
    private void buildQueue(LinkedList<Node> nodeQueue, Node node) {
        if (node == null) {
            return;
        }
        nodeQueue.add(node);
        if (node.folderList == null) {
            return;
        }
        ArrayList<Node> nodeList = node.folderList;
        for (int i = 0; i < nodeList.size(); i++) {
            buildQueue(nodeQueue, nodeList.get(i));
        }
    }
    
    public void insertFolder() {
        Log.d(TAG, "Enter insertFolder()");
        LinkedList<Node> nodeQueue = getFolderNodeQueue(mRoot);
        if (nodeQueue == null || (mFolderCursor = queryFolder()) == null) {
            return;
        }
        boolean needInsert;
        ContentValues cv = null;
        Uri uri = null;
        Node tmpNode;
        int oldId, newId;
        Iterator<Node> iter = nodeQueue.iterator();
        while (iter.hasNext()) {
            tmpNode = iter.next();
            iter.remove();
            cv = getNodeCV(tmpNode);
            Log.d("Node", tmpNode.bm.toString());
            Log.d("Content Values", cv.toString());
            needInsert = true;
            mFolderCursor.moveToFirst();
            while (!mFolderCursor.isAfterLast()) {
                Log.d("mFolderCursor", 
                        "parent = " + mFolderCursor.getInt(mFolderCursor.getColumnIndex(BookmarkConstants.column_parent))
                         + " title = "  + mFolderCursor.getString(mFolderCursor.getColumnIndex(BookmarkConstants.column_title)));
                if (cv.getAsInteger(BookmarkConstants.column_parent) != (mFolderCursor.getInt(mFolderCursor.getColumnIndex(BookmarkConstants.column_parent)))) {
                    Log.d("Compare Result ", "parent don't Equal !!! ");
                    mFolderCursor.moveToNext();
                    continue;
                }
                if (!cv.getAsString(BookmarkConstants.column_title).equals(mFolderCursor.getString(mFolderCursor.getColumnIndex(BookmarkConstants.column_title)))) {
                    Log.d("Compare Result ", "title don't Equal !!! ");
                    mFolderCursor.moveToNext();
                    continue;
                }
                if (!stringEquals(cv.getAsString(BookmarkConstants.column_account_name), mFolderCursor.getString(mFolderCursor.getColumnIndex(BookmarkConstants.column_account_name)))) {
                    mFolderCursor.moveToNext();
                    continue;
                }
                if (!stringEquals(cv.getAsString(BookmarkConstants.column_account_type), mFolderCursor.getString(mFolderCursor.getColumnIndex(BookmarkConstants.column_account_type)))) {
                    mFolderCursor.moveToNext();
                    continue;
                }
                Log.d("insertFolder()", "The folder is existed!");
                needInsert = false;
                mFolderCursor.moveToNext();
                break;
            }
            if (needInsert) {
                oldId = tmpNode.bm.id;
                cv.remove(BookmarkConstants.column_id);
                uri = mContext.getContentResolver().insert(Bookmarks.CONTENT_URI, cv);
                newId = (int)ContentUris.parseId(uri);
                mBookmarkTree.updateNodeId(oldId, newId);
            }
        }
    }
    
    private boolean stringEquals(String str1, String str2) {
        if (str1 != null && str1.equals("null")) {
            str1 = null;
        }
        if (str2 != null && str2.equals("null")) {
            str2 = null;
        }
        if (str1 == null && str2 == null) {
            return true;
        } else if (str1 != null && str2 != null) {
            return str1.equals(str2);
        } else {
            return false;
        }        
    }
    
    public boolean isBookmarkFolder(ContentValues cv) {
        int folder = cv.getAsInteger(BookmarkConstants.column_folder);
        return folder == 1 ? true : false;
    }
    
    public String buildBackupString() {
        if (mRoot == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(BookmarkConstants.BOOKMARK_FILE_TITLE).append(BookmarkConstants.CR)
            .append(getTag(BookmarkConstants.HtmlTag.H1, true)).append(BookmarkConstants.BM).append(getTag(BookmarkConstants.HtmlTag.H1, false)).append(BookmarkConstants.CR)
            .append(BookmarkConstants.BOOKMARK_HEADER).append(BookmarkConstants.CR)
            .append(buildTreeNodeString(mRoot));
        return sb.toString();
    }
    
    private String buildTreeNodeString(Node node) {
        if (node == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (node.bm.folder == 1) {
            if (node.bm.id != mRoot.bm.id) {
                sb.append(wrapTagWithoutValues(wrapTagWithoutValues(buildStringFromBookmark(BookmarkConstants.HtmlTag.H3, node.bm)
                                                                        , BookmarkConstants.HtmlTag.DT
                                                                        , false)
                                                    ,BookmarkConstants.HtmlTag.P,
                                                    false
                                                    ))
                    .append(BookmarkConstants.CR);
            }
            ArrayList<Node> folderList = node.folderList;
            ArrayList<Node> bookmarkList = node.bookmarkList;
            sb.append(getTag(BookmarkConstants.HtmlTag.DL, true)).append(BookmarkConstants.CR);
            if (folderList != null) {
                for (int i = 0; i < folderList.size(); i++) {
                    sb.append(buildTreeNodeString(folderList.get(i)));
                }
            } 
            if (bookmarkList != null) {
                sb.append(buildBookmarkListString(bookmarkList));
            }
            sb.append(getTag(BookmarkConstants.HtmlTag.DL, false)).append(BookmarkConstants.CR);
        }
        return sb.toString();
    }
    
    private String buildBookmarkListString(ArrayList<Node> bookmarkList) {
        StringBuilder sb = new StringBuilder();
        sb.append("<" + BookmarkConstants.HtmlTag.P + ">").append(BookmarkConstants.CR);
        for (int i = 0; i < bookmarkList.size(); i++) {
            if (bookmarkList.get(i).bm.folder != 0) {
                continue;
            }
            sb.append(wrapTagWithoutValues(buildStringFromBookmark(BookmarkConstants.HtmlTag.A, bookmarkList.get(i).bm), BookmarkConstants.HtmlTag.DT, false)).append(BookmarkConstants.CR);
        }
        return sb.toString();
    }
    
    private HashMap<String, String> convertClassToMap(Bookmark obj) {
        if (obj == null) {
            return null;
        }
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put(BookmarkConstants.href, obj.url);
        hm.put(BookmarkConstants.column_id, Integer.toString(obj.id));
        hm.put(BookmarkConstants.column_modified, Long.toString(obj.modified));
        hm.put(BookmarkConstants.column_created, Long.toString(obj.created));
        hm.put(BookmarkConstants.column_folder, Integer.toString(obj.folder));
        hm.put(BookmarkConstants.column_parent, Integer.toString(obj.parent));
        hm.put(BookmarkConstants.column_account_name, obj.account_name);
        hm.put(BookmarkConstants.column_account_type, obj.account_type);
        return hm;
    }
    
    private String wrapTagWithoutValues(String pureData, String tag, boolean withEndSymbol) {
        if (tag == null || tag.equals("") || pureData == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(tag).append('>').append(pureData);
        if (withEndSymbol) {
            sb.append("</").append(tag).append('>').append(BookmarkConstants.CR);
        }
        return sb.toString();
    }
    
    private String buildStringFromBookmark(String tag, Bookmark bm) {
        if (bm == null || tag == null || tag.equals("")) {
            return "";
        }
        StringBuilder serilize = new StringBuilder();
        String key = null;
        String value = null;
        serilize.append("<").append(tag);
        HashMap<String, String> data = convertClassToMap(bm);
        if (data != null) {
            Iterator iter = data.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, String> entry = (Map.Entry<String, String>)iter.next();
                key = entry.getKey();
                if (key.equals(BookmarkConstants.column_title)) {
                    continue;
                }
                value = entry.getValue();
                if (value == null) {
                    value = "";
                }
                serilize.append(" ").append(key).append("=").append("\"").append(value).append("\"");
            }
        }
        return serilize.append(">").append(bm.title).append("<").append("/").append(tag).append(">").toString();
    }
    
    private ArrayList<Node> createNodesFromFile(String fileString) {
        Log.d(TAG, "Enter createNodesFromFile()");
        ArrayList<Node> nodeList = new ArrayList<Node>();
        String tag_td = getTag(BookmarkConstants.HtmlTag.DT, true);
        int index_dt = fileString.indexOf(tag_td);
        int tag_td_length = tag_td.length();
        int index_pre_bracket = -1;
        int index_rear_tag = -1;
        String tag = null;
        Node node;
        while (index_dt != -1) {
            index_pre_bracket = fileString.indexOf("<", index_dt);
            if (index_pre_bracket == -1) break;
            if (fileString.substring(index_pre_bracket + tag_td_length + 1, index_pre_bracket + tag_td_length + 3).equals(BookmarkConstants.HtmlTag.H3)) {
                tag = BookmarkConstants.HtmlTag.H3;
            } else if (fileString.substring(index_pre_bracket + tag_td_length + 1, index_pre_bracket + tag_td_length + 2).equals(BookmarkConstants.HtmlTag.A)) {
                tag = BookmarkConstants.HtmlTag.A;
            } else {
                break;
            }
            index_rear_tag = fileString.indexOf(getTag(tag, false), index_dt);
            if (index_rear_tag == -1) break;
            node = mBookmarkTree.getNode(getTagValues(fileString.substring(index_pre_bracket, index_rear_tag + getTag(tag, false).length()), tag));
            nodeList.add(node);
            if (node.bm.folder == 1) {
                mFolderNodeList.add(node);
            } else {
                mBookmarkNodeList.add(node);
            }
            index_dt = fileString.indexOf(getTag(BookmarkConstants.HtmlTag.DT, true), index_rear_tag);
        }
        return nodeList;
    }
    
    /*
     * get the params in the tag and title between tags, such as <tag param1="value1" param2="value2">title</tag>
     */
    private Bookmark getTagValues(String packageString, String tag) {
        HashMap<String, String> values = new HashMap<String, String>();
        if (packageString == null) {
            return null;
        }
        int index_tag_pre_bracket = packageString.indexOf("<" + tag);
        String wrappedValues = packageString.substring(index_tag_pre_bracket + ("<" + tag).length()
                                                     , packageString.indexOf(">", index_tag_pre_bracket)).trim();
        String[] params = wrappedValues.split(" ");
        String key = null;
        String value = null;
        int pre_index_mark;
        int rear_index_mark;
        for (int i = 0; i < params.length; i++) {
            pre_index_mark = params[i].indexOf("\"");
            if (pre_index_mark == -1) continue;
            rear_index_mark = params[i].indexOf("\"", pre_index_mark + 1);
            if (rear_index_mark == -1) continue;
            key = params[i].substring(0, params[i].indexOf("="));
            if (key.equals(BookmarkConstants.href)) {
                key = BookmarkConstants.column_url;
            }
            value = params[i].substring(pre_index_mark + 1, rear_index_mark);
            if (value.equals("")) {
                value = null;
            }
            values.put(key, value);
        }
        pre_index_mark = packageString.indexOf(">", packageString.indexOf("<" + tag));
        rear_index_mark = packageString.indexOf("</" + tag, pre_index_mark);
        values.put(BookmarkConstants.column_title, packageString.substring(pre_index_mark + 1, rear_index_mark));
        return setBookmark(values);
    }
    
    private String getTag(String tag, boolean isPreSymbol) {
        if (isPreSymbol) {
            return "<" + tag + ">";
        } else {
            return "</" + tag + ">";
        }
    }
    
    private Bookmark setBookmark(HashMap<String, String> values) {
        Bookmark bm = new Bookmark();
        Object v;
        String key, value;
        Iterator iter = values.entrySet().iterator();
        Map.Entry<String, String> entry;
        while (iter.hasNext()) {
            entry = (Map.Entry<String, String>)iter.next();
            key = entry.getKey();
            value = entry.getValue();
            if (key.equals(BookmarkConstants.column_id)) {
                bm.id = Integer.parseInt(value);
            } else if (key.equals(BookmarkConstants.column_version)) {
                bm.version = Integer.parseInt(value);
            } else if (key.equals(BookmarkConstants.column_folder)) {
                bm.folder = Integer.parseInt(value);
            } else if (key.equals(BookmarkConstants.column_parent)) {
                bm.parent = Integer.parseInt(value);
            } else if (key.equals(BookmarkConstants.column_created)) {
                bm.created = Long.parseLong(value);
            } else if (key.equals(BookmarkConstants.column_modified)) {
                bm.modified = Long.parseLong(value);
            } else if (key.equals(BookmarkConstants.column_url)) {
                bm.url = value;
            } else if (key.equals(BookmarkConstants.column_title)) {
                bm.title = value;
            } else if (key.equals(BookmarkConstants.column_account_name)) {
                bm.account_name = value;
            } else if (key.equals(BookmarkConstants.column_account_type)) {
                bm.account_type = value;
            }
        }
        return bm;
    }
    
    public void test() {
        
    }
    
}
