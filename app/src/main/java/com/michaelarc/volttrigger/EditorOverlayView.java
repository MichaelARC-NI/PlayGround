package com.michaelarc.volttrigger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

final class EditorOverlayView extends View {

    interface Listener {
        void onSave(float leftX, float leftY, float rightX, float rightY);

        void onCancel();
    }

    private static final int BUTTON_NONE = 0;
    private static final int BUTTON_CANCEL = 1;
    private static final int BUTTON_RESET = 2;
    private static final int BUTTON_SAVE = 3;
    private static final int BUTTON_CLOSE = 4;

    private final Listener listener;
    private final Paint dimPaint = new Paint();
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint topScrimPaint = new Paint();
    private final Paint bottomScrimPaint = new Paint();
    private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crosshairPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF scratch = new RectF();
    private final RectF scratch2 = new RectF();
    private final Path topScrimPath = new Path();
    private final Path bottomScrimPath = new Path();

    private final RectF cancelRect = new RectF();
    private final RectF resetRect = new RectF();
    private final RectF saveRect = new RectF();
    private final RectF closeRect = new RectF();

    private float leftX;
    private float leftY;
    private float rightX;
    private float rightY;

    private int insetLeft;
    private int insetTop;
    private int insetRight;
    private int insetBottom;

    private Trigger dragging;
    private float dragOffsetX;
    private float dragOffsetY;
    private float downX;
    private float downY;
    private int activeButton = BUTTON_NONE;

    EditorOverlayView(Context context, float leftX, float leftY, float rightX, float rightY,
            Listener listener) {
        super(context);
        this.leftX = leftX;
        this.leftY = leftY;
        this.rightX = rightX;
        this.rightY = rightY;
        this.listener = listener;
        setClickable(true);
        setFocusable(false);
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        dimPaint.setColor(0xA805080C);
        gridPaint.setColor(0x1FFFFFFF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1));
        topScrimPaint.setShader(new LinearGradient(
                0f, 0f, 0f, dp(190), new int[]{0xE605080C, 0x0005080C}, null,
                Shader.TileMode.CLAMP));
        bottomScrimPaint.setShader(new LinearGradient(
                0f, 0f, 0f, dp(230), new int[]{0x0005080C, 0xF205080C}, null,
                Shader.TileMode.CLAMP));

        titlePaint.setColor(0xFFF3F7FC);
        titlePaint.setTextSize(sp(25));
        titlePaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));

        subtitlePaint.setColor(0xFF9AA8B8);
        subtitlePaint.setTextSize(sp(13));
        subtitlePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        labelPaint.setColor(0xFFFF6B35);
        labelPaint.setTextSize(sp(10));
        labelPaint.setLetterSpacing(0.16f);
        labelPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        chipTextPaint.setColor(0xFFDDE6F0);
        chipTextPaint.setTextSize(sp(12));
        chipTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        buttonTextPaint.setColor(0xFFFFFFFF);
        buttonTextPaint.setTextSize(sp(14));
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(dp(1));
        crosshairPaint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{dp(5), dp(7)}, 0f));

        glowPaint.setStyle(Paint.Style.FILL);
        markerFillPaint.setStyle(Paint.Style.FILL);
        markerStrokePaint.setStyle(Paint.Style.STROKE);
        markerStrokePaint.setStrokeWidth(dp(2.5f));
        markerTextPaint.setTextAlign(Paint.Align.CENTER);
        markerTextPaint.setTextSize(sp(21));
        markerTextPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        markerTextPaint.setColor(Color.WHITE);

        pillPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateInsets();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateInsets();
        layoutButtons();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        updateInsets();
        invalidate();
        return super.onApplyWindowInsets(insets);
    }

    private void updateInsets() {
        WindowInsets insets = getRootWindowInsets();
        if (insets == null) {
            return;
        }
        insetLeft = insets.getSystemWindowInsetLeft();
        insetTop = insets.getSystemWindowInsetTop();
        insetRight = insets.getSystemWindowInsetRight();
        insetBottom = insets.getSystemWindowInsetBottom();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && insets.getDisplayCutout() != null) {
            insetLeft = Math.max(insetLeft, insets.getDisplayCutout().getSafeInsetLeft());
            insetTop = Math.max(insetTop, insets.getDisplayCutout().getSafeInsetTop());
            insetRight = Math.max(insetRight, insets.getDisplayCutout().getSafeInsetRight());
            insetBottom = Math.max(insetBottom, insets.getDisplayCutout().getSafeInsetBottom());
        }
        layoutButtons();
    }

    private void layoutButtons() {
        float margin = dp(20);
        float gap = dp(8);
        float available = getWidth() - insetLeft - insetRight - margin * 2f;
        if (available <= 0f) {
            return;
        }
        float buttonHeight = dp(50);
        float bottom = getHeight() - insetBottom - dp(22);
        float top = bottom - buttonHeight;
        float cancelWidth = available * 0.27f;
        float resetWidth = available * 0.24f;
        float saveWidth = available - cancelWidth - resetWidth - gap * 2f;
        float x = insetLeft + margin;

        cancelRect.set(x, top, x + cancelWidth, bottom);
        x += cancelWidth + gap;
        resetRect.set(x, top, x + resetWidth, bottom);
        x += resetWidth + gap;
        saveRect.set(x, top, x + saveWidth, bottom);

        float closeSize = dp(42);
        closeRect.set(getWidth() - insetRight - margin - closeSize,
                insetTop + dp(18),
                getWidth() - insetRight - margin,
                insetTop + dp(18) + closeSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();

        canvas.drawRect(0f, 0f, w, h, dimPaint);
        drawGrid(canvas, w, h);

        topScrimPath.reset();
        topScrimPath.addRect(0f, 0f, w, dp(190), Path.Direction.CW);
        canvas.drawPath(topScrimPath, topScrimPaint);

        bottomScrimPath.reset();
        bottomScrimPath.addRect(0f, h - dp(240), w, h, Path.Direction.CW);
        canvas.drawPath(bottomScrimPath, bottomScrimPaint);

        drawHeader(canvas);
        drawMarkers(canvas);
        drawActions(canvas);
    }

    private void drawGrid(Canvas canvas, float w, float h) {
        for (int i = 1; i < 8; i++) {
            float x = insetLeft + (w - insetLeft - insetRight) * i / 8f;
            float y = insetTop + (h - insetTop - insetBottom) * i / 8f;
            canvas.drawLine(x, insetTop, x, h - insetBottom, gridPaint);
            canvas.drawLine(insetLeft, y, w - insetRight, y, gridPaint);
        }
        scratch.set(insetLeft + dp(12), insetTop + dp(12),
                w - insetRight - dp(12), h - insetBottom - dp(12));
        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1));
        border.setColor(0x33FFFFFF);
        canvas.drawRoundRect(scratch, dp(20), dp(20), border);
    }

    private void drawHeader(Canvas canvas) {
        float x = insetLeft + dp(24);
        float labelY = insetTop + dp(34);
        canvas.drawText("MODO AJUSTE", x, labelY, labelPaint);

        titlePaint.setTextSize(sp(25));
        float titleY = labelY + dp(31);
        canvas.drawText("Ajusta tus gatillos", x, titleY, titlePaint);

        float subtitleY = titleY + dp(21);
        canvas.drawText("Arrastra L y R hasta el botón del juego.", x, subtitleY,
                subtitlePaint);
        canvas.drawText("Se guarda como % de la pantalla.", x, subtitleY + dp(18),
                subtitlePaint);

        float chipY = subtitleY + dp(34);
        float chipH = dp(28);
        String leftChip = "L  " + percent(leftX) + " · " + percent(leftY);
        String rightChip = "R  " + percent(rightX) + " · " + percent(rightY);
        float leftWidth = chipTextPaint.measureText(leftChip) + dp(24);
        scratch.set(x, chipY, x + leftWidth, chipY + chipH);
        drawChip(canvas, scratch, 0xFF35D2FF, leftChip);
        float rightXPos = x + leftWidth + dp(8);
        float rightWidth = chipTextPaint.measureText(rightChip) + dp(24);
        scratch2.set(rightXPos, chipY, rightXPos + rightWidth, chipY + chipH);
        drawChip(canvas, scratch2, 0xFFFF7A45, rightChip);

        pillPaint.setColor(activeButton == BUTTON_CLOSE ? 0xFF273444 : 0xCC121A24);
        canvas.drawCircle(closeRect.centerX(), closeRect.centerY(), closeRect.width() / 2f,
                pillPaint);
        Paint closeStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        closeStroke.setStyle(Paint.Style.STROKE);
        closeStroke.setStrokeWidth(dp(2));
        closeStroke.setStrokeCap(Paint.Cap.ROUND);
        closeStroke.setColor(0xFFF3F7FC);
        float arm = dp(7);
        canvas.drawLine(closeRect.centerX() - arm, closeRect.centerY() - arm,
                closeRect.centerX() + arm, closeRect.centerY() + arm, closeStroke);
        canvas.drawLine(closeRect.centerX() + arm, closeRect.centerY() - arm,
                closeRect.centerX() - arm, closeRect.centerY() + arm, closeStroke);
    }

    private void drawChip(Canvas canvas, RectF rect, int accentColor, String text) {
        pillPaint.setColor(0xCC121A24);
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, pillPaint);
        Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(dp(1));
        outline.setColor((accentColor & 0x00FFFFFF) | 0x66000000);
        canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, outline);
        float tx = rect.left + dp(12);
        float baseline = rect.centerY() - (chipTextPaint.ascent() + chipTextPaint.descent()) / 2f;
        canvas.drawText(text, tx, baseline, chipTextPaint);
    }

    private void drawMarkers(Canvas canvas) {
        drawMarker(canvas, Trigger.LEFT, leftX, leftY);
        drawMarker(canvas, Trigger.RIGHT, rightX, rightY);
    }

    private void drawMarker(Canvas canvas, Trigger trigger, float nx, float ny) {
        float cx = nx * getWidth();
        float cy = ny * getHeight();
        float radius = dp(31);
        boolean active = dragging == trigger;

        crosshairPaint.setColor((trigger.color & 0x00FFFFFF) | 0x55000000);
        canvas.drawLine(cx, insetTop, cx, getHeight() - insetBottom, crosshairPaint);
        canvas.drawLine(insetLeft, cy, getWidth() - insetRight, cy, crosshairPaint);

        glowPaint.setColor((trigger.color & 0x00FFFFFF) | 0x33000000);
        glowPaint.setShadowLayer(dp(18), 0f, 0f, trigger.color);
        canvas.drawCircle(cx, cy, radius + dp(12), glowPaint);
        glowPaint.clearShadowLayer();

        markerFillPaint.setColor(0xE60D141D);
        canvas.drawCircle(cx, cy, radius, markerFillPaint);
        markerStrokePaint.setColor(trigger.color);
        markerStrokePaint.setStrokeWidth(dp(active ? 4 : 2.5f));
        canvas.drawCircle(cx, cy, radius, markerStrokePaint);
        canvas.drawCircle(cx, cy, dp(5), markerStrokePaint);

        Paint.FontMetrics metrics = markerTextPaint.getFontMetrics();
        float baseline = cy - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(trigger.badge, cx, baseline, markerTextPaint);

        labelPaint.setColor(trigger.color);
        labelPaint.setTextSize(sp(11));
        labelPaint.setLetterSpacing(0.04f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        String coordinates = percent(nx) + " · " + percent(ny);
        canvas.drawText(coordinates, cx, cy + radius + dp(20), labelPaint);
        labelPaint.setLetterSpacing(0.16f);
        labelPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawActions(Canvas canvas) {
        labelPaint.setColor(0xFF8E9BAC);
        labelPaint.setTextSize(sp(10));
        labelPaint.setLetterSpacing(0.08f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        String hint = dragging == null
                ? "TOCA Y ARRASTRA LOS PUNTOS"
                : "SUELTA PARA FIJAR EL PUNTO";
        canvas.drawText(hint, getWidth() / 2f,
                saveRect.top - dp(18), labelPaint);
        labelPaint.setLetterSpacing(0.16f);
        labelPaint.setTextAlign(Paint.Align.LEFT);

        drawButton(canvas, cancelRect, "Cancelar", false, activeButton == BUTTON_CANCEL);
        drawButton(canvas, resetRect, "Reset", false, activeButton == BUTTON_RESET);
        drawButton(canvas, saveRect, "Guardar", true, activeButton == BUTTON_SAVE);
    }

    private void drawButton(Canvas canvas, RectF rect, String text, boolean primary,
            boolean pressed) {
        pillPaint.setColor(primary
                ? (pressed ? 0xFFFF8A5C : 0xFFFF6B35)
                : (pressed ? 0xFF27374A : 0xE61A2430));
        canvas.drawRoundRect(rect, dp(14), dp(14), pillPaint);
        Paint.FontMetrics metrics = buttonTextPaint.getFontMetrics();
        float baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(text, rect.centerX(), baseline, buttonTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                activeButton = buttonAt(downX, downY);
                if (activeButton != BUTTON_NONE) {
                    invalidate();
                    return true;
                }
                dragging = markerAt(downX, downY);
                if (dragging != null) {
                    float cx = pixelX(dragging);
                    float cy = pixelY(dragging);
                    dragOffsetX = downX - cx;
                    dragOffsetY = downY - cy;
                    invalidate();
                    return true;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging != null) {
                    moveMarker(dragging, event.getX() - dragOffsetX,
                            event.getY() - dragOffsetY);
                    invalidate();
                    return true;
                }
                return activeButton != BUTTON_NONE;
            case MotionEvent.ACTION_UP:
                if (dragging != null) {
                    dragging = null;
                    invalidate();
                    return true;
                }
                if (activeButton != BUTTON_NONE) {
                    int selected = activeButton;
                    activeButton = BUTTON_NONE;
                    invalidate();
                    if (buttonAt(event.getX(), event.getY()) == selected) {
                        performClick();
                        activateButton(selected);
                    }
                    return true;
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = null;
                activeButton = BUTTON_NONE;
                invalidate();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void activateButton(int button) {
        if (button == BUTTON_CANCEL || button == BUTTON_CLOSE) {
            listener.onCancel();
        } else if (button == BUTTON_RESET) {
            leftX = Trigger.LEFT.defaultX;
            leftY = Trigger.LEFT.defaultY;
            rightX = Trigger.RIGHT.defaultX;
            rightY = Trigger.RIGHT.defaultY;
            invalidate();
        } else if (button == BUTTON_SAVE) {
            listener.onSave(leftX, leftY, rightX, rightY);
        }
    }

    private int buttonAt(float x, float y) {
        if (closeRect.contains(x, y)) {
            return BUTTON_CLOSE;
        }
        if (cancelRect.contains(x, y)) {
            return BUTTON_CANCEL;
        }
        if (resetRect.contains(x, y)) {
            return BUTTON_RESET;
        }
        if (saveRect.contains(x, y)) {
            return BUTTON_SAVE;
        }
        return BUTTON_NONE;
    }

    private Trigger markerAt(float x, float y) {
        float radius = dp(48);
        float leftDistance = distanceSquared(x, y, pixelX(Trigger.LEFT), pixelY(Trigger.LEFT));
        float rightDistance = distanceSquared(x, y, pixelX(Trigger.RIGHT), pixelY(Trigger.RIGHT));
        if (leftDistance <= radius * radius && leftDistance <= rightDistance) {
            return Trigger.LEFT;
        }
        if (rightDistance <= radius * radius) {
            return Trigger.RIGHT;
        }
        return null;
    }

    private void moveMarker(Trigger trigger, float x, float y) {
        float margin = dp(34);
        float minX = insetLeft + margin;
        float maxX = getWidth() - insetRight - margin;
        float minY = insetTop + margin;
        float maxY = getHeight() - insetBottom - margin;
        float clampedX = Math.max(minX, Math.min(maxX, x));
        float clampedY = Math.max(minY, Math.min(maxY, y));
        float nx = clampedX / Math.max(1f, getWidth());
        float ny = clampedY / Math.max(1f, getHeight());
        if (trigger == Trigger.LEFT) {
            leftX = nx;
            leftY = ny;
        } else {
            rightX = nx;
            rightY = ny;
        }
    }

    private float pixelX(Trigger trigger) {
        return (trigger == Trigger.LEFT ? leftX : rightX) * getWidth();
    }

    private float pixelY(Trigger trigger) {
        return (trigger == Trigger.LEFT ? leftY : rightY) * getHeight();
    }

    private static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    private String percent(float value) {
        return Math.round(value * 100f) + "%";
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
