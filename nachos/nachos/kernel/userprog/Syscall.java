// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.List;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.kernel.devices.ConsoleDriver;
import nachos.kernel.threads.Scheduler;
import nachos.kernel.threads.SpinLock;
import nachos.kernel.userprog.test.ProgTest;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.NachosThread;
import nachos.machine.Simulation;

/**
 * Nachos system call interface.  These are Nachos kernel operations
 * 	that can be invoked from user programs, by trapping to the kernel
 *	via the "syscall" instruction.
 *
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class Syscall {

    // System call codes -- used by the stubs to tell the kernel 
    // which system call is being asked for.

    /** Integer code identifying the "Halt" system call. */
    public static final byte SC_Halt = 0;

    /** Integer code identifying the "Exit" system call. */
    public static final byte SC_Exit = 1;

    /** Integer code identifying the "Exec" system call. */
    public static final byte SC_Exec = 2;

    /** Integer code identifying the "Join" system call. */
    public static final byte SC_Join = 3;

    /** Integer code identifying the "Create" system call. */
    public static final byte SC_Create = 4;

    /** Integer code identifying the "Open" system call. */
    public static final byte SC_Open = 5;

    /** Integer code identifying the "Read" system call. */
    public static final byte SC_Read = 6;

    /** Integer code identifying the "Write" system call. */
    public static final byte SC_Write = 7;

    /** Integer code identifying the "Close" system call. */
    public static final byte SC_Close = 8;

    /** Integer code identifying the "Fork" system call. */
    public static final byte SC_Fork = 9;

    /** Integer code identifying the "Yield" system call. */
    public static final byte SC_Yield = 10;

    /** Integer code identifying the "Remove" system call. */
    public static final byte SC_Remove = 11;
    
    /** Integer code identifying the "Sleep" system call. */
    public static final byte SC_Sleep = 12;

    /** Integer code identifying the "Sleep" system call. */
    public static final byte SC_Mmap = 15;

    /**
     * Stop Nachos, and print out performance stats.
     */
    public static void halt() {
	Debug.print('+', "Inside halt().\n");
	Debug.print('+', "Shutdown, initiated by user program.\n");
	Simulation.stop();
    }

    /* Address space control operations: Exit, Exec, and Join */

    /**
     * This user program is done.
     *
     * @param status Status code to pass to processes doing a Join().
     * status = 0 means the program exited normally.
     */
    public static void exit(int status) {
	((UserThread)NachosThread.currentThread()).setExitFlag(); // Stop timer interrupt disrupting current thread
	
	Nachos.scheduler.getMultilevelQueueOperator().setCurrentBurst(Nachos.scheduler.getEachBurstCounter()); // Fetch burst actually used
	
	Debug.println('+', "User program exits with status=" + status + ": " + NachosThread.currentThread().name);
	
	AddrSpace space =  ((UserThread)NachosThread.currentThread()).space;
	
	((UserThread)NachosThread.currentThread()).setStauts(status); // Each thread calling Exit(int) will be assigned Exit status
	
       
	/*free the current thread*/
	if(space.getThreadCounter().checkLastThread()) {
	    space.setPageTable(((UserThread)NachosThread.currentThread()).getPageTable());
	    MemoryManager.freePMP(space); // Deallocate physical memory of the address only until last thread
					  // In other threads, should only deallocate stack memory
	    Debug.println('+', "Last thread is here");
	    space.setStauts(status); // Set Exit status for this addrSpace, cuz last thread is about to Exit
	    
	    space.getSemaphore().V(); // I cant call V() directly cuz over one thread in a address will call exit(int)
	} else{
	    int[] PMPArray = space.freeStack(((UserThread)NachosThread.currentThread()).getPageTable());
	    MemoryManager.freeStackPMP(PMPArray);
	}
	
	space.getThreadCounter().decrement();; // Decrement 
	
	// Assign status to the thread, thus this thread has its status assigned value
	
//	for(int i = 0; i<MemoryManager.PMP.length; i++){
//	    if(MemoryManager.PMP[i] == 0){
//		Debug.println('+', "current free physical page: "+ i+"");
//	    }
//	    
//	}
	
	Nachos.scheduler.finishThread(); // Calling this after deallocating memory to that thread
	
    }

    /**
     * Run the executable, stored in the Nachos file "name", and return the 
     * address space identifier.
     *
     * @param name The name of the file to execute.
     */
    public static int exec(String name) {
	name = "test/"+name;
	ProgTest newProTest = new ProgTest(name,0); // Once you create new instance of ProgTest, a new process is made ready to run 
	
	Debug.println('+', "Current SpaceId of calling thread: "+ ((UserThread)NachosThread.currentThread()).space.getSpaceId());
	Debug.println('+', "SpaceId in exec() about to return: "+ newProTest.getSpace().getSpaceId());
	return newProTest.getSpace().getSpaceId(); // Return the index of this address
    }

    /**
     * Wait for the user program specified by "id" to finish, and
     * return its exit status.
     *
     * @param id The "space ID" of the program to wait for.
     * @return the exit status of the specified program.
     */
    public static int join(int id) {
	Debug.println('+', "Space id in join: "+id);
	
	AddrSpace space = SpaceIdManager.AddrSpaceArray.get(id); // Get the space context mapping to id
	
	space.getSemaphore().P(); // Freeze the thread that invokes join()
	
	return space.getStatus(); // Return the status value of the last thread of the address
    }


    /* File system operations: Create, Open, Read, Write, Close
     * These functions are patterned after UNIX -- files represent
     * both files *and* hardware I/O devices.
     *
     * If this assignment is done before doing the file system assignment,
     * note that the Nachos file system has a stub implementation, which
     * will work for the purposes of testing out these routines.
     */

    // When an address space starts up, it has two open files, representing 
    // keyboard input and display output (in UNIX terms, stdin and stdout).
    // Read and write can be used directly on these, without first opening
    // the console device.

    /** OpenFileId used for input from the keyboard. */
    public static final int ConsoleInput = 0;

    /** OpenFileId used for output to the display. */
    public static final int ConsoleOutput = 1;

    /**
     * Create a Nachos file with a specified name.
     *
     * @param name  The name of the file to be created.
     */
    public static void create(String name) { }

    /**
     * Remove a Nachos file.
     *
     * @param name  The name of the file to be removed.
     */
    public static void remove(String name) { }

    /**
     * Open the Nachos file "name", and return an "OpenFileId" that can 
     * be used to read and write to the file.
     *
     * @param name  The name of the file to open.
     * @return  An OpenFileId that uniquely identifies the opened file.
     */
    public static int open(String name) {return 0;}

    /**
     * Write "size" bytes from "buffer" to the open file.
     *
     * @param buffer Location of the data to be written.
     * @param size The number of bytes to write.
     * @param id The OpenFileId of the file to which to write the data.
     */
    public static void write(byte buffer[], int size, int id) {
	if (id == ConsoleOutput) {
	    for(int i = 0; i < size; i++) {
		Nachos.consoleDriver.putChar((char)buffer[i]);
	    }
	}
    }

    /**
     * Read "size" bytes from the open file into "buffer".  
     * Return the number of bytes actually read -- if the open file isn't
     * long enough, or if it is an I/O device, and there aren't enough 
     * characters to read, return whatever is available (for I/O devices, 
     * you should always wait until you can return at least one character).
     *
     * @param buffer Where to put the data read.
     * @param size The number of bytes requested.
     * @param id The OpenFileId of the file from which to read the data.
     * @return The actual number of bytes read.
     */
    public static int read(byte buffer[], int size, int id) {
	
	char ch;
	ArrayList<Character> inputBuffer = (ArrayList<Character>) Nachos.consoleDriver.inputBuffer;
	if (id == ConsoleInput) {
	    for(int i = 0; i < size; i++) {
		
		ch = Nachos.consoleDriver.getChar();
		Nachos.consoleDriver.putChar(ch);
		/*The condition when type back space to the console*/
		
		if(ch == '\b'){
		    if(inputBuffer.size()>0){
			inputBuffer.remove(inputBuffer.size()-1);
			i--;
		    }
		    /*no need to increment*/
		    i--;
		}
		else if((int)ch == 21){
		    inputBuffer.clear();
		    /*no need to increment*/
		    i=0;
		}
		else if((int)ch>=32 && (int)ch<=126){
		    
		    inputBuffer.add(ch);
		    
		}
		else {
		    /*other invalid input do nothing*/
		    i--;
		}
		
		if(ch == '\r' || ch == '\n'){
		    break;
		}
		//buffer[i] = (byte)Nachos.consoleDriver.getChar();
		//Nachos.consoleDriver.putChar((char) buffer[i]);
	    }
	    /*final buffer*/
	    for(int k = 0; k<inputBuffer.size();k++){
		ch = inputBuffer.get(k);
		buffer[k] = (byte)ch;
	    }
	}
	
	return inputBuffer.size();
    }

    /**
     * Close the file, we're done reading and writing to it.
     *
     * @param id  The OpenFileId of the file to be closed.
     */
    public static void close(int id) {}


    /*
     * User-level thread operations: Fork and Yield.  To allow multiple
     * threads to run within a user program. 
     */

    /**
     * Fork a thread to run a procedure ("func") in the *same* address space 
     * as the current thread.
     *
     * @param func The user address of the procedure to be run by the
     * new thread.
     */
    public static void fork(int func) {
	
	Debug.println('+', "HELLOOOOOOOOOOOOOOOOOOOOOOOOOOO:" + func);
	
	Debug.println('+', "Current SpaceId of calling thread: "+ ((UserThread)NachosThread.currentThread()).space.getSpaceId());
	AddrSpace space = ((UserThread)NachosThread.currentThread()).space; // Use *same* space address

	space.getThreadCounter().increment(); // Fork a thread will increment a the amount of threads in this address
	
	new ForkFunction(func, space);
    }

    /**
     * Yield the CPU to another runnable thread, whether in this address space 
     * or not. 
     */
    public static void yield() {
	Nachos.scheduler.yieldThread();
    }
    
    /**
     * Sleep a process for a number of ticks
     * @param ticks
     */
    public static void sleep(int ticks) {
	Debug.println('+', "Printing ticks passed to sleep sys call: "+ticks);
	
	((UserThread)NachosThread.currentThread()).setSleepTicks(ticks);
	
	SleepListManager.SleepListArray.add((UserThread)NachosThread.currentThread()); // Add to sleep list
	
	((UserThread)NachosThread.currentThread()).getSleepSem().P(); // Block for number of ticks
    }
    
    public static void mmap(String name, int reference) {
	
	
    }

}
