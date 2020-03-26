import java.util.HashMap;

public class SimpleAgent {
	
	String name;
	boolean lastAgent;
	HashMap<String, Double> utilities = new HashMap<String, Double>();

	public SimpleAgent(boolean isLastAgent) {
		lastAgent = isLastAgent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isLastAgent() {
		return lastAgent;
	}
	
	public HashMap<String, Double> getUtilities() {
		return utilities;
	}

	public void addUtility(String task, double utility) {
		utilities.put(task, utility);
	}
	
}
