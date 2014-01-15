package com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;

import com.motorola.mmsp.socialGraph.R;

public class SkinsParser {
	public static void getSkins(Context context, ArrayList<Skin> skins, SkinViews views) {
	 
		XmlPullParser parser = context.getResources().getXml(R.xml.skins);
		try {
			while(parser.next() != XmlPullParser.END_DOCUMENT) {
				String name = parser.getName();
				if (name != null && name.equals("item")) {
					HashMap<String, Object> item = new HashMap<String, Object>();
					int count = parser.getAttributeCount();
					for (int i=0; i < count; i++) {
						String attrName = parser.getAttributeName(i);
						String attrValue = parser.getAttributeValue(i);
						if (attrName != null && attrValue != null) {
							if (attrValue.startsWith("@")) {
								item.put(attrName, Integer.parseInt(attrValue.replace("@", "")));
							} else {
								item.put(attrName, attrValue);
							}
						}
					}
					Object skinName = (Object)item.get(CommonColumn.NAME);
					if (skinName != null && !skinName.equals("")) {
						if (skinName.equals(SkinViews.ME)) {
							item.remove(CommonColumn.NAME);
							views.form(item);
						} else {
							Skin skin = new Skin(item);
							skins.add(skin);
						}						
					}
				}
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
