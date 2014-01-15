package com.motorola.mmsp.render.util;

import java.util.Arrays;

public class MotoQueue<T> {
	private Object[] mQueue;
	private int mHead;
	private int mTail;
	
	public MotoQueue(int capitally) {
		mQueue = new Object[capitally];
	}
	
	public void add(T item) {
		mQueue[mTail++] = item;
		if (getSize() >= mQueue.length) {
			int newLen = mQueue.length < 10 ? 10 : (mQueue.length << 1);
			Object[] newQueue = new Object[newLen];
			System.arraycopy(mQueue, 0, newQueue, 0, mQueue.length);
			Arrays.fill(mQueue, null);
			mQueue = newQueue;
		}
		if (mTail >= mQueue.length) {
			mTail = 0;
		}
	}
	
	public T remove() {
		@SuppressWarnings("unchecked")
		T item = (T)mQueue[mHead];
		mQueue[mHead++] = null;
		if (mHead >= mQueue.length) {
			mHead = 0;
		}
		return item;
	}
	
	public int getSize() {
		return ((mTail >= mHead) ? (mTail - mHead) : (mTail + mQueue.length - mHead));
	}
}
