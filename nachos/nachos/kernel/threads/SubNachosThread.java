package nachos.kernel.threads;

import nachos.machine.NachosThread;

public class SubNachosThread extends NachosThread{
    
    private boolean exitFlag = false;
    
    private boolean alreadyLoadedFlag = false; 

    private int remainingTicks;
    
    private int queueIndex;
    
    private int sleepTicks;
    
    private double prevEstimate;
    
    private boolean wakeupFlag;
    
    private Semaphore sleepSem = new Semaphore("sleepSem", 0);
    
    public SubNachosThread(String name, Runnable runObj) {
	super(name, runObj);
	remainingTicks = 100;
	prevEstimate = 0.0;
	wakeupFlag = false;
    }
    
    public boolean isRemainingTicksZero() {
	return (remainingTicks==0);
    }
    
    public int getRemainingTicks() {
	return remainingTicks;
    }
    
    public void decrementRemainingTicks() {
	remainingTicks = remainingTicks - 100;
    }
    
    public void resetRemainingTicks(int num) {
	remainingTicks = num;
    }
    
    public void resetRemainingTicks() {
	remainingTicks = 800;
    }
    
    public void setQueueIndex(int num) {
	queueIndex = num;
    }
    
    public int getQueueIndex() {
	return queueIndex;
    }
    
    public void setExitFlag() {
	exitFlag = true;
    }
    
    public boolean getExitFlag() {
	return exitFlag;
    }
    
    public void setAlreadyLoadedFlag() {
	alreadyLoadedFlag = true;
    }
    
    public boolean getAlreadyLoadedFlag() {
	return alreadyLoadedFlag;
    }
    
    public void setSleepTicks(int num) {
	sleepTicks = num;
    }
    
    public int getSleepTicks() {
	return sleepTicks;
    }
    
    public void decrementSleepTicks() {
	sleepTicks = sleepTicks - 100;
    }
    
    public boolean isSleepTicksBelowZero() {
	if (sleepTicks <= 0) {
	    return true;
	} else {
	    return false;
	}
    }
    
    public Semaphore getSleepSem() {
	return sleepSem;
    }
    
    public void setPrevEstimate(double num) {
	prevEstimate = num;
    }
    
    public double getPrevEstimate() {
	return prevEstimate;
    }
    
    public void setWakeupFlag() {
	wakeupFlag = true;
    }
    
    public void resetWakeupFlag() {
	wakeupFlag = false;
    }
    
    public boolean getWakeupFlag() {
	return wakeupFlag;
    }
}
