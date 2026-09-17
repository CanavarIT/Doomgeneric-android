package com.example.doomandroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class KeyboardView extends View {

    private static final String[] ROW1 = {"q","w","e","r","t","y","u","i","o","p"};
    private static final String[] ROW2 = {"a","s","d","f","g","h","j","k","l"};
    private static final String[] ROW3 = {"z","x","c","v","b","n","m"};
    private static final String[] ROW4 = {"0","1","2","3","4","5","6","7","8","9"};
    private static final String[] ROW5 = {"⌫", "SPACE", "↵", "DEL"};

    private final Paint keyPaint = new Paint();
    private final Paint keyActivePaint = new Paint();
    private final Paint delPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();

    private int viewW, viewH;
    private float rowH;
    private String activeKey = null;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public KeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        keyPaint.setColor(Color.argb(220, 40, 40, 40));
        keyActivePaint.setColor(Color.argb(240, 80, 120, 200));
        delPaint.setColor(Color.argb(220, 180, 40, 40));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        bgPaint.setColor(Color.argb(200, 0, 0, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewW = w;
        viewH = h;
        rowH = h / 5.5f;
        textPaint.setTextSize(rowH * 0.5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(0, 0, viewW, viewH, bgPaint);
        float gap = rowH * 0.05f;
        float y = gap;

        drawRow(canvas, ROW1, y, gap);
        y += rowH + gap;
        drawRow(canvas, ROW2, y, gap);
        y += rowH + gap;
        drawRow(canvas, ROW3, y, gap);
        y += rowH + gap;
        drawRow(canvas, ROW4, y, gap);
        y += rowH + gap;
        drawBottomRow(canvas, ROW5, y, gap);
    }

    private void drawRow(Canvas canvas, String[] keys, float y, float gap) {
        int n = keys.length;
        float keyW = (viewW - (n + 1) * gap) / n;
        float x = gap;

        for (String key : keys) {
            RectF rect = new RectF(x, y, x + keyW, y + rowH);
            boolean active = key.equals(activeKey);
            canvas.drawRoundRect(rect, rowH * 0.1f, rowH * 0.1f,
                    active ? keyActivePaint : keyPaint);

            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textY = y + rowH / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(key, x + keyW / 2f, textY, textPaint);
            x += keyW + gap;
        }
    }

    private void drawBottomRow(Canvas canvas, String[] keys, float y, float gap) {
        float totalUnits = 5f;
        float unitW = (viewW - 5 * gap) / totalUnits;
        float x = gap;

        for (int i = 0; i < keys.length; i++) {
            float w;
            if (i == 1) w = unitW * 2;
            else w = unitW;

            RectF rect = new RectF(x, y, x + w, y + rowH);
            boolean active = keys[i].equals(activeKey);
            Paint p = "DEL".equals(keys[i]) ? delPaint : keyPaint;
            canvas.drawRoundRect(rect, rowH * 0.1f, rowH * 0.1f,
                    active ? keyActivePaint : p);

            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textY = y + rowH / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(keys[i], x + w / 2f, textY, textPaint);
            x += w + gap;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                String key = hitKey(x, y);
                if (key != null) {
                    activeKey = key;
                    sendKey(key);
                    invalidate();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activeKey = null;
                invalidate();
                return true;
            }
        }
        return true;
    }

    private String hitKey(float x, float y) {
        float gap = rowH * 0.05f;
        int row = (int) ((y - gap) / (rowH + gap));
        if (row < 0 || row > 4) return null;

        float rowY = gap + row * (rowH + gap);
        if (y < rowY || y > rowY + rowH) return null;

        if (row < 4) {
            String[] keys;
            switch (row) {
                case 0: keys = ROW1; break;
                case 1: keys = ROW2; break;
                case 2: keys = ROW3; break;
                default: keys = ROW4; break;
            }
            int n = keys.length;
            float keyW = (viewW - (n + 1) * gap) / n;
            int col = (int) ((x - gap) / (keyW + gap));
            if (col < 0 || col >= n) return null;
            float keyX = gap + col * (keyW + gap);
            if (x < keyX || x > keyX + keyW) return null;
            return keys[col];
        } else {
            float totalUnits = 5f;
            float unitW = (viewW - 5 * gap) / totalUnits;
            float xPos = gap;
            for (int i = 0; i < ROW5.length; i++) {
                float w = (i == 1) ? unitW * 2 : unitW;
                if (x >= xPos && x <= xPos + w) {
                    return ROW5[i];
                }
                xPos += w + gap;
            }
        }
        return null;
    }

    private void sendKey(String key) {
        if ("DEL".equals(key)) {
            for (int i = 0; i < 24; i++) {
                DoomLib.nativeKeyEvent(true, DoomKeys.BACKSPACE);
                DoomLib.nativeKeyEvent(false, DoomKeys.BACKSPACE);
            }
            return;
        }

        int code = mapKeyToAscii(key);
        if (code <= 0) return;

        DoomLib.nativeKeyEvent(true, code);
        long delay = "⌫".equals(key) ? 40 : 20;
        handler.postDelayed(() -> DoomLib.nativeKeyEvent(false, code), delay);
    }

    private int mapKeyToAscii(String key) {
        switch (key) {
            case "SPACE": return DoomKeys.SPACE;
            case "⌫":     return DoomKeys.BACKSPACE;
            case "↵":     return DoomKeys.ENTER;
        }
        if (key.length() == 1) {
            char c = key.charAt(0);
            if (c >= 'a' && c <= 'z') return (int) c;
            if (c >= '0' && c <= '9') return (int) c;
        }
        return 0;
    }
}