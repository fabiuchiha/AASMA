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
								Double newAverage = 0.0;
								updateMaps(newAgentsAverage);
								if (homogeneousIndexes.containsKey(maBestTask)) {
									double oldAverage = homogeneousAverages.get(maBestTask);
									int counter = homogeneousIndexes.get(maBestTask).size();
									newAverage = oldAverage + ((newAgentsAverage - oldAverage) / (counter));
								} else {									
									newAverage = newAgentsAverage;
								}
								homogeneousAverages.put(maBestTask, newAverage);
								agentsSum = 0.0;
								for (SimpleAgent agent2: agents) {
									agent2.addUtility(maBestTask, newAverage);
								}
								//System.out.println("Task: " + maBestTask + ", Utilidade: " + newAgentsAverage);
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
	
	public void decideAndAct() {
		maStepCounter++;
		if (decision.equals("homogeneous-society")) {
			maBestTask = bestUtilityTask(homogeneousAverages);
			//System.out.println(homogeneousAverages.toString());
		} else {
			
		}
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
