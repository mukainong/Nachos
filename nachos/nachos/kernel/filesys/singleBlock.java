
package nachos.kernel.filesys;

import nachos.Debug;

/**
 * This is the single block stores in an sector contains the dataSector to help
 * file header to store the data into the disk. The data that file header cannot 
 * reference
 */
class singleBlock {
    /** Number of pointers to data blocks stored in a file header. */
    private final int NumDirect;

    /** Maximum file size that can be stored in this single block */
    private final int MaxBlockSize;


    /** Disk sector numbers for each data block in the file. */
    private int dataSectors[];

    /** The underlying filesystem in which the file header resides. */
    private final FileSystemReal filesystem;

    /** Disk sector size for the underlying filesystem. */
    private final int diskSectorSize;
    
    //create a single block to store the 
    singleBlock(FileSystemReal filesystem) {
	this.filesystem = filesystem;
	diskSectorSize = filesystem.diskSectorSize;
	//the number of pointers to data blocks is 32
	NumDirect = diskSectorSize/4;
	//max file size is 32 * 128
	MaxBlockSize = (NumDirect * diskSectorSize);
	//create 32 blocks
	dataSectors = new int[NumDirect];
	// Safest to fill the table with garbage sector numbers,
	// so that we error out quickly if we forget to initialize it properly.
	for(int i = 0; i < NumDirect; i++)
	    //default
	    dataSectors[i] = -1;
    }

    // the following methods deal with conversion between the on-disk and
    // the in-memory representation of a DirectoryEnry.
    // Note: these methods must be modified if any instance variables 
    // are added!!

    /**
     * Initialize the fields of this single data block object using
     * data read from the disk.
     *
     * @param buffer A buffer holding the data read from the disk.
     * @param pos Position in the buffer at which to start.
     */
    private void internalize(byte[] buffer, int pos) {
	for (int i = 0; i < NumDirect; i++)
	    dataSectors[i] = FileSystem.bytesToInt(buffer, pos+i*4);
    }

    /**
     * Export the fields of this data block object to a buffer
     * in a format suitable for writing to the disk.
     *
     * @param buffer A buffer into which to place the exported data.
     * @param pos Position in the buffer at which to start.
     */
    private void externalize(byte[] buffer, int pos) {
	for (int i = 0; i < NumDirect; i++)
	    FileSystem.intToBytes(dataSectors[i], buffer, pos+i*4);
    }

 
    /**
     * 
     * @param freeMap freeMap use to find the free sector
     * @param space the new additional space needed to allocate for the file  
     * @param numBytes the number of bytes has been used in this single block it can be get by totalSize - 3840
     * @return true if add successfully, failed if fail to add
     */
    boolean allocateAdditional(BitMap freeMap, int space, int numBytes){
	//the number of current sectors be used
	int numSectors = numBytes/diskSectorSize;
	if(numBytes % diskSectorSize!= 0) numSectors++;
	//Debug.println('+', "space is " + space + " numBytes" + numBytes);
	int totalSize = numBytes + space;
	//can not allocate space inside the signle block anymore, should add to the double block
	if(totalSize > MaxBlockSize)
	    return false;
	numBytes = totalSize;
	//get the total number of sectors needed
	int totalSectors = totalSize/diskSectorSize;
	
	if(totalSize % diskSectorSize!= 0) totalSectors++;
	//get the number of sectors added
	int addSectors = totalSectors - numSectors;
	//Debug.println('+',"total size is " + totalSize);
	//Debug.println('+', "totalSector number is " + totalSectors);
	if(addSectors!=0){
	for(int i = numSectors; i<totalSectors;i++){
	    dataSectors[i] = freeMap.find();
	}
	}
	numSectors  = totalSectors;
	if(freeMap.numClear()<addSectors||NumDirect<totalSectors)
	    return false;
	//Debug.println('+', "allocate " + space +" amount of space usinng the single block");
	return true;
    }
    /**
     * De-allocate all the space allocated for data blocks for this single block.
     *
     * @param freeMap is the bit map of free disk sectors.
     */
    void deallocate(BitMap freeMap) {
	//get the number of sectors used
	int numberSectors = 0;
	for (; numberSectors<dataSectors.length;numberSectors++){
	    if(dataSectors[numberSectors] == -1)
		break;
	}
	for (int i = 0; i < numberSectors; i++) {
	    Debug.ASSERT(freeMap.test(dataSectors[i]));  // ought to be marked!
	    freeMap.clear(dataSectors[i]);
	}
    }
    
    // get data from the disk to the single block
    void fetchFrom(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	filesystem.readSector(sector, buffer, 0);
	internalize(buffer, 0);
    }
    /**
     * Write the modified contents of the single block back to disk. 
     *
     * @param sector is the disk sector to contain the single block.
     */
    void writeBack(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	externalize(buffer, 0);
	filesystem.writeSector(sector, buffer, 0); 
    }

    /**
     * Calculate which disk sector is storing a particular byte within the file.
     *    This is essentially a translation from a virtual address (the
     *	offset in the file) to a physical address (the sector where the
     *	data at the offset is stored).
     *
     * @param offset The location within the file of the byte in question.
     * @return the disk sector number storing the specified byte.
     */
    int byteToSector(int offset) {
	//Debug.println('+', "single block sector is " + (offset / diskSectorSize - 28));
	return(dataSectors[offset / diskSectorSize - 28]);
    }
    
    int byteToSectorForDouble(int sector){
	return dataSectors[sector];
    }

    /**
     * 	Print the contents of the file header, and the contents of all
     *	the data blocks pointed to by the file header.
     */
    void print(int numBytes) {

	int numSectors = 0;
	while(dataSectors[numSectors]!=-1){
	    numSectors++;
	}
	int i, j, k;
	byte data[] = new byte[diskSectorSize];

	System.out.print("single block contents.  single block size: " + numBytes
		+ ".,  File blocks: ");
	for (i = 0; i < numSectors; i++)
	    System.out.print(dataSectors[i] + " ");

	System.out.println("\nFile contents:");
	for (i = k = 0; i < numSectors; i++) {
	    filesystem.readSector(dataSectors[i], data, 0);
	    for (j = 0; (j < diskSectorSize) && (k < numBytes); j++, k++) {
		if ('\040' <= data[j] && data[j] <= '\176')   // isprint(data[j])
		    System.out.print((char)data[j]);
		else
		    System.out.print("\\" + Integer.toHexString(data[j] & 0xff));
	    }
	    System.out.println();
	}
    }
}
