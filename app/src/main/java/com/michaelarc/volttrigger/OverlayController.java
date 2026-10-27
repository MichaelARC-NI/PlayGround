package com.michaelarc.volttrigger;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

final class OverlayController {

    private static final int BUBBLE_SIZE_DP = 54;
    private static final int FLASH_SIZE_DP = 76;

    private final TriggerAccessibilityService service;
    private final WindowManager windowManager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BubbleView bubbleView;
    private WindowManager.LayoutParams bubbleParams;

    private EditorOverlayView editorView;
    private WindowManager.LayoutParams editorParams;
    private boolean editorVisible;
    private boolean hostVisible;

    OverlayController(TriggerAccessibilityService service) {
        this.service = service;
        this.windowManager =
                (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    }

    boolean isEditorVisible() {
        return editorVisible;
    }

    void refreshBubble() {
        if (editorVisible) {
            return;
        }
        boolean shouldShow = !hostVisible
                && AppPrefs.isMappingEnabled(service)
                && AppPrefs.isBubbleEnabled(service);
        if (shouldShow) {
            showBubble();
        } else {
            hideBubble();
        }
    }

    void showEditor() {
        if (editorVisible) {
            return;
        }
        service.releaseAllTriggers();
        removeBubble();
        float leftX = AppPrefs.getTargetX(service, Trigger.LEFT);
        float leftY = AppPrefs.getTargetY(service, Trigger.LEFT);
        float rightX = AppPrefs.getTargetX(service, Trigger.RIGHT);
        float rightY = AppPrefs.getTargetY(service, Trigger.RIGHT);

        editorView = new EditorOverlayView(service, leftX, leftY, rightX, rightY,
                new EditorOverlayView.Listener() {
                    @Override
                    public void onSave(float lx, float ly, float rx, float ry) {
                        AppPrefs.setTarget(service, Trigger.LEFT, lx, ly);
                        AppPrefs.setTarget(service, Trigger.RIGHT, rx, ry);
                        hideEditor();
                        toast("Gatillos guardados");
                    }

                    @Override
                    public void onCancel() {
                        hideEditor();
                    }
                });

        editorParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.graphics.PixelFormat.TRANSLUCENT);
        editorParams.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            editorParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            editorParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        try {
            windowManager.addView(editorView, editorParams);
            editorVisible = true;
        } catch (RuntimeException exception) {
            editorView = null;
            editorParams = null;
            toast("No se pudo abrir el editor");
        }
    }

    void hideEditor() {
        if (!editorVisible) {
            return;
        }
        editorVisible = false;
        if (editorView != null) {
            try {
                windowManager.removeView(editorView);
            } catch (RuntimeException ignored) {
                // The window may already be detached.
            }
        }
        editorView = null;
        editorParams = null;
        refreshBubble();
    }

    void flashAt(Trigger trigger) {
        if (editorVisible || !AppPrefs.isFlashEnabled(service)) {
            return;
        }
        PointF point = service.injectedPoint(trigger);
        int size = SettingsUtils.dp(service, FLASH_SIZE_DP);
        FlashView view = new FlashView(service, trigger.color);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Math.round(point.x - size / 2f);
        params.y = Math.round(point.y - size / 2f);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        try {
            windowManager.addView(view, params);
        } catch (RuntimeException ignored) {
            return;
        }
        view.setAlpha(1f);
        view.setScaleX(0.68f);
        view.setScaleY(0.68f);
        view.animate()
                .scaleX(1.16f)
                .scaleY(1.16f)
                .alpha(0f)
                .setDuration(340L)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    try {
                        windowManager.removeView(view);
                    } catch (RuntimeException ignored) {
                        // The window may already be gone.
                    }
                })
                .start();
    }

    void setHostVisible(boolean visible) {
        if (hostVisible == visible) {
            return;
        }
        hostVisible = visible;
        if (visible) {
            removeBubble();
        } else {
            refreshBubble();
        }
    }

    void onConfigurationChanged() {
        if (bubbleView != null) {
            removeBubble();
            refreshBubble();
        }
    }

    void destroy() {
        hideEditor();
        removeBubble();
        handler.removeCallbacksAndMessages(null);
    }

    private void showBubble() {
        if (bubbleView != null || editorVisible) {
            return;
        }
        if (windowManager == null) {
            return;
        }
        int size = SettingsUtils.dp(service, BUBBLE_SIZE_DP);
        Rect bounds = GestureInjector.screenBounds(service);
        int maxX = Math.max(0, bounds.width() - size);
        int maxY = Math.max(0, bounds.height() - size);
        int x = Math.round(AppPrefs.getBubbleX(service) * maxX);
        int y = Math.round(AppPrefs.getBubbleY(service) * maxY);
        x = Math.max(0, Math.min(maxX, x));
        y = Math.max(0, Math.min(maxY, y));

        BubbleView view = new BubbleView(service);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x;
        params.y = y;
        params.alpha = 0.94f;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        view.setOnTouchListener(new BubbleTouchListener(size, maxX, maxY));
        try {
            windowManager.addView(view, params);
            bubbleView = view;
            bubbleParams = params;
        } catch (RuntimeException ignored) {
            // Accessibility overlay might not be available yet.
        }
    }

    private void removeBubble() {
        if (bubbleView == null) {
            return;
        }
        try {
            windowManager.removeView(bubbleView);
        } catch (RuntimeException ignored) {
            // The window may already be detached.
        }
        bubbleView = null;
        bubbleParams = null;
    }

    private void hideBubble() {
        removeBubble();
    }

    private void toast(String message) {
        Toast.makeText(service, message, Toast.LENGTH_SHORT).show();
    }

    private final class BubbleTouchListener implements View.OnTouchListener {
        private final int size;
        private final int maxX;
        private final int maxY;
        private final int touchSlop;
        private float downRawX;
        private float downRawY;
        private int startX;
        private int startY;
        private boolean moved;

        BubbleTouchListener(int size, int maxX, int maxY) {
            this.size = size;
            this.maxX = maxX;
            this.maxY = maxY;
            this.touchSlop = ViewConfiguration.get(service).getScaledTouchSlop();
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    startX = bubbleParams.x;
                    startY = bubbleParams.y;
                    moved = false;
                    view.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80L).start();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downRawX;
                    float dy = event.getRawY() - downRawY;
                    if (!moved && Math.hypot(dx, dy) > touchSlop) {
                        moved = true;
                    }
                    if (moved && bubbleParams != null) {
                        bubbleParams.x = Math.max(0, Math.min(maxX, startX + Math.round(dx)));
                        bubbleParams.y = Math.max(0, Math.min(maxY, startY + Math.round(dy)));
                        try {
                            windowManager.updateViewLayout(view, bubbleParams);
                        } catch (RuntimeException ignored) {
                            // The window may already be detached.
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(110L).start();
                    if (moved) {
                        saveBubblePosition();
                    } else {
                        view.performClick();
                        showEditor();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(110L).start();
                    return true;
                default:
                    return false;
            }
        }

        private void saveBubblePosition() {
            if (bubbleParams == null) {
                return;
            }
            float nx = maxX == 0 ? 0.94f : bubbleParams.x / (float) maxX;
            float ny = maxY == 0 ? 0.48f : bubbleParams.y / (float) maxY;
            AppPrefs.setBubblePosition(service, nx, ny);
        }
    }
}
