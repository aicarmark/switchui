package com.motorola.mmsp.render;

public final class MotoAttributeArray {
	int mSize;
	String mAttrs[];
	Object mValues[];
	
	public MotoAttributeArray(int capacity) {
		mAttrs = new String[capacity];
		mValues = new Object[capacity];
	}
	
	public Object getValue(String attri) {
		for (int i=0; i<mSize; i++) {
			if (mAttrs[i].equals(attri)) {
				return mValues[i];
			}
		}
		return null;
	}
	
	public void add(String attribute, Object value) {
		set(mSize++, attribute, value);
	}
	
	public void set(int index, String attribute, Object value) {
		mAttrs[index] = attribute;
		mValues[index] = value;
	}
	
	public String[] getAttributes() {
		return mAttrs;
	}
	
	public Object[] getValues() {
		return mValues;
	}
	
	public int getSize() {
		return mSize;
	}
}
