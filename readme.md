# pulse-indicator

[![Download](https://api.bintray.com/packages/jackpocket/maven/pulse-indicator/images/download.svg) ](https://bintray.com/jackpocket/maven/pulse-indicator/_latestVersion)

An Android View system for indicating Views using fading pulses

![pulse-indicator Sample](https://github.com/jackpocket/android-pulse-indicator/raw/master/pulse.gif)

# Installation

```
    repositories {
        jcenter()
    }

    dependencies {
        compile('com.jackpocket:pulse-indicator:1.0.2')
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
        ... />
</RelativeLayout>
``` 

You could then use it the same way you would going the Layout Approach mentioned above.

##### Custom Approach

If you want to add pulsing to your own custom layouts, just checkout one of the supplied layout class files for detailed information on how to implemented the `PulsingController` manually.

### Configs

The default configs for pulsing color, duration, individual lifespan, respawn rates can be overwritten via the following, respectively:

    R.color.pulse__color
    R.integer.pulse__duration_default
    R.integer.pulse__lifespan_default
    R.integer.pulse__respawn_rate_default

Note, all time values are in milliseconds.



