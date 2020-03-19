import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Agent {

	int steps;
	String decision;
	int restart;
	double memoryFactor;
	
	int preparingStage;
	String bestTask;
	double gain;
	HashMap<String, Double> speculativeUtilities = new HashMap<String, Double>();
	HashMap<String, Double> observedUtilities = new HashMap<String, Double>();
	HashMap<String, Integer> utilityCounter = new HashMap<String, Integer>();
	
	public Agent(String options) {
		if (options.contains("cycle")) steps = Integer.parseInt(options.split("cycle=")[1].split(" ")[0].toString().trim());
		if (options.contains("decision")) decision = options.split("decision=")[1].split(" ")[0].trim();
		if (options.contains("restart")) restart = Integer.parseInt(options.split("restart=")[1].split(" ")[0].trim());
		if (options.contains("memory-factor")) memoryFactor = Double.parseDouble(options.split("memory-factor=")[1].split(" ")[0].trim());
	}
	
	public void perceive(String input) {
		if (input.startsWith("T")) {
			String task = input.split(" ")[0].toString();
			double utility = Integer.parseInt(input.split("=")[1].toString().trim());
			speculativeUtilities.put(task, utility);
		} else {
			if (input.startsWith("A")) {
				double newValue = Integer.parseInt(input.split("=")[1].toString().trim());
				gain += newValue;
				if (!observedUtilities.containsKey(bestTask)) {
					speculativeUtilities.put(bestTask, newValue);
					observedUtilities.put(bestTask, newValue);
					utilityCounter.put(bestTask, 1);
				} else {
					double oldAverage = observedUtilities.get(bestTask);
					int oldUtilityCounter = utilityCounter.get(bestTask);
					double newAverage = oldAverage + ((newValue - oldAverage) / (oldUtilityCounter + 1));
					speculativeUtilities.put(bestTask, newAverage);
					observedUtilities.put(bestTask, newAverage);
					utilityCounter.put(bestTask, oldUtilityCounter + 1);
				}
			}
		}
	}
	
	public void decideAndAct() {
		if (restart > 0) {
			if (preparingStage == restart) {
				bestTask = bestUtilityTask(speculativeUtilities);
				preparingStage = 0;
			} else {
				preparingStage += 1;
			}	
		} else {
			bestTask = bestUtilityTask(speculativeUtilities);
		}	
	}
	
	private String bestUtilityTask(HashMap<String, Double> map) {
		String bestTask = null;
		for (String task: map.keySet()) {
			if (bestTask == null || map.get(task) > map.get(bestTask)) {
				bestTask = task;
			} else {
				if (map.get(task).compareTo(map.get(bestTask)) == 0) {
					int index1 = Integer.parseInt(task.split("T")[1].toString().trim());
					int index2 = Integer.parseInt(bestTask.split("T")[1].toString().trim());
					if (index1 < index2) bestTask = task;
				}
			}
		}
		return bestTask;
	}
	
	public void recharge() {
		List<String> taskList = new ArrayList<>(speculativeUtilities.keySet());
		Collections.sort(taskList, (o1, o2) -> o1.compareTo(o2));

		Locale locale  = new Locale("en", "UK");
		DecimalFormat f = (DecimalFormat)NumberFormat.getNumberInstance(locale);
		f.applyPattern("#0.00");

		System.out.print("state={");
		int count = 0;
		for (String task: taskList) {
			count += 1;
			System.out.print(task + "=");
			if (observedUtilities.get(task) != null) {
				System.out.print(f.format(observedUtilities.get(task)));
			} else {
				System.out.print("NA");
			}
			if (count != speculativeUtilities.keySet().size()) System.out.print(",");
		}
		System.out.print("} gain=" + f.format(gain));
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