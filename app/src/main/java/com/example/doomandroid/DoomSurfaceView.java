package com.example.doomandroid;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Выводит кадры DOOM (320x200 софтверный буфер) на весь экран.
 * Кадр запрашивается через Choreographer, чтобы не гонять лишние memcpy
 * чаще частоты обновления экрана, но независимо от тикрейта самой игры
 * (игровой цикл крутится в отдельном native-потоке, см. jni_bridge.c).
 */
public class DoomSurfaceView extends SurfaceView implements SurfaceHolder.Callback,
        Choreographer.FrameCallback {

    private static final int DOOM_W = 640;
    private static final int DOOM_H = 400;

    private final Bitmap frameBitmap;
    private final Rect dstRect = new Rect();
    private volatile boolean running = false;

    public DoomSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        frameBitmap = Bitmap.createBitmap(DOOM_W, DOOM_H, Bitmap.Config.ARGB_8888);
        setWillNotDraw(false);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Растягиваем 320x200 на весь физический размер поверхности с
        // сохранением пропорций 4:3 внутри доступной области.
        float targetAspect = 4f / 3f;
        float viewAspect = (float) width / height;
        if (viewAspect > targetAspect) {
            int w = (int) (height * targetAspect);
            int left = (width - w) / 2;
            dstRect.set(left, 0, left + w, height);
        } else {
            int h = (int) (width / targetAspect);
            int top = (height - h) / 2;
            dstRect.set(0, top, width, top + h);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        Choreographer.getInstance().removeFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        if (!running) return;

        DoomLib.nativeBlitFrame(frameBitmap);

        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            try {
                canvas.drawColor(0xFF000000); // чёрные полосы вне 4:3-области
                canvas.drawBitmap(frameBitmap, null, dstRect, null);
            } finally {
                getHolder().unlockCanvasAndPost(canvas);
            }
        }

        Choreographer.getInstance().postFrameCallback(this);
    }
}