package com.example.doomandroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private TouchControlsView touchControlsView;
    private KeyboardView keyboardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUi();
        setContentView(R.layout.activity_main);

        touchControlsView = findViewById(R.id.touchControlsView);
        keyboardView = findViewById(R.id.keyboardView);

        // Клавиатура по умолчанию скрыта.
        keyboardView.setVisibility(View.GONE);

        // Задаём высоту клавиатуры: 45% от высоты экрана.
        // Так она занимает нижнюю часть, перекрывая игру, но оставляя
        // верхнюю часть с сообщением Doom видимой.
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int keyboardHeight = (int)(dm.heightPixels * 0.45f);
        ViewGroup.LayoutParams lp = keyboardView.getLayoutParams();
        lp.height = keyboardHeight;
        keyboardView.setLayoutParams(lp);

        // Связка: тап по кнопке ⌨ в TouchControlsView переключает клавиатуру.
        touchControlsView.setOnKeyboardToggleListener(() -> {
            if (keyboardView.getVisibility() == View.GONE) {
                keyboardView.setVisibility(View.VISIBLE);
            } else {
                keyboardView.setVisibility(View.GONE);
            }
        });

        String wadPath = ensureWadCopied("doom1.wad");
        DoomLib.nativeInit(wadPath, getFilesDir().getAbsolutePath());
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

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    /**
     * WAD лежит в assets/ (зашит в APK), но нативный код не умеет читать
     * assets напрямую (это не POSIX-путь) — копируем один раз в filesDir
     * и возвращаем обычный абсолютный путь.
     */
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