// ConsoleDriver.java
//
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and 
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.devices;

import java.util.ArrayList;
import java.util.List;

import nachos.Debug;
import nachos.machine.Console;
import nachos.machine.InterruptHandler;
import nachos.util.FIFOQueue;
import nachos.util.Queue;
import nachos.kernel.threads.Lock;
import nachos.kernel.threads.Semaphore;

/**
 * This class provides for the initialization of the NACHOS console,
 * and gives NACHOS user programs a capability of outputting to the console.
 * This driver does not perform any input or output buffering, so a thread
 * performing output must block waiting for each individual character to be
 * printed, and there are no input-editing (backspace, delete, and the like)
 * performed on input typed at the keyboard.
 * 
 * Students will rewrite this into a full-fledged interrupt-driven driver
 * that provides efficient, thread-safe operation, along with echoing and
 * input-editing features.
 * 
 * @author Eugene W. Stark
 */
public class ConsoleDriver {
    
    private static final int MAX = 10;
    /** Raw console device. */
    private Console console;

    /** Lock used to ensure at most one thread trying to input at a time. */
    private Lock inputLock;
    
    /** Lock used to ensure at most one thread trying to output at a time. */
    private Lock outputLock;
    
    /** Semaphore used to indicate that an input character is available. */
    private Semaphore charAvail = new Semaphore("Console char avail", 0);
    
    /** Semaphore used to indicate that output is ready to accept a new character. */
    private Semaphore outputDone = new Semaphore("Console output done", 1);
    
    
    /** Interrupt handler used for console keyboard interrupts. */
    private InterruptHandler inputHandler;
    
    /** Interrupt handler used for console output interrupts. */
    private InterruptHandler outputHandler;
    
    /** The buffer stores characters being output */
    
    private Queue<Character> output;
    private int countOutput ;
    
    /** The final input buffer store into memory  */
    public List<Character> finalInput;
    /** The input buffered stored */
    public List<Character> inputBuffer;
    
    
    private ArrayList<Character> line;
    /**
     * Initialize the driver and the underlying physical device.
     * 
     * @param console  The console device to be managed.
     */
    public ConsoleDriver(Console console) {
	inputLock = new Lock("console driver input lock");
	outputLock = new Lock("console driver output lock");
	/* no need to change console because it is in the machine*/
	this.console = console;
	// Delay setting the interrupt handlers until first use.
	/*The size of output buffer is 10*/
	countOutput = 0;
	finalInput = new ArrayList<>();
	inputBuffer = new ArrayList<>();
	output = new FIFOQueue<Character>();
	line = new ArrayList<>();
    }
    
    /**
     * Create and set the keyboard interrupt handler, if one has not
     * already been set.
     */
    private void ensureInputHandler() {
	if(inputHandler == null) {
	    inputHandler = new InputHandler();
	    console.setInputHandler(inputHandler);
	}
    }

    /**
     * Create and set the output interrupt handler, if one has not
     * already been set.
     */
    private void ensureOutputHandler() {
	if(outputHandler == null) {
	    outputHandler = new OutputHandler();
	    /*set the output handler, every time it handle interrupt, it will call the v()*/
	    console.setOutputHandler(outputHandler);
	}
    }

    /**
     * Wait for a character to be available from the console and then
     * return the character.
     */
    public char getChar() {
	inputLock.acquire();
	ensureInputHandler();
	charAvail.P();
	Debug.ASSERT(console.isInputAvail());
	/*input into the console*/
	char ch = console.getChar();
	inputLock.release();
	return ch;
    }
    
   
	
	
    

    /**
     * Print a single character on the console.  If the console is already
     * busy outputting a character, then wait for it to finish before
     * attempting to output the new character.  A lock is employed to ensure
     * that at most one thread at a time will attempt to print.
     *
     * @param buffer The character buffer to be printed.
     * @param the number of characters to be print
     */
    public void putChar(char ch) {
	//Debug.println('+', "print ch is " + ch);
	
	if(countOutput<MAX){
	    /*initiate the output*/
	    if(countOutput == 0){
		 /*put character in a buffer for output at a later time when the device is ready*/
		output.offer(ch);
		/*careful about synchronize access*/
		countOutput++;
		processOutput();
	    }
	    else{
		output.offer(ch);
		countOutput++;
	    }
	  
	}
	/*when the number of outputs in the queue is 10 or greater, block*/
	else{   
	outputLock.acquire();
//	outputDone.P();
//	Debug.ASSERT(!console.isOutputBusy());
	//console.putChar(ch);
//	for(int i = 0; i< output.s;i++){
//	    //Debug.println('+', "debug print");
//	   operatePrint(output.)
//	    //console.putChar(ch);
//	}
	Debug.println('+', "blocked");
	while(countOutput>=10){
	}
	outputLock.release();
	output.offer(ch);
	countOutput++;
	}
	
    }
    /*let console to print*/
    public void operatePrint(char ch){
	ensureOutputHandler();
	
	outputDone.P();
	    Debug.ASSERT(!console.isOutputBusy());
	    console.putChar(ch);
    }
    /*in the machine console to print all the characters*/
    public void processOutput(){
  		char result;
  		
  		if(countOutput!=0){
  		result = output.poll();
  		 countOutput--;
  		if(result == '\b'){
  		    if(line.size()>0){
  			operatePrint(result);
  			operatePrint(' ');
  			operatePrint('\b');
  			//Debug.println('+', "size is " + line.size());
  			
  			line.remove(line.size()-1);
  		    }
  		}
  		
  		else if ((int)result== 21){
  		    int size = line.size();
  		    for(int i =0; i<size;i++){
  			operatePrint('\b');
  			operatePrint(' ');
  			operatePrint('\b');
  			
  		    }
  		    line.clear();
  		    
  		}
  		
  		else if((int)result == 18){
  		  
  		    /*delete*/
  		    for(int i =0; i<line.size();i++){
			operatePrint('\b');
			operatePrint(' ');
			operatePrint('\b');
		    }
  		    
  		    /*retype*/
  		  for(int i =0; i<line.size();i++){
			operatePrint(line.get(i));
		    }
  		}
  		
  		else if(result == '\r' || result =='\n'){
  		    line.clear();
  		  operatePrint(result);
  		}
  		else if((int)result>=32 && (int)result<=126){
  		  line.add(result);
  		  operatePrint(result);
  		  
  		}
  		
  		//Debug.println('+', "size is " + line.size());
  		}
      }
    /**
     * Stop the console device.
     * This removes the interrupt handlers, which otherwise prevent the
     * Nachos simulation from terminating automatically.
     */
    public void stop() {
	inputLock.acquire();
	console.setInputHandler(null);
	inputLock.release();
	outputLock.acquire();
	console.setOutputHandler(null);
	outputLock.release();
    }
    
    /**
     * Interrupt handler for the input (keyboard) half of the console.
     */
    private class InputHandler implements InterruptHandler {
	
	@Override
	public void handleInterrupt() {
	    charAvail.V();
	}
	
    }
    
    /**
     * Interrupt handler for the output (screen) half of the console.
     */
    private class OutputHandler implements InterruptHandler {
	
	@Override
	public void handleInterrupt() {
	    outputDone.V();
	    processOutput();
	}
 	
    }
}
