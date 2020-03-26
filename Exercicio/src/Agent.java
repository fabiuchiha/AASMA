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

	private int steps;
	private String decision;
	private int restart;
	private double memoryFactor;

	private int stepCounter;
	private int restartCounter;
	private String bestTask;
	private String currentTask;
	private double gain;
	
	private HashMap<String, ArrayList<Double>> observedUtilities = new HashMap<String, ArrayList<Double>>();
	private HashMap<String, ArrayList<Integer>> utilitiesIndexes = new HashMap<String, ArrayList<Integer>>();
	private HashMap<String, Double> utilitiesAverage = new HashMap<String, Double>();

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
			utilitiesAverage.put(task, utility);
		} else {
			if (input.startsWith("A")) {
				double newValue = Integer.parseInt(input.split("=")[1].toString().trim());
				gain += newValue;
				updateMaps(newValue);
				if (!utilitiesIndexes.containsKey(bestTask)) utilitiesAverage.put(bestTask, newValue);
				else utilitiesAverage.put(bestTask, memoryFactorAverage());
			}
		}
	}
	
	private void updateMaps(Double newValue) {
		ArrayList<Double> values;
		ArrayList<Integer> indexes;
		if (utilitiesIndexes.containsKey(bestTask)) {
			values = observedUtilities.get(bestTask);
			indexes = utilitiesIndexes.get(bestTask);
		} else {
			values = new ArrayList<Double>();
			indexes = new ArrayList<Integer>();
		}
		values.add(newValue);
		observedUtilities.put(bestTask, values);
		indexes.add(stepCounter);
		utilitiesIndexes.put(bestTask, indexes);
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
		stepCounter++;
		if (restart > 0) {
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

	public String bestUtilityTask(HashMap<String, Double> map) {
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
		
		Agent agent;
		if (line.contains("decision=")) {
			String decision = line.split("decision=")[1].split(" ")[0].trim();
			if (decision.equals("homogeneous-society") || decision.equals("heterogeneous-society")) {
				agent = new MultiAgent(line);
			} else {
				agent = new Agent(line);
			}
		} else {
			agent = new Agent(line);
		}
		
		while (!(line = br.readLine()).startsWith("end")) {
			if (line.startsWith("TIK")) agent.decideAndAct();
			else agent.perceive(line);
		}
		agent.recharge();
		br.close();
	}

}