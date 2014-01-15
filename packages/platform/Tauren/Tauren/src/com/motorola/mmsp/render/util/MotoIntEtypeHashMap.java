package com.motorola.mmsp.render.util;

import java.util.Arrays;

import android.util.Log;

public final class MotoIntEtypeHashMap<T> {
    /**
     * Creates a new IntTHashMap containing no mappings.
     */
    MotoIntEtypeHashMap() {
        this(10);
    }

    /**
     * Creates a new IntTHashMap containing no mappings that will not
     * require any additional memory allocation to store the specified
     * number of mappings.
     */
    public MotoIntEtypeHashMap(int initialCapacity) {
        mKeys = new int[initialCapacity];
        mValues = new Object[initialCapacity];
        mSize = 0;
    }

    /**
     * Gets the int mapped from the specified key, or <code>0</code>
     * if no such mapping has been made.
     */
    public final T get(int key) {
        return get(key, null);
    }

    /**
     * Gets the int mapped from the specified key, or the specified value
     * if no such mapping has been made.
     */
    @SuppressWarnings("unchecked")
	public final T get(int key, T valueIfKeyNotFound) {
        int i = binarySearch(mKeys, 0, mSize, key);

        if (i < 0) {
            return valueIfKeyNotFound;
        } else {
            return (T)mValues[i];
        }
    }

    /**
     * Removes the mapping from the specified key, if there was any.
     */
    public final void delete(int key) {
        int i = binarySearch(mKeys, 0, mSize, key);

        if (i >= 0) {
            removeAt(i);
        }
    }

    /**
     * Removes the mapping at the given index.
     */
    public final void removeAt(int index) {
        System.arraycopy(mKeys, index + 1, mKeys, index, mSize - (index + 1));
        System.arraycopy(mValues, index + 1, mValues, index, mSize - (index + 1));
        mSize--;
        mKeys[mSize] = 0;
        mValues[mSize] = null;
    }
    
    public final void removeValue(T value) {
    	int index = indexOfValue(value);
    	if (index >= 0) {
    		removeAt(index);
    	}
    }

    /**
     * Adds a mapping from the specified key to the specified value,
     * replacing the previous mapping from the specified key if there
     * was one.
     */
    public final void put(int key, T value) {
        int i = binarySearch(mKeys, 0, mSize, key);

        if (i >= 0) {
            mValues[i] = value;
        } else {
            i = ~i;

            if (mSize >= mKeys.length) {
                int n = mSize + 1;

                int[] nkeys = new int[n];
                Object[] nvalues = new Object[n];

                // Log.e("IntTHashMap", "grow " + mKeys.length + " to " + n);
                System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
                System.arraycopy(mValues, 0, nvalues, 0, mValues.length);

                mKeys = nkeys;
                mValues = nvalues;
            }

            if (mSize - i != 0) {
                // Log.e("IntTHashMap", "move " + (mSize - i));
                System.arraycopy(mKeys, i, mKeys, i + 1, mSize - i);
                System.arraycopy(mValues, i, mValues, i + 1, mSize - i);
            }

            mKeys[i] = key;
            mValues[i] = value;
            mSize++;
        }
    }

    /**
     * Returns the number of key-value mappings that this IntTHashMap
     * currently stores.
     */
    public final int size() {
        return mSize;
    }

    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the key from the <code>index</code>th key-value mapping that this
     * IntTHashMap stores.  
     */
    public final int keyAt(int index) {
        return mKeys[index];
    }
    
    /**
     * Given an index in the range <code>0...size()-1</code>, returns
     * the value from the <code>index</code>th key-value mapping that this
     * IntTHashMap stores.  
     */
    @SuppressWarnings("unchecked")
	public final T valueAt(int index) {
        return (T)mValues[index];
    }
    
    public final int[] getKeySet() {
    	int[] keys = new int[mSize];
    	System.arraycopy(mKeys, 0, keys, 0, mSize);
    	return keys;
    }

    /**
     * Returns the index for which {@link #keyAt} would return the
     * specified key, or a negative number if the specified
     * key is not mapped.
     */
    public final int indexOfKey(int key) {
        return binarySearch(mKeys, 0, mSize, key);
    }

    /**
     * Returns an index for which {@link #valueAt} would return the
     * specified key, or a negative number if no keys map to the
     * specified value.
     * Beware that this is a linear search, unlike lookups by key,
     * and that multiple keys can map to the same value and this will
     * find only one of them.
     */
    public final int indexOfValue(T value) {
        for (int i = 0; i < mSize; i++)
            if (mValues[i] == value)
                return i;

        return -1;
    }

    /**
     * Removes all key-value mappings from this IntTHashMap.
     */
    public final void clear() {
    	if (mSize != 0) {
    		Arrays.fill(mKeys, 0, mSize, 0);
            Arrays.fill(mValues, 0, mSize, null);
            mSize = 0;
    	}
    }

    /**
     * Puts a key/value pair into the array, optimizing for the case where
     * the key is greater than all existing keys in the array.
     */
    public final void append(int key, T value) {
        if (mSize != 0 && key <= mKeys[mSize - 1]) {
            put(key, value);
            return;
        }

        int pos = mSize;
        if (pos >= mKeys.length) {
            int n = pos + 1;

            int[] nkeys = new int[n];
            Object[] nvalues = new Object[n];

            // Log.e("IntTHashMap", "grow " + mKeys.length + " to " + n);
            System.arraycopy(mKeys, 0, nkeys, 0, mKeys.length);
            System.arraycopy(mValues, 0, nvalues, 0, mValues.length);

            mKeys = nkeys;
            mValues = nvalues;
        }

        mKeys[pos] = key;
        mValues[pos] = value;
        mSize = pos + 1;
    }
    
    final private static int binarySearch(int[] a, int start, int len, int key) {
        int high = start + len, low = start - 1, guess;

        while (high - low > 1) {
            guess = (high + low) / 2;

            if (a[guess] < key)
                low = guess;
            else
                high = guess;
        }

        if (high == start + len)
            return ~(start + len);
        else if (a[high] == key)
            return high;
        else
            return ~high;
    }

    @SuppressWarnings("unused")
	final private void checkIntegrity() {
        for (int i = 1; i < mSize; i++) {
            if (mKeys[i] <= mKeys[i - 1]) {
                for (int j = 0; j < mSize; j++) {
                    Log.e("FAIL", j + ": " + mKeys[j] + " -> " + mValues[j]);
                }

                throw new RuntimeException();
            }
        }
    }
    
    @Override
    public String toString() {
    	String keyString = buildString(mKeys, mSize);
    	String valueString = buildString(mValues, mSize);
    	String s = String.format("%s -- %s", keyString, valueString);
    	return s;
    }
    
    private final static String buildString(Object[] array, int size) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(size * 6);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < size; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }
    
    private final static String buildString(int[] array, int size) {
        if (array == null) {
            return "null";
        }
        if (array.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(size * 6);
        sb.append('[');
        sb.append(array[0]);
        for (int i = 1; i < size; i++) {
            sb.append(", ");
            sb.append(array[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private int[] mKeys;
    private Object[] mValues;
    private int mSize;
}

