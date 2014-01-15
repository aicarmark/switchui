/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* This file is copied from frameworks/base/core/jni/android_util_EventLog.cpp and modified */
// #define LOG_NDEBUG 0
#define LOG_TAG "DvcStat"

#include <fcntl.h>

#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include "jni.h"
#include "cutils/logger.h"

namespace devicestats {

enum {
    DEVICESTATS_EVENT_PARAM_LOGGER_MAX_SIZE,
    DEVICESTATS_EVENT_PARAM_LOGGER_CURRENT_SIZE,
    DEVICESTATS_EVENT_PARAM_FIRST_LOG_TIME,
    DEVICESTATS_EVENT_PARAM_SANITY_ERRORS
};

static jclass gCollectionClass;
static jmethodID gCollectionAddID;

static jclass gEventClass;
static jmethodID gEventInitID;

static int fd;
static jlong firstLogTimeMs;
static jlong numSanityErrors;

#define WRAP(C) do { C } while(0)
#define ERROR_EXIT WRAP( { errorLine = __LINE__; goto FUNCTION_EXIT;  } )
#define SANITY_ASSERT(VALUE) WRAP( if ( !(VALUE) ) ERROR_EXIT; )
#define SKIP_PAYLOAD(N) WRAP( SANITY_ASSERT(len >= N) ; payload += N; len -= N; )
#define PROCESS_PAYLOAD(N,COMMAND) WRAP( SANITY_ASSERT(len >= N ) ; COMMAND; payload += N; len -= N; )
#define MAX_ERROR_EVENT_LEN 100

// IKSTABLE6-15974, the eventlog buffer has corrupt data, that causes a wtf to be added to dropbox.
// This function, based on the inverse of android_util_EventLog_writeEvent_Array, detects whether
// the eventlog data is corrupted
// Also see EventLog.Event.decodeObject() in frameworks/base/core/java/android/util/EventLog.java.
static bool deviceStatsSanityCheckEventData( logger_entry *entry, uint32_t len ) {
    jint errorLine = 0, savedLen = len, stringLen, numObjects;
    uint8_t *savedData = (uint8_t *)entry, type, *payload;

    SANITY_ASSERT( entry->len + sizeof(*entry) == (uint32_t)len );

    payload = (uint8_t *)entry + sizeof(*entry);
    len -= sizeof(*entry);
    SKIP_PAYLOAD( 4 ); // Skip the tag

    PROCESS_PAYLOAD( 1, ( type = *payload ) );

    if ( type == EVENT_TYPE_INT ) {
        SKIP_PAYLOAD( sizeof(jint) );
    } else if ( type == EVENT_TYPE_LONG ) {
        SKIP_PAYLOAD( sizeof(jlong) );
    } else if ( type == EVENT_TYPE_STRING ) {
        PROCESS_PAYLOAD( sizeof(jint), ( memcpy( &stringLen, payload, sizeof(jint) ) ) );
        SKIP_PAYLOAD( (uint32_t)stringLen );

        // dc_mm event tag logging code added in multimedia doesnt add a trailing '\n'
        // PROCESS_PAYLOAD( 1, { if ( *payload != '\n' ) { ERROR_EXIT; }  } );
    } else if ( type == EVENT_TYPE_LIST ) {
        PROCESS_PAYLOAD( 1, ( numObjects = *payload ) );
        while ( numObjects-- ) {
            PROCESS_PAYLOAD( 1, ( type = *payload ) );
            if ( type == EVENT_TYPE_STRING ) {
                PROCESS_PAYLOAD( sizeof(jint), ( memcpy( &stringLen, payload, sizeof(jint) ) ) );
                SKIP_PAYLOAD( (uint32_t)stringLen );
            } else if ( type == EVENT_TYPE_INT ) {
                SKIP_PAYLOAD( sizeof(jint) );
            } else if ( type == EVENT_TYPE_LONG ) {
                SKIP_PAYLOAD( sizeof(jlong) );
            } else {
                ERROR_EXIT;
            }
        }
        // The last byte must be '\n', but dvm_gc_info is logged incorrectly from native code
        // PROCESS_PAYLOAD( 1, { if ( *payload != '\n' ) { ERROR_EXIT; }  } );
    } else {
        ERROR_EXIT;
    }
    // EventLog.decodeObject() doesn't validate this, so wtf wont be reported if this fails
    // SANITY_ASSERT( len == 0 );

FUNCTION_EXIT:
    if ( errorLine != 0 ) {
        numSanityErrors++;

        static bool errorLoggedOnce;
        // report binary dump of 1 log to logcat
        if ( !errorLoggedOnce ) {
            errorLoggedOnce = true;
            char errorBuf[ 3 + MAX_ERROR_EVENT_LEN * 3 + 1 ], *ptr = errorBuf;
            ptr += sprintf( ptr, "%02x ", (uint8_t)errorLine );
            if ( savedLen > MAX_ERROR_EVENT_LEN ) {
                savedLen = MAX_ERROR_EVENT_LEN;
            }
            for ( int i=0; i<savedLen; i++ ) {
                ptr += sprintf( ptr, "%02x ", savedData[i] );
            }
            ALOGE( "%s", errorBuf );
        }
        return false;
    }

    return true;
}

/*
 * In class android.util.EventLog:
 *  static native void readEvents(int[] tags, Collection<Event> output)
 *
 *  Reads events from the event log, typically /dev/log/events
 */
static void deviceStatsReadEvents(JNIEnv* env, jobject clazz,
                                             jintArray tags,
                                             jobject out) {
    firstLogTimeMs = numSanityErrors = 0;

    if (tags == NULL || out == NULL) {
        jniThrowException(env, "java/lang/NullPointerException", NULL);
        return;
    }

    jsize tagLength = env->GetArrayLength(tags);
    jint *tagValues = env->GetIntArrayElements(tags, NULL);

    bool isFirstLog = true;
    uint8_t buf[LOGGER_ENTRY_MAX_LEN];
    struct timeval timeout = {0, 0};
    fd_set readset;
    FD_ZERO(&readset);

    for (;;) {
        // Use a short select() to try to avoid problems hanging on read().
        // This means we block for 5ms at the end of the log -- oh well.
        timeout.tv_usec = 5000;
        FD_SET(fd, &readset);
        int r = select(fd + 1, &readset, NULL, NULL, &timeout);
        if (r == 0) {
            break;  // no more events
        } else if (r < 0 && errno == EINTR) {
            continue;  // interrupted by signal, try again
        } else if (r < 0) {
            jniThrowIOException(env, errno);  // Will throw on return
            break;
        }

        int len = read(fd, buf, sizeof(buf));
        if (len == 0 || (len < 0 && errno == EAGAIN)) {
            break;  // no more events
        } else if (len < 0 && errno == EINTR) {
            continue;  // interrupted by signal, try again
        } else if (len < 0) {
            jniThrowIOException(env, errno);  // Will throw on return
            break;
        } else if ((size_t) len < sizeof(logger_entry) + sizeof(int32_t)) {
            jniThrowException(env, "java/io/IOException", "Event too short");
            break;
        }

        logger_entry* entry = (logger_entry*) buf;
        int32_t tag = * (int32_t*) (buf + sizeof(*entry));

        if ( isFirstLog ) {
            firstLogTimeMs = ((jlong)entry->sec) * 1000 + entry->nsec / 1000000;
            isFirstLog = false;
        }

        int found = 0;
        for (int i = 0; !found && i < tagLength; ++i) {
            found = (tag == tagValues[i]);
        }

        if (found && deviceStatsSanityCheckEventData( entry, len ) ) {
            jsize len = sizeof(*entry) + entry->len;
            jbyteArray array = env->NewByteArray(len);
            if (array == NULL) break;

            jbyte *bytes = env->GetByteArrayElements(array, NULL);
            memcpy(bytes, buf, len);
            env->ReleaseByteArrayElements(array, bytes, 0);

            jobject event = env->NewObject(gEventClass, gEventInitID, array);
            if (event == NULL) break;

            env->CallBooleanMethod(out, gCollectionAddID, event);
            env->DeleteLocalRef(event);
            env->DeleteLocalRef(array);
        }
    }

    env->ReleaseIntArrayElements(tags, tagValues, 0);
}

static void nativeInit( JNIEnv* env, jobject clazz ) {
    static bool initDone;
    if ( initDone == true ) return;

    fd = open("/dev/" LOGGER_LOG_EVENTS, O_RDONLY | O_NONBLOCK);
    if (fd < 0) {
        jniThrowIOException(env, errno);
        return;
    }

    initDone = true;
}

static jlong nativeGetEventParam( JNIEnv* env, jobject clazz, jint type ) {
    long ret = -1;
    bool throwException = false;
    switch ( type ) {
    case DEVICESTATS_EVENT_PARAM_LOGGER_MAX_SIZE:
        {
            if ( ( ret = ioctl( fd, LOGGER_GET_LOG_BUF_SIZE ) ) == -1 ) {
                jniThrowIOException(env, errno);
            }
        }
        break;

    case DEVICESTATS_EVENT_PARAM_LOGGER_CURRENT_SIZE:
        {
            if ( ( ret = ioctl( fd, LOGGER_GET_LOG_LEN ) ) == -1 ) {
                jniThrowIOException(env, errno);
            }
        }
        break;

    case DEVICESTATS_EVENT_PARAM_FIRST_LOG_TIME:
        {
            ret = firstLogTimeMs;
        }
        break;

    case DEVICESTATS_EVENT_PARAM_SANITY_ERRORS:
        {
            ret = numSanityErrors;
        }
        break;

    default:
        {
            jniThrowIOException(env, EINVAL);
        }
        break;
    }
    return ret;
}

/*
 * JNI registration.
 */
static JNINativeMethod gRegisterMethods[] = {
    /* name, signature, funcPtr */
    { "nativeReadEvents", "([ILjava/util/Collection;)V", (void*) deviceStatsReadEvents },
    { "nativeInit", "()V", (void *)nativeInit },
    { "nativeGetEventParam", "(I)J", (void *)nativeGetEventParam },
};

static struct { const char *name; jclass *clazz; } gClasses[] = {
    { "android/util/EventLog$Event", &gEventClass },
    { "java/util/Collection", &gCollectionClass },
};

static struct { jclass *c; const char *name, *mt; jmethodID *id; } gMethods[] = {
    { &gEventClass, "<init>", "([B)V", &gEventInitID },
    { &gCollectionClass, "add", "(Ljava/lang/Object;)Z", &gCollectionAddID },
};

}; // namespace devicestats

using namespace devicestats;

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jclass  clazz;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("JNI_OnLoad: GetEnv failed");
        return -1;
    }
    else if ((clazz = env->FindClass("com/motorola/devicestatistics/eventlogs/EventLoggerService")) == NULL) {
        ALOGE("JNI_OnLoad: failed to find class");
        return 0;
    }

    for (int i = 0; i < NELEM(gClasses); ++i) {
        jclass clazz = env->FindClass(gClasses[i].name);
        if (clazz == NULL) {
            ALOGE("Can't find class: %s\n", gClasses[i].name);
            return -1;
        }
        *gClasses[i].clazz = (jclass) env->NewGlobalRef(clazz);
    }

    for (int i = 0; i < NELEM(gMethods); ++i) {
        *gMethods[i].id = env->GetMethodID(
                *gMethods[i].c, gMethods[i].name, gMethods[i].mt);
        if (*gMethods[i].id == NULL) {
            ALOGE("Can't find method: %s\n", gMethods[i].name);
            return -1;
        }
    }

    return env->RegisterNatives( clazz, gRegisterMethods,
            NELEM(gRegisterMethods)) < 0 ? -1 : JNI_VERSION_1_4;
}
