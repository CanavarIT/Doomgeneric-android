cmake_minimum_required(VERSION 3.22.1)

project(doom C)

# === Сборка нативной библиотеки отключена ===
# Готовый libdoom.so лежит в src/main/jniLibs/arm64-v8a/
# и упаковывается в APK через Gradle (jniLibs), а не через CMake.
#
# Если захочешь вернуть сборку через CMake — раскомментируй блок ниже
# и убери комментарии с externalNativeBuild в build.gradle.
#
# set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)
# set(CMAKE_C_STANDARD 99)
#
# file(GLOB DOOMGENERIC_SOURCES "${CMAKE_SOURCE_DIR}/doomgeneric/*.c")
#
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/doomgeneric_allegro\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/doomgeneric_emscripten\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/doomgeneric_sdl\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/doomgeneric_sosox\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/doomgeneric_soso\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/doomgeneric_linuxvt\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/doomgeneric_xlib\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/doomgeneric_win\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/i_sdlmusic\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/i_sdlsound\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/i_allegromusic\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/i_allegrosound\\.c$")
# list(FILTER DOOMGENERIC_SOURCES EXCLUDE REGEX ".*/i_endoom\\.c$")
#
# add_library(doom SHARED
#     jni_bridge.c
#     doomgeneric_android.c
#     ${DOOMGENERIC_SOURCES}
# )
#
# target_include_directories(doom PRIVATE
#     ${CMAKE_SOURCE_DIR}/doomgeneric
# )
#
# target_compile_options(doom PRIVATE
#     -Wno-implicit-function-declaration
#     -Wno-unused-but-set-variable
#     -Wno-unused-variable
#     -Wno-unused-function
# )
#
# find_library(log-lib log)
# find_library(android-lib android)
# find_library(jnigraphics-lib jnigraphics)
#
# target_link_libraries(doom
#     ${log-lib}
#     ${android-lib}
#     ${jnigraphics-lib}
# )
#
# # === Пост-обработка TLS ===
# add_custom_command(TARGET doom POST_BUILD
#     COMMAND ${CMAKE_COMMAND} -E echo "Patching TLS alignment..."
#     COMMAND python3 ${CMAKE_SOURCE_DIR}/align_tls.py $<TARGET_FILE:doom>
#     COMMENT "Fixing TLS segment alignment for ARM64 Bionic"
# )