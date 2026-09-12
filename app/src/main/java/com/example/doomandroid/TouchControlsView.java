package com.example.doomandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

/**
 * Оверлей управления поверх DoomSurfaceView:
 *  - слева: виртуальный джойстик движения (перевод в стрелки UP/DOWN/LEFT/RIGHT)
 *  - справа: кнопки FIRE, USE, ENTER
 *
 * ENTER нужен для навигации по меню Doom (New Game / Options / ...).
 * В самом геймплее FIRE стреляет, USE открывает двери.
 */
public class TouchControlsView extends View {

    private static final String TAG = "DoomTouch";
    private static final String PREFS = "doom_controls";

    private final Paint basePaint = new Paint();
    private final Paint stickPaint = new Paint();
    private final Paint buttonPaint = new Paint();
    private final Paint enterPaint = new Paint();
    private final Paint textPaint = new Paint();

    // Геометрия
    private float joyCenterX, joyCenterY, joyRadius;
    private float fireBtnX, fireBtnY, useBtnX, useBtnY, enterBtnX, enterBtnY, btnRadius;

    // Смещение внутреннего кружка джойстика относительно центра (для анимации)
    private float stickOffsetX = 0f, stickOffsetY = 0f;

    // pointerId -> zone: 1=fire, 2=use, 3=enter
    private final SparseIntArray pointerToZone = new SparseIntArray();
    private int joystickPointerId = -1;
    private int lastSentDirMask = 0;

    private static final int DIR_UP = 1, DIR_DOWN = 2, DIR_LEFT = 4, DIR_RIGHT = 8;

    public TouchControlsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        basePaint.setColor(Color.argb(80, 255, 255, 255));
        stickPaint.setColor(Color.argb(200, 255, 255, 255)); // сделал поярче
        buttonPaint.setColor(Color.argb(100, 255, 60, 60));
        enterPaint.setColor(Color.argb(120, 60, 160, 255));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        joyRadius = Math.min(w, h) * 0.15f;
        joyCenterX = prefs.getFloat("joy_x", joyRadius * 1.6f);
        joyCenterY = prefs.getFloat("joy_y", h - joyRadius * 1.6f);

        btnRadius = joyRadius * 0.7f;
        fireBtnX = prefs.getFloat("fire_x", w - btnRadius * 1.6f);
        fireBtnY = prefs.getFloat("fire_y", h - btnRadius * 1.6f);
        useBtnX = prefs.getFloat("use_x", w - btnRadius * 3.2f);
        useBtnY = prefs.getFloat("use_y", h - btnRadius * 3.2f);
        enterBtnX = prefs.getFloat("enter_x", w - btnRadius * 3.2f);
        enterBtnY = prefs.getFloat("enter_y", h - btnRadius * 5.0f);

        Log.d(TAG, "onSizeChanged w=" + w + " h=" + h
                + " joy=(" + joyCenterX + "," + joyCenterY + ") r=" + joyRadius
                + " fire=(" + fireBtnX + "," + fireBtnY + ")"
                + " use=(" + useBtnX + "," + useBtnY + ")"
                + " enter=(" + enterBtnX + "," + enterBtnY + ")"
                + " btnR=" + btnRadius);
    }

    public void saveLayout() {
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("joy_x", joyCenterX).putFloat("joy_y", joyCenterY)
                .putFloat("fire_x", fireBtnX).putFloat("fire_y", fireBtnY)
                .putFloat("use_x", useBtnX).putFloat("use_y", useBtnY)
                .putFloat("enter_x", enterBtnX).putFloat("enter_y", enterBtnY)
                .apply();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Внешний круг джойстика
        canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, basePaint);

        // Внутренний кружок — смещён в сторону нажатия
        float stickX = joyCenterX + stickOffsetX;
        float stickY = joyCenterY + stickOffsetY;
        canvas.drawCircle(stickX, stickY, joyRadius * 0.4f, stickPaint);

        canvas.drawCircle(fireBtnX, fireBtnY, btnRadius, buttonPaint);
        canvas.drawText("FIRE", fireBtnX, fireBtnY + 10, textPaint);

        canvas.drawCircle(useBtnX, useBtnY, btnRadius, buttonPaint);
        canvas.drawText("USE", useBtnX, useBtnY + 10, textPaint);

        canvas.drawCircle(enterBtnX, enterBtnY, btnRadius, enterPaint);
        canvas.drawText("ENTER", enterBtnX, enterBtnY + 10, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                float x = event.getX(index), y = event.getY(index);
                Log.d(TAG, "DOWN id=" + pointerId + " x=" + x + " y=" + y);

                if (dist(x, y, joyCenterX, joyCenterY) < joyRadius * 1.5f && joystickPointerId == -1) {
                    joystickPointerId = pointerId;
                    Log.d(TAG, " -> JOYSTICK");
                    updateJoystick(x, y);
                } else if (dist(x, y, fireBtnX, fireBtnY) < btnRadius) {
                    Log.d(TAG, " -> FIRE down");
                    pointerToZone.put(pointerId, 1);
                    DoomLib.nativeKeyEvent(true, DoomKeys.FIRE);
                } else if (dist(x, y, useBtnX, useBtnY) < btnRadius) {
                    Log.d(TAG, " -> USE down");
                    pointerToZone.put(pointerId, 2);
                    DoomLib.nativeKeyEvent(true, DoomKeys.USE);
                } else if (dist(x, y, enterBtnX, enterBtnY) < btnRadius) {
                    Log.d(TAG, " -> ENTER down");
                    pointerToZone.put(pointerId, 3);
                    DoomLib.nativeKeyEvent(true, DoomKeys.ENTER);
                } else {
                    Log.d(TAG, " -> NOTHING");
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pid = event.getPointerId(i);
                    if (pid == joystickPointerId) {
                        updateJoystick(event.getX(i), event.getY(i));
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                Log.d(TAG, "UP id=" + pointerId);
                if (pointerId == joystickPointerId) {
                    joystickPointerId = -1;
                    releaseAllDirections();
                    // Возвращаем внутренний кружок в центр
                    stickOffsetX = 0f;
                    stickOffsetY = 0f;
                }
                int zone = pointerToZone.get(pointerId, 0);
                if (zone == 1) {
                    Log.d(TAG, " -> FIRE up");
                    DoomLib.nativeKeyEvent(false, DoomKeys.FIRE);
                }
                if (zone == 2) {
                    Log.d(TAG, " -> USE up");
                    DoomLib.nativeKeyEvent(false, DoomKeys.USE);
                }
                if (zone == 3) {
                    Log.d(TAG, " -> ENTER up");
                    DoomLib.nativeKeyEvent(false, DoomKeys.ENTER);
                }
                pointerToZone.delete(pointerId);
                break;
            }
        }
        invalidate();
        return true;
    }

    private void updateJoystick(float x, float y) {
        float dx = x - joyCenterX;
        float dy = y - joyCenterY;

        // Ограничиваем длину смещения, чтобы кружок не улетал за пределы основания
        float maxOffset = joyRadius * 0.6f;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > maxOffset) {
            float k = maxOffset / len;
            dx *= k;
            dy *= k;
        }
        stickOffsetX = dx;
        stickOffsetY = dy;

        // Определяем направление по смещению (после ограничения)
        float deadZone = joyRadius * 0.35f;
        int mask = 0;
        if (dy < -deadZone) mask |= DIR_UP;
        if (dy > deadZone) mask |= DIR_DOWN;
        if (dx < -deadZone) mask |= DIR_LEFT;
        if (dx > deadZone) mask |= DIR_RIGHT;

        applyDirMask(mask);
    }

    private void applyDirMask(int newMask) {
        int changed = newMask ^ lastSentDirMask;
        if ((changed & DIR_UP) != 0) DoomLib.nativeKeyEvent((newMask & DIR_UP) != 0, DoomKeys.UPARROW);
        if ((changed & DIR_DOWN) != 0) DoomLib.nativeKeyEvent((newMask & DIR_DOWN) != 0, DoomKeys.DOWNARROW);
        if ((changed & DIR_LEFT) != 0) DoomLib.nativeKeyEvent((newMask & DIR_LEFT) != 0, DoomKeys.LEFTARROW);
        if ((changed & DIR_RIGHT) != 0) DoomLib.nativeKeyEvent((newMask & DIR_RIGHT) != 0, DoomKeys.RIGHTARROW);
        lastSentDirMask = newMask;
    }

    private void releaseAllDirections() {
        applyDirMask(0);
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}