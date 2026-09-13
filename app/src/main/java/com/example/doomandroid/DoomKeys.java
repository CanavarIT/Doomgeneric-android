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

    // Подтверждения в диалогах (пока не используются, пригодятся позже)
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

    private DoomKeys() {}
}