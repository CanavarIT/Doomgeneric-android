package com.example.doomandroid;

import android.graphics.Bitmap;

/** Тонкая обёртка над нативными функциями (см. jni_bridge.c). */
public class DoomLib {

    static {
        System.loadLibrary("doom");
    }

    /** Разовая инициализация игры. wadAbsolutePath — путь в filesDir, не assets! */
    public static native void nativeInit(String wadAbsolutePath);

    /** Копирует текущий кадр DOOM в переданный Bitmap (ARGB_8888, 320x200). */
    public static native void nativeBlitFrame(Bitmap frameBitmap);

    /** pressed: true = down, false = up/cancel. doomKey — см. DoomKeys. */
    public static native void nativeKeyEvent(boolean pressed, int doomKey);
}