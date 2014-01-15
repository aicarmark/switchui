package com.motorola.mmsp.render;

public class MotoPictureInfo {
	float mX;
	float mY;
	float mZ;
	int mLevel;
	float mAlpha = 255;
	float mRotate;
	float mRotatePx;
	float mRotatePy;
	float mRotateX;
	float mRotateY;
	float mRotateXPy;
	float mRotateYPx;
	float mScalePx;
	float mScalePy;
	float mScaleX = 1;
	float mScaleY = 1;
	float mRotateXYOffsetAngle;
	boolean mVisible = true;
	int mWidth;
	int mHeight;
	boolean mScalePivotExplicitlySet;
	private boolean mRotatePivotExplicitlySet;
	private boolean mRotateXPivotExplicitlySet;
	private boolean mRotateYPivotExplicitlySet;

	public MotoPictureInfo() {

	}

	/**
	 * Construction
	 * 
	 * @param bitmapId
	 * @param bitmap
	 * @param x
	 * @param y
	 * @param alpha
	 *            modify at 2012.04.20 for mScaleX and mScaleY default by 1
	 */
	public MotoPictureInfo(float x, float y, float alpha) {
		this(x, y, 0, 0, alpha, 0, 0, 0, 1, 1, true, 0, 0, 0, 0, false, false,
				false);
	}

	/**
	 * Construction
	 * 
	 * @param bitmapId
	 * @param bitmap
	 * @param x
	 * @param y
	 * @param z
	 * @param alpha
	 * @param rotate
	 * @param rotatePx
	 * @param rotatePy
	 * @param scaleX
	 * @param scaleY
	 * @param visible
	 */
	public MotoPictureInfo(float x, float y, float z, int level, float alpha,
			float rotate, float rotatePx, float rotatePy, float scaleX,
			float scaleY, boolean visible, float rotateX, float rotateY,
			float rotateYPx, float rotateXPy, boolean isRatotePivotSet,
			boolean isRotateXPivotSet, boolean isRotateYPivotSet) {
		this.mX = x;
		this.mY = y;
		this.mLevel = level;
		this.mAlpha = alpha;
		this.mRotate = rotate;
		this.mRotatePx = rotatePx;
		this.mRotatePy = rotatePy;
		this.mScaleX = scaleX;
		this.mScaleY = scaleY;
		this.mVisible = visible;
		this.mRotateX = rotateX;
		this.mRotateY = rotateY;
		this.mRotateYPx = rotateYPx;
		this.mRotateXPy = rotateXPy;
		this.mZ = z;
		this.mRotatePivotExplicitlySet = isRatotePivotSet;
		this.mRotateXPivotExplicitlySet = isRotateXPivotSet;
		this.mRotateYPivotExplicitlySet = isRotateYPivotSet;
	}

	@Override
	public MotoPictureInfo clone() {
		MotoPictureInfo copy = new MotoPictureInfo(mX, mY, mZ, mLevel, mAlpha,
				mRotate, mRotatePx, mRotatePy, mScaleX, mScaleY, mVisible,
				mRotateX, mRotateY, mRotateXPy, mRotateYPx,
				mRotatePivotExplicitlySet, mRotateXPivotExplicitlySet,
				mRotateYPivotExplicitlySet);
		copy.mScalePx = mScalePx;
		copy.mScalePy = mScalePy;
		copy.mScalePivotExplicitlySet = mScalePivotExplicitlySet;
		copy.mRotateXYOffsetAngle = mRotateXYOffsetAngle;
		copy.mWidth = mWidth;
		copy.mHeight = mHeight;
		return copy;
	}

	public void set(MotoPictureInfo other) {
		mX = other.mX;
		mY = other.mY;
		mLevel = other.mLevel;
		mAlpha = other.mAlpha;
		mRotate = other.mRotate;
		mRotatePx = other.mRotatePx;
		mRotatePy = other.mRotatePy;
		mScalePx = other.mScalePx; 
		mScalePy = other.mScalePy;
		mScaleX = other.mScaleX;
		mScaleY = other.mScaleY;
		mVisible = other.mVisible;
		mWidth = other.mWidth;
		mHeight = other.mHeight;
		mRotateX = other.mRotateX;
		mRotateY = other.mRotateY;
		mRotateXPy = other.mRotateXPy;
		mRotateYPx = other.mRotateYPx;
		mZ = other.mZ;
		this.mRotatePivotExplicitlySet = other.mRotatePivotExplicitlySet;
		this.mScalePivotExplicitlySet = other.mScalePivotExplicitlySet;
		this.mRotateXPivotExplicitlySet = other.mRotateXPivotExplicitlySet;
		this.mRotateYPivotExplicitlySet = other.mRotateYPivotExplicitlySet;
	}

	/**
	 * set x position
	 * 
	 * @param x
	 */
	public void setX(float x) {
		this.mX = x;
	}

	/**
	 * get x position
	 * 
	 * @return float
	 */
	public float getX() {
		return mX;
	}

	/**
	 * set y position
	 * 
	 * @param y
	 */
	public void setY(float y) {
		this.mY = y;
	}

	/**
	 * get y position
	 * 
	 * @return float
	 */
	public float getY() {
		return mY;
	}

	/**
	 * set z position
	 * 
	 * @param y
	 */
	public void setLevel(int level) {
		mLevel = level;
	}

	/**
	 * get z layer
	 * 
	 * @return float
	 */
	public int getLevel() {
		return mLevel;
	}

	/**
	 * set alpha value
	 * 
	 * @param alpha
	 */
	public void setAlpha(float alpha) {
		this.mAlpha = alpha;
	}

	/**
	 * get alpha value of this pictureInfo
	 * 
	 * @return float
	 */
	public float getAlpha() {
		return mAlpha;
	}

	/**
	 * set rotate degree for this pictureInfo
	 * 
	 * @param rotate
	 */
	public void setRotate(float rotate) {
		this.mRotate = rotate;
	}

	/**
	 * get rotate degree value
	 * 
	 * @return float
	 */
	public float getRotate() {
		return mRotate;
	}

	/**
	 * set x position of rotation
	 * 
	 * @param rotatePx
	 */
	public void setRotatePx(float rotatePx) {
		this.mRotatePivotExplicitlySet = true;
		mRotatePx = rotatePx;
	}

	/**
	 * get x position of rotation
	 * 
	 * @return x
	 */
	public float getRotatePx() {
		if (!mRotatePivotExplicitlySet) {
			return mWidth / 2f;
		}
		return mRotatePx;
	}

	/**
	 * set y position of rotation
	 * 
	 * @param rotatePy
	 */
	public void setRotatePy(float rotatePy) {
		this.mRotatePivotExplicitlySet = true;
		mRotatePy = rotatePy;
	}

	/**
	 * get y position of rotation
	 * 
	 * @return y
	 */
	public float getRotatePy() {
		if (!mRotatePivotExplicitlySet) {
			return mHeight / 2f;
		}
		return mRotatePy;
	}


	/**
	 * set x position of scale
	 * 
	 * @param scalePx
	 */
	public void setScalePx(float scalePx) {
		this.mScalePivotExplicitlySet= true;
		mScalePx = scalePx;
	}

	/**
	 * get x position of scale
	 * 
	 * @return mScalePx
	 */
	public float getScalePx() {
		if (!mScalePivotExplicitlySet) {
			return mWidth / 2f;
		}
		return mScalePx;
	}

	/**
	 * set y position of scale
	 * 
	 * @param scalePy
	 */
	public void setScalePy(float scalePy) {
		this.mScalePivotExplicitlySet = true;
		mScalePx = scalePy;
	}

	/**
	 * get y position of scale
	 * 
	 * @return mScalePy
	 */
	public float getScalePy() {
		if (!mScalePivotExplicitlySet) {
			return mHeight / 2f;
		}
		return mScalePy;
	}
	

	/**
	 * get rotate degree value with the x-axis as the center
	 * 
	 * @return float
	 */
	public float getRotateX() {
		return mRotateX;
	}

	/**
	 * set rotate degree value with the x-axis as the center for this picture
	 * 
	 * @param mRotateX
	 */
	public void setRotateX(float mRotateX) {
		this.mRotateX = mRotateX;
	}

	/**
	 * get rotate degree value with the y-axis as the center
	 * 
	 * @return
	 */
	public float getRotateY() {
		return mRotateY;
	}

	/**
	 * set rotate degree value with the y-axis as the center for this picture
	 * 
	 * @param mRotateY
	 */
	public void setRotateY(float mRotateY) {
		this.mRotateY = mRotateY;
	}

	/**
	 * get rotate position value with the y-axis as the center
	 * 
	 * @return
	 */
	public float getRotateYPx() {
		if (!mRotateYPivotExplicitlySet) {
			return mWidth / 2f;
		}
		return mRotateYPx;
	}

	/**
	 * set rotate position with the y-axis as the center for this picture
	 * 
	 * @param mRotateXPy
	 */
	public void setRotateYPx(float rotateYPx) {
		this.mRotateYPivotExplicitlySet = true;
		this.mRotateYPx = rotateYPx;
	}

	/**
	 * get rotate position with the x-axis as the center
	 * 
	 * @return
	 */
	public float getRotateXPy() {
		if (!mRotateXPivotExplicitlySet) {
			return mHeight / 2f;
		}
		return mRotateXPy;
	}

	/**
	 * set rotate position with the x-axis as the center for this picture
	 * 
	 * @param mRotateYPx
	 */
	public void setRotateXPy(float rotateXPy) {
		this.mRotateXPivotExplicitlySet = true;
		this.mRotateXPy = rotateXPy;
	}

	/**
	 * get z position value
	 * 
	 * @return
	 */
	public float getZ() {
		return mZ;
	}

	/**
	 * set z position
	 * 
	 * @param mZ
	 */
	public void setZ(float mZ) {
		this.mZ = mZ;
	}

	/**
	 * set x position of scale
	 * 
	 * @param scalePx
	 */
	public void setScaleX(float scalex) {
		mScaleX = scalex;
	}

	/**
	 * get x position of scale
	 * 
	 * @return float
	 */
	public float getScaleX() {
		return mScaleX;
	}

	/**
	 * set y position of scale
	 * 
	 * @param scalePy
	 */
	public void setScaleY(float scalePy) {
		mScaleY = scalePy;
	}

	/**
	 * get y position of scale
	 * 
	 * @return float
	 */
	public float getScaleY() {
		return mScaleY;
	}

	public float getRotateXYOffsetAngle() {
		return mRotateXYOffsetAngle;
	}

	public void setRotateXYOffsetAngle(float rotateXYOffsetAngle) {
		this.mRotateXYOffsetAngle = rotateXYOffsetAngle;
	}
	
	/**
	 * set visibility
	 * 
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		this.mVisible = visible;
	}
	
	/**
	 * get visibility
	 * 
	 * @return boolean
	 */
	public boolean getVisible() {
		return mVisible;
	}

	public int getWidth() {
		return mWidth;
	}

	public void setWidth(int width) {
		mWidth = width;
	}

	public int getHeight() {
		return mHeight;
	}

	public void setHeight(int height) {
		mHeight = height;
	}

	/**
	 * add pictureInfo
	 * 
	 * @param info
	 * @param properties
	 * @param output
	 */
	public static void addPictureInfo(MotoPictureInfo info1,
			MotoAnimationProperties info2, MotoPictureInfo output) {
		if (output != null) {
			if (info1 != null && info2 != null) {
				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_TRANSLATE_X)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mX = info1.mX
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_X);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mX = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_X);
					break;
				default:
					// output.mX = info1.mX;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_TRANSLATE_Y)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mY = info1.mY
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_Y);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mY = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_Y);
					break;
				default:
					// output.mY = info1.mY;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_TRANSLATE_Z)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mZ = info1.mZ
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_Z);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mZ = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_Z);
					break;
				default:
					// output.mTranslateZ = info1.mTranslateZ;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_ALPHA)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mAlpha = info1.mAlpha
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_ALPHA);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mAlpha = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_ALPHA);
					break;
				default:
					// output.mAlpha = info1.mAlpha;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mRotate = info1.mRotate
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mRotate = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE);
					break;
				default:
					// output.mRotate = info1.mRotate;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_PX)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mRotatePivotExplicitlySet = true;
					output.mRotatePx = info1.mRotatePx
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_PX);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mRotatePivotExplicitlySet = true;
					output.mRotatePx = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_PX);
					break;
				default:
					// output.mRotatePx = info1.mRotatePx;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_PY)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mRotatePivotExplicitlySet = true;
					output.mRotatePy = info1.mRotatePy
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_PY);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mRotatePivotExplicitlySet = true;
					output.mRotatePy = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_PY);
					break;
				default:
					// output.mRotatePy = info1.mRotatePy;
					break;
				}


				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_X)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mRotateX = info1.mRotateX
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_X);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mRotateX = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_X);
					break;
				default:
					// output.mRotateX = info1.mRotateX;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_Y)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mRotateY = info1.mRotateY
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_Y);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mRotateY = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_Y);
					break;
				default:
					// output.mRotateY = info1.mRotateY;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_SCALE_X)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mScaleX = info1.mScaleX
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_SCALE_X);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mScaleX = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_SCALE_X);
					break;
				default:
					// output.mScaleX = info1.mScaleX;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_SCALE_Y)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mScaleY = info1.mScaleY
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_SCALE_Y);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mScaleY = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_SCALE_Y);
					break;
				default:
					// output.mScaleY = info1.mScaleY;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_VISIBLE)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					if (info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_VISIBLE) > 0) {
						output.mVisible = !output.mVisible;
					} else {
						//output.mVisible = output.mVisible;
					}
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mVisible = (info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_VISIBLE) == 1);
					break;
				default:
					// output.mVisible = info1.mVisible;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_X_PY)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mRotateXPy = info1.mRotateXPy
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_X_PY);
					output.mRotateXPivotExplicitlySet = true;
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mRotateXPy = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_X_PY);
					output.mRotateXPivotExplicitlySet = true;
					break;
				default:
					// output.mRotateXPy = info1.mRotateXPy;
					break;
				}

				switch (info2
						.getPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_Y_PX)) {
				case MotoObjectAnimation.AFFECT_TYPE_DELTA:
					output.mRotateYPivotExplicitlySet = true;
					output.mRotateYPx = info1.mRotateYPx
							+ info2.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_Y_PX);
					break;
				case MotoObjectAnimation.AFFECT_TYPE_SET:
					output.mRotateYPivotExplicitlySet = true;
					output.mRotateYPx = info2
							.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_Y_PX);
					break;
				default:
					// output.mRotateYPx = info1.mRotateYPx;
					break;
				}

			}
			
			/*else if (info1 != null) {
				output.mX = info1.mX;
				output.mY = info1.mY;
				output.mZ = info1.mZ;
				output.mAlpha = info1.mAlpha;
				output.mRotate = info1.mRotate;
				output.mRotatePx = info1.mRotatePx;
				output.mRotatePy = info1.mRotatePy;
				output.mRotateXPy = info1.mRotateXPy;
				output.mRotateYPx = info1.mRotateYPx;
				output.mRotateX = info1.mRotateX;
				output.mRotateY = info1.mRotateY;
				output.mTranslateZ = info1.mTranslateZ;
				output.mScaleX = info1.mScaleX;
				output.mScaleY = info1.mScaleY;
				output.mVisible = info1.mVisible;
			} else if (info2 != null) {
				output.mX = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_X);
				output.mY = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_Y);
				output.mAlpha = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_ALPHA);
				output.mRotate = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE);
				output.mRotatePx = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_PX);
				output.mRotatePy = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_PY);
				output.mRotateYPx = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_Y_PX);
				output.mRotateXPy = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_X_PY);
				output.mRotateX = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_X);
				output.mRotateY = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_ROTATE_Y);
				output.mTranslateZ = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_TRANSLATE_Z);
				output.mScaleX = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_SCALE_X);
				output.mScaleY = info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_SCALE_Y);
				output.mVisible = (info2
						.getPropertyValue(MotoObjectAnimation.ANIMATION_VISIBLE) == 1);
			}*/
		}
	}

}
