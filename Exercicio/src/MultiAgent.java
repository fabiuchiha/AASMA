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
	private int concurrencyPenalty;
	private double memoryFactor;
	
	private String maBestTask;
	private String maCurrentTask;
	private int maStepCounter;
	private int maRestartCounter;
	private double agentsSum;
	private double maGain;
	private boolean concurrentHomogeneousSameTask;
	
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
		if (options.contains("concurrency-penalty")) concurrencyPenalty = Integer.parseInt(options.split("concurrency-penalty=")[1].split(" ")[0].trim());
		if (options.contains("memory-factor")) memoryFactor = Double.parseDouble(options.split("memory-factor=")[1].split(" ")[0].trim());
		
		String[] agentsNames = options.split("agents=\\[")[1].split("\\]")[0].split(",");
		for (String agentName: agentsNames) {
			SimpleAgent newAgent;
			if (agentName.equals(agentsNames[agentsNames.length - 1])) {
				newAgent = new SimpleAgent(true, steps, restart, concurrencyPenalty, memoryFactor);
			} else {
				newAgent = new SimpleAgent(false, steps, restart, concurrencyPenalty, memoryFactor);
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
							if (concurrencyPenalty > 0 && !concurrentHomogeneousSameTask) {
								updateMaps(newValue, agent.getBestTask());
								homogeneousAverages.put(agent.getBestTask(), memoryFactorAverage(agent.getBestTask()));
								agent.addUtilityAverage(agent.getBestTask(), memoryFactorAverage(agent.getBestTask()));
								return;
							} else {
								agentsSum += newValue;
								Double newAgentsAverage = 0.0;
								if (agent.isLastAgent()) {
									newAgentsAverage = agentsSum / agents.size();
									updateMaps(newAgentsAverage, maBestTask);			
									homogeneousAverages.put(maBestTask, memoryFactorAverage(maBestTask));
									agentsSum = 0.0;
									for (SimpleAgent agent2: agents) {
										agent2.addUtilityAverage(maBestTask, memoryFactorAverage(maBestTask));
									}
								}
							}						
						}			
					}				
				}
			}
		} else {
			if (input.startsWith("T")) {
				String task = input.split(" ")[0].toString();
				double utility = Integer.parseInt(input.split("=")[1].toString().trim());
				for (SimpleAgent agent: agents) {
					agent.addUtilityAverage(task, utility);
				}
			} else {
				if (input.startsWith("A")) {
					for (SimpleAgent agent: agents) {
						if (agent.getName().equals(input.split(" ")[0].toString())) {
							double newValue = Integer.parseInt(input.split("=")[1].toString().trim());
							maGain += newValue;
							agent.updateMaps(newValue, agent.getBestTask());
							agent.addUtilityAverage(agent.getBestTask(), agent.memoryFactorAverage(agent.getBestTask()));
						}
					}
				}
			}
		}
	}
	
	private void updateMaps(Double newAgentsAverage, String task) {
		ArrayList<Double> values;
		ArrayList<Integer> indexes;
		if (homogeneousIndexes.containsKey(task)) {
			values = homogeneousObservedUtilities.get(task);
			indexes = homogeneousIndexes.get(task);
		} else {
			values = new ArrayList<Double>();
			indexes = new ArrayList<Integer>();
		}
		values.add(newAgentsAverage);
		homogeneousObservedUtilities.put(task, values);
		indexes.add(maStepCounter);
		homogeneousIndexes.put(task, indexes);
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
				if (concurrencyPenalty > 0) {
					
				} else {
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
				}
			} else {
				if (concurrencyPenalty > 0) {
					if (agents.size() == 2) {
						bestHomogeneousConcurrencyDecision2();
					}
				} else {
					maBestTask = bestUtilityTask(homogeneousAverages);
				}
			}
		} else {
			if (restart > 0) {
				if (concurrencyPenalty > 0) {
					bestRestartHeterogeneousConcurrencyDecision();
				} else {
					for (SimpleAgent agent: agents) {
						agent.setStepCounter(agent.getStepCounter() + 1);
						agent.restartDecision();
					}
				}
			} else {
				if (concurrencyPenalty > 0) {
					if (agents.size() == 2) bestHeterogeneousConcurrencyDecision2();
					if (agents.size() == 5) bestHeterogeneousConcurrencyDecision5();
				} else {
					for (SimpleAgent agent: agents) {
						agent.setStepCounter(agent.getStepCounter() + 1);
						String agentBestTask = bestUtilityTask(agent.getUtilitiesAverage());
						agent.setBestTask(agentBestTask);
					}
				}
			}
		}
	}
	
	private void bestRestartHeterogeneousConcurrencyDecision() {
		for (SimpleAgent agent: agents) {
			agent.setStepCounter(agent.getStepCounter() + 1);
			agent.updateExpectedUtilities();
			System.out.println(agent.getUtilitiesAverage());
		}
		ArrayList<String> bestTasks = new ArrayList<String>(); 
		for (SimpleAgent agent: agents) {
			String bestTask = bestUtilityTask(agent.getConcExpectedUtilities());
			if (bestTasks.contains(bestTask)) {
				String secondBest = ignoreBestTask(agent.getConcExpectedUtilities(), bestTask);
				Double concUtility;
				if (agent.getBestTask() != null) concUtility = (agent.getConcExpectedUtilities().get(bestTask) - ((steps - maStepCounter + 1) * concurrencyPenalty)) 
							+ (agents.get(bestTasks.indexOf(bestTask)).getConcExpectedUtilities().get(bestTask) - ((steps - maStepCounter + 1) * concurrencyPenalty));
				else concUtility = agent.getConcExpectedUtilities().get(bestTask) + agents.get(bestTasks.indexOf(bestTask)).getConcExpectedUtilities().get(bestTask);
				Double sumUt = agent.getConcExpectedUtilities().get(secondBest) + agents.get(bestTasks.indexOf(bestTask)).getConcExpectedUtilities().get(bestTask);
				if (sumUt > concUtility) bestTasks.add(secondBest);
				else bestTasks.add(bestTask);
			} else {
				bestTasks.add(bestTask);
			}
		}
		for (int i=0; i<bestTasks.size(); i++) {
			agents.get(i).updateRestartTasks(bestTasks.get(i));
		}
	}

	private void bestHeterogeneousConcurrencyDecision2() {
		for (SimpleAgent agent: agents) {
			agent.setStepCounter(agent.getStepCounter() + 1);
			agent.setBestTask(bestUtilityTask(agent.getUtilitiesAverage()));
		}
		String a1BestTask = agents.get(0).getBestTask();
		String a2BestTask = agents.get(1).getBestTask();
		if (a1BestTask.equals(a2BestTask)) {
			Double concUtility = (agents.get(0).getUtilitiesAverage().get(a1BestTask) - concurrencyPenalty) + (agents.get(1).getUtilitiesAverage().get(a2BestTask) - concurrencyPenalty);
			String a2SecondBest = ignoreBestTask(agents.get(1).getUtilitiesAverage(), a2BestTask);
			Double sumUt = agents.get(0).getUtilitiesAverage().get(a1BestTask) + agents.get(1).getUtilitiesAverage().get(a2SecondBest);
			if (sumUt > concUtility) {
				agents.get(1).setBestTask(a2SecondBest);
			}
		}
	}
	
	private void bestHeterogeneousConcurrencyDecision5() {
		for (SimpleAgent agent: agents) {
			agent.setStepCounter(agent.getStepCounter() + 1);
		}
		ArrayList<String> bestTasks = new ArrayList<String>(); 
		for (SimpleAgent agent: agents) {
			String bestTask = bestUtilityTask(agent.getUtilitiesAverage());
			if (bestTasks.contains(bestTask)) {
				String secondBest = ignoreBestTask(agent.getUtilitiesAverage(), bestTask);
				Double concUtility;
				concUtility = (agent.getUtilitiesAverage().get(bestTask) - concurrencyPenalty) 
							+ (agents.get(bestTasks.indexOf(bestTask)).getUtilitiesAverage().get(bestTask) - concurrencyPenalty);
				Double sumUt = agent.getUtilitiesAverage().get(secondBest) + agents.get(bestTasks.indexOf(bestTask)).getUtilitiesAverage().get(bestTask);
				if (sumUt > concUtility) bestTasks.add(secondBest);
				else bestTasks.add(bestTask);
			} else {
				bestTasks.add(bestTask);
			}
		}
		for (int i=0; i<bestTasks.size(); i++) {
			agents.get(i).setBestTask(bestTasks.get(i));
		}
	}

	private void bestHomogeneousConcurrencyDecision2() {
		String currentBestTask = bestUtilityTask(homogeneousAverages);
		Double bestTaskUtility = homogeneousAverages.get(currentBestTask);
		String currentSecondBest = ignoreBestTask(homogeneousAverages, currentBestTask);
		Double secondBestUtility = homogeneousAverages.get(currentSecondBest);
		if ((bestTaskUtility-1) * agents.size() >= (bestTaskUtility + secondBestUtility)) {
			concurrentHomogeneousSameTask = true;
			maBestTask = currentBestTask;
		} else {
			concurrentHomogeneousSameTask = false;
			int index1 = Integer.parseInt(currentBestTask.split("T")[1].toString().trim());
			int index2 = Integer.parseInt(currentSecondBest.split("T")[1].toString().trim());
			if (index1 < index2) {
				agents.get(0).setBestTask(currentBestTask);
				agents.get(1).setBestTask(currentSecondBest);
			} else {
				agents.get(0).setBestTask(currentSecondBest);
				agents.get(1).setBestTask(currentBestTask);
			}
		}
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
	
	public String ignoreBestTask(HashMap<String, Double> map, String ignoreTask) {
		Locale l = new Locale("en", "UK");
		DecimalFormat f = (DecimalFormat) NumberFormat.getNumberInstance(l);
		f.applyPattern("#0.0000");
		String currentBestTask = null;
		for (String task : map.keySet()) {
			if (!task.equals(ignoreTask)) {
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
		}
		return currentBestTask;
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
		if (decision.equals("homogeneous-society")) {
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
					if (homogeneousIndexes.containsKey(task)) {
						System.out.print(frmt.format(homogeneousAverages.get(task)));
					} else {
						System.out.print("NA");
					}
					if (taskCounter != taskList.size()) System.out.print(",");
				}
				if (agentsCounter != agents.size()) System.out.print("},");
			}
			System.out.print("}} gain=" + frmt.format(maGain));
			System.out.println();
		} else {
			System.out.print("state={");
			int agentsCounter = 0;
			for (SimpleAgent agent: agents) {
				agentsCounter++;
				List<String> taskList = agent.getOrderedTaskList();
				System.out.print(agent.getName() + "={");
				int taskCounter = 0;
				for (String task : taskList) {
					taskCounter++;
					System.out.print(task + "=");
					if (agent.getUtilitiesIndexes().containsKey(task)) {
						System.out.print(frmt.format(agent.getUtilitiesAverage().get(task)));
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

}
