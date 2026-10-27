package com.michaelarc.volttrigger;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public final class AppPrefs {

    public static final String FILE_NAME = "volt_trigger";

    public static final String KEY_MAPPING_ENABLED = "mapping_enabled";
    public static final String KEY_BUBBLE_ENABLED = "bubble_enabled";
    public static final String KEY_HAPTICS_ENABLED = "haptics_enabled";
    public static final String KEY_FLASH_ENABLED = "flash_enabled";
    public static final String KEY_TAP_DURATION = "tap_duration_ms";
    public static final String KEY_BUBBLE_X = "bubble_x";
    public static final String KEY_BUBBLE_Y = "bubble_y";

    public static final int DEFAULT_TAP_DURATION_MS = 60;

    public enum Mode {
        TAP,
        HOLD
    }

    private AppPrefs() {
    }

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isMappingEnabled(Context context) {
        return get(context).getBoolean(KEY_MAPPING_ENABLED, false);
    }

    public static void setMappingEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_MAPPING_ENABLED, enabled).apply();
    }

    public static boolean isBubbleEnabled(Context context) {
        return get(context).getBoolean(KEY_BUBBLE_ENABLED, true);
    }

    public static void setBubbleEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_BUBBLE_ENABLED, enabled).apply();
    }

    public static boolean isHapticsEnabled(Context context) {
        return get(context).getBoolean(KEY_HAPTICS_ENABLED, true);
    }

    public static void setHapticsEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply();
    }

    public static boolean isFlashEnabled(Context context) {
        return get(context).getBoolean(KEY_FLASH_ENABLED, true);
    }

    public static void setFlashEnabled(Context context, boolean enabled) {
        get(context).edit().putBoolean(KEY_FLASH_ENABLED, enabled).apply();
    }

    public static int getTapDurationMs(Context context) {
        return get(context).getInt(KEY_TAP_DURATION, DEFAULT_TAP_DURATION_MS);
    }

    public static void setTapDurationMs(Context context, int durationMs) {
        int safe = Math.max(20, Math.min(250, durationMs));
        get(context).edit().putInt(KEY_TAP_DURATION, safe).apply();
    }

    public static Mode getMode(Context context, Trigger trigger) {
        String value = get(context).getString(modeKey(trigger), Mode.HOLD.name());
        try {
            return Mode.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return Mode.HOLD;
        }
    }

    public static void setMode(Context context, Trigger trigger, Mode mode) {
        get(context).edit().putString(modeKey(trigger), mode.name()).apply();
    }

    public static float getTargetX(Context context, Trigger trigger) {
        return clamp(get(context).getFloat(xKey(trigger), trigger.defaultX));
    }

    public static float getTargetY(Context context, Trigger trigger) {
        return clamp(get(context).getFloat(yKey(trigger), trigger.defaultY));
    }

    public static void setTarget(Context context, Trigger trigger, float x, float y) {
        get(context).edit()
                .putFloat(xKey(trigger), clamp(x))
                .putFloat(yKey(trigger), clamp(y))
                .apply();
    }

    public static float getBubbleX(Context context) {
        return clamp(get(context).getFloat(KEY_BUBBLE_X, 0.94f));
    }

    public static float getBubbleY(Context context) {
        return clamp(get(context).getFloat(KEY_BUBBLE_Y, 0.48f));
    }

    public static void setBubblePosition(Context context, float x, float y) {
        get(context).edit()
                .putFloat(KEY_BUBBLE_X, clamp(x))
                .putFloat(KEY_BUBBLE_Y, clamp(y))
                .apply();
    }

    private static String modeKey(Trigger trigger) {
        return "mode_" + trigger.name().toLowerCase(Locale.ROOT);
    }

    private static String xKey(Trigger trigger) {
        return "target_x_" + trigger.name().toLowerCase(Locale.ROOT);
    }

    private static String yKey(Trigger trigger) {
        return "target_y_" + trigger.name().toLowerCase(Locale.ROOT);
    }

    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 0.5f;
        }
        return Math.max(0.01f, Math.min(0.99f, value));
    }
}
