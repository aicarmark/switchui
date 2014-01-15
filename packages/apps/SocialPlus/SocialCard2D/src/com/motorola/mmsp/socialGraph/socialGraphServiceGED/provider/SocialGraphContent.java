package com.motorola.mmsp.socialGraph.socialGraphServiceGED.provider;

import android.net.Uri;

/**
 * SocialGraphContent is the superclass of the various classes of content stored
 * by SocialGraphProvider.
 * 
 * It is intended to include 1) column definitions for use with the Provider,
 * and 2) convenience methods for saving and retrieving content from the
 * Provider.
 * 
 * This class will be used by 1) the Social Graph process (which includes the
 * application and SocialGraphProvider) as well as 2) the listener process
 * (which runs independently). It will necessarily be cloned for use in these
 * two cases.
 * 
 * Conventions used in naming columns: RECORD_ID is the primary key for all
 * Email records
 * 
 * <name>_KEY always refers to a foreign key <name>_ID always refers to a unique
 * identifier (whether on client, server, etc.)
 * 
 */
public abstract class SocialGraphContent {
	public static final String AUTHORITY = SocialGraphProvider.SOCIALGRAPH_AUTHORITY;
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

	public static final Uri EXCUTE_SQL = Uri.parse(CONTENT_URI + "/excute_sql");
	public static final Uri SET_DAY_RANKING = Uri.parse(CONTENT_URI + "/set_ranking");
	public static final Uri SET_TOTAL_RANKING = Uri.parse(CONTENT_URI + "/set_total_ranking");
	
	
	// All classes share this
	public static final String RECORD_ID = "id";

	public static class ContactInfoColumns {
		// Basic columns used in contact info list presentation
		public static final String ID = RECORD_ID;
		// contact id in the contact
		public static final String PERSON = "person";
		// contact info
		public static final String INFO_URI = "display_name_photo_uri";
		//auto manual
		public static final String TYPE = "type";
		
		public static final int CONTENT_ID_COLUMN = 0;
		public static final int CONTENT_PERSON_COLUMN = 1;
		public static final int CONTENT_INFO_URI_COLUMN = 2;
		public static final int CONTENT_TYPE_COLUMN = 3;
		
	}

	public static final class ContactInfo {

		public static final String TABLE_NAME = "socialcontacts";
		public static final Uri CONTENT_URI = Uri
				.parse(SocialGraphContent.CONTENT_URI + "/socialcontacts");

		public static final String[] CONTENT_PROJECTION = new String[] {
				ContactInfoColumns.ID, 
				ContactInfoColumns.PERSON,
				ContactInfoColumns.INFO_URI, 
				ContactInfoColumns.TYPE,
				
		};
		
	}
	
	/*
	 * It is for each different phone number only
	 */
	public class FrequencyColumns {
		// Basic columns used in message list presentation
		public static final String ID = RECORD_ID;
		// The person of each phone number
		public static final String PERSON = "person";
		// The ranking of this person for that day.
		public static final String RANKING = "ranking";

		public static final int CONTENT_ID_COLUMN = 0;
		public static final int CONTENT_PERSON_COLUMN = 1;
		public static final int CONTENT_RANKING_COLUMN = 2;
	}

	public static final class Frequency {

		public static final String TABLE_NAME = "frequency";
		public static final Uri CONTENT_URI = Uri
				.parse(SocialGraphContent.CONTENT_URI + "/frequency");

		public static final String[] CONTENT_PROJECTION = new String[] {
				FrequencyColumns.ID, 
				FrequencyColumns.PERSON,
				FrequencyColumns.RANKING,
		};
	}
	
	public class MiscValueColumns {
		// Basic columns used in misc list presentation
		public static final String ID = RECORD_ID;
		
		public static final String MISC_KEY = "misc_key";
		public static final String MISC_VALUE = "misc_value";
		
		public static final int CONTENT_ID_COLUMN = 0;
		public static final int CONTENT_MISC_KEY_COLUMN = 1;
		public static final int CONTENT_MISC_VALUE_COLUMN = 2;
	}

	public static final class MiscValue {

		public static final String TABLE_NAME = "miscvalue";
		public static final Uri CONTENT_URI = Uri
				.parse(SocialGraphContent.CONTENT_URI + "/miscvalue");

		public static final String[] CONTENT_PROJECTION = new String[] {
			    MiscValueColumns.ID, 
			    MiscValueColumns.MISC_KEY,
			    MiscValueColumns.MISC_VALUE,
		};
	}

}
