package com.jackpocket.pulse;

import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.animation.Interpolator;

public class Pulse {

    private static final int MAX_ALPHA = 255;

    protected Paint paint;
    protected Interpolator alphaInterpolator;
    protected Interpolator scaleInterpolator;

    protected Rect startBoundaries;
    protected Path path;

    protected boolean circlePathOverride = false;

    protected float maxScale = 10f;
    protected float scale = 1f;

    protected long createdAt = 0;
    protected long duration = 1000;

    protected int[] centers;
    protected int radius = 0;

    public Pulse(Rect startBoundaries){
        this(startBoundaries, true);
    }

    public Pulse(Rect startBoundaries, boolean circlePathOverride){
        this.startBoundaries = startBoundaries;
        this.circlePathOverride = circlePathOverride;
        this.paint = buildPaint();

        this.centers = new int[]{
                startBoundaries.left + ((startBoundaries.right - startBoundaries.left) / 2),
                Math.abs(startBoundaries.top + ((startBoundaries.bottom - startBoundaries.top) / 2))
        };

        this.path = buildPath();
        this.createdAt = System.currentTimeMillis();
    }

    protected Paint buildPaint(){
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setPathEffect(new CornerPathEffect(10));

        int strokeWidth = (int) Math.max(5, Math.abs((startBoundaries.right - startBoundaries.left)) * .065);

        paint.setStrokeWidth(strokeWidth);

        return paint;
    }

    protected Path buildPath(){
        Path path = new Path();

        if(circlePathOverride){
            radius = (int) (Math.min(startBoundaries.right - startBoundaries.left,
                    Math.abs(startBoundaries.bottom - startBoundaries.top)) / 2);

            path.addCircle(centers[0],
                    centers[1],
                    radius,
                    Path.Direction.CW);
        }
        else{
            path.moveTo(startBoundaries.left, startBoundaries.top);
            path.lineTo(startBoundaries.right, startBoundaries.top);
            path.lineTo(startBoundaries.right, startBoundaries.bottom);
            path.lineTo(startBoundaries.left, startBoundaries.bottom);
            path.lineTo(startBoundaries.left, startBoundaries.top);
        }

        return path;
    }

    public void draw(Canvas canvas){
        canvas.save();

//        Log.d("Pulse", "Centers: " + startBoundaries.centerX() + ", " + startBoundaries.centerY());
        Log.d("Pulse", "Centers: " + centers[0] + ", " + centers[1]);

        canvas.scale(scale,
                scale,
                centers[0],
                centers[1]);

//        if(circlePathOverride)
//            canvas.drawCircle(startBoundaries.centerX(),
//                    startBoundaries.centerY(),
//                    radius,
//                    paint);
//        else canvas.drawRect(startBoundaries, paint);

        canvas.drawPath(path, paint);

        canvas.restore();
    }

    public void update(){
        float percentCompleted = (System.currentTimeMillis() - createdAt) / (float) duration;

        if(alphaInterpolator != null)
            this.paint.setAlpha((int) (MAX_ALPHA - (alphaInterpolator.getInterpolation(percentCompleted) * MAX_ALPHA)));

        if(scaleInterpolator != null)
            this.scale = 1 + ((maxScale - 1) * scaleInterpolator.getInterpolation(percentCompleted));
    }

    public Pulse setPaint(Paint paint) {
        this.paint = paint;
        return this;
    }

    public Pulse setAlphaInterpolator(Interpolator alphaInterpolator) {
        this.alphaInterpolator = alphaInterpolator;
        return this;
    }

    public Pulse setScaleInterpolator(Interpolator scaleInterpolator) {
        this.scaleInterpolator = scaleInterpolator;
        return this;
    }

    public Pulse setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    public Pulse setCirclePathOverride(boolean circlePathOverride) {
        this.circlePathOverride = circlePathOverride;
        return this;
    }

    public Pulse setMaxScale(float maxScale) {
        this.maxScale = maxScale;
        return this;
    }

    public Pulse setColor(int color){
        paint.setColor(color);
        return this;
    }

    public boolean isAlive(){
        return System.currentTimeMillis() - createdAt < duration;
    }

}
