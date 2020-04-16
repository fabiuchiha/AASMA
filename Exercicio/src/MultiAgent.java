import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
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
	
	private ArrayList<SimpleAgent> agents = new ArrayList<SimpleAgent>();
	private HashMap<String, Double> homogeneousAverages = new HashMap<String, Double>();
	private HashMap<String, ArrayList<Double>> homogeneousObservedUtilities = new HashMap<String, ArrayList<Double>>();
	private HashMap<String, ArrayList<Integer>> homogeneousIndexes = new HashMap<String, ArrayList<Integer>>();
	private HashMap<String, List<Integer>> repeatedTasks = new HashMap<String, List<Integer>>();
	private HashMap<String, ArrayList<Double>> homogeneousTempAverages = new HashMap<String, ArrayList<Double>>();

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
							if (concurrencyPenalty > 0) {
								boolean repAgent = false;
								for (String t: repeatedTasks.keySet()) {
									for (int index: repeatedTasks.get(t)) {
										if (agent.getName().equals(agents.get(index).getName())) {
											repAgent = true;
											if (repeatedTasks.get(t).size() > 1) {
												ArrayList<Double> tempList;
												if (homogeneousTempAverages.containsKey(t)) {
													tempList = new ArrayList<Double>(homogeneousTempAverages.get(t));
												} else {
													tempList = new ArrayList<Double>();
												}
												tempList.add(newValue);
												homogeneousTempAverages.put(t, tempList);
												if (repeatedTasks.get(t).size() == homogeneousTempAverages.get(t).size()) {
													Double average = 0.0;
													for (Double u: homogeneousTempAverages.get(t)) {
														average += u;
													}
													average /= homogeneousTempAverages.get(t).size();
													updateMaps(average, agent.getBestTask());
													homogeneousAverages.put(agent.getBestTask(), Utils.memoryFactorAverage(homogeneousIndexes, homogeneousObservedUtilities, agent.getBestTask(), memoryFactor));
												}
											} else {
												updateMaps(newValue, agent.getBestTask());
												homogeneousAverages.put(agent.getBestTask(), Utils.memoryFactorAverage(homogeneousIndexes, homogeneousObservedUtilities, agent.getBestTask(), memoryFactor));										
											}
										}
									}
								}
								if (!repAgent) {
									updateMaps(newValue, agent.getBestTask());
									homogeneousAverages.put(agent.getBestTask(), Utils.memoryFactorAverage(homogeneousIndexes, homogeneousObservedUtilities, agent.getBestTask(), memoryFactor));
								}
							} else {
								agentsSum += newValue;
								Double newAgentsAverage = 0.0;
								if (agent.isLastAgent()) {
									newAgentsAverage = agentsSum / agents.size();
									updateMaps(newAgentsAverage, maBestTask);			
									homogeneousAverages.put(maBestTask, Utils.memoryFactorAverage(homogeneousIndexes, homogeneousObservedUtilities, maBestTask, memoryFactor));
									agentsSum = 0.0;
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
				if (concurrencyPenalty > 0) homogeneousConcurrentDecision();	
				else {
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
				if (concurrencyPenalty > 0) homogeneousConcurrentDecision();
				else maBestTask = Utils.bestUtilityTask(homogeneousAverages);
			}
		} else {
			if (restart > 0) {
				if (concurrencyPenalty > 0) heterogeneousConcurrentRestartDecision();
				else {
					for (SimpleAgent agent: agents) {
						agent.setStepCounter(agent.getStepCounter() + 1);
						agent.restartDecision();
					}
				}
			} else {
				if (concurrencyPenalty > 0) heterogeneousConcurrentDecision();
				else {
					for (SimpleAgent agent: agents) {
						agent.setStepCounter(agent.getStepCounter() + 1);
						String agentBestTask = Utils.bestUtilityTask(agent.getUtilitiesAverage());
						agent.setBestTask(agentBestTask);
					}
				}
			}
		}
	}

	private void homogeneousConcurrentDecision() {
		homogeneousTempAverages.clear();
		ArrayList<ArrayList<String>> lists = new ArrayList<ArrayList<String>>();
		for (SimpleAgent agent: agents) {
			agent.setStepCounter(agent.getStepCounter() + 1);
			ArrayList<String> list = new ArrayList<String>();
			for (String task: homogeneousAverages.keySet()) {
				list.add(task);
			}
			lists.add(list);
		}
		ArrayList<String> bestTaskSequence = getBestTaskSequence(lists, true, false);
		for (int i=0; i<agents.size(); i++) {
			agents.get(i).setBestTask(bestTaskSequence.get(i));
		}
	}

	private void heterogeneousConcurrentRestartDecision() {
		ArrayList<ArrayList<String>> lists = new ArrayList<ArrayList<String>>();
		for (SimpleAgent agent: agents) {
			agent.setStepCounter(agent.getStepCounter() + 1);
			agent.updateConcRestartExpectedUtilities();
			ArrayList<String> list = new ArrayList<String>();
			for (String task: agent.getConcRestartExpectedUtilities().keySet()) {
				list.add(task);
			}
			lists.add(list);
		}
		ArrayList<String> bestTaskSequence = getBestTaskSequence(lists, false, true);
		for (int i=0; i<agents.size(); i++) {
			agents.get(i).updateRestartTasks(bestTaskSequence.get(i));
		}
	}
	
	private void heterogeneousConcurrentDecision() {
		ArrayList<ArrayList<String>> lists = new ArrayList<ArrayList<String>>();
		for (SimpleAgent agent: agents) {
			agent.setStepCounter(agent.getStepCounter() + 1);
			ArrayList<String> list = new ArrayList<String>();
			for (String task: agent.getUtilitiesAverage().keySet()) {
				list.add(task);
			}
			lists.add(list);
		}
		ArrayList<String> bestTaskSequence = getBestTaskSequence(lists, false, false);
		for (int i=0; i<agents.size(); i++) {
			agents.get(i).setBestTask(bestTaskSequence.get(i));
		}
	}
		
	private ArrayList<String> getBestTaskSequence(ArrayList<ArrayList<String>> lists, boolean isHomogeneous, boolean hasRestart) {
		Locale l = new Locale("en", "UK");
		DecimalFormat f = (DecimalFormat) NumberFormat.getNumberInstance(l);
		f.applyPattern("#0.0000");
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
	            if (isHomogeneous) sum += homogeneousAverages.get(task);
	            else sum += agents.get(counter).getUtilitiesAverage().get(task);
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
					if (hasRestart) sum -= (repTasks.get(t).size() * ((steps - maStepCounter + 1) * concurrencyPenalty));
					else sum -= (repTasks.get(t).size() * concurrencyPenalty);
					updateSumTasks.add(t);
				}
			}
			sum = Double.parseDouble(f.format(sum));
			if (sum > bestSum) {
				bestSum = sum;
				bestTaskSequence = new ArrayList<>(currentTasks);
				repeatedTasks = new HashMap<String, List<Integer>>(repTasks);
			} else {
				if (sum == bestSum) {
					int k = 0;
					while (k<bestTaskSequence.size()) {
						int index1 = Integer.parseInt(bestTaskSequence.get(k).split("T")[1].toString().trim());
						int index2 = Integer.parseInt(currentTasks.get(k).split("T")[1].toString().trim());
						if (index2 < index1) {
							bestSum = sum;
							bestTaskSequence = new ArrayList<>(currentTasks);
							repeatedTasks = new HashMap<String, List<Integer>>(repTasks);
							break;
						}
						k++;
					}
				}
			}
	    }
	    if (isHomogeneous) Collections.sort(bestTaskSequence, (o1, o2) -> o1.compareTo(o2));
	    return bestTaskSequence;
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
