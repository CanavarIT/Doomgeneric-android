package com.example.doomandroid;

/** Коды клавиш из doomkeys.h — держите синхронно с исходниками doomgeneric. */
public final class DoomKeys {

    // Стрелки
    public static final int RIGHTARROW = 0xae;
    public static final int LEFTARROW  = 0xac;
    public static final int UPARROW    = 0xad;
    public static final int DOWNARROW  = 0xaf;

    // Боевые / взаимодействие
    public static final int STRAFE_L   = 0xa0;
    public static final int STRAFE_R   = 0xa1;
    public static final int USE        = 0xa2;
    public static final int FIRE       = 0xa3;

    // Специальные
    public static final int ESCAPE     = 27;
    public static final int ENTER      = 13;
    public static final int TAB        = 9;
    public static final int SPACE      = 32;
    public static final int BACKSPACE  = 0x7f;

    // Подтверждения в диалогах
    public static final int Y          = 'y';    // 121
    public static final int N          = 'n';    // 110

    // Бег (правый Shift)
    public static final int RSHIFT     = 0x80 + 0x36;

    // Переключение оружия — клавиши '1'..'7'
    public static final int WPN_1      = '1';
    public static final int WPN_2      = '2';
    public static final int WPN_3      = '3';
    public static final int WPN_4      = '4';
    public static final int WPN_5      = '5';
    public static final int WPN_6      = '6';
    public static final int WPN_7      = '7';

    // Буквы a-z (ASCII). Doom сам преобразует их в верхний регистр
    // при вводе имени сейва.
    public static final int A = 'a';
    public static final int B = 'b';
    public static final int C = 'c';
    public static final int D = 'd';
    public static final int E = 'e';
    public static final int F = 'f';
    public static final int G = 'g';
    public static final int H = 'h';
    public static final int I = 'i';
    public static final int J = 'j';
    public static final int K = 'k';
    public static final int L = 'l';
    public static final int M = 'm';
    public static final int N_LETTER = 'n';  // чтобы не путать с "No"
    public static final int O = 'o';
    public static final int P = 'p';
    public static final int Q = 'q';
    public static final int R = 'r';
    public static final int S = 's';
    public static final int T = 't';
    public static final int U = 'u';
    public static final int V = 'v';
    public static final int W = 'w';
    public static final int X = 'x';
    public static final int Y_LETTER = 'y';
    public static final int Z = 'z';

    // Цифры 0-9 (ASCII)
    public static final int NUM_0 = '0';
    public static final int NUM_1 = '1';
    public static final int NUM_2 = '2';
    public static final int NUM_3 = '3';
    public static final int NUM_4 = '4';
    public static final int NUM_5 = '5';
    public static final int NUM_6 = '6';
    public static final int NUM_7 = '7';
    public static final int NUM_8 = '8';
    public static final int NUM_9 = '9';

    private DoomKeys() {}
}