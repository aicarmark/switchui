package com.motorola.mmsp.render.gl;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;

import android.util.Log;
import android.view.SurfaceHolder;

import com.motorola.mmsp.render.MotoConfig;

public final class EglHelper {

	private EGL10 mEgl;
	private EGLDisplay mEglDisplay;
	private EGLSurface mEglSurface;
	private EGLContext mEglContext;
	EGLConfig mEglConfig;

	private EGLConfigChooser mEGLConfigChooser;
	private EGLContextFactory mEGLContextFactory;
	private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
	private GLWrapper mGLWrapper;

	public EglHelper(EGLConfigChooser chooser, EGLContextFactory contextFactory,
			EGLWindowSurfaceFactory surfaceFactory, GLWrapper wrapper) {
		this.mEGLConfigChooser = chooser;
		this.mEGLContextFactory = contextFactory;
		this.mEGLWindowSurfaceFactory = surfaceFactory;
		this.mGLWrapper = wrapper;
	}

	/**
     * Initialize EGL for a given configuration spec.
     * @param configSpec
     */
    public void start() {
        if (MotoConfig.KEY_LOG) {
            Log.w("EglHelper", "start() tid=" + Thread.currentThread().getId());
        }
        /*
         * Get an EGL instance
         */
        mEgl = (EGL10) EGLContext.getEGL();

        /*
         * Get to the default display.
         */
        mEglDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        /*
         * We can now initialize EGL for that display
         */
        int[] version = new int[2];
        if(!mEgl.eglInitialize(mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed");
        }
        mEglConfig = mEGLConfigChooser.chooseConfig(mEgl, mEglDisplay);

        /*
        * Create an EGL context. We want to do this as rarely as we can, because an
        * EGL context is a somewhat heavy object.
        */
        mEglContext = mEGLContextFactory.createContext(mEgl, mEglDisplay, mEglConfig);
        if (mEglContext == null || mEglContext == EGL10.EGL_NO_CONTEXT) {
            mEglContext = null;
            throw new EglCreateContextException("createContext");
        }
        if (MotoConfig.KEY_LOG) {
            Log.d("EglHelper", "createContext " + mEglContext 
            		+ " tid=" + Thread.currentThread().getId());
        }

        mEglSurface = null;
    }

    /*
     * React to the creation of a new surface by creating and returning an
     * OpenGL interface that renders to that surface.
     */
    public GL createSurface(SurfaceHolder holder) {
        if (MotoConfig.KEY_LOG) {
            Log.w("EglHelper", "createSurface()  tid=" + Thread.currentThread().getId());
        }
        /*
         * Check preconditions.
         */
        if (mEgl == null) {
            throw new RuntimeException("egl not initialized");
        }
        if (mEglDisplay == null) {
            throw new RuntimeException("eglDisplay not initialized");
        }
        if (mEglConfig == null) {
            throw new RuntimeException("mEglConfig not initialized");
        }
        /*
         *  The window size has changed, so we need to create a new
         *  surface.
         */
        if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {

            /*
             * Unbind and destroy the old EGL surface, if
             * there is one.
             */
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
        }

        /*
         * Create an EGL surface we can render into.
         */
        mEglSurface = mEGLWindowSurfaceFactory.createWindowSurface(mEgl,
                mEglDisplay, mEglConfig, holder);

        if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
            int error = mEgl.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
            }
            return null;
        }

        /*
         * Before we can issue GL commands, we need to make sure
         * the context is current and bound to a surface.
         */
        if (!mEgl.eglMakeCurrent(mEglDisplay, mEglSurface, mEglSurface, mEglContext)) {
            throw new EglMakeCurrentException("eglMakeCurrent");
        }

        GL gl = mEglContext.getGL();
        if (mGLWrapper != null) {
            gl = mGLWrapper.wrap(gl);
        }

        return gl;
    }

    public void purgeBuffers() {
        mEgl.eglMakeCurrent(mEglDisplay,
                EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT);
        mEgl.eglMakeCurrent(mEglDisplay,
                mEglSurface, mEglSurface,
                mEglContext);
    }

    /**
     * Display the current render surface.
     * @return false if the context has been lost.
     */
    public boolean swap() {
        if (! mEgl.eglSwapBuffers(mEglDisplay, mEglSurface)) {

            /*
             * Check for EGL_CONTEXT_LOST, which means the context
             * and all associated data were lost (For instance because
             * the device went to sleep). We need to sleep until we
             * get a new surface.
             */
            int error = mEgl.eglGetError();
            switch(error) {
            case EGL11.EGL_CONTEXT_LOST:
                return false;
            case EGL10.EGL_BAD_NATIVE_WINDOW:
                // The native window is bad, probably because the
                // window manager has closed it. Ignore this error,
                // on the expectation that the application will be closed soon.
                Log.e("EglHelper", "eglSwapBuffers returned EGL_BAD_NATIVE_WINDOW. tid=" + Thread.currentThread().getId());
                break;
            default:
                throw new EglBadSurfaceException("eglSwapBuffers error " + error);
            }
        }
        return true;
    }

    public void destroySurface() {
        if (MotoConfig.KEY_LOG) {
            Log.w("EglHelper", "destroySurface()  tid=" 
            			+ Thread.currentThread().getId());
        }
        if (mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE) {
            mEgl.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT);
            mEGLWindowSurfaceFactory.destroySurface(mEgl, mEglDisplay, mEglSurface);
            mEglSurface = null;
        }
    }

    public void finish() {
        if (MotoConfig.KEY_LOG) {
            Log.w("EglHelper", "finish() tid=" + Thread.currentThread().getId());
        }
        if (mEglContext != null) {
            mEGLContextFactory.destroyContext(mEgl, mEglDisplay, mEglContext);
            mEglContext = null;
        }
        if (mEglDisplay != null) {
            mEgl.eglTerminate(mEglDisplay);
            mEglDisplay = null;
        }
    }
}