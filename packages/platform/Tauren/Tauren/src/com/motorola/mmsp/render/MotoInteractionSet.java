package com.motorola.mmsp.render;

import java.util.ArrayList;

import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoMathUtil;



public class MotoInteractionSet {
	private ArrayList<MotoInteraction> mInteractions = new ArrayList<MotoInteraction>();
	private int mId;
	private ArrayList<MotoInteraction> mHandleEventInteractions = new ArrayList<MotoInteraction>();
	
	public MotoInteractionSet(MotoAttributeArray attr, ResOutputInfo resInfo) {
		String id = (String)attr.getValue("id");
		if (id != null) {
			mId = MotoMathUtil.parseInt(id);
		}
	}
	
	public void setId(int id) {
		mId = id;
	}
	
	public int getId() {
		return mId;
	}
	
	public void addInteraction(MotoInteraction interaction) {
		mInteractions.add(interaction);
	}
	public void removeInteraction(MotoInteraction interaction) {
		mInteractions.remove(interaction);
	}
	public void removeInteraction(int index) {
		mInteractions.remove(index);
	}
	public void removeAll() {
		mInteractions.clear();
	}
	public MotoInteraction getInteractionAt(int index) {
		if (index <= mInteractions.size()) {
			return mInteractions.get(index);
		}
		return null;
	}
	public int getInteractionCount() {
		return mInteractions.size();
	}
	
	
	public boolean isInterceptEvent(MotoInteractionEvent event, boolean isProvioursIntercepted) {
		int size = mInteractions.size();
		for (int i=0; i<size; i++) {
			MotoInteraction child = mInteractions.get(i);
			if (child.isInterceptEvent(event, isProvioursIntercepted)) {
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<MotoInteraction> handleEventInteractions(MotoInteractionEvent event, boolean isProvioursIntercepted) {
		mHandleEventInteractions.clear();
		int size = mInteractions.size();
		for (int i=0; i<size; i++) {
			MotoInteraction child = mInteractions.get(i);
			if (child.isHandleEvent(event, isProvioursIntercepted)) {
				mHandleEventInteractions.add(child);
			}
		}
		return mHandleEventInteractions;
	}
}
