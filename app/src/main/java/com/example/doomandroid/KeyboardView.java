package com.example.doomandroid;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Экранная клавиатура для ввода имени сейва в Doom.
 * Появляется по кнопке "⌨" и перекрывает нижнюю часть экрана.
 *
 * Каждая кнопка — один тап, отправляет keyDown + keyUp через JNI.
 */
public class KeyboardView extends View {

    private static final String TAG = "DoomKeyboard";

    // Раскладка: строки символов
    private static final String[] ROW1 = {"q","w","e","r","t","y","u","i","o","p"};
    private static final String[] ROW2 = {"a","s","d","f","g","h","j","k","l"};
    private static final String[] ROW3 = {"z","x","c","v","b","n","m"};
    private static final String[] ROW4 = {"0","1","2","3","4","5","6","7","8","9"};
    // Управление: backspace, space, enter
    private static final String[] ROW5 = {"⌫", "SPACE", "↵"};

    private final Paint keyPaint = new Paint();
    private final Paint keyActivePaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint bgPaint = new Paint();

    private int viewW, viewH;
    private float rowH;

    // Какую кнопку держим сейчас (для подсветки)
    private String activeKey = null;

    public KeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        keyPaint.setColor(Color.argb(220, 40, 40, 40));
        keyActivePaint.setColor(Color.argb(240, 80, 120, 200));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        bgPaint.setColor(Color.argb(200, 0, 0, 0));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewW = w;
        viewH = h;
        rowH = h / 5.5f;   // 5 строк + небольшой зазор

        // Адаптивный размер шрифта
        textPaint.setTextSize(rowH * 0.55f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Полупрозрачный фон
        canvas.drawRect(0, 0, viewW, viewH, bgPaint);

        float gap = rowH * 0.05f;

        // Начальная Y (от верха панели)
        float y = gap;

        // Row 1: qwertyuiop
        drawRow(canvas, ROW1, y, gap);
        y += rowH + gap;

        // Row 2: asdfghjkl (с отступом)
        drawRow(canvas, ROW2, y, gap);
        y += rowH + gap;

        // Row 3: zxcvbnm
        drawRow(canvas, ROW3, y, gap);
        y += rowH + gap;

        // Row 4: 0-9
        drawRow(canvas, ROW4, y, gap);
        y += rowH + gap;

        // Row 5: управление — три широкие кнопки
        drawBottomRow(canvas, ROW5, y, gap);
    }

    /** Рисует строку символов, распределяя их по ширине. */
    private void drawRow(Canvas canvas, String[] keys, float y, float gap) {
        int n = keys.length;
        float keyW = (viewW - (n + 1) * gap) / n;
        float x = gap;

        for (String key : keys) {
            RectF rect = new RectF(x, y, x + keyW, y + rowH);
            boolean active = key.equals(activeKey);
            canvas.drawRoundRect(rect, rowH * 0.1f, rowH * 0.1f,
                    active ? keyActivePaint : keyPaint);

            // Центр текста
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textY = y + rowH / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(key, x + keyW / 2f, textY, textPaint);

            x += keyW + gap;
        }
    }

    /** Рисует нижнюю строку управления (Backspace / Space / Enter). */
    private void drawBottomRow(Canvas canvas, String[] keys, float y, float gap) {
        // Соотношение ширины: backspace : space : enter = 1 : 2 : 1
        float totalUnits = 4;
        float unitW = (viewW - 4 * gap) / totalUnits;
        float x = gap;

        for (int i = 0; i < keys.length; i++) {
            float w = (i == 1) ? unitW * 2 : unitW;   // space в 2 раза шире
            RectF rect = new RectF(x, y, x + w, y + rowH);
            boolean active = keys[i].equals(activeKey);
            canvas.drawRoundRect(rect, rowH * 0.1f, rowH * 0.1f,
                    active ? keyActivePaint : keyPaint);

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

    /** Определяет, по какой клавише попал тап. Возвращает строку или null. */
    private String hitKey(float x, float y) {
        float gap = rowH * 0.05f;
        int row = (int)((y - gap) / (rowH + gap));
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
            int col = (int)((x - gap) / (keyW + gap));
            if (col < 0 || col >= n) return null;
            float keyX = gap + col * (keyW + gap);
            if (x < keyX || x > keyX + keyW) return null;
            return keys[col];
        } else {
            // Нижняя строка
            float totalUnits = 4;
            float unitW = (viewW - 4 * gap) / totalUnits;
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

    /** Отправляет нажатие клавиши в Doom через JNI. */
    private void sendKey(String key) {
        int code = mapKeyToAscii(key);
        if (code <= 0) return;

        Log.d(TAG, "key: " + key + " (code=" + code + ")");

        DoomLib.nativeKeyEvent(true, code);
        DoomLib.nativeKeyEvent(false, code);
    }

    /** Преобразует строку-клавишу в ASCII-код, понятный Doom. */
    private int mapKeyToAscii(String key) {
        if (key.length() == 1) {
            char c = key.charAt(0);
            if (c >= 'a' && c <= 'z') return (int) c;
            if (c >= '0' && c <= '9') return (int) c;
            return 0;
        }
        switch (key) {
            case "SPACE": return DoomKeys.SPACE;
            case "⌫":     return DoomKeys.BACKSPACE;
            case "↵":     return DoomKeys.ENTER;
            default:      return 0;
        }
    }
}