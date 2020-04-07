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

	private boolean flexibleDecision;
	private int stepCounter;
	private int restartCounter;
	private String bestTask;
	private String currentTask;
	private double gain;
	
	private HashMap<String, ArrayList<Double>> observedUtilities = new HashMap<String, ArrayList<Double>>();
	private HashMap<String, ArrayList<Integer>> utilitiesIndexes = new HashMap<String, ArrayList<Integer>>();
	private HashMap<String, Double> utilitiesAverage = new HashMap<String, Double>();
	
	private HashMap<String, Double> flexibleProbabilities = new HashMap<String, Double>();
	private ArrayList<String> negativeTasks = new ArrayList<String>();
	
	private Locale locale = new Locale("en", "UK");
	private DecimalFormat frmt = (DecimalFormat) NumberFormat.getNumberInstance(locale);

	public Agent(String options) {
		frmt.applyPattern("#0.00");
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
				if (!flexibleDecision) {
					double newValue = Integer.parseInt(input.split("=")[1].toString().trim());
					if (newValue < 0 && decision.equals("flexible")) {
						flexibleDecision = true;
						negativeTasks.add(bestTask);
					}
					gain += newValue;
					updateMaps(newValue, bestTask);
					utilitiesAverage.put(bestTask, memoryFactorAverage(bestTask));
				} else {
					String task1 = input.split("=")[1].split("\\{")[1].toString().trim();
					String task2 = input.split("=")[2].split(",")[1].toString().trim();
					double utility1 = Double.parseDouble(input.split("=")[2].split(",")[0].toString().trim());
					updateMaps(utility1, task1);
					double utility2 = Double.parseDouble(input.split("=")[3].split("\\}")[0].toString().trim());
					updateMaps(utility2, task2);
					for (String task: flexibleProbabilities.keySet()) {
						if (task.equals(task1)) {
							gain += (utility1 * flexibleProbabilities.get(task));
						} else {
							gain += (utility2 * flexibleProbabilities.get(task));
						}
					}
					utilitiesAverage.put(task1, memoryFactorAverage(task1));
					utilitiesAverage.put(task2, memoryFactorAverage(task2));
					flexibleDecision = false;
					flexibleProbabilities.clear();
				}
			}
		}
	}
	
	private void updateMaps(Double newValue, String task) {
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
	
	private Double memoryFactorAverage(String task) {
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

	public void decideAndAct() {
		stepCounter++;
		System.out.println("Step: " + stepCounter);
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
			if (decision.equals("flexible")) {
				if (flexibleDecision) {
					System.out.println(utilitiesAverage);
					String negativeTask = bestTask;
					String bestPositiveTask = bestFlexibleTask(utilitiesAverage);
					bestTask = bestPositiveTask;
					System.out.println("Negative task: " + negativeTask);
					System.out.println("Best positive: " + bestPositiveTask);
					Double absNegative = Math.abs(observedUtilities.get(negativeTask).get(observedUtilities.get(negativeTask).size() - 1));
					Double bestPositiveUtility = utilitiesAverage.get(bestPositiveTask);
					System.out.println("Negative abs: " + absNegative);
					System.out.println("Positive utility: " + bestPositiveUtility);
					Double prob1 = absNegative / (absNegative + bestPositiveUtility);
					Double prob2 = bestPositiveUtility / (absNegative + bestPositiveUtility);
					int index1 = Integer.parseInt(negativeTask.split("T")[1].toString().trim());
					int index2 = Integer.parseInt(bestPositiveTask.split("T")[1].toString().trim());
					String smallIndex, bigIndex;
					if (index1 < index2) {
						smallIndex = negativeTask;
						bigIndex = bestPositiveTask;
					} else {
						smallIndex = bestPositiveTask;
						bigIndex = negativeTask;
					}
					flexibleProbabilities.put(smallIndex, Math.max(prob1, prob2));
					flexibleProbabilities.put(bigIndex, Math.min(prob1, prob2));
					printFlexibleProbabilities();
					System.out.println();
				} else {
					bestTask = bestFlexibleTask(utilitiesAverage);
				}
			} else {
				bestTask = bestUtilityTask(utilitiesAverage);
			}
		}
	}
	
	private void printFlexibleProbabilities() {
		System.out.print("{");
		int count = 0;
		for (String task: flexibleProbabilities.keySet()) {
			count++;
			System.out.print(task + "=" + frmt.format(flexibleProbabilities.get(task)));
			if (count==1) {
				System.out.print(",");
			}
		}
		System.out.print("}");
	}
	
	private String bestFlexibleTask(HashMap<String, Double> map) {
		String currentBestTask = null;
		for (String task : map.keySet()) {
			if ((map.get(task) >= 0) && (!negativeTasks.contains(task))) {
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
		}
		return currentBestTask;
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

		System.out.print("state={");
		int count = 0;
		for (String task : taskList) {
			count++;
			System.out.print(task + "=");
			if (utilitiesIndexes.get(task) != null) {
				System.out.print(frmt.format(utilitiesAverage.get(task)));
			} else {
				System.out.print("NA");
			}
			if (count != utilitiesAverage.keySet().size())
				System.out.print(",");
		}
		System.out.print("} gain=" + frmt.format(gain));
		System.out.println();
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