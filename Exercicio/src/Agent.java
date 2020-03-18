import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Agent {

	int steps;
	String decision;
	int restart;
	double memoryFactor;
	
	String bestTask;
	int gain;
	HashMap<String, Integer> speculativeUtilities = new HashMap<String, Integer>();
	HashMap<String, Integer> observedUtilities = new HashMap<String, Integer>();
	
	public Agent(String options) {
		if (options.contains("cycle")) steps = Integer.parseInt(options.split("cycle=")[1].split(" ")[0].toString().trim());
		if (options.contains("decision")) decision = options.split("decision=")[1].split(" ")[0].trim();
		if (options.contains("restart")) restart = Integer.parseInt(options.split("restart=")[1].split(" ")[0].trim());
		if (options.contains("memory-factor")) memoryFactor = Integer.parseInt(options.split("memory-factor=")[1].split(" ")[0].trim());
	}
	
	public void perceive(String input) {
		if (input.startsWith("T")) {
			String task = input.split(" ")[0].toString();
			int utility = Integer.parseInt(input.split("=")[1].toString().trim());
			speculativeUtilities.put(task, utility);
			System.out.println(speculativeUtilities.get(task));
		} else {
			if (input.startsWith("A")) {
				int newValue = Integer.parseInt(input.split("=")[1].toString().trim());
				speculativeUtilities.put(bestTask, newValue);
				observedUtilities.put(bestTask, newValue);
				gain += newValue;
			}
		}
	}
	
	public void decideAndAct() {
		bestTask = bestUtilityTask(speculativeUtilities);
		System.out.println("Best task: " + bestTask);
	}
	
	private String bestUtilityTask(HashMap<String, Integer> map) {
		String bestTask = null;
		for (String task: map.keySet()) {
			if (bestTask == null || map.get(task) > map.get(bestTask)) {
				bestTask = task;
			}
			if (map.get(task) == map.get(bestTask)) {
				int index1 = Integer.parseInt(task.split("T")[1].toString().trim());
				int index2 = Integer.parseInt(bestTask.split("T")[1].toString().trim());
				if (index1 < index2) bestTask = task;
			}
		}
		return bestTask;
	}
	
	public void recharge() {
		List<String> taskList = new ArrayList<>(speculativeUtilities.keySet());
		Collections.sort(taskList, (o1, o2) -> o1.compareTo(o2));

		System.out.print("state={");
		int count = 0;
		for (String task: taskList) {
			count += 1;
			System.out.print(task + "=");
			if (observedUtilities.get(task) != null) {
				System.out.print(observedUtilities.get(task));
			} else {
				System.out.print("NA");
			}
			if (count != speculativeUtilities.keySet().size()) System.out.print(", ");
		}
		System.out.print("} gain=" + gain);
	}
	

    public static void main(String[] args) throws IOException { 
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		Agent agent = new Agent(line);
		while(!(line=br.readLine()).startsWith("end")) {
			if(line.startsWith("TIK")) agent.decideAndAct();
			else agent.perceive(line);
		}
		agent.recharge();
		br.close();
	}
}