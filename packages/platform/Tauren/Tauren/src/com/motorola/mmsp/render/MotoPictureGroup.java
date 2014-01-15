package com.motorola.mmsp.render;

import java.util.ArrayList;

import android.graphics.Rect;

import com.motorola.mmsp.render.resloader.MotoResLoader.PlayerConfigInfo;
import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoLinkedList;

public class MotoPictureGroup extends MotoPicture {

	@SuppressWarnings("unused")
	private static final String TAG = MotoConfig.TAG;
	
	/*
	 * 
	 */
	private ArrayList<MotoPicture> mPictureArrayList = new ArrayList<MotoPicture>();
	
	
	public MotoPictureGroup(MotoAttributeArray attr, ResOutputInfo resInfo) {
		super(attr, resInfo);
	}

	/**
	 * Constructor
	 * 
	 * @param pictureList
	 * @param name
	 */
	public MotoPictureGroup(MotoPictureInfo pictureInfo, MotoPicture[] pictureList, int id) {
		this(pictureInfo, pictureList, id, null, null);
	}

	/**
	 * Constructor
	 * 
	 * @param pictureList
	 *            : picture list
	 * @param x
	 *            : x position
	 * @param y
	 *            : y position
	 * @param name
	 *            : name
	 */
	public MotoPictureGroup(MotoPictureInfo pictureInfo, MotoPicture[] pictureList, 
			int id, MotoAnimationSet animationSet, MotoInteractionSet interactionSet) {
		super(pictureInfo, id, animationSet, interactionSet);
		setChildren(pictureList);
	}
	
	/**
	 * set pictures that belong to this picture group
	 * 
	 * @param pictureList
	 */
	public void setChildren(MotoPicture[] pictureList) {
		int count = (pictureList != null) ? pictureList.length : 0;
		for (int i = 0; i < count; i++) {
			MotoPicture child = pictureList[i];
			if (child != null) {
				child.setParent(this);
			}
			/*
			 * add by neil 2012-03-06
			 */
			mPictureArrayList.add(child);
		}
	}

	/**
	 * set pictures that belong to this picture group
	 * 
	 * @param pictureListInput
	 */
	public void setChildren(ArrayList<MotoPicture> pictureListInput) {
		if (pictureListInput == null) {
			mPictureArrayList.clear();
		}
		mPictureArrayList = pictureListInput;
		int size = mPictureArrayList != null ? mPictureArrayList.size() : 0;
		for (int i=0; i<size; i++) {
			MotoPicture child = mPictureArrayList.get(i);
			if (child != null) {
				child.setParent(this);
			}
		}
	}

	/**
	 * add a picture into this picture group
	 * 
	 * @param picture
	 */
	public void addChild(MotoPicture picture) {
		if (picture == null) {
			return;
		}
		if (mPictureArrayList != null) {
			mPictureArrayList.add(picture);
			picture.setParent(this);
		}
	}

	/**
	 * get a picture from this picture group
	 * 
	 * @param index
	 * @return
	 */
	public MotoPicture getChildAt(int index) {
		if (mPictureArrayList != null) {
			return mPictureArrayList.get(index);
		}
		return null;
	}

	/**
	 * get pictures count from this picture group
	 * 
	 * @return pictures count
	 */
	public int getChildCount() {
		if (mPictureArrayList != null) {
			return mPictureArrayList.size();
		}
		return 0;
	}

	/**
	 * get picture list from this picture group
	 * 
	 * @return picture list
	 */
	public ArrayList<MotoPicture> getChildren() {
		return mPictureArrayList;
	}

	/**
	 * remove a picture from this picture group by index
	 * 
	 * @param index
	 */
	public void removeChildAt(int index) {
		if (mPictureArrayList != null) {
			MotoPicture child = mPictureArrayList.remove(index);
			if (child != null) {
				child.setParent(null);
			}
		}
	}

	/**
	 * remove a picture from this picture group by picture
	 * 
	 * @param picture
	 */
	public void removeChildren(MotoPicture picture) {
		if (mPictureArrayList != null) {
			mPictureArrayList.remove(picture);
			if (picture != null) {
				picture.setParent(null);
			}
		}
	}

	/**
	 * remove all pictures from this picture group
	 */
	public void removeAllChildren() {
		if (mPictureArrayList != null) {
			int size = mPictureArrayList.size();
			for (int i=0; i<size; i++) {
				MotoPicture child = mPictureArrayList.get(i);
				if (child != null) {
					child.setParent(null);
				}
			}
			mPictureArrayList.clear();
		}
	}

	/**
	 * whether has child
	 * 
	 * @return boolean
	 */
	public boolean isPictureLeaf() {
		return false;
	}

	/**
	 * Prepare the MotoPicture itself information, not multiply with parent.
	 * 
	 * @param interactionInput
	 *            : calculate next frame by interactionInput
	 */
	@Override
	public void nextFrame(int time, PlayerConfigInfo playerConfig) {
		super.nextFrame(time, playerConfig);
		
		int size = mPictureArrayList != null ? mPictureArrayList.size() : 0;
		for (int i = 0; i < size; i++) {
			MotoPicture child = mPictureArrayList.get(i);
			if (child != null) {
				child.nextFrame(time, playerConfig);
			}
		}
	}

	/**
	 * render current picture base on some information
	 * 
	 * @param paintableList
	 * @param parentPaintable
	 * @param dirtyRects
	 */
	@Override
	public void renderFrame(MotoLinkedList<MotoPaintable> paintableList,
			MotoPaintable parentPaintable, ArrayList<Rect> dirtyRects, PlayerConfigInfo playerConfig) {
		super.renderFrame(paintableList, parentPaintable, dirtyRects, playerConfig);
		int size = mPictureArrayList != null ? mPictureArrayList.size() : 0;
		for (int i = 0; i < size; i++) {
			MotoPicture child = mPictureArrayList.get(i);
			if (child != null) {
				child.renderFrame(paintableList, mPhysicalStatus, dirtyRects, playerConfig);
			}
		}
	}

	public MotoPicture findPictureById(int id) {
		if (getId() == id) {
			return this;
		}
		
		for (MotoPicture pic : mPictureArrayList) {
			if (pic != null) {
				MotoPicture find = pic.findPictureById(id);
				if (find != null) {
					return find;
				}
			}
		}
		return null;
	}
}
