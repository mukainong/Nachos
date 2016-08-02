package nachos.util;

public class LevelQueue<T> extends FIFOQueue<T> {
    private int quantum;
    
    public void setQuantum(int num) {
	quantum = num;
    }
    
    public int getQuantum() {
	return quantum;
    }
}
