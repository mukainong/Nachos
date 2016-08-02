// AddrSpace.java
//	Class to manage address spaces (executing user programs).
//
//	In order to run a user program, you must:
//
//	1. link with the -N -T 0 option 
//	2. run coff2noff to convert the object file to Nachos format
//		(Nachos object code format is essentially just a simpler
//		version of the UNIX executable object code format)
//	3. load the NOFF file into the Nachos file system
//		(if you haven't implemented the file system yet, you
//		don't need to do this last step)
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;
import nachos.noff.NoffHeader;
import nachos.kernel.filesys.OpenFile;
import nachos.kernel.threads.Semaphore;
import nachos.kernel.userprog.MemoryManager;
/**
 * This class manages "address spaces", which are the contexts in which
 * user programs execute.  For now, an address space contains a
 * "segment descriptor", which describes the the virtual-to-physical
 * address mapping that is to be used when the user program is executing.
 * As you implement more of Nachos, it will probably be necessary to add
 * other fields to this class to keep track of things like open files,
 * network connections, etc., in use by a user program.
 *
 * NOTE: Most of what is in currently this class assumes that just one user
 * program at a time will be executing.  You will have to rewrite this
 * code so that it is suitable for multiprogramming.
 * 
 * @author Thomas Anderson (UC Berkeley), original C++ version
 * @author Peter Druschel (Rice University), Java translation
 * @author Eugene W. Stark (Stony Brook University)
 */
public class AddrSpace {
    
    
    public class ThreadCounter {
	private int counter;
	Semaphore counterLock = new Semaphore("counterLock", 1);
	
	public ThreadCounter () {
	    counter = 1;
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
	
	public boolean checkLastThread() {
	    boolean isLastThread = false;
	    counterLock.P();
	    if (counter == 1)
		isLastThread = true;
	    counterLock.V();
	    return isLastThread;
	}
  }  
    
  /** Page table that describes a virtual-to-physical address mapping. */
  private TranslationEntry pageTable[];
  
  private int SpaceId;
  
  public  long currentSize = 0;
  
  private ThreadCounter threadCounter;
  
  private int status;
  
  private Semaphore processLock;
  
  /** Default size of the user stack area -- increase this as necessary! */
  private static final int UserStackSize = 1024;

  /**
   * Create a new address space.
   */
  public AddrSpace() { 
      SpaceIdManager.AddrSpaceArray.add(this);
      this.SpaceId = SpaceIdManager.AddrSpaceArray.indexOf(this);
      processLock = new Semaphore("processLock", 0);
      threadCounter = new ThreadCounter();
  }


  /**
   * Load the program from a file "executable", and set everything
   * up so that we can start executing user instructions.
   *
   * Assumes that the object code file is in NOFF format.
   *
   * First, set up the translation from program memory to physical 
   * memory.  For now, this is really simple (1:1), since we are
   * only uniprogramming.
   *
   * @param executable The file containing the object code to 
   * 	load into memory
   * @return -1 if an error occurs while reading the object file,
   *    otherwise 0.
   */
  public int exec(OpenFile executable) {
    NoffHeader noffH;
    long size;
    
    if((noffH = NoffHeader.readHeader(executable)) == null)
	return(-1);

    // how big is address space? the code segment + initialized data + uninitialized data
    size = roundToPage(noffH.code.size) + roundToPage(noffH.initData.size + noffH.uninitData.size) + UserStackSize;	
    				// we need to increase the size
    				// to leave room for the stack
    
    this.currentSize = size;
    int numPages = (int)(size / Machine.PageSize);
    
    Debug.ASSERT((numPages<=MemoryManager.numberOfPMPLeft), "AddrSpace constructor: Not enough memory!");
    
    Debug.println('a', "Initializing address space, numPages=" + numPages + ", size=" + size);
    
    // first, set up the translation 
    pageTable = new TranslationEntry[numPages];
    
    int[] PMPArray = MemoryManager.allocatePMP(numPages);

    for (int i = 0; i < numPages; i++) {
      pageTable[i] = new TranslationEntry();
     
      pageTable[i].virtualPage = i; 
      pageTable[i].physicalPage = PMPArray[i];
      pageTable[i].valid = true;
      pageTable[i].use = false;
      pageTable[i].dirty = false;
      pageTable[i].readOnly = false;  // if code and data segments live on				      
      				      // separate pages, we could set code 
				      // pages to be read-only
    }
    
    if (noffH.code.size > 0) {
	int VA = 0;
	int PA = 0;
	for(int i = 0; i< noffH.code.size;i++){
	    VA = noffH.code.virtualAddr + i;
	    PA = this.convertPA(VA);
	    executable.seek(noffH.code.inFileAddr + i);
	    executable.read(Machine.mainMemory,PA,1);
	}
	
	Debug.println('a', "Storing code segment");
    }

    if (noffH.initData.size > 0) {
  	int VA = 0;
  	int PA = 0;
  	for(int i = 0; i< noffH.initData.size;i++){
  	    VA = noffH.initData.virtualAddr + i;
  	    PA = this.convertPA(VA);
  	    executable.seek(noffH.initData.inFileAddr + i);
  	    executable.read(Machine.mainMemory,PA,1);
  	}
  	Debug.println('a', "Storing initData segment");
    }
    
    if(noffH.uninitData.size>0){
	int VA = 0;
  	int PA = 0;
  	for(int i = 0; i< noffH.uninitData.size;i++){
  	    VA = noffH.uninitData.virtualAddr + i;
  	    PA = this.convertPA(VA);
  	    executable.seek(noffH.uninitData.inFileAddr + i);
  	    executable.read(Machine.mainMemory,PA,1);
  	}
  	Debug.println('a', "Storing uninitData segment");
    }
    MemoryManager.allocatePM(size);
    return(0);
  }
  
  

  /**
   * Initialize the user-level register set to values appropriate for
   * starting execution of a user program loaded in this address space.
   *
   * We write these directly into the "machine" registers, so
   * that we can immediately jump to user code.
   */
  public void initRegisters() {
    int i;
   
    for (i = 0; i < MIPS.NumTotalRegs; i++)
      CPU.writeRegister(i, 0);

    // Initial program counter -- must be location of "Start"
    CPU.writeRegister(MIPS.PCReg, 0);	

    // Need to also tell MIPS where next instruction is, because
    // of branch delay possibility
    CPU.writeRegister(MIPS.NextPCReg, 4);

    // Set the stack register to the end of the segment.
    // NOTE: Nachos traditionally subtracted 16 bytes here,
    // but that turns out to be to accomodate compiler convention that
    // assumes space in the current frame to save four argument registers.
    // That code rightly belongs in start.s and has been moved there.
    int sp = pageTable.length * Machine.PageSize;
    CPU.writeRegister(MIPS.StackReg, sp);
    Debug.println('a', "Initializing stack register to " + sp);
  }

  /**
   * Overload initRegisters method with an integer argument
   * @param func
   */
  public void initRegisters(int func) {
      int i;
     
      for (i = 0; i < MIPS.NumTotalRegs; i++)
        CPU.writeRegister(i, 0);

      // Initial program counter -- must be location of "Start"
      CPU.writeRegister(MIPS.PCReg, func);	

      // Need to also tell MIPS where next instruction is, because
      // of branch delay possibility
      CPU.writeRegister(MIPS.NextPCReg, func + 4);

      // Set the stack register to the end of the segment.
      // NOTE: Nachos traditionally subtracted 16 bytes here,
      // but that turns out to be to accomodate compiler convention that
      // assumes space in the current frame to save four argument registers.
      // That code rightly belongs in start.s and has been moved there.
      int sp = pageTable.length * Machine.PageSize;
      CPU.writeRegister(MIPS.StackReg, sp);
      Debug.println('a', "Initializing stack register to " + sp);
    }
  
  /**
   * On a context switch, save any machine state, specific
   * to this address space, that needs saving.
   *
   * For now, nothing!
   */
  public void saveState() {}

  /**
   * On a context switch, restore any machine state specific
   * to this address space.
   *
   * For now, just tell the machine where to find the page table.
   */
  public void restoreState() {
    CPU.setPageTable(pageTable);
  }

  /**
   * Utility method for rounding up to a multiple of CPU.PageSize;
   */
  private long roundToPage(long size) {
    return(Machine.PageSize * ((size+(Machine.PageSize-1))/Machine.PageSize));
  }
  
  /*the address free the physical address and return an integer array that contains the physical page number being free*/
  public int[] free(){
      int i = 0;
      /*count the size of the page table*/
      int count = 0;
      int PA = 0;
      /*check whether there is a translation entry*/
      count = pageTable.length;
      /*after get the number of pages used*/
      int[] result = new int[count];
      i = 0;
      for( i = 0; i< count; i++){
	  result[i] =  pageTable[i].physicalPage;
      }
      /*free Everything inside the memory*/
      for(i = 0; i<currentSize; i++){
	 PA = this.convertPA(i);
	  Machine.mainMemory[PA]=0;
      }
      return result;   
  }
  
  /*return an array store pages in each element*/
  /*free stack of any thread according to its page table*/
  public int[] freeStack(TranslationEntry pageTable[])
  {
      
      int PA =0;
      int StackSize = 1024;
      int numPages = (int)(this.currentSize/Machine.PageSize);
      int StackPages = (int)(StackSize/Machine.PageSize);
      int[] PMPArray = new int[StackPages];
      for(int i = numPages - StackPages; i < numPages; i++){
	  PMPArray[i-numPages+StackPages] = pageTable[i].physicalPage;
      }
      /*free Everyting inside the memory Stack*/
   
      for(long i = currentSize- StackSize; i<currentSize;i++){
	  PA = this.convertPA((int)i);
	  Machine.mainMemory[PA]=0;
      }
      
      return PMPArray;
  }

  public String ReadString(int VA){
      String result = "";
      int PA = 0;
      byte read = 0;

      do{
	 
	  PA  = this.convertPA(VA);
	  read = Machine.mainMemory[PA];
	  if(read == 0){
	      break;
	  }
	  result = result + (char)read;
	  Debug.println('+',""+(char)read);
	  VA++;
      }while(true);
      
      Debug.println('+', "The name pass to exec is: " + result);
      return result;
  }
  
  public int convertPA (int VA) {
      int VPN = 0;
      int OFF = 0;
      int PPN = 0;
      int PA = 0;
      
      VPN = ((VA >>7 )& 0x1ffffff);
      OFF = (VA & 0x7f);
      PPN = pageTable[VPN].physicalPage;
      PA = ((PPN<<7) | OFF);
      
      return PA;
  }
  
  //Load function, and set everything
  //up so that we can start forking function
  //the start virtual address for the function should be passed
  //because the new stack should be allocated
  //The code and data pages are the same for all the threads executing in the same address space
  public TranslationEntry[] StackAllocate(int func) {
      int FunctionStackSize = UserStackSize;
     //allocate stack for process the function which is equal to the 
     TranslationEntry FunctionpageTable[];
     int numPages = (int)(this.currentSize / Machine.PageSize);
     FunctionpageTable = new TranslationEntry[numPages];
     /*Copy the data and code segment, get the stack portion size is 1024*/
     int StackPages = (int)(FunctionStackSize/Machine.PageSize);
     for (int i = 0; i < numPages - StackPages; i++) {
		//Debug.println('+', ""+i);
	 FunctionpageTable[i] = pageTable[i];
     }
     
     int[] PMPArray = MemoryManager.allocatePMP(StackPages);
     
     for(int k = numPages-StackPages; k<numPages; k++){
	FunctionpageTable[k] = new TranslationEntry();    
	FunctionpageTable[k].virtualPage = k; 
	FunctionpageTable[k].physicalPage = PMPArray[k-numPages+StackPages];
	FunctionpageTable[k].valid = true;
	FunctionpageTable[k].use = false;
	FunctionpageTable[k].dirty = false;
	FunctionpageTable[k].readOnly = false;	
     }
     return FunctionpageTable;
  }
  
 
  
  public int getSpaceId(){
      return this.SpaceId;
  }
  
  public Semaphore getSemaphore() {
      return processLock;
  }
  
  public void setStauts(int value) {
	status = value;
  }
  
  public int getStatus() {
	return status;
  }
  
  public ThreadCounter getThreadCounter() {
      return threadCounter;
  }
  
  public TranslationEntry[] getPageTable(){
      return this.pageTable;
  }
  public void setPageTable( TranslationEntry[] pageTable){
      this.pageTable = pageTable;
  }
}
