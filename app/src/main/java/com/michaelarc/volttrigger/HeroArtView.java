package com.michaelarc.volttrigger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

final class HeroArtView extends View {

    private final Paint phonePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint screenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF phone = new RectF();
    private final RectF screen = new RectF();
    private final RectF keyOne = new RectF();
    private final RectF keyTwo = new RectF();
    private final Path arrow = new Path();

    HeroArtView(Context context) {
        super(context);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        phonePaint.setColor(0xFF141D28);
        screenPaint.setColor(0xFF070B11);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.2f));
        strokePaint.setColor(0xFF33465B);
        glowPaint.setStyle(Paint.Style.STROKE);
        glowPaint.setStrokeWidth(dp(2));
        keyPaint.setColor(0xFF243342);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(dp(1.3f));
        arrowPaint.setPathEffect(new android.graphics.DashPathEffect(
                new float[]{dp(3), dp(4)}, 0f));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(dp(13));
        textPaint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        textPaint.setColor(0xFFFFFFFF);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float phoneW = dp(104);
        float phoneH = dp(136);
        float top = dp(5);
        phone.set(cx - phoneW / 2f, top, cx + phoneW / 2f, top + phoneH);
        screen.set(phone.left + dp(5), phone.top + dp(5),
                phone.right - dp(5), phone.bottom - dp(5));

        canvas.drawRoundRect(phone, dp(17), dp(17), phonePaint);
        canvas.drawRoundRect(phone, dp(17), dp(17), strokePaint);
        canvas.drawRoundRect(screen, dp(13), dp(13), screenPaint);

        drawGame(canvas);
        drawKeys(canvas);
        drawArrows(canvas);
    }

    private void drawGame(Canvas canvas) {
        Paint scene = new Paint(Paint.ANTI_ALIAS_FLAG);
        scene.setColor(0xFF101B28);
        canvas.drawRect(screen.left, screen.top, screen.right,
                screen.top + screen.height() * 0.46f, scene);
        scene.setColor(0xFF0A121C);
        canvas.drawRect(screen.left, screen.top + screen.height() * 0.46f,
                screen.right, screen.bottom, scene);

        Paint horizon = new Paint(Paint.ANTI_ALIAS_FLAG);
        horizon.setColor(0xFF1D344A);
        horizon.setStrokeWidth(dp(1));
        canvas.drawLine(screen.left, screen.top + screen.height() * 0.46f,
                screen.right, screen.top + screen.height() * 0.46f, horizon);

        float leftX = screen.left + screen.width() * 0.12f;
        float rightX = screen.right - screen.width() * 0.12f;
        float triggerY = screen.top + screen.height() * 0.30f;
        drawTrigger(canvas, leftX, triggerY, 0xFF35D2FF, "L");
        drawTrigger(canvas, rightX, triggerY, 0xFFFF7A45, "R");

        Paint control = new Paint(Paint.ANTI_ALIAS_FLAG);
        control.setStyle(Paint.Style.STROKE);
        control.setStrokeWidth(dp(1.5f));
        control.setColor(0x55FFFFFF);
        canvas.drawCircle(screen.left + screen.width() * 0.22f,
                screen.bottom - screen.height() * 0.18f, dp(16), control);
        canvas.drawCircle(screen.left + screen.width() * 0.22f,
                screen.bottom - screen.height() * 0.18f, dp(6), control);
        canvas.drawCircle(screen.right - screen.width() * 0.20f,
                screen.bottom - screen.height() * 0.18f, dp(9), control);
        canvas.drawCircle(screen.right - screen.width() * 0.36f,
                screen.bottom - screen.height() * 0.15f, dp(7), control);
    }

    private void drawTrigger(Canvas canvas, float x, float y, int color, String label) {
        glowPaint.setColor((color & 0x00FFFFFF) | 0x66000000);
        glowPaint.setShadowLayer(dp(8), 0f, 0f, color);
        canvas.drawCircle(x, y, dp(12), glowPaint);
        glowPaint.clearShadowLayer();

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(0xCC0B121A);
        canvas.drawCircle(x, y, dp(11), fill);
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(dp(2));
        ring.setColor(color);
        canvas.drawCircle(x, y, dp(11), ring);
        textPaint.setTextSize(dp(10));
        textPaint.setColor(ColorUtil.WHITE);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        canvas.drawText(label, x, y - (metrics.ascent + metrics.descent) / 2f, textPaint);
    }

    private void drawKeys(Canvas canvas) {
        float keyX = phone.right + dp(4);
        keyOne.set(keyX, phone.top + dp(42), keyX + dp(4), phone.top + dp(67));
        keyTwo.set(keyX, phone.top + dp(73), keyX + dp(4), phone.top + dp(98));
        keyPaint.setColor(0xFF35D2FF);
        canvas.drawRoundRect(keyOne, dp(2), dp(2), keyPaint);
        keyPaint.setColor(0xFFFF7A45);
        canvas.drawRoundRect(keyTwo, dp(2), dp(2), keyPaint);

        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setTextSize(dp(8));
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        label.setColor(0xFF7E8C9D);
        label.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("VOL +", keyX + dp(9), keyOne.centerY() + dp(3), label);
        canvas.drawText("VOL −", keyX + dp(9), keyTwo.centerY() + dp(3), label);
    }

    private void drawArrows(Canvas canvas) {
        float keyX = phone.right + dp(5);
        float leftTargetX = screen.left + screen.width() * 0.12f;
        float rightTargetX = screen.right - screen.width() * 0.12f;
        float targetY = screen.top + screen.height() * 0.30f;

        arrowPaint.setColor(0x6635D2FF);
        arrow.reset();
        arrow.moveTo(keyX, phone.top + dp(54));
        arrow.quadTo(phone.right + dp(22), phone.top + dp(20),
                leftTargetX, targetY);
        canvas.drawPath(arrow, arrowPaint);

        arrowPaint.setColor(0x66FF7A45);
        arrow.reset();
        arrow.moveTo(keyX, phone.top + dp(86));
        arrow.quadTo(phone.right + dp(22), phone.top + dp(122),
                rightTargetX, targetY);
        canvas.drawPath(arrow, arrowPaint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private static final class ColorUtil {
        static final int WHITE = 0xFFFFFFFF;
    }
}
