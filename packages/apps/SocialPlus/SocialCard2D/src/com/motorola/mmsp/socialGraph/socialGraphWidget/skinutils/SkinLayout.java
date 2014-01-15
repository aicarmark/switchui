package com.motorola.mmsp.socialGraph.socialGraphWidget.skinutils;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;

@SuppressWarnings("deprecation")
public class SkinLayout extends AbsoluteLayout{
	private SkinResources skinRes = null;

	public SkinLayout(Context context) {
		super(context);
		if (skinRes == null) {
			skinRes = SkinResources.getInstance(context);
		}
	}
	
	public SkinLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (skinRes == null) {
			skinRes = SkinResources.getInstance(context);
		}
	}
	
	public SkinLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (skinRes == null) {
			skinRes = SkinResources.getInstance(context);
		}
	}
	
	public void setSkin(int skin) {
		ArrayList<Skin> skins = skinRes.getSkins();
		if (skin >= skins.size()) {
			return;
		}
		
		for (String id : skinRes.getViews().ids.keySet()) {
			if (id != null && skinRes.getViews().ids.get(id) != null) {
				int view = (Integer)skinRes.getViews().ids.get(id);
				if (view >= 0) {
					ImageView imageView = (ImageView) findViewById(view);
					if (imageView == null) {
						continue;
					} else {
						imageView.setImageResource((Integer) getSkins().get(
								skin).properties.get(id));
					}
				}
			}
		}
	}
	
	public void setSkin(String skin) {
		int skinInt = skinRes.getSkin(skin);
		setSkin(skinInt);
	}
	
	public int getSkin(String skin) {
		return skinRes.getSkin(skin);
	}
	
	public String getSkin(int skin) {
		return skinRes.getSkin(skin);
	}

	private ArrayList<Skin> getSkins() {
		return skinRes.getSkins();
	}
}
