package nachos.kernel.threads;

import java.util.ArrayList;
import java.util.List;

import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.NachosThread;
import nachos.util.LevelQueue;
import nachos.util.Queue;

public class MultilevelQueueOperator {
    
    private final double p = 0.4;
    
    private double prevPrediction;
    
    private double nextPrediction; // May not be useful
    
    private double currentBurst;
    
    private NachosThread nextThread;
    
    List<LevelQueue<NachosThread>> multilevelQueue;
    
    public MultilevelQueueOperator(int numQueues, int baseQuantum) {
	prevPrediction = 0.0; // Default value for initial estimate
	
	multilevelQueue = new ArrayList<LevelQueue<NachosThread>>(numQueues);
	
	for (int i = 0; i < numQueues; i++) { // Double quantum every another queue
	    if(i == 0) {
		LevelQueue<NachosThread> temQueue = new LevelQueue<NachosThread>();
		temQueue.setQuantum(baseQuantum);
		multilevelQueue.add(temQueue);
	    } else {
		LevelQueue<NachosThread> temQueue = new LevelQueue<NachosThread>();
		temQueue.setQuantum(2*multilevelQueue.get(i-1).getQuantum());
		multilevelQueue.add(temQueue);
	    }
	}
	
	for (int i = 0; i < numQueues; i++) {
	    Debug.println('+', "Queue: "+ i + "quantum: "+multilevelQueue.get(i).getQuantum());
	}
    }
    
    public double calculateAvg() {
	double result;
	
	result = p*getCurrentBurst() + (1-p)*getPrevPrediction();
	
	//setPrevPrediction(result); // Update previous estimate immediately
	
	//Debug.println('+', "Only calculate: "+ result);
	
	return result;
    }
    
    // Offer thread into one specific queue
    public void pickPriorityQueueAndInsert(SubNachosThread thread) {
	
	// Round-Robin queue
	if(multilevelQueue.size() == 1) {
	    multilevelQueue.get(0).offer(thread);
		thread.setQueueIndex(0);
		thread.resetRemainingTicks(multilevelQueue.get(0).getQuantum()); // Reset remaining ticks
		//Debug.println('+', "Now thread: "+thread.name+", has been placed in queue with quantum:  "+multilevelQueue.get(0).getQuantum());
		return;
	}
	
	//Starting below is for multilevel queue
	double tem = calculateAvg();
	
	//Debug.println('+', "Next estimate(prediction) calculated for "+thread.name+" : "+tem);

	for(int i = 0; i < multilevelQueue.size(); i++) {
	    if(tem < multilevelQueue.get(i).getQuantum()) {
		multilevelQueue.get(i).offer(thread);
		thread.setQueueIndex(i);
		thread.resetRemainingTicks(multilevelQueue.get(i).getQuantum()); // Reset remaining ticks
		//Debug.println('+', "Now thread: "+thread.name+", has been placed in queue with quantum:  "+multilevelQueue.get(i).getQuantum());
		break;
	    }
	}
    }
    
    // Poll thread from one specific queue
    public NachosThread searchNextThread() {
	for(int i = 0; i < multilevelQueue.size();i++) { // Do you understand?
	    if(!multilevelQueue.get(i).isEmpty()) {
		//Debug.println('+', "search next thread: "+ multilevelQueue.get(i).peek().name);
		setNextThread(multilevelQueue.get(i).poll());
		return getNextThread();
	    }
	}
	return null;
    }
    
    // Check if the entire multilevelQueue is empty
    public boolean isEmpty() {
	for(int i = 0; i < multilevelQueue.size();i++) { // Do you understand?
	    if(!multilevelQueue.get(i).isEmpty()) {
		return true;
	    }
	}
	return false;
    }
    
    // Not my top choice to use
    public void insertQueue(SubNachosThread thread, double prevEstimate) {
	for(int i = 0; i < multilevelQueue.size(); i++) {
	    if(prevEstimate < multilevelQueue.get(i).getQuantum()) {
		multilevelQueue.get(i).offer(thread);
		thread.setQueueIndex(i);
		thread.resetRemainingTicks(multilevelQueue.get(i).getQuantum()); // Reset remaining ticks
		break;
	    }
	}
    }
    
    /*The following are setters and getters*/
    
    public void setPrevPrediction(double num) {
	prevPrediction = num;
    }
    
    public double getPrevPrediction() {
	return prevPrediction;
    }
    
//    public void setNextPrediction(double num) {
//	nextPrediction = num;
//    }
//    
//    public double getNextPrediction() {
//	return nextPrediction;
//    }
    
    public void setCurrentBurst(double num) {
	currentBurst = num;
    }
    
    public double getCurrentBurst() {
	return currentBurst;
    }
    
    public void setNextThread(NachosThread thread) {
	nextThread = thread;
    }
    
    public NachosThread getNextThread() {
	return nextThread;
    }
}
