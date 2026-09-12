// Платформенный слой doomgeneric для Android.
// Реализует функции, которые ожидает doomgeneric.c: DG_Init, DG_DrawFrame,
// DG_SleepMs, DG_GetTicksMs, DG_GetKey, DG_SetWindowTitle.
//
// Подключается автоматически в CMakeLists.txt вместе с исходниками
// doomgeneric/*.c, которые нужно docкопировать самостоятельно (см. README
// в CMakeLists.txt).

#include <time.h>
#include <string.h>
#include <pthread.h>
#include <android/log.h>
#include <android/bitmap.h>

#include "doomgeneric.h"      // из исходников doomgeneric — объявляет DG_ScreenBuffer и т.д.
#include "doomgeneric_android.h"

#define TAG "DoomNative"

// ---- Очередь клавиш (Java touch -> DOOM input) -----------------------------
// Простая кольцевая очередь, доступ из одного native-потока игры и из
// UI-потока Java (через JNI-вызов) — защищаем мьютексом.

#define KEY_QUEUE_SIZE 64

typedef struct {
    int pressed;
    unsigned char key;
} KeyEvent;

static KeyEvent keyQueue[KEY_QUEUE_SIZE];
static int keyQueueHead = 0;
static int keyQueueTail = 0;
static pthread_mutex_t keyQueueMutex = PTHREAD_MUTEX_INITIALIZER;

static JavaVM *g_vm = NULL;

void android_set_javavm(JavaVM *vm) {
    g_vm = vm;
}

void android_push_key(int pressed, unsigned char doomKey) {
    pthread_mutex_lock(&keyQueueMutex);
    int nextHead = (keyQueueHead + 1) % KEY_QUEUE_SIZE;
    if (nextHead != keyQueueTail) { // если очередь не полна
        keyQueue[keyQueueHead].pressed = pressed;
        keyQueue[keyQueueHead].key = doomKey;
        keyQueueHead = nextHead;
    } else {
        __android_log_print(ANDROID_LOG_WARN, TAG, "key queue full, dropping event");
    }
    pthread_mutex_unlock(&keyQueueMutex);
}

static int android_pop_key(int *pressed, unsigned char *doomKey) {
    int has = 0;
    pthread_mutex_lock(&keyQueueMutex);
    if (keyQueueTail != keyQueueHead) {
        *pressed = keyQueue[keyQueueTail].pressed;
        *doomKey = keyQueue[keyQueueTail].key;
        keyQueueTail = (keyQueueTail + 1) % KEY_QUEUE_SIZE;
        has = 1;
    }
    pthread_mutex_unlock(&keyQueueMutex);
    return has;
}

// ---- Реализация DG_* --------------------------------------------------------

void DG_Init(void) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "DG_Init: buffer %dx%d",
                         DOOMGENERIC_RESX, DOOMGENERIC_RESY);
}

void DG_SetWindowTitle(const char *title) {
    __android_log_print(ANDROID_LOG_INFO, TAG, "window title: %s", title ? title : "(null)");
}

uint32_t DG_GetTicksMs(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (uint32_t)(ts.tv_sec * 1000L + ts.tv_nsec / 1000000L);
}

void DG_SleepMs(uint32_t ms) {
    struct timespec ts;
    ts.tv_sec = ms / 1000;
    ts.tv_nsec = (ms % 1000) * 1000000L;
    nanosleep(&ts, NULL);
}

int DG_GetKey(int *pressed, unsigned char *doomKey) {
    return android_pop_key(pressed, doomKey);
}

// DG_DrawFrame вызывается движком каждый готовый кадр. Сам по себе он ничего
// не блитует на экран — мы просто оставляем DG_ScreenBuffer как есть,
// а перерисовку в Bitmap делает android_blit_to_bitmap(), которую вызывает
// jni_bridge.c из Java-таймера отрисовки (см. DoomSurfaceView).
void DG_DrawFrame(void) {
    // намеренно пусто — блит делается по явному запросу из Java (nativeBlitFrame)
}

void android_blit_to_bitmap(JNIEnv *env, jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;

    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "getInfo failed");
        return;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "bitmap must be ARGB_8888");
        return;
    }
    if ((int)info.width != DOOMGENERIC_RESX || (int)info.height != DOOMGENERIC_RESY) {
        __android_log_print(ANDROID_LOG_ERROR, TAG,
            "bitmap size %ux%u != doom buffer %dx%d",
            info.width, info.height, DOOMGENERIC_RESX, DOOMGENERIC_RESY);
        return;
    }

    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "lockPixels failed");
        return;
    }

    // DG_ScreenBuffer — это uint32_t* в формате 0xAARRGGBB (обычно) размером
    // DOOMGENERIC_RESX * DOOMGENERIC_RESY. Если stride бампа совпадает с
    // шириной * 4 байта, копируем одним memcpy, иначе — построчно.
    size_t rowBytes = (size_t)DOOMGENERIC_RESX * 4;
    if (info.stride == rowBytes) {
        memcpy(pixels, DG_ScreenBuffer, rowBytes * DOOMGENERIC_RESY);
    } else {
        uint8_t *dst = (uint8_t *)pixels;
        const uint8_t *src = (const uint8_t *)DG_ScreenBuffer;
        for (int y = 0; y < DOOMGENERIC_RESY; y++) {
            memcpy(dst + y * info.stride, src + y * rowBytes, rowBytes);
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}