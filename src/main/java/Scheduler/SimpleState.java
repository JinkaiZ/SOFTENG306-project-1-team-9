package Scheduler;

import java.util.ArrayList;
import java.util.HashMap;

public class SimpleState {

    protected int[] tasksStartTime;
    protected int[] tasksProcessorTracker;
    protected int[] processorsFinishTime;
    protected HashMap<Integer, ArrayList<Integer>> dynamicInAdjacentList = new HashMap<>();
    protected int remainTaskDuration;
    protected int loadBalance;

    public SimpleState( int[] tasksStartTime, int[] tasksProcessorTracker,
                        int[] processorsFinishTime, HashMap<Integer , ArrayList<Integer>> dynamicInAdjacentList, int remainTaskDuration, int loadBalance) {
        this.tasksStartTime = tasksStartTime;
        this.tasksProcessorTracker = tasksProcessorTracker;
        this.processorsFinishTime = processorsFinishTime;
        this.dynamicInAdjacentList = dynamicInAdjacentList;
        this.remainTaskDuration = remainTaskDuration;
        this.loadBalance = loadBalance;
    }

    public int getLoadBalance() {
        return this.loadBalance;
    }

    public void setLoadBalance(int loadBalance) {
        this.loadBalance = loadBalance;
    }

    public HashMap<Integer, ArrayList<Integer>> getDynamicInAdjacentList() {
        return dynamicInAdjacentList;
    }

    public void setTasksStartTime(int taskID, int startTime) {
        tasksStartTime[taskID] = startTime;
    }

    public void setTasksProcessorTracker(int taskID, int processorNum) {
        tasksProcessorTracker[taskID] = processorNum;
    }

    public void setProcessorsFinishTime(int processorNum, int finishTime) {
        processorsFinishTime[processorNum] = finishTime;
    }

    public void subtractRemainTaskDuration(int weight) {
        remainTaskDuration = remainTaskDuration - weight;
    }

    // Make a deep copy / clone of the Search state
    public SimpleState clone() {

        int nOfTasks = tasksStartTime.length;
        int nOfP = processorsFinishTime.length;

        int[] tasksStartTimeClone = new int[nOfTasks];
        int[] tasksProcessorTrackerClone = new int[nOfTasks];
        int[] processorsFinishTimeClone = new int[nOfP];

        for (int i = 0; i < nOfTasks; i++) {
            tasksStartTimeClone[i] = tasksStartTime[i];
            tasksProcessorTrackerClone[i] = tasksProcessorTracker[i];
        }

        for(int i = 0; i < nOfP; i++) {
            processorsFinishTimeClone[i] = processorsFinishTime[i];
        }

        HashMap<Integer , ArrayList<Integer>> dynamicInAdjacentListClone = new HashMap<>();
        for(int key : dynamicInAdjacentList.keySet()) {
            dynamicInAdjacentListClone.put(key, new ArrayList<>(dynamicInAdjacentList.get(key)));
        }

        return new SimpleState(tasksStartTimeClone, tasksProcessorTrackerClone, processorsFinishTimeClone, dynamicInAdjacentListClone, this.remainTaskDuration, this.loadBalance);
    }

}
