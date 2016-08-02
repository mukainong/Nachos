// Copyright (c) 2003 State University of New York at Stony Brook.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

package nachos.kernel.userprog;

import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.MachineException;
import nachos.machine.NachosThread;
import nachos.kernel.userprog.Syscall;

/**
 * An ExceptionHandler object provides an entry point to the operating system
 * kernel, which can be called by the machine when an exception occurs during
 * execution in user mode.  Examples of such exceptions are system call
 * exceptions, in which the user program requests service from the OS,
 * and page fault exceptions, which occur when the user program attempts to
 * access a portion of its address space that currently has no valid
 * virtual-to-physical address mapping defined.  The operating system
 * must register an exception handler with the machine before attempting
 * to execute programs in user mode.
 */
public class ExceptionHandler implements nachos.machine.ExceptionHandler {

  /**
   * Entry point into the Nachos kernel.  Called when a user program
   * is executing, and either does a syscall, or generates an addressing
   * or arithmetic exception.
   *
   * 	For system calls, the following is the calling convention:
   *
   * 	system call code -- r2,
   *		arg1 -- r4,
   *		arg2 -- r5,
   *		arg3 -- r6,
   *		arg4 -- r7.
   *
   *	The result of the system call, if any, must be put back into r2. 
   *
   * And don't forget to increment the pc before returning. (Or else you'll
   * loop making the same system call forever!)
   *
   * @param which The kind of exception.  The list of possible exceptions 
   *	is in CPU.java.
   *
   * @author Thomas Anderson (UC Berkeley), original C++ version
   * @author Peter Druschel (Rice University), Java translation
   * @author Eugene W. Stark (Stony Brook University)
   */
    public void handleException(int which) {
	int type = CPU.readRegister(2);

	if (which == MachineException.SyscallException) {

	    switch (type) {
	    case Syscall.SC_Halt:
		Syscall.halt();
		break;
	    case Syscall.SC_Exit:
		Syscall.exit(CPU.readRegister(4));
		break;
	    case Syscall.SC_Exec:
		Debug.println('+', "Register 4 in exec syscall exception: "+CPU.readRegister(4));
		String execFileName ="";
		int VA = CPU.readRegister(4);

		/* At present, CPU stores the virtual memory inside the register
		 * Pointer argument are user virtual adresses.
		 * The data has to be copied in byte by byte from the user adress space
		 * we read the virtual memory we need to convert the virtual memory into physical 
		 * memory to look up the byte stored inside the mainMemory*/
		/*virtual page number*/
		
		AddrSpace space = ((UserThread)NachosThread.currentThread()).space;
		execFileName = space.ReadString(VA);
		
		CPU.writeRegister(2, Syscall.exec(execFileName)); // Sys call & write back return value to r2
		
		break;
	    case Syscall.SC_Write:
		//The virtual start address of the buffer to write
		int ptr1 = CPU.readRegister(4);
		//The length of the buffer
		int len1 = CPU.readRegister(5);
		byte buf1[] = new byte[len1];
		int tem1 = 0;
		AddrSpace space1 = ((UserThread)NachosThread.currentThread()).space;
		for(int i = 0; i<len1; i++){
		    tem1 = space1.convertPA(ptr1);
		    System.arraycopy(Machine.mainMemory, tem1, buf1, i, 1);
		    ptr1++;
		}
		Syscall.write(buf1, len1, CPU.readRegister(6));
		break;
	    
	    case Syscall.SC_Read:
		 
		int ptr2 = CPU.readRegister(4);
		
		int len2 = CPU.readRegister(5);
		
		byte buf2[] = new byte[len2];
		
		int tem2 = 0;			
		
		int max = Syscall.read(buf2, len2, CPU.readRegister(6));
		
		AddrSpace space2 = ((UserThread)NachosThread.currentThread()).space;
		/*store everything into the memory*/
		for(int i = 0; i<len2; i++)
		{
		    /*the physical address*/
		    tem2 = space2.convertPA(ptr2);
		    System.arraycopy(buf2, i, Machine.mainMemory, tem2, 1);
		    ptr2++;
		    /*only this amount of characters to be printed*/
		    if(i==max-1)break;
		}
		
		// Remember Read() sys call returns an integer
		CPU.writeRegister(2, max);
		break;
		
	    case Syscall.SC_Fork:
		Syscall.fork(CPU.readRegister(4));
		break;
		
	    case Syscall.SC_Join:
		CPU.writeRegister(2, Syscall.join(CPU.readRegister(4))); // Sys call & write back return value to r2
		break;
		
	    case Syscall.SC_Yield:
		Syscall.yield();
		break;
		
	    case Syscall.SC_Sleep:
		Syscall.sleep(CPU.readRegister(4));
		break;
		
	    case Syscall.SC_Mmap:
		
		String execFileName3 ="";
		
		int VA3 = CPU.readRegister(4);
		
		AddrSpace space3 = ((UserThread)NachosThread.currentThread()).space;
		execFileName3 = space3.ReadString(VA3);
		
		Syscall.mmap(execFileName3, CPU.readRegister(5));
		
		    MemoryManager.pageFaultHanle();
		break;
	    }
	    
	    // Update the program counter to point to the next instruction
	    // after the SYSCALL instruction.
	    CPU.writeRegister(MIPS.PrevPCReg,
		    CPU.readRegister(MIPS.PCReg));
	    CPU.writeRegister(MIPS.PCReg,
		    CPU.readRegister(MIPS.NextPCReg));
	    CPU.writeRegister(MIPS.NextPCReg,
		    CPU.readRegister(MIPS.NextPCReg)+4);
	    return;
	}

	System.out.println("Unexpected user mode exception " + which + ", " + type);
	Debug.ASSERT(false);

    }
}
