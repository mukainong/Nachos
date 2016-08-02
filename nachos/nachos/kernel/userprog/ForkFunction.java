package nachos.kernel.userprog;

import nachos.Debug;
import nachos.kernel.Nachos;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.NachosThread;
import nachos.machine.TranslationEntry;

public class ForkFunction implements Runnable{
    	int startLocation = 0;
    	/*The address space of the calling thread should be gotten*/
    	AddrSpace space;
    	TranslationEntry FunctionpageTable[];
        
    	public ForkFunction(int startLocation, AddrSpace space){   
    	    this.startLocation = startLocation;
    	    this.space = space;
    	    //space.funcArray.add(this);
    	    Debug.println('+', "Start Fork");
        
    	    UserThread t = new UserThread("children thread forked at "+startLocation, this, space);
    	    this.FunctionpageTable = space.StackAllocate(startLocation);
            t.setPageTable(FunctionpageTable);	  
    	    Nachos.scheduler.readyToRun(t);
        }
    	
    	@Override
	public void run() {
         
    	    /*set the start location of register*/
    	    //CPU.writeRegister(MIPS.PCReg, startLocation);
    	    /*tell the next instruction*/
    	    //CPU.writeRegister(MIPS.NextPCReg, startLocation+4);
    	    /*set the stack pointer to find stack to process the new thread*/ 
   	
    	    // Set the stack register to the end of the segment.
    	    // set the initial register values
   	 
    	    space.initRegisters(startLocation);	// set the initial register values
   	
    	    CPU.setPageTable(FunctionpageTable);		// load page table register
    	    CPU.runUserCode();			// jump to the user progam
    	    //Debug.ASSERT(false);	
    	}
        
        public void setSpace(AddrSpace space) {
            /**
             * The address space should be shared between parent thread and child thread 
             */
            this.space = space;
        }
        
        /*return the pageTable for free the Stack */
        public TranslationEntry[] getStackTable(){
            return FunctionpageTable;
        }
}
