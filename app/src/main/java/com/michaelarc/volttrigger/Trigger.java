package com.michaelarc.volttrigger;

import android.view.KeyEvent;

public enum Trigger {
    LEFT(
            "L",
            "Gatillo izquierdo",
            "Volumen +",
            KeyEvent.KEYCODE_VOLUME_UP,
            0.10f,
            0.30f,
            0xFF35D2FF),
    RIGHT(
            "R",
            "Gatillo derecho",
            "Volumen −",
            KeyEvent.KEYCODE_VOLUME_DOWN,
            0.90f,
            0.30f,
            0xFFFF7A45);

    public final String badge;
    public final String title;
    public final String keyLabel;
    public final int keyCode;
    public final float defaultX;
    public final float defaultY;
    public final int color;

    Trigger(String badge, String title, String keyLabel, int keyCode,
            float defaultX, float defaultY, int color) {
        this.badge = badge;
        this.title = title;
        this.keyLabel = keyLabel;
        this.keyCode = keyCode;
        this.defaultX = defaultX;
        this.defaultY = defaultY;
        this.color = color;
    }

    public Trigger opposite() {
        return this == LEFT ? RIGHT : LEFT;
    }

    public static Trigger fromKeyCode(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return LEFT;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return RIGHT;
        }
        return null;
    }
}
