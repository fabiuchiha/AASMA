import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Utils {
	
	public static Double memoryFactorAverage(HashMap<String, ArrayList<Integer>> utilitiesIndexes, HashMap<String, ArrayList<Double>> observedUtilities, String task, double memoryFactor) {
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
	
	public static HashMap<String, Double> createRestartDecisionMap(HashMap<String, Double> utilitiesAverages, String bestTask, String currentTask, double stepCounter, int restart, int steps ) {
		HashMap<String, Double> decisionMap = new HashMap<String, Double>(utilitiesAverages);
		if (bestTask != null) {
			for (String task : decisionMap.keySet()) {
				if (!task.equals(currentTask)) {
					decisionMap.put(task, utilitiesAverages.get(task) * (double)(steps - stepCounter + 1 - restart));
				} else {
					decisionMap.put(task, utilitiesAverages.get(task) * (double)(steps - stepCounter + 1));
				}
			}
		}
		return decisionMap;
	}
	
	public static String bestUtilityTask(HashMap<String, Double> map) {
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
	
	public static List<String> getOrderedTaskList(HashMap<String, Double> utilitiesAverage) {
		List<String> taskList = new ArrayList<>(utilitiesAverage.keySet());
		Collections.sort(taskList, (o1, o2) -> o1.compareTo(o2));
		return taskList;
	}

}
