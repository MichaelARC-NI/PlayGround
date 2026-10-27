package com.michaelarc.volttrigger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class BubbleView extends View {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint leftPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    BubbleView(Context context) {
        super(context);
        setContentDescription("Abrir editor de gatillos");
        fillPaint.setColor(0xEB0D141D);
        strokePaint.setColor(0xFFFF6B35);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(2));
        leftPaint.setColor(0xFF35D2FF);
        leftPaint.setStyle(Paint.Style.STROKE);
        leftPaint.setStrokeWidth(dp(3));
        leftPaint.setStrokeCap(Paint.Cap.ROUND);
        rightPaint.setColor(0xFFFF7A45);
        rightPaint.setStyle(Paint.Style.STROKE);
        rightPaint.setStrokeWidth(dp(3));
        rightPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setColor(0xFFF3F7FC);
        textPaint.setTextSize(dp(17));
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-black",
                android.graphics.Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - dp(4);
        oval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        fillPaint.setShadowLayer(dp(10), 0f, dp(3), 0x88000000);
        canvas.drawCircle(cx, cy, radius, fillPaint);
        canvas.drawCircle(cx, cy, radius, strokePaint);

        RectF leftArc = new RectF(cx - radius + dp(9), cy - dp(12),
                cx - radius + dp(23), cy + dp(12));
        RectF rightArc = new RectF(cx + radius - dp(23), cy - dp(12),
                cx + radius - dp(9), cy + dp(12));
        canvas.drawArc(leftArc, 100f, 160f, false, leftPaint);
        canvas.drawArc(rightArc, -80f, 160f, false, rightPaint);

        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = cy - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText("V", cx, baseline, textPaint);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
