package com.jackpocket.pulse.layouts;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.jackpocket.pulse.PulseController;

public class PulsingView extends View implements PulseLayout {

    private PulseController pulseController = new PulseController(this);

    public PulsingView(Context context) {
        super(context);
        setWillNotDraw(false);
    }

    public PulsingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public PulsingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        pulseController.draw(canvas);
    }

    @Override
    public PulseController attachTo(Activity activity, View view){
        return pulseController.attachTo(activity, view);
    }

    @Override
    public PulseController getPulseController(){
        return pulseController;
    }

}
