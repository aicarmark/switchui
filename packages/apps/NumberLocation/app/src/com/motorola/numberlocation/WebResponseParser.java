package com.motorola.numberlocation;

import java.io.IOException;
import java.io.Reader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;
import android.util.TimeFormatException;

public class WebResponseParser {
	// xml key word
	private static final String TAG_PROBLEM_CAUSE = "problem_cause";

	private static final String TAG_NUMBER_LOCATION = "NumberLocation";
	private static final String TAG_STATUS = "Status";
	private static final String TAG_XML_PARSE_VERSION = "XMLParseVersion";
	private static final String TAG_DATABASE_VERSION = "DatabaseVersion";
	private static final String TAG_NUMBER_CHANGE = "NumberChange";
	private static final String TAG_DATA_MANAGE = "DataManage";
	private static final String TAG_ADD = "Add";
	private static final String TAG_DELETE = "Delete";
	private static final String TAG_UPDATE = "Update";
	private static final String TAG_COUNT = "Count";
	
	private static final String TAG_ITEMS = "Items";
	private static final String TAG_ITEM = "Item";

	private static final String TAG_NUMBER = "number";
	private static final String TAG_PROVINCE = "province";
	private static final String TAG_CITY = "city";
	private static final String TAG_COUNTRY = "country";
	
	private static final String TAG = "WebResponseParser";
	
	static NumberLocationUpdateData parseWebResponse(
			Reader responseReader) throws WebUpdateException {
		Log.d(TAG, ">> parseWebResponse");
		NumberLocationUpdateData UpdateData = null;

		try {
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			XmlPullParser parser = factory.newPullParser();
			parser.setInput(responseReader);

			String tagName = null;
			String startTagName = null;
			int eventType = parser.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					tagName = parser.getName();
					startTagName = tagName;

					if (TAG_PROBLEM_CAUSE.equals(tagName)) {
						throw new WebUpdateException("## invalid tag!");
					} else if (TAG_NUMBER_LOCATION.equals(tagName)) {
						UpdateData = new NumberLocationUpdateData();
						parseNumberLocationInfo(parser, tagName, UpdateData);
					}
				} else if (eventType == XmlPullParser.TEXT) {
				} else if (eventType == XmlPullParser.END_TAG) {
					tagName = parser.getName();
					 if (startTagName.equals(tagName)) {
					 break;
					 }
				}
				eventType = parser.next();
			}

		} catch (IOException e) {
			throw new WebUpdateException("## XML parse error!", e);
		} catch (XmlPullParserException e) {
			throw new WebUpdateException("## XML parse error!", e);
		} catch (TimeFormatException e) {
			throw new WebUpdateException("## XML parse error!", e);
		}
		return UpdateData;
	}

	private static void parseNumberLocationInfo(XmlPullParser parser,
			String startTagName, NumberLocationUpdateData UpdateData)
	throws IOException, XmlPullParserException, WebUpdateException {

		parser.next();

		String tagName = null;
		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (TAG_PROBLEM_CAUSE.equals(tagName)) {
					throw new WebUpdateException("## invalid tag!");
				} else if (TAG_NUMBER_CHANGE.equals(tagName)) {
					NumberChangeInfo ChangeInfo = new NumberChangeInfo();
					parseNumberChangeInfo(parser, tagName, ChangeInfo);
					UpdateData.setNumberChange(ChangeInfo);
				} else if (TAG_DATA_MANAGE.equals(tagName)) {
					DataManageInfo ManageInfo = new DataManageInfo();
					parseDataManageInfo(parser, tagName, ManageInfo);
					UpdateData.setDataManage(ManageInfo);
				}
			} else if (eventType == XmlPullParser.TEXT) {
				if (TAG_STATUS.equals(tagName)) {
					UpdateData.setStatus(Integer.parseInt(parser.getText()));
				} else if (TAG_XML_PARSE_VERSION.equals(tagName)) {
					UpdateData.setXMLParseVersion(parser.getText());
				} else if (TAG_DATABASE_VERSION.equals(tagName)) {
					UpdateData.setDatabaseVersion(parser.getText());
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (startTagName.equals(tagName)) {
					break;
				}
			}
			eventType = parser.next();
		}
	}

	private static void parseNumberChangeInfo(XmlPullParser parser,
			String startTagName, NumberChangeInfo info) throws IOException,
			XmlPullParserException, WebUpdateException {

		parser.next();

		String tagName = null;
		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (TAG_PROBLEM_CAUSE.equals(tagName)) {
					throw new WebUpdateException("## invalid tag!");
				} else if (TAG_ADD.equals(tagName)) {
					AddCollection add = new AddCollection();
					parseAddCollection(parser, tagName, add);
					info.setAdd(add);
				} else if (TAG_DELETE.equals(tagName)) {
					DeleteCollection delete = new DeleteCollection();
					parseDeleteCollection(parser, tagName, delete);
					info.setDelete(delete);
				} else if (TAG_UPDATE.equals(tagName)) {
					UpdateCollection update = new UpdateCollection();
					parseUpdateCollection(parser, tagName, update);
					info.setUpdate(update);
				}
			} else if (eventType == XmlPullParser.TEXT) {
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (startTagName.equals(tagName)) {
					break;
				}
			}
			eventType = parser.next();
		}
	}

	private static void parseDataManageInfo(XmlPullParser parser,
			String startTagName, DataManageInfo info) throws IOException,
			XmlPullParserException, WebUpdateException {

		parser.next();
		String tagName = null;
		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (TAG_PROBLEM_CAUSE.equals(tagName)) {
					throw new WebUpdateException("## invalid tag!");
				} else if (TAG_ADD.equals(tagName)) {
					AddCollection add = new AddCollection();
					parseAddCollection(parser, tagName, add);
					info.setAdd(add);
				} else if (TAG_DELETE.equals(tagName)) {
					DeleteCollection delete = new DeleteCollection();
					parseDeleteCollection(parser, tagName, delete);
					info.setDelete(delete);
				} else if (TAG_UPDATE.equals(tagName)) {
					UpdateCollection update = new UpdateCollection();
					parseUpdateCollection(parser, tagName, update);
					info.setUpdate(update);
				}
			} else if (eventType == XmlPullParser.TEXT) {
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (startTagName.equals(tagName)) {
					break;
				}
			}
			eventType = parser.next();
		}
	}

	private static void parseAddCollection(XmlPullParser parser,
			String startTagName, AddCollection add) throws IOException,
			XmlPullParserException, WebUpdateException {

		parser.next();

		String tagName = null;
		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (TAG_PROBLEM_CAUSE.equals(tagName)) {
					throw new WebUpdateException("## invalid tag!");
				} else if (TAG_ITEMS.equals(tagName)) {
					ItemsArray items = new ItemsArray();
					parseItems(parser, tagName, items);
					add.setItems(items);
				}
			} else if (eventType == XmlPullParser.TEXT) {
				if (TAG_COUNT.equals(tagName)) {
					add.setCount(Integer.parseInt(parser.getText()));
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (startTagName.equals(tagName)) {
					break;
				}
			}
			eventType = parser.next();
		}
	}

	private static void parseDeleteCollection(XmlPullParser parser,
			String startTagName, DeleteCollection delete) throws IOException,
			XmlPullParserException, WebUpdateException {

		parser.next();

		String tagName = null;
		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (TAG_PROBLEM_CAUSE.equals(tagName)) {
					throw new WebUpdateException("## invalid tag!");
				} else if (TAG_ITEMS.equals(tagName)) {
					ItemsArray items = new ItemsArray();
					parseItems(parser, tagName, items);
					delete.setItems(items);
				}
			} else if (eventType == XmlPullParser.TEXT) {
				if (TAG_COUNT.equals(tagName)) {
					delete.setCount(Integer.parseInt(parser.getText()));
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (startTagName.equals(tagName)) {
					break;
				}
			}
			eventType = parser.next();
		}
	}

	private static void parseUpdateCollection(XmlPullParser parser,
			String startTagName, UpdateCollection update) throws IOException,
			XmlPullParserException, WebUpdateException {

		parser.next();

		String tagName = null;
		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (TAG_PROBLEM_CAUSE.equals(tagName)) {
					throw new WebUpdateException("## invalid tag!");
				} else if (TAG_ITEMS.equals(tagName)) {
					ItemsArray items = new ItemsArray();
					parseItems(parser, tagName, items);
					update.setItems(items);
				}
			} else if (eventType == XmlPullParser.TEXT) {
				if (TAG_COUNT.equals(tagName)) {
					update.setCount(Integer.parseInt(parser.getText()));
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (startTagName.equals(tagName)) {
					break;
				}
			}
			eventType = parser.next();
		}
	}

	private static void parseItems(XmlPullParser parser,
			String startTagName, ItemsArray items) throws IOException,
			XmlPullParserException, WebUpdateException {

		parser.next();

		String tagName = null;
		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
				if (TAG_PROBLEM_CAUSE.equals(tagName)) {
					throw new WebUpdateException("## invalid tag!");
				} else if (TAG_ITEM.equals(tagName)) {
					ItemInfo item = new ItemInfo();
					parseItem(parser, tagName, item);
					items.getItem().add(item);
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (startTagName.equals(tagName)) {
					break;
				}
			}
			eventType = parser.next();
		}
	}

	private static void parseItem(XmlPullParser parser,
			String startTagName, ItemInfo item) throws IOException,
			XmlPullParserException {

		parser.next();

		String tagName = null;
		int eventType = parser.getEventType();

		while (eventType != XmlPullParser.END_DOCUMENT) {

			if (eventType == XmlPullParser.START_TAG) {
				tagName = parser.getName();
			} else if (eventType == XmlPullParser.TEXT) {
				if (TAG_NUMBER.equals(tagName)) {
					item.setNumber(parser.getText());
				}else if (TAG_PROVINCE.equals(tagName)) {
					item.setProvince(parser.getText());
				}else if (TAG_CITY.equals(tagName)) {
					item.setCity(parser.getText());
				}
				else if (TAG_COUNTRY.equals(tagName)) {
					item.setCountry(parser.getText());
				}
			} else if (eventType == XmlPullParser.END_TAG) {
				tagName = parser.getName();
				if (startTagName.equals(tagName)) {
					break;
				}
			}
			eventType = parser.next();
		}
	}	
}
