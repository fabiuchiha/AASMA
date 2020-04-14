import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MultiAgent extends Agent {
	
	Utils utils;
	
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
		utils = new Utils();
		if (options.contains("cycle")) steps = Integer.parseInt(options.split("cycle=")[1].split(" ")[0].toString().trim());
		if (options.contains("decision")) decision = options.split("decision=")[1].split(" ")[0].trim();
		if (options.contains("restart")) restart = Integer.parseInt(options.split("restart=")[1].split(" ")[0].trim());
		if (options.contains("concurrency-penalty")) concurrencyPenalty = Integer.parseInt(options.split("concurrency-penalty=")[1].split(" ")[0].trim());
		if (options.contains("memory-factor")) memoryFactor = Double.parseDouble(options.split("memory-factor=")[1].split(" ")[0].trim());
		
		String[] agentsNames = options.split("agents=\\[")[1].split("\\]")[0].split(",");
		for (String agentName: agentsNames) {
			SimpleAgent newAgent;
			if (agentName.equals(agentsNames[agentsNames.length - 1])) {
				newAgent = new SimpleAgent(utils, true, steps, restart);
			} else {
				newAgent = new SimpleAgent(utils, false, steps, restart);
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
								homogeneousAverages.put(agent.getBestTask(), Utils.memoryFactorAverage(homogeneousIndexes, homogeneousObservedUtilities, agent.getBestTask(), memoryFactor));
								agent.addUtilityAverage(agent.getBestTask(), Utils.memoryFactorAverage(homogeneousIndexes, homogeneousObservedUtilities, agent.getBestTask(), memoryFactor));
								return;
							} else {
								agentsSum += newValue;
								Double newAgentsAverage = 0.0;
								if (agent.isLastAgent()) {
									newAgentsAverage = agentsSum / agents.size();
									updateMaps(newAgentsAverage, maBestTask);			
									homogeneousAverages.put(maBestTask, Utils.memoryFactorAverage(homogeneousIndexes, homogeneousObservedUtilities, maBestTask, memoryFactor));
									agentsSum = 0.0;
									for (SimpleAgent agent2: agents) {
										agent2.addUtilityAverage(maBestTask, Utils.memoryFactorAverage(homogeneousIndexes, homogeneousObservedUtilities, maBestTask, memoryFactor));
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
							agent.addUtilityAverage(agent.getBestTask(), Utils.memoryFactorAverage(agent.getUtilitiesIndexes(), agent.getObservedUtilities(), agent.getBestTask(), memoryFactor));
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
	
	public void decideAndAct() {
		maStepCounter++;
		if (decision.equals("homogeneous-society")) {
			if (restart > 0) {
				if (concurrencyPenalty > 0) {
					
				} else {
					HashMap<String, Double> decisionMap = Utils.createRestartDecisionMap(homogeneousAverages, maBestTask, maCurrentTask, maStepCounter, restart, steps);
					String task = Utils.bestUtilityTask(decisionMap);
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
					maBestTask = Utils.bestUtilityTask(homogeneousAverages);
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
					if (agents.size() == 5) heterogeneousConcurrentDecision();
					else bestHeterogeneousConcurrencyDecision2();
				} else {
					for (SimpleAgent agent: agents) {
						agent.setStepCounter(agent.getStepCounter() + 1);
						String agentBestTask = Utils.bestUtilityTask(agent.getUtilitiesAverage());
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
		}
		ArrayList<String> bestTasks = new ArrayList<String>(); 
		for (SimpleAgent agent: agents) {
			String bestTask = Utils.bestUtilityTask(agent.getConcExpectedUtilities());
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
			agent.setBestTask(Utils.bestUtilityTask(agent.getUtilitiesAverage()));
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
	
	private void heterogeneousConcurrentDecision() {
		Locale l = new Locale("en", "UK");
		DecimalFormat f = (DecimalFormat) NumberFormat.getNumberInstance(l);
		f.applyPattern("#0.0000");		
		ArrayList<ArrayList<String>> lists = new ArrayList<ArrayList<String>>();
		for (SimpleAgent agent: agents) {
			agent.setStepCounter(agent.getStepCounter() + 1);
			ArrayList<String> list = new ArrayList<String>();
			for (String task: agent.getUtilitiesAverage().keySet()) {
				list.add(task);
			}
			lists.add(list);
		}
		Double bestSum = 0.0;
		ArrayList<String> bestTaskSequence = new ArrayList<String>();
		int solutions = 1;
	    for(int i = 0; i < lists.size(); solutions *= lists.get(i).size(), i++);
	    for(int i = 0; i < solutions; i++) {
	        int j = 1;
	        double sum = 0.0;
	        int counter = 0;
	        ArrayList<String> currentTasks = new ArrayList<String>();
	        for(ArrayList<String> list: lists) {
	            String task = list.get((i/j)%list.size());
	            currentTasks.add(task);
	            sum += agents.get(counter).getUtilitiesAverage().get(task);
	            j *= list.size();;
	            counter++;
	        }
	        HashMap<String, List<Integer>> repTasks = new HashMap<String, List<Integer>>();
			for (int m=0; m<currentTasks.size(); m++) {
			    repTasks.computeIfAbsent(currentTasks.get(m), c -> new ArrayList<>()).add(m);
			}
			ArrayList<String> updateSumTasks = new ArrayList<String>();
			for (String t: repTasks.keySet()) {
				if (repTasks.get(t).size() > 1 && !updateSumTasks.contains(t)) {
					sum -= (repTasks.get(t).size() * concurrencyPenalty);
					updateSumTasks.add(t);
				}
			}
			sum = Double.parseDouble(f.format(sum));
			if (sum > bestSum) {
				bestSum = sum;
				bestTaskSequence.clear();
				bestTaskSequence = new ArrayList<>(currentTasks);
			} else {
				if (sum == bestSum) {
					int k = 0;
					while (k<bestTaskSequence.size()) {
						int index1 = Integer.parseInt(bestTaskSequence.get(k).split("T")[1].toString().trim());
						int index2 = Integer.parseInt(currentTasks.get(k).split("T")[1].toString().trim());
						if (index2 < index1) {
							bestSum = sum;
							bestTaskSequence.clear();
							bestTaskSequence = new ArrayList<>(currentTasks);
							break;
						}
						k++;
					}
				}
			}
	    }
		for (int i=0; i<agents.size(); i++) {
			agents.get(i).setBestTask(bestTaskSequence.get(i));
		}
	}
	
//	private void bestHeterogeneousConcurrencyDecision5() {
//		Locale l = new Locale("en", "UK");
//		DecimalFormat f = (DecimalFormat) NumberFormat.getNumberInstance(l);
//		f.applyPattern("#0.0000");		
//		for (SimpleAgent agent: agents) {
//			agent.setStepCounter(agent.getStepCounter() + 1);
//		}
//		HashMap<String, Double> mapA1 = agents.get(0).getUtilitiesAverage();
//		HashMap<String, Double> mapA2 = agents.get(1).getUtilitiesAverage();
//		HashMap<String, Double> mapA3 = agents.get(2).getUtilitiesAverage();
//		HashMap<String, Double> mapA4 = agents.get(3).getUtilitiesAverage();
//		HashMap<String, Double> mapA5 = agents.get(4).getUtilitiesAverage();		
//		Double bestSum = 0.0;
//		ArrayList<String> bestTaskSequence = new ArrayList<String>();		
//		for (String task1: mapA1.keySet()) {
//			for (String task2: mapA2.keySet()) {
//				for (String task3: mapA3.keySet()) {
//					for (String task4: mapA4.keySet()) {
//						for (String task5: mapA5.keySet()) {
//							Double sum = Double.parseDouble(f.format(mapA1.get(task1) + mapA2.get(task2) + mapA3.get(task3) + mapA4.get(task4) + mapA5.get(task5)));
//							List<String> tasks = Arrays.asList(task1, task2, task3, task4, task5);
//							ArrayList<String> tempTasks = new ArrayList<>();
//							tempTasks.addAll(tasks);
//							HashMap<String, List<Integer>> repTasks = new HashMap<String, List<Integer>>();
//							for (int i = 0; i < tempTasks.size(); i++) {
//							    repTasks.computeIfAbsent(tempTasks.get(i), c -> new ArrayList<>()).add(i);
//							}
//							ArrayList<String> updateSumTasks = new ArrayList<String>();
//							for (String task: repTasks.keySet()) {
//								if (repTasks.get(task).size() > 1 && !updateSumTasks.contains(task)) {
//									sum -= (repTasks.get(task).size() * concurrencyPenalty);
//									updateSumTasks.add(task);
//								}
//							}	
//							if (sum > bestSum) {
//								bestSum = sum;
//								bestTaskSequence.clear();
//								bestTaskSequence.addAll(tasks);
//							} else {
//								if (sum == bestSum) {
//									int i = 0;
//									while (i<bestTaskSequence.size()) {
//										int index1 = Integer.parseInt(bestTaskSequence.get(i).split("T")[1].toString().trim());
//										int index2 = Integer.parseInt(tempTasks.get(i).split("T")[1].toString().trim());
//										if (index2 < index1) {
//											bestSum = sum;
//											bestTaskSequence.clear();
//											bestTaskSequence.addAll(tasks);
//											break;
//										}
//										i++;
//									}
//								}
//							}
//							
//							
//						}
//					}
//				}
//			}
//		}
//		for (int i=0; i<agents.size(); i++) {
//			agents.get(i).setBestTask(bestTaskSequence.get(i));
//		}	
//	}

	private void bestHomogeneousConcurrencyDecision2() {
		String currentBestTask = Utils.bestUtilityTask(homogeneousAverages);
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
	
	public void recharge() {
		if (decision.equals("homogeneous-society")) {
			List<String> taskList = Utils.getOrderedTaskList(homogeneousAverages);
	
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
