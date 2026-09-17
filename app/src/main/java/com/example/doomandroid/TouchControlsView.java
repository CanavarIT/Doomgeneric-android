package com.example.doomandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

public class TouchControlsView extends View {

    private static final String PREFS = "doom_controls";
    private static final long TRIPLE_TAP_TIMEOUT = 400;

    private final Paint basePaint = new Paint();
    private final Paint stickPaint = new Paint();
    private final Paint buttonPaint = new Paint();
    private final Paint enterPaint = new Paint();
    private final Paint specialPaint = new Paint();
    private final Paint weaponPaint = new Paint();
    private final Paint runOffPaint = new Paint();
    private final Paint runOnPaint = new Paint();
    private final Paint yesNoPaint = new Paint();
    private final Paint keyboardPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint smallTextPaint = new Paint();
    private final Paint editHighlightPaint = new Paint();
    private final Paint editBtnPaint = new Paint();
    private final Paint editTextPaint = new Paint();
    private final Paint deleteBtnPaint = new Paint();

    // Позиции и радиусы — каждый независимый
    private float joyCenterX, joyCenterY, joyRadius;
    private float fireBtnX, fireBtnY, fireRadius;
    private float useBtnX, useBtnY, useRadius;
    private float enterBtnX, enterBtnY, enterRadius;
    private float runBtnX, runBtnY, runRadius;
    private float escBtnX, escBtnY, escRadius;
    private float tabBtnX, tabBtnY, tabRadius;
    private float prevWpnBtnX, prevWpnBtnY, prevWpnRadius;
    private float nextWpnBtnX, nextWpnBtnY, nextWpnRadius;
    private float yesBtnX, yesBtnY, yesRadius;
    private float noBtnX, noBtnY, noRadius;
    private float keyboardBtnX, keyboardBtnY, keyboardRadius;

    // Удалённые кнопки
    private boolean[] deleted = new boolean[12];

    private float stickOffsetX = 0f, stickOffsetY = 0f;
    private final SparseIntArray pointerToZone = new SparseIntArray();
    private int joystickPointerId = -1;
    private int lastSentDirMask = 0;
    private boolean runActive = false;
    private int currentWeapon = 1;
    private int pendingPrevWeaponKey = 0;
    private int pendingNextWeaponKey = 0;

    private boolean editMode = false;
    private int selectedControl = -1;
    private int dragPointerId = -1;
    private float dragOffsetX, dragOffsetY;
    private float pinchStartDist = 0f;
    private float pinchStartRadius = 0f;
    private boolean isPinching = false;

    private int tapCount = 0;
    private long lastTapTime = 0;

    private float resetBtnX, resetBtnY, doneBtnX, doneBtnY, deleteBtnX, deleteBtnY;
    private float editBtnW = 130f, editBtnH = 55f;

    public interface OnKeyboardToggleListener {
        void onToggle();
    }
    private OnKeyboardToggleListener keyboardToggleListener = null;

    public void setOnKeyboardToggleListener(OnKeyboardToggleListener l) {
        this.keyboardToggleListener = l;
    }

    private static final int DIR_UP = 1, DIR_DOWN = 2, DIR_LEFT = 4, DIR_RIGHT = 8;

    private static final int CTRL_JOY = 0;
    private static final int CTRL_FIRE = 1;
    private static final int CTRL_USE = 2;
    private static final int CTRL_ENTER = 3;
    private static final int CTRL_RUN = 4;
    private static final int CTRL_ESC = 5;
    private static final int CTRL_TAB = 6;
    private static final int CTRL_PREV = 7;
    private static final int CTRL_NEXT = 8;
    private static final int CTRL_YES = 9;
    private static final int CTRL_NO = 10;
    private static final int CTRL_KEYBOARD = 11;

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
        keyboardPaint.setColor(Color.argb(160, 160, 80, 220));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        smallTextPaint.setColor(Color.WHITE);
        smallTextPaint.setTextSize(22f);
        smallTextPaint.setTextAlign(Paint.Align.CENTER);

        editHighlightPaint.setColor(Color.argb(180, 255, 255, 0));
        editHighlightPaint.setStyle(Paint.Style.STROKE);
        editHighlightPaint.setStrokeWidth(6f);
        editBtnPaint.setColor(Color.argb(220, 30, 30, 30));
        deleteBtnPaint.setColor(Color.argb(220, 180, 40, 40));
        editTextPaint.setColor(Color.WHITE);
        editTextPaint.setTextSize(26f);
        editTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        loadLayout(w, h);

        resetBtnX = 20 + editBtnW / 2;
        resetBtnY = 30 + editBtnH / 2;
        doneBtnX = w - 20 - editBtnW / 2;
        doneBtnY = 30 + editBtnH / 2;
        deleteBtnX = w / 2f;
        deleteBtnY = 30 + editBtnH / 2;
    }

    private void loadLayout(int w, int h) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        float defJoyR = Math.min(w, h) * 0.15f;
        float defBtnR = defJoyR * 0.7f;
        float defSmallR = defJoyR * 0.42f;
        float defYnR = defJoyR * 0.38f;

        joyRadius = prefs.getFloat("joy_r", defJoyR);
        joyCenterX = prefs.getFloat("joy_x", joyRadius * 1.6f);
        joyCenterY = prefs.getFloat("joy_y", h - joyRadius * 1.6f);

        fireRadius = prefs.getFloat("fire_r", defBtnR);
        fireBtnX = prefs.getFloat("fire_x", w - fireRadius * 1.6f);
        fireBtnY = prefs.getFloat("fire_y", h - fireRadius * 1.3f);

        useRadius = prefs.getFloat("use_r", defBtnR);
        useBtnX = prefs.getFloat("use_x", w - useRadius * 3.2f);
        useBtnY = prefs.getFloat("use_y", h - useRadius * 2.9f);

        enterRadius = prefs.getFloat("enter_r", defBtnR);
        enterBtnX = prefs.getFloat("enter_x", w - enterRadius * 3.2f);
        enterBtnY = prefs.getFloat("enter_y", h - enterRadius * 5.0f);

        runRadius = prefs.getFloat("run_r", defBtnR);
        runBtnX = prefs.getFloat("run_x", w - runRadius * 1.1f);
        runBtnY = prefs.getFloat("run_y", h - runRadius * 5.0f);

        escRadius = prefs.getFloat("esc_r", defSmallR);
        escBtnX = prefs.getFloat("esc_x", escRadius * 1.2f);
        escBtnY = prefs.getFloat("esc_y", escRadius * 1.2f);

        tabRadius = prefs.getFloat("tab_r", defSmallR);
        tabBtnX = prefs.getFloat("tab_x", tabRadius * 3.4f);
        tabBtnY = prefs.getFloat("tab_y", tabRadius * 1.2f);

        prevWpnRadius = prefs.getFloat("prev_r", defSmallR);
        nextWpnRadius = prefs.getFloat("next_r", defSmallR);
        float wpnCenterX = w - defBtnR * 2.3f;
        float wpnRowY = h - defBtnR * 6.8f;
        prevWpnBtnX = prefs.getFloat("prev_x", wpnCenterX - defSmallR * 1.4f);
        prevWpnBtnY = prefs.getFloat("prev_y", wpnRowY);
        nextWpnBtnX = prefs.getFloat("next_x", wpnCenterX + defSmallR * 1.0f);
        nextWpnBtnY = prefs.getFloat("next_y", wpnRowY);

        yesRadius = prefs.getFloat("yes_r", defYnR);
        noRadius = prefs.getFloat("no_r", defYnR);
        keyboardRadius = prefs.getFloat("kb_r", defYnR);

        noBtnX = prefs.getFloat("no_x", w - noRadius * 1.4f);
        noBtnY = prefs.getFloat("no_y", noRadius * 1.4f);
        yesBtnX = prefs.getFloat("yes_x", w - yesRadius * 1.4f);
        yesBtnY = prefs.getFloat("yes_y", yesRadius * 3.6f);
        keyboardBtnX = prefs.getFloat("kb_x", w - keyboardRadius * 4.0f);
        keyboardBtnY = prefs.getFloat("kb_y", keyboardRadius * 1.4f);

        for (int i = 0; i < 12; i++) {
            deleted[i] = prefs.getBoolean("del_" + i, false);
        }
    }

    public void saveLayout() {
        SharedPreferences.Editor ed = getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
        ed.putFloat("joy_x", joyCenterX).putFloat("joy_y", joyCenterY).putFloat("joy_r", joyRadius)
          .putFloat("fire_x", fireBtnX).putFloat("fire_y", fireBtnY).putFloat("fire_r", fireRadius)
          .putFloat("use_x", useBtnX).putFloat("use_y", useBtnY).putFloat("use_r", useRadius)
          .putFloat("enter_x", enterBtnX).putFloat("enter_y", enterBtnY).putFloat("enter_r", enterRadius)
          .putFloat("run_x", runBtnX).putFloat("run_y", runBtnY).putFloat("run_r", runRadius)
          .putFloat("esc_x", escBtnX).putFloat("esc_y", escBtnY).putFloat("esc_r", escRadius)
          .putFloat("tab_x", tabBtnX).putFloat("tab_y", tabBtnY).putFloat("tab_r", tabRadius)
          .putFloat("prev_x", prevWpnBtnX).putFloat("prev_y", prevWpnBtnY).putFloat("prev_r", prevWpnRadius)
          .putFloat("next_x", nextWpnBtnX).putFloat("next_y", nextWpnBtnY).putFloat("next_r", nextWpnRadius)
          .putFloat("yes_x", yesBtnX).putFloat("yes_y", yesBtnY).putFloat("yes_r", yesRadius)
          .putFloat("no_x", noBtnX).putFloat("no_y", noBtnY).putFloat("no_r", noRadius)
          .putFloat("kb_x", keyboardBtnX).putFloat("kb_y", keyboardBtnY).putFloat("kb_r", keyboardRadius);

        for (int i = 0; i < 12; i++) {
            ed.putBoolean("del_" + i, deleted[i]);
        }
        ed.apply();
    }

    private void resetToDefault() {
        getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
        for (int i = 0; i < 12; i++) deleted[i] = false;
        loadLayout(getWidth(), getHeight());
        selectedControl = -1;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!deleted[CTRL_JOY]) {
            canvas.drawCircle(joyCenterX, joyCenterY, joyRadius, basePaint);
            canvas.drawCircle(joyCenterX + stickOffsetX, joyCenterY + stickOffsetY, joyRadius * 0.4f, stickPaint);
        }

        if (!deleted[CTRL_ESC]) {
            canvas.drawCircle(escBtnX, escBtnY, escRadius, specialPaint);
            canvas.drawText("ESC", escBtnX, escBtnY + 8, smallTextPaint);
        }
        if (!deleted[CTRL_TAB]) {
            canvas.drawCircle(tabBtnX, tabBtnY, tabRadius, specialPaint);
            canvas.drawText("TAB", tabBtnX, tabBtnY + 8, smallTextPaint);
        }
        if (!deleted[CTRL_FIRE]) {
            canvas.drawCircle(fireBtnX, fireBtnY, fireRadius, buttonPaint);
            canvas.drawText("FIRE", fireBtnX, fireBtnY + 10, textPaint);
        }
        if (!deleted[CTRL_USE]) {
            canvas.drawCircle(useBtnX, useBtnY, useRadius, buttonPaint);
            canvas.drawText("USE", useBtnX, useBtnY + 10, textPaint);
        }
        if (!deleted[CTRL_ENTER]) {
            canvas.drawCircle(enterBtnX, enterBtnY, enterRadius, enterPaint);
            canvas.drawText("ENTER", enterBtnX, enterBtnY + 10, textPaint);
        }
        if (!deleted[CTRL_RUN]) {
            canvas.drawCircle(runBtnX, runBtnY, runRadius, runActive ? runOnPaint : runOffPaint);
            canvas.drawText("RUN", runBtnX, runBtnY + 10, textPaint);
        }
        if (!deleted[CTRL_PREV]) {
            canvas.drawCircle(prevWpnBtnX, prevWpnBtnY, prevWpnRadius, weaponPaint);
            canvas.drawText("<", prevWpnBtnX, prevWpnBtnY + 12, textPaint);
        }
        if (!deleted[CTRL_NEXT]) {
            canvas.drawCircle(nextWpnBtnX, nextWpnBtnY, nextWpnRadius, weaponPaint);
            canvas.drawText(">", nextWpnBtnX, nextWpnBtnY + 12, textPaint);
        }
        if (!deleted[CTRL_NO]) {
            canvas.drawCircle(noBtnX, noBtnY, noRadius, yesNoPaint);
            canvas.drawText("N", noBtnX, noBtnY + 8, smallTextPaint);
        }
        if (!deleted[CTRL_YES]) {
            canvas.drawCircle(yesBtnX, yesBtnY, yesRadius, yesNoPaint);
            canvas.drawText("Y", yesBtnX, yesBtnY + 8, smallTextPaint);
        }
        if (!deleted[CTRL_KEYBOARD]) {
            canvas.drawCircle(keyboardBtnX, keyboardBtnY, keyboardRadius, keyboardPaint);
            canvas.drawText("⌨", keyboardBtnX, keyboardBtnY + 10, smallTextPaint);
        }

        if (editMode) {
            drawEditHighlight(canvas);
            drawEditButtons(canvas);
        }
    }

    private void drawEditHighlight(Canvas canvas) {
        if (selectedControl < 0 || deleted[selectedControl]) return;
        float[] pos = getControlPos(selectedControl);
        float r = getControlRadius(selectedControl);
        if (pos != null) {
            canvas.drawCircle(pos[0], pos[1], r + 8, editHighlightPaint);
        }
    }

    private void drawEditButtons(Canvas canvas) {
        canvas.drawRoundRect(resetBtnX - editBtnW / 2, resetBtnY - editBtnH / 2,
                resetBtnX + editBtnW / 2, resetBtnY + editBtnH / 2, 12, 12, editBtnPaint);
        canvas.drawText("RESET", resetBtnX, resetBtnY + 9, editTextPaint);

        canvas.drawRoundRect(doneBtnX - editBtnW / 2, doneBtnY - editBtnH / 2,
                doneBtnX + editBtnW / 2, doneBtnY + editBtnH / 2, 12, 12, editBtnPaint);
        canvas.drawText("DONE", doneBtnX, doneBtnY + 9, editTextPaint);

        if (selectedControl >= 0 && !deleted[selectedControl]) {
            canvas.drawRoundRect(deleteBtnX - editBtnW / 2, deleteBtnY - editBtnH / 2,
                    deleteBtnX + editBtnW / 2, deleteBtnY + editBtnH / 2, 12, 12, deleteBtnPaint);
            canvas.drawText("DELETE", deleteBtnX, deleteBtnY + 9, editTextPaint);
        }
    }

    private float[] getControlPos(int id) {
        switch (id) {
            case CTRL_JOY: return new float[]{joyCenterX, joyCenterY};
            case CTRL_FIRE: return new float[]{fireBtnX, fireBtnY};
            case CTRL_USE: return new float[]{useBtnX, useBtnY};
            case CTRL_ENTER: return new float[]{enterBtnX, enterBtnY};
            case CTRL_RUN: return new float[]{runBtnX, runBtnY};
            case CTRL_ESC: return new float[]{escBtnX, escBtnY};
            case CTRL_TAB: return new float[]{tabBtnX, tabBtnY};
            case CTRL_PREV: return new float[]{prevWpnBtnX, prevWpnBtnY};
            case CTRL_NEXT: return new float[]{nextWpnBtnX, nextWpnBtnY};
            case CTRL_YES: return new float[]{yesBtnX, yesBtnY};
            case CTRL_NO: return new float[]{noBtnX, noBtnY};
            case CTRL_KEYBOARD: return new float[]{keyboardBtnX, keyboardBtnY};
            default: return null;
        }
    }

    private float getControlRadius(int id) {
        switch (id) {
            case CTRL_JOY: return joyRadius;
            case CTRL_FIRE: return fireRadius;
            case CTRL_USE: return useRadius;
            case CTRL_ENTER: return enterRadius;
            case CTRL_RUN: return runRadius;
            case CTRL_ESC: return escRadius;
            case CTRL_TAB: return tabRadius;
            case CTRL_PREV: return prevWpnRadius;
            case CTRL_NEXT: return nextWpnRadius;
            case CTRL_YES: return yesRadius;
            case CTRL_NO: return noRadius;
            case CTRL_KEYBOARD: return keyboardRadius;
            default: return 40f;
        }
    }

    private void setControlPos(int id, float x, float y) {
        switch (id) {
            case CTRL_JOY: joyCenterX = x; joyCenterY = y; break;
            case CTRL_FIRE: fireBtnX = x; fireBtnY = y; break;
            case CTRL_USE: useBtnX = x; useBtnY = y; break;
            case CTRL_ENTER: enterBtnX = x; enterBtnY = y; break;
            case CTRL_RUN: runBtnX = x; runBtnY = y; break;
            case CTRL_ESC: escBtnX = x; escBtnY = y; break;
            case CTRL_TAB: tabBtnX = x; tabBtnY = y; break;
            case CTRL_PREV: prevWpnBtnX = x; prevWpnBtnY = y; break;
            case CTRL_NEXT: nextWpnBtnX = x; nextWpnBtnY = y; break;
            case CTRL_YES: yesBtnX = x; yesBtnY = y; break;
            case CTRL_NO: noBtnX = x; noBtnY = y; break;
            case CTRL_KEYBOARD: keyboardBtnX = x; keyboardBtnY = y; break;
        }
    }

    private void setControlRadius(int id, float r) {
        r = Math.max(18f, Math.min(r, 220f));
        switch (id) {
            case CTRL_JOY: joyRadius = r; break;
            case CTRL_FIRE: fireRadius = r; break;
            case CTRL_USE: useRadius = r; break;
            case CTRL_ENTER: enterRadius = r; break;
            case CTRL_RUN: runRadius = r; break;
            case CTRL_ESC: escRadius = r; break;
            case CTRL_TAB: tabRadius = r; break;
            case CTRL_PREV: prevWpnRadius = r; break;
            case CTRL_NEXT: nextWpnRadius = r; break;
            case CTRL_YES: yesRadius = r; break;
            case CTRL_NO: noRadius = r; break;
            case CTRL_KEYBOARD: keyboardRadius = r; break;
        }
    }

    private int hitControl(float x, float y) {
        if (!deleted[CTRL_JOY] && dist(x, y, joyCenterX, joyCenterY) < joyRadius * 1.3f) return CTRL_JOY;
        if (!deleted[CTRL_FIRE] && dist(x, y, fireBtnX, fireBtnY) < fireRadius) return CTRL_FIRE;
        if (!deleted[CTRL_USE] && dist(x, y, useBtnX, useBtnY) < useRadius) return CTRL_USE;
        if (!deleted[CTRL_ENTER] && dist(x, y, enterBtnX, enterBtnY) < enterRadius) return CTRL_ENTER;
        if (!deleted[CTRL_RUN] && dist(x, y, runBtnX, runBtnY) < runRadius) return CTRL_RUN;
        if (!deleted[CTRL_ESC] && dist(x, y, escBtnX, escBtnY) < escRadius) return CTRL_ESC;
        if (!deleted[CTRL_TAB] && dist(x, y, tabBtnX, tabBtnY) < tabRadius) return CTRL_TAB;
        if (!deleted[CTRL_PREV] && dist(x, y, prevWpnBtnX, prevWpnBtnY) < prevWpnRadius) return CTRL_PREV;
        if (!deleted[CTRL_NEXT] && dist(x, y, nextWpnBtnX, nextWpnBtnY) < nextWpnRadius) return CTRL_NEXT;
        if (!deleted[CTRL_YES] && dist(x, y, yesBtnX, yesBtnY) < yesRadius) return CTRL_YES;
        if (!deleted[CTRL_NO] && dist(x, y, noBtnX, noBtnY) < noRadius) return CTRL_NO;
        if (!deleted[CTRL_KEYBOARD] && dist(x, y, keyboardBtnX, keyboardBtnY) < keyboardRadius) return CTRL_KEYBOARD;
        return -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);
        float x = event.getX(index);
        float y = event.getY(index);

        if (editMode) {
            return handleEditTouch(event, action, index, pointerId, x, y);
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int hit = hitControl(x, y);
                if (hit < 0) {
                    long now = System.currentTimeMillis();
                    if (now - lastTapTime < TRIPLE_TAP_TIMEOUT) {
                        tapCount++;
                    } else {
                        tapCount = 1;
                    }
                    lastTapTime = now;

                    if (tapCount >= 3) {
                        tapCount = 0;
                        editMode = true;
                        selectedControl = -1;
                        invalidate();
                        return true;
                    }
                } else {
                    tapCount = 0;
                }

                if (!deleted[CTRL_JOY] && dist(x, y, joyCenterX, joyCenterY) < joyRadius * 1.5f && joystickPointerId == -1) {
                    joystickPointerId = pointerId;
                    updateJoystick(x, y);
                } else if (!deleted[CTRL_FIRE] && dist(x, y, fireBtnX, fireBtnY) < fireRadius) {
                    press(pointerId, 1, DoomKeys.FIRE);
                } else if (!deleted[CTRL_USE] && dist(x, y, useBtnX, useBtnY) < useRadius) {
                    press(pointerId, 2, DoomKeys.USE);
                } else if (!deleted[CTRL_ENTER] && dist(x, y, enterBtnX, enterBtnY) < enterRadius) {
                    press(pointerId, 3, DoomKeys.ENTER);
                } else if (!deleted[CTRL_ESC] && dist(x, y, escBtnX, escBtnY) < escRadius) {
                    press(pointerId, 4, DoomKeys.ESCAPE);
                } else if (!deleted[CTRL_TAB] && dist(x, y, tabBtnX, tabBtnY) < tabRadius) {
                    press(pointerId, 5, DoomKeys.TAB);
                } else if (!deleted[CTRL_RUN] && dist(x, y, runBtnX, runBtnY) < runRadius) {
                    toggleRun();
                } else if (!deleted[CTRL_PREV] && dist(x, y, prevWpnBtnX, prevWpnBtnY) < prevWpnRadius) {
                    pressPrevWeapon(pointerId);
                } else if (!deleted[CTRL_NEXT] && dist(x, y, nextWpnBtnX, nextWpnBtnY) < nextWpnRadius) {
                    pressNextWeapon(pointerId);
                } else if (!deleted[CTRL_YES] && dist(x, y, yesBtnX, yesBtnY) < yesRadius) {
                    press(pointerId, 10, DoomKeys.Y);
                } else if (!deleted[CTRL_NO] && dist(x, y, noBtnX, noBtnY) < noRadius) {
                    press(pointerId, 11, DoomKeys.N);
                } else if (!deleted[CTRL_KEYBOARD] && dist(x, y, keyboardBtnX, keyboardBtnY) < keyboardRadius) {
                    if (keyboardToggleListener != null) keyboardToggleListener.onToggle();
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
                if (pointerId == joystickPointerId) {
                    joystickPointerId = -1;
                    releaseAllDirections();
                    stickOffsetX = 0f;
                    stickOffsetY = 0f;
                }
                int zone = pointerToZone.get(pointerId, 0);
                switch (zone) {
                    case 1: DoomLib.nativeKeyEvent(false, DoomKeys.FIRE); break;
                    case 2: DoomLib.nativeKeyEvent(false, DoomKeys.USE); break;
                    case 3: DoomLib.nativeKeyEvent(false, DoomKeys.ENTER); break;
                    case 4: DoomLib.nativeKeyEvent(false, DoomKeys.ESCAPE); break;
                    case 5: DoomLib.nativeKeyEvent(false, DoomKeys.TAB); break;
                    case 7: releasePrevWeapon(); break;
                    case 8: releaseNextWeapon(); break;
                    case 10: DoomLib.nativeKeyEvent(false, DoomKeys.Y); break;
                    case 11: DoomLib.nativeKeyEvent(false, DoomKeys.N); break;
                }
                pointerToZone.delete(pointerId);
                break;
            }
        }
        invalidate();
        return true;
    }

    private boolean handleEditTouch(MotionEvent event, int action, int index, int pointerId, float x, float y) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                if (event.getPointerCount() == 1) {
                    if (hitEditButton(x, y, resetBtnX, resetBtnY)) {
                        resetToDefault();
                        return true;
                    }
                    if (hitEditButton(x, y, doneBtnX, doneBtnY)) {
                        editMode = false;
                        selectedControl = -1;
                        saveLayout();
                        invalidate();
                        return true;
                    }
                    if (selectedControl >= 0 && !deleted[selectedControl] && hitEditButton(x, y, deleteBtnX, deleteBtnY)) {
                        deleted[selectedControl] = true;
                        selectedControl = -1;
                        invalidate();
                        return true;
                    }

                    int hit = hitControl(x, y);
                    if (hit >= 0) {
                        selectedControl = hit;
                        dragPointerId = pointerId;
                        float[] pos = getControlPos(hit);
                        dragOffsetX = x - pos[0];
                        dragOffsetY = y - pos[1];
                    } else {
                        selectedControl = -1;
                        dragPointerId = -1;
                    }
                } else if (event.getPointerCount() == 2 && selectedControl >= 0 && !deleted[selectedControl]) {
                    isPinching = true;
                    pinchStartDist = spacing(event);
                    pinchStartRadius = getControlRadius(selectedControl);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (isPinching && event.getPointerCount() >= 2 && selectedControl >= 0) {
                    float newDist = spacing(event);
                    if (pinchStartDist > 10f) {
                        float scale = newDist / pinchStartDist;
                        setControlRadius(selectedControl, pinchStartRadius * scale);
                    }
                } else if (dragPointerId == pointerId && selectedControl >= 0 && !isPinching) {
                    setControlPos(selectedControl, x - dragOffsetX, y - dragOffsetY);
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (event.getPointerCount() <= 1) {
                    isPinching = false;
                    dragPointerId = -1;
                }
                break;
            }
        }
        invalidate();
        return true;
    }

    private boolean hitEditButton(float x, float y, float bx, float by) {
        return Math.abs(x - bx) < editBtnW / 2 && Math.abs(y - by) < editBtnH / 2;
    }

    private float spacing(MotionEvent event) {
        if (event.getPointerCount() < 2) return 0;
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void toggleRun() {
        runActive = !runActive;
        DoomLib.nativeKeyEvent(runActive, DoomKeys.RSHIFT);
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