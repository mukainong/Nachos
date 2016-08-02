package nachos.kernel.userprog;

import java.util.ArrayList;
import java.util.List;

import nachos.kernel.threads.SubNachosThread;

/*
 * This is the class for keeping track of sleeping threads
 * */
public class SleepListManager {
   
    public static List<SubNachosThread> SleepListArray = new ArrayList<>();
    
}
