import java.util.*;

public class Route {

	public String name;
	public int numStops;
	public int timeBetweenStops;
	public List<Bus> busses;

	public Route(String name) {
		busses = new ArrayList<Bus>();
		this.name = name;
	}
}