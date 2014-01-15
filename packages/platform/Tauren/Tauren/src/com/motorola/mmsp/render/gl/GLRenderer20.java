package com.motorola.mmsp.render.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import com.motorola.mmsp.render.MotoBitmapInfo;
import com.motorola.mmsp.render.MotoBitmapWrapper;
import com.motorola.mmsp.render.MotoConfig;
import com.motorola.mmsp.render.MotoPaintable;
import com.motorola.mmsp.render.MotoPicture;
import com.motorola.mmsp.render.MotoPictureGroup;
import com.motorola.mmsp.render.MotoTextureInfo;
import com.motorola.mmsp.render.util.MotoLinkedList;

public class GLRenderer20 implements Renderer {
	private static final String TAG = MotoConfig.TAG;
	private static final boolean DEBUG = MotoConfig.DEBUG;
	private MotoGLRenderPlayer mPlayer;
	private int mProgram;
	private boolean mCreated;
	
	private HashMap<Bitmap, Integer> mTextureIDMap = new HashMap<Bitmap, Integer>(2);
	private int[] mTextureID = new int[0];
	
	private int maPositionHandle;
	private int maTextureHandle;
	private int maIndexHandle;
	private int muDataHandle;
	private int muProjMatrixHandle;
	
	private int mVertexShader;
	private int mPixelShader;

	int mCurrentTextureid = -1;
	int mCurrentBlend = -1;
	
	private int VBID;
	private int IBID;
	
	private static final int DATA_ONCE = 480;
	private static final int OBJECT_DATA_LENGTH = 16;
	private static final int OBJECT_COUNT_ONCE = DATA_ONCE / OBJECT_DATA_LENGTH;
	private float[] mMatrixProjection = new float[16];
	private float[] mMatrix2D = new float[9];
	private float[] mDataToGPU = new float[DATA_ONCE];

	private static final int FLOAT_SIZE_BYTES = 4;
	private static final int SHORT_SIZE_BYTES = 2;
	private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 6 * FLOAT_SIZE_BYTES;

	private static final float[] mTriangleVerticesData = {
			    //X, Y,  Z,   U,  V,  index
				0, 	0, 	 0.0f, 0, 0,  0,
				0, 	1, 	 0.0f, 0, 1,  0,
				1, 	1, 	 0.0f, 1, 1,  0,
				1, 0, 0.0f, 1, 0,  0};
	private static final float[] mTriangleVerticesDataMany = new float[mTriangleVerticesData.length * OBJECT_COUNT_ONCE];
	static {
		int singleTriangleLen = mTriangleVerticesData.length;
		for (int i = 0; i < OBJECT_COUNT_ONCE; i++) {
			int baseIndex = i * singleTriangleLen;
			for (int j=0; j<singleTriangleLen; j++) {
				float f = mTriangleVerticesData[j];
				mTriangleVerticesDataMany[baseIndex + j] = f;
			}
			mTriangleVerticesDataMany[baseIndex + 5] = i;
			mTriangleVerticesDataMany[baseIndex + 6 * 1 + 5] = i;
			mTriangleVerticesDataMany[baseIndex + 6 * 2 + 5] = i;
			mTriangleVerticesDataMany[baseIndex + 6 * 3 + 5] = i;
		}
	}
	
	private static final short[] mIndices = {0, 1, 2,   3, 0, 2};
	private static final short[] mIndicesMany = new short[mIndices.length * OBJECT_COUNT_ONCE];
	static {
		int singleIndicesLen = mIndices.length;
		for (int i = 0; i < OBJECT_COUNT_ONCE; i++) {
			int baseIndex = i * singleIndicesLen;
			short baseValue = (short)(i * 4);
			for (int j= 0; j < singleIndicesLen; j++) {
				short f = mIndices[j];
				mIndicesMany[baseIndex + j] = (short)(f + baseValue);
			}
		}
	}
	
	private FloatBuffer mTriangleVertices;
	private ShortBuffer mIndexBuffer;
	
	private int mDebugFrame;
	private long mDebugDrawTime;
	private long mDebugDrawNow;
	private int mScreenWidth;
	private int mScreenHeight;
	private int mLowFrame;
	
	
	public GLRenderer20(MotoGLRenderPlayer player) {
		mPlayer = player;
	}

	public void setRenderPlayer(MotoGLRenderPlayer mrp) {
		this.mPlayer = mrp;
	}
	
	public void onSurfaceCreated(GL10 gl10, EGLConfig config) {
		long time = SystemClock.uptimeMillis();
		
		if (mCreated) {
			release();
		}
		mCreated = true;
		
		mProgram = createProgram(ShaderLanguage.VS, ShaderLanguage.PS);
		if (mProgram == 0) {
			return;
		}
		
		GLES20.glUseProgram(mProgram);
		maPositionHandle = getGLSLAttribLocation(mProgram, "aPosition");
		maTextureHandle = getGLSLAttribLocation(mProgram, "aTextureCoord");
		maIndexHandle = getGLSLAttribLocation(mProgram, "aIndex");
		muProjMatrixHandle = getGLSLUniformHandle(mProgram, "uProjMatrix");
		muDataHandle = getGLSLUniformHandle(mProgram, "uData");
		
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		GLES20.glFrontFace(GLES20.GL_CW);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		
		
		long timeTexture = SystemClock.uptimeMillis();
		synchronized (MotoBitmapWrapper.sGlobalBitmaps) {
			if (mPlayer.getLoadedRes().mLoader != null) {
				mPlayer.getLoadedRes().mLoader.reloadPreloadBitmaps();
			}
			ArrayList<Bitmap> bitmapList = new ArrayList<Bitmap>(2);
			attachToRenderer(mPlayer.getRootPicture(), bitmapList);
			
			int size = bitmapList.size();
			if (size > 0) {
				mTextureID = new int[size];
				mTextureIDMap = new HashMap<Bitmap, Integer>(2);
				GLES20.glGenTextures(size, mTextureID, 0);
				for (int i = 0; i < size; i++) {
					Bitmap bitmap = bitmapList.get(i);
					if (bitmap != null) {
						createTexture(bitmap, mTextureID[i]);
						mTextureIDMap.put(bitmap, new Integer(
								mTextureID[i]));
					}
				}
			}
			if (mPlayer.getLoadedRes().mLoader != null) {
				mPlayer.getLoadedRes().mLoader.recyclePreloadBitmaps();
			}
			mPlayer.mInnerBitmapChanged = false;
			Log.d(TAG, "Player " 
						+ (mPlayer != null ? mPlayer.getId() : null) 
						+ ", " + MotoBitmapWrapper.getString());
		}
		long timeTextureCreate = (SystemClock.uptimeMillis() - timeTexture);
		

		createVertexBuffers();
				
		IntBuffer maxTextureSize = IntBuffer.allocate(1);
		GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTextureSize);
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "GLRender20 max texture size = " + maxTextureSize.get(0));
			Log.d(TAG, "GLRender20 create time = " + (SystemClock.uptimeMillis() - time)
					+ ", create texture time = " + timeTextureCreate
					+ ", textures = " + Arrays.toString(mTextureID));
		}
		
		System.gc();
		System.gc();
	}

	public void release() {
		if (!mCreated) {
			return;
		}

		long time = SystemClock.uptimeMillis();
		
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "GLRender20 release begin");
		}
		mCreated = false;

		if (mTextureID != null && mTextureID.length > 0) {
			if (MotoConfig.KEY_LOG) {
				Log.d(TAG, "releaseAllTexture(), " 
								+ "textrue length = " + mTextureID.length 
								+ ", items = " + Arrays.toString(mTextureID));
			}
			GLES20.glDeleteTextures(mTextureID.length, mTextureID, 0);
			mTextureID = new int[0];
		}
		if (mTextureIDMap != null) {
			mTextureIDMap.clear();
		}
		
		IntBuffer VBOBuffer = ByteBuffer
				.allocateDirect(2 * FLOAT_SIZE_BYTES)
				.order(ByteOrder.nativeOrder()).asIntBuffer();
		int[] vbos = {VBID, IBID};
		VBOBuffer.put(vbos);
		VBOBuffer.position(0);
		GLES20.glDeleteBuffers(2, VBOBuffer);

		GLES20.glDeleteShader(mVertexShader);
		GLES20.glDeleteShader(mPixelShader);
		GLES20.glDeleteProgram(mProgram);
		
		MotoPicture picture = mPlayer.getRootPicture();
		detachedFromRenderer(picture);
		
		maPositionHandle = 0;
		maTextureHandle = 0;
		muDataHandle = 0;
		muProjMatrixHandle = 0;
		
		mVertexShader = 0;
		mPixelShader = 0;
		
		mCurrentTextureid = -1;
		mCurrentBlend  = -1;
		mProgram = 0;
		
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "GLRender20 release end time = " 
					+ (SystemClock.uptimeMillis() - time));
		}	
	}

	public void onSurfaceChanged(GL10 gl10, int width, int height) {
		mScreenWidth = width;
		mScreenHeight = height;
		
		GLES20.glViewport(0, 0, mScreenWidth, mScreenHeight);
		GLES20.glScissor(0, 0, mScreenWidth, mScreenHeight);
		Matrix.setIdentityM(mMatrixProjection, 0);
		Matrix.orthoM(mMatrixProjection, 0, 0.0f, (float) mScreenWidth - 1.0f, 
				(float) mScreenHeight - 1.0f, 0.0f, -1.0f, 1.0f);
		
		GLES20.glUseProgram(mProgram);
		GLES20.glUniformMatrix4fv(muProjMatrixHandle, 1, false,
				mMatrixProjection, 0);
		
		GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT,
				false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
				0);
		GLES20.glEnableVertexAttribArray(maPositionHandle);
		
		GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT,
				false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
				3 * FLOAT_SIZE_BYTES);
		GLES20.glEnableVertexAttribArray(maTextureHandle);
		
		GLES20.glVertexAttribPointer(maIndexHandle, 1, GLES20.GL_FLOAT,
				false, TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
				5 * FLOAT_SIZE_BYTES);
		GLES20.glEnableVertexAttribArray(maIndexHandle);
		if (MotoConfig.KEY_LOG) {
			Log.d(TAG, "Renderer20 on surface changed!");
		}
	}
	
	public boolean onDrawFrame(GL10 gl10) {
		if (mPlayer != null ) {
			mPlayer.prepareNextFrame();
		}
		mDebugDrawNow = SystemClock.uptimeMillis();
		//Log.d(TAG, "on draw frame begin");
		
		//check bitmap changed. release the useless texture.
		deleteUselessTextures();
		
		GLES20.glClearColor(0f, 0f, 0f, 1f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		
		MotoLinkedList.Node<MotoPaintable> cursor = mPlayer.getPaintableList()
				.getFirst();
		int drawCount = 0;
		while (cursor != null) {
			MotoTextureInfo textureInfo = cursor.mData.mTextureInfo;
			if (textureInfo == null
					|| textureInfo.mBitmapInfo == null
					|| textureInfo.mBitmapInfo.mBitmap == null) {
				cursor = cursor.mNext;
				continue;
			}  else {
				if (textureInfo.mBitmapInfo.mGLTextureID <= 0) {
					Integer glTextureId = mTextureIDMap.get(textureInfo.mBitmapInfo.mBitmap);
					if (glTextureId != null) {
						textureInfo.mBitmapInfo.mGLTextureID = glTextureId;
					}
					if (textureInfo.mBitmapInfo.mGLTextureID <= 0 && textureInfo.mBitmapInfo.mBitmap.isRecycled()) {
				                cursor = cursor.mNext;
						continue;
					}
				}
			}
			
			MotoPaintable paintable = cursor.mData;
			MotoBitmapInfo bitmapInfo = textureInfo.mBitmapInfo;
			if (!bitmapInfo.mGLInit) {
				Bitmap bitmap = bitmapInfo.mBitmap;
				if (bitmap != null) {
					if (bitmapInfo.mGLTextureID <= 0) {
						bitmapInfo.mGLTextureID = getOrAddTexture(bitmap);
					}
				}
				bitmapInfo.mGLInit = true;
			}
			
			int objectWithSameTexture = 0;
			int dataIndex = 0;
			int texture = bitmapInfo.mGLTextureID;
			boolean hasAlpha = bitmapInfo.mHasAlpha;
			boolean alphaNeedOpen = false;
			while (cursor != null) {
				objectWithSameTexture ++;
				paintable = cursor.mData;
				textureInfo = paintable.mTextureInfo;
				bitmapInfo = textureInfo.mBitmapInfo;
				MotoLinkedList.Node<MotoPaintable> next = cursor.mNext;
				
				int alpha = paintable.mAlpha;
				if (alpha < 255) {
					alphaNeedOpen = true;
				}
				
				paintable.mMatrix.getValues(mMatrix2D);
		
				System.arraycopy(mMatrix2D, 0, mDataToGPU, dataIndex, mMatrix2D.length);
				mDataToGPU[dataIndex + 9] = (float)textureInfo.mWidth;
				mDataToGPU[dataIndex + 10] = (float)textureInfo.mHeight;
				mDataToGPU[dataIndex + 11] = alpha / 255f;
				mDataToGPU[dataIndex + 12] = textureInfo.mTextureCoordX;
				mDataToGPU[dataIndex + 13] = textureInfo.mTextureCoordY;
				mDataToGPU[dataIndex + 14] = textureInfo.mTextureCoordWidth;
				mDataToGPU[dataIndex + 15] = textureInfo.mTextureCoordHeight;
				
				dataIndex += OBJECT_DATA_LENGTH;
				cursor = cursor.mNext;
				
				if ((next == null)
						|| (next.mData.mTextureInfo == null)
						|| (next.mData.mTextureInfo.mBitmapInfo == null)
						|| (next.mData.mTextureInfo.mBitmapInfo.mGLTextureID != texture)
						|| (dataIndex +  OBJECT_DATA_LENGTH > mDataToGPU.length)) {
					break;
				}
			}
			
			
			if (!hasAlpha && !alphaNeedOpen) {
				if (mCurrentBlend != 0) {
					GLES20.glDisable(GLES20.GL_BLEND);
					mCurrentBlend = 0;
				}
			} else {
				if (mCurrentBlend != 1) {
					GLES20.glEnable(GLES20.GL_BLEND);
					mCurrentBlend = 1;
				}
			}

			if (mCurrentTextureid != texture) {
				mCurrentTextureid = texture;
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mCurrentTextureid);
			}
			
			drawCount ++;
			GLES20.glUniform4fv(muDataHandle, OBJECT_DATA_LENGTH * objectWithSameTexture / 4, mDataToGPU, 0);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndices.length * objectWithSameTexture, GLES20.GL_UNSIGNED_SHORT, 0);
		}
		//Log.d(TAG, "on draw frame end");
		if (DEBUG) {
			mDebugDrawTime += SystemClock.uptimeMillis() - mDebugDrawNow;
			if (++mDebugFrame % 100 == 0) {
				Log.d(TAG, "draw time = " + mDebugDrawTime / 100f 
						+ ", paintable list count = " + mPlayer.getPaintableList().size() 
						+ ", draw count = " + drawCount);
				mDebugDrawTime = 0;
				mDebugFrame = 0;
			}
		}
		
		if (MotoConfig.LOW) {
			if (++mLowFrame % 300 == 0) {
				logObjectInfo();
				mLowFrame = 0;
			}
		}
		
		return (drawCount > 0);
	}
	
	private void logObjectInfo() {
		int objectArea = 0;
		MotoLinkedList.Node<MotoPaintable> current = mPlayer.getPaintableList()
				.getFirst();
		RectF rect = new RectF();
		RectF src = new RectF();
		while (current != null) {
			MotoPaintable pnt = current.mData;
			int width = 0, height = 0;
			if (pnt.mTextureInfo != null) {
				width = pnt.mTextureInfo.mWidth;
				height = pnt.mTextureInfo.mHeight;
			}
			src.set(0, 0, width, height);
			pnt.mMatrix.mapRect(rect, src);
			if (rect.intersect(0, 0, mScreenWidth, mScreenHeight)) {
				objectArea += rect.width() * rect.height();
			}
			current = current.mNext;
		}
		
		Log.v(TAG, "objects count is " + mPlayer.getPaintableList().size());
		Log.v(TAG,"objects area is " 
				+ ((float)objectArea / (1024 * 1024)) + "M, "
				+ ((float)objectArea / (480 * 854)) + " of IRM, "
				+ ((float)objectArea / (1280 * 720)) + " of Galaxy");
	}
	
	private int getGLSLAttribLocation(int program, String name) {
		int handle = GLES20.glGetAttribLocation(program, name);
		checkGlError("glGetAttribLocation " + name);
		if (handle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for " + name);
		}
		return handle;
	}
	
	private int getGLSLUniformHandle(int program, String name) {
		int handle = GLES20.glGetUniformLocation(program, name);
		checkGlError("glGetAttribLocation " + name);
		if (handle == -1) {
			throw new RuntimeException(
					"Could not get attrib location for " + name);
		}
		return handle;
	}
	
	private static int genNewId() {
		IntBuffer buffer = BufferFactory.createIntBuffer(1);
		GLES20.glGenBuffers(1, buffer);
		return buffer.get(0);
	}

	private void createVertexBuffers() {
		VBID = genNewId();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, VBID);
		mTriangleVertices = ByteBuffer
				.allocateDirect(mTriangleVerticesDataMany.length * FLOAT_SIZE_BYTES)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mTriangleVertices.put(mTriangleVerticesDataMany).position(0);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				mTriangleVertices.capacity() * 4, mTriangleVertices,
				GLES20.GL_STATIC_DRAW);
		
		IBID = genNewId();
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, IBID);
		mIndexBuffer = ByteBuffer
				.allocateDirect(mIndicesMany.length * SHORT_SIZE_BYTES)
				.order(ByteOrder.nativeOrder()).asShortBuffer();
		mIndexBuffer.put(mIndicesMany).position(0);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mIndexBuffer.capacity() * 2, mIndexBuffer,
				GLES20.GL_STATIC_DRAW);
	}

	private int loadShader(int shaderType, String fn) {
		String source = fn;
		
		int shader = GLES20.glCreateShader(shaderType);
		if (shader != 0) {
			GLES20.glShaderSource(shader, source);
			GLES20.glCompileShader(shader);
			int[] compiled = new int[1];
			GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
			if (compiled[0] == 0) {
				Log.e(TAG, "Could not compile shader " + shaderType + ":");
				Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
				GLES20.glDeleteShader(shader);
				shader = 0;
			}
		}
		return shader;
	}

	private int createProgram(String vertexSource, String fragmentSource) {
		mVertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
		if (mVertexShader == 0) {
			return 0;
		}

		mPixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
		if (mPixelShader == 0) {
			return 0;
		}

		int program = GLES20.glCreateProgram();
		if (program != 0) {
			GLES20.glAttachShader(program, mVertexShader);
			checkGlError("glAttachShader");
			GLES20.glAttachShader(program, mPixelShader);
			checkGlError("glAttachShader");
			GLES20.glLinkProgram(program);
			int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
			if (linkStatus[0] != GLES20.GL_TRUE) {
				Log.e(TAG, "Could not link program: ");
				Log.e(TAG, GLES20.glGetProgramInfoLog(program));
				GLES20.glDeleteProgram(program);
				program = 0;
			}
		}
		return program;
	}
	
	private void checkGlError(String op) {
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			Log.e(TAG, op + ": glError " + error);
			throw new RuntimeException(op + ": glError " + error);
		}
	}
	
	private final void attachToRenderer(MotoPicture picture, ArrayList<Bitmap> bitmapList) {
		if (picture == null) {
			return;
		}
		
		if (picture.isPictureLeaf()) {
			Bitmap bitmap = picture.getBitmap();
			if (bitmap != null) {
				if (!bitmapList.contains(bitmap)) {
					bitmapList.add(bitmap);
				}
			}
		} else {
			ArrayList<MotoPicture> children = ((MotoPictureGroup)picture).getChildren();
			for (MotoPicture pic : children) {
				attachToRenderer(pic, bitmapList);
			}
		}
	}
	
	private final void checkBitmapChanged(MotoPicture picture, ArrayList<Bitmap> bitmapList) {
		if (picture == null) {
			return;
		}
		
		if (picture.isPictureLeaf()) {
			Bitmap bitmap = picture.getBitmap();
			if (bitmap != null) {
				bitmapList.remove(bitmap);
			}
		} else {
			ArrayList<MotoPicture> children = ((MotoPictureGroup)picture).getChildren();
			for (MotoPicture pic : children) {
				checkBitmapChanged(pic, bitmapList);
			}
		}
	}

 	private final void detachedFromRenderer(MotoPicture picture) {
		if (picture == null) {
			return;
		}
		
		if (picture instanceof MotoPictureGroup) {
			ArrayList<MotoPicture> children = ((MotoPictureGroup)picture).getChildren();
			if (children != null) {
				for (MotoPicture pic : children) {
					detachedFromRenderer(pic);
				}
			}
		} else {
			MotoPaintable paintable = picture.getPaintable();
			if (paintable != null) {
				paintable.glChanged();
			}
		}
	}
 	
 	private final void createTexture(Bitmap bitmap, int textureId) {
		long begin = SystemClock.uptimeMillis();
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
	                GLES20.GL_LINEAR);
	    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
	                GLES20.GL_TEXTURE_MAG_FILTER,
	                GLES20.GL_LINEAR);

	    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
	                GLES20.GL_REPEAT);
	    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
	                GLES20.GL_REPEAT);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		if (DEBUG) {
			Log.d(TAG, "create texture " + textureId + ", bitmap = " + bitmap + " cost " 
					+ (SystemClock.uptimeMillis() - begin) + " ms");
		}
	}
 	
 	private Integer getOrAddTexture(Bitmap bitmap) {
 		if (bitmap == null) {
 			return 0;
 		}
 		
		Integer glTextureId = mTextureIDMap.get(bitmap);
		if (glTextureId == null) {
			int oldSize = mTextureID.length;
			int[] newTextures = new int[oldSize + 1];
			System.arraycopy(mTextureID, 0, newTextures, 0, oldSize);
			mTextureID = newTextures;
			
			GLES20.glGenTextures(1, mTextureID, oldSize);
			int textrueId = mTextureID[oldSize];
			createTexture(bitmap, textrueId);
			mTextureIDMap.put(bitmap, textrueId);
			
			glTextureId = textrueId;
		}
		
		return glTextureId;
	}
 	
 	private void deleteUselessTextures() {
		if (mPlayer.mInnerBitmapChanged) {
			long begin = SystemClock.uptimeMillis();
			String s = null;
			mPlayer.mInnerBitmapChanged = false;
			
			ArrayList<Bitmap> bitmapList = new ArrayList<Bitmap>();
			bitmapList.addAll(mTextureIDMap.keySet());
			checkBitmapChanged(mPlayer.getRootPicture(), bitmapList);
			
			
			if (bitmapList.size() > 0) {
				int[] uselessTextures = new int[bitmapList.size()];
				int count = 0;
				for (Bitmap bmp : bitmapList) {
					Integer glTextureId = mTextureIDMap.remove(bmp);
					if (glTextureId != null) {
						uselessTextures[count] = glTextureId;
						count++;
					}
				}
				GLES20.glDeleteTextures(count, uselessTextures, 0);
				
				Object[] usefulTextures = mTextureIDMap.values().toArray();
				int[] usefulTextureIds = new int[usefulTextures.length];
				for (int i = 0, size = usefulTextures.length; i < size; ) {
					Object text = usefulTextures[i];
					if (text != null) {
						usefulTextureIds[i] = (Integer)usefulTextures[i];
						i++;
					}
				}
				
				if (DEBUG) {
					s = Arrays.toString(uselessTextures)
							+ " = " + Arrays.toString(mTextureID) 
							+ " - " + Arrays.toString(usefulTextureIds);
				}
				mTextureID = usefulTextureIds;
			}
			
			if (DEBUG) {
				Log.d(TAG, "GLRenderer20 bitmap changed! " 
						+ "time = " + (SystemClock.uptimeMillis() - begin)
						+ ", delete textures = " + s);
			}
		}
	}
}
