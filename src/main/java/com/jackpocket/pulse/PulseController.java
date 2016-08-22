package com.jackpocket.pulse;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.jackpcoket.pulse.R;

import java.util.ArrayList;
import java.util.List;

public class PulseController {

    public interface PulseEventListener {
        public void onPulseEvent(View target);
    }

    protected View parent;

    protected View pulseTarget;
    protected Bitmap pulseTargetDrawingCache;
    protected Rect pulseStartBoundaries = new Rect();

    protected List<Pulse> pulses = new ArrayList<Pulse>();

    protected Interpolator alphaInterpolator = new AccelerateInterpolator();
    protected Interpolator scaleInterpolator = new LinearInterpolator();

    protected long durationMs = 1500;
    protected long pulseLifeSpanMs = 900;
    protected long respawnRateMs = 300;

    protected float pulseMaxScale = 3;

    protected long startTimeMs = 0;
    protected long lastAddedMs = 0;
    protected boolean circlePathOverride = true;

    protected int pulsingColor;

    protected PulseTask pulseTask;

    protected PulseEventListener finishedListener;

    /**
     * @param parent the View triggering the controller's drawing (i.e. the PulseLayout)
     */
    public PulseController(View parent){
        this.parent = parent;

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
     * Attach to the target and begin the pulsing sequence
     * @param activity
     * @param pulseTarget the target to pulse behind
     */
    public PulseController attachTo(Activity activity, View pulseTarget){
        this.pulseTarget = pulseTarget;
        this.pulseStartBoundaries = findViewInParent(activity, pulseTarget);
        this.pulseTargetDrawingCache = getDrawingCache(pulseTarget);
        this.startTimeMs = System.currentTimeMillis();

        cancelPulseTask();

        pulseTask = new PulseTask(this)
            .setFinishedListener(new Runnable() {
                public void run() {
                    finishPulsing();
                }
            });

        pulseTask.start();

        return this;
    }

    protected Rect findViewInParent(Activity activity, View view){
        Rect viewRect = getWindowLocation(activity, pulseTarget);

        stripParentPositions(activity, viewRect);

        return viewRect;
    }

    protected void stripParentPositions(Activity activity, Rect rect){
        Rect parentRect = getWindowLocation(activity, parent);

        rect.left = rect.left - parentRect.left;
        rect.top = rect.top - parentRect.top;
        rect.right = rect.right - parentRect.left;
        rect.bottom = rect.bottom - parentRect.top;
    }

    protected Rect getWindowLocation(Activity activity, View view){
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

    public void draw(Canvas canvas){
        for(Pulse pulse : pulses)
            pulse.draw(canvas);

        if(pulseTargetDrawingCache == null)
            return;

        canvas.drawBitmap(pulseTargetDrawingCache,
                pulseStartBoundaries.left,
                pulseStartBoundaries.top,
                null);
    }

    protected Bitmap getDrawingCache(View view) {
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();

        Bitmap bitmap = view.getDrawingCache();

        if(bitmap != null)
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight());

        view.setDrawingCacheEnabled(false);

        return bitmap;
    }

    public void update(){
        if(!isRunning())
            return;

        addNewPulseIfPossible();

        for(Pulse pulse : pulses)
            pulse.update();

        for(int i = pulses.size() - 1; 0 <= i; i--)
            if(!pulses.get(i).isAlive())
                pulses.remove(i);

        parent.invalidate();
    }

    protected void addNewPulseIfPossible(){
        if(isPulseAddingAvailable()){
            this.lastAddedMs = System.currentTimeMillis();
            this.pulses.add(buildPulse());
        }
    }

    protected Pulse buildPulse(){
        return new Pulse(pulseStartBoundaries, circlePathOverride)
                .setColor(pulsingColor)
                .setAlphaInterpolator(alphaInterpolator)
                .setScaleInterpolator(scaleInterpolator)
                .setDuration(pulseLifeSpanMs)
                .setMaxScale(pulseMaxScale);
    }

    public boolean isRunning(){
        return System.currentTimeMillis() - startTimeMs < durationMs
                || 0 < pulses.size();
    }

    protected boolean isPulseAddingAvailable(){
        return System.currentTimeMillis() - startTimeMs < durationMs
                && respawnRateMs < System.currentTimeMillis() - lastAddedMs;
    }

    protected void finishPulsing(){
        cancelPulseTask();

        pulseTargetDrawingCache = null;

        if(finishedListener != null)
            finishedListener.onPulseEvent(pulseTarget);

        pulseTarget = null;
    }

    public PulseController stopPulsing(){
        cancelPulseTask();

        this.pulses = new ArrayList<Pulse>();

        return this;
    }

    protected void cancelPulseTask(){
        if(pulseTask != null)
            pulseTask.cancel();

        pulseTask = null;
    }

    public PulseController setAlphaInterpolator(Interpolator alphaInterpolator) {
        this.alphaInterpolator = alphaInterpolator;
        return this;
    }

    public PulseController setScaleInterpolator(Interpolator scaleInterpolator) {
        this.scaleInterpolator = scaleInterpolator;
        return this;
    }

    public PulseController setDurationMs(long durationMs) {
        this.durationMs = durationMs;
        return this;
    }

    public PulseController setPulseLifeSpanMs(long pulseLifeSpanMs) {
        this.pulseLifeSpanMs = pulseLifeSpanMs;
        return this;
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

    public PulseController setFinishedListener(PulseEventListener finishedListener) {
        this.finishedListener = finishedListener;
        return this;
    }

    public View getParent(){
        return parent;
    }

    public long getDurationMs(){
        return durationMs;
    }

}
