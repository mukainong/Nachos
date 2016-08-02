
package nachos.kernel.filesys;

import nachos.Debug;

/**
 * This is the double block stores in an sector contains the Sectors  
 * reference to the single block sector
 */
class doubleBlock {
    /** Number of pointers to data blocks stored in a file header. */
    private final int NumDirect;

    /** Maximum number of single block sectors in the file */
    private final int MaxBlockSize;


    /** Disk sector numbers for each single block */
    private int dataSectors[];

    /** The underlying filesystem in which the file header resides. */
    private final FileSystemReal filesystem;

    /** Disk sector size for the underlying filesystem. */
    private final int diskSectorSize;
    
    private singleBlock[] blockArray;
    
    //create a single block to store the 
    doubleBlock(FileSystemReal filesystem) {
	this.filesystem = filesystem;
	diskSectorSize = filesystem.diskSectorSize;
	//the number of pointers to data blocks is 32
	NumDirect = diskSectorSize/4;
	//max file size is 32 * 128
	MaxBlockSize = (NumDirect * diskSectorSize);
	//create 32 blocks each block references to a single block
	dataSectors = new int[NumDirect];
	// Safest to fill the table with garbage sector numbers,
	// so that we error out quickly if we forget to initialize it properly.
	for(int i = 0; i < NumDirect; i++){
	    dataSectors[i] = -1;
	}
	blockArray = new singleBlock[NumDirect];
	for(int i =0; i< NumDirect;i++ ){
	    blockArray[i] = new singleBlock(filesystem);
	}
    }

    // the following methods deal with conversion between the on-disk and
    // the in-memory representation of a DirectoryEnry.
    // Note: these methods must be modified if any instance variables 
    // are added!!

    /**
     * Initialize the fields of this double data block object using
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
     * @param space the new space needed to allocate for the file  
     * @param numBytes the number of bytes has been used in this double block it can be get by totalSize - 3840
     * @return true if add successfully, failed if fail to add
     */
    boolean allocateAdditional(BitMap freeMap, int space, int numBytes){
	//Debug.println('+', "add double block" + " space is " + space + " numBytes is " + numBytes);
	//first get the number of the single blocks has been used
	int numberOfSectors = 0;
	for (;numberOfSectors<NumDirect;numberOfSectors++){
	    if(dataSectors[numberOfSectors]== -1){
		break;
	    }
	}
	if(space == 0){
	    return true; // no need to change anything
	}
	//get the single block size and the total size inside the doulbe block
	int singleBlockSize = NumDirect * diskSectorSize;
	int totalSize = numBytes + space;
	
	//get the total number of sectors needed
	int totalSectors = totalSize/singleBlockSize;
	
	if(totalSize % singleBlockSize!= 0) totalSectors++;
	if(totalSectors > NumDirect){
	    return false;  //not enough sectors to use
	}
	//get the number of sectors added which the more single block will be used
	int addSectors = totalSectors - numberOfSectors;
	//according to the number of single block added, allocate space for each block
	//find the sector to store those single block
	for(int i = numberOfSectors; i<totalSectors;i++){
	    dataSectors[i] = freeMap.find();
	}
	
	
	int remainSpace = 0;
	//condition when the double has been used before
	//first allocate for the single block that is not full
	if(numBytes % singleBlockSize != 0){
	remainSpace = singleBlockSize - (numBytes % singleBlockSize);
	}
	//the condition when no remaining space
	//allocate space for that single block
	if(addSectors==0){
	    //Debug.println('+', "added space is " + space + " numberBytes % singleBlockSize is" +numBytes +" in double block" );
	    blockArray[numberOfSectors-1].allocateAdditional(freeMap, space, numBytes % singleBlockSize); // when no need to have new block
	}
	
	else{
	    int numberOfFullBlock;
	    //the condition there are partial block
	    if(remainSpace != singleBlockSize && remainSpace !=0){
	    blockArray[numberOfSectors-1].allocateAdditional(freeMap, remainSpace, numBytes % singleBlockSize);
	    
	    //allocate space for other single blocks, first calculate how many full size single block needed
	    numberOfFullBlock = (space - remainSpace)/singleBlockSize;
	    }
	    //no partial block
	    else{
		numberOfFullBlock = space/singleBlockSize;
	    }
	    for(int i = 0; i< numberOfFullBlock;i++){
		blockArray[numberOfSectors+i].allocateAdditional(freeMap, singleBlockSize, 0);
	    }
	    //remaining space for the last new single block
	    //when remainSpace == singleBlockSize and numBytes == 0
	    //Debug.println('+', "space is " + space + "remainSpace " + remainSpace);
	    int  remainLastSpace =0;
	   
	    remainLastSpace = (space - remainSpace)%singleBlockSize;
	    
	    if(remainLastSpace != 0){
		blockArray[totalSectors-1].allocateAdditional(freeMap, remainLastSpace, 0);
	    }
	
	}
	if(freeMap.numClear()<addSectors||NumDirect<totalSectors)
	    return false;
	//Debug.println('+', "allocate " + space +" amount of space usinng the double block");
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
	    blockArray[i].deallocate(freeMap);
	}
    }
    
    // get data from the disk to the single block
    void fetchFrom(int sector) {
	byte buffer[] = new byte[diskSectorSize];
	filesystem.readSector(sector, buffer, 0);
	internalize(buffer, 0);
	//after restoring the single block sector into the data sector, restoring each single block's data
	for(int i = 0; i < NumDirect;i++){
	    if(dataSectors[i]!=-1){
	    blockArray[i].fetchFrom(dataSectors[i]);
	    }
	    else{
		break;
	    }
	}
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
	//after storing the single block sector into the data sector, writing each single block's data
	for(int i =0; i<NumDirect; i++){
	    if(dataSectors[i]!=-1){
	    blockArray[i].writeBack(dataSectors[i]);
	    }
	    else{
		break;
	    }
	}
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
	//first determine to get sector from which single block
	int sectorLocation = offset/diskSectorSize - 28- 32;
	//determine in which single block
	int blockLocation = sectorLocation / 32;
	
	int blockRM = sectorLocation % 32;
	//Debug.println('+', "block location is"+blockLocation + " blockRM is " + blockRM);
	    return blockArray[blockLocation].byteToSectorForDouble(blockRM);
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
