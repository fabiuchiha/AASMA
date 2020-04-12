import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SimpleAgent {
	
	private String name;
	private boolean lastAgent;
	
	private int steps;
	private int restart;
	private int concurrencyPenalty;
	private double memoryFactor;
	
	private String bestTask;
	private String currentTask;
	private int stepCounter;
	private int restartCounter;

	private HashMap<String, ArrayList<Double>> observedUtilities = new HashMap<String, ArrayList<Double>>();
	private HashMap<String, ArrayList<Integer>> utilitiesIndexes = new HashMap<String, ArrayList<Integer>>();
	private HashMap<String, Double> utilitiesAverage = new HashMap<String, Double>();
	private HashMap<String, Double> concExpectedUtilities = new HashMap<String, Double>();

	public SimpleAgent(boolean lastAgent, int steps, int restart, int concurrencyPenalty, double memoryFactor) {
		this.lastAgent = lastAgent;
		this.steps = steps;
		this.restart = restart;
		this.concurrencyPenalty = concurrencyPenalty;
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
	
	public void restartDecision() {
		HashMap<String, Double> decisionMap = createRestartDecisionMap();
		String task = bestUtilityTask(decisionMap);
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
	
	private HashMap<String, Double> createRestartDecisionMap() {
		HashMap<String, Double> decisionMap = new HashMap<String, Double>(utilitiesAverage);
		if (bestTask != null) {
			for (String task : decisionMap.keySet()) {
				if (!task.equals(currentTask)) {
					decisionMap.put(task, utilitiesAverage.get(task) * (double)(steps - stepCounter + 1 - restart));
				} else {
					decisionMap.put(task, utilitiesAverage.get(task) * (double)(steps - stepCounter + 1));
				}
			}
		}
		return decisionMap;
	}
	
	public String bestUtilityTask(HashMap<String, Double> map) {
		Locale l = new Locale("en", "UK");
		DecimalFormat f = (DecimalFormat) NumberFormat.getNumberInstance(l);
		f.applyPattern("#0.0000");
		String currentBestTask = null;
		for (String task : map.keySet()) {
			if (currentBestTask == null || Double.parseDouble(f.format(map.get(task))) > Double.parseDouble(f.format(map.get(currentBestTask)))) {
				currentBestTask = task;
			} else {
				if (Double.parseDouble(f.format(map.get(task))) == Double.parseDouble(f.format(map.get(currentBestTask)))) {
					int index1 = Integer.parseInt(task.split("T")[1].toString().trim());
					int index2 = Integer.parseInt(currentBestTask.split("T")[1].toString().trim());
					if (index1 < index2)
						currentBestTask = task;
				}
			}
		}
		return currentBestTask;
	}
	
	public void updateExpectedUtilities() {
		concExpectedUtilities = new HashMap<String, Double>(createRestartDecisionMap());
	}

	public HashMap<String, Double> getConcExpectedUtilities() {
		return concExpectedUtilities;
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
