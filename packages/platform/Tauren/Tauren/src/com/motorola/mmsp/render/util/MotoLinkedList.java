package com.motorola.mmsp.render.util;

public final class MotoLinkedList<E> {
	public static final class Node<E> {
		public E mData;
		public Node<E> mPre;
		public Node<E> mNext;
		public float mOrder;
	}
	
	public MotoLinkedList(int initCap, float increaceMul) {
		mNodeCache = new Object[initCap];
		mIncreaceMul = increaceMul;
		for (int i=0; i<initCap; i++) {
			mNodeCache[i] = new Node<E>();
		}
	}

	private int mSize = 0;
	private Node<E> mFirst = null;
	private Node<E> mLast = null;
	private float mIncreaceMul;
	
	private Object[] mNodeCache;
	private int mNodeCacheIndex = 0;

	public final int size() {
		return mSize;
	}

	public final Node<E> getFirst() {
		return mFirst;
	}
	
	public final Node<E> getLast() {
		return mLast;
	}

	public final void addBefore(Node<E> info, Node<E> newInfo) {
		int cacheLength = mNodeCache.length;
		if (mSize >= cacheLength) {
			Object[] newArray = new Object[(int)((cacheLength * mIncreaceMul) + 1)];
			System.arraycopy(mNodeCache, 0, newArray, 0, cacheLength);
			mNodeCache = newArray;
		}
		
		if (mFirst == null) {
			mFirst = newInfo;
			mLast = newInfo;
		} else {
			if (info != null) {
				newInfo.mPre = info.mPre;
				if (newInfo.mPre == null) {
					mFirst = newInfo;
				} else {
					newInfo.mPre.mNext = newInfo;
				}
				newInfo.mNext = info;
				info.mPre = newInfo;
			}
		}

		mSize++;
	}

	public final void addAfter(Node<E> info, Node<E> newInfo) {
		int cacheLength = mNodeCache.length;
		if (mSize >= cacheLength) {
			Object[] newArray = new Object[(int)((cacheLength * mIncreaceMul) + 1)];
			System.arraycopy(mNodeCache, 0, newArray, 0, cacheLength);
			mNodeCache = newArray;
		}
		
		if (mFirst == null) {
			mFirst = newInfo;
			mLast = newInfo;
		} else {
			if (info != null) {
				newInfo.mNext = info.mNext;
				if (newInfo.mNext != null) {
					newInfo.mNext.mPre = newInfo;
				} else {
					mLast = newInfo;
				}
				info.mNext = newInfo;
				newInfo.mPre = info;
			}
		}

		mSize++;
	}

	public final void remove(Node<E> info) {
		if (info != null) {
			if (info.mPre != null) {
				info.mPre.mNext = info.mNext;
			} else {
				mFirst = info.mNext;
			}

			
			if (info.mNext != null) {
				info.mNext.mPre = info.mPre;
			} else {
				mLast = info.mPre;
			}

			if (mSize > 0) {
				mSize--;
			}
		}
	}

	public final void addAfter(Node<E> info, E data) {
		int cacheLength = mNodeCache.length;
		if (mSize >= cacheLength) {
			Object[] newArray = new Object[(int)((cacheLength * mIncreaceMul) + 1)];
			System.arraycopy(mNodeCache, 0, newArray, 0, cacheLength);
			mNodeCache = newArray;
		}
		
		@SuppressWarnings("unchecked")
		Node<E> node = (Node<E>) mNodeCache[mNodeCacheIndex++];
		if (node == null) {
			node = new Node<E>();
			mNodeCache[mNodeCacheIndex-1] = node;
		}
		if (mSize >= mNodeCacheIndex) {
			mNodeCacheIndex = 0;
		}
		
		node.mData = data;
		node.mPre = null;
		node.mNext = null;
		
		addAfter(info, node);
	}
	
	public final void orderAdd(E data, float order) {
		int cacheLength = mNodeCache.length;
		if (mSize >= cacheLength) {
			Object[] newArray = new Object[(int)((cacheLength * mIncreaceMul) + 1)];
			System.arraycopy(mNodeCache, 0, newArray, 0, cacheLength);
			mNodeCache = newArray;
		}
		
		@SuppressWarnings("unchecked")
		Node<E> node = (Node<E>) mNodeCache[mNodeCacheIndex++];
		if (node == null) {
			node = new Node<E>();
			mNodeCache[mNodeCacheIndex-1] = node;
		}
		if (mSize >= mNodeCacheIndex) {
			mNodeCacheIndex = 0;
		}
		
		node.mData = data;
		node.mOrder = order;
		node.mPre = null;
		node.mNext = null;
		
		if (mFirst != null) {
			if (order <= mFirst.mOrder) {
				addBefore(mFirst, node);
			} else {
				Node<E> cursor = mFirst;
				while (cursor.mNext != null) {
					if (order <= cursor.mNext.mOrder) {
						break;
					}
					cursor = cursor.mNext;
				}
				addAfter(cursor, node);
			}
		} else {
			mFirst = node;
			mLast = node;
			mSize ++;
		}
	}
	
	public final void clear() {
		mSize = 0;
		mNodeCacheIndex = 0;
		mFirst = null;
		mLast = null;
	}
}
