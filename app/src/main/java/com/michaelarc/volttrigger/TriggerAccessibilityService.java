package com.michaelarc.volttrigger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public final class TriggerAccessibilityService extends AccessibilityService {

    private static volatile TriggerAccessibilityService instance;
    private static volatile boolean hostVisible;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private GestureInjector injector;
    private OverlayController overlay;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    private Vibrator vibrator;

    public static boolean isRunning() {
        return instance != null;
    }

    public static boolean requestEditor() {
        TriggerAccessibilityService service = instance;
        if (service == null) {
            return false;
        }
        service.mainHandler.post(() -> {
            if (service.overlay != null) {
                service.overlay.showEditor();
            }
        });
        return true;
    }

    public static void setHostVisible(boolean visible) {
        hostVisible = visible;
        TriggerAccessibilityService service = instance;
        if (service == null) {
            return;
        }
        service.mainHandler.post(() -> {
            if (service.overlay != null) {
                service.overlay.setHostVisible(visible);
            }
        });
    }

    public static boolean testTrigger(Trigger trigger) {
        TriggerAccessibilityService service = instance;
        if (service == null || trigger == null) {
            return false;
        }
        service.mainHandler.post(() -> service.performTest(trigger));
        return true;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        injector = new GestureInjector(this);
        overlay = new OverlayController(this);
        overlay.setHostVisible(hostVisible);
        vibrator = obtainVibrator();
        preferences = AppPrefs.get(this);

        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }

        preferenceListener = (sharedPreferences, key) -> {
            if (key == null) {
                return;
            }
            if (AppPrefs.KEY_MAPPING_ENABLED.equals(key) && !AppPrefs.isMappingEnabled(this)) {
                releaseAllTriggers();
            }
            if (AppPrefs.KEY_MAPPING_ENABLED.equals(key)
                    || AppPrefs.KEY_BUBBLE_ENABLED.equals(key)) {
                if (overlay != null) {
                    overlay.refreshBubble();
                }
            }
        };
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
        if (overlay != null) {
            overlay.refreshBubble();
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Key filtering and gesture dispatch do not need window content.
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        if (event == null || injector == null) {
            return false;
        }
        Trigger trigger = Trigger.fromKeyCode(event.getKeyCode());
        if (trigger == null) {
            return false;
        }
        if (overlay != null && overlay.isEditorVisible()) {
            return false;
        }
        if (!AppPrefs.isMappingEnabled(this)) {
            return false;
        }

        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() == 0) {
                if (AppPrefs.getMode(this, trigger) == AppPrefs.Mode.HOLD) {
                    injector.holdDown(trigger);
                } else {
                    injector.tap(trigger);
                }
                if (overlay != null) {
                    overlay.flashAt(trigger);
                }
                vibrate();
            }
            return true;
        }
        if (action == KeyEvent.ACTION_UP) {
            injector.holdUp(trigger);
            return true;
        }
        return true;
    }

    @Override
    public void onInterrupt() {
        releaseAllTriggers();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        releaseAllTriggers();
        if (overlay != null) {
            overlay.onConfigurationChanged();
        }
    }

    @Override
    public void onDestroy() {
        if (preferences != null && preferenceListener != null) {
            preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        }
        if (overlay != null) {
            overlay.destroy();
        }
        if (injector != null) {
            injector.destroy();
        }
        preferenceListener = null;
        overlay = null;
        injector = null;
        preferences = null;
        instance = null;
        super.onDestroy();
    }

    void releaseAllTriggers() {
        if (injector != null) {
            injector.releaseAll();
        }
    }

    PointF injectedPoint(Trigger trigger) {
        if (injector == null) {
            return new PointF();
        }
        return injector.pointFor(trigger);
    }

    private void performTest(Trigger trigger) {
        if (injector == null) {
            return;
        }
        injector.tap(trigger);
        if (overlay != null) {
            overlay.flashAt(trigger);
        }
        vibrate();
    }

    private void vibrate() {
        if (vibrator == null || !AppPrefs.isHapticsEnabled(this)
                || !vibrator.hasVibrator()) {
            return;
        }
        vibrator.vibrate(VibrationEffect.createOneShot(
                16L, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private Vibrator obtainVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager =
                    (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            return manager == null ? null : manager.getDefaultVibrator();
        }
        return (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }
}
