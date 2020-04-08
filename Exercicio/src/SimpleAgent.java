import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SimpleAgent {
	
	private String name;
	private boolean lastAgent;
	
	private int steps;
	private String decision;
	private int restart;
	private double memoryFactor;
	
	private String bestTask;
	private String currentTask;
	private int stepCounter;
	private int restartCounter;

	private HashMap<String, ArrayList<Double>> observedUtilities = new HashMap<String, ArrayList<Double>>();
	private HashMap<String, ArrayList<Integer>> utilitiesIndexes = new HashMap<String, ArrayList<Integer>>();
	private HashMap<String, Double> utilitiesAverage = new HashMap<String, Double>();

	public SimpleAgent(boolean lastAgent, int steps, String decision, int restart, double memoryFactor) {
		this.lastAgent = lastAgent;
		this.steps = steps;
		this.decision = decision;
		this.restart = restart;
		this.memoryFactor = memoryFactor;
	}
	
	public void updateMaps(Double newValue, String task) {
		ArrayList<Double> values;
		ArrayList<Integer> indexes;
		if (utilitiesIndexes.containsKey(task)) {
			values = observedUtilities.get(task);
			indexes = utilitiesIndexes.get(task);
		} else {
			values = new ArrayList<Double>();
			indexes = new ArrayList<Integer>();
		}
		values.add(newValue);
		observedUtilities.put(task, values);
		indexes.add(stepCounter);
		utilitiesIndexes.put(task, indexes);
	}
	
	public Double memoryFactorAverage(String task) {
		Double sum = 0.0;
		for (Integer index: utilitiesIndexes.get(task)) {
			sum += Math.pow(index, memoryFactor);
		}
		Double average = 0.0;
		int i=0;
		for (Double value: observedUtilities.get(task)) {
			int valueIndex = utilitiesIndexes.get(task).get(i);
			Double valueProb = Math.pow(valueIndex, memoryFactor) / sum;
			average += (valueProb*value);
			i++;
		}
		return average;
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
	
	public int getStepCounter() {
		return stepCounter;
	}

	public void setStepCounter(int stepCounter) {
		this.stepCounter = stepCounter;
	}
	
	public int getRestartCounter() {
		return restartCounter;
	}

	public void setRestartCounter(int restartCounter) {
		this.restartCounter = restartCounter;
	}
	
	public String getBestTask() {
		return bestTask;
	}

	public void setBestTask(String bestTask) {
		this.bestTask = bestTask;
	}

	public HashMap<String, Double> getUtilitiesAverage() {
		return utilitiesAverage;
	}

	public void addUtilityAverage(String task, double utility) {
		utilitiesAverage.put(task, utility);
	}

	public HashMap<String, ArrayList<Double>> getObservedUtilities() {
		return observedUtilities;
	}
	
	public HashMap<String, ArrayList<Integer>> getUtilitiesIndexes() {
		return utilitiesIndexes;
	}

	public List<String> getOrderedTaskList() {
		List<String> taskList = new ArrayList<>(utilitiesAverage.keySet());
		Collections.sort(taskList, (o1, o2) -> o1.compareTo(o2));
		return taskList;
	}
	
	
	
}
