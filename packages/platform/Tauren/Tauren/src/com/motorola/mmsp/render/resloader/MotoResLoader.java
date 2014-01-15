package com.motorola.mmsp.render.resloader;

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import com.motorola.mmsp.render.MotoAnimationSet;
import com.motorola.mmsp.render.MotoAttributeArray;
import com.motorola.mmsp.render.MotoBitmapInfo;
import com.motorola.mmsp.render.MotoBitmapWrapper;
import com.motorola.mmsp.render.MotoConfig;
import com.motorola.mmsp.render.MotoKeyframeAnimation;
import com.motorola.mmsp.render.MotoLinearAnimation;
import com.motorola.mmsp.render.MotoPicture;
import com.motorola.mmsp.render.MotoPictureGroup;
import com.motorola.mmsp.render.MotoRenderPlayer;
import com.motorola.mmsp.render.MotoTextureInfo;
import com.motorola.mmsp.render.util.MotoIntEtypeHashMap;
import com.motorola.mmsp.render.util.MotoMathUtil;


public final class MotoResLoader {
	private static final String TAG = "MotoRenderPlayer";
	
	private ResOutputInfo mResLoaded;
	private Resources mRes;
	static final MotoAttributeArray CLASS_MAP = new MotoAttributeArray(
			4);
	
	static {
		CLASS_MAP.add("MotoAnimationSet", MotoAnimationSet.class);
		CLASS_MAP.add("MotoLinearAnimation", MotoLinearAnimation.class);
		CLASS_MAP.add("MotoPictureGroup", MotoPictureGroup.class);
		CLASS_MAP.add("MotoPicture", MotoPicture.class);
	}
	
	public MotoResLoader(Resources res, ResOutputInfo output) {
		mResLoaded = output;
		mResLoaded.mLoader = this;
		mRes = res;
	}
	
	final static MotoAttributeArray getAtrribute(XmlPullParser parser, ResOutputInfo resInfo) {
		MotoAttributeArray attr = new MotoAttributeArray(50);
		int attributeCount = parser.getAttributeCount();
		for (int i=0; i<attributeCount; i++) {
			String attName = parser.getAttributeName(i);
			String value = parser.getAttributeValue(i);
			if (attName == null || attName.length() <= 0) {
				continue;
			}
			attr.add(attName, value);
		}
		//attr.add("frame_time", String.valueOf(resInfo.mPlayConfigInfo.mFrameTime));
		return attr;
	}
	
	@SuppressWarnings("rawtypes")
	final static Class loadClass(String className)
			throws ClassNotFoundException {
		Object obj = CLASS_MAP.getValue(className);
		if (null != obj) {
			return (Class) obj;
		}

		Class clz;
		if (className.indexOf('.') < 0) {
			clz = Class.forName("com.motorola.mmsp.render." + className);
		} else {
			clz = Class.forName(className);
		}
		return clz;
	}
	
	final static MotoComponentBuilder
		loadComponentBuilder(XmlPullParser parser, ResOutputInfo resInfo, String className, int type) 
		throws ClassNotFoundException {
		
		MotoAttributeArray attr = getAtrribute(parser, resInfo);
		MotoComponentBuilder builder = new MotoComponentBuilder();
		builder.mAttr = attr;
		if(null == builder.mClass)
		{
			builder.mClass = loadClass(className);
		}
		String instance = (String)attr.getValue("instance");
		if (instance != null) {
			builder.mInstanceCount = MotoMathUtil.parseInt(instance);
		}
		builder.mType = type;
		String id = (String) attr.getValue("id");
		if (id != null) {
			builder.mId = MotoMathUtil.parseInt(id);
		}
		return builder;
	}
	
	final static MotoComponentParentBuilder 
		loadComponentParentBuilder (XmlPullParser parser, ResOutputInfo resInfo, String className, int type, int capChildCount) 
		throws ClassNotFoundException {
		
		MotoAttributeArray attr = getAtrribute(parser, resInfo);
		MotoComponentParentBuilder builder = new MotoComponentParentBuilder(capChildCount);
		builder.mAttr = attr;
		builder.mClass = loadClass(className);
		String instance = (String)attr.getValue("instance");
		if (instance != null) {
			builder.mInstanceCount = MotoMathUtil.parseInt(instance);
		}
		builder.mType = type;
		String id = (String) attr.getValue("id");
		if (id != null) {
			builder.mId = MotoMathUtil.parseInt(id);
		}
		return builder;
	}
	
	public final MotoBitmapInfo loadBitmap(int id) {
		long time = SystemClock.uptimeMillis();
		MotoBitmapInfo bmpInfo = mResLoaded.mBitmapMap.get(id);
		if (bmpInfo != null 
				&& bmpInfo.mBitmap != null 
				&& !bmpInfo.mBitmap.isRecycled()) {
			return bmpInfo;
		}
		
		Bitmap bitmap = MotoBitmapWrapper.decodeBitmap(mRes, id);
		if (bitmap != null) {
			if (bmpInfo == null) {
				bmpInfo = new MotoBitmapInfo();
			}
			bmpInfo.setBitmap(id, bitmap);
			mResLoaded.mBitmapMap.put(id, bmpInfo);
			mResLoaded.mDebugInfo.mLoadBmpTime += (SystemClock.uptimeMillis() - time);
			if (bitmap != null) {
				mResLoaded.mDebugInfo.mBmpRAM += bitmap.getByteCount();
				mResLoaded.mDebugInfo.mBmpArea += bitmap.getWidth() * bitmap.getHeight();
			}
		}
		
		return bmpInfo;
	}
	
	public final void reloadBitmap(MotoBitmapInfo bitmapInfo) {
		if (bitmapInfo == null) {
			return;
		}
		
		bitmapInfo.mBitmap = MotoBitmapWrapper.reload(mRes, bitmapInfo.mId);
	}
	
	public final MotoTextureInfo loadTexture(int id) {
		MotoTextureInfo textureInfo = mResLoaded.mTextureInfoMap.get(id);
		if (textureInfo != null) {
			return textureInfo;
		}
		
		MotoBitmapInfo bitmapInfo = loadBitmap(id);
		if (bitmapInfo != null) {
			textureInfo = new MotoTextureInfo();
			textureInfo.set(id, bitmapInfo);
			mResLoaded.mTextureInfoMap.put(id, textureInfo);
			return textureInfo;
		}
		
		return null;
	}
	
	public final void reloadPreloadBitmaps() {
		MotoIntEtypeHashMap<MotoBitmapInfo> prelaodBitmap = mResLoaded.mBitmapMap;
		for (int i=0, preloadBmpSize=prelaodBitmap.size(); 
				i < preloadBmpSize; i++) {
			MotoBitmapInfo bitmapInfo = prelaodBitmap.valueAt(i);
			reloadBitmap(bitmapInfo);
		}
	}
	
	public final void recyclePreloadBitmaps() {
		MotoIntEtypeHashMap<MotoBitmapInfo> prelaodBitmap = mResLoaded.mBitmapMap;
		for (int i=0, preloadBmpSize=prelaodBitmap.size(); 
				i < preloadBmpSize; i++) {
			MotoBitmapInfo bitmapInfo = prelaodBitmap.valueAt(i);
			if (bitmapInfo != null) {
				MotoBitmapWrapper.recycleForce(bitmapInfo.mId);
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final void loadRes(int configId) {
		long buildTime = 0;
		long time = SystemClock.uptimeMillis();
		XmlPullParser parser = mRes.getXml(configId);
		MotoResBuilderStack stack = new MotoResBuilderStack(100);
		stack.push(new MotoComponentBuilder());
		try {
			int type = 0;
			while((type = parser.next()) != XmlPullParser.END_DOCUMENT) {
				if (type == XmlPullParser.START_TAG) {
					String name = parser.getName();
					if (name != null) {
						if (name.equals("InteractionTag")) {
							MotoComponentBuilder interactionTag = new MotoComponentBuilder();
							interactionTag.mType = MotoComponentBuilder.TYPE_INTERACTION_TAG;
							stack.push(interactionTag);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_INTERACTION_TAG) {
							MotoComponentParentBuilder interactionSetBuilder 
								= loadComponentParentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_INTERACTION_SET, 10);
							stack.push(interactionSetBuilder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_INTERACTION_SET) {
							MotoComponentBuilder interactionBuilder = loadComponentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_INTERACTION);
							stack.push(interactionBuilder);
						} else if (name.equals("AnimationTag")) {
							MotoComponentBuilder builder = new MotoComponentBuilder();
							builder.mType = MotoComponentBuilder.TYPE_ANIMATION_TAG;
							stack.push(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_ANIMATION_TAG) {
							MotoComponentParentBuilder builder 
								= loadComponentParentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_ANIMATION_SET, 10);
							stack.push(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_ANIMATION_SET) {
							Class clz = loadClass(name);
							MotoComponentBuilder builder;
						    /* boolean isKeyframe =false;
							boolean isSet = MotoAnimationSet.class.isAssignableFrom(clz);
							if(!isSet)
							{
								isKeyframe = MotoKeyframeAnimation.class.isAssignableFrom(clz);
							}
							
							try {
								if (MotoAnimationSet.class.asSubclass(clz) != null) {
									isSet = true;
								}
								
							} catch (Exception e) {
								try {
									if (MotoKeyframeAnimation.class
											.asSubclass(clz) != null) {
										isKeyframe = true;
									}
								} catch (Exception se) {

								}
							}*/
							
							if (MotoAnimationSet.class.isAssignableFrom(clz)) {
								builder = loadComponentParentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_ANIMATION_SET, 5);
							}else if(MotoKeyframeAnimation.class.isAssignableFrom(clz)){
								builder = loadComponentParentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_KEYFRAME_ANIMATION, 1);
							} else {
								builder = loadComponentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_ANIMATION);
							}
							builder.setClass(clz);
							stack.push(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_KEYFRAME_ANIMATION) {
							MotoComponentParentBuilder builder = loadComponentParentBuilder(
									parser, mResLoaded, name,
									MotoComponentBuilder.TYPE_KEYFRAME_SET, 5);
							stack.push(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_KEYFRAME_SET) {
							MotoComponentBuilder builder = loadComponentBuilder(
									parser, mResLoaded, name,
									MotoComponentBuilder.TYPE_KEYFRAME);
							stack.push(builder);
						} else if (name.equals("PictureTag")) {
							MotoComponentBuilder builder = new MotoComponentBuilder();
							builder.mType = MotoComponentBuilder.TYPE_PICTURE_TAG;
							stack.push(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_PICTURE_TAG) {
							MotoComponentParentBuilder builder 
								= loadComponentParentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_PICTURE_SET, 50);
							stack.push(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_PICTURE_SET) {
							Class clz = loadClass(name);
							MotoComponentBuilder builder;
							/*boolean isGroup = false;
							try {
								if (clz.asSubclass(MotoPictureGroup.class) != null) {
									isGroup = true;
								}
							} catch (Exception e) {
								
							}*/
							if (MotoPictureGroup.class.isAssignableFrom(clz)) {
								builder = loadComponentParentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_PICTURE_SET, 30);
							} else {
								builder = loadComponentBuilder(parser, mResLoaded, name, MotoComponentBuilder.TYPE_PICTURE);
							}
							builder.setClass(clz);
							stack.push(builder);
						} else if (name.startsWith("PlayerConfigInfo")) {
							loadPlayerConfigInfo(parser, mResLoaded);
						} else if (name.endsWith("Texture")) {
							loadTexture(parser, mResLoaded);
						}
					}
				} else if (type == XmlPullParser.END_TAG) {
					String name = parser.getName();
					if (name != null) {
						if (stack.getLastType() == MotoComponentBuilder.TYPE_INTERACTION) {
							MotoComponentBuilder interactionBuilder = stack.pop();
							MotoComponentParentBuilder parent = (MotoComponentParentBuilder)stack.getLast();
							parent.add(interactionBuilder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_INTERACTION_SET) {
							MotoComponentParentBuilder set = (MotoComponentParentBuilder)stack.pop();
							mResLoaded.mInteractionSetsBuilders.put(set.mId, set);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_INTERACTION_TAG) {
							stack.pop();
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_KEYFRAME) {
							MotoComponentBuilder builder = stack.pop();
							MotoComponentParentBuilder parent = (MotoComponentParentBuilder)stack.getLast();
							parent.add(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_KEYFRAME_SET) {
							MotoComponentParentBuilder set = (MotoComponentParentBuilder)stack.pop();
							MotoComponentParentBuilder parent = (MotoComponentParentBuilder)stack.getLast();
							parent.add(set);
						}else if (stack.getLastType() == MotoComponentBuilder.TYPE_ANIMATION || 
								stack.getLastType() == MotoComponentBuilder.TYPE_KEYFRAME_ANIMATION) {
							MotoComponentBuilder builder = stack.pop();
							MotoComponentParentBuilder parent = (MotoComponentParentBuilder)stack.getLast();
							parent.add(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_ANIMATION_SET) {
							MotoComponentParentBuilder set = (MotoComponentParentBuilder)stack.pop();
							if (stack.getLastType() == MotoComponentBuilder.TYPE_ANIMATION_SET) {
								MotoComponentParentBuilder setParent = (MotoComponentParentBuilder)stack.getLast();
								setParent.add(set);
							} else {
								mResLoaded.mAnimationSetsBuilders.put(set.mId, set);
							}
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_ANIMATION_TAG) {
							stack.pop();
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_PICTURE) {
							MotoComponentBuilder builder = stack.pop();
							MotoComponentParentBuilder parent = (MotoComponentParentBuilder)stack.getLast();
							parent.add(builder);
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_PICTURE_SET) {
							MotoComponentParentBuilder set = (MotoComponentParentBuilder)stack.pop();
							if (stack.getLastType() == MotoComponentBuilder.TYPE_PICTURE_SET) {
								MotoComponentParentBuilder setParent = (MotoComponentParentBuilder)stack.getLast();
								setParent.add(set);
							} else {
								mResLoaded.mRootPictureBuilder = set;
							}
						} else if (stack.getLastType() == MotoComponentBuilder.TYPE_PICTURE_TAG) {
							stack.pop();
						} 
					}
				}
				
			}
			long time0 = SystemClock.uptimeMillis();
			ArrayList<MotoPicture> pictures = (ArrayList<MotoPicture>)mResLoaded.mRootPictureBuilder.build(mResLoaded);
			if (pictures != null && pictures.size() > 0) {
				mResLoaded.mPicture = pictures.get(0);
			}
			buildTime = SystemClock.uptimeMillis() - time0;

			
		} catch (Exception e) {
			e.printStackTrace();
		} /*catch (IOException e) {
			e.printStackTrace();
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} finally {
			
		}*/
		mResLoaded.mDebugInfo.mLoadResTime = SystemClock.uptimeMillis() - time;
		if (MotoConfig.DEBUG) {
			float fps = mResLoaded.mPlayConfigInfo.mFrameTime != 0 
					? 1000 / mResLoaded.mPlayConfigInfo.mFrameTime
					: 0;
			Log.d(TAG, "fps is " + fps);
			Log.d(TAG, "load res time = " + mResLoaded.mDebugInfo.mLoadResTime + ", build object time = "+ buildTime
					+ ", load bitmap time = " + mResLoaded.mDebugInfo.mLoadBmpTime
					+ ", bitmap byte count = " + (float)mResLoaded.mDebugInfo.mBmpRAM / (1204 * 1024) + "M");
			String s = "bitmap count = " + mResLoaded.mBitmapMap.size()
					+ ", area is " 
					+ ((float)mResLoaded.mDebugInfo.mBmpArea / (1024 * 1024)) + "M, "
					+ ((float)mResLoaded.mDebugInfo.mBmpArea / (480 * 854)) + " of IRM, "
					+ ((float)mResLoaded.mDebugInfo.mBmpArea / (1280 * 720)) + " of Galaxy"
					+ ", texture count = " + mResLoaded.mTextureInfoMap.size();
			if ((float)mResLoaded.mDebugInfo.mBmpArea / (480 * 854) > 3) {
				Log.e(TAG, s);
				Log.e(TAG, "Bitmaps area is more than 3 !!!!");
			} else {
				Log.d(TAG, s);
			}
			s = "objects count " + mResLoaded.mDebugInfo.mObjectCount 
					+ ", area is " 
					+ ((float)mResLoaded.mDebugInfo.mObjectArea / (1024 * 1024)) + "M, "
					+ ((float)mResLoaded.mDebugInfo.mObjectArea / (480 * 854)) + " of IRM, "
					+ ((float)mResLoaded.mDebugInfo.mObjectArea / (1280 * 720)) + " of Galaxy";
			if ((float)mResLoaded.mDebugInfo.mObjectArea / (480 * 854) > 5) {
				Log.e(TAG, s);
				Log.e(TAG, "Objects area is more than 5 !!!!");
			} else {
				Log.d(TAG, s);
			}
		}
	}
	
	
	private static final PlayerConfigInfo loadPlayerConfigInfo(XmlPullParser parser, ResOutputInfo resInfo) {
		PlayerConfigInfo info = resInfo.mPlayConfigInfo;
		int attributeCount = parser.getAttributeCount();
		for (int i=0; i<attributeCount; i++) {
			String attName = parser.getAttributeName(i);
			String value = parser.getAttributeValue(i);
			if (attName == null || attName.length() <= 0 || value == null) {
				continue;
			}
			
			if (attName.equals("frame_time")) {
				info.mFrameTime = MotoMathUtil.parseInt(value);
			} else if (attName.equals("fps")) {
				info.mFrameTime = (int) (1000 / MotoMathUtil.parseFloat(value));
			} else if (attName.equals("based_on_density")) {
				final float basedOnDensity = MotoMathUtil.parseFloat(value);
				final float currentDensity = resInfo.mLoader.mRes
						.getDisplayMetrics().density;
				info.mDs2PxScale = currentDensity / basedOnDensity;
			}
		}
		return info;
	}
	
	private final MotoTextureInfo loadTexture(XmlPullParser parser, ResOutputInfo resInfo) {
		MotoTextureInfo texture = new MotoTextureInfo();
		
		int id = 0;
		MotoBitmapInfo bitmapInfo = null;
		int x=0, y=0, width=0, height=0;
		int attributeCount = parser.getAttributeCount();
		for (int i=0; i<attributeCount; i++) {
			String attName = parser.getAttributeName(i);
			String value = parser.getAttributeValue(i);
			if (attName == null || attName.length() <= 0 || value == null) {
				continue;
			}
			
			if (attName.equals("id")) {
				id = MotoMathUtil.parseInt(value);
			} else if (attName.equals("bitmap")) {
				int bitmapId = MotoMathUtil.parseInt(value);
				bitmapInfo = loadBitmap(bitmapId);
			} else if (attName.equals("x")) {
				x = MotoMathUtil.parseInt(value);
			} else if (attName.equals("y")) {
				y = MotoMathUtil.parseInt(value);
			} else if (attName.equals("width")) {
				width = MotoMathUtil.parseInt(value);
			} else if (attName.equals("height")) {
				height = MotoMathUtil.parseInt(value);
			}
		}
		
		texture.set(id, x, y, width, height, bitmapInfo);
		resInfo.mTextureInfoMap.put(texture.mId, texture);
		return texture;
	}
	
	public final static class ResOutputInfo {
		public MotoPicture mPicture;
		public MotoIntEtypeHashMap<MotoBitmapInfo> mBitmapMap = new MotoIntEtypeHashMap<MotoBitmapInfo>(5);
		public MotoIntEtypeHashMap<MotoTextureInfo> mTextureInfoMap = new MotoIntEtypeHashMap<MotoTextureInfo>(30);
		public MotoIntEtypeHashMap<MotoComponentParentBuilder> mAnimationSetsBuilders = new MotoIntEtypeHashMap<MotoComponentParentBuilder>(100);
		public MotoIntEtypeHashMap<MotoComponentParentBuilder> mInteractionSetsBuilders = new MotoIntEtypeHashMap<MotoComponentParentBuilder>(50);
		public MotoComponentBuilder mRootPictureBuilder;
		public PlayerConfigInfo mPlayConfigInfo = new PlayerConfigInfo();
		public MotoResLoader mLoader;
		public DebugInfo mDebugInfo = new DebugInfo();
	}
	
	public final static class PlayerConfigInfo {
		public long mFrameTime = MotoRenderPlayer.DEFAULT_FRAME_TIME; 
		public boolean mDiryRectEnable = true;
		public boolean mHitRectEnable = true;

		//for flushes of picture 
		public boolean mHasFlushed;
		//for scale when transform ds to px
		public float mDs2PxScale = 1f;
	}
	
	public final static class DebugInfo {
		public long mLoadResTime;
		public long mLoadBmpTime;
		public int mBmpRAM;
		public int mBmpArea;
		public int mObjectArea;
		public int mObjectCount;
	}
}
