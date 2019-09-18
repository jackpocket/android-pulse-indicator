# pulse-indicator

[![Download](https://api.bintray.com/packages/jackpocket/maven/pulse-indicator/images/download.svg) ](https://bintray.com/jackpocket/maven/pulse-indicator/_latestVersion)

An Android system for indicating Views with fading pulses

![pulse-indicator Sample](https://github.com/jackpocket/android-pulse-indicator/raw/master/pulse.gif)

# Installation

```
    repositories {
        jcenter()
    }

    dependencies {
        compile('com.jackpocket:pulse-indicator:1.1.0')
    }
```

# Usage

##### Layout Approach

Make the root of your `Activity`'s layout one of the base `PulseLayouts` included in this library: the `PulsingLinearLayout` or the `PulsingRelativeLayout`. e.g.

```xml
<com.jackpocket.pulse.layouts.PulsingLinearLayout 
    ...
    >
``` 

Then find it in your Activity and simply call `PulseLayout.attachTo(Activity, View)`. Done. e.g.

```java
((PulseLayout) findViewById(R.id.my_pulsing_layout))
    .attachTo(this, findViewById(R.id.some_view_I_want_to_indicate);
```

##### PulseView Approach

Just add a `PulseView` to your layout (make sure you're using a ViewGroup that allows overlapping children (e.g. RelativeLayout)).

```xml
<RelativeLayout>
    <com.jackpocket.pulse.layouts.PulseView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        ... />
</RelativeLayout>
``` 

You could then use it the same way you would going the Layout Approach mentioned above.

##### Custom Approach

If you want to add pulsing to your own custom layouts, just checkout one of the supplied layout class files for detailed information on how to implemented the `PulsingController` manually.

Changing the values at runtime can also be configured by working with the PulseController:

```java
((PulseLayout) findViewById(R.id.my_pulsing_layout))
        .getPulseController()
        .setCirclePathOverride(false) // Set it to use the rectangular boundaries instead of circle pulsing
        .setPulsingColor(0xFF22FF22) // Set the pulse starting color
        .setPulsingStrokeWidth(10) // Override the dynamic stroke width with a custom one
        .setDurationMs(1500) // Set the overall duration of the pulsing (will continue until no pulses exist)
        .setPulseLifeSpanMs(900) // The length of time a pulse is visible
        .setRespawnRateMs(300) // The rate at which a new pulse should be added
        .setAlphaInterpolator(new AccelerateInterpolator()) // Set the Interpolator for the alpha animation
        .setScaleInterpolator(new LinearInterpolator()) // Set the Interpolator for the scaling animation
        .setFinishedListener(view -> doSomethingOnFinished()) // Set a callback to be triggered when the pulsing finished for a View. Calling attach() before it completes will prevent it from being triggered
        .attachTo(this, findViewById(R.id.some_view_I_want_to_indicate)); // Attach the configured controller to the target and start pulsing
```

### Configs

The default configs for pulsing color, duration, individual lifespan, respawn rates can be overwritten via the following, respectively:

    R.color.pulse__color
    R.integer.pulse__duration_default
    R.integer.pulse__lifespan_default
    R.integer.pulse__respawn_rate_default

Note, all time values are in milliseconds.



