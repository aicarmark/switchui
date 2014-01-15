package com.motorola.sdcardbackuprestore;

import android.provider.Browser.BookmarkColumns;
import android.provider.BrowserContract.Bookmarks;

public class BookmarkConstants {
	
	public static final class HtmlTag {
		public final static String TITLE = "TITLE";
		public final static String H1 = "H1";
		public final static String H3 = "H3";
		public final static String DL = "DL";
		public final static String DT = "DT";
		public final static String P = "p";
		public final static String A = "A";
	}
	
	public final static String BOOKMARK_HEADER = "<META CONTENT=\"text/html; charset=UTF-8\">";
	public final static String BOOKMARK_FILE_TITLE = "<TITLE>Bookmarks</TITLE>";
	public final static String column_version = Bookmarks.VERSION;
	public final static String column_id = Bookmarks._ID;
	public final static String column_url = Bookmarks.URL;
	public final static String column_created = Bookmarks.DATE_CREATED;
	public final static String column_modified = Bookmarks.DATE_MODIFIED;
	public final static String column_title = Bookmarks.TITLE;
	public final static String column_folder = "folder";
	public final static String column_parent =  Bookmarks.PARENT;
	public final static String column_account_name = Bookmarks.ACCOUNT_NAME;
	public final static String column_account_type = Bookmarks.ACCOUNT_TYPE;
	public final static String column_deleted = Bookmarks.IS_DELETED;
	public final static String CR = "\r\n";
	public final static String TAB = "\t";
	public final static String BM = "Bookmarks";

	public final static String BEGIN_MARK_BOOKMARK = "<" + HtmlTag.DT + ">" + "<" + HtmlTag.A;
	public final static String END_MARK_BOOKMARK = "</" + HtmlTag.A + ">";
	
	public final static String href = "href";
	
}