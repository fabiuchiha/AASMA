import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MultiAgent extends Agent {
	
	private int steps;
	private String decision;
	private int restart;
	private double memoryFactor;
	
	private String maBestTask;
	private String maCurrentTask;
	private int maStepCounter;
	private int maRestartCounter;
	private double agentsSum;
	private double maGain;
	
	private ArrayList<SimpleAgent> agents = new ArrayList<SimpleAgent>();
	private HashMap<String, Double> homogeneousAverages = new HashMap<String, Double>();
	private HashMap<String, ArrayList<Double>> homogeneousObservedUtilities = new HashMap<String, ArrayList<Double>>();
	private HashMap<String, ArrayList<Integer>> homogeneousIndexes = new HashMap<String, ArrayList<Integer>>();
	
	private Locale locale = new Locale("en", "UK");
	private DecimalFormat frmt = ((DecimalFormat) NumberFormat.getNumberInstance(locale));
	
	public MultiAgent(String options) {
		super(options);
		frmt.applyPattern("#0.00");
		if (options.contains("cycle")) steps = Integer.parseInt(options.split("cycle=")[1].split(" ")[0].toString().trim());
		if (options.contains("decision")) decision = options.split("decision=")[1].split(" ")[0].trim();
		if (options.contains("restart")) restart = Integer.parseInt(options.split("restart=")[1].split(" ")[0].trim());
		if (options.contains("memory-factor")) memoryFactor = Double.parseDouble(options.split("memory-factor=")[1].split(" ")[0].trim());
		
		String[] agentsNames = options.split("agents=\\[")[1].split("\\]")[0].split(",");
		for (String agentName: agentsNames) {
			SimpleAgent newAgent;
			if (agentName.equals(agentsNames[agentsNames.length - 1])) {
				newAgent = new SimpleAgent(true);
			} else {
				newAgent = new SimpleAgent(false);
			}
			newAgent.setName(agentName.trim());
			agents.add(newAgent);
		}
	}
	
	public void perceive(String input) {
		if (decision.equals("homogeneous-society")) {
			if (input.startsWith("T")) {
				String task = input.split(" ")[0].toString();
				double utility = Integer.parseInt(input.split("=")[1].toString().trim());
				homogeneousAverages.put(task, utility);
			} else {
				if (input.startsWith("A")) {
					for (SimpleAgent agent: agents) {
						if (agent.getName().equals(input.split(" ")[0].toString())) {
							double newValue = Integer.parseInt(input.split("=")[1].toString().trim());
							maGain += newValue;
							agentsSum += newValue;
							if (agent.isLastAgent()) {
								Double newAgentsAverage = agentsSum / agents.size();
								updateMaps(newAgentsAverage);			
								homogeneousAverages.put(maBestTask, memoryFactorAverage(maBestTask));
								agentsSum = 0.0;
								for (SimpleAgent agent2: agents) {
									agent2.addUtility(maBestTask, memoryFactorAverage(maBestTask));
								}
							}
						}
					}
				}
			}
		} else {
			
		}
	}
	
	private void updateMaps(Double newAgentsAverage) {
		ArrayList<Double> values;
		ArrayList<Integer> indexes;
		if (homogeneousIndexes.containsKey(maBestTask)) {
			values = homogeneousObservedUtilities.get(maBestTask);
			indexes = homogeneousIndexes.get(maBestTask);
		} else {
			values = new ArrayList<Double>();
			indexes = new ArrayList<Integer>();
		}
		values.add(newAgentsAverage);
		homogeneousObservedUtilities.put(maBestTask, values);
		indexes.add(maStepCounter);
		homogeneousIndexes.put(maBestTask, indexes);
	}
	
	private Double memoryFactorAverage(String task) {
		Double sum = 0.0;
		for (Integer index: homogeneousIndexes.get(task)) {
			sum += Math.pow(index, memoryFactor);
		}
		Double average = 0.0;
		int i=0;
		for (Double value: homogeneousObservedUtilities.get(task)) {
			int valueIndex = homogeneousIndexes.get(task).get(i);
			Double valueProb = Math.pow(valueIndex, memoryFactor) / sum;
			average += (valueProb*value);
			i++;
		}
		return average;
	}
	
	public void decideAndAct() {
		maStepCounter++;
		if (decision.equals("homogeneous-society")) {
			if (restart > 0) {
				HashMap<String, Double> decisionMap = createRestartDecisionMap();
				String task = bestUtilityTask(decisionMap);
				if (maStepCounter == steps) return;
				if (maRestartCounter == restart) {
					if (task.equals(maCurrentTask)) maBestTask = task;
					maRestartCounter = 0;
				} else {
					if (!task.equals(maCurrentTask)) {
						maRestartCounter = 0;
					} else {
						if (maBestTask != null && maBestTask.equals(maCurrentTask) && maRestartCounter == 1) {
							maRestartCounter = 0;
						}
					}
				}
				maRestartCounter += 1;
				maCurrentTask = task;
			} else {
				maBestTask = bestUtilityTask(homogeneousAverages);
			}
		} else {
			
		}
	}
	
	private HashMap<String, Double> createRestartDecisionMap() {
		HashMap<String, Double> decisionMap = new HashMap<String, Double>(homogeneousAverages);
		if (maBestTask != null) {
			for (String task : decisionMap.keySet()) {
				if (!task.equals(maCurrentTask)) {
					decisionMap.put(task, homogeneousAverages.get(task) * (double)(steps - maStepCounter + 1 - restart));
				} else {
					decisionMap.put(task, homogeneousAverages.get(task) * (double)(steps - maStepCounter + 1));
				}
			}
		}
		return decisionMap;
	}
	
	public void recharge() {
		List<String> taskList = new ArrayList<>(homogeneousAverages.keySet());
		Collections.sort(taskList, (o1, o2) -> o1.compareTo(o2));

		System.out.print("state={");
		int agentsCounter = 0;
		for (SimpleAgent agent: agents) {
			agentsCounter++;
			System.out.print(agent.getName() + "={");
			int taskCounter = 0;
			for (String task : taskList) {
				taskCounter++;
				System.out.print(task + "=");
				if (agent.getUtilities().containsKey(task)) {
					System.out.print(frmt.format(agent.getUtilities().get(task)));
				} else {
					System.out.print("NA");
				}
				if (taskCounter != taskList.size()) System.out.print(",");
			}
			if (agentsCounter != agents.size()) System.out.print("},");
		}
		System.out.print("}} gain=" + frmt.format(maGain));
		System.out.println();
	}

}
