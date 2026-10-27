package com.michaelarc.volttrigger;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Serializes all injected pointers through one accessibility gesture stream. Held triggers are
 * kept down with continued strokes, which also lets two shoulders stay pressed at once.
 */
final class GestureInjector {

    private static final long DOWN_STROKE_MS = 1L;
    private static final long CONTINUE_STROKE_MS = 1L;
    private static final long ADD_POINTER_DELAY_MS = 8L;
    private static final long DISPATCH_DELAY_MS = 4L;

    private final AccessibilityService service;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<Trigger, Hold> holds = new EnumMap<>(Trigger.class);
    private final List<Hold> retiringHolds = new ArrayList<>();
    private final List<PendingTap> pendingTaps = new ArrayList<>();

    private boolean dispatchInFlight;
    private boolean dispatchRequested;
    private boolean scheduled;

    private final Runnable dispatchRunnable = new Runnable() {
        @Override
        public void run() {
            scheduled = false;
            if (dispatchInFlight) {
                dispatchRequested = true;
                return;
            }
            dispatchNow();
        }
    };

    GestureInjector(AccessibilityService service) {
        this.service = service;
    }

    void tap(Trigger trigger) {
        PointF point = pointFor(trigger);
        PendingTap tap = new PendingTap(trigger, pathAt(point));
        pendingTaps.add(tap);
        if (pendingTaps.size() > 4) {
            pendingTaps.remove(0);
        }
        requestDispatch();
    }

    void holdDown(Trigger trigger) {
        Hold previous = holds.get(trigger);
        if (previous != null && !previous.releasing) {
            return;
        }
        if (previous != null) {
            holds.remove(trigger);
            if (previous.stroke != null) {
                retiringHolds.add(previous);
            }
        }
        Hold hold = new Hold(trigger, pathAt(pointFor(trigger)));
        holds.put(trigger, hold);
        requestDispatch();
    }

    void holdUp(Trigger trigger) {
        Hold hold = holds.get(trigger);
        if (hold == null || hold.releasing) {
            return;
        }
        hold.releasing = true;
        requestDispatch();
    }

    void releaseAll() {
        pendingTaps.clear();
        for (Hold hold : holds.values()) {
            hold.releasing = true;
        }
        requestDispatch();
    }

    void destroy() {
        handler.removeCallbacksAndMessages(null);
        pendingTaps.clear();
        holds.clear();
        retiringHolds.clear();
        dispatchInFlight = false;
        dispatchRequested = false;
        scheduled = false;
    }

    PointF pointFor(Trigger trigger) {
        Rect bounds = screenBounds(service);
        float normalizedX = AppPrefs.getTargetX(service, trigger);
        float normalizedY = AppPrefs.getTargetY(service, trigger);
        float x = bounds.left + normalizedX * Math.max(1, bounds.width() - 1);
        float y = bounds.top + normalizedY * Math.max(1, bounds.height() - 1);
        x = Math.max(bounds.left + 1f, Math.min(bounds.right - 2f, x));
        y = Math.max(bounds.top + 1f, Math.min(bounds.bottom - 2f, y));
        return new PointF(x, y);
    }

    static Rect screenBounds(Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            return new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return new Rect(windowManager.getCurrentWindowMetrics().getBounds());
        }
        DisplayMetrics metrics = new DisplayMetrics();
        Display display = windowManager.getDefaultDisplay();
        if (display != null) {
            display.getRealMetrics(metrics);
        } else {
            metrics = context.getResources().getDisplayMetrics();
        }
        return new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
    }

    private static Path pathAt(PointF point) {
        Path path = new Path();
        path.moveTo(point.x, point.y);
        return path;
    }

    private void requestDispatch() {
        dispatchRequested = true;
        if (scheduled) {
            return;
        }
        scheduled = true;
        handler.postDelayed(dispatchRunnable, DISPATCH_DELAY_MS);
    }

    private void dispatchNow() {
        dispatchRequested = false;

        boolean hasExistingPointer = !retiringHolds.isEmpty();
        if (!hasExistingPointer) {
            for (Hold hold : holds.values()) {
                if (hold.stroke != null) {
                    hasExistingPointer = true;
                    break;
                }
            }
        }

        boolean hasNewPointer = !pendingTaps.isEmpty();
        if (!hasNewPointer) {
            for (Hold hold : holds.values()) {
                if (hold.stroke == null && !hold.releasing) {
                    hasNewPointer = true;
                    break;
                }
            }
        }

        long newPointerStart = (hasExistingPointer && hasNewPointer)
                ? ADD_POINTER_DELAY_MS : 0L;
        long continueDuration = hasNewPointer
                ? Math.max(CONTINUE_STROKE_MS, ADD_POINTER_DELAY_MS + 2L)
                : CONTINUE_STROKE_MS;

        List<StrokeDescriptionUpdate> updates = new ArrayList<>();
        List<GestureDescription.StrokeDescription> strokes = new ArrayList<>();

        Iterator<Map.Entry<Trigger, Hold>> iterator = holds.entrySet().iterator();
        while (iterator.hasNext()) {
            Hold hold = iterator.next().getValue();
            if (hold.releasing && hold.stroke == null) {
                iterator.remove();
                continue;
            }
            GestureDescription.StrokeDescription stroke;
            if (hold.stroke == null) {
                stroke = new GestureDescription.StrokeDescription(
                        hold.path, newPointerStart, DOWN_STROKE_MS, true);
            } else {
                long duration = hold.releasing ? CONTINUE_STROKE_MS : continueDuration;
                stroke = hold.stroke.continueStroke(
                        hold.path, 0L, duration, !hold.releasing);
            }
            strokes.add(stroke);
            updates.add(new StrokeDescriptionUpdate(hold, stroke));
        }

        for (Hold retiring : retiringHolds) {
            if (retiring.stroke != null) {
                strokes.add(retiring.stroke.continueStroke(
                        retiring.path, 0L, CONTINUE_STROKE_MS, false));
            }
        }

        int tapDuration = AppPrefs.getTapDurationMs(service);
        for (PendingTap tap : pendingTaps) {
            strokes.add(new GestureDescription.StrokeDescription(
                    tap.path, newPointerStart, tapDuration, false));
        }

        if (strokes.isEmpty()) {
            return;
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        for (GestureDescription.StrokeDescription stroke : strokes) {
            builder.addStroke(stroke);
        }

        final boolean[] finished = new boolean[1];
        boolean accepted = service.dispatchGesture(
                builder.build(),
                new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        finishDispatch(finished);
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        cancelActivePointers();
                        finishDispatch(finished);
                    }
                },
                null);

        if (!accepted) {
            handler.postDelayed(dispatchRunnable, 40L);
            scheduled = true;
            return;
        }

        for (StrokeDescriptionUpdate update : updates) {
            update.hold.stroke = update.stroke;
        }
        Iterator<Map.Entry<Trigger, Hold>> removal = holds.entrySet().iterator();
        while (removal.hasNext()) {
            if (removal.next().getValue().releasing) {
                removal.remove();
            }
        }
        retiringHolds.clear();
        pendingTaps.clear();
        dispatchInFlight = true;
    }

    private void cancelActivePointers() {
        pendingTaps.clear();
        retiringHolds.clear();
        Iterator<Map.Entry<Trigger, Hold>> iterator = holds.entrySet().iterator();
        while (iterator.hasNext()) {
            Hold hold = iterator.next().getValue();
            if (hold.releasing) {
                iterator.remove();
            } else {
                hold.stroke = null;
            }
        }
    }

    private void finishDispatch(boolean[] finished) {
        if (finished[0]) {
            return;
        }
        finished[0] = true;
        dispatchInFlight = false;
        if (dispatchRequested && !scheduled) {
            scheduled = true;
            handler.postDelayed(dispatchRunnable, DISPATCH_DELAY_MS);
        }
    }

    private static final class Hold {
        final Trigger trigger;
        final Path path;
        GestureDescription.StrokeDescription stroke;
        boolean releasing;

        Hold(Trigger trigger, Path path) {
            this.trigger = trigger;
            this.path = path;
        }
    }

    private static final class PendingTap {
        final Trigger trigger;
        final Path path;

        PendingTap(Trigger trigger, Path path) {
            this.trigger = trigger;
            this.path = path;
        }
    }

    private static final class StrokeDescriptionUpdate {
        final Hold hold;
        final GestureDescription.StrokeDescription stroke;

        StrokeDescriptionUpdate(Hold hold, GestureDescription.StrokeDescription stroke) {
            this.hold = hold;
            this.stroke = stroke;
        }
    }
}
