package nachos.kernel.threads;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.NachosThread;
import nachos.util.FIFOQueue;

/**
 * This class provides a facility for scheduling work to be performed
 * "in the background" by "child" threads and safely communicating the
 * results back to a "parent" thread.  It is loosely modeled after the
 * AsyncTask facility provided in the Android API.
 *
 * The class is provided in skeletal form.  It is your job to
 * complete the implementation of this class in conformance with the
 * Javadoc specifications provided for each method.  The method
 * signatures specified below must not be changed.  However, the
 * implementation will likely require additional private methods as well
 * as (private) instance variables.  Also, it is essential to provide
 * proper synchronization for any data that can be accessed concurrently.
 * You may use any combination of semaphores, locks, and conditions
 * for this purpose.
 *
 * NOTE: You may NOT disable interrupts or use spinlocks.
 */
public class TaskManager {
    
    private class RequestQueue {
	private FIFOQueue<Runnable> buffer;
	private Semaphore bufferLock = new Semaphore("bufferLock", 1);
	
	public RequestQueue () {
	    buffer = new FIFOQueue<>();
	}
	
	public void add(Runnable runnable) {
	    bufferLock.P();
	    buffer.add(runnable);
	    bufferLock.V();
	}
	
	public Runnable poll() {
	    Runnable runnable;
	    bufferLock.P();
	    runnable = buffer.remove();
	    bufferLock.V();
	    return(runnable);
	}
	
	public boolean isEmpty() {
	    boolean isEmpty = true;
	    bufferLock.P();
	    isEmpty = buffer.isEmpty();
	    bufferLock.V();
	    return isEmpty;
	}
    }
    
    private class ChildThreadCounter {
	private int counter;
	Semaphore counterLock = new Semaphore("counterLock", 1);
	
	public ChildThreadCounter () {
	    counter = 0;
	}
	
	public void increment () {
	    counterLock.P();
	    ++counter;
	    counterLock.V();
	}
	
	public void decrement () {
	    counterLock.P();
	    --counter;
	    counterLock.V();
	}
	
	public boolean isZero() {
	    boolean isZero = true;
	    counterLock.P();
	    if (counter > 0)
		isZero = false;
	    counterLock.V();
	    return isZero;
	}
    }
    
    /* Data field*/
    
    private Lock processRequestLock = new Lock("processRequestLock");
    private Condition processRequestCondition = new Condition("processRequestCondition", processRequestLock);
    //private Semaphore processRequestSem = new Semaphore("processRequestSem", 0);
    private RequestQueue requestQueue;
    ChildThreadCounter childThreadCounter;
    
    /**
     * Initialize a new TaskManager object, and register the
     * calling thread as the "parent" thread.  The parent thread is
     * responsible (after creating at least one Task object and
     * calling its execute() method) for calling processRequests() to
     * track the completion of "child" threads and process onCompletion()
     * or onCancellation() requests on their behalf.
     */
    public TaskManager() {
	requestQueue = new RequestQueue();
	childThreadCounter = new ChildThreadCounter();
    }
    
    /**
     * Posts a request for a Runnable to be executed by the parent thread.
     * Such a Runnable might consist of a call to <CODE>onCompletion()</CODE>
     * or <CODE>onCancellation() associated with the completion of a task
     * being performed by a child thread, or it might consist of
     * completely unrelated work (such as responding to user interface
     * events) for the parent thread to perform.
     * 
     * NOTE: This method should be safely callable by any thread.
     *
     * @param runnable  Runnable to be executed by the parent thread.
     */
    public void postRequest(Runnable runnable) {
	requestQueue.add(runnable);
	
	processRequestLock.acquire();
	processRequestCondition.signal(); // I just want to wake up the parent thread, if waiting
	processRequestLock.release();
    }

    /**
     * Called by the parent thread to process work requests posted
     * for it.  This method does not return unless there are no
     * further pending requests to be processed AND all child threads
     * have terminated.  If there are no requests to be processed,
     * but some child threads are still active, then the parent thread
     * will block within this method awaiting further requests
     * (for example, those that will eventually result from the termination
     * of the currently active child threads).
     *
     * @throws IllegalStateException  if the calling thread is not
     * registered as the parent thread for this TaskManager.
     */
    public void processRequests() throws IllegalStateException {
	
	Lock processRequestLock = this.processRequestLock;
	processRequestLock.acquire();
	
	try {
	    
	    for (;;) { // Infinite loop until certain situations met to break
		if (!requestQueue.isEmpty()) {
		    Debug.println('+', "running 1st checking"); // Message for u to see in console
		    Runnable instance = requestQueue.poll();
		    instance.run();
		} else if (requestQueue.isEmpty() && !childThreadCounter.isZero()) {
		    Debug.println('+', "running 2nd checking"); // Message for u to see in console
		    processRequestCondition.await();
		} else if (requestQueue.isEmpty() && childThreadCounter.isZero()) {
		    Debug.println('+', "running 3rd checking"); // Message for u to see in console
		    break;
		}
	    }
	    
	} finally {
	    processRequestLock.release();
	}
	
    }

    /**
     * Inner class representing a task to be executed in the background
     * by a child thread.  This class must be subclassed in order to
     * override the doInBackground() method and possibly also the
     * onCompletion() and onCancellation() methods.
     */
    public class Task {
	
	private class CancelFlag {
	    private boolean cancelFlag;
	    Semaphore cancelFlagLock = new Semaphore("cancelFlagLock", 1);
	    
	    public CancelFlag () {
		cancelFlag = false;
	    }
	    
	    public void action () {
		cancelFlagLock.P();
		cancelFlag = true;
		cancelFlagLock.V();
	    }
	    
	    public boolean check() {
		boolean temFlag;
		cancelFlagLock.P();
		temFlag = cancelFlag;
		cancelFlagLock.V();
		return temFlag;
	    }
	}
	
	private class IsCancelledFlag {
	    private boolean isCancelledFlag;
	    Semaphore isCancelledFlagLock = new Semaphore("isCancelledFlagLock", 1);
	    
	    public IsCancelledFlag () {
		isCancelledFlag = false;
	    }
	    
	    public void action () {
		isCancelledFlagLock.P();
		isCancelledFlag = true;
		isCancelledFlagLock.V();
	    }
	    
	    public boolean check() {
		boolean temFlag;
		isCancelledFlagLock.P();
		temFlag = isCancelledFlag;
		isCancelledFlagLock.V();
		return temFlag;
	    }
	}
	
	private class FinishFlag {
	    private boolean finishFlag;
	    Semaphore finishFlagLock = new Semaphore("finishFlagLock", 1);
	    
	    public FinishFlag () {
		finishFlag = false;
	    }
	    
	    public void action () {
		finishFlagLock.P();
		finishFlag = true;
		finishFlagLock.V();
	    }
	    
	    public boolean check() {
		boolean temFlag;
		finishFlagLock.P();
		temFlag = finishFlag;
		finishFlagLock.V();
		return temFlag;
	    }
	}
	
	/* Data field*/
	
        private CancelFlag cancelFlag = new CancelFlag(); // Create the only instance of cancelFlag
        private FinishFlag finishFlag= new FinishFlag();
        private IsCancelledFlag isCancelledFlag = new IsCancelledFlag();
	
	/**
	 * Cause the current task to be executed by a new child thread.
	 * In more detail, a new child thread is created, the child
	 * thread runs the doInBackground() method and upon termination
	 * of that method a request is posted for the parent thread to
	 * run either onCancellation() or onCompletion(), respectively,
	 * depending on	whether or not the task was cancelled.
	 */
	public void execute() {
	    childThreadCounter.increment();
	    
	    NachosThread thread = new NachosThread("newChildThread",new Runnable() {
		@Override
	        public void run() {
		    
		    doInBackground(); // Must be called and return as the 1st job.
		    
		    finishFlag.action(); // Cancel() will return true if invoked before executing this statement
		    
		    if (!cancelFlag.check()) {
			postRequest(new Runnable(){ // Post onCompletion() runnable to queue
			    public void run () {
				onCompletion();
			    }
			});
		    } else {
			postRequest(new Runnable(){ // Post onCancellation() runnable to queue
			    public void run () {
				onCancellation();
			    }
			});
		    }
		    
		    childThreadCounter.decrement();
		    
		    Nachos.scheduler.finishThread(); // task terminates
	        }
	    });
	    Nachos.scheduler.readyToRun(thread);
	}

	/**
	 * Flag the current Task as "cancelled", if the task has not
	 * already completed.  Successful cancellation (as indicated
	 * by a return value of true) guarantees that the onCancellation()
	 * method will be executed instead of the normal onCompletion()
	 * method.  This method should be safely callable by any thread.
	 *
	 * @return true if the task was successfully cancelled,
	 * otherwise false.
	 */
	public boolean cancel() {
	    
	    cancelFlag.action();
	    
	    if (finishFlag.check()) { // Successful cancellation depends on timing of finishFlag 
		return false; 
	    } else {
		isCancelledFlag.action(); // Successful cancellation is recorded in isCancellationFlag
		return true;
	    }
	}

	/**
	 * Determine whether this Task has been cancelled.
	 * This method should be safely callable by any thread.
	 *
	 * @return true if this Task has been cancelled, false otherwise.
	 */
	public boolean isCancelled() {
	    return isCancelledFlag.check();
	}

	/**
	 * Method to be executed in the background by a child thread.
	 * Subclasses will override this with desired code.  The default
	 * implementation is to do nothing and terminate immediately.
	 * Subclass implementations should call isCancelled() periodically
	 * so that this method will return promptly if this Task is
	 * cancelled.  This method should not be called directly;
	 * rather, it will be called indirectly as a result of a call to
	 * the execute() method.
	 */
	protected void doInBackground() {
	    // This method will be overriden by user decision.
	}

	/**
	 * Method to be executed by the main thread upon termination of
	 * of doInBackground().  Will not be executed if the task was
	 * cancelled.  This method should not be called directly; 
	 * rather, it will be called indirectly as a result of a call to
	 * the execute() method.
	 */
	protected void onCompletion() {
	    // This method will be overriden by user decision.
	    
	}

	/**
	 * Method to be executed by the main thread upon termination
	 * of doInBackground().  Will only be executed if the task
	 * was cancelled.
	 */
	protected void onCancellation() {
	    // This method will be overriden by user decision.
	}
	
	/**
	 * This method can be called to simulate "doing work".
	 * Each time it is called it gives control to the NACHOS
	 * simulator so that the simulated time can advance by a
	 * few "ticks".
	 */
	protected void allowTimeToPass() {
	    dummy.P();
	    dummy.V();
	}

    }

    /** Semaphore used by allowTimeToPass above. */
    private static Semaphore dummy = new Semaphore("Time waster", 1);

    /**
     *  Run a demonstration of the TaskManager facility.
     *
     * @param args  Arguments from the "command line" that can be
     * used to modify features of the demonstration such as the
     * number of tasks to execute, the amount of "work" performed by
     * each task, etc.
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
	// Very simple example of the intended use of the TaskManager
	// facility: you should replace this code with something much
	// more interesting.
	TaskManager mgr = new TaskManager();
	for(int i = 0; i < 5; i++) {
	    final int tn = i;
	    Task task = mgr.new Task() {
		protected void doInBackground() {
		    Debug.println('+', "Thread " + NachosThread.currentThread().name + " is starting task " + tn);
		    for(int j = 0; j < 10; j++) {
			allowTimeToPass();   // Do "work".
			Debug.println('+', "Thread " + NachosThread.currentThread().name + " is working on task " + tn);
		    }
		    Debug.println('+', "Thread " + NachosThread.currentThread().name + " is finishing task " + tn);
		}

		protected void onCompletion() {
		    Debug.println('+', "Thread " + NachosThread.currentThread().name + " is executing onCompletion() " + " for task " + tn);
		}
		
		protected void onCancellation() {
		    Debug.println('+', "Thread " + NachosThread.currentThread().name + " is executing onCancellation() " + " for task " + tn);
		}
	    };
	    task.execute();
	    
	    for(int j = 0; j < 25; j++) {
		allowTimeToPass();   // Allow some time for parent thread fail to cancel one or two child threads
	    }
	    
	    if (i % 2 == 0)
		Debug.println('+', "Currently trying to cancel thread taks "+i+"? And return "+task.cancel()); // My cancel
	    
	    Debug.println('+', "Thread "+i+" is actually cancelled? "+task.isCancelled()); // Check if cancelled
	}
	
	mgr.processRequests(); // Will not return until no requests AND no childThreads available
	Debug.println('+', "Demo terminating");
    }
    
    static void allowTimeToPass() {
	    dummy.P();
	    dummy.V();
	}
}
