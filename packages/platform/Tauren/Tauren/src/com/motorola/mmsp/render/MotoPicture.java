package com.motorola.mmsp.render;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;

import com.motorola.mmsp.render.resloader.MotoComponentBuilder;
import com.motorola.mmsp.render.resloader.MotoResLoader.PlayerConfigInfo;
import com.motorola.mmsp.render.resloader.MotoResLoader.ResOutputInfo;
import com.motorola.mmsp.render.util.MotoLinkedList;
import com.motorola.mmsp.render.util.MotoMathUtil;

public class MotoPicture {
	@SuppressWarnings("unused")
	private static final String TAG = MotoConfig.TAG;

	protected MotoPictureInfo mPictureInitStatus;
	protected MotoAnimationProperties mProperties = new MotoAnimationProperties();
	protected MotoPictureInfo mPictureCurrentStatus;
	protected MotoPaintable mPhysicalStatus = new MotoPaintable();
	protected MotoAnimationSet mAnimationSet;
	protected ArrayList<MotoAnimationSet> mInteractionAnimations = new ArrayList<MotoAnimationSet>(10); 
	protected MotoInteractionSet mInteractionSet;
	protected MotoPictureGroup mParent;
	protected int mId;
	protected MotoEventHandler mEventHandler;
	protected boolean mBlockAnimation;

	protected Rect mFormerRect = new Rect();
	protected Rect mCurrentRect = new Rect();
	protected RectF mCurrentRectFloat = new RectF();
	protected RectF mRectToBeMappedFloat = new RectF();
	protected Matrix mCurrentMatrix = new Matrix();


	// protected static final Matrix INIT_MATRIX = new Matrix();
	protected static final Matrix MATRIX_3D = new Matrix();
	protected static final Camera CAMERA = new Camera();

	protected boolean mDirty = true;
	//This player only available in rootPictrue
	protected MotoRenderPlayer mPlayer;

	/**
	 * Default constructor invoked by resource loader.
	 * 
	 * @param attr
	 *            : Attribute set defined in resource file.
	 * @param resInfo
	 *            : the loaded resource.
	 */
	public MotoPicture(MotoAttributeArray attr, ResOutputInfo resInfo) {
		resInfo.mDebugInfo.mObjectCount++;
		
		String s_id = (String) attr.getValue("id");
		if (s_id != null) {
			mId = MotoMathUtil.parseInt(s_id);
		}

		String s_x = (String) attr.getValue("x");
		float x = 0;
		final float ds2PxScale = resInfo.mPlayConfigInfo.mDs2PxScale;
		if (s_x != null) {
			x = MotoMathUtil.parseFloatNormal(s_x, ds2PxScale);
		}
		String s_y = (String) attr.getValue("y");
		float y = 0;
		if (s_y != null) {
			y = MotoMathUtil.parseFloatNormal(s_y, ds2PxScale);
		}
		String s_z = (String) attr.getValue("z");
		float z = 0;
		if (s_z != null) {
			z = MotoMathUtil.parseFloatNormal(s_z, ds2PxScale);
		}
		String s_level = (String) attr.getValue("level");
		int level = 0;
		if (s_level != null) {
			level = MotoMathUtil.parseInt(s_level);
		}
		String s_alpha = (String) attr.getValue("alpha");
		float alpha = 255;
		if (s_alpha != null) {
			alpha = MotoMathUtil.parseFloat(s_alpha);
		}
		String s_rotate_degrees = (String) attr.getValue("rotate");
		float rotate_degrees = 0;
		if (s_rotate_degrees != null) {
			rotate_degrees = MotoMathUtil.parseFloat(s_rotate_degrees);
		}

		boolean rotatePivotSet = false;
		String s_rotate_px = (String) attr.getValue("rotate_px");
		float rotate_px = 0;
		if (s_rotate_px != null) {
			rotate_px = MotoMathUtil.parseFloatNormal(s_rotate_px, ds2PxScale);
			rotatePivotSet = true;
		}
		String s_rotate_py = (String) attr.getValue("rotate_py");
		float rotate_py = 0;
		if (s_rotate_py != null) {
			rotate_py = MotoMathUtil.parseFloatNormal(s_rotate_py, ds2PxScale);
			rotatePivotSet = true;
		}


		boolean scalePivotSet = false;
		String s_scale_px = (String) attr.getValue("scale_px");
		float scale_px = 0;
		if (s_scale_px != null) {
			scale_px = MotoMathUtil.parseFloatNormal(s_scale_px, ds2PxScale);
			scalePivotSet = true;
		}
		
		String s_scale_py = (String) attr.getValue("scale_py");
		float scale_py = 0;
		if (s_scale_py != null) {
			scale_py = MotoMathUtil.parseFloatNormal(s_scale_py, ds2PxScale);
			scalePivotSet = true;
		}

		String s_rotate_x = (String) attr.getValue("rotate_x");
		float rotate_x = 0;
		if (s_rotate_x != null) {
			rotate_x = MotoMathUtil.parseFloat(s_rotate_x);
		}
		boolean rotateXPivotExplicitlySet = false;
		String s_rotatex_py = (String) attr.getValue("rotate_x_py");
		float rotatex_py = 0;
		if (s_rotatex_py != null) {
			rotatex_py = MotoMathUtil.parseFloatNormal(s_rotatex_py, ds2PxScale);
			rotateXPivotExplicitlySet = true;
		}

		String s_rotate_y = (String) attr.getValue("rotate_y");
		float rotate_y = 0;
		if (s_rotate_y != null) {
			rotate_y = MotoMathUtil.parseFloat(s_rotate_y);
		}
		boolean rotateYPivotExplicitlySet = false;
		String s_rotatey_px = (String) attr.getValue("rotate_y_px");
		float rotatey_px = 0;
		if (s_rotatey_px != null) {
			rotatey_px = MotoMathUtil.parseFloatNormal(s_rotatey_px, ds2PxScale);
			rotateYPivotExplicitlySet = true;
		}

		String s_scale_x = (String) attr.getValue("scale_x");
		float scale_x = 1;
		if (s_scale_x != null) {
			scale_x = MotoMathUtil.parseFloat(s_scale_x);
		}
		String s_scale_y = (String) attr.getValue("scale_y");
		float scale_y = 1;
		if (s_scale_y != null) {
			scale_y = MotoMathUtil.parseFloat(s_scale_y);
		}
		String s_visible = (String) attr.getValue("visible");
		boolean visible = true;
		if (s_visible != null) {
			visible = MotoMathUtil.parseBoolean(s_visible);
		}
		String s_rotatexy_offset_angle = (String) attr.getValue("rotatexy_offset_angle");
		float rotatexy_offset_angle = 0;
		if (s_rotatexy_offset_angle != null) {
			rotatexy_offset_angle = MotoMathUtil.parseFloat(s_rotatexy_offset_angle);
		}
		
		String s_bitmap = (String) attr.getValue("bitmap");
		if (s_bitmap != null) {
			int bitmapId = MotoMathUtil.parseInt(s_bitmap);
			mPhysicalStatus.setTexture(resInfo.mLoader.loadTexture(bitmapId));
		}
		int pictureWidth = mPhysicalStatus.getWidth();
		int pictureHeight = mPhysicalStatus.getHeight();
		resInfo.mDebugInfo.mObjectArea += pictureWidth * pictureHeight
				* scale_x * scale_y;
		
		String s_animation_set = (String) attr.getValue("animation_set");
		MotoAnimationSet animationSet = null;
		int animation_set = 0;
		if (s_animation_set != null) {
			animation_set = MotoMathUtil.parseInt(s_animation_set);
			MotoComponentBuilder builder = resInfo.mAnimationSetsBuilders.get(animation_set);
			if (builder != null) {
				try {
					animationSet = (MotoAnimationSet) builder.build(resInfo);
				} catch (Exception e) {
					Log.e(TAG, "build MotoAnimationSet failure, ID="
							+ s_animation_set, e);
				}
			}
		}
		String s_interaction_set = (String) attr.getValue("interaction_set");
		MotoInteractionSet interactionSet = null;
		int interaction_set = 0;
		if (s_interaction_set != null) {
			interaction_set = MotoMathUtil.parseInt(s_interaction_set);
			MotoComponentBuilder builder = resInfo.mInteractionSetsBuilders.get(interaction_set);
			if (builder != null) {
				try {
					interactionSet = (MotoInteractionSet) builder
							.build(resInfo);
				} catch (Exception e) {
					Log.e(TAG, "build MotoInteractionSet failure, ID="
							+ s_interaction_set, e);
				}
			}
		}

		/*
		 * String s_instance = (String) attr.getValue("instance"); int instance
		 * = 0; if (s_instance != null) { instance =
		 * MotoMathUtil.parseInt(s_instance); }
		 */
		MotoPictureInfo pictureInfo = new MotoPictureInfo(x, y, z, level,
				alpha, rotate_degrees, rotate_px, rotate_py, scale_x, scale_y,
				visible, rotate_x, rotate_y, rotatey_px, rotatex_py, 
				rotatePivotSet,  rotateXPivotExplicitlySet,
				rotateYPivotExplicitlySet);
		pictureInfo.mScalePx = scale_px;
		pictureInfo.mScalePy = scale_py;
		pictureInfo.mScalePivotExplicitlySet = scalePivotSet;
		
		pictureInfo.mRotateXYOffsetAngle = rotatexy_offset_angle;
		pictureInfo.mWidth = pictureWidth;
		pictureInfo.mHeight = pictureHeight;

		mPictureInitStatus = pictureInfo;
		mPictureCurrentStatus = pictureInfo.clone();
		mAnimationSet = animationSet;
		mInteractionSet = interactionSet;
		mPhysicalStatus.mPicture = this;
		mPhysicalStatus.mRect = mCurrentRect;
		mPhysicalStatus.mLevel = pictureInfo.getLevel();
		mRectToBeMappedFloat.set(0, 0, mPictureInitStatus.getWidth(), mPictureInitStatus.getHeight());
		
		mCurrentRect.set(0, 0, mPictureCurrentStatus.mWidth, mPictureCurrentStatus.mHeight);
		mFormerRect.set(mCurrentRect);

		if (mAnimationSet != null) {
			mAnimationSet.start();
		}

		String s_seek_animation_to = (String) attr.getValue("seek_animation_to");
		float seek_animation_to = 0;
		if (s_seek_animation_to != null) {
			seek_animation_to = MotoMathUtil.parseFloat(s_seek_animation_to);
			seekAnimationTo(seek_animation_to);
		}
	}

	public MotoPicture(MotoPictureInfo pictureInfo, int id) {
		this(pictureInfo, id, null, null);
	}

	public MotoPicture(MotoPictureInfo pictureInfo, int id,
			MotoAnimationSet animationSet, MotoInteractionSet interactionSet) {
		mPictureInitStatus = pictureInfo;
		
		//modify for NullPointer Verification by owen 2012.05.15
		mPictureCurrentStatus = (null == pictureInfo) ? null : pictureInfo
				.clone();
		mAnimationSet = animationSet;
		mInteractionSet = interactionSet;
		mId = id;
		mPhysicalStatus.mPicture = this;
		mPhysicalStatus.mRect = mCurrentRect;
		mPhysicalStatus.mLevel = pictureInfo.getLevel();
		mRectToBeMappedFloat.set(
				0,
				0,
				(null == mPictureInitStatus) ? 0 : mPictureInitStatus
						.getWidth(),
				(null == mPictureInitStatus) ? 0 : mPictureInitStatus
						.getHeight());
		mCurrentRect.set(0, 0, (null == mPictureCurrentStatus) ? 0
				: mPictureCurrentStatus.mWidth,
				(null == mPictureCurrentStatus) ? 0
						: mPictureCurrentStatus.mHeight);
		mFormerRect.set(mCurrentRect);
	}

	public void setView(View view) {
		mPhysicalStatus.setView(view);
		int width = (view != null) ? view.getWidth() : 0;
		int height = (view != null) ? view.getHeight() : 0;
		mPictureInitStatus.setWidth(width);
		mPictureInitStatus.setHeight(height);
		mPictureCurrentStatus.setWidth(width);
		mPictureCurrentStatus.setHeight(height);

		invalidate();
	}

	public View getView() {
		return mPhysicalStatus.getView();
	}

	public void setBitmap(Bitmap bitmap) {
		mPhysicalStatus.mView = null;
		int width = (bitmap != null) ? bitmap.getWidth() : 0;
		int height = (bitmap != null) ? bitmap.getHeight() : 0;
		setBitmap(bitmap, 0, 0, width, height);
	}
	
	/**
	 * Set a large bitmap to this picture, 
	 * but only some area of this bitmap is useful.
	 * The area not in the bound specified by {@link leftInBitmap}, 
	 * {@link topInBitmap}, {@link widthUsed}, {@link heightUsed} will not appear.
	 * <br>
	 * This interface is only available in GL Render Player. 
	 * <br>
	 * Use {@link get} to get which area is used.
	 * 
	 * @param bitmap
	 * @param leftInBitmap
	 * @param topInBitmap
	 * @param widthUsed
	 * @param heightUsed
	 */
	public void setBitmap(Bitmap bitmap, int leftInBitmap, int topInBitmap,
			int widthUsed, int heightUsed) {
		mPhysicalStatus.setBitmap(bitmap, 
				leftInBitmap, topInBitmap, widthUsed, heightUsed);
		
		mRectToBeMappedFloat.set(0, 0, widthUsed, heightUsed);
		mPictureInitStatus.setWidth(widthUsed);
		mPictureInitStatus.setHeight(heightUsed);
		mPictureCurrentStatus.setWidth(widthUsed);
		mPictureCurrentStatus.setHeight(heightUsed);
		
		invalidate();
	}
	
	public void invalidate() {
		mDirty = true;
	}

	public Bitmap getBitmap() {
		return mPhysicalStatus.getBitmap();
	}
	
	public Rect getBoundInBitmap() {
		return mPhysicalStatus.getBoundInBitmap();
	}

	public void seekAnimationTo(float progress) {
		if (mAnimationSet != null) {
			// delete at 2012.04.17
			// if (mAnimationSet.getStatus() ==
			// MotoAnimation.STATUS_NOT_STARTED) {
			// mAnimationSet.start();
			// }
			mAnimationSet.seekToProgress(progress, mProperties);
		}
	}

	protected void seekAnimationToTime(int time) {
		if (mAnimationSet != null) {
			mAnimationSet.seekToTime(time, mProperties);
		}
	}

	public boolean isPictureLeaf() {
		return true;
	}
	
	public void setParent(MotoPictureGroup parent) {
		if (mParent != null) {
			MotoPicture root = mParent.findRoot();
			if (root != null) {
				MotoRenderPlayer player = root.getPlayer();
				if (player != null) {
					player.mInnerBitmapChanged = true;
				}
			}
		} else {
			if (mPlayer != null) {
				mPlayer.mInnerBitmapChanged = true;
			}
		}
		
		mParent = parent;
	}

	public MotoPictureGroup getParent() {
		return mParent;
	}

	public void setId(int id) {
		mId = id;
	}

	public int getId() {
		return mId;
	}

	public MotoPictureInfo getCurrentPictureInfo() {
		return mPictureCurrentStatus;
	}
	
	/**
	 * @hide
	 * Internal used.
	 * @return
	 */
	public MotoPaintable getPaintable() {
		return mPhysicalStatus;
	}

	/*
	public MotoPictureInfo getInitPictureInfo() {
		return mPictureInitStatus;
	}
	*/

	public void setAnimationSet(MotoAnimationSet animaitonSet) {
		mPictureInitStatus.set(mPictureCurrentStatus);
		mAnimationSet = animaitonSet;
	}

	public MotoAnimationSet getAnimationSet() {
		return mAnimationSet;
	}

	public void setInteractionSet(MotoInteractionSet interactionSet) {
		mInteractionSet = interactionSet;
	}

	public MotoInteractionSet getInteractionSet() {
		return mInteractionSet;
	}

	public MotoPicture findPictureById(int id) {
		return ((getId() == id) ? this : null);
	}

	/**
	 * Don't dispatch event to parent or children here.
	 * 
	 * @param event
	 *            : event to be handled.
	 * @return True if intercepted, false if not.
	 */
	public boolean handleEvnet(MotoInteractionEvent event, boolean isProvioursIntercepted) {
		//add for don't handle event while the picture is invisible by owen 2012.05.15
		if (!getVisible())
			return false;
		
		if (mEventHandler != null && event != null && event.mEvent != null) {
			if (mEventHandler.onHandleEvent(event)) {
				return true;
			}
		}

		boolean intercept = false;
		if (mInteractionSet != null) {
			if (mInteractionSet.isInterceptEvent(event, isProvioursIntercepted)) {
				intercept = true;
			}
			ArrayList<MotoInteraction> handleInteractions = mInteractionSet.handleEventInteractions(event, isProvioursIntercepted);
			int size = handleInteractions.size();
			if (size > 0) {
				for (int i = 0; i < size; i++) {
					MotoInteraction interaction = handleInteractions.get(i);
					if (interaction != null) {
						interaction.handleEvent(event, mProperties, this);
					}
				}
			}
		}
		return intercept;
	}

	/* package */void addInteractionAnimation(MotoAnimationSet animationSet) {
		if (mInteractionAnimations.contains(animationSet)) {
			animationSet.reset();
		} else {
			mInteractionAnimations.add(animationSet);
		}
	}

	/* package */void removeIneractionAnimation(MotoAnimationSet animationSet) {
		mInteractionAnimations.remove(animationSet);
	}

	/* package */void removeIneractionAnimationAt(int index) {
		mInteractionAnimations.remove(index);
	}

	public void setEventHandler(MotoEventHandler handler) {
		mEventHandler = handler;
	}

	public MotoEventHandler getEventHandler() {
		return mEventHandler;
	}


	/**
	 * Don't modify the return rectangle.
	 * 
	 * @return
	 */
	public Rect getCurrentRect() {
		return mCurrentRect;
	}

	/**
	 * Don't modify the return rectangle.
	 * 
	 * @return
	 */
	public Rect getFormerRect() {
		return mFormerRect;
	}

	/**
	 * Run on UI thread. Calculate the pciture's translate_x, translate_y, and
	 * other 2d information here, and alpha. But not calculate the matrix here.
	 * Prepare the picture itself information. Not multiply with its parent.
	 * 
	 * @param interactionInput
	 */
	protected void nextFrame(int time, PlayerConfigInfo playerConfig) {
		// long time = SystemClock.uptimeMillis();
		for (int i = 0; i < mInteractionAnimations.size(); i++) {
			MotoAnimationSet animationSet = mInteractionAnimations.get(i);
			if (animationSet != null && animationSet.hasNext() && animationSet.isGoing()) {
				animationSet.next(time, mProperties);
			}
			if (animationSet.getStatus() == MotoAnimation.STATUS_STOPED) {
				mInteractionAnimations.remove(i);
			}
		}

		if (!mBlockAnimation) {
			if (mAnimationSet != null && mAnimationSet.hasNext() && mAnimationSet.isGoing()) {
				mAnimationSet.next(time, mProperties);
			}
		}
		
		if (mProperties.mHasChanged) {
            // reset flag
            playerConfig.mHasFlushed = true;
            mProperties.mHasChanged = false;
            
            MotoPictureInfo.addPictureInfo(mPictureInitStatus, mProperties,
                    mPictureCurrentStatus);
        }

        if (mDirty) {
            playerConfig.mHasFlushed = true;
        }
		// Log.d(TAG, "next frame time = " + (SystemClock.uptimeMillis() -
		// time));
	}

	/**
	 * Run on UI thread.
	 * Multiply its own matrix, alpha, visible... with its parent, then add the picture's paintable to paintablelist.
	 * dirty rect control is needed.
	 * If the picture binds a view, set view's properties here and don't draw the view by 2d engine.
	 * change checked is needed before set the view's properties, 
	 * @param paintableList
	 * @param parentPaintable
	 * @param dirtyRects: output of dirty rectangle array, could be null if you don't want to enable this function.
	 */
	protected void renderFrame(MotoLinkedList<MotoPaintable> paintableList,
			MotoPaintable parentPaintable, ArrayList<Rect> dirtyRects, 
			PlayerConfigInfo playerConfig) {
		// calculate physical status.

		// matrix: use matrix multiple
		Matrix matrix = mPhysicalStatus.mMatrix;
		mCurrentMatrix.set(matrix);
		matrix.set(parentPaintable.mMatrix);

		if (mPictureCurrentStatus.getX() != 0
				|| mPictureCurrentStatus.getY() != 0) {
			matrix.preTranslate(mPictureCurrentStatus.getX(),
					mPictureCurrentStatus.getY());
		}
		if (mPictureCurrentStatus.getRotate() != 0) {
			matrix.preRotate(mPictureCurrentStatus.getRotate(),
					mPictureCurrentStatus.getRotatePx(),
					mPictureCurrentStatus.getRotatePy());
		}
		if (mPictureCurrentStatus.getScaleX() != 1
				|| mPictureCurrentStatus.getScaleY() != 1) {
			matrix.preScale(mPictureCurrentStatus.getScaleX(),
					mPictureCurrentStatus.getScaleY(),
					mPictureCurrentStatus.getScalePx(),
					mPictureCurrentStatus.getScalePy());
		}

		if (mPictureCurrentStatus.getRotateY() != 0
				|| mPictureCurrentStatus.getRotateX() != 0
				|| mPictureCurrentStatus.getZ() != 0) {
			MATRIX_3D.reset();
			CAMERA.save();
			if (mPictureCurrentStatus.getZ() != 0)
				CAMERA.translate(0, 0, mPictureCurrentStatus.getZ());

			if (mPictureCurrentStatus.getRotateY() != 0
					|| mPictureCurrentStatus.getRotateX() != 0) {
				CAMERA.rotate(mPictureCurrentStatus.getRotateX(),
						mPictureCurrentStatus.getRotateY(), 0f);
			}

			CAMERA.getMatrix(MATRIX_3D);
			CAMERA.restore();

			float rotateXPivot = mPictureCurrentStatus.getRotateXPy();
			float rotateYPivot = mPictureCurrentStatus.getRotateYPx();
			MATRIX_3D.preTranslate(-rotateYPivot, -rotateXPivot);
			MATRIX_3D.postTranslate(rotateYPivot, rotateXPivot);
			
			if (mPictureCurrentStatus.mRotateXYOffsetAngle != 0) {
				float halfWidth = mPictureCurrentStatus.mWidth / 2f;
				float halfHeight = mPictureCurrentStatus.mHeight / 2f;
				MATRIX_3D.postRotate(mPictureCurrentStatus.mRotateXYOffsetAngle, halfWidth, halfHeight);
				MATRIX_3D.preRotate(-mPictureCurrentStatus.mRotateXYOffsetAngle, halfWidth, halfHeight);
			}
			
			matrix.preConcat(MATRIX_3D);
		}

		// alpha: multiple with its parent.
		int myOldAlpha = mPhysicalStatus.mAlpha;
		int parentAlpha = parentPaintable.mAlpha;
		int myCurrentAlpha = (int) (mPictureCurrentStatus.getAlpha()
				* parentAlpha / 255);
		mPhysicalStatus.mAlpha = myCurrentAlpha;

		// visible: only visible when its parent and itself is visible.
		boolean visible = mPhysicalStatus.mVisible;
		if (parentPaintable.mVisible && mPictureCurrentStatus.getVisible()) {
			mPhysicalStatus.mVisible = true;
		} else {
			mPhysicalStatus.mVisible = false;
		}

		// Z order: add with its parent
		float myOldZ = mPhysicalStatus.mLevel;
		float myCurrentZ = mPictureCurrentStatus.getLevel()
				+ parentPaintable.mLevel;
		mPhysicalStatus.mLevel = myCurrentZ;

		// add it to paintableList.
		if (isPictureLeaf()) {
			if (mPhysicalStatus.hasBitmapOrTexture() 
					&& myCurrentAlpha != 0) {
				paintableList.orderAdd(mPhysicalStatus, mPhysicalStatus.mLevel);
			}
		}

		//calculate the dirty rectangle.
		if (playerConfig.mHitRectEnable || playerConfig.mDiryRectEnable) {
			if (isPictureLeaf()) {
				if (mCurrentMatrix.equals(matrix) && !mDirty) {
					// Log.d("owen","------------01");
					if (playerConfig.mDiryRectEnable) {
						if (myOldAlpha != myCurrentAlpha
								|| visible != mPhysicalStatus.mVisible
								|| myOldZ != myCurrentZ) {
							dirtyRects.add(mCurrentRect);
						}

					}

				} else {
					mCurrentMatrix.set(matrix);
					mFormerRect.set(mCurrentRect);
					matrix.mapRect(mCurrentRectFloat, mRectToBeMappedFloat);
					mCurrentRect.set(((int) mCurrentRectFloat.left) - 1,
							((int) mCurrentRectFloat.top) - 1,
							((int) mCurrentRectFloat.right + 1),
							((int) mCurrentRectFloat.bottom) + 1);
					// Log.d("owen", "mCurrentRect[" + mCurrentRect.toString());
					if (dirtyRects != null) {
						dirtyRects.add(mFormerRect);
						dirtyRects.add(mCurrentRect);
					}
					mDirty = false;
				}
			} else {
				mDirty = false;
			}
		} else {
			mDirty = false;
		}

		// if this picture bind a view.
		renderView();
	}

	private void renderView() {
		if (mPhysicalStatus.mView == null) {
			return;
		}

		View view = mPhysicalStatus.mView;
		
		float alpha = mPictureCurrentStatus.getAlpha() / 255;
		if (view.getAlpha() != alpha) {
			//view.setFastAlpha(alpha);
			view.setAlpha(alpha);
		}
		float x = mPictureCurrentStatus.getX();
		if (view.getX() != x) {
			//view.setFastX(x);
			view.setX(x);
		}
		float y = mPictureCurrentStatus.getY();
		if (view.getY() != y) {
			//view.setFastY(y);
			view.setY(y);
		}

		float rotate = mPictureCurrentStatus.getRotate();
		if (view.getRotation() != rotate) {
			view.setRotation(rotate);
		}
		float rotateX = mPictureCurrentStatus.getRotateX();
		if (view.getRotationX() != rotateX) {
			view.setRotationX(rotateX);
		}

		float rotateY = mPictureCurrentStatus.getRotateY();
		if (view.getRotationY() != rotateY) {
			//view.setFastRotationY(rotateY);
			view.setRotationY(rotateY);
		}
		float scaleX = mPictureCurrentStatus.getScaleX();
		if (view.getScaleX() != scaleX) {
			//view.setFastScaleX(scaleX);
			view.setScaleX(scaleX);
		}
		float scaleY = mPictureCurrentStatus.getScaleY();
		if (view.getScaleY() != scaleY) {
			//view.setFastScaleY(scaleY);
			view.setScaleY(scaleY);
		}
		boolean visible = mPictureCurrentStatus.getVisible();
		if ((view.getVisibility() == View.VISIBLE) != visible) {
			view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
		}
		
	}
	
	
	public MotoPicture findRoot() {
		MotoPicture parent = this;
		MotoPicture root = this;
		while (parent != null) {
			root = parent;
			parent = parent.getParent();
		}
		
		return root;
	}
	
	/**
	 * The player only exits in root picture.
	 * @return NULL if it's not a root picture or it's not bind to player.
	 */
	public MotoRenderPlayer getPlayer() {
		return mPlayer;
	}
	
	void setPlayer(MotoRenderPlayer player) {
		mPlayer = player;
	}
	
	
	/**
	 * set x position
	 * 
	 * @param x
	 */
	public void setX(float x) {
		mPictureCurrentStatus.setX(x);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_TRANSLATE_X, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get x position
	 * 
	 * @return float
	 */
	public float getX() {
		return mPictureCurrentStatus.getX();
	}

	/**
	 * set y position
	 * 
	 * @param y
	 */
	public void setY(float y) {
		mPictureCurrentStatus.setY(y);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_TRANSLATE_Y, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get y position
	 * 
	 * @return float
	 */
	public float getY() {
		return mPictureCurrentStatus.getY();
	}

	/**
	 * set z position
	 * 
	 * @param y
	 */
	public void setLevel(int level) {
		mPictureCurrentStatus.setLevel(level);
		mDirty = true;
	}

	/**
	 * get z layer
	 * 
	 * @return float
	 */
	public int getLevel() {
		return mPictureCurrentStatus.getLevel();
	}

	/**
	 * set alpha value
	 * 
	 * @param alpha
	 */
	public void setAlpha(float alpha) {
		mPictureCurrentStatus.setAlpha(alpha);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_ALPHA, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get alpha value of this pictureInfo
	 * 
	 * @return float
	 */
	public float getAlpha() {
		return mPictureCurrentStatus.getAlpha();
	}

	/**
	 * set rotate degree for this pictureInfo
	 * 
	 * @param rotate
	 */
	public void setRotate(float rotate) {
		mPictureCurrentStatus.setRotate(rotate);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get rotate degree value
	 * 
	 * @return float
	 */
	public float getRotate() {
		return mPictureCurrentStatus.getRotate();
	}

	/**
	 * set x position of rotation
	 * 
	 * @param rotatePx
	 */
	public void setRotatePx(float rotatePx) {
		mPictureCurrentStatus.setRotatePx(rotatePx);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_PX, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get x position of rotation
	 * 
	 * @return x
	 */
	public float getRotatePx() {
		return mPictureCurrentStatus.getRotatePx();
	}

	/**
	 * set y position of rotation
	 * 
	 * @param rotatePy
	 */
	public void setRotatePy(float rotatePy) {
		mPictureCurrentStatus.setRotatePy(rotatePy);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_PY, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get y position of rotation
	 * 
	 * @return y
	 */
	public float getRotatePy() {
		return mPictureCurrentStatus.getRotatePy();
	}
	
	/**
	 * set rotate degree value with the x-axis as the center for this picture
	 * 
	 * @param mRotateX
	 */
	public void setRotateX(float mRotateX) {
		mPictureCurrentStatus.setRotateX(mRotateX);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_X, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}
	
	/**
	 * get rotate degree value with the x-axis as the center
	 * 
	 * @return float
	 */
	public float getRotateX() {
		return mPictureCurrentStatus.getRotateX();
	}

	/**
	 * set rotate degree value with the y-axis as the center for this picture
	 * 
	 * @param mRotateY
	 */
	public void setRotateY(float mRotateY) {
		mPictureCurrentStatus.setRotateY(mRotateY);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_Y, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get rotate degree value with the y-axis as the center
	 * 
	 * @return
	 */
	public float getRotateY() {
		return mPictureCurrentStatus.getRotateY();
	}

	/**
	 * set rotate position with the y-axis as the center for this picture
	 * 
	 * @param mRotateXPy
	 */
	public void setRotateYPx(float rotateYPx) {
		mPictureCurrentStatus.setRotateYPx(rotateYPx);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_Y_PX, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get rotate position value with the y-axis as the center
	 * 
	 * @return
	 */
	public float getRotateYPx() {
		return mPictureCurrentStatus.getRotateYPx();
	}

	/**
	 * set rotate position with the x-axis as the center for this picture
	 * 
	 * @param mRotateYPx
	 */
	public void setRotateXPy(float rotateXPy) {
		mPictureCurrentStatus.setRotateXPy(rotateXPy);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_ROTATE_X_PY, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get rotate position with the x-axis as the center
	 * 
	 * @return
	 */
	public float getRotateXPy() {
		return mPictureCurrentStatus.getRotateXPy();
	}

	/**
	 * set z position
	 * 
	 * @param mZ
	 */
	public void setZ(float z) {
		mPictureCurrentStatus.setZ(z);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_TRANSLATE_Z, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get z position value
	 * 
	 * @return
	 */
	public float getZ() {
		return mPictureCurrentStatus.getZ();
	}
	
	/**
	 * set x position of scale
	 * 
	 * @param rotatePx
	 */
	public void setScalePx(float scalePx) {
		mPictureCurrentStatus.setScalePx(scalePx);
		mDirty = true;
	}

	/**
	 * get x position of scale
	 * 
	 * @return x
	 */
	public float getScalePx() {
		return mPictureCurrentStatus.getScalePx();
	}
	
	/**
	 * set y position of scale
	 * 
	 * @param rotatePx
	 */
	public void setScalePy(float scalePy) {
		mPictureCurrentStatus.setScalePy(scalePy);
		mDirty = true;
	}

	/**
	 * get x position of rotation
	 * 
	 * @return x
	 */
	public float getScalePy() {
		return mPictureCurrentStatus.getScalePy();
	}
	
	

	/**
	 * set x position of scale
	 * 
	 * @param scalePx
	 */
	public void setScaleX(float scalex) {
		mPictureCurrentStatus.setScaleX(scalex);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_SCALE_X, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get x position of scale
	 * 
	 * @return float
	 */
	public float getScaleX() {
		return mPictureCurrentStatus.getScaleX();
	}

	/**
	 * set y position of scale
	 * 
	 * @param scalePy
	 */
	public void setScaleY(float scaleY) {
		mPictureCurrentStatus.setScaleY(scaleY);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_SCALE_Y, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}

	/**
	 * get y position of scale
	 * 
	 * @return float
	 */
	public float getScaleY() {
		return mPictureCurrentStatus.getScaleY();
	}
	
	public void setRotateXYOffsetAngle(float rotateXYOffsetAngle) {
		mPictureCurrentStatus.setRotateXYOffsetAngle(rotateXYOffsetAngle);
		mDirty = true;
	}

	public float getRotateXYOffsetAngle() {
		return mPictureCurrentStatus.getRotateXYOffsetAngle();
	}
	
	/**
	 * set visibility
	 * 
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		mPictureCurrentStatus.setVisible(visible);
		mProperties.setPropertyAffectType(MotoObjectAnimation.ANIMATION_VISIBLE, MotoObjectAnimation.AFFECT_TYPE_NONE);
		mDirty = true;
	}
	
	/**
	 * get visibility
	 * 
	 * @return boolean
	 */
	public boolean getVisible() {
		return mPictureCurrentStatus.getVisible();
	}

	public int getWidth() {
		return mPictureCurrentStatus.getWidth();
	}
	
	public int getHeight() {
		return mPictureCurrentStatus.getHeight();
	}
}
