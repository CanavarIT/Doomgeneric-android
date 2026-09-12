// JNI-мост между Java (DoomLib) и движком doomgeneric.
#include <jni.h>
#include <android/log.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>

#include "doomgeneric.h"
#include "doomgeneric_android.h"

#define TAG "DoomNative"

static pthread_t gameThread;
static int gameThreadStarted = 0;
static char g_wadPath[1024];

// Игровой цикл doomgeneric крутится в СВОЁМ потоке, а не в UI-потоке Java —
// иначе он заблокирует отрисовку/тач-события Android.
static void *game_thread_main(void *arg) {
    // Правильный формат аргументов для doomgeneric / Chocolate Doom:
    // argv[0] = имя программы
    // argv[1] = "-iwad"
    // argv[2] = полный путь к файлу
    char *argv[] = {
        "doom",
        "-iwad",
        g_wadPath
    };
    int argc = 3;

    __android_log_print(ANDROID_LOG_INFO, TAG, "Starting doomgeneric_Create with iwad=%s", g_wadPath);

    doomgeneric_Create(argc, argv);

    for (;;) {
        doomgeneric_Tick();
    }
    return NULL;
}

JNIEXPORT void JNICALL
Java_com_example_doomandroid_DoomLib_nativeInit(JNIEnv *env, jclass clazz, jstring wadPath) {
    JavaVM *vm;
    (*env)->GetJavaVM(env, &vm);
    android_set_javavm(vm);

    const char *path = (*env)->GetStringUTFChars(env, wadPath, NULL);
    if (path == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "GetStringUTFChars failed");
        return;
    }

    strncpy(g_wadPath, path, sizeof(g_wadPath) - 1);
    g_wadPath[sizeof(g_wadPath) - 1] = '\0';
    (*env)->ReleaseStringUTFChars(env, wadPath, path);

    __android_log_print(ANDROID_LOG_INFO, TAG, "nativeInit, wad=%s", g_wadPath);

    if (!gameThreadStarted) {
        gameThreadStarted = 1;
        int err = pthread_create(&gameThread, NULL, game_thread_main, NULL);
        if (err != 0) {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "pthread_create failed: %d", err);
            gameThreadStarted = 0;
        }
    }
}

// Вызывается из Java на каждый кадр отрисовки (например, из Choreographer'а
// в DoomSurfaceView) — просто копирует текущий DG_ScreenBuffer в Bitmap.
JNIEXPORT void JNICALL
Java_com_example_doomandroid_DoomLib_nativeBlitFrame(JNIEnv *env, jclass clazz, jobject bitmap) {
    android_blit_to_bitmap(env, bitmap);
}

// pressed: 1 = ACTION_DOWN, 0 = ACTION_UP/CANCEL. doomKey — код клавиши DOOM
// (см. doomkeys.h: KEY_UPARROW, KEY_FIRE, KEY_USE, KEY_ESCAPE и т.д.)
JNIEXPORT void JNICALL
Java_com_example_doomandroid_DoomLib_nativeKeyEvent(JNIEnv *env, jclass clazz,
                                                     jint pressed, jint doomKey) {
    android_push_key(pressed, (unsigned char) doomKey);
}