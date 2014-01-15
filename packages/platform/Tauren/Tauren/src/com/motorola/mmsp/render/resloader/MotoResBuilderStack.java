package com.motorola.mmsp.render.resloader;

import java.util.ArrayList;


class MotoResBuilderStack extends ArrayList<MotoComponentBuilder>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int mMark = 0;
	public MotoResBuilderStack(int cap) {
		super(cap);
	}
	void push(MotoComponentBuilder builder) {
		add(builder);
	}
	MotoComponentBuilder pop() {
		return remove(size()-1);
	}
	MotoComponentBuilder getLast() {
		return get(size() - 1);
	}
	int getLastType() {
		MotoComponentBuilder last = get(size() - 1);
		return last.mType;
	}
	int upMark() {
		return ++mMark;
	}
	int downMark() {
		return --mMark;
	}
}
