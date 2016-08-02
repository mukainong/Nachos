package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.List;

/*
 * This is the class for keeping track of the Space Id
 * */
public class SpaceIdManager {
   
    /*
     * An array stored all the AddrSpace element the location
     *  number is the space id. 
     * */
    public static List<AddrSpace> AddrSpaceArray = new ArrayList<>();
    
}
