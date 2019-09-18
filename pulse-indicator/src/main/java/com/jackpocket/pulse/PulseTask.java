package com.jackpocket.pulse;

public class PulseTask extends Thread {

    private static final int SLEEP = 15;

    private PulseController controller;
    private boolean canceled = false;

    private Runnable finishedListener;

    public PulseTask(PulseController controller){
        this.controller = controller;
    }

    public PulseTask setFinishedListener(Runnable finishedListener) {
        this.finishedListener = finishedListener;

        return this;
    }

    @Override
    public void run(){
        try {
            while(!canceled && controller.isRunning()) {
                controller.getParent()
                    .post(new Runnable(){
                        public void run(){
                            controller.update();
                        }
                    });

                Thread.sleep(SLEEP);
            }
        }
        catch(Exception e) { e.printStackTrace(); }

        controller.getParent()
                .post(new Runnable() {
                    public void run() {
                        if(!(canceled || finishedListener == null))
                            finishedListener.run();

                        controller = null;
                    }
                });
    }

    public void cancel(){
        this.canceled = true;
    }
}
