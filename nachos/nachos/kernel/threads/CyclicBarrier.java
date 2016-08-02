package nachos.kernel.threads;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.NachosThread;

/**
 * A <CODE>CyclicBarrier</CODE> is an object that allows a set of threads to
 * all wait for each other to reach a common barrier point.
 * To find out more, read
 * <A HREF="http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CyclicBarrier.html">the documentation</A>
 * for the Java API class <CODE>java.util.concurrent.CyclicBarrier</CODE>.
 *
 * The class is provided in skeletal form.  It is your job to
 * complete the implementation of this class in conformance with the
 * Javadoc specifications provided for each method.  The method
 * signatures specified below must not be changed.  However, the
 * implementation will likely require additional private methods as well
 * as (private) instance variables.  Also, it is essential to provide
 * proper synchronization for any data that can be accessed concurrently.
 * You may use ONLY semaphores for this purpose.  You may NOT disable
 * interrupts or use locks or spinlocks.
 *
 * NOTE: The skeleton below reflects some simplifications over the
 * version of this class in the Java API.
 */
public class CyclicBarrier {
    
    private static class Generation {
        boolean broken = false;
    }
    
    /** The semaphore for guarding barrier entry */
    private Semaphore sem1 = new Semaphore("sem1",1);
    /** The semaphore counting amount of waiting threads until tripped */
    private Semaphore sem2 = new Semaphore("sem2",0);
    /** The number of parties */
    private final int parties;
    /* The command to run when tripped */
    private final Runnable barrierCommand;
    /** The current generation */
    private Generation generation = new Generation();
    
    /**
     * Number of parties still waiting. Counts down from parties to 0
     * on each generation.  It is reset to parties on each new
     * generation or when broken.
     */
    private int count;
    
    /**
     * Updates state on barrier trip and wakes up everyone.
     * Called only while holding lock.
     */
    private void nextGeneration() {
        for (int i = 0; i < parties; i++) {
            sem2.V();
        }
        
        // set up next generation
        count = parties;
        generation = new Generation();
    }
    
    /**
     * Sets current barrier generation as broken and wakes up everyone.
     * Called only while holding lock.
     */
    private void breakBarrier() {
        generation.broken = true;
        count = parties;

        for (int i = 0; i < count; i++) {
            sem2.V();
        }
    }
    
    /**
     * Main barrier code, covering the various handling strategies.
     * @throws BrokenBarrierException 
     */
    private int dowait() throws BrokenBarrierException{
	        Semaphore sem1 = this.sem1;
	        sem1.P(); // Lock this critical session
	        	        
	        final Generation g = generation;
	        int index;
	        
	        try {
	            if (g.broken)
	        	throw new BrokenBarrierException();

	            index = --count;

	            if (index == 0) {  // tripped
	                boolean ranAction = false;
	                try {
	                    final Runnable command = barrierCommand;
	                    if (command != null)
	                        command.run();
	                    ranAction = true;
	                    nextGeneration();
	                    return 0;
	                } finally {
	                    if (!ranAction)
	                        breakBarrier();
	                }
	            }
	        } finally {
	            sem1.V(); // Unlock this critical session
	        }

	        // loop until tripped, broken, interrupted, or timed out
	        for (;;) {
	            sem2.P();
	            
	            if (g.broken)
	        	throw new BrokenBarrierException();
	            
	            if (g != generation)
	                return index;
	        }
    }
    
    /** Class of exceptions thrown in case of a broken barrier. */
    public static class BrokenBarrierException extends Exception {}

   /**
     * Creates a new CyclicBarrier that will trip when the given number
     * of parties (threads) are waiting upon it, and does not perform a
     * predefined action when the barrier is tripped.
     *
     * @param parties  The number of parties.
     */
    public CyclicBarrier(int parties) {
	this(parties, null);
    }
    
    /**
     * Creates a new CyclicBarrier that will trip when the given number of
     * parties (threads) are waiting upon it, and which will execute the
     * given barrier action when the barrier is tripped, performed by the
     * last thread entering the barrier.
     *
     * @param parties  The number of parties.
     * @param barrierAction  An action to be executed when the barrier
     * is tripped, performed by the last thread entering the barrier.
     */
    public CyclicBarrier(int parties, Runnable barrierAction) {
	if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }

    /**
     * Waits until all parties have invoked await on this barrier.
     * If the current thread is not the last to arrive then it blocks
     * until either the last thread arrives or some other thread invokes
     * reset() on this barrier.
     *
     * @return  The arrival index of the current thread, where index
     * getParties() - 1 indicates the first to arrive and zero indicates
     * the last to arrive.
     * @throws  BrokenBarrierException in case this barrier is broken.
     */
    public int await() throws BrokenBarrierException{
	return dowait();
    }

    /**
     * Returns the number of parties currently waiting at the barrier.
     * @return the number of parties currently waiting at the barrier.
     */
    public int getNumberWaiting() {
	Semaphore sem1 = this.sem1;
        sem1.P();
        try {
            return parties - count;
        } finally {
            sem1.V();
        }
    }

    /**
     * Returns the number of parties required to trip this barrier.
     * @return the number of parties required to trip this barrier.
     */
    public int getParties() {
	return parties;
    }

    /**
     * Queries if this barrier is in a broken state.
     * @return true if this barrier was reset while one or more threads
     * were blocked in await(), false otherwise.
     */
    public boolean isBroken() {
	Semaphore sem1 = this.sem1;
        sem1.P();
        try {
            return generation.broken;
        } finally {
            sem1.V();
        }
    }

    /**
     * Resets the barrier to its initial state. 
     */
    public void reset() {
	Semaphore sem1 = this.sem1;
        sem1.P();
        try {
            breakBarrier();   // break the current generation
            //nextGeneration(); // start a new generation
        } finally {
            sem1.V();
        }
    }

    /**
      * This method can be called to simulate "doing work".
      * Each time it is called it gives control to the NACHOS
      * simulator so that the simulated time can advance by a
      * few "ticks".
      */
    public static void allowTimeToPass() {
	dummy.P();
	dummy.V();
	Nachos.scheduler.yieldThread();
    }

    /** Semaphore used by allowTimeToPass above. */
    private static Semaphore dummy = new Semaphore("Time waster", 1);

    /**
     * Run a demonstration of the CyclicBarrier facility.
     * @param args  Arguments from the "command line" that can be
     * used to modify features of the demonstration such as the
     * number of parties, the amount of "work" performed by
     * each thread, etc.
     *
     * IMPORTANT: Be sure to test your demo with the "-rs xxxxx"
     * command-line option passed to NACHOS (the xxxxx should be
     * replaced by an integer to be used as the seed for
     * NACHOS' pseudorandom number generator).  If you fail to
     * include this option, then a thread that has been started will
     * always run to completion unless it explicitly yields the CPU.
     * This will result in the same (very uninteresting) execution
     * each time NACHOS is run, which will not be a very good
     * test of your code.
     */
    public static void demo(String[] args) {
	// Very simple example of the intended use of the CyclicBarrier
	// facility: you should replace this code with something much
	// more interesting.
	final CyclicBarrier barrier = new CyclicBarrier(5, new Runnable(){
          @Override
          public void run(){
              //This task will be executed once all thread reaches barrier
              Debug.println('+', "This is the command running which is defined in CyclicBarrier constructor, and is executed firstly once barrier is tripped");
          }
	});
	Debug.println('+', "Demo1 starting");
	
	for(int i = 0; i < 5; i++) {
	    NachosThread thread = new NachosThread ("Worker thread " + i, new Runnable() {
		    public void run() {
			Debug.println('1', "Thread " + NachosThread.currentThread().name + " is starting");
			for(int j = 0; j < 3; j++) {
			    Debug.println('1', "Thread " + NachosThread.currentThread().name + " beginning phase " + j);
			    for(int k = 0; k < 5; k++) {
				Debug.println('1', "Thread " + NachosThread.currentThread().name + " is working");
				CyclicBarrier.allowTimeToPass();  // Do "work".
			    }
			    try {
				Debug.println('+', "Amount of waiting threads at the barrier: " + barrier.getNumberWaiting());
				Debug.println('1', "Thread " + NachosThread.currentThread().name + " is waiting at the barrier");
				barrier.await();
				Debug.println('1', "Thread " + NachosThread.currentThread().name + " has finished phase " + j);
			    } catch (BrokenBarrierException e) {
				
			    }
			}
			Debug.println('+', "Thread " + NachosThread.currentThread().name + " is terminating");
			Nachos.scheduler.finishThread();
		    }
		});
	    Nachos.scheduler.readyToRun(thread);
	}
	Debug.println('+', "Demo1 terminating");
	
	
	for(int j = 0; j < 150; j++) {
		allowTimeToPass();   // Gap between 2 demo.
	}
	
	Debug.println('+', "");
	Debug.println('+', "********************************Demo1 ends, Demo2 starts********************************");
	Debug.println('+', "");
	
	/** 
	 * The following demo is used to show functionality of reset() and isBroken().
	 */
	final CyclicBarrier barrier2 = new CyclicBarrier(3);
	Debug.println('+', "Demo2 starting");
	
	for (int i = 0; i < 3; i++) {
	    NachosThread thread = new NachosThread ("Worker thread " + i, new Runnable() {
		 public void run() {
		     Debug.println('1', "Thread " + NachosThread.currentThread().name + " is starting");
		     for(int k = 0; k < 5; k++) {
			Debug.println('1', "Thread " + NachosThread.currentThread().name + " is working");
			CyclicBarrier.allowTimeToPass();  // Do "work".
		     }
		     try {
			Debug.println('+', "Amount of waiting threads before invoking await(): " + barrier2.getNumberWaiting());
			if (barrier2.getNumberWaiting() == 2) { // Last thread runs into this if statement
			    Debug.println('+', "Implementing reset()");
			    barrier2.reset(); // This barrier is broken and is not useful any more
			}
			Debug.println('+', "Is the current barrier broken?: " + barrier2.isBroken());
			barrier2.await();
			Debug.println('1', "Thread " + NachosThread.currentThread().name + " has crossed barrier ");
		     } catch (BrokenBarrierException e) {
			 Debug.println('+', "Exception occurs due to invoking reset(), the barrier is now broken and is not useful any more");
		     }
		     Debug.println('+', "Thread " + NachosThread.currentThread().name + " is terminating");
		     Nachos.scheduler.finishThread();
		 }
	    });
	    Nachos.scheduler.readyToRun(thread);
	}
	Debug.println('+', "Demo2 terminating");
    }
}