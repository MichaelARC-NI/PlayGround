package com.michaelarc.volttrigger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

final class FlashView extends View {

    private final int color;
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);

    FlashView(Context context, int color) {
        super(context);
        this.color = color;
        ring.setColor(color);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(dp(3));
        dot.setColor(0xFFFFFFFF);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - dp(8);
        ring.setShadowLayer(dp(8), 0f, 0f, color);
        canvas.drawCircle(cx, cy, radius, ring);
        canvas.drawCircle(cx, cy, dp(3), dot);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
