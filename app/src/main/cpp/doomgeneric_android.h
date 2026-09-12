#ifndef DOOMGENERIC_ANDROID_H
#define DOOMGENERIC_ANDROID_H

#include <jni.h>
#include <stdint.h>

// Кладёт нажатие/отпускание клавиши DOOM в очередь, откуда её потом
// заберёт DG_GetKey() внутри игрового цикла.
// pressed: 1 = down, 0 = up. doomKey — код из doomkeys.h (KEY_UPARROW и т.п.)
void android_push_key(int pressed, unsigned char doomKey);

// Копирует текущий DG_ScreenBuffer в переданный Java Bitmap (формат ARGB_8888,
// размер бэкбуфера должен точно совпадать с DOOMGENERIC_RESX x DOOMGENERIC_RESY).
void android_blit_to_bitmap(JNIEnv *env, jobject bitmap);

// Сохраняем ссылку на JavaVM, чтобы native-поток (если будет отдельный) мог
// получить JNIEnv через AttachCurrentThread.
void android_set_javavm(JavaVM *vm);

#endif // DOOMGENERIC_ANDROID_H