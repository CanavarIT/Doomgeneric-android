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
 * Оверлей управления поверх DoomSurfaceView.
 *
 * Раскладка:
 *   - джойстик (слева внизу)
 *   - ESC, TAB (левый верх)
 *   - N, Y (правый верх, столбиком)
 *   - < > (над RUN/ENTER) — переключение оружия
 *   - RUN (правее ENTER) — тумблер бега
 *   - ENTER, USE, FIRE (справа снизу)
 */
public class TouchControlsView extends View {

    private static final String TAG = "DoomTouch";
    private static final String PREFS = "doom_controls";

    private final Paint basePaint = new Paint();
    private final Paint stickPaint = new Paint();
    private final Paint buttonPaint = new Paint();
    private final Paint enterPaint = new Paint();
    private final Paint specialPaint = new Paint();
    private final Paint weaponPaint = new Paint();
    private final Paint runOffPaint = new Paint();
    private final Paint runOnPaint = new Paint();
    private final Paint yesNoPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint smallTextPaint = new Paint();

    private float joyCenterX, joyCenterY, joyRadius;
    private float runBtnX, runBtnY;
    private float fireBtnX, fireBtnY;
    private float useBtnX, useBtnY;
    private float enterBtnX, enterBtnY;
    private float escBtnX, escBtnY;
    private float tabBtnX, tabBtnY;
    private float prevWpnBtnX, prevWpnBtnY;
    private float nextWpnBtnX, nextWpnBtnY;
    private float yesBtnX, yesBtnY;
    private float noBtnX, noBtnY;
    private float btnRadius;
    private float smallRadius;
    private float ynRadius;

    private float stickOffsetX = 0f, stickOffsetY = 0f;

    private final SparseIntArray pointerToZone = new SparseIntArray();
    private int joystickPointerId = -1;
    private int lastSentDirMask = 0;

    private boolean runActive = false;

    private int currentWeapon = 1;
    private int pendingPrevWeaponKey = 0;
    private int pendingNextWeaponKey = 0;

    private static final int DIR_UP = 1, DIR_DOWN = 2, DIR_LEFT = 4, DIR_RIGHT = 8;

    public TouchControlsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        basePaint.setColor(Color.argb(80, 255, 255, 255));
        stickPaint.setColor(Color.argb(200, 255, 255, 255));
        buttonPaint.setColor(Color.argb(100, 255, 60, 60));
        enterPaint.setColor(Color.argb(120, 60, 160, 255));
        specialPaint.setColor(Color.argb(120, 60, 200, 120));
        weaponPaint.setColor(Color.argb(120, 200, 140, 60));
        runOffPaint.setColor(Color.argb(140, 220, 60, 60));
        runOnPaint.setColor(Color.argb(180, 60, 140, 255));
        yesNoPaint.setColor(Color.argb(140, 220, 200, 60));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        smallTextPaint.setColor(Color.WHITE);
        smallTextPaint.setTextSize(22f);
        smallTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        joyRadius = Math.min(w, h) * 0.15f;
        joyCenterX = prefs.getFloat("joy_x", joyRadius * 1.6f);
        joyCenterY = prefs.getFloat("joy_y", h - joyRadius * 1.6f);

        btnRadius = joyRadius * 0.7f;
        smallRadius = joyRadius * 0.42f;   // чуть меньше — чтобы < > не выходили на экран
        ynRadius = joyRadius * 0.38f;

        // FIRE, USE — как было
        fireBtnX  = prefs.getFloat("fire_x",  w - btnRadius * 1.6f);
        fireBtnY  = prefs.getFloat("fire_y",  h - btnRadius * 1.6f);
        useBtnX   = prefs.getFloat("use_x",   w - btnRadius * 3.2f);
        useBtnY   = prefs.getFloat("use_y",   h - btnRadius * 3.2f);

        // ENTER — как было
        enterBtnX = w - btnRadius * 3.2f;
        enterBtnY = h - btnRadius * 5.0f;

        // RUN — правее ENTER (за ним, ближе к краю)
        runBtnX = w - btnRadius * 1.4f;
        runBtnY = h - btnRadius * 5.0f;

        // ESC и TAB — в одну строку, слева вверху
        escBtnX = smallRadius * 1.2f;
        escBtnY = smallRadius * 1.2f;
        tabBtnX = smallRadius * 3.4f;
        tabBtnY = smallRadius * 1.2f;

        // < и > — над RUN/ENTER, сдвинуты правее, чтобы не заходить на экран Doom
        float wpnCenterX = w - btnRadius * 2.3f;
        float wpnRowY = h - btnRadius * 6.8f;
        prevWpnBtnX = wpnCenterX - smallRadius * 1.4f;
        prevWpnBtnY = wpnRowY;
        nextWpnBtnX = wpnCenterX + smallRadius * 1.4f;
        nextWpnBtnY = wpnRowY;

        // Y/N — правый верх, столбиком: N сверху, Y под ним
        noBtnX  = w - ynRadius * 1.4f;
        noBtnY  = ynRadius * 1.4f;
        yesBtnX = w - ynRadius * 1.4f;
        yesBtnY = ynRadius * 3.6f;

        Log.d(TAG, "onSizeChanged w=" + w + " h=" + h
                + " joy=(" + joyCenterX + "," + joyCenterY + ") r=" + joyRadius
                + " btnR=" + btnRadius + " smallR=" + smallRadius + " ynR=" + ynRadius);
    }

    public void saveLayout() {
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putFloat("joy_x", joyCenterX).putFloat("joy_y", joyCenterY)
                .putFloat("fire_x", fireBtnX).putFloat("fire_y", fireBtnY)
                .putFloat("use_x", useBtnX).putFloat("use_y", useBtnY)
                .apply();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Джойстик
        canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, basePaint);
        canvas.drawCircle(joyCenterX + stickOffsetX, joyCenterY + stickOffsetY,
                          joyRadius * 0.4f, stickPaint);

        // ESC и TAB
        canvas.drawCircle(escBtnX, escBtnY, smallRadius, specialPaint);
        canvas.drawText("ESC", escBtnX, escBtnY + 8, smallTextPaint);

        canvas.drawCircle(tabBtnX, tabBtnY, smallRadius, specialPaint);
        canvas.drawText("TAB", tabBtnX, tabBtnY + 8, smallTextPaint);

        // Правый нижний кластер
        canvas.drawCircle(fireBtnX, fireBtnY, btnRadius, buttonPaint);
        canvas.drawText("FIRE", fireBtnX, fireBtnY + 10, textPaint);

        canvas.drawCircle(useBtnX, useBtnY, btnRadius, buttonPaint);
        canvas.drawText("USE", useBtnX, useBtnY + 10, textPaint);

        canvas.drawCircle(enterBtnX, enterBtnY, btnRadius, enterPaint);
        canvas.drawText("ENTER", enterBtnX, enterBtnY + 10, textPaint);

        // RUN — правее ENTER
        canvas.drawCircle(runBtnX, runBtnY, btnRadius, runActive ? runOnPaint : runOffPaint);
        canvas.drawText("RUN", runBtnX, runBtnY + 10, textPaint);

        // < и > — над RUN/ENTER
        canvas.drawCircle(prevWpnBtnX, prevWpnBtnY, smallRadius, weaponPaint);
        canvas.drawText("<", prevWpnBtnX, prevWpnBtnY + 12, textPaint);

        canvas.drawCircle(nextWpnBtnX, nextWpnBtnY, smallRadius, weaponPaint);
        canvas.drawText(">", nextWpnBtnX, nextWpnBtnY + 12, textPaint);

        // Y / N — правый верх
        canvas.drawCircle(noBtnX, noBtnY, ynRadius, yesNoPaint);
        canvas.drawText("N", noBtnX, noBtnY + 8, smallTextPaint);

        canvas.drawCircle(yesBtnX, yesBtnY, ynRadius, yesNoPaint);
        canvas.drawText("Y", yesBtnX, yesBtnY + 8, smallTextPaint);
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

                if (dist(x, y, joyCenterX, joyCenterY) < joyRadius * 1.5f && joystickPointerId == -1) {
                    joystickPointerId = pointerId;
                    updateJoystick(x, y);
                }
                else if (dist(x, y, fireBtnX, fireBtnY) < btnRadius)  { press(pointerId, 1, DoomKeys.FIRE);   }
                else if (dist(x, y, useBtnX, useBtnY) < btnRadius)    { press(pointerId, 2, DoomKeys.USE);    }
                else if (dist(x, y, enterBtnX, enterBtnY) < btnRadius){ press(pointerId, 3, DoomKeys.ENTER);  }
                else if (dist(x, y, escBtnX, escBtnY) < smallRadius)  { press(pointerId, 4, DoomKeys.ESCAPE); }
                else if (dist(x, y, tabBtnX, tabBtnY) < smallRadius)  { press(pointerId, 5, DoomKeys.TAB);    }
                else if (dist(x, y, runBtnX, runBtnY) < btnRadius)    { toggleRun(); }
                else if (dist(x, y, prevWpnBtnX, prevWpnBtnY) < smallRadius) { pressPrevWeapon(pointerId); }
                else if (dist(x, y, nextWpnBtnX, nextWpnBtnY) < smallRadius) { pressNextWeapon(pointerId); }
                else if (dist(x, y, yesBtnX, yesBtnY) < ynRadius)     { press(pointerId, 10, DoomKeys.Y); }
                else if (dist(x, y, noBtnX,  noBtnY)  < ynRadius)     { press(pointerId, 11, DoomKeys.N); }
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
                if (pointerId == joystickPointerId) {
                    joystickPointerId = -1;
                    releaseAllDirections();
                    stickOffsetX = 0f;
                    stickOffsetY = 0f;
                }
                int zone = pointerToZone.get(pointerId, 0);
                switch (zone) {
                    case 1:  DoomLib.nativeKeyEvent(false, DoomKeys.FIRE);   break;
                    case 2:  DoomLib.nativeKeyEvent(false, DoomKeys.USE);    break;
                    case 3:  DoomLib.nativeKeyEvent(false, DoomKeys.ENTER);  break;
                    case 4:  DoomLib.nativeKeyEvent(false, DoomKeys.ESCAPE); break;
                    case 5:  DoomLib.nativeKeyEvent(false, DoomKeys.TAB);    break;
                    case 7:  releasePrevWeapon(); break;
                    case 8:  releaseNextWeapon(); break;
                    case 10: DoomLib.nativeKeyEvent(false, DoomKeys.Y);      break;
                    case 11: DoomLib.nativeKeyEvent(false, DoomKeys.N);      break;
                }
                pointerToZone.delete(pointerId);
                break;
            }
        }
        invalidate();
        return true;
    }

    private void toggleRun() {
        runActive = !runActive;
        if (runActive) {
            DoomLib.nativeKeyEvent(true, DoomKeys.RSHIFT);
            Log.d(TAG, "RUN: on");
        } else {
            DoomLib.nativeKeyEvent(false, DoomKeys.RSHIFT);
            Log.d(TAG, "RUN: off");
        }
    }

    private void press(int pointerId, int zone, int doomKey) {
        pointerToZone.put(pointerId, zone);
        DoomLib.nativeKeyEvent(true, doomKey);
    }

    private void pressPrevWeapon(int pointerId) {
        pointerToZone.put(pointerId, 7);
        currentWeapon--;
        if (currentWeapon < 1) currentWeapon = 7;
        int key = weaponKey(currentWeapon);
        pendingPrevWeaponKey = key;
        DoomLib.nativeKeyEvent(true, key);
        Log.d(TAG, "prev weapon → slot " + currentWeapon);
    }

    private void releasePrevWeapon() {
        if (pendingPrevWeaponKey != 0) {
            DoomLib.nativeKeyEvent(false, pendingPrevWeaponKey);
            pendingPrevWeaponKey = 0;
        }
    }

    private void pressNextWeapon(int pointerId) {
        pointerToZone.put(pointerId, 8);
        currentWeapon++;
        if (currentWeapon > 7) currentWeapon = 1;
        int key = weaponKey(currentWeapon);
        pendingNextWeaponKey = key;
        DoomLib.nativeKeyEvent(true, key);
        Log.d(TAG, "next weapon → slot " + currentWeapon);
    }

    private void releaseNextWeapon() {
        if (pendingNextWeaponKey != 0) {
            DoomLib.nativeKeyEvent(false, pendingNextWeaponKey);
            pendingNextWeaponKey = 0;
        }
    }

    private static int weaponKey(int slot) {
        switch (slot) {
            case 1: return DoomKeys.WPN_1;
            case 2: return DoomKeys.WPN_2;
            case 3: return DoomKeys.WPN_3;
            case 4: return DoomKeys.WPN_4;
            case 5: return DoomKeys.WPN_5;
            case 6: return DoomKeys.WPN_6;
            default: return DoomKeys.WPN_7;
        }
    }

    private void updateJoystick(float x, float y) {
        float dx = x - joyCenterX;
        float dy = y - joyCenterY;

        float maxOffset = joyRadius * 0.6f;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > maxOffset) {
            float k = maxOffset / len;
            dx *= k;
            dy *= k;
        }
        stickOffsetX = dx;
        stickOffsetY = dy;

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
        if ((changed & DIR_UP) != 0)    DoomLib.nativeKeyEvent((newMask & DIR_UP) != 0, DoomKeys.UPARROW);
        if ((changed & DIR_DOWN) != 0)  DoomLib.nativeKeyEvent((newMask & DIR_DOWN) != 0, DoomKeys.DOWNARROW);
        if ((changed & DIR_LEFT) != 0)  DoomLib.nativeKeyEvent((newMask & DIR_LEFT) != 0, DoomKeys.LEFTARROW);
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