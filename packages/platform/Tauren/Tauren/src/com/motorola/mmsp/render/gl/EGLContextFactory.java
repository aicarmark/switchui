package com.motorola.mmsp.render.gl;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * An interface for customizing the eglCreateContext and eglDestroyContext calls.
 *

 * This interface must be implemented by clients wishing to call
 * {@link GLWallpaperService#setEGLContextFactory(EGLContextFactory)}
 */
interface EGLContextFactory {
	EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig);

	void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context);
}

class DefaultContextFactory implements EGLContextFactory {

	public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config) {
		int[] attrib_list = { 0x3098, 2, EGL10.EGL_NONE };
		return egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attrib_list);
	}

	public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
		egl.eglDestroyContext(display, context);
	}
}
