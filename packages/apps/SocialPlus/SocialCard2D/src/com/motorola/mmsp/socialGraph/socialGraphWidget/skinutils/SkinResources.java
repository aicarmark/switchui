package com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils;

import java.util.ArrayList;

import android.content.Context;

public class SkinResources {
	private final ArrayList<Skin> skins = new ArrayList<Skin>();
	private final SkinViews views = new SkinViews();
	private static Object o = new Object();
	private static SkinResources res = null;
	private Context context;
	private SkinResources(){
		
	}
	
	public synchronized static SkinResources getInstance(Context context) {

			synchronized (o) {
				if (res == null) {
					res = new SkinResources();
					res.context = context;
					SkinsParser.getSkins(context, res.skins, res.views);
				}
			}
	
		
		return res;
	}
	
	public int getSkinCount() {
		return skins.size();
	}
	
	public ArrayList<Skin> getSkins() {
		return skins;
	}
	
	public SkinViews getViews() {
		return views;
	}
	
	public int getSkin(String skin) {
		for (int i = 0; i < getSkinCount(); i++) {
			if (skins.get(i).properties.get(Skin.NAME) != null 
					&& context.getString((Integer)skins.get(i).properties.get(Skin.NAME)).equals(skin)) {
				return i;
			}
		}
		
		return 0;
	}
	
	public String getSkin(int skin) {
		if (skin <= skins.size()) {
			return context.getString((Integer)skins.get(skin).properties.get(Skin.NAME));
		}
		return "";
	}
}
