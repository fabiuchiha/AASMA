import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SimpleAgent {
	
	Utils utils;
	
	private String name;
	private boolean lastAgent;
	
	private int steps;
	private int restart;
	
	private String bestTask;
	private String currentTask;
	private int stepCounter;
	private int restartCounter;

	private HashMap<String, ArrayList<Double>> observedUtilities = new HashMap<String, ArrayList<Double>>();
	private HashMap<String, ArrayList<Integer>> utilitiesIndexes = new HashMap<String, ArrayList<Integer>>();
	private HashMap<String, Double> utilitiesAverage = new HashMap<String, Double>();
	private HashMap<String, Double> concRestartExpectedUtilities = new HashMap<String, Double>();

	public SimpleAgent(Utils utils, boolean lastAgent, int steps, int restart) {
		this.utils = utils;
		this.lastAgent = lastAgent;
		this.steps = steps;
		this.restart = restart;
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
	
	public void restartDecision() {
		HashMap<String, Double> decisionMap = Utils.createRestartDecisionMap(utilitiesAverage, bestTask, currentTask, stepCounter, restart, steps);
		String task = Utils.bestUtilityTask(decisionMap);
		updateRestartTasks(task);
	}
	
	public void updateRestartTasks(String task) {
		if (stepCounter == steps) return;
		if (restartCounter == restart) {
			if (task.equals(currentTask)) bestTask = task;
			restartCounter = 0;
		} else {
			if (!task.equals(currentTask)) {
				restartCounter = 0;
			} else {
				if (bestTask != null && bestTask.equals(currentTask) && restartCounter == 1) {
					restartCounter = 0;
				}
			}
		}
		restartCounter += 1;
		currentTask = task;
	}
	
	public void updateConcRestartExpectedUtilities() {
		concRestartExpectedUtilities = new HashMap<String, Double>(Utils.createRestartDecisionMap(utilitiesAverage, bestTask, currentTask, stepCounter, restart, steps));
	}

	public HashMap<String, Double> getConcRestartExpectedUtilities() {
		return concRestartExpectedUtilities;
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

	public String getCurrentTask() {
		return currentTask;
	}

	public void setCurrentTask(String currentTask) {
		this.currentTask = currentTask;
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
