package com.jackpocket.pulse;

import android.os.Handler;
import android.os.Looper;

public class PulseTask extends Thread {

    private static final int SLEEP = 15;

    private PulseController controller;
    private boolean canceled = false;

    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private Runnable finishedListener;

    public PulseTask(PulseController controller) {
        this.controller = controller;
    }

    public PulseTask setFinishedListener(Runnable finishedListener) {
        this.finishedListener = finishedListener;

        return this;
    }

    @Override
    public void run() {
        try {
            final Runnable updateRunnable = new Runnable() {
                @Override
                public void run(){
                    controller.update();
                }
            };

            while (!canceled && controller.isRunning()) {
                mainThreadHandler.post(updateRunnable);

                Thread.sleep(SLEEP);
            }
        }
        catch (Exception e) { e.printStackTrace(); }

        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!(canceled || finishedListener == null)) {
                    finishedListener.run();
                }

                controller = null;
                finishedListener = null;
            }
        });
    }

    public void cancel() {
        this.canceled = true;
    }
}
