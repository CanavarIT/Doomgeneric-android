package com.example.doomandroid;

import android.graphics.Bitmap;

/** Тонкая обёртка над нативными функциями (см. jni_bridge.c). */
public class DoomLib {

    static {
        System.loadLibrary("doom");
    }

    /**
     * Разовая инициализация игры.
     * wadAbsolutePath — путь к doom1.wad (в filesDir, не assets!).
     * tmpDir — writable-директория для временных файлов (например,
     *          getCacheDir() или getFilesDir()).
     */
    public static native void nativeInit(String wadAbsolutePath, String tmpDir);

    /** Копирует текущий кадр DOOM в переданный Bitmap (ARGB_8888). */
    public static native void nativeBlitFrame(Bitmap frameBitmap);

    /** pressed: true = down, false = up/cancel. doomKey — см. DoomKeys. */
    public static native void nativeKeyEvent(boolean pressed, int doomKey);
}
