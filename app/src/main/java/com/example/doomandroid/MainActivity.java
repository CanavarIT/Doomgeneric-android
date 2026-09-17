package com.example.doomandroid;

import android.app.Activity;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private TouchControlsView touchControlsView;
    private KeyboardView keyboardView;

    private boolean gamepadConnected = false;
    private int lastDirMask = 0;
    private static final float STICK_DEADZONE = 0.35f;

    private boolean firePressed = false;
    private boolean usePressed = false;
    private boolean runPressed = false;
    private boolean enterPressed = false;
    private boolean escPressed = false;
    private boolean tabPressed = false;

    private InputManager inputManager;
    private final InputManager.InputDeviceListener deviceListener = new InputManager.InputDeviceListener() {
        @Override
        public void onInputDeviceAdded(int deviceId) {
            checkGamepadPresence();
        }

        @Override
        public void onInputDeviceRemoved(int deviceId) {
            checkGamepadPresence();
        }

        @Override
        public void onInputDeviceChanged(int deviceId) {
            checkGamepadPresence();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUi();
        setContentView(R.layout.activity_main);

        touchControlsView = findViewById(R.id.touchControlsView);
        keyboardView = findViewById(R.id.keyboardView);

        keyboardView.setVisibility(View.GONE);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int keyboardHeight = (int) (dm.heightPixels * 0.45f);
        ViewGroup.LayoutParams lp = keyboardView.getLayoutParams();
        lp.height = keyboardHeight;
        keyboardView.setLayoutParams(lp);

        touchControlsView.setOnKeyboardToggleListener(() -> {
            if (keyboardView.getVisibility() == View.GONE) {
                keyboardView.setVisibility(View.VISIBLE);
            } else {
                keyboardView.setVisibility(View.GONE);
            }
        });

        inputManager = (InputManager) getSystemService(INPUT_SERVICE);
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(deviceListener, null);
        }
        checkGamepadPresence();

        String wadPath = ensureWadCopied("doom1.wad");
        DoomLib.nativeInit(wadPath, getFilesDir().getAbsolutePath());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(deviceListener);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (touchControlsView != null) touchControlsView.saveLayout();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isGamepad(event)) {
            setGamepadConnected(true);
            return handleGamepadKey(keyCode, true);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isGamepad(event)) {
            return handleGamepadKey(keyCode, false);
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                && event.getAction() == MotionEvent.ACTION_MOVE) {

            setGamepadConnected(true);

            float x = event.getAxisValue(MotionEvent.AXIS_X);
            float y = event.getAxisValue(MotionEvent.AXIS_Y);

            float hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X);
            float hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

            if (Math.abs(hatX) > 0.1f || Math.abs(hatY) > 0.1f) {
                x = hatX;
                y = hatY;
            }

            int mask = 0;
            if (y < -STICK_DEADZONE) mask |= 1;
            if (y > STICK_DEADZONE) mask |= 2;
            if (x < -STICK_DEADZONE) mask |= 4;
            if (x > STICK_DEADZONE) mask |= 8;

            applyDirMask(mask);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    private boolean isGamepad(KeyEvent event) {
        int sources = event.getSource();
        return (sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK;
    }

    private boolean handleGamepadKey(int keyCode, boolean pressed) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BUTTON_R1:
            case KeyEvent.KEYCODE_BUTTON_R2:
                if (pressed != firePressed) {
                    firePressed = pressed;
                    DoomLib.nativeKeyEvent(pressed, DoomKeys.FIRE);
                }
                return true;

            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_L1:
                if (pressed != usePressed) {
                    usePressed = pressed;
                    DoomLib.nativeKeyEvent(pressed, DoomKeys.USE);
                }
                return true;

            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_BUTTON_MODE:
                if (pressed != escPressed) {
                    escPressed = pressed;
                    DoomLib.nativeKeyEvent(pressed, DoomKeys.ESCAPE);
                }
                return true;

            case KeyEvent.KEYCODE_BUTTON_B:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                if (pressed != enterPressed) {
                    enterPressed = pressed;
                    DoomLib.nativeKeyEvent(pressed, DoomKeys.ENTER);
                }
                return true;

            case KeyEvent.KEYCODE_BUTTON_Y:
                if (pressed != tabPressed) {
                    tabPressed = pressed;
                    DoomLib.nativeKeyEvent(pressed, DoomKeys.TAB);
                }
                return true;

            case KeyEvent.KEYCODE_BUTTON_L2:
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
                if (pressed != runPressed) {
                    runPressed = pressed;
                    DoomLib.nativeKeyEvent(pressed, DoomKeys.RSHIFT);
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                DoomLib.nativeKeyEvent(pressed, DoomKeys.UPARROW);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                DoomLib.nativeKeyEvent(pressed, DoomKeys.DOWNARROW);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                DoomLib.nativeKeyEvent(pressed, DoomKeys.LEFTARROW);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                DoomLib.nativeKeyEvent(pressed, DoomKeys.RIGHTARROW);
                return true;

            default:
                return false;
        }
    }

    private void applyDirMask(int newMask) {
        int changed = newMask ^ lastDirMask;
        if ((changed & 1) != 0) DoomLib.nativeKeyEvent((newMask & 1) != 0, DoomKeys.UPARROW);
        if ((changed & 2) != 0) DoomLib.nativeKeyEvent((newMask & 2) != 0, DoomKeys.DOWNARROW);
        if ((changed & 4) != 0) DoomLib.nativeKeyEvent((newMask & 4) != 0, DoomKeys.LEFTARROW);
        if ((changed & 8) != 0) DoomLib.nativeKeyEvent((newMask & 8) != 0, DoomKeys.RIGHTARROW);
        lastDirMask = newMask;
    }

    private void setGamepadConnected(boolean connected) {
        if (gamepadConnected == connected) return;
        gamepadConnected = connected;

        runOnUiThread(() -> {
            if (touchControlsView != null) {
                touchControlsView.setVisibility(connected ? View.GONE : View.VISIBLE);
            }
            if (connected && keyboardView != null) {
                keyboardView.setVisibility(View.GONE);
            }
        });
    }

    private void checkGamepadPresence() {
        boolean found = false;
        int[] ids = InputDevice.getDeviceIds();
        for (int id : ids) {
            InputDevice dev = InputDevice.getDevice(id);
            if (dev == null) continue;
            int sources = dev.getSources();
            if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                    || (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                found = true;
                break;
            }
        }
        setGamepadConnected(found);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private String ensureWadCopied(String assetName) {
        File outFile = new File(getFilesDir(), assetName);
        if (!outFile.exists()) {
            try (InputStream is = getAssets().open(assetName);
                 OutputStream os = new FileOutputStream(outFile)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) {
                    os.write(buf, 0, n);
                }
            } catch (Exception e) {
                throw new RuntimeException("Не найден " + assetName +
                        " в assets/ — положите doom1.wad в app/src/main/assets/", e);
            }
        }
        return outFile.getAbsolutePath();
    }
}