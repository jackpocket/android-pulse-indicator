package com.jackpocket.pulse;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PulseController {

    public interface PulseEventListener {
        public void onPulseEvent(View target);
    }

    protected WeakReference<View> parent;

    protected WeakReference<View> pulseTarget = new WeakReference<View>(null);
    protected Bitmap pulseTargetDrawingCache;
    protected Rect pulseStartBoundaries = new Rect();

    protected final ArrayList<Pulse> pulses = new ArrayList<Pulse>();

    protected Interpolator alphaInterpolator = new AccelerateInterpolator();
    protected Interpolator scaleInterpolator = new LinearInterpolator();

    protected long durationMs = 1500;
    protected long pulseLifeSpanMs = 900;
    protected long respawnRateMs = 300;
    protected boolean respawnAllowed = true;

    protected float pulseMaxScale = 3;

    protected long startTimeMs = 0;
    protected long lastAddedMs = 0;
    protected boolean circlePathOverride = true;

    protected int pulsingColor;

    protected int pulsingStrokeWidth = -1;
    protected int defaultPulsingStrokeWidth = 1;

    protected PulseTask pulseTask;

    protected WeakReference<PulseEventListener> finishedListener;

    private final Object lock = new Object();

    /**
     * @param parent the non-null View triggering the controller's drawing (i.e. the PulseLayout)
     */
    public PulseController(View parent) {
        if (pulseTarget == null)
            throw new RuntimeException("View supplied to PulseController() cannot be null!");

        this.parent = new WeakReference<View>(parent);

        this.circlePathOverride = parent.getContext()
                .getResources()
                .getBoolean(R.bool.pulse__circle_path_default);

        this.pulseMaxScale = parent.getContext()
                .getResources()
                .getInteger(R.integer.pulse__max_scale_percent_default) / 100f;

        this.durationMs = parent.getContext()
                .getResources()
                .getInteger(R.integer.pulse__duration_default);

        this.pulseLifeSpanMs = parent.getContext()
                .getResources()
                .getInteger(R.integer.pulse__lifespan_default);

        this.respawnRateMs = parent.getContext()
                .getResources()
                .getInteger(R.integer.pulse__respawn_rate_default);

        this.pulsingColor = parent.getContext()
                .getResources()
                .getColor(R.color.pulse__color);
    }

    /**
     * Post {@link PulseController#attachTo(Activity,View)} call on the UI Thread.
     *
     * @param activity
     * @param pulseTarget the non-null target to pulse behind
     */
    public PulseController attachToOnUiThread(Activity activity, View pulseTarget) {
        if (pulseTarget == null)
            throw new RuntimeException("View supplied to PulseController.attachToOnUiThread cannot be null!");

        final WeakReference<Activity> weakActivity = new WeakReference<Activity>(activity);
        final WeakReference<View> weakTarget = new WeakReference<View>(pulseTarget);

        Runnable attachmentRunnable = new Runnable() {
            @Override
            public void run() {
                Activity activity = weakActivity.get();
                View target = weakTarget.get();

                if (activity == null || target == null)
                    return;

                PulseController.this.attachTo(activity, target);
            }
        };

        activity.runOnUiThread(attachmentRunnable);

        return this;
    }

    /**
     * Attach to the target and begin the pulsing sequence.
     *
     * @param activity
     * @param pulseTarget the non-null target to pulse behind
     */
    public PulseController attachTo(Activity activity, View pulseTarget) {
        if (pulseTarget == null)
            throw new RuntimeException("View supplied to PulseController.attachTo cannot be null!");

        this.pulseTarget = new WeakReference<View>(pulseTarget);
        this.pulseStartBoundaries = findViewInParent(activity, pulseTarget);
        this.pulseTargetDrawingCache = getDrawingCache(pulseTarget);
        this.defaultPulsingStrokeWidth = (int) Math.max(5, Math.abs((pulseStartBoundaries.right - pulseStartBoundaries.left)) * .065);
        this.startTimeMs = System.currentTimeMillis();
        this.respawnAllowed = true;

        cancelPulseTask();

        this.pulseTask = new PulseTask(this)
            .setFinishedListener(new Runnable() {
                public void run() {
                    finishPulsing();
                }
            });

        this.pulseTask.start();

        return this;
    }

    protected Rect findViewInParent(Activity activity, View view) {
        Rect viewRect = getWindowLocation(activity, view);

        stripParentPositions(activity, viewRect);

        return viewRect;
    }

    protected void stripParentPositions(Activity activity, Rect rect) {
        Rect parentRect = getWindowLocation(activity, this.parent.get());

        rect.left = rect.left - parentRect.left;
        rect.top = rect.top - parentRect.top;
        rect.right = rect.right - parentRect.left;
        rect.bottom = rect.bottom - parentRect.top;
    }

    protected Rect getWindowLocation(Activity activity, View view) {
        int[] windowLocation = new int[2];

        Rect statusBar = new Rect();

        activity.getWindow()
                .getDecorView()
                .getWindowVisibleDisplayFrame(statusBar);

        view.getLocationInWindow(windowLocation);

        windowLocation[1] = windowLocation[1] - statusBar.height(); // Stupid statusbar ruins the positioning

        Rect rect = new Rect();
        rect.left = windowLocation[0];
        rect.top = windowLocation[1];
        rect.right = windowLocation[0] + view.getWidth();
        rect.bottom = windowLocation[1] + view.getHeight();

        return rect;
    }

    public void draw(Canvas canvas) {
        List<Pulse> pulsesToDraw;

        synchronized (lock) {
            pulsesToDraw = new ArrayList<Pulse>(pulses);
        }

        for (Pulse pulse : pulsesToDraw) {
            pulse.draw(canvas);
        }

        if (pulseTargetDrawingCache == null)
            return;

        canvas.drawBitmap(
                pulseTargetDrawingCache,
                pulseStartBoundaries.left,
                pulseStartBoundaries.top,
                null);
    }

    protected Bitmap getDrawingCache(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();

        Bitmap bitmap = view.getDrawingCache();

        if (bitmap != null) {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());
        }

        view.setDrawingCacheEnabled(false);

        return bitmap;
    }

    public void update() {
        if (!isRunning())
            return;

        addNewPulseIfPossible();

        synchronized (lock) {
            for (Pulse pulse : pulses) {
                pulse.update();
            }

            for (int i = pulses.size() - 1; 0 <= i; i--) {
                if (pulses.get(i).isAlive())
                    continue;

                pulses.remove(i);
            }
        }

        safelyInvalidateParent();
    }

    protected void addNewPulseIfPossible() {
        synchronized (lock) {
            if (isPulseAddingAvailable()) {
                this.lastAddedMs = System.currentTimeMillis();
                this.pulses.add(buildPulse());
            }
        }
    }

    protected Pulse buildPulse() {
        return new Pulse(pulseStartBoundaries, circlePathOverride)
                .setColor(pulsingColor)
                .setStrokeWidth(pulsingStrokeWidth < 1 ? defaultPulsingStrokeWidth : pulsingStrokeWidth)
                .setAlphaInterpolator(alphaInterpolator)
                .setScaleInterpolator(scaleInterpolator)
                .setDuration(pulseLifeSpanMs)
                .setMaxScale(pulseMaxScale);
    }

    protected void safelyInvalidateParent() {
        View parent = this.parent.get();

        if (parent == null)
            return;

        parent.invalidate();
    }

    public boolean isRunning() {
        synchronized (lock) {
            return System.currentTimeMillis() - startTimeMs < durationMs
                    || 0 < pulses.size();
        }
    }

    protected boolean isPulseAddingAvailable() {
        return respawnAllowed
                && System.currentTimeMillis() - startTimeMs < durationMs
                && respawnRateMs < System.currentTimeMillis() - lastAddedMs;
    }

    protected void finishPulsing() {
        View pulseTarget = this.pulseTarget.get();

        stopPulsing();

        final PulseEventListener completionCallback = this.finishedListener.get();

        if (completionCallback != null) {
            completionCallback.onPulseEvent(pulseTarget);
        }
    }

    /**
     * Immediately stop all current and new Pulses from being created.
     * <br><br>
     * Completion callbacks will not be triggered.
     */
    public PulseController stopPulsing() {
        cancelPulseTask();

        synchronized (lock) {
            this.pulses.clear();
        }

        this.pulseTarget = new WeakReference<View>(null);

        safelyInvalidateParent();

        return this;
    }

    protected void cancelPulseTask() {
        if (pulseTask == null)
            return;

        this.pulseTask.cancel();
        this.pulseTask = null;
    }

    /**
     * Suspend the creation of new animated Pulses. This will continue
     * currently-active Pulses until all have been completed, then
     * finish normally.
     */
    public PulseController suspendPulseCreation() {
        this.respawnAllowed = false;

        return this;
    }

    public PulseController setAlphaInterpolator(Interpolator alphaInterpolator) {
        this.alphaInterpolator = alphaInterpolator;

        return this;
    }

    public PulseController setScaleInterpolator(Interpolator scaleInterpolator) {
        this.scaleInterpolator = scaleInterpolator;

        return this;
    }

    public PulseController setDuration(long duration, TimeUnit unit) {
        return setDurationMs(unit.toMillis(duration));
    }

    public PulseController setDurationMs(long durationMs) {
        this.durationMs = durationMs;

        return this;
    }

    public PulseController setPulseLifeSpan(long pulseLifeSpan, TimeUnit unit) {
        return setPulseLifeSpanMs(unit.toMillis(pulseLifeSpan));
    }

    public PulseController setPulseLifeSpanMs(long pulseLifeSpanMs) {
        this.pulseLifeSpanMs = pulseLifeSpanMs;

        return this;
    }

    public PulseController setRespawnRate(long respawnRateMs, TimeUnit unit) {
        return setRespawnRateMs(unit.toMillis(respawnRateMs));
    }

    public PulseController setRespawnRateMs(long respawnRateMs) {
        this.respawnRateMs = respawnRateMs;

        return this;
    }

    public PulseController setPulseMaxScale(float pulseMaxScale) {
        this.pulseMaxScale = pulseMaxScale;

        return this;
    }

    public PulseController setCirclePathOverride(boolean circlePathOverride) {
        this.circlePathOverride = circlePathOverride;

        return this;
    }

    public PulseController setPulsingColor(int pulsingColor) {
        this.pulsingColor = pulsingColor;

        return this;
    }

    public PulseController setPulsingStrokeWidth(int pulsingStrokeWidth){
        this.pulsingStrokeWidth = pulsingStrokeWidth;

        return this;
    }

    /**
     * Set a callback to be triggered on (non-canceled or stopped) pulse completions.
     * <br><br>
     * This callback is weakly held.
     *
     * @param finishedListener the callback to be triggered
     * @return this instance
     */
    public PulseController setFinishedListener(PulseEventListener finishedListener) {
        this.finishedListener = new WeakReference<PulseEventListener>(finishedListener);

        return this;
    }

    public View getParent() {
        return parent.get();
    }

    public long getDurationMs() {
        return durationMs;
    }
}
