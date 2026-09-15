# Doom for Android — Native Port

<p align="center">
  <img src="Screenshot_2026-09-14-16-44-29-156_com.example.doomandroid.jpg" width="100%" />
</p>

A native Android port of the classic DOOM (shareware) built on top of
[doomgeneric](https://github.com/ozkl/doomgeneric). The engine is compiled
directly to ARM64 via the Android NDK and linked with the Java layer through
JNI. This is not an emulator — the game code runs natively on the device CPU.

## Download

Grab the latest APK from the [Releases page](../../releases/latest).

The shareware WAD is bundled inside the APK, so no extra files are needed —
just download, install, and play.

## About the project

This project started as an experiment: how hard is it to take a piece of
open-source C code written for desktop platforms and make it run as a native
Android application, with a proper touch interface and no emulation layer.

The result is a working build of DOOM that runs on modern ARM64 Android
devices. It uses a virtual joystick for movement, on-screen buttons for
FIRE / USE / ENTER, and a software framebuffer that is blitted to a Bitmap
and rendered through SurfaceView.

## How it works

The architecture consists of three layers.

### Native engine (C)

The DOOM engine itself comes from doomgeneric, which is a portable
reimplementation of the original DOOM source code released by id Software.
All engine files are located in `app/src/main/cpp/doomgeneric/`. They are
compiled into a shared library called `libdoom.so`.

Two additional C files glue the engine to Android:

- `jni_bridge.c` — exposes JNI functions that the Java layer calls.
  It handles initialization, key events, and frame blitting.
- `doomgeneric_android.c` — implements the platform layer required by
  doomgeneric: `DG_Init`, `DG_DrawFrame`, `DG_SleepMs`, `DG_GetTicksMs`,
  `DG_GetKey`, `DG_SetWindowTitle`, plus the framebuffer blitting routine
  that copies `DG_ScreenBuffer` into an Android Bitmap.

Input events are pushed from Java into a thread-safe ring buffer and
consumed by the engine on its own game thread.

### Java layer

The Android side contains:

- `MainActivity` — sets up the view hierarchy, copies the WAD file from
  assets to internal storage, and starts the native engine.
- `DoomSurfaceView` — a SurfaceView that owns the 640x400 Bitmap used
  as the framebuffer target. Frames are requested through Choreographer
  and blitted to the screen.
- `TouchControlsView` — draws the virtual joystick and buttons on top of
  the game surface, and translates touch events into DOOM key codes.
- `KeyboardView` — on-screen keyboard for entering save names (A–Z, 0–9,
  Space, Backspace, Enter). Appears by tapping the ⌨ button.
- `DoomLib` — static native method declarations that match the JNI
  functions in `jni_bridge.c`.
- `DoomKeys` — key code constants mirrored from `doomkeys.h`.

### Audio

Sound effects and music are routed through the same OpenSL ES mixer.

- Sound effects come from the original DOOM `.ds` lumps inside the WAD
  and are converted on the fly into 16-bit stereo PCM.
- Music is emulated through **Nuked OPL3**, a bit-exact emulator of the
  Yamaha YMF262 (OPL3) chip used by DOOM in 1993. The generated PCM is
  mixed into the same buffer as the sound effects. No external
  dependencies, no sound banks, no SDL.

### Communication

Java and C communicate exclusively through JNI. There is no shared memory,
no Binder, no AIDL. The Java layer calls native methods directly, and the
native engine calls back into Java only where necessary.

## Building

### Requirements

- Android Studio or AndroidIDE
- Android NDK (r27 or newer recommended)
- A device or emulator running Android 10 (API 29) or later
- The shareware WAD file `doom1.wad`

### Steps

1. Clone the repository.

2. Place `doom1.wad` into `app/src/main/assets/`.

   The WAD is not included in this repository for licensing reasons.
   The shareware version can be downloaded freely from various public
   archives.

3. Build the APK.

4. Install on a connected device.

### Native library

The native library `libdoom.so` is built separately with `clang` and a
plain Makefile, then placed into `app/src/main/jniLibs/arm64-v8a/` and
picked up by Gradle as a prebuilt shared library.

The `doomgeneric` sources are filtered so that platform backends for
other systems (SDL, Allegro, X11, Windows, Emscripten, and so on) are
excluded from the build. Only the Android backend is compiled.

The Makefile also links against `libOpenSLES`, which is used for both
sound effects and music output.

## Controls

The touch interface provides:

- **Virtual joystick** (bottom-left) — move forward/backward, strafe left/right.
- **FIRE** — shoot.
- **USE** — open doors, press switches.
- **ENTER** — confirm in menus.
- **ESC** — open the in-game menu, go back.
- **TAB** — toggle the automap.
- **RUN** — toggle running. Red = off, blue = on.
- **`<` / `>`** — previous / next weapon.
- **Y / N** — answer confirmation dialogs (Quit, Save overwrite, Load, End Game, Nightmare).
- **⌨ (keyboard)** — opens the on-screen keyboard for entering save
  names. Tap again to hide.

The controls are implemented as custom Views drawn over the game surface,
forwarding touch events to the native engine.

## Why the engine is not built through CMake

During development, building doomgeneric through CMake inside Gradle's
native build pipeline led to TLS alignment issues on ARM64. The library
compiled and linked successfully, but Android's dynamic linker refused to
load it because the TLS segment was not aligned to a power of two.

To work around this, the library is compiled separately using a plain
Makefile, then the resulting `libdoom.so` is placed into
`app/src/main/jniLibs/arm64-v8a/` and picked up by Gradle as a prebuilt
shared library. The `CMakeLists.txt` is retained but the native build
step is disabled in `build.gradle`.

This is not the most elegant setup, but it works reliably and is easy
to reproduce.

## Status

The port is fully playable from start to finish on the shareware episode.

**What works:**

- Movement, shooting, doors, switches
- Weapon switching (`<` / `>`)
- In-game menu (ESC) and automap (TAB)
- Confirmation dialogs (Y / N)
- RUN toggle (red = off, blue = on)
- Sound effects — gunshots, footsteps, doors, monsters
- OPL music emulation (Nuked OPL3) — background music
- Save and load game with on-screen keyboard
- Custom app icon

**What's missing (planned):**

- External gamepad support — currently touch-only
- Configurable button layout — buttons are fixed to their positions

**Requirements:**

- Android 10 (API 29) or later
- ARM64 device (all modern phones)

## License

The source code in this repository is distributed under the GNU General
Public License version 2, the same license used by doomgeneric and the
original DOOM source release from id Software.

The shareware WAD file `doom1.wad` is not covered by the GPL. It is
property of id Software and is redistributed under the terms of the
id Software Limited Use Software License Agreement, which permits free
redistribution in its original, unmodified form, provided that no fee
is charged for its receipt or use.

DOOM is a trademark of id Software LLC. This is an unofficial,
non-commercial fan project and is not affiliated with or endorsed by
id Software, ZeniMax, or Bethesda Softworks.

## Acknowledgements

- id Software for creating DOOM and releasing the source code.
- The doomgeneric project for providing a portable base.
- The Android NDK team for making native development on Android practical.
- The Nuked OPL3 project for the bit-exact OPL emulator.

## Author

CanavarIT — [github.com/CanavarIT](https://github.com/CanavarIT)
