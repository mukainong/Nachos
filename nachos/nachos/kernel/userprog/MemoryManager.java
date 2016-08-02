package nachos.kernel.userprog;

import nachos.Debug;
import nachos.machine.CPU;
import nachos.machine.MIPS;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;
import nachos.noff.NoffHeader;
import nachos.kernel.filesys.OpenFile;
/*This class is to manage the address space and keep track of what process is 
 * using each page of memory
 *multiple processes work together in memory. 
 *Each memory has its own virtual address space.
 *VPN != PPN virtual memoty space != 
 *initialize the page tables to refer to memory allocated by the memory manager.
 * */
/*This is the physical memory address manager*/
public class MemoryManager {
    public static int startFreePMP = 0;
    //PMP is the number of physical page. This represents the number of physical memory page left
    public static int numberOfPMPLeft = Machine.NumPhysPages;
    /*get the processes according to the physical page*/
    /*The physical memory manager allocate the physical memory for virtual address space*/
    public static long numberOfPMLeft = Machine.MemorySize;
    /*constructor for the memory manager*/
    public MemoryManager(){
	
    }
    
    
    /*An integer array start from 0 record which physical page is being used*/
    public static int[]PMP = new int[Machine.NumPhysPages];
    
    /*get the number of physical number of pages needed for the virtual memory*/
    /*number of pages needed for the space*/
    public static int getStartNumberOfPMP(){
	return numberOfPMPLeft;
    }
    
    public static void allocatePM(long size){
	numberOfPMLeft = numberOfPMLeft - size;
	//Debug.println('+', "allocate size " + size);
    }
    
    public static void freePM(long size){
	numberOfPMLeft = numberOfPMLeft + size;
	//Debug.println('+', "free size " + size);
    }
    /*free the AddrSpace. According to the translationEntry, get the number of physical page is used and get the corresponding
     * physical page number to free them.*/
    public static void freePMP(AddrSpace addressSpace){
	int[] PMArray = addressSpace.free();
	//Debug.println('+', "Physical page deallocated:");
	for(int i = 0; i < PMArray.length ;i++) {
	    
	    PMP[PMArray[i]]=0;
	    /*free the main memory need to be implemented*/
	    //Debug.println('+', PMArray[i]+"");
	}
	/*free the physical pages*/
	numberOfPMPLeft = numberOfPMPLeft + PMArray.length;
	freePM(addressSpace.currentSize);
    }
    
    public static void freeStackPMP(int[] PMPArray){
	//Debug.println('+', "Physical page deallocated:");
	for(int i = 0; i< PMPArray.length; i++){
	    PMP[PMPArray[i]]= 0;
	    //Debug.println('+', PMPArray[i]+"");
	}
	numberOfPMPLeft = numberOfPMPLeft + PMPArray.length;
	freePM(1024);
    }
    
    /*This method return an integer array store an array of stored the physical page number in each element */
    public static int[] allocatePMP(int numPages){
	int[] result = new int[numPages];
	int count = 0;
	//Debug.println('+', "Physical page allocated:");
	for(int i = (PMP.length-1);i>=0; i--){
	    if(count == numPages) 
		break;
	    if(PMP[i]==0){
		result[count] = i;
		count++;
		PMP[i]=1;
	    }
	    //Debug.println('+', i+"");
	}
	numberOfPMPLeft = numberOfPMPLeft - numPages;
	
	return result;
    }
    
    public static void pageFaultHanle() {
	
    }
    
}
