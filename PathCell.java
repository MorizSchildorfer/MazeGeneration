import java.util.ArrayList;
import java.util.Random;


public class PathCell {
	public PathCell representative;
	public ArrayList<Integer> unlinked = new ArrayList<Integer>();
	public ArrayList<Integer> linked = new ArrayList<Integer>();
	public static int[] directions = {0,1,2,3};
	public static Random randomizer = new Random();
	public boolean disconnected = false;
	
	public PathCell() {
		representative = this;
	}
	
	public static void directionShuffle() {
		int temp;
		int randInt;
		for(int i = directions.length-1; i>0; i--) {
			randInt = randomizer.nextInt(i+1);
			temp = directions[randInt];
			directions[randInt]= directions[i];
			directions[i] = temp;
		}
	}
	
	public void semiLink(int dir) {
		unlinked.remove(unlinked.indexOf(dir));
		linked.add(dir);
	}
	public void unSemiLink(int dir) {
		unlinked.add(dir);
		linked.remove(linked.indexOf(dir));
	}
}
