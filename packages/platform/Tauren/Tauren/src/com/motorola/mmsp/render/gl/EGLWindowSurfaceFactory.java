package com.motorola.mmsp.render.gl;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import com.motorola.mmsp.render.MotoConfig;

import android.util.Log;

/**
 * An interface for customizing the eglCreateWindowSurface and eglDestroySurface calls.
 *

 * This interface must be implemented by clients wishing to call
 * {@link GLWallpaperService#setEGLWindowSurfaceFactory(EGLWindowSurfaceFactory)}
 */
interface EGLWindowSurfaceFactory {
	EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow);

	void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface);
}


class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {

    public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display,
            EGLConfig config, Object nativeWindow) {
        EGLSurface result = null;
        try {
            result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
        } catch (IllegalArgumentException e) {
            // This exception indicates that the surface flinger surface
            // is not valid. This can happen if the surface flinger surface has
            // been torn down, but the application has not yet been
            // notified via SurfaceHolder.Callback.surfaceDestroyed.
            // In theory the application should be notified first,
            // but in practice sometimes it is not. See b/4588890
            Log.e(MotoConfig.TAG, "eglCreateWindowSurface", e);
        }
        return result;
    }

    public void destroySurface(EGL10 egl, EGLDisplay display,
            EGLSurface surface) {
        egl.eglDestroySurface(display, surface);
    }
}
