/****************************************************************************************
 *                          Motorola Confidential Proprietary
 *                 Copyright (C) 2009 Motorola, Inc.  All Rights Reserved.
 *   
 *
 * Revision History:
 *                           Modification    Tracking
 * Author                      Date          Number     Description of Changes
 * ----------------------   ------------    ----------   --------------------
 * 
 * ***************************************************************************************
 * [QuickNote] JNI function to support utility functions for Quick Note
 * Createor : hbg683 (Younghyung Cho)
 * Main History
 *  - 2009. Dec. 04 : first created.
 * 
 * 
 *****************************************************************************************/

#include <stdio.h>
#include "jni.h"

#include <string.h>
#include <JNIHelp.h>

#include "SkUtils.h"
#include "SkRect.h"
#include "SkUnPreMultiply.h"
#include "SkColorPriv.h"
#include "GraphicsJNI.h"

#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>

#include <binder/IMemory.h>
/*2012-11-12, add by amt_sunzhao for SWITCHUITWOV-315 */ 
//#include <surfaceflinger/ISurfaceComposer.h>
#include <gui/ISurfaceComposer.h>
/*2012-11-12, add end*/ 

#include <SkImageEncoder.h>
#include <SkBitmap.h>
#include <SkCanvas.h>

#include <cutils/log.h>
#include <utils/String16.h>

//====================================================
// == Local Variables
//====================================================

/***** obsolete
static jclass    _classref_Bitmap;
static jfieldID  _fieldID_Bitmap_mNativeBitmap;
static jmethodID _methodID_Bitmap_Bitmap; // constructor
*****/

//====================================================
// == Common Local Functions
//====================================================

/***** obsolete
static inline jclass _classref_global(JNIEnv* env, const char classname[]) {
    return (jclass) env->NewGlobalRef(env->FindClass(classname));
}

static inline jfieldID _fieldID(JNIEnv* env, jclass clazz, const char fieldname[], const char type[]) {
    return env->GetFieldID(clazz, fieldname, type);
}
*****/


// === From Bitmap.cpp : START
typedef void (*ToColorProc)(SkColor dst[], const void* src, int width,
                            SkColorTable*);

static void ToColor_S32_Alpha(SkColor dst[], const void* src, int width,
                              SkColorTable*) {
    SkASSERT(width > 0);
    const SkPMColor* s = (const SkPMColor*)src;
    do {
        *dst++ = SkUnPreMultiply::PMColorToColor(*s++);
    } while (--width != 0);
}

static void ToColor_S32_Opaque(SkColor dst[], const void* src, int width,
                               SkColorTable*) {
    SkASSERT(width > 0);
    const SkPMColor* s = (const SkPMColor*)src;
    do {
        SkPMColor c = *s++;
        *dst++ = SkColorSetRGB(SkGetPackedR32(c), SkGetPackedG32(c),
                               SkGetPackedB32(c));
    } while (--width != 0);
}

static void ToColor_S565(SkColor dst[], const void* src, int width,
                         SkColorTable*) {
    SkASSERT(width > 0);
    const uint16_t* s = (const uint16_t*)src;
    do {
        uint16_t c = *s++;
        *dst++ =  SkColorSetRGB(SkPacked16ToR32(c), SkPacked16ToG32(c),
                                SkPacked16ToB32(c));
    } while (--width != 0);
}
// ==== From Bitmap.cpp : END


//====================================================
// == Initialize
//====================================================
// return : 0 (success) / -1 (fail)
static int _initialize(JNIEnv* env)
{
    /***** obsolete 
    _classref_Bitmap = _classref_global(env, "android/graphics/Bitmap");
    if(!_classref_Bitmap) { return -1; }
    _fieldID_Bitmap_mNativeBitmap = _fieldID(env, _classref_Bitmap, "mNativeBitmap", "I");
    if(!_fieldID_Bitmap_mNativeBitmap) { return -1; }
    _methodID_Bitmap_Bitmap = env->GetMethodID(_classref_Bitmap, "<init>", "(IZ[BI)V");
    if(!_methodID_Bitmap_Bitmap) { return -1; }
    *****/
    return 0;
}

//====================================================
// == Bitmap handling : START
//====================================================


/***** obsolete
static inline SkBitmap* _nativeBitmap(JNIEnv* env, jobject bitmap) {
    SkASSERT(env && bitmap && env->IsInstanceOf(bitmap, _classref_Bitmap));
    SkBitmap* b = (SkBitmap*)env->GetIntField(bitmap, _fieldID_Bitmap_mNativeBitmap);
    SkASSERT(b);
    return b;
}
*****/


// QuickNote - Add
// Unrolling macro.
#define UNROLL16( eXPR, cOUNT, cOND)                \
    switch( (cOUNT) & 0xF ) {                       \
        case 0: while (cOND){                       \
            eXPR;                                   \
        case 15: eXPR;                              \
        case 14: eXPR;                              \
        case 13: eXPR;                              \
        case 12: eXPR;                              \
        case 11: eXPR;                              \
        case 10: eXPR;                              \
        case 9: eXPR;                               \
        case 8: eXPR;                               \
        case 7: eXPR;                               \
        case 6: eXPR;                               \
        case 5: eXPR;                               \
        case 4: eXPR;                               \
        case 3: eXPR;                               \
        case 2: eXPR;                               \
        case 1: eXPR;                               \
        }                                           \
    }

/*
 * Class:     com.motorola.quicknote_QNUtil
 * Method:    nativeSet_Bitmap
 * Signature: (Landroid/graphics/Bitmap;Landroid/graphics/Rect;I)Z
 */
static jboolean _jni_set_bitmap(JNIEnv* env, jclass, jobject jbm, jobject jrect, jint jcolor)
{
    // NOTE! : Assume that verification check for all prerequisite is done before calling this function!

    SkBitmap*  bm = GraphicsJNI::getNativeBitmap(env, jbm); //_nativeBitmap(env, dstbm);
    SkRect     r;   
    jboolean   ret = true;

    GraphicsJNI::jrect_to_rect(env, jrect, &r);

    SkAutoLockPixels alp_bm(*bm);
    switch(bm->config()) {
        case SkBitmap::kARGB_8888_Config: {
            uint32_t color;
            { // Just Scope
                // Convert from normal SkColor to bitmap color format.
                SkColor skcolor;
                ToColorProc proc = bm->isOpaque() ? ToColor_S32_Opaque : ToColor_S32_Alpha;
                if (NULL == proc) { return false;  }
                proc(&skcolor, &jcolor, 1, bm->getColorTable());
                color = (uint32_t)skcolor;
            }
                     
            uint32_t* p = (uint32_t*)bm->getPixels();
            int hcnt = (int)r.height();
            p += (uint32_t)bm->width() * (uint32_t)r.fTop + (uint32_t)r.fLeft;
            while (--hcnt >= 0) {
                sk_memset32(p, color, (int)r.width());
                p = (uint32_t*)((char*)p + (int)bm->rowBytes());
            }
        } break;
   
        default:
            ret = false;
    }

    return ret;
}


/*
 * Class:     com.motorola.quicknote_QNUtil
 * Method:    nativeCopy_Bitmap
 * Signature: (Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;Landroid/graphics/Rect;Landroid/graphics/Rect;I)Z
 */
static jboolean _jni_copy_bitmap(JNIEnv* env, jclass, 
                                 jobject jdstbm, jobject jsrcbm, 
                                 jobject jdstrect, jobject jsrcrect, 
                                 jint jmaskColor)
{
    // NOTE! : Assume that verification check for all prerequisite is done before calling this function!

    SkBitmap*       dst = GraphicsJNI::getNativeBitmap(env, jdstbm); //_nativeBitmap(env, dstbm);
    const SkBitmap* src = GraphicsJNI::getNativeBitmap(env, jsrcbm); // _nativeBitmap(env, srcbm);
    SkRect          rd, rs;
    jboolean ret = true;

    GraphicsJNI::jrect_to_rect(env, jdstrect, &rd);
    GraphicsJNI::jrect_to_rect(env, jsrcrect, &rs);

    SkAutoLockPixels alp_dst(*dst);
    SkAutoLockPixels alp_src(*src);

    switch(src->config()) {
        case SkBitmap::kARGB_8888_Config: {
            SkASSERT(dst->getPixels() && src->getPixels()
                     && 4 == dst->bytesPerPixel()
                     && dst->width()*dst->bytesPerPixel() == dst->rowBytes()
                     && dst->getSize() == dst->width()*dst->height()*dst->bytesPerPixel()
                     && rd.width() == rs.width() 
                     && rd.height() == rs.height() );

            register uint32_t mask;
            { // Just Scope
                // Convert from normal SkColor to bitmap color format.
                uint32_t mcolor;
                ToColorProc proc = src->isOpaque() ? ToColor_S32_Opaque : ToColor_S32_Alpha;
                if (NULL == proc) { return false;  }
                proc(&mcolor, &jmaskColor, 1, src->getColorTable());
                mask = mcolor;
            }
                     
            register uint32_t* d = (uint32_t*)dst->getPixels();
            register uint32_t* s = (uint32_t*)src->getPixels();
            register uint32_t* dend;
            uint32_t           hcnt; // height counter

            d += dst->width() * (int)rd.fTop + (int)rd.fLeft;
            s += dst->width() * (int)rs.fTop + (int)rs.fLeft;
            hcnt = (int)rd.height();
            while(hcnt--) {
                dend = d + (int)rd.width();
                UNROLL16(if(mask != *s){*d = *s;}; d++; s++, (int)rd.width(), d<dend);
                d += dst->width() - (int)rd.width();
                s += src->width() - (int)rs.width();
            }
            
        } break;

        // Only ARGB_8888 is supported until now...
        default: 
            ret = false;
    }

    return ret;
}

#undef UNROLL16

//====================================================
// == Bitmap handling : END
//====================================================

//====================================================
// == Screen Shot : START
//====================================================

#include <fcntl.h>
#include <sys/ioctl.h>
#include <stdlib.h>
#include <linux/fb.h>

static int _read_fd(int fd, char* ptr, int len) 
{
    char *p = ptr;
    int r;

    while(len > 0) {
        r = read(fd, p, len);
        if(r > 0) {
            len -= r;
            p += r;
        } else {
            if((r < 0) && (errno == EINTR)) continue;
            return -1;
        }
    }
    return 0;
}


/*********
 * obsolete!!!

static int _write_fd(int fd, const void *ptr, int len)
{
    char *p = (char*) ptr;
    int r;

    while(len > 0) {
        r = write(fd, p, len);
        if(r > 0) {
            len -= r;
            p += r;
        } else {
            if((r < 0) && (errno == EINTR)) continue;
            return -1;
        }
    }

    return 0;
}

************/


static int _read_fb_info(struct fb_var_screeninfo* si) 
{
    SkASSERT(si);
    int fb = open("/dev/graphics/fb0", O_RDONLY);
    if(fb < 0) { 
        goto bail; 
    }

    if(ioctl(fb, FBIOGET_VSCREENINFO, si) < 0) { 
      goto bail; 
    }
    fcntl(fb, F_SETFD, FD_CLOEXEC);
    close(fb);
    return 0;

 bail:
    return -1;
}
// Memory is allocated in it!!
// 0 (success) / -1 (fail)
static int _read_fb(const struct fb_var_screeninfo& si, char* buffer)
{
    /*
     * Reference : framebuffer_service.c 
     */

    int    fb, offset, sz;
    unsigned  int bytespp; // bytes per pixel
    char*  p;
    int    ret = -1;

    fb = open("/dev/graphics/fb0", O_RDONLY);
    if(fb < 0) { goto done; }

    fcntl(fb, F_SETFD, FD_CLOEXEC);

    bytespp = si.bits_per_pixel / 8;

    /* HACK: for several of our 3d cores a specific alignment
     * is required so the start of the fb may not be an integer number of lines
     * from the base.  As a result we are storing the additional offset in
     * xoffset. This is not the correct usage for xoffset, it should be added
     * to each line, not just once at the beginning */
    offset = si.xoffset * bytespp;
    offset += si.xres * si.yoffset * bytespp;

    sz = si.xres * si.yres * bytespp;
    
    p = buffer;
    lseek(fb, offset, SEEK_SET);
    if(_read_fd(fb, p, sz)) { goto done; }
    
    ret = 0;

 done:
    if(fb >= 0) { close(fb); }

    return ret;
    
}


/*
 * Class:     qilin_qn_QNUtil
 * Method:    nativeScreenshot
 * Signature: ()Landroid/graphics/Bitmap;
 */

using namespace android;

//refer to frameworks/base/services/surfaceflinger/tests/screencap/screencap.cpp
static jboolean  _jni_screenshot(JNIEnv* env, jclass, jstring  path)
{
   // LOGW(" in _jni_screenshot");

 /*   const String16 name("SurfaceFlinger");
    sp<ISurfaceComposer> composer;
    getService(name, &composer);

    sp<IMemoryHeap> heap;
    uint32_t w, h;
    PixelFormat f;

    status_t err = composer->captureScreen(0, &heap, &w, &h, &f, 0, 0);
    
    if (err != NO_ERROR) {
        fprintf(stderr, "screen capture failed: %s\n", strerror(-err));
        LOGI("_jni_screenshot: LOGI: error, return false");
        return false; 
        exit(0);
    }

    const char *pathStr = env->GetStringUTFChars(path, NULL);
    if (pathStr == NULL) { // Out of memory
        LOGI("_jni_screenshot: Error: pathStr = null");
     // jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
       return false;
    }


    printf("screen capture success: w=%u, h=%u, pixels=%p\n",
            w, h, heap->getBase());

    printf("_jni_screenshot: saving file as PNG in %s ...\n", pathStr);

    SkBitmap b;
    b.setConfig(SkBitmap::kARGB_8888_Config, w, h);
    b.setPixels(heap->getBase());

    SkImageEncoder::EncodeFile(pathStr, b,
           SkImageEncoder::kPNG_Type, SkImageEncoder::kDefaultQuality);

   return true;

*/
  return false;
}


/*
 * Class:     com.motorola.quicknote_QNUtil
 * Method:    nativeAvoid_color
 * Signature: (Landroid/graphics/Bitmap;I)Z
 */
/* Comment by a22977 to pass TTUPG build
 *
static jboolean _jni_avoid_color(JNIEnv* env, jclass, jobject jbm, jint jcolor)
{
    // NOTE! : Assume that verification check for all prerequisite is done before calling this function!

    SkBitmap*         skbm = GraphicsJNI::getNativeBitmap(env, jbm); //_nativeBitmap(env, dstbm);
    jboolean          ret = true;

    SkAutoLockPixels alp_bm(*skbm);

    switch(skbm->config()) {
        case SkBitmap::kARGB_8888_Config: {
            register uint32_t color;
            { // Just Scope
                // Convert from normal SkColor to bitmap color format.
                uint32_t avoid_color;
                ToColorProc proc = skbm->isOpaque() ? ToColor_S32_Opaque : ToColor_S32_Alpha;
                if (NULL == proc) { return false;  }
                proc(&avoid_color, &jcolor, 1, skbm->getColorTable());
                color = avoid_color;
            }
            
            register uint32_t* p = (uint32_t*)skbm->getPixels();
            register uint32_t* pend = p + skbm->width() * skbm->height();
            register uint32_t  avoid_to = color ^ 0x00010000;
            while(p < pend) {
                if(*p == color) {
                    // we should avoid this color... change 16-th bit..(LSB of 'Blue' in SkBitmap)
                    *p = avoid_to;
                }
                p++;
            }
            
        } break;

        // Only ARGB_8888 is supported until now...
        default: 
            ret = false;
    }

    return ret;  
}*/


//====================================================
// == Register natives
//====================================================

#include <android_runtime/AndroidRuntime.h>

static JNINativeMethod _native_methods[] = {
    { "nativeSet_Bitmap",
      "(Landroid/graphics/Bitmap;Landroid/graphics/Rect;I)Z",
      (void*)_jni_set_bitmap },
    { "nativeCopy_Bitmap", 
      "(Landroid/graphics/Bitmap;Landroid/graphics/Bitmap;Landroid/graphics/Rect;Landroid/graphics/Rect;I)Z",
      (void*)_jni_copy_bitmap },
    { "nativeScreenshot",
      "(Ljava/lang/String;)Z",
      (void*)_jni_screenshot },
/*    { "nativeAvoid_color", 
      "(Landroid/graphics/Bitmap;I)Z",
      (void*)_jni_avoid_color }*/
};

static const char _CLASS_NAME[] = "com/motorola/quicknote/QNUtil";

static int register_native_methods(JNIEnv* env, const char classname[], 
                                   JNINativeMethod* methods, int num)
{
    jclass clazz = env->FindClass(classname);
    if(!clazz) { return JNI_FALSE; }
    if(env->RegisterNatives(clazz, methods, num) < 0) { return JNI_FALSE; }
    return JNI_TRUE;
}

static int register_natives(JNIEnv* env)
{
    if(!register_native_methods(env, _CLASS_NAME, _native_methods, 
                                sizeof(_native_methods)/sizeof(_native_methods[0]))) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    jint result = -1;
    JNIEnv* env = NULL;
    JavaVMAttachArgs args;

    if(JNI_OK != vm->GetEnv((void**)&env, JNI_VERSION_1_4) ) { goto bail; }
    if(0 != _initialize(env) ) { goto bail; }
    if(!register_natives(env)) { goto bail; }

    result = JNI_VERSION_1_4;

 bail:
    return result;
}
