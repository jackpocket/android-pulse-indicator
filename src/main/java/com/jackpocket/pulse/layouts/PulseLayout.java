package com.jackpocket.pulse.layouts;

import android.app.Activity;
import android.view.View;

import com.jackpocket.pulse.PulseController;

public interface PulseLayout {

    public PulseController attachTo(Activity activity, View view);

    public PulseController getPulseController();

}
