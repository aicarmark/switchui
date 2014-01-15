package com.motorola.mmsp.motohomex.util;

import java.util.ArrayList;

public class FilteredArrayList<E> extends ArrayList<E> {

    /** Ordered indexes into ArrayList that should be returned. */
    private ArrayList<Integer> mFilter;
    private ArrayList<E> mArray;

    /**
     *
     */
    private static final long serialVersionUID = 173801792948L;

    public void setFilter(ArrayList<Integer> filter){
        mFilter = filter;
    }

    public void setArray(ArrayList<E> array){
        mArray = array;
    }
    /*Added by ncqp34 at Jun-13-2012 for app tray*/
    public  ArrayList<E> getFilterList(){
	if(mArray!=null){
	    if (mFilter != null){
		ArrayList<E> array = new ArrayList<E>();
		for(int i=0;i < mFilter.size();i++){
		    array.add((E) mArray.get(mFilter.get(i)));
		}
		return array;
	    }
	    return mArray;
	}
	return null;
    }
    /*ended by ncqp34*/
    @SuppressWarnings("unchecked") @Override
    public E get(int index) {
        if (mArray != null){
            if (mFilter != null){
                return (E) mArray.get(mFilter.get(index));
            } else {
                return (E) mArray.get(index);
            }
        } else {
            return null;
        }
    }

    @Override
    public int size() {
        if (mArray != null){
            if (mFilter != null){
                return mFilter.size();
            } else {
                return mArray.size();
            }
        } else {
            return 0;
        }
    }

    @Override 
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains (Object object){
        if (mArray != null){
            if (mFilter != null){
                int index = mArray.indexOf(object);
                return mFilter.contains(index);
            } else {
                return mArray.contains(object);
            }
        } else {
            return false;
        }
    }

    @Override 
    public void clear() {
        mArray = null;
        mFilter = null;
     }

}
