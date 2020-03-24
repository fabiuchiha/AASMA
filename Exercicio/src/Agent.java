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

	int stepCounter;
	int restartCounter;
	String bestTask;
	String currentTask;
	double gain;
	
	HashMap<String, ArrayList<Double>> observedUtilities = new HashMap<String, ArrayList<Double>>();
	HashMap<String, ArrayList<Integer>> utilitiesIndexes = new HashMap<String, ArrayList<Integer>>();
	HashMap<String, Double> utilitiesAverage = new HashMap<String, Double>();

	public Agent(String options) {
		if (options.contains("cycle"))
			steps = Integer.parseInt(options.split("cycle=")[1].split(" ")[0].toString().trim());
		if (options.contains("decision"))
			decision = options.split("decision=")[1].split(" ")[0].trim();
		if (options.contains("restart"))
			restart = Integer.parseInt(options.split("restart=")[1].split(" ")[0].trim());
		if (options.contains("memory-factor"))
			memoryFactor = Double.parseDouble(options.split("memory-factor=")[1].split(" ")[0].trim());
	}

	public void perceive(String input) {
		if (input.startsWith("T")) {
			String task = input.split(" ")[0].toString();
			double utility = Integer.parseInt(input.split("=")[1].toString().trim());
			utilitiesAverage.put(task, utility);
		} else {
			if (input.startsWith("A")) {
				double newValue = Integer.parseInt(input.split("=")[1].toString().trim());
				gain += newValue;
				if (!utilitiesIndexes.containsKey(bestTask)) {
					ArrayList<Double> values = new ArrayList<Double>();
					values.add(newValue);
					observedUtilities.put(bestTask, values);
					ArrayList<Integer> indexes = new ArrayList<Integer>();
					indexes.add(stepCounter);
					utilitiesIndexes.put(bestTask, indexes);
					utilitiesAverage.put(bestTask, newValue);
				} else {
					ArrayList<Double> values = observedUtilities.get(bestTask);
					values.add(newValue);
					observedUtilities.put(bestTask, values);
					ArrayList<Integer> indexes = utilitiesIndexes.get(bestTask);
					indexes.add(stepCounter);
					utilitiesIndexes.put(bestTask, indexes);
					Double newAverage = memoryFactorAverage();
					utilitiesAverage.put(bestTask, newAverage);
				}
			}
		}
	}
	
	private Double memoryFactorAverage() {
		Double sum = 0.0;
		for (Integer index: utilitiesIndexes.get(bestTask)) {
			sum += Math.pow(index, memoryFactor);
		}
		Double average = 0.0;
		int i=0;
		for (Double value: observedUtilities.get(bestTask)) {
			int valueIndex = utilitiesIndexes.get(bestTask).get(i);
			Double valueProb = Math.pow(valueIndex, memoryFactor) / sum;
			average += (valueProb*value);
			i++;
		}
		return average;
	}

	public void decideAndAct() {
		if (restart > 0) {
			stepCounter += 1;
			HashMap<String, Double> decisionMap = createRestartDecisionMap();
			String task = bestUtilityTask(decisionMap);
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
		} else {
			stepCounter += 1;
			bestTask = bestUtilityTask(utilitiesAverage);
		}
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

	private String bestUtilityTask(HashMap<String, Double> map) {
		String currentBestTask = null;
		for (String task : map.keySet()) {
			if (currentBestTask == null || map.get(task) > map.get(currentBestTask)) {
				currentBestTask = task;
			} else {
				if (map.get(task).compareTo(map.get(currentBestTask)) == 0) {
					int index1 = Integer.parseInt(task.split("T")[1].toString().trim());
					int index2 = Integer.parseInt(currentBestTask.split("T")[1].toString().trim());
					if (index1 < index2)
						currentBestTask = task;
				}
			}
		}
		return currentBestTask;
	}

	public void recharge() {
		List<String> taskList = new ArrayList<>(utilitiesAverage.keySet());
		Collections.sort(taskList, (o1, o2) -> o1.compareTo(o2));

		Locale locale = new Locale("en", "UK");
		DecimalFormat f = (DecimalFormat) NumberFormat.getNumberInstance(locale);
		f.applyPattern("#0.00");

		System.out.print("state={");
		int count = 0;
		for (String task : taskList) {
			count += 1;
			System.out.print(task + "=");
			if (utilitiesIndexes.get(task) != null) {
				System.out.print(f.format(utilitiesAverage.get(task)));
			} else {
				System.out.print("NA");
			}
			if (count != utilitiesAverage.keySet().size())
				System.out.print(",");
		}
		System.out.print("} gain=" + f.format(gain));
	}

	public static void main(String[] args) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = br.readLine();
		Agent agent = new Agent(line);
		while (!(line = br.readLine()).startsWith("end")) {
			if (line.startsWith("TIK"))
				agent.decideAndAct();
			else
				agent.perceive(line);
		}
		agent.recharge();
		br.close();
	}
}